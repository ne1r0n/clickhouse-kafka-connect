# Phase 41: Command evidence

- **Base commit:** `phase_start_sha` = `69d5da3aa0d2f443294b26b954bc78c84643fe17` (HEAD when plan 41-01 started, branch `sync/v1.7.0`).
- **Shell:** macOS zsh, multi-line logic under `/bin/bash` or `python3 -I`. Gradle is run as `env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain ... -PjavaVersion=21`, so every run shares one HotSpot 21 JVM line and the OpenJ9 crash is avoided.
- **Docker:** colima profile `vz` (aarch64, 8 CPU, 16 GiB, 256 GiB disk), docker CLI from `/opt/homebrew/Cellar/docker/29.8.2/bin/docker`.
- **Date (UTC):** 2026-11-02.
- **Entry format:** each entry has a `### E-NN: {title}` heading, a `Backs:` line, an `sh` fence with the commands as run, a `text` fence of `key: value` lines (keys match `[a-z0-9_]+`, one fact per line, no key twice) with free text allowed after the fence, a UTC timestamp line, and a final line `Verdict: pass`, `Verdict: fail`, `Verdict: pending ({reason})` or `Verdict: unmeasured ({reason})`.
- **Rule:** a fail or pending verdict is fixed by re-measuring in a new run, never by editing the evidence.
- **Helper:** `41-evidence-check.py` in this directory is a byte copy of Phase 39's `39-evidence-check.py` (get, expect, index, rows, secrets, junit, anchors, skips, failed) and is never edited.
- **Red-reason rule:** a red counts only when every failing testcase is an `org.opentest4j.AssertionFailedError` whose message contains `execution timed out after` (the text `assertTimeoutPreemptively` produces) and no failing testcase took more than 30 s; a compile error, an unexpected exception, a different assertion or a hang is not a recorded red.
- **No push happens in Phase 41.** Reads use `git ls-remote` only.

## Index

| Entry | Requirement | Plan | Verdict |
|-------|-------------|------|---------|
| E-01 | PIN-01 | 41-01 | pass |
| E-02 | PIN-01 | 41-01 | pass |
| E-03 | PIN-01, TMO-01, TMO-02 | 41-01 | pass |
| E-04 | PIN-01 | 41-02 | pass |
| E-05 | TMO-01 | 41-02 | pass |
| E-06 | TMO-01, TMO-02, TMO-03 | 41-02 | pending (plan 41-02) |
| E-07 | TMO-01 | 41-02 | pending (plan 41-02) |
| E-08 | TMO-01, TMO-02 | 41-03 | pending (plan 41-03) |
| E-09 | TMO-03 | 41-03 | pending (plan 41-03) |
| E-10 | PIN-01, TMO-01, TMO-02, TMO-03 | 41-03 | pending (plan 41-03) |

### E-01: Execution-start facts, measured baseline and environment

Backs: PIN-01 (execution-start facts: base commit, origin re-read, the evidence helper copy, the anchors mirror, the environment, the Spotless baselines of the three files the phase touches, and the start baseline measured on the unchanged tree instead of taken from the Phase 40 report).

