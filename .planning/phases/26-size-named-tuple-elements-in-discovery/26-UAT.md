---
status: complete
phase: 26-size-named-tuple-elements-in-discovery
source: 26-01-SUMMARY.md, 26-02-SUMMARY.md, 26-03-SUMMARY.md, 26-04-SUMMARY.md, 26-05-SUMMARY.md
started: 2026-10-22T08:33:49Z
updated: 2026-10-22T11:47:45.668Z
---

## Current Test

[testing complete]

## Tests

### 1. Client V1 parity of the DESCRIBE snapshots (assumption A6)
expected: Evidence E-10 in 26-COMMAND-EVIDENCE.md records one manual CLIENT_VERSION=V1 run of ClickHouseHelperClientForkLiveTest, TableTest and DescribeFixtureDriftLiveTest: 113 tests, 0 failures, and the drift class printed `DESCRIBE_FIXTURE_DRIFT client=V1 server=26.9.19.9`, so all 47 committed DESCRIBE snapshots were checked through describeTableV1 and matched the V2-captured text. Reproducing with `CLIENT_VERSION=V1 env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain test --tests '*ClickHouseHelperClientForkLiveTest' --tests '*TableTest' --tests '*DescribeFixtureDriftLiveTest' --rerun` is green and prints the `client=V1` line.
result: pass
coverage_id: 26-02 D5
rationale: The full suite exercises V1 only through TableTest.extractCommentV1 and the V1 rows of ClickHouseWriterTest; plan 26-04 records the manual CLIENT_VERSION=V1 run of the drift class (reason: human_judgment)

### 2. DESC-09 completes in plan 26-04: the live round-trip is pinned both ways
expected: Plan 26-03 deferred the live round-trip and the V1 run to plan 26-04. 26-04-SUMMARY.md and evidence E-08/E-09 show TupleSizeNamedElementRoundTripLiveTest passing 46 of 46 catalog shapes on the fix tree (each shape stored through the real ClickHouseSinkTask on the RowBinary route with client V2, read back equal, zero WARN on the helper and DeclaredNullability loggers), and the same unchanged class failing exactly the 25 affected keys of E-02 on a worktree of the pre-fix commit 9c186046 (24 puts rejected with rows=0, r01 stored as size 1667391751 / hex 78797A). Reading those two entries, DESC-09 is complete and nothing from 26-03's deferral is still open.
result: pass
coverage_id: 26-03 D6
rationale: Plan 26-04 records the round-trip on the pre-fix and fix trees and the CLIENT_VERSION=V1 run; DESC-09 completes there (reason: human_judgment)

### 3. E-11 rollout facts are adequate operator guidance for the Phase 29 release note
expected: Evidence E-11 in 26-COMMAND-EVIDENCE.md lists five rollout facts for Phase 29, each tagged with its provenance. Facts (1) upgrade from gg3 needs no clean stop, (3) a short write that parsed on gg3 is an unrepairable data tail, (4) the exact new WARN text and that it never fires for the 46 round-tripped tables, and (5) the fix changes the bytes of a retried batch, are each marked measured and cite their evidence entry (E-06, E-08, E-09, 24-COMMAND-EVIDENCE E-15, 23-EVIDENCE R-01). Fact (2), that a rollback to gg3 needs a clean stop first because of block deduplication, is marked assumed (research A5) and says Phase 29 must measure it or word it as a precaution, never cite it as measured. The wording is clear enough for the Phase 29 operator note without re-deriving anything.
result: pass
coverage_id: 26-04 D4
rationale: The facts are operator guidance; whether the wording is adequate for the Phase 29 note is a judgment call, and the assumed fact is not testable here (reason: human_judgment)

### 4. captureDescribeFixtures re-captures the raw DESCRIBE rows and the production Column snapshot of 47 fixtures reproducibly (two runs byte-identical)
expected: captureDescribeFixtures re-captures the raw DESCRIBE rows and the production Column snapshot of 47 fixtures reproducibly (two runs byte-identical)
result: pass
source: automated
coverage_id: 26-01 D1

