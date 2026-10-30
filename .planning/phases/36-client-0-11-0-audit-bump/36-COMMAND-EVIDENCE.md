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
| E-04 | CLT-06 | 36-01 | pass |
| E-05 | CLT-03 | 36-02 | pass |
| E-06 | CLT-02, CLT-03, CLT-04 | 36-02 | pass |
| E-07 | CLT-07 | 36-02 | pass |
| E-08 | CLT-06 | 36-03 | pass |
| E-09 | CLT-08 | 36-03 | pass |
| E-10 | CLT-05, CLT-02 | 36-03 | pass |
| E-11 | CLT-09 | 36-04 | pass |
| E-12 | CLT-09 | 36-04 | pass |
| E-13 | CLT-07 | 36-04 | pending (user-gated) |
| E-14 | CLT-09 | 36-04 | unmeasured (no-go branch not taken) |
| E-15 | CLT-07 | 36-04 | fail |

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

### E-04: The RowBinary golden: 96 lines blessed on client 0.9.6, verified, zone-independent and able to fail

Backs: CLT-06, ROADMAP Phase 36 criterion 4, 0.9.6 half (the exact RowBinary body the connector hands the client is pinned for 48 type cases in both variants, so plan 36-03 can show the same bytes on 0.11.0 and a replayed exactly-once batch is the same block).

```sh
cp .planning/phases/36-client-0-11-0-audit-bump/36-assets/<RowBinaryParityCases | RowBinaryClientParityLiveTest>.java.txt src/test/java/com/clickhouse/kafka/connect/sink/<Name>.java
cmp <each asset> <each copy>
rm -rf build/test-results/test
ROWBINARY_GOLDEN_BLESS=true CLICKHOUSE_VERSION=latest CLIENT_VERSION=V2 env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain test --rerun --tests '*RowBinaryClientParityLiveTest' > /tmp/gsd36/exec/golden-bless.log 2>&1
sed -n 2p src/test/resources/rowbinary-golden-sha256.txt
grep -v '^#' src/test/resources/rowbinary-golden-sha256.txt | shasum -a 256
rm -rf build/test-results/test
CLICKHOUSE_VERSION=latest CLIENT_VERSION=V2 env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain test --rerun --tests '*RowBinaryClientParityLiveTest' > /tmp/gsd36/exec/golden-verify-096.log 2>&1
rm -rf build/test-results/test
TZ=America/Los_Angeles CLICKHOUSE_VERSION=latest CLIENT_VERSION=V2 env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain test --rerun --tests '*RowBinaryClientParityLiveTest' > /tmp/gsd36/exec/golden-tz-096.log 2>&1
git worktree add --detach /tmp/gsd36/fmt HEAD
env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew -p /tmp/gsd36/fmt --console=plain spotlessApply
git commit -F /tmp/gsd36/exec/msg-golden.txt
git worktree add --detach /tmp/gsd36/mut 83d0b42a4e5e92a620577df74e83b684e8327439
python3 -I <flip the first hex digit of the SHA-256 field of the line starting plain|string|rowbinary|false|>
CLICKHOUSE_VERSION=latest CLIENT_VERSION=V2 env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew -p /tmp/gsd36/mut --console=plain test --rerun --tests '*RowBinaryClientParityLiveTest' > /tmp/gsd36/exec/golden-mutation.log 2>&1
python3 -I .planning/phases/36-client-0-11-0-audit-bump/36-evidence-check.py failed /tmp/gsd36/mut/build/test-results/test --expect 'RowBinaryClientParityLiveTest#CLT-06: the RowBinary body of every type case equals the golden hash blessed on client 0.9.6'
grep -c 'plain|string|rowbinary|false|' /tmp/gsd36/mut/build/test-results/test/TEST-com.clickhouse.kafka.connect.sink.RowBinaryClientParityLiveTest.xml
git worktree remove --force /tmp/gsd36/mut
git worktree prune
git diff --quiet 83d0b42a4e5e92a620577df74e83b684e8327439 HEAD -- src/test/resources/rowbinary-golden-sha256.txt
docker run --rm --entrypoint clickhouse clickhouse/clickhouse-server:latest local --query 'SELECT version()'
docker image inspect --format '{{index .RepoDigests 0}}' clickhouse/clickhouse-server:latest
```

```text
assets_identical: yes
bless_exit: 1
bless_message_present: yes
golden_header_line: # blessed on client-v2 0.9.6 (revision: 5777f5a)
golden_data_lines: 96
golden_plain_lines: 48
golden_defaults_lines: 48
golden_defaults_flag_ok: yes
golden_data_sha256: 716b0f7e5de97198e031983d4602f135c0bd14753e6603d7747338e46c1acb6e
verify_exit: 0
verify_xml: tests=1 failures=0 errors=0 skipped=0
tz_la_exit: 0
tz_la_xml: tests=1 failures=0 errors=0 skipped=0
spotless_worktree_cmp: identical
golden_commit: 83d0b42a4e5e92a620577df74e83b684e8327439
golden_commit_files: src/test/java/com/clickhouse/kafka/connect/sink/RowBinaryClientParityLiveTest.java; src/test/java/com/clickhouse/kafka/connect/sink/RowBinaryParityCases.java; src/test/resources/rowbinary-golden-sha256.txt
mutated_line_prefix: plain|string|rowbinary|false|
mutation_exit: 1
mutation_failed: RowBinaryClientParityLiveTest#CLT-06: the RowBinary body of every type case equals the golden hash blessed on client 0.9.6
mutation_prediction_met: yes
mutation_names_case: yes
mut_worktree_removed: yes
golden_unmodified_in_main: yes
hygiene_scan: clean
clickhouse_version: 26.10.3.7
clickhouse_digest: clickhouse/clickhouse-server@sha256:739e099abd1157390960a59e4532d21781d8fde57e6e4151cb920ea02c30b1ce
```

The bless run wrote the golden file and failed on purpose (exit 1), so a run with the switch set can never go green. The data-lines hash equals the planning prototype's, the header names client-v2 0.9.6, and the committed golden is green on 0.9.6 under the workstation zone and under `America/Los_Angeles`. Flipping one hex digit of the `plain|string|rowbinary|false|` line turned the test red and the failure message names that case.

Measured 2026-10-29T12:36:00Z (golden bless, verify, zone run and mutation proof).

Verdict: pass

### E-05: The createClientV2 fix commit: nine added lines, three anchor rows, one FORK-DELTA row, every pin unchanged on 0.9.6

Backs: CLT-03, ROADMAP Phase 36 criterion 2, first half (the two required `createClientV2` fixes land on client 0.9.6 where they are proven no-ops, so the catalog commit of E-06 can be one line and a revert of the bump keeps them).

```sh
git apply --check .planning/phases/36-client-0-11-0-audit-bump/36-assets/createClientV2-fix.patch.txt
git apply .planning/phases/36-client-0-11-0-audit-bump/36-assets/createClientV2-fix.patch.txt
git diff --numstat -- src/main
env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain compileJava --rerun-tasks > /tmp/gsd36/exec/compilejava-fix.log 2>&1
git worktree add --detach /tmp/gsd36/fmt HEAD
env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew -p /tmp/gsd36/fmt --console=plain spotlessApply
git worktree remove --force /tmp/gsd36/fmt
cat .planning/phases/36-client-0-11-0-audit-bump/36-assets/fork-anchors-fix.txt >> .planning/scripts/fork-anchors.txt
python3 -I .planning/phases/36-client-0-11-0-audit-bump/36-evidence-check.py anchors .planning/scripts/fork-anchors.txt
python3 -I <insert the asset row directly below the row starting "| The server-side table filter:" in docs/FORK-DELTA.md>
rm -rf build/test-results/testUnit
env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain testUnit --rerun --tests '*ClientErrorShapePinUnitTest' --tests '*ClientTimeoutPrefixPinUnitTest' --tests '*ClientEndpointAndOptionsPinUnitTest' --tests '*ClientInsertWirePinUnitTest' --tests '*SourcePlanningIdGuardUnitTest' > /tmp/gsd36/exec/pins-fix-096.log 2>&1
rm -rf build/test-results/test
CLICKHOUSE_VERSION=latest CLIENT_VERSION=V2 env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain test --rerun --tests '*Client011BehaviourPinLiveTest' --tests '*RowBinaryClientParityLiveTest' --tests '*ClickHouseSinkJdbcPropertiesTest' > /tmp/gsd36/exec/live-fix-096.log 2>&1
rm -rf build/test-results/testUnit
env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain testUnit --rerun > /tmp/gsd36/exec/testunit-fix.log 2>&1
python3 -I .planning/phases/36-client-0-11-0-audit-bump/36-hygiene-check.py /tmp/gsd36/exec/added-lines-fix.txt
git commit -F /tmp/gsd36/exec/msg-fix.txt
PRE_TAG=pre-v1.7.0-tail-2 UPSTREAM_REF=9bed6f7970670a55c66be8efda246889e1f95bd0 CONFLICTED_PATHS_FILE=.planning/scripts/conflicted-paths-v1.7.0-tail-2.txt bash .planning/scripts/verify-merge-invariants.sh adaf8c4e76375e5099c076f2ca3bace892709765 a1cc33825a07d960bc336e0e6c7474c6698ab40b
docker run --rm --entrypoint clickhouse clickhouse/clickhouse-server:latest local --query 'SELECT version()'
docker image inspect --format '{{index .RepoDigests 0}}' clickhouse/clickhouse-server:latest
```

```text
unfixed_row_counts: endpoint=0; url=1; ignore=0
fix_numstat: ClickHouseHelperClient.java:9/1
import_lines_changed: 0
object_creations_equal: yes
compilejava_exit: 0
deprecation_warnings_096: 100
spotless_numstat_baseline: ClickHouseHelperClient.java=3/4
spotless_numstat_edited: ClickHouseHelperClient.java=3/4
spotless_unchanged: yes
anchor_counts_after: endpoint=1; url=0; ignore=1
anchors_mirror: rows=151 bad=0
fork_delta_numstat: 1/0
pins_dockerfree_exit: 0
error_shape_xml: tests=7 failures=0 errors=0 skipped=0
timeout_prefix_xml: tests=2 failures=0 errors=0 skipped=0
endpoint_xml: tests=3 failures=0 errors=0 skipped=0
wire_xml: tests=2 failures=0 errors=0 skipped=0
planning_id_guard_xml: tests=18 failures=0 errors=0 skipped=0
live_exit: 0
live_xml: tests=6 failures=0 errors=0 skipped=0
golden_xml: tests=1 failures=0 errors=0 skipped=0
jdbc_props_xml: tests=2 failures=0 errors=0 skipped=0
testunit_exit: 0
testunit_tests: 1472
testunit_failures: 0
testunit_errors: 0
testunit_skipped: 2
testunit_delta_vs_baseline: 14
hygiene_scan: clean
fix_commit: a1cc33825a07d960bc336e0e6c7474c6698ab40b
fix_commit_files: .planning/scripts/fork-anchors.txt; docs/FORK-DELTA.md; src/main/java/com/clickhouse/kafka/connect/sink/db/helper/ClickHouseHelperClient.java
tail_gate_exit: 0
tail_gate_line: invariants: provenance_paths=27 anchor_commits=73 anchor_rows=151 marker_commits=80 anchor_exempt=0 result=PASS
clickhouse_version: 26.10.3.7
clickhouse_digest: clickhouse/clickhouse-server@sha256:739e099abd1157390960a59e4532d21781d8fde57e6e4151cb920ea02c30b1ce
```

