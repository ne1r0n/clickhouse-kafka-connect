---
phase: 34-upstream-tail-merge
plan: "01"
subsystem: upstream-sync
tags: [git-merge, upstream-sync, verify-merge-invariants, merge-checklist, kafka-connect, clickhouse]

requires:
  - phase: 33-dependency-sweep-protobuf-decision
    provides: the sweep tip (client 0.9.6) and the unedited gate scripts proven by the Phase 33 self-test
provides:
  - "local rollback tag pre-v1.7.0-tail-2 (lightweight, M3's first parent, never pushed)"
  - "M3 adaf8c4e, one mechanical two-parent merge of upstream main 9bed6f79 (no authored line, no .planning path)"
  - "MERGE-CHECKLIST-v1.7.0-tail-2.md and conflicted-paths-v1.7.0-tail-2.txt (the standing tail gate inputs for Phases 35-37)"
  - "34-COMMAND-EVIDENCE.md with E-01..E-04 and the measured break inventory of the bare merge"
affects: [34-02, 34-03, 35-buffered-fix, 36-client-bump, 38-release]

actuals:
  tokens: 53742
  tasks: 2
  commits: 12

tech-stack:
  added: []
  patterns:
    - "rehearse-then-reproduce merge: tag, re-read upstream immediately before merging, resolve by side/union only, record in the next commit"
    - "evidence file read back by a read-only helper (34-evidence-check.py); facts are never edited to pass a gate"

key-files:
  created:
    - .planning/phases/34-upstream-tail-merge/34-COMMAND-EVIDENCE.md
    - .planning/MERGE-CHECKLIST-v1.7.0-tail-2.md
    - .planning/scripts/conflicted-paths-v1.7.0-tail-2.txt
  modified:
    - CHANGELOG.md
    - src/main/java/com/clickhouse/kafka/connect/sink/ProxySinkTask.java
    - src/main/java/com/clickhouse/kafka/connect/util/Utils.java
    - src/test/java/com/clickhouse/kafka/connect/sink/ClickHouseSinkTaskMappingTest.java
    - src/test/java/com/clickhouse/kafka/connect/sink/db/ClickHouseWriterTest.java

key-decisions:
  - "ProxySinkTask takes the fork side in both hunks inside M3; the DBWriter seam is deliberately absent until plan 34-02 (TAL-02), so the bare merge has exactly the predicted 5 test-compile errors."
  - "Utils.java and ClickHouseSinkTaskMappingTest.java import hunks were ordered for the fork's sorted import order (a within-hunk reordering), so M3's blobs for those two files differ from the research rehearsal by line order only; ClickHouseWriterTest's Supplier import sits at its within-hunk position after the org.slf4j imports."

patterns-established:
  - "Run the unedited tail gate with explicit SHAs for the merge and the range end, never a moving HEAD"
  - "In zsh, read a blob as git show \"${M}:path\"; $M:src is parsed as a history modifier"

requirements-completed: [TAL-01, TAL-03]

coverage:
  - id: D1
    description: "Upstream main 9bed6f79 merged as one mechanical commit M3 on sync/v1.7.0 with a local rollback tag, a conflicted-paths record and a resolution checklist; the unedited tail gate passes"
    requirement: "TAL-01"
    verification:
      - kind: other
        ref: "PRE_TAG=pre-v1.7.0-tail-2 UPSTREAM_REF=9bed6f79... CONFLICTED_PATHS_FILE=... bash .planning/scripts/verify-merge-invariants.sh adaf8c4e f6f4dd8e -> provenance_paths=27 anchor_rows=131 anchor_exempt=0 result=PASS"
        status: pass
      - kind: other
        ref: "/bin/bash /tmp/gsd34/exec/verify1.sh -> P34-01-T1-MERGE-OK, P34-01-T1-GATE-OK, P34-01-T1-COMMITTED"
        status: pass
    human_judgment: false
  - id: D2
    description: "Baseline attribution: green testUnit and full suite on the untouched pre-merge tip, and the bare merge's break inventory (compileJava and compileTestFixturesJava pass, compileTestJava has exactly 5 javac errors in 2 files)"
    requirement: "TAL-03"
    verification:
      - kind: other
        ref: "python3 -I 34-evidence-check.py expect 34-COMMAND-EVIDENCE.md E-04 ... -> OK, 11 checks, 0 failed; verify P34-01-T2-OK"
        status: pass
    human_judgment: false

