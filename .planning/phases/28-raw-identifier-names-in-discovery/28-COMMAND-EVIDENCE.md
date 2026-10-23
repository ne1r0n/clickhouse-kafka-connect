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
| E-05 | NAME-03/04 tracer red then green, and the testUnit red set of the fix | pass |
| E-06 | NAME-03/04 changed pins red on the unchanged production, coupling proof | pass |
| E-07 | NAME-03/04 fix commit, full suite, old-name loud failure, re-render, gate | pass |
| E-08 | NAME-05 pins red on the pre-fix production, green, commit, gate | pass |
| E-09 | NAME-06 red run and green | pass |
| E-10 | NAME-06/07 fix commit, documents, full suite, manual CLIENT_VERSION=V1 run, gate | pass |
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

### E-05: NAME-03/04 tracer red then green, and the testUnit red set of the fix

Commands as run:

```bash
python3 -I /tmp/gsd28/exec/predict-28-03.py /tmp/gsd28/exec/capture-run2 > /tmp/gsd28/exec/predict-28-03.out
env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain testUnit --tests '*RawNameMatrixFixturesUnitTest' --rerun > /tmp/gsd28/exec/red-28-03-unit.log 2>&1
env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain test --tests '*RawNameMatrixLiveTest' --rerun > /tmp/gsd28/exec/red-28-03-v1.log 2>&1
# the three src/main edits applied as one change in the working tree
env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain testUnit --tests '*RawNameMatrixFixturesUnitTest' --rerun > /tmp/gsd28/exec/green-28-03-unit.log 2>&1
env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain test --tests '*RawNameMatrixLiveTest' --rerun > /tmp/gsd28/exec/green-28-03-v1.log 2>&1
env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain testUnit --rerun > /tmp/gsd28/exec/fixset-28-03.log 2>&1
git diff -- src/main > /tmp/gsd28/exec/fix-28-03.patch
```

```text
predictions (from E-04 cells, rule: a name cell differs from the decoded form, is UNMAPPED/ABSENT/ERROR, or the names WARN count is non-zero):
  name04_red: ['bs', 'bsn', 'bt', 'btbsn', 'cr', 'dq', 'lf', 'sl', 'tab']
  v1_red: ['cr', 'dq', 'lf', 'sl', 'tab']
red on the unchanged production (RawNameMatrixFixturesUnitTest 37 tests: 20 succeeded, 17 failed; RawNameMatrixLiveTest 25 tests: 20 succeeded, 5 failed):
  NAME-03 fromJsonRow red set: ['bs', 'bsn', 'btbsn', 'cr', 'dq', 'lf', 'sl', 'tab'] (exactly bs, bsn, lf, tab, cr, sl, dq, btbsn)
  NAME-04 discovery red set:   ['bs', 'bsn', 'bt', 'btbsn', 'cr', 'dq', 'lf', 'sl', 'tab'] (= prediction)
  NAME-03 client V1 red set:   ['cr', 'dq', 'lf', 'sl', 'tab'] (= prediction)
  the 13 plan 28-02 cases of each class passed (no NAME-02 case failed)
first failure line of one case each:
  NAME-03 ... entry bs: entry bs row {"name":"r\\z",...}: name ==> expected: <r\z>
  NAME-04 ... entry bs: entry bs step root: one root column carries the raw name ==> expected: <1> but was: <0>
  NAME-03 client V1 ... entry lf: entry lf step warnings: discovery must not warn ==> expected: <[]> but was: <[Could not take element nullability for column t of table ...
green on the fix: RawNameMatrixFixturesUnitTest 37 tests, 37 succeeded, 0 failed; RawNameMatrixLiveTest 25 tests, 25 succeeded, 0 failed
testUnit of the fix on the unedited existing tests: 1230 tests, 1218 succeeded, 10 failed, 2 skipped
  failing set (exactly the ten DeclaredNullabilityTest cases): COLS-13 (D-10) x6 (backslash-n inside a backtick name, real newline inside a backtick name, backslash-n inside an Enum8 label, backslash-n names on two nesting levels, names ending in a backslash, the Nullable(Tuple) literal of the live premise), COLS-13 (D-10) edges, NAME-01 x2 (escaped backtick keeps the quote open, two backslashes before n outside quotes), COLS-12 (D-07) structure mismatch: a root with a null name
git diff --stat -- src/main:
  ClickHouseFieldDescriptor.java | 40 +++++-, ClickHouseHelperClient.java | 43 +++---, DeclaredNullability.java | 153 +++++++++++++++------ (3 files, 173 insertions, 63 deletions)
RawNameMatrix.java needed no separate fix commit (the discovery assertion held on the first red run)
```

2026-10-23T09:54:30Z
Verdict: pass

### E-06: NAME-03/04 changed pins red on the unchanged production, coupling proof

Commands as run:

