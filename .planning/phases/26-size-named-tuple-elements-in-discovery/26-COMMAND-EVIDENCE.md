# Phase 26: Command evidence

- Phase: 26 (Size-Named Tuple Elements in Discovery)
- Branch: `sync/v1.7.0`
- Purpose: measured proof for the Phase 26 plans. Nothing here was pushed.
- Shell: macOS zsh; every Gradle call is `env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew ...`; Docker is colima profile `vz`
- Entry format: one `### E-NN: {title}` section per entry, with the command as run, the relevant output in a `text` fence, a UTC timestamp, and a final line `Verdict: pass`, `Verdict: fail`, `Verdict: pending ({reason})` or `Verdict: unmeasured ({reason})`. A red-run entry's verdict is `pass` when the red set equals the prediction. Long output stays in `/tmp/gsd26/exec/`.

## Index

| Entry | Requirement | Verdict |
|-------|-------------|---------|
| E-01 | DESC-06 (capture harness runs, server, WARN oracle) | pass |
| E-02 | DESC-06 (spike record: double row, classifier rule, affected set) | pass |
| E-03 | DESC-06 (drift premise and fixture guards, commit, tail gate) | pass |
| E-04 | DESC-07 (extraction: testUnit, snapshots, full suite, anchors) | pass |
| E-05 | DESC-08 (red run on the extraction tree) | pass |
| E-06 | DESC-08 (fix green, snapshot red set and re-capture) | pass |
| E-07 | DESC-10 (fix commit: documents, anchors, gate, full suite) | pending (plan 26-03) |
| E-08 | DESC-09 (round-trip on the pre-fix tree) | pending (plan 26-04) |
| E-09 | DESC-09 (round-trip green on the fix tree) | pending (plan 26-04) |
| E-10 | DESC-07 (manual CLIENT_VERSION=V1 run) | pending (plan 26-04) |
| E-11 | Rollout facts for Phase 29 | pending (plan 26-04) |
| E-12 | Phase gate | pending (plan 26-05) |

### E-01: Capture harness runs (DESC-06, D-06)

Commands as run:

```bash
env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain captureDescribeFixtures > /tmp/gsd26/exec/capture-1.log 2>&1
rm -rf /tmp/gsd26/exec/capture-run1 && cp -R src/test/resources/describe-fixtures /tmp/gsd26/exec/capture-run1
env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain captureDescribeFixtures > /tmp/gsd26/exec/capture-2.log 2>&1
diff -r /tmp/gsd26/exec/capture-run1 src/test/resources/describe-fixtures
docker image inspect clickhouse/clickhouse-server:latest --format '{{.Id}} {{.Created}}'
env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain testUnit --tests '*TestsWorkflowPinGuardUnitTest' --rerun > /tmp/gsd26/exec/pinguard-26-01.log 2>&1
```

