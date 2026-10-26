---
phase: 29-release-publication
plan: "04"
subsystem: release
tags: [release, rollout-note, gg3-to-gg4, raw-identifier-names, size-named-elements, in-flight-batches, rollback, kafka-connect-worker, advisories, clickhouse-26.9]

requires:
  - phase: 29-release-publication
    provides: "plans 29-01 to 29-03: the bump commit, the archive compare, E-01..E-25 and the corrected gg1 and gg2 notes"
  - phase: 28-raw-identifier-names-in-discovery
    provides: "the behaviour ledger L-58..L-66 and the handoff table of name forms"
  - phase: 27-writer-failure-path-test-debt
    provides: "the behaviour ledger L-52..L-57"
provides:
  - "docs/ROLLOUT-v1.7.0_gg4.md: the gg3 to gg4 operator rollout note, seven three-part items, 356 lines"
  - "29-COMMAND-EVIDENCE.md E-30 (item 7 rollback on a dry-run merge of the tip) and every note-backing verdict flipped"
  - "local lightweight tag pre-v1.7.0_gg4 on 69cd680b (not on origin)"
affects: [29-05, 29-06, 29-07, 29-08]

actuals:
  tokens: 14185
  tasks: 3
  commits: 6

tech-stack:
  added: []
  patterns:
    - "Every sql and sh block of the note is a byte-for-byte evidence block, checked by script; every behaviour statement carries a measured, read from connector code or inferred label"
    - "The rollback commands run by script against a dry-run --no-ff merge of the tip into the rollback target, never pushed"

key-files:
  created:
    - docs/ROLLOUT-v1.7.0_gg4.md
  modified:
    - .planning/phases/29-release-publication/29-COMMAND-EVIDENCE.md

key-decisions:
  - "Item 7 names the ` M gradlew.bat` line as the one expected output of git status --porcelain in a fresh checkout of the published tip (committed CRLF blob against .gitattributes text eol=crlf), measured in the dry run"
  - "Item 1 carries both name queries and the label query, the over-flag of the name queries (a backtick in a Tuple element name) and the Enum8-only caveat of the label query"
  - "Item 2 names both over-flags of the suspects query (s4_o01 and x08) and says the server code is 32 or 33, not a code per shape"
  - "The server version 26.9.19.9 stays in the note literally, so the IP-address scan of NOTE-FINAL-OK is run with that one string masked"

requirements-completed: [ROLL-04, OPS-06]

plan_head_before: 2d3d9c598d20b4831d6771a1b5acfc8ec2bab613
plan_head_after: be420666d2e96c8e49b0dd7dc0596ac35dfc5050
duration: 10min
completed: 2026-10-23
status: complete

coverage:
  - id: D1
    description: "docs/ROLLOUT-v1.7.0_gg4.md has the seven items in order, each with What changed, Am I affected? and What to do, a matching table of contents, the rename notice and the gg3 pointer in the first 15 lines, no bare planning ID, every sql and sh block equal to an evidence block and every sql block read-only"
    requirement: ROLL-04
    verification:
      - kind: other
        ref: "NOTE-FRAME-OK and ITEMS-5-6-OK (the plan's verify commands, run as /tmp/gsd29-scripts/p04/cmd1.sh and cmd4.sh)"
        status: pass
    human_judgment: false
  - id: D2
    description: "Item 1 states the per-character name forms for both clients, the root-silent and element-loud failure, the Enum label paragraph, both name queries and the label query with their empty-result meanings, and the backlog-wide rename precondition"
    requirement: ROLL-04
    verification:
      - kind: other
        ref: "ITEMS-1-7-OK (the plan's verify command, /tmp/gsd29-scripts/p04/cmd2.sh)"
        status: pass
    human_judgment: true
    rationale: "Whether the wording of each measured claim matches its evidence entry is a reading task; the fresh-context review rounds of plan 29-06 check it"
  - id: D3
    description: "Items 2 to 4 state the size-named element scope with the two over-flags, the writer text changes with the accepted residual, and the per-change, per-direction in-flight behaviour with the corrected state-table query"
    requirement: ROLL-04
    verification:
      - kind: other
        ref: "ITEMS-2-4-OK (the plan's verify command, /tmp/gsd29-scripts/p04/cmd3.sh)"
        status: pass
    human_judgment: true
    rationale: "Quantifiers and the per-direction wording need a reader who checks them against E-10..E-15, E-21 and E-25; plan 29-06 does this"
  - id: D4
    description: "Items 5 and 6 state the MBean and client name, the Connect worker facts (production worker, oldest verified worker, KIP-1161), the archive compare and both advisories by source with no exposure assessment"
    requirement: OPS-06
    verification:
      - kind: other
        ref: "ITEMS-5-6-OK (the plan's verify command, /tmp/gsd29-scripts/p04/cmd4.sh)"
        status: pass
    human_judgment: true
    rationale: "Advisory wording is third-party information read on 2026-10-23; the reviewer checks it against E-07"
  - id: D5
    description: "Item 7 rollback commands (git read-tree -u --reset pre-v1.7.0_gg4, commit, diff, revert, show) ran by script on a dry-run merge of the tip into 69cd680b, with the local tag on 69cd680b whose tests.yaml triggers on branch pushes only and which is not on origin"
    requirement: ROLL-04
    verification:
      - kind: other
        ref: "E-30 and ITEMS-1-7-OK; MERGE-DRY-RUN-OK; git ls-remote --tags origin lists no pre-v1.7.0_gg4"
        status: pass
    human_judgment: false
