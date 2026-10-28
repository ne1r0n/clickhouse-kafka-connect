---
phase: 31-fork-owned-nightly-integration-tests
plan: "01"
subsystem: testing
tags: [integration-tests, testcontainers, clickhouse-latest, client-v2, confluent-connect, gradle, measurement]

requires:
  - phase: 30-release-workflows-pin-guard
    provides: evidence-file format, WorkflowActionPinGuardUnitTest, SHA-pinned action lines
provides:
  - "31-COMMAND-EVIDENCE.md E-01 to E-05: the criterion-1 measurement of the whole integrationTest task on ClickHouse latest x client V2"
  - "nightly-timeouts: step=30 job=40 (derived from the 14 minute cold run, PT-02)"
  - "Root cause of every connector-dependent integration failure: Java 17 connector jar (class file 61) against the Java 11 Connect worker of the harness image"
affects: [31-02, 31-03, 31-04, 31-05, 32]

actuals:
  tokens: 7100
  tasks: 2
  commits: 2

tech-stack:
  added: []
  patterns:
    - "Measurement before workflow: evidence entries with machine-read lines (server-version, skip-candidates, nightly-timeouts, upstream-red, run-commit)"

key-files:
  created:
    - .planning/phases/31-fork-owned-nightly-integration-tests/31-COMMAND-EVIDENCE.md
  modified: []

key-decisions:
  - "Gradle for live runs is invoked with JAVA_HOME set to the Gradle-provisioned Temurin 17: this machine's default daemon JVM (Homebrew JDK 21) differs from the Semeru 17 toolchain, and build.gradle.kts lines 197-198 copy java.home into the test JVM, which kills the OpenJ9 test JVM (environment-only, no repository change)"
  - "Proposed category for all 54 failed testcases is harness (Java 11 worker image cannot load the fork's Java 17 connector jar); connector-defect is the stated alternative if the user holds that the connector must load on Java 11"
  - "upstream-red verdict is reproduced at the symptom level for the fork's run 38881153720; the CI log was not read and the 29.6 minute CI wall time against 13.4 minutes locally is unexplained"

requirements-completed: []

coverage:
  - id: D1
    description: "Criterion-1 measurement record E-01 to E-05 exists and is committed before any workflow file: slice, cold full run, images, classes, failures with proposed causes, timeouts, upstream verdict"
    requirement: "NIT-01"
    verification:
      - kind: other
        ref: "Task 1 verify command (SLICE-OK) and Task 2 verify commands (RUN-CAPTURED-OK, MEASUREMENT-RECORD-OK) in 31-01-PLAN.md"
        status: pass
    human_judgment: false
  - id: D2
    description: "The user's disposition of each measured failure, the timeouts and the upstream explanation is recorded verbatim as E-06"
    requirement: "NIT-03"
    verification: []
    human_judgment: true
    rationale: "Task 3 is a blocking-human checkpoint:decision; the answer does not exist yet and must come from the user directly"

duration: 26min
completed: 2026-10-26
status: complete
plan_head_before: 41d965e2d6d04cd5fa335291b999fe6721fc53d3
plan_head_after: 943b10728a46b29d28db32c42cad14117e8760f7
commits: 2
---

# Phase 31 Plan 01: Measurement Summary

**The whole `integrationTest` task on ClickHouse 26.10.3.7 (`latest`) x client V2 fails 54 of 54 connector tests for one reason, a Java 17 connector jar against the harness's Java 11 Connect worker; the cloud and exactly-once classes skip as designed, and the nightly timeouts come out at step 30 / job 40 minutes. Task 3, the blocking-human decision, is pending.**

## Status

`complete` (was `halted` at the designed stop until the user answered Task 3 with `fix-harness`, recorded as E-06; gap plan 31-06 re-measures into E-06b): Tasks 1 and 2 are done and committed; Task 3 (`checkpoint:decision`, `gate="blocking-human"`) waits for the user's own answer. Plans 31-02 to 31-05 must not start until E-06 is recorded.

## Performance

