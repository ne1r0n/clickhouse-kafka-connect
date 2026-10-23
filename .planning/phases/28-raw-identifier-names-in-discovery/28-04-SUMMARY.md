---
phase: 28-raw-identifier-names-in-discovery
plan: "04"
subsystem: testing
tags: [live-test, clickhouse-base, rowbinary, raw-identifier, doubled-form, red-first, name-matrix]

requires:
  - phase: 28-raw-identifier-names-in-discovery
    provides: plan 28-03 fix (raw identifier names in discovery on both clients), plan 28-02 RawNameMatrix catalog and E-04 predictions
provides:
  - RawIdentifierNameLiveTest, 52 live cases through the real ClickHouseSinkTask on the default RowBinary route with typed SQL read-back
  - the raw form pinned for all twelve spiked characters at the root and in Tuple, Array(Tuple), Nullable(Tuple) and Map-value Tuple
  - the doubled form pinned as the fix leaves it, root field ignored with the DEFAULT stored, Tuple element failing loudly in every shape
  - E-08 (red on the pre-fix production, green, gate) and ledger row L-62
affects: [28-05, 28-06, phase-29-rollout]

actuals:
  tokens: 6700
  tasks: 2
  commits: 2

tech-stack:
  added: []
  patterns:
    - "Both-form pin: one table from the spike's DDL, a record built from the matrix names, typed toString read-back of every column"
    - "A failure pin must name the column of the case under test, otherwise sibling columns of the same record fail it for the wrong reason"

key-files:
  created:
    - src/test/java/com/clickhouse/kafka/connect/sink/RawIdentifierNameLiveTest.java
  modified:
    - .planning/phases/28-raw-identifier-names-in-discovery/28-BEHAVIOUR-LEDGER.md
    - .planning/phases/28-raw-identifier-names-in-discovery/28-COMMAND-EVIDENCE.md

key-decisions:
  - "The doubled-element pin also requires the cause chain to hold the writer's `column '<container path>.<raw element>'` text; without it the pin is vacuous (it passes on the pre-fix production because the record's raw-named sibling containers fail the put)"
  - "The class prints one metadata line per failing element case (exception class, root cause class, first line of the root cause message); the message names a column path, never a value"

requirements-completed: [NAME-05]

coverage:
  - id: D1
    description: "A record using the raw names lands and reads back typed equal at the root and in all four Tuple shapes, with no discovery WARN, for all 12 matrix entries"
    requirement: NAME-05
    verification:
      - kind: integration
        ref: "RawIdentifierNameLiveTest#rawNamesLandAndReadBackTyped (12 cases); red on the pre-fix production for exactly bs, bsn, bt, btbsn, cr, dq, lf, sl, tab (E-08)"
        status: pass
    human_judgment: false
  - id: D2
    description: "A root field named in the doubled form is ignored and the column's DEFAULT 99 is stored while the raw-named Tuple columns land (8 entries)"
    requirement: NAME-05
    verification:
      - kind: integration
        ref: "RawIdentifierNameLiveTest#doubledRootNameStoresTheDefault (8 cases); red on the pre-fix production for all 8 (E-08)"
        status: pass
    human_judgment: false
  - id: D3
    description: "An element named in the doubled form fails the put and leaves no row in a Tuple, an Array(Tuple), a Nullable(Tuple) and a Map-value Tuple (32 cases)"
    requirement: NAME-05
    verification:
      - kind: integration
        ref: "RawIdentifierNameLiveTest#doubledElementNameFailsLoudly (32 cases); red on the pre-fix production for all 32 (E-08)"
        status: pass
    human_judgment: false
  - id: D4
    description: "testUnit 1232 tests unchanged, Spotless equal to the base, tail gate anchor_rows=90 result=PASS, no src/main change"
    verification:
      - kind: other
        ref: "Task 2 verify BOTH-FORMS-PINNED and NAME05-GATED"
        status: pass
    human_judgment: false

plan_head_before: 1ee032bc76d65e24ec310d17d001d854f2d38fca
plan_head_after: 95e9a27b893e19d9ddaafad5742d19f43082920c

duration: 12min
completed: 2026-10-23
status: complete
---

# Phase 28 Plan 04: Both name forms pinned live (NAME-05) Summary

**RawIdentifierNameLiveTest pins, through the real sink task with typed read-back, that a record under the raw names lands for all twelve spiked characters in every Tuple shape, that a doubled-form root field is ignored with the DEFAULT stored, and that a doubled-form Tuple element fails the put loudly.**

## Performance

