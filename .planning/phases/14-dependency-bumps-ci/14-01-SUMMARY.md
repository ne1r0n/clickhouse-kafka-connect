---
phase: 14-dependency-bumps-ci
plan: "01"
subsystem: ci-testing
tags: [github-actions, lz4, client-v2, testcontainers, clickhouse]

requires:
  - phase: 12-sync-merge
    provides: "catalog lz4Java 1.10.4, Recorded Facts 25-26 (ZSTD default on 26.9), ClickHouseBase live-test pattern"
provides:
  - "tests.yaml triggers on branch pushes only, so a tag push cannot cancel a branch run"
  - "Report test fork count step (log, step summary, notice annotation) mirroring build.gradle.kts"
  - "Compile integration-test sources step before Run tests"
  - "Lz4RoundTripLiveTest: live LZ4 round trip on both legs, green on the pre-bump lz4-java"
  - "Recorded Facts 31 (CI-08 trigger note, CI readability) and 32 (BLD-10 evidence)"
affects: [14-02, 14-03, 14-04, 15]

actuals:
  tokens: 14000
  tasks: 2
  commits: 4

plan_head_before: 3c9aafded6bf321f664184c6b66ae98fb71367a2
plan_head_after: 6a615aaa7bd6894f7fc544a32a9ddfa6e0905e07

tech-stack:
  added: []
  patterns:
    - "Test-owned client-v2 with compression on both legs, independent of ClickHouseHelperClient"
    - "Request-leg proof through system.query_log ProfileEvents CompressedReadBufferBlocks"

key-files:
  created:
    - src/test/java/com/clickhouse/kafka/connect/sink/db/helper/Lz4RoundTripLiveTest.java
  modified:
    - .github/workflows/tests.yaml
    - .planning/MERGE-CHECKLIST-v1.7.0.md

key-decisions:
  - "Read DateTime64 back with getZonedDateTime(...).toInstant() because client-v2 0.9.6 getInstant() throws ClassCastException for a timezone-bearing DateTime64"
  - "Response leg proven by a one-off uncommitted zstd probe that fails in ClickHouseLZ4InputStream with Invalid LZ4 magic byte"

requirements-completed: [CI-08, CI-09, CI-10, BLD-10]

coverage:
  - id: D1
    description: "tests.yaml runs on branch pushes only (push with branches '**', no tags key); concurrency group unchanged"
    requirement: CI-08
    verification:
      - kind: other
        ref: "ruby YAML structural check printing WORKFLOW-OK"
        status: pass
    human_judgment: true
    rationale: "That '**' matches sync/v1.7.0 on GitHub is only confirmed by the first pushed run in plan 14-04"
  - id: D2
    description: "Report test fork count step prints nproc and derived maxParallelForks to log, step summary and a notice annotation"
    requirement: CI-09
    verification:
      - kind: other
        ref: "stub-nproc run for nproc 1, 3, 4 and empty CLICKHOUSE_CLUSTER_NAME printing FORKCOUNT-OK"
        status: pass
    human_judgment: true
    rationale: "The real runner nproc is only observable in the pushed run (plan 14-04)"
  - id: D3
    description: "Compile integration-test sources step runs compileIntegrationTestJava before Run tests"
    requirement: CI-10
    verification:
      - kind: other
        ref: "./gradlew compileIntegrationTestJava --rerun BUILD SUCCESSFUL; ruby step-order check"
        status: pass
    human_judgment: false
  - id: D4
    description: "Live LZ4 round trip on client-v2 0.9.6 with typed read-back, green on the unchanged catalog lz4-java"
    requirement: BLD-10
    verification:
      - kind: integration
        ref: "src/test/java/com/clickhouse/kafka/connect/sink/db/helper/Lz4RoundTripLiveTest.java (2 tests, 0 skipped, 0 failed)"
        status: pass
    human_judgment: false

duration: 25min
completed: 2026-10-09
status: complete
---

# Phase 14 Plan 01: CI branch-only trigger, fork-count and compile steps, live LZ4 round trip Summary

**tests.yaml now runs on branch pushes only with a fork-count notice and a separate integration-source compile step, and a live client-v2 test proves LZ4 on both legs against lz4-java 1.10.4 on ClickHouse 26.9.14.10.**

## Performance

