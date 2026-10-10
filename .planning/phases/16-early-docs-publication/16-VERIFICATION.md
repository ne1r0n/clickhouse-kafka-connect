---
phase: 16-early-docs-publication
verified: 2026-10-10T19:10:00Z
status: human_needed
score: 5/5 must-haves verified
covered_files:
  - ".planning/phases/16-early-docs-publication/16-01-PLAN.md"
  - ".planning/phases/16-early-docs-publication/16-01-SUMMARY.md"
  - ".planning/phases/16-early-docs-publication/16-02-PLAN.md"
  - ".planning/phases/16-early-docs-publication/16-02-SUMMARY.md"
  - ".planning/phases/16-early-docs-publication/16-03-PLAN.md"
  - ".planning/phases/16-early-docs-publication/16-03-SUMMARY.md"
  - "CHANGELOG.md"
  - "docs/FORK-DELTA.md"
  - "docs/ROLLOUT-v1.7.0_gg1.md"
covered_digest: "v3:sha256:e20adfc20c42aa6d79c38be7e38e5a0d9a06eec5179bd8b175eb3266e29de4fb"
behavior_unverified: 0
overrides_applied: 0
human_verification:
  - test: "Decide what to do with open review finding WR-01 (rollout item 8 step 1 conflicts on the published custom_build tip)"
    expected: "One of: (a) fix item 8 step 1 in docs, re-run the item 8 evidence at 537beb1f, review it, and publish through a NEW user-approved docs-only merge; (b) record WR-01 as `deferred` in 16-REVIEW-DISPOSITION.md with a reason, accepting that the published rollback text is wrong until a later publication; (c) fold the fix into Phase 20's publication. Phase 20's ROLL-02 covers a NEW file (ROLLOUT-v1.7.0_gg2.md) and does not name item 8 of the gg1 doc, so (c) needs an explicit plan item."
    why_human: "Fixing it changes a published doc, so it needs a new publication and the user's own approval (16-03-PLAN execution rules). Whether an incorrect rollback step is acceptable to leave on origin is a risk call, not something code inspection can decide."
---

# Phase 16: Early Docs Publication Verification Report

**Phase Goal:** An operator reading `origin/custom_build` gets a rollout doc that can no longer erase other connectors' exactly-once state, with the 15-REVIEW docs findings fixed. This happens before any code work starts and ships no code.
**Verified:** 2026-10-10T19:10:00Z
**Status:** human_needed
**Re-verification:** No, initial verification

## Verdict

The phase goal is achieved, and all five ROADMAP success criteria hold against the repository and against `origin`. I checked the publication facts and the doc text myself instead of reading them from the SUMMARY.

One reproduced defect is still open: WR-01 from the post-execution code review. Item 8 step 1 of the published rollout doc (`git revert -m 1 <merge-commit>`) does not complete on the published `custom_build` tip. It is not a ROADMAP success criterion and not a 15-REVIEW finding. I report it as a warning that needs a user decision, and I do not hide it inside a clean `passed`.

## Goal Achievement

### Observable Truths (ROADMAP success criteria)

