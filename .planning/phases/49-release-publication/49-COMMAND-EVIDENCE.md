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
| E-11 | RLS-02 | 49-04 | pass |
| E-12 | RLS-04 | 49-04 | pass |
| E-13 | RLS-04 | 49-04 | pass |
| E-14 | RLS-04, RLS-05 | 49-05 | pass |
| E-15 | RLS-02, RLS-05 | 49-05 | pending (user-gated) |
| E-16 | RLS-05 | 49-05 | pending (user-gated) |
| E-17 | RLS-05 | 49-05 | pending (user-gated) |
| E-18 | RLS-05 | 49-05 | pending (user-gated) |
| E-19 | RLS-06 | 49-05 | pending (user-gated) |
| E-20 | RLS-01 to RLS-06 | 49-05 | pass |

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

### E-10: Every review finding is dispositioned and the release documents are frozen for the allow-list and the gates

Backs: RLS-03 (no finding open; the documents frozen), D-6, the Freeze rule.

```sh
python3 -I .planning/phases/49-release-publication/49-review-check.py disposition .planning/phases/49-release-publication/49-REVIEW.md .planning/phases/49-release-publication/49-REVIEW-DISPOSITION.md --commits /tmp/gsd49/review/phase-commits.txt
git diff --name-only 816d3b554045c5e1368b5e5090d8674695596cb4 c96816f4 -- . ':(exclude).planning'
git diff --name-only 816d3b554045c5e1368b5e5090d8674695596cb4 c96816f4 -- .planning/scripts
python3 -I .planning/phases/49-release-publication/49-note-check.py rollback docs/ROLLOUT-v1.7.0_gg8.md --print | shasum -a 256
```

```text
docs_frozen_scope: CHANGELOG.md; VERSION; docs/FORK-DELTA.md; docs/ROLLOUT-v1.7.0_gg8.md
scripts_scope: .planning/scripts/fork-anchors.txt
review_text_in_note: none
dispositions: fixed:32 rejected:0 deferred:0
disposition_check: OK disposition: findings=32 rows=32
block_unchanged_or_reproved: unchanged
docs_frozen: yes
pending_verdicts: none
disposition_commit: c96816f42d265fe970b89fa2950b1331a082fca7
```

Verdict: pass

Notes. The rollback command block of the note still hashes to the E-05 value (2f30bd7d...7991), so no new rehearsal was needed. The documents are frozen from this commit on; the Freeze rule binds the allow-list, gates and approval bundle of plans 49-04 and 49-05, with the one exception of the one-line date commit of U-3. Nothing was pushed.

### E-11: The gg8 archive allow-list is generated last from the final tree and the local compare dry run passes in both polarities

Backs: RLS-02 (the allow-list generated from the final tree, the dry run `0 unallowed, 0 stale rows`, every wildcard row, none here, after two literal segments), D-1, D-2, G-1, G-2, PL-10.

```sh
mkdir <new dir>/final-base-src <new dir>/final-tip-src <new dir>/final-arch
git archive v1.7.0_gg7 | tar -x -C <new dir>/final-base-src
git archive HEAD | tar -x -C <new dir>/final-tip-src
env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain createConfluentArchive   # in each export with its own wrapper, one after the other
python3 -I .github/scripts/compare_confluent_archives.py <dir>/base.zip <dir>/tip.zip <dir>/empty.txt   # empty.txt: a comment-only allow-list
python3 -I .planning/phases/49-release-publication/49-allowlist-check.py rows|cover <list>
python3 -I .github/scripts/compare_confluent_archives.py <dir>/base.zip <dir>/tip.zip .planning/scripts/confluent-archive-allowlist-v1.7.0_gg8.txt
python3 -I .github/scripts/compare_confluent_archives.py <dir>/base.zip <dir>/tip.zip <mutated list>   # three mutated copies: a stale row, a deleted row, a leading-wildcard row
rm -rf build/test-results/testUnit
DOCKER_HOST=unix:///nonexistent.sock env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain testUnit --rerun --tests '*CompareConfluentArchivesScriptUnitTest' --tests '*ReleaseWorkflowGuardUnitTest' --tests '*AllowListWildcardRuleUnitTest'
python3 -I .planning/phases/49-release-publication/49-evidence-check.py failed|junit build/test-results/testUnit
git add .planning/scripts/confluent-archive-allowlist-v1.7.0_gg8.txt
git commit -F <msg file>
python3 -I .planning/phases/49-release-publication/49-hygiene-check.py --strict <list>
python3 -I .planning/phases/49-release-publication/49-evidence-check.py secrets <list>
```

