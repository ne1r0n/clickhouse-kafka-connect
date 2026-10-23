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
| E-03 | WRT-17 red run on the unchanged writer, A1 gate, residual probe and predicate seen-red | pass |
| E-04 | WRT-17 predicate sign-off and flip decision | pass |
| E-05 | WRT-17 fix, full suite, commit, gate | pass |
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

### E-03: WRT-17 red run on the unchanged writer, A1 gate, residual probe and predicate seen-red

Commands as run (src/main unchanged from the phase base except during the scratch predicate run, reverted with `git checkout --`):

```bash
env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain testUnit --tests '*WriterValueFreeFailureUnitTest' --rerun > /tmp/gsd27/exec/red-27-02-unit.log 2>&1        # exit 1
javac --release 17 -cp <build/classes/java/main + jackson-databind/core 2.22.4, jackson-annotations 2.22, connect-api/kafka-clients 4.1.1, gson 2.14.0, slf4j-api 2.0.17> -d /tmp/gsd27/exec/probe/out /tmp/gsd27/exec/probe/ResidualProbe.java && java ... ResidualProbe > /tmp/gsd27/exec/probe-27-02.txt   # exit 0
# scratch: loop condition of passesThroughUnchanged edited to `t instanceof IOException && !(t instanceof com.fasterxml.jackson.core.JacksonException)`, nothing else, original assertJacksonRouteReached
env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain test --tests '*JsonAsStringKeyResidualLiveTest' --rerun > /tmp/gsd27/exec/red-27-02-predicate.log 2>&1   # exit 1
git checkout -- src/main/java/com/clickhouse/kafka/connect/sink/db/ClickHouseWriter.java; git diff --quiet HEAD -- src/main   # exit 0
# helper widened (research F-1), unchanged writer
env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain test --tests '*JsonAsStringKeyResidualLiveTest' --rerun > /tmp/gsd27/exec/red-27-02-live.log 2>&1   # exit 1
```

```text
a1: confirmed
a1_fallback: none
unit_class: 27 tests, 24 passed, 3 failed (21 existing tests unchanged and green)
unit_red:
  WRT-17 value-free: a Jackson failure on the JSON-as-string route throws a DataException naming the column and InvalidDefinitionException, with no Map key in the chain, the rendering or the logs
    -> org.opentest4j.AssertionFailedError: Unexpected type, expected: <org.apache.kafka.connect.errors.DataException> but was: <com.fasterxml.jackson.databind.exc.InvalidDefinitionException>
  WRT-17 value-free: a Jackson failure in an Array(JSON) element is converted at the element, so no Map key passes through the Array
    -> org.opentest4j.AssertionFailedError: Unexpected type, expected: <org.apache.kafka.connect.errors.DataException> but was: <java.lang.RuntimeException>
  WRT-17 value-free: a Jackson exception alone from the stream becomes a DataException with its message withheld
    -> org.opentest4j.AssertionFailedError: Unexpected type, expected: <org.apache.kafka.connect.errors.DataException> but was: <com.fasterxml.jackson.databind.JsonMappingException>
unit_green_new:
  WRT-17 pass-through: a Jackson exception over a SocketTimeoutException is rethrown as the same instance
  WRT-17 accepted residual: a Jackson exception over a plain IOException is rethrown as the same instance
  WRT-17 accepted residual: a plain IOException over a Jackson exception is rethrown as the same instance
probe:
object: chain=com.fasterxml.jackson.databind.exc.InvalidDefinitionException key=true
plain_io: chain=com.fasterxml.jackson.databind.JsonMappingException key=false
io_over_jackson: chain=com.fasterxml.jackson.databind.JsonMappingException key=false
jackson_over_io: chain=com.fasterxml.jackson.databind.JsonMappingException > java.io.IOException key=true
unchecked_io: chain=com.fasterxml.jackson.databind.JsonMappingException > java.io.UncheckedIOException > java.io.IOException key=true
residual_signoff: criterion 2 narrowed by user decision at plan time (2026-10-22, "Accept residual, proceed."); D-02 not reopened
predicate_red: (original assertJacksonRouteReached, scratch-fixed writer; 4 tests, 3 failed)
  errors.tolerance=none keeps the Map key out of the task status chain and the Error inserting records throwable
    -> org.opentest4j.AssertionFailedError: the Jackson route was not reached; the cause chain is: [java.lang.RuntimeException, java.lang.RuntimeException, java.lang.RuntimeException, com.clickhouse.client.api.ClickHouseException, org.apache.kafka.connect.errors.DataException, com.clickhouse.kafka.connect.util.WithheldMessageException ...
  errors.tolerance=all keeps the Map key out of both DLQ exception headers
    -> org.opentest4j.AssertionFailedError: the Jackson route was not reached; the cause chain is: [java.lang.RuntimeException, com.clickhouse.client.api.ClickHouseException, org.apache.kafka.connect.errors.DataException, com.clickhouse.kafka.connect.util.WithheldMessageException] ==> expected: <true> but ...
  a List element that Jackson cannot serialize is named by its index, so no record text reaches any destination
    -> org.opentest4j.AssertionFailedError: the Jackson route was not reached; the cause chain is: [java.lang.RuntimeException, java.lang.RuntimeException, java.lang.RuntimeException, com.clickhouse.client.api.ClickHouseException, org.apache.kafka.connect.errors.DataException, com.clickhouse.kafka.connect.util.WithheldMessageException ...
predicate_green:
  without the setting the write fails before Jackson with the unsupported-type text and no record text
live_red: (widened predicate, unchanged writer; 4 tests, 2 failed, both messages contain `carries SENTKEYa1b2`)
  errors.tolerance=none keeps the Map key out of the task status chain and the Error inserting records throwable
    -> org.opentest4j.AssertionFailedError: task status carries SENTKEYa1b2:
  errors.tolerance=all keeps the Map key out of both DLQ exception headers
    -> org.opentest4j.AssertionFailedError: the stack-trace header carries SENTKEYa1b2:
live_green:
  a List element that Jackson cannot serialize is named by its index, so no record text reaches any destination
  without the setting the write fails before Jackson with the unsupported-type text and no record text
JSON_AS_STRING_RESIDUAL server version=26.9.19.9
src_main_vs_HEAD: identical (git diff --quiet HEAD -- src/main exits 0); nothing committed
```

