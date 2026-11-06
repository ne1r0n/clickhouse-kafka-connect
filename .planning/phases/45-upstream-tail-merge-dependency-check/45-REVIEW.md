---
phase: 45-upstream-tail-merge-dependency-check
reviewed: 2026-11-04T00:00:00Z
depth: standard
files_reviewed: 12
files_reviewed_list:
  - src/main/java/com/clickhouse/kafka/connect/sink/db/ClickHouseWriter.java
  - src/main/java/com/clickhouse/kafka/connect/util/Utils.java
  - docs/FORK-DELTA.md
  - src/test/java/com/clickhouse/kafka/connect/sink/JsonRouteValueFreeDlqUnitTest.java
  - src/test/java/com/clickhouse/kafka/connect/sink/WriterRecordContextDlqUnitTest.java
  - src/test/java/com/clickhouse/kafka/connect/sink/db/ClickHouseWriterRecordContextTest.java
  - src/test/java/com/clickhouse/kafka/connect/sink/db/FieldDescriptorEscapeTest.java
  - src/test/java/com/clickhouse/kafka/connect/sink/db/JacksonOverIoValueFreeUnitTest.java
  - src/test/java/com/clickhouse/kafka/connect/sink/db/JsonRouteValueFreeUnitTest.java
  - src/test/java/com/clickhouse/kafka/connect/sink/db/WriterRecordContextUnitTest.java
  - src/test/java/com/clickhouse/kafka/connect/sink/db/WriterValueFreeFailureUnitTest.java
  - src/test/java/com/clickhouse/kafka/connect/sink/db/helper/FieldDescriptorLiteralFieldsUnitTest.java
findings:
  critical: 0
  warning: 4
  info: 5
  total: 9
  necessity: 0
status: issues_found
---

# Phase 45: Code Review Report

**Reviewed:** 2026-11-04
**Depth:** standard
**Files Reviewed:** 12
**Status:** issues_found

## Summary

Reviewed the diff `12e2cf17..HEAD` for `src` and `docs`: the value-free Jackson-chain rebuild
(`withheldJacksonChain`, `sameKindStandIn`, `withheldJacksonRoot`, `throwRebuiltJacksonChain`), the
re-expressed RowBinary per-record seam (`writeColumnWithRecordContext`), the guarded JSON route
(`writeRecordsJson`), the `Utils` constant visibility change, the FORK-DELTA rows and the new and
edited tests. The four unit classes `JacksonOverIoValueFreeUnitTest`, `JsonRouteValueFreeUnitTest`,
`WriterRecordContextUnitTest` and `FieldDescriptorLiteralFieldsUnitTest` were run with
`./gradlew testUnit --offline` and pass.

Priority results:

1. Record value reaching a message, DLQ header or log line. The new log line
   (`RECORD_CONTEXT_FAILURE_LOG_FORMAT`) passes five arguments for five placeholders: topic, partition,
   offset, column and the exception class name. It passes no throwable. The rebuilt top texts are built
   from `Column` metadata, the table name and class names. I found no path that leaks a value, with one
   residual: suppressed exceptions on kept links (WR-02).
2. Retry classification. Traced `Utils.handleException` against the rebuild: root-cause type and
   timeout prefix, the first server-error code, any `RetriableException` link and the `DataException`
   top are all preserved. No regression found for chains the tests exercise. Two gaps in the pins
   remain (WR-01).
3. Hot path. The new `try` blocks add no work on the success path. The only eager cost is on the
   failure path (IN-02).
4. FORK-DELTA. Claims match the code except for two over-statements (IN-01).

No blocker found. The warnings concern a fail-open security helper, an unexamined residual, thin pins
for the retry-critical branches and heavy test duplication.

## Warnings

### WR-01: Retry-classification pins miss the Write-timeout prefix branch and the realistic Map-lambda shape

**File:** `src/test/java/com/clickhouse/kafka/connect/sink/db/JacksonOverIoValueFreeUnitTest.java:84-112`
(also `src/main/java/com/clickhouse/kafka/connect/sink/db/ClickHouseWriter.java:787-800`)
**Issue:** `withheldJacksonRoot` has two prefix branches (`Read timed out after` and `Write timed out
after`). No shape in `Shape` (19 chains) or in `JsonRouteValueFreeUnitTest.Shape` has a Jackson root
whose message starts with the write prefix. `PLAIN_IO_OVER_TIMEOUT_PREFIXED_JACKSON_ROOT` uses only the
read prefix. The test helper computes the expected prefix from the original message, so the write case
is never driven. Deleting the `else if` for the write prefix, which would turn a retriable write
timeout into a task failure, passes every test. The comment on `passesThroughUnchanged` names the most
realistic production shape: the Map/Tuple/Array lambdas wrap a stream failure in a `RuntimeException`.
The matching rebuild chain `RuntimeException -> Jackson -> SocketTimeoutException` (exact
`RuntimeException` top, Jackson in the middle, trusted IO root) is also absent. Only
`RUNTIME_OVER_JACKSON_ROOT`, with a Jackson root, exists. The DLQ and outcome tests therefore never
check that the mid-chain Jackson link is dropped and that the top is rebuilt with the
`SocketTimeoutException` as its direct cause.
**Fix:** Add shapes and mirror them in the JSON-route test:

