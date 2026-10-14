# Phase 20: Release & Publication - Context

**Gathered:** 2026-10-14
**Status:** Ready for planning

<domain>
## Phase Boundary

Publish `v1.7.0_gg2` on `custom_build`. Four requirements: write and review `docs/ROLLOUT-v1.7.0_gg2.md` before the publication gate (ROLL-02); bump `VERSION` to `v1.7.0_gg2`, add a plain-language `1.7.0_gg2` CHANGELOG section, and give `docs/FORK-DELTA.md` a row for every Phase 17–19 fork-side patch plus a raw-numbers refresh (PUB-07); pass the full suite twice on ClickHouse `latest` × client V2 (second run `--rerun`, 0 failures, 0 errors, every skip named) with `testUnit` green Docker-stopped (SUITE-05); land the release as a non-force `--no-ff` merge after the user's own explicit approval, with exactly one green `tests.yaml` run on the published SHA and the `v1.7.0_gg2` tag pushed only after that run is green (PUB-08).

Starting state: `origin/custom_build` = `be6b8861`; local `sync/v1.7.0` is 117 commits ahead (Phases 17–19 code and planning); `VERSION` still reads `v1.7.0_gg1`; no gg2 rollout note exists.

Locked by ROADMAP/STATE and not re-opened here:
- The Phase 11/15/16 publication recipe: push `sync/v1.7.0` once as a scratch branch (explicit refspec, never at a SHA another pushed branch already holds), wait for green; merge `--no-ff` from a throwaway worktree so the merged tree equals the rehearsed tip; push `custom_build` only; wait for its single run; push the tag by name; delete the scratch branch.
- Merge checks are Phase 16's milestone-specific set: two parents, first parent = `origin/custom_build`, merged tree = rehearsed tip. `verify-merge-invariants.sh`'s upstream-sync parent checks do not apply and must not be "fixed"; the anchors invariant still runs on the branch tip.
- The rollback point is the Phase 16 merge commit, which already carries the branch-only trigger. Re-run the rollout item 8 rollback check against the merge commit before pushing (STATE [Publish] 2026-10-11).
- Every approval gate (rehearsal push, release merge) runs orchestrator-inline and needs the user's own explicit answer; a relayed approval is refused.
- The `VERSION` anchor row in `.planning/scripts/fork-anchors.txt` moves in the bump commit. Docs ship inside the merged tree, written and reviewed before the gate (nothing stranded locally like `505e4981` was).
- Mandated rollout content (ROADMAP criterion 1 + carried WR-02/L-38): the 999.1 value change with "rows already stored wrongly are not repaired", the dedup-token retry consequence verified on `latest`, the empty-CDC skip change, the MBean `version=` change, that the `Nullable(Tuple)` limitation is closed and its discovery WARN is gone, and ledger L-38 (a single-field `Nullable(Tuple)` with a null field now throws a `DataException`, which stops the task under `errors.tolerance=none`).
- The published gg1 rollout and CHANGELOG text stays as released — no edits to gg1 sections or `docs/ROLLOUT-v1.7.0_gg1.md`.
- PUB-07 owns the AGENTS.md pre-PR obligation (VERSION bump + CHANGELOG entry; Phase 19 left both untouched on purpose).
- IN-04 (Tuple Column lacking a declared element) is copied into the milestone close record with its owner and re-open trigger.
- Suite gate mechanics: `env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew ...`; the second run must bust Gradle's cache with `--rerun`.

</domain>

<decisions>
## Implementation Decisions

