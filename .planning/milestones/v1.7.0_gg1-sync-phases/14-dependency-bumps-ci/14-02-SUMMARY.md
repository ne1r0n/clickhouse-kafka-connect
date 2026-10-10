---
phase: 14-dependency-bumps-ci
plan: "02"
subsystem: build-dependencies
tags: [lz4-java, okhttp, mockito, gradle-catalog, osv, client-v2]

requires:
  - phase: 14-01
    provides: "Lz4RoundTripLiveTest, green on lz4-java 1.10.4 and naming no version"
provides:
  - "lz4Java 1.12.0 in the catalog, advisory-free on 2026-10-09, one pure catalog commit"
  - "okhttp 5.5.0 and mockito 5.24.0, one pure catalog commit each"
  - "Recorded Facts 33 (lz4 OSV lookup and dependencyInsight) and 34 (okhttp and mockito gates)"
  - "REQUIREMENTS correction: the advisory-free floor is 1.11.4, not 1.11.2"
  - "ROADMAP backlog 999.2 for the org.lz4:lz4-java 1.8.0 test-classpath duplicate"
affects: [14-03, 14-04, 15]

actuals:
  tokens: 9000
  tasks: 2
  commits: 5

plan_head_before: 630d7729cba3c0cae8321deb64997d8791454ed0
plan_head_after: 5c898b848c70ec85aac70c7d8584cb048c23930e

tech-stack:
  added: []
  patterns:
    - "Bump discipline: one catalog line per build commit, evidence in separate docs commits"
    - "Advisory evidence is an OSV query made on the execution day, never a copied research table"

key-files:
  created:
    - .planning/phases/999.2-test-classpath-org-lz4-duplicate/.gitkeep
  modified:
    - gradle/libs.versions.toml
    - .planning/MERGE-CHECKLIST-v1.7.0.md
    - .planning/REQUIREMENTS.md
    - .planning/ROADMAP.md

key-decisions:
  - "lz4-java 1.12.0 passed every gate on the first try, so the 1.11.4 fallback rung was not used"
  - "The advisory-free floor is 1.11.4; 1.11.2 and 1.11.3 are never described as advisory-free"
  - "The test-classpath org.lz4:lz4-java 1.8.0 duplicate is a backlog item, not fixed, because it needs a build.gradle.kts change outside Phase 14"

requirements-completed: [BLD-10, BLD-12]

coverage:
  - id: D1
    description: "lz4-java raised from 1.10.4 to 1.12.0 in a pure one-line catalog commit, with client-v2 0.9.6 shown working by compile, testUnit and the live LZ4 round trip"
    requirement: BLD-10
    verification:
      - kind: integration
        ref: "./gradlew test --rerun --tests '*Lz4RoundTripLiveTest' --tests '*NetworkCompressionPinLiveTest' (2 + 9 tests, 0 failed, 0 skipped)"
        status: pass
      - kind: unit
        ref: "./gradlew compileJava compileTestJava compileIntegrationTestJava testUnit --rerun (431 tests, 0 failed, 2 skipped)"
        status: pass
    human_judgment: false
  - id: D2
    description: "dependencyInsight shows one non-redirected at.yawk.lz4:lz4-java 1.12.0 headline on runtimeClasspath and clickhouseDependencies; OSV returns no advisory for 1.12.0 on the execution day"
    requirement: BLD-10
    verification:
      - kind: other
        ref: "dependencyInsight loop printing INSIGHT-OK; curl api.osv.dev | jq -e '(.vulns // []) | length == 0' printing true"
        status: pass
    human_judgment: false
  - id: D3
    description: "okhttp 5.5.0 and mockito 5.24.0 each landed in their own one-line catalog commit, each gated by compileIntegrationTestJava and testUnit; mockito also run live through ClickHouseWriterTest"
    requirement: BLD-12
    verification:
      - kind: unit
        ref: "./gradlew compileIntegrationTestJava testUnit --rerun after each edit (431 tests, 0 failed)"
        status: pass
      - kind: integration
        ref: "./gradlew test --rerun --tests '*ClickHouseWriterTest' (16 tests, 0 failed)"
        status: pass
    human_judgment: false
  - id: D4
    description: "okhttp 5.5.0 runtime behaviour: only compile-checked, because its sole consumer is an integration test needing ClickHouse Cloud credentials and a Confluent stack"
    requirement: BLD-12
    verification: []
    human_judgment: true
    rationale: "No Phase 14 gate runs the integration source set; the plan records this as a backstop, not a proof"

duration: 30min
completed: 2026-10-09
status: complete
---

# Phase 14 Plan 02: lz4-java 1.12.0, okhttp 5.5.0 and mockito 5.24.0 Summary

**lz4-java moved from 1.10.4 to 1.12.0 (OSV-clean on the execution day, live LZ4 round trip green), plus okhttp 5.5.0 and mockito 5.24.0, as three pure one-line catalog commits with the 1.11.4 advisory floor on record.**

## Performance

- **Duration:** about 30 min
- **Completed:** 2026-10-09
- **Tasks:** 2
- **Files modified:** 5 (one new `.gitkeep`)

## Accomplishments

