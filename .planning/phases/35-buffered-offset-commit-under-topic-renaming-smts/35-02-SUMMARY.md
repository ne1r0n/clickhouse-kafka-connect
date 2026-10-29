---
phase: 35-buffered-offset-commit-under-topic-renaming-smts
plan: "02"
subsystem: kafka-connect-sink
tags: [kafka-connect, offsets, preCommit, topic-renaming-smt, live-pin, tripwire, exactly-once-state-key, BUF-03]

requires:
  - phase: 35-buffered-offset-commit-under-topic-renaming-smts
    provides: the fix commit 6f3533c2, the red-pins commit, the RenamedTopicRecords and RecordingDBWriter fixtures and evidence E-01..E-04
provides:
  - BufferedOffsetCommitLiveTest, a ClickHouse-backed pin of the offsets preCommit returns under a topic-renaming SMT (4 executions, seen red on the unchanged tree)
  - the harness decision for BUF-05 with its reasons (test suite, no broker-level class)
  - BUF-03 probes P1 to P7 and the counterfactual P6b measured on the tip and recorded verbatim
  - ProcessingRenamedTopicStateKeyUnitTest, a Docker-free tripwire on the exactly-once state key (2 executions, mutation-proved)
  - 35-BUF03-RECORD.md with open items OI-35-1, OI-35-2 and OI-35-3 for the milestone close
affects: [35-03, phase-36-client-bump, phase-38-release, milestone-close]

actuals:
  tokens: 12800
  tasks: 3
  commits: 5

plan_head_before: b4fb0ca8680350e38ce72891e8fed9f69e50a22c
plan_head_after: b7379a638d3710b2509969ee5f39d9ab342582cb

tech-stack:
  added: []
  patterns:
    - "Live pin written and run red in a detached worktree at the red-pins commit, then copied byte-identical (cmp) to the tip and mutation-proved per restored main file"
    - "Throwaway probe class and Processing.java counterfactual live only in a removed worktree; the repository keeps the recorded output"
    - "Tripwire for a decision that must stop for a user (the state key) built from the Docker-free Processing seam, red under a worktree mutation"

key-files:
  created:
    - src/test/java/com/clickhouse/kafka/connect/sink/BufferedOffsetCommitLiveTest.java
    - src/test/java/com/clickhouse/kafka/connect/sink/processing/ProcessingRenamedTopicStateKeyUnitTest.java
    - .planning/phases/35-buffered-offset-commit-under-topic-renaming-smts/35-BUF03-RECORD.md
  modified:
    - docs/FORK-DELTA.md
    - .planning/phases/35-buffered-offset-commit-under-topic-renaming-smts/35-COMMAND-EVIDENCE.md

key-decisions:
  - "The BUF-05 pin is a test-suite class (ClickHouseSinkTask on ClickHouse latest with client V2); no broker-level integrationTest class is added because no phase gate runs integrationTest, a broker class needs an unfixed connector archive, a 95 s class start-up and a new ConfluentPlatform helper, and ConfluentPlatform.getOffset reads a topic's end offset, not a group's committed offset"
  - "Processing.java, KeeperStateProvider.java and the state package stay unedited (recorded user decision); the originalTopic and many-to-one findings are measured and recorded as open items for the milestone close"
  - "The BUF-03 record carries a third open item, OI-35-3 (exactly-once buffering under a partition-changing SMT), from reading the code, because the research left it unprobed"

patterns-established:
  - "A live pin observes the whole preCommit map with assertEquals and a message, applies Connect's assignment filter through RenamedTopicRecords.acceptedByConnect and asserts it drops nothing"
  - "Evidence entries copy probe and mutation output as printed; the verify chain checks the exact strings the plan names"

requirements-completed: [BUF-02, BUF-03, BUF-05]

