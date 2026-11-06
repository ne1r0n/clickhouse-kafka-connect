# Phase 49: Release & Publication - Research

**Researched:** 2026-11-06
**Domain:** fork release engineering for `v1.7.0_gg8`: version bump, final CHANGELOG and FORK-DELTA, one operator note, a generated archive allow-list, two full gates, orchestrator-run independent review rounds, and a user-gated publication through `release.yaml`
**Confidence:** HIGH for repository, origin, upstream and script facts (read or measured this session); MEDIUM for anything that depends on a runner (the scratch dry run) or on the user's answers at the gates

Labels used per claim: `measured` (a command ran this session and its output is quoted or summarised), `read` (a file was opened this session, value quoted), `derived` (follows from measured or read facts, nothing ran for it itself), `[ASSUMED]` (listed in the Assumptions Log). Scratch work is under `/tmp/gsd49r/` (two read-only Python recount scripts) plus one dangling commit object and one `git merge-tree --write-tree` tree object created in `.git/objects` (no ref moved; see the safety note in section 6). The only file written in the repository is this one. Not run, by instruction: Gradle, Docker, Testcontainers, any push or other network write. No library, API or ecosystem question arose, so the `research-plan` and `package-legitimacy` seams were not called.

## Loud items for the orchestrator and the planner (decisions or corrections that change the plan)

1. **The scratch dry run would compare against the wrong baseline (measured).** `release.yaml` picks the baseline with `git tag --merged HEAD --list 'v*' | python3 -I .github/scripts/baseline_tag.py "$RELEASE_VERSION"`. On the current tip this prints `v1.7.0_gg6`, because tag `v1.7.0_gg7` is the merge commit `94834763`, which is **not** an ancestor of `sync/v1.7.0` (`git rev-list --left-right --count origin/custom_build...HEAD` prints `1	220`). The gg8 allow-list is built against a gg7 baseline (14 rows, totals `only-b 3 changed 11 same 7501`), so a dispatch from the scratch branch at the current history fails the compare with about 120 `UNALLOWED` lines (gson and the rest of gg6 to gg7) and would also report every allow-list row as `STALE-ROW`. A dangling probe commit with parents `HEAD` and `origin/custom_build` (tree = `HEAD` tree) makes the same pipe print `v1.7.0_gg7`. Recommended: merge `origin/custom_build` into `sync/v1.7.0` locally, tree-neutral (a `git merge-tree --write-tree origin/custom_build HEAD` exits 0 and writes tree `fdb32cc5...`, equal to `HEAD^{tree}`), **before** the gates, so the gated head, the scratch push and the dry run all see gg7 as the baseline. The publish run on the real merge commit is unaffected (the merge's first parent is `94834763`, so gg7 is merged). The ROADMAP recipe has no dry-run step; gg7 had one (U2). See Open Question 2.
2. **Origin `custom_build` is `94834763` (the gg7 merge) and the divergence count is `1 220`, not 0 (measured).** ROADMAP step 1 ("A left count above 0 means `origin` moved") is wrong for this milestone: left 1 is exactly the gg7 merge commit (`git rev-list origin/custom_build ^HEAD` prints only `9483476322e0d331e81c5b92ac266a18f0f01450`); `sync/*` continues from `M^2` (`fc570937`), as the Phase 16 memory note says. The check must read "left count is 1 and that one commit is `94834763`; anything else means origin moved". `tree(94834763) = tree(fc570937) = ebe6c13a...` (measured), which is why the merge of the tip onto `94834763` is clean.
3. **`pre-v1.7.0_gg8` does not name the published tip (measured).** The local lightweight tag points at `baa404cd` (the Phase 45 evidence-open commit on `sync/v1.7.0`), not at `94834763` (= tag `v1.7.0_gg7`). `git diff --stat 94834763 baa404cd -- . ':(exclude).planning'` is empty, so the two trees differ only under `.planning` (148 files). For gg7 the pre-tag was `72e8ccff`, the published tip and `v1.6.0_gg6`'s tag, and the rollback note could say "which is also tag `v1.7.0_gg6`". For gg8 the gg7 note's sentence cannot be copied. Either state the truth in the note ("`baa404cd`, whose tree outside `.planning` equals tag `v1.7.0_gg7`") or, with the user's word, retag locally to `94834763` before the push (the tail gates of Phases 45-48 used `PRE_TAG=pre-v1.7.0_gg8`; `verify-merge-invariants.sh` accepts a tag whose every parent is an ancestor, per its own comment, but the gates would have to be re-run). Open Question 1.
4. **Only two of D-3's "three upstream-tail items" are new (measured).** `83808df9:CHANGELOG.md` holds three bullets: `# 1.7.1, Unreleased` > Bug Fixes (#838, escaped `/` in DESCRIBE) and `# Unreleased` > Improvements (`additionalRetriableErrorCodes`, issues #847 and #455) and Bug Fixes (#640 record context). The `additionalRetriableErrorCodes` bullet is byte-identical in `9bed6f79:CHANGELOG.md` and sits in the `1.7.0_gg6` section of this CHANGELOG (Important Changes and Improvements); it shipped in gg6. The gg8 section should mention it only as already shipped, not as a new bullet.
5. **FORK-DELTA "The raw numbers" holds no gate totals today (measured).** The section (lines 149-201) contains only `git diff --shortstat` figures, their reproduction commands, per-release prose and the import-only recount. `grep` for `2425`, `2295`, `2898`, `2325` finds none in the file. D-5 and E-18 item 6 ("refresh with the gate totals") therefore have no slot to refresh. Planner must decide: refresh the shortstat table and the recount (they move, section 4), and add one measured sentence with the gate totals if the user wants them there. Open Question 4.
6. **FORK-DELTA's "128 of 143 test-tree files byte-identical" does not reproduce (measured).** A recount over `git diff --name-only --diff-filter=AM v1.2.9 v1.7.0 -- src/test src/testFixtures src/integrationTest` (143 files, all present at every tip) finds 115 byte-identical at `v1.7.0_gg6`, `v1.7.0_gg7` and `HEAD` alike. The figure is unchanged by gg8, so it is not a Phase 49 move, but a fresh reviewer who tries to re-derive it will file a finding (gg7's Plan 44-01 left it "as written" for the same reason). Either leave it and tell the reviewer, or recount and correct it in the final pass.
7. **Upstream state changed since the ROADMAP text (measured).** Upstream `main` is `83808df9` (= the merged tail), newest tag still `v1.7.0` (no tag past it). #861 is open, head `b62e0ae4` (updated 2026-11-06T00:17Z; it was `3d4cfdf2` at gg7), #855 open `ddabf28b`, #854 open `69d1efc5`; **#788 is closed, not merged** (`closed_at` 2026-11-04T15:10:16Z, `merged_at` null) and #857 is closed and merged (its content is the tail). No PR is open on origin (`gh api .../pulls?state=open` printed nothing). FORK-DELTA item 13 still says "upstream PR #861 (open)", which holds.
8. **Authenticated `gh` can read job logs now (measured).** `gh api --allow-escape-sequences repos/ne1r0n/clickhouse-kafka-connect/actions/jobs/126216113863/logs` returned the gg7 publish job's log (ANSI codes stripped with `sed`). The Phase 17 memory note (no `gh`, a user-made Keychain token) is out of date: no token route is needed; `gh auth status` reads logged in as `ne1r0n`.
9. **Counts in the Phase 48 hand-off that are stale (read, measured):** E-18 item 6's testUnit 2320 is superseded by 2325 (E-19); the full suite is 2923 + 5 = 2928 derived, to be re-measured here; E-18 item 1's "14 rows" is the count at the Phase 48 tip and becomes 17 once `VERSION` changes (section 3, derived).

<user_constraints>
## User Constraints (from CONTEXT.md)

### Locked Decisions
Copied verbatim from `49-CONTEXT.md` (Mode: auto-generated, `workflow.skip_discuss`; "No new user question was needed; every push, the merge, the publish run and the scratch-branch deletion stay the user's, asked at their gate."):

