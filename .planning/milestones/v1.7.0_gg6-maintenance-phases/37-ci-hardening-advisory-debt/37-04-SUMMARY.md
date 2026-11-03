---
phase: 37-ci-hardening-advisory-debt
plan: "04"
subsystem: ci
tags: [python, junit, gitattributes, fork-anchors, review-debt, spotless]

requires:
  - phase: 37-ci-hardening-advisory-debt
    provides: "plan 37-03: the release workflows, the anchors mirror at rows=171, the pin-guard tally 44"
provides:
  - "failed_tests_summary.py states Result unknown for unreadable reports, warns on zero testcases, escapes Markdown backslash first and reads glob-named directories literally"
  - ".gitattributes marks integrationTests.yaml -text (blob 3e9b9be0 unchanged, text: unset), pinned by a guard test and anchors row 172"
  - "ConfluentArchiveCheck names the lib jars it found when the archive shape is wrong"
  - "the reference derivation fails on a member no loaded type declares (measured: zero unresolved)"
  - "a Docker-free bell pin in ColumnTest, the helper client closed in DescribeUnreadableEnumMockServerTest"
  - "E-13, E-14, E-15 (pass)"
affects: [37-05, 38-release]

plan_head_before: b03a940e72fe5f5d3df7a2dd256ba7bbb247bde6
plan_head_after: dc10b078a6a8347e90e5ac8c14411706e8e38041

actuals:
  tokens: 13877
  tasks: 3
  commits: 9

tech-stack:
  added: []
  patterns:
    - "measure in a throwaway worktree first, then apply the same edit script to the main tree only when the measured rule holds"
    - "stage the edited files in the detached formatting worktree before Spotless so its numstat shows only Spotless's own hunks"

key-files:
  created: []
  modified:
    - .github/scripts/failed_tests_summary.py
    - .gitattributes
    - .gitignore
    - .planning/scripts/fork-anchors.txt
    - docs/FORK-DELTA.md
    - src/test/java/com/clickhouse/kafka/connect/ci/FailedTestsSummaryScriptUnitTest.java
    - src/test/java/com/clickhouse/kafka/connect/ci/UpstreamIntegrationWorkflowBytesGuardUnitTest.java
    - src/test/java/com/clickhouse/kafka/connect/ci/ConfluentArchiveCheck.java
    - src/test/java/com/clickhouse/kafka/connect/ci/ConfluentArchiveCheckUnitTest.java
    - src/test/java/com/clickhouse/kafka/connect/ci/ConnectApiReferencesUnitTest.java
    - src/test/java/com/clickhouse/kafka/connect/sink/db/mapping/ColumnTest.java
    - src/test/java/com/clickhouse/kafka/connect/sink/db/helper/DescribeUnreadableEnumMockServerTest.java
    - .planning/phases/37-ci-hardening-advisory-debt/37-COMMAND-EVIDENCE.md

key-decisions:
  - "The attributes rule requires that the only .gitattributes line starting with the workflow path is exactly the -text line, so a later line that turns conversion back on is rejected too"
  - "The reference derivation was committed, not dispositioned: the measurement found zero unresolved members"
  - "The bell pin was committed, not dispositioned: both inputs give the measured key"

patterns-established:
  - "Existing test files change by additions, a record component, an extra output line and whitespace-neutral try-with-resources only; the cumulative assertion diff from the phase start stays empty"

requirements-completed: [ADV-01, ADV-02]

