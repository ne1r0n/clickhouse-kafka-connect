---
phase: 34-upstream-tail-merge
plan: "03"
subsystem: upstream-sync
tags: [additional-retriable-error-codes, handle-exception, config-def, phase-gate, fork-anchors, mutation-run, junit]

requires:
  - phase: 34-upstream-tail-merge
    provides: "plan 34-02's repaired tip (ProxySinkTask three constructors, five-argument handleException caller, re-blessed BLD-16 list, 141 anchor rows)"
provides:
  - "Docker-free AdditionalRetriableErrorCodesPinUnitTest (19 test executions) pinning additionalRetriableErrorCodes from the config string through the ConfigDef, the constructor and the ProxySinkTask seam into handleException, proven to bite by a mutation run"
  - "34-COMMAND-EVIDENCE.md entries E-09..E-11, index E-01..E-11 all pass"
  - "The Phase 34 gate: cache-busted testUnit 1441, two --rerun full suites 2028 tests on latest x V2 with the 18 named skips, anchors mirror rows=141 bad=0, tail gate and cumulative gate PASS, nothing pushed"
affects: [35-buffered-fix, 36-client-bump, 37, 38-release]

actuals:
  tokens: 9205
  tasks: 3
  commits: 4

tech-stack:
  added: []
  patterns:
    - "a literal oracle copied from the pre-merge source, never built from the production constant, so a sync that edits the constant fails the pin"
    - "pin both the ConfigDef path and the constructor path of a setting; they throw different types by design"
    - "run the cumulative gate in the background and the suites alongside it; the gate reads commit trees through git only"

key-files:
  created:
    - src/test/java/com/clickhouse/kafka/connect/sink/AdditionalRetriableErrorCodesPinUnitTest.java
  modified:
    - .planning/phases/34-upstream-tail-merge/34-COMMAND-EVIDENCE.md

key-decisions:
  - "The DataException clause of TAL-04 is pinned as: adding codes never makes a DataException that carries no ClickHouse server error and no network root cause retriable. The wrapped cases are retriable today and are not asserted (see the interpretation flag)."
  - "The pin class holds exactly one three-argument ServerException construction (its V2 factory), so the Phase 36 recount is 10 sites in 3 files."

patterns-established:
  - "Mutation run for a default-set pin: drop one code from the production set in a detached worktree and require exactly the default-set test and the unset-key sweep to fail"

requirements-completed: [TAL-04, TAL-05, TAL-01]

coverage:
  - id: D1
    description: "additionalRetriableErrorCodes parsing is pinned on the ConfigDef (validate and parse) and the constructor: whitespace, duplicates and empty items accepted and normalised; non-integers, values <= 0 and out-of-range numbers rejected naming the key (ConfigException on the ConfigDef path, IllegalArgumentException on the constructor path)"
    requirement: "TAL-04"
    verification:
      - kind: unit
        ref: "src/test/java/com/clickhouse/kafka/connect/sink/AdditionalRetriableErrorCodesPinUnitTest.java (19 executions, 0 failures); E-09 verdict pass"
        status: pass
    human_judgment: false
  - id: D2
    description: "The default retriable set equals a literal copy of the pre-merge switch's 15 codes including 159, is unmodifiable, and with the key unset every server code 0..1100 on client V2 and V1 classifies exactly like the pre-merge switch; dropping 159 from the production set fails exactly the two default-set tests"
    requirement: "TAL-04"
    verification:
      - kind: unit
        ref: "AdditionalRetriableErrorCodesPinUnitTest default-set test and unset-key sweep; mutation run mutation_prediction_met: yes (E-09)"
        status: pass
    human_judgment: false
  - id: D3
    description: "A DataException that carries no ClickHouse server error and no network root cause is never retriable whichever codes are added, and a writer DataException through ProxySinkTask with codes configured fails the task non-retriably (tolerance none) or is tolerated (all)"
    requirement: "TAL-04"
    verification:
      - kind: unit
        ref: "AdditionalRetriableErrorCodesPinUnitTest plain-DataException, non-ClickHouse-cause and writer tests; E-09 verdict pass"
        status: pass
    human_judgment: true
    rationale: "TAL-04's wording ('never reclassified as retriable') was interpreted narrowly; whether the wrapped retriable cases should stay retriable is a user decision on upstream behavior, not something a test can settle"
  - id: D4
    description: "The Phase 34 gate holds on the tip dced2c0f: origin custom_build still 3a3a15a9 with no tail-2 tag or sync branch on origin, cache-busted testUnit 1441/0/0, two --rerun full suites 2028/0/0 with the 18 named skips on one ClickHouse image, Docker-only count 587 equal to the baseline, anchors mirror rows=141 bad=0, tail gate (anchor_exempt=0) and cumulative gate PASS"
    requirement: "TAL-05"
    verification:
      - kind: other
        ref: "verify P34-03-T3-OK, P34-03-T3-ORIGIN-GATE-OK, P34-03-T3-TESTUNIT-OK; E-11 verdict pass"
        status: pass
    human_judgment: false
  - id: D5
    description: "Handoffs for Phases 36 and 38 are recorded on the code-complete tip (10 three-argument ServerException sites, the FUT-05 rollout item, the null-codes edge, untouched release and high-risk files)"
    requirement: "TAL-01"
    verification:
      - kind: other
        ref: "verify P34-03-T2-OK; E-10 verdict pass"
        status: pass
    human_judgment: false

