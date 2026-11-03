---
phase: 44-release-publication
plan: "01"
subsystem: release
tags: [release, version-bump, changelog, fork-delta, evidence, v1.7.0_gg7]

requires:
  - phase: 43-document-fixes-phase-37-dispositions
    provides: the evidence helpers (43-evidence-check.py, 43-doc-check.py) and the Phase 37 dispositions the CHANGELOG and FORK-DELTA state
provides:
  - VERSION v1.7.0_gg7 and its fork-anchors.txt row, set by bump_version.py in one two-file commit
  - the dated 1.7.0_gg7 CHANGELOG section, released sections byte-identical
  - the final docs/FORK-DELTA.md for gg7 (recount, tag-pair commands, raw numbers refreshed once, gg7 paragraphs)
  - the local rollback tag pre-v1.7.0_gg7 on 72e8ccff
  - 44-COMMAND-EVIDENCE.md with E-01 to E-04 and the 19-row index, and the five Phase 44 helpers
affects: [44-02, 44-03, 44-04, 44-05]

actuals:
  tokens: 20037
  tasks: 3
  commits: 7
plan_head_before: 9a5209c17e5380ec1f675cd8b3225250d3ff41fe
plan_head_after: 38d03ca2710a604e63f3515df5ec99500c360e20

tech-stack:
  added: []
  patterns:
    - "one script call writes VERSION and its anchors row; one commit of exactly those two files"
    - "raw numbers measured once on the release tip and written as tag-pair commands"

key-files:
  created:
    - .planning/phases/44-release-publication/44-COMMAND-EVIDENCE.md
    - .planning/phases/44-release-publication/44-evidence-check.py
    - .planning/phases/44-release-publication/44-doc-check.py
    - .planning/phases/44-release-publication/44-allowlist-check.py
    - .planning/phases/44-release-publication/44-hygiene-check.py
    - .planning/phases/44-release-publication/44-review-check.py
  modified:
    - VERSION
    - .planning/scripts/fork-anchors.txt
    - CHANGELOG.md
    - docs/FORK-DELTA.md

key-decisions:
  - "The CHANGELOG section carries one bullet beyond the plan's list: the JMX version= segment and client name now read v1.7.0_gg7, as the gg6 section documented the same change"
  - "The recount of src/main with the file's own method reproduced the published gg6 figures (33 14 16 10) before it was applied to the release tip"
  - "The 128-of-143 upstream test-tree count in FORK-DELTA was left as written: no scope tried re-derives it, and every scope tried gives the same counts at v1.7.0_gg6 and at the tip"

requirements-completed: [RLS-01]

coverage:
  - id: D1
    description: "VERSION reads v1.7.0_gg7, set by bump_version.py gg together with the VERSION row of fork-anchors.txt in one commit of exactly those two files; the generated Version.java reads ARTIFACT_VERSION v1.7.0_gg7 after a clean build; baseline_tag.py still picks v1.7.0_gg6; the anchors mirror reads rows=190 bad=0"
    requirement: "RLS-01"
    verification:
      - kind: unit
        ref: "BumpVersionScriptUnitTest, MBeanNameUnitTest, BaselineTagScriptUnitTest (82 tests, Docker unreachable)"
        status: pass
      - kind: other
        ref: "bash verify block of task 1: P44-01-T1-OK"
        status: pass
    human_judgment: false
  - id: D2
    description: "CHANGELOG.md starts with a dated 1.7.0_gg7 section with the five headings, every released section byte-identical, no planning identifier, every client-V2-only effect scoped to client V2"
    requirement: "RLS-01"
    verification:
      - kind: other
        ref: "bash verify block of task 2: P44-01-T2-OK"
        status: pass
    human_judgment: true
    rationale: "Whether each bullet states its effect in the right operator terms and keeps its source's label strength is a wording judgment; the independent review of plan 44-03 reads the CHANGELOG section"
  - id: D3
    description: "docs/FORK-DELTA.md is final: the false Processing.java byte-identical claim and the stale recounts replaced, HEAD-relative commands are tag-pair commands, the after-gg7 rows equal a fresh git diff --shortstat, the gg7 paragraphs written"
    requirement: "RLS-01"
    verification:
      - kind: other
        ref: "bash verify block of task 3: P44-01-T3-OK"
        status: pass
    human_judgment: true
    rationale: "The paragraphs describe the tree in prose; the numbers and spans are checked by command, the wording is read by the independent review of plan 44-03"
  - id: D4
    description: "The local lightweight rollback tag pre-v1.7.0_gg7 points at 72e8ccff and nothing reached origin; no v1.7.0_gg7 tag exists locally or on origin"
    requirement: "RLS-01"
    verification:
      - kind: other
        ref: "git rev-parse 'pre-v1.7.0_gg7^{commit}'; git ls-remote origin refs/heads/custom_build; git tag -l 'v1.7.0_gg7'"
        status: pass
    human_judgment: false

