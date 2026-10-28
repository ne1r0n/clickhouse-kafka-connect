---
phase: 32-release-publication
plan: "01"
subsystem: release-engineering
tags: [release, version-bump, confluent-archive, allow-list, javap, jmx, github-rest]

requires:
  - phase: 30-release-workflows-pin-guard
    provides: two green dry runs (40296330498, 40296481765) at b847fbdb, the Wrapper Gradle annotation
  - phase: 31-fork-owned-nightly-integration-tests
    provides: green integrationTest run at c4b409db (E-18), the nightly workflow and its toggle
provides:
  - VERSION v1.7.0_gg5 with its fork-anchors row in one commit
  - .planning/scripts/confluent-archive-allowlist-32.txt (four exact-path rows)
  - 32-COMMAND-EVIDENCE.md with E-01..E-05 (measured inputs for the rollout note, CHANGELOG and FORK-DELTA)
affects: [32-02, 32-03, 32-04, 32-05, 32-06, 32-07]

plan_head_before: 0791a8a7fb04031423eaad6cdd0bac27a10d7845
plan_head_after: b321839811902d1fbd6d90578a39bc7c2aac94fc

actuals:
  tokens: 13549
  tasks: 3
  commits: 5

tech-stack:
  added: []
  patterns:
    - "per-release archive allow-list with exact paths, one row per inlined-constant class"
    - "measure on the final tree, then write: every document number comes from an E-nn entry"

key-files:
  created:
    - .planning/scripts/confluent-archive-allowlist-32.txt
    - .planning/phases/32-release-publication/32-COMMAND-EVIDENCE.md
  modified:
    - VERSION
    - .planning/scripts/fork-anchors.txt
    - .planning/phases/32-release-publication/32-01-PLAN.md

key-decisions:
  - "Archive allow-list holds four exact-path rows, not two (user decision A, 32-AMENDMENT-01): javac inlines Version.ARTIFACT_VERSION into ClickHouseHelperClient.class and SinkTaskStatistics.class"
  - "No wildcard and no catch-all row; compare-confluent-archives.py and the 25 and 29 allow-lists stay unedited"
  - "Plan 32-01 own wording moved from two rows / changed 2 to four rows / changed 4 in the same commit as the allow-list; plans 32-02 and 32-03 are left to their executors"

requirements-completed: [CIG-02]

coverage:
  - id: D1
    description: "VERSION reads v1.7.0_gg5, the generated ARTIFACT_VERSION agrees after clean, and the VERSION anchor row changed in the same commit (anchors mirror rows=131 bad=0)"
    requirement: CIG-02
    verification:
      - kind: other
        ref: "plan 32-01 Task 1 verify: VERSION-GG5-OK and ANCHORS-MIRROR-OK"
        status: pass
    human_judgment: false
  - id: D2
    description: "A live task registers MBeans with version=v1.7.0_gg5 (per-task and per-topic) and sends user agent clickhouse-kafka-connect/v1.7.0_gg5; the gg4 tag shows v1.7.0_gg4 (E-02)"
    requirement: CIG-02
    verification:
      - kind: other
        ref: "plan 32-01 Task 1 verify: MBEAN-PROBE-OK (Phase32RolloutProbe, scratch, ClickHouse latest x client V2)"
        status: pass
    human_judgment: false
  - id: D3
    description: "Archive at tag v1.7.0_gg4 versus the gg5 tip: RESULT PASS, only-a 0 only-b 0 changed 4 under the four-row allow-list, with the javap proof (E-03)"
    requirement: CIG-02
    verification:
      - kind: other
        ref: "plan 32-01 Task 2 verify: ARCHIVE-COMPARE-OK"
        status: pass
    human_judgment: false
  - id: D4
    description: "Release path unchanged since Phase 30 dry runs except VERSION; both dry runs green with the Wrapper Gradle annotation read again from the REST API (E-04)"
    requirement: CIG-02
    verification:
      - kind: other
        ref: "plan 32-01 Task 2 verify: RELEASE-PATH-OK"
        status: pass
    human_judgment: false
  - id: D5
    description: "Origin custom_build is 21c51598, the release merge is clean and publishes the tip's tree, src/main equals v1.7.0_gg4, integration inputs unchanged since c4b409db, GitHub state recorded (E-05)"
    requirement: CIG-02
    verification:
      - kind: other
        ref: "plan 32-01 Task 3 verify: GROUND-TRUTH-OK"
        status: pass
    human_judgment: false

