# Roadmap: ClickHouse Kafka Connect — `custom_build` Fork Test Coverage

## Milestones

- ✅ **v1.2.9_gg4-tests Fork Test Coverage** — Phases 1-5 (shipped 2026-09-18)
- ✅ **v1.6.0_gg1-sync Upstream v1.6.0 Sync** — Phases 6-11 (shipped 2026-10-07)
- ✅ **v1.7.0_gg1-sync Upstream v1.7.0 Sync & Follow-up Hardening** — Phases 12-15 (shipped 2026-10-10)
- ✅ **v1.7.0_gg2-hardening Follow-up Hardening & CI Hygiene** — Phases 16-20 (shipped 2026-10-14)
- 🚧 **v1.7.0_gg3-hardening Record-Data Exposure & Review Debt** — Phases 21-24 (in progress)

## Phases

<details>
<summary>✅ v1.2.9_gg4-tests Fork Test Coverage (Phases 1-5) — SHIPPED 2026-09-18</summary>

- [x] Phase 1: Test Infrastructure & Green Baseline (3/3 plans) — completed 2026-09-16
- [x] Phase 2: Fork SMT Coverage (5/5 plans) — completed 2026-09-16
- [x] Phase 3: Table Metadata & JSON/Dynamic Types (3/3 plans) — completed 2026-09-17
- [x] Phase 4: JSON Insert Path & Connector Plumbing (3/3 plans) — completed 2026-09-18
- [x] Phase 5: CI & Final Verification (2/2 plans) — completed 2026-09-18

Full phase details: [`milestones/v1.2.9_gg4-tests-ROADMAP.md`](milestones/v1.2.9_gg4-tests-ROADMAP.md)
Requirements as shipped: [`milestones/v1.2.9_gg4-tests-REQUIREMENTS.md`](milestones/v1.2.9_gg4-tests-REQUIREMENTS.md)
Phase artifacts: `milestones/v1.2.9_gg4-tests-phases/`

</details>

<details>
<summary>✅ v1.6.0_gg1-sync Upstream v1.6.0 Sync (Phases 6-11) — SHIPPED 2026-10-07</summary>

- [x] Phase 6: Merge Groundwork & Build Migration (3/3 plans) — completed 2026-09-18
- [x] Phase 7: Type Mapping, Writer, Helper Client & Config (4/4 plans) — completed 2026-09-19
- [x] Phase 8: Orchestration & JMX Re-plumbing (3/3 plans) — completed 2026-09-19
- [x] Phase 9: Metadata-free JSON Path — Keep or Drop (3/3 plans) — completed 2026-09-20
- [x] Phase 10: Test Harness Reconciliation (3/3 plans) — completed 2026-09-20
- [x] Phase 11: Full Suite, CI & Publication (8/8 plans) — completed 2026-10-07

Full phase details: [`milestones/v1.6.0_gg1-sync-ROADMAP.md`](milestones/v1.6.0_gg1-sync-ROADMAP.md)
Requirements as shipped: [`milestones/v1.6.0_gg1-sync-REQUIREMENTS.md`](milestones/v1.6.0_gg1-sync-REQUIREMENTS.md)
Phase artifacts: `milestones/v1.6.0_gg1-sync-phases/`

</details>

<details>
<summary>✅ v1.7.0_gg1-sync Upstream v1.7.0 Sync & Follow-up Hardening (Phases 12-15) — SHIPPED 2026-10-10</summary>

- [x] Phase 12: Upstream v1.7.0 Merge & Reconcile (5/5 plans) — completed 2026-10-08
- [x] Phase 13: Fork Hardening on the Merged Tree (8/8 plans) — completed 2026-10-08
- [x] Phase 14: Dependency Bumps & CI (4/4 plans) — completed 2026-10-10
- [x] Phase 15: Suite Gate, Publication & Rollout (8/8 plans) — completed 2026-10-10

Full phase details: [`milestones/v1.7.0_gg1-sync-ROADMAP.md`](milestones/v1.7.0_gg1-sync-ROADMAP.md)
Requirements as shipped: [`milestones/v1.7.0_gg1-sync-REQUIREMENTS.md`](milestones/v1.7.0_gg1-sync-REQUIREMENTS.md)
Phase artifacts: `milestones/v1.7.0_gg1-sync-phases/`

