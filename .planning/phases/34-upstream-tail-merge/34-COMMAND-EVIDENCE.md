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
| E-04 | TAL-03 | 34-01 | pass |
| E-05 | TAL-02, TAL-05 | 34-02 | pass |
| E-06 | TAL-03, TAL-05 | 34-02 | pass |
| E-07 | TAL-03 | 34-02 | pass |
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

### E-04: Post-merge break inventory on the bare merge (before any repair)

Backs: TAL-03 (attribution half), ROADMAP Phase 34 criterion 2 (every compile break is measured on the bare merge, attributable to the tail, before plan 34-02 repairs it).

```sh
git rev-parse HEAD
git diff --quiet adaf8c4e76375e5099c076f2ca3bace892709765 HEAD -- . ':(exclude).planning'
env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain compileJava compileTestFixturesJava > /tmp/gsd34/exec/compile-main-m3.log 2>&1
env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain compileTestJava > /tmp/gsd34/exec/compile-test-m3.log 2>&1
grep -E '\.java:[0-9]+: error:' /tmp/gsd34/exec/compile-test-m3.log
grep -rl 'new CreateTableStatement(' src
grep -c 'public static void handleException(' src/main/java/com/clickhouse/kafka/connect/util/Utils.java
grep -rn 'handleException(' src
grep -rn 'new ServerException(' src
grep -c '^# Unreleased' CHANGELOG.md
```

```text
measured_head: 3bcc27f6db978a150d899e6d45928c1b33650326
tree_vs_m3_outside_planning: identical
compile_java_exit: 0
compile_testfixtures_java_exit: 0
compile_test_java_exit: 1
compile_test_java_errors: 5
compile_test_java_error_files: AdditionalRetriableErrorCodesTest.java x3; UtilsNullMessageClassificationUnitTest.java x2
error_excerpt_seam: AdditionalRetriableErrorCodesTest.java:59: error: incompatible types: FailOnceDBWriter cannot be converted to String (new ProxySinkTask(config, null, writer))
error_excerpt_handleexception: UtilsNullMessageClassificationUnitTest.java:32: error: method handleException in class Utils cannot be applied to given types (Utils.handleException(e, errorsTolerance, NO_SOCKET_RETRY, new ArrayList<>()))
break_prediction_met: yes
create_table_statement_ctor_sites: src/testFixtures/java/com/clickhouse/kafka/connect/sink/helper/CreateTableStatement.java
handle_exception_overloads: 1
handle_exception_callers: ChunkFlusher.java x1; ProxySinkTask.java x1; UtilsNullMessageClassificationUnitTest.java x1; UtilsTest.java x12
server_exception_sites: 9
server_exception_site_files: AdditionalRetriableErrorCodesTest.java x1; UtilsTest.java x8
changelog_unreleased_present: yes
```

The five errors are exactly the research prediction: three on the missing `ProxySinkTask(ClickHouseSinkConfig, ErrorReporter, DBWriter)` seam constructor (lines 59, 76, 88) and two on the one line of the stale four-argument `handleException` call (line 32; javac reports the method-applicability error and the follow-on `ArrayList<>` inference error). Only `CreateTableStatement.java` itself calls a `CreateTableStatement` constructor, so the TAL-03 `CreateTableStatement` clause needs no fork edit. `handle_exception_callers` counts every `handleException(` token outside the declaration, including test method names and calls in `UtilsTest`. Client-v2 0.11.0 has only the four-argument `ServerException(int, String, int, String)` constructor (read with `javap` at planning), so Phase 36 revisits 9 sites in 2 files, not the eight the ROADMAP note names.

Timestamp (UTC): 2026-10-28T21:19:37Z to 2026-10-28T21:19:45Z

Verdict: pass

### E-05: ProxySinkTask re-expressed with three constructors and its 8 anchor rows

Backs: TAL-02, TAL-05, ROADMAP Phase 34 criteria 2 and 4 (the fork's connectorName constructor and upstream's DBWriter seam both delegate to one four-argument core, with the ProxySinkTask anchor rows landing in the same commit; the tail gate over M3..that commit passes).

