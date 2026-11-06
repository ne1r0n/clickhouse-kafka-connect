# Phase 46 code-review disposition

Source: 46-REVIEW.md (0 critical, 5 warnings, 7 info). Advisory gate; nothing blocks. Fix round is plan 46-06 (46-06-SUMMARY.md, E-15..E-19). Status `open` means seen and not acted on.

| ID | Severity | Status | Note |
|----|----------|--------|------|
| WR-01 | warning | fixed (46-06) | V2 deadline re-armed in `onRetry` via `deadlineAwareWriter`; red-first pin `clientInternalRetryOfAnInsertGetsAFreshWriteDeadline`, mutation on empty `onRetry` turns it red. |
| WR-02 | warning | fixed (46-06) | Help text and item 14 state that the V1 library 30 s limit still ends a quiet insert. |
| WR-03 | warning | fixed (46-06) | Help text claims the response-wait bound only; no claim for a V1 body-write stall. |
| WR-04 | warning | fixed (46-06) | "30 s" stated per attempt (retry count read from code, not measured); `timeoutSeconds` named. |
| WR-05 | warning | open | A deadline cancel on the RowBinary route logs two misleading ERROR lines (same family as Phase 45 IN-04). Backlog candidate. |
| IN-01 | info | fixed (46-06) | Lead sentence no longer states a general bound. |
| IN-02 | info | accepted | Progress-while-sending rests on a stub pin; the deadline is wall-clock, so the claim follows from the design. |
| IN-03 | info | fixed (46-06) | Case renamed to what it tests. |
| IN-04 | info | fixed (46-06) | `socket_timeout` wording made true for `async=true`. |
| IN-05 | info | open | V1 timeout path ignores `cancel(true)` result. |
| IN-06 | info | open | `stop()` during an insert can surface `RejectedExecutionException` from `arm()`. |
| IN-07 | info | fixed (46-06) | `E-NN` placeholder replaced; schema-refresh fresh bound stated. |
