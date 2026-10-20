---
status: complete
phase: 22-writer-record-data-exposure
source: [22-VERIFICATION.md]
started: 2026-10-20T00:35:00Z
updated: 2026-10-20T08:02:36.992Z
---

## Current Test

[testing complete]

## Tests

### 1. Confirm the second flipped upstream test, ClickHouseSinkTaskWithSchemaTest.schemaWithFixedStringMismatchTest (ledger L-47)
expected: The user accepts that assertInstanceOf(IllegalArgumentException.class, rootCause) became assertInstanceOf(WithheldMessageException.class, rootCause) plus assertEquals(IllegalArgumentException.class.getName(), withheld.getOriginalClassName()), as an equally strict replacement forced by D-05 (project rule: existing tests are never deleted or weakened).
result: pass

### 2. Run a real Connect worker with a DLQ and a record with an invalid UUID string, then read the task status trace and the __connect.errors.exception.* headers
expected: The task status trace and the exception.stacktrace header contain 'Failed to write column `<col>`', the ClickHouse type and the original exception class. The record value is in neither header. The exception.message header is Processing's queryId text (D-15). These are plan 22-04 assumptions A3 and A4, which are reproduced in tests because connect-runtime is not on the test classpath.
result: pass

### 3. Decide whether the Jackson residual (WR-01) is acceptable for the goal "without ever carrying the record value"
expected: Either accept it as an opt-in (binaryFormatWrtiteJsonAsString), narrowly reachable residual that FORK-DELTA item 6 already discloses (the verifier measured that a Map key leaks in the "through reference chain" suffix), or schedule the one-line fix (exclude JsonProcessingException from the IOException pass-through) plus a sentinel-map-key test. 22-REVIEW-DISPOSITION.md has all six findings `open`.
result: pass

### 4. Decide whether the HARN-08 wording "overrides one level up, in the CapturingClickHouseWriter pattern" is met by a Mockito-free ClickHouseHelperClient subclass (F7 reading)
expected: Accept the F7 reading (operative clauses: no Mockito, OpenJ9 item closed), or ask for a writer-level counting seam, which would need a production change.
result: pass

## Summary

total: 4
passed: 4
issues: 0
pending: 0
skipped: 0
blocked: 0

## Gaps
