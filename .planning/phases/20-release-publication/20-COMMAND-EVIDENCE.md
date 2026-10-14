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

### E-04: seen-red without the connector's token

Backs: no doc text. It proves that the connector-path pins depend on the token the connector sends (Pitfall 1: a pin that would also pass with no token proves nothing). Nothing below is committed: both edits to `src/main` were reverted with `git checkout` before any commit.

**Run A (the mutation the plan names).** `QueryIdentifier.getDeduplicationToken()` was edited to return null for every identifier, then the class was run with `--rerun`. Without a token the server falls back to hashing the data: an identical retry is still dropped, but a retry whose bytes changed lands (for example the changed-bytes retry of the ReplicatedMergeTree table read back both the old and the changed rows). Because the test builds its own tokens through the same method (it never copies the format string), the two server-matrix cases on the deduplicating engines went red as well; the plan had expected those to stay green. Run A therefore shows the pins depend on the token, but not by itself that the connector is the sender.

```text
SEEN-RED failed: D-13: same-token and different-token inserts on REPLICATED
SEEN-RED failed: D-13: same-token and different-token inserts on NON_REPLICATED_WINDOW
SEEN-RED failed: D-13: an exactlyOnce retry of a landed range is dropped and keeps the earlier rows
SEEN-RED failed: D-14: with exactlyOnce=false a re-delivered range on ReplicatedMergeTree is dropped by its token, identical or changed
SEEN-RED passed: 4 other testcases
SEEN-RED restored: git diff --quiet -- src/main exit=0
```

**Run B (isolating the connector).** To separate the connector from the test's own token builder, a second temporary edit changed only `Processing.doInsert(List, RangeContainer)` so it built the partition-less identifier (no token), leaving `QueryIdentifier` untouched. Only the two connector-path cases that assert a dropped retry went red; the whole server matrix stayed green, and the case whose earlier attempt never landed stayed green because there is nothing to drop.

```text
ISOLATED-RED failed: D-13: an exactlyOnce retry of a landed range is dropped and keeps the earlier rows
ISOLATED-RED failed: D-14: with exactlyOnce=false a re-delivered range on ReplicatedMergeTree is dropped by its token, identical or changed
ISOLATED-RED passed: 6 other testcases
ISOLATED-RED restored: git diff --quiet -- src/main exit=0
```

Green re-run after both restores (`env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew test --tests '*DedupTokenRetryLiveTest' --rerun`): tests=8 failures=0 errors=0 skipped=0.

Matches text: n/a (gate check)

### E-10: probe versions

Backs: no doc text on its own; it fixes which code each later probe entry (E-11 onwards) was run against. The probe is one throwaway JUnit class, `Phase20RolloutProbe`, kept under `/tmp/gsd20/probe` and copied into two detached worktrees. The gg1 worktree is the published v1.7.0_gg1 code (`be6b8861c25f3d7611194e7150b18362c5c75e32`, `origin/custom_build`); the gg2 worktree is the VERSION bump commit `43c82975` (`v1.7.0_gg2`, all Phase 17 to 19 code). Both ran against ClickHouse `latest` with client V2 in their own Testcontainers container, and `latest` was pulled again for each run; both reported the same server.

```sh
git worktree add --detach /tmp/gsd20/gg1 be6b8861c25f3d7611194e7150b18362c5c75e32
git worktree add --detach /tmp/gsd20/gg2 43c82975
env -u TESTCONTAINERS_HOST_OVERRIDE /tmp/gsd20/gg1/gradlew -p /tmp/gsd20/gg1 test --tests '*Phase20RolloutProbe' --rerun
env -u TESTCONTAINERS_HOST_OVERRIDE /tmp/gsd20/gg2/gradlew -p /tmp/gsd20/gg2 test --tests '*Phase20RolloutProbe' --rerun
```

```text
PROBE-VERSION tree=gg1 server=26.9.18.11 artifact=v1.7.0_gg1
PROBE-VERSION tree=gg2 server=26.9.18.11 artifact=v1.7.0_gg2
```

Matches text: n/a (gate check)

### E-11: Nullable(Tuple) wrong row and phantom row, gg1 versus gg2

Backs: `docs/ROLLOUT-v1.7.0_gg2.md`, the 999.1 item, the `Nullable(Tuple)` detection query (written in plan 20-03). Fixture: a plain `MergeTree ORDER BY id` table `(id Int32, c Nullable(Tuple(a Nullable(Int32), b String)))` with no DEFAULT column (the plain twin of 19-SPIKE C2). Three schema'd `Struct` records, ids 1 to 3, each with a non-NULL tuple whose `a` (5, 6, 7) and `b` (`y`, `z`, `w`) are non-NULL, put in one batch through `ClickHouseSinkTask` on the RowBinary route (base properties, no `bypassRowBinary`). Typed read-back is `id, isNull(c), tupleElement(c,'a'), tupleElement(c,'b')` (a NULL element prints as `NULL`).

