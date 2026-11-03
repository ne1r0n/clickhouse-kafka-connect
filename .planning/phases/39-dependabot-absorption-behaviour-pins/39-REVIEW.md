---
phase: 39-dependabot-absorption-behaviour-pins
reviewed: 2026-11-01T00:00:00Z
depth: standard
files_reviewed: 10
files_reviewed_list:
  - src/test/java/com/clickhouse/kafka/connect/sink/WrappedDataExceptionClassificationPinUnitTest.java
  - src/test/java/com/clickhouse/kafka/connect/sink/db/helper/ClientEndpointAndOptionsPinUnitTest.java
  - src/integrationTest/java/com/clickhouse/kafka/connect/sink/helper/ConfluentPlatform.java
  - src/integrationTest/java/com/clickhouse/kafka/connect/sink/ClickHouseSinkConnectorIntegrationTest.java
  - gradle/libs.versions.toml
  - .github/workflows/tests.yaml
  - .github/workflows/nightly-integration-tests.yaml
  - .github/workflows/release.yaml
  - .github/workflows/pre-release.yaml
  - docs/FORK-DELTA.md
findings:
  critical: 0
  warning: 2
  info: 3
  total: 5
status: issues_found
---

# Phase 39: Code Review Report

**Reviewed:** 2026-11-01T00:00:00Z
**Depth:** standard
**Files Reviewed:** 10
**Status:** issues_found

## Summary

The diff base is `8d9d1a17` and Phase 39 changed no `src/main`. I found no blockers.

- **Workflow and catalog bumps:** All four workflows move `setup-gradle` to `8da46b53...`. I checked against the GitHub API that this is the peeled commit of the annotated tag `v6.4.1`. No `v6.4.0` reference is left in `.github/`. The gson bump is a one-line change to `libs.versions.toml`, and the catalog row in `FORK-DELTA.md` matches it.
- **PIN-02 test:** The matrix is consistent with `ProxySinkTask.put`. Conversion runs outside the `try`, so a conversion `DataException` escapes raw. Writer failures are wrapped by `Processing` before `Utils.handleException`. The catch ordering (`RetriableException`, then `DataException`, then `RuntimeException`) classifies correctly.
- **PIN-03 test:** It is gated by `ClientErrorShapes.line()`, so an unknown client line fails loudly.
- **PIN-04 harness:** The scoped counting is correct. The weaknesses are in diagnostics and docs, not in the counting logic.

## Warnings

### WR-01: Diagnostic log lines in two integration tests now fail the test on a non-200 status response

**File:** `src/integrationTest/java/com/clickhouse/kafka/connect/sink/ClickHouseSinkConnectorIntegrationTest.java:172` (also `:184`)
**Issue:** The old code logged `confluentPlatform.getConnectors()`, which swallowed `IOException` and returned `""`. It is replaced by `getConnectorTasksStatus(SINK_CONNECTOR_NAME)`, which throws `IOException` on any non-200 response (`ConfluentPlatform.java`, `getConnectorTasksStatus`). The call sits between `waitWhileCounting` and the row-count assertion, so it only produces a log line. A transient 404 or 5xx from the Connect REST endpoint during a rebalance now fails `stockGenMultiTaskTopicTest` and `stockGenMultiTaskSchemalessTest` before the assertion that matters is reached. This works against the phase's goal of making the nightly less flaky.
**Fix:** Keep the log purely diagnostic:
```java
private String sinkTaskStatusForLog() {
    try {
        return confluentPlatform.getConnectorTasksStatus(SINK_CONNECTOR_NAME).toString();
    } catch (IOException e) {
        return "unavailable: " + e.getMessage();
    }
}
...
LOGGER.info("Sink connector task status: {}", sinkTaskStatusForLog());
```

### WR-02: `ROLLOUT-v1.7.0_gg6.md` row D7 now states the opposite of the repository

**File:** `docs/ROLLOUT-v1.7.0_gg6.md:146` (How cell of row D7)
**Issue:** The row says "no test in the repository asserts the forwarding, so a later client bump could change it unnoticed". Phase 39 adds `ClientEndpointAndOptionsPinUnitTest.customKeysAreForwardedWithDoubledEncodingClientVersionTripwire`, which does assert the forwarding. The test's own `CLIENT_VERSION_TRIPWIRE` message sends maintainers to this row, so a reader landing there is told the pin does not exist. `FORK-DELTA.md` was updated for the gson, setup-gradle, 15 and 17 rows, but this row was missed.
**Fix:** If the rollout note is meant to stay a snapshot of the release tip, add a short pointer, for example "since Phase 39 `ClientEndpointAndOptionsPinUnitTest` pins the forwarding". Otherwise reword the sentence.

## Info

### IN-01: Topic name literal duplicated instead of reusing the local variable

**File:** `src/integrationTest/java/com/clickhouse/kafka/connect/sink/ClickHouseSinkConnectorIntegrationTest.java:~210`
**Issue:** `datagenCompletionCountIgnoresLeftoverConnectorTest` creates the topic from the variable `afterTopic`, then calls `generateData("datagenAfterLeftoverTopic", ...)` with a second copy of the string. Renaming one copy would produce a produce-to-missing-topic failure.
**Fix:** `generateData(afterTopic, 1, recordsPerTask)`.

### IN-02: `assertOwnDatagenTaskCount` hard-codes partition counts in its failure message

**File:** `src/integrationTest/java/com/clickhouse/kafka/connect/sink/ClickHouseSinkConnectorIntegrationTest.java:~226-229`
**Issue:** The message computes `datagenTaskCount(1) + datagenTaskCount(2)` independently of the `leftoverPartitions` and `secondPartitions` locals. If those values change, the diagnostic misreports the cross-connector total. `deleteConnectors` also ignores the HTTP status (`ConfluentPlatform.java:~406`), so the "cannot leak" claim in the `generateData` comment holds only for IO failures, not for a 4xx or 5xx on delete.
**Fix:** Pass the total as a parameter, or sum the two task counts already in scope. Optionally assert a 2xx or 404 in `deleteConnectors`.

### IN-03: Misleading `_CONTROL` name and an unmeasured branch in the PIN tests

**File:** `src/test/java/com/clickhouse/kafka/connect/sink/WrappedDataExceptionClassificationPinUnitTest.java:75-76`; `src/test/java/com/clickhouse/kafka/connect/sink/db/helper/ClientEndpointAndOptionsPinUnitTest.java:130-134`
**Issue:** `UNKNOWN_HOST_ROOT_CONTROL` and `RUNTIME_EXCEPTION_SOCKET_TIMEOUT_CONTROL` expect `RETRIABLE`, the same outcome as the shape they are supposed to be controls for. They pin additional retriable roots, not contrasts, so the name suggests a negative case that is not there. In the PIN-03 test, the 0.9.6 `else` branch is dead on the pinned client (0.11.0). Its comment says "measured in Phase 36 E-11, not re-measured here", which cites a `.planning` artifact from test code.
**Fix:** Rename the shapes to `*_ROOT` and drop the planning-artifact reference from the comment, or leave a plain note that the branch is unmeasured.

---

_Reviewed: 2026-11-01T00:00:00Z_
_Reviewer: Claude (gsd-code-reviewer)_
_Depth: standard_
