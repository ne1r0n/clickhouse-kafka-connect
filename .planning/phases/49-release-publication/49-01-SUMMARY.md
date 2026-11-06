---
phase: 49-release-publication
plan: "01"
subsystem: release
tags: [release, version-bump, changelog, fork-delta, tree-neutral-merge, evidence]
status: complete

requires:
  - phase: 48-phase-37-dispositions-document-fixes
    provides: the gate tree (testUnit 2325 measured), the item-14 wording, the hand-off inventory
provides:
  - sync/v1.7.0 contains the published gg7 merge (94834763) without a tree change; the local rollback tag pre-v1.7.0_gg8 names 94834763
  - VERSION v1.7.0_gg8 with its fork-anchors.txt row, one commit
  - the completed 1.7.0_gg8 CHANGELOG section under the Unreleased heading
  - docs/FORK-DELTA.md final for gg8 (after-gg8 raw numbers, totals sentence, recount, gg8 paragraphs)
  - 49-COMMAND-EVIDENCE.md with the index E-01..E-20 and entries E-01..E-04
affects: [49-02, 49-03, 49-04, 49-05]

actuals:
  tokens: 11255
  tasks: 3
  commits: 9
plan_head_before: 816d3b554045c5e1368b5e5090d8674695596cb4
plan_head_after: 32d31ec7abf5e6bf06ed494a3b5fd8a85bb8a42f

tech-stack:
  added: []
  patterns:
    - "tree-neutral --no-ff merge of the published tip to make the release tag an ancestor of the release branch"
    - "evidence entries opened before documents and flipped only after a verify passes"

key-files:
  created:
    - .planning/phases/49-release-publication/49-COMMAND-EVIDENCE.md
  modified:
    - VERSION
    - .planning/scripts/fork-anchors.txt
    - CHANGELOG.md
    - docs/FORK-DELTA.md

key-decisions:
  - "U-1 and U-2 carried out exactly as the user recorded them: one local merge as the first change, one local retag of pre-v1.7.0_gg8, nothing pushed"
  - "FORK-DELTA test-tree row states what the recount found: 115 of 143 byte-identical, 28 differ, split into fork adaptation, upstream-tail files and mixed files, not all fork adaptation"
  - "the CHANGELOG heading stays '# 1.7.0_gg8, Unreleased' (PL-5); the UTC date is set later by the one-line commit of U-3"

requirements-completed: [RLS-01, RLS-04]

coverage:
  - id: D1
    description: "sync/v1.7.0 contains the published gg7 merge tree-neutrally, pre-v1.7.0_gg8 names 94834763, the baseline pipe picks v1.7.0_gg7, the tail gate on the merge reads PASS"
    requirement: "RLS-01"
    verification:
      - kind: other
        ref: "bash /tmp/gsd49/scripts/verify-t1.sh (the task 1 automated block of 49-01-PLAN.md): P49-01-T1-OK"
        status: pass
    human_judgment: false
  - id: D2
    description: "VERSION v1.7.0_gg8 and its anchors row in one commit; the generated constant, the baseline tag, the anchors mirror and the four version guard classes agree"
    requirement: "RLS-01"
    verification:
      - kind: unit
        ref: "BumpVersionScriptUnitTest, BaselineTagScriptUnitTest, MBeanNameUnitTest, ClientEndpointAndOptionsPinUnitTest (87 tests, Docker unreachable)"
        status: pass
    human_judgment: false
  - id: D3
    description: "the 1.7.0_gg8 CHANGELOG section completed (five headings, the Phase 47 bullets word for word, released sections byte-identical, no planning identifier)"
    requirement: "RLS-01"
    verification:
      - kind: other
        ref: "task 2 automated block of 49-01-PLAN.md: P49-01-T2-OK"
        status: pass
    human_judgment: true
    rationale: "the structure is checked mechanically; the accuracy of each claim is judged by the fresh-context review of plan 49-03"
  - id: D4
    description: "docs/FORK-DELTA.md final: after-gg8 rows equal a fresh shortstat, gate totals sentence from the measured runs, recounts, gg8 paragraphs"
    requirement: "RLS-01, RLS-04"
    verification:
      - kind: other
        ref: "task 3 automated block of 49-01-PLAN.md: P49-01-T3-OK (testUnit 2325 and suite 2928 from JUnit XML)"
        status: pass
    human_judgment: true
    rationale: "the figures are checked mechanically; the prose accuracy (test-tree classification, gg8 paragraph) is judged by the fresh-context review of plan 49-03"