2026-10-22T21:46:30Z
Verdict: pass

### E-04: WRT-17 predicate sign-off and flip decision

Checkpoint: Task 2 of 27-02 (`blocking-human`), presented by the orchestrator with the E-03 red evidence, the changed live-test assertions and the probe lines.

```text
user_answer (verbatim): "flip-as-shown"
given directly to the orchestrator
answered_at: 2026-10-23T00:06:49Z
option: flip-as-shown
signs_off: assertJacksonRouteReached predicate widening (research F-1); D-03 covers the key flip
seen_red: E-03 predicate_red
  errors.tolerance=none keeps the Map key out of the task status chain and the Error inserting records throwable
    -> org.opentest4j.AssertionFailedError: the Jackson route was not reached; the cause chain is: [java.lang.RuntimeException, java.lang.RuntimeException, java.lang.RuntimeException, com.clickhouse.client.api.ClickHouseException, org.apache.kafka.connect.errors.DataException, com.clickhouse.kafka.connect.util.WithheldMessageException ...
  errors.tolerance=all keeps the Map key out of both DLQ exception headers
    -> org.opentest4j.AssertionFailedError: the Jackson route was not reached; the cause chain is: [java.lang.RuntimeException, com.clickhouse.client.api.ClickHouseException, org.apache.kafka.connect.errors.DataException, com.clickhouse.kafka.connect.util.WithheldMessageException] ==> expected: <true> but ...
  a List element that Jackson cannot serialize is named by its index, so no record text reaches any destination
    -> org.opentest4j.AssertionFailedError: the Jackson route was not reached; the cause chain is: [java.lang.RuntimeException, java.lang.RuntimeException, java.lang.RuntimeException, com.clickhouse.client.api.ClickHouseException, org.apache.kafka.connect.errors.DataException, com.clickhouse.kafka.connect.util.WithheldMessageException ...
```

Notes: the answer is a single word that names exactly one option id and carries no condition or request, so it maps unambiguously. The residual was not asked again (E-03 `residual_signoff:`). Recorded before the fix commit; nothing is committed yet and src/main is unchanged.

