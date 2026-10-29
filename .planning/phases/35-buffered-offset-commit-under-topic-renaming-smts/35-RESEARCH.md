# Phase 35: Buffered Offset Commit Under Topic-Renaming SMTs - Research

**Researched:** 2026-10-29
**Domain:** Kafka Connect sink offset contract (`preCommit`/`close`) under topic-renaming SMTs, in the fork's buffered delivery strategies; red-first Docker-free pins, a live pin, the reporter's external stand, FORK-DELTA and fork-anchors bookkeeping
**Confidence:** HIGH (every code claim was read this session; the fix, the red-first pins, a live probe and the BLD-16 diff were all run on a throwaway clone of `ea0126fe`; the stand was inspected read-only)

<user_constraints>
## User Constraints (from CONTEXT.md)

### Locked Decisions
None. CONTEXT.md is "Auto-generated (discuss skipped via workflow.skip_discuss)". Its `## Decisions` section holds only the `### Claude's Discretion` block below.

### Claude's Discretion
All implementation choices are at Claude's discretion — discuss phase was skipped per user setting. Use ROADMAP phase goal, success criteria, and codebase conventions to guide decisions.

### Deferred Ideas (OUT OF SCOPE)
None — discuss phase skipped.

Phase boundary restated by CONTEXT.md and ROADMAP "Phase 35" (these are governing): with `bufferCount > 0` and a topic-renaming SMT such as `RegexRouter`, the connector commits consumer offsets for the partitions it was assigned; revoked partitions drop their buffered records; the exactly-once buffer groups by original partition; the non-buffered path and the non-renamed case behave exactly as before; `Processing.doLogic` is checked for the same defect and the outcome recorded, and **a state-key change stops for a user decision**; fixes upstream #777 on the fork (PR #786 closed unmerged). Lands on client 0.9.6, after the Phase 34 tail merge, before the client bump. The reporter's patch is an input, not a commit: tests are written red first. The stand lives outside the repo: run its scripts unedited and copy results into the phase record. Pushes nothing.
</user_constraints>

<phase_requirements>
## Phase Requirements

| ID | Description | Research Support |
|----|-------------|------------------|
| BUF-01 | `ChunkFlusher.flush` keys flushed offsets by `originalTopic()`, `originalKafkaPartition()`, `originalKafkaOffset() + 1` | Exact current code (L53-54) quoted; the reporter's patch applies to the post-tail tree unmodified (`git apply --check` clean at `ea0126fe`); all three accessors verified by `javap` in worker 7.9.7, 4.1.1, 4.4.0 and the stand's 7.9.10; red-first probe: preCommit keys `bufT-0, bufT-1` on unchanged code, `bufT-dev-0, bufT-dev-1` after the patch |
| BUF-02 | `AtLeastOnceBufferStrategy.close` and `ExactlyOnceBufferStrategy.put` use the original topic/partition | Probes: unchanged `close()` leaves the revoked partition's 3 records in the buffer (13 written instead of 10); unchanged EO `put` buckets by transformed TP so `close(orig TP)` removes nothing (20 written instead of 10); both fixed by the patch's two one-line edits |
| BUF-03 | `Processing.doLogic` checked for the same defect; result recorded | Measured with `InMemoryState` probes P1-P7 (section "BUF-03"): the state key (`topic-partition`, transformed) is NOT the offset-commit defect; `StateRecord.originalTopic` is memory-only and keys `lastInsertedOffsets`, which is a real defect for `DirectDeliveryStrategy` + `reportInsertedOffsets=true` (P6: `{}` committed); a many-to-one state-key hazard exists independent of this fix (P2-P4). Recommendation + STOP list below |
| BUF-04 | Docker-free unit pins built through `newRecord` with a different topic | `newRecord` recipe and the seam (`new ProxySinkTask(cfg, null, writer)` + `new ChunkFlusher(...)` + strategy) proven red on unchanged code; test shapes, `@DisplayName` rule, MBean `stop()` rule, no-`@Disabled` rule |
| BUF-05 | Live pin on `latest` x V2: committed offsets equal end offsets; Direct path and buffer contract unchanged | Harness decision with reasons (suite pin carries the guarantee through Phases 36/38; broker-level observation only via integrationTest/stand); live probe red on unpatched, green on patched (~18 s); existing 35 buffer tests green on the patched tree (71 s) |
| BUF-06 | Stand rerun on the fixed jar: 300000 records, 3 partitions, `buf-20k` and `buf-100k`, `docker kill kc_connect_new` | Exact inventory of the stand, current state (it holds the reporter's AFTER state and the reporter's patched jar), a no-script-edit run recipe, reset plan, flags needing user action |
| BUF-07 | FORK-DELTA row, anchor rows, rollout item with evidence label | Insertion point, row format, 7 measured anchor rows, the Phase 38 wording constraints, first-start-after-upgrade behaviour and how to measure it |
</phase_requirements>

## STOP-condition and premise check (done first)

| Check | Result | Evidence |
|---|---|---|
| Upstream `main` moved? | No: `9bed6f7970670a55c66be8efda246889e1f95bd0` | [VERIFIED: `git ls-remote upstream main`, 2026-10-29] |
| Upstream #777 resolved? | No: `state: OPEN`, title "Buffered sink offsets use transformed topic after topic-mutating SMT, causing commits for unassigned partitions" | [VERIFIED: `gh issue view 777 -R ClickHouse/clickhouse-kafka-connect`] |
| Upstream PR #786 | `CLOSED`, `mergedAt: null`, closedAt 2026-09-09. Maintainer thread: closed as stale because the PR used reflection for an old connect-api; he wrote the accessors "were introduced in 4.0.0" | [VERIFIED: `gh pr view 786 --json`]. The 4.0.0 claim is contradicted for the fork's runtime by `javap` below (present in the 7.9.7 worker jar); the PR body says "Kafka Connect 3.6+ runtimes" |
| PR #786 scope | touched `ClickHouseSinkTask.java`, a new `SinkRecordMetadata.java`, **`Processing.java` (+2/-1)**, two test files | [VERIFIED: `gh pr view 786 --json files`]. Upstream's attempt also touched the inserted-offsets reporting path (see BUF-03) |
| Repo state | `sync/v1.7.0` at `ea0126fe`; only `.planning/config.json` and `.planning/state.json` dirty (pre-existing). I modified nothing in the repo | [VERIFIED: `git status --short`] |

No STOP condition. The planner should re-run `git ls-remote upstream main` and the `gh` check at phase close, because BUF-07 words the fix as "fork-only until upstream resolves #777".

## Summary

The defect is exactly as the reporter describes, and it is three one-line sites. With `bufferCount > 0` and a renaming SMT, `ChunkFlusher.flush` keys `flushedOffsets` by `record.topic()` (the renamed topic), so `preCommit` returns keys Connect does not own and Connect ignores them; the group commits nothing. Two companion sites use the same wrong key for revocation: `AtLeastOnceBufferStrategy.close` and `ExactlyOnceBufferStrategy.put`/`close`. The reporter's patch changes exactly those three lines to `originalTopic()/originalKafkaPartition()/originalKafkaOffset()`. It applies cleanly to the post-tail tree (`git apply --check` exit 0 at `ea0126fe`), compiles, turns every red-first pin green, leaves the non-renamed control unchanged, and the existing 35 `ClickHouseSinkTaskBufferTest` cases stay green on it. The only collateral is BLD-16: three new `SinkRecord` members must be blessed (measured: derived members 138 to 141, all present in the production worker's 7.9.7 jar).

The live stand currently shows the "after" state of the reporter's patch (all four `connect-buf-*` groups at committed = end, 700,339 records in `bufT-dev`); `kc_connect_new` is running the reporter's patched jar (sha256 `13d904c4...`, hot-swapped into the running container, not baked into the image). The BUF-06 rerun must therefore replace that jar with the Phase 35 build and reset the stand's topic, groups and tables first. That is destructive to the user's stand state and needs the user's confirmation.

BUF-03 is not a "measured non-issue" and not a state-key problem. Findings: (1) the exactly-once state key (`<transformed topic>-<partition>`) is unchanged and out of scope here; (2) `StateRecord.originalTopic` is held in memory only (never written to KeeperMap) and keys `lastInsertedOffsets`; it is read from `getSinkRecord().topic()` (transformed), which makes `DirectDeliveryStrategy` with `reportInsertedOffsets=true` commit nothing under a renaming SMT (measured `{}`), a one-line fix in a high-risk file that would also change the Direct path BUF-05 says must be unchanged; (3) two source topics renamed to one target share one state record per partition and silently lose or reject records when their offsets are skewed (measured). (2) and (3) need user decisions; neither gates the at-least-once fix.

**Primary recommendation:** Apply the reporter's three edits as written (tests first, seen red), re-bless BLD-16 in the same fix commit, add 7 anchor rows and one FORK-DELTA row in that same commit, pin the guarantee in the `test` suite (Docker ClickHouse, survives Phases 36/38 suite gates), observe real broker offsets once with the stand (and optionally an `integrationTest` class), and record BUF-03 as measured evidence with a user-decision checkpoint for the Direct/`reportInsertedOffsets` one-liner. Do not edit `Processing.java` or `KeeperStateProvider.java` without that decision.

## Architectural Responsibility Map

Tiers here are connector layers, not web tiers.

