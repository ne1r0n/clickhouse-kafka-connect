---
phase: 14-dependency-bumps-ci
plan: "03"
subsystem: build-dependencies
tags: [jackson, org-json, gradle-catalog, osv, full-live-suite, client-v2]

requires:
  - phase: 14-02
    provides: "lz4Java 1.12.0, okhttp 5.5.0 and mockito 5.24.0 in the catalog, Recorded Facts 33 and 34"
provides:
  - "jackson 2.22.3 in a pure one-line catalog commit, closing CVE-2026-91776 and CVE-2026-91777"
  - "orgJson 20260814 in a pure one-line catalog commit, the final code commit of Phase 14"
  - "Recorded Facts 35 (Jackson full run) and 36 (org.json run, D-06 comparison, D-09 gate)"
affects: [14-04, 15]

actuals:
  tokens: 6000
  tasks: 2
  commits: 4

plan_head_before: c3e91054169e4ae2b198296516067932b6014d69
plan_head_after: 28ec85ccee5726d8561ce71a9c732367d029c66f

tech-stack:
  added: []
  patterns:
    - "Back-to-back full-suite runs with nothing but one catalog line between them, so the first run is the exact before-run for the second"
    - "Per-testcase XML outcome comparison between two runs instead of trusting totals"

key-files:
  created: []
  modified:
    - gradle/libs.versions.toml
    - .planning/MERGE-CHECKLIST-v1.7.0.md

key-decisions:
  - "jacksonAnnotations stays 2.22: jackson-bom 2.22.3 pairs databind 2.22.3 with annotations 2.22 and no 2.22.3 annotations artifact exists"
  - "No bisect was needed: the D-06 differing list was empty"

requirements-completed: [BLD-11, BLD-12]

coverage:
  - id: D1
    description: "Jackson 2.22.3 in its own one-line catalog commit, gated by a full live suite on ClickHouse latest x client V2 with every JSON-route class green, Phase 13's R-01 parity matrix included"
    requirement: BLD-11
    verification:
      - kind: integration
        ref: "./gradlew test --rerun: classes=69 tests=805 failures=0 errors=0 skipped=18 (the 18 named skips, pair by pair)"
        status: pass
      - kind: integration
        ref: "MetadataFreeJsonTypeParityLiveTest 8/0/0/0, ExplicitJsonInsertPathLiveTest 7/0/0/0, JsonInsertColumnsLiveTest 5/0/0/0, MetadataFreeJsonWriterTest 33, MetadataFreeJsonInsertUnitTest 23, JsonSerializationEquivalenceTest 16, StructToJsonMapTest 2, JsonColumnTest 7 (1 named skip)"
        status: pass
      - kind: other
        ref: "dependencyInsight runtimeClasspath: jackson-databind:2.22.3 and jackson-annotations:2.22; OSV for databind 2.22.3 returned {}"
        status: pass
    human_judgment: false
  - id: D2
    description: "org.json 20260814 in its own one-line catalog commit, gated by compileIntegrationTestJava, testUnit and a second full live run, with unchanged Int256/UInt256/Decimal256 test outcomes (D-06)"
    requirement: BLD-12
    verification:
      - kind: integration
        ref: "comparison command: compared 152 152 differing [] across WideIntegerTypeGateLiveTest, RowBinaryUnsupportedTypeGateTest, ColumnTest, ClickHouseWriterColumnContextTest, ClickHouseSinkTaskWithSchemaTest"
        status: pass
      - kind: unit
        ref: "./gradlew compileIntegrationTestJava testUnit --rerun (431 tests, 0 failed, 2 skipped)"
        status: pass
    human_judgment: false
  - id: D3
    description: "D-09 phase gate on the final code commit: full suite with exactly the 18 named skips, Lz4RoundTripLiveTest 2 tests with no skip, testUnit green, merge-invariant script PASS"
    requirement: BLD-12
    verification:
      - kind: other
        ref: "verify-merge-invariants.sh printing: invariants: provenance_paths=21 anchor_commits=88 anchor_rows=40 marker_commits=135 result=PASS"
        status: pass
    human_judgment: false

duration: 25min
completed: 2026-10-09
status: complete
---

# Phase 14 Plan 03: Jackson 2.22.3 and org.json 20260814 Summary

**Jackson moved to 2.22.3 (two open databind CVEs closed) and org.json to 20260814, each as a pure one-line catalog commit behind its own full live run on ClickHouse 26.9.14.10 x V2, with both runs at 805 tests, 0 failures, 0 errors and exactly the 18 named skips.**

## Performance

- **Duration:** about 25 min, of which the two full runs took 2m 40s each
- **Completed:** 2026-10-09
- **Tasks:** 2
- **Files modified:** 2

## Accomplishments

