---
phase: 28
review: 28-REVIEW.md
titles: json
findings:
  - id: WR-01
    severity: warning
    disposition: fixed
    title: "Enum label scan reads the SQL escapes `\\0`, `\\b` and `\\f` as the letters 0, b and f"
  - id: WR-02
    severity: warning
    disposition: fixed
    title: "`describeTableV1` lets the new enum parse failure escape and abort the mapping of the whole database"
  - id: WR-03
    severity: warning
    disposition: fixed
    title: "The new JSON-layer decoder's `\\uXXXX` path and the malformed-enum branches have no test"
  - id: WR-04
    severity: warning
    disposition: fixed
    title: "`legacyEnumLabelKeyFailsLoudly` passes on any failure, not on the enum lookup miss it names"
  - id: IN-01
    severity: info
    disposition: deferred
    title: "`DeclaredNullability.ESCAPED_NEWLINE` is a public production constant that only tests read"
  - id: IN-02
    severity: info
    disposition: deferred
    title: "`ForkCoverageScopeUnitTest` cannot detect a fork class missing from the list, and `SinkTaskStatistics` is one"
  - id: IN-03
    severity: info
    disposition: fixed
    title: "A record still using the escaped name form is stored with the column DEFAULT and no log line"
  - id: IN-04
    severity: info
    disposition: deferred
    title: "The server's escape set for enum labels is now measurable, but the pin and the wording still say \"not measured\""
  - id: IN-05
    severity: info
    disposition: deferred
    title: "`DescribeUnreadableEnumMockServerTest` never closes the helper clients and does not tie the null to the enum"
open: 0
total: 9
recorded: 2026-10-23T18:45:37.538Z
---

# Phase 28 review disposition

Code review was advisory and found 0 critical, 4 warnings and 3 info items; the re-review after the fixes found no new critical or warning items and added IN-04 and IN-05 (info). `/gsd-code-review 28 --fix` fixed WR-01 to WR-04 (see 28-REVIEW-FIX.md); the five info items stay open. WR-01 rests on the reviewer's reading of ClickHouse string escaping and is not measured in this repo. IN-03 is the accepted D-06 decision, recorded so it is not lost.

Phase 29 (plan 29-05) fixed IN-03 by promoting the rename notice to the top of the CHANGELOG section and the rollout note (commit `aad30ee6`) and recorded IN-01, IN-02, IN-04 and IN-05 as `deferred` in `29-MILESTONE-CLOSE-RECORD.md`, because a release phase makes no `src/` or test edit (D-05). IN-02 and the server half of IN-04 have their documentation written in FORK-DELTA.

## Findings

| Finding | Severity | Disposition | Source |
|---------|----------|-------------|--------|
| WR-01 | warning | fixed | 28-REVIEW-FIX.md |
| WR-02 | warning | fixed | 28-REVIEW-FIX.md |
| WR-03 | warning | fixed | 28-REVIEW-FIX.md |
| WR-04 | warning | fixed | 28-REVIEW-FIX.md |
| IN-01 | info | deferred | 29-MILESTONE-CLOSE-RECORD.md (carried: a public constant that only tests read; src edit) |
| IN-02 | info | deferred | 29-MILESTONE-CLOSE-RECORD.md (carried: the coverage guard cannot detect a missing class; documentation half written, list and test carried) |
| IN-03 | info | fixed | aad30ee6 (Phase 29; rename notice in CHANGELOG Important Changes and the note's opening) |
| IN-04 | info | deferred | 29-MILESTONE-CLOSE-RECORD.md (carried: server half measured and documented, test half carried) |
| IN-05 | info | deferred | 29-MILESTONE-CLOSE-RECORD.md (carried: test hygiene of one mock-server test; test edit) |

Dispositions: `open` (recorded, not yet triaged), `fixed`, `skipped`, `deferred`.
Set `deferred` by hand and put the reason in the Source cell; both are preserved. A `|` in the reason is kept as prose and escaped on the next run.
Re-running the gate keeps every row it can. A row the current review no longer reports is kept and its Source cell flagged, so a finding does not leave this record silently. ONE exception: when a finding id is REUSED by a different finding, the earlier decision cannot keep a row — the id is taken — and it is dropped. A RECORDED decision (anything but `open`) is named on the console when that happens; a row still at `open` is replaced silently, because `open` records no decision to lose.