```sh
env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain compileJava > /tmp/gsd34/exec/compilejava-05.log 2>&1
javap -p -cp build/classes/java/main com.clickhouse.kafka.connect.sink.ProxySinkTask | grep 'ProxySinkTask('
env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain compileTestJava > /tmp/gsd34/exec/compiletest-05.log 2>&1
grep -E '\.java:[0-9]+: error:' /tmp/gsd34/exec/compiletest-05.log
python3 -I .planning/phases/34-upstream-tail-merge/34-evidence-check.py anchors .planning/scripts/fork-anchors.txt
git worktree add --detach /tmp/gsd34/fmt HEAD
env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew -p /tmp/gsd34/fmt --console=plain spotlessApply > /tmp/gsd34/exec/fmt-1.log 2>&1
cmp /tmp/gsd34/fmt/src/main/java/com/clickhouse/kafka/connect/sink/ProxySinkTask.java src/main/java/com/clickhouse/kafka/connect/sink/ProxySinkTask.java
git worktree remove --force /tmp/gsd34/fmt
git add src/main/java/com/clickhouse/kafka/connect/sink/ProxySinkTask.java .planning/scripts/fork-anchors.txt
git commit -F /tmp/gsd34/exec/msg-t1.txt
PRE_TAG=pre-v1.7.0-tail-2 UPSTREAM_REF=9bed6f7970670a55c66be8efda246889e1f95bd0 CONFLICTED_PATHS_FILE=.planning/scripts/conflicted-paths-v1.7.0-tail-2.txt bash .planning/scripts/verify-merge-invariants.sh adaf8c4e76375e5099c076f2ca3bace892709765 608d70a15cc898292daea425819edfd1c2eca405 > /tmp/gsd34/exec/tail-gate-05.log 2>&1
```

```text
compile_java_exit: 0
declared_constructors: public ProxySinkTask(ClickHouseSinkConfig, ErrorReporter, String); ProxySinkTask(ClickHouseSinkConfig, ErrorReporter, DBWriter) [package-private]; ProxySinkTask(ClickHouseSinkConfig, ErrorReporter, String, DBWriter) [package-private]
compile_test_java_exit: 1
compile_test_java_errors: 2
compile_test_java_error_files: UtilsNullMessageClassificationUnitTest.java x2
anchors_mirror: rows=139 bad=0
new_anchor_rows: 8
spotless_worktree_cmp: identical
proxysinktask_commit: 608d70a15cc898292daea425819edfd1c2eca405
proxysinktask_commit_files: .planning/scripts/fork-anchors.txt; src/main/java/com/clickhouse/kafka/connect/sink/ProxySinkTask.java
tail_gate_line: invariants: provenance_paths=27 anchor_commits=6 anchor_rows=139 marker_commits=13 anchor_exempt=0 result=PASS
tail_gate_exit: 0
```

The seam errors from E-04 (three in `AdditionalRetriableErrorCodesTest`) are gone; the two remaining errors are the stale four-argument `handleException` call on line 32 of `UtilsNullMessageClassificationUnitTest.java`. The tail gate log holds no VIOLATION line. The start time below is approximate (reconstructed from log file times; the clock was not captured at the first edit).

Timestamp (UTC): 2026-10-28T21:21:40Z to 2026-10-28T21:22:48Z

Verdict: pass

### E-06: The stale handleException caller on the five-argument form, the Utils anchor rows and the four compile tasks

Backs: TAL-03, TAL-05, ROADMAP Phase 34 criteria 2 and 4 (every fork caller of Utils.handleException uses the five-argument form; compileJava, compileTestJava, compileTestFixturesJava and compileIntegrationTestJava pass; the DEFAULT_RETRIABLE_ERROR_CODES lookup and the signature are anchored).