coverage:
  - id: D1
    description: "The failed-test summary no longer claims a clean result it does not know: unreadable reports give Result unknown with no success notice, an empty run warns, Markdown metacharacters are escaped backslash first, glob characters in the directory name are literal"
    requirement: "ADV-01"
    verification:
      - kind: unit
        ref: "FailedTestsSummaryScriptUnitTest (13 executions; the five new ones were red on the old script and green on the asset); 37-COMMAND-EVIDENCE.md E-13"
        status: pass
    human_judgment: false
  - id: D2
    description: ".gitattributes keeps the upstream-identical integrationTests.yaml out of line-ending conversion, with a guard test, an anchors row and FORK-DELTA rows"
    requirement: "ADV-01"
    verification:
      - kind: unit
        ref: "UpstreamIntegrationWorkflowBytesGuardUnitTest (9 to 11 executions); git check-attr text reports unset, blob 3e9b9be0 unchanged; anchors mirror rows=172 bad=0; E-13"
        status: pass
    human_judgment: false
  - id: D3
    description: "The archive check names the jars it found (and the stale-directory hint) when the archive does not hold exactly one lib jar"
    requirement: "ADV-02"
    verification:
      - kind: unit
        ref: "ConfluentArchiveCheckUnitTest (6 to 7 executions, existing assertions unedited); E-14"
        status: pass
    human_judgment: false
  - id: D4
    description: "The reference derivation fails on a connector-referenced member that no loaded type declares; measured first, zero unresolved members"
    requirement: "ADV-02"
    verification:
      - kind: unit
        ref: "ConnectApiReferencesUnitTest (3 executions green on the stricter derivation); E-14 derive_unresolved: 0"
        status: pass
    human_judgment: false
  - id: D5
    description: "The fourth control character (bell) is pinned Docker-free and the helper client of the describe mock-server test is closed; the plan-level testUnit is green and equals the sum of the added executions; no existing assertion deleted or weakened, src/main unchanged"
    requirement: "ADV-02"
    verification:
      - kind: unit
        ref: "ColumnTest (50 to 51), DescribeUnreadableEnumMockServerTest (2 to 2); testUnit 1633 tests, 0 failures, delta 94 equals the sum of additions; cumulative assertion diff clean; E-15"
        status: pass
    human_judgment: false

duration: 8min
completed: 2026-10-31
status: complete
---

# Phase 37 Plan 04: Carried review rows fixed by script, test and comment Summary

**The failed-test summary script now says `Result unknown` for reports it cannot read, warns on an empty run, escapes Markdown backslash first and reads glob-named directories, `integrationTests.yaml` is protected from line-ending conversion by `.gitattributes` with a guard test and anchors row 172, the archive check names the jars it found, the reference derivation fails on unresolvable members (measured zero), a Docker-free bell pin and a closed helper client land, and no existing assertion changed.**

## Performance

- **Duration:** about 8 min (wall clock of the run)
- **Started:** 2026-10-31T09:31Z (approximate, first command)
- **Completed:** 2026-10-31T09:39Z
- **Tasks:** 3 (all auto)
- **Files modified:** 13 in the diff, none created

## Accomplishments

- E-13: five new `FailedTestsSummaryScriptUnitTest` executions failed on the unedited script (13 executions, 5 failures, exactly the new names) and passed on the plan asset copied over it (mode 100755 kept). `.gitattributes` gained `.github/workflows/integrationTests.yaml -text`: `git check-attr` moved from `text: unspecified` to `text: unset`, the blob stays `3e9b9be0`, `git status` for the file is clean. The byte guard class went from 9 to 11 executions, the anchors mirror reads `rows=172 bad=0`, FORK-DELTA names the attribute and the summary script.
- E-14: `ConfluentArchiveCheck.Result` has a third component `libJarNames`; `run` prints one extra stderr line only when the archive does not hold exactly one lib jar; one CI-20 test pins the two-jar case (6 to 7). The reference derivation was measured in a throwaway worktree with a lookup that separates "a Kafka type declares it", "a non-Kafka type declares it" and "no type declares it": 0 unresolved members and green, so the same change was committed. The `.gitignore` typo is fixed (1/1).
- E-15: `ColumnTest` pins an enum label holding a bell from the JSON escape and from the raw character (50 to 51, green on first run, committed). `DescribeUnreadableEnumMockServerTest` closes the helper client by try-with-resources, no assertion line changed (2 to 2). The 27 IN-02 intro was re-read (current; class name stale; rename cost 4). Plan gate: cache-busted `testUnit` 1633 tests, 0 failures, 0 errors, 2 skipped; delta 94 over E-04 equals the sum of added executions (27 + 19 + 34 + 5 + 5 + 2 + 1 + 1); cumulative assertion diff from the phase start prints nothing; `src/main` unchanged; anchors mirror `rows=172 bad=0`.

## Task Commits

1. **Task 1: script, attribute and guard**
   - `46fcba8d` fix: the summary script and its five new tests (summary_commit)
   - `b3446dc5` fix: `.gitattributes`, the guard, the anchors row, FORK-DELTA (attributes_commit)
   - `61a5d796` docs: E-13
