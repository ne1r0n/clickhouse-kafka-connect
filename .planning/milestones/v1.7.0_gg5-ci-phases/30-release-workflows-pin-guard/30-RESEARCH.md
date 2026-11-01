# Phase 30: Release Workflows & Pin Guard - Research

**Researched:** 2026-10-24
**Domain:** GitHub Actions release-path hardening (SHA pins, `gh release create`, dry run) plus a Docker-free JUnit guard over `.github/workflows/`
**Confidence:** MEDIUM-HIGH. Action SHAs, Node LTS dates, `gh` source behaviour and the in-repo values are verified. Two GitHub platform behaviours (a draft creates no tag; the dispatch form reads the selected ref's file) are documented only indirectly or by community sources, so the design below does not depend on them and the dry run falsifies them.

<user_constraints>
## User Constraints (from CONTEXT.md)

### Locked Decisions

**Dry-run mechanism (REL-05)**
- **D-01:** Both `release.yaml` and `pre-release.yaml` get a boolean `workflow_dispatch` input `dry_run`. When true: build, run `verifyConfluentArchive`, `gh release create --draft` with the archive attached (this exercises the attach step), then delete the draft with `gh release delete --cleanup-tag` in an `if: always()` step. Research must confirm a draft creates no tag before this is relied on.
- **D-02:** `dry_run` defaults to `false`; dispatching without touching the form publishes exactly as today.
- **D-03:** A real publish runs only on `custom_build`: the publish step is gated on `!inputs.dry_run && github.ref == 'refs/heads/custom_build'`. A scratch-branch dispatch can only dry-run.
- **D-04:** The dry-run steps live once per workflow (no duplicated job); real and dry paths differ only by the `--draft` flag and the cleanup step.

**Tag ownership and permissions**
- **D-05:** `gh release create <tag> --target $GITHUB_SHA` creates the tag. The separate `git tag` / `git push` step in `release.yaml` is deleted. `pre-release.yaml` uses the same step with `--prerelease`; `release.yaml` does not.
- **D-06:** The workflows enforce "tags reach origin only after the published SHA has a green `tests.yaml` run": before a real publish, query the Actions API for a successful `tests.yaml` run on `$GITHUB_SHA` and fail if none. Skipped on dry runs.
- **D-07:** Per-job least privilege: top-level `permissions: contents: read`; the release job escalates to `contents: write` plus `actions: read` (for D-06). Checkout uses `persist-credentials: false`.

**increment-version.yml**
- **D-08:** Pin only; do not fix the likely `semver.inc('v1.7.0_gg4', ...)` -> `vnull` defect. Record it in FORK-DELTA as known/unfixed (the fork bumps `VERSION` by hand). Before recording, run a one-line node check to confirm the defect and record its output in the phase (it was read from the grammar, not run).
- **D-09:** Replace Node 16 with a current LTS via a SHA-pinned current `setup-node` major (research picks exact versions). Drop the pointless `npm install fs`.

**Dependabot and guard scope**
- **D-10:** User signs off on editing the pinned assertion: drop `increment-version.yml`, `pre-release.yaml`, `release.yaml` from `dependabot.yml` `exclude-paths`, keep `integrationTests.yaml`, and update `UPSTREAM_WORKFLOWS` in `TestsWorkflowPinGuardUnitTest` to match. The reason is recorded in the same commit.
- **D-11:** CIG-01 guard is a NEW class under `src/test/java/com/clickhouse/kafka/connect/ci/` (e.g. `WorkflowActionPinGuardUnitTest`). It discovers `.github/workflows/*` from the directory, excludes only upstream-identical `integrationTests.yaml`, reuses the `PINNED_USES` regex shape (40-hex SHA + `# vX.Y.Z`), names file and line on failure, and carries red-on-fixture tests per unpinned form (tag, branch, short SHA, missing comment). `TestsWorkflowPinGuardUnitTest`'s tests.yaml-specific rules are left alone apart from D-10.
- **D-12:** The guard reads workflows only, not `.github/actions/*` composites (`use-clickhouse-java` is not called by any fork workflow). Record this in the guard's javadoc.

### Claude's Discretion
- Exact action versions/SHAs: reuse `tests.yaml`'s pins (checkout v7.0.1, setup-java v6.0.1 with `temurin`, setup-gradle v6.4.0 by peeled commit); research picks `setup-node` and the Node LTS.
- Exact shape of the Actions API query for D-06 and the guard's fixture layout.

### Deferred Ideas (OUT OF SCOPE)
- Fixing `increment-version.yml` for the `vX.Y.Z_ggN` scheme, or retiring it - a future phase if the fork ever wants automated bumps.
</user_constraints>

<phase_requirements>
## Phase Requirements

| ID | Description | Research Support |
|----|-------------|------------------|
| CIG-01 | A check fails when a fork-owned workflow uses an unpinned action | New `WorkflowActionPinGuardUnitTest`: directory discovery, line-numbered messages, fixture matrix, local-path/`docker://` handling (Architecture Patterns 1, Pitfalls 6-8) |
| REL-01 | `pre-release.yaml` builds with the project Gradle wrapper (via setup-gradle), no pinned Gradle 7.4.2 | setup-gradle v6.4.0 with no `gradle-version` input uses the wrapper (verified in action.yml); wrapper is Gradle 9.8.1 |
| REL-02 | The three files pin every action by full SHA with a version comment | SHA table in Standard Stack (all from `git ls-remote`, peeled where annotated) |
| REL-03 | `gradle-build-action` replaced by `setup-gradle` in release and pre-release | Code Example 1; fork-anchor rows (count 0) in Pitfall 10 |
| REL-04 | `create-release` / `upload-release-asset` replaced by `gh release create` with the archive attached | `gh` source behaviour (draft-then-publish when assets given), archive name verified from Phase 29 evidence |
| REL-05 | A dry run proves both paths build and attach the archive without publishing | Dry-run design (Architecture Pattern 2), including the refinement of D-01's `--cleanup-tag` (Pitfall 1) and the ref-derived dry mode (Pitfall 3) |
</phase_requirements>

## Project Constraints (from CLAUDE.md)

Sources: `./CLAUDE.md` (imports `AGENTS.md`) and `./.claude/CLAUDE.md`.

- **GSD workflow enforcement:** repo edits go through a GSD command; no direct edits outside a workflow.
- **Behaviour preservation / rebasability:** changes are additive and small; the fork runs in production. No connector code changes (`src/main` diff stays empty).
- **Tests:** JUnit 5 (Jupiter); "Existing tests: Only add new tests, never delete/weaken existing ones" (AGENTS.md). D-10 is the user-approved exception for the `UPSTREAM_WORKFLOWS` assertion; every fixture touched must be replaced by an equally strong one, not dropped.
- **Docker-free guards extend nothing** (no `ClickHouseBase`), so they stay in `testUnit`; files read as UTF-8 explicitly (test JVM runs `file.encoding=windows-1252`).
- **Style:** Google Java Style, Spotless (`importOrder()` + `removeUnusedImports()`); DRY; string constants extracted to top-level statics; comments only for non-obvious invariants. Project notes record 46 pre-existing Spotless violations elsewhere, so do not run a repo-wide `spotlessApply` blindly [CITED: project memory `gsd-code-review-fix-gotchas`].
- **Security:** do not log secrets; do not upgrade dependencies without asking (this phase adds none to the build).
- **Do not commit** without being asked; no PR title/description changes without asking.
- **Fork-anchors invariant:** any commit that intentionally changes an anchored line updates `.planning/scripts/fork-anchors.txt` in the same commit.

## Summary

The phase is three small workflow rewrites plus one new Docker-free guard. Everything needed is available from public sources: all action SHAs resolve with `git ls-remote`, the semver defect reproduces in one node line, and the `gh` CLI source documents the release flow. The one design problem is the dry run. Taken literally, D-01 (`gh release delete --cleanup-tag`) is unsafe on both branches of reality: if a draft creates no tag, `--cleanup-tag` issues `DELETE git/refs/tags/<tag>` on a tag that does not exist and fails the step after the release is already gone; if the dry run uses the real `VERSION` tag and that tag already exists (a re-dispatch after publication), `--cleanup-tag` would delete the published tag. The recommended refinement keeps D-01's intent (draft with archive attached, always cleaned up) but uses a throwaway tag name `dry-run-<run_id>-<attempt>` and asserts, from inside the workflow, that no tag exists while the draft is open.

D-03 as worded also has a hole: gating only the publish step on `!inputs.dry_run && ref == custom_build` leaves a scratch-branch dispatch with `dry_run=false` doing nothing at all. Make the two paths exact complements (`inputs.dry_run || github.ref != 'refs/heads/custom_build'` for the dry steps). That also removes dependence on the dispatch form showing the new input. The docs do not say whether the "Run workflow" form renders inputs from the selected branch's file or the default branch's; the run itself uses the selected ref's file (the only documented caveat is that the file must also exist on that ref).

For the guard, copy `PINNED_USES` and `USES_LINE` (make them package-private rather than duplicating), iterate lines with indexes so messages read `file:line`, and fail closed on local `./` and `docker://` forms because D-12 means composites are never scanned. Dropping three files from Dependabot's `exclude-paths` shrinks `UPSTREAM_WORKFLOWS` to one entry and breaks several existing fixtures that index into a four-element list; they must be rewritten, not deleted.

**Primary recommendation:** Implement the guard first (red on the unchanged files, recorded as evidence), then rewrite the three workflows with the pins in the Standard Stack table, use the ref-derived dry/real complement with a throwaway dry-run tag instead of `--cleanup-tag`, and land the pins, the anchor rows and the Dependabot/`UPSTREAM_WORKFLOWS` edit in one commit.

## Architectural Responsibility Map

| Capability | Primary Tier | Secondary Tier | Rationale |
|------------|-------------|----------------|-----------|
| Build and verify the Confluent archive | CI runner (Gradle wrapper via setup-gradle) | Gradle task graph (`createConfluentArchive` finalized by `verifyConfluentArchive`) | The finalizer, not the workflow, owns the archive rules; the workflow only has to call the wrapper |
| Create the tag and the GitHub release | GitHub Releases API via `gh release create` | Workflow `if:` gate (custom_build only) | D-05: the API creates the tag at publish time, so no `git push` credentials are needed |
| "Tag only after green CI" gate | Actions REST API query in the release job | Branch protection (not configured here) | D-06; needs `actions: read` |
| Dry-run proof | Same job, draft release plus in-workflow assertions | Operator before/after check (`git ls-remote --tags`, REST `/releases`) | REL-05 is proven by the run conclusion, step annotations and the operator's diff |
| Unpinned-action detection | `testUnit` (JUnit, line-level text checks) | Dependabot keeps pins current | CIG-01; no YAML library, no Docker |
| Pin freshness | Dependabot `github-actions` ecosystem | - | D-10 puts the three files under it |
| Version bump automation | `increment-version.yml` (known broken, pinned only) | Hand edit of `VERSION` | D-08 |

## Standard Stack

### Core (GitHub Actions, all pinned by full commit SHA)

Every SHA was resolved this session with `git ls-remote https://github.com/<repo>.git` against the public repository; where the tag is annotated the `^{}` (peeled) commit is used. Registry-style "legitimacy" does not apply to Actions; the authority is the upstream repository's own tag ref.

| Action | Pin line (SHA + comment) | Source of the SHA | Notes |
|--------|--------------------------|-------------------|-------|
| `actions/checkout` | `@3d3c42e5aac5ba805825da76410c181273ba90b1 # v7.0.1` | `git ls-remote`: `3d3c42e5aac5ba805825da76410c181273ba90b1 refs/tags/v7.0.1`; identical to `tests.yaml:41` | Lightweight tag. Latest v7 line at research time. |
| `actions/setup-java` | `@de7274f081f381c8f8158605e0321c36c376e2e6 # v6.0.1` | `git ls-remote`: `de7274f081f381c8f8158605e0321c36c376e2e6 refs/tags/v6.0.1`; identical to `tests.yaml:46` | `distribution: 'temurin'`, `java-version: '17'`. A newer `v6.1.0` exists (`b843ebf9b913146b38da2eaa7aeddf76074ae1ad`); reuse of v6.0.1 follows CONTEXT, and the grouped Dependabot PR will move all files together. |
| `gradle/actions/setup-gradle` | `@3f5f9adaf7d9fecd50b5935e54106014257a94e6 # v6.4.0` | `git ls-remote`: `b9bee63ef481cc545e4517ec730544a4d8b1dfa0 refs/tags/v6.4.0` (tag object) and `3f5f9adaf7d9fecd50b5935e54106014257a94e6 refs/tags/v6.4.0^{}` (peeled commit); identical to `tests.yaml:52` | Annotated tag: the peeled commit is the pin, never the tag-object SHA. `runs.using: node24`. |
| `actions/setup-node` (increment-version only) | `@949feb2413d6458794dcd2491c4babbbce0c15c1 # v7.1.0` | `git ls-remote 'refs/tags/v7*'`: `949feb2413d6458794dcd2491c4babbbce0c15c1 refs/tags/v7.1.0` (no `^{}` line, so a lightweight tag and already a commit) | Latest release v7.1.0 (8 Oct); `runs.using: node24`. `v7.0.0` is `820762786026740c76f36085b0efc47a31fe5020`. |

Node version for `setup-node`: **`node-version: '24'`**. Official schedule: v24 "Krypton" `lts: 2025-10-28`, `maintenance: 2026-10-20`, `end: 2028-04-30` (still supported, in Maintenance LTS from 4 days ago). v22 ends 2027-04-30; v26 only becomes LTS on 2026-10-28, four days after today, so it is not chosen. The Node 24 choice also matches the developer machine (`node v24.14.1`) on which the semver defect was reproduced. [CITED: raw.githubusercontent.com/nodejs/Release/main/schedule.json]

### Supporting
| Tool | Version | Purpose | When to Use |
|------|---------|---------|-------------|
| `gh` CLI | 2.102.0 preinstalled on `ubuntu-24.04` [CITED: actions/runner-images Ubuntu2404-Readme.md] | `gh release create/view/delete`, `gh api` | Release steps; needs `GH_TOKEN` in the step env |
| `semver` (npm) | 7.8.5, `engines.node >=10` [VERIFIED: npm registry] | Only used by `increment-version.yml` | Pin the version in the install line (see Package Legitimacy Audit) |
| JUnit 5 | 5.11.3 (already in build) | New guard test | No new test dependency |

### Alternatives Considered
| Instead of | Could Use | Tradeoff |
|------------|-----------|----------|
| Throwaway dry-run tag + plain `gh release delete --yes` | D-01's `--cleanup-tag` on the real tag | `--cleanup-tag` fails on a nonexistent tag and deletes a real tag that exists (Pitfall 1) |
| Two near-identical workflow files | One reusable `workflow_call` workflow | Reusable workflow cuts duplication but reshapes upstream files and breaks the one-step `run name` view; D-04 keeps one job per file |
| Line-level text guard | YAML parser | Parser needs a new test dependency (SnakeYAML is not a direct dependency); the established convention is line-level and library-free |

**Installation:** no build dependency is added. Only `increment-version.yml` installs one npm package at workflow runtime (`semver@7.8.5`).

**Version verification:** `npm view semver version` returned `7.8.5` (dist-tag `latest`), `time.modified 2026-09-09`; `npm view semver scripts.postinstall` returned nothing.

## Package Legitimacy Audit

| Package | Registry | Age | Downloads | Source Repo | Verdict | Disposition |
|---------|----------|-----|-----------|-------------|---------|-------------|
| semver | npm | multi-year (7.x current, published 2026-06-19 for 7.8.5) | ~549M/wk | github.com/npm/node-semver | OK (`gsd-tools query package-legitimacy check --ecosystem npm semver`, `postinstall: null`, `deprecated: false`) | Approved; pin `semver@7.8.5` and pass `--ignore-scripts` |

**Packages removed due to [SLOP] verdict:** none
**Packages flagged as suspicious [SUS]:** none
**Dropped:** `fs` (`npm install fs` in `increment-version.yml`; D-09). `fs` is a Node built-in; the npm package of that name is an unrelated placeholder.

GitHub Actions are not installable packages; their identity is established by resolving the tag in the owning repository (`actions/*`, `gradle/actions`), which is the pin source above.

## Architecture Patterns

### System Architecture Diagram

```
Operator (GitHub "Run workflow" UI, ref = custom_build or scratch branch)
        |  workflow_dispatch  { dry_run: bool, default false }
        v
+-----------------------------------------------------------------+
| job build_release  (runs-on ubuntu-24.04, contents:write,        |
|                     actions:read)                                 |
|                                                                   |
|  checkout (persist-credentials:false) -> setup-java -> setup-gradle
|        |                                                          |
|        v                                                          |
|  read VERSION --> ./gradlew createConfluentArchive               |
|        |             \--finalizedBy--> verifyConfluentArchive     |
|        v                                                          |
|  archive present? build/confluent/clickhouse-kafka-connect-$V.zip|
|        |                                                          |
|   mode = (inputs.dry_run || ref != custom_build) ?                |
|        |                                  |                       |
|     NO (real)                         YES (dry)                   |
|        |                                  |                       |
|  gate: tests.yaml green on SHA?     advisory: same query          |
|  gate: tag $V absent?               tag := dry-run-<run>-<attempt>|
|        | fail -> stop                     |                       |
|        v                                  v                       |
|  gh release create $V $ZIP          gh release create $TAG $ZIP   |
|    --target $GITHUB_SHA               --draft --target $GITHUB_SHA|
|    [--prerelease]                         |                       |
|        |                              view: asset name+size ok    |
|   GitHub API creates tag at           assert: no ref refs/tags/$TAG
|   publish time (draft->publish)           |                       |
|                                       if: always() cleanup:       |
|                                       gh release delete $TAG --yes|
|                                       delete tag if it exists+FAIL|
+-----------------------------------------------------------------+
        |
        v
  Operator diff: git ls-remote --tags origin / REST /releases (before == after for a dry run)
```

Guard path (Docker-free, `testUnit`):

```
.github/workflows/*.yml|*.yaml --(directory listing, minus integrationTests.yaml)--> per-file line scan
   --> skip full-comment lines, keep original line numbers
   --> each `uses:` line must match PINNED_USES, else "file:line: <text>"
   --> any message => test fails naming file and line
fixtures (in-memory lines): tag, branch, short SHA, missing comment, local ./, docker://, quoted, 39/41-hex
```

### Recommended Project Structure
```
.github/workflows/
  release.yaml            # rewritten (pins, setup-gradle, gh release create, dry_run)
  pre-release.yaml        # rewritten (same shape, --prerelease)
  increment-version.yml   # pins + Node 24 + semver pin only
  tests.yaml              # unchanged
  integrationTests.yaml   # unchanged, excluded from the guard
.github/dependabot.yml    # exclude-paths: integrationTests.yaml only
src/test/java/com/clickhouse/kafka/connect/ci/
  WorkflowActionPinGuardUnitTest.java   # NEW
  TestsWorkflowPinGuardUnitTest.java    # D-10 edit only (+ package-private regex constants)
.planning/scripts/fork-anchors.txt      # new rows, same commit as the workflow edits
docs/FORK-DELTA.md                      # increment-version defect + stale "left exactly as upstream" paragraph
```

### Pattern 1: Directory-discovered, line-numbered pin guard
**What:** list `.github/workflows`, keep `*.yml`/`*.yaml`, drop exactly `integrationTests.yaml`, read each file as UTF-8, and test every non-comment `uses:` line against the shared `PINNED_USES`. Return `Optional<String>` (project convention) but accumulate all offending lines into one message `file:line: <stripped text>`.
**When to use:** CIG-01. Phase 31's nightly passes without editing the guard.
**Floor assertion:** at least 4 files discovered (`increment-version.yml`, `pre-release.yaml`, `release.yaml`, `tests.yaml` today), so an unreadable or empty directory cannot pass; the existing guard uses the same idea (`MIN_PINNED_ACTIONS`).
**Example (shape, derived from the in-repo guard):**
```java
// Source: src/test/java/com/clickhouse/kafka/connect/ci/TestsWorkflowPinGuardUnitTest.java:43-46
// private static final Pattern USES_LINE = Pattern.compile("^\\s*(?:-\\s+)?uses:\\s.*$");
// private static final Pattern PINNED_USES = Pattern.compile(
//     "^\\s*(?:-\\s+)?uses:\\s+[\\w.-]+/[\\w./-]+@[0-9a-f]{40}\\s+#\\s+v\\d+\\.\\d+\\.\\d+\\s*$");

static Optional<String> unpinnedUsesProblem(String file, List<String> lines) {
    List<String> problems = new ArrayList<>();
    for (int i = 0; i < lines.size(); i++) {           // keep index: line number = i + 1
        String line = lines.get(i);
        if (line.stripLeading().startsWith("#") || !USES_LINE.matcher(line).matches()) {
            continue;
        }
        if (!PINNED_USES.matcher(line).matches()) {
            problems.add(file + ":" + (i + 1) + ": not a 40-hex SHA with a '# vX.Y.Z' comment: '"
                    + line.strip() + "'");
        }
    }
    return problems.isEmpty() ? Optional.empty() : Optional.of(String.join("\n", problems));
}
```
Make `USES_LINE` and `PINNED_USES` package-private in `TestsWorkflowPinGuardUnitTest` and reference them (AGENTS.md DRY) rather than copying the regexes.

### Pattern 2: Dry run as the exact complement of publish
**What:** two step groups with complementary literal conditions, no extra state:
- real: `if: ${{ !inputs.dry_run && github.ref == 'refs/heads/custom_build' }}` (D-03 verbatim)
- dry: `if: ${{ inputs.dry_run || github.ref != 'refs/heads/custom_build' }}` (De Morgan complement)

Every dispatch executes exactly one of them. A scratch-branch dispatch is therefore a dry run even if the form did not offer, or the operator did not tick, `dry_run`. The shared steps (checkout, java, gradle, build, archive check) are unconditional, which satisfies D-04.
**Dry steps:** (1) set `DRY_TAG=dry-run-${GITHUB_RUN_ID}-${GITHUB_RUN_ATTEMPT}`; (2) `gh release create "$DRY_TAG" "$ARCHIVE" --draft --target "$GITHUB_SHA" --title ... --notes ...` (add `--prerelease` in pre-release for parity); (3) `gh release view "$DRY_TAG" --json assets` and assert the asset name equals `clickhouse-kafka-connect-${RELEASE_VERSION}.zip` with size > 0; (4) `gh api repos/$GITHUB_REPOSITORY/git/matching-refs/tags/$DRY_TAG --jq length` must print `0` (the in-workflow proof that a draft creates no tag); (5) cleanup under `if: ${{ always() && (inputs.dry_run || github.ref != 'refs/heads/custom_build') }}`.
**Evidence channel:** job logs are not readable without repository admin rights (project convention, `tests.yaml` header). Emit `::notice title=...::` annotations and `$GITHUB_STEP_SUMMARY` lines (archive name, size, sha256, "asset attached", "tag absent") so the unauthenticated REST API (`/check-runs/.../annotations`, `/actions/runs/{id}/jobs`) can show them. Use distinct step names, because per-step conclusions are what the REST jobs endpoint exposes.

### Pattern 3: Real publish with the gates in front
1. `tests.yaml` green on `$GITHUB_SHA`: `gh api "repos/${GITHUB_REPOSITORY}/actions/workflows/tests.yaml/runs?head_sha=${GITHUB_SHA}&status=success&per_page=1" --jq '.total_count'` must be >= 1. The endpoint accepts `head_sha` and a `status` of a conclusion value such as `success`. [CITED: docs.github.com/en/rest/actions/workflow-runs#list-workflow-runs-for-a-workflow]
2. Tag `$RELEASE_VERSION` must not already exist (query `git/matching-refs/tags/...` and filter on exact `.ref`). The API's `target_commitish` is "Unused if the Git tag already exists" [CITED: docs.github.com/en/rest/releases/releases#create-a-release], so without this check a re-run would attach a new build to a tag at a different commit.
3. `gh release create "$RELEASE_VERSION" "$ARCHIVE" --target "$GITHUB_SHA" --title "$RELEASE_VERSION" --notes ""` (plus `--prerelease` for pre-release only). With an asset argument and no `--draft`, `gh` itself creates a draft, uploads, then publishes, and deletes the draft on upload failure; the tag appears at publish time, so a failed upload leaves no tag. [CITED: cli/cli pkg/cmd/release/create/create.go via WebFetch summary]

### Anti-Patterns to Avoid
- **`--cleanup-tag` against a tag name that can collide with a real release tag** (Pitfall 1).
- **Tag-ref pins or branch pins in any `uses:`**, including the "easy" ones like `gradle/actions/setup-gradle@v6` that `release.yaml` carries today.
- **`git tag` + `git push` in the workflow** after `gh release create` (D-05 deletes it; with `persist-credentials: false` it would also fail).
- **Job-level `env: GH_TOKEN`** for the whole job; set `GH_TOKEN` only on the steps that call `gh`.
- **Interpolating `${{ ... }}` into `run:` bodies** for anything not a constant; pass through `env:` (Security Domain).

## Don't Hand-Roll

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| Creating release + tag + asset upload | `curl` against the REST API, or `git tag` + `git push` | `gh release create <tag> <file> --target <sha>` | Handles the draft-upload-publish sequence and cleans up the draft on failure |
| Deleting the draft | Hand-computed release id, REST `DELETE /releases/{id}` | `gh release delete <tag> --yes` | `gh` resolves drafts by tag (GraphQL lookup of a draft by `tagName`) |
| Peeled commit of an annotated tag | Trusting the `refs/tags/vX` line | `git ls-remote ... 'refs/tags/vX' 'refs/tags/vX^{}'` | The tag-object SHA is not a commit; `setup-gradle` v6.4.0 is annotated |
| Gradle archive verification | A shell `unzip` check in the workflow | The existing `verifyConfluentArchive` finalizer | One rule set, already unit-tested by `ConfluentArchiveCheckUnitTest` |
| YAML parsing in the guard | SnakeYAML dependency | Line-level `Pattern` checks as in `TestsWorkflowPinGuardUnitTest` | Established convention; avoids a new test dependency |
| Pin freshness | A script that bumps SHAs | Dependabot `github-actions` ecosystem (already configured, grouped, monthly) | Moves SHA and `# vX.Y.Z` comment together |

**Key insight:** the release path's risk is not in building, it is in publishing something wrong or unrecoverable (a stray tag). Every hand-rolled shell tag operation adds a way to do that; the API plus two cheap assertions removes them.

## Runtime State Inventory

Not applicable. This phase changes workflow definitions and tests; it renames no identifier that is stored elsewhere. Two GitHub-side facts for the planner:
- **Workflow registry:** both release files are named `Release`; GitHub keys workflow history by file path, not by `name:`, so renaming `pre-release.yaml`'s `name:` to `Pre-release` loses no history. Nothing in the repo references these display names (they are not in `fork-anchors.txt`).
- **Live repo state read this session** (unauthenticated REST, `GET /repos/ne1r0n/clickhouse-kafka-connect`): `default_branch=custom_build`, `private=False`, `fork=True`. `GET .../actions/workflows` lists `increment-version.yml`, `pre-release.yaml`, `release.yaml`, `tests.yaml` as `active` and `integrationTests.yaml` as `disabled_manually`. `git ls-remote --symref origin HEAD` returns `ref: refs/heads/custom_build	HEAD` at `21c51598b06b6f5af58430765d57b1e5f6747c5a`. (FORK-DELTA's statement that the default branch is `main` is stale.)

## Common Pitfalls

### Pitfall 1: `gh release delete --cleanup-tag` is unsafe in both directions (conflicts with D-01 wording)
**What goes wrong:** (a) If the dry run used the real `VERSION` tag and a published release or tag with that name exists (a re-dispatch after publication), `FetchRelease` takes "the first successful result" of a published-release lookup and a draft lookup, so the delete may hit the published release, and `--cleanup-tag` then runs `DELETE repos/{owner}/{repo}/git/refs/tags/{tag}` and removes the real tag. (b) If a draft creates no tag, that `DELETE` targets a missing ref, `deleteTag` returns the error, and the step fails after the release is already deleted. [CITED: cli/cli `pkg/cmd/release/delete/delete.go`, `shared/fetch.go` via WebFetch]
**Why it happens:** `--cleanup-tag` assumes the tag was created by the release being deleted.
**How to avoid:** use a throwaway name that cannot be a real release tag (`dry-run-${GITHUB_RUN_ID}-${GITHUB_RUN_ATTEMPT}`), delete with `gh release delete "$DRY_TAG" --yes`, then check the ref explicitly and delete it only if it exists (and fail the run, because that means a draft did create a tag and the premise of D-01 is false). The asset keeps its real name because `gh` uploads the file by basename.
**Warning signs:** a dry run that ends red at the delete step; a release list that changes on the real tag.
**Planner note:** this refines a locked decision; surface it for confirmation (Open Question 1). It preserves D-01's intent (draft + asset + guaranteed cleanup).

### Pitfall 2: "A draft creates no tag" is not documented by GitHub
**What goes wrong:** the CONTEXT asks research to confirm it. GitHub's REST docs only say `target_commitish` "determines where the Git tag is created from" and is "Unused if the Git tag already exists"; the Managing releases page does not say when a new tag is created. Community write-ups state that a draft has no git tag until published (gitify PR; github.community thread "Creation of draft release should be possible without tag name"), but those are secondary. I could not test it: dispatching or creating a draft mutates the user's repository, which the ROADMAP makes operator-only.
**How to avoid:** make the workflow its own falsification (Pattern 2 step 4) and keep the operator's before/after `git ls-remote --tags` as the second check. Tag the claim `[ASSUMED]` (A1) until the first dry run reports `0`.

### Pitfall 3: The dispatch form may not show `dry_run` before the files reach the default branch
**What goes wrong:** the docs say "To trigger the `workflow_dispatch` event, your workflow must be in the default branch" and that the dispatch can target another branch whose copy of the file exists; they do not say whether the UI form is rendered from the selected branch's file or from the default branch's (which, until publication, has no `dry_run` input). Likewise the REST create-dispatch page does not document how unknown inputs are treated.
**How to avoid:** the complement design in Pattern 2 makes a scratch-branch dispatch a dry run regardless of the input, so the operator does not depend on seeing the checkbox. State this in the operator instructions: "dispatch on the scratch branch; do not rely on the checkbox". The documented fact that the run reads the selected ref's file is what lets the new steps run at all. [CITED: docs.github.com manually-running-a-workflow; community discussion #25746 via search summary]

### Pitfall 4: Job-level `permissions:` replaces the top-level block
**What goes wrong:** a job that declares `permissions:` gets exactly those scopes and nothing else. D-07's escalation must list `contents: write` and `actions: read` explicitly; `contents: write` alone would make the D-06 query 403 on a private repo and leaves every other scope `none`.
**How to avoid:** top-level `permissions: contents: read`; job-level `contents: write` + `actions: read`. The `tests.yaml` guard rules (single `permissions:` line, no `write`) apply to `tests.yaml` only and must not be pointed at the release files.

### Pitfall 5: `increment-version.yml` needs credentials to push
**What goes wrong:** applying `persist-credentials: false` to its checkout makes `git push` (step "Commit and push") fail with no credentials. This job's whole purpose is to push a branch and open a PR.
**How to avoid:** either leave the checkout's credential persistence on for this one job, with a comment saying why (recommended; the file is known-broken and unused), or authenticate the push explicitly. If D-07 is read as covering this file, say so in the plan and take the explicit route. Its permissions are `contents: write` and `pull-requests: write` at job level, top level `contents: read`.
**Related:** the workflow also depends on the repository setting that allows Actions to create pull requests; not verified and not in scope.

### Pitfall 6: The guard silently skips what it cannot parse
**What goes wrong:** a guard that only checks `@`-pinned remote actions misses `uses: ./.github/actions/x` (local), `uses: docker://img:tag`, and quoted values. D-12 declares composites unscanned, so a local `uses:` would let unpinned actions inside the composite pass unseen.
**How to avoid:** treat every `uses:` line that is not `PINNED_USES` as a violation (fail closed), including `./` and `docker://`, with a message that says local actions are not scanned. Fixtures for each. (Recommendation; the CONTEXT lists only four forms. See Open Question 3.)

### Pitfall 7: Fixture index breakage when `UPSTREAM_WORKFLOWS` shrinks to one entry
**What goes wrong:** `TestsWorkflowPinGuardUnitTest` builds fixtures from a four-element `UPSTREAM`: `UPSTREAM.subList(0, 3)` (line 1062), `UPSTREAM.get(0)`..`get(3)` (lines 1068-1071, 1355-1356), `reversed` order (1355-1356) and `assertSame(UPSTREAM_WORKFLOWS, UPSTREAM, ...)` (1393). With one entry these throw `IndexOutOfBoundsException` or become vacuous (a one-element list cannot test ordering). The `@DisplayName`s say "four upstream workflows" (lines 790, 1047).
**How to avoid:** rewrite each fixture to an equally strong form (missing path -> empty `exclude-paths` and a list holding only `tests.yaml`; misspelt path -> `.github/workflows/integrationTest.yaml`; stale path -> `plus(UPSTREAM, ".github/workflows/gone.yml")`; ordering -> use a two-entry list built from `UPSTREAM` plus a non-required existing path only if the rule still allows it). Consider tightening the rule so `exclude-paths` must equal `UPSTREAM_WORKFLOWS` exactly, which pins D-10's intent (re-adding `release.yaml` to the exclude list fails); then the order fixture is dropped as vacuous and replaced by an equally meaningful one (for example list items at the key's indent, already covered, plus a trailing-comment item). Update both `@DisplayName`s and the class javadoc. AGENTS.md forbids weakening; record each replacement in the commit message.
**Also:** `EXISTING_WORKFLOWS = Set.copyOf(plus(UPSTREAM, ".github/workflows/tests.yaml"))` (line 698) now lacks the three release files; add them if any fixture lists them.

### Pitfall 8: `testUnit` can be UP-TO-DATE after a workflow-only edit
**What goes wrong:** the workflow files are not declared inputs of the test task (`build.gradle.kts:227` declares only `build.gradle.kts`), so a run after editing only YAML can be skipped as UP-TO-DATE, which is how "red on unchanged files, green after pins" evidence goes wrong.
**How to avoid:** use the project's cache-busted form for every red/green observation and for the phase gate (`cleanTestUnit testUnit` or `--rerun`). Do not add `inputs.dir` to `build.gradle.kts` (rebase surface); note it in the plan.

### Pitfall 9: Floating runner label
**What goes wrong:** `release.yaml` and `pre-release.yaml` use `runs-on: ubuntu-latest`; `tests.yaml` pins `ubuntu-24.04` because the label moves to Ubuntu 26 from 2026-10-19 [CITED: docs/FORK-DELTA.md tests.yaml row; tests.yaml header comment CI-12].
**How to avoid:** pin `ubuntu-24.04` in all three files and add `timeout-minutes`. The pin guard does not check runners (out of CIG-01), so this is a plan-level instruction. `gh` 2.102.0 is on that image.

### Pitfall 10: Fork-anchor rows must land in the same commit
**What goes wrong:** `.planning/scripts/fork-anchors.txt` rows are `path<TAB>fixed-string<TAB>count<TAB>backs-ID`, evaluated with `grep -cF` (count of matching lines) at every commit containing the file. Adding a count-0 row that the same commit's workflow still violates, or editing the workflow without the row, is an `ANCHOR VIOLATION`.
**How to avoid:** add rows in the commit that rewrites the workflows, and keep comments in the workflows free of the anchored strings (a comment line counts). Suggested rows (counts measured after the final write with `grep -cF -e <pattern> <file>`):
- `.github/workflows/pre-release.yaml	gradle-version	0	REL-01`
- `.github/workflows/pre-release.yaml	gradle-build-action	0	REL-03`
- `.github/workflows/release.yaml	gradle-build-action	0	REL-03`
- `.github/workflows/release.yaml	actions/create-release@	0	REL-04` and same for `actions/upload-release-asset@` in both release files
- `.github/workflows/release.yaml	git push origin	0	REL-04`
- a positive row `gh release create` with its measured count in both files
The anchors gate is `bash .planning/scripts/verify-merge-invariants.sh 54e63648 HEAD` (default environment) followed by `verify-merge-invariants-selftest.sh` [CITED: `.planning/STATE.md:71`, Phase 29 plans]; it takes about 15 minutes, so run it once per wave, in the background.

### Pitfall 11: A real publish must not run twice or race
**What goes wrong:** two concurrent dispatches could both pass the tag-absent check. 
**How to avoid:** `concurrency: { group: release-publish, cancel-in-progress: false }` shared by both release files (same group name), optional but cheap.

## Code Examples

All snippets are design patterns assembled from the verified sources above; the strings that matter (action SHAs, archive name, YAML keys) are verified, the shell wording is `[ASSUMED]` until the dry run executes it (A2).

### 1. `release.yaml` skeleton (pre-release differs by `name`, `--prerelease`, and its `run-name`)
```yaml
name: Release
run-name: Release ${{ github.ref_name }} (dry_run=${{ inputs.dry_run }})

on:
  workflow_dispatch:
    inputs:
      dry_run:
        description: 'Build and attach the archive to a throwaway draft, then delete it. No release, no tag.'
        type: boolean
        default: false

permissions:
  contents: read

concurrency:
  group: release-publish
  cancel-in-progress: false

jobs:
  build_release:
    name: build_release
    runs-on: ubuntu-24.04
    timeout-minutes: 30
    permissions:
      contents: write
      actions: read
    steps:
      - name: Checkout code
        uses: actions/checkout@3d3c42e5aac5ba805825da76410c181273ba90b1 # v7.0.1
        with:
          persist-credentials: false

      - name: Set up JDK 17
        uses: actions/setup-java@de7274f081f381c8f8158605e0321c36c376e2e6 # v6.0.1
        with:
          java-version: '17'
          distribution: 'temurin'

      - name: Set up Gradle
        uses: gradle/actions/setup-gradle@3f5f9adaf7d9fecd50b5935e54106014257a94e6 # v6.4.0

      - name: Read version
        run: |
          version="$(tr -d '[:space:]' < VERSION)"
          test -n "$version"
          echo "RELEASE_VERSION=$version" >> "$GITHUB_ENV"
          echo "ARCHIVE=build/confluent/clickhouse-kafka-connect-${version}.zip" >> "$GITHUB_ENV"

      - name: Build and verify Confluent archive
        run: ./gradlew createConfluentArchive   # verifyConfluentArchive runs as the finalizer

      - name: Check archive exists
        run: |
          test -s "$ARCHIVE"
          echo "### Archive" >> "$GITHUB_STEP_SUMMARY"
          echo "$(basename "$ARCHIVE") $(stat -c %s "$ARCHIVE") bytes sha256=$(sha256sum "$ARCHIVE" | cut -d' ' -f1)" >> "$GITHUB_STEP_SUMMARY"

      # ... real path: if: ${{ !inputs.dry_run && github.ref == 'refs/heads/custom_build' }}
      #       gate: tests.yaml green on $GITHUB_SHA; gate: tag absent; gh release create
      # ... dry path:  if: ${{ inputs.dry_run || github.ref != 'refs/heads/custom_build' }}
      #       draft with throwaway tag; assert asset; assert no tag; cleanup under always()
```
`ARCHIVE` path: Gradle names the Zip `<appendix>-<version>.zip` with an empty base name; the archive of `v1.7.0_gg4` was `clickhouse-kafka-connect-v1.7.0_gg4.zip` [VERIFIED: `.planning/milestones/v1.7.0_gg4-hardening-phases/29-release-publication/29-COMMAND-EVIDENCE.md:96`: `clickhouse-kafka-connect-v1.7.0_gg4.zip   (B, gg4 tip)`; `build.gradle.kts:449` `archiveAppendix.set(archiveFilename)`, `build.gradle.kts:452` `finalizedBy("verifyConfluentArchive")`, `build.gradle.kts:48` `version = file("VERSION").readText().trim()`].

### 2. Real publish step bodies
```yaml
      - name: Require a green tests.yaml run on this SHA
        if: ${{ !inputs.dry_run && github.ref == 'refs/heads/custom_build' }}
        env:
          GH_TOKEN: ${{ github.token }}
        run: |
          count="$(gh api "repos/${GITHUB_REPOSITORY}/actions/workflows/tests.yaml/runs?head_sha=${GITHUB_SHA}&status=success&per_page=1" --jq '.total_count')"
          if [ "${count:-0}" -lt 1 ]; then
            echo "::error::no successful tests.yaml run on ${GITHUB_SHA}; refusing to create tag ${RELEASE_VERSION}"
            exit 1
          fi

      - name: Publish release
        if: ${{ !inputs.dry_run && github.ref == 'refs/heads/custom_build' }}
        env:
          GH_TOKEN: ${{ github.token }}
          GH_REPO: ${{ github.repository }}
        run: |
          gh release create "$RELEASE_VERSION" "$ARCHIVE" \
            --target "$GITHUB_SHA" --title "$RELEASE_VERSION" --notes ""   # pre-release.yaml adds --prerelease
```

### 3. Dry-run cleanup (always)
```yaml
      - name: Dry run cleanup
        if: ${{ always() && (inputs.dry_run || github.ref != 'refs/heads/custom_build') }}
        env:
          GH_TOKEN: ${{ github.token }}
          GH_REPO: ${{ github.repository }}
        run: |
          tag="dry-run-${GITHUB_RUN_ID}-${GITHUB_RUN_ATTEMPT}"
          gh release delete "$tag" --yes || echo "::notice::no draft ${tag} to delete"
          leftover="$(gh api "repos/${GITHUB_REPOSITORY}/git/matching-refs/tags/${tag}" --jq 'length')"
          if [ "$leftover" != "0" ]; then
            gh api -X DELETE "repos/${GITHUB_REPOSITORY}/git/refs/tags/${tag}"
            echo "::error::a draft release created tag ${tag}"
            exit 1
          fi
```

### 4. `increment-version.yml` changes (pin only)
```yaml
    - uses: actions/checkout@3d3c42e5aac5ba805825da76410c181273ba90b1 # v7.0.1
      with:
        fetch-depth: 0 # this is required to see all branches   (credentials stay persisted: the job pushes)
    - uses: actions/setup-node@949feb2413d6458794dcd2491c4babbbce0c15c1 # v7.1.0
      with:
        node-version: '24'
        package-manager-cache: false
    - run: npm install --no-save --ignore-scripts semver@7.8.5   # `fs` dropped
```

### 5. D-08 evidence (command run this session, node v24.14.1, semver 7.8.5, in `/tmp/p30-semver`)
```
$ node -p "require('semver').inc('v1.7.0_gg4', 'patch')"   -> null
$ node -p "require('semver').inc('v1.7.0_gg4', 'minor')"   -> null
$ node -p "require('semver').inc('v1.7.0_gg4', 'major')"   -> null
```
`NEW_VERSION=$(node -p ...)` therefore becomes the string `null`, and `echo "v$NEW_VERSION" > VERSION` writes `vnull`. Defect confirmed, not fixed (D-08).

## State of the Art

| Old Approach | Current Approach | When Changed | Impact |
|--------------|------------------|--------------|--------|
| `actions/create-release@v1` + `actions/upload-release-asset@v1` | `gh release create <tag> <asset>` | both actions are archived and run on Node 12/16-era runtimes | REL-04 |
| `gradle/gradle-build-action@v2` with `gradle-version: '7.4.2'` | `gradle/actions/setup-gradle` (no version, use the wrapper) | gradle-build-action superseded by gradle/actions | REL-01/03; wrapper is Gradle 9.8.1 [VERIFIED: `gradle/wrapper/gradle-wrapper.properties` `distributionUrl=https\://services.gradle.org/distributions/gradle-9.8.1-bin.zip`] |
| `actions/*@v3`, `distribution: 'adopt'`, Node 16 | v7 / v6 SHA pins, `temurin`, Node 24 | Node 16 and 20 are EOL (v20 ended 2026-03-24) | REL-02 |
| `ubuntu-latest` | `ubuntu-24.04` | label moves to Ubuntu 26 from 2026-10-19 | repo convention CI-12 |

**Deprecated/outdated:** `adopt` (Temurin successor), Node 16, `actions/create-release`, `actions/upload-release-asset`, `gradle-build-action`.

## Assumptions Log

| # | Claim | Section | Risk if Wrong |
|---|-------|---------|---------------|
| A1 | A draft release created by the API creates no git tag until published (community sources only; GitHub docs are silent) | Pitfall 2, Pattern 2 | Dry run leaves a tag. Mitigated: the workflow asserts the ref is absent and deletes+fails if present; the operator's `git ls-remote --tags` diff is the second check |
| A2 | The exact shell bodies and `gh api` endpoint forms in Code Examples (`git/matching-refs/tags/...`, `actions/workflows/tests.yaml/runs?head_sha=&status=success`) behave as written | Code Examples | A step fails on the first dry run; fix is a `.github/` edit in this phase, which is why the dry run exists |
| A3 | The Actions "Run workflow" form on a non-default branch renders inputs from the selected ref's file | Pitfall 3 | The checkbox is invisible; harmless because the ref-derived dry mode does not need it |
| A4 | `gh release delete <tag>` finds a draft whose tag does not exist yet (the GraphQL `release(tagName:)` lookup is documented in `gh` source as supporting drafts, but not exercised here) | Pitfall 1 | Cleanup finds nothing; the cleanup step then leaves an orphan draft. Fallback: look it up in `GET /releases` (which lists drafts to push-access tokens) and delete by id |
| A5 | `gh release create` with `--notes ""` and no TTY creates the release without prompting | Code Example 2 | First real or dry run hangs or fails; dry run exposes it |
| A6 | A job-level `permissions:` block with `contents: write` and `actions: read` is honoured despite the repository's default workflow-permission setting | Pitfall 4 | `gh release create` returns 403; seen in the dry run (draft creation needs `contents: write`) |
| A7 | The repository setting "allow GitHub Actions to create pull requests" is irrelevant to this phase (increment-version is only pinned) | Pitfall 5 | None for Phase 30 scope |

## Open Questions

1. **D-01 wording: `--cleanup-tag`.**
   - What we know: `--cleanup-tag` errors on a missing tag and deletes a real one if the name collides (Pitfall 1).
   - What's unclear: whether the user wants the literal D-01 flag or the refinement.
   - Recommendation: adopt the throwaway-tag refinement, state the deviation in PLAN.md, and have the user confirm it at plan review. The behavioural contract (draft with asset, `always()` cleanup, no release, no tag) is unchanged.

2. **`increment-version.yml` credentials.**
   - What we know: `git push` needs credentials; `persist-credentials: false` removes them.
   - What's unclear: whether D-07's checkout rule is meant to cover this file.
   - Recommendation: keep persisted credentials in this one job, commented; it is the only workflow whose job is to push.

3. **Guard strictness for `./` and `docker://` `uses:`.**
   - What we know: D-12 leaves composites unscanned; CONTEXT lists four unpinned forms.
   - Recommendation: fail closed on both extra forms (Pitfall 6). It costs nothing today (no workflow uses them) and closes the hole D-12 would otherwise open.

4. **Run the D-06 query on dry runs too (advisory)?**
   - D-06 says skipped on dry runs, so the query's first execution would be the real publish. Running the same `gh api` call on dry runs, reporting the count as a notice but failing only on real publish, exercises the code path (and the `actions: read` grant) before it matters. Recommendation: do it, as a refinement the user can veto.

5. **Phase 32 tag flow.** `gh release create` refuses to proceed (by this design) if the tag exists. If Phase 32 chooses hand-pushed tags (Phase 29 recipe) instead of a real `release.yaml` dispatch, the tag-absent gate must not be hit; decide in Phase 32 planning which path publishes.

6. **FORK-DELTA scope in this phase.** The paragraph "`release.yaml`, `pre-release.yaml`, `increment-version.yml` ... intentionally left exactly as upstream ships them" (FORK-DELTA.md line 74) and the Dependabot row ("leaving out the four upstream workflows") become false when this phase lands. D-08 already requires an FORK-DELTA entry for the semver defect. Recommendation: correct those two statements and add the defect in this phase's commit; leave the CHANGELOG, `VERSION` and rollout note to Phase 32 (CIG-02).

7. **`gh` and a token do exist locally.** `gh auth status` reports a keyring login for `ne1r0n`, contradicting D-11 and STATE ("deliberately not installed"). Nothing in this research used the token for writes, and dispatch stays operator-only; the planner should still not script any `gh` write or dispatch. Worth the user reconciling D-11.

## Environment Availability

| Dependency | Required By | Available | Version | Fallback |
|------------|------------|-----------|---------|----------|
| Docker | not needed (guard is Docker-free; dry run builds no ClickHouse) | n/a | - | - |
| JDK 17+ and Gradle wrapper | `testUnit`, `./gradlew createConfluentArchive` locally | yes | local `openjdk 21.0.12.1` (Homebrew; the build forces a JDK 17 toolchain for tests, `-PjavaVersion=21` is the recorded escape) | - |
| Node + npm | D-08 one-liner | yes | node v24.14.1, semver 7.8.5 | - |
| git | `ls-remote` SHA resolution, anchors gate | yes | - | - |
| `gh` CLI | none by the agent | present and logged in (`ne1r0n`) | - | do not use for writes |
| Network to github.com / raw.githubusercontent.com / docs.github.com | SHA and docs verification | yes | - | - |
| GitHub Actions runner `ubuntu-24.04` | dry run (operator) | hosted | gh 2.102.0, JDK 17.0.20 default, Git 2.55.0 | - |
| Scratch branch push + operator dispatch | REL-05 | operator action | - | Phase 30 cannot self-verify REL-05; this is a human checkpoint |

**Missing dependencies with no fallback:** none for planning. REL-05's final proof is a human step (push approval and two dispatches).

## Validation Architecture

Skipped: `workflow.nyquist_validation` is `false` in `.planning/config.json`. For the planner's convenience, the verification surface is:

| Requirement | Check | Command / Evidence |
|-------------|-------|--------------------|
| CIG-01 | New guard green; red on fixtures; red on the unchanged release files before pinning | cache-busted `testUnit` (`./gradlew cleanTestUnit testUnit --tests '*WorkflowActionPinGuardUnitTest'` and the full class set) |
| REL-01/02/03/04 | Text pins in the guard plus anchor rows (count 0 for `gradle-version`, `gradle-build-action`, `actions/create-release@`, `actions/upload-release-asset@`) | guard + `verify-merge-invariants.sh 54e63648 HEAD` |
| REL-05 | Two green operator dry runs on the scratch branch; `git ls-remote --tags origin` and REST `/releases` identical before and after | unauthenticated REST for the run, jobs and annotations (60 req/hr) |
| No connector change | `git diff --stat <phase-base> HEAD -- src/main` empty | phase gate |

## Security Domain

`security_enforcement` is enabled (absent or true), ASVS level 1.

### Applicable ASVS Categories

| ASVS Category | Applies | Standard Control |
|---------------|---------|-----------------|
| V2 Authentication | no | no user auth in scope |
| V3 Session Management | no | - |
| V4 Access Control | yes | least-privilege `permissions:` per job; real publish gated to `refs/heads/custom_build`; `persist-credentials: false` on checkout (except the pushing job) |
| V5 Input Validation | yes | boolean `dry_run` only; `increment-version`'s `choice` input and derived values go through `env:`, not inline `${{ }}` in shell |
| V6 Cryptography | no | none hand-rolled; SHA-256 only for evidence output |
| V10 Malicious Code / V14 Config (supply chain) | yes | full-SHA action pins with Dependabot, the CIG-01 guard, `npm install --ignore-scripts` with an exact version, no third-party release actions (`gh` replaces two) |

### Known Threat Patterns for GitHub Actions release workflows

| Pattern | STRIDE | Standard Mitigation |
|---------|--------|---------------------|
| Mutable action tag swapped upstream (tag poisoning) | Tampering | Pin by 40-hex commit with a version comment; guard fails on anything else |
| Over-broad `GITHUB_TOKEN` | Elevation of privilege | Top-level `contents: read`; job-level `contents: write` + `actions: read` only where needed |
| Credential left in `.git/config` | Information disclosure | `persist-credentials: false` (release files); exception documented for the pushing job |
| Script injection via `${{ inputs.* }}` / `${{ env.* }}` in `run:` | Tampering | Pass through `env:`; `inputs.version` in increment-version is a `choice`, but still prefer env indirection (optional hardening) |
| Publishing a tag for a commit that never passed CI | Repudiation / Tampering | D-06 `tests.yaml` success query on `$GITHUB_SHA`; tag-absent precheck |
| Stray tag or release from a test run | Tampering | Dry run uses a throwaway tag, asserts absence, cleans in `always()`; real publish only from `custom_build` |
| Build cache poisoning of a release artifact | Tampering | `setup-gradle` writes cache only on the default branch by default (`cache-read-only` defaults to "not the default branch"); optionally set `cache-disabled: true` on the release job (low cost, rare runs) [CITED: gradle/actions setup-gradle action.yml at `3f5f9ad`] |
| Dependency confusion on `npm install` | Tampering | Exact `semver@7.8.5`, `--ignore-scripts`, no `fs` placeholder package |

## Sources

### Primary (HIGH confidence)
- `git ls-remote` against `github.com/actions/{checkout,setup-java,setup-node}` and `github.com/gradle/actions` (SHAs and peeled commits, this session)
- `nodejs/Release` `schedule.json` (Node 22, 24, 26 dates)
- In-repo files read this session: `.github/workflows/{release,pre-release,increment-version,tests}.yaml|yml`, `.github/dependabot.yml`, `TestsWorkflowPinGuardUnitTest.java` (full), `build.gradle.kts` (lines 205-227, 420-470), `docs/FORK-DELTA.md` (lines 60-90), `.planning/scripts/{fork-anchors.txt,fork-anchor-exemptions.txt,verify-merge-invariants.sh}`, `.planning/ROADMAP.md` Phase 30, `.planning/STATE.md`, `29-COMMAND-EVIDENCE.md:96`
- Local runs: `node` + `semver@7.8.5` (D-08), `npm view semver`, `gsd-tools query package-legitimacy check`, REST `GET /repos/ne1r0n/clickhouse-kafka-connect` and `/actions/workflows`, `git ls-remote --symref origin HEAD`

### Secondary (MEDIUM confidence)
- `raw.githubusercontent.com/gradle/actions/3f5f9ad.../setup-gradle/action.yml` (inputs and defaults, `node24`)
- `raw.githubusercontent.com/actions/setup-node/949feb2.../action.yml` (`node24`) and the setup-node releases page (v7.1.0)
- `actions/runner-images` Ubuntu2404 README (gh 2.102.0, Git 2.55.0, Java 17.0.20)
- docs.github.com: workflow runs REST (`head_sha`, `status`), create-a-release REST (`target_commitish`), manually-running-a-workflow, events-that-trigger-workflows (`workflow_dispatch`)
- `cli/cli` source for `release create`, `release delete`, `shared/fetch.go` (read through a summarising fetch tool, not line-by-line)

### Tertiary (LOW confidence)
- Community statements that a draft release has no git tag until published (github.community thread "Creation of draft release should be possible without tag name"; gitify PR 3108). Not GitHub documentation.
- Community discussion on running a `workflow_dispatch` workflow against a non-default branch (github.com/orgs/community/discussions/25746). It confirms the file must also exist on that branch; does not settle the input form.

## Metadata

**Confidence breakdown:**
- Standard stack (SHAs, Node 24, archive name, wrapper): HIGH, all read or resolved this session
- Architecture and guard design: HIGH, modelled on the existing in-repo guard
- Dry-run mechanics: MEDIUM, because the "draft creates no tag" and "form reads selected ref" platform behaviours are undocumented; the design is built not to depend on them
- Pitfalls: HIGH for the repo-specific ones (fixture indexes, anchors, UP-TO-DATE, permissions), MEDIUM for `gh` edge behaviour

**Research date:** 2026-10-24
**Valid until:** 2026-11-07 for action SHAs and Node dates (Dependabot can move pins monthly; Node 26 becomes LTS on 2026-10-28); the in-repo findings hold until the next edit of the cited files.
