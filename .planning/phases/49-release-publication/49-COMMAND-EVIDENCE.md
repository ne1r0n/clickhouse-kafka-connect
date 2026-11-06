# Phase 49: Command evidence

- **Base commit:** `local_head` = `816d3b554045c5e1368b5e5090d8674695596cb4` (HEAD when plan 49-01 started, branch `sync/v1.7.0`); this is L0.
- **The U-2 merge:** `merge_commit` = `77ea9c8ccb1ff7b6a7f04fa06c13a515174dda1f` (B1), the local tree-neutral merge of `94834763` into `sync/v1.7.0`; its tree equals L0's tree.
- **Shell:** macOS zsh, multi-line logic under `/bin/bash` or `python3 -I`. Gradle is run as `env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain ...`; ClickHouse-backed runs prefix `CLICKHOUSE_VERSION=latest CLIENT_VERSION=V2`.
- **Docker:** colima profile `vz`.
- **Date (UTC):** 2026-11-06.
- **Entry format:** each entry has a `### E-NN: {title}` heading, a `Backs:` line, an `sh` fence with the commands as run, a `text` fence of `key: value` lines (keys match `[a-z0-9_]+`, one fact per line, no key twice) with free text allowed after the fence, a UTC timestamp line, and a final line `Verdict: pass`, `Verdict: fail`, `Verdict: pending ({reason})` or `Verdict: unmeasured ({reason})`.
- **Rule:** a fail or pending verdict is fixed by re-measuring in a new run, never by editing the evidence. Every expected number is re-measured here; a measured value that differs from the expectation is recorded and used, never forced.
- **Helpers:** `49-evidence-check.py` (`7123d354f51d45e019837f43b3a30fbc`), `49-testcase-status.py` (`41f14e07ae7b402ad2e29bfc381a2aff`), `49-allowlist-check.py` (`886006e1302ff7f9b0c2ac023639a38d`), `49-hygiene-check.py` (`8d18a641065b320e28e571a19679ab98`), `49-doc-check.py` (`110fd44a2495ebd4b9845675a0419ad7`), `49-path-check.py` (`641402547d365dc5b663d6c88d6e1604`), `49-id-scan.py` (`0922b7a9ab304cb8a77f181798827ed6`), `49-review-check.py` (`4fce996a3b9e8676bf7b5a4f7100a613`) and `49-note-check.py` (`4773618affcaf355632262823a1723a9`) are read-only plan-time files; they are never edited.
- **Evidence ids:** plans make E-01 to E-20; E-15 to E-19 are the pending records of the five user handoffs and stay pending; E-20 is the local close; the tail from E-21 on is recorded by the orchestrator.
- **No push happens in plans 49-01 to 49-05.** Reads use `git ls-remote`, `gh api` GET and `curl -s` GET only.

## Index

| Entry | Requirement | Plan | Verdict |
|-------|-------------|------|---------|
| E-01 | RLS-01 | 49-01 | pass |
| E-02 | RLS-01 | 49-01 | pass |
| E-03 | RLS-01 | 49-01 | pass |
| E-04 | RLS-01, RLS-04 | 49-01 | pass |
| E-05 | RLS-02, RLS-03 | 49-02 | pass |
| E-06 | RLS-03 | 49-02 | pass |
| E-07 | RLS-03 | 49-02 | pass |
| E-08 | RLS-03 | 49-03 | pass |
| E-09 | RLS-03 | 49-03 | pass |
| E-10 | RLS-03 | 49-03 | pass |
| E-11 | RLS-02 | 49-04 | pending (plan 49-04) |
| E-12 | RLS-04 | 49-04 | pending (plan 49-04) |
| E-13 | RLS-04 | 49-04 | pending (plan 49-04) |
| E-14 | RLS-04, RLS-05 | 49-05 | pending (plan 49-05) |
| E-15 | RLS-02, RLS-05 | 49-05 | pending (plan 49-05) |
| E-16 | RLS-05 | 49-05 | pending (plan 49-05) |
| E-17 | RLS-05 | 49-05 | pending (plan 49-05) |
| E-18 | RLS-05 | 49-05 | pending (plan 49-05) |
| E-19 | RLS-06 | 49-05 | pending (plan 49-05) |
| E-20 | RLS-01 to RLS-06 | 49-05 | pending (plan 49-05) |

### E-01: Pre-flight, the tree-neutral merge of the gg7 release merge, the retag of pre-v1.7.0_gg8 and the tail gate on the merge

Backs: RLS-01, U-1 (the local retag of `pre-v1.7.0_gg8`), U-2 (the local tree-neutral merge of the published gg7 merge), D-8 (upstream and divergence rechecked before any change), PL-8 (the tail gate on the merge commit with the retagged `PRE_TAG`).

```sh
git rev-parse --abbrev-ref HEAD
git rev-parse HEAD
git status --porcelain
python3 -I (md5 of the nine plan-time helpers)
bash .planning/milestones/v1.7.0_gg6-maintenance-phases/38-release-publication/38-assets/hygiene-selftest.sh.txt .planning/phases/49-release-publication/49-hygiene-check.py <new dir>
colima list
python3 -I .planning/phases/49-release-publication/49-evidence-check.py anchors .planning/scripts/fork-anchors.txt --rows 218
git diff --quiet a968e4867f374f09177faadd80f1462b9a146233 HEAD -- . ':(exclude).planning'
git ls-remote origin refs/heads/custom_build
git rev-list --left-right --count origin/custom_build...sync/v1.7.0
git rev-list origin/custom_build --not HEAD
git rev-list --count HEAD --not --remotes=origin
git ls-remote --heads origin
git ls-remote --tags origin 'v1.7.0_gg8*' 'pre-v1.7.0_gg8*'
git tag -l 'v1.7.0_gg8'
gh api 'repos/ne1r0n/clickhouse-kafka-connect/pulls?state=open&per_page=50' --jq '[.[] | "\(.number) \(.head.sha[0:8])"] | sort | join("; ")'
gh api repos/ne1r0n/clickhouse-kafka-connect/releases --jq '[.[] | select(.tag_name=="v1.7.0_gg7") | "\(.id) \(.tag_name) \(.draft)"] | join("; ")'
git ls-remote upstream refs/heads/main
git ls-remote --tags --refs upstream 'v*'
curl -s https://api.github.com/repos/ClickHouse/clickhouse-kafka-connect/pulls/<n>   # n = 861, 855, 854, 788
docker run --rm --entrypoint clickhouse clickhouse/clickhouse-server:latest local --query 'SELECT version()'
docker image inspect --format '{{index .RepoDigests 0}}' clickhouse/clickhouse-server:latest
sysctl vm.swapusage
git tag --merged HEAD --list 'v*' | python3 -I .github/scripts/baseline_tag.py v1.7.0_gg8
git merge-tree --write-tree 9483476322e0d331e81c5b92ac266a18f0f01450 HEAD
git --attr-source="$(git hash-object -t tree /dev/null)" merge --no-ff -F <msg file> 9483476322e0d331e81c5b92ac266a18f0f01450
git tag -f pre-v1.7.0_gg8 9483476322e0d331e81c5b92ac266a18f0f01450
PRE_TAG=pre-v1.7.0_gg8 UPSTREAM_REF=83808df9a0993cf49b2ceaa4633c782e550c0180 CONFLICTED_PATHS_FILE=.planning/scripts/conflicted-paths-v1.7.0-tail-3.txt bash .planning/scripts/verify-merge-invariants.sh 12e2cf17cca9f5b17917dd9aceaa38b44f3f8b10 77ea9c8ccb1ff7b6a7f04fa06c13a515174dda1f
```

