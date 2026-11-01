---
phase: 31-fork-owned-nightly-integration-tests
plan: "03"
subsystem: testing
tags: [ci, github-actions, sha256, byte-guard, junit5, nightly]

requires:
  - phase: 31-fork-owned-nightly-integration-tests
    provides: "31-02 branch decision (no skip list), E-07; the evidence file and testUnit conventions from 31-01"
provides:
  - "UpstreamIntegrationWorkflowBytesGuardUnitTest: Docker-free SHA-256 plus length pin of upstream's integrationTests.yaml (NIT-02)"
  - "E-10: the guard red on a one-byte edit of the real file and green after the restore"
  - "E-11: Actions API reads (disabled_manually, 2 runs, 0 after 2026-10-16T10:00:00Z) and the suite and Spotless gates"
affects: [31-04, 31-05, phase-32-fork-delta]

actuals:
  tokens: 3000
  tasks: 2
  commits: 3
plan_head_before: 406371fe585e94a1c5413f4760541d2f3be3b09c
plan_head_after: ab38a34b9462abdf95964eac720f5a917e118de1

tech-stack:
  added: []
  patterns:
    - "Byte-identity guard: Files.readAllBytes plus MessageDigest SHA-256 plus length, never line-based reads, with the sync update procedure in the failure message"
    - "Seeded-red proof on the real file: one-byte in-place edit, red run, git checkout restore, green run, all saved as evidence"

key-files:
  created:
    - src/test/java/com/clickhouse/kafka/connect/ci/UpstreamIntegrationWorkflowBytesGuardUnitTest.java
  modified:
    - .planning/phases/31-fork-owned-nightly-integration-tests/31-COMMAND-EVIDENCE.md

key-decisions:
  - "Hash constants are SHA-256 07dff3d0973ae821a259544632d161a270126ff8054b45adbf64849edd2d5109 and length 4812, remeasured identical at HEAD, v1.7.0, a6f1df83 and upstream/main (git blob 3e9b9be0)"
  - "The one-byte red edit (name: to Name:) keeps the length at 4812, so the red message differs only in the hash; length-changing edits are covered by four of the seven fixtures"
  - "testUnit ran on the default Gradle JVM; the E-02 JAVA_HOME override is only needed for the Testcontainers integrationTest task"

requirements-completed: [NIT-02]

coverage:
  - id: D1
    description: "Upstream's integrationTests.yaml is pinned byte for byte (SHA-256 and length 4812) by a Docker-free testUnit guard, seen red on a one-byte edit of the real file and green after the restore"
    requirement: NIT-02
    verification:
      - kind: unit
        ref: "UpstreamIntegrationWorkflowBytesGuardUnitTest#integrationTestsWorkflowIsByteIdenticalToUpstream (red run bytes-red.xml, green run bytes-green.xml, E-10)"
        status: pass
    human_judgment: false
  - id: D2
    description: "The byte rule rejects seven derived edits (first, middle and last byte flipped, one byte appended, final newline removed, LF to CRLF, empty) and its message carries the update procedure"
    requirement: NIT-02
    verification:
      - kind: unit
        ref: "UpstreamIntegrationWorkflowBytesGuardUnitTest#byteRuleRejectsEveryEditedCopy and #problemNamesExpectedAndFoundValuesAndUpdateProcedure (9 cases, 0 failures)"
        status: pass
    human_judgment: false
  - id: D3
    description: "GitHub reports workflow 384641581 as disabled_manually with no run created after 2026-10-16T10:00:00Z"
    requirement: NIT-02
    verification:
      - kind: other
        ref: "Task 2 verify API-STATE-OK over the saved Actions API bodies (E-11)"
        status: pass
    human_judgment: false

duration: 3min
completed: 2026-10-26
status: complete
---

# Phase 31 Plan 03: Upstream integrationTests.yaml byte guard Summary

**A Docker-free SHA-256 plus length guard pins upstream's integrationTests.yaml byte for byte, was seen red on a one-byte edit of the real file, and the Actions API confirms the workflow is disabled with no run since 2026-10-16.**

## Performance

- **Duration:** 3 min
- **Started:** 2026-10-26T17:22:22Z
- **Completed:** 2026-10-26T17:25:16Z
- **Tasks:** 2
- **Files modified:** 2 (one new test class, the evidence file)

