---
phase: 47-task-stop-retry-classification
plan: "03"
subsystem: docs
tags: [fut-05, fork-delta, changelog, decision-record, gg8, docs-only]
status: complete

requires:
  - phase: 47-task-stop-retry-classification
    provides: plan 47-01's fix commit and E-02..E-04, plan 47-02's FORK-DELTA item 17 and E-05..E-07, the plan-time 47-doc-edits.json (md5 3c4b1ce86e50bf8970e744a7eb6dc3a6)
provides:
  - E-08, the FUT-05 decision record (user, 2026-11-05, 47-CONTEXT.md U-1 and U-2) with its reason
  - FORK-DELTA items 8 and 24 with FUT-05 closed by decision (no line lists it as open)
  - the `# 1.7.0_gg8, Unreleased` CHANGELOG section with the corrected stop() Bug Fixes and Known Issues bullets
  - E-09, the claims map from each gg8 sentence to the fact that backs it
affects: [47-04, 49]

actuals:
  tokens: 3060
  tasks: 2
  commits: 4
plan_head_before: 7755b196f9bc1d27921eca356c9674a0b090e04c
plan_head_after: 9b82bdbb0fef1381708c2991267f8b69cae52cfa

tech-stack:
  added: []
  patterns: [plan-time JSON copy of the exact doc text applied by an asserted-once replace, replay of the planned edit on the commit parent as the byte check, prepend with the earlier file content proved byte-identical]

key-files:
  created: []
  modified:
    - docs/FORK-DELTA.md
    - CHANGELOG.md
    - .planning/phases/47-task-stop-retry-classification/47-COMMAND-EVIDENCE.md

key-decisions:
  - "U-2 applied: FUT-05 is closed with no validator, no retry budget and no removal setting; the start-up WARN, its text and its two pins stay, and ClickHouseSinkConfig, Utils and ProxySinkTask are byte-identical to the phase base"
  - "U-3 applied: the corrected stop() wording is a new 1.7.0_gg8, Unreleased section; the published 1.7.0_gg7 section, VERSION and both rollout notes are untouched"
  - "The both-throw Known Issues bullet states the E-04 outcome (the proxy task's exception arrives, the strategy's is dropped, 0 suppressed) as a one-off probe, not a pinned test"

requirements-completed: [RTY-02, LIF-02]

coverage:
  - id: D1
    description: "FUT-05 is closed by the user's decision of 2026-11-05 with its reason: E-08 records the decision, its source (47-CONTEXT.md U-1 and U-2), the reason, and validator, budget and removal setting all absent; FORK-DELTA items 8 and 24 are byte-equal to the planned replacements, no line keeps the open FUT-05 parenthesis, the stays-open wording or the not-closed sentence, and the Connector-source WARN row is unchanged"
    requirement: RTY-02
    verification:
      - kind: other
        ref: "P47-03-T1-OK (Task 1 automated verify; E-08)"
        status: pass
    human_judgment: false
  - id: D2
    description: "The WARN text, warnOnUnvettedRetriableCodes, UnvettedRetriableCodesWarnUnitTest, UnvettedRetriableCodesWarnLiveTest, AdditionalRetriableErrorCodesPinUnitTest, AdditionalRetriableErrorCodesTest, ClickHouseSinkConfig, Utils and ProxySinkTask are unchanged since the phase base; src/main differs from the base only in ClickHouseSinkTask.java (LIF-01); anchors mirror rows=218"
    requirement: RTY-02
    verification:
      - kind: other
        ref: "P47-03-T1-OK (git diff against B0, lines 22-28 and 136-143 of ClickHouseSinkTask, anchors check)"
        status: pass
    human_judgment: false
  - id: D3
    description: "CHANGELOG.md starts with the planned `# 1.7.0_gg8, Unreleased` section (22 lines, longest 117, SHA-256 0fa57f14...b8b8), byte-equal to the JSON copy and followed by the phase base's content byte for byte; each sentence maps to E-02, E-03, E-04 or FORK-DELTA item 13; VERSION, docs/ROLLOUT-v1.7.0_gg7.md (also against its tag) and docs/ROLLOUT-v1.7.0_gg6.md are unchanged"
    requirement: LIF-02
    verification:
      - kind: other
        ref: "P47-03-T2-OK (Task 2 automated verify; E-09)"
        status: pass
    human_judgment: false
