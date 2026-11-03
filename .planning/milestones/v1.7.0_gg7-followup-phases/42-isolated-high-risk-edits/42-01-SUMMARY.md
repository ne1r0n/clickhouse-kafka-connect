---
phase: 42-isolated-high-risk-edits
plan: "01"
subsystem: testing
tags: [exactly-once, kafka-connect, reportInsertedOffsets, originalTopic, fork-delta, junit5]

requires:
  - phase: 35-buffered-offset-commit
    provides: original-topic keyed buffered offsets and the OI-35-2 / OI-35-3 limits this plan records
  - phase: 41-bounded-insert-timeout
    provides: the evidence helper, the status-delta gate recipe and the 1757 / 2355 baselines
provides:
  - "Processing.doLogic line 137 reads SinkRecord.originalTopic(): the Direct path reports and clears inserted offsets under the consumer's topic"
  - "OriginalTopicInsertedOffsetsUnitTest: 6 Docker-free cases (2 behaviour pins, 2 limit pins, 2 guards)"
  - "FORK-DELTA Processing.java Connector source row and Known cost 16 rewritten with the OI-35-2 and OI-35-3 limits"
  - "42-COMMAND-EVIDENCE.md E-01..E-04, 42-testcase-status.py, 42-rsk01-expected-added.tsv"
affects: [42-02, 42-03, 42-04, 44-release-notes]

actuals:
  tokens: 19400
  tasks: 3
  commits: 4

tech-stack:
  added: []
  patterns:
    - "red-then-green by assertion with a SHA-256 freeze of the test file between the red run and the code commit"
    - "per-testcase status dump and delta against a committed expected-added file as the no-existing-test-changed gate"
    - "limit pins named limit... characterize unsupported configurations so FORK-DELTA can label them measured"

key-files:
  created:
    - src/test/java/com/clickhouse/kafka/connect/sink/OriginalTopicInsertedOffsetsUnitTest.java
    - .planning/phases/42-isolated-high-risk-edits/42-COMMAND-EVIDENCE.md
    - .planning/phases/42-isolated-high-risk-edits/42-evidence-check.py
    - .planning/phases/42-isolated-high-risk-edits/42-testcase-status.py
    - .planning/phases/42-isolated-high-risk-edits/42-rsk01-expected-added.tsv
  modified:
    - src/main/java/com/clickhouse/kafka/connect/sink/processing/Processing.java
    - docs/FORK-DELTA.md
    - .planning/scripts/fork-anchors.txt

key-decisions:
  - "The state key stays on the transformed topic: line 137 feeds only the in-memory inserted-offset map, so the one-line edit cannot orphan a KeeperMap row (trace guard pins key orig-0 and BEFORE before AFTER on both trees)"
  - "The two limit cases (many-to-one rename, partition-changing SMT) are pinned in the RSK-01 test class so Known cost 16 labels them measured, not read from the code"
  - "STATE.md is updated by the GSD verbs but left uncommitted, as the plan's commit rules forbid staging it"

patterns-established:
  - "Each Phase 42 code commit carries its test, its code line and its FORK-DELTA and anchors rows, so it reverts alone"

requirements-completed: [RSK-01, RSK-04]

plan_head_before: 1c808c02728e4f9344c46015f63fe4f6cf52a6d9
plan_head_after: 7b76d76d6dde431c0a16d7c6fbe2ffa0f943e848