</details>

<details>
<summary>✅ v1.7.0_gg2-hardening Follow-up Hardening & CI Hygiene (Phases 16-20) — SHIPPED 2026-10-14</summary>

- [x] Phase 16: Early Docs Publication (3/3 plans) — completed 2026-10-11
- [x] Phase 17: CI, Build & Test Hygiene (4/4 plans) — completed 2026-10-12
- [x] Phase 18: Writer Correctness (4/4 plans) — completed 2026-10-12
- [x] Phase 19: Nullable Enum & Tuple Mapping (10/10 plans) — completed 2026-10-13
- [x] Phase 20: Release & Publication (8/8 plans) — completed 2026-10-14

Full phase details: [`milestones/v1.7.0_gg2-hardening-ROADMAP.md`](milestones/v1.7.0_gg2-hardening-ROADMAP.md)
Requirements as shipped: [`milestones/v1.7.0_gg2-hardening-REQUIREMENTS.md`](milestones/v1.7.0_gg2-hardening-REQUIREMENTS.md)
Milestone audit: [`milestones/v1.7.0_gg2-hardening-MILESTONE-AUDIT.md`](milestones/v1.7.0_gg2-hardening-MILESTONE-AUDIT.md)
Phase artifacts: `milestones/v1.7.0_gg2-hardening-phases/`

</details>

### 🚧 v1.7.0_gg3-hardening Record-Data Exposure & Review Debt (In Progress)

**Milestone Goal:** Close the last known record-data exposure (backlog 999.3), clear every review advisory carried from `v1.7.0_gg2-hardening`, and absorb the Dependabot bumps already on `origin/custom_build`. Then publish `v1.7.0_gg3` with the suite green on ClickHouse `latest` × client V2 and CI green on the published SHA.

- [x] **Phase 21: Dependabot Integration & CI Hygiene** - Merge `custom_build`'s three Dependabot commits into the working branch before any code work; close the 17-REVIEW pin-guard gaps; the user disables the inherited-red nightly, checks Dependabot and revokes the stale token (completed 2026-10-19)
- [ ] **Phase 22: Writer Record-Data Exposure** - Backlog 999.3: writer conversion failures name the column, type and exception class without the record value, with retry classification pinned before and after the change; the inline Mockito mock is gone
- [ ] **Phase 23: Declared-Nullability Hardening & Review Closure** - The 19-REVIEW advisories are fixed in `DeclaredNullability` and its tests, IN-04 is decided, and every 17-REVIEW and 19-REVIEW finding is dispositioned
- [ ] **Phase 24: Release & Publication** - `v1.7.0_gg3` published with a reviewed rollout note, two green suite runs and one green CI run on the published SHA before the tag

## Phase Details

### Phase 21: Dependabot Integration & CI Hygiene

**Goal**: Every later phase builds and tests on the dependency versions already published on `custom_build`. The CI pin guard catches the two drift cases 17-REVIEW found. The GitHub-side loose ends left by `v1.7.0_gg2` are closed by the user. No connector behaviour changes.

**Depends on**: Nothing in this milestone. Phase 20 shipped `v1.7.0_gg2` as `a246ca01`; since then the user merged three Dependabot PRs onto `origin/custom_build` (tip `6473e24f`).

**Requirements**: DEPS-01, CI-18, CI-19, OPS-01, OPS-02, OPS-03