```sh
mkdir -p /tmp/gsd41/exec
git rev-parse HEAD
git rev-parse --abbrev-ref HEAD
git status --porcelain
git ls-remote origin refs/heads/custom_build
cp .planning/phases/39-dependabot-absorption-behaviour-pins/39-evidence-check.py .planning/phases/41-bounded-insert-timeout/41-evidence-check.py
cmp .planning/phases/39-dependabot-absorption-behaviour-pins/39-evidence-check.py .planning/phases/41-bounded-insert-timeout/41-evidence-check.py
python3 -I .planning/phases/41-bounded-insert-timeout/41-evidence-check.py anchors .planning/scripts/fork-anchors.txt --rows 180
grep -cF -e '//this.addClickHouseSetting("async_insert", "0", false);' src/main/java/com/clickhouse/kafka/connect/sink/ClickHouseSinkConfig.java
git rev-parse HEAD:src/main/java/com/clickhouse/kafka/connect/sink/ClickHouseSinkConfig.java
colima list
ls -d /opt/homebrew/Cellar/docker/*/bin/docker
/usr/libexec/java_home -v 21
env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain -q spotlessApply -PspotlessIdeHook="$(pwd)/<file>" -PspotlessIdeHookUseStdOut > /tmp/gsd41/exec/sc-base-<name>.out 2> /tmp/gsd41/exec/sc-base-<name>.err
docker run --rm --entrypoint clickhouse clickhouse/clickhouse-server:latest local --query 'SELECT version()'
docker image inspect --format '{{index .RepoDigests 0}}' clickhouse/clickhouse-server:latest
./gradlew --stop
rm -rf build/test-results/testUnit
DOCKER_HOST=unix:///nonexistent/docker.sock env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain testUnit -PjavaVersion=21 --rerun > /tmp/gsd41/exec/baseline-testunit.log 2>&1
cp -R build/test-results/testUnit /tmp/gsd41/exec/xml-baseline-testunit
rm -rf build/test-results/test
./gradlew --stop
CLICKHOUSE_VERSION=latest CLIENT_VERSION=V2 env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain test -PjavaVersion=21 --rerun > /tmp/gsd41/exec/baseline-full.log 2>&1
cp -R build/test-results/test /tmp/gsd41/exec/xml-baseline-full
python3 -I .planning/phases/41-bounded-insert-timeout/41-evidence-check.py skips /tmp/gsd41/exec/xml-baseline-full
```

```text
phase_start_sha: 69d5da3aa0d2f443294b26b954bc78c84643fe17
branch: sync/v1.7.0
dirty_paths: M .planning/config.json; M .planning/state.json
origin_custom_build: 72e8ccffe05ff6dffb14098d0b8c0f67aaa6a236
origin_custom_build_expected: 72e8ccffe05ff6dffb14098d0b8c0f67aaa6a236
helper_copy: cmp-equal
anchors_mirror: rows=180 bad=0
cfg02_anchor_count: 1
sink_config_blob: 7ab06b3c2eebe131c06f4c1087b2c73295b443d7
colima_vz: Running
docker_cli: /opt/homebrew/Cellar/docker/29.8.2/bin/docker
jdk21_home: /opt/homebrew/Cellar/openjdk@21/21.0.12.1/libexec/openjdk.jdk/Contents/Home
testcontainers_host_override: set
spotless_baseline_clickhouse_writer: IS DIRTY
spotless_baseline_clickhouse_helper_client: IS DIRTY
spotless_baseline_stub_server: IS CLEAN
baseline_testunit_exit: 0
baseline_testunit_tests: 1719
baseline_testunit_failures: 0
baseline_testunit_errors: 0
baseline_testunit_skipped: 2
baseline_full_exit: 0
baseline_full_tests: 2317
baseline_full_failures: 0
baseline_full_errors: 0
baseline_full_skipped: 18
baseline_full_skips_helper: OK skips: named=7 state_mismatch_cases=11 unexpected=0
baseline_recorded_testunit: 1719
baseline_recorded_full: 2317
baseline_matches_record: yes
clickhouse_version_baseline: 26.10.3.7
clickhouse_digest_baseline: clickhouse/clickhouse-server@sha256:739e099abd1157390960a59e4532d21781d8fde57e6e4151cb920ea02c30b1ce
jvm_line: -PjavaVersion=21; openjdk version 21.0.12.1 2026-08-18 (Homebrew build 21.0.12.1); no OpenJ9 launcher
```

The only dirty paths are the two orchestrator-managed `.planning/` files, so the dirty-path rule holds. `origin/custom_build` equals the expected tip. The three Spotless verdicts equal the planning measurement: `ClickHouseWriter.java` differs from the hook's output in 21 lines and `ClickHouseHelperClient.java` in 7, every differing line an import or blank line with equal import sets, so neither file is ever rewritten by the hook; `StubClickHouseServer.java` is clean. The measured `testUnit` totals (1719 tests, 2 skipped) and the measured full-suite totals on ClickHouse `latest` x client V2 (2317 tests, the 18 named skips) both equal the Phase 40 review-fix record, and both runs exited 0 with 0 failures and 0 errors; those measured totals are the phase baseline. The ClickHouse version and image digest read the same before the `testUnit` run and after the full run. No crash file (`hs_err_pid*`, `javacore.*`, `core*`, `Snap.*`) appeared in the repository root. `testcontainers_host_override` is recorded as set only; this shell sets it to a literal that breaks Ryuk, which is why every Gradle run unsets it.

