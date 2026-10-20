---
phase: quick-261020-vcr
plan: 01
type: execute
wave: 1
depends_on: []
files_modified:
  - docs/ROLLOUT-v1.7.0_gg3.md
  - CHANGELOG.md
  - docs/FORK-DELTA.md
  - .planning/phases/24-release-publication/24-REVIEW-DISPOSITION.md
  - .planning/phases/24-release-publication/24-MILESTONE-CLOSE-RECORD.md
autonomous: true
requirements: [ROLL-03]
findings: [WR-03, WR-04, WR-05, WR-06, WR-07, IN-04, IN-05]

estimate:
  tokens: 70000
  raw_tokens: 70000
  tasks: 3
  confidence: low

must_haves:
  truths:
    - "Item 7 of the branch copy of the rollout note has the operator work through item 4 (and keep the v1.7.0_gg3 archive) BEFORE the read-tree branch commit, and item 4 speaks of redeploying v1.7.0_gg2, not of rolling back (24-REVIEW WR-06); the rollback demo, run on a dry-run --no-ff merge of the new tip into 3376a18f, prints the item 7 order marker 'yes' and every E-22 line"
    - "Item 4 tells an exactlyOnce=false connector that writes an item 3 shape table that the retry failure applies to it too and that it must stop cleanly before v1.7.0_gg2 is redeployed (WR-05, E-12)"
    - "Item 4's state-table instructions cover any number of topics and name the database to run in, and the empty-result sentence speaks only of the listed topics; the SQL text is unchanged (WR-07, E-14)"
    - "Item 2, the CHANGELOG Known Issues bullet and FORK-DELTA Known costs item 10 state the size-element insert failure only for the four inserted shapes and keep the unmeasured wrong-value case as a live possibility, while item 2 keeps the D-02 phrases 'whatever the connector settings' and 'this release has no fix' (WR-04, E-15)"
    - "Item 3 describes the 30 identical tables by type content (no Nullable(Tuple(...)) at any depth) and its known-limitation lead sentence names the line break (WR-03 E-10, IN-04 E-16); the CHANGELOG Build and CI bullet names ClickHouseHelperClient (IN-05)"
    - "A one-paragraph notice near the top of the note links exactly the items whose text differs from the copy in tag v1.7.0_gg3, and no fenced sql or sh block of the note differs from 3376a18f"
    - "24-REVIEW-DISPOSITION.md reads open: 0 with each of the seven round 3 rows fixed and citing the commit that fixed it; nothing was pushed, custom_build, v1.7.0_gg3 (3376a18f) and pre-v1.7.0_gg3 (6473e24f) did not move, and no tag was created"
  artifacts:
    - path: docs/ROLLOUT-v1.7.0_gg3.md
      provides: "Corrected operator note: items 2, 3, 4 and 7 plus the revision notice"
      contains: "do not redeploy `v1.7.0_gg2` yet"
    - path: CHANGELOG.md
      provides: "1.7.0_gg3 Known Issues bullet (WR-04) and Build and CI bullet (IN-05); everything from the 1.7.0_gg2 heading down byte-equal to 07bf1e76"
      contains: "`ClickHouseHelperClient`"
    - path: docs/FORK-DELTA.md
      provides: "Known costs item 10 limited to the four inserted shapes (WR-04)"
      contains: "could store a wrong value"
    - path: .planning/phases/24-release-publication/24-REVIEW-DISPOSITION.md
      provides: "Seven round 3 rows flipped to fixed with their commit SHA; open: 0"
      contains: "open: 0"
    - path: .planning/phases/24-release-publication/24-MILESTONE-CLOSE-RECORD.md
      provides: "Milestone audit sentence accurate again (open: 0 after quick task 261020-vcr)"
      contains: "261020-vcr"
  key_links:
    - from: "docs/ROLLOUT-v1.7.0_gg3.md item 7 step 1"
      to: "#item-4-in-flight-batches"
      via: "markdown anchor link placed before the read-tree step"
      pattern: "item-4-in-flight-batches"
    - from: "docs/ROLLOUT-v1.7.0_gg3.md revision notice"
      to: "the changed item anchors"
      via: "one link per changed item"
      pattern: "tag `v1.7.0_gg3`"
    - from: "24-REVIEW-DISPOSITION.md rows WR-03..IN-05"
      to: "the fix commits on sync/v1.7.0"
      via: "8-char SHA at the start of each Source cell"
      pattern: "\\| fixed \\| [0-9a-f]{8} "
---

<objective>
Fix the seven open round 3 findings of `24-REVIEW.md` (WR-03, WR-04, WR-05, WR-06, WR-07, IN-04, IN-05) on the published `v1.7.0_gg3` rollout note, the CHANGELOG `1.7.0_gg3` section and FORK-DELTA Known costs item 10, as a docs-only follow-up on the local branch `sync/v1.7.0`. Prove the corrected text with the Phase 24 document checks, `consistency.py`, a rollback demo on a dry-run `--no-ff` merge of the new tip into `origin/custom_build` (`3376a18f`), the anchors gate and `testUnit`, then flip the seven disposition rows to `fixed`.

Purpose: three of the findings (WR-05, WR-06, WR-07) are procedures an operator could follow wrongly during a rollback. The milestone audit holds `tech_debt` until these seven rows are settled. At 24-UAT test 1 the user accepted them as a post-release docs-only follow-up on `custom_build`.

Output: about eight docs commits on `sync/v1.7.0` (one per finding plus one for the revision notice) and one `.planning` bookkeeping commit. Recorded check outputs go to the SUMMARY.

The executor's work ends after Task 3. The section "Orchestrator: review and publication (not executed by the executor)" belongs to the orchestrator. The executor skips it entirely: it does not push, does not touch `custom_build`, creates or moves no tag, and commits no SUMMARY.md, STATE.md or PLAN.md.

Fix-text authority: each fix uses the **Fix:** block of its finding in `24-REVIEW.md` "Round 3". The cited evidence was re-read at planning time, and none of it contradicts a fix: E-10 lists exactly r01/r04/r06/r08/r11 as identical roots (WR-03); E-12 ran `exactlyOnce=false` and failed with code 33 and rows=1 (WR-05); E-14 proved the query with both `<topic>` slots set to one topic, run in the state table's database (WR-07); E-15 inserted exactly `s4_a01`/`a04`/`a08`/`a13`, the other nine were found by the WARN oracle only, and the wrong-value case was not measured (WR-04); E-16 has the line-break row `e5_e03` (IN-04); E-22 proves the git commands of item 7 but not their order (WR-06). Three project conventions narrow the literal Fix text, and each is enforced by an existing check:
- The note carries no evidence or finding IDs, so WR-05's "(E-12 ran with `exactlyOnce=false`)" becomes prose. The checks `ROLLOUT-FRAME-OK` and `ROLLOUT-T3-OK` reject bare IDs.
- No fenced `sql` or `sh` block changes, so WR-07 changes prose only. Every block must stay byte-equal to its evidence entry, which `DOCS-FROZEN-OK` checks.
- FORK-DELTA phrases the unmeasured cases in the has/have-not-been form, because `FORK-DELTA-ROWS-OK` rejects the was-form anywhere in that file.
</objective>

<execution_context>
@~/.claude/gsd-core/workflows/execute-plan.md
@~/.claude/gsd-core/templates/summary.md
</execution_context>

<context>
@.planning/STATE.md
@AGENTS.md
@.planning/phases/24-release-publication/24-REVIEW.md
@.planning/phases/24-release-publication/24-REVIEW-DISPOSITION.md
@.planning/phases/24-release-publication/24-MILESTONE-CLOSE-RECORD.md
@docs/ROLLOUT-v1.7.0_gg3.md

Read on demand (by section, not whole): `.planning/phases/24-release-publication/24-COMMAND-EVIDENCE.md` E-10 (line ~257), E-12 (~341), E-14 (~418), E-15 (~441), E-16 (~511), E-22 (~634); `CHANGELOG.md` lines 1-60 (the `1.7.0_gg3` section only); `docs/FORK-DELTA.md` the `**10. ` paragraph under "## Known costs and open items".

