---
phase: 25
review: 25-REVIEW.md
titles: json
findings:
  - id: WR-01
    severity: warning
    disposition: deferred
    title: "The archive-step guard accepts a `createConfluentArchive` call that does not verify anything"
  - id: WR-02
    severity: warning
    disposition: deferred
    title: "`ConnectApiReferencesUnitTest` silently drops inherited-member references it cannot resolve, and resolves them on the test classpath"
  - id: WR-03
    severity: warning
    disposition: deferred
    title: "The reference derivation only reads Class/Field/Method constants, so a Kafka type used only in a descriptor, signature or annotation is never checked"
  - id: IN-01
    severity: info
    disposition: deferred
    title: "A stale version directory under `build/confluentArchive` turns into an unexplained \"found 2\" failure"
  - id: IN-02
    severity: info
    disposition: deferred
    title: "`ConfluentArchiveCheck` coverage edges"
  - id: IN-03
    severity: info
    disposition: deferred
    title: "Brittle exact-text assertions in the new guards"
  - id: IN-04
    severity: info
    disposition: deferred
    title: "Build and repository hygiene carried by the merges"
open: 0
total: 7
recorded: 2026-10-23T18:45:37.538Z
---

# Phase 25: Code Review Disposition

| Finding | Severity | Disposition | Source |
|---------|----------|-------------|--------|
| WR-01 | warning | deferred | 29-MILESTONE-CLOSE-RECORD.md (carried: the pin guard accepts an archive call that verifies nothing; test edit) |
| WR-02 | warning | deferred | 29-MILESTONE-CLOSE-RECORD.md (carried: the reference test skips unresolved inherited members; test edit) |
| WR-03 | warning | deferred | 29-MILESTONE-CLOSE-RECORD.md (carried: the reference derivation reads Class, Field and Method constants only; test edit) |
| IN-01 | info | deferred | 29-MILESTONE-CLOSE-RECORD.md (carried: a stale version directory gives an unexplained found 2 failure; build edit) |
| IN-02 | info | deferred | 29-MILESTONE-CLOSE-RECORD.md (carried: archive check coverage edges; test edit) |
| IN-03 | info | deferred | 29-MILESTONE-CLOSE-RECORD.md (carried: brittle exact-text assertions; test edit) |
| IN-04 | info | deferred | 29-MILESTONE-CLOSE-RECORD.md (carried: build hygiene points; the Gradle 7.4.2 half is documented in FORK-DELTA, the rest is build-side) |

Dispositions: `open` (recorded, not yet triaged), `fixed`, `skipped`, `deferred`.
Set `deferred` by hand and put the reason in the Source cell; both are preserved. A `|` in the reason is kept as prose and escaped on the next run.
Re-running the gate keeps every row it can. A row the current review no longer reports is kept and its Source cell flagged, so a finding does not leave this record silently. ONE exception: when a finding id is REUSED by a different finding, the earlier decision cannot keep a row — the id is taken — and it is dropped. A RECORDED decision (anything but `open`) is named on the console when that happens; a row still at `open` is replaced silently, because `open` records no decision to lose.