**Success Criteria** (what must be TRUE):

  1. `sync/v1.7.0` contains `origin/custom_build`'s three Dependabot commits through a merge, with no rebase and no force: `git merge-base --is-ancestor 6473e24f sync/v1.7.0` succeeds, and the build resolves lombok 1.18.48, spotless 8.10.4 and slf4j-reload4j 2.0.20. Before any code plan of Phases 22-23 starts:
     - spotless 8.10.4 adds no violations: `spotlessApply` on the merged tree gives the same diff as the pre-existing 8.10.1 baseline (46 files, import order only). There is no tree-wide reformat, and every file this phase edits is spotless-clean (amended 2026-10-17 per 21-CONTEXT D-14)
     - `testUnit` is green
     - the build still prints no Lombok "Ambiguous: Jackson2 and Jackson3" warning
     - the `fork-anchors.txt` row and the FORK-DELTA rows that name the old slf4j-reload4j and Lombok versions name the new ones
  2. `TestsWorkflowPinGuardUnitTest` fails if `tests.yaml`'s "Test fork count" step stops reading `reportTestForks`, and fails if the `reportTestForks` task disappears from `build.gradle.kts`. Each failure has been seen red on a fixture.
  3. The pin guard accepts `runs-on: ubuntu-24.04 # comment`, and it no longer reports a step `name:` or `run:` text containing `: write` as a write scope. Each case was seen red against the old regex and green against the fixed one, on a fixture.
  4. The user has disabled the nightly `integrationTests.yaml` in the repository's Actions settings, and the disable date is recorded. The file is byte-identical to upstream `v1.7.0`. Once at least one 05:00 UTC cron time has passed, the REST API lists no `schedule` run of it created after that date.
  5. The user has confirmed, directly and in their own words, that the Dependabot `gradle` entry shows a recent "Last checked" with no configuration error, and that the read-only token is revoked on GitHub. The Keychain lookup for `gsd17-gh-actions-read` finds nothing.

**Plans:** 5/5 plans complete

Plans:
**Wave 1**
- [x] 21-01-PLAN.md — OPS-01..03 operator checks at the start: REST/clock tracer (E-01), blocking-human checkpoint, answers verbatim (E-02) (wave 1)

**Wave 2** *(blocked on Wave 1 completion)*
- [x] 21-02-PLAN.md — DEPS-01 merge of origin/custom_build (6473e24f) with the D-03 edits inside, then build, Lombok, testUnit and spotless baseline-equal evidence (wave 2)

**Wave 3** *(blocked on Wave 2 completion)*
- [x] 21-03-PLAN.md — SHA- and row-pinned anchor exemption in verify-merge-invariants.sh plus fail-first self-test cases ST5-ST9 (D-01, D-02, D-04) (wave 3)

**Wave 4** *(blocked on Wave 3 completion)*
- [x] 21-04-PLAN.md — Pin guard: rules extracted, fixtures red then green, CI-18 fork-step/task rules, CI-19 runs-on/write-scope fixes, D-09 exclude-paths guard (wave 4)

**Wave 5** *(blocked on Wave 4 completion)*
- [x] 21-05-PLAN.md — Phase close gate at the tip, then the OPS-01 closing observation at or after 2026-10-17T12:00:00Z (D-11, D-12) (wave 5)

**Note**: DEPS-01 is the opening plan and gates all code work in this milestone.

- Merge `origin/custom_build` into `sync/v1.7.0`. Do not interleave a `.planning/` commit with it. The local commits past `3879b883` are `.planning/`-only, and the Dependabot diff touches only `build.gradle.kts` and `gradle/libs.versions.toml`, so no conflict is expected.
- `fork-anchors.txt` pins `build.gradle.kts` `slf4j-reload4j:2.0.17` (BLD-08) at count 1. Dependabot #3 (`6473e24f`) changed that line on `custom_build` without touching the anchors file. `verify-merge-invariants.sh` checks every commit in its range against that commit's own copy of the file, and published commits cannot be rewritten. Plan time decides, on the record, where the row moves (in the merge commit itself or in the first commit after it) and which `PRE_TAG` later gates use, so the invariant stays meaningful.
- The FORK-DELTA rows that name `slf4j-reload4j:2.0.17` (BLD-08) and "Lombok 1.18.46" (BLD-14) take the new versions. Raw numbers wait for Phase 24.
- This phase pushes nothing. The Dependabot commits were CI-green on `custom_build`; the merged tree's first CI run is Phase 24's rehearsal.
- Dependabot PR #4 (client `0.10.0`) stays open, unmerged and not ignored. If the user merges further Dependabot PRs onto `custom_build` during the milestone, Phase 24 merges them in before its gate.

CI-18 and CI-19 are test-only. Neither `tests.yaml` nor `build.gradle.kts` is edited for them, and the `timeout-minutes: 45` anchor must survive.