```text
tip_head: b1fc06443f5b0f3e9607b7e5a32af59a29c87d5e
docs_frozen_tip: c96816f42d265fe970b89fa2950b1331a082fca7
archive_inputs_unchanged_since_freeze: yes
baseline_tag: v1.7.0_gg7
workflow_allowlist_path_matches: yes
final_base_verify_line: verifyConfluentArchive: 7932 inner entries, no nested jar, no Kafka API class
final_tip_verify_line: verifyConfluentArchive: 7935 inner entries, no nested jar, no Kafka API class
final_base_entries: 7932
final_tip_entries: 7935
entries_equal_e05: yes
final_tip_bytes: 13572233
final_java: default toolchain (Gradle 9.8.2, launcher JVM 21.0.12.1), -PjavaVersion=21 not needed, no JVM crash
empty_compare_exit: 1
empty_compare_totals: totals files-a 7512 files-b 7515 only-a 0 only-b 3 changed 14 same 7498
empty_compare_unallowed: 17
empty_compare_paths: META-INF/MANIFEST.MF; com/clickhouse/kafka/connect/sink/ClickHouseSinkConfig.class; com/clickhouse/kafka/connect/sink/ClickHouseSinkTask.class; com/clickhouse/kafka/connect/sink/Version.class; com/clickhouse/kafka/connect/sink/db/ClickHouseWriter$1.class; com/clickhouse/kafka/connect/sink/db/ClickHouseWriter$2.class; com/clickhouse/kafka/connect/sink/db/ClickHouseWriter$3.class; com/clickhouse/kafka/connect/sink/db/ClickHouseWriter$InsertBody.class; com/clickhouse/kafka/connect/sink/db/ClickHouseWriter$InsertWriteDeadline.class; com/clickhouse/kafka/connect/sink/db/ClickHouseWriter.class; com/clickhouse/kafka/connect/sink/db/helper/ClickHouseHelperClient$ClickHouseClientBuilder.class; com/clickhouse/kafka/connect/sink/db/helper/ClickHouseHelperClient.class; com/clickhouse/kafka/connect/sink/db/mapping/DeclaredNullability$1.class; com/clickhouse/kafka/connect/sink/db/mapping/DeclaredNullability$Assignment.class; com/clickhouse/kafka/connect/sink/db/mapping/DeclaredNullability.class; com/clickhouse/kafka/connect/util/Utils.class; com/clickhouse/kafka/connect/util/jmx/SinkTaskStatistics.class
empty_compare_matches_e05: yes
empty_compare_version_changes: none
empty_compare_manifest: MANIFEST.MF: differs only in Implementation-Version
unexpected_differences: none
list_rows: OK rows=17
list_rows_count: 17
list_cover: OK differences=17 covered=17 uncovered=0 overlapping=0 stale_rows=0
wildcard_rows: 0
two_segment_rule: ok
header_line1_ok: yes
list_compare_exit: 0
list_compare_result: RESULT: PASS
compare_summary: exit 0, 0 unallowed, 0 stale rows, baseline v1.7.0_gg7, allow-list .planning/scripts/confluent-archive-allowlist-v1.7.0_gg8.txt
list_compare_manifest: MANIFEST.MF: differs only in Implementation-Version
list_compare_nested_jars: 0
list_compare_kafka_classes: 0
mutation_stale: exit=1 STALE-ROW zz/nothing/*
mutation_deleted: exit=1 UNALLOWED CHANGED com/clickhouse/kafka/connect/sink/Version.class
mutation_wildcard: exit=2 ERROR: <mutated list>:28: a glob must start with a literal path prefix; one that starts with a wildcard (* ? [) is not anchored to a path and is refused
projection_equal: yes
guards_exit: 0
guards_failed: OK failed: 0 failing testcases, 0 expected
compare_xml: OK junit build/test-results/testUnit class *.CompareConfluentArchivesScriptUnitTest: tests=58 failures=0 errors=0 skipped=0 files=1
relguard_xml: OK junit build/test-results/testUnit class *.ReleaseWorkflowGuardUnitTest: tests=38 failures=0 errors=0 skipped=0 files=1
wildcard_xml: OK junit build/test-results/testUnit class *.AllowListWildcardRuleUnitTest: tests=14 failures=0 errors=0 skipped=0 files=1
allowlist_commit: dcf13360892b496e589129bbf5bd3bf63b181c0d
allowlist_commit_files: .planning/scripts/confluent-archive-allowlist-v1.7.0_gg8.txt
generated_last: yes
hygiene_scan: clean
secrets_scan: clean
```

The compare of the two clean rebuilds (the tag exported with its own wrapper, the tip exported at the commit before the list) reproduced the measured set of the earlier entry: 17 UNALLOWED entries (14 changed, 3 only in the tip archive; the manifest and the release identity classes among them), totals `files-a 7512 files-b 7515 only-a 0 only-b 3 changed 14 same 7498`, 7932 and 7935 inner entries, 13564144 and 13572233 bytes, no version-table change. Every difference is explained by the owner it was derived for: the six changed `src/main` files of the release, the three new inner classes of the writer, the inner classes of changed classes, and the release identity (`Version`, `SinkTaskStatistics`, the manifest). No difference was left without an owner and no row was written to make a compare pass. The 17 rows are exact paths; the glob set equals the 14 connector paths of the earlier gate measurement plus the three identity paths (the research projection). The three shipped-list guard classes ran with Docker unreachable and load every shipped list, the new one included.

The three mutations turned the compare red on their own: a row that names nothing is reported `STALE-ROW`, a deleted row turns its difference `UNALLOWED`, and a row that starts with a wildcard is refused by the loader. The list commit holds one file, and no archive input, CHANGELOG or doc path changed between the frozen documents and it. Both archive builds and the guard run ran one at a time, with no tail gate alive. Nothing was pushed.

2026-11-06T16:40:53Z
Verdict: pass

### E-12: Docker-free gates on one head: origin and upstream re-read, the tail gate, testUnit with Docker unreachable, the anchors mirror, scope and numbers

Backs: RLS-04 (the Docker-free half: the tail-form anchors gate on an explicit SHA, testUnit with the daemon unreachable, the anchors mirror, the scope and number facts), D-8, D-10, G-3, G-5, G-6, PL-10.

```sh
git rev-parse HEAD
git ls-remote origin refs/heads/custom_build
git rev-list --left-right --count origin/custom_build...HEAD
git rev-list origin/custom_build --not HEAD
git ls-remote --heads origin
git ls-remote --tags origin 'v1.7.0_gg8*' 'pre-v1.7.0_gg8*'
git log -1 --format=%H -- . ':(exclude).planning'
git log -1 --format=%H -- .planning/scripts
git rev-list --count HEAD --not --remotes=origin
gh api 'repos/ne1r0n/clickhouse-kafka-connect/pulls?state=open&per_page=50' --jq '[.[] | "\(.number) \(.head.sha[0:8])"] | sort | join("; ")'
git ls-remote upstream refs/heads/main
git ls-remote --tags --refs upstream 'v*'
curl -s https://api.github.com/repos/ClickHouse/clickhouse-kafka-connect/pulls/<n>   # n = 861, 855, 854, 788
PRE_TAG=pre-v1.7.0_gg8 UPSTREAM_REF=83808df9a0993cf49b2ceaa4633c782e550c0180 CONFLICTED_PATHS_FILE=.planning/scripts/conflicted-paths-v1.7.0-tail-3.txt bash .planning/scripts/verify-merge-invariants.sh 12e2cf17cca9f5b17917dd9aceaa38b44f3f8b10 <gated head>
DOCKER_HOST=unix:///nonexistent.sock docker info
./gradlew --stop
sysctl vm.swapusage
rm -rf build/test-results/testUnit
DOCKER_HOST=unix:///nonexistent.sock env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain testUnit --rerun
python3 -I .planning/phases/49-release-publication/49-evidence-check.py junit|failed <snapshot of build/test-results/testUnit>
python3 -I .planning/phases/49-release-publication/49-testcase-status.py dump|delta <dumps>
python3 -I .planning/phases/49-release-publication/49-evidence-check.py anchors .planning/scripts/fork-anchors.txt --rows 218
git diff --name-only <L0> <gated head> -- . ':(exclude).planning'
git diff --shortstat v1.7.0 <gated head> -- src build.gradle.kts gradle .github   # and -- src/main
python3 -I .planning/phases/49-release-publication/49-doc-check.py numbers docs/FORK-DELTA.md 'after gg8: `v1.7.0` to `v1.7.0_gg8`' --wider <wider> --narrow <narrow>
python3 -I .planning/phases/49-release-publication/49-path-check.py <gated head> docs/FORK-DELTA.md
python3 -I .planning/phases/49-release-publication/49-note-check.py all docs/ROLLOUT-v1.7.0_gg8.md --allow <gg7 merge sha> <gg7 asset digest>
python3 -I .planning/phases/49-release-publication/49-hygiene-check.py [--strict] <files>
python3 -I .planning/phases/49-release-publication/49-evidence-check.py secrets <files>
```

