---
phase: 16-early-docs-publication
plan: "02"
subsystem: docs
tags: [code-review, review-disposition, publication-gate, approval-bundle, rollout-checklist]

requires:
  - phase: 16-early-docs-publication
    provides: "Plan 16-01 docs commits 65582a31, fad6f7f9, 8c1f0bc1 and 16-COMMAND-EVIDENCE.md E-01..E-13"
provides:
  - "16-REVIEW.md: incremental review of the Phase 16 docs diff (diff base 505e4981), one Info finding, both D-09 confirmations passed"
  - "16-REVIEW-DISPOSITION.md: the finding dispositioned fixed, open 0"
  - "15-REVIEW-DISPOSITION.md: IN-07..IN-10 closed as fixed with Phase 16 SHAs, open 0"
  - "docs/ROLLOUT-v1.7.0_gg1.md item 8 names reachable history instead of the unpublished sync branch"
  - "A measured approval bundle on tip X0 for the plan 16-03 gate"
affects: [16-03]

actuals:
  tokens: 2942
  tasks: 3
  commits: 4

tech-stack:
  added: []
  patterns:
    - "Review fix as its own docs(16-02) commit, with the plan 16-01 checks re-run and a new evidence entry"
    - "Approval bundle built only from commands run on the exact tip X0"

key-files:
  created:
    - .planning/phases/16-early-docs-publication/16-REVIEW.md
    - .planning/phases/16-early-docs-publication/16-REVIEW-DISPOSITION.md
  modified:
    - .planning/milestones/v1.7.0_gg1-sync-phases/15-suite-gate-publication-rollout/15-REVIEW-DISPOSITION.md
    - .planning/phases/16-early-docs-publication/16-COMMAND-EVIDENCE.md
    - docs/ROLLOUT-v1.7.0_gg1.md

key-decisions:
  - "IN-01 was fixed rather than deferred: the item 8 sentence now says 'merging the released history again (`git merge <merge-commit>^2`)'. This refines the wording of the D-05 line without changing what D-05 decided (revert the revert first)."

requirements-completed: [PUB-05, PUB-06]

coverage:
  - id: D1
    description: "A recorded incremental review of exactly the Phase 16 docs changes exists, with both D-09 confirmations stated"
    requirement: PUB-05
    verification:
      - kind: other
        ref: "REVIEW-OK (plan 16-02 Task 1 verify block)"
        status: pass
    human_judgment: false
  - id: D2
    description: "Every Phase 16 review finding is dispositioned and none is open; IN-07..IN-10 are closed in the Phase 15 record with SHAs that resolve to the fixing commits"
    requirement: PUB-05
    verification:
      - kind: other
        ref: "DISPOSITIONS-COMPLETE, D10-CLOSED and SCOPE-AND-COMMITS-OK (plan 16-02 Task 2 verify blocks)"
        status: pass
    human_judgment: false
  - id: D3
    description: "Tip X0 satisfies the publication rules by command: allow-list, tree equality, no .gsd-ws-arg, push config, secret scan, no TRUNCATE instruction, merge dry run"
    requirement: PUB-06
    verification:
      - kind: other
        ref: "ALLOW-LIST-OK, WS-ARG-ABSENT PUSH-CONFIG-SAFE, SECRET-SCAN-CLEAN, MERGED-TREE-NO-TRUNCATE-INSTRUCTION, MERGE-DRY-RUN-OK BEFORE-RUNNER-SWITCH (plan 16-02 Task 3 verify blocks)"
        status: pass
    human_judgment: false
  - id: D4
    description: "Whether the changed operator text reads correctly to an operator (criterion 1)"
    verification: []
    human_judgment: true
    rationale: "Readability of operator prose cannot be asserted by a command. The D-09 review judged it and recorded the verdict in 16-REVIEW.md; the user's approval at plan 16-03 is the final check"

duration: 7min
completed: 2026-10-10
status: complete
plan_head_before: bb063373e3a67fff83a461872148b021d35b7763
plan_head_after: 427a2f9d06c3d67b13eef6697f5ba898526ad790
commits: 4
---

# Phase 16 Plan 02: Pre-gate review, dispositions and measured approval bundle Summary

**The Phase 16 docs diff was reviewed incrementally (one Info finding, fixed), IN-07..IN-10 are closed in the Phase 15 record with resolvable SHAs, and tip X0 passes every publication rule by command; the measured approval bundle for plan 16-03 is below.**

## Performance

