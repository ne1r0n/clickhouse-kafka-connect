---
phase: 31-fork-owned-nightly-integration-tests
plan: "06"
subsystem: testing
tags: [testcontainers, confluent-platform, kafka-connect, java17, integration-harness, nightly]

requires:
  - phase: 31-fork-owned-nightly-integration-tests
    provides: "31-01 measurement (E-03 to E-05) and the user's fix-harness disposition (E-06)"
provides:
  - "integration harness Connect worker on Java 17 (ConfluentPlatform.connectWorkerImage())"
  - "E-17: worker facts, decision rule result, tracer; E-18: whole-task re-measurement with nightly-timeouts"
  - "three NIT-01 anchor rows pinning the Java 17 worker"
affects: [31-02, 31-03, 31-04, 31-05, phase-32-fork-delta]

# Actuals (#2632): chars/4 over the diff of the three plan commits (harness, anchors, evidence)
actuals:
  tokens: 6000
  tasks: 2
  commits: 3
plan_head_before: c7965a7d08d9377bf5e0a0b3463f392d3e516574
plan_head_after: 79cd92720722959fc4be7f3d090458ae6f8466c6

tech-stack:
  added: []
  patterns:
    - "Connect worker built at test time with Testcontainers ImageFromDockerfile: Confluent Java 17 image plus one plugin directory copied from the pinned datagen image"

key-files:
  created: []
  modified:
    - src/integrationTest/java/com/clickhouse/kafka/connect/sink/helper/ConfluentPlatform.java
    - .planning/scripts/fork-anchors.txt
    - .planning/phases/31-fork-owned-nightly-integration-tests/31-COMMAND-EVIDENCE.md

key-decisions:
  - "Option A (PT-14): confluentinc/cp-server-connect at CONFLUENT_VERSION 7.9.6 plus the datagen plugin copied from the pinned datagen image; all four decision-rule facts held, B (amd64-only CP 8.0 datagen image) and C (confluent-hub download at test time) rejected with Docker Hub evidence"
  - "No pinned value, dependency, assertion, wait, connector JSON or src/main file changed"
  - "nightly-timeouts from the cold-equivalent wall (PT-15): W = 36 min, step 75, job 85; supersedes E-05's step 30 job 40 unless the user amends it"

patterns-established:
  - "Raw FROM ... AS stage statement in DockerfileBuilder because from() validates its argument as an image name"

requirements-completed: []

coverage:
  - id: D1
    description: "The integration harness's Connect worker runs Java 17 and loads the fork's connector, its seven SMTs and the datagen plugin"
    requirement: NIT-01
    verification:
      - kind: integration
        ref: "ClickHouseSinkConnectorIntegrationTest#stockGenSingleTaskTest on ClickHouse latest x V2 (E-17)"
        status: pass
      - kind: other
        ref: "connect-plugin-path list against build/confluentArchive: 8 of 8 loadable in cp-server-connect:7.9.6, 0 of 8 in the datagen worker (E-17)"
        status: pass
    human_judgment: false
  - id: D2
    description: "Criterion 1 re-measured: the whole integrationTest task on latest x V2 is green (55 tests, 54 passed, 0 failed, 1 skipped) and E-18 records classes, images, run commit and timeouts"
    requirement: NIT-03
    verification:
      - kind: integration
        ref: "CLICKHOUSE_VERSION=latest CLIENT_VERSION=V2 ./gradlew integrationTest (E-18, run commit c4b409db)"
        status: pass
    human_judgment: false
  - id: D3
    description: "The user's disposition of E-18 (keep the harness change, skip entries if any, the timeouts) recorded as E-06b"
    requirement: NIT-03
    verification: []
    human_judgment: true
    rationale: "Task 3 is a blocking-human decision; the answer is the user's own and is recorded verbatim by the orchestrator, never by the executor"

duration: 61min
completed: 2026-10-26
status: complete
---

# Phase 31 Plan 06: Java 17 harness worker, re-measurement, E-06b preparation Summary

**The integration harness now builds its Connect worker from Confluent's Java 17 `cp-server-connect:7.9.6` plus the pinned datagen plugin, and the whole `integrationTest` task goes from 54 of 54 connector tests failing to 54 of 54 passing on ClickHouse `latest` x client V2; halted at the blocking-human disposition (E-06b).**

