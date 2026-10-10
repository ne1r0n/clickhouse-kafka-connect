---
phase: 16-early-docs-publication
plan: "01"
subsystem: docs
tags: [rollout-checklist, changelog, fork-delta, 15-review, publication-gate, command-evidence]

requires:
  - phase: 15-suite-gate-publication-rollout
    provides: "505e4981 (WR-01..WR-03 fixes) and the open 15-REVIEW findings IN-07..IN-10"
provides:
  - "docs/FORK-DELTA.md skip-count commands that print totals; raw-numbers sentence in the present tense (IN-09)"
  - "docs/ROLLOUT-v1.7.0_gg1.md with exactlyOnce-qualified recovery (IN-07), revert-of-revert note (IN-08), plain-language IDs, ledger link and narrowed item 6 grep (IN-10)"
  - "CHANGELOG.md 1.7.0_gg1 section corrected in place, with the MBean version= and client-name facts and the lz4 advisory reason (IN-10)"
  - "16-COMMAND-EVIDENCE.md: entries E-01..E-13 with a Finding map, for the approval bundle"
  - "A publication gate dry run (allow-list diff, tree equality, --no-ff merge shape) proved by command with no ref moved"
affects: [16-02, 16-03, 20-release]

actuals:
  tokens: 7400
  tasks: 3
  commits: 6

tech-stack:
  added: []
  patterns:
    - "Every command quoted in changed doc text is run and its output recorded with a Matches text verdict"
    - "Publication gate rehearsed in a detached throwaway worktree before the real merge"

key-files:
  created:
    - .planning/phases/16-early-docs-publication/16-COMMAND-EVIDENCE.md
  modified:
    - docs/FORK-DELTA.md
    - docs/ROLLOUT-v1.7.0_gg1.md
    - CHANGELOG.md

key-decisions:
  - "Followed D-01..D-09 as planned; no new decisions were needed"
  - "The evidence file keeps exactly one `Matches text:` line per entry; E-05's old-text run carries an `Old-text verdict:` line instead, so the one-verdict-per-entry check holds"

requirements-completed: [PUB-05]

coverage:
  - id: D1
    description: "IN-09: FORK-DELTA skip-count commands print the totals 7 and 1 their arrow text states, and the raw-numbers sentence says the four shortstat commands reproduce the figures against the v1.7.0_gg1 tag"
    requirement: PUB-05
    verification:
      - kind: other
        ref: "16-COMMAND-EVIDENCE.md#E-01..E-03 (grep -rho ... | wc -l prints 7, 1 and 0 for the empty case; four git diff --shortstat commands print the table rows)"
        status: pass
    human_judgment: false
  - id: D2
    description: "IN-07: rollout item 2 opens with the exactlyOnce qualifier, says the replayed-ranges sentence once and ends on the nothing-to-recover sentence; the Do-not-TRUNCATE paragraph and both DELETE examples are unchanged"
    requirement: PUB-05
    verification:
      - kind: other
        ref: "16-COMMAND-EVIDENCE.md#E-11 (ROLLOUT-FINDINGS-OK) and #E-08 (SQL blocks equal 505e4981)"
        status: pass
    human_judgment: false
  - id: D3
    description: "IN-08: rollout item 8 says going forward after git revert -m 1 needs the revert commit reverted first, demonstrated on the real release merge f328fd21"
    requirement: PUB-05
    verification:
      - kind: other
        ref: "16-COMMAND-EVIDENCE.md#E-06 (REVERT-OF-REVERT-DEMONSTRATED)"
        status: pass
    human_judgment: false
  - id: D4
    description: "IN-10 rollout part: plain-language item 2 and 5 headings, no bare planning IDs, ledger tags, ledger link at its archived path, item 6 grep narrowed to the four risky lines"
    requirement: PUB-05
    verification:
      - kind: other
        ref: "16-COMMAND-EVIDENCE.md#E-05, #E-07, #E-10 (ROLLOUT-PLAIN-LANGUAGE-OK, ITEM6-GREP-NARROWED)"
        status: pass
    human_judgment: false
  - id: D5
    description: "IN-10 CHANGELOG part: 1.7.0_gg1 section corrected in place in plain language, with the MBean version= and client-name bullet and the lz4 advisory reason; upstream's sections byte-identical"
    requirement: PUB-05
    verification:
      - kind: other
        ref: "16-COMMAND-EVIDENCE.md#E-12, #E-13 (CHANGELOG-SECTION-OK)"
        status: pass
    human_judgment: false
  - id: D6
    description: "The plan 16-03 publication gate (allow-list diff, tree equality outside the allow-list, no .gsd-ws-arg, --no-ff merge shape) passes on the IN-09 tip in a throwaway worktree"
    requirement: PUB-05
    verification:
      - kind: other
        ref: "16-COMMAND-EVIDENCE.md#E-04 (GATE-DRY-RUN-OK)"
        status: pass
    human_judgment: false
  - id: D7
    description: "Whether the corrected rollout and release-note wording reads well to an operator"
    verification: []
    human_judgment: true
    rationale: "Readability and plain-language adequacy of operator prose cannot be asserted by a command; plan 16-02 runs the incremental review and the user approves publication"

