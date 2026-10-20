---
phase: 24-release-publication
plan: "03"
subsystem: release
tags: [release, rollout-note, rollback, error-text, unpaired-element-warn, escaped-names, in-flight-batches, evidence]

requires:
  - phase: 24-release-publication
    provides: plan 24-01 evidence E-01..E-05 (MBean and client name, Map-key residual branch 1, shipped-archive diff) and plan 24-02 evidence E-10..E-16 (column trees, rollback retry, detection queries)
provides:
  - "docs/ROLLOUT-v1.7.0_gg3.md: the gg2 to gg3 operator note with seven three-part items (ROLL-03)"
  - "Evidence E-20 (config grep), E-21 (alert grep), E-22 (rollback on a dry-run merge of the tip) and E-23 (log grep)"
  - "Local lightweight tag pre-v1.7.0_gg3 on 6473e24f (not on origin; pushed only in plan 24-07)"
  - "Every evidence verdict that backs the note flipped to yes; E-03, E-04, E-15 and E-16 still wait on plan 24-04"
affects: [24-04, 24-05, 24-06, 24-07]

actuals:
  tokens: 12876
  tasks: 3
  commits: 4
plan_head_before: b06775ee41b8f4761bb7475b6b0e0b247176f8cd
plan_head_after: 28160b13f3bc3d632e0add09b0253bbca4684f5d
commits: 4

tech-stack:
  added: []
  patterns:
    - "Every sql and sh block of an operator note is byte-equal to an evidence block, checked mechanically"
    - "A rollback command in a release doc is proven by script on a dry-run --no-ff merge of the tip into the pre-merge tag"

key-files:
  created:
    - docs/ROLLOUT-v1.7.0_gg3.md
  modified:
    - .planning/phases/24-release-publication/24-COMMAND-EVIDENCE.md

key-decisions:
  - "Item 1 is worded on D-04 branch 1: the headline 'no record value' carries the D-14 exception sentence for a Map key on the input_format_binary_read_json_as_string route, with the three places it appeared and the two it did not, measured on 26.9.19.9"
  - "Item 2 gives no workaround and no schema advice (D-02); the 'whatever the connector settings' sentence is followed by what was actually measured (client V2, default route) and says the explicit insertFormat=json route was not measured"
  - "Item 4 uses the narrowed shape query of E-13 (a backslash followed by n) and the E-12 rollback result: upgrade needs no clean stop, rollback does, for that one shape"

patterns-established:
  - "The first task commit holds the note, the second holds the dry-run evidence, so the merged subject X exists before it is recorded"

requirements-completed: [ROLL-03]

coverage:
  - id: D1
    description: "docs/ROLLOUT-v1.7.0_gg3.md has the seven items in order with a matching table of contents, each with What changed, Am I affected? and What to do; the gg2 pointer and upstream base sit in the first 15 lines"
    requirement: ROLL-03
    verification:
      - kind: other
        ref: "plan 24-03 verify commands ROLLOUT-FRAME-OK, ROLLOUT-T3-OK"
        status: pass
    human_judgment: false
  - id: D2
    description: "Item 1 value-free headline with the measured Map-key exception (D-04 branch 1, server 26.9.19.9), the new text, the unchanged DLQ headers and two self-checks proven on scratch directories (E-20, E-21)"
    requirement: ROLL-03
    verification:
      - kind: other
        ref: "plan 24-03 verify command ITEMS-1-7-OK"
        status: pass
    human_judgment: true
    rationale: "Operator-facing wording of a security-relevant claim; the pre-gate code review should read it against E-03"
  - id: D3
    description: "Items 2 and 3 quote the proven size-element suspects query, the log grep (E-23), the shape query and the escaped-name query verbatim, with no workaround for the size defect"
    requirement: ROLL-03
    verification:
      - kind: other
        ref: "plan 24-03 verify command ITEMS-2-3-OK"
        status: pass
    human_judgment: true
    rationale: "The no-workaround statement (D-02) and the hedge on routes that were not measured are judgment calls for the reviewer"
  - id: D4
    description: "Item 7 rollback commands ran by script on a dry-run --no-ff merge of the tip into 6473e24f (E-22); tag pre-v1.7.0_gg3 is local only and its tests.yaml triggers on branch pushes only"
    requirement: ROLL-03
    verification:
      - kind: other
        ref: "E-22 rollback-dry.log (MERGE-DRY-RUN-OK, ok, doc gone, revert restores the tree) and ITEMS-1-7-OK tail"
        status: pass
    human_judgment: false
  - id: D5
    description: "Items 4, 5 and 6: one sentence each way per change, the shape-limited clean stop with the proven KeeperMap query, MBean and client name, and the three dependency updates leaving the shipped jar unchanged"
    requirement: ROLL-03
    verification:
      - kind: other
        ref: "plan 24-03 verify commands ROLLOUT-T3-OK and ROLLOUT-FINAL-OK"
        status: pass
    human_judgment: false

