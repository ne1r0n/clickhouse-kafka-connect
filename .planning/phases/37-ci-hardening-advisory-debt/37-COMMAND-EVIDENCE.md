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
| E-07 | CIH-04, CIH-05 | 37-02 | pass |
| E-08 | ADV-03 | 37-02 | pass |
| E-09 | CIH-04, ADV-01 | 37-03 | pass |
| E-10 | CIH-04, ADV-01, ADV-02 | 37-03 | pass |
| E-11 | CIH-04 | 37-03 | pass |
| E-12 | CIH-04 | 37-03 | pending (user-gated) |
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

### E-07: Allow-lists named by VERSION, and the compare on real archives in both polarities

Backs: CIH-04 and CIH-05, ROADMAP Phase 37 criterion 3, O-1, O-3 and O-5 (the lists are keyed by `VERSION`; both polarities of the compare are proven on real archives without Docker; no provisional list; no `zstd-jni` row), threats T-37-05, T-37-06 and T-37-07.

```sh
git mv .planning/scripts/confluent-archive-allowlist-29.txt .planning/scripts/confluent-archive-allowlist-v1.7.0_gg4.txt
git mv .planning/scripts/confluent-archive-allowlist-32.txt .planning/scripts/confluent-archive-allowlist-v1.7.0_gg5.txt
python3 -I .planning/phases/37-ci-hardening-advisory-debt/37-assets/edit_lines.py.txt replace-text docs/FORK-DELTA.md 1 '<the -29 path>' '<the -v1.7.0_gg4 path> (renamed from the -29 file in Phase 37)'
python3 -I .planning/phases/37-ci-hardening-advisory-debt/37-assets/edit_lines.py.txt replace-text docs/FORK-DELTA.md 1 '<the -32 path>' '<the -v1.7.0_gg5 path> (renamed from the -32 file in Phase 37)'
python3 -I .planning/phases/37-ci-hardening-advisory-debt/37-assets/edit_lines.py.txt insert-after docs/FORK-DELTA.md .planning/phases/37-ci-hardening-advisory-debt/37-assets/fork-delta-row-2-compare.md.txt '| `increment-version.yml` bumps'
git commit -F /tmp/gsd37/exec/msg-p2-rename.txt
git diff -M --summary {rename_commit}^ {rename_commit}
git diff --quiet 3003b763 HEAD -- src/main build.gradle.kts gradle settings.gradle.kts VERSION
git worktree add --detach /tmp/gsd37/w/<name> <v1.7.0_gg4 | v1.7.0_gg5 | HEAD>
env -u TESTCONTAINERS_HOST_OVERRIDE /tmp/gsd37/w/<name>/gradlew -p /tmp/gsd37/w/<name> --console=plain createConfluentArchive > /tmp/gsd37/exec/build-<name>.log 2>&1
cp /tmp/gsd37/w/<name>/build/confluent/clickhouse-kafka-connect-<VERSION>.zip /tmp/gsd37/w/archives/<name>/archive.zip
shasum -a 256 /tmp/gsd37/w/archives/<name>/archive.zip
git worktree remove --force /tmp/gsd37/w/<name>
python3 -I .github/scripts/compare_confluent_archives.py /tmp/gsd37/w/archives/base_gg4/archive.zip /tmp/gsd37/w/archives/cand_gg5/archive.zip .planning/scripts/confluent-archive-allowlist-v1.7.0_gg5.txt > /tmp/gsd37/exec/runA.out 2>&1
python3 -I .github/scripts/compare_confluent_archives.py /tmp/gsd37/w/archives/base_gg4/archive.zip /tmp/gsd37/w/archives/cand_tip/archive.zip .planning/scripts/confluent-archive-allowlist-v1.7.0_gg5.txt > /tmp/gsd37/exec/runB.out 2>&1
cp .planning/scripts/confluent-archive-allowlist-v1.7.0_gg5.txt /tmp/gsd37/exec/allow-stale.txt
printf 'zz/never\ta row that names no difference\n' >> /tmp/gsd37/exec/allow-stale.txt
python3 -I .github/scripts/compare_confluent_archives.py /tmp/gsd37/w/archives/base_gg4/archive.zip /tmp/gsd37/w/archives/cand_gg5/archive.zip /tmp/gsd37/exec/allow-stale.txt > /tmp/gsd37/exec/runC.out 2>&1
```

