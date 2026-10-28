---
status: complete
phase: 31-fork-owned-nightly-integration-tests
source: 31-01-SUMMARY.md, 31-02-SUMMARY.md, 31-03-SUMMARY.md, 31-04-SUMMARY.md, 31-05-SUMMARY.md, 31-06-SUMMARY.md
started: 2026-10-27T13:02:10Z
updated: 2026-10-27T14:52:11.089Z
---

## Current Test

[testing complete]

## Tests

### 1. The user's disposition of each measured failure, the timeouts and the upstream explanation is recorded verbatim as E-06
expected: E-06 in 31-COMMAND-EVIDENCE.md records your disposition of each measured failure, the timeouts and the upstream explanation, in your own words (verbatim).
result: pass

### 2. The nightly actually runs green on a GitHub-hosted runner within the timeouts
expected: The fork-owned nightly workflow ran green on a GitHub-hosted runner within the 75/85-minute timeouts (a real dispatched run, not just local guards).
result: pass

### 3. The user's NIT-03 decision is recorded verbatim and the planning documents agree with it
expected: Your NIT-03 decision is recorded verbatim in the planning docs, and REQUIREMENTS/ROADMAP agree with it.
result: pass

### 4. The user's disposition of E-18 (keep the harness change, skip entries if any, the timeouts) recorded as E-06b
expected: E-06b records your disposition of the E-18 re-measurement (keep the harness change, skip entries if any, timeouts) verbatim.
result: pass

### 5. Criterion-1 measurement record E-01 to E-05 exists and is committed before any workflow file: slice, cold full run, images, classes, failures with proposed causes, timeouts, upstream verdict
expected: Criterion-1 measurement record E-01 to E-05 exists and is committed before any workflow file: slice, cold full run, images, classes, failures with proposed causes, timeouts, upstream verdict
result: pass
source: automated
coverage_id: 31-01/D1

### 6. D-07 branch decision recorded: the last numeric disposition (E-06b) has zero skip entries, so E-07 states that no skip list is needed and nothing outside the evidence changes
expected: D-07 branch decision recorded: the last numeric disposition (E-06b) has zero skip entries, so E-07 states that no skip list is needed and nothing outside the evidence changes
result: pass
source: automated
coverage_id: 31-02/D1

### 7. Tasks 2 and 3 are no-ops in the no-entry branch: no guard test, no live re-run, src/main identical to v1.7.0_gg4
expected: Tasks 2 and 3 are no-ops in the no-entry branch: no guard test, no live re-run, src/main identical to v1.7.0_gg4
result: pass
source: automated
coverage_id: 31-02/D2

### 8. Upstream's integrationTests.yaml is pinned byte for byte (SHA-256 and length 4812) by a Docker-free testUnit guard, seen red on a one-byte edit of the real file and green after the restore
expected: Upstream's integrationTests.yaml is pinned byte for byte (SHA-256 and length 4812) by a Docker-free testUnit guard, seen red on a one-byte edit of the real file and green after the restore
result: pass
source: automated
coverage_id: 31-03/D1

### 9. The byte rule rejects seven derived edits (first, middle and last byte flipped, one byte appended, final newline removed, LF to CRLF, empty) and its message carries the update procedure
expected: The byte rule rejects seven derived edits (first, middle and last byte flipped, one byte appended, final newline removed, LF to CRLF, empty) and its message carries the update procedure
result: pass
source: automated
coverage_id: 31-03/D2

### 10. GitHub reports workflow 384641581 as disabled_manually with no run created after 2026-10-16T10:00:00Z
expected: GitHub reports workflow 384641581 as disabled_manually with no run created after 2026-10-16T10:00:00Z
result: pass
source: automated
coverage_id: 31-03/D3

### 11. The fork-owned nightly workflow runs the whole integrationTest task on ClickHouse latest x client V2 on ubuntu-24.04 with read-only permissions, non-persisted checkout credentials, a 75-minute Gradle-step timeout inside an 85-minute job timeout, no secret, no cloud reference and no test filter
expected: The fork-owned nightly workflow runs the whole integrationTest task on ClickHouse latest x client V2 on ubuntu-24.04 with read-only permissions, non-persisted checkout credentials, a 75-minute Gradle-step timeout inside an 85-minute job timeout, no secret, no cloud reference and no test filter
result: pass
source: automated
coverage_id: 31-04/D1

### 12. The job is gated so a manual dispatch always starts and a scheduled run starts only once NIGHTLY_ENABLED is true; triggers are exactly schedule and an input-free workflow_dispatch
expected: The job is gated so a manual dispatch always starts and a scheduled run starts only once NIGHTLY_ENABLED is true; triggers are exactly schedule and an input-free workflow_dispatch
result: pass
source: automated
coverage_id: 31-04/D2

### 13. Failed test names reach the step summary and one escaped, capped error annotation, and the summary and both uploads run under always()
expected: Failed test names reach the step summary and one escaped, capped error annotation, and the summary and both uploads run under always()
result: pass
source: automated
coverage_id: 31-04/D3

### 14. Phase 31 gate on the final tree and the GitHub state of both workflows are recorded
expected: Phase 31 gate on the final tree and the GitHub state of both workflows are recorded
result: pass
source: automated
coverage_id: 31-05/D1

### 15. The integration harness's Connect worker runs Java 17 and loads the fork's connector, its seven SMTs and the datagen plugin
expected: The integration harness's Connect worker runs Java 17 and loads the fork's connector, its seven SMTs and the datagen plugin
result: pass
source: automated
coverage_id: 31-06/D1

### 16. Criterion 1 re-measured: the whole integrationTest task on latest x V2 is green (55 tests, 54 passed, 0 failed, 1 skipped) and E-18 records classes, images, run commit and timeouts
expected: Criterion 1 re-measured: the whole integrationTest task on latest x V2 is green (55 tests, 54 passed, 0 failed, 1 skipped) and E-18 records classes, images, run commit and timeouts
result: pass
source: automated
coverage_id: 31-06/D2

## Summary

total: 16
passed: 16
issues: 0
pending: 0
skipped: 0

## Gaps

[none yet]
