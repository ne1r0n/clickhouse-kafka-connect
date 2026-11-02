# Phase 42: Command evidence

- **Base commit:** `phase_start_sha` = `1c808c02728e4f9344c46015f63fe4f6cf52a6d9` (HEAD when plan 42-01 started, branch `sync/v1.7.0`).
- **Shell:** macOS zsh, multi-line logic under `/bin/bash` or `python3 -I`. Gradle is run as `env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain ... -PjavaVersion=21`, so every run shares one HotSpot 21 JVM line and the OpenJ9 crash is avoided.
- **Docker:** colima profile `vz` (aarch64, 8 CPU, 16 GiB, 256 GiB disk), docker CLI from `/opt/homebrew/Cellar/docker/29.8.2/bin/docker`.
- **Date (UTC):** 2026-11-02.
- **Entry format:** each entry has a `### E-NN: {title}` heading, a `Backs:` line, an `sh` fence with the commands as run, a `text` fence of `key: value` lines (keys match `[a-z0-9_]+`, one fact per line, no key twice) with free text allowed after the fence, a UTC timestamp line, and a final line `Verdict: pass`, `Verdict: fail`, `Verdict: pending ({reason})` or `Verdict: unmeasured ({reason})`.
- **Rule:** a fail or pending verdict is fixed by re-measuring in a new run, never by editing the evidence.
- **Helpers:** `42-evidence-check.py` in this directory is a byte copy of Phase 41's `41-evidence-check.py` (get, expect, index, rows, secrets, junit, anchors, skips, failed) and is never edited. `42-testcase-status.py` (dump, select, delta, count over JUnit XML) is read-only and is never edited after E-01.
- **Red-reason rule:** a red counts only when the new class's XML exists, every failing testcase carries a `failure` element (no `error`) of type `org.opentest4j.AssertionFailedError`, no failing testcase took more than 30 s, the failing set by name equals the plan's list, and every other case of the class passes.
- **Red freeze rule:** the SHA-256 of each new test file is recorded at its red run, and the file committed in the code commit has the same SHA-256.
- **Status-delta rule:** a gate compares the per-testcase status dump of the previous gate with the new one against the committed expected-added file; no removed line and no unexpected added line is allowed (the existing-test stop rule).
- **No push happens in Phase 42.** Reads use `git ls-remote` only.

## Index

| Entry | Requirement | Plan | Verdict |
|-------|-------------|------|---------|
| E-01 | RSK-01, RSK-02, RSK-03, RSK-04 | 42-01 | pass |
| E-02 | RSK-01 | 42-01 | pass |
| E-03 | RSK-01, RSK-04 | 42-01 | pass |
| E-04 | RSK-01, RSK-04 | 42-01 | pass |
| E-05 | RSK-02 | 42-02 | pass |
| E-06 | RSK-02 | 42-02 | pass |
| E-07 | RSK-02 | 42-02 | pass |
| E-08 | RSK-03 | 42-03 | pass |
| E-09 | RSK-03 | 42-03 | pass |
| E-10 | RSK-03 | 42-03 | pending (plan 42-03) |
| E-11 | RSK-01, RSK-02, RSK-03, RSK-04 | 42-04 | pending (plan 42-04) |
| E-12 | RSK-01, RSK-02, RSK-03, RSK-04 | 42-04 | pending (plan 42-04) |

### E-01: Execution-start facts, measured baseline and environment

Backs: RSK-01, RSK-02, RSK-03, RSK-04 (execution-start facts: base commit, origin re-read, the helper copies, the anchors mirror, the environment, the Spotless baselines of the three files the phase touches, and the start baseline measured on the unchanged tree instead of taken from the Phase 41 record).

```sh
mkdir -p /tmp/gsd42/exec
git rev-parse HEAD
git rev-parse --abbrev-ref HEAD
git status --porcelain
git ls-remote origin refs/heads/custom_build
cp .planning/phases/41-bounded-insert-timeout/41-evidence-check.py .planning/phases/42-isolated-high-risk-edits/42-evidence-check.py
cmp .planning/phases/41-bounded-insert-timeout/41-evidence-check.py .planning/phases/42-isolated-high-risk-edits/42-evidence-check.py
shasum -a 256 .planning/phases/42-isolated-high-risk-edits/42-testcase-status.py
python3 -I .planning/phases/42-isolated-high-risk-edits/42-evidence-check.py anchors .planning/scripts/fork-anchors.txt --rows 184
git diff --quiet refs/tags/v1.7.0 HEAD -- src/main/java/com/clickhouse/kafka/connect/sink/processing/Processing.java
git rev-parse HEAD:src/main/java/com/clickhouse/kafka/connect/sink/ClickHouseSinkConfig.java
git rev-parse HEAD:src/main/java/com/clickhouse/kafka/connect/sink/state/provider/KeeperStateProvider.java
colima list
ls -d /opt/homebrew/Cellar/docker/*/bin/docker
/usr/libexec/java_home -v 21
env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain -q spotlessApply -PspotlessIdeHook="$(pwd)/<file>" -PspotlessIdeHookUseStdOut > /tmp/gsd42/exec/sc-base-<name>.out 2> /tmp/gsd42/exec/sc-base-<name>.err
./gradlew --stop
rm -rf build/test-results/testUnit
DOCKER_HOST=unix:///nonexistent/docker.sock env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain testUnit -PjavaVersion=21 --rerun > /tmp/gsd42/exec/base-testunit.log 2>&1
cp -R build/test-results/testUnit /tmp/gsd42/exec/xml-base-testunit
python3 -I .planning/phases/42-isolated-high-risk-edits/42-testcase-status.py dump /tmp/gsd42/exec/xml-base-testunit /tmp/gsd42/exec/status-base-testunit.tsv
docker run --rm --entrypoint clickhouse clickhouse/clickhouse-server:latest local --query 'SELECT version()'
docker image inspect --format '{{index .RepoDigests 0}}' clickhouse/clickhouse-server:latest
rm -rf build/test-results/test
./gradlew --stop
CLICKHOUSE_VERSION=latest CLIENT_VERSION=V2 env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain test -PjavaVersion=21 --rerun > /tmp/gsd42/exec/base-full.log 2>&1
cp -R build/test-results/test /tmp/gsd42/exec/xml-base-full
python3 -I .planning/phases/42-isolated-high-risk-edits/42-testcase-status.py dump /tmp/gsd42/exec/xml-base-full /tmp/gsd42/exec/status-base-full.tsv
python3 -I .planning/phases/42-isolated-high-risk-edits/42-evidence-check.py skips /tmp/gsd42/exec/xml-base-full
```

