---
phase: 43-document-fixes-phase-37-dispositions
verified: 2026-11-02T20:30:00Z
status: passed
score: 4/4 must-haves verified
covered_files:
  - ".github/workflows/increment-version.yml"
  - ".planning/phases/43-document-fixes-phase-37-dispositions/43-01-PLAN.md"
  - ".planning/phases/43-document-fixes-phase-37-dispositions/43-01-SUMMARY.md"
  - ".planning/phases/43-document-fixes-phase-37-dispositions/43-02-PLAN.md"
  - ".planning/phases/43-document-fixes-phase-37-dispositions/43-02-SUMMARY.md"
  - ".planning/phases/43-document-fixes-phase-37-dispositions/43-03-PLAN.md"
  - ".planning/phases/43-document-fixes-phase-37-dispositions/43-03-SUMMARY.md"
  - ".planning/scripts/confluent-archive-allowlist-v1.7.0_gg4.txt"
  - ".planning/scripts/confluent-archive-allowlist-v1.7.0_gg5.txt"
  - "docs/FORK-DELTA.md"
  - "docs/ROLLOUT-v1.7.0_gg1.md"
  - "docs/ROLLOUT-v1.7.0_gg6.md"
  - "src/test/java/com/clickhouse/kafka/connect/ci/ConfluentArchiveCheck.java"
  - "src/test/java/com/clickhouse/kafka/connect/ci/ConfluentArchiveCheckUnitTest.java"
  - "src/test/java/com/clickhouse/kafka/connect/ci/ReleaseWorkflowGuardUnitTest.java"
covered_digest: "v4:sha256:84d91c0cc0c1ff9ee2a7cea42d3286cbb7eaec3d8c8e192460dd071fc75153c0"
behavior_unverified: 0
overrides_applied: 0
---

# Phase 43: Document Fixes & Phase 37 Dispositions Verification Report

**Phase Goal:** The published documents stop carrying the errors found at the gg6 close, and every row that Phase 37 left open has a decision and an owner, so the release phase starts with no undecided debt. A `src/main` row changes only on the user's approval; without approval it is deferred with an owner and a re-open trigger.
**Verified:** 2026-11-02
**Status:** passed
**Re-verification:** No, initial verification

## Goal Achievement

### Observable Truths

