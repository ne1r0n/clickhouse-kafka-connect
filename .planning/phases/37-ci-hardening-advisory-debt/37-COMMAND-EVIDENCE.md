# Phase 37: Command evidence

- **Base commit:** `local_head` = `ff8730860ba8560171fcda44bc1cdcd6dba1ba2a` (HEAD when plan 37-01 started, branch `sync/v1.7.0`).
- **Shell:** macOS zsh, multi-line logic under `/bin/bash`. Gradle is run as `env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain ...`.
- **Docker:** colima profile `vz` (aarch64, 8 CPU, 16 GiB, 256 GiB disk), docker CLI from `/opt/homebrew/Cellar/docker/29.8.2/bin/docker`.
- **Date (UTC):** 2026-10-31.
- **Entry format:** each entry has a `### E-NN: {title}` heading, a `Backs:` line, an `sh` fence with the commands as run, a `text` fence of `key: value` lines (keys match `[a-z0-9_]+`, one fact per line) with free text allowed after the fence, a UTC timestamp line, and a final line `Verdict: pass`, `Verdict: fail`, `Verdict: pending ({reason})` or `Verdict: unmeasured ({reason})`.
- **Rule:** a fail or pending verdict is fixed by re-measuring in a new run, never by editing the evidence.
- **Helpers:** `37-evidence-check.py` in this directory is the read-only gate helper (a byte-identical copy of Phase 36's; get, expect, index, rows, secrets, junit, anchors, skips, failed) and `37-ledger-check.py` is the read-only ledger helper of this phase; neither is ever edited.
- **Server version:** E-01 and E-18 record `clickhouse_version`, the `SELECT version()` of the ClickHouse `latest` image at the time of the entry, even when no server ran in the entry.

## Index

| Entry | Requirement | Plan | Verdict |
|-------|-------------|------|---------|
| E-01 | CIH-01, CIH-02, CIH-03 | 37-01 | pass |
| E-02 | CIH-01, CIH-02, CIH-03 | 37-01 | pass |
| E-03 | CIH-01, CIH-03 | 37-01 | pass |
| E-04 | CIH-02 | 37-01 | pending (plan 37-01) |
| E-05 | CIH-05 | 37-02 | pending (plan 37-02) |
| E-06 | CIH-04 | 37-02 | pending (plan 37-02) |
| E-07 | CIH-04, CIH-05 | 37-02 | pending (plan 37-02) |
| E-08 | ADV-03 | 37-02 | pending (plan 37-02) |
| E-09 | CIH-04, ADV-01 | 37-03 | pending (plan 37-03) |
| E-10 | CIH-04, ADV-01, ADV-02 | 37-03 | pending (plan 37-03) |
| E-11 | CIH-04 | 37-03 | pending (plan 37-03) |
| E-12 | CIH-04 | 37-03 | pending (plan 37-03) |
| E-13 | ADV-01 | 37-04 | pending (plan 37-04) |
| E-14 | ADV-02 | 37-04 | pending (plan 37-04) |
| E-15 | ADV-02 | 37-04 | pending (plan 37-04) |
| E-16 | ADV-01, ADV-02 | 37-05 | pending (plan 37-05) |
| E-17 | ADV-02, ADV-03 | 37-05 | pending (plan 37-05) |
| E-18 | CIH-01 to CIH-05, ADV-01 to ADV-03 | 37-05 | pending (plan 37-05) |

### E-01: Pre-flight: origin re-read, Phase 36 complete, baselines on the untouched tree

Backs: CIH-01, CIH-02, CIH-03, ROADMAP Phase 37 criterion 1 (the premise re-read before any code lands, and a green baseline re-measured on the untouched tree so every later red or green is attributable to this phase).

```sh
colima list
python3 --version
git rev-parse --abbrev-ref HEAD
git rev-parse HEAD
git status --porcelain
git log --format=%s | grep -qF 'docs(36): complete phase 36'
git ls-remote origin refs/heads/custom_build
git ls-remote --heads origin 'scratch/*'
git ls-remote --heads origin sync/v1.7.0
git rev-parse 'v1.7.0_gg4^{commit}'
git rev-parse 'v1.7.0_gg5^{commit}'
gh auth status
cat VERSION
python3 -I .planning/phases/37-ci-hardening-advisory-debt/37-evidence-check.py anchors .planning/scripts/fork-anchors.txt
cmp .planning/phases/37-ci-hardening-advisory-debt/37-evidence-check.py .planning/phases/36-client-0-11-0-audit-bump/36-evidence-check.py
cmp .planning/phases/37-ci-hardening-advisory-debt/37-hygiene-check.py .planning/phases/36-client-0-11-0-audit-bump/36-hygiene-check.py
git ls-files src/main | wc -l
docker run --rm --entrypoint clickhouse clickhouse/clickhouse-server:latest local --query 'SELECT version()'
docker image inspect --format '{{index .RepoDigests 0}}' clickhouse/clickhouse-server:latest
rm -rf build/test-results/testUnit
env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain testUnit --rerun > /tmp/gsd37/exec/testunit-baseline.log 2>&1
python3 -I .planning/phases/37-ci-hardening-advisory-debt/37-evidence-check.py junit build/test-results/testUnit
python3 -I .planning/phases/37-ci-hardening-advisory-debt/37-evidence-check.py junit build/test-results/testUnit --class <ClassName>
python3 -I .planning/phases/37-ci-hardening-advisory-debt/37-evidence-check.py failed build/test-results/testUnit
```

```text
branch: sync/v1.7.0
local_head: ff8730860ba8560171fcda44bc1cdcd6dba1ba2a
dirty_paths: M .planning/config.json; M .planning/state.json
colima_vz: Running
phase36_complete: yes
python_version: Python 3.14.8
origin_custom_build: 3a3a15a9ae064ffe09c696b8bb87bce23419d463	refs/heads/custom_build
origin_unchanged: yes
origin_scratch_refs: de8c685b80d40b0a6e19338271e07b08ede84f3b refs/heads/scratch/clt-07-client-0.11.0; 939e899298c2f12dec2cfe1e69b1a6b53b4f511e refs/heads/scratch/clt-07-prebump-0.9.6
origin_sync_ref: none
tag_gg4: 21c51598b06b6f5af58430765d57b1e5f6747c5a
tag_gg5: 3a3a15a9ae064ffe09c696b8bb87bce23419d463
gh_auth: github.com
version_file: v1.7.0_gg5
anchors_version_row: VERSION|v1.7.0_gg5|1|BLD-07
anchors_mirror: rows=152 bad=0
helper_identical_to_phase36: yes
hygiene_helper_identical_to_phase36: yes
assets_present: 16
src_main_files: 73
clickhouse_version: 26.10.3.7
clickhouse_digest: clickhouse/clickhouse-server@sha256:739e099abd1157390960a59e4532d21781d8fde57e6e4151cb920ea02c30b1ce
baseline_testunit_exit: 0
baseline_testunit_tests: 1475
baseline_testunit_failures: 0
baseline_testunit_errors: 0
baseline_testunit_skipped: 2
baseline_pinguard_tests: 39
baseline_actionpin_tests: 21
baseline_failedsummary_tests: 8
baseline_bytesguard_tests: 9
baseline_archivecheck_tests: 6
baseline_connectapi_tests: 3
baseline_columntest_tests: 50
baseline_describeunreadable_tests: 2
baseline_jvm_crash: none
```

The planning-time research found no `scratch/*` ref on origin; two now exist (`scratch/clt-07-client-0.11.0` and `scratch/clt-07-prebump-0.9.6`, the Phase 36 CLT-07 scratch refs). They are recorded, not a stop condition: this phase merges nothing and pushes nothing. `origin_custom_build` still equals the `v1.7.0_gg5` tag. The phase helper files and the 16 assets were already tracked at `local_head` (committed with the plan), so this evidence commit stages only the evidence file. The untouched testUnit run reports 2 skipped tests; the 18 skips of the full suite belong to the `test` task, not to `testUnit`.

2026-10-31T08:52:00Z

Verdict: pass

### E-02: Tracer: the bump script, red first, the workflow wired to it, and the real files bumped end to end

Backs: CIH-01, CIH-02, CIH-03, ROADMAP Phase 37 criteria 1 and 2 (the thinnest end-to-end path of the tooling chain: the script, a first table test seen red before the script existed, the workflow that calls it, the anchors rows and the docs row, run on the real files).

```sh
rm -rf build/test-results/testUnit
env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain testUnit --rerun --tests '*BumpVersionScriptUnitTest' > /tmp/gsd37/exec/tracer-red.log 2>&1
cp .planning/phases/37-ci-hardening-advisory-debt/37-assets/bump_version.py.txt .github/scripts/bump_version.py
chmod +x .github/scripts/bump_version.py
cmp .github/scripts/bump_version.py .planning/phases/37-ci-hardening-advisory-debt/37-assets/bump_version.py.txt
env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain testUnit --rerun --tests '*BumpVersionScriptUnitTest' > /tmp/gsd37/exec/tracer-green.log 2>&1
git worktree add --detach /tmp/gsd37/w/fmt HEAD
env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew -p /tmp/gsd37/w/fmt --console=plain spotlessApply
cmp src/test/java/com/clickhouse/kafka/connect/ci/BumpVersionScriptUnitTest.java /tmp/gsd37/w/fmt/src/test/java/com/clickhouse/kafka/connect/ci/BumpVersionScriptUnitTest.java
git worktree remove --force /tmp/gsd37/w/fmt
cp .planning/phases/37-ci-hardening-advisory-debt/37-assets/increment-version.yml.txt .github/workflows/increment-version.yml
ruby -ryaml -e 'YAML.load_file(ARGV[0])' .github/workflows/increment-version.yml
python3 -I .planning/phases/37-ci-hardening-advisory-debt/37-assets/edit_lines.py.txt replace-line .planning/scripts/fork-anchors.txt .planning/phases/37-ci-hardening-advisory-debt/37-assets/anchors-rows-plan01.txt "$(printf '.github/workflows/increment-version.yml\tnode-version')"
python3 -I .planning/phases/37-ci-hardening-advisory-debt/37-assets/edit_lines.py.txt replace-line docs/FORK-DELTA.md .planning/phases/37-ci-hardening-advisory-debt/37-assets/fork-delta-row-1-increment-version.md.txt '| `increment-version.yml` is hardened'
git worktree add --detach /tmp/gsd37/w/e2e HEAD
ruby -ryaml -e 'y=YAML.load_file(ARGV[0]); s=y["jobs"]["build"]["steps"].find{|x| x["name"]=="Increment version"}; puts s["run"]' .github/workflows/increment-version.yml > /tmp/gsd37/exec/step-increment.sh
cd /tmp/gsd37/w/e2e && env BUMP=gg GITHUB_ENV=/tmp/gsd37/exec/e2e-github-env bash --noprofile --norc -eo pipefail /tmp/gsd37/exec/step-increment.sh
git -C /tmp/gsd37/w/e2e diff --name-only
git -C /tmp/gsd37/w/e2e diff --numstat
cd /tmp/gsd37/w/e2e && python3 -I <repo>/.planning/phases/37-ci-hardening-advisory-debt/37-evidence-check.py anchors .planning/scripts/fork-anchors.txt
git -C /tmp/gsd37/w/e2e checkout -- .
printf 'v1.7.0_gg6\n' > /tmp/gsd37/w/e2e/VERSION
cd /tmp/gsd37/w/e2e && python3 -I <repo>/.planning/phases/37-ci-hardening-advisory-debt/37-evidence-check.py anchors .planning/scripts/fork-anchors.txt
git worktree remove --force /tmp/gsd37/w/e2e
git worktree prune
env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain testUnit --rerun --tests '*BumpVersionScriptUnitTest' --tests '*WorkflowActionPinGuardUnitTest' --tests '*TestsWorkflowPinGuardUnitTest' > /tmp/gsd37/exec/tracer-guards.log 2>&1
python3 -I .planning/phases/37-ci-hardening-advisory-debt/37-evidence-check.py junit build/test-results/testUnit --class <ClassName>
python3 -I .planning/phases/37-ci-hardening-advisory-debt/37-evidence-check.py failed build/test-results/testUnit
git diff --quiet ff8730860ba8560171fcda44bc1cdcd6dba1ba2a HEAD -- src/main
git grep -n -I -E '<the three identifier words of the hygiene rule>'
```

```text
assets_identical: yes
tracer_red_exit: 1
tracer_red_junit: OK junit build/test-results/testUnit class *BumpVersionScriptUnitTest: tests=2 failures=2 errors=0 skipped=0 files=1
tracer_red_reason: script missing
tracer_green_exit: 0
tracer_green_junit: OK junit build/test-results/testUnit class *BumpVersionScriptUnitTest: tests=2 failures=0 errors=0 skipped=0 files=1
spotless_new_file_cmp: identical
script_commit: 1f2ba9f359e9819f6cf1707a5b5c13327914a69d
script_commit_files: .github/scripts/bump_version.py; src/test/java/com/clickhouse/kafka/connect/ci/BumpVersionScriptUnitTest.java
anchors_rows_before: rows=152
anchors_rows_after: rows=157
anchors_mirror_after_edit: rows=157 bad=0
forkdelta_numstat: 1/1
forkdelta_defect_sentence_gone: yes
workflow_commit: 501d069644b452db7405aee96a9c9e9df056bce0
workflow_commit_files: .github/workflows/increment-version.yml; .planning/scripts/fork-anchors.txt; docs/FORK-DELTA.md
e2e_exit: 0
e2e_new_version: v1.7.0_gg6
e2e_version_file: v1.7.0_gg6
e2e_changed_files: .planning/scripts/fork-anchors.txt; VERSION
e2e_numstat: 1/1; 1/1
e2e_anchors_after: rows=157 bad=0
e2e_version_only_anchors: rows=157 bad=1
e2e_version_only_moved: ANCHOR MOVED VERSION [v1.7.0_gg5] expected 1 got 0
e2e_worktree_removed: yes
guards_exit: 0
guards_failed: OK failed: 0 failing testcases, 0 expected
guards_tallies: BumpVersionScriptUnitTest tests=2 failures=0 errors=0 skipped=0; WorkflowActionPinGuardUnitTest tests=21 failures=0 errors=0 skipped=0; TestsWorkflowPinGuardUnitTest tests=39 failures=0 errors=0 skipped=0
anchors_mirror_main: rows=157 bad=0
src_main_unchanged: yes
hygiene_scan: clean
```

Before the script existed both tracer tests failed with python3 reporting that it could not open `.github/scripts/bump_version.py`; with the asset copied unchanged both passed. The workflow's own `Increment version` run block, extracted from the committed YAML and run the way GitHub runs it (`bash -eo pipefail` with `GITHUB_ENV` set), turned `v1.7.0_gg5` into `v1.7.0_gg6` in `VERSION` and in the `VERSION` row of the anchors file and nothing else; the anchors mirror read `bad=0` afterwards. A bump of `VERSION` alone, the old workflow's behaviour, reads `bad=1`. The Spotless baseline of the pristine worktree changed 50 existing files and the same 50 after the new file was added; the new file was unchanged by Spotless. The pin guard tallies equal their E-01 baselines (21 and 39).

2026-10-31T08:55:30Z

Verdict: pass

### E-03: The bump rule table, the refused texts and the anchors-row fixtures, with both mutations seen red

Backs: CIH-01, CIH-03, ROADMAP Phase 37 criteria 1 and 2 (the rules of the bump pinned by a Docker-free table, and the failure of the row check seen red on a fixture and by mutation).

```sh
rm -rf build/test-results/testUnit
env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain testUnit --rerun --tests '*BumpVersionScriptUnitTest' > /tmp/gsd37/exec/table-green.log 2>&1
python3 -I .planning/phases/37-ci-hardening-advisory-debt/37-evidence-check.py junit build/test-results/testUnit --class BumpVersionScriptUnitTest
git worktree add --detach /tmp/gsd37/w/fmt HEAD
env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew -p /tmp/gsd37/w/fmt --console=plain spotlessApply
cmp src/test/java/com/clickhouse/kafka/connect/ci/BumpVersionScriptUnitTest.java /tmp/gsd37/w/fmt/src/test/java/com/clickhouse/kafka/connect/ci/BumpVersionScriptUnitTest.java
git worktree remove --force /tmp/gsd37/w/fmt
git diff -w -U0 ff8730860ba8560171fcda44bc1cdcd6dba1ba2a HEAD -- src/test src/integrationTest src/testFixtures
git worktree add --detach /tmp/gsd37/w/mut1 56cc488c1dfafbe3b678c3b19c0d9e16e6c074f3
# in the worktree script: delete the equality check of the row's pattern field against the current version (5 lines)
env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew -p /tmp/gsd37/w/mut1 --console=plain testUnit --rerun --tests '*BumpVersionScriptUnitTest' > /tmp/gsd37/exec/mut1.log 2>&1
python3 -I .planning/phases/37-ci-hardening-advisory-debt/37-evidence-check.py failed /tmp/gsd37/w/mut1/build/test-results/testUnit
git worktree remove --force /tmp/gsd37/w/mut1
git worktree add --detach /tmp/gsd37/w/mut2 56cc488c1dfafbe3b678c3b19c0d9e16e6c074f3
# in the worktree script: replace the statement fields[1] = new by pass
env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew -p /tmp/gsd37/w/mut2 --console=plain testUnit --rerun --tests '*BumpVersionScriptUnitTest' > /tmp/gsd37/exec/mut2.log 2>&1
python3 -I .planning/phases/37-ci-hardening-advisory-debt/37-evidence-check.py failed /tmp/gsd37/w/mut2/build/test-results/testUnit
git worktree remove --force /tmp/gsd37/w/mut2
git diff --quiet 56cc488c1dfafbe3b678c3b19c0d9e16e6c074f3 HEAD -- .github/scripts/bump_version.py
```

```text
assets_identical: yes
table_exit: 0
table_junit: OK junit build/test-results/testUnit class *BumpVersionScriptUnitTest: tests=48 failures=0 errors=0 skipped=0 files=1
table_executions: 48
spotless_new_file_cmp: identical
assertion_diff: clean
hygiene_scan: clean
table_commit: 56cc488c1dfafbe3b678c3b19c0d9e16e6c074f3
table_commit_files: src/test/java/com/clickhouse/kafka/connect/ci/BumpVersionScriptUnitTest.java
mut1_exit: 1
mut1_junit: OK junit /tmp/gsd37/w/mut1/build/test-results/testUnit class *BumpVersionScriptUnitTest: tests=48 failures=1 errors=0 skipped=0 files=1
mut1_failed: BumpVersionScriptUnitTest#CIH-03: a VERSION row that does not match VERSION refuses and writes nothing
mut1_names_mismatch: yes
mut1_worktree_removed: yes
mut2_exit: 1
mut2_junit: OK junit /tmp/gsd37/w/mut2/build/test-results/testUnit class *BumpVersionScriptUnitTest: tests=48 failures=17 errors=0 skipped=0 files=1
mut2_failures: 17
mut2_worktree_removed: yes
script_unmodified_in_main: yes
```

The 48 executions are 14 accepted bumps, 17 refused version texts, 4 unknown-bump usage errors, the run without a bump argument, 10 anchors-row fixtures and the 2 tracer tests. The mismatch mutation (the equality check of the row pattern against the current version deleted) turned exactly the tracer's mismatch fixture red and nothing else; the skipped-rewrite mutation (the row pattern never updated) turned 17 executions red: the 14 accepted bumps, the tracer's gg test, the decoy fixture and the CRLF fixture, as predicted. Both mutations ran in detached worktrees that were removed, and the script in the main tree is unmodified. The per-file Spotless check ran against a worktree at HEAD, which already held the tracer version of the class, so the only numstat difference against the pristine baseline is that class itself; the 50 pre-existing violations are the same files before and after, and Spotless did not change the new file. The table commit also rebuilds the anchors template from a header, an example row, the VERSION row and a Gradle row, a refactor of fixture helpers only: the two tracer tests keep their text.

2026-10-31T08:58:30Z

Verdict: pass
