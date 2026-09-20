---
status: complete
phase: 10-test-harness-reconciliation
source: [10-VERIFICATION.md]
started: 2026-09-20T16:06:09Z
updated: 2026-09-20T17:53:29Z
---

## Current Test

[testing complete]

## Tests

### 1. Stranger-read of the REQUIREMENTS.md HARN correction blockquote
expected: The blockquote at `.planning/REQUIREMENTS.md:104-110` leaves a cold reader with the true picture — four of seven requirements pre-satisfied and closed on re-measured evidence; the server-version gap (Recorded Fact 15) as the phase's real finding; HARN-03 on the re-derive branch; HARN-02 discharged via `MBeanServerUtils`'s rethrow. It must not read as "seven requirements' worth of work".
why_human: Plan 10-03 task 3's own deferred `human_judgment: true` item, held for end-of-phase UAT per `workflow.human_verify_mode=end-of-phase` rather than self-certified by the executor or the verifier. A stranger-read for tone and completeness is a judgment call, not a grep.
result: pass

### 2. HARN-07 sequencing clause — accept "moot on arrival", or record as formally unmet
expected: A considered call, recorded either as accepted or as a correction to REQUIREMENTS.md's HARN-07 note.
why_human: |
  HARN-07 is worded as a *sequencing* guarantee: Testcontainers 2.0.5 verified to start
  a ClickHouse container through `ClickHouseBase` **before** any of the 371 fork tests
  are adapted to the new harness.

  The two halves land differently:
    - The STATE half — TC 2.0.5 demonstrably starts a container through `ClickHouseBase`
      — is robustly true and was re-verified live during verification (container reports
      `clickhouse/clickhouse-server:latest`), plus the `2.0.5` pin in
      `gradle/libs.versions.toml`.
    - The PROCESS half — check first, *then* adapt — cannot be retroactively performed
      once the adaptation has happened. The adaptation ran across Phases 6-10, finishing
      in this phase's own plan 10-01, i.e. after plan 10-03 task 1's evidence-gathering.

  Plan 10-03 recorded the clause as "moot on arrival" on the grounds that TC 2.0.5's
  compatibility with `ClickHouseBase` was already proven by Phase 8/9's green runs, which
  do precede plan 10-01's harness adaptation. The verifier called this defensible but
  genuinely debatable, and declined to mark it FAILED chiefly because Phase 10's own
  documents (ROADMAP note, REQUIREMENTS.md blockquote, MERGE-CHECKLIST fact) disclose it
  plainly rather than hiding it. It is an interpretive call no command can settle.
result: pass

## Summary

total: 2
passed: 2
issues: 0
pending: 0
skipped: 0
blocked: 0

## Gaps

None blocking. Carried forward for context, not as a gate:

- **WR-01** (code review, `ClickHouseBase.java:139-146`) — `tearDown()`'s database-drop
  failure is logged and swallowed. Under the shared container that drop is the only
  isolation mechanism between classes in a JVM fork, so a transient failure strands a
  database and can make a *different, innocent* later class fail its isolation assertion
  with no attributable cause. Explicitly accepted as a cost in `MERGE-CHECKLIST.md`
  Recorded Fact 16; its only detection mechanism today is a clean full-suite run.
