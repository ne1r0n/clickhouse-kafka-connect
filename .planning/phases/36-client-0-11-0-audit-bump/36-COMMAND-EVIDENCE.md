# Phase 36: Command evidence

- **Base commit:** `local_head` = `99b9d85c15ce1a9b76f5e843b9aec472d24f9708` (HEAD when plan 36-01 started, branch `sync/v1.7.0`).
- **Shell:** macOS zsh, multi-line logic under `/bin/bash`. Gradle is run as `env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain ...`; live runs set `CLICKHOUSE_VERSION=latest CLIENT_VERSION=V2` explicitly.
- **Docker:** colima profile `vz` (aarch64, 8 CPU, 16 GiB, 256 GiB disk), docker CLI from `/opt/homebrew/Cellar/docker/29.8.2/bin/docker`.
- **Date (UTC):** 2026-10-29.
- **Entry format:** each entry has a `### E-NN: {title}` heading, a `Backs:` line, an `sh` fence with the commands as run, a `text` fence of `key: value` lines (keys match `[a-z0-9_]+`, one fact per line) with free text allowed after the fence, a UTC timestamp line, and a final line `Verdict: pass`, `Verdict: fail`, `Verdict: pending ({reason})` or `Verdict: unmeasured ({reason})`. No entry carries a token or credential.
- **Rule:** a fail or pending verdict is fixed by re-measuring in a new run, never by editing the evidence.
- **Helper:** `36-evidence-check.py` in this directory is the read-only gate helper (a byte-identical copy of Phase 35's and Phase 34's; get, expect, index, rows, secrets, junit, anchors, skips, failed); it is never edited.
- **Server version:** every entry records `clickhouse_version`, the `SELECT version()` of the ClickHouse `latest` image at the time of the entry, even when no server ran in it.

## Index

| Entry | Requirement | Plan | Verdict |
|-------|-------------|------|---------|
| E-01 | CLT-01, CLT-02, CLT-09 | 36-01 | pass |
| E-02 | CLT-01 | 36-01 | pass |
| E-03 | CLT-01, CLT-03, CLT-04 | 36-01 | pass |
| E-04 | CLT-06 | 36-01 | pending (plan 36-01) |
| E-05 | CLT-03 | 36-02 | pending (plan 36-02) |
| E-06 | CLT-02, CLT-03, CLT-04 | 36-02 | pending (plan 36-02) |
| E-07 | CLT-07 | 36-02 | pending (plan 36-02) |
| E-08 | CLT-06 | 36-03 | pending (plan 36-03) |
| E-09 | CLT-08 | 36-03 | pending (plan 36-03) |
| E-10 | CLT-05, CLT-02 | 36-03 | pending (plan 36-03) |
| E-11 | CLT-09 | 36-04 | pending (plan 36-04) |
| E-12 | CLT-09 | 36-04 | pending (plan 36-04) |
| E-13 | CLT-07 | 36-04 | pending (plan 36-04) |

### E-01: Pre-flight: origin, pull request and upstream re-read, Phase 35 gated, baselines on the untouched tree

Backs: CLT-01, CLT-02, CLT-09, ROADMAP Phase 36 criterion 1 (the premise re-read before any code lands, and a green baseline re-measured on the untouched tree so every later red or green is attributable to this phase).

