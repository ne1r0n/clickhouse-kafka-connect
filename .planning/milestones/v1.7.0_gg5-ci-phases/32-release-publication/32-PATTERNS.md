# Phase 32: Release & Publication - Pattern Map

**Mapped:** 2026-10-26
**Files analyzed:** 12 (5 edited release files, 1 new operator note, 6 new phase-dir records)
**Analogs found:** 12 / 12

No RESEARCH.md. All analogs are git-tracked (checked `git ls-files` for the five repo files). Phase 29 records live at `.planning/milestones/v1.7.0_gg4-hardening-phases/29-release-publication/` (abbreviated `P29/` below). This phase edits no `src/`, test, `build.gradle.kts`, `gradle/` or `.github/` file. Measured at mapping time: `git diff --quiet v1.7.0_gg4 HEAD -- src/main` exits 0.

## File Classification

| New/Modified File | Role | Data Flow | Closest Analog | Match Quality |
|---|---|---|---|---|
| `VERSION` | config | transform (feeds `Version.ARTIFACT_VERSION`) | Phase 29 commit `112fbdb5` (gg3 to gg4 bump) | exact |
| `.planning/scripts/fork-anchors.txt` (line 41 `VERSION` row) | config | batch (anchor mirror) | same commit `112fbdb5`; P29 E-01 | exact |
| `CHANGELOG.md` (new `# 1.7.0_gg5` section on top) | doc | transform | `# 1.7.0_gg4, 2026-10-23` section, `CHANGELOG.md` lines 1-122 | exact |
| `docs/FORK-DELTA.md` | doc | transform | existing "Build, dependencies and CI" table (lines 58-74) and "The raw numbers" (117-152) | exact |
| `docs/ROLLOUT-v1.7.0_gg5.md` | doc | request-response (operator reads, runs checks) | `docs/ROLLOUT-v1.7.0_gg4.md` items 5 and 7 (style only) | role-match (gg5 is shorter) |
| `32-COMMAND-EVIDENCE.md` | record | event-driven (gate log) | `P29/29-COMMAND-EVIDENCE.md`; `.planning/phases/31-*/31-COMMAND-EVIDENCE.md` | exact |
| `32-REVIEW.md` | record | request-response | `.planning/phases/31-*/31-REVIEW.md` | exact |
| `32-REVIEW-DISPOSITION.md` | record | CRUD | `.planning/phases/31-*/31-REVIEW-DISPOSITION.md` | exact |
| `32-NN-PLAN.md` / `32-NN-SUMMARY.md` | plan | batch | `P29/29-06..29-08-PLAN.md`, Phase 30/31 plans | role-match |
| `32-VERIFICATION.md`, `32-SECURITY.md` | record | request-response | `.planning/phases/31-*/31-VERIFICATION.md`, `31-SECURITY.md` | exact |
| Merge message (`/tmp/gsd32/merge-msg.txt`, not committed) | git metadata | event-driven | `/tmp/gsd29/merge-msg.txt`, quoted in P29 E-70 | exact |
| Nightly first-dispatch record (E-entry) | record | event-driven | P29 E-71 (CI run read from the REST API) | role-match |

## Pattern Assignments

### `VERSION` and `.planning/scripts/fork-anchors.txt` (config)

**Analog:** P29 E-01 (commit `112fbdb5`). One commit changes only these two files.

Current state: `VERSION` holds `v1.7.0_gg4`; `fork-anchors.txt` line 41 is `VERSION<TAB>v1.7.0_gg4<TAB>1<TAB>BLD-07`.

```text
-v1.7.0_gg4                                              (VERSION)
+v1.7.0_gg5
-VERSION	v1.7.0_gg4	1	BLD-07                       (fork-anchors.txt line 41)
+VERSION	v1.7.0_gg5	1	BLD-07
```

Fields are TAB-separated: `path`, fixed string, expected count, backs-ID. Verification, copied from P29 E-01 (needs `clean`, because `Version.java` is not a task input):

