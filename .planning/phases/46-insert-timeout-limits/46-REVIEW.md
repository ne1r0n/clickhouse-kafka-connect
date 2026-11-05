---
phase: 46-insert-timeout-limits
reviewed: 2026-11-05T00:00:00Z
depth: deep
files_reviewed: 13
files_reviewed_list:
  - src/main/java/com/clickhouse/kafka/connect/sink/db/ClickHouseWriter.java
  - src/main/java/com/clickhouse/kafka/connect/sink/db/helper/ClickHouseHelperClient.java
  - src/main/java/com/clickhouse/kafka/connect/sink/ClickHouseSinkConfig.java
  - docs/FORK-DELTA.md
  - src/test/java/com/clickhouse/kafka/connect/sink/InsertBoundPinFixtures.java
  - src/test/java/com/clickhouse/kafka/connect/sink/InsertTimeoutHelpTextPinUnitTest.java
  - src/test/java/com/clickhouse/kafka/connect/sink/InsertV1WaitBoundPinUnitTest.java
  - src/test/java/com/clickhouse/kafka/connect/sink/InsertWriteStallBoundPinUnitTest.java
  - src/test/java/com/clickhouse/kafka/connect/sink/db/helper/InsertBoundGateUnitTest.java
  - src/test/java/com/clickhouse/kafka/connect/sink/db/helper/StubClickHouseServer.java
  - src/test/java/com/clickhouse/kafka/connect/sink/db/helper/StubClickHouseServerRawModeUnitTest.java
  - .planning/phases/46-insert-timeout-limits/46-COMMAND-EVIDENCE.md
  - src/main/java/com/clickhouse/kafka/connect/sink/processing/Processing.java
findings:
  critical: 0
  warning: 5
  info: 7
  total: 12
  necessity: 0
status: issues_found
---

# Phase 46: Code Review Report

**Reviewed:** 2026-11-05
**Depth:** deep
**Files Reviewed:** 13
**Status:** issues_found

## Summary

Reviewed the diff `3d4ce2e8..HEAD` for `src` and `docs`: the V2 `InsertWriteDeadline` watchdog and its
three call sites, `awaitInsertResponseV1` and its three call sites, the `insertBoundMs` gate, the new help
text of `clickhouseClientInsertTimeoutMs`, the FORK-DELTA row and item 14, and the new tests and stub. I did
not run the tests. For the client V2 contract I decompiled `client-v2-0.11.0.jar` (`Client.cancelTransportRequest`,
`registerTransportReq`, `failIfCancelled`, the `insert` retry loop, `ClientConfigProperties.RETRY_ON_FAILURE`)
and read the code against it.

Priority results:

1. **Thread safety and leaks of the watchdog.** Lazy creation is under `insertWatchdogLock`; `scheduled` is
   volatile and is assigned before the insert starts, so `disarm()` from the client's writer callback sees it.
   `arm()` is the only place a thread starts, so a failure between `insertWriteDeadline(...)` and `arm()` leaks
   nothing. `setRemoveOnCancelPolicy(true)` keeps the queue empty, the thread is a daemon and `stop()` ends it.
   A fire that is already running when `disarm()` calls `cancel(false)` cannot be recalled, but
   `cancelTransportRequest` on an unregistered id is a no-op in 0.11.0 (`ongoingRequests.get` returns null), and
   `Processing` makes a fresh UUID query id per batch. The one same-id reuse, the schema-refresh retry inside
   `doInsertRawBinary`, runs after a network round trip, so a stale cancel cannot reach it in practice. No race
   found that loses or double-reports an insert.
2. **Exactly-once.** The deadline only turns a failure into a `RetriableException`; `Processing` is untouched, so
   `BEFORE`/`AFTER` flags and the dedup token are unchanged and the retry rebuilds the identical batch. A fire
   that races a successful insert returns the success, because `throwIfFired` runs only on the exception path.
   Under `exactlyOnce=false` a cut insert the server still commits is retried as a duplicate, which is recorded
   and stated in the help text.