coverage:
  - id: D1
    description: "Processing.doLogic line 137 reads originalTopic(); the Direct path reports and clears inserted offsets under the consumer's topic for an injective rename"
    requirement: RSK-01
    verification:
      - kind: unit
        ref: "OriginalTopicInsertedOffsetsUnitTest#directDeliveryReportsTheConsumersTopicUnderAnInjectiveRename, #closeClearsTheInsertedOffsetOfTheRevokedConsumerPartition (red on the unchanged tree, green after the edit; E-02, E-03, E-04)"
        status: pass
    human_judgment: false
  - id: D2
    description: "The exactly-once state key, the KeeperMap row and the BEFORE/AFTER sequence are unchanged: Processing diff is 1/1, sink/state and sink/kafka have an empty diff, ProcessingTest 14/0 byte-unchanged"
    requirement: RSK-01
    verification:
      - kind: unit
        ref: "OriginalTopicInsertedOffsetsUnitTest#stateKeyAndFlagSequenceStayOnTheTransformedTopicUnderAnInjectiveRename; ProcessingTest; ProcessingRenamedTopicStateKeyUnitTest"
        status: pass
    human_judgment: false
  - id: D3
    description: "The OI-35-2 and OI-35-3 limits and the remaining onPartitionRemoved keying mismatch are written down in FORK-DELTA Known cost 16, labelled measured from the two limit pins and derived for the Connect half"
    requirement: RSK-04
    verification:
      - kind: unit
        ref: "OriginalTopicInsertedOffsetsUnitTest#limitManyToOneRenameReportsTheSharedMaximumForTheFirstSourceTopic, #limitPartitionChangingSmtKeepsTheTransformedPartitionInTheKey"
        status: pass
    human_judgment: true
    rationale: "The wording of the published limits is a maintainer judgment; the tests pin the measured behaviour, not the prose"
  - id: D4
    description: "The gate: a cache-busted testUnit (Docker unreachable) and a full suite on latest x V2 pass at the baselines plus 6 with a status delta of exactly the 6 added rows"
    requirement: RSK-01
    verification:
      - kind: other
        ref: "42-COMMAND-EVIDENCE.md E-04: testUnit 1763/0/0/2, full 2361/0/0/18, delta removed=0 added=6 expected=6 match=yes"
        status: pass
    human_judgment: false

duration: 11min
completed: 2026-11-02
status: complete
---

# Phase 42 Plan 01: RSK-01 original-topic edit Summary

**One-line `Processing.doLogic` edit (line 137, `topic()` to `originalTopic()`) so the Direct path reports and clears inserted offsets under the consumer's topic, proven red-then-green by six Docker-free pins with the exactly-once state key unmoved, and the OI-35 limits it does not fix recorded as measured in FORK-DELTA Known cost 16**

## Performance

- **Duration:** 11 min
- **Started:** 2026-11-02T12:01:24Z
- **Completed:** 2026-11-02T12:12:24Z
- **Tasks:** 3
- **Files modified:** 8 (5 created, 3 modified)

## Accomplishments

- Measured the phase baseline on the unchanged tree instead of trusting the record: `testUnit` with Docker unreachable 1757 tests / 2 skipped, full suite on ClickHouse 26.10.3.7 (`latest`) x client V2 2355 tests / the 18 named skips, both exit 0 with 0 failures and 0 errors, equal to the Phase 41 numbers (E-01).
- Wrote `OriginalTopicInsertedOffsetsUnitTest` (6 cases) and saw it red on the unchanged tree for the stated reason: exactly the four pins fail, each by `AssertionFailedError`, the trace guard and the no-SMT control pass (E-02).
- Made the one-line edit: the Processing diff is one removed and one added line, `sink/state`, `sink/kafka` and `ClickHouseSinkConfig` have an empty diff against phase start, and the 14 `ProcessingTest` cases and the 2 `ProcessingRenamedTopicStateKeyUnitTest` cases are byte-unchanged and green (E-03).
- Recorded the change and its limits in FORK-DELTA (new Processing.java Connector source row, Known cost 16 rewritten with OI-35-2, OI-35-3, the remaining `onPartitionRemoved` mismatch, the many-to-one over-report and the do-not-enable guidance) plus two anchor rows (mirror 184 to 186, `bad=0`).
- Gated the commit: `testUnit` 1763 and full suite 2361, exactly the baselines plus 6, and a status delta that removed nothing and added exactly the six new PASS rows (E-04).

## Task Commits

