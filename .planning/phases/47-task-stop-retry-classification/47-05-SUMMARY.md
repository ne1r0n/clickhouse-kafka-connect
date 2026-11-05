---
phase: 47-task-stop-retry-classification
plan: "05"
subsystem: documentation-and-tests
tags: [gap-closure, review-fixes, fork-delta, changelog, redelivery-pin, evidence-only-production]
status: complete

requires:
  - phase: 47-task-stop-retry-classification
    provides: 47-REVIEW.md (WR-01..WR-05, IN-01..IN-04) and plans 47-01..47-04 (E-01..E-11)
provides:
  - E-12 (FORK-DELTA and CHANGELOG stop() wording), E-13 (redelivery pin cut and mutation re-run), E-14 (item 17 qualified), E-15 (gate), evidence index E-01..E-15 every row pass
  - 47-rty01-expected-added-gap.tsv, the 108 expected rows of the renamed redelivery pin
affects: [49]

actuals:
  tokens: 13500
  tasks: 4
  commits: 8
plan_head_before: b68bcb04d4c9a4a88db9fdcf51c2790ed0b3c65f
plan_head_after: e9ccfcba5069d2284d586097cffd05ca47e2579d

tech-stack:
  added: []
  patterns: [doc edits as asserted-once old/new pairs replayed on the parent commit, mutation re-run in a removed worktree, gate on an explicit SHA with the tail gate in the background]

key-files:
  created:
    - .planning/phases/47-task-stop-retry-classification/47-rty01-expected-added-gap.tsv
  modified:
    - docs/FORK-DELTA.md
    - CHANGELOG.md
    - src/test/java/com/clickhouse/kafka/connect/sink/RetriableRedeliveryLimitPinUnitTest.java
    - .planning/phases/47-task-stop-retry-classification/47-COMMAND-EVIDENCE.md

key-decisions:
  - "No production change: src/main is byte-identical to the E-10 tip a9669f7b; the optional LOGGER.warn of WR-02 was not added"
  - "The redelivery pin runs 50 redeliveries per row and the loggers are not silenced: the cost fell about 10-fold with the count and a level change would be global to the test JVM"

requirements-completed: [LIF-01, LIF-02, RTY-01, RTY-02]

coverage:
  - id: D1
    description: "WR-01, WR-02 and IN-03: FORK-DELTA no longer names the task's stop() as an open residual, the gg8 CHANGELOG claims only that an exception reaches the caller of stop() (Connect logs a WARN), the dropped exception is stated as not suppressed and not logged, close(partitions) is in the gg8 Known Issues, item 13 says 2 of 6 cases were red; the gg7 section is byte-identical"
    requirement: LIF-02
    verification:
      - kind: other
        ref: "E-12 (asserted-once pairs, gg7 section compared with the phase base, grep counts)"
        status: pass
    human_judgment: false
  - id: D2
    description: "WR-04: RetriableRedeliveryLimitPinUnitTest runs 50 redeliveries per row (class time 22.0 s to 2.9 s, XML report 36.2 MB to 3.9 MB), is renamed retriableShapeIsStillRedeliveredAfterManyAttempts, and the budget-of-5 mutation still turns exactly the 64 redelivery rows red (at redelivery 6) and leaves the 44 first-put rows green"
    requirement: RTY-01
    verification:
      - kind: other
        ref: "E-13 (green run, mutation in a removed worktree at the pin commit 0c063acf)"
        status: pass
    human_judgment: false
  - id: D3
    description: "WR-03 and WR-05: FORK-DELTA item 17 states the interval as measured on a Kafka 3.9 worker (deliverMessages body identical in 3.9.1, 4.1.1 and 4.4.0, cadence code not compared in the 4.x sources), scopes 'fails the task even under all' to a task that throws from put, describes the putDirect path as read from the code, and says which rows the pin covers"
    requirement: RTY-01
    verification:
      - kind: other
        ref: "E-14 (five asserted-once pairs, replay on the parent identical, E-06 numbers kept)"
        status: pass
    human_judgment: false
  - id: D4
    description: "Gate on the tip f3612284: cache-busted testUnit with Docker unreachable 2295 tests (E-10's total), full suite on ClickHouse latest 26.10.3.7 x V2 2898 tests with the 18 named skips, deltas against E-01 remove nothing and add the 114 expected rows, src/main unchanged since the E-10 tip, anchors 218 and the tail gate PASS"
    requirement: LIF-01
    verification:
      - kind: other
        ref: "E-15"
        status: pass
    human_judgment: false