```text
rename_commit: 163c0f33d09cb61df00a27c23c2c660587c61501
rename_similarity: 2
rename_commit_files: .planning/scripts/confluent-archive-allowlist-29.txt; .planning/scripts/confluent-archive-allowlist-32.txt; .planning/scripts/confluent-archive-allowlist-v1.7.0_gg4.txt; .planning/scripts/confluent-archive-allowlist-v1.7.0_gg5.txt; docs/FORK-DELTA.md
allowlists_present: confluent-archive-allowlist-25.txt; confluent-archive-allowlist-v1.7.0_gg4.txt; confluent-archive-allowlist-v1.7.0_gg5.txt
forkdelta_row2_added: yes
inputs_unchanged_since_research: yes
base_gg4_build_exit: 0
base_gg4_verify_line: verifyConfluentArchive: 7801 inner entries, no nested jar, no Kafka API class
base_gg4_gradle: Gradle 9.8.1 (the wrapper of v1.7.0_gg4, distributionUrl gradle-9.8.1-bin.zip)
base_gg4_archive_size: 12453558
base_gg4_archive_sha256: 5ab57c9bc5b864a3e195e880c756ed192aec548d00a56ccbb6a6a21c6315bc48
cand_gg5_build_exit: 0
cand_gg5_verify_line: verifyConfluentArchive: 7801 inner entries, no nested jar, no Kafka API class
cand_gg5_gradle: Gradle 9.8.1 (the wrapper of v1.7.0_gg5, distributionUrl gradle-9.8.1-bin.zip)
cand_gg5_archive_size: 12453556
cand_gg5_archive_sha256: efa6cca4f30d182eea52565f331581f2c4ccee30c99e91ce0f053b341629e0fe
cand_tip_build_exit: 0
cand_tip_verify_line: verifyConfluentArchive: 7933 inner entries, no nested jar, no Kafka API class
cand_tip_gradle: Gradle 9.8.2 (the wrapper of the current tip)
cand_tip_archive_size: 13558637
cand_tip_archive_sha256: 0027568f5fba22fbe5e9b7c5d31f0f3b4d2f94cb345efeb58411b7925769e5c9
runa_exit: 0
runa_result: RESULT: PASS
runa_totals: totals files-a 7399 files-b 7399 only-a 0 only-b 0 changed 4 same 7395
runa_unallowed: 0
runa_stale: 0
runb_exit: 1
runb_result: RESULT: FAIL
runb_totals: totals files-a 7399 files-b 7513 only-a 4 only-b 118 changed 120 same 7275
runb_unallowed: 238
runb_kinds: 116/118/4
runb_zstd_natives: 3
runb_stale: 0
runb_connector_paths: 31
runb_version_table: VERSION at.yawk.lz4:lz4-java 1.12.0 -> 1.12.1; VERSION com.fasterxml.jackson.core:jackson-annotations 2.22 -> 2.22; VERSION com.fasterxml.jackson.core:jackson-core 2.22.4 -> 2.22.4; VERSION com.fasterxml.jackson.core:jackson-databind 2.22.4 -> 2.22.4; VERSION com.google.code.gson:gson 2.14.0 -> 2.14.0; VERSION com.google.errorprone:error_prone_annotations 2.50.0 -> 2.50.0; VERSION com.google.guava:failureaccess 1.0.3 -> 1.0.3; VERSION com.google.guava:guava 33.7.2-jre -> 33.7.2-jre; VERSION com.google.guava:listenablefuture 9999.0-empty-to-avoid-conflict-with-guava -> 9999.0-empty-to-avoid-conflict-with-guava; VERSION com.google.j2objc:j2objc-annotations 3.1 -> 3.1; VERSION commons-codec:commons-codec 1.19.0 -> 1.19.0; VERSION commons-io:commons-io 2.20.0 -> 2.20.0; VERSION org.apache.commons:commons-compress 1.28.0 -> 1.28.0; VERSION org.apache.commons:commons-lang3 3.20.0 -> 3.20.0; VERSION org.apache.httpcomponents.client5:httpclient5 5.6.5 -> 5.6.5; VERSION org.apache.httpcomponents.core5:httpcore5 5.4.4 -> 5.4.4; VERSION org.apache.httpcomponents.core5:httpcore5-h2 5.4.4 -> 5.4.4; VERSION org.slf4j:slf4j-api 2.0.7 -> 2.0.7
version_table_has_zstd: no
research_numbers_match: yes
runc_exit: 1
runc_stale_line: STALE-ROW zz/never
runc_unallowed: 0
worktrees_removed: yes
hygiene_scan: clean
compare_scope_note: the passing compare in CI belongs to the release that bumps VERSION (Phase 38, RLS-03 and RLS-05); here the passing polarity (run A), the failing polarity (run B) and the stale polarity (run C) were shown on real archives built locally without Docker
```

Both renames are pure 100 percent renames, so the rows of the published releases' lists are byte-identical, and `-25` stays. Run A (the gg4 baseline against gg5 rebuilt from its tag, under the gg5 list) passes with exactly the four changed entries the list names. Run B (the same baseline against the archive built from the current tip, still `VERSION` `v1.7.0_gg5`, under the same list) fails with 238 UNALLOWED lines, among them the three `libzstd-jni-1.5.7-23` natives (darwin/aarch64, linux/aarch64, linux/amd64) and the connector classes, and no STALE-ROW; its numbers equal the research's exactly (the inputs have not moved since the research). The VERSION table has no `zstd-jni` line because that jar ships no `pom.properties`, so the natives surface as UNALLOWED instead, and no row and no table line was added for them (O-3). Run C adds one row that names nothing to the gg5 list and fails with `STALE-ROW zz/never` and no UNALLOWED line. The scratch archives under `/tmp/gsd37/w/archives/` (`base_gg4`, `cand_gg5`, `cand_tip`, each `archive.zip`) stay for plan 37-03. The `runa_*`, `runb_*` and `runc_*` keys are lowercase because the evidence helper's fact grammar is `[a-z0-9_]+`; the plan's `runA_*` spelling cannot be read by the unmodified helper.