```sh
colima list
git rev-parse --abbrev-ref HEAD
git rev-parse HEAD
git status --porcelain
python3 -I .planning/phases/35-buffered-offset-commit-under-topic-renaming-smts/35-evidence-check.py index .planning/phases/35-buffered-offset-commit-under-topic-renaming-smts/35-COMMAND-EVIDENCE.md 9 --done
git ls-remote --exit-code --tags origin pre-v1.7.0-tail-2
git ls-remote --exit-code --heads origin sync/v1.7.0
git ls-remote --exit-code --heads origin 'scratch/*'
git ls-remote origin refs/heads/custom_build
gh pr view 17 -R ne1r0n/clickhouse-kafka-connect --json state,baseRefName,headRefOid,mergeable
git ls-remote origin refs/heads/dependabot/gradle/clickHouseDriver-0.11.0
git ls-remote upstream main
curl -s https://repo1.maven.org/maven2/com/clickhouse/client-v2/maven-metadata.xml
git diff --quiet c9443756eb85e601552cb8241f0e7b52ed6209da HEAD -- .planning/scripts/verify-merge-invariants.sh .planning/scripts/verify-merge-invariants-selftest.sh
python3 -I .planning/phases/36-client-0-11-0-audit-bump/36-evidence-check.py anchors .planning/scripts/fork-anchors.txt
cmp .planning/phases/36-client-0-11-0-audit-bump/36-evidence-check.py .planning/phases/35-buffered-offset-commit-under-topic-renaming-smts/35-evidence-check.py
cmp .planning/phases/36-client-0-11-0-audit-bump/36-evidence-check.py .planning/phases/34-upstream-tail-merge/34-evidence-check.py
cmp .planning/phases/36-client-0-11-0-audit-bump/36-hygiene-check.py .planning/phases/35-buffered-offset-commit-under-topic-renaming-smts/35-hygiene-check.py
grep '^clickHouseDriver' gradle/libs.versions.toml
env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain dependencies --configuration runtimeClasspath
grep -rnE 'new[ ]ServerException[(]' src
rm -rf build/test-results/testUnit
env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain testUnit --rerun > /tmp/gsd36/exec/testunit-baseline.log 2>&1
rm -rf build/test-results/test
CLICKHOUSE_VERSION=latest CLIENT_VERSION=V2 env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain test --rerun > /tmp/gsd36/exec/suite-baseline.log 2>&1
python3 -I .planning/phases/36-client-0-11-0-audit-bump/36-evidence-check.py junit build/test-results/testUnit
python3 -I .planning/phases/36-client-0-11-0-audit-bump/36-evidence-check.py junit build/test-results/test
python3 -I .planning/phases/36-client-0-11-0-audit-bump/36-evidence-check.py skips build/test-results/test
python3 -I .planning/phases/36-client-0-11-0-audit-bump/36-evidence-check.py junit build/test-results/test --class <UtilsTest | AdditionalRetriableErrorCodesTest | AdditionalRetriableErrorCodesPinUnitTest | ClickHouseSinkJdbcPropertiesTest | NetworkCompressionPinLiveTest>
docker run --rm --entrypoint clickhouse clickhouse/clickhouse-server:latest local --query 'SELECT version()'
docker image inspect --format '{{index .RepoDigests 0}}' clickhouse/clickhouse-server:latest
```

```text
branch: sync/v1.7.0
local_head: 99b9d85c15ce1a9b76f5e843b9aec472d24f9708
dirty_paths: M .planning/config.json; M .planning/state.json
colima_vz: Running
phase35_complete: yes
pre_tail2_tag_on_origin: absent
sync_branch_on_origin: absent
scratch_branch_on_origin: absent
origin_custom_build: 3a3a15a9ae064ffe09c696b8bb87bce23419d463	refs/heads/custom_build
pr17_state: OPEN
pr17_base: custom_build
pr17_head_sha: 6a53525b738d90ee4c98a3761ebebe11421cf1f0
pr17_mergeable: MERGEABLE
dependabot_branch_sha: 6a53525b738d90ee4c98a3761ebebe11421cf1f0
upstream_main: 9bed6f7970670a55c66be8efda246889e1f95bd0
upstream_moved: no
maven_client_v2_latest: 0.11.0
gate_scripts_vs_33_e14: identical
anchors_mirror: rows=148 bad=0
helper_identical_to_phase35: yes
hygiene_helper_identical_to_phase35: yes
catalog_driver_line: clickHouseDriver = "0.9.6"
client_v2_resolved: com.clickhouse:client-v2:0.9.6
server_exception_sites: 10 in 3 files
baseline_testunit_exit: 0
baseline_testunit_tests: 1458
baseline_testunit_failures: 0
baseline_testunit_errors: 0
baseline_testunit_skipped: 2
baseline_suite_exit: 0
baseline_suite_tests: 2049
baseline_suite_failures: 0
baseline_suite_errors: 0
baseline_suite_skipped: 18
baseline_suite_duration: 2m 57s
baseline_skips: OK skips: named=7 state_mismatch_cases=11 unexpected=0
clickhouse_version: 26.10.3.7
clickhouse_digest: clickhouse/clickhouse-server@sha256:739e099abd1157390960a59e4532d21781d8fde57e6e4151cb920ea02c30b1ce
image_same_as_phase35: yes
jvm_crash: none
baseline_docker_only_tests: 591
baseline_utilstest_tests: 34
baseline_additional_tests: 3
baseline_additional_pin_tests: 19
baseline_jdbc_props_tests: 2
baseline_netcomp_live_tests: 9
```

Phase 35's recorded baselines (testUnit 1456, full suite 2047) are not reused: the untouched tree at this HEAD measures testUnit 1458 and a full suite of 2049, both green, and every later count of this phase is compared with these re-measured numbers. Origin `custom_build` is still the published `v1.7.0_gg5` commit, none of `pre-v1.7.0-tail-2`, `sync/v1.7.0` or a scratch branch exists on origin, and PR #17 and the Dependabot branch were only read.

