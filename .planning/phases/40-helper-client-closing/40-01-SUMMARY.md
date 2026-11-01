---
phase: 40-helper-client-closing
plan: "01"
subsystem: testing
tags: [helper-client-lifecycle, counting-seam, red-first, evidence, junit5, clickhouse-kafka-connect]

requires:
  - phase: 39-dependabot-absorption-behaviour-pins
    provides: the evidence helper (byte copy), the 1687 / 2285 test baselines and the Docker-free filtered-run recipe
provides:
  - "Recorded red runs (E-01..E-04) of every case of the helper-client closing matrix at the three sites, before any fix exists"
  - "40-red-writer.patch: the writer half's red tree (CountingHelperClient extension, createHelperClient factory seam, the 22-case writer test), re-appliable on HEAD"
  - "40-red-keeper.patch: the Keeper half's red tree (StateProvider default close, KeeperStateProvider factory constructor, Keeper and ProxySinkTask.stop tests), re-appliable on HEAD"
  - "40-evidence-check.py and 40-COMMAND-EVIDENCE.md with E-01..E-04 and the E-05..E-11 index rows pending"
affects: [40-02, 40-03]

actuals:
  tokens: 19800
  tasks: 3
  commits: 3

tech-stack:
  added: []
  patterns:
    - "Mockito-free counting double: a real ClickHouseHelperClient subclass built from a real builder with scripted ping, version, table mapping, queryV1/queryV2 and a counted close"
    - "Behaviour-neutral factory seam (protected createHelperClient returning builder.build(); package-private Keeper factory constructor) so a red tree compiles without the fix"
    - "Red tree saved as a SHA-256-recorded patch and src restored to HEAD, so each fix half lands in its own commit"

key-files:
  created:
    - .planning/phases/40-helper-client-closing/40-COMMAND-EVIDENCE.md
    - .planning/phases/40-helper-client-closing/40-evidence-check.py
    - .planning/phases/40-helper-client-closing/40-red-writer.patch
    - .planning/phases/40-helper-client-closing/40-red-keeper.patch
  modified: []

key-decisions:
  - "Strict red-first reading (research OQ-2): all reds of both halves recorded before any fix; no src change is committed in this plan, the red trees live only in the two committed patches"
  - "The injected-client clause of the Keeper close() has no behavioural pin, because the injected-client constructor cannot complete (init() dereferences the null config); the K3 NullPointerException tripwire stands in and plan 40-03 adds the source-identity pin"
  - "The two batch cases of the writer test unregister their writer statistics in finally, so the topic MBean one case leaves registered cannot make the next case throw InstanceAlreadyExistsException"

patterns-established:
  - "Red-reason check: every recorded red is an AssertionFailedError naming 'close count'; anything else is not a red"
  - "Scoped Spotless via the IDE hook with an import-only diff rule for files that are dirty at phase start"

requirements-completed: [CLS-04]

coverage:
  - id: D1
    description: "Counting seam (CountingHelperClient from a real builder, createHelperClient factory) carries a writer case Docker-free; red on V1 and V2 with a close count assertion, the four existing counting users green"
    requirement: CLS-04
    verification:
      - kind: unit
        ref: "WriterHelperClientClosingUnitTest#successfulStartThenStopClosesTheHelperClientOnce (red, E-02)"
        status: pass
    human_judgment: false
  - id: D2
    description: "22-case writer matrix run red: 19 failing cases over ten named methods, every failure a close count assertion, the two controls green; red tree saved as 40-red-writer.patch"
    requirement: CLS-04
    verification:
      - kind: unit
        ref: "WriterHelperClientClosingUnitTest (22 cases, 19 red, E-03)"
        status: pass
    human_judgment: false
  - id: D3
    description: "Keeper (5 cases) and ProxySinkTask.stop (2 cases) run red: 6 failing cases, every failure a close count assertion, tripwire green; red tree saved as 40-red-keeper.patch and src restored to HEAD with both patches applying"
    requirement: CLS-04
    verification:
      - kind: unit
        ref: "KeeperStateProviderClosingUnitTest + ProxySinkTaskStopUnitTest (7 cases, 6 red, E-04)"
        status: pass
    human_judgment: false

duration: 8min
completed: 2026-11-01
status: complete
plan_head_before: 52ce1cd4c1501f1c4a332f42c64295be43f0f53c
plan_head_after: 7a366b38be18a258b0c1be95f29d9678e351bc56
commits: 3
---

# Phase 40 Plan 01: Red-first matrix for helper-client closing Summary

**A Mockito-free counting seam and 29 recorded cases (22 writer, 5 Keeper, 2 ProxySinkTask.stop) run red for the right reason on the unchanged tree, with both red trees saved as re-appliable SHA-256-recorded patches and src back at HEAD**

## Performance

- **Duration:** 8 min
- **Started:** 2026-11-01T21:57:30Z
- **Completed:** 2026-11-01T22:05:38Z
- **Tasks:** 3
- **Files modified:** 4 committed (evidence, helper copy, two patches); the src seams and the four new or extended test files exist only inside the patches

## Accomplishments