2026-11-02T08:49:47Z

Verdict: pass

### E-02: Tracer: the stall responder and the writer-level pin, red on three V2 routes

Backs: PIN-01 (the stall responder in `StubClickHouseServer` and the first stall pin, a real `ClickHouseWriter` insert on client V2 with `async=false` wrapped in `assertTimeoutPreemptively`, run end to end with Docker unreachable on the JSON, String and RowBinary routes; the first red of the matrix, recorded before any fix exists).

```sh
git diff --quiet HEAD -- src/main
env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain -q spotlessApply -PspotlessIdeHook="$(pwd)/src/test/java/com/clickhouse/kafka/connect/sink/db/helper/StubClickHouseServer.java" -PspotlessIdeHookUseStdOut > /tmp/gsd41/exec/sc-stub.out 2> /tmp/gsd41/exec/sc-stub.err
env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain -q spotlessApply -PspotlessIdeHook="$(pwd)/src/test/java/com/clickhouse/kafka/connect/sink/InsertStallBoundPinUnitTest.java" -PspotlessIdeHookUseStdOut > /tmp/gsd41/exec/sc-pin.out 2> /tmp/gsd41/exec/sc-pin.err
./gradlew --stop
rm -rf build/test-results/testUnit
DOCKER_HOST=unix:///nonexistent/docker.sock env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain testUnit -PjavaVersion=21 --rerun --tests '*InsertStallBoundPinUnitTest' --tests '*ClientInsertWirePinUnitTest' --tests '*ClientErrorShapePinUnitTest' --tests '*ClientEndpointAndOptionsPinUnitTest' --tests '*WriterHelperClientClosingUnitTest' > /tmp/gsd41/exec/tracer.log 2>&1
cp -R build/test-results/testUnit /tmp/gsd41/exec/xml-tracer
python3 -I .planning/phases/41-bounded-insert-timeout/41-evidence-check.py junit /tmp/gsd41/exec/xml-tracer --class InsertStallBoundPinUnitTest tests=3 failures=3
python3 -I .planning/phases/41-bounded-insert-timeout/41-evidence-check.py failed /tmp/gsd41/exec/xml-tracer --expect 'InsertStallBoundPinUnitTest#stalledV2InsertEndsWithinTheBoundWithARetriableRootCause'
```

```text
spotless_stub_server: IS CLEAN
spotless_test_class: IS CLEAN
tracer_run_exit: 1
tracer_class_tests: 3
tracer_class_failures: 3
red_failed_set: InsertStallBoundPinUnitTest#stalledV2InsertEndsWithinTheBoundWithARetriableRootCause
red_reason_timeout: yes
red_case_max_seconds: 10.604
stub_users_green: yes
src_main_untouched: yes
```

The run exited non-zero (`40 tests completed, 3 failed`) and produced the class XML, which proves the sources compiled and the run did not hang. The three failures are the pin on the JSON, String and RowBinary routes, each an `org.opentest4j.AssertionFailedError` whose message contains `execution timed out after`; they took 10.604 s, 10.02 s and 10.017 s, the 10 s preemptive bound plus teardown, so the unfixed V2 insert with `async=false` blocks in the stalled read and is ended only by the bound. The other 37 tests pass, among them the four existing stub users (`ClientInsertWirePinUnitTest` 2, `ClientErrorShapePinUnitTest` 7, `ClientEndpointAndOptionsPinUnitTest` 5, `WriterHelperClientClosingUnitTest` 23), so the stub change (public class, release latch, daemon pool factories, idempotent `close()`) did not disturb them. `git diff --quiet HEAD -- src/main` exits 0: no fix of any kind exists in the tree. The two test files stay uncommitted for Task 2.

2026-11-02T08:51:08Z

Verdict: pass

### E-03: The 15-case stall pin red on the unfixed tree, the red tree saved as a patch, src restored

