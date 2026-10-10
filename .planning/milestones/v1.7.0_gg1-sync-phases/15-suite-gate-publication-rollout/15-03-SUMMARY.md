---
phase: 15-suite-gate-publication-rollout
plan: 03
subsystem: release-identity-and-records
tags: [version, fork-anchors, behaviour-ledger, review-disposition, merge-checklist, pub-01, pub-02, pub-03]

requires:
  - phase: 15-suite-gate-publication-rollout
    provides: plan 15-01 commits 981a89d6 (D-01 Debezium Timestamp, WR-02 fix) and 4a91b92c (D-02/D-15 WARN); plan 15-02 review confirming the code is settled
  - phase: 13-fork-hardening-on-the-merged-tree
    provides: behaviour ledger L-01..L-19, review disposition, evidence for the two pending ClickHouseWriter checklist rows
  - phase: 14-dependency-bumps-ci
    provides: 14-REVIEW findings to disposition
provides:
  - VERSION v1.7.0_gg1 with its BLD-07 anchor row, proven through a clean build (generated ARTIFACT_VERSION agrees)
  - Ledger rows L-20, L-21, L-22 and updated Known JSON-route gaps and Phase 15 destination notes
  - 13-REVIEW-DISPOSITION and 14-REVIEW-DISPOSITION closed (open 0 in YAML and table)
  - MERGE-CHECKLIST-v1.7.0.md with zero pending rows
affects: [15-04, 15-05, 15-06, 15-07, 15-08]

plan_head_before: 141db3893413e1d9d069756c6ec5088c3d20cba3
plan_head_after: 42026e808e926429f604aba24346992310171185

actuals:
  tokens: 5200
  tasks: 2
  commits: 3

tech-stack:
  added: []
  patterns:
    - "A release-identity change and its fork-anchors row land in one pure commit so invariant 3 holds at every commit"
    - "Disposition files edited in the YAML frontmatter and the Markdown table together, with the open count recomputed"

key-files:
  created: []
  modified:
    - VERSION
    - .planning/scripts/fork-anchors.txt
    - .planning/phases/13-fork-hardening-on-the-merged-tree/13-BEHAVIOUR-LEDGER.md
    - .planning/phases/13-fork-hardening-on-the-merged-tree/13-REVIEW-DISPOSITION.md
    - .planning/phases/14-dependency-bumps-ci/14-REVIEW-DISPOSITION.md
    - .planning/MERGE-CHECKLIST-v1.7.0.md

key-decisions:
  - "L-22 records plainly that no test pins stored values for explicit insertFormat=json plus bypassRowBinary=true with Struct/CDC records; only the routing is pinned"
  - "13-REVIEW WR-02 is the only finding marked fixed (commit 981a89d6); every other finding in both phases is deferred with a one-line reason"

requirements-completed: [PUB-01, PUB-02, PUB-03]

coverage:
  - id: D1
    description: "VERSION reads v1.7.0_gg1 with one trailing newline, the fork-anchors VERSION row moved with it in one commit, and a clean build generates ARTIFACT_VERSION = v1.7.0_gg1"
    requirement: PUB-01
    verification:
      - kind: other
        ref: "VERSION-COMMIT-OK printed (commit c750ed1a carries exactly VERSION and fork-anchors.txt)"
        status: pass
      - kind: other
        ref: "env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew clean compileJava testUnit --rerun, then grep ARTIFACT_VERSION in generated Version.java"
        status: pass
      - kind: other
        ref: ".planning/scripts/verify-merge-invariants.sh result=PASS"
        status: pass
    human_judgment: false
  - id: D2
    description: "Ledger rows L-20, L-21, L-22 exist in the seven-column shape, L-01..L-19 and the R-01 backfill procedure are untouched, and the review dispositions and checklist rows are closed"
    requirement: PUB-02
    verification:
      - kind: other
        ref: "LEDGER-OK, DISPOSITIONS-OK and CHECKLIST-ZERO-PENDING printed; no removed line from L-01..L-19 or the R-01 section"
        status: pass
    human_judgment: false
  - id: D3
    description: "The one-line deferral reasons for the Phase 13 and 14 review findings, and the wording of L-20..L-22, are accurate and adequate for the public CHANGELOG and FORK-DELTA that copy them"
    verification: []
    human_judgment: true
    rationale: "Whether a deferral reason is acceptable is a maintainer judgment; the checks verify structure and agreement, not the adequacy of the reasoning"