The patch asset applied cleanly and the commit holds exactly the patch: nine added lines and one removed line in `createClientV2`, no import line, no new object. The zero row `.addEndpoint(url)` read 1 on the unfixed file and reads 0 after the fix, so it could not pass on unfixed code. Spotless keeps the same three added and four removed lines for `ClickHouseHelperClient.java` before and after the edit, so the fix moves no existing hunk. On client 0.9.6 every pin keeps its result (the fixes are no-ops there): 7, 2, 3 and 2 Docker-free executions, the planning-id guard, the 6 live behaviour pins, the golden and the JDBC-properties class are all green, and the cache-busted testUnit is exactly 14 larger than the E-01 baseline. The tail gate over the merge commit of Phase 34 up to the fix commit passes with `anchor_rows=151`.

Measured 2026-10-29T12:44:17Z (fix commit and its gates on client 0.9.6).

Verdict: pass

### E-06: The catalog commit: clickHouseDriver 0.11.0 as one marked override, the pins, BLD-16 and the golden green on 0.11.0

Backs: CLT-02, CLT-03, CLT-04, ROADMAP Phase 36 criteria 2 and 3 (the client moves to 0.11.0 in one revertable commit, and with the two fixes every pin that was red on the bare bump is green, BLD-16 needs no re-bless, and the golden blessed on 0.9.6 still matches).

```sh
cp .planning/phases/36-client-0-11-0-audit-bump/36-assets/ClientCatalogPinUnitTest.java.txt src/test/java/com/clickhouse/kafka/connect/ci/ClientCatalogPinUnitTest.java
cmp <asset> <copy>
rm -rf build/test-results/testUnit
env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain testUnit --rerun --tests '*ClientCatalogPinUnitTest' > /tmp/gsd36/exec/catalog-pin-red.log 2>&1
python3 -I .planning/phases/36-client-0-11-0-audit-bump/36-evidence-check.py failed build/test-results/testUnit --expect 'ClientCatalogPinUnitTest#CLT-02: clickHouseDriver is 0.11.0 and carries the FORK OVERRIDE marker naming upstream 0.9.6'
sed -i '' 's/^clickHouseDriver = "0.9.6"$/clickHouseDriver = "0.11.0"  # FORK OVERRIDE (upstream 0.9.6)/' gradle/libs.versions.toml
sed -i '' "49r .planning/phases/36-client-0-11-0-audit-bump/36-assets/fork-anchors-catalog-row.txt" .planning/scripts/fork-anchors.txt
python3 -I .planning/phases/36-client-0-11-0-audit-bump/36-evidence-check.py anchors .planning/scripts/fork-anchors.txt
env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain dependencies --configuration <runtimeClasspath | clickhouseDependencies | testRuntimeClasspath | integrationTestRuntimeClasspath>
env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain compileJava compileTestFixturesJava compileTestJava compileIntegrationTestJava
env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain compileJava --rerun-tasks > /tmp/gsd36/exec/compilejava-0110.log 2>&1
rm -rf build/test-results/testUnit
env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain testUnit --rerun --tests '*ClientErrorShapePinUnitTest' --tests '*ClientTimeoutPrefixPinUnitTest' --tests '*ClientEndpointAndOptionsPinUnitTest' --tests '*ClientInsertWirePinUnitTest' --tests '*ClientCatalogPinUnitTest' --tests '*UtilsTest' --tests '*AdditionalRetriableErrorCodes*' --tests '*ConnectApiReferencesUnitTest' --tests '*NetworkCompressionPinUnitTest' --tests '*ClickHouseHelperClientForkUnitTest' --tests '*SourcePlanningIdGuardUnitTest' > /tmp/gsd36/exec/pins-0110.log 2>&1
rm -rf build/test-results/test
CLICKHOUSE_VERSION=latest CLIENT_VERSION=V2 env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain test --rerun --tests '*Client011BehaviourPinLiveTest' --tests '*RowBinaryClientParityLiveTest' --tests '*ClickHouseSinkJdbcPropertiesTest' --tests '*NetworkCompressionPinLiveTest' --tests '*Lz4RoundTripLiveTest' > /tmp/gsd36/exec/live-0110.log 2>&1
git worktree add --detach /tmp/gsd36/fmt HEAD
env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew -p /tmp/gsd36/fmt --console=plain spotlessApply
git worktree remove --force /tmp/gsd36/fmt
git commit -F /tmp/gsd36/exec/msg-catalog.txt
rm -rf build/test-results/testUnit
env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain testUnit --rerun > /tmp/gsd36/exec/testunit-0110.log 2>&1
PRE_TAG=pre-v1.7.0-tail-2 UPSTREAM_REF=9bed6f7970670a55c66be8efda246889e1f95bd0 CONFLICTED_PATHS_FILE=.planning/scripts/conflicted-paths-v1.7.0-tail-2.txt bash .planning/scripts/verify-merge-invariants.sh adaf8c4e76375e5099c076f2ca3bace892709765 5ab3410e94fd64e8fa6a8bca6ffd3510b13d68f8
docker run --rm --entrypoint clickhouse clickhouse/clickhouse-server:latest local --query 'SELECT version()'
docker image inspect --format '{{index .RepoDigests 0}}' clickhouse/clickhouse-server:latest
```

```text
assets_identical: yes
catalog_pin_red_exit: 1
catalog_pin_red_failed: ClientCatalogPinUnitTest#CLT-02: clickHouseDriver is 0.11.0 and carries the FORK OVERRIDE marker naming upstream 0.9.6
catalog_pin_red_prediction_met: yes
catalog_numstat: libs.versions.toml:1/1
anchor_row_follows_kafka_row: yes
anchor_row_count: 1
anchors_mirror: rows=152 bad=0
client_v2_runtimeclasspath: +--- com.clickhouse:client-v2:0.11.0
client_v2_clickhousedependencies: +--- com.clickhouse:client-v2:0.11.0
client_v2_testruntimeclasspath: +--- com.clickhouse:client-v2:0.11.0
client_v2_integrationtestruntimeclasspath: +--- com.clickhouse:client-v2:0.11.0
clickhouse_modules_0110: com.clickhouse:client-v2:0.11.0; com.clickhouse:clickhouse-client:0.11.0; com.clickhouse:clickhouse-data:0.11.0; com.clickhouse:clickhouse-http-client:0.11.0
zstd_jni_resolved: +--- com.github.luben:zstd-jni:1.5.7-23
compile_all_exit: 0
deprecation_warnings_0110: 100
deprecation_warnings_equal: yes
pins_exit: 0
error_shape_xml: tests=7 failures=0 errors=0 skipped=0
timeout_prefix_xml: tests=2 failures=0 errors=0 skipped=0
endpoint_xml: tests=3 failures=0 errors=0 skipped=0
wire_xml: tests=2 failures=0 errors=0 skipped=0
catalog_pin_xml: tests=2 failures=0 errors=0 skipped=0
utilstest_tests: 34
additional_tests: 3
bld16_xml: tests=3 failures=0 errors=0 skipped=0
live_exit: 0
live_xml: tests=6 failures=0 errors=0 skipped=0
golden_xml: tests=1 failures=0 errors=0 skipped=0
jdbc_props_xml: tests=2 failures=0 errors=0 skipped=0
netcomp_live_xml: tests=9 failures=0 errors=0 skipped=0
lz4_live_xml: tests=2 failures=0 errors=0 skipped=0
bare_set_green_with_fix: yes
spotless_worktree_cmp: identical
catalog_commit: 5ab3410e94fd64e8fa6a8bca6ffd3510b13d68f8
catalog_commit_files: .planning/scripts/fork-anchors.txt; gradle/libs.versions.toml; src/test/java/com/clickhouse/kafka/connect/ci/ClientCatalogPinUnitTest.java
catalog_commit_diff: -clickHouseDriver = "0.9.6"|+clickHouseDriver = "0.11.0"  # FORK OVERRIDE (upstream 0.9.6)
testunit_exit: 0
testunit_tests: 1474
testunit_failures: 0
testunit_errors: 0
testunit_skipped: 2
testunit_delta_vs_baseline: 16
hygiene_scan: clean
tail_gate_exit: 0
tail_gate_line: invariants: provenance_paths=27 anchor_commits=75 anchor_rows=152 marker_commits=82 anchor_exempt=0 result=PASS
clickhouse_version: 26.10.3.7
clickhouse_digest: clickhouse/clickhouse-server@sha256:739e099abd1157390960a59e4532d21781d8fde57e6e4151cb920ea02c30b1ce
```

The catalog pin was red on the unbumped catalog with exactly its version-line test (the second test, that the four client modules ride one ref, was green on both lines). After the one-line edit all four configurations resolve `client-v2` 0.11.0, and the four client modules (`client-v2`, `clickhouse-client`, `clickhouse-data`, `clickhouse-http-client`) all read 0.11.0. `zstd-jni` 1.5.7-23 comes in through the client's own POM and no other module version moved. The four compile tasks pass and the deprecation-warning count is 100 on both lines, so 0.11.0 deprecates nothing the connector uses beyond what 0.9.6 did. On 0.11.0 with the fixes every Docker-free pin is green, the Kafka and Connect API reference list (BLD-16) needs no re-bless, `UtilsTest` and the `AdditionalRetriableErrorCodes` class keep their baseline counts, and the live classes are green on ClickHouse 26.10.3.7 with client V2: the six behaviour pins, the golden blessed on 0.9.6, the JDBC-properties class, the compression pin and the lz4 round trip. The three bare-bump live reds of E-03 and the three Docker-free ones are green with the fixes. The cache-busted testUnit is exactly 16 larger than the E-01 baseline.

Measured 2026-10-29T12:54:55Z (catalog commit, pins and gates on client 0.11.0).

Verdict: pass

