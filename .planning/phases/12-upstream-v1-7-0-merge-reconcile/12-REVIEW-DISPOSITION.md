---
phase: 12
review: 12-REVIEW.md
titles: json
findings:
  - id: WR-01
    severity: warning
    disposition: open
    title: "A null non-nullable Map child (e.g. `Array(Map(...))` element) writes zero bytes when defaults are off, desyncing RowBinary"
  - id: WR-02
    severity: warning
    disposition: open
    title: "The TUPLE null guard is still unconditional, so a non-nullable Tuple child gets a stray null marker (same defect class as WRT-06)"
  - id: WR-03
    severity: warning
    disposition: open
    title: "The compression live and unit tests no longer reproduce the failure they pin, and the documented operator hazard has only a tautological pin"
  - id: IN-01
    severity: info
    disposition: open
    title: "The MAP null guard is unreachable for real ClickHouse schemas, and the docs describe an impossible scenario"
  - id: IN-02
    severity: info
    disposition: open
    title: "Stale and misleading wording in test javadocs and the FORK-DELTA lz4 rows"
  - id: IN-03
    severity: info
    disposition: open
    title: "Planning-process references in code comments"
  - id: IN-04
    severity: info
    disposition: open
    title: "`KeeperStateProviderRestartLiveTest` never closes the providers' clients"
  - id: IN-05
    severity: info
    disposition: open
    title: "Double blank line left where the constants were removed"
open: 8
total: 8
recorded: 2026-10-07T20:14:35.712Z
---

# Phase 12: Code Review Disposition

| Finding | Severity | Disposition | Source |
|---------|----------|-------------|--------|
| WR-01 | warning | open | - |
| WR-02 | warning | open | - |
| WR-03 | warning | open | - |
| IN-01 | info | open | - |
| IN-02 | info | open | - |
| IN-03 | info | open | - |
| IN-04 | info | open | - |
| IN-05 | info | open | - |

Dispositions: `open` (recorded, not yet triaged), `fixed`, `skipped`, `deferred`.
Set `deferred` by hand and put the reason in the Source cell; both are preserved. A `|` in the reason is kept as prose and escaped on the next run.
Re-running the gate keeps every row it can. A row the current review no longer reports is kept and its Source cell flagged, so a finding does not leave this record silently. ONE exception: when a finding id is REUSED by a different finding, the earlier decision cannot keep a row — the id is taken — and it is dropped. A RECORDED decision (anything but `open`) is named on the console when that happens; a row still at `open` is replaced silently, because `open` records no decision to lose.
