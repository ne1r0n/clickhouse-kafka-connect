# Phase 11: Full Suite, CI & Publication - Context

**Gathered:** 2026-09-20
**Status:** Ready for planning

<domain>
## Phase Boundary

Prove the merged connector is accepted by the suite the previous milestone was built to provide, then publish it: one cache-busted full `./gradlew test` on ClickHouse `latest` × client V2 over the exact tree being published, a CI run machine-verified green on the pushed SHA, `custom_build` brought forward from `sync/v1.6.0` by a `--no-ff` merge and pushed to the public fork after human approval, and the shrunken fork delta written down for whoever runs the next sync.

Delivers: the last pre-push debt closed (6 Section 2 checklist rows read and flipped, CR-01's fail-open invariant script fixed and run, `SECURITY.md` for Phases 07 and 09); `tests.yaml` stripped of its redundant `CLICKHOUSE_VERSION` pin so CI exercises the code default; a CI wall-clock measurement asserted against a stated threshold; `docs/FORK-DELTA.md` as a behaviour-level ledger with a "known costs and open items" section; the publication sequence itself.

Gate: **one** `env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew test --rerun` on the final tree at zero failures/zero errors with the skip inventory non-growing, **plus** Docker-free `testUnit` with Docker genuinely stopped, **plus** a green CI run on the pushed `custom_build` SHA — see D-12, D-02.

Out of this phase: fixing any of the four open code-review findings (D-08); R-01's fail-fast fix and every other Future Requirement; `maxParallelForks` tuning; Gradle caching or a `testUnit` job in CI; adopting `DebeziumRecordConvertor` / `auto.evolve` / buffered delivery; production rollout of the v1.6.0-based connector.

### Measured starting state

Re-derived at discussion time against `sync/v1.6.0` @ `c48754be` — **re-confirm by command before planning; do not re-derive by reading this table.**

| Fact | Evidence |
|---|---|
| The full suite is **already green twice** on `latest` | Phase 10 plan 10-02: 654 tests, 636 succeeded, 0 failed, 0 errors, 18 skipped; wall-clock 172s and 174s (second forced with `--rerun`); byte-identical 57-class executed sets |
| The 18 skips are **already named and explained** | `10-02-SUMMARY.md` § "Named Skip Inventory" — SUITE-01's hardest clause is substantially pre-discharged; all 18 are `@Disabled` or `Assumptions.assumeTrue(isCluster \|\| isCloud)` |
| **SUITE-02 already holds** | 10-02: "None of the 18 skips belong to the fork's own coverage (SMT sink-landing, JSON/Dynamic round-trip, RowBinary fail-fast, JMX registration, insert-columns) — all of those ran and passed in both runs" |
| CI has **never seen the merged tree** | `git ls-remote --heads origin` → only `refs/heads/custom_build` @ `a295af8a` and `refs/heads/main`. `sync/v1.6.0` is unpushed |
| CI **works** on the public fork | GitHub REST `actions/runs`: 6 runs, most recent `conclusion: success` on `custom_build` @ `a295af8a` (2026-09-18), `path: .github/workflows/tests.yaml`, `event: push`, `run_number: 32` |
| `custom_build` is an **ancestor** of `sync/v1.6.0` | `git rev-list --left-right --count custom_build...sync/v1.6.0` → `0  697`. A plain `git merge` **fast-forwards**; `--no-ff` must be forced (D-01) |
| Local `custom_build` is **28 commits ahead** of `origin/custom_build` | `git branch -vv` → `[origin/custom_build: ahead 28]`. Publication pushes ~**725** commits, not 697 |
| `origin` carries **zero tags** | `git ls-remote --tags origin` → empty. `pre-v1.6.0-sync` (`6ac2d20c`) — the rollback point the entire merge strategy rests on — exists **only on this laptop** (D-04) |
| **6 checklist rows still pending**, none owned by Phase 10 | all Section 2 "merged clean, READ ANYWAY": `Record.java`, `Data.java`, `StructToJsonMap.java`, the five convertors (one row), the two state providers (one row) — Phase 8; `settings.gradle.kts` — Phase 6 |
| Those 6 rows cover a **trivially small** surface | `git diff --shortstat v1.6.0 sync/v1.6.0 --` per path: `Record.java` 2+/3−, `Data.java` 1+/2−, `StructToJsonMap.java` 5+/6−, `convert/` 7+/11− across 4 files, `InMemoryState.java` 0+/1−, and `KeeperStateProvider.java` + `settings.gradle.kts` **byte-identical to upstream** (empty diff) |
| **CR-01 still fails open** | `.planning/scripts/verify-merge-invariants.sh:44` — `if git grep -q -- "^${MARKER}" "${c}" -- … 2>/dev/null; then`. Exit ≥2 (broken pathspec, unreadable commit) is indistinguishable from exit 1 (clean), stderr discarded |
| Phases **07 and 09 have no `SECURITY.md`** | `ls .planning/phases/*/[0-9]*-SECURITY.md` → only `06-`, `08-`, `10-`, while `config.json` has `workflow.security_enforcement: true` |
| CI runner fork shape is the **same shape already measured** | `build.gradle.kts:190-196` → `maxParallelForks = availableProcessors()/2`; a 2-core runner resolves to **1**. Phase 10 observed `sharedContainerStarts() = 1` in both local runs — local was already single-fork, so 172s is a fork-count-representative number |
| `CLIENT_VERSION: V2` in CI is **also redundant** | `ClickHouseTestHelpers.extractClientVersion()` (`:91-97`) returns `"V2"` for anything that is not literally `"V1"` |
| ⚠ `ClickHouseTestHelpers`' D-04 Javadoc **asserts the CI pin** | `:32-34` — "CI pins `CLICKHOUSE_VERSION: latest` in its job environment, so the regression was invisible there". D-09 makes that sentence false the moment it lands |
| ⚠ Fork delta **shrank by files, grew by lines** | src+`build.gradle.kts`+`gradle`+`.github`: `93 files, 8401+/423−` (`v1.2.9`→`pre-v1.6.0-sync`) → `82 files, 9256+/449−` (`v1.6.0`→`sync/v1.6.0`). `src/main` only: `32 files, 1308+/139−` → `30 files, 1358+/97−`. Insertions grew because 371 fork-only tests plus this milestone's canaries are the previous milestone's deliverable |
| `docs/` already exists as a home for a repo doc | `docs/DESIGN.md` plus the architecture diagrams; `AGENTS.md` links it |
| `CHANGELOG.md`'s top entry is **upstream's** `1.6.0, 2026-09-11` | no fork entry for `v1.6.0_gg1`, though `AGENTS.md` § "Before submitting a PR" asks for one. Not a requirement — see Deferred Ideas |

### What this means for the phase's shape

**The suite work is largely done; the risk has moved to CI and to the one-way push.** SUITE-01's run, its skip accounting and SUITE-02's SMT clause were all substantially discharged by Phase 10's acceptance gate. What is genuinely unproven is everything downstream of the laptop: 697 merged commits have never met a 2-core runner, a cold Gradle cache or a freshly pulled floating `latest` image, and the act that ends the milestone cannot be undone by re-running it.

This phase is therefore shaped as **rehearse → close the debt → measure → approve → publish**, not as test work.

</domain>

<decisions>
## Implementation Decisions

### Publication sequence & merge shape

- **D-01: Bring `custom_build` forward with a forced `--no-ff` merge commit**, even though `custom_build` is an ancestor and a fast-forward is available.
  - **Why:** it is the only shape that leaves a single-command undo on a branch that must never be force-pushed. `git revert -m 1 <that commit>` backs out the entire 697-commit bring-forward at once. Under a fast-forward there is no commit that means "the sync" — `673d29c3` is only the 581-commit upstream merge, and Phases 7–10's re-applications sit linearly on top, so a rollback would need a range revert or a force-reset.
  - **Accepted cost:** the merge commit is a SHA CI has never run, which is what forces D-02's second run.
  - Rejected — *fast-forward*: cheaper and tighter on evidence (the scratch CI run **is** the run on the published SHA), but it makes ROADMAP's "keeps `git revert -m 1` viable" rationale weaker than it reads.
  - Rejected — *squash to one commit*: contradicts the milestone's locked "history preserved, no rebase, no replay" decision and destroys upstream's 581-commit attribution.
  - **Reversibility:** **one-way** — once pushed, the shape of the published history cannot be changed without a force-push to a branch the milestone forbids force-pushing.

- **D-02: CI rehearses on the scratch branch; CI-06's evidence is the run on the published SHA.** Push `sync/v1.6.0` to `origin` (`on: [push]` fires for any branch) and require that run green. Only then make the `--no-ff` merge locally, push `custom_build`, and take **that** run — located by `head_sha` + workflow `path`, asserting exactly one match — as CI-06's evidence.
  - **Why:** the one-way publication push must never be the first time CI sees this tree. The merge commit's tree is byte-identical to the proven scratch tip, so the second run can only fail on infrastructure.
  - **Cost:** two runs, roughly 2× CI wall-clock.
  - Rejected — *only the published SHA*: the first-ever CI exposure of 697 commits would happen after publication, with no rehearsal and a fix-forward on a public branch.
  - Rejected — *scratch only, argue tree identity*: substitutes an argument for a measurement, which is the exact move Phase 10 named ("green is not the same as correct").
  - **Reversibility:** reversible up to the `custom_build` push — `git push origin --delete sync/v1.6.0` fully undoes the rehearsal.

- **D-03: One blocking human gate, immediately before the `custom_build` push.** Everything upstream of it runs unattended: local gate, debt closure, scratch push, scratch CI green, the fork-delta record, the local `--no-ff` merge.
  - **The gate's evidence bundle** (all of it on the table, none of it cited from a planning document): local run totals and skip count; the scratch CI run URL, `conclusion` and measured duration; `MERGE-CHECKLIST.md` at zero pending rows; `verify-merge-invariants.sh` exit code from an unattended run; the fork-delta figures; the list of knowingly-open items being published (D-08).
  - The scratch push stays **ungated** because it is reversible; the `custom_build` push is not.
  - Rejected — *two gates*: an extra stop in a `yolo`-mode milestone for an act a branch deletion fully undoes.
  - Rejected — *approve up front, run unattended*: the approval would be given before the CI evidence it is meant to weigh exists, making CI-07's "after human approval" a formality.
  - **Reversibility:** n/a — a gate, not an artifact.

- **D-04: Push the branch, the rollback tag, and a `v1.6.0_gg1` tag; delete the scratch branch afterwards.**
  - `custom_build` (~725 commits — the 697 plus the 28 that were already unpushed).
  - `pre-v1.6.0-sync` — **`origin` carries zero tags today**, so the rollback point the whole merge strategy rests on currently exists only on this machine.
  - `v1.6.0_gg1` on the published tip, matching the `VERSION` file. `config.json` already sets `git.create_tag: true`.
  - `git push origin --delete sync/v1.6.0` once merged — it is fully contained in `custom_build`, and a stale scratch branch on a public fork invites a build from the wrong ref.
  - **Reversibility:** **one-way** for the branch push (a published non-force branch cannot be retracted, only reverted forward); tags are deletable but a consumer may already have fetched them.

### Pre-publication debt — what blocks the push

- **D-05: Phase 11 reads the 6 pending Section 2 rows and flips them itself.** Read each path's resolved diff against `v1.6.0`, confirm no fork logic was lost, flip the row citing the command re-run at execution time.
  - **Why it is cheap:** the entire surface is ~16 insertions / 23 deletions across 9 files, and two of the nine (`KeeperStateProvider.java`, `settings.gradle.kts`) are byte-identical to upstream. Under 40 lines of reading.
  - **Why it is not optional:** Phase 11's gate asserts zero pending rows, and plan 10-03 went out of its way so that "a `done` row never outruns its evidence". Flipping six rows whose owning phases never read them would hollow out the gate in the phase that asserts it.
  - Rejected — *flip with an annotate-don't-flip caveat*: uses the right pattern for the wrong purpose — the pattern exists for rows another phase already discharged, not for rows nobody did.
  - Rejected — *leave pending and restate the gate*: rewriting a criterion at the moment it would otherwise fail.
  - **Reversibility:** reversible — a checklist edit.

- **D-06: Fix CR-01's exit-code branching, then run the script unattended as gate evidence.** Branch on `git grep`'s exit code explicitly: `0` = marker found (violation), `1` = clean, `≥2` = tool error (violation, surface stderr rather than discarding it). Then run over `pre-v1.6.0-sync..HEAD` and put the exit code in D-03's bundle.
  - Touches a `.planning/` script only — no production code, no test code, so it cannot affect the suite or the CI evidence.
  - `STATE.md` has carried "fix before Phase 11" since the Phase 6 security audit; Phase 10's CONTEXT listed it as still unowned.
  - Rejected — *re-derive by hand, leave the script alone*: leaves a checked-in detector that fails open, forever.
  - Rejected — *drop the script from the gate*: discards the only automated guard against a conflict marker reaching the published branch, immediately before the push that publishes it.
  - **Reversibility:** reversible.

- **D-07: Run `/gsd-secure-phase 07` and `/gsd-secure-phase 09` before the approval gate.** Both phases changed production code that ships to the public fork — Phase 7 rewrote the writer, helper client and config (and its gap-closure fixed a `x\' OR 1=1-- ` backslash-before-quote breakout into the `system.columns` discovery query), Phase 9 re-applied the metadata-free bypass across five guard sites. Retroactive audits produce two `SECURITY.md` files with no code change unless a threat turns up open.
  - If an audit returns an open high-severity threat, that is a **blocking checkpoint**, not an automatic scope expansion — `security_block_on` is `high`.
  - Rejected — *07 only* / *deliberate omission for both*: leaves "we turned the gate on and skipped it" on the record, in the phase that publishes.
  - **Reversibility:** reversible — documents, unless an audit forces a fix.

- **D-08: Publish with all four open code-review findings open and recorded.** `Column.extractColumn`'s swapped `hasDefaultValue`/`isSubColumn` positional args (dead code today), `ClickHouseSinkConfig`'s `USERNAME` `ConfigDef` default reading `passwordDefault` (`""`) instead of `usernameDefault` (`"default"`), `JsonColumnTest`'s row-count-only assertions, and P9-IN-01's guard dereferencing `csc` before the predicate meant to null-check it.
  - **Decisive argument:** any production edit invalidates both the local gate evidence and the CI evidence and forces the whole gate re-run. Phase 11 is a proof phase, not a fix phase.
  - Supported by the milestone's locked rule — "bug fixes live in the phase whose tests expose them" — and Phase 11 exposes none of these. All four are latent: dead code, a value misreported through Connect's config UI with no runtime effect, a weak assertion, an unreachable null path.
  - They are **not** silently dropped: each appears in `docs/FORK-DELTA.md` § "Known costs and open items" (D-16) and stays a Future Requirement.
  - Rejected — *fix the two one-token ones first*: defensible if sequenced before the local run, but reaches into two other phases' findings against the locked ownership rule.
  - Rejected — *fix all open Warnings*: the largest pre-publication code delta, in a type-mapping path and a test file this phase does not own, immediately before a one-way push.
  - **Reversibility:** reversible — they remain fixable in a follow-up phase.

### CI shape & the wall-clock measurement

- **D-09: Drop the `CLICKHOUSE_VERSION` env pin (value `latest`) from `tests.yaml`.** Phase 10 deferred this decision to here explicitly.
  - **Why:** `ClickHouseVersionDefaultUnitTest` is now the detector and it runs in CI too, so the env pin is no longer protective — it is what made the `latest` → `25.8.18.1` reversion **invisible from CI** for four phases. Dropping it makes CI exercise exactly the configuration a developer gets with nothing set.
  - **Mandatory companion edit:** `ClickHouseTestHelpers.java:32-34`'s Javadoc states "CI pins `CLICKHOUSE_VERSION: latest` in its job environment" — that sentence becomes false in the same commit and must be corrected with it. A stale comment asserting a pin that no longer exists is the next reader's trap.
  - **Apply consistently to `CLIENT_VERSION: V2`** — `extractClientVersion()` returns `"V2"` for anything not literally `"V1"`, so the same argument holds. Planner's call; the stated default is to drop both and keep the matrix declared in the job `name:` and the `FORK MODIFICATION` header comment, updating that comment if it names the env vars.
  - Rejected — *keep it* / *keep it with a comment*: preserves the exact CI-vs-local divergence that hid the reversion.
  - **Reversibility:** reversible.

- **D-10: Measure the CI run's duration and assert headroom against a stated threshold — under half the budget (22.5 min of `timeout-minutes: 45`).** Read it from the API (`run_started_at` → `updated_at`, plus the job timing). Under the threshold: record the number, change nothing. Over it: escalate at D-03's gate with the measurement in hand.
  - **Why a threshold:** CI-05 asks for a measurement; a criterion that cannot fail is not a gate, and this milestone has twice been bitten by evidence nobody acted on. Local single-fork was 172s; the runner adds an image pull, a cold Gradle and weaker CPU, so this should land with wide headroom — the threshold is what makes "measured" binary rather than a note.
  - Rejected — *tighten `timeout-minutes` to ~2× measured*: a floating `latest` image plus a shared container means a legitimately slow run could be killed, and the number needs revisiting as the suite grows.
  - Rejected — *record with no threshold*.
  - **Reversibility:** n/a — a measurement rule.

- **D-11: A red CI run is attributed before anything is touched, then dispositioned at a blocking checkpoint.** Re-run the failing class locally on `latest` first. **Green locally + red in CI** = a CI-environment failure (2 cores, cold cache, image pull, the container's memory cap) and the fix is to the harness or the workflow. **Red in both** = a genuine defect, and the milestone's locked rule sends it to the phase whose tests expose it — which is now this one.
  - Explicitly **not** re-run-until-green: a genuine order-dependent or resource-dependent defect is indistinguishable from a flake under a blind re-run, and this milestone's whole thesis is that a green run can be wrong.
  - Mirrors Phase 10's D-06 attribution rule, re-pointed from server-version to runner-environment.
  - Rejected — *one blind re-run first* / *any red blocks publication with no attribution*.
  - **Reversibility:** n/a — a rule.

- **D-12: Phase 11's local gate is ONE cache-busted full run on the exact published tree, plus Docker-free `testUnit`.**
  - `env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew test --rerun` — **`--rerun` is mandatory**: `STATE.md` records that Gradle's `UP-TO-DATE` cache will otherwise echo Phase 10's results as a fake new run. Pass condition: zero failures, zero errors, and the skip inventory **re-derived from this run** and compared against Phase 10's named 18 (non-growing; any new skip named and explained, discharging SUITE-01's "every skip accounted for").
  - `env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew testUnit` with Docker **genuinely stopped** (colima profile `vz` down, `docker info` failing), re-proving the `@Tag("clickhouse")` contract after a `src/testFixtures` file is touched by D-09.
  - **Why one run and not Phase 10's two:** Phase 10 already established fork-assignment order-independence, and nothing in Phase 11's diff (a CI workflow file, a Javadoc, a `.planning/` script, checklist and requirement edits) can change it. What is unproven is only that the *published* tree is green. CI then supplies a second independent execution on different hardware.
  - The run must be taken on the tree **as it will be published** — after D-05/D-06/D-09's edits, not before.
  - **Reversibility:** n/a — a gate.

### SUITE-03's fork-delta record

- **D-13: The "smaller delta" claim is carried by a behaviour-level ledger, with the raw numbers disclosed alongside it.** Count what the fork still patches and what it retired in favour of upstream — the duplicate `Map(K, Nullable(V))` patch, the client-V2 error-classification workaround, the `DESCRIBE TABLE` database fix, `ProxySinkTask.buildMBeanName` giving way to upstream's `SinkTaskStatistics`, the vestigial JUnit `5.11.3` pin, the `signing`/`buildconfig` plugins, upstream's 56 test files adopted wholesale.
  - Publish the file and line counts next to it **including the uncomfortable one**: files went 93 → 82 and upstream-deletions 139 → 97, but insertions went 8401 → 9256 because 371 fork-only tests plus this milestone's canaries are the previous milestone's deliverable. State it plainly rather than picking the flattering metric.
  - Rejected — *file count as the headline*: says nothing about how much fork logic each file carries.
  - Rejected — *`src/main` line delta only*: `src/main` insertions still grew (1308 → 1358), so that framing makes the claim false unless it leans on the deletions figure, which needs the same explanation the ledger gives directly.
  - **Reversibility:** reversible.

- **D-14: The record lives at `docs/FORK-DELTA.md`, written for whoever runs the next upstream sync.** `docs/` already exists (`DESIGN.md` + diagrams) and `AGENTS.md` links it, so a maintainer will find it; `.planning/` is GSD bookkeeping the next syncer has no reason to open.
  - **Why this audience:** the most expensive fact of this milestone is that **seven** fork behaviours vanished with no conflict marker and no compile error. A tracked ledger of "what this fork still patches and why" is the artifact that makes the next merge cheaper.
  - Rejected — *`.planning/` only*: buried where discovery requires already knowing it exists.
  - Rejected — *both*: two documents that drift and a rule about which is canonical.
  - **Reversibility:** reversible.

- **D-15: Derive the ledger from `MERGE-CHECKLIST.md` and its Recorded Facts, then verify every entry against the merged tree.** The checklist already classifies all 40 conflicted paths as survives-as-is / re-express / redundant, and its Recorded Facts carry the dispositions (FREE-01/02/03, the JMX re-plumb, the KEEP decision, the image-default reversion). Use it as the index; confirm each "dropped" entry by grepping the merged tree so the ledger states what the code contains, not what a plan intended.
  - Rejected — *re-derive from `git diff v1.6.0 sync/v1.6.0` alone*: re-does five phases of analysis, and a diff shows what changed but not why a patch was retired.
  - Rejected — *transcribe as-is*: produces a record of intentions, the exact failure this milestone caught twice.
  - **Reversibility:** reversible.

- **D-16: `docs/FORK-DELTA.md` carries a "Known costs and open items" section, leading on R-01.** `insertFormat=json` + a `Struct` record + default `bypassRowBinary` **silently discards every row** — re-introduced knowingly as an accepted cost by Phase 9's KEEP decision, resting on the operator fact that no live pipeline takes that route today. Anyone building from the public fork needs that in the tree, not buried in `.planning/`.
  - Also listed: WR-01 (`ClickHouseBase.tearDown()` swallowing a database-drop failure under the shared container), the four open review findings from D-08, and the named skip inventory.
  - Rejected — *R-01 only in the preamble* / *ledger only*: the second leaves the published repo with no statement of a known data-loss path.
  - **Reversibility:** reversible.

### Claude's Discretion

The user did not constrain the following. All reversible; a planner may override with a stated reason.

- **Commit sequence and wave shape.** The one-behaviour-per-commit rule inherited from Phase 7 D-19 → Phase 8 D-14 → Phase 9 D-13 → Phase 10 D-11 continues to apply, with `./gradlew spotlessApply` run over **only** the files each commit touches (a tree-wide reformat has been pending since the merge and would destroy the single-behaviour diff property). Natural ordering: debt closure (D-05/D-06/D-07) and the `tests.yaml` + Javadoc edit (D-09) are independent of each other; the local gate (D-12) must follow all of them because it must run on the published tree; the scratch push and its CI run follow the gate; `docs/FORK-DELTA.md` (D-13..D-16) is independent and can land any time before the approval gate; the merge and push are necessarily last.
- **The exact threshold phrasing and the API fields used for D-10's measurement** — `run_started_at`/`updated_at` on the run, or the per-job timing, or both. `gh` is deliberately not installed (D-11 of an earlier phase), so the unauthenticated REST API (60 req/hr) or the web UI is the mechanism.
- **Whether `CLIENT_VERSION: V2` is dropped alongside `CLICKHOUSE_VERSION`** (D-09) — stated default is to drop both.
- **Whether `docs/FORK-DELTA.md` is linked from `README.md` and/or `AGENTS.md`.** Linking it is the cheap half of D-14's discoverability argument.
- **How SUITE-01's skip accounting is presented** — re-derived from Phase 11's own run per D-12 and diffed against Phase 10's named 18, rather than re-authored from scratch.
- **The merge commit message for D-01's `--no-ff` merge.** It is the commit a future `git revert -m 1` will name, so it is worth writing as a pointer to `docs/FORK-DELTA.md` and `MERGE-CHECKLIST.md`.
- **No new metric.** `AGENTS.md` asks that metrics land in `SinkTaskStatistics`/`TopicStatistics`; this phase changes no production insert path.

</decisions>

<canonical_refs>
## Canonical References

**Downstream agents MUST read these before planning or implementing.**

### Milestone contract
- `.planning/ROADMAP.md` § "Phase 11: Full Suite, CI & Publication" — the five success criteria this phase is judged against. Criterion 1's local run is raised in shape (cache-busting) but **lowered in count** versus Phase 10's two-run gate; D-12 states why
- `.planning/ROADMAP.md` § "Merge Mechanics (applies to every phase)" — the scratch-branch / non-force-merge rule this phase executes, the no-`.planning`-churn-during-conflict-resolution cadence, and the **mandatory `env -u TESTCONTAINERS_HOST_OVERRIDE` prefix**
- `.planning/REQUIREMENTS.md` § "Suite & CI (SUITE)" lines 112-119 — `SUITE-01`, `SUITE-02`, `SUITE-03`, `CI-05`, `CI-06`, `CI-07`, this phase's six requirements and the only ones still `Pending` in the whole milestone
- `.planning/REQUIREMENTS.md` § "Locked decisions this milestone is built on" — the `Done` row ("`./gradlew test` green locally + `tests.yaml` green on the pushed SHA + `custom_build` pushed + `VERSION` = `v1.6.0_gg1`") is the literal definition this phase satisfies; the conflict policy and the "bug fixes live in the phase whose tests expose them" rule D-08 and D-11 operate under
- `.planning/REQUIREMENTS.md` § "Future Requirements" — where D-08's four findings and R-01's fail-fast fix stay; `docs/FORK-DELTA.md` (D-16) must not contradict this list
- `.planning/REQUIREMENTS.md` § "Out of Scope" — `src/integrationTest` is compile-only; no cloud/cluster jobs; no production rollout
- `.planning/MERGE-CHECKLIST.md` — **6 pending rows remain, owned by Phases 6 and 8** (D-05); Section 1's 40-path classification and the Recorded Facts are D-15's index for the fork-delta ledger. The document's own opening paragraph states Phase 11's zero-pending assertion
- `.planning/PROJECT.md` § Key Decisions — "Test matrix: ClickHouse `latest` × client V2 only" is what D-09's dropped pin now rests on

### Prior phase context
- `.planning/phases/10-test-harness-reconciliation/10-02-SUMMARY.md` § "Named Skip Inventory" — the 18 named skips D-12 diffs against, and the `--rerun` cache-busting lesson
- `.planning/phases/10-test-harness-reconciliation/10-02-SUMMARY.md` § "Checkpoint Decision (Task 3)" — the local wall-clock figures (172s / 174s) and `sharedContainerStarts() = 1`, which is why D-10 expects wide CI headroom
- `.planning/phases/10-test-harness-reconciliation/10-CONTEXT.md` § D-06 — the attribution-before-fixing rule D-11 re-points from server-version to runner-environment
- `.planning/phases/10-test-harness-reconciliation/10-CONTEXT.md` § Deferred Ideas — raises the `tests.yaml` `CLICKHOUSE_VERSION` question (D-09), the `maxParallelForks` question (deferred again), and names CR-01 as "must be fixed before Phase 11"
- `.planning/phases/09-metadata-free-json-path-keep-or-drop/09-CONTEXT.md` — the KEEP decision whose accepted cost D-16 publishes
- `.planning/phases/08-orchestration-jmx-re-plumbing/08-CONTEXT.md` § D-13/D-15 — annotate-don't-flip for cross-phase bookkeeping, and scoped `spotlessApply` only
- `.planning/phases/07-type-mapping-writer-helper-client-config/07-REVIEW.md` and `.planning/phases/09-metadata-free-json-path-keep-or-drop/09-REVIEW.md` — the four open findings D-08 publishes and D-16 lists

### Files this phase touches directly
- `.github/workflows/tests.yaml:33-35` — drop `CLICKHOUSE_VERSION: latest` (D-09) and, at the planner's discretion, `CLIENT_VERSION: V2`; update the `FORK MODIFICATION` header comment if it names them. `timeout-minutes: 45` at `:15` is D-10's budget; `on: [push]` at `:8` is what makes D-02's scratch rehearsal possible
- `src/testFixtures/java/com/clickhouse/kafka/connect/sink/helper/ClickHouseTestHelpers.java:32-34` — the D-04 Javadoc that asserts the CI pin; **must be corrected in the same commit as D-09**. `:41` `CLICKHOUSE_VERSION_DEFAULT`, `:67-69` the reader, `:91-97` `extractClientVersion()`
- `.planning/scripts/verify-merge-invariants.sh:44` — CR-01's fail-open `git grep` branch (D-06); `:36` builds the marker at runtime, `:39` is the pathspec
- `.planning/MERGE-CHECKLIST.md` — 6 row flips (D-05) plus any new Recorded Facts
- `.planning/REQUIREMENTS.md` § SUITE — `SUITE-01..03` and `CI-05..07` closed; the Traceability table's six `Pending` rows flipped
- `docs/FORK-DELTA.md` — **new** (D-13..D-16)
- The nine files behind D-05's rows, **read only**: `src/main/java/com/clickhouse/kafka/connect/sink/data/{Record,Data,StructToJsonMap}.java`, `…/sink/data/convert/{EmptyRecordConvertor,RecordConvertor,SchemaRecordConvertor,SchemalessRecordConvertor,StringRecordConvertor}.java`, `…/sink/state/provider/{InMemoryState,KeeperStateProvider}.java`, `settings.gradle.kts`

### Environment and process
- `.planning/STATE.md` § "Blockers/Concerns" — the `TESTCONTAINERS_HOST_OVERRIDE="null"` hazard and colima profile `vz`; the `[Tooling]` row on `gh` not being installed (unauthenticated REST, 60 req/hr); the `[Phase 06]` CR-01 row (D-06); the `[Phase 10 → 11]` Gradle `UP-TO-DATE` row (D-12); the `[Prod]` R-01 row (D-16); the `[Security]` row (D-07); the GSD worktree-isolation quirk needing `--force-isolation none` immediately before each dispatch
- `.planning/STATE.md` § "Operator Next Steps" — the four items this phase is expected to clear
- `.planning/config.json` — `build_command`/`test_command` carry the `env -u` prefix; `git.create_tag: true` (D-04); `granularity: coarse`; `mode: yolo`; `test_gate_timeout: 2400`; `workflow.security_enforcement: true` with `security_block_on: high` (D-07)
- `build.gradle.kts:190-196` — `maxParallelForks` (deliberately untuned, see Deferred); `:239-250` the `testUnit` task with `excludeTags("clickhouse")`
- `.github/scripts/jacoco_summary.py` — CI-05's "publishes coverage" half; `BLD-03` recorded that it renders both XML reports without edits
- `AGENTS.md` § "Before submitting a PR" (VERSION bumped, CHANGELOG updated, `spotlessApply` run), § "Testing Discipline" (only add, never delete or weaken existing tests), § "Review" rules — **do not commit to the branch or change a PR title/description without explicitly asking the user**
- `docs/DESIGN.md` — the existing repo doc `docs/FORK-DELTA.md` sits beside (D-14)
- `.planning/research/PITFALLS.md` — 17 pitfalls with symptom → cause → prevention → detection → owning phase

</canonical_refs>

<code_context>
## Existing Code Insights

### Reusable Assets

- **Phase 10's acceptance-gate procedure is directly reusable** — `docker info` to prove Docker is genuinely down before `testUnit`, `--rerun` to defeat the build cache, the summary listener's `Tests summary:` line as the totals source, and the executed-result-class set as the comparison key. Plan 10-02 is the worked example.
- **The GitHub REST API is already proven usable from this environment** — `curl https://api.github.com/repos/ne1r0n/clickhouse-kafka-connect/actions/runs` returns the run list with `head_sha`, `path`, `status`, `conclusion`, `html_url`, `run_started_at` and `updated_at`, which is exactly CI-06's locator and D-10's measurement source. No `gh` needed.
- **`MERGE-CHECKLIST.md` is already structured as D-15 needs it** — Section 1's per-path `survives-as-is / re-express / redundant` classification plus the Recorded Facts are a ledger index, not prose to mine.
- **`10-02-SUMMARY.md`'s skip table is the diff baseline** for D-12's re-derived inventory; it already names class, test and reason for all 18.

### Established Patterns

- **Evidence re-run at execution time, never cited from a planning document** (plan 10-03's discipline, and the reason its 17 flips are trustworthy). Every fact in this CONTEXT's measured-state table is re-confirmable by one command; D-05, D-06, D-10, D-12 and D-15 all require the command, not the citation.
- **Attribution before fixing** (Phase 10 D-06) — D-11 is the same rule re-pointed at the CI runner.
- **One behaviour per commit with scoped `spotlessApply`** (Phase 7 D-19 → 8 D-14/D-15 → 9 D-13 → 10 D-11).
- **Annotate-don't-flip for rows another phase owns** — note that D-05 deliberately does **not** use it: these rows were never discharged by anyone, so the pattern does not apply.
- **Docker-free canary for a silently-vanishing behaviour** (Phase 9 D-11, Phase 10 D-04) — relevant to D-09: `ClickHouseVersionDefaultUnitTest` is the reason the CI env pin is now redundant rather than protective.

### Integration Points

- **⚠ The publication push is the milestone's only irreversible act.** Everything else in this phase can be re-run, re-measured or reverted locally. D-01, D-03 and D-04 all exist to make that single act recoverable-forward.
- **⚠ `tests.yaml` and `ClickHouseTestHelpers.java` are coupled by a comment, not by code.** The Javadoc at `:32-34` asserts the CI pin that D-09 removes. Nothing will fail if they diverge — which is precisely the class of silent drift this milestone spent five phases chasing.
- **⚠ Every production edit re-opens the gate.** D-08's whole argument. If the planner adds any `src/main` change, D-12's local run and D-02's scratch CI run must both be re-taken on the new tree, and the approval bundle rebuilt.
- **CI concurrency is keyed on the SHA for push events** (`tests.yaml:11`), so D-02's two runs land in different concurrency groups and cannot cancel each other.
- **`custom_build`'s 28 unpushed commits ride along silently.** They are already ancestors of `sync/v1.6.0`, so nothing special happens — but the push is ~725 commits, and anyone reasoning about "697" from the roadmap will be surprised.
- **CI is the only execution on hardware the developer does not control.** With `maxParallelForks = 1` on a 2-core runner and D-01's shared container, that is one container for the whole live suite — the same single-fork shape Phase 10 measured locally, so the first CI run is a hardware comparison, not a topology change.

</code_context>

<specifics>
## Specific Ideas

- **The organising fact of this phase is that the test work is done and the risk has moved downstream.** SUITE-01's run, its 18-skip accounting and SUITE-02's SMT clause were substantially discharged by Phase 10's gate. Three of the six requirements are therefore evidence acts. What is genuinely unproven is CI on a runner nobody has tried and a push that cannot be taken back. The plan should say so plainly rather than presenting SUITE-01/02 as fresh test work — the same correction Phase 10 had to make about `HARN-04/05/06/07`.
- **`--no-ff` is a rollback decision, not a style preference.** On a branch that must never be force-pushed, the only undo is a revert, and under a fast-forward there is no single commit that means "the sync" — you would be reverting a 697-commit range. The extra merge commit buys `git revert -m 1` as a real option, which is what ROADMAP's Merge Mechanics section claims the strategy preserves.
- **Dropping the CI version pin is the correct reading of Phase 10's finding, not a loosening.** The instinct is that removing a pin makes CI weaker. Here the pin was the camouflage: it kept CI green against the right server while every local gate silently moved to the wrong one, so CI could never have reported the regression. With the canary in place, removing the pin turns CI into a real check on the default instead of a machine that routes around it.
- **The fork-delta record's honest number is the awkward one.** Files down 93 → 82, upstream-deletions down 139 → 97, insertions **up** 8401 → 9256. Picking the flattering metric would make SUITE-03 a marketing sentence. The behaviour ledger is the framing where the claim is both true and useful, and the line growth has a one-sentence explanation (371 fork-only tests are the previous milestone's product) that belongs in the document rather than being hidden by it.
- **Publishing a fork with a known silent-data-loss path and no statement of it in the tree is the thing that would bite a stranger.** R-01's KEEP rests on an operator fact — no live pipeline takes the `Struct` + explicit-JSON route today — that is true of *this* deployment and unverifiable for anyone else building from the public fork. D-16 is the cheapest possible mitigation.

</specifics>

<deferred>
## Deferred Ideas

- **`CHANGELOG.md` entry for `v1.6.0_gg1`.** The top entry is still upstream's `1.6.0, 2026-09-11`, and `AGENTS.md` § "Before submitting a PR" asks for a CHANGELOG update. No requirement in this milestone names it, and `docs/FORK-DELTA.md` (D-13..D-16) covers the same ground for the audience that matters. Worth raising at the approval gate as a one-line addition if the user wants it.
- **`maxParallelForks = availableProcessors() / 2` is still untuned for the post-merge suite** — deferred out of Phase 10 into here, and deferred again: the CI shape turns out to be the same single-fork shape already measured locally, so there is no evidence gap to close. Revisit only if D-10's measurement lands near the threshold.
- **Gradle caching, a separate `testUnit` job, or a matrix in `tests.yaml`.** All real improvements, none required by CI-05/06, and each one changes the artifact the publication gate measures.
- **R-01's fail-fast fix** — still a Future Requirement, still unscheduled. D-16 publishes the cost; it does not fix it.
- **The four open code-review findings** (`Column.extractColumn`'s swapped positional args, the `USERNAME` `ConfigDef` default, `JsonColumnTest`'s row-count-only assertions, P9-IN-01's operand swap) — D-08 publishes with them open; each stays a Future Requirement and is listed in `docs/FORK-DELTA.md`.
- **WR-01 / P4-WR-01** — `ClickHouseBase.tearDown()` swallowing a drop failure, and `ClickHouseSinkTaskJsonInsertFormatTest`'s missing `try/finally`. Accepted follow-ups, listed not fixed.
- **Adopting `DebeziumRecordConvertor`, `auto.evolve`, buffered delivery, or `retryOnSocketException`** — real new upstream capability the merge makes available. Explicitly out of scope as feature work; a candidate for the next milestone, and `docs/FORK-DELTA.md` is a natural place to note that they are available-but-unadopted.
- **Whether the public fork's `main` branch should also be updated** from upstream. Untouched by this milestone; `origin/main` is at upstream's `bec167d8`.

### Reviewed Todos (not folded)

`gsd_run query todo.match-phase 11` returned zero matches — no pending todos intersect this phase.

</deferred>

---

*Phase: 11-Full Suite, CI & Publication*
*Context gathered: 2026-09-20*