---

# Phase 47 Plan 05: Review gap closure Summary

**Five review warnings and one info item are closed without a production change: FORK-DELTA and the gg8 CHANGELOG no longer claim more than the pins and measurements show about `stop()`, the Kafka Connect cadence and the putDirect path, and the RTY-01 redelivery pin runs in 3 s instead of 22 s with its budget-of-5 mutation still turning exactly the 64 redelivery rows red.**

## Performance

- **Duration:** about 13 minutes (start 2026-11-05T21:43:33Z, last commit about 21:56Z; the 3-minute full suite and the 10-minute tail gate ran inside it).
- **Tasks:** 4 (docs wording, pin, item 17, gate), 5 files changed (1 created, 4 modified), 8 commits before this SUMMARY (4 content, 4 evidence).

## Findings table

| Finding | Status | Where |
|---|---|---|
| WR-01 FORK-DELTA `ClickHouseWriter` row names the task's `stop()` as open | fixed: the other two residuals are the Keeper state provider and the task's `close(partitions)`; no open mention of the task's `stop()` is left | `84f804d0`, E-12 |
| WR-02 CHANGELOG says the strategy's exception "reaches Kafka Connect" | fixed: "the one `stop()` throws", Connect only logs a WARN and does not fail the task (read in its sources); the both-throw bullet claims both stops run, an exception reaches the caller of `stop()`, the strategy's is dropped (not suppressed, not logged by `stop()`); gg7 section byte-identical. The optional `LOGGER.warn` in `stop()` was not added (no production change in scope) | `3e0575a7`, E-12 |
| WR-03 item 17 states the interval as a general fact | fixed: measured on a Kafka 3.9 worker; `deliverMessages` body identical in 3.9.1, 4.1.1 and 4.4.0; cadence code (`iteration`, `poll`) not compared in 4.1.1 or 4.4.0; "no count and no deadline" stated as read from the body next to the 3 calls over 120 s measured | `7b348644`, E-14 |
| WR-04 pin class takes 22 s, writes 36 MB, name says "without bound" | fixed: 50 redeliveries per row (2.9 s, 3.9 MB, 19,291 lines against 179,491), renamed `retriableShapeIsStillRedeliveredAfterManyAttempts`, Javadoc says a finite count shows no budget below it; mutation re-run: exactly the 64 redelivery rows red at redelivery 6, 44 first-put rows green. Loggers not silenced (not needed) | `0c063acf`, E-13 |
| WR-05 first-put outcomes pinned only through `ProxySinkTask.put`; item 17 reads as end-to-end | fixed by qualification, pin not extended: "fails the task even under all" is scoped to a task that throws from `put` (measured with the c2 probe); the Direct path through `ChunkFlusher.putDirect` is described as read from the code (second classification, DLQ under `all`, rethrow under `none`); the item states the pin covers `putDirect` only for the 16 retriable shapes (the 64 redelivery rows) and that the non-retriable and conversion first-put outcomes through `putDirect` are not pinned. A `putDirect` first-put matrix was the review's other option and is left as a possible later pin | `7b348644`, E-14 |
| IN-01 two `assertProxyStoppedOnce` assertions never seen red, MBean check has no precondition | left open (not touched, as instructed) | |
| IN-02 stop pin reaches into private and final fields by reflection | left open (not touched, as instructed) | |
| IN-03 (a) item 13 "red on the unchanged tree"; (b) `close(partitions)` missing from gg8 Known Issues | fixed: (a) item 13 says two of six cases (the ones with a throwing strategy) were red and all six are green after; (b) the gg8 Known Issues first bullet carries the `close(partitions)` residual | `84f804d0`, `3e0575a7`, E-12 |
| IN-04 `ClickHouseWriter.stop()` swallows an interrupt (pre-existing) | left open (not touched, as instructed) | |