2026-10-23T00:06:49Z
Verdict: pass

### E-05: WRT-17 fix, full suite on the staged tree, commit, gate

Commands as run:

```bash
env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain testUnit --rerun > /tmp/gsd27/exec/green-27-02.log 2>&1
env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain test --tests '*JsonAsStringKeyResidualLiveTest' --rerun > /tmp/gsd27/exec/live-27-02.log 2>&1
git add <6 explicit paths>; git write-tree > /tmp/gsd27/exec/fix-tree-27-02.txt; date -u > /tmp/gsd27/exec/suite-27-02-start.txt; git diff --quiet -- src docs CHANGELOG.md .planning/scripts
rm -rf build/test-results/test; env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain test --rerun > /tmp/gsd27/exec/suite-27-02.log 2>&1   # exit 0
git commit (staged index, hooks on)
/tmp/gsd27/exec/spotless-numstat.sh "$PWD" HEAD > /tmp/gsd27/exec/spotless-27-02.txt
PRE_TAG=pre-v1.7.0-tail ... .planning/scripts/verify-merge-invariants.sh 38265d26ce00e078b9e8c7d1198ade333a34f3ac HEAD > /tmp/gsd27/exec/gate-27-02.log
```

```text
fix_commit: a41d5cb1c4b510ea4a2b145ebad5f9347ca50588  fix(27-02): exclude Jackson exceptions from the IOException pass-through and flip the JSON-as-string key pin (WRT-17, 22-REVIEW WR-01)
fix_files: .planning/scripts/fork-anchors.txt CHANGELOG.md docs/FORK-DELTA.md ClickHouseWriter.java JsonAsStringKeyResidualLiveTest.java WriterValueFreeFailureUnitTest.java
tree_equality: staged-tree 5768544d37bf573a7f91418dd6ff4a9bcab1c650 == HEAD^{tree} of the fix commit
a1: confirmed (E-03); a1_fallback: none, so the Docker-free Jackson rows drive the real InvalidDefinitionException
testunit: Tests summary: 1166 tests, 1164 succeeded, 0 failed, 2 skipped
live: JsonAsStringKeyResidualLiveTest 4 tests, 4 succeeded, 0 failed, 0 skipped (ClickHouse server version=26.9.19.9, client V2)
suite: 1655 tests, 0 failures, 0 errors, 18 skipped, 96 XML files, start 2026-10-23T00:10:05Z, no stale XML, BUILD SUCCESSFUL in 2m 40s, one server version 26.9.19.9
suite_skips: the 18 as a set: 11 ClickHouseSinkTaskWithSchemaTest bracketed cases + clientNameTest, schemaWithNestedTupleMapArrayAndVariant, schemaWithTupleOfMapsWithVariantTest, testDeepNestedStructPerformance, testWideStructPerformance, compareInsertPerformance, compareAllConfigurations
class_counts: WriterFailureClassificationUnitTest=216 WriterValueFreeFailureUnitTest=27 JsonAsStringKeyResidualLiveTest=4 NullableTupleMatrixLiveTest=33 ColumnTest=33 ClickHouseWriterColumnContextTest=8
spotless: 46 lines as at the phase base; the only difference is ClickHouseWriter.java at 10<TAB>11 (base 9<TAB>10), not the plan's 9<TAB>12
gate: invariants: provenance_paths=3 anchor_commits=79 anchor_rows=80 marker_commits=91 anchor_exempt=0 result=PASS
wt_anchors: rows 80 bad []
src_main_since_phase_base: only the fix(27-02) commit touches src/main
```

Deviation, Spotless expectation: the plan expects `9<TAB>12` for ClickHouseWriter.java. That figure reproduces only when the two new imports are unused: Spotless `removeUnusedImports` then deletes both (0 additions, 2 deletions). Re-measured on the phase-base file with the two imports added and no usage: `9<TAB>12`. With the fix in place both imports are used (`JacksonException` in the loop condition, `ConnectException` in the condition and the Javadoc link), Spotless keeps them, and the `org.*` import block moves as one hunk: `10<TAB>11`. The imports sit exactly where the plan places them, no other file moved, and the 46 pre-existing violations are the same set.

2026-10-23T00:24:00Z
Verdict: pass
