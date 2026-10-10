---
phase: 16
review: 16-REVIEW.md
titles: json
findings:
  - id: IN-01
    severity: info
    disposition: fixed
    title: "Item 8 tells the operator to merge `sync/v1.7.0`, a branch that is not published"
open: 0
total: 1
recorded: 2026-10-10T17:48:37.000Z
---

# Phase 16: Code Review Disposition

| Finding | Severity | Disposition | Source |
|---------|----------|-------------|--------|
| IN-01 | info | fixed | fd99a476 (docs only, Phase 16) |

Dispositions: `open` (recorded, not yet triaged), `fixed`, `skipped`, `deferred`.
Set `deferred` by hand and put the reason in the Source cell; both are preserved. A `|` in the reason is kept as prose and escaped on the next run.
Re-running the gate keeps every row it can. A row the current review no longer reports is kept and its Source cell flagged, so a finding does not leave this record silently. ONE exception: when a finding id is REUSED by a different finding, the earlier decision cannot keep a row — the id is taken — and it is dropped. A RECORDED decision (anything but `open`) is named on the console when that happens; a row still at `open` is replaced silently, because `open` records no decision to lose.

## Mandated confirmations (D-09)

Restated from `16-REVIEW.md`.

- **Confirmation 1: `505e4981`'s WR-01..WR-03 fixes still read correctly after the Phase 16 edits. Yes.** The item 1 shared-table scoping and topic-scoped query, the item 2 prefixed-key and topic-scoped `DELETE` examples with the Do-not-`TRUNCATE` paragraph, the item 3 value-based `date_time_input_format` wording with FORK-DELTA's L-05 qualification, and the item 7 `ObjectName` and regular-expression matcher examples are all present at HEAD. The SQL blocks equal `505e4981` (E-08), and the IN-01 fix (`fd99a476`) touched only the item 8 sentence.
- **Confirmation 2: no operator text in the tree to be merged tells an operator to TRUNCATE the shared KeeperMap state table. Confirmed.** The only sentence is the intended negative, "Do not `TRUNCATE` the table." (`docs/ROLLOUT-v1.7.0_gg1.md`); the FORK-DELTA hit is a Debezium truncate event. The MERGED-TREE-NO-TRUNCATE-INSTRUCTION command reported `md-files 17 truncate-lines 2 bad []` on HEAD at review time and is re-run on X0 in Task 3.