2026-10-31T09:15:00Z

Verdict: pass

### E-08: The real v1.7.0_gg5 release, read from the REST API: no workflow run created it

Backs: ADV-03, ROADMAP Phase 37 criterion 5 (with its wording deviation, O-6), threat T-37-06 for the downloaded copy.

```sh
gh auth status
gh api repos/ne1r0n/clickhouse-kafka-connect/releases --jq '<length, and the fields of the release with tag v1.7.0_gg5 and of its asset>'
gh api repos/ne1r0n/clickhouse-kafka-connect/git/ref/tags/v1.7.0_gg5 --jq '<ref, object type, sha>'
git rev-parse v1.7.0_gg5
gh api repos/ne1r0n/clickhouse-kafka-connect/commits/3a3a15a9ae064ffe09c696b8bb87bce23419d463 --jq '<parents, date, first message line>'
gh api 'repos/ne1r0n/clickhouse-kafka-connect/actions/workflows/release.yaml/runs?per_page=10' --jq '<id:event:branch:sha8:conclusion:title>'
gh api 'repos/ne1r0n/clickhouse-kafka-connect/actions/workflows/pre-release.yaml/runs?per_page=10' --jq '.total_count'
gh api repos/ne1r0n/clickhouse-kafka-connect/actions/runs/40474419526/jobs --jq '<steps and conclusions>'
gh api repos/ne1r0n/clickhouse-kafka-connect/actions/runs/40693904004/jobs --jq '<job id and the Publish step>'
gh api repos/ne1r0n/clickhouse-kafka-connect/check-runs/122835509071/annotations --jq '<annotation messages>'
gh api 'repos/ne1r0n/clickhouse-kafka-connect/actions/workflows/tests.yaml/runs?head_sha=3a3a15a9ae064ffe09c696b8bb87bce23419d463&status=success&per_page=3' --jq '<run id, event, branch>'
mkdir /tmp/gsd37/w/asset-dl
gh api repos/ne1r0n/clickhouse-kafka-connect/releases/assets/685055288 -H 'Accept: application/octet-stream' > /tmp/gsd37/w/asset-dl/asset.zip
shasum -a 256 /tmp/gsd37/w/asset-dl/asset.zip
gh api repos/ne1r0n/clickhouse-kafka-connect/releases --jq '<download_count of the asset>'
python3 -I .github/scripts/compare_confluent_archives.py /tmp/gsd37/w/asset-dl/asset.zip /tmp/gsd37/w/archives/cand_gg5/archive.zip /tmp/gsd37/exec/allow-empty.txt > /tmp/gsd37/exec/rebuild-vs-asset.out 2>&1
rm -rf /tmp/gsd37/w/asset-dl
```

```text
release_id: 426690002
release_tag: v1.7.0_gg5
release_draft: false
release_prerelease: false
release_target: custom_build
release_author: ne1r0n
release_author_type: User
release_created_at: 2026-10-26T22:56:14Z
release_published_at: 2026-10-28T13:16:09Z
releases_total: 1
asset_id: 685055288
asset_name: clickhouse-kafka-connect-v1.7.0_gg5.zip
asset_size: 12453556
asset_digest: sha256:4c69b6fb3b22ab8f7c329383079cc284d3d2e0ae497f3ffb2f0114d171bc6767
asset_uploader: ne1r0n
asset_uploader_type: User
asset_created_at: 2026-10-28T13:15:52Z
download_count_before: 0
tag_ref: refs/tags/v1.7.0_gg5
tag_object_type: commit
tag_sha: 3a3a15a9ae064ffe09c696b8bb87bce23419d463
tag_commit_parents: 21c51598b06b6f5af58430765d57b1e5f6747c5a,41e7d5465e8353abb43a5f56dcf9f697c96b537a
tag_commit_date: 2026-10-26T22:56:14Z
tag_commit_subject: Merge sync/v1.7.0: release v1.7.0_gg5
tag_matches_local: yes
release_runs_total: 3
release_runs: 40693904004:workflow_dispatch:custom_build:3a3a15a9:success:Release custom_build (dry run); 40474419526:workflow_dispatch:custom_build:3a3a15a9:cancelled:Release custom_build (publish); 40296481765:workflow_dispatch:sync/v1.7.0:b847fbdb:success:Release sync/v1.7.0 (dry run)
pre_release_runs_total: 1
publish_run_id: 40474419526
publish_run_conclusion: cancelled
publish_run_stopped_step: Build and verify Confluent archive (cancelled)
publish_run_publish_step: skipped
dry_run_id: 40693904004
dry_run_conclusion: success
dry_run_archive_annotation: clickhouse-kafka-connect-v1.7.0_gg5.zip 12453661 bytes sha256=d61a69324393f961410b35a861551f3f6783a77b957262516cb813efb264fa99
dry_run_archive_size: 12453661
dry_run_archive_sha256: d61a69324393f961410b35a861551f3f6783a77b957262516cb813efb264fa99
tests_green_run_id: 40416215635
tests_green_event: push
tests_green_branch: custom_build
asset_downloaded_size: 12453556
asset_downloaded_sha256: 4c69b6fb3b22ab8f7c329383079cc284d3d2e0ae497f3ffb2f0114d171bc6767
asset_matches_api: yes
download_count_after: 0
rebuild_vs_asset_exit: 0
rebuild_vs_asset_result: RESULT: PASS
rebuild_vs_asset_totals: totals files-a 7399 files-b 7399 only-a 0 only-b 0 changed 0 same 7399
rebuild_sha256: efa6cca4f30d182eea52565f331581f2c4ccee30c99e91ce0f053b341629e0fe
rebuild_size: 12453556
rebuild_byte_identical: no
asset_dir_removed: yes
created_by_workflow_run: no
publish_path_exercised: no
caveat_state: open until Phase 38
roadmap_wording_deviation: criterion 5 says the record gives the workflow run that created the release and closes the caveat; the facts allow neither, so the record states them as measured and the caveat stays open
hygiene_scan: clean
```