## Accomplishments

- E-12: three asserted-once pairs in `docs/FORK-DELTA.md` (row 39 and item 13) and three in `CHANGELOG.md` (the two gg8 bullets and the Known Issues residual list). The `1.7.0_gg7` section is byte-identical to the phase base; `src/main` is untouched by the edits.
- E-13: `REDELIVERIES` 500 to 50, method rename, Javadoc and comment; per-file Spotless identical; 108 tests green (44 first-put, 64 redelivery); the 64 redelivery row names changed, so the gate's expected rows are `47-rty01-expected-added-gap.tsv` (the 44 first-put rows are line for line the old list's). Mutation (consecutive-failure counter of 5 in `ProxySinkTask.put`, in a removed worktree at `0c063acf`): 64 failures all at `redelivery 6`, 44 first-put rows green, main tree untouched.
- E-14: five asserted-once pairs on the item-17 line, replayed on the parent commit with an identical result; the classification sentences before "A code listed in `additionalRetriableErrorCodes`" are verbatim, every E-06 instant and gap is kept.
- E-15: gate on the explicit SHA `f3612284`: testUnit (Docker unreachable) 2295 tests, full suite on ClickHouse `latest` 26.10.3.7 x client V2 2898 tests with the 18 named skips, exact deltas against E-01 (removed 0, added 114), eight E-01 pin classes at their counts, scope (production code unchanged since the E-10 tip), anchors `rows=218 bad=0`, tail gate `provenance_paths=11 anchor_commits=119 anchor_rows=218 marker_commits=137 anchor_exempt=0 result=PASS`, nothing pushed.

## Gate totals

| Run | Tests | Failures | Errors | Skipped | Delta against E-01 |
|---|---|---|---|---|---|
| testUnit, Docker unreachable | 2295 | 0 | 0 | 2 | removed 0, added 114 (exact, with the gap list) |
| Full suite, ClickHouse `latest` 26.10.3.7 x V2 | 2898 | 0 | 0 | 18 (7 named, 11 state-mismatch cases) | removed 0, added 114 (by class) |

No JVM crash occurred; `-PjavaVersion=21` was not used; no core dump was left in the repository root.

## Commits

| Commit | Message | Files |
|---|---|---|
| `84f804d07ae1d761e03792510b07237073795716` | correct the FORK-DELTA stop() residual wording (WR-01, IN-03a) | `docs/FORK-DELTA.md` |
| `3e0575a77842255f055ffa962f15d2f8d244719b` | reword the gg8 CHANGELOG stop() bullets to what is shown (WR-02, IN-03b) | `CHANGELOG.md` |
| `a535c816f93fce4282a1d68c283f8d8da20128b0` | record the wording corrections (E-12) | `47-COMMAND-EVIDENCE.md` |
| `0c063acf0068fd284ef21adfbadd5f3223d7bed2` | cut the RTY-01 redelivery pin to 50 attempts and name it for what it shows (WR-04) | `RetriableRedeliveryLimitPinUnitTest.java`, `47-rty01-expected-added-gap.tsv` |
| `0d78f3449162485a6461af6a0b4b31c3d77d5496` | record the redelivery pin cut and its mutation re-run (E-13) | `47-COMMAND-EVIDENCE.md` |
| `7b3486444a64b4fde4fd3f564590973a59812ab2` | qualify FORK-DELTA item 17 to what was measured and pinned (WR-03, WR-05) | `docs/FORK-DELTA.md` |
| `f3612284a3fba473825efba28eda1dab487a44b3` | record the item 17 qualification (E-14) | `47-COMMAND-EVIDENCE.md` |
| `e9ccfcba5069d2284d586097cffd05ca47e2579d` | record the gap-closure gate (E-15) | `47-COMMAND-EVIDENCE.md` |