Planning-time observations (live, 2026-10-20, HEAD `f4573ce6`). These are the baselines the gates below were grounded on:
- `origin/custom_build` = local `custom_build` = `3376a18fd057dba329ceae7d8c74d1ba4e3d36f6`. `origin` holds `pre-v1.7.0_gg3` at `6473e24f372a8b37dfd8cc8cf3124e6cff52edca` and `v1.7.0_gg3` at `3376a18f`, and has no `sync/v1.7.0` head. There are 77 local tags. `git diff --quiet 3376a18f 8b1c318d` holds, so a merge of the tip into `3376a18f` has the tip's tree. `git diff 3376a18f HEAD -- . ':(exclude).planning'` is empty.
- `.planning/v1.7.0_gg3-hardening-MILESTONE-AUDIT.md` is untracked. Do not stage, edit or delete it. Stage files by exact path only, and never with `git add -A` or `git add .`.
- Phase 24 check scripts at HEAD:
  - Pass: `v03-0` ROLLOUT-FRAME-OK, `v03-2` ITEMS-2-3-OK, `v03-3` ROLLOUT-T3-OK, `v03-4` ROLLOUT-FINAL-OK, `v04-0` CHANGELOG-GG3-OK, `v04-1` FORK-DELTA-ROWS-OK, `v04-2` FORK-DELTA-PATHS-OK, `v04-3` RAW-NUMBERS-GG3-OK (runs `consistency.py`, 0 failures), and `/tmp/gsd24-scripts/v05-5.sh` DOCS-FROZEN-OK.
  - `v03-1` fails by design after the publication: its tail asserts that origin does NOT list `pre-v1.7.0_gg3`. The post-publication copy built in Task 1 replaces that one clause and prints ITEMS-1-7-OK at HEAD.
  - `v05-3` and `v05-4` are stale (the round 2 and round 3 headings broke their section assumptions), so they are not reused. The disposition gate below replaces `v05-4`.