Every reading is a plain GET (`gh api` without `-X`, `-f`, `-F` or `--input`). The release exists once, was authored by the user account, and its asset was uploaded by the same account about four and a half minutes after the successful dry run finished (13:11:25Z to 13:15:52Z), not by a workflow bot. `release.yaml` has exactly three runs: a dry run from `sync/v1.7.0`, the only publish-mode run (cancelled during `Build and verify Confluent archive`, so `Publish release` and every later step were skipped) and a later dry run on `custom_build` (its `Publish release` step also skipped; it created, checked and deleted its own draft, and reported an archive of 12453661 bytes with another digest than the published asset). So no workflow run created the release, the publish path of `release.yaml` has not been exercised, and the caveat stays open until Phase 38 publishes through it. This deviates from the ROADMAP wording of criterion 5, which asks for the run that created the release and for the caveat to be closed: the facts allow neither, so this record states them as measured. The downloaded copy matches the API's size and digest. The download counter read 0 after the download as well as before it, so the API download did not move it (the plan expected it to rise by one). A rebuild of the tag compares PASS against the published asset under a comment-only allow-list with `changed 0 same 7399`: the two archives differ in bytes only because the archive carries a build date, which the compare ignores (the research found the `manifest.json` `release_date` line to be the only differing file of an extracted copy; this entry measured the entries, not the extracted bytes). The downloaded asset was never extracted or executed, and its directory was removed after the compare.

2026-10-31T09:16:00Z

Verdict: pass

### E-09: The release workflows compare the archive with the previous release: four tested edits, the anchors rows and the docs row

Backs: CIH-04, ROADMAP Phase 37 criterion 3 (workflow half), 30-REVIEW WR-01 (the shared concurrency group), threats T-37-09 to T-37-12.

```sh
for each of release.yaml and pre-release.yaml: grep -cF -e '<gate text>' <file>   # the gates this task moves, before and after
diff .github/workflows/release.yaml .github/workflows/pre-release.yaml | grep '^[<>]' | wc -l
python3 -I .planning/phases/37-ci-hardening-advisory-debt/37-assets/edit_lines.py.txt release-workflow .github/workflows/release.yaml
python3 -I .planning/phases/37-ci-hardening-advisory-debt/37-assets/edit_lines.py.txt release-workflow .github/workflows/pre-release.yaml
python3 -I .planning/phases/37-ci-hardening-advisory-debt/37-assets/edit_lines.py.txt release-anchors .planning/scripts/fork-anchors.txt
python3 -I .planning/phases/37-ci-hardening-advisory-debt/37-assets/edit_lines.py.txt insert-after docs/FORK-DELTA.md .planning/phases/37-ci-hardening-advisory-debt/37-assets/fork-delta-row-3-release-compare.md.txt '| The built Confluent archive is compare'
ruby -ryaml -e 'YAML.load_file(ARGV[0])' .github/workflows/release.yaml
ruby -ryaml -e 'YAML.load_file(ARGV[0])' .github/workflows/pre-release.yaml
python3 -I .planning/phases/37-ci-hardening-advisory-debt/37-evidence-check.py anchors .planning/scripts/fork-anchors.txt --rows 171
env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain testUnit --rerun --tests '*WorkflowActionPinGuardUnitTest' --tests '*TestsWorkflowPinGuardUnitTest' --tests '*IncrementVersionWorkflowGuardUnitTest' > /tmp/gsd37/exec/p3t1-verify.log 2>&1
git commit -F /tmp/gsd37/exec/msg-p3-workflows.txt
```

