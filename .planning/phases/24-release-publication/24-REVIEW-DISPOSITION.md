---
phase: 24
review: 24-REVIEW.md
titles: json
findings:
  - id: WR-01
    severity: warning
    disposition: fixed
    title: "The `size` defect is stated without the route that was measured, and the note contradicts its own qualifier"
  - id: WR-02
    severity: warning
    disposition: fixed
    title: "Item 4 says `v1.7.0_gg2` \"never stored a batch of that shape\" for all 14 shapes, and E-11 inserted four"
  - id: IN-01
    severity: info
    disposition: fixed
    title: "The CHANGELOG Bug Fixes bullet states the insert outcome for every `Nullable(Tuple)` with such a name"
  - id: IN-02
    severity: info
    disposition: skipped
    title: "FORK-DELTA describes the release as published and `pre-v1.7.0_gg3` as existing before plan 24-07 makes it so"
  - id: IN-03
    severity: info
    disposition: deferred
    title: "Planning identifiers in the new live test's Javadoc and display names"
  - id: WR-03
    severity: warning
    disposition: fixed
    title: "Item 3 says \"every root that is not `Nullable`\" is identical on both releases, and four of the 14 differing roots are not `Nullable`"
  - id: WR-04
    severity: warning
    disposition: fixed
    title: "\"Every insert into such a table fails\" is stated for all `size` shapes, and E-15 inserted four of 13 and the same documents say a shorter row might parse"
  - id: WR-05
    severity: warning
    disposition: fixed
    title: "Item 4 sends `exactlyOnce=false` connectors to the token for a rollback, and E-12 measured that the token does not save them for the differing shape"
  - id: WR-06
    severity: warning
    disposition: fixed
    title: "Item 7 makes the branch rollback step 1 and the in-flight check step 2, while item 4 says \"do not roll back yet\" and has the operator restart the `v1.7.0_gg3` jar"
  - id: WR-07
    severity: warning
    disposition: fixed
    title: "The state-table query has room for two topics only, and its \"Empty result\" sentence says it is safe to roll back"
  - id: IN-04
    severity: info
    disposition: fixed
    title: "The known-limitation sentence lists a backslash, a slash and a tab, and the table below it also requires the escaped form for a line break"
  - id: IN-05
    severity: info
    disposition: fixed
    title: "The CHANGELOG Build and CI bullet says the tests no longer mock \"the ClickHouse client\""
open: 0
total: 12
recorded: 2026-10-20T18:42:11.122Z
---

# Phase 24: Code Review Disposition

| Finding | Severity | Disposition | Source |
|---------|----------|-------------|--------|
| WR-01 | warning | fixed | 0c08e663 (Phase 24; the note, the CHANGELOG and FORK-DELTA item 10 name the measured route; D-02 phrase kept) |
| WR-02 | warning | fixed | cd40803c (Phase 24; item 4 upgrade sentence limited to the four inserted shapes) |
| IN-01 | info | fixed | 6ee4a22a (Phase 24; CHANGELOG bullet names the shapes whose insert was measured) |
| IN-02 | info | skipped | by design: the gg2 text used the same tense before its tag existed; the document is part of the tree the tag names; plan 24-06 states it in the approval bundle |
| IN-03 | info | deferred | 24-MILESTONE-CLOSE-RECORD.md (carried: planning identifiers in the new test's comments; fork-wide convention; same point as 22-REVIEW IN-05) |
| WR-03 | warning | fixed | 7987a093 (quick 261020-vcr; item 3 describes the 30 identical tables as those whose type holds no Nullable(Tuple(...)) at any depth, E-10) |
| WR-04 | warning | fixed | 12e9790d (quick 261020-vcr; item 2, the CHANGELOG Known Issues bullet and FORK-DELTA item 10 name the four inserted shapes and keep the wrong-value case open, E-15; released CHANGELOG text changed for this finding) |
| WR-05 | warning | fixed | 83cbaf94 (quick 261020-vcr; item 4 tells an exactlyOnce=false connector to stop cleanly before the redeploy, E-12) |
| WR-06 | warning | fixed | 40699434 (quick 261020-vcr; item 7 works through item 4 before the branch commit and keeps the v1.7.0_gg3 archive, item 4 says do not redeploy v1.7.0_gg2 yet; order proven by the rollback demo on a dry-run merge, E-22) |
| WR-07 | warning | fixed | 72773c7c (quick 261020-vcr; item 4 asks for one entry per topic and names the database, Empty result speaks of the listed topics; query text unchanged, E-14) |
| IN-04 | info | fixed | 6df28936 (quick 261020-vcr; item 3 lead sentence lists the line break, E-16) |
| IN-05 | info | fixed | 1ba3fe96 (quick 261020-vcr; CHANGELOG Build and CI bullet names ClickHouseHelperClient; released CHANGELOG text changed for this finding) |

Dispositions: `open` (recorded, not yet triaged), `fixed`, `skipped`, `deferred`.
Set `deferred` by hand and put the reason in the Source cell; both are preserved. A `|` in the reason is kept as prose and escaped on the next run.
Re-running the gate keeps every row it can. A row the current review no longer reports is kept and its Source cell flagged, so a finding does not leave this record silently. ONE exception: when a finding id is REUSED by a different finding, the earlier decision cannot keep a row — the id is taken — and it is dropped. A RECORDED decision (anything but `open`) is named on the console when that happens; a row still at `open` is replaced silently, because `open` records no decision to lose.
