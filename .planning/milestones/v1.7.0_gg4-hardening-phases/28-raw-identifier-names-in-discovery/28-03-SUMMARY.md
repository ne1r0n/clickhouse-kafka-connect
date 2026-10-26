---
phase: 28-raw-identifier-names-in-discovery
plan: "03"
subsystem: database
tags: [describe-json, jackson-streaming, declared-nullability, normalise, utf-8, client-v1-v2, red-first]

requires:
  - phase: 28-raw-identifier-names-in-discovery
    provides: plan 28-02 committed server DESCRIBE rows, the 12-entry name matrix, assertDiscoveredUnderRawNames and the raw-name fixture constants; E-04 predictions
provides:
  - ClickHouseFieldDescriptor.fromJsonRow decodes the name to the raw identifier and keeps every other string as the server's literal JSON text
  - one UTF-8 raw-line reader (readDescribeRows) for both describe paths, so client V1 and V2 yield identical names
  - DeclaredNullability.normalise rewritten (JSON layer via unescapeJsonLayer, then a quote-aware pass), so declared nullability pairs on the raw-name key
  - 23-REVIEW IN-01 (apply Javadoc) and IN-02 (null-name placeholder) closed
  - E-05 to E-07, the named exemption list (X-01..X-12, S-01..S-03) with its sign-off, and ledger rows L-58 to L-61
affects: [28-04, 28-05, 28-06, phase-29-rollout]

actuals:
  tokens: 10435
  tasks: 3
  commits: 2

tech-stack:
  added: []
  patterns:
    - "Names-only decode: stream the row with the parser, slice every non-name string by token offsets, bind through treeToValue so databind rules stay unchanged"
    - "Rewrite a parser rather than extend it; prove the coupling by swapping the old half back in and watching the pairing rows fail"
    - "Patch-swap red runs: save the fix as a patch, restore HEAD's src/main, run, re-apply, and cmp the diff to the saved patch"

key-files:
  created:
    - .planning/phases/28-raw-identifier-names-in-discovery/28-BEHAVIOUR-LEDGER.md
  modified:
    - src/main/java/com/clickhouse/kafka/connect/sink/db/helper/ClickHouseFieldDescriptor.java
    - src/main/java/com/clickhouse/kafka/connect/sink/db/helper/ClickHouseHelperClient.java
    - src/main/java/com/clickhouse/kafka/connect/sink/db/mapping/DeclaredNullability.java
    - src/test/java/com/clickhouse/kafka/connect/sink/db/helper/RawNameMatrixFixturesUnitTest.java
    - src/test/java/com/clickhouse/kafka/connect/sink/db/helper/RawNameMatrixLiveTest.java
    - src/test/java/com/clickhouse/kafka/connect/sink/db/mapping/DeclaredNullabilityTest.java
    - src/test/java/com/clickhouse/kafka/connect/sink/NullableTupleMatrixLiveTest.java
    - .planning/scripts/fork-anchors.txt
    - .planning/phases/28-raw-identifier-names-in-discovery/28-COMMAND-EVIDENCE.md

key-decisions:
  - "The decode streams the row, takes the decoded text for name and the literal text between the quotes for every other string (offsets from currentTokenLocation and currentLocation), and binds through OBJECT_MAPPER.treeToValue, so snake_case binding, the unknown-property failure and the 0/1 is_subcolumn coercion stay the databind's"
  - "unescapeJsonLayer is a package-private static of DeclaredNullability, total and linear, so plan 28-05's enum-label parser in Column can call the same decoder"
  - "The decode, the V1 raw-line reader and the normalise rewrite are one commit (fix(28-03)); the coupling run proves the decode without the rewrite fails the discovery rows for lf, tab and cr"

requirements-completed: [NAME-03, NAME-04]