```text
base_dry_expr_release: 5
base_dry_expr_prerelease: 5
base_publish_expr_release: 3
base_publish_expr_prerelease: 3
base_create_archive_release: 1
base_create_archive_prerelease: 1
base_release_create_release: 2
base_release_create_prerelease: 2
base_continue_release: 0
base_continue_prerelease: 0
base_twin_diff_lines: 16
editor_exit_release: 0
editor_exit_prerelease: 0
editor_exit_anchors: 0
editor_exit_forkdelta: 0
yaml_parse: ok
dry_expr_after_release: 6
dry_expr_after_prerelease: 6
publish_expr_after_release: 3
publish_expr_after_prerelease: 3
create_archive_after_release: 1
create_archive_after_prerelease: 1
release_create_after_release: 2
release_create_after_prerelease: 2
continue_after_release: 0
continue_after_prerelease: 0
compare_call_release: 1
compare_call_prerelease: 1
baseline_tag_call_release: 1
baseline_tag_call_prerelease: 1
worktree_add_release: 1
worktree_add_prerelease: 1
upload_artifact_release: 1
upload_artifact_prerelease: 1
fetch_depth_release: 1
fetch_depth_prerelease: 1
allowlist_path_release: 1
allowlist_path_prerelease: 1
twin_diff_lines_after: 16
anchors_mirror: rows=171 bad=0
step_names: Checkout code; Set up JDK 17; Set up Gradle; Read version; Build and verify Confluent archive; Check archive; Select the baseline tag; Build the baseline archive from the previous tag; Compare the archive with the previous release; Upload the archive compare report; Check tests.yaml is green on this SHA; Check the release tag is absent; Publish release; Create draft release (dry run); Check the draft holds the archive (dry run); Check no tag exists for the draft (dry run); Delete draft release (dry run)
no_other_file_touched: yes
assets_identical: yes
pin_guard_junit: OK junit build/test-results/testUnit: tests=76 failures=0 errors=0 skipped=0 files=3
hygiene_scan: clean
workflows_commit: 44b8fb62c2177558ff38d075eef9401a4426286a
workflows_commit_files: .github/workflows/pre-release.yaml; .github/workflows/release.yaml; .planning/scripts/fork-anchors.txt; docs/FORK-DELTA.md
```

Every baseline of the gates this task moves matched the plan before any edit (dry-run expression 5, publish expression 3, archive call 1, release create 2, continue-on-error 0, twin diff 16). The line editor wrote all four files with exit 0 and its inputs are the plan assets unchanged (`git diff --quiet` of the assets directory against the phase-start commit). Both workflows parse as YAML; the dry-run expression rose from 5 to 6 in each (the `group:` line reuses it), and the other gate counts did not move. The new steps sit between `Check archive` and `Check tests.yaml is green on this SHA`; each compare, baseline, worktree, upload, fetch-depth and allow-list text occurs once per file. The twin diff is 16 lines before and after, so the four edits are identical in both files. Before the commit `git status --porcelain` listed exactly the four committed paths plus the two orchestrator-owned state files; the evidence file is the fifth path and was committed separately. The three pin guard classes pass on the edited workflows (76 executions, 0 failures). The scan for names of other systems over the two workflows, the anchors file and the added FORK-DELTA line printed nothing.

2026-10-31T09:27:00Z

Verdict: pass

### E-10: The release safety gates and the compare chain are pinned line by line, the archive step rules are stronger, and three mutations were seen red

Backs: CIH-04, ADV-01, ADV-02; 30-REVIEW WR-02, WR-03, IN-03 and 25-REVIEW WR-01; threats T-37-09, T-37-10, T-37-12.

```sh
DOCKER_HOST=unix:///nonexistent/docker.sock env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain testUnit --rerun --tests '*ReleaseWorkflowGuardUnitTest' > /tmp/gsd37/exec/guard-green.log 2>&1
python3 -I .planning/phases/37-ci-hardening-advisory-debt/37-evidence-check.py junit build/test-results/testUnit --class ReleaseWorkflowGuardUnitTest 'tests>=30' failures=0 errors=0 skipped=0
python3 -I .planning/phases/37-ci-hardening-advisory-debt/37-assets/edit_lines.py.txt replace-text .github/dependabot.yml 1 'WorkflowActionPinGuardUnitTest fails if this list changes.' 'WorkflowActionPinGuardUnitTest fails if the exclude-paths of this entry hold anything but integrationTests.yaml.'
python3 -I .planning/phases/37-ci-hardening-advisory-debt/37-assets/edit_lines.py.txt insert-after docs/FORK-DELTA.md .planning/phases/37-ci-hardening-advisory-debt/37-assets/fork-delta-row-4-release-guard.md.txt '<the first 90 characters of the row inserted in Task 1>'
DOCKER_HOST=unix:///nonexistent/docker.sock env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain testUnit --rerun --tests '*ReleaseWorkflowGuardUnitTest' --tests '*TestsWorkflowPinGuardUnitTest' --tests '*WorkflowActionPinGuardUnitTest' --tests '*DependabotProtobufIgnoreGuardUnitTest' > /tmp/gsd37/exec/p3t2-pins.log 2>&1
git worktree add --detach /tmp/gsd37/w/fmt HEAD
env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew -p /tmp/gsd37/w/fmt --console=plain spotlessApply
git -C /tmp/gsd37/w/fmt diff --numstat -- src/test/java/com/clickhouse/kafka/connect/ci/TestsWorkflowPinGuardUnitTest.java
cmp src/test/java/com/clickhouse/kafka/connect/ci/ReleaseWorkflowGuardUnitTest.java /tmp/gsd37/w/fmt/src/test/java/com/clickhouse/kafka/connect/ci/ReleaseWorkflowGuardUnitTest.java
git worktree remove --force /tmp/gsd37/w/fmt
git diff -w -U0 {L0} HEAD -- src/test src/integrationTest src/testFixtures
git commit -F /tmp/gsd37/exec/msg-p3-guard.txt
git worktree add --detach /tmp/gsd37/w/gmut {guard_commit}
python3 -I /tmp/gsd37/exec/<mutation script> 1   # continue-on-error: true added to the compare step of release.yaml
env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew -p /tmp/gsd37/w/gmut --console=plain testUnit --rerun --tests '*ReleaseWorkflowGuardUnitTest' > /tmp/gsd37/exec/gmut1.log 2>&1
python3 -I /tmp/gsd37/exec/<mutation script> 2   # the first PUBLISH value of pre-release.yaml changed to true
env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew -p /tmp/gsd37/w/gmut --console=plain testUnit --rerun --tests '*ReleaseWorkflowGuardUnitTest' > /tmp/gsd37/exec/gmut2.log 2>&1
python3 -I /tmp/gsd37/exec/<mutation script> 3   # the fetch-depth line of release.yaml deleted
env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew -p /tmp/gsd37/w/gmut --console=plain testUnit --rerun --tests '*ReleaseWorkflowGuardUnitTest' > /tmp/gsd37/exec/gmut3.log 2>&1
git -C /tmp/gsd37/w/gmut checkout -- .
git worktree remove --force /tmp/gsd37/w/gmut
git worktree prune
```

