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
| E-04 | NAME-02 spike record | pending |
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
