---
phase: 41-bounded-insert-timeout
reviewed: 2026-11-02T10:30:00Z
depth: standard
files_reviewed: 6
files_reviewed_list:
  - src/main/java/com/clickhouse/kafka/connect/sink/db/ClickHouseWriter.java
  - src/main/java/com/clickhouse/kafka/connect/sink/db/helper/ClickHouseHelperClient.java
  - src/test/java/com/clickhouse/kafka/connect/sink/InsertStallBoundPinUnitTest.java
  - src/test/java/com/clickhouse/kafka/connect/sink/db/helper/InsertSocketTimeoutOptionUnitTest.java
  - src/test/java/com/clickhouse/kafka/connect/sink/db/helper/StubClickHouseServer.java
  - docs/FORK-DELTA.md
findings:
  critical: 0
  warning: 3
  info: 3
  total: 6
status: issues_found
---

# Phase 41: Code Review Report

**Reviewed:** 2026-11-02T10:30:00Z
**Depth:** standard
**Files Reviewed:** 6
**Status:** issues_found

## Summary

I reviewed the diff from `69d5da3a` to `HEAD` for the six files. I also ran `InsertStallBoundPinUnitTest` and `InsertSocketTimeoutOptionUnitTest`. Each run was a forced rerun with the full compile. The last run's result XML shows 15/15 and 11/11 passing, 0 failures. I read the first two runs' output only as `gradlew` stderr, which does not show test results, so I cannot vouch for those two.

The core change is small and I found it correct against client-v2 0.11.0:

- `InsertSettings.setNetworkTimeout` stores a `Long` under `socket_timeout`, in milliseconds (`Duration.of(...).toMillis()`).
- `ClientConfigProperties.SOCKET_OPERATION_TIMEOUT.getKey()` is `socket_timeout`.
- `Client.Builder.setOption` stores keys verbatim. The override guard's exact-match test therefore agrees with what the client applies, and the shared parser is a pure extraction of the old inline loop (same `?` strip, same `split("&")`, same `split("=", 2)`, same empty-key filter).
- `clickhouseClientInsertTimeoutMs` is validated `>= 100` by `ConfigDef.Range.atLeast(100)`. The 0-and-overflow cases are therefore unreachable through config, and the `Math.min` clamp covers the int overflow.
- `awaitInsertResponse` is unchanged. With the client's default `async=false` the insert runs synchronously and hands back an already-completed future. This is why `future.get(timeoutMs)` is a no-op and the per-insert socket option is the only bound. It also means there is no race between the two timeouts in the stall tests.
- The non-insert V2 sites (ping, version, `DESCRIBE`, table refresh, Keeper state queries) are untouched.

No blocker found. The findings below concern misleading documentation, an unguarded public entry point, and test gaps in the override guard. The guard is the part with the least pinning.

## Warnings

### WR-01: Insert-timeout documentation now describes a mechanism that is a no-op while `async=false`

**File:** `src/main/java/com/clickhouse/kafka/connect/sink/db/ClickHouseWriter.java:1800-1821`, `src/main/java/com/clickhouse/kafka/connect/sink/ClickHouseSinkConfig.java:838-849`
**Issue:** The `awaitInsertResponse` Javadoc and the `clickhouseClientInsertTimeoutMs` config description both say the connector "waits up to N ms for the insert response" and presents `future.get(timeout)` as the bound. After this phase the real bound on the default path is the per-insert `socket_timeout`.
- `future.get` only does work when the operator sets client `async=true`.
- `socket_timeout` is a read-inactivity timer, not a total-time timer. FORK-DELTA Known cost 14 says an insert that streams progress headers escapes it (inferred).
- The config description still advises "must stay below `max.poll.interval.ms`" as if the value were a hard wall-clock cap.

A later maintainer who sees a "redundant" `future.get` timeout, or who relies on the config text for a hard cap, would be misled. FORK-DELTA keeps the `awaitInsertResponse` Javadoc claim "unchanged" while its meaning changed.
**Fix:** Add one sentence to the `awaitInsertResponse` Javadoc and to the config description. The Javadoc should say that with `async=false` the insert returns a completed future, that the bound is the per-insert `socket_timeout` set by `ClickHouseHelperClient.setInsertSocketTimeoutV2`, and that this is an inactivity bound. If changing the config text is out of scope for the fork's "ClickHouseSinkConfig unchanged" rule, fix only the Javadoc and note the config text in Known cost 14.

### WR-02: `setInsertSocketTimeoutV2` is public and only clamps the upper bound

**File:** `src/main/java/com/clickhouse/kafka/connect/sink/db/helper/ClickHouseHelperClient.java:725-734`
**Issue:** `Math.min(timeoutMs, Integer.MAX_VALUE)` handles overflow, but 0 or a negative `timeoutMs` passes straight to `setNetworkTimeout`.
- `0` silently means "unbounded", which is the opposite of what the method promises.
- A negative value is handed to the HTTP client.

