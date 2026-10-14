---
phase: 16-early-docs-publication
verified: 2026-10-11T17:05:00Z
status: passed
score: 5/5 must-haves verified
covered_files:
  - ".planning/phases/16-early-docs-publication/16-01-PLAN.md"
  - ".planning/phases/16-early-docs-publication/16-01-SUMMARY.md"
  - ".planning/phases/16-early-docs-publication/16-02-PLAN.md"
  - ".planning/phases/16-early-docs-publication/16-02-SUMMARY.md"
  - ".planning/phases/16-early-docs-publication/16-03-PLAN.md"
  - ".planning/phases/16-early-docs-publication/16-03-SUMMARY.md"
  - ".planning/phases/16-early-docs-publication/16-COMMAND-EVIDENCE.md"
  - ".planning/phases/16-early-docs-publication/16-REVIEW-DISPOSITION.md"
  - ".planning/phases/16-early-docs-publication/16-REVIEW.md"
  - ".planning/phases/16-early-docs-publication/16-UAT.md"
  - "CHANGELOG.md"
  - "docs/FORK-DELTA.md"
  - "docs/ROLLOUT-v1.7.0_gg1.md"
covered_digest: "v3:sha256:c6e60ebdb16854a97db4e8c70048236c84b9039a9e22f824e9794d13bc242ac4"
behavior_unverified: 0
overrides_applied: 0
re_verification:
  previous_status: human_needed
  previous_score: 5/5
  gaps_closed:
    - "WR-01 (item 8 step 1 conflicts on the published tip): fixed on sync/v1.7.0 in cf8cfc04, with WR-02, IN-02, IN-04 follow-ups; NOT on origin"
  gaps_remaining: []
  regressions: []
human_verification:
  - test: "Approve (or decline) republishing the item 8 rollback fix to origin/custom_build"
    expected: "Local sync/v1.7.0 holds the corrected item 8 (commits cf8cfc04, c2b7c49d, 35baed27, all docs/ROLLOUT-v1.7.0_gg1.md only). origin/custom_build is still 537beb1f and still carries the old step 1 `git revert -m 1 <merge-commit>`, which stops with 97 conflicts. A new docs-only, non-force, --no-ff merge into custom_build, with no tag, makes the corrected text what an operator reads. Alternatively the user records that the fix may stay unpublished until Phase 20."
    why_human: "Merging and pushing a protected branch needs the user's own explicit approval (16-03-PLAN execution rules). A relayed approval is refused. I did not push, merge, tag or edit any doc."
---

# Phase 16: Early Docs Publication Verification Report

**Phase Goal:** An operator reading `origin/custom_build` gets a rollout doc that can no longer erase other connectors' exactly-once state, with the 15-REVIEW docs findings fixed. This happens before any code work starts and ships no code.
**Verified:** 2026-10-11T17:05:00Z
**Status:** human_needed
**Re-verification:** Yes, after the WR-01 decision (UAT option (a), fix) and the docs fixes that followed. The earlier report of 2026-10-10 was stale because `docs/ROLLOUT-v1.7.0_gg1.md` changed after it was written.

## Verdict

The phase goal, as written, is achieved by what is already on `origin/custom_build`. I checked the remote and the published doc myself. The fix the user chose for WR-01 is real and correct, but it is on the local branch only, so an operator reading `origin/custom_build` today still sees the rollback step the user decided to fix. That one gap between "decided and verified locally" and "what the operator reads" is the only reason the status is `human_needed` and not `passed`. It is not a goal failure and not a gap needing a new plan: republishing is a single user-approved action.

## What is on origin versus local (checked after `git fetch`)

| Ref | SHA | Notes |
|-----|-----|-------|
| `origin/custom_build` | `537beb1fcf23b987c8b340006f4f74f7c5229d00` | `git ls-remote origin`. Unchanged since the first verification. |
| `origin` heads | `custom_build`, `main` (`bec167d8`) | `sync/v1.7.0` is not on origin. |
| `origin` tags | `pre-v1.6.0-sync`, `pre-v1.7.0-sync` (`c61d90ce`), `v1.6.0_gg1` (`c61d90ce`), `v1.7.0_gg1` (`f328fd21`) | No Phase 16 tag. |
| local `sync/v1.7.0` | `4633e0eb` | 18 commits ahead of origin, none pushed. |

`git diff origin/custom_build HEAD`: outside `.planning/` only `docs/ROLLOUT-v1.7.0_gg1.md` differs (6 insertions, 2 deletions, item 8 only). `git diff --quiet origin/custom_build HEAD -- src build.gradle.kts VERSION .github` exits 0, so no code differs. The three docs commits not on origin are `cf8cfc04` (WR-01), `c2b7c49d` (WR-02, IN-02), `35baed27` (IN-04).

