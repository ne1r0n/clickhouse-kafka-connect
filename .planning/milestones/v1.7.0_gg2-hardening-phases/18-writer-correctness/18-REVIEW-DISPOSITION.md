---
phase: 18
review: 18-REVIEW.md
titles: json
findings:
  - id: WR-01
    severity: warning
    disposition: open
    title: "T-13-19 is documented as \"closed\" while the rethrown exception still carries record values into logs"
  - id: WR-02
    severity: warning
    disposition: open
    title: "`ClickHouseWriterNullMessageUnitTest` inline-mocks `ClickHouseHelperClient`, the pattern this phase found to crash the test JVM"
  - id: WR-03
    severity: warning
    disposition: open
    title: "The emoji literal makes `valueLengthCountsUtf16CodeUnits` depend on the compiler's source encoding"
  - id: WR-04
    severity: warning
    disposition: open
    title: "The \"clients V1 and V2 accept an empty insert body\" claim has no test, and the all-empty case is not tried with request compression"
  - id: IN-01
    severity: info
    disposition: open
    title: "The retry test reuses the same `Record` instances, so it cannot catch non-determinism in conversion"
  - id: IN-02
    severity: info
    disposition: open
    title: "Only the DateTime branch of the unsupported-conversion message is pinned"
  - id: IN-03
    severity: info
    disposition: open
    title: "The bypass-route live test skips the `ts` read-back without saying why"
  - id: IN-04
    severity: info
    disposition: open
    title: "Duplicated test constants and placeholder-looking display names"
  - id: IN-05
    severity: info
    disposition: open
    title: "Skipped empty CDC records are still counted as inserted rows in JMX statistics"
open: 9
total: 9
recorded: 2026-10-12T14:42:08.935Z
---

# Phase 18: Code Review Disposition

| Finding | Severity | Disposition | Source |
|---------|----------|-------------|--------|
| WR-01 | warning | open | - |
| WR-02 | warning | open | - |
| WR-03 | warning | open | - |
| WR-04 | warning | open | - |
| IN-01 | info | open | - |
| IN-02 | info | open | - |
| IN-03 | info | open | - |
| IN-04 | info | open | - |
| IN-05 | info | open | - |

Dispositions: `open` (recorded, not yet triaged), `fixed`, `skipped`, `deferred`.
Set `deferred` by hand and put the reason in the Source cell; both are preserved. A `|` in the reason is kept as prose and escaped on the next run.
Re-running the gate keeps every row it can. A row the current review no longer reports is kept and its Source cell flagged, so a finding does not leave this record silently. ONE exception: when a finding id is REUSED by a different finding, the earlier decision cannot keep a row — the id is taken — and it is dropped. A RECORDED decision (anything but `open`) is named on the console when that happens; a row still at `open` is replaced silently, because `open` records no decision to lose.