duration: 11min
completed: 2026-11-02
status: complete
---

# Phase 44 Plan 01: Release identity and maintainer documents Summary

**VERSION moved to v1.7.0_gg7 through bump_version.py with its anchors row in one two-file commit, the dated 1.7.0_gg7 CHANGELOG section written, docs/FORK-DELTA.md made final (false Processing.java claim fixed, recount 30/14/19/10, raw numbers measured once, tag-pair commands), and the local rollback tag pre-v1.7.0_gg7 set on 72e8ccff, with nothing pushed.**

## Performance

- **Duration:** 11 min
- **Started:** 2026-11-02T20:59:50Z (first command logged at 21:00:08Z)
- **Completed:** 2026-11-02T21:10:01Z
- **Tasks:** 3
- **Files modified:** 10 (6 created, 4 modified; 1317 insertions, 11 deletions)

## Accomplishments

- Pre-flight on the untouched tree: origin `custom_build` still `72e8ccff`, left count 0, no gg7 tag anywhere, the first upstream recheck found nothing new, and the five helper copies were verified (four byte copies, one derivation whose `agents` subcommand passed 5 of 5 synthetic cases).
- The bump tracer ran end to end on the real files: the script wrote `VERSION` and the anchors row, a clean `generateVersionFile` produced `ARTIFACT_VERSION = "v1.7.0_gg7"`, the baseline picker still names `v1.7.0_gg6`, and the three version guard classes passed (82 tests) with Docker unreachable.
- The CHANGELOG section states the bounded client V2 insert, the `RetriableException` pass-through, the closing fixes, the `originalTopic()` fix, the start-up WARN and the build changes, each scoped to the clients it affects.
- FORK-DELTA now describes the gg7 tree with measured numbers: 345 files, 42439 insertions, 1221 deletions at the wider scope and 43, 3183, 294 over `src/main`.

## Task Commits

1. **Task 1 (tracer): pre-flight, helpers, bump, rollback tag** - `d7c418af` (evidence opening with the five helpers), `1d7383b9` (the bump: `VERSION` and `fork-anchors.txt`), `40616438` (E-02)
2. **Task 2: the 1.7.0_gg7 CHANGELOG section** - `9c30e3ba` (CHANGELOG), `5a4381ad` (E-03)
3. **Task 3: the final FORK-DELTA pass** - `e979004e` (FORK-DELTA), `38d03ca2` (E-04)

**Plan metadata:** the commit holding this file (docs: complete plan).

## Files Created/Modified

- `VERSION`, `.planning/scripts/fork-anchors.txt` - `v1.7.0_gg7` and its anchors row, one commit
- `CHANGELOG.md` - the dated 1.7.0_gg7 section above the unchanged gg6 section
- `docs/FORK-DELTA.md` - recount, split Adopted row, tag-pair commands, after-gg7 rows, gg7 intro, pointer and next-syncer paragraphs
- `.planning/phases/44-release-publication/44-COMMAND-EVIDENCE.md` - header, 19-row index, E-01 to E-04
- `.planning/phases/44-release-publication/44-evidence-check.py`, `44-doc-check.py`, `44-allowlist-check.py`, `44-hygiene-check.py`, `44-review-check.py` - the five helpers

## Facts

- `local_head` (L0): `9a5209c17e5380ec1f675cd8b3225250d3ff41fe`; branch `sync/v1.7.0`; tip after the plan `38d03ca2710a604e63f3515df5ec99500c360e20`.
- Bump commit: `1d7383b9f21101c800c96a61fea714d999d6b092` (files `.planning/scripts/fork-anchors.txt`, `VERSION`). CHANGELOG commit `9c30e3ba08aa32141e1c9965e2c130870b60e8fa`. FORK-DELTA commit `e979004e91a4f84fa9223b9d957ab3216fa37e00`.
- `changelog_date`: 2026-11-02.
- Wider numbers `345 42439 1221`, narrow `43 3183 294`; since gg6 `34 2826 112` and `8 194 22`.
- Recount at the tip: 63 files at `v1.7.0`, 30 byte-identical, 14 import-only, 19 modified with fork logic, 10 new, none deleted (43 differ, 29 carry fork logic); the same recount at `v1.7.0_gg6` printed `33 14 16 10`; `Processing`, `StateProvider` and `KeeperStateProvider` moved from byte-identical to fork logic since gg6.
- `clickhouse_version`: 26.10.3.7; image digest `sha256:739e099abd1157390960a59e4532d21781d8fde57e6e4151cb920ea02c30b1ce`, equal to the research digest.
- Upstream recheck 1: `main` `9bed6f79` unchanged, newest tag `v1.7.0` (none past it), upstream pull requests 861, 855, 857 open and unmerged with their planning heads. Open pull requests on origin: 21 (`14be67ce`) and 22 (`b5cccbd3`), both ancestors of the tip.
- Left count of `origin/custom_build...sync/v1.7.0`: 0 (right count 201 at the start); `unpublished_commits` 199 before this plan.
- Swap was 27152 MB used of 27648 MB at the start; neither Gradle run crashed.