- **Duration:** 7 min
- **Started:** 2026-10-10T17:45:46Z
- **Completed:** 2026-10-10T17:53:00Z
- **Tasks:** 3
- **Files modified:** 5 (two new planning files, two edited planning files, one doc)

## Accomplishments

- `16-REVIEW.md` covers `CHANGELOG.md`, `docs/FORK-DELTA.md` and `docs/ROLLOUT-v1.7.0_gg1.md` against diff base `505e4981`. Critical 0, Warning 0, Info 1. Both mandated confirmations passed: `505e4981`'s WR-01..WR-03 fixes still read correctly, and no operator text tells an operator to TRUNCATE the shared state table.
- IN-01 (item 8 told the operator to merge `sync/v1.7.0`, a branch that exists only locally) is fixed in `fd99a476`. The new command, `git merge <merge-commit>^2`, was demonstrated on the real release merge as evidence E-14.
- `16-REVIEW-DISPOSITION.md` has one row, fixed, `open: 0`. `15-REVIEW-DISPOSITION.md` shows IN-07..IN-10 `fixed` in the frontmatter and the table, `open: 0`, and every other row unchanged.
- X0 was measured against every PUB-06 rule and the bundle below was written from those measurements.

## Task Commits

1. **Task 1: review** - `9207ec01` (docs(16-02): pre-gate review of the Phase 16 docs changes)
2. **Task 2: fix and dispositions** - `fd99a476` (IN-01 fix, rollout doc alone), `5eac200d` (evidence E-14), `427a2f9d` (both disposition files)
3. **Task 3: measurement only** - no commit; results are in the bundle

**Plan metadata:** the SUMMARY commit and the STATE/ROADMAP commit follow this file.

## Skill adaptation

The review ran inline, following `~/.claude/gsd-core/agents/gsd-code-reviewer.md`. The executor has no sub-agent tool, so the `gsd-code-review` skill could not spawn its reviewer (assumption A5, the 15-02 precedent). Scope was `git diff 505e4981 HEAD` for the three files; surrounding text was read for context only. The review schema (frontmatter with `diff_base`, `### ID:` headings at three hashes, a `## Mandated confirmations (D-09)` section) was written directly.

## Fix commits and evidence

| Commit | What |
|---|---|
| `fd99a476` | IN-01: rollout item 8 sentence now reads "merging the released history again (`git merge <merge-commit>^2`) without that brings nothing back" |
| `5eac200d` | Evidence entry E-14 (the go-forward command on `f328fd21` in a throwaway worktree: revert, merge `^2` prints "Already up to date.", revert-of-revert restores the release tree) |

The plan 16-01 checks for the touched file were re-run after the fix and printed ROLLOUT-PLAIN-LANGUAGE-OK, ROLLOUT-FINDINGS-OK, ITEM6-GREP-NARROWED and SQL-UNCHANGED-NO-TRUNCATE-OK. No SQL text changed, so no live ClickHouse run (D-07) was needed. The 16-01 EVIDENCE-COMPLETE check expects exactly E-01..E-13 and no longer applies now that E-14 exists; every entry still carries exactly one verdict (13 yes, 1 n/a).

## Assignment-scan classification

SECRET-SCAN-CLEAN found no private-key header, AWS key id or GitHub token over 126 changed files in `f328fd21..X0` (deletions excluded, no rename detection). The credential-shaped assignment scan found five hits, all in `.planning/` prose, none real:

| File | Hit | Class |
|---|---|---|
| `.planning/MERGE-CHECKLIST-v1.7.0.md:134` | `password = builder.password`, `password = password` | documentation of code (Java identifiers quoted in a checklist) |
| `.planning/research/ARCHITECTURE.md:173` | `secrets: inherit` | documentation of a workflow setting |
| `.planning/research/STACK.md:234` | `secrets: inherit` | documentation of a workflow setting |
| `.planning/research/PITFALLS.md:376` | `token = topic-partition-min-max` | documentation of the dedup token name |
| `.../13-fork-hardening-on-the-merged-tree/13-03-PLAN.md:17-18` | `tokens: 100000` | placeholder (token-count estimate) |

## Deviations from Plan

None - plan executed exactly as written. One judgement call, not a deviation from a rule: IN-01's fix rewrites the wording of the D-05 sentence ("merging `sync/v1.7.0`" became "merging the released history"). D-05's decision (revert the revert first) is unchanged, and the fix adds no content and touches no other file.

