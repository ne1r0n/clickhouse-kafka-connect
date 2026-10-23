---
phase: 29
review: 29-REVIEW.md
titles: json
findings:
  - id: CR-01
    severity: critical
    disposition: fixed
    title: "Item 4 tells the operator to rename the producers back before the connector is stopped and drained, so gg4 (or gg3 after redeploy) reads records under the wrong name rule"
  - id: WR-01
    severity: warning
    disposition: fixed
    title: "The in-flight check procedure of item 4 is written for rollback only, yet items 1 and 2 and the upgrade bullets depend on \"stop the connector cleanly (item 4)\""
  - id: WR-02
    severity: warning
    disposition: fixed
    title: "The rollback bullet states that gg3's short bytes are \"rejected before the token\", which is false for the shape that gg3 stored shifted"
  - id: WR-03
    severity: warning
    disposition: fixed
    title: "The note never says which of the 25 differing tables were inserted, and the upgrade rule \"for the shapes that failed\" cannot be applied by an operator"
  - id: WR-04
    severity: warning
    disposition: fixed
    title: "\"A unit test repeats the check on every build\" overclaims what `ConnectApiReferencesUnitTest` does, and the close record says the note claims less"
  - id: WR-05
    severity: warning
    disposition: fixed
    title: "The dependency table and the CHANGELOG list are presented as complete and omit jspecify (and, in the CHANGELOG, j2objc and error_prone)"
  - id: WR-06
    severity: warning
    disposition: fixed
    title: "The unreadable-declared-type residual is described three different ways (which names, how often, which WARNs)"
  - id: WR-07
    severity: warning
    disposition: fixed
    title: "The gg1 note still gives a false \"safe to upgrade\" for a connector with more than two topics"
  - id: WR-08
    severity: warning
    disposition: fixed
    title: "The upgrade order for the producer rename leaves an open window with no instruction to stop the producers"
  - id: IN-01
    severity: info
    disposition: fixed
    title: "\"Rolling back brings back\" ends with a bullet about the gg4 documents, which a rollback removes"
  - id: IN-02
    severity: info
    disposition: fixed
    title: "The gg1 `DELETE` example comment omits the trailing-split-character form that item 1 of the same note lists"
  - id: IN-03
    severity: info
    disposition: fixed
    title: "The CHANGELOG states \"No other change in this release needs one\" without the label the note gives the dependency claim"
  - id: IN-04
    severity: info
    disposition: fixed
    title: "The rename banner and the CHANGELOG bullet do not say that most of the escaped forms were client V2 only"
  - id: WR-09
    severity: warning
    disposition: fixed
    title: "Item 4 \"What to do\" still says to stop cleanly before an upgrade only for the shifted shape, which contradicts the bullet and the CHANGELOG that WR-03 made the default for every listed table"
  - id: WR-10
    severity: warning
    disposition: fixed
    title: "The rollback order names no consequence for a producer that cannot be paused, although the upgrade order does, and the CHANGELOG carries only the upgrade order"
  - id: IN-05
    severity: info
    disposition: fixed
    title: "The residual WARN quoted in item 2 says `size<digits>` sub-columns are treated as structural, while the sentence above it and the code say they are kept for the Array and Map rule"
  - id: IN-06
    severity: info
    disposition: fixed
    title: "The close record's rename-order carry says the rollback \"runs the same order in reverse\", which does not match the order the note gives"
  - id: IN-07
    severity: info
    disposition: fixed
    title: "The new unpausable-producer sentence in the rollback paragraph says \"after you saw no lag\" and repeats the sentence that follows it"
  - id: IN-08
    severity: info
    disposition: fixed
    title: "The CHANGELOG rollback sentence leaves out the in-flight check, the resume, and the window for a producer that cannot be paused"
  - id: IN-09
    severity: info
    disposition: fixed
    title: "The CHANGELOG rollback sentence gates the switch-back on an \"in-flight check\" that exists only for `exactlyOnce=true`"
open: 0
total: 20
recorded: 2026-10-23T19:20:51.000Z
---

# Phase 29: Code Review Disposition

| Finding | Severity | Disposition | Source |
|---------|----------|-------------|--------|
| CR-01 | critical | fixed | ae5b5ca6 (Phase 29) |
| WR-01 | warning | fixed | 66fe0f35 (Phase 29) |
| WR-02 | warning | fixed | 85d5ff27 (Phase 29) |
| WR-03 | warning | fixed | 768e9e79 (Phase 29) |
| WR-04 | warning | fixed | 1f27c98a (Phase 29) |
| WR-05 | warning | fixed | 9df54fe0 (Phase 29) |
| WR-06 | warning | fixed | 6a588859 (Phase 29) |
| WR-07 | warning | fixed | ae28e659 (Phase 29) |
| WR-08 | warning | fixed | b805af5f (Phase 29) |
| IN-01 | info | fixed | b51bb211 (Phase 29) |
| IN-02 | info | fixed | 68b41883 (Phase 29) |
| IN-03 | info | fixed | a2bf0a7d (Phase 29) |
| IN-04 | info | fixed | 972a196a (Phase 29) |
| WR-09 | warning | fixed | e420d5fc (Phase 29) |
| WR-10 | warning | fixed | 00fa73f2 (Phase 29) |
| IN-05 | info | fixed | 15ce9220 (Phase 29) |
| IN-06 | info | fixed | dc281a54 (Phase 29) |
| IN-07 | info | fixed | 0bcef5d6 (Phase 29) |
| IN-08 | info | fixed | ac903f11 (Phase 29) |
| IN-09 | info | fixed | be5a339f (Phase 29) (not reviewed by a later round; reflow b3035d25) |

round3_decision: "Fix them, then round 4" (the user, answering the orchestrator's AskUserQuestion after round 3 reported IN-07 and IN-08; options were fix then round 4, fix without round 4, accept and defer, hold)

Round 4 reported one Info finding (IN-09). The user's decision authorised a fourth round, not a fifth; IN-09 was fixed by the orchestrator in be5a339f (wording only, CHANGELOG) and was not reviewed by a further round. This is a recorded deviation. Every owning check was re-run and printed its OK marker after the fix.

## Mandated confirmations (PUB-12)

Restated from round 4 of 29-REVIEW.md, all five hold: (a) no TRUNCATE advice that would wipe other connectors' state remains; (b) rollback uses `pre-v1.7.0_gg4` and the nested-jar archive hazard is stated correctly; (c) the CREATE TABLE-name rename notice is present and correct; (d) exactlyOnce=false connectors are handled correctly and no false "safe" remains (IN-09 scoped the one CHANGELOG sentence); (e) the static checks and greps the note quotes are accurate.

---

_Recorded: 2026-10-23T19:20:51.000Z_