```text
phase_start_sha: 1c808c02728e4f9344c46015f63fe4f6cf52a6d9
branch: sync/v1.7.0
dirty_paths: M .planning/config.json; M .planning/state.json
origin_custom_build: 72e8ccffe05ff6dffb14098d0b8c0f67aaa6a236
origin_custom_build_expected: 72e8ccffe05ff6dffb14098d0b8c0f67aaa6a236
helper_copy: cmp-equal
status_helper_sha256: 0e5662aab7cf6925f8491fda1e10304803f5167e837cb79a0c9bdb96393a2762
anchors_mirror: rows=184 bad=0
processing_identical_to_v1_7_0: yes
sink_config_blob: 7ab06b3c2eebe131c06f4c1087b2c73295b443d7
keeper_state_provider_blob: a10abb3b1839eaf5e9b40341e3f38e2fc9411af6
colima_vz: Running
docker_cli: /opt/homebrew/Cellar/docker/29.8.2/bin/docker
jdk21_home: /opt/homebrew/Cellar/openjdk@21/21.0.12.1/libexec/openjdk.jdk/Contents/Home
testcontainers_host_override: set
spotless_baseline_processing: IS DIRTY
spotless_baseline_processing_import_only: yes
spotless_baseline_utils: IS CLEAN
spotless_baseline_sink_task: IS CLEAN
baseline_testunit_exit: 0
baseline_testunit_tests: 1757
baseline_testunit_failures: 0
baseline_testunit_errors: 0
baseline_testunit_skipped: 2
baseline_full_exit: 0
baseline_full_tests: 2355
baseline_full_failures: 0
baseline_full_errors: 0
baseline_full_skipped: 18
baseline_full_skips_helper: OK skips: named=7 state_mismatch_cases=11 unexpected=0
baseline_recorded_testunit: 1757
baseline_recorded_full: 2355
baseline_matches_record: yes
clickhouse_version_baseline: 26.10.3.7
clickhouse_digest_baseline: clickhouse/clickhouse-server@sha256:739e099abd1157390960a59e4532d21781d8fde57e6e4151cb920ea02c30b1ce
jvm_line: -PjavaVersion=21; openjdk version 21.0.12.1 2026-08-18 (Homebrew build 21.0.12.1); no OpenJ9 launcher
base_dump_testunit: 1757
base_dump_full: 2355
matrix_executions_baseline: 216
processing_test_baseline: 14/0
```

The only dirty paths are the two orchestrator-managed `.planning/` files, so the dirty-path rule holds. `origin/custom_build` equals the expected tip. The three Spotless verdicts equal the planning measurement: `Processing.java` differs from the hook's output in 11 lines, every one an import or blank line with equal import sets, so the file is never rewritten by the hook; `Utils.java` and `ClickHouseSinkTask.java` are clean. The measured `testUnit` totals (1757 tests, 2 skipped) and the measured full-suite totals on ClickHouse `latest` x client V2 (2355 tests, the 18 named skips) both equal the recorded Phase 41 totals, and both runs exited 0 with 0 failures and 0 errors; those measured totals are the phase baseline, and each dump's line count equals its run total. The ClickHouse version and image digest read the same before the `testUnit` run and after the full run. No crash file (`hs_err_pid*`, `javacore.*`, `core*`, `Snap.*`) appeared in the repository root. `testcontainers_host_override` is recorded as set only; this shell sets it to a literal that breaks Ryuk, which is why every Gradle run unsets it.

2026-11-02T12:05:50Z

Verdict: pass

### E-02: RSK-01 red run on the unchanged tree: four behaviour and limit pins fail by assertion, the two guards pass

Backs: RSK-01 (the Docker-free `OriginalTopicInsertedOffsetsUnitTest`, run before any production change: two behaviour pins and two limit pins fail by assertion, the trace guard and the no-SMT control pass; the existing classes of the path pass).

```sh
git diff --quiet HEAD -- src/main
env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain -q spotlessApply -PspotlessIdeHook="$(pwd)/src/test/java/com/clickhouse/kafka/connect/sink/OriginalTopicInsertedOffsetsUnitTest.java" -PspotlessIdeHookUseStdOut > /tmp/gsd42/exec/sc-r1.out 2> /tmp/gsd42/exec/sc-r1.err
shasum -a 256 src/test/java/com/clickhouse/kafka/connect/sink/OriginalTopicInsertedOffsetsUnitTest.java
rm -rf build/test-results/testUnit
DOCKER_HOST=unix:///nonexistent/docker.sock env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain testUnit -PjavaVersion=21 --rerun --tests '*OriginalTopicInsertedOffsetsUnitTest' --tests '*.ProcessingTest' --tests '*ProcessingRenamedTopicStateKeyUnitTest' --tests '*BufferedOffsetCommitUnitTest' > /tmp/gsd42/exec/rsk01-red.log 2>&1
cp -R build/test-results/testUnit /tmp/gsd42/exec/xml-rsk01-red
python3 -I .planning/phases/42-isolated-high-risk-edits/42-evidence-check.py junit /tmp/gsd42/exec/xml-rsk01-red --class .OriginalTopicInsertedOffsetsUnitTest tests=6 failures=4
python3 -I .planning/phases/42-isolated-high-risk-edits/42-evidence-check.py failed /tmp/gsd42/exec/xml-rsk01-red --expect 'OriginalTopicInsertedOffsetsUnitTest#directDeliveryReportsTheConsumersTopicUnderAnInjectiveRename' 'OriginalTopicInsertedOffsetsUnitTest#closeClearsTheInsertedOffsetOfTheRevokedConsumerPartition' 'OriginalTopicInsertedOffsetsUnitTest#limitManyToOneRenameReportsTheSharedMaximumForTheFirstSourceTopic' 'OriginalTopicInsertedOffsetsUnitTest#limitPartitionChangingSmtKeepsTheTransformedPartitionInTheKey'
```

```text
spotless_test_class: IS CLEAN
red_run_exit: 1
red_class_tests: 6
red_class_failures: 4
red_failed_count: 4
red_failed_set: OriginalTopicInsertedOffsetsUnitTest#directDeliveryReportsTheConsumersTopicUnderAnInjectiveRename; OriginalTopicInsertedOffsetsUnitTest#closeClearsTheInsertedOffsetOfTheRevokedConsumerPartition; OriginalTopicInsertedOffsetsUnitTest#limitManyToOneRenameReportsTheSharedMaximumForTheFirstSourceTopic; OriginalTopicInsertedOffsetsUnitTest#limitPartitionChangingSmtKeepsTheTransformedPartitionInTheKey
red_reason_assertion: yes
guards_green_on_unchanged: yes
existing_classes_green: yes
red_test_sha256: 137ddbbcf23e9b544bd01564715253e0ba1d9595dbb148bd54b215eea4204fb1
src_main_untouched: yes
```

The four failures are all `org.opentest4j.AssertionFailedError` failure elements (no `error` element) and none took more than 30 s. The failure messages show the unchanged tree's behaviour: `directDeliveryReportsTheConsumersTopicUnderAnInjectiveRename` expected `{orig-dev-0=10}` from `preCommit` and got an empty map, `closeClearsTheInsertedOffsetOfTheRevokedConsumerPartition` expected an empty snapshot and found the stale `{orig-0=10}`, and the two `limit...` pins saw an empty `preCommit` result where the consumer's topic key is expected. The trace guard `stateKeyAndFlagSequenceStayOnTheTransformedTopicUnderAnInjectiveRename` and the control `noSmtRecordsKeepTheirTopicAndAreClearedOnClose` passed on the unchanged tree with the literal written from the research trace, so no guard literal needed a correction. `ProcessingTest` (14), `ProcessingRenamedTopicStateKeyUnitTest` (2) and `BufferedOffsetCommitUnitTest` (15) passed in the same run.

2026-11-02T12:06:26Z

Verdict: pass

### E-03: RSK-01 code commit: the line-137 edit green with its pins, the FORK-DELTA row, Known cost 16 with the OI-35 limits, two anchor rows

Backs: RSK-01, RSK-04 (the one-line edit of `Processing.doLogic` line 137 green together with its six pins and the existing classes of the path, the Processing.java Connector source row, Known cost 16 rewritten with the OI-35-2 and OI-35-3 limits, the two anchor rows, and the code commit that holds them as one revert unit).