---

# Phase 47 Plan 03: FUT-05 closure and the gg8 CHANGELOG section Summary

**FUT-05 is closed by the user's recorded decision (no validator, no budget, no removal setting; the WARN and its pins stay) in FORK-DELTA items 8 and 24, and CHANGELOG.md now starts with a `# 1.7.0_gg8, Unreleased` section whose stop() bullets claim only what the LIF-01 pin and the both-throw probe show, with no source change.**

## Performance

- **Duration:** about 3 min of execution (the plan's two doc edits and their evidence were applied from the plan-time JSON copy; start about 21:12:40Z, last task commit about 21:15Z). The start time was not captured by a `date` call at the first tool use, so the figure is read from the later timestamps.
- **Tasks:** 2 (tracer plus auto), 3 files changed (0 created, 3 modified), 4 commits before this SUMMARY.

## Accomplishments

- Task 1 (tracer, E-08): scope facts first, all as planned: `ClickHouseSinkConfig.java`, `Utils.java` and `ProxySinkTask.java` unchanged since the base, lines 22-28 (the WARN text) and 136-143 (`warnOnUnvettedRetriableCodes`) of `ClickHouseSinkTask.java` equal to the base, the four WARN and additional-codes test classes unchanged since the base. Then the three pairs of `47-doc-edits.json` key `fork_delta_fut05` applied to `docs/FORK-DELTA.md` (each old text occurred once), one commit changing only that file (2 insertions, 2 deletions: item 8 and item 24). Item 8 now says FUT-05 "is closed by decision (items 17 and 24)"; item 24's title reads "FUT-05 is closed by decision" and its tail records the user's decision of 2026-11-05, its reason (transient is the connector's judgment; a validator cannot tell a deployment-specific transient code such as 60 from a permanent one; a rejecting check would break a running connector at its next configuration write or task start) and the re-open trigger. The count of lines with the open FUT-05 parenthesis, the stays-open wording or the not-closed sentence is 0; the Connector-source WARN row citing Known cost 24 is unchanged.
- Task 2 (E-09): E-04's `both_throw_thrown` (`IllegalArgumentException:proxy side failure`), `both_throw_suppressed` (0) and E-03's `green_tests` (6) confirmed first. The `changelog_gg8` value prepended to `CHANGELOG.md` (22 lines added, none removed); the replay of the prepend on the commit parent equals the commit, the parent's CHANGELOG equals the phase base's, and the SHA-256 of the section is `0fa57f146c9923b6d87881631adaaddedbd88d3d7d87ff2a97f7e1e3ed97b8b8`. Heading counts: one `1.7.0_gg8` heading, one `1.7.0_gg7, 2026-11-02` heading.
- The claims map (E-09): every sentence of the gg8 section is mapped to its fact: the strategy-throw cleanup to E-02 (red) and E-03 (green), the strategy's exception reaching Connect to the pin's `assertSame` and the single-throw probe, the three shipped strategies to the four green cases on the unchanged tree and all six after, the gg7 sentence correction to `ProxySinkTaskStopUnitTest` (4 tests green) against the E-02 red run, the other residuals and the five-second wait to FORK-DELTA item 13, and the both-throw outcome to the E-04 probe, labelled one-off and not pinned.

## Commits

| Commit | Message | Files |
|---|---|---|
| `6ff00ea641e31d8f6df119524797b064c86cb665` | FORK-DELTA items 8 and 24 close FUT-05 by the user's decision, with its reason (RTY-02) | `docs/FORK-DELTA.md` |
| `a5feba35` | record the FUT-05 decision (E-08) | `47-COMMAND-EVIDENCE.md` |
| `15aa445fac0b034ffdd57e64e88042af4e196db5` | start the 1.7.0_gg8 CHANGELOG section with the corrected stop() bullets (LIF-02) | `CHANGELOG.md` |
| `9b82bdbb` | record the gg8 CHANGELOG section (E-09) | `47-COMMAND-EVIDENCE.md` |

## Baseline numbers later plans compute from

| Measure | Value |
|---|---|
| FORK-DELTA FUT-05 commit | `6ff00ea641e31d8f6df119524797b064c86cb665` (2 lines changed) |
| CHANGELOG gg8 commit | `15aa445fac0b034ffdd57e64e88042af4e196db5` (22 lines added) |
| gg8 section | 22 lines, longest 117, SHA-256 `0fa57f14...b8b8`; JSON copy md5 `3c4b1ce86e50bf8970e744a7eb6dc3a6` |
| Open FUT-05 lines in FORK-DELTA | 0 |
| Anchors mirror | 218 rows (unchanged by this plan) |
| `src/main` change since the phase base | `ClickHouseSinkTask.java` only (LIF-01) |
| Tests run in this plan | none (docs only); the last testUnit gate is E-05's 2295 tests, 0 failures |

## Deviations from Plan

None - plan executed exactly as written.

Process notes, not deviations. (1) `.planning/STATE.md`, `.planning/ROADMAP.md` and `.planning/REQUIREMENTS.md` were not updated or staged in this plan: the orchestrator's instructions reserved them (as in plan 47-02), so the state-update and requirement-marking steps of the workflow were left to it; `.planning/config.json` and `.planning/state.json` stay dirty and unstaged. (2) The two verify scripts were run from copies of the plan's `<automated>` commands saved in `/tmp/gsd47/exec/verify-t1.sh` and `verify-t2.sh` (a `set -e` wrapper, otherwise verbatim); both printed their OK line, and both were re-run on the committed tip after the E-09 commit. (3) The plan's `P47-03-T1-OK` check `git status --porcelain -- src docs CHANGELOG.md ...` is clean because the edits were committed before the verify ran.

## Authentication Gates

None.

## Issues Encountered

None.

## Hand-offs to Phase 49 (D-2, not edited here)

The rest of the gg8 section is Phase 49's: the upstream tail, the timeout limits, the RTY-01 limit bullet and the FUT-05 closure bullet, and the release date. The gg7 rollout note rows F5/R8 and its "FUT-05 stays open." line are Phase 49's as well, and are only inventoried in plan 47-04. Backlog candidates recorded in E-04 and FORK-DELTA item 13, unchanged in this phase: a `finally` in `ClickHouseSinkTask.close(partitions)`, and the in-repo `keepFirst` idiom in `stop()` (which would attach the strategy's exception as suppressed when both stops throw).

## Known Stubs

None.

## Threat Flags

None. No `src` change and no new endpoint or trust-boundary surface; T-47-14 (the gg8 text byte-checked, each sentence mapped, the both-throw sentence gated on E-04), T-47-15 (the gg7 section, VERSION and both rollout notes byte-identical to the base, the gg7 note also to its tag), T-47-16 (no validator or budget; the three files, the WARN text and method and the four tests unchanged) and T-47-17 (E-08 cites U-1 and U-2) are mitigated as planned.

## Ready for

Plan 47-04 (the phase gate: full suite on ClickHouse latest x client V2, the tail gate, the rollout-note inventory, E-10 and E-11).

## Self-Check: PASSED

The three modified files exist; commits `6ff00ea6`, `a5feba35`, `15aa445f` and `9b82bdbb` are ancestors of HEAD; P47-03-T1-OK and P47-03-T2-OK both printed on the committed tip; `git diff --name-only 427f30b4 HEAD -- src/main` prints only `ClickHouseSinkTask.java`; `git rev-list --count 7755b196..HEAD` was 4 before this SUMMARY.
