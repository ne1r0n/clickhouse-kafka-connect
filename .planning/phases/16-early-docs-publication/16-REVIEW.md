---
phase: 16-early-docs-publication
reviewed: 2026-10-10T17:55:00Z
depth: standard
diff_base: 505e49819fba7b2ba980a718d9ec6b66b55027b0
diff_base_incremental: 9207ec019358f7e88db4727237b7505e59bb396a
files_reviewed: 3
files_reviewed_list:
  - CHANGELOG.md
  - docs/FORK-DELTA.md
  - docs/ROLLOUT-v1.7.0_gg1.md
findings:
  critical: 0
  warning: 1
  info: 1
  total: 2
status: issues_found
---

# Phase 16: Code Review Report

**Reviewed:** 2026-10-10T17:55:00Z
**Depth:** standard
**Files Reviewed:** 3
**Status:** issues_found
**Diff base:** `505e49819fba7b2ba980a718d9ec6b66b55027b0` (HEAD `bb063373`)
**How it ran:** inline, following `~/.claude/gsd-core/agents/gsd-code-reviewer.md` (no sub-agent tool in the executor; see "Skill adaptation" in 16-02-SUMMARY.md)

## Summary

Scope is exactly the Phase 16 change to the three operator-facing documents: `git diff 505e4981 HEAD -- CHANGELOG.md docs/FORK-DELTA.md docs/ROLLOUT-v1.7.0_gg1.md`, produced by commits `65582a31` (FORK-DELTA, IN-09), `fad6f7f9` (rollout, IN-07/IN-08/IN-10) and `8c1f0bc1` (CHANGELOG, IN-10). Surrounding text was read for context only.

The change is sound. One Info finding: the item 8 sentence added for D-05 tells the operator to merge `sync/v1.7.0`, a branch that exists only locally and is not on origin. Nothing Critical or Warning was found.

Explicit checks, beyond the standard list:

- **(a) D-01..D-06 applied as decided.** CHANGELOG corrected in place with no correction note (D-01). Rollout in plain language: item 2 and item 5 headings and TOC rewritten, every `L-nn` inside a `(ledger L-nn)` tag, the ledger linked once at its archived path (D-02). FORK-DELTA changed on lines 94 and 151-156 only (D-03). Item 2 opens with "Step 1 always; step 2 only for connectors with `exactlyOnce=true`", states the replayed-ranges sentence once, and ends on the nothing-to-recover sentence (D-04). Item 8 gained one note (D-05). The item 6 grep alternative is `set_compression_method"?[[:space:]]*[:=][[:space:]]*"?false` (D-06). Tool-checked by E-05, E-07, E-11, E-13 in `16-COMMAND-EVIDENCE.md`.
- **(b) Quoted commands have evidence whose output matches the text.** FORK-DELTA's two `grep -rho ... | wc -l` lines print 7 and 1 (E-01, E-02). The four `git diff --shortstat` commands print the table rows (E-03). The item 6 grep returns the four risky lines and none of the four safe ones (E-05). `git revert -m 1` then merging the second parent again changes nothing, and reverting the revert restores the release tree (E-06). All verdicts "yes".
- **(c) No bare planning ID in rollout or CHANGELOG operator text.** E-07 and E-13 pass. The one remaining `backlog 999.1` is inside the verbatim startup-WARN quote (exempt) and resolves through FORK-DELTA Known costs item 1, which the WARN itself names. FORK-DELTA is exempt (D-03).
- **(d) No secret, hostname, address or production identifier added.** The added lines contain no URL, address, credential or host; the only `@` is the `@Disabled` annotation text in FORK-DELTA.
- **(e) Added CHANGELOG facts agree with the other docs.** `version=` segment and client name `clickhouse-kafka-connect/v1.7.0_gg1` match rollout item 7 and FORK-DELTA row 52. lz4-java `1.10.2` to `1.12.0`, advisory floor `1.11.4`, `1.11.2` and `1.11.3` affected, match FORK-DELTA row 55 and the catalog (E-12).
- **(f) Truncation advice across `docs/`.** `docs/DESIGN.md`, `docs/FORK-DELTA.md`, `docs/ROLLOUT-v1.7.0_gg1.md`, `README.md` and `CHANGELOG.md` contain two truncate lines in total: the rollout "Do not `TRUNCATE` the table." sentence and the FORK-DELTA Debezium truncate-event line (E-09).

## Info

### IN-01: Item 8 tells the operator to merge `sync/v1.7.0`, a branch that is not published

**File:** `docs/ROLLOUT-v1.7.0_gg1.md:322`
**Issue:** The D-05 note reads "merging `sync/v1.7.0` again without that brings nothing back". `sync/v1.7.0` is a local scratch branch. Origin carries `custom_build` and `main` only, so an operator following the note finds no such ref (`git merge sync/v1.7.0` fails with "not something we can merge"). This is the only mention of the branch in the rollout checklist, and item 8 step 1 already names the commit the operator has: `<merge-commit>`, the `v1.7.0_gg1` merge commit. The released history is its second parent. Running `git merge <merge-commit>^2` after the revert prints "Already up to date." (E-06 ran exactly this on `f328fd21`), which is what the note's "brings nothing back" describes.
**Fix:** Name the history the operator can reach: "merging the released history again (`git merge <merge-commit>^2`) without that brings nothing back." Re-run the demonstration with that command and record it as evidence.

## Mandated confirmations (D-09)

**Confirmation 1: do `505e4981`'s WR-01..WR-03 fixes still read correctly after the Phase 16 edits? Yes.** Checked against the tree at HEAD, not against memory of the base.

