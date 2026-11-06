---
phase: 40-helper-client-closing
fixed_at: 2026-11-02T09:50:00Z
review_path: .planning/phases/40-helper-client-closing/40-REVIEW.md
iteration: 1
findings_in_scope: 3
fixed: 3
skipped: 0
status: all_fixed
---

# Phase 40: Code Review Fix Report

**Fixed at:** 2026-11-02
**Source review:** .planning/phases/40-helper-client-closing/40-REVIEW.md
**Iteration:** 1

**Summary:**
- Findings in scope: 3 (critical and warning; the three Info findings IN-01 to IN-03 are out of scope)
- Fixed: 3
- Skipped: 0

## Fixed Issues

### WR-01: A throwing per-batch helper `close()` now fails the insert batch

**Files modified:** `src/main/java/com/clickhouse/kafka/connect/sink/db/ClickHouseWriter.java`, `src/test/java/com/clickhouse/kafka/connect/sink/db/helper/WriterHelperClientClosingUnitTest.java`, `docs/FORK-DELTA.md`, `.planning/scripts/fork-anchors.txt`
**Commit:** e7c4958d
**Applied fix:** `closeHelperClient()` now delegates to a new private static `closeQuietly(ClickHouseHelperClient)` that closes and WARN-logs a failure with the existing constant message. `getMutationRequest` no longer uses try-with-resources: it reads the `ClickHouseNode` in a `try` and releases `chcTmp` through `closeQuietly` in the `finally`, so a failed close cannot fail an insert batch. New case `clientV1BatchStillInsertsWhenClosingItsPerBatchHelperClientThrows` scripts a throwing close on the per-batch helper and asserts the batch is still posted with the right body. The exactly-once state paths are untouched. FORK-DELTA's writer row now says a failed close, of `chc` or of the per-batch client, is logged through one shared method and never fails `stop()` or a batch. The anchors row `try (ClickHouseHelperClient chcTmp = createHelperClient(` (count 1) became `closeQuietly(chcTmp)` (count 1), so the mirror stays at `rows=180 bad=0`.

### WR-02: `KeeperStateProvider.close()` can throw, and `ProxySinkTask.stop()` then loses the primary exception

**Files modified:** `src/main/java/com/clickhouse/kafka/connect/sink/ProxySinkTask.java`, `src/test/java/com/clickhouse/kafka/connect/sink/ProxySinkTaskStopUnitTest.java`, `docs/FORK-DELTA.md`
**Commit:** 53c0f06b
**Applied fix:** Took the review's second option (accumulate in `ProxySinkTask.stop()`), because it also covers a throwing `unregisterMBean`, which the swallow-in-`close()` option would not. The nested `finally` chain became three guarded steps (writer stop, MBean unregister, provider close), each catching `RuntimeException | Error`. The first failure is kept and later ones are attached with `addSuppressed` through a private `keepFirst` helper; the first failure is rethrown with its original type. Catching `Error` as well keeps the earlier property that the later steps run for any `Throwable`. `KeeperStateProvider` is not modified, so `close()`, `getStateRecord`, `setStateRecord`, both constructors and `init()` are byte-identical to before. Two new cases in `ProxySinkTaskStopUnitTest`: a throwing provider close reaches the caller after the writer stopped and the MBean went away, and writer plus provider both failing throws the writer's exception with the close failure as suppressed. The existing `assertSame(writer.lastThrown, thrown)` case stays green. FORK-DELTA's `ProxySinkTask.stop()` clause now says "first exception thrown reaches the caller, later ones attached as suppressed". The `stateProvider.close()` anchors row (count 1) still holds. Status note: this is a control-flow change in `stop()`; the logic is simple and pinned by the new cases, but a maintainer should confirm the "first failure wins" choice is the intended one.

### WR-03: `ClickHouseWriterTest.runWithWriter` leaks its `seedClient`

**Files modified:** `src/test/java/com/clickhouse/kafka/connect/sink/db/ClickHouseWriterTest.java`
**Commit:** 1d5e943d
**Applied fix:** The method body now runs inside `try (ClickHouseHelperClient seedClient = ClickHouseTestHelpers.createClient(props))`, so the seed client closes on every exit, including a failed seed table creation. The inner `try/finally` for the writer stop and the seed table drop is unchanged.

## Verification

- **Where it ran:** the isolated worktree `.claude/worktrees/rf-40-10089-1793604713`, on the temporary branch `gsd-reviewfix/40-10089`, with Docker through colima profile `vz` and `TESTCONTAINERS_HOST_OVERRIDE` unset. The numbers are reproducible from the main checkout once the branch is fast-forwarded, because the worktree has the same tracked tree.
- `./gradlew testUnit --rerun` (Docker-free): 1719 tests, 1717 succeeded, 0 failed, 2 skipped. Baseline 1716 plus the 3 new cases.
- Targeted classes run before commit: `WriterHelperClientClosingUnitTest` 23 of 23 passed; `ProxySinkTask*` and `KeeperStateProviderClosingUnitTest` 28 of 28 passed.
- `./gradlew test --rerun` (ClickHouse `latest`, client V2): 2317 tests, 2299 succeeded, 0 failed, 18 skipped. Baseline 2314 plus the 3 new cases. No JVM crash, so `-PjavaVersion=21` was not needed and no core, javacore, Snap or jitdump files were produced.
- Anchors mirror: `40-evidence-check.py anchors .planning/scripts/fork-anchors.txt --rows 180` printed `rows=180 bad=0`.
- Scoped per-file Spotless IDE hook: `ProxySinkTask.java`, `ProxySinkTaskStopUnitTest.java`, `WriterHelperClientClosingUnitTest.java` report IS CLEAN. `ClickHouseWriter.java` and `ClickHouseWriterTest.java` report IS DIRTY as at HEAD, and the hook output differs from the file only in import and blank lines, so neither was reordered or rewritten.
- `ClickHouseWriterTest` runs against Docker as part of the full suite above, which covers the WR-03 edit.

---

_Fixed: 2026-11-02_
_Fixer: Claude (gsd-code-fixer)_
_Iteration: 1_