---

# Phase 29 Plan 04: The gg3 to gg4 rollout note Summary

**`docs/ROLLOUT-v1.7.0_gg4.md` is written: seven three-part items (names and Enum labels as written in `CREATE TABLE`, size-named Tuple elements, writer text, in-flight batches per direction, MBean and client name, dependencies and the Connect worker, rollback), every command in it equal to an evidence block, and the rollback proven by script on a dry-run merge, which also found that a fresh checkout of the rollback tree lists `gradlew.bat` as modified.**

## Performance

- **Duration:** 10 min (18:25Z to 18:35Z)
- **Tasks:** 3 (1 tracer, 2 auto)
- **Files modified:** 2 (`docs/ROLLOUT-v1.7.0_gg4.md` new, 356 lines; `29-COMMAND-EVIDENCE.md`); nothing else under `docs/`, `src/`, the build, `gradle/`, `.github/` or CHANGELOG changed

## Accomplishments

- **The frame.** Within the first 15 lines: the rename notice naming `CREATE TABLE`, the pointer to `ROLLOUT-v1.7.0_gg3.md`, the upstream base `1.7.0` with upstream's tail `a6f1df83` (an ancestor of HEAD), the links, the placeholder list, the three-part and ledger-tag sentences and the D-09 label sentence.
- **Item 1 (names and labels).** The per-character table for both clients from E-20 and the 28 handoff table; root silent versus element loud with the measured failure text; the Enum label paragraph (nine of twelve forms differ, seven fail as the withheld lookup miss, two as an unmapped table); the gg3 name query unchanged, the companion and the label query in `sql` blocks with empty-result meanings, the recorded over-flag and the `Enum8`-only caveat; the backlog-wide precondition (drain with the old release, stop cleanly, switch producers, upgrade), labelled measured per record and inferred for the backlog.
- **Items 2 to 4.** Item 2: 25 of 47 fixture tables change, six shapes failed with code 32 or 33 and one stored a shifted row on gg3, all store the record on gg4, the residual WARN, the suspects query with both over-flags (`s4_o01`, `x08`) and both greps. Item 3: the Map-key exception of the gg3 note is gone on the JSON-as-string route with the accepted residual, the `insertFormat=json` route stated as not measured, `IllegalArgumentException` for an unknown Enum label with the lookup-miss text withheld, and the two greps. Item 4: per change and per direction from E-11, E-12, E-21, seven "no clean stop is needed" sentences, the corrected KeeperMap query with the `enableDbTopicSplit` key rule and the `exactlyOnce=false` case; no instruction to empty or edit the state table.
- **Items 5 and 6.** MBean `version=v1.7.0_gg4`, client name, and the user-agent HTTP client token `5.6.2` to `5.6.5`; `connect-api` 4.4.0 compile-only (0 Kafka entries in the archive), the production worker `confluentinc/cp-kafka-connect-base:7.9.7` (Kafka 3.9), Kafka 3.9 as the oldest verified worker by a static check, KIP-1161 from Kafka 4.2 workers with the seven LIST keys carrying no validator, Shadow 9.6.1 and Gradle 9.8.1, the archive compare result, the changed bundled versions and both advisories by source and read date with no exposure assessment.
- **Item 7 and E-30.** Tag `pre-v1.7.0_gg4` on `69cd680b`; the steps with gg4 values, the warning that the rolled-back tip builds a green archive with 25 nested jars, and the brought-back list with item links. The script ran on a dry-run `--no-ff` merge: `X = 48bf42d6946ac4a608384d51d7ec5af5a7676b04`, `D = 74cc816d469e121905dddff8765c7dbb430802f2`, `MERGE-DRY-RUN-OK`, `item-7 headings: 1`, `gg1 corrected rule after rollback: absent`, tree restored by `git revert`.