| # | Truth | Status | Evidence |
|---|-------|--------|----------|
| 1 | Rollout doc, FORK-DELTA and CHANGELOG read correctly; every quoted command was run with matching output; item 2 qualified by `exactlyOnce`; item 8 says going forward means reverting the revert; FORK-DELTA greps print a total; plain language, MBean change and lz4 reason named | VERIFIED (with the WR-01 caveat below) | See sub-rows. |
| 1a | Item 2 says which step applies only with `exactlyOnce=true` | VERIFIED | `git show 537beb1f:docs/ROLLOUT-v1.7.0_gg1.md`, "What to do" of Item 2: "Step 1 always; step 2 only for connectors with `exactlyOnce=true`." The step 2 text keeps the `AFTER_PROCESSING` replay reason. |
| 1b | Item 8 states that going forward after `git revert -m 1` means reverting the revert | VERIFIED | Item 8: "To go forward again after `git revert -m 1`, revert the revert commit first (`git revert <revert-commit>`); merging the released history again (`git merge <merge-commit>^2`) without that brings nothing back." The IN-01 fix `fd99a476` is an ancestor of the published `1e0eefde`. |
| 1c | Every FORK-DELTA grep command prints a total | VERIFIED | FORK-DELTA lines 154-155 are `grep -rho ... \| wc -l`. I ran both on the tree: they print `7` and `1`, matching "7 annotation sites" and "1 site". `src/` is identical between `537beb1f` and HEAD. |
| 1d | Plain language instead of bare planning IDs; MBean `version=`/client-name change and lz4 advisory reason named | VERIFIED | Remaining `L-nn` tokens in the rollout are inside `(ledger L-nn)` tags with a one-time legend and link (line 13). The one `backlog 999.1` is inside the verbatim startup-WARN quote. CHANGELOG 1.7.0_gg1 names `version=` `v1.6.0_gg1` -> `v1.7.0_gg1`, client name `clickhouse-kafka-connect/v1.7.0_gg1`, and lz4 `1.10.2` -> `1.12.0` with advisory floor `1.11.4` (`1.11.2`, `1.11.3` affected). No TBD/FIXME/XXX/TODO in added doc lines. |
| 1e | Every quoted command was run with output matching the text | VERIFIED | `16-COMMAND-EVIDENCE.md` E-01..E-14, all "Matches text: yes" or "n/a (gate check)". Caveat: E-06 and E-14 ran at `f328fd21`, not at the published tip. See WR-01. |
| 2 | Code review of the changed docs recorded, every finding dispositioned, before the merge approval was asked | VERIFIED | Pre-gate review `9207ec01` (20:48:01), IN-01 fix `fd99a476`, dispositions `427a2f9d` (20:48:43), approval bundle `1e0eefde` (20:52:58): all before the 21:08 record of the publication. IN-01 is `fixed`. 15-REVIEW IN-07..IN-10 are `fixed` in `15-REVIEW-DISPOSITION.md` (lines 76-79). WR-01 was raised only by the later post-execution review (`28bcc7b1`, `b93ae9ca`), so it is not a pre-approval finding. It is still `open`. |
| 3 | No text in the merged tree tells an operator to `TRUNCATE` the shared KeeperMap state table | VERIFIED | `git grep -i truncate 537beb1f -- . ':(exclude).planning'` in docs and CHANGELOG: only `ROLLOUT:111` "Do not `TRUNCATE` the table. It is shared by every `exactlyOnce` connector ..." (the negative) and FORK-DELTA:126 (a Debezium truncate event record type). Other hits are source, test and benchmark code. CHANGELOG says "a KeeperMap state clear". Item 2's SQL uses topic-scoped `DELETE`. |
| 4 | `origin/custom_build` is a non-force two-parent `--no-ff` merge of `sync/v1.7.0`, after the user's own approval | VERIFIED (approval provenance attested, not inspectable) | See "Publication checks" below. |
| 5 | The merge SHA has exactly one `tests.yaml` run, green, no cancelled sibling | VERIFIED | Unauthenticated GitHub API `actions/runs?head_sha=537beb1f...`: `total_count: 1`; run `38074050935`, path `.github/workflows/tests.yaml`, event `push`, `head_branch` `custom_build`, `status` completed, `conclusion` success, 18:00:42Z to 18:07:11Z. Re-queried about an hour after the push, so the cardinality held long after the 120 s window. |

**Score:** 5/5 truths verified, 0 present-but-behavior-unverified. One open warning (WR-01) routes the status to `human_needed`.

### Publication checks (criterion 4, run by me)

| Check | Command | Result |
|-------|---------|--------|
| origin head | `git ls-remote origin` | `refs/heads/custom_build` = `537beb1fcf23b987c8b340006f4f74f7c5229d00` |
| Two parents | `git rev-list --parents -n 1 537beb1f` | `f328fd218dce5b9c652f8872042e922f9686b87f` `1e0eefdefbe253e1c028cf0ff369ed8f85b33a59` (M^1 = `f328fd21`, M^2 = `1e0eefde`) |
| Non-force | `git merge-base --is-ancestor f328fd21 537beb1f` | true: the old origin tip is an ancestor, so the push moved the ref forward only |
| Tree equals merged tip | `git diff --quiet 537beb1f 537beb1f^2` | exit 0 (trees equal). `1e0eefde` is an ancestor of `sync/v1.7.0`. |
| Allow-list | `git diff --name-only f328fd21 537beb1f` | 125 under `.planning/`, 2 under `docs/`, `CHANGELOG.md`; 0 outside the allow-list |
| No `.gsd-ws-arg` | `git ls-tree -r --name-only 537beb1f \| grep -c .gsd-ws-arg` | 0 |
| No uncommitted `state.json` edit | tree(M) == tree(X) with X a committed tip, merged by hash from a clean worktree (SUMMARY: status empty before and after) | The uncommitted `state.json` modification now in the main tree is local and not in M |
| Only `custom_build`, no tag | `git ls-remote origin` against `/tmp/gsd16/origin-*-before.txt` | Heads: `custom_build`, `main` (`main` unchanged at `bec167d8`). Tags: exactly `pre-v1.6.0-sync` (`6ac2d20c`), `pre-v1.7.0-sync` and `v1.6.0_gg1` (`c61d90ce`), `v1.7.0_gg1` (`f328fd21`). `sync/v1.7.0` is not on origin. |
| Merge message | `git log -1 --format=%B 537beb1f` | Names the three docs and `git revert -m 1 <this commit>` |