```java
PLAIN_IO_OVER_WRITE_TIMEOUT_PREFIXED_JACKSON_ROOT(
        () -> new IOException(STREAM_CLOSED, jackson(WRITE_TIMEOUT_PREFIX + " 30000 ms " + JACKSON_TEXT))),
RUNTIME_OVER_JACKSON_OVER_SOCKET_TIMEOUT(
        () -> new RuntimeException(jackson(JACKSON_TEXT, new SocketTimeoutException(TIMED_OUT)))),
```

### WR-02: Rebuild is fail-open and the kept suffix is not scanned for suppressed exceptions

**File:** `src/main/java/com/clickhouse/kafka/connect/sink/db/ClickHouseWriter.java:723-731, 757-766, 1082-1084, 1677-1679`
**Issue:** Two weaknesses in a control whose job is to stop record data leaving the writer.
(a) `throwRebuiltJacksonChain` returns normally when the rebuilt value is neither an `IOException`
nor a `RuntimeException`. Every caller then executes `throw e;`, which is the original, leaking
exception. The Javadoc says the result is "never another type", so this is unreachable today. But the
void-then-`throw e` idiom fails open, and a later edit to `sameKindStandIn` (for instance a new branch
returning a checked type) would silently re-open the leak instead of failing.
(b) `withheldJacksonChain` decides `holdsJackson` by walking `getCause()` only, and keeps the bottom run
of non-Jackson `IOException`s as the same objects. Suppressed exceptions on those kept objects, or on a
chain with no cause-link Jackson, are never inspected. A Jackson exception suppressed onto a kept
`IOException` (for example by a try-with-resources close inside a serializer) prints with its message
in `exception.stacktrace`. `WithheldMessageException` drops suppressed exceptions by design; the
rebuild does not. This contradicts the FORK-DELTA statement "no Jackson residual remains".
**Fix:** Make the helper return what to throw, so there is no fall-through to the raw exception:

```java
private static <E extends Exception> E valueFree(E e, String topText) {
    Exception rebuilt = withheldJacksonChain(e, topText);
    return rebuilt == null ? e : (E) rebuilt;   // callers: throw valueFree(e, text);
}
```

Also cover `getSuppressed()` in the `holdsJackson` scan, and either wrap the kept suffix links
(`clearSuppressed`) or state the residual in FORK-DELTA and Known costs item 6.

### WR-03: Retry-classification knowledge is duplicated between `Utils.handleException` and the writer rebuild, with no test tying them together

**File:** `src/main/java/com/clickhouse/kafka/connect/sink/db/ClickHouseWriter.java:735-800`
(`Utils.java:84-150`)
**Issue:** The rebuild encodes what `handleException` reads: root `SocketTimeout`, `UnknownHost` and
`Socket` kinds (kept only because they are `IOException`s in the trusted suffix), the timeout prefix
at index 0, server-error codes on `ClickHouseException` and `ServerException`, `RetriableException`
anywhere, and `DataException` at the top. A non-IO root below a Jackson link is flattened to a
plain `RuntimeException` by `sameKindStandIn(root, ..., null)`. If `handleException` later reads
another root type or message fact, the rebuild drops it silently on a Jackson chain. Both
`classifiedKinds` and `rootFacts` in the tests are copies of the same knowledge, so they cannot detect
this drift either. The coupling exists only through the two public constants.
**Fix:** Keep one source of truth. Put the "classification-relevant facts of a chain" in `Utils`
(for example `Utils.retryRelevantFacts(Throwable)`). Have both the rebuild and the tests assert
`facts(original).equals(facts(rebuilt))` through it. At minimum, add a comment in `handleException`
pointing at `sameKindStandIn` and `withheldJacksonRoot`.

### WR-04: The new tests copy the same helper block three times and re-implement the production oracle

