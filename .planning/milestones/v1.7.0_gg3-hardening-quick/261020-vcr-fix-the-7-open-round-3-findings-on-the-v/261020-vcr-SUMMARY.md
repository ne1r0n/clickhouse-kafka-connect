---
phase: quick-261020-vcr
plan: 01
subsystem: docs
tags: [rollout-note, changelog, fork-delta, review-disposition, phase-24]
requires: [24-REVIEW round 3]
provides: ["seven round 3 findings fixed on sync/v1.7.0", "24-REVIEW-DISPOSITION open: 0"]
affects: [docs/ROLLOUT-v1.7.0_gg3.md, CHANGELOG.md, docs/FORK-DELTA.md]
key-files:
  modified:
    - docs/ROLLOUT-v1.7.0_gg3.md
    - CHANGELOG.md
    - docs/FORK-DELTA.md
    - .planning/phases/24-release-publication/24-REVIEW-DISPOSITION.md
    - .planning/phases/24-release-publication/24-MILESTONE-CLOSE-RECORD.md
requirements-completed: [ROLL-03]
status: complete
completed: 2026-10-20
commits: 9
plan_head_before: f4573ce6f509b14ab7453d2cadbfe5ec92ab0232
plan_head_after: 874fb6923a3f4ceb05b0ecac2511853576cfa650
actuals:
  tokens: 8000
  tasks: 3
  commits: 9
---

# Quick 261020-vcr: Round 3 fixes on the v1.7.0_gg3 rollout note Summary

Seven open round 3 findings (WR-03..WR-07, IN-04, IN-05) fixed as docs-only commits on local `sync/v1.7.0`, proven with the Phase 24 document checks, the rollback demo on a dry-run merge into `3376a18f`, the anchors gate and `testUnit`. Nothing was pushed or tagged.

## Commits (all on sync/v1.7.0, none pushed)

| SHA | Finding | Files |
|-----|---------|-------|
| 40699434 | WR-06 item 7 works through item 4 before the branch commit; item 4 says "do not redeploy v1.7.0_gg2 yet" | note |
| 7987a093 | WR-03 item 3 describes the 30 identical tables by type content | note |
| 6df28936 | IN-04 line break named in the known-limitation lead sentence | note |
| 12e9790d | WR-04 size-element failure stated for the four inserted shapes (released CHANGELOG reason in body) | note, CHANGELOG.md, FORK-DELTA.md |
| 83cbaf94 | WR-05 exactlyOnce=false connectors told to stop cleanly | note |
| 72773c7c | WR-07 one entry per topic, database named, Empty result speaks of listed topics (SQL unchanged) | note |
| 1ba3fe96 | IN-05 CHANGELOG Build and CI bullet names ClickHouseHelperClient (released CHANGELOG reason in body) | CHANGELOG.md |
| bbfb0a86 | Revision notice linking items 2, 3, 4 and 7 | note |
| 874fb692 | Disposition rows flipped to fixed, `open: 0`; close record audit sentence | 2 `.planning` files |

Gate tip T (last docs commit) = `bbfb0a8647f8b1ccda304e0ecbe2e08c3281da30`. D' (throwaway `--no-ff` merge of T into `3376a18f`, never pushed) = `2e59ea3aadb668649cbff3c3533c648df8a58990`. Task 1 intermediate D' was `2b927283...` (tip `40699434`).

## Verification results (exit 0 for every chain)

