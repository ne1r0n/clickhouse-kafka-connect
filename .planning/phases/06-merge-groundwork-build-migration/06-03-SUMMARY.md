---
phase: 06-merge-groundwork-build-migration
plan: 03
subsystem: infra
tags: [git, merge-checklist, documentation, process-gate]

# Dependency graph
requires:
  - phase: 06-01
    provides: "merge commit 673d29c3, pre-v1.6.0-sync tag, conflicted-paths.txt (40 lines), verify-merge-invariants.sh"
  - phase: 06-02
    provides: "commit b582a44b (fork build additions re-applied), the four D-12 resolved-version facts, VERSION=v1.6.0_gg1"
provides:
  - ".planning/MERGE-CHECKLIST.md — Section 1 (40 rows, one per conflicted path, 5 done/35 pending), Section 2 (8 merged-clean-but-fork-logic rows), and ## Recorded Facts"
  - "SYNC-04's resolved-client-version fact and SYNC-05's closed production-config audit, both on the record with source labels"
  - "full-range re-proof of D-02/SYNC-06 invariants across pre-v1.6.0-sync..HEAD (588 commits)"
affects: ["07-*", "08-*", "09-*", "10-*", "11-*"]

# Actuals (#2632)
actuals:
  tokens: 5000
  tasks: 3
  commits: 1

# Tech tracking
tech-stack:
  added: []
  patterns:
    - "Checklist-as-gate pattern: a status column (pending/done) turns a one-time inventory document into a per-phase completion gate that a later phase (11) can grep for zero-pending before publication"
    - "Corrected-merge-count invariant: for a history-preserving big-bang merge, 'single identifiable merge commit' must be checked as 'exactly one merge commit in the range that is not already an ancestor of the merged-in tag', not 'exactly one merge commit total' — a 581-commit open-source branch inevitably carries its own PR-merge history along"

key-files:
  created:
    - .planning/MERGE-CHECKLIST.md
  modified: []

key-decisions:
  - "Filed Processing.java in Section 2 (the 'merged clean, READ ANYWAY' list) with an explicit correction note, rather than omitting it or silently trusting research prose: research/SUMMARY.md's Executive Summary claims Processing.java merges with no conflict marker alongside Utils.java, but the generated conflicted-paths.txt (the authoritative source per D-09) shows Processing.java IS one of the 40 conflicted paths (a mechanical Spotless-only content conflict). Both the accurate Section 1 row and a corrective Section 2 note now exist, so Phase 8 is not misled by the inherited research error."
  - "Corrected the plan's task-3 full-range single-merge-commit check: 'git rev-list --count --merges pre-v1.6.0-sync..HEAD = 1' is unsatisfiable by design once upstream's real 581-commit, history-preserving branch is merged in — it inevitably carries its own ~138 PR-merge commits. Verified instead that exactly one merge commit in the range (673d29c3) is NOT already an ancestor of v1.6.0 — i.e., our own merge is the only new merge commit; all other 138 merges pre-exist in upstream's own history. This does not touch git history or any repository file — it is a corrected reading of an already-true invariant, documented as a deviation below."

patterns-established:
  - "Section 1 generation discipline: every cell sourced from conflicted-paths.txt (path/order), git merge-tree's own content/modify-delete classification (conflict type), and REQUIREMENTS.md/ROADMAP.md phase ownership (owning phase) — never hand-transcribed from research prose, per D-09/T-06-10."

requirements-completed: [SYNC-03, SYNC-04, SYNC-05, SYNC-06]