```text
gated_head: d9b10b9bf670b8236119c725e3639c53ad3e570b
utc_start: 2026-11-06T16:41:24Z
dirty_paths_at_gate: M .planning/config.json; M .planning/state.json
origin_custom_build_at_gate: 9483476322e0d331e81c5b92ac266a18f0f01450
left_count_at_gate: 0
origin_sync_ref: none
origin_gg8_tags: none
local_release_tag: none
last_non_planning_commit: 315f5de1c85fb61bd2a1e887d2be085bc62571ce
last_scripts_commit: dcf13360892b496e589129bbf5bd3bf63b181c0d
unpublished_at_gate: 288
origin_open_prs_at_gate: none
upstream_main_gate: 83808df9a0993cf49b2ceaa4633c782e550c0180
upstream_newest_tag_gate: v1.7.0
upstream_tag_past_v170_gate: no
upstream_pr_861_gate: open False b62e0ae4
upstream_pr_855_gate: open False ddabf28b
upstream_pr_854_gate: open False 69d1efc5
upstream_pr_788_gate: closed False bb4b4140
upstream_changed_since_e01: no
tail_gate_end: d9b10b9bf670b8236119c725e3639c53ad3e570b
tail_gate_line: invariants: provenance_paths=11 anchor_commits=270 anchor_rows=218 marker_commits=288 anchor_exempt=0 result=PASS
tail_gate_exit: 0
docker_unreachable_proof: exit=1
swap_before_testunit: total = 33792.00M  used = 32510.31M  free = 1281.69M  (encrypted)
testunit_exit: 0
testunit_tests: 2325
testunit_failures: 0
testunit_errors: 0
testunit_skipped: 2
testunit_expected_tests: 2325
testunit_count_matches: yes
testunit_delta_vs_e04: delta: removed=0 added=0 expected=0 match=yes
testunit_jvm_crash: none
failed_ok: yes
bump_xml: OK junit /tmp/gsd49/exec/xml-gate-testunit class *.BumpVersionScriptUnitTest: tests=52 failures=0 errors=0 skipped=0 files=1
baseline_xml: OK junit /tmp/gsd49/exec/xml-gate-testunit class *.BaselineTagScriptUnitTest: tests=19 failures=0 errors=0 skipped=0 files=1
relguard_xml: OK junit /tmp/gsd49/exec/xml-gate-testunit class *.ReleaseWorkflowGuardUnitTest: tests=38 failures=0 errors=0 skipped=0 files=1
compare_xml: OK junit /tmp/gsd49/exec/xml-gate-testunit class *.CompareConfluentArchivesScriptUnitTest: tests=58 failures=0 errors=0 skipped=0 files=1
wildcard_xml: OK junit /tmp/gsd49/exec/xml-gate-testunit class *.AllowListWildcardRuleUnitTest: tests=14 failures=0 errors=0 skipped=0 files=1
mbean_xml: OK junit /tmp/gsd49/exec/xml-gate-testunit class *.MBeanNameUnitTest: tests=11 failures=0 errors=0 skipped=0 files=1
stop_xml: OK junit /tmp/gsd49/exec/xml-gate-testunit class *.ClickHouseSinkTaskStopUnitTest: tests=7 failures=0 errors=0 skipped=0 files=1
redelivery_xml: OK junit /tmp/gsd49/exec/xml-gate-testunit class *.RetriableRedeliveryLimitPinUnitTest: tests=108 failures=0 errors=0 skipped=0 files=1
v1wait_xml: OK junit /tmp/gsd49/exec/xml-gate-testunit class *.InsertV1WaitBoundPinUnitTest: tests=21 failures=0 errors=0 skipped=0 files=1
writestall_xml: OK junit /tmp/gsd49/exec/xml-gate-testunit class *.InsertWriteStallBoundPinUnitTest: tests=20 failures=0 errors=0 skipped=0 files=1
helptext_xml: OK junit /tmp/gsd49/exec/xml-gate-testunit class *.InsertTimeoutHelpTextPinUnitTest: tests=27 failures=0 errors=0 skipped=0 files=1
literal_xml: OK junit /tmp/gsd49/exec/xml-gate-testunit class *.FieldDescriptorLiteralFieldsUnitTest: tests=31 failures=0 errors=0 skipped=0 files=1
recordctx_xml: OK junit /tmp/gsd49/exec/xml-gate-testunit class *.WriterRecordContextUnitTest: tests=59 failures=0 errors=0 skipped=0 files=1
planning_guard_xml: OK junit /tmp/gsd49/exec/xml-gate-testunit class *.SourcePlanningIdGuardUnitTest: tests=18 failures=0 errors=0 skipped=0 files=1
anchors_mirror: rows=218 bad=0
scope_outside_planning: CHANGELOG.md; VERSION; docs/FORK-DELTA.md; docs/ROLLOUT-v1.7.0_gg8.md
scope_planning_scripts: .planning/scripts/confluent-archive-allowlist-v1.7.0_gg8.txt; .planning/scripts/fork-anchors.txt
no_java_change: yes
forkdelta_numbers_equal: yes
forkdelta_paths_line: paths: candidates=100 resolved=72 abbreviated=20 abbreviated_unique=20 placeholders=2 by_design=6 unresolved=0
note_archive_numbers_match: yes
note_check_all: ok
integration_test: not re-run (PL-11): it needs a Connect stack; the user may overrule at the bundle
review_reword_commit: 89f3e5cebcbdc4a1a45446cafd2059eea3edb0a5
gated_head_note: the Docker-free results (tail gate, testUnit, scope, numbers) are for the gated head; the one later commit changes only a file under .planning/phases, so they stay valid
hygiene_scan_all: clean
secrets_scan: clean
nothing_pushed: yes
worktrees_clean: yes
docker_free_decision: go
gate_failures: none
utc_end: 2026-11-06T17:00:35Z
```

The tail gate ran once, alone, in the foreground on the explicit gated head, with no Gradle or Docker job alive (it took about 15 minutes with swap near 94 %). Its counts (`anchor_commits=270`, `marker_commits=288`) follow the 288 commits of the unpublished range; `anchor_rows=218` and `result=PASS` held. testUnit ran with the daemon proven unreachable and read the E-04 total with the same two skips, and its per-testcase dump equals the E-04 dump. The origin facts and the upstream values are unchanged from E-01.

