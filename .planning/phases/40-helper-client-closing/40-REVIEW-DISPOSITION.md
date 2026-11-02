---
phase: 40
review: 40-REVIEW.md
titles: json
findings:
  - id: WR-01
    severity: warning
    disposition: open
    title: "A throwing per-batch helper `close()` now fails the insert batch"
  - id: WR-02
    severity: warning
    disposition: open
    title: "`KeeperStateProvider.close()` can throw, and `ProxySinkTask.stop()` then loses the primary exception"
  - id: WR-03
    severity: warning
    disposition: open
    title: "`ClickHouseWriterTest.runWithWriter` leaks its `seedClient`"
  - id: IN-01
    severity: info
    disposition: open
    title: "`ownedClient` duplicates `chc`, and the \"injected client stays with its owner\" clause has no behavioural pin"
  - id: IN-02
    severity: info
    disposition: open
    title: "After `stop()` a late `updateMapping` fails with an uninformative message"
  - id: IN-03
    severity: info
    disposition: open
    title: "`ProxySinkTaskStopUnitTest` does not discriminate every branch of the nested `finally` chain"
open: 6
total: 6
recorded: 2026-11-02T07:31:26.103Z
---

# Phase 40: Code Review Disposition

| Finding | Severity | Disposition | Source |
|---------|----------|-------------|--------|
| WR-01 | warning | open | - |
| WR-02 | warning | open | - |
| WR-03 | warning | open | - |
| IN-01 | info | open | - |
| IN-02 | info | open | - |
| IN-03 | info | open | - |

Dispositions: `open` (recorded, not yet triaged), `fixed`, `skipped`, `deferred`.
Set `deferred` by hand and put the reason in the Source cell; both are preserved. A `|` in the reason is kept as prose and escaped on the next run.
Re-running the gate keeps every row it can. A row the current review no longer reports is kept and its Source cell flagged, so a finding does not leave this record silently. ONE exception: when a finding id is REUSED by a different finding, the earlier decision cannot keep a row — the id is taken — and it is dropped. A RECORDED decision (anything but `open`) is named on the console when that happens; a row still at `open` is replaced silently, because `open` records no decision to lose.
