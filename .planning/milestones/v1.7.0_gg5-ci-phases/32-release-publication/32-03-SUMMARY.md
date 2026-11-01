---
phase: 32-release-publication
plan: "03"
subsystem: release-documentation
tags: [changelog, fork-delta, ci-workflows, raw-numbers, evidence]

requires:
  - phase: 32-release-publication
    provides: "plan 32-01 evidence E-01..E-05 and plan 32-02 rollout note and E-10, E-11"
  - phase: 30-release-workflows-pin-guard
    provides: "release-path workflow facts, the vnull defect (E-06), the dry runs (E-09), review IN-01"
  - phase: 31-fork-owned-nightly-integration-tests
    provides: "nightly, byte guard and Java 17 harness facts (E-10, E-17, E-18)"
provides:
  - "CHANGELOG.md section 1.7.0_gg5, 2026-10-26 (Important Changes, Known Issues, Build and CI)"
  - "docs/FORK-DELTA.md: one CI workflows group, gg5 identity, Java 17 harness row, skip-inventory and syncer paragraphs, after-gg5 raw numbers"
  - "32-COMMAND-EVIDENCE.md E-20, E-21, E-22; E-03..E-05 verdicts resolved to yes"
affects: [32-04, 32-05, 32-06, 32-07]

plan_head_before: 043896b0623942cd973558a3306c43fbf80053a6
plan_head_after: a92aae5fda780cb666346978721a22bde8707a34

actuals:
  tokens: 8458
  tasks: 3
  commits: 3

tech-stack:
  added: []
  patterns:
    - "one FORK-DELTA group per concern, every workflow row tied to the backs-IDs of its fork-anchors rows"
    - "raw numbers measured once on the final tree and recomputed by the verify command"

key-files:
  created: []
  modified:
    - CHANGELOG.md
    - docs/FORK-DELTA.md
    - .planning/phases/32-release-publication/32-COMMAND-EVIDENCE.md

key-decisions:
  - "Four-row archive wording (32-AMENDMENT-01) used in the CHANGELOG, FORK-DELTA syncer paragraph and E-20: the archive differs from the gg4 one only in the release identity (Version constant, the two classes that inline it, manifest Implementation-Version); the plan's two-row wording was not used"
  - "FORK-DELTA's Dependabot row now says version updates are live on custom_build and points at E-05 instead of naming pull request numbers"
  - "The skip-inventory sentence says the nightly runs the whole integrationTest task when dispatched or scheduled; the green run is stated as a local one (E-18), because no nightly run on GitHub exists yet"

requirements-completed: [CIG-02]

coverage:
  - id: D1
    description: "CHANGELOG 1.7.0_gg5 section dated 2026-10-26, led by no connector code changes, with Known Issues (vnull) and seven Build and CI bullets; released sections byte-identical to P32_BASE, no planning ID, no line over 120 characters"
    requirement: CIG-02
    verification:
      - kind: other
        ref: "plan 32-03 Task 1 and Task 3 verify: GG5-IDENTITY-OK and CHANGELOG-GG5-OK"
        status: pass
    human_judgment: false
  - id: D2
    description: "FORK-DELTA CI workflows group: all workflow, script, guard and Dependabot rows, every listed ID and every .github anchor backs-ID in a Pinned-by cell, the DEPS-F4 paragraph and the stale default-branch clauses gone, Node 24 stated, harness row, skip inventory and syncer paragraph (E-21)"
    requirement: CIG-02
    verification:
      - kind: other
        ref: "plan 32-03 Task 2 verify: FORK-DELTA-CI-GROUP-OK"
        status: pass
    human_judgment: false
  - id: D3
    description: "After-gg5 raw-number rows equal git diff --shortstat v1.7.0 HEAD for both scopes (287 / 31774 / 731 and 37 / 2877 / 241), adjacent to the gg4 rows, with the two gg5 commands (E-22)"
    requirement: CIG-02
    verification:
      - kind: other
        ref: "plan 32-03 Task 3 verify: RAW-NUMBERS-GG5-OK"
        status: pass
    human_judgment: false
  - id: D4
    description: "Whether the CHANGELOG bullets and FORK-DELTA rows read clearly to an operator and the next syncer"
    requirement: CIG-02
    verification: []
    human_judgment: true
    rationale: "Readability of prose is decided in the phase review round (D-04); no test asserts it"