duration: 12min
completed: 2026-10-28
status: complete
plan_head_before: 6de1e0bdda8b4a3ef90b7d4f0d4d4b45dee3e2eb
plan_head_after: 967c55214c878172adb1713ad2fe75167f8da04d
commits: 12
---

# Phase 34 Plan 01: Upstream tail merge Summary

**Upstream's untagged tail 9bed6f79 merged into sync/v1.7.0 as one mechanical, gated merge commit (M3 adaf8c4e) with a local rollback tag and a resolution checklist, and the bare merge's compile breaks measured as exactly the predicted 5 javac errors in 2 files.**

## Performance

- **Duration:** about 12 min (including the 3 min baseline suite)
- **Started:** 2026-10-28T21:09Z (approximate; the baseline testUnit started 21:12:46Z)
- **Completed:** 2026-10-28T21:21Z
- **Tasks:** 2
- **Files modified:** 30 (27 upstream-changed paths via M3, plus the checklist, the conflicted-paths file and the evidence file)

## Accomplishments

- Pre-flight (E-01): upstream main still `9bed6f79`, newest upstream tag `v1.7.0`, `merge-tree` exit 1 with exactly the five expected paths, origin `custom_build` still `3a3a15a9`, gate scripts byte-identical to the Phase 33 gate head, anchors mirror `rows=131 bad=0`.
- Baseline on the untouched tip: testUnit 1387 tests / 0 failures / 0 errors / 2 skipped; full suite 1974 / 0 / 0 / 18 skipped (the 18 named skips) on ClickHouse 26.10.3.7 x client V2, BUILD SUCCESSFUL in 2m 44s, no JVM crash; 587 Docker-only tests.
- Lightweight tag `pre-v1.7.0-tail-2` on `68f1b0cb`, upstream re-read immediately before the merge (E-02), and M3 `adaf8c4e`: second parent the full upstream SHA, first-parent diff exactly the 27 upstream-changed paths, no `.planning` path, ProxySinkTask on the fork side (`new SinkTaskStatistics(connectorName, id)` once), `compileJava` green before the commit.
- Record commit `f6f4dd8e`: `conflicted-paths-v1.7.0-tail-2.txt` (five lines) and `MERGE-CHECKLIST-v1.7.0-tail-2.md` (Section 1 conflicted, Section 2 all 27 paths, Section 3 breaks without a marker, gate commands, Recorded Facts 44-45).
- The unedited tail gate over M3..record commit: `invariants: provenance_paths=27 anchor_commits=2 anchor_rows=131 marker_commits=9 anchor_exempt=0 result=PASS` (E-03); re-run live by the plan's verify.
- Break inventory on the bare merge (E-04): `compileJava` and `compileTestFixturesJava` pass; `compileTestJava` fails with exactly 5 javac errors (`AdditionalRetriableErrorCodesTest` x3 on the missing DBWriter seam constructor, `UtilsNullMessageClassificationUnitTest` x2 on the stale four-argument `handleException`); no fork file calls a `CreateTableStatement` constructor; `handleException` has one overload; 9 three-argument `ServerException` sites in 2 files for Phase 36.

## Task Commits

1. **Task 1: Tail-merge tracer** - `68f1b0cb` (docs: E-01 evidence), `adaf8c4e` (merge M3), `f6f4dd8e` (docs: conflicted-paths record and checklist), `3bcc27f6` (docs: E-02 and E-03)
2. **Task 2: Post-merge break inventory** - `967c5521` (docs: E-04)