Measured 2026-10-29T12:27:00Z (pre-flight reads and baselines).

Verdict: pass

### E-02: Factory and the error-shape pin: green on 0.9.6, the factory compiles on a bare 0.11.0 worktree

Backs: CLT-01, ROADMAP Phase 36 criterion 1 (the 10 three-argument `ServerException` sites compile on both client lines through one factory, and the first client pin drives the real V2 client over a real socket into `Utils.handleException`), and the premise of the CLT-09 one-commit revert (the test sources compile on 0.11.0 without the catalog commit carrying them).

```sh
cp .planning/phases/36-client-0-11-0-audit-bump/36-assets/ServerExceptions.java.txt src/test/java/com/clickhouse/kafka/connect/sink/util/ServerExceptions.java
sed -i '' 's/new ServerException(/ServerExceptions.create(/g' <UtilsTest | AdditionalRetriableErrorCodesTest | AdditionalRetriableErrorCodesPinUnitTest>
grep -rnE 'new[ ]ServerException[(]' src
grep -rn 'ServerExceptions.create(' src/test
git worktree add --detach /tmp/gsd36/fmt HEAD
env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew -p /tmp/gsd36/fmt --console=plain spotlessApply
git -C /tmp/gsd36/fmt diff --numstat -- <the three existing files and the factory>
env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain compileTestJava
env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain testUnit --rerun --tests '*UtilsTest' --tests '*AdditionalRetriableErrorCodes*'
git commit -F /tmp/gsd36/exec/msg-factory.txt
cp .planning/phases/36-client-0-11-0-audit-bump/36-assets/<StubClickHouseServer | ClientErrorShapes | ClientErrorShapePinUnitTest>.java.txt src/test/java/com/clickhouse/kafka/connect/sink/db/helper/<Name>.java
cmp <each asset> <each copy>
env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain testUnit --rerun --tests '*ClientErrorShapePinUnitTest'
git commit -F /tmp/gsd36/exec/msg-shape.txt
git worktree add --detach /tmp/gsd36/bare0 c6c7edb4ab5c821ac1c5c53a0dae4872315a2059^
sed -i '' 's/^clickHouseDriver = "0.9.6"$/clickHouseDriver = "0.11.0"/' /tmp/gsd36/bare0/gradle/libs.versions.toml
env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew -p /tmp/gsd36/bare0 --console=plain compileTestJava
git worktree add --detach /tmp/gsd36/bare HEAD
sed -i '' 's/^clickHouseDriver = "0.9.6"$/clickHouseDriver = "0.11.0"/' /tmp/gsd36/bare/gradle/libs.versions.toml
git -C /tmp/gsd36/bare diff --numstat
env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew -p /tmp/gsd36/bare --console=plain compileTestJava
env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew -p /tmp/gsd36/bare --console=plain testUnit --rerun --tests '*ClientErrorShapePinUnitTest' --tests '*UtilsTest' --tests '*AdditionalRetriableErrorCodes*'
env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew -p /tmp/gsd36/bare --console=plain dependencies --configuration testRuntimeClasspath
git worktree remove --force /tmp/gsd36/bare0
git worktree remove --force /tmp/gsd36/bare
git worktree prune
docker run --rm --entrypoint clickhouse clickhouse/clickhouse-server:latest local --query 'SELECT version()'
```

```text
sites_before: 10
sites_after: 0
factory_calls: 10
spotless_numstat_baseline: AdditionalRetriableErrorCodesTest.java=8/9; UtilsTest.java=10/11
spotless_numstat_edited: AdditionalRetriableErrorCodesTest.java=8/9; UtilsTest.java=10/11
spotless_unchanged: yes
spotless_factory_cmp: identical
spotless_new_files_cmp: identical
compile_test_exit_096: 0
utilstest_tests: 34
additional_tests: 3
additional_pin_tests: 19
factory_tests_failed: none
assets_identical: yes
factory_commit: c6c7edb4ab5c821ac1c5c53a0dae4872315a2059
factory_commit_files: src/test/java/com/clickhouse/kafka/connect/sink/AdditionalRetriableErrorCodesPinUnitTest.java; src/test/java/com/clickhouse/kafka/connect/sink/AdditionalRetriableErrorCodesTest.java; src/test/java/com/clickhouse/kafka/connect/sink/util/ServerExceptions.java; src/test/java/com/clickhouse/kafka/connect/sink/util/UtilsTest.java
shape_pin_exit: 0
shape_pin_xml: tests=7 failures=0 errors=0 skipped=0
shape_pin_failed: none
shape_pin_commit: 9dd365882f2188ec27a8fb762aba300039a59523
shape_pin_commit_files: src/test/java/com/clickhouse/kafka/connect/sink/db/helper/ClientErrorShapePinUnitTest.java; src/test/java/com/clickhouse/kafka/connect/sink/db/helper/ClientErrorShapes.java; src/test/java/com/clickhouse/kafka/connect/sink/db/helper/StubClickHouseServer.java
bare_unfactored_compile_exit: 1
bare_unfactored_error_count: 10
bare_unfactored_error_files: AdditionalRetriableErrorCodesPinUnitTest.java=1; AdditionalRetriableErrorCodesTest.java=1; UtilsTest.java=8
bare_unfactored_required: int,String,int,String
bare0_worktree_removed: yes
bare_catalog_numstat: 1/1
bare_compile_test_exit: 0
bare_resolved_client: com.clickhouse:client-v2:0.11.0
bare_shape_pin_xml: tests=7 failures=0 errors=0 skipped=0
bare_utilstest_tests: 34
bare_additional_tests: 3
bare_additional_pin_tests: 19
bare_failed: none
bare_worktree_removed: yes
hygiene_scan: clean
clickhouse_version: 26.10.3.7
```