duration: 9min
completed: 2026-10-26
status: complete
---

# Phase 32 Plan 03: CHANGELOG and FORK-DELTA for 1.7.0_gg5 Summary

**A dated `1.7.0_gg5` CHANGELOG section that leads with "no connector code changes", and a FORK-DELTA with one CI workflows group (release path with `gh release create`, nightly, both guards, Dependabot), the gg5 identity and raw numbers refreshed once on the final tree (287 / 31774 / 731 wide, 37 / 2877 / 241 for `src/main`).**

## Performance

- **Duration:** about 9 min
- **Started:** 2026-10-26T21:20:33Z
- **Completed:** 2026-10-26T21:29:00Z
- **Tasks:** 3 (one tracer, two auto)
- **Files modified:** 3 (`CHANGELOG.md`, `docs/FORK-DELTA.md`, `32-COMMAND-EVIDENCE.md`)

## Accomplishments

- **CHANGELOG heading date: 2026-10-26.** Intro, then Important Changes: (1) no connector code changes, `src/main` identical to `1.7.0_gg4`; (2) `version=v1.7.0_gg5` and `clickhouse-kafka-connect/v1.7.0_gg5` (E-02); (3) the archive differs only in the release identity, in the four-row wording of `32-AMENDMENT-01.md` (E-03). Known Issues: the `increment-version.yml` `vnull` defect. Build and CI: seven bullets (release path, `increment-version.yml` on Node 24, the nightly with `NIGHTLY_ENABLED`, the pin guard, the byte guard with the nightly guard, the Java 17 harness, `dependabot.yml`). No planning ID, no line over 120 characters; everything from the gg4 heading down equals `P32_BASE`.
- **FORK-DELTA identity:** intro names `v1.7.0_gg5` and links `ROLLOUT-v1.7.0_gg5.md`; the gg4 note joins the chain of older notes; the VERSION row reads `v1.7.0_gg5` with `CIG-02` added; the MBean row's current version is gg5.
- **FORK-DELTA CI workflows group** (`### CI workflows`, build table renamed `### Build and dependencies`, Contents updated):
  - Moved: the `tests.yaml` row unchanged; the `test-head.yaml` deletion row (the `main` default-branch clause replaced by "`custom_build` is the fork's default branch (since 2026-10-14) and the file is deleted there", source E-05 and `30-COMMAND-EVIDENCE.md` E-07 carry); the Dependabot row (the stale "`main` here ... inert" clause replaced by the live state on `custom_build`, source E-05 and the `git diff v1.7.0_gg4 HEAD` of `dependabot.yml`).
  - Added: `release.yaml` and `pre-release.yaml` (source the two files, `30-COMMAND-EVIDENCE.md` E-09, the 0-count anchor rows for DEPS-F4); `increment-version.yml` with Node 24, `semver@7.8.5` and the `vnull` defect (the file, `30-REVIEW.md` IN-01, `30-COMMAND-EVIDENCE.md` E-06); the nightly with `failed_tests_summary.py` (the workflow, the script docstring, `31-COMMAND-EVIDENCE.md` E-18); the pin guard (`WorkflowActionPinGuardUnitTest` class comment); the byte guard (class, `31-COMMAND-EVIDENCE.md` E-10, E-11, this phase's E-05).
  - Replaced: the paragraph that said the release workflows are left as upstream ships them, now one short paragraph under the table.
  - Test harness row for `ConfluentPlatform.java` (`confluentinc/cp-server-connect:7.9.6`, datagen plugin directory from `cnfldemos/cp-server-connect-datagen:0.6.4-7.6.0`); skip inventory keeps `The current suite skips 18 tests.` and names the nightly; the syncer paragraph for `v1.7.0_gg5` (rollback tag `pre-v1.7.0_gg5` on `21c51598`, `confluent-archive-allowlist-32.txt`, byte guard).
- **Raw numbers (E-22):** wider scope after gg5 = 287 files, 31774 insertions, 731 deletions; `src/main` after gg5 = 37 / 2877 / 241 (equals the gg4 row). Growth gg4 to gg5 by directory: `.github` 6 files 616/92, `src/test` 5 files 1486/25, `src/integrationTest` 1 file 23/1; `build.gradle.kts` and `gradle` unchanged. The two gg5 commands are in the command block.
- **Audits:** E-20 (with `changelog_date: 2026-10-26`), E-21 (group audit with the source of every statement), E-22; E-03, E-04 and E-05 flipped from `pending (32-03)` to `yes`. No verdict reads pending.

## Task Commits

1. **Task 1 (tracer): the gg5 identity in both documents** - `1027b000` (docs). The tracer `<verify>` (automated only) was re-run at the committed tip and passed: GG5-IDENTITY-OK, so expansion continued.
2. **Task 2: FORK-DELTA's CI workflows group and E-21** - `39465506` (docs)
3. **Task 3: CHANGELOG Known Issues and Build and CI, raw numbers, E-20, E-22, verdicts** - `a92aae5f` (docs)

**Plan metadata:** the commit that carries this SUMMARY.

## Files Created/Modified

- `CHANGELOG.md` - the `1.7.0_gg5` section above the gg4 section
- `docs/FORK-DELTA.md` - gg5 identity, CI workflows group, harness row, skip-inventory and syncer paragraphs, raw numbers
- `.planning/phases/32-release-publication/32-COMMAND-EVIDENCE.md` - E-20, E-21, E-22 and the resolved verdicts
- Not committed (scratch): `/tmp/gsd32/exec/fd_task2.py`, `v2.py`, `v3.py`, `v4.py`, `e20_22.md`, `e21.md`

## Decisions Made

- The archive statement uses the four-row wording binding from `32-AMENDMENT-01.md` ("differs only in the release identity: the `Version` constant, the two classes that inline it, and the manifest `Implementation-Version`"); the plan's "only Version.class and the manifest" text was not used anywhere.
- The Dependabot row cites this phase's E-05 for the open pull requests instead of listing their numbers, which will go stale.
- The nightly's "green" is stated as a local run (E-18); the first CI run is plan 32-07's.

## Deviations from Plan

None - plan executed exactly as written, with the binding four-row wording of `32-AMENDMENT-01.md` applied where the plan text said two rows (CHANGELOG third Important Changes bullet, FORK-DELTA syncer paragraph, E-20, E-21). One wording fix during Task 2: the plan's verify requires the text `Node 24`, so the increment-version row reads "the job runs Node 24 instead of Node 16" (the first draft said "Node is 24" and the verify caught it before the commit).

**Total deviations:** 0 auto-fixed
**Impact on plan:** none.

## Issues Encountered

- A background run of `.planning/scripts/verify-merge-invariants.sh 54e63648 HEAD` (a sanity check outside the plan's verify list) exceeded the 120 s foreground limit and was moved to the background; it is not part of this plan's gates and its result is not relied on here.

## Known Stubs

None - documentation only.

## Threat Flags

None - no new network endpoint, auth path or file access pattern; only documents and evidence changed.

## Facts left out for lack of a source

- Why the nightly's timeouts are exactly 75 and 85 minutes beyond the workflow header's statement that they come from Phase 31's measured run.
- Any observed scheduled nightly run (none exists yet).
- Why upstream's `increment-version.yml` chose Node 16 (only the change to 24 is stated).

## User Setup Required

None - no external service configuration required.

## Next Phase Readiness

- Ready for 32-04 and later plans: the documents are written on the final non-publication tree; `FORK-DELTA.md` gg5 raw-number rows must be re-run on the `v1.7.0_gg5` tag by plan 32-06 (`RAW-NUMBERS-GG5-OK` recomputes against `HEAD`).
- Dirty tree not touched: ` M gradlew.bat`, `.planning/state.json`, `.planning/milestone.lock`.

## Self-Check: PASSED

`CHANGELOG.md`, `docs/FORK-DELTA.md` and the evidence file exist; commits `1027b000`, `39465506` and `a92aae5f` are ancestors of HEAD; GG5-IDENTITY-OK, FORK-DELTA-CI-GROUP-OK, CHANGELOG-GG5-OK and RAW-NUMBERS-GG5-OK print on the committed tip; `git diff --name-only 8dcbbfc1 HEAD -- . ':(exclude).planning'` lists exactly `CHANGELOG.md`, `VERSION`, `docs/FORK-DELTA.md` and `docs/ROLLOUT-v1.7.0_gg5.md`; no evidence verdict reads pending; origin was not contacted and nothing was pushed.

---
*Phase: 32-release-publication*
*Completed: 2026-10-26*
