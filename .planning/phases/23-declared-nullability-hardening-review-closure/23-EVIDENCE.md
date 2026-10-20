# Phase 23 Evidence

- Phase: 23 (Declared-Nullability Hardening & Review Closure)
- Branch: `sync/v1.7.0`
- Purpose: measured proof for COLS-12..COLS-14 and DEBT-10..DEBT-12, read by the verifier together with 23-BEHAVIOUR-LEDGER.md. The file holds commands, raw DESCRIBE rows, counts and message formats only, never a record value from a real system (every value below is synthetic). Nothing here was pushed.
- Entry format: `### E-NN: {title}` per execution entry, `key: value` lines for measured facts. Long output stays in `/tmp/gsd23/` (research) and `/tmp/gsd23/exec/` (execution).

## Index

| ID | Source | What it records | Status |
|----|--------|-----------------|--------|
| R-01 | 23-RESEARCH | D-03 live capture: Tuple element named `size` / `size0` / `null` | recorded |
| R-02 | 23-RESEARCH | D-11 live capture: backslash-n inside a quoted element name and an enum label | recorded |
| R-03 | 23-RESEARCH | D-07 null-shape matrix on the unchanged code | recorded |
| R-04 | 23-RESEARCH | Prototype run: which existing tests D-01 turns red | recorded |
| E-01 | 23-01 | DEBT-11 baselines and commits | recorded |
| E-02 | 23-02 | COLS-12 | recorded |
| E-03 | 23-03 | DEBT-10 fallback-WARN mutations | recorded |
| E-04 | 23-04 | COLS-13 | recorded |
| E-05 | 23-05 | COLS-14, the unpaired-WARN mutations and the SC3 reading | recorded |
| E-06 | 23-06 | DEBT-12 closure pointers | recorded |
| E-07 | 23-06 | phase gate and SC1..SC5 readings | recorded |

## Research captures (written during 23-RESEARCH, not by an executor)

All captures ran on ClickHouse `latest` (server `26.9.19.9`, image `sha256:bdb5c911a23d`, created 2026-10-14T18:05:21Z) with client V2 (client-v2 0.9.6), against a throwaway container `gsd23-ch` (`docker run`, port 18123, removed afterwards). The harness classes were compiled outside the repo (`/tmp/gsd23/src*`) against `build/classes/java/main` plus the test runtime classpath. `git status` of the repo was clean before and after.

### R-01: D-03, Tuple elements named `size`, `size0`, `null`

Verdict: **CONFIRMED for `size` and `size0`; REFUTED for `null`.**

`null`: the server refuses the DDL for every shape tried (plain Tuple, `Nullable(Tuple)`):
`Code: 36. DB::Exception: Tuple element name 'null' is reserved because it would conflict with the subcolumn name used for Nullable null maps if such a tuple is wrapped in Nullable. Please use a different name. (BAD_ARGUMENTS)`.
So a Tuple element cannot be named `null` on `latest`.

`size` / `size0`: DDL `c Tuple(size UInt32, name String)`, `d Nullable(Tuple(size0 Int32, other String))`, `e Array(Tuple(size UInt8, v String))`. Raw `DESCRIBE TABLE ... FORMAT JSONEachRow` with `describe_include_subcolumns=1` (only the fields that matter; the other keys are empty strings and `is_subcolumn`):

```
{"name":"c","type":"Tuple(\n    size UInt32,\n    name String)", ... "is_subcolumn":0}
{"name":"d","type":"Nullable(Tuple(size0 Int32, other String))", ... "is_subcolumn":0}
{"name":"e","type":"Array(Tuple(\n    size UInt8,\n    v String))", ... "is_subcolumn":0}
{"name":"c.size","type":"UInt32", ... "is_subcolumn":1}
{"name":"c.name","type":"String", ... "is_subcolumn":1}
{"name":"c.name.size","type":"UInt64", ... "is_subcolumn":1}
{"name":"d.null","type":"UInt8", ... "is_subcolumn":1}
{"name":"d.size0","type":"Nullable(Int32)", ... "is_subcolumn":1}
{"name":"d.other","type":"Nullable(String)", ... "is_subcolumn":1}
{"name":"d.other.size","type":"Nullable(UInt64)", ... "is_subcolumn":1}
{"name":"e.size0","type":"UInt64", ... "is_subcolumn":1}
{"name":"e.size","type":"Array(UInt8)", ... "is_subcolumn":1}
{"name":"e.v","type":"Array(String)", ... "is_subcolumn":1}
{"name":"e.v.size","type":"Array(UInt64)", ... "is_subcolumn":1}
```

