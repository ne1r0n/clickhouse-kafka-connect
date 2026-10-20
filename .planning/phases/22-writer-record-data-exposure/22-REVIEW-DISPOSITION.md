---
phase: 22
review: 22-REVIEW.md
titles: json
findings:
  - id: WR-01
    severity: warning
    disposition: deferred
    title: "The blanket `IOException` pass-through lets Jackson's `JsonMappingException` carry record text unsanitized"
  - id: IN-01
    severity: info
    disposition: deferred
    title: "An unknown Enum label is reported as `java.lang.NullPointerException`"
  - id: IN-02
    severity: info
    disposition: deferred
    title: "The fixed `No content in enum` text drops the column name, and the exact-`RuntimeException` pass-through delivers it as it is"
  - id: IN-03
    severity: info
    disposition: deferred
    title: "The metadata-derivation code is duplicated between the `ClassCastException` branch and the new helper"
  - id: IN-04
    severity: info
    disposition: deferred
    title: "`schemaWithFixedStringMismatchTest` passes vacuously when `put` does not throw"
  - id: IN-05
    severity: info
    disposition: deferred
    title: "Dead counter and planning-ID noise"
open: 0
total: 6
recorded: 2026-10-19T21:09:06.603Z
---

# Phase 22: Code Review Disposition

| Finding | Severity | Disposition | Source |
|---------|----------|-------------|--------|
| WR-01 | warning | deferred | 24-MILESTONE-CLOSE-RECORD.md (carried: accepted residual on the opt-in JSON-as-string route, measured by JsonAsStringKeyResidualLiveTest (E-03); src/main edit) |
| IN-01 | info | deferred | 24-MILESTONE-CLOSE-RECORD.md (carried: unknown Enum label reported as NullPointerException; src/main edit) |
| IN-02 | info | deferred | 24-MILESTONE-CLOSE-RECORD.md (carried: No content in enum text lacks the column name; src/main edit) |
| IN-03 | info | deferred | 24-MILESTONE-CLOSE-RECORD.md (carried: metadata derivation duplicated in ClickHouseWriter; src/main edit) |
| IN-04 | info | deferred | 24-MILESTONE-CLOSE-RECORD.md (carried: schemaWithFixedStringMismatchTest can pass vacuously; existing test not edited in the release phase) |
| IN-05 | info | deferred | 24-MILESTONE-CLOSE-RECORD.md (carried: dead test counter and planning IDs in comments; no behaviour effect) |

Dispositions: `open` (recorded, not yet triaged), `fixed`, `skipped`, `deferred`.
Set `deferred` by hand and put the reason in the Source cell; both are preserved. A `|` in the reason is kept as prose and escaped on the next run.
Re-running the gate keeps every row it can. A row the current review no longer reports is kept and its Source cell flagged, so a finding does not leave this record silently. ONE exception: when a finding id is REUSED by a different finding, the earlier decision cannot keep a row — the id is taken — and it is dropped. A RECORDED decision (anything but `open`) is named on the console when that happens; a row still at `open` is replaced silently, because `open` records no decision to lose.
