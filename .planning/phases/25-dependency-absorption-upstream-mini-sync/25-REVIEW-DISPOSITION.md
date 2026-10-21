---
phase: 25
review: 25-REVIEW.md
titles: json
findings:
  - id: WR-01
    severity: warning
    disposition: open
    title: "The archive-step guard accepts a `createConfluentArchive` call that does not verify anything"
  - id: WR-02
    severity: warning
    disposition: open
    title: "`ConnectApiReferencesUnitTest` silently drops inherited-member references it cannot resolve, and resolves them on the test classpath"
  - id: WR-03
    severity: warning
    disposition: open
    title: "The reference derivation only reads Class/Field/Method constants, so a Kafka type used only in a descriptor, signature or annotation is never checked"
  - id: IN-01
    severity: info
    disposition: open
    title: "A stale version directory under `build/confluentArchive` turns into an unexplained \"found 2\" failure"
  - id: IN-02
    severity: info
    disposition: open
    title: "`ConfluentArchiveCheck` coverage edges"
  - id: IN-03
    severity: info
    disposition: open
    title: "Brittle exact-text assertions in the new guards"
  - id: IN-04
    severity: info
    disposition: open
    title: "Build and repository hygiene carried by the merges"
open: 7
total: 7
recorded: 2026-10-21T17:40:47.687Z
---

# Phase 25: Code Review Disposition

| Finding | Severity | Disposition | Source |
|---------|----------|-------------|--------|
| WR-01 | warning | open | - |
| WR-02 | warning | open | - |
| WR-03 | warning | open | - |
| IN-01 | info | open | - |
| IN-02 | info | open | - |
| IN-03 | info | open | - |
| IN-04 | info | open | - |

Dispositions: `open` (recorded, not yet triaged), `fixed`, `skipped`, `deferred`.
Set `deferred` by hand and put the reason in the Source cell; both are preserved. A `|` in the reason is kept as prose and escaped on the next run.
Re-running the gate keeps every row it can. A row the current review no longer reports is kept and its Source cell flagged, so a finding does not leave this record silently. ONE exception: when a finding id is REUSED by a different finding, the earlier decision cannot keep a row — the id is taken — and it is dropped. A RECORDED decision (anything but `open`) is named on the console when that happens; a row still at `open` is replaced silently, because `open` records no decision to lose.