Backs: PIN-01 (every stall pin has a recorded red on a tree with no fix), TMO-01 (the writer-level pin and the operator guards that the fix of plan 41-02 must turn green and keep green) and TMO-02 (the `ProxySinkTask.put` pin under `errors.tolerance` none and all).

```sh
env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain -q spotlessApply -PspotlessIdeHook="$(pwd)/src/test/java/com/clickhouse/kafka/connect/sink/InsertStallBoundPinUnitTest.java" -PspotlessIdeHookUseStdOut > /tmp/gsd41/exec/sc-pin2.out 2> /tmp/gsd41/exec/sc-pin2.err
env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain -q spotlessApply -PspotlessIdeHook="$(pwd)/src/test/java/com/clickhouse/kafka/connect/sink/db/helper/StubClickHouseServer.java" -PspotlessIdeHookUseStdOut > /tmp/gsd41/exec/sc-stub2.out 2> /tmp/gsd41/exec/sc-stub2.err
./gradlew --stop
rm -rf build/test-results/testUnit
DOCKER_HOST=unix:///nonexistent/docker.sock env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain testUnit -PjavaVersion=21 --rerun --tests '*InsertStallBoundPinUnitTest' --tests '*ClientInsertWirePinUnitTest' --tests '*ClientErrorShapePinUnitTest' --tests '*ClientEndpointAndOptionsPinUnitTest' --tests '*WriterHelperClientClosingUnitTest' > /tmp/gsd41/exec/red.log 2>&1
cp -R build/test-results/testUnit /tmp/gsd41/exec/xml-red
python3 -I .planning/phases/41-bounded-insert-timeout/41-evidence-check.py junit /tmp/gsd41/exec/xml-red --class InsertStallBoundPinUnitTest tests=15 failures=9
python3 -I .planning/phases/41-bounded-insert-timeout/41-evidence-check.py failed /tmp/gsd41/exec/xml-red --expect 'InsertStallBoundPinUnitTest#stalledV2InsertEndsWithinTheBoundWithARetriableRootCause' 'InsertStallBoundPinUnitTest#stalledV2InsertThroughPutIsRetriableWithNoDlqRecord'
git add -N src/test/java/com/clickhouse/kafka/connect/sink/InsertStallBoundPinUnitTest.java
git diff --no-color --no-ext-diff -- src/test/java/com/clickhouse/kafka/connect/sink/db/helper/StubClickHouseServer.java src/test/java/com/clickhouse/kafka/connect/sink/InsertStallBoundPinUnitTest.java > .planning/phases/41-bounded-insert-timeout/41-red-pins.patch
git reset -q -- src/test/java/com/clickhouse/kafka/connect/sink/InsertStallBoundPinUnitTest.java
shasum -a 256 .planning/phases/41-bounded-insert-timeout/41-red-pins.patch
git checkout -- src/test/java/com/clickhouse/kafka/connect/sink/db/helper/StubClickHouseServer.java
rm src/test/java/com/clickhouse/kafka/connect/sink/InsertStallBoundPinUnitTest.java
git status --porcelain -- src
git diff --quiet HEAD -- src
git apply --check .planning/phases/41-bounded-insert-timeout/41-red-pins.patch
```

```text
spotless_test_class: IS CLEAN
spotless_stub_server: IS CLEAN
red_run_exit: 1
class_tests: 15
class_failures: 9
red_failed_count: 2
red_failed_set: InsertStallBoundPinUnitTest#stalledV2InsertEndsWithinTheBoundWithARetriableRootCause; InsertStallBoundPinUnitTest#stalledV2InsertThroughPutIsRetriableWithNoDlqRecord
red_reason_timeout: yes
red_case_max_seconds: 10.691
operator_guards_green_on_unfixed: yes
stub_users_green: yes
patch_paths: src/test/java/com/clickhouse/kafka/connect/sink/InsertStallBoundPinUnitTest.java; src/test/java/com/clickhouse/kafka/connect/sink/db/helper/StubClickHouseServer.java
patch_sha256: 10c738b2beac4abd91c4a7b126e947052d0b7b1d62e7f807d243b84de36e9ffc
tree_restored: yes
patch_applies_on_head: yes
```

