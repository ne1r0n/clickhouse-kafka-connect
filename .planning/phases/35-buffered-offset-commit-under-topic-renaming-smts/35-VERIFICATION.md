---
phase: 35-buffered-offset-commit-under-topic-renaming-smts
verified: 2026-10-29T11:10:00Z
status: passed
score: 4/4 in-scope roadmap success criteria verified (criterion 4 / BUF-06 out of scope by user decision, not gated)
covered_files:
  - ".planning/phases/35-buffered-offset-commit-under-topic-renaming-smts/35-01-PLAN.md"
  - ".planning/phases/35-buffered-offset-commit-under-topic-renaming-smts/35-01-SUMMARY.md"
  - ".planning/phases/35-buffered-offset-commit-under-topic-renaming-smts/35-02-PLAN.md"
  - ".planning/phases/35-buffered-offset-commit-under-topic-renaming-smts/35-02-SUMMARY.md"
  - ".planning/phases/35-buffered-offset-commit-under-topic-renaming-smts/35-03-PLAN.md"
  - ".planning/phases/35-buffered-offset-commit-under-topic-renaming-smts/35-03-SUMMARY.md"
  - ".planning/scripts/fork-anchors.txt"
  - "docs/FORK-DELTA.md"
  - "src/main/java/com/clickhouse/kafka/connect/sink/AtLeastOnceBufferStrategy.java"
  - "src/main/java/com/clickhouse/kafka/connect/sink/ChunkFlusher.java"
  - "src/main/java/com/clickhouse/kafka/connect/sink/ExactlyOnceBufferStrategy.java"
  - "src/test/java/com/clickhouse/kafka/connect/sink/BufferedOffsetCommitLiveTest.java"
  - "src/test/java/com/clickhouse/kafka/connect/sink/BufferedOffsetCommitUnitTest.java"
  - "src/test/resources/connect-api-references.txt"
covered_digest: "v4:sha256:fc8d853b7954dfbc227eb1e19122065472563781e64b85b2a300c61ef4819b22"
behavior_unverified: 0
overrides_applied: 0
---

# Phase 35: Buffered Offset Commit Under Topic-Renaming SMTs Verification Report

**Phase Goal:** With `bufferCount > 0` and a topic-renaming SMT such as `RegexRouter`, the connector commits consumer offsets for the partitions it was assigned, so a restart or rebalance resumes from the last flushed offset instead of rereading from `auto.offset.reset`. Revoked partitions drop their buffered records and the exactly-once buffer groups by original partition. The non-buffered path and the non-renamed case behave as before. `Processing.doLogic` is checked for the same defect and the outcome recorded.
**Verified:** 2026-10-29
**Status:** passed
**Re-verification:** No, initial verification
**Verified tree:** `sync/v1.7.0` at `0bbed53a` (the three modified `.planning/` state files are unstaged and were not read as evidence).

## Goal Achievement

### Observable Truths (ROADMAP Phase 35 success criteria)