```sh
git show --name-only --format='%h %s' <bump-commit>
env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain clean compileJava
grep -n ARTIFACT_VERSION build/generated/sources/version/java/main/com/clickhouse/kafka/connect/sink/Version.java
# anchors mirror: for every row of fork-anchors.txt, grep -cF of the literal in the named file equals the row's count
```
Expected output: `ARTIFACT_VERSION = "v1.7.0_gg5"`, then `rows=<n> bad=0` and `ANCHORS-MIRROR-OK`. Row count is now above 94 because Phases 30 and 31 added rows (line 151+ holds the nightly rows).

---

### `CHANGELOG.md` (doc, new top section)

**Analog:** `CHANGELOG.md` lines 1-9 (heading and intro) and 103-122 (`## Build and CI`).

Heading shape: `# 1.7.0_gg4, 2026-10-23`. The intro paragraph says what it is built on, links `docs/FORK-DELTA.md` and the matching `docs/ROLLOUT-v1.7.0_gg4.md`, then points to the previous checklist. The section order is `## Important Changes`, `## Known Issues`, `## Bug Fixes`, `## Build and CI`. Bullets are `* ` with 2-space continuation, wrapped near 120 characters.

For gg5 (D-05): the first bullet leads with "no connector code changes (`src/main` identical to `1.7.0_gg4`)". Build and CI bullets then cover the pinned release workflows with `gh release create`, the nightly, the CIG-01 and NIT-02 guards, and `dependabot.yml` (it did change: `.github/dependabot.yml` shows 9 lines changed since `v1.7.0_gg4`, so it gets a bullet). Use the gg4 Build and CI bullet style:

```text
* The workflow pin-guard test strips comments string- and line-comment-aware, accepts a trailing comment on the pinned
  runner label, rejects commented-out fork-count text, and reads the `dependabot.yml` layouts that YAML allows.
```

Date check from P29 E-70: the heading date must equal the UTC date of the merge. Re-check it at the gate, because a merge after 23:59:59Z makes it wrong. Phase 29 ran a "CHANGELOG section check" (E-40) and a one-line reflow fix (IN-09); repeat the E-40 check.

---

### `docs/FORK-DELTA.md` (doc, edit in place)

**Analog:** the "Build, dependencies and CI" table (lines 58-74), columns `| Behaviour | Where it lives | Why upstream does not provide it | Pinned by |`.

The file's current state, which the planner must read before editing:

- **Line 74** already carries the release-workflow text: "`release.yaml`, `pre-release.yaml` and `increment-version.yml` are fork-modified (DEPS-F4, done by REL-01 to REL-05) ... create the GitHub release and its tag with the GitHub CLI ...". This is the paragraph D-06 says to replace. It begins "`integrationTests.yaml` and the composite action ... are intentionally left exactly as upstream ships them", and that sentence stays true for `integrationTests.yaml` only.
- The `dependabot.yml` row already says `exclude-paths` leaves out only `integrationTests.yaml`.
- Row `VERSION reads v1.7.0_gg4`: change to `v1.7.0_gg5`. Its "Pinned by" cell reads `BLD-07, PUB-01, PUB-07, PUB-11; Recorded Fact 4; MBeanNameUnitTest`.
- `grep -in "nightly|CIG-01|NIT-02"` of `docs/FORK-DELTA.md` and `CHANGELOG.md` returns nothing, so the nightly and both guards have no rows yet. Add the CI/workflows group there (D-06): rows for the release-path workflows (anchors in `fork-anchors.txt` lines with `release.yaml`, `pre-release.yaml`, `increment-version.yml`; for example `.github/workflows/release.yaml gradle-version 0 REL-03`), the nightly (anchors at `fork-anchors.txt` lines 151-156, `NIT-01`), `TestsWorkflowPinGuardUnitTest`/`WorkflowActionPinGuardUnitTest` (CIG-01), `UpstreamIntegrationWorkflowBytesGuardUnitTest` (NIT-02) and `NightlyWorkflowGuardUnitTest`.
- Intro paragraph (lines 3-7): add gg5 and the `ROLLOUT-v1.7.0_gg5.md` link, as it already does for gg4.