Config validation (`>= 100`) makes both unreachable today, but this is a public method on a helper class. Any future caller, or a test passing a mocked config, can get an unbounded insert with no signal. The new unit tests cover `Long.MAX_VALUE` and `null` settings but not `<= 0`.
**Fix:**
```java
if (timeoutMs <= 0) {
    return; // leave the client-level value; document that a non-positive timeout adds no bound
}
settings.setNetworkTimeout(Math.min(timeoutMs, Integer.MAX_VALUE), ChronoUnit.MILLIS);
```
Add a test for `0` and `-1`. Alternatively throw `IllegalArgumentException`.

### WR-03: The operator-override guard's parse edge cases are mostly unpinned

**File:** `src/test/java/com/clickhouse/kafka/connect/sink/db/helper/InsertSocketTimeoutOptionUnitTest.java:38-65`
**Issue:** The guard is the highest-risk part of the change, because a false negative silently overrides an operator's `socket_timeout=0`. The negative inputs cover only `""`, `ssl=true&sslmode=strict`, `SOCKET_TIMEOUT=5`, `=5`, `null` and `"   "`.
- Not covered: `socket_timeout` as a prefix or suffix of another key (`socket_timeout_x=5`, `my_socket_timeout=5`), a valueless `socket_timeout`, an empty value `socket_timeout=`, a key with surrounding whitespace, a URL-encoded key, a leading `&`, and duplicate keys (`socket_timeout=0&socket_timeout=5`).
- The `SOCKET_TIMEOUT=5` case asserts the per-insert value is set. It does not assert the client has no `socket_timeout` from that pair. Case-insensitivity of the client's key handling is therefore assumed, not pinned. I checked client-v2 0.11.0 `Client.Builder.setOption` and it stores keys verbatim, but nothing here would fail if a client upgrade changed that.
- A mutation that replaced `equals` with `startsWith`, `contains` or `equalsIgnoreCase` would pass on every listed case except `SOCKET_TIMEOUT=5` (the `equalsIgnoreCase` mutation).

**Fix:** Add to the first parameterized test (per-insert expected): `socket_timeout_x=5`, `x_socket_timeout=5`, `socket_timeout`, ` socket_timeout=5`, `socket%5Ftimeout=0`. Add to the second (operator wins): `&socket_timeout=0`, `socket_timeout=0&socket_timeout=5`, `socket_timeout=`. For the `SOCKET_TIMEOUT=5` case also assert `chc.getClient().getConfiguration()` has no `socket_timeout`.

## Info

### IN-01: Three identical two-line `InsertSettings` setups

**File:** `src/main/java/com/clickhouse/kafka/connect/sink/db/ClickHouseWriter.java:1337-1340`, `1614-1617`, `1763-1766`
**Issue:** Each V2 insert site repeats `new InsertSettings(); setDatabase; chc.setReplicaTagHeaderV2; chc.setInsertSocketTimeoutV2(...)`, and then the dedup-token block. This phase added a third copy of one line to each. AGENTS.md says DRY. A fourth site that forgets the new call would silently be unbounded, and nothing but the stall pins would catch it.
**Fix:** Extract `private InsertSettings newInsertSettings(String database, QueryIdentifier queryId)` that does all of it, and call it from the three sites. The stall pins already cover all three routes.

### IN-02: Redundant blank check in `operatorNamesSocketTimeout`

**File:** `src/main/java/com/clickhouse/kafka/connect/sink/db/helper/ClickHouseHelperClient.java:186-189`
**Issue:** The `null`/`isBlank()` early return is redundant, because `parseJdbcConnectionProperties` already returns an empty list for `null` or empty input, and a whitespace-only string yields no pair containing `=`. It also creates a small inconsistency: the parser tests `isEmpty()`, the guard tests `isBlank()`.
**Fix:** Delete the early return and loop over `parseJdbcConnectionProperties(...)` directly.

### IN-03: Stall-pin `POST` count assertion depends on the stub having recorded the request

**File:** `src/test/java/com/clickhouse/kafka/connect/sink/InsertStallBoundPinUnitTest.java:210`, `src/test/java/com/clickhouse/kafka/connect/sink/db/helper/StubClickHouseServer.java:62-67`
**Issue:** The stub records a request only after `readAllBytes()` on its handler thread. The test asserts `posts(stub).size() == 1` right after the client's 300 ms read timeout fires. The client's timer starts after it has written the body, so in practice the stub has recorded first. On a heavily loaded CI runner the margin is only the stub thread's scheduling latency, so a 0-count flake is theoretically possible. Three consecutive runs showed no flake on this machine, but only the last run's XML was inspected.
**Fix:** Widen `INSERT_TIMEOUT_MS` to about 1000 ms, which leaves the 10 s preemptive bound untouched. Alternatively, let the stub expose a latch released when a request is recorded and await it before asserting the count.

---

_Reviewed: 2026-11-02T10:30:00Z_
_Reviewer: Claude (gsd-code-reviewer)_
_Depth: standard_
