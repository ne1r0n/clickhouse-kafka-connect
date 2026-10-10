---
phase: 15-suite-gate-publication-rollout
plan: 06
subsystem: docs-fork-delta
tags: [fork-delta, pub-02, known-costs, skip-inventory, raw-numbers, grep-confirmation]

requires:
  - phase: 15-suite-gate-publication-rollout
    provides: 15-01 (D-01 and D-02 code), 15-02 (review, IN-01), 15-03 (VERSION, ledger L-20..L-22, closed dispositions), 15-04 (audit threats), 15-05 (rollout checklist and CHANGELOG)
  - phase: 13-fork-hardening-on-the-merged-tree
    provides: behaviour ledger L-01..L-19 and the review dispositions
provides:
  - docs/FORK-DELTA.md describing the v1.7.0 merged and hardened tree, every row grep-confirmed at this plan's HEAD
  - Known costs and open items listing exactly what is knowingly open in the published tree
  - Re-measured raw numbers (before v1.6.0 to pre-v1.7.0-sync, after v1.7.0 to HEAD) with the four commands printed beneath them
  - Named skip inventory re-confirmed at 18 skips (7 disabled methods + 11 cases behind 1 assumeTrue site), the reference the 15-07 gate compares both runs against
affects: [15-07, 15-08]

plan_head_before: e592776e7294e99872561f070eea0213b7b66f44
plan_head_after: 670c601df16d23581ec830ae1aacb4de1198e84b

actuals:
  tokens: 7600
  tasks: 2
  commits: 2

tech-stack:
  added: []
  patterns:
    - "Docs claims re-derived by command before writing: git diff byte-identity, shortstat triples asserted against the table by a verify command, backticked-identifier sweep over the whole file"

key-files:
  created: []
  modified:
    - docs/FORK-DELTA.md

key-decisions:
  - "The raw-numbers prose states plainly that the delta against upstream is larger after this release at both scopes, so last sync's smaller-delta result does not carry over; the claim now rests on the retired rows"
  - "Known costs gains item 6 (audit threats T-13-19 and T-14-16 left open below the block threshold) beyond the plan's list, because they are operator-visible or repo-visible and the section is meant to be exactly what is knowingly open"
  - "FORK-DELTA wording never names a ClickHouse patch version: the D-17 row now says 26.9, not the four-part server build it quoted"

requirements-completed: [PUB-02]

coverage:
  - id: D1
    description: "docs/FORK-DELTA.md introduction, patch rows, retired rows and next-syncer notes describe the v1.7.0 tree: sync/v1.7.0, merge commit 54e63648, release v1.7.0_gg1, rollout doc link, CI-08/09/10, lz4 1.12.0, Jackson 2.22.3, INT256/UINT256, any depth, nested-null split, WARN row"
    requirement: PUB-02
    verification:
      - kind: other
        ref: "Task 1 automated check: required-string loop printed no MISSING line; 'five guard sites' count 0; 'reads `v1.6.0_gg1`' count 0"
        status: pass
    human_judgment: false
  - id: D2
    description: "The four raw-number rows equal git diff --shortstat output for the four printed commands"
    requirement: PUB-02
    verification:
      - kind: other
        ref: "Task 1 automated check: RAW-NUMBERS-MATCH printed"
        status: pass
    human_judgment: false
  - id: D3
    description: "Known costs lists exactly the knowingly-open items (999.1 limitation and WARN, 13-REVIEW WR-01 and WR-03, JSON-route gaps L-03/L-04/L-05/L-22, 999.2, upstream capability not adopted), R-01 and the tearDown item are closed, P9-IN-01 is described as evaluation order"
    requirement: PUB-02
    verification:
      - kind: other
        ref: "Task 2 automated check: required strings all present in the Known costs section; 'silently discarded' and 'swallows a database-drop failure' absent from it"
        status: pass
    human_judgment: false
  - id: D4
    description: "Skip basis unchanged: 7 @Disabled sites and 1 Assumptions.assumeTrue site, 18 named skips"
    requirement: PUB-02
    verification:
      - kind: other
        ref: "grep -rc '@Disabled' src/test/java summed to 7; grep -rc 'Assumptions.assumeTrue' src/test/java summed to 1; exactlyOnceStateMismatchTestArgs holds 11 cases; 'The current suite skips 18 tests' present"
        status: pass
    human_judgment: false
  - id: D5
    description: "No internal host, address, connector name or production version appears in the file"
    requirement: PUB-02
    verification:
      - kind: other
        ref: "PUBLIC-SCAN regex over docs/FORK-DELTA.md printed nothing (grep rc 1)"
        status: pass
    human_judgment: false
  - id: D6
    description: "FORK-DELTA is accurate at publication, and the new prose (Known costs wording, the larger-delta paragraph, the nested-null row) is adequate for the next syncer"
    verification: []
    human_judgment: true
    rationale: "The plan flags PUB-02 as an unresolved probe item: a claim can go stale if code changes after this plan, and wording adequacy is a maintainer judgment that the greps do not test"

