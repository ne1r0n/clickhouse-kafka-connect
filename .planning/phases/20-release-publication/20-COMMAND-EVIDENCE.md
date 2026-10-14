# Phase 20: Command evidence (D-03, D-04)

- **Base commit:** `P20_BASE` = `d73848807c58c4ba535da05e7515ecbbf5025bdf` (the tip when Phase 20 planning started; every "Phase 20 changed nothing here" check diffs non-`.planning` paths against it).
- **Shell:** macOS zsh, BSD `grep`. Gradle is always run as `env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew ...` because this shell sets that variable to the literal `null`. Log lines carry the local time zone (UTC+3); the date below is UTC.
- **Date (UTC):** 2026-10-14.
- **Rule:** a `Matches text: no` verdict means the doc text gets fixed, never the evidence. Entries that back no doc text carry `Matches text: n/a (gate check)`. Entries that back text not yet written carry a pending verdict until plan 20-03 writes that text; every entry that backs the gg2 rollout note now reads `Matches text: yes`.

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

Matches text: yes

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

Matches text: yes

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

Matches text: yes

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

Matches text: yes

### E-12: Nullable(Enum8) and Nullable(Enum16) at the top level, gg1 versus gg2

Backs: `docs/ROLLOUT-v1.7.0_gg2.md`, the 999.1 item, the `Nullable(Enum)` detection query (written in plan 20-03). Fixtures, each a plain `MergeTree ORDER BY id` table with no DEFAULT column and three schema'd `Struct` records (ids 1 to 3, column `c` holding the string label `a`, `b`, `a`, never NULL) put in one batch through `ClickHouseSinkTask` on the RowBinary route: `c Nullable(Enum8('a' = 1, 'b' = 2))` (shape `enum8-top`) and `c Nullable(Enum16('a' = 1, 'b' = 1000))` (shape `enum16-top`). Typed read-back is `id, ifNull(toString(c), 'NULL')`.

What each tree did: the gg1 code stored the three Enum8 rows with `c` NULL and raised no error (every label lost, silently); on the Enum16 table it failed the batch with the server error `CANNOT_READ_ALL_DATA` and left no row. The gg2 code stored both tables exactly as sent. Because the gg1 code failed the Enum16 batch, no Enum16 row was ever stored by it, so the query has nothing to find for Enum16 columns written by gg1; the Enum16 run only shows that the same query flags nothing on gg2 rows.

Final-run versions (both runs, same fixture records):

```text
PROBE-VERSION tree=gg1 server=26.9.18.11 artifact=v1.7.0_gg1
PROBE-VERSION tree=gg2 server=26.9.18.11 artifact=v1.7.0_gg2
```

Doc-form query (SELECT only; one text serves both Enum widths; the probe reads it from `/tmp/gsd20/probe/sql/enum-top.sql`, replaces the placeholders and runs it otherwise unchanged):

```sql
SELECT
    <key_column> AS row_key,
    _part AS part,
    'Nullable(Enum) column holds NULL' AS reason
FROM `<db>`.`<table>`
WHERE <column> IS NULL
ORDER BY part, row_key;
```

Substitution map: `<column>` = `c`, `<key_column>` = `id`. gg1 run: `<db>` = `kafka_connect_test_1254766197_1791983592900`, `<table>` = `p20_enum8_top_1791983593467` (Enum8) and `p20_enum16_top_1791983593912` (Enum16). gg2 run: `<db>` = `kafka_connect_test_696237895_1791983608390`, `<table>` = `p20_enum8_top_1791983608922` and `p20_enum16_top_1791983609863`.

```sh
env -u TESTCONTAINERS_HOST_OVERRIDE /tmp/gsd20/gg1/gradlew -p /tmp/gsd20/gg1 test --tests '*Phase20RolloutProbe' --rerun
env -u TESTCONTAINERS_HOST_OVERRIDE /tmp/gsd20/gg2/gradlew -p /tmp/gsd20/gg2 test --tests '*Phase20RolloutProbe' --rerun
python3 -I /tmp/gsd20/probe/xml-probe-lines.py /tmp/gsd20/exec/probe-gg1.xml
python3 -I /tmp/gsd20/probe/xml-probe-lines.py /tmp/gsd20/exec/probe-gg2.xml
```

`SELECT version()` of both runs: `26.9.18.11` (the `PROBE-VERSION` lines of the final runs are in E-12).

```text
PROBE-OBSERVED tree=gg1 shape=enum8-top outcome=stored match=no detail=1,NULL;2,NULL;3,NULL
PROBE-RESULT tree=gg1 shape=enum8-top flagged=3
PROBE-SUSPECTS tree=gg1 shape=enum8-top rows=1,all_1_1_0,Nullable(Enum) column holds NULL;2,all_1_1_0,Nullable(Enum) column holds NULL;3,all_1_1_0,Nullable(Enum) column holds NULL
PROBE-OBSERVED tree=gg1 shape=enum16-top outcome=failed match=n/a detail=ServerException: Code: 33. DB::Exception: Cannot read all data. Bytes read: 1. Bytes expected: 4: (at row 4) : While executing WaitForAsyncInsert. (CANNOT_READ_ALL_DATA) (version 26.9.18.11 (official build))  rowsLeft=0 chain=RuntimeException: Number of records: 3 | RuntimeException: Number of records: 3 | RuntimeException: Topic: [p20_enum16_top_1791983593912], Partition: [0], MinOffset: [0], MaxOffset: [2], (QueryId: [78da0eb9-5c95-44ef-bc3
PROBE-RESULT tree=gg1 shape=enum16-top flagged=0
PROBE-OBSERVED tree=gg2 shape=enum8-top outcome=stored match=yes detail=1,a;2,b;3,a
PROBE-RESULT tree=gg2 shape=enum8-top flagged=0
PROBE-OBSERVED tree=gg2 shape=enum16-top outcome=stored match=yes detail=1,a;2,b;3,a
PROBE-RESULT tree=gg2 shape=enum16-top flagged=0
```

A hit is a suspect, never proof: stored data cannot always tell a legitimate NULL from a wrongly stored one. An empty result means no row matches the shape, not that the table is healthy.

Matches text: yes

### E-13: Array(Nullable(Enum8)), gg1 versus gg2

Backs: `docs/ROLLOUT-v1.7.0_gg2.md`, the 999.1 item, the Enum array detection query (written in plan 20-03). Fixture: a plain `MergeTree ORDER BY id` table `(id Int32, c Array(Nullable(Enum8('a' = 1, 'b' = 2))))`, two schema'd `Struct` records (ids 1 and 2) each with the array `['a', 'b']` and no NULL element, one batch, RowBinary route. Typed read-back is `id, toString(c)`. The gg1 code stored both arrays as `[NULL,NULL]` with no error; the gg2 code stored `['a','b']`.

Doc-form query (SELECT only; `/tmp/gsd20/probe/sql/enum-array.sql`):

```sql
SELECT
    <key_column> AS row_key,
    _part AS part,
    'array holds a NULL element' AS reason
FROM `<db>`.`<table>`
WHERE arrayExists(x -> x IS NULL, <column>)
ORDER BY part, row_key;
```

Substitution map: `<column>` = `c`, `<key_column>` = `id`. gg1 run: `<db>` = `kafka_connect_test_1254766197_1791983592900`, `<table>` = `p20_enum8_array_1791983592975`. gg2 run: `<db>` = `kafka_connect_test_696237895_1791983608390`, `<table>` = `p20_enum8_array_1791983608463`.