Raw numbers (lines 117-152): append rows `after gg5: v1.7.0 to v1.7.0_gg5` for both scopes, and append the two matching command lines. Refresh once, on the final tree.

```text
| `src build.gradle.kts gradle .github` | after gg4: `v1.7.0` to `v1.7.0_gg4` | 277 | 29679 | 643 |
| `src/main` | after gg4: `v1.7.0` to `v1.7.0_gg4` | 37 | 2877 | 241 |
git diff --shortstat v1.7.0 v1.7.0_gg4 -- src build.gradle.kts gradle .github
git diff --shortstat v1.7.0 v1.7.0_gg4 -- src/main
```

gg5 `src/main` numbers must equal the gg4 row (37 / 2877 / 241), because `src/main` did not change. That equality is itself a claim for the note. Paths resolve: P29 E-41 (coverage audit) and E-42 (path resolution) and E-43 (recount, skip inventory) are the check recipes to repeat; the "Named skip inventory" section must still agree with the 18 named skips.

---

### `docs/ROLLOUT-v1.7.0_gg5.md` (doc, operator-facing, one page)

**Analog:** `docs/ROLLOUT-v1.7.0_gg4.md`, for style only. gg4 is 363 lines; gg5 must be short (D-03).

Style to copy:

- H1 `# Rollout checklist: \`v1.7.0_gg4\` to \`v1.7.0_gg5\``, then a bold lead line, a pointer back to `ROLLOUT-v1.7.0_gg4.md` for earlier releases, and the placeholder rule (`<connector-name>`, `<rollback-commit>`; no host or production names).
- Per item: `## Item N: ...` with `### What changed`, `### Am I affected?`, `### What to do`.
- Measured claims carry "Measured on ClickHouse <SELECT version()> with client V2".

The five claims and their closest gg4 analog:

| gg5 claim | Analog text | Measure with |
|---|---|---|
| `git diff v1.7.0_gg4 -- src/main` is empty | none in gg4 (new); the freeze check of P29 E-60 | `git diff --quiet v1.7.0_gg4 <tip> -- src/main; echo $?` |
| Wrapper-built archive verified on release path | gg4 Item 6 and `verifyConfluentArchive` rows; Phase 30 dry-run record `30-COMMAND-EVIDENCE.md` E-09 | read from the REST API, per E-09 |
| MBean `version=` and client name read `v1.7.0_gg5` | gg4 Item 5 (lines 272-290) | copy its two bullets, swap gg4 for gg5 and gg3 for gg4; re-measure the live MBean and user agent as P29 E-02 did |
| Nightly on and off | none in gg4; source is `31-CONTEXT.md` D-01 | gate is `github.event_name == 'workflow_dispatch' \|\| vars.NIGHTLY_ENABLED == 'true'`; set or unset the repository variable `NIGHTLY_ENABLED`; manual dispatch always works |
| Rollback | gg4 Item 7 (lines 330-363) | see below |

Rollback excerpt to adapt (gg4 Item 7, step 2). The target is the `pre-v1.7.0_gg5` tag, which per D-02 names the `custom_build` tip before the merge (the gg4 tip, `21c51598`, if `origin/custom_build` does not move):

```text
1. ... Run `git fetch --tags origin`, `git checkout custom_build` and `git merge --ff-only origin/custom_build`, and check that
   `git status --porcelain` prints nothing, with one expected exception: ` M gradlew.bat` ...
2. Run `git read-tree -u --reset pre-v1.7.0_gg4`, then `git commit -m "Roll back v1.7.0_gg4"`. ...
   `git diff --quiet HEAD pre-v1.7.0_gg4 && echo ok` prints `ok`.
To go forward again, revert the rollback commit with `git revert <rollback-commit>`. The rollback removes this document from the branch;
read it again with `git show <rollback-commit>^:docs/ROLLOUT-v1.7.0_gg4.md`.
```