- WR-01, item 1: the "table is shared" scoping paragraph, the topic-scoped state query with `arrayStringConcat(arrayPopBack(splitByChar('-', `key`)), '-') IN (...)`, the `enableDbTopicSplit` prefix sentence and the "may never become empty" warning are all present and unchanged. Item 1's What to do keeps the stale-row bullet.
- WR-01, item 2: the prefixed-key comment, both `DELETE` examples (key list and topic-scoped) and the "Do not `TRUNCATE` the table." paragraph are byte-identical to `505e4981` (E-08, E-11). The Phase 16 opener "Step 1 always; step 2 only for connectors with `exactlyOnce=true`" sits above them and does not contradict them.
- WR-02, item 3: the check reads "decide on `value`, not on `changed`", with the `basic` and `best_effort` outcomes spelled out. FORK-DELTA line 134 (L-05) keeps "the default on ClickHouse 26.9; older releases default to `basic`". The Phase 16 edit to this item only added the `(ledger ...)` tags and "the case of the last note above".
- WR-03, item 7: the `ObjectName` pattern `...,name=SinkTask*,version=*,*` and the regular-expression form `version=[^,]*`, with the sentence that `version=*` in a regular expression silently stops matching, are present and unchanged.

**Confirmation 2: does any operator text in the tree to be merged tell an operator to TRUNCATE the shared KeeperMap state table? No.** The only sentence about `TRUNCATE` in operator-facing text is the intended negative, "Do not `TRUNCATE` the table." (`docs/ROLLOUT-v1.7.0_gg1.md:111`), followed by the reason. The FORK-DELTA line 126 hit is a Debezium truncate event (a record type), not an instruction. The CHANGELOG says "a KeeperMap state clear" and points to the rollout checklist; no `TRUNCATE` word appears there. The MERGED-TREE-NO-TRUNCATE-INSTRUCTION command run on HEAD `bb063373e3a67fff83a461872148b021d35b7763` printed `md-files 17 truncate-lines 2 bad []` and `MERGED-TREE-NO-TRUNCATE-INSTRUCTION`.

---

_Reviewed: 2026-10-10T17:55:00Z_
_Reviewer: Claude (gsd-code-reviewer procedure, run inline)_
_Depth: standard_

## Incremental review: post-fix (diff base 9207ec01)

**Reviewed:** 2026-10-10
**Scope:** `git diff 9207ec01 HEAD -- docs/ROLLOUT-v1.7.0_gg1.md` (one line, `docs/ROLLOUT-v1.7.0_gg1.md:322`, commit `fd99a476`). Surrounding item 8 text and `16-COMMAND-EVIDENCE.md` E-14 read for context.

**The IN-01 fix is correct.** The sentence now reads "merging the released history again (`git merge <merge-commit>^2`) without that brings nothing back."

- `<merge-commit>` is defined two lines above as the merge tagged `v1.7.0_gg1`. That is `f328fd21` (`git tag --points-at` confirms; the tag is on origin). Its second parent, `f328fd21^2`, is `ae8520ed`, the released history. It resolves for a `--no-ff` merge because both parents are recorded.
- `ae8520ed` is an ancestor of the published tip `537beb1f` (checked with `merge-base --is-ancestor`), so the ref the operator types is reachable from `origin/custom_build`. The unpublished `sync/v1.7.0` name is gone from the checklist.
- It reads consistently with the revert text. After `git revert -m 1 <merge-commit>`, git treats `^2` as already merged and prints "Already up to date.", which is what "brings nothing back" says. Reverting the revert is what restores the release. I reproduced both on a throwaway worktree at `537beb1f`: merge prints "Already up to date.", the revert-of-revert restores the tree of `537beb1f`.
- No new defect in the changed line: no secret, hostname, planning ID or sync-branch name was added.

One issue found in the neighbouring step the fix sits beside. It is not caused by the one-line diff, but the publication merge the fix was written for makes it reachable.

### WR-01: Item 8 step 1 `git revert -m 1 <merge-commit>` conflicts on the published `custom_build` tip, and E-14 does not cover that state

**File:** `docs/ROLLOUT-v1.7.0_gg1.md:318`
**Issue:** Step 1 says "On `custom_build`, back out the whole release in one commit: `git revert -m 1 <merge-commit>`". The release merge `f328fd21` is no longer the tip of `custom_build`. The docs-only publication merge `537beb1f` (parents `f328fd21` and `1e0eefde`) sits on top and changed `CHANGELOG.md`, `docs/FORK-DELTA.md` and `docs/ROLLOUT-v1.7.0_gg1.md` after the release. I ran step 1 on a detached worktree at `537beb1f`. It stops with conflicts: `UU CHANGELOG.md`, `UU docs/FORK-DELTA.md`, `UD docs/ROLLOUT-v1.7.0_gg1.md` (modify/delete, because the pre-release tree has no rollout file), and many `.planning/` files. The operator does not get "one commit"; they get a stopped revert in the middle of an incident. E-06 and E-14 both demonstrate on `f328fd21`, the pre-publication state, so their "Matches text: yes" does not describe what an operator will see on the published branch. The go-forward sentence this fix touches only holds if the revert actually completed.
**Fix:** Give step 1 a path that works on the published tip, and re-run E-06/E-14 at `537beb1f`. A sequence that worked in the same throwaway worktree: `git revert --no-commit -m 1 <merge-commit>`, then `git read-tree -u --reset pre-v1.7.0-sync`, then `git commit`. The resulting tree equals the `pre-v1.7.0-sync` tree, `git merge <merge-commit>^2` then prints "Already up to date.", and `git revert <revert-commit>` restores the tree of `537beb1f`. Alternatively state in step 1 that conflicts in the three docs files and `.planning/` are expected and are resolved by taking the `pre-v1.7.0-sync` side.