```bash
git diff -- src/main | cmp - /tmp/gsd28/exec/fix-28-03.patch            # patch identity before the swap
git checkout HEAD -- src/main                                          # unchanged production
env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain testUnit --tests '*DeclaredNullabilityTest' --tests '*RawNameMatrixFixturesUnitTest' --rerun > /tmp/gsd28/exec/changed-red-unit.log 2>&1
env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain test --tests '*NullableTupleMatrixLiveTest' --rerun > /tmp/gsd28/exec/changed-red-live.log 2>&1
git apply /tmp/gsd28/exec/fix-28-03.patch && git diff -- src/main | cmp - /tmp/gsd28/exec/fix-28-03.patch
# coupling: HEAD's DeclaredNullability with the decode and the V1 reader applied
git show HEAD:src/main/java/com/clickhouse/kafka/connect/sink/db/mapping/DeclaredNullability.java > src/main/java/com/clickhouse/kafka/connect/sink/db/mapping/DeclaredNullability.java
env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain testUnit --tests '*RawNameMatrixFixturesUnitTest' --rerun > /tmp/gsd28/exec/coupling.log 2>&1
cp /tmp/gsd28/exec/DeclaredNullability.fixed.java src/main/java/com/clickhouse/kafka/connect/sink/db/mapping/DeclaredNullability.java && git diff -- src/main | cmp - /tmp/gsd28/exec/fix-28-03.patch
env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain testUnit --rerun > /tmp/gsd28/exec/testunit-28-03.log 2>&1
env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain test --tests '*NullableTupleMatrixLiveTest' --tests '*RawNameMatrixLiveTest' --rerun > /tmp/gsd28/exec/live-28-03.log 2>&1
```

```text
red on the unchanged production, DeclaredNullabilityTest (69 tests): 11 failed = the ten changed pins X-01..X-10 plus the new NAME-04 case escapedBacktickElementPairsWithoutAFallbackWarning
red on the unchanged production, NullableTupleMatrixLiveTest (33 tests): 3 failed = the two COLS-13 twins (PLAIN, DEFAULT) and the COLS-13 drift test, each "expected: not <null>"
control on the unchanged production: NAME-03 a malformed row fails without echoing its comment passed (both decoders keep the row out of the message chain)
patch identity: git diff -- src/main equals fix-28-03.patch after the apply and after the coupling swap back (cmp silent)
coupling (decode and V1 reader applied, HEAD's DeclaredNullability), RawNameMatrixFixturesUnitTest 38 tests: 7 failed
  NAME-04 discovery red set: ['bs', 'bsn', 'bt', 'btbsn', 'cr', 'lf', 'tab'] (contains lf, tab and cr)
green on the fix: testUnit 1232 tests, 1230 succeeded, 0 failed, 2 skipped; NullableTupleMatrixLiveTest + RawNameMatrixLiveTest 58 tests, 58 succeeded, 0 failed
```

#### Named exemption list

Red evidence cells hold the testcase name and the first failure line measured on HEAD's production (backticks in the failure line are shown as apostrophes).