- **Duration:** about 12 min (start not recorded at launch; estimated from the previous plan's last commit at 10:10Z)
- **Completed:** 2026-10-23T10:22Z
- **Tasks:** 2 (one tracer, one auto)
- **Files modified:** 3 (one new test class, the ledger, the evidence file); no src/main file

## Accomplishments

- **Raw form, 12 cases.** For every matrix entry the record built from `RawNameMatrix` names lands, `toString` of the root column and the four Tuple columns reads back equal (`7`, `(1,'t')`, `[(2,'a')]`, `(3,'n')`, `{'k':(4,'m')}`), and a direct `describeTable` plus the put log no WARN on the helper or `DeclaredNullability` loggers. For `dq` this is the first record ever stored into a table with a double quote in a name.
- **Doubled root, 8 cases.** bs, bsn, lf, tab, cr, sl, dq and btbsn: the field named `doubledForm(rootName)` is ignored and the column reads back its DEFAULT 99, while the four raw-named containers land.
- **Doubled element, 32 cases.** The same 8 entries across `tuple`, `array_tuple`, `nullable_tuple` and `map_tuple`: the put throws a `RuntimeException` (first message `Number of records: 1`) whose root cause is a `DataException` reading `column '<path>' (INT32): no value provided for a non-nullable column without a default; ...`, and no row is stored. The message names a column path, not a value.
- **Red first (E-08).** On the pre-fix production (the three files of `fix(28-03)` restored from `F^`) the class ran 52 tests with 49 failed: the raw cases for exactly bs, bsn, bt, btbsn, cr, dq, lf, sl, tab, all 8 doubled-root cases and all 32 doubled-element cases; the raw and root sets equal the E-04 prediction. After `git checkout HEAD -- src/main` all 52 pass.
- **Gates.** testUnit 1232 tests, 0 failed (unchanged); Spotless numstat equal to the 46-line base with no new file; tail gate `anchor_rows=90 result=PASS`; `git log f3b1c487..HEAD -- src/main` lists only `fix(28-03)`.

## Task Commits

1. **Task 1: tracer, raw form of every entry through the real sink task** - no commit of its own (the class lands in the test commit, as the plan directs)
2. **Task 2: doubled-form pins, red proof, gates, E-08 and L-62** - `ba55f99b` (test, the new class alone) and `95e9a27b` (docs: L-62 and E-08)

**Plan metadata:** the `docs(28-04): complete ...` commit that carries this SUMMARY, STATE.md and ROADMAP.md.

## Files Created/Modified

- `src/test/java/com/clickhouse/kafka/connect/sink/RawIdentifierNameLiveTest.java` - the three parameterized pins and the record builders
- `.planning/phases/28-raw-identifier-names-in-discovery/28-BEHAVIOUR-LEDGER.md` - row L-62
- `.planning/phases/28-raw-identifier-names-in-discovery/28-COMMAND-EVIDENCE.md` - E-08, index row flipped to pass

## Decisions Made

- The doubled-element pin names the column of the container under test (see the deviation below).
- One metadata line per doubled-element case (`NAME05_DOUBLED_ELEMENT key=... container=... class=... root_cause=... message=...`) carries the measured failure class and first message line; it contains a column path from the matrix, never a record value.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 1 - Bug] The doubled-element pin as specified was vacuous on the pre-fix production**
- **Found during:** Task 2, step 4 (the red proof)
- **Issue:** The plan has one container named in the doubled form and the other three raw, with `assertPutFails` and no substring, and predicts the pin red before the fix except in 6 cases. The first pre-fix measurement (52 tests, 17 failed) showed all 32 element cases green. Before the fix the doubled form is the one that matches, so the three raw-named sibling containers of the same record fail the put on their own, and the pin passed regardless of the container under test. A vacuous pin is what threat T-28-11 forbids, and the plan's stop rule applies to a measured set that differs from the prediction.
- **Fix:** Investigated rather than loosened: the pin was strengthened. `assertPutFails` now also requires the cause chain to contain `column '<container path>.<raw element name>'` (`t.`, `a.`, `n.` or `m.values.` plus the element), the writer's report for the container under test. With it all 32 cases pass on HEAD and all 32 fail on the pre-fix production (the failure names another column, or the table is unmapped for dq). The prediction script states the new rule (`element_green` is empty); the raw and doubled-root predictions are unchanged. The first measurement is kept as `/tmp/gsd28/exec/prefix-red-28-04-design1.xml` and recorded in E-08.
- **Files modified:** `src/test/java/com/clickhouse/kafka/connect/sink/RawIdentifierNameLiveTest.java`
- **Verification:** BOTH-FORMS-PINNED with the updated prediction; E-08 verdict pass
- **Committed in:** `ba55f99b`

**2. [Rule 3 - Blocking] Tracer run filtered by method**
- **Found during:** Task 1
- **Issue:** The plan has the tracer run expect 12 tests, but the class already held all three pins when it was first run.
- **Fix:** Ran the tracer with `--tests '*RawIdentifierNameLiveTest.rawNamesLandAndReadBackTyped'`, which is the 12-test run the verify reads. Both the 12-test and the 52-test runs were green.
- **Files modified:** none
- **Committed in:** not applicable

---

**Total deviations:** 2 (1 bug in the plan's prediction and pin design, 1 run-filter adjustment)
**Impact on plan:** The strengthened pin is the only change to what the plan asked for; it makes the rollout statement "a Tuple element fails loudly" verifiable per shape. The plan's wording that the writer's failure text is "recorded, not pinned" no longer holds for the column-path fragment, which is now pinned. No src/main change.

## Issues Encountered

- One red-proof attempt used an unset shell variable for `F` in a fresh shell, restoring the wrong content and failing to compile; it was reverted with `git checkout HEAD -- src/main` and redone with `F` set and file lengths checked. Its stale XML was never used.

## Known Stubs

None.

## Threat Flags

None. The class only adds tests; the failure line prints a column path and exception class names, never a record value (T-28-10 and T-28-11 are covered by the pins).

## User Setup Required

None - no external service configuration required.

## Next Phase Readiness

- Plan 28-05 can state the root-silent versus element-loud behaviour from L-62 and run the manual `CLIENT_VERSION=V1` pass of this class; FORK-DELTA, CHANGELOG and ROLL entries cite L-62.
- The wording in plan 28-05 or the rollout note that the doubled-element failure is "the writer's error" now has a measured form: `DataException`, `no value provided for a non-nullable column without a default`, naming the raw column path.

---
*Phase: 28-raw-identifier-names-in-discovery*
*Completed: 2026-10-23*

## Self-Check: PASSED

Verified: `RawIdentifierNameLiveTest.java`, the ledger and the evidence file exist; commits `ba55f99b` and `95e9a27b` are ancestors of HEAD; the Task 1 and Task 2 automated verifies printed RAW-FORM-TRACER-GREEN, BOTH-FORMS-PINNED and NAME05-GATED.