duration: 35min (estimated; the start time was not captured)
completed: 2026-10-10
status: complete
---

# Phase 15 Plan 06: FORK-DELTA for the v1.7.0 tree Summary

**docs/FORK-DELTA.md now describes the merged and hardened `sync/v1.7.0` tree published as `v1.7.0_gg1`: stale v1.6.0 rows rewritten, R-01 and the tearDown item closed, the Nullable Enum/Tuple limitation and the JSON-route gaps listed as knowingly open, raw numbers re-measured, and the 18-skip inventory re-confirmed for the 15-07 gate.**

## Performance

- **Duration:** about 35 min (estimated)
- **Completed:** 2026-10-10
- **Tasks:** 2
- **Files modified:** 1 (`docs/FORK-DELTA.md`, 61 insertions and 47 deletions across both commits)

## Accomplishments

- **Introduction and rows.** Intro names `sync/v1.7.0`, merge commit `54e63648` and `v1.7.0_gg1`, and links `docs/ROLLOUT-v1.7.0_gg1.md`. The metadata-free row now describes routing on `shouldSkipTableMetadata()` (8 lines of `ClickHouseWriter.java`: the definition plus seven reads), `MetadataFreeJsonWriter`, the Debezium encodings, the empty-CDC skip and the null semantics. The fail-fast row covers INT256/UINT256, the describe-skip row says any depth, and new rows cover the nested-null split and the Nullable Enum/Tuple discovery WARN. CI row names CI-08, CI-09, CI-10; VERSION and connector-name rows read `v1.7.0_gg1`; the dependency row adds lz4 1.12.0, Jackson 2.22.3 and the test-side bumps with the 1.11.4 floor correction.
- **Retired rows.** D-17 row kept and consistent with Recorded Facts 25 and 26; the lz4 row names `lz4Java = "1.12.0"` selected by upstream's group-based capability rule. "Adopted wholesale" re-measured against `v1.7.0`.
- **Known costs.** Rewritten as the knowingly-open list; see "Known costs as published" below.
- **Skip inventory.** Unchanged and re-confirmed.

## Raw numbers as measured (HEAD `e592776e` tree for `src`, build and CI; unchanged by the two docs commits)

```text
git diff --shortstat v1.6.0 pre-v1.7.0-sync -- src build.gradle.kts gradle .github   84 files changed, 9476 insertions(+), 451 deletions(-)
git diff --shortstat v1.7.0 HEAD            -- src build.gradle.kts gradle .github   95 files changed, 13587 insertions(+), 484 deletions(-)
git diff --shortstat v1.6.0 pre-v1.7.0-sync -- src/main                              30 files changed, 1368 insertions(+), 98 deletions(-)
git diff --shortstat v1.7.0 HEAD            -- src/main                              31 files changed, 1826 insertions(+), 130 deletions(-)
```