### 5. Spike record E-02: double row CONFIRMED from the committed rows, classifier rule stated before any src/main change, measured affected set equals the WARN oracle
expected: Spike record E-02: double row CONFIRMED from the committed rows, classifier rule stated before any src/main change, measured affected set equals the WARN oracle
result: pass
source: automated
coverage_id: 26-01 D2

### 6. Live drift premise: every committed DESCRIBE line and Column snapshot equals the live server's, per fixture, with the key in the failure message
expected: Live drift premise: every committed DESCRIBE line and Column snapshot equals the live server's, per fixture, with the key in the failure message
result: pass
source: automated
coverage_id: 26-01 D3

### 7. Shared DESCRIBE reader: NullableTupleMatrixLiveTest stays 33 tests with 0 failures through NullableRoundTrip.describeRows delegation
expected: Shared DESCRIBE reader: NullableTupleMatrixLiveTest stays 33 tests with 0 failures through NullableRoundTrip.describeRows delegation
result: pass
source: automated
coverage_id: 26-01 D4

### 8. describeTableV1 and describeTableV2 share one fork-owned DescribeTableBuilder; behaviour is unchanged on all 47 committed fixtures
expected: describeTableV1 and describeTableV2 share one fork-owned DescribeTableBuilder; behaviour is unchanged on all 47 committed fixtures
result: pass
source: automated
coverage_id: 26-02 D1

### 9. The 999.4 pin builds its Table through DescribeTableBuilder and still asserts the defect (size-named element not a Column, one COLS-14 WARN, apply() true)
expected: The 999.4 pin builds its Table through DescribeTableBuilder and still asserts the defect (size-named element not a Column, one COLS-14 WARN, apply() true)
result: pass
source: automated
coverage_id: 26-02 D2

### 10. DeclaredNullability.elementKey is the one public Tuple element key; named, two unnamed and a backtick-named element are pinned
expected: DeclaredNullability.elementKey is the one public Tuple element key; named, two unnamed and a backtick-named element are pinned
result: pass
source: automated
coverage_id: 26-02 D3

### 11. The extraction commit is green on the full suite against ClickHouse latest with client V2 (1543 tests, 0 failed, 18 named skips) and the tail anchors gate passes with 74 rows
expected: The extraction commit is green on the full suite against ClickHouse latest with client V2 (1543 tests, 0 failed, 18 named skips) and the tail anchors gate passes with 74 rows
result: pass
source: automated
coverage_id: 26-02 D4

### 12. A declared Tuple element named size, size0 or size<digits> becomes a Column at both drop sites; for all 47 fixtures the tupleFields of every Tuple equal the declared element keys in declared order
expected: A declared Tuple element named size, size0 or size<digits> becomes a Column at both drop sites; for all 47 fixtures the tupleFields of every Tuple equal the declared element keys in declared order
result: pass
source: automated
coverage_id: 26-03 D1

### 13. Every structural row stays out: no .null Column, no Map keys row in a Tuple, nested size1 offsets rows registered once and in no Tuple, nothing below a JSON root; a structural twin never enters the skip set
expected: Every structural row stays out: no .null Column, no Map keys row in a Tuple, nested size1 offsets rows registered once and in no Tuple, nothing below a JSON root; a structural twin never enters the skip set
result: pass
source: automated
coverage_id: 26-03 D2

### 14. An unreadable parent fails closed to gg3's behaviour with one metadata-only WARN (no throwable, no message, no value)
expected: An unreadable parent fails closed to gg3's behaviour with one metadata-only WARN (no throwable, no message, no value)
result: pass
source: automated
coverage_id: 26-03 D3