`git show origin/custom_build:docs/ROLLOUT-v1.7.0_gg1.md`, item 8 step 1, still reads: "On `custom_build`, back out the whole release in one commit: `git revert -m 1 <merge-commit>` ...". The local text replaces it with a two-part `git read-tree -u --reset pre-v1.7.0-sync` procedure that states the precondition, the destructive behaviour, and that `git revert -m 1` conflicts.

## Goal Achievement

### Observable Truths (ROADMAP success criteria)

| # | Truth | Status | Evidence |
|---|-------|--------|----------|
| 1 | On `sync/v1.7.0`, the three docs read correctly and every quoted command was run with matching output | VERIFIED | The four enumerated bullets are unchanged since the first report (1a-1d below). The umbrella clause now holds for item 8 as well: E-15, E-16, E-17 ran the new item 8 commands, and I reproduced them (see Behavioral Spot-Checks). |
| 1a | Item 2 says which step applies only with `exactlyOnce=true` | VERIFIED | Unchanged at `537beb1f` and HEAD ("Step 1 always; step 2 only for connectors with `exactlyOnce=true`"). |
| 1b | Item 8 says going forward again means reverting the revert | VERIFIED, wording superseded | The text now says "To go forward again, revert the rollback commit first (`git revert <rollback-commit>`); merging the released history again (`git merge <merge-commit>^2` ...) without that brings nothing back." The step it follows is no longer `git revert -m 1`, because that step does not complete on the published tip. The criterion's intent (revert the backing-out commit first) holds. Its literal words "after `git revert -m 1`" are no longer the doc's procedure: the doc now says not to use `git revert -m 1`. I read this as the intended outcome of the WR-01 decision, not a regression. |
| 1c | FORK-DELTA grep commands print a total | VERIFIED | Re-ran on the tree: `@Disabled` prints 7, `Assumptions.assumeTrue` prints 1. FORK-DELTA is unchanged since the publication. |
| 1d | Plain language; MBean `version=`/client-name change and lz4 reason named | VERIFIED | `L-nn` tokens in the rollout: 25, none outside a `ledger` tag line (grep for lines with `L-nn` and no "ledger" is empty). CHANGELOG and FORK-DELTA are unchanged since publication. |
| 2 | Code review of the changed docs recorded and every finding dispositioned before the merge approval was asked | VERIFIED | Pre-approval review and dispositions as in the first report. Since then the post-execution findings WR-01, WR-02, IN-02, IN-03, IN-04 were raised, fixed and re-reviewed. `16-REVIEW.md` frontmatter `status: clean`, and its closing note says "No finding is open". `16-REVIEW-DISPOSITION.md`: 6 findings, all `fixed`, `open: 0`. |
| 3 | No text in the merged tree tells an operator to `TRUNCATE` the shared KeeperMap state table | VERIFIED | `git grep -n -i truncate HEAD -- docs CHANGELOG.md` shows only `ROLLOUT:111` ("Do not `TRUNCATE` the table. It is shared by every `exactlyOnce` connector ...") plus the FORK-DELTA:126 Debezium truncate event. Same result at `537beb1f`, so the published tree is clean too. |
| 4 | `origin/custom_build` is a non-force two-parent `--no-ff` merge of `sync/v1.7.0`, after the user's own approval | VERIFIED (approval attested, not inspectable) | `git ls-remote` shows `custom_build` = `537beb1f`; checks from the first report stand (parents `f328fd21` + `1e0eefde`, old tip is an ancestor, tree equals `1e0eefde`, allow-list, no `.gsd-ws-arg`, one head, no tag). Nothing has been pushed since. |
| 5 | The merge SHA has exactly one green `tests.yaml` run, no cancelled sibling | VERIFIED | From the first report: run `38074050935`, `total_count: 1`, success. The SHA has not changed, and I did not re-query because nothing in this phase's published state moved. |

**Score:** 5/5 truths verified, 0 present-but-behavior-unverified.

### Why the goal counts as met by `537beb1f`, and what stays open

- The goal has two clauses. "Can no longer erase other connectors' exactly-once state" is answered by the TRUNCATE removal and the topic-scoped `DELETE` text, both published and unchanged. "With the 15-REVIEW docs findings fixed" is IN-07..IN-10, all published and `fixed` in `15-REVIEW-DISPOSITION.md`.
- WR-01 is not a 15-REVIEW finding and not a ROADMAP criterion. It is a defect in the Phase 15 rollback step that the publication merge made reachable. Item 8 does not touch the goal's first clause.
- The wording "an operator reading `origin/custom_build`" is the reason I do not call this `passed`. Item 8 on origin is the text the user decided was wrong. Calling the phase `passed` would let that sit unseen. I did not call it `gaps_found` either: the user answered the UAT question with option (a), the fix exists and is verified, and the only missing step is the one only the user can take.
- If the user would rather ship it with Phase 20's publication, that is a valid choice. It should be written down, since `16-REVIEW-DISPOSITION.md` marks WR-01 `fixed` with "not yet published" and nothing downstream forces the publication.

