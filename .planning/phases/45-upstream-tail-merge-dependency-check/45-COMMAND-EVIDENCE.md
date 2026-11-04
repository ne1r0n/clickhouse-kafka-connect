# Phase 45: Command evidence

- **Base commit:** `local_head` = `2c19e93fca9e155f08ddd646eb06f8e990d94ecf` (HEAD when plan 45-01 started, branch `sync/v1.7.0`).
- **Shell:** macOS zsh, multi-line logic under `/bin/bash` or `python3 -I`. Gradle is run as `env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain ...`; ClickHouse-backed runs prefix `CLICKHOUSE_VERSION=latest CLIENT_VERSION=V2`.
- **Docker:** colima profile `vz`.
- **Date (UTC):** 2026-11-04.
- **Entry format:** each entry has a `### E-NN: {title}` heading, a `Backs:` line, an `sh` fence with the commands as run, a `text` fence of `key: value` lines (keys match `[a-z0-9_]+`, one fact per line, no key twice) with free text allowed after the fence, a UTC timestamp line, and a final line `Verdict: pass`, `Verdict: fail`, `Verdict: pending ({reason})` or `Verdict: unmeasured ({reason})`.
- **Rule:** a fail or pending verdict is fixed by re-measuring in a new run, never by editing the evidence.
- **Helper:** `45-evidence-check.py` is the read-only gate helper written at plan time (byte-identical to the Phase 34-44 copies); it is never edited.
- **No push happens in Phase 45.** Reads use `git ls-remote` and `gh api` only.

## Index

| Entry | Requirement | Plan | Verdict |
|-------|-------------|------|---------|
| E-01 | UPS-01 | 45-01 | pass |
| E-02 | UPS-01 | 45-01 | pending (plan 45-01) |
| E-03 | UPS-01 | 45-01 | pending (plan 45-01) |
| E-04 | UPS-01, UPS-02, UPS-04 | 45-01 | pending (plan 45-01) |
| E-05 | UPS-02, UPS-04 | 45-02 | pending (plan 45-02) |
| E-06 | UPS-02 | 45-02 | pending (plan 45-02) |
| E-07 | UPS-02 | 45-03 | pending (plan 45-03) |
| E-08 | UPS-02 | 45-03 | pending (plan 45-03) |
| E-09 | UPS-02 | 45-03 | pending (plan 45-03) |
| E-10 | UPS-02 | 45-04 | pending (plan 45-04) |
| E-11 | UPS-02 | 45-04 | pending (plan 45-04) |
| E-12 | UPS-02 | 45-04 | pending (plan 45-04) |
| E-13 | UPS-03, UPS-04 | 45-05 | pending (plan 45-05) |
| E-14 | UPS-05 | 45-05 | pending (plan 45-05) |
| E-15 | UPS-02, UPS-04 | 45-05 | pending (plan 45-05) |
| E-16 | DEP-01 | 45-06 | pending (plan 45-06) |
| E-17 | DEP-01, DEP-02 | 45-06 | pending (plan 45-06) |
| E-18 | UPS-01, UPS-02, UPS-03, UPS-04, UPS-05, DEP-02 | 45-06 | pending (plan 45-06) |

### E-01: Pre-flight: upstream and origin re-read, conflict set, baseline suites on the untouched tip

Backs: UPS-01, ROADMAP Phase 45 criterion 1 (pre-flight reads before the merge; baseline suites on the untouched tip so every later red is attributable to the tail).