The user's direct "publish" is recorded in `16-03-SUMMARY.md` and stated by the orchestrator. A conversation is not something I can inspect from the repository, so I accept it as attested and do not claim to have independently confirmed it.

### Required Artifacts

| Artifact | Expected | Status | Details |
|----------|----------|--------|---------|
| `docs/ROLLOUT-v1.7.0_gg1.md` at `537beb1f` | Corrected rollout doc (IN-07, IN-08, IN-10; WR-01..03 of the Phase 15 review kept) | VERIFIED | Items 2 and 8 read as required; topic-scoped DELETE examples and the "Do not TRUNCATE" paragraph present. |
| `docs/FORK-DELTA.md` at `537beb1f` | Greps print totals (IN-09) | VERIFIED | Lines 154-155; outputs 7 and 1 reproduced. |
| `CHANGELOG.md` at `537beb1f` | In-place correction of the 1.7.0_gg1 section (IN-10) | VERIFIED | MBean/client-name and lz4 text present. |
| `16-COMMAND-EVIDENCE.md` | Evidence for each quoted command | VERIFIED | E-01..E-14 present. |
| `16-REVIEW.md`, `16-REVIEW-DISPOSITION.md` | Review recorded and dispositioned | VERIFIED, with WR-01 `open` | |

Plans 16-01/16-02/16-03 declare no `must_haves.artifacts` entries that `verify.artifacts` could score mechanically beyond the above, so Step 4 was done by direct inspection of the files at the published SHA.

### Key Link Verification

| From | To | Via | Status | Details |
|------|----|-----|--------|---------|
| `custom_build` merge M | `sync/v1.7.0` tip X | `--no-ff`, M^2 = X, equal trees | WIRED | Verified above. |
| `tests.yaml` (branch-only trigger) | single CI run | push to `custom_build`, no tag push | WIRED | One run for the SHA. |
| Merge message | rollback | names `git revert -m 1 <this commit>` | WIRED, but see WR-01 | The text exists. The step it points to as the one-command rollback of the release does not complete on this tip. |

### Data-Flow Trace (Level 4)

Not applicable. The phase changes only Markdown and planning files.

### Behavioral Spot-Checks

