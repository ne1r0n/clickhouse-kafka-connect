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
| E-04 | CIH-02 | 37-01 | pass |
| E-05 | CIH-05 | 37-02 | pass |
| E-06 | CIH-04 | 37-02 | pass |
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

### E-04: increment-version.yml is pinned line by line, the pin guards and the anchors hold, and the new classes pass with Docker unreachable

Backs: CIH-02, ROADMAP Phase 37 criterion 1, and the Phase 30 review rows IN-01 and IN-02 (the workflow cannot regain an npm step, an expression inside a run block, a missing anchors add or the old defect note).

```sh
rm -rf build/test-results/testUnit
DOCKER_HOST=unix:///nonexistent/docker.sock env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain testUnit --rerun --tests '*IncrementVersionWorkflowGuardUnitTest' --tests '*BumpVersionScriptUnitTest' > /tmp/gsd37/exec/guard-nodocker.log 2>&1
python3 -I .planning/phases/37-ci-hardening-advisory-debt/37-evidence-check.py junit build/test-results/testUnit --class <ClassName>
grep -nE '^[[:space:]]*@(Disabled|Tag)\b|Assumptions\.|^(public |final |abstract )*class .* extends ' src/test/java/com/clickhouse/kafka/connect/ci/BumpVersionScriptUnitTest.java src/test/java/com/clickhouse/kafka/connect/ci/IncrementVersionWorkflowGuardUnitTest.java
git worktree add --detach /tmp/gsd37/w/mut3 HEAD
cp src/test/java/com/clickhouse/kafka/connect/ci/IncrementVersionWorkflowGuardUnitTest.java /tmp/gsd37/w/mut3/src/test/java/com/clickhouse/kafka/connect/ci/
# in the worktree workflow: the git commit -m argument gets an env expression
env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew -p /tmp/gsd37/w/mut3 --console=plain testUnit --rerun --tests '*IncrementVersionWorkflowGuardUnitTest' > /tmp/gsd37/exec/guard-mutation.log 2>&1
python3 -I .planning/phases/37-ci-hardening-advisory-debt/37-evidence-check.py failed /tmp/gsd37/w/mut3/build/test-results/testUnit
git worktree remove --force /tmp/gsd37/w/mut3
rm -rf build/test-results/testUnit
env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain testUnit --rerun > /tmp/gsd37/exec/testunit-plan01.log 2>&1
python3 -I .planning/phases/37-ci-hardening-advisory-debt/37-evidence-check.py junit build/test-results/testUnit
python3 -I .planning/phases/37-ci-hardening-advisory-debt/37-evidence-check.py failed build/test-results/testUnit
python3 -I .planning/phases/37-ci-hardening-advisory-debt/37-evidence-check.py anchors .planning/scripts/fork-anchors.txt
git worktree add --detach /tmp/gsd37/w/fmt HEAD
env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew -p /tmp/gsd37/w/fmt --console=plain spotlessApply
cmp src/test/java/com/clickhouse/kafka/connect/ci/IncrementVersionWorkflowGuardUnitTest.java /tmp/gsd37/w/fmt/src/test/java/com/clickhouse/kafka/connect/ci/IncrementVersionWorkflowGuardUnitTest.java
git worktree remove --force /tmp/gsd37/w/fmt
git diff -w -U0 ff8730860ba8560171fcda44bc1cdcd6dba1ba2a HEAD -- src/test src/integrationTest src/testFixtures
git diff --quiet ff8730860ba8560171fcda44bc1cdcd6dba1ba2a HEAD -- src/main
```

```text
guard_exit: 0
guard_junit: OK junit build/test-results/testUnit class *IncrementVersionWorkflowGuardUnitTest: tests=16 failures=0 errors=0 skipped=0 files=1
bump_junit: OK junit build/test-results/testUnit class *BumpVersionScriptUnitTest: tests=48 failures=0 errors=0 skipped=0 files=1
guard_executions: 16
bump_executions: 48
extends_nothing: yes
guard_mutation_exit: 1
guard_mutation_junit: OK junit /tmp/gsd37/w/mut3/build/test-results/testUnit class *IncrementVersionWorkflowGuardUnitTest: tests=16 failures=2 errors=0 skipped=0 files=1
guard_mutation_failed: IncrementVersionWorkflowGuardUnitTest#CIH-02: increment-version.yml adds VERSION together with the anchors file and commits with the new version; IncrementVersionWorkflowGuardUnitTest#CIH-02: increment-version.yml has no expression inside any run block
guard_mutation_worktree_removed: yes
plan_testunit_exit: 0
plan_testunit_tests: 1539
plan_testunit_delta: 64
plan_testunit_failures: 0
plan_testunit_errors: 0
pin_classes_unchanged: yes
pin_class_tallies: WorkflowActionPinGuardUnitTest tests=21; TestsWorkflowPinGuardUnitTest tests=39
anchors_mirror_main: rows=157 bad=0
assertion_diff: clean
spotless_new_file_cmp: identical
src_main_unchanged: yes
hygiene_scan: clean
guard_commit: b40263edb679a8e6588ec1a898244d735c0f116f
guard_commit_files: src/test/java/com/clickhouse/kafka/connect/ci/IncrementVersionWorkflowGuardUnitTest.java
```

