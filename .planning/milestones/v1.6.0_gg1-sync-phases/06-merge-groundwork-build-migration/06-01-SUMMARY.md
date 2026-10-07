---
phase: 06-merge-groundwork-build-migration
plan: 01
subsystem: infra
tags: [git, gradle, merge, upstream-sync, ci]

# Dependency graph
requires: []
provides:
  - "upstream git remote wired to ClickHouse/clickhouse-kafka-connect"
  - "pre-v1.6.0-sync tag naming the clean pre-merge custom_build tip"
  - "sync/v1.6.0 local scratch branch carrying the mechanical merge"
  - "one mechanical merge commit (673d29c3) where every conflicted path == upstream v1.6.0"
  - ".planning/scripts/verify-merge-invariants.sh + conflicted-paths.txt (D-02/D-09 invariants, script-verifiable)"
  - "Gradle wrapper bootstrapped at 9.6.1"
affects: [06-02, 06-03, "07-*", "08-*", "09-*", "10-*", "11-*"]

# Actuals (#2632)
actuals:
  tokens: 362542
  tasks: 2
  commits: 585

# Tech tracking
tech-stack:
  added: []
  patterns:
    - "Mechanical conflict resolution: git merge --no-commit --no-ff <ref>, then per-path git checkout --theirs -- <path> / git rm -- <path> — never -X theirs (D-01)"
    - "Invariant-verification script pattern mirroring v1.6.0:scripts/run-ch-version-client-matrix.sh bash idiom (set -uo pipefail, BASH_SOURCE-relative ROOT_DIR)"

key-files:
  created:
    - .planning/scripts/verify-merge-invariants.sh
    - .planning/scripts/conflicted-paths.txt
  modified:
    - .gitignore
    - .planning/STATE.md
    - .planning/state.json
    - .planning/milestone.lock

key-decisions:
  - "Two pre-tag housekeeping commits (chore: track .gsd in .gitignore; chore(06): remove GSD scratch checkpoint and sync execution-start state) were added ahead of pre-v1.6.0-sync to satisfy the task's hard 'git status --porcelain produces no output before tagging' requirement — .planning/STATE.md, .planning/state.json and .planning/milestone.lock were GSD orchestrator bookkeeping files with pending edits/an untracked lock file that were not explicitly named by the plan but blocked a clean tree (Rule 3 auto-fix)."
  - "Regenerated the 35 content-conflict + 5 modify/delete path lists directly from git merge-tree --write-tree custom_build v1.6.0 into a /tmp scratch location per the plan's explicit instruction, never transcribed from RESEARCH.md prose (D-09)."
  - "Per-path resolution loops were executed via `bash <script-file>` rather than inline zsh eval — inline `while read` / `for x in $(cat file)` loops in this shell intermittently lost PATH resolution for `git` (a zsh/tool-harness eval quirk, not a git or logic issue); wrapping the same commands in a standalone script invoked with `bash` avoided it and every git invocation ran cleanly. No plan behavior changed, only invocation mechanics."

patterns-established:
  - "verify-merge-invariants.sh: D-02 (every conflicted path byte-identical to upstream at the merge SHA) and SYNC-06 (no <<<<<<<-style marker in tracked source across the full pre-v1.6.0-sync..RANGE_END range) as two independently-toggleable, always-both-reported invariants — re-run in Phase 11 across the whole range."

requirements-completed: [SYNC-01, SYNC-02, SYNC-06, BLD-01, BLD-02]