OPS-01..OPS-03 are user actions in the GitHub web UI (D-11). No agent changes repository settings, and a relayed answer is not accepted. Present them at the start of the phase, so OPS-01's no-scheduled-run check has nights to observe. If the rest of the phase finishes before a cron time has passed, verification waits for it. Phase 24 re-checks OPS-01 in its pre-gate bundle. `integrationTests.yaml` is not edited, deleted or fixed.

The 17-REVIEW closures made here (WR-01 residue by OPS-02, IN-01 by CI-18, IN-02 by CI-19) are written into the disposition file by Phase 23 (DEBT-12).

### Phase 22: Writer Record-Data Exposure

**Goal**: A value-conversion failure in the writer tells operators which column, ClickHouse type and exception class failed, in the Connect task status, the DLQ headers and the ERROR logs, without ever carrying the record value. Retry classification is provably unchanged. This closes backlog 999.3 and FORK-DELTA Known costs item 6.

**Depends on**: Phase 21 (code work starts only on the merged tree, with no new spotless violations beyond the 8.10.1 baseline per 21-CONTEXT D-14, and `testUnit` green)

**Requirements**: HARN-08, WRT-13, WRT-14, WRT-15

**Success Criteria** (what must be TRUE):

  1. A Docker-free test pins `Utils.handleException`'s retriable vs non-retriable classification both ways for every failure kind that reaches `doWriteColValue`'s generic catch. It passes on the tree before the writer change and, unchanged, after it.
  2. For an invalid UUID string and for an unparseable number, the Connect task status, the DLQ exception headers (message and stack trace) and both `Error inserting records` ERROR logs of `doInsertRawBinary` name the column, the ClickHouse type and the exception class. A sentinel test asserts that the record value is absent from every message in the cause chain and from every captured log line (T-18-07).
  3. `ClickHouseWriterNullMessageUnitTest` no longer inline-mocks `ClickHouseHelperClient` with Mockito; it overrides the writer one level up, in the `CapturingClickHouseWriter` pattern. Every original assertion of the WRT-11 null-message behaviour is kept, and the Phase 18 deferred item (OpenJ9 `retransformClasses` crash risk) is closed.
  4. FORK-DELTA Known costs item 6, the WRT-12 row of the patch table and this roadmap's backlog 999.3 entry (marked delivered) change in the same commit as the writer code they describe. The anchors invariant passes at every commit in the phase.

**Plans**: 5 plans (sequential, waves 1-5)

Plans:
**Wave 1**
- [ ] 22-01-PLAN.md — HARN-08: ClickHouseWriterNullMessageUnitTest on a Mockito-free CountingHelperClient; opens 22-EVIDENCE.md with the D-16 spotless baseline

**Wave 2** *(blocked on Wave 1 completion)*
- [ ] 22-02-PLAN.md — WRT-14: Docker-free classification pin for 21 failure kinds through the real writer catch, ProxySinkTask and ChunkFlusher, green on the unchanged writer

**Wave 3** *(blocked on Wave 2 completion)*
- [ ] 22-03-PLAN.md — WRT-13 + WRT-15: value-free DataException with a WithheldMessageException cause, D-14 enum text, Docker-free sentinel tests, F2 flip and all doc edits in one fix(22-03) commit

**Wave 4** *(blocked on Wave 3 completion)*
- [ ] 22-04-PLAN.md — WRT-13 live: put() pins on latest x V2 for an invalid UUID and an unparseable Int128, and the D-15 DLQ header facts (live and Docker-free)

**Wave 5** *(blocked on Wave 4 completion)*
- [ ] 22-05-PLAN.md — Phase gate: testUnit --rerun, full suite, anchors invariant, D-16 spotless, scope/order/no-push checks, and E-05 with the SC1..SC4 readings

**Note**: `ClickHouseWriter.java` is a high-risk file (AGENTS.md). Order inside the phase:
1. HARN-08. It moves the existing writer error-path test off the inline mock, so the new pins follow the same pattern and no new test brings the OpenJ9 risk back.
2. WRT-14's classification pin, committed and green on the unchanged writer.
3. The WRT-13 change, with WRT-15's doc edits in the same commit.

