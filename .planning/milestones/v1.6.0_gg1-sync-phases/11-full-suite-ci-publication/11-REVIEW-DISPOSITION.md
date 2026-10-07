---
phase: 11
review: 11-REVIEW.md
titles: json
findings:
  - id: WR-01
    severity: warning
    disposition: open
    title: "CI concurrency group keyed on `github.sha` with `cancel-in-progress` cancels legitimate runs and cancels nothing useful"
  - id: WR-02
    severity: warning
    disposition: open
    title: "Pin is unconditional and non-removable, and on V2 it rides on the connect-time ping, so a rejecting profile fails startup with a misleading error"
  - id: WR-03
    severity: warning
    disposition: open
    title: "The common V1 operator case (`custom_settings` that does not mention `network_compression_method`) is not pinned by any test"
  - id: WR-04
    severity: warning
    disposition: open
    title: "The pins do not exercise the production V1 call sites, so a dropped `.options(getDefaultClientOptions())` still passes every test"
  - id: WR-05
    severity: warning
    disposition: open
    title: "`operatorOverrideReachesServerOnV2` can pass vacuously"
  - id: IN-01
    severity: info
    disposition: open
    title: "Stale Javadoc in `ClickHouseVersionDefaultUnitTest` contradicts the workflow after D-09"
  - id: IN-02
    severity: info
    disposition: open
    title: "Operator `clickhouseSettings` cannot override the pin for non-insert requests, and this is undocumented"
  - id: IN-03
    severity: info
    disposition: open
    title: "CI now resolves the floating `latest` tag purely through code defaults, so the failure class that caused D-17 will recur"
  - id: IN-04
    severity: info
    disposition: open
    title: "`verify-merge-invariants.sh` still collapses a `git rev-list` failure into a \"zero commits\" diagnosis"
open: 9
total: 9
recorded: 2026-10-07T12:34:03.269Z
---

# Phase 11: Code Review Disposition

| Finding | Severity | Disposition | Source |
|---------|----------|-------------|--------|
| WR-01 | warning | open | - |
| WR-02 | warning | open | - |
| WR-03 | warning | open | - |
| WR-04 | warning | open | - |
| WR-05 | warning | open | - |
| IN-01 | info | open | - |
| IN-02 | info | open | - |
| IN-03 | info | open | - |
| IN-04 | info | open | - |

Dispositions: `open` (recorded, not yet triaged), `fixed`, `skipped`, `deferred`.
Set `deferred` by hand and put the reason in the Source cell; both are preserved. A `|` in the reason is kept as prose and escaped on the next run.
Re-running the gate keeps every row it can. A row the current review no longer reports is kept and its Source cell flagged, so a finding does not leave this record silently. ONE exception: when a finding id is REUSED by a different finding, the earlier decision cannot keep a row — the id is taken — and it is dropped. A RECORDED decision (anything but `open`) is named on the console when that happens; a row still at `open` is replaced silently, because `open` records no decision to lose.
