---
phase: 43-document-fixes-phase-37-dispositions
plan: "01"
subsystem: testing
tags: [ci-guards, release-workflow, confluent-archive, allow-list, junit5, evidence]

requires:
  - phase: 42-isolated-high-risk-edits
    provides: the evidence helper, the testcase status helper and the red-then-green gate recipe
  - phase: 37-ci-hardening-advisory-debt
    provides: the open review rows (header comment, IN-04, IN-05, IN-06) this plan dispositions as fixed
provides:
  - "increment-version.yml header names IncrementVersionWorkflowGuardUnitTest, the class that reads the file (row 37-HDR)"
  - "ReleaseWorkflowGuardUnitTest CHAIN holds TAG_GATE_STEP between TESTS_GATE_STEP and PUBLISH_STEP, plus a fixture test that requires it (row 37-IN-04, user-approved)"
  - "ConfluentArchiveCheck prints the lib jars line only for more than one jar, plus a zero-jar test (row 37-IN-06)"
  - "line 1 of the gg4 and gg5 allow-lists names .github/scripts/compare_confluent_archives.py (row 37-IN-05(a))"
  - "43-COMMAND-EVIDENCE.md E-01..E-05, three byte-copied helpers, 43-expected-added.tsv"
affects: [43-02, 43-03, 44-release-notes]

actuals:
  tokens: 8261
  tasks: 3
  commits: 8

tech-stack:
  added: []
  patterns:
    - "red-then-green by assertion with a SHA-256 freeze of the test file between the red run and the code commit"
    - "per-testcase status dump and delta against a committed expected-added file as the no-existing-test-changed gate"

key-files:
  created:
    - .planning/phases/43-document-fixes-phase-37-dispositions/43-COMMAND-EVIDENCE.md
    - .planning/phases/43-document-fixes-phase-37-dispositions/43-evidence-check.py
    - .planning/phases/43-document-fixes-phase-37-dispositions/43-testcase-status.py
    - .planning/phases/43-document-fixes-phase-37-dispositions/43-doc-check.py
    - .planning/phases/43-document-fixes-phase-37-dispositions/43-expected-added.tsv
  modified:
    - .github/workflows/increment-version.yml
    - src/test/java/com/clickhouse/kafka/connect/ci/ReleaseWorkflowGuardUnitTest.java
    - src/test/java/com/clickhouse/kafka/connect/ci/ConfluentArchiveCheck.java
    - src/test/java/com/clickhouse/kafka/connect/ci/ConfluentArchiveCheckUnitTest.java
    - .planning/scripts/confluent-archive-allowlist-v1.7.0_gg4.txt
    - .planning/scripts/confluent-archive-allowlist-v1.7.0_gg5.txt

key-decisions:
  - "37-IN-04 landed on the user's recorded approval ('Approve 37 IN-04, defer 28 IN-05(b) (Recommended)'), read from 43-RESEARCH.md at f8ff8fc3; the only existing-test-source change is the one added CHAIN line"
  - "37-IN-05 is restricted to the gg4 and gg5 lists the review names; the 25 and gg6 lists are untouched because the gg6 list belongs to the published tag v1.7.0_gg6 (row 37-IN-05(b) is deferred by plan 43-03)"
  - "The full ClickHouse-backed suite is not run: every changed Java file is in the Docker-free ci package and no src/main, src/testFixtures, src/integrationTest or build file changed; Phase 44 runs two full suites on the release tip"
  - "STATE.md is advanced by the GSD verbs but not staged, because the plan's commit rules forbid staging it; the orchestrator commits it"

patterns-established:
  - "Each Phase 43 code commit holds exactly its declared files and is its own revert unit; each evidence entry is committed separately"

requirements-completed: []

plan_head_before: cb1ccb669be916b6da439d3f936c5cfbade42bd2
plan_head_after: 8e5d484562c83685ab6529f5f696efdc13feac30

