---
phase: 08-orchestration-jmx-re-plumbing
plan: 03
subsystem: merge-bookkeeping
tags: [merge-checklist, orch-01, orch-04, free-01, verification-only, jmx]

requires:
  - phase: 08-orchestration-jmx-re-plumbing
    provides: "plan 08-01's connector=<name> JMX re-plumb and commit hashes (34afc938, 75aa8576, 1927992f); plan 08-02's live-suite evidence and TableTest.extractNullables() triage"
provides:
  - "Measured evidence discharging ORCH-01 (grep + constructor-arity proof), ORCH-04 (Utils.java/Processing.java read in full, delta stated), and FREE-01 (verbatim classification quote, closed citing upstream issue #818)"
  - "Eight Phase-8-owned MERGE-CHECKLIST.md rows flipped to done"
  - "Two Phase-10-owned rows annotated with Phase 8's early-landing commits, left pending"
  - "Three new Recorded Facts (11, 12, 13) closing D-07, D-11, and carrying plan 08-02's extractNullables() triage onto the permanent record with its ownership gap stated plainly"
affects: [09-metadata-free-json-path, 10-harness-and-live-suite-restoration, 11-full-suite-ci-publication]

actuals:
  tokens: 5217
  tasks: 2
  commits: 1

tech-stack:
  added: []
  patterns: []

key-files:
  created: []
  modified:
    - .planning/MERGE-CHECKLIST.md

key-decisions:
  - "Task 1 gathered evidence only — zero files under src/ or build.gradle.kts were touched, per the plan's hard prohibition and REQUIREMENTS § FREE-01's 'confirm and record, never fix'."
  - "Discovered and documented (not fixed) that the plan's own planning-time premise 'ProcessingTest.java is byte-identical to v1.6.0' is now stale: plan 08-01's own Rule-3 compile fix (commit 34afc938) updated one call site in that file. Reported per the plan's own instruction to treat unexpected emptiness/non-emptiness as a failure signal, not to rationalize it away."
  - "Corrected a git staging error before it reached permanent history: the first commit attempt (no trailing pathspec) picked up two pre-existing staged .planning/ files (milestone.lock, 08-DISCUSS-CHECKPOINT.json) that are explicitly not this plan's to touch. Used git reset --soft HEAD^ (non-destructive) and re-committed scoped to .planning/MERGE-CHECKLIST.md only — the same recovery shape plan 08-01 documented for the identical mistake."

requirements-completed: [ORCH-01, ORCH-04, FREE-01]

coverage:
  - id: D1
    description: "ORCH-01: ProxySinkTask/ClickHouseSinkTask resolved against upstream's DeliveryStrategy/ChunkFlusher layering and 5-arg Processing construction, with zero stale 4-arg call sites anywhere in the tree"
    requirement: ORCH-01
    verification:
      - kind: other
        ref: "grep -rn 'new Processing(' src/ --include='*.java' -> 16 call sites, all 5-arg; Processing.java declares exactly 1 constructor; git diff v1.6.0 HEAD is 0 lines for Processing.java, DeliveryStrategy.java, ChunkFlusher.java, DirectDeliveryStrategy.java, AtLeastOnceBufferStrategy.java, ExactlyOnceBufferStrategy.java"
        status: pass
    human_judgment: true
    rationale: "This is a documentation/record-keeping requirement (assert the evidence and write it down), not something an automated test proves on its own — the deliverable is the grep output and the diff measurements themselves, reproduced fresh at execution time rather than copied from planning-time claims."
  - id: D2
    description: "ORCH-04: Utils.java and Processing.java read in full and what was read written down; UtilsTest.java/ProcessingTest.java/RangeContainerTest.java confirmed against upstream's versions"
    requirement: ORCH-04
    verification:
      - kind: other
        ref: "Utils.java (304 lines) and Processing.java (267 lines) read in full this session; git diff v1.6.0 HEAD -- Utils.java is a 29-line import-reorder-only delta (0 non-import body lines changed); UtilsTest.java and RangeContainerTest.java are 0-line diffs; ProcessingTest.java is NOT 0-line (see Deviations) but its only substantive line is a Phase-8-caused constructor-arg update, not new test logic"
        status: pass
    human_judgment: true
    rationale: "Whether 'read and record' has actually been discharged (versus restating the file's existence) is itself the human-check the plan's own acceptance criteria ask for; the ProcessingTest.java finding required judgment to classify as a stale planning-time premise rather than a real regression."
  - id: D3
    description: "FREE-01: Utils.handleException confirmed to classify ServerException (client V2) alongside ClickHouseException (client V1), closed citing upstream issue #818, no new test added"
    requirement: FREE-01
    verification:
      - kind: other
        ref: "Utils.java:60-62 (isServerError) and :77-80 (handleException's ternary) quoted verbatim; ClickHouseException confirmed as client V1's type, ServerException (com.clickhouse.client.api.ServerException) confirmed as client V2's type; Utils holds exactly 2 private static final String fields and 0 mutable static fields"
        status: pass
    human_judgment: true
    rationale: "REQUIREMENTS § FREE-01 asks for a confirmation and a record, not test coverage — closing it is a documentation act the plan's own prohibitions forbid pinning with a test."
  - id: D4
    description: "MERGE-CHECKLIST.md: eight Phase-8-owned rows flipped, two Phase-10-owned rows annotated and left pending, three Recorded Facts appended before the closing JSON-path paragraph"
    verification:
      - kind: other
        ref: "grep-verified: all 8 rows read '| done |'; both annotated rows read '| pending |' and contain 'Phase 8'; items 11/12/13 each appear exactly once, item 10 un-renumbered, closing paragraph still last; commit 813d08e3 removed 10 lines (within the 8-12 expected band)"
        status: pass
    human_judgment: true
    rationale: "Whether a checklist row correctly reflects ownership (not stealing another phase's row, not silently satisfying Phase 11's publication gate) is exactly the judgment Phase 11's zero-pending gate depends on staying honest."

