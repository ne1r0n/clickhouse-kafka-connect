# Phase 20: Command evidence (D-03, D-04)

- **Base commit:** `P20_BASE` = `d73848807c58c4ba535da05e7515ecbbf5025bdf` (the tip when Phase 20 planning started; every "Phase 20 changed nothing here" check diffs non-`.planning` paths against it).
- **Shell:** macOS zsh, BSD `grep`. Gradle is always run as `env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew ...` because this shell sets that variable to the literal `null`. Log lines carry the local time zone (UTC+3); the date below is UTC.
- **Date (UTC):** 2026-10-14.
- **Rule:** a `Matches text: no` verdict means the doc text gets fixed, never the evidence. Entries that back no doc text carry `Matches text: n/a (gate check)`. Entries that back text not yet written carry the verdict `pending (20-03)` until plan 20-03 writes that text.

### E-01: server version and deduplication windows

Backs: `docs/ROLLOUT-v1.7.0_gg2.md`, the dedup item, the window sentence (written in plan 20-03). The class reads `SELECT version()` and the three window settings from `system.merge_tree_settings` and asserts only that `replicated_deduplication_window` is above zero.

```sh
env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew test --tests '*DedupTokenRetryLiveTest' --rerun
python3 -I -c 'import sys,xml.etree.ElementTree as E;r=E.parse(sys.argv[1]).getroot();o="".join(x.text or "" for x in r.iter("system-out"));print("\n".join(l for l in o.split("\n") if "DEDUP_TOKEN_RETRY server version=" in l or "DEDUP_TOKEN_RETRY settings" in l))' build/test-results/test/TEST-com.clickhouse.kafka.connect.sink.DedupTokenRetryLiveTest.xml
```

```text
[2026-10-14 15:16:25,731] INFO com.clickhouse.kafka.connect.sink.DedupTokenRetryLiveTest DEDUP_TOKEN_RETRY server version=26.9.18.11
[2026-10-14 15:16:25,739] INFO com.clickhouse.kafka.connect.sink.DedupTokenRetryLiveTest DEDUP_TOKEN_RETRY settings replicated_deduplication_window=10000 replicated_deduplication_window_seconds=3600 non_replicated_deduplication_window=0
```

The research phase measured `26.9.17.8` with the same three values; `latest` moved to `26.9.18.11` between the research and this run, and the windows did not change.

Matches text: pending (20-03)

### E-02: re-delivered range on ReplicatedMergeTree, exactlyOnce=false (tracer)

Backs: `docs/ROLLOUT-v1.7.0_gg2.md`, the dedup item, the `exactlyOnce=false` wording (written in plan 20-03). Three fresh tasks (an empty in-memory state each) put the same offsets 0 to 2 of one partition into a `ReplicatedMergeTree` table through the real connector: step 1 the original rows, step 2 the same offsets with every `name` changed, step 3 the original rows again. After every step the table is read back by typed SQL (`SELECT id, name, count() ... GROUP BY id, name`) and compared with the rows step 1 left.

```sh
python3 -I -c 'import sys,xml.etree.ElementTree as E;r=E.parse(sys.argv[1]).getroot();o="".join(x.text or "" for x in r.iter("system-out"));print("\n".join(l for l in o.split("\n") if "DEDUP_TOKEN_RETRY outcome case=eo-false-replicated" in l))' build/test-results/test/TEST-com.clickhouse.kafka.connect.sink.DedupTokenRetryLiveTest.xml
```

```text
[2026-10-14 15:16:26,248] INFO com.clickhouse.kafka.connect.sink.DedupTokenRetryLiveTest DEDUP_TOKEN_RETRY outcome case=eo-false-replicated step=1 rows=3
[2026-10-14 15:16:26,358] INFO com.clickhouse.kafka.connect.sink.DedupTokenRetryLiveTest DEDUP_TOKEN_RETRY outcome case=eo-false-replicated step=2 rows=3
[2026-10-14 15:16:26,455] INFO com.clickhouse.kafka.connect.sink.DedupTokenRetryLiveTest DEDUP_TOKEN_RETRY outcome case=eo-false-replicated step=3 rows=3
```

Assertion summary: after step 2 the table still reads back as step 1's three rows (the changed-bytes retry was dropped, not stored), and after step 3 it is unchanged. The research premise (same token, changed bytes, dropped) holds on today's `latest`, so the plan continued.

Matches text: pending (20-03)

### E-03: dedup outcome table

Backs: `docs/ROLLOUT-v1.7.0_gg2.md`, the dedup item: the consequence of a retried range, the window conditions, and the `exactlyOnce=false` wording (written in plan 20-03). Server version for every row: `26.9.18.11` (E-01). Every row below is a testcase of `DedupTokenRetryLiveTest`, all passing, and each step is read back by typed SQL and compared with the expected rows, never counted.

```sh
env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew test --tests '*DedupTokenRetryLiveTest' --rerun
python3 -I -c 'import sys,xml.etree.ElementTree as E;r=E.parse(sys.argv[1]).getroot();print([int(r.get(k)) for k in ("tests","failures","errors","skipped")]);o="".join(x.text or "" for x in r.iter("system-out"));print("\n".join(l.split("DedupTokenRetryLiveTest ",1)[1] for l in o.split("\n") if "DEDUP_TOKEN_RETRY outcome" in l))' build/test-results/test/TEST-com.clickhouse.kafka.connect.sink.DedupTokenRetryLiveTest.xml
```