---

# Phase 49 Plan 01: Release identity, CHANGELOG and FORK-DELTA for v1.7.0_gg8 Summary

`sync/v1.7.0` now holds the published gg7 merge by a tree-neutral merge, the local rollback tag names `94834763`, `VERSION` reads `v1.7.0_gg8` through `bump_version.py`, the CHANGELOG section is complete under its Unreleased heading, FORK-DELTA is final, and the measured totals (testUnit 2325, suite 2928, anchors 218) are on the record. Nothing was pushed.

Start 2026-11-06T12:52:09Z, end about 13:21Z, about 29 minutes, 3 tasks, 5 files changed outside the evidence (`VERSION`, `fork-anchors.txt`, `CHANGELOG.md`, `docs/FORK-DELTA.md`, plus the new evidence file).

## Accomplishments

- Task 1 (tracer): pre-flight on the untouched tree, the U-2 merge `77ea9c8c` (parents `816d3b55` and `94834763`, tree equal to the first parent), the U-1 retag (`baa404cd` to `94834763`, lightweight, local), the tail gate on the merge, then the bump commit `b6666fe7` holding exactly `VERSION` and `fork-anchors.txt`.
- Task 2: the `1.7.0_gg8` CHANGELOG section completed (commit `d2240a70`), the Phase 47 bullets unchanged, every released section byte-identical, `ids: hits=0`.
- Task 3: testUnit and one full suite measured on the bumped tree, then FORK-DELTA final (commit `7fed6067`).

## Facts