```sh
env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain compileJava compileTestJava compileTestFixturesJava compileIntegrationTestJava > /tmp/gsd34/exec/compile-gates-06.log 2>&1
grep -rn 'handleException(' src
python3 -I .planning/phases/34-upstream-tail-merge/34-evidence-check.py anchors .planning/scripts/fork-anchors.txt
git worktree add --detach /tmp/gsd34/fmt HEAD
env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew -p /tmp/gsd34/fmt --console=plain spotlessApply > /tmp/gsd34/exec/fmt-2.log 2>&1
cmp /tmp/gsd34/fmt/src/test/java/com/clickhouse/kafka/connect/sink/util/UtilsNullMessageClassificationUnitTest.java src/test/java/com/clickhouse/kafka/connect/sink/util/UtilsNullMessageClassificationUnitTest.java
git worktree remove --force /tmp/gsd34/fmt
git add src/test/java/com/clickhouse/kafka/connect/sink/util/UtilsNullMessageClassificationUnitTest.java .planning/scripts/fork-anchors.txt
git commit -F /tmp/gsd34/exec/msg-t2.txt
PRE_TAG=pre-v1.7.0-tail-2 UPSTREAM_REF=9bed6f7970670a55c66be8efda246889e1f95bd0 CONFLICTED_PATHS_FILE=.planning/scripts/conflicted-paths-v1.7.0-tail-2.txt bash .planning/scripts/verify-merge-invariants.sh adaf8c4e76375e5099c076f2ca3bace892709765 4c9b3ed8dad6efb56220c8673448238ce4ce0da5 > /tmp/gsd34/exec/tail-gate-06.log 2>&1
```

```text
compile_gates_exit: 0
handle_exception_callers: ChunkFlusher.java x1; ProxySinkTask.java x1; UtilsNullMessageClassificationUnitTest.java x1; UtilsTest.java x12
anchors_mirror: rows=141 bad=0
new_anchor_rows: 2
spotless_worktree_cmp: identical
handle_exception_commit: 4c9b3ed8dad6efb56220c8673448238ce4ce0da5
handle_exception_commit_files: .planning/scripts/fork-anchors.txt; src/test/java/com/clickhouse/kafka/connect/sink/util/UtilsNullMessageClassificationUnitTest.java
tail_gate_line: invariants: provenance_paths=27 anchor_commits=8 anchor_rows=141 marker_commits=15 anchor_exempt=0 result=PASS
tail_gate_exit: 0
```