duration: 4min
completed: 2026-10-10
status: complete
plan_head_before: 4fa3ded756609b0601760d72f21f28537bb4d407
plan_head_after: ad93295392c357048ad6bdc6acebc325a6cc1b71
commits: 6
---

# Phase 16 Plan 01: Docs findings IN-07..IN-10 fixed and command-proven Summary

**The four open 15-REVIEW docs findings are fixed in FORK-DELTA, the rollout checklist and the 1.7.0_gg1 CHANGELOG section (corrected in place), every command the changed text quotes was run and recorded in 16-COMMAND-EVIDENCE.md, and the publication gate was dry-run on the real repository without moving a ref.**

## Performance

- **Duration:** 4 min
- **Started:** 2026-10-10T17:39:40Z
- **Completed:** 2026-10-10T17:43:52Z
- **Tasks:** 3
- **Files modified:** 4 (three docs and the evidence file)

## Accomplishments

- IN-09: FORK-DELTA's two skip-count lines are `grep -rho ... | wc -l` and print 7 and 1; the raw-numbers sentence says the four commands reproduce the figures against the `v1.7.0_gg1` tag, and they print exactly the table's numbers. FORK-DELTA changed on no other line (old lines 94, 154, 155).
- IN-07, IN-08 and the rollout part of IN-10 are fixed in `docs/ROLLOUT-v1.7.0_gg1.md`: exactlyOnce-qualified recovery, the revert-of-revert note, plain-language headings for items 2 and 5, no bare planning IDs, every ledger reference as a `(ledger L-nn)` tag, the ledger linked at its archived path, and the item 6 grep narrowed.
- The `1.7.0_gg1` CHANGELOG section reads in plain language, gains the MBean `version=` and client-name bullet, and gives the lz4 advisory reason with versions only. Everything from `# 1.7.0, 2026-09-30` onward is byte-identical to `505e4981`.
- The publication gate dry run passed on the IN-09 tip: allow-list diff names only `.planning/` and `docs/`, the tree outside them equals `f328fd21`, `.planning/.gsd-ws-arg` is absent, and a `--no-ff` merge in a detached throwaway worktree gave M^1 = `f328fd21`, M^2 = the tip, equal trees. `custom_build` stayed `f328fd21`.

## Task Commits

1. **Task 1 (tracer): IN-09 and the gate dry run** - `65582a31` (docs, FORK-DELTA alone); evidence `6cb673a9`
2. **Task 2: rollout doc (IN-07, IN-08, IN-10)** - `fad6f7f9` (docs, rollout doc alone); evidence `a77cef3f`
3. **Task 3: CHANGELOG correction (IN-10)** - `8c1f0bc1` (docs, CHANGELOG alone); evidence and finding map `ad932953`

**Plan metadata:** the SUMMARY commit and the STATE/ROADMAP commit follow this file.

### D-10 source SHAs for plan 16-02

| Finding | Source commit(s) |
|---|---|
| IN-07 | `fad6f7f9` |
| IN-08 | `fad6f7f9` |
| IN-09 | `65582a31` |
| IN-10 | `fad6f7f9` (rollout part) and `8c1f0bc1` (CHANGELOG part) |

## Evidence entries

`16-COMMAND-EVIDENCE.md` holds E-01 to E-13, each with the command, its verbatim output and a verdict (yes everywhere except E-04, `n/a (gate check)`), plus a Finding map for IN-07..IN-10.