coverage:
  - id: D1
    description: "upstream remote wired exactly once, no fetch required since v1.6.0 was already local"
    requirement: SYNC-01
    verification:
      - kind: other
        ref: "git remote get-url upstream && git remote -v (2 lines, fetch+push)"
        status: pass
    human_judgment: false
  - id: D2
    description: "pre-v1.6.0-sync tag names the clean custom_build tip; scratch branch sync/v1.6.0 created from it; custom_build unmoved and unpushed"
    requirement: SYNC-02
    verification:
      - kind: other
        ref: "git rev-parse pre-v1.6.0-sync == git rev-parse custom_build; git rev-list --count pre-v1.6.0-sync..custom_build == 0; git config --get branch.sync/v1.6.0.remote prints nothing"
        status: pass
    human_judgment: false
  - id: D3
    description: "one mechanical merge commit (673d29c3) landing all 40 conflicted paths as upstream-verbatim or deletion, no .planning/ churn inside it, no unmerged paths"
    requirement: SYNC-06
    verification:
      - kind: other
        ref: "git rev-list --parents -n1 HEAD (3 SHAs), git show --stat HEAD -- .planning/ (empty), git diff --name-only --diff-filter=U (empty)"
        status: pass
    human_judgment: false
  - id: D4
    description: "build.gradle.kts and gradle/libs.versions.toml adopted byte-identical to v1.6.0, signing/buildconfig plugins gone"
    requirement: BLD-01
    verification:
      - kind: other
        ref: "git grep -i -e signing -e buildconfig HEAD -- build.gradle.kts (empty); git rev-parse HEAD:gradle/libs.versions.toml == git rev-parse v1.6.0:gradle/libs.versions.toml"
        status: pass
    human_judgment: false
  - id: D5
    description: "Gradle wrapper bumped to 9.6.1 (properties + jar both arrive from upstream in commit 1) and bootstraps cleanly"
    requirement: BLD-02
    verification:
      - kind: other
        ref: "env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --version | grep 'Gradle 9.6.1'"
        status: pass
    human_judgment: false
  - id: D6
    description: "verify-merge-invariants.sh + conflicted-paths.txt committed, prove D-02 and SYNC-06 against commit 1, and correctly fail on both degenerate inputs (empty worklist, zero-commit range)"
    verification:
      - kind: other
        ref: "bash .planning/scripts/verify-merge-invariants.sh <merge-sha> (exit 0); same with emptied conflicted-paths.txt (exit 1); same with RANGE_END=pre-v1.6.0-sync (exit 1)"
        status: pass
    human_judgment: false

duration: ~20min
completed: 2026-09-18
status: complete
---

# Phase 6 Plan 1: Merge Groundwork Summary

**Landed the 581-commit upstream v1.6.0 merge onto scratch branch `sync/v1.6.0` as one 100%-mechanical commit (`673d29c3`) with every one of the 40 conflicted paths verified byte-identical to upstream, backed by a committed, self-testing invariant script.**

## Performance

- **Duration:** ~20 min
- **Started:** 2026-09-18T18:20:00Z (approx.)
- **Completed:** 2026-09-18T18:33:00Z (approx.)
- **Tasks:** 2
- **Files modified:** 222 (across both commits, `git diff --stat pre-v1.6.0-sync..HEAD`)

## Accomplishments

