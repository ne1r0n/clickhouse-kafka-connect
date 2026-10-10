---
status: complete
phase: 12-upstream-v1-7-0-merge-reconcile
source: [12-VERIFICATION.md]
started: 2026-10-07T20:22:34Z
updated: 2026-10-08T12:51:22.315Z
---

## Current Test

[testing complete]

## Tests

### 1. Backstop truth (12-01): line provenance is set-membership and ignores order and multiplicity; any mis-ordered or duplicated resolution in a conflicted file is caught by the D-03 compile + testUnit gate at M
expected: Reviewer accepts that the set-membership provenance check plus the recorded D-03 gate at M is adequate for ordering and duplication errors in the 5 conflicted files, or asks for an ordered-hunk review
why_human: Non-inferable design claim. I confirmed set-membership holds (independent re-check: 0 authored lines across 21 changed paths) and that the tree compiles and tests green at HEAD, but nothing in the repo exercises a mis-ordered or duplicated resolution to show the gate would catch it.
result: pass

### 2. Backstop truth (12-02): each byte pin builds its own writer and stream, so the pins are independent of test execution order
expected: No order dependence between the Docker-free MAP/TUPLE/VARIANT pins in NestedNullableCompositeWriteTest
why_human: I read all six test methods and each builds its own ClickHouseWriter and ByteArrayOutputStream, but no randomized-order run exists to confirm it.
result: pass

### 3. Backstop truth (12-02): with one capability rule left, LZ4 selection no longer depends on the registration order of configurations.all versus configurations.configureEach
expected: Reviewer accepts the single remaining rule (build.gradle.kts:65) as order-independent
why_human: Absence of a second rule is verified (one withCapability line, dependencyInsight shows one at.yawk.lz4:lz4-java 1.10.4 on runtimeClasspath and clickhouseDependencies). Order independence is a Gradle-semantics claim with no test.
result: pass

### 4. Backstop truth (12-05): pin outcomes do not depend on test execution order (full suite ran once)
expected: Reviewer accepts one green full-suite run as sufficient, or schedules a second ordering
why_human: Only one full-suite ordering was observed (691 tests, 0 failures); Phase 15 requires two clean runs.
result: pass

### 5. WRT-09 interpretation (judgment): REQUIREMENTS says CR-02 pin tests that asserted pre-#820 early-null bytes are re-pinned. In the tree, 0 existing assertions changed (git diff 54e63648 HEAD on NestedNullableCompositeWriteTest has zero removed lines): the three CR-02 pins sit on nullable columns and stay correct, and 3 new pins were added (non-nullable RowBinary {0x00}, non-nullable WithDefaults {0x00,0x00}, nullable through doWriteCol {0x01}), each with a 2026-10-07 rationale comment.
expected: Reviewer confirms 'nullable unchanged, non-nullable per #820, rationale next to each' is satisfied by adding pins rather than editing any, or asks for a different shape
why_human: Literal reading of 're-pinned' (edit existing) differs from the delivered shape (additive). The 12-02 plan prohibition forbids editing existing assertions, so the two readings conflict and the call belongs to the user.
result: pass

### 6. D-17 FORK-DELTA wording (judgment prohibition): docs/FORK-DELTA.md line 77 says compress=false alone is sufficient on both clients, the lz4 setting alone is sufficient on V2 when an operator re-enables compression, and at least one control is required on V2; V1 stripped-both was not observed. Compare against Recorded Fact 26 in .planning/MERGE-CHECKLIST-v1.7.0.md.
expected: Every clause is derivable from the Fact 26 table and no control is claimed load-bearing beyond what was observed
why_human: I compared the sentence to the table and found each clause supported, but the plan marks this prohibition 'judgment' and the probe class was throwaway, so the observation cannot be re-run here.
result: pass

### 7. SYNC-10 compile-only reading (judgment): the #800 Avro-union and cluster buildFromClause tests live in the integrationTest source set. They are byte-identical to refs/tags/v1.7.0 (git diff --quiet passes for all 11 unmodified upstream test-tree paths) and compileIntegrationTestJava passes, but they were never executed.
expected: Reviewer accepts 'compile-only, named in FORK-DELTA skip inventory' as the 'skip for a named reason' SYNC-10 allows
why_human: REQUIREMENTS lets these skip for a named reason; whether compile-only plus byte identity counts is a user call (12-RESEARCH Open Question 3).
result: pass

### 8. EOS-01 mutation evidence (could not reproduce): the restart-read test was recorded failing 4 of 6 against the pre-v1.7.0 summary-count read (Recorded Fact 28, 12-04-SUMMARY).
expected: Optional: with Docker/colima running, temporarily revert getStateRecord in a throwaway worktree to the v1.6.0 read and confirm KeeperStateProviderRestartLiveTest goes red
why_human: colima is not running in this environment so I could not re-run the mutation. I did verify the restart test is non-vacuous by reading it (fresh provider per read, distinct min 10 / max 19, beyond-int offsets, swapped-constructor-order detectable), the v1.6.0 to v1.7.0 diff of KeeperStateProvider shows exactly the summary-count to streamed-count change, and the live test passed in the 691-test XML.
result: pass

## Summary

total: 8
passed: 8
issues: 0
pending: 0
skipped: 0
blocked: 0

## Gaps