The document prints the after ref as `v1.7.0_gg1`, as the plan prescribes; that tag does not exist yet (publication is 15-08), so the after rows were measured with `HEAD`. The before rows equal the previous sync's published after rows, as they should, since `pre-v1.7.0-sync` is the tree `v1.6.0_gg1` came from. `RAW-NUMBERS-MATCH` printed against the final file.

Other re-measurements: `Table.java`, `Processing.java` and `KeyToValue.java` are byte-identical to `v1.7.0` (`git diff --quiet v1.7.0 HEAD -- <path>` exit 0 for each). `src/main`: 63 files at `v1.7.0`, 38 identical, 25 modified (15 import-order only, 10 with fork logic), 6 added. Test tree: 143 files upstream added or changed between `v1.2.9` and `v1.7.0`, 128 identical, 15 differ (13 adapted, 2 import-order only).

## Skip basis

`grep -rc '@Disabled' src/test/java` summed to **7**; `grep -rc 'Assumptions.assumeTrue' src/test/java` summed to **1**; `exactlyOnceStateMismatchTestArgs` holds **11** `new int[]` cases. The seven disabled methods were matched one by one to the table (`clientNameTest`, `schemaWithTupleOfMapsWithVariantTest`, `schemaWithNestedTupleMapArrayAndVariant`, `compareInsertPerformance`, `compareAllConfigurations`, `testWideStructPerformance`, `testDeepNestedStructPerformance`). 7 + 11 = 18; no wording change was needed, and the compile-only #800 and cluster note stays.

## Known costs as published

Closed (stated once, not listed as open): R-01 (routing, L-01, backfill in rollout item 2), the tearDown drop (DEBT-04, L-19), COLS-07, CFG-04, DEBT-02, P9-IN-01 (described as evaluation order; a null config still throws), and the map null-entry gap (WRT-07).

Open, numbered: (1) Nullable(Enum8|Enum16|Tuple) in D-15's words with 999.1, the WARN and the 15-REVIEW IN-01 route restriction; (2) 13-REVIEW WR-01; (3) 13-REVIEW WR-03; (4) JSON-route gaps L-03, L-04, L-05 narrowed by L-20, ByteBuffer, `input_format_null_as_default`, L-22 (with the note that no value pin exists for that combination); (5) review findings with no operator-visible effect (15-REVIEW IN-02..IN-06, pointer to both disposition files); (6) audit threats T-13-19 and T-14-16; (7) backlog 999.2; (8) Future Requirements; (9) upstream capability not adopted.

## Sweep

`python3 -I /tmp/gsd15/sweep.py` (a throwaway script, not committed): extracts every backticked token in `docs/FORK-DELTA.md`, checks file paths against `git ls-files` and identifiers against `git grep -F` over `src`, `build.gradle.kts`, `gradle`, `.github` and `VERSION`. Requirement, ledger and finding IDs, version strings, SQL and config values were skipped (93 tokens). **294 identifiers and paths were checked; 13 were not found, all expected:** retired items whose absence the document claims (`ClickHouseAPI.java`, `src/test/java/transforms/KeyToValueTest.java`, the `signing` and `com.github.gmazzo.buildconfig` plugins, verified absent separately), the generated `Version.java` (exists under `build/` after a build), elided `src/test/.../` paths, and names that are not repo identifiers (`ClickHouse/clickhouse-kafka-connect`, `custom_build`, `sync/v1.7.0`, the Gradle task `dependencyInsight`, the server setting `input_format_null_as_default`).

The sweep also found real drift, fixed before the commits: `Column` carries a second fork line (the COLS-07 argument order), `ClickHouseSinkConfig` carries the CFG-04 default, `Data.java` carries the `getSchema()` accessor, "seven tests pass a null connector name" was now five plus two `buildMBeanName(null, ...)` callers, and the D-17 row quoted a four-part server build that the public-scan regex would have flagged.

## Task Commits