## Performance

- **Duration:** about 61 min (the full re-measurement run alone took 29.5 min)
- **Started:** 2026-10-26T15:12Z
- **Completed (halted at Task 3):** 2026-10-26T16:14Z
- **Tasks:** 2 of 3 executed; Task 3 is the blocking-human checkpoint
- **Files modified:** 3 (one test helper, the anchors file, the evidence file)

## Accomplishments

- **Facts first (E-17):** the old worker (`cnfldemos/cp-server-connect-datagen:0.6.4-7.6.0`) runs Zulu Java 11.0.21 and lists 0 of the fork's 8 plugins loadable (8 `UnsupportedClassVersionError`, class file 61 against 55); `confluentinc/cp-server-connect:7.9.6` runs Temurin 17.0.18, has amd64 and arm64 images and lists 8 of 8 loadable; the datagen image holds `confluentinc-kafka-connect-datagen/lib/kafka-connect-datagen-0.6.4.jar`; Docker Hub shows no datagen tag for CP 7.7 to 7.9 and `0.6.7-8.0.0` as amd64 only. All four decision-rule results held, so option A was taken.
- **Harness change:** `ConfluentPlatform.java` gains `CP_SERVER_CONNECT`, `DATAGEN_PLUGIN_DIR`, `DATAGEN_STAGE` and `connectWorkerImage()` (a Testcontainers `ImageFromDockerfile` with a raw `FROM <datagen> AS datagen`, `from(<Connect image>)` and a raw `COPY --from=datagen`). The only removed line is `cp_data_gen = new GenericContainer<>(CP_DATA_GEN)`. `CONFLUENT_VERSION`, `DATAGEN_VERSION`, `CP_DATA_GEN`, the POST assertion, the 5-minute RUNNING wait and the archive copy are untouched; `src/main` is unchanged.
- **Tracer (E-17):** `stockGenSingleTaskTest` passes on `latest` x V2 (1 test, 1 succeeded; sink connector and task RUNNING; two `POST /connectors` 201; 0 `UnsupportedClassVersionError` in the captured worker log).
- **Re-measurement (E-18):** `integrationTest` with no filter: `Tests summary: 55 tests, 54 succeeded, 0 failed, 1 skipped`, BUILD SUCCESSFUL in 29m 26s at run commit `c4b409db87d36d7599e472d38f3a4bc5a118c646`, server `26.10.3.7`. `ClickHouseSinkConnectorIntegrationTest` ran 54 (25 avro, 22 proto, 7 stockGen) with 0 failures; `ExactlyOnceTest` (8) and `ClickHouseCloudTest` (1) were skipped through assumptions. `skip-candidates: 0`.
- **Timeouts (PT-15):** T = 1768 s, U = 94.7 s, P = 363.3 s, W = 36 min, so `nightly-timeouts: step=75 job=85`.
- **Anchors:** three rows on `ConfluentPlatform.java` (mirror 123 -> 126 rows, bad=0); cumulative gate `invariants: provenance_paths=21 anchor_commits=872 anchor_rows=126 marker_commits=931 anchor_exempt=19 result=PASS`.

## Task Commits

1. **Task 1: facts, Java 17 worker, tracer** - `781fd526` (test: harness + anchors), `c4b409db` (docs: E-17)
2. **Task 2: whole-task re-measurement** - `79cd9272` (docs: E-18)
3. **Task 3: disposition of E-18** - not executed: blocking-human checkpoint; E-06b is written by the orchestrator from the user's own answer

**Plan metadata:** this SUMMARY (status `halted`) is committed separately; STATE.md, ROADMAP.md and requirements are intentionally not updated until Task 3 resolves.

## Files Created/Modified

- `src/integrationTest/java/com/clickhouse/kafka/connect/sink/helper/ConfluentPlatform.java` - Java 17 Connect worker built at test time
- `.planning/scripts/fork-anchors.txt` - comment line plus three NIT-01 rows
- `.planning/phases/31-fork-owned-nightly-integration-tests/31-COMMAND-EVIDENCE.md` - E-17 and E-18

## Decisions Made

