# Roadmap: ClickHouse Kafka Connect — `custom_build` Fork Test Coverage

## Milestones

- ✅ **v1.2.9_gg4-tests Fork Test Coverage** — Phases 1-5 (shipped 2026-09-18)
- 🚧 **v1.6.0_gg1-sync Upstream v1.6.0 Sync** — Phases 6-11 (in progress)

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

### v1.6.0_gg1-sync — Upstream v1.6.0 Sync

- [x] **Phase 6: Merge Groundwork & Build Migration** - Rollback point, conflict inventory, open decisions, then upstream's version-catalog build with all five fork additions re-applied (completed 2026-09-18)
- [x] **Phase 7: Type Mapping, Writer, Helper Client & Config** - The leaf mapping layer and the two hardest files re-expressed against upstream's rewritten APIs (completed 2026-09-19)
- [x] **Phase 8: Orchestration & JMX Re-plumbing** - `connector=<name>` restored where upstream now owns MBean naming — a re-plumb, not a patch re-apply (completed 2026-09-19)
- [x] **Phase 9: Metadata-free JSON Path — Keep or Drop** - The R-01 keep/drop decision made at a blocking-human checkpoint, then the code and tests made to say the same thing (completed 2026-09-20)
- [x] **Phase 10: Test Harness Reconciliation** - The fork's `ClickHouseBase` contract and test fixtures re-composed over upstream's rewritten infrastructure (completed 2026-09-20)
- [ ] **Phase 11: Full Suite, CI & Publication** - Green `./gradlew test` on `latest` × V2, green CI on the pushed SHA, `custom_build` published

## Phase Details

### Phase 6: Merge Groundwork & Build Migration

**Goal**: The merge is underway on a disposable branch with a parseable, resolvable build — upstream is a named remote, an unambiguous rollback point exists, every open version decision is on the record, and upstream's version-catalog `build.gradle.kts` carries all five fork additions.

**Depends on**: Nothing (first phase of the milestone)

**Requirements**: SYNC-01, SYNC-02, SYNC-03, SYNC-04, SYNC-05, SYNC-06, BLD-01, BLD-02, BLD-03, BLD-04, BLD-05, BLD-06, BLD-07, BLD-08

**Success Criteria** (what must be TRUE):

  1. A `pre-v1.6.0-sync` tag names the pre-merge tip of `custom_build`, and the merge is being carried out on the scratch branch `sync/v1.6.0` — the already-published `custom_build` is untouched and never needs a force-push.
  2. A checked-in per-file resolution checklist covers all 40 `git merge-tree` conflict messages across 39 paths, each marked survives-as-is / re-express / redundant; upstream is reachable as a named git remote; the catalog's resolved ClickHouse client version and the production-config audit (`enableDbTopicSplit`+`exactlyOnce`, `bufferCount`+`exactlyOnce`) are recorded as named facts.
  3. `env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew help` and `./gradlew dependencies` both succeed on the merged build — version catalog, `jvm-test-suite`, `java-test-fixtures`, wrapper `9.6.1`, `signing`/`buildconfig` dropped — and `VERSION` reads `v1.6.0_gg1`.
  4. All five fork build additions are provably present, not just intended: `jacocoTestReport` + the 15-class `jacocoForkReport` exist report-only, `testUnit` exists and is excluded from `check`, the *test* JVM is pinned to JDK 17 by an explicit `javaLauncher` and runs `file.encoding=windows-1252`, and the LZ4 capability resolution plus the ben-manes `dependencyUpdates` config still resolve.
  5. No committed revision on the scratch branch contains a literal `<<<<<<<` marker in tracked source, and no `.planning/` commit is interleaved with in-progress conflict resolution.

**Plans**: 3/3 plans executed
**Wave 1**

- [x] 06-01-PLAN.md — Upstream remote, `pre-v1.6.0-sync` tag, `sync/v1.6.0` scratch branch, the 100%-mechanical merge commit, and the committed D-02/D-09 invariant script (wave 1)

**Wave 2** *(blocked on Wave 1 completion; gap closure added 2026-10-07, D-17)*

- [ ] 11-08-PLAN.md — Gap closure: force `network_compression_method=lz4` on every connector-built client so client 0.9.6 works against ClickHouse 26.9+ (`latest`), pinned by a Docker-free unit test and a live round-trip test (D-17)

