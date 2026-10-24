# Phase 30: Command evidence

- **Base commit:** `P30_BASE` = `d6a574d09437f813c5967a118e81f2a07fb237f2` (HEAD when plan 30-01 started, branch `sync/v1.7.0`; every "Phase 30 changed nothing here" check diffs non-`.planning` paths against it or against tag `v1.7.0_gg4`).
- **Shell:** macOS zsh, BSD `grep`. Gradle is always run as `env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew ...` because this shell sets that variable to the literal `null`. Every observation of `testUnit` uses `--rerun`, because `testUnit` does not declare `.github/` files as inputs.
- **Date (UTC):** 2026-10-24.
- **Rule:** a `Matches text: no` verdict means the document gets fixed, never the evidence. Entries that back no document text carry `Matches text: n/a (gate check)`. Entries whose document is written later carry `Matches text: pending (30-0N)`, and the plan that writes that text flips the verdict.

### E-01: phase base, origin custom_build and the four pins re-resolved

Backs: REL-02; this is the gate check for the pin values used in every workflow of this phase. Read-only network access only (`git ls-remote`).

```sh
git ls-remote origin custom_build
git rev-parse HEAD
git rev-parse --abbrev-ref HEAD
git ls-remote https://github.com/actions/checkout.git refs/tags/v7.0.1
git ls-remote https://github.com/actions/setup-java.git refs/tags/v6.0.1
git ls-remote https://github.com/actions/setup-node.git refs/tags/v7.1.0
git ls-remote https://github.com/gradle/actions.git 'refs/tags/v6.4.0' 'refs/tags/v6.4.0^{}'
```

```text
21c51598b06b6f5af58430765d57b1e5f6747c5a	refs/heads/custom_build
d6a574d09437f813c5967a118e81f2a07fb237f2
sync/v1.7.0
3d3c42e5aac5ba805825da76410c181273ba90b1	refs/tags/v7.0.1
de7274f081f381c8f8158605e0321c36c376e2e6	refs/tags/v6.0.1
949feb2413d6458794dcd2491c4babbbce0c15c1	refs/tags/v7.1.0
b9bee63ef481cc545e4517ec730544a4d8b1dfa0	refs/tags/v6.4.0
3f5f9adaf7d9fecd50b5935e54106014257a94e6	refs/tags/v6.4.0^{}
```

All four pins equal the values of the plan: checkout `3d3c42e5`, setup-java `de7274f0`, setup-node `949feb24` (a lightweight tag), and setup-gradle by its peeled commit `3f5f9ada` (the tag object `b9bee63e` is never a pin). `origin/custom_build` equals tag `v1.7.0_gg4` (`21c51598`).

Matches text: n/a (gate check)

### E-02: the pin guard red on the unchanged release-path workflows

Backs: CIG-01 (the guard is seen red on the unchanged tree before any workflow edit). The guard `WorkflowActionPinGuardUnitTest` exists in the working tree and is uncommitted; the only change to `TestsWorkflowPinGuardUnitTest.java` is six removed `private` words (`USES_LINE`, `PINNED_USES`, `DEPENDABOT`, `WORKFLOWS_DIR`, `SHA`, `nonCommentLines`). No file under `.github/` has changed.

```sh
env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain testUnit --tests '*WorkflowActionPinGuardUnitTest' --tests '*TestsWorkflowPinGuardUnitTest' --rerun
# XML copies: build/test-results/testUnit/TEST-com.clickhouse.kafka.connect.ci.{WorkflowActionPinGuardUnitTest,TestsWorkflowPinGuardUnitTest}.xml
```

