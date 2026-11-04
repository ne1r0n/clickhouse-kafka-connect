---
phase: 45-upstream-tail-merge-dependency-check
plan: "06"
subsystem: dependency-cve-check-and-phase-gate
tags: [dep-01, dep-02, ups-01, ups-05, osv, ghsa, confluent-archive, phase-gate, evidence]

requires:
  - phase: 45-upstream-tail-merge-dependency-check
    provides: plans 45-01..45-05 (merge M, the value-free rebuild and its pins, the JSON-route guard, the fromJsonRow pin, FORK-DELTA), anchors at 200 rows, testUnit 2072
provides:
  - 45-DEPENDENCY-CHECK.md, the DEP-01 record of 27 shipped coordinates, 228 test-scope, 7 integration-only, 47 plugin and JaCoCo artifacts, the wrapper, the JDK, 7 workflow actions and the test images, with 16 open advisories written up as declined proposals P1-P16
  - E-16, E-17 and E-18; the evidence index E-01..E-18 complete and every row pass
  - the Phase 45 gate on the tip: testUnit with Docker unreachable green, two full suites green and equal, tail gate PASS
affects: [46, 47, 48, 49]

actuals:
  tokens: 9000
  tasks: 3
  commits: 7
plan_head_before: add636569e3dcca877a109547aca4f7b1b206814
plan_head_after: fd156fd603819704a15e7e859f277d8e773ab07a

tech-stack:
  added: []
  patterns: [two-source advisory query proven live by a control, archive cross-check inside the shadow jar, record-only dependency check under U-4]

key-files:
  created:
    - .planning/phases/45-upstream-tail-merge-dependency-check/45-DEPENDENCY-CHECK.md
  modified:
    - .planning/phases/45-upstream-tail-merge-dependency-check/45-COMMAND-EVIDENCE.md
    - .planning/phases/45-upstream-tail-merge-dependency-check/45-CONTEXT.md

key-decisions:
  - "U-4 held: no dependency, plugin, wrapper or action version moved; build.gradle.kts, gradle/ and .github/ are byte-unchanged since M"
  - "DEP-02 decisions (relayed by the coordinator as the user's AskUserQuestion answers, recorded in spirit, not verbatim): P1-P16 record only, all declined, kept as backlog, none ships; the 12 Confluent 7.9.1-ccs Kafka artifacts: note the unmapped Apache version as an unknown, do not chase it"

requirements-completed: [DEP-01, DEP-02, UPS-05, UPS-01]

coverage:
  - id: D1
    description: "Every coordinate clickhouseDependencies resolves (27) is recorded with resolved and latest stable version and the OSV.dev and GitHub advisories on 2026-11-04, after both sources returned 74 advisories for the jackson-databind 2.9.0 control; the Confluent archive's single shadow jar is cross-checked (18 pom.properties versions equal the resolution, 8 more matched by package root, jackson-bom POM-only); no shipped coordinate has an open advisory"
    requirement: DEP-01
    verification:
      - kind: other
        ref: "P45-06-T1-OK; E-16"
        status: pass
    human_judgment: false
  - id: D2
    description: "The test, test-fixture and integration-test classpaths, the plugins, JaCoCo, the wrapper, the JDK, the 7 workflow actions (pin kind, exact version, workflow state, guard) and the test images are recorded; 16 test-scope coordinates carry open advisories, none ships, all are declined as record only and nothing moved"
    requirement: DEP-02
    verification:
      - kind: other
        ref: "P45-06-T2-OK; E-17"
        status: pass
    human_judgment: false
  - id: D3
    description: "Phase gate: cache-busted testUnit with DOCKER_HOST=unix:///nonexistent.sock (2072 tests, 2 skipped), two full suites on latest x V2 (2675 tests each, 18 named skips, 216-case matrix, WRT-10), pins and gate scripts unchanged, M the only merge since the tag, tail gate PASS with provenance_paths=11 on explicit SHAs, nothing pushed"
    requirement: UPS-05
    verification:
      - kind: other
        ref: "P45-06-T3-OK (re-ran the unedited tail gate live); E-18"
        status: pass
    human_judgment: false
  - id: D4
    description: "Whether the 12 Confluent 7.9.1-ccs Kafka artifacts are free of the Apache 3.9.x advisories is unknown and was deliberately not pursued"
    requirement: DEP-01
    verification: []
    human_judgment: true
    rationale: "Neither advisory database nor the jar maps 7.9.1-ccs to an Apache release; the user chose to note the unknown and not chase it"

duration: 33min
completed: 2026-11-04
status: complete
---

# Phase 45 Plan 06: Dependency and CVE check and the phase gate Summary