Both new classes ran green with `DOCKER_HOST` pointing at a socket that does not exist, so neither needs Docker. The guard class holds eight real-file tests (one per rule) and eight fixture tests, each accepting the valid workflow and rejecting one edit per bad form. The mutation (an env expression put into the `git commit -m` argument of a worktree copy of the workflow) turned two tests red: the commit-step rule and the run-block rule. The plan-level testUnit tally of 1539 is the E-01 baseline of 1475 plus 48 and 16, and the two pin guards keep their baseline tallies of 21 and 39. The class Javadoc says "has no superclass" instead of the words "extends nothing", because the plan's own verify greps the class for the text "extends " and that text matches a comment.

2026-10-31T09:02:30Z

Verdict: pass

### E-05: The compare script moved with git mv, edited to the asset and pinned on synthetic archives: exit 0, 1, 2, UNALLOWED and STALE-ROW

Backs: CIH-05, ROADMAP Phase 37 criterion 3 (the script half: the compare lives under `.github/scripts/` and its self-test covers UNALLOWED, STALE-ROW and the exit codes 0, 1 and 2), O-1 and O-2 (the move and the all-stars refusal), threat T-37-05.

```sh
git mv .planning/scripts/compare-confluent-archives.py .github/scripts/compare_confluent_archives.py
git commit -F /tmp/gsd37/exec/msg-p2-move.txt
git diff -M --summary {move_commit}^ {move_commit}
rm -rf build/test-results/testUnit
env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain testUnit --rerun --tests '*CompareConfluentArchivesScriptUnitTest' > /tmp/gsd37/exec/compare-red.log 2>&1
python3 -I .planning/phases/37-ci-hardening-advisory-debt/37-evidence-check.py junit build/test-results/testUnit --class CompareConfluentArchivesScriptUnitTest
python3 -I .planning/phases/37-ci-hardening-advisory-debt/37-evidence-check.py failed build/test-results/testUnit
cp .planning/phases/37-ci-hardening-advisory-debt/37-assets/compare_confluent_archives.py.txt .github/scripts/compare_confluent_archives.py
cmp .github/scripts/compare_confluent_archives.py .planning/phases/37-ci-hardening-advisory-debt/37-assets/compare_confluent_archives.py.txt
env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain testUnit --rerun --tests '*CompareConfluentArchivesScriptUnitTest' > /tmp/gsd37/exec/compare-green.log 2>&1
python3 -I .planning/phases/37-ci-hardening-advisory-debt/37-assets/edit_lines.py.txt replace-text docs/FORK-DELTA.md 3 '`.planning/scripts/compare-confluent-archives.py`' '`.github/scripts/compare_confluent_archives.py` (moved there in Phase 37)'
git worktree add --detach /tmp/gsd37/w/fmt HEAD
env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew -p /tmp/gsd37/w/fmt --console=plain spotlessApply
cmp src/test/java/com/clickhouse/kafka/connect/ci/CompareConfluentArchivesScriptUnitTest.java /tmp/gsd37/w/fmt/src/test/java/com/clickhouse/kafka/connect/ci/CompareConfluentArchivesScriptUnitTest.java
git worktree remove --force /tmp/gsd37/w/fmt
git commit -F /tmp/gsd37/exec/msg-p2-edit.txt
git worktree add --detach /tmp/gsd37/w/cmut1 {edit_commit}
python3 -I /tmp/gsd37/exec/mutate.py /tmp/gsd37/w/cmut1/.github/scripts/compare_confluent_archives.py '<the stale list statement>' '    stale = []'
env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew -p /tmp/gsd37/w/cmut1 --console=plain testUnit --rerun --tests '*CompareConfluentArchivesScriptUnitTest' > /tmp/gsd37/exec/cmut1.log 2>&1
git worktree add --detach /tmp/gsd37/w/cmut2 {edit_commit}
python3 -I /tmp/gsd37/exec/mutate.py /tmp/gsd37/w/cmut2/.github/scripts/compare_confluent_archives.py '<the unallowed.append statement>' '            pass'
env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew -p /tmp/gsd37/w/cmut2 --console=plain testUnit --rerun --tests '*CompareConfluentArchivesScriptUnitTest' > /tmp/gsd37/exec/cmut2.log 2>&1
git worktree remove --force /tmp/gsd37/w/cmut1
git worktree remove --force /tmp/gsd37/w/cmut2
git worktree prune
git diff --quiet {edit_commit} HEAD -- .github/scripts/compare_confluent_archives.py
```

