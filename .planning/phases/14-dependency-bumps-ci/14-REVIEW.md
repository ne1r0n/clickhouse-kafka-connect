---
phase: 14-dependency-bumps-ci
reviewed: 2026-10-10T00:00:00Z
depth: standard
files_reviewed: 3
files_reviewed_list:
  - .github/workflows/tests.yaml
  - gradle/libs.versions.toml
  - src/test/java/com/clickhouse/kafka/connect/sink/db/helper/Lz4RoundTripLiveTest.java
findings:
  critical: 0
  warning: 2
  info: 4
  total: 6
status: issues_found
---

# Phase 14: Code Review Report

**Reviewed:** 2026-10-10
**Depth:** standard
**Files Reviewed:** 3
**Status:** issues_found

## Summary

I reviewed the Phase 14 diff (`3c9aafde..HEAD`) for the workflow, the version catalog and the new live LZ4 test. I also cross-checked `build.gradle.kts` (the `org.lz4` capability resolution, `maxParallelForks`, the `integrationTest` suite and the Spotless config), `ClickHouseBase`, `ClickHouseTestHelpers` and `ClickHouseHelperClient`.

I found no blockers. I did not run the build or tests.

- **Catalog bumps:** They are mechanically correct. Every `version.ref` still resolves, the module coordinates are unchanged, and `lz4Java` is still selected by the `at.yawk.lz4` capability rule.
- **Workflow:** The trigger change is sound. All other workflows use `workflow_dispatch` or `schedule`, so nothing depends on tag-triggered runs.
- **LZ4 test:** It does verify the request leg. Its "both legs" claim is only partly backed by assertions (WR-01).

## Warnings

### WR-01: LZ4 test never proves the response leg was compressed

**File:** `src/test/java/com/clickhouse/kafka/connect/sink/db/helper/Lz4RoundTripLiveTest.java:115-173`
**Issue:** The `@DisplayName` and the class Javadoc claim the test proves LZ4 "on both legs". Only the request leg is asserted, through `ProfileEvents['CompressedReadBufferBlocks'] > 0`. For the response leg the test does two things:
- It checks that `getSetting('network_compression_method')` returns `lz4`. That only shows the server setting was applied.
- It reads the data back. A read-back succeeds just as well if `compressServerResponse(true)` is silently ignored, or if the server answers uncompressed. It would also pass if a future client-v2 stopped sending `compress=1`.

The test is meant to guard later lz4 and client bumps. Such a regression on the response leg (`LZ4BlockInputStream`/decompression path) would go undetected.

**Fix:** Add a server-side or wire-level assertion for the response leg. Options:
- Run a large `SELECT` with a known query id, then compare `system.query_log` `result_bytes` (uncompressed) with `NetworkSendBytes`/`HTTPConnection` ProfileEvents for that query. Assert that bytes sent are clearly smaller than `result_bytes`, because the 2.4 KB repeated payloads compress very well.
- Or put a counting proxy/Toxiproxy in front of the endpoint.

If neither is feasible, narrow the display name and Javadoc to "request leg verified, response leg verified by read-back only".

### WR-02: Fork-count step reports a recomputed value, not the one Gradle uses

**File:** `.github/workflows/tests.yaml:39-48`
**Issue:** CI-09 needs to know what `maxParallelForks` the run really used. The step re-implements the formula from `build.gradle.kts:201-205` in shell.
- It uses `nproc`. Gradle uses `Runtime.availableProcessors()`, which is cgroup/container-aware. The two can differ.
- It is a second copy of the formula, so it silently drifts if `build.gradle.kts` changes.
- It runs before any Gradle invocation. The notice therefore states a prediction, labelled as the derived value, not an observation.
- The `CLICKHOUSE_CLUSTER_NAME` branch is dead code in this workflow, because the variable is never set.

**Fix:** Have Gradle report it. For example, add `doFirst { logger.lifecycle("maxParallelForks=$maxParallelForks") }` to the `Test` task. Alternatively, parse `build/test-results` after the run, or at minimum print `java -XshowSettings:system -version` to show the JVM's CPU view next to `nproc`.

## Info

### IN-01: Concurrency group still dead-codes the pull_request branch and stays SHA-keyed

**File:** `.github/workflows/tests.yaml:18-20`
**Issue:** The workflow now triggers only on `push`, so `github.event_name == 'pull_request' && github.event.number` can never be true. Keying on `github.sha` also means that pushing one SHA to a second branch (such as the Phase 14 scratch-branch flow) cancels the first branch's in-flight run. Check results attach to the SHA, so this is mostly cosmetic.
**Fix:** Simplify to `group: ${{ github.workflow }}-${{ github.ref }}`, which cancels superseded pushes per branch, and drop the unreachable pull_request expression.

### IN-02: Test duplicates constants already exported by production code

**File:** `src/test/java/com/clickhouse/kafka/connect/sink/db/helper/Lz4RoundTripLiveTest.java:44-45`
**Issue:** `NETWORK_COMPRESSION_METHOD` and `LZ4` are redeclared. `ClickHouseHelperClient` (same package) already exposes `public static final` constants with the same names and values (`ClickHouseHelperClient.java:83-84`). This violates the project's DRY rule. The test also stays correct if production renames the key, which is arguably desirable, so decide deliberately.
**Fix:** Reference `ClickHouseHelperClient.NETWORK_COMPRESSION_METHOD` and `ClickHouseHelperClient.LZ4`, or add a one-line comment saying the independent copy is intentional.

### IN-03: Jar-origin check is a substring match on a filesystem path

**File:** `src/test/java/com/clickhouse/kafka/connect/sink/db/helper/Lz4RoundTripLiveTest.java:103-111`
**Issue:** `location.toString().contains("at.yawk.lz4")` works only while the jar sits in a Gradle or Maven cache layout where the group appears in the path. It fails for a flattened or relocated classpath (an IDE, a shaded test jar, a custom `GRADLE_USER_HOME` layout). It also false-positives if any parent directory happens to contain the string. `getCodeSource()` can be `null` for bootstrap-loaded classes, which would NPE before the `assertNotNull`.
**Fix:** Check the code source for null first. Check the jar's `META-INF/maven/at.yawk.lz4/lz4-java/pom.properties` resource, or `LZ4Factory.class.getResource(...)`. Failing that, match the `/at.yawk.lz4/` path segment explicitly.

### IN-04: Payload length is asserted inside the data-building loop

**File:** `src/test/java/com/clickhouse/kafka/connect/sink/db/helper/Lz4RoundTripLiveTest.java:84-86`
**Issue:** `assertTrue(payload.getBytes(...).length >= MIN_PAYLOAD_BYTES)` runs 2000 times inside a builder helper with no message. The constants `ROWS`, `PAYLOAD_REPEATS` and `MIN_BODY_BYTES` already guarantee the property. The later `body.length >= MIN_BODY_BYTES` assertion is the meaningful one.
**Fix:** Drop the per-row assertion, or check it once on `payload(1)` with a message.

---

_Reviewed: 2026-10-10_
_Reviewer: Claude (gsd-code-reviewer)_
_Depth: standard_