### 15. The D-04 pin is flipped to D-10 (a size-named Tuple element becomes a Column and discovery logs no WARN), seen red on the extraction tree first
expected: The D-04 pin is flipped to D-10 (a size-named Tuple element becomes a Column and discovery logs no WARN), seen red on the extraction tree first
result: pass
source: automated
coverage_id: 26-03 D4

### 16. The fix commit carries the code, the tests, the 25 re-captured snapshots and every DESC-10 document and anchor edit, and the full suite ran on exactly its tree
expected: The fix commit carries the code, the tests, the 25 re-captured snapshots and every DESC-10 document and anchor edit, and the full suite ran on exactly its tree
result: pass
source: automated
coverage_id: 26-03 D5

### 17. All 46 round-trippable catalog shapes store through the real sink task on the default RowBinary route with client V2 and read back equal, with zero WARN on the helper and DeclaredNullability loggers
expected: All 46 round-trippable catalog shapes store through the real sink task on the default RowBinary route with client V2 and read back equal (whole value text), with zero WARN on the helper and DeclaredNullability loggers across the sink task's discovery and a second describeTable
result: pass
source: automated
coverage_id: 26-04 D1

### 18. The crafted R-01 value reads back correctly on the fix tree, and the same class on the refactor(26-02) worktree fails exactly the 25 affected keys
expected: The crafted R-01 value reads back tupleElement(c,'size') = 7 and hex(tupleElement(c,'v')) = 6162630378797A on the fix tree, and the same class on the refactor(26-02) worktree fails exactly the 25 affected keys (24 puts rejected with rows=0, r01 stored as 1667391751 / 78797A)
result: pass
source: automated
coverage_id: 26-04 D2

### 19. One manual CLIENT_VERSION=V1 run of ClickHouseHelperClientForkLiveTest, TableTest and DescribeFixtureDriftLiveTest passed, and the drift class printed client=V1
expected: One manual CLIENT_VERSION=V1 run of ClickHouseHelperClientForkLiveTest, TableTest and DescribeFixtureDriftLiveTest passed, and the drift class printed client=V1 so all 47 snapshots went through describeTableV1
result: pass
source: automated
coverage_id: 26-04 D3

### 20. FORK-DELTA Known costs item 10 gains one sentence citing E-08 to E-10, and the shouldSkipDescribeRow row names TupleSizeNamedElementRoundTripLiveTest
expected: FORK-DELTA Known costs item 10 gains one sentence citing E-08 to E-10, and the shouldSkipDescribeRow row names TupleSizeNamedElementRoundTripLiveTest
result: pass
source: automated
coverage_id: 26-04 D5

### 21. Cache-busted testUnit is green with 1126 tests
expected: Cache-busted testUnit is green with 1126 tests
result: pass
source: automated
coverage_id: 26-05 D1

### 22. Full suite on latest x V2: 1709 tests in 97 XML files, 0 failures, 0 errors, the 18 named skips, exact class counts, DESC-05 and DESC-02 live rows green, one server version
expected: Full suite on latest x V2: 1709 tests in 97 XML files, 0 failures, 0 errors, the 18 named skips, exact class counts, DESC-05 and DESC-02 live rows green, one server version
result: pass
source: automated
coverage_id: 26-05 D2

### 23. Anchors invariant at every phase commit (anchor_rows=77, result=PASS) and the self-test proves the gate can still fail
expected: Anchors invariant at every phase commit (anchor_rows=77, result=PASS) and the self-test proves the gate can still fail (no failures)
result: pass
source: automated
coverage_id: 26-05 D3

### 24. Spotless baseline-equal and nothing pushed (origin/custom_build still 69cd680b)
expected: Spotless baseline-equal and nothing pushed (origin/custom_build still 69cd680b)
result: pass
source: automated
coverage_id: 26-05 D4

## Summary

total: 24
passed: 24
issues: 0
pending: 0
skipped: 0
blocked: 0

## Gaps

[none yet]