The design tension is Phase 18's D-09. The catch rethrows the original exception so that `Utils.handleException` sees the same root cause. Removing the value from every message in the cause chain means the original cannot stay in the chain with its message intact. Two readers downstream must behave exactly as before:
- `Utils.handleException`'s root-cause classification. The same catch also sees `IOException`s from the output stream, not only conversion failures, so a value-free replacement must not turn a retriable failure non-retriable.
- `doInsertRawBinary`'s generic catch, which matches the failure message against `UPDATE_TABLE_EXCEPTION_STR_TO_ERROR_CODE` to decide on a table-mapping refresh. The new message must neither start nor stop matching it.

The `ClassCastException` branch just above the generic catch already throws a column-naming, value-free `DataException`; it is the precedent for the message shape. Keep the `Error writing value of` log prefix for greps. Each message change is operator-visible, so record it as it lands for Phase 24's rollout note.

Phase gate: cache-busted `testUnit` and a full suite on `latest` × V2, plus the anchors invariant.

### Phase 23: Declared-Nullability Hardening & Review Closure

**Goal**: The declared-nullability pass is total for any input, keeps a literal backslash-n inside element names and enum labels, and has a recorded rule for an unpaired declared element, and its tests can actually fail. Every 17-REVIEW and 19-REVIEW finding ends fixed or closed with a written reason.

**Depends on**: Phase 21 (the merged tree; DEBT-12 also records the 17-REVIEW closures made there by CI-18, CI-19 and OPS-02). Phase 23 shares no `src/main` file with Phase 22; it runs after it because phases run sequentially.

**Requirements**: COLS-12, COLS-13, COLS-14, DEBT-10, DEBT-11, DEBT-12

**Success Criteria** (what must be TRUE):

  1. `DeclaredNullability.apply` and `containsTuple` return without throwing for a null root and for a Column with a null type, pinned by a Docker-free test (19-REVIEW WR-01). The existing test that provokes a `NullPointerException` through a null-type Column is flipped with a written rationale, not deleted (IN-02's re-open trigger).
  2. A literal backslash-n inside a backtick-quoted Tuple element name or an enum label survives `normalise`. Pairing succeeds, and declared nullability applies without the metadata-only fallback WARN (IN-03). The Docker-free pin's input is the escaping the server actually returns, captured live.
  3. Whether a Tuple Column can legitimately lack a declared element is decided and recorded, by the user at a checkpoint if it is not clear-cut (IN-04). The pass then either logs and falls back on an unpaired declared element, or keeps today's behaviour with a written reason. `missingDeclaredElementDoesNotShiftPairing` stays green either way.
  4. The fallback-WARN content assertions use a column name that cannot occur in the WARN's fixed text and assert it in a delimited form, and each was seen red with the column argument dropped (WR-02). The backslash-n literal is defined once and shared by every test that needs it. The one-line pass-through helper and the mid-class constant are cleaned up (IN-01, IN-02).
  5. Every 17-REVIEW and 19-REVIEW finding is fixed or closed with a written reason, and each row in the archived `17-REVIEW-DISPOSITION.md` and `19-REVIEW-DISPOSITION.md` points to where it was closed. 17-REVIEW IN-03 is closed as "leave `Lz4RoundTripLiveTest`'s jar-origin check, cross-referenced to `TestRuntimeClasspathLz4UnitTest`, with no new origin assertions"; the live test is neither deleted nor weakened.

**Plans**: TBD

**Note**: Order inside the phase:
1. DEBT-11's shared literal, so the COLS-13 pin uses it instead of adding a fourth copy (IN-01's re-open trigger).
2. COLS-12 with the IN-02 flip in one commit. The null-safe pass is exactly the change that makes the NPE-provoking test fail although the contract holds.
3. DEBT-10.
4. COLS-13. It needs a short live capture of what the server returns for a backslash-n inside a quoted element name and inside an enum label.
5. COLS-14. If the decision is not clear-cut, a blocking-human checkpoint runs orchestrator-inline and the user answers directly.
6. DEBT-12 last, because it records every other outcome.

`DeclaredNullability` is fork-only `src/main` code. Any behaviour change gets its FORK-DELTA clause and its anchors row in the same commit. Never edit upstream-identical tests (`ColumnTest`, `TableTest`); add fork-only classes beside them. Parse the type string once per `Column`, never per record. COLS-14 must not change pairing for any shape that pairs today, so Phase 19's live `Nullable(Tuple)` matrix stays green.

