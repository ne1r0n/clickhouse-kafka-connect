---
phase: 11-full-suite-ci-publication
plan: 01
subsystem: testing
tags: [git-grep, merge-invariants, shell, merge-checklist, security-audit, stride]

requires:
  - phase: 06-merge-and-build-baseline
    provides: verify-merge-invariants.sh, MERGE-CHECKLIST.md Section 2 rows, the CR-01 carried defect
  - phase: 07-type-mapping-writer-helper-client-config
    provides: the writer/helper-client/config production surface audited retroactively here
  - phase: 09-metadata-free-json-path-keep-or-drop
    provides: the metadata-free bypass and its five guard sites audited retroactively here
provides:
  - verify-merge-invariants.sh that fails closed on a git grep tool error (exit status 0 / 1 / 2-or-greater)
  - the unattended invariant-script exit code over pre-v1.6.0-sync..HEAD (0) for D-03's evidence bundle
  - MERGE-CHECKLIST.md with zero open rows (48 done), each of the six last rows carrying its measured diff
  - 07-SECURITY.md and 09-SECURITY.md, closing the security-enforcement gap in the publishing phase
affects: [11-03, 11-05, 11-06, 11-07]

actuals:
  tokens: 8400
  tasks: 3
  commits: 3
plan_head_before: 3eedb43f0bc14f81bad53694a9bd14316e65043c
plan_head_after: ebd6c9e721ae35d847ac8a7072895469edc98280

tech-stack:
  added: []
  patterns:
    - "Capture a tool's exit status into a variable and branch on all three outcomes instead of testing it as an if condition"
    - "Prove a fail-open fix by running the pre-fix and fixed script on identical, out-of-tree, deliberately broken input"

key-files:
  created:
    - .planning/phases/07-type-mapping-writer-helper-client-config/07-SECURITY.md
    - .planning/phases/09-metadata-free-json-path-keep-or-drop/09-SECURITY.md
  modified:
    - .planning/scripts/verify-merge-invariants.sh
    - .planning/MERGE-CHECKLIST.md

key-decisions:
  - "Invariant 2's loop assigns a per-outcome message and emits the violation line and fail=1 once after the case, rather than setting fail=1 in each branch — this is what keeps the plan's own shape check (exactly four fail=1) satisfiable while still giving the tool-error case its own message"
  - "T-09-03 (R-01, high, accept) and T-07-SC (high, accept) are recorded closed, not escalated: both carry a documented prior disposition, and the blocking-human trigger is an open high finding"
  - "The client's raw splicing of jsonInsertColumns is recorded as a low accepted risk (AR-04), not a connector fix, because quoting it would be a dependency-level change outside this milestone"

patterns-established:
  - "Retroactive SECURITY.md in plan-time-register mode: verify each planned mitigation against current code, then add only threats that the live suite, review or the audit itself surfaced"

requirements-completed: [CI-07]

coverage:
  - id: D1
    description: "The invariant script's Invariant 2 distinguishes marker-found, clean, and git-grep tool error; a tool error is a reported violation carrying the numeric status and git's stderr"
    requirement: "CI-07"
    verification:
      - kind: other
        ref: "out-of-tree copy with pathspec ':(badmagic)src' (git grep exit 128): fixed copy exits 1 with INVARIANT 2 VIOLATION lines; pre-fix copy exits 0"
        status: pass
      - kind: other
        ref: "bash .planning/scripts/verify-merge-invariants.sh 673d29c3d5dc24b97de3b022acdfe473e1a5493c -> exit 0, empty stderr"
        status: pass
    human_judgment: false
  - id: D2
    description: "All six open MERGE-CHECKLIST.md Section 2 rows are done, each on a shortstat and resolved diff measured at execution time"
    requirement: "CI-07"
    verification:
      - kind: other
        ref: "grep -cF '| pending |' .planning/MERGE-CHECKLIST.md = 0 and grep -cF '| done |' = 48"
        status: pass
      - kind: other
        ref: "git diff --quiet v1.6.0 sync/v1.6.0 -- KeeperStateProvider.java settings.gradle.kts succeeds"
        status: pass
    human_judgment: false
  - id: D3
    description: "07-SECURITY.md and 09-SECURITY.md exist with every threat carrying a severity and a disposition, zero open"
    requirement: "CI-07"
    verification:
      - kind: other
        ref: "ls .planning/phases/*/[0-9]*-SECURITY.md returns 5 files; 40 register rows all with severity and disposition, 0 not closed"
        status: pass
      - kind: other
        ref: "git diff --name-only ebd6c9e7^..ebd6c9e7 -- src/ is empty; git status --porcelain -- src/ is empty"
        status: pass
    human_judgment: false
  - id: D4
    description: "The decision to record T-09-03 (R-01, high severity) and T-07-SC as closed accepted risks rather than escalate them at a blocking-human checkpoint"
    verification: []
    human_judgment: true
    rationale: "The plan's trigger wording ('severity high ... disposition not mitigate-and-verified') read literally also matches two already-dispositioned accept rows; whether treating them as closed is the intended reading is an interpretive call no command settles"

