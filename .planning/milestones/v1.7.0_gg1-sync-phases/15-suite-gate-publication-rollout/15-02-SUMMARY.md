---
phase: 15-suite-gate-publication-rollout
plan: 02
subsystem: code-review
tags: [code-review, d-10, d-04, metadata-free-json, describe-table, debezium]

requires:
  - phase: 15-suite-gate-publication-rollout
    provides: plan 15-01 commits 981a89d6 (D-01) and 4a91b92c (D-02, D-15) under review
provides:
  - 15-REVIEW.md, a standard-depth review of exactly the six files the D-01 and D-02 commits changed
  - Confirmation that no review finding forces a src/main change, so the code that the release documents describe is settled
  - Six Info items deferred with reasons, for the 15-07 approval bundle's knowingly-open list
affects: [15-03, 15-05, 15-06, 15-07]

plan_head_before: 0ff84d159fb2b39c5ff14ee6ba1ac4a4a3fd3b3a
plan_head_after: 55ff8e7429086dabec27305b16fcb36e254cac0b

actuals:
  tokens: 2400
  tasks: 2
  commits: 1

tech-stack:
  added: []
  patterns:
    - "Inline review against the gsd-code-reviewer procedure when the executor has no sub-agent tool (Phase 11 precedent)"

key-files:
  created:
    - .planning/phases/15-suite-gate-publication-rollout/15-REVIEW.md
  modified: []

key-decisions:
  - "No fix(15-02) commit: the review found 0 Critical and 0 Warning findings, so Task 2 made no code change and the src/main diff against the phase-start base stays at the two D-04 files"
  - "All six Info findings are deferred, not fixed: none is both inside the carve-out and worth the churn against upstream rebasing; IN-01 is a D-15 locked-wording matter"

requirements-completed: [PUB-02]

coverage:
  - id: D1
    description: "A standard-depth review of the six files changed by the D-01 and D-02 commits exists, is committed, and names all six files, before any release document is written"
    requirement: PUB-02
    verification:
      - kind: other
        ref: "Task 1 automated check: REVIEW-OK printed (file non-empty, six files named, status and findings.total present, tracked by git)"
        status: pass
    human_judgment: false
  - id: D2
    description: "After the review the D-01 and D-02 pins, testUnit and the merge-invariant script still pass, and src/main differs from the phase-start base in the two D-04 files only"
    requirement: PUB-02
    verification:
      - kind: unit
        ref: "env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew testUnit --rerun (454 tests, 0 failures, 0 errors, 2 skipped)"
        status: pass
      - kind: integration
        ref: "./gradlew test --rerun --tests '*MetadataFreeJsonWriterTest' --tests '*MetadataFreeJsonTypeParityLiveTest' --tests '*ClickHouseHelperClientForkLiveTest' (62 tests, 0 failures, 0 errors, 0 skipped)"
        status: pass
      - kind: other
        ref: "bash .planning/scripts/verify-merge-invariants.sh <first-parent merge> HEAD -> result=PASS; SCOPE-OK printed"
        status: pass
    human_judgment: false
  - id: D3
    description: "The review's severity calls (no Warning among the six findings, IN-01 deferred as a D-15 wording matter) are right"
    verification: []
    human_judgment: true
    rationale: "Severity classification of a diagnostic-accuracy finding is a judgment call; the user may prefer to promote IN-01 to a wording change"

duration: 5min
completed: 2026-10-10
status: complete
---

# Phase 15 Plan 02: Code review of the D-01 and D-02 commits Summary

**A standard-depth review of the six files changed by the Debezium Timestamp/NanoTimestamp fix and the Nullable(Enum|Tuple) discovery WARN found 0 Critical, 0 Warning and 6 Info items, so no fix was forced and the code the release documents will describe is settled.**

## Performance

- **Duration:** 5 min
- **Started:** 2026-10-10T10:37Z
- **Completed:** 2026-10-10T10:42Z
- **Tasks:** 2 (Task 2 closed with no code change)
- **Files modified:** 1 (`15-REVIEW.md`, new)

## Accomplishments

- `15-REVIEW.md` covers exactly the six files from the two 15-01 code commits, with `findings: critical 0, warning 0, info 6, total 6` and `status: issues_found`.
- The plan's explicit focus points were checked and recorded in the review's Summary: non-Number fall-through, overflow-free scaling, nested array/map/struct recursion, atomic warned-key set, WARN outside the write path and data-free, hook position in both describe loops, D-15 wording, fork-anchor counts (one each), and that the renamed unit test still covers MicroTimestamp and ZonedTimestamp.
- One empirical probe beyond reading: small `NanoTimestamp` values that render with a mantissa fraction in scientific notation (`1.23E-7`, `9.99E-7`, `-9.99E-7`, `-1.5E-8`) were fed to ClickHouse `latest` (`clickhouse-local`, `JSONEachRow` into `DateTime64(9)`) and read back as 123, 999, -999 and -15 ns. The server accepts the form, so no writer change is needed.

## Skill adaptation

The plan asks for `gsd-code-review` through the Skill tool, which spawns a `gsd-code-reviewer` sub-agent. This executor has no sub-agent tool, so the review ran inline, following `~/.claude/gsd-core/agents/gsd-code-reviewer.compact.md` at depth `standard` and writing `15-REVIEW.md` in the `14-REVIEW.md` schema (frontmatter `phase`, `reviewed`, `depth`, `files_reviewed`, `files_reviewed_list`, `findings`, `status`). This matches the Phase 11 precedent (11-01-SUMMARY). The review file records "run inline by the plan executor" in its footer.

## Review counts

