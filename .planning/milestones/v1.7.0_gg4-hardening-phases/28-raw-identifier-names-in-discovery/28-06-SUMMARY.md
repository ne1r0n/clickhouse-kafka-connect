---
phase: 28-raw-identifier-names-in-discovery
plan: "06"
subsystem: ci-coverage
tags: [jacoco, fork-coverage, fork-anchors, phase-gate, assertion-diff-check, exemption-list]

requires:
  - phase: 28-raw-identifier-names-in-discovery
    provides: plans 28-01 to 28-05 (the six fork classes, the E-06 named exemption list, the 92-row anchors, E-10)
provides:
  - forkChangedClasses and jacoco_summary.py FORK_CLASSES_IN_ORDER list 21 fork classes (six added, each with its inner-class glob)
  - ForkCoverageScopeUnitTest, a Docker-free guard that keeps the two copies equal and complete
  - FORK-DELTA jacoco row (21-class jacocoForkReport) and two DEBT-17 anchor rows (94), in the same commit as the list edit
  - the generated fork report with real coverage for all 21 classes (E-12)
  - the Phase 28 gate E-13 and the assertion diff check against the 16-row signed-off exemption list
affects: [phase-28-verification, phase-29-rollout]

actuals:
  tokens: 9000
  tasks: 3
  commits: 3

tech-stack:
  added: []
  patterns:
    - "A Docker-free ci guard reads build.gradle.kts and the CI summary script as UTF-8 and compares the two copies of one list"
    - "The gate's assertion diff check walks every commit since the phase base against a named, user-signed exemption list"

key-files:
  created:
    - src/test/java/com/clickhouse/kafka/connect/ci/ForkCoverageScopeUnitTest.java
  modified:
    - build.gradle.kts
    - .github/scripts/jacoco_summary.py
    - docs/FORK-DELTA.md
    - .planning/scripts/fork-anchors.txt
    - .planning/phases/28-raw-identifier-names-in-discovery/28-BEHAVIOUR-LEDGER.md
    - .planning/phases/28-raw-identifier-names-in-discovery/28-COMMAND-EVIDENCE.md

key-decisions:
  - "The six classes are appended after the MBeanServerUtils pair in the order ClickHouseFieldDescriptor, DescribeTableBuilder, DeclaredNullability, ParentShape, MetadataFreeJsonWriter, WithheldMessageException; the summary script follows the same order"
  - "S-04 added to E-06's exemption list at the gate: the private constant RESOURCE_PATH_FORMAT changed with S-03's overload in test(28-02); the user chose option 1 (a 16th refactor row), relayed by the coordinator, and that choice is the sign-off of the 16-row list"
  - "The assertion diff check ran from a local copy (assertion-diff-16.py) with len(L)==16 and the S-01..S-04 sign-off text; 28-06-PLAN.md is unchanged"

patterns-established:
  - "A guard test keeps a build-script list and its CI-script copy equal (research Pitfall 6)"

requirements-completed: [DEBT-17]

coverage:
  - id: D1
    description: "The fork coverage report and the CI summary list 21 fork classes, including DeclaredNullability and WithheldMessageException, edited once with the FORK-DELTA row in the same commit"
    requirement: DEBT-17
    verification:
      - kind: unit
        ref: "ForkCoverageScopeUnitTest (3 cases); the required-set case was red on the unchanged lists (E-11), green after"
        status: pass
      - kind: other
        ref: "Task 2 verify DEBT17-ONE-COMMIT and DEBT17-GATED; fork-report-28-06.xml has all six new classes with a LINE counter, summary-28-06.md 21 rows with no n/a (E-12)"
        status: pass
    human_judgment: false
  - id: D2
    description: "The phase gate: cache-busted testUnit 1248, full suite 1835 tests in 101 XML files on latest x V2 with the 18 named skips, self-test, tail gate anchor_rows=94, Spotless with no new violation, origin frozen, src/main scope held"
    requirement: DEBT-17
    verification:
      - kind: other
        ref: "Task 3 verify PHASE28-SUITE-GREEN and PHASE28-GATE-PASS (E-13)"
        status: pass
    human_judgment: false
  - id: D3
    description: "No existing assertion was deleted or weakened across the phase: every changed existing test member is one of the 16 list rows in its named commit"
    requirement: DEBT-17
    verification:
      - kind: other
        ref: "assertion-diff-16.py against E-06 (list 16, signoff True, unused [], problems [], extra [], lowered [], tables True); E-13 tables"
        status: pass
    human_judgment: true
    rationale: "The exemption list is a user sign-off item; the sign-off here is the user's option-1 choice relayed by the coordinator, and the plan's end-of-phase human-check on E-13 still stands for the reviewer"