### E-07: CLT-07 local halves: non-UTC date pins, the V1-mode run and the Connect-worker integrationTest on 0.11.0

Backs: CLT-07 (first two halves) and ROADMAP Phase 36 criterion 3 (the date and datetime live pins pass on a non-UTC JVM, the connector runs the V1 client mode on 0.11.0, and the Connect-worker integrationTest passes with client 0.11.0 inside the shaded archive).

```sh
git worktree add --detach /tmp/gsd36/tz HEAD
cp .planning/phases/36-client-0-11-0-audit-bump/36-assets/ZzTzProbeTest.java.txt /tmp/gsd36/tz/src/test/java/com/clickhouse/kafka/connect/ZzTzProbeTest.java
TZ=<zone> EXPECTED_ZONE=<zone> env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew -p /tmp/gsd36/tz --console=plain testUnit --rerun --tests '*ZzTzProbeTest'
TZ=Asia/Kolkata EXPECTED_ZONE=UTC env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew -p /tmp/gsd36/tz --console=plain testUnit --rerun --tests '*ZzTzProbeTest'
python3 -I .planning/phases/36-client-0-11-0-audit-bump/36-evidence-check.py failed /tmp/gsd36/tz/build/test-results/testUnit --expect 'ZzTzProbeTest#jvmDefaultZoneIsTheRequestedOne'
git worktree remove --force /tmp/gsd36/tz
git worktree prune
readlink /etc/localtime
rm -rf build/test-results/test
TZ=<UTC | America/Los_Angeles | Asia/Kolkata | Pacific/Kiritimati> CLICKHOUSE_VERSION=latest CLIENT_VERSION=V2 env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain test --rerun --tests '*ClickHouseSinkTaskWithSchemaTest.supportDatesTest' --tests '*ClickHouseSinkTaskWithSchemaTest.supportArrayDateTime64Test' --tests '*ClickHouseSinkTaskWithSchemaTest.supportZonedDatesStringTest' --tests '*ClickHouseSinkTaskWithSchemaTest.supportFormattedDatesStringTest' --tests '*ClickHouseSinkTaskWithSchemaTest.testAvroDateAndTimeTypes' --tests '*MetadataFreeJsonTypeParityLiveTest' --tests '*ClickHouseSinkTaskWithSchemaProxyTest.supportDatesTest' > /tmp/gsd36/exec/dates-<zone-key>.log 2>&1
rm -rf build/test-results/test
CLICKHOUSE_VERSION=latest CLIENT_VERSION=V1 env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain test --rerun --tests '*ClickHouseSinkJdbcPropertiesTest' --tests '*NetworkCompressionPinLiveTest' --tests '*ClickHouseSinkTaskSchemalessTest' --tests '*ClickHouseSinkTaskStringTest' > /tmp/gsd36/exec/v1-0110.log 2>&1
git diff --quiet 5ab3410e94fd64e8fa6a8bca6ffd3510b13d68f8 HEAD -- src build.gradle.kts gradle .github
rm -rf build/test-results/integrationTest build/reports/tests/integrationTest
/bin/bash -c 'CLICKHOUSE_VERSION=latest CLIENT_VERSION=V2 env -u TESTCONTAINERS_HOST_OVERRIDE JAVA_HOME=$HOME/.gradle/jdks/adoptium-17-aarch64-hotspot-mac/Contents/Home ./gradlew --console=plain integrationTest > /tmp/gsd36/exec/integration.log 2>&1; echo $? > /tmp/gsd36/exec/integration.exit'
python3 -I .planning/phases/36-client-0-11-0-audit-bump/36-evidence-check.py junit build/test-results/integrationTest [--class <ClickHouseSinkConnectorIntegrationTest | ExactlyOnceTest | ClickHouseCloudTest>]
unzip -p build/confluentArchive/clickhouse-kafka-connect-v1.7.0_gg5/lib/clickhouse-kafka-connect-v1.7.0_gg5-confluent.jar clickhouse-client-version.properties
docker run --rm --entrypoint clickhouse clickhouse/clickhouse-server:latest local --query 'SELECT version()'
docker image inspect --format '{{index .RepoDigests 0}}' clickhouse/clickhouse-server:latest
```

```text
tz_probe_la: pass
tz_probe_kolkata: pass
tz_probe_kiritimati: pass
tz_probe_negative_control: red
tz_worktree_removed: yes
workstation_zone: Europe/Tallinn
date_pins_selectors: ClickHouseSinkTaskWithSchemaTest.{supportDatesTest, supportArrayDateTime64Test, supportZonedDatesStringTest, supportFormattedDatesStringTest, testAvroDateAndTimeTypes}; MetadataFreeJsonTypeParityLiveTest (all); ClickHouseSinkTaskWithSchemaProxyTest.supportDatesTest
date_pins_utc_exit: 0
date_pins_utc_xml: tests=15 failures=0 errors=0 skipped=0 files=3
date_pins_la_exit: 0
date_pins_la_xml: tests=15 failures=0 errors=0 skipped=0 files=3
date_pins_kolkata_exit: 0
date_pins_kolkata_xml: tests=15 failures=0 errors=0 skipped=0 files=3
date_pins_kiritimati_exit: 0
date_pins_kiritimati_xml: tests=15 failures=0 errors=0 skipped=0 files=3
date_pins_la_schema_xml: tests=5 failures=0 errors=0 skipped=0
date_pins_la_metadata_free_xml: tests=9 failures=0 errors=0 skipped=0
date_pins_la_proxy_xml: tests=1 failures=0 errors=0 skipped=0
v1_exit: 0
v1_xml: tests=35 failures=0 errors=0 skipped=0 files=4
integration_head: 3d0b046118b4f2514655a76b66be72ee66ce8764
utc_start: 2026-10-29T13:02:19Z
utc_end: 2026-10-29T13:34:04Z
integration_exit: 0
integration_duration: BUILD SUCCESSFUL in 31m 30s
integration_summary: Tests summary: 55 tests, 54 succeeded, 0 failed, 1 skipped
integration_java_home: /Users/alexandr.chashnikov/.gradle/jdks/adoptium-17-aarch64-hotspot-mac/Contents/Home
integration_connector_class_xml: tests=54 failures=0 errors=0 skipped=0 files=1
integration_skipped_classes: ExactlyOnceTest tests=8 failures=0 errors=0 skipped=8; ClickHouseCloudTest tests=1 failures=0 errors=0 skipped=1
integration_all_xml: tests=63 failures=0 errors=0 skipped=9 files=3
integration_vs_phase31: Same shape as Phase 31 E-18 (55 tests, 54 succeeded, 0 failed, 1 skipped, HotSpot Temurin 17): the connector class ran 54 executions green; the 31 minute wall time is about 2 minutes above the 29.5 minutes recorded there.
integration_archive_client_version: version=0.11.0
integration_clickhouse_version: 26.10.3.7
integration_jvm_crash: none
clickhouse_version: 26.10.3.7
clickhouse_digest: clickhouse/clickhouse-server@sha256:739e099abd1157390960a59e4532d21781d8fde57e6e4151cb920ea02c30b1ce
```

The zone reaches the test JVM: the probe, which asserts that the JVM default zone id equals the requested one, passed under `America/Los_Angeles`, `Asia/Kolkata` and `Pacific/Kiritimati`, and a mismatched request (`TZ=Asia/Kolkata`, `EXPECTED_ZONE=UTC`) went red naming exactly the probe test. The 15 date and datetime live executions (5 in the schema class, 9 in the metadata-free parity class, 1 in the proxy class) are green on client 0.11.0 under the UTC control and under the three non-UTC zones; the workstation's own zone is `Europe/Tallinn` and CI runs UTC. The V1-mode run (35 executions in 4 files) is green, which also shows the createClientV2 fixes let a V1-mode connector build its V2 client on 0.11.0.

The Connect-worker integrationTest ran with the HotSpot Temurin 17 toolchain against the shaded archive built from the catalog commit tree (`git diff --quiet` between the catalog commit and the starting head over `src`, `build.gradle.kts`, `gradle` and `.github` exited 0), and the archive's own `clickhouse-client-version.properties` reads `version=0.11.0`, so the worker ran client 0.11.0. Exit 0, no failure and no error in any class; the connector class ran 54 executions with none skipped. The `ExactlyOnceTest` (8) and `ClickHouseCloudTest` (1) executions are the skipped ones, aborted in `@BeforeAll` through assumptions because no cluster or cloud properties are set, which matches Gradle's own summary of 55 tests, 54 succeeded, 0 failed, 1 skipped.

CLT-07's second half, the manual nightly dispatch, is the user's step and is not part of this entry: it needs a scratch branch on origin, and nothing was pushed or dispatched here.

Measured 2026-10-29T13:34:45Z (zone probes, four date runs, V1-mode run and integrationTest).

Verdict: pass

### E-08: The RowBinary byte comparison: the 0.9.6 golden holds on 0.11.0 under four zones, the wire is the same, no clean stop

Backs: CLT-06 and ROADMAP Phase 36 criterion 4 (the RowBinary golden blessed on client 0.9.6 is verified on client 0.11.0 and a `BEFORE_PROCESSING` batch replayed across the upgrade carries identical bytes and the same deduplication token).

```sh
git diff --quiet 83d0b42a4e5e92a620577df74e83b684e8327439 HEAD -- src/test/resources/rowbinary-golden-sha256.txt
sed -n 2p src/test/resources/rowbinary-golden-sha256.txt
grep -v '^#' src/test/resources/rowbinary-golden-sha256.txt | shasum -a 256
grep -vc '^#' src/test/resources/rowbinary-golden-sha256.txt
env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain dependencies --configuration testRuntimeClasspath
rm -rf build/test-results/test
[TZ=<zone>] CLICKHOUSE_VERSION=latest CLIENT_VERSION=V2 env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain test --rerun --tests '*RowBinaryClientParityLiveTest'
python3 -I .planning/phases/36-client-0-11-0-audit-bump/36-evidence-check.py junit build/test-results/test --class RowBinaryClientParityLiveTest
rm -rf build/test-results/testUnit
env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain testUnit --rerun --tests '*ClientInsertWirePinUnitTest' --tests '*ClientEndpointAndOptionsPinUnitTest'
python3 -I .planning/phases/36-client-0-11-0-audit-bump/36-evidence-check.py junit build/test-results/testUnit --class ClientInsertWirePinUnitTest
python3 -I .planning/phases/36-client-0-11-0-audit-bump/36-evidence-check.py junit build/test-results/testUnit --class ClientEndpointAndOptionsPinUnitTest
git diff --quiet 99b9d85c15ce1a9b76f5e843b9aec472d24f9708 HEAD -- src/main/java/com/clickhouse/kafka/connect/util/QueryIdentifier.java
git diff --quiet 99b9d85c15ce1a9b76f5e843b9aec472d24f9708 HEAD -- src/main/java/com/clickhouse/kafka/connect/sink/db/ClickHouseWriter.java src/main/java/com/clickhouse/kafka/connect/sink/processing src/main/java/com/clickhouse/kafka/connect/sink/state
docker run --rm --entrypoint clickhouse clickhouse/clickhouse-server:latest local --query 'SELECT version()'
docker image inspect --format '{{index .RepoDigests 0}}' clickhouse/clickhouse-server:latest
```