duration: 6min
completed: 2026-10-20
status: complete
---

# Phase 24 Plan 03: The gg2 to gg3 rollout note Summary

**docs/ROLLOUT-v1.7.0_gg3.md with seven three-part items, every quoted query and command taken from a Phase 24 transcript, and a rollback proven by script on a dry-run merge of the tip into the pre-merge tag**

## Performance

- **Duration:** 6 min
- **Started:** 2026-10-20T16:45:13Z
- **Completed:** 2026-10-20T16:51:30Z
- **Tasks:** 3
- **Files modified:** 2 (1 created, 1 modified); nothing outside `docs/` and `.planning/` changed

## Accomplishments

- The note exists with these seven items, in order: **Item 1** Record values in writer error text; **Item 2** The unpaired-element WARN and Tuple elements named size; **Item 3** Backslashes and line breaks in names; **Item 4** In-flight batches; **Item 5** JMX MBean name and client name; **Item 6** Dependency updates; **Item 7** Rollback. The first 15 lines send operators on a release before gg2 to the gg2 note, name upstream base `1.7.0` and list the placeholders.
- **D-04 branch wording used: branch 1.** Item 1 contains the D-14 sentence ("the only exception we know of is a Map key, on connectors whose `clickhouseSettings` sets `input_format_binary_read_json_as_string` to `1` or `true` and whose records carry a value Jackson cannot serialize"), a table of the three places the key appeared (task status cause chain, DLQ stack-trace header, `Error inserting records` throwable) and the two it did not (DLQ message header, `Error writing value of` line), the server version 26.9.19.9, and the self-check `grep -rnF 'input_format_binary_read_json_as_string' <connector-config-directory>` with its empty-result meaning. The derived internal flag is never named.
- Item 2 explains that the new WARN marks a table hit by a defect older than the release (a Tuple element named `size` or `size0` at any depth, or `size` plus digits under an `Array`), that inserts fail with code 32 or 33, quotes the suspects query with its one over-flag, gives the log grep as the exact check, and states plainly that this release has no fix and offers no workaround.
- Item 3 explains the COLS-13 change with the narrowed shape query, and beside it the known limitation (escaped Kafka field names in a table, the default stored silently, a double quote leaving the table unmapped) with the escaped-name query and no changelog pointer.
- Item 4 gives one sentence each way for the four other changes, and for the one differing shape says the upgrade needs no clean stop and a rollback does, with the KeeperMap query proven on a gg3-created state table and no instruction to empty or edit the state table.
- Item 7 restores the tree of `pre-v1.7.0_gg3` (`6473e24f`) with `git read-tree -u --reset` plus a commit, points at item 4 before redeploying, tells the operator to rebuild tag `v1.7.0_gg2` with `./gradlew createConfluentArchive` (no release asset exists), and lists what rolling back brings back with item links.

### Evidence entries each item rests on