coverage:
  - id: D1
    description: "fromJsonRow yields the raw identifier for column and element names and keeps the type text, default expression and is_subcolumn as before, for all 12 matrix entries"
    requirement: NAME-03
    verification:
      - kind: unit
        ref: "RawNameMatrixFixturesUnitTest#fromJsonRowYieldsTheRawNameAndKeepsTheTypeText (12 cases); red on HEAD for exactly bs, bsn, lf, tab, cr, sl, dq, btbsn (E-05)"
        status: pass
    human_judgment: false
  - id: D2
    description: "Both describe clients read the raw response lines as UTF-8 and map every matrix table under the raw identifiers with no fallback WARN"
    requirement: NAME-03
    verification:
      - kind: integration
        ref: "RawNameMatrixLiveTest#clientV1DescribesTheRawIdentifiers (12 cases); red on HEAD for exactly cr, dq, lf, sl, tab (E-05); post-fix re-render of 60 name cells on both clients (E-07)"
        status: pass
    human_judgment: false
  - id: D3
    description: "DeclaredNullability pairs on the raw-name key; normalise decodes the JSON layer and the SQL line-feed, tab and carriage-return escapes inside quotes; the COLS-13 twins stay green keyed on the raw name"
    requirement: NAME-04
    verification:
      - kind: unit
        ref: "RawNameMatrixFixturesUnitTest#discoveryPairsUnderTheRawName (12 cases), DeclaredNullabilityTest (69), coupling run fails lf, tab, cr (E-06)"
        status: pass
      - kind: integration
        ref: "NullableTupleMatrixLiveTest (33 tests green; HEAD's version fails exactly the two twins and the drift test on the fix, E-07)"
        status: pass
    human_judgment: false
  - id: D4
    description: "The named exemption list X-01..X-12 (pin) and S-01..S-03 (javadoc, refactor) is recorded row for row in E-06 with each pin's red line from the unchanged production and the sign-off line"
    requirement: NAME-04
    verification:
      - kind: other
        ref: "Task 2 verify EXEMPTION-LIST-RECORDED"
        status: pass
    human_judgment: true
    rationale: "The list is the user-authorized set of changed existing pins; plan 28-06's end-of-phase human-check re-confirms it against what landed"
  - id: D5
    description: "Full suite on exactly the commit tree: 1746 tests in 99 XML files, 0 failures, the 18 named skips, one server version; anchors at 90 rows; Spotless equal to the base"
    verification:
      - kind: other
        ref: "Task 3 verify DECODE-ONE-COMMIT and DECODE-GATED; gate anchor_rows=90 result=PASS"
        status: pass
    human_judgment: false

plan_head_before: baea0cf58b6aedbc4edbdd0de951e676ff81e05d
plan_head_after: b88859efefc7f837a599703799c9f9e68473fbcc

duration: 21min
completed: 2026-10-23
status: complete
---

# Phase 28 Plan 03: Raw identifier names in discovery (NAME-03, NAME-04) Summary

**Discovery now yields the raw `CREATE TABLE` identifier for column and element names on both clients: `fromJsonRow` streams the DESCRIBE row, decoding only the name; both clients read the same UTF-8 lines; and `DeclaredNullability.normalise` is rewritten to decode the JSON layer so nullability pairs on the raw-name key, all in one commit.**

## Performance