```text
golden_commit: 83d0b42a4e5e92a620577df74e83b684e8327439
catalog_commit: 5ab3410e94fd64e8fa6a8bca6ffd3510b13d68f8
golden_not_reblessed: yes
golden_header_line: # blessed on client-v2 0.9.6 (revision: 5777f5a)
golden_data_sha256: 716b0f7e5de97198e031983d4602f135c0bd14753e6603d7747338e46c1acb6e
golden_lines: 96
client_v2_test_runtime: +--- com.clickhouse:client-v2:0.11.0
golden_0110_default_exit: 0
golden_0110_default_xml: tests=1 failures=0 errors=0 skipped=0
golden_0110_utc_exit: 0
golden_0110_utc_xml: tests=1 failures=0 errors=0 skipped=0
golden_0110_la_exit: 0
golden_0110_la_xml: tests=1 failures=0 errors=0 skipped=0
golden_0110_kiritimati_exit: 0
golden_0110_kiritimati_xml: tests=1 failures=0 errors=0 skipped=0
wire_xml: tests=2 failures=0 errors=0 skipped=0
endpoint_xml: tests=3 failures=0 errors=0 skipped=0
dedup_token_source: QueryIdentifier.getDeduplicationToken: return String.format("%s-%s-%s-%s", topic, partition, minOffset, maxOffset);
dedup_token_code_unchanged: yes
writer_and_state_unchanged: yes
clean_stop_required: no
replay_statement: A BEFORE_PROCESSING batch replayed after the upgrade is rebuilt by the new task from the stored offset range, serialised by the same writer into the same bytes (the golden: E-04 on 0.9.6 and this entry on 0.11.0), and sent on the same path with the same body and the same insert_deduplication_token (the wire pin), so ClickHouse's block deduplication sees the block the old task would have sent; the User-Agent header is the only request header that differs.
byte_comparison_scope: 48 type cases x 2 variants (96 lines), including nested Tuple and Map nullable markers; a column type outside the matrix, a time-zoned DateTime and a Variant fed plain text were not compared
byte_comparison_labels: measured: the golden on 0.9.6 and on 0.11.0 under four zones, the wire pin; read from the code: the token format and that the writer and state package are unchanged
hygiene_scan: clean
clickhouse_version: 26.10.3.7
clickhouse_digest: clickhouse/clickhouse-server@sha256:739e099abd1157390960a59e4532d21781d8fde57e6e4151cb920ea02c30b1ce
```

The golden file is read, never written: `ROWBINARY_GOLDEN_BLESS` was not set, the file is byte-identical to its 0.9.6 commit and its data lines hash to the value E-04 recorded. A green `RowBinaryClientParityLiveTest` means the length and SHA-256 of the exact body of every one of the 96 lines equals the 0.9.6 line. The four zones are the workstation default (no `TZ`), `UTC`, `America/Los_Angeles` and `Pacific/Kiritimati`; plan 36-02's E-07 proved that `TZ` reaches the test JVM. The wire pin shows a 100,000-byte body, the `insert_deduplication_token` and no `Content-Encoding` arriving unchanged on 0.11.0, and a User-Agent that names the connector and `clickhouse-java-v2/0.11.0`. The token is built from the batch (`topic-partition-minOffset-maxOffset`), not from the client, and the query-id, writer, processing and state sources are unchanged since the phase start, which is why no clean stop is required for the compared matrix.

Measured 2026-10-29T13:41:00Z (four golden runs, the wire pins, the code reads).

Verdict: pass

### E-09: The Confluent archive before and after the bump: +131 inner entries, zstd-jni with three natives, guava redundant

Backs: CLT-08 and ROADMAP Phase 36 criterion 5, record half (the Confluent archive is listed before and after the bump and compared entry by entry; the `zstd-jni` addition with its natives and the redundant guava override are recorded).

```sh
git worktree add --detach /tmp/gsd36/arch-before 5ab3410e94fd64e8fa6a8bca6ffd3510b13d68f8^
git worktree add --detach /tmp/gsd36/arch-after 5ab3410e94fd64e8fa6a8bca6ffd3510b13d68f8
grep '^clickHouseDriver' <worktree>/gradle/libs.versions.toml
env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew -p /tmp/gsd36/arch-<before|after> --console=plain createConfluentArchive
python3 -I .planning/scripts/compare-confluent-archives.py <before>.zip <after>.zip <empty-allowlist>
unzip -l <zip>
unzip -p <zip> '*/lib/*-confluent.jar' > <fresh dir>/<before|after>-inner.jar
unzip -l <inner> | awk 'NR>3 && NF>=4 {print $4"\t"$1}' | LC_ALL=C sort
unzip -p <inner> clickhouse-client-version.properties
shasum -a 256 <gradle cache>/zstd-jni-1.5.7-23-cloud.jar
unzip -Z1 <gradle cache>/zstd-jni-1.5.7-23-cloud.jar
unzip -Z1 <gradle cache>/zstd-jni-1.5.7-23.jar
env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew -p /tmp/gsd36/arch-<before|after> --console=plain dependencies --configuration clickhouseDependencies
env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew -p /tmp/gsd36/arch-after --console=plain dependencyInsight --dependency com.google.guava:guava --configuration clickhouseDependencies
git worktree add --detach /tmp/gsd36/guava 5ab3410e94fd64e8fa6a8bca6ffd3510b13d68f8
sed -i '' '/libs\.guava/d' /tmp/gsd36/guava/build.gradle.kts
env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew -p /tmp/gsd36/guava --console=plain dependencies --configuration <runtimeClasspath|clickhouseDependencies>
git worktree remove --force /tmp/gsd36/<arch-before|arch-after|guava>
git worktree prune
docker run --rm --entrypoint clickhouse clickhouse/clickhouse-server:latest local --query 'SELECT version()'
docker image inspect --format '{{index .RepoDigests 0}}' clickhouse/clickhouse-server:latest
```

```text
catalog_commit: 5ab3410e94fd64e8fa6a8bca6ffd3510b13d68f8
before_catalog_line: clickHouseDriver = "0.9.6"
after_catalog_line: clickHouseDriver = "0.11.0"  # FORK OVERRIDE (upstream 0.9.6)
archive_version_file: v1.7.0_gg5
before_build_exit: 0
after_build_exit: 0
verify_before: verifyConfluentArchive: 7802 inner entries, no nested jar, no Kafka API class
verify_after: verifyConfluentArchive: 7933 inner entries, no nested jar, no Kafka API class
zip_bytes_before: 12456907
zip_bytes_after: 13558637
compare_exit: 1
compare_totals: totals files-a 7400 files-b 7513 only-a 4 only-b 117 changed 94 same 7302
compare_manifest: MANIFEST.MF: identical
compare_nested_jars_in_b: nested jars in b: 0
compare_kafka_classes_in_b: kafka classes in b: 0
only_a_entries: client-v2-version.properties; com/clickhouse/client/api/internal/ClickHouseLZ4InputStream.class; com/clickhouse/client/api/internal/ClickHouseLZ4OutputStream.class; com/clickhouse/client/api/internal/LZ4Entity.class
zstd_classes_added: 37
zstd_multirelease_classes_added: 17
zstd_natives_added: darwin/aarch64/libzstd-jni-1.5.7-23.dylib; linux/aarch64/libzstd-jni-1.5.7-23.so; linux/amd64/libzstd-jni-1.5.7-23.so
guava_entries_in_diff: 0
connector_classes_changed: 0
inner_listing_lines_before: 7802
inner_listing_lines_after: 7933
inner_client_version_before: version=0.9.6
inner_client_version_after: version=0.11.0
zstd_cloud_jar_sha256: 37589ad51ad534e4fcee7cd5a3e006d73fac9d98dc3f06c07f35eadab432e7b3
zstd_cloud_natives: darwin/aarch64/libzstd-jni-1.5.7-23.dylib; linux/aarch64/libzstd-jni-1.5.7-23.so; linux/amd64/libzstd-jni-1.5.7-23.so
zstd_plain_natives_count: 18
zstd_license: BSD 2-Clause License
zstd_declared_by_client_v2_pom: yes
deps_before_guava: com.google.guava:guava:33.4.6-jre -> 33.7.2-jre
deps_after_guava: com.google.guava:guava:33.7.2-jre
guava_arrow_after: no
deps_before_httpclient5: org.apache.httpcomponents.client5:httpclient5:5.4.4 -> 5.6.5
deps_after_httpclient5: org.apache.httpcomponents.client5:httpclient5:5.6.4 -> 5.6.5
deps_before_lz4: at.yawk.lz4:lz4-java:1.10.2 -> 1.12.1
deps_after_lz4: at.yawk.lz4:lz4-java:1.11.4 -> 1.12.1
deps_after_zstd: com.github.luben:zstd-jni:1.5.7-23
guava_requested_by_client_after: yes
guava_experiment_numstat: 0/2
guava_without_override_runtime: 33.7.2-jre
guava_without_override_clickhouse_deps: 33.7.2-jre
worktrees_removed: yes
listings_commit: 4b4d488133189f637baa1f2fc75fa327eccbcba6
listings_commit_files: .planning/phases/36-client-0-11-0-audit-bump/36-ARCHIVE-DIFF.txt; .planning/phases/36-client-0-11-0-audit-bump/36-ARCHIVE-INNER-AFTER.txt; .planning/phases/36-client-0-11-0-audit-bump/36-ARCHIVE-INNER-BEFORE.txt; .planning/phases/36-client-0-11-0-audit-bump/36-ARCHIVE-OUTER-AFTER.txt; .planning/phases/36-client-0-11-0-audit-bump/36-ARCHIVE-OUTER-BEFORE.txt
hygiene_scan: clean
clickhouse_version: 26.10.3.7
clickhouse_digest: clickhouse/clickhouse-server@sha256:739e099abd1157390960a59e4532d21781d8fde57e6e4151cb920ea02c30b1ce
```

