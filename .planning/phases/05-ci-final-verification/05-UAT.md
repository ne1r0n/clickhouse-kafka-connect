---
status: testing
phase: 05-ci-final-verification
source: [05-VERIFICATION.md]
started: 2026-09-18T07:26:54Z
updated: 2026-09-18T07:26:54Z
---

## Current Test

number: 1
name: GitHub renders the Coverage Summary markdown on the CI run's Summary tab
expected: |
  Opening https://github.com/ne1r0n/clickhouse-kafka-connect/actions/runs/35317272594 and looking
  at the run's Summary tab (the landing page of the run, below the job list) shows:
  1. A "Coverage Summary" heading rendered as formatted markdown, NOT as raw pipe-delimited text.
  2. Under it, a table with a "Full suite" row and a "Fork-changed classes" row, each carrying
     Instruction / Line / Branch percentages.
  3. Below that, a "Fork-changed classes" heading and a 15-row per-class table, one row per class,
     each with a line and a branch figure. A few cells legitimately read "n/a".
awaiting: user response

## Tests

### 1. GitHub renders the Coverage Summary markdown on the CI run's Summary tab
expected: A rendered "Coverage Summary" heading appears as formatted markdown (not raw pipe-delimited text), followed by a table with a "Full suite" row and a "Fork-changed classes" row (each with Instruction/Line/Branch percentages), followed by a "Fork-changed classes" heading and a 15-row per-class table (a few cells legitimately reading "n/a").
why_human: GitHub exposes no unauthenticated REST endpoint for a rendered job-summary page (confirmed during planning: `/logs` requires admin rights, `/zip` returns 401). The step's SUCCESS conclusion and the script's CONTENT are both machine-proven; only GitHub's actual rendering of that markdown has never been seen by a human. Roadmap success criterion 3 requires the workflow "writes a coverage summary to the job summary" — the write and its content are proven, the rendering is not.
url: https://github.com/ne1r0n/clickhouse-kafka-connect/actions/runs/35317272594
result: [pending]

### 2. Judgment-tier prohibitions were not violated
expected: Agree (or disagree) with the verifier's assessment that all four judgment-tier prohibitions declared in 05-01-PLAN.md and 05-02-PLAN.md hold:
  1. CI execution was NOT narrowed to obtain a green badge — no substitution of the Docker-free unit-only task for the full suite, no exclusion of the ClickHouse-tagged classes, no test-name filter, no `continue-on-error`.
  2. The published coverage summary came from the same run's own real test execution — not a stale, cached, partial or locally-copied report.
  3. No existing test was disabled, deleted, annotated-away or narrowed in scope to make CI pass.
  4. The CI run reported on was located by the pushed commit's own SHA (`head_sha` + `path` filter, exactly one match), NOT by "most recent run on the branch".
why_human: These carry `verification: judgment` in the plan frontmatter. The verifier's assessment is a non-authoritative LLM-judge verdict, not an authoritative pass. Supporting evidence is in 05-VERIFICATION.md's Prohibitions section; the verifier found no violations.
result: [pending]

## Summary

total: 2
passed: 0
issues: 0
pending: 2
skipped: 0
blocked: 0

## Gaps
