---
status: testing
phase: 06-merge-groundwork-build-migration
source: [06-VERIFICATION.md]
started: 2026-09-18T19:20:17Z
updated: 2026-09-18T19:20:17Z
---

## Current Test

number: 1
name: MERGE-CHECKLIST Section 1 owning-phase and adjudicating-test assignments are correct, not merely non-empty
expected: |
  No row is mis-assigned to a phase that will not actually resolve it; no fork behaviour is
  orphaned between two phases; every named fork behaviour has a real adjudicating
  test/requirement ID attached.
awaiting: user response

## Tests

### 1. MERGE-CHECKLIST Section 1 owning-phase and adjudicating-test assignments are correct, not merely non-empty

Read `.planning/MERGE-CHECKLIST.md` Section 1's `owning phase`, `fork behaviour at risk`, and
`adjudicating test` columns end-to-end for all 35 still-`pending` rows (plus Section 2's 8 rows).
Confirm each is assigned to the phase that will genuinely resolve it, and that each of the five
silently-vanishing fork behaviours appears against at least one row with a named adjudicating test:

1. JMX `connector=<name>` segment
2. Un-forcing of `async_insert`
3. UTF-8 `describeTable` readers
4. `targetTableFilter` + the describe sub-column skip
5. `ClickHouseBase`'s `@Tag` / shared-container / MBean-sweep contract

expected: No row is mis-assigned to a phase that will not actually resolve it; no fork behaviour is orphaned between two phases; every named fork behaviour has a real adjudicating test/requirement ID attached.
why_human: This is a judgment call about future work distribution and behavioural completeness requiring domain knowledge of what each of Phases 7-10 will actually touch. No automated check can verify that an owning-phase assignment is *correct* — only that the column is *non-empty*. `06-03-PLAN.md`'s task 3 verify block flags this exact check as `<human-check>`, deferred to end-of-phase per the project's `human_verify_mode: end-of-phase` convention.
result: [pending]

## Summary

total: 1
passed: 0
issues: 0
pending: 1
skipped: 0
blocked: 0

## Gaps

None from automated verification — 18/18 must-haves verified independently against the repository
(git refs, the 40-path D-02 byte-identity invariant, a fresh regeneration of `conflicted-paths.txt`,
live `./gradlew --version` / `help` / `dependencies`, and the `build.gradle.kts` pattern audit).

Carried forward from code review (`06-REVIEW.md`), not a Phase 6 blocker:

- **CR-01 (Critical)** — `.planning/scripts/verify-merge-invariants.sh:44` treats a `git grep`
  error (exit ≥2) identically to "no match", with stderr suppressed, so the safety-net script fails
  **open**. The verifier re-ran the full 588-commit range with corrected exit-code branching and
  confirmed the current result is genuinely clean, so SYNC-06 holds for this repository state. The
  defect is latent — fix it before Phase 11 relies on this script unattended.