The run exited non-zero (`52 tests completed, 9 failed`) and produced the class XML. `InsertStallBoundPinUnitTest` ran 15 cases: the writer-level pin on the JSON, String and RowBinary routes (3 cases) and the `ProxySinkTask.put` pin over the same routes under `errors.tolerance` none and all (6 cases) all failed, every one an `org.opentest4j.AssertionFailedError` whose message contains `execution timed out after`, the slowest at 10.691 s (the 10 s preemptive bound plus teardown). The six operator `socket_timeout` cases (a longer value than the insert timeout against a 1 s delayed answer, a shorter value than the insert timeout against a stall) passed on the unfixed tree, as designed: the operator path already works today because `createClientV2` forwards `socket_timeout` as a client-level option, so these cases are guards against a naive fix that sets the per-insert value unconditionally (per-insert settings override the client-level option), not red pins; plan 41-02 proves they discriminate by a recorded mutation. The four existing stub users (2, 7, 5 and 23 tests) pass. The red tree (the stub and the pin class) was saved with `git add -N`, `git diff` and `git reset -q`, the stub restored with `git checkout --` on that one file and the pin class removed by name, leaving `src` equal to HEAD; the patch names exactly those two paths and applies on HEAD. A first attempt of this run stopped at test compilation (the `ProxySinkTask.put` case lacked `throws Exception` because `StubClickHouseServer.stalling()` throws `IOException`); that was a compile error in the new test, fixed before any recorded red, and the red run above is the first that compiled.

2026-11-02T08:59:54Z

Verdict: pass

### E-04: Red tree re-applied and re-confirmed; the pin/seam commit

