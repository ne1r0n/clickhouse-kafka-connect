---
phase: 10-test-harness-reconciliation
plan: 03
subsystem: testing
tags: [testcontainers, gradle, clickhouse-kafka-connect, harness, merge-checklist, requirements-closeout]

requires:
  - phase: 10-test-harness-reconciliation
    provides: "Plan 10-01's harness re-composition (shared container, MBean sweep, latest-default fix) and plan 10-02's two-clean-full-run acceptance gate — the evidence every flip in this plan rests on"
provides:
  - "HARN-04, HARN-05, HARN-06 and HARN-07 discharged as recorded evidence acts, each backed by a command re-run at execution time rather than a citation of a planning document"
  - "All 17 Phase-10-owned MERGE-CHECKLIST.md Section 1 rows flipped from pending to done; zero Phase-10 rows remain pending"
  - "MERGE-CHECKLIST.md Recorded Facts 15 (the image-default reversion, the seventh silently-vanishing fork behaviour) and 16 (the container-ownership promotion and its accepted cost)"
  - "REQUIREMENTS.md's HARN-01..07 checkboxes closed with Complete traceability rows, plus a correction blockquote recording which four closed on evidence rather than work, the server-version gap, HARN-03's re-derive branch, and how HARN-02's collision clause was discharged"
affects: [phase-11-publication]

actuals:
  tokens: 8345
  tasks: 3
  commits: 2

tech-stack:
  added: []
  patterns: []

key-files:
  created: []
  modified:
    - .planning/MERGE-CHECKLIST.md
    - .planning/REQUIREMENTS.md

key-decisions:
  - "HARN-04/05/06/07 closed on re-run evidence captured inside this plan's own task 1, not by citing 10-CONTEXT.md's measured-state table — the tree had moved twice since that table was written (plan 10-01 edited five source files and added two)."
  - "The six conflicted sink-task test file rows closed by re-verification (plan 10-02's two clean full runs), not by edits — five of the six were untouched by this phase; the two Toxiproxy proxy classes changed by exactly one method each (the usesSharedContainer() override, commit 28b14b0a)."
  - "Pre-existing cross-phase notes (Phase 7 D-06/D-07 on ClickHouseBase.java, Phase 8 D-02 on ClickHouseSinkTaskTest.java, Phase 8 D-13 on ClickHouseTestHelpers.java) were appended to, never overwritten, per the annotate-don't-flip convention."

requirements-completed: [HARN-04, HARN-05, HARN-06, HARN-07]

coverage:
  - id: D1
    description: "HARN-04, HARN-05, HARN-06 and HARN-07 discharged as recorded evidence acts — every claim backed by the literal output of a command re-run in task 1, not a citation"
    requirement: "HARN-04, HARN-05, HARN-06, HARN-07"
    verification:
      - kind: other
        ref: "git ls-files tracked-path check: ClickHouseTestHelpers.java, SchemaTestData.java, SchemalessTestData.java, FromVersionConditionExtension.java each exactly 1 path; ClickHouseAPI.java 0 paths, 0 references"
        status: pass
      - kind: other
        ref: "env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew compileTestJava compileTestFixturesJava compileIntegrationTestJava (BUILD SUCCESSFUL, 7 class files under build/classes/java/integrationTest, no NO-SOURCE)"
        status: pass
      - kind: other
        ref: "env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew compileIntegrationTestJava as sole task (BUILD SUCCESSFUL in 663ms)"
        status: pass
      - kind: other
        ref: "gradle/libs.versions.toml grep: testcontainers = \"2.0.5\""
        status: pass
    human_judgment: false
  - id: D2
    description: "All 17 Phase-10-owned MERGE-CHECKLIST.md Section 1 rows flipped pending to done, with per-group closure notes citing commits/runs, plus Recorded Facts 15 and 16 appended"
    verification:
      - kind: other
        ref: "grep-based row-count/status verify: phase-10 rows pending=0, phase-10 rows done=17, total pending=6 (Phases 6/8 Section 2 rows untouched), Section 1 row count unchanged at 40, pre-existing Phase 7/8 notes intact, Facts 15/16 present with commit SHAs"
        status: pass
    human_judgment: false
  - id: D3
    description: "REQUIREMENTS.md's HARN-01..07 closed (checkboxes + Traceability rows) with a correction blockquote naming the four evidence-closed requirements, the moot HARN-07 sequencing clause, the server-version gap, HARN-03's re-derive branch, and HARN-02's discharge mechanism"
    verification:
      - kind: other
        ref: "grep-based closure verify: all 7 HARN checkboxes [x], all 7 Traceability rows Complete, 6 requirements still open, 53 total unchanged, P4-WR-01 still a Future Requirement, only REQUIREMENTS.md/MERGE-CHECKLIST.md touched"
        status: pass
    human_judgment: true
    rationale: "Task 3's own <verify> carries a <human-check> requiring a stranger-read confirmation that the appended blockquote leaves a reader with the true picture of the phase (four of seven requirements already satisfied, the server-version gap the real finding, HARN-03's re-derive branch) rather than a flattering one. That judgment is deferred to end-of-phase UAT per workflow.human_verify_mode=end-of-phase; not self-certified here."

