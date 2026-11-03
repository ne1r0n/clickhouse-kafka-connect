---
phase: 41-bounded-insert-timeout
fixed_at: 2026-11-02T11:10:00Z
review_path: /Users/alexandr.chashnikov/repo/other/clickhouse-kafka-connect_my/.planning/phases/41-bounded-insert-timeout/41-REVIEW.md
iteration: 1
findings_in_scope: 3
fixed: 3
skipped: 0
status: all_fixed
---

# Phase 41: Code Review Fix Report

**Fixed at:** 2026-11-02T11:10:00Z
**Source review:** /Users/alexandr.chashnikov/repo/other/clickhouse-kafka-connect_my/.planning/phases/41-bounded-insert-timeout/41-REVIEW.md
**Iteration:** 1

**Summary:**
- Findings in scope: 3 (WR-01, WR-02, WR-03; Info out of scope)
- Fixed: 3
- Skipped: 0

## Fixed Issues

### WR-01: Insert-timeout documentation describes a mechanism that is a no-op while `async=false`

**Files modified:** `src/main/java/com/clickhouse/kafka/connect/sink/db/ClickHouseWriter.java`, `docs/FORK-DELTA.md`
**Commit:** 459354fc
**Applied fix:** Javadoc-only change in `ClickHouseWriter.awaitInsertResponse` (no code change, no planning IDs): with `async=false` the future is already complete, so `future.get` only bounds `async=true`; the default-path bound is the per-insert `socket_timeout` from `ClickHouseHelperClient.setInsertSocketTimeoutV2`, an inactivity bound and not a total-time cap. The FORK-DELTA row no longer says "`awaitInsertResponse` is unchanged" bare; it says the code is unchanged and states the Javadoc meaning. Known cost 14 now records the stale `clickhouseClientInsertTimeoutMs` help text in `ClickHouseSinkConfig` as a known residual. `ClickHouseSinkConfig.java` was NOT edited (empty diff against `69d5da3a`).

### WR-02: `setInsertSocketTimeoutV2` is public and only clamps the upper bound

**Files modified:** `src/main/java/com/clickhouse/kafka/connect/sink/db/helper/ClickHouseHelperClient.java`, `src/test/java/com/clickhouse/kafka/connect/sink/db/helper/InsertSocketTimeoutOptionUnitTest.java`
**Commit:** e49be8c0
**Applied fix:** Added `|| timeoutMs <= 0` to the early return, so 0 or a negative value leaves the insert settings untouched (never an accidental unbounded value). Valid values, the `Integer.MAX_VALUE` clamp and the operator override (operator `socket_timeout=0` still wins as unbounded) are unchanged. New parameterized test `aNonPositiveInsertTimeoutAddsNoPerInsertSocketTimeout` for `0`, `-1`, `Long.MIN_VALUE`. The guard is a plain bound check covered directly by the new cases.

### WR-03: The operator-override guard's parse edge cases are mostly unpinned

**Files modified:** `src/test/java/com/clickhouse/kafka/connect/sink/db/helper/InsertSocketTimeoutOptionUnitTest.java`
**Commit:** d6a09dcb
**Applied fix:** Test-only, parser behaviour unchanged. Per-insert-expected cases added: `socket_timeout_x=5`, `x_socket_timeout=5`, `socket%5Ftimeout=0`. Operator-wins cases added: `&socket_timeout=0` (client value 0) and `socket_timeout=0&socket_timeout=5` (client value 5, the last pair wins). New `anUpperCaseSocketTimeoutKeyIsNotAClientSocketTimeout` asserts the client-level `socket_timeout` for `SOCKET_TIMEOUT=5` equals the client default.

Current-behaviour findings pinned that differ from the review's expectation: the review proposed a valueless `socket_timeout`, `socket_timeout=` and ` socket_timeout=5` as plain guard cases. Measured today, none of them reaches the guard, because the helper client cannot be built with them: `socket_timeout` (valueless, read as `true`) and `socket_timeout=` throw `NumberFormatException`, and ` socket_timeout=5` throws `IllegalArgumentException` ("Invalid URI"). They are pinned by `aSocketTimeoutPairWithoutANumberFailsHelperConstruction` and `aKeyWithSurroundingWhitespaceFailsHelperConstruction`, which also means those forms cannot silently override an operator value.

## Verification

- Where run: the isolated review-fix worktree (`.claude/worktrees/rf-41-...`) on temp branch `gsd-reviewfix/41-77361`, then fast-forwarded into `sync/v1.7.0`. Results are reproducible from the main checkout at the final tip.
- Scoped Spotless IDE hook: `InsertSocketTimeoutOptionUnitTest.java` IS CLEAN; `ClickHouseHelperClient.java` and `ClickHouseWriter.java` IS DIRTY as at baseline, diff against the formatted output is import and blank lines only, so no imports were reordered.
- `fork-anchors.txt`: not edited. No anchored line changed (the `settings.setNetworkTimeout(` count stays 1, `operatorSetSocketTimeout` stays 3, the writer call-site row stays 3). `41-evidence-check.py anchors ... --rows 184` printed `rows=184 bad=0`. The 40-minute `verify-merge-invariants.sh` was not run and does not need a re-run for an anchor change; it was not re-run for these fixes (report only).
- `./gradlew testUnit --rerun` (Docker-free, `-PjavaVersion=21`): 1757 tests, 1755 succeeded, 0 failed, 2 skipped (baseline 1745 + 12 new cases).
- Full suite `./gradlew test --rerun` on ClickHouse `latest` x client V2 (`-PjavaVersion=21`, colima profile `vz`): 2355 tests, 2337 succeeded, 0 failed, 18 skipped (baseline 2343 + 12 new cases).
- No OpenJ9 crash seen (HotSpot 21 used as in the plans).
- `ClickHouseSinkConfig.java`: empty diff against `69d5da3a`.

## Notes for the orchestrator

- `.planning/config.json` and `.planning/state.json` were left uncommitted and untouched. Nothing was pushed. This file was not committed.
- Info findings IN-01 to IN-03 are out of scope and untouched.

---

_Fixed: 2026-11-02T11:10:00Z_
_Fixer: Claude (gsd-code-fixer)_
_Iteration: 1_
