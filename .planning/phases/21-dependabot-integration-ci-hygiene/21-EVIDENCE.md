# Phase 21 Evidence

- Phase: 21 (Dependabot Integration & CI Hygiene)
- Branch: `sync/v1.7.0`
- Purpose: measured proof for the Phase 21 plans. Nothing here was pushed.
- Entry format: one `### E-NN: {title}` section per entry, with the command as run, the relevant output in a `text` fence, a UTC timestamp, and a final line `Verdict: pass`, `Verdict: fail` or `Verdict: unmeasured ({reason})`. Long output stays in `/tmp/gsd21/exec/`.

## Index

| Entry | Requirement | Verdict |
|-------|-------------|---------|
| E-01 | OPS-01, OPS-02, OPS-03 (facts at phase start) | unmeasured (D-11 window opens 2026-10-17T12:00:00Z; the closing OPS-01 check is E-13 in plan 21-05) |
| E-02 | OPS-01, OPS-02, OPS-03 (operator answers) | unmeasured (OPS-02 unmeasured: no gradle "Last checked" time reported; OPS-01 disable recorded and OPS-03 pass) |
| E-03 | DEPS-01 (merge) | pass |
| E-04 | DEPS-01, D-03 (gate before the exemption) | pass |
| E-05 | DEPS-01 (dependencyInsight) | pass |
| E-06 | DEPS-01 (testUnit and Lombok warning) | pass |
| E-07 | DEPS-01, D-14 (spotless baseline-equal) | pass |
| E-08 | DEPS-01, D-01, D-02 (gate with and without the exemption) | pass |
| E-09 | D-04 (self-test) | pass |
| E-10 | CI-18, CI-19, D-07 (pin-guard red run) | pass |
| E-11 | CI-18, CI-19, D-09, D-14 (pin-guard green run) | pass |
| E-12 | DEPS-01, D-14 (phase close gate at the tip) | pass |
| E-13 | OPS-01, D-11 (closing observation) | pending (plan 21-05) |

### E-01: OPS facts at phase start (REST, GitHub clock, Keychain)

Commands as run (repo root, unauthenticated, `R=https://api.github.com/repos/ne1r0n/clickhouse-kafka-connect`, every curl `-sS --fail`, output in `/tmp/gsd21/exec/`):

1. `curl -sS --fail -D ops-wf.headers -o ops-wf.json "$R/actions/workflows/integrationTests.yaml"` (id, state, `updated_at` U, `date:` header)
2. `curl -sS --fail -o ops-runs-after.json "$R/actions/workflows/integrationTests.yaml/runs?event=schedule&created=%3E{U}&per_page=100"`
3. `curl -sS --fail -o ops-repo-runs-after.json "$R/actions/runs?event=schedule&created=%3E{U}&per_page=100"`
4. `curl -sS --fail -o ops-runs-control.json "$R/actions/workflows/integrationTests.yaml/runs?event=schedule&created=%3E2026-10-01T00:00:00Z&per_page=100"`
5. `curl -sS --fail -o ops-dependabot-runs.json "$R/actions/workflows/384810012/runs?per_page=20"` (newest run named `gradle in /`)
6. `security find-generic-password -s gsd17-gh-actions-read >/dev/null 2>&1; echo $?` (exit status only)
7. `git diff --quiet v1.7.0 HEAD -- .github/workflows/integrationTests.yaml` and `git diff --quiet v1.7.0 origin/custom_build -- .github/workflows/integrationTests.yaml` (both succeeded)

```text
workflow_id: 384641581
workflow_state: disabled_manually
workflow_updated_at: 2026-10-16T09:38:23Z
github_date: Sat, 17 Oct 2026 07:34:27 GMT
schedule_runs_after_disable: 0
repo_schedule_runs_after_disable: 0
control_schedule_runs_since_2026-10-01: 2
dependabot_gradle_job: gradle in /. - Update #1627525145 2026-10-14T21:05:35Z completed success
keychain_lookup_exit: 44
integration_tests_yaml_identical_to_v1.7.0: yes
```

Timestamp: 2026-10-17T07:34:34Z

Verdict: unmeasured (D-11 window opens 2026-10-17T12:00:00Z; the closing OPS-01 check is E-13 in plan 21-05)

### E-02: OPS-01..OPS-03 operator answers (verbatim)

Presented inline by the orchestrator in this session: the three web-UI checks from plan 21-01 Task 2, each shown beside its E-01 fact. Every answer below is the user's typed reply, copied unedited.

OPS-01 answer (verbatim, given directly to the orchestrator, 2026-10-17T08:30:20Z):
> 1. disabled

OPS-02 answer (verbatim, given directly to the orchestrator, 2026-10-17T08:30:20Z):
> 2. dependabot works

