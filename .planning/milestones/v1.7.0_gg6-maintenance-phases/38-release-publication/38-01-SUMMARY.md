---
phase: 38-release-publication
plan: "01"
subsystem: release
tags: [version-bump, changelog, fork-delta, fork-anchors, evidence]

requires:
  - phase: 37-ci-hardening-advisory-debt
    provides: "bump_version.py, baseline_tag.py, the evidence helper and a green sync/v1.7.0 tip with the anchors mirror at rows=172"
provides:
  - "VERSION and its fork-anchors.txt row at v1.7.0_gg6 in one two-file commit, with the generated Version.java read back after a clean build"
  - "CHANGELOG.md 1.7.0_gg6 section (introduction, Important Changes, Improvements, Known Issues, Build and CI) that absorbs upstream's Unreleased entry verbatim"
  - "docs/FORK-DELTA.md final for gg6: pointer, gg6 syncer paragraph, outcomes and conditions for the stale forward references, raw numbers re-run once, recounted import-only and identical-file figures, open items 13 to 23"
  - "38-COMMAND-EVIDENCE.md with the index E-01..E-19 and entries E-01..E-04"
affects: [38-02, 38-03, 38-04, 38-05]

plan_head_before: eb8c163ab9cdc7951612931f7c49b23e3ec37d6b
plan_head_after: 64040101ecc1b48163407833bab261d61581baca

actuals:
  tokens: 14955
  tasks: 3
  commits: 7

tech-stack:
  added: []
  patterns:
    - "scripted assembly of a CHANGELOG section from a template and the phase-start file, so the released sections are byte-identical by construction"
    - "FORK-DELTA numbers measured once on the release tip and cross-checked by the helper against fresh git output"

key-files:
  created:
    - .planning/phases/38-release-publication/38-COMMAND-EVIDENCE.md
  modified:
    - VERSION
    - .planning/scripts/fork-anchors.txt
    - CHANGELOG.md
    - docs/FORK-DELTA.md

key-decisions:
  - "Open items 13 to 23 (eleven, not the nine the step lists): the publish-path caveat and Dependabot PR 17 are added as conditions because the plan's carried-items table routes them to FORK-DELTA"
  - "The reading-order sentence lists both tail checklists (the first tail checklist was already missing from it), with the verified Recorded Fact ranges 1-16, 17-41, 42-43, 44 onward"
  - "The paragraph for the next syncer names docs/ROLLOUT-v1.7.0_gg6.md in code form, so the file has exactly one broken link line (the introduction's link) as the plan's verify allows"

patterns-established:
  - "Evidence timestamps are read from the clock or from git log, never estimated"

requirements-completed: [RLS-01]

coverage:
  - id: D1
    description: "VERSION reads v1.7.0_gg6 together with its VERSION anchors row in one commit of exactly two files; a clean generateVersionFile reads ARTIFACT_VERSION = v1.7.0_gg6; the baseline selection still names v1.7.0_gg5; the anchors mirror reads rows=172 bad=0"
    requirement: "RLS-01"
    verification:
      - kind: other
        ref: "38-COMMAND-EVIDENCE.md E-02; plan verify P38-01-T1-OK"
        status: pass
      - kind: unit
        ref: "BumpVersionScriptUnitTest 52, MBeanNameUnitTest 11, BaselineTagScriptUnitTest 19, all green with Docker unreachable"
        status: pass
    human_judgment: false
  - id: D2
    description: "CHANGELOG.md starts with a dated 1.7.0_gg6 section holding upstream's Unreleased entry verbatim, no Unreleased heading remains and every released section is byte-identical"
    requirement: "RLS-01"
    verification:
      - kind: other
        ref: "38-doc-check.py changelog (OK changelog: section_lines=71 required=13); plan verify P38-01-T2-OK"
        status: pass
    human_judgment: false
  - id: D3
    description: "docs/FORK-DELTA.md is final for gg6 with refreshed raw numbers equal to fresh git output and open items that each carry an owner and a re-open trigger"
    requirement: "RLS-01"
    verification:
      - kind: other
        ref: "38-doc-check.py numbers (OK); plan verify P38-01-T3-OK"
        status: pass
    human_judgment: false
  - id: D4
    description: "The CHANGELOG and FORK-DELTA wording is accurate for operators and the next syncer, and keeps each label no stronger than its source"
    verification: []
    human_judgment: true
    rationale: "Accuracy of prose against its handoff sources is judged by the independent review of plan 38-03, not by a helper"

