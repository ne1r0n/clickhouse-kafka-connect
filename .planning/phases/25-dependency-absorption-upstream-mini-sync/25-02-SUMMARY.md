---
phase: 25-dependency-absorption-upstream-mini-sync
plan: "02"
subsystem: ci-pin-guard
tags: [debt-13, pin-guard, kts-scanner, red-first, 21-review-wr-02]

requires:
  - phase: 25-dependency-absorption-upstream-mini-sync
    provides: 25-COMMAND-EVIDENCE.md with the E-03/E-04 index rows (plan 25-01)
provides:
  - "TestsWorkflowPinGuardUnitTest.ktsCode: a one-pass, line-preserving scanner that recognises string/char literals with escapes, raw strings, line and trailing comments, and nested block comments"
  - "Three DEBT-13 tests (29 in the class), seen red against the old regex reader in E-03 and green in E-04"
affects: [25-04, 25-06, 25-08]

actuals:
  tokens: 3900
  tasks: 2
  commits: 1
plan_head_before: 3c59a1daadc9e78e07a7ef51c7ff7cbd5582eead
plan_head_after: 92b2be1eb955fa57bd93b1120c48adbad447e019
commits: 1

tech-stack:
  added: []
  patterns:
    - "Red-first in one commit (Phase 21 D-05..D-07): fixtures seen red through the real testUnit path, recorded in the evidence file, then committed together with the fix"
    - "Per-file spotless proof in a throwaway HEAD worktree, baseline-equal (46 baseline files plus the class)"

key-files:
  created: []
  modified:
    - src/test/java/com/clickhouse/kafka/connect/ci/TestsWorkflowPinGuardUnitTest.java
    - .planning/phases/25-dependency-absorption-upstream-mini-sync/25-COMMAND-EVIDENCE.md

key-decisions:
  - "The empty-text edge (ktsCode returns an empty list; forkTaskProblem reports the 'not found outside comments' message) is asserted inside the ktsCode test, not the accept fixture test, so each of the three named tests keeps a single theme"
  - "Scanner is a plain character state machine inside the test class; no Kotlin parser, per Phase 17 D-06 and Phase 21 D-06. A quote inside a string template's braces is documented as the one unsupported case"

requirements-completed: [DEBT-13]

coverage:
  - id: D1
    description: "ktsCode keeps string literals (glob with slash-star, escaped quote), raw strings and line structure, strips line, trailing and nested block comments, returns an empty list for empty text, and a quote in a char literal does not open a string"
    requirement: DEBT-13
    verification:
      - kind: test
        ref: "TestsWorkflowPinGuardUnitTest#ktsCodeKeepsStringsAndLineStructure"
        status: pass
    human_judgment: false
  - id: D2
    description: "reportTestForks task rule accepts the task after a slash-star in a string, raw string, line comment or trailing comment; rejects a maxParallelForks read inside a trailing comment and a task inside a nested block comment"
    requirement: DEBT-13
    verification:
      - kind: test
        ref: "TestsWorkflowPinGuardUnitTest#forkTaskRuleAcceptsTaskAfterSlashStarInStringOrComment, #forkTaskRuleRejectsTrailingCommentReadAndNestedCommentedTask"
        status: pass
    human_judgment: false
  - id: D3
    description: "The three new tests were red against the old reader (exactly 3 of 29) before the scanner, and the 26 existing tests were kept unchanged"
    requirement: DEBT-13
    verification:
      - kind: other
        ref: "plan 25-02 Task 1 verify DEBT13-RED-RECORDED (E-03); Task 2 verify DEBT13-GREEN and DEBT13-COMMITTED (E-04)"
        status: pass
    human_judgment: false

duration: 12min
completed: 2026-10-21
status: complete
---

# Phase 25 Plan 02: DEBT-13 string- and comment-aware ktsCode Summary

**Replaced the pin guard's regex `ktsCode` with a one-pass, line-preserving scanner (strings, char literals, raw strings, line/trailing comments, nested block comments), with three DEBT-13 fixture tests seen red (3 of 29) before the fix and green after (29/29; full `testUnit` 841 tests, 0 failed).**

## Performance

- **Duration:** 12 min
- **Completed:** 2026-10-21T15:17:00Z
- **Tasks:** 2 of 2
- **Files modified:** 2

## Accomplishments

- E-03 (tracer): the three new tests failed against the old reader and only those three (`Tests summary: 29 tests, 26 succeeded, 3 failed`). The old reader let a slash-star in a string, line comment, trailing comment or raw string swallow the `reportTestForks` task, kept a trailing comment, and closed a nested comment at the first closer.
- E-04: scanner green, class `29 tests, 29 succeeded, 0 failed, 0 skipped`, full `testUnit` `841 tests, 839 succeeded, 0 failed, 2 skipped` with `Test result: SUCCESS`; spotlessApply in a HEAD worktree left the class byte-identical.
- The fix lands before the milestone's first authored `build.gradle.kts` commit (plan 25-04's Shadow repair); the last `build.gradle.kts` commit is still `6473e24f`.

## Task Commits

1. **Task 1: DEBT-13 fixtures seen red (E-03)** - no commit, by design (Phase 21 D-07)
2. **Task 2: scanner, 29/29 green, full testUnit, per-file spotless (E-04)** - `92b2be1e` (test) carries tests, fix and E-03/E-04 together

**Plan metadata:** the `docs(25-02)` completion commit with this SUMMARY, STATE.md, ROADMAP.md and REQUIREMENTS.md.

## Files Created/Modified

- `src/test/java/com/clickhouse/kafka/connect/ci/TestsWorkflowPinGuardUnitTest.java` - scanner-based `ktsCode`, `KtsState` enum, `RAW_QUOTE` constant, three DEBT-13 tests, `assertEquals` import
- `.planning/phases/25-dependency-absorption-upstream-mini-sync/25-COMMAND-EVIDENCE.md` - E-03, E-04, index rows flipped to pass

## Decisions Made

See `key-decisions` above.

## Deviations from Plan

None - plan executed exactly as written. The red set matched the prediction on the first run, so no fixture was changed.

## Issues Encountered

None. The OpenJ9 Mockito crash did not occur; no `core.*` dump was present.

## Known Stubs

None.

## Threat Flags

None. Test-only change; no new endpoint, auth path or file access.

## Coverage

| Requirement | Evidence |
|-------------|----------|
| DEBT-13 (21-REVIEW WR-02, WR-03 kts half) | E-03 (red, 3 of 29), E-04 (green, 29/29, 841 full) |

## Next Plan Readiness

Plan 25-03 (DEPS-02, DEPS-03) can proceed. Plans 25-04 and 25-06 may now add slash-star strings to `build.gradle.kts`; the pin guard reads them correctly.

## Self-Check: PASSED

- FOUND: src/test/java/com/clickhouse/kafka/connect/ci/TestsWorkflowPinGuardUnitTest.java
- FOUND: .planning/phases/25-dependency-absorption-upstream-mini-sync/25-COMMAND-EVIDENCE.md (E-03, E-04)
- FOUND: commit 92b2be1e