| Item | Evidence |
|------|----------|
| 1 | E-03 (branch, destinations, server version), E-20, E-21, ledger L-42..L-45, L-48 |
| 2 | E-15, E-23, ledger L-51 |
| 3 | E-10, E-11, E-13, E-16, ledger L-50 |
| 4 | E-10, E-11, E-12, E-13, E-14 |
| 5 | E-02 |
| 6 | E-04 (E-05 is for plan 24-04) |
| 7 | E-04, E-22 |

### Dry-run merge (E-22)

- X (the commit that added the note's frame, item 1 and item 7): `1ea720ede4b0e04abc6b89fdb338288be8912ffd`
- D (the `--no-ff` merge of X into `6473e24f372a8b37dfd8cc8cf3124e6cff52edca` in the throwaway worktree, never pushed): `02e0e5f057409cc15cfd2169496c928b05bfd746`
- `MERGE-DRY-RUN-OK`: two parents, `D^1` is `6473e24f`, `D^2` is X, `git diff --quiet D X` holds. The script printed `ok` after `git read-tree -u --reset pre-v1.7.0_gg3` and the commit, `rollout doc gone from branch`, `item-7 headings: 1`, and a tree equal to D after `git revert <rollback-commit>`. The dry-run worktree and the demo branch were removed. Items 2 to 6 were written after X, so plans 24-06 and 24-07 re-run the script on the publication tip and on the real merge.

## Task Commits

1. **Task 1: frame, item 1 and item 7 with their proofs (tracer)** - `1ea720ed` (note, E-20, E-21), `84d2d5a4` (E-22 and the E-03, E-04 verdicts). The tracer verify was re-run end to end after the commits (`ROLLOUT-FRAME-OK`, `ITEMS-1-7-OK`); mode `end-of-phase` with an automated-only verify, so expansion followed without a checkpoint.
2. **Task 2: items 2 and 3** - `9d4cb9b2` (`ITEMS-2-3-OK`)
3. **Task 3: items 4 to 6 and the whole-note checks** - `28160b13` (`ROLLOUT-T3-OK`, `ROLLOUT-FINAL-OK`; all three tasks' checks re-run on the committed tip)

**Plan metadata:** the commit of this SUMMARY and the state update.

## Files Created/Modified

- `docs/ROLLOUT-v1.7.0_gg3.md` - the note (332 lines)
- `.planning/phases/24-release-publication/24-COMMAND-EVIDENCE.md` - E-20..E-23 added, verdicts flipped
- Scratch, not committed: `/tmp/gsd24/rollback-demo.sh`, `/tmp/gsd24/conncfg`, `conncfg-empty`, `alerts`, `alerts-empty`, `alerts-escaped`, `logs`, `logs-empty`, `/tmp/gsd24/exec/rollback-dry.log`
- Local tag `pre-v1.7.0_gg3` on `6473e24f` (lightweight, not on origin)

## Decisions Made

- The headline claim in item 1 is stated as the change, and only the exception is stated as a measurement with a server version. The Phase 22 live tests pin the headline on `latest` with client V2, but their server version is not in a Phase 24 transcript, so the note does not name one for it.
- The unpaired-element item says why a table can appear in the log without being written by the connector: discovery describes every table the `targetTableFilter` lets through.
- Evidence IDs (E-nn), decision IDs and phase numbers appear nowhere in the note; only `(ledger L-nn)` tags do.

## Deviations from Plan

### Auto-fixed Issues

None needed a fix to code or tests.

### Wording adjustments to stay inside the measurements

**1. [Rule 2 - Missing critical: accuracy] "Inserts fail whatever the connector settings" is scoped to what was measured**
- **Found during:** Task 2, item 2
- **Issue:** The plan and D-02 require the sentence that inserts into a size-element table fail whatever the connector settings. E-15 measured client V2 on the default RowBinary route only. The explicit `insertFormat=json` route runs no table description (`ClickHouseWriter.start` skips it), so it neither logs the WARN nor was measured.
- **Fix:** The required sentence is kept, followed by what was measured and by the statement that the explicit JSON route was not. No workaround was added.
- **Files modified:** `docs/ROLLOUT-v1.7.0_gg3.md`
- **Committed in:** `9d4cb9b2`

**2. [Rule 2 - Missing critical: accuracy] Item 3 separates the measured insert outcomes from the by-analogy ones**
- **Found during:** Task 2, item 3
- **Issue:** E-11 inserted four of the seven differing root shapes; the other differing shapes (an element that is itself Nullable, a nested Tuple) differ in their column description in the same way but were not inserted.
- **Fix:** The note says the four inserted shapes failed on gg2 with code 33, and that the outcome of the other listed shapes is likely, not measured.
- **Committed in:** `9d4cb9b2`

**3. [Plan structure] Task 1 produced two commits, not one**
- **Issue:** The rollback proof needs the note committed first (X is the merged subject) and E-22 records X.
- **Fix:** One commit for the note with E-20 and E-21, one for E-22 and the verdict flips. Items 2 to 6 were short placeholders in the first commit and are replaced in Tasks 2 and 3.
- **Committed in:** `1ea720ed`, `84d2d5a4`

### Statements dropped for lack of evidence

- Why the Jackson failure passes through unchanged (the `IOException` pass-through) is not in a Phase 24 transcript; item 1 says only that its text is passed on unchanged.
- No claim was written about the `bypassRowBinary=true` or schemaless routes for the size defect.

---

**Total deviations:** 3 wording or structure adjustments, 0 code changes.
**Impact on plan:** None on scope. Wording stays inside the measurements; the extra commit is a bookkeeping consequence of the dry-run order.

## Issues Encountered

- A first run of the dry-run script was not needed twice; it passed on the first run. `git fetch --tags origin` inside the script only fetched.
- The `Edit` tool noticed that the note changed on disk after a scripted edit of my own; no external change.

## Known Stubs

None. The placeholder bodies of items 2 to 6 in the first commit were replaced in the same plan; `grep -c "completed in a later step"` on the final note prints 0.

## Threat Flags

None. Only a document and evidence changed. T-24-08: the only state query is the read-only SELECT of the gg2 note, and the verify rejects any non-SELECT sql block and any TRUNCATE instruction in docs, CHANGELOG and README. T-24-09: placeholders only; the secret-pattern scan passed. T-24-10: the D-14 sentence matches E-03's `D-04-BRANCH=1` line and names 26.9.19.9. T-24-11: the tag is local and absent from origin, and no dry-run worktree or demo branch remains.

## User Setup Required

None - no external service configuration required.

## Next Phase Readiness

- Plan 24-04 can quote the note and flip E-03, E-04, E-15 and E-16, whose verdicts now read `pending (24-04)`; E-05 reads `pending (24-04)` as before.
- Plan 24-06 and 24-07 must re-run `/tmp/gsd24/rollback-demo.sh` on the publication tip and on the real merge commit.
- For the pre-gate review: items 1 and 2 carry the two judgment-heavy statements (the Map-key exception and the no-fix wording).

## Self-Check: PASSED

- Files: `docs/ROLLOUT-v1.7.0_gg3.md`, `24-COMMAND-EVIDENCE.md` exist; `### E-20:` to `### E-23:` present.
- Commits `1ea720ed`, `84d2d5a4`, `9d4cb9b2`, `28160b13` are ancestors of HEAD; `git rev-list --count b06775ee..HEAD` = 4 before this SUMMARY.
- Acceptance: `ROLLOUT-FRAME-OK`, `ITEMS-1-7-OK`, `ITEMS-2-3-OK`, `ROLLOUT-T3-OK` and `ROLLOUT-FINAL-OK` printed on the committed tip; `git diff --name-only 07bf1e76 HEAD` outside `.planning` lists `VERSION`, the new note and the Phase 24 live test only.

---
*Phase: 24-release-publication*
*Completed: 2026-10-20*