## Item titles and the evidence each rests on

| Item | Title | Evidence |
|------|-------|----------|
| 1 | Names and Enum labels as written in CREATE TABLE | E-20, E-21 (item 4 use), E-22, E-23; ledger L-58..L-64 |
| 2 | Tuple elements named size | E-10, E-11, E-13, E-14 |
| 3 | Writer error text | E-15; ledger L-52, L-54 |
| 4 | In-flight batches | E-11, E-12, E-21, E-25 |
| 5 | JMX MBean name and client name | E-02 |
| 6 | Dependencies, build and the Connect worker | E-03, E-06, E-07 |
| 7 | Rollback | E-04, E-30 |

D-09 labels by claim type: table forms, query results, retry outcomes, MBean, user agent, archive compare and advisory records are "measured" (server 26.9.19.9) or read from a named source with its date; the writer text, the token build, the state-table key rule, the residual WARN text and the LIST-key count are "read from connector code"; the backlog consequence of the rename, the bundled patch bumps and the `Enum16`/nested-enum reach of the label query are "inferred" or stated as not run.

## Task Commits

1. **Task 1 (tracer): frame, item 1, item 7 and the rollback proof** - `fae7e42a` (note), `48bf42d6` (gradlew.bat exception), `8e050273` (E-30 and verdicts)
2. **Task 2: items 2, 3 and 4** - `d68d7422`
3. **Task 3: items 5 and 6 and the whole-note checks** - `f61a81d3`, then `be420666` (one wording fix in item 1)

**Plan metadata:** the SUMMARY commit that follows this file.

## Decisions Made

- The note's `git status --porcelain` check in item 7 names ` M gradlew.bat` as expected, because the committed blob has CRLF endings and `.gitattributes` has `*.bat text eol=crlf`.
- The label query is quoted although the plan made it conditional: E-23 shows it lists exactly the nine differing enum tables with no over-flag.
- Verdicts: `pending (29-04, 29-05)` became `pending (29-05)` for E-03, E-04 and E-07; E-02, E-06, E-10..E-15, E-20..E-23 and E-25 read `yes`; E-08 and E-24 stay `pending (29-05)`.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 1 - Bug] The NOTE-FINAL-OK address scan flags the server version**
- **Found during:** Task 3 (final verify)
- **Issue:** the plan requires the note to contain the server version `26.9.19.9` (item checks 1, 2 and 4) and the final check scans the note for four dot-separated numbers as IP addresses, which `26.9.19.9` matches five times. The two requirements cannot both hold.
- **Fix:** the final check was run unchanged except that the one string `26.9.19.9` is masked before the address scan (`/tmp/gsd29-scripts/p04/cmd5-masked.sh`). Printed `truncate-bad [] secrets-or-addresses [] pending-29-04-left False` and `NOTE-FINAL-OK`. Plans 29-06 and 29-07 that re-run the same scan on the note need the same mask.
- **Files modified:** none (scratch script)
- **Commit:** none

