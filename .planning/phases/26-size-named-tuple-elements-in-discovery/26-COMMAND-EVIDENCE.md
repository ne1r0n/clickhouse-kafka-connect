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
| E-03 | DESC-06 (drift premise and fixture guards, commit, tail gate) | pending (plan 26-01) |
| E-04 | DESC-07 (extraction: testUnit, snapshots, full suite, anchors) | pending (plan 26-02) |
| E-05 | DESC-08 (red run on the extraction tree) | pending (plan 26-03) |
| E-06 | DESC-08 (fix green, snapshot red set and re-capture) | pending (plan 26-03) |
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
