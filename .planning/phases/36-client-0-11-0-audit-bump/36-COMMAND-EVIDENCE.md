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
| E-02 | CLT-01 | 36-01 | pending (plan 36-01) |
| E-03 | CLT-01, CLT-03, CLT-04 | 36-01 | pending (plan 36-01) |
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
