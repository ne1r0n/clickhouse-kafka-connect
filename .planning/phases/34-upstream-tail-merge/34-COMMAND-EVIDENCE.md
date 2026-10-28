# Phase 34: Command evidence

- **Base commit:** `local_head` = `6de1e0bdda8b4a3ef90b7d4f0d4d4b45dee3e2eb` (HEAD when plan 34-01 started, branch `sync/v1.7.0`).
- **Shell:** macOS zsh, multi-line logic under `/bin/bash`. Gradle is run as `env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain ...`; live runs set `CLICKHOUSE_VERSION=latest CLIENT_VERSION=V2` explicitly.
- **Docker:** colima profile `vz` (aarch64, 8 CPU, 16 GiB, 256 GiB disk), docker CLI from `/opt/homebrew/Cellar/docker/29.8.2/bin/docker`.
- **Date (UTC):** 2026-10-28.
- **Entry format:** each entry has a `### E-NN: {title}` heading, a `Backs:` line, an `sh` fence with the commands as run, a `text` fence of `key: value` lines (keys match `[a-z0-9_]+`, one fact per line) with free text allowed after the fence, a UTC timestamp line, and a final line `Verdict: pass`, `Verdict: fail`, `Verdict: pending ({reason})` or `Verdict: unmeasured ({reason})`. No entry carries a token or credential.
- **Rule:** a fail or pending verdict is fixed by re-measuring, never by editing the evidence.
- **Helper:** `34-evidence-check.py` in this directory is the read-only gate helper (get, expect, index, rows, secrets, junit, anchors, skips, failed); it is never edited.

## Index

| Entry | Requirement | Plan | Verdict |
|-------|-------------|------|---------|
| E-01 | TAL-01 | 34-01 | pass |
| E-02 | TAL-01 | 34-01 | pending (plan 34-01) |
| E-03 | TAL-01 | 34-01 | pending (plan 34-01) |
| E-04 | TAL-03 | 34-01 | pending (plan 34-01) |
| E-05 | TAL-02, TAL-05 | 34-02 | pending (plan 34-02) |
| E-06 | TAL-03, TAL-05 | 34-02 | pending (plan 34-02) |
| E-07 | TAL-03 | 34-02 | pending (plan 34-02) |
| E-08 | TAL-02 | 34-02 | pending (plan 34-02) |
| E-09 | TAL-04 | 34-03 | pending (plan 34-03) |
| E-10 | TAL-01, TAL-04, TAL-05 | 34-03 | pending (plan 34-03) |
| E-11 | TAL-01, TAL-02, TAL-03, TAL-04, TAL-05 | 34-03 | pending (plan 34-03) |

### E-01: Pre-flight: upstream and origin re-read, conflict set, baseline suites on the untouched tip

Backs: TAL-01, ROADMAP Phase 34 criterion 1 (upstream and origin re-read and the conflict set re-measured before the merge; a green baseline so every later break is attributable to the tail).

