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

### E-08: dry-run readiness, the REST reader and the before-snapshots (REL-05)

Backs: REL-05 (gate check for the push that Task 2 asks the user about). Read-only network access only (`git fetch`, `git ls-remote`, unauthenticated `api.github.com`). Nothing was pushed, dispatched or run through `gh`.

```sh
curl -sS https://api.github.com/rate_limit
git fetch --quiet origin --no-tags
git ls-remote --heads origin custom_build
git ls-remote --heads origin sync/v1.7.0
git rev-parse --abbrev-ref HEAD
git config --get push.followTags; git config --get remote.origin.push; git config --get remote.origin.mirror
git ls-files .planning/.gsd-ws-arg
git diff --quiet v1.7.0_gg4 HEAD -- src/main
git diff --name-only v1.7.0_gg4 HEAD | grep -v '^\.planning/'
git diff --name-only v1.7.0_gg4 HEAD | grep -c '^\.planning/'
git diff v1.7.0_gg4 HEAD | grep -Ec 'ghp_[A-Za-z0-9]{20,}|github_pat_[A-Za-z0-9_]{20,}|gh[ousr]_[A-Za-z0-9]{30,}|AKIA[0-9A-Z]{16}|-----BEGIN [A-Z ]*PRIVATE KEY-----'
git ls-remote origin | grep -F "$(git rev-parse HEAD)"
git log --oneline v1.7.0_gg4..HEAD -- . ':(exclude).planning'
git diff --shortstat v1.7.0_gg4 HEAD
```

```text
rate limit before: limit 60, remaining 60
custom_build on origin: 21c51598b06b6f5af58430765d57b1e5f6747c5a  (unchanged since planning)
sync/v1.7.0 on origin: (empty)
current branch: sync/v1.7.0
push.followTags, remote.origin.push, remote.origin.mirror: (all unset, no output)
.planning/.gsd-ws-arg tracked: (empty; the file does not exist)
src/main differs from v1.7.0_gg4: no (exit 0)
paths outside .planning/ that differ from v1.7.0_gg4:
  .github/dependabot.yml
  .github/workflows/increment-version.yml
  .github/workflows/pre-release.yaml
  .github/workflows/release.yaml
  docs/FORK-DELTA.md
  src/test/java/com/clickhouse/kafka/connect/ci/TestsWorkflowPinGuardUnitTest.java
  src/test/java/com/clickhouse/kafka/connect/ci/WorkflowActionPinGuardUnitTest.java
.planning/ files that differ from v1.7.0_gg4: 144
secret-scan hits (GitHub token, AWS access key, private-key block): 0
HEAD held by an origin ref: no (grep exit 1)
non-.planning commits since v1.7.0_gg4:
  2f66977a docs(30-02): FORK-DELTA's Dependabot row covers every fork workflow (D-10)
  082c127b docs(30-02): record the vnull defect and the fork-modified release workflows in FORK-DELTA (REL-02, D-08)
  496de32a ci(30-01): pin the release-path workflows, build with the wrapper, publish with gh release create, and guard every fork workflow (CIG-01, REL-01..REL-05)
total commits v1.7.0_gg4..HEAD (before the E-08 commit): 23
shortstat: 151 files changed, 4361 insertions(+), 739 deletions(-)
```

#### REST tracer on an existing run (tests.yaml run 40022281005 on custom_build 21c51598)

Bodies saved to `/tmp/gsd30/exec/dry/tracer-runs.json`, `tracer-jobs.json` (`/actions/runs/40022281005/jobs`) and `tracer-ann.json` (`/check-runs/120649561100/annotations?per_page=100`).

```text
runs for head_sha 21c51598: [(40022281005, .github/workflows/tests.yaml, success)]
job 120649561100 step "Report test fork count": success
annotation: notice | Test fork count | runner nproc=4, Gradle maxParallelForks=2 (read from the test task in build.gradle.kts)
```

The runs, jobs and annotations endpoints therefore return what Task 3 needs on T: a step conclusion, and annotations selected by title.