| Severity | Count |
|---|---|
| Critical | 0 |
| Warning | 0 |
| Info | 6 |
| Total | 6 |

## Outcome table (Critical and Warning findings)

No Critical or Warning finding exists, so the table has zero rows, equal to `findings.critical + findings.warning = 0`. Task 2 therefore made no `fix(15-02)` commit and recorded "no findings" per its step 4. No seen-RED evidence applies.

## Info findings (all deferred; they join the 15-07 knowingly-open list)

| Id | Where | Outcome |
|---|---|---|
| IN-01 | `ClickHouseHelperClient` WARN text | deferred: the WARN fires on routes where the defect cannot occur (schemaless JSON, String, `bypassRowBinary=true`) because discovery cannot see the route; this is D-15 as locked, and routing knowledge needs `ClickHouseWriter` edits outside the D-04 carve-out. FORK-DELTA Known costs (15-06) can state the route restriction |
| IN-02 | `ClickHouseHelperClientForkLiveTest` | deferred: the V1 describe hook is exercised only under `CLIENT_VERSION=V1`; the suite target is V2 only |
| IN-03 | `WARNED_NULLABLE_ENUM_OR_TUPLE` | deferred: set never evicts and the `.`/`:` key can collide for dotted names; bounded and harmless in practice |
| IN-04 | `ClickHouseHelperClientForkUnitTest` | deferred: the WARN wording is not pinned by a test (mechanism is); wording stays a read-by-human deliverable (coverage D3 of 15-01) |
| IN-05 | `ClickHouseHelperClient` lines 490-499 | deferred: constants sit mid-class; a style move adds diff churn against upstream rebasing for no behaviour change |
| IN-06 | `MetadataFreeJsonWriterTest` | deferred: edge pins cover the mantissa-less scientific form only; the mantissa-with-fraction form was verified against the server in this review but is not a committed test |

## Task Commits

1. **Task 1: Review the D-01 and D-02 files (D-10 step 2)** - `55ff8e74` (docs)
2. **Task 2: Resolve Critical and Warning findings inside the D-04 carve-out** - no commit (no findings)

**Plan metadata:** recorded in the docs commit that follows this file.

## Verification results

- Task 1 automated check printed `REVIEW-OK`.
- Task 2 automated checks, all run after the review commit:
  - `env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew testUnit --rerun`: `BUILD SUCCESSFUL`; XML totals 454 tests, 0 failures, 0 errors, 2 skipped.
  - `./gradlew test --rerun --tests '*MetadataFreeJsonWriterTest' --tests '*MetadataFreeJsonTypeParityLiveTest' --tests '*ClickHouseHelperClientForkLiveTest'`: `BUILD SUCCESSFUL`; XML totals 62 tests, 0 failures, 0 errors, 0 skipped (ClickHouse `latest`, client V2, colima profile `vz`).
  - `SCOPE-OK` printed: `git diff --name-only e4755e38..HEAD -- src/main` lists `MetadataFreeJsonWriter.java` and `ClickHouseHelperClient.java` only.
  - Invariant script: `invariants: provenance_paths=21 anchor_commits=105 anchor_rows=43 marker_commits=152 result=PASS`, exit 0.

## Files Created/Modified

- `.planning/phases/15-suite-gate-publication-rollout/15-REVIEW.md` - the scoped review

## Decisions Made

- No finding reaches Warning. IN-01 was the closest candidate (an unqualified sentence in a WARN that fires on unaffected routes); it stays Info because D-15 locks both the firing condition and the wording, and a fix would either change user-decided text or reach into `ClickHouseWriter`.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 3 - Blocking] Review ran inline instead of through the Skill tool**
- **Found during:** Task 1
- **Issue:** the `gsd-code-review` skill spawns a sub-agent, and this executor has no sub-agent tool. The plan's Execution environment section pre-authorises the inline route.
- **Fix:** followed the reviewer agent definition inline and wrote the file in the house schema. See "Skill adaptation".
- **Files modified:** none beyond `15-REVIEW.md`
- **Verification:** `REVIEW-OK`
- **Committed in:** `55ff8e74`

---

**Total deviations:** 1 (Rule 3, pre-authorised by the plan; no production impact)
**Impact on plan:** none on scope or outputs.

## Issues Encountered

None. The planned single-repo commit ledger file (`gsd-plan-head-before-15-02`) was not created before the first commit; `plan_head_before` was taken from the HEAD at plan start (`0ff84d15`) and `commits: 1` was measured with `git rev-list --count 0ff84d15..HEAD` before this file was written.

## Known Stubs

None.

## Threat Flags

None. T-15-06 (scope creep through a review fix): no `src/main` file was edited, and `SCOPE-OK` confirms the diff against `e4755e38`. T-15-07 (a finding dropped before publication): every one of the six findings has a recorded outcome above.

## User Setup Required

None - no external service configuration required.

## Next Phase Readiness

- The D-01/D-02 code is final. Plans 15-03 onward can describe and measure it; any later `src/main` edit re-opens the gate.
- 15-06 should state the IN-01 route restriction in the FORK-DELTA Known costs entry the WARN points at, and 15-07 carries IN-01 to IN-06 in the knowingly-open list.

## Self-Check: PASSED

- `15-REVIEW.md` exists on disk and is tracked by git.
- Commit `55ff8e7429086dabec27305b16fcb36e254cac0b` is an ancestor of HEAD; `git rev-list --count 0ff84d15..HEAD` was 1 before this file.
- `REVIEW-OK`, `SCOPE-OK` and the invariant `result=PASS` printed; `testUnit` and the three named test classes passed.

---
*Phase: 15-suite-gate-publication-rollout*
*Completed: 2026-10-10*
