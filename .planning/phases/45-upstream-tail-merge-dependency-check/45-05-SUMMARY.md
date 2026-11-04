---
phase: 45-upstream-tail-merge-dependency-check
plan: "05"
subsystem: fromjsonrow-pin-ups05-facts-fork-delta
tags: [ups-02, ups-03, ups-04, ups-05, from-json-row, slash-column, dependency-insight, fork-anchors, fork-delta, mutation-testing, evidence]

requires:
  - phase: 45-upstream-tail-merge-dependency-check
    provides: plan 45-04's JSON-route guard, anchors at 197 rows, E-12 testUnit total 2041, the 45-03 and 45-04 facts for FORK-DELTA
provides:
  - commit b41f913c, FieldDescriptorLiteralFieldsUnitTest (31 Docker-free cases; a worktree mutation to upstream's readValue turns exactly the 30 field cases red)
  - E-13, live SlashColumnRoundTripTest (4 cases, byte-identical to upstream) and the three live NAME pin classes green on ClickHouse 26.10.3.7 x client V2; the 14 NAME pin files byte-unchanged since pre-v1.7.0_gg8
  - commit 9c7a31a1, three fork-anchors rows (two connect-json test-scope lines, one 0 row for #839's dead describeTableV1 reader); mirror rows=200 bad=0; tail gate anchor_rows=200 PASS
  - E-14, UPS-05 resolution facts by dependencyInsight and buildEnvironment
  - commit 722d713f, docs/FORK-DELTA.md: two Connector-source rows, one Test-harness row, the WRT-13/WRT-17 row and Known costs item 6 rewritten for the closed Jackson residual, the raw-identifier row extended, the syncer pointer to the tail-3 checklist
  - E-15, testUnit 2072 tests green after the docs commit
affects: [45-06]

actuals:
  tokens: 8500
  tasks: 3
  commits: 6
plan_head_before: 6a98eb01b884ce1476d7ce19b1bfb0547bd89910
plan_head_after: c998287b7c0dffbed60e02983a18db7ceb9df37c

tech-stack:
  added: []
  patterns: [Docker-free parameterized pin over the Lombok getters of a parsed DESCRIBE row, detached-worktree mutation to upstream's one-liner, measured-before-written FORK-DELTA rows citing evidence entries]

key-files:
  created:
    - src/test/java/com/clickhouse/kafka/connect/sink/db/helper/FieldDescriptorLiteralFieldsUnitTest.java
  modified:
    - .planning/scripts/fork-anchors.txt
    - docs/FORK-DELTA.md
    - .planning/phases/45-upstream-tail-merge-dependency-check/45-COMMAND-EVIDENCE.md

key-decisions:
  - "Executed as locked: no src/main change, CHANGELOG.md and VERSION untouched (U-3), no NAME pin class and not SlashColumnRoundTripTest edited, no dependency bumped"
  - "FORK-DELTA says the JSON route's record value reached the thrown message, trace and DLQ headers and no measured logger line (E-10), not 'and the logs' as the plan text had it"

requirements-completed: [UPS-03, UPS-04, UPS-05, UPS-02]

coverage:
  - id: D1
    description: "fromJsonRow keeps type, comment, default_expression, codec_expression and ttl_expression as the server's literal JSON text and decodes only name, for six escape samples; the pin bites against upstream's decode-all readValue (30 red, JSON-null case green)"
    requirement: UPS-04
    verification:
      - kind: other
        ref: "P45-05-T1-OK; FieldDescriptorLiteralFieldsUnitTest 31 green, mutation 30 red; E-13"
        status: pass
    human_judgment: false
  - id: D2
    description: "A slash column round-trips live on ClickHouse latest x client V2 through DESCRIBE, insert and typed read-back (upstream's SlashColumnRoundTripTest, byte-identical, 4 cases) with the live NAME pins green and the NAME pin files byte-unchanged since the tag"
    requirement: UPS-03
    verification:
      - kind: other
        ref: "P45-05-T1-OK; 135 live tests, 0 failures on 26.10.3.7; E-13"
        status: pass
    human_judgment: false
  - id: D3
    description: "slf4j-simple 2.0.20, commons-lang3 3.21.0 (test and test-fixtures classpaths; 3.20.0 on the shipped runtimeClasspath), spotless 8.10.4 resolve where expected; connect-json 4.4.0 is requested only by the root project on testRuntimeClasspath and absent from runtimeClasspath and clickhouseDependencies; three anchor rows land in one commit and the unedited tail gate passes with anchor_rows=200"
    requirement: UPS-05
    verification:
      - kind: other
        ref: "P45-05-T2-OK; E-14"
        status: pass
    human_judgment: false
  - id: D4
    description: "docs/FORK-DELTA.md names the Option A row (E-06), the JSON-route row (E-10), the closed Jackson residual, #839's decode-all difference with the malformed-row contract, every user-decided test edit, and points the next syncer at the tail-3 checklist; testUnit is green at 2072 tests"
    requirement: UPS-02
    verification:
      - kind: other
        ref: "P45-05-T3-OK; E-15"
        status: pass
    human_judgment: true
    rationale: "Whether the rows read correctly to an operator is judgment; the orchestrator's independent gsd-code-reviewer pass over FORK-DELTA before Phase 49's publication gate covers it"

duration: 10min
completed: 2026-11-04
status: complete
---

# Phase 45 Plan 05: fromJsonRow pin, UPS-05 facts and FORK-DELTA Summary

**The one behaviour where the fork's `fromJsonRow` differs from upstream #839 (it decodes only `name`) is pinned by a Docker-free 31-case class that turns 30 red against upstream's `readValue`; upstream's slash round trip and the NAME pins pass live on latest x V2; connect-json is proven test-scope with three anchor rows (200 total); and FORK-DELTA now names every tail-3 difference with its pin.**

## Performance

- **Duration:** about 10 min of active work (the unedited tail gate is about 90 s, the live run 23 s)
- **Tasks:** 3
- **Files:** 1 test class created; the anchors file, FORK-DELTA and the evidence modified

## Accomplishments

- **Task 1 (tracer, E-13, commits `b41f913c`, `53c64a0f`).** `FieldDescriptorLiteralFieldsUnitTest`: 5 fields (`type`, `comment`, `default_expression`, `codec_expression`, `ttl_expression`) x 6 samples (escaped slash, backslash, quote, newline, tab, unicode with slash) plus a JSON-null case = 31 green cases. Mutation in a detached worktree (upstream's `readValue` as the first statement of `fromJsonRow`): exactly 30 red, the null case green, worktree removed. Live on ClickHouse `26.10.3.7` x client V2 (digest `sha256:739e099a...`): `SlashColumnRoundTripTest` 4 tests, `RawIdentifierNameLiveTest` 73, `RawNameMatrixLiveTest` 25, `NullableTupleMatrixLiveTest` 33, 0 failures. The 14 test files of the classes named by the four NAME-0N FORK-DELTA rows are byte-unchanged since `pre-v1.7.0_gg8`; `SlashColumnRoundTripTest` is byte-identical to upstream's blob.
- **Task 2 (E-14, commits `9c7a31a1`, `52573c68`).** `dependencyInsight`/`buildEnvironment`: slf4j-simple 2.0.20 (integrationTestRuntimeClasspath); commons-lang3 3.21.0 on testRuntimeClasspath and testFixturesRuntimeClasspath; **commons-lang3 3.20.0 on the shipped runtimeClasspath** (the ClickHouse client 0.11.0's own pin, next to the catalog's 3.21.0); spotless plugin 8.10.4; connect-json 4.4.0 requested by the root project only on testRuntimeClasspath, "No dependencies matching" on runtimeClasspath and clickhouseDependencies. Of the four coordinates approved to land in M, three (commons-lang3 3.21.0, slf4j 2.0.20, spotless 8.10.4) already sat at `pre-v1.7.0_gg8`; only the connect-json library line changed the catalog. Three anchor rows in one commit: counts 1, 1, 0; the 0 row's pattern occurs once in upstream's blob; mirror `rows=200 bad=0`; unedited tail gate over M..anchors commit `provenance_paths=11 anchor_commits=26 anchor_rows=200 marker_commits=44 anchor_exempt=0 result=PASS`.
- **Task 3 (E-15, commits `722d713f`, `c998287b`).** FORK-DELTA edited only: Option A row (cites E-06), JSON-route row (cites E-10), the WRT-13/WRT-17 pass-through row and Known costs item 6 rewritten for the closed Jackson residual (cites E-07, E-10..E-12), the raw-identifier row extended with #839's decode-all difference and the malformed-row contract plus the new pins, one Test-harness row for the user-decided test edits, "For the next syncer" leading with the tail-3 checklist and Recorded Facts 46 onward. Row deltas against the tag: Connector source +2, Test harness +1. Cache-busted testUnit: 2072 tests (2041 + 31), 0 failures, 2 skipped.

## Task Commits

1. **Task 1: fromJsonRow tracer** - `b41f913c` (test), `53c64a0f` (E-13)
2. **Task 2: UPS-05 facts and anchors** - `9c7a31a1` (anchors), `52573c68` (E-14)
3. **Task 3: FORK-DELTA rows** - `722d713f` (docs), `c998287b` (E-15)

`actuals.commits: 6` is the measured `git rev-list --count plan_head_before..plan_head_after`; the docs(45-05) completion commit follows this file (SUMMARY only; STATE.md, config.json, state.json, ROADMAP and requirements are not staged).

## Deviations from Plan

### Auto-fixed Issues

None needing code or plan change. Two wording reconciliations of plan text against the real tree and evidence (minor factual mismatches, recorded in E-15):

**1. [Wording] JSON-route row: "and the logs"**
- **Found during:** Task 3
- **Issue:** the plan's JSON-route row text says a Jackson chain naming a Map key reaches the task status, the DLQ stack-trace header "and the logs". E-10 measured `json_value_in_logs_shapes: none` (no value on any of the four captured loggers); the value reaches the thrown message, the rendered stack trace and the DLQ headers.
- **Fix:** the row states exactly the measured surfaces and says no measured logger line carried the value.
- **Commit:** `722d713f`

**2. [Wording] Raw-identifier row: "the four expression fields"**
- **Found during:** Task 3
- **Issue:** the plan says "the four expression fields are read nowhere in `src/main`", but only three of `comment`, `default_expression`, `codec_expression`, `ttl_expression` are expressions. A grep of `src/main` (outside the descriptor) finds no read of any of the four getters.
- **Fix:** the row says "the comment and the three expression fields are read nowhere in `src/main`".
- **Commit:** `722d713f`

Other reconciled counts: the anchors mirror reads `rows=200` after adding three rows to the 197 left by plan 45-04 (the plan's planning-time baseline of 190 is stale, as 45-03 and 45-04 added rows); testUnit total is 2041 + 31 = 2072 as the verify block computes.

### Other observations (not rule triggers)

- The tail gate for Task 2 ran twice with the same result (once to record, once inside the verify block); the first run's log is `tail-gate-04b.log`.
- Evidence facts for the `dependencyInsight` versions are recorded bare (`2.0.20`, `3.21.0`) so the plan's exact-value expectations hold; the configuration names are in the entry's prose.
- Spotless: `FieldDescriptorLiteralFieldsUnitTest.java` is `identical` after `spotlessApply` in a worktree.
- No JVM crash and no core dump occurred; every worktree was removed (`git worktree list` has one line); nothing was pushed and no remote ref touched.

**Total deviations:** 0 auto-fixed, 2 wording reconciliations. **Impact:** none on locked decisions or gate counts.

## Issues Encountered

None.

## Known Stubs

None.

## Threat Flags

None. T-45-13 (name-only decode pinned, bites 30 red), T-45-14 (malformed-row message contract kept by the unchanged NAME pin and named in FORK-DELTA), T-45-15 (connect-json test-scope proven by dependencyInsight, two anchor rows), T-45-16 (each new row cites its evidence entry, row deltas checked against the tag) are mitigated as the register states.

## User Setup Required

None - no external service configuration required.

## Notes for the orchestrator

- **Shipped commons-lang3:** the catalog line is `commonsLang3 = "3.21.0"` but the shipped `runtimeClasspath` resolves 3.20.0 (the ClickHouse client 0.11.0's pin); 3.21.0 reaches only the test and test-fixtures classpaths. Recorded in E-14.
- **FORK-DELTA awaits the orchestrator's independent `gsd-code-reviewer` pass** (it is an operator document) before Phase 49's publication gate. CHANGELOG.md and VERSION are untouched (U-3).

## Next Phase Readiness

- Ready for plan 45-06: `src/main` differs from the merge commit only in `ClickHouseWriter.java` and `Utils.java`; the anchors mirror reads `rows=200 bad=0`; testUnit is 2072 tests with 2 skips; the full-suite expectation is 2644 + 31 = 2675 tests with the 18 named skips (the new class is untagged).

## Self-Check: PASSED

- Files: `FieldDescriptorLiteralFieldsUnitTest.java`, `45-05-SUMMARY.md` and the E-13..E-15 entries are present.
- Commits `b41f913c`, `53c64a0f`, `9c7a31a1`, `52573c68`, `722d713f`, `c998287b` are ancestors of HEAD.
- Verify blocks `P45-05-T1-OK`, `P45-05-T2-OK` and `P45-05-T3-OK` printed on the committed tip; `git worktree list` has one line; `git diff --quiet pre-v1.7.0_gg8 HEAD -- CHANGELOG.md VERSION` exits 0.
