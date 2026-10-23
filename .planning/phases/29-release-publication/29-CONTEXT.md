# Phase 29: Release & Publication - Context

**Gathered:** 2026-10-23
**Status:** Ready for planning

<domain>
## Phase Boundary

Publish `v1.7.0_gg4` on `custom_build`. Deliver `docs/ROLLOUT-v1.7.0_gg4.md` (every claim measured or labelled), the gg1/gg2 note corrections, fresh-context review of all operator documents, two green `--rerun` suite runs, and the rehearse → approve → publish recipe from Phase 24. No `src/main` edit and no edit of an existing test.

</domain>

<decisions>
## Implementation Decisions

### Anchors gate
- **D-01:** The release-tip gate uses the cumulative form: `PRE_TAG=pre-v1.7.0-sync` with SHA-pinned exemption rows (the gg2/gg3 form). `pre-v1.7.0-tail` is not the gate; it stays a Phase 25 rollback tag. `pre-v1.7.0_gg4` is reserved for the release rollback tag.

### gg1/gg2 note corrections
- **D-02:** The corrected `enableDbTopicSplit=true` key rule and state-table query in `ROLLOUT-v1.7.0_gg1.md` and `ROLLOUT-v1.7.0_gg2.md` ride the release merge, in the same rehearsed tip. There is no docs-only merge. The reviewer prompt names both notes.

### Rollback and clean stop
- **D-03:** Rollback redeploys the published `v1.7.0_gg3` artifact. The branch rollback restores the `pre-v1.7.0_gg4` tree (Phase 24 D-07 `read-tree` plus commit form). The note lists what rollback brings back.
- **D-04:** In-flight batches are measured live per direction (Phase 24 D-09, `DedupTokenRetryLiveTest` rule). Planned wording: upgrade needs no clean stop for 999.4 because failing inserts stored nothing; rollback needs a clean stop; 999.5 needs the rename as precondition. Research confirms or corrects each claim; no blanket clean stop.

### Review carry-forward and post-tag
- **D-05:** No code or test edit. Only doc-side findings are fixed. Every other open review finding from Phases 25-28 gets a written reason, owner and re-open trigger in a milestone close record (`20-MILESTONE-CLOSE-RECORD.md` shape). Disposition rows are flipped where they live.
- **D-06:** After the tag, a reminder only. PRs #4 (client 0.10.0) and #10 (protobuf-java 4.36.2) stay open, unmerged, not ignored. The close record lists the user's web-UI steps; nothing waits on answers. The pre-gate bundle re-checks OPS-04 (`dependencies` label) and OPS-05 (origin freeze).

### Carried from the roadmap (already locked)
- **D-07:** Recipe: push `sync/v1.7.0` once as a scratch branch and wait for green; `--no-ff` merge from a throwaway worktree so the merged tree equals the rehearsed tip; push `custom_build` only and wait for its single run; push tags by name after that run is green; delete the scratch branch.
- **D-08:** Approval gates run orchestrator-inline (executors refuse relayed approvals; `custom_build` is the protected base). Record `SELECT version()` at each gate.
- **D-09:** The note labels each claim "measured", "read from connector code" or "inferred, to be pinned live". Budget two to three review rounds (max three), reviewer fix text is itself reviewed, finding IDs are never reused.
- **D-10:** Before the note states them, re-check the Kafka 4.4.0 upgrade notes and the guava advisory identity.

### Claude's Discretion
- Plan split and wave order across docs, measurements, review rounds and the publication gates.
- Exact wording and structure of the note, within D-09 labelling.

</decisions>

<canonical_refs>
## Canonical References

**Downstream agents MUST read these before planning or implementing.**

### Phase and requirements
- `.planning/ROADMAP.md` §Phase 29 — goal, success criteria, ordering note
- `.planning/REQUIREMENTS.md` — PUB-11, ROLL-04, ROLL-05, OPS-05, OPS-06, PUB-12, PUB-13

### Prior release recipe
- `.planning/milestones/v1.7.0_gg3-hardening-phases/24-release-publication/24-CONTEXT.md` — D-01..D-12 recipe being repeated
- `.planning/milestones/v1.7.0_gg3-hardening-phases/24-release-publication/24-MILESTONE-CLOSE-RECORD.md` — close record shape
- `docs/ROLLOUT-v1.7.0_gg1.md`, `docs/ROLLOUT-v1.7.0_gg2.md`, `docs/ROLLOUT-v1.7.0_gg3.md` — notes to correct / model on
- `docs/FORK-DELTA.md` — rows and raw numbers refreshed once here

### Phase inputs
- `.planning/phases/25-dependency-absorption-upstream-mini-sync/25-COMMAND-EVIDENCE.md`, `.planning/phases/26-size-named-tuple-elements-in-discovery/26-COMMAND-EVIDENCE.md`, `.planning/phases/27-writer-failure-path-test-debt/`, `.planning/phases/28-raw-identifier-names-in-discovery/` — measurements and the Phase 29 handoff records
- `.planning/MERGE-CHECKLIST-v1.7.0-tail.md`, `fork-anchors.txt`, `fork-anchor-exemptions.txt`, `verify-merge-invariants.sh` — anchors gate

</canonical_refs>

<code_context>
## Existing Code Insights

### Reusable Assets
- `verify-merge-invariants.sh` with `PRE_TAG`, exemption rows: the cumulative gate.
- Phase 24 plans and evidence files: the rehearse/measure/approve/publish recipe.

### Established Patterns
- Operator commands are run live and transcripts kept in a `*-COMMAND-EVIDENCE.md`.
- Local tags: `pre-v1.7.0-sync`, `pre-v1.7.0-tail`, `baseline-v1.7.0_gg4` exist; `pre-v1.7.0_gg4` does not yet.

### Integration Points
- `VERSION` and its anchor row change in the same commit; `CHANGELOG.md` gets a `1.7.0_gg4` section.
- `origin/custom_build` is at `69cd680b` (verified 2026-10-23, unchanged since the DEPS-02 merge).

</code_context>

<specifics>
## Specific Ideas

No specific requirements — follow the Phase 24 recipe.

</specifics>

<deferred>
## Deferred Ideas

None — discussion stayed within phase scope. PRs #4 and #10 remain open by earlier decision (2026-10-21).

</deferred>

---

*Phase: 29-release-publication*
*Context gathered: 2026-10-23*