## Issues Encountered

None. `.planning/state.json` (tracked, modified) and `.planning/milestone.lock` (untracked) were already changed by the orchestrator before this plan and were never staged.

## Known Stubs

None.

## Threat Flags

None.

## User Setup Required

None.

## Next Phase Readiness

Plan 16-03 presents the Approval bundle below verbatim, re-checks that X0..X touches only `.planning/`, and asks for the publication approval. Nothing was pushed, `custom_build` is still `f328fd21`, and `git worktree list` shows only the main working tree.

## Approval bundle
X0=427a2f9d06c3d67b13eef6697f5ba898526ad790

All items below were measured on X0 by command in this plan, or read by command from a committed file.

**1. The tip.**
- `git rev-list --count f328fd21..X0` = 41 commits. 36 of them touch `.planning/` only; 5 touch docs or the CHANGELOG.
- `git log --oneline f328fd21..X0 -- docs CHANGELOG.md`:
  - `fd99a476` docs(16-02): IN-01 item 8 names the reachable history instead of the unpublished sync branch
  - `8c1f0bc1` docs(16-01): correct the 1.7.0_gg1 CHANGELOG section in place (15-REVIEW IN-10)
  - `fad6f7f9` docs(16-01): rollout recovery qualified by exactlyOnce, revert-of-revert note, narrowed compression grep, plain-language IDs (15-REVIEW IN-07, IN-08, IN-10)
  - `65582a31` docs(16-01): FORK-DELTA skip counts print totals; raw numbers reproduce against the tag (15-REVIEW IN-09)
  - `505e4981` docs(15): scope rollout KeeperMap steps to the connector, qualify defaults (15-REVIEW WR-01..03)

**2. What the published docs now say differently.** `git diff --stat f328fd21 X0 -- CHANGELOG.md docs/`:

```text
 CHANGELOG.md               | 17 +++++++----
 docs/FORK-DELTA.md         |  8 ++---
 docs/ROLLOUT-v1.7.0_gg1.md | 74 +++++++++++++++++++++++++++-------------------
 3 files changed, 58 insertions(+), 41 deletions(-)
```

- From `505e4981` (WR-01..WR-03): the KeeperMap steps are scoped to the connector (the state table is shared, the state query is topic-scoped, the recovery `DELETE` uses prefixed or topic-scoped keys, and the doc says not to `TRUNCATE` it); the `date_time_input_format` default is qualified by ClickHouse version and the check decides on `value`; item 7 gives separate matcher examples for JMX `ObjectName` patterns and regular expressions.
- IN-07: item 2 recovery says step 1 always, step 2 only with `exactlyOnce=true`. IN-08: item 8 explains how to go forward again after `git revert -m 1`. IN-09: FORK-DELTA skip counts print totals and the raw numbers reproduce against the `v1.7.0_gg1` tag. IN-10: no bare planning IDs in the rollout and CHANGELOG, the ledger linked once, the item 6 grep narrowed, and the CHANGELOG gains the MBean `version=` and client-name change and the lz4 advisory reason.
- IN-01 (this plan): item 8 names `git merge <merge-commit>^2` instead of the unpublished branch `sync/v1.7.0`.

**3. Readiness checks on X0 (verbatim outputs).**

```text
ALLOW-LIST-OK                          (X0=427a2f9d06c3d67b13eef6697f5ba898526ad790; diff names: 124 .planning, 1 CHANGELOG.md, 2 docs)
WS-ARG-ABSENT PUSH-CONFIG-SAFE
SECRET-SCAN-CLEAN                      (126 changed files; assignment scan classified above)
md-files 17 truncate-lines 2 bad []
MERGED-TREE-NO-TRUNCATE-INSTRUCTION
MERGE-DRY-RUN-OK BEFORE-RUNNER-SWITCH
```

- `.planning/state.json` is tracked (`git ls-files --error-unmatch` succeeds). `git status --porcelain -- .planning/state.json` prints ` M .planning/state.json`: it has uncommitted edits, which cannot ride a merge of the commit X by hash from a separate worktree. `.planning/milestone.lock` is untracked. CONTEXT's note that `state.json` is untracked was stale.
- Tree outside `.planning/`, `docs/` and `CHANGELOG.md` is identical to `f328fd21`; no path under src, the build files, `gradle`, `.github` or `VERSION` differs; the docs diff is exactly `CHANGELOG.md docs/FORK-DELTA.md docs/ROLLOUT-v1.7.0_gg1.md`.