```sh
sed -n 137p src/main/java/com/clickhouse/kafka/connect/sink/processing/Processing.java
rm -rf build/test-results/testUnit
DOCKER_HOST=unix:///nonexistent/docker.sock env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain testUnit -PjavaVersion=21 --rerun --tests '*OriginalTopicInsertedOffsetsUnitTest' --tests '*.ProcessingTest' --tests '*ProcessingRenamedTopicStateKeyUnitTest' --tests '*BufferedOffsetCommitUnitTest' --tests '*ConnectApiReferencesUnitTest' --tests '*SourcePlanningIdGuardUnitTest' > /tmp/gsd42/exec/rsk01-green.log 2>&1
cp -R build/test-results/testUnit /tmp/gsd42/exec/xml-rsk01-green
python3 -I .planning/phases/42-isolated-high-risk-edits/42-testcase-status.py dump /tmp/gsd42/exec/xml-rsk01-red /tmp/gsd42/exec/status-rsk01-red.tsv
python3 -I .planning/phases/42-isolated-high-risk-edits/42-testcase-status.py select .planning/phases/42-isolated-high-risk-edits/42-rsk01-expected-added.tsv PASS /tmp/gsd42/exec/status-rsk01-red.tsv .OriginalTopicInsertedOffsetsUnitTest
python3 -I .planning/phases/42-isolated-high-risk-edits/42-evidence-check.py anchors .planning/scripts/fork-anchors.txt --rows 186
env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain -q spotlessApply -PspotlessIdeHook="$(pwd)/<file>" -PspotlessIdeHookUseStdOut > /tmp/gsd42/exec/sc-<name>.out 2> /tmp/gsd42/exec/sc-<name>.err
git add src/main/java/com/clickhouse/kafka/connect/sink/processing/Processing.java src/test/java/com/clickhouse/kafka/connect/sink/OriginalTopicInsertedOffsetsUnitTest.java docs/FORK-DELTA.md .planning/scripts/fork-anchors.txt
git commit -F /tmp/gsd42/exec/msg-code1.txt
git diff --numstat cba0909b^ cba0909b -- src/main/java/com/clickhouse/kafka/connect/sink/processing/Processing.java
git diff --quiet 1c808c02 cba0909b -- src/main/java/com/clickhouse/kafka/connect/sink/state src/main/java/com/clickhouse/kafka/connect/sink/kafka src/main/java/com/clickhouse/kafka/connect/sink/ClickHouseSinkConfig.java src/test/java/com/clickhouse/kafka/connect/sink/processing/ProcessingTest.java src/test/java/com/clickhouse/kafka/connect/sink/processing/ProcessingRenamedTopicStateKeyUnitTest.java
git show cba0909b:src/test/java/com/clickhouse/kafka/connect/sink/OriginalTopicInsertedOffsetsUnitTest.java | shasum -a 256
```

```text
green_run_exit: 0
green_class_tests: 6
green_class_failures: 0
green_failed: none
processing_test: 14/0
renamed_state_key_test: 2/0
buffered_offset_test: 15/0
connect_api_refs_green: yes
planning_id_guard_green: yes
spotless_test_class: IS CLEAN
spotless_processing_import_only: yes
expected_added_rows: 6
expected_added_sha256: 13540b05e5790d60f4d4ce8deb09e8dbb26127f2a2fc3bdb76338bb85446a879
fork_delta_row_position: ok
item16_statements: ok
anchors_block_position: ok
anchors_mirror: rows=186 bad=0
code_commit: cba0909bc15fb83305e1b539c257b2e7b95842d9
code_commit_files: .planning/scripts/fork-anchors.txt; docs/FORK-DELTA.md; src/main/java/com/clickhouse/kafka/connect/sink/processing/Processing.java; src/test/java/com/clickhouse/kafka/connect/sink/OriginalTopicInsertedOffsetsUnitTest.java
committed_test_sha256_equals_red: yes
processing_numstat: 1/1
line_137_reads_original_topic: yes
state_key_line_count: 1
state_paths_diff_empty: yes
processing_test_unchanged: yes
sink_config_unchanged: yes
revert_unit: one commit (tests, code and record rows together): cba0909bc15fb83305e1b539c257b2e7b95842d9
```

Why the state key cannot move: the value read at line 137 is the sixth argument of every `StateRecord` that `doLogic` builds and is set on the record read back from the provider, but its only reader in `src/main` is `BaseStateProviderImpl.onStateUpdate`, which uses it as the topic half of the key of the in-memory `lastInsertedOffsets` map, while the state key `<transformed topic>-<partition>` comes from `TopicPartitionContainer` and the KeeperMap row `(key, minOffset, maxOffset, state)` never reads it. The trace guard `stateKeyAndFlagSequenceStayOnTheTransformedTopicUnderAnInjectiveRename` passed on both trees with the key `orig-0` and `BEFORE_PROCESSING` before `AFTER_PROCESSING` for every inserted range, and `sink/state` and `sink/kafka` have an empty diff against the phase start. The test file committed in the code commit has the SHA-256 recorded at the red run. `ProcessingTest` and `ProcessingRenamedTopicStateKeyUnitTest` are byte-unchanged.

2026-11-02T12:07:54Z

Verdict: pass

### E-04: Gate on the RSK-01 commit: testUnit with Docker unreachable and the full suite on latest x V2, status delta of exactly the added rows

Backs: RSK-01, RSK-04 (success criterion 4 for the RSK-01 commit: a cache-busted `testUnit` with Docker unreachable and a full suite on ClickHouse `latest` x client V2 on the tree of the code commit, totals equal to the E-01 baselines plus 6, and a per-testcase status delta of exactly the 6 added rows).

```sh
git ls-remote origin refs/heads/custom_build
git rev-parse HEAD
git diff --quiet cba0909b HEAD -- . ':!.planning'
docker run --rm --entrypoint clickhouse clickhouse/clickhouse-server:latest local --query 'SELECT version()'
docker image inspect --format '{{index .RepoDigests 0}}' clickhouse/clickhouse-server:latest
./gradlew --stop
rm -rf build/test-results/testUnit
DOCKER_HOST=unix:///nonexistent/docker.sock env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain testUnit -PjavaVersion=21 --rerun > /tmp/gsd42/exec/gate1-testunit.log 2>&1
cp -R build/test-results/testUnit /tmp/gsd42/exec/xml-gate1-testunit
python3 -I .planning/phases/42-isolated-high-risk-edits/42-testcase-status.py dump /tmp/gsd42/exec/xml-gate1-testunit /tmp/gsd42/exec/status-gate1-testunit.tsv
python3 -I .planning/phases/42-isolated-high-risk-edits/42-testcase-status.py delta /tmp/gsd42/exec/status-base-testunit.tsv /tmp/gsd42/exec/status-gate1-testunit.tsv .planning/phases/42-isolated-high-risk-edits/42-rsk01-expected-added.tsv
rm -rf build/test-results/test
./gradlew --stop
CLICKHOUSE_VERSION=latest CLIENT_VERSION=V2 env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain test -PjavaVersion=21 --rerun > /tmp/gsd42/exec/gate1-full.log 2>&1
cp -R build/test-results/test /tmp/gsd42/exec/xml-gate1-full
python3 -I .planning/phases/42-isolated-high-risk-edits/42-testcase-status.py dump /tmp/gsd42/exec/xml-gate1-full /tmp/gsd42/exec/status-gate1-full.tsv
python3 -I .planning/phases/42-isolated-high-risk-edits/42-evidence-check.py skips /tmp/gsd42/exec/xml-gate1-full
python3 -I .planning/phases/42-isolated-high-risk-edits/42-testcase-status.py delta /tmp/gsd42/exec/status-base-full.tsv /tmp/gsd42/exec/status-gate1-full.tsv .planning/phases/42-isolated-high-risk-edits/42-rsk01-expected-added.tsv --by-class
```