Full file: `/tmp/gsd23/desc/t_size.jsonl`. `c.size`, `d.size0` and `e.size` are the real elements; `c.name.size`, `d.other.size`, `e.size0` and `e.v.size` are structural rows that the skip is meant to drop.

Column tree discovery builds (the real `shouldSkipDescribeRow` applied row by row, then `buildDescribedTable`, harness `Cap6`): rows skipped `c.size`, `c.name.size`, `d.null`, `d.size0`, `d.other.size`, `e.size0`, `e.size`, `e.v.size`. Tree:

```
c TUPLE nullable=false
  c.name STRING                       <- c.size is missing
d TUPLE nullable=true
  d.other STRING                      <- d.size0 is missing
e ARRAY
  [] e TUPLE nullable=false
    e.v STRING                        <- e.size is missing
```

Insert outcome through the real sink task (`ClickHouseSinkTask.start/put`, client V2, one record `{id:1, c:{size:7, name:"nm"}}`), read back with typed SQL:

| Table | Insert | Read-back |
|-------|--------|-----------|
| `c Tuple(size UInt32, name String)` | `put` throws; cause chain ends `Code: 33 ... Cannot read all data. Bytes read: 3. Bytes expected: 4 ... (CANNOT_READ_ALL_DATA)` | `count()=0` |
| `c Nullable(Tuple(size0 Int32, name String))` | same code 33 | `count()=0` |
| `c Array(Tuple(size Int32, name String))` | same code 33 | `count()=0` |
| `c Tuple(size0 Int32, name String)` | same code 33 | `count()=0` |
| `c Tuple(size UInt32, name String)` with a constructed value `name = "abc" + U+0003 + "xyz"` | `put` returns normally | `c = (1667391751,'xyz')`, `hex(c.name) = 78797A`: **silently wrong stored value** |

Reading, stated plainly: for ordinary data the outcome is a **server error, code 33 `CANNOT_READ_ALL_DATA`, and no row is stored**. A silently wrong stored value is also **reachable** (the last row), when the bytes of the shorter write happen to parse as a longer tuple. Severity for 999.4: HIGH for the table (every insert into it fails), with a data-integrity tail for crafted or unlucky values.

### R-02: D-11, backslash-n inside a quoted name and an enum label

DDL (SQL text, so `\\` is one literal backslash and `\n` is a real newline): `` c Tuple(`a\\nb` Int32, d String) `` (`bs_name`), `` c Tuple(e Enum8('x\\ny' = 1, 'z' = 2), d String) `` (`bs_enum`), `` c Tuple(`a\nb` Int32, d String) `` (`nl_name`, a real newline in the name), `` c Tuple(e Enum8('x\ny' = 1, 'z' = 2), d String) `` (`nl_enum`), plus nested, Nullable and trailing-backslash variants.

Raw JSON `type` text of the root row, in the JSON escaping the server sends (this is what `fromJsonRow` receives and what the `row(name, type)` test helper embeds with `%s`). `fromJsonRow` doubles every backslash before Jackson parses, so the Java `getType()` string is **the same text, with its backslashes kept**:

