---
phase: 48-phase-37-dispositions-document-fixes
plan: "02"
subsystem: testing
tags: [characterization-pins, mutation-evidence, allow-list-rule, null-binding, stop-pins, retriable-codes, docker-free]
status: complete

requires:
  - phase: 48-phase-37-dispositions-document-fixes
    provides: plan 48-01 baseline B0 c77d1a14 (testUnit 2295, eight per-class counts), the md5-checked XML snapshots and status dumps under /tmp/gsd48/exec, the frozen release path
provides:
  - six Docker-free test commits (two new classes, four extended classes with lines added only), each shown red by a throwaway mutation
  - 48-expected-added.tsv with the 25 testcase rows this plan adds, for the testUnit and suite deltas of plans 48-03 and 48-05
  - evidence entries E-03..E-09
affects: [48-03, 48-05, 49]

actuals:
  tokens: 13700
  tasks: 3
  commits: 14
plan_head_before: b47d92f657cb7f88e428c1ebf4fdcc43fc3599a8
plan_head_after: 284cff49071142bd3ca8841679b9ede25778026d

tech-stack:
  added: []
  patterns: [characterization pin shown red by a throwaway worktree mutation recorded in the evidence file, additive-only extension of existing test classes (numstat deletions 0), allow-list rule as a test over every shipped list instead of a script rule]

key-files:
  created:
    - src/test/java/com/clickhouse/kafka/connect/sink/db/helper/DescribeUnreadableEnumNullBindingUnitTest.java
    - src/test/java/com/clickhouse/kafka/connect/ci/AllowListWildcardRuleUnitTest.java
    - .planning/phases/48-phase-37-dispositions-document-fixes/48-expected-added.tsv
  modified:
    - src/test/java/com/clickhouse/kafka/connect/sink/ProxySinkTaskStopUnitTest.java
    - src/test/java/com/clickhouse/kafka/connect/sink/ClickHouseSinkTaskStopUnitTest.java
    - src/test/java/com/clickhouse/kafka/connect/sink/UnvettedRetriableCodesWarnUnitTest.java
    - src/test/java/com/clickhouse/kafka/connect/sink/RetriableExceptionPassThroughUnitTest.java
    - .planning/phases/48-phase-37-dispositions-document-fixes/48-COMMAND-EVIDENCE.md

key-decisions:
  - "The allow-list rule is a test over every confluent-archive-allowlist-*.txt (49 wildcard rows over 5 lists, minimum 2 literal segments), not a script rule: compare_confluent_archives.py (md5 a637e0cc) and every list stay as published (U-9)"
  - "The null binding is a new class beside DescribeUnreadableEnumMockServerTest, which stays byte-identical to B0 (U-5)"
  - "The four extra pins (40 IN-03, 42 IN-03 part b, 42 IN-04, 47 IN-01) are new methods in the existing classes, zero deleted lines (U-13)"

requirements-completed: [P37-01, P37-02, P37-03, P37-04]

coverage:
  - id: D1
    description: "Null binding of the unreadable enum describe (28 IN-05(b) item i, U-5): DescribeUnreadableEnumNullBindingUnitTest, 4 cases on both clients, the null comes with an ERROR whose throwable holds 'Enum type could not be read'; a malformed non-enum row is also null but without that text; mutation (stub serves the malformed row for the enum table) turns exactly the 2 binding rows red"
    requirement: P37-01
    verification:
      - kind: other
        ref: "P48-02-T1-OK (E-03)"
        status: pass
    human_judgment: false
  - id: D2
    description: "Two-literal-segment rule over every shipped allow-list (UF-37-3, U-9): AllowListWildcardRuleUnitTest, 9 cases over 5 lists with 49 wildcard rows; a com/* row in a worktree copy of the gg7 list turns exactly the shipped-list case red with a message naming com/*; release path unedited"
    requirement: P37-03
    verification:
      - kind: other
        ref: "P48-02-T1-OK (E-04)"
        status: pass
    human_judgment: false
  - id: D3
    description: "40 IN-03 and 47 IN-01 stop pins: 2 MBean-unregister-failure cases in ProxySinkTaskStopUnitTest (mutation: exactly those 2 red) and the MBean precondition case in ClickHouseSinkTaskStopUnitTest (mutations: CLOSE_COUNT in 7 of 7, MBEAN_GONE in 7 of 7, precondition only the new case)"
    requirement: P37-04
    verification:
      - kind: other
        ref: "P48-02-T2-OK (E-05, E-06)"
        status: pass
    human_judgment: false
  - id: D4
    description: "42 IN-03 part b and 42 IN-04 pins: 7 blank/empty/repeated entry rows of additionalRetriableErrorCodes (mutation: exactly 216,,159 and 159,,60 red) and the 2 plain DataException control rows (mutation: exactly the none row red)"
    requirement: P37-04
    verification:
      - kind: other
        ref: "P48-02-T3-OK (E-07, E-08)"
        status: pass
    human_judgment: false
  - id: D5
    description: "Wave gate: cache-busted testUnit with Docker unreachable 2320 tests (2295 + 25), 0 failures, 2 skips; delta removed=0 added=25 expected=25 match=yes against the committed 48-expected-added.tsv; src diff since B0 is the two new and four extended test files, deletions 0; src/main, .github, .planning/scripts unchanged"
    requirement: P37-04
    verification:
      - kind: other
        ref: "P48-02-T3-OK (E-09)"
        status: pass
    human_judgment: false