1. **Task 1: introduction, patch rows, retired rows, raw numbers, next-syncer notes** - `6bdbfc82` (docs)
2. **Task 2: Known costs, skip inventory, sweep** - `670c601d` (docs)

**Plan metadata:** recorded in the docs commit that follows this file.

## Files Created/Modified

- `docs/FORK-DELTA.md` - rewritten for the v1.7.0 tree

## Decisions Made

- The raw-numbers paragraph says the delta grew rather than inventing a retirement story; the supporting claim is now the retired rows.
- Known costs item 6 (audit threats) was added beyond the plan's list; it is two sentences and points at the security files.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 2 - Missing critical] Rows the plan did not name were stale and were corrected**
- **Found during:** Task 1 and the Task 2 sweep
- **Issue:** after the v1.7.0 hardening, the config row (CFG-04), the `Type.DYNAMIC` row and the retired `Column` row ("the only fork line in `Column`"), the test-harness row (DEBT-04), the "Adopted wholesale" counts and the retired Map-null row (WRT-07) no longer matched the tree. A FORK-DELTA claim not confirmed against the tree violates the plan's own prohibition.
- **Fix:** corrected each from the tree, by diff against `v1.7.0` and by grep.
- **Files modified:** `docs/FORK-DELTA.md`
- **Committed in:** `6bdbfc82`, `670c601d`

**2. [Rule 2 - Missing critical] Known costs item 6 added (T-13-19, T-14-16)**
- **Found during:** Task 2
- **Issue:** the plan's open-items list omitted the two audit threats 15-04 left open, one of which is a record-data log line an operator can observe.
- **Fix:** one item pointing at `13-SECURITY.md` and `14-SECURITY.md`.
- **Committed in:** `670c601d`

---

**Total deviations:** 2 auto-fixed (both Rule 2, documentation accuracy). **Impact on plan:** no scope creep; both keep the file true of the tree.

## Issues Encountered

- **CI row has no test-class pin.** The acceptance criterion asks every rewritten row's "Pinned by" cell to name a requirement ID and an existing test class. The CI row is pinned by CI-05, CI-08, CI-09, CI-10 and Recorded Facts 15, 31 and 37, but no test class exercises a workflow file; that is a property of the row, not an omission.
- **Observation, not changed:** `jacocoForkReport`'s 15-class list in `build.gradle.kts` does not include `MetadataFreeJsonWriter`, the largest fork-only class. The FORK-DELTA build row still says "15-class" truthfully. Adding it is a build change and out of this plan's scope.
- **Observation for 15-07:** the after raw numbers describe `src`, build and CI at this plan's HEAD. If the gate changes any of them, the table must be re-measured (the `RAW-NUMBERS-MATCH` command in 15-06 Task 1 does it).

## Known Stubs

None.

## Threat Flags

None. T-15-16: every changed row was grep-confirmed and the raw numbers are asserted by `RAW-NUMBERS-MATCH`. T-15-17: the public scan printed nothing; no internal host, connector or production version is named.

## User Setup Required

None - no external service configuration required.

## Next Phase Readiness

- The Named skip inventory is the reference for 15-07's skip-set comparison: 18 entries, 7 disabled methods and `exactlyOnceStateMismatchTest` cases `[1]` to `[11]`.
- 15-07 runs the tree-wide public scan before the rehearsal push; FORK-DELTA itself is clean.
- Any `src`, build or CI edit after this point stales the after rows and re-opens the gate.

## Self-Check: PASSED

- `docs/FORK-DELTA.md` exists and is tracked; the required-string loop, `RAW-NUMBERS-MATCH`, the Known costs checks, the skip-basis check and the public scan all passed against the final file.
- Commits `6bdbfc82` and `670c601d` are ancestors of HEAD; `git rev-list --count e592776e..HEAD` was 2 before this file.
- Neither commit deleted a tracked file.

---
*Phase: 15-suite-gate-publication-rollout*
*Completed: 2026-10-10*