```text
sha256:bdb5c911a23d92fd364e246ab20b5a0a485f00112f3663fa626b083fb46c146b 2026-10-14T18:05:21.432177888Z
DESCRIBE_FIXTURE_SERVER version=26.9.19.9 image=clickhouse/clickhouse-server:latest image_id=sha256:bdb5c911a23d92fd364e246ab20b5a0a485f00112f3663fa626b083fb46c146b java=17.0.9
DESCRIBE_FIXTURE key=s4_a01 source=E15_AFFECTED rows=5 double_rows=- warns=1 unpaired_warn=yes
DESCRIBE_FIXTURE key=s4_a02 source=E15_AFFECTED rows=5 double_rows=- warns=1 unpaired_warn=yes
DESCRIBE_FIXTURE key=s4_a03 source=E15_AFFECTED rows=6 double_rows=- warns=1 unpaired_warn=yes
DESCRIBE_FIXTURE key=s4_a04 source=E15_AFFECTED rows=6 double_rows=- warns=1 unpaired_warn=yes
DESCRIBE_FIXTURE key=s4_a05 source=E15_AFFECTED rows=9 double_rows=- warns=1 unpaired_warn=yes
DESCRIBE_FIXTURE key=s4_a06 source=E15_AFFECTED rows=6 double_rows=- warns=1 unpaired_warn=yes
DESCRIBE_FIXTURE key=s4_a07 source=E15_AFFECTED rows=5 double_rows=c.x.size0 warns=1 unpaired_warn=yes
DESCRIBE_FIXTURE key=s4_a08 source=E15_AFFECTED rows=6 double_rows=- warns=1 unpaired_warn=yes
DESCRIBE_FIXTURE key=s4_a09 source=E15_AFFECTED rows=6 double_rows=- warns=1 unpaired_warn=yes
DESCRIBE_FIXTURE key=s4_a10 source=E15_AFFECTED rows=7 double_rows=c.size1 warns=1 unpaired_warn=yes
DESCRIBE_FIXTURE key=s4_a11 source=E15_AFFECTED rows=7 double_rows=- warns=1 unpaired_warn=yes
DESCRIBE_FIXTURE key=s4_a12 source=E15_AFFECTED rows=7 double_rows=- warns=1 unpaired_warn=yes
DESCRIBE_FIXTURE key=s4_a13 source=E15_AFFECTED rows=6 double_rows=- warns=1 unpaired_warn=yes
DESCRIBE_FIXTURE key=s4_u01 source=E15_LOOKALIKE rows=5 double_rows=- warns=0 unpaired_warn=no
DESCRIBE_FIXTURE key=s4_u02 source=E15_LOOKALIKE rows=6 double_rows=- warns=0 unpaired_warn=no
DESCRIBE_FIXTURE key=s4_u03 source=E15_LOOKALIKE rows=9 double_rows=- warns=0 unpaired_warn=no
DESCRIBE_FIXTURE key=s4_u04 source=E15_LOOKALIKE rows=5 double_rows=- warns=0 unpaired_warn=no
DESCRIBE_FIXTURE key=s4_u05 source=E15_LOOKALIKE rows=5 double_rows=- warns=0 unpaired_warn=no
DESCRIBE_FIXTURE key=s4_u06 source=E15_LOOKALIKE rows=5 double_rows=- warns=0 unpaired_warn=no
DESCRIBE_FIXTURE key=s4_u07 source=E15_LOOKALIKE rows=5 double_rows=- warns=0 unpaired_warn=no
DESCRIBE_FIXTURE key=s4_u08 source=E15_LOOKALIKE rows=5 double_rows=- warns=0 unpaired_warn=no
DESCRIBE_FIXTURE key=s4_u09 source=E15_LOOKALIKE rows=5 double_rows=- warns=0 unpaired_warn=no
DESCRIBE_FIXTURE key=s4_u10 source=E15_LOOKALIKE rows=5 double_rows=- warns=0 unpaired_warn=no
DESCRIBE_FIXTURE key=s4_u11 source=E15_LOOKALIKE rows=5 double_rows=- warns=0 unpaired_warn=no
DESCRIBE_FIXTURE key=s4_u12 source=E15_LOOKALIKE rows=2 double_rows=- warns=0 unpaired_warn=no
DESCRIBE_FIXTURE key=s4_u13 source=E15_LOOKALIKE rows=2 double_rows=- warns=0 unpaired_warn=no
DESCRIBE_FIXTURE key=s4_u14 source=E15_LOOKALIKE rows=3 double_rows=- warns=0 unpaired_warn=no
DESCRIBE_FIXTURE key=s4_u15 source=E15_LOOKALIKE rows=6 double_rows=- warns=0 unpaired_warn=no
DESCRIBE_FIXTURE key=s4_u16 source=E15_LOOKALIKE rows=2 double_rows=- warns=0 unpaired_warn=no
DESCRIBE_FIXTURE key=s4_u17 source=E15_LOOKALIKE rows=6 double_rows=- warns=0 unpaired_warn=no
DESCRIBE_FIXTURE key=s4_u18 source=E15_LOOKALIKE rows=5 double_rows=- warns=0 unpaired_warn=no
DESCRIBE_FIXTURE key=s4_u19 source=E15_LOOKALIKE rows=5 double_rows=- warns=0 unpaired_warn=no
DESCRIBE_FIXTURE key=s4_o01 source=E15_OVERFLAG rows=8 double_rows=- warns=0 unpaired_warn=no
DESCRIBE_FIXTURE key=n01 source=SPIKE_SHAPE rows=6 double_rows=c.size1,c.size0 warns=1 unpaired_warn=yes
DESCRIBE_FIXTURE key=n02 source=SPIKE_SHAPE rows=7 double_rows=c.size0 warns=1 unpaired_warn=yes
DESCRIBE_FIXTURE key=n03 source=SPIKE_SHAPE rows=7 double_rows=c.size0 warns=1 unpaired_warn=yes
DESCRIBE_FIXTURE key=n04 source=SPIKE_SHAPE rows=9 double_rows=- warns=1 unpaired_warn=yes
DESCRIBE_FIXTURE key=n05 source=SPIKE_SHAPE rows=5 double_rows=- warns=0 unpaired_warn=no
DESCRIBE_FIXTURE key=x01 source=EXTRA_SHAPE rows=6 double_rows=- warns=1 unpaired_warn=yes
DESCRIBE_FIXTURE key=x02 source=EXTRA_SHAPE rows=7 double_rows=- warns=1 unpaired_warn=yes
DESCRIBE_FIXTURE key=x03 source=EXTRA_SHAPE rows=5 double_rows=- warns=1 unpaired_warn=yes
DESCRIBE_FIXTURE key=x04 source=EXTRA_SHAPE rows=6 double_rows=- warns=1 unpaired_warn=yes
DESCRIBE_FIXTURE key=x05 source=EXTRA_SHAPE rows=6 double_rows=c.size1,c.size0 warns=1 unpaired_warn=yes
DESCRIBE_FIXTURE key=x06 source=EXTRA_SHAPE rows=7 double_rows=c.size0 warns=1 unpaired_warn=yes
DESCRIBE_FIXTURE key=x07 source=EXTRA_SHAPE rows=10 double_rows=- warns=1 unpaired_warn=yes
DESCRIBE_FIXTURE key=x08 source=EXTRA_SHAPE rows=6 double_rows=- warns=0 unpaired_warn=no
DESCRIBE_FIXTURE key=r01 source=R01_CRAFTED rows=5 double_rows=- warns=1 unpaired_warn=yes
run 1: BUILD SUCCESSFUL in 22s; run 2: BUILD SUCCESSFUL in 9s
files: 47 .jsonl, 47 .columns.txt, SERVER_VERSION.txt
diff -r run1 run2: silent (byte-identical)
the per-fixture DESCRIBE_FIXTURE lines of run 1 and run 2 are identical
pin guard: Test result: SUCCESS, Tests summary: 39 tests, 39 succeeded, 0 failed, 0 skipped
```

