---
phase: 17
review: 17-REVIEW.md
titles: json
findings:
  - id: WR-01
    severity: warning
    disposition: fixed
    title: "Unobserved `exclude-paths` key could invalidate the whole Dependabot config, and the guard does not pin it"
  - id: IN-01
    severity: info
    disposition: fixed
    title: "The guard does not pin the workflow's use of `reportTestForks`"
  - id: IN-02
    severity: info
    disposition: fixed
    title: "Pin guard regexes are brittle in two small ways"
  - id: IN-03
    severity: info
    disposition: skipped
    title: "The jar-origin check in `Lz4RoundTripLiveTest` now duplicates the Docker-free unit test"
open: 0
total: 4
recorded: 2026-10-12T08:28:47.317Z
---

# Phase 17: Code Review Disposition

| Finding | Severity | Disposition | Source |
|---------|----------|-------------|--------|
| WR-01 | warning | fixed | fee898c6 (21-04, D-09: TestsWorkflowPinGuardUnitTest pins the four exclude-paths entries and that each exists under .github/workflows, 21-EVIDENCE.md E-11) and 5d616806 (21-01 OPS-02: the user confirmed the Dependabot gradle entry runs with no configuration error, 21-EVIDENCE.md E-02; 21-UAT.md test 1 pass, 5010093f, 2026-10-19) |
| IN-01 | info | fixed | fee898c6 (21-04, CI-18: TestsWorkflowPinGuardUnitTest fails if the Test fork count step stops reading reportTestForks or the task disappears from build.gradle.kts; red on fixtures in 21-EVIDENCE.md E-10, green in E-11) |
| IN-02 | info | fixed | fee898c6 (21-04, CI-19: runs-on accepts a trailing comment, and a step name or run text containing a write colon is no longer a write scope; red against the old regexes in 21-EVIDENCE.md E-10, green in E-11) |
| IN-03 | info | skipped | leave Lz4RoundTripLiveTest's jar-origin check, cross-referenced to TestRuntimeClasspathLz4UnitTest, with no new origin assertions (ROADMAP Phase 23 SC5, DEBT-12); the live test is neither deleted nor weakened, and its Javadoc already names the unit test |

Dispositions: `open` (recorded, not yet triaged), `fixed`, `skipped`, `deferred`.
Set `deferred` by hand and put the reason in the Source cell; both are preserved. A `|` in the reason is kept as prose and escaped on the next run.
Re-running the gate keeps every row it can. A row the current review no longer reports is kept and its Source cell flagged, so a finding does not leave this record silently. ONE exception: when a finding id is REUSED by a different finding, the earlier decision cannot keep a row — the id is taken — and it is dropped. A RECORDED decision (anything but `open`) is named on the console when that happens; a row still at `open` is replaced silently, because `open` records no decision to lose.