### gg2 rollout note shape (ROLL-02)
- **D-01:** `docs/ROLLOUT-v1.7.0_gg2.md` is a **delta note** on top of gg1: it covers only what changed gg1 → gg2 and opens by pointing operators coming from pre-gg1 at `docs/ROLLOUT-v1.7.0_gg1.md` first. No content duplicated from the gg1 note. — **Reversibility:** one-way once pushed — `custom_build` is published; later restructuring needs a new commit.
- **D-02:** The 999.1 item gives operators a **ready, runnable SQL detection query per broken shape** — `Nullable(Enum)` slots read back as unexpected NULL (the pre-fix all-NULL read, ledger L-13) and the `Nullable(Tuple)` wrong-row + phantom-row symptom (19-01 spike side (a)) — each proven live on ClickHouse `latest` before publication. The note states rows are not repaired by the connector.
- **D-03:** Phase 16 writing disciplines carry: plain-language prose with planning IDs only as trailing ledger tags (16-D-02), and every quoted command/SQL in changed text run live with output matching the text, transcripts recorded in a Phase 20 command-evidence file under `.planning/phases/20-release-publication/` (16-D-07/D-08).

### Dedup-token retry consequence (ROLL-02)
- **D-04:** The "verified on `latest`" evidence is a **fresh live probe**, not a citation of existing pins: insert a block, retry the same offset range with changed bytes (the gg2 serialization shape), and observe the dedup outcome against `replicated_deduplication_window`, with `SELECT version()` recorded in the phase evidence file.
- **D-05:** The probe is a **committed live JUnit test** beside the existing exactly-once tests, permanently pinning both halves: a byte-identical retry dedups, a changed-bytes retry duplicates (Phase 19 spike D-05 precedent: committed, not throwaway). — **Reversibility:** costly — the test becomes the quoted evidence surface for the rollout item; replacing it later means rebuilding the measurement.
- **D-06:** The rollout item gives a **procedure plus self-check**: stop connectors cleanly so no offset range is left in `BEFORE` state before upgrading, and a self-check query against the KeeperMap state table confirming no `BEFORE`-state ranges before restarting on gg2. Note that `exactlyOnce=false` pipelines are not affected by the dedup-token half.

### Review gate & open advisories
- **D-07:** The pre-gate review is `gsd-code-reviewer`, incremental, over **everything Phase 20 itself commits**: the gg2 rollout note, the CHANGELOG section, the FORK-DELTA edits, the VERSION bump, and the new dedup live test (16-D-09 shape). Findings go into `20-REVIEW-DISPOSITION.md` before the approval is asked for. Phases 17–19 code is not re-reviewed — each already has a dispositioned review.
- **D-08:** Of the six open 19-REVIEW advisories, the **FORK-DELTA per-twin wording finding is fixed in Phase 20** (the phase edits that doc anyway); the code-side warnings (unreachable totality paths, vacuous test assertions) and the three infos are **carried to the milestone close record with a written reason each** — no code churn between Phase 19's green gate and the release. Dispositions recorded in `.planning/phases/19-nullable-enum-tuple-mapping/19-REVIEW-DISPOSITION.md` (Phase 16 D-10 precedent: flip rows where they live).
- **D-09:** `/gsd-secure-phase 19` runs as a Phase 20 prerequisite, **before the merge gate** — the release ships with every phase's threat check done and any finding can still be weighed at the approval.

### CHANGELOG & milestone-close bookkeeping (PUB-07)
- **D-10:** The `1.7.0_gg2` CHANGELOG section covers **behaviour plus a short Build/CI subsection**: Important Changes and Bug Fixes drawn from the Phase 18/19 behaviour ledgers (`18-BEHAVIOUR-LEDGER.md`, `19-BEHAVIOUR-LEDGER.md`), plus a brief note on the CI hardening (SHA-pinned actions, `ubuntu-24.04`, read-only token, `test-head.yaml` deleted) and `lombok.config` (artifact proven byte-identical). gg1's section structure is the template.
- **D-11:** After the tag is pushed, the orchestrator **presents the three manual checks with exact steps, waits for the user's answers in-session, and records the outcomes** in the milestone close record: Dependabot version-updates enablement, the default-branch intent (`main` at Phase 17, undecided), and that `dependabot.yml`'s `exclude-paths` key did not invalidate the config (17-REVIEW WR-01: the `gradle` entry must still run). No agent changes repository settings (D-05 milestone rule, F1).
- **D-12:** A **milestone audit runs** after Phase 20 verification, before `/gsd-complete-milestone`, replacing the stale start-of-milestone snapshot (`.planning/v1.7.0_gg2-hardening-MILESTONE-AUDIT.md`, 2026-10-10, untracked) — so `v1.7.0_gg2-hardening` closes clean instead of a third override closeout.

