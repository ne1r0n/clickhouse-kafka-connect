# Phase 16: Early Docs Publication - Context

**Gathered:** 2026-10-10
**Status:** Ready for planning

<domain>
## Phase Boundary

Fix the four open 15-REVIEW docs findings (IN-07..IN-10) in `docs/ROLLOUT-v1.7.0_gg1.md`, `docs/FORK-DELTA.md` and `CHANGELOG.md`. Then publish them, together with the already-local WR-01..WR-03 fix (`505e4981`), to `origin/custom_build` as a docs-only, non-force, two-parent `--no-ff` merge of `sync/v1.7.0`. Requirements: PUB-05, PUB-06.

No code changes ship in this phase: no `src/`, build or `.github/` file may differ from `f328fd21`. It also adds no new rollout content beyond what the four findings ask for. The gg2 rollout note is a new file written in Phase 20.

Locked by ROADMAP/STATE and not re-opened here:
- The merge gate is a path allow-list diff (`.planning/`, `docs/`, `CHANGELOG.md` only) plus a tree-equality check (the merged tree equals the `sync/v1.7.0` tip). Assert by command that the tree outside those paths is identical to `f328fd21`. There is no full suite.
- `verify-merge-invariants.sh` does not apply to this merge and must not be "fixed".
- The approval gate runs orchestrator-inline and needs the user's own explicit answer. A relayed approval is refused.
- Merge from a throwaway worktree so the main tree stays on `sync/v1.7.0`.
- Push `custom_build` only, with an explicit refspec, non-force, and no tag. Exactly one `tests.yaml` run on the merge SHA, located by `head_sha` plus workflow path, green, with no cancelled sibling.
- Push before 2026-10-19, on the unchanged workflow.

</domain>

<decisions>
## Implementation Decisions

### CHANGELOG correction (IN-10, CHANGELOG part)
- **D-01:** Correct the published `# 1.7.0_gg1, 2026-10-10` section **in place**, with no correction note. This overrides PITFALLS 13's "correct forward". Rationale: the change is wording only, the release is days old, and an operator should read one coherent section. Git history keeps the original. Edits:
  - Replace the bare IDs with plain descriptions: the `R-01:` bullet prefix, `WR-02 strictness:`, and `(13-REVIEW WR-02)` in Bug Fixes.
  - Add an **Important Changes** bullet for the JMX MBean `version=` segment change (`v1.6.0_gg1` → `v1.7.0_gg1`) and the new client name `clickhouse-kafka-connect/v1.7.0_gg1`. It points at rollout item 7 for the matcher examples.
  - Reword the lz4 Dependencies bullet to give the reason: versions plus a pointer, e.g. "from `1.10.2` to `1.12.0`, because `1.11.2` and `1.11.3` are affected by three published advisories (fixed in `1.11.4`); see FORK-DELTA". Do not list advisory IDs. The FORK-DELTA dependency-pins row is the source of truth for the wording.
  - Leave upstream's sections untouched.
  — **Reversibility:** one-way once pushed — `custom_build` is published and protected, so after the merge the old wording can only be restored by a new commit.

### Planning IDs in operator text (IN-10, rollout part)
- **D-02:** In `docs/ROLLOUT-v1.7.0_gg1.md`, use plain text and give the ledger path once:
  - Rename the item 2 heading ("R-01 behaviour change") and the item 5 heading ("TYPE-08 and Array(Tuple)") to plain-language titles, and update the table of contents anchors to match. No other file links to these anchors; re-check by grep.
  - Remove the bare `R-01`, `TYPE-08`, `WR-02`, `13-REVIEW` and `999.1` references from prose and replace them with what they mean.
  - Keep `L-xx` ledger references only as a trailing tag, e.g. "(ledger L-20)".
  - Rewrite the preface sentence at line 13 to give the ledger's real path, as a relative link: `.planning/milestones/v1.7.0_gg1-sync-phases/13-fork-hardening-on-the-merged-tree/13-BEHAVIOUR-LEDGER.md`. Note that IN-10's suggested `.planning/phases/13-…` path is stale; the phase was archived.