```sh
colima list
git rev-parse HEAD
git status --porcelain
git remote get-url upstream
git ls-remote upstream main
git ls-remote --tags --refs upstream 'v*'
git fetch upstream --tags
git rev-parse upstream/main
git merge-base HEAD 9bed6f7970670a55c66be8efda246889e1f95bd0
git diff --name-only a6f1df839c079180bc7c343b5823f205e204dafc 9bed6f7970670a55c66be8efda246889e1f95bd0 | wc -l
git merge-tree --write-tree --name-only HEAD 9bed6f7970670a55c66be8efda246889e1f95bd0 > /tmp/gsd34/exec/mt-preflight.txt 2>&1
git ls-remote origin refs/heads/custom_build
git ls-remote --exit-code --tags origin pre-v1.7.0-tail-2
git ls-remote --exit-code --heads origin sync/v1.7.0
git diff --quiet c9443756eb85e601552cb8241f0e7b52ed6209da HEAD -- .planning/scripts/verify-merge-invariants.sh .planning/scripts/verify-merge-invariants-selftest.sh
python3 -I .planning/phases/34-upstream-tail-merge/34-evidence-check.py anchors .planning/scripts/fork-anchors.txt
rm -rf build/test-results/testUnit
env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain testUnit --rerun > /tmp/gsd34/exec/testunit-baseline.log 2>&1
rm -rf build/test-results/test
CLICKHOUSE_VERSION=latest CLIENT_VERSION=V2 env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain test --rerun > /tmp/gsd34/exec/suite-baseline.log 2>&1
python3 -I .planning/phases/34-upstream-tail-merge/34-evidence-check.py junit build/test-results/test
python3 -I .planning/phases/34-upstream-tail-merge/34-evidence-check.py skips build/test-results/test
docker run --rm --entrypoint clickhouse clickhouse/clickhouse-server:latest local --query 'SELECT version()'
docker image inspect --format '{{index .RepoDigests 0}}' clickhouse/clickhouse-server:latest
```

```text
local_head: 6de1e0bdda8b4a3ef90b7d4f0d4d4b45dee3e2eb
dirty_paths: M .planning/STATE.md; M .planning/config.json; M .planning/state.json
upstream_url: https://github.com/ClickHouse/clickhouse-kafka-connect.git
upstream_main: 9bed6f7970670a55c66be8efda246889e1f95bd0
upstream_moved: no
upstream_newest_release_tag: v1.7.0
upstream_release_tag_past_v170: no
upstream_main_after_fetch: 9bed6f7970670a55c66be8efda246889e1f95bd0
merge_base: a6f1df839c079180bc7c343b5823f205e204dafc
upstream_changed_paths: 27
merge_tree_exit: 1
merge_tree_conflicts: CHANGELOG.md; src/main/java/com/clickhouse/kafka/connect/sink/ProxySinkTask.java; src/main/java/com/clickhouse/kafka/connect/util/Utils.java; src/test/java/com/clickhouse/kafka/connect/sink/ClickHouseSinkTaskMappingTest.java; src/test/java/com/clickhouse/kafka/connect/sink/db/ClickHouseWriterTest.java
merge_tree_conflicts_match_roadmap: yes
origin_custom_build: 3a3a15a9ae064ffe09c696b8bb87bce23419d463
pre_tail2_tag_on_origin: absent
sync_branch_on_origin: absent
gate_scripts_vs_33_e14: identical
anchors_mirror: rows=131 bad=0
colima_vz: Running
baseline_testunit_exit: 0
baseline_testunit_tests: 1387
baseline_testunit_failures: 0
baseline_testunit_errors: 0
baseline_testunit_skipped: 2
baseline_suite_exit: 0
baseline_suite_tests: 1974
baseline_suite_failures: 0
baseline_suite_errors: 0
baseline_suite_skipped: 18
baseline_suite_duration: BUILD SUCCESSFUL in 2m 44s
baseline_skips: OK skips: named=7 state_mismatch_cases=11 unexpected=0
clickhouse_version: 26.10.3.7
clickhouse_digest: clickhouse/clickhouse-server@sha256:739e099abd1157390960a59e4532d21781d8fde57e6e4151cb920ea02c30b1ce
jvm_crash: none
baseline_docker_only_tests: 587
```

`git ls-remote --exit-code` exited 2 for both the tag and the branch (absent on origin). The testUnit baseline is 1387 tests over 64 XML files in 6 s; the full suite is 1974 tests over 108 XML files. The dirty tree held only `.planning/STATE.md`, `.planning/config.json` and `.planning/state.json`. No `javacore.*`, `core.*.dmp`, `Snap.*.trc` or `jitdump.*` file appeared.

Timestamp (UTC): 2026-10-28T21:12:46Z to 2026-10-28T21:18:40Z

Verdict: pass