#### Before-snapshots (origin, saved before any push)

`/tmp/gsd30/exec/dry/origin-tags-before.txt`:

```text
6ac2d20cc0c7feacc315b21b322325b4fee25784	refs/tags/pre-v1.6.0-sync
c61d90ce5dfb32ea5876dffacc8bb54d8846739a	refs/tags/pre-v1.7.0-sync
be6b8861c25f3d7611194e7150b18362c5c75e32	refs/tags/pre-v1.7.0_gg2
6473e24f372a8b37dfd8cc8cf3124e6cff52edca	refs/tags/pre-v1.7.0_gg3
69cd680bf548bfb0329142752d37f8a6c3190c26	refs/tags/pre-v1.7.0_gg4
c61d90ce5dfb32ea5876dffacc8bb54d8846739a	refs/tags/v1.6.0_gg1
f328fd218dce5b9c652f8872042e922f9686b87f	refs/tags/v1.7.0_gg1
a246ca01b073faae86e2e92bb0288c07dd7eb0d7	refs/tags/v1.7.0_gg2
3376a18fd057dba329ceae7d8c74d1ba4e3d36f6	refs/tags/v1.7.0_gg3
21c51598b06b6f5af58430765d57b1e5f6747c5a	refs/tags/v1.7.0_gg4
```

`/tmp/gsd30/exec/dry/origin-heads-before.txt`:

```text
21c51598b06b6f5af58430765d57b1e5f6747c5a	refs/heads/custom_build
ca52b81ad1a188e1e288336a2231d9d3092d45fe	refs/heads/dependabot/gradle/clickHouseDriver-0.10.0
011bc42bfe8d8230c430ec8001d481885daec5fa	refs/heads/dependabot/gradle/com.google.protobuf-protobuf-java-4.36.2
bec167d85918e77c11621063000acc6e031ae2fa	refs/heads/main
```

`/tmp/gsd30/exec/dry/releases-before.json` (`/releases?per_page=100`): `[]` (a JSON array, no published release).

Deviation from the plan text: the plan's planning-time note says "11 tags including v1.7.0_gg4"; origin lists 10 tags today. Task 3 compares the after-state with this snapshot, not with the planning-time count, and no check depends on the number 11. It is raised in the checkpoint so the user can say whether a tag was removed on purpose. The heads are as planned (`custom_build` and `main`, plus two `dependabot/*` branches that the Task 3 comparison ignores).

Rate limit after the reads: 56 of 60 remaining.

#### Operator bundle for Task 2

Push command (exactly this, no other flag and no tag):

```sh
git push origin refs/heads/sync/v1.7.0:refs/heads/sync/v1.7.0
```

Delete command (only after both dispatch runs are green):

```sh
git push origin --delete refs/heads/sync/v1.7.0
```

Dispatch rule: `Use workflow from` is `sync/v1.7.0`, never `custom_build`. The run names must read `Pre-release sync/v1.7.0 (dry run)` and `Release sync/v1.7.0 (dry run)` (`run-name` of `.github/workflows/release.yaml` line 9).