The Spotless numstat of an existing file is read against the staged copy of the edited file (`git add` of the copies in the throwaway worktree before the second `spotlessApply`), so the figure counts what Spotless still wants to change and not the call-site edit itself; the baseline is the same figure on the pristine worktree, and the pin class and the factory carry no Spotless hunk. On client 0.9.6 the seven shape rows are 502, 503, an unexpected status, 159, 252, 60 and a bare `ClientException`. The bare worktree without the factory fails `compileTestJava` with exactly the 10 constructor errors (`required: int,String,int,String`, `found: int,String,int`), and the one with the factory and the catalog value as the only change compiles the test sources and keeps the shape rows, `UtilsTest` and both `AdditionalRetriableErrorCodes` classes green on client 0.11.0.

Measured 2026-10-29T12:30:00Z (factory, shape pin and bare worktrees).

Verdict: pass

### E-03: The remaining 0.9.6 pins are green, and the bare 0.11.0 red set is exactly the endpoint and unknown-key pins

Backs: CLT-01, CLT-03, CLT-04, ROADMAP Phase 36 criteria 1 and 3 (the timeout prefixes, the endpoint and options pins, the insert wire and the live behaviours are green on the shipped client, and the break the two `createClientV2` fixes of plan 36-02 must repair is pinned exactly before any fix exists).

```sh
cp .planning/phases/36-client-0-11-0-audit-bump/36-assets/<ClientTimeoutPrefixPinUnitTest | ClientEndpointAndOptionsPinUnitTest | ClientInsertWirePinUnitTest | Client011BehaviourPinLiveTest>.java.txt src/test/java/com/clickhouse/kafka/connect/sink/db/helper/<Name>.java
cmp <each asset> <each copy>
rm -rf build/test-results/testUnit
env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain testUnit --rerun --tests '*ClientErrorShapePinUnitTest' --tests '*ClientTimeoutPrefixPinUnitTest' --tests '*ClientEndpointAndOptionsPinUnitTest' --tests '*ClientInsertWirePinUnitTest' > /tmp/gsd36/exec/pins-096.log 2>&1
rm -rf build/test-results/test
CLICKHOUSE_VERSION=latest CLIENT_VERSION=V2 env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain test --rerun --tests '*Client011BehaviourPinLiveTest' > /tmp/gsd36/exec/live-096.log 2>&1
python3 -I .planning/phases/36-client-0-11-0-audit-bump/36-evidence-check.py junit build/test-results/<testUnit | test> --class <each class>
docker image inspect --format '{{index .RepoDigests 0}}' clickhouse/clickhouse-server:latest
git worktree add --detach /tmp/gsd36/fmt HEAD
env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew -p /tmp/gsd36/fmt --console=plain spotlessApply
git commit -F /tmp/gsd36/exec/msg-pins.txt
git worktree add --detach /tmp/gsd36/bare2 HEAD
sed -i '' 's/^clickHouseDriver = "0.9.6"$/clickHouseDriver = "0.11.0"/' /tmp/gsd36/bare2/gradle/libs.versions.toml
git -C /tmp/gsd36/bare2 diff --numstat
env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew -p /tmp/gsd36/bare2 --console=plain testUnit --rerun --tests '*ClientErrorShapePinUnitTest' --tests '*ClientTimeoutPrefixPinUnitTest' --tests '*ClientEndpointAndOptionsPinUnitTest' --tests '*ClientInsertWirePinUnitTest' --tests '*UtilsTest' --tests '*AdditionalRetriableErrorCodes*' > /tmp/gsd36/exec/bare-dockerfree.log 2>&1
python3 -I .planning/phases/36-client-0-11-0-audit-bump/36-evidence-check.py failed /tmp/gsd36/bare2/build/test-results/testUnit --expect <the three endpoint names>
CLICKHOUSE_VERSION=latest CLIENT_VERSION=V2 env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew -p /tmp/gsd36/bare2 --console=plain test --rerun --tests '*Client011BehaviourPinLiveTest' --tests '*ClickHouseSinkJdbcPropertiesTest' > /tmp/gsd36/exec/bare-live.log 2>&1
python3 -I .planning/phases/36-client-0-11-0-audit-bump/36-evidence-check.py failed /tmp/gsd36/bare2/build/test-results/test --expect <the three live names>
git worktree remove --force /tmp/gsd36/bare2
git worktree prune
docker run --rm --entrypoint clickhouse clickhouse/clickhouse-server:latest local --query 'SELECT version()'
```

