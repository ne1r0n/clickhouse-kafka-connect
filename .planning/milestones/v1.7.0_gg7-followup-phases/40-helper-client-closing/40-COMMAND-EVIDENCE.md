# Phase 40: Command evidence

- **Base commit:** `phase_start_sha` = `52ce1cd4c1501f1c4a332f42c64295be43f0f53c` (HEAD when plan 40-01 started, branch `sync/v1.7.0`).
- **Shell:** macOS zsh, multi-line logic under `/bin/bash` or `python3 -I`. Gradle is run as `env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain ... -PjavaVersion=21`, so every run shares one HotSpot 21 JVM line and the OpenJ9 crash is avoided.
- **Docker:** colima profile `vz` (aarch64, 8 CPU, 16 GiB, 256 GiB disk), docker CLI from `/opt/homebrew/Cellar/docker/29.8.2/bin/docker`.
- **Date (UTC):** 2026-11-01.
- **Entry format:** each entry has a `### E-NN: {title}` heading, a `Backs:` line, an `sh` fence with the commands as run, a `text` fence of `key: value` lines (keys match `[a-z0-9_]+`, one fact per line, no key twice) with free text allowed after the fence, a UTC timestamp line, and a final line `Verdict: pass`, `Verdict: fail`, `Verdict: pending ({reason})` or `Verdict: unmeasured ({reason})`.
- **Rule:** a fail or pending verdict is fixed by re-measuring in a new run, never by editing the evidence.
- **Helper:** `40-evidence-check.py` in this directory is a byte copy of Phase 39's `39-evidence-check.py` (get, expect, index, rows, secrets, junit, anchors, skips, failed) and is never edited.
- **Red-reason rule:** a red counts only when every failing testcase is an `org.opentest4j.AssertionFailedError` whose message contains `close count`; a compile error, an unexpected exception or any other assertion is not a recorded red.
- **No push happens in Phase 40.** Reads use `git ls-remote` and unauthenticated GitHub REST GETs only.

## Index

| Entry | Requirement | Plan | Verdict |
|-------|-------------|------|---------|
| E-01 | CLS-04 | 40-01 | pass |
| E-02 | CLS-01, CLS-04 | 40-01 | pass |
| E-03 | CLS-01, CLS-02, CLS-04 | 40-01 | pass |
| E-04 | CLS-03, CLS-04 | 40-01 | pass |
| E-05 | CLS-01, CLS-04 | 40-02 | pass |
| E-06 | CLS-01, CLS-02, CLS-04 | 40-02 | pass |
| E-07 | CLS-01, CLS-02 | 40-02 | pass |
| E-08 | CLS-03, CLS-04 | 40-03 | pass |
| E-09 | CLS-03, CLS-04 | 40-03 | pass |
| E-10 | CLS-03 | 40-03 | pass |
| E-11 | CLS-01, CLS-02, CLS-03, CLS-04 | 40-03 | pass |

### E-01: Execution-start facts, baselines and environment