```text
guard_exit: 0
guard_junit: OK junit build/test-results/testUnit class *ReleaseWorkflowGuardUnitTest: tests=34 failures=0 errors=0 skipped=0 files=1
guard_executions: 34
guard_class_tag: none
guard_class_superclass: none
spotless_new_file_cmp: identical
spotless_numstat_baseline: none
spotless_numstat_edited: none
assertion_diff: clean
pinguard_before: 39
pinguard_after: 44
pins_exit: 0
pins_junit: OK junit build/test-results/testUnit: tests=112 failures=0 errors=0 skipped=0 files=4
dependabot_sentence: fails if the exclude-paths of this entry hold anything but integrationTests.yaml.
forkdelta_prefix_chars: 90
hygiene_scan: clean
guard_commit: fb63b0e9cf477d34f0830cd30e8c17132c660f22
guard_commit_files: .github/dependabot.yml; docs/FORK-DELTA.md; src/test/java/com/clickhouse/kafka/connect/ci/ReleaseWorkflowGuardUnitTest.java; src/test/java/com/clickhouse/kafka/connect/ci/TestsWorkflowPinGuardUnitTest.java
gmut1_exit: 1
gmut1_junit: OK junit /tmp/gsd37/w/gmut/build/test-results/testUnit class *ReleaseWorkflowGuardUnitTest: tests=34 failures=3 errors=0 skipped=0 files=1
gmut2_exit: 1
gmut2_junit: OK junit /tmp/gsd37/w/gmut/build/test-results/testUnit class *ReleaseWorkflowGuardUnitTest: tests=34 failures=2 errors=0 skipped=0 files=1
gmut3_exit: 1
gmut3_junit: OK junit /tmp/gsd37/w/gmut/build/test-results/testUnit class *ReleaseWorkflowGuardUnitTest: tests=34 failures=2 errors=0 skipped=0 files=1
gmut1_failed: ReleaseWorkflowGuardUnitTest#CIH-04: release.yaml has no continue-on-error; ReleaseWorkflowGuardUnitTest#CIH-04: the compare step of release.yaml has no condition and does not swallow its exit status; ReleaseWorkflowGuardUnitTest#CIH-04: release.yaml and pre-release.yaml are equal once name, flag and expectation lines are normalised
gmut2_failed: ReleaseWorkflowGuardUnitTest#CIH-04: the publish step of pre-release.yaml runs only from custom_build and every PUBLISH value agrees; ReleaseWorkflowGuardUnitTest#CIH-04: release.yaml and pre-release.yaml are equal once name, flag and expectation lines are normalised
gmut3_failed: ReleaseWorkflowGuardUnitTest#CIH-04: release.yaml checks out the full history and persists no credentials; ReleaseWorkflowGuardUnitTest#CIH-04: release.yaml and pre-release.yaml are equal once name, flag and expectation lines are normalised
gmut_worktree_removed: yes
```

`ReleaseWorkflowGuardUnitTest` has no superclass and no tag, 34 executions (20 real-file executions over the two workflows, the twin equality test, and 13 fixture tests), all green with Docker unreachable. Each rule has a fixture test with an accepted list and rejected lists built by anchor-checked edit helpers. The compare rule is stricter than the plan's wording in one respect: besides requiring the compare call (after the report redirection and status capture are removed) and no `if:` or `continue-on-error`, it requires the line `exit "$status"`, so a step that captures the status and then ignores it is rejected. `TestsWorkflowPinGuardUnitTest` gained five executions (39 to 44): one real-file order test and four fixtures, and `archiveStepProblem` now also rejects `-x`, `--exclude-task`, `--dry-run` and ` -m ` on the call line, `|| true`, `|| :` and `|| exit 0` after it, and a step `if:`; the existing archive fixtures pass unedited and no existing assertion or fixture was changed (the removed-line scan of assertion, fail and expect text over the test source sets printed nothing). Spotless made no change to either edited test file in a detached worktree, so the numstat of the existing class is empty before and after, and the new file is identical to its formatted copy. The FORK-DELTA insertion needed a prefix longer than the 40 characters the plan names, because the first 40 characters of the compare row also start the existing release-workflow row and the editor refuses an ambiguous prefix; the first 90 characters are unique, and the editor wrote the row after the compare row. The three mutations of a copy at the guard commit were each red: the compare made advisory failed the strictness rule, the no-`continue-on-error` rule and the twin equality (3 executions); one drifted `PUBLISH:` value failed the publish rule and the twin equality (2); the `fetch-depth: 0` line deleted from `release.yaml` alone failed the checkout rule and the twin equality (2). The mutation worktree was removed and pruned (`git worktree list` shows one entry).