```sh
env -u TESTCONTAINERS_HOST_OVERRIDE /tmp/gsd20/gg1/gradlew -p /tmp/gsd20/gg1 test --tests '*Phase20RolloutProbe' --rerun
env -u TESTCONTAINERS_HOST_OVERRIDE /tmp/gsd20/gg2/gradlew -p /tmp/gsd20/gg2 test --tests '*Phase20RolloutProbe' --rerun
python3 -I /tmp/gsd20/probe/xml-probe-lines.py /tmp/gsd20/exec/probe-gg1.xml
python3 -I /tmp/gsd20/probe/xml-probe-lines.py /tmp/gsd20/exec/probe-gg2.xml
```

`SELECT version()` of both runs: `26.9.18.11` (the `PROBE-VERSION` lines of the final runs are in E-12).

```text
PROBE-OBSERVED tree=gg1 shape=enum8-array outcome=stored match=no detail=1,[NULL,NULL];2,[NULL,NULL]
PROBE-RESULT tree=gg1 shape=enum8-array flagged=2
PROBE-SUSPECTS tree=gg1 shape=enum8-array rows=1,all_1_1_0,array holds a NULL element;2,all_1_1_0,array holds a NULL element
PROBE-OBSERVED tree=gg2 shape=enum8-array outcome=stored match=yes detail=1,['a','b'];2,['a','b']
PROBE-RESULT tree=gg2 shape=enum8-array flagged=0
```

A hit is a suspect, never proof: stored data cannot always tell a legitimate NULL from a wrongly stored one. An empty result means no row matches the shape, not that the table is healthy.

Matches text: yes

### E-14: Map(String, Nullable(Enum8)), gg1 versus gg2

Backs: `docs/ROLLOUT-v1.7.0_gg2.md`, the 999.1 item, the Enum map detection query (written in plan 20-03). Fixture: a plain `MergeTree ORDER BY id` table `(id Int32, c Map(String, Nullable(Enum8('a' = 1, 'b' = 2))))`, two schema'd `Struct` records (ids 1 and 2) each with the map `{k1: a, k2: b}` and no NULL value, one batch, RowBinary route. Typed read-back is `id, toString(c)`. The gg1 code stored both maps as `{'k1':NULL,'k2':NULL}` with no error; the gg2 code stored `{'k1':'a','k2':'b'}`.

Doc-form query (SELECT only; `/tmp/gsd20/probe/sql/enum-map.sql`):

```sql
SELECT
    <key_column> AS row_key,
    _part AS part,
    'map holds a NULL value' AS reason
FROM `<db>`.`<table>`
WHERE arrayExists(x -> x IS NULL, mapValues(<column>))
ORDER BY part, row_key;
```

Substitution map: `<column>` = `c`, `<key_column>` = `id`. gg1 run: `<db>` = `kafka_connect_test_1254766197_1791983592900`, `<table>` = `p20_enum8_map_1791983593364`. gg2 run: `<db>` = `kafka_connect_test_696237895_1791983608390`, `<table>` = `p20_enum8_map_1791983608815`.

```sh
env -u TESTCONTAINERS_HOST_OVERRIDE /tmp/gsd20/gg1/gradlew -p /tmp/gsd20/gg1 test --tests '*Phase20RolloutProbe' --rerun
env -u TESTCONTAINERS_HOST_OVERRIDE /tmp/gsd20/gg2/gradlew -p /tmp/gsd20/gg2 test --tests '*Phase20RolloutProbe' --rerun
python3 -I /tmp/gsd20/probe/xml-probe-lines.py /tmp/gsd20/exec/probe-gg1.xml
python3 -I /tmp/gsd20/probe/xml-probe-lines.py /tmp/gsd20/exec/probe-gg2.xml
```

`SELECT version()` of both runs: `26.9.18.11` (the `PROBE-VERSION` lines of the final runs are in E-12).

```text
PROBE-OBSERVED tree=gg1 shape=enum8-map outcome=stored match=no detail=1,{'k1':NULL,'k2':NULL};2,{'k1':NULL,'k2':NULL}
PROBE-RESULT tree=gg1 shape=enum8-map flagged=2
PROBE-SUSPECTS tree=gg1 shape=enum8-map rows=1,all_1_1_0,map holds a NULL value;2,all_1_1_0,map holds a NULL value
PROBE-OBSERVED tree=gg2 shape=enum8-map outcome=stored match=yes detail=1,{'k1':'a','k2':'b'};2,{'k1':'a','k2':'b'}
PROBE-RESULT tree=gg2 shape=enum8-map flagged=0
```

A hit is a suspect, never proof: stored data cannot always tell a legitimate NULL from a wrongly stored one. An empty result means no row matches the shape, not that the table is healthy.

Matches text: yes

### E-15: single-field Nullable(Tuple) with a NULL field, gg1 versus gg2

Backs: `docs/ROLLOUT-v1.7.0_gg2.md`, the 999.1 item, the single-field tuple detection query, and the "what changed" text for ledger L-38 (written in plan 20-03). Fixture: a plain `MergeTree ORDER BY id` table `(id Int32, c Nullable(Tuple(a Int32)))` and one schema'd `Struct` record (id 1) whose tuple has `a` = null, RowBinary route. Typed read-back is `id, isNull(c), tupleElement(c, 'a')`.

What each tree did: the gg1 code stored the row with a NULL tuple (`c` is NULL), silently. The gg2 code refused the batch with a `DataException` whose message is `column 'c' (Nullable(Tuple(...))) tuple field 'a': null in non-nullable slot`, under `errors.tolerance=none`, and left no row (ledger L-38). Since gg2 stores nothing, the query returns nothing there.

Doc-form query (SELECT only; `/tmp/gsd20/probe/sql/tuple-x1-null-field.sql`):

```sql
SELECT
    <key_column> AS row_key,
    _part AS part,
    'single-field Nullable(Tuple) holds a NULL tuple' AS reason
FROM `<db>`.`<table>`
WHERE <column> IS NULL
ORDER BY part, row_key;
```

Substitution map: `<column>` = `c`, `<key_column>` = `id`. gg1 run: `<db>` = `kafka_connect_test_1254766197_1791983592900`, `<table>` = `p20_tuple_x1_1791983593567`. gg2 run: `<db>` = `kafka_connect_test_696237895_1791983608390`, `<table>` = `p20_tuple_x1_1791983609212`.

```sh
env -u TESTCONTAINERS_HOST_OVERRIDE /tmp/gsd20/gg1/gradlew -p /tmp/gsd20/gg1 test --tests '*Phase20RolloutProbe' --rerun
env -u TESTCONTAINERS_HOST_OVERRIDE /tmp/gsd20/gg2/gradlew -p /tmp/gsd20/gg2 test --tests '*Phase20RolloutProbe' --rerun
python3 -I /tmp/gsd20/probe/xml-probe-lines.py /tmp/gsd20/exec/probe-gg1.xml
python3 -I /tmp/gsd20/probe/xml-probe-lines.py /tmp/gsd20/exec/probe-gg2.xml
```

`SELECT version()` of both runs: `26.9.18.11` (the `PROBE-VERSION` lines of the final runs are in E-12).

```text
PROBE-OBSERVED tree=gg1 shape=tuple-x1-null-field outcome=stored match=no detail=1,1,NULL
PROBE-RESULT tree=gg1 shape=tuple-x1-null-field flagged=1
PROBE-SUSPECTS tree=gg1 shape=tuple-x1-null-field rows=1,all_1_1_0,single-field Nullable(Tuple) holds a NULL tuple
PROBE-OBSERVED tree=gg2 shape=tuple-x1-null-field outcome=failed match=n/a detail=DataException: column 'c' (Nullable(Tuple(...))) tuple field 'a': null in non-nullable slot rowsLeft=0 chain=RuntimeException: Number of records: 1 | RuntimeException: Number of records: 1 | RuntimeException: Topic: [p20_tuple_x1_1791983609212], Partition: [0], MinOffset: [0], MaxOffset: [0], (QueryId: [1ece109c-d744-4487-a6da-
PROBE-RESULT tree=gg2 shape=tuple-x1-null-field flagged=0
```

