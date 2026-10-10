# Roadmap: ClickHouse Kafka Connect — `custom_build` Fork Test Coverage

## Milestones

- ✅ **v1.2.9_gg4-tests Fork Test Coverage** — Phases 1-5 (shipped 2026-09-18)
- ✅ **v1.6.0_gg1-sync Upstream v1.6.0 Sync** — Phases 6-11 (shipped 2026-10-07)
- ✅ **v1.7.0_gg1-sync Upstream v1.7.0 Sync & Follow-up Hardening** — Phases 12-15 (shipped 2026-10-10)

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

## Sync Mechanics (applies to every upstream sync)

- **Scratch branch, not `custom_build`.** All merge and hardening work happens on a scratch branch, with the pre-sync tip tagged `pre-vX.Y.Z-sync`. `custom_build` is published and protected. It moves forward only by a non-force `--no-ff` merge after human approval, which keeps `git revert -m 1` viable.
- **Upstream ref is the tag.** Never use bare `upstream/main`, because a stale `refs/upstream/main` makes it ambiguous.
- **Commit cadence.** No `.planning/` commit is interleaved with in-progress conflict resolution. No dependency bump rides inside the sync commit.
- **Gradle invocation.** Always `env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew ...`. This shell sets the variable to the literal string `"null"`, which breaks Ryuk. Docker is colima (profile `vz`).
- **Gates.** `testUnit` (Docker-free) is the cheap gate. Any two-run gate busts Gradle's cache with `--rerun`. If the full suite goes red on unchanged code, check the `latest` image date first.
- **Pushes.** Pushing any branch triggers `tests.yaml`; a tag push does not (CI-08). Two branches pushed at one SHA still cancel each other.
- **Editing this file by script.** Slice to the target `### Phase N:` section before any find-and-replace. An unanchored replace truncated this roadmap once (`c1480f8f`).

## Backlog

### Phase 999.1: Column.extractColumn drops Nullable for Enum8/Enum16 and Tuple (BACKLOG)

**Goal:** Nullable(Enum8/16) values are stored as NULL or shifted (RESEARCH Spike b2, Pitfall 8); multi-field Nullable(Tuple) fails ATTEMPT_TO_READ_AFTER_EOF and single-field Nullable(Tuple) array elements now throw under D-16 (Spike b3); found 2026-10-08, Phase 13 research, deliberately not fixed in Phase 13
**Requirements:** TBD
**Plans:** 0 plans

Plans:
- [ ] TBD (promote with /gsd-review-backlog when ready)

### Phase 999.2: Test classpath carries org.lz4:lz4-java 1.8.0 beside at.yawk.lz4 (BACKLOG)

**Goal:** `testRuntimeClasspath` selects `org.lz4:lz4-java:1.8.0` (via the Confluent 7.9.1 test stack) beside `at.yawk.lz4:lz4-java`, although upstream's capability rule redirects it on `runtimeClasspath` and `integrationTestRuntimeClasspath`; it is test-only and not shipped; it is affected by the 2026-10-07 lz4 advisories, with no fix on that coordinate; fixing it needs a `build.gradle.kts` change outside Phase 14's scope; found 2026-10-09, Phase 14 research
**Requirements:** TBD
**Plans:** 0 plans

Plans:
- [ ] TBD (promote with /gsd-review-backlog when ready)
