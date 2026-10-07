# Phase 11: Full Suite, CI & Publication - Discussion Log

> **Audit trail only.** Do not use as input to planning, research, or execution agents.
> Decisions are captured in CONTEXT.md — this log preserves the alternatives considered.

**Date:** 2026-09-20
**Phase:** 11-Full Suite, CI & Publication
**Areas discussed:** Publication sequence & merge shape, What blocks the push, CI shape & the wall-clock measurement, SUITE-03's fork-delta record

---

## Publication sequence & merge shape

### Q1 — How should `custom_build` be brought forward from `sync/v1.6.0`?

| Option | Description | Selected |
|--------|-------------|----------|
| `--no-ff` merge commit | Forces a merge commit despite a fast-forward being available; the only shape giving a single-command `git revert -m 1` undo on a published branch. Cost: the merge commit is a SHA CI has never run | ✓ |
| Fast-forward | `custom_build`'s tip becomes byte-identical to the scratch tip, so the scratch CI run *is* the run on the published SHA. Cost: no single revertable commit for the sync | |
| Squash into one commit | Trivially revertable and readable. Cost: destroys the merge history the milestone's strategy was built to preserve | |

**User's choice:** `--no-ff` merge commit
**Notes:** Rated **one-way** in CONTEXT.md — once pushed, the shape of the published history cannot be changed without a force-push the milestone forbids.

### Q2 — Where does CI first see the merged tree, and which run is CI-06's evidence?

| Option | Description | Selected |
|--------|-------------|----------|
| Scratch first, then the merge commit | Push `sync/v1.6.0`, require green; then merge, push `custom_build`, and that second run is CI-06's evidence. Two runs | ✓ |
| Only the published SHA | One run, but the first-ever CI exposure of 697 commits happens after publication | |
| Scratch only, argue tree identity | One run, full rehearsal, but substitutes an argument for a measurement | |

**User's choice:** Scratch first, then the merge commit
**Notes:** Directly coupled to Q1 — the `--no-ff` merge commit is a new SHA, so the second run is not optional under this merge shape.

### Q3 — Where does the human approval gate sit?

| Option | Description | Selected |
|--------|-------------|----------|
| One blocking gate right before the `custom_build` push | Everything upstream runs unattended; the gate sees the full evidence bundle. The scratch push stays ungated because it is reversible | ✓ |
| Two gates — before the scratch push and before the `custom_build` push | Treats making the tree public as its own act. Cost: an extra stop in a `yolo` milestone for something a branch deletion undoes | |
| Approve the whole sequence up front | Fastest. Cost: approval given before the CI evidence it should weigh exists | |

**User's choice:** One blocking gate right before the `custom_build` push
**Notes:** The evidence bundle is enumerated in CONTEXT.md D-03 and must be re-measured, not cited.

### Q4 — What else rides along with the publication push?

| Option | Description | Selected |
|--------|-------------|----------|
| Branch + `pre-v1.6.0-sync` + `v1.6.0_gg1` tag; scratch deleted | `origin` carries zero tags today, so the rollback point exists only on this laptop | ✓ |
| Branch + rollback tag, keep the scratch branch | No release tag; two refs at the same tree | |
| Branch only — leave `origin` tagless | Minimal public surface. Cost: the rollback point stays local-only | |

**User's choice:** Branch + rollback tag + `v1.6.0_gg1` tag; scratch deleted
**Notes:** Surfaced during discussion: the push is ~725 commits, not 697 — local `custom_build` was already 28 ahead of `origin/custom_build`.

---

## What blocks the push

### Q1 — How does Phase 11 reach zero pending Section 2 checklist rows?

| Option | Description | Selected |
|--------|-------------|----------|
| Phase 11 does the reads and flips them | The whole surface is ~16 insertions / 23 deletions across 9 files, two of them byte-identical to upstream | ✓ |
| Flip with an annotate-don't-flip caveat | Costs nothing. Cost: a `done` row outrunning its evidence, in the phase that asserts the invariant | |
| Leave them pending and restate the gate | Most honest about not doing the work. Cost: rewrites a criterion at the moment it would fail | |

**User's choice:** Phase 11 does the reads and flips them
**Notes:** Diff sizes were measured live during the discussion and are recorded in CONTEXT.md's measured-state table.

### Q2 — What happens to CR-01's fail-open invariant script?

