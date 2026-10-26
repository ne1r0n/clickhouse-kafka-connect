---
phase: 28-raw-identifier-names-in-discovery
plan: "05"
subsystem: sink-discovery
tags: [enum-labels, column, escape-aware-scan, raw-identifier, fork-delta, changelog, rollout-handoff, red-first]

requires:
  - phase: 28-raw-identifier-names-in-discovery
    provides: plan 28-03 fix (DeclaredNullability.unescapeJsonLayer, names-only decode), plan 28-02 RawNameMatrix catalog and E-04 spike record, plan 28-04 both-form live pins and E-08
provides:
  - Column.extractEnumValues as an escape-aware left-to-right scan, so an Enum8 or Enum16 label is keyed under its raw text on both clients
  - the apostrophe-label table, and labels holding a parenthesis, a comma or an equals sign with a negative code, read instead of unmapping the table
  - live and Docker-free pins for the raw label (12 cases), the pre-fix key form failing loudly (9 cases) and the ColumnTest row
  - NAME-07 documents in the fix commit: FORK-DELTA rows, Known costs item 11 closed, CHANGELOG line, ROADMAP 999.5 delivered, two fork-anchors rows (92)
  - measured full suite (1832 tests) and manual CLIENT_VERSION=V1 run (105 tests) on exactly the fix tree
  - ledger L-63 to L-65, six rollout lines and the 72-row per-character handoff table for Phase 29 (E-09, E-10)
affects: [28-06, phase-29-rollout]

actuals:
  tokens: 12000
  tasks: 3
  commits: 2

tech-stack:
  added: []
  patterns:
    - "Escape-aware scan after a single JSON-layer decode: a backslash always consumes the next character, malformed text fails with one fixed value-free message"
    - "Documents land in the fix commit after the measurements they cite; the commit tree is the tree the suite ran on"

key-files:
  created: []
  modified:
    - src/main/java/com/clickhouse/kafka/connect/sink/db/mapping/Column.java
    - src/test/java/com/clickhouse/kafka/connect/sink/db/mapping/ColumnTest.java
    - src/test/java/com/clickhouse/kafka/connect/sink/db/helper/RawNameMatrixFixturesUnitTest.java
    - src/test/java/com/clickhouse/kafka/connect/sink/RawIdentifierNameLiveTest.java
    - .planning/scripts/fork-anchors.txt
    - docs/FORK-DELTA.md
    - CHANGELOG.md
    - .planning/ROADMAP.md
    - .planning/phases/28-raw-identifier-names-in-discovery/28-BEHAVIOUR-LEDGER.md
    - .planning/phases/28-raw-identifier-names-in-discovery/28-COMMAND-EVIDENCE.md

key-decisions:
  - "The scan decodes the type text with DeclaredNullability.unescapeJsonLayer (no new file), reads n, t and r escapes as the characters and any other backslash pair as the character after the backslash, and throws IllegalArgumentException(\"Enum type could not be read\") with no label and no type text for text it cannot read"
  - "A value in the earlier V2 key form is pinned as failing loudly (the Phase 27 lookup miss), so Phase 29 states the enum-label outcome as measured; the clean stop in both directions is labelled reasoned, not measured"
  - "The ColumnTest row uses java.util.Map.of fully qualified so the already Spotless-red file gains no import line"

requirements-completed: [NAME-06, NAME-07]

coverage:
  - id: D1
    description: "An Enum8 or Enum16 label with an escaped character is keyed under its raw text; a raw-label value lands and reads back code 1 for all 12 matrix entries"
    requirement: NAME-06
    verification:
      - kind: unit
        ref: "RawNameMatrixFixturesUnitTest#enumKeysAreTheRawLabels (12 cases); red on the unchanged Column for exactly bs, bsn, lf, tab, cr, sl, dq, ap, btbsn (E-09)"
        status: pass
      - kind: integration
        ref: "RawIdentifierNameLiveTest#rawEnumLabelLandsAndReadsBackTyped (12 cases); red on the unchanged Column for the same nine (E-09)"
        status: pass
    human_judgment: false
  - id: D2
    description: "A value in the pre-fix label key form fails the put loudly and stores no row (9 entries); labels holding a parenthesis, a comma or an equals sign and a negative code are read"
    requirement: NAME-06
    verification:
      - kind: integration
        ref: "RawIdentifierNameLiveTest#legacyEnumLabelKeyFailsLoudly (9 cases); red on the unchanged Column for bs, bsn, lf, tab, cr, sl, dq, btbsn (E-09)"
        status: pass
      - kind: unit
        ref: "ColumnTest#enumLabelsHoldingAParenthesisACommaOrAnEqualsSignAreRead; red on the unchanged Column (E-09)"
        status: pass
    human_judgment: false
  - id: D3
    description: "The fix commit carries the FORK-DELTA rows, Known costs item 11 closed, the CHANGELOG line, ROADMAP 999.5 delivered and the 92-row anchors; the full suite and the manual V1 run are green on its exact tree"
    requirement: NAME-07
    verification:
      - kind: other
        ref: "Task 3 verify NAME06-07-ONE-COMMIT and NAME07-GATED; suite 1832 tests 0 failed, V1 run 105 tests 0 failed, tail gate anchor_rows=92 result=PASS (E-10)"
        status: pass
    human_judgment: false
  - id: D4
    description: "The Phase 29 handoff record: L-63 to L-65, six rollout lines and the 72-row old/new name table for both clients, with the clean stop labelled reasoned"
    requirement: NAME-07
    verification: []
    human_judgment: true
    rationale: "The rollout wording is Phase 29's; whether the handoff record is sufficient for the operator note is a reviewer judgment (28-06 human-check)"

