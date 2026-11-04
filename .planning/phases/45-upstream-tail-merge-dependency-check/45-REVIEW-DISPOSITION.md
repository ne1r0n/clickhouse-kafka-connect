# Phase 45 code-review disposition

Source: 45-REVIEW.md (0 critical, 4 warnings, 5 info). Advisory gate; nothing blocks. Status `open` means seen and not yet acted on.

| ID | Severity | Status | Note |
|----|----------|--------|------|
| WR-01 | warning | open | No test drives a Jackson root with the write-timeout prefix, or the Map-lambda shape. Candidate for a follow-up test-only plan. |
| WR-02 | warning | open | Rebuild is fail-open on an unexpected exception type; suppressed exceptions on the kept IOException suffix are not scanned. Possible value-free gap, needs the user's call (production change). |
| WR-03 | warning | open | handleException classification is duplicated in the rebuild with no tying test. |
| WR-04 | warning | open | Three new test classes duplicate helper blocks. |
| IN-01 | info | open | Two FORK-DELTA over-statements; reviewed again by the independent reviewer before Phase 49's publication gate. |
| IN-02 | info | open | Failure-path message formatted eagerly. |
| IN-03 | info | open | Withheld note defined in two constants. |
| IN-04 | info | open | A failing column logs two ERROR lines. |
| IN-05 | info | open | Two edited upstream tests weaken assertions; both recorded as user decisions (U-2). |