```text
gate_head: b21e0f15f056b4acfee9af81e60ac8989c3d1817
gated_tree_equals_code_commit: yes
jvm_line: -PjavaVersion=21; openjdk version 21.0.12.1 2026-08-18 (Homebrew build 21.0.12.1); no OpenJ9 launcher
testunit_exit: 0
testunit_tests: 1763
testunit_failures: 0
testunit_errors: 0
testunit_skipped: 2
testunit_delta_vs_baseline: 6
full_exit: 0
full_tests: 2361
full_failures: 0
full_errors: 0
full_skipped: 18
full_skips_helper: OK skips: named=7 state_mismatch_cases=11 unexpected=0
full_delta_vs_baseline: 6
new_class_testunit: 6/0
new_class_full: 6/0
processing_test_testunit: 14/0
processing_test_full: 14/0
status_delta_testunit: delta: removed=0 added=6 expected=6 match=yes
status_delta_full: delta: removed=0 added=6 expected=6 match=yes
clickhouse_version_before: 26.10.3.7
clickhouse_version_after: 26.10.3.7
clickhouse_digest: clickhouse/clickhouse-server@sha256:739e099abd1157390960a59e4532d21781d8fde57e6e4151cb920ea02c30b1ce
jvm_crash: none
origin_reread: equal
```

The gated head descends from the RSK-01 code commit `cba0909b` and its non-planning tree is equal to it. Both runs exited 0 with 0 failures and 0 errors: `testUnit` with Docker unreachable ran 1763 tests (the E-01 baseline 1757 plus 6, 2 skipped) and the full suite on `latest` x V2 ran 2361 tests (2355 plus 6, the 18 named skips). Both status deltas against the E-01 dumps removed nothing and added exactly the six `OriginalTopicInsertedOffsetsUnitTest` rows, so every pre-existing testcase kept its status. The ClickHouse version and the image digest equal E-01's and read the same before and after the full run. No crash file appeared in the repository root, and `origin/custom_build` still equals the E-01 value.

2026-11-02T12:11:49Z

Verdict: pass

### E-05: RSK-02 expected changed rows written down before the change, and the red run of the pass-through pins

Backs: RSK-02 (success criterion 2: the set of classification rows the chain pass-through changes is written down and committed before `Utils.java` is touched; the Docker-free `RetriableExceptionPassThroughUnitTest` is run red on the unchanged tree together with every existing classification pin).

```sh
test -f /tmp/gsd42/exec/status-gate1-testunit.tsv && test -f /tmp/gsd42/exec/status-gate1-full.tsv
wc -l /tmp/gsd42/exec/status-gate1-testunit.tsv /tmp/gsd42/exec/status-gate1-full.tsv
env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain -q spotlessApply -PspotlessIdeHook="$(pwd)/src/test/java/com/clickhouse/kafka/connect/sink/RetriableExceptionPassThroughUnitTest.java" -PspotlessIdeHookUseStdOut > /tmp/gsd42/exec/sc-r2.out 2> /tmp/gsd42/exec/sc-r2.err
git diff --quiet HEAD -- src/main
shasum -a 256 src/test/java/com/clickhouse/kafka/connect/sink/RetriableExceptionPassThroughUnitTest.java
rm -rf build/test-results/testUnit
DOCKER_HOST=unix:///nonexistent/docker.sock env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain testUnit -PjavaVersion=21 --rerun --tests '*RetriableExceptionPassThroughUnitTest' --tests '*WriterFailureClassificationUnitTest' --tests '*WrappedDataExceptionClassificationPinUnitTest' --tests '*InsertStallBoundPinUnitTest' --tests '*AdditionalRetriableErrorCodesPinUnitTest' --tests '*.UtilsTest' --tests '*UtilsNullMessageClassificationUnitTest' --tests '*.ProcessingTest' > /tmp/gsd42/exec/rsk02-red.log 2>&1
cp -R build/test-results/testUnit /tmp/gsd42/exec/xml-rsk02-red
python3 -I .planning/phases/42-isolated-high-risk-edits/42-testcase-status.py dump /tmp/gsd42/exec/xml-rsk02-red /tmp/gsd42/exec/status-rsk02-red.tsv
python3 -I .planning/phases/42-isolated-high-risk-edits/42-evidence-check.py junit /tmp/gsd42/exec/xml-rsk02-red --class .RetriableExceptionPassThroughUnitTest tests=38 failures=30
python3 -I .planning/phases/42-isolated-high-risk-edits/42-evidence-check.py failed /tmp/gsd42/exec/xml-rsk02-red --expect 'RetriableExceptionPassThroughUnitTest#handleExceptionOutcome' 'RetriableExceptionPassThroughUnitTest#writerFailureOutcomeThroughPut'
python3 -I .planning/phases/42-isolated-high-risk-edits/42-testcase-status.py count /tmp/gsd42/exec/status-rsk02-red.tsv .RetriableExceptionPassThroughUnitTest FAIL --name-contains _CONTROL
python3 -I .planning/phases/42-isolated-high-risk-edits/42-testcase-status.py count /tmp/gsd42/exec/status-rsk02-red.tsv .RetriableExceptionPassThroughUnitTest PASS --name-lacks _CONTROL
python3 -I .planning/phases/42-isolated-high-risk-edits/42-testcase-status.py select .planning/phases/42-isolated-high-risk-edits/42-rsk02-expected-added.tsv PASS /tmp/gsd42/exec/status-rsk02-red.tsv .RetriableExceptionPassThroughUnitTest
shasum -a 256 .planning/phases/42-isolated-high-risk-edits/42-rsk02-expected-added.tsv
```

```text
before_dumps: from E-04
spotless_test_class: IS CLEAN
red_run_exit: 1
red_class_tests: 38
red_class_failures: 30
red_failed_set: RetriableExceptionPassThroughUnitTest#handleExceptionOutcome; RetriableExceptionPassThroughUnitTest#writerFailureOutcomeThroughPut
red_fail_controls: 0
red_pass_non_controls: 0
red_reason_assertion: yes
red_test_sha256: aaca2de46d0e17c3cc30789305f29b4ff6392eee08b6952ecba790ca96fdba4f
src_main_untouched: yes
matrix_tests: 216
matrix_failures: 0
pin02: 14/0
tmo02: 15/0
codes_pin: 19/0
utils_test: 34/0
utils_null_message: 6/0
processing_test: 14/0
matrix_label_184_note: the ROADMAP and REQUIREMENTS say 184 cases, the Phase 22 count with 21 failure kinds; Phase 27 (WRT-16, commit 108f79a5) added four Jackson kinds, so 25 x 8 + 8 + 8 = 216 executions; the roadmap text is not edited in this phase
expected_changed_matrix_rows: none
expected_changed_pin02_rows: none
expected_changed_tmo02_rows: none
expected_changed_existing_rows: none
expected_added_rows: 38
expected_added_sha256: 534089c3c7dc47fe10c4bdb5a8f24b8f58d59e17246306f9d0f824c783026848
```

