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
| E-06 | WRT-18 red run | pass |
| E-07 | WRT-18 fix, full suite, commit, gate | pass |
| E-08 | WRT-19 exact ClassCastException pin and refactor | pass |
| E-09 | WRT-19 guard red on the unscrubbed tree and scrub commits | pass |
| E-10 | WRT-19 guard commit and mutation proof | pass |
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

2026-10-23T00:15:50Z
Verdict: pass

### E-06: WRT-18 red run on the unchanged Column

Commands as run (src/main unchanged from HEAD, nothing committed):

```bash
env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain testUnit --tests '*ColumnTest' --tests '*WriterValueFreeFailureUnitTest' --rerun > /tmp/gsd27/exec/red-27-03.log 2>&1   # rc=1
cp build/test-results/testUnit/TEST-...ColumnTest.xml /tmp/gsd27/exec/red-27-03-column.xml
cp build/test-results/testUnit/TEST-...WriterValueFreeFailureUnitTest.xml /tmp/gsd27/exec/red-27-03-writer.xml
```

```text
plan_head_before: daffc6fbd6fa474341e6175984e35f77ed9bce62
column_red: (37 tests, 4 failed, the 33 existing rows green)
  unknownEnum8LabelIsALookupMissNamingTheColumn()
    -> AssertionFailedError: Expected java.lang.IllegalArgumentException to be thrown, but nothing was thrown.
  unknownEnum16LabelIsALookupMissNamingTheColumn()
    -> AssertionFailedError: Expected java.lang.IllegalArgumentException to be thrown, but nothing was thrown.
  nullEnumLabelIsALookupMiss()
    -> AssertionFailedError: Expected java.lang.IllegalArgumentException to be thrown, but nothing was thrown.
  enumWithoutValuesNamesTheColumn()
    -> AssertionFailedError: expected: <No content in enum column c_enum> but was: <No content in enum>
writer_red: (29 tests, 3 failed, the 26 other rows green)
  WRT-18 value-free: an unknown Enum8 label throws a DataException naming the column and IllegalArgumentException, with no label in any text
    -> AssertionFailedError: expected message ends ...ClickHouse type Enum8: java.lang.IllegalArgumentException but was ...: java.lang.NullPointerException
  WRT-18 value-free: an unknown Enum16 label throws a DataException naming the column and IllegalArgumentException, with no label in any text
    -> AssertionFailedError: expected message ends ...ClickHouse type Enum16: java.lang.IllegalArgumentException but was ...: java.lang.NullPointerException
  D-14: an Enum8 column without enum values throws the fixed text with no label in any text
    -> AssertionFailedError: expected: <No content in enum column c_enum> but was: <No content in enum>
removed_lines_vs_phase_head: WriterValueFreeFailureUnitTest removes exactly one line, the old NO_ENUM_CONTENT_MESSAGE value
src_main_vs_HEAD: identical
```

2026-10-23T00:19:10Z
Verdict: pass

### E-07: WRT-18 fix, full suite on the staged tree, commit, gate

Commands as run:

```bash
env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain testUnit --rerun > /tmp/gsd27/exec/green-27-03.log 2>&1   # 1172 tests, 0 failed
git add <6 explicit paths>; git write-tree > /tmp/gsd27/exec/fix-tree-27-03.txt; date -u > /tmp/gsd27/exec/suite-27-03-start.txt; git diff --quiet -- src docs CHANGELOG.md .planning/scripts
rm -rf build/test-results/test; env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain test --rerun > /tmp/gsd27/exec/suite-27-03.log 2>&1   # rc=0
git commit (staged index, hooks on)
/tmp/gsd27/exec/spotless-numstat.sh "$PWD" HEAD > /tmp/gsd27/exec/spotless-27-03.txt
PRE_TAG=pre-v1.7.0-tail ... .planning/scripts/verify-merge-invariants.sh 38265d26ce00e078b9e8c7d1198ade333a34f3ac HEAD > /tmp/gsd27/exec/gate-27-03.log
```