**2. [Rule 1 - Bug] The "status prints nothing" check of the rollback step is false in a fresh checkout**
- **Found during:** Task 1 (first run of the rollback script on the dry-run merge)
- **Issue:** `git status --porcelain` lists ` M gradlew.bat` in a fresh worktree of `69cd680b` and of the fast-forwarded tip; the line also remains after the rollback commit. The gg3 note's wording ("prints nothing") would mislead operators.
- **Fix:** step 2.1 of item 7 names that one line as expected, with its cause; the script and dry run were repeated on the corrected note (first run: X `fae7e42a`, D `5ce5ad47`; recorded run: X `48bf42d6`, D `74cc816d`), and E-30 says why two lines of the Phase 24 output differ.
- **Files modified:** `docs/ROLLOUT-v1.7.0_gg4.md`, `29-COMMAND-EVIDENCE.md`
- **Verification:** ITEMS-1-7-OK; `git diff --quiet HEAD pre-v1.7.0_gg4` holds in the script
- **Commit:** `48bf42d6`, `8e050273`

### Planned-versus-done details

- **E-21 flipped in Task 2, not Task 1.** The plan's Task 1 lists E-21 among the verdicts to flip, but E-21 backs item 4, which Task 2 writes. Flipping it earlier would have claimed a match with text that did not exist yet.
- **Task 1 became three commits** (note, wording fix after the dry run, evidence), because X had to be a committed tip that contains the note, and the gradlew.bat finding changed the note after the first dry run. A fourth wording fix (`be420666`) followed Task 3.
- **Placeholder text in items 2 to 6** existed in commits `fae7e42a` to `8e050273` (a line saying the item is written in the next commit) and is gone at the tip; `grep -c "next commit"` prints 0.
- **Statements not written for lack of evidence:** a server code per shape (E-11 shows 32 or 33), the text of the `insertFormat=json` route's Jackson failure (not measured), inserts for names with a backspace, form feed, NUL or BEL (E-24 measured discovery only), an `Enum16`, `Nullable(Enum8)` or nested enum run of the label query, any exposure assessment for either advisory, and an older-than-3.9 worker.

**Total deviations:** 2 auto-fixed (2 Rule 1), 3 planned-detail changes.
**Impact on plan:** none on the content the plan asked for; both fixes make the note or the check agree with what was measured.

## Issues Encountered

None beyond the deviations. The dry-run worktrees and the `gg4-rollback-demo-*` branches were removed; `git worktree list` shows the main tree and the two probe worktrees `wt-gg3` and `wt-tip` kept from earlier plans. No Gradle build or test ran in this plan; no Gradle daemon was started.

## Known Stubs

None. No source file was created or modified, and the note has no placeholder text left.

## Threat Flags

None. The note quotes only read-only queries and greps (T-29-14); it names the gg3 tag as the artifact source and warns with the measured nested-jar count, and the rollback commands ran on a dry-run merge (T-29-15); every claim has a D-09 label and item 6 makes no exposure claim (T-29-16); the note holds placeholders only, and the scan found no secret or address once the server version was masked (T-29-17). Nothing was pushed, tagged on origin, merged or commented on.

## User Setup Required

None - no external service configuration required.

## Next Phase Readiness

- Plan 29-05 writes CHANGELOG and FORK-DELTA; it should carry the same wording of the rename precondition as item 1 and the first lines of the note (key link in the plan). Verdicts left for it: E-03, E-04, E-07, E-08, E-24 read `pending (29-05)`.
- Plan 29-06 reviews the note with fresh eyes; the plan's checks that scan the note for addresses need the server-version mask (deviation 1).
- Plans 29-07 and 29-08 re-run the E-30 script on the publication tip and on the real merge commit; the local tag `pre-v1.7.0_gg4` is pushed only after the merge's green run.
- Local state outside this plan's scope remains dirty and untouched: `gradlew.bat`, `.planning/state.json`, `.planning/milestone.lock`.

## Self-Check: PASSED

Files verified on disk: `docs/ROLLOUT-v1.7.0_gg4.md` (356 lines), `.planning/phases/29-release-publication/29-COMMAND-EVIDENCE.md` (E-30 present), `/tmp/gsd29/rollback-demo.sh`, `/tmp/gsd29/exec/rollback-dry.log`. Commits verified as ancestors of HEAD: `fae7e42a`, `48bf42d6`, `8e050273`, `d68d7422`, `f61a81d3`, `be420666`. Verify commands printed NOTE-FRAME-OK, ITEMS-1-7-OK, ITEMS-2-4-OK, ITEMS-5-6-OK and NOTE-FINAL-OK (with the mask of deviation 1).

---
*Phase: 29-release-publication*
*Completed: 2026-10-23*