Both archives came from detached worktrees at the commit before the catalog commit (the createClientV2 fixes in, catalog still 0.9.6) and at the catalog commit itself, so the only source difference is the catalog line and what it pulls in; the comparison script ran with an empty allow-list, so its exit status 1 and `RESULT: FAIL` are by design (every difference is UNALLOWED and the script cannot tell an intended bump from a surprise, which is a Phase 37 allow-list task). The archives were read with `unzip -l` and `unzip -p` only; the two inner jars were piped into a fresh directory and never extracted or loaded.

What changed: the four entries only in the before archive are the client V2 version properties file and the three LZ4 stream and entity classes that 0.11.0 removed; the 117 only-after files make the inner jar 113 files larger (7400 to 7513) and 18 directory entries larger (402 to 420), the 131 more inner entries, and include 37 `com/github/luben/zstd/` classes, 17 multi-release classes under `META-INF/versions/22/com/github/luben/zstd/` and exactly three native libraries. `zstd-jni` 1.5.7-23 arrives only through client-v2 0.11.0's own POM, as a compile dependency with the `cloud` classifier. That classifier carries natives for darwin/aarch64, linux/aarch64 and linux/amd64 only, against 18 in the plain jar; that a worker on any other platform (linux/arm, i386, ppc64le, s390x, riscv64, Windows, FreeBSD, AIX, Intel macOS) cannot load the native when a ZSTD path runs is read from the library's loader in the research and was not re-measured here. No connector class, no guava class and no Kafka API class changed, and the manifest is identical.

The guava override is redundant, not lost: with both `libs.guava` lines removed from `build.gradle.kts` in a throwaway worktree (`0/2` numstat), guava 33.7.2-jre still resolves on `runtimeClasspath` and on `clickhouseDependencies`, requested by `com.clickhouse:client-v2:0.11.0`, and the archive diff holds no guava entry. The httpclient5 and lz4 pins still win (`5.6.4 -> 5.6.5`, `1.11.4 -> 1.12.1`). `build.gradle.kts` was not edited in this phase; the stale `// temp to override client's with CVE` comment and the pin are a follow-up recorded for plan 36-04's rollout items.

Measured 2026-10-29T13:42:00Z (two archive builds, the comparison, the listings, the dependency trees and the guava experiment).

Verdict: pass

### E-10: The D-17 rationale reworded to the 0.11.0 measurements, in three places, and the client-override wording

Backs: CLT-05, CLT-02, CLT-08 and ROADMAP Phase 36 criterion 5, record half (the D-17 default stays and its rationale is reworded to the 0.11.0 measurements in the Javadoc, `docs/FORK-DELTA.md` and the gg1 rollout warning; the client override, the redundant guava override and the httpclient5 pin are worded in FORK-DELTA).

```sh
python3 -I /tmp/gsd36/exec/apply-docs.py .planning/phases/36-client-0-11-0-audit-bump/36-assets/docs-rewording.txt
git diff --name-only -- docs src
git diff -U0 -- src/test/java/com/clickhouse/kafka/connect/sink/db/helper/NetworkCompressionPinUnitTest.java src/test/java/com/clickhouse/kafka/connect/sink/db/helper/NetworkCompressionPinLiveTest.java | grep -E '^[-+][^-+]' | grep -vE '^[-+][[:space:]]*(/[*][*]|[*])'
git diff --numstat -- docs/FORK-DELTA.md
git diff --numstat -- docs/ROLLOUT-v1.7.0_gg1.md
grep '^| Fork dependency pins in the version catalog:' docs/FORK-DELTA.md | grep -F <required text>
grep "^| D-17's unconditional" docs/FORK-DELTA.md | grep -F <required text>
grep "^| Upstream's guava override" docs/FORK-DELTA.md | grep -F <required text>
grep "^| Upstream's httpclient5 pin" docs/FORK-DELTA.md | grep -F <required text>
grep '^An eighth behaviour' docs/FORK-DELTA.md | grep -F 'Magic is not correct'
grep -F <required text> docs/ROLLOUT-v1.7.0_gg1.md
git diff --quiet -- .planning/MERGE-CHECKLIST-v1.7.0.md
env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain compileTestJava
rm -rf build/test-results/testUnit
env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain testUnit --rerun --tests '*NetworkCompressionPinUnitTest' --tests '*SourcePlanningIdGuardUnitTest'
python3 -I .planning/phases/36-client-0-11-0-audit-bump/36-evidence-check.py junit build/test-results/testUnit --class NetworkCompressionPinUnitTest
git worktree add --detach /tmp/gsd36/fmt HEAD
env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew -p /tmp/gsd36/fmt --console=plain spotlessApply
git -C /tmp/gsd36/fmt diff --numstat -- <the two test files>
git -C /tmp/gsd36/fmt checkout -- .
cp <the two test files> /tmp/gsd36/fmt/<same relative paths>
env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew -p /tmp/gsd36/fmt --console=plain spotlessApply
cmp <each test file> /tmp/gsd36/fmt/<same relative path>
git worktree remove --force /tmp/gsd36/fmt
git worktree prune
git diff -U0 -- docs src | grep '^+' > /tmp/gsd36/exec/added-lines-docs.txt
python3 -I .planning/phases/36-client-0-11-0-audit-bump/36-hygiene-check.py /tmp/gsd36/exec/added-lines-docs.txt
docker run --rm --entrypoint clickhouse clickhouse/clickhouse-server:latest local --query 'SELECT version()'
```

```text
docs_files_changed: docs/FORK-DELTA.md; docs/ROLLOUT-v1.7.0_gg1.md; src/test/java/com/clickhouse/kafka/connect/sink/db/helper/NetworkCompressionPinLiveTest.java; src/test/java/com/clickhouse/kafka/connect/sink/db/helper/NetworkCompressionPinUnitTest.java
javadoc_only: yes
fork_delta_numstat: 5/5
rollout_numstat: 2/2
catalog_row_ok: yes
d17_row_ok: yes
guava_row_ok: yes
httpclient_row_ok: yes
paragraph_ok: yes
gg1_note_ok: yes
recorded_fact_26_untouched: yes
compile_test_exit: 0
docs_tests_exit: 0
netcomp_unit_xml: tests=9 failures=0 errors=0 skipped=0
guard_unit_xml: tests=18 failures=0 errors=0 skipped=0
spotless_numstat_baseline: none (0 hunks on both files)
spotless_numstat_edited: none (0 hunks: the formatted copies are byte-identical to the edited files)
spotless_unchanged: yes
hygiene_scan: clean
docs_commit: 375c93be91d2a32eafd4e0364f905e51f8dfd280
docs_commit_files: docs/FORK-DELTA.md; docs/ROLLOUT-v1.7.0_gg1.md; src/test/java/com/clickhouse/kafka/connect/sink/db/helper/NetworkCompressionPinLiveTest.java; src/test/java/com/clickhouse/kafka/connect/sink/db/helper/NetworkCompressionPinUnitTest.java
clickhouse_version: 26.10.3.7
```

The D-17 default itself (`setCompressionMethod` true with `compress=false`) is untouched: no main file and no assertion changed, and every changed line of the two test files is a Javadoc comment line. The 15 edit blocks of the planning asset each matched exactly once and were applied in order with a script that asserts a single match per block and writes the result (a byte-for-byte equivalent of the Edit tool). The reworded text says what E-06 measured on 0.11.0 and what Recorded Fact 26 measured on 0.9.6: V2 of client 0.11.0 decodes ZSTD and loads the `zstd-jni` native, client V1 still fails with `Magic is not correct - expect [-126] but got [-112]` on both client lines, and client 0.9.6 fails on V2 with the LZ4 magic-byte error; each of these is pinned by `Client011BehaviourPinLiveTest`. The claim that a worker whose operator set `compress=true` with the lz4 setting never loads a zstd native, and the executable temp folder requirement, are labelled in the text as measured in the public confluentinc/cp-kafka-connect-base:7.9.7 image during the research, not pinned. The catalog row now carries the override marker, `CLT-02, CLT-09, BLD-08` and `ClientCatalogPinUnitTest`, and the sentence calling 0.9.6 upstream's own value is gone; the guava row says `redundant, not lost` with `E-09`; the httpclient5 row records `5.6.4 -> 5.6.5`. Recorded Fact 26 in the merge checklist is unchanged and stays the dated 0.9.6 record, cited beside this entry. The two test classes compile and `NetworkCompressionPinUnitTest` (9) and `SourcePlanningIdGuardUnitTest` (18) are green; their E-06 tally for the first class was also 9.

Measured 2026-10-29T13:43:31Z (shape checks, compile, Docker-free classes, per-file formatting).

Verdict: pass

### E-11: Rollout items for Phase 38, the custom_ probe and the CLT-09 revert rehearsal

Backs: CLT-09, CLT-05, CLT-06, CLT-08 and ROADMAP Phase 36 criterion 5 (the operator-visible effects of the bump are recorded with evidence labels for Phase 38, the one operator-breaking effect is measured on both client lines, and the CLT-09 revert is rehearsed on the final history in a detached worktree before the gate decides).