The list is written down before the change. The before dumps are the E-04 gate dumps (`status-gate1-testunit.tsv` 1763 lines and `status-gate1-full.tsv` 2361 lines, both equal to the E-04 totals). `Utils.java` equals HEAD at this point (`git diff --quiet HEAD -- src/main` exits 0), so the red run is on the unchanged tree. The red run exited non-zero with the class XML present: 38 tests, 30 failures, every failure an `org.opentest4j.AssertionFailedError` failure element (no `error` element), none slower than 30 s. The failing methods are exactly `handleExceptionOutcome` (18 of 22 rows) and `writerFailureOutcomeThroughPut` (12 of 16 rows). The 8 `_CONTROL` rows pass on the unchanged tree and no non-control row passes. The failure messages show today's outcome, for example `ASYNC_INSERT_TIMEOUT through PROXY_PUT under errors.tolerance=none` expected `RETRIABLE` but was `FAILS_TASK`, and under `all` was `DLQ`. The existing classes of the path all pass in the same run at their measured counts: `WriterFailureClassificationUnitTest` 216, `WrappedDataExceptionClassificationPinUnitTest` 14, `InsertStallBoundPinUnitTest` 15, `AdditionalRetriableErrorCodesPinUnitTest` 19, `UtilsTest` 34, `UtilsNullMessageClassificationUnitTest` 6, `ProcessingTest` 14.

Existing classes and the rows expected to change in them:

| Existing class | Executions | Expected changed rows |
|----------------|-----------:|-----------------------|
| `WriterFailureClassificationUnitTest` (matrix, 25 kinds x 8 + 8 + 8) | 216 | none |
| `WrappedDataExceptionClassificationPinUnitTest` (PIN-02) | 14 | none |
| `InsertStallBoundPinUnitTest` (TMO-02) | 15 | none |
| `AdditionalRetriableErrorCodesPinUnitTest` | 19 | none |
| `UtilsTest` | 34 | none |
| `UtilsNullMessageClassificationUnitTest` | 6 | none |
| `ProcessingTest` | 14 | none |
| every other existing testcase (whole `testUnit` 1763, full suite 2361) | | none |

The added rows (38, all `PASS` after the change; the chain form chosen by the user):

| Path | Shape | Tolerance | Expected outcome after the change |
|------|-------|-----------|-----------------------------------|
| direct `Utils.handleException` | `BARE_RETRIABLE`, `BARE_RETRIABLE_OVER_TIMEOUT`, `BARE_RETRIABLE_OVER_NON_RETRIABLE_SERVER_CODE`, `RETRIABLE_FROM_FIRST_HANDLING`, `RETRIABLE_OVER_NUMBER_OF_RECORDS_WRAP` | none, all | `RETRIABLE_SAME` (the input instance is rethrown) |
| direct `Utils.handleException` | `WRAPPED_INSERT_TIMEOUT`, `WRAPPED_DDL_PROPAGATION_TIMEOUT`, `DATA_EXCEPTION_OVER_RETRIABLE`, `WRAPPED_RETRIABLE_OVER_NON_RETRIABLE_SERVER_CODE`, `WRAPPED_RETRIABLE_SERVER_CODE_CONTROL` | none, all | `RETRIABLE_WRAPPED` (one new `RetriableException` around the input) |
| direct `Utils.handleException` | `WRAPPED_ILLEGAL_STATE_CONTROL` | none | `RUNTIME_WRAP` (control, unchanged) |
| direct `Utils.handleException` | `WRAPPED_ILLEGAL_STATE_CONTROL` | all | `RETURNS` (control, unchanged) |
| `ProxySinkTask.put` and `ChunkFlusher.putDirect` | writer throws `ASYNC_INSERT_TIMEOUT`, `DDL_PROPAGATION_TIMEOUT`, `DATA_EXCEPTION_OVER_RETRIABLE` | none, all | `RETRIABLE`, no dead letter queue record |
| `ProxySinkTask.put` and `ChunkFlusher.putDirect` | writer throws `SOCKET_TIMEOUT_CONTROL` | none, all | `RETRIABLE`, no dead letter queue record (control, unchanged) |

The outcomes of `BARE_RETRIABLE_OVER_NON_RETRIABLE_SERVER_CODE`, `WRAPPED_RETRIABLE_OVER_NON_RETRIABLE_SERVER_CODE` and `DATA_EXCEPTION_OVER_RETRIABLE` are fixed by the user's chain-form choice; a change to any of them returns to the user.

2026-11-02T12:16:15Z

Verdict: pass

### E-06: RSK-02 code commit: the chain pass-through green with its pins, the FORK-DELTA row, the Known cost 17 sentence, two anchor rows

Backs: RSK-02 (the chain pass-through in `Utils.handleException` green together with its 38 pins and the existing classification classes, the FORK-DELTA Connector source row, the two sentences at the front of Known cost 17, the two anchor rows, and the code commit that holds them as one revert unit).

```sh
rm -rf build/test-results/testUnit
DOCKER_HOST=unix:///nonexistent/docker.sock env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain testUnit -PjavaVersion=21 --rerun --tests '*RetriableExceptionPassThroughUnitTest' --tests '*WriterFailureClassificationUnitTest' --tests '*WrappedDataExceptionClassificationPinUnitTest' --tests '*InsertStallBoundPinUnitTest' --tests '*AdditionalRetriableErrorCodesPinUnitTest' --tests '*.UtilsTest' --tests '*UtilsNullMessageClassificationUnitTest' --tests '*.ProcessingTest' --tests '*ConnectApiReferencesUnitTest' --tests '*SourcePlanningIdGuardUnitTest' > /tmp/gsd42/exec/rsk02-green.log 2>&1
cp -R build/test-results/testUnit /tmp/gsd42/exec/xml-rsk02-green
python3 -I .planning/phases/42-isolated-high-risk-edits/42-evidence-check.py junit /tmp/gsd42/exec/xml-rsk02-green --class .RetriableExceptionPassThroughUnitTest tests=38 failures=0 errors=0
python3 -I .planning/phases/42-isolated-high-risk-edits/42-evidence-check.py anchors .planning/scripts/fork-anchors.txt --rows 188
env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain -q spotlessApply -PspotlessIdeHook="$(pwd)/<file>" -PspotlessIdeHookUseStdOut > /tmp/gsd42/exec/sc-<name>.out 2> /tmp/gsd42/exec/sc-<name>.err
git diff --numstat
git diff -U0 -- src/main/java/com/clickhouse/kafka/connect/util/Utils.java
git add src/main/java/com/clickhouse/kafka/connect/util/Utils.java src/test/java/com/clickhouse/kafka/connect/sink/RetriableExceptionPassThroughUnitTest.java docs/FORK-DELTA.md .planning/scripts/fork-anchors.txt
git commit -F /tmp/gsd42/exec/msg-code2.txt
git diff --name-only c8574890^ c8574890
git diff --quiet b34d0280 c8574890 -- src/main/java/com/clickhouse/kafka/connect/sink/processing/Processing.java
git diff --quiet 1c808c02 c8574890 -- src/main/java/com/clickhouse/kafka/connect/sink/ClickHouseSinkConfig.java src/main/java/com/clickhouse/kafka/connect/sink/state src/main/java/com/clickhouse/kafka/connect/sink/kafka
git show c8574890:src/test/java/com/clickhouse/kafka/connect/sink/RetriableExceptionPassThroughUnitTest.java | shasum -a 256
```

