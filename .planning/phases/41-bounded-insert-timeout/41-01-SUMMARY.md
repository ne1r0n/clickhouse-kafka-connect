---
phase: 41-bounded-insert-timeout
plan: "01"
subsystem: testing
tags: [insert-timeout, stall-pin, red-first, evidence, assertTimeoutPreemptively, client-v2, junit5, clickhouse-kafka-connect]

requires:
  - phase: 40-helper-client-closing
    provides: the Docker-free filtered-run recipe, the CountingHelperClient seam, the 1719 / 2317 totals that E-01 re-measures
provides:
  - "Measured Phase 41 start baseline on the unchanged tree: testUnit 1719 tests / 2 skips and full suite 2317 tests / 18 named skips on ClickHouse latest x client V2, both green (E-01)"
  - "Recorded red runs of every stall pin on a tree with no fix: 9 of 15 cases red for the right reason (preemptive timeout), the 6 operator socket_timeout guards green on the unfixed tree (E-02, E-03)"
  - "41-red-pins.patch: the red tree (StubClickHouseServer stall responder and InsertStallBoundPinUnitTest, 15 cases), re-appliable on HEAD, SHA-256 recorded"
  - "41-evidence-check.py (byte copy of the Phase 39 helper) and 41-COMMAND-EVIDENCE.md with E-01..E-03 and the E-04..E-10 index rows pending"
affects: [41-02, 41-03]

actuals:
  tokens: 13590
  tasks: 2
  commits: 2

plan_head_before: 69d5da3aa0d2f443294b26b954bc78c84643fe17
plan_head_after: 784ea1912d63c9fba2774ee5b2ae64d6102e0b71
commits: 2

tech-stack:
  added: []
  patterns:
    - "Stall stub: a latch-held responder on a daemon handler pool whose idempotent close() releases the held request before HttpServer.stop(0)"
    - "Preemptive bound around a real client V2 insert (assertTimeoutPreemptively 10 s) so an unfixed hang fails the case instead of hanging the suite"
    - "Red tree saved as a SHA-256-recorded patch and src restored to HEAD, so the pins and the fix land in separate commits"

key-files:
  created:
    - .planning/phases/41-bounded-insert-timeout/41-COMMAND-EVIDENCE.md
    - .planning/phases/41-bounded-insert-timeout/41-evidence-check.py
    - .planning/phases/41-bounded-insert-timeout/41-red-pins.patch
  modified: []

key-decisions:
  - "No src change is committed in this plan: the stub and the pin class exist only inside the committed 41-red-pins.patch, src ends equal to HEAD, and no src/main file was touched"
  - "The six operator socket_timeout cases are guards against a naive fix, not red pins: they pass on the unfixed tree because createClientV2 already forwards the operator's socket_timeout as a client-level option"
  - "The writer-level, ProxySinkTask.put and operator-guard cases live in one class (InsertStallBoundPinUnitTest, package sink) so their fixtures are written once; the stub's stalling() and okAfter() factories use a daemon pool so every request is recorded while one is held"

patterns-established:
  - "Red-reason check: every failing testcase must be an AssertionFailedError containing 'execution timed out after' and at most 30 s long; a compile error or any other failure is not a recorded red"
  - "Unique topic per stall case plus the finally order stub.close(), writer.stop(), closeLeftoverHelpers(), unregisterStatistics()"

requirements-completed: [PIN-01]