**Wave 3** *(blocked on Wave 2 completion)*

- [ ] 11-04-PLAN.md — Local acceptance gate: one cache-busted full run on the published tree with the skip inventory re-derived from it, plus Docker-free `testUnit` (D-12)

**Wave 4** *(blocked on Wave 3 completion)*

- [ ] 11-05-PLAN.md — Tracer: scratch-branch CI rehearsal end-to-end, wall-clock measured against a stated threshold, then D-03's evidence bundle (D-02/D-10/D-11)

**Wave 5** *(blocked on Wave 4 completion)*

- [ ] 11-06-PLAN.md — The one blocking human gate, then the forced non-fast-forward merge, the non-force push of branch and both tags, and CI-06's evidence on the published SHA (D-01/D-03/D-04)

**Wave 6** *(blocked on Wave 5 completion)*

- [ ] 11-07-PLAN.md — Close the milestone's last six requirements on re-run evidence and assert the final published state

## Merge Mechanics (applies to every phase)

- **Scratch branch, not `custom_build`.** All conflict resolution happens on `sync/v1.6.0`, with the pre-merge tip tagged `pre-v1.6.0-sync`. `custom_build` is already published to the public fork, so it is only ever brought forward by a **non-force** merge from the scratch branch, once green (Phase 11). This keeps `git revert -m 1` viable — there is no bisect path through 581 upstream commits.
- **Commit cadence.** Phases 6–10 commit onto the scratch branch. Plan-level commits must **not** interleave `.planning/` churn with in-progress conflict resolution: a commit made while a `<<<<<<<` marker exists anywhere in tracked source is the failure signal for SYNC-06. Batch planning-doc updates before a merge sequence starts or after it is resolved and compiling.
- **Gradle invocation.** Always `env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew ...` — this shell sets the variable to the literal string `"null"`, which breaks Ryuk. Docker is colima profile `vz`.
- **`testUnit` is the cheap gate.** It is Docker-free and usable from Phase 7 onward, giving Phases 7, 8, 9 and the first criterion of Phase 10 fast feedback before any container-dependent work.

## Structure Notes

This roadmap diverges from the research SUMMARY's six-phase A–F proposal in two places, both deliberate:

- **SUMMARY's Phase A (inventory/rollback) and Phase B (build plumbing) are merged into Phase 6.** Both are pre-conflict-resolution prerequisites with no `src/main` semantics, both gate on the same thing (a build that parses and resolves before anything is compiled), and the project's `coarse` granularity argues against a standalone phase whose only output is a checklist and a tag. The ordering inside the phase is unchanged — remote, tag, scratch branch, inventory and decisions all land before the first conflict is touched.
- **SUMMARY's Phase F is split into Phase 10 (harness) and Phase 11 (suite, CI, publication).** They have genuinely different gates — `testUnit` with Docker *stopped* versus a full container-backed run plus a one-way, human-approved publication — and the harness is the widest fan-in in the milestone, so bundling it with the acceptance gate would hide a failure that needs its own signal.

The dependency ordering is otherwise exactly as researched and is not negotiable: build plumbing first (nothing compiles otherwise) → `Type`/`Column`/`Table` before `ClickHouseWriter`/`ClickHouseHelperClient` (the immutable `Table` constructor and `Type.DYNAMIC` are compile-time prerequisites) → `ProxySinkTask`'s JMX work after both compile (it constructs both) → the JSON-path decision before its dependent work → `ClickHouseBase` before the six conflicted sink-task test files that extend it.

## Progress

| Phase | Plans Complete | Status | Completed |
|-------|----------------|--------|-----------|
| 6. Merge Groundwork & Build Migration | 3/3 | Complete    | 2026-09-18 |
| 7. Type Mapping, Writer, Helper Client & Config | 4/4 | Complete    | 2026-09-19 |
| 8. Orchestration & JMX Re-plumbing | 3/3 | Complete    | 2026-09-19 |
| 9. Metadata-free JSON Path — Keep or Drop | 3/3 | Complete    | 2026-09-20 |
| 10. Test Harness Reconciliation | 3/3 | Complete    | 2026-09-20 |
| 11. Full Suite, CI & Publication | 3/7 | In Progress | - |

---
*Roadmap for milestone `v1.6.0_gg1-sync` created: 2026-09-18*