duration: 55min
completed: 2026-10-29
status: complete
plan_head_before: 8e7e44816a4803caafaab5b0b2d7d098554a6133
plan_head_after: dced2c0fb9c2289d6f58975dfa505cc97279020f
commits: 4
---

# Phase 34 Plan 03: Pin additionalRetriableErrorCodes and gate the phase Summary

**A Docker-free 19-execution pin class drives `additionalRetriableErrorCodes` from the config string through the ConfigDef, the constructor and the ProxySinkTask seam into `handleException`, with a literal 15-code oracle and a 0..1100 unset-key sweep proven to bite by a mutation run, and the Phase 34 gate is green: two full suites on latest x V2, anchors rows=141, tail and cumulative gates PASS, nothing pushed.**

## Performance

- **Duration:** about 55 min of active work (see Issues Encountered for the clock gap)
- **Started:** 2026-10-28T22:18Z (green-run clock marker; writing the class before it was not clocked)
- **Completed:** 2026-10-29T07:41Z
- **Tasks:** 3
- **Files modified:** 2 (one created, one modified)

## Accomplishments

- Tracer (E-09): `AdditionalRetriableErrorCodesPinUnitTest` (package `com.clickhouse.kafka.connect.sink`, extends nothing, ten named tests, 19 executions) passed 19/0/0/0 on its first run beside upstream's `AdditionalRetriableErrorCodesTest` (3), `ClickHouseSinkConfigTest` (8) and `UtilsTest` (34). The per-file formatting check against a detached worktree was identical. The mutation run (drop `159, // TIMEOUT_EXCEEDED` from `Utils.DEFAULT_RETRIABLE_ERROR_CODES`, worktree only) failed exactly the default-set test and the unset-key sweep.
- Handoffs (E-10): 10 `new ServerException(` sites in 3 files recorded with their lines; client-v2 0.11.0 declares only the four-argument constructor; the V1 `ClickHouseException` constructors are identical between 0.9.6 and 0.11.0; the FUT-05 rollout item, the unedited `# Unreleased` CHANGELOG block, untouched `VERSION` and `docs/FORK-DELTA.md`, the null-codes edge (`Utils.java:99`), `src/main` changed in exactly four files since the tag, and the four high-risk files untouched.
- Phase gate (E-11): cache-busted testUnit 1441 tests (1418 + 4 + 19, as expected); full suite run 1 and run 2 each 2028 tests, 0 failures, 0 errors, 18 skips (`OK skips: named=7 state_mismatch_cases=11 unexpected=0`), ClickHouse `26.10.3.7` with one digest across both runs and the same digest as E-01; Docker-only count 587 equal to the baseline; anchors mirror `rows=141 bad=0`; tail gate `provenance_paths=27 ... anchor_rows=141 ... anchor_exempt=0 result=PASS`; cumulative gate `anchor_rows=141 anchor_exempt=19 result=PASS`; gate scripts identical to Phase 33's E-14 bytes; origin `custom_build` still `3a3a15a9`, with `pre-v1.7.0-tail-2` and `sync/v1.7.0` absent from origin.