Expected annotations of each `build_release` job (Task 3 checks them from the REST API; `VERSION` at T is `v1.7.0_gg4`, the wrapper's Gradle is `9.8.1`):

| Title | Level | Message |
|-------|-------|---------|
| Wrapper Gradle | notice | `Gradle 9.8.1 from the wrapper ran createConfluentArchive and verifyConfluentArchive` |
| Archive | notice | `clickhouse-kafka-connect-v1.7.0_gg4.zip <n> bytes sha256=<64 hex>` |
| tests.yaml on this SHA | notice | `successful tests.yaml runs on <T>: <n>` with n at least 1 |
| Release tag | warning | `tag v1.7.0_gg4 already exists ...` (advisory on a dry run, because the tag exists on origin) |
| Draft asset attached | notice | `dry-run-<id>-<attempt> holds clickhouse-kafka-connect-v1.7.0_gg4.zip (<n> bytes), draft=true, prerelease=true` for pre-release (`false` for release), `target=<T>` |
| No tag for the draft | notice | `refs/tags/dry-run-<id>-<attempt> does not exist while the draft is open` |
| Dry run cleaned up | notice | `no draft and no tag dry-run-<id>-<attempt> remain` |

`Publish release` is skipped, and no annotation has level failure.

T is the commit that holds this entry; its SHA is in `/tmp/gsd30/exec/dry/pushed-sha.txt` and in the Task 2 checkpoint.

Matches text: n/a (gate check)

### E-09: both dry runs, read from the REST API, and origin before and after (REL-05)

Backs: REL-05, REL-01, REL-03, REL-04 (criterion 4). T = `b847fbdbf54a42a94738bb7d1fb0c6668a58d5c2`, VERSION `v1.7.0_gg4`, wrapper Gradle 9.8.1.

user_report_received_by: orchestrator (direct)

user_report:
- option chosen: `push` (user's answer: "push")
- after the push and the tests.yaml read: "check", then "tell me step by step what should i do"
- dispatch report: "оба отработали в dry run, вкладка релизы пустая"
- deletion: "delete"

```sh
git push origin refs/heads/sync/v1.7.0:refs/heads/sync/v1.7.0     # orchestrator, on the user's direct "push"
git push origin --delete refs/heads/sync/v1.7.0                    # orchestrator, 2026-10-26T09:49:34Z..09:49:36Z, on the user's direct "delete"
```

| Path | Run id | Event | Branch | Display title | Conclusion |
|------|--------|-------|--------|---------------|------------|
| .github/workflows/tests.yaml | 40103794037 | push | sync/v1.7.0 | docs(30-03): record the dry-run readiness... | success |
| .github/workflows/pre-release.yaml | 40296330498 | workflow_dispatch | sync/v1.7.0 | Pre-release sync/v1.7.0 (dry run) | success |
| .github/workflows/release.yaml | 40296481765 | workflow_dispatch | sync/v1.7.0 | Release sync/v1.7.0 (dry run) | success |

Each `build_release` job: all named steps success, `Publish release` skipped. Annotations per job (identical except the prerelease flag):
- notice `Wrapper Gradle`: Gradle 9.8.1 from the wrapper ran createConfluentArchive and verifyConfluentArchive
- notice `Archive`: clickhouse-kafka-connect-v1.7.0_gg4.zip 12453663 bytes sha256=5a8522a8167f3c039c6033b70ed13967643cf61001765efe8a39f2d8d7a5638c
- notice `tests.yaml on this SHA`: successful tests.yaml runs on T: 1
- warning `Release tag`: tag v1.7.0_gg4 already exists (advisory on a dry run)
- notice `Draft asset attached`: dry-run-<run>-1 holds the zip, draft=true, prerelease=true (pre-release) / false (release), target=T
- notice `No tag for the draft`: refs/tags/dry-run-<run>-1 does not exist while the draft is open
- notice `Dry run cleaned up`: no draft and no tag remain
- no annotation at level failure (other warnings: ClickHouse client deprecation)

Origin comparison: tags 10 before and 10 after (identical, no dry-run tag); published releases 0 before and after; heads equal the before-snapshot (custom_build `21c51598`, main, two dependabot branches) with `sync/v1.7.0` deleted. The user also reported the Releases tab empty (drafts are invisible to the unauthenticated API).

Verifier output: `dry-run-problems 0 []` / `DRY-RUNS-PROVEN-OK`. Matches text: n/a (gate check)

Research assumptions settled: A1 (a draft creates no tag): yes, `No tag for the draft` notice on both runs. A2 (endpoint forms): the matching-refs and release reads worked as written. A4 (`gh` finds a tagless draft): yes, the draft check step passed without the REST fallback acting. A5 (non-interactive create): yes, `Create draft release (dry run)` succeeded. A6 (job permissions): sufficient, create, check and delete all succeeded.
Open: origin lists 10 tags where the plan expected 11 (unanswered); the Tooling blocker's "gh not installed" line is left unchanged pending the user.
