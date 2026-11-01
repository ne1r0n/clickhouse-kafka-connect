---
phase: 35-buffered-offset-commit-under-topic-renaming-smts
plan: "01"
subsystem: kafka-connect-sink
tags: [kafka-connect, offsets, preCommit, topic-renaming-smt, buffered-delivery, fork-anchors, BLD-16]

requires:
  - phase: 34-upstream-tail-merge
    provides: the merged tail tree, the gate scripts and the read-only evidence helper this plan reuses
provides:
  - buffered connectors under a topic-renaming SMT commit the offsets of the partitions Connect assigned (upstream #777, fork-only fix)
  - at-least-once close and exactly-once grouping use the consumer's original topic and partition
  - Docker-free BufferedOffsetCommitUnitTest (13 executions) with RenamedTopicRecords and RecordingDBWriter fixtures
  - the three SinkRecord accessors blessed in the Kafka API list from the production worker jars
  - 7 fork-anchors rows, one FORK-DELTA row and one clause on its buffered-delivery open item
  - Phase 35 evidence E-01..E-04 with a per-file mutation proof
affects: [35-02, 35-03, phase-36-client-bump, phase-38-release]

actuals:
  tokens: 13680
  tasks: 3
  commits: 6

plan_head_before: 6ac0cdc0acfc22601dd19acdc6e564fb5a96e61e
plan_head_after: 4db8087df1cccdd6b4747119c2aaee67f93cd87a

tech-stack:
  added: []
  patterns:
    - "Offsets and revocation keyed by SinkRecord.originalTopic()/originalKafkaPartition()/originalKafkaOffset(); table routing keeps the transformed topic"
    - "Red-first pins committed on the unchanged tree, then the fix, then a per-file mutation proof from a detached worktree"

key-files:
  created:
    - src/test/java/com/clickhouse/kafka/connect/sink/BufferedOffsetCommitUnitTest.java
    - src/testFixtures/java/com/clickhouse/kafka/connect/sink/helper/RenamedTopicRecords.java
    - src/testFixtures/java/com/clickhouse/kafka/connect/sink/helper/RecordingDBWriter.java
    - .planning/phases/35-buffered-offset-commit-under-topic-renaming-smts/35-COMMAND-EVIDENCE.md
  modified:
    - src/main/java/com/clickhouse/kafka/connect/sink/ChunkFlusher.java
    - src/main/java/com/clickhouse/kafka/connect/sink/AtLeastOnceBufferStrategy.java
    - src/main/java/com/clickhouse/kafka/connect/sink/ExactlyOnceBufferStrategy.java
    - src/test/resources/connect-api-references.txt
    - .planning/scripts/fork-anchors.txt
    - docs/FORK-DELTA.md

key-decisions:
  - "The fix is exactly the three offset sites; Processing.java, KeeperStateProvider.java, every state key and table routing are untouched (recorded user decision)"
  - "The fix commit carries six paths by design: BLD-16 goes red the instant the main edit lands without the re-bless, and anchors may not precede or lag the code they pin"
  - "The red-pins commit is deliberately red and local; it changes no src/main file"

patterns-established:
  - "Pins observe whole maps and written-record sets (assertEquals with a message), never Map.get, so the red run names the cause instead of throwing a NullPointerException"
  - "Mutation proof restores a file from the red-pins commit (no text replacement) and checks both the failing test set and the anchors mirror"

requirements-completed: [BUF-01, BUF-02, BUF-04, BUF-07]

coverage:
  - id: D1
    description: "preCommit under a topic-renaming SMT is keyed by the consumer's original topic and partition at the original offset plus one, in both buffered strategies, including a record whose partition and offset the SMT also changed"
    requirement: "BUF-01"
    verification:
      - kind: unit
        ref: "BufferedOffsetCommitUnitTest#BUF-01 preCommit keys / follows / close forgets (6 executions, red on the unchanged tree, green after the fix)"
        status: pass
    human_judgment: false
  - id: D2
    description: "At-least-once close drops a revoked original partition's buffered records and the exactly-once buffer groups by original partition, so no stale record reaches a later chunk"
    requirement: "BUF-02"
    verification:
      - kind: unit
        ref: "BufferedOffsetCommitUnitTest#BUF-02 at-least-once close / exactly-once buffer groups"
        status: pass
    human_judgment: false
  - id: D3
    description: "Records whose topic was not renamed, the direct strategy and table routing under the renamed topic behave exactly as before"
    requirement: "BUF-04"
    verification:
      - kind: unit
        ref: "BufferedOffsetCommitUnitTest#BUF-04 records whose topic was not renamed; BUF-05 direct delivery; BUF-02 flushed chunks are still inserted under the renamed topic (5 controls, green on both trees)"
        status: pass
    human_judgment: false
  - id: D4
    description: "The three SinkRecord accessors are blessed in the Kafka API list from the production worker's 7.9.7 jars, 4.1.1 and 4.4.0, each shown present in the worker's connect-api jar"
    requirement: "BUF-01"
    verification:
      - kind: unit
        ref: "ConnectApiReferencesUnitTest#BLD-16 (red with the edit and no re-bless, green after); 35-COMMAND-EVIDENCE.md E-03"
        status: pass
    human_judgment: false
  - id: D5
    description: "The fix commit carries 7 fork-anchors rows and a FORK-DELTA row stating the fix is fork-only until upstream resolves #777 (PR #786 closed unmerged); the tail gate over the tail merge..the fix commit passes"
    requirement: "BUF-07"
    verification:
      - kind: other
        ref: "bash .planning/scripts/verify-merge-invariants.sh <tail merge> <fix commit> ends anchor_rows=148 anchor_exempt=0 result=PASS (E-03)"
        status: pass
    human_judgment: false
  - id: D6
    description: "Every pin and anchor row of the fix can fail: restoring each edited main file from the red-pins commit turns exactly its pins and its anchor rows red"
    requirement: "BUF-04"
    verification:
      - kind: other
        ref: "35-COMMAND-EVIDENCE.md E-04 (6 pins and 3 rows, 1 pin and 2 rows, 1 pin and 2 rows)"
        status: pass
    human_judgment: false

duration: 14min
completed: 2026-10-29
status: complete
---

# Phase 35 Plan 01: Buffered Offset Commit Under Topic-Renaming SMTs Summary

**Buffered connectors under RegexRouter-style SMTs now commit offsets for the partitions Connect assigned: three one-line sites in ChunkFlusher and both buffer strategies key by originalTopic(), originalKafkaPartition() and originalKafkaOffset() + 1, pinned red-first by 13 Docker-free executions and proven by a per-file mutation run.**

## Performance

- **Duration:** about 14 min (the start time was not captured; the first command ran at about 09:19Z)
- **Started:** about 2026-10-29T09:19Z
- **Completed:** 2026-10-29T09:34Z
- **Tasks:** 3
- **Files modified:** 10 (6 in the fix commit, 3 new test-side files, 1 evidence file)

## Accomplishments

- **Fix (BUF-01, BUF-02).** `ChunkFlusher.flush` keys flushed offsets by `originalTopic()`, `originalKafkaPartition()` and `originalKafkaOffset() + 1`; `AtLeastOnceBufferStrategy.close` and `ExactlyOnceBufferStrategy.put` use the original topic and partition. The main diff is 3/2 lines in `ChunkFlusher` (one is a comment) and 1/1 in each strategy, changes no import line and adds no object creation. Table routing (`Record.convert`, `RecordConvertor`, `ProxySinkTask.batchKey`), `Processing.java`, `KeeperStateProvider.java` and every state key are untouched.
- **Red first (BUF-04).** `BufferedOffsetCommitUnitTest` (8 methods, 13 executions, extends nothing so it stays in `testUnit`) builds records through `SinkRecord.newRecord` and the 13-argument constructor. On the unchanged tree it failed exactly 8 executions (assertion failures, no errors) and its 5 controls passed; the red-pins commit changes no `src/main` file.
- **BLD-16.** With the edit and no re-bless, exactly `ConnectApiReferencesUnitTest#BLD-16` failed. The re-bless, from the worker jars of the local `cp-kafka-connect-base:7.9.7` image (hashes equal to the blessed header) plus 4.1.1 and 4.4.0, added exactly three `M` lines and a new date line and kept the image line byte-identical (derived members 138 to 141). Each accessor is present in the production worker's `connect-api-7.9.7-ccs.jar`.
- **Bookkeeping (BUF-07, first half).** The fix commit carries 7 `fork-anchors.txt` rows (anchors mirror `rows=148 bad=0`), one `docs/FORK-DELTA.md` row stating the fix is fork-only until upstream resolves #777 (PR #786 closed unmerged), and one clause on FORK-DELTA's buffered-delivery open item. The tail gate over the tail merge..the fix commit ended `anchor_rows=148 anchor_exempt=0 result=PASS`.
- **Mutation proof (E-04).** Restoring `ChunkFlusher.java` from the red-pins commit turned exactly 6 pins and 3 anchor rows red; `AtLeastOnceBufferStrategy.java` exactly 1 pin and 2 rows; `ExactlyOnceBufferStrategy.java` exactly 1 pin and 2 rows. The five controls stayed green under every mutation.

## Measured evidence

| Fact | Value |
|------|-------|
| Baseline, untouched tree | testUnit 1441/0/0/2; full suite on `latest` x V2 2028/0/0/18, skips `OK`; ClickHouse 26.10.3.7 (`sha256:739e099abd11...`, the digest of Phase 34's gate); no JVM crash |
| Red set | `tests=13 failures=8 errors=0 skipped=0`: BUF-01 preCommit x2, BUF-01 partition-and-offset x2, BUF-01 close-forgets x2, BUF-02 at-least-once close, BUF-02 exactly-once grouping |
| Controls (green on both trees) | not-renamed x2, direct strategy, table routing under the renamed topic x2 |
| After the fix | the class `13/0/0/0`; cache-busted testUnit 1454/0/0/2 (13 more than the baseline) |
| Failure messages on the unchanged tree | `expected {buf35-dev-1=110, buf35-dev-0=50} but was {buf35-0=50, buf35-1=110}`; the at-least-once pin wrote the revoked partition's records; the exactly-once pin flushed stale records in a later chunk |
| Anchors | `rows=141 bad=0` before, `rows=148 bad=0` after; the three zero rows read 1 on the unfixed tree and 0 after |
| Tail gate | `provenance_paths=27 anchor_commits=33 anchor_rows=148 marker_commits=40 anchor_exempt=0 result=PASS` |

## Task Commits

1. **Task 1: red pins (tracer)** - `e23247b1` (docs: evidence opened, E-01), `e8573752` (test: the red pins, deliberately red and local), `ad978fcd` (docs: E-02)
2. **Task 2: the fix commit** - `6f3533c2` (fix: six paths by design), `76ed1511` (docs: E-03)
3. **Task 3: mutation proof** - `4db8087d` (docs: E-04)

**Plan metadata:** the SUMMARY commit that follows this file (docs: complete plan).

## Files Created/Modified

- `src/main/java/com/clickhouse/kafka/connect/sink/ChunkFlusher.java` - flushed offsets keyed by the consumer's original topic, partition and offset + 1
- `src/main/java/com/clickhouse/kafka/connect/sink/AtLeastOnceBufferStrategy.java` - revocation drop matched by original topic and partition
- `src/main/java/com/clickhouse/kafka/connect/sink/ExactlyOnceBufferStrategy.java` - per-partition buckets keyed by original topic and partition
- `src/test/java/com/clickhouse/kafka/connect/sink/BufferedOffsetCommitUnitTest.java` - the 13 pins
- `src/testFixtures/java/com/clickhouse/kafka/connect/sink/helper/RenamedTopicRecords.java` - SinkRecords as a renaming SMT leaves them, and Connect's assignment filter
- `src/testFixtures/java/com/clickhouse/kafka/connect/sink/helper/RecordingDBWriter.java` - `InMemoryDBWriter` that records the consumer position and table topic of every inserted record
- `src/test/resources/connect-api-references.txt` - re-blessed (three accessor lines and the date)
- `.planning/scripts/fork-anchors.txt` - 7 Phase 35 rows
- `docs/FORK-DELTA.md` - the connector-source row and the item 9 clause
- `.planning/phases/35-buffered-offset-commit-under-topic-renaming-smts/35-COMMAND-EVIDENCE.md` - E-01 to E-04 (E-05 to E-09 stay pending for plans 35-02 and 35-03)

## Decisions Made

- Followed the plan as written: the fix is the three sites, nothing in `Processing.java` or any state key (a recorded user decision). The `reportInsertedOffsets` defect and the many-to-one state-key hazard found in research stay open items for the BUF-03 record of plan 35-02.
- The three new Java files were checked in a detached worktree with `spotlessApply` and compared with `cmp`: all identical, so no reformatting was needed. The three edited main files were never run through Spotless.

## Deviations from Plan

None - plan executed exactly as written.

One note on process, not a deviation: the full baseline suite ran in the background while the two fixtures and the unit class were being written. Gradle had compiled before any of them existed, and the tally (2028 tests, equal to Phase 34's baseline) confirms the new class was not part of the baseline. The red-pins and mutation runs each ran alone, never two Gradle builds at once.

**Total deviations:** 0.
**Impact:** none.

## Authentication Gates

None.

## Issues Encountered

None. The BLD-16 red run was a plan prediction, not an issue. No JVM crash occurred in any run.

## Known Stubs

None. No hardcoded empty values, placeholders or unwired data in the files this plan created or changed; no skipped or disabled test was added (the skip inventory stays 18).

## Threat Flags

None. No new endpoint, auth path, file access or schema change; the main diff only changes which `SinkRecord` accessors key two in-memory maps and one removal predicate.

## Items for the user

- The red-pins commit `e8573752` is red on purpose and local; the fix is the next code commit `6f3533c2`. If the branch is ever bisected, expect the red there.
- Nothing was pushed, no pull request was touched, no dependency, `VERSION` or `CHANGELOG.md` changed.
- `.planning/STATE.md`, `.planning/config.json` and `.planning/state.json` were left uncommitted, as instructed.
- Still open for the later plans of this phase (not part of this plan): the live pin and the BUF-03 record (35-02), the rollout item and the phase gate (35-03).

## Next Phase Readiness

Ready for plan 35-02 (the live pin and the BUF-03 record). It must find `ClickHouseSinkTaskBufferTest` unchanged (35 tests, E-01), and the fork-anchors file now holds 148 rows.

## Self-Check: PASSED