| # | Truth | Status | Evidence |
|---|-------|--------|----------|
| 1 | Docker-free pins built through `newRecord` with a renamed topic; `preCommit` keyed by `originalTopic()`/`originalKafkaPartition()` at `originalKafkaOffset()+1`; `AtLeastOnceBufferStrategy.close()` drops revoked partitions by original key; `ExactlyOnceBufferStrategy.put` keys `perPartitionBuffer` by original key; non-renamed unchanged; `ConnectApiReferencesUnitTest` lists three accessors | VERIFIED | Source read: `ChunkFlusher.java:54-55`, `AtLeastOnceBufferStrategy.java:86`, `ExactlyOnceBufferStrategy.java:43` use the original accessors. `git diff 6ac0cdc0 HEAD -- src/main` is exactly these 3 files (5 added, 4 removed lines). `grep` of `\.topic()\|kafkaPartition()\|kafkaOffset()` in `sink/*.java` finds no remaining transformed-key site in any buffered path; `ClickHouseSinkTask.preCommit/close` only forward Connect's own keys. `connect-api-references.txt:187-189` lists the three accessors. Ran cache-busted `testUnit --rerun`: `tests=1458 failures=0 errors=0 skipped=2` (1456 + 2 WR-01 executions); `BufferedOffsetCommitUnitTest` 15/0/0, `ConnectApiReferencesUnitTest` 3/0/0, `ProcessingRenamedTopicStateKeyUnitTest` 2/0/0. Red-on-unchanged-tree and per-file mutation proof are recorded in E-02 and E-04. Worker 7.9.7 jar presence is recorded in E-03 (`worker_accessors_present`); I did not re-run the jar probe. |
| 2 | [record] `Processing.doLogic` checked; result stated with evidence; BEFORE/AFTER intact; old-key KeeperMap rows addressed | VERIFIED (accepted per user decision) | `35-BUF03-RECORD.md` states the outcome with `measured` / `read from the code` labels (E-06 P1 to P7, E-07), the BEFORE/AFTER statement and the old-key rows section. `Processing.java:61,87,136-137` confirms the record's code reading: state key from `getRecordOffsetContainer().getTopic()`, `originalTopic` local from `getSinkRecord().topic()`. `git diff 6ac0cdc0 HEAD` over `sink/processing`, `sink/state`, `ProxySinkTask`, `ClickHouseSinkConfig`, `DirectDeliveryStrategy` is empty. Tripwire `ProcessingRenamedTopicStateKeyUnitTest` passes (2/2). The outcome is neither "fix with red pins" nor a clean "non-issue": it is a measured, recorded, unapplied defect (OI-35-1) and hazards (OI-35-2, OI-35-3). I accept it as satisfying the criterion's "record states the result with evidence" by the user's explicit decision that `Processing.java` is unedited; see Advisories. |
| 3 | [live] On `latest` x V2, `bufferCount > 0` + `RegexRouter` shows committed offsets equal end offsets; Direct path and `bufferCount`/`bufferFlushTime` contract unchanged; existing buffer tests pass unedited | VERIFIED | Ran `CLICKHOUSE_VERSION=latest CLIENT_VERSION=V2 ./gradlew test --tests '*BufferedOffsetCommitLiveTest'` (build dir cleared first): BUILD SUCCESSFUL, XML `tests=4 failures=0 errors=0 skipped=0`. The test asserts `assertEquals(expected end offsets, committed)` keyed by the original topic, applies Connect's assignment filter, reads rows back from ClickHouse, and has a Direct-path `assertSame` control. `git diff --name-status 6ac0cdc0 HEAD -- src/test src/integrationTest` shows only three added test files; `ClickHouseSinkTaskBufferTest` and `ClickHouseSinkTaskTest` have 0 commits and an empty diff since `6ac0cdc0`. `ClickHouseSinkTaskBufferTest` 35/35 per E-09 (full suite 2047 tests, 0 failures, 18 named skips; I did not re-run the full Docker suite). The live pin observes the map `preCommit` returns, not a broker consumer group; E-05 records this harness decision. |
| 4 | [record] Real-environment rerun of the fixed jar (BUF-06) | OUT OF SCOPE | Moved out of this repository by the user and tracked outside; not gated. Not a gap. |
| 5 | [record] Fix commit carries a FORK-DELTA row and `fork-anchors.txt` anchor; FORK-DELTA says fork-only until upstream resolves #777; rollout item for Phase 38 with evidence labels, saying offsets are now committed (no reread from `auto.offset.reset` after restart or rebalance) and where the first start resumes | VERIFIED | `fork-anchors.txt` has 7 Phase 35 rows (1 and 0 counts for the three files). Live `evidence-check.py anchors`: `rows=148 bad=0`. `docs/FORK-DELTA.md:32` row names the three sites, says "fork-only until upstream resolves #777", cites PR #786 closed unmerged and BLD-16, and item 9 carries the "not used, not unpatched" clause. `35-ROLLOUT-ITEM.md` has: "What changes" (offsets now committed, group resumes from last flushed offset, no reread from `auto.offset.reset` after restart or rebalance), "First start after the upgrade" (resumes at the existing committed offset if any, else the `auto.offset.reset` position; one reread with `earliest`), a `measured` / `read from the code` / `derived` evidence-label table, and the minimum Connect runtime. Commit order: anchors and FORK-DELTA landed in fix commit `6f3533c2`. |