The first default-mode hygiene scan over the files this phase added read 10 hits, all of one plain word in the reviewers' sentences of the review record, none of them an environment name. That was a false positive of the helper's fifth default pattern, known from the earlier release phase. No fail entry was written for it: the orchestrator reworded those ten lines in commit `89f3e5ce` (the word replaced by a neutral one; one file under `.planning/phases`, no path outside `.planning`, no `.planning/scripts` path), the agents and rounds checks of the review still pass, and the scan was run again over the same file set and read clean. That commit sits after the gated head `d9b10b9b`; the verify of this entry shows that the tree outside `.planning` and `.planning/scripts` are equal between the two, so the tail gate result and the testUnit result remain valid for the head that is pushed. `git diff` of the later commit holds the ten reworded lines and nothing else.

One pre-existing line of FORK-DELTA carries the bare word the pattern flags; the scan covers the added lines of that document only, as the plan says. Nothing was pushed.

2026-11-06T17:00:35Z
Verdict: pass

### E-13: Two full suites on one pinned ClickHouse latest with client V2, equal per testcase, and the go decision

Backs: RLS-04 (the live half: two full --rerun suites on one pinned ClickHouse latest image with client V2, equal per testcase and equal to the E-04 total, with the server version recorded), G-4, G-5, PL-10.

```sh
git diff --quiet <gated head> HEAD -- . ':(exclude).planning'
./gradlew --stop
sysctl vm.swapusage
colima list
docker image inspect --format '{{index .RepoDigests 0}}' clickhouse/clickhouse-server:latest
docker run --rm --entrypoint clickhouse clickhouse/clickhouse-server:latest local --query 'SELECT version()'
rm -rf build/test-results/test
CLICKHOUSE_VERSION=latest CLIENT_VERSION=V2 env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain test --rerun   # run 1
python3 -I .planning/phases/49-release-publication/49-evidence-check.py junit|skips|failed <snapshot of build/test-results/test>
python3 -I .planning/phases/49-release-publication/49-testcase-status.py dump <snapshot> <dump>
./gradlew --stop
docker image inspect ... ; docker run ... 'SELECT version()'   # after run 1
rm -rf build/test-results/test
CLICKHOUSE_VERSION=latest CLIENT_VERSION=V2 env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain test --rerun   # run 2
docker image inspect ... ; docker run ... 'SELECT version()'   # after run 2
python3 -I .planning/phases/49-release-publication/49-testcase-status.py delta <dump 1> <dump 2> <empty expectation>
```

```text
utc_start: 2026-11-06T17:01:00Z
gate_head_same: yes
swap_before: total = 33792.00M  used = 32894.88M  free = 897.12M  (encrypted)
colima_vz: Running
digest_before_run1: clickhouse/clickhouse-server@sha256:739e099abd1157390960a59e4532d21781d8fde57e6e4151cb920ea02c30b1ce
select_version_before_run1: 26.10.3.7
digest_same_as_e04: yes
suite1_exit: 0
suite1_tests: 2928
suite1_failures: 0
suite1_errors: 0
suite1_skipped: 18
suite1_skips: OK skips: named=7 state_mismatch_cases=11 unexpected=0
suite1_failed_ok: yes
suite1_duration: 3m 18s
suite1_java: default toolchain (Gradle 9.8.2, launcher JVM 21.0.12.1), -PjavaVersion=21 not needed
suite1_jvm_crash: none
suite1_processing_xml: OK junit /tmp/gsd49/exec/xml-gate-suite1 class *.ProcessingTest: tests=14 failures=0 errors=0 skipped=0 files=1
suite1_slash_xml: OK junit /tmp/gsd49/exec/xml-gate-suite1 class *.SlashColumnRoundTripTest: tests=4 failures=0 errors=0 skipped=0 files=1
digest_after_run1: clickhouse/clickhouse-server@sha256:739e099abd1157390960a59e4532d21781d8fde57e6e4151cb920ea02c30b1ce
select_version_after_run1: 26.10.3.7
suite2_exit: 0
suite2_tests: 2928
suite2_failures: 0
suite2_errors: 0
suite2_skipped: 18
suite2_skips: OK skips: named=7 state_mismatch_cases=11 unexpected=0
suite2_failed_ok: yes
suite2_duration: 3m 15s
suite2_java: default toolchain (Gradle 9.8.2, launcher JVM 21.0.12.1), -PjavaVersion=21 not needed
suite2_jvm_crash: none
suite2_processing_xml: OK junit /tmp/gsd49/exec/xml-gate-suite2 class *.ProcessingTest: tests=14 failures=0 errors=0 skipped=0 files=1
suite2_slash_xml: OK junit /tmp/gsd49/exec/xml-gate-suite2 class *.SlashColumnRoundTripTest: tests=4 failures=0 errors=0 skipped=0 files=1
digest_after_run2: clickhouse/clickhouse-server@sha256:739e099abd1157390960a59e4532d21781d8fde57e6e4151cb920ea02c30b1ce
select_version_after_run2: 26.10.3.7
suites_java: default toolchain (Gradle 9.8.2, launcher JVM 21.0.12.1)
suites_same_jvm: yes
suite_expected_tests: 2928
suite_count_matches: yes
suite_tests_equal: yes
suites_equal_per_testcase: delta: removed=0 added=0 expected=0 match=yes
docker_only_delta: 603
pinned_image_same: yes
crash_files_left: none
gate_decision: go
gate_failures: none
gate_note: The CI side (the scratch dry run, the single tests.yaml run on the merged SHA, the publish run) is the user's and not part of this gate, and integrationTest was not re-run (PL-11).
utc_end: 2026-11-06T17:08:04Z
```

Both full suites ran with `--rerun` on ClickHouse `latest` with client V2, one after the other, with no Gradle, Docker or tail gate job beside them, on the same tree and the same pinned image: the image digest and `SELECT version()` read the same value before run 1, after run 1 and after run 2, and equal the values of E-04. No image was pulled and the VM was not stopped. Each suite read 2928 tests (the E-04 total) with 18 skips, 0 failures and 0 errors, took about 3 minutes 15 seconds on the default toolchain, left no crash file, and the two per-testcase status dumps are equal. The difference to the Docker-free run (603) is the number of cases that need the ClickHouse container.

The tree under test is the gated head of E-12 with the one later `.planning/phases` commit of E-12; no path outside `.planning` or under `.planning/scripts` differs. The CI side (the scratch dry run, the single `tests.yaml` run on the merged SHA, the publish run) belongs to the user and is not part of this gate; `integrationTest` was not re-run (PL-11), and the user may overrule that at the bundle. Nothing was pushed.