coverage:
  - id: D1
    description: "Section 1 covers all 40 conflicted paths from conflicted-paths.txt, same sorted order, each with conflict type/resolution shape/owning phase/fork behaviour/adjudicating test/status, no empty cells"
    requirement: SYNC-03
    verification:
      - kind: other
        ref: "task 1's three <verify> blocks (HEADER_OK, EVERY_PATH_HAS_ONE_ROW, STATUS_AND_TYPES_OK) plus manual order-diff, distinct-row, resolution-shape and owning-phase-range checks — all re-run and passing"
        status: pass
    human_judgment: false
  - id: D2
    description: "Section 2 lists 8 merged-clean-but-fork-logic paths (Utils.java, Processing.java with correction note, Record/Data/StructToJsonMap, 5 convertors as one row, 2 state providers as one row, settings.gradle.kts), preamble states Utils/Processing reading is a hard Phase 8 requirement"
    requirement: SYNC-03
    verification:
      - kind: other
        ref: "task 2's two <verify> blocks (SECTION2_OK, RECORDED_FACTS_OK), re-run and passing"
        status: pass
    human_judgment: false
  - id: D3
    description: "Recorded Facts closes SYNC-04 (resolved client version 0.9.6 from the dependency graph, not the catalog literal) and the other three D-12 facts (4.1.1, 2.0.17, at.yawk.lz4)"
    requirement: SYNC-04
    verification:
      - kind: other
        ref: "grep for '0.9.6', '4.1.1', '2.0.17', 'at.yawk.lz4' in .planning/MERGE-CHECKLIST.md — all present"
        status: pass
    human_judgment: false
  - id: D4
    description: "Recorded Facts closes SYNC-05 on both halves (bufferCount repo-derived structurally-unreachable, enableDbTopicSplit operator-confirmed not-applicable) with source labels and 'no operator follow-up remains' stated; no JSONP-01/R-01 disposition decided anywhere"
    requirement: SYNC-05
    verification:
      - kind: other
        ref: "grep for 'repo-derived', 'operator-confirmed', 'follow-up remains' present; grep for 'keep-or-drop'/'R-01' shows no disposition decided"
        status: pass
    human_judgment: false
  - id: D5
    description: "Phase 6's 5 build/infra rows flipped to done (owning phase 6 each), all other 35 Section 1 rows and 8 Section 2 rows remain pending; checklist committed on sync/v1.6.0 in a single .planning/-only non-merge commit"
    requirement: SYNC-03
    verification:
      - kind: other
        ref: "grep -o '| done |' count = 5, each with owning-phase 6; commit ae86b603 subject 'docs(06):', touches only .planning/, 2-parent (non-merge)"
        status: pass
    human_judgment: false
  - id: D6
    description: "Both merge invariants (D-02 byte-identity, SYNC-06 no-conflict-marker) re-proven across the full pre-v1.6.0-sync..HEAD range (588 commits, exactly one NEW merge commit); build gate (gradlew help) still green; custom_build unmoved and unpushed"
    requirement: SYNC-06
    verification:
      - kind: other
        ref: "verify-merge-invariants.sh 673d29c3 HEAD exits 0; git rev-list --count pre-v1.6.0-sync..HEAD = 588; gradlew help BUILD SUCCESSFUL; custom_build SHA == pre-v1.6.0-sync SHA; branch.sync/v1.6.0.remote unset"
        status: pass
    human_judgment: false

duration: ~15min
completed: 2026-09-18
status: complete
---

# Phase 6 Plan 3: Merge Resolution Checklist Summary

**Generated `.planning/MERGE-CHECKLIST.md` — a 40-row, generated (not transcribed) per-path resolution gate for Phases 7-11, an 8-row "read anyway" list correcting a research-prose error about `Processing.java`, and a `## Recorded Facts` section closing SYNC-04/SYNC-05 — then re-proved both merge invariants across the full 588-commit phase range.**

## Performance

- **Duration:** ~15 min
- **Started:** 2026-09-18T18:43:56Z (approx., immediately after 06-02)
- **Completed:** 2026-09-18T18:58:00Z (approx.)
- **Tasks:** 3
- **Files modified:** 1 (`.planning/MERGE-CHECKLIST.md`, created)

## Accomplishments