coverage:
  - id: D1
    description: "The increment-version.yml header comment names IncrementVersionWorkflowGuardUnitTest and no longer the release guard class; the six workflow guard classes pass cache-busted at 188 testcases"
    requirement: DOC-02
    verification:
      - kind: unit
        ref: "IncrementVersionWorkflowGuardUnitTest, WorkflowActionPinGuardUnitTest, TestsWorkflowPinGuardUnitTest, ReleaseWorkflowGuardUnitTest, NightlyWorkflowGuardUnitTest, UpstreamIntegrationWorkflowBytesGuardUnitTest (188/0/0, 43-COMMAND-EVIDENCE.md E-02)"
        status: pass
    human_judgment: false
  - id: D2
    description: "CHAIN requires the tag gate after the tests gate: the new fixture test was red on the unchanged CHAIN and is green with the one added line"
    requirement: DOC-02
    verification:
      - kind: unit
        ref: "ReleaseWorkflowGuardUnitTest#CIH-04: fixture: the chain rule requires the tag gate after the tests gate (red then green, E-03)"
        status: pass
    human_judgment: false
  - id: D3
    description: "ConfluentArchiveCheck prints the lib jars line and the stale-directory hint only for more than one jar; an archive with zero lib jars gets neither"
    requirement: DOC-02
    verification:
      - kind: unit
        ref: "ConfluentArchiveCheckUnitTest#CI-20: an archive with no lib jar gets no stale directory hint on stderr (red then green, E-04); the two-jar case still passes"
        status: pass
    human_judgment: false
  - id: D4
    description: "Line 1 of the gg4 and gg5 allow-lists names the moved compare script; the 25 and gg6 lists and the gg6 tag copy are untouched"
    requirement: DOC-02
    verification:
      - kind: unit
        ref: "CompareConfluentArchivesScriptUnitTest#WR-02: every allow-list shipped under .planning/scripts loads (58/0/0, E-05)"
        status: pass
    human_judgment: false
  - id: D5
    description: "No existing assertion was weakened: the full cache-busted testUnit totals 1826 (baseline 1824 plus 2), 0 failures, 2 skipped, and its status delta is exactly the two expected PASS rows"
    requirement: DOC-02
    verification:
      - kind: other
        ref: "43-testcase-status.py delta status-base-testunit.tsv status-p01-testunit.tsv 43-expected-added.tsv: removed=0 added=2 match=yes (E-05)"
        status: pass
    human_judgment: false

duration: 9min
completed: 2026-11-02
status: complete
---

# Phase 43 Plan 01: Fixed Phase 37 rows Summary

**Four Phase 37 review rows fixed in four separate commits (increment-version.yml header, the approved release-guard CHAIN tag-gate line, the zero-jar stderr hint, the gg4 and gg5 allow-list headers), each seen red first where it adds a test, with a testUnit status delta of exactly two added PASS rows.**

## Performance

- **Duration:** 9 min
- **Started:** 2026-11-02T19:25:31Z
- **Completed:** 2026-11-02T19:34:00Z
- **Tasks:** 3
- **Files modified:** 11 (5 created, 6 modified)

## Accomplishments

- Measured the phase baseline on the unchanged tree instead of trusting the record: cache-busted `testUnit` with Docker unreachable ran 1824 tests, 0 failures, 0 errors, 2 skipped (equal to the recorded 1824); the three Java files this plan touches read `IS CLEAN`, the anchors mirror read `rows=190 bad=0`, origin `custom_build` was `72e8ccff`, and the three user answers were found in `43-RESEARCH.md` at `f8ff8fc3`.
- Row 37-HDR (tracer): two comment lines of `increment-version.yml` now name `IncrementVersionWorkflowGuardUnitTest`; the six workflow guard classes pass cache-busted at 188 testcases and the anchors mirror stays at 190 rows.
- Row 37-IN-04: the new fixture test failed alone on the unchanged `CHAIN` with a `MultipleFailuresError` carrying `expected the fixture to be rejected by compare chain order`; one added `TAG_GATE_STEP,` line turned it green, and both real workflows pass `workflowRunsTheCompareChainInOrder`.
- Row 37-IN-06: the new zero-jar test failed alone on the unchanged helper (the hint line was printed for zero jars); changing the one size condition to `> 1` turned it green with the committed test byte-identical to the red copy.
- Row 37-IN-05(a): line 1 of the gg4 and gg5 allow-lists now names `.github/scripts/compare_confluent_archives.py`; the 25 and gg6 lists stay as they were and the gg6 list equals its copy in tag `v1.7.0_gg6`.
- Gated the phase so far: full cache-busted `testUnit` 1826 tests (baseline plus 2), status delta removed=0 added=2, the only removed test-source line is the old size condition, `src/main` byte-identical to the phase start.

## Task Commits

1. **Task 1 (tracer: baseline E-01, header row E-02)** - `566f92e4` (fix, the header comment) and `ad3785d5` (docs, evidence E-01 and E-02 plus the three helper copies).
2. **Task 2 (row 37-IN-04, E-03)** - `33d079a9` (fix, test plus the CHAIN line) and `0df5f024` (docs, E-03).
3. **Task 3 (rows 37-IN-06 and 37-IN-05(a), E-04, E-05)** - `7796ad0a` (fix, helper condition plus zero-jar test), `527ea406` (docs, E-04), `103221a4` (fix, gg4 and gg5 allow-list headers) and `8e5d4845` (docs, E-05 plus `43-expected-added.tsv`).