duration: 14min
completed: 2026-10-10
status: complete
---

# Phase 15 Plan 03: Release identity and closed records Summary

**VERSION is now `v1.7.0_gg1` with its anchor row in the same commit and a clean build that generates the matching `ARTIFACT_VERSION`; the behaviour ledger gained L-20..L-22, both review dispositions are closed at open 0, and the merge checklist has no pending rows.**

## Performance

- **Duration:** 14 min
- **Tasks:** 2
- **Files modified:** 6

## Accomplishments

- PUB-01: `VERSION` holds `v1.7.0_gg1` plus one newline. The `fork-anchors.txt` row is now `VERSION<TAB>v1.7.0_gg1<TAB>1<TAB>BLD-07`. Both moved in commit `c750ed1a`, whose diff is exactly those two files. `grep -rn 'v1.6.0_gg1' src/main src/test src/testFixtures` prints nothing.
- Generated line after `clean compileJava`: `public static final String ARTIFACT_VERSION = "v1.7.0_gg1";`. `testUnit` after that clean build: 454 tests, 0 failures, 0 errors, 2 skipped (the MBean-name unit test reads the constant dynamically and passes).
- Ledger: L-20 (Debezium Timestamp/NanoTimestamp decimal seconds, WR-02), L-21 (the Nullable Enum/Tuple WARN with the D-15 wording), L-22 (explicit json plus `bypassRowBinary=true` with Struct/CDC now on the metadata-free route). The intro, the D-18 bullet under Known JSON-route gaps, and the Phase 15 destination lists were annotated; no row L-01..L-19 and nothing in the R-01 backfill section was removed or changed.
- Dispositions: see counts below.
- MERGE-CHECKLIST: the two Phase-13-owned `ClickHouseWriter.java` rows are `done`.

## The L-22 pin finding

`MetadataFreeJsonInsertUnitTest#schemaAndCdcRecordsRouteToJsonOnlyWhenMetadataFreeOrBypassed` has the case "SCHEMA + insertFormat=json + bypassRowBinary=true -> JSON", but it only counts `doInsertJson` versus `doInsertRawBinary` calls on a recording writer. No test pins stored values for that exact combination. Live tests that read stored values (`MetadataFreeJsonTypeParityLiveTest`, `ExplicitJsonInsertPathLiveTest`) set `insertFormat=json` and leave `bypassRowBinary` unset; live tests that set `bypassRowBinary=true` (`JsonDynamicRoundTripTest`, `RowBinaryFailFastLiveTest`) leave `insertFormat` unset. The ledger row says "no value pin for this combination". Plan 15-05 should treat this as the item to cover in the D-07 pre-upgrade check and the JSON-route notes.

## Final disposition counts

| File | fixed | deferred | open | total |
|------|-------|----------|------|-------|
| 13-REVIEW-DISPOSITION.md | 1 (WR-02, commit `981a89d6`) | 8 (WR-01, WR-03, WR-04, WR-05, IN-01..IN-04) | 0 | 9 |
| 14-REVIEW-DISPOSITION.md | 0 | 6 (WR-01, WR-02, IN-01..IN-04) | 0 | 6 |

WR-05 carries exactly `WARN added (D-02); root cause backlog 999.1`. Frontmatter and table agree in both files.

## Evidence named in the two checklist rows