**Score:** 4/4 in-scope truths verified (0 behavior-unverified, 0 overrides).

### Required Artifacts

| Artifact | Expected | Status | Details |
|----------|----------|--------|---------|
| `sink/ChunkFlusher.java` | flush keyed by original topic/partition, `originalKafkaOffset()+1` | VERIFIED | Lines 53-55 |
| `sink/AtLeastOnceBufferStrategy.java` | revocation drop by original key | VERIFIED | Line 86 |
| `sink/ExactlyOnceBufferStrategy.java` | `perPartitionBuffer` keyed by original key | VERIFIED | Line 43 |
| `BufferedOffsetCommitUnitTest` | Docker-free pins | VERIFIED | 15 executions green |
| `BufferedOffsetCommitLiveTest` | live pin | VERIFIED | 4 executions green on latest x V2 |
| `ProcessingRenamedTopicStateKeyUnitTest` | state-key tripwire | VERIFIED | 2 executions green |
| `connect-api-references.txt` | three accessors | VERIFIED | Lines 187-189 |
| `fork-anchors.txt`, `docs/FORK-DELTA.md` | anchors and row | VERIFIED | 148 rows, 0 bad |
| `35-BUF03-RECORD.md`, `35-ROLLOUT-ITEM.md`, `35-COMMAND-EVIDENCE.md` | records | VERIFIED | Content checked above |

### Key Link Verification

| From | To | Via | Status |
|------|----|-----|--------|
| `ClickHouseSinkTask.preCommit` | `ChunkFlusher.drainFlushedOffsets` | `AtLeastOnce/ExactlyOnce` strategy `preCommit` | WIRED |
| `ClickHouseSinkTask.close` | strategy `close` -> `forgetPartitions` | Connect's (original) partitions | WIRED |
| `ChunkFlusher.flush` | `ProxySinkTask.put` | `putDirect` before offset tracking (table routing keeps the transformed topic) | WIRED |

### Data-Flow Trace (Level 4)

Offsets flow from `SinkRecord.originalKafkaOffset()` to `flushedOffsets` to `drainFlushedOffsets()` to Connect. The unit and live pins assert the actual returned map, so the flow is FLOWING, not static.

### Behavioral Spot-Checks and Probe Execution

| Behavior | Command | Result | Status |
|----------|---------|--------|--------|
| Docker-free suite | `env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew testUnit --rerun` | 1458 tests, 0 failures, 2 skipped | PASS |
| Live pin | `./gradlew test --tests '*BufferedOffsetCommitLiveTest'` (latest, V2) | 4/0/0 | PASS |
| Anchors mirror | `35-evidence-check.py anchors .planning/scripts/fork-anchors.txt` | `rows=148 bad=0` | PASS |
| Tail merge gate | `PRE_TAG=pre-v1.7.0-tail-2 ... verify-merge-invariants.sh adaf8c4e HEAD` | exit 0, `anchor_rows=148 anchor_exempt=0 result=PASS` | PASS |
| Cumulative gate | not re-run (long); E-09 records exit 0 at `3e574320` | not re-verified | SKIPPED by instruction |
| Origin unchanged | `git ls-remote origin refs/heads/custom_build` | `3a3a15a9...` | PASS |
| Tag and branch absent on origin | `ls-remote` of `pre-v1.7.0-tail-2`, `sync/v1.7.0` | 0 and 0 | PASS |
| No external-environment names | `git grep -n -I -E` over the three forbidden environment strings named in the verification request | 0 matches before this file was written | PASS |
| Hygiene gate | `python3 -I 35-hygiene-check.py` over phase docs, FORK-DELTA, tests, fixtures, anchors | `OK hygiene: 18 files, 0 hits` and `7 files, 0 hits` | PASS |