duration: 7min
completed: 2026-09-20
status: complete
---

# Phase 10 Plan 03: Test Harness Reconciliation Bookkeeping Closeout Summary

**Closed Phase 10's books by re-running every HARN-04/05/06/07 verifying command live against the current tree, flipping all 17 Phase-10-owned MERGE-CHECKLIST.md rows to done with two new Recorded Facts, and closing HARN-01 through HARN-07 in REQUIREMENTS.md with a correction blockquote naming which four requirements closed on evidence rather than new code.**

## Performance

- **Duration:** 7 min
- **Started:** 2026-09-20T15:43:20Z
- **Completed:** 2026-09-20T15:49:46Z
- **Tasks:** 3
- **Files modified:** 2

## Accomplishments

- **HARN-04/05/06/07 discharged as recorded evidence acts, not citations.** Re-ran every verifying command against the tree as plan 10-01 left it: `git ls-files` shows exactly one `ClickHouseTestHelpers.java` at the canonical `src/testFixtures` path with no non-canonical imports; `SchemaTestData.java`/`SchemalessTestData.java`/`FromVersionConditionExtension.java` each resolve to exactly one tracked path; `ClickHouseAPI.java` is tracked at zero paths with zero references under `src/`; `compileTestJava`/`compileTestFixturesJava`/`compileIntegrationTestJava` succeed together and `compileIntegrationTestJava` succeeds alone, producing 7 real class files (not `NO-SOURCE`); `gradle/libs.versions.toml` pins Testcontainers at `2.0.5`. The task changed no file — confirmed by a clean `git status --porcelain` over the task's own scope.
- **17 MERGE-CHECKLIST.md Section 1 rows flipped from pending to done.** The seven `src/integrationTest` rows (closed by task 1's compile/grep evidence), the `ClickHouseBase.java` row (closed by plan 10-01, note appended to the existing Phase 7 D-06/D-07 note rather than overwritten), the six conflicted sink-task test files (closed by re-verification via plan 10-02's two clean full runs — five untouched, two changed by exactly one method each), and the helper + testFixtures test-data rows (closed by task 1's grep evidence, note appended to the existing Phase 8 D-13 note). Zero Phase-10 rows remain pending; the six Section 2 rows owned by Phases 6 and 8 are untouched; Section 1's row count is unchanged at 40.
- **Recorded Facts 15 and 16 appended**, citing real commit SHAs and real measured numbers from plans 10-01/10-02: Fact 15 records the `latest` → `25.8.18.1` image-default reversion as the seventh silently-vanishing fork behaviour and the deliberate Phase-11 question of whether `tests.yaml` should stop pinning `CLICKHOUSE_VERSION` explicitly; Fact 16 records the container-ownership promotion from per-class to per-JVM-fork and its accepted cost (server-global state now outliving the class that created it), with plan 10-02's measured wall-clock (172s/174s full-suite, 2s `testUnit`) recorded for Phase 11's CI timeout measurement.
- **REQUIREMENTS.md's Test Harness section fully closed.** All seven `HARN-01`..`HARN-07` checkboxes checked and their Traceability rows read `Complete`. An appended correction blockquote states plainly that `HARN-04`/`05`/`06`/`07` were already satisfied before Phase 10 began — by the Phase 6 merge and Phase 7's sliver — and closed on re-measured evidence, names the evidence for each, records that `HARN-07`'s sequencing clause was moot on arrival, points at `MERGE-CHECKLIST.md` Recorded Fact 15 for the server-version gap (a gap in the requirement set itself, since no HARN requirement named which server the harness starts), records that `HARN-03` took the re-derive branch rather than the fallback the requirement text also permits, and records that `HARN-02`'s collision clause is discharged by `MBeanServerUtils`'s rethrow mechanism rather than a new detector. `## Coverage` arithmetic (53 total, 53 mapped) is unchanged; `P4-WR-01` remains a Future Requirement.

## Task Commits

Each bookkeeping task was committed atomically (task 1 produced no diff, per its own design):

1. **Task 1: discharge HARN-04/05/06/07 as recorded evidence acts** - no commit (task changes no file; evidence held for tasks 2 and 3)
2. **Task 2: flip the 17 checklist rows and append Recorded Facts 15/16** - `2b8d9009` (docs)
3. **Task 3: close HARN-01..07 in REQUIREMENTS.md** - `e836b318` (docs)

**Plan metadata:** committed as part of this SUMMARY's own docs commit.

_Note: TDD is not applicable to this plan._

## Files Created/Modified

- `.planning/MERGE-CHECKLIST.md` - 17 Section-1 rows flipped pending → done with closure notes; Recorded Facts 15 and 16 appended
- `.planning/REQUIREMENTS.md` - `HARN-04`..`HARN-07` checkboxes closed, all 7 `HARN-*` Traceability rows set `Complete`, correction blockquote appended beneath the Test Harness section

## Decisions Made

See `key-decisions` in frontmatter. All decisions were locked in `10-CONTEXT.md` (D-10, D-11) and the plan's own task instructions prior to execution; this plan applied them without deviation.

## Deviations from Plan

None — plan executed exactly as written. One note worth recording for a future reader, not a deviation from this plan's own scope: task 1's and task 2's/task 3's literal `git status --porcelain` verify commands additionally showed `.planning/milestone.lock` and `.planning/state.json` as modified. Both were already modified *before* this plan began execution — visible in the orchestrator's own git-status snapshot at session start (`state.json`'s phase-10 status flipping `pending` → `in_progress` and its `updated_at` timestamp, both GSD session-bookkeeping writes unrelated to this plan's task content). No task in this plan wrote to either file; the plan's own tasks only ever staged and committed `.planning/MERGE-CHECKLIST.md` and `.planning/REQUIREMENTS.md`. This is recorded here because a verifier re-running the plan's literal verify scripts against the current working tree (rather than against the diff this plan's commits introduced) would see the same two extra paths and should not read them as scope creep.