The disposition files live in the archive (`milestones/v1.7.0_gg2-hardening-phases/17-ci-build-test-hygiene/` and `…/19-nullable-enum-tuple-mapping/`). DEBT-12 adds closure pointers to their rows and does not rewrite the findings.

Phase gate: cache-busted `testUnit` and a full suite on `latest` × V2, plus the anchors invariant.

### Phase 24: Release & Publication

**Goal**: `v1.7.0_gg3` is published on `custom_build`. Operators have a reviewed rollout note for the changed writer error text, the suite is green twice on `latest` × V2, and the published SHA has one green CI run before the tag is pushed.

**Depends on**: Phases 21, 22 and 23 (it documents and publishes their outcome)

**Requirements**: ROLL-03, PUB-09, SUITE-06, PUB-10

**Success Criteria** (what must be TRUE):

  1. `docs/ROLLOUT-v1.7.0_gg3.md` tells operators that writer error text in the task status, the DLQ headers and the ERROR logs no longer carries the record value, and what to grep for instead. It also covers every other operator-visible change: the MBean `version=` segment, the client name, and the lombok, spotless and slf4j-reload4j bumps. Its review is recorded before the publication gate, and its rollback commands were re-run on a dry-run merge of the publication tip.
  2. After `clean`, `VERSION` reads `v1.7.0_gg3` and the generated version agrees. `CHANGELOG.md` has a plain-language `1.7.0_gg3` section, and `docs/FORK-DELTA.md` has a row for every fork-side patch added in Phases 21-23.
  3. The full suite passes twice on ClickHouse `latest` × client V2, the second run with `--rerun`. Both runs have 0 failures, 0 errors and every skip named. `testUnit` passes with the Docker daemon stopped.
  4. After the user's own explicit approval, given directly and not relayed, `custom_build` on `origin` is a non-force `--no-ff` merge whose first parent is the pre-merge `custom_build` tip. That SHA has exactly one `tests.yaml` run, and it is green. The `v1.7.0_gg3` tag reaches `origin` only after that run is green, and the rollback tag `pre-v1.7.0_gg3` points at the pre-merge `custom_build` tip.

**Plans**: TBD

**Note**: This phase follows the Phase 20 recipe: rehearse, measure, approve, publish. The steps:
1. Push `sync/v1.7.0` once as a scratch branch (explicit refspec, never at a SHA another pushed branch already holds) and wait for green.
2. Push the rollback tag `pre-v1.7.0_gg3` at the pre-merge `custom_build` tip. That is `6473e24f` unless `custom_build` moved.
3. Merge with `--no-ff` from a throwaway worktree, so the merged tree equals the rehearsed tip.
4. Push `custom_build` only, and wait for its single run to finish.
5. Push the `v1.7.0_gg3` tag by name, only after that run is green.
6. Delete the scratch branch.

Before the gate, re-fetch `origin/custom_build`. If it moved past `6473e24f`, merge it into `sync/v1.7.0` and re-run the gate; the rollback tag follows the actual pre-merge tip. After DEPS-01, `custom_build` is an ancestor of the working branch, so the left count of `git rev-list --left-right --count origin/custom_build...sync/v1.7.0` is 0 unless `custom_build` moved.

The rollout doc is written and reviewed before the gate, inside the merged tree. Budget two to three review rounds for its operator command text. The `VERSION` anchor row moves in the bump commit. Refresh FORK-DELTA's raw numbers here only.

The approval gate runs orchestrator-inline, because executors refuse relayed approvals. `custom_build` is GSD's protected base branch. The upstream-sync parent checks of `verify-merge-invariants.sh` do not apply to this merge. Use Phase 16/20's checks instead: two parents, the first parent equal to `origin/custom_build`, and the merged tree equal to the rehearsed tip. Run the anchors invariant on the branch tip, from the `PRE_TAG` Phase 21 chose.

Record `SELECT version()` at each gate. If the full suite goes red on unchanged code, check the `latest` image date first. The pre-gate bundle re-checks OPS-01: no `schedule` run of `integrationTests.yaml` since the recorded disable date.

## Structure Notes