### Required Artifacts

| Artifact | Expected | Status | Details |
|----------|----------|--------|---------|
| `docs/ROLLOUT-v1.7.0_gg1.md` at `537beb1f` (origin) | Corrected rollout doc: IN-07, IN-08, IN-10, WR-01..03 of Phase 15 kept, no TRUNCATE | VERIFIED | Published. Item 8 step 1 carries the WR-01 defect. |
| `docs/ROLLOUT-v1.7.0_gg1.md` at HEAD (local) | Item 8 rewritten (WR-01, WR-02, IN-02, IN-04) | VERIFIED, unpublished | Lines 316-327; differs from origin in item 8 only. |
| `docs/FORK-DELTA.md`, `CHANGELOG.md` | IN-09, IN-10 | VERIFIED | Identical on origin and HEAD. |
| `16-COMMAND-EVIDENCE.md` | Evidence for every quoted command | VERIFIED | E-01..E-17 present. E-15 (rollback on `537beb1f` and dry-run tip), E-16 (precondition wording), E-17 (dirty-tree behaviour and `git stash -u`) back the new item 8. |
| `16-REVIEW.md`, `16-REVIEW-DISPOSITION.md` | Review recorded, all dispositioned | VERIFIED | `status: clean`, `open: 0`. |
| `16-UAT.md` | Human item resolved | VERIFIED | `status: complete`, 1 passed, decision (a), "Republication ... needs its own explicit approval". |

### Key Link Verification

| From | To | Via | Status | Details |
|------|----|-----|--------|---------|
| `custom_build` merge `537beb1f` | `sync/v1.7.0` tip `1e0eefde` | `--no-ff`, M^2 = X, equal trees | WIRED | Unchanged. |
| `tests.yaml` | single CI run | push to `custom_build`, no tag | WIRED | One run. |
| item 8 step 1 text | an operator's rollback on origin | doc on `origin/custom_build` | NOT WIRED on origin | The corrected step exists only on local `sync/v1.7.0`. This is the human item. |

### Data-Flow Trace (Level 4)

Not applicable. The phase changes only Markdown and planning files.

### Behavioral Spot-Checks

I ran these in a throwaway clone under `/tmp` (removed afterwards). The main tree, branches, tags and remote were not touched.

| Behavior | Command | Result | Status |
|----------|---------|--------|--------|
| Old step 1 on the published tip | clone at `custom_build` = `537beb1f`; `git revert --no-edit -m 1 v1.7.0_gg1` | exit 1, 97 unmerged paths | FAIL, as E-15 records; this is why the fix was needed |
| New step 1.1 precondition | tip `537beb1f`, `git status --porcelain` | prints nothing | PASS |
| New step 1.2 | `git read-tree -u --reset pre-v1.7.0-sync` then `git commit -m "Roll back v1.7.0_gg1"`, then `git diff --quiet HEAD pre-v1.7.0-sync && echo ok` | exit 0, prints `ok` | PASS |
| Go-forward: merge brings nothing back | `git merge --no-edit 'v1.7.0_gg1^2'` after the rollback | "Already up to date." | PASS |
| Document readable after rollback | `git show <rollback-commit>^:docs/ROLLOUT-v1.7.0_gg1.md \| grep -c "Item 8: Rollback"` | 1 | PASS |
| Go-forward: revert the rollback commit | `git revert --no-edit <rollback-commit>`; `git diff --quiet HEAD 537beb1f` | exit 0, tree equals `537beb1f` | PASS |
| FORK-DELTA totals | `grep -rho '@Disabled' src/test/java \| wc -l`, same for `Assumptions.assumeTrue` | 7, 1 | PASS |
| No code differs from origin | `git diff --quiet origin/custom_build HEAD -- src build.gradle.kts VERSION .github` | exit 0 | PASS |
| Docs differ from origin only in item 8 | `git diff origin/custom_build HEAD -- docs CHANGELOG.md` | one hunk, rollout lines 315-325 | PASS |

I did not re-run the destructive-behaviour demonstration of E-17 (tracked, staged, untracked overwrite). I read the E-17 script and output, and the claim agrees with documented `git read-tree -u --reset` behaviour. I rely on E-17 and on the reviewer's independent reproduction in `16-REVIEW.md`.

### Probe Execution

Step 7c: SKIPPED. The phase declares no probes (`scripts/*/tests/probe-*.sh`). `verify-merge-invariants.sh` deliberately does not gate this merge (ROADMAP note).

### Requirements Coverage