- **D-1 (order):** docs and counts first, the allow-list generated last from the final tree (a later `src/main` or build change would change the row count; regenerate, never copy), then the two full suites and the review rounds, then the gate. Docker-free work is separated from live work.
- **D-2 (gg8 allow-list):** 14 exact rows and no wildcard measured at Phase 48 tip (the 12 of E-14 plus `DeclaredNullability$1.class` and `DeclaredNullability$Assignment.class`); totals against gg7 `files-a 7512 files-b 7515 only-a 0 only-b 3 changed 11 same 7501`. Regenerate from the final tree; any wildcard row must pass `AllowListWildcardRuleUnitTest` (two literal path segments after stripping `outer/`).
- **D-3 (CHANGELOG):** complete the existing `# 1.7.0_gg8, Unreleased` section; add the three upstream-tail items of `1.7.1, Unreleased` (#838 escaped `/` in DESCRIBE, `additionalRetriableErrorCodes`, record-context serialization failures, with the fork's value-free policy stated), the Phase 46 timeout limits, the Phase 47 retry outcome (a `RetriableException` from `put` is redelivered without bound, a limit by decision) and the FUT-05 decision (no validator), and Known Issues for the two-ERROR-line family per FORK-DELTA item 14 (as reworded in Phase 48), naming the removed public `DeclaredNullability.ESCAPED_NEWLINE` (user-approved U-4). The heading date is the merge date in UTC, set at the gate and not before.
- **D-4 (rollout note):** one new `docs/ROLLOUT-v1.7.0_gg8.md` in the gg7 note's shape, every effect labelled `measured`, `read from the code` or `derived`, and each claim pointing at evidence that survives archiving (FORK-DELTA items, tests by name, tags), not at `.planning` paths.
- **D-5 (raw numbers):** FORK-DELTA "The raw numbers" is refreshed once, with the gate totals measured on the final tree: testUnit 2325 (measured at Phase 48 review fix), full suite re-measured here (expected 2928 with the 18 named skips, derived until the suites run), anchors as the mirror reads.
- **D-6 (independent review):** before the push gate the orchestrator spawns a fresh `gsd-code-reviewer` on `docs/ROLLOUT-v1.7.0_gg8.md`, `CHANGELOG.md`, `docs/FORK-DELTA.md` and the gg6 and gg1 notes edited in Phase 48; rounds repeat until one finds nothing, at most three. A fourth round needs the user. Every finding's disposition is recorded.
- **D-7 (gates):** the gates run orchestrator-inline. Nothing is pushed, merged, published or deleted by an executor. Plans stop at each gate and the orchestrator asks the user, in their own words, one gate at a time.
- **D-8 (before the gate):** recheck upstream (tags past `v1.7.0`, open PRs #861, #855, #854, #788) and `origin/custom_build...sync/v1.7.0` divergence (the user merges Dependabot PRs on origin directly).
- **D-9 (Phase 48 hand-offs):** the E-18 inventory of Phase 48 applies: gg8 CHANGELOG Known Issues and rollout carry the item-14 wording, raw numbers use 2325, and the 12 `.planning/phases/45..47` evidence paths in FORK-DELTA move at the milestone close.

(Phase boundary, verbatim: "`v1.7.0_gg8` is prepared and, only on the user's direct word, published on `custom_build`. This phase edits documents, `VERSION`, the gg8 allow-list and `fork-anchors.txt` rows only. It edits no `src/`, test, `build.gradle.kts`, `gradle/` or `.github/` file. A defect found here goes back to its phase as a gap, or is deferred with a reason, an owner and a re-open trigger in the milestone close record.")

### Claude's Discretion
"Wording, section order and the evidence-entry layout, guided by ROADMAP, the gg7 note and Phase 43 and 47 precedent."

### Deferred Ideas (OUT OF SCOPE)
"Backlog P1-P16 test-scope advisories, the Confluent `7.9.1-ccs` question, the JSON route wrapping of non-Jackson IOExceptions, `close(partitions)` without `finally`, the `keepFirst` idiom in `stop()` and the first-put matrix through `putDirect`: milestone-close backlog, not this phase."
</user_constraints>

<phase_requirements>
## Phase Requirements

| ID | Description | Research Support |
|----|-------------|------------------|
| RLS-01 | `VERSION=v1.7.0_gg8` via `bump_version.py`; CHANGELOG, FORK-DELTA, `fork-anchors.txt` in the same commits as the counts they change | Section 2 (bump, the one anchors row, places the string lives), section 1 (CHANGELOG gap list), section 4 (numbers that move and the one anchors row coupled to a document-adjacent value) |
| RLS-02 | gg8 allow-list generated from the final tree; dry run `0 unallowed, 0 stale` | Section 3 (recipe, projected 17 rows, what the `VERSION` bump adds, the baseline trap of loud item 1) |
| RLS-03 | `docs/ROLLOUT-v1.7.0_gg8.md` with every effect labelled and fresh-context review rounds | Section 5 (inventory with labels and sources, structure, reviewer brief, round rules) |
| RLS-04 | Two equal green full suites on `latest` x V2, `testUnit` green with Docker unreachable, anchors gate | Section 7.3 (gate commands, expected totals 2325 and 2928 derived, digest pin) |
| RLS-05 | `--no-ff` merge after explicit approval, one green `tests.yaml` on the published SHA, publish run creates tag and release | Section 6 (sequence, gates, workflow facts, merge shape, protected-branch and worktree constraints) |
| RLS-06 | Delete the two `scratch/clt-07-*` branches after a separate confirmation | Section 6 step 9 (both tips are ancestors of `origin/custom_build`, deletion loses no commit, measured) |
</phase_requirements>

## Project Constraints (from CLAUDE.md)

Read this session: `./CLAUDE.md` (imports `AGENTS.md`), `./.claude/CLAUDE.md`, the memory index. Actionable directives for this phase:
- GSD workflow enforcement: file edits go through a GSD workflow (this phase is one). No commits to the branch, no PR title or description changes, without explicitly asking the user (`AGENTS.md` Review rules). Commit trailer: `Co-Authored-By: Claude Sonnet 5.5 <noreply@anthropic.com>` (git commit messages only).
- `AGENTS.md`: bump `VERSION` and update `CHANGELOG.md` for a release; never upgrade dependencies without asking; do not log secrets or record data; Spotless is enforced but `spotlessApply` must never run over the whole tree (46 violations pre-exist; this phase edits no Java).
- `.claude/CLAUDE.md`: tests pin current fork behaviour; code changes only for genuine bug fixes; keep changes additive for rebasability; Docker is colima (profile `vz`).
- Memory rules that bind execution: relayed approvals are refused; orchestrator spawns the real `gsd-code-reviewer` before the push gate; reviewer findings under `### ID: title`; test rollback commands on the publication tip; zsh `${M}:refs/...`; no `timeout` binary; bare `colima start` hits the wrong profile; Dependabot merges diverge origin; run `printf` rather than `echo "==="` in zsh; `env -u TESTCONTAINERS_HOST_OVERRIDE` before every Gradle call; never `git clean`; `h` is a zsh alias, use `bash -c`/scripts.

## Summary

Phase 49 repeats the gg7 Phase 44 recipe with three structural differences that the gg7 research could not have: the published tip is a merge commit that the working branch does not contain (divergence `1 220`), the rollback tag is not the published tip, and the baseline-tag pipe therefore selects gg6 on the scratch branch. Everything else carries over: one `bump_version.py gg` commit holding exactly `VERSION` and the `VERSION` anchors row; a CHANGELOG section completed (it exists, 22 lines, two headings); a FORK-DELTA final pass whose measurable figures all move in the `src/main` row and the wider row but not in the file-class recount; a new operator note in the gg7 shape; an allow-list generated last (projected 17 exact rows, derived: the 14 of Phase 48 plus `Version`, `SinkTaskStatistics` and the inner `META-INF/MANIFEST.MF`); two equal suites; a fresh-reviewer loop run by the orchestrator; and a publication tail that is entirely orchestrator-inline and user-gated.

The tree outside `.planning` has not changed since the Phase 48 gate head `a968e486` (measured: `git diff --name-only a968e486 HEAD -- . ':(exclude).planning'` and `-- .planning/scripts` print nothing), so Phase 49's non-`.planning` diff will be exactly: `VERSION`, `CHANGELOG.md`, `docs/FORK-DELTA.md`, `docs/ROLLOUT-v1.7.0_gg8.md`, `.planning/scripts/fork-anchors.txt` (one row) and `.planning/scripts/confluent-archive-allowlist-v1.7.0_gg8.txt`. No test, workflow or build script reads `CHANGELOG.md`, `docs/` or `VERSION`'s text for a gate (measured: `grep -rln CHANGELOG src/test src/testFixtures src/integrationTest .github build.gradle.kts` lists only `.github/pull_request_template.md`), so a late documentation edit cannot turn a suite red or move the archive; the gg7 "Freeze rule" is a procedural rule (reviewed text, approved head) and not a technical one, and its cost in gg7 was about 2.5 hours per trigger.

**Primary recommendation:** five local plans (1: pre-flight, helpers, tree-neutral merge of `origin/custom_build`, bump, CHANGELOG, FORK-DELTA; 2: the note and its rollback rehearsal; 3: orchestrator-run review rounds; 4: allow-list last and the gates; 5: dry-run merge, approval bundle, handoff files) plus an orchestrator-inline, user-gated tail (U1 scratch push, U2 dry run, "publish" -> build `M'` -> U3 push, U4 publish dispatch, U5 pre-tag and branch deletions, RLS-06 as a separate confirmation). Settle Open Questions 1 to 5 with the user before the docs freeze.

## Architectural Responsibility Map

| Capability | Primary Tier | Secondary Tier | Rationale |
|------------|-------------|----------------|-----------|
| Version bump, CHANGELOG, FORK-DELTA, rollout note | Local repo, executor (`sync/v1.7.0`) | none | Plain files; `VERSION` plus its anchors row is one script call and one commit |
| Tree-neutral merge of `origin/custom_build` into `sync/v1.7.0` | Orchestrator or executor, local only | none | Local history edit, no origin write; decides the baseline tag (loud item 1) |
| Independent review of operator documents | Orchestrator (spawns `gsd-code-reviewer`) | none | Executors have no Agent tool; an inline review does not count (Phase 15 and 24 memory, recurred) |
| Allow-list generation and compare proof | Local repo, executor (two `createConfluentArchive` builds in `/tmp`) | GitHub runner (repeats the compare in the dry run and the publish run) | Docker-free, same steps as `release.yaml`; the list is a derived artifact |
| Docker-free `testUnit`, two full suites, anchors gates | Local repo, executor (colima `vz`) | none | Needs Docker and the pinned `latest` image; never overlap two Gradle jobs |
| Dry-run merge `D` and rollback rehearsal | Local repo, throwaway detached worktree on `origin/custom_build` | none | Phase 16 lesson: rehearse the rollback on the publication tip |
| Merge commit `M'` construction | Orchestrator inline, detached worktree at `94834763` | origin (pushed by SHA) | `custom_build` is protected by GSD rule (GitHub `protected:false`, measured); local `custom_build` is stale at `3a3a15a9`, never use it |
| Scratch push, `custom_build` push, release dispatches, tag push, branch deletes | Orchestrator on the user's direct word per step (gg7 practice), or the user | origin / GitHub runner | ROADMAP: only on the user's direct word; the arrangement is the user's choice at the bundle |
| Approval and the RLS-06 confirmation | User, directly to the orchestrator | none | Executors refuse relayed approvals |
| Read-back after each write | Orchestrator, read-only `gh api` GET, `git ls-remote`, `curl` GET | none | No agent runs a re-run, cancel, comment, merge or close |

## Standard Stack

Nothing is added or installed. The phase uses what is in the repository and on the host.

### Core
| Tool | Version | Purpose | Source |
|------|---------|---------|--------|
| `.github/scripts/bump_version.py` | in tree | writes `VERSION` and the anchors `VERSION` row in one staged step; refuses a mismatched row | read (docstring and code, this session) |
| `.github/scripts/baseline_tag.py` | in tree | baseline = greatest `vX.Y.Z[_ggN]` below `VERSION` among the tags given on stdin | read and measured (`HEAD` -> `v1.7.0_gg6`, probe merge -> `v1.7.0_gg7`) |
| `.github/scripts/compare_confluent_archives.py` | in tree | entry-by-entry archive compare under an allow-list; refuses a glob starting `*`, `?`, `[` | read (docstring) |
| `.planning/scripts/verify-merge-invariants.sh` | in tree | anchor/provenance gate; tail form `PRE_TAG=pre-v1.7.0_gg8 UPSTREAM_REF=83808df9a0993cf49b2ceaa4633c782e550c0180 CONFLICTED_PATHS_FILE=.planning/scripts/conflicted-paths-v1.7.0-tail-3.txt bash .planning/scripts/verify-merge-invariants.sh 12e2cf17cca9f5b17917dd9aceaa38b44f3f8b10 <H>` (Phases 45-48, last result `anchor_commits=171 anchor_rows=218 ... result=PASS`), or the cumulative form `bash ... 54e63648 <H>` (gg7: about 40-50 min alone) | read (script header, E-17, E-19 of Phase 48) |
| Phase 48 helpers | in tree | `48-evidence-check.py` (get, expect, index, rows, junit, skips, failed, anchors), `48-testcase-status.py`, `48-path-check.py`, `48-id-scan.py`, `48-ledger-check.py`; copy to `49-` names | read (docstring) |
| gg7 helpers | archived | `44-allowlist-check.py` (rows, cover, same-globs), `44-review-check.py` (round, rounds, disposition; cap 12), `44-note-check.py` (rows, labels, forbidden, fut, claims, all; hard-codes the 33 gg7 inventory IDs, must be rewritten for the gg8 IDs), `44-doc-check.py` (changelog, links, shortstat, numbers), `44-hygiene-check.py` (flags the plain word "stand") under `.planning/milestones/v1.7.0_gg7-followup-phases/44-release-publication/` | read (docstrings) |
| Host | git 2.50.1 (Apple Git-155), Python 3.14.8, `gh` 2.102.0 authenticated as `ne1r0n`, colima profile `vz` running, Docker client 29.8.2 | scripts, reads, suites | measured |

### Alternatives Considered
| Instead of | Could Use | Tradeoff |
|------------|-----------|----------|
| Tree-neutral merge of `origin/custom_build` into the branch before the gates (recommended) | Skip the scratch dry run and let the publish run be the first compare run | Cheaper, but the first runner exercise of the gg7-baseline compare is the publish run itself (it fails before `Publish release` if wrong, so nothing is created, but the tests.yaml SHA is already pushed). Loses the rehearsal gg7 had |
| Keep tag `pre-v1.7.0_gg8` at `baa404cd` and word the note truthfully | Retag locally to `94834763` | Retag matches gg7's meaning ("the published tip") but the Phase 45-48 gate records name the old target; the tail gate must be re-run on the final head anyway (it runs once on an explicit SHA), so the cost is small. User decision |
| Tail-form invariants gate (Phases 45-48) | Cumulative `54e63648 H` (gg7) | Cumulative covers all history incl. the Dependabot merges at about 45 min; the tail form was the gate of every gg8 phase. Run time of the tail form was not recorded in E-17 (unmeasured) |
| Three label forms in the note | Four (gg7: adds `derived (mechanism, not measured)`) | RLS-03 and ROADMAP name three; gg7 used four and its review accepted them. Use three plus a parenthetical on `derived` where nothing observed the effect |

**Installation:** none. **Version verification:** not applicable (no package).

## Package Legitimacy Audit

No external package is installed or recommended; the gate does not apply. `connect-json` (test scope) arrived in Phase 45 as an upstream line (read: `gradle/libs.versions.toml` diff `+kafka-connect-json = { module = "org.apache.kafka:connect-json", version.ref = "kafka" }`, `build.gradle.kts` diff `+    testImplementation(libs.kafka.connect.json)`); it is documented, not added, here. **Packages removed due to [SLOP] verdict:** none. **Packages flagged [SUS]:** none.

## Architecture Patterns

### System Architecture Diagram

```
LOCAL (executors; sync/v1.7.0 @ fae0fb9d; nothing pushed)                  ORIGIN / GITHUB (every write on the user's direct word)
------------------------------------------------------                      ----------------------------------------------------
W0 read-only pre-flight: origin custom_build == 94834763, left/right 1/220, upstream tags + PRs, `latest` digest
W1 merge origin/custom_build -> sync (tree-neutral)  | bump_version.py gg -> ONE commit {VERSION, fork-anchors.txt}
   clean generateVersionFile | CHANGELOG gg8 | FORK-DELTA final (raw numbers once)
W2 docs/ROLLOUT-v1.7.0_gg8.md + evidence map + rollback block on dry-run merge D0
W3 ORCHESTRATOR: fresh gsd-code-reviewer rounds (<=3; 4th needs user) -> fixes -> dispositions -> docs frozen
W4 allow-list generated LAST (gg7 tag export + tip export, compare) -> commit
   gates on head H: testUnit (Docker unreachable) | 2x test --rerun on one pinned latest | anchors mirror | tail gate once on H
W5 dry-run merge D of X0 onto 94834763 + rollback block on D + approval bundle + handoffs + runbook
        | user's direct "go"
        v
U1 --------------------------------------------------------------------->  push X0 -> refs/heads/sync/v1.7.0 -> tests.yaml (1 run, green)
U2 --------------------------------------------------------------------->  dispatch release.yaml from sync/v1.7.0 (always a dry run): baseline gg7, COMPARE, draft+delete
        | user's own direct "publish"
        v
O  build M' = merge --no-ff X0 onto origin/custom_build 94834763 (detached worktree), tree(M') == tree(X0)
U3 --------------------------------------------------------------------->  push M' -> refs/heads/custom_build (non-force) -> tests.yaml on M' (exactly 1, green)
        | user's "publish run" word
U4 --------------------------------------------------------------------->  dispatch release.yaml from custom_build, dry_run NOT ticked
                                                                           compare -> tests.yaml gate -> tag-absent gate -> gh release create v1.7.0_gg8 --target M'
A  read-only GETs: release, tag ref type, asset, run steps, PR states
U5 --------------------------------------------------------------------->  push tag pre-v1.7.0_gg8 by name; delete sync/v1.7.0
        | separate user confirmation (RLS-06)
U6 --------------------------------------------------------------------->  one push deleting scratch/clt-07-client-0.11.0 and scratch/clt-07-prebump-0.9.6
```

### Recommended file layout (all under `.planning/phases/49-release-publication/` unless noted)
```
49-COMMAND-EVIDENCE.md, 49-evidence-check.py (copy of 48-), 49-allowlist-check.py, 49-review-check.py (--max-rounds), 49-note-check.py, 49-doc-check.py, 49-hygiene-check.py
49-REVIEW.md, 49-REVIEW-DISPOSITION.md        # heading rule: ### <CR|BL|WR|IN>-NN: <title>
49-MERGE-MESSAGE.txt, 49-APPROVAL-BUNDLE.md, 49-USER-DISPATCH-{1..4}-*.md, 49-ORCHESTRATOR-RUNBOOK.md, 49-RELEASE-RECORD.md (after U4)
docs/ROLLOUT-v1.7.0_gg8.md                    # published
.planning/scripts/confluent-archive-allowlist-v1.7.0_gg8.txt   # written last; name fixed by release.yaml line 97
```

### Pattern 1: Publish by merge SHA, never by checking out `custom_build`
**What:** build `M'` in a detached throwaway worktree created from `origin/custom_build` (`94834763`), not from the local `custom_build` ref (stale at `3a3a15a9`, measured: `git rev-parse --verify -q custom_build` prints `3a3a15a9`). `M'` has parents `94834763` and `X0`, and `tree(M') == tree(X0)`.
**Merge shape (measured):** `git merge-tree --write-tree origin/custom_build HEAD` exit 0, tree `fdb32cc5f282ecdd0e7a461004da972de9278d82` = `HEAD^{tree}`. If the recommended pre-gate merge is done (X0 then contains `94834763`), `--no-ff` onto `94834763` is a merge of an ancestor and `M'` still gets a SHA different from `X0`, which keeps `tests.yaml`'s SHA-keyed group from cancelling the scratch run [VERIFIED: .github/workflows/tests.yaml:31-33 `group: ${{ github.workflow }}-${{ github.event_name == 'pull_request' && github.event.number || github.sha }}`, `cancel-in-progress: true`]. gg7 precedent subject: `Merge sync/v1.7.0: release v1.7.0_gg7` [VERIFIED: `git log -1 94834763`]; use `Merge sync/v1.7.0: release v1.7.0_gg8` with `git merge -F` and the attribution trailer as last line.
**The `gradlew.bat` trap (gg6/gg7):** merge and rollback `switch`/`revert` ran with `git --attr-source="$(git hash-object -t tree /dev/null)"` (git 2.40 or later; host is 2.50.1). Re-measure on `D`; gg7 found the tree clean.

### Pattern 2: Allow-list as a derived artifact (section 3)
### Pattern 3: User handoff document shape
Status; what you decide; numbered pre-checks each with an expected value; the exact command; expected result; fields to report; if it fails earlier; clean-up; a "Resume (for the orchestrator)" block that records the user's report verbatim with the word `user-reported`. Evidence ids for the tail are the orchestrator's from the next free number; failed records are never edited, a re-run is a new number (gg7 E-12 -> E-20). `[CITED: gg7 44-USER-DISPATCH-*.md, 44-ORCHESTRATOR-RUNBOOK.md]`

### Anti-Patterns to Avoid
- **Hand-pushing `v1.7.0_gg8`, or creating it locally.** `Check the release tag is absent` refuses to publish over it. Only `pre-v1.7.0_gg8` is pushed by hand, after the publish run. `[VERIFIED: release.yaml:162-175]`
- **Writing the allow-list before the tree is final, or copying the gg7 list.** The gg7 list has 18 rows (gson, `ProxySinkTask`, `Processing`, ...) none of which differ gg7 to gg8; copied, it is mostly `STALE-ROW`.
- **Treating a green dry run as proof of the publish step.** The dry run skips `Publish release` (`if: ${{ !inputs.dry_run && github.ref == 'refs/heads/custom_build' }}`). The publish step has now run twice (gg6, gg7).
- **Re-running or re-pushing to turn a run green** without the user's answer.
- **Using local `custom_build` as a merge base or checkout** (stale `3a3a15a9`).

## Don't Hand-Roll

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| Version bump with anchors row | `sed` on `VERSION` and the anchors file | `python3 -I .github/scripts/bump_version.py gg` | Stages both writes, refuses a mismatched row, restores on failure; `increment-version.yml` commits exactly `VERSION` and the anchors file |
| Baseline tag choice | A hard-coded `v1.7.0_gg7` | the `baseline_tag.py` pipe on the commit being built | The same code the workflow runs; it is the thing that goes wrong (loud item 1) |
| Archive compare | `diff`, `unzip` trees | `compare_confluent_archives.py` | Reads in memory, escapes log-forging names, refuses catch-all rows |
| Allow-list rows | wildcards beyond what a diff needs | exact class paths (all 17 projected rows are exact) | A wildcard must follow two literal segments after stripping `outer/` (`AllowListWildcardRuleUnitTest`, strengthened in Phase 48) |
| JUnit totals and named skips | ad hoc XML reading | `49-evidence-check.py junit/skips/failed` and `49-testcase-status.py` | The 18 named skips are checked by name (`OK skips: named=7 state_mismatch_cases=11 unexpected=0`) |
| Gate-day date check | eyeballing | `date -u +%F` against `head -1 CHANGELOG.md` | The heading date was wrong in gg6 and again in gg7 |
| Rollback proof | reasoning | the extracted rollback block run unchanged on dry-run merge `D` | Phase 16 lesson |

**Key insight:** every number in the note and FORK-DELTA must come from a command the planner can name; the gg7 review found 25 claims in 5 rounds, almost all scope or label errors. Build the evidence map before the prose, and settle measure-versus-derive questions before the freeze.

## Runtime State Inventory

Not a rename or migration phase, but the version string has places to agree (measured: `git ls-files | xargs grep -l 'v1\.7\.0_gg7'` outside `.planning`):

| Category | Items Found | Action Required |
|----------|-------------|------------------|
| Source of truth | `VERSION` reads `v1.7.0_gg7`; `build.gradle.kts:48` `version = file("VERSION").readText().trim()`; `build.gradle.kts:399` `attributes["Implementation-Version"] = project.version.toString()` | `bump_version.py gg` |
| Anchors | the only `VERSION` row: `VERSION	v1.7.0_gg7	1	BLD-07` (file line 41; the second non-code row is `.gitattributes`, unaffected) | moved by the script in the same commit |
| Generated | `build/generated/sources/version/java/main/com/clickhouse/kafka/connect/sink/Version.java` currently reads `ARTIFACT_VERSION = "v1.7.0_gg7"` (measured). `generateVersionFile` declares an output directory and no inputs (`build.gradle.kts:505-510`), so it is not re-run on a changed `VERSION` | `./gradlew clean generateVersionFile` and read the file |
| Inlined by javac | `ClickHouseHelperClient.CONNECT_CLIENT_NAME = "clickhouse-kafka-connect/" + Version.ARTIFACT_VERSION` (client name, user agent) and `SinkTaskStatistics.buildMBeanName(connectorName, taskId, Version.ARTIFACT_VERSION)` (JMX `version=`); no other `src/main` class references either (grep) | archive rows (section 3) and rollout row D3 |
| Archive | `META-INF/MANIFEST.MF` `Implementation-Version`; `config/archive/manifest.json` `expand(project.properties)` (outer `manifest.json` compared ignoring `release_date` and `version` lines) | none beyond the compare |
| Documents | `CHANGELOG.md` (7 mentions of `1.7.0_gg7`, all history; the released sections stay byte-identical), `docs/FORK-DELTA.md` (14 mentions: intro line 5, pointer line 7, row line 74 "`VERSION` reads `v1.7.0_gg7`", raw-numbers rows and prose, syncer paragraph line 308), `docs/ROLLOUT-v1.7.0_gg6.md` (1, history), `docs/ROLLOUT-v1.7.0_gg7.md` (tag copy, never edited) | FORK-DELTA line 74 must become gg8; the rest gets gg8 additions beside gg7 |
| Test fixtures | `BaselineTagScriptUnitTest`, `BumpVersionScriptUnitTest`, `ClientEndpointAndOptionsPinUnitTest` use `v1.7.0_gg*` as literals only; gg7 ran 1826 green on a bumped export | none |
| Origin / OS / secrets | no stored data, services, OS registrations or secrets carry the version; origin refs only (tag, release) | none |

## 1. CHANGELOG: the gg8 section as it stands, what is missing, and how gg6/gg7 handled upstream items

### 1.1 Current state (measured/read)
`CHANGELOG.md` line 1 is `# 1.7.0_gg8, Unreleased`; the section is lines 1-21 and the gg7 section starts at line 23 (`# 1.7.0_gg7, 2026-11-02`). Only two commits touched the file since `v1.7.0_gg7`: `15aa445f` (start the section, LIF-02) and `3e0575a7` (reword, WR-02/IN-03b). Headings present: `## Bug Fixes` (one bullet: `ClickHouseSinkTask.stop()` reaches the proxy stop in a `finally`; names the correction of the gg7 sentence "The task's `stop()` stops the writer, the MBean and the state provider even when one of them throws") and `## Known Issues` (two bullets: the other gg7 residuals plus `close(partitions)` shape; the both-throw outcome from a one-off probe). `git show v1.7.0_gg7:CHANGELOG.md` equals the file from line 23 on (measured: `diff` silent), so every released section is byte-identical and must stay so (the gg6 review rule).

### 1.2 What is missing (derived from `git log v1.7.0_gg7..HEAD`, FORK-DELTA items 13/14/17/24, D-3, E-11 and E-18)
| # | Missing item | Source | Note |
|---|--------------|--------|------|
| 1 | Intro paragraph: base `upstream 1.7.0` plus tail up to `83808df9` (gg6/gg7 said `9bed6f79`); changes since `v1.7.0_gg7`; pointers to `docs/ROLLOUT-v1.7.0_gg8.md` and FORK-DELTA | gg7 section lines 23-30 shape | Do not call `83808df9` "tagged": upstream has no tag past `v1.7.0` (measured) |
| 2 | **Important Changes**: client V2 body-write stall now ends retriably at `clickhouseClientInsertTimeoutMs` (write-phase deadline, `cancelTransportRequest` by query id, re-armed per client retry) | FORK-DELTA item 14; commits `b53cd23c`, `10fdbf41` | Scope client V2; skipped when the operator names `socket_timeout` |
| 3 | **Important Changes**: client V1 insert wait is bounded by `clickhouseClientInsertTimeoutMs` (retriable, operator `socket_timeout` wins) | item 14; commit `05b46d18` | **Contradicts the gg7 bullet "no timeout is added to client V1 inserts"**: say so. At the default 240000 ms a quiet V1 insert still ends at the library's own 30 s limit; the bound matters for write stalls, progress-streaming inserts and smaller values |
| 4 | Upstream tail #838: field names with `/` in a `DESCRIBE` response round-trip | tail bullet; `SlashColumnRoundTripTest` (4 live cases), `FieldDescriptorLiteralFieldsUnitTest` (31 cases) | State the fork's difference: `fromJsonRow` decodes only `name`; other strings stay literal JSON (FORK-DELTA row 55) |
| 5 | Upstream tail #640: serialization failures name topic, partition, offset and column; with the fork's value-free policy: one ERROR line, the writer's own exception unchanged, no value on any surface, JSON route included | tail bullet; FORK-DELTA rows 48-49; commits `f13d8378`, `f4b85584`, `0c8be1da` | Differs from upstream (upstream puts the coordinates in the exception message) |
| 6 | `additionalRetriableErrorCodes` | already in gg6 (loud item 4) | At most one clause "already shipped in 1.7.0_gg6"; not a new bullet |
| 7 | Help text of `clickhouseClientInsertTimeoutMs` rewritten to say what it bounds and what it does not | commit `63aade66`, `19c36eaa`; `InsertTimeoutHelpTextPinUnitTest` | Closes the gg7 Known Issue "The help text ... still describes a hard limit" |
| 8 | Phase 47 retry outcome: a `RetriableException` out of `put` is redelivered without bound under both tolerances, a limit by decision | FORK-DELTA item 17; `RetriableRedeliveryLimitPinUnitTest` | Supersedes gg7 Known Issues bullets at lines 89-93 (E-11 items 6, 7); gg7 section not edited |
| 9 | FUT-05 closed by decision (no validator, no budget, no removal setting; WARN stays) | items 8, 24 | The gg7 note said "FUT-05 stays open" |
| 10 | **Known Issues**: two ERROR lines on a deadline cut (`Record serialization failed: ...` always; `Error writing value of ...` only when the cut lands inside the value write) and neither means the record is bad; names removed public `DeclaredNullability.ESCAPED_NEWLINE` (user-approved U-4) | item 14 (as reworded in `7ae4284c`); commit `c4ec0cbf` | Use the Phase 48 wording; E-18 items 4, 5 |
| 11 | Known Issues carried: gg7 residuals that stay open; the limits of the bounded insert that remain (non-insert requests, V2 ping, progress-streaming V2 insert after the body is sent: **measured as escaping, by decision**) | item 14 | Tighten the gg7 wording that the progress escape was the only V2 limit |
| 12 | **Build and CI**: `connect-json` is a test dependency only (archive unaffected); the three upstream bumps (slf4j-simple 2.0.20, spotless 8.10.4, commons-lang3 3.21.0) were already in the `v1.7.0_gg7` catalog, so they arrive as no-ops (measured: `gradle/libs.versions.toml` at gg7 holds `commonsLang3 = "3.21.0"`, `slf4j = "2.0.20"`, `spotless = "8.10.4"`; checklist tail-3 says "arrive as no-ops"); allow-list compare for gg8 against `v1.7.0_gg7` | catalog diff | State only what the diff shows |
| 13 | Release identity: JMX `version=` and client name read `v1.7.0_gg8` | gg7 bullet shape | One bullet, as gg6/gg7 |
| 14 | Test-only items (allow-list wildcard rule counts real segments; `ESCAPED_NEWLINE` moved to fixtures) | Phase 48 | Optional; one line under Build and CI |

### 1.3 How gg7 and gg6 handled upstream items (read)
gg6's section "absorbs the `Unreleased` entry that upstream left in this file": its intro says so, `additionalRetriableErrorCodes` appears as an Important Changes bullet ("from upstream's tail (see Improvements)") and verbatim under Improvements; the fork-owned section replaces upstream's `# Unreleased` block (gg6 `44-doc-check.py changelog` rule: new section ends with OLD minus its upstream `# Unreleased` block). gg7 carried no upstream bullets: its intro names the base ("upstream `1.7.0` plus upstream's untagged tail up to `9bed6f79` (the same base as `1.7.0_gg6`)") and lists only fork changes. For gg8 the fork's CHANGELOG has no upstream `# 1.7.1, Unreleased` or `# Unreleased` heading (the merge took the fork side), so there is nothing to delete; add the two new upstream bullets (#838, #640) as fork-worded bullets.

### 1.4 Heading date
`date -u +%F` today is `2026-11-06`; the heading must carry the UTC merge date. gg6 and gg7 both published a day early and the user kept the date. No test, workflow or script reads `CHANGELOG.md` (measured), so a one-line date-fix commit cannot affect any gate. Recommended: write the expected merge date in the last pre-review commit; at the approval bundle compare with `date -u +%F`; if the day will change before U3, make the one-line date-fix **before U1** so the pushed head already carries it (a later change makes a new `X0`, which needs a new scratch push). Open Question 5.

## 2. VERSION bump: invocation, edits, generated-version check, every place the string appears

**Invocation (read, `.github/scripts/bump_version.py`):** `python3 -I .github/scripts/bump_version.py gg` from the repository root. Grammar `v([0-9]+)\.([0-9]+)\.([0-9]+)(?:_gg([1-9][0-9]*))?`; `gg` raises N by one, so `v1.7.0_gg7` becomes `v1.7.0_gg8` (the only line on stdout). It computes both new contents and a backup of the anchors file before replacing either; refuses (exit 1) when the anchors file has not exactly one non-comment row whose first TAB field is `VERSION`, with four fields and count `1`, or when that row's pattern differs from `VERSION`'s content. It edits exactly two files: `VERSION` and `.planning/scripts/fork-anchors.txt` (the `VERSION` row's pattern field). Verbatim row now: `VERSION	v1.7.0_gg7	1	BLD-07`.
**Commit:** one commit holding exactly `VERSION` and `.planning/scripts/fork-anchors.txt` (the shape `increment-version.yml` commits) so invariant 3 of the gate sees the row move in the commit that moves the file. The anchors mirror must still read `rows=218 bad=0` (218 non-comment rows measured now: `grep -v '^#' ... | grep -v '^$' | wc -l` prints 218; E-19 `anchors_mirror: rows=218 bad=0`).
**Generated-version check after `clean` (derived from the build file, gg7 E-02 measured it):** `generateVersionFile` is `outputs.dir(...)` only, so a plain build keeps a stale file (the file in `build/` reads `v1.7.0_gg7` now, measured). Run `env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain clean generateVersionFile` (Docker-free), then `grep -n ARTIFACT_VERSION build/generated/sources/version/java/main/com/clickhouse/kafka/connect/sink/Version.java` must show `"v1.7.0_gg8"`, and `git tag --merged HEAD --list 'v*' | python3 -I .github/scripts/baseline_tag.py "$(tr -d '[:space:]' < VERSION)"` must print `v1.7.0_gg7` (it prints `v1.7.0_gg6` until the merge of loud item 1 is done; measured).
**Bump timing:** before CHANGELOG and FORK-DELTA wording (they name the new version), after the pre-flight and the tree-neutral merge. `BumpVersionScriptUnitTest` and `BaselineTagScriptUnitTest` pin the script rules (Docker-free).
**Does `manifest.json` carry the version?** `config/archive/manifest.json` is expanded with `project.properties` at archive time (read: `build.gradle.kts` `from("config/archive/manifest.json") { expand(project.properties) }`); there is no `manifest.json` at the repository root. Nothing to edit.

## 3. The allow-list: generation, comparison, and what changes once VERSION and CHANGELOG move

**What the gate is (read):** `release.yaml` builds `allowlist=".planning/scripts/confluent-archive-allowlist-${RELEASE_VERSION}.txt"` and fails when absent; the compare runs `python3 -I .github/scripts/compare_confluent_archives.py "$BASELINE_ARCHIVE" "$ARCHIVE" "$ALLOWLIST"`; any non-zero exit, `UNALLOWED` or `STALE-ROW` fails the run before the tag and tests gates. Format: `glob<TAB>reason`, non-empty reason, no glob only stars, none starting `*`, `?` or `[`. `CompareConfluentArchivesScriptUnitTest.everyShippedAllowListStillLoads` and `AllowListWildcardRuleUnitTest` load every list under `.planning/scripts` (read), so the new file must be well-formed from its first commit. No test requires a list for the current `VERSION` (measured: only `ReleaseWorkflowGuardUnitTest` names the path template), so testUnit stays green between the bump and the list commit.
**gg7 recipe (read, 44-RESEARCH sec. 4 and E-11):** export the baseline tag and the tip with `git archive <ref> | tar -x -C <new empty dir>` (separate directories; Python for any script reading them uses `-I`), build each with its own wrapper `env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain createConfluentArchive` (the `verifyConfluentArchive:` line must print; `-PjavaVersion=21` was used for the Phase 48 dry runs), compare with a comment-only list to obtain the full `UNALLOWED` set, derive rows by owner, write the file, re-run (exit 0, `RESULT: PASS`, no `STALE-ROW`), run three mutations (stale row -> `STALE-ROW`; a deleted row -> `UNALLOWED`; a leading-wildcard row -> exit 2), then `49-allowlist-check.py rows` and `cover`. The first line must name `.github/scripts/compare_confluent_archives.py`. The baseline for gg8 is the export of tag `v1.7.0_gg7` (local tag exists, equals `94834763`, measured).
**State at the Phase 48 tip (read, E-15):** `tip_comment_only_unallowed: 14`, `draft_rows: 14`, `draft_wildcard_rows: 0`, `draft_result: RESULT: PASS`, totals `files-a 7512 files-b 7515 only-a 0 only-b 3 changed 11 same 7501`; the 14 exact paths (read from `/tmp/gsd48/exec/dry-draft-gg8-14.txt`, which survives only until `/tmp` is cleaned): `ClickHouseSinkConfig`, `ClickHouseSinkTask`, `db/ClickHouseWriter` with `$1`, `$2`, `$3`, `$InsertBody`, `$InsertWriteDeadline`, `db/helper/ClickHouseHelperClient` and `$ClickHouseClientBuilder`, `db/mapping/DeclaredNullability` with `$1` and `$Assignment`, `util/Utils` (each under `com/clickhouse/kafka/connect/sink/...`; the three `ClickHouseWriter` nested classes are `ONLY-B`, new since gg7). Do not rely on `/tmp`: regenerate.
**What changes between the Phase 48 tip and the final tree (derived; no build was run):** `VERSION` moves `v1.7.0_gg7` -> `v1.7.0_gg8` and nothing else in the archive inputs moves (the non-`.planning` diff of Phase 49 is documents, `VERSION`, and `.planning/scripts` only; `docs/` and `CHANGELOG.md` are not in the archive, which carries `README.md` and `LICENSE` from the repo root). Consequences: `Version.class` (generated constant) changes, `SinkTaskStatistics.class` changes (inlined `Version.ARTIFACT_VERSION`), `ClickHouseHelperClient.class` already differs (inlined `CONNECT_CLIENT_NAME`, already in the 14), and inner-jar `META-INF/MANIFEST.MF` differs in `Implementation-Version` only. Projected list: **17 exact rows = the 14 + `com/clickhouse/kafka/connect/sink/Version.class` + `com/clickhouse/kafka/connect/util/jmx/SinkTaskStatistics.class` + `META-INF/MANIFEST.MF`**; projected totals against the gg7 archive `files-a 7512 files-b 7515 only-a 0 only-b 3 changed 14 same 7498`. D-2's "14 rows" and "`changed 11 same 7501`" are the Phase 48 numbers; the gg7 list shows the same three identity rows (read: `Version.class`, `SinkTaskStatistics.class`, `META-INF/MANIFEST.MF` in `confluent-archive-allowlist-v1.7.0_gg7.txt`). Treat 17 as an expectation to be measured, as 12 -> 14 was in E-14: any other count is a finding, and regenerate rather than patch.
**Archive-affecting hazards:** `README.md` is the only repository document inside the archive; Phase 49 must not edit it. The compare ignores `release_date` and `version` lines of the outer `manifest.json`.
**Baseline on the runner:** see loud item 1; after the tree-neutral merge `git tag --merged` on the pushed head includes `v1.7.0_gg7`, so the runner's baseline is gg7 and the 17-row list applies. On the published `M'` (first parent `94834763`) the same holds.

## 4. FORK-DELTA "The raw numbers": every figure, how it is measured, what moves

Section lines 149-201 (read). All scopes below contain no file Phase 49 may edit, so documentation commits cannot move them; measure once on the release tip (before the tag exists; a local `v1.7.0_gg8` must not be created) and write tag-pair commands for `v1.7.0_gg8`, using the sentence gg7 used (measured once on the release tip before the release workflow creates the tag; the merge's trees equal its second parent's). With the recommended pre-gate merge the same holds because `M'` is a merge of an ancestor.

| Figure (location) | How measured | Current text | Moves with gg8? | Value now (measured on `HEAD`, equal to the Phase 49 final tree for these scopes) |
|---|---|---|---|---|
| Table row "after gg7", wider (`src build.gradle.kts gradle .github`) | `git diff --shortstat v1.7.0 <tip> -- src build.gradle.kts gradle .github` | 345 / 42439 / 1221 | **add a new row "after gg8"** (keep gg7 row) | `364 files changed, 47983 insertions(+), 1260 deletions(-)` |
| Table row "after gg7", `src/main` | `git diff --shortstat v1.7.0 <tip> -- src/main` | 43 / 3183 / 294 | **new row "after gg8"** | `43 files changed, 3578 insertions(+), 333 deletions(-)` |
| gg7 rows reproduce (method check) | `git diff --shortstat v1.7.0 v1.7.0_gg7 -- <scopes>` | 345/42439/1221 and 43/3183/294 | no | printed exactly those figures |
| Reproduction commands block | one pair per release | ends at `v1.7.0_gg7` | add the `v1.7.0_gg8` pair | n/a |
| Per-release prose "From `v1.7.0_gg7` to `v1.7.0_gg8`" | `git diff --shortstat v1.7.0_gg7 <tip> -- <scope>` and per directory | does not exist | **new paragraph** | wider 36 files, 5602 insertions, 97 deletions; `src/main` 6 files, 428 insertions, 72 deletions (`ClickHouseSinkConfig` 32/7, `ClickHouseSinkTask` 8/5, `ClickHouseWriter` 376/55, `ClickHouseHelperClient` 10/0, `DeclaredNullability` 0/3, `Utils` 2/2); `src/test` 28 files 5172/25; `gradle` 1 file 1/0 and `build.gradle.kts` 1 file 1/0 (the `connect-json` test dependency); `.github`, `src/testFixtures`, `src/integrationTest` unchanged |
| Recount of `src/main` against `v1.7.0` (Adopted wholesale row, line 144; prose line 199) | each of the 63 files at `v1.7.0`: byte-identical, identical after removing `import` and blank lines (import-only), or modified; plus files new since | 43 differ; 30 byte-identical of 63; 14 import-only; 19 modified; 10 new | **No** (counts equal at gg7 and `HEAD`) | measured by `/tmp/gsd49r/recount.py`: `v1.7.0_gg6 {identical 33, import_only 14, modified 16, new 10, differ 40}`, `v1.7.0_gg7 {30, 14, 19, 10, 43}`, `HEAD {30, 14, 19, 10, 43}` |
| `git diff --quiet v1.7.0 v1.7.0_gg7 -- <file>` commands (rows 142, 143, 199) | exit codes per file | name gg7 | update the tag in the commands to `v1.7.0_gg8` (the result is the same: `Processing.java` and `KeyToValue.java` are not among the 6 changed files) | n/a |
| `Processing.java` row text "one-line change at `doLogic` line 137" | read | true | no (file unchanged since gg7) | n/a |
| "Between `v1.2.9` and `v1.7.0` upstream added or changed 143 test-tree files ... 128 byte-identical ... 15 differ" (line 145) | not re-derivable (loud item 6) | 143 / 128 / 15 | not moved by gg8 | 143 files; **115** byte-identical at gg6, gg7 and `HEAD` under `src/test src/testFixtures src/integrationTest` (also 101 of 125 without `src/integrationTest`) |
| "Named skip inventory": "The current suite skips 18 tests" | `grep -rho '@Disabled' src/test/java \| wc -l` and `grep -rho 'Assumptions.assumeTrue' src/test/java \| wc -l` | 7 and 1 (11 cases) | no | measured `7` and `1` |
| Upstream-tail figure `a6f1df83` "2 files, 11 insertions and 9 deletions" | historic | static | no | n/a |
| Gate totals (testUnit, suite, skips, anchors) | the gate commands, section 7.3 | **not in the file** (loud item 5) | add only if the user wants them in the section | testUnit 2325 / 2 skipped (E-19, measured at `a968e486`, tree unchanged since); suite 2928 derived, to measure; 18 skips; anchors `rows=218 bad=0` |

**Other FORK-DELTA edits the final pass needs (read):** intro line 5 and pointer line 7 (add `v1.7.0_gg8` and `docs/ROLLOUT-v1.7.0_gg8.md`, keep older pointers); row line 74 "`VERSION` reads `v1.7.0_gg7`" -> gg8; the "For the next syncer" paragraph (add a gg8 paragraph in the gg7 shape: published from `sync/v1.7.0` by a non-force merge into `custom_build`; rollback tag per Open Question 1; what it carries (the upstream tail `83808df9`, the stop() fix, the timeout limits, FUT-05 decision); upstream state at the time (main `83808df9`, no tag past `v1.7.0`, #861/#855/#854 open, #788 closed unmerged, #857 merged); the compare baseline `v1.7.0_gg7`); keep items 13, 14, 17, 24 as written but re-read them against the note's rows; do not move the 12 `.planning/phases/45..47` tokens (milestone close, E-18 item 7; measured: 5 + 5 + 2 occurrences in `45-`, `46-` and `47-COMMAND-EVIDENCE.md`); no phase or evidence identifier added to published text (`48-path-check.py` and `48-id-scan.py` give `unresolved=0`, `ids: hits=0`).

**`fork-anchors.txt` rows coupled to counts (RLS-01 same-commit rule).** The file holds 218 non-comment rows: 138 under `src/main`, 54 `.github/workflows`, 11 `build.gradle.kts`, 5 `gradle/libs.versions.toml`, 3 `src/testFixtures`, 3 `src/integrationTest`, 2 `src/test`, and exactly two rows outside code paths: `VERSION	v1.7.0_gg7	1	BLD-07` and `.gitattributes	.github/workflows/integrationTests.yaml -text	1	ADV-01` (measured). No row names `CHANGELOG`, `docs/` or a rollout note (`grep -c` finds one comment-line hit only). **So exactly one row moves in Phase 49: the `VERSION` row, moved by `bump_version.py` in the commit that changes `VERSION`.** The anchors mirror total (218) is the count the gate helpers pass as `--rows 218`; it does not change in Phase 49 because no row is added or removed. A review fix that touches an anchored file is outside this phase's allowed edit set and goes back as a gap.

## 5. The rollout note `docs/ROLLOUT-v1.7.0_gg8.md`

### 5.1 Structure of the gg7 note (read, 212 lines)
Title `# Rollout checklist: <from> to <to>` (backticked tags) -> bold lead paragraph "The connector source changes in this release" with `git diff --shortstat <from> <to> -- src/main` figure, the sentence that commands naming the new tag were measured with `HEAD` on the release tip, and a numbered five-item "at a glance" list -> pointers to CHANGELOG and FORK-DELTA, and to the previous note for older upgrades -> "names no host, connector, cluster or production system; angle-bracket values only" -> **How each claim is labelled** (four label forms, `claim: label; claim: label` for mixed rows) -> **Which connectors a row concerns** (`Applies when` vocabulary; client V2 versus V1 default `V1`) -> `## Before you upgrade` (numbered steps each naming its section) -> sections A..G, each a table `| ID | Effect | Label | Applies when | How it is known |` with IDs F, T, R, D, followed by a "What to do" paragraph -> `### Release identity` -> `### Rollback` (numbered steps, one code block run on a dry-run merge, the "Rolling back brings back" list, the D5 row) -> `## What this release closes from the gg6 note` (supersession table) -> `## Known limits carried into this release`. Rules carried from the gg6 and gg7 reviews: every row has `Applies when`; every client-V2-only fact says so; the note states no fact about the not-yet-existing release object (asset size, digest, tag SHA); no planning, requirement or review identifier in published text (FUT-05 is the one deliberate exception, and **it is now closed**, so the gg8 note should state the closure in one sentence, with or without the identifier, and the reviewer must be told which); no `TRUNCATE`, `DROP`, `DELETE FROM`.

### 5.2 Effects list for Phases 45-48 (proposed IDs; label per RLS-03: measured / read from the code / derived; sources survive archiving)
| # | Effect (operator-visible) | Label | Applies when | Source that survives archiving |
|---|---------------------------|-------|--------------|--------------------------------|
| **Upstream tail** | | | | |
| U1 | Fields whose ClickHouse column names contain `/` are no longer lost: the `DESCRIBE` reader keeps the server's JSON escaping on both clients and decodes only `name`; other strings stay literal JSON text | measured (`SlashColumnRoundTripTest` live on `latest` x V2, 4 cases; `FieldDescriptorLiteralFieldsUnitTest` 31 cases, 30 turn red against upstream's decode-all) ; the V1 half: read from the code | either client | FORK-DELTA Connector-source rows (raw identifier row), the tests by name |
| U2 | A serialization failure logs one ERROR line `Record serialization failed: topic [<topic>] partition [<p>] offset [<o>] column ...: exception <class>` and rethrows the writer's own exception unchanged; no record value on any surface (message, cause chain, trace, DLQ headers, logs), RowBinary and JSON routes; a `RetriableException` passes without the line | measured (`WriterRecordContextUnitTest`, `WriterRecordContextDlqUnitTest`, `JacksonOverIoValueFreeUnitTest`, `JsonRouteValueFreeUnitTest`, `JsonRouteValueFreeDlqUnitTest`) | either client | FORK-DELTA rows 47-49 |
| U3 | Differs from upstream #857: upstream puts the coordinates in the exception text; the fork does not, by the value-free policy; the DLQ `exception.message` header is unchanged | read from the code | any connector with a DLQ | FORK-DELTA row 48 and the test-change row |
| U4 | Upstream's bumps (slf4j-simple, spotless, commons-lang3) arrive as no-ops; `connect-json` is test scope only; the archive shows no third-party difference | measured (catalog values equal at the `v1.7.0_gg7` tag and now; the compare lists connector classes only) | this release | the compare totals, `gradle/libs.versions.toml` |
| **Timeout limits (Phase 46)** | | | | |
| T1 | Scope: `client_version` defaults to `V1`; the V2 bounds and the V1 bound are different mechanisms | read from the code | client V1 vs V2 | gg7 row T1 shape |
| T2 | Client V2: a body-write stall (peer stops reading) ends retriably at the bound (was blocked at the 10 s guard of the pin on the unchanged tree, ends at 1.010 to 1.082 s with a 1000 ms bound); the deadline is armed before `client.insert`, disarmed when the body stream closes, re-armed per client retry; serialization time counts | measured (`InsertWriteStallBoundPinUnitTest`; numbers in FORK-DELTA item 14) | client V2, operator did not name `socket_timeout` | item 14 |
| T3 | Client V1: the three insert methods wait through a bounded `get` (`awaitInsertResponseV1`); a 64 MiB write stall ended at 1.137 to 1.218 s against 1000 ms; a progress-streaming insert that ran about 6 s on the unchanged tree ends at the bound; at the default 240000 ms a quiet V1 insert still ends at the library's 30 s limit and a value above 30000 does not extend it | measured (`InsertV1WaitBoundPinUnitTest`; live: 8141 ms -> failed retriably at 3034 ms with a 3000 ms bound on ClickHouse 26.10.3.7) | client V1 | item 14 |
| T4 | **Supersedes gg7 rows T1 and T12 and the CHANGELOG gg7 bullet "no timeout is added to client V1 inserts"** | read from the code | client V1 | gg7 note rows T1, T12; supersession table |
| T5 | An operator `socket_timeout` in `jdbcConnectionProperties` removes both connector bounds and keeps its own value; `socket_timeout=0` keeps inserts unbounded; one exception: the response wait of a client V2 `async=true` insert | measured (`InsertSocketTimeoutOptionUnitTest`, `InsertBoundGateUnitTest`) | either client | item 14 |
| T6 | A client V2 insert whose body has been sent and whose server keeps streaming progress headers escapes the bound, by decision (re-measured 8299 ms against 3000 ms, only with `async_insert=0` and 500-row blocks; under the default `async_insert=1` it failed at 3050 ms); the cut of a progress-streaming send rests on a stub pin, not a live measurement | measured; the progress-during-send cut: measured on the stub only (say so) | client V2 | item 14 |
| T7 | Not bounded, by decision: `ping`, version query, table list, `DESCRIBE`, `ALTER`, Keeper state requests on V2 (still blocked at the 20 s probe cap), the V1 equivalents end at the library's 30 s limit; the V2 `ping(timeout)` does not fire (still blocked at 20005 ms with 2000 ms) | measured (probe numbers); mechanisms read from the code | as named | item 14 (b), (c) |
| T8 | A timed-out insert is not rolled back; with `exactlyOnce=false` the redelivery inserts the batch again (duplicate); with `exactlyOnce=true` the retry reuses the deduplication token, whether the server drops it depends on the table | outcome after timeout: measured (2500 then 4000 rows); duplicate and token: derived and read from the code | either client | item 14, gg7 rows T7, T8 |
| T9 | The help text of `clickhouseClientInsertTimeoutMs` now states what it bounds and what it does not (gg7 row T14 closed) | read from the code, pinned (`InsertTimeoutHelpTextPinUnitTest`, 18 cases) | either client | `ClickHouseSinkConfig` text; item 14 |
| T10 | Two ERROR lines on a deadline cut (`Record serialization failed: ...`; `Error writing value of ...` only when the cut lands inside the value write); neither means the record is bad | read from the code | client V2 RowBinary route | item 14 (as reworded in Phase 48), E-18 item 5 |
| **Task stop and retry (Phase 47)** | | | | |
| S1 | `ClickHouseSinkTask.stop()` runs the proxy stop in a `finally`; a strategy whose `stop()` throws no longer skips writer stop, MBean unregistration, state-provider close; its exception is the one `stop()` throws; with the three shipped strategies `stop()` returns normally | measured (`ClickHouseSinkTaskStopUnitTest`: two of six cases red on the unchanged tree) ; Connect only logs a throw from `stop()` as WARN: read from 4.x sources | either client | item 13 |
| S2 | **Corrects gg7 row F5 and the residuals bullet**: the task-level `stop()` residual is closed; the others stay (V1 Keeper constructor, helper-client constructor after its V2 client, second `start()`, the five-second wait); new limits: both-throw drops the strategy's exception; `close(partitions)` has no `finally` | read from the code; both-throw: measured by one probe, not pinned | as named | item 13 |
| S3 | A `RetriableException` out of `put` is redelivered without bound under both `errors.tolerance` values; a limit by decision (108 Docker-free cases show it fails against a budget of 5); **corrects gg7 row R8** | measured (`RetriableRedeliveryLimitPinUnitTest`; measured on a real Connect worker) | either client | item 17 |
| S4 | FUT-05 is closed by decision: no retry budget, no removal setting, no validator; the start-up WARN, its text and pins stay; **replaces the gg7 note's "FUT-05 stays open."** | read from the code | any connector that sets `additionalRetriableErrorCodes` | items 8, 24 |
| **Phase 37 dispositions (Phase 48)** | | | | |
| P1 | The public constant `DeclaredNullability.ESCAPED_NEWLINE` is removed from the connector jar (moved to test fixtures); nothing in `src/main` read it | read from the code | anyone compiling against the jar (none known) | CHANGELOG Known Issues, FORK-DELTA item 20 |
| P2 | The `Nullable` tuple describe null binding and the allow-list wildcard rule are test-only | n/a | CI only | tests by name; no operator row needed (one line in "closes") |
| **Identity, dependencies, rollback** | | | | |
| D1 | The release identity (JMX `version=`, client name) reads `v1.7.0_gg8`; update matchers that spell out `v1.7.0_gg7` | identity: read from the code; classes differ in the compare: measured | this release | `Version.ARTIFACT_VERSION` users (section 2) |
| D2 | Archive: inner entry count, sizes of local rebuilds (state them as local rebuilds only), the compare table (17 rows projected), no gson or other library change | measured on the final tree (to be measured in Plan 4) | this release | the compare run |
| D3 | Rollback: see Open Question 1; redeploy the `v1.7.0_gg7` release asset (id 433237218, 13564238 bytes, digest `sha256:3dbdcf03e5825073b7b0535718c9998a5e5754c0ae05a17502bbf4c6d69472af`, uploader `github-actions[bot]`, read from the gg7 release record and `gh api`), not a local rebuild; the block is rehearsed on `D`; rolling back reverses S1, S3 (no change in behaviour), T2, T3, T9, U1 to U3, D1 | rollback block on a dry-run merge: measured; reversal: read from the code | this release | the rehearsal |

**Corrections the gg8 note must carry (E-11 and E-18):** gg7 rows F5 (task-level `stop()` closed), R8 and the line `FUT-05 stays open.` (limit by decision, FUT-05 closed), the residuals bullet (line 208 of the gg7 note), the CHANGELOG gg7 Known Issues bullets at lines 85-93 (retry budget; writer `DataException` shapes), the gg7 bullet "no timeout is added to client V1 inserts", and gg7 rows T1, T12, T14. The gg7 note stays a tag copy (measured: `git diff --quiet v1.7.0_gg7 HEAD -- docs/ROLLOUT-v1.7.0_gg7.md` exits 0); supersede through a "What this release closes from the gg7 note" table, as gg7 did for gg6.

### 5.3 The independent reviewer brief (RLS-03, D-6)
- **Who and when:** the orchestrator spawns a fresh `gsd-code-reviewer` per round after the note, CHANGELOG section and FORK-DELTA final pass exist and before the allow-list, gates, bundle and any push. An executor's inline review does not count (recurred in gg3 Phase 24). Record the reviewer's `agentId` in the round section and in the evidence; gate it with `49-review-check.py agents`-style attestation (gg7 pattern).
- **Files under review (named in round 1):** `docs/ROLLOUT-v1.7.0_gg8.md`; the `1.7.0_gg8` section of `CHANGELOG.md`; `docs/FORK-DELTA.md` (whole file; diff base `9483476322e0d331e81c5b92ac266a18f0f01450`, the published gg7 merge; the docs diff since the tag is CHANGELOG 26 lines, FORK-DELTA 38 lines changed, gg1 note 4, gg6 note 6 at present, measured); `docs/ROLLOUT-v1.7.0_gg6.md` and `docs/ROLLOUT-v1.7.0_gg1.md` as edited in Phase 48 (commits `5453a69c`, `eadacf83` and the FORK-DELTA commits `8f187c9d`, `cc2ca206`, `7ae4284c`).
- **Completeness checklists to hand over (evidence that survives or is attached):** FORK-DELTA items 13, 14, 17, 24, rows 47-49, 55, 74; the Phase 47 and 48 hand-off inventories; the gg7 note's own rows that gg8 supersedes (list above).
- **Deliberate, tell the reviewer so it is not filed as new:** the gg7 note and the gg6 correction notice are history and are not re-edited; FUT-05 appears in the gg6 note row C2 and notice by the user's earlier choice and is closed by decision in gg8; every V1 claim is scoped to V1 on purpose; the T6 claim is carried as measured on a stub and live exactly as item 14 says; which of the label forms the note uses; the CHANGELOG heading date rule; `additionalRetriableErrorCodes` is not a new item; the 128 of 143 figure if left unchanged (loud item 6).
- **Mandated confirmations, one verdict each (`## Mandated confirmations (RLS-03)`):** every inventory row against its source and label (no row stronger than its evidence); commands, numbers and versions against the evidence; each row's `Applies when` against the code path (client V1 vs V2, `exactlyOnce`, `bufferCount`, `reportInsertedOffsets`, `async`); the rollback block and its target; no fact about the `v1.7.0_gg8` release object; no advice that empties or destroys data; the WARN text verbatim if quoted; FORK-DELTA stale rows (line 74, the recount commands, the new gg8 paragraphs); released CHANGELOG sections byte-identical; no host, connector or planning identifier in published text; the CHANGELOG heading date.
- **Heading rule (recurring failure, Phase 15, 24, 44):** every finding under `### <CR|BL|WR|IN>-NN: <title>`, never deeper; each round ends `round: <n> findings: <k>`; frontmatter totals equal the finding headings; IDs continue per prefix; spell this out in every appending round's prompt. The disposition step silently recorded 6 of 13 findings once.
- **Round rules (D-6, User-Owned Gates):** at most three rounds; a fourth needs the user's word in their own words. gg7 ran 5 rounds (14, 2, 2, 2, 5 findings; 8 warnings, 17 info) with the user authorising rounds 4 and 5; gg6 needed 12. Plan for the same question after round 3; give `49-review-check.py` a `--max-rounds` option from the start (the gg6 copy was edited seven times).
- **Reviewer fix text is checked, not applied:** eight proposed fixes were wrong or too strong in gg6 and one in gg7 (a key claim copied from an older note). Check every key, query, command and scope claim against the code first.
- **Dispositions:** `49-REVIEW-DISPOSITION.md`, one row per finding, `open: 0`; fixed rows start with a SHA of this phase; deferred rows carry reason, owner and re-open trigger under `## Carried to the milestone close`; run the disposition step from the extracted bash fence of `~/.claude/gsd-core/workflows/execute-phase/steps/code-review-disposition.md` with `PHASE_DIR` and `PHASE_NUMBER=49`.

## 6. Publication recipe, CI, constraints, divergence, and current origin state

### 6.1 Origin and upstream state, measured 2026-11-06 (read-only)
| Item | Reading |
|------|---------|
| origin `custom_build` | `9483476322e0d331e81c5b92ac266a18f0f01450` (`Merge sync/v1.7.0: release v1.7.0_gg7`, parents `72e8ccff`, `fc570937`); GitHub `protected: false` (the protection is the GSD rule) |
| origin heads | `custom_build`, `main` `bec167d85918e77c11621063000acc6e031ae2fa`, `scratch/clt-07-client-0.11.0` `de8c685b80d40b0a6e19338271e07b08ede84f3b`, `scratch/clt-07-prebump-0.9.6` `939e899298c2f12dec2cfe1e69b1a6b53b4f511e`; **no `sync/v1.7.0`**, no gg8 refs |
| origin tags | `pre-v1.6.0-sync`, `pre-v1.7.0-sync`, `pre-v1.7.0_gg2`..`gg7`, `v1.6.0_gg1`, `v1.7.0_gg1`..`gg7`; **no `v1.7.0_gg8`, no `pre-v1.7.0_gg8`** |
| local tags | `pre-v1.7.0_gg8` -> `baa404cd5ed95c556db0acff84def1024c3bafe3` (lightweight, `git cat-file -t` prints `commit`); `v1.7.0_gg7` -> `94834763` |
| divergence | `git rev-list --left-right --count origin/custom_build...sync/v1.7.0` = `1	220`; the one left commit is `94834763`; `git rev-list --count HEAD --not --remotes=origin` = 220 (commits on no origin ref); outside `.planning` against `origin/custom_build`: 40 files, 5654 insertions, 119 deletions; `.planning` against `94834763`: 229 files, 22744 insertions, 315 deletions |
| local refs | local `custom_build` = `3a3a15a9` (stale); one worktree (the repo); `git status` shows only `.planning/config.json` and `.planning/state.json` modified (orchestrator-managed; they do not ride in any push) |
| releases | `v1.7.0_gg7` id 433237218 (`github-actions[bot]`, not draft, not prerelease), `v1.7.0_gg6` id 430511927, `v1.7.0_gg5` id 426690002 (`ne1r0n`); the gg7 publish run `41721771615` ("Release custom_build (publish)", `workflow_dispatch`, success) and `tests.yaml` push run on `94834763` success |
| origin open PRs | none (PRs #21 and #22 were closed and merged after gg7) |
| upstream | `main` `83808df9a0993cf49b2ceaa4633c782e550c0180`; newest tag `v1.7.0` (`379b8d01`); #861 open `b62e0ae4`, #855 open `ddabf28b`, #854 open `69d1efc5`, #788 **closed unmerged** (2026-11-04), #857 closed merged (in the tail) |
| Docker | colima profile `vz` running; local `clickhouse/clickhouse-server@sha256:739e099abd1157390960a59e4532d21781d8fde57e6e4151cb920ea02c30b1ce`, created 2026-10-26T00:06:54Z; Docker Hub `latest` digest read now: the same, `last_updated` 2026-10-26T00:08:26Z |
| host | swap `used = 33424.31M` of `34816.00M` (96 %): crash risk for the OpenJ9 test JVM; escape `-PjavaVersion=21` |

### 6.2 The recheck block (read-only; run at execution start, at the bundle, and before each U step)
```bash
git ls-remote origin refs/heads/custom_build                     # expect 9483476322e0d331e81c5b92ac266a18f0f01450
git rev-list --left-right --count origin/custom_build...sync/v1.7.0   # expect left 1 (before the tree-neutral merge); 0 after it; right grows
git rev-list origin/custom_build ^HEAD                           # expect only 9483476322e0d331e81c5b92ac266a18f0f01450 before the merge; empty after
git ls-remote --tags origin 'v1.7.0_gg8*' 'pre-v1.7.0_gg8*'       # expect empty
git ls-remote --heads origin sync/v1.7.0                          # expect empty (name free)
git ls-remote --tags upstream | tail -3                           # newest must still be v1.7.0
git ls-remote upstream refs/heads/main                            # 83808df9a0993cf49b2ceaa4633c782e550c0180 unless upstream moved
for n in 861 855 854 788 857; do curl -s https://api.github.com/repos/ClickHouse/clickhouse-kafka-connect/pulls/$n | python3 -I -c 'import json,sys;d=json.load(sys.stdin);print(d["number"],d["state"],d["merged"],d["head"]["sha"][:8])'; done
gh api 'repos/ne1r0n/clickhouse-kafka-connect/pulls?state=open' --jq '.[] | [.number,.head.sha[0:8]]'   # quote the URL: zsh expands ?
```
What a result means: origin moved (left count or sha differs): push nothing, merge `origin/custom_build` into the branch (never a rebase), check landed lines against the anchors (Dependabot memory), re-gate, ask again. An upstream tag past `v1.7.0` makes the next sync FUT-01 and is the user's call (publish first or sync first). A merged #861/#855/#854 without a tag changes nothing for the release; record it in the FORK-DELTA syncer paragraph.

### 6.3 CI workflows (read)
- **`tests.yaml`** (name "ClickHouse Kafka Connect Tests CI"): `on: push: branches: ['**']` only, so every branch push runs it once and a tag push does not (CI-08); `concurrency: group: ${{ github.workflow }}-${{ ... github.sha }}`, `cancel-in-progress: true`: two refs at one SHA cancel each other; a tag on an already-pushed SHA cancels the branch run (Phase 11 memory); `timeout-minutes: 45`. It tests ClickHouse `latest` with client V2 only.
- **`release.yaml`** (name Release; run-name `Release ${{ github.ref_name }} (${{ (inputs.dry_run || github.ref != 'refs/heads/custom_build') && 'dry run' || 'publish' }})`): `workflow_dispatch` with boolean `dry_run` (default false); any ref other than `custom_build` is always a dry run. Steps: checkout `fetch-depth: 0` -> JDK 17 temurin -> setup-gradle v6.4.1 -> read `VERSION` (must match `^v[0-9]+\.[0-9]+\.[0-9]+(_gg[0-9]+)?$`) -> build and verify the archive -> check archive -> select baseline (`git tag --merged HEAD --list 'v*' | baseline_tag.py`) and require the allow-list file -> build the baseline archive from a detached worktree of the baseline tag (VERSION there must equal the tag) -> compare (fail on non-zero) -> upload the report artifact `archive-compare-report` (14 days) -> **tests.yaml gate**: `gh api .../workflows/tests.yaml/runs?head_sha=${GITHUB_SHA}&status=success&per_page=1` `total_count` must be >= 1 for a publish (advisory warning on a dry run) -> **tag-absent gate** -> publish: `gh release create "$RELEASE_VERSION" "$ARCHIVE" --target "$GITHUB_SHA" --title "$RELEASE_VERSION" --notes ""`, or for a dry run a draft with `--draft`, a draft-holds-archive check, a no-tag check and a delete step (`if: always()`). Concurrency group `release-publish` for a publish, `release-dry-run-<run_id>` otherwise; `cancel-in-progress: false`. `pre-release.yaml` differs only in names and `--prerelease` (measured by `diff`).
- **Not triggered by this phase:** `nightly-integration-tests.yaml` (schedule `17 3 * * *`; `NIGHTLY_ENABLED` unset, repository variable count 0, measured: scheduled runs end `skipped`), upstream's `integrationTests.yaml` (schedule `00 05 * * *`, red by inheritance, cloud job lacks secrets), `increment-version.yml` (dispatch; the fork bumps through the script locally instead).

### 6.4 The sequence with each user-owned gate
| # | Who | Step | Gate / precondition |
|---|-----|------|---------------------|
| 0 | O | read-only recheck (6.2) | none |
| 1 | O/E | Plans 1-5 locally; nothing pushed | executors never run a step below |
| 2 | O | approval bundle presented verbatim | **G3: the user's direct "go"** (their own message, recorded verbatim with who received it; a relayed "go" is refused). Authorises U1 and U2 only |
| 3 | O (or user) | **U1:** `X0=$(git rev-parse sync/v1.7.0)`; `git diff --quiet <gated head> "$X0" -- . ':(exclude).planning'` and `-- .planning/scripts` both exit 0; `git ls-remote origin \| grep "$X0"` empty; `git ls-remote --heads origin sync/v1.7.0` empty; `git push origin "${X0}:refs/heads/sync/v1.7.0"` (brace the variable: zsh `:r`); wait for exactly one green `tests.yaml` run, polling `gh run view` in a bounded loop (no `timeout`) | **A push of any branch** (User-Owned Gates). Publishes all unpublished commits (220 now, the `.planning` history included); deleting the branch later does not unpublish them |
| 4 | O (or user) | **U2:** `gh workflow run release.yaml -R ne1r0n/clickhouse-kafka-connect --ref sync/v1.7.0 -f dry_run=true` (UI: "Use workflow from" the scratch branch, never `custom_build`); read run id, `headSha`, notices `Baseline: v1.7.0_gg8 is compared with v1.7.0_gg7 under .planning/scripts/confluent-archive-allowlist-v1.7.0_gg8.txt`, `Archive compare: exit 0, 0 unallowed, 0 stale rows`, artifact last line `RESULT: PASS`, no draft or tag left | same G3; **baseline must read `v1.7.0_gg7`** (loud item 1) |
| 5 | O | the user's own direct "publish"; recheck origin still `94834763`, date vs CHANGELOG heading, upstream | **G4: "publish"** authorises building `M'` and its push only |
| 6 | O | build `M'` detached: `git worktree add --detach /tmp/<new-dir>/cb 9483476322e0d331e81c5b92ac266a18f0f01450`; in it `git --attr-source="$(git hash-object -t tree /dev/null)" merge --no-ff -F <repo>/.planning/phases/49-release-publication/49-MERGE-MESSAGE.txt <X0>`; `git update-ref refs/gsd/release-m-v1.7.0_gg8 "$M"`; checks: two parents (`94834763`, `X0`), `git diff --quiet HEAD <X0>`, subject `Merge sync/v1.7.0: release v1.7.0_gg8`, `VERSION` reads `v1.7.0_gg8`, `.gsd-ws-arg` absent, rollback block prints `ok` on a further throwaway copy | tell the user `X0` and `M'`; `M'` exists only in this repository |
| 7 | O (or user) | **U3:** `git push origin "${M}:refs/heads/custom_build"` (no `--force`, `--tags`, `--all`) from the repository that built `M'`; origin must still be `94834763` (a fast-forward from it); exactly one `tests.yaml` run on `M'` | **The `--no-ff` merge into `custom_build` and its push** (RLS-05) |
| 8 | O | read: `gh api 'repos/ne1r0n/clickhouse-kafka-connect/actions/runs?head_sha=<M>' --jq '[.workflow_runs[] \| [.name,.path,.event,.conclusion]]'`: one `tests.yaml` run, success, still one after 120 s, no `release.yaml` run; `git ls-remote origin refs/heads/custom_build` equals `M'`; `v1.7.0_gg8` absent | none |
| 9 | O (or user) | **U4:** dispatch `release.yaml` from `custom_build` with `dry_run` not ticked (run-name must read `Release custom_build (publish)`) | **G5: the publish run (`release.yaml`, `dry_run` not ticked)**; in gg7 the user overrode the runbook rule that U4 stays the user's, so say who runs it at the bundle |
| 10 | O | read: release (id, `tag_name`, author, draft, assets with uploader), `git/ref/tags/v1.7.0_gg8` (object type `commit`, lightweight), the run's jobs (`Publish release` success); write `49-RELEASE-RECORD.md` | none |
| 11 | O (or user) | **U5:** `git push origin refs/tags/pre-v1.7.0_gg8` by name (target per Open Question 1) and `git push origin --delete refs/heads/sync/v1.7.0` | tag push and branch deletion are pushes: the user's word |
| 12 | O | **U6, only after a separate confirmation in the user's own words (RLS-06):** one push deleting both branches by name: `git push origin --delete refs/heads/scratch/clt-07-client-0.11.0 refs/heads/scratch/clt-07-prebump-0.9.6`; then `git ls-remote origin` lists neither | RLS-06 gate; both tips are ancestors of `HEAD` and of `origin/custom_build` (measured), so deletion loses no commit |
| 13 | O | final read-backs; optional local tail: fetch and a no-change merge so the local branch contains `M'` (never a rebase); `.planning` records after `X0` stay local | none |

**Constraints.** `custom_build` is the GSD protected base branch: no executor commits there and the local ref is never moved or checked out; the throwaway worktree is detached. A tag push joins `tests.yaml`'s SHA-keyed group, so push `pre-v1.7.0_gg8` only after the `custom_build` run finished (and it lands on a different SHA anyway). `M'` is a real merge so its SHA differs from `X0`. The merge must be `--no-ff`, non-force; GitHub does not enforce protection (`protected: false`), so the rules are procedural. Pushing `X0` also publishes the local `.planning` tail; uncommitted `.planning/config.json` and `state.json` are not pushed.
**What a green dry run proves:** the full-history checkout, the baseline worktree build with the tag's wrapper, the strict compare under the gg8 list, the report upload, a draft with the archive created, checked and deleted, no tag for the draft. It does not prove the publish step, the tests gate on `M'` (advisory warning only on a dry run), or the tag-absent gate in publish mode.
**Contingencies (from gg6/gg7, still valid):** C-1 `custom_build` moved (absorb by merging `origin/custom_build`, re-gate, re-ask); C-2 CI-only defect before the compare (no re-dispatch; a gap, no `.github/` edit allowed); C-3 compare reports `UNALLOWED` or `STALE-ROW` on the runner (investigate against the artifact; a corrected list needs a new local gate and review); C-4 red or not-exactly-one `tests.yaml` run on `X0` or `M'` (stop; forward-only rollback, never force-push); C-5 accidental publish from the wrong ref (dry run harmless); C-6 failed publish run (read state, the user chooses); C-7 date mismatch; C-8 a hand-pushed `v1.7.0_gg8`.
**Safety note for this research's own probes:** `git merge-tree --write-tree` and `git commit-tree` each wrote an unreferenced object; no ref, index or worktree changed; `git gc` will drop them. `git status` after the probes still shows only the two orchestrator-managed files.

## Common Pitfalls

### Pitfall 1: The runner picks the gg6 baseline on the scratch branch
**What goes wrong:** the dry run compares gg6 to gg8, fails with ~120 `UNALLOWED` and a wall of `STALE-ROW`s, and the first impression is a bad allow-list. **Why:** tag `v1.7.0_gg7` is not merged into `sync/v1.7.0` (measured). **Avoid:** merge `origin/custom_build` into the branch before the gates; assert in Plan 1 that the baseline pipe prints `v1.7.0_gg7` on the head to be gated; assert it again on `D`. **Warning sign:** notice `Baseline: ... is compared with v1.7.0_gg6`.

### Pitfall 2: Rollback wording copied from gg7
**What goes wrong:** the note says the pre-tag "is also tag `v1.7.0_gg7`", which is false for `baa404cd` (measured). **Avoid:** decide Open Question 1; rehearse the block on `D` and state exactly what the tag tree contains.

### Pitfall 3: Allow-list copied or written early
**Avoid:** regenerate from the final tree in Plan 4 after the docs freeze; expect 17 and measure; run the three mutations.

### Pitfall 4: Scope errors and over-strong labels in the note
**What goes wrong:** gg6 had 28 warnings and gg7 8, almost all a V2 fact stated for every connector or `measured` on a claim that was read or derived. **Avoid:** an `Applies when` value in every row from the first draft; the T3/T4 V1 bound states the 30 s library limit; the T6 progress escape says live-measured only for the post-send case and stub-only for the during-send cut.

### Pitfall 5: Overwriting history in the evidence
**Avoid:** failed entries are never edited; a re-run is a new `E-NN` (the evidence helper accepts only `E-NN` headings); keep the pending-tail entries pending.

### Pitfall 6: Freeze-rule cost when a document changes after the gates
**What goes wrong:** gg7's one late documentation change re-opened the allow-list, testUnit, two suites, the invariants script (~45-50 min alone) and the rehearsal, about 2.5 hours, and voided the approval. **Avoid:** settle Open Questions 1-5 and the measure-versus-derive questions before the freeze; do the date edit before the freeze or before U1. Technically a docs-only edit moves no gate (measured), so the re-run is a judgement call for the user.

### Pitfall 7: `latest` drift and OpenJ9 crashes read as red tests
**Avoid:** read the image digest before run 1, after run 1 and after run 2 (all must equal `739e099a...`), `SELECT version()` at each (`26.10.3.7` at Phase 48); a crash is exit 255 with no XML and an ~8 GB `core.*.dmp`: read `javacore.*.txt`, delete the dump files by name (never `git clean`), `./gradlew --stop`, re-run once, record both; if it recurs run both suites with `-PjavaVersion=21`. Swap is 96 % used now.

### Pitfall 8: Overlapping Gradle jobs
**Avoid:** run `testUnit`, each suite and the invariants script strictly one after another; the invariants script alone. Always `env -u TESTCONTAINERS_HOST_OVERRIDE`.

### Pitfall 9: Same SHA on two refs / tag push right after a branch push
**Avoid:** `M'` is a real merge; push tags only after the branch run is finished and by name.

### Pitfall 10: `custom_build` moves under you
The user merges Dependabot PRs on origin directly; none are open now, but a new one can arrive. Pre-check before every U step; contingency C-1; the bundle tells the user not to merge any PR before `M'` lands.

### Pitfall 11: Relayed approvals and notifications
Executors refuse approvals relayed by another agent; a task notification is not an approval. Only the user's own message to the orchestrator counts, recorded verbatim. gg7 note: the user answered the bundle through the orchestrator's question tool, and the orchestrator ran U1-U5 on the user's direct word per step (E-29 to E-35).

### Pitfall 12: Reviewer fixes applied unchecked; heading-rule drift
See 5.3.

### Pitfall 13: Hygiene helper false positive
`44-hygiene-check.py` flags the plain word "stand" in `.planning` text; use "hold". A `.planning` rewording is outside the freeze scope.

## Code Examples

### Bump and baseline
```bash
python3 -I .github/scripts/bump_version.py gg                     # -> v1.7.0_gg8; edits VERSION and the VERSION anchors row
git add VERSION .planning/scripts/fork-anchors.txt && git commit -m "chore: bump VERSION to v1.7.0_gg8 through bump_version.py"
git tag --merged HEAD --list 'v*' | python3 -I .github/scripts/baseline_tag.py v1.7.0_gg8   # must print v1.7.0_gg7 (prints v1.7.0_gg6 today)
```
### Tree-neutral merge of the published tip (local only)
```bash
git merge-tree --write-tree origin/custom_build HEAD              # exit 0, tree == HEAD^{tree} (measured: fdb32cc5...)
git merge --no-ff -m "Merge origin/custom_build (v1.7.0_gg7 release merge) into sync/v1.7.0" origin/custom_build
git diff --quiet HEAD^1 HEAD -- . && echo "tree equals the first parent"
```
### Compare proof
```bash
python3 -I .github/scripts/compare_confluent_archives.py <gg7-export>/build/confluent/clickhouse-kafka-connect-v1.7.0_gg7.zip \
  <tip-export>/build/confluent/clickhouse-kafka-connect-v1.7.0_gg8.zip .planning/scripts/confluent-archive-allowlist-v1.7.0_gg8.txt   # exit 0, RESULT: PASS
```
### Dry-run merge `D` and read-backs (orchestrator, read-only except the throwaway worktree)
```bash
git worktree add --detach /tmp/<new-dir>/cb 9483476322e0d331e81c5b92ac266a18f0f01450
git -C /tmp/<new-dir>/cb --attr-source="$(git hash-object -t tree /dev/null)" merge --no-ff -F <message file> <X0>
gh api repos/ne1r0n/clickhouse-kafka-connect/releases --jq '.[] | {id, tag_name, author: .author.login, draft, assets: [.assets[]|{name,size,uploader: .uploader.login}]}'
gh api repos/ne1r0n/clickhouse-kafka-connect/git/ref/tags/v1.7.0_gg8
gh api --allow-escape-sequences repos/ne1r0n/clickhouse-kafka-connect/actions/jobs/<job id>/logs | sed 's/\x1b\[[0-9;]*m//g' | grep -E 'Baseline|Archive compare'
```

## State of the Art

| Old | Current | Impact |
|-----|---------|--------|
| gg7: pre-tag = published tip = previous release tag | gg8: pre-tag `baa404cd` on the branch; published tip is a merge the branch lacks | note wording, baseline pipe, divergence check all differ (loud items 1-3) |
| gg7: left count 0 expected | gg8: left count 1 (the gg7 merge) | ROADMAP step 1 wording is wrong for gg8 |
| gg7 allow-list 18 rows (gson, ProxySinkTask, Processing, ...) | gg8 projected 17 exact rows, no wildcard | new file, not a copy |
| Phase 17: no `gh`, user-made token for job logs | authenticated `gh api --allow-escape-sequences .../logs` works | no token needed for runner log reads |
| gg7 review: 5 rounds | D-6: 3 rounds, a fourth needs the user | ask after round 3 |

## Assumptions Log

| # | Claim | Section | Risk if wrong |
|---|-------|---------|---------------|
| A1 | The allow-list will have 17 exact rows and totals `changed 14 same 7498` (not built this session: no Gradle) | 3 | Plan 4 measures; any other count is a finding. Low |
| A2 | A tree-neutral merge of `origin/custom_build` before the gates does not disturb the tail gate (`verify-merge-invariants.sh` in tail form, range `12e2cf17..H`, one extra tree-neutral merge in range) | 6, Open Question 2 | Not run (it needs the merged head and is a long git-only script). If it fails, use the cumulative form or place the merge after the gates (the pushed tree equals the gated tree either way) |
| A3 | The user prefers keeping the scratch dry run (U2) as in gg7 although the ROADMAP recipe omits it | 6, Open Question 2 | Without U2 the first compare on a runner is the publish run, which fails before `Publish release` if wrong |
| A4 | The orchestrator, not the user, runs the origin writes on the user's direct word per step (gg7 practice) | 6.4 | The handoff files are the wrong shape; ask at the bundle |
| A5 | GitHub marks nothing to clean up (no open PRs), so no PR step is needed after `M'` | 6.1 | None found; a new Dependabot PR would add one |
| A6 | Testcontainers does not re-pull a present local `latest` (gg7 recorded three equal digest reads) | 7.3 | A suite runs a newer build; the digest reads show it |
| A7 | Tail-form invariants gate is sufficient for "the anchors gate passes" (RLS-04 does not name a form) | Standard Stack | The user may want the cumulative gg7 form (~45 min) |
| A8 | The host can run both suites on the default OpenJ9 17 JVM (it did in Phase 48 at about the same swap level) | 7.3 | Crash; escape `-PjavaVersion=21`, a user judgement item at the bundle |

## Open Questions

1. **Which commit does `pre-v1.7.0_gg8` name, and what does the note say?**
   - Known (measured): local tag `baa404cd`; published tip `94834763`; trees equal outside `.planning`, 148 `.planning` files differ; gg7's pre-tag was the published tip.
   - Unclear: whether to retag locally to `94834763` (gg7 meaning, needs the tail gate re-run on the final head, which Plan 4 does anyway) or keep `baa404cd` and word the note exactly.
   - Recommendation: retag to `94834763` before the gates (one local command, user's word because it moves a tag recorded in Phase 45-48 evidence), so the rollback target equals tag `v1.7.0_gg7` and `git read-tree -u --reset pre-v1.7.0_gg8` restores the published tree exactly; record the retag in the evidence. If the user declines, state `baa404cd` and its tree relation in the note and the D3 row.
2. **Keep the scratch dry run, and merge `origin/custom_build` into the branch first?**
   - Known: without the merge the runner's baseline is gg6 (measured); with it, gg7; the merge is tree-neutral (measured `merge-tree`).
   - Recommendation: yes to both; do the merge as the first local change of Plan 1 so one head is gated and pushed; ask at the bundle whether U2 is wanted (the ROADMAP recipe lists no dry run).
3. **Which invariants form is the RLS-04 "anchors gate"?** Recommendation: the tail form once on the explicit final SHA `H` (it was the gate of Phases 45-48, `anchor_rows=218`), plus the anchors mirror `--rows 218`; offer the cumulative form to the user if they want gg7 parity.
4. **Where do the gate totals go in FORK-DELTA?** Known: no slot exists. Recommendation: refresh the shortstat table, paragraph and commands (section 4) and add one measured sentence at the end of "The raw numbers" with testUnit, suite, skips and anchors totals, so D-5 and E-18 item 6 are met literally; if the user disagrees, state in the evidence that the section holds no totals.
5. **CHANGELOG date policy:** keep the freeze date as a recorded deviation (gg6/gg7 precedent), or set the UTC merge date in a one-line commit before U1. Recommendation: date-fix before U1 if the push day is known; it moves no gate (measured), but the reviewed text changes by one line, so tell the reviewer or run the check yourself and record it.
6. **The 128/143 test-tree figure:** correct it to the recount (115) in the final pass, or leave and brief the reviewer (loud item 6). Recommendation: recount with the script of this research and correct, since a reviewer will re-derive it.
7. **Who runs the origin writes?** Default per gg7 practice: the orchestrator, on the user's direct word per step; ask once at the bundle (A4).

## Environment Availability

| Dependency | Required By | Available | Version | Fallback |
|------------|------------|-----------|---------|----------|
| Docker via colima profile `vz` | two full suites | yes (running) | client 29.8.2 | `colima start -p vz` (never bare `colima start`) |
| ClickHouse `latest` image | suites | yes, local | `sha256:739e099a...b1ce`, created 2026-10-26T00:06:54Z; same digest on Docker Hub now | none; do not `docker pull` |
| JDK 17 toolchain / JDK 21 | build, suites | toolchain per `build.gradle.kts`; `-PjavaVersion=21` property exists | n/a | `-PjavaVersion=21` |
| Gradle wrapper | archive builds, `clean generateVersionFile`, suites | yes (not run this session) | wrapper in tree | none |
| python3 | scripts | yes | 3.14.8 | none |
| `gh` authenticated | read-only GETs, job logs | yes (`ne1r0n`) | 2.102.0 | unauthenticated `curl` (60/h) |
| git | merges, worktrees, `--attr-source` | yes | 2.50.1 | none |
| Network (github.com, api.github.com, Docker Hub) | reads | yes | n/a | none |
| Host memory | JVM stability | swap 96 % used | n/a | `./gradlew --stop`, `-PjavaVersion=21` |

**Missing dependencies with no fallback:** none. Origin, GitHub Actions and the user's approvals are inputs, not tools.

## 7. Risks, plan decomposition, and gate commands

### 7.1 Risks and pitfalls summary (from the memory notes and gg7)
Baseline trap (loud 1); rollback tag meaning (loud 3); stale local `custom_build`; divergence count 1 not 0; relayed approvals; CHANGELOG date and UTC midnight; Freeze-rule cost; reviewer heading rule and agent attestation; reviewer fix text can be wrong; `latest` drift; OpenJ9 GPF under swap pressure; overlapping Gradle jobs; `${M}:refs` zsh modifier; no `timeout` binary; `printf` not `echo "==="`; `h` alias; `gh` URLs with `?` need quotes; tag push cancelling a branch run; Dependabot merges on origin; publishing exposes the `.planning` tail (220 commits now); hygiene helper "stand"; STATE.md must be committed by the orchestrator after each plan (executors never stage it); haiku plan-checker is grep-level; the gg7 context budget (about 650k tokens by the gate) argues for a `/clear` and resume at the approval bundle.

### 7.2 Proposed plan decomposition (all local; waves sequential; E = executor-safe, O = orchestrator-inline)
| Plan | Wave | Who | Content | Requirements |
|------|------|-----|---------|--------------|
| 49-01 | 1 | E (Docker-free), with O for the user answers of Open Questions 1, 2, 5 | Pre-flight (6.2 block, expected values per this research); copy and adapt helpers (`--rows 218`, `--max-rounds`, gg8 note IDs); tree-neutral merge of `origin/custom_build` (if approved); optional retag (if approved); `bump_version.py gg` and ONE commit `{VERSION, fork-anchors.txt}`; `clean generateVersionFile` check; baseline pipe prints `v1.7.0_gg7`; CHANGELOG gg8 section (1.2); FORK-DELTA final pass with raw numbers measured once (4); hygiene scan | RLS-01 |
| 49-02 | 2 | E (Docker-free) | `docs/ROLLOUT-v1.7.0_gg8.md` from 5.2 with an evidence map entry per row; cheap re-measures (source facts, rollback block on dry-run merge `D0` of the committed tip onto `94834763`); note checker (rows, labels, forbidden, scope) | RLS-03 (part) |
| 49-03 | 3 | **O only** | Reviewer rounds (5.3), fixes, dispositions, docs-frozen proof; after round 3 non-empty ask the user (a fourth round is theirs) | RLS-03 |
| 49-04 | 4 | E (live) with O reading results | **Allow-list generated last** (3) and committed; gates on `H`: testUnit with Docker unreachable (expect 2325 / 2 skipped), two `--rerun` suites on one pinned `latest` (expect 2928 / 18 skipped derived), anchors mirror `rows=218 bad=0`, tail gate once on `H`; scope check: outside `.planning` the diff is exactly four files (`VERSION`, `CHANGELOG.md`, `docs/FORK-DELTA.md`, `docs/ROLLOUT-v1.7.0_gg8.md`), and under `.planning/scripts` exactly `fork-anchors.txt` and the new allow-list | RLS-02, RLS-04 |
| 49-05 | 5 | E (rehearsal, bundle writing) with O presenting | Dry-run merge `D` of the final tip onto `94834763` in a throwaway detached worktree with the real merge message, rollback block unchanged on `D` (prints `ok`), recheck block, date comparison, `49-MERGE-MESSAGE.txt`, `49-APPROVAL-BUNDLE.md` (valid for one head, Freeze rule stated), handoff files U1-U6, runbook, local close record with the exact ROADMAP/REQUIREMENTS wording changes (ROADMAP step 1 left count; D-2 17 rows) | RLS-04 (rehearsal) |
| tail | 6 | **O inline, user-gated** | G3 "go" -> U1, U2; G4 "publish" -> build `M'`, U3; G5 -> U4; U5; separate RLS-06 confirmation -> U6; read-backs; release record; then verification, `phase.complete`, milestone close | RLS-05, RLS-06 |

A red anywhere in Plan 4 is recorded with its evidence and returned: this phase never fixes code, a test or a workflow, and never edits an allow-list row to turn a red green. A defect found goes to its phase as a gap (STATE decision, Phase 29 D-05). Each plan commits STATE.md via the orchestrator afterwards.

### 7.3 Gate commands and expected totals (derived/read; to be re-measured, not copied)
```bash
./gradlew --stop; sysctl vm.swapusage
rm -rf build/test-results/testUnit
DOCKER_HOST=unix:///nonexistent.sock env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain testUnit --rerun > $E/gate-testunit.log 2>&1      # expect 2325 tests, 0 failures, 2 skipped (E-19)
rm -rf build/test-results/test
CLICKHOUSE_VERSION=latest CLIENT_VERSION=V2 env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain test --rerun > $E/suite1.log 2>&1   # twice; expect 2928 tests, 18 skipped, OK skips: named=7 state_mismatch_cases=11 unexpected=0
python3 -I 49-evidence-check.py junit|skips|failed build/test-results/<task>
python3 -I 49-evidence-check.py anchors .planning/scripts/fork-anchors.txt --rows 218                       # expect rows=218 bad=0
docker image inspect --format '{{index .RepoDigests 0}}' clickhouse/clickhouse-server:latest                # before run 1, after run 1, after run 2: all equal
docker run --rm --entrypoint clickhouse clickhouse/clickhouse-server:latest local --query 'SELECT version()' # 26.10.3.7 at Phase 48
PRE_TAG=pre-v1.7.0_gg8 UPSTREAM_REF=83808df9a0993cf49b2ceaa4633c782e550c0180 CONFLICTED_PATHS_FILE=.planning/scripts/conflicted-paths-v1.7.0-tail-3.txt bash .planning/scripts/verify-merge-invariants.sh 12e2cf17cca9f5b17917dd9aceaa38b44f3f8b10 <H>   # once, alone, explicit SHA
git diff --name-only a968e486 <H> -- . ':(exclude).planning'   # exactly: CHANGELOG.md VERSION docs/FORK-DELTA.md docs/ROLLOUT-v1.7.0_gg8.md (plus nothing else)
```
Totals: testUnit 2325 is measured (E-19, tree unchanged since); the suite 2928 is derived (E-17's 2923 + the 5 cases of E-19) and equals gg7's 2425 + 503 added since. Do not run `spotlessApply` or `spotlessCheck` as a gate (46 pre-existing violations). `integrationTest` is not re-run in this phase (needs a Connect stack; the gg7 run of record was 55 passed on `ClickHouseSinkConnectorIntegrationTest`); state that in the bundle so the user can overrule (an overrule re-opens Plan 4 with nothing pushed). If `PRE_TAG` is retagged (Open Question 1), re-run the tail gate on the final head with the new tag.

## Validation Architecture

Skipped: `workflow.nyquist_validation` is `false` in `.planning/config.json` [VERIFIED: config.json read this session, `"nyquist_validation": false`]. The gate commands and totals are in 7.3.

## Security Domain

`workflow.security_enforcement` is `true`, `security_asvs_level` 1, `security_block_on` `high` [VERIFIED: .planning/config.json read this session].

### Applicable ASVS Categories
| ASVS Category | Applies | Standard Control |
|---------------|---------|-----------------|
| V2 Authentication, V3 Session | no | none in scope |
| V4 Access Control | yes (process) | only the user's direct word authorises an origin write; executors refuse relayed approvals; the release job's `GITHUB_TOKEN` has `contents: write` on that job only [VERIFIED: release.yaml `permissions: contents: read` at workflow level and `contents: write`, `actions: read` on `build_release`] |
| V5 Input Validation | yes | the compare script escapes archive entry names before logging; the allow-list loader refuses catch-all and leading-wildcard rows; `dry_run` is a typed boolean input |
| V6 Cryptography | no new use | archive digests are SHA-256, read from the run or `gh api`, never hand-computed |
| V10/V14 Supply chain and configuration | yes | no package added; actions pinned to full SHAs (guard tests); the archive gains no new third-party entry (projected: connector classes only) |

### Known Threat Patterns
| Pattern | STRIDE | Standard Mitigation |
|---------|--------|---------------------|
| Hosts, connector or deployment names, secrets in published docs and `.planning` | Information disclosure | `49-hygiene-check.py` (default and `--strict`) over every file added or modified since `94834763`; angle-bracket values only; `gh` tokens never written |
| Accidental publish or wrong ref | Tampering | `run-name`, dry-run-from-any-other-ref rule, tag-absent gate, tests gate, separate handoff per step |
| Force push, `--tags`, `--all`, `--mirror` | Tampering | exact refspecs in handoffs; prohibitions kept |
| Rollback advice that damages state (gg3 `TRUNCATE`) | Tampering | reviewer confirmation; the bundle check that no document tells anyone to empty a table |
| Record data in logs or docs | Information disclosure | U2/U3 rows state the value-free policy; the reviewer confirms the ERROR-line text is quoted accurately |
| Loss of unpublished work by deleting scratch refs | Tampering/repudiation | both `scratch/clt-07-*` tips are ancestors of `origin/custom_build` (measured) before deletion |
| Publishing the local `.planning` tail | Information disclosure | the bundle prints the live count (220) and says deletion of a branch does not unpublish; hygiene scan precedes U1 |

## Sources

### Primary (HIGH confidence, opened or run this session)
- `.planning/phases/49-release-publication/49-CONTEXT.md`, `.planning/ROADMAP.md` (Phase 49 and User-Owned Gates, Structure Notes, Sync Mechanics), `.planning/REQUIREMENTS.md`, `.planning/STATE.md`, `.planning/config.json`, `./CLAUDE.md`, `AGENTS.md`, `.claude/CLAUDE.md`, the memory notes (`gsd-phase15-review-gate-gotchas`, `gsd-phase16-publication-gotchas`, `gsd-phase11-orchestration-gotchas`, `gsd-phase17-ci-log-access`, plus `gsd-phase39-44-orchestration-notes`, `gsd-code-review-fix-gotchas`, `dependabot-merges-on-custom-build`, `openj9-mockito-test-jvm-crash`, `zsh-var-colon-modifier-in-git-refspec`)
- Phase 48 `48-COMMAND-EVIDENCE.md` E-14, E-15, E-17, E-18, E-19; Phase 47 `47-COMMAND-EVIDENCE.md` E-11; Phase 45-48 SUMMARY and VERIFICATION headers
- gg7 precedent under `.planning/milestones/v1.7.0_gg7-followup-phases/44-release-publication/`: `44-RESEARCH.md`, SUMMARYs, `44-REVIEW.md`, `44-APPROVAL-BUNDLE.md`, `44-ORCHESTRATOR-RUNBOOK.md`, `44-RELEASE-RECORD.md`, `44-REOPEN-HANDOFF.md`, `44-USER-DISPATCH-1`/`-4`, helper docstrings, evidence index; `docs/ROLLOUT-v1.7.0_gg7.md` (all 212 lines)
- Code and documents: `.github/workflows/release.yaml` (lines 1-245), `pre-release.yaml` (diff), `tests.yaml` (header, triggers), `.github/scripts/{bump_version,baseline_tag,compare_confluent_archives}.py`, `.planning/scripts/{fork-anchors.txt,verify-merge-invariants.sh,confluent-archive-allowlist-v1.7.0_gg7.txt}`, `build.gradle.kts` (48, 395-470, 500-540), `docs/FORK-DELTA.md` (lines 1-27, 119-201, 242-316), `CHANGELOG.md` (lines 1-215), `docs/ROLLOUT-v1.7.0_gg6.md` (lead), `83808df9:CHANGELOG.md`, `9bed6f79:CHANGELOG.md`
- Commands run (read-only): `git ls-remote origin` and `upstream`, `git rev-list/merge-base/diff/log/show/tag/cat-file/worktree list/status`, `git merge-tree --write-tree`, `git commit-tree` (dangling probe), `gh api` GETs (pulls, releases, runs, jobs, jobs logs, branches, variables), `curl` GETs (upstream PRs 788, 855, 854, 857, 861; Docker Hub `latest`), `docker image inspect`, `colima status -p vz`, `sysctl vm.swapusage`, two `python3 -I` recount scripts under `/tmp/gsd49r/`

### Secondary and tertiary
- None. No web documentation was needed; nothing depends on a library API.

## Metadata

**Confidence breakdown:**
- Repository, origin, upstream and workflow facts: HIGH (read or measured this session)
- Allow-list projection (17 rows): MEDIUM (derived; no archive was built, by instruction)
- FORK-DELTA numbers: HIGH for the shortstat and recount figures (measured), MEDIUM for where the gate totals belong (a decision)
- Publication sequence: HIGH for the local and workflow facts; MEDIUM for the runner half (the compare with a gg7 baseline has not yet run on a runner for gg8)
- Pitfalls: HIGH, each from a measured state or a recorded incident

**Research date:** 2026-11-06
**Valid until:** about 7 days, or sooner if origin `custom_build` moves, Docker Hub `latest` changes, upstream tags past `v1.7.0` or a PR head changes (re-run the 6.2 block).
