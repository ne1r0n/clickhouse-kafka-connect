---
phase: 23
review: 23-REVIEW.md
titles: json
findings:
  - id: WR-01
    severity: warning
    disposition: deferred
    title: "The escaped-quote branch of `normalise` is not pinned by any test"
  - id: WR-02
    severity: warning
    disposition: deferred
    title: "`discoverAsDescribeTableV2` copies the production DESCRIBE loop into the test"
  - id: IN-01
    severity: info
    disposition: deferred
    title: "`apply` Javadoc still says \"all or none\""
  - id: IN-02
    severity: info
    disposition: deferred
    title: "Null-name check sits inside the `try` and logs a null column slot"
  - id: IN-03
    severity: info
    disposition: deferred
    title: "Live-test premise loops can pass vacuously, and lookups can NPE instead of failing with a message"
  - id: IN-04
    severity: info
    disposition: fixed
    title: "The unpaired-element WARN repeats at every refresh for a known, unfixed defect"
open: 0
total: 6
recorded: 2026-10-20T12:29:36.081Z
---

# Phase 23: Code Review Disposition

| Finding | Severity | Disposition | Source |
|---------|----------|-------------|--------|
| WR-01 | warning | deferred | 24-MILESTONE-CLOSE-RECORD.md (carried: escaped-quote branch of normalise has no test; test edit outside the release phase) |
| WR-02 | warning | deferred | 24-MILESTONE-CLOSE-RECORD.md (carried: test copies the production DESCRIBE loop; needs a src/main extraction) |
| IN-01 | info | deferred | 24-MILESTONE-CLOSE-RECORD.md (carried: apply Javadoc wording; src/main edit) |
| IN-02 | info | deferred | 24-MILESTONE-CLOSE-RECORD.md (carried: null-name check placement and WARN text; src/main edit) |
| IN-03 | info | deferred | 24-MILESTONE-CLOSE-RECORD.md (carried: live-test premise loops can pass vacuously; existing test not edited in the release phase) |
| IN-04 | info | fixed | 9d4cb9b2 (Phase 24; rollout note item 2 says what the unpaired-element WARN marks) |

Dispositions: `open` (recorded, not yet triaged), `fixed`, `skipped`, `deferred`.
Set `deferred` by hand and put the reason in the Source cell; both are preserved. A `|` in the reason is kept as prose and escaped on the next run.
Re-running the gate keeps every row it can. A row the current review no longer reports is kept and its Source cell flagged, so a finding does not leave this record silently. ONE exception: when a finding id is REUSED by a different finding, the earlier decision cannot keep a row — the id is taken — and it is dropped. A RECORDED decision (anything but `open`) is named on the console when that happens; a row still at `open` is replaced silently, because `open` records no decision to lose.
