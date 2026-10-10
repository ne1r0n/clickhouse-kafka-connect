---
phase: 15
review: 15-REVIEW.md
titles: json
findings:
  - id: IN-01
    severity: info
    disposition: open
    title: "The WARN fires on routes where the defect cannot occur"
  - id: IN-02
    severity: info
    disposition: open
    title: "The V1 describe hook is not exercised by the V2-only target run"
  - id: IN-03
    severity: info
    disposition: open
    title: "The warned-key set never evicts and its key is delimiter-ambiguous"
  - id: IN-04
    severity: info
    disposition: open
    title: "The WARN wording is not pinned by a test"
  - id: IN-05
    severity: info
    disposition: open
    title: "New constants sit in the middle of the class"
  - id: IN-06
    severity: info
    disposition: open
    title: "Edge-value pins cover the mantissa-less scientific form only"
  - id: WR-01
    severity: warning
    disposition: open
    title: "Item 1 and item 2 treat a shared KeeperMap table as if it were per-connector"
  - id: WR-02
    severity: warning
    disposition: open
    title: "The `date_time_input_format` default is stated as `best_effort` without a version"
  - id: WR-03
    severity: warning
    disposition: open
    title: "The `version=*` wildcard advice is wrong for the regex-based consumers the same sentence names"
  - id: IN-07
    severity: info
    disposition: open
    title: "Item 2's recovery steps are not qualified by `exactlyOnce`, and the closing sentence is logically loose"
  - id: IN-08
    severity: info
    disposition: open
    title: "Item 8 omits the re-merge consequence of `git revert -m 1`"
  - id: IN-09
    severity: info
    disposition: open
    title: "FORK-DELTA still speaks of the `v1.7.0_gg1` tag as future, and the skip-count command does not print the totals it claims"
  - id: IN-10
    severity: info
    disposition: open
    title: "Planning-internal identifiers appear in operator-facing text without a path, and the item 6 self-check pattern is broader than its explanation"
open: 13
total: 13
recorded: 2026-10-10T12:29:38.081Z
---

# Phase 15: Code Review Disposition

| Finding | Severity | Disposition | Source |
|---------|----------|-------------|--------|
| IN-01 | info | open | - |
| IN-02 | info | open | - |
| IN-03 | info | open | - |
| IN-04 | info | open | - |
| IN-05 | info | open | - |
| IN-06 | info | open | - |
| WR-01 | warning | open | - |
| WR-02 | warning | open | - |
| WR-03 | warning | open | - |
| IN-07 | info | open | - |
| IN-08 | info | open | - |
| IN-09 | info | open | - |
| IN-10 | info | open | - |

Dispositions: `open` (recorded, not yet triaged), `fixed`, `skipped`, `deferred`.
Set `deferred` by hand and put the reason in the Source cell; both are preserved. A `|` in the reason is kept as prose and escaped on the next run.
Re-running the gate keeps every row it can. A row the current review no longer reports is kept and its Source cell flagged, so a finding does not leave this record silently. ONE exception: when a finding id is REUSED by a different finding, the earlier decision cannot keep a row — the id is taken — and it is dropped. A RECORDED decision (anything but `open`) is named on the console when that happens; a row still at `open` is replaced silently, because `open` records no decision to lose.