- Task 1: TRACER-OK. Markers: ROLLOUT-FRAME-OK, ITEMS-1-7-OK (post-publication copy), ITEMS-2-3-OK, ROLLOUT-T3-OK, ROLLOUT-FINAL-OK, DOCS-FROZEN-OK, `consistency failures: 0`, MERGE-DRY-RUN-OK, `item-7 order, item-4 check before read-tree: yes`, ROLLBACK-ON-DRY-MERGE-OK.
- Task 2: PRE-PUBLICATION-GATE-OK on T. Findings gate all True (WR-03..IN-05, NOTICE, BLOCKS; changed items 2, 3, 4, 7; notice names 2, 3, 4, 7). Markers: ROLLOUT-FRAME-OK, ITEMS-1-7-OK, ITEMS-2-3-OK, ROLLOUT-T3-OK, ROLLOUT-FINAL-OK, CHANGELOG-GG3-OK, FORK-DELTA-ROWS-OK, FORK-DELTA-PATHS-OK, RAW-NUMBERS-GG3-OK, DOCS-FROZEN-OK, SCOPE-OK (only the three docs differ from `3376a18f` outside `.planning`), `destructive-or-secret []`, MERGE-DRY-RUN-OK, ROLLBACK-ON-DRY-MERGE-OK (7 operator docs, TRUNCATE scan clean).
- Anchors gate on T: `invariants: provenance_paths=21 anchor_commits=537 anchor_rows=69 marker_commits=584 anchor_exempt=1 result=PASS` (exit 0).
- `testUnit --rerun` on T: BUILD SUCCESSFUL, exit 0 (no OpenJ9 crash), classes 49, tests 838, skipped 2, failures 0, errors 0.
- Task 3: disposition gate `bad [] changed-old-rows [] ids 12 headings 12 all-open0 True audit-updated True`; only the two `.planning` files changed since T; NO-PUBLISH-OK and BOOKKEEPING-OK (origin `custom_build` still `3376a18f`, no `sync/v1.7.0` head on origin, `pre-v1.7.0_gg3` at `6473e24f`, `v1.7.0_gg3` at `3376a18f`, 77 local tags, no origin ref at HEAD).

## Deviations from Plan

**1. [Rule 3 - Blocking] Findings gate WR-04 check is case-sensitive but the note's text is "This release has no fix"**
- **Found during:** Task 2, WR-04
- **Issue:** the plan's `findings-gate.py` tests `has(i2, "this release has no fix")` on un-lowered text, while the original sentence (kept verbatim, as the plan requires) starts "This release has no fix for the defect". The plan's own `ITEMS-2-3-OK` check lowercases first and passes.
- **Fix:** changed that one clause in the scratch copy `/tmp/gsd-vcr/findings-gate.py` to `has(i2.lower(), ...)`. The note text was not altered to suit the gate. The orchestrator's re-run of `/tmp/gsd-vcr/findings-gate.py` uses the corrected file.

**2. [Rule 1 - plan wording] WR-05 "You are affected if ..." sentence**: dropped the `exactlyOnce=true` limit from the opening sentence of item 4 "Am I affected?" (the finding says the sentence no longer limits the hazard); the `exactlyOnce=true` procedure intro in "What to do" is unchanged.

Otherwise the plan was executed as written. Evidence-versus-Fix narrowings held: no evidence or finding IDs entered the note (WR-05 as prose), no fenced block changed (WR-07 prose only), FORK-DELTA uses the has/have-not-been form ("have not been measured"; no "was not measured").

## Same-claim edits beyond the three named WR-04 places (as the plan requires)

- Item 2 log-grep "Non-empty result" now says the table has the defect described above.
- Item 2 "What to do" now says the inserts that were run failed on the measured route (default route, client V2), keeping "whatever the connector settings" and "This release has no fix".
- Item 7 "Rolling back brings back" size/size0 bullet: inserts behave as before (inserted shapes failed with code 32 or 33).

## Residuals for the round 4 review

- FORK-DELTA `shouldSkipDescribeRow` row and the declared-nullability row still carry short parentheticals mentioning codes 32 and 33 (outside the item 10 scope of this task).
- `docs/ROLLOUT-v1.7.0_gg2.md` item 1 has the same inherited state-query gap as WR-07 (two topic slots, no database); gg1 and gg2 notes are protected by `ROLLOUT-FINAL-OK`.
- CHANGELOG has pre-existing lines over 120 characters in the 1.7.0_gg2 section (untouched).

## Known Stubs

None.

## Threat Flags

None. No new endpoint, auth path or schema; docs and `.planning` files only.

## Next