- Findings gate (section "Gate programs") at HEAD: every finding check and NOTICE is False (exit 1), and BLOCKS is True. The item 7 order awk prints `no` at HEAD (item-4 link at line 320, read-tree at line 317).
- Dry-run merge plus rollback demo on HEAD (D' into `3376a18f`): MERGE-DRY-RUN-OK and every E-22 line. A synthetic, never-pushed WR-06 commit in a throwaway worktree printed `item-7 order ... yes` and ROLLBACK-ON-DRY-MERGE-OK, and left no worktree or branch behind.
- Anchors gate `bash .planning/scripts/verify-merge-invariants.sh 54e63648 HEAD`: `invariants: provenance_paths=21 anchor_commits=529 anchor_rows=69 marker_commits=576 anchor_exempt=1 result=PASS` in 8 min 57 s. Run it in the background.
- `testUnit` baseline (E-60, same `src` tree: `git diff --quiet 3ab55e1d HEAD -- src build.gradle.kts gradle settings.gradle.kts` holds): 49 classes, 838 tests, 2 skipped, 0 failures, 0 errors.
- Destructive-statement and secret scan over the note, the CHANGELOG `1.7.0_gg3` section and FORK-DELTA: 0 hits at HEAD (regression guard).
- `.planning/config.json`: `workflow.human_verify_mode` is `end-of-phase` and `git.create_tag` is `true`. This task creates no tag, whatever the config says.
- Commit subjects must contain `261020-vcr`, because the quick workflow's review scope resolves the task's files from the commits that name the quick id.
</context>

<tasks>

<task type="tracer">
  <name>Task 1: Tracer, WR-06 end to end: reorder item 7, commit it, and prove it with the doc checks and the rollback demo on a dry-run merge into 3376a18f</name>
  <precondition>The current branch is sync/v1.7.0 with nothing staged and no change under docs/ or CHANGELOG.md. After `git fetch --quiet origin --no-tags`, origin/custom_build is 3376a18fd057dba329ceae7d8c74d1ba4e3d36f6. These scripts exist: /tmp/gsd24/rollback-demo.sh, /tmp/gsd24/exec/v03-0.sh..v03-4.sh, /tmp/gsd24/exec/v04-0.sh..v04-3.sh, /tmp/gsd24-scripts/consistency.py, /tmp/gsd24-scripts/v05-5.sh. If any of these does not hold, halt and report.</precondition>
  <reversibility rating="reversible">Local docs commit on sync/v1.7.0 and scratch files under /tmp/gsd-vcr. Nothing is pushed.</reversibility>
  <files>docs/ROLLOUT-v1.7.0_gg3.md</files>
  <read_first>
    - .planning/phases/24-release-publication/24-REVIEW.md, the "### WR-06:" block (its Issue quotes both sentences; its Fix gives the new step order) and the "### WR-07:" block (it touches the same item 4 paragraph in Task 2)
    - docs/ROLLOUT-v1.7.0_gg3.md, "## Item 4: In-flight batches" "### What to do", and all of "## Item 7: Rollback"
    - .planning/phases/24-release-publication/24-COMMAND-EVIDENCE.md E-22 (the rollback script and what it asserts)
    - this plan's section "Gate programs" (all five programs)
  </read_first>
  <action>
1. **Scratch harness.** Create `/tmp/gsd-vcr/` and write the five programs of the section "Gate programs" with the Write tool: `findings-gate.py`, `make-v03-1-post.py`, `dry-run-demo.sh`, `disposition-gate.py`, and the one-line order assertion that goes into the rollback demo copy.
   - Run `python3 -I /tmp/gsd-vcr/make-v03-1-post.py`. It must print `occurrences 1` and write `/tmp/gsd-vcr/v03-1-post.sh`.
   - Copy `/tmp/gsd24/rollback-demo.sh` to `/tmp/gsd-vcr/rollback-demo.sh`. With the Edit tool, insert the order-assertion line directly after the line that prints `item-7 headings`. Then `diff /tmp/gsd24/rollback-demo.sh /tmp/gsd-vcr/rollback-demo.sh` must show exactly one added line and no removed line.
   - Never edit the originals under `/tmp/gsd24` or `/tmp/gsd24-scripts`.
   - Record the baseline: `python3 -I /tmp/gsd-vcr/findings-gate.py` exits 1 with every finding False and BLOCKS True, and `bash /tmp/gsd-vcr/v03-1-post.sh` prints ITEMS-1-7-OK.
2. **WR-06 edit** in `docs/ROLLOUT-v1.7.0_gg3.md`, per the WR-06 Fix. Locate each sentence by its text; line numbers are hints only.
   - **Item 7 "What to do", new step 1:** before any branch change, work through [item 4](#item-4-in-flight-batches) for each connector it applies to. Word this step so that it fits both `exactlyOnce` settings, because Task 2's WR-05 adds the `exactlyOnce=false` instruction to item 4. Keep the `v1.7.0_gg3` archive you deployed, or rebuild it from tag `v1.7.0_gg3` (check out the tag and run `./gradlew createConfluentArchive`, as step 3 does for `v1.7.0_gg2`), because item 4 may need you to start it again. Keep the reason from the old step 2: a rollback is the one direction in which a batch can be stuck.
   - **Step 2** is the current "Back out the whole release in one commit on `custom_build`" step. Its two sub-steps and the indented "Use this form rather than `git revert -m 1`" paragraph stay unchanged.
   - **Step 3** is the current redeploy step, unchanged.
   - **Remove** the old "Before you redeploy, run the check of item 4" step, because its content is now in step 1.
   - **Item 4 "What to do":** the step 2 "Non-empty result" clause that tells the operator to wait now reads "do not redeploy `v1.7.0_gg2` yet". The step 2 "Empty result" sentence ends "so it is safe to redeploy `v1.7.0_gg2`" (Task 2's WR-07 then edits its topic wording). Step 3's closing instruction no longer says to roll back; it says to continue with step 2 of [item 7](#item-7-rollback).
   - **Constraints:**
     - Every git command keeps its text exactly. The commands are inline code in prose, and the item 7 checks look for `read-tree -u --reset pre-v1.7.0_gg3`, `diff --quiet HEAD pre-v1.7.0_gg3`, `revert <rollback-commit>`, `show <rollback-commit>^:docs/ROLLOUT-v1.7.0_gg3.md`, `status --porcelain`, `createConfluentArchive` and `#item-4-in-flight-batches`.
     - No list line may start with a link: a line such as `1. [` would be read as a table-of-contents entry.
     - No evidence, finding, decision or phase identifier may enter the note.
     - No fenced block changes.
3. **Commit.** `git add docs/ROLLOUT-v1.7.0_gg3.md` and nothing else, then commit with the subject `docs(quick-261020-vcr): WR-06 work through item 4 before the item 7 branch rollback`. The body is two or three bullets that name the 24-REVIEW round 3 WR-06 finding and its fix, ending with the line `Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>`. Do not push.
4. **Prove it end to end** with the verify chain below.
   - `dry-run-demo.sh` builds D' as a `--no-ff` merge of HEAD into `3376a18f` in a throwaway detached worktree. It checks D'^1 = `3376a18f`, D'^2 = HEAD and tree(D') = tree(HEAD), runs the adapted demo against D', checks the demo log (the E-22 lines plus `item-7 order, item-4 check before read-tree: yes`), scans D''s operator docs for a TRUNCATE instruction, and removes both worktrees and any `gg3-rollback-demo-*` branch on exit. D' is never pushed.
   - If `dry-run-demo.sh` stops with "origin/custom_build is no longer ...", someone moved the base (for example a Dependabot merge on origin). Halt and report; do not adapt the base.
   - Record every marker and exit code for the SUMMARY.
  </action>
  <verify>
    <automated>python3 -I /tmp/gsd-vcr/findings-gate.py WR-06 BLOCKS && bash /tmp/gsd24/exec/v03-0.sh && bash /tmp/gsd-vcr/v03-1-post.sh && bash /tmp/gsd24/exec/v03-2.sh && bash /tmp/gsd24/exec/v03-3.sh && bash /tmp/gsd24/exec/v03-4.sh && bash /tmp/gsd24-scripts/v05-5.sh && python3 -I /tmp/gsd24-scripts/consistency.py && bash /tmp/gsd-vcr/dry-run-demo.sh && W=$(git worktree list) && test -z "$(printf '%s\n' "$W" | grep -F /tmp/gsd-vcr/)" && test -z "$(git branch --list 'gg3-rollback-demo-*')" && S1=$(git log -1 --format=%s) && printf '%s\n' "$S1" | grep -q '261020-vcr' && test -z "$(git status --porcelain -- docs CHANGELOG.md)" && echo TRACER-OK</automated>
  </verify>
  <done>One commit on sync/v1.7.0 changes only docs/ROLLOUT-v1.7.0_gg3.md. The findings gate passes WR-06 and BLOCKS. ROLLOUT-FRAME-OK, ITEMS-1-7-OK (post-publication copy), ITEMS-2-3-OK, ROLLOUT-T3-OK, ROLLOUT-FINAL-OK, DOCS-FROZEN-OK, `consistency failures: 0`, MERGE-DRY-RUN-OK and ROLLBACK-ON-DRY-MERGE-OK are printed, the demo log has `item-7 order, item-4 check before read-tree: yes`, and no worktree or demo branch is left.</done>
</task>

<task type="auto">
  <name>Task 2: The other six fixes and the revision notice, one commit each, then the full pre-publication gate on the final docs tip</name>
  <precondition>Task 1 printed TRACER-OK, and the current branch is sync/v1.7.0.</precondition>
  <reversibility rating="reversible">Local docs commits on sync/v1.7.0. The released CHANGELOG text changes only on the branch; tag v1.7.0_gg3 keeps the old text.</reversibility>
  <files>docs/ROLLOUT-v1.7.0_gg3.md, CHANGELOG.md, docs/FORK-DELTA.md</files>
  <read_first>
    - .planning/phases/24-release-publication/24-REVIEW.md, blocks "### WR-03:", "### WR-04:", "### WR-05:", "### WR-07:", "### IN-04:" and "### IN-05:" (each Issue quotes the sentence to replace; each Fix gives the new text), and "## Round 2" (why the D-02 phrase "whatever the connector settings" stays)
    - docs/ROLLOUT-v1.7.0_gg3.md, items 2, 3 and 4, and the "Rolling back brings back" list of item 7
    - CHANGELOG.md lines 1-60; docs/FORK-DELTA.md, the `**10. ` paragraph
    - .planning/phases/24-release-publication/24-COMMAND-EVIDENCE.md E-12, E-14, E-15 and E-16 (the measurements each fix must agree with)
  </read_first>
  <action>
Apply the fixes in this order, one commit each. Stage only the files named for each fix. Commit subjects follow the pattern `docs(quick-261020-vcr): <ID> <summary>`, the body names the 24-REVIEW round 3 finding, and the message ends with the line `Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>`. After each commit, run the fast checks (`bash /tmp/gsd24/exec/v03-0.sh`, `bash /tmp/gsd24/exec/v03-3.sh`, `bash /tmp/gsd24/exec/v03-4.sh`, `bash /tmp/gsd24/exec/v04-0.sh`, `bash /tmp/gsd24/exec/v04-1.sh`, `python3 -I /tmp/gsd24-scripts/consistency.py`, and `python3 -I /tmp/gsd-vcr/findings-gate.py <ID> BLOCKS`), and fix before moving on. If a fast check fails right after a commit, correct the text and `git commit --amend --no-edit` that same local, unpushed commit, so that each finding stays exactly one commit whose files match its disposition row (the Task 3 gate checks this). Locate every target by its text, using the sentence the finding's Issue quotes; line numbers are hints only.

1. **WR-03** (note only, item 3 "What changed"). In the sentence that begins "The other 30, among them", replace the clause about roots with the Fix wording "every table whose type holds no `Nullable(Tuple(...))` at any depth". Keep the rest of the sentence. (E-10: the identical roots are r01, r04, r06, r08 and r11, none of which holds a `Nullable(Tuple)`.)
2. **IN-04** (note only, item 3, the "A known limitation" paragraph). The lead sentence that ends "therefore means the Kafka field must use the connector's escaped form as its name" lists the characters in the order of the Fix: "A backslash, a line break, a slash or a tab in a column or element name". (E-16, row `e5_e03`.)
3. **WR-04** (one commit over `docs/ROLLOUT-v1.7.0_gg3.md`, `CHANGELOG.md` and `docs/FORK-DELTA.md`). In all three places, follow the Fix: say what was inserted, and keep the wrong-value case open.
   - **Fix sentence:** "Inserts that were run into such tables (four shapes: `Tuple(size ...)`, `Array(Tuple(size ...))`, `Array(Tuple(size1 ...))` and `Array(Tuple(v String, size1 ...))`) failed with the server error code 32 or 33 and stored no row. The other affected shapes were detected by the WARN and were not inserted, and a shorter row that happens to parse could store a wrong value instead of failing; neither was measured."
   - **Note, item 2 "What changed":** in the paragraph that starts "**What the WARN marks", replace the clause that states the insert outcome for every table with the Fix sentence. Keep both code names, `ATTEMPT_TO_READ_AFTER_EOF` and `CANNOT_READ_ALL_DATA`, keep the server version line and the bullet list of measured codes, and keep `size0`, 32 and 33 in that one paragraph. In the paragraph after the bullets, drop the "A wrongly stored value is possible in principle" sentence, because the Fix sentence now says it. Keep the `v1.7.0_gg2` sentence and the code 36 sentence.
   - **Same claim elsewhere in the note** (the round 3 finding's "all three places" intent; record each in the SUMMARY):
     - Item 2's log-grep "**Non-empty result:**" clause, which ends by saying inserts into the named table fail: it now says the table has the defect described above.
     - Item 2's "What to do" sentence that states the failure of every insert: it now says the inserts that were run failed on the route that was measured, the default route with client V2. Keep "this release has no fix" and the D-02 phrase "whatever the connector settings" verbatim; both are checked.
     - Item 7's "Rolling back brings back" bullet for the `size`/`size0` table: the inserts behave as before (the shapes that were inserted failed with code 32 or 33). Keep `size0`, 32 and 33 in that bullet.
   - **CHANGELOG Known Issues bullet:** the second sentence becomes the Fix sentence, prefixed "On the default route with client V2," and keeping "on `v1.7.0_gg2` and on this release alike". Keep `size`, `size0`, 32, 33 and `Array`. Re-wrap to at most 120 characters per line with two-space continuation indent, write no planning identifier, and leave everything from `# 1.7.0_gg2, 2026-10-14` down byte-equal (`CHANGELOG-GG3-OK` compares it with `07bf1e76`).
   - **FORK-DELTA Known costs item 10:** replace the clause that states the failure of every insert with the four inserted shapes and their codes (32 `ATTEMPT_TO_READ_AFTER_EOF`, 33 `CANNOT_READ_ALL_DATA`), and say that the other nine affected shapes were detected by the WARN and not inserted. Fold the existing wrong-value sentence into "a shorter row that happens to parse could store a wrong value instead of failing". Keep "client V1 and the route of an explicit `insertFormat=json` ... have not been measured" and the `24-COMMAND-EVIDENCE.md` E-15 pointer. Use the has/have-not-been form for every unmeasured case, because `FORK-DELTA-ROWS-OK` rejects the was-form anywhere in the file. Do not touch the `shouldSkipDescribeRow` row or the declared-nullability row, whose short parentheticals also mention codes 32 and 33. They are outside this task's FORK-DELTA scope (item 10 only); list them in the SUMMARY under "Residuals for the round 4 review".
   - **Commit body:** add the line `Reason for changing released CHANGELOG content: 24-REVIEW round 3 WR-04 (ROADMAP Structure Notes rule).`
4. **WR-05** (note only, item 4).
   - **"Am I affected?":** the opening sentence no longer limits the hazard to `exactlyOnce=true`. Replace the sentence that begins "A connector with `exactlyOnce=false`" with the Fix text, minus the evidence ID. The connector has no state table to query, so the check below does not exist for it, but the failure does: a range that Kafka delivers again to `v1.7.0_gg2` for a table of the item 3 shape fails with code 33 even when `v1.7.0_gg3` stored its rows. The measurement in the Rollback bullet above ran with `exactlyOnce=false`, which still sends the token. Stop its tasks cleanly, so that the offsets are committed, before you redeploy.
   - **"What to do":** keep the `exactlyOnce=true` procedure intro and its three steps, and add one sentence or short paragraph after step 3 for a connector with `exactlyOnce=false` that writes such a table: stop its tasks cleanly before you redeploy `v1.7.0_gg2`; there is no state table to query.
   - **Link:** the later paragraph that links item 1 of the gg2 note stays, because `ROLLOUT-FINAL-OK` needs that anchor in item 4.
5. **WR-07** (note only, item 4 step 2). Change the prose only; the SQL block stays byte-equal to E-14.
   - The instruction above the query becomes: replace the `'<topic>'` entries with one entry for each topic the connector consumes, adding an entry per further topic and writing a single topic in both entries. Run the query in the database the connector connects to (its `database` setting, where the connector creates the state table), or write the table as `` `<db>`.`<zkDatabase>` ``.
   - The "**Empty result:**" sentence speaks of the listed topics: "no batch of the listed topics is in flight, so it is safe to redeploy `v1.7.0_gg2`".
   - Do not edit `docs/ROLLOUT-v1.7.0_gg2.md`, which has the same inherited gap: the gg1 and gg2 notes are protected files (`ROLLOUT-FINAL-OK`). Name it in the SUMMARY residuals.
6. **IN-05** (`CHANGELOG.md` only). The last "Build and CI" bullet becomes the Fix text: "The writer error-path tests no longer inline-mock the connector's `ClickHouseHelperClient`; they use a subclass." Keep it within 120 characters per line. Add the commit-body line `Reason for changing released CHANGELOG content: 24-REVIEW round 3 IN-05 (ROADMAP Structure Notes rule).`
7. **Revision notice** (note only, its own commit `docs(quick-261020-vcr): note which items this copy corrects`).
   - **Where:** one paragraph directly under the `#` title, before the paragraph that points to the gg2 note. The gg2 link must stay within the first 15 lines.
   - **Content:** say that this copy corrects the text of the copy in tag `v1.7.0_gg3`, link each changed item with its exact anchor, say that no query or command changed and that item 7 now has the reader work through item 4 before changing the branch, and say to read those items here if you hold the tagged copy.
   - **Rules:** it must be the only paragraph in the first 20 lines that contains "tag `v1.7.0_gg3`". It must link exactly the items whose text differs from `3376a18f`; the NOTICE check computes that set, and it is items 2, 3, 4 and 7 if the steps above were followed. It carries no identifier (no WR, E, D or phase IDs) and no date.
8. **Final gate on the docs tip.**
   - Write `git rev-parse HEAD` to `/tmp/gsd-vcr/gate-tip`.
   - Start the anchors gate in the background (about 9 minutes; do not use `time` around it): `git rev-parse HEAD > /tmp/gsd-vcr/anchors.tip && bash .planning/scripts/verify-merge-invariants.sh 54e63648 HEAD > /tmp/gsd-vcr/anchors.log 2>&1`.
   - Meanwhile run `testUnit` in the foreground with a 10-minute timeout: `rm -rf build/test-results/testUnit && git rev-parse HEAD > /tmp/gsd-vcr/testUnit.tip && env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew testUnit --rerun > /tmp/gsd-vcr/testUnit.log 2>&1`. This is the brief's command plus `--rerun`, so that Gradle does not answer UP-TO-DATE on a docs-only change.
   - If `testUnit` exits 255, suspect the intermittent OpenJ9 GPF in Mockito inline mocking. List and delete the core dumps in the repository root (`core*`, `javacore*`, `Snap*`, `jitdump*`, up to about 8 GB), then re-run once. A second failure stops the task and is reported.
   - When the anchors gate finishes, run the verify chain, and record each marker, exit code, the gate tip T, D', the anchors result line and the `testUnit` counts for the SUMMARY.
  </action>
  <verify>
    <automated>python3 -I /tmp/gsd-vcr/findings-gate.py && bash /tmp/gsd24/exec/v03-0.sh && bash /tmp/gsd-vcr/v03-1-post.sh && bash /tmp/gsd24/exec/v03-2.sh && bash /tmp/gsd24/exec/v03-3.sh && bash /tmp/gsd24/exec/v03-4.sh && bash /tmp/gsd24/exec/v04-0.sh && bash /tmp/gsd24/exec/v04-1.sh && bash /tmp/gsd24/exec/v04-2.sh && bash /tmp/gsd24/exec/v04-3.sh && bash /tmp/gsd24-scripts/v05-5.sh && N=$(git diff --name-only 3376a18fd057dba329ceae7d8c74d1ba4e3d36f6 HEAD -- . ':(exclude).planning') && test "$(printf '%s\n' "$N" | tr '\n' ' ')" = "CHANGELOG.md docs/FORK-DELTA.md docs/ROLLOUT-v1.7.0_gg3.md " && git diff --quiet 3376a18fd057dba329ceae7d8c74d1ba4e3d36f6 HEAD -- src build.gradle.kts settings.gradle.kts gradle VERSION .github docs/ROLLOUT-v1.7.0_gg1.md docs/ROLLOUT-v1.7.0_gg2.md && python3 -I -c 'import re;c=open("CHANGELOG.md",encoding="utf-8").read();c=c[:c.find("# 1.7.0_gg2, 2026-10-14")];D={"note":open("docs/ROLLOUT-v1.7.0_gg3.md",encoding="utf-8").read(),"changelog-gg3":c,"fork-delta":open("docs/FORK-DELTA.md",encoding="utf-8").read()};P=re.compile(r"(?i)\b(?:truncate\s+table|delete\s+from|drop\s+(?:table|database)|alter\s+table)\b");S=re.compile(r"-----BEGIN [A-Z ]*PRIVATE KEY-----|AKIA[0-9A-Z]{16}|gh[pousr]_[A-Za-z0-9]{30,}|(?i:password)\s*[=:]\s*\S+");bad=[(k,n) for k,t in D.items() for n,l in enumerate(t.split("\n"),1) if P.search(l) or S.search(l)];print("destructive-or-secret",bad);raise SystemExit(1 if bad else 0)' && test "$(cat /tmp/gsd-vcr/gate-tip)" = "$(git rev-parse HEAD)" && bash /tmp/gsd-vcr/dry-run-demo.sh && W=$(git worktree list) && test -z "$(printf '%s\n' "$W" | grep -F /tmp/gsd-vcr/)" && test "$(cat /tmp/gsd-vcr/anchors.tip)" = "$(git rev-parse HEAD)" && tail -n 1 /tmp/gsd-vcr/anchors.log | grep -q 'result=PASS$' && test "$(cat /tmp/gsd-vcr/testUnit.tip)" = "$(git rev-parse HEAD)" && grep -q 'BUILD SUCCESSFUL' /tmp/gsd-vcr/testUnit.log && python3 -I -c 'import glob,xml.etree.ElementTree as E;R=[E.parse(p).getroot() for p in glob.glob("build/test-results/testUnit/TEST-*.xml")];S=lambda k:sum(int(r.get(k)) for r in R);print("classes",len(R),"tests",S("tests"),"skipped",S("skipped"),"failures",S("failures"),"errors",S("errors"));raise SystemExit(0 if R and S("tests")==838 and S("skipped")==2 and S("failures")==0 and S("errors")==0 else 1)' && L8=$(git log --format=%s -n 8 HEAD) && test -z "$(printf '%s\n' "$L8" | grep -v '261020-vcr')" && echo PRE-PUBLICATION-GATE-OK</automated>
  </verify>
  <done>
    - Seven more docs commits are on `sync/v1.7.0`: WR-03, IN-04, WR-04, WR-05, WR-07, IN-05 and the notice. Each subject names `261020-vcr`, and the WR-04 and IN-05 commit bodies record the released-CHANGELOG reason.
    - The findings gate passes every check (WR-03..IN-05, NOTICE, BLOCKS).
    - All nine Phase 24 document checks, the post-publication ITEMS-1-7-OK and DOCS-FROZEN-OK print their markers.
    - Only the three docs differ from `3376a18f` outside `.planning`, and the destructive-statement and secret scan is empty.
    - ROLLBACK-ON-DRY-MERGE-OK holds on D' built from the final docs tip T, the anchors gate on T ends `result=PASS`, and `testUnit` on T reports 838 tests, 2 skipped, 0 failures and 0 errors.
    - PRE-PUBLICATION-GATE-OK is printed.
  </done>
</task>

<task type="auto">
  <name>Task 3: Flip the seven round 3 rows to fixed with their commit SHAs, correct the close record's audit sentence, and prove nothing was published</name>
  <precondition>Task 2 printed PRE-PUBLICATION-GATE-OK, and /tmp/gsd-vcr/gate-tip holds the final docs tip T.</precondition>
  <reversibility rating="reversible">A local .planning commit on sync/v1.7.0.</reversibility>
  <files>.planning/phases/24-release-publication/24-REVIEW-DISPOSITION.md, .planning/phases/24-release-publication/24-MILESTONE-CLOSE-RECORD.md</files>
  <read_first>
    - .planning/phases/24-release-publication/24-REVIEW-DISPOSITION.md (frontmatter `findings:` list, `open:`, and the table `| Finding | Severity | Disposition | Source |`; the rules footer says hand edits are preserved)
    - .planning/phases/24-release-publication/24-MILESTONE-CLOSE-RECORD.md section "## Milestone audit"
    - this plan's section "Gate programs", program `disposition-gate.py`
  </read_first>
  <action>
1. Collect the eight-character SHA of each fix commit: `git log --format='%h %s' --abbrev=8 f4573ce6f509b14ab7453d2cadbfe5ec92ab0232..HEAD`, matching each finding ID in the subjects. The notice commit is not a finding and gets no row.
2. **`24-REVIEW-DISPOSITION.md`** (Edit tool):
   - In the frontmatter, change `disposition: open` to `disposition: fixed` for exactly WR-03, WR-04, WR-05, WR-06, WR-07, IN-04 and IN-05, and change `open: 7` to `open: 0`. Leave `total: 12`, `recorded:`, the titles and the five older rows untouched.
   - Each of the seven table rows becomes `| <ID> | <severity> | fixed | <sha8> (quick 261020-vcr; <one-line what changed, with the evidence entry it rests on>) |`.
     - WR-04 and IN-05 add "released CHANGELOG text changed for this finding".
     - WR-06 adds that the order was proven by the rollback demo on a dry-run merge.
     - WR-07 adds that the query text is unchanged (E-14).
   - Keep the table shape, put no `|` inside a Source cell, and keep the footer.
3. **`24-MILESTONE-CLOSE-RECORD.md`, "## Milestone audit":** keep the sentence that says all four disposition files read `open: 0`, and append a factual parenthesis: `24-REVIEW-DISPOSITION.md` read `open: 7` after the round 3 review until quick task 261020-vcr fixed those seven findings on `sync/v1.7.0`. Do not claim publication; that happens later and is the orchestrator's. Change nothing else in the file.
4. Commit both files together, `git add` by exact path only, with the subject `docs(quick-261020-vcr): record the round 3 fixes in the Phase 24 review disposition`. The body lists the seven ID-to-SHA pairs and ends with the line `Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>`.
5. Run the verify chain. Then write the SUMMARY (not committed) with:
   - every commit and its finding;
   - every marker and exit code from Tasks 1-3;
   - T, D', the anchors result line, and the `testUnit` counts;
   - the residuals list (the two FORK-DELTA row parentheticals, and the gg2 note's inherited state-query gap);
   - the evidence-versus-Fix narrowings named in the objective;
   - a pointer that the orchestrator's round 4 review and publication come next.
  </action>
  <verify>
    <automated>python3 -I /tmp/gsd-vcr/disposition-gate.py && N=$(git diff --name-only "$(cat /tmp/gsd-vcr/gate-tip)" HEAD) && test "$(printf '%s\n' "$N" | tr '\n' ' ')" = ".planning/phases/24-release-publication/24-MILESTONE-CLOSE-RECORD.md .planning/phases/24-release-publication/24-REVIEW-DISPOSITION.md " && test -z "$(git status --porcelain -- docs CHANGELOG.md .planning/phases/24-release-publication)" && git fetch --quiet origin --no-tags && test "$(git rev-parse --abbrev-ref HEAD)" = sync/v1.7.0 && CB=$(git ls-remote --heads origin custom_build) && test "${CB%%[[:space:]]*}" = 3376a18fd057dba329ceae7d8c74d1ba4e3d36f6 && test -z "$(git ls-remote --heads origin sync/v1.7.0)" && R=$(git ls-remote --tags origin) && printf '%s\n' "$R" | grep -qxF "$(printf '6473e24f372a8b37dfd8cc8cf3124e6cff52edca\trefs/tags/pre-v1.7.0_gg3')" && printf '%s\n' "$R" | grep -qxF "$(printf '3376a18fd057dba329ceae7d8c74d1ba4e3d36f6\trefs/tags/v1.7.0_gg3')" && test "$(git rev-parse custom_build)" = 3376a18fd057dba329ceae7d8c74d1ba4e3d36f6 && test "$(git rev-parse 'v1.7.0_gg3^{commit}')" = 3376a18fd057dba329ceae7d8c74d1ba4e3d36f6 && test "$(git rev-parse 'pre-v1.7.0_gg3^{commit}')" = 6473e24f372a8b37dfd8cc8cf3124e6cff52edca && TL=$(git tag -l) && test "$(printf '%s\n' "$TL" | wc -l | tr -d ' ')" = 77 && RA=$(git ls-remote origin) && HH=$(git rev-parse HEAD) && test -z "$(printf '%s\n' "$RA" | grep -F "$HH")" && echo NO-PUBLISH-OK && echo BOOKKEEPING-OK</automated>
  </verify>
  <done>
    - The disposition file reads `open: 0`, and each of the seven rows is `fixed` with the SHA of a commit that touches exactly the files of its finding (WR-04: the three docs; IN-05: CHANGELOG.md; the rest: the note).
    - The five older rows are unchanged, and all four Phase 21-24 disposition files read `open: 0`.
    - The close record's audit sentence names 261020-vcr.
    - Since T, only those two `.planning` files changed.
    - origin/custom_build, origin's tags and local `custom_build`, `v1.7.0_gg3`, `pre-v1.7.0_gg3` and the tag count (77) are unchanged, and no origin ref points at HEAD.
    - BOOKKEEPING-OK is printed.
  </done>
</task>

</tasks>

## Gate programs (scratch, written by Task 1 into /tmp/gsd-vcr/ with the Write tool)

These programs were grounded at planning time against HEAD `f4573ce6`, which is where the baselines in `<context>` come from. Write them as shown. They read the working tree from the repository root.

**`/tmp/gsd-vcr/findings-gate.py`** runs one check per finding plus NOTICE and BLOCKS. With no argument it runs all of them; arguments select checks. Negative markers are the old sentences, and positive markers are fragments of the Fix text.

```python
import re
import subprocess
import sys

BASE = "3376a18fd057dba329ceae7d8c74d1ba4e3d36f6"
NOTE = "docs/ROLLOUT-v1.7.0_gg3.md"


def git(*a):
    return subprocess.run(["git", *a], capture_output=True, text=True, check=True).stdout


def norm(x):
    return re.sub(r"\s+", " ", x)


def has(text, marker):
    return marker in norm(text)


def sec(text, a, b):
    i = text.find(a)
    if i < 0:
        return ""
    j = text.find(b, i + 1)
    return text[i : j if j > 0 else len(text)]


def items(text):
    return {
        m.group(1): m.group(2)
        for m in re.finditer(r"^## Item (\d):(.*?)(?=^## Item \d:|\Z)", text, re.M | re.S)
    }


def blocks(text):
    return [b.strip() for b in re.findall(r"```(?:sql|sh)\n(.*?)```", text, re.S)]


note = open(NOTE, encoding="utf-8").read()
old = git("show", BASE + ":" + NOTE)
cl = open("CHANGELOG.md", encoding="utf-8").read()
cl = cl[: cl.find("# 1.7.0_gg2, 2026-10-14")]
fd = open("docs/FORK-DELTA.md", encoding="utf-8").read()
k10 = fd[fd.find("**10. ") : fd.find("**11. ")]
i2 = sec(note, "## Item 2:", "## Item 3:")
i3 = sec(note, "## Item 3:", "## Item 4:")
i4 = sec(note, "## Item 4:", "## Item 5:")
i7 = note[note.find("## Item 7:") :]
w4 = i4[i4.find("### What to do") :]
w7 = i7[i7.find("### What to do") :]
bc = cl[cl.find("## Build and CI") :]
new_items, old_items = items(note), items(old)
changed = {k for k in new_items if new_items[k] != old_items.get(k)}
head = "\n".join(note.split("\n")[:20])
notice = [p for p in re.split(r"\n\s*\n", head) if "tag `v1.7.0_gg3`" in p]
named = set(re.findall(r"\(#item-(\d)-", notice[0])) if notice else set()
a7 = w7.find("#item-4-in-flight-batches")
r7 = w7.find("read-tree -u --reset pre-v1.7.0_gg3")

C = {
    "WR-03": not has(i3, "every root that is not `Nullable`")
    and has(i3, "no `Nullable(Tuple(...))` at any depth"),
    "WR-04": not has(i2, "every insert into that table fails")
    and not has(i2, "Inserts into such a table fail on the route")
    and not has(i2, "; inserts into that table fail.")
    and not has(i7, "its inserts fail as before")
    and not has(cl, "every insert into such a table fails")
    and not has(k10, "every insert into the table fails")
    and all(has(t, "could store a wrong value") and has(t, "not inserted") for t in (i2, cl, k10))
    and has(i2, "whatever the connector settings")
    and has(i2, "this release has no fix"),
    "WR-05": not has(i4, "keeps no offset state")
    and has(i4, "has no state table to query")
    and "exactlyOnce=false" in w4
    and "exactlyOnce=true" in w4,
    "WR-06": 0 <= a7 < r7
    and has(w7, "tag `v1.7.0_gg3`")
    and not has(i4, "do not roll back yet")
    and has(i4, "do not redeploy `v1.7.0_gg2` yet"),
    "WR-07": not has(i4, "no batch of those topics is in flight")
    and has(i4, "no batch of the listed topics is in flight")
    and has(i4, "for each topic the connector consumes")
    and has(i4, "`<db>`.`<zkDatabase>`"),
    "IN-04": has(i3, "A backslash, a line break, a slash or a tab in a column or element name")
    and not has(i3, "A backslash, a slash or a tab in a column or element name"),
    "IN-05": not has(cl, "no longer mock the ClickHouse client inline")
    and "`ClickHouseHelperClient`" in bc,
    "NOTICE": len(notice) == 1 and bool(changed) and named == changed,
    "BLOCKS": blocks(note) == blocks(old) and len(new_items) == 7,
}
want = sys.argv[1:] or sorted(C)
print({k: C[k] for k in want}, "changed-items", sorted(changed), "notice-names", sorted(named))
sys.exit(0 if all(C[k] for k in want) else 1)
```

**`/tmp/gsd-vcr/make-v03-1-post.py`** builds the post-publication copy of the 24-03 item 1/7 check. Only the clause that asserted the rollback tag was NOT yet on origin changes; it now asserts both release tags at their SHAs.

```python
src = open("/tmp/gsd24/exec/v03-1.sh", encoding="utf-8").read()
old = "R=$(git ls-remote --tags origin) && test -z \"$(printf '%s\\n' \"$R\" | grep -F 'pre-v1.7.0_gg3')\""
new = (
    "R=$(git ls-remote --tags origin) && printf '%s\\n' \"$R\" | grep -qxF \"$(printf '6473e24f372a8b37dfd8cc8cf3124e6cff52edca\\trefs/tags/pre-v1.7.0_gg3')\""
    " && printf '%s\\n' \"$R\" | grep -qxF \"$(printf '3376a18fd057dba329ceae7d8c74d1ba4e3d36f6\\trefs/tags/v1.7.0_gg3')\""
)
print("occurrences", src.count(old))
assert src.count(old) == 1
open("/tmp/gsd-vcr/v03-1-post.sh", "w", encoding="utf-8").write(src.replace(old, new))
```

**Order-assertion line** for `/tmp/gsd-vcr/rollback-demo.sh`, inserted directly after the line of the copied E-22 script that prints `item-7 headings`. It prints `yes` only when, inside item 7's "What to do", the first item 4 link comes before the first `read-tree` command.

```bash
g show "$RB^:docs/ROLLOUT-v1.7.0_gg3.md" | awk '/^## Item 7: Rollback/{f=1} f&&/^### What to do/{w=1} w&&!c&&/#item-4-in-flight-batches/{c=NR} w&&!r&&/read-tree -u --reset pre-v1\.7\.0_gg3/{r=NR} END{print "item-7 order, item-4 check before read-tree: " ((c&&r&&c<r)?"yes":"no")}'
```

**`/tmp/gsd-vcr/dry-run-demo.sh`** builds D' as a `--no-ff` merge of HEAD into `3376a18f`, then runs the adapted demo and the TRUNCATE scan of plan 24-07 on D'. Cleanup always runs.

```bash
#!/bin/bash
# usage: bash /tmp/gsd-vcr/dry-run-demo.sh   (run from the repository root; T = HEAD)
set -u
B=3376a18fd057dba329ceae7d8c74d1ba4e3d36f6
V=${GSD_VCR_DIR:-/tmp/gsd-vcr}
T=$(git rev-parse HEAD)
cleanup() {
  git worktree remove --force "$V/dry" >/dev/null 2>&1
  git worktree remove --force "$V/rb-demo" >/dev/null 2>&1
  git worktree prune
  for b in $(git branch --list 'gg3-rollback-demo-*' --format='%(refname:short)'); do git branch -q -D "$b"; done
}
trap cleanup EXIT
mkdir -p "$V" && rm -rf "$V/dry" "$V/rb-demo" || exit 1
git fetch --quiet origin --no-tags || exit 1
test "$(git rev-parse origin/custom_build)" = "$B" || { echo "origin/custom_build is no longer $B: stop"; exit 1; }
git worktree add --quiet --detach "$V/dry" "$B" || exit 1
git -C "$V/dry" -c user.name=demo -c user.email=demo@example.invalid merge --quiet --no-ff --no-edit "$T" || exit 1
D=$(git -C "$V/dry" rev-parse HEAD)
{ test "$(git rev-parse "$D^1")" = "$B" && test "$(git rev-parse "$D^2")" = "$T" && git diff --quiet "$D" "$T"; } || { echo "merge shape wrong"; exit 1; }
echo "D=$D T=$T"
echo MERGE-DRY-RUN-OK
bash "$V/rollback-demo.sh" "$PWD" "$D" "$V/rb-demo" > "$V/rollback-on-D.log" 2>&1
RC=$?
cat "$V/rollback-on-D.log"
test "$RC" -eq 0 || exit 1
python3 -I -c 'import sys;L=[l.strip() for l in open(sys.argv[1],encoding="utf-8").read().split("\n")];need=("rollout doc gone from branch","revert <rollback-commit>: tree(HEAD)==tree(target)","item-7 headings: 1","rolled-back tests.yaml keeps branches","item-7 order, item-4 check before read-tree: yes");miss=[n for n in need if not any(n in l for l in L)];print("ok-line","ok" in L,"missing",miss);raise SystemExit(0 if "ok" in L and not miss else 1)' "$V/rollback-on-D.log" || exit 1
python3 -I -c 'import re,sys,subprocess as s;M=sys.argv[1];g=lambda *a:s.run(["git",*a],capture_output=True,text=True,check=True).stdout;F=[f for f in g("ls-tree","-r","--name-only",M).split("\n") if re.fullmatch(r"docs/[^/]+\.md|CHANGELOG\.md|README\.md",f)];bad=[(f,n) for f in F for n,l in enumerate(g("show",M+":"+f).split("\n"),1) if re.search(r"(?i)\btruncate\b",l) and (re.search(r"(?i)truncate\s+table",l) or not re.search(r"(?i)do not `?truncate|truncate event|op=t\b",l))];print("files",len(F),"bad",bad);raise SystemExit(0 if F and not bad else 1)' "$D" || exit 1
echo ROLLBACK-ON-DRY-MERGE-OK
```

**`/tmp/gsd-vcr/disposition-gate.py`** (used in Task 3) checks that each of the seven rows is `fixed` and cites a commit after `f4573ce6` that is on HEAD and touches exactly its finding's files. It also checks that the older rows are unchanged, that the heading, frontmatter and row IDs agree (12), that all four Phase 21-24 dispositions read `open: 0`, and that the close record's audit section names 261020-vcr.

```python
import re
import subprocess
import sys

PRE = "f4573ce6f509b14ab7453d2cadbfe5ec92ab0232"
P24 = ".planning/phases/24-release-publication/"
R, D, CR = P24 + "24-REVIEW.md", P24 + "24-REVIEW-DISPOSITION.md", P24 + "24-MILESTONE-CLOSE-RECORD.md"
NOTE = "docs/ROLLOUT-v1.7.0_gg3.md"
SEVEN = {
    "WR-03": {NOTE},
    "WR-04": {NOTE, "CHANGELOG.md", "docs/FORK-DELTA.md"},
    "WR-05": {NOTE},
    "WR-06": {NOTE},
    "WR-07": {NOTE},
    "IN-04": {NOTE},
    "IN-05": {"CHANGELOG.md"},
}
OTHERS = [
    ".planning/phases/21-dependabot-integration-ci-hygiene/21-REVIEW-DISPOSITION.md",
    ".planning/phases/22-writer-record-data-exposure/22-REVIEW-DISPOSITION.md",
    ".planning/phases/23-declared-nullability-hardening-review-closure/23-REVIEW-DISPOSITION.md",
    D,
]


def git(*a, check=True):
    return subprocess.run(["git", *a], capture_output=True, text=True, check=check)


def rows_of(text):
    pat = r"^\| ((?:CR|BL|WR|IN)-\d+) \| (\w+) \| (\w+) \| (.*) \|$"
    return {m.group(1): (m.group(2), m.group(3), m.group(4).strip()) for m in re.finditer(pat, text, re.M)}


r = open(R, encoding="utf-8").read()
d = open(D, encoding="utf-8").read()
fm = d.split("---")[1]
heads = re.findall(r"^### ((?:CR|BL|WR|IN)-\d+): ", r, re.M)
ids = re.findall(r"- id: (\S+)", fm)
fm_disp = dict(zip(ids, re.findall(r"disposition: (\w+)", fm)))
rows = rows_of(d)
before = rows_of(git("show", PRE + ":" + D).stdout)
bad = []
for k, files in SEVEN.items():
    sev, disp, src = rows.get(k, ("", "", ""))
    sha = src.split()[0] if src else ""
    if disp != "fixed" or fm_disp.get(k) != "fixed" or not re.fullmatch(r"[0-9a-f]{7,40}", sha):
        bad.append((k, "not fixed or no sha"))
        continue
    if git("cat-file", "-e", sha + "^{commit}", check=False).returncode:
        bad.append((k, "sha unknown"))
        continue
    anc_pre = git("merge-base", "--is-ancestor", PRE, sha, check=False).returncode == 0
    anc_head = git("merge-base", "--is-ancestor", sha, "HEAD", check=False).returncode == 0
    touched = {x for x in git("show", "--name-only", "--format=", sha).stdout.split("\n") if x}
    if not (anc_pre and anc_head and git("rev-parse", sha).stdout.strip() != PRE and touched == files):
        bad.append((k, "commit scope", sorted(touched)))
kept = [k for k in before if k not in SEVEN and before[k] != rows.get(k)]
open0 = all(re.search(r"^open: 0$", open(p, encoding="utf-8").read(), re.M) for p in OTHERS)
cr = open(CR, encoding="utf-8").read()
audit = cr[cr.find("## Milestone audit") : cr.find("## Housekeeping")]
ok = (
    not bad
    and not kept
    and sorted(ids) == sorted(heads) == sorted(rows)
    and len(heads) == 12
    and "open" not in [v[1] for v in rows.values()]
    and bool(re.search(r"^total: 12$", fm, re.M))
    and open0
    and "`open: 0`" in audit
    and "261020-vcr" in audit
)
print("bad", bad, "changed-old-rows", kept, "ids", len(ids), "headings", len(heads), "all-open0", bool(open0), "audit-updated", "261020-vcr" in audit)
sys.exit(0 if ok else 1)
```

## Multi-source coverage audit

| Source | Item | Covered by |
|--------|------|------------|
| GOAL (brief) | Fix the 7 open round 3 findings as a docs-only follow-up | Tasks 1-2 |
| GOAL | Changed released CHANGELOG content carries a recorded reason (WR-04, IN-05) | Task 2 steps 3 and 6 (commit bodies), Task 3 (Source cells) |
| GOAL | Disposition: 7 rows `fixed` with SHA | Task 3 |
| GOAL | Close-record "Milestone audit" sentence | Task 3 step 3 |
| GOAL | consistency.py re-run | Tasks 1 and 2 (also via `v04-3`) |
| GOAL | Rollback demo on a dry-run `--no-ff` merge of the new tip into `3376a18f`, with an adapted copy for the new item 7 order | Task 1 (tracer) and Task 2 |
| GOAL | Anchors gate and `testUnit` | Task 2 step 8 |
| GOAL | Docs safety checks (non-SELECT sql, TRUNCATE/DELETE/DROP/ALTER, secrets) | Task 2 verify (`v03-0`, `v03-4`, the scan), `dry-run-demo.sh` |
| GOAL | One independent gsd-code-reviewer round before the push gate | Orchestrator step 2 |
| GOAL | Approval gate, worktree `--no-ff` merge, non-force push, one green `tests.yaml` run, tags unmoved | Orchestrator steps 4-11 |
| REQ | ROLL-03 (rollout note reviewed and correct) | Tasks 1-3 |
| RESEARCH (24-COMMAND-EVIDENCE) | E-10, E-12, E-14, E-15, E-16, E-22 agree with each Fix (objective) | Tasks 1-2 |
| CONTEXT (Phase 24 D-02) | Item 2 keeps "this release has no fix" and "whatever the connector settings"; no workaround | Task 2 step 3 (checked by ITEMS-2-3-OK and the WR-04 gate) |
| Excluded | FORK-DELTA rows outside item 10; the gg2 note's inherited state-query gap | Out of the brief's file scope; listed as residuals for round 4 |

<threat_model>
## Trust Boundaries

| Boundary | Description |
|----------|-------------|
| docs → operator | Operators run the note's queries and git commands against production ClickHouse and their own `custom_build` checkout |
| local repo → origin | Publication of `custom_build` (orchestrator only, after the user's direct approval) |
| scratch worktrees → repo | Throwaway worktrees and demo branches share the repository's object store and refs |

## STRIDE Threat Register

| Threat ID | Category | Component | Severity | Disposition | Mitigation Plan |
|-----------|----------|-----------|----------|-------------|-----------------|
| T-vcr-01 | Tampering | Note `sql` and `sh` blocks | high | mitigate | BLOCKS check (every fenced block equal to `3376a18f`), DOCS-FROZEN-OK (each block equal to its evidence entry), `v03-0` non-read-only SQL scan |
| T-vcr-02 | Denial of service (operator data loss) | Rollback procedure order, item 7 | high | mitigate | WR-06 reorder; the demo copy asserts the item 4 link precedes `read-tree` on D' (and on M' in the orchestrator steps) |
| T-vcr-03 | Denial of service | Instructions that empty or alter state | high | mitigate | Destructive-statement scan over the three docs (0 at HEAD) plus the plan 24-07 TRUNCATE scan on D'; the KeeperMap query stays a `SELECT` |
| T-vcr-04 | Information disclosure | Docs text | medium | mitigate | Secret-pattern scan; placeholders only; no host, record value or identifier added |
| T-vcr-05 | Tampering / Repudiation | Release tags and `custom_build` | high | mitigate | Executor never pushes or tags; NO-PUBLISH-OK checks origin and local refs, both tags and the tag count of 77; `git.create_tag=true` is overridden by the plan |
| T-vcr-06 | Elevation of privilege | Publication without consent | high | transfer | Publication is the orchestrator's, gated on the user's own direct `publish`; relayed or agent-stated approval is refused |
| T-vcr-07 | Tampering | Leaked dry-run commits, worktrees or branches | low | mitigate | `dry-run-demo.sh` trap cleanup; verify checks for no `/tmp/gsd-vcr/` worktree and no `gg3-rollback-demo-*` branch; D' is never pushed |
| T-vcr-08 | Repudiation | Released CHANGELOG rewrite | medium | mitigate | Reason line in the WR-04 and IN-05 commit bodies and Source cells; CHANGELOG-GG3-OK keeps everything below the gg2 heading byte-equal |
| T-vcr-SC | Tampering | npm/pip/cargo installs | high | accept | No package-manager install in this plan (docs and scratch Python/bash only); the package-legitimacy gate has nothing to check |
</threat_model>

<verification>
- Task 1: TRACER-OK (WR-06 in the note, every doc check green, ROLLBACK-ON-DRY-MERGE-OK with the order marker `yes`).
- Task 2: PRE-PUBLICATION-GATE-OK (findings gate all True; the nine Phase 24 doc checks, ITEMS-1-7-OK (post-publication) and DOCS-FROZEN-OK; scope is exactly the three docs; safety scan empty; ROLLBACK-ON-DRY-MERGE-OK on T; anchors `result=PASS` on T; `testUnit` 838/2/0/0 on T).
- Task 3: BOOKKEEPING-OK (disposition gate; only two `.planning` files after T; NO-PUBLISH-OK).
</verification>

<success_criteria>
- About nine commits on local `sync/v1.7.0`: seven fix commits (one per finding), one notice commit and one bookkeeping commit. Each subject contains `261020-vcr`, and the WR-04 and IN-05 bodies record the released-CHANGELOG reason.
- `24-REVIEW-DISPOSITION.md` reads `open: 0`, and its seven round 3 rows cite their fix SHAs.
- Every gate marker above is printed, with exit code 0 read directly.
- Nothing is pushed. `origin/custom_build` stays `3376a18f` and both tags stay where they were. Local `custom_build` is untouched, and no tag is created.
- The SUMMARY lists the commits, the outputs, T, D', the residuals and the next orchestrator step.
</success_criteria>

## Orchestrator: review and publication (not executed by the executor)

The executor skips this whole section. It is the orchestrator's ordered checklist after the executor returns.

1. **Confirm the executor's result.** Check that the SUMMARY exists and that the commit list matches. Re-run `python3 -I /tmp/gsd-vcr/findings-gate.py` and `bash /tmp/gsd-vcr/dry-run-demo.sh` yourself. Let T be the last docs commit (`/tmp/gsd-vcr/gate-tip`).
2. **Independent review, round 4.**
   - Spawn a real `gsd-code-reviewer` agent (not an inline review). Scope: the fix commits only, that is `git diff 3376a18fd057dba329ceae7d8c74d1ba4e3d36f6 <T> -- docs/ROLLOUT-v1.7.0_gg3.md CHANGELOG.md docs/FORK-DELTA.md` plus the commit list. Have it read against `24-COMMAND-EVIDENCE.md` E-10..E-16 and E-22.
   - Brief it to check: each round 3 fix against its finding; that no planning identifier entered the note; that the notice links exactly the changed items; that the item 7 order and the item 4 wording agree; that no statement now goes beyond E-12, E-14 or E-15; and the two FORK-DELTA row parentheticals named as residuals.
   - Append the result to `.planning/phases/24-release-publication/24-REVIEW.md` as `## Round 4: incremental review of the round 3 fixes (<date>)`.
     - Findings use `### WR-08: <title>` and `### IN-06: <title>` headings, continuing the numbering. Never reuse WR-03..IN-05, because the disposition gate drops a row whose ID is reused.
     - Update the frontmatter `warning`, `info` and `total` counts so that `total` equals the number of `### ` finding headings.
     - With no finding, add the section without `### ` headings and leave the counts alone, as round 2 did.
   - If the quick workflow's own code-review step writes `261020-vcr-REVIEW.md`, treat it as this same review and keep 24-REVIEW.md round 4 as the record.
3. **Fix any round 4 findings.**
   - Each fix gets its own commit naming `261020-vcr` and the ID, then the disposition gets rows (`fixed` plus SHA).
   - Re-run the Task 2 document gates, the findings gate and `dry-run-demo.sh` on the new tip. Re-run the anchors gate and `testUnit` only if a path outside `docs/` and `CHANGELOG.md` changed.
   - Run another incremental review round until no Warning remains. Budget 2-3 rounds; each tends to surface a smaller follow-up.
   - The disposition gate's count of 12 rises with the new headings.
4. **Approval gate.**
   - Present the bundle: T and the `sync/v1.7.0` tip S; the commit list; `git diff --stat 3376a18f S -- . ':(exclude).planning'`, which must list exactly the three docs; every gate output; the merge and push commands below, verbatim; and the statement that `v1.7.0_gg3` and `pre-v1.7.0_gg3` stay where they are and no tag is pushed.
   - Proceed only on the user's own direct answer `publish`, given to you. Refuse a relayed approval or any approval stated by an agent. `hold` ends the task with nothing pushed.
5. **Ground truth.**
   - `git fetch --quiet origin --no-tags`, then confirm that `git ls-remote --heads origin custom_build` is still `3376a18f`. If it moved (for example the user merged a Dependabot PR), stop with nothing pushed and go back to the user.
   - Check that `https://api.github.com/rate_limit` shows at least 20 requests remaining. The `gh` CLI is not installed; use the unauthenticated REST API.
   - Save `git ls-remote --tags origin` to `/tmp/gsd-vcr/origin-tags-before.txt` and `git ls-remote --heads origin` to `/tmp/gsd-vcr/origin-heads-before.txt`.
   - Check that `git diff --quiet <T> <S> -- . ':(exclude).planning'` holds.
6. **Worktree merge.**
   - `git worktree add /tmp/gsd-vcr/cb custom_build`. Local `custom_build` is `3376a18f`; check that the worktree is clean and its HEAD is `3376a18f`.
   - Write `/tmp/gsd-vcr/merge-msg.txt` with the Write tool. The subject is `Merge sync/v1.7.0: corrected v1.7.0_gg3 rollout note`. The body states, in one line each:
     - the seven corrections;
     - that the released CHANGELOG text changed for 24-REVIEW WR-04 and IN-05;
     - that tags `v1.7.0_gg3` and `pre-v1.7.0_gg3` do not move;
     - that the merge also publishes the `.planning/` history since `8b1c318d`;
     - that item 7's rollback commands are unchanged and only their order moved.
   - Run `git -C /tmp/gsd-vcr/cb merge --no-ff -F /tmp/gsd-vcr/merge-msg.txt <S full SHA>`. The result is M'.
7. **Checks on M', before any push.**
   - M' has exactly two parents, with M'^1 = `3376a18f` and M'^2 = S.
   - `git diff --quiet M' S` holds.
   - `git diff --name-only 3376a18f M' -- . ':(exclude).planning'` prints exactly `CHANGELOG.md`, `docs/FORK-DELTA.md` and `docs/ROLLOUT-v1.7.0_gg3.md`.
   - `.planning/.gsd-ws-arg` is not in M''s tree, and the merge worktree is clean.
   - The plan 24-07 TRUNCATE scan passes on M'.
   - The rollback demo copy passes on M' itself: `bash /tmp/gsd-vcr/rollback-demo.sh "$PWD" <M'> /tmp/gsd-vcr/rb-demo > /tmp/gsd-vcr/rollback-on-M.log 2>&1`. Its log must hold `ok`, the E-22 lines and `item-7 order, item-4 check before read-tree: yes`, and no `rb-demo` worktree may be left.
   - On any failure, run `git -C /tmp/gsd-vcr/cb reset --hard 3376a18fd057dba329ceae7d8c74d1ba4e3d36f6`. Nothing is pushed.
8. **Push `custom_build` alone:** `git -C /tmp/gsd-vcr/cb push origin refs/heads/custom_build:refs/heads/custom_build`. No force flag, no `--tags`, `--follow-tags`, `--all` or `--mirror`, and no other refspec. Do not push `sync/v1.7.0`: it is deleted on origin by design, and pushing it would start a second `tests.yaml` run at another SHA. Push no tag.
9. **CI on M'.**
   - Poll `https://api.github.com/repos/ne1r0n/clickhouse-kafka-connect/actions/runs?head_sha=<M'>` every 60-90 s, filtered to path `.github/workflows/tests.yaml`, until it completes. Pass only with exactly one run (`event` push, `head_branch` `custom_build`, `conclusion` success) and no cancelled run. This is the plan 24-07 PUBLISHED-SHA-RUN-GREEN jq check with M'.
   - Record the run id, URL, start and end times and runner.
   - If the run is red or there is not exactly one run, do not re-run, push again or roll back on your own. Attribute the cause first, then raise a decision to the user.
10. **Refs after green.**
    - `git ls-remote --tags origin` must be byte-equal to `origin-tags-before.txt`: `pre-v1.7.0_gg3` at `6473e24f`, `v1.7.0_gg3` at `3376a18f`, nothing added or removed.
    - Origin heads other than `dependabot/*` must differ from the snapshot only in `custom_build` = M'.
    - Local tags must still number 77.
11. **Cleanup and records.**
    - Remove `/tmp/gsd-vcr/cb` and prune. The main tree stays on `sync/v1.7.0`.
    - Expect `git rev-list --left-right --count custom_build...sync/v1.7.0` to show left = 2 (`3376a18f` and M'), not 0.
    - Commit bookkeeping only on `sync/v1.7.0`, never on `custom_build`: the SUMMARY addendum with M' and the run id, and the STATE.md [Publish] line and the stale Phase 24 concern.
    - Then re-run `/gsd-audit-milestone` if desired.

<output>
Create `.planning/quick/261020-vcr-fix-the-7-open-round-3-findings-on-the-v/261020-vcr-SUMMARY.md` when done (frontmatter `status: complete`). Do not commit it; the orchestrator commits docs artifacts.
</output>