```sh
git worktree add --detach /tmp/gsd36/rehearse HEAD
cp .planning/phases/36-client-0-11-0-audit-bump/36-assets/ZzCustomProbeTest.java.txt /tmp/gsd36/rehearse/src/test/java/com/clickhouse/kafka/connect/sink/db/helper/ZzCustomProbeTest.java
env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew -p /tmp/gsd36/rehearse --console=plain testUnit --rerun --tests '*ZzCustomProbeTest' -i > /tmp/gsd36/exec/custom-probe-0110.log 2>&1
rm /tmp/gsd36/rehearse/src/test/java/com/clickhouse/kafka/connect/sink/db/helper/ZzCustomProbeTest.java
git -C /tmp/gsd36/rehearse revert --no-edit 375c93be91d2a32eafd4e0364f905e51f8dfd280
git -C /tmp/gsd36/rehearse revert --no-edit 5ab3410e94fd64e8fa6a8bca6ffd3510b13d68f8
git -C /tmp/gsd36/rehearse show --name-only --format= HEAD
grep '^clickHouseDriver' /tmp/gsd36/rehearse/gradle/libs.versions.toml
python3 -I .planning/phases/36-client-0-11-0-audit-bump/36-evidence-check.py anchors .planning/scripts/fork-anchors.txt   # run inside the worktree
env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew -p /tmp/gsd36/rehearse --console=plain dependencies --configuration runtimeClasspath
cp .planning/phases/36-client-0-11-0-audit-bump/36-assets/ZzCustomProbeTest.java.txt /tmp/gsd36/rehearse/src/test/java/com/clickhouse/kafka/connect/sink/db/helper/ZzCustomProbeTest.java
env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew -p /tmp/gsd36/rehearse --console=plain testUnit --rerun --tests '*ZzCustomProbeTest' -i > /tmp/gsd36/exec/custom-probe-096.log 2>&1
rm /tmp/gsd36/rehearse/src/test/java/com/clickhouse/kafka/connect/sink/db/helper/ZzCustomProbeTest.java
rm -rf /tmp/gsd36/rehearse/build/test-results/testUnit
env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew -p /tmp/gsd36/rehearse --console=plain testUnit --rerun --tests '*ClientErrorShapePinUnitTest' --tests '*ClientTimeoutPrefixPinUnitTest' --tests '*ClientEndpointAndOptionsPinUnitTest' --tests '*ClientInsertWirePinUnitTest' --tests '*UtilsTest' --tests '*AdditionalRetriableErrorCodes*' --tests '*ConnectApiReferencesUnitTest' --tests '*SourcePlanningIdGuardUnitTest' > /tmp/gsd36/exec/rehearsal-dockerfree.log 2>&1
python3 -I .planning/phases/36-client-0-11-0-audit-bump/36-evidence-check.py failed /tmp/gsd36/rehearse/build/test-results/testUnit
rm -rf /tmp/gsd36/rehearse/build/test-results/test
CLICKHOUSE_VERSION=latest CLIENT_VERSION=V2 env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew -p /tmp/gsd36/rehearse --console=plain test --rerun --tests '*Client011BehaviourPinLiveTest' --tests '*RowBinaryClientParityLiveTest' --tests '*ClickHouseSinkJdbcPropertiesTest' > /tmp/gsd36/exec/rehearsal-live.log 2>&1
python3 -I .planning/phases/36-client-0-11-0-audit-bump/36-evidence-check.py junit /tmp/gsd36/rehearse/build/test-results/<testUnit | test> --class <each class>
git worktree remove --force /tmp/gsd36/rehearse
git worktree prune
python3 -I .planning/phases/36-client-0-11-0-audit-bump/36-hygiene-check.py .planning/phases/36-client-0-11-0-audit-bump/36-ROLLOUT-ITEMS.md
git add .planning/phases/36-client-0-11-0-audit-bump/36-ROLLOUT-ITEMS.md
git commit -F /tmp/gsd36/exec/msg-rollout.txt
docker run --rm --entrypoint clickhouse clickhouse/clickhouse-server:latest local --query 'SELECT version()'
docker image inspect --format '{{index .RepoDigests 0}}' clickhouse/clickhouse-server:latest
```

```text
docs_commit: 375c93be91d2a32eafd4e0364f905e51f8dfd280
catalog_commit: 5ab3410e94fd64e8fa6a8bca6ffd3510b13d68f8
fix_commit: a1cc33825a07d960bc336e0e6c7474c6698ab40b
rehearsal_base: 52a43e2d9a676b0052caf059544fcc6eb23bc933
custom_probe_exit: 0
custom_probe_uri_0110: /?custom_myparam=7&custom_settings=max_threads%253D3&query=INSERT%20INTO%20t%20FORMAT%20RowBinary&network_compression_method=lz4
custom_forwarded_on_0110: yes
revert_docs_exit: 0
revert_docs_files: docs/FORK-DELTA.md; docs/ROLLOUT-v1.7.0_gg1.md; src/test/java/com/clickhouse/kafka/connect/sink/db/helper/NetworkCompressionPinLiveTest.java; src/test/java/com/clickhouse/kafka/connect/sink/db/helper/NetworkCompressionPinUnitTest.java
revert_catalog_exit: 0
revert_catalog_files: .planning/scripts/fork-anchors.txt; gradle/libs.versions.toml; src/test/java/com/clickhouse/kafka/connect/ci/ClientCatalogPinUnitTest.java
revert_conflicts: none
rehearsal_catalog_line: clickHouseDriver = "0.9.6"
rehearsal_anchors_mirror: rows=151 bad=0
rehearsal_client_v2: +--- com.clickhouse:client-v2:0.9.6
rehearsal_catalog_pin_absent: yes
rehearsal_fixes_kept: yes
rehearsal_fork_delta_row_kept: yes
rehearsal_golden_kept: yes
custom_probe_exit_096: 0
custom_probe_uri_096: /?query=INSERT%20INTO%20t%20FORMAT%20RowBinary&network_compression_method=lz4
custom_forwarded_on_096: no
rehearsal_dockerfree_exit: 0
rehearsal_dockerfree_failed: none
rehearsal_error_shape_xml: tests=7 failures=0 errors=0 skipped=0
rehearsal_timeout_prefix_xml: tests=2 failures=0 errors=0 skipped=0
rehearsal_endpoint_xml: tests=3 failures=0 errors=0 skipped=0
rehearsal_wire_xml: tests=2 failures=0 errors=0 skipped=0
rehearsal_live_exit: 0
rehearsal_live_failed: none
rehearsal_live_xml: tests=6 failures=0 errors=0 skipped=0
rehearsal_golden_xml: tests=1 failures=0 errors=0 skipped=0
rehearsal_jdbc_xml: tests=2 failures=0 errors=0 skipped=0
rehearse_worktree_removed: yes
rollout_items_file: 36-ROLLOUT-ITEMS.md
rollout_items_commit: 7464977bed6efe53c20827e6ef3bc38459a482bc
rollout_items_count: 14
rollout_sections: Items; Exactly-once replay across the upgrade; zstd-jni: platforms and the temp folder; custom_ keys in jdbcConnectionProperties; What to do; Handoffs
rollout_labels: measured; read from the code; measured in research (not re-run)
rollout_applies_values: either client; client 0.11.0 ships
gg6_rollout_note_absent: yes
hygiene_scan: clean
clickhouse_version: 26.10.3.7
clickhouse_digest: clickhouse/clickhouse-server@sha256:739e099abd1157390960a59e4532d21781d8fde57e6e4151cb920ea02c30b1ce
```

The probe builds a V2 client with `?custom_settings=max_threads%3D3&custom_myparam=7&ssl=true` against the in-process stub and prints the request URI: client 0.11.0 forwards both `custom_` keys and doubles the operator's percent-encoding (`%3D` becomes `%253D`), client 0.9.6 forwards neither. The rehearsal ran in one detached worktree at the committed head: the documentation commit and then the catalog commit reverted with no conflict, the catalog revert touched exactly the catalog line, `ClientCatalogPinUnitTest` and the one anchor row, and the worktree then showed the 0.9.6 catalog line, `rows=151 bad=0`, `client-v2` 0.9.6, and the two `createClientV2` fixes, their FORK-DELTA row and the golden still in place. The pins that carry a 0.9.6 row (error shapes 7, timeout prefixes 2, endpoint and options 3, insert wire 2), `UtilsTest`, the `AdditionalRetriableErrorCodes` classes, the Kafka and Connect API reference list and the planning-id guard are green on 0.9.6 with no failing testcase; the live class (6), the golden (1) and the JDBC-properties class (2) are green on ClickHouse `latest` with client V2. The probe file was removed from the worktree after each of its two runs, so it is not part of either revert, and the main tree and the branch were not touched by the rehearsal. The 14 rollout items each carry a label, an Applies-when value and an evidence citation; claims that were only read from the code or measured in research are labelled so, and the `custom_` alternative stays a user decision.

Measured 2026-10-29T13:50:27Z (probe on both lines, the revert rehearsal and its green runs on 0.9.6).

Verdict: pass

### E-12: Phase 36 gate: testUnit, two full suites on latest with client V2 on 0.11.0, the anchors mirror, the tail and cumulative gates, and the go decision

Backs: CLT-09 and ROADMAP Phase 36 criterion 5 and its phase gate (cache-busted `testUnit`, two `--rerun` full suites on ClickHouse `latest` with client V2 on whichever client ships, the anchors invariant, the tail gate and the cumulative merge gate), plus the CLT-09 go or no-go decision on measured evidence.

```sh
git rev-parse HEAD
git status --porcelain
git ls-remote origin refs/heads/custom_build
git ls-remote --exit-code --tags origin pre-v1.7.0-tail-2
git ls-remote --exit-code --heads origin sync/v1.7.0
git ls-remote --exit-code --heads origin 'scratch/*'
gh pr view 17 -R ne1r0n/clickhouse-kafka-connect --json state,headRefOid
git ls-remote origin refs/heads/dependabot/gradle/clickHouseDriver-0.11.0
git ls-remote upstream main
git ls-remote --tags --refs upstream 'v*'
curl -s https://repo1.maven.org/maven2/com/clickhouse/client-v2/maven-metadata.xml
/bin/bash -c 'bash .planning/scripts/verify-merge-invariants.sh 54e63648 a8b61f1512ab245a584b506527beaceca7264b04 > /tmp/gsd36/exec/cumulative-gate-gate.log 2>&1; echo $? > /tmp/gsd36/exec/cumulative-gate-gate.exit'
rm -rf build/test-results/testUnit
env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain testUnit --rerun > /tmp/gsd36/exec/testunit-gate.log 2>&1
rm -rf build/test-results/test
CLICKHOUSE_VERSION=latest CLIENT_VERSION=V2 env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain test --rerun > /tmp/gsd36/exec/suite1-gate.log 2>&1   # run 1 crashed (JVM exit 255), crash files deleted by name, run 2 green
rm -rf build/test-results/test
CLICKHOUSE_VERSION=latest CLIENT_VERSION=V2 env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain test --rerun > /tmp/gsd36/exec/suite2-gate.log 2>&1   # runs 1 and 2 crashed (JVM exit 255), crash files deleted by name
rm -rf build/test-results/test
CLICKHOUSE_VERSION=latest CLIENT_VERSION=V2 env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain test --rerun -PjavaVersion=21 > /tmp/gsd36/exec/suite2-gate-java21.log 2>&1
python3 -I .planning/phases/36-client-0-11-0-audit-bump/36-evidence-check.py junit build/test-results/<testUnit | test> [--class <each class>]
python3 -I .planning/phases/36-client-0-11-0-audit-bump/36-evidence-check.py skips build/test-results/test
python3 -I .planning/phases/36-client-0-11-0-audit-bump/36-evidence-check.py failed build/test-results/<testUnit | test>
python3 -I .planning/phases/36-client-0-11-0-audit-bump/36-evidence-check.py anchors .planning/scripts/fork-anchors.txt
PRE_TAG=pre-v1.7.0-tail-2 UPSTREAM_REF=9bed6f7970670a55c66be8efda246889e1f95bd0 CONFLICTED_PATHS_FILE=.planning/scripts/conflicted-paths-v1.7.0-tail-2.txt bash .planning/scripts/verify-merge-invariants.sh adaf8c4e76375e5099c076f2ca3bace892709765 a8b61f1512ab245a584b506527beaceca7264b04
git diff --quiet c9443756eb85e601552cb8241f0e7b52ed6209da a8b61f1512ab245a584b506527beaceca7264b04 -- .planning/scripts/verify-merge-invariants.sh .planning/scripts/verify-merge-invariants-selftest.sh
git diff --name-only 99b9d85c15ce1a9b76f5e843b9aec472d24f9708 a8b61f1512ab245a584b506527beaceca7264b04 -- <src/main | build and release files | docs | .planning/scripts | test side>
git rev-list --first-parent --merges pre-v1.7.0-tail-2..a8b61f1512ab245a584b506527beaceca7264b04
docker run --rm --entrypoint clickhouse clickhouse/clickhouse-server:latest local --query 'SELECT version()'
docker image inspect --format '{{index .RepoDigests 0}}' clickhouse/clickhouse-server:latest
```

