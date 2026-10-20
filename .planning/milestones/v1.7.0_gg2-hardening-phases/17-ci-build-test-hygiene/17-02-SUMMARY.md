---
phase: 17-ci-build-test-hygiene
plan: "02"
subsystem: testing
tags: [gradle, lz4, lombok, jacoco-free, testcontainers, clickhouse-client-v2, debezium]

requires:
  - phase: 17-ci-build-test-hygiene
    provides: plan 17-01 CI hardening, FORK-DELTA Build/CI section and Known costs items 5 and 6, and 17-EVIDENCE.md E-01..E-04
provides:
  - testRuntimeClasspath exclude of org.lz4:lz4-java, pinned Docker-free by TestRuntimeClasspathLz4UnitTest (DEBT-07, backlog 999.2)
  - response-leg LZ4 assertion in Lz4RoundTripLiveTest from system.query_log, seen red (DEBT-06, 14-REVIEW WR-01)
  - lombok.jacksonized.jacksonVersion += 2 with a javap no-change proof (BLD-14)
  - four mantissa-with-fraction NanoTimestamp pin rows (DEBT-08, 15-REVIEW IN-06)
affects: [17-03, 17-04, phase-20-release]

actuals:
  tokens: 4060
  tasks: 3
  commits: 8

tech-stack:
  added: []
  patterns:
    - "Docker-free classpath pin that counts every jar holding a class, not only the first code source"
    - "Live-test compression proof read from system.query_log under a client-chosen query id, with a one-off uncommitted red flip"

key-files:
  created:
    - src/test/java/com/clickhouse/kafka/connect/ci/TestRuntimeClasspathLz4UnitTest.java
  modified:
    - build.gradle.kts
    - lombok.config
    - src/test/java/com/clickhouse/kafka/connect/sink/db/helper/Lz4RoundTripLiveTest.java
    - src/test/java/com/clickhouse/kafka/connect/sink/db/MetadataFreeJsonWriterTest.java
    - docs/FORK-DELTA.md
    - .planning/phases/17-ci-build-test-hygiene/17-EVIDENCE.md

key-decisions:
  - "The duplicate org.lz4 jar is removed by an exclude scoped to testRuntimeClasspath, not by configurations.all or a catalog substitution; the three shipped configurations are byte-identical before and after"
  - "DEBT-06 uses the primary D-07 path (the read's own QueryFinish row); the 'both legs' claim needed no fallback"
  - "lombok.config selects Jackson 2 with += 2 because the key is a list and the runtime mapper is Jackson 2; Jackson 3 was not tried"

patterns-established:
  - "Red-first Docker-free pin, committed as test(...) before the build(...) commit that turns it green"
  - "Negative control flipped locally, recorded with real bytes in evidence, reverted before any commit"

requirements-completed: [DEBT-07, DEBT-06, BLD-14, DEBT-08]

