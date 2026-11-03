# Phase 44: Release & Publication - Research

**Researched:** 2026-11-02
**Domain:** fork release engineering for `v1.7.0_gg7`: version bump, final CHANGELOG and FORK-DELTA, one operator note, a generated archive allow-list, two full gates, an orchestrator-run independent review, and a user-gated publication through `release.yaml`
**Confidence:** HIGH for repository, origin, archive and gate facts (read or measured this session); MEDIUM for the review-round count and for anything that depends on the user's choices at the gate

Evidence tags used below: `[VERIFIED: path:lines]` (file opened this session, value quoted), `[VERIFIED: command]` (command run this session, output quoted), `[CITED: file]` (a committed planning record, read but not re-run), `[ASSUMED]` (listed in the Assumptions Log). Scratch work is under `/tmp/gsd44r/` (two archive exports and builds, compare outputs, a candidate allow-list, one `testUnit` run and one full-suite run on an export of `HEAD`). The only file written in the repository is this one; `git status --short` after the full suite still read only the two orchestrator-managed files `.planning/config.json` and `.planning/state.json`.

**Loud items for the orchestrator (nothing here is a STOP, three need a user answer before or at the gate):**
1. **No STOP-level blocker was found.** Origin `custom_build` is still `72e8ccff` (the gg6 merge), upstream `main` is still `9bed6f79` with newest tag `v1.7.0`, PRs #861, #855, #857 are all still open, `latest` is the same image as for the last six phases, and testUnit and the full suite are green on the current tree (section 5).
2. **Who runs the origin writes (push, merge push, dispatch, tag push, branch delete) is not stated for gg7.** The gg6 decision (b) was "user-run handoffs, orchestrator reads GitHub read-only". The ROADMAP says only that the user launches `release.yaml` and that every write needs the user's direct word. Default recommended here: repeat gg6 (the user runs every write with their own account from handoff files; the orchestrator builds `M` and reads back). Ask once, in the bundle, so the handoff files can be written for the right shape (Open Question 9).
3. **`docs/FORK-DELTA.md` holds a claim that is false today:** row at line 138 says `Processing.java` is byte-identical to upstream `v1.7.0`, and the same file's row at line 62 says the RSK-01 change "makes `Processing.java` differ from upstream for the first time" (measured: `git diff --quiet v1.7.0 HEAD -- .../Processing.java` exits non-zero). The counts at line 139 (33 of 63 identical) and line 188 (40 differ) are stale for the same reason. Phase 44's FORK-DELTA pass must fix them (section 6.2). This is the "re-measure the FORK-DELTA claims" item of the Phase 43 handoff.

<user_constraints>
## User Constraints (from CONTEXT.md)

CONTEXT.md was auto-generated (`workflow.skip_discuss`), so it holds no user decisions of its own. The ROADMAP goal, success criteria and Note are the locked scope. Copied verbatim from `44-CONTEXT.md`:

### Locked Decisions
None beyond the ROADMAP: "All implementation choices are at Claude's discretion — discuss phase was skipped per user setting. Use ROADMAP phase goal, success criteria, and codebase conventions to guide decisions."

