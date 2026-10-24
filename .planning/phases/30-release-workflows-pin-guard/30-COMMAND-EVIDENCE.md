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