| Fact | Value |
|------|-------|
| `local_head` (L0) | `816d3b554045c5e1368b5e5090d8674695596cb4` |
| `merge_commit` (B1) | `77ea9c8ccb1ff7b6a7f04fa06c13a515174dda1f`, parents `816d3b55` `94834763`, tree equal to L0 |
| retag | `pre-v1.7.0_gg8` from `baa404cd5ed95c556db0acff84def1024c3bafe3` to `9483476322e0d331e81c5b92ac266a18f0f01450`, lightweight, local only, absent on origin |
| left count before / after | `1` (the gg7 merge) / `0` (`left_right_after` `0 224`) |
| baseline pipe | `v1.7.0_gg6` before the merge; `v1.7.0_gg7` after the merge and after the bump |
| tail gate on B1 | `invariants: provenance_paths=11 anchor_commits=206 anchor_rows=218 marker_commits=224 anchor_exempt=0 result=PASS`, exit 0, run alone |
| bump commit | `b6666fe748629ea597a7534fba0d9a2e6ef54cee`, files `.planning/scripts/fork-anchors.txt; VERSION`, each 1/1 |
| generated constant | `ARTIFACT_VERSION = "v1.7.0_gg8";` after `clean generateVersionFile` |
| version guards (Docker unreachable) | `BumpVersionScriptUnitTest` 52, `BaselineTagScriptUnitTest` 19, `MBeanNameUnitTest` 11, `ClientEndpointAndOptionsPinUnitTest` 5, all green |
| CHANGELOG commit | `d2240a706587134fb24d050fa613002a9d093fe2` |
| FORK-DELTA commit | `7fed6067f1fe97880d7bc88d00efac4aab00b034` |
| evidence commits | E-01 `f85e8f3e`, E-02 `7f92af5b`, E-03 `a0438464`, E-04 `32d31ec7` |
| testUnit, Docker unreachable (E-04) | 2325 tests, 0 failures, 0 errors, 2 skipped, 107 XML files, 38 s (equals Phase 48's 2325) |
| full suite, ClickHouse `latest` x client V2 (E-04) | 2928 tests, 0 failures, 0 errors, 18 skipped (`OK skips: named=7 state_mismatch_cases=11 unexpected=0`), 156 XML files, 3 min 25 s, default toolchain, no JVM crash (equals the derived 2923 + 5) |
| server | `SELECT version()` `26.10.3.7`, digest `sha256:739e099abd1157390960a59e4532d21781d8fde57e6e4151cb920ea02c30b1ce`, same before and after the suite and equal to the research value |
| `wider_now` / `narrow_now` | `364 47983 1260` / `43 3578 333` |
| `wider_since_gg7` / `narrow_since_gg7` | `36 5602 97` / `6 428 72` |
| `by_dir_since_gg7` | `src/test=28 5172 25; src/integrationTest=0 0 0; src/testFixtures=0 0 0; .github=0 0 0; gradle=1 1 0; build.gradle.kts=1 1 0` |
| `src/main` files changed since gg7 | `ClickHouseHelperClient.java; ClickHouseSinkConfig.java; ClickHouseSinkTask.java; ClickHouseWriter.java; DeclaredNullability.java; Utils.java` |
| recounts of `src/main` | `30 14 19 10` at `HEAD` and at `v1.7.0_gg7` (identical, import-only, modified, new) |
| test-tree recount | 143 files, 115 byte-identical, 28 differ (category counts `2; 5; 2; 2; 1; 1; 1; 8; 6`) |
| anchors mirror | `rows=218 bad=0` after every task |
| upstream recheck (E-01) | `main` `83808df9a0993cf49b2ceaa4633c782e550c0180`; newest tag `v1.7.0`, none past it; PRs 861 `open b62e0ae4`, 855 `open ddabf28b`, 854 `open 69d1efc5`, 788 `closed False bb4b4140`; all as at planning (`upstream_prs_unchanged: yes`) |
| origin | `custom_build` `94834763`, no `sync/v1.7.0` head, no gg8 tag, no open pull request, gg7 release `433237218 v1.7.0_gg7 false`, scratch tips `de8c685b` and `939e8992` both ancestors of the gg7 merge and of `HEAD` |

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 1 - Documentation accuracy] The test-tree row could not say "changed by the fork after `v1.7.0_gg1`" for the extra files**
- **Found during:** Task 3, step 4(d)
- **Issue:** the plan asked for one added category of the other 13 differing files "said as changed by the fork after `v1.7.0_gg1`". `git log v1.7.0_gg1..HEAD` and a byte comparison against upstream's tail show otherwise: 8 of them (`ClickHouseCloudTest`, `ExactlyOnceTest`, `ClickHouseSinkTaskBufferTest`, `ClickHouseSinkTaskSchemalessTest`, `ClickHouseSinkTaskStringTest`, `ProtobufIngestTest`, `ClickHouseHelperClientTest`, `CreateTableStatement`) are byte-identical to upstream `83808df9` (upstream changed them after `v1.7.0`), and 6 were changed by upstream's tail, by the fork, or both. Also one file of the earlier "two import-order only" pair, `ClickHouseSinkJdbcPropertiesTest`, is no longer import-only.
- **Fix:** the row now states that the other 28 differ, keeps the 13 named categories, names `LocalProviderTest` as the one import-order-only file, and adds two categories (8 files equal to upstream's tail, 6 mixed files with a reason each read from the file's log). Counts sum to 28 (`test_tree_categories`).
- **Files modified:** `docs/FORK-DELTA.md`
- **Commit:** `7fed6067`

**2. [Rule 3 - Blocking] A phase number inside a long changed line of FORK-DELTA**
- **Found during:** Task 3, gate `added_lines_ids`
- **Issue:** the raw-numbers intro paragraph is one line and had to change for the gg8 sentence; it already contained the parenthetical "(a Phase 28 review-fix test)", and the plan's own check rejects a phase number on any changed line.
- **Fix:** the parenthetical reads "(a review-fix test)"; nothing else of that sentence moved.
- **Files modified:** `docs/FORK-DELTA.md`
- **Commit:** `7fed6067`

**3. [Rule 1 - Source strength] CHANGELOG value-free wording for the record context**
- **Found during:** Task 2
- **Issue:** the plan's wording says the writer "rethrows its own exception unchanged"; FORK-DELTA rows for the RowBinary and JSON routes say a Jackson link in the cause chain is rebuilt without any record value.
- **Fix:** the bullet says the writer rethrows the exception it produced, with "a Jackson link in its cause chain rebuilt without any record value", so no claim is stronger than the source.
- **Files modified:** `CHANGELOG.md`
- **Commit:** `d2240a70`

**Total deviations:** 3 auto-fixed (2 documentation accuracy, 1 blocking wording). **Impact:** wording only; every plan verify passes and no figure moved.