| Requirement | Source Plan | Description | Status | Evidence |
|-------------|-------------|-------------|--------|----------|
| PUB-05 | 16-01, 16-02 | IN-07..IN-10 fixed in the three docs; changed docs reviewed before the merge gate | SATISFIED | Truths 1a-1d, 2. Published on origin. |
| PUB-06 | 16-03 | Docs-only non-force `--no-ff` merge after the user's approval, diff limited to `.planning/`, `docs/`, `CHANGELOG.md`; one branch, no tag, one green `tests.yaml` run | SATISFIED | Truths 3, 4, 5. Origin still at `537beb1f`. |

Both IDs are in the plan frontmatter, in REQUIREMENTS.md (lines 13-14, `[x]`) and in its traceability table (lines 76-77). REQUIREMENTS.md maps no other ID to Phase 16, so there is no orphaned requirement. Any republication is an additional publication on the same requirement: PUB-06's wording ("one branch pushed, no tag, exactly one green `tests.yaml` run on that SHA") would apply again to the new SHA, which is another reason it needs its own approval and CI check.

### Anti-Patterns Found

| File | Line | Pattern | Severity | Impact |
|------|------|---------|----------|--------|
| `docs/ROLLOUT-v1.7.0_gg1.md` on `origin/custom_build` | 318 | Rollback command that conflicts on the published tip | Warning | An operator following item 8 during an incident gets a stopped revert with 97 conflicts. Fixed locally, not published. |

No TBD/FIXME/XXX/TODO debt marker in the docs lines this phase changed.

### Human Verification Required

#### 1. Republish the item 8 fix to `origin/custom_build`, or record that it waits

**Test:** Decide whether to publish `cf8cfc04`, `c2b7c49d`, `35baed27` (plus the review and evidence records) through a new docs-only, non-force, `--no-ff` merge into `custom_build`, with no tag. This follows `16-03-PLAN.md`'s rule that a docs change after publication goes through a new user-approved publication and is never an edit to `custom_build`. E-15 already dry-ran it as `ea94d28c` and E-16 as `117829bc`; neither was pushed.
**Expected after approval:** the new merge SHA has exactly one green `tests.yaml` run and an allow-list diff against `537beb1f` that names only `.planning/` and `docs/`. `git show origin/custom_build:docs/ROLLOUT-v1.7.0_gg1.md` then shows the two-part step 1 and no `git revert -m 1` instruction.
**If declined:** record the reason in `16-REVIEW-DISPOSITION.md` (Source cell) or add an explicit item to Phase 20, because Phase 20's ROLL-02 covers a new `ROLLOUT-v1.7.0_gg2.md` and does not name item 8 of the gg1 doc.
**Why human:** the push targets a protected branch and needs the user's own explicit answer; I cannot give it, and an agent-relayed approval is refused.

### Gaps Summary

No gaps against the five success criteria and no failed truths. The published tip satisfies PUB-05 and PUB-06. One item is left for the user: the WR-01 fix, which the user chose and which I verified, has not reached `origin/custom_build`. I made no edit, commit, push, merge or tag. The only file I wrote is this report. The pre-existing uncommitted `.planning/state.json` change and the untracked `.planning/milestone.lock` and milestone audit file were left alone.

---

_Verified: 2026-10-11T17:05:00Z_
_Verifier: Claude (gsd-verifier)_

## Human verification resolved: republication (2026-10-11)

The user approved the republication. On 2026-10-11 the user answered "Publish now" in the `/gsd-verify-work 16` session.

- **Merge:** a non-force `--no-ff` merge of `sync/v1.7.0` (`8497dc1d`) into `custom_build`, made in a separate worktree: `be6b8861` "Merge sync/v1.7.0: publish corrected v1.7.0_gg1 rollback steps (item 8)", with parents `537beb1f` and `8497dc1d`.
- **Contents:** outside `.planning/`, the merge changes only `docs/ROLLOUT-v1.7.0_gg1.md` (item 8, 6 insertions and 2 deletions). No tag was pushed.
- **Pre-push check:** before the push, the E-16 script (`/tmp/gsd16-wr01/demo2.sh`) ran against `be6b8861` itself, starting from a stale branch at `f328fd21`. The fast-forward reached `be6b8861`, the rollback commit's tree equals `pre-v1.7.0-sync` (`ok`), `git merge <merge-commit>^2` printed "Already up to date.", and `git revert <rollback-commit>` restored the `be6b8861` tree.
- **Push:** `537beb1f..be6b8861  custom_build -> custom_build`.
- **CI:** one `tests.yaml` run on `be6b8861` (id 38156040416, push event). It started at 16:34:36Z and finished at 16:40:59Z with conclusion `success`. Its job "ClickHouse latest Client version V2 tests" succeeded.

Item 8 on `origin/custom_build` now holds the corrected rollback procedure, so the open human item from this report is resolved.