```text
branch: sync/v1.7.0
local_head: 816d3b554045c5e1368b5e5090d8674695596cb4
dirty_paths: M .planning/config.json; M .planning/state.json
helpers_md5: ok
helpers_tracked: yes
hygiene_selftest: selftest_default_hits=7 selftest_strict_hits=9
colima_vz: Running
python_version: Python 3.14.8
git_version: git version 2.50.1 (Apple Git-155)
version_file: v1.7.0_gg7
anchors_version_row: VERSION|v1.7.0_gg7|1|BLD-07
anchors_mirror: rows=218 bad=0
tree_equals_phase48_gate: yes
changelog_tail_equals_gg7: yes
origin_custom_build: 9483476322e0d331e81c5b92ac266a18f0f01450
origin_tracking_equal: yes
left_right_before: 1 223
left_count_before: 1
left_commit_before: 9483476322e0d331e81c5b92ac266a18f0f01450
unpublished_before: 223
origin_heads: 939e8992 refs/heads/scratch/clt-07-prebump-0.9.6; 94834763 refs/heads/custom_build; bec167d8 refs/heads/main; de8c685b refs/heads/scratch/clt-07-client-0.11.0
origin_sync_ref: none
origin_gg8_tags: none
local_release_tag: none
scratch_tips: de8c685b scratch/clt-07-client-0.11.0; 939e8992 scratch/clt-07-prebump-0.9.6
scratch_tips_ancestors: yes
origin_open_prs: none
releases_gg7: 433237218 v1.7.0_gg7 false
upstream_main: 83808df9a0993cf49b2ceaa4633c782e550c0180
upstream_newest_tag: v1.7.0
upstream_tag_past_v170: no
upstream_pr_861: open False b62e0ae4
upstream_pr_855: open False ddabf28b
upstream_pr_854: open False 69d1efc5
upstream_pr_788: closed False bb4b4140
upstream_prs_unchanged: yes
clickhouse_version: 26.10.3.7
clickhouse_digest: clickhouse/clickhouse-server@sha256:739e099abd1157390960a59e4532d21781d8fde57e6e4151cb920ea02c30b1ce
digest_matches_research: yes
swap_used: vm.swapusage: total = 35840.00M  used = 34860.25M  free = 979.75M  (encrypted)
baseline_before_merge: v1.7.0_gg6
merge_tree_probe: yes
merge_commit: 77ea9c8ccb1ff7b6a7f04fa06c13a515174dda1f
merge_parents: 816d3b554045c5e1368b5e5090d8674695596cb4 9483476322e0d331e81c5b92ac266a18f0f01450
merge_tree_equal_first_parent: yes
merge_status_after: M .planning/config.json; M .planning/state.json
left_right_after: 0 224
left_count_after: 0
origin_ahead_none: yes
pre_tag_before: baa404cd5ed95c556db0acff84def1024c3bafe3
pre_tag_after: 9483476322e0d331e81c5b92ac266a18f0f01450
pre_tag_type: commit
pre_tag_on_origin: absent
baseline_after_merge: v1.7.0_gg7
tail_gate_merge_end: 77ea9c8ccb1ff7b6a7f04fa06c13a515174dda1f
tail_gate_merge_line: invariants: provenance_paths=11 anchor_commits=206 anchor_rows=218 marker_commits=224 anchor_exempt=0 result=PASS
tail_gate_merge_exit: 0
```

