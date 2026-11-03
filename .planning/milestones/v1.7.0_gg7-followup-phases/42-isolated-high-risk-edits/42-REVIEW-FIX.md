---
phase: 42-isolated-high-risk-edits
fixed_at: 2026-11-02T00:00:00Z
review_path: .planning/phases/42-isolated-high-risk-edits/42-REVIEW.md
iteration: 1
findings_in_scope: 1
fixed: 1
skipped: 0
status: all_fixed
---

# Phase 42: Code Review Fix Report

**Fixed at:** 2026-11-02
**Source review:** .planning/phases/42-isolated-high-risk-edits/42-REVIEW.md
**Iteration:** 1

**Summary:**
- Findings in scope: 1 (WR-01 only, as instructed; the four Info findings are out of scope)
- Fixed: 1
- Skipped: 0

## Fixed Issues

### WR-01: New cause-chain loop in `handleException` does not terminate on a cyclic cause chain that contains a ClickHouse server error

**Files modified:** `src/main/java/com/clickhouse/kafka/connect/util/Utils.java`, `src/test/java/com/clickhouse/kafka/connect/sink/HandleExceptionCauseCycleUnitTest.java` (new), `docs/FORK-DELTA.md`
**Commit:** 1c0d9d00 (on `sync/v1.7.0`, fast-forwarded from the isolated worktree; not pushed)
**Applied fix:** The cause-chain loop added in `c8574890` now keeps an identity set of visited causes and stops the walk at the first repeated cause. The `for (Throwable t = e; t != null; t = t.getCause())` line is byte-identical, so the guard is `visited.add(t)` plus a `break` inside the loop. A cyclic chain therefore falls through to the DataException and tolerance handling, the outcome before the chain loop existed. No retriable rule, other branch, log line or comment-ID changed; the only import additions are `java.util.Collections` and `java.util.IdentityHashMap`. The production diff is 7 added lines in `Utils.java`.

New test class `HandleExceptionCauseCycleUnitTest` (Docker-free, untagged, so it runs in `testUnit`) builds a two-node cycle (`RuntimeException` whose cause is a non-retriable `ServerException` code 60 that names the wrapper as its cause) and asserts under `assertTimeoutPreemptively` (10 s):
- `errors.tolerance=none`: a plain `RuntimeException` wrapping the input with message `Number of records: 3` (the same outcome as before the chain loop existed), not a `RetriableException`.
- `errors.tolerance=all`: returns without throwing.

Red check: with the guard reverted in the worktree (`git checkout -- Utils.java`), both cases fail with `execution timed out after 10000 ms`; with the guard restored both pass. No existing test file was edited.

**Status:** fixed. The finding is a termination defect, not a classification-logic change; the retriable classification itself is untouched.

**Revert unit (separate note, RSK-02 only):** the revert unit of the RSK-02 edit is now commit `c8574890` together with this fix commit `1c0d9d00`. A revert of `c8574890` alone conflicts with the guard. The same sentence is recorded in the RSK-02 row of `docs/FORK-DELTA.md` (a separate sentence from the RSK-01 and RSK-03 text, which was not touched). The RSK-01 and RSK-03 revert units are unchanged.

## Verification

Where it ran: in the isolated worktree `.claude/worktrees/rf-42-56021-1793627962` (branch `gsd-reviewfix/42-56021` from `5ff16fea`), with `-PjavaVersion=21`, `env -u TESTCONTAINERS_HOST_OVERRIDE`, under `/bin/bash`. The worktree was fast-forwarded into `sync/v1.7.0` and removed afterwards, so these runs are not reproducible from a leftover tree; re-run the commands on `sync/v1.7.0` at `1c0d9d00` to reproduce.

- Spotless: scoped IDE-hook check (no write mode) on `Utils.java` and `HandleExceptionCauseCycleUnitTest.java`: both `IS CLEAN`.
- `testUnit --rerun` with Docker unreachable (`DOCKER_HOST=unix:///nonexistent/docker.sock`): 1824 tests, 1822 succeeded, 0 failed, 2 skipped (baseline 1822 + 2 new, 2 skips). BUILD SUCCESSFUL.
- Full suite `test --rerun` on ClickHouse `latest` x client V2 (Docker via colima profile `vz`): 2423 tests, 2405 succeeded, 0 failed, 18 skipped (baseline 2421 + 2 new, 18 skips). BUILD SUCCESSFUL in 2m 40s. The skips helper reported `named=7 state_mismatch_cases=11 unexpected=0`.
- Per-testcase status delta against the E-10 gate dumps (`/tmp/gsd42/exec/status-gate3-testunit.tsv` and `status-gate3-full.tsv`), helper `42-testcase-status.py delta`: `removed=0 added=2 expected=2 match=yes` for both runs. The two added rows are the two `HandleExceptionCauseCycleUnitTest` cases, both PASS; every pre-existing testcase kept its status.
- `SourcePlanningIdGuardUnitTest` passes (no planning ID in `src/main`).
- No crash file (`core*`, `javacore*`, `Snap*`, `hs_err*`) appeared in the worktree root.

## Anchors

No anchored line changed. The two RSK-02 anchors in `.planning/scripts/fork-anchors.txt` (`for (Throwable t = e; t != null; t = t.getCause())` and `t instanceof RetriableException`, count 1 each) still match, and `fork-anchors.txt` was not edited. An ad-hoc re-count of every anchor row against the new tree read `rows=190 bad=0`. The `verify-merge-invariants.sh` 40-minute script was not re-run, as instructed; since no anchored line changed, no anchor row needs updating, but a re-run of that script remains the authoritative check before any push.

## Constraints check

`ClickHouseSinkConfig.java`, `sink/state`, `sink/kafka`, `Processing.java` and `ClickHouseSinkTask.java` are unchanged by this commit (the commit holds exactly three paths). `.planning/config.json` and `.planning/state.json` were left uncommitted. Nothing was pushed. REVIEW-FIX.md is not committed.

---

_Fixed: 2026-11-02_
_Fixer: Claude (gsd-code-fixer)_
_Iteration: 1_
