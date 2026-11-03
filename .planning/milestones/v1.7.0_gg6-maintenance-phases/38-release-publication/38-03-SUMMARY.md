---
phase: 38-release-publication
plan: "03"
subsystem: release
tags: [independent-review, review-rounds, disposition, frozen-documents, evidence]

requires:
  - phase: 38-release-publication
    provides: "plan 38-01: VERSION v1.7.0_gg6, the CHANGELOG section, FORK-DELTA and E-01..E-04; plan 38-02: the rollout note with its 29 labelled rows and E-05..E-07"
provides:
  - "38-REVIEW.md: twelve review rounds, each by an orchestrator-spawned fresh-context gsd-code-reviewer, 43 findings (28 warning, 15 info, 0 critical), a confirmations section per round"
  - "51 fix commits to docs/ROLLOUT-v1.7.0_gg6.md, the 1.7.0_gg6 section of CHANGELOG.md, docs/FORK-DELTA.md and one line pair of docs/ROLLOUT-v1.7.0_gg5.md"
  - "38-REVIEW-DISPOSITION.md: 43 rows, open: 0, 42 fixed and 1 deferred (the docs/ROLLOUT-v1.7.0_gg1.md half of WR-08), the recorded round3_decision"
  - "38-COMMAND-EVIDENCE.md entries E-08..E-10: round 1, the rounds and re-proofs, the frozen documents"
affects: [38-04, 38-05]

plan_head_before: 83026266
plan_head_after: d9d8d41b64953d2732e5b8831e51d861c2754877

actuals:
  tokens: 88680
  tasks: 3
  commits: 74

tech-stack:
  added: []
  patterns:
    - "fix-only executor runs, one commit per finding `docs(38-03): <ID> <what>`, each followed by the owning checks and a re-proof written to a scratch file"
    - "disposition frontmatter ids and table rows generated from the review's `### ID:` headings and checked by 38-review-check.py"

key-files:
  created:
    - .planning/phases/38-release-publication/38-REVIEW.md
    - .planning/phases/38-release-publication/38-REVIEW-DISPOSITION.md
  modified:
    - docs/ROLLOUT-v1.7.0_gg6.md
    - docs/ROLLOUT-v1.7.0_gg5.md
    - CHANGELOG.md
    - docs/FORK-DELTA.md
    - .planning/phases/38-release-publication/38-COMMAND-EVIDENCE.md
    - .planning/phases/38-release-publication/38-review-check.py

key-decisions:
  - "The plan's cap of three review rounds was exceeded to twelve, each further round authorised by the user's answer (relayed by the orchestrator); the loop ended on the user's decision to fix the two round-12 info findings and close the review, not on a clean round"
  - "Reviewer fix text was checked before use and changed where it overreached (WR-03, WR-13, WR-18, WR-23, WR-27, WR-28, IN-14, IN-15); labels were only ever weakened to what the phase ran, never strengthened by adding evidence"
  - "The optional Docker-free pin of IN-04 was not added (this phase may not add a test); the wording half is fixed and the pin half is neither applied nor carried"
  - "The docs/ROLLOUT-v1.7.0_gg1.md half of WR-08 is deferred with owner and re-open trigger, because that file is outside the editable set"

requirements-completed: [RLS-04]

coverage:
  - id: D1
    description: "Every review round was run by the orchestrator as a separately spawned fresh-context gsd-code-reviewer over the previous round's fix commits (round 1 over the five documents); the executor never reviewed"
    requirement: "RLS-04"
    verification:
      - kind: other
        ref: "38-review-check.py rounds 38-REVIEW.md 38-REVIEW-DISPOSITION.md (OK, twelve rounds, 43 findings); E-08 and E-09 name the reviewer; plan verify REVIEW-38-ROUND-OK and REVIEW-ROUNDS-38-OK"
        status: pass
    human_judgment: false
  - id: D2
    description: "All 43 findings are dispositioned: 42 fixed with the SHA of a Phase 38 commit and 1 deferred with reason, owner, re-open trigger and a carried entry; open is 0"
    requirement: "RLS-04"
    verification:
      - kind: other
        ref: "38-review-check.py disposition (OK findings=43 rows=43); plan verify DISPOSITION-38-OK"
        status: pass
    human_judgment: false
  - id: D3
    description: "The documents are frozen for the gate: the owning checks of plans 38-01 and 38-02 pass on the tip, the non-planning change set is exactly the five expected files, and every protected path is unchanged"
    requirement: "RLS-04"
    verification:
      - kind: other
        ref: "chk.sh (note, labels, links, changelog, numbers, hygiene, anchors); E-10; plan verify DOCS-FROZEN-OK"
        status: pass
    human_judgment: false
  - id: D4
    description: "The twelve rounds and the fixes make the release documents true to the tree and the evidence"
    verification: []
    human_judgment: true
    rationale: "The last two fixes (IN-14, IN-15) were not reviewed by a fresh reviewer; whether the final text is right is for the operator reading the note and for the approval bundle of plan 38-05"