## Decisions Made

- The CHANGELOG section adds one bullet beyond the plan's list (the JMX `version=` segment and client name now read `v1.7.0_gg7`), because the release identity moves and the gg6 section documented the same change in the same place.
- The FORK-DELTA claim that 128 of 143 upstream test-tree files are byte-identical was not edited: no scope tried re-derives it, and every scope tried gives the same counts at `v1.7.0_gg6` and at the tip, so this release does not move it.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 3 - Blocking] The py_compile syntax check wrote bytecode into the phase directory**
- **Found during:** Task 1, step 1
- **Issue:** the plan's `PYTHONPYCACHEPREFIX=... python3 -I -m py_compile` form is ignored under `-I` (isolated mode drops `PYTHON*` variables), so `__pycache__` with five `.pyc` files appeared next to the helpers.
- **Fix:** removed the five `.pyc` files one by one and the empty directory, then re-ran the check as `python3 -I -X pycache_prefix=/tmp/gsd44/w/pyc -m py_compile`, which keeps bytecode out of the tree. `helper_syntax: yes` in E-01 records the second form.
- **Files modified:** none tracked
- **Verification:** `git status --porcelain` showed no `__pycache__` path; the task 1 verify passed.
- **Committed in:** not applicable (no tracked change)

---

**Total deviations:** 1 auto-fixed (1 blocking). **Impact on plan:** none on the deliverables; the helper copies and the evidence are as specified.

## Issues Encountered

- The task 1 verify run before the E-02 commit failed at its last clause (the evidence file was still modified, and the clause demands a clean tree). That is the order the plan prescribes (commit the evidence, then re-run), so the run after the E-02 commit passed.
- The empty by-directory diffs make `44-doc-check.py shortstat` exit with a usage error; the two empty scopes are recorded as `0 0 0` after the empty output was confirmed.

## Known Stubs

None.

## Open for the orchestrator

- Broken relative links to `docs/ROLLOUT-v1.7.0_gg7.md` (created by plan 44-02), the only broken links: `CHANGELOG.md` line 7, `docs/FORK-DELTA.md` lines 7 and 304.
- `pr_heads_unchanged: yes`, `upstream_prs_unchanged: yes`, `upstream_tag_past_v170: no`; nothing to put to the user from the first upstream recheck. Re-read them at the approval bundle and before the first user handoff.
- The CHANGELOG heading date `2026-11-02` equals the UTC date only until 2026-11-03T00:00Z (02:00 local). Compare `date -u +%F` with `head -1 CHANGELOG.md` at the approval bundle and again before building the merge commit; a date-fix commit is a freeze-rule event for plans 44-03 to 44-05.
- This plan did not stage `.planning/STATE.md`, `.planning/config.json` or `.planning/state.json` (the plan's commit rule). `STATE.md` was updated by the state verbs and is left modified and uncommitted; the orchestrator commits it. `REQUIREMENTS.md` was not edited and RLS-01 is not marked complete: the plan reserves the requirements wording and the closing for the orchestrator after verification.
- Nothing was pushed and no workflow was dispatched. `origin/custom_build` still reads `72e8ccff`; `pre-v1.7.0_gg7` exists only locally on `72e8ccff`; no `v1.7.0_gg7` tag exists.

## Handoffs

- **Plan 44-02:** the label vocabulary is O-9 (`measured`, `read from the code`, `derived`; the progress-header claim is `derived (mechanism, not measured)`). The archive figures are research expectations to re-measure, never values to copy: inner entries 7933 to 7932, local rebuild sizes 13558633 to 13564144 bytes, the gson version-table line `2.14.0 -> 2.15.0` with 17 other lines equal. The note links `CHANGELOG.md` and `docs/FORK-DELTA.md`, which already point at `docs/ROLLOUT-v1.7.0_gg7.md`, and the rollback tag it names is `pre-v1.7.0_gg7` on `72e8ccff`.
- **Plan 44-04:** the bump commit is `1d7383b9f21101c800c96a61fea714d999d6b092`; `.planning/scripts/confluent-archive-allowlist-v1.7.0_gg7.txt` does not exist yet and is written last from the final tree; `docs/FORK-DELTA.md` already names it. The anchors mirror reads `rows=190 bad=0` and no later phase commit may touch `fork-anchors.txt` without moving the row it changes in the same commit.

## Next Phase Readiness

- Plan 44-02 (the operator note) can start: the version, CHANGELOG, FORK-DELTA, helpers and evidence it builds on are committed on `sync/v1.7.0`.
- No blockers.

## Self-Check: PASSED

All created files exist, all seven commits are ancestors of HEAD, the three task verify blocks print `P44-01-T1-OK`, `P44-01-T2-OK` and `P44-01-T3-OK` on the final tip.

---
*Phase: 44-release-publication*
*Completed: 2026-11-02*