- BLD-11: `jackson = "2.22.2"` to `"2.22.3"` in `32e91452`. `jacksonAnnotations` stays `"2.22"`.
- Jackson run (V2, `latest`, single node, `--rerun`): BUILD SUCCESSFUL, classes=69, tests=805, failures=0, errors=0, skipped=18. The skipped set equals FORK-DELTA's Named skip inventory pair by pair.
- JSON-route classes (tests/skipped/failures/errors): `MetadataFreeJsonTypeParityLiveTest` 8/0/0/0, `ExplicitJsonInsertPathLiveTest` 7/0/0/0, `JsonInsertColumnsLiveTest` 5/0/0/0, `MetadataFreeJsonWriterTest` 33/0/0/0, `MetadataFreeJsonInsertUnitTest` 23/0/0/0, `JsonSerializationEquivalenceTest` 16/0/0/0, `StructToJsonMapTest` 2/0/0/0, `JsonColumnTest` 7/1/0/0 (the named `@Disabled` method).
- `dependencyInsight --configuration runtimeClasspath`: `jackson-databind:2.22.3`, `jackson-annotations:2.22`.
- OSV (2026-10-09): databind 2.22.2 has GHSA-wv8q-qhhj-9h54 (CVE-2026-91776) and GHSA-cxp5-3px4-pw24 (CVE-2026-91777); 2.22.3 returns `{}`.
- BLD-12 (org.json part): `orgJson = "20260522"` to `"20260814"` in `7bdf8328`. Gate: `compileIntegrationTestJava testUnit --rerun` green (testUnit 35 classes, 431 tests, 0 failures, 0 errors, 2 skipped), then a second full run with the same totals as the Jackson run (69/805/0/0/18) and an identical skipped set.
- D-06: 152 testcases compared in each run across the five wide-integer and Decimal256 classes, differing list `[]`. No bisect was required.
- D-09 gate on the final code commit: `Lz4RoundTripLiveTest` 2 tests, 0 skipped, 0 failures, 0 errors. `verify-merge-invariants.sh`: `invariants: provenance_paths=21 anchor_commits=88 anchor_rows=40 marker_commits=135 result=PASS`.
- ORDER-OK: nothing in `src`, `build.gradle.kts`, `gradle` or `.github` changed between the Jackson commit and the org.json commit's parent.
- Server version, both runs: `26.9.14.10` (image label `com.clickhouse.build.version`, read before and after each run). Phase 13 ran on `26.9.13.15`, so `latest` moved by one patch release between phases, with no effect on results.
- Total test count is 805 against Phase 13's 803 because `Lz4RoundTripLiveTest` (2 tests) came in with plan 14-01.

## Task Commits

1. **Task 1: Jackson 2.22.3** - `32e91452` (build), `1ad47624` (docs, Recorded Fact 35)
2. **Task 2: org.json 20260814** - `7bdf8328` (build), `28ec85cc` (docs, Recorded Fact 36)

**Plan metadata:** the docs(14-03) completion commit that follows this file.

## Files Created/Modified

- `gradle/libs.versions.toml` - `jackson` and `orgJson` raised, one line per commit
- `.planning/MERGE-CHECKLIST-v1.7.0.md` - Recorded Facts 35 and 36

## Decisions Made

- `jacksonAnnotations` is left at 2.22; this answers CONTEXT's discretion question as "no change".
- Local evidence stays untracked under `build/`: `phase14-jackson-test-results/`, `phase14-jackson-run.log`, `phase14-orgjson-run.log`, plus the skipped-set listings.

## Deviations from Plan

None - plan executed exactly as written.

## Issues Encountered

None. Both bumps were green on the first run, so the flake and drift rule and the D-05 stop were never triggered. The `--rerun` flag re-executes the test tasks; compile tasks were re-run because the dependency versions changed.

## Known Stubs

None.

## Threat Flags

None. T-14-08 is mitigated (databind 2.22.3 on the runtime classpath, OSV clean). T-14-09 is mitigated (empty D-06 difference list). T-14-10 is mitigated (ORDER-OK, server version recorded for both runs). T-14-SC is mitigated (existing coordinates only, OSV re-checked on the execution day).

## Next Phase Readiness

- Ready for 14-04. This plan's org.json commit is the final Phase 14 code commit and carries the D-09 local gate.
- okhttp 5.5.0 stays compile-checked only; no Phase 14 gate runs the integration source set.

---
*Phase: 14-dependency-bumps-ci*
*Completed: 2026-10-09*

## Self-Check: PASSED

- Files exist: catalog, MERGE-CHECKLIST-v1.7.0.md, this SUMMARY.
- Commits 32e91452, 1ad47624, 7bdf8328, 28ec85cc are ancestors of HEAD; `git rev-list --count` from the ledger base gives 4.
- Acceptance greps passed: `jackson = "2.22.3"`, `jacksonAnnotations = "2.22"`, `orgJson = "20260814"` each count 1; PURE-OK, PRECHECK-OK and ORDER-OK printed; `git diff --quiet "$S^" HEAD -- src` exits 0; catalog diff headers are exactly +jackson, +orgJson, -jackson, -orgJson; Recorded Facts 35 and 36 present with `CVE-2026-91776`, `1000` and `differing`.