duration: 20min
completed: 2026-10-26
status: complete
---

# Phase 32 Plan 01: Release identity bump and measured release inputs Summary

**VERSION bumped to v1.7.0_gg5 with its anchor row in one commit, and E-01..E-05 measure the MBean and client name, the gg4-versus-gg5 archive (four release-identity classes, javap-proven), the unchanged release path and origin's merge shape for the rollout documents.**

## Performance

- **Duration:** about 20 min of active work (excluding the wait for the user's allow-list decision)
- **Started:** 2026-10-26T20:52:00Z (approximate; plan commit 0791a8a7 at 20:52:15Z)
- **Completed:** 2026-10-26T21:13:00Z
- **Tasks:** 3
- **Files modified:** 5 (VERSION, fork-anchors.txt, the new allow-list, the new evidence file, this plan's own text)

## Accomplishments

- Bump commit `a6bfd78f` changes exactly `VERSION` and line 41 of `fork-anchors.txt`; after `clean compileJava` the generated `ARTIFACT_VERSION` is `v1.7.0_gg5` and the anchors mirror reads `rows=131 bad=0`.
- A live sink task on ClickHouse `26.10.3.7` (`latest` image `sha256:739e099abd11...b1ce`, client V2) registers the per-task and the per-topic MBean with `version=v1.7.0_gg5` and sends `clickhouse-kafka-connect/v1.7.0_gg5 clickhouse-java-v2/0.9.6 (Mac OS X; jvm:17.0.9) Apache-HttpClient/5.6.5`; the same probe on tag `v1.7.0_gg4` shows `v1.7.0_gg4`, and the rest of the user agent is identical.
- The archive built at tag `v1.7.0_gg4` (SHA-256 `455df230...3088`) and at the gg5 tip (`ab37b0a2...a682e`) both ran `verifyConfluentArchive`; the compare prints `RESULT: PASS` with `files-a 7399 files-b 7399 only-a 0 only-b 0 changed 4 same 7395`. The four changed entries are `Version.class`, `ClickHouseHelperClient.class`, `SinkTaskStatistics.class` and `META-INF/MANIFEST.MF`; `javap -c -p -constants` shows only the `v1.7.0_gg4` -> `v1.7.0_gg5` string differing in the three classes.
- Release path: `release.yaml`, `pre-release.yaml`, `build.gradle.kts`, `settings.gradle.kts`, `gradle/`, `gradlew`, `src/main`, `ConfluentArchiveCheck.java` and `config/` are unchanged since `b847fbdb`; only `VERSION` differs. Both dry runs (head_sha `b847fbdb`, `workflow_dispatch`, success) carry the `Wrapper Gradle` notice ("Gradle 9.8.1 from the wrapper ran createConfluentArchive and verifyConfluentArchive"). Nothing was dispatched.
- Ground truth (E-05): origin `custom_build` is `21c51598` (tag `v1.7.0_gg4`), `HEAD..origin/custom_build` is exactly that commit, its second parent `3d847009` is an ancestor of HEAD, and the merge of it and the tip produces the tip's tree (left count 1, right 75, per PT-02). `src/main` is byte-identical to `v1.7.0_gg4`; gg5 changes 14 paths outside `.planning`. The integration inputs are unchanged since `c4b409db`; GitHub reports default branch `custom_build`, `integrationTests.yaml` `disabled_manually` with no schedule run after 2026-10-16, the nightly absent from `custom_build` (404), 0 releases, and open Dependabot PRs #4 and #10.

## Task Commits

1. **Task 1: VERSION and anchor row, clean build, MBean and client-name probe** - `a6bfd78f` (chore, bump) and `4c3b42e6` (docs, E-01 and E-02), written by the previous agent
2. **Task 2: Archive compare under the Phase 32 allow-list and the release path** - `a3b7c236` (docs: allow-list, E-03, E-04, plan wording)
3. **Task 3: Origin, merge shape, src/main, integration gate and GitHub state** - `b3218398` (docs: E-05)

Orchestrator commit inside the measured range: `4b985fe8` (`docs(32): record amendment 01, four-row archive allow-list`), which counts in `commits: 5`. **Plan metadata:** the commit that carries this SUMMARY.

## Files Created/Modified

- `VERSION` - release identity `v1.7.0_gg5`
- `.planning/scripts/fork-anchors.txt` - line 41 `VERSION<TAB>v1.7.0_gg5<TAB>1<TAB>BLD-07`
- `.planning/scripts/confluent-archive-allowlist-32.txt` - four exact-path rows for the inlined release-identity constant
- `.planning/phases/32-release-publication/32-COMMAND-EVIDENCE.md` - header and E-01..E-05
- `.planning/phases/32-release-publication/32-01-PLAN.md` - eight wording/verify-string edits from two rows / changed 2 to four rows / changed 4

## Decisions Made

- The allow-list has four exact-path rows (user decision A, recorded in `32-AMENDMENT-01.md`), because javac inlines the compile-time constant `Version.ARTIFACT_VERSION` into `ClickHouseHelperClient.class` (`CONNECT_CLIENT_NAME`) and `SinkTaskStatistics.class` (MBean version segment). A wildcard or an `only-b`/`changed` slack was not added.
- The CI-built gg4 archive of the dry runs (`5a8522a8...638c`, 12453663 bytes) is not compared byte for byte with the local archive A (12453558 bytes); the claim rests on the entry-by-entry compare.

## Deviations from Plan

### Auto-fixed Issues

None under Rules 1-3. One plan-level deviation resolved by a user decision:

**1. [Rule 4 - Architectural/scope] Allow-list is four rows, not two**
- **Found during:** Task 2 step 4 (the compare)
- **Issue:** The two-row list failed with two `UNALLOWED CHANGED` lines and totals `changed 4`; the plan said to stop, not to add a row.
- **Fix:** Stopped with a `blocking-human` decision; the user chose "A: 4-row allow-list". The allow-list, plan 32-01's own wording and verify strings, and E-03 (with the javap proof) were updated in one commit.
- **Files modified:** `.planning/scripts/confluent-archive-allowlist-32.txt`, `32-01-PLAN.md`, `32-COMMAND-EVIDENCE.md`
- **Commit:** `a3b7c236` (amendment record `4b985fe8`)

**Also noted:** the E-05 commit was amended once (`30340c80` -> `b3218398`, local only, before any later commit) because two descriptive lines in the `text` block repeated the `default_branch:` and `releases:` key names that the E-05 verify reads as single lines.

---

**Total deviations:** 1 (a user-decided allow-list change)
**Impact on plan:** Plans 32-02 and 32-03 still say "two rows / changed 2" and must read it as four rows / changed 4 per `32-AMENDMENT-01.md`; their executors were not edited here by instruction.

## Issues Encountered

- The planned two-row allow-list was wrong for this tree (see the deviation); no code or build input changed, the measurement was right.
- Every dirty-tree item outside the plan (` M gradlew.bat`, `.planning/state.json`, `.planning/milestone.lock`, the orchestrator's `STATE.md` edit) was left unstaged.

## Known Stubs

None - this plan changes `VERSION`, an anchors row, an allow-list and evidence only.

## User Setup Required

None - no external service configuration required.

## Next Phase Readiness

- Ready for 32-02 (the rollout note): E-02, E-03, E-04 and E-05 hold every number it cites. Its claim 2 and 32-03's text must use "four rows / changed 4 / differs only in the release identity: the Version constant and the two classes that inline it, plus the manifest Implementation-Version".
- `Matches text:` verdicts of E-02..E-05 stay `pending` until 32-02 and 32-03 flip them.
- Floating `latest` was `sha256:739e099abd11...b1ce` (2026-10-26T00:06:54Z), `SELECT version()` = `26.10.3.7`, for every live measurement here.

## Self-Check: PASSED

Created files exist (`VERSION`, the allow-list, the evidence file, `fork-anchors.txt`); commits `a6bfd78f`, `4c3b42e6`, `a3b7c236`, `b3218398` are ancestors of HEAD; the plan's verify commands print VERSION-GG5-OK, ANCHORS-MIRROR-OK, MBEAN-PROBE-OK, ARCHIVE-COMPARE-OK, RELEASE-PATH-OK and GROUND-TRUTH-OK; `git diff --name-only 8dcbbfc1 HEAD -- . ':(exclude).planning'` lists exactly `VERSION`.

---
*Phase: 32-release-publication*
*Completed: 2026-10-26*