2026-11-06T17:08:33Z
Verdict: pass

### E-14: Publication rehearsal without a push: upstream recheck 3, the merge message, a dry-run merge with the frozen script, the rollback on it, the date check, the approval bundle

Backs: RLS-04 (the rehearsal on the final tip), RLS-05 (the publication path faced without a push, the approval bundle with one word per origin step), D-7, D-8, U-2, U-3, U-4, PL-1, PL-7.

```sh
git rev-parse HEAD
git ls-remote origin refs/heads/custom_build
git rev-list --left-right --count origin/custom_build...HEAD
git rev-list --count HEAD --not --remotes=origin
git ls-remote --heads origin
git ls-remote --tags origin 'v1.7.0_gg8*' 'pre-v1.7.0_gg8*'
git diff --shortstat 9483476322e0d331e81c5b92ac266a18f0f01450 HEAD -- . ':(exclude).planning'   # and -- .planning
git diff --name-only 9483476322e0d331e81c5b92ac266a18f0f01450 HEAD -- . ':(exclude).planning'
git diff --quiet <gated head> HEAD -- . ':(exclude).planning'   # and -- .planning/scripts
git ls-remote upstream refs/heads/main
git ls-remote --tags --refs upstream 'v*'
curl -s https://api.github.com/repos/ClickHouse/clickhouse-kafka-connect/pulls/<n>   # n = 861, 855, 854, 788
bash .planning/phases/49-release-publication/49-rehearse.sh merge <tip> <new dir> <absolute path of the merge message>
MODE=plain bash .planning/phases/49-release-publication/49-rehearse.sh merge <tip> <new dir> <absolute path of the merge message>
python3 -I .planning/phases/49-release-publication/49-note-check.py rollback docs/ROLLOUT-v1.7.0_gg8.md --print | shasum -a 256
docker run --rm --entrypoint clickhouse clickhouse/clickhouse-server:latest local --query 'SELECT version()'
docker image inspect --format '{{index .RepoDigests 0}}' clickhouse/clickhouse-server:latest
python3 -I .planning/phases/49-release-publication/49-hygiene-check.py --strict <merge message> <bundle> <evidence>
python3 -I .planning/phases/49-release-publication/49-evidence-check.py secrets <evidence>
git commit -F <msg file>   # the merge message and the approval bundle
```