Backs: CLS-04 (execution-start facts: base commit, origin and upstream re-read, the Phase 39 baselines that Phase 40's totals build on, the evidence helper copy, the environment and the Spotless baselines of the five files this phase touches).

```sh
mkdir -p /tmp/gsd40/exec
git rev-parse HEAD
git rev-parse --abbrev-ref HEAD
git status --porcelain
git ls-remote origin refs/heads/custom_build
curl -sS --fail https://api.github.com/repos/ClickHouse/clickhouse-kafka-connect/pulls/861
curl -sS --fail https://api.github.com/repos/ClickHouse/clickhouse-kafka-connect/pulls/855
curl -sS --fail https://api.github.com/repos/ClickHouse/clickhouse-kafka-connect/pulls/857
python3 -I .planning/phases/39-dependabot-absorption-behaviour-pins/39-evidence-check.py get .planning/phases/39-dependabot-absorption-behaviour-pins/39-COMMAND-EVIDENCE.md E-10 testunit_tests
python3 -I .planning/phases/39-dependabot-absorption-behaviour-pins/39-evidence-check.py get .planning/phases/39-dependabot-absorption-behaviour-pins/39-COMMAND-EVIDENCE.md E-10 full_tests
git diff --quiet bcf4276365c6a12017be27e8a613d545ebf796eb HEAD -- src/main src/test src/testFixtures build.gradle.kts gradle
cp .planning/phases/39-dependabot-absorption-behaviour-pins/39-evidence-check.py .planning/phases/40-helper-client-closing/40-evidence-check.py
cmp .planning/phases/39-dependabot-absorption-behaviour-pins/39-evidence-check.py .planning/phases/40-helper-client-closing/40-evidence-check.py
python3 -I .planning/phases/40-helper-client-closing/40-evidence-check.py anchors .planning/scripts/fork-anchors.txt --rows 172
colima list
ls -d /opt/homebrew/Cellar/docker/*/bin/docker
/usr/libexec/java_home -v 21
env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain -q spotlessApply -PspotlessIdeHook="$(pwd)/<file>" -PspotlessIdeHookUseStdOut > /tmp/gsd40/exec/sc-base-<name>.out 2> /tmp/gsd40/exec/sc-base-<name>.err
```

```text
phase_start_sha: 52ce1cd4c1501f1c4a332f42c64295be43f0f53c
branch: sync/v1.7.0
dirty_paths: M .planning/config.json; M .planning/state.json
origin_custom_build: 72e8ccffe05ff6dffb14098d0b8c0f67aaa6a236
origin_custom_build_expected: 72e8ccffe05ff6dffb14098d0b8c0f67aaa6a236
upstream_pr_861: open merged_at=None head=3d4cfdf27181
upstream_pr_855: open merged_at=None head=ddabf28b9827
upstream_pr_857: open merged_at=None head=11d88bc0a552
baseline_source: 39-COMMAND-EVIDENCE.md E-10
baseline_testunit_tests: 1687
baseline_full_tests: 2285
baseline_inputs_unchanged: 0
anchors_mirror: rows=172 bad=0
helper_copy: cmp-equal
colima_vz: Running
docker_cli: /opt/homebrew/Cellar/docker/29.8.2/bin/docker
jdk21_home: /opt/homebrew/Cellar/openjdk@21/21.0.12.1/libexec/openjdk.jdk/Contents/Home
testcontainers_host_override: set
spotless_baseline_clickhouse_writer: IS DIRTY
spotless_baseline_keeper_state_provider: IS DIRTY
spotless_baseline_state_provider: IS DIRTY
spotless_baseline_proxy_sink_task: IS CLEAN
spotless_baseline_counting_helper: IS CLEAN
```

Both dirty paths sit under `.planning/` and are orchestrator-managed, so the dirty-path rule holds. `origin/custom_build` equals the expected tip and all three upstream PRs (#861, #855, #857) are still open and unmerged, as at planning. `git diff --quiet` from Phase 39's `gate_head` `bcf42763` to HEAD over `src/main`, `src/test`, `src/testFixtures`, `build.gradle.kts` and `gradle` exits 0, so the Phase 39 totals 1687 (`testUnit`) and 2285 (full suite) are this phase's baseline. The Spotless verdicts equal the research measurement: the three production files are import-only dirty and are never rewritten by the hook, the other two are clean. `testcontainers_host_override` is recorded as set only; this shell sets it to a literal that breaks Ryuk, which is why every Gradle run unsets it.

2026-11-01T21:59:36Z

Verdict: pass

### E-02: Tracer: counting seam and writer factory carry one writer case, red on V1 and V2

Backs: CLS-04 (the Mockito-free counting seam, the writer's behaviour-neutral `createHelperClient` factory and one writer case run end to end with Docker unreachable; the first red of the matrix, recorded before any fix exists) and CLS-01 (the writer's helper client is never closed on the unchanged lifecycle).

```sh
git diff -U0 -- src/main/java/com/clickhouse/kafka/connect/sink/db/ClickHouseWriter.java
git diff -U0 -- src/main/java | grep '^[-+]import'
env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain -q spotlessApply -PspotlessIdeHook="$(pwd)/src/test/java/com/clickhouse/kafka/connect/sink/db/CountingHelperClient.java" -PspotlessIdeHookUseStdOut
env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain -q spotlessApply -PspotlessIdeHook="$(pwd)/src/test/java/com/clickhouse/kafka/connect/sink/db/helper/WriterHelperClientClosingUnitTest.java" -PspotlessIdeHookUseStdOut
env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain -q spotlessApply -PspotlessIdeHook="$(pwd)/src/main/java/com/clickhouse/kafka/connect/sink/db/ClickHouseWriter.java" -PspotlessIdeHookUseStdOut
./gradlew --stop
rm -rf build/test-results/testUnit
DOCKER_HOST=unix:///nonexistent/docker.sock env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain testUnit -PjavaVersion=21 --rerun --tests '*WriterHelperClientClosingUnitTest' --tests '*WriterFailureClassificationUnitTest' --tests '*ValueFreeDlqHeadersUnitTest' --tests '*ClickHouseWriterNullMessageUnitTest' --tests '*WriterValueFreeFailureUnitTest' > /tmp/gsd40/exec/tracer.log 2>&1
cp -R build/test-results/testUnit /tmp/gsd40/exec/xml-tracer
python3 -I .planning/phases/40-helper-client-closing/40-evidence-check.py junit /tmp/gsd40/exec/xml-tracer --class WriterHelperClientClosingUnitTest tests=2 failures=2
python3 -I .planning/phases/40-helper-client-closing/40-evidence-check.py failed /tmp/gsd40/exec/xml-tracer --expect 'WriterHelperClientClosingUnitTest#successfulStartThenStopClosesTheHelperClientOnce'
```

```text
seam_removed_lines: 6
seam_import_lines_changed: 0
spotless_counting_helper: IS CLEAN
spotless_test_class: IS CLEAN
spotless_clickhouse_writer_diff_import_only: yes
tracer_run_exit: 1
tracer_class_tests: 2
tracer_class_failures: 2
red_failed_set: WriterHelperClientClosingUnitTest#successfulStartThenStopClosesTheHelperClientOnce
red_reason_close_count: yes
counting_users_green: yes
compile_exit: 0
```

The run exited non-zero (`254 tests, 252 succeeded, 2 failed`) and produced the class XML, which proves the sources compiled. The two failures are the tracer case on client V1 and on client V2, both `org.opentest4j.AssertionFailedError` with a message beginning `close count of the writer's helper client`; the other 252 tests, which include the four existing `CountingHelperClient` users (`WriterFailureClassificationUnitTest`, `ValueFreeDlqHeadersUnitTest`, `ClickHouseWriterNullMessageUnitTest`, `WriterValueFreeFailureUnitTest`), pass. The `ClickHouseWriter.java` seam removes exactly six lines (the two builder starts, the two compression lines and the two `.build();` lines) and changes no import line; the hook's stdout differs from the file in the same 21 import and blank lines as at the baseline, with equal import sets.

2026-11-01T22:00:05Z

Verdict: pass

### E-03: Writer red matrix (22 cases) and the writer red tree saved as a patch

Backs: CLS-04 (every writer-site case run red on the unchanged lifecycle, success, double stop, each of the six failed-start exits, the executor order and the V1 per-batch helper, with the two controls green), CLS-01 and CLS-02 (the writer's helper and the V1 per-batch helper are never closed today).

```sh
git diff -U0 -- src/main/java/com/clickhouse/kafka/connect/sink/state/provider/KeeperStateProvider.java
git diff -U0 -- src/main/java/com/clickhouse/kafka/connect/sink/state/StateProvider.java
env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain -q spotlessApply -PspotlessIdeHook="$(pwd)/src/test/java/com/clickhouse/kafka/connect/sink/db/helper/WriterHelperClientClosingUnitTest.java" -PspotlessIdeHookUseStdOut
env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain -q spotlessApply -PspotlessIdeHook="$(pwd)/src/main/java/com/clickhouse/kafka/connect/sink/state/StateProvider.java" -PspotlessIdeHookUseStdOut
env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain -q spotlessApply -PspotlessIdeHook="$(pwd)/src/main/java/com/clickhouse/kafka/connect/sink/state/provider/KeeperStateProvider.java" -PspotlessIdeHookUseStdOut
./gradlew --stop
rm -rf build/test-results/testUnit
DOCKER_HOST=unix:///nonexistent/docker.sock env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain testUnit -PjavaVersion=21 --rerun --tests '*WriterHelperClientClosingUnitTest' > /tmp/gsd40/exec/writer-red.log 2>&1
cp -R build/test-results/testUnit /tmp/gsd40/exec/xml-writer-red
python3 -I .planning/phases/40-helper-client-closing/40-evidence-check.py junit /tmp/gsd40/exec/xml-writer-red --class WriterHelperClientClosingUnitTest tests=22 failures=19
python3 -I .planning/phases/40-helper-client-closing/40-evidence-check.py failed /tmp/gsd40/exec/xml-writer-red --expect WriterHelperClientClosingUnitTest#successfulStartThenStopClosesTheHelperClientOnce WriterHelperClientClosingUnitTest#secondStopNeitherThrowsNorClosesAgain WriterHelperClientClosingUnitTest#failedPingClosesTheHelperClientAndALaterStopClosesNothingMore WriterHelperClientClosingUnitTest#unreadableServerVersionClosesTheHelperClient WriterHelperClientClosingUnitTest#serverOlderThanTheMinimumVersionClosesTheHelperClient WriterHelperClientClosingUnitTest#versionLookupFailureClosesTheHelperClient WriterHelperClientClosingUnitTest#noTablesWithoutDbTopicSplitClosesTheHelperClient WriterHelperClientClosingUnitTest#tableMappingFailureClosesTheHelperClientAndPropagatesTheSameException WriterHelperClientClosingUnitTest#stopClosesTheHelperClientOnlyAfterTheTableSyncExecutorTerminated WriterHelperClientClosingUnitTest#clientV1BatchesCloseTheirPerBatchHelperClientAndTheNodeStillServesTheInsert
git add -N src/test/java/com/clickhouse/kafka/connect/sink/db/helper/WriterHelperClientClosingUnitTest.java
git diff --no-color --no-ext-diff -- src/main/java/com/clickhouse/kafka/connect/sink/db/ClickHouseWriter.java src/test/java/com/clickhouse/kafka/connect/sink/db/CountingHelperClient.java src/test/java/com/clickhouse/kafka/connect/sink/db/helper/WriterHelperClientClosingUnitTest.java > .planning/phases/40-helper-client-closing/40-red-writer.patch
git reset -q -- src/test/java/com/clickhouse/kafka/connect/sink/db/helper/WriterHelperClientClosingUnitTest.java
shasum -a 256 .planning/phases/40-helper-client-closing/40-red-writer.patch
```

```text
writer_red_run_exit: 1
class_tests: 22
class_failures: 19
red_failed_count: 10
red_failed_set: WriterHelperClientClosingUnitTest#successfulStartThenStopClosesTheHelperClientOnce; WriterHelperClientClosingUnitTest#secondStopNeitherThrowsNorClosesAgain; WriterHelperClientClosingUnitTest#failedPingClosesTheHelperClientAndALaterStopClosesNothingMore; WriterHelperClientClosingUnitTest#unreadableServerVersionClosesTheHelperClient; WriterHelperClientClosingUnitTest#serverOlderThanTheMinimumVersionClosesTheHelperClient; WriterHelperClientClosingUnitTest#versionLookupFailureClosesTheHelperClient; WriterHelperClientClosingUnitTest#noTablesWithoutDbTopicSplitClosesTheHelperClient; WriterHelperClientClosingUnitTest#tableMappingFailureClosesTheHelperClientAndPropagatesTheSameException; WriterHelperClientClosingUnitTest#stopClosesTheHelperClientOnlyAfterTheTableSyncExecutorTerminated; WriterHelperClientClosingUnitTest#clientV1BatchesCloseTheirPerBatchHelperClientAndTheNodeStillServesTheInsert
red_reason_close_count: yes
controls_passed: stopWithoutStartCreatesNoHelperClient; clientV2BatchBuildsNoPerBatchHelperClient
keeper_seam_import_lines: +import java.util.function.Function;
state_provider_import_lines_changed: 0
spotless_test_class: IS CLEAN
spotless_state_provider_diff_import_only: yes
spotless_keeper_state_provider_diff_import_only: yes
writer_patch_paths: src/main/java/com/clickhouse/kafka/connect/sink/db/ClickHouseWriter.java; src/test/java/com/clickhouse/kafka/connect/sink/db/CountingHelperClient.java; src/test/java/com/clickhouse/kafka/connect/sink/db/helper/WriterHelperClientClosingUnitTest.java
writer_patch_sha256: e1aa8499cc63f2e937325115b5bd7d615e50d0e93a80889b29f7c82e98581419
```

The run exited non-zero (`22 tests, 3 succeeded, 19 failed`) and produced the class XML, so the sources compiled. The 19 failing cases are the ten named methods (nine run on both client versions, the V1 batch case once); each failure is an `AssertionFailedError` whose message begins `close count of the writer's helper client` (nine methods) or `close count of a per-batch helper client` (the V1 batch case, which first passes its open-count assertion of 3). The three passing cases are the two controls: `stopWithoutStartCreatesNoHelperClient` on V1 and V2, and `clientV2BatchBuildsNoPerBatchHelperClient`. The Keeper seams are behaviour-neutral and sit in the tree but outside this patch: `StateProvider.java` gains a default no-op `close()` and no import change, `KeeperStateProvider.java` gains a package-private factory constructor and exactly the import `java.util.function.Function`; neither contains `ownedClient`, and `ClickHouseWriter.java` contains no `closeHelperClient()`.

A first run of this class (attempt 1, log `/tmp/gsd40/exec/writer-red-attempt1.log`) was not recorded: 20 cases failed because the control `clientV2BatchBuildsNoPerBatchHelperClient` threw `InstanceAlreadyExistsException` from the topic MBean that the V1 batch case's writer statistics had left registered under the same topic. That is a test-isolation defect in the new class itself (the plan requires MBeans an unchanged tree leaves registered to be unregistered in `finally`), not a behaviour of the unchanged tree: the test class now unregisters its writer statistics in the `finally` of both batch cases, and the run recorded here is a fresh run on the corrected class. No production line and no assertion about closing changed between the two runs.

2026-11-01T22:03:23Z

Verdict: pass

### E-04: Keeper red matrix (7 cases), the Keeper red tree saved as a patch, src restored

Backs: CLS-04 (the Keeper site and `ProxySinkTask.stop` run red on the unchanged lifecycle, on client V1 and V2, with the injected-client tripwire green) and CLS-03 (the state provider's own helper client and the state provider are never closed on `stop()` today).

```sh
env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain -q spotlessApply -PspotlessIdeHook="$(pwd)/src/test/java/com/clickhouse/kafka/connect/sink/state/provider/KeeperStateProviderClosingUnitTest.java" -PspotlessIdeHookUseStdOut
env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain -q spotlessApply -PspotlessIdeHook="$(pwd)/src/test/java/com/clickhouse/kafka/connect/sink/ProxySinkTaskStopUnitTest.java" -PspotlessIdeHookUseStdOut
./gradlew --stop
rm -rf build/test-results/testUnit
DOCKER_HOST=unix:///nonexistent/docker.sock env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain testUnit -PjavaVersion=21 --rerun --tests '*KeeperStateProviderClosingUnitTest' --tests '*ProxySinkTaskStopUnitTest' > /tmp/gsd40/exec/keeper-red.log 2>&1
cp -R build/test-results/testUnit /tmp/gsd40/exec/xml-keeper-red
python3 -I .planning/phases/40-helper-client-closing/40-evidence-check.py junit /tmp/gsd40/exec/xml-keeper-red --class KeeperStateProviderClosingUnitTest tests=5 failures=4
python3 -I .planning/phases/40-helper-client-closing/40-evidence-check.py junit /tmp/gsd40/exec/xml-keeper-red --class ProxySinkTaskStopUnitTest tests=2 failures=2
python3 -I .planning/phases/40-helper-client-closing/40-evidence-check.py failed /tmp/gsd40/exec/xml-keeper-red --expect KeeperStateProviderClosingUnitTest#closeClosesTheHelperClientTheProviderCreated KeeperStateProviderClosingUnitTest#secondCloseDoesNotCloseAgain ProxySinkTaskStopUnitTest#writerStopFailureStillUnregistersTheMBeanAndClosesTheStateProvider ProxySinkTaskStopUnitTest#stopClosesTheStateProviderOnceAfterTheWriterAndTheMBean
git add -N src/test/java/com/clickhouse/kafka/connect/sink/state/provider/KeeperStateProviderClosingUnitTest.java src/test/java/com/clickhouse/kafka/connect/sink/ProxySinkTaskStopUnitTest.java src/test/java/com/clickhouse/kafka/connect/sink/db/helper/WriterHelperClientClosingUnitTest.java
git diff --no-color --no-ext-diff -- src/main/java/com/clickhouse/kafka/connect/sink/state/StateProvider.java src/main/java/com/clickhouse/kafka/connect/sink/state/provider/KeeperStateProvider.java src/test/java/com/clickhouse/kafka/connect/sink/state/provider/KeeperStateProviderClosingUnitTest.java src/test/java/com/clickhouse/kafka/connect/sink/ProxySinkTaskStopUnitTest.java > .planning/phases/40-helper-client-closing/40-red-keeper.patch
git diff --no-color --no-ext-diff -- src/main/java/com/clickhouse/kafka/connect/sink/db/ClickHouseWriter.java src/test/java/com/clickhouse/kafka/connect/sink/db/CountingHelperClient.java src/test/java/com/clickhouse/kafka/connect/sink/db/helper/WriterHelperClientClosingUnitTest.java > /tmp/gsd40/exec/writer-recheck.patch
shasum -a 256 .planning/phases/40-helper-client-closing/40-red-keeper.patch /tmp/gsd40/exec/writer-recheck.patch
git reset -q -- src/test/java/com/clickhouse/kafka/connect/sink/state/provider/KeeperStateProviderClosingUnitTest.java src/test/java/com/clickhouse/kafka/connect/sink/ProxySinkTaskStopUnitTest.java src/test/java/com/clickhouse/kafka/connect/sink/db/helper/WriterHelperClientClosingUnitTest.java
git checkout -- src/main/java/com/clickhouse/kafka/connect/sink/db/ClickHouseWriter.java src/test/java/com/clickhouse/kafka/connect/sink/db/CountingHelperClient.java src/main/java/com/clickhouse/kafka/connect/sink/state/StateProvider.java src/main/java/com/clickhouse/kafka/connect/sink/state/provider/KeeperStateProvider.java
rm src/test/java/com/clickhouse/kafka/connect/sink/db/helper/WriterHelperClientClosingUnitTest.java src/test/java/com/clickhouse/kafka/connect/sink/state/provider/KeeperStateProviderClosingUnitTest.java src/test/java/com/clickhouse/kafka/connect/sink/ProxySinkTaskStopUnitTest.java
git status --porcelain -- src
git diff --quiet HEAD -- src
git apply --check .planning/phases/40-helper-client-closing/40-red-writer.patch
git apply --check .planning/phases/40-helper-client-closing/40-red-keeper.patch
```

```text
keeper_red_run_exit: 1
keeper_class_tests: 5
keeper_class_failures: 4
proxy_class_tests: 2
proxy_class_failures: 2
keeper_red_failed_count: 4
red_failed_set: KeeperStateProviderClosingUnitTest#closeClosesTheHelperClientTheProviderCreated; KeeperStateProviderClosingUnitTest#secondCloseDoesNotCloseAgain; ProxySinkTaskStopUnitTest#writerStopFailureStillUnregistersTheMBeanAndClosesTheStateProvider; ProxySinkTaskStopUnitTest#stopClosesTheStateProviderOnceAfterTheWriterAndTheMBean
red_reason_close_count: yes
tripwire_passed: yes
spotless_keeper_test: IS CLEAN
spotless_proxy_stop_test: IS CLEAN
keeper_patch_paths: src/main/java/com/clickhouse/kafka/connect/sink/state/StateProvider.java; src/main/java/com/clickhouse/kafka/connect/sink/state/provider/KeeperStateProvider.java; src/test/java/com/clickhouse/kafka/connect/sink/state/provider/KeeperStateProviderClosingUnitTest.java; src/test/java/com/clickhouse/kafka/connect/sink/ProxySinkTaskStopUnitTest.java
keeper_patch_sha256: bdc2533ca2baef3434d779add6b13c00cd71e8d4d19a230eff904f1c2d19ea9c
writer_patch_unchanged: yes
tree_restored: yes
writer_patch_applies_on_head: yes
keeper_patch_applies_on_head: yes
```

The run exited non-zero (`7 tests, 1 succeeded, 6 failed`) and produced both class XMLs, so the sources compiled. The six failing cases are the two Keeper methods on client V1 and V2 (four cases) and the two `ProxySinkTask.stop` cases; each is an `AssertionFailedError` whose message begins `close count of the state provider`. The one passing case is the tripwire `injectedClientConstructorStillFailsBeforeAnyInstanceExistsTripwire`: the injected-client constructor throws `NullPointerException` before an instance exists (`init()` dereferences the null config) and the injected helper's close count stays 0, so the clause that an injected client stays open has no behavioural pin; plan 40-03 pins it by source identity plus this tripwire. The writer patch, regenerated with the three writer-side paths before `src` was restored, has the same SHA-256 as E-03. `src` was then restored: `git status --porcelain -- src` is empty, `git diff --quiet HEAD -- src` exits 0, the three new test classes are removed, and each patch passes `git apply --check` on this clean tree.

2026-11-01T22:05:13Z

Verdict: pass

### E-05: Writer red tree re-applied and re-confirmed; the stop slice turns three cases green

Backs: CLS-01 (stop() closes the writer's helper client once, after the table-sync executor, null-safe and idempotent) and CLS-04 (the writer red tree from 40-red-writer.patch re-confirmed red, then the thinnest fix that reaches every layer turns exactly the stop-path cases green while the failed-start and per-batch cases stay red for the right reason).

```sh
shasum -a 256 .planning/phases/40-helper-client-closing/40-red-writer.patch
git apply --check .planning/phases/40-helper-client-closing/40-red-writer.patch
git apply .planning/phases/40-helper-client-closing/40-red-writer.patch
./gradlew --stop
rm -rf build/test-results/testUnit
DOCKER_HOST=unix:///nonexistent/docker.sock env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain testUnit -PjavaVersion=21 --rerun --tests '*WriterHelperClientClosingUnitTest' > /tmp/gsd40/exec/writer-reconfirm.log 2>&1
cp -R build/test-results/testUnit /tmp/gsd40/exec/xml-writer-reconfirm
python3 -I .planning/phases/40-helper-client-closing/40-evidence-check.py junit /tmp/gsd40/exec/xml-writer-reconfirm --class WriterHelperClientClosingUnitTest tests=22 failures=19
python3 -I .planning/phases/40-helper-client-closing/40-evidence-check.py failed /tmp/gsd40/exec/xml-writer-reconfirm --expect <the ten E-03 names>
# edit ClickHouseWriter.java: HELPER_CLIENT_CLOSE_FAILURE_LOG, private void closeHelperClient(), closeHelperClient(); as the last statement of stop()
./gradlew --stop
rm -rf build/test-results/testUnit
DOCKER_HOST=unix:///nonexistent/docker.sock env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain testUnit -PjavaVersion=21 --rerun --tests '*WriterHelperClientClosingUnitTest' > /tmp/gsd40/exec/writer-intermediate.log 2>&1
cp -R build/test-results/testUnit /tmp/gsd40/exec/xml-writer-intermediate
python3 -I .planning/phases/40-helper-client-closing/40-evidence-check.py junit /tmp/gsd40/exec/xml-writer-intermediate --class WriterHelperClientClosingUnitTest tests=22 failures=13
python3 -I .planning/phases/40-helper-client-closing/40-evidence-check.py failed /tmp/gsd40/exec/xml-writer-intermediate --expect <the seven names below>
env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain -q spotlessApply -PspotlessIdeHook="$(pwd)/src/main/java/com/clickhouse/kafka/connect/sink/db/ClickHouseWriter.java" -PspotlessIdeHookUseStdOut
```

```text
writer_patch_sha256_matches: yes
writer_patch_applied: yes
reconfirm_run_exit: 1
reconfirm_failed_count: 10
reconfirm_matches_e03: yes
intermediate_run_exit: 1
intermediate_class_tests: 22
intermediate_class_failures: 13
intermediate_failed_count: 7
intermediate_failed_set: WriterHelperClientClosingUnitTest#failedPingClosesTheHelperClientAndALaterStopClosesNothingMore; WriterHelperClientClosingUnitTest#unreadableServerVersionClosesTheHelperClient; WriterHelperClientClosingUnitTest#serverOlderThanTheMinimumVersionClosesTheHelperClient; WriterHelperClientClosingUnitTest#versionLookupFailureClosesTheHelperClient; WriterHelperClientClosingUnitTest#noTablesWithoutDbTopicSplitClosesTheHelperClient; WriterHelperClientClosingUnitTest#tableMappingFailureClosesTheHelperClientAndPropagatesTheSameException; WriterHelperClientClosingUnitTest#clientV1BatchesCloseTheirPerBatchHelperClientAndTheNodeStillServesTheInsert
now_green: WriterHelperClientClosingUnitTest#successfulStartThenStopClosesTheHelperClientOnce; WriterHelperClientClosingUnitTest#secondStopNeitherThrowsNorClosesAgain; WriterHelperClientClosingUnitTest#stopClosesTheHelperClientOnlyAfterTheTableSyncExecutorTerminated
red_reason_close_count: yes
spotless_clickhouse_writer_diff_import_only: yes
```

The patch SHA-256 equals E-03's `e1aa8499...1419`, and `git apply` re-created exactly the three writer paths. The re-run reproduced E-03 (22 tests, 19 failing cases over the same ten methods, every failure an `AssertionFailedError` with `close count`). With `closeHelperClient()` appended to `stop()` the class shows 22 tests and 13 failures: the three stop-path methods (`successfulStartThenStopClosesTheHelperClientOnce`, `secondStopNeitherThrowsNorClosesAgain`, `stopClosesTheHelperClientOnlyAfterTheTableSyncExecutorTerminated`) are green on client V1 and V2, and the seven remaining methods (the five failed-start exits, the table-mapping exception and the V1 per-batch case) stay red, each an `AssertionFailedError` naming `close count`. The scoped Spotless hook's stdout for `ClickHouseWriter.java` differs from the file in the same 21 import and blank lines as at the phase baseline, with equal import sets, so the import-only rule holds and the file was not rewritten.

2026-11-01T22:08:40Z

Verdict: pass

### E-06: Writer-half commit: start wrapper, per-batch close, 22 cases and guards green, FORK-DELTA and anchors

Backs: CLS-01 (start() closes the client on every exit that does not start the writer, stop() closes it once), CLS-02 (the client V1 per-batch helper is closed in try-with-resources and the node still serves the insert) and CLS-04 (all 22 writer cases and the guards green with Docker unreachable); the FORK-DELTA rows and the four anchors rows land with the code.

```sh
./gradlew --stop
rm -rf build/test-results/testUnit
DOCKER_HOST=unix:///nonexistent/docker.sock env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain testUnit -PjavaVersion=21 --rerun --tests '*WriterHelperClientClosingUnitTest' --tests '*SourcePlanningIdGuardUnitTest' --tests '*ProxySinkTaskConstructorUnitTest' --tests '*ConnectApiReferencesUnitTest' --tests '*WriterFailureClassificationUnitTest' --tests '*ValueFreeDlqHeadersUnitTest' --tests '*ClickHouseWriterNullMessageUnitTest' --tests '*WriterValueFreeFailureUnitTest' > /tmp/gsd40/exec/writer-green.log 2>&1
cp -R build/test-results/testUnit /tmp/gsd40/exec/xml-writer-green
python3 -I .planning/phases/40-helper-client-closing/40-evidence-check.py junit /tmp/gsd40/exec/xml-writer-green --class WriterHelperClientClosingUnitTest tests=22 failures=0 errors=0 skipped=0
python3 -I .planning/phases/40-helper-client-closing/40-evidence-check.py failed /tmp/gsd40/exec/xml-writer-green
env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain -q spotlessApply -PspotlessIdeHook="$(pwd)/src/main/java/com/clickhouse/kafka/connect/sink/db/ClickHouseWriter.java" -PspotlessIdeHookUseStdOut
env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain -q spotlessApply -PspotlessIdeHook="$(pwd)/src/test/java/com/clickhouse/kafka/connect/sink/db/CountingHelperClient.java" -PspotlessIdeHookUseStdOut
env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain -q spotlessApply -PspotlessIdeHook="$(pwd)/src/test/java/com/clickhouse/kafka/connect/sink/db/helper/WriterHelperClientClosingUnitTest.java" -PspotlessIdeHookUseStdOut
git diff 52ce1cd4c1501f1c4a332f42c64295be43f0f53c -- src/main/java/com/clickhouse/kafka/connect/sink/db/ClickHouseWriter.java
grep -cF -e <each of the four anchored strings> src/main/java/com/clickhouse/kafka/connect/sink/db/ClickHouseWriter.java
python3 -I .planning/phases/40-helper-client-closing/40-evidence-check.py anchors .planning/scripts/fork-anchors.txt --rows 176
git add <the five files> && git commit -m "fix(40-02): close the writer's helper clients on stop, on a failed start and per client V1 batch (CLS-01, CLS-02, CLS-04)"
git diff --name-only 52ce1cd4c1501f1c4a332f42c64295be43f0f53c HEAD -- src/main
git diff --quiet 52ce1cd4c1501f1c4a332f42c64295be43f0f53c HEAD -- src/main/java/com/clickhouse/kafka/connect/sink/state src/main/java/com/clickhouse/kafka/connect/sink/ProxySinkTask.java
```

```text
green_run_exit: 0
class_tests: 22
class_failures: 0
guards_failed: none
spotless_clickhouse_writer: IS DIRTY
spotless_clickhouse_writer_import_lines_changed: 0
spotless_clickhouse_writer_diff_import_only: yes
spotless_counting_helper: IS CLEAN
spotless_test_class: IS CLEAN
documented_limit_stop_timeout: accepted; stop() waits at most 5 s for the table-sync executor and then closes chc, so a refresh still running can run on a closed client and fails into TableMappingRefresher.run's catch; recorded in FORK-DELTA Known cost 13; owner: a future phase if observed
anchor_counts: createHelperClient(=3; closeHelperClient()=3; try (ClickHouseHelperClient chcTmp = createHelperClient(=1; ClickHouseHelperClient chcTmp = new=0
anchors_mirror: rows=176 bad=0
fork_delta_writer_row: added
fork_delta_harness_row: updated
fork_delta_known_cost_13: writer half
chc_field_kept: yes
writer_commit: de3faf775b424e54a8625959cce9c9648cbd40ec
writer_commit_files: .planning/scripts/fork-anchors.txt; docs/FORK-DELTA.md; src/main/java/com/clickhouse/kafka/connect/sink/db/ClickHouseWriter.java; src/test/java/com/clickhouse/kafka/connect/sink/db/CountingHelperClient.java; src/test/java/com/clickhouse/kafka/connect/sink/db/helper/WriterHelperClientClosingUnitTest.java
src_main_files_vs_phase_start: src/main/java/com/clickhouse/kafka/connect/sink/db/ClickHouseWriter.java
keeper_files_untouched: yes
```

The green run exited 0: the writer class ran 22 tests with 0 failures, errors and skips, and the seven guard and `CountingHelperClient` classes (`SourcePlanningIdGuardUnitTest`, `ProxySinkTaskConstructorUnitTest`, `ConnectApiReferencesUnitTest`, `WriterFailureClassificationUnitTest`, `ValueFreeDlqHeadersUnitTest`, `ClickHouseWriterNullMessageUnitTest`, `WriterValueFreeFailureUnitTest`) report no failing testcase. The `ClickHouseWriter.java` diff against phase start changes no import line; the scoped hook still prints `IS DIRTY` for it, with the same 21 import and blank lines as at the baseline and equal import sets (the import-only rule), so the file was never rewritten. `start()` is now a wrapper over `startInternal` whose `finally` closes the client when the start did not return true; `getMutationRequest` reads the node inside `try (ClickHouseHelperClient chcTmp = createHelperClient(` and the old `ClickHouseHelperClient chcTmp = new` line is gone. All four anchors counts equal the planned ones; the anchors mirror reads `rows=176 bad=0` (172 existing rows unchanged plus four). The commit holds exactly the five planned files.

Documented limit (accepted by the user, Known cost 13 and threat T-40-10): `stop()` waits at most 5 s for the table-sync executor and then closes `chc` regardless. A refresh still running after a timed-out wait can run on a closed client and fails into `TableMappingRefresher.run`'s catch, not into the task. On the normal path the executor has terminated at the close, pinned by `stopClosesTheHelperClientOnlyAfterTheTableSyncExecutorTerminated`.

2026-11-01T22:10:35Z

Verdict: pass

### E-07: Gate A on the writer-half commit: testUnit with Docker unreachable and the full suite on latest x V2

Backs: CLS-01 and CLS-02 (success criterion 2: `testUnit` and a full suite pass on the writer-half commit before the Keeper half starts). Re-measured after a user-authorised, test-only deviation; the failed first attempt is kept below.

Deviation (recorded): attempt 1 was red on three existing `ClickHouseWriterTest` cases that used a writer after `start()` returned false on an empty database. The user chose to seed a table in the test helper rather than change production code: commit `fae1fcb1` edits only `ClickHouseWriterTest.runWithWriter` (it creates a uniquely named seed table through the test helper before `start()`, asserts that `start()` returned true, and drops the seed table in `finally`). No assertion of any existing case changed, no test was weakened or deleted, and the three cases still query a table that does not exist (or that the case creates itself) while another table is present. The plan's rule against editing existing tests was waived for this one fix only. The gated tree is therefore the writer-half commit plus `fae1fcb1`.

```sh
git rev-parse HEAD
git diff --name-only de3faf77 HEAD -- . ':!.planning'
env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain -q spotlessApply -PspotlessIdeHook="$(pwd)/src/test/java/com/clickhouse/kafka/connect/sink/db/ClickHouseWriterTest.java" -PspotlessIdeHookUseStdOut
CLICKHOUSE_VERSION=latest CLIENT_VERSION=V2 env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain test -PjavaVersion=21 --rerun --tests '*sink.db.ClickHouseWriterTest' > /tmp/gsd40/exec/cwt-fix.log 2>&1
./gradlew --stop
rm -rf build/test-results/testUnit
DOCKER_HOST=unix:///nonexistent/docker.sock env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain testUnit -PjavaVersion=21 --rerun > /tmp/gsd40/exec/gateA2-testunit.log 2>&1
cp -R build/test-results/testUnit /tmp/gsd40/exec/xml-gateA2-testunit
docker run --rm --entrypoint clickhouse clickhouse/clickhouse-server:latest local --query 'SELECT version()'
docker image inspect --format '{{index .RepoDigests 0}}' clickhouse/clickhouse-server:latest
rm -rf build/test-results/test
./gradlew --stop
CLICKHOUSE_VERSION=latest CLIENT_VERSION=V2 env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain test -PjavaVersion=21 --rerun > /tmp/gsd40/exec/gateA2-full.log 2>&1
cp -R build/test-results/test /tmp/gsd40/exec/xml-gateA2-full
python3 -I .planning/phases/40-helper-client-closing/40-evidence-check.py junit /tmp/gsd40/exec/xml-gateA2-full tests=2307 failures=0 errors=0 skipped=18
python3 -I .planning/phases/40-helper-client-closing/40-evidence-check.py skips /tmp/gsd40/exec/xml-gateA2-full
```

```text
origin_reread: equal
gate_head: fae1fcb17dc580436108aa50a994d0c5204fbbc5
gated_tree_equals_writer_commit: no
gated_tree_delta_vs_writer_commit: src/test/java/com/clickhouse/kafka/connect/sink/db/ClickHouseWriterTest.java only (commit fae1fcb1, test-only, no assertion changed)
keeper_half_absent: yes
test_fix_commit: fae1fcb17dc580436108aa50a994d0c5204fbbc5
test_fix_spotless: IS DIRTY, import-only (3 lines, same as before the edit), no import line changed
test_fix_class_run: 16 tests, 0 failures, 0 skipped
jvm_line: HotSpot 21 (OpenJDK 64-Bit Server VM, -PjavaVersion=21); no OpenJ9 launcher
testunit_exit: 0
testunit_tests: 1709
testunit_failures: 0
testunit_errors: 0
testunit_skipped: 2
testunit_delta_vs_baseline: 22
clickhouse_version_before: 26.10.3.7
clickhouse_digest: sha256:739e099abd1157390960a59e4532d21781d8fde57e6e4151cb920ea02c30b1ce
full_exit: 0
full_tests: 2307
full_failures: 0
full_errors: 0
full_skipped: 18
full_skips_helper: OK
full_delta_vs_baseline: 22
clickhouse_version_after: 26.10.3.7
jvm_crash: none
v1_live_cases_passed: yes
v1_live_cases_rationale: the three [1] V1 testcases are the client V1 invocations of the only three V1/V2-parameterized methods of ClickHouseWriterTest (@ParameterizedTest at lines 299, 403 and 524: testSchemaRefreshedAfterCode131, staleDescribeTableIsNotCachedAsCurrentSchema, doInsertSupportsSpecialCharactersInTableNames); the only other parameterized method (line 577) names its cases [1] json to [3] tsv, so no other testcase is named [1] V1
attempt_1_verdict: fail (three ClickHouseWriterTest cases, kept below)
```

Both runs are green on the writer-half commit plus the test-only fix: `testUnit` with Docker unreachable ran 1709 tests (1687 plus 22), 0 failures, 2 skipped; the full suite on ClickHouse `latest` (`26.10.3.7`, same digest before and after) with client V2 ran 2307 tests (2285 plus 22), 0 failures, 0 errors, 18 skipped (the named inventory, helper OK). The class XML of `ClickHouseWriterTest` has no failing or skipped case, with three passed `[1] V1` cases and the passed JSON comparison case, so every `getMutationRequest` call site was inserted through on client V1 against real ClickHouse and read back. Origin `custom_build` still equals E-01's tip and nothing was pushed.

#### Attempt 1 (superseded, kept; gate head 1659e9be): gate A on the writer-half commit

Backs: CLS-01 and CLS-02 (success criterion 2: `testUnit` and a full suite pass on the writer-half commit before the Keeper half starts). The testUnit half passed; the full-suite half is red on three existing `ClickHouseWriterTest` cases, so the gate fails.

```sh
git ls-remote origin refs/heads/custom_build
git rev-parse HEAD
git diff --quiet <E-06 writer_commit> HEAD -- . ':!.planning'
./gradlew --stop
rm -rf build/test-results/testUnit
DOCKER_HOST=unix:///nonexistent/docker.sock env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain testUnit -PjavaVersion=21 --rerun > /tmp/gsd40/exec/gateA-testunit.log 2>&1
cp -R build/test-results/testUnit /tmp/gsd40/exec/xml-gateA-testunit
docker run --rm --entrypoint clickhouse clickhouse/clickhouse-server:latest local --query 'SELECT version()'
docker image inspect --format '{{index .RepoDigests 0}}' clickhouse/clickhouse-server:latest
rm -rf build/test-results/test
./gradlew --stop
CLICKHOUSE_VERSION=latest CLIENT_VERSION=V2 env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain test -PjavaVersion=21 --rerun > /tmp/gsd40/exec/gateA-full.log 2>&1
cp -R build/test-results/test /tmp/gsd40/exec/xml-gateA-full
python3 -I .planning/phases/40-helper-client-closing/40-evidence-check.py failed /tmp/gsd40/exec/xml-gateA-full
python3 -I .planning/phases/40-helper-client-closing/40-evidence-check.py skips /tmp/gsd40/exec/xml-gateA-full
```

```txt
origin_reread: equal
gate_head: 1659e9bece45860ad73842074a499aec0ea53ef0
gated_tree_equals_writer_commit: yes
keeper_half_absent: yes
jvm_line: HotSpot 21 (OpenJDK 64-Bit Server VM, -PjavaVersion=21); no OpenJ9 launcher
testunit_exit: 0
testunit_tests: 1709
testunit_failures: 0
testunit_errors: 0
testunit_skipped: 2
testunit_delta_vs_baseline: 22
clickhouse_version_before: 26.10.3.7
clickhouse_digest: sha256:739e099abd1157390960a59e4532d21781d8fde57e6e4151cb920ea02c30b1ce
full_exit: 1
full_tests: 2307
full_failures: 3
full_errors: 0
full_skipped: 18
full_skips_helper: OK (named=7 state_mismatch_cases=11 unexpected=0)
full_delta_vs_baseline: 22
clickhouse_version_after: 26.10.3.7
jvm_crash: none
v1_live_cases_passed: no
full_failed_set: ClickHouseWriterTest#getTableReturnsNullWhenMissingAndSuppressionEnabled; ClickHouseWriterTest#getTableThrowsWhenMissingAndSuppressionDisabled; ClickHouseWriterTest#updateMapping
failure_touches_writer_lifecycle: yes
```

The testUnit run with Docker unreachable passed: 1709 tests (1687 baseline plus 22), 0 failures, 0 errors, 2 skipped, the gated head's non-planning tree equal to the writer-half commit, the Keeper files untouched, origin equal to E-01.

The full suite on ClickHouse `latest` (`26.10.3.7`, digest unchanged before and after) x client V2 ran 2307 tests (2285 baseline plus 22), 18 skipped (the named skips, helper OK), and 3 failed, all in `ClickHouseWriterTest`: `updateMapping` and `getTableReturnsNullWhenMissingAndSuppressionEnabled` fail with a `NullPointerException` at `ClickHouseWriter.updateMapping` (`this.chc` is null), and `getTableThrowsWhenMissingAndSuppressionDisabled` fails its `assertTrue(ex.getMessage().contains("does not exist"))` because the message is now that `NullPointerException`'s. The three `[1] V1` cases and the JSON comparison case of that class passed, but the class XML carries the three failures, so `v1_live_cases_passed` is recorded `no`.

Cause, read from the stack traces and the test log. The three failing cases call the private helper `runWithWriter`, which does `writer.start(config)`, ignores the returned boolean and then uses the writer (`updateMapping`, `getTable`). Each of them starts against a destination database that holds no tables (the test drops its own table first; the class log shows exactly three `Did not find any tables in destination` lines, one per failing case), so `start()` returns false on the "no tables without db-topic split" exit. On the previous code that exit left `chc` open and the test went on using it; the writer-half commit closes and nulls `chc` on that exit, as CLS-01 requires ("every exit of `start()` that does not start the writer ... closes the client it opened"), so the writer is unusable afterwards. In production this is correct: `ProxySinkTask.start` throws `Connection to ClickHouse is not active.` when `start()` returns false and never uses the writer again. The plan's research did not list `ClickHouseWriterTest` as a user of a writer after a failed `start()`, and the plan forbids changing existing assertions, so no test or production line was changed after the run. The gate fails (not adjusted to pass); plan 40-03 must not start until a decision is made on how the three cases should obtain a started writer.

2026-11-01T22:15:19Z

Attempt 1 verdict: fail

2026-11-02T06:35:09Z

Verdict: pass

### E-08: Keeper red tree re-applied and re-confirmed; the provider slice turns the Keeper cases green

Backs: CLS-03 (KeeperStateProvider.close() closes only the helper client its own constructor built) and CLS-04 (the Keeper red tree from 40-red-keeper.patch re-confirmed red on the plan-40-02 tip, then the thinnest fix through the provider layer turns exactly the two Keeper cases green on V1 and V2 while the two ProxySinkTask.stop cases stay red for the right reason).

```sh
shasum -a 256 .planning/phases/40-helper-client-closing/40-red-keeper.patch
git apply --check .planning/phases/40-helper-client-closing/40-red-keeper.patch
git apply .planning/phases/40-helper-client-closing/40-red-keeper.patch
./gradlew --stop
rm -rf build/test-results/testUnit
DOCKER_HOST=unix:///nonexistent/docker.sock env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain testUnit -PjavaVersion=21 --rerun --tests '*KeeperStateProviderClosingUnitTest' --tests '*ProxySinkTaskStopUnitTest' > /tmp/gsd40/exec/keeper-reconfirm.log 2>&1
cp -R build/test-results/testUnit /tmp/gsd40/exec/xml-keeper-reconfirm
python3 -I .planning/phases/40-helper-client-closing/40-evidence-check.py failed /tmp/gsd40/exec/xml-keeper-reconfirm --expect <the four E-04 names>
# edit KeeperStateProvider.java: ownedClient field, ownedClient = chc; in the factory constructor, @Override public void close()
./gradlew --stop
rm -rf build/test-results/testUnit
DOCKER_HOST=unix:///nonexistent/docker.sock env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain testUnit -PjavaVersion=21 --rerun --tests '*KeeperStateProviderClosingUnitTest' --tests '*ProxySinkTaskStopUnitTest' > /tmp/gsd40/exec/keeper-intermediate.log 2>&1
cp -R build/test-results/testUnit /tmp/gsd40/exec/xml-keeper-intermediate
python3 -I .planning/phases/40-helper-client-closing/40-evidence-check.py junit /tmp/gsd40/exec/xml-keeper-intermediate --class KeeperStateProviderClosingUnitTest tests=5 failures=0 errors=0
python3 -I .planning/phases/40-helper-client-closing/40-evidence-check.py junit /tmp/gsd40/exec/xml-keeper-intermediate --class ProxySinkTaskStopUnitTest tests=2 failures=2
python3 -I .planning/phases/40-helper-client-closing/40-evidence-check.py failed /tmp/gsd40/exec/xml-keeper-intermediate --expect <the two stop names below>
env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain -q spotlessApply -PspotlessIdeHook="$(pwd)/src/main/java/com/clickhouse/kafka/connect/sink/state/provider/KeeperStateProvider.java" -PspotlessIdeHookUseStdOut
```

```text
keeper_patch_sha256_matches: yes
keeper_patch_applied: yes
reconfirm_run_exit: 1
reconfirm_failed_count: 4
reconfirm_matches_e04: yes
intermediate_run_exit: 1
intermediate_keeper_failures: 0
intermediate_proxy_failures: 2
intermediate_failed_count: 2
intermediate_failed_set: ProxySinkTaskStopUnitTest#writerStopFailureStillUnregistersTheMBeanAndClosesTheStateProvider; ProxySinkTaskStopUnitTest#stopClosesTheStateProviderOnceAfterTheWriterAndTheMBean
red_reason_close_count: yes
```

The patch SHA-256 equals E-04's `bdc2533c...ea9c` and `git apply` re-created exactly the four Keeper paths on the plan-40-02 tip (HEAD `e8e0318b`, which contains the writer-half commit `de3faf77` and the test-only seed `fae1fcb1`; the patch applied cleanly, so no regeneration was needed). The re-run reproduced E-04: 7 tests, 4 failing cases over the same four methods, the tripwire `injectedClientConstructorStillFailsBeforeAnyInstanceExistsTripwire` passing, every failure an `AssertionFailedError` with `close count`. With `ownedClient` (four occurrences), `ownedClient = chc;` in the factory constructor only and `close()` between `init()` and `getStateRecord`, `KeeperStateProviderClosingUnitTest` shows 5 tests and 0 failures on V1 and V2, and both `ProxySinkTaskStopUnitTest` cases stay red on `close count` because `ProxySinkTask.stop()` does not call the provider yet. The scoped Spotless hook's stdout for `KeeperStateProvider.java` differs from the file in the same 5 import and blank lines as at the phase baseline, with equal import sets, and the file's only import change against HEAD is `+import java.util.function.Function;`; the file was not rewritten.

2026-11-02T06:38:18Z

Verdict: pass

### E-09: Keeper-half commit: provider close, nested stop, 7 cases and guards green, state paths byte-identical, FORK-DELTA and anchors

Backs: CLS-03 (StateProvider default close, KeeperStateProvider.close() for the client it built, ProxySinkTask.stop() in nested try/finally, the state read and write paths byte-identical) and CLS-04 (all 7 Keeper-side cases and the guards green with Docker unreachable, with the 22 writer cases); the FORK-DELTA rows and the four anchors rows land with the code.

```sh
# edit ProxySinkTask.java: stop() as try { dbWriter.stop(); } finally { try { statistics.unregisterMBean(); } finally { stateProvider.close(); } }
./gradlew --stop
rm -rf build/test-results/testUnit
DOCKER_HOST=unix:///nonexistent/docker.sock env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain testUnit -PjavaVersion=21 --rerun --tests '*KeeperStateProviderClosingUnitTest' --tests '*ProxySinkTaskStopUnitTest' --tests '*ProxySinkTaskConstructorUnitTest' --tests '*SourcePlanningIdGuardUnitTest' --tests '*ConnectApiReferencesUnitTest' --tests '*WrappedDataExceptionClassificationPinUnitTest' --tests '*WriterHelperClientClosingUnitTest' > /tmp/gsd40/exec/keeper-green.log 2>&1
cp -R build/test-results/testUnit /tmp/gsd40/exec/xml-keeper-green
python3 -I .planning/phases/40-helper-client-closing/40-evidence-check.py junit /tmp/gsd40/exec/xml-keeper-green --class KeeperStateProviderClosingUnitTest tests=5 failures=0 errors=0 skipped=0
python3 -I .planning/phases/40-helper-client-closing/40-evidence-check.py junit /tmp/gsd40/exec/xml-keeper-green --class ProxySinkTaskStopUnitTest tests=2 failures=0 errors=0 skipped=0
python3 -I .planning/phases/40-helper-client-closing/40-evidence-check.py junit /tmp/gsd40/exec/xml-keeper-green --class WriterHelperClientClosingUnitTest tests=22 failures=0
python3 -I .planning/phases/40-helper-client-closing/40-evidence-check.py failed /tmp/gsd40/exec/xml-keeper-green
env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain -q spotlessApply -PspotlessIdeHook="$(pwd)/<each of the five Java files>" -PspotlessIdeHookUseStdOut
git diff 52ce1cd4c1501f1c4a332f42c64295be43f0f53c -- <KeeperStateProvider.java, StateProvider.java> | grep -E '^[-+]import '
git show <phase_start_sha | writer_commit>:KeeperStateProvider.java; compare from getStateRecord to the end, the injected constructor through init(), and ProxySinkTask.java outside stop()
python3 -I .planning/phases/40-helper-client-closing/40-evidence-check.py anchors .planning/scripts/fork-anchors.txt --rows 176
grep -cF -e <each of the four anchored strings> <the three Keeper src/main files>
python3 -I .planning/phases/40-helper-client-closing/40-evidence-check.py anchors .planning/scripts/fork-anchors.txt --rows 180
git add <the seven files> && git commit -F /tmp/gsd40/exec/msg-keeper.txt
git diff --name-only "<keeper_commit>^1" "<keeper_commit>"
git diff --name-only de3faf775b424e54a8625959cce9c9648cbd40ec HEAD -- src/main
git diff --quiet fae1fcb17dc580436108aa50a994d0c5204fbbc5 HEAD -- src/main/java/com/clickhouse/kafka/connect/sink/db src/test/java/com/clickhouse/kafka/connect/sink/db
```

```text
green_run_exit: 0
keeper_class_tests: 5
proxy_class_tests: 2
writer_class_tests: 22
green_failures: 0
guards_failed: none
spotless_keeper_state_provider: IS DIRTY
spotless_keeper_state_provider_import_lines: +import java.util.function.Function;
spotless_keeper_state_provider_diff_import_only: yes
spotless_state_provider: IS DIRTY
spotless_state_provider_import_lines_changed: 0
spotless_state_provider_diff_import_only: yes
injected_client_clause_pin: source identity (ownedClient is assigned only in the factory constructor, once, before the injected-client constructor; the injected-client constructor and init() are byte-identical to phase start) plus the K3 NullPointerException tripwire; there is no behavioural pin, because the injected-client constructor cannot complete today
spotless_proxy_sink_task: IS CLEAN
spotless_keeper_test: IS CLEAN
spotless_proxy_stop_test: IS CLEAN
state_record_methods_identical_vs_writer_commit: yes
state_record_methods_identical_vs_phase_start: yes
injected_constructor_and_init_identical: yes
proxy_outside_stop_identical: yes
anchor_counts: ownedClient=4; public void close()=1; default void close()=1; stateProvider.close()=1
anchors_mirror: rows=180 bad=0
fork_delta_keeper_row: added
fork_delta_harness_row: updated
fork_delta_known_cost_13: closed
keeper_commit: b55f108a74bcadf25a21174302e20a0ca729cfda
keeper_commit_files: .planning/scripts/fork-anchors.txt; docs/FORK-DELTA.md; src/main/java/com/clickhouse/kafka/connect/sink/ProxySinkTask.java; src/main/java/com/clickhouse/kafka/connect/sink/state/StateProvider.java; src/main/java/com/clickhouse/kafka/connect/sink/state/provider/KeeperStateProvider.java; src/test/java/com/clickhouse/kafka/connect/sink/ProxySinkTaskStopUnitTest.java; src/test/java/com/clickhouse/kafka/connect/sink/state/provider/KeeperStateProviderClosingUnitTest.java
src_main_files_vs_writer_commit: src/main/java/com/clickhouse/kafka/connect/sink/ProxySinkTask.java; src/main/java/com/clickhouse/kafka/connect/sink/state/StateProvider.java; src/main/java/com/clickhouse/kafka/connect/sink/state/provider/KeeperStateProvider.java
writer_files_baseline: fae1fcb17dc580436108aa50a994d0c5204fbbc5 (the writer-half commit plus the user-authorised test-only seed)
writer_files_untouched: yes
```

The green run exited 0: the Keeper class ran 5 tests, the stop class 2 and the writer class 22, all with 0 failures, errors and skips, and the guard classes (`ProxySinkTaskConstructorUnitTest`, `SourcePlanningIdGuardUnitTest`, `ConnectApiReferencesUnitTest`, `WrappedDataExceptionClassificationPinUnitTest`) report no failing testcase. `ProxySinkTask.stop()` stops the writer, unregisters the MBean and closes the provider in the innermost `finally`, so the writer failure case still unregisters and closes. The text of `getStateRecord` and `setStateRecord` (from `getStateRecord` to the end of the file) equals both phase start and the writer-half commit, the injected-client constructor and `init()` equal phase start, and `ProxySinkTask.java` equals the writer-half commit outside `stop()`; `ownedClient = chc;` occurs once, inside the factory constructor and before the injected-client constructor. `KeeperStateProvider.java` and `StateProvider.java` print `IS DIRTY` at the baseline and still do, with the hook's stdout differing in the same 5 import and blank lines and equal import sets (import-only rule); the only import change against phase start is the `Function` import, and `StateProvider.java` changes no import line. The anchors mirror read `rows=176 bad=0` before the rows were added and reads `rows=180 bad=0` after; all four counts equal the planned ones.

Adjustment against plan 40-02 (recorded here, no earlier record edited): the plan's `writer_files_untouched` check compares against the writer-half commit `de3faf77`, but the user-authorised test-only seed `fae1fcb1` changed `ClickHouseWriterTest.java` (under `src/test/.../sink/db`) after it. The check is therefore taken against `fae1fcb1`, the tip that holds the seed: `git diff --quiet fae1fcb1 HEAD -- src/main/.../sink/db src/test/.../sink/db` exits 0, and `fae1fcb1` is an ancestor of the Keeper-half commit. The `src/main` comparison against `de3faf77` is unaffected, because the seed changed no `src/main` file.

2026-11-02T06:40:40Z

Verdict: pass

### E-10: Gate B on the Keeper-half commit: testUnit with Docker unreachable and the full suite with the exactly-once classes

Backs: CLS-03 (success criterion 3: `testUnit` and a full suite, including the exactly-once tests, pass on the Keeper-half commit) and CLS-04 (every case of the matrix green under both gates, with the exactly-once classes unharmed).

```sh
git ls-remote origin refs/heads/custom_build
git rev-parse HEAD
git diff --quiet b55f108a74bcadf25a21174302e20a0ca729cfda HEAD -- . ':!.planning'
./gradlew --stop
rm -rf build/test-results/testUnit
DOCKER_HOST=unix:///nonexistent/docker.sock env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain testUnit -PjavaVersion=21 --rerun > /tmp/gsd40/exec/gateB-testunit.log 2>&1
cp -R build/test-results/testUnit /tmp/gsd40/exec/xml-gateB-testunit
python3 -I .planning/phases/40-helper-client-closing/40-evidence-check.py junit /tmp/gsd40/exec/xml-gateB-testunit tests=1716 failures=0 errors=0 skipped=2
docker run --rm --entrypoint clickhouse clickhouse/clickhouse-server:latest local --query 'SELECT version()'
docker image inspect --format '{{index .RepoDigests 0}}' clickhouse/clickhouse-server:latest
rm -rf build/test-results/test
./gradlew --stop
CLICKHOUSE_VERSION=latest CLIENT_VERSION=V2 env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain test -PjavaVersion=21 --rerun > /tmp/gsd40/exec/gateB-full.log 2>&1
cp -R build/test-results/test /tmp/gsd40/exec/xml-gateB-full
python3 -I .planning/phases/40-helper-client-closing/40-evidence-check.py junit /tmp/gsd40/exec/xml-gateB-full tests=2314 failures=0 errors=0 skipped=18
python3 -I .planning/phases/40-helper-client-closing/40-evidence-check.py skips /tmp/gsd40/exec/xml-gateB-full
python3 -I .planning/phases/40-helper-client-closing/40-evidence-check.py junit /tmp/gsd40/exec/xml-gateB-full --class <each exactly-once class> 'tests>=1' failures=0 errors=0
docker run --rm --entrypoint clickhouse clickhouse/clickhouse-server:latest local --query 'SELECT version()'
docker image inspect --format '{{index .RepoDigests 0}}' clickhouse/clickhouse-server:latest
git ls-remote origin refs/heads/custom_build
```

```text
origin_reread: equal
gate_head: 19e5cf75d7885dee809252da90fac8ecc29ee61a
gated_tree_equals_keeper_commit: yes
jvm_line: HotSpot 21 (OpenJDK 64-Bit Server VM, -PjavaVersion=21); no OpenJ9 launcher
testunit_exit: 0
testunit_tests: 1716
testunit_failures: 0
testunit_errors: 0
testunit_skipped: 2
testunit_delta_vs_baseline: 29
clickhouse_version_before: 26.10.3.7
clickhouse_digest: sha256:739e099abd1157390960a59e4532d21781d8fde57e6e4151cb920ea02c30b1ce
full_exit: 0
full_tests: 2314
full_failures: 0
full_errors: 0
full_skipped: 18
full_skips_helper: OK (named=7 state_mismatch_cases=11 unexpected=0)
full_delta_vs_baseline: 29
exactly_once_classes: DedupTokenRetryLiveTest 8/0; KeeperStateProviderRestartLiveTest 6/0; ClickHouseSinkTaskBufferTest 35/0; ClickHouseSinkTaskWithSchemaTest 97/0; BufferedOffsetCommitUnitTest 15/0; ProcessingTest 14/0
exactly_once_classes_green: yes
v1_live_cases_passed: yes
v1_live_cases_rationale: the three [1] V1 testcases are the client V1 invocations of the only three V1/V2-parameterized methods of ClickHouseWriterTest (@ParameterizedTest at lines 299, 403 and 524: testSchemaRefreshedAfterCode131, staleDescribeTableIsNotCachedAsCurrentSchema, doInsertSupportsSpecialCharactersInTableNames); the only other parameterized method (line 577) names its cases [1] json to [3] tsv, so no other testcase is named [1] V1
clickhouse_version_after: 26.10.3.7
jvm_crash: none
origin_reread_after: equal
```

Both runs are green on the Keeper-half commit: `testUnit` with Docker unreachable ran 1716 tests (1687 plus 29: 22 writer cases and 7 Keeper-side cases), 0 failures, 0 errors, 2 skipped; the full suite on ClickHouse `latest` (`26.10.3.7`, digest equal to E-07's and unchanged before and after) with client V2 ran 2314 tests (2285 plus 29), 0 failures, 0 errors, 18 skipped (the named inventory, helper OK; 13 of them sit in `ClickHouseSinkTaskWithSchemaTest`). Full-suite wall time was 2 m 49 s (06:41:34Z to 06:44:24Z). Each of the six exactly-once classes ran with at least one test and no failure or error; `KeeperStateProviderRestartLiveTest` and `DedupTokenRetryLiveTest` exercise a real KeeperMap table, so the provider's constructor, `init()` and the state read and write paths ran against real ClickHouse with this commit. The `ClickHouseWriterTest` XML has three passed `[1] V1` cases and the passed JSON comparison case, so the client V1 per-batch close is exercised against real ClickHouse and read back. The gated head `19e5cf75` is the E-09 evidence commit on top of the Keeper-half commit and its non-planning tree equals the Keeper-half commit's. Origin `custom_build` still equals E-01's tip, before and after both runs; nothing was pushed. No `hs_err_pid*.log`, `javacore.*` or `Snap.*.trc` appeared; the runs used HotSpot 21 through `-PjavaVersion=21`.

2026-11-02T06:44:50Z

Verdict: pass

### E-11: Phase gate: cumulative merge-invariant gate, anchors, src/main file list, origin

Backs: CLS-01, CLS-02, CLS-03 and CLS-04 (ROADMAP Phase 40 success criteria 3 and 4 and the phase gate sentence: the per-half gates, then the anchors invariant over every commit of the range, with `src/main` limited to the four declared files and nothing pushed).

```sh
git rev-parse HEAD
bash .planning/scripts/verify-merge-invariants.sh 54e63648 6775cd934e105a9fc251874cdb1f96b8f35dab03 > /tmp/gsd40/exec/invariants-40.log 2>&1
tail -1 /tmp/gsd40/exec/invariants-40.log
python3 -I .planning/phases/40-helper-client-closing/40-evidence-check.py anchors .planning/scripts/fork-anchors.txt --rows 180
git diff --name-only 52ce1cd4c1501f1c4a332f42c64295be43f0f53c HEAD -- src/main
git diff --quiet 52ce1cd4c1501f1c4a332f42c64295be43f0f53c HEAD -- VERSION CHANGELOG.md
git ls-remote origin refs/heads/custom_build
```

```text
invariants_head: 6775cd934e105a9fc251874cdb1f96b8f35dab03
invariants_start_utc: 2026-11-02T06:45:20Z
invariants_end_utc: 2026-11-02T07:26:22Z
invariants_line: invariants: provenance_paths=21 anchor_commits=1356 anchor_rows=180 marker_commits=1422 anchor_exempt=19 result=PASS
invariants_exit: 0
invariants_scope: run once, alone, on the E-10 commit: the script evaluates the anchor inventory at every commit of the range, so one run on the final SHA covers both code commits, each of which already passed the anchors mirror before it was made (rows=176 at the writer-half commit, rows=180 at the Keeper-half commit); a second run would cost about 40 minutes and add nothing; no Gradle run, Docker run or commit happened while it ran
anchors_mirror: rows=180 bad=0
src_main_files_vs_phase_start: src/main/java/com/clickhouse/kafka/connect/sink/ProxySinkTask.java; src/main/java/com/clickhouse/kafka/connect/sink/db/ClickHouseWriter.java; src/main/java/com/clickhouse/kafka/connect/sink/state/StateProvider.java; src/main/java/com/clickhouse/kafka/connect/sink/state/provider/KeeperStateProvider.java
version_changelog_untouched: yes
injected_client_clause: pinned by source identity (ownedClient is assigned only in the factory constructor, once, before the injected-client constructor; the injected-client constructor and init() are byte-identical to phase start) plus the K3 NullPointerException tripwire; there is no behavioural pin, because the injected-client constructor cannot complete today
documented_limits: stop() timeout race accepted (Known cost 13, E-06); ClickHouseSinkTask.stop() calls deliveryStrategy.stop() before proxySinkTask.stop() without try/finally, outside scope (Known cost 13); a Keeper constructor that fails on client V1 leaks its client, out of scope by research OQ-1 (Known cost 13)
origin_custom_build_at_gate: 72e8ccffe05ff6dffb14098d0b8c0f67aaa6a236
pushes: none
```

The cumulative gate ended `result=PASS` with exit 0 after 41 minutes on an explicit SHA, `6775cd93`, the E-10 commit, whose non-planning tree equals the Keeper-half commit's (E-10 `gated_tree_equals_keeper_commit` held for its parent and only planning files changed since). It evaluated 1356 anchor commits against 180 rows and 21 provenance paths, with the 19 SHA-pinned exemptions unchanged. Against phase start `src/main` differs in exactly four files (`ProxySinkTask.java`, `ClickHouseWriter.java`, `StateProvider.java`, `KeeperStateProvider.java`); `VERSION` and `CHANGELOG.md` are untouched (the release phase owns them); origin `custom_build` still equals E-01's tip `72e8ccff`, and no push, `gh` write or tag push was made in this phase. The documented limits are those of Known cost 13 and threats T-40-10, T-40-13 and T-40-17.

2026-11-02T07:26:30Z

Verdict: pass