| Case | Raw JSON type text (`\` shown as is) | Backslashes before the `n` |
|------|--------------------------------------|----------------------------|
| server line break between elements | `Tuple(\n    a Int32,\n    b String)` | 1 (outside any quote) |
| literal backslash-n in a backtick name | `Tuple(\n    `a\\\\nb` Int32,\n    d String)` | 4 (inside backticks) |
| real newline in a backtick name | `Tuple(\n    `a\\nb` Int32,\n    d String)` | 2 (inside backticks) |
| literal backslash-n in an enum label | `Tuple(\n    e Enum8('x\\\\ny' = 1, 'z' = 2),\n    d String)` | 4 (inside single quotes) |
| real newline in an enum label | `Tuple(\n    e Enum8('x\\ny' = 1, 'z' = 2),\n    d String)` | 2 (inside single quotes) |

Sub-column rows: `{"name":"c.a\\nb","type":"Int32"...}` for the literal case (2 backslashes in the JSON text and in the Java `getName()`), `{"name":"c.a\nb"...}` for the real-newline case (JSON escape of a newline). The ClickHouse client parser (`ClickHouseColumn.of`) reads a backtick name with one level of unescape (4 backslashes become 2), so the parsed element name equals the sub-column name only when the text is left untouched. All raw rows are in `/tmp/gsd23/desc/*.jsonl`.

Effect of the current `normalise` (`replace("\\n", " ")`), harness `Cap2`:

| Case | `normalise` result | Parsed element name vs sub-column name | `apply` |
|------|--------------------|-----------------------------------------|---------|
| literal backslash-n, name | 3 backslashes + space (`a\\\ b`) | `a\ b` vs `a\\nb` | false, fallback WARN `(structure mismatch)` |
| real newline, name | 1 backslash + space | `a b` vs `a\nb` | false, fallback WARN |
| literal / real, enum label | label text changed, never compared | element `e` pairs | true, no WARN |
| literal backslash-n, name inside `Nullable(Tuple(...))` | corrupted | mismatch | false, fallback WARN |
| two nested levels of such names | corrupted | mismatch | false, fallback WARN |

So the enum-label half of IN-03 **does not change the pairing outcome today** (no WARN either way); only the `normalise` output string differs. A real newline inside a name is broken today as well, which 19-REVIEW IN-03 did not mention.

D-13 verdict: **a quote-aware scan CAN tell the server's line break from a backslash-n inside quotes.** The line break is a lone backslash before `n` and occurs only outside quotes (after `(` or `,`). Inside quotes the server's escapes are doubled by the JSON layer, so every run of backslashes there has even length (2 for a real newline, 4 for a literal backslash-n). No checkpoint is needed unless the plan finds a counter-example. The prototype scanner (source `/tmp/gsd23/src2/com/clickhouse/kafka/connect/sink/db/mapping/Cap2.java`, method `quoteAware`; production-shaped version in `/tmp/gsd23/proto/src/`) normalised every row above to the expected text and the parsed names then equalled the sub-column names (`a\\nb`, `a\nb`, `e\\`, `f\\n`).

Live end-to-end on `` c Nullable(Tuple(`a\\nb` Int32, d String)) `` (the natural D-12 table; Kafka Struct field names must be `a\\nb` with **two** backslashes, the Column name, because the writer looks fields up by `getName()` after the last dot):

| Code | fallback WARN | `put` | read-back |
|------|---------------|-------|-----------|
| unchanged `main` | 1, `(structure mismatch)` | throws, `CANNOT_READ_ALL_DATA` (code 33) | `count()=0` |
| prototype (D-10 quote-aware) | 0 | ok | `c = (7,'nm')` |

### R-03: D-07, which null shapes throw today (unchanged code, harness `Cap4`)

| Input | `containsTuple` | `apply` |
|-------|-----------------|---------|
| null root | false | **throws NullPointerException** (the handler dereferences the root) |
| root Column with null type | **throws NPE** | false, WARN `(NullPointerException)` |
| root with null name, tuple type | true | false, WARN `(IllegalArgumentException)` (from `ClickHouseColumn.of`, not an NPE) |
| tuple with a null entry in `tupleFields` | true | false, WARN `(NullPointerException)` |
| tuple field with null name | true | false, WARN `(NullPointerException)` |
| tuple field with null type | true | false, WARN `(NullPointerException)` |
| `tupleFields` null | true | true, no WARN |
| array root, element Column has null type | **throws NPE** | false, WARN `(NullPointerException)` |
| array root, `arrayType` null | false | false, WARN `(structure mismatch)` |
| map root, value Column null | false | true, no WARN |
| map root, value Column has null type | **throws NPE** | false, WARN `(NullPointerException)` |
| declared type null | true | false, no WARN |
| declared type `Tuple(` | true | false, WARN `(IllegalArgumentException)` |

`ClickHouseColumn.of("c", s)` throws `java.lang.IllegalArgumentException` for `Nullable(Tuple(SENTINEL_ELEMENT_NAME NoSuchType))`, `Tuple(`, the empty string, `Foo(`, `Array(`, `Map(String)`, `Tuple()`; it throws `NumberFormatException` for `Enum8('a' = x)`.

### R-04: D-01 prototype against the existing Docker-free tests

Prototype = the current `DeclaredNullability.java` plus D-01 (unpaired WARN, still apply), D-06/D-07 (null totality) and D-10 (quote-aware `normalise`, public `ESCAPED_NEWLINE`), compiled outside the repo and put in front of the classpath. Test runner = JUnit Platform launcher.

```
DeclaredNullabilityTest + ClickHouseHelperClientForkUnitTest   baseline main: 70 tests, 70 successful
same two classes with the prototype:                          70 tests, 66 successful, 4 failed
  1. DeclaredNullabilityTest.unexpectedExceptionInThePassFailsOpenWithOneMetadataWarning   (the D-08 flip target, expected)
  2. ClickHouseHelperClientForkUnitTest.otherTypesDiscoverWithoutWarningAndUnchangedRootNullability  [Tuple(a Nullable(Int32))]
  3. ClickHouseHelperClientForkUnitTest.otherTypesDiscoverWithoutWarningAndUnchangedRootNullability  [Tuple(a Nullable(Enum8('q' = 1)))]
  4. ClickHouseHelperClientForkUnitTest.repeatedDiscoveryNeverWarns
live classes with the prototype (NullableTupleMatrixLiveTest, SimpleAggregateTupleRoundTripLiveTest,
  ClickHouseHelperClientForkLiveTest, NullableEnumRoundTripLiveTest, NestedNullableCompositeWriteTest): 68 tests, 68 successful
```

Failures 2 to 4 are NOT in the plan's list of expected flips: their fixtures are a Tuple **root row alone**, so every declared element is unpaired and the new WARN fires. See 23-RESEARCH.md, Open Question 1.

Shape check with the prototype on the R-01 rows: three events, one per root, e.g. `Declared Tuple element(s) [c.size] of column c of table db.t (declared type Tuple(\n    size UInt32,\n    name String)) have no matching column; applying the declared nullability to the elements that paired` (wording is the prototype's, not binding).

### E-01: DEBT-11 shared literal and test cleanups (23-01)

Recorded 2026-10-20T11:41:16Z. Scratch output is in `/tmp/gsd23/exec/`.

base_rev: 16e0b2d68f90cb1ecc6471c5fbca5240ffd3f05f
spotless_base_files: 46
spotless_base_md5: f2e32c7bc069ecd155830f82d26285d6
spotless_after_23_01: SPOTLESS-BASELINE-EQUAL
anchors_mirror_23_01: rows=65 bad=0
testunit_23_01: Tests summary: 795 tests, 793 succeeded, 0 failed, 2 skipped
shared_literal_definitions: 1 (before 4)
old_test_constant_lines: 0 (before 4)
pass_through_refs: 0 (before 2)
d14_commit: 4478016aecc7188ee334692374b969c82881ee3d
d15_commit: 252cf8f330d7caa258b9e9f85be913269ba97113
live_d14: SimpleAggregateTupleRoundTripLiveTest tests=2 failures=0; declaredTupleTypesParseWithDdlNullability tests=1 failures=0
live_d15: declaredTupleTypesParseWithDdlNullability tests=1 failures=0
clickhouse_image: sha256:bdb5c911a23d92fd364e246ab20b5a0a485f00112f3663fa626b083fb46c146b 2026-10-14T18:05:21.432177888Z
fork_unit_testcases: 45
assumption_delta: no-change (the detector fired on the prose words "also" and "fallback"; no identity model generalises)

### E-02: COLS-12 null totality and the D-08 flip (23-02)

Recorded 2026-10-20T11:46:40Z. Scratch output is in `/tmp/gsd23/exec/`.

cols12_tracer_red: rc=1 failed=3
cols12_expansion_red: rc=1 d07_failed=8 kept_and_d09_passed=3
cols12_green: tests=38 failures=0
d08_flip: method and null-type input kept; NullPointerException class-name assertion -> (structure mismatch); 19-REVIEW IN-02 re-open trigger
null_root_name_reading: a null root name is now (structure mismatch), not (IllegalArgumentException) (RESEARCH Open Question 2)
cols12_commit: 4bbc9627fe7a8a0545dc8f4bb9cf3c3b9109509c
cols12_commit_files: 6
anchors_mirror_23_02: rows=66 bad=0
spotless_after_23_02: SPOTLESS-BASELINE-EQUAL
testunit_23_02: Tests summary: 808 tests, 806 succeeded, 0 failed, 2 skipped

### E-03: DEBT-10 fallback-WARN mutations (23-03)

Recorded 2026-10-20T11:51:00Z. Scratch output is in `/tmp/gsd23/exec/`.

d17_expected: 14 or more (found 14: the three converted names and the COLS-12 (D-07) structure mismatch / (D-08 flip) / (D-09) testcases)
d17_tracer: rc=1 under both mutations; `COLS-10: a declared type that does not parse fails open with one metadata-only WARN` failed with `WR-02 column segment: ` under m1 and m2, green after the revert (38 testcases)
d17_fallback_m1: rc=1 marked=14 missing=none
d17_fallback_m2: rc=1 marked=14 missing=none
d17_reverted: yes (git diff --quiet -- src/main after each run)
d17_m2_reasoning: with the placeholder dropped SLF4J writes the sentinel into the table slot (of table nullability_root.db), so a bare contains(sentinel) passes; only the delimited column <name> of table segment fails
assert_lines_unchanged: unparsable=8 structure=7 fork=3
debt10_commit: 075e6466aab3413ececb1301535f80ed00ec4e83
spotless_after_23_03: SPOTLESS-BASELINE-EQUAL
anchors_mirror_23_03: rows=66 bad=0
testunit_23_03: Tests summary: 809 tests, 807 succeeded, 0 failed, 2 skipped

Mutation 1 deleted `columnName, ` after `FALLBACK_WARNING, ` in the three LOGGER.warn calls (before: 3 lines, after: 0; `FALLBACK_WARNING, database, ` 3 lines). Mutation 2 replaced `for column {} of table` with `for column  of table` in the constant (1 line). DeclaredNullability.java was restored with `git checkout --` after each run. DeclaredNullabilityTest ends at 39 testcases and ClickHouseHelperClientForkUnitTest at 45, both with 0 failures. Other testcases that check `db.t` without a column assertion also went red under both mutations; they carry no `WR-02 column segment: ` text and are not in the D-17 list.

| Testcase | m1 | m2 |
|----------|----|----|
| COLS-10: a declared type that does not parse fails open with one metadata-only WARN | red | red |
| COLS-12 (D-09): a declared type ClickHouseColumn.of rejects falls back with one WARN ending in the thrown class name | red | red |
| COLS-12 (D-07) structure mismatch: a null tupleFields entry | red | red |
| COLS-12 (D-07) structure mismatch: a field with a null name | red | red |
| COLS-12 (D-07) structure mismatch: an Array element with a null type | red | red |
| COLS-10: a Tuple Column whose declared type is not a Tuple fails open and changes nothing | red | red |
| COLS-12 (D-07) structure mismatch (kept): an Array root with no element Column | red | red |
| COLS-12 (D-07) structure mismatch: an element Tuple with a null name never pairs a field literally named null.a | red | red |
| COLS-12 (D-07) structure mismatch: a field with a null type | red | red |
| COLS-12 (D-07) structure mismatch: a root with a null name | red | red |
| COLS-12 (D-08 flip): a Column with a null type falls back as a structure mismatch with one metadata-only WARN naming the table | red | red |
| COLS-12 (D-07) structure mismatch: a Map value with a null type | red | red |
| COLS-12 (D-07) structure mismatch: a second call on the same null-type root warns again and leaves it untouched | red | red |
| COLS-10 IN-01: the fallback WARN raised during discovery names the database and the table | red | red |

### E-04: COLS-13 quote-aware normalise and the live premise (23-04)

Recorded 2026-10-20. Scratch output is in `/tmp/gsd23/exec/` (`cols13-*`, `live-six-23-04*`, `testunit-23-04.log`).

cols13_tracer_red: unit rc=1 failed=1; live rc=1 failed=2 (fallback WARN)
cols13_expansion_red: unit rc=1 disc_failed=7 unchanged_passed=6; live drift rc=1 failed=1
cols13_green: unit tests=52; live round trip tests=2; live drift tests=1
live_six_23_04: NullableTupleMatrixLiveTest=33/0, SimpleAggregateTupleRoundTripLiveTest=2/0, NullableEnumRoundTripLiveTest=6/0, NestedNullRowBinaryLiveTest=10/0, NestedNullableCompositeWriteTest=18/0, ClickHouseHelperClientForkLiveTest=12/0 (tests/failures, errors 0 for all six)
d13_verdict: not triggered; the server line break is a lone backslash-n outside quotes and every backslash run inside quotes is even (R-02); the live premise matched every captured literal
clickhouse_server: 26.9.19.9 (clickhouse/clickhouse-server:latest, image bdb5c911a23d created 2026-10-14)
cols13_commit: f55165ee8397d0574a81abec348761fef8fb7e67
cols13_commit_files: 7
anchors_mirror_23_04: rows=68 bad=0
spotless_after_23_04: SPOTLESS-BASELINE-EQUAL
testunit_23_04: Tests summary: 822 tests, 820 succeeded, 0 failed, 2 skipped

Tracer red (unchanged production file): the new unit pin failed on the `normalise` assertion (the old replace turned the backslash-n inside the name into a space, `\\ b` instead of `\\nb`), and both twin testcases failed on the fallback WARN assertion, not on the raw-type premise. Expansion red by file swap: the seven `COLS-13 (D-10)` / `COLS-13 (D-10) edges` testcases failed, the six `COLS-13 (D-10) unchanged` cases and every older testcase passed, and the live drift test failed on six fallback WARNs (columns p, r, n, s and the Array and Map wrappers; the enum column q already paired) with the raw-type premise passing. The restored production file was byte-identical to the saved copy (`cmp`).

### E-05: COLS-14 unpaired-element WARN, D-19 flips and the SC3 reading (23-05)

Recorded 2026-10-20. Scratch output is in `/tmp/gsd23/exec/` (`cols14-*`, `d19-forced-red*`, `d17-unpaired-*`, `live-six-23-05*`, `testunit-23-05.log`).

cols14_tracer_red: rc=1 failed=1
d19_forced_red: rc=1 failed=3 (the three D-19 names)
cols14_expansion_red: rc=1 unpaired_failed=16 others_passed=yes
cols14_green: DeclaredNullabilityTest tests=62; ClickHouseHelperClientForkUnitTest tests=51
d17_unpaired_m1: rc=1 marked=16 missing=none fallback_unaffected=yes
d17_unpaired_m2: rc=1 marked=16 missing=none fallback_unaffected=yes
d17_reverted: yes (cmp against the saved copy)
sc3_reading: ROADMAP SC3 (amended 2026-10-20 per 23-CONTEXT D-01) names log and still apply, the form shipped here; it replaced the original falls back / keeps today's behaviour wording, which RESEARCH Open Question 3 had flagged as a third form; falling back cannot repair the missing element's bytes and would bring back the double flag; missingDeclaredElementDoesNotShiftPairing is byte-identical and green
missing_declared_unchanged: yes (block md5 equals 16e0b2d6)
d03_verdict: confirmed for size and size0, refuted for null (code 36); backlog 999.4 created
backlog_9995: created (D-20)
live_six_23_05: NullableTupleMatrixLiveTest=33/0, SimpleAggregateTupleRoundTripLiveTest=2/0, NullableEnumRoundTripLiveTest=6/0, NestedNullRowBinaryLiveTest=10/0, NestedNullableCompositeWriteTest=18/0, ClickHouseHelperClientForkLiveTest=12/0 (tests/failures, errors 0 for all six)
cols14_commit: f83ac147b5aa733c0a7b326d603a416b5cbba38c
cols14_commit_files: 7
anchors_mirror_23_05: rows=69 bad=0
spotless_after_23_05: SPOTLESS-BASELINE-EQUAL
testunit_23_05: Tests summary: 838 tests, 836 succeeded, 0 failed, 2 skipped

Tracer red (unchanged production file): only the sibling `COLS-14 unpaired-element WARN (D-01, D-02)` failed, on the exactly-one-WARN assertion, because the unchanged pass returned true and logged nothing; the other 52 testcases passed. D-19 forced red (new pass, unflipped fork unit test): exactly the three collided testcases failed, `Tuple(a Nullable(Int32))` and `Tuple(a Nullable(Enum8('q' = 1)))` of `otherTypesDiscoverWithoutWarningAndUnchangedRootNullability` and `repeatedDiscoveryNeverWarns`, each on the no-WARN assertion; the other 95 of 98 passed. After the flips the fork XML still holds 15 testcases named `COLS-10 (D-11 flip)`. Expansion red by file swap (HEAD's production file, the two premise assertions of the `DEBT-10:` test removed for that run only): the 16 testcases starting `COLS-14 unpaired-element WARN` failed (10 in DeclaredNullabilityTest, 6 in ClickHouseHelperClientForkUnitTest, 3 of them the D-04 known-defect 999.4 cases), and the other 97 of 113 passed, the D-19 flipped ones included. The production file and the test file were restored byte-identically (`cmp`).

D-17 for the new WARN: mutation 1 deleted `columnName, ` from the `LOGGER.warn` call (marker count 0) and mutation 2 turned `of column {}` into `of column ` in the constant (marker count 1). Under both, all 16 `COLS-14 unpaired-element WARN` testcases failed with the `WR-02 column segment: ` text, and the 14 fallback-WARN column testcases (the three COLS-10 fallback pins, the COLS-12 structure-mismatch pins, the D-08 flip and the D-09 pins) stayed green, which shows the mutations are local.

D-03 / D-04: the Docker-free pin runs the R-01 rows through the real `shouldSkipDescribeRow` and `buildDescribedTable` in the `describeTableV2` loop order. For `size`, `size0` and `Array(Tuple(size ..))` the Tuple Column lacks the element, one WARN names it, a direct `apply` returns true and every paired element carries its declared flag (the `size0` case keeps `other` non-nullable although DESCRIBE reported `Nullable(String)`). `ClickHouseHelperClient.java` is unchanged since 16e0b2d6 (`git diff --quiet` on `sink/db/helper` in src/main).

### E-06: DEBT-12 closure pointers (23-06)

Recorded 2026-10-20. The Task 1 tracer (19-REVIEW WR-01) was verified with its own check and committed together with the rest in the single DEBT-12 commit, because the plan's commit check requires that commit to hold exactly the two disposition files. The DEBT-12 checker printed `errors: []`.

| Review | Finding | Disposition | Pointer | Evidence |
|--------|---------|-------------|---------|----------|
| 17 | WR-01 | fixed | fee898c6 and 5d616806 (Phase 21) | 21-EVIDENCE.md E-02, E-11; 21-UAT.md test 1 (5010093f) |
| 17 | IN-01 | fixed | fee898c6 (Phase 21, CI-18) | 21-EVIDENCE.md E-10, E-11 |
| 17 | IN-02 | fixed | fee898c6 (Phase 21, CI-19) | 21-EVIDENCE.md E-10, E-11 |
| 17 | IN-03 | skipped | roadmap reason: leave Lz4RoundTripLiveTest's jar-origin check, cross-referenced to TestRuntimeClasspathLz4UnitTest | Lz4RoundTripLiveTest unchanged since 16e0b2d6 |
| 19 | WR-01 | fixed | 4bbc9627 (fix(23-02), COLS-12) | E-02 |
| 19 | WR-02 | fixed | 075e6466 (test(23-03), DEBT-10) | E-03, E-05 |
| 19 | WR-03 | unchanged | 3e39867d (already fixed) | byte-identical to 16e0b2d6 |
| 19 | IN-01 | fixed | 4478016a (refactor(23-01) D-14, DEBT-11) | E-01 |
| 19 | IN-02 | fixed | 252cf8f3 (refactor(23-01) D-15) and 4bbc9627 (fix(23-02) D-08) | E-01, E-02 |
| 19 | IN-03 | fixed | f55165ee (fix(23-04), COLS-13) | E-04 |
| 19 | CR-01 | unchanged | 4e133d4e (already fixed) | byte-identical to 16e0b2d6 |
| 19 | IN-04 | fixed | f83ac147 (fix(23-05), COLS-14, D-01; backlog 999.4) | E-05 |

debt12_commit: fb71551e8e30b2e3306206106177dbeef939b2c0
open_17: 0 (before 4)
open_19: 0 (before 0)
findings_closed: 10
findings_unchanged: 2 (19 WR-03, CR-01)
close_record_edited: no
lz4_live_test_changed: no

### E-07: Phase 23 close gate and SC readings (23-06)

Recorded 2026-10-20 on ClickHouse `latest` with client V2 (CLICKHOUSE_VERSION and CLIENT_VERSION unset), on HEAD fb71551e (the DEBT-12 commit). Scratch output is in `/tmp/gsd23/exec/` (`testunit-final.log`, `full-suite.log`, `full-suite.rc`, `gate-final.{out,err,rc}`, `scope-final.txt`). Every run is listed: `testUnit --rerun` once, `test --rerun` once; no OpenJ9 exit 255 occurred and no re-run was needed.

testunit_rerun: Tests summary: 838 tests, 836 succeeded, 0 failed, 2 skipped
full_suite: Tests summary: 1277 tests, 1259 succeeded, 0 failed, 18 skipped
full_suite_runs: 1 (exit 0, no exit-255 crash, no javacore or core dump produced)
full_suite_skips: 18
gate_final: invariants: provenance_paths=21 anchor_commits=468 anchor_rows=69 marker_commits=515 anchor_exempt=1 result=PASS
spotless_final: SPOTLESS-BASELINE-EQUAL
scope_ok: yes
order_ok: yes
no_push: yes
worktrees: 1
clickhouse_server: 26.9.19.9 (clickhouse/clickhouse-server:latest, image bdb5c911a23d created 2026-10-14; logged by NullableTupleMatrixLiveTest)
sc1: pass (E-02: COLS-12 pins red before and green after; the D-08 flip keeps its method and input with a written rationale)
sc2: pass (E-04: Docker-free pins on the live capture R-02; live premise on both twins and the drift oracle; no fallback WARN; D-13 not triggered)
sc3: pass (SC3 as amended 2026-10-20 per 23-CONTEXT D-01: log one metadata-only WARN and still apply; E-05 sc3_reading records that the shipped behaviour matches it; missingDeclaredElementDoesNotShiftPairing byte-identical and green)
sc4: pass (E-03 and E-05: sentinel root, delimited segment, both D-17 mutations red for every column assertion; E-01: one backslash-n literal, pass-through and mid-class constant cleaned up)
sc5: pass (E-06: every 17-REVIEW and 19-REVIEW finding fixed or closed with a written reason and a pointer; 17 IN-03 skipped with the roadmap's reason; Lz4RoundTripLiveTest unchanged)

Gate detail: all 89 JUnit XML files under `build/test-results/test` show 0 failures and 0 errors, and DeclaredNullabilityTest (62), ClickHouseHelperClientForkUnitTest (51), NullableTupleMatrixLiveTest (33) and SimpleAggregateTupleRoundTripLiveTest (2) hold their expected testcase counts. The anchors invariant exited 0 with an empty stderr. The testUnit total (838/836/2 skipped) equals the figure E-05 recorded after 23-05, so the 23-06 plan, which changes no source, left it unchanged.