```text
[8, 0, 0, 0]
DEDUP_TOKEN_RETRY outcome case=eo-false-plain step=1 rows=3
DEDUP_TOKEN_RETRY outcome case=eo-false-plain step=2 rows=6
DEDUP_TOKEN_RETRY outcome case=eo-false-plain step=3 rows=9
DEDUP_TOKEN_RETRY outcome case=server-replicated step=1 rows=2
DEDUP_TOKEN_RETRY outcome case=server-replicated step=2 rows=2
DEDUP_TOKEN_RETRY outcome case=server-replicated step=3 rows=2
DEDUP_TOKEN_RETRY outcome case=server-replicated step=4 rows=4
DEDUP_TOKEN_RETRY outcome case=server-non-replicated-window step=1 rows=2
DEDUP_TOKEN_RETRY outcome case=server-non-replicated-window step=2 rows=2
DEDUP_TOKEN_RETRY outcome case=server-non-replicated-window step=3 rows=2
DEDUP_TOKEN_RETRY outcome case=server-non-replicated-window step=4 rows=4
DEDUP_TOKEN_RETRY outcome case=server-plain step=1 rows=2
DEDUP_TOKEN_RETRY outcome case=server-plain step=2 rows=4
DEDUP_TOKEN_RETRY outcome case=server-plain step=3 rows=5
DEDUP_TOKEN_RETRY outcome case=server-plain step=4 rows=7
DEDUP_TOKEN_RETRY outcome case=eo-true-not-landed step=1 rows=2
DEDUP_TOKEN_RETRY outcome case=eo-true-not-landed step=2 rows=3
DEDUP_TOKEN_RETRY outcome case=eo-true-landed step=1 rows=4
DEDUP_TOKEN_RETRY outcome case=eo-true-landed step=2 rows=5
DEDUP_TOKEN_RETRY outcome case=eo-false-replicated step=1 rows=3
DEDUP_TOKEN_RETRY outcome case=eo-false-replicated step=2 rows=3
DEDUP_TOKEN_RETRY outcome case=eo-false-replicated step=3 rows=3
```

| Testcase (display name) | Engine | Steps (what is sent) and rows in the table after each | What it shows |
|---|---|---|---|
| `D-13: same-token and different-token inserts on REPLICATED` | `ReplicatedMergeTree` | 1 block A (2 rows), token T1: 2. 2 block A again, T1: 2. 3 a changed 1-row block, T1: 2. 4 block A, different token T2: 4. | Same token is dropped whether the bytes are identical or changed; identical data under a different token lands. |
| `D-13: same-token and different-token inserts on NON_REPLICATED_WINDOW` | `MergeTree` with `non_replicated_deduplication_window = 100` | same four steps: 2, 2, 2, 4. | A non-replicated table deduplicates by token only when it has a window. |
| `D-13: same-token and different-token inserts on PLAIN` | `MergeTree`, window 0 | same four steps: 2, 4, 5, 7. | With no window every insert lands, the token is ignored. |
| `D-13: an exactlyOnce retry of a landed range is dropped and keeps the earlier rows` | `ReplicatedMergeTree` | The earlier attempt is simulated as the pre-fix connector wrote it (2 real rows plus 2 all-defaults phantom rows) under the token the connector sends for offsets 1-4, and the range is left `BEFORE_PROCESSING`. 1 the task retries offsets 1-4: 4 (the same four rows, no row written twice), state reads `AFTER_PROCESSING` min 1 max 4. 2 the next range, offsets 5-6: 5 (one real row added). | The retry of a landed range is dropped by the token and the old rows, phantom rows included, stay. |
| `D-13: an exactlyOnce retry of a range that never landed stores only the real rows` | `ReplicatedMergeTree` | Same `BEFORE_PROCESSING` state, nothing inserted before. 1 the task retries offsets 1-4: 2 (only the two real rows, no phantom row). 2 the next range: 3. | If the earlier attempt did not land, the retry lands the correct rows once. |
| `D-14: with exactlyOnce=false a re-delivered range on ReplicatedMergeTree is dropped by its token, identical or changed` | `ReplicatedMergeTree` | 1 offsets 0-2 through a fresh task: 3. 2 the same offsets with every name changed, a fresh task: 3. 3 the original rows again, a fresh task: 3. | Without exactly-once the connector keeps no offset state, and the token alone drops the re-delivery. |
| `D-14: a re-delivered range on a plain MergeTree is stored a second time, identical or changed` | `MergeTree`, window 0 | same three steps: 3, 6, 9. | On a table without a window a re-delivery lands as new rows. |
| `D-04: the server version and the deduplication windows are logged and the replicated window is above zero` | n/a | see E-01 | Window values for the wording. |

No cell of this table records "rows written twice because the bytes differ": a changed-bytes retry under the same token was dropped on every deduplicating engine. The only duplicates observed are on the plain table, where the window is 0.

For the two `eo-true` connector cases the read-back leaves out the `ts` column, as `EmptyCdcSkipRouteLiveTest` does for the `bypassRowBinary` route (the first run of those cases showed that route storing a different `ts` than the source instant; that is existing behaviour of the metadata-free JSON path and not what this class pins). The compared columns are `id`, `amount`, `name`, `_version` and `is_deleted`. The simulated earlier attempt of the landed case inserts those same columns and leaves `ts` at its column default.

Matches text: pending (20-03)
