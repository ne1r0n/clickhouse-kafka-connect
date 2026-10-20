---
status: testing
phase: 24-release-publication
source: [24-VERIFICATION.md]
started: 2026-10-20T18:45:24Z
updated: 2026-10-20T18:45:24Z
---

## Current Test

number: 1
name: Decide what to do with the five Round 3 Warnings (WR-03..WR-07) and two Info items (IN-04, IN-05) that 24-REVIEW-DISPOSITION.md still records as `open`
expected: |
  Either accept them as a follow-up docs-only commit on custom_build after the milestone (tag v1.7.0_gg3 and M do not move), or fix them now. WR-03 (item 3 says 'every root that is not Nullable' is identical, and 4 of 14 differing roots are not Nullable), WR-05 (item 4 excludes exactlyOnce=false connectors from a rollback hazard that E-12 measured on exactlyOnce=false), and WR-06 (item 7 commits the rollback, which removes the note, before the in-flight check that item 4 says to run first) are statements or procedures an operator could act on wrongly.
awaiting: user response

## Tests

### 1. Decide what to do with the five Round 3 Warnings (WR-03..WR-07) and two Info items (IN-04, IN-05) that 24-REVIEW-DISPOSITION.md still records as `open`
expected: Either accept them as a follow-up docs-only commit on custom_build after the milestone (tag v1.7.0_gg3 and M do not move), or fix them now. WR-03, WR-05 and WR-06 are statements or procedures an operator could act on wrongly; WR-04 and WR-07 are an overclaim and an ambiguous query instruction (a connector with three or more topics could read a false "safe").
result: [pending]

### 2. Confirm that the two approvals were your own, direct answers
expected: You answered `push` (rehearsal, E-62) and `publish` (release, E-70) yourself, directly to the orchestrator, not through a relayed message.
result: [pending]

## Summary

total: 2
passed: 0
issues: 0
pending: 2
skipped: 0
blocked: 0

## Gaps