Because gg5 changes no connector code, the "Rolling back brings back" list reduces to: the gg4 workflows, the gg4 `VERSION`, MBean `version=` and client name. Warn against building an archive from the rolled-back tip only if it is still true (the gg4 warning concerned a Shadow 9 defect that the gg4 tree fixed, so the gg5 rollback tip `pre-v1.7.0_gg5` builds correctly; measure before keeping or dropping it).

Rehearse the rollback on the dry-run merge as P29 E-30 and E-70 did (script `/tmp/gsd29/rollback-demo.sh <repo> <target-tip> <worktree-path>`, outside the repo; re-create it for gg5, since `/tmp` content may be gone). Reviewer findings go to REVIEW files, not into the note (D-04).

---

### Phase-directory records

**`32-COMMAND-EVIDENCE.md`. Analog:** `P29/29-COMMAND-EVIDENCE.md` header (lines 1-6) and entry shape.

```markdown
# Phase 29: Command evidence

- **Base commit:** `P29_BASE` = `<sha>` (the tip when Phase 29 planning started; ...)
- **Shell:** macOS zsh, BSD `grep`. Gradle is always run as `env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew ...` because this shell sets that variable to the literal `null`. ...
- **Date (UTC):** 2026-10-23.
- **Rule:** a `Matches text: no` verdict means the document gets fixed, never the evidence. Entries that back no document text carry `Matches text: n/a (gate check)`. ...

### E-01: <title>

Backs: <document text or "nothing in a document; this is the ... gate check">.

```sh
<commands>
```

```text
<captured output>
```

Matches text: n/a (gate check)
```

Entry numbering to mirror: E-01 bump and clean build; E-02 MBean and client name; E-4x CHANGELOG, FORK-DELTA audit, path resolution and raw numbers recount; E-60 suite gate (two runs, `testUnit` with Docker stopped, freeze check); E-61 anchors gate and pre-push checks; E-62 rehearsal push and run; E-70 publication dry run; E-71 merge, checks and CI; E-72 tags and scratch deletion. Add a final E entry for the nightly's first manual dispatch (NIT-03). Phase 31's version of the header (`31-COMMAND-EVIDENCE.md`) adds a `Docker:` line (colima profile `vz`) and a `SELECT version()` entry. Record `SELECT version()` at each gate (ROADMAP note), using the command from P29 E-70:

```sh
docker run --rm --entrypoint clickhouse-local clickhouse/clickhouse-server:latest --query "SELECT version()"
```
(The Phase 31 variant is `docker run --rm --entrypoint clickhouse clickhouse/clickhouse-server:latest local --query 'SELECT version()'`.) Also record the image id and date, as E-70 did, to detect `latest` drift.

**`32-REVIEW.md`. Analog:** `31-REVIEW.md`. YAML front matter: `phase`, `reviewed`, `depth: standard`, `files_reviewed`, `files_reviewed_list`, `findings` (`critical`, `warning`, `info`, `total`), `status`. Body: `# Phase N: Code Review Report`, a Summary, then `## Warnings` and `## Info` with `### WR-01: <title>`, `**File:**`, `**Issue:**`, `**Fix:**`. A round with no findings is a clean pass (success criterion 2: rounds repeat until one finds nothing, at most three). Scope is the note, `CHANGELOG.md` and `docs/FORK-DELTA.md`. The reviewer is orchestrator-spawned and has fresh context. Memory notes say appended REVIEW.md rounds need `### ID:` headings and a reviewer's Fix text can itself be wrong.

**`32-REVIEW-DISPOSITION.md`. Analog:** `31-REVIEW-DISPOSITION.md`.

```markdown
---
phase: 31
review: 31-REVIEW.md
updated: 2026-10-26
---
# Phase 31 review disposition
| Finding | Severity | Disposition |
|---------|----------|-------------|
| WR-01 | Warning | open: <reason> |
```
For Phase 32, use `fixed` or `rejected` with a reason where the finding is on a release document (these must close; `open` is only for advisory findings).

