# Phase 28: Command evidence

- Phase: 28 (Raw Identifier Names in Discovery)
- Branch: `sync/v1.7.0`
- Purpose: measured proof for the Phase 28 plans. Nothing here was pushed.
- Shell: macOS zsh; every Gradle call is `env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew ...`; Docker is colima profile `vz`
- Entry format: one `### E-NN: {title}` section per entry, with the command as run, the relevant output in a `text` fence, a UTC timestamp, and a final line `Verdict: pass`, `Verdict: fail`, `Verdict: pending ({reason})` or `Verdict: unmeasured ({reason})`. A red-run entry's verdict is `pass` when the red set equals the prediction. Long output stays in `/tmp/gsd28/exec/`.

## Index

| Entry | Requirement | Verdict |
|-------|-------------|---------|
| E-01 | Phase base | pass |
| E-02 | NAME-01 pin green on the current code and red under the always-close mutation | pass |
| E-03 | NAME-02 tracer and capture runs | pass |
| E-04 | NAME-02 spike record | pass |
| E-05 | NAME-03/04 tracer red then green, and the testUnit red set of the fix | pending |
| E-06 | NAME-03/04 changed pins red on the unchanged production, coupling proof | pending |
| E-07 | NAME-03/04 fix commit, full suite, old-name loud failure, re-render, gate | pending |
| E-08 | NAME-05 pins red on the pre-fix production, green, commit, gate | pending |
| E-09 | NAME-06 red run and green | pending |
| E-10 | NAME-06/07 fix commit, documents, full suite, manual CLIENT_VERSION=V1 run, gate | pending |
| E-11 | DEBT-17 tracer | pending |
| E-12 | DEBT-17 commit and generated fork report | pending |
| E-13 | Phase gate and the assertion diff check | pending |

### E-01: Phase base

Commands as run:

```bash
git rev-parse HEAD
git ls-remote origin refs/heads/custom_build
/tmp/gsd28/exec/spotless-numstat.sh "$PWD" HEAD > /tmp/gsd28/exec/spotless-base.txt
env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain testUnit --rerun > /tmp/gsd28/exec/testunit-base.log 2>&1
rm -rf build/test-results/test
env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain test --rerun > /tmp/gsd28/exec/suite-base.log 2>&1
cp -R build/test-results/test /tmp/gsd28/exec/suite-base
PRE_TAG=pre-v1.7.0-tail UPSTREAM_REF=a6f1df839c079180bc7c343b5823f205e204dafc CONFLICTED_PATHS_FILE=.planning/scripts/conflicted-paths-v1.7.0-tail.txt .planning/scripts/verify-merge-invariants.sh 38265d26ce00e078b9e8c7d1198ade333a34f3ac HEAD > /tmp/gsd28/exec/gate-base.log 2>&1
python3 -I -c '<WT-ANCHORS one-liner>'
docker image inspect clickhouse/clickhouse-server:latest --format '{{.Id}}'
```

```text
phase_base: f3b1c48774bc855913ee69c67b18508f23d84367
origin_custom_build_tip: 69cd680bf548bfb0329142752d37f8a6c3190c26
testunit_base: 1191 tests, 0 failed, 2 skipped
suite_base: 1680 tests, 0 failures, 0 errors, 18 skipped, 97 XML files
class_counts: DeclaredNullabilityTest=66 ColumnTest=37 TableTest=7 DescribeTableBuilderUnitTest=121 ClickHouseHelperClientForkUnitTest=51 DescribeFixturesUnitTest=96 DescribeFixtureSnapshotUnitTest=47 NullableTupleMatrixLiveTest=33 SourcePlanningIdGuardUnitTest=18 WriterValueFreeFailureUnitTest=29
server: ClickHouse server version: 26.9.19.9 (requested tag: latest, image: clickhouse/clickhouse-server:latest) image_id: sha256:bdb5c911a23d92fd364e246ab20b5a0a485f00112f3663fa626b083fb46c146b
anchors: invariants: provenance_paths=3 anchor_commits=108 anchor_rows=83 marker_commits=120 anchor_exempt=0 result=PASS
wt_anchors: rows 83 bad []
spotless_base_lines: 46
spotless_base_md5: 58d9ab85aa1f3be240971fa1c6ec00c9
```