- BLD-10: `lz4Java = "1.12.0"` in commit `cf448fcb`. 1.12.0 passed every gate first time, so the D-03 fallback to 1.11.4 was never needed.
- OSV lookup (api.osv.dev, Maven ecosystem, 2026-10-09):
  - `at.yawk.lz4:lz4-java` 1.10.4: 6 advisories (GHSA-343h-94h5-c4wr, GHSA-4v53-57pg-c464, GHSA-6cx8-rjf8-pr8g, GHSA-gm45-99xc-r7wv, GHSA-mcr4-qmvw-px4g, GHSA-xx22-p4ch-683r).
  - 1.11.2 and 1.11.3: still 3 (GHSA-343h-94h5-c4wr, GHSA-gm45-99xc-r7wv, GHSA-mcr4-qmvw-px4g), all fixed in 1.11.4.
  - 1.11.4 and 1.12.0: none.
  - `org.lz4:lz4-java` 1.8.0: 8 advisories, no fix on that coordinate.
- `dependencyInsight` headlines: `runtimeClasspath` and `clickhouseDependencies` each print exactly one non-redirected `at.yawk.lz4:lz4-java:1.12.0`. Edges are `1.10.2 -> 1.12.0` from `client-v2:0.9.6`, plus `org.lz4:lz4-java:1.8.0 -> at.yawk.lz4:lz4-java:1.12.0` from `kafka-clients:4.1.1` on `runtimeClasspath`.
- `Lz4RoundTripLiveTest` (2 tests) and `NetworkCompressionPinLiveTest` (9 tests) pass unchanged on ClickHouse `26.9.14.10` x V2. The logged `LZ4Factory` code source is `.../at.yawk.lz4/lz4-java/1.12.0/.../lz4-java-1.12.0.jar`.
- REQUIREMENTS.md carries the dated Phase 14 correction that the floor is 1.11.4; the BLD-10 text is untouched.
- BLD-12 (okhttp and mockito parts): `60006d49` okhttp 5.5.0, `7c85dc73` mockito 5.24.0. The org.json part of BLD-12 belongs to a later plan.
- testUnit totals after each bump, summed from `build/test-results/testUnit/*.xml`: tests=431, skipped=2, failures=0, errors=0. `ClickHouseWriterTest` live on mockito 5.24.0: 16 tests, 0 failed.
- `dependencyInsight`: `okhttp:5.5.0` and `okhttp-jvm:5.5.0` on `integrationTestRuntimeClasspath`; `mockito-core:5.24.0` on `testRuntimeClasspath`.
- `verify-merge-invariants.sh`: `anchor_rows=40 result=PASS`.

## Task Commits

1. **Task 1: lz4-java 1.12.0 with evidence and floor correction** - `cf448fcb` (build), `2780624d` (docs)
2. **Task 2: okhttp 5.5.0 and mockito 5.24.0** - `60006d49` (build okhttp), `7c85dc73` (build mockito), `5c898b84` (docs, Recorded Fact 34)

**Plan metadata:** the docs(14-02) completion commit that follows this file.

## Files Created/Modified

- `gradle/libs.versions.toml` - `lz4Java`, `okhttp`, `mockito` raised, one line per commit
- `.planning/MERGE-CHECKLIST-v1.7.0.md` - Recorded Facts 33 and 34; `@@ -9,0 +10 @@` row set to done
- `.planning/REQUIREMENTS.md` - dated Phase 14 floor correction after the BLD list
- `.planning/ROADMAP.md` - backlog entry 999.2
- `.planning/phases/999.2-test-classpath-org-lz4-duplicate/.gitkeep` - backlog directory

## Decisions Made

- Used 1.12.0 as the first ladder rung; no rung failed, so the "1.11.4 after a 1.12.0 failure" body note was not needed.
- Left `build.gradle.kts` untouched. `testRuntimeClasspath` still resolves `org.lz4:lz4-java:1.8.0` beside the fork; it is test-only and not shipped, and the round-trip test pins the jar origin. Tracked as backlog 999.2.

## Deviations from Plan

None - plan executed exactly as written.

## Issues Encountered

None. The backlog number returned by `phase.next-decimal 999` was 999.2, as expected.

## Known Stubs

None.

## Threat Flags

None. No new endpoint, auth path or trust-boundary access beyond the plan's threat model. T-14-05 is mitigated (OSV returns none for 1.12.0; one headline on both required configurations). T-14-06 is accepted and backlogged. T-14-07 is accepted (no verification metadata exists).

## Next Phase Readiness

- Ready for 14-03 (Jackson 2.22.3 and org.json, each gated by a full live suite run).
- okhttp 5.5.0 and mockito's non-`ClickHouseWriterTest` live paths are only covered once 14-03's full runs happen.

---
*Phase: 14-dependency-bumps-ci*
*Completed: 2026-10-09*

## Self-Check: PASSED

- Files exist: catalog, MERGE-CHECKLIST, REQUIREMENTS, ROADMAP, 999.2 .gitkeep, this SUMMARY.
- Commits cf448fcb, 2780624d, 60006d49, 7c85dc73, 5c898b84 are ancestors of HEAD; git rev-list --count from the ledger base gives 5.
- Acceptance greps and the PURE-OK numstat checks passed for all three build commits; src/ and build.gradle.kts unchanged.
