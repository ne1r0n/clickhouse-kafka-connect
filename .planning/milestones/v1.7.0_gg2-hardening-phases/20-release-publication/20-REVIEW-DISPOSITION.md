---
phase: 20
review: 20-REVIEW.md
titles: json
findings:
  - id: WR-01
    severity: warning
    disposition: fixed
    title: "CHANGELOG calls `lombok.config` new and the artifact byte-identical; neither is what was measured"
  - id: WR-02
    severity: warning
    disposition: fixed
    title: "FORK-DELTA says a serialization change never duplicates rows on a retry; item 1 of the rollout note says that holds only with a deduplication window"
  - id: IN-01
    severity: info
    disposition: fixed
    title: "The sentence about the hardening ledgers now points at the wrong range"
  - id: IN-02
    severity: info
    disposition: fixed
    title: "The new Tuple WARN is logged once per column, not once per table"
  - id: IN-03
    severity: info
    disposition: fixed
    title: "The Known costs preamble contradicts its own list"
  - id: IN-04
    severity: info
    disposition: fixed
    title: "Item 1 says \"measured\" for a gg1 attempt that the test simulates"
open: 0
total: 6
recorded: 2026-10-14T13:47:04.000Z
---

# Phase 20: Code Review Disposition

| Finding | Severity | Disposition | Source |
|---------|----------|-------------|--------|
| WR-01 | warning | fixed | 54474bf5 (docs only, Phase 20; CHANGELOG now says lombok.config gains a line and only the generated class is unchanged; evidence E-50) |
| WR-02 | warning | fixed | 32a06bcf (docs only, Phase 20; FORK-DELTA limits the dedup-token statement to tables with a deduplication window; evidence E-50) |
| IN-01 | info | fixed | e6682d9a (docs only, Phase 20; the L-01 to L-22 sentence is back in the gg1 paragraph and gg2 names L-23 to L-41; evidence E-50) |
| IN-02 | info | fixed | a15c3887 (docs only, Phase 20; CHANGELOG and rollout item 2 say once per affected column each time the table is described; evidence E-50) |
| IN-03 | info | fixed | aef8d39d (docs only, Phase 20; the Known costs preamble says each item is closed or knowingly open; evidence E-50) |
| IN-04 | info | fixed | 02adff03 (docs only, Phase 20; item 1 says the earlier gg1 attempt is simulated; evidence E-50) |

Dispositions: `open` (recorded, not yet triaged), `fixed`, `skipped`, `deferred`.
Set `deferred` by hand and put the reason in the Source cell; both are preserved. A `|` in the reason is kept as prose and escaped on the next run.
Re-running the gate keeps every row it can. A row the current review no longer reports is kept and its Source cell flagged, so a finding does not leave this record silently. ONE exception: when a finding id is REUSED by a different finding, the earlier decision cannot keep a row — the id is taken — and it is dropped. A RECORDED decision (anything but `open`) is named on the console when that happens; a row still at `open` is replaced silently, because `open` records no decision to lose.

## Mandated confirmations (D-07)

Verdicts of `20-REVIEW.md` after the fix commits (round 2):

1. The dedup claims match the test and E-01/E-03: confirmed (item 1 says the earlier gg1 attempt is simulated; FORK-DELTA limits the statement to tables with a window).
2. Every quoted query and command has matching evidence, and the 999.1 queries were proven on gg1-written rows: confirmed (ROLLOUT-T3-OK; E-10 to E-19, E-30).
3. No TRUNCATE or unscoped DELETE instruction for the shared KeeperMap table: confirmed (ROLLOUT-FINAL-OK, DOCS-FROZEN-OK).
4. The rollback item uses read-tree with pre-v1.7.0_gg2, and E-31 proves it: confirmed.
5. The released gg1 CHANGELOG section and gg1 note are unchanged: confirmed (CHANGELOG-GG2-OK, DOCS-FROZEN-OK).
6. No secret, hostname, record value or production identifier: confirmed.
7. The note, CHANGELOG and FORK-DELTA agree with each other and with the quoted source texts: confirmed after WR-01, WR-02, IN-01, IN-02 and IN-03 were fixed.
8. The new test follows the AGENTS.md test rules: confirmed (`DedupTokenRetryLiveTest` is unchanged since plan 20-01).