3. **Message and query-id content.** Both new messages carry the setting name, the bound and the query id
   (a UUID plus offsets in `QueryIdentifier`); the cause is attached unchanged. No record value or secret is
   added. The existing Phase 45 value-free rebuild still governs the serialization exceptions that can become
   the cause.
4. **Claims.** The numbers in item 14 match E-04..E-11 (checked each: 1.010-1.082 s, 1.137-1.218 s, 1.023, 1.013,
   1.51, 1530, 8299/8315/8059, 3050, 8380/8324, 2305, 3034, 2082, 20005, 30034-30045, 2500 then 4000). Five wording
   findings below, including the four the executor flagged. One substantive code gap: a client-internal retry
   escapes the V2 write deadline (WR-01).

No blocker found.

## Warnings

### WR-01: The V2 write deadline is disarmed after the first attempt and never re-armed on a client-internal retry

**File:** `src/main/java/com/clickhouse/kafka/connect/sink/db/ClickHouseWriter.java:1494-1501, 1796-1807, 1949-1957, 2208-2227`
(also `docs/FORK-DELTA.md` per-insert `socket_timeout` row, "the client makes one request (no client-side retry)")
**Issue:** `InsertWriteDeadline` is armed once, before `client.insert`, and disarmed the first time the
`DataStreamWriter` closes the body stream. The V2 client does retry an insert: `ClientConfigProperties.RETRY_ON_FAILURE`
(`retry`) defaults to `3`, the retried faults are `NoHttpResponse`, `ConnectTimeout`, `ConnectionRequestTimeout` and
`ServerRetryable`, and `Client.insert` calls `DataStreamWriter.onRetry()` and then runs `onOutput` again for the next
attempt. The connector sets neither the retry count nor `onRetry`. So after a first attempt that closed its body and
then failed with, for example, a closed connection or a 503, the second attempt sends the body again with the deadline
already cancelled. A peer that stops reading during that second attempt blocks the SinkTask thread with no connector
bound: the per-insert `socket_timeout` is a read-inactivity limit and does not cover a blocked write. That is the
failure the phase exists to remove, and it can push the task past `consumer.override.max.poll.interval.ms`.
No pin runs a retried attempt: every pin has the stub never answer, so the first attempt is the only one.
The claim "one insert makes one request" (class Javadoc of `InsertWriteDeadline`) holds only for a cancelled
request (`failIfCancelled` stops a retry after a cancel); the unqualified "the client makes one request (no
client-side retry)" in the Phase 41 row is wrong in general and now sits next to the new deadline row.
**Fix:** Re-arm in the writer's retry hook, for example by making the lambda a small class:

```java
DataStreamWriter dataWriter = new DataStreamWriter() {
    @Override public void onOutput(OutputStream out) throws IOException {
        pushStreamTime.set(writeRecordsRowBinary(records, table, out, supportDefaults));
        out.close();
        deadline.disarm();
    }
    @Override public void onRetry() { deadline.rearm(); } // schedule a fresh task for boundMs
};
```

Add a pin where the stub closes the first request after the body and stalls the second, and narrow the sentence in
the Phase 41 row to "a read timeout is not retried". Alternatively set the client's `retry` option to 0 for inserts.

### WR-02: Help text and item 14 do not say that on client V1, the default client, the library's 30 s limit still ends a quiet insert

