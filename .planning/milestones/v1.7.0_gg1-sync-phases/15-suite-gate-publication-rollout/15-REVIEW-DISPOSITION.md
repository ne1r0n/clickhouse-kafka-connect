---
phase: 15
review: 15-REVIEW.md
titles: json
findings:
  - id: IN-01
    severity: info
    disposition: deferred
    title: "The WARN fires on routes where the defect cannot occur"
  - id: IN-02
    severity: info
    disposition: deferred
    title: "The V1 describe hook is not exercised by the V2-only target run"
  - id: IN-03
    severity: info
    disposition: deferred
    title: "The warned-key set never evicts and its key is delimiter-ambiguous"
  - id: IN-04
    severity: info
    disposition: deferred
    title: "The WARN wording is not pinned by a test"
  - id: IN-05
    severity: info
    disposition: deferred
    title: "New constants sit in the middle of the class"
  - id: IN-06
    severity: info
    disposition: deferred
    title: "Edge-value pins cover the mantissa-less scientific form only"
  - id: WR-01
    severity: warning
    disposition: fixed
    title: "Item 1 and item 2 treat a shared KeeperMap table as if it were per-connector"
  - id: WR-02
    severity: warning
    disposition: fixed
    title: "The `date_time_input_format` default is stated as `best_effort` without a version"
  - id: WR-03
    severity: warning
    disposition: fixed
    title: "The `version=*` wildcard advice is wrong for the regex-based consumers the same sentence names"
  - id: IN-07
    severity: info
    disposition: fixed
    title: "Item 2's recovery steps are not qualified by `exactlyOnce`, and the closing sentence is logically loose"
  - id: IN-08
    severity: info
    disposition: fixed
    title: "Item 8 omits the re-merge consequence of `git revert -m 1`"
  - id: IN-09
    severity: info
    disposition: fixed
    title: "FORK-DELTA still speaks of the `v1.7.0_gg1` tag as future, and the skip-count command does not print the totals it claims"
  - id: IN-10
    severity: info
    disposition: fixed
    title: "Planning-internal identifiers appear in operator-facing text without a path, and the item 6 self-check pattern is broader than its explanation"
open: 0
total: 13
recorded: 2026-10-10T12:29:38.081Z
---

# Phase 15: Code Review Disposition

| Finding | Severity | Disposition | Source |
|---------|----------|-------------|--------|
| IN-01 | info | deferred | deferred in 15-02: D-15 locks the WARN's firing condition and wording; route restriction stated in FORK-DELTA Known costs |
| IN-02 | info | deferred | deferred in 15-02: V1 describe hook exercised only under CLIENT_VERSION=V1; suite target is V2 only |
| IN-03 | info | deferred | deferred in 15-02: set is bounded by schema size and the key collision is harmless in practice |
| IN-04 | info | deferred | deferred in 15-02: WARN mechanism is pinned; wording stays a read-by-human deliverable |
| IN-05 | info | deferred | deferred in 15-02: moving constants adds churn against upstream rebasing for no behaviour change |
| IN-06 | info | deferred | deferred in 15-02: mantissa-with-fraction form verified against the server in review, no committed test |
| WR-01 | warning | fixed | 505e4981 (docs only, local sync/v1.7.0; not yet published) |
| WR-02 | warning | fixed | 505e4981 (docs only, local sync/v1.7.0; not yet published) |
| WR-03 | warning | fixed | 505e4981 (docs only, local sync/v1.7.0; not yet published) |
| IN-07 | info | fixed | fad6f7f9 (docs only, Phase 16) |
| IN-08 | info | fixed | fad6f7f9 (docs only, Phase 16) |
| IN-09 | info | fixed | 65582a31 (docs only, Phase 16) |
| IN-10 | info | fixed | fad6f7f9 (rollout), 8c1f0bc1 (CHANGELOG) (docs only, Phase 16) |

Dispositions: `open` (recorded, not yet triaged), `fixed`, `skipped`, `deferred`.
Set `deferred` by hand and put the reason in the Source cell; both are preserved. A `|` in the reason is kept as prose and escaped on the next run.
Re-running the gate keeps every row it can. A row the current review no longer reports is kept and its Source cell flagged, so a finding does not leave this record silently. ONE exception: when a finding id is REUSED by a different finding, the earlier decision cannot keep a row — the id is taken — and it is dropped. A RECORDED decision (anything but `open`) is named on the console when that happens; a row still at `open` is replaced silently, because `open` records no decision to lose.