```text
fix_commit: c1e83945d6ef337007b848739cb32de1a6ddd514  fix(27-03): report an unknown Enum label as a lookup miss naming the column (WRT-18, 22-REVIEW IN-01, IN-02)
fix_files: .planning/scripts/fork-anchors.txt CHANGELOG.md docs/FORK-DELTA.md Column.java ColumnTest.java WriterValueFreeFailureUnitTest.java
tree_equality: staged-tree 1516b9a8ccec0ba9000acf099101e203909f8c4a == HEAD^{tree} of the fix commit
testunit: Tests summary: 1172 tests, 1170 succeeded, 0 failed, 2 skipped
suite: 1661 tests, 0 failures, 0 errors, 18 skipped, 96 XML files, start 2026-10-23T00:20:17Z, no stale XML, BUILD SUCCESSFUL in 2m 39s, one server version 26.9.19.9, client V2
suite_skips: the 18 as a set: 11 ClickHouseSinkTaskWithSchemaTest bracketed cases + clientNameTest, schemaWithNestedTupleMapArrayAndVariant, schemaWithTupleOfMapsWithVariantTest, testDeepNestedStructPerformance, testWideStructPerformance, compareInsertPerformance, compareAllConfigurations
class_counts: WriterFailureClassificationUnitTest=216 WriterValueFreeFailureUnitTest=29 JsonAsStringKeyResidualLiveTest=4 NullableTupleMatrixLiveTest=33 ColumnTest=37 ClickHouseWriterColumnContextTest=8
spotless: 46 lines as at the phase base; Column.java (13/14) and ColumnTest.java (5/7) byte-equal to the base; the only difference is ClickHouseWriter.java at 10<TAB>11 (base 9<TAB>10), the figure plan 27-02 already left (E-05), not the plan's 9<TAB>12
gate: invariants: provenance_paths=3 anchor_commits=83 anchor_rows=82 marker_commits=95 anchor_exempt=0 result=PASS
wt_anchors: rows 82 bad []
src_main_since_phase_base: only fix(27-02) and fix(27-03) touch src/main
open_question_1: decided as recommended. The lookup-miss text is proven at Column level (exact text, label absent); at writer level the operator sees the IllegalArgumentException class and the column already named by the DataException prefix, because WithheldMessageException withholds the IllegalArgumentException's message. Making the text operator-visible would need a pass-through type thrown from Column, a different decision from D-06. Flagged to the user (ledger L-54).
context_deviations:
- D-06: CONTEXT says the UNKNOWN_ENUM_LABEL expectations in WriterValueFreeFailureUnitTest and WriterFailureClassificationUnitTest change from the NullPointerException class to IllegalArgumentException. In the code WriterValueFreeFailureUnitTest has no unknown-label row, and WriterFailureClassificationUnitTest.UNKNOWN_ENUM_LABEL asserts only RetryClass.NON_RETRIABLE (research F-3). No existing expectation changes class; the class pins are the new rows of this plan, and UNKNOWN_ENUM_LABEL stays byte-identical.
- D-01: CONTEXT gives the project's Jackson as 2.21. gradle/libs.versions.toml pins jackson = "2.22.4" (research F-8). D-01's rationale (Jackson 2.12+) still holds; E-02 already records the version.
```

Deviation, Spotless expectation: same as E-05. The plan expects `9<TAB>12` for ClickHouseWriter.java; the measured `10<TAB>11` comes from plan 27-02's used imports, not from this plan (which touches neither ClickHouseWriter.java nor any import).

Deviation, process: an unscoped `./gradlew spotlessApply` run mid-task reformatted the 46 pre-existing violating files (Column.java, ColumnTest.java and WriterValueFreeFailureUnitTest.java included). Every file outside this plan's three Java files was restored with `git checkout -- <path>`, and the three were restored and the plan's edits re-applied by hand, before anything was staged. The staged diff is the plan's own (Column.java 10/3, WriterValueFreeFailureUnitTest.java 21/1, ColumnTest.java 40/0), and the Spotless numstat above confirms no file moved relative to the base other than ClickHouseWriter.java.

2026-10-23T00:25:29Z
Verdict: pass

### E-08: WRT-19 exact ClassCastException pin and the metadata refactor

Commands as run:

```bash
env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain testUnit --tests '*ClickHouseWriterColumnContextTest' --rerun > /tmp/gsd27/exec/cce-pin.log 2>&1   # on the writer as it stood after 27-03
git commit (test file only)
env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain testUnit --rerun > /tmp/gsd27/exec/refactor-27-04.log 2>&1
git add <3 explicit paths>; git write-tree > /tmp/gsd27/exec/refactor-tree-27-04.txt; date -u > /tmp/gsd27/exec/suite-27-04-start.txt
rm -rf build/test-results/test; env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain test --rerun > /tmp/gsd27/exec/suite-27-04.log 2>&1   # twice rc=1 (OpenJ9 GPF), then -PjavaVersion=21 rc=0
git commit (staged index, hooks on)
```

