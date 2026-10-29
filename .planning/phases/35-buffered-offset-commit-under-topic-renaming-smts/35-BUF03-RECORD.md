# Phase 35 BUF-03: the Processing.doLogic check and what it leaves open

Evidence labels: `measured` means a run recorded in `35-COMMAND-EVIDENCE.md` (E-05 to E-07), `read from the code` means the source line named. Nothing in this record rests on an assumption about a deployment.

## Outcome

1. **The exactly-once state key is not part of the offset-commit defect and is unchanged by Phase 35.** The state key is `<transformed topic>-<partition>`: `Processing.doLogic` reads the topic from `record.getRecordOffsetContainer().getTopic()` and `InMemoryState.genKey` formats `%s-%d` from it (read from the code: `Processing.java` `doLogic`, `InMemoryState.java`). Phase 35 changes no state key, no KeeperMap row schema and no step of the `BEFORE`/`AFTER` flag sequence: `git diff` of `Processing.java`, `KeeperStateProvider.java` and the state package against the phase-start tree is empty (measured: `processing_untouched_in_main_tree: yes` in E-06 and E-07). The key is pinned Docker-free by `ProcessingRenamedTopicStateKeyUnitTest` S1 and S2, which pass on the tip and both go red when the key is mutated to the original topic in a worktree (measured: E-07).
2. **`Processing.doLogic`'s `originalTopic` local reads the transformed topic, and under a renaming SMT the Direct path with `reportInsertedOffsets=true` returns no offsets.** The local is assigned `record.getSinkRecord().topic()` and is passed as the last argument of `StateRecord`, which is where `lastInsertedOffsets` takes its key (read from the code: `Processing.java` `doLogic`, `BaseStateProviderImpl.onStateUpdate`). Measured on the tip: `lastInsertedOffsets` holds `bufT-0`, the transformed name (E-06 P5), and `DirectDeliveryStrategy.preCommit` with `reportInsertedOffsets=true` returns `none` for Connect's keys `bufT-dev-0`, matching only a caller whose keys are the transformed ones (E-06 P6). The default configuration is unaffected: P7 returns Connect's own map, the same instance. A one-line change to read `getSinkRecord().originalTopic()` makes P6 return `bufT-dev-0=10` and leaves all 14 `ProcessingTest` cases green (measured: E-06 P6b, in a removed worktree). It is not applied: the user decided that `Processing.java` is not edited in this phase. It is open item OI-35-1.
3. **Two source topics renamed to one target share one state record per partition.** Equal offset ranges lose nothing: 400 of 400 records were written under the one key `t-0` (measured: E-06 P1). Skewed ranges lose or reject the lower-offset source's records: with a state at offsets 5000 to 5099 for one source, the other source's 100 records at offsets 100 to 199 were all dropped (`written_b=0`, P2), the batch at offsets 10 to 109 raised a `RuntimeException` starting `AFTER_PROCESSING: State MISMATCH as batch already processed` (P3), and with `tolerateStateMismatch=true` all 100 were skipped silently (`written_b=0`, P4) (measured: E-06 P2 to P4). This predates the phase and is independent of buffering: the probes call `Processing` directly, the Direct path reaches the same state machine, and the key comes from the upstream design that keys state by the table topic (read from the code: `InMemoryState.genKey`, `KeeperStateProvider` key `%s-%d`; `Processing.doLogic` bypasses the state machine only when `exactlyOnce=false` and `ignorePartitionsWhenBatching=true`). The Phase 35 fix does not make this worse in any measured way: the exactly-once buffer now buckets many-to-one sources separately, while `Processing` still keys by the shared `t-0`. Exactly-once requires an injective rename. It is open item OI-35-2.

## Evidence