The harness JVM printed `java=17.0.9` (flagged assumption A1: the JavaExec ran on a Java 17 runtime without an explicit launcher).

Timestamp: 2026-10-21T20:54:11Z
Verdict: pass

### E-02: Spike record: double row, classifier rule and the pre-fix affected set (DESC-06)

Measured from the committed rows in `src/test/resources/describe-fixtures/` (captured by `captureDescribeFixtures`, E-01) and from the harness log `/tmp/gsd26/exec/capture-2.log`; no `src/main` file was touched.

server: 26.9.19.9
image_id: sha256:bdb5c911a23d92fd364e246ab20b5a0a485f00112f3663fa626b083fb46c146b

Commands as run:

```bash
python3 -I /tmp/gsd26/exec/double-rows.py src/test/resources/describe-fixtures > /tmp/gsd26/exec/double-rows.txt
env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain testUnit --tests '*DescribeFixturesUnitTest' --rerun > /tmp/gsd26/exec/fixtures-unit.log 2>&1
```

double_row: CONFIRMED

n02 prints `c.size0` twice, in this server order: first typed `UInt64` (the structural size of the outer Array), then typed `Array(Tuple(\n    a UInt8))` (the declared Tuple element `size0`). The same structural-then-declared order holds in every fixture below; the structural row always comes first. Duplicate names per fixture (server order, types):