coverage:
  - id: D1
    description: "On ClickHouse latest with client V2, a buffered sink under a topic-renaming SMT returns the end offset of every assigned original partition from preCommit, Connect's assignment filter drops nothing, and every row is inserted and read back"
    requirement: "BUF-05"
    verification:
      - kind: integration
        ref: "BufferedOffsetCommitLiveTest#BUF-05: buffered sink under a topic-renaming SMT commits the end offset of every assigned original partition and inserts every row (red on the unchanged tree in a worktree, green on the tip); E-05"
        status: pass
    human_judgment: false
  - id: D2
    description: "A revoked original partition is dropped by the buffered sink and only the partitions it still owns are committed, with no stale row inserted"
    requirement: "BUF-02"
    verification:
      - kind: integration
        ref: "BufferedOffsetCommitLiveTest#BUF-02: buffered sink under a topic-renaming SMT drops the revoked partition and commits only the partitions it still owns; E-05 mutation a and b"
        status: pass
    human_judgment: false
  - id: D3
    description: "The not-renamed buffered case and the non-buffered DirectDeliveryStrategy case under a renaming SMT behave as before, and the existing buffer tests and the five direct-path offset tests pass unedited with the baseline count"
    requirement: "BUF-05"
    verification:
      - kind: integration
        ref: "BufferedOffsetCommitLiveTest controls (green on both trees); ClickHouseSinkTaskBufferTest 35/0/0/0 and ClickHouseSinkTaskTest 5/0/0/0; E-05"
        status: pass
    human_judgment: false
  - id: D4
    description: "Processing.doLogic was checked: the state key is the transformed topic and partition and is pinned by a tripwire that fails both executions under a state-key mutation; the originalTopic local and the many-to-one hazard are measured, recorded and left open"
    requirement: "BUF-03"
    verification:
      - kind: unit
        ref: "ProcessingRenamedTopicStateKeyUnitTest#BUF-03 (2 executions, green on the tip, red under the worktree mutation); E-06 probes P1 to P7 and P6b; E-07; 35-BUF03-RECORD.md"
        status: pass
    human_judgment: false
  - id: D5
    description: "Whether to apply the one-line originalTopic fix and how to treat a many-to-one or partition-changing rename is a user decision for the milestone close (OI-35-1, OI-35-2, OI-35-3)"
    verification: []
    human_judgment: true
    rationale: "Each item is a deliberate scope decision on a high-risk file or on a documented limit; no test can decide it"

duration: 11min
completed: 2026-10-29
status: complete
---

# Phase 35 Plan 02: Live Pin and the BUF-03 Record Summary

**The committed offsets of a buffered sink under a RegexRouter-style rename are now pinned on ClickHouse latest (red on the unchanged tree, green on the fix, mutation-proved), and Processing.doLogic was checked by seven measured probes with its state key guarded by a tripwire and its two defects left open, unedited, for the milestone close.**

## Performance

- **Duration:** about 11 min (the start time was not captured; the first command ran at about 09:34Z)
- **Started:** about 2026-10-29T09:34Z
- **Completed:** 2026-10-29T09:46Z
- **Tasks:** 3
- **Files modified:** 5 (2 new test classes, 1 new record, FORK-DELTA one line, the evidence file)

## Accomplishments