- **Duration:** about 21 min (start not recorded at launch; estimated from the previous plan's last commit at 09:48Z)
- **Completed:** 2026-10-23T10:10Z
- **Tasks:** 3 (one tracer, two auto)
- **Files modified:** 10 in the plan range (3 src/main, 4 test files, the anchors file, the evidence file, the new ledger)

## Accomplishments

- **Red first.** On HEAD's production the new NAME-03 rows failed for exactly bs, bsn, lf, tab, cr, sl, dq and btbsn, the NAME-04 discovery rows for bs, bsn, bt, btbsn, cr, dq, lf, sl and tab, and the new client V1 pin for cr, dq, lf, sl and tab; all three sets equalled the E-04 predictions, and the 13 plan-28-02 cases per class passed.
- **The fix, one change in three files.** `ClickHouseFieldDescriptor.fromJsonRow` takes the decoded text for `name` and the literal text between the quotes for every other string, binding through `treeToValue`; `describeTableV1` now reads `response.getInputStream()` through `readDescribeRows`, the same private reader as V2; `DeclaredNullability.normalise` decodes the JSON layer with the package-private `unescapeJsonLayer`, then in one quote-aware pass turns a line feed outside quotes into a space and backslash-n, backslash-t and backslash-r inside quotes into the characters.
- **The fix's testUnit red set** was exactly the ten `DeclaredNullabilityTest` cases the plan lists. They were changed (X-01 to X-10), each seen red on HEAD's production first, plus the new `escapedBacktickElementPairsWithoutAFallbackWarning`.
- **Coupling proof.** The decode and V1 reader with HEAD's `DeclaredNullability` fail the NAME-04 discovery rows for bs, bsn, bt, btbsn, cr, lf and tab (lf, tab and cr included), so the decode and the pairing key cannot land apart.
- **The COLS-13 twins and drift test** (X-11, X-12) now key on the raw element name and stay 33 tests, green; HEAD's version of the class fails exactly those three on the fix with `expected: not <null>`.
- **Full suite on exactly the commit tree:** 1746 tests in 99 XML files, 0 failures, 18 skipped, ClickHouse 26.9.19.9; post-fix re-render: all 60 name cells equal the decoded form on V2 and V1; anchors 90 rows, gate `result=PASS`; Spotless numstat equal to the 46-line base.
- **23-REVIEW IN-01 and IN-02** closed: the `apply` Javadoc no longer says all or none, and a null root name logs the fixed placeholder `<unnamed>`.

## Task Commits

1. **Task 1: tracer, decode end to end, red first** - no commit of its own (lands in the fix commit, as the plan directs)
2. **Task 2: changed pins, coupling proof, exemption list** - no commit of its own (lands in the fix commit)
3. **Task 3: anchors, ledger, loud failure, suite, commit, gates** - `d589234f` (fix, 8 files) and `b88859ef` (docs: E-05 to E-07 and the ledger)

**Plan metadata:** the `docs(28-03): complete ...` commit that carries this SUMMARY, STATE.md and ROADMAP.md.

## Files Created/Modified

- `src/main/java/.../db/helper/ClickHouseFieldDescriptor.java` - the names-only streaming decode
- `src/main/java/.../db/helper/ClickHouseHelperClient.java` - `readDescribeRows` shared by both describe paths; the unused `ClickHouseValue` and `JsonProcessingException` imports removed
- `src/main/java/.../db/mapping/DeclaredNullability.java` - rewritten `normalise`, `unescapeJsonLayer`, `UNNAMED_COLUMN`, the IN-01 Javadoc
- `RawNameMatrixFixturesUnitTest` (38 tests), `RawNameMatrixLiveTest` (25), `DeclaredNullabilityTest` (69), `NullableTupleMatrixLiveTest` (33) - the red-first rows and the named changed pins
- `.planning/scripts/fork-anchors.txt` - seven Phase 28 rows (90 total)
- `28-BEHAVIOUR-LEDGER.md` - L-58 to L-61; `28-COMMAND-EVIDENCE.md` - E-05 to E-07

## Decisions Made

- The decode slices non-name strings by token offsets rather than re-escaping, so the type text stays byte-identical to what the doubling replace produced (verified by the existing 272-row fixture suites).
- `unescapeJsonLayer` accepts only ASCII hex digits in the u escape and copies any unknown or truncated escape, so it is total.
- `ESCAPED_NEWLINE` stays public (tests and the multi-line type text still use it) although `normalise` no longer references it.

## Deviations from Plan

None - plan executed exactly as written.

Environment notes, not deviations: the first full-suite run on the staged tree ended with `Gradle Test Executor 161 finished with non-zero exit value 255` (the known OpenJ9 Mockito GPF; 1635 tests run, 0 failed). The core dump, javacore, Snap and jitdump files it left in the repository root were deleted and the suite was re-run once on the same unchanged staged tree; the counted run is the second (E-07 says so). `RawNameMatrix.java` needed no separate fix commit.

## Issues Encountered

None beyond the one OpenJ9 crash above.

## Known Stubs

None.

## Threat Flags

None. The change reads DESCRIBE rows already read before; `aMalformedRowFailsWithoutEchoingItsComment` pins T-28-05 (a sentinel comment is absent from the whole failure chain), and `normalise` and `unescapeJsonLayer` are single linear passes with no exception path (T-28-06).

## User Setup Required

None - no external service configuration required.

## Next Phase Readiness

- Plan 28-04 can pin live writes under the raw names on both clients: discovery matches `CREATE TABLE` names, including the `dq` tables that were unmapped.
- Plan 28-05 can reuse `DeclaredNullability.unescapeJsonLayer` in `Column`'s enum-label parser and fills the ledger's rollout lines; the FORK-DELTA, CHANGELOG and ROADMAP edits of NAME-07 land in `fix(28-05)`. Between the two commits FORK-DELTA rows 49 and 52 describe the earlier code; nothing is pushed.
- The named exemption list in E-06 is surfaced to the user for plan 28-06's end-of-phase human-check, not decided by them.

---
*Phase: 28-raw-identifier-names-in-discovery*
*Completed: 2026-10-23*

## Self-Check: PASSED

Verified: the three src/main files, the ledger and the evidence file exist; commits `d589234f` and `b88859ef` are ancestors of HEAD; the Task 1, Task 2 and Task 3 automated verifies printed DECODE-TRACER-RED-GREEN, DECODE-FIX-APPLIED, CHANGED-PINS-RED-GREEN, EXEMPTION-LIST-RECORDED, DECODE-ONE-COMMIT and DECODE-GATED.