**4. Records.**
- `16-COMMAND-EVIDENCE.md`: 14 entries (E-01..E-14), 13 verdicts "yes" and 1 "n/a (gate check)" (E-04). No "no".
- `16-REVIEW.md`: Critical 0, Warning 0, Info 1 (IN-01, fixed). Confirmation 1 (WR-01..WR-03 from `505e4981` still read correctly): yes. Confirmation 2 (no instruction to TRUNCATE the shared state table): confirmed.
- `16-REVIEW-DISPOSITION.md`: `open: 0`, `total: 1`. Deferred findings: none. D10-CLOSED printed: IN-07..IN-10 are `fixed` in `15-REVIEW-DISPOSITION.md` with SHAs `fad6f7f9` (IN-07, IN-08, IN-10 rollout), `65582a31` (IN-09) and `8c1f0bc1` (IN-10 CHANGELOG).

**5. Publication shape.**
- A `--no-ff` merge of the `sync/v1.7.0` tip X by hash, in the throwaway worktree `/tmp/gsd16/cb`, with first parent `f328fd21`.
- X0..X will be re-checked to touch only `.planning/`.
- Push command, with no force flag: `git -C /tmp/gsd16/cb push origin refs/heads/custom_build:refs/heads/custom_build`.
- No tag is created. `sync/v1.7.0` is not pushed.
- Origin before (`git ls-remote`):

```text
f328fd218dce5b9c652f8872042e922f9686b87f	refs/heads/custom_build
bec167d85918e77c11621063000acc6e031ae2fa	refs/heads/main
6ac2d20cc0c7feacc315b21b322325b4fee25784	refs/tags/pre-v1.6.0-sync
c61d90ce5dfb32ea5876dffacc8bb54d8846739a	refs/tags/pre-v1.7.0-sync
c61d90ce5dfb32ea5876dffacc8bb54d8846739a	refs/tags/v1.6.0_gg1
f328fd218dce5b9c652f8872042e922f9686b87f	refs/tags/v1.7.0_gg1
```

**6. CI expectation.** Exactly one `tests.yaml` run on the merge SHA, on the unchanged workflow and `ubuntu-latest`. Baseline: `f328fd21` has one run, `.github/workflows/tests.yaml`, id 38050860929, event push, completed, conclusion success, `run_started_at` 2026-10-10T12:08:09Z, `updated_at` 2026-10-10T12:14:38Z (about 6.5 minutes), https://github.com/ne1r0n/clickhouse-kafka-connect/actions/runs/38050860929. D-11 for a red run: no blind re-run; attribute the failure first. GitHub REST API core limit: 55 of 60 remaining when measured.

**7. Environment.** Today (UTC) is 2026-10-10, nine days before the 2026-10-19 `ubuntu-latest` switch. D-12 does not apply yet; publication must happen before 2026-10-19, or plan 16-03 offers D-12's choices instead of publish.

**8. What this publication does not contain.** No src, build or CI change since `f328fd21` (the tree outside `.planning/`, `docs/` and `CHANGELOG.md` is identical). None of the Phase 17-20 work. Local-only tags (`v1.6.0`, `v1.7.0`, `v1.6.0_gg1-sync`, `v1.7.0_gg1-sync`) stay local.

---
*Phase: 16-early-docs-publication*
*Completed: 2026-10-10*

## Self-Check: PASSED

- Created/modified files found: `16-REVIEW.md`, `16-REVIEW-DISPOSITION.md`, `15-REVIEW-DISPOSITION.md`, `16-COMMAND-EVIDENCE.md`, `docs/ROLLOUT-v1.7.0_gg1.md`.
- Commits found as ancestors of HEAD: `9207ec01`, `fd99a476`, `5eac200d`, `427a2f9d`, and the cited plan 16-01 commits `65582a31`, `fad6f7f9`, `8c1f0bc1`.
- Plan checks printed: REVIEW-OK, DISPOSITIONS-COMPLETE, D10-CLOSED, SCOPE-AND-COMMITS-OK, ALLOW-LIST-OK, WS-ARG-ABSENT PUSH-CONFIG-SAFE, SECRET-SCAN-CLEAN, MERGED-TREE-NO-TRUNCATE-INSTRUCTION, MERGE-DRY-RUN-OK BEFORE-RUNNER-SWITCH. `custom_build` is `f328fd218dce5b9c652f8872042e922f9686b87f`; `git worktree list` shows one entry.