2. **Task 2: jar names, derivation, typo**
   - `e266a5b4` test: the archive check names the jars (check_commit)
   - `d93ff629` test: the derivation fails on an unresolved member (derive_commit)
   - `6ca4c460` chore: the `.gitignore` typo (typo_commit)
   - `cdab3a09` docs: E-14
3. **Task 3: the bell pin and the closed helper client**
   - `1c583b46` test: the pin and the close (pin_commit)
   - `dc10b078` docs: E-15

## Commits

- `summary_commit`: `46fcba8d97ded2f5243b31b4a147e962e7612ae7` (31-REVIEW WR-01, WR-02, IN-01, IN-03 script half)
- `attributes_commit`: `b3446dc59348733d420add3e036e1a1f1582a103` (31-REVIEW WR-03)
- `check_commit`: `e266a5b4609f175bfbe0f8000569a9282533dc47` (25-REVIEW IN-01)
- `derive_commit`: `d93ff6298ef9e63121a50eeec40f1676ad5bb6f3` (25-REVIEW WR-02, first half)
- `typo_commit`: `6ca4c460ba246b3cb47ec422218cf255fe2805cc` (25-REVIEW IN-04 b)
- `pin_commit`: `1c583b46249ab9bd30ff717c256c7f583c4f0ed9` (28-REVIEW IN-04 a, IN-05 close half)

## Measurements for the ledger

- **25 WR-02:** `derive_decision: committed`, `derive_unresolved: 0`, `derive_unresolved_names: none`. The first half (fail on an unresolved member) is fixed by `derive_commit`. The second half (the derivation resolves members on the test classpath, whose Kafka version can differ from the compile pin) stays a disposition (`derive_classpath_half: dispositioned`).
- **28 IN-04:** `bell_pin: committed`; `bell_observed`: both the JSON-escape text and the raw-character text give the map of `a`, U+0007, `b` to 1 and `z` to 2. The live half (an entry for the four characters in the shared raw-name matrix, which needs a Docker run and an edit of that matrix) stays a disposition (`bell_live_half: dispositioned`).
- **28 IN-05:** the close half is fixed (`close_helper: try-with-resources`). The null assertion is not tightened (it would change an assertion and needs the user's sign-off): disposition.
- **27 IN-02:** `intro_reads_current: yes`, `class_name_stale: yes`, `rename_cost: 4` (lines of `docs/FORK-DELTA.md`; none in `CHANGELOG.md`). No edit; the stale class name is accepted, a disposition for plan 37-05.
- **Rows that stayed dispositions inside this plan:** 25 WR-02 classpath half, 28 IN-04 live half, 28 IN-05 null assertion, 27 IN-02 class name.

## Files Created/Modified

- `.github/scripts/failed_tests_summary.py` - byte copy of the plan asset, mode 100755
- `.gitattributes`, `.gitignore` - one line each
- `.planning/scripts/fork-anchors.txt` - 171 to 172 rows; `docs/FORK-DELTA.md` - three edits of the byte-guard row and the summary-script row
- Seven existing Java test files, additions only (and a try-with-resources re-indentation)
- `.planning/phases/37-ci-hardening-advisory-debt/37-COMMAND-EVIDENCE.md` - entries E-13 to E-15, index rows flipped

## Decisions Made

- The attributes rule is slightly stricter than the plan's wording: the only line of `.gitattributes` that starts with the workflow path must be exactly the `-text` line, so `text`, `text eol=crlf` and a later re-enabling line are all rejected (fixtures for each).
- The derivation change is structured so the lookup returns the declaring type or empty and the caller collects the unresolved members into one `IllegalStateException`, instead of the lookup throwing and the caller catching, so the existing class-load failure is not caught by mistake. The behaviour the plan names is the same.
- STATE.md, ROADMAP.md, REQUIREMENTS.md, `.planning/state.json` and `.planning/config.json` were not touched by this run, per the orchestrator's staging rules; `ADV-01` and `ADV-02` are listed in `requirements-completed` for the orchestrator to mark after verification (ADV-01 and ADV-02 also need plan 37-05's ledger).

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 3 - Blocking] Spotless baseline taken once for all seven files**
- **Found during:** Task 1 step 1.
- **Issue:** the plan asks for a baseline per task; the pristine files do not change between tasks except through this plan's own edits.
- **Fix:** one pristine run for the seven existing files (empty for five, 6/8 for `DescribeUnreadableEnumMockServerTest`, 5/7 for `ColumnTest`), and per task the edited copies staged in the detached worktree before Spotless ran (as plan 37-03). Recorded in E-13 and E-15.
- **Committed in:** not applicable (measurement method).

