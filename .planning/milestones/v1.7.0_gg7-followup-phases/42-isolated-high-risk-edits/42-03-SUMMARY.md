---
phase: 42-isolated-high-risk-edits
plan: "03"
subsystem: testing
tags: [kafka-connect, retriable-error-codes, startup-warning, fork-delta, junit5, log-capture]

requires:
  - phase: 42-isolated-high-risk-edits
    provides: plan 42-02 gate E-07 (testUnit 1801, full 2399, the status dumps), the evidence helpers and the record placement rule
provides:
  - "ClickHouseSinkTask.start logs one WARN at task start, before the connection step, naming in ascending order every additionalRetriableErrorCodes entry that is neither in Utils.DEFAULT_RETRIABLE_ERROR_CODES nor 216"
  - "UnvettedRetriableCodesWarnUnitTest: 21 Docker-free cases that start a real task against a closed loopback port"
  - "UnvettedRetriableCodesWarnLiveTest: one live case (ClickHouse latest, client V2): start with code 60 succeeds, 5 records insert and read back equal, then the one WARN is asserted"
  - "FORK-DELTA Connector source row and Known cost 24 (the judgment behind 'transient'; FUT-05 stays open)"
  - "42-rsk03-expected-added-testunit.tsv (21 rows) and 42-rsk03-expected-added-full.tsv (22 rows)"
  - "42-COMMAND-EVIDENCE.md E-08..E-10"
affects: [42-04, 44-release-notes]

actuals:
  tokens: 14500
  tasks: 3
  commits: 4

tech-stack:
  added: []
  patterns:
    - "Docker-free wiring test: a real ClickHouseSinkTask started against a closed loopback port, LogCapture on the task logger, task.stop() in finally"
    - "live test whose WARN assertions come last, so a red run proves start, insert and read-back already work"
    - "red-then-green by assertion with a SHA-256 freeze of both test files between the red run and the code commit"

key-files:
  created:
    - src/test/java/com/clickhouse/kafka/connect/sink/UnvettedRetriableCodesWarnUnitTest.java
    - src/test/java/com/clickhouse/kafka/connect/sink/UnvettedRetriableCodesWarnLiveTest.java
    - .planning/phases/42-isolated-high-risk-edits/42-rsk03-expected-added-testunit.tsv
    - .planning/phases/42-isolated-high-risk-edits/42-rsk03-expected-added-full.tsv
  modified:
    - src/main/java/com/clickhouse/kafka/connect/sink/ClickHouseSinkTask.java
    - docs/FORK-DELTA.md
    - .planning/scripts/fork-anchors.txt
    - .planning/phases/42-isolated-high-risk-edits/42-COMMAND-EVIDENCE.md

key-decisions:
  - "The vetted set is the connector's own Utils.DEFAULT_RETRIABLE_ERROR_CODES plus 216 (QUERY_WITH_SAME_ID_IS_ALREADY_RUNNING); every other listed code is named in one WARN per task start. This is the connector's judgment, recorded as such in Known cost 24"
  - "The WARN fires in start() directly after validateBufferConfig and before ProxySinkTask is built, so it is logged even when ClickHouse is down; nothing validates, rejects or budgets the codes and ClickHouseSinkConfig is byte-identical to phase start"
  - "'Start-up still succeeds' is shown in two halves: the Docker-free class proves start() reaches the connection step after the WARN, the live class proves a real start, insert and read-back"

patterns-established:
  - "The WARN text is a constant plus a TreeSet<Integer> from the validated parser, and the unit test asserts the exact rendered message, so no runtime value other than the codes can appear in it"

requirements-completed: [RSK-03]

plan_head_before: cec34bb3f74ebe954c970d9d634e5034f8809357
plan_head_after: 372d5fcbc982a9aadea9e2bf8300fb5ea16e0c57

coverage:
  - id: D1
    description: "A task started with a non-transient code in additionalRetriableErrorCodes logs exactly one WARN on the ClickHouseSinkTask logger, naming the unvetted codes in ascending order (for '62, 60,159' it names [60, 62] and not 159), with only fixed text and integers; an unset setting, the 15 built-in codes, 216 and '216,159,3' log none"
    requirement: RSK-03
    verification:
      - kind: unit
        ref: "UnvettedRetriableCodesWarnUnitTest (21 cases; the 3 unvetted cases red on the unchanged tree, all 21 green after the edit; E-08, E-09, E-10)"
        status: pass
    human_judgment: false
  - id: D2
    description: "With additionalRetriableErrorCodes=60 a real task starts against ClickHouse latest x client V2, inserts 5 records that read back equal to the originals, and logs the one WARN; the Docker-free cases show start() reaches the connection step after the WARN"
    requirement: RSK-03
    verification:
      - kind: integration
        ref: "UnvettedRetriableCodesWarnLiveTest#startupWithAnUnvettedCodeWarnsAndStillInserts (red on the WARN-count assertion only, then green; E-08, E-09, E-10)"
        status: pass
    human_judgment: false
  - id: D3
    description: "Success criteria 3 (scope) and 4: ClickHouseSinkTask.java only gains 21 lines, ClickHouseSinkConfig.java and both existing additionalRetriableErrorCodes pin classes equal phase start, and a cache-busted testUnit (1822 = 1801 + 21) and a full suite (2421 = 2399 + 22) are green with status deltas of exactly the added rows"
    requirement: RSK-03
    verification:
      - kind: other
        ref: "42-COMMAND-EVIDENCE.md E-09 and E-10 (git diff --quiet against the phase start; status delta removed=0 added=21 and removed=0 added=22 match=yes)"
        status: pass
    human_judgment: false