**File:** `src/test/java/com/clickhouse/kafka/connect/sink/db/JacksonOverIoValueFreeUnitTest.java:125-344`,
`.../JsonRouteValueFreeUnitTest.java:152-494`, `.../WriterRecordContextUnitTest.java:71-406`
**Issue:** `Outcome`, `outcome(...)`, `classifiedKinds`, `rootFacts`, `isTrustedIoLink`,
`trustedSuffixStart`, `anyMessageContains`, `withheld` and the V2 wrapper simulation are copied
nearly verbatim across the three classes. AGENTS.md says "DRY" and "Test utilities" belong in shared
helpers. The same-definition problem is worse: `isTrustedIoLink` and `trustedSuffixStart` in the tests
reimplement production's own definition of the trusted suffix, so a mistake in the definition passes
both sides. The "client V2 wraps it exactly as before" claim is pinned only by a simulated wrapper
(`ScriptedWriteFailureWriter.V2_INSERT_FAILED_MESSAGE`) applied to `RuntimeException`s, not by the real
V2 client. The live `WriterErrorPathLiveTest` is the only real check.
**Fix:** Move the duplicated helpers into one test utility next to `ThrowableTexts` (for example
`FailureClassification`) and delete the copies. State the expected suffix explicitly per shape
instead of recomputing it with production's rule.

## Info

### IN-01: FORK-DELTA over-states two properties of the rebuild

**File:** `docs/FORK-DELTA.md:47` and `docs/FORK-DELTA.md:227`
**Issue:** The row says "every link above becomes a stand-in of the same kind". In the code, Jackson
links that sit above the root are dropped from the rebuilt chain (`ClickHouseWriter.java:750-755`
skips them), so the chain gets shorter; the text does not say so. "so no Jackson residual remains" and
"no Jackson chain is left alone" ignore suppressed exceptions on the kept suffix (WR-02b). Neither
changes retry behaviour.
**Fix:** Add "Jackson links above the root are removed" to the row. Qualify the closing sentence
("no Jackson link remains in the cause chain") unless WR-02b is fixed.

### IN-02: `buildValueConversionFailureMessage` runs on every pass-through exception

**File:** `src/main/java/com/clickhouse/kafka/connect/sink/db/ClickHouseWriter.java:1082-1084`
**Issue:** `throwRebuiltJacksonChain(e, buildValueConversionFailureMessage(...))` formats the top text
eagerly for every pass-through failure, including plain timeouts and connector-thrown
`ConnectException`s that hold no Jackson link and discard the text. It is the failure path only, so
the cost is small, but it contradicts "the success path stays free of extra work" in spirit and
formats a string that is never used in the common case.
**Fix:** Pass a `Supplier<String>`, or compute the text inside `throwRebuiltJacksonChain` only after
`holdsJackson` is true.

### IN-03: The "message withheld" note exists in two constants that must stay equal

**File:** `src/main/java/com/clickhouse/kafka/connect/sink/db/ClickHouseWriter.java:607-609`,
`src/main/java/com/clickhouse/kafka/connect/util/WithheldMessageException.java` (`WITHHELD_NOTE`)
**Issue:** `WITHHELD_LINK_NOTE` (with a leading space) and `WithheldMessageException.WITHHELD_NOTE`
carry the same text. The comment says they must read alike, but no test ties them together. The tests
also hard-code the string a third and fourth time.
**Fix:** Expose the note from `WithheldMessageException` (public constant or a `withheldText(Class)`
helper) and use it from the writer and the tests.

### IN-04: A failing RowBinary column logs two ERROR lines

**File:** `src/main/java/com/clickhouse/kafka/connect/sink/db/ClickHouseWriter.java:1080-1082, 1562-1568`
**Issue:** `doWriteColValue` logs `Error writing value of ... exception <class>` and then
`writeColumnWithRecordContext` logs `Record serialization failed: topic ... column ...`. Both are
value-free, but a bad batch under `errors.tolerance=all` produces two ERROR lines per failing record
with the same exception class. The second line carries the coordinates the first lacks, so this is
noise rather than a defect.
**Fix:** Merge the coordinates into one line, or log the first at DEBUG.

### IN-05: Edited upstream tests weaken existing assertions

**File:** `src/test/java/com/clickhouse/kafka/connect/sink/db/ClickHouseWriterRecordContextTest.java:45-75`,
`src/test/java/com/clickhouse/kafka/connect/sink/db/FieldDescriptorEscapeTest.java:56-58`
**Issue:** AGENTS.md says "Existing tests: Only add new tests, never delete/weaken existing ones".
`serializationFailureNamesTopicPartitionOffsetAndColumn` no longer asserts that the thrown message
names topic, partition and offset. It asserts them on the log line instead.
`preservesEscapedCommentsAndDefaultExpressions` now expects the literal JSON text. Both edits are
recorded as user decisions in the new FORK-DELTA "Tests changed by a user decision" row and are pinned
by new tests, so this is informational. The first edit still asserts `message.contains("`" +
COLUMN_NAME + "`")` on the thrown exception, which is the new `doWriteColValue` text and not the
record-context layer.
**Fix:** None required. Keep the named-edit row in FORK-DELTA current when upstream's tests change
again.

---

_Reviewed: 2026-11-04_
_Reviewer: Claude (gsd-code-reviewer)_
_Depth: standard_
