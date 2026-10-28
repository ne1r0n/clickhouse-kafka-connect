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
| E-02 | TAL-01 | 34-01 | pass |
| E-03 | TAL-01 | 34-01 | pass |
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

Timestamp (UTC): 2026-10-28T21:12:46Z to 2026-10-28T21:15:47Z (corrected in the E-02/E-03 commit: the entry was first committed with an end time of 21:18:40Z that was written ahead of the clock; the suite log's own end is 21:15:47Z)

Verdict: pass

### E-02: Rollback tag, upstream re-read immediately before the merge, and the mechanical merge M3

Backs: TAL-01, ROADMAP Phase 34 criterion 1 (the rollback tag, the upstream re-read immediately before the merge, one mechanical merge commit with no authored line).

```sh
git tag pre-v1.7.0-tail-2 HEAD
git rev-parse refs/tags/pre-v1.7.0-tail-2
git cat-file -t refs/tags/pre-v1.7.0-tail-2
git ls-remote upstream main
git ls-remote --tags --refs upstream 'v*'
git diff --name-only $(git merge-base HEAD 9bed6f7970670a55c66be8efda246889e1f95bd0) 9bed6f7970670a55c66be8efda246889e1f95bd0 | wc -l
git merge-tree --write-tree --name-only HEAD 9bed6f7970670a55c66be8efda246889e1f95bd0
git merge --no-ff --no-commit 9bed6f7970670a55c66be8efda246889e1f95bd0
git diff --name-only --diff-filter=U | LC_ALL=C sort
git add CHANGELOG.md src/main/java/com/clickhouse/kafka/connect/sink/ProxySinkTask.java src/main/java/com/clickhouse/kafka/connect/util/Utils.java src/test/java/com/clickhouse/kafka/connect/sink/ClickHouseSinkTaskMappingTest.java src/test/java/com/clickhouse/kafka/connect/sink/db/ClickHouseWriterTest.java
git grep -n --cached -E '^(<{7}|>{7})( |$)' -- src CHANGELOG.md
env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain compileJava
git commit -F /tmp/gsd34/exec/msg-m3.txt
git rev-list --parents -n 1 adaf8c4e76375e5099c076f2ca3bace892709765
git diff --name-only adaf8c4e76375e5099c076f2ca3bace892709765^1 adaf8c4e76375e5099c076f2ca3bace892709765
```

```text
pre_tag_target: 68f1b0cbfcb217fea1de769b48bc985dacdc40f3
pre_tag_object_type: commit
upstream_main_at_merge: 9bed6f7970670a55c66be8efda246889e1f95bd0
upstream_newest_release_tag_at_merge: v1.7.0
upstream_sha: 9bed6f7970670a55c66be8efda246889e1f95bd0
expected_provenance_paths: 27
merge_tree_conflicts_at_merge: CHANGELOG.md; src/main/java/com/clickhouse/kafka/connect/sink/ProxySinkTask.java; src/main/java/com/clickhouse/kafka/connect/util/Utils.java; src/test/java/com/clickhouse/kafka/connect/sink/ClickHouseSinkTaskMappingTest.java; src/test/java/com/clickhouse/kafka/connect/sink/db/ClickHouseWriterTest.java
merge_conflicted_paths: CHANGELOG.md; src/main/java/com/clickhouse/kafka/connect/sink/ProxySinkTask.java; src/main/java/com/clickhouse/kafka/connect/util/Utils.java; src/test/java/com/clickhouse/kafka/connect/sink/ClickHouseSinkTaskMappingTest.java; src/test/java/com/clickhouse/kafka/connect/sink/db/ClickHouseWriterTest.java
resolution_changelog: union, fork gg sections first, blank line, upstream Unreleased block
resolution_proxysinktask: fork side in both hunks, seam deferred to 34-02
resolution_utils: Set import first, then the five fork org.* imports
resolution_mappingtest: Supplier import, then the two fork imports, duplicates dropped
resolution_writertest: only upstream's Supplier import and its trailing blank line
marker_scan_staged: clean
compile_java_before_commit: 0
merge_commit: adaf8c4e76375e5099c076f2ca3bace892709765
merge_parent1: 68f1b0cbfcb217fea1de769b48bc985dacdc40f3
merge_parent2: 9bed6f7970670a55c66be8efda246889e1f95bd0
merge_changed_paths: 27
merge_changed_paths_equal_upstream_changed: yes
merge_planning_paths: 0
merge_proxysinktask_fork_side: yes
```

The tag was created after the E-01 commit `68f1b0cb` and before the merge, so it equals M3's first parent. The ordered hunk lines are within-hunk reorderings only: the `Utils.java` and `ClickHouseSinkTaskMappingTest.java` import hunks were ordered for the fork's sorted import order, and `ClickHouseWriterTest.java`'s `Supplier` import sits at its within-hunk position after the `org.slf4j` imports. No commit was made between `git merge --no-commit` and M3. `git show M3:path` was read as `"${M}:path"` (zsh would otherwise treat `$M:src` as a history modifier).

Timestamp (UTC): 2026-10-28T21:16:23Z to 2026-10-28T21:17:24Z

Verdict: pass

### E-03: Record commit and the unedited tail gate over M3

Backs: TAL-01, ROADMAP Phase 34 criterion 1 (the record of every conflicted path and its resolution; the unedited `verify-merge-invariants.sh` passing on the merge with `UPSTREAM_REF` set to the full upstream SHA).

```sh
git merge-tree --write-tree --name-only adaf8c4e76375e5099c076f2ca3bace892709765^1 adaf8c4e76375e5099c076f2ca3bace892709765^2
git add .planning/scripts/conflicted-paths-v1.7.0-tail-2.txt .planning/MERGE-CHECKLIST-v1.7.0-tail-2.md
git commit -F /tmp/gsd34/exec/msg-record.txt
git diff --name-only f6f4dd8e0da05d7e60473e0a391fa3c972906f9e^ f6f4dd8e0da05d7e60473e0a391fa3c972906f9e
PRE_TAG=pre-v1.7.0-tail-2 UPSTREAM_REF=9bed6f7970670a55c66be8efda246889e1f95bd0 CONFLICTED_PATHS_FILE=.planning/scripts/conflicted-paths-v1.7.0-tail-2.txt bash .planning/scripts/verify-merge-invariants.sh adaf8c4e76375e5099c076f2ca3bace892709765 f6f4dd8e0da05d7e60473e0a391fa3c972906f9e > /tmp/gsd34/exec/tail-gate-m3.log 2>&1
```

```text
record_commit: f6f4dd8e0da05d7e60473e0a391fa3c972906f9e
record_commit_files: .planning/MERGE-CHECKLIST-v1.7.0-tail-2.md; .planning/scripts/conflicted-paths-v1.7.0-tail-2.txt
conflicted_paths_lines: 5
checklist_sections: Section 1 (5 rows), Section 2 (27 rows), Section 3, Gate commands, Recorded Facts 44-45
tail_gate_line: invariants: provenance_paths=27 anchor_commits=2 anchor_rows=131 marker_commits=9 anchor_exempt=0 result=PASS
tail_gate_exit: 0
tail_gate_duration: 12s
tail_gate_violations: 0
```

The gate ran unedited over M3..record commit with explicit SHAs; its log holds no VIOLATION or MISMATCH line. Section 1 and Section 2 of the checklist were generated by a scratch script (not committed) from the conflicted-paths file and `git diff --name-status`, not hand-transcribed.

Timestamp (UTC): 2026-10-28T21:17:40Z to 2026-10-28T21:18:24Z

Verdict: pass