A hit is a suspect, never proof: stored data cannot always tell a legitimate NULL from a wrongly stored one. An empty result means no row matches the shape, not that the table is healthy.

Matches text: yes

### E-16: empty CDC records on the bypassRowBinary JSON route, gg1 versus gg2

Backs: `docs/ROLLOUT-v1.7.0_gg2.md`, the empty-CDC (phantom row) item, ledger L-30 (written in plan 20-03). Fixture: `bypassRowBinary=true` and `debeziumCDCEnabled=true`, a plain `MergeTree ORDER BY id` table `(id Int32, ts DateTime64(3, 'UTC'), amount Decimal(18,4), name String, _version UInt64, is_deleted UInt8)`, and one batch of four Debezium envelopes: `op=c` (after: id 1, `created`, lsn 100), `op=t`, `op=u` with a null `after`, and `op=d` (before: id 2, `deleted`, lsn 101). Typed read-back is `id, amount, name, _version, is_deleted` (`ts` is left out: this route does not store the source instant, see E-03).

What each tree did: the gg1 code stored the two real rows plus two all-defaults phantom rows (`id` 0, `amount` 0, empty `name`, `_version` 0, `is_deleted` 0), one per empty record. The gg2 code stored only the two real rows (ledger L-30). The query lists rows whose key and `_version` are both 0; on a real table a row can have key 0 legitimately, so the hits are suspects to compare with the source.

Doc-form query (SELECT only; `/tmp/gsd20/probe/sql/empty-cdc-bypass.sql`):

```sql
SELECT
    <key_column> AS row_key,
    _part AS part,
    'key and version are both 0 (possible phantom row of an empty CDC record)' AS reason
FROM `<db>`.`<table>`
WHERE <key_column> = 0
  AND <version_column> = 0
ORDER BY part, row_key;
```

Substitution map: `<key_column>` = `id`, `<version_column>` = `_version`. gg1 run: `<db>` = `kafka_connect_test_1254766197_1791983592900`, `<table>` = `p20_empty_cdc_1791983593674`. gg2 run: `<db>` = `kafka_connect_test_696237895_1791983608390`, `<table>` = `p20_empty_cdc_1791983609618`.

```sh
env -u TESTCONTAINERS_HOST_OVERRIDE /tmp/gsd20/gg1/gradlew -p /tmp/gsd20/gg1 test --tests '*Phase20RolloutProbe' --rerun
env -u TESTCONTAINERS_HOST_OVERRIDE /tmp/gsd20/gg2/gradlew -p /tmp/gsd20/gg2 test --tests '*Phase20RolloutProbe' --rerun
python3 -I /tmp/gsd20/probe/xml-probe-lines.py /tmp/gsd20/exec/probe-gg1.xml
python3 -I /tmp/gsd20/probe/xml-probe-lines.py /tmp/gsd20/exec/probe-gg2.xml
```

`SELECT version()` of both runs: `26.9.18.11` (the `PROBE-VERSION` lines of the final runs are in E-12).

```text
PROBE-OBSERVED tree=gg1 shape=empty-cdc-bypass outcome=stored match=no detail=0,0,,0,0;0,0,,0,0;1,12.3456,created,100,0;2,12.3456,deleted,101,1
PROBE-RESULT tree=gg1 shape=empty-cdc-bypass flagged=2
PROBE-SUSPECTS tree=gg1 shape=empty-cdc-bypass rows=0,all_1_1_0,key and version are both 0 (possible phantom row of an empty CDC record);0,all_1_1_0,key and version are both 0 (possible phantom row of an empty CDC record)
PROBE-OBSERVED tree=gg2 shape=empty-cdc-bypass outcome=stored match=yes detail=1,12.3456,created,100,0;2,12.3456,deleted,101,1
PROBE-RESULT tree=gg2 shape=empty-cdc-bypass flagged=0
```

A hit is a suspect, never proof: stored data cannot always tell a legitimate NULL from a wrongly stored one. An empty result means no row matches the shape, not that the table is healthy.

Matches text: yes

### E-17: topic-scoped KeeperMap self-check on a connector-created state table

Backs: `docs/ROLLOUT-v1.7.0_gg2.md`, the exactly-once item's state-table self-check (written in plan 20-03); it is the gg1 note's item 1 query, now run live. The probe (gg2 run only) creates the state table through the connector's own code, `new KeeperStateProvider(new ClickHouseSinkConfig(props))` with `zkPath` `/kafka-connect` and `zkDatabase` `connect_state` against the server's embedded Keeper, then writes three state records through that provider: `orders` partition 0 `AFTER_PROCESSING` (offsets 0 to 9), `orders` partition 1 `BEFORE_PROCESSING` (10 to 19) and `orders-v2` partition 0 `BEFORE_PROCESSING` (0 to 4). It runs the query for topic `orders`, then sets `orders` partition 1 to `AFTER_PROCESSING` and runs it again.

Result: the first run returns exactly the one in-flight key of the listed topic, `orders-1`; it does not return `orders-0` (already `AFTER_PROCESSING`) and does not return `orders-v2-0`, which is `BEFORE_PROCESSING` under a topic that merely shares the prefix. The second run returns nothing.

Doc-form query (SELECT only; `/tmp/gsd20/probe/sql/keeper-self-check.sql`; the two `<topic>` placeholders are replaced by the same topic here, an operator replaces each with one of their topics):

```sql
SELECT `key`, minOffset, maxOffset, state
FROM `<zkDatabase>`
WHERE state != 'AFTER_PROCESSING'
  AND arrayStringConcat(arrayPopBack(splitByChar('-', `key`)), '-') IN ('<topic>', '<topic>')
ORDER BY `key`;
```