**The shipped dependency set (27 coordinates, cross-checked inside the Confluent archive's shadow jar) has no open advisory in OSV.dev or the GitHub advisory database; 16 test-scope coordinates do, none ships, and under the user's record-only decision nothing was bumped; the Phase 45 gate is green (testUnit 2072 with Docker unreachable, two equal 2675-test full suites, tail gate PASS).**

## Performance

- **Duration:** about 33 min of active work (the two full suites take 2 min 43 s each; the advisory queries over about 300 coordinates took roughly 25 min in the background)
- **Tasks:** 3
- **Files:** 1 created (the dependency record), the evidence and CONTEXT modified

## Accomplishments

- **Task 1 (tracer, E-16, commits `2f126eac`, `aadd7dd4`).** Both advisory sources were proven live first: the `jackson-databind@2.9.0` control returned 74 from OSV.dev and 74 from the GitHub database (Dependabot alerts are disabled, HTTP 403). 27 coordinates resolved by `clickhouseDependencies`, 0 advisories, 10 stale (lz4-java, zstd-jni, error_prone_annotations, guava, commons-codec, commons-io, commons-lang3 3.20.0 against 3.21.0, asm, RoaringBitmap, slf4j-api). The archive holds one shadow jar, so the cross-check reads inside it: 18 `pom.properties` versions equal the resolution, 8 coordinates match by package root or native file, no unexplained jar or package.
- **Task 2 (E-17, commits `ef369177`, `13a6214e`, `bd34d71b`, `33b80d13`).** Sections 2 to 7 of the record: 228 test and test-fixture rows, 7 integration-only rows, 47 plugin and JaCoCo artifacts, Gradle 9.8.2 (current), the JDK rows, 7 workflow actions (four SHA-pinned and current; three tag-pinned in upstream's disabled `integrationTests.yaml`, resolving to v3.7.0, v3.14.2 and v2.12.0; no active workflow has a tag-pinned action), the container images. 16 test-scope coordinates carry open advisories (OSV and GitHub agree on every row): logback-core, wire-runtime and wire-runtime-jvm, commons-beanutils, four netty modules, mina-core, zookeeper, jose4j, three jetty modules, snappy-java. All trace to Confluent platform 7.9.1 libraries or `kafka-clients`; none is a shipped coordinate. Written up as P1-P16 and then declined: record only, kept as backlog (see Decisions).
- **Task 3 (E-18, commits `33b80d13` as the gated head, `fd156fd6`).** Cache-busted testUnit with `DOCKER_HOST=unix:///nonexistent.sock`: 2072 tests, 0 failures, 2 skipped. Two `--rerun` full suites on ClickHouse `26.10.3.7` (digest `sha256:739e099a...30b1ce`) x client V2: 2675 tests each, 0 failures, 0 errors, 18 named skips (`named=7 state_mismatch_cases=11 unexpected=0`), equal counts; +250 over the E-01 baseline of 2425 = 15 upstream cases + 235 new fork cases (95 + 59 + 4 + 46 + 31); the 216-case matrix and WRT-10 pass in the suite. gg3 pins, the 14 NAME pin files and `SlashColumnRoundTripTest` unchanged; `WriterValueFreeFailureUnitTest` differs only in the three retired methods and `Utils` only in visibility; gate scripts and exemptions byte-unchanged since the tag; M is the only merge since the tag; the unedited tail gate over M..`33b80d13` reads `provenance_paths=11 anchor_commits=36 anchor_rows=200 marker_commits=54 anchor_exempt=0 result=PASS` (127 s). `pre-v1.7.0_gg8` is absent on origin, origin's sync branch is absent as at E-01, origin's `custom_build` is still `94834763`, and upstream's main and newest tag did not move.

## Task Commits

1. **Task 1: shipped-set check** - `2f126eac` (record), `aadd7dd4` (E-16)
2. **Task 2: remaining scopes and DEP-02** - `ef369177` (record with proposals), `13a6214e` (E-17 pending), `bd34d71b` (decisions in record and CONTEXT), `33b80d13` (E-17 re-measured pass)
3. **Task 3: phase gate** - `fd156fd6` (E-18)

`actuals.commits: 7` is the measured `git rev-list --count plan_head_before..plan_head_after`; the docs(45-06) completion commit follows this file (SUMMARY only; STATE.md, config.json, state.json, ROADMAP and requirements are not staged).

## Decisions

Recorded in 45-DEPENDENCY-CHECK.md section 7, 45-CONTEXT.md and E-17. They reached this plan through the coordinator, which relayed them as the user's AskUserQuestion answers; the exact words were not available, so they are recorded in spirit. Declining changes nothing, so no approval was needed from an agent and none was given.

1. **P1-P16: record only.** All sixteen declined; nothing bumped; the list stays as backlog for a later maintenance phase; none ships.
2. **The 12 Confluent `7.9.1-ccs` Kafka artifacts: note and do not chase.** They return 0 only because the databases index Apache version numbers; Apache `kafka-clients` 3.9.0/3.9.1 and `kafka_2.13` 3.9.0 carry advisories, and what `7.9.1-ccs` contains is unknown. The test classpath resolves the `-ccs` artifacts over Apache's 4.4.0 because Gradle orders 7.9.1 above 4.4.0.

**Stale but clean (for a later maintenance phase):** 223 rows across sections 1 to 5 are behind the newest stable version, among them 10 of the 27 shipped coordinates (listed in E-16 and section 1), 179 test-scope rows, 3 integration-only rows, 28 plugin-classpath rows and the three tag-pinned actions of the disabled upstream workflow. All are recorded as not proposed under U-4.

## Deviations from Plan

### Auto-fixed Issues

None needing a code or plan change. Reconciliations against the real tree, recorded in E-16 to E-18:

**1. [Assumption] Archive cross-check (plan assumption A2)**
- **Found during:** Task 1
- **Issue:** the plan expects per-dependency jar entries in the archive. `createConfluentArchive` packs one shadow jar with every dependency unpacked; `verifyConfluentArchive` itself asserts "no nested jar".
- **Fix:** the cross-check reads inside the shadow jar (pom.properties versions, package roots, native files). `archive_jars` is 1, `archive_jars_not_in_resolution` is `none`.
- **Commit:** `aadd7dd4`

**2. [Assumption] Zero open advisories expected**
- **Found during:** Task 2
- **Issue:** research's pre-measurement covered only the shipped set; the test classpaths carry 16 coordinates with open advisories.
- **Fix:** proposals P1-P16 written, the plan stopped at a decision checkpoint, and the decisions were recorded as above before Task 3.
- **Commit:** `ef369177`, `bd34d71b`

**3. [Environment] OpenJ9 crash in the full suite**
- **Found during:** Task 3
- **Issue:** the first full-suite attempt on the default Java 17 toolchain (OpenJ9) exited 255 (`Gradle Test Executor 262`, 8 GB core dump); after deleting the dump and siblings the single permitted re-run crashed the same way (`Gradle Test Executor 267`). Swap was 32.2 of 33.8 GB used.
- **Fix:** dumps deleted; both recorded runs used the documented `-PjavaVersion=21` escape (Homebrew OpenJDK 21.0.12.1); neither partial XML was used. The Docker-unreachable testUnit run used the default toolchain without crashing. Recorded in E-18 (`suite_java`, `suite_run1_crash`).
- **Commit:** `fd156fd6`

**4. [Stale counts]** the plan's planning-time baselines are stale: anchors 200 (not 190), testUnit 2072, full suite 2675 (E-01's 2425 plus 15 plus 235). All measured, none failed.

**5. [Accident]** one of my shell commands created three stray files in the repository root (a redirect typo); they were removed before any commit and never staged.

**Total deviations:** 0 code fixes, 3 plan-assumption or environment reconciliations, 1 stale-count note, 1 cleaned accident. **Impact:** none on locked decisions or gate counts.

## Issues Encountered

The OpenJ9 crash twice (deviation 3). Nothing else.

## Known Stubs

None.

## Threat Flags

None. T-45-17 (shipped coordinates checked against two live sources and the archive; the open advisories are named and routed to the user), T-45-18 (no version moved; `build.gradle.kts`, `gradle/` and `.github/` byte-unchanged since M), T-45-19 (secrets check on the record and the evidence, 0 token shapes; only `gh api` reads), T-45-20 (counts from JUnit XML via the read-only helper; two equal runs; the tail gate re-run live by the verify), T-45-21 (nothing pushed; `pre-v1.7.0_gg8` absent on origin) are mitigated as the register states.

## User Setup Required

None.

## Notes for the orchestrator

- **FORK-DELTA still awaits an independent `gsd-code-reviewer` pass** before Phase 49's publication gate (it is an operator document); the DEP-01 record is also an operator-facing document that Phase 49's release notes may cite.
- **Backlog for a maintenance phase:** P1-P16 in section 7 of the record (test-scope only), the unresolved `7.9.1-ccs` question, and the 223 stale rows. The shipped archive needs no change.
- The Phase 49 allow-list `confluent-archive-allowlist-v1.7.0_gg8.txt` is unaffected by anything here.
- **Gate host note:** full-suite runs on this host crash on OpenJ9 under swap pressure; use `-PjavaVersion=21`.

## Next Phase Readiness

- Phase 45 has all six plans summarized. The tip `fd156fd6` holds upstream's tail value-free and pinned, with the unedited tail gate PASS (`anchor_rows=200`), testUnit 2072, a full suite of 2675 tests with the 18 named skips, and the dependencies checked. Ready for the phase verifier and the code-gap phases.

## Self-Check: PASSED

- Files: `45-DEPENDENCY-CHECK.md`, `45-06-SUMMARY.md` and the E-16..E-18 entries are present.
- Commits `2f126eac`, `aadd7dd4`, `ef369177`, `13a6214e`, `bd34d71b`, `33b80d13`, `fd156fd6` are ancestors of HEAD.
- Verify blocks `P45-06-T1-OK`, `P45-06-T2-OK` and `P45-06-T3-OK` printed on the committed tip; the evidence index reads E-01..E-18, all pass; `git worktree list` has one line; `git diff --quiet 12e2cf17 HEAD -- build.gradle.kts gradle .github` exits 0; nothing pushed.