### Post-research clarifications (2026-10-14, user-answered after 20-RESEARCH.md)
- **D-13:** (amends D-05) The dedup live test pins the **measured** behaviour: a retry carrying the same `insert_deduplication_token` is dropped even when its bytes differ; a different token with identical data lands (measured on ClickHouse `26.9.17.8`). The rollout item states the measured gg1→gg2 consequence — a gg2 retry is dropped if the gg1 attempt landed, old phantom/wrongly-stored rows stay, rows are never written twice — and includes a delta correction of the gg1 note's "can write rows twice" reasoning. The gg1 note file itself stays untouched; the correction lives in the gg2 note.
- **D-14:** (amends D-06) The "`exactlyOnce=false` pipelines are not affected" claim is **verified live before it is stated**: a test proves the dedup-token behaviour with `exactlyOnce=false` on `latest` (the code path sends the token on every partition-aware insert regardless of mode), and the rollout wording follows the observed result.
- **D-15:** The rollback tag is **`pre-v1.7.0_gg2`**, created on the Phase 16 merge commit (`be6b8861`, which already carries the branch-only trigger) and **pushed to origin before the release merge**, pinning the rollback point first. The Phase 16 rollback check (read-tree plus commit; `git revert -m 1` is broken) is re-run against the actual gg2 merge commit before the release push.
- **D-16:** (amends D-02) The detection queries are proven against a **live gg1 fixture**: fixture rows written with a `be6b8861` worktree running gg1 code, then the queries shown to flag those rows and to NOT flag gg2-written rows.

### Claude's Discretion
- Plan split (likely shape: docs + live probe plan, suite-gate plan, inline publication plan — the gate must sit orchestrator-inline).
- Item ordering inside the gg2 note, exact plain-language titles, and the TOC shape.
- Exact SQL of the detection queries and the KeeperMap self-check, provided each is proven live per D-03.
- The dedup live test's class name, placement, and parameterisation.
- The merge commit message (by precedent it names what it publishes so a future `git revert -m 1` reads well) and the approval-bundle contents (Phase 15 D-12 precedent: measured, not cited).
- The scripted form of the merge checks (two parents, first parent, tree equality) and the FORK-DELTA raw-numbers refresh commands.

</decisions>

<canonical_refs>
## Canonical References

**Downstream agents MUST read these before planning or implementing.**

### Scope and requirements
- `.planning/ROADMAP.md` § "Phase 20: Release & Publication" — goal, 4 success criteria, the publication-recipe note, the carried Phase 19 items (WR-02/L-38, PUB-07, IN-04), the manual checks
- `.planning/REQUIREMENTS.md` — ROLL-02, PUB-07, SUITE-05, PUB-08
- `.planning/STATE.md` § Blockers/Concerns — the [Publish], [CI] and [Env] items (rollback re-check before pushing, one-branch-per-SHA, unauthenticated REST API for CI runs, `TESTCONTAINERS_HOST_OVERRIDE`)

### Rollout/CHANGELOG source material
- `.planning/phases/19-nullable-enum-tuple-mapping/19-BEHAVIOUR-LEDGER.md` — Phase 19 operator-visible rows including L-38
- `.planning/phases/18-writer-correctness/18-BEHAVIOUR-LEDGER.md` — Phase 18 operator-visible rows (empty-CDC skip, column-naming DataException, log changes)
- `.planning/phases/19-nullable-enum-tuple-mapping/19-SPIKE.md` — the Tuple matrix, the wrong-row + phantom-row symptom quoted by the 999.1 detection query
- `docs/ROLLOUT-v1.7.0_gg1.md` — the note gg2 deltas against; its item format and self-check discipline; item 8 rollback re-check
- `docs/FORK-DELTA.md` — rows to verify/refresh; the per-twin wording advisory fixed here (D-08)
- `CHANGELOG.md` — the `1.7.0_gg1` section as the structural template