- **Live pin, red first (BUF-02, BUF-05).** `BufferedOffsetCommitLiveTest` (extends `ClickHouseBase`, 4 executions) was written in a detached worktree at the red-pins commit with `src/main` unchanged. There it failed `tests=4 failures=2 errors=0 skipped=0`: exactly the commit test and the revocation test, with assertion messages naming the renamed topic as the returned key. It was copied byte-identical to the tip (`cmp` empty) and passes 4 of 4 on ClickHouse `latest` 26.10.3.7 (digest equal to the baseline) with client V2. The two controls pass on both trees.
- **Existing tests untouched (BUF-05).** `ClickHouseSinkTaskBufferTest` ran 35 of 35 (the baseline count) and `ClickHouseSinkTaskTest` ran its 5 offset tests, all green, and both files are byte-identical to the phase start.
- **Live mutation proof.** Restoring `AtLeastOnceBufferStrategy.java` from the red-pins commit turned exactly the revocation test red; restoring `ChunkFlusher.java` turned exactly the commit and revocation tests red. Both predictions were met.
- **Harness decision (E-05).** Test suite, `ClickHouseSinkTask` on ClickHouse `latest` with client V2. The pin observes what `preCommit` returns, which Connect commits after dropping unowned keys; it applies that drop and asserts it removes nothing. No broker-level class was added, with the reasons recorded; a group-level observation (`kafka-consumer-groups --describe`) stays a manual operator check that no gate depends on.
- **BUF-03 probes (E-06).** Run on the tip in a throwaway worktree: P1 400 of 400 written under the single key `t-0`; P2 `written_b=0` of 100; P3 `RuntimeException` starting `AFTER_PROCESSING: State MISMATCH as batch already processed`; P4 `written_b=0`; P5 `bufT-0`; P6 `preCommit_keys=none control_keys=bufT-0=10`; P7 `bufT-dev-0=10 same_instance=true`. The counterfactual P6b (one line in `Processing.java`, worktree only) gave `preCommit_keys=bufT-dev-0=10` with all 14 `ProcessingTest` cases green.
- **Tripwire and record (E-07).** `ProcessingRenamedTopicStateKeyUnitTest` (2 executions, extends nothing, in `testUnit`) passes on the tip and both executions fail when the `topic` local is mutated to the original topic in a worktree. `35-BUF03-RECORD.md` states the outcome with evidence labels, the open items, what happens to KeeperMap rows if a key ever changes, that the `BEFORE`/`AFTER` sequence is untouched and the stop rule.

## Measured evidence

| Fact | Value |
|------|-------|
| Live class on the unchanged tree (worktree) | `tests=4 failures=2 errors=0 skipped=0`; failed set equals the two BUF tests |
| Live class on the tip | `tests=4 failures=0 errors=0 skipped=0`; re-run by the task verify: `P35-02-T1-LIVE-OK` |
| Existing buffer class | 35/0/0/0 (baseline 35); `ClickHouseSinkTaskTest` offset tests 5/0/0/0 |
| ClickHouse | 26.10.3.7, `sha256:739e099abd1157390960a59e4532d21781d8fde57e6e4151cb920ea02c30b1ce` (baseline digest) |
| Live mutations | `AtLeastOnceBufferStrategy` restored: only the revocation test red; `ChunkFlusher` restored: the commit and revocation tests red |
| Probe run | `tests=7 failures=0 errors=0`; lines equal the planning-time observations, no difference |
| Tripwire | green 2/0/0/0 with `ProcessingTest` 14/0/0/0; both executions red under the state-key mutation |
| Cache-busted `testUnit` after the plan | 1456/0/0/2 (1454 after plan 35-01, plus the 2 tripwire executions); anchors mirror `rows=148 bad=0` |
| Main code | `Processing.java`, `KeeperStateProvider.java`, the state package unchanged since the phase start; `git diff --quiet` of `src/main` and `src/testFixtures` between the fix commit and the live-pin commit is empty |

## Task Commits

1. **Task 1: live pin (tracer)** - `2e99eadd` (test: the live class and the FORK-DELTA cell), `db4b7d0d` (docs: E-05)
2. **Task 2: BUF-03 probes** - `a7b3138c` (docs: E-06)
3. **Task 3: tripwire and record** - `3e4248e9` (test: the tripwire), `b7379a63` (docs: the record and E-07)

**Plan metadata:** the SUMMARY commit that follows this file (docs: complete plan).

## Files Created/Modified

