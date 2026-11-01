---
phase: 33-dependency-sweep-protobuf-decision
plan: "02"
subsystem: build-dependencies
tags: [okhttp, gradle-wrapper, setup-java, dependabot, pin-guards, fork-delta, evidence]

requires:
  - phase: 33-dependency-sweep-protobuf-decision
    provides: plan 33-01 lz4-java tip (PR #19 merge 6bf1c43b), E-01..E-06 and the rollback tag pre-v1.7.0_gg6
provides:
  - okhttp 5.5.1 absorbed from PR #18 as its own --no-ff merge commit e075b2cc
  - Gradle wrapper 9.8.2 absorbed from PR #20 as its own merge commit e4f49dca, gradlew.bat CRLF intact
  - actions/setup-java v6.1.0 SHA pin absorbed from PR #16 as its own merge commit 79f56a5e (the sweep tip)
  - full suite green on the sweep tip on latest x V2 (1961 tests, 18 skipped)
  - docs/FORK-DELTA.md refreshed for the four absorbed bumps (cefdcb39)
  - evidence entries E-07..E-11
affects: [33-03, 34-tail-merge, 38-publish]

actuals:
  tokens: 11400
  tasks: 3
  commits: 10

tech-stack:
  added: []
  patterns:
    - "One Dependabot head per --no-ff merge, fetched by refs/pull/N/head immediately before the merge, with author, single parent, file list and version change checked"
    - "Wrapper bump gated Docker-free: --version, SHA-256 of the same distribution URL against the published .sha256, gradlew.bat eol/CRLF, createConfluentArchive on the new Gradle"
    - "Full suite run on the merge commit itself, before any evidence commit, so the gated head is the sweep tip"

key-files:
  created: []
  modified:
    - gradle/libs.versions.toml
    - gradle/wrapper/gradle-wrapper.properties
    - .github/workflows/tests.yaml
    - .github/workflows/release.yaml
    - .github/workflows/pre-release.yaml
    - .github/workflows/nightly-integration-tests.yaml
    - docs/FORK-DELTA.md
    - .planning/phases/33-dependency-sweep-protobuf-decision/33-COMMAND-EVIDENCE.md

key-decisions:
  - "PRs #18, #20 and #16 absorbed unchanged from their fetched heads; none was rebased since research (rebased_since_research: no for all three)"
  - "FORK-DELTA GHSA clause written because E-03 pr19_body_ghsa is GHSA-48mj-8f7c-7qfm; upstream main 9bed6f79 still reads lz4Java 1.11.4 and okhttp 5.5.0, so the past-upstream wording stands"

requirements-completed: [SWP-02, SWP-01, SWP-04]

coverage:
  - id: D1
    description: "PR #18 (okhttp 5.5.1) fetched by ref, merged as its own commit; first-parent diff is the one okhttp catalog line; compileIntegrationTestJava green and dependencyInsight names okhttp 5.5.1"
    requirement: SWP-02
    verification:
      - kind: other
        ref: "33-COMMAND-EVIDENCE.md E-07 (P33-02-T1-OK check script); compileIntegrationTestJava exit 0"
        status: pass
    human_judgment: false
  - id: D2
    description: "PR #20 (Gradle wrapper 9.8.2) merged as its own commit changing only gradle-wrapper.properties; --version reads 9.8.2, distribution SHA-256 matches the published checksum, gradlew.bat keeps CRLF, createConfluentArchive passes verifyConfluentArchive"
    requirement: SWP-02
    verification:
      - kind: other
        ref: "33-COMMAND-EVIDENCE.md E-08 (P33-02-T1-OK and P33-02-T1-LIVE-OK check scripts); createConfluentArchive on 9.8.2"
        status: pass
    human_judgment: false
  - id: D3
    description: "PR #16 (setup-java v6.1.0) merged as its own commit touching exactly the four fork-owned workflows; one b843ebf9 v6.1.0 pin each, tag commit verified, integrationTests.yaml blob unchanged, four pin-guard classes green under testUnit --rerun"
    requirement: SWP-02
    verification:
      - kind: unit
        ref: "testUnit --rerun --tests '*WorkflowActionPinGuardUnitTest' '*TestsWorkflowPinGuardUnitTest' '*NightlyWorkflowGuardUnitTest' '*UpstreamIntegrationWorkflowBytesGuardUnitTest' (21, 39, 58 and 9 tests, 0 failures); E-09"
        status: pass
    human_judgment: false
  - id: D4
    description: "Full suite on the sweep tip (the PR #16 merge) green on latest x V2: 1961 tests, 0 failures, 0 errors, 18 skipped, src/main identical to pre-v1.7.0_gg6, anchors mirror bad=0"
    requirement: SWP-02
    verification:
      - kind: integration
        ref: "CLICKHOUSE_VERSION=latest CLIENT_VERSION=V2 ./gradlew test --rerun; E-10"
        status: pass
    human_judgment: false
  - id: D5
    description: "docs/FORK-DELTA.md states lz4Java 1.12.1, okhttp 5.5.1 past upstream 5.5.0, setup-java v6.1.0 and the Phase 33 open-PR list with PR #16 as the first observed github-actions pull request; stale 1.12.0, v6.0.1 and #4/#10 wording gone"
    requirement: SWP-01
    verification:
      - kind: other
        ref: "33-COMMAND-EVIDENCE.md E-11 (P33-02-T3-OK check script, grep -cF count gates)"
        status: pass
    human_judgment: false

duration: 15min
completed: 2026-10-28
status: complete
plan_head_before: f4322f4789be0401d39e08c2da0e0a9edbeea5d0
plan_head_after: 1bc7073849828ed5ff3c7756584de15a8c3d3339
---

# Phase 33 Plan 02: okhttp, Gradle wrapper and setup-java sweep Summary

**okhttp 5.5.1, Gradle 9.8.2 and actions/setup-java v6.1.0 each landed as their own merge of a fetched Dependabot head with its own gate (integration compile, checksum-verified wrapper with gradlew.bat CRLF intact, four green pin guards), the full suite is green on the sweep tip, and FORK-DELTA now describes the tree as it is.**

## Performance

- **Duration:** 15 min
- **Started:** 2026-10-28T19:08:43Z
- **Completed:** 2026-10-28T19:23:00Z
- **Tasks:** 3
- **Files modified:** 8 (catalog, wrapper properties, four workflows, FORK-DELTA, evidence file)

## Accomplishments

- PR #18 head `cb898dc2` (dependabot[bot], one parent equal to the origin tip, only `gradle/libs.versions.toml`) merged as `e075b2cc`; first-parent diff is the single `okhttp` version line. `compileIntegrationTestJava` exited 0 and `dependencyInsight` on `integrationTestCompileClasspath` names `com.squareup.okhttp3:okhttp:5.5.1` (E-07).
- PR #20 head `8786723b` merged as `e4f49dca`; first-parent diff is only `gradle/wrapper/gradle-wrapper.properties`. `./gradlew --version` reads 9.8.2, the distribution SHA-256 `21bd5da2...` equals the published checksum, `gradlew.bat` stayed `i/lf w/crlf attr/text eol=crlf` with CRLF terminators and clean, and `createConfluentArchive` passed `verifyConfluentArchive` on 9.8.2 (E-08).
- PR #16 head `cb3c7718` merged as `79f56a5e`; first-parent diff is exactly the four fork-owned workflows, each with exactly one `actions/setup-java@b843ebf9... # v6.1.0` line. The actions/setup-java `v6.1.0` tag (lightweight) resolves to that commit, and `integrationTests.yaml` keeps blob `3e9b9be0`. The four pin-guard classes passed under `testUnit --rerun` (21, 39, 58 and 9 tests, 0 failures) (E-09).
- Full suite on the sweep tip `79f56a5e` (latest x V2, ClickHouse 26.10.3.7, same image digest as E-02 and E-05): 1961 tests, 0 failures, 0 errors, 18 skipped, no JVM crash; `src/main` identical to `pre-v1.7.0_gg6`; anchors mirror `rows=131 bad=0` after every merge (E-10).
- `docs/FORK-DELTA.md` rows 70, 76, 83 and 117 refreshed from measured values: lz4Java 1.12.1 (with the GHSA-48mj-8f7c-7qfm clause), okhttp 5.5.1 past upstream 5.5.0, setup-java v6.1.0, the Phase 33 open-PR list, PR #16 as the first observed github-actions pull request (E-11). Upstream `main` `9bed6f79` still reads `lz4Java = "1.11.4"` and `okhttp = "5.5.0"`.

## Task Commits

1. **Task 1 (tracer): PR #18 and PR #20** - `e075b2cc` (PR #18 merge), `e4f49dca` (PR #20 merge), `9bf75154` (evidence E-07, E-08)
2. **Task 2: PR #16 and the sweep-tip suite** - `79f56a5e` (PR #16 merge), `cee7de4b` (evidence E-09, E-10)
3. **Task 3: FORK-DELTA refresh** - `cefdcb39` (docs), `1bc70738` (evidence E-11)

**Plan metadata:** committed separately after this SUMMARY (docs: complete plan).

`commits: 10` is measured with `git rev-list --count f4322f47..HEAD`. Seven are the plan's own commits above; the other three are the Dependabot heads (`cb898dc2`, `8786723b`, `cb3c7718`) that the merges brought in. `git rev-list --first-parent --merges --reverse pre-v1.7.0_gg6..HEAD` prints exactly the four merges #19, #18, #20, #16 in order.

## Files Created/Modified

- `gradle/libs.versions.toml` - `okhttp` 5.5.0 to 5.5.1, via the PR #18 merge
- `gradle/wrapper/gradle-wrapper.properties` - `distributionUrl` 9.8.1 to 9.8.2, via the PR #20 merge
- `.github/workflows/{tests,release,pre-release,nightly-integration-tests}.yaml` - setup-java pin v6.0.1 to v6.1.0, via the PR #16 merge
- `docs/FORK-DELTA.md` - four rows refreshed
- `.planning/phases/33-dependency-sweep-protobuf-decision/33-COMMAND-EVIDENCE.md` - entries E-07..E-11

## Decisions Made

- Each head was merged exactly as fetched; none had been rebased since research.
- The `TestsWorkflowPinGuardUnitTest` `FIVE_PINNED` fixture (setup-java v6.0.1 over a fake SHA) was left untouched, as the plan prohibits editing it.
- `VERSION`, `CHANGELOG.md` and `src/main` were not touched; the `wrapper` task, `spotlessApply` and `spotlessCheck` were not run.

## Deviations from Plan

None - plan executed exactly as written. One prose slip in my own FORK-DELTA edit (a dropped comma in row 70's list) was caught by re-reading the row before the commit and fixed in the same working-tree state.

## Issues Encountered

None. No JVM crash, the origin tip never moved (re-read before each merge: always `3a3a15a9`), and `curl` of the Gradle distribution and checksum succeeded on the first try.

## Known Stubs

None.

## Threat Flags

None. The only new surface is three Dependabot head merges that the plan's threat register (T-33-05, T-33-06, T-33-07, T-33-08) already covers, each checked by the PR-head rule, the tag lookup, the SHA-256 comparison and the pin guards.

## User Setup Required

None - no external service configuration required.

## Next Phase Readiness

- Ready for plan 33-03 (protobuf decision and the phase gate) from a gated sweep tip that holds all four bumps, each in its own commit. Evidence keys for reuse: `merge_commit` in E-07, E-08 and E-09, `pr18_head`, `pr20_head`, `pr16_head`, `pr16_branch`, `forkdelta_commit` (E-11).
- Everything is local: nothing was pushed, and no pull request on origin was acted on. PRs #17 and #10 remain unabsorbed for 33-03.

## Self-Check: PASSED

- FOUND: `.planning/phases/33-dependency-sweep-protobuf-decision/33-COMMAND-EVIDENCE.md`, `docs/FORK-DELTA.md`
- FOUND commits (ancestors of HEAD): `e075b2cc`, `e4f49dca`, `79f56a5e`, `9bf75154`, `cee7de4b`, `cefdcb39`, `1bc70738`
- Task verifies printed `P33-02-T1-OK`, `P33-02-T1-LIVE-OK`, `P33-02-T2-OK` and `P33-02-T3-OK`.
- Plan verification: the first-parent merges since `pre-v1.7.0_gg6` are exactly #19, #18, #20, #16; `git diff --quiet pre-v1.7.0_gg6 HEAD -- src/main` exits 0.

---
*Phase: 33-dependency-sweep-protobuf-decision*
*Completed: 2026-10-28*