OPS-03 answer (verbatim, given directly to the orchestrator, 2026-10-17T08:30:20Z):
> 3. token've been deleted

The first OPS-01 answer gave no disable date, and the first OPS-02 answer gave no "Last checked" time and no configuration-error reading. In the same session, the orchestrator showed the API instant `2026-10-16T09:38:23Z` and asked when the workflow was disabled (plan 21-01 Task 3 step 5). It also asked for the `gradle` entry's "Last checked" time and whether a configuration error shows. The user's reply:

OPS-01 disable instant resolution (verbatim, given directly to the orchestrator, 2026-10-17T08:37:22Z):
> OPS-01: time is right

OPS-02 follow-up answer (verbatim, given directly to the orchestrator, 2026-10-17T08:37:22Z):
> OPS-02: [Image #1] [Image #2]

orchestrator annotation (not the user's words): Image #1 shows Insights, Dependency graph, Dependabot tab. It has one banner, error-styled with a check icon, reading "Dependabot cannot open any more pull requests. The open pull request limit has been exceeded. The current limit is: 5. Dependabot will open new pull requests once you merge or close the already open pull requests. You can also update this limit in the config file." with a "Troubleshoot Dependabot errors" link. Below it is the "Dependabot update logs" panel. No per-manifest entry and no "Last checked" time are visible in the image. The banner is about the open-PR limit, not a `dependabot.yml` validation error. The repository navigation shows 1 open pull request, so the banner probably dates from the 2026-10-14 jobs, when PRs #1 to #5 were open; that is the orchestrator's inference, not an observation. Image #2 shows Actions, Dependabot Updates, with 2 runs on `custom_build`, both successful: `github_actions in /. - Update #1627650108` (Oct 15, 00:09 GMT+3, 1m 0s) and `gradle in /. - Update #1627525145` (Oct 15, 00:05 GMT+3, 3m 17s, which is 2026-10-14T21:05Z and matches E-01 `dependabot_gradle_job`). Its sidebar lists "Integration Tests" as "Disabled", which corroborates OPS-01.

Command re-run by the orchestrator after the first answers: `security find-generic-password -s gsd17-gh-actions-read >/dev/null 2>&1; echo $?` (exit status only, 2026-10-17T08:30:20Z)

```text
ops01_disable_date_user: not stated in the first answer ("1. disabled"); resolved below
ops01_disable_date_api: 2026-10-16T09:38:23Z
ops01_dates_agree: no
ops01_disable_instant_resolved: 2026-10-16T09:38:23Z
keychain_lookup_exit_after: 44
OPS-01 disable recorded: pass
OPS-02: unmeasured (no "Last checked" time for the gradle entry was reported or is visible in the screenshot; the Dependabot tab shows an open-PR-limit banner, not a configuration error; gradle in / job #1627525145 succeeded 2026-10-14T21:05:35Z)
OPS-03: pass
```

`ops01_dates_agree` is `no` because the first answer stated no date, not because the dates conflict. The user confirmed the API instant in the resolution above, so `ops01_disable_instant_resolved` equals E-01 `workflow_updated_at`. OPS-01's no-scheduled-run observation is E-13 (plan 21-05), not this entry.

Timestamp: 2026-10-17T08:37:22Z

Verdict: unmeasured (OPS-02 unmeasured: no gradle "Last checked" time reported; OPS-01 disable recorded and OPS-03 pass)

### E-03: DEPS-01 merge of origin/custom_build (6473e24f)

Commands as run (repo root, branch `sync/v1.7.0`, main working tree, scratch in `/tmp/gsd21/exec/`):

1. `git ls-remote origin refs/heads/custom_build`, then `git fetch origin` and `git rev-parse origin/custom_build`
2. `git rev-parse HEAD` (the `pre_merge_head`)
3. Rehearsal in `git worktree add --detach /tmp/gsd21/wt-rehearsal HEAD`: `git merge --no-commit --no-ff origin/custom_build`, `git ls-files -u`, `git diff --cached --name-only`, `git merge --abort`, `git worktree remove --force`
4. One `/bin/bash -c` with `set -euo pipefail`: `git merge --no-commit --no-ff origin/custom_build`, assert `git ls-files -u` empty, line-addressed `sed -i ''` on fork-anchors.txt line 43 and FORK-DELTA.md lines 63 and 65, `git add` of those two files, assert the staged set is the four paths, `git commit -F /tmp/gsd21/exec/merge-msg.txt`
5. `git rev-list --left-right --count origin/custom_build...HEAD`, `git diff --numstat HEAD^1 HEAD`, `git rev-list --first-parent --merges pre-v1.7.0-sync..HEAD`

```text
remote_tip: 6473e24f372a8b37dfd8cc8cf3124e6cff52edca (ls-remote and fetch agree)
pre_merge_head: 54d19dc506a8b43f561b69d6b48cc4d3a2218910
rehearsal: clean, 2 files staged (build.gradle.kts, gradle/libs.versions.toml), no unmerged paths
unmerged_paths_real_merge: none
merge_commit: 82524e12969cdcd8345835c1082b30b04da31c5a
merge_parents: 54d19dc506a8b43f561b69d6b48cc4d3a2218910 6473e24f372a8b37dfd8cc8cf3124e6cff52edca
numstat (merge vs first parent):
1	1	.planning/scripts/fork-anchors.txt
1	1	build.gradle.kts
2	2	docs/FORK-DELTA.md
2	2	gradle/libs.versions.toml
left_right_count (origin/custom_build...HEAD): 0	27
first-parent merges, newest first: 82524e12969cdcd8345835c1082b30b04da31c5a, 54e6364858cd728e3427ce2058fc00380895c689
```

Commit message body (verbatim):

```text
This merge commit also carries three bookkeeping edits that are in neither parent (D-03):
- .planning/scripts/fork-anchors.txt: the BLD-08 row for build.gradle.kts moves from slf4j-reload4j:2.0.17 to slf4j-reload4j:2.0.20.
- docs/FORK-DELTA.md: the BLD-08 row now names org.slf4j:slf4j-reload4j:2.0.20.
- docs/FORK-DELTA.md: the BLD-14 row now names Lombok 1.18.48.

They are made so that the merge commit satisfies its own anchors file, which leaves 6473e24f as the only commit the anchors gate has to exempt. No dependency is bumped beyond the three Dependabot commits (#1 lombok, #2 spotless, #3 slf4j-reload4j).
```

D-03 reversibility: before Phase 24's push, `git reset --hard 54d19dc506a8b43f561b69d6b48cc4d3a2218910` on `sync/v1.7.0` (a local reset to `pre_merge_head`) redoes the merge. Nothing was pushed and no rebase or force was used.

Timestamp: 2026-10-17T08:40:46Z (merge committed; the `MERGE-SHAPE-OK` verify ran after the commit)

Verdict: pass

### E-04: Gate on the merged tree before the exemption (unmodified script)

Command as run (repo root, unmodified script, background, redirected to files):

`bash .planning/scripts/verify-merge-invariants.sh 54e63648 HEAD > /tmp/gsd21/exec/gate-merge.out 2> /tmp/gsd21/exec/gate-merge.err; echo $? > /tmp/gsd21/exec/gate-merge.rc`

```text
exit status (gate-merge.rc): 1
stderr (gate-merge.err), exactly one line:
ANCHOR VIOLATION: 6473e24f build.gradle.kts 'slf4j-reload4j:2.0.17' expected 1 got 0 (BLD-08)
stdout last line (gate-merge.out):
invariants: provenance_paths=21 anchor_commits=383 anchor_rows=59 marker_commits=430 result=FAIL
```

No line names the merge commit `82524e12`, so the D-03 edits are complete and `6473e24f` is the only commit plan 21-03 must exempt.

Timestamp: 2026-10-17T08:45:46Z (gate exit status file written)

Verdict: pass

### E-05: slf4j resolution before and after (Phase 24 rollout note)

Commands as run (repo root, `env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew -q --console=plain dependencyInsight --dependency {D} --configuration {C}`; line 1 of each `/tmp/gsd21/exec/dep-{before,after}-*.txt` is the selected coordinate). Before = `54d19dc5`, after = the merge `82524e12`.

| Dependency | Configuration | Before | After |
|------------|---------------|--------|-------|
| `org.slf4j:slf4j-api` | runtimeClasspath | 2.0.17 | 2.0.20 |
| `org.slf4j:slf4j-api` | testRuntimeClasspath | 2.0.17 | 2.0.20 |
| `org.slf4j:slf4j-api` | integrationTestRuntimeClasspath | 2.0.18 | 2.0.20 |
| `org.slf4j:slf4j-reload4j` | runtimeClasspath | 2.0.17 | 2.0.20 |

The catalog line `slf4j = "2.0.18"` in `gradle/libs.versions.toml` is unchanged (bumping it is deferred in CONTEXT). The `slf4j-api` bump comes through `slf4j-reload4j`'s transitive requirement.

Timestamp: 2026-10-17T08:45:58Z (after-captures finished)

Verdict: pass

### E-06: testUnit and the Lombok warning check with its control

Commands as run (repo root, merged tree `82524e12`):

1. `env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain clean compileJava compileTestJava compileIntegrationTestJava compileTestFixturesJava > /tmp/gsd21/exec/compile-merge.log 2>&1`
2. `awk 'tolower($0) ~ /ambiguous/ {n++} END{print n+0}' /tmp/gsd21/exec/compile-merge.log`
3. Control, in `git worktree add --detach /tmp/gsd21/wt-lombok HEAD` with `lombok.config` deleted there: `env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain clean compileJava > /tmp/gsd21/exec/compile-nocfg.log 2>&1`, then `git worktree remove --force`
4. `env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain testUnit --rerun > /tmp/gsd21/exec/testunit-merge.log 2>&1`

```text
lombok in catalog: 1.18.48; spotless: 8.10.4
clean compile of main, test, integrationTest and testFixtures: exit 0, BUILD SUCCESSFUL
case-insensitive "ambiguous" lines in compile-merge.log: 0
control (no lombok.config), compile-nocfg.log line 9: .../ClickHouseFieldDescriptor.java:18: warning: Ambiguous: Jackson2 and Jackson3 exist; define which variant(s) you want in 'lombok.config'. See https://projectlombok.org/features/experimental/Jacksonized
lombok.config in the main tree: unchanged (git diff --quiet HEAD -- lombok.config)
testUnit: Test result: SUCCESS
Tests summary: 556 tests, 554 succeeded, 0 failed, 2 skipped
```

The counts are recorded as measured, not asserted. `BUILD-EVIDENCE-OK` was printed by the plan's first Task 2 verify command after these logs were written.

Timestamp: 2026-10-17T08:46:24Z (testUnit log written)

Verdict: pass

### E-07: Spotless baseline-equal under 8.10.4 (D-14)

Commands as run: `git worktree add --detach /tmp/gsd21/wt-fmt-pre 82524e12^1` and `git worktree add --detach /tmp/gsd21/wt-fmt-post 82524e12`; in each `env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew -q spotlessApply`, then `git diff | md5`, `git diff --name-only | wc -l | tr -d ' '` and `git diff --shortstat | sed 's/^ *//'` into `/tmp/gsd21/exec/spotless-{pre,post}.txt`; `cmp -s` of the two files; both worktrees removed (`git worktree list` shows only the main tree).

```text
spotless-pre.txt  (merge first parent, spotless 8.10.1):
3393873370f9b48d2239131eedba8ff8
46
46 files changed, 260 insertions(+), 337 deletions(-)

spotless-post.txt (merge commit, spotless 8.10.4):
3393873370f9b48d2239131eedba8ff8
46
46 files changed, 260 insertions(+), 337 deletions(-)

cmp: identical
```

The post-merge result equals the plan-time 8.10.1 baseline, so the merge adds no spotless violation. No reformat commit was made (D-14), and no spotlessApply ran in the main tree.

Timestamp: 2026-10-17T08:46:35Z (spotless-post.txt written)

Verdict: pass

### E-08: Gate with and without the exemption (D-01, D-02)

Commands as run (repo root, after commit `a231d4f1`, background, both in parallel, output in `/tmp/gsd21/exec/gate-exempt.*` and `gate-noexempt.*`):

1. `bash .planning/scripts/verify-merge-invariants.sh 54e63648 HEAD`
2. `ANCHOR_EXEMPTIONS_FILE= bash .planning/scripts/verify-merge-invariants.sh 54e63648 HEAD`

```text
run 1 (default, exemption file at HEAD): exit status 0, stderr empty
invariants: provenance_paths=21 anchor_commits=386 anchor_rows=59 marker_commits=433 anchor_exempt=1 result=PASS

run 2 (ANCHOR_EXEMPTIONS_FILE= , lookup disabled): exit status 1, stderr exactly one line
ANCHOR VIOLATION: 6473e24f build.gradle.kts 'slf4j-reload4j:2.0.17' expected 1 got 0 (BLD-08)
invariants: provenance_paths=21 anchor_commits=386 anchor_rows=59 marker_commits=433 anchor_exempt=0 result=FAIL
```

The one data row in `.planning/scripts/fork-anchor-exemptions.txt` is what lets the full range `pre-v1.7.0-sync..HEAD` pass, and `anchor_exempt=1` records that exactly one violation was forgiven.

Phases 22-24: `PRE_TAG` stays `pre-v1.7.0-sync` (`c61d90ce`), and the gate command stays `bash .planning/scripts/verify-merge-invariants.sh 54e63648 HEAD`. Phase 24's "PRE_TAG Phase 21 chose" means this default plus `.planning/scripts/fork-anchor-exemptions.txt`. Another published bump of an anchored line is absorbed by adding a row to that file, not by editing the script.

Hand-off to Phase 24: the "mechanical checks" paragraph of `docs/FORK-DELTA.md` (near L206) should name `fork-anchor-exemptions.txt` beside `fork-anchors.txt`. Per the ROADMAP shared-files rule Phase 21 edited only its own FORK-DELTA rows, so this sentence belongs to Phase 24's completeness pass. It is not done here.

Timestamp: 2026-10-17T08:54:45Z (gate-exempt.rc and gate-noexempt.rc written)

Verdict: pass

### E-09: Self-test, fail-first (D-04)

Commands as run (three background runs in parallel, combined output and exit status in `/tmp/gsd21/exec/selftest-{new,oldgate,mutant}.{out,rc}`):

1. new: `bash .planning/scripts/verify-merge-invariants-selftest.sh` in the main tree (patched gate, uncommitted self-test at the time of the run).
2. oldgate: a throwaway worktree at `a231d4f1^` (`a044dded`, the gate before the patch), with the new self-test copied in.
3. mutant: a throwaway worktree at `a231d4f1` whose gate lookup was changed from `grep -qxF -e "${c}${TAB}${a_path}${TAB}${a_pattern}"` to the SHA-only `grep -qF -e "${c}"`, with the new self-test copied in.

```text
new (exit status 0, 16 PASS, 0 FAIL, 0 SKIP):
PASS ST1 ST2 ST3 ST4
PASS ST5-empty-file ST5-disabled ST6-other-row ST6-other-commit
PASS ST7-other-bld08-row ST7-exempted-row-silent
PASS ST8-short-sha ST8-uppercase-sha ST8-missing-reason ST8-empty-reason ST8-extra-field
PASS ST9-exact-row
selftest: no failures

oldgate (exit status 1, 9 PASS, 7 FAIL):
PASS ST1 ST2 ST3 ST4 ST5-empty-file ST5-disabled ST6-other-row ST6-other-commit ST7-other-bld08-row
FAIL ST7-exempted-row-silent (stderr still reports slf4j-reload4j:2.0.17)
FAIL ST8-short-sha, ST8-uppercase-sha, ST8-missing-reason, ST8-empty-reason, ST8-extra-field
     (each: exit 1, but no 'ANCHOR EXEMPTION VIOLATION' on stderr)
FAIL ST9-exact-row (exit 1, result=FAIL: the old gate has no exemption)
selftest: 7 case(s) failed

mutant (SHA-only lookup; exit status 1, 14 PASS, 2 FAIL):
FAIL ST6-other-row (exit 0, wanted non-zero with the slf4j violation)
FAIL ST7-other-bld08-row (exit 0, wanted non-zero with the kafka violation)
selftest: 2 case(s) failed
```

ST5 and ST6 pass against the old gate because they assert narrowness, not the feature. ST7-exempted-row-silent, ST8-* and ST9-exact-row are the fail-first anchors. ST6-other-row and ST7-other-bld08-row are the cases that go red on an over-broad lookup.

Default merge lookup after the DEPS-01 merge (`git rev-list --first-parent --merges pre-v1.7.0-sync..HEAD`, newest first, so `tail -1` is the oldest):

```text
82524e12969cdcd8345835c1082b30b04da31c5a
54e6364858cd728e3427ce2058fc00380895c689
```

The self-test header (line 6) now says the default is the oldest first-parent merge, and ST4's needle is `'TARGET_TABLE_FILTER' expected`, the row its awk breaks.

Refs unchanged: `git for-each-ref --format='%(objectname) %(refname)'` before and after the three runs is byte-identical (145 lines, `cmp` clean). Both throwaway worktrees were removed and `git worktree list` shows only the main tree. No `git stash` was used.

Timestamp: 2026-10-17T09:02:56Z (selftest-new.rc and selftest-oldgate.rc written)

Verdict: pass

### E-10: Pin-guard red run against the old regexes (D-07)

State under test: HEAD is `37ef6d40` (`refactor(21-04)`, the behaviour-preserving extraction with the old `RUNS_ON` and `WRITE_SCOPE`), plus the uncommitted working-tree edit that adds the 18 fixture tests, the 3 new real-file tests, the helpers `ktsCode`, `excludePaths` and `indent`, and the new rules `forkCountStepProblem`, `nprocProblem`, `forkTaskProblem` and `dependabotExcludeProblem` as accept-all bodies (`return Optional.empty();`). Nothing was committed for this state, so no red commit exists and every commit keeps `testUnit` green.

Command as run (repo root, output in `/tmp/gsd21/exec/pinguard-red.log`, exit status in `pinguard-red.rc`, report copied to `pinguard-red.xml`):

```text
env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain testUnit --tests '*TestsWorkflowPinGuardUnitTest' --rerun
exit status: 1
Tests summary: 26 tests, 20 succeeded, 6 failed, 0 skipped
```

The XML has `tests="26" failures="6" errors="0" skipped="0"`. The six failing tests are exactly the predicted set; all eight real-file tests and every D-10 must-still-reject group for runs-on and permissions are green.

1. `fixture: runs-on rule accepts ubuntu-24.04 with a trailing comment (CI-19)`: 2 failures

```text
must accept 'runs-on: ubuntu-24.04 # pinned' but the rule said: runs-on must be the pinned runner, not 'ubuntu-24.04 # pinned'
must accept 'runs-on: ubuntu-24.04    # padded' but the rule said: runs-on must be the pinned runner, not 'ubuntu-24.04    # padded'
```

2. `fixture: write-scope rule ignores step name and run text (CI-19)`: 6 failures

```text
must accept '- name: write summary' but the rule said: No line may grant a write scope: '- name: write summary'
must accept 'name: write summary' but the rule said: No line may grant a write scope: 'name: write summary'
must accept '- name: Write summary: write' but the rule said: No line may grant a write scope: '- name: Write summary: write'
must accept 'run: echo "mode: write"' but the rule said: No line may grant a write scope: 'run: echo "mode: write"'
must accept 'echo "contents: write" >> "$GITHUB_STEP_SUMMARY"' but the rule said: No line may grant a write scope: 'echo "contents: write" >> "$GITHUB_STEP_SUMMARY"'
must accept 'run: write' but the rule said: No line may grant a write scope: 'run: write'
```

3. `fixture: write-scope rule rejects contents write, write-all, quoted write and a job-level block (D-10)`: 2 failures, both the quoted forms

```text
must reject 'contents: "write"' but the rule accepted it
must reject 'contents: 'write'' but the rule accepted it
```

4. `fixture: fork-count step rule rejects a shell nproc formula, an echoed or misplaced call and a renamed step (CI-18)`: 7 failures, one per fixture (the rule accepts everything)

```text
must reject 'steps: / - name: Report test fork count / run: | / forks=$(( $(nproc) / 2 )) / echo "::error::reportTestForks did not print an integer"' but the rule accepted it
must reject 'steps: / - name: Report test fork count / run: | / echo hello /  / - name: Compile integration-test sources / run: ./gradlew -q reportTestForks' but the rule accepted it
must reject 'steps: / - name: Report test fork count / run: | / echo "./gradlew reportTestForks"' but the rule accepted it
must reject 'steps: / - name: Report test fork count / run: | / forks="$(./gradlew -q --console=plain reportTestForks | tail -n 1)" / case ... / msg="runner nproc=$(nproc), ..." / echo "$msg" / forks=$(( $(nproc) / 2 ))' but the rule accepted it
must reject '- name: Count forks / run: | / forks="$(./gradlew -q --console=plain reportTestForks | tail -n 1)"' but the rule accepted it
must reject '(empty list)' but the rule accepted it
must reject 'steps: / - name: Report test fork count / run: | / forks="$(./gradlew -q --console=plain reportTestForks | tail -n 1)" / msg="$(( $(nproc) / 2 ))"' but the rule accepted it
```

5. `fixture: reportTestForks task rule rejects a deleted, renamed, commented-out or constant task (CI-18)`: 6 failures

```text
must reject '// reportTestForks' but the rule accepted it
must reject 'tasks.register("reportForks") { / ... }' (renamed) but the rule accepted it
must reject 'tasks.register("reportTestForks") { /     println(6) / }' but the rule accepted it
must reject '... tasks.named<Test>("testUnit") ...' but the rule accepted it
must reject '/* / tasks.register("reportTestForks") { ... } / */' (whole block commented out) but the rule accepted it
must reject '// tasks.register("reportTestForks") { / tasks.register("other") { ... maxParallelForks ... / }' but the rule accepted it
```

6. `fixture: exclude-paths rule rejects a missing, misspelt, stale or misplaced path (17-REVIEW WR-01)`: 5 failures, one per fixture: `release.yaml` missing, the typo `integrationTest.yaml`, the stale `gone.yml`, all four paths only under the `gradle` entry, and the empty list.

Old rule versus expected verdict:

| Case | Old rule | Expected | Red because |
|------|----------|----------|-------------|
| CI-19 `runs-on: ubuntu-24.04 # pinned` and the padded form | rejected | accepted | falsely rejected: the group is `ubuntu-24.04 # pinned` |
| CI-19 step `name:` and `run:` text with `: write` (six fixtures) | rejected | accepted | falsely rejected by `.*:\s*write\b.*` |
| `contents: "write"` and `contents: 'write'` | accepted | rejected | falsely accepted: a hole in the old regex, not named in CONTEXT |
| CI-18 fork-count step, `reportTestForks` task and D-09 exclude-paths rules | no rule yet (accept-all) | rejected where bad | red by construction |
| D-10 must-still-reject: `contents: write`, `permissions: write-all`, `id-token: write`, `packages: write # why`, flow mappings, the job-level block, `runs-on: ubuntu-latest # x`, `ubuntu-22.04 # x`, `ubuntu-24.04#x`, `runs-on:`, empty list | rejected | rejected | green: the regression guards the fix must not weaken |

Timestamp: 2026-10-17T09:08:41Z (pinguard-red.xml written)

Verdict: pass

### E-11: Pin-guard green run, per-file spotless and full testUnit (CI-18, CI-19, D-09, D-14)

After swapping in the fixed `RUNS_ON`, the `WRITE_SCALAR`/`WRITE_FLOW` pair and the real bodies of `forkCountStepProblem`, `nprocProblem`, `forkTaskProblem` and `dependabotExcludeProblem` (regexes as in 21-RESEARCH Pattern 4, "New constants and regexes (final forms)"). No fixture was changed between E-10 and E-11.

Commands as run (repo root, logs in `/tmp/gsd21/exec/`):

```text
1. env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain testUnit --tests '*TestsWorkflowPinGuardUnitTest' --rerun
   (pinguard-green.log, pinguard-green.xml)
   exit status 0; XML: tests="26" skipped="0" failures="0" errors="0"
   Tests summary: 26 tests, 26 succeeded, 0 failed, 0 skipped

2. env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain testUnit --rerun
   (testunit-pinguard.log, exit status 0 in testunit-pinguard.rc)
   Test result: SUCCESS
   Tests summary: 577 tests, 575 succeeded, 0 failed, 2 skipped
   (556 before this plan plus the 21 added tests; the two skips are the same two ConvertToMapOrJsonPerformanceTestJson tests as in E-06)

3. per-file spotless (throwaway worktree /tmp/gsd21/wt-fmt at HEAD, the edited class copied in,
   env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew -q spotlessApply, cmp, worktree removed):
   reformatted files in the worktree: 47 (the 46 baseline files plus the copied class, so spotlessApply ran)
   cmp of the worktree's class with the main tree's class: identical
   PER-FILE-SPOTLESS-CLEAN
```

Guarded files: `git diff --quiet 82524e12 HEAD -- .github build.gradle.kts gradle src/main` succeeds and `git diff --quiet -- .github build.gradle.kts gradle` succeeds in the working tree. `tests.yaml`, `build.gradle.kts` and `dependabot.yml` are untouched since the DEPS-01 merge, and `timeout-minutes: 45` is still in `tests.yaml`. No `spotlessApply` ran in the main tree.

The eight real-file tests pass unchanged on the real files: the five existing display names and the three new ones (`the Report test fork count step calls ./gradlew reportTestForks and tests.yaml holds no nproc formula (CI-18)`, `build.gradle.kts registers reportTestForks reading maxParallelForks from the test task (CI-18)`, `dependabot.yml excludes the four upstream workflows from github-actions updates and lists only existing workflow files (17-REVIEW WR-01)`).

#### Accept-set changes (D-10)

This is the deliberate, user-decided exception (D-10) to AGENTS.md's rule "Existing tests: only add new tests, never delete/weaken existing ones": the runs-on and write-scope rules were narrowed so they stop failing on valid input, and they were also strengthened where the old regex had a hole. No existing `@Test`, `@DisplayName` or assertion message text changed (only the "expected/but was" suffix JUnit appended to the former `assertEquals` checks is gone, because each message already names the offending value). The "before" column is the old regex as measured in E-10, the "after" column is the fixed regex as measured in this entry.

| Input | Rule | Before (old regex, E-10) | After (fixed regex, E-11) | Basis |
|-------|------|--------------------------|---------------------------|-------|
| `runs-on: ubuntu-24.04 # pinned` | runs-on | rejected | accepted | D-10: a whitespace-preceded trailing comment is stripped before the equality check |
| `runs-on: ubuntu-24.04    # padded` | runs-on | rejected | accepted | D-10: same, with padding around the comment |
| `- name: write summary` | write-scope | rejected | accepted | D-10: step name text is not a write scope |
| `name: write summary` | write-scope | rejected | accepted | D-10: step name text is not a write scope |
| `- name: Write summary: write` | write-scope | rejected | accepted | D-10: step name text is not a write scope |
| `run: echo "mode: write"` | write-scope | rejected | accepted | D-10: run text is not a write scope |
| `echo "contents: write" >> "$GITHUB_STEP_SUMMARY"` | write-scope | rejected | accepted | D-10: run text is not a write scope |
| `run: write` | write-scope | rejected | accepted | D-10: run text is not a write scope |
| `contents: write` | write-scope | rejected | rejected | D-10: a real write grant stays rejected |
| `permissions: write-all` | write-scope | rejected | rejected | D-10: a real write grant stays rejected |
| `id-token: write` | write-scope | rejected | rejected | D-10: a real write grant stays rejected |
| `packages: write # why` | write-scope | rejected | rejected | D-10: a real write grant stays rejected |
| `permissions: { contents: write }` | write-scope | rejected | rejected | D-10: a flow-mapping write grant stays rejected |
| `job-level write block: permissions: / contents: read / jobs: / build: / permissions: / contents: write` | write-scope | rejected | rejected | D-10: a job-level write block stays rejected |
| `runs-on: ubuntu-latest # x` | runs-on | rejected | rejected | D-10: a floating runner stays rejected even with a comment |
| `runs-on: ubuntu-24.04#x` | runs-on | rejected | rejected | D-10: no whitespace before the hash, so not a comment |
| `contents: "write"` | write-scope | accepted | rejected | D-10: the quoted form was a hole in the old regex, now closed |
| `contents: 'write'` | write-scope | accepted | rejected | D-10: the quoted form was a hole in the old regex, now closed |

Eight inputs are newly accepted (two runs-on trailing-comment fixtures, six step-text fixtures), exactly the eight fixtures whose E-10 messages show the old rule rejecting them. `- run: echo write-all` is not among them: the old regex already accepted it. One class is newly rejected (the quoted `"write"` and `'write'` forms), so the read-only pin is strictly stronger on real grants.

Timestamp: 2026-10-17T09:10:18Z (testunit-pinguard.rc and per-file spotless result)

Verdict: pass

### E-12: Phase close gate at the tip (DEPS-01, D-01, D-04, D-14)

Commands as run (repo root, tip `4ba6ce23dd09589edad48168c53251b439a22923` on `sync/v1.7.0`, main working tree, output in `/tmp/gsd21/exec/`):

1. Background, in parallel: `bash .planning/scripts/verify-merge-invariants.sh 54e63648 HEAD > gate-final.out 2> gate-final.err; echo $? > gate-final.rc` and `bash .planning/scripts/verify-merge-invariants-selftest.sh > selftest-final.out 2>&1; echo $? > selftest-final.rc`
2. `env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain testUnit --rerun > testunit-final.log 2>&1`
3. Spotless in `git worktree add --detach /tmp/gsd21/wt-fmt-final HEAD`: `env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew -q spotlessApply`, then `git diff | md5`, `git diff --name-only | wc -l | tr -d ' '` and `git diff --shortstat | sed 's/^ *//'` into `spotless-final.txt`; worktree removed (`git worktree list` shows only the main tree).
4. Scope and no-push: `git diff` / `git ls-remote origin` / `git rev-list --left-right --count origin/custom_build...HEAD`, with M the newest first-parent merge in `pre-v1.7.0-sync..HEAD`.

```text
gate (gate-final.rc): 0, stderr (gate-final.err) empty
invariants: provenance_paths=21 anchor_commits=391 anchor_rows=59 marker_commits=438 anchor_exempt=1 result=PASS

self-test (selftest-final.rc): 0, 16 PASS lines, last line:
selftest: no failures

testUnit (testunit-final.log):
Test result: SUCCESS
Tests summary: 577 tests, 575 succeeded, 0 failed, 2 skipped

spotless-final.txt (tip, spotless 8.10.4; equals E-07's):
3393873370f9b48d2239131eedba8ff8
46
46 files changed, 260 insertions(+), 337 deletions(-)

M (newest first-parent merge): 82524e12969cdcd8345835c1082b30b04da31c5a, second parent 6473e24f372a8b37dfd8cc8cf3124e6cff52edca
non-.planning paths changed M^1..HEAD (exactly four):
build.gradle.kts
docs/FORK-DELTA.md
gradle/libs.versions.toml
src/test/java/com/clickhouse/kafka/connect/ci/TestsWorkflowPinGuardUnitTest.java
git diff --quiet M HEAD -- src/main src/integrationTest src/testFixtures .github build.gradle.kts gradle lombok.config VERSION settings.gradle.kts: succeeds
src/test changed M..HEAD: only the pin-guard class (TestsWorkflowPinGuardUnitTest.java)
git diff --quiet v1.7.0 HEAD -- .github/workflows/integrationTests.yaml: succeeds (identical)
git ls-remote origin: refs at HEAD's SHA = 0
git ls-remote origin refs/heads/custom_build: 6473e24f372a8b37dfd8cc8cf3124e6cff52edca
left-right count origin/custom_build...HEAD: 0	35 (left side 0, so origin/custom_build is contained in HEAD)
```

The three DEPS-01 merge files are changed only by the merge commit M, and the pin-guard class is the only Phase 21 change to `src/test`. Nothing was pushed.

Timestamp: 2026-10-17T09:20:12Z (selftest-final.rc and gate-final.rc written; spotless and scope facts taken minutes earlier at the same tip)

Verdict: pass
