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
| E-08 | DEPS-01, D-01, D-02 (gate with and without the exemption) | pending (plan 21-03) |
| E-09 | D-04 (self-test) | pending (plan 21-03) |
| E-10 | CI-18, CI-19, D-07 (pin-guard red run) | pending (plan 21-04) |
| E-11 | CI-18, CI-19, D-09, D-14 (pin-guard green run) | pending (plan 21-04) |
| E-12 | DEPS-01 (phase close gate) | pending (plan 21-05) |
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