**File:** `src/main/java/com/clickhouse/kafka/connect/sink/ClickHouseSinkConfig.java:848-851`; `docs/FORK-DELTA.md` item 14
**Issue:** The V1 clause says the setting bounds "the whole wait for the insert response, including ... a quiet
server". With the default of 240000 ms a quiet V1 insert is ended at about 30 s by the library's own inactivity limit
(E-07: the unchanged tree waited for it; E-08 notes "the library's 30 s inactivity limit ended it on the unchanged tree"),
so at the default value this key is not what ends a quiet server. For any value above 30000 the key also cannot be
raised to allow a quiet insert that legitimately runs longer than 30 s: it still fails at 30 s unless the operator
names `socket_timeout`. The sentence "An insert that legitimately runs longer than this limit fails" therefore
understates it on V1 (the limit is the smaller of the two). The old item 14 said the library's 30 s "stays the only
read bound" for V1 inserts; the rewrite keeps the 30 s figure only for non-insert requests, so the statement was
dropped, not changed by a measurement. A V1 operator tuning this key is misled in the direction that causes failures.
**Fix:** Add to the V1 clause: "a quiet server is also ended by the client library's own 30 s inactivity limit, so a
value above 30000 ms does not extend it; name socket_timeout in jdbcConnectionProperties to change that limit." Restore
the matching sentence in item 14 and add a required phrase to `InsertTimeoutHelpTextPinUnitTest`.

### WR-03: The help text says the V1 bound covers "a stalled body write (measured with batches up to 256 MiB)", which contradicts the code Javadoc and FORK-DELTA

**File:** `src/main/java/com/clickhouse/kafka/connect/sink/ClickHouseSinkConfig.java:846-848`;
`src/main/java/com/clickhouse/kafka/connect/sink/db/ClickHouseWriter.java:2003-2010`
**Issue:** Executor gap (1). The measurement is that no stall inside the V1 body write was reached at 16, 64 or 256 MiB;
the task thread was always in the response wait (E-07, E-09). The Javadoc of `awaitInsertResponseV1` and item 14 say a
write that blocks on a full pipe "is not reached by this bound" and that no bound is claimed. The help text reads the
other way: "including a stalled body write (measured with batches up to 256 MiB)" reads as "a stalled body write is
bounded, verified up to 256 MiB". It names no residual, so the one place an operator reads says the opposite of the
two places that are accurate. The residual is real: a body larger than the pipe with a consumer that stops draining
blocks the task thread until the stream's write timeout (the socket timeout, 30 s by default), not until this bound.
That is a real defect (a text that overclaims its subject) but a bounded one, and it is retriable by the existing
"Write timed out" classification.
**Fix:** Replace the parenthetical with the scope as measured, and name the residual:
"On client V1: the wait for the insert response, which also ends a server that stops reading the batch for batches
up to 256 MiB (measured: the batch is fully handed to the client before the wait starts). A write that blocks on the
client's full 4 MiB pipe is not bounded by this setting; it ends with 'Write timed out' after socket_timeout."
Update H3 in the pin to match.

### WR-04: The help text's "30 s ... and ping at the timeout setting" is per attempt and names an ambiguous setting

**File:** `src/main/java/com/clickhouse/kafka/connect/sink/ClickHouseSinkConfig.java:855-858`;
`src/test/java/com/clickhouse/kafka/connect/sink/InsertTimeoutHelpTextPinUnitTest.java:69-72`
**Issue:** Executor gaps (2) and (3), both real. (2) E-11 measured the V1 non-insert requests with the helper client's
retry count at 1; `pingV1` and `queryV1` loop over `retry`, whose default is 3 (`ClickHouseHelperClient` lines
266-290 and 345-385), so a V1 `DESCRIBE`, table list or version query can take about 90 s before it returns, and
FORK-DELTA item 14 says "per-attempt" while the help text, the artifact an operator reads, says "end at ... 30 s" with
no count. (3) "the timeout setting" sits in the help text of a key called `clickhouseClientInsertTimeoutMs`, so it reads
as this key; it means `timeoutSeconds`. Read that way the sentence is false for this key and it disagrees with the
V2 clause two lines above (ping has no limit on V2). The pin H8 fixes the ambiguous wording verbatim, so a correction
fails `testUnit` until the pin is changed with it.
**Fix:** "on client V1 each attempt ends at the client library's default 30 s inactivity limit and the connector
repeats a failed request up to retryCount (default 3) times; the ping uses timeoutSeconds." Change H8 to match.

### WR-05: A deadline cancel on the RowBinary route logs two ERROR lines that blame one record and column