| Finding | Label | Where |
|---------|-------|-------|
| State key is the transformed topic and partition | read from the code | `Processing.java` `doLogic` (`topic` local), `InMemoryState.genKey` |
| State key, row schema and flag sequence untouched | measured | E-06 and E-07 `processing_untouched_in_main_tree: yes` |
| State key is pinned and the pin bites | measured | E-07 `tripwire_class_xml`, `mutation_state_key_failed` (S1 and S2) |
| `originalTopic` local reads the transformed topic | read from the code | `Processing.java` `doLogic`, `StateRecord` constructor arguments |
| `lastInsertedOffsets` keyed by the transformed topic | measured | E-06 `probe_p5` |
| Direct path with `reportInsertedOffsets=true` commits nothing under a renaming SMT | measured | E-06 `probe_p6` |
| Default Direct path returns Connect's own offsets | measured | E-06 `probe_p7` |
| One-line change restores the keys, `ProcessingTest` stays green | measured | E-06 `probe_p6b`, `processing_test_after_one_line_xml` |
| Equal offset ranges of two sources lose nothing | measured | E-06 `probe_p1` |
| Skewed ranges lose or reject the lower-offset source | measured | E-06 `probe_p2`, `probe_p3`, `probe_p4` |
| Exactly-once buffer groups by original partition while `Processing` keys by the transformed one | read from the code | `ExactlyOnceBufferStrategy.put`, `Processing.doLogic` |

## Open items for the milestone close

**OI-35-1: the Direct path with `reportInsertedOffsets=true` under a renaming SMT commits nothing.**
- What the user decides: whether to apply the known one-line change to `Processing.java` (`originalTopic` read from `getSinkRecord().originalTopic()`) or to keep the behaviour and the documented limit.
- Evidence it rests on: E-06 P5, P6, P7 and P6b.
- Owner: milestone close, user decision.
- When to re-open (the re-open trigger): a connector sets `reportInsertedOffsets=true` together with a renaming SMT.
- Cost: `Processing.java` is a high-risk file, and the change alters the Direct path for that configuration only; the state key, the KeeperMap schema and the `BEFORE`/`AFTER` sequence are not touched by the one line.

**OI-35-2: the many-to-one state-key hazard.**
- What the user decides: whether to document that exactly-once and the in-memory state machine require an injective rename and leave the key as it is, or to change the key or its inputs, which stops for a user decision before it lands (see the stop rule below).
- Evidence it rests on: E-06 P1 to P4.
- Owner: milestone close, user decision.
- When to re-open (the re-open trigger): a connector whose rename maps two source topics to one table with exactly-once, or with the in-memory state machine; any fix changes the state key or its inputs and therefore stops for a user decision.

**OI-35-3: exactly-once buffering under a partition-changing SMT.**
- What the user decides: whether the exactly-once claim stays limited to an injective rename or a measurement is commissioned.
- Evidence it rests on: reading the code only. The buffer is grouped by original partition (`ExactlyOnceBufferStrategy.put`) while `Processing` keys state by the transformed topic and partition, so several original partitions can feed one state key. This was not probed in this phase, so exactly-once safety is claimed only for an injective rename.
- Owner: milestone close, user decision.
- When to re-open (the re-open trigger): an exactly-once connector with a partition-changing SMT.

## State key and KeeperMap rows

Phase 35 changes no key, so no KeeperMap row is orphaned and the stored rows stay readable. If a key is ever changed (for example by adding the original topic), the rows stored under `<transformed topic>-<partition>` are orphaned: the first batch after the upgrade reads `NONE` and inserts as new, the old rows stay in the table (the state provider has no TTL), and the change is high risk per AGENTS.md. The stop rule: a change of the state key or of its inputs stops for a user decision before it lands (ROADMAP Phase 35 Note); `ProcessingRenamedTopicStateKeyUnitTest` turns red the moment the key moves, and no step of this phase edits it.

## BEFORE/AFTER sequence

No change was made, so the sequence is unchanged in the three cases of ROADMAP criterion 2: a crash before the insert (state `BEFORE`, retry sends the identical batch), a crash after the insert and before the `AFTER` flag, and a crash during the flag write. The existing `ProcessingTest` state-machine pins remain the guard, and all 14 passed on the tip (measured: E-07 `processing_test_xml`).

## What this record does not claim

It measures no broker and no Connect worker: the live pin of plan 35-02 observes what `preCommit` returns, not a group's committed offsets (E-05). The first start after the upgrade is covered by the rollout item of plan 35-03, not here. It says nothing about any deployment.