The planning HEAD read `1 222` (plan-time); it read `1 223` here because the plan commit for this phase sits one commit later. The one left commit is the published gg7 merge. `origin_heads` shows the origin `main` as `bec167d8` (the fork's own `main`, unrelated to upstream `main`). Swap was 97 % used when the pre-flight ran. The tail gate ran alone, with no Gradle or Docker job alive, and its counts (`anchor_commits=206`, `marker_commits=224`) are at or above the plan-time probe's 204 and 222, with `anchor_rows=218` and `result=PASS`. The merge is the first change of the plan; the local tag `pre-v1.7.0_gg8` moved from `baa404cd` to `94834763` only after it; neither was pushed.

2026-11-06T13:07:02Z
Verdict: pass

### E-02: Tracer: VERSION bumped through the script with its anchors row; the generated constant, the baseline tag, the anchors mirror and the guards agree

Backs: RLS-01 (VERSION set by `bump_version.py`, the `VERSION` anchors row in the same commit, the generated version agrees after a clean build, the baseline selection picks `v1.7.0_gg7`).

```sh
python3 -I .github/scripts/bump_version.py gg --version-file <scratch>/VERSION --anchors-file <scratch>/fork-anchors.txt
git diff --no-index --numstat <scratch>/VERSION.orig <scratch>/VERSION
git diff --no-index --numstat <scratch>/fork-anchors.orig <scratch>/fork-anchors.txt
python3 -I .github/scripts/bump_version.py gg
git diff --name-only
git diff --numstat -- VERSION .planning/scripts/fork-anchors.txt
python3 -I .planning/phases/49-release-publication/49-evidence-check.py anchors .planning/scripts/fork-anchors.txt --rows 218
./gradlew --stop
env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain clean generateVersionFile
grep -n ARTIFACT_VERSION build/generated/sources/version/java/main/com/clickhouse/kafka/connect/sink/Version.java
rm -rf build/test-results/testUnit
DOCKER_HOST=unix:///nonexistent.sock env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain testUnit --rerun --tests '*BumpVersionScriptUnitTest' --tests '*BaselineTagScriptUnitTest' --tests '*MBeanNameUnitTest' --tests '*ClientEndpointAndOptionsPinUnitTest'
python3 -I .planning/phases/49-release-publication/49-evidence-check.py failed build/test-results/testUnit
python3 -I .planning/phases/49-release-publication/49-evidence-check.py junit build/test-results/testUnit --class .BumpVersionScriptUnitTest   # and the other three classes
git add VERSION .planning/scripts/fork-anchors.txt
git commit -F <msg file>
git tag --merged HEAD --list 'v*' | python3 -I .github/scripts/baseline_tag.py "$(tr -d '[:space:]' < VERSION)"
```

```text
scratch_bump_stdout: v1.7.0_gg8
scratch_bump_numstat: 1/1; 1/1
bump_stdout: v1.7.0_gg8
bump_version_file: v1.7.0_gg8
bump_changed_files: .planning/scripts/fork-anchors.txt; VERSION
bump_numstat: 1/1; 1/1
version_rows_in_anchors: 1
anchors_mirror_after: rows=218 bad=0
version_clean_exit: 0
generated_version_line: public static final String ARTIFACT_VERSION = "v1.7.0_gg8";
bump_guard_exit: 0
bump_guard_failed: OK failed: 0 failing testcases, 0 expected
bump_xml: OK junit build/test-results/testUnit class *.BumpVersionScriptUnitTest: tests=52 failures=0 errors=0 skipped=0 files=1
baseline_xml: OK junit build/test-results/testUnit class *.BaselineTagScriptUnitTest: tests=19 failures=0 errors=0 skipped=0 files=1
mbean_xml: OK junit build/test-results/testUnit class *.MBeanNameUnitTest: tests=11 failures=0 errors=0 skipped=0 files=1
endpoint_xml: OK junit build/test-results/testUnit class *.ClientEndpointAndOptionsPinUnitTest: tests=5 failures=0 errors=0 skipped=0 files=1
bump_commit: b6666fe748629ea597a7534fba0d9a2e6ef54cee
bump_commit_files: .planning/scripts/fork-anchors.txt; VERSION
baseline_after_bump: v1.7.0_gg7
allowlist_gg8_present: no
note_gg8_present: no
release_tag_absent: yes
src_unchanged: yes
hygiene_scan: clean
```

The four guard classes ran with Docker unreachable. The first run of the generated-constant check used the clean build the plan names, because `generateVersionFile` keeps a stale file otherwise. The Gradle runs used the default toolchain (Gradle 9.8.2).

2026-11-06T13:09:30Z
Verdict: pass

### E-03: The 1.7.0_gg8 CHANGELOG section completed under the Unreleased heading, released sections untouched

Backs: RLS-01 (the CHANGELOG section of the release), D-3 (content), D-9 (the two-ERROR-line wording of FORK-DELTA item 14 as reworded at its last edit), U-3 and PL-5 (the heading date is set later, in one line).

```sh
git show v1.7.0_gg7:CHANGELOG.md > <scratch>/changelog-gg7.md
git show HEAD:CHANGELOG.md > <scratch>/changelog-old.md
python3 -I (tail check: the file ends with a newline followed by the gg7 tag copy; the old section lines occur in order)
python3 -I (section check: headings, no identifier, line width, required tokens)
git diff -U0 HEAD -- CHANGELOG.md   # added lines, written to a file
python3 -I .planning/phases/49-release-publication/49-hygiene-check.py --strict <added lines file>
python3 -I .planning/phases/49-release-publication/49-doc-check.py links CHANGELOG.md
git add CHANGELOG.md
git commit -F <msg file>
python3 -I .planning/phases/49-release-publication/49-id-scan.py d2240a706587134fb24d050fa613002a9d093fe2 CHANGELOG.md
```

```text
changelog_heading: # 1.7.0_gg8, Unreleased
changelog_tail_identical: yes
phase47_bullets_kept: yes
changelog_section_check: OK changelog-section
id_scan: ids: hits=0
links_ok: yes
hygiene_scan_added_lines: clean
secrets_scan: clean
changelog_commit: d2240a706587134fb24d050fa613002a9d093fe2
changelog_commit_files: CHANGELOG.md
added_lines: 75
```

The section holds the five headings in order. The three bullets written earlier (one under Bug Fixes, two under Known Issues) are unchanged line for line. The only broken link of the file is the coming gg8 operator note, which plan 49-02 creates. A client V2 only effect and a client V1 only effect are each stated for their client. The fork's value-free wording for the record context says that a Jackson link in a cause chain is rebuilt without a record value, because the code rebuilds it, and not that every exception is rethrown untouched. The Known Issues bullet for the two ERROR lines is worded after FORK-DELTA item 14.

2026-11-06T13:11:09Z
Verdict: pass

### E-04: The measured base on the bumped tree and docs/FORK-DELTA.md final for gg8

Backs: RLS-01 (the final FORK-DELTA pass with its raw numbers refreshed once), RLS-04 (the measured base for the totals: testUnit with Docker unreachable and one full suite on ClickHouse `latest` with client V2), D-5, D-9, D-10.

```sh
./gradlew --stop
DOCKER_HOST=unix:///nonexistent.sock docker info
rm -rf build/test-results/testUnit
DOCKER_HOST=unix:///nonexistent.sock env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain testUnit --rerun
python3 -I .planning/phases/49-release-publication/49-evidence-check.py junit build/test-results/testUnit
python3 -I .planning/phases/49-release-publication/49-evidence-check.py failed build/test-results/testUnit
cp -R build/test-results/testUnit <scratch>/xml-base-testunit
python3 -I .planning/phases/49-release-publication/49-testcase-status.py dump <scratch>/xml-base-testunit <scratch>/status-base-testunit.tsv
./gradlew --stop
docker image inspect --format '{{index .RepoDigests 0}}' clickhouse/clickhouse-server:latest
docker run --rm --entrypoint clickhouse clickhouse/clickhouse-server:latest local --query 'SELECT version()'
rm -rf build/test-results/test
CLICKHOUSE_VERSION=latest CLIENT_VERSION=V2 env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain test --rerun
python3 -I .planning/phases/49-release-publication/49-evidence-check.py junit build/test-results/test
python3 -I .planning/phases/49-release-publication/49-evidence-check.py skips build/test-results/test
python3 -I .planning/phases/49-release-publication/49-evidence-check.py failed build/test-results/test
cp -R build/test-results/test <scratch>/xml-base-suite
python3 -I .planning/phases/49-release-publication/49-testcase-status.py dump <scratch>/xml-base-suite <scratch>/status-base-suite.tsv
docker image inspect --format '{{index .RepoDigests 0}}' clickhouse/clickhouse-server:latest
docker run --rm --entrypoint clickhouse clickhouse/clickhouse-server:latest local --query 'SELECT version()'
git diff --shortstat v1.7.0 HEAD -- src build.gradle.kts gradle .github   # and the other scopes of the facts below
python3 -I <scratch>/recount.py recount HEAD   # and v1.7.0_gg7; then: tree
python3 -I .planning/phases/49-release-publication/49-doc-check.py numbers docs/FORK-DELTA.md 'after gg8: `v1.7.0` to `v1.7.0_gg8`' --wider "364 47983 1260" --narrow "43 3578 333"
python3 -I .planning/phases/49-release-publication/49-path-check.py 7fed6067f1fe97880d7bc88d00efac4aab00b034 docs/FORK-DELTA.md
python3 -I .planning/phases/49-release-publication/49-doc-check.py links docs/FORK-DELTA.md
git add docs/FORK-DELTA.md
git commit -F <msg file>
```

```text
testunit_head: a0438464d0270760ab3bc55b50dedd03547b5e36
docker_unreachable_proof: exit=1
testunit_exit: 0
testunit_tests: 2325
testunit_failures: 0
testunit_errors: 0
testunit_skipped: 2
testunit_count_matches_e19: yes
testunit_failed_ok: yes
testunit_jvm_crash: none
digest_before_suite: clickhouse/clickhouse-server@sha256:739e099abd1157390960a59e4532d21781d8fde57e6e4151cb920ea02c30b1ce
select_version_before_suite: 26.10.3.7
suite_exit: 0
suite_tests: 2928
suite_failures: 0
suite_errors: 0
suite_skipped: 18
suite_skips: OK skips: named=7 state_mismatch_cases=11 unexpected=0
suite_failed_ok: yes
suite_matches_derived: yes
suite_java: default toolchain
suite_jvm_crash: none
digest_after_suite: clickhouse/clickhouse-server@sha256:739e099abd1157390960a59e4532d21781d8fde57e6e4151cb920ea02c30b1ce
select_version_after_suite: 26.10.3.7
wider_now: 364 47983 1260
narrow_now: 43 3578 333
wider_since_gg7: 36 5602 97
narrow_since_gg7: 6 428 72
by_dir_since_gg7: src/test=28 5172 25; src/integrationTest=0 0 0; src/testFixtures=0 0 0; .github=0 0 0; gradle=1 1 0; build.gradle.kts=1 1 0
src_main_files_since_gg7: ClickHouseHelperClient.java; ClickHouseSinkConfig.java; ClickHouseSinkTask.java; ClickHouseWriter.java; DeclaredNullability.java; Utils.java
recount_now: 30 14 19 10
recount_gg7: 30 14 19 10
test_tree_recount: 143 115
test_tree_differing: AvroDecimalLogicalTest; AvroNullableUnionVariantTest; ClickHouseBase; ClickHouseCloudTest; ClickHouseHelperClientTest; ClickHouseSinkConnectorIntegrationTest; ClickHouseSinkJdbcPropertiesTest; ClickHouseSinkTaskBufferTest; ClickHouseSinkTaskMappingTest; ClickHouseSinkTaskSchemalessProxyTest; ClickHouseSinkTaskSchemalessTest; ClickHouseSinkTaskStringTest; ClickHouseSinkTaskTest; ClickHouseSinkTaskWithSchemaProxyTest; ClickHouseSinkTaskWithSchemaTest; ClickHouseTestHelpers; ClickHouseWriterColumnContextTest; ClickHouseWriterTest; ColumnTest; ConfluentPlatform; CreateTableStatement; ExactlyOnceTest; FailureTest; LocalProviderTest; ProcessingTest; ProtobufIngestTest; TableTest; UtilsTest
test_tree_categories: 2; 5; 2; 2; 1; 1; 1; 8; 6
numbers_check: OK numbers: label 'after gg8: `v1.7.0` to `v1.7.0_gg8`'
gg7_commands_left: 0
totals_sentence: yes
added_lines_ids: none
path_check_unresolved: .planning/scripts/confluent-archive-allowlist-v1.7.0_gg8.txt; docs/ROLLOUT-v1.7.0_gg8.md
links_ok: yes
hygiene_scan_added_lines: clean
secrets_scan: clean
anchors_mirror: rows=218 bad=0
items_checked: items 13, 14, 17, 20 and 24 read against the CHANGELOG section; no contradiction, no FORK-DELTA item edited
forkdelta_commit: 7fed6067f1fe97880d7bc88d00efac4aab00b034
forkdelta_commit_files: docs/FORK-DELTA.md
```

Every planning value held: testUnit 2325 tests with 2 skipped (the Phase 48 figure, unchanged by the bump), the suite 2928 tests with the 18 named skips (the derived total, first measured here), the six `src/main` files since gg7, and the figures of the recounts. The testUnit run took 38 s and the suite 3 min 25 s on the default toolchain, with no JVM crash and no crash file in the repository root. The two runs and the ClickHouse image lookups ran one at a time, never beside the tail gate. The server version and the image digest are the same before and after the suite.

The test-tree recount (143 files, 115 byte-identical, 28 differing) changed the nature of the sentence, not only its figures. The 15 files that FORK-DELTA listed as fork adaptation are still 13 named files plus `LocalProviderTest`, which is now the only file that differs in import order alone (the other file of the earlier pair, `ClickHouseSinkJdbcPropertiesTest`, was changed after `v1.7.0_gg1` by upstream's tail). Of the 14 further files, 8 are byte-identical to upstream's tail `83808df9` (upstream changed them after `v1.7.0`, the fork took the change with its tail merges), and 6 were changed by upstream's tail, by the fork or by both. The row therefore says the other 28 "differ", not that all of them are fork adaptation, and it names the reason of each of the 14 from the log of the file; the category counts sum to 28. One earlier line of the raw-numbers paragraph held a phase number inside a parenthetical ("a Phase 28 review-fix test"); because the plan's own check rejects any phase number on a changed line and that line had to change for the gg8 sentence, the parenthetical now reads "a review-fix test" and nothing else of that line moved.

2026-11-06T13:19:30Z
Verdict: pass

### E-05: Note tracer: the rehearsal script, the archives and source facts measured, preamble and section E, the rollback block rehearsed on a dry-run merge

Backs: RLS-02 (first measurement of the allow-list's difference set), RLS-03 (the rollback block of the note rehearsed unchanged), D-4, PL-7, PL-10.

```sh
git archive v1.7.0_gg7 | tar -x -C <new dir>/arch-base-src
git archive HEAD | tar -x -C <new dir>/arch-tip-src
env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain createConfluentArchive   # in each export, one after the other
python3 -I .github/scripts/compare_confluent_archives.py <dir>/base.zip <dir>/tip.zip <dir>/empty.txt   # empty.txt: a comment-only allow-list
python3 -I <scratch>/e05-cmp.py <compare output>   # counts UNALLOWED lines, splits them by owner, compares with the projection
gh api repos/ne1r0n/clickhouse-kafka-connect/releases/433237218 --jq '.assets[] | "\(.name)|\(.size)|\(.digest)|\(.uploader.login)"'
curl -sL -o <new dir>/asset.zip <release download URL of the v1.7.0_gg7 asset>
shasum -a 256 <new dir>/asset.zip
python3 -I .github/scripts/compare_confluent_archives.py <dir>/base.zip <new dir>/asset.zip <dir>/empty.txt
git diff --shortstat v1.7.0_gg7 HEAD -- src/main
bash .planning/phases/49-release-publication/49-rehearse.sh merge <note_commit_1> <new dir>/rh-d0 > rh-d0.txt
MODE=plain bash .planning/phases/49-release-publication/49-rehearse.sh merge <note_commit_1> <new dir>/rh-d0-plain > rh-d0-plain.txt
python3 -I .planning/phases/49-release-publication/49-note-check.py rows|labels|forbidden|rollback|claims docs/ROLLOUT-v1.7.0_gg8.md
python3 -I .planning/phases/49-release-publication/49-hygiene-check.py --strict docs/ROLLOUT-v1.7.0_gg8.md
python3 -I .planning/phases/49-release-publication/49-evidence-check.py secrets docs/ROLLOUT-v1.7.0_gg8.md
```

```text
rehearse_commit: dc91ab0c2b9ae2a1e8fdc022fb073704d1df8257
rehearse_md5: 67f30f31a1353a607aaed8d8f09767a7
rehearse_usage_selftest: 2 of 2 usage errors as expected
base_verify_line: verifyConfluentArchive: 7932 inner entries, no nested jar, no Kafka API class
tip_verify_line: verifyConfluentArchive: 7935 inner entries, no nested jar, no Kafka API class
base_entries: 7932
tip_entries: 7935
base_bytes: 13564144
tip_bytes: 13572233
archive_java: default toolchain (Gradle 9.8.2, launcher JVM 21.0.12.1), -PjavaVersion=21 not needed, no JVM crash
compare_exit: 1
compare_totals: totals files-a 7512 files-b 7515 only-a 0 only-b 3 changed 14 same 7498
compare_unallowed: 17
compare_unallowed_paths: META-INF/MANIFEST.MF; com/clickhouse/kafka/connect/sink/ClickHouseSinkConfig.class; com/clickhouse/kafka/connect/sink/ClickHouseSinkTask.class; com/clickhouse/kafka/connect/sink/Version.class; com/clickhouse/kafka/connect/sink/db/ClickHouseWriter$1.class; com/clickhouse/kafka/connect/sink/db/ClickHouseWriter$2.class; com/clickhouse/kafka/connect/sink/db/ClickHouseWriter$3.class; com/clickhouse/kafka/connect/sink/db/ClickHouseWriter$InsertBody.class; com/clickhouse/kafka/connect/sink/db/ClickHouseWriter$InsertWriteDeadline.class; com/clickhouse/kafka/connect/sink/db/ClickHouseWriter.class; com/clickhouse/kafka/connect/sink/db/helper/ClickHouseHelperClient$ClickHouseClientBuilder.class; com/clickhouse/kafka/connect/sink/db/helper/ClickHouseHelperClient.class; com/clickhouse/kafka/connect/sink/db/mapping/DeclaredNullability$1.class; com/clickhouse/kafka/connect/sink/db/mapping/DeclaredNullability$Assignment.class; com/clickhouse/kafka/connect/sink/db/mapping/DeclaredNullability.class; com/clickhouse/kafka/connect/util/Utils.class; com/clickhouse/kafka/connect/util/jmx/SinkTaskStatistics.class
compare_owner_split: connector classes 16; manifest 1; anything else 0
compare_version_changes: none
compare_manifest: MANIFEST.MF: differs only in Implementation-Version
compare_matches_projection: yes
gg7_asset_name: clickhouse-kafka-connect-v1.7.0_gg7.zip
gg7_asset_bytes: 13564238
gg7_asset_digest: sha256:3dbdcf03e5825073b7b0535718c9998a5e5754c0ae05a17502bbf4c6d69472af
gg7_asset_uploader: github-actions[bot]
gg7_asset_download_digest_equal: yes
gg7_asset_vs_rebuild_totals: totals files-a 7512 files-b 7512 only-a 0 only-b 0 changed 6 same 7506
gg7_asset_vs_rebuild_unallowed: 6
client_version_default: V1
insert_timeout_default: TimeUnit.MINUTES.toMillis(4), 240000 ms
insert_timeout_min: ConfigDef.Range.atLeast(100)
version_constant_users: ClickHouseHelperClient; SinkTaskStatistics
src_main_shortstat_since_gg7: 6 files changed, 428 insertions(+), 72 deletions(-)
escaped_newline_in_main: 0
note_commit_1: b46157003f4aed427b3ba9dc613765df17708320
d0_mode: attr
d0_start_commit: 7168cf95f4d1ed93990bf09a1516807be5c9cde6
d0_merge_exit: 0
d0_d_commit: 7168cf95f4d1ed93990bf09a1516807be5c9cde6
d0_d_parents: 9483476322e0d331e81c5b92ac266a18f0f01450 b46157003f4aed427b3ba9dc613765df17708320
d0_d_tree_equal: yes
d0_d_subject: Dry-run merge of b46157003f4aed427b3ba9dc613765df17708320 onto v1.7.0_gg7
d0_d_version: v1.7.0_gg8
d0_d_no_gsd_ws_arg: yes
d0_d_allowlist_present: no
d0_d_baseline_tag: v1.7.0_gg7
d0_d_status_before_block: clean
d0_block_extract: ok
d0_block_sha256: 2f30bd7d6b0109b22caaba63ff968a02f3f3eedfc11ec8ec4b46474012f67991
d0_rollback_exit: 0
d0_rollback_output: ok
d0_rollback_commit: 5aa7f8ceacda14644f47b48d2e842bef6f3fb4e1
d0_rollback_status_after: clean
d0_gradlew_bat_trap: no
d0_rollback_version: v1.7.0_gg7
d0_rollback_note_absent: yes
d0_rollback_tree_equals_tag: yes
d0_revert_exit: 0
d0_revert_restores_tree: yes
d0_rehearsal_cleaned: yes
d0_result: rehearsal: PASS
d0_plain_result: rehearsal: PASS
note_commit_1b: 06df048b7ac209ed89fbe005a2e311f4aacf30d6
block_unchanged_since_rehearsal: yes
nc_rows_d: OK rows=3 measured=3 code=2 derived=0
nc_labels: OK labels: 3 of 3 defined
nc_forbidden: OK forbidden: 0 lines
nc_rollback: OK rollback: the block is the four expected lines
nc_claims: OK claims: 0 hex strings outside the allowed set
hygiene_note: clean
secrets_note: clean
worktrees_clean: yes
rollback_branch_absent: yes
```

The compare of the two local rebuilds listed 17 UNALLOWED entries, the derived expectation of the research: the 14 exact paths of the earlier measurement, `Version.class`, `SinkTaskStatistics.class` and `META-INF/MANIFEST.MF`. The measured set equals the projection path for path (`compare_matches_projection: yes`), the totals equal the derived `files-a 7512 files-b 7515 only-a 0 only-b 3 changed 14 same 7498`, the version table lists 18 libraries and changes none, and no entry outside the connector classes and the manifest differs. These are the values the allow-list plan must regenerate.

The `v1.7.0_gg7` release asset has the size and digest the research read, the downloaded copy's SHA-256 equals the API digest, and the compare of the asset with the local rebuild of the tag lists six connector class files as different (`gg7_asset_vs_rebuild_totals`; the asset is 94 bytes larger than the rebuild), so the note tells operators to download the asset rather than rebuild it. Both archive builds ran on the default toolchain with no crash.

The rehearsal ran on a dry-run merge of the first note commit onto `94834763` in both modes; both printed `rehearsal: PASS`, the extracted block hashes to the same value in both and still hashes to it after the second note commit, and `gradlew.bat` did not show in the status after the block. The dry-run commit is unreferenced and its worktree and the rollback branch are gone.

2026-11-06T13:28:52Z
Verdict: pass

### E-06: Note sections A and B: the upstream tail and the timeout limits, numbers traced to FORK-DELTA item 14 and the E-04 snapshots

Backs: RLS-03 (rows U1 to U4 and T1 to T10 with labels, client scope, durations from item 14 and case counts from the E-04 snapshots), D-4, PL-2, PL-3, PL-4.

```sh
python3 -I .planning/phases/49-release-publication/49-note-check.py rows docs/ROLLOUT-v1.7.0_gg8.md --only U T
python3 -I .planning/phases/49-release-publication/49-note-check.py rows docs/ROLLOUT-v1.7.0_gg8.md --only D
python3 -I <scratch>/durations.py docs/ROLLOUT-v1.7.0_gg8.md docs/FORK-DELTA.md   # every duration of a T row occurs in item 14
python3 -I .planning/phases/49-release-publication/49-evidence-check.py junit <scratch>/xml-base-testunit --class .<Class> tests=<n>   # and xml-base-suite for a live class
git diff --quiet v1.7.0_gg7 HEAD -- src/main/java/com/clickhouse/kafka/connect/sink/db/helper/ClickHouseFieldDescriptor.java
env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain test --tests '*SlashColumnRoundTripTest'   # on the scratch export of v1.7.0_gg7 with the test copied in and the connect-json test dependency added
git diff v1.7.0_gg7 HEAD -- gradle build.gradle.kts
python3 -I .planning/phases/49-release-publication/49-note-check.py forbidden|rollback docs/ROLLOUT-v1.7.0_gg8.md
python3 -I .planning/phases/49-release-publication/49-hygiene-check.py --strict docs/ROLLOUT-v1.7.0_gg8.md
```

```text
snapshots_recreated: no
nc_rows_ut: OK rows=14 measured=9 code=8 derived=1
nc_rows_d_still: OK rows=3 measured=3 code=2 derived=0
t_durations_in_item14: yes
t_durations_checked: 28 durations in 10 rows, 24 distinct
case_counts: SlashColumnRoundTripTest=4; FieldDescriptorLiteralFieldsUnitTest=31; WriterRecordContextUnitTest=59; WriterRecordContextDlqUnitTest=4; JacksonOverIoValueFreeUnitTest=109; JsonRouteValueFreeUnitTest=42; JsonRouteValueFreeDlqUnitTest=4; InsertWriteStallBoundPinUnitTest=20; InsertV1WaitBoundPinUnitTest=21; InsertSocketTimeoutOptionUnitTest=23; InsertBoundGateUnitTest=20; InsertTimeoutHelpTextPinUnitTest=27
case_counts_match_snapshots: yes
u1_reader_unchanged_since_gg7: yes
slash_cases_on_gg7_export: OK junit /tmp/gsd49/w/arch-base-src/build/test-results/test class *.SlashColumnRoundTripTest: tests=4 failures=0 errors=0 skipped=0 files=1
catalog_diff_since_gg7: 1 added catalog line (connect-json); 1 added testImplementation line
nc_forbidden: OK forbidden: 0 lines
nc_rollback: OK rollback: the block is the four expected lines
hygiene_note: clean
secrets_note: clean
note_commit_2: 48835d5414a4ac0943084ed405b6c9bc253bc535
```

Every duration of a T row occurs in item 14 of FORK-DELTA as written there, and every case count a row states equals the count in the JUnit snapshots of the base run (E-04); the research's counts differed for two classes (`InsertTimeoutHelpTextPinUnitTest` 27 against the research's 18, `ClickHouseSinkTaskStopUnitTest` 7 against 6), and the measured values are used. Statements of the research that the snapshots cannot back (for example how many cases of `FieldDescriptorLiteralFieldsUnitTest` turn red against upstream's decode-all reader) are not written.

Two findings for the review plan, recorded and not fixed here. First, row U1: the `DESCRIBE` reader is byte-identical to `v1.7.0_gg7`, so upstream's slash fix changed no behaviour of this fork; the four live cases pass on an export of `v1.7.0_gg7` (the test file copied in, the test-scope `connect-json` dependency added). The CHANGELOG bug-fix bullet says a slash name "now round-trips", which reads as a change an operator will see; the note says the reader is unchanged and the tests are new. Second, the CHANGELOG names the two upstream items as #838 and #640, while the FORK-DELTA rows name upstream's pull requests #839 and #857; a read-only look at upstream shows #838 is the issue behind #839 and #857 is a pull request on record context, and the note names the pull requests 839 and 857. FORK-DELTA item 13 describes `ClickHouseSinkTaskStopUnitTest` as six cases, two of them red on the unchanged tree, and the snapshot holds seven; the note states no count for it.

2026-11-06T13:33:37Z
Verdict: pass

### E-07: Note sections C and D, the closes table and the known limits; the full checker, seven mutations, links, resolver and scan

Backs: RLS-03 (the finished note: 22 rows, the gg7 statements it closes, the known limits, the checker shown to fail on seven kinds of defect), D-4, D-9, PL-2, PL-3, PL-4.

```sh
python3 -I .planning/phases/49-release-publication/49-note-check.py all docs/ROLLOUT-v1.7.0_gg8.md --allow <gg7 merge sha> <gg7 asset digest>
python3 -I .planning/phases/49-release-publication/49-id-scan.py <note_commit_3> docs/ROLLOUT-v1.7.0_gg8.md
python3 -I .planning/phases/49-release-publication/49-path-check.py <note_commit_3> docs/ROLLOUT-v1.7.0_gg8.md
python3 -I .planning/phases/49-release-publication/49-doc-check.py links docs/ROLLOUT-v1.7.0_gg8.md CHANGELOG.md docs/FORK-DELTA.md
python3 -I .planning/phases/49-release-publication/49-note-check.py rollback docs/ROLLOUT-v1.7.0_gg8.md --print | shasum -a 256
python3 -I .planning/phases/49-release-publication/49-hygiene-check.py --strict docs/ROLLOUT-v1.7.0_gg8.md
python3 -I .planning/phases/49-release-publication/49-evidence-check.py secrets docs/ROLLOUT-v1.7.0_gg8.md
python3 -I <scratch>/mutate.py   # seven single mutations on scratch copies: S3 row deleted, T4 scope widened, one label replaced, an identifier appended to S4, T14 renamed in the closes table, the read-tree tag changed, a 64-hex string appended to D2
git diff --name-only 816d3b554045c5e1368b5e5090d8674695596cb4 HEAD -- . ':(exclude).planning'
```

```text
nc_rows: OK rows=22 measured=15 code=15 derived=1
nc_labels: OK labels: 3 of 3 defined
nc_forbidden: OK forbidden: 0 lines
nc_closes: OK closes: the section names 7 of 7 gg7 statements
nc_rollback: OK rollback: the block is the four expected lines
nc_claims: OK claims: 0 hex strings outside the allowed set
nc_all_exit: 0
id_scan_note: ids: hits=0
path_check_note: paths: candidates=3 resolved=3 abbreviated=0 abbreviated_unique=0 placeholders=0 by_design=0 unresolved=0
links_all: OK links: 35 relative targets checked, 0 broken
block_unchanged_since_rehearsal: yes
rehearse_md5_unchanged: yes
hygiene_note: clean
secrets_note: clean
section_order: ## Before you upgrade; ## A. The upstream tail; ## B. Insert timeout limits; ## C. Task stop and the retry outcome; ## D. A removed public constant; ## E. Release identity, archive and roll; ## What this release closes from the gg7; ## Known limits carried into this releas
mut_row_deleted: exit=1 FAIL rows: S3 occurs 0 times
mut_client_scope: exit=1 FAIL rows: line 63: T4 has an Applies-when cell without 'client V1' or 'client V2'
mut_label: exit=1 FAIL rows: line 49: U3 has the label 'assumed', which is not one of the three
mut_identifier: exit=1 FORBIDDEN line 87
mut_closes: exit=1 FAIL closes: the section does not name T14
mut_rollback: exit=1 FAIL rollback: the rollback block is ['git switch -c rollback-v1.7.0_gg8', 'git read-tree -u --reset pre-v1.7.0_gg7', 'git commit -m "Roll back v1.7.0_gg8"', 'git diff --quiet HEAD pre-v1.7.0_gg8 && echo ok'], not the four expected lines
mut_hex: exit=1 CLAIM line 104: 0000000000000000000000000000000000000000000000000000000000000000
note_commit_3: 1ee7017b83b7b589e63d801a717837b8e71ea9d3
non_planning_paths_since_l0: CHANGELOG.md VERSION docs/FORK-DELTA.md docs/ROLLOUT-v1.7.0_gg8.md
```

The finished note has 22 effect rows (15 carry the label measured, 15 read from the code and 1 derived, counted by label form, so a mixed row counts for each form it uses), the sections in the order of the plan, and the closes table names the seven gg7 statements the checker requires plus the gg7 statements the plan lists; the table also holds two rows the plan did not list, row R6 of the gg7 note (its decision is now taken) and the gg7 CHANGELOG Bug Fixes sentence on `stop()`, which the gg8 CHANGELOG already corrects. Each of the seven mutations turned the checker red on its own subcommand.

Rows whose source was weaker than the research's label or wording: U1 (the reader is unchanged since the previous release, so the row states a pinned behaviour and no change; measured on an export of the previous tag), U2 (the writer's exception is rethrown with a Jackson link rebuilt value-free, not rethrown unchanged, as the CHANGELOG says), S1 and S2 carry no case count (the snapshot holds seven cases of `ClickHouseSinkTaskStopUnitTest` where FORK-DELTA says six), S3 labels the Connect worker measurement for the Kafka 3.9 line only, and P1 names the fixture class in the test tree, not a test-fixtures source set. The rollback list adds row P1, which the plan's list left out and which the jar does reverse.

2026-11-06T13:36:25Z
Verdict: pass

### E-08: Review round 1 of the release documents by a fresh gsd-code-reviewer

Backs: RLS-03 (an independent fresh-context review of the release documents before the allow-list, the gates and any approval), D-6, D-7.

```sh
python3 -I .planning/phases/49-release-publication/49-review-check.py round .planning/phases/49-release-publication/49-REVIEW.md --diff-base 9483476322e0d331e81c5b92ac266a18f0f01450 --files docs/ROLLOUT-v1.7.0_gg8.md CHANGELOG.md docs/FORK-DELTA.md docs/ROLLOUT-v1.7.0_gg6.md docs/ROLLOUT-v1.7.0_gg1.md --tokens '22 rows' 'pre-v1.7.0_gg8' '94834763' 'client V1' 'client V2' 'socket_timeout' 'exactlyOnce=false' 'release object' 'FORK-DELTA' 'CHANGELOG' 'Unreleased' 'Record serialization failed' 'ESCAPED_NEWLINE' 'item 24'
python3 -I .planning/phases/49-release-publication/49-review-check.py agents .planning/phases/49-release-publication/49-REVIEW.md --expect-ids aa6175e0dcb6fb58e
```

```text
reviewer_agent: round 1 agentId=aa6175e0dcb6fb58e (gsd-code-reviewer spawned by the orchestrator with the Agent tool, fresh context)
agents_check: OK agents: rounds=1 distinct=1 ids=aa6175e0dcb6fb58e
brief_files: docs/ROLLOUT-v1.7.0_gg8.md; CHANGELOG.md; docs/FORK-DELTA.md; docs/ROLLOUT-v1.7.0_gg6.md; docs/ROLLOUT-v1.7.0_gg1.md
brief_path: /tmp/gsd49/review/brief-r1.md
round1_findings: 15
review_commit: bfdaa242329b32d673c9c6bd770caef1d1e26da8
schema_check: OK round: findings=15 rounds=1
own_checks_before: ok
```

Verdict: pass

Notes. Round 1 found 15 findings (0 critical, 9 warnings, 6 info). The verdict records that the process ran, not that the documents are clean. Nothing was pushed.

### E-09: Review rounds and fixes until the user accepted the last round

Backs: RLS-03 (every finding fixed, each fix reviewed by a later fresh round except the last two), D-6, G1.

```sh
python3 -I .planning/phases/49-release-publication/49-review-check.py rounds .planning/phases/49-release-publication/49-REVIEW.md .planning/phases/49-release-publication/49-REVIEW-DISPOSITION.md --max-rounds 5
python3 -I .planning/phases/49-release-publication/49-review-check.py agents .planning/phases/49-release-publication/49-REVIEW.md --expect-ids aa6175e0dcb6fb58e,a09cc0035374691f7,a33561b94d0bde104,a32c2a44ee49eac0e,a2456cb7a52ff4907
```

```text
rounds: 5
max_rounds: 5
reviewer_agent: round 2 agentId=a09cc0035374691f7; round 3 agentId=a33561b94d0bde104; round 4 agentId=a32c2a44ee49eac0e; round 5 agentId=a2456cb7a52ff4907
agents_check: OK agents: rounds=5 distinct=5 ids=aa6175e0dcb6fb58e,a09cc0035374691f7,a33561b94d0bde104,a32c2a44ee49eac0e,a2456cb7a52ff4907
round_findings: 1:15; 2:6; 3:5; 4:4; 5:2
fix_commits: 4d3d8c32; 07a83cc7; 1a2623ba; 63f6293e; 36c90fa7; bee9ce65; a009d050; d9d311aa; 9c718a48; 5eebb803; 003eb385; 868be463; 492937c4; 61a7e563; a5cb80cb; 887846b3; f5c2278a; f1c22f72; 65a8278a; dfd18498; f97dff40; 4676de01; 95a0d209; ff52fe11; d85c6de1; 9b713883; 0cd63ea9; f39e870d; 24ed029d; 2c6db32d; 5e61ed65; 74b97062; 315f5de1
fix_reproof_method: every proposed fix text was checked by the fixing executor or the orchestrator against ClickHouseWriter, ClickHouseSinkConfig, git and the gg7 tag before it was applied; the executors corrected IN-05, IN-06, WR-10, WR-11, WR-12, WR-13, WR-16, WR-18, WR-19, IN-09 and IN-10; no fix touched the rollback command block (hash unchanged, see E-10)
last_round_clean: no
round3_decision: Authorise round 4 (user, AskUserQuestion, 2026-11-06); Authorise round 5 (user, 2026-11-06); round 5 left with two findings fixed unreviewed: Accept as recorded (user, 2026-11-06); authorised_rounds: 5
own_checks_after_last_fix: ok
rounds_check: OK rounds: [(1, 15), (2, 6), (3, 5), (4, 4), (5, 2)] last_clean=False decision=True max_rounds=5
```

Verdict: pass

Notes. Rounds 1 to 5 found 15, 6, 5, 4 and 2 findings. The user was asked after rounds 3, 4 and 5 and answered directly to the orchestrator; the last answer accepted the two round-5 fixes (WR-20, WR-21, one commit) without a sixth round, so those two fixes are the only unreviewed change. The owning checks after the last fix: note checker `rows=22`, id scans `hits=0`, links 35 checked 0 broken, strict hygiene clean on the note and CHANGELOG, path resolver leaves only the gg8 allow-list unresolved in FORK-DELTA, FORK-DELTA numbers check OK against fresh shortstats (364 47983 1260; 43 3578 333), totals sentence holds 2325 and 2928, CHANGELOG heading `Unreleased` and its released sections equal to `v1.7.0_gg7`'s. Nothing was pushed.