```text
assets_identical: yes
pins_dockerfree_exit: 0
error_shape_xml: tests=7 failures=0 errors=0 skipped=0
timeout_prefix_xml: tests=2 failures=0 errors=0 skipped=0
endpoint_xml: tests=3 failures=0 errors=0 skipped=0
wire_xml: tests=2 failures=0 errors=0 skipped=0
live_exit: 0
live_xml: tests=6 failures=0 errors=0 skipped=0
spotless_worktree_cmp: identical
pins_commit: 765476af5d09c008e6b259299b6ff47deb83c06c
pins_commit_files: src/test/java/com/clickhouse/kafka/connect/sink/db/helper/Client011BehaviourPinLiveTest.java; src/test/java/com/clickhouse/kafka/connect/sink/db/helper/ClientEndpointAndOptionsPinUnitTest.java; src/test/java/com/clickhouse/kafka/connect/sink/db/helper/ClientInsertWirePinUnitTest.java; src/test/java/com/clickhouse/kafka/connect/sink/db/helper/ClientTimeoutPrefixPinUnitTest.java
bare_catalog_numstat: 1/1
bare_dockerfree_exit: 1
bare_dockerfree_failed: ClientEndpointAndOptionsPinUnitTest#CLT-03: ssl=true and sslmode=strict build a working V2 client and never reach the server; ClientEndpointAndOptionsPinUnitTest#CLT-03: the V1-only keys of the live jdbcConnectionProperties pins are accepted too; ClientEndpointAndOptionsPinUnitTest#CLT-03: the request path is slash and the database travels in the X-ClickHouse-Database header
bare_dockerfree_prediction_met: yes
bare_other_pins_green: yes
bare_live_exit: 1
bare_live_failed: ClickHouseSinkJdbcPropertiesTest#primitiveTypesTest; ClickHouseSinkJdbcPropertiesTest#withEmptyDataRecordsTest; Client011BehaviourPinLiveTest#CLT-04: ssl=true and sslmode=strict still ping and query a real server on V2
bare_live_prediction_met: yes
bare_live_error: Unknown and unmapped config properties: [ssl, sslmode]
bare2_worktree_removed: yes
hygiene_scan: clean
clickhouse_version: 26.10.3.7
clickhouse_digest: clickhouse/clickhouse-server@sha256:739e099abd1157390960a59e4532d21781d8fde57e6e4151cb920ea02c30b1ce
bare_clickhouse_version: 26.10.3.7
```

On client 0.9.6 the four Docker-free classes run 7, 2, 3 and 2 executions green and the live class 6 green on ClickHouse `latest` with client V2 (the 159 pin uses a real `max_execution_time=1` query, the ZSTD pin takes its 0.9.6 branch `Invalid LZ4 magic byte`, and the V1 pin expects `Magic is not correct`). On the bare 0.11.0 worktree, whose only change is the catalog value, the Docker-free red set is exactly the three `ClientEndpointAndOptionsPinUnitTest` tests (the request path, the unknown-key break for `ssl` and `sslmode`, and the other V1-only keys), and the shape, timeout and wire classes report no failure there. The live red set is exactly `ClickHouseSinkJdbcPropertiesTest` `primitiveTypesTest` and `withEmptyDataRecordsTest` plus the `Client011BehaviourPinLiveTest` ssl-and-sslmode pin, and the other five live pins are green on 0.11.0. These are the red tests that plan 36-02's two `createClientV2` fixes turn green.

Measured 2026-10-29T12:32:00Z (0.9.6 pins and the bare 0.11.0 worktree).

Verdict: pass
