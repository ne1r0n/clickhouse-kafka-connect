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
| E-03 | RLS-01 | 38-01 | pending (plan 38-01) |
| E-04 | RLS-01 | 38-01 | pending (plan 38-01) |
| E-05 | RLS-02 | 38-02 | pending (plan 38-02) |
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

2026-10-31T12:46:00Z

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

2026-10-31T12:55:00Z

Verdict: pass
