---
phase: 27
review: 27-REVIEW.md
titles: json
findings:
  - id: WR-01
    severity: warning
    disposition: deferred
    title: "Unknown-enum-label failure is not actionable for the operator, and the new column-named messages never reach any output"
  - id: IN-01
    severity: info
    disposition: deferred
    title: "`passesThroughUnchanged` short-circuits before the Jackson check, so the exclusion is a property of the call sites and not of the predicate"
  - id: IN-02
    severity: info
    disposition: deferred
    title: "`JsonAsStringKeyResidualLiveTest` name and Javadoc intro no longer describe what it pins"
  - id: IN-03
    severity: info
    disposition: deferred
    title: "`SourcePlanningIdGuardUnitTest` has a narrow scope and a broad single-letter prefix list"
open: 0
total: 4
recorded: 2026-10-23T18:45:37.538Z
---

# Phase 27 review disposition

WR-01 restates the plan 27-03 SUMMARY's Open Question 1: D-06 locked `IllegalArgumentException`, and a pass-through exception type is a separate decision for the user. Nothing was fixed in this run; all four findings stay open for `/gsd-code-review 27 --fix` or a user decision.

Phase 29 (plan 29-05) recorded all four as `deferred`: each is carried in `29-MILESTONE-CLOSE-RECORD.md` with a reason, an owner and a re-open trigger, because a release phase makes no `src/` or test edit (D-05). WR-01 stays a user decision on a pass-through exception type.

## Findings

| Finding | Severity | Disposition | Source |
|---------|----------|-------------|--------|
| WR-01 | warning | deferred | 29-MILESTONE-CLOSE-RECORD.md (carried: an unknown enum label shows the class and column only; a pass-through type is a user decision) |
| IN-01 | info | deferred | 29-MILESTONE-CLOSE-RECORD.md (carried: the Jackson exclusion is a property of the call sites; test or Javadoc edit) |
| IN-02 | info | deferred | 29-MILESTONE-CLOSE-RECORD.md (carried: the residual test keeps its old name; rename touches tests and documents) |
| IN-03 | info | deferred | 29-MILESTONE-CLOSE-RECORD.md (carried: narrow scope and broad prefix list of the planning-ID guard; test edit) |

Dispositions: `open` (recorded, not yet triaged), `fixed`, `skipped`, `deferred`.
Set `deferred` by hand and put the reason in the Source cell; both are preserved. A `|` in the reason is kept as prose and escaped on the next run.
Re-running the gate keeps every row it can. A row the current review no longer reports is kept and its Source cell flagged, so a finding does not leave this record silently. ONE exception: when a finding id is REUSED by a different finding, the earlier decision cannot keep a row — the id is taken — and it is dropped. A RECORDED decision (anything but `open`) is named on the console when that happens; a row still at `open` is replaced silently, because `open` records no decision to lose.