**Plan metadata:** the SUMMARY and STATE/ROADMAP commit follows this file.

`commits: 12` is the measured `git rev-list --count 6de1e0bd..HEAD`: the plan's 5 commits plus the 7 upstream commits M3 brings in.

## Files Created/Modified

- `.planning/phases/34-upstream-tail-merge/34-COMMAND-EVIDENCE.md` - Phase 34 evidence, index E-01..E-11, entries E-01..E-04
- `.planning/MERGE-CHECKLIST-v1.7.0-tail-2.md` - resolution record for M3
- `.planning/scripts/conflicted-paths-v1.7.0-tail-2.txt` - merge-tree conflict list over M3's parents, read by the tail gate
- `CHANGELOG.md`, `ProxySinkTask.java`, `Utils.java`, `ClickHouseSinkTaskMappingTest.java`, `ClickHouseWriterTest.java` - the five hand-resolved paths inside M3 (side/union only)

## Decisions Made

- ProxySinkTask keeps the fork side in M3; the DBWriter seam is re-expressed in plan 34-02 so that the bare merge's breaks are exactly the measured set.
- The import hunks of `Utils.java` and `ClickHouseSinkTaskMappingTest.java` were ordered for the fork's sorted import order (within-hunk reordering only), and `ClickHouseWriterTest.java`'s `Supplier` import sits at its within-hunk position after the `org.slf4j` imports. The checklist row for the latter records this.
- The checklist's Sections 1 and 2 were generated by a scratch script (not committed) from the conflicted-paths file and `git diff --name-status`, per the plan's no-hand-transcription rule.

## Deviations from Plan

None - plan executed exactly as written, with one evidence housekeeping correction: the E-01 entry was first committed with an end timestamp (`21:18:40Z`) written ahead of the clock. The E-02/E-03 commit corrects it to the suite log's own end (`21:15:47Z`) and says so in the entry. No fact, count or verdict changed.

**Total deviations:** 0 auto-fixed. **Impact on plan:** none.

## Issues Encountered

- zsh parsed `$M:src/...` as a history modifier in one `git show` check, which printed a spurious `0` fork-line count; re-running as `"${M}:path"` gave the expected `1`. The plan's verify blocks run under `/bin/bash`.
- The bare merge leaves 5 test-compile errors by design; `testUnit` and the full suite are therefore not runnable on M3 until plan 34-02. This is the expected state.

## User Setup Required

None - no external service configuration required. Nothing was pushed: `pre-v1.7.0-tail-2` and `sync/v1.7.0` are absent on origin and origin `custom_build` is still `3a3a15a9`.

## Next Phase Readiness

- Ready for plan 34-02: repair commits for the ProxySinkTask three-constructor shape with anchor rows (TAL-02), `UtilsNullMessageClassificationUnitTest` on the five-argument `handleException` (TAL-03), and the BLD-16 re-bless of `connect-api-references.txt`.
- Requirements TAL-01 and TAL-03 are also declared by later plans in this phase, so they are not marked complete here.
- For the plan summary shown to the user (research Open Question 3): a `DataException` that wraps a retriable ClickHouse server error is classified retriable by the existing root-cause-first rule; plan 34-03 pins only the plain cases.

## Known Stubs

None.

## Self-Check: PASSED

- FOUND: `.planning/phases/34-upstream-tail-merge/34-COMMAND-EVIDENCE.md`, `.planning/MERGE-CHECKLIST-v1.7.0-tail-2.md`, `.planning/scripts/conflicted-paths-v1.7.0-tail-2.txt`
- FOUND commits: `68f1b0cb`, `adaf8c4e`, `f6f4dd8e`, `3bcc27f6`, `967c5521` (all ancestors of HEAD)
- P34-01-T1-MERGE-OK, P34-01-T1-GATE-OK, P34-01-T1-COMMITTED, P34-01-T2-OK all printed on the committed tip; anchors mirror `rows=131 bad=0`.

---
*Phase: 34-upstream-tail-merge*
*Completed: 2026-10-28*