| fixture | name | types in server order |
|---------|------|-----------------------|
| n01 | c.size0 | UInt64 ; Array(Array(UInt8)) |
| n01 | c.size1 | Array(UInt64) ; Array(Array(UInt8)) |
| n02 | c.size0 | UInt64 ; Array(Tuple(\n    a UInt8)) |
| n03 | c.size0 | UInt64 ; Array(String) |
| s4_a07 | c.x.size0 | UInt64 ; Array(UInt8) |
| s4_a10 | c.size1 | Array(UInt64) ; Array(Array(UInt8)) |
| x05 | c.size0 | UInt64 ; Array(Array(UInt64)) |
| x05 | c.size1 | Array(UInt64) ; Array(Array(UInt64)) |
| x06 | c.size0 | UInt64 ; Array(Nullable(UInt8)) |

size-named sub-column rows per fixture (0 means none):
n01=4, n02=3, n03=5, n04=5, n05=1, r01=2, s4_a01=2, s4_a02=2, s4_a03=2, s4_a04=3, s4_a05=4, s4_a06=2, s4_a07=2, s4_a08=3, s4_a09=3, s4_a10=4, s4_a11=3, s4_a12=3, s4_a13=3, s4_o01=4, s4_u01=2, s4_u02=2, s4_u03=4, s4_u04=2, s4_u05=1, s4_u06=1, s4_u07=1, s4_u08=1, s4_u09=1, s4_u10=1, s4_u11=1, s4_u12=0, s4_u13=0, s4_u14=1, s4_u15=3, s4_u16=0, s4_u17=2, s4_u18=1, s4_u19=1, x01=2, x02=3, x03=3, x04=3, x05=4, x06=3, x07=5, x08=2

Observation on the plan's empty-row edge: `s4_u16` emits no sub-column row at all (root rows only), and `s4_u12`/`s4_u13` emit no size-named sub-column row (the `size`/`size0` there is a root column). `s4_u18` is not empty: it emits one size-named row, `c.v.size` typed `UInt64` (the structural size of the String element `v`; the Enum label text `x, size UInt8` is inside a quoted label and never a row name).

classifier_rule: stated before any src/main change

Inputs: the row's name and type, the parent name (the row's name up to its last dot), `rawTypes[parent]` (the raw DESCRIBE type of every kept row, not only roots) and the Columns kept so far. `s` is the text after the last dot; it is size-named when it is `size` or `size` followed by one or more ASCII digits.