duration: long, spread over twelve review rounds
completed: 2026-10-31
status: complete
---

# Phase 38 Plan 03: Independent review of the release documents Summary

**Twelve fresh-context review rounds found 43 issues in the rollout note, the CHANGELOG section and FORK-DELTA (28 warnings, 15 info, 0 critical); 42 are fixed in 51 commits with every fix re-proved against the tree, one half-finding about an older rollout note is deferred with its owner, and the documents are frozen with the owning checks green.**

## Rounds

Twelve rounds, findings per round `1:16; 2:4; 3:2; 4:2; 5:2; 6:2; 7:1; 8:2; 9:3; 10:5; 11:2; 12:2` (the `round: n findings: k` lines of `38-REVIEW.md`, sum 43). Each round was a separately spawned fresh `gsd-code-reviewer`, started by the orchestrator over the previous round's fix commits.

| Round | Review commit | Findings | Fix commits |
|-------|---------------|----------|-------------|
| 1 | `cdbdbbd7` | WR-01 to WR-09, IN-01 to IN-07 (16) | `63d51d56`, `ccdd14df`, `a8d05b43`, `cc3f119d`, `0d984e53`, `c01224a8`, `59afabb1`, `f1e32844`, `00ed58ff`, `51dcf7cf`, `08acd104`, `8185099d`, `299a6fed`, `a987e97c`, `e9d7796e`, `21d70b07` |
| 2 | `07101993` | WR-10 to WR-12, IN-08 (4) | `ed11c7ff`, `b0edab5a`, `292c82d3`, `266d157e` |
| 3 | `593fb972` | WR-13, IN-09 (2) | `3d55172c`, `45defb53` |
| 4 | `4ef5f17e` | WR-14, WR-15 (2) | `93db97f8`, `e00026fe`, sweep `e00a0682` |
| 5 | `04b7c299` | WR-16, WR-17 (2) | `2ff65bb2`, `6595c5e9` |
| 6 | `3e007c51` | WR-18, IN-10 (2) | `3e27f23c`, `667cfe21` |
| 7 | `6d3dafdb` | IN-11 (1) | `26056ca1` |
| 8 | `c2d8d28b` | WR-19, WR-20 (2) | `b4550087`, `4c765423`, sweeps `046aebcf`, `897c0baf`, `6dfd9202` |
| 9 | `f8f1507c` | WR-21, WR-22, IN-12 (3) | `9e5afc41`, `58fdadc3`, `3d664af2`, sweeps `17aaa713`, `2e1ad498` |
| 10 | `418908fd` | WR-23 to WR-26, IN-13 (5) | `97be60ba`, `3330b185`, `50104166`, `73ef2e6f`, `cd7183b1` |
| 11 | `0a0ab81d` | WR-27, WR-28 (2) | `dfbbe5b4`, `99757d2d`, sweeps `0e25bb5f`, `8c20804d` |
| 12 | `4a33d9ad` | IN-14, IN-15 (2) | `fd8b8575`, `927ac07c` (not reviewed again) |

The checker `38-review-check.py` was extended stepwise because the plan's cap (three rounds, one more with a decision) ran out: `3b1b90cb` (to six), `66792125` (seven), `b060274a` (eight), `02b72f81` (nine), `c085c308` (ten), `dd023c41` (eleven), `663105b4` (twelve). Only the cap and its messages changed; the decision rules were not touched, and each raise was proved on synthetic reviews.

