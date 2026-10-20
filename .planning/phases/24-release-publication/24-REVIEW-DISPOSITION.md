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
open: 0
total: 5
recorded: 2026-10-20T17:30:00.000Z
---

# Phase 24: Code Review Disposition

| Finding | Severity | Disposition | Source |
|---------|----------|-------------|--------|
| WR-01 | warning | fixed | 0c08e663 (Phase 24; the note, the CHANGELOG and FORK-DELTA item 10 name the measured route; D-02 phrase kept) |
| WR-02 | warning | fixed | cd40803c (Phase 24; item 4 upgrade sentence limited to the four inserted shapes) |
| IN-01 | info | fixed | 6ee4a22a (Phase 24; CHANGELOG bullet names the shapes whose insert was measured) |
| IN-02 | info | skipped | by design: the gg2 text used the same tense before its tag existed; the document is part of the tree the tag names; plan 24-06 states it in the approval bundle |
| IN-03 | info | deferred | 24-MILESTONE-CLOSE-RECORD.md (carried: planning identifiers in the new test's comments; fork-wide convention; same point as 22-REVIEW IN-05) |

Dispositions: `open` (recorded, not yet triaged), `fixed`, `skipped`, `deferred`.
Set `deferred` by hand and put the reason in the Source cell; both are preserved. A `|` in the reason is kept as prose and escaped on the next run.
Re-running the gate keeps every row it can. A row the current review no longer reports is kept and its Source cell flagged, so a finding does not leave this record silently. ONE exception: when a finding id is REUSED by a different finding, the earlier decision cannot keep a row — the id is taken — and it is dropped. A RECORDED decision (anything but `open`) is named on the console when that happens; a row still at `open` is replaced silently, because `open` records no decision to lose.

## Mandated confirmations (20-D-07)

Two review rounds: round 1 over the whole Phase 24 diff from `P24_BASE`, round 2 over the three fix commits. No Critical or Warning remains.

1. The headline and its Map-key exception match E-03, and no nonexistent connector key is told: holds.
2. Every quoted query and command has matching evidence, and the detection queries were proven against look-alikes: holds.
3. No TRUNCATE or unscoped DELETE of the KeeperMap table: holds.
4. The rollback uses `read-tree` with `pre-v1.7.0_gg3`, E-22 proves it, and the artifact wording matches E-04: holds.
5. Released CHANGELOG sections and the gg1 and gg2 notes are unchanged: holds.
6. No secret, host, record value or production identifier: holds.
7. The three documents agree with each other and with the evidence: did not hold in round 1 (WR-01, WR-02); holds after the fixes.
8. The new test `JsonAsStringKeyResidualLiveTest` follows the AGENTS.md test rules: holds (IN-03 is a naming point, carried).
9. The size-element item offers no workaround and the escaped-name limitation has no CHANGELOG line: holds.