duration: 14min
completed: 2026-10-31
status: complete
---

# Phase 38 Plan 01: Version bump, CHANGELOG section and final FORK-DELTA Summary

**The release identity moved to v1.7.0_gg6 through the bump script in one two-file commit with its anchors row, the CHANGELOG gained the dated 1.7.0_gg6 section that absorbs upstream's Unreleased entry verbatim, and FORK-DELTA is final with raw numbers re-run once on the release tip and eleven new open items with owners and triggers.**

## Performance

- **Duration:** about 14 min
- **Started:** 2026-10-31T12:40Z (approximate, first command)
- **Completed:** 2026-10-31T12:54Z
- **Tasks:** 3 (one tracer, two auto)
- **Files modified:** 5 (VERSION, the anchors file, CHANGELOG.md, docs/FORK-DELTA.md, the evidence file)

## Accomplishments

- E-01 re-read the premise on the untouched tree: origin `custom_build` still `3a3a15a9`, left count 0, no gg6 tag locally or on origin, `pre-v1.7.0_gg6` and `v1.7.0_gg5` both on `3a3a15a9`, the six open pull request heads and the one release unchanged, the client catalog on 0.11.0, the anchors mirror at `rows=172 bad=0`, the hygiene self-test at 7 and 9, the evidence helper byte-identical to Phase 37's.
- Tracer: the bump script moved `VERSION` and its row together (`1/1; 1/1`), a clean `generateVersionFile` read `ARTIFACT_VERSION = "v1.7.0_gg6"`, `baseline_tag.py` still picks `v1.7.0_gg5`, and the three Docker-free classes that touch the version (82 tests) are green with `DOCKER_HOST` pointing nowhere.
- CHANGELOG: a 71-line gg6 section dated 2026-10-31; the helper confirms the file ends with the phase-start file minus the Unreleased block, the entry is verbatim under Improvements and the 13 required tokens are present.
- FORK-DELTA: gg6 clause and rollout-note pointer, VERSION and MBean rows on gg6, the gg6 paragraph for the next syncer, both tail checklists in the reading order, open item 8 rewritten from the real Future Requirements list, the PR #10 and compare sentences turned into conditions, the gg6 raw-number rows (331 files, 39665 insertions, 1161 deletions; 40 files, 2989, 272), the recounted figures (14 of 40 import-only, 33 of 63 byte-identical) and open items 13 to 23.

## Task Commits

1. **Task 1 (tracer): pre-flight, helpers, version bump** - `0a9599f6` (evidence open, E-01), `70bf8108` (bump_commit), `4b252aaa` (E-02)
2. **Task 2: CHANGELOG section** - `fdf3be31` (changelog_commit), `180ac6aa` (E-03)
3. **Task 3: FORK-DELTA pass** - `21697379` (forkdelta_commit), `64040101` (E-04)

**Plan metadata:** the commit that carries this SUMMARY (docs: complete plan).

## Facts