| # | Truth (ROADMAP success criterion) | Status | Evidence |
|---|-----------------------------------|--------|----------|
| 1 | gg1 WR-08 item 6 corrected, FORK-DELTA gg6 lines in past tense, FUT-05 named by ID | VERIFIED | `docs/ROLLOUT-v1.7.0_gg1.md` item 6: "any line" claim removed. The `zstd-jni`/executable-temp-folder claim is now "measured by the maintainers in research ... not re-run on the release tip", pointing to rows D9/D13 of the gg6 note (both exist, D9 labelled "measured in research (not re-run)"). Client V1 is stated for the two lines observed (0.9.6, 0.11.0), and the connector-path claim is labelled "not observed". The V1 node does set `compress=false` (`ClickHouseHelperClient.java:156`). FORK-DELTA: the "are created by the release workflow" sentence (gg6 intro paragraph, line 295 paragraph) is now past tense citing publish run `41298074815`. I confirmed via `gh api` that the run is "Release custom_build (publish)", `workflow_dispatch`, success, head SHA `72e8ccff`. Items 19/22/23 are closed and item 12 retitled. Tags `v1.7.0_gg6` and `pre-v1.7.0_gg6` exist. Row C2 of `docs/ROLLOUT-v1.7.0_gg6.md` names FUT-05, with a correction notice at the top (the user's choice "gg6 note C2 + gg7 handoff (Recommended)"). The gg7 handoff is recorded in ledger "Handoff to Phase 44" item 1. |
| 2 | Ledger has one row per Phase 37 item, each fixed with a commit pointer or deferred with owner and re-open trigger | VERIFIED | `43-ledger-check.py check` printed "OK ledger: 18 rows, 0 problems". Counts: rows=18 bases=17 fixed=4 dispositioned=14. All four pointers (`566f92e4`, `33d079a9`, `103221a4`, `7796ad0a`) resolve to commits with matching subjects. Every dispositioned row has owner "fork maintainer (the user)" and a non-trivial re-open trigger. `carried` against the Phase 37 ledger: "OK carried: 12 rows, 0 problems". |
| 3 | `src/main` rows show the user's approval in their own words or are deferred; fixes tests-first in their own commit | VERIFIED | `git diff --stat cb1ccb66 HEAD -- src/main` is empty. The four `src/main` rows (26-IN-02, 27-WR-01, 27-IN-01, 28-IN-01) and 28-IN-05(b) are dispositioned, with the user's verbatim answer quoted in the ledger and in `43-RESEARCH.md` "User Decisions Recorded" (lines 410-418): "Defer all four (Recommended)" and "Approve 37 IN-04, defer 28 IN-05(b) (Recommended)". 37-IN-04 is approved and landed as `33d079a9` (red run recorded in E-03: one failing testcase of type MultipleFailuresError, then the one added CHAIN line). 37-IN-06 landed as `7796ad0a` (red then green, E-04). 37-IN-04 puts the new fixture test and the CHAIN line in the same commit. That file is test code, so it still counts as its own commit. |
| 4 | Header names the guard class that guards it, pin guard passes, no assertion deleted or weakened, phase gate | VERIFIED | `.github/workflows/increment-version.yml` header now reads "BumpVersionScriptUnitTest pins the rules, and IncrementVersionWorkflowGuardUnitTest pins this file." Six workflow guard classes passed 188/188 (E-02). I re-ran `./gradlew testUnit --rerun -PjavaVersion=21 --offline` with Docker unreachable: exit 0, 1826 tests, 0 failures, 0 errors, 2 skips, as expected. Anchors check: rows=190 bad=0. `git diff cb1ccb66 HEAD -- src/test` removes exactly one line, the old size condition `if (result.libJarNames().size() != 1) {`, and no assertion. Added tests: 2 (matches `43-expected-added.tsv`). Full suite not run because `src/main` is unchanged; E-10 records why. |

**Score:** 4/4 truths verified (0 present, behavior-unverified)

### Required Artifacts and Key Links

| Artifact | Status | Details |
|----------|--------|---------|
| `.github/workflows/increment-version.yml` | VERIFIED | Header comment only (4 lines changed, comment-only per E-02). |
| `ReleaseWorkflowGuardUnitTest.java` | VERIFIED | +15 lines, 0 deleted. Fixture test "CIH-04: fixture: the chain rule requires the tag gate after the tests gate" is present, and `TAG_GATE_STEP` is in `CHAIN`. |
| `ConfluentArchiveCheck.java` and `ConfluentArchiveCheckUnitTest.java` | VERIFIED | Size condition changed from "not one" to "more than one". The new zero-jar test (CI-20) was added (+35 lines). |
| gg4 and gg5 allow-lists | VERIFIED | Only line 1 changed in each, now naming `.github/scripts/compare_confluent_archives.py`. The gg6 list is byte-identical to its `v1.7.0_gg6` tag copy (`cmp` identical). The 25 list is untouched. |
| `43-DISPOSITION-LEDGER.md` | VERIFIED | 18 rows, with a Phase 44 handoff section. |
| `VERSION`, `CHANGELOG.md` | VERIFIED | Unchanged from the phase start. |

### Requirements Coverage

| Requirement | Source Plan | Description | Status | Evidence |
|-------------|------------|-------------|--------|----------|
| DOC-01 | 43-02 (and 43-03) | gg1 half of WR-08, FORK-DELTA past tense, FUT-05 by ID | SATISFIED | Truth 1. |
| DOC-02 | 43-01, 43-03 | Every Phase 37 row has a disposition with an owner | SATISFIED | Truths 2, 3 and 4. |

REQUIREMENTS.md marks both Complete, and no orphaned Phase 43 IDs exist.

### Behavioral Spot-Checks and Probes

| Check | Result | Status |
|-------|--------|--------|
| `testUnit --rerun`, Docker unreachable (full) | 1826 tests, 0 failed, 0 errors, 2 skipped | PASS |
| `43-evidence-check.py anchors ... --rows 190` | rows=190 bad=0 | PASS |
| `43-ledger-check.py check` and `carried` | 0 problems | PASS |
| Probes | No probe-*.sh declared | N/A |

### Anti-Patterns Found

No TBD, FIXME or XXX markers in the added lines of `src/test`, `.github`, `docs` or `.planning/scripts`.

### Warnings (non-blocking)

1. **`43-REVIEW-DISPOSITION.md` is stale.** It lists WR-01 and WR-02 as `open`, with total open 4, but `43-REVIEW-FIX.md` records them fixed in `d77da0f9` and `5c6c40e1`, and both commits exist and change the docs as described. WR-03 is correctly `skipped`. IN-01 and IN-02 remain open Infos. This is bookkeeping and does not affect the phase goal. The orchestrator may want to re-run the disposition gate or set these two rows to `fixed` before closing the phase.
2. **WR-03 is deferred to Phase 44.** Two `HEAD`-relative reproduction commands in FORK-DELTA (about lines 184 and 188) still do not reproduce their stated figures. The ledger "Handoff to Phase 44" item 4 assigns the refresh to Phase 44's raw-numbers pass, so it is not a Phase 43 gap. Phase 44's verifier should check that no `HEAD`-relative command is left in a measured-figures paragraph.

### Human Verification Required

None. The user's approvals are recorded verbatim in the committed research file.

### Gaps Summary

No gaps. All four ROADMAP success criteria hold against the repository. `src/main`, `VERSION` and `CHANGELOG.md` are unchanged. No test assertion was deleted or weakened. Every Phase 37 row is fixed with a resolvable commit or deferred with an owner and a re-open trigger.

---

_Verified: 2026-11-02_
_Verifier: Claude (gsd-verifier)_