- E-01 pins the start state: HEAD `52ce1cd4`, origin `custom_build` at the expected `72e8ccff`, upstream PRs #861, #855 and #857 still open, Phase 39 baselines 1687 and 2285 with inputs unchanged, anchors mirror 172/0, helper byte-equal, and the five Spotless baselines matching the research (three import-only dirty, two clean).
- E-02 proves the seam end to end: `CountingHelperClient(builder)` plus the writer's `createHelperClient` factory run the first writer case with Docker unreachable. It fails on client V1 and V2 with `close count`, and the four existing counting users stay green (254 tests, 2 failed).
- E-03: the 22-case writer matrix fails in exactly the ten named methods (19 cases) with a `close count` assertion each; `stopWithoutStartCreatesNoHelperClient` and `clientV2BatchBuildsNoPerBatchHelperClient` pass. The writer patch (sha256 `e1aa8499...1419`) names exactly three paths.
- E-04: the Keeper and stop cases fail in exactly the four named methods (6 cases) with a `close count` assertion; the injected-client tripwire passes. The Keeper patch (sha256 `bdc2533c...a9c`) names four paths, the regenerated writer patch is unchanged, src equals HEAD, and both patches pass `git apply --check`.

## Task Commits

1. **Task 1 (tracer): execution-start facts, counting seam, writer factory, tracer red run** - `708f9cbe` (docs)
2. **Task 2: writer matrix red and the writer red patch** - `53078354` (docs)
3. **Task 3: Keeper and stop cases red, the Keeper red patch, src restored** - `7a366b38` (docs)

**Plan metadata:** committed with this summary (docs: complete plan)

## Files Created/Modified

- `.planning/phases/40-helper-client-closing/40-COMMAND-EVIDENCE.md` - header, index of 11 rows, entries E-01..E-04
- `.planning/phases/40-helper-client-closing/40-evidence-check.py` - byte copy of the Phase 39 helper, never edited
- `.planning/phases/40-helper-client-closing/40-red-writer.patch` - `ClickHouseWriter.java` factory seam, extended `CountingHelperClient`, `WriterHelperClientClosingUnitTest`
- `.planning/phases/40-helper-client-closing/40-red-keeper.patch` - `StateProvider` default `close()`, `KeeperStateProvider` factory constructor, `KeeperStateProviderClosingUnitTest`, `ProxySinkTaskStopUnitTest`

## Decisions Made

- Followed the plan's strict red-first reading. Nothing in `src` is committed: the seams and tests live in the two patches, which plans 40-02 and 40-03 re-apply so each half lands in its own commit.
- K3 is a tripwire, not a pin: `new KeeperStateProvider(helper)` throws `NullPointerException` today, so "an injected client stays open" is satisfied by construction. Plan 40-03 pins it by source identity.
- Test hygiene decision (see deviation 1): batch cases unregister their writer statistics in `finally`.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 1 - Bug] Test-isolation defect in the new writer test class (MBean leak between batch cases)**
- **Found during:** Task 2 (first writer red run, attempt 1, not recorded)
- **Issue:** `clientV1Batches...` leaves the topic MBean `com.clickhouse:type=ClickHouseKafkaConnector,connector=null,...,topic=writer_closing_topic` registered through its writer statistics. The control `clientV2BatchBuildsNoPerBatchHelperClient` then threw `InstanceAlreadyExistsException` (a `RuntimeException`), so 20 cases failed and the control was red for a non-`close count` reason.
- **Fix:** `CountingWriter` keeps its `SinkTaskStatistics` and exposes `unregisterStatistics()`, called in the `finally` of both batch cases (the plan's own T-40-04 requires leaked MBeans to be unregistered in `finally`). A fresh run on the corrected class is the recorded E-03; E-03 states the attempt-1 outcome and the reason. No production line and no closing assertion changed.
- **Files modified:** `WriterHelperClientClosingUnitTest.java` (inside `40-red-writer.patch`)
- **Verification:** E-03 run: 22 tests, 19 failures, exact failing set, no wrong-reason failure.
- **Committed in:** `53078354`

---

**Total deviations:** 1 auto-fixed (1 bug, in new test code only)
**Impact on plan:** None on scope. The attempt-1 outcome was a defect of the new test class, not a red of the unchanged tree, and the recorded run is clean.

## Issues Encountered

- The plan's literal wording for the red-reason check says a different reason is "record and STOP". Attempt 1 failed for a reason that was the new class's own isolation bug, so it was corrected and re-run (disclosed in E-03 and above) rather than recorded as a red.
- Tasks 1 and 2's `<automated>` verify blocks cannot be re-run after the plan ends: they require the seams on disk, and Task 3 restores `src` to HEAD by design. Each ran green at the end of its own task; the plan-level gate is Task 3's verify, which printed `P40-01-T3-OK` equivalent (`P40-01-T3-PRE-OK` before the commit, plus the final commit check below).

## Known Stubs

None. No stub, placeholder or empty data flow was added (the plan commits only evidence and patch files).

## Threat Flags

None. No new network endpoint, auth path, file access or schema surface in committed code; the stub server in the tests is loopback-only and exists only inside the patch.

## User Setup Required

None - no external service configuration required.

## Next Phase Readiness

- Plan 40-02 re-applies `40-red-writer.patch`, writes the writer fix and lands the writer half behind its own gate; plan 40-03 does the same with `40-red-keeper.patch`.
- Origin `custom_build` was re-read at each task and still equals `72e8ccff`; nothing was pushed. The three upstream PRs (#861 touches the same `getMutationRequest` lines) were open at E-01.

## Self-Check: PASSED

- Created files exist: `40-COMMAND-EVIDENCE.md`, `40-evidence-check.py`, `40-red-writer.patch`, `40-red-keeper.patch` (FOUND).
- Task commits `708f9cbe`, `53078354`, `7a366b38` are ancestors of HEAD (FOUND); `check evaluation-scope --plan 40-01 --commits-only` resolved 3 commits.
- Task 3 verify block printed its OK line; `git diff --quiet HEAD -- src` exits 0; both patches pass `git apply --check`; anchors mirror `rows=172 bad=0`; evidence `secrets` check clean.

---
*Phase: 40-helper-client-closing*
*Completed: 2026-11-01*
