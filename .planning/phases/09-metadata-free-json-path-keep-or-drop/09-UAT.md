---
status: testing
phase: 09-metadata-free-json-path-keep-or-drop
source: [09-VERIFICATION.md]
started: "2026-09-19T23:08:58Z"
updated: "2026-09-19T23:08:58Z"
---

## Current Test

number: 1
name: CR-01 disposition — `auto.evolve` + metadata-free bypass interaction
expected: |
  Configure `auto.evolve=true` together with an explicit `insertFormat=json`, and send a
  batch whose records carry a non-null schema (`SCHEMA`/`DEBEZIUM_CDC` route) to a table
  whose actual columns differ from the record's fields.

  `ClickHouseWriter.doInsert` (`:210-222`) enters the `auto.evolve` branch with NO
  `shouldSkipTableMetadata()` guard — the four guards live at `:125`, `:147`, `:1400`,
  `:1617`, not here. The synthetic `Table` has an empty column list, so
  `table.getMissingColumns(...)` reports EVERY field as missing, and the chain runs
  `evolveTableSchema` -> `chc.alterTableAddColumns` (`:1041`, a DDL write) ->
  `refreshTableAfterDDL` -> `urgentTableUpdate` -> `chc.describeTable` (`:1113`).

  Two possible outcomes, neither matching "the bypass works":
    (a) the connector user lacks `DESCRIBE`/`SHOW` (operator fact 3, the entire reason
        KEEP was chosen) -> hard failure on every batch for that topic;
    (b) the user has the grant -> silent `ALTER TABLE` + `DESCRIBE` traffic on every
        batch — exactly the per-batch metadata traffic D-03's `updateMapping` guard
        was built to eliminate.

  Scope note in the connector's favour: `auto.evolve` defaults to `false`
  (`ClickHouseSinkConfig.java:344`), so this requires an explicit opt-in combined with
  an explicit `insertFormat=json`. Nothing validates that combination at startup.

  This is a scope/priority decision, not a pass/fail test: fix the `auto.evolve` branch
  now (the reviewer's suggested one-line guard), or record it as an accepted, scheduled
  follow-up. No plan's must_haves named this fifth consumer of the synthetic `Table`'s
  empty column list — `09-CONTEXT.md`'s Integration Points enumerated only three
  (`validateDataSchema`, `doInsertRawBinaryV2`'s write loop/R-01, `cleanupExtraFields`).
awaiting: user response

## Tests

### 1. CR-01 disposition: `auto.evolve` + metadata-free bypass interaction
expected: Decide whether to guard `ClickHouseWriter.doInsert`'s `auto.evolve` branch with `shouldSkipTableMetadata()` now, or record it as an accepted scheduled follow-up. Under `auto.evolve=true` + explicit `insertFormat=json` + a schema'd record, the unguarded branch drives `ALTER TABLE` and `DESCRIBE` traffic on every batch against the synthetic empty-column `Table` — or fails hard where the `DESCRIBE` grant is absent, which is the exact operator scenario the KEEP decision exists to serve. Source: `09-REVIEW.md` CR-01 (Critical), independently re-confirmed present during verification.
result: [pending]

## Summary

total: 1
passed: 0
issues: 0
pending: 1
skipped: 0
blocked: 0

## Gaps

No must-have from the ROADMAP's four success criteria, nor from any of the three plans'
frontmatter `must_haves`, failed verification — all four criteria are independently
confirmed true against the current tree (4/4). CR-01 sits outside the phase's declared
scope (the four guard sites) but bears on its headline claim that the bypass works under
a real, supported configuration combination, so it is escalated here rather than
absorbed into a silent pass.

Carried forward, low severity, doc-only (not a numbered test): **WR-01** —
`ExplicitJsonInsertPathLiveTest`, `JsonInsertColumnsLiveTest` and
`ClickHouseSinkTaskJsonInsertFormatTest` still carry class-level Javadoc reading
"Disabled pending the JSONP-01 keep/drop decision" even though `@Disabled` was removed
and all three now run live. `MetadataFreeJsonInsertUnitTest`'s Javadoc was correctly
rewritten in this phase, establishing the pattern the other three did not follow.