**File:** `src/main/java/com/clickhouse/kafka/connect/sink/db/ClickHouseWriter.java:1080-1086, 1562-1585`
**Issue:** When the deadline fires, `cancelTransportRequest` aborts the request and the next write on the body stream
in `writeRecordsRowBinary` throws an `IOException`. `doWriteColValue` catches every `Exception`, logs at ERROR
`Error writing value of ... column ... exception <class>`, and `writeColumnWithRecordContext` logs at ERROR
`Record serialization failed: topic ... partition ... offset ... column ...`, then rethrows. The only line that says
the insert timed out is the exception thrown later, outside those logs. An operator watching the log sees a
serialization error naming whichever record happened to be in flight, which is the wrong diagnosis for a transport
timeout and sends them looking for a poison record. The same happens for any broken pipe, but the new deadline makes it
a routine, expected path. No test asserts the log lines on this path. The JSON and String routes are not affected.
**Fix:** Do not log per record or column when the deadline has already fired. Pass a `BooleanSupplier` (the
deadline's `fired` flag) into the RowBinary write, or check it in the two catch blocks, and log one WARN line from
`throwIfFired`; at minimum have `throwIfFired` log `Insert deadline fired; the write errors above are the cancel`.

## Info

### IN-01: The first sentence of the help text still states a general bound that the rest of the text qualifies

**File:** `src/main/java/com/clickhouse/kafka/connect/sink/ClickHouseSinkConfig.java:841-843`
**Issue:** "Maximum time in milliseconds the connector gives one ClickHouse insert before it fails the batch" is the
text the Connect UI shows first. It is not true as a general statement: on V1 the clock starts after serialization and
the body write (so the batch's total time is the serialization time plus the bound), and on V2 a sent batch with
progress runs on. The qualifying paragraphs follow, and the pin checks only that the retired phrase is gone, not this
lead.
**Fix:** "Time limit in milliseconds for the phases of one ClickHouse insert listed below; when it is exceeded the
connector fails the batch with a RetriableException ...".

### IN-02: "Progress headers while the batch is still being sent" rests on a stub pin only (executor gap 4); the claim is plausible and the wording is acceptable

**File:** `src/main/java/com/clickhouse/kafka/connect/sink/ClickHouseSinkConfig.java:844-846`;
`src/test/java/com/clickhouse/kafka/connect/sink/InsertWriteStallBoundPinUnitTest.java:233-250`
**Issue:** Not a defect in behaviour. The deadline is wall-clock and independent of what the server sends, so the claim
follows from the design, not only from the stub; the stub pin shows it. The live run (E-10) never had a body still
being sent at the bound, and FORK-DELTA says so. The help text states it flatly. If the client ever started honouring
early responses (reading the response before the entity is written) the deadline would still fire, so the direction of
the claim cannot be wrong in a harmful way. Severity is informational.
**Fix:** None required. If the text is edited anyway, "(pinned against a scripted server)" costs nothing.

### IN-03: `lateFireAfterASuccessfulV2InsertIsHarmless` cannot fail for the scenario its name gives

**File:** `src/test/java/com/clickhouse/kafka/connect/sink/InsertWriteStallBoundPinUnitTest.java:378-397`
**Issue:** The test relies on `disarm()` having cancelled the task, so no late fire ever happens; it uses a different
query id for the second insert, so a stray cancel could not reach it either. E-06 mutation D1 (never disarm) confirms
it: only `progressStreamingV2InsertRunsPastTheBoundOnceTheBodyIsSent` went red, this case stayed green. The name and
the FORK-DELTA wording ("late-fire pair of inserts passed") suggest the harmless no-op path of a fire after the request
ended is pinned; it is not.
**Fix:** Rename to what it pins ("a second insert after the first one's bound has elapsed succeeds"), or add a case that
fires the canceller by hand against an unregistered id (the constructor takes a `Consumer<String>`), or reuses the
same query id for the second insert.

### IN-04: The text says the connector adds no limit of its own when the operator names `socket_timeout`; `awaitInsertResponse` still does with `async=true`

**File:** `src/main/java/com/clickhouse/kafka/connect/sink/ClickHouseSinkConfig.java:859-860`;
`src/main/java/com/clickhouse/kafka/connect/sink/db/ClickHouseWriter.java:1990-2000`
**Issue:** The unchanged `awaitInsertResponse` applies `future.get(csc.getClickhouseClientInsertTimeoutMs())`
regardless of `insertBoundMs`. With `async=true` the future is real, so a V2 connector with `socket_timeout` and
`async=true` in `jdbcConnectionProperties` is still cut at this key's value, and a cut future is cancelled but its
request keeps running. The claim "no limit of its own on either client" (H9) is therefore untrue for that combination.
It is an unusual combination and the behaviour is pre-existing.
**Fix:** Qualify H9 with "(except the response wait when async=true)", or pass `insertBoundMs` into
`awaitInsertResponse` so the gate is complete.

### IN-05: V1 timeout path ignores the result of `cancel(true)`, and a response that completes in the gap is reported as a failure

**File:** `src/main/java/com/clickhouse/kafka/connect/sink/db/ClickHouseWriter.java:2016-2027`
**Issue:** `CompletableFuture.cancel` neither interrupts nor stops the worker, so the Javadoc's "the future is
cancelled ... which releases the connection and the worker" is true only because the caller's try-with-resources then
closes the client (which the release pin shows). If the response completes between the `TimeoutException` and
`cancel`, `cancel` returns false, the completed `ClickHouseResponse` is dropped unclosed and a successful insert is
reported as a retriable failure; with `exactlyOnce=false` the retry duplicates. The window is a few microseconds.
**Fix:** Check the result and return the response when the cancel loses:

```java
} catch (TimeoutException e) {
    if (!future.cancel(true) && !future.isCompletedExceptionally()) {
        return future.getNow(null);
    }
    throw new RetriableException(String.format(V1_INSERT_TIMEOUT_FORMAT, bound, queryId.getQueryId()), e);
}
```

State in the Javadoc that the release comes from closing the client.

### IN-06: `stop()` during an insert can surface a `RejectedExecutionException` from `arm()`

**File:** `src/main/java/com/clickhouse/kafka/connect/sink/db/ClickHouseWriter.java:2037-2060, 211-221`
**Issue:** `stop()` calls `shutdownNow()` without waiting and nulls the field, while an insert that already holds the
old executor calls `deadline.arm()`, which throws `RejectedExecutionException` (a non-retriable `RuntimeException`) from
outside the `try`. `chc` is also nulled by `stop()`, so the same overlap already breaks other paths; Kafka Connect calls
`stop()` after the task thread has finished, so this is shutdown only.
**Fix:** Catch `RejectedExecutionException` in `arm()` and treat it as "not armed", or leave as is and note it.

### IN-07: A literal `E-NN` placeholder stayed in FORK-DELTA item 14, and the per-attempt budget of the schema-refresh retry is not mentioned

**File:** `docs/FORK-DELTA.md` item 14 ("`.planning/phases/46-insert-timeout-limits/46-COMMAND-EVIDENCE.md` E-NN"),
`src/main/java/com/clickhouse/kafka/connect/sink/db/ClickHouseWriter.java:1405-1410`
**Issue:** The sentence "Bounded, with the numbers (... E-NN):" cites a template token, not an entry; the evidence
check (`item14_cites: yes`) did not catch it. Separately, `doInsertRawBinary` retries once after a schema refresh with
the same query id and a fresh `InsertWriteDeadline`, so a refresh retry gets a second full bound; item 14 and the
help text say "one insert" and do not mention it.
**Fix:** Replace `E-NN` with the entries it stands for (E-04, E-05, E-07, E-08, E-09, E-10, E-11), and add a clause that a
schema-refresh retry starts a new bound.

---

_Reviewed: 2026-11-05_
_Reviewer: Claude (gsd-code-reviewer)_
_Depth: deep_