```text
gated_head: a8b61f1512ab245a584b506527beaceca7264b04
utc_start: 2026-10-29T13:50:47Z
utc_end: 2026-10-29T14:26:48Z
dirty_paths_at_gate: M .planning/config.json; M .planning/state.json
fix_commit: a1cc33825a07d960bc336e0e6c7474c6698ab40b
catalog_commit: 5ab3410e94fd64e8fa6a8bca6ffd3510b13d68f8
docs_commit: 375c93be91d2a32eafd4e0364f905e51f8dfd280
origin_custom_build_at_gate: 3a3a15a9ae064ffe09c696b8bb87bce23419d463	refs/heads/custom_build
origin_unchanged: yes
pre_tail2_tag_on_origin: absent
sync_branch_on_origin: absent
scratch_branch_on_origin: absent
pr17_state_at_gate: OPEN
pr17_head_at_gate: 6a53525b738d90ee4c98a3761ebebe11421cf1f0
dependabot_branch_at_gate: 6a53525b738d90ee4c98a3761ebebe11421cf1f0
upstream_main_at_gate: 9bed6f7970670a55c66be8efda246889e1f95bd0
upstream_moved_during_phase: no
upstream_newest_release_tag_at_gate: v1.7.0
maven_client_v2_latest_at_gate: 0.11.0
testunit_exit: 0
testunit_tests: 1474
testunit_failures: 0
testunit_errors: 0
testunit_skipped: 2
testunit_expected_tests: 1474
testunit_count_matches: yes
suite1_exit: 0
suite1_tests: 2072
suite1_failures: 0
suite1_errors: 0
suite1_skipped: 18
suite1_skips: OK skips: named=7 state_mismatch_cases=11 unexpected=0
suite1_duration: 3m 13s
suite1_jvm_crash: run 1 ended with Gradle Test Executor exit value 255 after 1790 executions (javacore stack: sun/instrument/InstrumentationImpl.retransformClasses0 under org/mockito/internal/creation/bytebuddy/InlineBytecodeGenerator.mockClass, the known OpenJ9 Mockito inline-mocking fault; swap 44.7 GB of 46 GB in use); javacore, core, Snap files deleted by name; run 2 on the same tree and image exited 0 with the counts above
clickhouse_version: 26.10.3.7
clickhouse_digest: clickhouse/clickhouse-server@sha256:739e099abd1157390960a59e4532d21781d8fde57e6e4151cb920ea02c30b1ce
image_same_as_baseline: yes
suite2_exit: 0
suite2_tests: 2072
suite2_failures: 0
suite2_errors: 0
suite2_skipped: 18
suite2_skips: OK skips: named=7 state_mismatch_cases=11 unexpected=0
suite2_duration: 2m 52s
suite2_jvm_crash: runs 1 and 2 on the default JVM both ended with exit value 255 after 1790 executions with the same javacore stack as suite 1's run 1 (no testcase failed); crash files deleted by name after each; the third run with -PjavaVersion=21 per the crash rule exited 0 with the counts above (the results this entry records)
suite2_jdk: -PjavaVersion=21 for the recorded run; suite 1 ran on the default JDK 17
suite2_clickhouse_digest: clickhouse/clickhouse-server@sha256:739e099abd1157390960a59e4532d21781d8fde57e6e4151cb920ea02c30b1ce
image_same_across_suites: yes
shape_class_xml: tests=7 failures=0 errors=0 skipped=0
timeout_class_xml: tests=2 failures=0 errors=0 skipped=0
endpoint_class_xml: tests=3 failures=0 errors=0 skipped=0
wire_class_xml: tests=2 failures=0 errors=0 skipped=0
catalog_class_xml: tests=2 failures=0 errors=0 skipped=0
live_class_xml: tests=6 failures=0 errors=0 skipped=0
golden_class_xml: tests=1 failures=0 errors=0 skipped=0
suite_tests_expected: 2072
suite_count_matches: yes
docker_only_delta: 7
docker_only_delta_matches: yes
anchors_mirror: rows=152 bad=0
tail_gate_exit: 0
tail_gate_line: invariants: provenance_paths=27 anchor_commits=86 anchor_rows=152 marker_commits=93 anchor_exempt=0 result=PASS
cumulative_gate_exit: 0
cumulative_gate_line: invariants: provenance_paths=21 anchor_commits=1097 anchor_rows=152 marker_commits=1163 anchor_exempt=19 result=PASS
gate_scripts_vs_33_e14: identical
src_main_changed: src/main/java/com/clickhouse/kafka/connect/sink/db/helper/ClickHouseHelperClient.java
high_risk_untouched: yes
build_and_release_files_changed: gradle/libs.versions.toml
docs_changed: docs/FORK-DELTA.md; docs/ROLLOUT-v1.7.0_gg1.md
scripts_changed: .planning/scripts/fork-anchors.txt
test_side_modified: M src/test/java/com/clickhouse/kafka/connect/sink/AdditionalRetriableErrorCodesPinUnitTest.java; M src/test/java/com/clickhouse/kafka/connect/sink/AdditionalRetriableErrorCodesTest.java; M src/test/java/com/clickhouse/kafka/connect/sink/db/helper/NetworkCompressionPinLiveTest.java; M src/test/java/com/clickhouse/kafka/connect/sink/db/helper/NetworkCompressionPinUnitTest.java; M src/test/java/com/clickhouse/kafka/connect/sink/util/UtilsTest.java
test_side_added_count: 12
no_test_deleted: yes
integration_run_still_valid: yes
no_gg6_rollout_note: yes
helper_commits: 1
hygiene_helper_commits: 1
helper_identical_to_phase35: yes
merges_since_tag: adaf8c4e76375e5099c076f2ca3bace892709765
phase_commits: a8b61f15 docs(36-04): record the rollout items and the revert rehearsal (E-11); 7464977b docs(36-04): record the client 0.11.0 rollout items for Phase 38 (CLT-05, CLT-06, CLT-08); 52a43e2d docs(36-03): complete the byte comparison, archive audit and D-17 rewording plan; 4edde777 docs(36-03): record the D-17 rewording and the client-override wording (E-10); 375c93be docs(36-03): reword the D-17 rationale to the 0.11.0 measurements and record the client override (CLT-05, CLT-02, CLT-08); 04ed4630 docs(36-03): record the archive comparison and the zstd-jni facts (E-09); 4b4d4881 docs(36-03): capture the Confluent archive before and after the client bump (CLT-08); 999453e6 docs(36-03): record the RowBinary byte comparison across the client bump (E-08); 18f229d1 docs(36-02): complete the createClientV2 fixes and client 0.11.0 bump plan; beeee6d8 docs(36-02): record the non-UTC date pins, the V1 run and the Connect-worker integrationTest on 0.11.0 (E-07); 3d0b0461 docs(36-02): record the catalog commit and the pins on 0.11.0 (E-06); 5ab3410e build(36-02): move clickHouseDriver to 0.11.0 as one fork override (CLT-02); 939e8992 docs(36-02): record the createClientV2 fix commit and its gates (E-05); a1cc3382 fix(36-02): build the V2 client with an endpoint of protocol, host and port only and ignore unknown config keys (CLT-03); 0a88f7b7 docs(36-01): complete the client 0.11.0 audit pins plan; 585ccb40 docs(36-01): record the RowBinary golden on 0.9.6 (E-04); 83d0b42a test(36-01): bless the RowBinary golden on client 0.9.6 (CLT-06); 5ccd15ea docs(36-01): record the 0.9.6 pins and the bare-bump red set (E-03); 765476af test(36-01): pin timeout prefixes, endpoint and options, the insert wire and the 0.11.0 behaviours on 0.9.6 (CLT-01, CLT-03, CLT-04); 681de964 docs(36-01): record the factory and the error-shape pin (E-02); 9dd36588 test(36-01): pin the client error shapes through a real socket on 0.9.6 (CLT-01); c6c7edb4 test(36-01): build the ServerException of the 10 three-argument call sites through one factory (CLT-01); 3db861ff docs(36-01): open the Phase 36 evidence: pre-flight and baselines on the untouched tree
hygiene_scan: clean
decision: go
decision_basis: The cache-busted testUnit (1474, baseline plus 16) and both full suites on ClickHouse latest with client V2 on client 0.11.0 (2072 each, baseline plus 23, 18 named skips, Docker-only delta 7) are green with every class tally as listed, the anchors mirror reads rows=152 bad=0, the tail gate and the cumulative gate end result=PASS with exit 0, origin custom_build is unchanged with no tail tag, sync or scratch ref on origin, and every scope fact is as expected.
clt_status: CLT-01 to CLT-06, CLT-08 and CLT-09 satisfied; CLT-07 first half satisfied, its nightly dispatch is user-gated
clickhouse_digest_after_suites: clickhouse/clickhouse-server@sha256:739e099abd1157390960a59e4532d21781d8fde57e6e4151cb920ea02c30b1ce
```

Decision: go. Phase 36 delivers the 0.9.6 pin set, the factory for the ten call sites, the two `createClientV2` fixes, the catalog commit that moves `clickHouseDriver` to 0.11.0 as one marked override, the RowBinary golden, the reworded D-17 rationale, the archive record and the labelled rollout items. The connector ships on client 0.11.0 behind a green cache-busted `testUnit` and two green `--rerun` full suites on ClickHouse 26.10.3.7 with client V2, and the CLT-09 revert (documentation commit, then catalog commit) was rehearsed conflict-free in E-11 and is not applied.