**2. [Plan wording] Derivation structure and attributes-rule strictness** - see Decisions Made; both recorded in E-14 and E-13.

**3. [Note] Evidence timestamps** use the run's clock; E-13 and E-14 were stamped by hand and by script within a minute of their commits.

---

**Total deviations:** 1 blocking method note, 2 wording notes. **Impact on plan:** none on scope or results; no `src/main`, dependency, existing assertion, workflow or origin change. Carry-overs (a) to (e) were applied: `tests>=N` is quoted and no stray `=N` file exists, evidence keys are lowercase `[a-z0-9_]+`, no new Javadoc text contains `extends `, path lists in verify commands are C-sorted, and the `scratch/*` clause was not part of this plan's verifies.

## Issues Encountered

- No OpenJ9 crash, core dump or javacore appeared in any run.
- Nothing was pushed, dispatched or written to GitHub; `git worktree list` shows one entry and `/tmp/gsd37/w` holds only its two older directories.

## Known Stubs

None.

## Threat Flags

None. The script change adds no input path or command surface (it only escapes more characters and reads the same files), and `.gitattributes` narrows, not widens, conversion.

## User Setup Required

None.

## Handoffs

- **Plan 37-05:**
  - The anchors count is `rows=172`; the cache-busted `testUnit` total is 1633 tests (2 skipped); the pin-guard tally is still 44 (`TestsWorkflowPinGuardUnitTest`).
  - Commit pointers for the ledger: `summary_commit` `46fcba8d` (31 WR-01, WR-02, IN-01, IN-03 script half), `attributes_commit` `b3446dc5` (31 WR-03), `check_commit` `e266a5b4` (25 IN-01), `derive_commit` `d93ff629` (25 WR-02 first half), `typo_commit` `6ca4c460` (25 IN-04 b), `pin_commit` `1c583b46` (28 IN-04 a and IN-05 close half).
  - The measurements and the four in-plan dispositions are in the section above; 25 WR-02's classpath half, 28 IN-04's live half, 28 IN-05's null assertion and 27 IN-02's class name need an owner and a re-open trigger each.
  - The last commit outside `.planning` made by this plan is `1c583b46249ab9bd30ff717c256c7f583c4f0ed9` (the `<gated sha>` that `37-USER-DISPATCH-DRYRUN.md` names must account for plan 37-05's own non-`.planning` commits, if any).
  - The `increment-version.yml` header comment (plan 37-03 deviation 5) is still open for a decision.
- **Orchestrator:** mark ADV-01 and ADV-02 only after plan 37-05; STATE.md and ROADMAP.md are untouched; E-12 stays `pending (user-gated)`.

## Next Phase Readiness

Plan 37-05 can start: the tree is clean apart from the orchestrator-owned state files, the evidence index reads pass for E-01 to E-11 and E-13 to E-15, pending for E-12 (user-gated) and E-16 to E-18, `testUnit` is green at 1633 tests, and the anchors mirror reads `rows=172 bad=0`.

## Self-Check: PASSED

- Files: every file listed under key-files.modified exists; the plan assets and both helper files are unchanged against the phase start.
- Commits `46fcba8d`, `b3446dc5`, `61a5d796`, `e266a5b4`, `d93ff629`, `6ca4c460`, `cdab3a09`, `1c583b46`, `dc10b078` are ancestors of `HEAD`.
- All six plan `<automated>` checks print their OK marker (`P37-04-T1-OK`, `P37-04-T1-TESTS-OK`, `P37-04-T2-OK`, `P37-04-T2-TESTS-OK`, `P37-04-T3-OK`, `P37-04-T3-TESTS-OK`).

---
*Phase: 37-ci-hardening-advisory-debt*
*Completed: 2026-10-31*