**Why four phases under `coarse` granularity.** DEPS-01 must land first and the release must come last, both by user decision. The 14 requirements between them split along the two production files that change: `ClickHouseWriter.java` (999.3) and `DeclaredNullability.java` (19-REVIEW). Folding those into one phase would put a high-risk writer change and a possible user checkpoint (COLS-14) behind one phase gate. The test-only items and the user actions ride with the phase whose files or theme they share, so no thin phase is created. Phase 21 is the DEPS-01 gate plus the CI and repository hygiene that needs no main-code change.

**Placement choices worth recording:**

- **CI-18 and CI-19 sit in Phase 21, not with the other review debt.** They are test-only, they read `tests.yaml` and `build.gradle.kts`, and they close 17-REVIEW items, which is that phase's theme. DEPS-01 touches `build.gradle.kts` just before them.
- **OPS-01..OPS-03 sit in Phase 21, not in the release phase's manual checks (gg2 did them in 20-08).** OPS-01's verification needs scheduled times to pass with no run, so the earlier the disable, the more nights it observes before the release; Phase 24 re-checks it. OPS-03's token had a 1-day expiry and is already dead, so nothing later needs it.
- **HARN-08 sits in Phase 22.** It is the writer error-path test that the classification pin (WRT-14) builds on. Doing it first keeps every new writer test off the inline mock.
- **WRT-14 precedes WRT-13 inside Phase 22.** The pin must pass on the unchanged tree to prove that the change kept classification.
- **DEBT-12 sits last in Phase 23.** It records the outcome of every 17-REVIEW and 19-REVIEW item, including the Phase 21 closures and the COLS-14 decision.

**Execution model.** Phases run sequentially on one working tree (dispatch isolation `none`). The cost to minimise is re-touching the same lines and repeating gates, not merge conflicts.

**Files shared across phases:**

| File | Phases | Rule |
|------|--------|------|
| `docs/FORK-DELTA.md` | 21 (slf4j-reload4j and Lombok version rows), 22 (Known costs item 6, WRT-12 row), 23 (`DeclaredNullability` clauses), 24 (raw numbers, completeness) | Each phase edits only its own rows; raw numbers are refreshed in Phase 24 only |
| `.planning/scripts/fork-anchors.txt` | 21 (BLD-08 `slf4j-reload4j` row), 22 and 23 (any new anchor), 24 (`VERSION` row) | Anchor rows change in the same commit as the code |
| `src/main/.../sink/db/ClickHouseWriter.java` | 22 only | — |
| `src/main/.../sink/db/mapping/DeclaredNullability.java` and its tests | 23 only | — |
| `TestsWorkflowPinGuardUnitTest` | 21 only | `tests.yaml` and `build.gradle.kts` are read, not edited, for CI-18/CI-19 |
| Archived `17-REVIEW-DISPOSITION.md`, `19-REVIEW-DISPOSITION.md` | 23 only | Closure pointers added; findings not rewritten |
| `.planning/ROADMAP.md` backlog 999.3 entry | 22 only | Marked delivered in the same commit as the writer code (WRT-15) |
| `VERSION`, `CHANGELOG.md`, `docs/ROLLOUT-v1.7.0_gg3.md` | 24 only | Never rewrite released CHANGELOG content without a recorded reason |

## Sync Mechanics (applies to every upstream sync)

- **Scratch branch, not `custom_build`.** All merge and hardening work happens on a scratch branch, with the pre-sync tip tagged `pre-vX.Y.Z-sync`. `custom_build` is published and protected. It moves forward only by a non-force `--no-ff` merge after human approval, which keeps `git revert -m 1` viable.
- **Upstream ref is the tag.** Never use bare `upstream/main`, because a stale `refs/upstream/main` makes it ambiguous.
- **Commit cadence.** No `.planning/` commit is interleaved with in-progress conflict resolution. No dependency bump rides inside the sync commit.
- **Gradle invocation.** Always `env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew ...`. This shell sets the variable to the literal string `"null"`, which breaks Ryuk. Docker is colima (profile `vz`).
- **Gates.** `testUnit` (Docker-free) is the cheap gate. Any two-run gate busts Gradle's cache with `--rerun`. If the full suite goes red on unchanged code, check the `latest` image date first.
- **Pushes.** Pushing any branch triggers `tests.yaml`; a tag push does not (CI-08). Two branches pushed at one SHA still cancel each other.
- **Editing this file by script.** Slice to the target `### Phase N:` section before any find-and-replace. An unanchored replace truncated this roadmap once (`c1480f8f`).