- `local_head` (L0): `eb8c163ab9cdc7951612931f7c49b23e3ec37d6b`
- Bump commit: `70bf8108d1720c229453782dbae6250de0931eba` (files: `.planning/scripts/fork-anchors.txt`, `VERSION`)
- `changelog_date`: 2026-10-31; CHANGELOG commit `fdf3be31016acccdc0813ddca0ee6063dd278d39`
- FORK-DELTA commit `21697379e188f163b1f2f401642be1a7401a1ada`
- Wider numbers (`v1.7.0` to tip): 331 files, 39665 insertions, 1161 deletions; narrow (`src/main`): 40 files, 2989 insertions, 272 deletions. Since gg5: wider 79, 7939, 478; narrow 7, 112, 31. All equal the planning-time values.
- `clickhouse_version`: 26.10.3.7, digest `clickhouse/clickhouse-server@sha256:739e099abd1157390960a59e4532d21781d8fde57e6e4151cb920ea02c30b1ce` (same as Phase 37 E-18)
- Test results: version guards 82 tests, 0 failures, 0 errors (`BumpVersionScriptUnitTest` 52, `MBeanNameUnitTest` 11, `BaselineTagScriptUnitTest` 19). No full suite was run: the plan changes no Java file; plan 38-04 re-gates.
- Scope: `git diff --name-only L0 HEAD -- . ':(exclude).planning'` lists exactly `CHANGELOG.md`, `VERSION`, `docs/FORK-DELTA.md`; under `.planning/scripts` only `fork-anchors.txt`. No src, test, build, gradle or `.github` change; nothing pushed, no tag, no mutating `gh` call.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 3 - Blocking] The helpers and assets were already tracked**
- **Found during:** Task 1, evidence commit.
- **Issue:** the plan says the first evidence commit stages the six helpers and the assets as untracked files; `git ls-files --error-unmatch` shows all eight tracked since `0d4763ee`.
- **Fix:** the evidence commit stages only the evidence file (as Phase 37's did). Recorded in E-01.

**2. [Rule 1 - Bug] The first helper syntax check left a `__pycache__`**
- **Found during:** Task 1, E-01 helper_syntax.
- **Issue:** `python3 -I -m py_compile` ignores `PYTHONPYCACHEPREFIX` and wrote bytecode into the phase directory.
- **Fix:** deleted the `.pyc` files and the directory by name; redid the check with `compile()`, which writes nothing. No `__pycache__` or stray file (no `=N`) exists in the repository.

**3. [Rule 1 - Bug] Two evidence timestamps were estimates**
- **Found during:** Task 2, after the E-01 and E-02 commits.
- **Issue:** the E-01 and E-02 timestamp lines were typed from memory (12:46 and 12:55 UTC) and disagreed with `git log` (12:44 and 12:45).
- **Fix:** the E-03 commit replaced both lines with the commit times and says so in E-03's text; no fact or verdict changed. From then on every stamp comes from `date -u`.

**4. [Plan wording] The Task 1 and Task 2 verify blocks hold a clause that cannot pass at the final tip**
- **Found during:** the plan-level verification.
- **Issue:** Task 1's verify requires `git diff --quiet L0 HEAD -- ... CHANGELOG.md docs` and Task 2's requires the same for `docs`; both are time-scoped and fail once Tasks 2 and 3 land, so "P38-01-T1-OK and T2-OK are printed on the committed tip" cannot hold literally.
- **Fix:** each verify passed as written at its own task tip (P38-01-T1-OK, P38-01-T2-OK, P38-01-T3-OK). At the final tip T3 passes as written and T1 and T2 pass with only that one clause removed; the scope check above replaces it.

**5. [Plan wording] Open items: eleven instead of nine, and two judgments**
- **Found during:** Task 3.
- **Issue:** step 7 lists nine items, while the carried-items table sends the publish-path caveat and the PR #17 state to FORK-DELTA as conditions. Step 5b expects a "no github-actions pull request was observed" sentence; none exists (the row already names PR 16 as the first observed one). The `Utils.java` row does not list checklists, so it was not changed. The reading-order sentence also lacked the first tail checklist.
- **Fix:** items 13 to 23 (11 items), 5b left unchanged with the reason in `forkdelta_forward_refs_note`, the reading order lists both tail checklists with verified Recorded Fact ranges, and the gg6 paragraph says "items 13 to 23".

**6. [Process] One scripted batch of four replacements in FORK-DELTA**
- **Found during:** Task 3.
- **Issue:** the plan asks for the Edit tool only; four wording fixes to items 14, 15, 21 and the item range were made by a Python call that asserted each old text matches exactly once.
- **Fix:** none needed; the result is identical to four Edit calls. Recorded in E-04.

**Total deviations:** 6 (2 blocking or bug fixes in the check commands, 1 evidence-accuracy fix, 3 plan-wording or process notes). **Impact:** none on scope or results.

## Issues Encountered

- Swap use was high (about 16 of 17 GB) at the start; no OpenJ9 crash, javacore or core dump appeared in the two Gradle runs.
- The `shell` default `DOCKER_HOST` already points at the colima `vz` socket, so no Docker prefix was needed; the Docker-free run overrode it deliberately.

## Known Stubs

None.

## Threat Flags

None. No new endpoint, auth path or file access; the changes are documents, a version file and an anchors row.

## User Setup Required

None - no external service configuration required.

## Open for the orchestrator

- The one broken link: `docs/ROLLOUT-v1.7.0_gg6.md`, linked from FORK-DELTA line 7 and from the CHANGELOG section; plan 38-02 creates it and its gate runs the link check.
- `pr_heads_unchanged`: yes (`10 011bc42b; 16 cb3c7718; 17 6a53525b; 18 cb898dc2; 19 2a806038; 20 8786723b`; one release `426690002 v1.7.0_gg5`).
- The CHANGELOG heading date is 2026-10-31 (the UTC date at writing, 12:5x UTC). It equals the UTC date at the merge only if the merge happens on 2026-10-31 UTC; otherwise the user's direct answer on the mismatch is needed (PT-14 of Phase 32) or the heading is re-dated in a last pre-gate commit.
- No ROADMAP or REQUIREMENTS wording change is needed from this plan. RLS-01 can be marked after verification.
- Origin still holds the user's two `scratch/clt-07-*` branches; they were read, never touched.

## Handoffs

- **Plan 38-02:** the four labels of O-3 (`measured`, `read from the code`, `derived from Kafka consumer-group semantics (not measured)`, `measured in research (not re-run)`) were applied in the CHANGELOG with the strength of their sources (the buffered first-start and the no-commit-before claims carry "derived ... not measured"). The carried-items table landed as FORK-DELTA items 13 to 23 and the CHANGELOG Known Issues; the note keeps F-1, F-2, the buffered limits and C2/C3 as known limits.
- **Plan 38-04:** the bump commit is `70bf8108d1720c229453782dbae6250de0931eba`. The allow-list name is `confluent-archive-allowlist-v1.7.0_gg6.txt` under `.planning/scripts/`; it does not exist yet and FORK-DELTA, the release workflows and the CHANGELOG already refer to it. Any change to a file inside the FORK-DELTA numbers' scopes (`src`, `build.gradle.kts`, `gradle`, `.github`) would move the gg6 rows.
- **Carry-over:** the evidence index is E-01..E-19 with E-01..E-04 `pass`; the scratch recount script `/tmp/gsd38/exec/recount.py` is not committed (it applies the file's own import-only method and reproduces the published gg5 figures).

## Next Phase Readiness

Plan 38-02 can start: the tree is clean apart from the two orchestrator-owned state files, the anchors mirror reads `rows=172 bad=0`, and `VERSION` reads `v1.7.0_gg6`.

## Self-Check: PASSED

- Files exist: `VERSION` (`v1.7.0_gg6`), `CHANGELOG.md`, `docs/FORK-DELTA.md`, `.planning/scripts/fork-anchors.txt`, `38-COMMAND-EVIDENCE.md`.
- Commits `0a9599f6`, `70bf8108`, `4b252aaa`, `fdf3be31`, `180ac6aa`, `21697379`, `64040101` are ancestors of `HEAD`.
- `P38-01-T1-OK`, `P38-01-T2-OK` and `P38-01-T3-OK` print (T1 and T2 at their own tips and, minus the time-scoped clause, at the final tip).

---
*Phase: 38-release-publication*
*Completed: 2026-10-31*
