# Phase 44: Command evidence

- **Base commit:** `local_head` = `9a5209c17e5380ec1f675cd8b3225250d3ff41fe` (HEAD when plan 44-01 started, branch `sync/v1.7.0`).
- **Shell:** macOS zsh, multi-line logic under `/bin/bash` or `python3 -I`. Gradle is run as `env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain ...`; a JVM crash is handled by the crash rule of the plans (delete the crash files by name, re-run once, then `-PjavaVersion=21`).
- **Docker:** colima profile `vz`; the ClickHouse image is the local `clickhouse/clickhouse-server:latest`, never pulled.
- **Date (UTC):** 2026-11-02.
- **Entry format:** each entry has a `### E-NN: {title}` heading, a `Backs:` line, an `sh` fence with the commands as run, a `text` fence of `key: value` lines (keys match `[a-z0-9_]+`, one fact per line, no key twice) with free text allowed after the fence, a UTC timestamp line, and a final line `Verdict: pass`, `Verdict: fail`, `Verdict: pending ({reason})` or `Verdict: unmeasured ({reason})`.
- **Rule:** a fail or pending verdict is fixed by re-measuring in a new entry, never by editing the evidence.
- **Helpers:** `44-evidence-check.py` is a byte copy of Phase 43's `43-evidence-check.py`, `44-doc-check.py` a byte copy of Phase 43's `43-doc-check.py`, `44-allowlist-check.py` and `44-hygiene-check.py` byte copies of Phase 38's helpers, and `44-review-check.py` is Phase 38's `38-review-check.py` with `38-` replaced by `44-`, `RLS-04` replaced by `RLS-03` and one marked block added (the review-attestation `agents` subcommand); none is ever edited.
- **Evidence ids (O-11):** plans make E-01 to E-19; E-15 to E-18 are the pending records of the four user handoffs and stay pending; the publication tail is recorded by the orchestrator from E-20 on.
- **No push happens in Phase 44 by an agent.** Reads use `git ls-remote`, `gh api` GET and `curl -s` GET only.

## Index

| Entry | Requirement | Plan | Verdict |
|-------|-------------|------|---------|
| E-01 | RLS-01 | 44-01 | pass |
| E-02 | RLS-01 | 44-01 | pass |
| E-03 | RLS-01 | 44-01 | pass |
| E-04 | RLS-01 | 44-01 | pass |
| E-05 | RLS-03 | 44-02 | pass |
| E-06 | RLS-03 | 44-02 | pass |
| E-07 | RLS-03 | 44-02 | pass |
| E-08 | RLS-03 | 44-03 | pass |
| E-09 | RLS-03 | 44-03 | pass |
| E-10 | RLS-03 | 44-03 | pass |
| E-11 | RLS-02 | 44-04 | pass |
| E-12 | RLS-04 | 44-04 | fail |
| E-13 | RLS-04 | 44-04 | pass |
| E-14 | RLS-04 | 44-05 | pass |
| E-15 | RLS-04 | 44-05 | pending (user-gated) |
| E-16 | RLS-04 | 44-05 | pending (user-gated) |
| E-17 | RLS-04 | 44-05 | pending (user-gated) |
| E-18 | RLS-04 | 44-05 | pending (user-gated) |
| E-19 | RLS-01 to RLS-04 | 44-05 | pass |
| E-20 | RLS-04 | 44-04 | pass |
| E-21 | RLS-03 | 44-03 | pass |
| E-22 | RLS-04 | 44-04 | pass |
| E-23 | RLS-02 | 44-04 | pass |
| E-24 | RLS-04 | 44-04 | fail |
| E-25 | RLS-04 | 44-04 | fail |
| E-26 | RLS-04 | 44-04 | pass |
| E-27 | RLS-04 | 44-05 | pass |
| E-28 | RLS-04 | 44-05 | pass |
| E-29 | RLS-04 | 44-05 | pass |
| E-30 | RLS-04 | 44-05 | pass |
| E-31 | RLS-04 | 44-05 | pass |
| E-32 | RLS-04 | 44-05 | pass |

### E-01: Pre-flight: origin, pull requests and upstream re-read, helpers checked, premise confirmed on the untouched tree

Backs: RLS-01 (execution-start facts: branch and base commit, the origin and pull-request re-read, the first upstream recheck, the tree values, the Docker environment and the five helper copies, all on the tree before any tracked file changed).

```sh
mkdir -p /tmp/gsd44/exec /tmp/gsd44/w
git rev-parse HEAD
git rev-parse --abbrev-ref HEAD
git status --porcelain
git ls-remote origin refs/heads/custom_build
git rev-parse origin/custom_build
git rev-list --left-right --count origin/custom_build...sync/v1.7.0
git merge-base --is-ancestor 72e8ccffe05ff6dffb14098d0b8c0f67aaa6a236 HEAD
git rev-list --count HEAD --not --remotes=origin
git ls-remote --heads origin
git ls-remote --heads origin sync/v1.7.0
git ls-remote --tags origin 'v1.7.0_gg7*' 'pre-v1.7.0_gg7*'
git tag -l 'v1.7.0_gg7*' 'pre-v1.7.0_gg7*'
git rev-parse 'v1.7.0_gg6^{commit}'
gh api "repos/ne1r0n/clickhouse-kafka-connect/releases" --jq '[.[] | "\(.id) \(.tag_name) \(.draft)"] | join("; ")'
gh api "repos/ne1r0n/clickhouse-kafka-connect/pulls?state=open&per_page=50" --jq '[.[] | "\(.number) \(.head.sha[0:8])"] | sort | join("; ")'
git merge-base --is-ancestor <pull request head> HEAD
git ls-remote upstream refs/heads/main
git ls-remote --tags --refs upstream 'v*'
curl -s https://api.github.com/repos/ClickHouse/clickhouse-kafka-connect/pulls/<n>
python3 -I .planning/phases/44-release-publication/44-evidence-check.py anchors .planning/scripts/fork-anchors.txt --rows 190
docker run --rm --entrypoint clickhouse clickhouse/clickhouse-server:latest local --query 'SELECT version()'
docker image inspect --format '{{index .RepoDigests 0}}' clickhouse/clickhouse-server:latest
sysctl vm.swapusage
cp .planning/phases/43-document-fixes-phase-37-dispositions/43-evidence-check.py .planning/phases/44-release-publication/44-evidence-check.py
cp .planning/phases/43-document-fixes-phase-37-dispositions/43-doc-check.py .planning/phases/44-release-publication/44-doc-check.py
cp .planning/milestones/v1.7.0_gg6-maintenance-phases/38-release-publication/38-allowlist-check.py .planning/phases/44-release-publication/44-allowlist-check.py
cp .planning/milestones/v1.7.0_gg6-maintenance-phases/38-release-publication/38-hygiene-check.py .planning/phases/44-release-publication/44-hygiene-check.py
python3 -I /tmp/gsd44/exec/make-rv.py <38-review-check.py> .planning/phases/44-release-publication/44-review-check.py
cmp <source> <copy>
bash .planning/milestones/v1.7.0_gg6-maintenance-phases/38-release-publication/38-assets/hygiene-selftest.sh.txt .planning/phases/44-release-publication/44-hygiene-check.py /tmp/gsd44/w/hyg-selftest
python3 -I -X pycache_prefix=/tmp/gsd44/w/pyc -m py_compile <the five helpers>
```

```text
branch: sync/v1.7.0
local_head: 9a5209c17e5380ec1f675cd8b3225250d3ff41fe
dirty_paths: M .planning/config.json; M .planning/state.json; ?? .planning/phases/44-release-publication/44-allowlist-check.py; ?? .planning/phases/44-release-publication/44-doc-check.py; ?? .planning/phases/44-release-publication/44-evidence-check.py; ?? .planning/phases/44-release-publication/44-hygiene-check.py; ?? .planning/phases/44-release-publication/44-review-check.py
phase43_complete: yes
colima_vz: Running
python_version: 3.14.8
origin_custom_build: 72e8ccffe05ff6dffb14098d0b8c0f67aaa6a236	refs/heads/custom_build
origin_unchanged: yes
origin_tracking_equal: yes
left_right: 0 201
left_count: 0
gg6_ancestor: yes
unpublished_commits: 199
origin_heads: 14be67ce refs/heads/dependabot/github_actions/github-actions-3bc6d10805; 72e8ccff refs/heads/custom_build; 939e8992 refs/heads/scratch/clt-07-prebump-0.9.6; b5cccbd3 refs/heads/dependabot/gradle/com.google.code.gson-gson-2.15.0; bec167d8 refs/heads/main; de8c685b refs/heads/scratch/clt-07-client-0.11.0
origin_sync_ref: none
origin_gg7_tags: none
local_gg7_tags: none
tag_gg6: 72e8ccffe05ff6dffb14098d0b8c0f67aaa6a236
releases_on_origin: 430511927 v1.7.0_gg6 false; 426690002 v1.7.0_gg5 false
pr_heads: 21 14be67ce; 22 b5cccbd3
pr_heads_unchanged: yes
pr_heads_ancestors: yes
upstream_main: 9bed6f7970670a55c66be8efda246889e1f95bd0
upstream_main_unchanged: yes
upstream_newest_tag: v1.7.0
upstream_tag_past_v170: no
upstream_pr_861: open False 3d4cfdf2
upstream_pr_855: open False ddabf28b
upstream_pr_857: open False 11d88bc0
upstream_prs_unchanged: yes
version_file: v1.7.0_gg6
anchors_version_row: VERSION|v1.7.0_gg6|1|BLD-07
anchors_mirror: rows=190 bad=0
gson_catalog: 2.15.0
clickhouse_version: 26.10.3.7
clickhouse_digest: clickhouse/clickhouse-server@sha256:739e099abd1157390960a59e4532d21781d8fde57e6e4151cb920ea02c30b1ce
digest_matches_research: yes
swap_used: used = 27152.88M of 27648.00M total (encrypted), 495.12M free
helper_evidence_identical: yes
helper_doc_identical: yes
helper_allowlist_identical: yes
helper_hygiene_identical: yes
review_source_counts: 38-=5 RLS-04=3
helper_review_derived: yes
helper_review_agents_selftest: 5 of 5 as expected
hygiene_selftest: selftest_default_hits=7 selftest_strict_hits=9
helper_syntax: yes
testunit_baseline: not run here: plan 44-04 gates the tip; expected 1826 tests and 2 skipped, Phase 43 verifier and research run
```

The premise holds on the untouched tree: origin `custom_build` still reads the gg6 release commit, the left count is 0 (the right count of 201 is the unpublished `sync/v1.7.0` history, which already contains the gg6 merge), no gg7 or pre-gg7 tag exists locally or on origin, and `sync/v1.7.0` has no head on origin. The two open pull requests on origin (21 and 22) point at commits that are ancestors of `HEAD`. The first upstream recheck found nothing new: upstream `main` is the planning value, the newest tag is `v1.7.0`, and upstream pull requests 861, 855 and 857 are open and unmerged with their planning heads. The swap figure is high, which is the condition under which the OpenJ9 inline-mock crash was seen before; the crash rule applies to every Gradle run of this plan. The helper `44-review-check.py` was verified against the stated derivation (file minus the marked block equals the replaced source, block directly after the `COMMANDS` line) and its `agents` subcommand against five synthetic cases.

2026-11-02T21:01:17Z

Verdict: pass

### E-02: Tracer: VERSION bumped through the script with its anchors row; the generated constant, the baseline tag, the anchors mirror, the guards and the local rollback tag agree

Backs: RLS-01 (the release identity moves to `v1.7.0_gg7` in `VERSION`, in its row of `fork-anchors.txt` and in the generated constant, in one two-file commit, with the baseline choice and the local rollback tag proven on the real files and no Docker).

```sh
TG=$(git tag --merged HEAD --list 'v*') && printf '%s\n' "${TG}" | python3 -I .github/scripts/baseline_tag.py v1.7.0_gg7
python3 -I .github/scripts/bump_version.py gg --version-file /tmp/gsd44/w/bump/VERSION --anchors-file /tmp/gsd44/w/bump/fork-anchors.txt
git diff --no-index --numstat <original> <copy>
python3 -I .github/scripts/bump_version.py gg
git diff --name-only
git diff --numstat -- VERSION
git diff --numstat -- .planning/scripts/fork-anchors.txt
python3 -I .planning/phases/44-release-publication/44-evidence-check.py anchors .planning/scripts/fork-anchors.txt --rows 190
env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain clean generateVersionFile > /tmp/gsd44/exec/version-clean.log 2>&1
grep ARTIFACT_VERSION build/generated/sources/version/java/main/com/clickhouse/kafka/connect/sink/Version.java
rm -rf build/test-results/testUnit
DOCKER_HOST=unix:///nonexistent/docker.sock env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain testUnit --rerun --tests '*MBeanNameUnitTest' --tests '*BumpVersionScriptUnitTest' --tests '*BaselineTagScriptUnitTest' > /tmp/gsd44/exec/bump-guards.log 2>&1
python3 -I .planning/phases/44-release-publication/44-evidence-check.py failed build/test-results/testUnit
python3 -I .planning/phases/44-release-publication/44-evidence-check.py junit build/test-results/testUnit --class <name>
git add VERSION .planning/scripts/fork-anchors.txt
git commit -F /tmp/gsd44/exec/msg-bump.txt
git tag pre-v1.7.0_gg7 72e8ccffe05ff6dffb14098d0b8c0f67aaa6a236
git rev-parse 'pre-v1.7.0_gg7^{commit}'
git cat-file -t pre-v1.7.0_gg7
git tag -l 'v1.7.0_gg7'
```

```text
baseline_for_gg7: v1.7.0_gg6
scratch_bump_stdout: v1.7.0_gg7
scratch_bump_numstat: 1/1; 1/1
bump_stdout: v1.7.0_gg7
bump_version_file: v1.7.0_gg7
bump_changed_files: .planning/scripts/fork-anchors.txt; VERSION
bump_numstat: 1/1; 1/1
version_rows_in_anchors: 1
anchors_mirror_after: rows=190 bad=0
version_clean_exit: 0
generated_version_line: public static final String ARTIFACT_VERSION = "v1.7.0_gg7";
bump_guard_exit: 0
bump_guard_failed: OK failed: 0 failing testcases, 0 expected
bump_xml: OK junit build/test-results/testUnit class *BumpVersionScriptUnitTest: tests=52 failures=0 errors=0 skipped=0 files=1
mbean_xml: OK junit build/test-results/testUnit class *MBeanNameUnitTest: tests=11 failures=0 errors=0 skipped=0 files=1
baseline_xml: OK junit build/test-results/testUnit class *BaselineTagScriptUnitTest: tests=19 failures=0 errors=0 skipped=0 files=1
bump_commit: 1d7383b9f21101c800c96a61fea714d999d6b092
bump_commit_files: .planning/scripts/fork-anchors.txt; VERSION
pre_tag_target: 72e8ccffe05ff6dffb14098d0b8c0f67aaa6a236
pre_tag_type: commit
baseline_after_bump: v1.7.0_gg6
allowlist_gg7_present: no
note_gg7_present: no
release_tag_absent: yes
src_unchanged: yes
hygiene_scan: clean
```

The three guard classes ran together (82 tests, none failed or skipped) with the Docker socket pointed at a path that does not exist, so none of them needs Docker. Neither Gradle run produced a JVM crash file, so the crash rule did not apply. The scratch rehearsal on copies and the real bump agree: both change one line in each of the two files. The rollback tag is a lightweight tag on the gg6 release commit, kept local; only the user's push step publishes it.

2026-11-02T21:02:50Z

Verdict: pass

### E-03: The 1.7.0_gg7 CHANGELOG section, released sections untouched

Backs: RLS-01 (the release carries a dated `1.7.0_gg7` section in operator terms, each client-V2-only effect scoped to client V2, and every released section is byte-identical to the phase-start file).

```sh
date -u +%F
git show 9a5209c17e5380ec1f675cd8b3225250d3ff41fe:CHANGELOG.md > /tmp/gsd44/exec/changelog-old.md
python3 -I <assemble: new section, one blank line, the unchanged phase-start file> > CHANGELOG.md
python3 -I <tail check: the new file ends with the phase-start file byte for byte>
python3 -I <section check: five headings in order, no planning identifier, no line over 125 columns without a URL, 17 required tokens>
python3 -I .planning/phases/44-release-publication/44-doc-check.py links CHANGELOG.md
git diff -U0 9a5209c17e5380ec1f675cd8b3225250d3ff41fe -- CHANGELOG.md   # the added lines, prefix removed, to /tmp/gsd44/exec/changelog-added.txt
python3 -I .planning/phases/44-release-publication/44-hygiene-check.py --strict /tmp/gsd44/exec/changelog-added.txt
python3 -I .planning/phases/44-release-publication/44-evidence-check.py secrets .planning/phases/44-release-publication/44-COMMAND-EVIDENCE.md
python3 -I .planning/phases/44-release-publication/44-evidence-check.py anchors .planning/scripts/fork-anchors.txt --rows 190
git add CHANGELOG.md
git commit -F /tmp/gsd44/exec/msg-cl.txt
```

```text
changelog_date: 2026-11-02
changelog_tail_identical: yes
changelog_section_check: OK changelog-section
links_ok: yes
hygiene_scan_added_lines: clean
secrets_scan: clean
anchors_mirror: rows=190 bad=0
changelog_commit: 9c30e3ba08aa32141e1c9965e2c130870b60e8fa
changelog_commit_files: CHANGELOG.md
```

The section adds 69 lines above the unchanged `1.7.0_gg6` heading. The one broken relative link is the pointer to `docs/ROLLOUT-v1.7.0_gg7.md`, the note that plan 44-02 creates. The section carries one bullet beyond the plan's list, the JMX `version=` segment and client-name change, because the release identity moves and gg6 documented the same change in the same place. The heading date is the UTC date at writing; the orchestrator compares it with the UTC date at the merge.

2026-11-02T21:04:53Z

Verdict: pass

### E-04: docs/FORK-DELTA.md is final for gg7: false claim fixed, recounted, raw numbers refreshed once