coverage:
  - id: D1
    description: "The Phase 41 start baseline is measured on the unchanged tree: a cache-busted testUnit with Docker unreachable (1719 tests, 2 skipped) and a full suite on ClickHouse latest x client V2 (2317 tests, the 18 named skips), both green"
    requirement: "PIN-01"
    verification:
      - kind: other
        ref: "python3 -I .planning/phases/41-bounded-insert-timeout/41-evidence-check.py expect .planning/phases/41-bounded-insert-timeout/41-COMMAND-EVIDENCE.md E-01 (25 checks, 0 failed) and junit/skips on /tmp/gsd41/exec/xml-baseline-testunit and xml-baseline-full"
        status: pass
    human_judgment: false
  - id: D2
    description: "StubClickHouseServer has a stall responder (stalling(), okAfter(Duration), public API, idempotent close that releases a held request first) and the writer-level pin, a real ClickHouseWriter insert on client V2 with async=false inside assertTimeoutPreemptively, fails on the JSON, String and RowBinary routes instead of hanging; the four existing stub users still pass"
    requirement: "PIN-01"
    verification:
      - kind: unit
        ref: "src/test/java/com/clickhouse/kafka/connect/sink/InsertStallBoundPinUnitTest.java#stalledV2InsertEndsWithinTheBoundWithARetriableRootCause (red by design on the unfixed tree: 3 tests, 3 failures, E-02)"
        status: pass
    human_judgment: false
  - id: D3
    description: "The 15-case stall pin matrix is red for the right reason on the unfixed tree (writer level 3 cases, ProxySinkTask.put under errors.tolerance none and all 6 cases) and the 6 operator socket_timeout guards are green there"
    requirement: "PIN-01"
    verification:
      - kind: other
        ref: "bash /tmp/gsd41/exec/verify-t2.sh prints P41-01-T2-OK (E-03: 15 tests, 9 failures, failing set exactly the writer-level and put-level pins, operator cases passed 6)"
        status: pass
    human_judgment: false
  - id: D4
    description: "The red tree is saved as 41-red-pins.patch naming exactly the stub and the pin class, its SHA-256 equals the recorded one, git apply --check passes on HEAD and src equals HEAD"
    requirement: "PIN-01"
    verification:
      - kind: other
        ref: "git apply --check .planning/phases/41-bounded-insert-timeout/41-red-pins.patch; shasum -a 256 equals E-03 patch_sha256 10c738b2...e9ffc; git diff --quiet HEAD -- src"
        status: pass
    human_judgment: false

duration: 21min
completed: 2026-11-02
status: complete
---

# Phase 41 Plan 01: Bounded insert timeout, red-first pins Summary

**Measured baseline (1719 testUnit, 2317 full suite on latest x V2, both green), a latch-held StubClickHouseServer stall responder, and a 15-case InsertStallBoundPinUnitTest run red on the unfixed tree: 9 cases fail by the 10 s assertTimeoutPreemptively bound instead of hanging, the 6 operator socket_timeout guards pass, and the red tree is saved as a SHA-256-recorded patch with src back at HEAD**

## Performance

- **Duration:** about 21 min (the start time was not captured at spawn; the first command ran at roughly 08:40Z)
- **Started:** about 2026-11-02T08:40:00Z (approximate)
- **Completed:** 2026-11-02T09:01:00Z
- **Tasks:** 2
- **Files modified:** 3 committed (evidence, helper copy, patch); the two test files were created and removed again by design

## Accomplishments

- E-01 measured the phase baseline on the unchanged tree instead of trusting the Phase 40 report: `testUnit` 1719 tests / 0 failures / 0 errors / 2 skipped with Docker unreachable, and the full suite on ClickHouse `latest` (26.10.3.7) x client V2 2317 tests / 0 failures / 0 errors / the 18 named skips. Both equal the recorded totals, so the phase gate totals are 1719 + 26 and 2317 + 26.
- E-02 (tracer): `StubClickHouseServer` became `public final` with public `Request`, `Responder`, `port()`, `requests()`, `status`, `serverException`, `ok`, plus `stalling()` and `okAfter(Duration)` factories on a daemon handler pool and an idempotent `close()` that counts the release latch down before `server.stop(0)`. The writer-level pin (a real `ClickHouseWriter` on client V2, `async=false`, wrapped in `assertTimeoutPreemptively(10 s)`) failed on all three routes with `execution timed out after` at 10.0 to 10.6 s; the four existing stub users (2, 7, 5 and 23 tests) stayed green.
- E-03: `InsertStallBoundPinUnitTest` completed to 15 cases (writer-level 3, `ProxySinkTask.put` under `errors.tolerance` none and all 6, operator `socket_timeout` guards 6). On the unfixed tree exactly the 9 stall cases failed, every one an `AssertionFailedError` containing `execution timed out after` and at most 10.691 s; the 6 operator cases passed. The red tree is `41-red-pins.patch` (two paths, SHA-256 `10c738b2beac4abd91c4a7b126e947052d0b7b1d62e7f807d243b84de36e9ffc`), `git apply --check` passes on HEAD, and `git diff --quiet HEAD -- src` exits 0.