- Hunk `@@ -556,0 +558,6 @@` (the #820 shadow): `79664ce3` (12-02 scoped the MAP guard to Nullable columns, WRT-06/WRT-09); `45db0bec` (13-03, WRT-07), `0f5ebe6f` and `1ffc147a` (13-03, WRT-08 and D-09), `1df97132` (13-03 live pins). Tests: `NestedNullableCompositeWriteTest`, `NestedNullRowBinaryLiveTest`.
- Hunk `@@ -1323 +1330,6 @@` (`cleanupExtraFields`): `f51f3018` (13-04, JSONP-08), `d3f6d6fb` (13-01), `fd31cadc` (13-02, JSONP-07). Tests: `MetadataFreeJsonInsertUnitTest` JSONP-08 cases, `MetadataFreeJsonTypeParityLiveTest`, `ExplicitJsonInsertPathLiveTest#metadataFreeRouteLandsCdcRecordsAndSkipsEmptyOnes`.
- Both verdict cells end with "closed in 15-03 on Phase 13 evidence".

## Task Commits

1. **Task 1: VERSION and anchor row** - `c750ed1a` (build)
2. **Task 2a: ledger L-20..L-22 and closed dispositions** - `fd3bece3` (docs)
3. **Task 2b: two pending checklist rows closed** - `42026e80` (docs)

**Plan metadata:** recorded in the docs commit that follows this file.

## Files Created/Modified

- `VERSION`, `.planning/scripts/fork-anchors.txt` - release identity and its anchor
- `.planning/phases/13-fork-hardening-on-the-merged-tree/13-BEHAVIOUR-LEDGER.md` - L-20..L-22 and annotations
- `.planning/phases/13-fork-hardening-on-the-merged-tree/13-REVIEW-DISPOSITION.md`, `.planning/phases/14-dependency-bumps-ci/14-REVIEW-DISPOSITION.md` - closed dispositions
- `.planning/MERGE-CHECKLIST-v1.7.0.md` - two rows to `done` with evidence

## Decisions Made

- Only WR-02 is `fixed`; D-03 forbids marking anything fixed without a commit in this milestone. The Phase 13 warnings that touch upstream lines (WR-01, WR-03, WR-04) are deferred as prefer-upstream and named as FORK-DELTA Known costs for plan 15-06.
- The Section 2a VERSION row of the merge checklist was not edited; it records the Phase 12 merge decision and stays historical.

## Deviations from Plan

None - plan executed exactly as written. (The `<automated>` checks were run individually, because the plan puts several in one `<verify>` block.)

## Issues Encountered

None. The `clean` deleted the untracked Phase 14 scratch evidence under `build/`, as the plan anticipated; its content is already in committed SUMMARYs.

## Known Stubs

None.

## Threat Flags

None. T-15-08: the generated `Version.java` shows the new version after `clean`. T-15-09: both places agree and open is 0. T-15-10: the anchor row moved in the same pure commit and the invariant script prints `result=PASS` (`anchor_commits=110 anchor_rows=43 marker_commits=157`).

## User Setup Required

None - no external service configuration required.

## Next Phase Readiness

- The tree names itself `v1.7.0_gg1`. 15-04 onward can cite L-20..L-22 and the closed dispositions; the 15-07 gate repeats the `clean` plus `ARTIFACT_VERSION` assertion.
- 15-05 should cover the L-22 route (no value pin exists) in the D-07 check; 15-06 owes FORK-DELTA Known costs entries for WR-01, WR-03 and the L-21 WARN.

## Self-Check: PASSED

- All six `key-files.modified` paths exist on disk.
- Commits `c750ed1a`, `fd3bece3` and `42026e80` are ancestors of HEAD; `git rev-list --count 141db389..HEAD` was 3 before this file.
- `VERSION-COMMIT-OK`, `LEDGER-OK`, `DISPOSITIONS-OK`, `CHECKLIST-ZERO-PENDING` printed; invariant `result=PASS`.

---
*Phase: 15-suite-gate-publication-rollout*
*Completed: 2026-10-10*