## Issues Encountered

None beyond the pre-existing-dirty-state note above.

## User Setup Required

None - no external service configuration required.

## Next Phase Readiness

- Phase 10 is complete: `HARN-01` through `HARN-07` are all `Complete` in `REQUIREMENTS.md`, and `MERGE-CHECKLIST.md` has zero Phase-10-owned rows pending — Phase 11's publication gate, which asserts zero pending rows before `custom_build` is brought forward, is now meaningful for Phase 10's contribution. The six remaining pending rows belong to Phases 6 and 8 and are unaffected by this plan.
- `STATE.md`'s `[Merge]` "six silently-vanishing fork behaviours" blocker and `[Phase 10]` "half done" blocker are both stale as of this plan (both were actually resolved by plan 10-01, not this plan) — see the STATE.md update accompanying this SUMMARY's own docs commit for the corrected wording (seven behaviours now, all re-applied and canary-green; the `ClickHouseTestHarnessTest` build-exclusion deletion confirmed via commit `b39ed8fd`).
- Phase 11 (Full Suite, CI & Publication) is next: `SUITE-01..03`, `CI-05..07` remain `Pending` in `REQUIREMENTS.md`'s Traceability table, and `.planning/MERGE-CHECKLIST.md`'s six Section 2 rows owned by Phases 6/8 are still open for whoever picks them up before Phase 11's own gate.
- Wall-clock figures for Phase 11's `timeout-minutes: 45` CI measurement are now consolidated in `MERGE-CHECKLIST.md` Recorded Fact 16: 172s/174s full-suite (654 tests each), 2s `testUnit` (327 tests), all from plan 10-02's live runs on a single-fork local machine.

## Self-Check: PASSED

Both key files (`MERGE-CHECKLIST.md`, `REQUIREMENTS.md`) found on disk with the expected content; both task commits (`2b8d9009`, `e836b318`) found in git history via `git log --oneline --all`. `commits: 2` in this SUMMARY's `actuals` frontmatter is the `git rev-list --count` measurement from the plan-start ledger (`4bb884b2`) to `HEAD` at SUMMARY-write time, before this SUMMARY's own docs commit.

---
*Phase: 10-test-harness-reconciliation*
*Completed: 2026-09-20*