`round3_decision` (recorded in `38-REVIEW-DISPOSITION.md`, relayed by the orchestrator from the user's separate AskUserQuestion answers, which this executor cannot verify): after round 3 «Исправить и чётвёртый раунд (Recommended)»; after round 4 «Исправить и пятый раунд»; after round 5 «Исправить и шестой раунд»; after round 6 «Исправить и седьмой раунд»; after round 7 «Исправить и восьмой раунд»; after round 8 «Девятый раунд (Recommended)»; after round 9 «Десятый раунд по разделу B (Recommended)»; after round 10 «Одиннадцатый раунд (Recommended)»; after round 11 «Двенадцатый раунд (Recommended)»; after round 12 «Исправить два info и закрыть ревью (Recommended)».

Evidence: E-08 (`020fc9fe`, round 1), E-09 (`a06428f7`, the rounds, the 51 fix commits and one `fix_*_reproof` key per fix that touched a command, number, label or rollback step), E-10 (`d9d8d41b`, frozen documents). The loop ended with `last_round_clean: no`: round 12 reported two info findings.

## Dispositions

`fixed:42 rejected:0 deferred:1`, open 0.

- **Deferred (1):** WR-08, the `docs/ROLLOUT-v1.7.0_gg1.md` half (item 6 says "measured" for the `zstd-jni` native and the executable temp folder, which the gg6 note labels measured in research, and says client V1 "of any line" where two lines were observed). Reason: that file is outside this phase's editable set. Owner: the user as fork maintainer. Re-open trigger: the next edit of `docs/ROLLOUT-v1.7.0_gg1.md`, or the milestone close. The FORK-DELTA half was fixed in `f1e32844`. Carried under `## Carried to the milestone close` in the disposition.
- **IN-04 (fixed, one half not applied):** the D7 wording says the probe is not in the repository (`299a6fed`); the reviewer's optional "consider a Docker-free pin" of the `custom_` forwarding was not added (a test is outside what this phase may change) and is not carried. A later client bump could change the forwarding without a test noticing; the orchestrator may want a carried entry for it.
- The 42 fixed rows each start with the SHA of a Phase 38 commit; sweep commits without a finding ID are listed in E-09 only.

## Task Commits

1. **Task 1 (round 1):** `cdbdbbd7` review, `020fc9fe` E-08 (committed before this executor's run)
2. **Task 2 (fixes and rounds 2 to 12):** the 51 fix commits, the eleven round commits and the seven checker commits in the table above; this run added `fd8b8575` (IN-14), `927ac07c` (IN-15) and `a06428f7` (E-09)
3. **Task 3:** `5031bb00` the disposition, `d9d8d41b` E-10

**Plan metadata:** the commit that carries this SUMMARY.

## Deviations from Plan

**1. [Plan wording] The loop ran twelve rounds, not at most three**
- The plan's stop rule offered accept, authorise a fourth round, or hold after round 3. The user chose a further round after each of rounds 3 to 11 and chose to fix and close after round 12. Phase 32 is the precedent for ending on the user's decision with the last fixes unreviewed.
- **Review checker cap:** raised in seven commits from four to twelve (list above); the plan lists no edit of the helper, so each raise was a user-authorised deviation.

**2. [Plan wording] Two Info fixes are unreviewed**
- IN-14 and IN-15 were fixed after round 12 and no round 13 exists, on the user's decision ("fix the two info and close the review"). Their traces were re-read in the tree (reproofs in E-09) and the owning checks pass, but no fresh reviewer read the result.

**3. [Rule 3 - Blocking] E-09 was committed before the disposition**
- `rounds_check` needs the `round3_decision` line that only the disposition file carries, so the disposition was written first and checked, then E-09 was committed, then the disposition (the user's part order). E-09 says so.

**4. [Plan wording] `pending_verdicts: none` and the time-scoped verify blocks**
- The plan's `pending_verdicts=none` cannot hold for index rows (plan 38-01 listed E-11 to E-19 as pending from the start), so E-10 records `none` for evidence entries and names the nine rows in `pending_index_rows`. The task verify blocks of plans 38-01 and 38-02 carry clauses fixed to their own tips (a three-file or one-file change set, unchanged `docs` and `CHANGELOG.md`, the one-line gg5 change count, the `--only B C` part-1 check) and fail on this tip by construction; the owning checks named in this plan's text were re-run as written and pass. All three verify blocks of this plan pass as written (`REVIEW-38-ROUND-OK`, `REVIEW-ROUNDS-38-OK`, `DISPOSITION-38-OK` and `DOCS-FROZEN-OK`).

**5. [Plan wording] The gg5 note changed two lines, not one**
- WR-04 made the note's line 39 (no release exists) agree with line 111 and with the published release; the plan's "one corrected line" became two. The change stays inside `docs/ROLLOUT-v1.7.0_gg5.md`, a listed file.

**6. [Rule 1 - Bug] Reviewer fix text that was wrong or too strong**
- WR-03 (a class named for a log line it does not write, a claim for every client), WR-13 (three places), WR-18 (an ambiguous "both client lines"), WR-23 (an assertion about what Connect accepts, not measured), WR-27 (a revocation sentence for every SMT), WR-28 ("silently" where the code logs a WARN) and IN-14 and IN-15 (an exhaustive "either or", keyed to the retained log instead of the first chunk) were corrected before applying; each correction is in the E-09 re-proof of that finding.

**Total deviations:** 6. **Impact:** scope unchanged (five documents, no `src`, test, build or workflow change); the review took twelve rounds instead of three and its last two fixes are unreviewed.

## Issues Encountered

- Tracing IN-14 showed one first-chunk case that neither the reviewers nor the note state: a first chunk that ends exactly at the stored maximum and starts at or below the stored minimum is `SAME` and is skipped, not reset or inserted. The note's cases (inside the range skipped, across its top cut, below it reset or failed) are not wrong for it, but they do not name it; it is left unstated, for the operator to weigh against the not-characterised list.
- `.planning/config.json` and `.planning/state.json` stay modified in the working tree and were never staged.

## Known Stubs

None.

## Threat Flags

None. The plan changes documents and planning files only; no endpoint, auth path or file access was added.

## User Setup Required

None - no external service configuration required.

## Handoffs

- **Plan 38-04 (allow-list and gate):** the documents are frozen at the tip named in E-10 (`docs_frozen: yes`, `docs_frozen_scope` the five expected files, `.planning/scripts` changed only in `fork-anchors.txt`). Any later edit of the note, the CHANGELOG section or FORK-DELTA re-opens the review and the allow-list inputs.
- **Plan 38-05 (publication and approval bundle):** the deferred finding WR-08 goes to the close record and the bundle's review item with its owner and re-open trigger; the bundle should say that twelve rounds ran, that the last two fixes (IN-14, IN-15) were not reviewed again by the user's decision, and that the optional `custom_` forwarding pin of IN-04 was not added. The two upstream states and the gg5 release record are re-read at publication.
- **Orchestrator:** REQUIREMENTS.md RLS-04 and the ROADMAP row for plan 38-03 were not touched by this run; `STATE.md`, `ROADMAP.md` and `REQUIREMENTS.md` updates are left to the orchestrator. The broken-windows ledger (`.planning/WINDOWS.md`) was not written to.

## Next Phase Readiness

Plan 38-04 can start: `38-REVIEW-DISPOSITION.md` is committed with `open: 0`, the owning checks pass, the anchors mirror reads `rows=172 bad=0`, and `git diff --shortstat v1.7.0_gg5 HEAD -- src/main` still reads 7 files, 112 insertions, 31 deletions.

## Self-Check: PASSED

- Files exist: `38-REVIEW.md`, `38-REVIEW-DISPOSITION.md`, `38-COMMAND-EVIDENCE.md`, `docs/ROLLOUT-v1.7.0_gg6.md`, `CHANGELOG.md`, `docs/FORK-DELTA.md`.
- Commits `fd8b8575`, `927ac07c`, `a06428f7`, `5031bb00`, `d9d8d41b` are ancestors of `HEAD`.
- `REVIEW-38-ROUND-OK`, `REVIEW-ROUNDS-38-OK`, `DISPOSITION-38-OK` and `DOCS-FROZEN-OK` print on the tip before this SUMMARY.

---
*Phase: 38-release-publication*
*Completed: 2026-10-31*