- **Duration:** about 26 min (cold integration run 13 min 22 s of it)
- **Started:** 2026-10-26T11:34Z (approximate)
- **Completed (tasks 1 and 2):** 2026-10-26T12:00Z
- **Tasks:** 2 of 3 (Task 3 pending the user)
- **Files modified:** 1 (the evidence file, new); no file under `src/`, `build.gradle.kts`, `gradle/` or `.github/` changed

## Accomplishments

- E-01 and E-02 (tracer): colima `vz` environment, ten-image inventory, a freshly pulled `latest` (digest `739e099a...`, `SELECT version()` = `26.10.3.7`), and a real Gradle `integrationTest` run of `ClickHouseCloudTest`: Gradle reports the assumption-aborted `@BeforeAll` class as a skipped XML testcase (research A4 measured).
- E-03 to E-05: the cold full run (14 min, W = 14), per-class table, all 54 failures with one shared cause, the Docker image table with digests, the Connect worker JVM, peak memory, the timeouts and the CI-only risk checklist.
- The root cause is measured, not guessed: the worker log shows `UnsupportedClassVersionError ... class file version 61.0 ... up to 55.0` for the connector and every fork SMT, so `POST /connectors` returns 500 and each test fails at `ConfluentPlatform.java:360`. Upstream v1.7.0 builds Java 11 (class file 55); the fork's Java 17 level follows from its `connect-api` 4.4.0 pin, and a scratch probe with `options.release.set(11)` failed dependency resolution, so the bytecode level cannot simply be lowered.

## Test classes (E-05)

| Class | Status | tests | failures | errors | skipped |
|-------|--------|-------|----------|--------|---------|
| ClickHouseSinkConnectorIntegrationTest | ran | 54 | 54 | 0 | 0 |
| ExactlyOnceTest | skipped | 8 | 0 | 0 | 8 |
| ClickHouseCloudTest | skipped | 1 | 0 | 0 | 1 |

## Failures and proposed dispositions (pending the user)

All nine methods below fail at the same line with the same cause and have the same proposal: category `harness`, reason "Connect worker image runs Java 11 and cannot load the Java 17 connector jar, so POST /connectors returns 500", re-open trigger "the harness uses a Connect worker with Java 17 or newer".

| Method | Invocations failed/total |
|--------|--------------------------|
| avroSchemaTest | 25/25 |
| protoSchemaTest | 22/22 |
| stockGenSingleTaskTest, stockGenSingleTaskSchemalessTest, stockGenMultiTaskTopicTest, stockGenMultiTaskSchemalessTest, stockGenSingleTaskInterruptTest, stockGenMultiTaskInterruptTest, stockGenWithJdbcPropSingleTaskTest | 1/1 each |

Excluding all of them would leave the nightly with zero executing tests. The first cause masks everything behind it: no test got as far as the connector starting.

## Timeouts (E-05, D-05)