```text
green_run_exit: 0
green_class_tests: 38
green_class_failures: 0
green_failed: none
matrix: 216/0
pin02: 14/0
tmo02: 15/0
codes_pin: 19/0
utils_test: 34/0
utils_null_message: 6/0
processing_test: 14/0
connect_api_refs_green: yes
planning_id_guard_green: yes
spotless_utils: IS CLEAN
spotless_test_class: IS CLEAN
utils_numstat: 7/0
utils_hunks: 1
utils_import_lines_changed: 0
utils_log_lines_added: 0
utils_block_position: ok
fork_delta_row_position: ok
item17_sentence: ok
anchors_block_position: ok
anchors_mirror: rows=188 bad=0
code_commit: c857489081e27133b74e1ebf9bb061d930624024
code_commit_files: .planning/scripts/fork-anchors.txt; docs/FORK-DELTA.md; src/main/java/com/clickhouse/kafka/connect/util/Utils.java; src/test/java/com/clickhouse/kafka/connect/sink/RetriableExceptionPassThroughUnitTest.java
committed_test_sha256_equals_red: yes
expected_list_commit: b34d02804a5cd2e69d9a80c4b0e10d788c25328b
expected_list_before_change: yes
processing_unchanged_since_rsk01: yes
sink_config_unchanged: yes
revert_unit: one commit (tests, code and record rows together): c857489081e27133b74e1ebf9bb061d930624024
```

The green run exited 0: the class is 38/0 with the test file byte-identical to the red run (the SHA-256 `aaca2de4...` of the committed blob equals `red_test_sha256` of E-05), and every existing classification class keeps its red-run count with 0 failures (216, 14, 15, 19, 34, 6, 14). `ConnectApiReferencesUnitTest` (3) and `SourcePlanningIdGuardUnitTest` (18) pass. The `Utils.java` diff is one hunk of 7 added lines and 0 removed (a comment, the `for` loop over the cause chain, the `if`, the `throw`, two braces and a blank line), with no import and no log line; it sits after the server block's `wasn't in the acceptable list.` line and directly before `//High-Level Explicit Exception Checking`. The expected-list commit `b34d0280` is a strict ancestor of the code commit and has `Utils.java` equal to the phase start. `Processing.java` is unchanged since the RSK-01 commit `cba0909b`, and `ClickHouseSinkConfig.java`, `sink/state` and `sink/kafka` equal the phase start. The FORK-DELTA row sits directly after the Four Single Message Transforms row and before the IOException-branch row, and states both consequences in the plan's exact words; Known cost 17 carries the two new sentences on its one physical line and still names FUT-05 and "no retry budget" (FUT-05 is not claimed closed). The two anchor rows and their comment sit between the TAL-03 row and the `# Phase 35` comment.

2026-11-02T12:18:19Z

Verdict: pass

### E-07: Gate on the RSK-02 commit and the actual classification diff against the list written before the change

Backs: RSK-02 (success criterion 2: the actual per-testcase status diff equals the list committed before the change; success criterion 4 for the RSK-02 commit: a cache-busted `testUnit` with Docker unreachable and a full suite on ClickHouse `latest` x client V2 on the tree of the code commit, totals equal to the E-04 totals plus 38).

```sh
git ls-remote origin refs/heads/custom_build
git rev-parse HEAD
git diff --quiet c8574890 HEAD -- . ':!.planning'
docker run --rm --entrypoint clickhouse clickhouse/clickhouse-server:latest local --query 'SELECT version()'
docker image inspect --format '{{index .RepoDigests 0}}' clickhouse/clickhouse-server:latest
./gradlew --stop
rm -rf build/test-results/testUnit
DOCKER_HOST=unix:///nonexistent/docker.sock env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain testUnit -PjavaVersion=21 --rerun > /tmp/gsd42/exec/gate2-testunit.log 2>&1
cp -R build/test-results/testUnit /tmp/gsd42/exec/xml-gate2-testunit
python3 -I .planning/phases/42-isolated-high-risk-edits/42-testcase-status.py dump /tmp/gsd42/exec/xml-gate2-testunit /tmp/gsd42/exec/status-gate2-testunit.tsv
python3 -I .planning/phases/42-isolated-high-risk-edits/42-testcase-status.py delta /tmp/gsd42/exec/status-gate1-testunit.tsv /tmp/gsd42/exec/status-gate2-testunit.tsv .planning/phases/42-isolated-high-risk-edits/42-rsk02-expected-added.tsv
rm -rf build/test-results/test
./gradlew --stop
CLICKHOUSE_VERSION=latest CLIENT_VERSION=V2 env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain test -PjavaVersion=21 --rerun > /tmp/gsd42/exec/gate2-full.log 2>&1
cp -R build/test-results/test /tmp/gsd42/exec/xml-gate2-full
python3 -I .planning/phases/42-isolated-high-risk-edits/42-testcase-status.py dump /tmp/gsd42/exec/xml-gate2-full /tmp/gsd42/exec/status-gate2-full.tsv
python3 -I .planning/phases/42-isolated-high-risk-edits/42-evidence-check.py skips /tmp/gsd42/exec/xml-gate2-full
python3 -I .planning/phases/42-isolated-high-risk-edits/42-testcase-status.py delta /tmp/gsd42/exec/status-gate1-full.tsv /tmp/gsd42/exec/status-gate2-full.tsv .planning/phases/42-isolated-high-risk-edits/42-rsk02-expected-added.tsv --by-class
```

```text
gate_head: 391742460349cf9424eb4d7e388078d629209d9d
gated_tree_equals_code_commit: yes
jvm_line: -PjavaVersion=21; openjdk version 21.0.12.1 2026-08-18 (Homebrew build 21.0.12.1); no OpenJ9 launcher
testunit_exit: 0
testunit_tests: 1801
testunit_failures: 0
testunit_errors: 0
testunit_skipped: 2
testunit_delta_vs_rsk01_gate: 38
full_exit: 0
full_tests: 2399
full_failures: 0
full_errors: 0
full_skipped: 18
full_skips_helper: OK skips: named=7 state_mismatch_cases=11 unexpected=0
full_delta_vs_rsk01_gate: 38
new_class_testunit: 38/0
new_class_full: 38/0
matrix_testunit: 216/0
matrix_full: 216/0
pin02_testunit: 14/0
tmo02_testunit: 15/0
status_delta_testunit: delta: removed=0 added=38 expected=38 match=yes
status_delta_full: delta: removed=0 added=38 expected=38 match=yes
actual_diff_equals_list: yes
clickhouse_version_before: 26.10.3.7
clickhouse_version_after: 26.10.3.7
clickhouse_digest: clickhouse/clickhouse-server@sha256:739e099abd1157390960a59e4532d21781d8fde57e6e4151cb920ea02c30b1ce
jvm_crash: none
origin_reread: equal
```

The gated head descends from the RSK-02 code commit `c8574890` and its non-planning tree is equal to it. Both runs exited 0 with 0 failures and 0 errors: `testUnit` with Docker unreachable ran 1801 tests (the E-04 total 1763 plus 38, 2 skipped) and the full suite on `latest` x V2 ran 2399 tests (2361 plus 38, the 18 named skips). Both status deltas against the E-04 dumps removed nothing and added exactly the 38 `RetriableExceptionPassThroughUnitTest` rows written down in E-05, so the actual diff equals the list: every pre-existing testcase, including all 216 `WriterFailureClassificationUnitTest` executions, the 14 PIN-02 rows and the 15 TMO-02 rows, kept its status. The ClickHouse version and the image digest equal E-01's and read the same before and after the full run. No crash file appeared in the repository root, and `origin/custom_build` still equals the E-01 value.