### Requirements Coverage

| Requirement | Description | Status | Evidence |
|-------------|-------------|--------|----------|
| BUF-01 | `ChunkFlusher.flush` keys by original topic/partition, offset+1 | SATISFIED | Source and unit pins |
| BUF-02 | revocation drop and exactly-once buffer keyed by original | SATISFIED | Source and unit/live pins |
| BUF-03 | `Processing.doLogic` checked, result recorded | SATISFIED | `35-BUF03-RECORD.md`, tripwire; unedited by user decision |
| BUF-04 | Docker-free pins via `newRecord`, non-renamed unchanged | SATISFIED | 15 green executions |
| BUF-05 | live pin on latest x V2; Direct path and buffer contract unchanged | SATISFIED | 4 green live executions; existing buffer tests unedited |
| BUF-06 | real-environment rerun | OUT OF SCOPE | Performed outside the repository; not gated |
| BUF-07 | FORK-DELTA row, anchor, rollout item | SATISFIED | FORK-DELTA line 32, 7 anchors, `35-ROLLOUT-ITEM.md` |

No orphaned requirements: REQUIREMENTS.md maps BUF-01..BUF-07 to Phase 35 and each is claimed above.

### Anti-Patterns Found

No TBD/FIXME/XXX debt markers or stubs in the three edited main files (the diff is 5 added / 4 removed lines, all real logic). No blockers.

### Human Verification Required

None. BUF-06 is tracked outside this repository by user decision.

## Advisories (non-blocking, none counted against the score)

1. **Criterion 2 is accepted by user decision, not by the criterion's literal either/or.** The record is neither a fix nor a clean non-issue. It measures a real defect on the Direct path (`reportInsertedOffsets=true` under a renaming SMT commits nothing, OI-35-1) and a many-to-one state-key hazard (OI-35-2), and leaves both unfixed. Both are outside the operator's `exactlyOnce=false` buffered configuration.
2. **IN-04 is open** (`35-REVIEW.md`, disposition `open`): `ClickHouseSinkTask.close` passes Connect's original-key partitions to `ProxySinkTask.onPartitionRemoved`, whose `lastInsertedOffsets` is keyed by the transformed topic, so that removal is a no-op under a rename. It shares OI-35-1's root cause and the buffered paths ignore that map. Its recommended one-sentence addition to OI-35-1 in `35-BUF03-RECORD.md` was not made (the record has no mention of `onPartitionRemoved`; confirmed by reading it).
3. **IN-01, IN-02, IN-03, IN-05 are open** (info only). IN-05 notes the tripwire exercises `InMemoryState`, not `KeeperStateProvider`, and that value-routed fan-out is not covered by OI-35-3.
4. **First-start and Connect-side commit behavior are `derived`, not measured** (labelled as such in the rollout item). The live pin observes the `preCommit` map, not a broker consumer group.
5. **Cumulative merge gate not re-run** here by instruction; E-09 records PASS at `3e574320`, and the tail gate was re-run against current HEAD and passes.
6. WR-01 and WR-02 are dispositioned `fixed` (commits `b50b8452`, `b232c205`); the two extra executions are present in the 1458 count.

## Gaps Summary

No gaps. The three wrong-key sites are fixed at the source, no other wrong-key site exists in the buffered paths, the high-risk state/processing code is untouched since `6ac0cdc0`, existing buffer tests are unedited, and the Docker-free, live, anchor and tail-gate checks pass on the current tree. Origin is untouched and nothing is pushed.

---

_Verified: 2026-10-29_
_Verifier: Claude (gsd-verifier)_