Locked by the ROADMAP Note and the Phase 43 ledger handoff (read, not discretionary):
- This phase edits no `src/`, test, `build.gradle.kts`, `gradle/` or `.github/` file (Phase 29 D-05). A defect found here goes back to its phase as a gap, or is deferred with a reason, an owner and a re-open trigger in the milestone close record.
- The recipe is the gg6 one: (1) check `origin/custom_build...sync/v1.7.0` for divergence before gating; (2) push the scratch branch once, at a SHA no other pushed branch holds, and wait for green; (3) merge with `--no-ff` from a throwaway worktree, so the merged tree equals the rehearsed tip; (4) push `custom_build` only, wait for its single run; (5) the user launches the publish run (`release.yaml`, `dry_run` not ticked), push `pre-v1.7.0_gg7` by name after it; (6) delete the scratch branch.
- Before the gate recheck upstream (tags past `v1.7.0`, PRs #861, #855, #857). The CHANGELOG heading carries the merge date in UTC. The approval gates run orchestrator-inline.
- "The gg7 rollout note states 'FUT-05 stays open' in its RSK-03 section" (the user's answer "gg6 note C2 + gg7 handoff (Recommended)"), and the RLS-03 reviewer brief covers the published documents edited in Phase 43. `[CITED: 43-DISPOSITION-LEDGER.md "Handoff to Phase 44"]`

### Claude's Discretion
Plan and wave split; wording and structure of the note, CHANGELOG section and FORK-DELTA edits; helper scripts; scratch branch name; layout of the handoff files.

### Deferred Ideas (OUT OF SCOPE)
"None — discuss phase was skipped." Future requirements stay out: FUT-01 (tagged upstream sync), FUT-02, FUT-03 (protobuf 4), FUT-04 (retire V1), FUT-05 (retry budget; only its rollout statement is in scope). `[CITED: .planning/REQUIREMENTS.md, FORK-DELTA item 8]`
</user_constraints>

<phase_requirements>
## Phase Requirements

| ID | Description | Research Support |
|----|-------------|------------------|
| RLS-01 | `VERSION=v1.7.0_gg7` through `bump_version.py`; CHANGELOG, FORK-DELTA and `fork-anchors.txt` updated in the same commits as the counts they change | Section 3 (bump measured on a scratch export: only the `VERSION` anchors row changes, rows stay 190), section 6 (CHANGELOG and FORK-DELTA edit lists, raw numbers measured) |
| RLS-02 | `confluent-archive-allowlist-v1.7.0_gg7.txt` generated from the final tree with gson rows; dry-run compare reads `0 unallowed, 0 stale rows` | Section 4 (two archives built, 120 differences, an 18-row candidate list proved exit 0, three mutations) |
| RLS-03 | `docs/ROLLOUT-v1.7.0_gg7.md` covers F-1, F-2, RSK-01..04 with labelled claims and passes a fresh `gsd-code-reviewer` | Section 7 (row inventory with proposed labels and sources), section 8 (review brief, heading rule, round rules) |
| RLS-04 | Two full runs on `latest` x V2 green, `--no-ff` merge only after the user's direct approval, one green `tests.yaml` run on the published SHA, tag created by the publish run | Section 5 (gates and measured totals), section 9 (order of operations, questions and what each approval authorises) |
</phase_requirements>

## Project Constraints (from CLAUDE.md)

`[VERIFIED: ./CLAUDE.md -> AGENTS.md and ./.claude/CLAUDE.md read this session]`
- No commits to the branch, no PR title or description changes, without explicitly asking the user. (Research does not commit. The plan's commits follow the project's existing GSD flow, which commits per task.)
- Do not upgrade dependencies without asking; do not log secrets or record data; Spotless is enforced but `spotlessApply` must never run over the whole tree (46 violations pre-exist; this phase edits no Java anyway).
- `VERSION` is bumped and `CHANGELOG.md` updated for a release; `.claude/CLAUDE.md` adds: tests pin current fork behaviour, code changes only for genuine bug fixes, keep changes additive for rebasability.
- GSD workflow enforcement: file edits go through a GSD workflow (this phase is one).
- `h` is a zsh alias for `history`: run shell snippets with `bash -c` or from a script. Never `git clean`.
- Memory rules read: orchestrator-inline approvals; fresh `gsd-code-reviewer` before the push gate; `### ID: title` headings; rollback commands tested on the publication tip; zsh `${M}:refs/...`; no `timeout` binary; colima profile `vz`; Dependabot merges diverge origin; nightly red is inherited.

## Summary

Phase 44 repeats the Phase 38 (gg6) recipe with a smaller content load and one structural simplification. Everything up to the user-gated writes is local and Docker-free except the two full suites. The measured facts that shape the plan: the bump moves exactly one anchors row and leaves `rows=190`; the archive differs from the `v1.7.0_gg6` rebuild in exactly 120 entries (101 gson classes plus gson's three metadata files, one removed gson class, 14 connector classes and the manifest) and an 18-row allow-list covers all of them with no overlap and no stale row; `testUnit` reads 1826 tests (2 skipped) in 13 s with Docker unreachable and the full suite on `latest` x V2 reads 2425 tests (18 skipped) in 2 min 46 s, both on the current tree.

The long poles are not the builds. They are (1) the independent review of the note (gg6 needed 12 rounds, because claims about client V1 versus client V2 and about "unbuffered" paths kept being scoped too widely; gg7's F-2, F-1 and RSK rows have the same scoping hazard, so the plan should build scoping into every row from the first draft), (2) the cumulative merge-invariant script (about 40 minutes, once, on an explicit SHA), and (3) the user's own writes.

Three concrete corrections the plan must carry, found while measuring: the false `Processing.java` byte-identical row and the stale counts in FORK-DELTA; the `HEAD`-relative reproduction commands at FORK-DELTA lines 184 and 188 (Phase 43 WR-03; the figures at 184 are correct tag-to-tag, so only the command text changes); and the ROADMAP wording "a left count of 1 is the gg6 merge", which is stale (measured today `0 197`: the gg6 merge is already an ancestor of `sync/v1.7.0`, so the expected left count is 0 and anything above 0 means origin moved).

**Primary recommendation:** five local plans in the gg6 shape (1: bump, CHANGELOG, FORK-DELTA; 2: the note; 3: orchestrator review rounds; 4: allow-list last and the gates; 5: rehearsal, approval bundle, handoff files, runbook) followed by an orchestrator-inline, user-gated publication tail (U1 scratch push and dry run, "publish" approval and build of `M`, U3 push of `M`, U4 publish dispatch, U5 pre tag and clean-up), with every origin write run by the user unless the user says otherwise.

## Architectural Responsibility Map

| Capability | Primary Tier | Secondary Tier | Rationale |
|------------|-------------|----------------|-----------|
| Version bump, CHANGELOG, FORK-DELTA, rollout note | Local repo, executor agent (`sync/v1.7.0`) | none | Plain files; one commit holds `VERSION` plus its anchors row |
| Independent review of operator documents | Orchestrator (spawns `gsd-code-reviewer`) | none | Executors have no Agent tool; an inline review does not count `[CITED: memory gsd-phase15-review-gate-gotchas]` |
| Allow-list generation and compare proof | Local repo, executor (builds in `/tmp`) | GitHub runner (repeats the compare in the dry run) | Same steps as `release.yaml`; the list is a derived artifact |
| Two full suites, Docker-free `testUnit`, anchors gates | Local repo, executor (colima `vz`) | none | Needs Docker and the pinned `latest` image |
| Dry-run merge `D` and rollback rehearsal | Local repo, throwaway detached worktree | none | Phase 16 lesson: test rollback on the publication tip |
| Merge commit `M` construction | Orchestrator inline, throwaway worktree on `72e8ccff` | Origin (user pushes by SHA) | `custom_build` is GSD's protected base branch; `M` is built detached |
| Scratch push, `custom_build` push, tag push, branch delete, release dispatch | User (default; Open Question 9) | Origin / GitHub runner | The ROADMAP requires the user's direct word for each; the publish run is the user's |
| Approval | User, directly to the orchestrator | none | Executors refuse relayed approvals |
| Read-back after each user step | Orchestrator, read-only `gh api`/`git ls-remote` | none | No agent runs a push, tag, dispatch, re-run, cancel, comment, merge or close |

## Standard Stack

No library is added and nothing is installed. The phase uses what is in the repository and on the host.

### Core
| Tool | Version | Purpose | Source |
|------|---------|---------|--------|
| `.github/scripts/bump_version.py` | in tree | writes `VERSION` and the anchors `VERSION` row in one staged step; refuses a mismatched row | `[VERIFIED: .github/scripts/bump_version.py docstring and run]` |
| `.github/scripts/baseline_tag.py` | in tree | baseline tag = greatest `vX.Y.Z[_ggN]` tag below `VERSION` among tags merged into `HEAD` | `[VERIFIED: command]` `git tag --merged HEAD --list 'v*' \| python3 -I .github/scripts/baseline_tag.py v1.7.0_gg7` printed `v1.7.0_gg6` |
| `.github/scripts/compare_confluent_archives.py` | in tree | entry-by-entry archive compare under an allow-list | `[VERIFIED: file read lines 1-60, runs]` |
| `.planning/scripts/verify-merge-invariants.sh` | in tree | cumulative anchors gate: `bash .planning/scripts/verify-merge-invariants.sh 54e63648 <explicit-sha>` | `[CITED: 42-COMMAND-EVIDENCE.md line 750; 41-03-SUMMARY.md "40 min of it"]` |
| Phase helpers | in tree | `43-evidence-check.py` (junit, skips, failed, anchors, expect, index), `38-allowlist-check.py` (rows, cover, same-globs), `38-review-check.py` (round, rounds, disposition) | `[VERIFIED: files read and run this session]` copy into `44-` names; `38-review-check.py` caps rounds at 12 and had to be edited seven times in gg6, so give the copy the cap as an option from the start |
| Gradle wrapper 9.8.2 | in tree | `createConfluentArchive`, `testUnit`, `test`, `generateVersionFile` | `[VERIFIED: gradle-wrapper.properties, both v1.7.0_gg6 and HEAD read "gradle-9.8.2-bin.zip"]` |
| Host | Python 3.14.8, `gh` 2.102.0 (read-only GETs only), Docker server 27.4.0 on colima profile `vz` (running), Homebrew `java` 21.0.12 on PATH, default test JVM Semeru 17.0.9 (OpenJ9) | scripts, reads, suites | `[VERIFIED: commands]` (the test JVM path read from `ps` during the suite) |

### Alternatives Considered
| Instead of | Could Use | Tradeoff |
|------------|-----------|----------|
| gg6 five-plan shape | one big plan | Rejected: the review loop and the gates are separate checkpoints with separate owners |
| User-run origin writes (gg6) | orchestrator runs the pushes on the user's direct word | Allowed by the ROADMAP wording ("only on the user's direct word"); needs the user to say so; changes who writes the handoff files (Open Question 9) |
| Four labels in the note (gg6) | three labels | RLS-03 and the ROADMAP name three: `measured`, `read from the code`, `derived`. Use exactly these; `derived` carries a parenthetical (see section 7.0) |

**Installation:** none. **Version verification:** not applicable (no package).

## Package Legitimacy Audit

No external package is installed or recommended. The gate does not apply: `gson` 2.15.0 and `setup-gradle` 6.4.1 entered in Phase 39 (their commits, `dependencyInsight` and pin guards are recorded there) and are documented, not added, here. No `research-plan` or `package-legitimacy` seam call was made; there was no library-API or ecosystem question to look up.

**Packages removed due to [SLOP] verdict:** none. **Packages flagged [SUS]:** none.

## Architecture Patterns

### System Architecture Diagram

```
LOCAL (executors, sync/v1.7.0, nothing pushed)                    ORIGIN / GITHUB (writes by the user unless told otherwise)
------------------------------------------------                  ----------------------------------------------------------
W0 read-only pre-flight: origin custom_build, upstream tags+PRs, PR heads, Docker, latest digest
W1 bump_version.py gg -> commit {VERSION, fork-anchors.txt}; clean generateVersionFile; anchors rows=190
   CHANGELOG 1.7.0_gg7, FORK-DELTA final (raw numbers once, recount, stale rows)
W2 docs/ROLLOUT-v1.7.0_gg7.md + evidence map (every row labelled, scoped to client V1/V2)
W3 ORCHESTRATOR spawns fresh gsd-code-reviewer (<=3 rounds unless the user authorises more) -> fixes -> dispositions -> docs frozen
W4 allow-list generated LAST (build v1.7.0_gg6 tag export + tip export, compare) -> commit
   gates on gated head H: testUnit (Docker unreachable) | 2x test --rerun on one pinned latest | anchors mirror | cumulative invariants once on H
W5 dry-run merge D (detached, on 72e8ccff) + rollback block rehearsal + approval bundle + handoff files + runbook
        | user's direct "go" to the orchestrator
        v
U1 -------------------------------------------------------------> push X0 to scratch branch sync/v1.7.0 -> tests.yaml run (1, green)
U2 -------------------------------------------------------------> dispatch release.yaml from scratch (forced dry run): build, baseline from tag, COMPARE, draft+delete
        | user's own direct "publish" message
        v
O  build M = merge --no-ff X0 onto 72e8ccff (detached), tree(M)==tree(X0)
U3 -------------------------------------------------------------> push M:refs/heads/custom_build (non-force) -> tests.yaml on M (exactly 1, green)
U4 -------------------------------------------------------------> dispatch release.yaml from custom_build, dry_run NOT ticked
                                                                   compare -> tests.yaml gate -> tag-absent gate -> gh release create v1.7.0_gg7 --target M
A  read-only GETs: release, tag object type, asset, run steps -> local release record
U5 -------------------------------------------------------------> push tag pre-v1.7.0_gg7 by name; delete scratch branch; PRs #21/#22 read back
```

### Recommended file layout (all under `.planning/phases/44-release-publication/` unless noted)
```
44-COMMAND-EVIDENCE.md                  # E-nn entries (the Phase 41-43 format)
44-evidence-check.py                    # copy of 43-evidence-check.py (+ skips, failed, junit, anchors)
44-allowlist-check.py                   # copy of 38-allowlist-check.py + the "two literal segments" rule (Handoff item 5)
44-review-check.py                      # copy of 38-review-check.py with --max-rounds
44-note-check.py, 44-doc-check.py       # optional: row/label/forbidden-word checks as in gg6 (38-note-check.py, 38-doc-check.py)
44-REVIEW.md, 44-REVIEW-DISPOSITION.md  # reviewer rounds, heading rule section 8
44-MERGE-MESSAGE.txt                    # merge message file (subject below)
44-APPROVAL-BUNDLE.md, 44-USER-DISPATCH-{1..4}-*.md, 44-ORCHESTRATOR-RUNBOOK.md
44-RELEASE-RECORD.md                    # local tail, written after U4 (release facts)
docs/ROLLOUT-v1.7.0_gg7.md              # published
.planning/scripts/confluent-archive-allowlist-v1.7.0_gg7.txt   # written last (exact name; see section 4)
```

### Pattern 1: Publish by merge SHA, not by branch checkout
**What:** Build `M` in a detached throwaway worktree on `72e8ccff`, never move the local `custom_build` ref; `M` has parents `72e8ccff` and `X0` and `tree(M) == tree(X0)`. `72e8ccff` is an ancestor of the tip, so `--no-ff` still makes a real merge commit whose SHA differs from `X0`, which keeps `tests.yaml`'s SHA-keyed concurrency group from collapsing the scratch run and the `custom_build` run. `[VERIFIED: .github/workflows/tests.yaml:31-33 "group: ${{ github.workflow }}-${{ github.event_name == 'pull_request' && github.event.number || github.sha }}", "cancel-in-progress: true"]`
**Shape of gg6's `M`:** parents `3a3a15a9` and `a46be37f`, subject `Merge sync/v1.7.0: release v1.7.0_gg6`. `[VERIFIED: git rev-list --parents -n1 72e8ccff; git log -1 72e8ccff]` Use `Merge sync/v1.7.0: release v1.7.0_gg7` and the attribution trailer of this session's reminder as the last line (`git merge -F` adds none).
**The `gradlew.bat` trap (measured in gg6):** a fresh worktree at the base lists ` M gradlew.bat` and a plain merge stops on it; run the merge, and the rollback `switch`/`revert` steps, with `git --attr-source="$(git hash-object -t tree /dev/null)"` (git 2.40 or later). `[CITED: 38-05-SUMMARY.md deviation 1; docs/ROLLOUT-v1.7.0_gg6.md Rollback step 2]` The base `72e8ccff` has the same `gradlew.bat` blob as `3a3a15a9`'s successor line, so expect the same trap; re-measure on `D`.

### Pattern 2: Allow-list as a derived artifact (section 4)
### Pattern 3: User handoff document shape
Status; what you decide (what the push publishes, with counts); numbered pre-checks each with an expected value (stop on any other); the exact command; the dispatch (UI wording and CLI); expected result; fields to report (run URL and id, `headSha`, event, branch, conclusion, per-step conclusions, notices, artifact, no draft left); if it fails earlier; clean-up; "Resume (for the orchestrator)": record the user's report verbatim with the word `user-reported`, never act on a relayed approval. `[CITED: gg6 38-USER-DISPATCH-*.md, 38-ORCHESTRATOR-RUNBOOK.md]`

### Anti-Patterns to Avoid
- **Hand-pushing `v1.7.0_gg7`, or creating it locally.** The step `Check the release tag is absent` refuses to publish over it. Only `pre-v1.7.0_gg7` is pushed by hand. `[VERIFIED: release.yaml header comment and step list]`
- **Writing the allow-list before the tree is final.** Any later change to `src/`, `build.gradle.kts`, `gradle/` or `README.md` changes the archive. (`docs/`, `CHANGELOG.md`, `VERSION` do not feed it except through `Version.class`, which `VERSION` does.) `[CITED: 38-RESEARCH.md section 4]`
- **Treating a green dry run as proof of the publish step.** The dry run skips `Publish release`. The publish step itself has run once, for gg6 (FORK-DELTA item 22, run `41298074815`). `[CITED: docs/FORK-DELTA.md item 22]`
- **Re-running or re-pushing to turn a red run green** without the user's explicit answer.

## Don't Hand-Roll

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| Version bump with anchors row | `sed` on `VERSION` and the anchors file | `python3 -I .github/scripts/bump_version.py gg` | Stages both writes, refuses a mismatched row, restores on failure |
| Baseline tag choice | A hard-coded `v1.7.0_gg6` | the `baseline_tag.py` pipe above | The same code the workflow runs |
| Archive compare | `diff`, `unzip` trees | `compare_confluent_archives.py` | Reads in memory, escapes log-forging names, refuses catch-all rows |
| Allow-list entries | `*` or a leading-wildcard row | anchored literal-prefix globs, one reason each | A row starting with `*`, `?` or `[` is refused (exit 2) `[VERIFIED: compare_confluent_archives.py:12-17 docstring]` |
| Anchors gate | manual grep | the mirror helper (`anchors ... --rows 190`) plus `verify-merge-invariants.sh` once | Per-commit anchor evaluation with SHA-pinned exemptions |
| JUnit totals, named skips | ad hoc XML reading | `43-evidence-check.py junit/skips/failed` | Checks the 18 named skips by name (`OK skips: named=7 state_mismatch_cases=11 unexpected=0`) |
| Gate-day date check | eyeballing | `date -u +%F` against `head -1 CHANGELOG.md` | The CHANGELOG heading date was wrong once (gg6) |

**Key insight:** every number in the note and in FORK-DELTA must come from a command the planner can name; the gg6 review spent 12 rounds finding claims that said more than their evidence. Build the evidence map before the prose.

## Runtime State Inventory

Not a rename or migration phase. A version bump has no runtime state outside the repository and origin refs. The places the version string lives: `VERSION`, the anchors row at `fork-anchors.txt:41` (`VERSION	v1.7.0_gg6	1	BLD-07`, the only anchors row whose path is `VERSION`, `CHANGELOG`, `docs/` or `README` `[VERIFIED: awk over the file]`), `CHANGELOG.md` line 1, the FORK-DELTA prose, the generated `Version.java`, the archive manifest. Test classes use `v1.7.0_gg5`/`gg6` only as literal fixtures: testUnit read 1826 green on an export whose `VERSION` was bumped to `v1.7.0_gg7`. `[VERIFIED: command, /tmp/gsd44r/tip]`

## 1. Pre-gate upstream recheck and origin divergence (settle a)

Measured today (2026-11-02, read-only):

| Item | Reading | Tag |
|------|---------|-----|
| origin `custom_build` | `72e8ccffe05ff6dffb14098d0b8c0f67aaa6a236`; `git rev-parse origin/custom_build` is the same; GitHub `protected: false` (the "protected base branch" is a GSD rule) | `[VERIFIED: git ls-remote origin; gh api .../branches/custom_build]` |
| left/right count | `git rev-list --left-right --count origin/custom_build...sync/v1.7.0` prints `0	197`; `git merge-base --is-ancestor 72e8ccff HEAD` is true; `git rev-list --count HEAD --not --remotes=origin` prints 195 | `[VERIFIED: command]` |
| ROADMAP wording | "A left count of 1 is the gg6 merge" is stale: the left count is 0 because `sync/v1.7.0` already contains the gg6 merge. Expected pre-check value: **0**. Above 0 means origin moved | `[VERIFIED: command]` |
| origin tags | `v1.7.0_gg1..gg6`, `pre-v1.7.0_gg2..gg6`, `v1.6.0_gg1`, `pre-v1.6.0-sync`, `pre-v1.7.0-sync`; no `v1.7.0_gg7`, no `pre-v1.7.0_gg7`. Locally `pre-v1.7.0_gg7` does not exist yet either | `[VERIFIED: git ls-remote origin 'refs/tags/*'; git tag -l]` |
| origin heads | `custom_build`, `main` `bec167d8`, `dependabot/github_actions/github-actions-3bc6d10805` (`14be67ce`), `dependabot/gradle/com.google.code.gson-gson-2.15.0` (`b5cccbd3`), `scratch/clt-07-client-0.11.0`, `scratch/clt-07-prebump-0.9.6`; **no `sync/v1.7.0`** | `[VERIFIED: ls-remote]` |
| open PRs on origin | exactly #21 (setup-gradle 6.4.1, head `14be67ce`) and #22 (gson 2.15.0, head `b5cccbd3`), both `dependabot[bot]`. Both heads are ancestors of the tip (`merge-base --is-ancestor` true), so GitHub should mark them merged once `M` lands (the same thing happened for #16, #18, #19, #20 at gg6) | `[VERIFIED: gh api pulls; git merge-base]`, precedent `[CITED: 38-COMMAND-EVIDENCE.md E-27 "pr_16_18_19_20: MERGED (A1 confirmed)"]` |
| releases | `v1.7.0_gg6` id 430511927 (`github-actions[bot]`), `v1.7.0_gg5` id 426690002 (`ne1r0n`); no gg7 | `[VERIFIED: gh api releases]` |
| runs on `72e8ccff` | one publish run `41298074815` success; scheduled nightly runs end `skipped` (repository variables: `total_count` 0, so `NIGHTLY_ENABLED` is unset); Dependabot dynamic runs | `[VERIFIED: gh api actions/runs; actions/variables]` |
| upstream | `main` `9bed6f7970670a55c66be8efda246889e1f95bd0`; newest tag `v1.7.0` (`379b8d01`); no tag past it | `[VERIFIED: git ls-remote upstream]` |
| upstream PRs | #861 `[client upgrade] Isolated client init by version and made tests use only v2` open, head `3d4cfdf2`, updated 2026-10-26; #855 `fix: apply jdbcConnectionProperties on Client V2 (mTLS)` open, head `ddabf28b`, updated 2026-10-27; #857 `fix: include Kafka record context in serialization errors` open, head `11d88bc0`, updated 2026-10-29; issue #777 open; PR #786 closed, `merged: false` | `[VERIFIED: curl GET api.github.com/repos/ClickHouse/clickhouse-kafka-connect/pulls/N]` |

### The recheck block (read-only; run at execution start and again at the approval bundle and before U3)
```bash
git ls-remote origin refs/heads/custom_build                      # expect 72e8ccffe05ff6dffb14098d0b8c0f67aaa6a236
git rev-list --left-right --count origin/custom_build...sync/v1.7.0   # expect left 0 (right grows with every commit)
git merge-base --is-ancestor 72e8ccff HEAD && echo ancestor
git ls-remote --tags origin 'v1.7.0_gg7*' 'pre-v1.7.0_gg7*'          # expect empty
git ls-remote --heads origin sync/v1.7.0                           # expect empty (scratch name free)
git ls-remote --tags upstream                                      # newest must still be v1.7.0
git ls-remote upstream refs/heads/main                             # 9bed6f79... unless upstream moved
for n in 861 855 857; do curl -s https://api.github.com/repos/ClickHouse/clickhouse-kafka-connect/pulls/$n | python3 -I -c 'import json,sys;d=json.load(sys.stdin);print(d["number"],d["state"],d["merged"],d["head"]["sha"][:8])'; done
gh api 'repos/ne1r0n/clickhouse-kafka-connect/pulls?state=open' --jq '.[] | [.number,.head.sha[0:8]]'
```
**What a result means.** An upstream tag past `v1.7.0` makes the next sync FUT-01, which stops for the user `[CITED: STATE.md line 390]`; it does not block this release (the release is the fork's own), but the user decides whether to publish gg7 first or sync first. A merged #861/#855/#857 without a tag changes nothing for the release; record it in the evidence and in the FORK-DELTA syncer paragraph (they touch `ClickHouseHelperClient`, `ClickHouseWriter` and the same builder blocks as Phases 40 and 41, so the next sync has conflicts there). Origin moved (left above 0): push nothing; merge `origin/custom_build` into `sync/v1.7.0` (never rebase), check landed lines against the anchors (`dependabot-merges-on-custom-build` memory), re-gate, ask the user to approve again. A new Dependabot PR is not a stop; the user merging #21 or #22 on origin before `M` is (it diverges `X0`): the bundle must tell the user not to.

## 2. What Phases 39-43 put in this release (the content to document)

`git diff --shortstat v1.7.0_gg6 HEAD` over `src/main`: 8 files, 194 insertions, 22 deletions: `ClickHouseSinkTask`, `ProxySinkTask`, `ClickHouseWriter`, `ClickHouseHelperClient`, `Processing`, `StateProvider`, `KeeperStateProvider`, `Utils`. Over the other build-affecting paths: `gradle/libs.versions.toml` 1 line (gson), five `.github/workflows` files 6 lines (the setup-gradle pin, plus the `increment-version.yml` header comment), `src/integrationTest` 2 files (F-3 harness). `[VERIFIED: command]`

| Phase | Change | Operator-visible? |
|-------|--------|-------------------|
| 39 | gson 2.14.0 -> 2.15.0 (archive: gson classes change, `LinkedTreeMap$1` is gone); `setup-gradle` 6.4.1 pin (CI only); pins for wrapped-`DataException` classification and the `custom_*` forwarding tripwire; F-3 harness fix (integration harness only) | gson: yes (archive); the rest: no |
| 40 | F-1: writer closes `chc` on `stop()` and failed `start()`, V1 per-batch helper closed, `KeeperStateProvider.close()` via `StateProvider.close()`, `ProxySinkTask.stop()` nested `try/finally` | yes |
| 41 | F-2: per-insert `socket_timeout` at three client V2 insert sites unless the operator names `socket_timeout` | yes (behaviour change) |
| 42 | RSK-01 `Processing.doLogic` line 137 reads `originalTopic()`; RSK-02 `RetriableException` pass-through in `Utils.handleException`; RSK-03 start-up WARN; RSK-04 limits recorded | yes |
| 43 | published-note corrections, a header comment, two CI test fixes, the disposition ledger | no (docs, tests) |

## 3. RLS-01: the bump, the generated-version check, the same-commit rule (settle b)

### The bump (measured on a scratch export; nothing in the repository touched)
```
cd /tmp/gsd44r/tip && python3 -I .github/scripts/bump_version.py gg
-> v1.7.0_gg7
diff of fork-anchors.txt:  41c41  < VERSION<TAB>v1.7.0_gg6<TAB>1<TAB>BLD-07   >  VERSION<TAB>v1.7.0_gg7<TAB>1<TAB>BLD-07
```
`[VERIFIED: command]`. The real call is `python3 -I .github/scripts/bump_version.py gg` from the repository root, then ONE commit holding exactly `VERSION` and `.planning/scripts/fork-anchors.txt`. That is the shape `increment-version.yml` itself commits (`git add VERSION .planning/scripts/fork-anchors.txt`, anchor row CIH-03 `[VERIFIED: fork-anchors.txt:165]`). The anchors file has 225 lines of which 190 are rows; the mirror helper must still read `rows=190 bad=0` after the bump (`python3 -I .planning/phases/43-document-fixes-phase-37-dispositions/43-evidence-check.py anchors .planning/scripts/fork-anchors.txt --rows 190` printed `rows=190 bad=0` today `[VERIFIED: command]`).

**Every anchors row changes in the same commit as the count it changes (RLS-01).** Phase 44 edits no anchored file except `VERSION`: the only anchors paths outside `src/`, `.github/`, `gradle/` and `build.gradle.kts` are `.gitattributes` and `VERSION` `[VERIFIED: awk over the file]`, so there is exactly one row to move and `bump_version.py` moves it. If a later review fix touches a file with an anchor row, it moves the row in that commit. Add to the plan: after the last docs commit, `git diff --name-only 72e8ccff HEAD -- src build.gradle.kts gradle .github` must list exactly the Phase 39-43 paths and no Phase 44 path.

### Generated version agrees after `clean`
`generateVersionFile` writes `Version.java` from `project.version`, which is `file("VERSION").readText().trim()` `[VERIFIED: build.gradle.kts:48 and 504-516]`. The generated file is not an input of the task, so in the real working tree (whose `build/` holds a gg6 copy) the check must be `./gradlew clean generateVersionFile` and then reading the file. Measured on the export: `public static final String ARTIFACT_VERSION = "v1.7.0_gg7";` `[VERIFIED: /tmp/gsd44r/tip/build/generated/sources/version/java/main/com/clickhouse/kafka/connect/sink/Version.java]`, and the compare reports `MANIFEST.MF: differs only in Implementation-Version`.
```bash
env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain clean generateVersionFile
grep -n 'ARTIFACT_VERSION' build/generated/sources/version/java/main/com/clickhouse/kafka/connect/sink/Version.java   # expect "v1.7.0_gg7"
git tag --merged HEAD --list 'v*' | python3 -I .github/scripts/baseline_tag.py "$(tr -d '[:space:]' < VERSION)"        # expect v1.7.0_gg6
```
Create the local rollback tag in the same plan (a local lightweight tag is harmless and is the gg6 shape): `git tag pre-v1.7.0_gg7 72e8ccffe05ff6dffb14098d0b8c0f67aaa6a236`. Never create a local `v1.7.0_gg7`.

### CHANGELOG `1.7.0_gg7` section
- The file starts `# 1.7.0_gg6, 2026-10-31` at line 1 with sections `Important Changes`, `Improvements`, `Known Issues`, `Build and CI`. `[VERIFIED: grep -n '^# \|^## ' CHANGELOG.md]` Add `# 1.7.0_gg7, <UTC date>` above it; leave every released section byte-identical (a gg6 review rule). The gg6 `Known Issues` bullets (writer never closes its client; the four-minute timeout has no effect while `async=false`; the Datagen counting; the retriable `DataException` cases) stay as history; the gg7 section states their closure or their new state. There is no `# Unreleased` block left to absorb `[VERIFIED: grep]`.
- Skeleton: intro (upstream base `1.7.0` plus tail `9bed6f79`, changes since `v1.7.0_gg6`, pointer to `docs/ROLLOUT-v1.7.0_gg7.md` and FORK-DELTA); `Important Changes` (F-2 bounded insert with its behaviour change and client V2 scope first; RSK-02 `RetriableException` pass-through); `Bug Fixes` (F-1, RSK-01, F-3 harness); `Improvements` (RSK-03 start-up WARN); `Known Issues` (the residuals of FORK-DELTA items 13, 14, 16, 17, 24, FUT-05 open); `Build and CI` (gson 2.15.0, `setup-gradle` 6.4.1, the allow-list compare for gg7 against `v1.7.0_gg6`). Name no planning identifier and state nothing about the gg7 release object (asset, tag SHA).
- **Heading date.** It must equal the UTC date at the merge. UTC now is 2026-11-02 20:03 (22:03 local, +02:00); the date flips at 02:00 local. Write the date at the last pre-gate commit and compare at the approval bundle and again before building `M` (`date -u +%F` against `head -1 CHANGELOG.md`). gg6 published with a heading one day early and the user chose to keep it `[CITED: 38-RELEASE-RECORD.md "Recorded deviations"]`; do not rely on that again. If the dates differ at U3: ask the user directly: keep as a recorded deviation, or a date-fix commit. A date-fix commit touches `CHANGELOG.md` only, which does not feed the archive or any test, so the allow-list and the suites stay valid; but it makes a new `X0'`, which needs a new scratch push if `X0` was already pushed, so run U1 to U3 inside one UTC day.

## 4. RLS-02: generating the gg7 allow-list last (settle c)

### Measured result
Both archives were built the way `release.yaml` builds them: the baseline from an export of tag `v1.7.0_gg6` with the tag's own wrapper, the candidate from an export of `HEAD` with `VERSION` bumped to `v1.7.0_gg7` by the real script.

| Archive | verify line | size (bytes) | SHA-256 |
|---------|-------------|--------------|---------|
| baseline gg6 (`/tmp/gsd44r/base/build/confluent/clickhouse-kafka-connect-v1.7.0_gg6.zip`) | `verifyConfluentArchive: 7933 inner entries, no nested jar, no Kafka API class` | 13558633 | `877802f6ac90cfd19a9a5395fff58b3c83979be7bdc8409dfd421fdd5aa4b50b` |
| candidate gg7 (`/tmp/gsd44r/tip/build/confluent/clickhouse-kafka-connect-v1.7.0_gg7.zip`) | `verifyConfluentArchive: 7932 inner entries, no nested jar, no Kafka API class` | 13564144 | `5f4e097c84e678d43de7e58d4d2019c9e4f0ac3aa2ba2e70f481f8fe3c08f310` |

`[VERIFIED: command]`. The published gg6 asset is 13558692 bytes, 59 more than the local rebuild of its tag (gg5 showed the same kind of difference), so never compare a runner digest with a local one `[CITED: 38-RELEASE-RECORD.md; 38-RESEARCH.md section 4 reproducibility note]`; the compare tool is the check.

Compare with a comment-only list: exit 1, `totals files-a 7513 files-b 7512 only-a 1 only-b 0 changed 119 same 7393`, 120 `UNALLOWED` lines, `nested jars in b: 0`, `kafka classes in b: 0`, `MANIFEST.MF: differs only in Implementation-Version`, and the `VERSION` table changes in exactly one line: `VERSION com.google.code.gson:gson 2.14.0 -> 2.15.0` `[VERIFIED: /tmp/gsd44r/out/cmp-empty.out]`.

### The 120 differences by owner
| Owner | Entries | Notes |
|-------|---------|-------|
| gson 2.14.0 -> 2.15.0, `com/google/gson/*` | 102 (101 CHANGED, 1 ONLY-A `com/google/gson/internal/LinkedTreeMap$1.class`) | `LinkedTreeMap$1` is in `gson-2.14.0.jar` and not in `gson-2.15.0.jar` `[VERIFIED: unzip -l of both jars in the Gradle cache]` |
| gson metadata | 3 CHANGED: `META-INF/maven/com.google.code.gson/gson/pom.properties`, `.../pom.xml`, `META-INF/versions/9/module-info.class` | the descriptor is gson's (it appears in both gson jars; the hashes differ) |
| connector classes, exact paths | 14 CHANGED | `ClickHouseSinkTask`, `ProxySinkTask`, `Version`, `db/ClickHouseWriter`, `db/ClickHouseWriter$1`, `db/ClickHouseWriter$2`, `db/helper/ClickHouseHelperClient`, `db/helper/ClickHouseHelperClient$ClickHouseClientBuilder`, `processing/Processing`, `state/StateProvider`, `state/provider/KeeperStateProvider`, `util/Utils`, `util/Utils$FailedRecords`, `util/jmx/SinkTaskStatistics` |
| manifest | 1 CHANGED: `META-INF/MANIFEST.MF` | `Implementation-Version` only |

The eight changed `src/main` files map to those classes (`StateProvider` and `KeeperStateProvider` are new differences compared with gg6; `Version` and `SinkTaskStatistics` are the release identity, because `Version.ARTIFACT_VERSION` is inlined by javac).

### What differs from the gg6 list
The gg6 list (31 rows, `.planning/scripts/confluent-archive-allowlist-v1.7.0_gg6.txt`) names client, zstd-jni, lz4-java and the gg5-to-gg6 connector classes. None of those differ between gg6 and gg7 (the client, zstd-jni and lz4-java are byte-identical in both archives: they are in the 7393 `same`). So the gg7 list is a new, shorter list: **18 rows** = 3 gson rows + 14 exact connector rows + the manifest. Never copy the gg6 list (its rows would be STALE-ROW). `[VERIFIED: compare run]`

### Candidate list (proved in `/tmp/gsd44r/out/allow-gg7-candidate.txt`)
```
com/google/gson/*                                      gson 2.14.0 -> 2.15.0 (classes; LinkedTreeMap$1 is gone in 2.15.0)
META-INF/maven/com.google.code.gson/gson/*             gson 2.14.0 -> 2.15.0 (pom.properties and pom.xml)
META-INF/versions/9/module-info.class                  gson 2.14.0 -> 2.15.0 (module descriptor; this jar supplies the descriptor at this path)
com/clickhouse/kafka/connect/sink/ClickHouseSinkTask.class                       source changed: start-up WARN for additionalRetriableErrorCodes outside the vetted set
com/clickhouse/kafka/connect/sink/ProxySinkTask.class                            source changed: stop() closes writer, MBean and state provider in try/finally
com/clickhouse/kafka/connect/sink/Version.class                                  release identity v1.7.0_gg6 -> v1.7.0_gg7
com/clickhouse/kafka/connect/sink/db/ClickHouseWriter$1.class, $2.class, .class  source changed (helper clients closed; per-insert socket_timeout)
com/clickhouse/kafka/connect/sink/db/helper/ClickHouseHelperClient$ClickHouseClientBuilder.class, ClickHouseHelperClient.class
com/clickhouse/kafka/connect/sink/processing/Processing.class                    source changed: doLogic reads originalTopic()
com/clickhouse/kafka/connect/sink/state/StateProvider.class                      source changed: default no-op close()
com/clickhouse/kafka/connect/sink/state/provider/KeeperStateProvider.class       source changed: close()
com/clickhouse/kafka/connect/util/Utils$FailedRecords.class, Utils.class         source changed: RetriableException pass-through
com/clickhouse/kafka/connect/util/jmx/SinkTaskStatistics.class                   release identity
META-INF/MANIFEST.MF                                   release identity v1.7.0_gg6 -> v1.7.0_gg7 (Implementation-Version)
```
(each class row on its own line with a tab and a reason in the real file; the above compresses the list). **Proved:** `python3 -I .github/scripts/compare_confluent_archives.py base.zip tip.zip allow-gg7-candidate.txt` exits 0, `RESULT: PASS`, no `UNALLOWED`, no `STALE-ROW`; `38-allowlist-check.py rows ... --expect 18` printed `OK rows=18`; `38-allowlist-check.py cover cmp-empty.out candidate` printed `OK differences=120 covered=120 uncovered=0 overlapping=0 stale_rows=0`. Three mutations: a row naming nothing -> `STALE-ROW com/clickhouse/nothing/*`, `RESULT: FAIL`; deleting the `com/google/gson/*` row -> 102 `UNALLOWED` lines. `[VERIFIED: command]`

### Rules the generated file must meet
- **Name:** `.planning/scripts/confluent-archive-allowlist-v1.7.0_gg7.txt`; `release.yaml` builds the path as `allowlist=".planning/scripts/confluent-archive-allowlist-${RELEASE_VERSION}.txt"` and fails when it is missing `[VERIFIED: .github/workflows/release.yaml, step "Select the baseline tag"]`.
- **Format:** `glob<TAB>reason`, a non-empty reason, no glob that is only stars, none starting with `*`, `?` or `[`; at least one row (`CompareConfluentArchivesScriptUnitTest` loads every shipped list and reports `STALE-ROW` against identical synthetic archives) `[CITED: 38-RESEARCH.md section 4; compare_confluent_archives.py docstring lines 7-17]`.
- **Header (Phase 43 handoff item 5):** the first line names `.github/scripts/compare_confluent_archives.py` (the older lists named a hyphenated `compare-confluent-archives.py`, fixed for gg4 and gg5 in commit `103221a4`; gg6 and `-25` keep the old name by decision). Convention from gg4: third-party rows may be globs; a `com/clickhouse/kafka/connect/` row is an exact path whose source changed, or the release identity.
- **UF-37-3 check (Phase 43 handoff item 5):** every wildcard row follows at least two literal path segments. Measured on the candidate: `com/google/gson/*` has three literal segments, `META-INF/maven/com.google.code.gson/gson/*` has four. Add this as a `rows`-style check in `44-allowlist-check.py` (the 38 copy has no such rule) `[VERIFIED: python over the candidate]`.
- **Generated LAST:** after the docs freeze and before the gates; no `src/`, `build.gradle.kts`, `gradle/`, `README.md` or workflow commit may follow it. `README.md` is the only repository document inside the archive besides `LICENSE` `[CITED: 38-RESEARCH.md A9]`; Phase 44 does not edit it.

### Regeneration recipe (agent-doable, no Docker, no origin)
1. On the final tip export `git archive HEAD | tar -x -C <new dir>` and the tag `v1.7.0_gg6` the same way (each into its own new empty directory; `python3 -I` for any script reading them).
2. Build each with its own wrapper: `env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain createConfluentArchive` (14 s and 11 s measured); the line `verifyConfluentArchive: <n> inner entries ...` must appear.
3. Compare with a comment-only list to get the full `UNALLOWED` set; derive the rows by owner as above; write the file; re-run: exit 0, `RESULT: PASS`, no `STALE-ROW`.
4. Mutations: one stale row (exit 1, `STALE-ROW`), one deleted row (`UNALLOWED`), one leading-wildcard row (exit 2).
5. `38-allowlist-check.py rows` (+ the two-segment rule) and `cover`.
6. Commit the file as the last non-gate commit. Any later change of the archive inputs invalidates it.
Because the archive inputs (`src/main`, dependency set, `README.md`) are frozen since Phase 42, the file above is expected to equal the final list; the plan must still regenerate it from the final tree and `diff` it against this candidate as a sanity check, not copy it.

## 5. RLS-04 gates (settle f)

### Expected and measured totals
| Gate | Command | Measured today on an export of `HEAD` (VERSION bumped) | Prior record |
|------|---------|--------------------------------------------------------|--------------|
| testUnit, Docker unreachable | `DOCKER_HOST=unix:///nonexistent/docker.sock env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain testUnit --rerun` | **1826 tests, 0 failures, 0 errors, 2 skipped**, BUILD SUCCESSFUL in 13 s | Phase 43 verifier read 1826 / 2 `[CITED: 43-VERIFICATION.md]` |
| full suite on `latest` x V2 | `CLICKHOUSE_VERSION=latest CLIENT_VERSION=V2 env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain test --rerun` | **2425 tests, 0 failures, 0 errors, 18 skipped**, BUILD SUCCESSFUL in 2 m 46 s, `OK skips: named=7 state_mismatch_cases=11 unexpected=0`, `OK failed: 0 failing testcases`, 138 XML files | Phase 42 post-fix 2423 `[CITED: 42-VERIFICATION.md:92]` + Phase 43's 2 Docker-free tests = 2425; Docker-only delta 2425 - 1826 = 599 (gg6 measured 598) |
| anchors mirror | `43-evidence-check.py anchors .planning/scripts/fork-anchors.txt --rows 190` | `rows=190 bad=0` | Phase 42 `anchor_rows=190` |

`[VERIFIED: command; /tmp/gsd44r/out/testunit.log, suite1.log, JUnit XML read with 43-evidence-check.py]`. These are expectations for the plan to check and then measure again on the release tip, not values to copy into evidence. The test JVM on this run was the default Semeru 17.0.9 (OpenJ9) with the host's swap at about 98 % (`vm.swapusage` used 27 218 MB of 27 648 MB), and no crash occurred. Do not read that as the crash being gone: it recurred twice in gg6.

### Pinned `latest` and drift check
- Local image `clickhouse/clickhouse-server@sha256:739e099abd1157390960a59e4532d21781d8fde57e6e4151cb920ea02c30b1ce`, created 2026-10-26T00:06:54Z, `SELECT version()` = `26.10.3.7`; Docker Hub `latest` read today: digest `sha256:739e099a...b1ce`, `last_updated` 2026-10-26T00:08:26Z, the same value. It is the image of Phases 34 to 43. The digest read after my full suite was still the same. `[VERIFIED: docker image inspect; curl hub.docker.com/v2/repositories/clickhouse/clickhouse-server/tags/latest; docker run --entrypoint clickhouse ... local --query 'SELECT version()']`
- "Pinned" means: do not `docker pull`; read `docker image inspect --format '{{index .RepoDigests 0}}' clickhouse/clickhouse-server:latest` before run 1, after run 1 and after run 2 (all equal) and `SELECT version()` at each. If the Hub digest moves before U1 the runner's `tests.yaml` tests a newer build than the local pin: record both digests, do not re-pin. If a suite is red on unchanged code, check the image date first `[CITED: memory gsd-phase11-orchestration-gotchas]`.
- That Testcontainers does not re-pull a present local `latest` is supported by today's observation (digest unchanged across a full suite) and by the gg6 reads; keep the before/after reads as the proof `[ASSUMED, A5]`.

### Gate commands (the order matters; `--rerun` on every run)
```bash
./gradlew --stop                                                # then check swap: sysctl vm.swapusage
rm -rf build/test-results/testUnit
DOCKER_HOST=unix:///nonexistent/docker.sock env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain testUnit --rerun > $E/gate-testunit.log 2>&1
rm -rf build/test-results/test
CLICKHOUSE_VERSION=latest CLIENT_VERSION=V2 env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain test --rerun > $E/suite1.log 2>&1   # run twice, rm -rf build/test-results/test between
python3 -I 44-evidence-check.py junit|skips|failed ...
python3 -I 44-evidence-check.py anchors .planning/scripts/fork-anchors.txt --rows 190
bash .planning/scripts/verify-merge-invariants.sh 54e63648 <H> > $E/invariants.log 2>&1       # once, alone, in the background, ~40 minutes
```
- **No tail gate this time.** gg6 ran a tail-form gate (`PRE_TAG`, `UPSTREAM_REF`, `CONFLICTED_PATHS_FILE`) because it carried an upstream tail sync. gg7 has none; Phases 39, 41 and 42 each ran the cumulative form `54e63648 <sha>` only `[VERIFIED: 39-/41-/42-COMMAND-EVIDENCE.md command lines]`.
- **The cumulative script's cost:** about 40 minutes (Phase 41 E-10: started 09:22:03Z, ended 10:02:23Z; `anchor_commits=1428 ... anchor_rows=190 ... result=PASS` at Phase 42). Run it once, on the explicit SHA `H` = the last commit that changes any path outside `.planning` (bump, CHANGELOG, FORK-DELTA, note, review fixes) plus the allow-list commit; assert afterwards `git diff --quiet H X0 -- . ':(exclude).planning'` and `git diff --quiet H X0 -- .planning/scripts` so the judged tree equals the pushed tree. The script judges per-commit anchor rows, so the `VERSION` row must have changed in the bump commit itself (it does, by the script).
- **JDK line:** record both full suites on one JVM line. Preferred: the default (JDK 17). If one crashes (exit 255, `Gradle Test Executor N finished with non-zero exit value 255`, an ~8 GB `core.*.dmp` in the repo root), read the `javacore.*.txt` stack, delete the core dump and `javacore`, `Snap`, `jitdump` files by name (never `git clean`), `./gradlew --stop`, rerun once, record both honestly; if it recurs run BOTH suites with `-PjavaVersion=21` (`build.gradle.kts:234` reads the property) and say so; this is a user judgement item at the bundle, as in gg6 `[CITED: 38-APPROVAL-BUNDLE.md item (2); memory openj9-mockito-test-jvm-crash]`.
- Docker is the colima `vz` profile (`colima status -p vz` read "running", 8 CPU / 16 GiB per the memory note); never a bare `colima start`.
- A crashed fork writes no XML, so a total below 2425 with 0 failures is a crash, not a pass.
- Do not run `spotlessApply` or `spotlessCheck` as a gate: 46 violations pre-exist and this phase edits no Java.
- Not re-run and stated as such: `integrationTest` (needs a Connect stack; Phase 39 recorded one run of `ClickHouseSinkConnectorIntegrationTest`, FORK-DELTA item 15) and the first GitHub runs of the Phase 39 workflow pins, which are the U1 and U2 runs.

## 6. FORK-DELTA final pass and raw numbers (settle d)

### 6.1 Numbers to measure once, and how
Every scope below contains no file Phase 44 may edit (`src build.gradle.kts gradle .github`, `src/main`), so doc-only commits cannot move them. Measured today on `HEAD` = `b417c2c2` `[VERIFIED: command]`:

| Scope | Command | Result |
|-------|---------|--------|
| wider, upstream to gg7 | `git diff --shortstat v1.7.0 HEAD -- src build.gradle.kts gradle .github` | 345 files changed, 42439 insertions, 1221 deletions |
| narrower | `git diff --shortstat v1.7.0 HEAD -- src/main` | 43 files changed, 3183 insertions, 294 deletions |
| gg6 to gg7, wider | `git diff --shortstat v1.7.0_gg6 HEAD -- src build.gradle.kts gradle .github` | 34 files, 2826 insertions, 112 deletions |
| gg6 to gg7, `src/main` | `git diff --shortstat v1.7.0_gg6 HEAD -- src/main` | 8 files, 194 insertions, 22 deletions |
| gg6 to gg7 by directory | same command per path | `src/test` 18 files 2470/28; `src/integrationTest` 2 files 155/55; `.github` 5 files 6/6; `gradle` 1 file 1/1; `src/testFixtures` and `build.gradle.kts` unchanged |
| gg6 check | `git diff --shortstat v1.7.0 v1.7.0_gg6 -- src build.gradle.kts gradle .github` and `-- src/main` | 331/39665/1161 and 40/2989/272, equal to the table's gg6 rows (the method reproduces the published figures) |

**How to refresh them once:** the tag `v1.7.0_gg7` does not exist before publication, and a local one must not be created. So: measure with `HEAD` of the release tip at the last commit that touches `src`, `build.gradle.kts`, `gradle` or `.github` (here already the case: none of Phase 44's commits is in those scopes), print the figures into the table's new row "after gg7: `v1.7.0` to `v1.7.0_gg7`", and write the reproduction commands with the tag name `v1.7.0_gg7` and the sentence gg6 used (measured once on the release tip, before the release workflow creates the tag; the merge commit's trees equal its second parent's because the merge is a `--no-ff` merge onto an ancestor, so the tag's commands reproduce the figures). Re-run both `HEAD` commands on the final tip and require equal output. After publication the orchestrator can run the `v1.7.0_gg7` forms and confirm.

### 6.2 Edits that the final pass must make (each found by reading the file this session)
1. **Line 138 (false claim):** "`Processing.java` and `KeyToValue.java` ... Byte-identical to upstream `v1.7.0` (`git diff --quiet v1.7.0 HEAD -- <path>` exits 0 for each). `Processing` takes upstream's five-argument constructor". Today `KeyToValue.java` is identical and `Processing.java` is not (`[VERIFIED: command]` `git diff --quiet v1.7.0 HEAD -- .../Processing.java` is non-zero). Rewrite: `KeyToValue.java` byte-identical; `Processing.java` takes upstream's five-argument constructor and differs from upstream by the single RSK-01 line (row at line 62 says it differs "for the first time"). Use tag pairs in the command.
2. **Line 139:** "33 of the 63 files present at `v1.7.0` are byte-identical ... Of the 30 that differ, 14 ... import order ... and 16 carry fork logic". Measured with the file's own method (strip `import` lines and blank lines, compare): gg6 33 / 14 / 16 / 10 new (reproduces the published numbers); **gg7 30 byte-identical, 14 import-only, 19 modified with fork logic, 10 new** (63 files at `v1.7.0`, 73 now). So: 30 byte-identical of 63; of the 33 that differ, 14 import-order only and 19 carry fork logic. `[VERIFIED: python over git show, this session]`. The three files that moved from byte-identical to fork logic since gg6: `Processing`, `StateProvider`, `KeeperStateProvider`.
3. **Line 188:** "of the 40 `src/main` files that differ ... 14 import-only ... The other 26 carry fork logic: sixteen modified ... and ten new files": gg7 values are 43 differ, 14 import-only, 29 carry fork logic (19 modified, 10 new). Add the sentence "The same recount at `v1.7.0_gg6` gives 33 byte-identical files, 14 import-only and 26 with fork logic, so since then `Processing`, `StateProvider` and `KeeperStateProvider` moved from byte-identical to fork logic and no file was added", and replace the `HEAD` command with `git diff --quiet v1.7.0 v1.7.0_gg7 -- <file>`.
4. **Line 184 (Phase 43 WR-03):** the figures are right as tag-to-tag (measured: `git diff --shortstat v1.7.0_gg4 v1.7.0_gg5 -- .github` = 6 files 616/92, `src/test` 5 files 1486/25, `src/integrationTest` 1 file 23/1, `build.gradle.kts` and `gradle` empty) but the command says `HEAD`. Change the command to `git diff --shortstat v1.7.0_gg4 v1.7.0_gg5`, add the `-- <path>` form. Then verify no backticked command in the raw-numbers section or in rows 138 and 188 contains `HEAD`: the remaining `HEAD` hits are line 146's description of how the gg4 and gg5 figures were measured (history) and line 291's gate recipe (`verify-merge-invariants.sh 54e63648 HEAD`), both acceptable if the reviewer is told.
5. **New gg7 paragraph** in the raw-numbers prose: "From `v1.7.0_gg6` to `v1.7.0_gg7` the delta against upstream grew by ..." with the by-directory figures above and the eight `src/main` files named, and the table row plus the two reproduction commands.
6. **Intro paragraph (line 5) and "Anyone moving a running connector" paragraph (line 7):** add `v1.7.0_gg7` (the tree `VERSION` named at the merge; the tag and release are created by the publish run) and a pointer to `docs/ROLLOUT-v1.7.0_gg7.md`; keep the older pointers (gg6 note, gg5, ...).
7. **"For the next syncer":** add the gg7 paragraph in the shape of the gg6 one: published from `sync/v1.7.0` by a non-force merge into `custom_build`, rollback tag `pre-v1.7.0_gg7` on `72e8ccff` (the gg6 release), what it carries (F-1, F-2, RSK-01..04, gson 2.15.0, `setup-gradle` 6.4.1), upstream state at the time (`main` `9bed6f79`, no tag past `v1.7.0`, #861/#855/#857 open and touching the same files), the note and CHANGELOG pointers, the archive compare baseline `v1.7.0_gg6`.
8. Known costs: items 13, 14, 16, 17, 24 are already written (Phases 40 to 42); the pass checks them against the note's rows and the item numbers cited by the new text, adds the gg7-closes-gg6-known-limits statement where the gg6 items are described, and keeps "FUT-05 stays open" in item 24. Item 12 reads "closed unmerged (the 3.x pin stays open)": unchanged. FORK-DELTA forward references: `grep -n "Phase 4[0-4]\|Phase 38"` printed none, so none go stale.
9. Dependency rows: Phase 39 already changed the gson row; re-read it against the measured `2.14.0 -> 2.15.0` compare line.

## 7. RLS-03: operator-visible effects for `docs/ROLLOUT-v1.7.0_gg7.md` (settle: content)

### 7.0 Label vocabulary
RLS-03 and the ROADMAP allow exactly three labels: `measured`, `read from the code`, `derived`. The F-2 evidence record carries a fourth word, `inferred`, for the progress-header claim, with the instruction "carry it as `derived (mechanism, not measured)` or measure it" `[CITED: 41-COMMAND-EVIDENCE.md E-09, 41-03-SUMMARY.md]`. Recommended: three labels in the preamble, `derived` defined as "follows from a stated mechanism; nothing ran for it" and the progress-header row written `derived (mechanism, not measured)` with its evidence (the 3030 ms and 2056 ms probes stated as what they showed). Do not re-measure: it needs a server version or insert shape that streams progress headers during a body insert, which E-06 did not find; the user may ask for it (Open Question 11). Do not reuse the gg6 labels `measured in research (not re-run)` or `derived from Kafka consumer-group semantics (not measured)`: the gg7 note restates no gg6 row except to correct or supersede it.

Preamble requirements carried from the gg6 note and its 43 findings: no host, connector, cluster or production name (angle-bracket values only); no planning, requirement, review or evidence identifier in published text (FUT-05 is the one deliberate exception, named in row C2 of the gg6 note and in the gg7 RSK-03 section); every row has an `Applies when` value (`client V2`, `client V1`, `either client`, `exactlyOnce=true`, `bufferCount 0` and so on). The gg6 review's 28 warnings were mostly scope errors: a client-V2 fact stated for every connector, an "unbuffered" claim without its condition. Fix the scope in the draft.

### 7.1 Inventory (sources are the committed Phase 39-43 records; the first column is a proposed row ID)

| # | Effect (operator-visible) | Label | Applies when | Source |
|---|---------------------------|-------|--------------|--------|
| **F-1** | | | | |
| F1 | `ClickHouseWriter.stop()` closes its helper client `chc` once, after it stops the table-sync executor and waits up to 5 s; a second `stop()` or one after a failed `start()` closes nothing more; every failing exit of `start()` closes the client it opened | measured (a Docker-free counting seam opens and closes: `WriterHelperClientClosingUnitTest`); the 5 s wait read from the code | either client | FORK-DELTA row line 39, item 13; `40-COMMAND-EVIDENCE.md` E-06, E-07 |
| F2 | On client V1 every insert batch builds one helper client (and with it a V2 client) in `getMutationRequest` and now closes it right after reading its node | measured (seam); the per-batch construction read from the code | client V1 insert path | line 39, item 13 |
| F3 | With `exactlyOnce=true` `KeeperStateProvider.close()` closes the client its constructor built; `ProxySinkTask.stop()` runs writer, MBean and state provider in sequence, a throw in one still runs the others, the first exception reaches the caller and later ones are suppressed; state read and write paths are byte-identical | measured (`KeeperStateProviderClosingUnitTest`, `ProxySinkTaskStopUnitTest`); byte-identical read from the code (diff empty) | `exactlyOnce=true` | line 38, item 13; E-09, E-10 |
| F4 | What an operator gains: fewer leaked HTTP connection pools on workers that restart or rebalance tasks often | derived (mechanism, not measured: no worker was observed leaking or not) | either client | gg6 note "Known limits" first bullet; item 13 |
| F5 | Residuals, not fixed: a failing `KeeperStateProvider` constructor on client V1; a failing `ClickHouseHelperClient` constructor after building its V2 client; a second `start()` on one writer; `ClickHouseSinkTask.stop()` without `try/finally` around `deliveryStrategy.stop()`; the 5 s wait can end with a refresh still running on a closed client | read from the code | as named | item 13 |
| **F-2** | The 13 claims of `41-COMMAND-EVIDENCE.md` E-09, in that order, client V2 scope first | as E-09, except the progress-header row | | E-09 table |
| T1 | Scope: client V2 only; a connector that does not set `client_version` runs client V1 (default `V1`) and nothing here applies; the V1 library's own `socket_timeout` of 30000 ms stays its only read bound | read from the code (default and selection); the 30 s bound measured (`41-RESEARCH.md` Probes 4, 5) | client V1 vs V2 | E-09 row 1 |
| T2 | On client V2 with `async=false`, an insert into a server that stops answering fails after `clickhouseClientInsertTimeoutMs` (default 4 minutes) with a retriable error; before it blocked the task | measured (E-03 red, E-06/E-08 green; Probe 4) | client V2 | E-09 row 2 |
| T3 | Retriable under `errors.tolerance=none` and `all`; the task is not failed and no DLQ record is written | measured (`stalledV2InsertThroughPutIsRetriableWithNoDlqRecord`, six cases) | client V2 | E-09 row 3 |
| T4 | One request per stalled insert (no client-side retry); the option is not sent to the server | measured | client V2 | E-09 row 4 |
| T5 | A quiet insert that legitimately runs longer than the setting now fails and is redelivered; the server may still commit it | measured (Probe 8: failed at 1003 ms, row landed; E-06: 3000 ms value failed at 3030 ms, 4000 rows committed) | client V2 | E-09 row 5 |
| T6 | Progress headers: a connector-shaped body insert sent no header while the server worked and one at the end, so the per-insert timeout bounded it; an `INSERT ... SELECT` through the query path streamed a header about every 0.2 s and a 1000 ms `socket_timeout` did not end it; whether a body insert that streams headers escapes the bound was not measured | derived (mechanism, not measured); the two probes quoted as what they showed | client V2 | E-09 row 6 (label `inferred`) |
| T7 | With `exactlyOnce=false` that redelivery inserts the batch again: a duplicate | derived (Probe 8 plus the redelivery path read from the code) | client V2, `exactlyOnce=false` | E-09 row 7 |
| T8 | With `exactlyOnce=true` the retry reuses the `insert_deduplication_token`; whether the server drops the repeat depends on the table's deduplication settings, not measured | token read from the code; server half derived | client V2, `exactlyOnce=true` | E-09 row 8 |
| T9 | An operator `socket_timeout` in `jdbcConnectionProperties` keeps its value for every request, inserts included, and the per-insert value is not set; `socket_timeout=0` keeps inserts unbounded | measured (`InsertStallBoundPinUnitTest` guards, `InsertSocketTimeoutOptionUnitTest`; E-07 shows the guards fail when the per-insert set is unconditional) | client V2 | E-09 row 9 |
| T10 | Remedies for a legitimately slow insert: raise `clickhouseClientInsertTimeoutMs` (minimum 100) or set `socket_timeout` | read from the code | client V2 | E-09 row 10 |
| T11 | Unbounded cases: a write-side stall (peer stops reading the body); client V1 inserts (three `future.get()` sites, V1 library 30 s read bound; V1 write-side stall not measured); `DESCRIBE`, version query, table refresh, `KeeperStateProvider` state queries stay unbounded unless the operator sets `socket_timeout` | write-side and V1 read stall measured (Probes 6, 4, 5); the non-insert list read from the code (and measured Probes D3, D4 for the operator bound) | as named | E-09 rows 11-13; item 14 |
| T12 | The help text of `clickhouseClientInsertTimeoutMs` still presents the setting as a hard limit "for a single ClickHouse insert response"; with `async=false` it is carried by the per-insert `socket_timeout`, which resets on each byte received | read from the code | client V2 | item 14 residual |
| T13 | **Supersedes the gg6 note's statement** that the four-minute timeout "has no effect while `async=false`" (gg6 note, "Known limits", second bullet; CHANGELOG gg6 Known Issues): that was true of gg6 and is closed on client V2 in gg7; the client V1 half stays as stated and is now measured for the library default | read from the code | client V2 | `41-COMMAND-EVIDENCE.md` E-09 `published_statement_correction_owner` |
| **RSK** | | | | |
| R1 | `Processing.doLogic` takes the consumer's topic from `SinkRecord.originalTopic()` (line 137). Only the in-memory inserted-offset map reads it, so with `reportInsertedOffsets=true` under an injective topic rename the Direct path (unbuffered, `bufferCount` 0) now reports offsets under the consumer's topic and `onPartitionRemoved` clears them; before it reported nothing for the renamed topic. With no topic-changing SMT nothing changes | measured (`OriginalTopicInsertedOffsetsUnitTest`, `42-COMMAND-EVIDENCE.md` E-02, E-03) | `reportInsertedOffsets=true`, topic-renaming SMT | line 62, item 16 |
| R2 | Unchanged by R1: the exactly-once state key `<transformed topic>-<partition>`, the KeeperMap row `(key, minOffset, maxOffset, state)`, table routing, the BEFORE/AFTER sequence; `KeeperStateProvider` is untouched by RSK-01 | read from the code; pinned by `stateKeyAndFlagSequenceStayOnTheTransformedTopicUnderAnInjectiveRename` (measured) | `exactlyOnce=true` | item 16 |
| R3 | Limits (RSK-04): exactly-once, the in-memory state machine and `reportInsertedOffsets` are supported only for an injective rename and a partition-preserving SMT; a many-to-one rename shares one state record and the Direct path reports the shared maximum plus one for the first record's topic (measured: a topic at offset 10 reported at 110); a partition-changing SMT keeps the transformed partition in the key and `onPartitionRemoved` does not clear it; do not enable `reportInsertedOffsets` with them. Whether Connect commits an offset above the consumer's position is derived from consumer-group semantics | measured (`limitManyToOneRename...`, `limitPartitionChangingSmt...`); the Connect half derived | as named | item 16 |
| R4 | `Utils.handleException` throws a `RetriableException` that is the exception or anywhere in its cause chain as retriable (rule after the server-error rule, before the `DataException` rule; own instance rethrown, else wrapped once; the walk stops at the first repeated cause). A writer-thrown `RetriableException` (the `async=true` insert timeout, the DDL-propagation timeout, a `DataException` wrapping one) is now retriable under both tolerances: a batch that went to the DLQ under `all` is redelivered with no retry budget, and under `none` the task no longer fails | measured (`RetriableExceptionPassThroughUnitTest`, `HandleExceptionCauseCycleUnitTest`; the pre-change changed-row list and the measured diff, `42-COMMAND-EVIDENCE.md` E-05, E-07) | either client | line 64, item 17 |
| R5 | A `RetriableException` anywhere in the chain overrides the server-code classification: a `RetriableException` over a non-retriable code (for example 60) is now retriable | measured (the two `..._OVER_NON_RETRIABLE_SERVER_CODE` rows) | either client | line 64, item 17 |
| R6 | Updates gg6 note row C3: the two retriable `DataException` behaviours are now pinned by `WrappedDataExceptionClassificationPinUnitTest` (Phase 39) and the RSK-02 change adds the `RetriableException` case; a decision on changing either is still open | measured (pin); open decision | either client | item 17 |
| R7 | New WARN at task start: `additionalRetriableErrorCodes lists {}, which the connector does not treat as transient. A batch that fails with such a code is redelivered with no retry budget and outside errors.tolerance and the dead letter queue, so a permanent error blocks the partition until the setting is changed.` The vetted set is `Utils.DEFAULT_RETRIABLE_ERROR_CODES` (15 codes) plus 216; one WARN per task start naming the unvetted codes (sorted), before the connection step so also when ClickHouse is down; start-up still succeeds; the WARN carries no record data; a transient code logs nothing; nothing validates the codes or budgets retries | the text and the vetted set read from the code (quoted above); behaviour measured (`UnvettedRetriableCodesWarnUnitTest`, `UnvettedRetriableCodesWarnLiveTest`, `42-COMMAND-EVIDENCE.md` E-08, E-09) | any connector that sets the setting | FORK-DELTA item 24; `ClickHouseSinkTask.java` diff |
| R8 | **"FUT-05 stays open"** (verbatim sentence the Phase 43 handoff requires in this section): no retry budget, and no setting removes a code from the built-in set | read from the code | any | `43-DISPOSITION-LEDGER.md` Handoff item 1; item 24 |
| **Dependencies, identity, rollback** | | | | |
| D1 | `gson` 2.14.0 -> 2.15.0 in the shipped archive (`LinkedTreeMap$1` removed); no other third-party jar differs | measured (compare VERSION table: `com.google.code.gson:gson 2.14.0 -> 2.15.0`; 17 other version lines equal) | this release | section 4 |
| D2 | `setup-gradle` 6.4.1 (CI only); no vulnerability is claimed to be closed (GHSA-chpw-2mqm-95cc does not apply) | read from the code (workflow pins) | CI only | ROADMAP Phase 39 Note |
| D3 | The release identity (JMX `version=`, client name) reads `v1.7.0_gg7`; update matchers that spell out the old value | read from the code (`Version.ARTIFACT_VERSION` inlined in `ClickHouseHelperClient`, `SinkTaskStatistics`); both differ in the compare (measured) | this release | gg6 note row E3 shape |
| D4 | Archive: 7933 -> 7932 inner entries; local rebuild sizes 13558633 -> 13564144 bytes (the published gg6 asset is 13558692, so state sizes as local rebuilds only) | measured | this release | section 4 |
| D5 | Rollback: tag `pre-v1.7.0_gg7` = `72e8ccff` = tag `v1.7.0_gg6`; redeploy the gg6 archive; rolling back reverses F1-F5, T1-T13, R1, R4-R7 and gson | rebuild and compare: measured on the gg6 side (this research rebuilt it); reversal read from the code | this release | section 8.4 |

The note must not state facts about the `v1.7.0_gg7` release object (asset size, digest, tag SHA) before it exists. State what `release.yaml` does; keep measured release facts for a local-tail record after U4 (`44-RELEASE-RECORD.md`). Since gg6's publish run proved the publish step, the note may say the release and its tag are created by the publish run, citing FORK-DELTA item 22, but not give any gg7 value. `[CITED: 38-RESEARCH.md section 3.3]`

### 7.2 Corrections to published text (decision, Open Question 10)
The gg6 note's "Known limits" bullets for `stop()` (F-1) and the insert timeout (F-2) are accurate for gg6. Recommended: **do not edit the gg6 note or the gg6 CHANGELOG section**; the gg7 note carries a short table "What this release closes from the gg6 note" (F-1, F-2, F-3 harness, C3 now pinned) and rows F4 and T13 say so. This matches the committed instruction that the published text "is not edited" `[CITED: 41-COMMAND-EVIDENCE.md E-09]`. If the orchestrator prefers edits (the Phase 43 handoff item 3 allows one), the correction notice at the top of the gg6 note must be extended to name each row it touches, and the edited note goes into the reviewer's brief. One more stale sentence exists in the gg6 note's Rollback section ("whether a publish-mode run of it does so is the open condition of `FORK-DELTA` item 22"): item 22 is now closed; leave it as history or include it in the same notice, and tell the reviewer which was chosen so they do not file it as a new finding.

## 8. RLS-03 review, rollback rehearsal and the brief (settle g)

### 8.1 Who and when
The orchestrator spawns a fresh-context `gsd-code-reviewer` with its Agent tool, after the note, CHANGELOG and FORK-DELTA are written and before the allow-list and gates (so "allow-list last" stays literal) and long before any push gate. An executor's inline review does not count (recurred in gg3 Phase 24 even with a plan for it). `[CITED: memory gsd-phase15-review-gate-gotchas; STATE.md line 273]`

### 8.2 Scope and brief (round 1 names these)
- Files under review: `docs/ROLLOUT-v1.7.0_gg7.md`; the `1.7.0_gg7` section of `CHANGELOG.md`; `docs/FORK-DELTA.md` (whole file; diff base **`72e8ccffe05ff6dffb14098d0b8c0f67aaa6a236`**, the gg6 merge); and the published notes Phase 43 edited: `docs/ROLLOUT-v1.7.0_gg1.md` and `docs/ROLLOUT-v1.7.0_gg6.md` (commit `965e646f`; FORK-DELTA commits `870fe9d8` and the ledger commit `e72447b5`, plus review fixes `d77da0f9`, `5c6c40e1`). `[VERIFIED: git log --format='%h %s' 72e8ccff..HEAD -- docs CHANGELOG.md]`
- Evidence map: `44-COMMAND-EVIDENCE.md`. Completeness checklists: `41-COMMAND-EVIDENCE.md` E-09 (13 claims; every one must appear with the same label strength), FORK-DELTA rows at lines 38, 39, 50, 62, 64 and items 13, 14, 16, 17, 24, `42-COMMAND-EVIDENCE.md` E-05 to E-09, and `43-DISPOSITION-LEDGER.md` "Handoff to Phase 44".
- Tell the reviewer what is deliberate (Phase 43 handoff item 2): FUT-05 appears in gg6 row C2 and in the gg6 correction notice by the user's choice, and in the gg7 RSK-03 section as "FUT-05 stays open"; the gg6 section "The bypass of errors.tolerance" (about line 121) describes the same redelivery without the ID by the user's choice. Tell them which option of 7.2 was chosen. Tell them the F-2 progress-header row is `derived (mechanism, not measured)` by design, and that every claim about client V1 inserts is scoped to client V1 on purpose.
- Mandated confirmations, one verdict each (section `## Mandated confirmations (RLS-03)`): every row of the inventory against its source and label (no row stronger than its evidence); commands, numbers and versions against the evidence; each row's `Applies when` against the code path it describes (client V1 vs V2, `exactlyOnce`, `bufferCount`, `reportInsertedOffsets`); the F-2 13 claims complete and ordered; the rollback block and its target `pre-v1.7.0_gg7`; no fact about the `v1.7.0_gg7` release object; no advice that empties or destroys data (no `TRUNCATE`, `DROP`, `DELETE FROM`); FUT-05 stays open stated in the RSK-03 section; the WARN text verbatim; FORK-DELTA stale rows (138, 139, 188 and the `HEAD` commands) and the new gg7 paragraphs; released CHANGELOG sections byte-identical; no host, connector or planning identifier in published text; the CHANGELOG heading date.

### 8.3 Rounds, headings, dispositions
- **Rounds:** repeat until one finds nothing, at most three unless the user authorises another. gg6 needed 12 rounds, the user chose a further round after each of rounds 3 to 11 and chose "fix and close" after round 12 with the last two fixes unreviewed `[CITED: 38-03-SUMMARY.md deviations 1, 2]`; gg5 stopped after round 3 with two Info findings and the verifier recorded an override. Plan for the same question: the plan's step after round 3 is a user decision (accept, authorise another, hold), and `44-review-check.py` takes the cap as `--max-rounds` (default 12) so no edit is needed per round.
- **Reviewer fix text is checked, not applied:** in gg6 eight proposed fixes were wrong or too strong (a class named for a log line it does not write, a claim for every client, an exhaustive "either or"). Check every key, query, command and scope claim against code before applying `[CITED: memory gsd-phase15-review-gate-gotchas; 38-03-SUMMARY.md deviation 6]`.
- **Heading rule (recurring failure):** every finding sits under `### <CR|BL|WR|IN>-NN: <title>`, never deeper; each round ends with `round: <n> findings: <k>`; frontmatter totals equal the number of finding headings and `rounds:` equals the rounds run; IDs continue per prefix and are never reused. Spell this out in the brief of every round that appends, or the disposition step silently records fewer rows (it recorded 6 of 13 once). The Phase 43 review frontmatter shape is the reference: `findings: critical/warning/info/total`, `status: issues_found|clean`. `[CITED: memory gsd-phase15-review-gate-gotchas, gsd-code-review-fix-gotchas; 43-REVIEW.md frontmatter]`
- **Dispositions:** `44-REVIEW-DISPOSITION.md` with one row per finding, `open: 0`, every fixed row starting with a commit SHA of this phase, every rejected row with its reason, every deferred row with reason, owner and re-open trigger under `## Carried to the milestone close`. The disposition step is the `code-review-disposition.md` bash fence extracted to a script and run with `bash` (`PHASE_DIR=<abs> PHASE_NUMBER=44`) `[CITED: memory gsd-phase25-orchestration-gotchas; ~/.claude/gsd-core/workflows/execute-phase/steps/code-review-disposition.md exists]`.
- **Docs frozen:** after the last fix the owning checks pass again, no evidence verdict reads pending, and `git diff --name-only <phase-start> HEAD -- . ':(exclude).planning'` lists exactly `VERSION`, `CHANGELOG.md`, `docs/FORK-DELTA.md`, `docs/ROLLOUT-v1.7.0_gg7.md` (and `docs/ROLLOUT-v1.7.0_gg6.md`/`gg1` only if 7.2 chose edits). Time-scoped verify clauses (a lesson of gg6 plans 38-01 to 38-03): anchor every clause to a SHA or a literal, never to "unchanged since the phase start" in a plan whose later tasks change it.

### 8.4 Rollback rehearsal (Phase 16 lesson: test on the publication tip)
The note's rollback section restores the tree of `pre-v1.7.0_gg7` (= `72e8ccff`) as one commit on a new branch (`git switch -c rollback-v1.7.0_gg7`, `git read-tree -u --reset pre-v1.7.0_gg7`, `git commit -m "Roll back v1.7.0_gg7"`, `git diff --quiet HEAD pre-v1.7.0_gg7 && echo ok`), forward-only, never a force-push; the `gradlew.bat` line and the `--attr-source` switch are re-measured, not copied. Rehearse the extracted block unchanged on the dry-run merge `D` of the release tip onto `72e8ccff` in a detached throwaway worktree: it must print `ok`; the branch has `VERSION` `v1.7.0_gg6`, no copy of the gg7 note, a tree equal to the tag; `git revert <rollback-commit>` restores the tree; then `git branch -D rollback-v1.7.0_gg7` and remove the worktree. Redeploy target: the `v1.7.0_gg6` archive (a release asset exists for it, id 430511927, `github-actions[bot]`; a rebuild from the tag has a different digest but compares equal entry by entry, which this research's baseline rebuild supports: 7933 entries, 13558633 bytes vs 13558692 asset). Rolling back reverses F-1, F-2 (inserts unbounded again on client V2), R1, R4 to R7 and gson 2.15.0 (back to 2.14.0). Nothing in the data format changes. `[CITED: docs/ROLLOUT-v1.7.0_gg6.md Rollback; 38-05-SUMMARY.md]`

## 9. Plan split with explicit gates, and the user gate (settle e)

### 9.1 Plans (gg6 shape, adapted; all local; waves are sequential)

| Plan | Wave | Autonomous | Content | Requirements |
|------|------|------------|---------|--------------|
| 44-01 | 1 | yes | **Pre-flight** (section 1 block, Docker, `latest` digest and version, `git status` clean except the two `.planning` files); copy and adapt helpers; local tag `pre-v1.7.0_gg7`; `bump_version.py gg` and ONE commit `{VERSION, fork-anchors.txt}`; `clean generateVersionFile` agreement; baseline selection; anchors `rows=190 bad=0`; CHANGELOG `1.7.0_gg7`; FORK-DELTA final pass (6.1, 6.2) with raw numbers once; hygiene scan | RLS-01 |
| 44-02 | 2 | yes | `docs/ROLLOUT-v1.7.0_gg7.md` from the inventory 7.1, with an evidence map entry per row; cheap local re-measures only (the archive builds of section 4, the WARN text read from source, the rollback block dry run); note checker (rows, labels, forbidden words, scope column) | RLS-03 (part) |
| 44-03 | 3 | **no (orchestrator)** | Reviewer rounds (8.1-8.3), fixes, dispositions, docs-frozen proof. After round 3 non-empty: ask the user | RLS-03 |
| 44-04 | 4 | yes | **Allow-list generated last** (section 4) and committed; gates on `H` (section 5): testUnit Docker-free, two `--rerun` suites on one pinned `latest`, anchors mirror, cumulative invariants once | RLS-02, RLS-04 (gates) |
| 44-05 | 5 | yes | Rehearsal: dry-run merge `D`, rollback block on `D`, "no operator document tells anyone to empty a table", re-run of the section 1 block, date comparison; `44-MERGE-MESSAGE.txt`; approval bundle; four user handoff files; orchestrator runbook; local close record with the exact ROADMAP/REQUIREMENTS wording changes | RLS-04 (rehearsal) |
| tail | 6 | **orchestrator inline, user-gated** | G1 to G4 below; the readings and the local release record `44-RELEASE-RECORD.md`; close | RLS-04 (publication) |

A red anywhere in 44-04 is recorded with its evidence and returned: this phase never fixes code, a test or a workflow, and never edits an allow-list row to turn a red green. A defect found goes to its phase as a gap (STATE decision, Phase 29 D-05).

### 9.2 Gates (G) and what each authorises
| Gate | Where | Who | Condition |
|------|-------|-----|-----------|
| G0 | plan 44-01 start | executor | section 1 block as expected; Docker up; digest `739e099a...`; else stop and report |
| G1 | after 44-03 | user (only if round 3 is not empty) | accept the open Info findings and fix, authorise another round, or hold |
| G2 | after 44-04 | orchestrator | allow-list PASS, testUnit 1826/2, two suites equal totals (expected 2425/18) on one digest, anchors `rows=190 bad=0`, invariants `result=PASS`, tree outside `.planning` equals the gated head |
| G3 | approval bundle (after 44-05) | **user's direct "go"** | authorises U1 and U2 only |
| G4 | after a green U1/U2 | **user's own direct "publish" message** | authorises building `M` and the push of `M` to `custom_build` (U3) |
| G5 | after `M`'s single green run | user | authorises U4 (the publish dispatch) and then U5 |

**Questions to put to the user in the approval bundle (G3).** Each answer is recorded verbatim with who received it (the user's own message to the orchestrator; a relayed approval is refused):
1. *(Open Question 9)* "Do you run every origin write yourself (scratch push, `custom_build` push, dispatches, tag push, branch delete), as for gg6, or do you allow the orchestrator to run the pushes on your direct word?" Default recommended: you run them.
2. "Go for U1 and U2: push `X0` to the scratch branch `sync/v1.7.0` (this publishes N commits that are on no origin ref yet, the `.planning` history included; deleting the branch later does not unpublish them) and dispatch `release.yaml` from that branch as a dry run?" Authorises U1 and U2, nothing more. The bundle prints the live count: today 195 commits are on no origin ref and 37 files outside `.planning` differ from the published tip.
3. "JVM record: both full suites ran on JDK 17 / on JDK 21 (after N crashes). Does that satisfy you?" (only if a crash happened).
4. "The review ended with round N; findings still open or unreviewed: ... fix and accept / authorise another round?" (only if G1 was reached).
5. "CHANGELOG heading date is D; UTC today is D2. Keep, or hold for a date-fix commit?" (asked again before U3).
6. "Do not merge PRs #21 and #22 on origin before `M` lands (a merge moves `custom_build` and diverges `X0`). GitHub should mark both merged when `M` lands; you may close them by hand if not." (information; no answer required.)
7. "`integrationTest` was not re-run in this phase (needs a Connect stack; the Phase 39 run on the same harness code is the record). Overrule?" (an overrule re-opens 44-04 with nothing pushed).
8. "The F-2 progress-header claim is carried as `derived (mechanism, not measured)`; do you want it measured first?" (Open Question 11; may be settled earlier).

**G4 "publish":** "Publish now: the orchestrator builds `M` (merge `X0` onto `72e8ccff`, tree equal to `X0`), and you push it to `custom_build` without force and wait for its one green `tests.yaml` run?" Before building, re-check origin (`custom_build` still `72e8ccff`, else void and contingency C-1), the date, `X0` ancestry and that no non-`.planning` path differs from the gated head.
**G5:** after exactly one green `tests.yaml` run on `M`, still one two minutes later, no `release.yaml` run on `M`, `v1.7.0_gg7` absent: "Launch `release.yaml` from `custom_build` with `dry_run` not ticked (run-name must read `Release custom_build (publish)`)". It creates the tag and the release; then `pre-v1.7.0_gg7` is pushed by name and the scratch branch deleted.

### 9.3 Publication sequence (orchestrator and user)
| # | Who | Step |
|---|-----|------|
| 0 | O | read-only pre-checks (section 1 block) |
| 1 | U | **U1:** `SHA=$(git rev-parse sync/v1.7.0)` equals the gated tip; `git diff --quiet <gated sha> "$SHA" -- . ':(exclude).planning'`; `git ls-remote origin \| grep "$SHA"` empty; `git ls-remote --heads origin sync/v1.7.0` empty; `git push origin "${SHA}":refs/heads/sync/v1.7.0` (brace the variable: zsh reads `$M:refs` as a modifier) `[CITED: memory zsh-var-colon-modifier-in-git-refspec]`; wait for the one `tests.yaml` run; there is no `timeout` binary, poll `gh run view` in a bounded loop |
| 2 | U | **U2:** dispatch `release.yaml` from the scratch branch (UI "Use workflow from" the scratch branch, never `custom_build`; or `gh workflow run release.yaml -R ne1r0n/clickhouse-kafka-connect --ref sync/v1.7.0 -f dry_run=true`); report run id, `headSha`, conclusion, every step conclusion, the baseline notice naming `v1.7.0_gg6` and the gg7 list, the compare notice `exit 0, 0 unallowed, 0 stale rows`, artifact `archive-compare-report` last line `RESULT: PASS`, no draft or tag left |
| 3 | O | the user's own "publish"; origin unchanged; build `M` detached: `git worktree add --detach /tmp/<new>/cb 72e8ccffe05ff6dffb14098d0b8c0f67aaa6a236`, `git --attr-source="$(git hash-object -t tree /dev/null)" merge --no-ff -F 44-MERGE-MESSAGE.txt <X0>`, `git update-ref refs/gsd/release-m-v1.7.0_gg7 "$M"`; checks: two parents `72e8ccff` and `X0`, `git diff --quiet HEAD <X0>`, subject `Merge sync/v1.7.0: release v1.7.0_gg7`, no `.gsd-ws-arg`, `VERSION` reads `v1.7.0_gg7`, the note's rollback block prints `ok` |
| 4 | U | **U3:** `git push origin "${M}":refs/heads/custom_build` (no `--force`, `--tags`, `--all`), from the repository that built `M` (it exists only there); exactly one `tests.yaml` run for `M` |
| 5 | O | read: `gh api 'repos/ne1r0n/clickhouse-kafka-connect/actions/runs?head_sha=<M>' --jq '[.workflow_runs[] \| [.name,.path,.event,.conclusion]]'` filtered by `.github/workflows/tests.yaml`: one run, success, still one 120 s later; no `release.yaml` run; `git ls-remote origin refs/heads/custom_build` equals `M` |
| 6 | U | **U4:** dispatch `release.yaml` from `custom_build`, `dry_run` unticked (UI, or `gh workflow run release.yaml -R ne1r0n/clickhouse-kafka-connect --ref custom_build`); report as U2 plus the `Publish release` step conclusion and the release URL |
| 7 | O | read: release (id, `tag_name`, author, draft, assets with uploader), `git/ref/tags/v1.7.0_gg7` (object type `commit` expected, lightweight), the run's jobs; write `44-RELEASE-RECORD.md` (the local tail, unpublished) |
| 8 | U | **U5:** `git push origin refs/tags/pre-v1.7.0_gg7` by name; `git push origin --delete refs/heads/sync/v1.7.0`; PRs #21 and #22 read back (expected MERGED) |
| 9 | O | final reads (tags, heads, PR states, release, upstream); STATE and ROADMAP/REQUIREMENTS wording; `git fetch origin` and a no-change merge of `origin/custom_build` into `sync/v1.7.0` is the optional local tail (never a rebase); `.planning` commits after `X0` stay local |

**What a green dry run proves:** the full-history checkout, the baseline worktree build with the tag's wrapper, the strict compare, the report upload, a draft with exactly the archive created, checked and deleted, no tag for the draft; it does not prove the publish step (skipped) `[CITED: 38-RESEARCH.md section 6.2]`. Because gg6's publish run succeeded, gg7 does not carry the ADV-03 caveat; the first GitHub runs of the Phase 39 `setup-gradle` 6.4.1 pin are U1 (`tests.yaml`) and U2/U4 (`release.yaml`) `[CITED: 39-COMMAND-EVIDENCE.md line 322]`.

**Contingencies** (from gg6, still valid): C-1 `custom_build` moved (absorb by merging `origin/custom_build`, re-gate, re-ask); C-2 a CI-only defect before the compare step (no re-dispatch; a gap, since no `.github/` edit is allowed; the user may authorise an exception); C-3 compare reports `UNALLOWED` or `STALE-ROW` on the runner (investigate against the artifact; a corrected list needs a new local gate and review); C-4 red or not-exactly-one `tests.yaml` run on `X0` or `M` (stop; forward-only rollback, never a force-push); C-5 accidental publish from the wrong ref (a dry run is harmless; a publish not meant is the user's to delete); C-6 failed publish run (read the state; the user chooses); C-7 date mismatch; C-8 a hand-pushed `v1.7.0_gg7` (the user deletes only that tag). `[CITED: 38-ORCHESTRATOR-RUNBOOK.md]`

## Common Pitfalls

### Pitfall 1: A hand-pushed or local `v1.7.0_gg7`
**What goes wrong:** `Check the release tag is absent` fails the publish run. **Avoid:** the handoff files say "do not push `v1.7.0_gg7`"; `git tag -l 'v1.7.0_gg7*'` must print nothing until U4 ends; only `pre-v1.7.0_gg7` exists locally and is pushed after the publish run. **Warning sign:** `git push --tags` or `--all` in any command.

### Pitfall 2: A false FORK-DELTA row survives (section 6.2)
**What goes wrong:** a reviewer finds `Processing.java` "byte-identical" next to a row that says it differs "for the first time". **Avoid:** recount by the file's method and fix lines 138, 139, 188; run `git diff --quiet v1.7.0 HEAD -- <each file named>` for every "byte-identical" claim in the file.

### Pitfall 3: Scope errors in the note (gg6's 28 warnings)
**What goes wrong:** a client V2 fact is stated for every connector (F-2 is client V2 only; the default `client_version` is `V1`), or an "unbuffered" claim lacks its condition. **Avoid:** an `Applies when` value in every row; the F-2 scope row first; reviewer mandated confirmation on scope.

### Pitfall 4: Overclaiming labels
**What goes wrong:** `measured` on a claim whose evidence is code reading or a mechanism (T6, T7, T8, F4). **Avoid:** section 7.0; the reviewer checks every label against its source; the progress-header row stays `derived`.

### Pitfall 5: The allow-list written before the tree is final, or copied from gg6
**Avoid:** section 4 order and the stale-row and delete-row mutations; the gg6 list would be 31 STALE-ROW failures.

### Pitfall 6: A note that asserts facts about a release that does not exist yet
**Avoid:** section 7.1 closing paragraph; asset size, digest and tag SHA live in the local record after U4 only.

### Pitfall 7: `latest` drift and JVM crashes read as test failures
See section 5. A crash is exit 255 with no XML; check the image date first for a red on unchanged code. The host is at about 98 % swap; stop daemons (`./gradlew --stop`) and close heavy processes before each suite.

### Pitfall 8: Same SHA on two branches
`tests.yaml` cancels in progress per SHA. `M` is a real merge (different SHA from `X0`), so scratch and `custom_build` runs never collide. Never push the scratch branch and `custom_build` at one SHA.

### Pitfall 9: `custom_build` moves under you
The user merging Dependabot PRs #21/#22 directly on origin diverges `X0` (it happened before gg4 and gg5). Pre-check at every U step; contingency C-1. Both PR heads are ancestors of the tip, so after `M` lands GitHub should mark them merged by itself.

### Pitfall 10: Publishing exposes the local tail
Pushing `X0` publishes 195 commits that are on no origin ref (197 since the gg6 merge), the `.planning` history included; 281 `.planning` files, 24751 insertions differ from the published tip, and 37 files (2863 insertions, 138 deletions) outside `.planning` `[VERIFIED: git diff --shortstat 72e8ccff HEAD]` (all figures grow with each Phase 44 commit; the bundle prints live values). Deleting a branch does not unpublish commits (UF-36-1). Uncommitted `.planning/config.json` and `state.json` changes are not published.

### Pitfall 11: Date mismatch at the merge
UTC midnight is 02:00 local. A U1 to U3 run that crosses it leaves the CHANGELOG heading a day early (as in gg6). Plan the user gate for one UTC day.

### Pitfall 12: Relayed approvals and notifications
Executors refuse approvals relayed by another agent; a task notification is not an approval. Only the user's own message to the orchestrator counts and it is recorded verbatim.

### Pitfall 13: Reviewer proposals applied unchecked
See 8.3. Also: the review helper's round cap (12) and `round3_decision` rule were edited seven times in gg6; start with `--max-rounds`.

## Code Examples

### Bump and baseline (as run on the export)
```bash
python3 -I .github/scripts/bump_version.py gg                                   # -> v1.7.0_gg7
git tag --merged HEAD --list 'v*' | python3 -I .github/scripts/baseline_tag.py v1.7.0_gg7   # -> v1.7.0_gg6
```
### Compare proof
```bash
python3 -I .github/scripts/compare_confluent_archives.py \
  <base-export>/build/confluent/clickhouse-kafka-connect-v1.7.0_gg6.zip \
  <tip-export>/build/confluent/clickhouse-kafka-connect-v1.7.0_gg7.zip \
  .planning/scripts/confluent-archive-allowlist-v1.7.0_gg7.txt        # exit 0, RESULT: PASS, no STALE-ROW
```
### Merge `M` (orchestrator, detached, after the user's direct approval; never pushed by an agent)
```bash
git worktree add --detach /tmp/<new-dir>/cb 72e8ccffe05ff6dffb14098d0b8c0f67aaa6a236
cd /tmp/<new-dir>/cb
git --attr-source="$(git hash-object -t tree /dev/null)" merge --no-ff -F "<repo>/.planning/phases/44-release-publication/44-MERGE-MESSAGE.txt" <X0>
git rev-parse HEAD^1 HEAD^2 && git diff --quiet HEAD <X0> && echo tree-equal
```
### Read-back after U4 (read-only GETs)
```bash
gh api repos/ne1r0n/clickhouse-kafka-connect/releases --jq '.[] | {id, tag_name, author: .author.login, draft, assets: [.assets[]|{name,size,uploader: .uploader.login}]}'
gh api repos/ne1r0n/clickhouse-kafka-connect/git/ref/tags/v1.7.0_gg7
gh api 'repos/ne1r0n/clickhouse-kafka-connect/actions/workflows/release.yaml/runs?per_page=5'
gh api repos/ne1r0n/clickhouse-kafka-connect/actions/runs/<run id>/jobs      # step "Publish release": success
```

## State of the Art

| Old | Current | Impact |
|-----|---------|--------|
| gg6: tag and release hand-checked, publish path unproven | gg6 publish run `41298074815` created tag `v1.7.0_gg6` and release 430511927 (FORK-DELTA item 22) | gg7 has no ADV-03 caveat; only `pre-v1.7.0_gg7` is hand-pushed |
| gg6 allow-list: 31 rows for client, zstd, lz4, buffer fix | gg7 allow-list: 18 rows (gson, 14 connector classes, manifest) | a new file, not a copy |
| gg6 review: 12 rounds | gg7: the same loop with scoping built in and `--max-rounds` | still ask the user after round 3 |
| ROADMAP "left count 1" | measured 0 (the gg6 merge is already an ancestor) | amend the wording at close |

## Assumptions Log

| # | Claim | Section | Risk if wrong |
|---|-------|---------|---------------|
| A1 | The user runs every origin write with their own account (the gg6 decision (b) carries over) | 9, Open Question 9 | The handoff files are the wrong shape; the orchestrator would need the user's direct word per write instead |
| A2 | GitHub marks PRs #21 and #22 merged once `M` lands (it did for #16, #18, #19, #20) | 1 | The user closes two PRs by hand; harmless |
| A3 | The runner's gg7 archive size is close to the local 13564144 bytes (gg6: runner 13558692 vs local 13558633) | 7.1 D4 | The user reports a different number; the compare tool, not size, decides |
| A4 | Docs-only commits cannot move the raw numbers (scopes contain no file Phase 44 edits) | 6.1 | A number must be re-measured; the plan re-runs both commands on the final tip |
| A5 | Testcontainers does not re-pull a present local `latest` | 5 | A suite runs a newer build; the three digest reads show it |
| A6 | The cumulative invariants script costs about 40 minutes and passes with `anchor_rows=190` on the final head | 5 | Only a sanity check; measure and record |
| A7 | A date-fix commit touching only `CHANGELOG.md` leaves the allow-list and the suites valid | 3 | None found: no test reads CHANGELOG (grep of `src/test` found message text only); stated for the plan to verify with `git diff --name-only` |
| A8 | The review loop ends in about 3 to 6 rounds with scoping built in | 8.3 | The user is asked after round 3 anyway |
| A9 | The reviewer's token/time budget allows whole-FORK-DELTA review in one round | 8.2 | Split the brief by section if a round times out |

## Open Questions (RESOLVED)

1. **Exact pre-gate upstream recheck and origin divergence (settle a).** RESOLVED. Block in section 1. Measured: origin `custom_build` `72e8ccff`; `0 197`; upstream `main` `9bed6f79`, newest tag `v1.7.0`; #861, #855, #857 open; no gg7 tag or `sync/v1.7.0` head on origin. The expected left count is **0** (the ROADMAP's "1" is stale); amend the wording at close.
2. **bump_version.py / CHANGELOG / VERSION procedure and the generated-version check (settle b).** RESOLVED. Section 3: `python3 -I .github/scripts/bump_version.py gg`, one commit `{VERSION, fork-anchors.txt}`, rows stay 190, `clean generateVersionFile` then read `ARTIFACT_VERSION`, CHANGELOG skeleton and UTC-date rule.
3. **How the gg7 allow-list is generated and what differs from gg6 (settle c).** RESOLVED. Section 4: 120 differences, 18 rows (gson 2.15.0 rows, 14 exact connector rows, manifest); client, zstd-jni and lz4-java no longer differ; candidate proved exit 0 with three mutations; two-literal-segment rule satisfied (3 and 4).
4. **Which FORK-DELTA numbers to refresh once and how (settle d).** RESOLVED. Section 6: measure on the release tip once (345/42439/1221 and 43/3183/294; gg6 to gg7 34/2826/112 and 8/194/22), write tag-based commands (`v1.7.0_gg7`), fix lines 138, 139, 184, 188 (the `Processing` byte-identical claim is false; recount 30/14/19/10).
5. **Plan split with gates and the user questions (settle e).** RESOLVED. Section 9: five plans plus an orchestrator-inline tail, gates G0 to G5, eight bundle questions with what each approval authorises.
6. **Gate recipe, expected counts, invariants cost, `latest` drift, Docker (settle f).** RESOLVED. Section 5: testUnit 1826/2 (13 s) and full suite 2425/18 (2 m 46 s) measured today (not copied); anchors 190; invariants about 40 min once on an explicit SHA; `latest` digest `739e099a...`, `26.10.3.7`, unchanged on Hub today; colima `vz` up; swap about 98 % so expect crash risk.
7. **What the review brief covers and the heading rule (settle g).** RESOLVED. Section 8.
8. **FUT-05 and the Phase 43 handoff.** RESOLVED. R8 carries "FUT-05 stays open"; the brief tells the reviewer FUT-05 in gg6 C2 and the notice is deliberate; item 3 (insert-timeout correction) resolved by Open Question 10.
9. **Who runs the origin writes.** NEEDS A USER DECISION (asked at the approval bundle, question 1). Default recommended: the user, as in gg6. Does not block planning; it decides how the four handoff files are worded.
10. **Correct the published gg6 note, or supersede it in the gg7 note?** Recommendation (orchestrator decision, low stakes): supersede; do not edit the gg6 note or CHANGELOG sections; the gg7 note carries a "what this release closes" table. If the orchestrator edits, extend the gg6 correction notice to name every row touched (including the item-22 sentence) and put the edited note in the review brief.
11. **Measure the F-2 progress-header claim, or carry it as derived?** Recommendation: carry it as `derived (mechanism, not measured)`. The user may ask for a measurement; it needs a server version or insert shape that streams headers during a body insert, which Phase 41 did not find.
12. **CHANGELOG heading date versus UTC at the merge.** To be settled at the gate by the user (keep as a recorded deviation, or a date-fix commit). Prevention: run U1 to U3 in one UTC day; the check is `date -u +%F` against `head -1 CHANGELOG.md` at the bundle and before building `M`.
13. **ROADMAP and REQUIREMENTS wording for the close record (orchestrator).** Amend: the Note's "left count of 1" to 0; RLS-04 and criterion 5 already say the tag is created by the publish run (consistent); optionally note that the first GitHub runs of the Phase 39 pins are U1 and U2.
14. **Host memory.** Swap is about 98 % used (27.2 of 27.6 GB). The full suite ran green on the default OpenJ9 JVM today, but the crash recurred twice in gg6 under the same condition; the plan uses the crash rule of section 5 and may need `-PjavaVersion=21` for both suites (a user judgement item, bundle question 3).

## Environment Availability

| Dependency | Required By | Available | Version | Fallback |
|------------|------------|-----------|---------|----------|
| Docker via colima profile `vz` | the two full suites | yes (running) | server 27.4.0 | `colima start -p vz` (never bare `colima start`) |
| ClickHouse `latest` image | suites | yes, local | `sha256:739e099a...b1ce`, `SELECT version()` `26.10.3.7` | none |
| JDK 17 toolchain | build, suites | yes: default test JVM Semeru 17.0.9 (OpenJ9); `~/.gradle/jdks` holds adoptium 11 and 17 (HotSpot); PATH `java` 21.0.12 | 17 | `-PjavaVersion=21` |
| Gradle wrapper 9.8.2 (both tag and tip) | archive builds | yes (ran twice this session) | 9.8.2 | none |
| python3 | scripts | yes | 3.14.8 | none |
| `gh` (authenticated, owner) | read-only GETs | yes | 2.102.0 | unauthenticated `curl`, 60 requests per hour |
| git | merges, worktrees | yes (`--attr-source` needs 2.40 or later) | 2.50.1 (Apple Git-155) `[VERIFIED: git --version]` | none |
| Network (Maven Central, Docker Hub, api.github.com) | wrapper, digest read, upstream reads | yes | n/a | none |
| Host memory | JVM stability | swap about 98 % used | n/a | `./gradlew --stop`, close heavy processes, `-PjavaVersion=21` |

**Missing dependencies with no fallback:** none. Origin, GitHub Actions and the user's approval are inputs, not tools.

## Validation Architecture

Skipped: `workflow.nyquist_validation` is `false` in `.planning/config.json` `[VERIFIED: config.json read this session]`. The gate commands and totals are in section 5.

## Security Domain

`workflow.security_enforcement` is true, ASVS level 1, block on high `[VERIFIED: config.json]`.

### Applicable ASVS Categories
| ASVS Category | Applies | Standard Control |
|---------------|---------|-----------------|
| V2 Authentication, V3 Session | no | none in scope |
| V4 Access Control | yes (process) | only the user runs origin writes (default); executors refuse relayed approvals; `GITHUB_TOKEN` has `contents: write` only on the release job `[VERIFIED: release.yaml permissions block]` |
| V5 Input Validation | yes | the compare script escapes archive entry names before logging (log forging); the allow-list loader refuses catch-all and leading-wildcard rows; `dry_run` is a typed boolean input |
| V6 Cryptography | no new use | archive digests are SHA-256, recorded not hand-computed |
| V10/V14 Supply chain and configuration | yes | all actions pinned to full SHAs (`setup-gradle` 6.4.1 pin guarded by four pin-guard classes); no package added; the archive gains no new native; no secret in any document |

### Known Threat Patterns
| Pattern | STRIDE | Standard Mitigation |
|---------|--------|---------------------|
| Secrets or environment names in published `.planning` and docs | Information disclosure | run the hygiene and secrets helpers (`44-evidence-check.py secrets`, the hygiene scan of gg6) over every text file added or modified since `72e8ccff` before U1; angle-bracket values only |
| Tampered archive or unexpected jar content | Tampering | strict compare against the tag-rebuilt baseline plus `verifyConfluentArchive` (no nested jar, no Kafka API class) |
| Accidental publish or wrong ref | Tampering | `run-name`, `dry_run` wording, separate handoff files, tag-absent gate, tests gate |
| Force push, `--tags`, `--all` | Tampering | the handoff files give exact refspecs; the prohibitions are kept |
| Operator-document advice that damages state (the gg3 `TRUNCATE` finding) | Tampering | reviewer check; the bundle's "no document tells anyone to empty a table" check |
| Record data in logs | Information disclosure | the RSK-03 WARN carries only code numbers (read from the code); the note states it, and the reviewer confirms it quotes the text verbatim |

## Sources

### Primary (HIGH confidence, opened or run this session)
- `.planning/phases/44-release-publication/44-CONTEXT.md`, `.planning/REQUIREMENTS.md`, `.planning/STATE.md`, `.planning/ROADMAP.md` Phase 39-44 blocks, `./CLAUDE.md`, `AGENTS.md`, `.claude/CLAUDE.md`, `.planning/config.json`
- The gg6 precedent: `.planning/milestones/v1.7.0_gg6-maintenance-phases/38-release-publication/` (`38-RESEARCH.md`, the five PLAN heads, SUMMARY deviation sections, `38-ORCHESTRATOR-RUNBOOK.md`, `38-APPROVAL-BUNDLE.md`, `38-RELEASE-RECORD.md`, `38-allowlist-check.py`, `38-review-check.py`)
- Phase 39-43 outputs: `41-COMMAND-EVIDENCE.md` E-09 and E-10, `41-03-SUMMARY.md`, `42-VERIFICATION.md`, `43-VERIFICATION.md`, `43-DISPOSITION-LEDGER.md` (including "Handoff to Phase 44"), `43-evidence-check.py` (run), `43-REVIEW.md` and `43-REVIEW-DISPOSITION.md` frontmatter
- Code and documents: `.github/workflows/release.yaml`, `tests.yaml`; `.github/scripts/{bump_version,baseline_tag,compare_confluent_archives}.py`; `.planning/scripts/{fork-anchors.txt,confluent-archive-allowlist-v1.7.0_gg6.txt,verify-merge-invariants.sh}`; `build.gradle.kts` lines 9-12, 48, 234, 504-516; `docs/FORK-DELTA.md` (lines 1-12, 38-39, 50, 62, 64-65, 138-146, 184-188, 231-245, 291-293), `CHANGELOG.md` (lines 1-104), `docs/ROLLOUT-v1.7.0_gg6.md` (lines 1-40, 109-128, 207-276); the `v1.7.0_gg6..HEAD` diff of `ClickHouseSinkTask.java`, `Utils.java`, `Processing.java`
- Commands (this session, read-only except builds in `/tmp/gsd44r/`): `git ls-remote origin` and `upstream`, `git rev-list`, `git merge-base`, `git diff --shortstat`, `gh api` GETs (pulls, releases, runs, variables, branches), `curl` GETs (upstream PRs 861, 855, 857, 777, 786; Docker Hub `latest`), `docker image inspect`, `docker run ... SELECT version()`, two `createConfluentArchive` builds, four compare runs, one `testUnit --rerun` and one full `test --rerun` on an export of `HEAD`, `bump_version.py` on the export
- Auto-memory notes: `gsd-phase11-orchestration-gotchas`, `gsd-phase15-review-gate-gotchas`, `gsd-phase16-publication-gotchas`, `gsd-phase17-ci-log-access`, `dependabot-merges-on-custom-build`, `nightly-integration-tests-inherited-red`, `zsh-var-colon-modifier-in-git-refspec`, `colima-vz-profile`, `openj9-mockito-test-jvm-crash`, `gsd-phase25-orchestration-gotchas`

### Secondary and tertiary
- None. No web documentation was needed; nothing here depends on a library API.

## Metadata

**Confidence breakdown:**
- Standard stack and scripts: HIGH, all in the tree and run.
- Allow-list and gate figures: HIGH (measured on an export of the current tree; the plan re-measures on the release tip).
- Order of operations and gates: HIGH for the local half; MEDIUM for the runner half (the compare chain ran once on a runner, for gg6; the Phase 39 `setup-gradle` pin has not run on GitHub yet).
- Operator-effect inventory: HIGH for completeness against the committed records; label strength inherited from them, to be re-checked by the reviewer.
- Pitfalls: HIGH, each from a measured state or a recorded incident.

**Research date:** 2026-11-02
**Valid until:** about 7 days, or sooner if origin `custom_build` moves, Docker Hub `latest` changes, a PR head changes, upstream tags past `v1.7.0`, or the user merges #21 or #22 (re-run the section 1 block).

## Autonomous-run decisions (2026-11-02)

- OQ-10 (published gg6 note): SUPERSEDE, i.e. do not edit the published gg6 note further in Phase 44; the gg7 note carries corrections (researcher recommendation).
- OQ-11 (F-2 progress-header claim): carry as `derived (mechanism, not measured)`; do not measure (researcher recommendation).
- OQ-9 (who runs origin writes): the plan repeats the gg6 arrangement as the default (the user runs every push and the release dispatch); the approval bundle at the gate asks the user to confirm or change it. Nothing is pushed before the user's direct word.
- FORK-DELTA false claim (line ~138 Processing.java "byte-identical to upstream v1.7.0", and stale lines ~139/188) is fixed in the Phase 44 FORK-DELTA pass.