| Entry | Backs |
|---|---|
| E-01, E-02 | FORK-DELTA skip totals (7, 1) and the empty case (0) |
| E-03 | the four shortstat commands against the raw-numbers table |
| E-04 | gate dry run (gate check) |
| E-05 | item 6 grep: 4 positive matches, 0 negative; old pattern matched the `true` lines |
| E-06 | revert-of-revert on `f328fd21` |
| E-07 | plain-language, ledger-tag, anchor and link check |
| E-08, E-09 | SQL blocks unchanged (7), no TRUNCATE instruction |
| E-10 | ledger link target tracked |
| E-11 | item 2 and item 8 findings |
| E-12, E-13 | CHANGELOG facts and section check |

## Gate dry-run tokens

```text
X=65582a31d5bdc62aa77cdf9ea873e9c2ae7b8257
ALLOW-LIST-OK            (120 .planning paths, 2 docs paths)
OUTSIDE-TREE-EQUALS-B
GSD-WS-ARG-ABSENT
M=5d391866754131842695dfb2d685cd4a2ce92c76   (unreferenced, garbage-collected)
TOKENS=3   M^1=f328fd21...   M^2=65582a31...   TREE(M)==TREE(X)
custom_build=f328fd218dce5b9c652f8872042e922f9686b87f
GATE-DRY-RUN-OK
```

The dry run used the tip after the IN-09 commit. Plan 16-03 must re-run the same checks on the final tip, which now also carries the rollout and CHANGELOG commits and this plan's evidence and SUMMARY.

## Files Created/Modified

- `docs/FORK-DELTA.md` - IN-09: raw-numbers sentence and the two skip-count command lines
- `docs/ROLLOUT-v1.7.0_gg1.md` - IN-07, IN-08, IN-10: preface, TOC, item 2 and 5 headings, item 2 What to do, ledger tags, item 6 grep, item 8 note
- `CHANGELOG.md` - IN-10: the `1.7.0_gg1` section only
- `.planning/phases/16-early-docs-publication/16-COMMAND-EVIDENCE.md` - transcripts for E-01..E-13 and the Finding map

## Decisions Made

None - followed the plan and decisions D-01..D-09 as specified. One bookkeeping choice: E-05 keeps exactly one `Matches text:` line, so its old-text run records `Old-text verdict: n/a (base text, replaced)` on a differently named line; this keeps the one-verdict-per-entry check true.

## Deviations from Plan

None - plan executed exactly as written. Both skip counts printed 7 and 1 (assumption A4 held), so Task 1 did not stop.

## Issues Encountered

None. Two environment notes: the revert-of-revert demonstration needed `-c user.name/user.email` on the throwaway worktree's commands to give its throwaway commits an identity (the global git identity was not used for them; recorded in E-06). `.planning/STATE.md`, `.planning/state.json` and `.planning/milestone.lock` were modified or untracked before this plan started; `state.json` and `milestone.lock` were never staged.

## Known Stubs

None. The docs and evidence contain no placeholder or TODO text added by this plan (the `<db>`, `<topic>` style placeholders in the rollout doc are the operator-facing convention it already used).

## Threat Flags

None. No network endpoint, auth path, file access or schema surface was added.

## User Setup Required

None - no external service configuration required.

## Next Phase Readiness

- Plan 16-02 can run the incremental review over `65582a31`, `fad6f7f9` and `8c1f0bc1` and flip IN-07..IN-10 in the archived `15-REVIEW-DISPOSITION.md` with the SHAs above.
- Plan 16-03 can use the dry-run procedure of E-04. The push must happen before 2026-10-19. Nothing was pushed, no branch moved, and `git worktree list` shows only the main working tree.

---
*Phase: 16-early-docs-publication*
*Completed: 2026-10-10*

## Self-Check: PASSED

- Created/modified files found: `docs/FORK-DELTA.md`, `docs/ROLLOUT-v1.7.0_gg1.md`, `CHANGELOG.md`, `.planning/phases/16-early-docs-publication/16-COMMAND-EVIDENCE.md`.
- Commits found as ancestors of HEAD: `65582a31`, `6cb673a9`, `fad6f7f9`, `a77cef3f`, `8c1f0bc1`, `ad932953`.
- Plan-level checks re-run: FORK-DELTA-TOTALS-OK, RAW-NUMBERS-REPRODUCE, GATE-DRY-RUN-OK, ROLLOUT-PLAIN-LANGUAGE-OK, ROLLOUT-FINDINGS-OK, ITEM6-GREP-NARROWED, SQL-UNCHANGED-NO-TRUNCATE-OK, REVERT-OF-REVERT-DEMONSTRATED, CHANGELOG-SECTION-OK, EVIDENCE-COMPLETE.