- Added the `upstream` remote (`https://github.com/ClickHouse/clickhouse-kafka-connect.git`), confirmed `v1.6.0` resolves both locally and via a live fetch — no duplicate-remote risk (idempotent by design: the plan's step asserts the URL rather than re-adding).
- Tagged the clean, GSD-bookkeeping-free `custom_build` tip as `pre-v1.6.0-sync` and branched `sync/v1.6.0` from it (local-only, no tracking remote, never pushed).
- Generated the 40-path conflict worklist (35 content conflicts + 5 modify/delete) directly from `git merge-tree --write-tree custom_build v1.6.0` — **confirms this session's re-derivation of the count exactly**: 35 + 5 = 40 across 40 unique paths (the earlier "39 unique paths" figure in ROADMAP/REQUIREMENTS was a miscount; there is no doubly-counted path — `src/testFixtures/.../SchemaTestData.java` and `src/integrationTest/.../SchemaTestData.java` are two distinct, same-basename files).
- Merged mechanically per D-01: `git merge --no-commit --no-ff v1.6.0`, then per-path `git checkout --theirs -- <path>` for all 35 content conflicts and `git rm -- <path>` for all 5 modify/delete conflicts — no `-X theirs`, no hand-editing. Landed as commit `673d29c3` with second parent `v1.6.0` (`789a57c2`) and first parent `pre-v1.6.0-sync`/`custom_build` (`6ac2d20`).
- Verified D-02 (every one of the 40 conflicted paths byte-identical to `v1.6.0` at the merge commit) and confirmed the 5 modify/delete paths no longer exist in the tree — both by direct `git diff --quiet`/`git cat-file -e` checks and later by the committed script.
- Ran the bootstrap probe (`./gradlew --version`, deliberately not `help`): Gradle 9.6.1 downloaded and reported cleanly on the first attempt — **no wrapper-jar regeneration fallback was needed**, confirming this session's earlier blob-equality finding.
- Authored and committed `.planning/scripts/verify-merge-invariants.sh` (D-09) and `.planning/scripts/conflicted-paths.txt` (40 lines, generated not transcribed) in a second, non-merge commit (`4f3ff2a9`). The script proves both invariants against the merge commit, walks the full `pre-v1.6.0-sync..RANGE_END` range for SYNC-06 (not just `HEAD`, per Pitfall 5), reports every violation rather than short-circuiting, assembles the 7-`<` marker at runtime via `printf` so its own body never contains the literal, and scopes the marker search to tracked source only (`src`, `build.gradle.kts`, `settings.gradle.kts`, `gradle`, `.github`, `VERSION`, `.gitignore`).
- Confirmed both required degenerate-input failures: an emptied `conflicted-paths.txt` and a zero-commit range (`RANGE_END=pre-v1.6.0-sync`) both correctly exit non-zero with a printed error rather than passing vacuously.

## Task Commits

Housekeeping (pre-tag, both required by the task's hard clean-tree gate):

0. **Clean the tip (part 1)** - `54d794f` (chore) — commit the pending `.gitignore` edit (adds `.gsd`); the already-index-staged `06-DISCUSS-CHECKPOINT.json` rode along in this commit (see Deviations).
0. **Clean the tip (part 2)** - `6ac2d20` (chore) — `git rm --cached` the GSD scratch checkpoint file back out, plus commit `.planning/STATE.md`/`.planning/state.json`/`.planning/milestone.lock` GSD orchestrator bookkeeping that was also blocking a clean tree.

Plan tasks:

1. **Task 1: End-to-end — clean tip → rollback tag → scratch branch → mechanical merge commit → wrapper bootstraps** - `673d29c3` (merge, tracer)
2. **Task 2: Commit the D-02/D-09 invariant script and its generated conflicted-path list, and prove both invariants against commit 1** - `4f3ff2a9` (chore)

**Plan metadata:** committed separately, see below.

_Note: Task 1 is a merge commit with two parents (`6ac2d20` / `789a57c2`); Task 2 is an ordinary single-parent commit on top of it, per D-01's two-commit shape (commit 2 of D-01, the fork-build-additions commit, is plan 06-02's job, not this plan's)._

## Files Created/Modified

- `.planning/scripts/verify-merge-invariants.sh` - D-02/SYNC-06 invariant-verification script, executable, mirrors `v1.6.0:scripts/run-ch-version-client-matrix.sh` bash style
- `.planning/scripts/conflicted-paths.txt` - 40-line generated worklist (35 content + 5 modify/delete), sorted, no duplicates
- `.gitignore` - housekeeping commit only; still upstream's version post-merge (fork's `.gsd`/`.vscode`/`/bin/` union resolution is plan 02's job per D-04)
- `.planning/STATE.md`, `.planning/state.json`, `.planning/milestone.lock` - GSD orchestrator bookkeeping, committed to reach a clean tree before tagging (not a plan-scoped file, see Deviations)
- 216 further files changed by the mechanical merge itself (`build.gradle.kts`, `gradle/libs.versions.toml`, `gradle/wrapper/gradle-wrapper.properties`, `gradle/wrapper/gradle-wrapper.jar`, `VERSION`, `.github/workflows/tests.yaml`, and the `src/main`/`src/test`/`src/integrationTest`/`src/testFixtures` conflicted + auto-merged paths) — untouched hand-edits, all upstream-verbatim or auto-merge output per D-01

## Decisions Made

- Committed `.planning/STATE.md` / `.planning/state.json` / `.planning/milestone.lock` as GSD orchestrator housekeeping ahead of the merge sequence, since the task's acceptance criteria hard-require a fully clean `git status --porcelain` before `pre-v1.6.0-sync` is tagged and these files were blocking that — see Deviations for the full Rule 3 rationale.
- Kept the merge-tree-derived worklists in `/tmp` (outside the repo) during Task 1 exactly as instructed, so no untracked `.planning/` file existed mid-merge; the committed, permanent copy is generated fresh in Task 2 from the same immutable `custom_build`/`v1.6.0` refs and verified byte-identical to the `/tmp` copy.
- Resolved a shell-harness quirk (inline zsh `while read`/`for x in $(cat …)` loops intermittently reporting `command not found: git` mid-loop, purely an eval/PATH-resolution artifact of this tool's bash invocation) by writing the per-path resolution and verification loops to standalone script files and invoking them with `bash <script>` — no change to the git plumbing itself, every command executed is exactly what the plan specifies.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 3 - Blocking] Committed GSD orchestrator bookkeeping files to reach the required clean tree before tagging**
- **Found during:** Task 1, Step 1 ("Do not proceed until `git status --porcelain` produces no output")
- **Issue:** Beyond the plan-named `.gitignore` edit and `06-DISCUSS-CHECKPOINT.json` scratch file, `.planning/STATE.md` and `.planning/state.json` had uncommitted GSD-orchestrator-authored edits (phase-execution-start bookkeeping), and `.planning/milestone.lock` (a GSD session lock, historically committed-then-deleted per prior-milestone git history) was untracked. None of these three files are named in the plan's `<files>` or action text, but leaving them uncommitted/untracked would have left `git status --porcelain` non-empty, blocking the tag per the task's own hard requirement.
- **Fix:** Committed all three alongside the `06-DISCUSS-CHECKPOINT.json` removal in a second `chore(06):` commit, immediately after the plan-specified `.gitignore` commit and before tagging.
- **Files modified:** `.planning/STATE.md`, `.planning/state.json`, `.planning/milestone.lock`, `.planning/phases/06-merge-groundwork-build-migration/06-DISCUSS-CHECKPOINT.json` (removed)
- **Verification:** `git status --porcelain` produced no output immediately before `git tag pre-v1.6.0-sync` (an explicit acceptance criterion for this task).
- **Committed in:** `6ac2d20`

**2. [Rule 3 - Blocking] `06-DISCUSS-CHECKPOINT.json` landed in the first chore commit instead of being `git rm --cached`-removed there**
- **Found during:** Task 1, Step 1
- **Issue:** The file was already index-staged (as an addition) from before this session started, with its content deleted only from the worktree (`git status` showed `AD`). Running `git add .gitignore && git commit` picked up that pre-existing staged addition too, since it was already in the index.
- **Fix:** Removed it with `git rm --cached` in the very next commit, which is exactly the "same commit or an adjacent chore: commit" latitude the plan itself grants.
- **Files modified:** `.planning/phases/06-merge-groundwork-build-migration/06-DISCUSS-CHECKPOINT.json`
- **Verification:** File absent from the working tree and from `HEAD` after `6ac2d20`; `git status --porcelain` clean.
- **Committed in:** `6ac2d20`

---

**Total deviations:** 2 auto-fixed (both Rule 3 — blocking issues preventing the mandatory clean-tree precondition). **Impact:** No production code touched; both fixes are pure planning/orchestrator bookkeeping housekeeping required to satisfy the plan's own acceptance criteria. No scope creep into `src/main`, `build.gradle.kts`, or any of the four files the plan explicitly reserves for plan 02.

## Issues Encountered

- This shell's inline `while IFS= read` and `for x in $(cat file)` loops intermittently failed to resolve `git` mid-iteration (`command not found: git`), a harness/zsh-eval quirk unrelated to git or the repo. Worked around by writing the per-path merge-resolution and invariant-verification logic to standalone script files under `/tmp` and invoking them with `bash <script>`, which executed cleanly every time. No git command's arguments or sequence changed from what the plan specifies — only the invocation wrapper.
- `git rev-list --count pre-v1.6.0-sync..HEAD` is `583` after Task 1 and includes all 581 upstream commits brought in by the merge's second parent, not just executor-authored commits; the frontmatter `actuals.commits: 585` (measured per the #3968 ledger protocol from `PLAN_HEAD_BEFORE=c746420` to `HEAD`) is similarly dominated by those same upstream commits. Only 4 commits in that range are first-parent/executor-authored: `54d794f`, `6ac2d20`, `673d29c3`, `4f3ff2a9`.

## User Setup Required

None - no external service configuration required.

## Next Phase Readiness

- **Merge commit SHA for plans 02 and 03: `673d29c3d5dc24b97de3b022acdfe473e1a5493c`.**
- **Confirmed conflicted-path count: 40** (35 content + 5 modify/delete), matching the corrected session figure, not the stale "39 unique paths" figure in ROADMAP.md/REQUIREMENTS.md — plan 03 (and any doc-sync work) should inherit `40`, not `39`.
- No wrapper-jar regeneration fallback was needed (Task 1 Step 7's contingency did not trigger) — Gradle 9.6.1 bootstrapped cleanly on the first `./gradlew --version` invocation.
- HEAD is on `sync/v1.6.0`, two commits past `pre-v1.6.0-sync` (`673d29c3` then `4f3ff2a9`) — exactly the shape plan 02 expects to continue from (its commit 2 lands the five fork build additions, `VERSION=v1.6.0_gg1`, and `.gitignore`/`tests.yaml` settlement).
- No push occurred to `origin` at any point, and `sync/v1.6.0` has no tracking remote — both prohibitions honored.
- No compilation or test task was run at any point (`gradlew help`/`dependencies`/`compileJava`/`assemble`/`build`/`test` were all correctly avoided; only `./gradlew --version` ran, per the plan's explicit "bootstrap probe only" instruction).
- `REQUIREMENTS.md`: SYNC-01 and SYNC-02 marked complete now (this plan is their sole declaring plan). SYNC-06, BLD-01, BLD-02 are also fully satisfied by this plan's work but stayed `pending` in `REQUIREMENTS.md` per the shared-ID gate (#2388) — sibling plans 06-02/06-03 also declare them and haven't produced a `*-SUMMARY.md` yet; they flip to `Complete` automatically once the last declaring plan finishes.

## Self-Check: PASSED

- `.planning/scripts/verify-merge-invariants.sh` — FOUND on disk
- `.planning/scripts/conflicted-paths.txt` — FOUND on disk
- Commit `54d794f` — FOUND in git log
- Commit `6ac2d20` — FOUND in git log
- Commit `673d29c` — FOUND in git log (merge commit)
- Commit `4f3ff2a` — FOUND in git log
- All task `<acceptance_criteria>` re-verified passing (D-02 invariant over all 40 paths, tracer `<verify>` block, task 2's four `<verify>` blocks including both degenerate-input failure cases)

---
*Phase: 06-merge-groundwork-build-migration*
*Completed: 2026-09-18*
