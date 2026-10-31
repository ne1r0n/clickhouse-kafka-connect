# Phase 38: Command evidence

- **Base commit:** `local_head` = `eb8c163ab9cdc7951612931f7c49b23e3ec37d6b` (HEAD when plan 38-01 started, branch `sync/v1.7.0`).
- **Shell:** macOS zsh, multi-line logic under `/bin/bash` or `python3 -I`. Gradle is run as `env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain ...`.
- **Docker:** colima profile `vz` (aarch64, 8 CPU, 16 GiB, 256 GiB disk), docker CLI from `/opt/homebrew/Cellar/docker/29.8.2/bin/docker`.
- **Date (UTC):** 2026-10-31.
- **Entry format:** each entry has a `### E-NN: {title}` heading, a `Backs:` line, an `sh` fence with the commands as run, a `text` fence of `key: value` lines (keys match `[a-z0-9_]+`, one fact per line) with free text allowed after the fence, a UTC timestamp line, and a final line `Verdict: pass`, `Verdict: fail`, `Verdict: pending ({reason})` or `Verdict: unmeasured ({reason})`.
- **Rule:** a fail or pending verdict is fixed by re-measuring in a new run, never by editing the evidence.
- **Helpers:** six read-only helpers live in this directory: `38-evidence-check.py` (a byte-identical copy of Phase 37's `37-evidence-check.py`; get, expect, index, rows, secrets, junit, anchors, skips, failed), `38-hygiene-check.py` (not a byte copy: two more always-on identifiers and a `--strict` mode), `38-note-check.py`, `38-allowlist-check.py`, `38-doc-check.py` and `38-review-check.py`; none is ever edited.
- **Server version:** E-01, E-12 and E-13 record `clickhouse_version`, the `SELECT version()` of the ClickHouse `latest` image at the time of the entry, even when no server ran in the entry.
- **Evidence ids (O-12):** plans make E-01 to E-19. E-15 to E-18 are the pending records of the four handoff files and stay pending. The user's reports and the orchestrator's publication steps are recorded from E-20 on, in the order they happen; an index row is added when its entry exists, so the index never has a gap.

## Index

| Entry | Requirement | Plan | Verdict |
|-------|-------------|------|---------|
| E-01 | RLS-01 | 38-01 | pass |
| E-02 | RLS-01 | 38-01 | pass |
| E-03 | RLS-01 | 38-01 | pass |
| E-04 | RLS-01 | 38-01 | pass |
| E-05 | RLS-02 | 38-02 | pass |
| E-06 | RLS-02 | 38-02 | pending (plan 38-02) |
| E-07 | RLS-02 | 38-02 | pending (plan 38-02) |
| E-08 | RLS-04 | 38-03 | pending (plan 38-03) |
| E-09 | RLS-04 | 38-03 | pending (plan 38-03) |
| E-10 | RLS-04 | 38-03 | pending (plan 38-03) |
| E-11 | RLS-03 | 38-04 | pending (plan 38-04) |
| E-12 | RLS-03, RLS-06 | 38-04 | pending (plan 38-04) |
| E-13 | RLS-06 | 38-04 | pending (plan 38-04) |
| E-14 | RLS-05 | 38-05 | pending (plan 38-05) |
| E-15 | RLS-05 | 38-05 | pending (plan 38-05) |
| E-16 | RLS-05 | 38-05 | pending (plan 38-05) |
| E-17 | RLS-05 | 38-05 | pending (plan 38-05) |
| E-18 | RLS-05 | 38-05 | pending (plan 38-05) |
| E-19 | RLS-01 to RLS-06 | 38-05 | pending (plan 38-05) |

### E-01: Pre-flight: origin and pull requests re-read, helpers checked, premise confirmed on the untouched tree

Backs: RLS-01, ROADMAP Phase 38 criterion 1 (the premise is re-read on the untouched tree and not carried from the research: origin has not moved, no gg6 tag exists anywhere, the client catalog is on 0.11.0, the anchors mirror is clean and the helpers behave as their plan says, all before any document or version file changes).

```sh
mkdir -p /tmp/gsd38/exec /tmp/gsd38/w
colima list
python3 --version
git rev-parse --abbrev-ref HEAD
git rev-parse HEAD
git status --porcelain
git log --format=%s | grep -qF 'docs(37): complete phase 37'
git ls-remote origin refs/heads/custom_build
git rev-list --count sync/v1.7.0..origin/custom_build
git rev-list --count 3a3a15a9ae064ffe09c696b8bb87bce23419d463..HEAD
git ls-remote --heads origin
git ls-remote --heads origin sync/v1.7.0
git ls-remote --tags origin 'v1.7.0_gg6*' 'pre-v1.7.0_gg6*'
git tag -l 'v1.7.0_gg6'
git rev-parse 'pre-v1.7.0_gg6^{commit}'
git rev-parse 'v1.7.0_gg5^{commit}'
gh api repos/ne1r0n/clickhouse-kafka-connect/releases --jq '[.[] | "\(.id) \(.tag_name) \(.draft)"] | join("; ")'
gh api 'repos/ne1r0n/clickhouse-kafka-connect/pulls?state=open&per_page=50' --jq '[.[] | "\(.number) \(.head.sha[0:8])"] | sort | join("; ")'
git ls-remote upstream refs/heads/main
git ls-remote --tags --refs upstream 'v*'
gh auth status
cat VERSION
grep -n 'clickHouseDriver' gradle/libs.versions.toml
python3 -I .planning/phases/38-release-publication/38-evidence-check.py anchors .planning/scripts/fork-anchors.txt
cmp .planning/phases/38-release-publication/38-evidence-check.py .planning/phases/37-ci-hardening-advisory-debt/37-evidence-check.py
bash .planning/phases/38-release-publication/38-assets/hygiene-selftest.sh.txt .planning/phases/38-release-publication/38-hygiene-check.py /tmp/gsd38/w/hygiene-selftest
python3 -I -c 'import sys;compile(open(sys.argv[1]).read(),sys.argv[1],"exec")' .planning/phases/38-release-publication/38-<helper>-check.py
/opt/homebrew/Cellar/docker/29.8.2/bin/docker run --rm --entrypoint clickhouse clickhouse/clickhouse-server:latest local --query 'SELECT version()'
/opt/homebrew/Cellar/docker/29.8.2/bin/docker image inspect --format '{{index .RepoDigests 0}}' clickhouse/clickhouse-server:latest
sysctl vm.swapusage
```

```text
branch: sync/v1.7.0
local_head: eb8c163ab9cdc7951612931f7c49b23e3ec37d6b
dirty_paths: M .planning/config.json; M .planning/state.json
colima_vz: Running
phase37_complete: yes
python_version: Python 3.14.8
origin_custom_build: 3a3a15a9ae064ffe09c696b8bb87bce23419d463	refs/heads/custom_build
origin_unchanged: yes
left_count: 0
ahead_of_gg5: 237
origin_heads: 011bc42b refs/heads/dependabot/gradle/com.google.protobuf-protobuf-java-4.36.2; 2a806038 refs/heads/dependabot/gradle/at.yawk.lz4-lz4-java-1.12.1; 3a3a15a9 refs/heads/custom_build; 6a53525b refs/heads/dependabot/gradle/clickHouseDriver-0.11.0; 8786723b refs/heads/dependabot/gradle/gradle-wrapper-9.8.2; 939e8992 refs/heads/scratch/clt-07-prebump-0.9.6; bec167d8 refs/heads/main; cb3c7718 refs/heads/dependabot/github_actions/github-actions-4430c5f07e; cb898dc2 refs/heads/dependabot/gradle/com.squareup.okhttp3-okhttp-5.5.1; de8c685b refs/heads/scratch/clt-07-client-0.11.0
origin_sync_ref: none
origin_gg6_tags: none
local_gg6_tag: none
pre_tag_local: 3a3a15a9ae064ffe09c696b8bb87bce23419d463
tag_gg5: 3a3a15a9ae064ffe09c696b8bb87bce23419d463
releases_on_origin: 426690002 v1.7.0_gg5 false
pr_heads: 10 011bc42b; 16 cb3c7718; 17 6a53525b; 18 cb898dc2; 19 2a806038; 20 8786723b
pr_heads_unchanged: yes
upstream_main: 9bed6f7970670a55c66be8efda246889e1f95bd0	refs/heads/main
upstream_newest_tag: v1.7.0
gh_auth: github.com
version_file: v1.7.0_gg5
anchors_version_row: VERSION|v1.7.0_gg5|1|BLD-07
anchors_mirror: rows=172 bad=0
catalog_client: clickHouseDriver = "0.11.0"  # FORK OVERRIDE (upstream 0.9.6)
clickhouse_version: 26.10.3.7
clickhouse_digest: clickhouse/clickhouse-server@sha256:739e099abd1157390960a59e4532d21781d8fde57e6e4151cb920ea02c30b1ce
digest_same_as_phase37: yes
swap_used: vm.swapusage: total = 17408.00M  used = 16075.06M  free = 1332.94M  (encrypted)
helper_evidence_identical_to_phase37: yes
hygiene_selftest: selftest_default_hits=7 selftest_strict_hits=9
helper_syntax: yes
assets_present: 2
testunit_baseline: not re-measured: Phase 38 changes no Java file, scope facts are checked in plan 38-04, expected testUnit total 1672 from the Phase 37 verifier
```

The six helpers and the two assets were already tracked at `local_head` (committed with the plan), so this evidence commit stages only the evidence file. The first syntax check used `py_compile` under `-I`, which ignores `PYTHONPYCACHEPREFIX` and left a `__pycache__` directory in the phase directory; it was removed file by file and the check was redone with `compile()`, which writes nothing. Origin holds two `scratch/clt-07-*` branches that belong to the user and are not touched. The swap reading is high (about 16 of 17 GB used), so an OpenJ9 crash in a later Gradle run is a known risk and not a test failure.

2026-10-31T12:44:00Z

Verdict: pass

### E-02: Tracer: VERSION bumped through the script with its anchors row, the generated version, the baseline tag and the anchors mirror agree

Backs: RLS-01, ROADMAP Phase 38 criterion 1 (the thinnest end-to-end path through the version layers on the real files: the bump script, one commit of `VERSION` with its anchors row, the clean build's generated constant, the baseline selection and the anchors mirror; no tag, no push, no Docker).

```sh
git tag --merged HEAD --list 'v*' | python3 -I .github/scripts/baseline_tag.py v1.7.0_gg6
python3 -I .github/scripts/bump_version.py gg --version-file /tmp/gsd38/w/bump/VERSION --anchors-file /tmp/gsd38/w/bump/fork-anchors.txt
git diff --no-index --numstat /tmp/gsd38/w/bump-orig/VERSION /tmp/gsd38/w/bump/VERSION
git diff --no-index --numstat /tmp/gsd38/w/bump-orig/fork-anchors.txt /tmp/gsd38/w/bump/fork-anchors.txt
python3 -I .github/scripts/bump_version.py gg
cat VERSION
git diff --name-only
git diff --numstat -- VERSION .planning/scripts/fork-anchors.txt
python3 -I .planning/phases/38-release-publication/38-evidence-check.py anchors .planning/scripts/fork-anchors.txt
env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain clean generateVersionFile > /tmp/gsd38/exec/version-clean.log 2>&1
grep ARTIFACT_VERSION build/generated/sources/version/java/main/com/clickhouse/kafka/connect/sink/Version.java
rm -rf build/test-results/testUnit
DOCKER_HOST=unix:///nonexistent/docker.sock env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain testUnit --rerun --tests '*MBeanNameUnitTest' --tests '*BumpVersionScriptUnitTest' --tests '*BaselineTagScriptUnitTest' > /tmp/gsd38/exec/bump-guards.log 2>&1
python3 -I .planning/phases/38-release-publication/38-evidence-check.py failed build/test-results/testUnit
python3 -I .planning/phases/38-release-publication/38-evidence-check.py junit build/test-results/testUnit
python3 -I .planning/phases/38-release-publication/38-evidence-check.py junit build/test-results/testUnit --class BumpVersionScriptUnitTest
python3 -I .planning/phases/38-release-publication/38-evidence-check.py junit build/test-results/testUnit --class MBeanNameUnitTest
python3 -I .planning/phases/38-release-publication/38-evidence-check.py junit build/test-results/testUnit --class BaselineTagScriptUnitTest
git add VERSION .planning/scripts/fork-anchors.txt && git commit -F /tmp/gsd38/exec/msg-bump.txt
git diff --name-only 70bf8108d1720c229453782dbae6250de0931eba^ 70bf8108d1720c229453782dbae6250de0931eba
git diff --quiet eb8c163ab9cdc7951612931f7c49b23e3ec37d6b HEAD -- src build.gradle.kts settings.gradle.kts gradle .github
```

```text
baseline_for_gg6: v1.7.0_gg5
scratch_bump_stdout: v1.7.0_gg6
scratch_bump_numstat: 1/1; 1/1
bump_stdout: v1.7.0_gg6
bump_version_file: v1.7.0_gg6
bump_changed_files: .planning/scripts/fork-anchors.txt; VERSION
bump_numstat: 1/1; 1/1
version_rows_in_anchors: 1
anchors_mirror_after: rows=172 bad=0
version_clean_exit: 0
generated_version_line: public static final String ARTIFACT_VERSION = "v1.7.0_gg6";
bump_guard_exit: 0
bump_guard_failed: OK
bump_guard_tally: OK junit build/test-results/testUnit: tests=82 failures=0 errors=0 skipped=0 files=3
bump_xml: OK junit build/test-results/testUnit class *BumpVersionScriptUnitTest: tests=52 failures=0 errors=0 skipped=0 files=1
mbean_xml: OK junit build/test-results/testUnit class *MBeanNameUnitTest: tests=11 failures=0 errors=0 skipped=0 files=1
baseline_xml: OK junit build/test-results/testUnit class *BaselineTagScriptUnitTest: tests=19 failures=0 errors=0 skipped=0 files=1
bump_commit: 70bf8108d1720c229453782dbae6250de0931eba
bump_commit_files: .planning/scripts/fork-anchors.txt; VERSION
allowlist_gg6_present: no
local_gg6_tag_absent: yes
src_unchanged: yes
hygiene_scan: clean
```

The baseline selection still names `v1.7.0_gg5` after the bump, the scratch rehearsal and the real bump changed one line in each of the two files, and the clean build read back the new constant (the generated file is not an input of `generateVersionFile`, so `clean` is required). The three Docker-free classes that touch the version ran with `DOCKER_HOST` pointing nowhere: 82 tests, no failure, no JVM crash file.

2026-10-31T12:45:00Z

Verdict: pass

### E-03: The 1.7.0_gg6 CHANGELOG section absorbs upstream's Unreleased entry and leaves every released section untouched

Backs: RLS-01, ROADMAP Phase 38 criterion 1 (a complete dated `1.7.0_gg6` section, upstream's `Unreleased` entry inside it verbatim, no `Unreleased` heading left, every released section byte-identical, no planning identifier and no fact about the release object in the section).

```sh
date -u +%F
git show eb8c163ab9cdc7951612931f7c49b23e3ec37d6b:CHANGELOG.md > /tmp/gsd38/exec/changelog-old.md
python3 -I <assemble>   # section text with the date and the old Unreleased entry, then the old file minus its Unreleased block
python3 -I .planning/phases/38-release-publication/38-doc-check.py changelog CHANGELOG.md /tmp/gsd38/exec/changelog-old.md --date 2026-10-31 --require 9bed6f79 0.11.0 1.12.1 GHSA-48mj-8f7c-7qfm additionalRetriableErrorCodes errors.tolerance custom_ zstd-jni v1.7.0_gg6 ROLLOUT-v1.7.0_gg6 FORK-DELTA issues/777 retryOnSocketException
grep -c '^# Unreleased' CHANGELOG.md
git diff -U0 eb8c163ab9cdc7951612931f7c49b23e3ec37d6b -- CHANGELOG.md | grep -E '^\+' | grep -vE '^\+\+\+' | sed 's/^+//' > /tmp/gsd38/exec/changelog-added.txt
python3 -I .planning/phases/38-release-publication/38-hygiene-check.py --strict /tmp/gsd38/exec/changelog-added.txt
python3 -I .planning/phases/38-release-publication/38-evidence-check.py anchors .planning/scripts/fork-anchors.txt --rows 172
git add CHANGELOG.md && git commit -F /tmp/gsd38/exec/msg-changelog.txt
git diff --name-only fdf3be31016acccdc0813ddca0ee6063dd278d39^ fdf3be31016acccdc0813ddca0ee6063dd278d39
```

```text
changelog_date: 2026-10-31
changelog_check: OK changelog: section_lines=71 required=13
unreleased_headings: 0
released_sections_identical: yes
link_to_note_pending: yes
hygiene_scan_added_lines: clean
secrets_scan: clean
anchors_mirror_after: rows=172 bad=0
changelog_commit: fdf3be31016acccdc0813ddca0ee6063dd278d39
changelog_commit_files: CHANGELOG.md
```

The section was assembled by script from a section template and the old file, so the rest of the file equals the phase-start file minus the `Unreleased` block (`released_sections_identical` is the helper's tail comparison). The link to `docs/ROLLOUT-v1.7.0_gg6.md` has no target yet; plan 38-02 creates it and its gate runs the link check. The date is the UTC date at writing and is re-checked against the merge date at the user's gate. Correction of this file: the timestamp lines of E-01 and E-02, which the first two commits wrote as estimates, were replaced by the commit times read from `git log` (12:44 and 12:45 UTC); no fact or verdict changed.

2026-10-31T12:47:18Z

Verdict: pass

### E-04: docs/FORK-DELTA.md is final for gg6: pointers, outcomes in place of forward references, raw numbers refreshed once, open items with owners

Backs: RLS-01, ROADMAP Phase 38 criterion 1 (the final fork ledger: gg6 sentences and the note pointer, forward references replaced by outcomes or conditions, the two raw-number commands re-run once on the release tip with the gg6 rows added, the counts derived from the comparison with `v1.7.0` recounted by the file's own method, and the new open items with an owner and a re-open trigger).

```sh
git show eb8c163ab9cdc7951612931f7c49b23e3ec37d6b:docs/FORK-DELTA.md | grep -c gg6
git show eb8c163ab9cdc7951612931f7c49b23e3ec37d6b:docs/FORK-DELTA.md | grep -c 'Phase 38'
git show eb8c163ab9cdc7951612931f7c49b23e3ec37d6b:docs/FORK-DELTA.md | wc -l
grep -nE 'Phase 3[6-9]|next phase|later phase|will be' docs/FORK-DELTA.md
git diff --shortstat v1.7.0 HEAD -- src build.gradle.kts gradle .github | python3 -I .planning/phases/38-release-publication/38-doc-check.py shortstat
git diff --shortstat v1.7.0 HEAD -- src/main | python3 -I .planning/phases/38-release-publication/38-doc-check.py shortstat
git diff --shortstat v1.7.0_gg5 HEAD -- src build.gradle.kts gradle .github | python3 -I .planning/phases/38-release-publication/38-doc-check.py shortstat
git diff --shortstat v1.7.0_gg5 HEAD -- src/main | python3 -I .planning/phases/38-release-publication/38-doc-check.py shortstat
for p in src/main src/test src/testFixtures src/integrationTest .github build.gradle.kts gradle; do git diff --shortstat v1.7.0_gg5 HEAD -- $p; done
python3 -I /tmp/gsd38/exec/recount.py HEAD      # recount by the file's method: pairs compared with import lines and blank lines removed
python3 -I /tmp/gsd38/exec/recount.py v1.7.0_gg5
python3 -I .planning/phases/38-release-publication/38-doc-check.py numbers docs/FORK-DELTA.md 'after gg6: `v1.7.0` to `v1.7.0_gg6`' --wider "331 39665 1161" --narrow "40 2989 272"
python3 -I .planning/phases/38-release-publication/38-doc-check.py links docs/FORK-DELTA.md
git diff --numstat eb8c163ab9cdc7951612931f7c49b23e3ec37d6b HEAD -- docs/FORK-DELTA.md
git diff -U0 eb8c163ab9cdc7951612931f7c49b23e3ec37d6b -- docs/FORK-DELTA.md | grep -E '^\+' | grep -vE '^\+\+\+' | sed 's/^+//' > /tmp/gsd38/exec/forkdelta-added.txt
python3 -I .planning/phases/38-release-publication/38-hygiene-check.py --strict /tmp/gsd38/exec/forkdelta-added.txt
python3 -I .planning/phases/38-release-publication/38-evidence-check.py anchors .planning/scripts/fork-anchors.txt --rows 172
git add docs/FORK-DELTA.md && git commit -F /tmp/gsd38/exec/msg-forkdelta.txt
```

```text
forkdelta_gg6_mentions_before: 2
forkdelta_phase38_refs_before: 2
forkdelta_lines_before: 264
wider_now: 331 39665 1161
narrow_now: 40 2989 272
wider_since_gg5: 79 7939 478
narrow_since_gg5: 7 112 31
numbers_check: OK numbers: label 'after gg6: `v1.7.0` to `v1.7.0_gg6`'
import_only_recount: 14 of 40
identical_files_recount: 33 of 63
recount_method_check: the same script at v1.7.0_gg5 gives 36 identical, 14 import-only, 13 modified and 10 new, the figures the file published for gg5
forkdelta_gg6_mentions_after: 10
forkdelta_phase38_refs_after: 0
forkdelta_lines_after: 294
forkdelta_numstat: 42 12
stale_spots_done: 2a intro clause; 2b rollout pointer and older-notes order; 2c MBean and VERSION rows; 3 gg6 syncer paragraph and tail checklists in the reading order; 4 open item 8; 5a protobuf row and open item 12; 5c compare-red sentence; 6 raw numbers, growth paragraph and recounts; 7 open items
forkdelta_forward_refs_note: 5b found no sentence saying that no github-actions pull request was observed (the row already names PR 16 as the first observed one and says Phase 33 absorbed it), so that row is unchanged; 5c judged every grep hit for Phase 3[6-9], next phase, later phase and will be: the hits that remain name work already done (Phase 37 moved the compare script, Phase 36 research) and are left, the compare-red sentence was replaced by a condition and an outcome
open_items_added: 11
links_ok: 1 broken target, docs/ROLLOUT-v1.7.0_gg6.md at line 7, created by plan 38-02
hygiene_scan_added_lines: clean
secrets_scan: clean
anchors_mirror_after: rows=172 bad=0
forkdelta_commit: 21697379e188f163b1f2f401642be1a7401a1ada
forkdelta_commit_files: docs/FORK-DELTA.md
```

The numbers equal the planning-time values. The two commands ran on the release tip (`HEAD` of `sync/v1.7.0`); the scopes contain no file that a documentation commit can move, and the since-gg5 directory split sums to the since-gg5 total (79 files, 7939 insertions, 478 deletions: `src/main` 7, `src/test` 52, `src/testFixtures` 4, `src/integrationTest` 4, `.github` 10, `gradle` 2 files). The recount script is a scratch file outside the repository that applies the file's own method (a pair counts as import-only when the lines that are not blank and do not start with `import ` are equal); run at `v1.7.0_gg5` it reproduces the figures the file published for gg5, which is why its result for the tip is used: 33 byte-identical, 14 import-only, 16 modified and 10 new files, 40 differing in all. The three files that moved from byte-identical to modified since gg5 are `AtLeastOnceBufferStrategy`, `ChunkFlusher` and `ExactlyOnceBufferStrategy`. The two PR-state sentences (the protobuf row and open item 12) now state a condition and no outcome. The Dependabot pull request 17 and the publish-path caveat are items 22 and 23, stated as conditions. Link check: the one broken target is the rollout note, created by plan 38-02; the paragraph for the next syncer names the note in code form, so exactly one link line is broken. The edits were made with the Edit tool, except one scripted batch of four single-occurrence replacements (each asserted to match exactly once) that reworded items 14, 15 and 21 and the item range in the paragraph for the next syncer.

2026-10-31T12:52:59Z

Verdict: pass

### E-05: The cheap local re-measures: archives built on the release tip, eight pin classes green, the client sources read, the custom_ rejection measured, the final labels

Backs: RLS-02, ROADMAP Phase 38 criterion 2 (the rollout note's labels are decided from what Phase 38 itself measured or read: both archives rebuilt the way the release workflow builds them, eight Docker-free pin classes green on the release tip, the public 0.11.0 and 0.9.6 client sources read for the rows that were measured only in research, one throwaway-container request for the `custom_` rejection, and the final label of every research-only row).

```sh
git archive v1.7.0_gg5 | tar -x -C /tmp/gsd38/w/base-src
git archive HEAD | tar -x -C /tmp/gsd38/w/tip-src
( cd /tmp/gsd38/w/base-src && env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain createConfluentArchive > /tmp/gsd38/exec/build-base.log 2>&1 )
( cd /tmp/gsd38/w/tip-src && env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain createConfluentArchive > /tmp/gsd38/exec/build-tip.log 2>&1 )
cp /tmp/gsd38/w/base-src/build/confluent/clickhouse-kafka-connect-v1.7.0_gg5.zip /tmp/gsd38/w/arch/base.zip
cp /tmp/gsd38/w/tip-src/build/confluent/clickhouse-kafka-connect-v1.7.0_gg6.zip /tmp/gsd38/w/arch/tip.zip
shasum -a 256 /tmp/gsd38/w/arch/base.zip /tmp/gsd38/w/arch/tip.zip
python3 -I .github/scripts/compare_confluent_archives.py /tmp/gsd38/w/arch/base.zip /tmp/gsd38/w/arch/tip.zip /tmp/gsd38/w/arch/empty.txt > /tmp/gsd38/exec/cmp-empty-note.out
rm -rf build/test-results/testUnit
DOCKER_HOST=unix:///nonexistent/docker.sock env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain testUnit --rerun --tests '*BufferedOffsetCommitUnitTest' --tests '*AdditionalRetriableErrorCodesPinUnitTest' --tests '*ClientEndpointAndOptionsPinUnitTest' --tests '*ClientErrorShapePinUnitTest' --tests '*ClientInsertWirePinUnitTest' --tests '*ClientTimeoutPrefixPinUnitTest' --tests '*ClientCatalogPinUnitTest' --tests '*NetworkCompressionPinUnitTest' > /tmp/gsd38/exec/pins.log 2>&1
python3 -I .planning/phases/38-release-publication/38-evidence-check.py failed build/test-results/testUnit
python3 -I .planning/phases/38-release-publication/38-evidence-check.py junit build/test-results/testUnit --class <ClassName> 'tests>=1' failures=0 errors=0
curl -sSfL -o /tmp/gsd38/w/src011/client-v2-0.11.0-sources.jar https://repo1.maven.org/maven2/com/clickhouse/client-v2/0.11.0/client-v2-0.11.0-sources.jar
curl -sSfL -o /tmp/gsd38/w/src096/client-v2-0.9.6-sources.jar https://repo1.maven.org/maven2/com/clickhouse/client-v2/0.9.6/client-v2-0.9.6-sources.jar
python3 -I /tmp/gsd38/exec/readsrc.py /tmp/gsd38/w/src011/client-v2-0.11.0-sources.jar '<search strings>'
python3 -I /tmp/gsd38/exec/readsrc.py /tmp/gsd38/w/src096/client-v2-0.9.6-sources.jar '<search strings>'
docker image inspect --format '{{index .RepoDigests 0}}' clickhouse/clickhouse-server:latest
docker run -d --rm --pull=never --name gsd38-note-probe -e CLICKHOUSE_SKIP_USER_SETUP=1 -p 127.0.0.1:18123:8123 clickhouse/clickhouse-server@sha256:739e099abd1157390960a59e4532d21781d8fde57e6e4151cb920ea02c30b1ce
curl -s http://127.0.0.1:18123/ping
curl -s -G http://127.0.0.1:18123/ --data-urlencode 'query=SELECT 1'
curl -s -G http://127.0.0.1:18123/ --data-urlencode 'query=SELECT 1' --data-urlencode 'custom_myparam=7'
docker rm -f gsd38-note-probe
docker ps -a --filter name=gsd38-note-probe -q
docker image inspect --format '{{index .RepoDigests 0}}' clickhouse/clickhouse-server:latest
rm -rf /tmp/gsd38/w/base-src /tmp/gsd38/w/tip-src
```

```text
base_verify_line: verifyConfluentArchive: 7801 inner entries, no nested jar, no Kafka API class
tip_verify_line: verifyConfluentArchive: 7933 inner entries, no nested jar, no Kafka API class
base_entries: 7801
tip_entries: 7933
entries_delta: 132
base_bytes: 12453556
tip_bytes: 13558634
base_sha256: efa6cca4f30d182eea52565f331581f2c4ccee30c99e91ce0f053b341629e0fe
tip_sha256: a1b9108859fc73b84ad6ac6931c387bd10800dde9fec8b6a488e8bb5b402f1cf
compare_exit: 1
compare_totals: totals files-a 7399 files-b 7513 only-a 4 only-b 118 changed 120 same 7275
compare_unallowed: 242
compare_version_table: VERSION at.yawk.lz4:lz4-java 1.12.0 -> 1.12.1
compare_version_lines_total: 18
compare_manifest: MANIFEST.MF: differs only in Implementation-Version
compare_nested_jars: nested jars in b: 0
compare_kafka_classes: kafka classes in b: 0
archive_matches_research: yes
pins_exit: 0
pins_failed: OK
buffered_xml: OK junit build/test-results/testUnit class *BufferedOffsetCommitUnitTest: tests=15 failures=0 errors=0 skipped=0 files=1
retriable_xml: OK junit build/test-results/testUnit class *AdditionalRetriableErrorCodesPinUnitTest: tests=19 failures=0 errors=0 skipped=0 files=1
endpoint_xml: OK junit build/test-results/testUnit class *ClientEndpointAndOptionsPinUnitTest: tests=4 failures=0 errors=0 skipped=0 files=1
shape_xml: OK junit build/test-results/testUnit class *ClientErrorShapePinUnitTest: tests=7 failures=0 errors=0 skipped=0 files=1
wire_xml: OK junit build/test-results/testUnit class *ClientInsertWirePinUnitTest: tests=2 failures=0 errors=0 skipped=0 files=1
timeout_xml: OK junit build/test-results/testUnit class *ClientTimeoutPrefixPinUnitTest: tests=2 failures=0 errors=0 skipped=0 files=1
catalog_xml: OK junit build/test-results/testUnit class *ClientCatalogPinUnitTest: tests=2 failures=0 errors=0 skipped=0 files=1
compression_xml: OK junit build/test-results/testUnit class *NetworkCompressionPinUnitTest: tests=9 failures=0 errors=0 skipped=0 files=1
pins_total: OK junit build/test-results/testUnit: tests=60 failures=0 errors=0 skipped=0 files=8
pins_map: buffered: B1, B5; retriable: C1; endpoint: D1; shape: D2 attempts, D3, D4; wire: D5 prefix, D11; timeout: D14; catalog: D12 and the 0.11.0 catalog line; compression: D13
source_jar: client-v2-0.11.0-sources.jar downloaded, read as data with python3 -I and zipfile, never extracted
ua_site: com/clickhouse/client/api/Client.java:2628 CLIENT_USER_AGENT = "clickhouse-java-v2/"; com/clickhouse/client/api/internal/WorkloadIdentitySupport.java:34-38 keys lv, os, hc; :157-160 putIfAbsent of lv:java/<java.version>, os:<os.name>, hc:ahc_<Apache HttpClient version>
ua_format_found: yes
ua_old_format_site: client-v2-0.9.6-sources.jar com/clickhouse/client/api/internal/HttpAPIClientHelper.java:781 appends jvm: and :805 appends Apache-HttpClient
log_sites: com/clickhouse/client/api/Client.java:2745 requestExMsg builds operation + " request failed (attempt: n, duration: Nms, queryId: id)" and :1663, :1678, :1889, :1912 pass "Insert"; com/clickhouse/client/api/internal/HttpAPIClientHelper.java:431 and :534 append " (queryId= <id>)"; :430, :473, :532 carry "transport error: <status>"
log_text_found: yes
log_old_text_site: client-v2-0.9.6-sources.jar com/clickhouse/client/api/Client.java:1496 "Insert failed (Attempt: %s/%s - Duration: %s)"
compression_default_site: com/clickhouse/client/api/ClientConfigProperties.java:105 COMPRESSION_METHOD("compression.method", CompressionMethod.class, CompressionMethod.ZSTD.name())
compression_default_value: ZSTD
socket_options_site: com/clickhouse/client/api/ClientConfigProperties.java:78 SOCKET_RCVBUF_OPT("socket_rcvbuf", Integer.class) and :85 SOCKET_SNDBUF_OPT("socket_sndbuf", Integer.class); applied only if set at com/clickhouse/client/api/internal/HttpAPIClientHelper.java:359-362
socket_options_default: none
socket_options_old_default: client-v2-0.9.6-sources.jar com/clickhouse/client/api/ClientConfigProperties.java:68 and :70 declare both keys with the default 804800, applied at HttpAPIClientHelper.java:278
probe_image: clickhouse/clickhouse-server@sha256:739e099abd1157390960a59e4532d21781d8fde57e6e4151cb920ea02c30b1ce
probe_server_version: 26.10.3.7
probe_control_response: 1
d7_rejection_text: Code: 115. DB::Exception: Setting custom_myparam is neither a builtin setting nor started with the prefix 'SQL_' registered for user-defined settings. (UNKNOWN_
d7_rejection_measured: yes
probe_container_removed: yes
image_digest_after: clickhouse/clickhouse-server@sha256:739e099abd1157390960a59e4532d21781d8fde57e6e4151cb920ea02c30b1ce
image_unchanged: yes
label_d2: attempts: measured; cadence: measured in research (not re-run)
label_d5: prefix: measured; tail: read from the code
label_d6: read from the code
label_d7: forwarding: measured; rejection: measured
label_d8: default method: read from the code; client-side failure: measured in research (not re-run)
label_d9: archive and natives: measured; worker behaviour: measured in research (not re-run); loader behaviour: read from the code
label_d12: measured
relabel_reasons: d5 tail from research to read from the code by ua_format_found (the strings lv:java, os: and hc:ahc_ are in WorkloadIdentitySupport of 0.11.0 and the old jvm: and Apache-HttpClient text in 0.9.6); d6 from research to read from the code by log_text_found and log_old_text_site (both the new text and the old text are in the public sources, nothing was run); d7 rejection from research to measured by d7_rejection_measured (a real server answered Code 115 UNKNOWN_SETTING); d8 default method from research to read from the code by compression_default_value ZSTD, the client-side failure keeps its research label; d2 cadence, d8 failure and d9 worker behaviour keep measured in research (not re-run) because they need a server with a one-second max_execution_time on both client lines, a body of 1000000 incompressible bytes through a probe, and a Connect worker image with a platform matrix; d10 stays read from the code and is now backed on both lines by socket_options_site and socket_options_old_default
exports_removed: yes
```

Both archives came out equal to the planning-time research on every figure (entries, bytes, digests, compare totals and the 242 unallowed lines), so the research figures and the measured ones are the same. The `compare_version_table` key holds the one version that differs: the compare tool prints one `VERSION` line per third-party library (18 lines, `compare_version_lines_total`) and 17 of them read the same version on both sides. A digest is recorded only; it is never compared with the runner's or the published asset's digest.

The probe container needed `CLICKHOUSE_SKIP_USER_SETUP=1`: without it the image's `default` user refuses a request that arrives through the published port (error 194, a password is required), for any query, with or without the `custom_` parameter. With it the control query `SELECT 1` answers `1` and the same query with `custom_myparam=7` answers error 115. The container ran by digest with `--pull=never` and published on `127.0.0.1` only, nothing left the machine, and `docker rm -f` removed it. A first attempt without the variable (also loopback-only, also removed) answered 194 and is not used.

The 0.9.6 sources were read in addition to the plan's 0.11.0 jar because the rows D5, D6 and D10 each make a claim about the old line too, and a label must not be stronger than what was read. The row D9's loader behaviour and the D8 failure text were not touched by the sources of the client and keep their labels.

2026-10-31T12:58:19Z

Verdict: pass