| Option | Description | Selected |
|--------|-------------|----------|
| Fix the exit-code branching, then run it unattended as gate evidence | `.planning/` script only — cannot affect the suite or CI evidence | ✓ |
| Re-derive by hand, leave the script alone | No file changes. Cost: the checked-in detector still fails open | |
| Drop the script from the publication gate | Cost: discards the only automated guard against a marker reaching the published branch | |

**User's choice:** Fix it, then run it unattended as gate evidence
**Notes:** Carried in `STATE.md` as "fix before Phase 11" since the Phase 6 security audit; listed as still unowned in Phase 10's CONTEXT.

### Q3 — Phases 07 and 09 have no `SECURITY.md` while `security_enforcement` is `true`.

| Option | Description | Selected |
|--------|-------------|----------|
| Run `/gsd-secure-phase 07` and `09` before the gate | Both changed production code that ships publicly; Phase 7's gap-closure fixed an actual injection breakout | ✓ |
| Run 07 only, record 09's omission | One audit instead of two | |
| Record a deliberate omission for both | Zero added work. Cost: "we turned the gate on and skipped it twice" on the record | |

**User's choice:** Run both
**Notes:** An open high-severity finding becomes a blocking checkpoint, not automatic scope expansion — `security_block_on` is `high`.

### Q4 — Do any of the four open code-review findings get fixed before publication?

| Option | Description | Selected |
|--------|-------------|----------|
| Publish with all four open and recorded | Any production edit invalidates both the local and the CI evidence and forces a full gate re-run | ✓ |
| Fix the two one-token ones first, then gate | Cheap if sequenced first. Cost: reaches into two other phases' findings against the locked ownership rule | |
| Fix all open Warnings | Largest pre-publication code delta immediately before a one-way push | |

**User's choice:** Publish with all four open and recorded
**Notes:** They are not silently dropped — each is listed in `docs/FORK-DELTA.md` § "Known costs and open items" and stays a Future Requirement.

---

## CI shape & the wall-clock measurement

### Q1 — Keep or drop `CLICKHOUSE_VERSION: latest` in `tests.yaml`?

| Option | Description | Selected |
|--------|-------------|----------|
| Drop it — let CI exercise the code default | The canary is now the detector; the pin is what made the reversion invisible from CI | ✓ |
| Keep it — belt-and-braces | Zero change. Cost: preserves the CI/local divergence that hid the `25.8.18.1` reversion | |
| Keep it, add a comment | Redundancy plus documentation. Cost: two sources of truth | |

**User's choice:** Drop it
**Notes:** Two facts surfaced live: `CLIENT_VERSION: V2` is redundant on the same argument (`extractClientVersion()` returns `V2` for anything not literally `V1`), and `ClickHouseTestHelpers.java:32-34`'s Javadoc asserts the CI pin, so it goes stale in the same commit.

### Q2 — What makes CI-05's wall-clock criterion binary?

| Option | Description | Selected |
|--------|-------------|----------|
| Measure, assert headroom against a stated threshold (22.5 min of 45), change nothing if it holds | Turns "measured" into pass/fail | ✓ |
| Measure, then tighten `timeout-minutes` to fit | Makes the timeout a real guard. Cost: a legitimately slow run can be killed | |
| Record the number, no threshold | Simplest reading. Cost: a criterion that cannot fail is not a gate | |

**User's choice:** Measure, assert headroom against a stated threshold
**Notes:** Local single-fork was 172s and the runner's `maxParallelForks` resolves to the same 1, so the expectation is wide headroom.

### Q3 — What is the rule if the scratch CI run is red?

| Option | Description | Selected |
|--------|-------------|----------|
| Attribute first (CI-only vs genuine), then decide at a checkpoint | Re-run the failing class locally before touching anything. Explicitly not re-run-until-green | ✓ |
| One blind re-run, then attribute | Cheaper on flakes. Cost: a genuine order/resource-dependent defect looks exactly like a flake | |
| Any red blocks publication, full stop | Maximum strictness. Cost: an infrastructure hiccup stalls publication with no distinguishing path | |

**User's choice:** Attribute first, then decide at a checkpoint
**Notes:** Mirrors Phase 10's D-06, re-pointed from server-version to runner-environment.

### Q4 — What is Phase 11's local gate?

| Option | Description | Selected |
|--------|-------------|----------|
| One cache-busted full run on the exact published tree, plus Docker-free `testUnit` | `--rerun` mandatory; Phase 10 already proved order-independence | ✓ |
| Two consecutive cache-busted runs, matching Phase 10's bar | Strongest local evidence. Cost: re-pays for a property Phase 10 established | |
| Re-use Phase 10's runs — no new local run | Fastest. Cost: the published tree would never have been run locally | |