plan_head_before: 9d9f8087049a3e55004e587c71306967deb2227b
plan_head_after: 06ad9c4c94beb7a1144c17320b0f820e5b6350da

duration: 25min
completed: 2026-10-23
status: complete
---

# Phase 28 Plan 05: Enum labels under the raw label and backlog 999.5 closed Summary

**Column.extractEnumValues is now an escape-aware scan over the JSON-decoded type text, so Enum8 and Enum16 labels are keyed under their raw text (an apostrophe label no longer unmaps its table), and the FORK-DELTA, CHANGELOG, ROADMAP and anchors land in the same commit as the fix, measured on the exact tree.**

## Performance

- **Duration:** about 25 min (start not recorded at launch; estimated from the previous plan's last evidence timestamp, 10:21Z)
- **Completed:** 2026-10-23T10:46Z
- **Tasks:** 3 (one tracer, two auto)
- **Files modified:** 10 (one src/main file; four test classes touched; anchors, three documents, ledger, evidence file)

## Accomplishments

- **Tracer, red first (E-09).** `enumKeysAreTheRawLabels` and `rawEnumLabelLandsAndReadsBackTyped` failed on the unchanged Column for exactly the nine entries the E-07 re-render predicted (bs, bsn, lf, tab, cr, sl, dq, ap, btbsn): a key-map mismatch Docker-free, the Phase 27 lookup miss live, and a `NumberFormatException` / unmapped table for the apostrophe. With the scan all 114 tests of the two classes pass.
- **Remaining pins.** `legacyEnumLabelKeyFailsLoudly` (9 cases) is red for the eight entries whose table is mapped on the unchanged Column and green for `ap` (unmapped there, so the put fails as the pin expects); the `ColumnTest` row for `)`, `,`, `=` and a negative code is the only red case of that class. The re-applied `src/main` diff is byte-identical to the saved patch. The four WRT-18 lookup-miss pins stayed green; `Utils.java` is untouched.
- **One fix commit, documents inside it.** `d7361cbb` holds Column.java, three test classes, `fork-anchors.txt` (92 rows), FORK-DELTA (999.5 row, name-matrix harness row, describe-reader, declared-nullability and adopted-wholesale rows now 36/14/13, Known costs item 11 in its closed form, the gg4 next-syncer sentence), the CHANGELOG bullet (gg4 section 4 bullets, released sections byte-identical) and ROADMAP's 999.5 Delivered line. No VERSION bump, no rollout note.
- **Measured on that exact tree.** Full suite 1832 tests in 100 XML files, 0 failures, 18 skips, server 26.9.19.9; manual `CLIENT_VERSION=V1` run of `RawIdentifierNameLiveTest`, `RawNameMatrixLiveTest` and `TableTest` 105 tests green (writer frames show `doInsertRawBinaryV1`); the re-render shows all 72 cells raw under both clients; Spotless numstat equal to the 46-line base; tail gate `anchor_rows=92 result=PASS`.
- **Phase 29 handoff (D-06).** Ledger rows L-63 to L-65, six rollout lines (rename before upgrading; root silent and element loud; enum outcome; the clean stop in both directions labelled `reasoned from the BEFORE/AFTER state machine, not measured`; no repair of stored rows; what was not measured) and the 72-row old/new table for both clients.

## Task Commits

1. **Tasks 1 and 2: tracer and remaining pins** - no commit of their own (the plan lands them in the Task 3 fix commit)
2. **Task 3: scan, pins, anchors and documents in one commit** - `d7361cbb` (fix)
3. **Task 3: E-09, E-10, ledger and handoff** - `06ad9c4c` (docs)

**Plan metadata:** the `docs(28-05): complete ...` commit that carries this SUMMARY, STATE.md and ROADMAP.md.

## Files Created/Modified

- `src/main/java/com/clickhouse/kafka/connect/sink/db/mapping/Column.java` - `extractEnumValues` scan, `MALFORMED_ENUM_TYPE_MESSAGE`, char constants, `Utils` import removed
- `src/test/java/com/clickhouse/kafka/connect/sink/db/helper/RawNameMatrixFixturesUnitTest.java` - `enumKeysAreTheRawLabels` (12)
- `src/test/java/com/clickhouse/kafka/connect/sink/RawIdentifierNameLiveTest.java` - `rawEnumLabelLandsAndReadsBackTyped` (12), `legacyEnumLabelKeyFailsLoudly` (9)
- `src/test/java/com/clickhouse/kafka/connect/sink/db/mapping/ColumnTest.java` - `enumLabelsHoldingAParenthesisACommaOrAnEqualsSignAreRead`
- `.planning/scripts/fork-anchors.txt`, `docs/FORK-DELTA.md`, `CHANGELOG.md`, `.planning/ROADMAP.md` - NAME-07 documents
- `28-BEHAVIOUR-LEDGER.md`, `28-COMMAND-EVIDENCE.md` - L-63 to L-65, rollout lines, handoff table, E-09, E-10

## Decisions Made

- See `key-decisions`. The scan reuses `DeclaredNullability.unescapeJsonLayer`, so the label scan and `normalise` cannot disagree on the JSON layer. Control characters outside the nine were not measured; a label holding one is a loud lookup miss and Known costs item 11 says so.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 3 - Blocking] ColumnTest import moved the Spotless numstat**
- **Found during:** Task 3, step 9 (SPOTLESS-CHECK on the fix commit)
- **Issue:** The new `ColumnTest` row used `Map.of` with an added `import java.util.Map;`. ColumnTest is among the 46 files already Spotless-red, so the extra import line changed its numstat line from `5 7` to `6 8`; the phase's rule allows such a change only for Column.java and ClickHouseHelperClient.java. The plan's own wording ("no import change") anticipated this.
- **Fix:** Replaced the import with `java.util.Map.of(` in that one row and amended the unpushed fix commit (`714e17c0` -> `d7361cbb`). Re-ran the full suite, the manual V1 run, the re-render, Spotless and the tail gate on the amended tree; the counted figures are from that second run (the first run's were identical).
- **Files modified:** `src/test/java/com/clickhouse/kafka/connect/sink/db/mapping/ColumnTest.java`
- **Verification:** SPOTLESS-CHECK `new [] changed []`; NAME06-07-ONE-COMMIT and NAME07-GATED printed
- **Committed in:** `d7361cbb`

---

**Total deviations:** 1 auto-fixed (1 blocking)
**Impact on plan:** None on scope or behaviour; one test line changed form. The tree equality the documents rely on holds for the amended commit.

## Issues Encountered

None beyond the deviation. No OpenJ9 crash occurred in any Gradle run; no core, javacore, Snap or jitdump file appeared in the repository root.

## Known Stubs

None.

## Threat Flags

None. The new exception text is a fixed constant with no label and no type text (T-28-12); the scan is one pass in which a backslash always consumes the next character and malformed text fails with one fixed exception (T-28-13); typed read-back of the code and the label covers all twelve entries (T-28-14); the suite and V1 run ran on the staged tree before the documents were committed (T-28-15).

## User Setup Required

None - no external service configuration required.

## Next Phase Readiness

- Plan 28-06 can gate the phase: 92 anchor rows hold, the ledger carries the handoff record, `docs/ROLLOUT-v1.7.0_gg4.md` is not written and VERSION is unchanged, and nothing is pushed.
- Phase 29 writes the rollout note from the ledger's rollout lines and the 72-row table; the clean stop is the one item it must keep labelled as reasoned.

---
*Phase: 28-raw-identifier-names-in-discovery*
*Completed: 2026-10-23*

## Self-Check: PASSED

Verified: `Column.java`, `ColumnTest.java`, `RawNameMatrixFixturesUnitTest.java`, `RawIdentifierNameLiveTest.java`, the ledger and the evidence file exist; commits `d7361cbb` and `06ad9c4c` are ancestors of HEAD; ENUM-TRACER-RED-GREEN, ENUM-PINS-RED-GREEN, NAME06-07-ONE-COMMIT-equivalent checks and NAME07-GATED printed.
