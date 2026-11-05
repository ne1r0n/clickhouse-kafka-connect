# Phase 47 code-review disposition

Source: 47-REVIEW.md (0 critical, 5 warnings, 4 info). Advisory gate; nothing blocks. Fix round is plan 47-05 (47-05-SUMMARY.md, E-12..E-15). Status `open` means seen and not acted on.

| ID | Severity | Status | Note |
|----|----------|--------|------|
| WR-01 | warning | fixed (84f804d0) | FORK-DELTA writer row no longer lists the task's `stop()` as open. |
| WR-02 | warning | fixed (3e0575a7) | gg8 CHANGELOG bullet claims only what is shown; gg7 section byte-identical. |
| WR-03 | warning | fixed (7b348644) | Item 17 interval scoped to the measured Kafka 3.9 worker. |
| WR-04 | warning | fixed (0c063acf) | 50 redeliveries per row, class time 22.0 s to 2.9 s, report 36.2 MB to 3.9 MB; mutation re-run turns the same 64 rows red. |
| WR-05 | warning | fixed by qualification (7b348644) | Item 17 scopes the claim and states what the pin covers through `putDirect`; first-put matrix through `putDirect` not added (backlog). |
| IN-01 | info | open | CLOSE_COUNT / MBEAN_GONE assertions never seen red on their own. |
| IN-02 | info | open | Reflection on a private final field is brittle on newer JDKs. |
| IN-03 | info | fixed (84f804d0, 3e0575a7) | Item 13 says 2 of 6 cases were red; gg8 Known Issues carries `close(partitions)`. |
| IN-04 | info | open | `ClickHouseWriter.stop()` swallows `InterruptedException` (pre-existing, outside the diff). |