- Option A over B and C (reasons and Hub lines in E-17).
- The CLASSPATH env line (`monitoring-interceptors-7.2.1.jar`) was left alone: the worker started and every test passed without touching it.
- No D-08 category was needed: nothing failed.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 3 - Blocking] Capture filter matched the host port, not the container port**
- **Found during:** Task 1 (slice, step 6)
- **Issue:** `docker ps --filter publish=8083` matches the HOST published port (measured with a throwaway container), so the first slice run (which passed) captured no worker log, and the verify command requires a worker log.
- **Fix:** scratch script `capture-h.sh` now selects the container whose published-ports column holds `->8083/tcp` and takes worker log and status every 15 s; the slice was re-run (attempt 2 is the record, attempt 1 kept in scratch).
- **Files modified:** none in the repository (scratch tooling under `/tmp`)
- **Verification:** HARNESS-TRACER-OK
- **Committed in:** n/a

**2. [Rule 3 - Blocking] Spotless file-list comparison reads 48 against 47 because of gradlew.bat**
- **Found during:** Task 1 (verify command 3)
- **Issue:** at the plan base the throwaway worktree's `spotlessApply` run also leaves `gradlew.bat` changed (a line-ending rewrite; the main tree has carried the same uncommitted change since 31-01), at the harness commit it did not. The plan's literal `L0 == L1` therefore fails and `SPOTLESS-31-06-OK` was NOT printed by the command as written.
- **Fix:** recomputed the same comparison from the same two lists with `gradlew.bat` excluded: both hold 47 identical files, the formatted import set grows by exactly the two new imports and loses none. Recorded in E-17.
- **Files modified:** none
- **Verification:** recomputation printed its OK line; the plan's own verify, run literally, would still exit 1 on this one file.
- **Committed in:** n/a

### Notes

- The captured worker log of the full run is only the tail of the container log (Docker `json-file` rotation: 534111 lines starting at 16:08:12), so "no `UnsupportedClassVersionError`" for the whole run rests on the 54 passing tests; E-18 says so.
- The generated worker image's id could not be recorded: Testcontainers removes it when the test JVM exits; only its generated tag is in the ps log.

---

**Total deviations:** 2 auto-fixed (both Rule 3, scratch tooling or check mechanics, no repository change)
**Impact on plan:** none on scope; the plan's Spotless check text needs a `gradlew.bat` exclusion if re-run.

## Issues Encountered

- Cumulative anchors gate again took about 17 minutes (as at planning time).
- A mistyped scratch invocation briefly started a label-less capture loop; it was killed and its three scratch files removed.

## Known Stubs

None.

## Threat Flags

None. The plan adds no network endpoint, auth path or dependency; the two Docker Hub reads were anonymous and read-only.

## Checkpoint State: Task 3 (blocking-human, awaiting the user)

Everything the user needs is in E-17 and E-18. The user answers the orchestrator directly with `accept`, `amend: {changes}` or `fix-harness: {tests or worker}`. With `skip-candidates: 0`, `accept` means: keep the harness change, no skip entries (`skip-entries: 0`, no list per D-07), `nightly-timeouts: step=75 job=85` stands. The orchestrator then records E-06b after E-18 and, once the answer is in, flips this SUMMARY to `status: complete` and updates STATE, ROADMAP and requirements.

## Next Phase Readiness

- Plan 31-02 is unblocked once E-06b exists with a numeric `skip-entries:` line; with zero failures it records "no skip list" and E-18's run is the phase-gate integration run named by 31-05.
- Plan 31-04 reads E-18's `nightly-timeouts:` line and should read the CI risk about image footprint (13.83 GB of images against a 14 GB runner SSD) before choosing its capacity probe.
- Phase 32 carries the FORK-DELTA row for the harness's Java 17 Connect worker (harness commit `781fd52683d6adb7f49468ed6eb88374053a9c1c`, CIG-02).

## Self-Check: PASSED

Created/modified files and the three task commits (`781fd526`, `c4b409db`, `79cd9272`) exist on the branch; `src/main` is unchanged against `v1.7.0_gg4`. HARNESS-TRACER-OK, ANCHORS-31-06-OK, RUNH-CAPTURED-OK and REMEASUREMENT-RECORD-OK printed; the Spotless check passed only after the `gradlew.bat` exclusion described under Deviations.

---
*Phase: 31-fork-owned-nightly-integration-tests*
*Completed (halted at Task 3): 2026-10-26*