duration: 7min
completed: 2026-10-07
status: complete
---

# Phase 11 Plan 01: Pre-publication debt Summary

**The merge-invariant script now fails closed when `git grep` itself errors (proved by a rejected pathspec that the old script passed), the last six MERGE-CHECKLIST rows are closed on diffs measured now, and Phases 07 and 09 have 0-open security audits, with no production code touched.**

## Performance

- **Duration:** 7 min
- **Started:** 2026-10-07T10:31:41Z
- **Completed:** 2026-10-07T10:38:11Z
- **Tasks:** 3
- **Files modified:** 4 (2 modified, 2 created)

## Evidence numbers (plan 11-05 reads these into D-03's bundle)

| Item | Value |
|---|---|
| Invariant script, unattended, merge SHA `673d29c3d5dc24b97de3b022acdfe473e1a5493c`, arg 2 omitted | **exit 0**, 0 bytes on stderr (range `pre-v1.6.0-sync..HEAD` = 702 commits at Task 1; re-run on final HEAD: exit 0, 705 commits) |
| Rejected pathspec | `:(badmagic)src` — `git grep` exit **128** (`fatal: Invalid pathspec magic 'badmagic'`), measured directly against `HEAD` before use |
| Induced tool error, **pre-fix** script (out-of-tree copy) | exit **0** |
| Induced tool error, **fixed** script (out-of-tree copy) | exit **1**, 702 stderr lines each `INVARIANT 2 VIOLATION: search failed with exit status 128 on commit <sha>: fatal: Invalid pathspec magic …` |
| Checklist rows open / closed | **0** open / **48** closed (was 6 open / 42 closed) |
| `07-SECURITY.md` open threats | **0** (23 threats, 23 closed, 10 accepted risks) |
| `09-SECURITY.md` open threats | **0** (17 threats, 17 closed, 4 accepted risks) |
| `[0-9]*-SECURITY.md` files across phases | **5** (06, 07, 08, 09, 10) |

The six shortstats, `git diff --shortstat v1.6.0 sync/v1.6.0 -- <path(s)>`, measured at execution time:

| Row | Shortstat |
|---|---|
| `Record.java` | 1 file, 2 insertions, 3 deletions |
| `Data.java` | 1 file, 1 insertion, 2 deletions |
| `StructToJsonMap.java` | 1 file, 5 insertions, 6 deletions |
| five record convertors | 4 files, 7 insertions, 11 deletions |
| `InMemoryState.java` + `KeeperStateProvider.java` | 1 file, 0 insertions, 1 deletion |
| `settings.gradle.kts` | empty (byte-identical to `v1.6.0`) |

## Accomplishments

- Closed Phase 6's CR-01: Invariant 2 now branches on `git grep`'s exit status (0 marker, 1 clean, 2-or-greater tool error) and reports a failed search as a violation carrying the status and git's stderr, where it previously reported clean.
- Flipped all six Section 2 rows to `done`, each citing its own measured shortstat and that the resolved diff was read and carries no fork logic (import reordering and one blank line only).
- Produced `07-SECURITY.md` and `09-SECURITY.md` in plan-time-register mode, verifying every planned mitigation against current code; neither audit proposed a `src/main/` change.

## Task Commits

1. **Task 1: Fix the fail-open git-grep branch** - `4b03e6f3` (fix)
2. **Task 2: Close six Section 2 rows on measured diffs** - `83713d2c` (docs)
3. **Task 3: Retroactive security audits for Phases 07 and 09** - `ebd6c9e7` (docs)

**Plan metadata:** committed separately as `docs(11-01): complete pre-publication debt plan`.

## Files Created/Modified

- `.planning/scripts/verify-merge-invariants.sh` - Invariant 2 loop body only; 0/1 outer exit contract, marker construction, `SOURCE_PATHSPEC` (same seven entries) and Invariant 1 unchanged
- `.planning/MERGE-CHECKLIST.md` - single hunk over lines 60-65; owning-phase column untouched; Section 1 and Recorded Facts untouched
- `.planning/phases/07-type-mapping-writer-helper-client-config/07-SECURITY.md` - new
- `.planning/phases/09-metadata-free-json-path-keep-or-drop/09-SECURITY.md` - new

## Decisions Made