2026-11-02T12:22:14Z

Verdict: pass

### E-08: RSK-03 red runs on the unchanged tree: the three unvetted start-up cases and the live WARN count fail by assertion

Backs: RSK-03 (the Docker-free `UnvettedRetriableCodesWarnUnitTest` and the live `UnvettedRetriableCodesWarnLiveTest`, run before any production change: the three unvetted start-up cases fail because no WARN is logged, while the unset, the 15 built-in, 216 and `216,159,3` cases pass; the live case starts, inserts and reads back, then fails on its WARN-count assertion).

```sh
test -f /tmp/gsd42/exec/status-gate2-testunit.tsv && test -f /tmp/gsd42/exec/status-gate2-full.tsv
wc -l /tmp/gsd42/exec/status-gate2-testunit.tsv /tmp/gsd42/exec/status-gate2-full.tsv
env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain -q spotlessApply -PspotlessIdeHook="$(pwd)/src/test/java/com/clickhouse/kafka/connect/sink/UnvettedRetriableCodesWarnUnitTest.java" -PspotlessIdeHookUseStdOut > /tmp/gsd42/exec/sc-r3.out 2> /tmp/gsd42/exec/sc-r3.err
env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain -q spotlessApply -PspotlessIdeHook="$(pwd)/src/test/java/com/clickhouse/kafka/connect/sink/UnvettedRetriableCodesWarnLiveTest.java" -PspotlessIdeHookUseStdOut > /tmp/gsd42/exec/sc-r3l.out 2> /tmp/gsd42/exec/sc-r3l.err
rm -rf build/test-results/testUnit
DOCKER_HOST=unix:///nonexistent/docker.sock env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain testUnit -PjavaVersion=21 --rerun --tests '*UnvettedRetriableCodesWarnUnitTest' --tests '*AdditionalRetriableErrorCodesPinUnitTest' > /tmp/gsd42/exec/rsk03-red-unit.log 2>&1
cp -R build/test-results/testUnit /tmp/gsd42/exec/xml-rsk03-red-unit
python3 -I .planning/phases/42-isolated-high-risk-edits/42-evidence-check.py junit /tmp/gsd42/exec/xml-rsk03-red-unit --class .UnvettedRetriableCodesWarnUnitTest tests=21 failures=3
python3 -I .planning/phases/42-isolated-high-risk-edits/42-evidence-check.py failed /tmp/gsd42/exec/xml-rsk03-red-unit --expect 'UnvettedRetriableCodesWarnUnitTest#unvettedCodesLogOneWarnNamingThemAtStartup'
python3 -I .planning/phases/42-isolated-high-risk-edits/42-evidence-check.py junit /tmp/gsd42/exec/xml-rsk03-red-unit --class AdditionalRetriableErrorCodesPinUnitTest tests=19 failures=0 errors=0
docker run --rm --entrypoint clickhouse clickhouse/clickhouse-server:latest local --query 'SELECT version()'
rm -rf build/test-results/test
./gradlew --stop
CLICKHOUSE_VERSION=latest CLIENT_VERSION=V2 env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain test -PjavaVersion=21 --rerun --tests '*UnvettedRetriableCodesWarnLiveTest' > /tmp/gsd42/exec/rsk03-red-live.log 2>&1
cp -R build/test-results/test /tmp/gsd42/exec/xml-rsk03-red-live
python3 -I .planning/phases/42-isolated-high-risk-edits/42-evidence-check.py junit /tmp/gsd42/exec/xml-rsk03-red-live --class .UnvettedRetriableCodesWarnLiveTest tests=1 failures=1
shasum -a 256 src/test/java/com/clickhouse/kafka/connect/sink/UnvettedRetriableCodesWarnUnitTest.java src/test/java/com/clickhouse/kafka/connect/sink/UnvettedRetriableCodesWarnLiveTest.java
git status --porcelain -- src
```

```text
before_dumps: from E-07
spotless_unit_class: IS CLEAN
spotless_live_class: IS CLEAN
red_unit_run_exit: 1
red_unit_class_tests: 21
red_unit_class_failures: 3
red_unit_failed_set: UnvettedRetriableCodesWarnUnitTest#unvettedCodesLogOneWarnNamingThemAtStartup
red_unit_reason_assertion: yes
codes_pin: 19/0
red_live_run_exit: 1
red_live_class_tests: 1
red_live_class_failures: 1
red_live_reason_warn_count: yes
red_live_clickhouse_version: 26.10.3.7
red_unit_test_sha256: 09b19f333b6d6ad6cc05548bcdb8f1a6ed93df46c8f79c5123b4fe90d4730f89
red_live_test_sha256: 0ababbde25dbc6b4c7f820aa5cc44486b6911c5f0708ccca214c936417a87029
src_main_untouched: yes
```

The before dumps `status-gate2-testunit.tsv` (1801 lines) and `status-gate2-full.tsv` (2399 lines) already existed and their line counts equal E-07 `testunit_tests` and `full_tests`. The unit red run exited 1 with the class at 21 tests and 3 failures; the failing set is exactly the three unvetted rows of `unvettedCodesLogOneWarnNamingThemAtStartup` (`60`, `62, 60,159`, `2147483647`), each an `org.opentest4j.AssertionFailedError` failure element (no `error` element) and none over 30 s, so the unchanged tree logs no WARN for them. The unset case, the 15 built-in codes, `216` and `216,159,3` pass: for each, `start()` reached the connection step and threw `Connection to ClickHouse is not active.` as asserted. `AdditionalRetriableErrorCodesPinUnitTest` is 19/0 in the same run. The live red run exited 1 with the class at 1 test and 1 failure, an `AssertionFailedError` whose message starts `unvetted-code WARN count at start-up ==> expected: <1> but was: <0>`: start, put and read-back all passed and only the WARN is missing. No guard literal needed a correction before the red was recorded, and `git status --porcelain -- src` lists only the two untracked test classes.

2026-11-02T12:26:40Z

Verdict: pass

### E-09: RSK-03 code commit: the start-up WARN green with its pins, the FORK-DELTA row, Known cost 24, two anchor rows

Backs: RSK-03 (the start-up WARN in `ClickHouseSinkTask.start` green together with its 21 Docker-free pins, the one live pin and the existing `additionalRetriableErrorCodes` pins, the FORK-DELTA Connector source row, Known cost 24, the two anchor rows, the two expected-added lists and the code commit that holds them as one revert unit).