Orchestrator: round 4 independent review, approval gate, `--no-ff` merge into `custom_build` and push, per the plan's section "Orchestrator: review and publication". The executor created no tag, pushed nothing and left local `custom_build` untouched. Untracked `.planning/v1.7.0_gg3-hardening-MILESTONE-AUDIT.md` was left alone. SUMMARY, STATE and PLAN are not committed.

## Self-Check: PASSED

All nine commits are ancestors of HEAD (`git rev-list --count f4573ce6..HEAD` = 9); the five modified files exist; the gate scripts under `/tmp/gsd-vcr/` exist.

## Orchestrator addendum: review rounds 4 to 6 and publication (2026-10-20)

**Review rounds, independent `gsd-code-reviewer` passes, appended to `24-REVIEW.md`:**
- **Round 4** found 3 Warnings and 2 Info, each fixed in its own commit: WR-08 `ee7a0db5`, WR-09 `cb648804`, WR-10 `77e76700`, IN-06 `0f2bb4e5`, IN-07 `68cab778`.
- **WR-10 deviates from the reviewer's Fix text.** The review asked for `'<db><dbTopicSplitChar><topic>'` "instead of" the plain topic, which is also the gg1 note's wording. `RecordConvertor.convert` keys a topic that the split character divides into exactly two parts under its own name, so that wording misses the key of a normal two-part topic. Step 2 now lists both forms. Round 5 checked the rule with `String.split` and confirmed it.
- **Round 5** found 1 Warning and 1 Info: WR-11 `133da776` and IN-08 `f4a51a29`. IN-08 also covers the trailing-split-character case, because `String.split` drops trailing empty parts.
- **Round 6** found nothing new.
- **Dispositions:** `24-REVIEW-DISPOSITION.md` has 19 rows, `open: 0` (`c7db8d03`, `68faf6d8`). Round 6 is recorded in `a403cc1d`.

**Gates on S = `a403cc1d`, the merged tip; last docs commit `f4a51a29`:**
- The fast document checks (`/tmp/gsd-vcr/fast.sh`, rc=0) cover all ten Phase 24 checks, `consistency.py` (0 failures) and the findings gate.
- `disposition-gate-r5.py` reports 19 IDs, 19 headings and all files at open 0.
- `verify-merge-invariants.sh 54e63648 HEAD` gives `anchor_commits=548 anchor_rows=69 anchor_exempt=1 result=PASS`.
- The rollback demo passes on dry-run merge `9afe7d0d`.
- `testUnit` was not re-run, because no path outside `docs/` and `.planning/` changed after `bbfb0a86` (838 tests, 0 failures there).

**Publication:**
- **Approval:** the user answered `publish` directly at the gate.
- **Merge:** M = `6a2a87a5`, a `--no-ff` merge with parents `3376a18f` and S, made in the worktree `/tmp/gsd-vcr/cb` (since removed).
  - tree(M) equals tree(S);
  - outside `.planning`, only `CHANGELOG.md`, `docs/FORK-DELTA.md` and `docs/ROLLOUT-v1.7.0_gg3.md` changed;
  - the TRUNCATE scan is clean, and the rollback demo passes on M itself.
- **Push:** `3376a18f..6a2a87a5 custom_build -> custom_build` at 2026-10-20T20:58:05Z. No force and no tag.
- **CI:** exactly one `tests.yaml` run on M, [39496482074](https://github.com/ne1r0n/clickhouse-kafka-connect/actions/runs/39496482074): event push, `custom_build`, success, 20:58:09Z to 21:05:10Z. "Run tests" took 4m51s; the release run 39472696952 took about 5 minutes.
- **Refs after green:** origin tags are byte-equal to the pre-push snapshot. `v1.7.0_gg3` is `3376a18f` and `pre-v1.7.0_gg3` is `6473e24f`. The only changed head is `custom_build`, there are 77 local tags, and `custom_build...sync/v1.7.0` reads `2 0`.

**Left as is:** `docs/ROLLOUT-v1.7.0_gg1.md` and `ROLLOUT-v1.7.0_gg2.md` (protected, published) keep the shorter `enableDbTopicSplit` key rule and the two-slot query. The gg3 note now tells the reader to follow its own step 2.
