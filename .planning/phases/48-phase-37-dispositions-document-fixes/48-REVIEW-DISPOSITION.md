# Phase 48 code-review disposition

Source: 48-REVIEW.md (0 critical, 2 warnings, 3 info). Advisory gate; nothing blocks. Fix round is the post-review gap-closure run (E-19). Status `open` means seen and not acted on.

| ID | Severity | Status | Note |
|----|----------|--------|------|
| WR-01 | warning | fixed (72928578) | Allow-list wildcard rule strips a leading `outer/` and counts non-empty segments; `outer/com/*`, `outer/META-INF/*` and `a//*` are refused. Red run first (3 of 14 failed on the old rule); the 49 shipped rows still pass. Class goes from 9 to 14 cases, so testUnit is 2325 and the added rows are 30 (E-19). |
| WR-02 | warning | fixed (7ae4284c) | FORK-DELTA item 14 now says which line is logged where, checked against `ClickHouseWriter`; resolver stays 98/70/0, anchors 218. |
| IN-01 | info | fixed (a968e486) | `DeclaredNullabilityFixtures` javadoc reworded; comment-only, no blank line added. |
| IN-02 | info | open | Folding the MBean test into the existing one would remove a test; project rule forbids it. |
| IN-03 | info | open | Milestone-close hand-off: repoint the `.planning/phases/45..47` evidence paths and re-run `48-path-check.py` (E-18). |

Counts after the fix round: testUnit 2325 (measured, Docker unreachable). The full suite was not re-run after the 5 added Docker-free cases; its expected total 2928 is derived from E-17's 2923, not measured. E-17's 2320/25 are superseded by 2325/30. Phase 49's raw-numbers refresh uses these.