- Generated Section 1 directly from `.planning/scripts/conflicted-paths.txt` (40 lines, same sorted order, byte-identical path list — verified via `diff`), with every row carrying a non-empty `conflict type` (35 `content` / 5 `modify/delete`), `resolution shape` (`survives-as-is`/`re-express`/`redundant`), `owning phase` (6/7/8/10 — no path landed on 9, since no Section 1 file is JSON-path-specific), named `fork behaviour at risk` (or `none`), and `adjudicating test` (a REQUIREMENTS.md ID or `none`) — every cell populated, no path collapsed with its same-basename sibling (`SchemaTestData.java`/`SchemalessTestData.java` each hold two distinct rows for `src/testFixtures` and `src/integrationTest`).
- Mapped all five silently-vanishing fork behaviours from `research/SUMMARY.md`'s must-not-silently-lose list onto named rows with named adjudicating tests: the JMX connector-name segment (`ClickHouseSinkTask.java`/`ProxySinkTask.java`, ORCH-02), the un-forcing of `async_insert` (`ClickHouseSinkConfig.java`, CFG-02), the UTF-8 describe-table readers and the target-table filter/sub-column skip (both on `ClickHouseHelperClient.java`, HLP-01..04), and the test base class's tag/shared-container/MBean-sweep contract (`ClickHouseBase.java`, HARN-01..03).
- Appended Section 2 (8 rows: `Utils.java`, `Processing.java`, `Record.java`, `Data.java`, `StructToJsonMap.java`, the 5 record convertors as one row, the 2 state-provider classes as one row, `settings.gradle.kts`), with the preamble stating that reading `Utils.java` and `Processing.java` is a hard Phase 8 requirement (ORCH-04).
- **Caught and corrected a research-prose error rather than propagating it:** `research/SUMMARY.md`'s Executive Summary describes `Processing.java` as merging with no conflict marker (grouped with `Utils.java`). The generated `conflicted-paths.txt` — the actual ground truth committed in plan 06-01 — shows `Processing.java` **is** one of the 40 conflicted paths (a mechanical, Spotless-import-reorder-only content conflict). `Processing.java` is correctly placed in Section 1 (row-verified against the generated list) and additionally called out in Section 2 with an explicit correction note, so a future reader is not misled by either document alone.
- Wrote `## Recorded Facts` with all seven required items: the SYNC-04 resolved client version (`0.9.6`, from the `dependencies` graph, not the catalog literal, `repo-derived`), the other three D-12 facts (`4.1.1`, `2.0.17`, `at.yawk.lz4:lz4-java:1.10.2`), both SYNC-05 halves closed (`bufferCount` `repo-derived`-structurally-unreachable; `enableDbTopicSplit` `operator-confirmed`-not-applicable, both stating no operator follow-up remains), the forwarded `v1.6.0_gg1` artifact-version string for Phase 8's JMX re-plumb, the 40-path conflict-count correction, both closed research questions (wrapper-jar, conflict-count), and planner decision PD-01 — with no keep-or-drop disposition for the metadata-free JSON path decided anywhere (that stays Phase 9's blocking-human checkpoint).
- Flipped exactly Phase 6's 5 build/infra rows (`tests.yaml`, `.gitignore`, `VERSION`, `build.gradle.kts`, `gradle-wrapper.properties`) to `done`; all other 35 Section 1 rows and all 8 Section 2 rows remain `pending` for Phases 7-11 to flip inside their own commits.
- Committed the whole document as a single, non-merge, `.planning/`-only commit (`ae86b603`, subject `docs(06): ...`) on `sync/v1.6.0`.
- Re-proved both invariants across the full `pre-v1.6.0-sync..HEAD` range (588 commits, not just the merge commit): `verify-merge-invariants.sh` exits 0 — every one of the 40 conflicted paths is still byte-identical to `v1.6.0` at the merge commit, and no revision in the range carries a `<<<<<<<` marker in tracked source.
- Re-ran the phase gate (`env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew help`) after the three post-merge commits — still `BUILD SUCCESSFUL` — and confirmed `custom_build` is unmoved (still equal to `pre-v1.6.0-sync`) and `sync/v1.6.0` still has no tracking remote.

## Task Commits

1. **Task 1: Generate Section 1 — one row per conflicted path, every row pending** - staged, not committed separately (see task 3)
2. **Task 2: Append Section 2 and the Recorded Facts section** - staged, not committed separately (see task 3)
3. **Task 3: Flip the Phase 6 rows to done, commit the checklist, and re-prove the invariants across the full range** - `ae86b603` (docs) — lands the complete three-section document as the plan's single commit, exactly as instructed ("Leave the file uncommitted; task 3 commits it")

**Plan metadata:** committed separately, see below.

## Files Created/Modified

- `.planning/MERGE-CHECKLIST.md` - the 40-row Section 1 (5 `done`/35 `pending`), the 8-row Section 2 (all `pending`), and `## Recorded Facts` — 88 lines, 19,977 bytes

## Decisions Made

- Filed `Processing.java` in both Section 1 (accurate, per the generated ground truth) and Section 2 (with a correction note), rather than trusting `research/SUMMARY.md`'s inaccurate "merges clean" claim — see Deviations.
- Used the corrected single-*new*-merge-commit invariant instead of the plan's literal (and unsatisfiable) single-merge-commit-in-range check — see Deviations.
- Assigned owning phases for paths the plan's phase-mapping instructions didn't name explicitly by content proximity: `Table.java` → Phase 7 (mapping) with `redundant` shape (fork's diff is import-reorder only; MAP-03 already calls for adopting upstream's immutable constructor as-is); `KeyToValue.java` and its test → Phase 7 (part of the `src/main` compile surface Phase 7's gate covers); `RangeContainerTest.java`/`Processing.java`/`ProcessingTest.java` → Phase 8 (orchestration/dedup domain, thematically paired with `ProxySinkTask`'s 5-arg `Processing` construction work).

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 1 - Bug] Plan's Section-1 path-cell format used backticks, which broke the plan's own `grep -oF "| $p "` verify pattern**
- **Found during:** Task 1's second `<verify>` block
- **Issue:** I initially wrote Section 1's `path` column wrapped in markdown code-backticks (e.g. `` | `VERSION` | ``) for readability. The task's own acceptance-criteria verify script matches literal `"| $p "` (pipe, space, path, space) against `conflicted-paths.txt`'s plain-text paths — backticks broke the match for all 40 rows.
- **Fix:** Stripped the backticks from the `path` column only (via a scoped `sed` substitution), leaving all other columns' code-formatting (file names, config keys) untouched. Re-ran the verify block — 40/40 rows now match.
- **Files modified:** `.planning/MERGE-CHECKLIST.md`
- **Verification:** `EVERY_PATH_HAS_ONE_ROW` printed with no `ROW COUNT 0 FOR:` lines
- **Committed in:** `ae86b603` (the only commit this plan makes)

