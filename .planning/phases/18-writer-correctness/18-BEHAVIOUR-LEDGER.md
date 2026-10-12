# Phase 18 Behaviour Ledger

**Purpose:** one place that lists every operator-visible behaviour change made in Phase 18 (Writer Correctness), so that Phase 20 can write the `1.7.0_gg2` CHANGELOG section, complete FORK-DELTA and write `docs/ROLLOUT-v1.7.0_gg2.md` (ROLL-02). Phase 18 writes no CHANGELOG, VERSION or ROLLOUT text (D-13).

**Source plans:** 18-01 (L-23..L-26)

**How to read it:** IDs continue after the Phase 13 ledger's L-22. Test references are `Class` or display-name prefixes. The ledger holds test names, decision IDs and message formats only, never record data.

**Destination key:** CHANGELOG = the `1.7.0_gg2` section of `CHANGELOG.md` (PUB-07); FORK-DELTA = a row or Known costs item (PUB-07); ROLL-02 = an item in `docs/ROLLOUT-v1.7.0_gg2.md`.

## Ledger

| ID | Source plan | Behaviour change (route or setting, before -> after) | Decisions | Pinned by | Operator-visible | How an operator notices | Phase 20 destination |
|----|-------------|------------------------------------------------------|-----------|-----------|------------------|-------------------------|----------------------|
| L-23 | 18-01 | RowBinary route (client V1 in practice; any exception that reaches the second catch of `doInsertRawBinary`): a writer failure whose exception has no message now surfaces as itself. Before, a NullPointerException `Cannot invoke "String.contains(java.lang.CharSequence)" because the return value of "java.lang.Exception.getMessage()" is null` replaced it in Connect status, the DLQ reason and the logs. A message-less SocketTimeoutException at that point is now retried (RetriableException) where it used to fail the task. Exceptions with a non-null message are unchanged, including the Code 33 and Code 131 refresh-and-retry-once. | D-06, D-07 | `ClickHouseWriterNullMessageUnitTest`, `WriterErrorPathLiveTest` (WRT-11 cases) | yes | That NullPointerException text disappears from task traces, and failed tasks show the original exception class. | CHANGELOG; FORK-DELTA (row, item 5) |