What each tree stored (from the `PROBE-OBSERVED` lines below): the gg1 code stored the three records as wrong rows (tuple not NULL, `a` NULL, `b` empty), and added three more rows under id `16777216` that no record carries, each with a NULL tuple (a phantom row per record); no error was raised. The gg2 code stored the three records exactly as sent and nothing else. The phantom rows have no content that identifies them by themselves (their key is garbage derived from the shifted bytes), so the query lists them by where they sit: a NULL tuple in a part that also holds a wrong-row suspect. Both kinds of hit are suspects, not proof: stored data cannot tell a legitimate NULL, or a legitimate tuple of NULL and empty string, from a wrongly stored one.

Doc-form query (SELECT only; the probe reads this exact text from `/tmp/gsd20/probe/sql/tuple-c2-plain.sql`, replaces the four placeholders and runs it otherwise unchanged):

```sql
SELECT
    <key_column> AS row_key,
    _part AS part,
    if(<column> IS NULL,
       'NULL tuple in a part that also holds a suspect (possible phantom row)',
       'tuple is not NULL but every element is NULL or empty') AS reason
FROM `<db>`.`<table>`
WHERE _part IN (
        SELECT _part
        FROM `<db>`.`<table>`
        WHERE <column> IS NOT NULL
          AND toString(assumeNotNull(<column>)) = toString(defaultValueOfTypeName(toTypeName(assumeNotNull(<column>))))
    )
  AND (
        <column> IS NULL
        OR toString(assumeNotNull(<column>)) = toString(defaultValueOfTypeName(toTypeName(assumeNotNull(<column>))))
    )
ORDER BY part, row_key;
```

Substitution map (gg1 run; the gg2 run used the same placeholders with its own database and table names, `kafka_connect_test_228901658_1791983368853` and `p20_tuple_c2_1791983368974`): `<db>` = `kafka_connect_test_497821928_1791983352541`, `<table>` = `p20_tuple_c2_1791983352677`, `<column>` = `c`, `<key_column>` = `id`.

```sh
env -u TESTCONTAINERS_HOST_OVERRIDE /tmp/gsd20/gg1/gradlew -p /tmp/gsd20/gg1 test --tests '*Phase20RolloutProbe' --rerun
env -u TESTCONTAINERS_HOST_OVERRIDE /tmp/gsd20/gg2/gradlew -p /tmp/gsd20/gg2 test --tests '*Phase20RolloutProbe' --rerun
python3 -I /tmp/gsd20/probe/xml-probe-lines.py /tmp/gsd20/exec/probe-gg1-t1.xml
python3 -I /tmp/gsd20/probe/xml-probe-lines.py /tmp/gsd20/exec/probe-gg2-t1.xml
```

`SELECT version()` of both runs: `26.9.18.11` (E-10).

```text
PROBE-OBSERVED tree=gg1 shape=tuple-c2-plain outcome=stored match=no detail=1,0,NULL,;2,0,NULL,;3,0,NULL,;16777216,1,NULL,NULL;16777216,1,NULL,NULL;16777216,1,NULL,NULL
PROBE-RESULT tree=gg1 shape=tuple-c2-plain flagged=6
PROBE-SUSPECTS tree=gg1 shape=tuple-c2-plain rows=1,all_1_1_0,tuple is not NULL but every element is NULL or empty;2,all_1_1_0,tuple is not NULL but every element is NULL or empty;3,all_1_1_0,tuple is not NULL but every element is NULL or empty;16777216,all_1_1_0,NULL tuple in a part that also holds a suspect (possible phantom row);16777216,all_1_1_0,NULL tuple in a part that also holds a suspect (possible phantom row);16777216,all_1_1_0,NULL tuple in a part that also holds a suspect (possible phantom row)
PROBE-OBSERVED tree=gg2 shape=tuple-c2-plain outcome=stored match=yes detail=1,0,5,y;2,0,6,z;3,0,7,w
PROBE-RESULT tree=gg2 shape=tuple-c2-plain flagged=0
```

An empty result of this query means no row of the column matches the shape, not that the column is healthy: a wrongly stored tuple whose elements are not all NULL or empty is not found by it.

Matches text: pending (20-03)