coverage:
  - id: D1
    description: "DEBT-07: LZ4Factory is on the test runtime classpath exactly once, from at.yawk.lz4; runtimeClasspath, integrationTestRuntimeClasspath and clickhouseDependencies are byte-identical, and testRuntimeClasspath lost only org.lz4:lz4-java:1.8.0"
    requirement: "DEBT-07"
    verification:
      - kind: unit
        ref: "TestRuntimeClasspathLz4UnitTest#lz4FactoryIsOnTheTestClasspathExactlyOnceFromTheMaintainedFork"
        status: pass
      - kind: other
        ref: "17-EVIDENCE.md#E-05 (CLASSPATHS-OK, LZ4-EXCLUDE-SHAPE-OK)"
        status: pass
    human_judgment: false
  - id: D2
    description: "DEBT-06: Lz4RoundTripLiveTest asserts lz4 and NetworkSendBytes below a tenth of result_bytes for the read's own query id, and fails with 'response not compressed' when server-response compression is off"
    requirement: "DEBT-06"
    verification:
      - kind: integration
        ref: "Lz4RoundTripLiveTest#insertAndReadBackOverLz4OnBothLegs"
        status: pass
      - kind: other
        ref: "17-EVIDENCE.md#E-06 (LZ4-RESPONSE-LEG-GREEN, SOURCE-AND-RED-FLIP-OK)"
        status: pass
    human_judgment: false
  - id: D3
    description: "BLD-14: a clean compile of main, testFixtures, test and integrationTest prints no Lombok Jackson-variant ambiguity warning, with ClickHouseFieldDescriptor and its builder byte-identical under normalized javap"
    requirement: "BLD-14"
    verification:
      - kind: other
        ref: "17-EVIDENCE.md#E-07 (BLD-14-OK)"
        status: pass
    human_judgment: false
  - id: D4
    description: "DEBT-08: NanoTimestamp 123, -123 and 999 render in scientific form with a fraction and 1000 is the first plain form; all seven NanoTimestamp cases pass"
    requirement: "DEBT-08"
    verification:
      - kind: unit
        ref: "MetadataFreeJsonWriterTest#debeziumTimestampEdgeValuesAreExactDecimalSeconds"
        status: pass
      - kind: other
        ref: "17-EVIDENCE.md#E-08 (DEBT-08-OK)"
        status: pass
    human_judgment: false

duration: 4min
completed: 2026-10-11
status: complete
plan_head_before: f0919f7b907091cdcdbd589eaddbd0146b8f2697
plan_head_after: 2be1db6cda6ef983a693414ac4f0a6461bd32171
---

# Phase 17 Plan 02: Build and test debt closure Summary

**A testRuntimeClasspath-only exclude leaves one LZ4Factory on the test classpath (pinned Docker-free), the "both legs" LZ4 claim is now asserted from the read's own query_log row and seen red, Lombok's Jackson ambiguity warning is gone with byte-identical generated code, and NanoTimestamp's scientific-notation form is pinned.**

## Performance

- **Duration:** 4 min
- **Started:** 2026-10-11T20:20:01Z
- **Completed:** 2026-10-11T20:24:25Z
- **Tasks:** 3
- **Files modified:** 7 (one created)

## Accomplishments

- DEBT-07: `TestRuntimeClasspathLz4UnitTest` failed with `expected: <1> but was: <2>` (at.yawk.lz4 1.12.0 and org.lz4 1.8.0), then passed after `configurations.named("testRuntimeClasspath") { exclude(group = "org.lz4", module = "lz4-java") }`. `dependencies` output for runtimeClasspath, integrationTestRuntimeClasspath and clickhouseDependencies is byte-identical; testRuntimeClasspath lost exactly the one `org.lz4:lz4-java:1.8.0` line. Closes backlog 999.2.
- DEBT-06: the read-back runs under its own query id. Green on ClickHouse `26.9.16.11`: `NetworkSendBytes=61864 result_bytes=4790219 ratio=0.0129 method=lz4`. With a local `compressServerResponse(false)`: `response not compressed: NetworkSendBytes=4778633 result_bytes=4790219` (ratio 0.9976). The flip was reverted and never committed. D-07 path used: primary (no fallback). Closes 14-REVIEW WR-01.
- BLD-14: Lombok ambiguity warning lines went from 1 to 0 across the four-source-set clean compile. Normalized `javap -v -p` is byte-identical for `ClickHouseFieldDescriptor` (1193 lines) and its builder (420 lines, 21 `lombok.Generated` occurrences before and after).
- DEBT-08: four `@CsvSource` rows (123, -123, 999, 1000) plus an extended display name; all seven NanoTimestamp cases pass. Closes 15-REVIEW IN-06.
- Full `testUnit`: 464 tests, 0 failures, 0 errors, 2 skipped (459 at the end of 17-01, plus 1 and 4 new).

## Evidence verdicts

| Entry | Requirement | Verdict |
|-------|-------------|---------|
| E-05 | DEBT-07 | pass |
| E-06 | DEBT-06 | pass |
| E-07 | BLD-14 | pass |
| E-08 | DEBT-08 | pass |