duration: 11min
completed: 2026-11-06
---

# Phase 48 Plan 02: Additive Docker-free test pins Summary

**Six additive Docker-free pins (null binding of the unreadable enum describe, two-literal-segment allow-list rule over all 49 shipped wildcard rows, MBean unregister failure and precondition in the two stop tests, blank/empty code entries, plain DataException control), each its own commit and each seen red by a throwaway mutation; testUnit now 2320 (2295 + 25) with the delta removing nothing.**

## Performance

- **Duration:** about 11 min (Gradle runs are 2 to 30 s each; the per-file Spotless checks and nine worktree mutation runs dominate)
- **Started:** about 2026-11-06T08:34Z
- **Completed:** 2026-11-06T08:45Z
- **Tasks:** 3
- **Files:** 2 test classes created, 4 test classes extended, `48-expected-added.tsv` created, evidence file extended; no `src/main`, `.github` or `.planning/scripts` change

## Accomplishments

The six test commits (each one file, in plan order) and the evidence commits after them:

| # | Row ID | Commit | File | Added cases |
|---|--------|--------|------|-------------|
| 1 | 28 IN-05(b) item i (U-5) | `4de705a6` | `DescribeUnreadableEnumNullBindingUnitTest` (new) | 4 |
| 2 | UF-37-3 (U-9) | `07e65662` | `AllowListWildcardRuleUnitTest` (new) | 9 |
| 3 | 40 IN-03 | `69b75cb3` | `ProxySinkTaskStopUnitTest` (+75 lines) | 2 |
| 4 | 47 IN-01 | `ae31c4a9` | `ClickHouseSinkTaskStopUnitTest` (+12 lines) | 1 |
| 5 | 42 IN-03 part b | `64ef7568` | `UnvettedRetriableCodesWarnUnitTest` (+26 lines) | 7 |
| 6 | 42 IN-04 | `3182791f` | `RetriableExceptionPassThroughUnitTest` (+34 lines) | 2 |

Evidence commits: E-03 `852fddc3`, E-04 `7b46cce0`, E-05 `a28d2dd9`, E-06 `7370d792`, E-07 `caa02650`, E-08 `007a909c`, `48-expected-added.tsv` `ee4f9d6e`, E-09 `284cff49`.

Mutation red sets (each in a throwaway `/tmp/gsd48/wt-*` worktree at the test commit, never committed, every worktree removed):

| Pin | Mutation | Red set |
|-----|----------|---------|
| null binding | stub serves the malformed row for the enum table (`? UNREADABLE_ENUM_ROW :` to `? MALFORMED_ROW :`) | 4 tests, 2 failures: both rows of `unreadableEnumTableIsNullBecauseTheEnumScanRefusedIt` |
| allow-list rule | `com/*` row appended to the worktree copy of the gg7 list | 9 tests, 1 failure: `everyShippedWildcardRowFollowsTwoLiteralSegments`, message names `gg7.txt:29: com/*` |
| 40 IN-03 | unregister catch narrowed to `Error` in `ProxySinkTask.stop` | 6 tests, 2 failures: exactly the two `mBeanUnregisterFailure...` cases |
| 47 IN-01 m1 | `stateProvider.close()` replaced | 7 of 7 red, 7 of 7 messages contain `close count of the state provider` |
| 47 IN-01 m2 | `statistics.unregisterMBean()` replaced | 7 of 7 red, 7 of 7 messages contain `the task MBean must be unregistered after stop` |
| 47 IN-01 m3 | MBean registration removed in `SinkTaskStatistics` | 7 tests, 1 failure: the new case, message `the task MBean must be registered before stop` (the six existing cases pass vacuously, which the precondition closes) |
| 42 IN-03 (b) | `code.isEmpty() && false` in `parseErrorCodes` | 28 tests, 2 failures: the `216,,159` and `159,,60` rows (start fails with `Failed to start new task` before the connection step) |
| 42 IN-04 | `&& false` on the `DataException` branch of `Utils.handleException` | 40 tests, 1 failure: the `errors.tolerance=none expected=DATA_EXCEPTION` row |