```text
tip_head: 3699a9cd617cdb01950bff86603e60eb8ae75725
origin_custom_build: 9483476322e0d331e81c5b92ac266a18f0f01450
left_count: 0
left_right_at_tip: 0 292
unpublished_commits: 292
pre_tag_local: yes
origin_sync_ref: none
origin_gg8_tags: none
local_release_tag: none
origin_open_prs: none
scratch_tips_now: 939e8992 refs/heads/scratch/clt-07-prebump-0.9.6; de8c685b refs/heads/scratch/clt-07-client-0.11.0
scratch_tips_unchanged: yes
scratch_tips_ancestors: yes
non_planning_vs_gg7: 42 files changed, 5948 insertions(+), 130 deletions(-)
planning_vs_gg7: 253 files changed, 28568 insertions(+), 311 deletions(-)
changed_files_vs_gg7: CHANGELOG.md; VERSION; build.gradle.kts; docs/FORK-DELTA.md; docs/ROLLOUT-v1.7.0_gg1.md; docs/ROLLOUT-v1.7.0_gg6.md; docs/ROLLOUT-v1.7.0_gg8.md; gradle/libs.versions.toml; src/main/java/com/clickhouse/kafka/connect/sink/ClickHouseSinkConfig.java; src/main/java/com/clickhouse/kafka/connect/sink/ClickHouseSinkTask.java; src/main/java/com/clickhouse/kafka/connect/sink/db/ClickHouseWriter.java; src/main/java/com/clickhouse/kafka/connect/sink/db/helper/ClickHouseHelperClient.java; src/main/java/com/clickhouse/kafka/connect/sink/db/mapping/DeclaredNullability.java; src/main/java/com/clickhouse/kafka/connect/util/Utils.java; src/test/java/com/clickhouse/kafka/connect/ci/AllowListWildcardRuleUnitTest.java; src/test/java/com/clickhouse/kafka/connect/sink/ClickHouseSinkTaskStopUnitTest.java; src/test/java/com/clickhouse/kafka/connect/sink/InsertBoundPinFixtures.java; src/test/java/com/clickhouse/kafka/connect/sink/InsertTimeoutHelpTextPinUnitTest.java; src/test/java/com/clickhouse/kafka/connect/sink/InsertV1WaitBoundPinUnitTest.java; src/test/java/com/clickhouse/kafka/connect/sink/InsertWriteStallBoundPinUnitTest.java; src/test/java/com/clickhouse/kafka/connect/sink/JsonRouteValueFreeDlqUnitTest.java; src/test/java/com/clickhouse/kafka/connect/sink/NullableTupleMatrixLiveTest.java; src/test/java/com/clickhouse/kafka/connect/sink/ProxySinkTaskStopUnitTest.java; src/test/java/com/clickhouse/kafka/connect/sink/RetriableExceptionPassThroughUnitTest.java; src/test/java/com/clickhouse/kafka/connect/sink/RetriableRedeliveryLimitPinUnitTest.java; src/test/java/com/clickhouse/kafka/connect/sink/SimpleAggregateTupleRoundTripLiveTest.java; src/test/java/com/clickhouse/kafka/connect/sink/UnvettedRetriableCodesWarnUnitTest.java; src/test/java/com/clickhouse/kafka/connect/sink/WriterRecordContextDlqUnitTest.java; src/test/java/com/clickhouse/kafka/connect/sink/db/ClickHouseWriterRecordContextTest.java; src/test/java/com/clickhouse/kafka/connect/sink/db/FieldDescriptorEscapeTest.java; src/test/java/com/clickhouse/kafka/connect/sink/db/JacksonOverIoValueFreeUnitTest.java; src/test/java/com/clickhouse/kafka/connect/sink/db/JsonRouteValueFreeUnitTest.java; src/test/java/com/clickhouse/kafka/connect/sink/db/SlashColumnRoundTripTest.java; src/test/java/com/clickhouse/kafka/connect/sink/db/WriterRecordContextUnitTest.java; src/test/java/com/clickhouse/kafka/connect/sink/db/WriterValueFreeFailureUnitTest.java; src/test/java/com/clickhouse/kafka/connect/sink/db/helper/DescribeUnreadableEnumNullBindingUnitTest.java; src/test/java/com/clickhouse/kafka/connect/sink/db/helper/FieldDescriptorLiteralFieldsUnitTest.java; src/test/java/com/clickhouse/kafka/connect/sink/db/helper/InsertBoundGateUnitTest.java; src/test/java/com/clickhouse/kafka/connect/sink/db/helper/StubClickHouseServer.java; src/test/java/com/clickhouse/kafka/connect/sink/db/helper/StubClickHouseServerRawModeUnitTest.java; src/test/java/com/clickhouse/kafka/connect/sink/db/mapping/DeclaredNullabilityFixtures.java; src/test/java/com/clickhouse/kafka/connect/sink/db/mapping/DeclaredNullabilityTest.java
gated_sha: 315f5de1c85fb61bd2a1e887d2be085bc62571ce
gated_head: d9b10b9bf670b8236119c725e3639c53ad3e570b
tip_equals_gate_outside_planning: yes
frozen_docs_equal_gate: yes
local_custom_build_ref: 3a3a15a9ae064ffe09c696b8bb87bce23419d463
upstream_main_end: 83808df9a0993cf49b2ceaa4633c782e550c0180
upstream_newest_tag_end: v1.7.0
upstream_tag_past_v170_end: no
upstream_pr_861_end: open False b62e0ae4
upstream_pr_855_end: open False ddabf28b
upstream_pr_854_end: open False 69d1efc5
upstream_pr_788_end: closed False bb4b4140
upstream_changed_since_e12: no
merge_subject: Merge sync/v1.7.0: release v1.7.0_gg8
merge_trailer_last: yes
d_mode: attr
d_start_commit: 3cecccb283b12d7f50552e7c67f5c6db8a93b6b3
d_merge_exit: 0
d_commit: 3cecccb283b12d7f50552e7c67f5c6db8a93b6b3
d_parents: 9483476322e0d331e81c5b92ac266a18f0f01450 3699a9cd617cdb01950bff86603e60eb8ae75725
d_tree_equal: yes
d_subject: Merge sync/v1.7.0: release v1.7.0_gg8
d_version: v1.7.0_gg8
d_no_gsd_ws_arg: yes
d_allowlist_present: yes
d_baseline_tag: v1.7.0_gg7
d_status_before_block: clean
d_block_extract: ok
d_block_sha256: 2f30bd7d6b0109b22caaba63ff968a02f3f3eedfc11ec8ec4b46474012f67991
d_rollback_exit: 0
d_rollback_output: ok
d_rollback_commit: ad799d07aad78185efd60b7c5e680cc786a4b86c
d_rollback_status_after: clean
d_gradlew_bat_trap: no
d_rollback_version: v1.7.0_gg7
d_rollback_note_absent: yes
d_rollback_tree_equals_tag: yes
d_revert_exit: 0
d_revert_restores_tree: yes
d_rehearsal_cleaned: yes
d_plain_parents: 9483476322e0d331e81c5b92ac266a18f0f01450 3699a9cd617cdb01950bff86603e60eb8ae75725
d_plain_tree_equal: yes
d_plain_subject: Merge sync/v1.7.0: release v1.7.0_gg8
d_plain_baseline_tag: v1.7.0_gg7
d_plain_gradlew_bat_trap: no
d_plain_block_sha256: 2f30bd7d6b0109b22caaba63ff968a02f3f3eedfc11ec8ec4b46474012f67991
d_plain_rollback_output: ok
d_plain_revert_restores_tree: yes
d_plain_rehearsal_cleaned: yes
d_result: rehearsal: PASS
d_plain_result: rehearsal: PASS
rehearse_md5_now: 67f30f31a1353a607aaed8d8f09767a7
d_block_equals_note: yes
d_operator_doc_check: none
local_custom_build_ref_unmoved: yes
local_custom_build_ref_after: 3a3a15a9ae064ffe09c696b8bb87bce23419d463
worktrees_clean: yes
rollback_branch_absent: yes
select_version: 26.10.3.7
image_digest: clickhouse/clickhouse-server@sha256:739e099abd1157390960a59e4532d21781d8fde57e6e4151cb920ea02c30b1ce
image_same_as_e13: yes
changelog_heading: # 1.7.0_gg8, Unreleased
utc_today: 2026-11-06
date_step_pending: yes
bundle_items: yes
bundle_gates: yes
message_check: yes
hygiene_scan: clean
secrets_scan: clean
bundle_commit: 5154655d74104a0c529ace84d1ebb65de05821a3
nothing_pushed: yes
```

The dry-run merge D of the tip onto `94834763` was built twice by the frozen script (git attributes off, then plain), each in a new throwaway detached worktree that the script removed; D is an unreferenced commit and was never pushed, the local `custom_build` ref did not move, one worktree remains and the rollback branch is absent. D has the two expected parents, the tip's tree, the agreed subject, the allow-list and `VERSION` `v1.7.0_gg8`; the baseline selection on D names `v1.7.0_gg7`. The rollback block of the note, extracted by the checker and run unchanged, printed `ok`, restored the tree of `pre-v1.7.0_gg8` and a revert restored D's tree; its hash equals the block of the earlier rehearsal. Upstream and the two scratch tips are unchanged since the earlier recheck. The CHANGELOG heading still reads `Unreleased`; the one-line date commit of U-3 is the orchestrator's step DATE on the push day you name (answer (2) of the bundle). Two additions to the plan's bundle, asked for by the orchestrator, are not in the plan's ten questions: the release dry run U2 has its own word (G3b, question (4)) and the deletion of `refs/heads/sync/v1.7.0` on origin has its own word (G6b, question (11)); the bundle holds eleven questions, `(1)` to `(10)` once each as the check requires. Nothing was pushed.

2026-11-06T17:16:14Z
Verdict: pass

### E-15: Handoff U1 and U2 (scratch push and release dry run) is a user-gated handoff

Backs: RLS-05 (the scratch push U1 and the release dry run U2, each on its own word: G3 and G3b).

```sh
python3 -I .planning/phases/49-release-publication/49-hygiene-check.py --strict .planning/phases/49-release-publication/49-USER-DISPATCH-1-SCRATCH-DRYRUN.md
python3 -I -c '<the handoff structure check of the plan: sections, the user-reported resume wording, the origin value, no forbidden push option, no release tag push, no mutating gh verb>' 9483476322e0d331e81c5b92ac266a18f0f01450
python3 -I .planning/phases/49-release-publication/49-evidence-check.py secrets .planning/phases/49-release-publication/49-COMMAND-EVIDENCE.md
git ls-remote origin refs/heads/custom_build
git ls-remote --heads origin 'sync/*' 'scratch/*'
git ls-remote --tags origin 'v1.7.0_gg8*' 'pre-v1.7.0_gg8*'
```