1. **Task 1 (evidence, helpers, baseline, red run)** - `1e53a6a7` (docs). The test class and the line edit stayed uncommitted until Task 2 by design.
2. **Task 2 (RSK-01 code commit)** - `cba0909b` (fix): `Processing.java`, `OriginalTopicInsertedOffsetsUnitTest.java`, `docs/FORK-DELTA.md`, `fork-anchors.txt`. This is the revert unit. Its E-03 record and the expected-added list: `b21e0f15` (docs).
3. **Task 3 (gate E-04)** - `7b76d76d` (docs).

**Plan metadata:** the SUMMARY and ROADMAP commit that follows this file (docs: complete plan).

## Files Created/Modified

- `src/main/java/com/clickhouse/kafka/connect/sink/processing/Processing.java` - line 137 reads `record.getSinkRecord().originalTopic()`.
- `src/test/java/com/clickhouse/kafka/connect/sink/OriginalTopicInsertedOffsetsUnitTest.java` - six Docker-free cases; `RecordingState` subclass of `InMemoryState` records the state-call order.
- `docs/FORK-DELTA.md` - Connector source row after the `Column.convertEnumValues` row; Known cost 16 rewritten.
- `.planning/scripts/fork-anchors.txt` - Phase 42 comment and two RSK-01 rows after the last Phase 35 row.
- `.planning/phases/42-isolated-high-risk-edits/42-COMMAND-EVIDENCE.md` - header, 12-row index, E-01 to E-04.
- `.planning/phases/42-isolated-high-risk-edits/42-evidence-check.py` - byte copy of Phase 41's helper.
- `.planning/phases/42-isolated-high-risk-edits/42-testcase-status.py` - read-only status dump, select, delta and count helper.
- `.planning/phases/42-isolated-high-risk-edits/42-rsk01-expected-added.tsv` - the six PASS rows later gates compare against.

## Decisions Made

- The state key stays on the transformed topic. The value at line 137 reaches only `BaseStateProviderImpl.onStateUpdate`, so the edit moves the key of the in-memory inserted-offset map and nothing stored; the trace guard pins `orig-0` with `BEFORE_PROCESSING` before `AFTER_PROCESSING` for every inserted range on both trees.
- The many-to-one and partition-changing behaviours are pinned as `limit...` cases rather than described from the code, so Known cost 16 can label them measured. Whether Connect would commit an offset above the consumer position stays labelled derived.
- `STATE.md` is advanced by the GSD verbs but not staged, because the plan's commit rules forbid staging it; the orchestrator commits it.

## Deviations from Plan

None - plan executed exactly as written. The trace guard's literal from the research matched the unchanged tree, so no guard correction was needed before the red run.

## Issues Encountered

None. No JVM crash file appeared, ClickHouse version and image digest were identical before and after every run, and `origin/custom_build` equaled the E-01 value at every gate.

## Known Stubs

None.

## Threat Flags

None - the edit adds no endpoint, auth path, file access or schema change. T-42-02 (committed offsets under a many-to-one rename or a partition-changing SMT with `reportInsertedOffsets=true`) is mitigated by the two limit pins and the do-not-enable statement in Known cost 16; the reversibility note stands: a revert does not undo offsets a deployment already committed under that hazard.

## User Setup Required

None - no external service configuration required.

## Next Phase Readiness

- Ready for 42-02 (RSK-02). Its code commit and record rows sit at positions separated from this commit's insertions, so each reverts alone (proven in 42-04).
- The anchors mirror stands at 186 rows; 42-02 should expect 188 after its commit.
- Scratch dumps `status-base-*.tsv` and `status-gate1-*.tsv` in `/tmp/gsd42/exec` are the inputs of the 42-02 delta; if they are gone, re-measure on the current tree.

## Self-Check: PASSED

- Created files exist: the test class, the evidence file, both helpers and the expected-added list.
- Commits exist on HEAD: `1e53a6a7`, `cba0909b`, `b21e0f15`, `7b76d76d`.

---
*Phase: 42-isolated-high-risk-edits*
*Completed: 2026-11-02*