- Emit the violation line and `fail=1` once after the `case` (each outcome only assigns its message). The plan's own shape check requires exactly four `fail=1` tokens, which per-branch assignment would make five.
- Recorded T-09-03 (R-01) and T-07-SC as closed accepted risks. See Deviations and the coverage block's D4.
- Recorded the client's unquoted `jsonInsertColumns` splice as accepted risk AR-04 instead of changing connector code.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 1 - Bug, process] First Task 1 commit swept in a pre-staged file**
- **Found during:** Task 1 commit
- **Issue:** `git commit` committed the orchestrator's pre-staged `.planning/milestone.lock` alongside the script, which the dispatch forbade.
- **Fix:** `git reset --soft HEAD~1` on my own just-made commit (nothing pushed), then `git commit --only` on explicit paths. Used `--only` for every later commit. `milestone.lock` is still staged and uncommitted exactly as found.
- **Files modified:** none beyond the intended script
- **Verification:** `git show --stat` of `4b03e6f3` lists the script only; `git status` still shows `A .planning/milestone.lock`
- **Committed in:** `4b03e6f3`

### Plan-reading adjustments (not code changes)

- **Row wording corrected, not contradicted.** Two rows' original text overstated their diffs: `StringRecordConvertor.java` was described as an import-reorder diff but is byte-identical to upstream, and `KeeperStateProvider.java` was described as "import-reorder only" but is also byte-identical. Both facts are recorded in the rows' evidence clauses. Neither is fork logic, so the flip proceeded.
- **Audits added threats beyond the plan-time register,** each closed: 07 gained T-07-20 (debug logging of settings), T-07-21 (regex in `targetTableFilter`), T-07-22 (null nested value NPE, fixed in `e411353c`/`95e49964`); 09 gained T-09-15 (`auto.evolve` guard site, fixed in `51baea62`) and T-09-16 (client splices `jsonInsertColumns` unquoted).
- **T-07-05's plan-time mitigation was insufficient** (quote doubling alone allowed a backslash breakout); the register records the later fix as the real mitigation.
- **Skill adaptation:** `/gsd-secure-phase` expects to spawn a `gsd-security-auditor` sub-agent and make its own commit. This executor has no sub-agent tool, so the audit ran inline, and the single plan commit replaced the skill's commit step.

---

**Total deviations:** 1 auto-fixed (process), plus the adjustments above
**Impact on plan:** None on scope; no `src/` change, no existing test touched.

## Issues Encountered

- The blocking-human checkpoint did **not** trigger. Two register rows are high severity with disposition accept: T-09-03 (R-01, accepted by developer decision D-01 at Phase 9's blocking-human checkpoint) and T-07-SC (no install surface). Read literally, the plan's trigger wording could be taken to match both; I read the trigger as an *open* high finding and followed the plan's own instruction to record R-01 with its existing disposition. That reading is a human-judgment item (coverage D4) in case the reviewer disagrees.
- An untracked `.planning/.STATE.md.swp` swap file appeared during the run. It is not mine and was left alone.

## Known Stubs

None.

## Threat Flags

| Flag | File | Description |
|------|------|-------------|
| threat_flag: injection-residual | `src/main/java/com/clickhouse/kafka/connect/sink/db/ClickHouseWriter.java:1391` | client-v2 `0.9.6` splices `jsonInsertColumns` into `INSERT INTO t (cols)` without quoting; operator-supplied config, recorded as accepted risk AR-04 (T-09-16) |

## Next Phase Readiness

- Plan 11-05's evidence bundle can read the numbers above. Plan 11-06's assertions on zero open checklist rows and zero open high-severity findings now hold.
- `CI-07` is listed in this plan's frontmatter but is only *contributed to* here: the requirement is publication to the public fork, which plan 11-06 performs. It should not be marked complete on this plan's account.
- Recorded Facts 1-16 are unchanged, as required for plan 11-03's parallel derivation.
- The open code-review findings (07 WR-01, WR-02, IN-03; 09 IN-01) are carried in the two SECURITY.md files with their existing dispositions.

## Self-Check: PASSED

- Created files exist: both `*-SECURITY.md` and this SUMMARY (FOUND).
- Commits `4b03e6f3`, `83713d2c`, `ebd6c9e7` are ancestors of HEAD; `git rev-list --count 3eedb43f..HEAD` = 3 at measurement time.
- All three tasks' acceptance criteria and the plan-level verification re-ran green: invariant script exit 0; `SCRIPT_SHAPE_OK`; `CHECKLIST_ZERO_OPEN`; `TWO_PATHS_BYTE_IDENTICAL_TO_UPSTREAM`; `NO_SOURCE_EDITS`; `SECURITY_COVERAGE_COMPLETE`; `AUDITS_TOUCHED_NO_PRODUCTION_CODE`.

---
*Phase: 11-full-suite-ci-publication*
*Completed: 2026-10-07*