```text
move_commit: 4eee1466f6e61db89cb64463c4d2c0f2b4407b17
move_similarity: rename .planning/scripts/compare-confluent-archives.py => .github/scripts/compare_confluent_archives.py (100%)
old_path_gone: yes
red_exit: 1
red_junit: OK junit build/test-results/testUnit class *CompareConfluentArchivesScriptUnitTest: tests=27 failures=2 errors=0 skipped=0 files=1
red_failed: CompareConfluentArchivesScriptUnitTest#CIH-05: a row of one star is refused as a catch-all; CompareConfluentArchivesScriptUnitTest#CIH-05: a row of two stars is refused as a catch-all
assets_identical: yes
script_mode: 100755
green_exit: 0
green_junit: OK junit build/test-results/testUnit class *CompareConfluentArchivesScriptUnitTest: tests=27 failures=0 errors=0 skipped=0 files=1
forkdelta_script_refs: 3
forkdelta_old_path_refs: 0
forkdelta_new_path_lines: 3
spotless_new_file_cmp: identical
assertion_diff: clean
hygiene_scan: clean
edit_commit: 10d90f61fcf10212a87bcc1cdf30d71949fd1932
edit_commit_files: .github/scripts/compare_confluent_archives.py; docs/FORK-DELTA.md; src/test/java/com/clickhouse/kafka/connect/ci/CompareConfluentArchivesScriptUnitTest.java
muta_exit: 1
muta_junit: OK junit /tmp/gsd37/w/cmut1/build/test-results/testUnit class *CompareConfluentArchivesScriptUnitTest: tests=27 failures=1 errors=0 skipped=0 files=1
muta_failed: CompareConfluentArchivesScriptUnitTest#CIH-05: a row that names no difference is a STALE-ROW and fails
mutb_exit: 1
mutb_junit: OK junit /tmp/gsd37/w/cmut2/build/test-results/testUnit class *CompareConfluentArchivesScriptUnitTest: tests=27 failures=5 errors=0 skipped=0 files=1
mutb_failed: CompareConfluentArchivesScriptUnitTest#CIH-05: a changed entry with an empty allow-list is UNALLOWED CHANGED; CompareConfluentArchivesScriptUnitTest#CIH-05: an entry only in the baseline archive is UNALLOWED ONLY-A; CompareConfluentArchivesScriptUnitTest#CIH-05: an entry only in the candidate archive is UNALLOWED ONLY-B; CompareConfluentArchivesScriptUnitTest#CIH-05: manifest.json differing in its name line is UNALLOWED CHANGED; CompareConfluentArchivesScriptUnitTest#CIH-05: one allowed and one unallowed difference together fail on the unallowed one
mut_worktrees_removed: yes
script_unmodified_in_main: yes
```

The rename commit is a pure 100 percent rename of one file. The 27 executions (6 exit 0, 8 exit 1, 13 exit 2) were first run against the moved but unedited script: exactly the two star-row executions failed, because that copy accepted a row of only stars as an ordinary glob. After the plan asset replaced the script (byte-identical, mode 100755 kept) all 27 are green. The mutation that empties the STALE-ROW list failed exactly the STALE-ROW execution, and the one that drops the UNALLOWED append failed five executions (the changed, only-B, only-A, manifest.json and mixed-pair ones), so neither failure kind can change without a test going red. The mutation keys are lowercase (`muta_*`, `mutb_*`) because the evidence helper's fact grammar is `[a-z0-9_]+`; the plan's `mutA_*` spelling cannot be read by the unmodified helper. FORK-DELTA names the new path three times and the old path no longer.

2026-10-31T09:09:00Z

Verdict: pass

### E-06: The baseline tag rule: a tested script, the real tags and one mutation seen red

Backs: CIH-04 (the previous release tag, derived from `VERSION`, is the baseline of the compare), threat T-37-08.