```sh
git rev-parse HEAD
git rev-parse --abbrev-ref HEAD
git status --porcelain
colima list
git remote get-url upstream
git ls-remote upstream main
git ls-remote --tags --refs upstream 'v*' | sed 's#.*refs/tags/##' | sort -V | tail -3
git ls-remote --tags upstream 'v1.7.1*'
git fetch upstream --tags
git rev-parse upstream/main
git merge-base HEAD 83808df9a0993cf49b2ceaa4633c782e550c0180
git diff --name-only 9bed6f7970670a55c66be8efda246889e1f95bd0 83808df9a0993cf49b2ceaa4633c782e550c0180 | wc -l
git merge-tree --write-tree --name-only HEAD 83808df9a0993cf49b2ceaa4633c782e550c0180 > /tmp/gsd45/exec/mt-preflight.txt 2>&1
gh api repos/ne1r0n/clickhouse-kafka-connect/branches/custom_build --jq .commit.sha
gh api repos/ne1r0n/clickhouse-kafka-connect/git/matching-refs/tags/pre-v1.7.0_gg8 --jq length
gh api repos/ne1r0n/clickhouse-kafka-connect/git/matching-refs/heads/sync/v1.7.0 --jq '.[].object.sha'
git log -1 --format=%H -- .planning/scripts/verify-merge-invariants.sh .planning/scripts/verify-merge-invariants-selftest.sh
bash .planning/scripts/verify-merge-invariants-selftest.sh > /tmp/gsd45/exec/selftest.log 2>&1
python3 -I .planning/phases/45-upstream-tail-merge-dependency-check/45-evidence-check.py anchors .planning/scripts/fork-anchors.txt
md5 -q .planning/phases/45-upstream-tail-merge-dependency-check/45-evidence-check.py
rm -rf build/test-results/testUnit
env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain testUnit --rerun > /tmp/gsd45/exec/testunit-baseline.log 2>&1
python3 -I .planning/phases/45-upstream-tail-merge-dependency-check/45-evidence-check.py junit build/test-results/testUnit
python3 -I .planning/phases/45-upstream-tail-merge-dependency-check/45-evidence-check.py failed build/test-results/testUnit
rm -rf build/test-results/test
CLICKHOUSE_VERSION=latest CLIENT_VERSION=V2 env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain test --rerun > /tmp/gsd45/exec/suite-baseline.log 2>&1
python3 -I .planning/phases/45-upstream-tail-merge-dependency-check/45-evidence-check.py junit build/test-results/test
python3 -I .planning/phases/45-upstream-tail-merge-dependency-check/45-evidence-check.py skips build/test-results/test
python3 -I .planning/phases/45-upstream-tail-merge-dependency-check/45-evidence-check.py failed build/test-results/test
docker run --rm --entrypoint clickhouse clickhouse/clickhouse-server:latest local --query 'SELECT version()'
docker image inspect --format '{{index .RepoDigests 0}}' clickhouse/clickhouse-server:latest
```

```text
local_head: 2c19e93fca9e155f08ddd646eb06f8e990d94ecf
dirty_paths: M .planning/config.json; M .planning/state.json
colima_vz: Running
upstream_url: https://github.com/ClickHouse/clickhouse-kafka-connect.git
upstream_main: 83808df9a0993cf49b2ceaa4633c782e550c0180
upstream_moved: no
upstream_newest_release_tag: v1.7.0
upstream_release_tag_past_v170: no
upstream_v171_tag: absent
merge_base: 9bed6f7970670a55c66be8efda246889e1f95bd0
upstream_changed_paths: 11
merge_tree_exit: 1
merge_tree_conflicts: .github/workflows/tests.yaml; CHANGELOG.md; VERSION; gradle/libs.versions.toml; src/main/java/com/clickhouse/kafka/connect/sink/db/helper/ClickHouseFieldDescriptor.java; src/main/java/com/clickhouse/kafka/connect/sink/db/helper/ClickHouseHelperClient.java
merge_tree_conflicts_match_roadmap: yes
origin_custom_build: 9483476322e0d331e81c5b92ac266a18f0f01450
origin_custom_build_moved: no
origin_pre_tag: absent
origin_sync_branch: absent
gate_scripts_last_commit: 046fe59defa5ed7cab9dc8cb29c25b4e36fd9d28
selftest_last_line: selftest: no failures
anchors_mirror: rows=190 bad=0
helper_md5: 7123d354f51d45e019837f43b3a30fbc
baseline_testunit_exit: 0
baseline_testunit_tests: 1826
baseline_testunit_failures: 0
baseline_testunit_errors: 0
baseline_testunit_skipped: 2
baseline_suite_exit: 0
baseline_suite_tests: 2425
baseline_suite_failures: 0
baseline_suite_errors: 0
baseline_suite_skipped: 18
baseline_suite_files: 138
baseline_suite_duration: 176s
baseline_skips: OK skips: named=7 state_mismatch_cases=11 unexpected=0
clickhouse_version: 26.10.3.7
clickhouse_digest: clickhouse/clickhouse-server@sha256:739e099abd1157390960a59e4532d21781d8fde57e6e4151cb920ea02c30b1ce
jvm_crash: none
baseline_docker_only_tests: 599
```

The gate self-test ran 16 cases (ST1-ST9 variants), all PASS, in about 55 minutes (ST4 runs the cumulative gate). `baseline_docker_only_tests` is 2425 suite tests minus 1826 testUnit tests. The research rehearsal's full-suite totals (2245 tests, partial, from a crashed run) were not used; these numbers are the measured expectation for the later plans. No `core.*`, `javacore.*`, `Snap.*` or `jitdump.*` file was left in the root. `origin_custom_build` has the `94834763` prefix, so origin did not move since planning.

2026-11-04T20:06:30Z
Verdict: pass