| ID | Kind | Commit | File | Member | Red evidence | Change | Strictness |
|----|------|--------|------|--------|--------------|--------|------------|
| X-01 | pin | fix(28-03) | `src/test/java/com/clickhouse/kafka/connect/sink/db/mapping/DeclaredNullabilityTest.java` | `backslashNInsideABacktickNameSurvivesNormaliseAndPairs` | `COLS-13 (D-10): a literal backslash-n inside a backtick-quoted element name survives normalise and pairs without a fallback WARN`: expected: <Tuple(     'a\\nb' Int32,     d String)> but was: <Tuple(     'a\\\\nb' Int32,     d String)> | expected normalised literal one escape layer fewer; flag key `c.` + `BACKSLASH_N_ELEMENT_NAME` | same `assertCapturedShapePairs` call: exact literal, exact flag map, no WARN |
| X-02 | pin | fix(28-03) | `src/test/java/com/clickhouse/kafka/connect/sink/db/mapping/DeclaredNullabilityTest.java` | `realNewlineInsideABacktickNameSurvivesNormaliseAndPairs` | `COLS-13 (D-10): a real newline inside a backtick-quoted element name survives normalise and pairs without a fallback WARN`: expected: <Tuple(     'a | expected literal holds the line feed; flag key `c.` + `REAL_NEWLINE_ELEMENT_NAME` | same call, same strictness |
| X-03 | pin | fix(28-03) | `src/test/java/com/clickhouse/kafka/connect/sink/db/mapping/DeclaredNullabilityTest.java` | `backslashNInsideAnEnumLabelIsKeptAndPairs` | `COLS-13 (D-10): a literal backslash-n inside an Enum8 label is kept by normalise and the tuple pairs without a fallback WARN`: expected: <Tuple(     e Enum8('x\\ny' = 1, 'z' = 2),     d String)> but was: <Tuple(     e Enum8('x\\\\ny' = 1, 'z' = 2),     d String)> | expected literal one escape layer fewer; keys unchanged | same call, same strictness |
| X-04 | pin | fix(28-03) | `src/test/java/com/clickhouse/kafka/connect/sink/db/mapping/DeclaredNullabilityTest.java` | `backslashNInNamesOnTwoLevelsSurvivesNormaliseAndPairs` | `COLS-13 (D-10): literal backslash-n names on two nesting levels survive normalise and pair without a fallback WARN`: expected: <Tuple(     'p\\nq' Tuple(         'r\\ns' Int32,         t Nullable(String)),     d String)> but was: <Tuple(     'p\\\\nq' Tuple(         'r\\\\ns' Int32,     | expected literal one escape layer fewer; keys under the raw `outerName` | same call, same strictness |
| X-05 | pin | fix(28-03) | `src/test/java/com/clickhouse/kafka/connect/sink/db/mapping/DeclaredNullabilityTest.java` | `namesEndingInABackslashSurviveNormaliseAndPair` | `COLS-13 (D-10): names ending in a backslash and in backslash-n survive normalise and pair without a fallback WARN`: expected: <Tuple(     'e\\' Int32,     'f\\n' Int32)> but was: <Tuple(     'e\\\\' Int32,     'f\\\\n' Int32)> | expected literal one escape layer fewer; keys `c.e` + backslash and `c.f` + backslash + `n` | same call, same strictness |
| X-06 | pin | fix(28-03) | `src/test/java/com/clickhouse/kafka/connect/sink/db/mapping/DeclaredNullabilityTest.java` | `nullableTupleWithABackslashNNameIsUnchangedAndPairs` | `COLS-13 (D-10): the Nullable(Tuple) literal of the live premise has no line break, loses one escape layer and pairs without a fallback WARN`: expected: <Nullable(Tuple('a\\nb' Int32, d String))> but was: <Nullable(Tuple('a\\\\nb' Int32, d String))> | expected literal one escape layer fewer instead of the RAW constant; raw key; display name says it loses one escape layer (method name kept, so the member is changed, not replaced) | same call, same strictness |
| X-07 | pin | fix(28-03) | `src/test/java/com/clickhouse/kafka/connect/sink/db/mapping/DeclaredNullabilityTest.java` | `normaliseIsTotalAndKeepsNonAsciiNames` | `COLS-13 (D-10) edges: an empty type, an unterminated backtick and a non-ASCII letter next to a backslash-n in a name`: expected: <Tuple('a | unterminated row expects the decoded line feed; non-ASCII row one escape layer fewer; empty-type row unchanged | same `assertEquals` rows, exact |
| X-08 | pin | fix(28-03) | `src/test/java/com/clickhouse/kafka/connect/sink/db/mapping/DeclaredNullabilityTest.java` | `escapedBacktickInsideANameKeepsTheQuoteOpen` | `NAME-01: an escaped backtick inside a quoted name keeps the quote open, so the following line break is still replaced`: expected: <Tuple(     'a\'\\nb' Int32,     d String)> but was: <Tuple(     'a\\'\\\\nb' Int32,     d String)> | expected re-derived for the rewritten `normalise` (pinned on the old one by plan 28-01, E-02) | same `assertEquals`, exact |
| X-09 | pin | fix(28-03) | `src/test/java/com/clickhouse/kafka/connect/sink/db/mapping/DeclaredNullabilityTest.java` | `twoBackslashesBeforeNOutsideQuotesAreNotALineBreak` | `NAME-01: two backslashes before n outside quotes are not a line break`: expected: <Tuple(a Int32,\n b String)> but was: <Tuple(a Int32,\\n b String)> | expected one escape layer fewer, input unchanged | same `assertEquals`, exact |
| X-10 | pin | fix(28-03) | `src/test/java/com/clickhouse/kafka/connect/sink/db/mapping/DeclaredNullabilityTest.java` | `rootWithANullNameIsAStructureMismatch` | `COLS-12 (D-07) structure mismatch: a root with a null name`: WR-02 column segment: Could not take element nullability for column null of table db.t from its declared type Tuple(a Int32) (structure mismatch); keeping the nullability | the WARN's column slot reads `<unnamed>` instead of `null` (23-REVIEW IN-02) | same `assertWarnNamesColumn`, exact |
| X-11 | pin | fix(28-03) | `src/test/java/com/clickhouse/kafka/connect/sink/NullableTupleMatrixLiveTest.java` | `backslashNInTupleElementNameRoundTripsWithoutFallback` | `COLS-13 (D-12) live: a backslash-n in a Tuple element name round-trips without a fallback WARN PLAIN`: c.a\nb ==> expected: not <null>; `COLS-13 (D-12) live: a backslash-n in a Tuple element name round-trips without a fallback WARN DEFAULT`: c.a\nb ==> expected: not <null> | element lookup key, cell schema field and Struct put use `BACKSLASH_N_ELEMENT_NAME` | same premise, no-WARN, nullability and typed read-back assertions |
| X-12 | pin | fix(28-03) | `src/test/java/com/clickhouse/kafka/connect/sink/NullableTupleMatrixLiveTest.java` | `describeReturnsTheCapturedBackslashNEscapingAndPairs` | `COLS-13 (D-12) live: DESCRIBE returns the captured escaping for every Docker-free backslash-n literal, and quoted names pair without a fallb...`: n.p\nq.t ==> expected: not <null> | nested key `n.p` + backslash + `nq.t` (raw) | same assertions |
| S-01 | javadoc | test(28-02) | `src/test/java/com/clickhouse/kafka/connect/sink/db/mapping/DeclaredNullabilityFixtures.java` | `BACKSLASH_N_ELEMENT` | n/a | Javadoc reworded, value unchanged | not a test |
| S-02 | javadoc | test(28-02) | `src/test/java/com/clickhouse/kafka/connect/sink/db/mapping/DeclaredNullabilityFixtures.java` | `REAL_NEWLINE_ELEMENT` | n/a | Javadoc reworded, value unchanged | not a test |
| S-03 | refactor | test(28-02) | `src/test/java/com/clickhouse/kafka/connect/sink/db/helper/DescribeFixtureCatalog.java` | `resourceLines` | n/a | the one-argument form delegates to the new two-argument overload; messages unchanged | not a test |

exemption_signoff: X-01..X-12 (pin) and S-01..S-03 (javadoc, refactor) are the complete list; every pin was red on HEAD's production above and is green on the fix; no other existing test member changed; surfaced to the user for the 28-06 end-of-phase human-check, not decided by them (Phase 22 L-47 precedent)

2026-10-23T09:57:17Z
Verdict: pass

### E-07: NAME-03/04 fix commit, full suite, old-name loud failure, re-render, gate

Commands as run:

```bash
env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain test --tests '*NullableTupleMatrixLiveTest' --rerun > /tmp/gsd28/exec/old-name-loud.log 2>&1   # HEAD's version of the class on the fix
git add <the three src/main files, the four test files, .planning/scripts/fork-anchors.txt>; git write-tree > /tmp/gsd28/exec/fix-tree-28-03.txt
rm -rf build/test-results/test build/raw-name-matrix
env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain test --rerun > /tmp/gsd28/exec/suite-28-03.log 2>&1
git commit            # fix(28-03)
/tmp/gsd28/exec/spotless-numstat.sh "$PWD" HEAD > /tmp/gsd28/exec/spotless-28-03.txt
PRE_TAG=pre-v1.7.0-tail UPSTREAM_REF=a6f1df839c079180bc7c343b5823f205e204dafc CONFLICTED_PATHS_FILE=.planning/scripts/conflicted-paths-v1.7.0-tail.txt .planning/scripts/verify-merge-invariants.sh 38265d26ce00e078b9e8c7d1198ade333a34f3ac HEAD > /tmp/gsd28/exec/gate-28-03.log 2>&1
```

```text
fix_commit: d589234f73b369cb844a0e6018f93f85615c5c52
files: ClickHouseFieldDescriptor.java, ClickHouseHelperClient.java, DeclaredNullability.java (src/main); RawNameMatrixFixturesUnitTest.java, RawNameMatrixLiveTest.java, DeclaredNullabilityTest.java, NullableTupleMatrixLiveTest.java (src/test); .planning/scripts/fork-anchors.txt
tree_equality: git rev-parse HEAD^{tree} = 4b70fbf1e644dd4cd536362ed695968d0e2a3cb2 = fix-tree-28-03.txt (the tree the suite ran on)
suite: 1746 tests in 99 XML files, 0 failures, 0 errors, 18 skipped (the 18 named skips), one server version 26.9.19.9, BUILD SUCCESSFUL in 2m 42s
suite_first_attempt: the first run of this tree ended with 'Gradle Test Executor 161 finished with non-zero exit value 255' (1635 tests run, 0 failed): the known OpenJ9 Mockito GPF, not a test result. The core dump, javacore, Snap and jitdump files it left in the repository root were deleted and the suite was re-run once; the counted run is the second one, on the same unchanged staged tree.
class_counts: NullableTupleMatrixLiveTest=33 DeclaredNullabilityTest=69 RawNameMatrixFixturesUnitTest=38 RawNameMatrixLiveTest=25 TableTest=7 ColumnTest=37 DescribeTableBuilderUnitTest=121 ClickHouseHelperClientForkUnitTest=51 DescribeFixturesUnitTest=96 DescribeFixtureSnapshotUnitTest=47 SourcePlanningIdGuardUnitTest=18
old_name_loud_failure: HEAD's NullableTupleMatrixLiveTest on the fix: 33 tests, 3 failed = the two COLS-13 twins (PLAIN, DEFAULT) and the COLS-13 drift test
  c.a\\nb ==> expected: not <null>   (twins, the old doubled element key)
  n.p\\nq.t ==> expected: not <null> (drift test, the old doubled nested key)
spotless: base 46 lines, head 46 lines, no new file, no changed line; ClickHouseHelperClient.java is not among the 46 base files and is not in head, so the allowed measured line did not appear
gate: invariants: provenance_paths=3 anchor_commits=118 anchor_rows=90 marker_commits=130 anchor_exempt=0 result=PASS
wt_anchors: rows 90 bad []
```

Post-fix re-render (72 cells from the full-suite capture `cells-post-28-03`, the JSON-rendered form of each cell): every name cell (60) of V2 and V1 equals the decoded form; for enum_label the V2 form is the legacy label key for every entry except ap, whose V2 and V1 cells are UNMAPPED / ERROR:NumberFormatException as before (the enum-label parser is plan 28-05's).

```text
entry	shape	decoded	V2	V1
ctl	root	"r_z"	"r_z"	"r_z"
ctl	tuple	"x_y"	"x_y"	"x_y"
ctl	array_tuple	"x_y"	"x_y"	"x_y"
ctl	nullable_tuple	"x_y"	"x_y"	"x_y"
ctl	map_tuple	"x_y"	"x_y"	"x_y"
ctl	enum_label	"x_y"	"x_y"	"x_y"
bs	root	"r\\z"	"r\\z"	"r\\z"
bs	tuple	"x\\y"	"x\\y"	"x\\y"
bs	array_tuple	"x\\y"	"x\\y"	"x\\y"
bs	nullable_tuple	"x\\y"	"x\\y"	"x\\y"
bs	map_tuple	"x\\y"	"x\\y"	"x\\y"
bs	enum_label	"x\\y"	"x\\\\\\\\y"	"x\\\\\\\\y"
bsn	root	"r\\nz"	"r\\nz"	"r\\nz"
bsn	tuple	"x\\ny"	"x\\ny"	"x\\ny"
bsn	array_tuple	"x\\ny"	"x\\ny"	"x\\ny"
bsn	nullable_tuple	"x\\ny"	"x\\ny"	"x\\ny"
bsn	map_tuple	"x\\ny"	"x\\ny"	"x\\ny"
bsn	enum_label	"x\\ny"	"x\\\\\\\\ny"	"x\\\\\\\\ny"
lf	root	"r\nz"	"r\nz"	"r\nz"
lf	tuple	"x\ny"	"x\ny"	"x\ny"
lf	array_tuple	"x\ny"	"x\ny"	"x\ny"
lf	nullable_tuple	"x\ny"	"x\ny"	"x\ny"
lf	map_tuple	"x\ny"	"x\ny"	"x\ny"
lf	enum_label	"x\ny"	"x\\\\ny"	"x\\\\ny"
tab	root	"r\tz"	"r\tz"	"r\tz"
tab	tuple	"x\ty"	"x\ty"	"x\ty"
tab	array_tuple	"x\ty"	"x\ty"	"x\ty"
tab	nullable_tuple	"x\ty"	"x\ty"	"x\ty"
tab	map_tuple	"x\ty"	"x\ty"	"x\ty"
tab	enum_label	"x\ty"	"x\\\\ty"	"x\\\\ty"
cr	root	"r\rz"	"r\rz"	"r\rz"
cr	tuple	"x\ry"	"x\ry"	"x\ry"
cr	array_tuple	"x\ry"	"x\ry"	"x\ry"
cr	nullable_tuple	"x\ry"	"x\ry"	"x\ry"
cr	map_tuple	"x\ry"	"x\ry"	"x\ry"
cr	enum_label	"x\ry"	"x\\\\ry"	"x\\\\ry"
sl	root	"r/z"	"r/z"	"r/z"
sl	tuple	"x/y"	"x/y"	"x/y"
sl	array_tuple	"x/y"	"x/y"	"x/y"
sl	nullable_tuple	"x/y"	"x/y"	"x/y"
sl	map_tuple	"x/y"	"x/y"	"x/y"
sl	enum_label	"x/y"	"x\\/y"	"x\\/y"
dq	root	"r\"z"	"r\"z"	"r\"z"
dq	tuple	"x\"y"	"x\"y"	"x\"y"
dq	array_tuple	"x\"y"	"x\"y"	"x\"y"
dq	nullable_tuple	"x\"y"	"x\"y"	"x\"y"
dq	map_tuple	"x\"y"	"x\"y"	"x\"y"
dq	enum_label	"x\"y"	"x\\\"y"	"x\\\"y"
bt	root	"r`z"	"r`z"	"r`z"
bt	tuple	"x`y"	"x`y"	"x`y"
bt	array_tuple	"x`y"	"x`y"	"x`y"
bt	nullable_tuple	"x`y"	"x`y"	"x`y"
bt	map_tuple	"x`y"	"x`y"	"x`y"
bt	enum_label	"x`y"	"x`y"	"x`y"
na	root	"r\u00E9\u65E5\u672Cz"	"r\u00E9\u65E5\u672Cz"	"r\u00E9\u65E5\u672Cz"
na	tuple	"x\u00E9\u65E5\u672Cy"	"x\u00E9\u65E5\u672Cy"	"x\u00E9\u65E5\u672Cy"
na	array_tuple	"x\u00E9\u65E5\u672Cy"	"x\u00E9\u65E5\u672Cy"	"x\u00E9\u65E5\u672Cy"
na	nullable_tuple	"x\u00E9\u65E5\u672Cy"	"x\u00E9\u65E5\u672Cy"	"x\u00E9\u65E5\u672Cy"
na	map_tuple	"x\u00E9\u65E5\u672Cy"	"x\u00E9\u65E5\u672Cy"	"x\u00E9\u65E5\u672Cy"
na	enum_label	"x\u00E9\u65E5\u672Cy"	"x\u00E9\u65E5\u672Cy"	"x\u00E9\u65E5\u672Cy"
ap	root	"r'z"	"r'z"	"r'z"
ap	tuple	"x'y"	"x'y"	"x'y"
ap	array_tuple	"x'y"	"x'y"	"x'y"
ap	nullable_tuple	"x'y"	"x'y"	"x'y"
ap	map_tuple	"x'y"	"x'y"	"x'y"
ap	enum_label	"x'y"	UNMAPPED	ERROR:NumberFormatException
btbsn	root	"r`\\nz"	"r`\\nz"	"r`\\nz"
btbsn	tuple	"x`\\ny"	"x`\\ny"	"x`\\ny"
btbsn	array_tuple	"x`\\ny"	"x`\\ny"	"x`\\ny"
btbsn	nullable_tuple	"x`\\ny"	"x`\\ny"	"x`\\ny"
btbsn	map_tuple	"x`\\ny"	"x`\\ny"	"x`\\ny"
btbsn	enum_label	"x`\\ny"	"x`\\\\\\\\ny"	"x`\\\\\\\\ny"
```

2026-10-23T10:09:02Z
Verdict: pass

### E-08: NAME-05 pins red on the pre-fix production, green, commit, gate

Commands as run:

```bash
env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain test --tests '*RawIdentifierNameLiveTest.rawNamesLandAndReadBackTyped' --rerun > /tmp/gsd28/exec/tracer-28-04.log 2>&1
env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain test --tests '*RawIdentifierNameLiveTest' --rerun > /tmp/gsd28/exec/green-28-04.log 2>&1   # then cp the XML to green-28-04.xml
python3 -I /tmp/gsd28/exec/predict-28-04.py /tmp/gsd28/exec/capture-run2 > /tmp/gsd28/exec/predict-28-04.out
F=$(git log -1 --format=%H --grep='^fix(28-03)'); for p in $(git diff --name-only "$F^" "$F" -- src/main); do git show "${F}^:${p}" > "$p"; done
env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain test --tests '*RawIdentifierNameLiveTest' --rerun > /tmp/gsd28/exec/prefix-red-28-04.log 2>&1   # cp the XML to prefix-red-28-04.xml
git checkout HEAD -- src/main; git diff --quiet HEAD -- src/main
git add src/test/java/com/clickhouse/kafka/connect/sink/RawIdentifierNameLiveTest.java; git commit   # test(28-04)
env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain testUnit --rerun > /tmp/gsd28/exec/testunit-28-04.log 2>&1
/tmp/gsd28/exec/spotless-numstat.sh "$PWD" HEAD > /tmp/gsd28/exec/spotless-28-04.txt
PRE_TAG=pre-v1.7.0-tail UPSTREAM_REF=a6f1df839c079180bc7c343b5823f205e204dafc CONFLICTED_PATHS_FILE=.planning/scripts/conflicted-paths-v1.7.0-tail.txt .planning/scripts/verify-merge-invariants.sh 38265d26ce00e078b9e8c7d1198ade333a34f3ac HEAD > /tmp/gsd28/exec/gate-28-04.log 2>&1
```

```text
test_commit: ba55f99b015294c9c600b8d41bcca1e67bdb3356 (only src/test/java/com/clickhouse/kafka/connect/sink/RawIdentifierNameLiveTest.java; no src/main change)
tracer: 12 tests, 12 succeeded, 0 failed (keys ap bs bsn bt btbsn cr ctl dq lf na sl tab)
head: 52 tests, 52 succeeded, 0 failed (12 raw, 8 doubled root = bs bsn lf tab cr sl dq btbsn, 32 doubled element)
prediction (predict-28-04.out):
  raw_red: ['bs', 'bsn', 'bt', 'btbsn', 'cr', 'dq', 'lf', 'sl', 'tab']
  root_red: ['bs', 'bsn', 'lf', 'tab', 'cr', 'sl', 'dq', 'btbsn']
  element_green: []
measured on the pre-fix production (the three files of fix(28-03) from F^): 52 tests, 3 succeeded, 49 failed
  raw_red: ['bs', 'bsn', 'bt', 'btbsn', 'cr', 'dq', 'lf', 'sl', 'tab']
  root_red: ['bs', 'bsn', 'btbsn', 'cr', 'dq', 'lf', 'sl', 'tab']
  element_green: []   (all 32 element cases red)
restore: git checkout HEAD -- src/main; git diff --quiet HEAD -- src/main holds
doubled-element failure on HEAD, one recorded case (key=bs container=tuple): RuntimeException "Number of records: 1", root cause DataException, first line
  column 't.x\y' (INT32): no value provided for a non-nullable column without a default; add the field, make the column Nullable, or give it a DEFAULT
  the line names a column path, not a value; the same class and message shape holds for all 32 cases
testunit: 1232 tests, 1230 succeeded, 0 failed, 2 skipped
spotless: base 46 lines, head 46 lines, no new file (the new class is not listed), no changed line
gate: invariants: provenance_paths=3 anchor_commits=122 anchor_rows=90 marker_commits=134 anchor_exempt=0 result=PASS
```

Deviation (Rule 1, plan prediction error, recorded here and in the SUMMARY). The plan's doubled-element design (one container named in the doubled form, the other three raw, no substring) predicted that the pin is red before the fix except in 6 cases. The first measurement on the pre-fix production (kept as `prefix-red-28-04-design1.xml` and `predict-28-04-design1.out`) showed all 32 element cases GREEN: before the fix the doubled form is the one that matches, so the three raw-named containers of the same record fail the put on their own, and the pin passed for a reason that has nothing to do with the container under test. That is a vacuous pin, which T-28-11 forbids. The pin was strengthened, not loosened: `assertPutFails` now also requires the cause chain to hold `column '<container path>.<raw element name>'` (the writer's report of the missing value for the container under test, in the raw form). With that substring all 32 cases pass on HEAD and all 32 fail on the pre-fix production (the pre-fix failure names another column, or the table is unmapped for dq), so `element_green` is empty; `predict-28-04.py` states the rule. The raw and doubled-root sets equal the plan's prediction unchanged.

2026-10-23T10:21:00Z
Verdict: pass

### E-09: NAME-06 red run on the unchanged Column and green

Commands as run (first half, Task 1):

```bash
# prediction from $B/cells-post-28-03/cells-*.txt: an entry is predicted red when its enum_label V2 form is not the decoded label
env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain testUnit --tests '*RawNameMatrixFixturesUnitTest' --rerun > /tmp/gsd28/exec/red-28-05-unit.log 2>&1   # XML copied to red-28-05-unit.xml
env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain test --tests '*RawIdentifierNameLiveTest' --rerun > /tmp/gsd28/exec/red-28-05-live.log 2>&1   # XML copied to red-28-05-live.xml
# Column.extractEnumValues replaced by the escape-aware scan over DeclaredNullability.unescapeJsonLayer
env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain test --tests '*RawNameMatrixFixturesUnitTest' --tests '*RawIdentifierNameLiveTest' --rerun > /tmp/gsd28/exec/green-28-05.log 2>&1
git diff -- src/main > /tmp/gsd28/exec/column-28-05.patch
```

```text
prediction (predict-28-05.out): enum_red: ['bs', 'bsn', 'lf', 'tab', 'cr', 'sl', 'dq', 'ap', 'btbsn']
red on the unchanged Column, enumKeysAreTheRawLabels (RawNameMatrixFixturesUnitTest 50 tests, 41 succeeded, 9 failed):
  red set: ap bs bsn btbsn cr dq lf sl tab (equals the prediction; ctl, bt, na green; no other case red)
  bs, bsn, lf, tab, cr, sl, dq, btbsn: AssertionFailedError "the enum keys are the labels as written in CREATE TABLE" (the map held the pre-fix key form)
  ap: NumberFormatException (the split mis-grouped on the escaped apostrophe: table's enum type unreadable)
red on the unchanged Column, rawEnumLabelLandsAndReadsBackTyped (RawIdentifierNameLiveTest 64 tests, 55 succeeded, 9 failed):
  red set: ap bs bsn btbsn cr dq lf sl tab (equals the prediction; no other case red)
  bs, bsn, lf, tab, cr, sl, dq, btbsn: RuntimeException "Number of records: 1", root cause IllegalArgumentException (the Phase 27 lookup miss, message withheld by the sink)
  ap: RuntimeException "Connection to ClickHouse is not active." (the table is unmapped, so the put fails before any lookup)
green on the scan (one run of the test task): Tests summary: 114 tests, 114 succeeded, 0 failed, 0 skipped (RawNameMatrixFixturesUnitTest 50, RawIdentifierNameLiveTest 64)
src/main: git diff --name-only HEAD -- src/main lists only Column.java; Utils.splitIgnoringQuotes( 1 -> 0, DeclaredNullability.unescapeJsonLayer( 0 -> 1, MALFORMED_ENUM_TYPE_MESSAGE 2, Dynamic 1, ENUM_LABEL_NOT_FOUND_FORMAT 2, NO_ENUM_CONTENT_FORMAT 2
```

Second half (Task 2), commands as run:

```bash
git checkout HEAD -- src/main        # the scan is saved in column-28-05.patch
env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain testUnit --tests '*ColumnTest' --rerun > /tmp/gsd28/exec/rest-red-28-05.log 2>&1   # XML copied to rest-red-28-05-column.xml
env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain test --tests '*RawIdentifierNameLiveTest' --rerun >> /tmp/gsd28/exec/rest-red-28-05.log 2>&1   # XML copied to rest-red-28-05-live.xml
git apply /tmp/gsd28/exec/column-28-05.patch; git diff -- src/main > /tmp/gsd28/exec/diff-now-28-05.patch; cmp diff-now-28-05.patch column-28-05.patch
env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain testUnit --rerun > /tmp/gsd28/exec/testunit-28-05.log 2>&1
env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain test --tests '*RawIdentifierNameLiveTest' --rerun > /tmp/gsd28/exec/live-28-05.log 2>&1
```

```text
red on the unchanged Column (ColumnTest 38 tests, 37 succeeded, 1 failed): enumLabelsHoldingAParenthesisACommaOrAnEqualsSignAreRead() is the only red case
red on the unchanged Column (RawIdentifierNameLiveTest 73 tests, 56 succeeded, 17 failed = the 9 raw cases of E-09 plus 8 legacy cases):
  legacyEnumLabelKeyFailsLoudly red for bs bsn btbsn cr dq lf sl tab (8 of 9); green for ap, whose table is unmapped on the unchanged Column, so the put fails as the pin expects
patch identity: the re-applied diff of src/main is byte-identical to column-28-05.patch (cmp exit 0)
green on the scan: testUnit 1245 tests, 1243 succeeded, 0 failed, 2 skipped (ColumnTest 38, 0 failed); RawIdentifierNameLiveTest 73 tests, 73 succeeded, 0 failed
Phase 27 WRT-18 lookup-miss pins green: unknownEnum8LabelIsALookupMissNamingTheColumn, unknownEnum16LabelIsALookupMissNamingTheColumn, nullEnumLabelIsALookupMiss, enumWithoutValuesNamesTheColumn
scope guard: git diff --name-only HEAD -- src/main lists only Column.java; git diff HEAD -- Utils.java is empty
```

2026-10-23T10:26:29Z
Verdict: pass

### E-10: NAME-06/07 fix commit, documents, full suite, manual CLIENT_VERSION=V1 run, gate

Commands as run:

```bash
git add <Column.java, ColumnTest.java, RawNameMatrixFixturesUnitTest.java, RawIdentifierNameLiveTest.java, .planning/scripts/fork-anchors.txt, docs/FORK-DELTA.md, CHANGELOG.md, .planning/ROADMAP.md>
git write-tree > /tmp/gsd28/exec/fix-tree-28-05.txt
rm -rf build/test-results/test build/raw-name-matrix
env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain test --rerun > /tmp/gsd28/exec/suite-28-05.log 2>&1      # then cp -R build/test-results/test suite-28-05, build/raw-name-matrix cells-post-28-05
CLIENT_VERSION=V1 env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain test --tests '*RawIdentifierNameLiveTest' --tests '*RawNameMatrixLiveTest' --tests '*TableTest' --rerun > /tmp/gsd28/exec/v1-run-28-05.log 2>&1
git commit            # fix(28-05)
/tmp/gsd28/exec/spotless-numstat.sh "$PWD" HEAD > /tmp/gsd28/exec/spotless-28-05.txt
PRE_TAG=pre-v1.7.0-tail UPSTREAM_REF=a6f1df839c079180bc7c343b5823f205e204dafc CONFLICTED_PATHS_FILE=.planning/scripts/conflicted-paths-v1.7.0-tail.txt .planning/scripts/verify-merge-invariants.sh 38265d26ce00e078b9e8c7d1198ade333a34f3ac HEAD > /tmp/gsd28/exec/gate-28-05.log 2>&1
python3 -I /tmp/gsd28/exec/wholesale.py <PB|WT> > /tmp/gsd28/exec/wholesale-28-05.txt
```

```text
fix_commit: d7361cbb482f8f1435b3152f45870fba89fc45c8
files: Column.java (src/main); ColumnTest.java, RawNameMatrixFixturesUnitTest.java, RawIdentifierNameLiveTest.java (src/test); .planning/scripts/fork-anchors.txt, docs/FORK-DELTA.md, CHANGELOG.md, .planning/ROADMAP.md
tree_equality: git rev-parse HEAD^{tree} = fix-tree-28-05.txt (the tree the counted suite and the counted V1 run ran on)
suite: 1832 tests in 100 XML files, 0 failures, 0 errors, 18 skipped (the 18 named skips), one server version 26.9.19.9, BUILD SUCCESSFUL in 2m 41s
class_counts: NullableTupleMatrixLiveTest=33 DeclaredNullabilityTest=69 RawNameMatrixFixturesUnitTest=50 RawNameMatrixLiveTest=25 RawIdentifierNameLiveTest=73 TableTest=7 ColumnTest=38 WriterValueFreeFailureUnitTest=29 DescribeTableBuilderUnitTest=121 ClickHouseHelperClientForkUnitTest=51 DescribeFixturesUnitTest=96 DescribeFixtureSnapshotUnitTest=47 SourcePlanningIdGuardUnitTest=18
v1_run: Tests summary: 105 tests, 105 succeeded, 0 failed, 0 skipped, in 3 XML files (RawIdentifierNameLiveTest, RawNameMatrixLiveTest, TableTest)
v1_evidence: the 41 writer stack frames recorded in the RawIdentifierNameLiveTest XML of the V1 run name doInsertRawBinaryV1(ClickHouseWriter.java:1420); the same class in the default full-suite run names doInsertRawBinaryV2(ClickHouseWriter.java:1324), so the CLIENT_VERSION=V1 run took the V1 write path
re_render: cells-post-28-05: 72 cells (12 entries x 6 shapes), V2 and V1 each equal the decoded form
wholesale (wholesale-28-05.txt): at the phase base f3b1c487: 37 identical, 14 import-only, 12 fork logic (reproduces the FORK-DELTA figures); working tree: 36 identical, 14 import-only, 13 fork logic, the difference being ClickHouseFieldDescriptor.java
changelog: the 1.7.0_gg4 section holds 4 bullets (3 + the new Bug Fixes bullet naming CREATE TABLE and backlog 999.5); the text from '# 1.7.0_gg3, 2026-10-20' down is unchanged
spotless: base 46 lines, head 46 lines, no new file, no changed line; Column.java's measured line is 13 14 in both the base and head (the edit did not change it)
gate: invariants: provenance_paths=3 anchor_commits=125 anchor_rows=92 marker_commits=137 anchor_exempt=0 result=PASS
wt_anchors: rows 92 bad []
```

Deviation (Rule 3, recorded here and in the SUMMARY). The first fix commit (`714e17c0`, suite and V1 run green on its tree) added `import java.util.Map;` to ColumnTest. ColumnTest is one of the 46 files already red under Spotless, and the extra import line moved its numstat line from `5 7` to `6 8`, which the phase's Spotless rule allows only for Column.java and ClickHouseHelperClient.java. The import was replaced by `java.util.Map.of(` in the new row, the commit was amended (not pushed) to `d7361cbb`, and the full suite, the V1 run, the re-render, Spotless and the gate were all re-run on the amended tree; the figures above are those of the second run (the first run's were identical: 1832 tests, 105 tests).

2026-10-23T10:44:00Z
Verdict: pass