```sh
rm -rf build/test-results/testUnit
env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain testUnit --rerun --tests '*BaselineTagScriptUnitTest' > /tmp/gsd37/exec/baseline-red.log 2>&1
python3 -I .planning/phases/37-ci-hardening-advisory-debt/37-evidence-check.py junit build/test-results/testUnit --class BaselineTagScriptUnitTest
cp .planning/phases/37-ci-hardening-advisory-debt/37-assets/baseline_tag.py.txt .github/scripts/baseline_tag.py
chmod +x .github/scripts/baseline_tag.py
cmp .github/scripts/baseline_tag.py .planning/phases/37-ci-hardening-advisory-debt/37-assets/baseline_tag.py.txt
env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain testUnit --rerun --tests '*BaselineTagScriptUnitTest' > /tmp/gsd37/exec/baseline-green.log 2>&1
git worktree add --detach /tmp/gsd37/w/fmt HEAD
env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew -p /tmp/gsd37/w/fmt --console=plain spotlessApply
cmp src/test/java/com/clickhouse/kafka/connect/ci/BaselineTagScriptUnitTest.java /tmp/gsd37/w/fmt/src/test/java/com/clickhouse/kafka/connect/ci/BaselineTagScriptUnitTest.java
git worktree remove --force /tmp/gsd37/w/fmt
git commit -F /tmp/gsd37/exec/msg-p2-baseline.txt
tags=$(git tag --merged HEAD --list 'v*')
echo "$tags" | python3 -I .github/scripts/baseline_tag.py v1.7.0_gg5
echo "$tags" | python3 -I .github/scripts/baseline_tag.py v1.7.0_gg6
echo "$tags" | python3 -I .github/scripts/baseline_tag.py "$(cat VERSION)"
git merge-base --is-ancestor v1.7.0_gg4 HEAD
git merge-base --is-ancestor v1.7.0_gg5 HEAD
git worktree add --detach /tmp/gsd37/w/bmut {baseline_commit}
python3 -I /tmp/gsd37/exec/mutate.py /tmp/gsd37/w/bmut/.github/scripts/baseline_tag.py 'int(gg) if gg else 0' '0'
env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew -p /tmp/gsd37/w/bmut --console=plain testUnit --rerun --tests '*BaselineTagScriptUnitTest' > /tmp/gsd37/exec/bmut.log 2>&1
git worktree remove --force /tmp/gsd37/w/bmut
git worktree prune
git diff --quiet {baseline_commit} HEAD -- .github/scripts/baseline_tag.py
```

```text
red_exit: 1
red_junit: OK junit build/test-results/testUnit class *BaselineTagScriptUnitTest: tests=19 failures=19 errors=0 skipped=0 files=1
assets_identical: yes
script_mode: 100755
green_exit: 0
green_junit: OK junit build/test-results/testUnit class *BaselineTagScriptUnitTest: tests=19 failures=0 errors=0 skipped=0 files=1
spotless_new_file_cmp: identical
hygiene_scan: clean
baseline_commit: b6cf6d30885dee5b43a61e7176c2db3d4733b8d4
baseline_commit_files: .github/scripts/baseline_tag.py; src/test/java/com/clickhouse/kafka/connect/ci/BaselineTagScriptUnitTest.java
real_tags_count: 78
real_baseline_gg5: v1.7.0_gg4
real_baseline_gg6: v1.7.0_gg5
real_baseline_current: v1.7.0_gg4
real_gg4_is_ancestor: yes
real_gg5_is_ancestor: yes
non_release_tags_ignored: 20
mut_exit: 1
mut_junit: OK junit /tmp/gsd37/w/bmut/build/test-results/testUnit class *BaselineTagScriptUnitTest: tests=19 failures=7 errors=0 skipped=0 files=1
mut_failed: BaselineTagScriptUnitTest#CIH-04: a list with carriage return line endings is read; BaselineTagScriptUnitTest#CIH-04: a tag with an Arabic-Indic digit is ignored; BaselineTagScriptUnitTest#CIH-04: a tag without a suffix sorts below the first counter; BaselineTagScriptUnitTest#CIH-04: counters compare numerically and not as text; BaselineTagScriptUnitTest#CIH-04: names that are not release tags are ignored; BaselineTagScriptUnitTest#CIH-04: the counter below a later release; BaselineTagScriptUnitTest#CIH-04: the previous counter is chosen over the same and an older one
mut_worktree_removed: yes
script_unmodified_in_main: yes
```

All 19 executions (10 printed baselines, 3 no-baseline exit 1, 6 exit 2 of which 4 are invalid versions and 2 argument-count errors) were red while the script did not exist, and green with the plan asset copied byte for byte. On the 78 release-looking tags reachable from `HEAD` the rule gives `v1.7.0_gg4` for `v1.7.0_gg5` and `v1.7.0_gg5` for `v1.7.0_gg6`, and `v1.7.0_gg4` for the current `VERSION`; both of those tags are ancestors of `HEAD`. Twenty of the 78 names (for example `v1.7.0_gg4-hardening`, `v1.2.9_gg4-tests`, `v1.7.0_gg5-ci` and the thirteen `-alfa` and `-beta` tags) are rejected by the grammar and ignored. Treating every counter as zero turned seven executions red, so the numeric counter order is pinned.

2026-10-31T09:11:30Z

Verdict: pass