| Capability | Primary Tier | Secondary Tier | Rationale |
|------------|-------------|----------------|-----------|
| Which `(topic, partition)` the consumer group owns, and committing offsets for it | Kafka Connect `WorkerSinkTask` (keys `currentOffsets` by the consumer record's own topic/partition) | `ClickHouseSinkTask.preCommit`/`close` (must speak the same keys) | The connector may only return keys the worker assigned, i.e. the ORIGINAL topic/partition |
| Offsets of data actually written (buffered paths) | `ChunkFlusher.flushedOffsets` | `AtLeastOnceBufferStrategy`, `ExactlyOnceBufferStrategy` (drain/forget) | One map, keyed once, in `flush`; both strategies share it |
| Revocation: drop buffered records of a revoked partition | `AtLeastOnceBufferStrategy.close`, `ExactlyOnceBufferStrategy.close` | `ChunkFlusher.forgetPartitions` | Connect hands `close()` original partitions |
| Table routing / database split | `Record.convert`, `RecordConvertor` (transformed `sinkRecord.topic()`) | `ProxySinkTask.batchKey` | Table names come from the renamed topic on purpose; must NOT change |
| Exactly-once state (BEFORE/AFTER) | `Processing.doLogic` + `StateProvider` keyed by transformed `topic-partition` | `KeeperStateProvider` (KeeperMap key = `topic-partition`) | High-risk; untouched by this phase unless the user decides |
| Real group offsets at the broker | Kafka broker (`__consumer_offsets`) | observed only by the Connect-worker harness or the stand | Not observable from `test`-suite code |

## Standard Stack

No new library, version bump or build-file change. The client stays 0.9.6 (Phase 36 owns the bump). [VERIFIED: ROADMAP Phase 35 text; no `build.gradle.kts`/catalog edit is needed by the patch]

| Item | Version | Purpose |
|---|---|---|
| Kafka Connect API (compile) | 4.4.0 | `SinkRecord.originalTopic()/originalKafkaPartition()/originalKafkaOffset()` |
| Worker runtime reference | `connect-api-7.9.7-ccs.jar` sha256 `b57546e9160d5435238fded6f38756d621fac62bd9f64737c3f68e094ae01de4`; `kafka-clients-7.9.7-ccs.jar` sha256 `28423086fbbf8f91b2f1d335eeeca6675916e24adf2f813b3f9c47f87b49f962` | Production worker image `confluentinc/cp-kafka-connect-base:7.9.7`, extracted this session, hashes equal the header of `src/test/resources/connect-api-references.txt` |
| JUnit 5, Mockito, Testcontainers | existing | Tests |

**Package Legitimacy Audit:** not applicable, this phase installs no external package. Packages removed due to SLOP verdict: none. Packages flagged SUS: none.

### SinkRecord accessors (BUF-01/02, item 1 of the brief)

`javap -cp <jar> org.apache.kafka.connect.sink.SinkRecord`, identical output in all four jars [VERIFIED: javap run this session on worker 7.9.7-ccs, gradle-cache 4.1.1, gradle-cache 4.4.0, and the stand image `kc_test/connect:new` 7.9.10-ccs]:

```
public java.lang.String originalTopic();
public java.lang.Integer originalKafkaPartition();
public long originalKafkaOffset();
```

The 7.9.7 jar also has the 13-argument constructor `SinkRecord(String, int, Schema, Object, Schema, Object, long, Long, TimestampType, Iterable<Header>, String, Integer, long)` (last three = original topic, partition, offset) and `newRecord(String, Integer, Schema, Object, Schema, Object, Long)` returning `SinkRecord`. [VERIFIED: same javap]

Consequences:
- Minimum worker for the fixed jar = a Connect runtime that has these accessors. PR #786's own body says 3.6+ [CITED: gh pr view 786]. The fork's production worker (7.9.7, Kafka 3.9 line) has them [VERIFIED]. On an older worker the first buffered flush would throw `NoSuchMethodError`; BLD-16 (blessed against the 7.9.7 jars) is the guard that proves the fork's floor. Say so in the FORK-DELTA row.
- `originalKafkaPartition()` returns `Integer`: `new TopicPartition(String, int)` auto-unboxes it (no allocation; the same unboxing as the existing `record.kafkaPartition()`).
- Records built with the plain 7-argument constructor have `original* == current*`, so every existing test that builds records that way is the "not renamed" control for free. [VERIFIED: probe control: `notRenamedControl` passes on both trees]

## Architecture Patterns

### System Architecture Diagram

```
Kafka partition (bufT-dev, p) --consumer.poll--> WorkerSinkTask
   currentOffsets key = (bufT-dev, p)  <--- the ONLY keys Connect accepts back from preCommit
        |
        v  SMT chain: RegexRouter ... SinkRecord.newRecord(topic = "bufT", ...)
        |      record.topic() = "bufT"   record.originalTopic() = "bufT-dev"
        v
ClickHouseSinkTask.put --> DeliveryStrategy
   |-- DirectDeliveryStrategy ---------> ChunkFlusher.putDirect --> ProxySinkTask.put --> Processing.doLogic --> ClickHouseWriter
   |        preCommit: returns Connect's currentOffsets (default), or getInsertedOffsetsSnapshot() (reportInsertedOffsets=true)
   |-- AtLeastOnceBufferStrategy: flat List<SinkRecord>, flush at size/time ----\
   |-- ExactlyOnceBufferStrategy: Map<TopicPartition,List<SinkRecord>> buckets --+--> ChunkFlusher.flush(chunk)
                                                                                 |     putDirect(chunk)  (table = TRANSFORMED topic: unchanged)
                                                                                 |     flushedOffsets[TP(<<KEY>>)] = offset + 1     <<< BUF-01
   WorkerSinkTask.commitOffsets --> task.preCommit(currentOffsets) --> drainFlushedOffsets()
        keys not in the assignment are dropped: "Ignoring invalid task provided offset ... partition not assigned"
   rebalance: onPartitionsRevoked -> commitOffsets(closing=true) then task.close(revoked ORIGINAL TPs)
        --> strategy.close(TPs): buffer.removeIf / perPartitionBuffer.remove   (keys must be ORIGINAL)   <<< BUF-02
```

The Connect behaviour in the diagram is cited from the 3.9-branch `WorkerSinkTask` source (offsets keyed from the `ConsumerRecord`'s `topic()/partition()`, "Ignoring invalid task provided offset" for non-assigned partitions, `close()` receives the revoked partitions after a closing commit). [CITED: raw.githubusercontent.com/apache/kafka/3.9/connect/runtime/.../WorkerSinkTask.java, read through a summarising fetch; line numbers approximate]. The live stand corroborates it: with the reporter's patch all buffered groups show `committed=end` [VERIFIED: `scripts/offsets.sh` read-only, below].

### Where offsets are keyed by the transformed topic/partition (item 1)

| # | Site | Verbatim current code | Verdict |
|---|---|---|---|
| 1 | `ChunkFlusher.flush` L53-54 | `TopicPartition tp = new TopicPartition(record.topic(), record.kafkaPartition());` / `long offset = record.kafkaOffset() + 1; // committed offset = next offset to consume` | **Defect** (BUF-01): key returned by `preCommit` |
| 2 | `AtLeastOnceBufferStrategy.close` L86 | `buffer.removeIf(record -> partitions.contains(new TopicPartition(record.topic(), record.kafkaPartition())));` | **Defect** (BUF-02): revoked records stay buffered and are flushed later |
| 3 | `ExactlyOnceBufferStrategy.put` L43 | `TopicPartition tp = new TopicPartition(r.topic(), r.kafkaPartition());` | **Defect** (BUF-02): buckets keyed transformed, so `close()`'s `perPartitionBuffer.remove(tp)` (L74, `tp` from Connect) never matches |
| 4 | `ChunkFlusher.forgetPartitions` L100-102 | `flushedOffsets.keySet().removeAll(partitions);` | Correct once site 1 is fixed (keys become original); no edit |
| 5 | `DirectDeliveryStrategy.preCommit` L37-54 | returns `currentOffsets`, or `proxySinkTask.getInsertedOffsetsSnapshot()` filtered with `inserted.keySet().removeIf(key -> !currentOffsets.containsKey(key))` | Default path correct (Connect's own keys). `reportInsertedOffsets=true` path is wrong under rename, see BUF-03 (P6) |
| 6 | `Processing.doLogic` L137 | `String originalTopic = record.getSinkRecord().topic();` -> `StateRecord(..., originalTopic)` -> `BaseStateProviderImpl.onStateUpdate` L27 `new TopicPartition(stateRecord.getOriginalTopic(), stateRecord.getPartition())` | Misnamed (reads the transformed name). Feeds only `lastInsertedOffsets` (site 5) |
| 7 | `Record.convert`/`RecordConvertor` L10 `String topic = sinkRecord.topic();`, `ProxySinkTask.put` grouping | table name and database split | **Correct on purpose**; the table is the renamed topic. Must NOT be changed |

[VERIFIED: `cat -n` of all five files this session; `KeeperStateProvider.setStateRecord` L139-157 writes only `key, minOffset, maxOffset, state`, so `originalTopic` is never persisted]

`ClickHouseSinkTask.preCommit` L131-137 and `close` L140-148 only delegate (`deliveryStrategy.preCommit(currentOffsets)`; `proxySinkTask.onPartitionRemoved` then `deliveryStrategy.close`); no edit there.

### The reporter's patch (input only)

`~/dev/clickhouse_mvp/local_kc_test/patches/buffer-commit-original-topic.patch` is a `diff -u` of three files, one hunk each, four changed lines:

```
ChunkFlusher.java:                  new TopicPartition(record.originalTopic(), record.originalKafkaPartition());
                                    long offset = record.originalKafkaOffset() + 1; // committed offset = next offset to consume
AtLeastOnceBufferStrategy.java:     buffer.removeIf(record -> partitions.contains(new TopicPartition(record.originalTopic(), record.originalKafkaPartition())));
ExactlyOnceBufferStrategy.java:     TopicPartition tp = new TopicPartition(r.originalTopic(), r.originalKafkaPartition());
```

- Applies to the post-tail tree: `git apply --check -p1 <patch>` printed `APPLIES_CLEAN` at HEAD `ea0126fe` [VERIFIED]. The hunk offsets in the patch match the tail's `ChunkFlusher` (it was authored 2026-10-28 17:33, after the Phase 34 tail merge existed in the working tree).
- It carries no tests, no anchors, no FORK-DELTA row, no BLD-16 re-bless. All of that is Phase 35's job.
- Allocation: identical to today (one `TopicPartition` per record in `flush`, one per record in EO `put`, one per buffered record per `close`). No new per-record allocation. Keep exactly these lines; do not "optimise" (roadmap note: no per-record allocation beyond the `TopicPartition` already built).
- The executor should still type the three edits by hand (or `git apply`) and treat the patch as a reference, per CONTEXT ("an input, not a commit").

### Recommended commit sequence (all on `sync/v1.7.0`, nothing pushed)

| # | Commit | Contents | Gate |
|---|---|---|---|
| 1 | `test(35)` red pins | Docker-free unit class + live `test`-suite class; run against unchanged `src/main`; record the failing set with the evidence helper (`34-evidence-check.py failed ... --expect`, names are `@DisplayName` text) | red set exactly the BUF pins; every control green |
| 2 | `fix(35)` the fix commit | the three main-file edits **+** `src/test/resources/connect-api-references.txt` re-bless **+** 7 new rows in `.planning/scripts/fork-anchors.txt` **+** the `docs/FORK-DELTA.md` row (roadmap criterion 5: "The fix commit carries a FORK-DELTA row and a fork-anchors.txt anchor") | cache-busted `testUnit` green; anchors mirror `bad=0`; tail-style gate invariant 3 passes at this commit |
| 3 | `test(35)` mutation proof (record only) | detached worktree: revert each of the three edits one at a time, each pin must go red (Phase 34 E-08/E-09 pattern); remove the worktree | evidence entry |
| 4 | `docs(35)` Processing record (BUF-03) | probe evidence, decision checkpoint outcome | evidence entry |
| 5 | `docs(35)` stand + first-start + rollout item (BUF-06/07) | stand outputs copied verbatim, jar SHA-256, the rollout-item file | evidence entry |
| 6 | gate | cache-busted `testUnit`, one full `test --rerun` on `latest` x V2, anchors invariant | phase gate |

Anchors may only appear in the same commit as, or after, the code they pin (gate invariant 3 evaluates each commit against its own copy of `fork-anchors.txt`; 34-RESEARCH). BLD-16 goes red the instant the main edit lands without the re-bless, so they travel together in commit 2 (Phase 34's separate commits existed only because its merge commit had to stay mechanical).

### Recommended Project Structure (additive only; no existing test is edited)

```
src/test/java/com/clickhouse/kafka/connect/sink/
├── BufferedOffsetCommitUnitTest.java            # NEW, extends nothing, Docker-free (BUF-01/02/04)
├── BufferedOffsetCommitLiveTest.java            # NEW, extends ClickHouseBase (BUF-05)
└── processing/ProcessingRenamedTopicStateKeyUnitTest.java   # NEW, InMemoryState (BUF-03 regression guard)
src/testFixtures/.../helper/RenamedTopicRecords.java          # NEW optional shared builder (DRY between the unit and live class)
src/integrationTest/java/.../BufferedRegexRouterOffsetCommitIntegrationTest.java   # OPTIONAL, see "Where the live pin lives"
```

## Where the live pin lives (Research flag A, item 3)

**Decision: the BUF-05 pin is a `test`-suite class on ClickHouse (Docker ClickHouse only). A one-time broker-level observation comes from the stand (BUF-06) and, optionally, a new additive `integrationTest` class.**

Why the `test` suite is the primary home:
1. ROADMAP Phase 35 Note: "The BUF-05 live pin then carries the guarantee across the client bump through the suite gates of Phases 36 and 38." Those gates are `testUnit` plus full `test --rerun` on `latest` x V2. `integrationTest` is not in any gate; it runs only in the fork-owned nightly (dispatch, or schedule while `NIGHTLY_ENABLED=true`). [VERIFIED: `.github/workflows/nightly-integration-tests.yaml` L1-27; 34-COMMAND-EVIDENCE E-11 gate list]
2. What the pin must observe is "the keys and values `preCommit` returns", because that is the whole defect: Connect commits exactly what the task returns, minus the keys it does not own. A `ClickHouseSinkTask` + real ClickHouse + records built through `newRecord` observes that value. The pin additionally asserts `result.keySet()` is a subset of the assigned original partitions (emulating Connect's "partition not assigned" filter) and `offset == end offset`. It reads the rows back and `assertEquals` them against the input (AGENTS.md testing rule).
3. Cost: measured live probe (new class on ClickHouse `latest`, V2) = 18 s per run; the existing 35-test `ClickHouseSinkTaskBufferTest` class = 1 m 11 s. [VERIFIED: runs this session]

Why that is not enough for the literal words "committed offsets": a `test`-suite pin cannot see the broker's group state. Real committed offsets are observable only with a broker plus a Connect worker:
- **The stand (BUF-06)**: `scripts/offsets.sh` runs `kafka-consumer-groups --describe --group connect-<name>` and prints `committed`, `end`, `lag`.
- **`integrationTest` harness (exists, runs locally)**: `ConfluentPlatform` starts zookeeper, `cp-server`, schema registry, a Java 17 Connect worker (`confluentinc/cp-server-connect:7.9.6` built with `ImageFromDockerfile`), control center, ksqlDB and REST proxy; the connector archive is copied from `build/confluentArchive`. [VERIFIED: `ConfluentPlatform.java` L31-260]. The whole task ran green locally on `latest` x V2 with the Java 17 worker: 55 tests, 54 passed, 1 skipped, 29.5 min, class setup about 95 s warm. [VERIFIED: `.planning/milestones/v1.7.0_gg5-ci-phases/31-fork-owned-nightly-integration-tests/31-COMMAND-EVIDENCE.md` E-18] All needed images are local (`cp-server-connect:7.9.6`, `cp-server`, `cp-kafka-rest`, `cp-schema-registry`, `cp-zookeeper`, `cp-enterprise-control-center`, `cp-ksqldb-server`, `cnfldemos/cp-server-connect-datagen:0.6.4-7.6.0`, `toxiproxy:2.7.0`) [VERIFIED: `docker images`]. No test in that source set reads consumer-group offsets today: `ConfluentPlatform.getOffset` returns the topic's `end_offset` via REST proxy (L474-492), not a committed offset.

Recommended optional class (planner's call; discretion): `BufferedRegexRouterOffsetCommitIntegrationTest` with its own `@BeforeAll` platform (about 95 s), one connector JSON (new resource, `RegexRouter` + `bufferCount`, modelled on `clickhouse_sink_no_proxy_buffer.json`), produce through `produceRecordsViaRest` (or datagen), read committed offsets by one new public helper on `ConfluentPlatform` (the container field `cp_server` is package-private in `...sink.helper`, so a helper method is required: either `cp_server.execInContainer("kafka-consumer-groups", "--bootstrap-server", "broker:29092", "--describe", "--group", "connect-<name>")` or the REST proxy v3 consumer-group lag endpoint [ASSUMED: v3 lags endpoint in kafka-rest 7.9.6, verify before relying on it]). Adding a method to `ConfluentPlatform.java` does not move any of its 3 anchor rows (`confluentinc/cp-server-connect:` 1, `new ImageFromDockerfile(` 1, `new GenericContainer<>(CP_DATA_GEN)` 0). [VERIFIED: fork-anchors.txt L148-150 and the file's structure] Red-first for this class = run it against an archive built from the unfixed tree: the group shows no committed offset.

Because the stand covers the same observable and is the roadmap's BUF-06 criterion, the optional class is a cost/benefit call: it is the only repo-resident, re-runnable broker-level observer. I recommend including it if the plan has budget; it must not be a gate dependency.

## Red-first test design (Research items 2 and 4)

### Existing coverage (all Docker-backed, none builds a renamed record)
- `ClickHouseSinkTaskBufferTest` (1007 lines, extends `ClickHouseBase`, `@Tag("clickhouse")`) covers buffers, `preCommit*`, `strictChunking*` over a real table and `SchemalessTestData.createPrimitiveTypes(topic, partition, n)` records (7-arg-constructor style, so `original* == current*`). 35 tests, all green on the patched clone: `OK junit build/test-results/test: tests=35 failures=0 errors=0 skipped=0 files=1`, BUILD SUCCESSFUL in 1m 11s [VERIFIED].
- `ClickHouseSinkTaskTest` L419-625: `preCommit*` for the Direct path (`reportInsertedOffsets`, `ignorePartitionsWhenBatching`). Docker-backed.
- `WriterFailureClassificationUnitTest` and `AdditionalRetriableErrorCodesTest` already drive `new ChunkFlusher(proxySinkTask, config, null)` Docker-free. [VERIFIED: grep]. There is **no** Docker-free test of `preCommit`, `close()` or `flush` offsets.
- `ProcessingTest` has `dbTopicSplitUsesProcessedTopicForStateOffsets` (L287-311) which pins `lastInsertedOffsets` keyed by `sinkRecord.topic()` when it is NOT renamed. It stays green under any `originalTopic()` change to Processing (verified in the P6 variant below).

### Docker-free seam (proven)
`ClickHouseSinkTask.start` builds a real `ClickHouseWriter` and connects, so a Docker-free test cannot go through the task. Use the Phase 34 seam instead:

```java
// Source: probe /tmp/gsd35probe/repo/.../Buf35ProbeUnitTest.java (ran red on unchanged code, green after the patch)
static SinkRecord renamed(String origTopic, int partition, long offset, String newTopic) {
    Map<String, Object> v = new HashMap<>();
    v.put("id", offset);
    SinkRecord r = new SinkRecord(origTopic, partition, null, null, null, v, offset,
            1700000000000L, org.apache.kafka.common.record.TimestampType.CREATE_TIME);
    return r.newRecord(newTopic, partition, null, null, null, v, r.timestamp());   // originals preserved
}
ClickHouseSinkConfig c = new ClickHouseSinkConfig(new HashMap<>(Map.of(ClickHouseSinkConfig.BUFFER_COUNT, "10")));
ProxySinkTask t = new ProxySinkTask(c, null, countingWriter);           // package-private seam ctor (Phase 34)
AtLeastOnceBufferStrategy s = new AtLeastOnceBufferStrategy(new ChunkFlusher(t, c, null), 10, 0);
```

Gotchas measured while building the probe:
- Give the record a **timestamp** (the 9-argument constructor). With a null timestamp, `newRecord(..., r.timestamp())` threw `NullPointerException: Cannot invoke "java.lang.Long.longValue()" because the return value of "...SinkRecord.timestamp()" is null`.
- A `Map` value with a null value schema routes through `SchemalessRecordConvertor`, needs no ClickHouse table, and `InMemoryDBWriter`/a counting `DBWriter` receives the records. `InMemoryDBWriter` stores by offset only (`recordMap.put(offset, r)`), so use a small counting writer when two partitions share offsets.
- Statistics MBeans: `Processing`-level probes created `SinkTaskStatistics(null, 990)` repeatedly and hit `InstanceAlreadyExistsException ... topic=t`; use unique ids, and for seam-built `ProxySinkTask`s call `stop()` in a `finally` (Phase 34 WR-01, `ad38ab27`: `stop()` unregisters task and topic MBeans).
- Put `@DisplayName` on every test; `34-evidence-check.py failed --expect Class#name` reads the JUnit XML testcase name, which is the DisplayName text.
- The class must extend nothing (extending `ClickHouseBase` inherits `@Tag("clickhouse")` and drops out of `testUnit`).
- Do NOT add `@Disabled` or an `Assumptions` skip anywhere: `34-evidence-check.py skips` requires the skipped testcases be exactly the 18 named in FORK-DELTA "Named skip inventory".

### Measured red-first results (unchanged code at `ea0126fe` vs patched), item 2/4

| Pin | Unchanged tree | Patched tree |
|---|---|---|
| BUF-01 `preCommit` keys (7 records p0, 6 records p1, bufferCount 10) | `preCommit keys = [bufT-0, bufT-1]` values `bufT-0=7, bufT-1=106`; test FAILED with `NullPointerException ... Map.get(...) is null` for key `bufT-dev-0` | keys `[bufT-dev-0, bufT-dev-1]`, values `bufT-dev-0=7, bufT-dev-1=106` |
| BUF-02 at-least-once `close` (3+3 buffered, revoke `bufT-dev-1`, then 7 more of p0 triggers the flush) | `written` has 13 records, including `bufT-dev:1:0..2` (revoked partition's records written); `expected: <10> but was: <13>` | 10 records, none from p1 |
| BUF-02 exactly-once grouping (same sequence, EO strategy) | 20 written, includes the 3 stale p1 records plus 7 new in one chunk; `expected: <10> but was: <20>`; EO `preCommit = {bufT-0=10, bufT-1=10}` | 10 written (p0 only); EO `preCommit = {bufT-dev-0=10}` |
| Control, not renamed | `{bufT-0=10}` | `{bufT-0=10}` (unchanged) |
| BUF-05 live probe (`ClickHouseSinkTask`, real ClickHouse `latest`, V2, 3 partitions x 100 records renamed `<t>-dev` to `<t>`, bufferCount 300) | rows 300 OK, `preCommit={<t>-0=100, <t>-1=100, <t>-2=100}`, assertion on `<t>-dev-p` FAILED with NPE (18 s) | `preCommit={<t>-dev-2=100, <t>-dev-1=100, <t>-dev-0=100}`, BUILD SUCCESSFUL (18 s) |
| BLD-16 `ConnectApiReferencesUnitTest` | green | **red** until re-blessed (3 new `M` lines, below) |
| Existing Docker-free classes on the patched clone (`WriterFailureClassificationUnitTest`, `AdditionalRetriableErrorCodesTest`, `AdditionalRetriableErrorCodesPinUnitTest`, `ProxySinkTaskConstructorUnitTest`, `ProcessingTest`) | n/a | `tests=256 failures=0 errors=0 skipped=0 files=5` |

[VERIFIED: probe runs this session in `/tmp/gsd35probe/repo` (throwaway clone, outside the repo; `Buf35ProbeUnitTest`, `Buf35LiveProbeTest`)]

### Pins the plan should write (names are suggestions)
Unit class `BufferedOffsetCommitUnitTest` (extends nothing):
1. `BUF-01` preCommit keyed by `originalTopic()/originalKafkaPartition()`, value `originalKafkaOffset() + 1`, over two partitions, offsets not starting at 0.
2. `BUF-01` independence: one test built with the **13-argument constructor** where transformed partition and offset differ from the originals (a partition-changing SMT); a fix that used only `originalTopic()` would stay red. Signature verified in the 7.9.7 jar (above).
3. `BUF-02` AtLeastOnce `close(orig TP)` drops that partition's buffered records and keeps the other's (observe via the next flush's written set, not by reflection on the private buffer).
4. `BUF-02` ExactlyOnce: buckets by original TP (observe by chunk composition: a revoked partition's stale records must not appear in a later chunk) and `preCommit` per original TP.
5. `BUF-01/02` `forgetPartitions` consistency: flush, `close([orig TP])`, `preCommit` returns no entry for it (before the fix the stale transformed key survived).
6. Control: records not renamed produce identical keys/offsets/drops to today (assert against both strategies).
7. Direct path unchanged (BUF-05): `new DirectDeliveryStrategy(...).preCommit(current)` returns the same `currentOffsets` instance when `reportInsertedOffsets` is false (default `Boolean.FALSE`, ClickHouseSinkConfig L99). Do not pin the `reportInsertedOffsets=true` rename result (it is a defect, P6).

Live class `BufferedOffsetCommitLiveTest extends ClickHouseBase` (suffix `LiveTest` is the repo convention, e.g. `AsyncInsertLiveTest`): the probe above plus content read-back, plus the Connect-validity assertion (returned keys are a subset of the assigned original partitions), plus a Direct-path control through `ClickHouseSinkTask` with `bufferCount` unset returning `currentOffsets` unchanged. Exactly-once live is out of scope (needs a Keeper cluster, skipped by assumption in this harness).

Mutation proof (record, do not commit): in a detached worktree revert each of the three edits separately; each must turn exactly its pins red.

## BUF-03: `Processing.doLogic` (Research flag B, item 4)

Measured with `InMemoryState` + a counting `DBWriter` + real `Processing` (probes P1-P7 in `Buf35ProbeUnitTest`, `/tmp/gsd35probe/repo`, throwaway). All on the **unchanged** `Processing`/`ProxySinkTask`.

| Probe | Setup | Observation |
|---|---|---|
| P1 | two sources `a-dev`, `b-dev` renamed to `t`, partition 0, offsets 0..199 each, interleaved in two batches | 400 of 400 written; one shared state record `t-0` (`lastInsertedOffsets={t-0=200}`). Equal offset spaces cause no loss |
| P2 | batch 1 = `a-dev` 5000..5099; batch 2 = `a-dev` 5100..5199 + `b-dev` 100..199 | written `a=200`, **`b=0` of 100 offered**: `dropRecords(stateRecord.getMinOffset(), ...)` filters the lower-offset source silently |
| P3 | state `a-dev` [5000,5099] AFTER; batch `b-dev` 10..109, `tolerateStateMismatch=false` (default) | `RuntimeException AFTER_PROCESSING: State MISMATCH as batch already processed - skipping [100] records for t...` (task failure) |
| P4 | same with `tolerateStateMismatch=true` | `b written = 0 of 100 offered` (silent skip) |
| P5 | 10 records `bufT-dev` renamed to `bufT`, partition 0, through `Processing` | `lastInsertedOffsets` key = `bufT-0` (the transformed name) |
| P6 | `DirectDeliveryStrategy` with `reportInsertedOffsets=true`, renamed records, `currentOffsets` keyed `bufT-dev-0` | `preCommit={}` (nothing committed) |
| P6 control | same, `currentOffsets` keyed by the transformed topic | `{bufT-0=10}` (only matches when keys are wrong) |
| P6 + one line | `Processing.java` L137 changed to `record.getSinkRecord().originalTopic()` (in the clone only; stashed and reverted) | `preCommit={bufT-dev-0=10}`; `ProcessingTest` stays green |
| P7 | Direct, default `reportInsertedOffsets=false`, renamed records | `{bufT-dev-0=10}` (Connect's own `currentOffsets` returned) |

[VERIFIED: runs this session]

Answers to the roadmap's questions:
- **Can two source topics mapped to one target share one state record?** Yes. The state key is `<transformed topic>-<partition>` for both `InMemoryState` (`genKey`: `String.format("%s-%d", topic, partition)`) and `KeeperStateProvider` (`key = String.format("%s-%d", topic, partition)`, written as `INSERT ... VALUES ('<key>', min, max, '<state>')`). `ProxySinkTask.batchKey()` also batches by that key unless `ignorePartitionsWhenBatching` is on without exactly-once. So `a-dev` p0 and `b-dev` p0 renamed to `t` share `t-0`.
- **What does the state machine do with interleaved ranges?** Equal offset spaces: nothing visible (P1). Skewed offset spaces within one task lifetime: records of the source with the lower offsets are dropped (`NEW`/`OVER_LAPPING` branches via `dropRecords`, P2), rejected (`PREVIOUS`, P3), or silently skipped with `tolerateStateMismatch=true` (P4). Offset 0 resets the state via the `ZERO` branch. This applies with `exactlyOnce=false` too, because `doLogic` runs the state machine against `InMemoryState` unless `ignorePartitionsWhenBatching=true` and `exactlyOnce=false`. It is independent of buffering (P2-P4 call `Processing` directly), it exists today on the Direct path, and it is upstream's design (state keyed by table-topic).
- **Does the Phase 35 BUF fix interact with it?** The fixed `ExactlyOnceBufferStrategy` now buckets many-to-one sources separately, so each flushed chunk covers one source's offset range, while `Processing` still keys by `t-0`. Exactly-once with a many-to-one rename was already unsound; the fix does not make it sound or worse in a measured way. Document that exactly-once requires an injective rename.
- **Is there a KeeperMap migration question?** Only if the state key changed. This research recommends no key change. If the user ever chooses one (for example adding the original topic to the key), the rows stored under `<transformed>-<partition>` are orphaned: the first batch after the upgrade reads `NONE` and inserts as new; old rows stay in the `KeeperMap` table forever (no TTL in `KeeperStateProvider`). That is a high-risk change by AGENTS.md ("Key Files": `Processing.java`, `KeeperStateProvider.java`) and, per ROADMAP, **STOPS for a user decision before it lands**.

### BUF-03 recommendation

1. **State key and exactly-once state: no change in Phase 35.** Record P1-P4 as measured evidence (a many-to-one defect class that predates this phase and is independent of buffering). Do not add a green test that pins the loss (P2-P4 pin a defect). Do add a Docker-free regression guard that pins the part that must NOT move: after `doLogic` over renamed records, `InMemoryState.getStateRecord("t", 0)` is `AFTER_PROCESSING` and `getStateRecord("t-dev", 0)` is `NONE` (class `ProcessingRenamedTopicStateKeyUnitTest`, new file; `ProcessingTest` stays untouched), plus P1 (equal-offset interleave writes everything). This makes any future key change trip a test, which is the "stop for a user decision" tripwire.
2. **`StateRecord.originalTopic` / `lastInsertedOffsets` (Direct + `reportInsertedOffsets=true`): present as a user decision at plan time (checkpoint).** Facts for the decision: the defect is real and measured (P6: zero commits, no `Ignoring invalid task provided offset` guard to save the user); the fix is one line in `Processing.java` L137 and does not touch the state key, the KeeperMap schema or the BEFORE/AFTER sequence (the variable is only passed as the last constructor argument of `StateRecord`; `originalTopic` is not persisted); upstream's PR #786 touched this file for the same reason; `reportInsertedOffsets` defaults to false and the operator's templates set neither it nor any buffer key [VERIFIED: grep of `~/repo/neo/data-engineering/kafka_connectors/templates/*.jinja` shows only `exactlyOnce: "false"`]; the stand's `buf-20k`/`buf-100k` overrides set `reportInsertedOffsets: "true"`, which is ignored by buffered strategies. Cost of doing it: Processing.java is a high-risk file; it changes the Direct path that BUF-05 says stays unchanged (BUF-05's "unchanged" means "for the default configuration and the non-renamed case"). Recommended default if the user agrees: a separate commit `fix(35): ...lastInsertedOffsets under original topic` with its own red-first pin and an anchor row, not folded into the BUF-01/02 fix commit; if the user declines, record it as a measured open item for the milestone close (owner, re-open trigger "operator sets `reportInsertedOffsets=true` with a renaming SMT").
3. **Many-to-one state-key hazard: record and escalate**, no Phase 35 code. State the operator topology assumption: the rendered templates give one jurisdiction per connector, so two source topics collapsing to one table does not arise from the template alone [ASSUMED: from reading `topics.regex` in `clickhouse-sink-kt-json-v2.jinja`; not verified against the operator's real topic list].

## Research flag C and the stand (items 5 and 6)

### What the stand is (read, not run)
`~/dev/clickhouse_mvp/local_kc_test` (own git-ignored dirs `build/`, `results/`, `connectors/rendered/`). A docker compose project `kc_test` with Kafka (`cp-kafka:7.9.10`, KRaft), schema registry, ClickHouse `26.3`, MySQL, and two Connect workers: `connect-old` (:8083, `kc_connect_old`, sink `1.2.9_gg4`, jar sha256 `b9821967...`) and `connect-new` (:8084, `kc_connect_new`, sink `v1.7.0_gg5`). Connectors are rendered from `connectors.yaml` with the real templates in `~/repo/neo/data-engineering/kafka_connectors/templates` and PUT with `scripts/render.py deploy [names]`. [VERIFIED: README.md, Makefile, docker-compose.yaml, connectors.yaml, scripts/*]

Buffer experiment: topic `bufT-dev` (3 partitions), one table per variant in database `buf` (`kt_buf_old`, `kt_buf_base`, `kt_buf_20k`, `kt_buf_100k`, MergeTree `ORDER BY (seq, __version)`), connectors `buf-old` (old worker, no buffer), `buf-base` (gg5, no buffer), `buf-20k` (`bufferCount 20000`, `bufferFlushTime 5000`, `reportInsertedOffsets true`), `buf-100k` (`100000`, `30000`, `true`). Template facts that matter: `RegexRouter` regex `^(.*?)(?:-(?:leonbets|leonru|slott|sunny|common|isnl))?(?:-(?:dev|rc))?$` replacement `$1` so `bufT-dev` becomes `bufT`; `auto.offset.reset` renders `earliest`; `partition.assignment.strategy` is `CooperativeStickyAssignor`; `max.poll.records` 10000; `exactlyOnce` false; DLQ topic `dlq-kt-common-dev`. Worker env `CONNECT_OFFSET_FLUSH_INTERVAL_MS: 10000` and `KAFKA_HEAP_OPTS -Xms512m -Xmx1536m`.

Scripts used by the criterion:
- `scripts/offsets.sh` (verbatim): for each of `buf-old buf-base buf-20k buf-100k` runs `docker exec kc_kafka0 kafka-consumer-groups --bootstrap-server kafka0:29092 --describe --group connect-$g` and prints `p<partition> committed=<n|none> end=<n> lag=<n>`.
- `scripts/buf_report.sh [since]`: rows and parts per variant, INSERT statements from `system.query_log`, **duplicates by seq (`count()` vs `uniqExact(seq)`)**, content equality vs `kt_buf_base`.
- `scripts/produce.py buf [bulk_rows]` writes 13 edge-case rows plus `bulk_rows` generic rows to `bufT-dev` through `kafka-console-producer` with headers; **total = 13 + bulk**. For exactly 300000 records use `python3 scripts/produce.py buf 299987`. [VERIFIED: EDGE has 13 templates; `lines()` appends `bulk` rows]. `bufT-dev` must already exist with 3 partitions: broker `KAFKA_NUM_PARTITIONS: 1` and `KAFKA_AUTO_CREATE_TOPICS_ENABLE: "true"`, so a topic auto-created by the producer after a delete would have ONE partition.
- `make` targets: there is no `buf` target; `make deploy` redeploys every connector. Use the scripts directly: `$(PY) scripts/render.py deploy buf-20k buf-100k` with `PY=$HOME/repo/neo/data-engineering/kafka_connectors/venv/bin/python` (both venv python and `templates/` exist [VERIFIED]).

### Current state of the stand (read-only inspection, nothing changed)
| Fact | Value | Evidence |
|---|---|---|
| Containers | `kc_connect_new`, `kc_connect_old` (both reported `unhealthy` by Docker, REST still answers), `kc_schema_registry0`, `kc_kafka0`, `kc_clickhouse`, `kc_mysql` all Up 17-18 h | `docker ps -a` |
| Ports | 8083, 8084, 9092, 9000, 8123, 3306, 8085, 5556, 5557 LISTEN (colima ssh forwards) | `lsof` |
| Connectors | new: `src-mysql-new, sink-dbz-cross, buf-base, sink-dbz-new, sink-kt2-new, sink-kt1-new, buf-20k, buf-100k`; old: `buf-old, src-mysql-old, sink-dbz-old, sink-kt2-old, sink-kt1-old` | `GET /connectors` on 8084 and 8083 |
| `offsets.sh` right now | all four groups, every partition `committed == end` (p0 233590, p1 234268, p2 232481 = **700,339 records**), lag 0 | `scripts/offsets.sh` output (read-only describe) |
| Jar in `kc_connect_new` | sha256 `13d904c41410c626823aaa8a0527093d529f376288b9be51001edfa98bf9ea8b` = the reporter's `build/clickhouse-kafka-connect-v1.7.0_gg5-patched.zip` inner jar. The image's own jar (`images/new/data/...zip`) is `8acc947c44fe0c5d12f7df1da4ac063adece72f0ff948fb0b39467604b2e68fa`. `docker diff` shows the installed jar as changed (`C`) in the container filesystem, so it was hot-swapped (probably `docker cp` plus restart: [ASSUMED] method, the SHA equality is verified) | `sha256sum` in container; `unzip` of both zips; `docker diff` |
| Sink version string | `manifest.json` says `"version" : "v1.7.0_gg5"`; the repo's `VERSION` is still `v1.7.0_gg5` until Phase 38 | read |

Consequences:
1. **The stand now holds the reporter's AFTER state.** For BUF-06 the executor must replace the jar with the Phase 35 build and reset topic, groups and tables so the 300000-record run is clean. The version string cannot distinguish the Phase 35 jar from gg5 or the reporter's jar; **SHA-256 of the zip and of the installed jar is the only discriminator** (roadmap: "recorded by version and SHA-256").
2. **Needs user action/confirmation:** resetting destroys the reporter's AFTER evidence (`bufT-dev` data, `connect-buf-*` groups, `buf.*` tables), and swapping the jar, `docker restart` and `docker kill kc_connect_new` also bounce every other connector on that worker (`src-mysql-new`, `sink-dbz-*`, `sink-kt*-new`). Ask before running; also note that both workers are flagged `unhealthy`; run `render.py status` (GET only) first.

### How the executor runs it, scripts unedited (do NOT run in research)
0. Snapshot `scripts/offsets.sh`, `scripts/buf_report.sh`, `sha256sum` of the installed jar and `render.py status`. Copy to the phase record as the "pre-state".
1. Build the Phase 35 jar in the repo: `./gradlew createConfluentArchive`; the zip is `build/confluent/clickhouse-kafka-connect-v1.7.0_gg5.zip` (the one there now, 21:16 yesterday, predates the fix: rebuild after the fix commit). Record `shasum -a 256` of the zip; unzip into a NEW empty directory; record the inner jar's sha256; prove it contains the fix: `unzip -p <jar> com/clickhouse/kafka/connect/sink/ChunkFlusher.class | strings | grep originalKafkaOffset`.
2. Reset (after user consent): `render.py delete buf-old buf-base buf-20k buf-100k`; delete groups `connect-buf-*` (`kafka-consumer-groups --delete --group ...`; the group must be empty, so delete connectors first); `kafka-topics --delete --topic bufT-dev`; `kafka-topics --create --topic bufT-dev --partitions 3 --replication-factor 1`; truncate `buf.kt_buf_*` (or drop database `buf` and rerun `scripts/ch_init.sh`, which is idempotent) through `docker exec kc_clickhouse clickhouse-client --user root --password root`.
3. **First-start measurement (flag C), needs a "before" state:** install the ORIGINAL gg5 jar (`8acc947c...`, from `images/new/data`) into `kc_connect_new`, restart, `render.py deploy buf-20k buf-100k`, `python3 scripts/produce.py buf 299987`, wait for rows to reach 300000 per table; `scripts/offsets.sh` must show `committed=none`. Then swap in the Phase 35 jar, `docker restart kc_connect_new`, and watch: `offsets.sh` right after start (committed still none), `docker logs kc_connect_new | grep -i "Resetting offset"` (expected: reset to earliest, offset 0, per `auto.offset.reset=earliest`), `buf_report.sh` duplicates (expected: the table doubles to about 600000 rows, 300000 distinct), then `offsets.sh` eventually `committed=end`. Record where it resumed. This is the evidence for the rollout item.
4. **BUF-06 proper:** reset again (step 2), keep the Phase 35 jar, `render.py deploy buf-20k buf-100k`, produce 299987, wait, `scripts/offsets.sh` expecting `committed=end` (the reporter's measurement: none before the fix). Then `docker kill kc_connect_new`, `docker start kc_connect_new`, wait until `committed=end`, `scripts/buf_report.sh` (the duplicates line). Run the kill both mid-consumption and after full consumption and record both; the reporter's numbers were 392k and 400k duplicates before the fix and 41.5k and 0 after; the roadmap wants them set side by side.
5. Copy every script output verbatim into `35-COMMAND-EVIDENCE.md`; the stand's own `results/` directory is empty and git-ignored, so tee into it only as a convenience.

Acceptance bound for "within the at-least-once window" (propose in the plan; [ASSUMED] derivation): after a hard kill the re-read is at most the unflushed buffer (below `bufferCount`, but a flat buffer can overshoot by up to `max.poll.records` = 10000 before it flushes) plus records flushed since the last offset commit (at most one `offset.flush.interval.ms` = 10 s of consumption). `buf-100k` reading 300000 as three 100000 chunks and killed after the last commit should show 0 duplicates, which matches the reporter's 0.

Runtime/resources: [ASSUMED, not measured] a few minutes per run; the stand is already up on colima `vz` (8 CPU, 16 GiB, Running [VERIFIED `colima list`]) and no image rebuild is needed with the jar swap. Network is needed only if the image is rebuilt (`curl` of log4j/Debezium in the Dockerfile after the `COPY` layer), which this recipe avoids.

### First start after the upgrade (item 6, flag C)
- Pre-fix, a buffered connector under a renaming SMT commits nothing for its partitions (`offsets.sh` prints `none`; README: "group stays at no offset"). [CITED: stand README findings log; the unbuffered `buf-old`/`buf-base` show `committed=end` because the Direct path returns Connect's own `currentOffsets`: VERIFIED in the output above]
- So where the first start on the fixed jar resumes depends on the group's history: (a) never committed, or the old commits expired: Kafka applies `auto.offset.reset`, which the templates render as `earliest`, so the connector **rereads the retained topic from the beginning once** and then commits; for non-deduplicating tables that is a full duplicate load. (b) a stale committed offset from an earlier unbuffered run (for example before `bufferCount` was turned on) is still in `__consumer_offsets`: it resumes there and re-reads everything produced since. [ASSUMED: general Kafka behaviour; broker `offsets.retention.minutes` (default 10080, 7 days after the group becomes empty) was not read; verify on the stand with `docker exec kc_kafka0 ...` before wording it, and measure case (a) per step 3.] A mitigation to verify on the stand: stop the connector and set the group's offsets with `kafka-consumer-groups --reset-offsets --to-latest|--to-datetime ... --execute` before starting the fixed jar [ASSUMED: standard Kafka tooling, verify].

## BUF-07: bookkeeping (item 6)

### fork-anchors.txt (read in full)
- Format: tab-separated `path TAB fixed-string TAB expected-count TAB backs-ID`; `#` lines are comments; count = number of LINES containing the string (`grep -cF -e`); a missing path counts 0. Each phase appends a block under a comment such as `# Phase 34 (v1.7.0_gg6): ...`. Current state: 168 lines, `rows=141 bad=0`. [VERIFIED: `34-evidence-check.py anchors .planning/scripts/fork-anchors.txt` run this session]
- The gate (`verify-merge-invariants.sh`) invariant 3: every commit in the range that carries the anchors file must satisfy its own copy; so an anchor row lands in the same commit as, or after, the code it pins. `fork-anchor-exemptions.txt` is only for already-published commits; nothing here needs one. The tail gate and the cumulative gate commands and their expected `result=PASS` lines are in 34-RESEARCH "Gate invocation" and 34-COMMAND-EVIDENCE E-11 (the Phase 34 tail-2 gate, `PRE_TAG`/`CONFLICTED_PATHS_FILE`, does not apply to a non-merge phase; for Phase 35 run the anchors mirror per commit and the cumulative gate once at the phase tip, in the background, as Phase 34 did).
- No row exists today for `ChunkFlusher.java`, `AtLeastOnceBufferStrategy.java` or `ExactlyOnceBufferStrategy.java`. [VERIFIED: grep of fork-anchors.txt]

Proposed rows, counts measured with `grep -cF -e` on the patched clone (re-measure on the real tip before committing):

| Path (under `src/main/java/com/clickhouse/kafka/connect/sink/`) | Pattern | Count | ID |
|---|---|---|---|
| `ChunkFlusher.java` | `new TopicPartition(record.originalTopic(), record.originalKafkaPartition())` | 1 | BUF-01 |
| `ChunkFlusher.java` | `record.originalKafkaOffset() + 1` | 1 | BUF-01 |
| `ChunkFlusher.java` | `record.topic()` | 0 | BUF-01 |
| `AtLeastOnceBufferStrategy.java` | `new TopicPartition(record.originalTopic(), record.originalKafkaPartition())` | 1 | BUF-02 |
| `AtLeastOnceBufferStrategy.java` | `record.topic()` | 0 | BUF-02 |
| `ExactlyOnceBufferStrategy.java` | `new TopicPartition(r.originalTopic(), r.originalKafkaPartition())` | 1 | BUF-02 |
| `ExactlyOnceBufferStrategy.java` | `r.topic()` | 0 | BUF-02 |

Unpatched counts for the three `.topic()` negatives are 1 each [VERIFIED], so those rows go red on the unfixed code. If the user approves the Processing one-liner, add `src/main/java/.../processing/Processing.java | record.getSinkRecord().originalTopic() | 1 | BUF-03` in that fix's commit.

### FORK-DELTA row (`docs/FORK-DELTA.md`)
Section `### Connector source` is a 4-column table `| Behaviour | Where it lives | Why upstream does not provide it | Pinned by |`, rows "ordered by file path and then by position". New row goes before the first `sink/ClickHouseSinkConfig.java` row (line 32 of the file; the strategy and flusher paths sort before `ClickHouseSinkConfig`). Content to state: the three offsets keyed by `originalTopic()/originalKafkaPartition()/originalKafkaOffset()+1`; **fork-only until upstream resolves #777 (PR #786 was closed unmerged on 2026-09-09)**; minimum Connect runtime = one with those accessors (worker 7.9.7 verified; PR #786 text says 3.6+); the Direct path and the non-renamed case unchanged; table routing keeps the transformed topic; pins `BufferedOffsetCommitUnitTest`, `BufferedOffsetCommitLiveTest`, anchor IDs BUF-01/02, BLD-16 list. No test guards FORK-DELTA's table format (only `DependabotProtobufIgnoreGuardUnitTest` mentions the file name in a comment), so format is by convention.
Note for Phase 38 (RLS-01, owns the final refresh): FORK-DELTA "Known costs and open items" item 9 says buffered delivery is "deliberately not adopted by this fork". After this row the fork patches that code, so item 9's wording needs a clause (the fork does not USE buffering but now carries a fix for it). Leave to RLS-01 or put one clause in the same commit; planner's call.

### Rollout item (Phase 38 places it; Phase 35 records it)
Constraints read from the existing note and RLS-02: `docs/ROLLOUT-v1.7.0_gg5.md` "names no host, connector, cluster or production system. Every environment-specific value is an angle-bracket stand-in, such as `<connector-name>`"; every claim says whether it is "measured" or "read from the code" and names the command/test. RLS-02 and ROADMAP Phase 38 criterion 2 check that the buffered-offset item is present. So the Phase 35 deliverable is a record file (suggest `35-ROLLOUT-ITEM.md`) with: the behaviour change in operator terms (offsets are now committed for buffered connectors under renaming SMTs, so after a restart or rebalance the group no longer rereads from `auto.offset.reset`); the evidence label per claim (measured: stand `offsets.sh` before/after with jar SHA-256; read from the code: the three sites); **where the first start after the upgrade resumes** (case (a)/(b) above, as measured); who is affected (only `bufferCount > 0` plus a topic-renaming SMT; unbuffered connectors and non-renamed topics unchanged); the mitigation option; and the minimum worker. Use `<connector-name>`, `<topic>`, `<group>` stand-ins; do not paste stand hostnames or the `bufT`/`buf-20k` names into the note itself (the evidence file may keep them).

## Don't Hand-Roll

| Problem | Don't Build | Use Instead | Why |
|---|---|---|---|
| Kafka API reference list | hand-edit `connect-api-references.txt` | `python3 -I .planning/scripts/connect-api-refs.py ... --write` (recipe below) | the script only writes when worker, 4.1.1 and 4.4.0 all report 0 missing |
| JUnit tallies, red-set checks, anchors count | ad hoc greps | `34-evidence-check.py` subcommands `junit`, `failed --expect`, `skips`, `anchors` (copy as `35-evidence-check.py` or call by path; it is read-only, standard library only) | same semantics as the Phase 33/34 evidence |
| Finding what Spotless would change | `spotlessApply` on the repo | detached worktree + `spotlessApply` + `cmp` on the touched file (34 E-09) | 50 tracked files are already dirty |
| Broker offset observation | custom consumer code | `kafka-consumer-groups --describe` (stand) or the new `ConfluentPlatform` helper | what the stand and operators use |
| Renamed record construction | a fake `SinkRecord` subclass | `SinkRecord.newRecord(...)` (and the 13-arg constructor for partition/offset divergence) | real Connect semantics: originals preserved |
| A new tracking structure for buffers | per-record maps | the existing `TopicPartition` already allocated | hot-path rule |

## Common Pitfalls

### Pitfall 1: BLD-16 goes red on the fix itself
**What goes wrong:** `ConnectApiReferencesUnitTest` fails once the three accessors are referenced: `M org/apache/kafka/connect/sink/SinkRecord.originalKafkaOffset:()J`, `...originalKafkaPartition:()Ljava/lang/Integer;`, `...originalTopic:()Ljava/lang/String;`. [VERIFIED: red run in the clone]
**How to avoid:** same commit as the main edit. Re-bless recipe, measured: `derived classes 39 members 141 total 180` (was 138/177), `REFERENCE worker/4.1.1/4.4.0 classes-missing 0 members-missing 0`, expected diff is exactly those 3 `M` lines plus a new `# date:` line:
```bash
# worker jars: docker create confluentinc/cp-kafka-connect-base:7.9.7; docker cp <c>:/usr/share/java/kafka/connect-api-7.9.7-ccs.jar <NEW EMPTY DIR>/ ; same for kafka-clients-7.9.7-ccs.jar; docker rm <c>
# (sha256 must equal b57546e9...1de4 and 28423086...f962; both matched this session)
env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain compileJava
G=~/.gradle/caches/modules-2/files-2.1/org.apache.kafka
python3 -I .planning/scripts/connect-api-refs.py --classes build/classes/java/main \
  --compile-classpath $G/connect-api/4.4.0/*/connect-api-4.4.0.jar:$G/kafka-clients/4.4.0/*/kafka-clients-4.4.0.jar \
  --reference worker=<dir>/connect-api-7.9.7-ccs.jar:<dir>/kafka-clients-7.9.7-ccs.jar \
  --reference 4.1.1=$G/connect-api/4.1.1/*/connect-api-4.1.1.jar:$G/kafka-clients/4.1.1/*/kafka-clients-4.1.1.jar \
  --reference 4.4.0=$G/connect-api/4.4.0/*/connect-api-4.4.0.jar:$G/kafka-clients/4.4.0/*/kafka-clients-4.4.0.jar \
  --image-note "image: confluentinc/cp-kafka-connect-base@sha256:93b9afd3e7f8003381401464e4e4fa3dbe5e3766bcbc99de12afd41dc2904055 (tag 7.9.7; linux/amd64 and linux/arm64 jars byte-identical; Temurin 17.0.19)" \
  --write src/test/resources/connect-api-references.txt
git diff -U0 -- src/test/resources/connect-api-references.txt     # expect +3 M lines and the date line only
```
Globs: expand each `*` to one real path first (zsh errors on a no-match glob; the `*` hash directory is unique per version). Without `--image-note` the `# image:` header line is dropped, and the note text must itself start with `image: `. The Phase 36 bump re-blesses again; blessed lines are sorted.

### Pitfall 2: Spotless would reformat the very files being edited
**What goes wrong:** `spotlessApply` at `ea0126fe` changes **50 tracked files**, including `ChunkFlusher.java`, `AtLeastOnceBufferStrategy.java`, `ExactlyOnceBufferStrategy.java`, `DirectDeliveryStrategy.java`, `DeliveryStrategy.java` and `ClickHouseSinkTaskBufferTest.java` (import order: the build only enables `importOrder()` and `removeUnusedImports()`). [VERIFIED: `spotlessApply` in the clone + `git diff --name-only`; `spotlessJavaCheck` reports 2 listed + "49 other files" with my untracked probe present]. The memory note says 46; the measured number now is 50 tracked files.
**How to avoid:** do not run `spotlessApply` on those three main files; the fix is 4 changed lines and must stay a 4-line diff (upstream-sync surface). "Spotless-neutral" = the set of Spotless hunks for each file is unchanged by the edit. NEW files must be Spotless-clean: apply in a detached worktree and `cmp` the file (34 E-09 recipe), never repo-wide.

### Pitfall 3: Silent changes to table routing
**What goes wrong:** "fixing" `Record.convert`/`RecordConvertor` or `ProxySinkTask.batchKey` to use the original topic would send data to a table named after the source topic.
**How to avoid:** the fix is confined to offset bookkeeping (sites 1-3). The anchors for the three `.topic()` zeros are per file, so `RecordConvertor.java` (which legitimately keeps `sinkRecord.topic()`) is not anchored.

### Pitfall 4: A pin that observes `Map.get(...)` NPE instead of a message
The unchanged code fails with `NullPointerException` inside the test, not an assertion message. Assert key presence first (`assertTrue(result.containsKey(tp), message)`) so the red run names the cause.

### Pitfall 5: Adding a skipped test or a `@Disabled` trips the skip inventory
`34-evidence-check.py skips` and FORK-DELTA "Named skip inventory" require exactly the 18 named skips. A test that cannot run must fail or not exist, not skip.

### Pitfall 6: The stand's data and names are not a clean room
It holds 700,339 records, four committed groups, a patched jar and nine other live connectors on the same worker (see "Current state"). Do not run `make down` (destroys volumes and the whole A/B harness) and do not run `make deploy` (re-PUTs everything). Use the targeted commands above, with the user's consent.

### Pitfall 7: Operational traps carried from memory notes and this session
- zsh: `echo =====` expands as a command (`=cmd`), `$T:path` is a history modifier, unmatched globs (`--include=*.java`, `*/x`) abort the command: quote globs. [observed this session]
- OpenJ9 (`openj9-mockito-test-jvm-crash`): a full `test` exit 255 can be an intermittent GPF; delete `core.*.dmp` one by one (never `git clean`), rerun once, escape hatch `-PjavaVersion=21`. No crash occurred in this session's targeted runs.
- `colima-vz-profile`: Docker is profile `vz`; `DOCKER_HOST=unix:///Users/alexandr.chashnikov/.colima/vz/docker.sock` is set in this shell; docker CLI is `/opt/homebrew/Cellar/docker/29.8.2/bin/docker` (on PATH in an interactive shell).
- `latest` drift: ClickHouse `latest` is `26.10.3.7`, digest `sha256:739e099abd1157390960a59e4532d21781d8fde57e6e4151cb920ea02c30b1ce`, image created 2026-10-26 (the same digest as Phase 34's gate) [VERIFIED: `docker image inspect`]. A red full suite on unchanged code means check the digest first.
- Do not push anything; do not touch the stand's scripts or `connectors.yaml`.

## Hot path, threading, style (item 7)

- **Hot path:** `ChunkFlusher.flush` runs one `new TopicPartition` per record (existing) and one `new OffsetAndMetadata` only when the offset advances (existing). The patch keeps both counts; `originalKafkaPartition()` returns the already-boxed `Integer` held by `SinkRecord`, `originalKafkaOffset()` is a primitive. EO `put` allocates one `TopicPartition` per record (existing); AtLeastOnce `close` allocates one per buffered record per revocation (existing, not per-poll). Keep the four lines as they are.
- **Threading:** Connect drives `put`, `preCommit` and `close` on the single task thread (`WorkerSinkTask`'s rebalance listener calls `commitOffsets(closing=true)` and `task.close` on the consumer thread, per the 3.9 source summary), which is why `flushedOffsets` is a plain `HashMap`. No new shared state is added.
- **Logging:** no new log lines. The existing `LOGGER.info("Rebalance: removed {} buffered records ...", dropped, partitions)` prints partitions (topic names, no data). Never log record contents or secrets.
- **Style:** AGENTS.md says Google Java Style and "run spotlessApply", but the repo's real config is imports only and 50 tracked files are already dirty: follow Pitfall 2.
- **Code comments:** one short comment at `ChunkFlusher.flush` is justified (the invariant: offsets are keyed by the consumer's original topic/partition because Connect validates them against the assignment; the table topic is the transformed one). Do not narrate the code.

## Code Examples

### The fix (reporter's patch content, applies clean)
```java
// ChunkFlusher.flush
TopicPartition tp = new TopicPartition(record.originalTopic(), record.originalKafkaPartition());
long offset = record.originalKafkaOffset() + 1; // committed offset = next offset to consume
// AtLeastOnceBufferStrategy.close
buffer.removeIf(record -> partitions.contains(new TopicPartition(record.originalTopic(), record.originalKafkaPartition())));
// ExactlyOnceBufferStrategy.put
TopicPartition tp = new TopicPartition(r.originalTopic(), r.originalKafkaPartition());
```
Source: `~/dev/clickhouse_mvp/local_kc_test/patches/buffer-commit-original-topic.patch`; `git apply --check -p1` clean at `ea0126fe`.

### Anchors mirror (working tree, same semantics as gate invariant 3)
```bash
python3 -I .planning/phases/34-upstream-tail-merge/34-evidence-check.py anchors .planning/scripts/fork-anchors.txt   # prints rows=N bad=M
```

## State of the Art

| Old approach | Current approach | Impact |
|---|---|---|
| Key buffered offsets by `record.topic()` (post-SMT) | Key by `originalTopic()/originalKafkaPartition()/originalKafkaOffset()` | Connect accepts the offsets; restart/rebalance resumes instead of rereading from `auto.offset.reset` |
| Upstream's PR #786 used reflection to reach the accessors on a connect-api 2.7.0 compile target | The fork compiles against 4.4.0, worker floor proven by BLD-16 on 7.9.7 | No reflection or `SinkRecordMetadata` shim needed on the fork |

**Deprecated/outdated:** none in scope. The maintainer's "accessors introduced in 4.0.0" comment on PR #786 does not match the 7.9.7 jar, which has them [VERIFIED javap]; prefer the measurement.

## Assumptions Log

| # | Claim | Section | Risk if Wrong |
|---|---|---|---|
| A1 | The reporter hot-swapped the patched jar with `docker cp` plus restart (SHA equality and `docker diff` `C` are verified; the method is inferred) | Stand state | Low: only affects how the executor is told to swap |
| A2 | Kafka REST proxy 7.9.6 serves the v3 consumer-group lag endpoint | Where the live pin lives | Low: fall back to `execInContainer("kafka-consumer-groups", ...)` |
| A3 | First start with no committed offsets rereads from `earliest` (template value); stale commits resume at the old offset; commits expire after the broker's `offsets.retention.minutes` | First start | Medium: the rollout item states resume behaviour; the stand measurement (step 3) must confirm before wording |
| A4 | `kafka-consumer-groups --reset-offsets ... --execute` on a stopped connector's group is a valid mitigation | First start | Low-medium: verify on the stand before recommending it to operators |
| A5 | "Within the at-least-once window" = (`bufferCount` + up to `max.poll.records` overshoot) + records flushed in the last `offset.flush.interval.ms` (10 s) | Stand | Medium: the criterion is vague; planner should fix the bound in the plan |
| A6 | Two source topics collapsing to one table does not arise from the rendered templates alone (one jurisdiction per connector) | BUF-03 | Medium: if the operator has such a connector, P2-P4 data loss is live today and is a separate decision |
| A7 | Stand run takes a few minutes per run; no network is needed with the jar swap | Stand | Low |
| A8 | The 3.9-branch `WorkerSinkTask` behaviours (original keys, assignment filter, close-after-commit) as read through a summarising fetch | Architecture | Low: also corroborated by the stand (`committed=end` after the patch) |

## Open Questions

1. **Does the user want the `Processing.java` L137 one-liner (Direct + `reportInsertedOffsets=true`)?**
   - Known: defect measured (P6 `{}`), fix measured (one line, `ProcessingTest` green), state key and KeeperMap untouched, upstream #786 also touched Processing.
   - Unclear: appetite for editing a high-risk file in this phase and for changing the Direct path BUF-05 wants unchanged.
   - Recommendation: a plan checkpoint (`checkpoint:decision`) before any `Processing.java` edit; default = separate commit with its own red pin if approved, else a recorded open item.
2. **Many-to-one state-key hazard (P2-P4):** record as measured evidence; any state-key change STOPS for the user (ROADMAP). Recommendation: surface it in the plan summary and the milestone close record; no code now.
3. **Include the optional `integrationTest` class?** Cost about one task plus one helper on `ConfluentPlatform`; only repo-resident broker-level observer. Recommendation: include if budget allows; never a gate dependency.
4. **Stand reset consent and blast radius** (shared worker, 700k-record AFTER evidence): user must confirm before the executor resets and swaps.
5. **Acceptance bound for duplicates after `docker kill`** (A5): fix in the plan so the verdict is mechanical.
6. **FORK-DELTA item 9 wording** ("buffered delivery deliberately not adopted") after the BUF row: clause now or at RLS-01.
7. **Re-check #777 / PR #786 at phase close** so FORK-DELTA's "fork-only until upstream resolves #777" stays true.

## Environment Availability

| Dependency | Required By | Available | Version | Fallback |
|------------|------------|-----------|---------|----------|
| Docker (colima profile `vz`) | live pin, stand, integration class, worker jar extraction | yes | `colima list`: `vz` Running aarch64, 8 CPU, 16 GiB; `DOCKER_HOST` set in this shell | Docker-free work (unit pins, fix, BLD-16 with jars from the Gradle cache for 4.1.1/4.4.0) proceeds without it; the worker jars need the local image |
| `clickhouse/clickhouse-server:latest` | live pin, full suite | yes | 26.10.3.7, `sha256:739e099abd11...` | none |
| `confluentinc/cp-kafka-connect-base:7.9.7` | BLD-16 worker jars | yes | image id `93b9afd3e7f8`; jars extracted, hashes match | none needed |
| `cp-server-connect:7.9.6` and the rest of the `ConfluentPlatform` image set | optional integration class | yes | all local | skip the optional class |
| Stand containers and images (`kc_test/connect:new` f290f39d9e2d, `:old` 5675da80c81b, `cp-kafka:7.9.10`, `cp-schema-registry:7.9.10`, `clickhouse-server:26.3`, `mysql:5.7.39`) | BUF-06, flag C | yes, stack is UP now | see "Current state" | none; do not rebuild (network) |
| stand venv + templates | `render.py` | yes | `~/repo/neo/data-engineering/kafka_connectors/venv/bin/python`, `templates/*.jinja` | none |
| `gh`, network to github.com | #777/#786 check | yes | worked this session | `git ls-remote` only |
| `javap`, `python3` | BLD-16 | yes | JDK / system | none |
| Gradle wrapper 9.8.2, JDK 17 toolchain | build, tests | yes | used this session | `-PjavaVersion=21` |

**Missing dependencies with no fallback:** none. **User action required:** consent to reset/modify the stand (Open Question 4).

## Validation Architecture

> `workflow.nyquist_validation` was not read as `false` (key absent in the context given); section included.

### Test Framework
| Property | Value |
|----------|-------|
| Framework | JUnit 5 (Jupiter) + Mockito; Testcontainers for `@Tag("clickhouse")` (`ClickHouseBase`) |
| Config file | `build.gradle.kts` (`testUnit` excludes tag `clickhouse`; `test`, `integrationTest` suites) |
| Quick run command | `env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain testUnit --tests '*BufferedOffsetCommitUnitTest'` |
| Live command | `CLICKHOUSE_VERSION=latest CLIENT_VERSION=V2 env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain test --rerun --tests '*BufferedOffsetCommitLiveTest'` |
| Full suite command | `CLICKHOUSE_VERSION=latest CLIENT_VERSION=V2 env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain test --rerun` (phase gate: once; baseline 2028 tests, 0 failures, 0 errors, 18 skipped on `latest` x V2) |

Cache busting: `rm -rf build/test-results/testUnit` plus `--rerun` (build.gradle.kts is an input of every `Test` task). Baselines to carry: `testUnit` 1441 tests / 0 / 0 / 2 skipped; full 2028 / 0 / 0 / 18; anchors `rows=141 bad=0`. [VERIFIED: 34-COMMAND-EVIDENCE E-11 and this session's `anchors` run]. Recompute totals after adding tests; do not reuse them.

### Phase Requirements to Test Map
| Req ID | Behavior | Test Type | Automated Command | File Exists? |
|--------|----------|-----------|-------------------|-------------|
| BUF-01 | `preCommit` keyed original, `+1` | unit (Docker-free) | `testUnit --tests '*BufferedOffsetCommitUnitTest'` | Wave 0 |
| BUF-02 | AtLeastOnce `close`, EO grouping, `forgetPartitions` | unit | same | Wave 0 |
| BUF-03 | state key unchanged under rename; equal-offset interleave writes all | unit | `testUnit --tests '*ProcessingRenamedTopicStateKeyUnitTest'` | Wave 0 |
| BUF-04 | newRecord-built records; non-renamed control; red first | unit + evidence | red run via `34-evidence-check.py failed --expect` | Wave 0 |
| BUF-05 | `ClickHouseSinkTask` + ClickHouse: keys, values, subset-of-assigned, rows read back; Direct control; existing buffer tests unedited | live (Docker) | `test --tests '*BufferedOffsetCommitLiveTest' --tests '*ClickHouseSinkTaskBufferTest'` | Wave 0 (new class); existing exists |
| BUF-05 (optional) | real committed offsets at the broker | integrationTest | `integrationTest --tests '*BufferedRegexRouterOffsetCommitIntegrationTest'` | optional |
| BUF-06 | stand: committed=end, duplicates after kill | manual/live record | stand scripts, outputs copied to evidence | evidence only |
| BUF-07 | anchors present and pass; FORK-DELTA row; rollout item exists | gate + doc | anchors mirror; `grep` FORK-DELTA; file exists | Wave 0 |
| BLD-16 | blessed list has the 3 accessors | unit | `testUnit --tests '*ConnectApiReferencesUnitTest'` | exists (data file changes) |

### Sampling Rate
- **Per task commit:** the narrowest `testUnit --tests`, the anchors mirror after any commit touching `fork-anchors.txt`.
- **Per wave:** cache-busted `testUnit`.
- **Phase gate:** cache-busted `testUnit`, one full `test --rerun` on `latest` x V2 with `SELECT version()` recorded, the anchors invariant (mirror per commit; cumulative gate once in the background), `34-evidence-check.py skips` still exactly 18.

### Wave 0 Gaps
- [ ] `BufferedOffsetCommitUnitTest.java` (BUF-01, BUF-02, BUF-04, Direct control)
- [ ] `BufferedOffsetCommitLiveTest.java` (BUF-05)
- [ ] `ProcessingRenamedTopicStateKeyUnitTest.java` (BUF-03 tripwire)
- [ ] optional `RenamedTopicRecords` fixture and optional integration class + `ConfluentPlatform` helper
- [ ] `35-evidence-check.py` (copy of the Phase 34 helper, or call it by path) and `35-COMMAND-EVIDENCE.md`
- [ ] Framework install: none

## Security Domain

`security_enforcement` is not disabled in the context given; included.

### Applicable ASVS Categories
| ASVS Category | Applies | Standard Control |
|---------------|---------|-----------------|
| V2 Authentication | no | no change |
| V3 Session Management | no | no change |
| V4 Access Control | no | no change |
| V5 Input Validation | no new input | the change reads three accessor values from `SinkRecord`; no parsing |
| V6 Cryptography | no | none |
| V7 Error handling and logging | yes | no new log lines; partitions are logged, never record data or credentials; evidence files carry no secrets |
| V14 Configuration | yes | rollout note and evidence use angle-bracket stand-ins; stand credentials (`root/root`, `sink/sink`) are local test values and must not be copied into repo documents |

### Known Threat Patterns
| Pattern | STRIDE | Standard Mitigation |
|---|---|---|
| Wrong offsets committed (too far) cause data loss on restart | Tampering/Integrity | commit only `originalKafkaOffset() + 1` of records actually flushed; pins assert exact values; control test for the non-renamed case; `preCommit` still drains only flushed data |
| Offsets for unassigned partitions (the original defect) | Integrity | keys now equal Connect's assignment; live pin asserts the returned keys are a subset of the assigned partitions |
| Revoked partition data flushed after revocation, duplicating on the new owner | Integrity | `close()` now matches by original partition; pins |
| Secrets in evidence copied from the stand | Information disclosure | copy script output only; the rollout item names no host or connector; `34-evidence-check.py secrets FILE` on the evidence file |
| Reset of the stand destroys the user's evidence | Availability of evidence | user consent gate before any reset; snapshot the pre-state first |

## Project Constraints (from CLAUDE.md)

Extracted from `./CLAUDE.md` -> `AGENTS.md` and `.claude/CLAUDE.md`; treated as locked:
- Do NOT commit to the branch without the user asking explicitly and do not change a PR title/description. (This phase's plans commit on `sync/v1.7.0` as part of the GSD workflow the user invoked; the researcher committed nothing.) Start repo edits through a GSD command.
- High-risk files (`Processing.java`, `KeeperStateProvider.java`, `ClickHouseWriter.java`, `ClickHouseSinkConfig.java`, `RangeContainer.java`): extra care; ROADMAP adds that a state-key change stops for the user. The BUF fix touches none of them; the optional Processing one-liner needs the user's decision.
- `ChunkFlusher`, the buffer strategies, `ProxySinkTask`, `ClickHouseSinkTask`, `ClickHouseWriter` and `Processing` are hot paths: no new per-record allocation, copies, autoboxing.
- Only add tests; never delete or weaken existing ones. Read `ClickHouseTestHelpers` before writing tests. When writing to ClickHouse, read back and `assertEquals` (the live pin does). Cover state-machine transitions in `ProcessingTest`; do not mock `ClickHouseWriter` in state-machine tests (use `InMemoryDBWriter`/`InMemoryState`).
- Behaviour preservation and rebasability: small targeted fixes, additive tests; the fork runs in production. Tests target ClickHouse `latest` + client V2.
- `VERSION` and `CHANGELOG.md` are release work (Phase 38), not this phase; do not upgrade dependencies without asking (none are touched).
- Do not log secrets or record data; useful comments only; SLF4J levels (TRACE per record, DEBUG per batch, INFO lifecycle).
- Docker is colima profile `vz`; GSD verbs use the space form; haiku checkers are grep-only (memory notes).

## Sources

### Primary (HIGH confidence, executed or read this session)
- Repo files read with line numbers: `ChunkFlusher.java`, `AtLeastOnceBufferStrategy.java`, `ExactlyOnceBufferStrategy.java`, `DirectDeliveryStrategy.java`, `DeliveryStrategy.java`, `ClickHouseSinkTask.java`, `ProxySinkTask.java`, `processing/Processing.java`, `state/BaseStateProviderImpl.java`, `state/StateProvider.java`, `state/StateRecord.java`, `state/provider/InMemoryState.java`, `state/provider/KeeperStateProvider.java`, `data/Record.java`, `data/convert/RecordConvertor.java`, `kafka/RangeContainer.java`, `ClickHouseSinkConfig.java` (defaults), `docs/FORK-DELTA.md` (structure and rows), `.planning/scripts/fork-anchors.txt`, `verify-merge-invariants.sh` (header and invariant 3), `connect-api-refs.py` (header), `src/test/resources/connect-api-references.txt`, `.github/workflows/nightly-integration-tests.yaml`, `ConfluentPlatform.java`, `ClickHouseSinkConnectorIntegrationTest.java`, `ClickHouseSinkTaskBufferTest.java`, `ProcessingTest.java`, `WriterFailureClassificationUnitTest.java`, `ProxySinkTaskConstructorUnitTest.java`
- Planning docs: `35-CONTEXT.md`, ROADMAP Phase 35 and Phase 38, REQUIREMENTS BUF-01..07, RLS-01/02, `34-RESEARCH.md`, `34-COMMAND-EVIDENCE.md` (E-07, E-11), `34-REVIEW-FIX.md` (WR-01), `31-COMMAND-EVIDENCE.md` E-18, `docs/ROLLOUT-v1.7.0_gg5.md` (head)
- Commands run: `git apply --check`, `git ls-remote upstream main`, `gh issue view 777`, `gh pr view 786 --json`, `javap` on four jars, `connect-api-refs.py` into `/tmp/gsd35/refs-new.txt`, `spotlessJavaCheck` and `spotlessApply` (clone), targeted `testUnit` and `test` runs in the clone, `docker ps/inspect/diff/exec sha256sum`, `scripts/offsets.sh` (read-only describe), `lsof`, `colima list`, `docker images`
- Throwaway artifacts (outside the repo, may disappear): `/tmp/gsd35probe/repo` (clone of `ea0126fe` with the patch applied and the probe classes `Buf35ProbeUnitTest`, `Buf35LiveProbeTest`), `/tmp/gsd35/img` (worker jars), `/tmp/gsd35/refs-new.txt`, `/tmp/gsd35/zipx`
- The research-plan seam / Context7 was not needed: there is no library-documentation question in this phase.

### Secondary (MEDIUM confidence)
- `raw.githubusercontent.com/apache/kafka/3.9/.../WorkerSinkTask.java` through a summarising fetch (behaviour, not line numbers)
- Memory notes under `~/.claude/projects/.../memory/` used as hints and cross-checked

### Tertiary (LOW confidence)
- Assumptions A1-A7 above

## Metadata

**Confidence breakdown:**
- Standard stack: HIGH, nothing changes.
- Architecture and the fix: HIGH, patch applied, pins red then green, live probe red then green, BLD-16 diff measured.
- BUF-03 findings: HIGH for what was measured (P1-P7), MEDIUM for the operator-topology reading (A6).
- Stand recipe: MEDIUM-HIGH: read in full and current state inspected, but not run; the first-start behaviour and the duplicate bound are assumptions until the executor measures them.
- Pitfalls: HIGH, each reproduced or read.

**Research date:** 2026-10-29
**Valid until:** until `git ls-remote upstream main` changes or upstream resolves #777; otherwise 30 days. The stand state is volatile (it changes whenever the user touches it): re-run the read-only `offsets.sh`/`sha256sum` snapshot immediately before any reset.