## Task Commits

Each task was committed atomically:

1. **Task 1 (tracer): execution-start facts, measured baseline and the writer-level stall pin run red (E-01, E-02)** - `cfab48ee` (docs)
2. **Task 2: the 15-case pin class run red, the red tree saved as a patch, src restored (E-03)** - `784ea191` (docs)

**Plan metadata:** the SUMMARY commit that follows this file (docs: complete plan)

## Files Created/Modified

- `.planning/phases/41-bounded-insert-timeout/41-COMMAND-EVIDENCE.md` - Phase 41 evidence: header, index of E-01..E-10, entries E-01..E-03
- `.planning/phases/41-bounded-insert-timeout/41-evidence-check.py` - byte copy of the Phase 39 read-only gate helper, never edited
- `.planning/phases/41-bounded-insert-timeout/41-red-pins.patch` - the red tree: the stub stall responder and the 15-case pin class, re-applied by plan 41-02

## Decisions Made

- No src change is committed: the stub and the pin class live only in the committed patch, and `src/main` was never touched.
- The operator `socket_timeout` cases are guards against a naive fix (per-insert settings override the client-level option), not red pins; plan 41-02 proves they discriminate by a recorded mutation.
- One class holds the writer-level, `ProxySinkTask.put` and operator-guard cases so the fixtures are written once (the research sketched two classes).

## Deviations from Plan

None - plan executed exactly as written.

The only unplanned event was a test-compile error in the new class during Task 2's first red run (the `ProxySinkTask.put` case lacked `throws Exception` because `StubClickHouseServer.stalling()` throws `IOException`). It was a defect in the new test itself, fixed before any red was recorded, and E-03 notes it; it is not a rule deviation.

## Issues Encountered

- The first Task 2 red run stopped at `compileTestJava` (see above). Fixed by declaring `throws Exception` on that one test method and re-running; the recorded red is the first run that compiled.
- Per the plan's commit rule ("never stage `.planning/STATE.md`"), STATE.md is not staged in the plan's commits.

## Known Stubs

None. No source file created or modified by this plan carries a stub; the stall stub is a deliberate test fixture that lives only in the committed patch.

## Threat Flags

None. The only new network surface is the loopback-only test stub, which exists only inside `41-red-pins.patch`; no `src/main` file changed.

## User Setup Required

None - no external service configuration required.

## Next Phase Readiness

- Plan 41-02 can `git apply` `41-red-pins.patch` on HEAD (checked), re-run the red, commit the pins apart from the fix, and land the per-insert `socket_timeout` fix in `ClickHouseHelperClient` and `ClickHouseWriter` in its own commit.
- Baseline for the phase gate: `testUnit` 1719 and full suite 2317 plus 26 new cases; anchors mirror `rows=180 bad=0`; `origin/custom_build` still `72e8ccffe05ff6dffb14098d0b8c0f67aaa6a236`.

## Self-Check: PASSED

- `41-COMMAND-EVIDENCE.md`, `41-evidence-check.py`, `41-red-pins.patch` exist; the helper is `cmp`-equal to the Phase 39 copy.
- Commits `cfab48ee` and `784ea191` are ancestors of HEAD; `git rev-list --count 69d5da3a..HEAD` read 2 before this SUMMARY.
- Both task verifies print `P41-01-T1-OK` and `P41-01-T2-OK`; `git diff --quiet HEAD -- src` exits 0; `git ls-remote origin refs/heads/custom_build` equals `72e8ccff...`.

---
*Phase: 41-bounded-insert-timeout*
*Completed: 2026-11-02*