## Task Commits

1. **Task 1 (DEBT-07):** RED `7993bea2` (test), GREEN `e50712a0` (build), FORK-DELTA and E-05 `eb4cdc5a` (docs)
2. **Task 2 (DEBT-06):** `4bbe32f9` (test), FORK-DELTA and E-06 `6632328f` (docs)
3. **Task 3 (BLD-14, DEBT-08):** `ac349085` (build, lombok.config), `6379fb9d` (test, NanoTimestamp rows), FORK-DELTA and E-07/E-08 `2be1db6c` (docs)

## Files Created/Modified

- `src/test/java/com/clickhouse/kafka/connect/ci/TestRuntimeClasspathLz4UnitTest.java` - Docker-free single-LZ4Factory pin
- `build.gradle.kts` - test-scoped org.lz4 exclude, placed between the capability rule and `dependencies {`
- `src/test/java/com/clickhouse/kafka/connect/sink/db/helper/Lz4RoundTripLiveTest.java` - response-leg assertion, ratio log line, Javadoc updated
- `lombok.config` - third line `lombok.jacksonized.jacksonVersion += 2`
- `src/test/java/com/clickhouse/kafka/connect/sink/db/MetadataFreeJsonWriterTest.java` - four NanoTimestamp rows
- `docs/FORK-DELTA.md` - exclude and lombok.config Build rows, Known costs items 5 and 7
- `.planning/phases/17-ci-build-test-hygiene/17-EVIDENCE.md` - E-05..E-08

## Decisions Made

- Scoped exclude on `testRuntimeClasspath` rather than a catalog substitution: smaller edit, reproduced, and it cannot move the shipped classpaths (proved by byte-identity).
- The ratio bound is a tenth of `result_bytes` (D-08); green sat at 0.0129 and red at 0.9976, so the margin is wide on both sides.
- `+= 2`, never `+= 3`: the runtime mapper is Jackson 2.

## Deviations from Plan

None - plan executed exactly as written. One slip was corrected before commit: the E-07 and E-08 timestamps were first written as 20:27Z against a system clock of 20:23Z and were fixed to 20:23Z in the same commit as the entries.

## Issues Encountered

None. The `warning:` line total of the clean compile was 179 in both the before and the after log; the Lombok line is the one that disappeared, and the difference was made up by javac reporting an unrelated `[deprecation]` line (ClickHouseHelperClient.java:687) once instead of twice.

## Known Stubs

None.

## Threat Flags

None. The exclude narrows the test classpath only, and the new log line carries byte counts, a ratio and a setting name (T-17-09..T-17-12 mitigated as planned).

## User Setup Required

None - no external service configuration required.

## Next Phase Readiness

- Ready for 17-03. `src/main` and `fork-anchors.txt` are unchanged against the phase start `e0017d69`; nothing was pushed.
- If `Lz4RoundTripLiveTest` goes red on unchanged code later, check the `clickhouse/clickhouse-server:latest` image date and `SELECT version()` first (green here on `26.9.16.11`).

## Self-Check: PASSED

- Files exist: the new unit test, `lombok.config`, the three edited test and build files, FORK-DELTA, and `17-EVIDENCE.md`.
- Commits `7993bea2`, `e50712a0`, `eb4cdc5a`, `4bbe32f9`, `6632328f`, `ac349085`, `6379fb9d`, `2be1db6c` are ancestors of HEAD; `git rev-list --count` from the plan base gives 8.
- Verify markers printed: LZ4-SINGLE-URL-GREEN (test XML 1/0/0/0), CLASSPATHS-OK, LZ4-EXCLUDE-SHAPE-OK parts, LZ4-RESPONSE-LEG-GREEN, SOURCE-AND-RED-FLIP-OK, BLD-14-OK, FORKDELTA checks, `src/main` unchanged.

---
*Phase: 17-ci-build-test-hygiene*
*Completed: 2026-10-11*