2026-10-31T09:25:49Z

Verdict: pass

### E-11: The three new compare steps replayed locally with the runner's shell options, red and green

Backs: CIH-04, threat T-37-09 (the compare fails the block on exit 1, and the two closed failures of the select step fail it too).

```sh
ruby -ryaml -e 'y=YAML.load_file(ARGV[0]); s=y["jobs"]["build_release"]["steps"].find{|x| x["name"]==ARGV[1]}; puts s["run"]' .github/workflows/release.yaml "Select the baseline tag" > /tmp/gsd37/exec/replay-select-release.sh
ruby -ryaml -e '<the same extraction>' .github/workflows/release.yaml "Build the baseline archive from the previous tag" > /tmp/gsd37/exec/replay-build-release.sh
ruby -ryaml -e '<the same extraction>' .github/workflows/release.yaml "Compare the archive with the previous release" > /tmp/gsd37/exec/replay-compare-release.sh
env RELEASE_VERSION=v1.7.0_gg5 GITHUB_ENV=/tmp/gsd37/exec/replay-env-select RUNNER_TEMP=/tmp/gsd37/w/runner-temp bash --noprofile --norc -eo pipefail /tmp/gsd37/exec/replay-select-release.sh
env RELEASE_VERSION=v1.7.0_gg6 GITHUB_ENV=/tmp/gsd37/exec/replay-env-missing RUNNER_TEMP=/tmp/gsd37/w/runner-temp bash --noprofile --norc -eo pipefail /tmp/gsd37/exec/replay-select-release.sh
env RELEASE_VERSION=v0.0.1 GITHUB_ENV=/tmp/gsd37/exec/replay-env-nobase RUNNER_TEMP=/tmp/gsd37/w/runner-temp bash --noprofile --norc -eo pipefail /tmp/gsd37/exec/replay-select-release.sh
env -u TESTCONTAINERS_HOST_OVERRIDE BASELINE_TAG=v1.7.0_gg4 GITHUB_ENV=/tmp/gsd37/exec/replay-env-build RUNNER_TEMP=/tmp/gsd37/w/runner-temp bash --noprofile --norc -eo pipefail /tmp/gsd37/exec/replay-build-release.sh
env BASELINE_ARCHIVE=<the BASELINE_ARCHIVE line of the build env file> ARCHIVE=/tmp/gsd37/w/archives/cand_tip/archive.zip ALLOWLIST=.planning/scripts/confluent-archive-allowlist-v1.7.0_gg5.txt BASELINE_TAG=v1.7.0_gg4 RUNNER_TEMP=/tmp/gsd37/w/runner-temp GITHUB_STEP_SUMMARY=/tmp/gsd37/exec/replay-summary-red.md bash --noprofile --norc -eo pipefail /tmp/gsd37/exec/replay-compare-release.sh
env BASELINE_ARCHIVE=<the same> ARCHIVE=/tmp/gsd37/w/archives/cand_gg5/archive.zip ALLOWLIST=.planning/scripts/confluent-archive-allowlist-v1.7.0_gg5.txt BASELINE_TAG=v1.7.0_gg4 RUNNER_TEMP=/tmp/gsd37/w/runner-temp GITHUB_STEP_SUMMARY=/tmp/gsd37/exec/replay-summary-green.md bash --noprofile --norc -eo pipefail /tmp/gsd37/exec/replay-compare-release.sh
cmp /tmp/gsd37/exec/replay-select-release.sh /tmp/gsd37/exec/replay-select-pre-release.sh   # and the same for the build and compare blocks
git worktree remove --force /tmp/gsd37/w/runner-temp/baseline
git worktree prune
```

```text
replay_select_exit: 0
replay_select_baseline: BASELINE_TAG=v1.7.0_gg4
replay_select_allowlist: ALLOWLIST=.planning/scripts/confluent-archive-allowlist-v1.7.0_gg5.txt
replay_missing_allowlist_exit: 1
replay_missing_allowlist_error: ::error title=Allow-list::.planning/scripts/confluent-archive-allowlist-v1.7.0_gg6.txt does not exist; the archive compare needs the allow-list named by VERSION
replay_no_baseline_exit: 1
replay_no_baseline_error: ::error title=Baseline tag::no release tag below v0.0.1 is reachable from this commit
replay_build_exit: 0
replay_build_archive: BASELINE_ARCHIVE=/tmp/gsd37/w/runner-temp/baseline/build/confluent/clickhouse-kafka-connect-v1.7.0_gg4.zip
replay_build_archive_exists: yes
replay_build_archive_bytes: 12453558
replay_build_wrapper: Gradle 9.8.1 (the tag's gradle-wrapper.properties names gradle-9.8.1-bin.zip and the build log's documentation link names docs.gradle.org/9.8.1)
replay_red_exit: 1
replay_red_error_line: ::error title=Archive compare::exit 1, 238 unallowed, 0 stale rows, baseline v1.7.0_gg4, allow-list .planning/scripts/confluent-archive-allowlist-v1.7.0_gg5.txt; the report is the archive-compare-report artifact
replay_red_unallowed: 238
replay_red_report_result: RESULT: FAIL
replay_red_summary: yes
replay_green_exit: 0
replay_green_notice: ::notice title=Archive compare::exit 0, 0 unallowed, 0 stale rows, baseline v1.7.0_gg4, allow-list .planning/scripts/confluent-archive-allowlist-v1.7.0_gg5.txt
replay_green_report_result: RESULT: PASS
replay_twin_blocks_identical: yes
replay_worktree_removed: yes
replay_ci_difference: the replay uses this workstation, the runner's bash options and the same variable names, and does not exercise the runner image, setup-java, setup-gradle, the tag fetch of the checkout or the artifact upload action
hygiene_scan: clean
```