### Review dispositions
- `.planning/phases/19-nullable-enum-tuple-mapping/19-REVIEW.md` and `19-REVIEW-DISPOSITION.md` — the six open advisory findings D-08 dispositions
- `.planning/phases/16-early-docs-publication/16-CONTEXT.md` — D-01..D-12 precedents carried here (writing disciplines, review shape, red-CI handling D-11)

### Publication mechanics
- `.planning/scripts/fork-anchors.txt` — the `VERSION` row moves in the bump commit
- `.planning/scripts/verify-merge-invariants.sh` — anchors invariant on the branch tip; its merge-parent checks do NOT apply to this merge
- `.planning/milestones/v1.7.0_gg1-sync-phases/15-suite-gate-publication-rollout/15-CONTEXT.md` — D-11..D-13 publish mechanics and the approval-bundle shape
- `AGENTS.md` — pre-PR obligations (VERSION bump, CHANGELOG), test rules

</canonical_refs>

<code_context>
## Existing Code Insights

### Current state (scouted 2026-10-14)
- `origin/custom_build` = `be6b8861`; local `sync/v1.7.0` = 117 commits ahead (all Phase 17–19 work, from `d00ff328` ci(17-01) to the Phase 19 bookkeeping tip). `VERSION` = `v1.7.0_gg1`; `docs/` has no gg2 note.
- `.planning/state.json` is tracked with uncommitted local edits; `.planning/v1.7.0_gg2-hardening-MILESTONE-AUDIT.md` is untracked and stale (start-of-milestone snapshot).
- The `tests.yaml` trigger is branch-only, so the rehearsal push and merge push each start exactly one run and the tag push starts none. CI runs are checked via the unauthenticated GitHub REST API (`actions/runs?head_sha=`); `gh` is not installed.

### Reusable Assets
- Phase 16's merge-gate script shape (allow-list/tree-equality assertions) adapts to the Phase 20 checks (two parents, first parent, merged tree = rehearsed tip).
- `18-04`'s exactly-once/byte-identity pins (`dc489019`) and the existing KeeperStateProvider live tests — the neighbourhood for the new dedup live probe (D-05).
- `16-COMMAND-EVIDENCE.md` format — the template for Phase 20's command/SQL transcripts (D-03).

### Integration Points
- The dedup live test rides the suite gate (SUITE-05) and is quoted by the rollout item (D-04/D-06).
- The behaviour ledgers (18, 19) feed both the rollout note and the CHANGELOG section; FORK-DELTA rows already exist per commit (D-13 discipline) — Phase 20 verifies coverage and refreshes raw numbers only.

</code_context>

<specifics>
## Specific Ideas

- The gg2 note opens with an explicit "coming from a release before v1.7.0_gg1? Work through docs/ROLLOUT-v1.7.0_gg1.md first" line.
- Detection queries are per-shape and runnable as-is against an operator's own table names with minimal substitution.

</specifics>

<deferred>
## Deferred Ideas

- The five carried 19-REVIEW advisories (two code warnings, three infos) — milestone close record, written reason each (D-08); any fix belongs to a future milestone.
- Upgrading the other upstream workflows (`integrationTests.yaml`, `release.yaml`, `pre-release.yaml`, `increment-version.yml`) off `@v3`-era actions — already in REQUIREMENTS Future.
- Repairing rows already stored wrongly by 999.1 — operator data; the connector never rewrites it (REQUIREMENTS Out of Scope; the rollout note documents detection only).

</deferred>

---

*Phase: 20-release-publication*
*Context gathered: 2026-10-14*