## Deviations from Plan

The plan was the orchestrator's brief; there is no PLAN.md, so these are departures from its wording, recorded as asked.

**1. [Rule 2 - Consistency] Item 13 received the WR-02 qualification too**
- **Found during:** WR-02 (CHANGELOG). Item 13 carried the same "its exception is the one that reaches Kafka Connect" claim as the CHANGELOG, and the brief named item 13 only for the IN-03 "red on the unchanged tree" fix and for consistency with the writer row.
- **Fix:** item 13 now says the exception is the one `stop()` throws, that Connect only logs it as a WARN (read from the 4.x sources, not measured), and that nothing in `stop()` logs the dropped exception in the both-throw case.
- **Files modified:** `docs/FORK-DELTA.md`. **Commit:** `84f804d0`.

**2. [Divergence from `47-doc-edits.json`, as announced in the brief]** The `changelog_gg8` value and the `fork_delta_item13` pair of the read-only plan-time copy no longer match the committed text of `CHANGELOG.md` and item 13 (E-12 records `plan_time_text_diverges: yes`; the file's md5 `3c4b1ce86e50bf8970e744a7eb6dc3a6` is unchanged). Item 17 had no plan-time text.

**3. [New expected-added list]** The method rename changed the names of the 64 redelivery rows, so the gate compares against the new `47-rty01-expected-added-gap.tsv`; `47-rty01-expected-added.tsv` stays committed and unedited and is no longer used by a gate.

**4. [Not done, by choice]** The optional `LOGGER.warn` in `ClickHouseSinkTask.stop()` (WR-02) and a first-put matrix through `ChunkFlusher.putDirect` (WR-05) were not added: the first is a production change outside the stated scope, the second is the review's alternative to the qualification that was applied. The test-only silencing of the redelivery loop's loggers (WR-04) was not needed after the count cut.

**Total deviations:** 1 auto-added consistency edit, 3 recorded divergences. **Impact:** none on behaviour; the documents say less and the pin costs less.

Process notes, not deviations. (1) `STATE.md`, `ROADMAP.md`, `REQUIREMENTS.md`, `.planning/config.json` and `.planning/state.json` were not updated or staged, per the brief (`config.json` and `state.json` stay dirty and unstaged). (2) `dispatch-isolation` was not run. (3) Nothing was pushed.

## Authentication Gates

None.

## Issues Encountered

None.

## Known Stubs

None.

## Threat Flags

None. No `src/main` change (byte-identical to the E-10 tip, scope check in E-15), no new endpoint or trust-boundary surface; the test change lowers a loop count and renames a method.

## Open items

- IN-01, IN-02 and IN-04 stay open as instructed.
- The Phase 49 hand-off of E-11 is unchanged, with one note for row 6 (the gg8 CHANGELOG section now also carries the `close(partitions)` Known Issues text and the corrected `stop()` bullets; Phase 49 completes and dates it).
- Possible later pin: the first-put matrix through `ChunkFlusher.putDirect` for the non-retriable and conversion shapes (WR-05 alternative).

## Self-Check: PASSED

`47-rty01-expected-added-gap.tsv` and this SUMMARY exist; commits `84f804d0`, `3e0575a7`, `a535c816`, `0c063acf`, `0d78f344`, `7b348644`, `f3612284` and `e9ccfcba` are ancestors of HEAD (`git rev-list --count b68bcb04..e9ccfcba` is 8); `index 15 --done` printed OK; `git diff --name-only a9669f7b HEAD -- src/main` prints nothing; nothing is staged and no core dump or other untracked file is left in the repository root.