**User's choice:** One cache-busted full run plus Docker-free `testUnit`
**Notes:** The run must be taken after the `tests.yaml`, Javadoc, script and checklist edits — i.e. on the tree as it will be published.

---

## SUITE-03's fork-delta record

### Q1 — Which metric carries the "smaller delta" claim?

| Option | Description | Selected |
|--------|-------------|----------|
| A behaviour-level ledger, with raw numbers disclosed alongside | Answers SUITE-03's first clause literally and makes the second true on its own terms | ✓ |
| File count as the headline (93 → 82) | Simplest defensible number. Cost: says nothing about how much fork logic each file carries | |
| `src/main` line delta only | Cost: `src/main` insertions still grew (1308 → 1358), making the claim false without the deletions figure | |

**User's choice:** A behaviour-level ledger, with the raw numbers disclosed
**Notes:** The uncomfortable number — insertions up 8401 → 9256 — is stated in the document rather than hidden by metric choice.

### Q2 — Where does the record live, and who is it written for?

| Option | Description | Selected |
|--------|-------------|----------|
| A repo doc (`docs/FORK-DELTA.md`), written for the next sync | Sits beside `docs/DESIGN.md` where a maintainer will find it; ships with the published branch | ✓ |
| `.planning/` milestone artifact only | Keeps artifacts together. Cost: buried in GSD bookkeeping | |
| Both — repo doc plus a `.planning/` closeout | Serves both audiences. Cost: two documents that drift | |

**User's choice:** A repo doc (`docs/FORK-DELTA.md`)
**Notes:** Rationale is the seven silently-vanishing fork behaviours — the ledger is what makes the next merge cheaper.

### Q3 — How is "every fork change dropped in favour of upstream" derived?

| Option | Description | Selected |
|--------|-------------|----------|
| Read it out of the checklist and Recorded Facts, then verify each claim against the merged tree | Reuses the gate this milestone built; states what the code contains | ✓ |
| Re-derive from `git diff v1.6.0 sync/v1.6.0` alone | Maximally faithful. Cost: re-does five phases of analysis and loses the "why" | |
| Transcribe the checklist and Recorded Facts | Fastest. Cost: a record of intentions, the failure this milestone caught twice | |

**User's choice:** Checklist + Recorded Facts as the index, verified against the merged tree

### Q4 — Does the record also carry the fork's accepted costs and open items?

| Option | Description | Selected |
|--------|-------------|----------|
| Yes — a "known costs and open items" section, leading on R-01 | Publishing a known silent-data-loss path with no statement of it in the tree is what would bite a stranger | ✓ |
| Only R-01, in the preamble | Short and weighted correctly. Cost: the other four stay undiscoverable | |
| No — ledger only | Cleanest single-purpose doc. Cost: the published repo carries no statement of the data-loss path | |

**User's choice:** Yes — a "known costs and open items" section

---

## Claude's Discretion

- Commit sequence and wave shape (one behaviour per commit, scoped `spotlessApply`, gate after the edits, merge and push last).
- The exact API fields used for the wall-clock measurement (`run_started_at`/`updated_at` vs per-job timing); `gh` is deliberately not installed.
- Whether `CLIENT_VERSION: V2` is dropped alongside `CLICKHOUSE_VERSION` — stated default is to drop both.
- Whether `docs/FORK-DELTA.md` is linked from `README.md` and/or `AGENTS.md`.
- How the skip accounting is presented — re-derived from Phase 11's own run and diffed against Phase 10's named 18.
- The `--no-ff` merge commit message (it is the commit a future `git revert -m 1` will name).

## Deferred Ideas

- `CHANGELOG.md` entry for `v1.6.0_gg1` — asked for by `AGENTS.md`, named by no requirement.
- `maxParallelForks` tuning — deferred out of Phase 10 into here, and deferred again: CI turns out to be the same single-fork shape already measured.
- Gradle caching, a separate `testUnit` CI job, or a matrix in `tests.yaml`.
- R-01's fail-fast fix; WR-01; P4-WR-01; the four open code-review findings.
- Adopting `DebeziumRecordConvertor`, `auto.evolve`, buffered delivery, `retryOnSocketException`.
- Whether the public fork's `main` branch should also be updated from upstream.