duration: ~11min
completed: 2026-09-19
status: complete
---

# Phase 8 Plan 3: Merge-Checklist Evidence and Recorded Facts Summary

**Discharged ORCH-01, ORCH-04 and FREE-01 by grep/diff evidence and a written record (no `src/` line touched), flipped Phase 8's eight `MERGE-CHECKLIST.md` rows to done, annotated the two Phase-10-owned rows Phase 8 landed early work against, and appended three Recorded Facts — while catching and reporting that the plan's own "`ProcessingTest.java` is byte-identical" premise had gone stale due to plan 08-01's own necessary compile fix.**

## Performance

- **Duration:** ~11 min
- **Started:** 2026-09-19T13:03:00Z (approximate — immediately following plan 08-02's completion)
- **Completed:** 2026-09-19T13:13:58Z
- **Tasks:** 2
- **Files modified:** 1 (`.planning/MERGE-CHECKLIST.md`); zero `src/` or `build.gradle.kts` files touched (Task 1 is evidence-only by design)

## Accomplishments

### Task 1 — ORCH-01 / ORCH-04 / FREE-01 evidence

- **ORCH-01 (layering + constructor arity).** `grep -rn 'new Processing(' src/ --include='*.java'` returns exactly **16** call sites — 1 in `ProxySinkTask.java:53`, 15 in `ProcessingTest.java` — every one 5-argument. `Processing.java` declares exactly **one** constructor:
  ```java
  public Processing(StateProvider stateProvider, DBWriter dbWriter, ErrorReporter errorReporter,
                    ClickHouseSinkConfig clickHouseSinkConfig, SinkTaskStatistics statistics)
  ```
  A 4-argument call site could not compile against this signature, so the already-passing compile is the real proof; the grep is its human-readable form. `git diff v1.6.0 HEAD` is **0 lines** for `Processing.java`, `DeliveryStrategy.java`, `ChunkFlusher.java`, `DirectDeliveryStrategy.java`, `AtLeastOnceBufferStrategy.java`, and `ExactlyOnceBufferStrategy.java` — upstream's buffered-delivery layering above `ProxySinkTask` and the 5-arg `Processing` construction below it are both adopted with zero fork delta.
- **ORCH-04 (read + record `Utils.java` and `Processing.java`).** Both files were read in full this session.
  - **`Utils.java`** (304 lines): a static utility class (table-name escaping, root-cause exception drilling, `handleException`'s retriable/non-retriable classification, DLQ dispatch, table-column-diff logging). Its entire delta against `v1.6.0` is **29 lines, confirmed import-reorder only** — 10 import lines moved, 0 non-import/non-blank lines changed. No production behaviour differs from upstream.
  - **`Processing.java`** (267 lines): the exactly-once state machine (`doLogic`'s `NONE`/`BEFORE_PROCESSING`/`AFTER_PROCESSING` switch, the `SAME`/`NEW`/`CONTAINS`/`OVER_LAPPING`/`ZERO`/`PREVIOUS`/`ERROR` range-overlap handling). `git diff v1.6.0 HEAD` is **0 lines** — the state machine and the `BEFORE_PROCESSING`→`AFTER_PROCESSING` flag-write sequence AGENTS.md names as consistency-critical are upstream's, byte-for-byte, unmodified by this or any prior phase.
  - `UtilsTest.java` and `RangeContainerTest.java` are confirmed **0-line diff** against `v1.6.0` — both take upstream's version wholesale, as their checklist rows require.
  - **`ProcessingTest.java` is NOT 0-line diff** — see Deviations below. This is reported, not glossed over, per the plan's own instruction that unexpected non-emptiness is a failure signal.
- **FREE-01 (confirm and record, never fix).** Quoted verbatim from `Utils.java`:
  ```java
  // :60-62
  private static boolean isServerError(Throwable t) {
      return t instanceof ClickHouseException || t instanceof ServerException;
  }
  // :77-80
  int errorCode = rootCause instanceof ServerException
          ? ((ServerException) rootCause).getCode()
          : ((ClickHouseException) rootCause).getErrorCode();
  ```
  `com.clickhouse.client.ClickHouseException` is client **V1**'s exception type; `com.clickhouse.client.api.ServerException` is client **V2**'s. Both branches are live in the merged tree today with no fork patch involved — upstream's own rewrite already closes the fork's "error classification is dead code on client V2" follow-up (`PROJECT.md` → Active), and the mechanism to cite is upstream issue **#818**. `Utils` holds exactly two `private static final String` message constants and zero mutable static fields, so the classification path is static, allocation-free, and concurrency-safe by construction. No test was added — REQUIREMENTS § FREE-01 asks for a confirmation and a record, not coverage.

### Task 2 — Checklist rows and Recorded Facts

- **Eight Phase-8-owned rows flipped to `done`:** Section 1 — `ClickHouseSinkTask.java`, `ProxySinkTask.java`, `Processing.java`, `RangeContainerTest.java`, `ProcessingTest.java`, `UtilsTest.java`. Section 2 (backtick-wrapped paths) — `` `Utils.java` `` and `` `Processing.java` ``.
- **Two Phase-10-owned rows annotated, left `pending`:** `ClickHouseSinkTaskTest.java` (four call sites rewritten to pass `null` explicitly, commit `34afc938`) and the HARN-04 `ClickHouseTestHelpers.java` row (UTF-8 fix landed early on the `src/testFixtures` copy, commit `1927992f`) — both notes start with a `**Phase 8 note (...)**` marker matching the existing Phase-7-note pattern.
- **Three Recorded Facts appended as items 11, 12, 13**, before the closing JSON-path paragraph (which survives verbatim and stays last):
  - **11 (D-07):** the operator-visible MBean ObjectName change — old/new rendered strings for both task-level and per-topic names, the two things that changed (`connector=` segment added; `version=` source moved to `Version.ARTIFACT_VERSION` at the Phase 6 merge), and the "consequence to carry, not to fix" for JMX-03's now-vestigial null-version/colon-sanitization branches.
  - **12 (D-11):** FREE-01 closed, quoting the predicate and ternary verbatim, naming both client versions, citing upstream issue #818, and stating no test was added.
  - **13 (D-13):** the `TableTest.extractNullables()` triage carried from plan 08-02 verbatim in substance — named cause (`ClickHouseHelperClient.shouldSkipDescribeRow`), the exact column (`date_number.null`), the supported explanation (upstream's own test is stale against the fork's already-shipped `DESC-01` design), and the ownership gap stated explicitly: **no existing phase (9, 10, or 11) owns this fix** — it is a named, unowned milestone-level follow-up, not silently assigned to any phase.

## Task Commits

1. **Task 1 (evidence gathering):** No commit — `files_modified: []` by design, zero `src/`/`build.gradle.kts` changes (confirmed via `git status --porcelain -- src/ build.gradle.kts` returning empty both before and after this task).
2. **Task 2 (checklist flips + Recorded Facts):** `813d08e3` (docs) — `.planning/MERGE-CHECKLIST.md` only, 35 insertions / 10 deletions.

## Files Created/Modified

- `.planning/MERGE-CHECKLIST.md` — eight status-cell flips, two annotate-not-flip notes, three new Recorded Facts (items 11-13)

## Decisions Made

- Followed the plan's locked decisions (D-07, D-10, D-11, D-13, D-14) exactly: evidence-only for ORCH-01/ORCH-04/FREE-01, no keep-or-drop disposition stated for the metadata-free JSON path anywhere, and the checklist commit landed as the phase's final commit.
- Reported the `ProcessingTest.java` byte-identity gap rather than treating it as a pass, per the plan's own critical-constraints instruction that unexpected non-emptiness is a failure signal, not something to rationalize away.

## Deviations from Plan

### Auto-fixed Issues (Rule 1 — stale plan-fact documentation, not a code bug)

**1. [Rule 1] `ProcessingTest.java` is no longer byte-identical to `v1.6.0` — the plan's planning-time premise had gone stale**

- **Found during:** Task 1's automated verify block (`git diff v1.6.0 HEAD -- ProcessingTest.java` returned a 60-line diff, not the expected 0 lines).
- **Issue:** The plan's "Verified facts established at planning time" table (and 08-CONTEXT.md's "Measured starting state" table before it) states `ProcessingTest.java` carries a 0-line diff against `v1.6.0`. That was true when 08-CONTEXT.md/08-RESEARCH.md were written, but plan **08-01** — which executed and committed before this plan started — needed to update `ProcessingTest.java`'s `@BeforeEach` constructor call from `new SinkTaskStatistics(0)` to `new SinkTaskStatistics(null, 0)` as a Rule-3 blocking-issue fix (documented in `08-01-SUMMARY.md`'s own Deviations section: "16 additional test call sites construct `SinkTaskStatistics` with the deleted 1-arg constructor", `ProcessingTest.java` explicitly named among the 8 affected files). That commit (`34afc938`) also carried a Spotless import-reorder over the same file (scoped per D-15). The premise was accurate at the time it was recorded; it drifted before this plan executed — the same class of drift 08-01-SUMMARY itself documented for `MBeanServerUtils.java`.
- **Verification:** Full diff inspected line-by-line — confirms exactly two categories of change and nothing else: (a) 10 import lines reordered (the same Spotless import-order convention as `Utils.java`'s own 29-line delta), (b) one substantive line, the constructor-arg update, tracing to commit `34afc938` via `git log --oneline v1.6.0..HEAD -- ProcessingTest.java`. No test assertion, expectation, or logic changed — the file's actual test *content* is still upstream's, adapted only for the constructor signature Phase 8's own ORCH-02 work introduced.
- **Fix:** None applied — none is needed or permitted. This task is evidence-only (`no file under src/ is edited by this task, and none should be`), and the delta is a correct, already-recorded consequence of Phase 8's own prior, sanctioned production change, not a regression to undo. Recorded here, in full, exactly as the plan's own constraints require ("report it rather than rationalising it").
- **Files modified:** None by this task. (The originating change lives in commit `34afc938`, plan 08-01.)
- **Impact on acceptance criteria:** The task's automated verify block and the "nine files carry a zero-line diff" claim in `must_haves.truths` are not literally satisfied for `ProcessingTest.java` — 8 of 9 named files are 0-line diff; `ProcessingTest.java` carries the 60-line diff described above. ORCH-04's substance (the test takes upstream's assertions wholesale) still holds: the only non-import change is a call-site adaptation forced by Phase 8's own already-committed, already-reviewed production work, not new or divergent test logic.

### Process Self-Correction

**2. [Process] Corrected a pre-commit staging error before it reached permanent history**

- **Found during:** Task 2's commit step.
- **Issue:** The first `git commit -m "..."` (without a trailing pathspec) committed the entire index, which included two pre-existing staged `.planning/` files (`milestone.lock`, `08-DISCUSS-CHECKPOINT.json` for phase 8) that the dispatch prompt explicitly said are not this plan's to touch — the identical mistake plan 08-01's SUMMARY documented (Deviation #3) for the same root cause (an unscoped `git commit` picking up someone else's already-staged index state).
- **Fix:** Used `git reset --soft HEAD^` (non-destructive — moves only the branch pointer; index and working tree untouched) to undo the commit, then re-committed with an explicit trailing pathspec (`-- .planning/MERGE-CHECKLIST.md`). Verified via `git status --short` that the pre-existing `.planning/` staged/unstaged state (`ROADMAP.md`, `STATE.md`, `state.json`, `milestone.lock`, both `*-DISCUSS-CHECKPOINT.json` files) was restored byte-for-byte to its pre-commit shape, and via `git show --stat HEAD` that the corrected commit touches only `.planning/MERGE-CHECKLIST.md`.
- **Verification:** `git show --stat HEAD` shows exactly 1 file changed; `git status --short` after the fix matches the dispatch prompt's documented pre-existing state exactly.
- **Committed in:** `813d08e3` (the corrected commit).

---

**Total deviations:** 2 (1 Rule-1 stale-plan-fact documentation, 1 process self-correction). **Impact:** The Rule-1 finding changes no code and is recorded so a future reviewer does not mistake `ProcessingTest.java`'s current diff for an unexplained regression — it is a fully-traced, already-sanctioned consequence of Phase 8's own earlier commit. The process self-correction left zero trace in the final commit history.

## Threat Flags

None — this plan reads source and edits one markdown file; no new network endpoint, auth path, file-access pattern, or schema change was introduced.

## Issues Encountered

None beyond the two deviations documented above.

## User Setup Required

None - no external service configuration required.

## Next Phase Readiness

- **Phase 8 is fully on the record.** All three of its verification-only requirements (ORCH-01, ORCH-04, FREE-01) are discharged by measured evidence in this SUMMARY; all eight Phase-8-owned `MERGE-CHECKLIST.md` rows are `done`; the two Phase-10-owned rows Phase 8 touched early are annotated and correctly left `pending`.
- **For Phase 9:** no keep-or-drop disposition for the metadata-free JSON insert path was stated anywhere in this plan's checklist edits — that decision remains Phase 9's blocking-human checkpoint (JSONP-01), exactly as required.
- **For Phase 10 (HARN-02):** the per-topic MBean names Phase 8 produced (`getMBeanName() + ",topic=" + topic`, now carrying `connector=<name>`) are the exact names the `@AfterEach` MBean leak sweep will have to match once restored — carried forward from plan 08-01's SUMMARY, now also on record in `MERGE-CHECKLIST.md`'s Recorded Fact 11.
- **For Phase 11:** inherits Recorded Fact 13's `TableTest.extractNullables()` finding with its ownership gap stated plainly — no phase (9, 10, or 11) currently owns the fix, yet Phase 11 gates publication on a fully green suite. Phase 11 must either inherit a completed fix or make an explicit, recorded accepted-failure decision; it is not this plan's place to invent that decision.
- **Milestone-level note:** `ProcessingTest.java`'s `git diff v1.6.0` is no longer 0 lines (see Deviations #1) — any future phase's own planning-time "Verified facts" table that re-cites this file's byte-identity should re-derive it rather than copy this phase's now-superseded claim.
- No blockers for the remainder of the milestone.

## Self-Check: PASSED

- `.planning/MERGE-CHECKLIST.md` confirmed present on disk with all eight target rows reading `| done |`, both annotated rows reading `| pending |` with a `Phase 8` note, and items `11.`/`12.`/`13.` each present exactly once, `10.` un-renumbered, and the closing JSON-path paragraph still last.
- Commit `813d08e3` confirmed in `git log --oneline`, touching exactly `.planning/MERGE-CHECKLIST.md` (1 file, 35 insertions, 10 deletions) — within the plan's expected 8-12-line-removed band.
- `git status --porcelain -- src/ build.gradle.kts` re-confirmed empty — this plan changed no source.
- `git status --short` re-confirmed to show only the pre-existing, explicitly-untouched `.planning/` files (`ROADMAP.md`, `STATE.md`, `state.json`, `milestone.lock`, the two `*-DISCUSS-CHECKPOINT.json` entries) that predate this plan's dispatch — none altered by this plan's commit.
- All plan-level `<verification>` items re-run: grep count 16 / 1 constructor confirmed; eight zero-diff files (excluding the documented `ProcessingTest.java` exception) confirmed; checklist row/annotation/fact structure confirmed; no `<<<<<<<` marker found in any commit touching `.planning/MERGE-CHECKLIST.md`.

---
*Phase: 08-orchestration-jmx-re-plumbing*
*Completed: 2026-09-19*