---

## Phase 29 Publication-Gate Command Sequence (primary analog)

Sources: `P29/29-COMMAND-EVIDENCE.md` E-60 (lines 1296-1372), E-61 (1373-1450), E-62 (1451-1467), E-70 (1468-1593), E-71 (1594-1608), E-72 (1609-1620). Substitute `sync/v1.7.0` with the Phase 32 release branch (currently `sync/v1.7.0`, the branch HEAD is on), `/tmp/gsd29` with a new `/tmp/gsd32`, gg4 with gg5, and `69cd680b` (PRE) with the `origin/custom_build` tip, which should now be `21c51598` (the gg4 merge M) plus any Dependabot merges. Verify by re-fetch. The gates run orchestrator-inline (executors refuse relayed approvals; `custom_build` is GSD's protected base branch).

**Step 0. Freeze and ground truth (E-60, E-70).**
```sh
git fetch --quiet origin --no-tags
git ls-remote --heads origin custom_build          # must equal PRE; else absorb first (OPS-05 contingency, E-63)
git merge-base --is-ancestor <PRE> HEAD
git rev-list --left-right --count origin/custom_build...HEAD
git ls-remote --heads origin ; git ls-remote --tags origin   # snapshots to /tmp/gsd32/exec/origin-{tags,heads}-before-*.txt
```
The freeze check asserts that only the allowed document files differ from the phase base outside `.planning/`. P29's allowed list was `CHANGELOG.md`, `VERSION`, `docs/FORK-DELTA.md`, the ROLLOUT notes. Here it is `CHANGELOG.md`, `VERSION`, `docs/FORK-DELTA.md`, `docs/ROLLOUT-v1.7.0_gg5.md` and `.planning/scripts/fork-anchors.txt`, plus a zero diff over `src build.gradle.kts gradle .github`.

**Step 1. Cheap gates (E-60).**
```sh
env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain clean compileJava compileIntegrationTestJava
grep -q 'ARTIFACT_VERSION = "v1.7.0_gg5"' build/generated/sources/version/java/main/com/clickhouse/kafka/connect/sink/Version.java
colima stop --profile vz
/opt/homebrew/opt/docker/bin/docker info        # must fail
env -u TESTCONTAINERS_HOST_OVERRIDE -u CLICKHOUSE_CLUSTER_NAME CLICKHOUSE_VERSION=latest CLIENT_VERSION=V2 ./gradlew testUnit --rerun
colima start --profile vz
```
Then the full suite twice with `--rerun` (the exact invocation is in E-60 lines 1320-1372; read it there), same 18 named skips both times. Then the anchors gate, `bash .planning/scripts/verify-merge-invariants.sh` (invoke as E-61 lines 1373-1450 do; its selftest is `verify-merge-invariants-selftest.sh`). Known exception: ` M gradlew.bat` is always present (CRLF, `.gitattributes` `*.bat text eol=crlf`) and must never be staged. Every "status is empty" check must be written as "no line other than ` M gradlew.bat`". Phase 29's plan checks failed on this once (E-70 finding). Memory notes also say `./gradlew test` can exit 255 from an intermittent OpenJ9 crash: delete the core dump and re-run once, or use `-PjavaVersion=21`.

**Step 2. Rehearsal push of scratch, once, at a SHA no other pushed branch holds (E-62).** Needs the user's direct "push".
```sh
git push origin refs/heads/sync/v1.7.0:refs/heads/sync/v1.7.0
curl -s 'https://api.github.com/repos/ne1r0n/clickhouse-kafka-connect/actions/runs?head_sha=<X0>'
curl -s https://api.github.com/repos/ne1r0n/clickhouse-kafka-connect/actions/runs/<run-id>
curl -s https://api.github.com/repos/ne1r0n/clickhouse-kafka-connect/actions/runs/<run-id>/jobs
```
Record exactly one `tests.yaml` run on X0, none cancelled, conclusion `success`, one job "ClickHouse latest Client version V2 tests". After the scratch push, X may advance only by planning-only commits (X0..X stays planning-only; assert with `git diff --name-only X0 X`).

**Step 3. Publication dry run, no push (E-70).**
```sh
git fetch --quiet origin --no-tags
git worktree add --detach /tmp/gsd32/pubdry <PRE>
git -C /tmp/gsd32/pubdry -c user.name=demo -c user.email=demo@example.invalid merge --no-ff -F /tmp/gsd32/merge-msg.txt <X>
# assert: 2 parents (<PRE>, X); `git diff --quiet D X`; tree(D) == tree(X); .planning/.gsd-ws-arg not tracked;
#         rollback script run on D; no operator doc tells anyone to TRUNCATE
git worktree remove --force /tmp/gsd32/pubdry && git worktree prune
```
Merge message form (E-70): subject `Merge sync/v1.7.0: release v1.7.0_gg4`, a body readable without `.planning/`, and the attribution trailer from the session's system reminder (currently `Co-Authored-By: Claude Sonnet 5.5 <noreply@anthropic.com>`; `git commit` verbs add none on their own, per memory notes).

**Step 4. One direct user approval ("publish").** Re-run the precondition checks first (`PUBLISH-PRECONDITIONS-OK` in E-71): origin `custom_build` still equals PRE, X0's run is the only green run, and at least 20 API requests remain on `/rate_limit`.

**Step 5. Real merge and push of `custom_build` only (E-71).**
```sh
git worktree add /tmp/gsd32/cb custom_build
git -C /tmp/gsd32/cb merge --ff-only <PRE>
git -C /tmp/gsd32/cb merge --no-ff -F /tmp/gsd32/merge-msg.txt <X>
git -C /tmp/gsd32/cb push origin refs/heads/custom_build:refs/heads/custom_build     # no force
```
Check on M: `tree(M) == tree(X)`, parents are `<PRE>` and X, rollback demo on M. Then wait for the single `tests.yaml` run on M (P29: run 40022281005, about 6.5 minutes). Re-check about 2 minutes later that it is still exactly one run, green, and that origin `custom_build` equals M.

**Step 6. Tags by name, only after the green run (E-72).** Tag pushes start no workflow (CI-08, `tests.yaml` triggers on `branches: ['**']` only), and the E-72 re-check confirms it about 75 seconds later.
```sh
git tag v1.7.0_gg5 <M>
git tag pre-v1.7.0_gg5 <PRE>               # lightweight, local tag on the pre-merge tip (E-30 shows `git cat-file -t` prints `commit`)
git push origin refs/tags/v1.7.0_gg5:refs/tags/v1.7.0_gg5
git push origin refs/tags/pre-v1.7.0_gg5:refs/tags/pre-v1.7.0_gg5
git push origin --delete refs/heads/sync/v1.7.0          # scratch branch
```
Final assertion (`PUBLISHED-AND-TAGGED-OK`): tags added are exactly the two new ones and none removed; non-Dependabot heads are exactly `custom_build` (M) and `main`; Dependabot heads unchanged; no `release.yaml` or `pre-release.yaml` dispatch ran (D-01); the merge worktree is removed.

**Step 7. Post-publication Actions API checks (success criterion 4).** `integrationTests.yaml` still reports `state: disabled_manually` and no `schedule` run was created after the Phase 29 marker (`2026-10-16T09:38:23Z`, E-61). The nightly is in the state Phase 31 recorded. No workflow run created a release or tag the plan did not intend. Read the REST API with `curl -s https://api.github.com/repos/ne1r0n/clickhouse-kafka-connect/actions/workflows` (unauthenticated limit is 60 per hour; the Phase 17 CI-log note says job logs need a user-made read-only token in Keychain).

**Step 8. Nightly first manual dispatch (NIT-03).** The user dispatches it from GitHub's `Run workflow` control on `custom_build` and reports the result directly. Record it as an E entry. D-08: if red, the tags stay, the failure returns as a Phase 31 gap fix or a named skip-list entry, and `NIGHTLY_ENABLED` stays unset until a green run. `NIGHTLY_ENABLED` is set only after the green run is recorded, with no earlier scheduled run whose conclusion is not `skipped`. The nightly's `schedule` is `cron: '17 3 * * *'`.

Phase 29 deviations worth copying forward: (a) the worktree-clean check excludes the CRLF `gradlew.bat` line; (b) the date in the CHANGELOG heading is checked at merge time; (c) the dry-run merge D is never pushed, and the real M has the same tree as the rehearsed X.

---

## Shared Patterns

### Gradle invocation
**Source:** ROADMAP.md "Sync Mechanics" and every P29 evidence header.
**Apply to:** every gate and every build command.
```sh
env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain <tasks>
```
The shell sets `TESTCONTAINERS_HOST_OVERRIDE` to the literal `null`, which breaks Ryuk. Live runs also set `CLICKHOUSE_VERSION=latest CLIENT_VERSION=V2`. Any two-run gate uses `--rerun`. Docker is colima profile `vz` (bare `colima status` hits the wrong profile).

### Anchor discipline
**Source:** `.planning/scripts/fork-anchors.txt` header (lines 1-9).
**Apply to:** the `VERSION` bump commit.
"Any commit that intentionally changes an anchor updates this file in the same commit." The invariants script evaluates the file at every commit from the pre-tag to the range end.

### Document-versus-evidence verdict
**Source:** P29 evidence header rule.
**Apply to:** `32-COMMAND-EVIDENCE.md`, the note, CHANGELOG and FORK-DELTA.
`Matches text: no` means fix the document, never the evidence. Every number in a document traces to an E-entry.

### Commit shape
**Source:** memory note "Plan-phase orchestration gotchas" and P29 E-01 (`112fbdb5`, `chore(29-01): bump VERSION ... (PUB-11)`).
**Apply to:** the commit titles. Use `<type>(32-NN): <subject> (<REQ-ID>)`, planning commits separate from release-file commits, so X0..X stays planning-only. Do not commit in this mapping step.

### Standing no-edit rule
**Source:** ROADMAP Phase 32 Note (Phase 29 D-05).
**Apply to:** every plan's verify block.
Add a freeze check that fails if `git diff --name-only <phase-base> HEAD -- src build.gradle.kts gradle .github` is non-empty. A workflow defect goes back to Phase 30 or 31, or is deferred with reason, owner and re-open trigger in the milestone close record. `integrationTests.yaml` stays byte-identical (`UpstreamIntegrationWorkflowBytesGuardUnitTest` pins it).

## No Analog Found

| File | Role | Data Flow | Reason |
|---|---|---|---|
| Nightly first-dispatch evidence entry (NIT-03) | record | event-driven | No earlier phase had an operator-dispatched workflow; use P29 E-71's REST-API read as the shape. The user reports the result, so the entry quotes the user's direct answer as E-62 does ("push") |
| Set and unset text for `NIGHTLY_ENABLED` in the note | doc | request-response | gg4 and earlier notes have no workflow-toggle item; the source of truth is `31-CONTEXT.md` D-01 and `.github/workflows/nightly-integration-tests.yaml` lines 1-30 (job `if:` at line 22) |

## Metadata

**Analog search scope:** `.planning/milestones/v1.7.0_gg4-hardening-phases/29-release-publication/`, `.planning/phases/30-*`, `.planning/phases/31-*`, `docs/`, `CHANGELOG.md`, `VERSION`, `.planning/scripts/`, `.github/workflows/`, `src/test/java/.../ci/`.
**Files scanned:** about 20 read or grepped (29-COMMAND-EVIDENCE.md is 1620 lines; read by grep and non-overlapping ranges).
**Pattern extraction date:** 2026-10-26
