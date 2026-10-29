---
phase: 35-buffered-offset-commit-under-topic-renaming-smts
fixed_at: 2026-10-29T00:00:00Z
review_path: .planning/phases/35-buffered-offset-commit-under-topic-renaming-smts/35-REVIEW.md
iteration: 1
findings_in_scope: 2
fixed: 2
skipped: 0
status: all_fixed
---

# Phase 35: Code Review Fix Report

**Fixed at:** 2026-10-29
**Source review:** .planning/phases/35-buffered-offset-commit-under-topic-renaming-smts/35-REVIEW.md
**Iteration:** 1

**Summary:**
- Findings in scope: 2 (Critical and Warning only)
- Fixed: 2
- Skipped: 0
- Left open by scope: IN-01 to IN-05

Verification ran in the main checkout (`workflow.use_worktrees` was off for this run). The mutation proof for WR-01 ran in a throwaway `git worktree` that was removed afterwards, so it is not reproducible from the main tree.

## Fixed Issues

### WR-01: No test pins `originalKafkaPartition()` at the two strategy sites, so a partition-only regression passes

**Files modified:** `src/test/java/com/clickhouse/kafka/connect/sink/BufferedOffsetCommitUnitTest.java`
**Commit:** b50b8452
**Applied fix:** Added `closeAndPutKeyTheBufferByTheOriginalPartitionWhenTheSmtMovesThePartition`, parameterised over `StrategyKind`, plus a `movedRecords` helper and three constants. Two original partitions (1 revoked, 2 kept) are moved onto transformed partition 0 through `RenamedTopicRecords.withTransformedPosition`, with unique transformed offsets. The test buffers both, calls `close` on original partition 1, completes a chunk of partition 2, and asserts that exactly `bufferCount` records were written, all from original partition 2, and that `preCommit` reports only original partition 2 at the original offset plus one. No existing test or fixture was edited; the class went from 13 to 15 executions.

Mutation proof (throwaway worktree at the pre-fix HEAD plus the new test file; only the partition argument reverted from `originalKafkaPartition()` to `kafkaPartition()` in `AtLeastOnceBufferStrategy.close` and `ExactlyOnceBufferStrategy.put`):
- The 2 new executions failed. At-least-once failed on the written-record count and exactly-once on the written partitions (`expected: <[2]> but was: <[1, 2]>`).
- The 13 existing executions stayed green.
- On the unmutated tree all 17 executions of `BufferedOffsetCommitUnitTest` and `ProcessingRenamedTopicStateKeyUnitTest` pass.
- The worktree was removed and `src/main` is unchanged in the main tree.

### WR-02: Rollout wording misdescribes the failure on a runtime that lacks the new accessors

**Files modified:** `docs/FORK-DELTA.md`, `.planning/phases/35-buffered-offset-commit-under-topic-renaming-smts/35-ROLLOUT-ITEM.md`
**Commit:** b232c205
**Applied fix:** Wording-only option, as instructed. `ChunkFlusher.flush` was not reordered and no `start()` probe was added. The FORK-DELTA Phase 35 row (last cell) and the rollout item's "Minimum Connect runtime" section now say that exactly-once fails with `NoSuchMethodError` on the first `put()`, and that at-least-once fails after the first chunk's insert (the error is not a `RuntimeException`, so `put()` does not catch it and the chunk is redelivered as a duplicate). Both say that only runtimes below the supported floor are affected and that BLD-16 guards the floor in CI. The rollout item keeps its "derived, not measured" label.

Guard checks: neither `.planning/scripts/fork-anchors.txt` nor `verify-merge-invariants.sh` references `docs/FORK-DELTA.md` or the rollout item. The anchors mirror reads `rows=148 bad=0` after the edit. `ForkCoverageScopeUnitTest`, `DependabotProtobufIgnoreGuardUnitTest` and `ConnectApiReferencesUnitTest` pass (36 executions in the combined run, 0 failed).

## Verification summary

| Check | Result |
|-------|--------|
| `testUnit` for `BufferedOffsetCommitUnitTest` and `ProcessingRenamedTopicStateKeyUnitTest` | 17 executions, 0 failed |
| Plus `ForkCoverageScopeUnitTest`, `DependabotProtobufIgnoreGuardUnitTest`, `ConnectApiReferencesUnitTest` | 36 executions, 0 failed |
| `spotlessJavaCheck` (import order and unused imports only in this build) | no violation reported for the changed file; the task fails on other, pre-existing files |
| `35-hygiene-check.py` over the three changed files | 0 hits |
| Anchors mirror | `rows=148 bad=0` |

Not staged: `.planning/config.json`, `.planning/state.json`, `.planning/STATE.md`. Nothing pushed.

---

_Fixed: 2026-10-29_
_Fixer: Claude (gsd-code-fixer)_
_Iteration: 1_
