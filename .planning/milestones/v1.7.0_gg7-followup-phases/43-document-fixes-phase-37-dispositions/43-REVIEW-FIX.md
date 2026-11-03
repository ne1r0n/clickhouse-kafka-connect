---
phase: 43-document-fixes-phase-37-dispositions
fixed_at: 2026-11-02T00:00:00Z
review_path: .planning/phases/43-document-fixes-phase-37-dispositions/43-REVIEW.md
iteration: 1
findings_in_scope: 3
fixed: 2
skipped: 1
status: partial
---

# Phase 43: Code Review Fix Report

**Fixed at:** 2026-11-02
**Source review:** .planning/phases/43-document-fixes-phase-37-dispositions/43-REVIEW.md
**Iteration:** 1

**Summary:**
- Findings in scope: 3
- Fixed: 2
- Skipped: 1

## Fixed Issues

### WR-01: gg1 item 6 overclaims that client V1 fails on the `compress=true` plus `setCompressionMethod=false` combination

**Files modified:** `docs/ROLLOUT-v1.7.0_gg1.md`, `docs/FORK-DELTA.md`
**Commit:** d77da0f9
**Applied fix:** Docs only, evidence labels kept honest. In gg1 item 6 and in the FORK-DELTA paragraph that begins "An eighth behaviour" (line 299), the `Invalid LZ4 magic byte` failure is now stated for client `0.9.6` only. Client V1 is stated to be unable to decode ZSTD on either client line checked (`0.9.6` and `0.11.0`), labelled as measured on a raw V1 client with `compress=true` set directly. Both places now say that whether a V1 connector with `compress=true` in `jdbcConnectionProperties` reaches that path was not observed, because the connector's V1 node sets `compress=false` (read from the code, `ClickHouseHelperClient.java`, line 156 verified). gg1 item 6 keeps its pointer to row D13 of `ROLLOUT-v1.7.0_gg6.md`; FORK-DELTA points to the D-17 row, which already says the V1 stripped-both case was not observed.

### WR-02: FORK-DELTA closing sentence and the status label of item 12 disagree with the items Phase 43 closed

**Files modified:** `docs/FORK-DELTA.md`
**Commit:** 5c6c40e1
**Applied fix:** The sentence ending the gg6 paragraph now reads "items 19, 22 and 23 have closed since, and the pull request of item 12 was closed unmerged on 2026-11-01". The item 12 title changed from "PR #10 declined (published knowingly open)" to "PR #10 closed unmerged (the 3.x pin stays open)". The item number and the rest of the item text are untouched. No other file refers to the old title (searched `docs`, `src`, `.planning/scripts` and the Phase 43 checkers).

## Skipped Issues

### WR-03: The repointing in FORK-DELTA is partial, so two published reproduction commands still give different numbers than the document states

**File:** `docs/FORK-DELTA.md:184` and `docs/FORK-DELTA.md:188`
**Reason:** skipped by instruction: already deferred to Phase 44 in the Phase 43 ledger (Handoff item 4) by design. The reproduction commands and claims are re-measured on the release tip in Phase 44, so repointing them now would pre-empt that refresh.
**Original issue:** `git diff --shortstat v1.7.0_gg4 HEAD` (line 184) and `git diff --quiet v1.7.0 HEAD -- <file>` (line 188) are `HEAD`-relative and no longer reproduce the stated figures; the review suggests Phase 44's verifier check that no `HEAD`-relative command is left in a measured-figures paragraph.

## Verification

Edits and commits were made in an isolated worktree, which was then fast-forwarded into `sync/v1.7.0` and removed (sentinel dropped, temp branch deleted). The gates below were run in the main checkout after the fast-forward, so they are reproducible from the tree a reader sees.

- Ledger check (phase-local `43-ledger-check.py`): `OK ledger: 18 rows, 0 problems`; counts `rows=18 bases=17 fixed=4 dispositioned=14`; carried `OK carried: 12 rows, 0 problems`.
- Anchors mirror: `43-evidence-check.py anchors .planning/scripts/fork-anchors.txt --rows 190` reports `rows=190 bad=0`. `fork-anchors.txt` has no row that targets `docs/FORK-DELTA.md` or `docs/ROLLOUT-v1.7.0_gg1.md`, so no anchored text was touched.
- `./gradlew testUnit --rerun -PjavaVersion=21 --offline`: `1826 tests, 1824 succeeded, 0 failed, 2 skipped`, which equals the baseline.
- `git diff --stat 30d89475 HEAD -- src VERSION CHANGELOG.md` is empty: no `src/main`, `VERSION` or `CHANGELOG` change and no test assertion touched.
- `.planning/config.json` and `.planning/state.json` are left uncommitted. Nothing was pushed.

---

_Fixed: 2026-11-02_
_Fixer: Claude (gsd-code-fixer)_
_Iteration: 1_