## Backlog

### Phase 999.1: Column.extractColumn drops Nullable for Enum8/Enum16 and Tuple (BACKLOG — scheduled into Phase 19)

**Goal:** Nullable(Enum8/16) values are stored as NULL or shifted (RESEARCH Spike b2, Pitfall 8); multi-field Nullable(Tuple) fails ATTEMPT_TO_READ_AFTER_EOF and single-field Nullable(Tuple) array elements now throw under D-16 (Spike b3); found 2026-10-08, Phase 13 research, deliberately not fixed in Phase 13
**Scheduled:** 2026-10-10 into milestone `v1.7.0_gg2-hardening` as Phase 19 (COLS-08..COLS-11, DEBT-09). The Enum half ships unconditionally. The Tuple half is a go/no-go after a spike. On no-go, COLS-10 updates this entry with the remaining Known cost.
**Requirements:** mapped under Phase 19
**Plans:** planned under Phase 19
**Delivered:** Phase 19 (go), shipped in `v1.7.0_gg2` on 2026-10-14 — `Nullable(Enum8|Enum16)` and `Nullable(Tuple)` round-trip; the discovery WARN is deleted

### Phase 999.2: Test classpath carries org.lz4:lz4-java 1.8.0 beside at.yawk.lz4 (BACKLOG — scheduled into Phase 17)

**Goal:** `testRuntimeClasspath` selects `org.lz4:lz4-java:1.8.0` (via the Confluent 7.9.1 test stack) beside `at.yawk.lz4:lz4-java`, although upstream's capability rule redirects it on `runtimeClasspath` and `integrationTestRuntimeClasspath`; it is test-only and not shipped; it is affected by the 2026-10-07 lz4 advisories, with no fix on that coordinate; fixing it needs a `build.gradle.kts` change outside Phase 14's scope; found 2026-10-09, Phase 14 research
**Scheduled:** 2026-10-10 into milestone `v1.7.0_gg2-hardening` as Phase 17 (DEBT-07)
**Requirements:** mapped under Phase 17
**Plans:** planned under Phase 17
**Delivered:** Phase 17 (DEBT-07), shipped in `v1.7.0_gg2` on 2026-10-14 — excluded from `testRuntimeClasspath` only, pinned by `TestRuntimeClasspathLz4UnitTest`

### Phase 999.3: Rethrown write-failure messages can carry record values (BACKLOG — scheduled into Phase 22)

**Goal:** `ClickHouseWriter.doWriteColValue`'s generic catch rethrows the original exception (kept by Phase 18 D-09 so classification cannot change). For some failures its message embeds the record value (`UUID.fromString`: `Invalid UUID string: <value>`; number parsing: `For input string: ...`), which reaches the Connect task status, the DLQ headers and `doInsertRawBinary`'s two `Error inserting records` ERROR logs. Fixing it means changing the rethrown type or message without changing retriable classification. Found 2026-10-12 in the Phase 18 discussion (D-09)
**Scheduled:** 2026-10-16 into milestone `v1.7.0_gg3-hardening` as Phase 22 (WRT-13, WRT-14, WRT-15). The classification pin (WRT-14) lands and is green on the unchanged writer before the change. WRT-15 marks this entry delivered in the same commit as the writer code.
**Requirements:** mapped under Phase 22
**Plans:** planned under Phase 22

## Progress

**Execution Order:** 21 → 22 → 23 → 24, sequential on one working tree.

| Phase | Milestone | Plans Complete | Status | Completed |
|-------|-----------|----------------|--------|-----------|
| 21. Dependabot Integration & CI Hygiene | v1.7.0_gg3-hardening | 5/5 | Complete    | 2026-10-19 |
| 22. Writer Record-Data Exposure | v1.7.0_gg3-hardening | 0/5 | Planned | - |
| 23. Declared-Nullability Hardening & Review Closure | v1.7.0_gg3-hardening | 0/TBD | Not started | - |
| 24. Release & Publication | v1.7.0_gg3-hardening | 0/TBD | Not started | - |

---
*Roadmap for milestone `v1.7.0_gg3-hardening` created: 2026-10-16*