```text
exit status: 1
Test result: FAILURE
Tests summary: 60 tests, 58 succeeded, 2 failed, 0 skipped

WorkflowActionPinGuardUnitTest:      tests=21 failures=2 errors=0 skipped=0
TestsWorkflowPinGuardUnitTest:       tests=39 failures=0 errors=0 skipped=0

failing tests (exactly two):
  every fork-owned workflow under .github/workflows pins every action to a full commit SHA with a version comment (CIG-01)
  dependabot.yml excludes only the upstream-identical integrationTests.yaml, so Dependabot keeps every guarded workflow current (D-10)

file:line tokens in the pin test's message (12):
  .github/workflows/increment-version.yml:27
  .github/workflows/increment-version.yml:36
  .github/workflows/pre-release.yaml:15
  .github/workflows/pre-release.yaml:23
  .github/workflows/pre-release.yaml:29
  .github/workflows/pre-release.yaml:34
  .github/workflows/pre-release.yaml:44
  .github/workflows/release.yaml:15
  .github/workflows/release.yaml:23
  .github/workflows/release.yaml:29
  .github/workflows/release.yaml:34
  .github/workflows/release.yaml:44

D-10 test message:
  expected: <[.github/workflows/integrationTests.yaml]>
  but was:  <[.github/workflows/increment-version.yml, .github/workflows/integrationTests.yaml, .github/workflows/pre-release.yaml, .github/workflows/release.yaml]>

16 reject fixtures passed (all named "fixture: the pin rule rejects <form>, naming file and line (CIG-01)"):
  a major tag, a full version tag, a branch, a short SHA, a 39-hex SHA, a 41-hex SHA,
  an upper-case SHA, a SHA without a comment, a SHA with a branch comment,
  a SHA with a short version comment, a local action, a docker image tag,
  a docker image digest, a quoted value, a flow mapping, an action without an owner
```

The red state is exactly the prediction of the plan: the real-file pin test fails with the 12 predicted lines (the three release-path workflows; `tests.yaml` passes), and the Dependabot test fails because `exclude-paths` still lists three fork-owned workflows. Every fixture test passed, so the rule itself is sound before it is applied to the real files. `TestsWorkflowPinGuardUnitTest` stays 39/0/0/0 after the visibility edit.

Matches text: n/a (gate check)

### E-03: the rewritten release-path workflows before the green commit

Backs: REL-01, REL-02, REL-03, REL-04, REL-05. The three workflows are rewritten and uncommitted; `tests.yaml`, `integrationTests.yaml` and `.github/actions/` are untouched. Dependabot is not edited yet, so the D-10 test must still be the only red one.

```sh
ruby -ryaml -e '... YAML.load_file(f) must be a Hash ...' .github/workflows/release.yaml .github/workflows/pre-release.yaml .github/workflows/increment-version.yml
# plan verify command: 13 step names in order, publish/dry/cleanup if: expressions, permissions, defaults, pins, create lines, expression allow-list, increment-version shape
env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain testUnit --tests '*WorkflowActionPinGuardUnitTest' --rerun
git diff --stat -- .github/workflows
# local check of the build step's two facts, on the unchanged project:
env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew clean createConfluentArchive --console=plain
```

```text
yaml-ok 3
shape-problems 0 []
WORKFLOW-SHAPE-OK

guard run: exit 1, Tests summary: 21 tests, 20 succeeded, 1 failed, 0 skipped
failing test (only): dependabot.yml excludes only the upstream-identical integrationTests.yaml, so Dependabot keeps every guarded workflow current (D-10)

 .github/workflows/increment-version.yml |  31 +++--
 .github/workflows/pre-release.yaml      | 202 +++++++++++++++++++++++++------
 .github/workflows/release.yaml          | 206 +++++++++++++++++++++++++-------
 3 files changed, 352 insertions(+), 87 deletions(-)

wrapper Gradle version printed by `./gradlew --version`: 9.8.1
createConfluentArchive: exit 0, the plain-console log holds the exact line (grep -Fxn): 625:> Task :verifyConfluentArchive
archive: build/confluent/clickhouse-kafka-connect-v1.7.0_gg4.zip
```

The CIG-01 pin test and all 20 other guard tests pass on the rewritten files; the 12 lines that failed in E-02 are gone. The local run confirms the two facts the build step relies on (the exact finalizer line under `--console=plain`, and the archive name `clickhouse-kafka-connect-<VERSION>.zip`). Whether `gh release view` finds a draft by tag, and whether the matching-refs endpoint returns an empty array for no match, are runner facts that the operator dry run of plan 30-03 measures.

Matches text: n/a (gate check)

### E-04: the green commit

Backs: CIG-01, REL-01, REL-02, REL-03, REL-04, REL-05, D-10. One `ci(30-01)` commit holds the guard, both `TestsWorkflowPinGuardUnitTest` edits, the three workflows, `dependabot.yml` and the 29 anchor rows. The body of the commit names the D-10 reason (the three files are fork-owned and pinned now, so their pins would go stale without Dependabot; only upstream-identical `integrationTests.yaml` stays excluded; the user signed off this change of a pinned assertion) and every replaced fixture.