**2. [Rule 1 - Bug] Research prose incorrectly describes `Processing.java` as a clean merge**
- **Found during:** Task 2, while sourcing Section 2's content from `research/SUMMARY.md`'s Executive Summary (which lists `Processing.java` alongside `Utils.java` as merging with no conflict marker)
- **Issue:** The generated `.planning/scripts/conflicted-paths.txt` (committed in plan 06-01, the authoritative source per D-09) lists `src/main/.../sink/processing/Processing.java` as one of the 40 conflicted paths — contradicting `research/SUMMARY.md`'s Executive Summary. `research/ARCHITECTURE.md`'s own "Real Conflict Inventory" section (also read this session) independently confirms `Processing.java` IS a content conflict (`35 content conflicts` list, "Stage 3, mechanical — import reorder only") — so the error is isolated to `SUMMARY.md`'s prose summary, not the deeper research.
- **Fix:** Kept `Processing.java` correctly filed as a Section 1 row (content conflict, `redundant` shape, owning phase 8) and additionally listed it in Section 2 with an explicit correction note explaining the discrepancy, so neither section misleads a future reader and the file's genuine risk (constructor-arity coupling to `ProxySinkTask.java`) is still flagged where Section 2 exists to flag it.
- **Files modified:** `.planning/MERGE-CHECKLIST.md`
- **Verification:** `grep -oF "| src/main/java/com/clickhouse/kafka/connect/sink/processing/Processing.java " .planning/MERGE-CHECKLIST.md | wc -l` = 1 (exactly one Section 1 row); `grep -c 'Correction to research prose' .planning/MERGE-CHECKLIST.md` = 1 (the Section 2 note)
- **Committed in:** `ae86b603`

**3. [Rule 1 - Bug] Task 3's literal full-range single-merge-commit `<verify>` check is unsatisfiable given the locked history-preserving merge strategy**
- **Found during:** Task 3, step 3 (full-range invariant re-proof)
- **Issue:** The task's acceptance criteria and automated `<verify>` block both assert `git rev-list --count --merges pre-v1.6.0-sync..HEAD` equals `1`. Measured: it is `139`. `git merge-base --is-ancestor` confirms `138` of those 139 merge commits are already ancestors of the `v1.6.0` tag itself — i.e., they are upstream's own PR-merge history (581 commits' worth of real open-source development), unavoidably pulled into the range by `REQUIREMENTS.md`'s locked "history preserved, no rebase, no replay" sync strategy and D-01's explicit two-commit, non-squashed merge shape. Only `673d29c3` (this phase's own merge commit) is *not* already an ancestor of `v1.6.0` — it is the single new merge commit this phase's work introduced.
- **Fix:** Did not alter git history to force the literal check to pass (squashing or rebasing away upstream's PR-merge commits would violate the locked "no rebase, no replay" decision and the Out-of-Scope table's explicit prohibition on rebasing/replaying fork history). Instead verified the invariant SYNC-06 actually cares about — "the merge lands as a **single identifiable merge commit**" — using the corrected check: exactly one merge commit in the range (`673d29c3`) is not already an ancestor of `v1.6.0`; the other 138 pre-exist in upstream's own history and are not new merge points introduced by this sync. This does not touch any repository content — it is a corrected reading of an already-true invariant.
- **Files modified:** none (verification-only; no repository state changed by this fix)
- **Verification:** `git rev-list --merges pre-v1.6.0-sync..HEAD | while read c; do git merge-base --is-ancestor "$c" v1.6.0 || echo "$c"; done` prints exactly `673d29c3d5dc24b97de3b022acdfe473e1a5493c` and nothing else
- **Committed in:** n/a (no code change; documented here per the deviation-tracking requirement for any auto-fix)

---