- `src/test/java/com/clickhouse/kafka/connect/sink/BufferedOffsetCommitLiveTest.java` - the four live executions (commit, revocation, not-renamed control, direct-path control)
- `src/test/java/com/clickhouse/kafka/connect/sink/processing/ProcessingRenamedTopicStateKeyUnitTest.java` - the state-key tripwire (S1, S2)
- `.planning/phases/35-buffered-offset-commit-under-topic-renaming-smts/35-BUF03-RECORD.md` - outcome, evidence table, open items, KeeperMap and `BEFORE`/`AFTER` statements, stop rule
- `docs/FORK-DELTA.md` - one cell of the BUF row: `BUF-05` and `BufferedOffsetCommitLiveTest` added to "Pinned by"
- `.planning/phases/35-buffered-offset-commit-under-topic-renaming-smts/35-COMMAND-EVIDENCE.md` - E-05 to E-07 (E-08 and E-09 stay pending for plan 35-03)

## Decisions Made

- Followed the plan as written. `Processing.java` is not edited (recorded user decision); the findings are measured and recorded.
- The live class is structured around one `runScenario` helper (task start, body, stop, read-back, table drop, client close in nested try blocks) so each test states only its records and expected offsets. The first draft closed the ClickHouse client before dropping the table; that was corrected before any run.
- Both new test files were checked in a detached worktree with `spotlessApply` and `cmp`: identical, no reformatting.

## Deviations from Plan

None - plan executed exactly as written.

Two process notes, not deviations:

- The first Task 3 verify chain printed no success line because the record said `Re-open trigger:` where the gate greps the lowercase literal `re-open trigger`. I reworded the three bullets to `When to re-open (the re-open trigger):` and amended my own unpushed E-07 commit (`8390fb54` became `b7379a63`); no other file changed and all gates then passed.
- The plan start time was not captured, so the evidence timestamps for E-05 and E-07 mark an approximate start.

**Total deviations:** 0.
**Impact:** none.

## Authentication Gates

None.

## Issues Encountered

None beyond the verify-string rewording above. No JVM crash file appeared in any run (`javacore.*`, `core.*.dmp`, `Snap.*.trc`, `jitdump.*` all absent).

## Known Stubs

None. No hardcoded empty values, placeholders or unwired data in the files this plan created, and no skipped or disabled test was added (the skip inventory stays 18).

## Threat Flags

None. No new endpoint, auth path, file access or schema change; the plan adds tests, a record and evidence, and edits no `src/main` file.

## Items for the user (open items for the milestone close)

- **OI-35-1:** the Direct path with `reportInsertedOffsets=true` under a renaming SMT commits nothing (measured P5, P6; P7 shows the default is unaffected). The one-line fix is known (P6b, `ProcessingTest` stays green) and not applied. Re-open trigger: a connector sets `reportInsertedOffsets=true` together with a renaming SMT.
- **OI-35-2:** two source topics renamed to one table share one state record per partition. Equal offset ranges lose nothing (P1); skewed ranges lose or reject the lower-offset source's records (P2 to P4). It predates the phase and is independent of buffering; exactly-once needs an injective rename. Re-open trigger: a connector whose rename maps two source topics to one table with exactly-once or with the in-memory state machine. Any fix changes the state key or its inputs and stops for a user decision.
- **OI-35-3 (added beyond the two the plan names):** exactly-once buffering under a partition-changing SMT was read from the code and not probed, so exactly-once safety is claimed only for an injective rename. Re-open trigger: an exactly-once connector with a partition-changing SMT.
- Nothing was pushed or merged on origin (`origin/custom_build` still `3a3a15a9`), no pull request was touched, no dependency, `VERSION` or `CHANGELOG.md` changed. `.planning/config.json`, `.planning/state.json` and `.planning/STATE.md` are orchestrator-managed: this plan did not touch or stage them, and they stay dirty.
- Still open for plan 35-03: the rollout item with the first-start derivation and the phase gate.

## Next Phase Readiness

Ready for plan 35-03. The suite now holds the live pin that carries BUF-05 through the gates of Phases 36 and 38, the anchors mirror reads `rows=148 bad=0`, and cache-busted `testUnit` is 1456/0/0/2.

## Self-Check: PASSED