Substitution map: `<zkDatabase>` = `connect_state`, `<topic>` = `orders` (both occurrences). The query runs in the database the state table lives in (the probe's connection database).

```sh
env -u TESTCONTAINERS_HOST_OVERRIDE /tmp/gsd20/gg1/gradlew -p /tmp/gsd20/gg1 test --tests '*Phase20RolloutProbe' --rerun
env -u TESTCONTAINERS_HOST_OVERRIDE /tmp/gsd20/gg2/gradlew -p /tmp/gsd20/gg2 test --tests '*Phase20RolloutProbe' --rerun
python3 -I /tmp/gsd20/probe/xml-probe-lines.py /tmp/gsd20/exec/probe-gg1.xml
python3 -I /tmp/gsd20/probe/xml-probe-lines.py /tmp/gsd20/exec/probe-gg2.xml
```

`SELECT version()` of both runs: `26.9.18.11` (the `PROBE-VERSION` lines of the final runs are in E-12).

```text
PROBE-KEEPER case=in-flight keys=orders-1
PROBE-KEEPER-ROWS case=in-flight rows=orders-1,10,19,BEFORE_PROCESSING
PROBE-KEEPER case=all-after keys=
```

Matches text: yes

### E-18: table classification for the deduplication item

Backs: `docs/ROLLOUT-v1.7.0_gg2.md`, the dedup item, the "does my table deduplicate" self-check (written in plan 20-03). The probe (gg2 run only) creates four tables in its database: a default `ReplicatedMergeTree`, a `MergeTree` with `SETTINGS non_replicated_deduplication_window = 100`, a plain `MergeTree`, and a `ReplicatedMergeTree` with `SETTINGS replicated_deduplication_window = 0`. The query reads `engine` and `engine_full` from `system.tables`; a table-level `SETTINGS` value wins, otherwise the server default from `system.merge_tree_settings` applies (10000 for the replicated window and 0 for the non-replicated window on this server, E-01). A table deduplicates only if its window is above 0.

Result: the replicated default table and the table with a non-replicated window return 1; the plain `MergeTree` and the `ReplicatedMergeTree` with window 0 return 0. This agrees with E-03 (the same four cases measured by inserts: replicated and window dropped a same-token retry, plain and window 0 stored every insert).

Limits the note must state: the query does not evaluate `replicated_deduplication_window_seconds`, does not know server-level overrides set through a user profile, and was not run on an engine other than `MergeTree` and `ReplicatedMergeTree` (for example `SharedMergeTree` on ClickHouse Cloud is not covered).

Doc-form query (SELECT only; `/tmp/gsd20/probe/sql/dedup-tables.sql`; the `\\s` and `\\d` are written exactly as the probe sent them, so the server receives the regular expressions `\s` and `\d` after its own string unescaping):

```sql
SELECT
    name AS table_name,
    engine,
    toUInt8(
        if(engine LIKE 'Replicated%MergeTree',
           coalesce(
               toUInt64OrNull(extract(engine_full, 'replicated_deduplication_window\\s*=\\s*(\\d+)')),
               (SELECT toUInt64(value) FROM system.merge_tree_settings WHERE name = 'replicated_deduplication_window')
           ) > 0,
           coalesce(
               toUInt64OrNull(extract(engine_full, 'non_replicated_deduplication_window\\s*=\\s*(\\d+)')),
               (SELECT toUInt64(value) FROM system.merge_tree_settings WHERE name = 'non_replicated_deduplication_window')
           ) > 0)
    ) AS deduplicates
FROM system.tables
WHERE database = '<db>'
  AND engine LIKE '%MergeTree'
ORDER BY table_name;
```

Substitution map: `<db>` = `kafka_connect_test_696237895_1791983608390` (gg2 run).

```sh
env -u TESTCONTAINERS_HOST_OVERRIDE /tmp/gsd20/gg1/gradlew -p /tmp/gsd20/gg1 test --tests '*Phase20RolloutProbe' --rerun
env -u TESTCONTAINERS_HOST_OVERRIDE /tmp/gsd20/gg2/gradlew -p /tmp/gsd20/gg2 test --tests '*Phase20RolloutProbe' --rerun
python3 -I /tmp/gsd20/probe/xml-probe-lines.py /tmp/gsd20/exec/probe-gg1.xml
python3 -I /tmp/gsd20/probe/xml-probe-lines.py /tmp/gsd20/exec/probe-gg2.xml
```

`SELECT version()` of both runs: `26.9.18.11` (the `PROBE-VERSION` lines of the final runs are in E-12).

```text
PROBE-DEDUP-ROW table_name=p20_dedup_plain_1791983609027 engine=MergeTree deduplicates=0
PROBE-DEDUP-ROW table_name=p20_dedup_replicated_1791983609027 engine=ReplicatedMergeTree deduplicates=1
PROBE-DEDUP-ROW table_name=p20_dedup_replicated_off_1791983609027 engine=ReplicatedMergeTree deduplicates=0
PROBE-DEDUP-ROW table_name=p20_dedup_window_1791983609027 engine=MergeTree deduplicates=1
PROBE-DEDUP-TABLE kind=replicated deduplicates=1
PROBE-DEDUP-TABLE kind=window deduplicates=1
PROBE-DEDUP-TABLE kind=plain deduplicates=0
PROBE-DEDUP-TABLE kind=replicated-off deduplicates=0
```

Matches text: yes

### E-19: MBean name and client name, gg1 versus gg2

Backs: `docs/ROLLOUT-v1.7.0_gg2.md`, the MBean and client-name item (written in plan 20-03); there is no new doc query, the `user-agent` query is the gg1 note's item 7 query copied verbatim. In each run a sink task is started, one record is put through it, the `com.clickhouse:type=ClickHouseKafkaConnector,*` MBeans of that connector are listed while the task still runs, the task is stopped, `SYSTEM FLUSH LOGS` is executed as its own statement, and then the query below reads `system.query_log` of the run's own container.

Result: every MBean of the gg2 run carries `version=v1.7.0_gg2` (the per-task bean and the per-topic bean) and the client name the connector sends starts `clickhouse-kafka-connect/v1.7.0_gg2`; the gg1 run shows `v1.7.0_gg1` for both. The rest of the user agent (`clickhouse-java-v2/0.9.6 (...) Apache-HttpClient/5.6.2`) is the client library and does not change between the two.

Query copied from the gg1 note (SELECT only; `/tmp/gsd20/probe/sql/user-agent.sql`):

```sql
SELECT DISTINCT http_user_agent
FROM system.query_log
WHERE event_date >= today() - 1
  AND http_user_agent LIKE '%clickhouse-kafka-connect/%';
```

No placeholders.

```sh
env -u TESTCONTAINERS_HOST_OVERRIDE /tmp/gsd20/gg1/gradlew -p /tmp/gsd20/gg1 test --tests '*Phase20RolloutProbe' --rerun
env -u TESTCONTAINERS_HOST_OVERRIDE /tmp/gsd20/gg2/gradlew -p /tmp/gsd20/gg2 test --tests '*Phase20RolloutProbe' --rerun
python3 -I /tmp/gsd20/probe/xml-probe-lines.py /tmp/gsd20/exec/probe-gg1.xml
python3 -I /tmp/gsd20/probe/xml-probe-lines.py /tmp/gsd20/exec/probe-gg2.xml
```

`SELECT version()` of both runs: `26.9.18.11` (the `PROBE-VERSION` lines of the final runs are in E-12).

```text
PROBE-MBEAN tree=gg1 name=com.clickhouse:type=ClickHouseKafkaConnector,connector=Phase20RolloutProbe-mbeanAndClientName-8,name=SinkTask7,version=v1.7.0_gg1
PROBE-MBEAN tree=gg1 name=com.clickhouse:type=ClickHouseKafkaConnector,connector=Phase20RolloutProbe-mbeanAndClientName-8,name=SinkTask7,version=v1.7.0_gg1,topic=p20_mbean_1791983594077
PROBE-AGENT tree=gg1 agent=clickhouse-kafka-connect/v1.7.0_gg1 clickhouse-java-v2/0.9.6 (Mac OS X; jvm:17.0.9) Apache-HttpClient/5.6.2
PROBE-MBEAN tree=gg2 name=com.clickhouse:type=ClickHouseKafkaConnector,connector=Phase20RolloutProbe-mbeanAndClientName-10,name=SinkTask7,version=v1.7.0_gg2,topic=p20_mbean_1791983609960
PROBE-MBEAN tree=gg2 name=com.clickhouse:type=ClickHouseKafkaConnector,connector=Phase20RolloutProbe-mbeanAndClientName-10,name=SinkTask7,version=v1.7.0_gg2
PROBE-AGENT tree=gg2 agent=clickhouse-kafka-connect/v1.7.0_gg2 clickhouse-java-v2/0.9.6 (Mac OS X; jvm:17.0.9) Apache-HttpClient/5.6.2
```

Matches text: yes

### E-20: shapes that failed instead of storing, recorded as observed facts

Backs: `docs/ROLLOUT-v1.7.0_gg2.md`, the "what changed" text of the 999.1 and tuple items (written in plan 20-03). These two shapes did not store a wrong row, so they need no detection query; they are facts about which release refuses what.

- `enum16-top` on the gg1 code: the batch failed with the server error `CANNOT_READ_ALL_DATA` (code 33, `Cannot read all data. Bytes read: 1. Bytes expected: 4`), root-cause class `ServerException`, and no row was left (`rowsLeft=0`). The gg2 code stores the same batch (E-12).
- `tuple-x1-null-field` on the gg2 code: the batch failed with a `DataException` naming `tuple field 'a'` (root-cause class `DataException`, message `column 'c' (Nullable(Tuple(...))) tuple field 'a': null in non-nullable slot`), and no row was left (`rowsLeft=0`). The gg1 code stored a NULL tuple silently (E-15).

```text
PROBE-OBSERVED tree=gg1 shape=enum16-top outcome=failed match=n/a detail=ServerException: Code: 33. DB::Exception: Cannot read all data. Bytes read: 1. Bytes expected: 4: (at row 4) : While executing WaitForAsyncInsert. (CANNOT_READ_ALL_DATA) (version 26.9.18.11 (official build))  rowsLeft=0 chain=RuntimeException: Number of records: 3 | RuntimeException: Number of records: 3 | RuntimeException: Topic: [p20_enum16_top_1791983593912], Partition: [0], MinOffset: [0], MaxOffset: [2], (QueryId: [78da0eb9-5c95-44ef-bc3
PROBE-OBSERVED tree=gg2 shape=tuple-x1-null-field outcome=failed match=n/a detail=DataException: column 'c' (Nullable(Tuple(...))) tuple field 'a': null in non-nullable slot rowsLeft=0 chain=RuntimeException: Number of records: 1 | RuntimeException: Number of records: 1 | RuntimeException: Topic: [p20_tuple_x1_1791983609212], Partition: [0], MinOffset: [0], MaxOffset: [0], (QueryId: [1ece109c-d744-4487-a6da-
```

Matches text: yes

### E-30: alert-rule grep against a scratch directory

Backs: `docs/ROLLOUT-v1.7.0_gg2.md`, the error-message and log-line item, its "Am I affected?" command (written in plan 20-03). The command lists matchers in `<alert-rule-directory>` that still use one of the four texts this release removes or changes: the ERROR line `Column <name> is not nullable and no value is provided` (ledger L-26), the colon form `Error parsing date time string:` (ledger L-27), the message-less-failure NullPointerException that mentions `String.contains` (ledger L-23, L-24), and the startup WARN of the Nullable Enum and Tuple limitation, matched by the fragment `writes such slots without their null marker` (ledger L-34, L-39). It uses `-F` so the dots and parentheses of the texts need no escaping, and fragments without parentheses so the search does not depend on the matcher's own quoting.

Scratch directory `/tmp/gsd20/alerts/` (never committed): one file per old text, each holding one matcher line that spells the old text out, and one unrelated file (`kafka_consumergroup_lag > 1000`). `/tmp/gsd20/alerts-empty/` holds only the unrelated file. `/tmp/gsd20/alerts-escaped/` holds one matcher that writes the dot of `String.contains` as a regular-expression escape (`String\\.contains`).

Doc-form command (the note quotes exactly this, placeholder included):

```sh
grep -rnF -e 'is not nullable and no value is provided' -e 'Error parsing date time string:' -e 'String.contains' -e 'writes such slots without their null marker' <alert-rule-directory>
```

Substitution: `<alert-rule-directory>` = `/tmp/gsd20/alerts`, `/tmp/gsd20/alerts-empty` and `/tmp/gsd20/alerts-escaped` for the three runs; nothing else changed. Date (UTC) 2026-10-14, macOS BSD `grep`.

```text
$ grep -rnF -e 'is not nullable and no value is provided' -e 'Error parsing date time string:' -e 'String.contains' -e 'writes such slots without their null marker' /tmp/gsd20/alerts
/tmp/gsd20/alerts/rule-missing-value.yml:2:  expr: log_message =~ "Column .* is not nullable and no value is provided"
/tmp/gsd20/alerts/rule-null-message.yml:2:  expr: log_message =~ "Cannot invoke .String.contains.*getMessage.*is null"
/tmp/gsd20/alerts/rule-nullable-warn.yml:2:  expr: log_message =~ "The connector writes such slots without their null marker"
/tmp/gsd20/alerts/rule-datetime-parse.yml:2:  expr: log_message =~ "Error parsing date time string: .*"
exit=0
$ grep -rnF ... /tmp/gsd20/alerts-empty
exit=1
$ grep -rnF ... /tmp/gsd20/alerts-escaped
exit=1
```

Result: every old text is listed once and the unrelated file is not. The directory holding only the unrelated file prints nothing and exits 1, which is what an operator sees when no matcher uses an old text. A matcher that escapes the dot of `String.contains` is not listed, because `-F` compares literal text; the note says so.

Matches text: yes

### E-31: item 6 rollback on a dry-run merge of the release tip

Backs: `docs/ROLLOUT-v1.7.0_gg2.md`, item 6 (the rollback): `git fetch --tags origin`, `git checkout custom_build`, `git merge --ff-only origin/custom_build`, `git status --porcelain` printing nothing, `git read-tree -u --reset pre-v1.7.0_gg2`, `git commit -m "Roll back v1.7.0_gg2"`, `git diff --quiet HEAD pre-v1.7.0_gg2 && echo ok`, `git revert <rollback-commit>` and `git show <rollback-commit>^:docs/ROLLOUT-v1.7.0_gg2.md` (written in plan 20-03). Date (UTC) 2026-10-14.

Tag `pre-v1.7.0_gg2` is a local lightweight tag on `be6b8861c25f3d7611194e7150b18362c5c75e32` (`origin/custom_build`, the published `v1.7.0_gg1` tree); its `tests.yaml` has the trigger `branches: ['**']` and `git ls-remote --tags origin` does not list the tag. X is the tip that contains the finished note: `X = 8a913415d15ea47e4695c4fb3f9ad321975acb26` (the commit that added item 6). Throwaway merge: `git worktree add --detach /tmp/gsd20/dry be6b8861c25f3d7611194e7150b18362c5c75e32`, then in it `git -c user.name=demo -c user.email=demo@example.invalid merge --no-ff --no-edit X`, giving the merge commit `D = 7f64a8e7cf4419519a0630755f29f4b5eba18341`. D has two parents, `D^1` is `be6b8861`, `D^2` is X, and `git diff --quiet D X` holds (no non-merge difference), which printed `MERGE-DRY-RUN-OK`. D was never pushed, and plan 20-07 re-runs the script against the real merge commit before its push (the real release merge has a different tip). The dry-run worktree was removed and pruned afterwards.

The script lives outside the repository (`/tmp/gsd20/rollback-demo.sh <repo> <target> <worktree>`), adapted from the Phase 16 demo script. A throwaway branch that starts at `pre-v1.7.0_gg2` stands in for a stale local `custom_build`, so `git checkout custom_build` is the one command not run verbatim (a linked worktree cannot check out `custom_build` itself); the fast-forward target D stands in for `origin/custom_build`. The `-c user.name/user.email` options only give the throwaway commits an identity. Deviation from a published tag: `git fetch --tags origin` ran while the tag existed only locally, and it kept the local tag (the script prints it after the fetch).

```sh
#!/bin/bash
# usage: rollback-demo.sh <repo> <target-tip> <worktree-path>
# Runs item 6 as written, on a throwaway branch standing in for a stale local custom_build
# (starts at pre-v1.7.0_gg2, then fast-forwards to <target-tip>, the stand-in for origin/custom_build).
set -u
REPO=$1; TARGET=$2; WT=$3; BR=gg2-rollback-demo-$$
ID=(-c user.name=demo -c user.email=demo@example.invalid)
git -C "$REPO" worktree add --quiet -b "$BR" "$WT" 'pre-v1.7.0_gg2^{commit}'
g() { git -C "$WT" "$@"; }
TGT=$(git -C "$REPO" rev-parse "$TARGET")
echo "target=$TGT stale-start=$(g rev-parse --short HEAD)"
echo "x" >> "$WT/README.md"; echo "dirty: status --porcelain lines=$(g status --porcelain | wc -l | tr -d ' ')"; g checkout -q -- README.md
g fetch -q --tags origin && echo "fetch --tags exit=0"
echo "local tag kept after fetch: $(g rev-parse --short 'pre-v1.7.0_gg2^{commit}')"
g merge -q --ff-only "$TARGET" && echo "merge --ff-only exit=0, HEAD==target: $([ "$(g rev-parse HEAD)" = "$TGT" ] && echo yes)"
echo "clean: status --porcelain lines=$(g status --porcelain | wc -l | tr -d ' ')"
g read-tree -u --reset pre-v1.7.0_gg2 && g "${ID[@]}" commit -q -m "Roll back v1.7.0_gg2" && echo "read-tree + commit exit=0"
RB=$(g rev-parse HEAD); echo "rollback-commit parent==target: $([ "$(g rev-parse HEAD^)" = "$TGT" ] && echo yes)"
g diff --quiet HEAD pre-v1.7.0_gg2 && echo ok
[ -z "$(g status --porcelain)" ] && echo "worktree clean after commit"
g ls-files --error-unmatch docs/ROLLOUT-v1.7.0_gg2.md >/dev/null 2>&1 || echo "rollout doc gone from branch"
g show "$RB^:docs/ROLLOUT-v1.7.0_gg2.md" | grep -c '^## Item 6: Rollback' | sed 's/^/git show <rollback-commit>^:docs\/ROLLOUT... item-6 headings: /'
g show HEAD:.github/workflows/tests.yaml | grep -qF "branches: ['**']" && echo "rolled-back tests.yaml keeps branches: ['**']"
g "${ID[@]}" revert --no-edit "$RB" >/dev/null && g diff --quiet HEAD "$TARGET" && echo "after git revert <rollback-commit>: tree(HEAD)==tree(target)"
git -C "$REPO" worktree remove --force "$WT" && git -C "$REPO" worktree prune && git -C "$REPO" branch -q -D "$BR"
```

```text
X=8a913415d15ea47e4695c4fb3f9ad321975acb26
D=7f64a8e7cf4419519a0630755f29f4b5eba18341
MERGE-DRY-RUN-OK
target=7f64a8e7cf4419519a0630755f29f4b5eba18341 stale-start=be6b8861
dirty: status --porcelain lines=1
fetch --tags exit=0
local tag kept after fetch: be6b8861
merge --ff-only exit=0, HEAD==target: yes
clean: status --porcelain lines=0
read-tree + commit exit=0
rollback-commit parent==target: yes
ok
worktree clean after commit
rollout doc gone from branch
git show <rollback-commit>^:docs/ROLLOUT... item-6 headings: 1
rolled-back tests.yaml keeps branches: ['**']
after git revert <rollback-commit>: tree(HEAD)==tree(target)
```

Afterwards `git worktree list` shows only the main working tree, no `gg2-rollback-demo-*` branch is left, and `git ls-remote --tags origin` lists no `pre-v1.7.0_gg2`.

Matches text: yes

### E-40: FORK-DELTA raw numbers and src/main counts at ce9ea7ae

Backs: `docs/FORK-DELTA.md`, "The raw numbers" table (two new `after gg2` rows), the comparison paragraph, the "One number does bear" paragraph and the Adopted-wholesale `src/main` row (written in plan 20-04). Date (UTC) 2026-10-14. HEAD is `ce9ea7ae` on `sync/v1.7.0`. Nothing under `src`, `build.gradle.kts`, `gradle` or `.github` changed after `7ae6af45` (the last such commit, the dedup live test of plan 20-01), checked with `git diff --quiet d73848807c58c4ba535da05e7515ecbbf5025bdf HEAD -- src build.gradle.kts gradle .github ':(exclude)src/test/java/com/clickhouse/kafka/connect/sink/DedupTokenRetryLiveTest.java'`, which exits 0. The tag `v1.7.0_gg2` does not exist yet, so the gg2 figures are measured against HEAD; plan 20-07 re-runs the two tag commands once the tag exists.

```sh
git diff --shortstat v1.7.0 HEAD -- src build.gradle.kts gradle .github
git diff --shortstat v1.7.0 HEAD -- src/main
git diff --shortstat v1.6.0 pre-v1.7.0-sync -- src build.gradle.kts gradle .github
git diff --shortstat v1.7.0 v1.7.0_gg1 -- src build.gradle.kts gradle .github
git diff --shortstat v1.6.0 pre-v1.7.0-sync -- src/main
git diff --shortstat v1.7.0 v1.7.0_gg1 -- src/main
for s in src/test src/testFixtures src/main build.gradle.kts gradle .github; do git diff --shortstat v1.7.0 HEAD -- $s; done
```

```text
 119 files changed, 18802 insertions(+), 529 deletions(-)
 32 files changed, 2042 insertions(+), 143 deletions(-)
 84 files changed, 9476 insertions(+), 451 deletions(-)
 95 files changed, 13587 insertions(+), 484 deletions(-)
 30 files changed, 1368 insertions(+), 98 deletions(-)
 31 files changed, 1826 insertions(+), 130 deletions(-)
src/test:  78 files changed, 16288 insertions(+), 102 deletions(-)
src/testFixtures:  3 files changed, 68 insertions(+), 12 deletions(-)
src/main:  32 files changed, 2042 insertions(+), 143 deletions(-)
build.gradle.kts:  1 file changed, 177 insertions(+), 9 deletions(-)
gradle:  1 file changed, 6 insertions(+), 6 deletions(-)
.github:  4 files changed, 221 insertions(+), 257 deletions(-)
```

The four gg1 and before rows keep their published figures (84/9476/451, 95/13587/484, 30/1368/98, 31/1826/130); the two new rows carry 119/18802/529 and 32/2042/143.

The byte-identical and import-order counts. Method (A10): a file counts as byte-identical when `git diff --quiet v1.7.0 HEAD -- <file>` exits 0; of the others, a file counts as import-order-only when its content with every line starting `import ` and every blank line removed equals upstream's. The same method run on `v1.7.0_gg1` reproduces the published 15 and 10, so the two figures are comparable.

```sh
python3 -I -c '...'   # for each file in `git ls-tree -r --name-only v1.7.0 -- src/main`: identical / import-only / logic; also git diff --diff-filter=A for added files
```

```text
files present at v1.7.0: 63
byte-identical at HEAD: 38
differ: 25  (import-order only: 14, carry fork logic: 11)
same method at v1.7.0_gg1: import-order only 15, logic 10
moved from import-order-only to logic since gg1: Utils.java (the null-message guard, WRT-11)
files added in src/main since v1.7.0: 7 (MetadataFreeJsonWriter, DeclaredNullability, ConvertToMapOrJson, HeaderToValue, HeaderToValueConfig, HeadersToValue, TableOperationFilter)
differing src/main files in total: 25 + 7 = 32 (matches the shortstat file count)
```

The "Named skip inventory" is not changed in this plan; the suite gate (plan 20-06) re-confirms its 18 names.

Matches text: n/a (gate check)

### E-41: FORK-DELTA coverage audit

Backs: `docs/FORK-DELTA.md` "Pinned by" cells for the Phase 17 to 19 code requirements (plan 20-04). Date (UTC) 2026-10-14. For each of CI-11 to CI-16, BLD-14, DEBT-06 to DEBT-08, JSONP-09, WRT-10 to WRT-12, COLS-08 and COLS-10, the script finds the table rows whose last column (Pinned by) names the ID, and for each row of `.planning/scripts/fork-anchors.txt` whose backs-ID is one of them it checks that the ID is pinned in such a cell.

```sh
python3 -I /tmp/gsd20-cov.py   # throwaway script outside the repository; reads docs/FORK-DELTA.md and .planning/scripts/fork-anchors.txt
```

First run, before the fix: every ID was pinned except `DEBT-06` and `DEBT-08`, which were named only in the Known costs text. Both were added to the Pinned-by cells of the rows they belong to (`DEBT-08` to the metadata-free JSON row, whose `MetadataFreeJsonWriterTest` carries the NanoTimestamp pin; `DEBT-06` to the dependency-pins row, whose `Lz4RoundTripLiveTest` carries the response-leg assertion). Second run, after the fix:

```text
CI-11 1 | A single-job CI workflow
CI-12 1 | A single-job CI workflow
CI-13 1 | A single-job CI workflow
CI-14 1 | A single-job CI workflow
CI-15 1 | Dependabot also updates the `github-actions` eco
CI-16 1 | Upstream's `.github/workflows/test-head.yaml` is
BLD-14 1 | `lombok.config` sets `config.stopBubbling = true
DEBT-06 1 | Fork dependency pins in the version catalog
DEBT-07 1 | `testRuntimeClasspath` excludes `org.lz4:lz4-java`
DEBT-08 1 | The metadata-free `insertFormat=json` path
JSONP-09 1 | One shared skip, `skipEmptyCdcRecord`
WRT-10 1 | A missing value for a non-nullable column
WRT-11 2 | `doInsertRawBinary`'s second catch; `Utils.handleException`'s IOException branch
WRT-12 2 | `doWriteDates` logs an unparseable DateTime; `doWriteColValue`'s generic catch
COLS-08 1 | `Column.extractColumn`'s Enum8/Enum16 branch
COLS-10 1 | Tuple element nullability comes from the declared type
anchor rows backed by a Phase 17-19 ID: 17 rows, all covered
uncovered IDs: [] anchor rows missing: 0
```

Matches text: n/a (gate check)

### E-42: FORK-DELTA path resolution

Backs: every `.planning/` path that `docs/FORK-DELTA.md` cites (plan 20-04). Date (UTC) 2026-10-14. One ledger path and two disposition paths still pointed at `.planning/phases/13-...` and `.planning/phases/14-...`, which moved when the `v1.7.0_gg1-sync` milestone was archived (they did not resolve). They now point at `.planning/milestones/v1.7.0_gg1-sync-phases/`. The Phase 18 and 19 ledgers are named by file only, because the Phase 18 and 19 folders move when `v1.7.0_gg2-hardening` is archived.

```sh
grep -o '\.planning/[A-Za-z0-9_./-]*[A-Za-z0-9_-]' docs/FORK-DELTA.md | sort -u | while read p; do [ -n "$(git ls-files -- "$p")" ] && echo "OK   $p" || echo "DEAD $p"; done
```

```text
OK   .planning/MERGE-CHECKLIST-v1.7.0.md
OK   .planning/MERGE-CHECKLIST.md
OK   .planning/REQUIREMENTS.md
OK   .planning/milestones/v1.7.0_gg1-sync-phases/13-fork-hardening-on-the-merged-tree/13-BEHAVIOUR-LEDGER.md
OK   .planning/milestones/v1.7.0_gg1-sync-phases/13-fork-hardening-on-the-merged-tree/13-REVIEW-DISPOSITION.md
OK   .planning/milestones/v1.7.0_gg1-sync-phases/14-dependency-bumps-ci/14-REVIEW-DISPOSITION.md
OK   .planning/scripts/fork-anchors.txt
OK   .planning/scripts/verify-merge-invariants.sh
```

Before the fix the same command printed `DEAD` for exactly those three paths (the Phase 13 ledger, the Phase 13 disposition and the Phase 14 disposition). A9 held: the archived files exist under the same names.

Matches text: n/a (gate check)

### E-50: re-run of the owning plans' checks after the plan 20-05 review fixes

Backs: the changed text of `docs/ROLLOUT-v1.7.0_gg2.md` (IN-02, IN-04), `CHANGELOG.md` (WR-01, IN-02) and `docs/FORK-DELTA.md` (WR-02, IN-01, IN-03) after the review of plan 20-05. Date (UTC) 2026-10-14. The fixes are commits `54474bf5` (WR-01), `32a06bcf` (WR-02), `e6682d9a` (IN-01), `aef8d39d` (IN-03), `a15c3887` (IN-02) and `02adff03` (IN-04), all prose; no quoted `sql` or `sh` block and no test changed, so `DedupTokenRetryLiveTest` was not re-run and the evidence entries above stay as they were. The six verify commands below are extracted from the plans of the owning tasks (20-03 Task 1 to 3, 20-04 Task 1 and 2) and run verbatim from the repository root.

```sh
bash /tmp/gsd20-05/runall.sh "$PWD"
```

```text
ROLLOUT-T3-OK            (6 items, TOC matches, no bare planning ID, every quoted block in the evidence, no non-read-only SQL)
ITEM1-CONTENT-OK         (server version 26.9.18.11 cited, 2 sql blocks, gg1 note unchanged)
ITEMS2-5-CONTENT-OK      (item 2 has 5 sql blocks, item 3 has 1, E-30 present)
ROLLOUT-FINAL-OK         (item 6 commands, E-31 lines, no TRUNCATE instruction, no secret, tag only local, gg1 note unchanged)
CHANGELOG-GG2-OK         (gg2 heading first, gg1 tail equal to P20_BASE, no planning ID)
FORK-DELTA-GG2-OK        (raw numbers 119/18802/529 and 32/2042/143 equal git, 38 of 63 identical, no dead path, read-past-end wording)
```

What the changed sentences rest on: the IN-04 sentence says the earlier gg1 attempt is simulated, which is how E-03 describes the two `eo-true` cases (two real rows plus two all-defaults phantom rows inserted under the connector's token for offsets 1 to 4, then the task retries the range); the WR-02 sentence restates E-03's rows for `REPLICATED` and `NON_REPLICATED_WINDOW` (a same-token retry dropped) against `PLAIN` (every insert stored); the IN-02 sentence matches `DeclaredNullability.apply`, which logs the WARN per root column on every describe; the IN-01 ledger ranges `L-23` to `L-30` and `L-31` to `L-41` are the row ranges of the Phase 18 and Phase 19 ledgers.

Matches text: yes

### E-60: v1.7.0_gg2 suite gate

Backs: SUITE-05 (the full suite twice on ClickHouse `latest` x client V2, `testUnit` Docker-free). Date (UTC) 2026-10-14. The tree gated is `951310f0` (HEAD on `sync/v1.7.0` before this entry was committed); the last non-`.planning` commit is `02adff03` (committed 2026-10-14T16:46:17+03:00), so every XML timestamp had to be later than that. Every command ran through `env -u TESTCONTAINERS_HOST_OVERRIDE`, colima profile `vz` only, the docker CLI by absolute path `/opt/homebrew/opt/docker/bin/docker`.

Freeze (verify command 1), run before anything else:

```text
branch sync/v1.7.0 dirty False scope-ok True ['CHANGELOG.md', 'VERSION', 'docs/FORK-DELTA.md', 'docs/ROLLOUT-v1.7.0_gg2.md', 'src/test/java/com/clickhouse/kafka/connect/sink/DedupTokenRetryLiveTest.java'] anchors ['-VERSION\tv1.7.0_gg1\t1\tBLD-07', '+VERSION\tv1.7.0_gg2\t1\tBLD-07'] review-open0 True 19-review-open0 True threats0 True protected-diff 0
FREEZE-OK
```

Clean build, version, Docker stopped, `testUnit` (verify command 2):

```sh
env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew clean compileJava
grep -q 'ARTIFACT_VERSION = "v1.7.0_gg2"' build/generated/sources/version/java/main/com/clickhouse/kafka/connect/sink/Version.java
colima stop --profile vz
/opt/homebrew/opt/docker/bin/docker info        # must fail
env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew testUnit --rerun
colima start --profile vz
/opt/homebrew/opt/docker/bin/docker info        # must succeed again
```

```text
generated:  public static final String ARTIFACT_VERSION = "v1.7.0_gg2";
docker info with colima stopped (exit non-zero):  Server:
  failed to connect to the docker API at unix:///Users/alexandr.chashnikov/.colima/vz/docker.sock; check if the path is correct and if the daemon is running: dial unix /Users/alexandr.chashnikov/.colima/vz/docker.sock: connect: no such file or directory
testUnit: BUILD SUCCESSFUL in 10s
testUnit classes 45 tests 556 failures 0 errors 0 missing [] live-test-ran False stale 0
TESTUNIT-DOCKER-STOPPED-GREEN
colima list afterwards: vz  Running
```

Two full runs (verify command 3). `rm -rf build/test-results/test` came first; run 1's XML was copied to `/tmp/gsd20/exec/run1-results` before run 2 started:

```sh
env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew test --rerun    # run 1, then the XML check, then the copy
env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew test --rerun    # run 2, then the XML check against run 1's copy
```

```text
run-1-duration=158s
run-1 classes 85 tests 988 failures 0 errors 0 skipped 18 missing [] stale 0 exact True skip-set-ok True after-run-1 True server ['26.9.18.11'] matrix 30 dedup 8 min-ts 2026-10-14 13:51:23.750000+00:00 max-ts 2026-10-14 13:53:55.965000+00:00
run-2-duration=157s
run-2 classes 85 tests 988 failures 0 errors 0 skipped 18 missing [] stale 0 exact True skip-set-ok True after-run-1 True server ['26.9.18.11'] matrix 30 dedup 8 min-ts 2026-10-14 13:54:02.343000+00:00 max-ts 2026-10-14 13:56:33.012000+00:00
SUITE-TWICE-GREEN
```

Run 2's earliest XML timestamp (13:54:02) is later than run 1's latest (13:53:55), read from run 1's copied results. Server version `26.9.18.11` in both runs (`SELECT version()` logged by `DedupTokenRetryLiveTest`); the local `clickhouse/clickhouse-server:latest` image was created 2026-10-13 21:01:43 +0300. Phase 19's gate recorded `26.9.17.8`, so `latest` moved by one patch release between the two gates; both are green and nothing was edited. `NullableTupleMatrixLiveTest` ran exactly 30 cases and `DedupTokenRetryLiveTest` 8, in both runs.

The 18 skips, compared as a set of `class#method` names with the inventory in `docs/FORK-DELTA.md` (identical in both runs; the 11 parameterised cases are `ClickHouseSinkTaskWithSchemaTest#[1]` to `[11]`, the `exactlyOnceStateMismatchTest` cases skipped by the standalone assumption):

```text
ClickHouseSinkTaskTest#clientNameTest()
ClickHouseSinkTaskWithSchemaTest#schemaWithNestedTupleMapArrayAndVariant()
ClickHouseSinkTaskWithSchemaTest#schemaWithTupleOfMapsWithVariantTest()
ClickHouseSinkTaskWithSchemaTest#[1] 11, 7 ... #[11] 229, 221   (11 cases)
ConvertToMapOrJsonPerformanceTestJson#testDeepNestedStructPerformance()
ConvertToMapOrJsonPerformanceTestJson#testWideStructPerformance()
InsertFormatPerformanceTest#compareInsertPerformance()
JsonColumnTest#compareAllConfigurations()
```

Anchors invariant on the branch tip (verify command 4, first half):

```sh
bash .planning/scripts/verify-merge-invariants.sh 54e63648 HEAD
```

```text
invariants: provenance_paths=21 anchor_commits=344 anchor_rows=59 marker_commits=391 result=PASS
```

Matches text: n/a (gate check)

### E-61: pre-push checks and dry-run merge

Backs: SUITE-05 and PUB-08 (the push is safe to make; the release merge shape is rehearsed). Date (UTC) 2026-10-14. Both commands ran on `951310f0`, before this entry was committed; the evidence commit touches only `.planning/`, so the tree outside `.planning/` is the same. Verify commands 4 and 5 were re-run on the committed tip after the commit and their output is in the rehearsal bundle.

PUSH-READY (verify command 4, second half: HEAD on `sync/v1.7.0`, `.gsd-ws-arg` untracked, no `push.followTags`, no `remote.origin.push` or `mirror`, `git fetch --no-tags`, origin `custom_build` at `be6b8861`, origin has no `sync/v1.7.0` and no ref at X, `8497dc1d` an ancestor, no secret pattern in the changed files, API rate limit at least 20):

```text
X=951310f070400f871eb6a796b6fd01df87605859 rate=58
PUSH-READY
```

MERGE-DRY-RUN-OK (verify command 5: a `--no-ff` merge of X into a detached worktree at `be6b8861`, then the worktree removed and pruned):

```text
D=514c13f692c19e6eebb4fe29f6cfb6b9322a86ae X=951310f070400f871eb6a796b6fd01df87605859
MERGE-DRY-RUN-OK
```

The dry-run merge `D` has first parent `be6b8861c25f3d7611194e7150b18362c5c75e32`, second parent X, and `git diff --quiet D X` is empty, so the merged tree equals the tested tip. `git worktree list` afterwards shows only the main checkout.

Matches text: n/a (gate check)

### E-62: rehearsal push and run

Backs: PUB-08 (the release tree ran green on CI under its own SHA before any merge depends on it). Date (UTC) 2026-10-14.

**The user's answer, verbatim:** `push` — given directly to the orchestrator in the main session at the Task 2 `blocking-human` checkpoint, after the rehearsal bundle (items 1-8) was presented verbatim. Received by: the orchestrator, directly. Task 3 ran inline in the orchestrator; no subagent held push authority.

**Pre-push re-checks on the tip (Task 3 step 1):** HEAD equalled the bundle's X exactly (no commit landed after it). PUSH-READY re-passed (`X=d316bc454f99615e2d9c49a7e9632ec94eed80e5 rate=51`; anchors `result=PASS` in `/tmp/gsd20/exec/anchors-20-06-t3.log`). MERGE-DRY-RUN-OK re-passed (`D=d99c01a04c30f1ac1d21fcbc5c67b2824bc6ae1e`, parents `be6b8861` and X, tree equal, worktree removed). Ref snapshots saved to `origin-heads-before-rehearsal.txt` and `origin-tags-before-rehearsal.txt`.

**The push, as run:**

```sh
git push origin refs/heads/sync/v1.7.0:refs/heads/sync/v1.7.0
```

```text
 * [new branch]        sync/v1.7.0 -> sync/v1.7.0
```

No force flag, no tag, no other refspec.

X0=d316bc454f99615e2d9c49a7e9632ec94eed80e5

**The run** (`actions/runs?head_sha=X0`: exactly one run, zero cancelled):

```text
id=38578966373 path=.github/workflows/tests.yaml event=push head_branch=sync/v1.7.0 status=completed conclusion=success
run_started_at=2026-10-14T14:15:57Z updated_at=2026-10-14T14:23:10Z (about 7 min)
url=https://github.com/ne1r0n/clickhouse-kafka-connect/actions/runs/38578966373
job="ClickHouse latest Client version V2 tests" runner_name="GitHub Actions 1000000188" labels=ubuntu-24.04 completed/success
```

**Origin before the push** (heads; tags in `origin-tags-before-rehearsal.txt`, 4 tags):

```text
be6b8861c25f3d7611194e7150b18362c5c75e32  refs/heads/custom_build
bec167d85918e77c11621063000acc6e031ae2fa  refs/heads/main
```

**Origin after the push** (tags byte-identical, `origin-tags-after-rehearsal.txt`):

```text
be6b8861c25f3d7611194e7150b18362c5c75e32  refs/heads/custom_build
bec167d85918e77c11621063000acc6e031ae2fa  refs/heads/main
d316bc454f99615e2d9c49a7e9632ec94eed80e5  refs/heads/sync/v1.7.0
```

Matches text: n/a (gate check)