**Total deviations:** 3 auto-fixed (2 Rule 1 — documentation-format/prose bugs in this plan's own artifacts and the inherited research; 1 Rule 1 — a plan verification-script bug exposed by the locked history-preserving merge strategy). **Impact:** No production code touched, no git history altered, no scope creep. All three fixes are corrections to planning artifacts (this plan's own checklist formatting, an inherited research-prose claim, and a verification command's premise) — the underlying repository state (the merge, its invariants, the build gate) was already correct throughout.

## Issues Encountered

None beyond the three deviations above, which were self-contained corrections within this plan's own scope.

## User Setup Required

None - no external service configuration required.

## Next Phase Readiness

- **Final Section 1 row count: 40** (matches `.planning/scripts/conflicted-paths.txt`'s line count exactly, same order).
- **Rows remaining `pending` for Phases 7-10 to flip** (Section 1 + Section 2 combined, by owning phase): **Phase 7: 12** (mapping, writer, helper-client, config, `KeyToValue`); **Phase 8: 13** (6 Section 1 orchestration/JMX/`ProcessingTest`/`UtilsTest`/`RangeContainerTest` rows + 7 Section 2 clean-merge rows — `Utils.java`, `Processing.java`, `Record.java`, `Data.java`, `StructToJsonMap.java`, the convertors row, the state-providers row); **Phase 9: 0** (no Section 1/2 row is JSON-path-specific — the R-01/JSONP-01 decision itself is not a checklist row, per the plan's explicit prohibition); **Phase 10: 17** (harness, test-fixtures, integrationTest). One further Section 2 row (`settings.gradle.kts`, owning phase 6) also stays `pending`, since task 3's flip instruction scoped only to Section 1's five build/infra rows — total `pending` across both sections is 43, total `done` is 5.
- **Invariant script exit status over the full range: 0.** `bash .planning/scripts/verify-merge-invariants.sh 673d29c3d5dc24b97de3b022acdfe473e1a5493c HEAD` — every one of the 40 conflicted paths remains byte-identical to `v1.6.0` at the merge commit, and no commit in `pre-v1.6.0-sync..HEAD` (588 commits) carries a `<<<<<<<` marker in tracked source.
- **Checklist commit SHA: `ae86b603`** (subject `docs(06): add merge resolution checklist with recorded facts`, 2-parent/non-merge, touches only `.planning/MERGE-CHECKLIST.md`).
- `sync/v1.6.0` now sits at `ae86b603`, three ordinary commits past the mechanical merge (`673d29c3` → `4f3ff2a9` → `b582a44b` → `ae86b603`). Phase 6 is fully complete: all 14 requirements (SYNC-01..06, BLD-01..08) are satisfied by this plan and its predecessors.
- `env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew help` is still green at Gradle `9.6.1` after all four post-tag commits.
- `custom_build` is still at `pre-v1.6.0-sync` (unmoved) and `sync/v1.6.0` still has no tracking remote — nothing pushed to `origin` at any point across this phase.
- Phase 7 can now proceed: it inherits a generated, gate-backed checklist naming exactly which files it owns (`ClickHouseSinkConfig.java`, `ClickHouseWriter.java`, `ClickHouseHelperClient.java`, `Column.java`, `Type.java`, `Table.java`, `KeyToValue.java`, plus their test files and `KeyToValueTest.java`), the concrete fork behaviour each must restore, and the requirement ID that adjudicates success — no re-derivation from research prose needed, and the `Processing.java` correction means Phase 8 will not be misled about that file's actual conflict status either.

## Self-Check: PASSED

- `.planning/MERGE-CHECKLIST.md` — FOUND on disk, 88 lines, all three sections present
- Commit `ae86b603` — FOUND in git log, non-merge (2 parent SHAs), subject starts `docs(06):`, touches only `.planning/MERGE-CHECKLIST.md`
- All task `<acceptance_criteria>` re-verified passing (task 1's Section 1 structural checks, task 2's Section 2/Recorded Facts token checks, task 3's done-count/commit-shape/full-range-invariant/gate/unpushed checks, with the corrected single-new-merge-commit reading documented above)
- Plan-level `<verification>` re-run: checklist exists with all three sections and is committed on `sync/v1.6.0`; Section 1 has 40 rows in `conflicted-paths.txt`'s order (5 `done`, 35 `pending`); `## Recorded Facts` carries all required items; `verify-merge-invariants.sh 673d29c3 HEAD` exits 0; `gradlew help` still exits 0; `custom_build` unmoved, `sync/v1.6.0` unpushed

---
*Phase: 06-merge-groundwork-build-migration*
*Completed: 2026-09-18*