Backs: RLS-01 (the false `Processing.java` byte-identical claim and the stale 33/14/16 and 40/14/26 recounts are replaced by a recount with the file's own method, the `HEAD`-relative commands are tag-pair commands, the raw numbers are measured once on the release tip with an after-gg7 row per scope equal to the command output, and the gg7 intro, pointer and next-syncer paragraphs are written).

```sh
python3 -I /tmp/gsd44/exec/recount.py   # every src/main file at v1.7.0 against HEAD (identical, import-only after dropping import and blank lines, fork logic), new and deleted files, and the same recount at v1.7.0_gg6
git diff --shortstat v1.7.0 HEAD -- src build.gradle.kts gradle .github
git diff --shortstat v1.7.0 HEAD -- src/main
git diff --shortstat v1.7.0_gg6 HEAD -- src build.gradle.kts gradle .github
git diff --shortstat v1.7.0_gg6 HEAD -- src/main
git diff --shortstat v1.7.0_gg6 HEAD -- <path>   # src/test, src/integrationTest, src/testFixtures, .github, gradle, build.gradle.kts
git diff --name-only v1.7.0_gg6 HEAD -- src/main
git diff --shortstat v1.7.0_gg4 v1.7.0_gg5 -- <path>   # .github, src/test, src/integrationTest
git diff --quiet v1.7.0 HEAD -- <each file named by a byte-identical claim>
git diff v1.7.0 HEAD -- src/main/java/com/clickhouse/kafka/connect/sink/processing/Processing.java
python3 -I .planning/phases/44-release-publication/44-doc-check.py numbers docs/FORK-DELTA.md 'after gg7: `v1.7.0` to `v1.7.0_gg7`' --wider "345 42439 1221" --narrow "43 3183 294"
python3 -I <head span check: backticked git diff spans that still name HEAD, Processing.java byte-identical lines, split rows>
python3 -I .planning/phases/44-release-publication/44-doc-check.py links docs/FORK-DELTA.md
git diff -U0 9a5209c17e5380ec1f675cd8b3225250d3ff41fe -- docs/FORK-DELTA.md   # the added lines, prefix removed, to /tmp/gsd44/exec/fd-added.txt
python3 -I .planning/phases/44-release-publication/44-hygiene-check.py --strict /tmp/gsd44/exec/fd-added.txt
python3 -I .planning/phases/44-release-publication/44-evidence-check.py secrets .planning/phases/44-release-publication/44-COMMAND-EVIDENCE.md
python3 -I .planning/phases/44-release-publication/44-evidence-check.py anchors .planning/scripts/fork-anchors.txt --rows 190
git add docs/FORK-DELTA.md
git commit -F /tmp/gsd44/exec/msg-fd.txt
```

```text
files_at_v170: 63
identical_files_now: 30
import_only_now: 14
fork_logic_modified_now: 19
new_files_now: 10
deleted_since_v170: 0
differ_now: 43
fork_logic_total_now: 29
gg6_recount: 33 14 16 10
moved_since_gg6: KeeperStateProvider.java; Processing.java; StateProvider.java
wider_now: 345 42439 1221
narrow_now: 43 3183 294
wider_since_gg6: 34 2826 112
narrow_since_gg6: 8 194 22
by_dir_since_gg6: src/test=18 2470 28; src/integrationTest=2 155 55; src/testFixtures=0 0 0; .github=5 6 6; gradle=1 1 1; build.gradle.kts=0 0 0
src_main_files_since_gg6: ClickHouseHelperClient.java; ClickHouseSinkTask.java; ClickHouseWriter.java; KeeperStateProvider.java; Processing.java; ProxySinkTask.java; StateProvider.java; Utils.java
gg4_gg5_tagpair: .github=6 616 92; src/test=5 1486 25; src/integrationTest=1 23 1
byte_identical_claims_checked: 5 claims, 5 true
numbers_check: OK numbers: label 'after gg7: `v1.7.0` to `v1.7.0_gg7`'
head_command_check: head_spans=['git diff --shortstat v1.7.0 HEAD'] processing_identical_lines=[] processing_rows=1 keytovalue_rows=1
links_ok: yes
hygiene_scan_added_lines: clean
secrets_scan: clean
anchors_mirror: rows=190 bad=0
forkdelta_commit: e979004e91a4f84fa9223b9d957ab3216fa37e00
forkdelta_commit_files: docs/FORK-DELTA.md
```

All figures equal the planning values, so nothing was forced. The method check holds: the same recount at `v1.7.0_gg6` prints 33 14 16 10, the published figures. The raw numbers were measured once, on this tip, and no path in their scopes (`src`, `build.gradle.kts`, `gradle`, `.github`) changes in the rest of the phase. The empty by-directory diffs (`src/testFixtures`, `build.gradle.kts`) make the shortstat helper exit with a usage error, so they are recorded as `0 0 0` after the empty output was confirmed. The five byte-identical claims that name a file or a file set were checked: `integrationTests.yaml` against upstream `v1.7.0` (true), `KeyToValue.java` against upstream (true), `Processing.java` against upstream at `v1.7.0_gg6`, the state before the one-line change (true), the `KeeperStateProvider` injected-client constructor and `init()` against `v1.7.0_gg6` (the diff touches only the factory constructor, `close()` and the owned-client field; true), and `getStateRecord` and `setStateRecord` against `v1.7.0_gg6` (not in the diff; true). The row counting 128 of 143 upstream test-tree files as byte-identical could not be re-derived here with any scope tried, so it is not a checked claim; every scope tried gives the same counts at `v1.7.0_gg6` and at this tip, so this release does not move it. The `Processing.java` row now reads as a one-line difference, which the diff of the file against `v1.7.0` confirms (the line at `doLogic` 137, `topic()` to `originalTopic()`). The gson row already names `2.15.0` with upstream's `2.13.1`, so it needed no edit. The two broken relative links are the pointers to `docs/ROLLOUT-v1.7.0_gg7.md`, the note that plan 44-02 creates.

2026-11-02T21:09:09Z

Verdict: pass

### E-05: Note tracer: re-measured archives and source facts, preamble and section G, rollback block rehearsed on a dry-run merge

Backs: RLS-03 (the note skeleton exists end to end before any effect section: both archives rebuilt and compared, the source facts read, the preamble with the four label forms, section G rows D1 to D5, and the rollback block of the note extracted and run unchanged on a dry-run merge of the committed tip onto the gg6 commit).

```sh
git archive v1.7.0_gg6 | tar -x -C /tmp/gsd44/w/arch-base-src
git archive HEAD | tar -x -C /tmp/gsd44/w/arch-tip-src
env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain createConfluentArchive   # in each export, with its own wrapper
python3 -I .github/scripts/compare_confluent_archives.py /tmp/gsd44/w/arch/base.zip /tmp/gsd44/w/arch/tip.zip /tmp/gsd44/w/arch/empty.txt > /tmp/gsd44/exec/cmp-empty-e05.out
python3 -I /tmp/gsd44/exec/classify.py /tmp/gsd44/exec/cmp-empty-e05.out
python3 -I /tmp/gsd44/exec/srcfacts.py "$PWD"
git diff --shortstat v1.7.0_gg6 HEAD -- src/main
curl -s https://api.github.com/repos/ne1r0n/clickhouse-kafka-connect/releases/430511927
curl -sL -o /tmp/gsd44/w/gg6asset/asset.zip https://github.com/ne1r0n/clickhouse-kafka-connect/releases/download/v1.7.0_gg6/clickhouse-kafka-connect-v1.7.0_gg6.zip
shasum -a 256 /tmp/gsd44/w/gg6asset/asset.zip
python3 -I .github/scripts/compare_confluent_archives.py /tmp/gsd44/w/gg6asset/asset.zip /tmp/gsd44/w/arch/base.zip /tmp/gsd44/w/gg6asset/empty.txt
javap -c -p -v <StateRecord.class and ExtractTopic.class of the asset and of the rebuild>
python3 -I .planning/phases/44-release-publication/44-note-check.py rows docs/ROLLOUT-v1.7.0_gg7.md --only D
python3 -I .planning/phases/44-release-publication/44-note-check.py labels|forbidden|claims docs/ROLLOUT-v1.7.0_gg7.md
python3 -I /tmp/gsd44/exec/extract-rb.py docs/ROLLOUT-v1.7.0_gg7.md /tmp/gsd44/exec/rollback-block.sh
git add .planning/phases/44-release-publication/44-note-check.py docs/ROLLOUT-v1.7.0_gg7.md && git commit -F /tmp/gsd44/exec/msg-n1.txt
/tmp/gsd44/exec/rehearse.sh <note commit> /tmp/gsd44/exec/rollback-block.sh /tmp/gsd44/w/rb
MODE=plain /tmp/gsd44/exec/rehearse.sh <note commit> /tmp/gsd44/exec/rollback-block.sh /tmp/gsd44/w/rb-plain
python3 -I .planning/phases/44-release-publication/44-hygiene-check.py --strict docs/ROLLOUT-v1.7.0_gg7.md
python3 -I .planning/phases/44-release-publication/44-evidence-check.py secrets docs/ROLLOUT-v1.7.0_gg7.md
```

```text
base_verify_line: verifyConfluentArchive: 7933 inner entries, no nested jar, no Kafka API class
tip_verify_line: verifyConfluentArchive: 7932 inner entries, no nested jar, no Kafka API class
base_entries: 7933
tip_entries: 7932
base_bytes: 13558633
tip_bytes: 13564144
compare_exit: 1
compare_totals: totals files-a 7513 files-b 7512 only-a 1 only-b 0 changed 119 same 7393
compare_unallowed: 120
compare_owner_split: gson classes changed 101; gson classes only in the base 1; gson metadata 3; connector classes 14; manifest 1; anything else 0
compare_version_lines: 18
compare_version_table: com.google.code.gson:gson 2.14.0 -> 2.15.0
compare_manifest: MANIFEST.MF: differs only in Implementation-Version
src_main_shortstat: 8 files changed, 194 insertions(+), 22 deletions(-)
client_version_default: V1
insert_timeout_default: TimeUnit.MINUTES.toMillis(4)
insert_timeout_min: atLeast(100)
originaltopic_line: line 137: String originalTopic = record.getSinkRecord().originalTopic();
warn_text: additionalRetriableErrorCodes lists {}, which the connector does not treat as transient. A batch that fails with such a code is redelivered with no retry budget and outside errors.tolerance and the dead letter queue, so a permanent error blocks the partition until the setting is changed.
vetted_codes: 15 + 216
writer_stop_wait: 5 seconds
gg6_asset_digest: sha256:e745e7d5363fca8067ddfcc80f3ebea034b93abb4f2912e514492201a729c2af
gg6_asset_bytes: 13558692
gg6_asset_download_digest_equal: yes
gg6_asset_vs_rebuild_totals: totals files-a 7513 files-b 7513 only-a 0 only-b 0 changed 6 same 7507
gg6_asset_vs_rebuild_changed: ClickHouseWriter; Column; Column$ColumnBuilder; StateRecord; ConvertToMapOrJson; ExtractTopic (all under com/clickhouse/kafka/connect)
gg6_asset_vs_rebuild_size_delta: 59
gg6_asset_vs_rebuild_javap: StateRecord and ExtractTopic examined; both differ in the compiled string concatenation (the asset calls String.valueOf before the invokedynamic concat); the other four not examined; the compiler that built the asset was not established
rehearsal_tip: 7ef33b4feae0759c17a77e171e4d53e36b924b3a
rehearsal_parents: 72e8ccffe05ff6dffb14098d0b8c0f67aaa6a236 7ef33b4feae0759c17a77e171e4d53e36b924b3a
rehearsal_tree_equal: yes
d0_status_before_block: clean
rollback_exit: 0
rollback_output: ok
gradlew_bat_trap: no
rollback_status_after: clean
rollback_version: v1.7.0_gg6
rollback_note_absent: yes
rollback_tree_equals_tag: yes
revert_restores_tree: yes
plain_switch_exit: 0
plain_revert_restores_tree: yes
rehearsal_cleaned: yes
nc_selftest: 4 of 4 failed as expected
nc_rows_d: OK rows=5 measured=4 code=3 derived=0 mechanism=0
nc_labels: OK labels: 4 of 4 defined
nc_forbidden: OK forbidden: 0 lines
nc_claims: OK claims: 0 hex strings outside the allowed set
hygiene_note: clean
secrets_scan: clean
note_commit_1: 7ef33b4feae0759c17a77e171e4d53e36b924b3a
```

The two archives were rebuilt from clean exports and compared with a comment-only list: every one of the 120 differences is a gson entry, a connector class or the manifest, and the version table differs for `gson` only. The planning figures held (7933 and 7932 entries, the two sizes, 120 differences). One planning statement did not hold: the research and the plan said that a rebuild of tag `v1.7.0_gg6` compares equal entry by entry to the published gg6 asset. Measured here, the local rebuild is 59 bytes smaller than the asset and six connector class files differ; the two that were disassembled differ in the compiled form of a string concatenation, which points at a different compiler, not a different source, but that cause was not established. The note therefore tells operators to download the asset and does not claim the rebuild is equal to it. The rehearsal ran on the committed note (the note is present in the merged tip and absent after the block) with git 2.50.1; the first run switched git's attributes off for the revert, the second ran plain `git switch` and `git revert` and also restored the merged tree, and `gradlew.bat` was not listed after the block, so the note carries no sentence about it. A scratch rebuild on another JDK may differ again; the runner dry run of plan 44-05 is the check that counts for the gg7 allow-list.

2026-11-02T21:19:25Z

Verdict: pass

### E-06: Note sections A and B: closing on stop, the bounded client V2 insert with its 13 claims in order, the supersession table

Backs: RLS-03 (section A rows F1 to F5, section B rows T1 to T15 with the 13 claims of the bounded-insert evidence in their order and the client V2 scope first, the progress-header row as `derived (mechanism, not measured)`, and the table of what this release closes from the gg6 note, with the gg6 note itself untouched).

```sh
python3 -I .planning/phases/44-release-publication/44-note-check.py rows docs/ROLLOUT-v1.7.0_gg7.md --only F T D
python3 -I .planning/phases/44-release-publication/44-note-check.py labels|forbidden|claims docs/ROLLOUT-v1.7.0_gg7.md
python3 -I <order check: the T IDs in the note in the order T1..T15; the ten section B tokens present>
git diff --quiet 9a5209c17e5380ec1f675cd8b3225250d3ff41fe HEAD -- docs/ROLLOUT-v1.7.0_gg6.md
python3 -I .planning/phases/44-release-publication/44-hygiene-check.py --strict docs/ROLLOUT-v1.7.0_gg7.md
python3 -I .planning/phases/44-release-publication/44-evidence-check.py secrets docs/ROLLOUT-v1.7.0_gg7.md
git add docs/ROLLOUT-v1.7.0_gg7.md && git commit -F /tmp/gsd44/exec/msg-n2.txt
```

```text
nc_rows_ftd: OK rows=25 measured=16 code=15 derived=2 mechanism=2
t_order: yes
nc_labels: OK labels: 4 of 4 defined
nc_forbidden: OK forbidden: 0 lines
nc_claims: OK claims: 0 hex strings outside the allowed set
section_b_tokens: yes
gg6_note_untouched: yes
t7_token_on_default_path: yes
t12_label_split: yes
hygiene_note: clean
secrets_scan: clean
note_commit_2: 38b26281f0556350b777313231093c036870ddb4
```

Every row was checked against its source before it was written. Two rows are worded differently from the planning inventory because the code does not support the inventory's wording. Row T7: the planning text said no deduplication token is sent without `exactlyOnce`. The code sends one on the default path: `Processing.doLogic` runs for `exactlyOnce=false` too (with the in-memory state) and builds a `QueryIdentifier` with partition and offsets, so each attempt of a redelivered batch carries the same `insert_deduplication_token`; only `ignorePartitionsWhenBatching=true` sends none. The row therefore says the batch is inserted again, that a duplicate follows in a table that does not deduplicate, that a table deduplicating for the token may drop the repeat, and labels the token claim `read from the code` and the rest `derived`. Row T12: the three `future.get()` sites are read from the code and only the 30.1 s bound was measured, so the label is split instead of `measured`. Row T2 quotes the 304 to 432 ms timings as a probe on a scratch copy of the change, which is what they were. The statements in the maintainer documents (FORK-DELTA item 14 and the changelog section) say the batch can be inserted twice, which the new wording does not contradict; the earlier planning sentence about the token is the only one that does.

2026-11-02T21:24:26Z

Verdict: pass

### E-07: Note complete: 33 labelled rows, the verbatim WARN, FUT-05 stays open, checker and six mutations, links

Backs: RLS-03 (sections C to F with rows R1 to R8, the start-up WARN quoted verbatim from the source on one line, the line `FUT-05 stays open.` in section F and nowhere else, the known limits, the full checker run with six mutations that each fail, every relative link of the note, the changelog and the maintainer document resolving, and no fact about the release object).

```sh
python3 -I .planning/phases/44-release-publication/44-note-check.py all docs/ROLLOUT-v1.7.0_gg7.md --allow 72e8ccffe05ff6dffb14098d0b8c0f67aaa6a236 e745e7d5363fca8067ddfcc80f3ebea034b93abb4f2912e514492201a729c2af
python3 -I .planning/phases/44-release-publication/44-note-check.py rows docs/ROLLOUT-v1.7.0_gg7.md
python3 -I .planning/phases/44-release-publication/44-note-check.py fut docs/ROLLOUT-v1.7.0_gg7.md
python3 -I /tmp/gsd44/exec/mut7.py docs/ROLLOUT-v1.7.0_gg7.md /tmp/gsd44/exec   # six scratch copies
python3 -I .planning/phases/44-release-publication/44-note-check.py rows|forbidden|fut|claims <each scratch copy>
grep -cF <the warn text of E-05> docs/ROLLOUT-v1.7.0_gg7.md
python3 -I .planning/phases/44-release-publication/44-doc-check.py links docs/ROLLOUT-v1.7.0_gg7.md CHANGELOG.md docs/FORK-DELTA.md
python3 -I <release object scan: no v1.7.0_gg7.zip followed by a byte count, no releases/tag/v1.7.0_gg7, no digest other than the gg6 asset's>
git diff --name-only 9a5209c17e5380ec1f675cd8b3225250d3ff41fe HEAD -- . ':(exclude).planning'
python3 -I .planning/phases/44-release-publication/44-hygiene-check.py --strict docs/ROLLOUT-v1.7.0_gg7.md
python3 -I .planning/phases/44-release-publication/44-evidence-check.py secrets docs/ROLLOUT-v1.7.0_gg7.md
git add docs/ROLLOUT-v1.7.0_gg7.md && git commit -F /tmp/gsd44/exec/msg-n3.txt
```

```text
nc_all: OK claims: 0 hex strings outside the allowed set
nc_rows: OK rows=33 measured=23 code=18 derived=3 mechanism=2
nc_fut: OK fut: 'FUT-05 stays open.' stands in section F and FUT-05 appears nowhere else
nc_mutations: 6 of 6 failed as expected
warn_verbatim: yes
links_all: OK links: 32 relative targets checked, 0 broken
release_object_facts: none
scope_outside_planning: CHANGELOG.md; VERSION; docs/FORK-DELTA.md; docs/ROLLOUT-v1.7.0_gg7.md
hygiene_note: clean
secrets_scan: clean
note_commit_3: 7b6640fdfe30532eb03224bc6bd93a5f9915f4b0
```

The full checker run passed all five checks (rows, labels, forbidden, fut, claims) with exit 0. The six mutations were a deleted T6 row and a T6 label set to `measured` (both fail `rows`), a line naming a planning identifier in section A and a line with a destructive SQL word (both fail `forbidden`), the deleted `FUT-05 stays open.` line (fails `fut`) and a 40-hex string (fails `claims`). The only 64-hex string in the note is the digest of the published gg6 asset, which is a fact about a release that exists. The three relative links that were broken at the end of plan 44-01 (to this note, from the changelog and from the maintainer document) now resolve. The rows were checked against their sources before writing: the WARN text equals the string constant of `ClickHouseSinkTask` joined from its literals (E-05 `warn_text`); the vetted set is the 15 codes of `Utils.DEFAULT_RETRIABLE_ERROR_CODES` plus 216 (E-05 `vetted_codes`); `Processing.java` differs from `v1.7.0_gg6` by exactly the line of row R1; `StateRecord.getOriginalTopic` is read only in `BaseStateProviderImpl.onStateUpdate`, so the claim that only the in-memory inserted-offset map reads the consumer's topic holds; `KeeperStateProvider.setStateRecord` writes no original topic. No row had a weaker source than the planning inventory expected, apart from the two wording corrections recorded in E-06 and the rebuild-versus-asset correction recorded in E-05.

2026-11-02T21:27:19Z

Verdict: pass

### E-08: Review round 1 of the release documents by a fresh gsd-code-reviewer

Backs: RLS-03 (round 1 of the independent review of the release documents: a fresh-context gsd-code-reviewer spawned by the orchestrator with the Agent tool, the brief it ran on, the schema-valid 44-REVIEW.md and its commit).

```sh
python3 -I .planning/phases/44-release-publication/44-review-check.py round .planning/phases/44-release-publication/44-REVIEW.md --diff-base 72e8ccffe05ff6dffb14098d0b8c0f67aaa6a236 --files <the five files> --tokens <the eleven tokens>
python3 -I .planning/phases/44-release-publication/44-review-check.py agents .planning/phases/44-release-publication/44-REVIEW.md --expect-ids a37f170dc8cc7a801
git add .planning/phases/44-release-publication/44-REVIEW.md
git commit -m "docs(44-03): review round 1 of the release documents (RLS-03)"
git rev-parse HEAD
```

```text
reviewer_agent: round 1 agentId=a37f170dc8cc7a801 (gsd-code-reviewer spawned by the orchestrator with the Agent tool, fresh context)
agents_check: OK agents: rounds=1 distinct=1 ids=a37f170dc8cc7a801
brief_files: docs/ROLLOUT-v1.7.0_gg7.md; CHANGELOG.md; docs/FORK-DELTA.md; docs/ROLLOUT-v1.7.0_gg6.md; docs/ROLLOUT-v1.7.0_gg1.md
brief_path: /tmp/gsd44/review/brief-r1.md
round1_findings: 14
review_commit: e02adb0c7ab638b171e6768a525da537dc3a38fd
schema_check: OK round: findings=14 rounds=1
```

The round has 4 warnings and 10 info findings, no critical one. The orchestrator spawned the reviewer and copied the returned agentId into the round section; the executor that recorded this entry did not review. A round with findings is a pass here: the verdict is about the process.

2026-11-02T21:43:34Z

Verdict: pass

### E-09: Review rounds and fixes until a round found nothing

Backs: RLS-03 (rounds 2 to 4 of the independent review, each by a fresh gsd-code-reviewer spawned by the orchestrator over the previous round's fix commits; every fix commit of the four rounds; the re-proofs of the fixes that touched a command, number, label or rollback step; the two user answers at gate G1; the owning checks after the last fix). The round did not end clean: the fourth round had two info findings, and the user chose to fix them and accept without a fifth round.

```sh
python3 -I .planning/phases/44-release-publication/44-review-check.py agents .planning/phases/44-release-publication/44-REVIEW.md --expect-ids a37f170dc8cc7a801,a752798ab528d2b1f,a401c814e0b118957,a32f740ad20019e8a
python3 -I .planning/phases/44-release-publication/44-review-check.py rounds .planning/phases/44-release-publication/44-REVIEW.md .planning/phases/44-release-publication/44-REVIEW-DISPOSITION.md
git branch --contains f95f9e9c
git log --first-parent --merges --oneline custom_build -4
# IN-14: three edits of docs/ROLLOUT-v1.7.0_gg7.md, then
python3 -I .planning/phases/44-release-publication/44-note-check.py all docs/ROLLOUT-v1.7.0_gg7.md --allow 72e8ccffe05ff6dffb14098d0b8c0f67aaa6a236 e745e7d5363fca8067ddfcc80f3ebea034b93abb4f2912e514492201a729c2af
python3 -I .planning/phases/44-release-publication/44-doc-check.py links docs/ROLLOUT-v1.7.0_gg7.md CHANGELOG.md docs/FORK-DELTA.md
bash <the verify block of plan 44-01 Task 2>   # prints P44-01-T2-OK
bash <the verify block of plan 44-01 Task 3>   # prints P44-01-T3-OK
bash <the verify block of plan 44-02 Task 3>   # prints P44-02-T3-OK
python3 -I .planning/phases/44-release-publication/44-hygiene-check.py --strict docs/ROLLOUT-v1.7.0_gg7.md
python3 -I .planning/phases/44-release-publication/44-evidence-check.py anchors .planning/scripts/fork-anchors.txt --rows 190
git add docs/ROLLOUT-v1.7.0_gg7.md && git commit   # a43ff32f, IN-14
```

```text
rounds: 4
reviewer_agent: round 2 agentId=a752798ab528d2b1f; round 3 agentId=a401c814e0b118957; round 4 agentId=a32f740ad20019e8a
agents_check: OK agents: rounds=4 distinct=4 ids=a37f170dc8cc7a801,a752798ab528d2b1f,a401c814e0b118957,a32f740ad20019e8a
round_findings: 1:14; 2:2; 3:2; 4:2
fix_commits: 4bdc105b; 01eddcd0; 8b28fa55; fa57386f; 733ac3ab; f5498829; 277334e3; 15716dd2; 5b7cc337; 6390da76; fdcd96cd; f014c329; f88fa7a5; e7be1edd; c5362ecd; 71fe00e9; f95f9e9c; 06be77ce; a43ff32f
fix_wr_01_reproof: the T5 label split was re-read against 41-COMMAND-EVIDENCE.md E-06: the table held 4000 rows after both runs, and the 8000 ms control run wrote 2000, so the failed 3000 ms insert's share (2000) is derived, not measured
fix_wr_02_reproof: the R1 and R4 labels were split by claim; the tests assert that Utils.handleException throws a RetriableException and do not run Kafka Connect, so the redelivery and the writer sites are labelled read from the code
fix_wr_03_reproof: the exclusion exactlyOnce=false together with ignorePartitionsWhenBatching=true was read in DirectDeliveryStrategy.preCommit and in Processing.doLogic (both return Connect's own offsets or insert without state before reportInsertedOffsets is looked at)
fix_wr_04_reproof: the universal about every helper client was reworded to the clients a started task holds, with row F5 listing the residuals; the CHANGELOG line length check (125 columns) was re-run on the reworded intro
fix_in_04_reproof: the tag-pair commands were re-run for src/main between v1.7.0_gg6 and HEAD and print 8 files changed, 194 insertions, 22 deletions, the figure the note quotes
fix_in_10_reproof: the rollback block was rehearsed on a dry-run merge based on 7ef33b4f (E-05) and re-run later on a dry-run merge of f95f9e9c (see fix_in_12_reproof): merge exit 0, tree equal, clean tree, block output ok, VERSION v1.7.0_gg6, note absent, tree equal to the tag, revert restores the tree
fix_wr_05_reproof: read in the code, not measured: ClickHouseWriter.java line 102 (the writer builds its helper client through the protected factory) and the awaitTermination wait at line 198
fix_in_11_reproof: read in the code: DirectDeliveryStrategy.java line 39 and Processing.java line 143 (the exclusion is the combination only, bufferCount 0 carried)
fix_rollback_step1_reproof: commit 71fe00e9 reworded rollback step 1 to say the dry run measured a clean tree and does not run git fetch or the fast-forward merge; the wording was checked against the clean-tree figure of E-05 (d0_status_before_block: clean) and against the same code reads as fix_in_11_reproof
fix_wr_06_reproof: read in the code: ClickHouseHelperClient lines 110 and 111 (client V2 is assigned before client V1, so a throw from the second leaves a V2 client open) and ClickHouseWriter lines 102 and 1833; the row now says three of the five residuals of Known cost 13
fix_in_12_reproof: the rollback rehearsal was re-run on a dry-run merge of f95f9e9c onto 72e8ccff after the note named that commit; the key lines of both runs follow the fence (attr run: merge_exit 0, rehearsal_parents 72e8ccffe05ff6dffb14098d0b8c0f67aaa6a236 f95f9e9cb5859f7a77cc7cfc32d72d67686ec64c, rehearsal_tree_equal yes, d0_status_before_block clean, rollback_exit 0, rollback_output ok, gradlew_bat_trap no, rollback_status_after empty, rollback_version v1.7.0_gg6, rollback_note_absent yes, rollback_tree_equals_tag yes, revert_restores_tree yes, rehearsal_cleaned yes; plain run: the same keys with revert_plain_restores_tree yes and plain_switch_exit 0)
fix_in_13_reproof: IN-13 is closed by this record: E-05's rehearsal of 7ef33b4f is superseded for the wording of the note, which names f95f9e9c, and the rehearsal on f95f9e9c is recorded in fix_in_12_reproof and in the two raw outputs below the fence; branch facts: git branch --contains f95f9e9c prints only sync/v1.7.0
fix_in_14_reproof: the note now names the release branch sync/v1.7.0 where it points at f95f9e9c; the claim that the commit stays reachable from custom_build after the release rests on the earlier releases being merge commits of that branch (git log --first-parent --merges custom_build lists 3a3a15a9 and 21c51598, both Merge sync/v1.7.0: release v1.7.0_gg5 and v1.7.0_gg4), which is the release process of this phase too; the owning checks were re-run on the edited note
last_round_clean: no
round3_decision: G1 after round 3: "Fix both, then one more round (Recommended)"; G1 after round 4: "Fix both, accept without round 5 (Recommended)"
owning_checks_after_last_fix: note all ok; links ok (32 relative targets, 0 broken); CHANGELOG checks of plan 44-01 Task 2 ok (P44-01-T2-OK); FORK-DELTA checks of plan 44-01 Task 3 ok (P44-01-T3-OK); note checks of plan 44-02 Task 3 ok (P44-02-T3-OK); hygiene strict ok; anchors mirror ok (rows=190 bad=0)
rounds_check: OK rounds: [(1, 14), (2, 2), (3, 2), (4, 2)] last_clean=False decision=True
```

The orchestrator spawned the four reviewers and supplied their agentIds; the executor that wrote this entry and the disposition did not review, and it copied the ids and the two G1 answers from the orchestrator's dispatch without being able to verify them. The two answers were given by the user directly to the orchestrator on 2026-11-02. The keys use lowercase ids (`fix_in_12_reproof`) because the evidence checker's key pattern is lowercase. The rehearsal outputs of the two runs on f95f9e9c, as printed by the rehearsal script (scratch files rehearse-attr.out and rehearse-plain.out of the fix executor, copied here because scratch is not durable):

```out
# attr run
merge_exit: 0
rehearsal_parents: 72e8ccffe05ff6dffb14098d0b8c0f67aaa6a236 f95f9e9cb5859f7a77cc7cfc32d72d67686ec64c
rehearsal_tree_equal: yes
d0_status_before_block: clean
rollback_exit: 0
rollback_output: ok
gradlew_bat_trap: no
rollback_status_after: 
rollback_version: v1.7.0_gg6
rollback_note_absent: yes
rollback_tree_equals_tag: yes
revert_restores_tree: yes
rehearsal_cleaned: yes
# plain run
merge_exit: 0
rehearsal_parents: 72e8ccffe05ff6dffb14098d0b8c0f67aaa6a236 f95f9e9cb5859f7a77cc7cfc32d72d67686ec64c
rehearsal_tree_equal: yes
d0_status_before_block: clean
rollback_exit: 0
rollback_output: ok
gradlew_bat_trap: no
rollback_status_after: 
rollback_version: v1.7.0_gg6
rollback_note_absent: yes
rollback_tree_equals_tag: yes
plain_switch_exit: 0
revert_plain_restores_tree: yes
rehearsal_cleaned: yes
```

2026-11-02T22:30:18Z

Verdict: pass

### E-10: Every review finding is dispositioned and the release documents are frozen for the allow-list and the gates

Backs: RLS-03 (the disposition of all 20 findings of the four review rounds, with open 0; the frozen documents after the last fix: the non-planning path set since the phase start, the scripts scope, the absence of review text in the note, the unchanged protected paths and published notes, and the green owning checks).

```sh
git rev-list 9a5209c17e5380ec1f675cd8b3225250d3ff41fe..HEAD > /tmp/gsd44/review/phase-commits.txt
python3 -I .planning/phases/44-release-publication/44-review-check.py disposition .planning/phases/44-release-publication/44-REVIEW.md .planning/phases/44-release-publication/44-REVIEW-DISPOSITION.md --commits /tmp/gsd44/review/phase-commits.txt
python3 -I .planning/phases/44-release-publication/44-review-check.py rounds .planning/phases/44-release-publication/44-REVIEW.md .planning/phases/44-release-publication/44-REVIEW-DISPOSITION.md
git add .planning/phases/44-release-publication/44-REVIEW-DISPOSITION.md && git commit -m "docs(44-03): disposition every Phase 44 review finding (RLS-03)"
git diff --name-only 9a5209c17e5380ec1f675cd8b3225250d3ff41fe c2bf5f0d21ea7f9ae09578941bc4b81e629b290e -- . ':(exclude).planning'
git diff --name-only 9a5209c17e5380ec1f675cd8b3225250d3ff41fe c2bf5f0d21ea7f9ae09578941bc4b81e629b290e -- .planning/scripts
grep -cE '(WR|IN|CR|BL)-[0-9]{2}|^round:' docs/ROLLOUT-v1.7.0_gg7.md
git diff --quiet 9a5209c17e5380ec1f675cd8b3225250d3ff41fe HEAD -- src build.gradle.kts settings.gradle.kts gradle .github README.md LICENSE docs/ROLLOUT-v1.7.0_gg2.md docs/ROLLOUT-v1.7.0_gg3.md docs/ROLLOUT-v1.7.0_gg4.md docs/ROLLOUT-v1.7.0_gg5.md .planning/scripts/verify-merge-invariants.sh .planning/scripts/fork-anchor-exemptions.txt
git diff --quiet 9a5209c17e5380ec1f675cd8b3225250d3ff41fe HEAD -- docs/ROLLOUT-v1.7.0_gg6.md docs/ROLLOUT-v1.7.0_gg1.md
bash <the verify block of plan 44-03 Task 3>
```

```text
docs_frozen_scope: CHANGELOG.md; VERSION; docs/FORK-DELTA.md; docs/ROLLOUT-v1.7.0_gg7.md
scripts_scope: .planning/scripts/fork-anchors.txt
review_text_in_note: none
dispositions: fixed:19 rejected:0 deferred:1
disposition_check: OK disposition: findings=20 rows=20
docs_frozen: yes
pending_verdicts: none
disposition_commit: c2bf5f0d21ea7f9ae09578941bc4b81e629b290e
published_notes_changed: none
protected_paths_changed: none
deferred_finding: IN-03 (the planning identifier that Phase 43 added to the published gg6 note; owner: fork maintainer (the user); re-open trigger: the next edit of the published gg6 note; carried to the milestone close)
```

The documents are frozen at the disposition commit above. The one deferred finding is carried to the milestone close and is not a gap in the release documents. The fixes after round 4 (IN-13 and IN-14) were not re-reviewed, by the user's answer at gate G1; the owning checks were re-run after them (E-09).

2026-11-02T22:31:15Z

Verdict: pass

### E-11: The gg7 archive allow-list is generated last from the final tree and the local compare dry run passes in both polarities

Backs: RLS-02 (the allow-list `release.yaml` reads for VERSION `v1.7.0_gg7`: both archives rebuilt from clean exports of tag `v1.7.0_gg6` and of the tip, the 120 differences grouped by owner, the 18 rows derived from them, the compare green on the list and red on three mutations, the shipped-lists tests, and the list committed alone after the frozen documents).

```sh
git rev-parse HEAD
git diff --quiet c2bf5f0d21ea7f9ae09578941bc4b81e629b290e HEAD -- src build.gradle.kts settings.gradle.kts gradle README.md LICENSE config .github VERSION
TG=$(git tag --merged HEAD --list 'v*') && printf '%s\n' "${TG}" | python3 -I .github/scripts/baseline_tag.py v1.7.0_gg7
git archive v1.7.0_gg6 | tar -x -C /tmp/gsd44/w/final-base-src
git archive HEAD | tar -x -C /tmp/gsd44/w/final-tip-src
env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain createConfluentArchive   # in each export, with its own wrapper
cp <export>/build/confluent/clickhouse-kafka-connect-<version>.zip /tmp/gsd44/w/final-arch/base.zip and tip.zip
python3 -I .github/scripts/compare_confluent_archives.py /tmp/gsd44/w/final-arch/base.zip /tmp/gsd44/w/final-arch/tip.zip /tmp/gsd44/w/final-arch/empty.txt > /tmp/gsd44/exec/cmp-empty-final.out
python3 -I .planning/phases/44-release-publication/44-allowlist-check.py rows|cover ...
python3 -I .github/scripts/compare_confluent_archives.py base.zip tip.zip .planning/scripts/confluent-archive-allowlist-v1.7.0_gg7.txt > /tmp/gsd44/exec/cmp-final.out
python3 -I .github/scripts/compare_confluent_archives.py base.zip tip.zip <scratch list with a stale row | without the gson classes row | with a *.class row>
DOCKER_HOST=unix:///nonexistent/docker.sock env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain testUnit --rerun --tests '*CompareConfluentArchivesScriptUnitTest' --tests '*ReleaseWorkflowGuardUnitTest'
git add .planning/scripts/confluent-archive-allowlist-v1.7.0_gg7.txt && git commit -F /tmp/gsd44/exec/msg-a1.txt
git diff --quiet c2bf5f0d21ea7f9ae09578941bc4b81e629b290e 402b6e2ea99a65e80c42f710e2b93277c6f78c64 -- src build.gradle.kts settings.gradle.kts gradle README.md LICENSE config .github VERSION CHANGELOG.md docs
python3 -I .planning/phases/44-release-publication/44-hygiene-check.py --strict .planning/scripts/confluent-archive-allowlist-v1.7.0_gg7.txt
```

```text
tip_head: d0ab16b6984c5f06d429a9f0b1b51b89498aa302
docs_frozen_tip: c2bf5f0d21ea7f9ae09578941bc4b81e629b290e
archive_inputs_unchanged_since_freeze: yes
baseline_tag: v1.7.0_gg6
workflow_allowlist_path_matches: yes
final_base_verify_line: verifyConfluentArchive: 7933 inner entries, no nested jar, no Kafka API class
final_tip_verify_line: verifyConfluentArchive: 7932 inner entries, no nested jar, no Kafka API class
final_base_entries: 7933
final_tip_entries: 7932
entries_equal_e05: yes
final_base_bytes: 13558634
final_tip_bytes: 13564145
empty_compare_exit: 1
empty_compare_totals: totals files-a 7513 files-b 7512 only-a 1 only-b 0 changed 119 same 7393
empty_compare_unallowed: 120
empty_compare_matches_e05: yes
empty_compare_version_table: com.google.code.gson:gson 2.14.0 -> 2.15.0
empty_compare_manifest: MANIFEST.MF: differs only in Implementation-Version
owner_split: gson classes 102 (101 changed, 1 only in the base); gson metadata 3; connector classes 14; manifest 1; anything else 0
unexpected_differences: none
list_rows: OK rows=18
list_rows_count: 18
list_cover: OK differences=120 covered=120 uncovered=0 overlapping=0 stale_rows=0
two_segment_rule: ok
connector_rows_exact: yes
gson_rows: com/google/gson/*; META-INF/maven/com.google.code.gson/gson/*; META-INF/versions/9/module-info.class
header_line1_ok: yes
list_compare_exit: 0
list_compare_result: RESULT: PASS
compare_summary: exit 0, 0 unallowed, 0 stale rows, baseline v1.7.0_gg6, allow-list .planning/scripts/confluent-archive-allowlist-v1.7.0_gg7.txt
list_compare_totals: totals files-a 7513 files-b 7512 only-a 1 only-b 0 changed 119 same 7393
list_compare_manifest: MANIFEST.MF: differs only in Implementation-Version
list_compare_nested_jars: 0
list_compare_kafka_classes: 0
mutation_stale: exit=1 STALE-ROW zz/nothing/*
mutation_deleted: exit=1 102 UNALLOWED lines, the first UNALLOWED CHANGED com/google/gson/FieldNamingStrategy.class
mutation_wildcard: exit=2 ERROR: a glob must start with a literal path prefix; one that starts with a wildcard (* ? [) is not anchored to a path and is refused
research_list_equal: yes
research_difference_note: the glob set equals the 18 rows of research section 4 (three gson rows, the fourteen connector class paths of the owner table, the manifest); the list was written from this plan's UNALLOWED lines and compared afterwards
guards_exit: 0
guards_failed: OK failed: 0 failing testcases, 0 expected
compare_xml: OK junit build/test-results/testUnit class *CompareConfluentArchivesScriptUnitTest: tests=58 failures=0 errors=0 skipped=0 files=1
relguard_xml: OK junit build/test-results/testUnit class *ReleaseWorkflowGuardUnitTest: tests=38 failures=0 errors=0 skipped=0 files=1
shipped_lists_test: WR-02 every allow-list shipped under .planning/scripts loads, inside CompareConfluentArchivesScriptUnitTest, green with the gg7 list present
allowlist_commit: 402b6e2ea99a65e80c42f710e2b93277c6f78c64
allowlist_commit_files: .planning/scripts/confluent-archive-allowlist-v1.7.0_gg7.txt
generated_last: yes
hygiene_scan: clean
secrets_scan: clean
```

Both archives were rebuilt on the final tree and match the figures of E-05 (the entry counts 7933 and 7932, the compare totals, the 120 differences, every one of them a gson entry, a connector class or the manifest, and the version table changing for `gson` only). The two zips are each one byte larger than the sizes E-05 recorded (13558633 and 13564144); the cause was not established and the compare tool, which hashes entries, reads the same totals and the same 120 differences. The list holds 18 rows and no difference is left without a row or has two. The local dry run reads `exit 0, 0 unallowed, 0 stale rows`; the runner dry run of plan 44-05 is the check that counts for the release. The allow-list commit changes the one list file only, and no archive input, `CHANGELOG.md` or `docs/` path changed between the frozen documents and it.

2026-11-02T22:36:30Z

Verdict: pass

### E-12: Docker-free gates on one head: the invariant script started on it, testUnit with Docker unreachable, the anchors mirror, scope and numbers

Backs: RLS-04 (Docker-free half: the gated head and origin facts, the cumulative merge-invariant script started once on that explicit SHA, testUnit with the Docker daemon proven unreachable, the anchors mirror, the scope and number facts and the scans).

```sh
git rev-parse HEAD
git ls-remote origin refs/heads/custom_build
git rev-list --left-right --count origin/custom_build...HEAD
git ls-remote --heads origin 'sync/*'
git ls-remote --tags origin 'v1.7.0_gg7*'
git log -1 --format=%H -- . ':(exclude).planning'
git log -1 --format=%H -- .planning/scripts
/bin/bash -c 'bash .planning/scripts/verify-merge-invariants.sh 54e63648 b265f5a45a0752f314bb95315e3cdfbeaa2b78cd > /tmp/gsd44/exec/invariants.log 2>&1; echo $? > /tmp/gsd44/exec/invariants.exit'   # once, in the background
DOCKER_HOST=unix:///nonexistent/docker.sock docker info
./gradlew --stop; sysctl vm.swapusage; rm -rf build/test-results/testUnit
DOCKER_HOST=unix:///nonexistent/docker.sock env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain testUnit --rerun > /tmp/gsd44/exec/gate-testunit.log 2>&1
python3 -I .planning/phases/44-release-publication/44-evidence-check.py junit|failed build/test-results/testUnit ...
python3 -I .planning/phases/44-release-publication/44-evidence-check.py anchors .planning/scripts/fork-anchors.txt --rows 190
git diff --name-only 9a5209c17e5380ec1f675cd8b3225250d3ff41fe b265f5a45a0752f314bb95315e3cdfbeaa2b78cd -- . ':(exclude).planning'
git diff --name-only 9a5209c17e5380ec1f675cd8b3225250d3ff41fe b265f5a45a0752f314bb95315e3cdfbeaa2b78cd -- .planning/scripts
git diff --shortstat v1.7.0 b265f5a45a0752f314bb95315e3cdfbeaa2b78cd -- src build.gradle.kts gradle .github
git diff --shortstat v1.7.0 b265f5a45a0752f314bb95315e3cdfbeaa2b78cd -- src/main
git diff --shortstat v1.7.0_gg6 b265f5a45a0752f314bb95315e3cdfbeaa2b78cd -- src/main
git log --format=%h 31b1721427915576bb631e59a6c75812725fd8b0..b265f5a45a0752f314bb95315e3cdfbeaa2b78cd -- src/integrationTest
python3 -I .planning/phases/44-release-publication/44-hygiene-check.py [--strict] <every file added since the phase start, the added lines of the modified documents>
python3 -I .planning/phases/44-release-publication/44-evidence-check.py secrets <evidence, review, disposition, note>
git worktree list
```

```text
gated_head: b265f5a45a0752f314bb95315e3cdfbeaa2b78cd
utc_start: 2026-11-02T22:36:45Z
dirty_paths_at_gate: M .planning/config.json; M .planning/state.json
origin_custom_build_at_gate: 72e8ccffe05ff6dffb14098d0b8c0f67aaa6a236 refs/heads/custom_build
origin_unchanged: yes
left_count_at_gate: 0
origin_sync_ref: none
origin_gg7_tags: none
local_release_tag: none
last_non_planning_commit: a43ff32f39647dfc3afb9849f6729b251f2619d2
last_scripts_commit: 402b6e2ea99a65e80c42f710e2b93277c6f78c64
invariants_started_on: b265f5a45a0752f314bb95315e3cdfbeaa2b78cd
invariants_started_utc: 2026-11-02T22:37:10Z
invariants_exit_seen: 0
invariants_line_seen: invariants: provenance_paths=21 anchor_commits=1534 anchor_rows=190 marker_commits=1600 anchor_exempt=19 result=PASS
invariants_finished_utc: 2026-11-02T23:25:44Z
invariants_overlap: none (no Gradle build ran between the start and the finish of the script, by the operator's instruction)
docker_unreachable_proof: exit=1 failed to connect to the docker API at unix:///nonexistent/docker.sock; check if the path is correct and if the daemon is running: dial unix /nonexistent/docker.sock: connect: no such file or directory
swap_before_testunit: vm.swapusage: total = 28672.00M  used = 28044.94M  free = 627.06M  (encrypted)
testunit_exit: 0
testunit_tests: 1826
testunit_failures: 0
testunit_errors: 0
testunit_skipped: 2
testunit_expected_tests: 1826
testunit_count_matches: yes
testunit_jdk: the default test JDK line (defaultJdkVersion 17, no -PjavaVersion; the installed JDK 17 entries are IBM Semeru 17.0.9)
testunit_jvm_crash: none
failed_ok: yes
compare_xml: OK junit build/test-results/testUnit class *CompareConfluentArchivesScriptUnitTest: tests=58 failures=0 errors=0 skipped=0 files=1
bump_xml: OK junit build/test-results/testUnit class *BumpVersionScriptUnitTest: tests=52 failures=0 errors=0 skipped=0 files=1
baseline_xml: OK junit build/test-results/testUnit class *BaselineTagScriptUnitTest: tests=19 failures=0 errors=0 skipped=0 files=1
relguard_xml: OK junit build/test-results/testUnit class *ReleaseWorkflowGuardUnitTest: tests=38 failures=0 errors=0 skipped=0 files=1
mbean_xml: OK junit build/test-results/testUnit class *MBeanNameUnitTest: tests=11 failures=0 errors=0 skipped=0 files=1
closing_writer_xml: OK junit build/test-results/testUnit class *WriterHelperClientClosingUnitTest: tests=23 failures=0 errors=0 skipped=0 files=1
closing_keeper_xml: OK junit build/test-results/testUnit class *KeeperStateProviderClosingUnitTest: tests=5 failures=0 errors=0 skipped=0 files=1
stop_xml: OK junit build/test-results/testUnit class *ProxySinkTaskStopUnitTest: tests=4 failures=0 errors=0 skipped=0 files=1
stall_xml: OK junit build/test-results/testUnit class *InsertStallBoundPinUnitTest: tests=15 failures=0 errors=0 skipped=0 files=1
sockopt_xml: OK junit build/test-results/testUnit class *InsertSocketTimeoutOptionUnitTest: tests=23 failures=0 errors=0 skipped=0 files=1
origtopic_xml: OK junit build/test-results/testUnit class *OriginalTopicInsertedOffsetsUnitTest: tests=6 failures=0 errors=0 skipped=0 files=1
passthrough_xml: OK junit build/test-results/testUnit class *RetriableExceptionPassThroughUnitTest: tests=38 failures=0 errors=0 skipped=0 files=1
cycle_xml: OK junit build/test-results/testUnit class *HandleExceptionCauseCycleUnitTest: tests=2 failures=0 errors=0 skipped=0 files=1
warn_unit_xml: OK junit build/test-results/testUnit class *UnvettedRetriableCodesWarnUnitTest: tests=21 failures=0 errors=0 skipped=0 files=1
wrapped_xml: OK junit build/test-results/testUnit class *WrappedDataExceptionClassificationPinUnitTest: tests=14 failures=0 errors=0 skipped=0 files=1
anchors_mirror: rows=190 bad=0
scope_outside_planning: CHANGELOG.md; VERSION; docs/FORK-DELTA.md; docs/ROLLOUT-v1.7.0_gg7.md
scope_planning_scripts: .planning/scripts/confluent-archive-allowlist-v1.7.0_gg7.txt; .planning/scripts/fork-anchors.txt
no_java_change: yes
src_main_vs_gg6_equals_forkdelta: yes
forkdelta_numbers_equal: yes
note_archive_numbers_match: yes
archive_size_delta_cause: the outer manifest.json of each archive holds the build date (release_date 2026-11-02 in the E-05 builds, 2026-11-03 in the E-11 builds, local time); the one-digit change alters its compressed size by one byte, so the two sizes in the note's row D4 (the E-05 figures) are each one byte below a rebuild made on a later date; the entry counts in that row match
integration_test: not re-run: it needs a Connect stack; the recorded run is the one of the Phase 39 harness fix, and src/integrationTest changed after it only as git log shows in integration_sources_after_run
integration_sources_after_run: 51200928 (fix(39): WR-01, the sink task status log line kept purely diagnostic in the integration test)
hygiene_scan_all: not clean: default and strict scans of every other added file and of the added lines of the modified documents report 0 hits, but 44-REVIEW.md has 3 hits at lines 116, 204 and 244 (the helper's fifth default pattern, a bare four-letter verb beginning with st, in the reviewers' sentences)
secrets_scan: clean
nothing_pushed: yes
worktrees_clean: yes
docker_free_decision: no-go
gate_failures: hygiene_scan_all (3 hits in 44-REVIEW.md, a file plan 44-03 never scanned; this plan does not edit a document)
utc_end: 2026-11-02T23:27:31Z
```

Every Docker-free gate other than the hygiene scan holds on the gated head: testUnit with the daemon proven unreachable read 1826 tests with 0 failures, 0 errors and 2 skipped and every pin class green; the invariant script, started once on this SHA with no Gradle build overlapping it, ended `result=PASS` with `anchor_rows=190`; the anchors mirror reads `rows=190 bad=0`; the non-planning scope is the four document paths and the scripts scope the allow-list and the anchors file; the FORK-DELTA figures and the note's entry counts agree with fresh runs; origin `custom_build` still reads the gg6 commit and nothing was pushed. The one red is a hygiene hit that the plan's own scope (strict over the review file) makes a gate: the independent reviewers' round text uses a verb the helper's pattern for an outside-system name also matches, so the file fails although it names no outside system. A wording change in that planning file is outside this plan (it never edits a document) and is the orchestrator's decision. Plan 44-04 Task 3 is not started: its precondition needs E-12 to read pass. The invariant result is kept in `/tmp/gsd44/exec/invariants.exit` and `invariants.log` for it.

2026-11-02T23:27:31Z

Verdict: fail

### E-20: Re-run of the E-12 hygiene scan after the neutral rewording of three review sentences; the Docker-free decision is go

Backs: RLS-04 (E-12 read verdict fail on one gate, the strict hygiene scan of 44-REVIEW.md; this entry re-runs that scan and re-confirms the freeze rule against the current tip, and so supersedes the E-12 decision without editing it). The id E-20 is the next free number after the plan's E-19 (the Freeze rule's re-run text); the publication tail of the orchestrator continues after it.

```sh
python3 -I .planning/phases/44-release-publication/44-hygiene-check.py [--strict] <the same files as E-12>
python3 -I .planning/phases/44-release-publication/44-review-check.py agents .planning/phases/44-release-publication/44-REVIEW.md --expect-ids a37f170dc8cc7a801,a752798ab528d2b1f,a401c814e0b118957,a32f740ad20019e8a
git show 4c1f600b -U0
git diff --quiet b265f5a45a0752f314bb95315e3cdfbeaa2b78cd HEAD -- . ':(exclude).planning'
git diff --quiet b265f5a45a0752f314bb95315e3cdfbeaa2b78cd HEAD -- .planning/scripts
python3 -I .planning/phases/44-release-publication/44-evidence-check.py anchors .planning/scripts/fork-anchors.txt --rows 190
python3 -I .planning/phases/44-release-publication/44-evidence-check.py secrets <evidence, review>
git ls-remote origin refs/heads/custom_build
```

```text
supersedes: E-12 (docker_free_decision: no-go, gate_failures: hygiene_scan_all)
reviewer_hits_before: 3 hits in 44-REVIEW.md at lines 116, 204 and 244 (the helper's fifth default pattern, a bare four-letter verb beginning with st)
wording_change_commit: 4c1f600b870d6b2912d6d7ed7f8c6d4fbf3dcb68
wording_change_by: the orchestrator, by option (a) of the plan 44-04 halt; the executor did not edit the review file
wording_change_scope: three insertions and three deletions in 44-REVIEW.md, the verb replaced by a synonym; no finding, count, id or attestation line changed
reviewer_hits_after: 0 default, 0 strict
agents_check: OK agents: rounds=4 distinct=4 ids=a37f170dc8cc7a801,a752798ab528d2b1f,a401c814e0b118957,a32f740ad20019e8a
gated_head: b265f5a45a0752f314bb95315e3cdfbeaa2b78cd
tip_at_rerun: 4c1f600b870d6b2912d6d7ed7f8c6d4fbf3dcb68
tip_equals_gate_outside_planning: yes
tip_equals_gate_planning_scripts: yes
anchors_mirror: rows=190 bad=0
hygiene_scan_all: clean
hygiene_default_files: 12 files, 0 hits
hygiene_strict_files: 10 files, 0 hits
secrets_scan: clean
origin_unchanged: yes
nothing_pushed: yes
testunit_and_invariants_valid: yes (the gated head, the tree outside .planning and the tree under .planning/scripts are unchanged; E-12 keeps the testUnit totals and the invariant start, and the invariant result is read in E-13)
docker_free_decision: go
utc_end: 2026-11-02T23:29:54Z
```

The only red of E-12 is gone: the three reviewer sentences use a synonym now, both hygiene modes read 0 hits on 44-REVIEW.md and on every other scanned file, and the reviewer attestation still reads four distinct agent ids. The gated head is unchanged outside `.planning` and under `.planning/scripts`, so the testUnit result (1826 tests, 0 failures, 2 skipped), the anchors mirror and the invariant run of E-12 hold for the head the user pushes. Plan 44-04's own verify blocks name E-12 with verdict pass; they are run with E-20 read in place of E-12 for the decision and the hygiene fact, and every other E-12 key is read from E-12 unchanged.

2026-11-02T23:29:54Z

Verdict: pass

### E-13: Two full suites on one pinned ClickHouse latest with client V2, the invariant script's result, and the go decision

Backs: RLS-04 (live half: two full `--rerun` suites on ClickHouse `latest` with client V2 on one pinned image, the digest and `SELECT version()` read before run 1, after run 1 and after run 2, the invariant script's result on the gated head, and the go decision).

```sh
git diff --quiet b265f5a45a0752f314bb95315e3cdfbeaa2b78cd HEAD -- . ':(exclude).planning'
git diff --quiet b265f5a45a0752f314bb95315e3cdfbeaa2b78cd HEAD -- .planning/scripts
./gradlew --stop; sysctl vm.swapusage; colima list
docker image inspect --format '{{index .RepoDigests 0}}' clickhouse/clickhouse-server:latest
docker run --rm --entrypoint clickhouse clickhouse/clickhouse-server:latest local --query 'SELECT version()'
rm -rf build/test-results/test
CLICKHOUSE_VERSION=latest CLIENT_VERSION=V2 env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain test --rerun > /tmp/gsd44/exec/suite1.log 2>&1
python3 -I .planning/phases/44-release-publication/44-evidence-check.py junit|skips|failed build/test-results/test ...
rm -rf build/test-results/test
CLICKHOUSE_VERSION=latest CLIENT_VERSION=V2 env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain test --rerun > /tmp/gsd44/exec/suite2.log 2>&1
python3 -I .planning/phases/44-release-publication/44-evidence-check.py junit|skips|failed build/test-results/test ...
cat /tmp/gsd44/exec/invariants.exit; grep '^invariants:' /tmp/gsd44/exec/invariants.log
```

```text
gate_head_same: yes
swap_before: vm.swapusage: total = 28672.00M  used = 27932.94M  free = 739.06M  (encrypted)
colima_vz: Running
digest_before_run1: clickhouse/clickhouse-server@sha256:739e099abd1157390960a59e4532d21781d8fde57e6e4151cb920ea02c30b1ce
select_version_before_run1: 26.10.3.7
digest_same_as_e01: yes
suite1_utc_start: 2026-11-02T23:30:07Z
suite1_exit: 0
suite1_tests: 2425
suite1_failures: 0
suite1_errors: 0
suite1_skipped: 18
suite1_skips: OK skips: named=7 state_mismatch_cases=11 unexpected=0
suite1_failed_ok: yes
suite1_duration: BUILD SUCCESSFUL in 2m 53s
suite1_jdk: the default test JDK line (defaultJdkVersion 17, no -PjavaVersion)
suite1_jvm_crash: none
suite1_warn_live_xml: OK junit build/test-results/test class *UnvettedRetriableCodesWarnLiveTest: tests=1 failures=0 errors=0 skipped=0 files=1
suite1_processing_xml: OK junit build/test-results/test class *ProcessingTest: tests=14 failures=0 errors=0 skipped=0 files=1
digest_after_run1: clickhouse/clickhouse-server@sha256:739e099abd1157390960a59e4532d21781d8fde57e6e4151cb920ea02c30b1ce
select_version_after_run1: 26.10.3.7
suite2_utc_start: 2026-11-02T23:33:19Z
suite2_exit: 0
suite2_tests: 2425
suite2_failures: 0
suite2_errors: 0
suite2_skipped: 18
suite2_skips: OK skips: named=7 state_mismatch_cases=11 unexpected=0
suite2_failed_ok: yes
suite2_duration: BUILD SUCCESSFUL in 2m 45s
suite2_jdk: the default test JDK line (defaultJdkVersion 17, no -PjavaVersion)
suite2_jvm_crash: none
suite2_warn_live_xml: OK junit build/test-results/test class *UnvettedRetriableCodesWarnLiveTest: tests=1 failures=0 errors=0 skipped=0 files=1
suite2_processing_xml: OK junit build/test-results/test class *ProcessingTest: tests=14 failures=0 errors=0 skipped=0 files=1
digest_after_run2: clickhouse/clickhouse-server@sha256:739e099abd1157390960a59e4532d21781d8fde57e6e4151cb920ea02c30b1ce
select_version_after_run2: 26.10.3.7
suites_jdk: the default test JDK line (defaultJdkVersion 17, no -PjavaVersion)
suites_same_jvm: yes
suite_expected_tests: 2425
suite_count_matches: yes
suite_tests_equal: yes
docker_only_delta: 599
pinned_image_same: yes
crash_files_left: none
invariants_exit: 0
invariants_line: invariants: provenance_paths=21 anchor_commits=1534 anchor_rows=190 marker_commits=1600 anchor_exempt=19 result=PASS
invariants_head: b265f5a45a0752f314bb95315e3cdfbeaa2b78cd
invariants_finished_utc: 2026-11-02T23:25:44Z
docker_free_decision_source: E-20 (supersedes E-12, whose only red was the hygiene scan of the review file)
gate_decision: go
gate_note: The CI side (the scratch dry run, the single tests.yaml run on the merged SHA and the publish run) is the user's and not part of this gate, and integrationTest was not re-run (E-12 integration_test).
utc_start: 2026-11-02T23:30:07Z
utc_end: 2026-11-02T23:36:45Z
```

Both full suites read 2425 tests with 0 failures, 0 errors and the 18 named skips (7 disabled methods and the 11 `exactlyOnceStateMismatchTest` cases), on one build of `latest` (`26.10.3.7`, one digest at all three reads) with client V2 and the default JDK 17 line; the Docker-only delta over `testUnit` is 599, as in the research. The invariant script ran alone, before the suites, on the gated head and ended `result=PASS`. The runner-side checks are the user's.

2026-11-02T23:36:45Z

Verdict: pass

### E-14: Publication rehearsal without a push: upstream recheck 2, the merge message, a dry-run merge with its checks, the rollback on it, the date check, the approval bundle

Backs: RLS-04 (the publication path rehearsed without a push on the final tip: origin and divergence facts, the second upstream recheck, the merge message, a dry-run merge D of the tip onto 72e8ccff in a throwaway detached worktree with the note's rollback block run on it, the server and image read, the CHANGELOG date check, and the approval bundle with its gates and eight questions).

```sh
git rev-parse HEAD
git ls-remote origin refs/heads/custom_build
git rev-list --left-right --count origin/custom_build...sync/v1.7.0
git rev-list --count 72e8ccffe05ff6dffb14098d0b8c0f67aaa6a236..HEAD
git rev-list --count HEAD --not --remotes=origin
git rev-parse 'pre-v1.7.0_gg7^{commit}'; git cat-file -t pre-v1.7.0_gg7
git ls-remote --heads origin 'sync/*'; git ls-remote --tags origin 'v1.7.0_gg7*' 'pre-v1.7.0_gg7*'; git tag -l 'v1.7.0_gg7'
gh api "repos/ne1r0n/clickhouse-kafka-connect/pulls?state=open&per_page=50" --jq '[.[] | "\(.number) \(.head.sha[0:8])"] | sort | join("; ")'
git diff --shortstat 72e8ccffe05ff6dffb14098d0b8c0f67aaa6a236 HEAD -- . ':(exclude).planning'    # and -- .planning
git diff --quiet <E-12 gated_head> HEAD -- . ':(exclude).planning'; git diff --quiet <E-12 gated_head> HEAD -- .planning/scripts
git diff --quiet <E-10 disposition_commit> <E-12 gated_head> -- . ':(exclude).planning'
git ls-remote upstream refs/heads/main; git ls-remote --tags --refs upstream 'v*'
curl -s https://api.github.com/repos/ClickHouse/clickhouse-kafka-connect/pulls/<861|855|857>
curl -s https://api.github.com/repos/ClickHouse/clickhouse-kafka-connect/compare/9bed6f79...d92a1830
/bin/bash /tmp/gsd44/exec/t1-facts-a.sh        # the reads above, read-only
python3 -I /tmp/gsd44/exec/extract-rb.py docs/ROLLOUT-v1.7.0_gg7.md /tmp/gsd44/exec/rollback-block-t1.sh
git worktree add --detach /tmp/gsd44/w/pubdry 72e8ccffe05ff6dffb14098d0b8c0f67aaa6a236
git --attr-source="$(git hash-object -t tree /dev/null)" merge --no-ff -F <absolute path of 44-MERGE-MESSAGE.txt> <tip_head>    # in the worktree, demo identity
bash -e /tmp/gsd44/exec/rollback-block-t1.sh   # in the worktree
git --attr-source="$(git hash-object -t tree /dev/null)" revert --no-edit <rollback commit>    # in the worktree
git worktree remove --force /tmp/gsd44/w/pubdry; git worktree prune; git branch -D rollback-v1.7.0_gg7
/bin/bash /tmp/gsd44/exec/t1-facts-b.sh <tip_head> <absolute path of 44-MERGE-MESSAGE.txt> /tmp/gsd44/exec/rollback-block-t1.sh   # the five lines above in one script
docker run --rm --entrypoint clickhouse clickhouse/clickhouse-server:latest local --query 'SELECT version()'
docker image inspect --format '{{index .RepoDigests 0}}' clickhouse/clickhouse-server:latest
head -1 CHANGELOG.md; date -u +%F
python3 -I .planning/phases/44-release-publication/44-hygiene-check.py --strict <merge message> <bundle> <evidence>
python3 -I .planning/phases/44-release-publication/44-evidence-check.py secrets .planning/phases/44-release-publication/44-COMMAND-EVIDENCE.md
git add .planning/phases/44-release-publication/44-MERGE-MESSAGE.txt .planning/phases/44-release-publication/44-APPROVAL-BUNDLE.md && git commit -F /tmp/gsd44/exec/msg-t1a.txt
```

```text
tip_head: 45cbc9db5bba2e5f367eff4bb7043251587e44e2
origin_custom_build: 72e8ccffe05ff6dffb14098d0b8c0f67aaa6a236 refs/heads/custom_build
origin_unchanged: yes
left_count: 0
left_right: 0 261
ahead_of_gg6: 261
unpublished_commits: 259
pre_tag_local: yes (commit at 72e8ccffe05ff6dffb14098d0b8c0f67aaa6a236)
origin_sync_ref: none
origin_gg7_tags: none
local_release_tag: none
pr_heads: 21 14be67ce; 22 b5cccbd3
pr_heads_unchanged: yes
non_planning_vs_gg6: 40 files changed, 3167 insertions(+), 146 deletions(-)
planning_vs_gg6: 301 files changed, 30107 insertions(+), 1427 deletions(-)
gated_sha: a43ff32f39647dfc3afb9849f6729b251f2619d2
gated_head: b265f5a45a0752f314bb95315e3cdfbeaa2b78cd
tip_equals_gate_outside_planning: yes
frozen_docs_equal_gate: yes
upstream_main_end: d92a1830113a5f9c1cf49bf67f65ec6d188d1391
upstream_newest_tag_end: v1.7.0
upstream_tag_past_v170_end: no
upstream_pr_861_end: open False 3d4cfdf2
upstream_pr_855_end: open False ddabf28b
upstream_pr_857_end: open False 11d88bc0
upstream_main_moved: yes: 9bed6f7970670a55c66be8efda246889e1f95bd0 (E-01) to d92a1830113a5f9c1cf49bf67f65ec6d188d1391, 6 commits, ahead_by 6 behind_by 0, merged 2026-11-01 to 2026-11-02T23:29:30Z; files changed: .github/workflows/tests.yaml, gradle/libs.versions.toml
upstream_changed_since_e01: yes: main moved (dependency bumps spotless 8.10.4 and commons-lang3 3.21.0, cloud-test secrets in tests.yaml); newest tag, tags past v1.7.0 and pull requests 861, 855 and 857 unchanged; not a STOP (a bundle item and information for the next sync)
merge_subject: Merge sync/v1.7.0: release v1.7.0_gg7
merge_trailer_last: yes
d_commit: 0209fb6a218dc5e669e2f108fd73e981ef012457
d_parents: 72e8ccffe05ff6dffb14098d0b8c0f67aaa6a236 45cbc9db5bba2e5f367eff4bb7043251587e44e2
d_tree_equal: yes
d_subject: Merge sync/v1.7.0: release v1.7.0_gg7
d_no_gsd_ws_arg: yes
d_version: v1.7.0_gg7
d_allowlist_present: yes
d_baseline_tag: v1.7.0_gg6
d_no_gg7_tag_merged: yes
d_operator_doc_check: none
d_rollback_exit: 0
d_rollback_output: ok
d_rollback_version: v1.7.0_gg6
d_rollback_note_absent: yes
d_rollback_tree_equals_tag: yes
d_gradlew_bat_trap: no
d_revert_restores_tree: yes
d_cleaned: yes
local_custom_build_ref_unmoved: yes
select_version: 26.10.3.7
image_digest: clickhouse/clickhouse-server@sha256:739e099abd1157390960a59e4532d21781d8fde57e6e4151cb920ea02c30b1ce
image_same_as_e13: yes
changelog_heading_date: 2026-11-02
utc_today: 2026-11-02 (read at 2026-11-02T23:41:23Z)
changelog_date_vs_today: equal (2026-11-02) at 2026-11-02T23:41:23Z; the UTC date flips at 2026-11-03T00:00Z, so at every later read, and at any merge, the heading differs; the user decides at bundle question 5 and again before the push of M
changelog_read_by_tooling: no: no workflow, script or test under .github or src/test refers to CHANGELOG.md
message_check: yes
bundle_items: yes
bundle_gates: yes
hygiene_scan: clean
secrets_scan: clean
anchors_mirror: rows=190 bad=0
bundle_commit: adf06e15dacbe2bb08f128621d06fd272aab8520
```

D was built from the committed merge message, with parents 72e8ccff and the tip, and removed; it was never pushed and the local custom_build ref did not move. The upstream recheck found that upstream main moved by six commits since E-01 (dependency bumps and the cloud-test secrets in tests.yaml, no source change); nothing else changed. The task's CHANGELOG date check reads equal at the moment of measurement, and the bundle states that the date flips minutes later and asks the user directly. The plan's verify clause for plan 44-05 Task 1 was run as written; the evidence helper's rows check accepts E-20 beside E-01 to E-14 (E-20 is the superseding hygiene re-run of plan 44-04).

2026-11-02T23:44:09Z

Verdict: pass

### E-15: Handoff U1 and U2 (scratch push and release dry run) is a user-gated handoff

Backs: RLS-04 (the user-run origin step, as a document: numbered pre-checks with expected values, the exact command, the expected result, the fields to report, abort and contingency rules, a clean-up and a resume path; the step itself is the user's and has not happened).

```sh
python3 -I .planning/phases/44-release-publication/44-hygiene-check.py --strict .planning/phases/44-release-publication/44-USER-DISPATCH-1-SCRATCH-DRYRUN.md
grep -E 'git push[^#]*(--force|--tags|--all|--mirror)' .planning/phases/44-release-publication/44-USER-DISPATCH-1-SCRATCH-DRYRUN.md    # no match outside a never sentence
grep -E '^[[:space:]]*git push[^#]*[^-]v1\.7\.0_gg7' .planning/phases/44-release-publication/44-USER-DISPATCH-1-SCRATCH-DRYRUN.md      # no match
grep -E '^[[:space:]]*gh (release|pr|issue|run)[[:space:]]+(create|delete|edit|merge|close|comment|review|cancel|rerun)' .planning/phases/44-release-publication/44-USER-DISPATCH-1-SCRATCH-DRYRUN.md    # no match
git ls-remote origin refs/heads/custom_build
```

```text
handoff_file: .planning/phases/44-release-publication/44-USER-DISPATCH-1-SCRATCH-DRYRUN.md
handoff_hygiene: clean
pre_checks_listed: 9
expected_values_given: yes
abort_rules: yes
resume_path: yes
dangerous_commands: none
nothing_pushed: yes
origin_custom_build_now: 72e8ccffe05ff6dffb14098d0b8c0f67aaa6a236 refs/heads/custom_build
user_report: not yet received
```

The handoff is a document only. Its step is the user's (or, under the user's answer to bundle question (1), the orchestrator's on the user's direct word), and this record stays pending and is never edited.

2026-11-02T23:47:32Z

Verdict: pending (user-gated: the push and the dispatch are the user's)

### E-16: Handoff U3 (push of M to custom_build) is a user-gated handoff

Backs: RLS-04 (the user-run origin step, as a document: numbered pre-checks with expected values, the exact command, the expected result, the fields to report, abort and contingency rules, a clean-up and a resume path; the step itself is the user's and has not happened).

```sh
python3 -I .planning/phases/44-release-publication/44-hygiene-check.py --strict .planning/phases/44-release-publication/44-USER-DISPATCH-2-CUSTOM-BUILD-PUSH.md
grep -E 'git push[^#]*(--force|--tags|--all|--mirror)' .planning/phases/44-release-publication/44-USER-DISPATCH-2-CUSTOM-BUILD-PUSH.md    # no match outside a never sentence
grep -E '^[[:space:]]*git push[^#]*[^-]v1\.7\.0_gg7' .planning/phases/44-release-publication/44-USER-DISPATCH-2-CUSTOM-BUILD-PUSH.md      # no match
grep -E '^[[:space:]]*gh (release|pr|issue|run)[[:space:]]+(create|delete|edit|merge|close|comment|review|cancel|rerun)' .planning/phases/44-release-publication/44-USER-DISPATCH-2-CUSTOM-BUILD-PUSH.md    # no match
git ls-remote origin refs/heads/custom_build
```

```text
handoff_file: .planning/phases/44-release-publication/44-USER-DISPATCH-2-CUSTOM-BUILD-PUSH.md
handoff_hygiene: clean
pre_checks_listed: 7
expected_values_given: yes
abort_rules: yes
resume_path: yes
dangerous_commands: none
nothing_pushed: yes
origin_custom_build_now: 72e8ccffe05ff6dffb14098d0b8c0f67aaa6a236 refs/heads/custom_build
user_report: not yet received
```

The handoff is a document only. Its step is the user's (or, under the user's answer to bundle question (1), the orchestrator's on the user's direct word), and this record stays pending and is never edited.

2026-11-02T23:47:32Z

Verdict: pending (user-gated: the push and the dispatch are the user's)

### E-17: Handoff U4 (the publish dispatch) is a user-gated handoff

Backs: RLS-04 (the user-run origin step, as a document: numbered pre-checks with expected values, the exact command, the expected result, the fields to report, abort and contingency rules, a clean-up and a resume path; the step itself is the user's and has not happened).

```sh
python3 -I .planning/phases/44-release-publication/44-hygiene-check.py --strict .planning/phases/44-release-publication/44-USER-DISPATCH-3-PUBLISH.md
grep -E 'git push[^#]*(--force|--tags|--all|--mirror)' .planning/phases/44-release-publication/44-USER-DISPATCH-3-PUBLISH.md    # no match outside a never sentence
grep -E '^[[:space:]]*git push[^#]*[^-]v1\.7\.0_gg7' .planning/phases/44-release-publication/44-USER-DISPATCH-3-PUBLISH.md      # no match
grep -E '^[[:space:]]*gh (release|pr|issue|run)[[:space:]]+(create|delete|edit|merge|close|comment|review|cancel|rerun)' .planning/phases/44-release-publication/44-USER-DISPATCH-3-PUBLISH.md    # no match
git ls-remote origin refs/heads/custom_build
```

```text
handoff_file: .planning/phases/44-release-publication/44-USER-DISPATCH-3-PUBLISH.md
handoff_hygiene: clean
pre_checks_listed: 4
expected_values_given: yes
abort_rules: yes
resume_path: yes
dangerous_commands: none
nothing_pushed: yes
origin_custom_build_now: 72e8ccffe05ff6dffb14098d0b8c0f67aaa6a236 refs/heads/custom_build
user_report: not yet received
```

The handoff is a document only. Its step is the user's (or, under the user's answer to bundle question (1), the orchestrator's on the user's direct word), and this record stays pending and is never edited.

2026-11-02T23:47:32Z

Verdict: pending (user-gated: the push and the dispatch are the user's)

### E-18: Handoff U5 (rollback tag, scratch deletion, pull requests) is a user-gated handoff

Backs: RLS-04 (the user-run origin step, as a document: numbered pre-checks with expected values, the exact command, the expected result, the fields to report, abort and contingency rules, a clean-up and a resume path; the step itself is the user's and has not happened).

```sh
python3 -I .planning/phases/44-release-publication/44-hygiene-check.py --strict .planning/phases/44-release-publication/44-USER-DISPATCH-4-PRETAG-CLEANUP.md
grep -E 'git push[^#]*(--force|--tags|--all|--mirror)' .planning/phases/44-release-publication/44-USER-DISPATCH-4-PRETAG-CLEANUP.md    # no match outside a never sentence
grep -E '^[[:space:]]*git push[^#]*[^-]v1\.7\.0_gg7' .planning/phases/44-release-publication/44-USER-DISPATCH-4-PRETAG-CLEANUP.md      # no match
grep -E '^[[:space:]]*gh (release|pr|issue|run)[[:space:]]+(create|delete|edit|merge|close|comment|review|cancel|rerun)' .planning/phases/44-release-publication/44-USER-DISPATCH-4-PRETAG-CLEANUP.md    # no match
git ls-remote origin refs/heads/custom_build
```

```text
handoff_file: .planning/phases/44-release-publication/44-USER-DISPATCH-4-PRETAG-CLEANUP.md
handoff_hygiene: clean
pre_checks_listed: 5
expected_values_given: yes
abort_rules: yes
resume_path: yes
dangerous_commands: none
nothing_pushed: yes
origin_custom_build_now: 72e8ccffe05ff6dffb14098d0b8c0f67aaa6a236 refs/heads/custom_build
user_report: not yet received
```

The handoff is a document only. Its step is the user's (or, under the user's answer to bundle question (1), the orchestrator's on the user's direct word), and this record stays pending and is never edited.

2026-11-02T23:47:32Z

Verdict: pending (user-gated: the push and the dispatch are the user's)

### E-19: The phase closes locally and stops: RLS-01 to RLS-03 satisfied, RLS-04 gated and pending the user's direct approval, nothing pushed

Backs: RLS-01 to RLS-04 (the local close of Phase 44: the requirement status, the deferred must-haves of ROADMAP criterion 5, the wording changes for the orchestrator, the carried items, and the final no-push and Freeze-rule facts at the close).

```sh
git ls-remote origin refs/heads/custom_build
git ls-remote --heads origin 'sync/*'; git ls-remote --tags origin 'v1.7.0_gg7*' 'pre-v1.7.0_gg7*'
git tag -l 'v1.7.0_gg7'; git rev-parse 'pre-v1.7.0_gg7^{commit}'
git diff --quiet b265f5a45a0752f314bb95315e3cdfbeaa2b78cd HEAD -- . ':(exclude).planning'
git diff --quiet b265f5a45a0752f314bb95315e3cdfbeaa2b78cd HEAD -- .planning/scripts
git diff --name-only 9a5209c17e5380ec1f675cd8b3225250d3ff41fe HEAD -- . ':(exclude).planning'
git diff --name-only 9a5209c17e5380ec1f675cd8b3225250d3ff41fe HEAD -- .planning/scripts
git show-ref refs/gsd/release-m-v1.7.0_gg7
git worktree list
python3 -I .planning/phases/44-release-publication/44-hygiene-check.py --strict .planning/phases/44-release-publication/44-LOCAL-CLOSE-RECORD.md .planning/phases/44-release-publication/44-COMMAND-EVIDENCE.md
python3 -I .planning/phases/44-release-publication/44-evidence-check.py secrets .planning/phases/44-release-publication/44-COMMAND-EVIDENCE.md
python3 -I .planning/phases/44-release-publication/44-evidence-check.py anchors .planning/scripts/fork-anchors.txt --rows 190
```

```text
local_complete: yes
requirements_status: RLS-01=satisfied; RLS-02=satisfied; RLS-03=satisfied; RLS-04=pending
pending_user_steps: G3; U1; U2; G4; U3; G5; U4; U5
deferred_must_haves_listed: 6
wording_changes_listed: 5
carried_items_listed: 16
origin_custom_build_final: 72e8ccffe05ff6dffb14098d0b8c0f67aaa6a236 refs/heads/custom_build
nothing_pushed: yes
no_gg7_tag_anywhere: yes
pre_tag_local_only: yes
scope_outside_planning: CHANGELOG.md; VERSION; docs/FORK-DELTA.md; docs/ROLLOUT-v1.7.0_gg7.md
scope_planning_scripts: .planning/scripts/confluent-archive-allowlist-v1.7.0_gg7.txt; .planning/scripts/fork-anchors.txt
no_protected_change: yes
tip_equals_gate_outside_planning: yes
index_rows: E-01..E-19 present; E-15..E-18 pending
index_note: E-20 (the plan 44-04 hygiene re-run that supersedes E-12) is also present, so the index helper's contiguous check up to E-19 reads one row more; the publication tail is recorded from E-21
hygiene_scan_all: clean
secrets_scan: clean
worktrees_clean: yes
final_head: 9d5c8d0de54db1043eb846d82cbcd9e0293f0d18
ready_for_approval: yes
close_commit: 9d5c8d0de54db1043eb846d82cbcd9e0293f0d18
```

The phase is complete locally and stops here. The bundle, the four handoffs and the runbook exist; the merge, the runs and the tag are the user's and the orchestrator's after the user's direct approval. The verdict is pass for the local gate only.

2026-11-02T23:48:57Z

Verdict: pass

### E-21: The T6 progress-header claim measured on a real ClickHouse; row T6, Known cost 14 and the CHANGELOG now say measured; review round 5 and its fixes

Backs: RLS-03 (re-open of plan 44-03 at the user's G3 answer "Measure the T6 progress-header claim first"; this entry supersedes the E-09 statement that T6 stays `derived (mechanism, not measured)` without editing it).

```sh
docker run -d -p 18123:8123 -e CLICKHOUSE_SKIP_USER_SETUP=1 clickhouse/clickhouse-server:latest
java -cp "t6:$CP" ProbeT6   # /tmp/gsd44/ProbeT6.java; connector helper client V2, body insert of 4000 rows
python3 -I .planning/phases/44-release-publication/44-note-check.py all docs/ROLLOUT-v1.7.0_gg7.md --allow 72e8ccffe05ff6dffb14098d0b8c0f67aaa6a236 e745e7d5363fca8067ddfcc80f3ebea034b93abb4f2912e514492201a729c2af
python3 -I .planning/phases/44-release-publication/44-review-check.py rounds .planning/phases/44-release-publication/44-REVIEW.md .planning/phases/44-release-publication/44-REVIEW-DISPOSITION.md
```

```text
server: 26.10.3.7, image digest sha256:739e099abd1157390960a59e4532d21781d8fde57e6e4151cb920ea02c30b1ce
shape: table dst(id UInt32, s String) Memory with a materialized view to a Null table using sleepEachRow(0.002); async_insert=0; wait_end_of_query=1; input_format_skip_unknown_fields=1; http_headers_progress_interval_ms=50; block settings for about one block per second
control_no_timeout_progress_on: 8106 ms OK
bound_3000ms_progress_on: 8141 ms OK (escaped the bound)
bound_3000ms_progress_off: 3007 ms FAIL DataTransferException>SocketTimeoutException
bound_3000ms_server_default_async_insert: 89 ms rejected by the server (TOO_SLOW sleep limit), not measured
curl_progress_headers: 8 over about 8 s
first_probe_without_wait_end_of_query: 8093 ms OK escaped; 3003 ms fail without progress
doc_commits: 14f4567e, 5b25c18c
review_round_5_agent: a87ce913aac02a00b
review_round_5_findings: WR-07 WR-08 IN-15 IN-16 IN-17, all fixed, not re-reviewed (round 6 needs the user's direct word)
disposition_commit: 8188a9ac
note_check: rows=33 measured=24 code=21 derived=4 mechanism=1, all OK
rounds_check: OK, five rounds
```

The T6 claim is measured for the shape above. The documents changed outside `.planning`, so the Freeze rule applies: the E-14 rehearsal, the E-13 suites and the approval bundle are void for the new head until plans 44-04 and 44-05 are re-run.

2026-11-03T09:40:00Z

Verdict: pass

### E-22: integrationTest re-run on the changed tree: ClickHouseSinkConnectorIntegrationTest green

Backs: RLS-04 (the user's G3 answer "Re-run integrationTest first").

```sh
env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain integrationTest --tests "*ClickHouseSinkConnectorIntegrationTest" -PjavaVersion=21 --rerun-tasks
```

```text
head: 14f4567e70814d4bd245aaec1fe398d403d4bb2a (docs-only change since the last src change; src unchanged)
first_attempt: failed in 16 s, the default OpenJ9 17 test JVM could not load class version 65 (environment, not a test result)
second_attempt: BUILD SUCCESSFUL in 30m 52s, exit 0
tests: 55 passed, 0 failed, 0 skipped, 0 errors
```

The nightly upstream workflow stays red by inheritance and is unrelated.

2026-11-03T10:15:00Z

Verdict: pass

### E-23: Re-run of plan 44-04 Task 1 on the new head: the gg7 archive allow-list regenerated last from the final tree after the T6 documents; the row set is unchanged and the compare dry run passes in both polarities

Backs: RLS-02 (the Freeze rule's re-run of the allow-list after the T6 re-open of plan 44-03, E-21; this entry supersedes E-11 as the allow-list evidence for the new head without editing it).

```sh
git rev-parse HEAD
git diff --quiet 8188a9ac69ec98882fa3365a359d4ff88d154d6b HEAD -- src build.gradle.kts settings.gradle.kts gradle README.md LICENSE config .github VERSION
TG=$(git tag --merged HEAD --list 'v*') && printf '%s\n' "${TG}" | python3 -I .github/scripts/baseline_tag.py v1.7.0_gg7
git archive v1.7.0_gg6 | tar -x -C /tmp/gsd44/w/final2-base-src
git archive HEAD | tar -x -C /tmp/gsd44/w/final2-tip-src
env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain createConfluentArchive -PjavaVersion=21   # in each export, with its own wrapper, one after the other
cp <export>/build/confluent/clickhouse-kafka-connect-<version>.zip /tmp/gsd44/w/final2-arch/base.zip and tip.zip
python3 -I .github/scripts/compare_confluent_archives.py /tmp/gsd44/w/final2-arch/base.zip /tmp/gsd44/w/final2-arch/tip.zip /tmp/gsd44/w/final2-arch/empty.txt > /tmp/gsd44/exec2/cmp-empty-final.out
python3 (the rows derived by owner from the UNALLOWED lines, the reasons taken from the committed list by glob) > /tmp/gsd44/w/final2-arch/derived.txt
python3 -I .planning/phases/44-release-publication/44-allowlist-check.py rows|same-globs|cover ...
python3 -I .github/scripts/compare_confluent_archives.py base.zip tip.zip .planning/scripts/confluent-archive-allowlist-v1.7.0_gg7.txt > /tmp/gsd44/exec2/cmp-final.out
python3 -I .github/scripts/compare_confluent_archives.py base.zip tip.zip <scratch list with a stale row | without the gson classes row | with a *.class row>
DOCKER_HOST=unix:///nonexistent/docker.sock env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain testUnit --rerun --tests '*CompareConfluentArchivesScriptUnitTest' --tests '*ReleaseWorkflowGuardUnitTest' -PjavaVersion=21
python3 -I .planning/phases/44-release-publication/44-hygiene-check.py --strict .planning/scripts/confluent-archive-allowlist-v1.7.0_gg7.txt
git diff --quiet 8188a9ac69ec98882fa3365a359d4ff88d154d6b HEAD -- src build.gradle.kts settings.gradle.kts gradle README.md LICENSE config .github VERSION CHANGELOG.md docs
git log -1 --format=%H -- .planning/scripts/confluent-archive-allowlist-v1.7.0_gg7.txt
```

```text
supersedes: E-11 (the allow-list evidence of the older gated head b265f5a45a0752f314bb95315e3cdfbeaa2b78cd)
utc_start: 2026-11-03T11:39:02Z
tip_head: e18459dd1841ebad562dda5ea4b0cf7ec096ca16
docs_frozen_tip: 8188a9ac69ec98882fa3365a359d4ff88d154d6b
documents_changed_since_e10: CHANGELOG.md; docs/FORK-DELTA.md; docs/ROLLOUT-v1.7.0_gg7.md (commits 14f4567e70814d4bd245aaec1fe398d403d4bb2a and 5b25c18ce6137892383899f9f10663a22f498fe5, E-21)
archive_inputs_unchanged_since_freeze: yes
baseline_tag: v1.7.0_gg6
workflow_allowlist_path_matches: yes
jvm_flag: -PjavaVersion=21 on every Gradle call (it selects the test JVM only; the archive is compiled by the same toolchain as in E-11)
final_base_verify_line: verifyConfluentArchive: 7933 inner entries, no nested jar, no Kafka API class
final_tip_verify_line: verifyConfluentArchive: 7932 inner entries, no nested jar, no Kafka API class
final_base_entries: 7933
final_tip_entries: 7932
entries_equal_e05: yes
final_base_bytes: 13558634
final_tip_bytes: 13564145
bytes_equal_e11: yes
empty_compare_exit: 1
empty_compare_totals: totals files-a 7513 files-b 7512 only-a 1 only-b 0 changed 119 same 7393
empty_compare_unallowed: 120
empty_compare_matches_e05: yes
empty_compare_version_table: com.google.code.gson:gson 2.14.0 -> 2.15.0
empty_compare_manifest: MANIFEST.MF: differs only in Implementation-Version
owner_split: gson classes 102 (101 changed, 1 only in the base); gson metadata 3; connector classes 14; manifest 1; anything else 0
unexpected_differences: none
derived_rows: 18
derived_vs_committed_list: the same 18 globs and the same 14 connector paths (same-globs only_a=0 only_b=0); the committed file lists three entries in another order than the sorted derivation, so the bytes differ only in row order
allowlist_rewritten: no (the committed list is the derivation up to row order; nothing in it needs a change, and a commit of an unchanged file does not exist)
list_rows: OK rows=18
list_rows_count: 18
list_cover: OK differences=120 covered=120 uncovered=0 overlapping=0 stale_rows=0
two_segment_rule: ok
connector_rows_exact: yes
gson_rows: com/google/gson/*; META-INF/maven/com.google.code.gson/gson/*; META-INF/versions/9/module-info.class
header_line1_ok: yes
list_compare_exit: 0
list_compare_result: RESULT: PASS
compare_summary: exit 0, 0 unallowed, 0 stale rows, baseline v1.7.0_gg6, allow-list .planning/scripts/confluent-archive-allowlist-v1.7.0_gg7.txt
list_compare_totals: totals files-a 7513 files-b 7512 only-a 1 only-b 0 changed 119 same 7393
list_compare_manifest: MANIFEST.MF: differs only in Implementation-Version
list_compare_nested_jars: 0
list_compare_kafka_classes: 0
mutation_stale: exit=1 STALE-ROW zz/nothing/*
mutation_deleted: exit=1 102 UNALLOWED lines, the first UNALLOWED CHANGED com/google/gson/FieldNamingStrategy.class
mutation_wildcard: exit=2 ERROR: a glob must start with a literal path prefix; one that starts with a wildcard (* ? [) is not anchored to a path and is refused
research_list_equal: yes
guards_exit: 0
guards_failed: OK failed: 0 failing testcases, 0 expected
compare_xml: OK junit build/test-results/testUnit class *CompareConfluentArchivesScriptUnitTest: tests=58 failures=0 errors=0 skipped=0 files=1
relguard_xml: OK junit build/test-results/testUnit class *ReleaseWorkflowGuardUnitTest: tests=38 failures=0 errors=0 skipped=0 files=1
allowlist_commit: 402b6e2ea99a65e80c42f710e2b93277c6f78c64
allowlist_commit_files: .planning/scripts/confluent-archive-allowlist-v1.7.0_gg7.txt
allowlist_commit_new: none
generated_last: yes
last_scripts_commit: 402b6e2ea99a65e80c42f710e2b93277c6f78c64
last_non_planning_commit: 5b25c18ce6137892383899f9f10663a22f498fe5
hygiene_scan: clean
secrets_scan: clean
utc_end: 2026-11-03T11:42:30Z
```

The archives were rebuilt from clean exports of tag `v1.7.0_gg6` and of the new tip and read the same figures as E-11: the same entry counts, the same byte sizes, the same 120 differences, every one a gson entry, a connector class or the manifest, and the version table changing for `gson` only. The three documents that changed after the E-10 freeze are not archive inputs, so the 18 derived rows equal the committed list as a set, and the list stays as committed: its last commit is the E-11 allow-list commit, and `generated_last` reads yes because no archive input, `CHANGELOG.md` or `docs/` path changed between the freeze commit of E-21 and this regeneration. The local dry run reads `exit 0, 0 unallowed, 0 stale rows` and the three mutations read exit 1, 1 and 2. The runner dry run of plan 44-05 remains the check that counts for the release.

2026-11-03T11:42:30Z

Verdict: pass

### E-24: Re-run of plan 44-04 Task 2 on the new head: the invariant script started on it, testUnit with Docker unreachable, the anchors mirror, scope and numbers; one hygiene hit in the orchestrator's re-open handoff

Backs: RLS-04 (Docker-free half re-run after the T6 re-open of plan 44-03, E-21; this entry is the re-run of E-12 and E-20 on the new head and supersedes their gated head, testUnit totals and invariant start without editing them).

```sh
git rev-parse HEAD
git ls-remote origin refs/heads/custom_build
git rev-list --left-right --count origin/custom_build...HEAD
git ls-remote --heads origin 'sync/*'
git ls-remote --tags origin 'v1.7.0_gg7*'
git log -1 --format=%H -- . ':(exclude).planning'
git log -1 --format=%H -- .planning/scripts
/bin/bash -c 'bash .planning/scripts/verify-merge-invariants.sh 54e63648 06a9980a7f7271efa1bae4c8b88ad149e51f50ee > /tmp/gsd44/exec/invariants.log 2>&1; echo $? > /tmp/gsd44/exec/invariants.exit'   # once, in the background, alone (no Gradle build ran until it had finished)
DOCKER_HOST=unix:///nonexistent/docker.sock docker info
./gradlew --stop; sysctl vm.swapusage; rm -rf build/test-results/testUnit
DOCKER_HOST=unix:///nonexistent/docker.sock env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain testUnit --rerun -PjavaVersion=21 > /tmp/gsd44/exec2/gate-testunit.log 2>&1
python3 -I .planning/phases/44-release-publication/44-evidence-check.py junit|failed build/test-results/testUnit ...
python3 -I .planning/phases/44-release-publication/44-evidence-check.py anchors .planning/scripts/fork-anchors.txt --rows 190
git diff --name-only 9a5209c17e5380ec1f675cd8b3225250d3ff41fe 06a9980a7f7271efa1bae4c8b88ad149e51f50ee -- . ':(exclude).planning'
git diff --name-only 9a5209c17e5380ec1f675cd8b3225250d3ff41fe 06a9980a7f7271efa1bae4c8b88ad149e51f50ee -- .planning/scripts
git diff --shortstat v1.7.0 06a9980a7f7271efa1bae4c8b88ad149e51f50ee -- src build.gradle.kts gradle .github
git diff --shortstat v1.7.0 06a9980a7f7271efa1bae4c8b88ad149e51f50ee -- src/main
git diff --shortstat v1.7.0_gg6 06a9980a7f7271efa1bae4c8b88ad149e51f50ee -- src/main
git log --format=%h 31b1721427915576bb631e59a6c75812725fd8b0..06a9980a7f7271efa1bae4c8b88ad149e51f50ee -- src/integrationTest
python3 -I .planning/phases/44-release-publication/44-review-check.py agents|rounds|disposition ...   # five reviewer ids
python3 -I .planning/phases/44-release-publication/44-note-check.py all docs/ROLLOUT-v1.7.0_gg7.md --allow 72e8ccffe05ff6dffb14098d0b8c0f67aaa6a236 e745e7d5363fca8067ddfcc80f3ebea034b93abb4f2912e514492201a729c2af
python3 -I .planning/phases/44-release-publication/44-hygiene-check.py [--strict] <every file added since the phase start, the added lines of the modified documents>
python3 -I .planning/phases/44-release-publication/44-evidence-check.py secrets <evidence, review, note>
git worktree list
```

```text
supersedes: E-12 and E-20 (gated head, testUnit totals, invariant start); E-24 itself keeps verdict fail for the one hygiene hit below, which a later entry re-runs
gated_head: 06a9980a7f7271efa1bae4c8b88ad149e51f50ee
gated_head_note: the commit of E-23, the first commit after the T6 documents and their disposition; only .planning evidence commits follow it
utc_start: 2026-11-03T11:42:00Z
dirty_paths_at_gate: M .planning/config.json; M .planning/state.json
origin_custom_build_at_gate: 72e8ccffe05ff6dffb14098d0b8c0f67aaa6a236 refs/heads/custom_build
origin_unchanged: yes
left_count_at_gate: 0
origin_sync_ref: none
origin_gg7_tags: none
local_release_tag: none
last_non_planning_commit: 5b25c18ce6137892383899f9f10663a22f498fe5
last_scripts_commit: 402b6e2ea99a65e80c42f710e2b93277c6f78c64
invariants_started_on: 06a9980a7f7271efa1bae4c8b88ad149e51f50ee
invariants_started_utc: 2026-11-03T11:42:10Z
invariants_exit_seen: 0
invariants_line_seen: invariants: provenance_paths=21 anchor_commits=1556 anchor_rows=190 marker_commits=1622 anchor_exempt=19 result=PASS
invariants_finished_utc: 2026-11-03T12:29:13Z
invariants_overlap: none (no Gradle build ran between the start and the finish of the script; only read-only git and python checks ran beside it)
docker_unreachable_proof: exit=1 failed to connect to the docker API at unix:///nonexistent/docker.sock; check if the path is correct and if the daemon is running: dial unix /nonexistent/docker.sock: connect: no such file or directory
swap_before_testunit: vm.swapusage: total = 30720.00M  used = 29506.56M  free = 1213.44M  (encrypted)
testunit_exit: 0
testunit_tests: 1826
testunit_failures: 0
testunit_errors: 0
testunit_skipped: 2
testunit_expected_tests: 1826
testunit_count_matches: yes
testunit_jdk: -PjavaVersion=21 (the default OpenJ9 17 test JVM cannot load class version 65 on this machine, as E-22 recorded)
testunit_jvm_crash: none
testunit_duration: BUILD SUCCESSFUL in 17s
failed_ok: yes
compare_xml: OK junit build/test-results/testUnit class *CompareConfluentArchivesScriptUnitTest: tests=58 failures=0 errors=0 skipped=0 files=1
bump_xml: OK junit build/test-results/testUnit class *BumpVersionScriptUnitTest: tests=52 failures=0 errors=0 skipped=0 files=1
baseline_xml: OK junit build/test-results/testUnit class *BaselineTagScriptUnitTest: tests=19 failures=0 errors=0 skipped=0 files=1
relguard_xml: OK junit build/test-results/testUnit class *ReleaseWorkflowGuardUnitTest: tests=38 failures=0 errors=0 skipped=0 files=1
mbean_xml: OK junit build/test-results/testUnit class *MBeanNameUnitTest: tests=11 failures=0 errors=0 skipped=0 files=1
closing_writer_xml: OK junit build/test-results/testUnit class *WriterHelperClientClosingUnitTest: tests=23 failures=0 errors=0 skipped=0 files=1
closing_keeper_xml: OK junit build/test-results/testUnit class *KeeperStateProviderClosingUnitTest: tests=5 failures=0 errors=0 skipped=0 files=1
stop_xml: OK junit build/test-results/testUnit class *ProxySinkTaskStopUnitTest: tests=4 failures=0 errors=0 skipped=0 files=1
stall_xml: OK junit build/test-results/testUnit class *InsertStallBoundPinUnitTest: tests=15 failures=0 errors=0 skipped=0 files=1
sockopt_xml: OK junit build/test-results/testUnit class *InsertSocketTimeoutOptionUnitTest: tests=23 failures=0 errors=0 skipped=0 files=1
origtopic_xml: OK junit build/test-results/testUnit class *OriginalTopicInsertedOffsetsUnitTest: tests=6 failures=0 errors=0 skipped=0 files=1
passthrough_xml: OK junit build/test-results/testUnit class *RetriableExceptionPassThroughUnitTest: tests=38 failures=0 errors=0 skipped=0 files=1
cycle_xml: OK junit build/test-results/testUnit class *HandleExceptionCauseCycleUnitTest: tests=2 failures=0 errors=0 skipped=0 files=1
warn_unit_xml: OK junit build/test-results/testUnit class *UnvettedRetriableCodesWarnUnitTest: tests=21 failures=0 errors=0 skipped=0 files=1
wrapped_xml: OK junit build/test-results/testUnit class *WrappedDataExceptionClassificationPinUnitTest: tests=14 failures=0 errors=0 skipped=0 files=1
anchors_mirror: rows=190 bad=0
scope_outside_planning: CHANGELOG.md; VERSION; docs/FORK-DELTA.md; docs/ROLLOUT-v1.7.0_gg7.md
scope_planning_scripts: .planning/scripts/confluent-archive-allowlist-v1.7.0_gg7.txt; .planning/scripts/fork-anchors.txt
no_java_change: yes
src_main_vs_gg6_equals_forkdelta: yes
forkdelta_numbers_equal: yes
note_archive_numbers_match: yes
reviewer_agents_check: OK agents: rounds=5 distinct=5 ids=a37f170dc8cc7a801,a752798ab528d2b1f,a401c814e0b118957,a32f740ad20019e8a,a87ce913aac02a00b
reviewer_rounds_check: OK rounds: [(1, 14), (2, 2), (3, 2), (4, 2), (5, 5)] last_clean=False decision=True
disposition_check: OK disposition: findings=25 rows=25
note_check: OK rows=33 measured=24 code=21 derived=4 mechanism=1
integration_test: the whole integrationTest task was not re-run (it needs a Connect stack); E-22 re-ran ClickHouseSinkConnectorIntegrationTest on head 14f4567e (docs-only change since the last src change) and it passed with 55 tests
integration_sources_after_run: 51200928 (fix(39): WR-01, the sink task status log line kept purely diagnostic in the integration test; before the Phase 39 run of record, and E-22 is the newer run)
hygiene_scan_all: not clean: 24 of the 25 text files added since the phase start read 0 hits in default and strict mode, the added lines of CHANGELOG.md, VERSION, docs/FORK-DELTA.md and fork-anchors.txt read 0 hits in both modes, and the evidence, review, disposition, allow-list and note files read 0 hits strict; but 44-REOPEN-HANDOFF.md (added by the orchestrator after E-20) has 1 hit at line 32 in both modes (the helper's fifth default pattern, a bare four-letter verb beginning with st, written in a sentence that describes the helper's own quirk)
secrets_scan: clean
nothing_pushed: yes
worktrees_clean: yes
docker_free_decision: no-go
gate_failures: hygiene_scan_all (1 hit in 44-REOPEN-HANDOFF.md line 32, an orchestrator working note; this plan never edits a document; every other Docker-free gate holds)
utc_end: 2026-11-03T12:33:00Z
```

Every Docker-free gate other than the hygiene scan holds on the new gated head: the invariant script, started once on this SHA and run alone, ended `result=PASS` with `anchor_rows=190` over 1556 anchor commits; testUnit with the daemon proven unreachable read 1826 tests with 0 failures, 0 errors and 2 skipped and every pin class green; the anchors mirror reads `rows=190 bad=0`; the non-planning scope is the four document paths and the scripts scope is the allow-list and the anchors file; the FORK-DELTA figures and the note's entry counts agree with fresh runs; the five reviewer ids, the disposition of all 25 findings and the note checker are green; origin `custom_build` still reads the gg6 commit and nothing was pushed. The one red is the helper's pattern for an outside-system name matching a plain verb in the orchestrator's re-open handoff, a file that the plan's wide scan covers and that no earlier entry scanned. A wording change in that planning file is outside this plan and is the orchestrator's decision; the verdict stays fail until a later entry re-runs the scan, as E-20 did for E-12. The invariant result is kept in `/tmp/gsd44/exec/invariants.exit` and `invariants.log` for the suites entry.

2026-11-03T12:33:00Z

Verdict: fail

### E-25: Re-run of plan 44-04 Task 3 on the new head: two full suites on one pinned ClickHouse latest with client V2, the invariant script's result, and the gate decision (no-go on the single hygiene hit of E-24 only)

Backs: RLS-04 (live half re-run after the T6 re-open of plan 44-03, E-21; supersedes E-13 for the new gated head without editing it). The suites and the invariant script do not depend on the hygiene scan, so they were run although E-24 reads fail, which the plan's Task 3 precondition would not allow; this is a recorded deviation and the decision below carries it.

```sh
git diff --quiet 06a9980a7f7271efa1bae4c8b88ad149e51f50ee HEAD -- . ':(exclude).planning'
git diff --quiet 06a9980a7f7271efa1bae4c8b88ad149e51f50ee HEAD -- .planning/scripts
./gradlew --stop; sysctl vm.swapusage; colima list
docker image inspect --format '{{index .RepoDigests 0}}' clickhouse/clickhouse-server:latest
docker run --rm --entrypoint clickhouse clickhouse/clickhouse-server:latest local --query 'SELECT version()'
rm -rf build/test-results/test
CLICKHOUSE_VERSION=latest CLIENT_VERSION=V2 env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain test --rerun -PjavaVersion=21 > /tmp/gsd44/exec2/suite1.log 2>&1
python3 -I .planning/phases/44-release-publication/44-evidence-check.py junit|skips|failed build/test-results/test ...
rm -rf build/test-results/test
CLICKHOUSE_VERSION=latest CLIENT_VERSION=V2 env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain test --rerun -PjavaVersion=21 > /tmp/gsd44/exec2/suite2.log 2>&1
python3 -I .planning/phases/44-release-publication/44-evidence-check.py junit|skips|failed build/test-results/test ...
cat /tmp/gsd44/exec/invariants.exit; grep '^invariants:' /tmp/gsd44/exec/invariants.log
```

```text
supersedes: E-13 (the live half of the older gated head b265f5a45a0752f314bb95315e3cdfbeaa2b78cd)
gated_head: 06a9980a7f7271efa1bae4c8b88ad149e51f50ee
gate_head_same: yes
swap_before: vm.swapusage: total = 30720.00M  used = 29726.12M  free = 993.88M  (encrypted)
colima_vz: Running
digest_before_run1: clickhouse/clickhouse-server@sha256:739e099abd1157390960a59e4532d21781d8fde57e6e4151cb920ea02c30b1ce
select_version_before_run1: 26.10.3.7
digest_same_as_e01: yes
suite1_utc_start: 2026-11-03T12:31:15Z
suite1_exit: 0
suite1_tests: 2425
suite1_failures: 0
suite1_errors: 0
suite1_skipped: 18
suite1_skips: OK skips: named=7 state_mismatch_cases=11 unexpected=0
suite1_failed_ok: yes
suite1_duration: BUILD SUCCESSFUL in 2m 47s
suite1_jdk: -PjavaVersion=21 (the default OpenJ9 17 test JVM cannot load class version 65 on this machine, as E-22 recorded; E-13 ran on the default JDK 17 line)
suite1_jvm_crash: none
suite1_warn_live_xml: OK junit build/test-results/test class *UnvettedRetriableCodesWarnLiveTest: tests=1 failures=0 errors=0 skipped=0 files=1
suite1_processing_xml: OK junit build/test-results/test class *ProcessingTest: tests=14 failures=0 errors=0 skipped=0 files=1
digest_after_run1: clickhouse/clickhouse-server@sha256:739e099abd1157390960a59e4532d21781d8fde57e6e4151cb920ea02c30b1ce
select_version_after_run1: 26.10.3.7
suite2_utc_start: 2026-11-03T12:34:32Z
suite2_exit: 0
suite2_tests: 2425
suite2_failures: 0
suite2_errors: 0
suite2_skipped: 18
suite2_skips: OK skips: named=7 state_mismatch_cases=11 unexpected=0
suite2_failed_ok: yes
suite2_duration: BUILD SUCCESSFUL in 2m 38s
suite2_jdk: -PjavaVersion=21 (as run 1)
suite2_jvm_crash: none
suite2_warn_live_xml: OK junit build/test-results/test class *UnvettedRetriableCodesWarnLiveTest: tests=1 failures=0 errors=0 skipped=0 files=1
suite2_processing_xml: OK junit build/test-results/test class *ProcessingTest: tests=14 failures=0 errors=0 skipped=0 files=1
digest_after_run2: clickhouse/clickhouse-server@sha256:739e099abd1157390960a59e4532d21781d8fde57e6e4151cb920ea02c30b1ce
select_version_after_run2: 26.10.3.7
suites_jdk: -PjavaVersion=21 for both suites and for the testUnit run of E-24
suites_same_jvm: yes
suite_expected_tests: 2425
suite_count_matches: yes
suite_tests_equal: yes
docker_only_delta: 599
pinned_image_same: yes
crash_files_left: none
invariants_exit: 0
invariants_line: invariants: provenance_paths=21 anchor_commits=1556 anchor_rows=190 marker_commits=1622 anchor_exempt=19 result=PASS
invariants_head: 06a9980a7f7271efa1bae4c8b88ad149e51f50ee
invariants_finished_utc: 2026-11-03T12:29:13Z
technical_gates: green (testUnit, both suites, the invariant script, the anchors mirror, the allow-list compare and the scope and number facts of E-23 and E-24)
docker_free_decision_source: E-24 (no-go: hygiene_scan_all only)
gate_decision: no-go
gate_failures: hygiene_scan_all (E-24: 1 hit in 44-REOPEN-HANDOFF.md line 32, an orchestrator working note outside this plan's remit)
gate_note: The CI side (the scratch dry run, the single tests.yaml run on the merged SHA and the publish run) is the user's and not part of this gate; the whole integrationTest task was not re-run and E-22 holds its one recorded class run (E-24 integration_test).
utc_start: 2026-11-03T12:31:15Z
utc_end: 2026-11-03T12:38:00Z
```

Both full suites read 2425 tests with 0 failures, 0 errors and the 18 named skips on one build of `latest` (`26.10.3.7`, one digest at all three reads) with client V2 and one JVM line (`-PjavaVersion=21`, not the default JDK 17 line of E-13, because the default test JVM cannot load class version 65 here); the Docker-only delta over `testUnit` is 599, as in E-13. The invariant script ran alone before the suites on the gated head and ended `result=PASS`. The decision is no-go only because E-24 reads fail on the one hygiene hit; every technical gate is green, and a neutral rewording of that one planning-file line (a `.planning` path outside `.planning/scripts`, so the Freeze rule's gated head is unchanged) followed by a superseding hygiene re-run, as E-20 did for E-12, turns the decision to go without a new suite or invariant run. The runner-side checks are the user's.

2026-11-03T12:38:00Z

Verdict: fail

### E-26: Re-run of the E-24 hygiene scan after a neutral rewording of one handoff line; the Docker-free decision and the suites decision are go

Backs: RLS-04 (E-24 read verdict fail on one hygiene hit, line 32 of 44-REOPEN-HANDOFF.md; this entry re-runs the scan and supersedes the E-24 and E-25 decision without editing them).

```sh
python3 -I .planning/phases/44-release-publication/44-hygiene-check.py [--strict] <the 25 files added since the phase start>
python3 -I .planning/phases/44-release-publication/44-evidence-check.py secrets <evidence, review, note>
git diff --quiet 06a9980a7f7271efa1bae4c8b88ad149e51f50ee HEAD -- . ':(exclude).planning'
git diff --quiet 06a9980a7f7271efa1bae4c8b88ad149e51f50ee HEAD -- .planning/scripts
git ls-remote origin refs/heads/custom_build
```

```text
supersedes: E-24 and E-25 (decision only; their numbers stay as recorded)
hit_before: 44-REOPEN-HANDOFF.md line 32, the helper's fifth default pattern (a plain four-letter verb beginning with st)
wording_change_by: the orchestrator; one line reworded, no fact changed
hygiene_default: 25 files, 0 hits
hygiene_strict: 25 files, 0 hits
secrets_scan: clean
gated_head: 06a9980a7f7271efa1bae4c8b88ad149e51f50ee
tip_equals_gate_outside_planning: yes
tip_equals_gate_planning_scripts: yes
origin_custom_build: 72e8ccffe05ff6dffb14098d0b8c0f67aaa6a236 (unchanged)
nothing_pushed: yes
docker_free_decision: go
suites_decision: go
```

Every technical gate of E-23 to E-25 was green; the one red was the hygiene hit, now gone.

2026-11-03T12:50:00Z

Verdict: pass

### E-27: Re-run of plan 44-05 Task 1 on the new head: upstream recheck 3, the merge message, a dry-run merge with its checks, the rollback on it, the date check, and the approval bundle rewritten for the new gated head

Backs: RLS-04 (the Freeze rule's re-run of the publication rehearsal after the T6 re-open of plan 44-03, E-21, and the re-runs of plan 44-04, E-23 to E-26; this entry supersedes E-14 for the new gated head without editing it).

```sh
git rev-parse HEAD
git ls-remote origin refs/heads/custom_build
git rev-list --left-right --count origin/custom_build...sync/v1.7.0
git rev-list --count 72e8ccffe05ff6dffb14098d0b8c0f67aaa6a236..HEAD
git rev-list --count HEAD --not --remotes=origin
git rev-parse 'pre-v1.7.0_gg7^{commit}'; git cat-file -t pre-v1.7.0_gg7
git ls-remote --heads origin 'sync/*'; git ls-remote --tags origin 'v1.7.0_gg7*' 'pre-v1.7.0_gg7*'; git tag -l 'v1.7.0_gg7'
gh api "repos/ne1r0n/clickhouse-kafka-connect/pulls?state=open&per_page=50" --jq '[.[] | "\(.number) \(.head.sha[0:8])"] | sort | join("; ")'
git diff --shortstat 72e8ccffe05ff6dffb14098d0b8c0f67aaa6a236 HEAD -- . ':(exclude).planning'    # and -- .planning
git diff --quiet <E-24 gated_head> HEAD -- . ':(exclude).planning'; git diff --quiet <E-24 gated_head> HEAD -- .planning/scripts
git diff --quiet <E-21 disposition_commit> <E-24 gated_head> -- . ':(exclude).planning'
git ls-remote upstream refs/heads/main; git ls-remote --tags --refs upstream 'v*'
curl -s https://api.github.com/repos/ClickHouse/clickhouse-kafka-connect/pulls/<861|855|857>
curl -s https://api.github.com/repos/ClickHouse/clickhouse-kafka-connect/compare/d92a1830...main
/bin/bash /tmp/gsd44/exec3/t1-facts-a.sh        # the reads above, read-only (the E-14 script with E-24 and E-21 as the gate and freeze sources)
git diff --stat adf06e15dacbe2bb08f128621d06fd272aab8520 HEAD -- .planning/phases/44-release-publication/44-MERGE-MESSAGE.txt
python3 -I /tmp/gsd44/exec/extract-rb.py docs/ROLLOUT-v1.7.0_gg7.md /tmp/gsd44/exec3/rollback-block-t1.sh
git worktree add --detach /tmp/gsd44/w/pubdry 72e8ccffe05ff6dffb14098d0b8c0f67aaa6a236
git --attr-source="$(git hash-object -t tree /dev/null)" merge --no-ff -F <absolute path of 44-MERGE-MESSAGE.txt> <tip_head>    # in the worktree, demo identity
bash -e /tmp/gsd44/exec3/rollback-block-t1.sh   # in the worktree
git --attr-source="$(git hash-object -t tree /dev/null)" revert --no-edit <rollback commit>    # in the worktree
git worktree remove --force /tmp/gsd44/w/pubdry; git worktree prune; git branch -D rollback-v1.7.0_gg7
/bin/bash /tmp/gsd44/exec3/t1-facts-b.sh <tip_head> <absolute path of 44-MERGE-MESSAGE.txt> /tmp/gsd44/exec3/rollback-block-t1.sh   # the five lines above in one script
docker run --rm --entrypoint clickhouse clickhouse/clickhouse-server:latest local --query 'SELECT version()'
docker image inspect --format '{{index .RepoDigests 0}}' clickhouse/clickhouse-server:latest
head -1 CHANGELOG.md; date -u +%F
python3 -I .planning/phases/44-release-publication/44-hygiene-check.py --strict <merge message> <bundle> <four handoffs> <runbook> <evidence>
python3 -I .planning/phases/44-release-publication/44-evidence-check.py secrets .planning/phases/44-release-publication/44-COMMAND-EVIDENCE.md
python3 -I .planning/phases/44-release-publication/44-evidence-check.py anchors .planning/scripts/fork-anchors.txt --rows 190
git add .planning/phases/44-release-publication/44-APPROVAL-BUNDLE.md && git commit -F <message>
git add <four handoffs> <runbook> && git commit -F <message>
```

```text
supersedes: E-14 (the rehearsal and the bundle of the older gated head b265f5a45a0752f314bb95315e3cdfbeaa2b78cd)
utc_start: 2026-11-03T12:43:05Z
tip_head: 16ffde588434c36085d80c124e6036a9cf478043
origin_custom_build: 72e8ccffe05ff6dffb14098d0b8c0f67aaa6a236 refs/heads/custom_build
origin_unchanged: yes
left_count: 0
left_right: 0 278
ahead_of_gg6: 278
unpublished_commits: 276
pre_tag_local: yes (commit at 72e8ccffe05ff6dffb14098d0b8c0f67aaa6a236)
origin_sync_ref: none
origin_gg7_tags: none
local_release_tag: none
pr_heads: 21 14be67ce; 22 b5cccbd3
pr_heads_unchanged: yes
non_planning_vs_gg6: 40 files changed, 3169 insertions(+), 146 deletions(-)
planning_vs_gg6: 311 files changed, 31735 insertions(+), 1428 deletions(-)
gated_sha: 5b25c18ce6137892383899f9f10663a22f498fe5
gated_head: 06a9980a7f7271efa1bae4c8b88ad149e51f50ee
tip_equals_gate_outside_planning: yes
frozen_docs_equal_gate: yes
e21_disposition_commit: 8188a9ac
changed_files_outside_planning_equal_e14: yes (the same 40 paths as the list of the first bundle)
src_gradle_github_unchanged_since_phase_start: yes
upstream_main_end: d92a1830113a5f9c1cf49bf67f65ec6d188d1391
upstream_newest_tag_end: v1.7.0
upstream_tag_past_v170_end: no
upstream_pr_861_end: open False 3d4cfdf2
upstream_pr_855_end: open False ddabf28b
upstream_pr_857_end: open False 11d88bc0
upstream_main_vs_e14: unchanged (d92a1830 at E-14 and now; the compare of d92a1830 against main reads identical)
upstream_changed_since_e14: no
upstream_changed_since_e01: yes: main moved from 9bed6f7970670a55c66be8efda246889e1f95bd0 (E-01) to d92a1830113a5f9c1cf49bf67f65ec6d188d1391 before E-14 (six commits, dependency bumps and cloud-test secrets in tests.yaml); newest tag, tags past v1.7.0 and pull requests 861, 855 and 857 unchanged; information for the next sync, not a STOP
merge_message_unchanged_since_adf06e15: yes
merge_message_facts_hold: yes (it names no SHA but 72e8ccff, no count other than eight src/main files, four minutes and the version, all unchanged)
merge_message_note: the message says an insert "is bounded" and that "a quiet insert that runs longer fails"; after the T6 measurement (E-21) the CHANGELOG says an insert that streams progress headers escapes the bound; the message is accurate as worded but silent on the escape; not edited, put to the user in bundle question (8)
merge_subject: Merge sync/v1.7.0: release v1.7.0_gg7
merge_trailer_last: yes
d_commit: ff4a84e70281a97951766268afdd6c0830fc0181
d_parents: 72e8ccffe05ff6dffb14098d0b8c0f67aaa6a236 16ffde588434c36085d80c124e6036a9cf478043
d_tree_equal: yes
d_subject: Merge sync/v1.7.0: release v1.7.0_gg7
d_no_gsd_ws_arg: yes
d_version: v1.7.0_gg7
d_allowlist_present: yes
d_baseline_tag: v1.7.0_gg6
d_no_gg7_tag_merged: yes
d_operator_doc_check: none
d_rollback_block_equal_e14: yes
d_rollback_exit: 0
d_rollback_output: ok
d_rollback_version: v1.7.0_gg6
d_rollback_note_absent: yes
d_rollback_tree_equals_tag: yes
d_gradlew_bat_trap: no
d_revert_restores_tree: yes
d_cleaned: yes
local_custom_build_ref_unmoved: yes
select_version: 26.10.3.7
image_digest: clickhouse/clickhouse-server@sha256:739e099abd1157390960a59e4532d21781d8fde57e6e4151cb920ea02c30b1ce
image_same_as_e25: yes
changelog_heading_date: 2026-11-02
utc_today: 2026-11-03 (read at 2026-11-03T12:43:14Z)
changelog_date_vs_today: differs (2026-11-02 vs 2026-11-03)
changelog_date_deviation: the heading date stays by the user's recorded answer "Keep the date (Recommended)"; read as the date the documents were frozen; no edit; the release record states it as a deviation
user_answers_carried: "Go for U1 and U2" (for the older head, void, asked again); "You run them (Recommended)"; "Keep the date (Recommended)"; "Re-run integrationTest first" (E-22); "Measure the T6 progress-header claim first" (E-21)
bundle_questions_asked_again: (2) the go for U1 and U2 on the new head; (4) round 5 fixes accepted without a round 6, yes or no; (1), (3), (5), (6), (7), (8) restate answers or measurements and need a word only on disagreement
handoff_files_updated: the four handoffs and the runbook name the new gated head 06a9980a, last non-planning commit 5b25c18c, tip 16ffde58 and the evidence numbers E-23 to E-27; the publication tail is recorded from E-28; E-15 to E-18 are untouched
bundle_items: yes
bundle_gates: yes
message_check: yes
hygiene_scan: clean
secrets_scan: clean
anchors_mirror: rows=190 bad=0
bundle_commit: 9f72b5d888fe63f1c8f0e2d7057a9a9fa97ae9a9
handoffs_commit: d71825e3020358b6345b7186bcf841044ac52334
nothing_pushed: yes
worktrees_clean: yes
utc_end: 2026-11-03T12:48:41Z
```

D was built from the committed merge message (unchanged since `adf06e15`), with parents `72e8ccff` and the tip, and removed; it was never pushed, no branch or tag was created on origin, and the local custom_build ref did not move. Upstream `main` and the three pull requests read the same as at E-14. The extracted rollback block is byte-equal to the E-14 block although the note itself changed (the T6 row). The CHANGELOG heading date differs from UTC today, which the user's recorded answer accepts as a deviation. The one loose end for the user is the merge message's silence on the progress-header escape (bundle question (8)). The plan's verify clause was adapted: the gate and freeze sources are E-24 and E-21, the digest compare reads E-25, and the clause that demands `derived (mechanism, not measured)` in the bundle is dropped because the F-2 claim is measured (E-21).

2026-11-03T12:48:41Z

Verdict: pass

### E-28: The user's answers to the rewritten approval bundle, received by the orchestrator; the merge message gains one sentence

Backs: RLS-04 (the Freeze rule voided the first go; this records the answers to the bundle rewritten for gated head 06a9980a).

```text
received_by: the orchestrator, directly from the user through its question tool, 2026-11-03
go_u1_u2: "Go for U1 and U2 (Recommended)" (for gated head 06a9980a7f7271efa1bae4c8b88ad149e51f50ee, tip 16ffde588434c36085d80c124e6036a9cf478043 at the rehearsal)
review_end_state: "Accept without round 6 (Recommended)"
merge_message: "Add one sentence (Recommended)"
jdk_record: "JDK 21 is enough (Recommended)"
carried_answers: origin writes "You run them (Recommended)"; CHANGELOG heading date "Keep the date (Recommended)" (heading 2026-11-02 stays, recorded as a deviation)
merge_message_commit: 43f6c9da7c2d4c9f8cf5d4fac44c112594fd9c7c (44-MERGE-MESSAGE.txt, three added lines about the progress-header limit; strict hygiene 0 hits)
non_planning_change: none
publish_word: not given; the user's own direct word 'publish' is still required for step O3
```

The go covers U1 and U2 only. The merge message sits under `.planning`, so the Freeze rule is not triggered.

2026-11-03T13:30:00Z

Verdict: pass

### E-29: U1 scratch push and its tests.yaml run, user-reported (user-authorised, run by the orchestrator)

Backs: RLS-04 (the dry-run half; the push of the scratch branch).

```text
user_instruction: the user's own message "делай сам по документу то что нужно" (do it yourself per the document), after the E-28 go for U1 and U2 and the earlier answer "You run them (Recommended)"
pre_checks_1_to_9: all as expected (custom_build 72e8ccffe05ff6dffb14098d0b8c0f67aaa6a236; left count 0, right count 283; commit; same-as-gated; scripts-same-as-gated; no origin ref holds the SHA; no sync/v1.7.0 on origin; no v1.7.0_gg7 tag; heading date 2026-11-02 against UTC 2026-11-03, accepted as a deviation in E-28)
pushed_sha: fc5709374adcf87a1537e88ded8fb2968be331c3
push: git push origin "${SHA}":refs/heads/sync/v1.7.0, output "[new branch] fc5709374adcf87a1537e88ded8fb2968be331c3 -> sync/v1.7.0"
tests_yaml_run_id: 41687285579
tests_yaml_url: https://github.com/ne1r0n/clickhouse-kafka-connect/actions/runs/41687285579
tests_yaml_head_sha: fc5709374adcf87a1537e88ded8fb2968be331c3
tests_yaml_runs_for_sha: 1
tests_yaml_conclusion: success
```

Origin `custom_build` was not touched. The pushed head holds `.planning` commits after the gated head only.

2026-11-03T17:50:00Z

Verdict: pass

### E-30: U2 release dry run, user-reported (user-authorised, run by the orchestrator)

Backs: RLS-04 (the dry-run half only; RLS-04 stays pending until the publication entries exist).

```text
dispatch: gh workflow run release.yaml --ref sync/v1.7.0 -f dry_run=true
run_id: 41688323407
run_url: https://github.com/ne1r0n/clickhouse-kafka-connect/actions/runs/41688323407
run_name: Release sync/v1.7.0 (dry run)
event: workflow_dispatch
headBranch: sync/v1.7.0
headSha: fc5709374adcf87a1537e88ded8fb2968be331c3
conclusion: success
steps: every step success except "Publish release", skipped; draft created, checked, shown to have no tag, deleted
select_baseline_notice: v1.7.0_gg7 is compared with v1.7.0_gg6 under .planning/scripts/confluent-archive-allowlist-v1.7.0_gg7.txt
compare_notice: exit 0, 0 unallowed, 0 stale rows, baseline v1.7.0_gg6, allow-list .planning/scripts/confluent-archive-allowlist-v1.7.0_gg7.txt
artifact: archive-compare-report present (the RESULT line was not read; the compare step's notice above is the verdict)
check_archive_notice: clickhouse-kafka-connect-v1.7.0_gg7.zip 13564238 bytes sha256=3dbdcf03e5825073b7b0535718c9998a5e5754c0ae05a17502bbf4c6d69472af
tests_yaml_check_notice: successful tests.yaml runs on fc5709374adcf87a1537e88ded8fb2968be331c3: 1
cleanup_notices: no draft and no tag dry-run-41688323407-1 remain; tag v1.7.0_gg7 is absent
releases_listed: v1.7.0_gg6, v1.7.0_gg5 only
v1_7_0_gg7_tag_on_origin: none
```

A green dry run is not a pass for the whole publication; the publish run (U4) is the first to exercise the Publish release step for this release.

2026-11-03T17:55:00Z

Verdict: pass

### E-31: The user's own publish message and the merge commit M, built after it

Backs: RLS-04 (O2 pre-checks and O3: the merge commit M, built locally and detached, nothing pushed).

```text
publish_message: "publish" (the user's own message to the orchestrator, 2026-11-03, a second time after the reply that U1 and U2 were green; received directly, not relayed)
o2_a_custom_build: 72e8ccffe05ff6dffb14098d0b8c0f67aaa6a236 (unmoved)
o2_b_date: heading 2026-11-02, UTC 2026-11-03; kept as a recorded deviation by the user's answer in E-28 (no edit)
o2_c_upstream: newest tag v1.7.0 (no tag past v1.7.0)
o2_d: X0 is an ancestor of sync/v1.7.0 (equals the pushed head); same-as-gated and scripts-same-as-gated both printed; the merge message unchanged since 43f6c9da7c2d4c9f8cf5d4fac44c112594fd9c7c
X0: fc5709374adcf87a1537e88ded8fb2968be331c3 (origin sync/v1.7.0 reads the same)
M: 9483476322e0d331e81c5b92ac266a18f0f01450
m_parents: 72e8ccffe05ff6dffb14098d0b8c0f67aaa6a236 fc5709374adcf87a1537e88ded8fb2968be331c3
m_tree_equals_x0: yes
m_subject: Merge sync/v1.7.0: release v1.7.0_gg7
m_gsd_ws_arg_present: no
m_version: v1.7.0_gg7
m_author: the repository's configured identity
ref_keeping_m: refs/gsd/release-m-v1.7.0_gg7 (local only)
rollback_block_on_m: exit 0, output ok, status before and after empty, VERSION v1.7.0_gg6, note absent
rollback_cleanup: throwaway worktrees removed, branch rollback-v1.7.0_gg7 deleted, one worktree left
local_custom_build_ref: 3a3a15a9ae064ffe09c696b8bb87bce23419d463 (unmoved)
pushed: nothing
```

M exists only in this local repository. The next step is U3, the user's push of M to `custom_build`.

2026-11-03T18:20:00Z

Verdict: pass

### E-32: U3 push of M to custom_build and its single tests.yaml run, user-reported (user-authorised, run by the orchestrator)

Backs: RLS-04 (the push of M to the default branch).

```text
user_instruction: "push m" (the user's own message to the orchestrator, 2026-11-03)
pre_checks_1_to_7: all as expected (custom_build 72e8ccff; two parents 72e8ccff and fc570937; tree-equal; same-as-gated; scripts-same-as-gated; no origin ref holds M; left count 0; heading 2026-11-02 against UTC 2026-11-03, accepted in E-28)
push: git push origin "${M}":refs/heads/custom_build, output "72e8ccff..94834763  9483476322e0d331e81c5b92ac266a18f0f01450 -> custom_build" (fast-forward, no force)
origin_custom_build_after: 9483476322e0d331e81c5b92ac266a18f0f01450
runs_for_M: exactly one, tests.yaml, event push, run id 41704437509, conclusion success
runs_for_M_120s_after_completion: still one; no release.yaml run
tag_v1_7_0_gg7_on_origin: none
```

2026-11-03T18:50:00Z

Verdict: pass