duration: 43min
completed: 2026-11-02
status: complete
---

# Phase 42 Plan 03: Start-up WARN for unvetted additionalRetriableErrorCodes Summary

**A start-up WARN in `ClickHouseSinkTask.start` names every `additionalRetriableErrorCodes` entry outside the built-in retriable set and 216, with the no-retry-budget consequence, pinned by 21 Docker-free cases and one live case, in one revert unit with its FORK-DELTA row and Known cost 24.**

## Performance

- **Duration:** 43 min
- **Started:** 2026-11-02T12:24:27Z
- **Completed:** 2026-11-02T13:08Z
- **Tasks:** 3
- **Files modified:** 8 (4 created, 4 modified)

## Accomplishments

- A task started with a non-transient code logs exactly one value-free WARN (the code list in ascending order plus fixed text) before the connection step, so it also fires when ClickHouse is down. Vetted codes (the 15 built-in ones, 216) and an unset setting log nothing.
- Red on the unchanged tree for the right reason (E-08): of the Docker-free class's 21 cases only the 3 unvetted ones fail, each by `AssertionFailedError` because no WARN is logged. The live case starts, inserts and reads back, and fails only on its WARN-count assertion. Both test files were frozen by SHA-256 and the committed blobs are byte-identical.
- Gate on the code commit (E-10): `testUnit` with Docker unreachable ran 1822 tests (1801 + 21, equal to the baseline 1757 + 65) and the full suite on ClickHouse `latest` x V2 ran 2421 (2399 + 22, equal to 2355 + 66), both with 0 failures and 0 errors and the 2 and 18 named skips. Both status deltas removed nothing and added exactly the committed rows, so every existing testcase, including the 19 `AdditionalRetriableErrorCodesPinUnitTest` and 3 `AdditionalRetriableErrorCodesTest` cases, kept its status.
- Scope held: `ClickHouseSinkTask.java` gained 21 lines and lost none (three imports, a comment, two constants, the private static helper, one call). `ClickHouseSinkConfig.java` is unchanged, and `git diff --name-only` of `src/main` against the phase start names exactly `Processing.java`, `Utils.java` and `ClickHouseSinkTask.java`.

## Task Commits

1. **Task 1 (tracer): red runs on the unchanged tree, then the WARN helper and its call turn both green** - `548969a8` (docs: E-08 red evidence; the test classes and the edit stay uncommitted until Task 2)
2. **Task 2: FORK-DELTA row, Known cost 24, two anchor rows, expected-added lists, RSK-03 code commit** - `4935e9c5` (fix), then `5d81088f` (docs: E-09 and the two expected-added files)
3. **Task 3: gate on the RSK-03 commit** - `372d5fcb` (docs: E-10)

**Plan metadata:** the SUMMARY commit that follows this file (docs: complete plan).

## Files Created/Modified

- `src/main/java/com/clickhouse/kafka/connect/sink/ClickHouseSinkTask.java` - the WARN constant, the vetted-code constant 216, `warnOnUnvettedRetriableCodes` and its call in `start()`
- `src/test/java/com/clickhouse/kafka/connect/sink/UnvettedRetriableCodesWarnUnitTest.java` - 21 Docker-free start-up cases
- `src/test/java/com/clickhouse/kafka/connect/sink/UnvettedRetriableCodesWarnLiveTest.java` - the live case
- `docs/FORK-DELTA.md` - the Connector source row after the connector-name row, and Known cost 24
- `.planning/scripts/fork-anchors.txt` - one Phase 42 comment and two RSK-03 rows (mirror rows=190 bad=0)
- `.planning/phases/42-isolated-high-risk-edits/42-rsk03-expected-added-testunit.tsv`, `42-rsk03-expected-added-full.tsv` - 21 and 22 PASS rows
- `.planning/phases/42-isolated-high-risk-edits/42-COMMAND-EVIDENCE.md` - E-08, E-09, E-10

## Decisions Made

- The definition of "transient" is the connector's own built-in set plus 216 (planning decision 2). It is a judgment, not a ClickHouse classification, and Known cost 24 says so: a code such as 60 can be transient while a table is created, and the WARN still names it. Its cost if wrong is one constant.
- No validator, rejection or retry budget was added, because a rejecting validator would stop an existing connector from starting after an upgrade. FUT-05 stays open and Known cost 17 is untouched.

## Deviations from Plan

None - plan executed exactly as written.

## Issues Encountered

None. Neither run hit the OpenJ9 crash (HotSpot 21 via `-PjavaVersion=21`), no crash file appeared, and `origin/custom_build` equalled the E-01 value at every gate.

## Known Stubs

None.

## Threat Flags

None. The WARN adds a log line built from a constant and a `TreeSet<Integer>`; no new endpoint, auth path, file access or schema surface.

## User Setup Required

None - no external service configuration required.

## Next Phase Readiness

All three code commits (RSK-01, RSK-02, RSK-03) have passed their own gates. Plan 42-04 (the closing gate, E-11 and E-12) is next. `.planning/config.json` and `.planning/state.json` were left modified for the orchestrator, and `.planning/STATE.md` is updated by the state verbs.

## Self-Check: PASSED

- Created files exist on disk: both test classes, both expected-added files, and the evidence file.
- Commits `548969a8`, `4935e9c5`, `5d81088f` and `372d5fcb` are on `sync/v1.7.0`.

---
*Phase: 42-isolated-high-risk-edits*
*Completed: 2026-11-02*