```sh
git show --stat --format='%H %s' 496de32a
# mirror: for every row of fork-anchors.txt, grep -cF of the pattern in the named file equals the row's count
env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain testUnit --rerun
git worktree add --detach /tmp/gsd30/wt-fmt 496de32a   # copy of nothing: the commit itself
( cd /tmp/gsd30/wt-fmt && env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew -q spotlessApply )
cmp <the two Java files, worktree vs main tree>
git worktree remove --force /tmp/gsd30/wt-fmt
git diff --quiet v1.7.0_gg4 HEAD -- src/main; echo $?
```

```text
496de32aa7a7847b56937646716fb600914ade54 ci(30-01): pin the release-path workflows, build with the wrapper, publish with gh release create, and guard every fork workflow (CIG-01, REL-01..REL-05)
 .github/dependabot.yml                             |   9 +-
 .github/workflows/increment-version.yml            |  31 ++-
 .github/workflows/pre-release.yaml                 | 202 ++++++++++++---
 .github/workflows/release.yaml                     | 206 +++++++++++++++++++++++++-------
 .planning/scripts/fork-anchors.txt                 |  30 +++
 .../connect/ci/TestsWorkflowPinGuardUnitTest.java  |  48 ++--
 .../connect/ci/WorkflowActionPinGuardUnitTest.java | 273 +++++++++++++++++++++
 7 files changed, 684 insertions(+), 115 deletions(-)

rows=123 bad=0
ANCHORS-MIRROR-OK

Test result: SUCCESS
Tests summary: 1299 tests, 1297 succeeded, 0 failed, 2 skipped      (the 2 skips are in other classes)
WorkflowActionPinGuardUnitTest:   tests=21 failures=0 errors=0 skipped=0
TestsWorkflowPinGuardUnitTest:    tests=39 failures=0 errors=0 skipped=0

spotlessApply in a worktree at 496de32a: exit 0, both Java files byte-identical to the main tree (SPOTLESS-IDENTICAL)
git diff --quiet v1.7.0_gg4 HEAD -- src/main: exit 0
```