```sh
rm -rf build/test-results/testUnit
DOCKER_HOST=unix:///nonexistent/docker.sock env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain testUnit -PjavaVersion=21 --rerun --tests '*UnvettedRetriableCodesWarnUnitTest' --tests '*AdditionalRetriableErrorCodesPinUnitTest' --tests '*ConnectApiReferencesUnitTest' --tests '*SourcePlanningIdGuardUnitTest' > /tmp/gsd42/exec/rsk03-green-unit.log 2>&1
cp -R build/test-results/testUnit /tmp/gsd42/exec/xml-rsk03-green-unit
python3 -I .planning/phases/42-isolated-high-risk-edits/42-evidence-check.py junit /tmp/gsd42/exec/xml-rsk03-green-unit --class .UnvettedRetriableCodesWarnUnitTest tests=21 failures=0 errors=0
rm -rf build/test-results/test
./gradlew --stop
CLICKHOUSE_VERSION=latest CLIENT_VERSION=V2 env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain test -PjavaVersion=21 --rerun --tests '*UnvettedRetriableCodesWarnLiveTest' > /tmp/gsd42/exec/rsk03-green-live.log 2>&1
cp -R build/test-results/test /tmp/gsd42/exec/xml-rsk03-green-live
python3 -I .planning/phases/42-isolated-high-risk-edits/42-evidence-check.py junit /tmp/gsd42/exec/xml-rsk03-green-live --class .UnvettedRetriableCodesWarnLiveTest tests=1 failures=0 errors=0
python3 -I .planning/phases/42-isolated-high-risk-edits/42-testcase-status.py dump /tmp/gsd42/exec/xml-rsk03-red-unit /tmp/gsd42/exec/status-rsk03-red-unit.tsv
python3 -I .planning/phases/42-isolated-high-risk-edits/42-testcase-status.py dump /tmp/gsd42/exec/xml-rsk03-red-live /tmp/gsd42/exec/status-rsk03-red-live.tsv
python3 -I .planning/phases/42-isolated-high-risk-edits/42-testcase-status.py select .planning/phases/42-isolated-high-risk-edits/42-rsk03-expected-added-testunit.tsv PASS /tmp/gsd42/exec/status-rsk03-red-unit.tsv .UnvettedRetriableCodesWarnUnitTest
python3 -I .planning/phases/42-isolated-high-risk-edits/42-testcase-status.py select .planning/phases/42-isolated-high-risk-edits/42-rsk03-expected-added-full.tsv PASS /tmp/gsd42/exec/status-rsk03-red-unit.tsv .UnvettedRetriableCodesWarnUnitTest /tmp/gsd42/exec/status-rsk03-red-live.tsv .UnvettedRetriableCodesWarnLiveTest
python3 -I .planning/phases/42-isolated-high-risk-edits/42-evidence-check.py anchors .planning/scripts/fork-anchors.txt --rows 190
env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain -q spotlessApply -PspotlessIdeHook="$(pwd)/<file>" -PspotlessIdeHookUseStdOut > /tmp/gsd42/exec/sc-<name>.out 2> /tmp/gsd42/exec/sc-<name>.err
git diff --numstat -- src/main/java/com/clickhouse/kafka/connect/sink/ClickHouseSinkTask.java
git diff -U0 -- src/main/java/com/clickhouse/kafka/connect/sink/ClickHouseSinkTask.java
git add src/main/java/com/clickhouse/kafka/connect/sink/ClickHouseSinkTask.java src/test/java/com/clickhouse/kafka/connect/sink/UnvettedRetriableCodesWarnUnitTest.java src/test/java/com/clickhouse/kafka/connect/sink/UnvettedRetriableCodesWarnLiveTest.java docs/FORK-DELTA.md .planning/scripts/fork-anchors.txt
git commit -F /tmp/gsd42/exec/msg-code3.txt
git diff --name-only 4935e9c5^ 4935e9c5
git diff --quiet 1c808c02 4935e9c5 -- src/main/java/com/clickhouse/kafka/connect/sink/ClickHouseSinkConfig.java
git diff --quiet 1c808c02 4935e9c5 -- src/test/java/com/clickhouse/kafka/connect/sink/AdditionalRetriableErrorCodesPinUnitTest.java src/test/java/com/clickhouse/kafka/connect/sink/AdditionalRetriableErrorCodesTest.java
git diff --quiet c8574890 4935e9c5 -- src/main/java/com/clickhouse/kafka/connect/util/Utils.java src/main/java/com/clickhouse/kafka/connect/sink/processing/Processing.java
git show 4935e9c5:src/test/java/com/clickhouse/kafka/connect/sink/UnvettedRetriableCodesWarnUnitTest.java | shasum -a 256
git show 4935e9c5:src/test/java/com/clickhouse/kafka/connect/sink/UnvettedRetriableCodesWarnLiveTest.java | shasum -a 256
```

```text
green_unit_run_exit: 0
green_unit_class: 21/0
codes_pin: 19/0
connect_api_refs_green: yes
planning_id_guard_green: yes
green_live_run_exit: 0
green_live_class: 1/0
spotless_sink_task: IS CLEAN
spotless_unit_class: IS CLEAN
spotless_live_class: IS CLEAN
sink_task_numstat: 21/0
sink_task_import_lines_added: +import com.clickhouse.kafka.connect.util.Utils;; +import java.util.Set;; +import java.util.TreeSet;
call_line_position: ok
fork_delta_row_position: ok
item24_statements: ok
anchors_block_position: ok
anchors_mirror: rows=190 bad=0
expected_added_testunit_rows: 21
expected_added_testunit_sha256: dd7de679e90ec19326ec523c80ec87c30c0e6089d05d6d7f3865695431712a49
expected_added_full_rows: 22
expected_added_full_sha256: b3693c3d54e2071358e1073a437effd4eddb772b344b6dc45f62100c3c754a42
code_commit: 4935e9c50402283ec5ac8625415ad38eade5c762
code_commit_files: .planning/scripts/fork-anchors.txt; docs/FORK-DELTA.md; src/main/java/com/clickhouse/kafka/connect/sink/ClickHouseSinkTask.java; src/test/java/com/clickhouse/kafka/connect/sink/UnvettedRetriableCodesWarnLiveTest.java; src/test/java/com/clickhouse/kafka/connect/sink/UnvettedRetriableCodesWarnUnitTest.java
committed_tests_sha256_equal_red: yes
sink_config_unchanged: yes
codes_pins_unchanged: yes
utils_unchanged_since_rsk02: yes
revert_unit: one commit (tests, code and record rows together): 4935e9c50402283ec5ac8625415ad38eade5c762
```

The green unit run exited 0: the class is 21/0 with both test files byte-identical to the red runs (the committed blobs hash to `09b19f33...` and `0ababbde...`, equal to `red_unit_test_sha256` and `red_live_test_sha256` of E-08), the existing `additionalRetriableErrorCodes` pin class is 19/0, `ConnectApiReferencesUnitTest` (3) and `SourcePlanningIdGuardUnitTest` (18) pass. The live green run on ClickHouse `latest` x V2 exited 0 with the class at 1/0: the task started with code 60, inserted five records, they read back equal, and then the one WARN was asserted. The `ClickHouseSinkTask.java` diff is 21 added lines and 0 removed: three imports (`Utils`, `Set`, `TreeSet`), the comment and the two constants, the helper, and the call on the line directly after `validateBufferConfig(clickHouseSinkConfig);` and before the `ProxySinkTask` construction. `ClickHouseSinkConfig.java` and both existing codes pin classes equal the phase start, and `Utils.java` and `Processing.java` equal the RSK-02 commit `c8574890`. The FORK-DELTA row sits directly after the connector-name row and before the `onPartitionsRevoked` row; Known cost 24 sits between item 23 and `## Named skip inventory`, names the vetted set, 216, the judgment, FUT-05 still open, the owner and the re-open trigger, and Known cost 17 is untouched. The comment and two rows at the end of `fork-anchors.txt` follow the last TMO-01 row. The two expected-added files hold 21 and 22 `PASS` rows, written from the red-run status dumps.

2026-11-02T13:03:04Z

Verdict: pass