- **D-03:** `docs/FORK-DELTA.md` is a maintainer doc, so the plain-language rule does **not** apply to it. Its IDs stay. In Phase 16 it changes only for IN-09:
  - Line 94: change "once the `v1.7.0_gg1` tag exists" to wording saying the commands reproduce the figures against the tag.
  - Lines 154-155: change the commands to ones that print totals, e.g. `grep -rho '@Disabled' src/test/java | wc -l` and `grep -rho 'Assumptions.assumeTrue' src/test/java | wc -l`, with the arrow text matching the actual output.
  - Phases 17-19 own their own FORK-DELTA rows. Raw numbers are refreshed only in Phase 20.

### Remaining finding fixes (carried from the review text, no discussion needed)
- **D-04:** IN-07: item 2's "What to do" opens with "Step 1 always; step 2 only for connectors with `exactlyOnce=true`". Remove the duplicated "sees the replayed ranges as already stored" sentence. End with "If no connector combines an explicit `insertFormat=json` with schema'd records, there is nothing to recover." Keep `505e4981`'s "Do not `TRUNCATE`" warning and its prefixed-key `DELETE` intact.
- **D-05:** IN-08: item 8 gains one line: going forward again after `git revert -m 1` needs the revert commit itself reverted first; merging `sync/v1.7.0` again without that brings nothing back.
- **D-06:** IN-10 item 6: narrow the self-check grep at rollout line 246 so the bare `set_compression_method` alternative becomes `set_compression_method"?[[:space:]]*[:=][[:space:]]*"?false`, consistent with the explanation that follows it.

### Review and command proof
- **D-07:** Every command quoted in **changed** text is run, and its output must match the text:
  - Run grep and git commands locally against the tree.
  - Run any SQL statement whose text changes on a live ClickHouse `latest` (Testcontainers, colima profile `vz`) with a KeeperMap state table (`keeper_map_path_prefix` set). Record `SELECT version()`.
  - SQL left unchanged keeps the 15-REVIEW verification (26.9.14.10) and is not re-run.
- **D-08:** Record the transcripts (command, actual output, matches text yes/no) in `.planning/phases/16-early-docs-publication/16-COMMAND-EVIDENCE.md`. It is committed in the phase, rides the merge under the `.planning/` allow-list, and is linked from the approval bundle.
- **D-09:** The pre-gate code review (criterion 2) is `gsd-code-reviewer`, incremental, over **only the Phase 16 commits** to the three docs. It must also confirm:
  - `505e4981`'s WR-01..WR-03 fixes still read correctly after the edits;
  - no text in the merged tree's operator docs tells an operator to `TRUNCATE` the shared KeeperMap state table. The existing "Do not `TRUNCATE`" sentence is the intended negative and is fine; the check must distinguish it.

  Every finding goes into `16-REVIEW-DISPOSITION.md` before the approval is asked for.
- **D-10:** Close IN-07..IN-10 in **both** places:
  - In the archived `.planning/milestones/v1.7.0_gg1-sync-phases/15-suite-gate-publication-rollout/15-REVIEW-DISPOSITION.md`, set each to `fixed`, citing the Phase 16 commit SHA in the Source cell.
  - Phase 16's own review gets `16-REVIEW-DISPOSITION.md`.

### Red CI and deadline
- **D-11:** If the merge SHA's single `tests.yaml` run is red, follow the Phase 11 D-11 precedent:
  - No blind re-run.
  - Attribute first: read the failing tests, compare the `latest` image date and server version with `f328fd21`'s green run, and reproduce locally.
  - Report the attribution to the user.
  - A re-run, which would create a second run on the SHA and break "exactly one run", happens only with the user's explicit OK. The criterion deviation is recorded.
- **D-12:** If the push cannot happen before 2026-10-19, **stop and ask**; do not push. The user chooses between:
  - publishing on Ubuntu 26 and attributing any red result to the runner, or
  - pulling CI-11's `ubuntu-24.04` pin forward. That is a workflow change, which breaks the docs-only allow-list, so it needs the user's decision.
  — **Reversibility:** one-way — the `custom_build` push can only be reverted forward.

### Claude's Discretion
- Exact plain-language titles for rollout items 2 and 5, and the exact CHANGELOG bullet wording, within D-01/D-02.
- The scripted form of the allow-list and tree-equality checks, and of the "tree outside the allow-list equals `f328fd21`" assertion.
- The merge commit message (by precedent it points at the corrected docs, so a future `git revert -m 1` names it), and the contents of the approval bundle (by Phase 15 D-12 precedent: measured, not cited).
- Plan split. One docs plan plus one inline publication plan is the likely shape.