```text
test_commit: c6b2101e4e31ef4f4a6ba74671502cea7d5aa4ff  test(27-04): pin the exact ClassCastException failure text (WRT-19, D-09)
refactor_commit: b78d869162c39b89ae286e29415fccb7a4f3fdec  refactor(27-04): derive write-failure metadata once and build the ClassCastException text from the shared prefix (WRT-19, 22-REVIEW IN-03)
pin_run: Tests summary: 9 tests, 9 succeeded, 0 failed, 0 skipped (ClickHouseWriterColumnContextTest, on the unrefactored writer)
a2: not refuted. The measured text equals the writer's format applied to these inputs character for character, so the constant holds the plan's text as written.
refactor_commit_adjacent: git rev-list --count test..refactor = 1; the test commit changes no src/main file
grep_counts_before_after (ClickHouseWriter.java): 'String sourceType =' 2 -> 0; 'String valueClass =' 2 -> 0; 'sourceTypeName(' 0 -> 3; 'valueClassName(' 0 -> 3; 'WRITE_TYPE_MISMATCH_FAILURE_FORMAT' 0 -> 2; '"Failed to write column ' 2 -> 1; 'WRITE_VALUE_CONVERSION_FAILURE_FORMAT' 2 -> 2
testunit: Tests summary: 1173 tests, 1171 succeeded, 0 failed, 2 skipped
tree_equality: staged-tree 71ab2b8d64b313d555ed95960acf7810261345f9 == HEAD^{tree} of the refactor commit
suite: 1662 tests, 0 failures, 0 errors, 18 skipped, 96 XML files, start 2026-10-23T00:34:44Z, no stale XML, BUILD SUCCESSFUL in 2m 39s, one server version 26.9.19.9, client V2
suite_skips: the 18 as a set (11 ClickHouseSinkTaskWithSchemaTest bracketed cases + the 7 named)
class_counts: WriterFailureClassificationUnitTest=216 WriterValueFreeFailureUnitTest=29 JsonAsStringKeyResidualLiveTest=4 NullableTupleMatrixLiveTest=33 ColumnTest=37 ClickHouseWriterColumnContextTest=9
wt_anchors: rows 83 bad []
```

Deviation, test JVM: the first two full-suite runs on the staged tree ended rc=1 after 1540 tests with 0 failed, 'Process Gradle Test Executor 164 finished with non-zero exit value 255'. The javacore of the first run shows the known OpenJ9 GPF in Mockito inline mocking (Test worker in InstrumentationImpl.retransformClasses0 via InlineBytecodeGenerator.mockClass), at the machine's swap pressure (28.7 of 29.7 GB used). The core.*, javacore.*, Snap.* and jitdump.* files of each run were deleted one by one (never git clean). Logs kept: suite-27-04-run1-gpf.log, suite-27-04-run2-gpf.log. The third run used the documented escape -PjavaVersion=21 on the same staged tree (tree equality holds) and ended BUILD SUCCESSFUL with the counts above. The plan's expectation of 1661 total grew to 1662 by the one new pin test.

Deviation, Spotless expectation: not measured in this task (the plan measures it in Task 3). Plan 27-02 left ClickHouseWriter.java at 10<TAB>11, not 9<TAB>12, and this task changes no import.

2026-10-23T00:38:00Z
Verdict: pass

### E-09: WRT-19 guard red on the unscrubbed tree and the scrub commits

Commands as run:

```bash
python3 -I -c '<PLANNING-ID MIRROR>' > /tmp/gsd27/exec/hits-before.txt   # rc=1: 27 hits, expected
env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain testUnit --tests '*SourcePlanningIdGuardUnitTest' --rerun > /tmp/gsd27/exec/guard-red.log 2>&1   # rc=1, copy class XML to guard-red.xml
mv SourcePlanningIdGuardUnitTest.java /tmp/gsd27/exec/guard-hold/   # so each scrub compile sees exactly the commit's tree
env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain compileJava compileTestJava > /tmp/gsd27/exec/compile-scrub-{1,2}.log 2>&1   # once per scrub commit
git commit (2 explicit src/main paths, then 6)
mv guard-hold/SourcePlanningIdGuardUnitTest.java src/test/java/com/clickhouse/kafka/connect/ci/
env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain testUnit --tests '*SourcePlanningIdGuardUnitTest' --rerun > /tmp/gsd27/exec/guard-green.log 2>&1
```