plan_head_before: c4503266959b83ecbd3a21fd3ed5817a98f48842
plan_head_after: 25fad533355f7b47219567432d41056913e63b2b

duration: 71min
completed: 2026-10-23
status: complete
---

# Phase 28 Plan 06: Fork coverage list and the Phase 28 gate Summary

**The JaCoCo fork report and its CI summary now list all 21 fork classes (ClickHouseFieldDescriptor, DescribeTableBuilder, DeclaredNullability, ParentShape, MetadataFreeJsonWriter and WithheldMessageException added), a Docker-free guard keeps the two copies equal, and the phase gate passes with the assertion diff check clean against a 16-row signed-off exemption list.**

## Performance

- **Duration:** about 71 min (plan start 2026-10-23T10:45:30Z; includes the wait for the S-04 decision)
- **Completed:** 2026-10-23T11:57Z
- **Tasks:** 3 (one tracer, two auto)
- **Files modified:** 8 (one new test class, two build files, FORK-DELTA, anchors, ledger, evidence file, this summary)

## Accomplishments

- **Tracer, red first (E-11).** `ForkCoverageScopeUnitTest` failed exactly its required-set case on the unchanged lists (the six classes missing) and passed 3/3 after both lists were edited. The regenerated fork report has a class element and a LINE counter for each of the 21 names; the summary prints 21 rows.
- **One DEBT-17 commit (E-12).** `898a20b2` holds build.gradle.kts (six entry pairs appended after MBeanServerUtils, the comment's neighbour list shortened), jacoco_summary.py (six names, comment says 21), the guard, the FORK-DELTA jacoco row (21-class, Where and Pinned-by cells extended) and two DEBT-17 anchor rows (94). The full suite ran on exactly that tree: 1835 tests in 101 XML files, 0 failures, the 18 named skips, server 26.9.19.9. Real Line coverage from the generated fork report: ClickHouseFieldDescriptor 88.0%, DescribeTableBuilder 97.5%, DeclaredNullability 89.6%, ParentShape 91.7%, MetadataFreeJsonWriter 88.4%, WithheldMessageException 100.0%.
- **Phase gate (E-13).** testUnit 1248 tests, 0 failed; the same full-suite figures; Spotless 46 lines against the 46-line base; self-test 16 PASS, 0 FAIL, `selftest: no failures`; tail gate `anchor_rows=94 result=PASS`; `origin/custom_build` still at `69cd680b` and no `sync/v1.7.0` on origin; `src/main` changed in exactly the four allowed files; Processing, the state classes, ClickHouseSinkConfig, ClickHouseWriter, the gg1 to gg3 rollout notes, VERSION and tests.yaml equal the phase base; no gg4 rollout note; both gate scripts equal `baseline-v1.7.0_gg4`. Nothing was pushed.
- **Assertion diff check.** Against the 16-row list: used rows 16, unused none, problems none, no file outside the list lost a line, no test or assert count lowered; X-01 to X-12 sit in fix(28-03) and S-01 to S-04 in test(28-02). E-13 holds both tables.

## Task Commits

1. **Task 1: tracer (guard red/green, lists edited, reports regenerated)** - no commit of its own (lands in Task 2's commit, as the plan directs)
2. **Task 2: list, guard, FORK-DELTA row and anchors in one commit** - `898a20b2` (build)
3. **Task 2: E-11, E-12, ledger L-66** - `400e4c77` (docs)
4. **Task 3: E-13, E-06 row S-04, ledger L-61 wording** - `25fad533` (docs)

**Plan metadata:** the commits that carry this SUMMARY and the STATE.md, ROADMAP.md and REQUIREMENTS.md updates.

## Files Created/Modified

- `src/test/java/com/clickhouse/kafka/connect/ci/ForkCoverageScopeUnitTest.java` - three cases comparing the gradle list with the summary script's list, requiring each inner-class glob and source file, and the six classes
- `build.gradle.kts` - `forkChangedClasses` (six pairs), its comment
- `.github/scripts/jacoco_summary.py` - `FORK_CLASSES_IN_ORDER` (six names), comment count
- `docs/FORK-DELTA.md` - the jacoco row; `.planning/scripts/fork-anchors.txt` - two DEBT-17 rows
- `28-BEHAVIOUR-LEDGER.md` - L-66, and L-61 now names S-01 to S-04; `28-COMMAND-EVIDENCE.md` - E-06 S-04 row and sign-off line, E-11, E-12, E-13

## Decisions Made

- See `key-decisions`. The tail of the list order follows the plan; the guard holds only the six required names, not a third copy of the list.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 3 - Blocking] The tracer's `-x test` report regeneration cannot run on Gradle 9.8.1**
- **Found during:** Task 1, step 5
- **Issue:** `./gradlew jacocoTestReport jacocoForkReport -x test` failed `jacocoForkReport`'s configuration validation (implicit dependency on the outputs of compileJava, generateProto and generateVersionFile, reachable only through `dependsOn(test)`, which `-x test` cuts). Fixing it needs a build.gradle.kts edit outside the list, which the plan prohibits.
- **Fix:** Regenerated both reports through `test --tests '*ForkCoverageScopeUnitTest' --rerun` (3 s, which finalizes both reports). It proves the same thing (the list reaches the report and the summary); the tracer's coverage figures are therefore all 0.0% and are marked as not real in E-11. The real figures are E-12's. The Gradle JaCoCo agent overwrites `build/jacoco/test.exec`, so E-12's figures are that suite's alone.
- **Files modified:** none (build.gradle.kts untouched outside the list)
- **Verification:** FORK-LIST-TRACER-GREEN printed
- **Committed in:** recorded in E-11 (`400e4c77`)

**2. [Gate finding, user decision] S-04 added to the exemption list**
- **Found during:** Task 3, step 7
- **Issue:** The assertion diff check reported one unlisted member: the private constant `RESOURCE_PATH_FORMAT` in `DescribeFixtureCatalog.java`, changed by test(28-02) together with S-03's `resourceLines` overload. It involves no assertion, but the list was the signed-off record and the plan forbids editing it to pass the gate, so the executor stopped and asked.
- **Fix:** The user chose option 1 (relayed by the coordinator): add S-04, refactor, test(28-02), `DescribeFixtureCatalog.java`, `RESOURCE_PATH_FORMAT`, "path format takes the directory as first argument; one-argument form unchanged", not a test. E-06 now has 16 rows and its `exemption_signoff:` line reads X-01..X-12 and S-01..S-04, with the provenance recorded; ledger L-61 names S-01 to S-04. The plan's check command hard-codes `len(L)==15` and the S-01..S-03 text, so the check ran from a local copy (`/tmp/gsd28/exec/assertion-diff-16.py`, differing in exactly those two lines); 28-06-PLAN.md was not edited. The first run's single problem is recorded in E-13.
- **Files modified:** `28-COMMAND-EVIDENCE.md`, `28-BEHAVIOUR-LEDGER.md`
- **Verification:** the 16-row check printed no problems and `tables True`; ASSERTION-DIFF-CLEAN equivalent and PHASE28-GATE-PASS printed
- **Committed in:** `25fad533`

---

**Total deviations:** 2 (1 Rule 3 blocking, 1 gate finding resolved by the user)
**Impact on plan:** No scope or behaviour change. The first only changed how the tracer regenerated the reports; the second widened the signed-off exemption list by one non-assertion support row.

## Issues Encountered

- The self-test (about 17 minutes) shared the machine with the testUnit and full-suite runs and finished well after them. No OpenJ9 crash occurred and no core, javacore, Snap or jitdump file appeared in the repository root.
- `HeaderToValueConfig` and `ClickHouseSinkConnector` show 0.0% Line coverage in the fork report. This predates the plan and the plan does not touch them.

## Known Stubs

None.

## Threat Flags

None. T-28-16 (the two lists drifting) is mitigated by the guard in `testUnit` and the two anchor rows; T-28-17 (a push or a moved origin) by the gate's freeze reads, with nothing pushed; T-28-18 (a weakened existing test) by the assertion diff check against the 16-row list.

## User Setup Required

None - no external service configuration required.

## Next Phase Readiness

- Phase 28 has all six plans complete and is ready for phase verification. The plan's end-of-phase human-check still applies to a reviewer reading E-06's list and E-13's tables, and to the two choices the plan names (the NAME-07 documents landing in fix(28-05), and Column calling `DeclaredNullability.unescapeJsonLayer`).
- VERSION is unchanged and `docs/ROLLOUT-v1.7.0_gg4.md` is not written; Phase 29 writes the rollout note from the ledger and the 72-row table.

---
*Phase: 28-raw-identifier-names-in-discovery*
*Completed: 2026-10-23*

## Self-Check: PASSED

Verified: `ForkCoverageScopeUnitTest.java`, the ledger and the evidence file exist; commits `898a20b2`, `400e4c77` and `25fad533` are ancestors of HEAD; FORK-LIST-TRACER-GREEN, DEBT17-ONE-COMMIT, DEBT17-GATED, PHASE28-SUITE-GREEN and PHASE28-GATE-PASS printed, and the 16-row assertion diff check is clean.