The only edit is the one stale call in `UtilsNullMessageClassificationUnitTest.java` (an empty `Set.of()` constant as the fourth argument, never null) plus its `java.util.Set` import. `handle_exception_callers` counts every `handleException(` token outside the declaration, as in E-04. No fork file calls a `CreateTableStatement` constructor, so there is no fork edit for that refactor. The start time below is approximate (reconstructed from the compile gate's wall clock; the clock was not captured at the first edit).

Timestamp (UTC): 2026-10-28T21:47:30Z to 2026-10-28T21:48:53Z

Verdict: pass

### E-07: BLD-16 red before the re-bless, worker jars from the local image, the re-bless and testUnit green

Backs: TAL-03, ROADMAP Phase 34 criterion 2 (the BLD-16 Kafka API list is re-blessed from the production worker's own jars after the tail added a ConfigException(String, Object, String) reference).

```sh
rm -rf build/test-results/testUnit
env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain testUnit --rerun > /tmp/gsd34/exec/testunit-red-07.log 2>&1
python3 -I .planning/phases/34-upstream-tail-merge/34-evidence-check.py failed build/test-results/testUnit --expect 'ConnectApiReferencesUnitTest#BLD-16: every org.apache.kafka class and member the compiled connector references is in the blessed worker-API list'
docker image inspect --format '{{.Id}}' confluentinc/cp-kafka-connect-base:7.9.7
docker create confluentinc/cp-kafka-connect-base:7.9.7
docker cp <container>:/usr/share/java/kafka/connect-api-7.9.7-ccs.jar /tmp/gsd34/img/
docker cp <container>:/usr/share/java/kafka/kafka-clients-7.9.7-ccs.jar /tmp/gsd34/img/
docker rm <container>
shasum -a 256 /tmp/gsd34/img/*.jar
env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain compileJava
python3 -I .planning/scripts/connect-api-refs.py --classes build/classes/java/main --compile-classpath <4.4.0 connect-api>:<4.4.0 kafka-clients> --reference worker=/tmp/gsd34/img/connect-api-7.9.7-ccs.jar:/tmp/gsd34/img/kafka-clients-7.9.7-ccs.jar --reference 4.1.1=<4.1.1 connect-api>:<4.1.1 kafka-clients> --reference 4.4.0=<4.4.0 connect-api>:<4.4.0 kafka-clients> --image-note "image: confluentinc/cp-kafka-connect-base@sha256:93b9afd3e7f8003381401464e4e4fa3dbe5e3766bcbc99de12afd41dc2904055 (tag 7.9.7; linux/amd64 and linux/arm64 jars byte-identical; Temurin 17.0.19)" --write src/test/resources/connect-api-references.txt > /tmp/gsd34/exec/rebless-07.log 2>&1
git diff -U0 -- src/test/resources/connect-api-references.txt
git add src/test/resources/connect-api-references.txt
git commit -F /tmp/gsd34/exec/msg-t2b.txt
rm -rf build/test-results/testUnit
env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain testUnit --rerun > /tmp/gsd34/exec/testunit-green-07.log 2>&1
```

```text
testunit_red_exit: 1
testunit_red_failed: ConnectApiReferencesUnitTest#BLD-16: every org.apache.kafka class and member the compiled connector references is in the blessed worker-API list
red_prediction_met: yes
worker_image_id: sha256:93b9afd3e7f8003381401464e4e4fa3dbe5e3766bcbc99de12afd41dc2904055
worker_connect_api_sha256: b57546e9160d5435238fded6f38756d621fac62bd9f64737c3f68e094ae01de4
worker_kafka_clients_sha256: 28423086fbbf8f91b2f1d335eeeca6675916e24adf2f813b3f9c47f87b49f962
worker_jar_hashes_match_header: yes
reference_jar_hashes_match_header: yes
rebless_exit: 0
rebless_summary: REFERENCE worker classes-missing 0 members-missing 0; REFERENCE 4.1.1 classes-missing 0 members-missing 0; REFERENCE 4.4.0 classes-missing 0 members-missing 0
rebless_diff: one removed and one added date line (2026-10-21T16:23:41Z to 2026-10-28T21:50:10Z) and one added M line for org/apache/kafka/common/config/ConfigException.<init>:(Ljava/lang/String;Ljava/lang/Object;Ljava/lang/String;)V
rebless_image_line_unchanged: yes
rebless_commit: 8bd35e59f9f21f160e42d1346e2288ecfbed822c
testunit_green_exit: 0
testunit_tests: 1418
testunit_failures: 0
testunit_errors: 0
testunit_skipped: 2
tail_tests_added: 31
tail_tests_added_matches_rehearsal: no
testunit_total_matches_rehearsal_total: yes
```

The red run failed exactly one testcase, the BLD-16 reference check, and its message named exactly one unlisted entry: the `ConfigException(String, Object, String)` constructor. The plan's `--expect` string uses the Java method name (`everyReferencedKafkaClassAndMemberIsInTheBlessedList`), but the helper reads the JUnit XML testcase name, which is the `@DisplayName` text, so the method-name form printed FAIL (unexpected: the display name; missing: the method name). With the display name as the expected testcase name the helper prints `OK failed: 1 failing testcases, 1 expected`; the two names are the same single test (the XML holds that DisplayName under the test class, whose method is `everyReferencedKafkaClassAndMemberIsInTheBlessedList`). `red_prediction_met: yes` rests on that equivalence, not on the literal command in the plan.

`tail_tests_added: 31` is 1418 minus E-01's baseline of 1387. The rehearsal's delta of 33 was measured against its own baseline of 1385; the rehearsal's absolute testUnit total was 1418, and this tree's total is exactly 1418 over 66 XML files, so the tail's test content equals the rehearsal's. `tail_tests_added_matches_rehearsal: no` records the arithmetic delta honestly; the two-test baseline difference (1385 in the research, 1387 measured in E-01 on an unchanged `src`) is not explained here. The worker jars were copied from the already-local image (no pull) into `/tmp/gsd34/img/` and treated as data.

Timestamp (UTC): 2026-10-28T21:48:59Z to 2026-10-28T21:50:22Z

Verdict: pass