```text
hits_before: 27 (ClickHouseWriter 11, SinkTaskStatistics 4, DescribeTableBuilder 4, ClickHouseHelperClient 2, DeclaredNullability 2, ParentShape 2, HeaderToValue 1, WithheldMessageException 1); the first line of hits-before.txt reads 27
guard_red: Tests summary: 18 tests, 17 succeeded, 1 failed; the only failure is 'src/main carries no planning identifier on any line', and its message names all eight files
scrub_commit_1: 5c3eb2d14eb2a21da8a9ce2e5c6977a56b6378e6  chore(27-04): drop planning identifiers from the writer-path comments (WRT-19, 22-REVIEW IN-05); 2 src/main files; comment-only check: changed 25, non-comment []; compileJava compileTestJava BUILD SUCCESSFUL (compile-scrub-1.log); wt_anchors rows 83 bad []
scrub_commit_2: e50f3e7bb0113e95c35e293aea648a0c73536898  chore(27-04): drop planning identifiers from the remaining src/main comments (WRT-19, 22-REVIEW IN-05); 6 src/main files; comment-only check: changed 29, non-comment []; compileJava compileTestJava BUILD SUCCESSFUL (compile-scrub-2.log); wt_anchors rows 83 bad []
mirror_after: 0 hits
upstream_820: still present in ClickHouseWriter.java
guard_green: Tests summary: 18 tests, 18 succeeded, 0 failed, 0 skipped (Test result: SUCCESS)
```

2026-10-23T00:39:43Z
Verdict: pass

### E-10: WRT-19 guard commit and mutation proof

Commands as run:

```bash
git commit (guard file and docs/FORK-DELTA.md)
printf '// planted (D-99)\n' >> src/main/java/com/clickhouse/kafka/connect/transforms/HeaderToValue.java
env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain testUnit --tests '*SourcePlanningIdGuardUnitTest' --rerun > /tmp/gsd27/exec/guard-planted.log 2>&1   # rc=1
git checkout -- src/main/java/com/clickhouse/kafka/connect/transforms/HeaderToValue.java; git diff --quiet HEAD -- src/main
env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain testUnit --rerun > /tmp/gsd27/exec/testunit-27-04.log 2>&1
/tmp/gsd27/exec/spotless-numstat.sh "$PWD" HEAD > /tmp/gsd27/exec/spotless-27-04.txt
PRE_TAG=pre-v1.7.0-tail ... .planning/scripts/verify-merge-invariants.sh 38265d26ce00e078b9e8c7d1198ade333a34f3ac HEAD > /tmp/gsd27/exec/gate-27-04.log
```

```text
guard_commit: cafd512cc6883153d669f6095410fe25f3c5bac6  test(27-04): guard src/main against planning identifiers (WRT-19, D-14); files: SourcePlanningIdGuardUnitTest.java, docs/FORK-DELTA.md (Test harness row)
planted_run: Test result: FAILURE; src/main/java/com/clickhouse/kafka/connect/transforms/HeaderToValue.java:237: // planted (D-99) ==> expected: <[]> but was: <[...HeaderToValue.java:237: // planted (D-99)]>
revert: git checkout -- HeaderToValue.java; git diff --quiet HEAD -- src/main exits 0
testunit: Tests summary: 1191 tests, 1189 succeeded, 0 failed, 2 skipped
spotless: 46 lines as at the phase base; the new guard file does not appear; the only difference is ClickHouseWriter.java at 10<TAB>11 (base 9<TAB>10), the figure plan 27-02 already left (E-05), not the plan's 9<TAB>12
gate: invariants: provenance_paths=3 anchor_commits=91 anchor_rows=83 marker_commits=103 anchor_exempt=0 result=PASS
src_main_since_phase_base: git log b8f67b3b..HEAD -- src/main lists fix(27-02), fix(27-03), refactor(27-04) and the two chore(27-04) commits, and nothing else
```

Deviation, Spotless expectation: same as E-05 and E-07. The plan expects `9<TAB>12` for ClickHouseWriter.java; the measured `10<TAB>11` comes from plan 27-02's used imports, and this plan adds no import. Every other line equals the base.

2026-10-23T00:42:40Z
Verdict: pass