Notes: the phase base is HEAD `f3b1c487`, the orchestrator commit that recorded the Phase 27 verification and the Phase 28 execution start; `git diff 51c00499 HEAD -- src build.gradle.kts gradle .github .planning/scripts docs CHANGELOG.md VERSION lombok.config` is empty, so the planning-time baselines hold. The testUnit and full-suite base runs exited 0 on the first attempt (no OpenJ9 crash). The full suite ended `BUILD SUCCESSFUL in 2m 44s` with `Test result: SUCCESS`. `.planning/state.json`, `.planning/milestone.lock` and `gradlew.bat` carried standing working-tree state and are not staged by any task commit.

2026-10-23T09:31:30Z
Verdict: pass

### E-02: NAME-01 pin green on the current code and red under the always-close mutation

Commands as run:

```bash
env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain testUnit --tests '*DeclaredNullabilityTest' --rerun > /tmp/gsd28/exec/tracer-28-01.log 2>&1
# mutation A (working tree only): `if (escaped == quote && (run / 2) % 2 == 0) {` -> `if (escaped == quote) {`
env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain testUnit --tests '*DeclaredNullabilityTest' --rerun > /tmp/gsd28/exec/mut-a.log 2>&1
git checkout HEAD -- src/main/java/com/clickhouse/kafka/connect/sink/db/mapping/DeclaredNullability.java
# mutation B (working tree only): condition -> `escaped == quote && (run / 2) % 2 != 0`
env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain testUnit --tests '*DeclaredNullabilityTest' --rerun > /tmp/gsd28/exec/mut-b.log 2>&1
git checkout HEAD -- src/main/java/com/clickhouse/kafka/connect/sink/db/mapping/DeclaredNullability.java
git diff --quiet HEAD -- src/main
env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain testUnit --tests '*DeclaredNullabilityTest' --rerun > /tmp/gsd28/exec/restored-28-01.log 2>&1
env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain testUnit --tests '*DeclaredNullabilityTest' --rerun > /tmp/gsd28/exec/dn-28-01.log 2>&1   # with the second test added
git commit (test file only)  -> 1f984bef
env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain testUnit --rerun > /tmp/gsd28/exec/testunit-28-01.log 2>&1
/tmp/gsd28/exec/spotless-numstat.sh "$PWD" HEAD > /tmp/gsd28/exec/spotless-28-01.txt
PRE_TAG=pre-v1.7.0-tail UPSTREAM_REF=a6f1df839c079180bc7c343b5823f205e204dafc CONFLICTED_PATHS_FILE=.planning/scripts/conflicted-paths-v1.7.0-tail.txt .planning/scripts/verify-merge-invariants.sh 38265d26ce00e078b9e8c7d1198ade333a34f3ac HEAD > /tmp/gsd28/exec/gate-28-01.log 2>&1
```

```text
green_on_current_code: DeclaredNullabilityTest 67 tests, 67 succeeded, 0 failed (one new test), then 68 tests, 0 failed (both new tests)
mutation_a_failing_set: ['NAME-01: an escaped backtick inside a quoted name keeps the quote open, so the following line break is still replaced']   (67 tests, 66 succeeded, 1 failed)
mutation_b_failing_set: ['COLS-13 (D-10): names ending in a backslash and in backslash-n survive normalise and pair without a fallback WARN', 'NAME-01: an escaped backtick inside a quoted name keeps the quote open, so the following line break is still replaced']   (67 tests, 65 succeeded, 2 failed)
restored: DeclaredNullabilityTest 67 tests, 0 failed; git diff --quiet HEAD -- src/main exit 0
run_one_guard: equivalent mutant (startsWith at the run start implies a run of one)
testunit_28_01: 1193 tests, 1191 succeeded, 0 failed, 2 skipped
test_commit: 1f984bef (test(28-01), only DeclaredNullabilityTest.java, 15 insertions, 0 removed lines)
spotless: spotless-28-01.txt byte-equal to spotless-base.txt (46 lines)
gate: invariants: provenance_paths=3 anchor_commits=109 anchor_rows=83 marker_commits=121 anchor_exempt=0 result=PASS
```

Notes: the two literals of the new test were compared with 23-REVIEW.md lines 49 and 50 with `grep -F` (one match each). No src/main file differs from the phase base at the test commit. The real server literal for the same shape is captured by the NAME-02 spike in plan 28-02 (entry `btbsn`).

2026-10-23T09:36:00Z
Verdict: pass

### E-03: NAME-02 tracer and capture runs

Commands as run:

```bash
rm -rf build/raw-name-matrix
env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain test --tests '*RawNameMatrixLiveTest' --rerun > /tmp/gsd28/exec/tracer-28-02.log 2>&1   # ctl and bs only
# all twelve entries, twice (run 1, run 2), each preceded by rm -rf build/raw-name-matrix
env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain test --tests '*RawNameMatrixLiveTest' --rerun > /tmp/gsd28/exec/live-28-02-run{1,2}.log 2>&1
cp -R build/raw-name-matrix /tmp/gsd28/exec/capture-run{1,2}
diff -r -x 'cells-*' /tmp/gsd28/exec/capture-run1 /tmp/gsd28/exec/capture-run2
cp /tmp/gsd28/exec/capture-run2/*.jsonl /tmp/gsd28/exec/capture-run2/SERVER_VERSION.txt src/test/resources/raw-name-matrix/
env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain testUnit --tests '*RawNameMatrixFixturesUnitTest' --rerun > /tmp/gsd28/exec/fixtures-28-02.log 2>&1
env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain describeFixtureDrift --tests '*RawNameMatrixDriftLiveTest' > /tmp/gsd28/exec/drift-28-02.log 2>&1
git commit (31 files)  -> test(28-02)
env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain testUnit --rerun > /tmp/gsd28/exec/testunit-28-02.log 2>&1
/tmp/gsd28/exec/spotless-numstat.sh "$PWD" HEAD > /tmp/gsd28/exec/spotless-28-02.txt
PRE_TAG=pre-v1.7.0-tail UPSTREAM_REF=a6f1df839c079180bc7c343b5823f205e204dafc CONFLICTED_PATHS_FILE=.planning/scripts/conflicted-paths-v1.7.0-tail.txt .planning/scripts/verify-merge-invariants.sh 38265d26ce00e078b9e8c7d1198ade333a34f3ac HEAD > /tmp/gsd28/exec/gate-28-02.log 2>&1
```

```text
tracer: RawNameMatrixLiveTest 3 tests, 3 succeeded, 0 failed (ctl, bs, server version); seven capture files; bs root cell V2 form r + two backslashes + z, decoded r + one backslash + z
run1: RawNameMatrixLiveTest 13 tests, 13 succeeded, 0 failed, 0 skipped
run2: RawNameMatrixLiveTest 13 tests, 13 succeeded, 0 failed, 0 skipped
diff_r_jsonl_and_version: silent (the cells-*.txt files were also byte-identical across the two runs)
files_copied: 24 .jsonl (<key>-names.jsonl and <key>-enum.jsonl for ctl bs bsn lf tab cr sl dq bt na ap btbsn) + SERVER_VERSION.txt (version=26.9.19.9), cp from capture-run2, no edit
fixtures: RawNameMatrixFixturesUnitTest 13 tests, 13 succeeded, 0 failed
drift: RawNameMatrixDriftLiveTest 12 tests, 12 succeeded, 0 failed (describeFixtureDrift)
testunit: 1206 tests, 1204 succeeded, 0 failed, 2 skipped
test_commit: f028c57e (test(28-02), 31 files: 4 new test classes, DescribeFixtureCatalog.java, DeclaredNullabilityFixtures.java, 24 rows + SERVER_VERSION.txt)
spotless: spotless-28-02.txt: base 46 head 46 new [] changed []
gate: invariants: provenance_paths=3 anchor_commits=113 anchor_rows=83 marker_commits=125 anchor_exempt=0 result=PASS
src_main: git diff --quiet HEAD -- src/main exit 0
```

Notes: no DDL was rejected by the server for any of the twelve entries and every server premise (P1 to P3) held for all of them. The cells files are not committed.

2026-10-23T09:46:07Z
Verdict: pass

### E-04: NAME-02 spike record

Commands as run:

```bash
python3 -I /tmp/gsd28/exec/spike_checks.py /tmp/gsd28/exec/capture-run2 /tmp/gsd28/exec/spike-table.md
git show f028c57e:src/test/resources/raw-name-matrix/btbsn-names.jsonl   # WR-01 literal
```

```text
server: ClickHouse server version: 26.9.19.9 (requested tag: latest, image: clickhouse/clickhouse-server:latest) image_id: sha256:bdb5c911a23d92fd364e246ab20b5a0a485f00112f3663fa626b083fb46c146b
cells_taken_on: capture run 2 (/tmp/gsd28/exec/capture-run2), the working tree that commit f028c57e then committed; the cells files are not committed and run 1 produced byte-identical cells
spike_checks: c1=pass c2=pass c3=pass c4=pass
```

Legend. server JSON text: the server's own text between the quotes of the `name` field (the `type` field for enum_label), non-ASCII shown as a lowercase \uXXXX escape. current V2 and current V1: the form that the production describeTable of that client yields, as a JSON string (so a backslash is written doubled and a line feed as backslash-n), or UNMAPPED (describeTable returned null) or ERROR:class (it threw). decoded: the raw identifier after a JSON decode of the server row (for enum_label the first label after the SQL unescape). V1 and V2 clients were each built explicitly. Checks: c1 every name cell's V2 form is the server's JSON escaping of the decoded name, dq is UNMAPPED; c2 every enum cell's V2 key is the JSON escaping of the label's SQL text, dq and ap are UNMAPPED; c3 no cell lacks its server row; c4 every cell has a V1 form.

| entry | character | shape | server JSON text | current V2 | current V1 | decoded |
|---|---|---|---|---|---|---|
| ctl | underscore (control) | root | r_z | "r_z" | "r_z" | "r_z" |
| ctl | underscore (control) | tuple | t.x_y | "x_y" | "x_y" | "x_y" |
| ctl | underscore (control) | array_tuple | a.x_y | "x_y" | "x_y" | "x_y" |
| ctl | underscore (control) | nullable_tuple | n.x_y | "x_y" | "x_y" | "x_y" |
| ctl | underscore (control) | map_tuple | m.values.x_y | "x_y" | "x_y" | "x_y" |
| ctl | underscore (control) | enum_label | Enum8('x_y' = 1, 'z' = 2) | "x_y" | "x_y" | "x_y" |
| bs | backslash | root | r\\z | "r\\\\z" | "r\\z" | "r\\z" |
| bs | backslash | tuple | t.x\\y | "x\\\\y" | "x\\y" | "x\\y" |
| bs | backslash | array_tuple | a.x\\y | "x\\\\y" | "x\\y" | "x\\y" |
| bs | backslash | nullable_tuple | n.x\\y | "x\\\\y" | "x\\y" | "x\\y" |
| bs | backslash | map_tuple | m.values.x\\y | "x\\\\y" | "x\\y" | "x\\y" |
| bs | backslash | enum_label | Enum8('x\\\\y' = 1, 'z' = 2) | "x\\\\\\\\y" | "x\\\\y" | "x\\y" |
| bsn | backslash followed by n | root | r\\nz | "r\\\\nz" | "r\\nz" | "r\\nz" |
| bsn | backslash followed by n | tuple | t.x\\ny | "x\\\\ny" | "x\\ny" | "x\\ny" |
| bsn | backslash followed by n | array_tuple | a.x\\ny | "x\\\\ny" | "x\\ny" | "x\\ny" |
| bsn | backslash followed by n | nullable_tuple | n.x\\ny | "x\\\\ny" | "x\\ny" | "x\\ny" |
| bsn | backslash followed by n | map_tuple | m.values.x\\ny | "x\\\\ny" | "x\\ny" | "x\\ny" |
| bsn | backslash followed by n | enum_label | Enum8('x\\\\ny' = 1, 'z' = 2) | "x\\\\\\\\ny" | "x\\\\ny" | "x\\ny" |
| lf | line feed | root | r\nz | "r\\nz" | "r\nz" | "r\nz" |
| lf | line feed | tuple | t.x\ny | "x\\ny" | "x\ny" | "x\ny" |
| lf | line feed | array_tuple | a.x\ny | "x\\ny" | "x\ny" | "x\ny" |
| lf | line feed | nullable_tuple | n.x\ny | "x\\ny" | "x\ny" | "x\ny" |
| lf | line feed | map_tuple | m.values.x\ny | "x\\ny" | "x\ny" | "x\ny" |
| lf | line feed | enum_label | Enum8('x\\ny' = 1, 'z' = 2) | "x\\\\ny" | "x\\ny" | "x\ny" |
| tab | tab | root | r\tz | "r\\tz" | UNMAPPED | "r\tz" |
| tab | tab | tuple | t.x\ty | "x\\ty" | UNMAPPED | "x\ty" |
| tab | tab | array_tuple | a.x\ty | "x\\ty" | UNMAPPED | "x\ty" |
| tab | tab | nullable_tuple | n.x\ty | "x\\ty" | UNMAPPED | "x\ty" |
| tab | tab | map_tuple | m.values.x\ty | "x\\ty" | UNMAPPED | "x\ty" |
| tab | tab | enum_label | Enum8('x\\ty' = 1, 'z' = 2) | "x\\\\ty" | "x\\ty" | "x\ty" |
| cr | carriage return | root | r\rz | "r\\rz" | UNMAPPED | "r\rz" |
| cr | carriage return | tuple | t.x\ry | "x\\ry" | UNMAPPED | "x\ry" |
| cr | carriage return | array_tuple | a.x\ry | "x\\ry" | UNMAPPED | "x\ry" |
| cr | carriage return | nullable_tuple | n.x\ry | "x\\ry" | UNMAPPED | "x\ry" |
| cr | carriage return | map_tuple | m.values.x\ry | "x\\ry" | UNMAPPED | "x\ry" |
| cr | carriage return | enum_label | Enum8('x\\ry' = 1, 'z' = 2) | "x\\\\ry" | "x\\ry" | "x\ry" |
| sl | slash | root | r\/z | "r\\/z" | "r\uFFFDz" | "r/z" |
| sl | slash | tuple | t.x\/y | "x\\/y" | "x\uFFFDy" | "x/y" |
| sl | slash | array_tuple | a.x\/y | "x\\/y" | "x\uFFFDy" | "x/y" |
| sl | slash | nullable_tuple | n.x\/y | "x\\/y" | "x\uFFFDy" | "x/y" |
| sl | slash | map_tuple | m.values.x\/y | "x\\/y" | "x\uFFFDy" | "x/y" |
| sl | slash | enum_label | Enum8('x\/y' = 1, 'z' = 2) | "x\\/y" | "x\uFFFDy" | "x/y" |
| dq | double quote | root | r\"z | UNMAPPED | "r\uFFFDz" | "r\"z" |
| dq | double quote | tuple | t.x\"y | UNMAPPED | "x\uFFFDy" | "x\"y" |
| dq | double quote | array_tuple | a.x\"y | UNMAPPED | "x\uFFFDy" | "x\"y" |
| dq | double quote | nullable_tuple | n.x\"y | UNMAPPED | "x\uFFFDy" | "x\"y" |
| dq | double quote | map_tuple | m.values.x\"y | UNMAPPED | "x\uFFFDy" | "x\"y" |
| dq | double quote | enum_label | Enum8('x\"y' = 1, 'z' = 2) | UNMAPPED | "x\uFFFDy" | "x\"y" |
| bt | backtick | root | r`z | "r`z" | "r`z" | "r`z" |
| bt | backtick | tuple | t.x`y | "x`y" | "x`y" | "x`y" |
| bt | backtick | array_tuple | a.x`y | "x`y" | "x`y" | "x`y" |
| bt | backtick | nullable_tuple | n.x`y | "x`y" | "x`y" | "x`y" |
| bt | backtick | map_tuple | m.values.x`y | "x`y" | "x`y" | "x`y" |
| bt | backtick | enum_label | Enum8('x`y' = 1, 'z' = 2) | "x`y" | "x`y" | "x`y" |
| na | non-ASCII (U+00E9 U+65E5 U+672C) | root | r\u00e9\u65e5\u672cz | "r\u00E9\u65E5\u672Cz" | "r\u00E9\u65E5\u672Cz" | "r\u00E9\u65E5\u672Cz" |
| na | non-ASCII (U+00E9 U+65E5 U+672C) | tuple | t.x\u00e9\u65e5\u672cy | "x\u00E9\u65E5\u672Cy" | "x\u00E9\u65E5\u672Cy" | "x\u00E9\u65E5\u672Cy" |
| na | non-ASCII (U+00E9 U+65E5 U+672C) | array_tuple | a.x\u00e9\u65e5\u672cy | "x\u00E9\u65E5\u672Cy" | "x\u00E9\u65E5\u672Cy" | "x\u00E9\u65E5\u672Cy" |
| na | non-ASCII (U+00E9 U+65E5 U+672C) | nullable_tuple | n.x\u00e9\u65e5\u672cy | "x\u00E9\u65E5\u672Cy" | "x\u00E9\u65E5\u672Cy" | "x\u00E9\u65E5\u672Cy" |
| na | non-ASCII (U+00E9 U+65E5 U+672C) | map_tuple | m.values.x\u00e9\u65e5\u672cy | "x\u00E9\u65E5\u672Cy" | "x\u00E9\u65E5\u672Cy" | "x\u00E9\u65E5\u672Cy" |
| na | non-ASCII (U+00E9 U+65E5 U+672C) | enum_label | Enum8('x\u00e9\u65e5\u672cy' = 1, 'z' = 2) | "x\u00E9\u65E5\u672Cy" | "x\u00E9\u65E5\u672Cy" | "x\u00E9\u65E5\u672Cy" |
| ap | apostrophe | root | r'z | "r'z" | "r'z" | "r'z" |
| ap | apostrophe | tuple | t.x'y | "x'y" | "x'y" | "x'y" |
| ap | apostrophe | array_tuple | a.x'y | "x'y" | "x'y" | "x'y" |
| ap | apostrophe | nullable_tuple | n.x'y | "x'y" | "x'y" | "x'y" |
| ap | apostrophe | map_tuple | m.values.x'y | "x'y" | "x'y" | "x'y" |
| ap | apostrophe | enum_label | Enum8('x\\'y' = 1, 'z' = 2) | UNMAPPED | ERROR:NumberFormatException | "x'y" |
| btbsn | backtick, backslash, n (WR-01) | root | r`\\nz | "r`\\\\nz" | "r`\\nz" | "r`\\nz" |
| btbsn | backtick, backslash, n (WR-01) | tuple | t.x`\\ny | "x`\\\\ny" | "x`\\ny" | "x`\\ny" |
| btbsn | backtick, backslash, n (WR-01) | array_tuple | a.x`\\ny | "x`\\\\ny" | "x`\\ny" | "x`\\ny" |
| btbsn | backtick, backslash, n (WR-01) | nullable_tuple | n.x`\\ny | "x`\\\\ny" | "x`\\ny" | "x`\\ny" |
| btbsn | backtick, backslash, n (WR-01) | map_tuple | m.values.x`\\ny | "x`\\\\ny" | "x`\\ny" | "x`\\ny" |
| btbsn | backtick, backslash, n (WR-01) | enum_label | Enum8('x`\\\\ny' = 1, 'z' = 2) | "x`\\\\\\\\ny" | "x`\\\\ny" | "x`\\ny" |

WARN counts (WARN events on the ClickHouseHelperClient and DeclaredNullability loggers during one describeTable):

- warns ctl: names V2=0 V1=0, enum V2=0 V1=0
- warns bs: names V2=0 V1=0, enum V2=0 V1=0
- warns bsn: names V2=0 V1=0, enum V2=0 V1=0
- warns lf: names V2=0 V1=4, enum V2=0 V1=0
- warns tab: names V2=0 V1=0, enum V2=0 V1=0
- warns cr: names V2=0 V1=0, enum V2=0 V1=0
- warns sl: names V2=4 V1=0, enum V2=0 V1=0
- warns dq: names V2=0 V1=0, enum V2=0 V1=0
- warns bt: names V2=4 V1=0, enum V2=0 V1=0
- warns na: names V2=0 V1=0, enum V2=0 V1=0
- warns ap: names V2=0 V1=0, enum V2=0 V1=0
- warns btbsn: names V2=4 V1=0, enum V2=0 V1=0

wr01_server_literal: Tuple(\n    `x\\`\\\\ny` Int32,\n    d String)
wr01_matches_pin: yes

The first line is the JSON inner text of the `type` of the `t` row of btbsn-names.jsonl as the file holds it (a backslash-n pair is the server's line break). The second line is yes because the NAME-01 pin's input (23-REVIEW line 49, as DeclaredNullabilityTest uses it), with a replaced by x and b by y inside the quoted name, equals that text character for character (compared by script on the runtime text).

findings:
- V2 and V1 both give the decoded name in all five name shapes for ctl, bt, na and ap. For bs, bsn and btbsn V2 is the doubled form (bs: two backslashes where the decoded name has one) and V1 equals the decoded name. For lf, tab and cr V2 is backslash plus n, t or r (two characters), while V1 is the decoded name for lf and UNMAPPED for tab and cr. For sl V2 keeps a backslash before the slash and V1 has U+FFFD in place of the slash. For dq V2 is UNMAPPED and V1 has U+FFFD in place of the double quote.
- Enum labels (enum_label): V2 keys carry two escape layers (bs: four backslashes, decoded: one), V1 keys carry one layer fewer (bs: two) for bs, bsn, btbsn, lf, tab and cr; ctl, bt and na give the same key on both. sl enum is the key with a backslash before the slash on V2 and has U+FFFD on V1. dq enum is UNMAPPED on V2 and has U+FFFD on V1. ap enum is UNMAPPED on V2 and ERROR:NumberFormatException on V1.
- Unmapped tables today: V2: the dq names table, the dq enum table and the ap enum table. V1: the tab and cr names tables (all five name shapes); no V1 table is UNMAPPED apart from those, and V1 throws NumberFormatException for the ap enum table instead of returning null.
- Non-zero WARN counts: names V2 for sl, bt and btbsn (4 each), and names V1 for lf (4). Every other count in the list is 0.

2026-10-23T09:47:28Z
Verdict: pass