**Plan metadata:** the SUMMARY commit that follows this file (docs: complete plan).

## Files Created/Modified

- `.github/workflows/increment-version.yml` - header lines 5 and 6 only.
- `src/test/java/com/clickhouse/kafka/connect/ci/ReleaseWorkflowGuardUnitTest.java` - one `CHAIN` line and the method `fixtureChainOrderRuleRequiresTheTagGateAfterTheTestsGate` (15 added lines, 0 deleted).
- `src/test/java/com/clickhouse/kafka/connect/ci/ConfluentArchiveCheck.java` - the size condition in `run` (1 line changed).
- `src/test/java/com/clickhouse/kafka/connect/ci/ConfluentArchiveCheckUnitTest.java` - the method `zeroLibJarsGetNoStaleDirectoryHint` (35 added lines, 0 deleted).
- `.planning/scripts/confluent-archive-allowlist-v1.7.0_gg4.txt`, `.planning/scripts/confluent-archive-allowlist-v1.7.0_gg5.txt` - line 1 only.
- `.planning/phases/43-document-fixes-phase-37-dispositions/43-COMMAND-EVIDENCE.md` - header, 10-row index, entries E-01 to E-05 (E-06 to E-10 stay pending for plans 43-02 and 43-03).
- `.planning/phases/43-document-fixes-phase-37-dispositions/43-evidence-check.py`, `43-testcase-status.py`, `43-doc-check.py` - byte copies, never edited.
- `.planning/phases/43-document-fixes-phase-37-dispositions/43-expected-added.tsv` - the two expected added PASS rows.

## Decisions Made

- 37-IN-04 is applied on the user's recorded answer, read from the committed research file rather than from any relayed message; the red-to-commit diff is exactly the one added `TAG_GATE_STEP,` line.
- 37-IN-05 is limited to the two lists the review names. The 25 and gg6 lists carry the same stale name but are deferred (row 37-IN-05(b)): the gg6 list belongs to the published tag and must not diverge from it.
- The full ClickHouse-backed suite is not run in this plan. Every changed Java file sits in the Docker-free `com.clickhouse.kafka.connect.ci` package and runs in `testUnit`; Phase 44 runs the two full suites on the release tip.
- `STATE.md` is advanced by the GSD verbs but not staged, as the plan's commit rules forbid it; the orchestrator commits it.

## Deviations from Plan

None - plan executed exactly as written. Two cosmetic notes: the plan's `<automated>` verify blocks were run as extracted bash scripts under `/bin/bash` (the shell alias `h` and zsh word handling make the one-line form fragile), with every clause kept; and the Task 3 verify's last clause (`git status` clean for the expected-added file) holds only after the E-05 evidence commit, which is where it was run to a pass.

## Issues Encountered

None. No JVM crash file appeared, the first Spotless loop in Task 3 was mis-quoted by zsh and was re-run as two plain commands (both `IS CLEAN`), and `origin/custom_build` equaled `72e8ccffe05ff6dffb14098d0b8c0f67aaa6a236` at every gate.

## Known Stubs

None.

## Threat Flags

None - the changes are two workflow comment lines, one test-source constant list entry, one test-helper condition, two inert comment lines and test methods; no endpoint, auth path, file access or schema change was added.

## User Setup Required

None - no external service configuration required.

## Next Phase Readiness

- Ready for plan 43-02 (the rollout notes and FORK-DELTA corrections). The pointers it and 43-03 consume: `code_commit` values `566f92e4f612dc4a33eebd3683ea04643e15daa9` (37-HDR, E-02), `33d079a986b04ade5c878caa0d3aa392915c38a8` (37-IN-04, E-03), `7796ad0ab338ca623f41901dcb36935c49226a40` (37-IN-06, E-04) and `103221a459dcd742a7404d3ea8eceefa17493a29` (37-IN-05(a), E-05).
- The testUnit baseline for later gates is 1826 tests (2 skipped); the anchors mirror stands at 190 rows.
- Scratch dumps in `/tmp/gsd43/exec` (`status-base-testunit.tsv`, `status-p01-testunit.tsv`) feed later deltas; if they are gone, re-measure on the current tree.
- DOC-02 is not marked complete: plans 43-02 and 43-03 still declare it. `.planning/STATE.md`, `.planning/config.json` and `.planning/state.json` are left modified for the orchestrator.

## Self-Check: PASSED

- Created files exist: the evidence file, the three helper copies and the expected-added list.
- Commits exist on HEAD: `566f92e4`, `ad3785d5`, `33d079a9`, `0df5f024`, `7796ad0a`, `527ea406`, `103221a4`, `8e5d4845`.

---
*Phase: 43-document-fixes-phase-37-dispositions*
*Completed: 2026-11-02*