JVM crash record. The documented crash rule applied twice. Suite 1 crashed once (the OpenJ9 fault in Mockito's inline retransformation, reproduced under heavy swap use) and was green on its re-run on the unchanged tree and image. Suite 2 crashed in its first run and again in its re-run at the same point, in the same stack, with no failing testcase; the third run with `-PjavaVersion=21` was green with exactly the counts of suite 1, and that run is the one recorded. Suite 1 therefore ran on the default JDK 17 and suite 2 on JDK 21. The crashes are a fault of the test JVM and not a result of the bump: a failing testcase was never reported, and the same two suites are green on a different JVM. This is reported to the orchestrator in the plan summary.

Nothing is pushed: origin `custom_build` still reads `3a3a15a9`, none of `pre-v1.7.0-tail-2`, `sync/v1.7.0` or a scratch branch exists on origin, and PR #17 (open, head `6a53525b`) and the Dependabot branch (same SHA) were only read. Upstream main is unchanged since E-01 (`9bed6f79`) and its newest release tag is `v1.7.0`; Maven's latest `client-v2` is `0.11.0`. The open item for the milestone is CLT-07's nightly dispatch, which is the user's step (plan task 3 writes the handoff).

Measured 2026-10-29T14:27:54Z (gated head, all gates and scope facts).

Verdict: pass

### E-13: CLT-07 nightly dispatch: handoff prepared, the dispatch is the user's step

Backs: CLT-07, second half, and the user's decision that the nightly dispatch is the user's step (the plan prepares a handoff file and ends before the push and the dispatch).

```sh
git rev-parse HEAD
git diff --quiet a8b61f1512ab245a584b506527beaceca7264b04 HEAD -- . ':(exclude).planning'
git rev-list --count 3a3a15a9ae064ffe09c696b8bb87bce23419d463..HEAD
git diff --stat 3a3a15a9ae064ffe09c696b8bb87bce23419d463 HEAD -- . ':(exclude).planning'
grep -n 'name:\|workflow_dispatch\|timeout-minutes' .github/workflows/nightly-integration-tests.yaml
gh api 'repos/ne1r0n/clickhouse-kafka-connect/contents/.github/workflows/nightly-integration-tests.yaml?ref=custom_build' --jq .name
git ls-remote origin refs/heads/custom_build
git ls-remote --exit-code --heads origin 'scratch/*'
python3 -I .planning/phases/36-client-0-11-0-audit-bump/36-hygiene-check.py .planning/phases/36-client-0-11-0-audit-bump/36-USER-DISPATCH.md
git add .planning/phases/36-client-0-11-0-audit-bump/36-USER-DISPATCH.md
git commit -F /tmp/gsd36/exec/msg-handoff.txt
docker run --rm --entrypoint clickhouse clickhouse/clickhouse-server:latest local --query 'SELECT version()'
```

```text
gated_head: a8b61f1512ab245a584b506527beaceca7264b04
head_now: fccb5bb142843593c40332259889300b3de17b13
non_planning_tree_equals_gated_head: yes
commits_published_by_push: 157
non_planning_files_published: 64 files changed, 3836 insertions(+), 528 deletions(-)
scratch_branch_name: scratch/clt-07-client-0.11.0
workflow_file_exists_at_head: yes
workflow_dispatch_trigger_present: yes
workflow_display_name: Nightly Integration Tests (ClickHouse latest, client V2)
nightly_job_timeout: 85
nightly_test_step_timeout: 75
workflow_on_default_branch: nightly-integration-tests.yaml
tests_yaml_push_trigger: on: push: branches: ['**']
local_integration_summary: Tests summary: 55 tests, 54 succeeded, 0 failed, 1 skipped
local_integration_duration: BUILD SUCCESSFUL in 31m 30s
local_integration_connector_tests: tests=54 failures=0 errors=0 skipped=0 files=1
origin_custom_build_now: 3a3a15a9ae064ffe09c696b8bb87bce23419d463	refs/heads/custom_build
handoff_file: 36-USER-DISPATCH.md
handoff_commit: fccb5bb142843593c40332259889300b3de17b13
handoff_sections: Status; What you decide; Before you push; Push; Dispatch; What to report; If the run is red; Clean up; Resume (for the orchestrator)
user_gated: yes
nothing_pushed: yes
hygiene_scan: clean
clickhouse_version: 26.10.3.7
```

The handoff file gives the user the gated SHA, the five checks to run before pushing (the origin tip, tree equality outside `.planning`, no other origin branch at the SHA, no scratch branch), the exact push command for the scratch branch, the dispatch with the branch-selection warning, the fields to report, the red-run and clean-up steps and the resume instructions that record the user's report as E-15. The count of commits that the push publishes is 157 here (it was 156 before the handoff commit and rises with the commits that follow); the handoff file says so and gives the command for the exact figure. This entry stays `pending` as the record of the handoff and is never edited; CLT-07's second half is marked passed only through E-15 after the user's own report. No agent pushed, dispatched, commented or acted on a pull request: origin `custom_build` is still the published `v1.7.0_gg5` commit and no scratch branch exists on origin.

Measured 2026-10-29T14:30:00Z (workflow facts, origin re-read, handoff committed).

Verdict: pending (user-gated: the manual nightly dispatch is the user's step)

### E-14: Phase 36 gate on 0.9.6 after a no-go revert: not measured, the go branch was taken

Backs: CLT-09, no-go branch only. E-12 decided go, so no revert was applied and no gate was re-run on 0.9.6; this entry exists only so the index stays contiguous with E-15.

```sh
python3 -I .planning/phases/36-client-0-11-0-audit-bump/36-evidence-check.py get .planning/phases/36-client-0-11-0-audit-bump/36-COMMAND-EVIDENCE.md E-12 decision
```

```text
decision_outcome: not-applicable
e12_decision: go
revert_applied: no
```

Measured 2026-10-30 (read from E-12; nothing was run).

Verdict: unmeasured (the no-go branch was not taken, E-12 decided go)

### E-15: CLT-07 nightly dispatch, user-reported

Backs: CLT-07, second half. This entry is the user-reported result of the manual nightly dispatch that `36-USER-DISPATCH.md` handed over (E-13 stays `pending` as the record of that handoff). The run facts below were reported by the user and re-read by the orchestrator with read-only `gh`; no agent pushed or dispatched anything. The numbering follows the handoff's Resume section; E-14 is the gate re-run of the no-go branch, which the go decision did not take, so it is recorded as `unmeasured` in its own entry below to keep the index contiguous.

```sh
git diff --quiet a8b61f1512ab245a584b506527beaceca7264b04 7da442f1b55163340d069b246b3281ac36c76770 -- . ':(exclude).planning'
git ls-remote origin refs/heads/custom_build
git ls-remote --heads origin 'scratch/*'
gh run view 40971181887 -R ne1r0n/clickhouse-kafka-connect --json headSha,conclusion,event,headBranch,status
gh run list -R ne1r0n/clickhouse-kafka-connect --commit 7da442f1b55163340d069b246b3281ac36c76770 --json name,conclusion,event,databaseId
```

```text
reported_by: user-reported
run_url: https://github.com/ne1r0n/clickhouse-kafka-connect/actions/runs/40971181887
run_id: 40971181887
workflow: nightly-integration-tests.yaml
event: workflow_dispatch
head_branch: scratch/clt-07-client-0.11.0
head_sha: 7da442f1b55163340d069b246b3281ac36c76770
pushed_sha: 7da442f1b55163340d069b246b3281ac36c76770
head_sha_equals_pushed_sha: yes
non_planning_tree_equals_gated_head: yes
run_status: completed
conclusion: failure
started_at: 2026-10-29T23:09:40Z
finished_at: 2026-10-29T23:58:00Z
run_duration: 48m20s
job_duration: 48m15s
job_id: 123788031133
runner_nproc: 4
runner_memory_total_mb: 15983
runner_memory_available_mb: 14536
runner_docker_server: 28.0.4
summary_line: tests=63 failures=5 errors=0 skipped=9
tests_summary: Tests summary: 55 tests, 49 succeeded, 5 failed, 1 skipped
failed_class: ClickHouseSinkConnectorIntegrationTest
failed_first: stockGenSingleTaskTest (ConditionTimeoutException, alias 'connector running', 5 min)
failed_then: stockGenMultiTaskSchemalessTest; stockGenMultiTaskInterruptTest; stockGenWithJdbcPropSingleTaskTest; stockGenMultiTaskTopicTest (each RuntimeException: Data generation failed at ConfluentPlatform.generateData:516)
preceding_test_passed: stockGenSingleTaskInterruptTest (generated 25000 records, Diff from Theoretical Total: [-12500])
connect_worker_logs_in_artifacts: no
cause: undetermined
baseline_run: 40474535605 (nightly-integration-tests.yaml, workflow_dispatch on custom_build at 3a3a15a9ae064ffe09c696b8bb87bce23419d463, client 0.9.6, 2026-10-27, success)
baseline_tests_summary: Tests summary: 55 tests, 54 succeeded, 0 failed, 1 skipped
baseline_summary_line: tests=63 failures=0 errors=0 skipped=9
local_connect_worker_integration_on_0_11_0: E-07, green (54 succeeded)
tests_yaml_run_on_push: 40971068623 success (informational)
origin_custom_build_now: 3a3a15a9ae064ffe09c696b8bb87bce23419d463
scratch_branch_on_origin: scratch/clt-07-client-0.11.0 at 7da442f1b55163340d069b246b3281ac36c76770
scratch_branch_decision: user kept the branch until the investigation is over (not deleted)
user_approved_push_and_dispatch: yes (the user's own message)
attribution_to_bump: not established
clt_07_passed: no
next_step: pre-bump dispatch at 939e899298c2f12dec2cfe1e69b1a6b53b4f511e, handoff in 36-USER-DISPATCH-PREBUMP.md, user-gated
hygiene_scan: clean
```

The run is red, so the Verdict is `fail`, and CLT-07 is not passed. The five failures are all in the Connect-worker class and start with the worker never reaching the running state in `stockGenSingleTaskTest`; the four later data-generation failures may be a consequence of it, and the passing `stockGenSingleTaskInterruptTest` still shows a record-count deviation (a record-count deviation of 12500 from the theoretical total). The artifacts hold no Connect-worker logs, so the cause is undetermined. In particular, attribution to the bump is NOT established: the baseline differs from this run in the client version, but also in date, runner, Docker version and the ClickHouse `latest` digest, and the local Connect-worker integration run on 0.11.0 was green (E-07). The unit and full-suite evidence of E-12 is unaffected by this entry. The next step is the same nightly on the commit before the catalog commit (client 0.9.6, same `.github`), which needs a second user-approved push (`36-USER-DISPATCH-PREBUMP.md`); the branch `scratch/clt-07-client-0.11.0` stays until that is done. A fail verdict is fixed by re-measuring in a new run, never by editing this entry.

Measured 2026-10-30 (user-reported run facts, re-read with read-only `gh` and `git ls-remote`).

Verdict: fail