## Accomplishments

- `UpstreamIntegrationWorkflowBytesGuardUnitTest` (9 cases) compares the raw bytes of `.github/workflows/integrationTests.yaml` with SHA-256 `07dff3d0...5109` and length 4812. It extends nothing, so it stays in `testUnit`.
- Red proof on the real file: changing the first byte (`name:` to `Name:`, in place) failed exactly the real-file test with the message `expected 4812 bytes / 07dff3d0... , found 4812 bytes / 346e55ca... ; if an upstream sync changed this file on purpose, confirm the new bytes equal the upstream tag's copy ... and replace EXPECTED_SHA256 and EXPECTED_LENGTH in the same commit`. After `git checkout --` the blob was `3e9b9be0` again and the guard ran 9/9 green.
- Seven derived fixtures are each rejected: first, middle and last byte flipped, one byte appended, final newline removed, LF to CRLF, empty file.
- Actions API (unauthenticated): workflow id 384641581 at `.github/workflows/integrationTests.yaml`, state `disabled_manually`, updated 2026-10-16T09:38:23Z; two runs in total (2026-10-15 and 2026-10-16, both `schedule`, both failed); `total_count` 0 after 2026-10-16T10:00:00Z.
- Gates: full `testUnit` 1308 tests, 1306 succeeded, 0 failed, 2 skipped; the guard class is unchanged by `spotlessApply`; `src/main` identical to `v1.7.0_gg4`.

## Task Commits

1. **Task 1: byte guard end to end (tracer)** - `3d0e3d6f` (test: the guard class), `563f7ff5` (docs: E-10)
2. **Task 2: API state, suite and Spotless gates, record** - `ab38a34b` (docs: E-11)

**Plan metadata:** the docs(31-03) completion commit that follows this SUMMARY.

## Files Created/Modified

- `src/test/java/com/clickhouse/kafka/connect/ci/UpstreamIntegrationWorkflowBytesGuardUnitTest.java` - the NIT-02 byte guard, constants `UPSTREAM_WORKFLOW`, `EXPECTED_SHA256`, `EXPECTED_LENGTH`, `UPDATE_PROCEDURE`, statics `sha256Hex`, `byteIdentityProblem`, `editedCopies`.
- `.planning/phases/31-fork-owned-nightly-integration-tests/31-COMMAND-EVIDENCE.md` - appended E-10 and E-11.

## Decisions Made

- Constants taken from values remeasured at four refs; all identical, so no stop condition applied.
- The tracer gate ran in the default mode: the tracer's verify is automated-only, so after the red/green proof it continued to Task 2 without a checkpoint.
- The Spotless pre-check ran on the new file in a throwaway worktree before the guard commit, so the commit needed no formatting follow-up.

## Deviations from Plan

None - plan executed exactly as written.

## Issues Encountered

- `spotlessApply` in the throwaway worktree rewrote 48 other tracked files (the inherited violations noted in memory, plus the worktree's own `gradlew.bat`). None is the guard class and the main tree was untouched, so this is recorded in E-11 and not acted on (out of scope).

## Known Stubs

None.

## Threat Flags

None - the plan adds only a read-only test and evidence; the one working-tree edit of the upstream workflow was restored before any commit, and `git log v1.7.0_gg4..HEAD -- .github/workflows/integrationTests.yaml` is empty.

## User Setup Required

None - no external service configuration required.

## Next Phase Readiness

- 31-04 and 31-05 can proceed; 31-05 and Phase 32 re-read the Actions API state using the three URLs in E-11.
- Windows checkouts that convert line endings would turn the guard red (T-31-11, accepted; `.gitattributes` is upstream-shared and untouched).

## Self-Check: PASSED

- Guard class exists; commits `3d0e3d6f`, `563f7ff5`, `ab38a34b` are ancestors of HEAD; E-10 and E-11 headings present in the evidence file.
- Task verifies printed BYTE-GUARD-RED-GREEN-OK, API-STATE-OK and NIT02-GATES-OK; the upstream workflow blob is `3e9b9be0` at HEAD and in every commit of this plan.

---
*Phase: 31-fork-owned-nightly-integration-tests*
*Completed: 2026-10-26*