## Interpretation flag: DataException never reclassified

TAL-04's wording ("a DataException is never reclassified as retriable") was planned and pinned narrowly: **adding codes never makes a DataException that carries no ClickHouse server error and no network root cause retriable.** The pin covers a plain DataException (with each of the 15 default codes alone, 216 alone, the full default set and `{216, 159}` added), a DataException caused by an `IllegalStateException` or by an `IOException` without a read/write-timeout prefix, and a writer DataException through `ProxySinkTask.put` with `additionalRetriableErrorCodes=216,159` (which `Processing` wraps in a `RuntimeException`, so it fails the task non-retriably under `errors.tolerance=none` and is tolerated under `all`). See E-09 and E-10.

Two behaviours that exist today are deliberately **not** asserted, because `handleException` classifies the root cause first:

1. A DataException whose cause chain holds a ClickHouse server error with a retriable code (default or added) **is retriable today**.
2. A DataException whose root cause is a `SocketTimeoutException` or `UnknownHostException` **is retriable today under `errors.tolerance=all`**.

Changing either is a behaviour change to upstream code and your decision, not Phase 34 work. If you want either to be non-retriable, say so and it becomes a separate, scoped change.

## Handoffs

- **Phase 36 (client bump):** `new ServerException(` is now **10 sites in 3 files** (`UtilsTest` 8, `AdditionalRetriableErrorCodesTest` 1, `AdditionalRetriableErrorCodesPinUnitTest` 1), not the ROADMAP's "all eight" (9 from the tail in 2 files per E-04, plus 1 in the new fork pin class). Client-v2 0.11.0 has only the four-argument `ServerException(int, String, int, String)`, so every site needs a fourth argument. The `file:line` list is in E-10; the pin class's single site is its V2 factory and carries a comment pointing at Phase 36. The V1 constructors are unchanged.
- **Phase 38 (release):** the FUT-05 rollout item (an added code raises `RetriableException`, which Connect redelivers with no retry budget and outside `errors.tolerance` and the DLQ, so only transient codes belong in `additionalRetriableErrorCodes`; upstream's ConfigDef description and its `Unreleased` CHANGELOG entry say the same); the upstream `# Unreleased` CHANGELOG block (present, unedited, one heading) for RLS-01 to absorb; and the FORK-DELTA `Utils.java` import-order row and checklist reading-order sentence, which gain `MERGE-CHECKLIST-v1.7.0-tail-2.md`.
- **Null-codes sharp edge (upstream code, unchanged):** `Utils.java:99` calls `additionalRetriableErrorCodes.contains(...)` with no null guard. `ProxySinkTask` passes the config's parsed set (never null), `ChunkFlusher` guards a null config, and every test passes `Set.of()`; only a future caller passing null would hit a `NullPointerException` on the server-error path. Cite E-10.
- **Test-count discrepancy carried from 34-02 (for you):** 34-02 recorded `tail_tests_added: 31` against the plan's and rehearsal's 33. The absolute testUnit total matched the rehearsal (1418), so the tail's test content is equal; the two-test difference is in the baseline (1385 in research, 1387 measured in E-01 on an unchanged `src`) and is still unexplained. This plan's expected count (E-07's 1418 + 4 + 19 = 1441) held exactly, so nothing after the tail is affected.

## Task Commits

1. **Task 1: TAL-04 tracer** - `31e99f81` (test: the pin class), `bba5d9c7` (docs: E-09)
2. **Task 2: handoff record** - `7342319a` (docs: E-10)
3. **Task 3: phase gate** - `dced2c0f` (docs: E-11)

**Plan metadata:** the SUMMARY and STATE/ROADMAP/REQUIREMENTS commit follows this file.

`commits: 4` is the measured `git rev-list --count 8e7e4481..dced2c0f`: one source commit plus three evidence commits, exactly as the plan's Output section states.

## Files Created/Modified

- `src/test/java/com/clickhouse/kafka/connect/sink/AdditionalRetriableErrorCodesPinUnitTest.java` - new, Docker-free TAL-04 pins
- `.planning/phases/34-upstream-tail-merge/34-COMMAND-EVIDENCE.md` - E-09, E-10, E-11 and the index flips

## Decisions Made

- The DataException interpretation above (narrow pin, wrapped cases not asserted), as the plan directed.
- `UtilsTest`, `AdditionalRetriableErrorCodesTest`, `ClickHouseSinkConfigTest`, `Utils.java`, `ClickHouseSinkConfig.java`, `fork-anchors.txt`, the gate scripts, `CHANGELOG.md`, `VERSION` and `docs/FORK-DELTA.md` were not touched; no main code changed, so no null guard was added.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 3 - Blocking] The evidence helper's `--expect` names must be the JUnit DisplayName, not the Java method name**
- **Found during:** Task 1 (mutation run)
- **Issue:** as in 34-02: `failed ... --expect 'AdditionalRetriableErrorCodesPinUnitTest#defaultRetriableSetIs...'` prints FAIL for the correct two failing tests because the XML testcase name is the `@DisplayName` text.
- **Fix:** ran the helper with the two DisplayName texts (`OK failed: 2 failing testcases, 2 expected`) and recorded `mutation_prediction_met: yes` with the equivalence explained in E-09. The helper and the plan were not edited.
- **Files modified:** none
- **Verification:** E-09's `expect` checks pass; the plan's verify blocks read the recorded facts and print `P34-03-T1-OK`.
- **Committed in:** `bba5d9c7`

---

**Total deviations:** 1 (Rule 3 helper naming, as in 34-02)
**Impact on plan:** no code or scope change.

## Issues Encountered

- A first attempt to write E-10 through a nested `bash -c` heredoc hung and was moved to the background by the harness; it had written nothing (the evidence file stayed clean, confirmed by `git status` and an E-10 heading count of 1 before and after its late completion), and E-10 was then written from a script file and committed once.
- The wall clock jumped between Task 2 and Task 3 (about 22:21Z on 2026-10-28 to 07:15Z on 2026-10-29, a gap of about nine hours that was not active work). E-09's start is the green-run clock marker (writing the class before it was not clocked), and the 55 min duration is the sum of active periods, not end minus start.
- The cumulative gate (about 23 min) ran in the background while testUnit and both full suites ran in the foreground; no OpenJ9 crash or core dump appeared.

## Known Stubs

None.

## User Setup Required

None - no external service configuration required. Nothing was pushed.

## Next Phase Readiness

- Phase 34 is complete and gated on tip `dced2c0f`: this is the baseline for Phases 35-37. Phase 35's BUF fix starts from a `ProxySinkTask` and `Utils.handleException` guarded by anchor rows and Docker-free pins.
- Upstream `main` was still `9bed6f79` and its newest release tag `v1.7.0` at the gate, so no further sync is pending.
- Open for you: the two unpinned DataException cases above, and the FUT-05 retry-budget item (Phase 38 only records the rollout warning).

## Self-Check: PASSED

- FOUND: `src/test/java/com/clickhouse/kafka/connect/sink/AdditionalRetriableErrorCodesPinUnitTest.java`, `34-COMMAND-EVIDENCE.md` (E-01..E-11 rows `pass`)
- FOUND commits: `31e99f81`, `bba5d9c7`, `7342319a`, `dced2c0f` (all ancestors of HEAD)
- P34-03-T1-TESTS-OK, P34-03-T1-OK, P34-03-T2-OK, P34-03-T3-OK, P34-03-T3-ORIGIN-GATE-OK and P34-03-T3-TESTUNIT-OK all printed on the committed tip.

---
*Phase: 34-upstream-tail-merge*
*Completed: 2026-10-29*