Deviation from the plan's verify command 3: its `GREEN-COMMIT-OK` check rejects any `subList(` in `TestsWorkflowPinGuardUnitTest.java`, but four unrelated `subList(` calls predate this phase (three inside the Kotlin-script and YAML helpers at lines 191, 358 and 380, and `FIVE_PINNED.subList(0, 4)` at line 820, a different fixture). The check was written to find the replaced `UPSTREAM.subList(0, 3)` fixture; the equivalent check on exactly that text, on `UPSTREAM.get(1)`, `UPSTREAM.get(2)`, `UPSTREAM.get(3)` and on "four upstream workflows" finds 0 matches, and the misspelt-path fixture `integrationTest.yaml` is present. The same command's clean-tree clause also saw `.planning/ROADMAP.md` and `.planning/STATE.md` modified; those were dirty before the plan started (the orchestrator's planning-session edits), not produced by this plan. The method set is unchanged (41 methods before and after, none lost).

#### Fixture replacements (D-10)

| Test method | Old fixture | New fixture | Rule branch exercised | Verdict before and after |
|-------------|-------------|-------------|-----------------------|--------------------------|
| `dependabotExcludeRuleRejectsMissingOrStalePaths` | `dependabot(List.of(), UPSTREAM.subList(0, 3))` (three of the four upstream paths) | `dependabot(List.of(), List.of(WORKFLOWS_DIR + "increment-version.yml", WORKFLOWS_DIR + "pre-release.yaml", WORKFLOWS_DIR + "release.yaml"))` (the three guarded workflows without `integrationTests.yaml`) | a required upstream path is missing from `exclude-paths` | rejected before, rejected after |
| `dependabotExcludeRuleRejectsMissingOrStalePaths` | four-element list `UPSTREAM.get(0)`, `integrationTest.yaml` (misspelt), `UPSTREAM.get(2)`, `UPSTREAM.get(3)` | `List.of(".github/workflows/integrationTest.yaml")` | a misspelt path leaves the required path missing | rejected before, rejected after |
| `dependabotRulesAcceptValidYamlSpellings` | `reversed` = `UPSTREAM.get(3), get(2), get(1), get(0)` | `List.of(WORKFLOWS_DIR + "tests.yaml", UPSTREAM.get(0))` (required path second) | the rule does not depend on list order | accepted before, accepted after |
| `EXISTING_WORKFLOWS` (fixture world) | upstream four plus `tests.yaml` | `UPSTREAM` plus `increment-version.yml`, `pre-release.yaml`, `release.yaml`, `tests.yaml` | the "lists only existing files" branch sees the same directory as the real tree | unchanged verdicts |
| `dependabotExcludesTheUpstreamWorkflowsAndOnlyExistingFiles` (display name) | "excludes the four upstream workflows ..." | "excludes upstream's integrationTests.yaml ... (17-REVIEW WR-01, D-10)" | text only | passes before (on the old file) and after |
| `dependabotExcludeRuleAcceptsUpstreamWorkflows` (display name) | "accepts the four existing upstream workflows" | "accepts the upstream-identical integrationTests.yaml (17-REVIEW WR-01, D-10)" | text only | accepted before, accepted after |

`upstreamFixtureIsTheProductionConstant` (the `assertSame` test) is unchanged: `assertSame` is an identity check, so it does not depend on the list's size (PT-03).

Matches text: n/a (gate check)

### E-05: the cumulative anchors gate and its self-test

Backs: the phase gate for the anchors invariant. Gated HEAD: `496de32aa7a7847b56937646716fb600914ade54` (recorded as the gate started). Neither script nor `fork-anchor-exemptions.txt` is edited.

```sh
bash .planning/scripts/verify-merge-invariants.sh 54e63648 HEAD
bash .planning/scripts/verify-merge-invariants-selftest.sh
git diff --quiet d6a574d09437f813c5967a118e81f2a07fb237f2 HEAD -- .planning/scripts/verify-merge-invariants.sh .planning/scripts/verify-merge-invariants-selftest.sh .planning/scripts/fork-anchor-exemptions.txt
```

```text
invariants: provenance_paths=21 anchor_commits=849 anchor_rows=123 marker_commits=908 anchor_exempt=19 result=PASS      (exit 0)
selftest: no failures      (exit 0; last case PASS ST9-exact-row)
git diff --quiet (three scripts/files since P30_BASE): exit 0
```

Matches text: n/a (gate check)

### E-06: semver.inc on a vX.Y.Z_ggN VERSION (D-08)

Backs: REL-02. Measured, not read from the grammar. `semver@7.8.5` was installed into the new empty directory `/tmp/gsd30/semver` with `--no-save --ignore-scripts`; `npm view semver@7.8.5 version scripts.postinstall deprecated` printed only `7.8.5` (no postinstall script, not deprecated). Every `node` run started from the repository root with `NODE_PATH=/tmp/gsd30/semver/node_modules`, never with the download directory as the working directory. The full transcript is `/tmp/gsd30/exec/semver-d08.txt` (scratch, not committed).

```sh
npm view semver@7.8.5 version scripts.postinstall deprecated
npm install --prefix /tmp/gsd30/semver --no-save --ignore-scripts --no-audit --no-fund semver@7.8.5
node --version
NODE_PATH=/tmp/gsd30/semver/node_modules node -p "require('semver').inc('v1.7.0_gg4', 'patch')"
NODE_PATH=/tmp/gsd30/semver/node_modules node -p "require('semver').inc('v1.7.0_gg4', 'minor')"
NODE_PATH=/tmp/gsd30/semver/node_modules node -p "require('semver').inc('v1.7.0_gg4', 'major')"
NODE_PATH=/tmp/gsd30/semver/node_modules node -p "require('semver').valid('v1.7.0_gg4')"
NODE_PATH=/tmp/gsd30/semver/node_modules node -p "require('semver/package.json').version"
NODE_PATH=/tmp/gsd30/semver/node_modules node -e '<verify command 1, one line>' "$(tr -d '[:space:]' < VERSION)"
```

```text
7.8.5
added 1 package in 183ms
v24.14.1
null
null
null
null
7.8.5
semver-d08 v1.7.0_gg4 [null,null,null,null,"7.8.5"]      (exit 0)
```

`semver.inc('v1.7.0_gg4', ...)` returns `null` for `patch`, `minor` and `major`, and `semver.valid` returns `null`, for the research one-liner and for the `VERSION` file's own value, on Node `v24.14.1` with semver `7.8.5`. In `increment-version.yml` the `Increment version` step therefore sets `NEW_VERSION` to the string `null` (`node -p` prints `null`), writes `vnull` to `VERSION`, and `Commit and push` opens a pull request named `update-version-null` for it. The defect is recorded, not fixed (D-08).

Matches text: yes

### E-07: FORK-DELTA corrections after the green commit (UD-5, D-10)

Backs: REL-02, REL-04. Both statements of `docs/FORK-DELTA.md` that commit `496de32a` made false, before and after. Commits: `082c127b` (paragraph, with E-06) and the commit that holds this entry (the Dependabot row).

#### Workflow paragraph (`### Build, dependencies and CI`, the paragraph after the table)

Before (verbatim):

```text
`release.yaml`, `pre-release.yaml`, `increment-version.yml`, `integrationTests.yaml` and the composite action `.github/actions/use-clickhouse-java` are intentionally left exactly as upstream ships them; upgrading and pinning them is a Future Requirement in `.planning/REQUIREMENTS.md` (DEPS-F4). `pre-release.yaml` still builds with `gradle-version: '7.4.2'`, which cannot configure a build that needs Shadow 9 and Spotless 8; that was read from the workflow and the build requirements and not run.
```

After: the paragraph that starts `` `integrationTests.yaml` and the composite action `.github/actions/use-clickhouse-java` are intentionally left exactly as upstream ships them. `` It says the guard reads every workflow except `integrationTests.yaml` and fails on `./` and `docker://` `uses:` lines; that the three files are fork-modified (SHA pins checked by `WorkflowActionPinGuardUnitTest`, the wrapper through `gradle/actions/setup-gradle` so `verifyConfluentArchive` runs, the GitHub CLI release with the archive attached, `--prerelease` only in `pre-release.yaml`); the publish conditions (dispatched from `custom_build`, a successful `tests.yaml` run on the SHA, tag absent); the dry run (throwaway `dry-run-<run id>-<attempt>` draft, no-tag check, delete); and the `vnull` defect with `30-COMMAND-EVIDENCE.md` E-06. The full text is in `docs/FORK-DELTA.md`.

#### Dependabot row (`| Dependabot also updates the github-actions ecosystem ...`)

| Cell | Before | After |
|------|--------|-------|
| Behaviour | `... with `exclude-paths` leaving out the four upstream workflows` | `... with `exclude-paths` leaving out only upstream's byte-identical `integrationTests.yaml`, so the pins of `tests.yaml`, `release.yaml`, `pre-release.yaml`, `increment-version.yml` and any later fork workflow stay current` |
| Why upstream does not provide it | `... so a bump PR that still appears for an upstream workflow is closed, not merged.` | `... so a bump PR that still appears for `integrationTests.yaml` is closed, not merged. The three release-path workflows left the list when they were pinned (Phase 30, D-10), and `WorkflowActionPinGuardUnitTest` fails when the list holds anything else.` |
| Pinned by | `CI-15; `TestsWorkflowPinGuardUnitTest`` | `CI-15, REL-02; `TestsWorkflowPinGuardUnitTest`, `WorkflowActionPinGuardUnitTest`` |

Nothing else in the row or the file changed apart from the paragraph above.

#### Cross-check of the claims against the tree

```text
rows 1 missing [] tree {'exclude': True, 'composite': True, 'create': True, 'prerelease': True, 'node': True, 'stale-row': True}      (exit 0)
```

`exclude`: `.github/dependabot.yml` lists exactly `.github/workflows/integrationTests.yaml` under `exclude-paths`. `composite`: no workflow file names `use-clickhouse-java`. `create`: `release.yaml` and `pre-release.yaml` each hold two `gh release create` lines. `prerelease`: `--prerelease` is in `pre-release.yaml` only. `node`: `increment-version.yml` holds `node-version: '24'` and `semver@7.8.5`. `stale-row`: the text `leaving out the four upstream workflows` is gone.

#### Carried to Phase 32 (CIG-02)

These two clauses of the same row were already stale before this phase. They are not edited here (UD-5: Phase 32 makes the single FORK-DELTA refresh); the row's other statements are true now.

1. The parenthesis `Dependabot reads its configuration from the default branch (`main` here)`. Stale because the default branch of `origin` is `custom_build` (`git ls-remote --symref origin HEAD` prints `ref: refs/heads/custom_build	HEAD`; the plan records it as the default since 2026-10-14). The `test-head.yaml` row above it makes the same default-branch claim (`which on this fork is `main``) and is outside this plan's one-row scope; Phase 32 should check it together with this clause.
2. The clause `so the entry is inert until the user changes both; that is a manual check listed for the next release`. Stale because Dependabot is active on the fork: `git log origin/custom_build --author=dependabot` shows Dependabot commits on 2026-10-20 and 2026-10-21 (`83de3b78` gson 2.14.0, `de27d2e9` jackson 2.22.4, `2555fd2b` slf4j-simple 2.0.20), and the Dependabot merges already counted in this file's `v1.7.0_gg4` paragraph.

Matches text: yes