| Behavior | Command | Result | Status |
|----------|---------|--------|--------|
| FORK-DELTA `@Disabled` total | `grep -rho '@Disabled' src/test/java \| wc -l` | 7 | PASS |
| FORK-DELTA `assumeTrue` total | `grep -rho 'Assumptions.assumeTrue' src/test/java \| wc -l` | 1 | PASS |
| Source untouched since the publication | `git diff --quiet 537beb1f HEAD -- src` | exit 0 | PASS |
| Item 8 step 1 on the published tip | `git merge-tree --write-tree --merge-base=f328fd21 537beb1f f328fd21^1` (equivalent of `git revert -m 1 f328fd21` on `537beb1f`) | exit 1, 97 conflicts, including `CHANGELOG.md`, `docs/FORK-DELTA.md`, modify/delete on `docs/ROLLOUT-v1.7.0_gg1.md` | FAIL (see WR-01) |
| Same step on `f328fd21` (the state E-06 tested) | same, with ours = `f328fd21` | exit 0, clean | PASS |
| Reverting the publication merge first, then the release merge | `merge-tree` simulation of each step | both exit 0 | PASS (reproduces the orchestrator's workaround) |

I used `git merge-tree`, which writes no refs and touches no worktree, so the working tree is as I found it.

The orchestrator's local gate (`./gradlew assemble` and `test` exit 0) was UP-TO-DATE for the test task, so it adds no new evidence. It is not needed. Source is byte-identical to `f328fd21` outside the allow-list, and the real proof is the green CI run on `537beb1f`.

### Probe Execution

Step 7c: SKIPPED. The phase declares no probes (`scripts/*/tests/probe-*.sh`), and `verify-merge-invariants.sh` is deliberately not this merge's gate (ROADMAP note).

### Requirements Coverage

| Requirement | Source Plan | Description | Status | Evidence |
|-------------|-------------|-------------|--------|----------|
| PUB-05 | 16-01 (and 16-02 for the review) | IN-07..IN-10 fixed in the three docs; changed docs reviewed before the merge gate | SATISFIED | Criteria 1a-1e and 2 above. `15-REVIEW-DISPOSITION.md` IN-07..IN-10 `fixed`. |
| PUB-06 | 16-03 | Docs-only non-force `--no-ff` merge after the user's approval, diff limited to `.planning/`, `docs/`, `CHANGELOG.md`; one branch, no tag, one green `tests.yaml` run | SATISFIED | Criteria 3, 4, 5 and the publication table above. |

Both IDs appear in the plan frontmatter (`requirements-completed: [PUB-06]` in 16-03; PUB-05 in 16-01/16-02), in REQUIREMENTS.md (lines 13-14, marked complete) and in the traceability table (lines 76-77). No ORPHANED requirement: REQUIREMENTS.md maps only PUB-05 and PUB-06 to Phase 16.

### Anti-Patterns Found

| File | Line | Pattern | Severity | Impact |
|------|------|---------|----------|--------|
| `docs/ROLLOUT-v1.7.0_gg1.md` | 318 | Rollback command that conflicts on the published tip (WR-01) | Warning | An operator following item 8 during an incident gets a stopped revert with 97 conflicts instead of one commit. |

No TBD/FIXME/XXX/TODO debt markers in the lines this phase added.

### Human Verification Required

#### 1. Disposition of WR-01 (rollout item 8 step 1 on the published tip)

**Test:** On `origin/custom_build` (`537beb1f`), follow item 8 step 1: `git revert -m 1 <merge-commit>` with `<merge-commit>` = `f328fd21`.
**Expected by the text:** one revert commit that backs out the whole release, after which `git revert <revert-commit>` goes forward again.
**Actual (reproduced by me via `git merge-tree` and by the orchestrator in a worktree):** the revert stops with 97 conflicts: `CHANGELOG.md`, `docs/FORK-DELTA.md`, and a modify/delete on `docs/ROLLOUT-v1.7.0_gg1.md`, plus many `.planning/` files. The sequence that works is `git revert -m 1 537beb1f` first, then `git revert -m 1 f328fd21`, which leaves the pre-v1.7.0-sync tree.
**Why human:** Fixing it means a doc change plus a new publication, and both need the user's own approval. Leaving it means an incorrect rollback step stays on origin, which is a risk call.

How I weighed it against the phase goal and the criteria:

- **Goal:** achieved. The goal is a rollout doc that cannot erase other connectors' state, with the 15-REVIEW findings fixed. Both hold. WR-01 is in a step Phase 15 wrote and Phase 16 did not change. The publication merge made it reachable, because it put the docs-only merge on top of `f328fd21`.
- **Criterion 1:** each of its four enumerated bullets is met. The umbrella "every command quoted ... matching the text" was proven for item 8 on `f328fd21` (E-06, E-14). It was not proven at the published tip, where the quoted `git revert -m 1` behaves differently. I count this as a warning on criterion 1, not a failure. The criterion is anchored to `sync/v1.7.0`, and the go-forward sentence this phase added is accurate once a revert has completed.
- **Criterion 2:** met as written. The review that was recorded and dispositioned before approval had one finding (IN-01, fixed). WR-01 came from the post-execution review, so no pre-approval finding was left open. `16-REVIEW-DISPOSITION.md` does still show WR-01 as `open`, and nothing downstream will close it unless someone does.
- **Deferral (Step 9b):** WR-01 is not covered by any later phase. Phase 20 ROLL-02 is a new `ROLLOUT-v1.7.0_gg2.md` and does not name item 8 of the gg1 doc, so I did not move WR-01 to `deferred`.
- **Phase rules:** `16-03-PLAN.md` says a docs change called for after the publication needs a new user-approved publication and is never an edit to `custom_build`. I made no edit, push or commit.

Suggested fix text, from the reviewer: give step 1 a path that works on the published tip (revert the publication merge `537beb1f` first, then `f328fd21`; or `git revert --no-commit -m 1 <merge-commit>`, `git read-tree -u --reset pre-v1.7.0-sync`, `git commit`), and re-run E-06/E-14 at `537beb1f`.

### Gaps Summary

No gaps against the five success criteria. The phase delivered what it set out to: the corrected docs are on `origin/custom_build` as a single revertible `--no-ff` merge with one green CI run and no tag, and the TRUNCATE instruction is gone. The only open item is WR-01, a reproduced rollback-text defect that needs the user to choose between a new publication, an explicit deferral, or folding the fix into Phase 20. I did not edit docs, push, commit, or touch `.planning/state.json` and `.planning/milestone.lock`.

---

_Verified: 2026-10-10T19:10:00Z_
_Verifier: Claude (gsd-verifier)_
