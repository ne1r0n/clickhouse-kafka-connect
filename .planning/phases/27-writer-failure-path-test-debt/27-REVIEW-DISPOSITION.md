---
phase: 27
review: 27-REVIEW.md
titles: json
findings:
  - id: WR-01
    severity: warning
    disposition: open
    title: "Unknown-enum-label failure is not actionable for the operator, and the new column-named messages never reach any output"
  - id: IN-01
    severity: info
    disposition: open
    title: "`passesThroughUnchanged` short-circuits before the Jackson check, so the exclusion is a property of the call sites and not of the predicate"
  - id: IN-02
    severity: info
    disposition: open
    title: "`JsonAsStringKeyResidualLiveTest` name and Javadoc intro no longer describe what it pins"
  - id: IN-03
    severity: info
    disposition: open
    title: "`SourcePlanningIdGuardUnitTest` has a narrow scope and a broad single-letter prefix list"
---

# Phase 27 review disposition

WR-01 restates the plan 27-03 SUMMARY's Open Question 1: D-06 locked `IllegalArgumentException`, and a pass-through exception type is a separate decision for the user. Nothing was fixed in this run; all four findings stay open for `/gsd-code-review 27 --fix` or a user decision.