The run blocks were extracted from the committed `release.yaml` (not typed) and run from the repository root with `bash --noprofile --norc -eo pipefail`. The select block chose `v1.7.0_gg4` for `v1.7.0_gg5` and wrote the baseline and the list name to its env file; for `v1.7.0_gg6` (baseline `v1.7.0_gg5`) it failed closed because no gg6 list exists, and for `v0.0.1` it failed closed because no release tag sorts below it (the script's own `ERROR:` line precedes the annotation). The build block made a worktree of the tag `v1.7.0_gg4` under the runner-temp directory, read its `VERSION`, built with the tag's own wrapper (Gradle 9.8.1, `BUILD SUCCESSFUL in 11s`, `verifyConfluentArchive: 7801 inner entries, no nested jar, no Kafka API class`) and exported an archive of 12453558 bytes, the size of the earlier gg4 scratch archive. The compare block failed red on the archive built from the tip with exit 1, an error annotation counting 238 unallowed lines (the number E-07 `runb_unallowed` recorded), 0 stale rows, a report ending `RESULT: FAIL` and the step summary heading `### Archive compare`; it passed green on the `v1.7.0_gg5` rebuild with a notice of 0 unallowed and 0 stale rows and a report ending `RESULT: PASS`. The three run blocks are byte-identical in `release.yaml` and `pre-release.yaml` (`cmp` on the extracted files). One Gradle build ran at a time, and the baseline worktree was removed and pruned afterwards. The replay does not prove the upload step or anything that needs the runner; that is the user-gated dry run of E-12.

2026-10-31T09:28:36Z

Verdict: pass

### E-12: The CI dry run is a user-gated handoff, expected RED

Backs: CIH-04, O-5, threat T-37-13 (nothing is pushed, dispatched or run through a mutating gh call, and the expected red run is never counted as a pass).

```sh
python3 -I .planning/phases/37-ci-hardening-advisory-debt/37-hygiene-check.py .planning/phases/37-ci-hardening-advisory-debt/37-USER-DISPATCH-DRYRUN.md
grep -c 'Expected result: RED' .planning/phases/37-ci-hardening-advisory-debt/37-USER-DISPATCH-DRYRUN.md
grep -c 'never counted as a pass' .planning/phases/37-ci-hardening-advisory-debt/37-USER-DISPATCH-DRYRUN.md
git ls-remote origin refs/heads/custom_build
git ls-remote --heads origin 'scratch/*'
```

```text
handoff_file: .planning/phases/37-ci-hardening-advisory-debt/37-USER-DISPATCH-DRYRUN.md
handoff_expected_red: yes
handoff_hygiene: clean
nothing_pushed: yes
origin_custom_build_now: 3a3a15a9ae064ffe09c696b8bb87bce23419d463
origin_scratch_refs_now: de8c685b80d40b0a6e19338271e07b08ede84f3b refs/heads/scratch/clt-07-client-0.11.0; 939e899298c2f12dec2cfe1e69b1a6b53b4f511e refs/heads/scratch/clt-07-prebump-0.9.6
scratch_branch_name: scratch/cih-04-dry-run
user_report: not yet received
```

The handoff `37-USER-DISPATCH-DRYRUN.md` gives the user the checks before the push, the one push command, the dispatch (Actions UI or `gh workflow run`, always from the scratch branch), the fields to report, the clean-up and the resume instruction for the orchestrator (entry E-19). It states under the heading `Expected result: RED` that the run is expected to fail at the step `Compare the archive with the previous release` with about 238 unallowed lines, that this proves the plumbing, and that it is never counted as a pass of any criterion. Nothing in this plan pushed a branch or tag, dispatched or re-ran a workflow, or made a gh call that is not a plain GET: origin `custom_build` still equals the `v1.7.0_gg5` commit and the only `scratch/*` refs on origin are the two Phase 36 refs that E-01 recorded (so the plan's expectation that no `scratch/*` ref exists is replaced by the absence of `scratch/cih-04-dry-run`). E-12 stays pending as the record of the handoff and is never edited; the user's report, if it arrives, becomes the new entry E-19.

2026-10-31T09:28:36Z

Verdict: pending (user-gated: the push and the dispatch are the user's)