</decisions>

<canonical_refs>
## Canonical References

**Downstream agents MUST read these before planning or implementing.**

### Findings being fixed
- `.planning/milestones/v1.7.0_gg1-sync-phases/15-suite-gate-publication-rollout/15-REVIEW.md` §"Incremental review: release documents" — IN-07..IN-10 text and suggested fixes; WR-01..WR-03, already fixed by `505e4981`
- `.planning/milestones/v1.7.0_gg1-sync-phases/15-suite-gate-publication-rollout/15-REVIEW-DISPOSITION.md` — rows to flip to `fixed` (D-10)

### Documents edited
- `docs/ROLLOUT-v1.7.0_gg1.md` — items 2, 5, 6 and 8, the preface (line 13) and the TOC
- `docs/FORK-DELTA.md` — line 94, lines 154-155; the dependency-pins row (line ~55) is the source for the lz4 advisory wording
- `CHANGELOG.md` — the `1.7.0_gg1` section only
- `.planning/milestones/v1.7.0_gg1-sync-phases/13-fork-hardening-on-the-merged-tree/13-BEHAVIOUR-LEDGER.md` — the ledger path the rollout preface links to

### Publication precedent and gates
- `.planning/ROADMAP.md` §"Phase 16" — success criteria, the note on the merge gate, the deadline
- `.planning/REQUIREMENTS.md` — PUB-05, PUB-06
- `.planning/STATE.md` §Blockers/Concerns — the [Publish] and [CI] items
- `.planning/milestones/v1.7.0_gg1-sync-phases/15-suite-gate-publication-rollout/15-CONTEXT.md` — D-11..D-13 publish mechanics, D-12 approval bundle, Phase 11 D-02/D-03/D-04/D-11 carried rules
- `.planning/research/PITFALLS.md` §"Pitfall 13" — the info-triage rule and "run every quoted command"; its "correct forward" advice is overridden by D-01

</canonical_refs>

<code_context>
## Existing Code Insights

### Current state (scouted 2026-10-10)
- `git diff --stat f328fd21 HEAD -- . ':!.planning'` shows only `docs/FORK-DELTA.md` and `docs/ROLLOUT-v1.7.0_gg1.md` (`505e4981`). Phase 16 adds `CHANGELOG.md`.
- `origin/custom_build` = `f328fd21`. `sync/v1.7.0` is 25 commits ahead.
- `.planning/state.json` is gitignored and untracked. `.planning/.gsd-ws-arg` does not exist right now. Re-check both at the gate.
- The rollout doc already says "Do not `TRUNCATE` the table" (line 111). No `TRUNCATE` instruction remains in `docs/` or `CHANGELOG.md`.

### Established Patterns
- CI runs are checked through the unauthenticated GitHub REST API (`actions/runs?head_sha=`); `gh` is not installed.
- The `tests.yaml` trigger is branch-only (CI-08), so the merge push starts exactly one run and a tag push starts none. No tag is pushed in this phase anyway.
- Planning-file edits by script: slice to the target section first (the `c1480f8f` truncation precedent).

### Integration Points
- Rollout anchors: only the doc's own TOC links to `#item-2-…` and `#item-5-…`. CHANGELOG and FORK-DELTA link to the file, not to anchors.

</code_context>

<specifics>
## Specific Ideas

- The lz4 reason should read like: "Bumped `at.yawk.lz4:lz4-java` from `1.10.2` to `1.12.0`, because `1.11.2` and `1.11.3` are affected by three published advisories (fixed in `1.11.4`); see FORK-DELTA."
- Ledger references in the rollout doc follow the form "(ledger L-20)".

</specifics>

<deferred>
## Deferred Ideas

- Plain-language rewrite of `docs/FORK-DELTA.md` prose: rejected for now (maintainer doc, D-03). Not scheduled.
- A full end-to-end re-review of all three documents: out of scope. Phase 20 reviews the new gg2 rollout note.

</deferred>

---

*Phase: 16-early-docs-publication*
*Context gathered: 2026-10-10*