Backs: PIN-01 (the red tree from `41-red-pins.patch` is re-applied byte-identical and re-run with the same red set as E-03, then committed apart from the fix as the pin/seam commit with the stub's FORK-DELTA harness row).

```sh
shasum -a 256 .planning/phases/41-bounded-insert-timeout/41-red-pins.patch
git apply --check .planning/phases/41-bounded-insert-timeout/41-red-pins.patch
git apply .planning/phases/41-bounded-insert-timeout/41-red-pins.patch
bash /tmp/gsd41/exec/run-filtered.sh reconfirm '*InsertStallBoundPinUnitTest' '*ClientInsertWirePinUnitTest' '*ClientErrorShapePinUnitTest' '*ClientEndpointAndOptionsPinUnitTest' '*WriterHelperClientClosingUnitTest'
python3 -I .planning/phases/41-bounded-insert-timeout/41-evidence-check.py junit /tmp/gsd41/exec/xml-reconfirm --class InsertStallBoundPinUnitTest tests=15 failures=9
python3 -I .planning/phases/41-bounded-insert-timeout/41-evidence-check.py failed /tmp/gsd41/exec/xml-reconfirm --expect 'InsertStallBoundPinUnitTest#stalledV2InsertEndsWithinTheBoundWithARetriableRootCause' 'InsertStallBoundPinUnitTest#stalledV2InsertThroughPutIsRetriableWithNoDlqRecord'
python3 -I /tmp/gsd41/exec/insert-harness-row.py
git add src/test/java/com/clickhouse/kafka/connect/sink/db/helper/StubClickHouseServer.java src/test/java/com/clickhouse/kafka/connect/sink/InsertStallBoundPinUnitTest.java docs/FORK-DELTA.md
git commit -F /tmp/gsd41/exec/msg-pin.txt
git diff --quiet 69d5da3aa0d2f443294b26b954bc78c84643fe17 HEAD -- src/main
```

```text
patch_sha256_matches: yes
patch_applied: yes
reconfirm_run_exit: 1
reconfirm_class_tests: 15
reconfirm_class_failures: 9
reconfirm_matches_e03: yes
red_reason_timeout: yes
harness_row_added: yes
pin_commit: 0a8e1e53698fca30efcfcc3051c46d59894a3c65
pin_commit_files: docs/FORK-DELTA.md; src/test/java/com/clickhouse/kafka/connect/sink/InsertStallBoundPinUnitTest.java; src/test/java/com/clickhouse/kafka/connect/sink/db/helper/StubClickHouseServer.java
pin_commit_src_main_untouched: yes
```

The patch SHA-256 equals E-03's `10c738b2...e9ffc`, `git apply --check` and `git apply` succeeded on the branch tip. The re-run exited non-zero with the pin class at 15 tests and 9 failures; the failing set is E-03's two names and every failure is an `AssertionFailedError` containing `execution timed out after`, none above 30 s. The FORK-DELTA harness row for `StubClickHouseServer` sits directly after the "Hand-written test doubles" row (a one-line insertion). The pin/seam commit holds exactly the stub, the pin class and FORK-DELTA, and no `src/main` file; it is red by design until the fix commit and the two commits are one revert unit.

2026-11-02T09:07:54Z

Verdict: pass

### E-05: Tracer: the per-insert timeout at the JSON site turns the JSON cases green

Backs: TMO-01 (the thinnest fix that reaches every layer: `ClickHouseHelperClient.setInsertSocketTimeoutV2` with its operator skip, called at the JSON insert site only; the JSON cases pass while the String and RowBinary cases stay red by timeout).

```sh
python3 -I /tmp/gsd41/exec/edit-helper.py
python3 -I /tmp/gsd41/exec/edit-writer.py json
bash /tmp/gsd41/exec/spot.sh imports src/main/java/com/clickhouse/kafka/connect/sink/db/ClickHouseWriter.java writer
bash /tmp/gsd41/exec/spot.sh imports src/main/java/com/clickhouse/kafka/connect/sink/db/helper/ClickHouseHelperClient.java helper
bash /tmp/gsd41/exec/run-filtered.sh intermediate '*InsertStallBoundPinUnitTest' '*NetworkCompressionPinUnitTest' '*ClientEndpointAndOptionsPinUnitTest'
python3 -I .planning/phases/41-bounded-insert-timeout/41-evidence-check.py junit /tmp/gsd41/exec/xml-intermediate --class InsertStallBoundPinUnitTest tests=15 failures=6
python3 -I .planning/phases/41-bounded-insert-timeout/41-evidence-check.py failed /tmp/gsd41/exec/xml-intermediate --expect 'InsertStallBoundPinUnitTest#stalledV2InsertEndsWithinTheBoundWithARetriableRootCause' 'InsertStallBoundPinUnitTest#stalledV2InsertThroughPutIsRetriableWithNoDlqRecord'
git diff -- src/main/java/com/clickhouse/kafka/connect/sink/db/helper/ClickHouseHelperClient.java | grep -E '^[-+]import '
```

```text
intermediate_run_exit: 1
intermediate_class_tests: 15
intermediate_class_failures: 6
intermediate_failed_set: InsertStallBoundPinUnitTest#stalledV2InsertEndsWithinTheBoundWithARetriableRootCause; InsertStallBoundPinUnitTest#stalledV2InsertThroughPutIsRetriableWithNoDlqRecord
json_cases_failing: 0
red_reason_timeout: yes
parser_guards_green: yes
helper_import_lines: +import com.clickhouse.client.api.ClientConfigProperties; and +import java.time.temporal.ChronoUnit;
writer_call_lines: 1
spotless_helper_diff_import_only: yes
spotless_writer_diff_import_only: yes
```

The run exited non-zero with the pin class at 15 tests and 6 failures: the String and RowBinary cases of the two stall pins (writer-level 2, `ProxySinkTask.put` under `none` and `all` 4), every one an `AssertionFailedError` containing `execution timed out after`, none above 30 s. No failing testcase name contains `route=JSON`, so the writer-level JSON case and the JSON `put` cases under both tolerances pass, and the 6 operator guards pass. `NetworkCompressionPinUnitTest` (9 tests) and `ClientEndpointAndOptionsPinUnitTest` (5 tests), the two parser guards, pass with 0 failures. The helper's import lines changed by exactly the two planned additions, the writer's import lines did not change, and the live scoped hook reports both files import-only dirty as at phase start. The helper and writer edits stay uncommitted for the fix commit.

2026-11-02T09:07:54Z

Verdict: pass