## Open for the orchestrator

- **Broken links and forward references:** `CHANGELOG.md` has one broken link (`docs/ROLLOUT-v1.7.0_gg8.md`, line 11) and `docs/FORK-DELTA.md` two (lines 7 and 318, the same note). The path resolver reads exactly two unresolved tokens in FORK-DELTA: `.planning/scripts/confluent-archive-allowlist-v1.7.0_gg8.txt` (plan 49-04) and `docs/ROLLOUT-v1.7.0_gg8.md` (plan 49-02). The note is created in plan 49-02, the allow-list in plan 49-04.
- **Measured values against planning values:** everything held (testUnit 2325, suite 2928, 18 skips, wider and narrow numbers, recounts `30 14 19 10`, test tree `143 115`, upstream state). Differences that are not errors: the divergence read `1 223` before the merge (plan time `1 222`, one more planning commit), and the tail gate counts grew to `anchor_commits=206` and `marker_commits=224` (plan-time probe 204 and 222, as predicted for the added merge commit and the retag).
- **`upstream_prs_unchanged`:** `yes`. **`upstream_tag_past_v170`:** `no`.
- **JVM crash:** none; `-PjavaVersion=21` was not needed; no core dump or crash file appeared. Swap was 97 percent used at pre-flight (96 percent expected), with no effect.
- **Gradle version:** the wrapper runs Gradle 9.8.2 (the project notes say 7.4.2); no change to it was made or needed.
- **Decision for the review round (plan 49-03):** the test-tree row now carries a classification of 28 files with reasons (deviation 1); the reviewer should check it against `git log` and the tail.
- **CHANGELOG date:** heading still `# 1.7.0_gg8, Unreleased`; U-3's one-line date commit is the orchestrator's and the reviewer is to be told. Working-tree note: `.planning/config.json` and `.planning/state.json` stay modified and were not staged.
- **Not done by design:** nothing was pushed, no workflow was dispatched, no tag named `v1.7.0_gg8` exists locally or on origin, `custom_build` was not checked out or moved.

## Handoffs

- **Plan 49-02 (note):** the labels are exactly `measured`, `read from the code`, `derived`, each with at most one parenthetical qualifier (PL-3); the note's inventory has 22 rows U1-U4, T1-T10, S1-S4, P1, D1-D3 (PL-2); the archive figures are to be measured there (projected 17 exact allow-list rows and `changed 14 same 7498`, first measured in plan 49-02). The rollback line must say `pre-v1.7.0_gg8` names `94834763`, the published `v1.7.0_gg7` release, not `baa404cd`. Neither the note nor the CHANGELOG names a planning identifier; the retry-budget decision is stated in words and points at FORK-DELTA item 24.
- **Plan 49-04 (gates and allow-list):** bump commit `b6666fe748629ea597a7534fba0d9a2e6ef54cee`; the E-04 totals its gates must equal: testUnit `2325` tests with `2` skipped, suite `2928` tests with `18` skipped, anchors `218` rows (a total that differs with zero failures is a finding, not a pass); FORK-DELTA's gate-totals sentence holds the same numbers. The allow-list `.planning/scripts/confluent-archive-allowlist-v1.7.0_gg8.txt` does not exist yet and is generated last from the final tree, against a `v1.7.0_gg7` baseline (the baseline pipe now picks it). The tail gate runs again on the final head with `PRE_TAG=pre-v1.7.0_gg8` (now `94834763`). Any later change under `src`, the build files, `gradle` or `.github` invalidates the FORK-DELTA after-gg8 numbers and the E-04 totals.

## Self-Check: PASSED

Created and changed files exist (`49-COMMAND-EVIDENCE.md`, `VERSION`, `CHANGELOG.md`, `docs/FORK-DELTA.md`, `.planning/scripts/fork-anchors.txt`); the merge `77ea9c8c`, the bump `b6666fe7`, the CHANGELOG commit `d2240a70`, the FORK-DELTA commit `7fed6067` and the four evidence commits are ancestors of `HEAD`; `P49-01-T1-OK`, `P49-01-T2-OK` and `P49-01-T3-OK` printed on the committed tip; `pre-v1.7.0_gg8` reads `94834763`; no `v1.7.0_gg8` tag locally or on origin; origin `custom_build` still `94834763`.
