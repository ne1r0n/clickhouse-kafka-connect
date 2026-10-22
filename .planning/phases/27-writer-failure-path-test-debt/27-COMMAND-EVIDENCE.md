# Phase 27: Command evidence

- Phase: 27 (Writer Failure Path & Test Debt)
- Branch: `sync/v1.7.0`
- Purpose: measured proof for the Phase 27 plans. Nothing here was pushed.
- Shell: macOS zsh; every Gradle call is `env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew ...`; Docker is colima profile `vz`
- Entry format: one `### E-NN: {title}` section per entry, with the command as run, the relevant output in a `text` fence, a UTC timestamp, and a final line `Verdict: pass`, `Verdict: fail`, `Verdict: pending ({reason})` or `Verdict: unmeasured ({reason})`. A red-run entry's verdict is `pass` when the red set equals the prediction. Long output stays in `/tmp/gsd27/exec/`.

## Index

| Entry | Requirement | Verdict |
|-------|-------------|---------|
| E-01 | Phase base | pass |
| E-02 | WRT-16 rows green on the unchanged writer | pass |
| E-03 | WRT-17 red run on the unchanged writer, A1 gate, residual probe and predicate seen-red | pending |
| E-04 | WRT-17 predicate sign-off and flip decision | pending |
| E-05 | WRT-17 fix, full suite, commit, gate | pending |
| E-06 | WRT-18 red run | pending |
| E-07 | WRT-18 fix, full suite, commit, gate | pending |
| E-08 | WRT-19 exact ClassCastException pin and refactor | pending |
| E-09 | WRT-19 guard red on the unscrubbed tree and scrub commits | pending |
| E-10 | WRT-19 guard commit and mutation proof | pending |
| E-11 | DEBT-16 FixedString mismatch red proof and counter removal | pending |
| E-12 | DEBT-16 premise-loop mutation proof | pending |
| E-13 | Phase gate and assertion diff check | pending |

### E-01: Phase base

Commands as run:

```bash
git rev-parse HEAD
git ls-remote origin refs/heads/custom_build
/tmp/gsd27/exec/spotless-numstat.sh "$PWD" HEAD > /tmp/gsd27/exec/spotless-base.txt
env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain testUnit --rerun > /tmp/gsd27/exec/testunit-base.log 2>&1
cp build/test-results/testUnit/TEST-com.clickhouse.kafka.connect.sink.WriterFailureClassificationUnitTest.xml /tmp/gsd27/exec/wfc-base.xml
PRE_TAG=pre-v1.7.0-tail UPSTREAM_REF=a6f1df839c079180bc7c343b5823f205e204dafc CONFLICTED_PATHS_FILE=.planning/scripts/conflicted-paths-v1.7.0-tail.txt .planning/scripts/verify-merge-invariants.sh 38265d26ce00e078b9e8c7d1198ade333a34f3ac HEAD > /tmp/gsd27/exec/gate-base.log 2>&1
python3 -I -c '<WT-ANCHORS one-liner>'
```

```text
phase_base: b8f67b3b0c81e19ed782932774e908423533629a
origin_custom_build_tip: 69cd680bf548bfb0329142752d37f8a6c3190c26
testunit_base: 1128 tests, 0 failed, 2 skipped
class_counts: WriterFailureClassificationUnitTest=184 WriterValueFreeFailureUnitTest=21 ColumnTest=33 ClickHouseWriterColumnContextTest=8 TestsWorkflowPinGuardUnitTest=39
anchors: invariants: provenance_paths=3 anchor_commits=74 anchor_rows=79 marker_commits=86 anchor_exempt=0 result=PASS
wt_anchors: rows 79 bad []
spotless_base_lines: 46
spotless_base_writer: 9	10
spotless_base_schema_test: 27	28
spotless_base_md5: f2e32c7bc069ecd155830f82d26285d6
```

Notes: the phase base is HEAD of the plan-27 planning commit (`b8f67b3b`); `git diff e6710e2d HEAD -- src` is empty, so the planning-time baselines hold. The testUnit base run exited 0 on the first attempt (no OpenJ9 crash). The tracked `.planning/STATE.md` carried the orchestrator's "Phase 27 execution started" edit at start; it is planning state, not source, and is not staged by any task commit.

2026-10-22T21:39:41Z
Verdict: pass

### E-02: WRT-16 rows green on the unchanged writer

Commands as run:

```bash
env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain testUnit --tests '*WriterFailureClassificationUnitTest' --rerun > /tmp/gsd27/exec/tracer-27-01.log 2>&1   # tracer, JACKSON_ALONE only: 192 cases
env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain testUnit --tests '*WriterFailureClassificationUnitTest' --rerun > /tmp/gsd27/exec/wfc-27-01.log 2>&1      # all four kinds: 216 cases
git diff -U0 b8f67b3b0c81e19ed782932774e908423533629a -- src/test/java/com/clickhouse/kafka/connect/sink/WriterFailureClassificationUnitTest.java | grep -v '^--- a/' | grep -c '^-'
git commit (test file only)
env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain testUnit --rerun > /tmp/gsd27/exec/testunit-27-01.log 2>&1
/tmp/gsd27/exec/spotless-numstat.sh "$PWD" HEAD > /tmp/gsd27/exec/spotless-27-01.txt; cmp spotless-base.txt spotless-27-01.txt
PRE_TAG=pre-v1.7.0-tail ... .planning/scripts/verify-merge-invariants.sh 38265d26ce00e078b9e8c7d1198ade333a34f3ac HEAD > /tmp/gsd27/exec/gate-27-01.log
```

```text
class_summary: 216 tests, 216 succeeded, 0 failed, 0 skipped
base_names_present: 184/184 passing
new_cases: 32 (JACKSON_ALONE, JACKSON_OVER_SOCKET_TIMEOUT, JACKSON_OVER_TIMEOUT_TEXT, PLAIN_IO_CONTROL, 8 each)
removed_lines_vs_phase_base: 0
src_main_vs_phase_base: identical
test_commit: 108f79a50d5f9946f7d654d8e921879fd996ef80  test(27-01): pin the retry classification of Jackson failures on the unchanged writer (WRT-16)
testunit: Tests summary: 1160 tests, 1158 succeeded, 0 failed, 2 skipped
spotless: numstat byte-equal to spotless-base.txt (46 lines)
gate: invariants: provenance_paths=3 anchor_commits=75 anchor_rows=79 marker_commits=87 anchor_exempt=0 result=PASS
```

Decisions:

- Fourth shape: the fourth row is a plain `IOException` (`PLAIN_IO_CONTROL`), the literal roadmap shape. It duplicates `IO_PLAIN` on purpose so the four shapes read together. A plain `IOException` over a Jackson cause, and a Jackson exception over a plain non-timeout `IOException`, are not WRT-16 rows: a classification row cannot tell a pass-through from a conversion because both are NON_RETRIABLE. Plan 27-02 pins them as same-instance accepted residuals.
- `JsonProcessingException` is realised as `JsonMappingException`: in jackson-core 2.22.4 every `JsonProcessingException` constructor is `protected`, while `JsonMappingException(String)` and `JsonMappingException(String, Throwable)` are public, and it extends `DatabindException` -> `JsonProcessingException` -> `JacksonException` -> `IOException`. The project's Jackson is 2.22.4, not 2.21.

2026-10-22T21:42:30Z
Verdict: pass