`nightly-timeouts: step=30 job=40` (cold local wall 14 min x 2, rounded up to a multiple of 5; job = step + 10). The measured run is a fast red run; healthy CI is about 20 minutes and red CI runs took 30 to 52 minutes, so S = 30 sits at the edge (the fork's own red run 38881153720 took 29.6 minutes). The user may amend this at Task 3.

## Upstream verdict (E-05)

`upstream-red: reproduced` at the symptom level for the fork's run 38881153720 (non-2xx `POST /connectors` in every connector test). Not confirmed against that run's log, and the CI wall time (29.6 min) against 13.4 minutes locally is unexplained. Upstream's own daily red cannot be this cause, because upstream compiles for Java 11. The token route is therefore still meaningful.

## Task Commits

1. **Task 1: tracer, environment, latest pull, SELECT version(), integrationTest slice** - `07fc1f31` (docs)
2. **Task 2: cold full run, E-03 to E-05** - `943b1072` (docs)
3. **Task 3: checkpoint:decision (blocking-human)** - pending the user's own answer; E-06 is appended by the orchestrator

**Plan metadata:** the commit that carries this SUMMARY, STATE.md and ROADMAP.md

## Files Created/Modified

- `.planning/phases/31-fork-owned-nightly-integration-tests/31-COMMAND-EVIDENCE.md` - E-01 to E-05 (new)

## Decisions Made

- `JAVA_HOME` set to the Gradle-provisioned Temurin 17 for live Gradle runs (see the deviation); recorded in the evidence header and E-02.
- `upstream-red: reproduced` is worded as symptom-level and not confirmed against the CI log, so the user can still choose the token route at Task 3.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 3 - Blocking] Gradle daemon JVM differs from the toolchain JVM, so the test JVM died before any test**
- **Found during:** Task 1 (the slice)
- **Issue:** The daemon runs on Homebrew JDK 21 (no `JAVA_HOME`), `integrationTest` resolves to Semeru JDK 17 (OpenJ9), and `build.gradle.kts` lines 197-198 copy every Gradle JVM system property, `java.home` included, into the test JVM. The OpenJ9 17 JVM then loaded the JDK 21 class library: `JVMCFRE199E bad major version 65.0 of class=java/lang/Object`, exit 1 in 5 s.
- **Fix:** Environment only. `JAVA_HOME` points at `~/.gradle/jdks/adoptium-17-aarch64-hotspot-mac/Contents/Home` (Temurin 17.0.9+9, HotSpot) for every Gradle run from the second slice attempt on. Daemon and test JVM are then one HotSpot JDK 17, the same family as CI. No repository file was changed.
- **Files modified:** none (evidence E-02 and the header record it)
- **Verification:** slice exit 0 with the class reported skipped; full run completed with 63 testcases in the XML
- **Committed in:** `07fc1f31` (the evidence)

**2. [Plan wording] Capture loop kept only the last 3000 lines of the Connect log**
- **Found during:** Task 2
- **Issue:** The Datagen worker is chatty, so the plan's `docker logs --tail 3000` snapshot held no ClickHouse lines.
- **Fix:** While the Connect container was still up, one full `docker logs` was saved to scratch and the error excerpts in E-04 come from it. Nothing in the repository changed.
- **Committed in:** `943b1072`

---

**Total deviations:** 2 (1 blocking environment fix, 1 capture adjustment)
**Impact on plan:** Neither changes a decision. The JVM note replaces the plan's "OpenJ9 against Temurin" difference with "Temurin on both sides".

## Issues Encountered

- Measurements differing from the plan or research (reported, not acted on): the Docker server is 27.4.0 (plan text implied 29.x; the client is 29.8.2); the console `Tests summary:` omits `ExactlyOnceTest` (55 against 63 testcases in the XML); the connector cannot load in the harness image at all, which research ranked only as hypothesis 2.
- None of this changes the basis of a locked decision, but it makes a skip list for these failures meaningless (a nightly with no executing test), which is why Task 3 recommends nothing automatically and leaves the choice to the user.

## Known Stubs

None. This plan wrote only an evidence document.

## Threat Flags

None. No network endpoint, auth path or schema was added; the evidence holds error-line excerpts only and was scanned for token and key patterns (0 hits).

## User Setup Required

None - no external service configuration required.

## Next Phase Readiness

Blocked on Task 3. Options and consequences are in the checkpoint return (accept, amend, fix-harness, token). Under `fix-harness` the orchestrator plans a gap plan (`/gsd-plan-phase 31 --gaps`) that changes the Connect worker image or the Kafka pin question and re-measures (E-06b) before 31-02 starts.

## Self-Check: PASSED

- `31-COMMAND-EVIDENCE.md` exists and holds E-01 to E-05.
- Commits `07fc1f31` and `943b1072` are on `sync/v1.7.0`.
- SLICE-OK, RUN-CAPTURED-OK and MEASUREMENT-RECORD-OK all passed; `git diff --quiet v1.7.0_gg4 HEAD -- src/main` exits 0 and nothing under `src`, `build.gradle.kts`, `gradle` or `.github` changed.

---
*Phase: 31-fork-owned-nightly-integration-tests*
*Completed (tasks 1-2): 2026-10-26; Task 3 pending*
