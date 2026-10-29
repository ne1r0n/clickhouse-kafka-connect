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
