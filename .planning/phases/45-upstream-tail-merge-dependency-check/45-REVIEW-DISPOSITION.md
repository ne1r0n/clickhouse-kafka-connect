# Phase 45 code-review disposition

Source: 45-REVIEW.md (0 critical, 4 warnings, 5 info). Advisory gate; nothing blocks. Status `open` means seen and not yet acted on.

| ID | Severity | Status | Note |
|----|----------|--------|------|
| WR-01 | warning | fixed (876a9bd1) | Test-only: `JacksonOverIoValueFreeUnitTest` gains the write-timeout-prefixed Jackson root and the RuntimeException over Jackson over read-timeout SocketTimeoutException chains, plus `staysRetriableAndValueFree`; the write-timeout branch deletion and a dropped-suffix mutation both turn the pins red (E-19). |
| WR-02 | warning | accepted residual (user decision) | No production change. FORK-DELTA is qualified (02baac3a): no Jackson link remains in the thrown cause chain; the writer path builds no suppressed Jackson exception and suppressed exceptions on the kept IOException suffix are not scanned. |
| WR-03 | warning | open | handleException classification is duplicated in the rebuild with no tying test. |
| WR-04 | warning | open | Three new test classes duplicate helper blocks. |
| IN-01 | info | fixed (02baac3a) | Both FORK-DELTA over-statements corrected: Jackson links above the root are dropped (the chain can be shorter), and the closing claim now says no Jackson link remains in the thrown cause chain. FORK-DELTA is still reviewed again by the independent reviewer before Phase 49's publication gate. |
| IN-02 | info | open | Failure-path message formatted eagerly. |
| IN-03 | info | open | Withheld note defined in two constants. |
| IN-04 | info | open | A failing column logs two ERROR lines. |
| IN-05 | info | open | Two edited upstream tests weaken assertions; both recorded as user decisions (U-2). |