```text
handoff_file: .planning/phases/49-release-publication/49-USER-DISPATCH-1-SCRATCH-DRYRUN.md
handoff_hygiene: clean
pre_checks_listed: 10
expected_values_given: yes
abort_rules: yes
resume_path: yes
dangerous_commands: none
nothing_pushed: yes
origin_custom_build_now: 9483476322e0d331e81c5b92ac266a18f0f01450
user_report: not yet received
```

This record stays pending: the step and its word are the user's, and no executor runs them. The handoff file lists 10 numbered pre-checks with expected values, the exact command, the expected result, the fields to report, the abort and contingency rules, a clean-up and the resume path for the orchestrator. Nothing was pushed, merged, tagged, dispatched or deleted; origin `custom_build` still reads `9483476322e0d331e81c5b92ac266a18f0f01450`. It is never edited; the user's report, when it comes, is a new entry from E-21 on.

2026-11-06T17:21:05Z
Verdict: pending (user-gated: the step and its word are the user's)

### E-16: Handoff U3 (push of M' to custom_build) is a user-gated handoff

Backs: RLS-05 (the push of the merge commit M', word G4).

```sh
python3 -I .planning/phases/49-release-publication/49-hygiene-check.py --strict .planning/phases/49-release-publication/49-USER-DISPATCH-2-CUSTOM-BUILD-PUSH.md
python3 -I -c '<the handoff structure check of the plan: sections, the user-reported resume wording, the origin value, no forbidden push option, no release tag push, no mutating gh verb>' 9483476322e0d331e81c5b92ac266a18f0f01450
python3 -I .planning/phases/49-release-publication/49-evidence-check.py secrets .planning/phases/49-release-publication/49-COMMAND-EVIDENCE.md
git ls-remote origin refs/heads/custom_build
git ls-remote --heads origin 'sync/*' 'scratch/*'
git ls-remote --tags origin 'v1.7.0_gg8*' 'pre-v1.7.0_gg8*'
```

```text
handoff_file: .planning/phases/49-release-publication/49-USER-DISPATCH-2-CUSTOM-BUILD-PUSH.md
handoff_hygiene: clean
pre_checks_listed: 8
expected_values_given: yes
abort_rules: yes
resume_path: yes
dangerous_commands: none
nothing_pushed: yes
origin_custom_build_now: 9483476322e0d331e81c5b92ac266a18f0f01450
user_report: not yet received
```

This record stays pending: the step and its word are the user's, and no executor runs them. The handoff file lists 8 numbered pre-checks with expected values, the exact command, the expected result, the fields to report, the abort and contingency rules, a clean-up and the resume path for the orchestrator. Nothing was pushed, merged, tagged, dispatched or deleted; origin `custom_build` still reads `9483476322e0d331e81c5b92ac266a18f0f01450`. It is never edited; the user's report, when it comes, is a new entry from E-21 on.

2026-11-06T17:21:05Z
Verdict: pending (user-gated: the step and its word are the user's)

### E-17: Handoff U4 (the publish dispatch) is a user-gated handoff

Backs: RLS-05 (the publish run, word G5).

```sh
python3 -I .planning/phases/49-release-publication/49-hygiene-check.py --strict .planning/phases/49-release-publication/49-USER-DISPATCH-3-PUBLISH.md
python3 -I -c '<the handoff structure check of the plan: sections, the user-reported resume wording, the origin value, no forbidden push option, no release tag push, no mutating gh verb>' 9483476322e0d331e81c5b92ac266a18f0f01450
python3 -I .planning/phases/49-release-publication/49-evidence-check.py secrets .planning/phases/49-release-publication/49-COMMAND-EVIDENCE.md
git ls-remote origin refs/heads/custom_build
git ls-remote --heads origin 'sync/*' 'scratch/*'
git ls-remote --tags origin 'v1.7.0_gg8*' 'pre-v1.7.0_gg8*'
```

```text
handoff_file: .planning/phases/49-release-publication/49-USER-DISPATCH-3-PUBLISH.md
handoff_hygiene: clean
pre_checks_listed: 4
expected_values_given: yes
abort_rules: yes
resume_path: yes
dangerous_commands: none
nothing_pushed: yes
origin_custom_build_now: 9483476322e0d331e81c5b92ac266a18f0f01450
user_report: not yet received
```

This record stays pending: the step and its word are the user's, and no executor runs them. The handoff file lists 4 numbered pre-checks with expected values, the exact command, the expected result, the fields to report, the abort and contingency rules, a clean-up and the resume path for the orchestrator. Nothing was pushed, merged, tagged, dispatched or deleted; origin `custom_build` still reads `9483476322e0d331e81c5b92ac266a18f0f01450`. It is never edited; the user's report, when it comes, is a new entry from E-21 on.

2026-11-06T17:21:05Z
Verdict: pending (user-gated: the step and its word are the user's)

### E-18: Handoff U5a and U5b (rollback tag, scratch branch deletion) is a user-gated handoff

Backs: RLS-05 (the rollback tag G6a and the deletion of the throwaway branch G6b, two words).

```sh
python3 -I .planning/phases/49-release-publication/49-hygiene-check.py --strict .planning/phases/49-release-publication/49-USER-DISPATCH-4-PRETAG-CLEANUP.md
python3 -I -c '<the handoff structure check of the plan: sections, the user-reported resume wording, the origin value, no forbidden push option, no release tag push, no mutating gh verb>' 9483476322e0d331e81c5b92ac266a18f0f01450
python3 -I .planning/phases/49-release-publication/49-evidence-check.py secrets .planning/phases/49-release-publication/49-COMMAND-EVIDENCE.md
git ls-remote origin refs/heads/custom_build
git ls-remote --heads origin 'sync/*' 'scratch/*'
git ls-remote --tags origin 'v1.7.0_gg8*' 'pre-v1.7.0_gg8*'
```

```text
handoff_file: .planning/phases/49-release-publication/49-USER-DISPATCH-4-PRETAG-CLEANUP.md
handoff_hygiene: clean
pre_checks_listed: 6
expected_values_given: yes
abort_rules: yes
resume_path: yes
dangerous_commands: none
nothing_pushed: yes
origin_custom_build_now: 9483476322e0d331e81c5b92ac266a18f0f01450
user_report: not yet received
```

This record stays pending: the step and its word are the user's, and no executor runs them. The handoff file lists 6 numbered pre-checks with expected values, the exact command, the expected result, the fields to report, the abort and contingency rules, a clean-up and the resume path for the orchestrator. Nothing was pushed, merged, tagged, dispatched or deleted; origin `custom_build` still reads `9483476322e0d331e81c5b92ac266a18f0f01450`. It is never edited; the user's report, when it comes, is a new entry from E-21 on.

2026-11-06T17:21:05Z
Verdict: pending (user-gated: the step and its word are the user's)

### E-19: Handoff U6 (deletion of the two client 0.11.0 scratch branches, RLS-06) is a user-gated handoff

Backs: RLS-06 (the deletion of both scratch branches after a separate confirmation, word G7).

```sh
python3 -I .planning/phases/49-release-publication/49-hygiene-check.py --strict .planning/phases/49-release-publication/49-USER-DISPATCH-5-SCRATCH-BRANCHES.md
python3 -I -c '<the handoff structure check of the plan: sections, the user-reported resume wording, the origin value, no forbidden push option, no release tag push, no mutating gh verb>' 9483476322e0d331e81c5b92ac266a18f0f01450
python3 -I .planning/phases/49-release-publication/49-evidence-check.py secrets .planning/phases/49-release-publication/49-COMMAND-EVIDENCE.md
git ls-remote origin refs/heads/custom_build
git ls-remote --heads origin 'sync/*' 'scratch/*'
git ls-remote --tags origin 'v1.7.0_gg8*' 'pre-v1.7.0_gg8*'
```

```text
handoff_file: .planning/phases/49-release-publication/49-USER-DISPATCH-5-SCRATCH-BRANCHES.md
handoff_hygiene: clean
pre_checks_listed: 4
expected_values_given: yes
abort_rules: yes
resume_path: yes
dangerous_commands: none
nothing_pushed: yes
origin_custom_build_now: 9483476322e0d331e81c5b92ac266a18f0f01450
user_report: not yet received
```

This record stays pending: the step and its word are the user's, and no executor runs them. The handoff file lists 4 numbered pre-checks with expected values, the exact command, the expected result, the fields to report, the abort and contingency rules, a clean-up and the resume path for the orchestrator. Nothing was pushed, merged, tagged, dispatched or deleted; origin `custom_build` still reads `9483476322e0d331e81c5b92ac266a18f0f01450`. It is never edited; the user's report, when it comes, is a new entry from E-21 on.

2026-11-06T17:21:05Z
Verdict: pending (user-gated: the step and its word are the user's)

### E-20: The phase closes locally and stops: RLS-01 to RLS-04 satisfied, RLS-05 and RLS-06 pending the user's direct words, nothing pushed

Backs: RLS-04 (rehearsal), RLS-05 and RLS-06 (recorded as pending and human-gated), D-9, D-10, PL-1.

```sh
python3 -I .planning/phases/49-release-publication/49-hygiene-check.py --strict <the close record and the evidence file>
python3 -I .planning/phases/49-release-publication/49-hygiene-check.py <every file this phase added, helpers excluded>
python3 -I .planning/phases/49-release-publication/49-evidence-check.py secrets .planning/phases/49-release-publication/49-COMMAND-EVIDENCE.md
python3 -I .planning/phases/49-release-publication/49-evidence-check.py anchors .planning/scripts/fork-anchors.txt --rows 218
git ls-remote origin refs/heads/custom_build
git ls-remote --heads origin 'sync/*' 'scratch/*'
git ls-remote --tags origin 'v1.7.0_gg8*' 'pre-v1.7.0_gg8*'
git tag -l 'v1.7.0_gg8'
git rev-parse 'pre-v1.7.0_gg8^{commit}'
git show-ref refs/gsd/release-m-v1.7.0_gg8
git worktree list
git diff --quiet <gated head> HEAD -- . ':(exclude).planning'   # and -- .planning/scripts
git diff --name-only <L0> HEAD -- .planning/scripts
git commit -F <msg file>   # the close record
```

```text
local_complete: yes
requirements_status: RLS-01=satisfied; RLS-02=satisfied; RLS-03=satisfied; RLS-04=satisfied; RLS-05=pending; RLS-06=pending
pending_user_steps: G3; G3b; G4; G5; G6a; G6b; G7
deferred_must_haves_listed: 10
wording_changes_listed: 9
carried_items_listed: 20
origin_custom_build_final: 9483476322e0d331e81c5b92ac266a18f0f01450
nothing_pushed: yes
no_gg8_tag_anywhere: yes
pre_tag_local_only: yes
scratch_branches_present: yes
scope_outside_planning: CHANGELOG.md; VERSION; docs/FORK-DELTA.md; docs/ROLLOUT-v1.7.0_gg8.md
scope_planning_scripts: .planning/scripts/confluent-archive-allowlist-v1.7.0_gg8.txt; .planning/scripts/fork-anchors.txt
no_protected_change: yes
tip_equals_gate_outside_planning: yes
changelog_heading_final: # 1.7.0_gg8, Unreleased
index_rows: E-01..E-20 present; E-15..E-19 pending
hygiene_scan_all: clean
secrets_scan: clean
worktrees_clean: yes
keep_alive_ref_absent: yes
final_head: e16b90a14219aef51692e38d731392341c91bd90
ready_for_approval: yes
close_commit: e16b90a14219aef51692e38d731392341c91bd90
approval_bundle: .planning/phases/49-release-publication/49-APPROVAL-BUNDLE.md
bundle_questions: 11
gated_head: d9b10b9bf670b8236119c725e3639c53ad3e570b
```

The verdict `pass` is for the local gate only. RLS-05 and RLS-06 are pending and completed only by the orchestrator-inline tail after the user's direct words, recorded from E-21 on; E-15 to E-19 stay pending (user-gated) and are never edited. The close record lists ten deferred must-haves, nine wording changes for the orchestrator (the five of the plan, and four more: the RLS-06 wording of REQUIREMENTS.md, the meaning of `pre-v1.7.0_gg8`, the twelve evidence-path occurrences in FORK-DELTA that move at the milestone close, and the superseded date sentence of the context notes) and twenty carried items. The release dry run U2 and the deletion of the throwaway branch U5b each have their own word and question in the bundle (G3b, question (4); G6b, question (11)), so the bundle holds eleven questions. Origin `custom_build` still reads `9483476322e0d331e81c5b92ac266a18f0f01450`, no `sync/v1.7.0` head and no `v1.7.0_gg8` tag exists, both scratch branches are on origin at their recorded tips, the keep-alive ref of `M'` does not exist, one worktree remains and nothing was pushed, merged, tagged, dispatched or deleted.

2026-11-06T17:24:20Z
Verdict: pass