| Step | Condition | Decision |
|------|-----------|----------|
| 1 | not a sub-column row | KEEP |
| 2 | name ends in `.null` | SKIP_STRUCTURAL (the server rejects an element named `null`, code 36) |
| 3 | a JSON or Dynamic ancestor is among the kept Columns (the DESC-05 walk, unchanged, before any size rule) | SKIP_STRUCTURAL |
| 4 | `s` is not size-named | KEEP |
| 5 | no raw type is known for the parent | SKIP_UNDECIDABLE for `size`/`size0`, KEEP_UNDECIDABLE for `size<N>`, N >= 1 (gg3's behaviour), one metadata-only WARN (D-03, D-04) |
| 6 | the parent's raw type text does not contain `s` (it cannot declare it) | structural without parsing: SKIP_STRUCTURAL_SIZE for `size`/`size0`, KEEP_STRUCTURAL_SIZE for `size<N>`, N >= 1 |
| 7 | `ClickHouseColumn.of(parent, DeclaredNullability.normalise(raw))` throws | undecidable as in step 5, one WARN |
| 8 | the parsed parent, read through Array (depth D), Nullable, LowCardinality and SimpleAggregateFunction, is not a Tuple whose `DeclaredNullability.elementKey` list holds `s` (String, scalar array, Map, Variant, a Tuple not declaring `s`) | structural as in step 6 |
| 9 | `s` is `size`, or for no k < D is `s` equal to `"size" + k` with the row type equal to `Array(` k times around `UInt64` (built by string concatenation, never an integer parse) | KEEP_DECLARED; otherwise structural as in step 6 |

Registration: only SKIP_STRUCTURAL and SKIP_UNDECIDABLE rows enter the skip set (a structural size twin never does, so a declared element's children survive); only KEEP and KEEP_DECLARED rows register their raw type; KEEP_DECLARED sets the Column's declared-element flag that `Table.updateParent`'s Array branch reads; at most one WARN per parent per DESCRIBE pass. Never classify on occurrence order, on a closed list of names, or on "the parent is a Tuple" alone.

affected_prefix: s4_a01, s4_a02, s4_a03, s4_a04, s4_a05, s4_a06, s4_a07, s4_a08, s4_a09, s4_a10, s4_a11, s4_a12, s4_a13, n01, n02, n03, n04, x01, x02, x03, x04, x05, x06, x07, r01

other_warn_prefix: -

Predicted versus measured (the plan predicted 25 fixtures):

| Predicted affected (25) | Measured `unpaired_warn=yes` (25) | Difference |
|-------------------------|-----------------------------------|------------|
| s4_a01, s4_a02, s4_a03, s4_a04, s4_a05, s4_a06, s4_a07, s4_a08, s4_a09, s4_a10, s4_a11, s4_a12, s4_a13, n01, n02, n03, n04, x01, x02, x03, x04, x05, x06, x07, r01 | s4_a01, s4_a02, s4_a03, s4_a04, s4_a05, s4_a06, s4_a07, s4_a08, s4_a09, s4_a10, s4_a11, s4_a12, s4_a13, n01, n02, n03, n04, x01, x02, x03, x04, x05, x06, x07, r01 | none |

Not affected, measured `unpaired_warn=no` and `warns=0`: s4_u01, s4_u02, s4_u03, s4_u04, s4_u05, s4_u06, s4_u07, s4_u08, s4_u09, s4_u10, s4_u11, s4_u12, s4_u13, s4_u14, s4_u15, s4_u16, s4_u17, s4_u18, s4_u19, s4_o01, n05, x08. This equals the plan's prediction (19 look-alikes, s4_o01, n05, x08); x08's rows are dropped by the DESC-05 JSON walk, as designed. There is no difference to explain: every predicted key logged the COLS-14 unpaired-element WARN and no other fixture logged any WARN on the helper or DeclaredNullability loggers.

A4 result: `DescribeFixturesUnitTest` reports 95 tests, 0 failures, 0 errors (1 fixture-set test, 47 row-parse tests, 47 parent-type-parse tests); every parent of a size-named sub-column row in all 47 fixtures parses through `ClickHouseColumn.of` after `DeclaredNullability.normalise`, so no E-15 table takes the fail-closed path.

Parse-time note for Phase 28: `DescribeTableBuilder` and `ParentShape` (new production classes of this phase, plans 26-02 and 26-03) are the `jacocoForkReport` inputs that DEBT-17 adds in Phase 28; the list is not edited in Phase 26.

Timestamp: 2026-10-21T20:55:23Z
Verdict: pass

### E-03: Drift premise, fixture guards, commit and tail gate (DESC-06, D-07)

Commands as run:

```bash
env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain test --tests '*DescribeFixtureDriftLiveTest' --tests '*NullableTupleMatrixLiveTest' --rerun > /tmp/gsd26/exec/drift-26-01.log 2>&1
env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain testUnit --rerun > /tmp/gsd26/exec/testunit-26-01.log 2>&1
git worktree add --detach /tmp/gsd26/wt-fmt HEAD   # then ./gradlew -q spotlessApply there, cmp each Java file, git worktree remove --force
PRE_TAG=pre-v1.7.0-tail UPSTREAM_REF=a6f1df839c079180bc7c343b5823f205e204dafc CONFLICTED_PATHS_FILE=.planning/scripts/conflicted-paths-v1.7.0-tail.txt .planning/scripts/verify-merge-invariants.sh 38265d26ce00e078b9e8c7d1198ade333a34f3ac HEAD
```

```text
DescribeFixtureDriftLiveTest: 94 tests, 0 failures, 0 errors
NullableTupleMatrixLiveTest: 33 tests, 0 failures, 0 errors (reads DESCRIBE through the shared DescribeFixtureCatalog.describeRawLines)
DescribeFixturesUnitTest: 95 tests, 0 failures, 0 errors (E-02)
DESCRIBE_FIXTURE_DRIFT client=V2 server=26.9.19.9
testUnit: Test result: SUCCESS | Tests summary: 955 tests, 953 succeeded, 0 failed, 2 skipped   (baseline 860 plus the 95 guards)
test commit: 40ffda7e test(26-01): capture harness, DESCRIBE fixtures and drift premise for size-named Tuple elements (DESC-06)
  non-fixture files: .planning/phases/26-size-named-tuple-elements-in-discovery/26-COMMAND-EVIDENCE.md, build.gradle.kts, src/test/java/com/clickhouse/kafka/connect/sink/NullableRoundTrip.java, src/test/java/com/clickhouse/kafka/connect/sink/db/helper/DescribeFixtureCapture.java, src/test/java/com/clickhouse/kafka/connect/sink/db/helper/DescribeFixtureCatalog.java, src/test/java/com/clickhouse/kafka/connect/sink/db/helper/DescribeFixtureDriftLiveTest.java, src/test/java/com/clickhouse/kafka/connect/sink/db/helper/DescribeFixturesUnitTest.java
  fixture files: 95 under src/test/resources/describe-fixtures/ (47 .jsonl, 47 .columns.txt, SERVER_VERSION.txt); no src/main path in the commit
spotless: spotlessApply in a detached HEAD worktree rewrote the 46 baseline files and gradlew.bat (line endings) and left the five new or edited Java files byte-identical (cmp)
tail gate: invariants: provenance_paths=3 anchor_commits=21 anchor_rows=72 marker_commits=33 anchor_exempt=0 result=PASS
```

Timestamp: 2026-10-21T20:57:59Z
Verdict: pass

### E-04: The behaviour-preserving extraction (DESC-07, D-01, D-02)

Commands as run:

```bash
env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain testUnit --tests '*ClickHouseHelperClientForkUnitTest' --tests '*DeclaredNullabilityTest' --tests '*DescribeFixturesUnitTest' --rerun > /tmp/gsd26/exec/extract-t1.log 2>&1
env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain testUnit --rerun > /tmp/gsd26/exec/testunit-26-02.log 2>&1
git commit (explicit paths)   # refactor(26-02), 8 files
rm -rf build/test-results/test && env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain test --rerun > /tmp/gsd26/exec/suite-26-02.log 2>&1
git worktree add --detach /tmp/gsd26/wt-fmt HEAD   # then ./gradlew -q spotlessApply there, cmp each Java file, git worktree remove --force
PRE_TAG=pre-v1.7.0-tail UPSTREAM_REF=a6f1df839c079180bc7c343b5823f205e204dafc CONFLICTED_PATHS_FILE=.planning/scripts/conflicted-paths-v1.7.0-tail.txt .planning/scripts/verify-merge-invariants.sh 38265d26ce00e078b9e8c7d1198ade333a34f3ac HEAD
```

```text
extraction commit: 9c186046 refactor(26-02): extract the DESCRIBE loop into DescribeTableBuilder and the Tuple element key into DeclaredNullability.elementKey, no behaviour change (DESC-07, 23-REVIEW WR-02)
  files: src/main/java/.../sink/db/helper/DescribeTableBuilder.java (new), src/main/java/.../sink/db/helper/ClickHouseHelperClient.java, src/main/java/.../sink/db/mapping/DeclaredNullability.java, .planning/scripts/fork-anchors.txt, src/test/java/.../sink/db/helper/DescribeFixtureCatalog.java, src/test/java/.../sink/db/helper/ClickHouseHelperClientForkUnitTest.java, src/test/java/.../sink/db/helper/DescribeFixtureSnapshotUnitTest.java (new), src/test/java/.../sink/db/mapping/DeclaredNullabilityTest.java
  no docs file and no committed .jsonl or .columns.txt in the commit (git diff -- src/test/resources/describe-fixtures is empty)
anchor rows: ClickHouseHelperClient shouldSkipDescribeRow (HLP-02) 3 -> 1; buildDescribedTable( (COLS-10) 3 -> 1; new rows 'new DescribeTableBuilder(' 2 (DESC-07) and DeclaredNullability 'elementKey(' 2 (DESC-08) under the '# Phase 26 (v1.7.0_gg4)' comment; 72 -> 74 rows; HLP-04 StandardCharsets.UTF_8 1 / InputStreamReader 2, COLS-10 DeclaredNullability.apply( 1 and two .inner checks unchanged; 'Set<String> skippedCols' 2 -> 0
targeted testUnit (Task 1): Test result: SUCCESS (ForkUnit 51, DeclaredNullabilityTest 62, DescribeFixturesUnitTest 95)
testUnit (Task 2): Test result: SUCCESS; ClickHouseHelperClientForkUnitTest 51, DeclaredNullabilityTest 66 (62 + 4 elementKey rows), DescribeFixtureSnapshotUnitTest 47, DescribeFixturesUnitTest 95, 0 failures, 0 errors; the three 999.4 pin rows pass through the builder
  snapshot differences: none (all 47 keys equal their pre-extraction .columns.txt on the first run)
full suite on 9c186046 (client V2, ClickHouse 26.9.19.9): Test result: SUCCESS | Tests summary: 1543 tests, 1525 succeeded, 0 failed, 18 skipped | BUILD SUCCESSFUL in 2m 40s
  XML files: 95, none older than the commit; failures 0, errors 0, skipped 18 (the 7 named skips plus the 11 bracketed ClickHouseSinkTaskWithSchemaTest rows)
  exact class counts: DescribeFixtureDriftLiveTest 94, DescribeFixturesUnitTest 95, DescribeFixtureSnapshotUnitTest 47, ClickHouseHelperClientForkUnitTest 51, DeclaredNullabilityTest 66, NullableTupleMatrixLiveTest 33, SimpleAggregateTupleRoundTripLiveTest 2, JsonAsStringKeyResidualLiveTest 4, ConnectApiReferencesUnitTest 3
  server version in the DESCRIBE_FIXTURE_DRIFT line: 26.9.19.9 (single, client=V2)
spotless: spotlessApply in a detached HEAD worktree rewrote 47 files, which shows it ran; the seven new or edited Java files are byte-identical (cmp) except ClickHouseHelperClient.java, whose only difference is the lombok/slf4j import order in its import block, a baseline rewrite that predates this plan (HEAD~1 has the same import order and the plan did not touch the import block). The worktree was removed and pruned.
tail gate: invariants: provenance_paths=3 anchor_commits=25 anchor_rows=74 marker_commits=37 anchor_exempt=0 result=PASS
```

Timestamp: 2026-10-21T21:10:39Z
Verdict: pass

### E-05: Red run on the extraction tree (DESC-08, D-10)

Commands as run (tree: `d1d32787`, no `src/main` file differs from HEAD; the only changes are the new `DescribeTableBuilderUnitTest` and the flipped pin in `ClickHouseHelperClientForkUnitTest`):

```bash
env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain testUnit --tests '*DescribeTableBuilderUnitTest' --tests '*ClickHouseHelperClientForkUnitTest' --rerun > /tmp/gsd26/exec/red-26-03.log 2>&1; echo $? > /tmp/gsd26/exec/red-26-03.rc
```

```text
rc: 7 (non-zero)
Test result: FAILURE | Tests summary: 151 tests, 96 succeeded, 55 failed, 0 skipped
DescribeTableBuilderUnitTest: 100 tests (47 + 47 + 6), 52 failing; ClickHouseHelperClientForkUnitTest: 51 tests, 3 failing
A (E-02 affected_prefix, 25 keys): s4_a01..s4_a13, n01..n04, x01..x07, r01
predicted failing set: "DESC-08 declared elements: k" and "D-10 no WARN: k" for every k in A (50), the cascade-children test, the fail-closed test (52 in the builder class), and the three "D-10 (999.4 fixed): ..." rows
actual failing set: equal to the prediction (symmetric difference empty); every other row passed, including the rows of the 19 look-alikes, s4_o01, n05 and x08, "DESC-08 cascade guard: a structural row below a declared element is still dropped", both "DESC-08 structural rows" tests and "DESC-05: no Column below a JSON root"
sample failures:
  DESC-08 declared elements: n02 -> n02: tupleFields of c must equal the declared elements in order ==> expected: <[size0, v]> but was: <[v]>
  D-10 no WARN: x04 -> x04 ==> expected: <[]> but was: <[Declared Tuple element(s) [c.size1] of column c of table db.t (declared type Array(Tuple(size1 UInt64, v String))) have no matching DESCRIBE sub-column; ...]>
  D-03, D-04: an unreadable parent fails closed with one metadata-only WARN -> expected: <1> but was: <0> (the premise assertThrows on ClickHouseColumn.of passed; no helper WARN exists yet)
  DESC-08 cascade guard: the children of a declared size-named element are Columns -> c.size0 is not a field of c
```

Timestamp: 2026-10-21T21:16:34Z
Verdict: pass

### E-06: Fix green, snapshot red set and re-capture (DESC-08)

Commands as run (working tree = the fix, nothing committed yet):

```bash
env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain testUnit --tests '*DescribeTableBuilderUnitTest' --tests '*ClickHouseHelperClientForkUnitTest' --tests '*DeclaredNullabilityTest' --rerun > /tmp/gsd26/exec/green-26-03.log 2>&1
env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain testUnit --rerun > /tmp/gsd26/exec/testunit-26-03-precapture.log 2>&1
env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain captureDescribeFixtures > /tmp/gsd26/exec/capture-26-03.log 2>&1
git diff --name-only -- src/test/resources/describe-fixtures
env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain testUnit --rerun > /tmp/gsd26/exec/testunit-26-03.log 2>&1
```

```text
targeted run: Test result: SUCCESS | Tests summary: 237 tests, 237 succeeded, 0 failed, 0 skipped (DescribeTableBuilderUnitTest 120 = the 100 of E-05 + 17 rule rows + carrier + 2 Table.updateParent tests; ClickHouseHelperClientForkUnitTest 51; DeclaredNullabilityTest 66)
precapture testUnit: Test result: FAILURE | Tests summary: 1126 tests, 1099 succeeded, 25 failed, 2 skipped
  failing set == "DESC-07 snapshot: k" for the 25 keys of A (E-02 affected_prefix), nothing else; DescribeTableBuilderUnitTest 120 tests, 0 failures
capture: server version 26.9.19.9 (unchanged), 47 DESCRIBE_FIXTURE lines, 47 with warns=0 unpaired_warn=no (E-01 had 25 with warns=1 unpaired_warn=yes)
git diff --name-only -- src/test/resources/describe-fixtures: 25 files, exactly <k>.columns.txt for the keys of A; no .jsonl and no SERVER_VERSION.txt changed, so the 19 look-alikes, s4_o01, n05 and x08 snapshots are byte-identical to the pre-fix production
snapshot diffs are pure additions (0 removed lines): the `all` and `tree` lines of the declared elements and their children
testUnit after re-capture: Test result: SUCCESS | Tests summary: 1126 tests, 1124 succeeded, 0 failed, 2 skipped
```

Sample snapshot diffs (n02, a declared `size0` Tuple element with a child; x03, a Tuple of size-named elements):

```text
@@ -3,0 +4,2 @@ all c ARRAY
+all c.size0 ARRAY
+all c.size0.a ARRAY
@@ -7,0 +10,2 @@ tree   [] c TUPLE nullable=false
+tree     c.size0 TUPLE nullable=false
+tree       c.size0.a UINT8 nullable=false
@@ -3,0 +4,2 @@ all size TUPLE
+all size.size UINT8
+all size.size0 STRING
@@ -5,0 +8,2 @@ tree size TUPLE nullable=false
+tree   size.size UINT8 nullable=false
+tree   size.size0 STRING nullable=false
```

Timestamp: 2026-10-21T21:21:28Z
Verdict: pass