Wave testUnit total: **2320 tests** (E-01 2295 plus 25), 0 failures, 0 errors, 2 skips, 107 XML files (105 at the base plus the two new classes), exit 0; delta against the base `removed=0 added=25 expected=25 match=yes`. SHA-256 of `48-expected-added.tsv`: `89c2dd51e59fb741a114322035a4ff5a976ee791e764782ad86c28c766208126` (25 rows, groups 4 9 2 1 7 2, all PASS).

Independent allow-list count (before the test was written): 5 lists, 49 wildcard rows, minimum 2 literal path segments (the `META-INF/versions/*/com/fasterxml/jackson/core/internal/shaded/fdp/*` row of the `-25` and `-gg4` lists). Compare script md5 `a637e0cc8fb33a33e7673ac682fa63c8`, equal to E-01.

No JVM crash occurred: no `-PjavaVersion=21` run was needed, no core dump or crash file appeared, and the default toolchain ran every build.

## Task Commits

1. **Task 1 (tracer): null binding and allow-list rule** - `4de705a6`, `07e65662` (test), evidence `852fddc3`, `7b46cce0` (docs)
2. **Task 2: stop pins** - `69b75cb3`, `ae31c4a9` (test), evidence `a28d2dd9`, `7370d792` (docs)
3. **Task 3: classification and start-up pins, wave gate** - `64ef7568`, `3182791f` (test), evidence `caa02650`, `007a909c`, `ee4f9d6e`, `284cff49` (docs)

## Files Created/Modified

- `src/test/java/com/clickhouse/kafka/connect/sink/db/helper/DescribeUnreadableEnumNullBindingUnitTest.java` - new, HTTP stub plus `LogCapture`, both clients
- `src/test/java/com/clickhouse/kafka/connect/ci/AllowListWildcardRuleUnitTest.java` - new, reads every `confluent-archive-allowlist-*.txt` under `.planning/scripts`
- `src/test/java/com/clickhouse/kafka/connect/sink/ProxySinkTaskStopUnitTest.java`, `ClickHouseSinkTaskStopUnitTest.java`, `UnvettedRetriableCodesWarnUnitTest.java`, `RetriableExceptionPassThroughUnitTest.java` - lines added only (12, 75, 26, 34; deletions 0)
- `.planning/phases/48-phase-37-dispositions-document-fixes/48-expected-added.tsv` - the 25 added testcase rows
- `.planning/phases/48-phase-37-dispositions-document-fixes/48-COMMAND-EVIDENCE.md` - E-03..E-09 and the flipped index rows

## Decisions Made

- Followed the plan's table exactly; no pin needed a test-side fix before it was green and no expected value moved. Every new test went green on its first run on the unchanged production code, and every mutation read exactly as planned.
- In the new classes the one case that parses text lists is a static helper (`followsTwoLiteralSegments`) shared by the shipped-list case and the two parameterized cases, so the rule has one implementation.

## Deviations from Plan

None - plan executed exactly as written.

## Authentication Gates

None.

## Issues Encountered

None. The inputs under `/tmp/gsd48/exec` were present and their md5 manifests matched E-01 (both XML snapshot manifests and both status dumps), so no re-base was needed.

## Known Stubs

None.

## Threat Flags

None. Only test files and evidence changed; no network endpoint, auth path or schema changed. The new HTTP stub binds `127.0.0.1` on an ephemeral port and is stopped after each test.

## Next Phase Readiness

Ready for plan 48-03 (the 28 IN-01 constant move): its testUnit expectation is 2320 (unchanged by the move) and its suite expectation is the E-01 baseline 2898 plus 25 using `48-expected-added.tsv` with `delta --by-class`. STATE.md, ROADMAP.md, REQUIREMENTS.md, `.planning/config.json` and `.planning/state.json` were not staged by any commit of this plan.

## Self-Check: PASSED

- Files exist: both new test classes, `48-expected-added.tsv`, `48-COMMAND-EVIDENCE.md` (tracked, committed).
- Commits exist on HEAD: `4de705a6`, `07e65662`, `69b75cb3`, `ae31c4a9`, `64ef7568`, `3182791f` (tests) and `852fddc3`, `7b46cce0`, `a28d2dd9`, `7370d792`, `caa02650`, `007a909c`, `ee4f9d6e`, `284cff49` (evidence); `git rev-list --count b47d92f6..HEAD` read 14 before this SUMMARY commit.
- `P48-02-T1-OK`, `P48-02-T2-OK` and `P48-02-T3-OK` printed on the committed tip; anchors `rows=218 bad=0`; no worktree remains; `git status --short` lists only `.planning/config.json` and `.planning/state.json`.