- **Duration:** about 25 min
- **Completed:** 2026-10-09
- **Tasks:** 2
- **Files modified:** 3 (one new)

## Accomplishments

- CI-08: `on: push: branches: ['**']` with no `tags` key. The `concurrency` block and `timeout-minutes: 45` (anchor count 1) are untouched. The header comment explains the tag-cancels-branch incident on c61d90ce.
- CI-09: "Report test fork count" step with the build.gradle.kts formula (`${CLICKHOUSE_CLUSTER_NAME+set}`, integer division, minimum 1) emitting log, `$GITHUB_STEP_SUMMARY` and `::notice title=Test fork count::`.
- CI-10: "Compile integration-test sources" runs `./gradlew compileIntegrationTestJava` as its own step before "Run tests".
- BLD-10 first half: `Lz4RoundTripLiveTest` (jar-origin pin plus a 2000-row, 4881366 byte round trip) is green on `lz4Java = "1.10.4"` and names no version.
- Recorded Facts 31 and 32 added to `.planning/MERGE-CHECKLIST-v1.7.0.md`.

## Task Commits

1. Task 1 (tracer): `80f4d6b7` ci(14-01) workflow edit; `2c0837f9` docs(14-01) Recorded Fact 31 (D-11a)
2. Task 2: `19bd55ce` test(14-01) Lz4RoundTripLiveTest; `6a615aaa` docs(14-01) Recorded Fact 32

## Evidence

- Ruby structural check: `WORKFLOW-OK`. Stub-nproc run: `FORKCOUNT-OK` (nproc 1 -> 1, 3 -> 1, 4 -> 2, empty CLICKHOUSE_CLUSTER_NAME with nproc 8 -> 1).
- `compileIntegrationTestJava --rerun`: BUILD SUCCESSFUL. `testUnit --rerun`: 431 tests, 429 succeeded, 0 failed, 2 skipped.
- Merge-invariant script: `result=PASS` (anchor_rows=40).
- `Lz4RoundTripLiveTest` class XML: tests=2, skipped=0, failures=0, errors=0.
- Jar location: `.../at.yawk.lz4/lz4-java/1.10.4/.../lz4-java-1.10.4.jar`.
- `CompressedReadBufferBlocks` for the insert: 75 (request leg).
- zstd probe (response leg): `ClientException: Invalid LZ4 magic byte: '-112'` from `ClickHouseLZ4InputStream.refill`. The probe was reverted and never committed.
- Server version: `26.9.14.10` (image label `com.clickhouse.build.version` the same; Recorded Fact 25 used `26.9.12.8`).

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 1 - Bug] getInstant fails on DateTime64 with timezone in client-v2 0.9.6**
- **Found during:** Task 2
- **Issue:** `record.getInstant("ts")` threw `ClassCastException: ZonedDateTime incompatible with LocalDateTime` for `DateTime64(3, 'UTC')`. This is a client quirk, unrelated to LZ4.
- **Fix:** read back with `record.getZonedDateTime("ts").toInstant()`, still a typed millisecond-exact comparison. A code comment names `getInstant` and the reason.
- **Files modified:** `Lz4RoundTripLiveTest.java`
- **Commit:** `19bd55ce`

**Total deviations:** 1 auto-fixed (1 bug in test read-back). **Impact:** none on scope; the assertion is equally strict.

## Issues Encountered

None. The PIN-OK command's `git status --porcelain` clause cannot be empty while the orchestrator's uncommitted `.planning/STATE.md` and `.planning/state.json` edits are present. Its other clauses (no version string in the test, catalog at 1.10.4, no Phase 14 commit touching `src/main`) passed, and the probe edit was confirmed reverted.

## Known Stubs

None.

## Threat Flags

None. No new network endpoint, auth path or trust-boundary file access beyond the plan's threat model.

## Self-Check: PASSED

- Files exist: `.github/workflows/tests.yaml`, `Lz4RoundTripLiveTest.java`, `.planning/MERGE-CHECKLIST-v1.7.0.md`.
- Commits `80f4d6b7`, `2c0837f9`, `19bd55ce`, `6a615aaa` are on `sync/v1.7.0`; `git rev-list --count` from the recorded base gives 4.
- Acceptance greps: all required strings present in the test; `@Tag` count 0; one CI-08 fact and one BLD-10 fact; anchor count 1.
