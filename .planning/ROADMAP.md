# Roadmap: ClickHouse Kafka Connect — `custom_build` Fork Test Coverage

## Milestones

- ✅ **v1.2.9_gg4-tests Fork Test Coverage** — Phases 1-5 (shipped 2026-09-18)
- ✅ **v1.6.0_gg1-sync Upstream v1.6.0 Sync** — Phases 6-11 (shipped 2026-10-07)
- ✅ **v1.7.0_gg1-sync Upstream v1.7.0 Sync & Follow-up Hardening** — Phases 12-15 (shipped 2026-10-10)
- ✅ **v1.7.0_gg2-hardening Follow-up Hardening & CI Hygiene** — Phases 16-20 (shipped 2026-10-14)
- ✅ **v1.7.0_gg3-hardening Record-Data Exposure & Review Debt** — Phases 21-24 (shipped 2026-10-20)
- ✅ **v1.7.0_gg4-hardening Dependency Absorption, Discovery Defects & Review Debt** — Phases 25-29 (shipped 2026-10-23)
- 🚧 **v1.7.0_gg5-ci CI & Release Workflow Modernization** — Phases 30-32 (in progress)

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

<details>
<summary>✅ v1.7.0_gg3-hardening Record-Data Exposure & Review Debt (Phases 21-24) — SHIPPED 2026-10-20</summary>

- [x] Phase 21: Dependabot Integration & CI Hygiene (5/5 plans) — completed 2026-10-19
- [x] Phase 22: Writer Record-Data Exposure (5/5 plans) — completed 2026-10-20
- [x] Phase 23: Declared-Nullability Hardening & Review Closure (6/6 plans) — completed 2026-10-20
- [x] Phase 24: Release & Publication (7/7 plans) — completed 2026-10-20
- [x] Quick 261020-vcr: rollout-note Round 3 fixes, review rounds 4-6, republished as `6a2a87a5` — completed 2026-10-20

Full phase details: [`milestones/v1.7.0_gg3-hardening-ROADMAP.md`](milestones/v1.7.0_gg3-hardening-ROADMAP.md)
Requirements as shipped: [`milestones/v1.7.0_gg3-hardening-REQUIREMENTS.md`](milestones/v1.7.0_gg3-hardening-REQUIREMENTS.md)
Milestone audit: [`milestones/v1.7.0_gg3-hardening-MILESTONE-AUDIT.md`](milestones/v1.7.0_gg3-hardening-MILESTONE-AUDIT.md)
Phase artifacts: `milestones/v1.7.0_gg3-hardening-phases/` · Quick tasks: `milestones/v1.7.0_gg3-hardening-quick/`

</details>

<details>
<summary>✅ v1.7.0_gg4-hardening Dependency Absorption, Discovery Defects & Review Debt (Phases 25-29) — SHIPPED 2026-10-23</summary>

- [x] Phase 25: Dependency Absorption & Upstream Mini-Sync (8/8 plans) — completed 2026-10-21
- [x] Phase 26: Size-Named Tuple Elements in Discovery (5/5 plans) — completed 2026-10-22
- [x] Phase 27: Writer Failure Path & Test Debt (5/5 plans) — completed 2026-10-23
- [x] Phase 28: Raw Identifier Names in Discovery (6/6 plans) — completed 2026-10-23
- [x] Phase 29: Release & Publication (8/8 plans) — completed 2026-10-24

Full phase details: [`milestones/v1.7.0_gg4-hardening-ROADMAP.md`](milestones/v1.7.0_gg4-hardening-ROADMAP.md)
Requirements as shipped: [`milestones/v1.7.0_gg4-hardening-REQUIREMENTS.md`](milestones/v1.7.0_gg4-hardening-REQUIREMENTS.md)
Milestone audit: [`milestones/v1.7.0_gg4-hardening-MILESTONE-AUDIT.md`](milestones/v1.7.0_gg4-hardening-MILESTONE-AUDIT.md)
Phase artifacts: `milestones/v1.7.0_gg4-hardening-phases/` · Research: `milestones/v1.7.0_gg4-hardening-research/`

</details>

### 🚧 v1.7.0_gg5-ci CI & Release Workflow Modernization (In Progress)

**Milestone Goal:** Bring the fork's workflows current and get a trustworthy integration-test signal, without touching connector code or ClickHouse client `0.10.0`. The release-path workflows build with the project's Gradle wrapper and run only SHA-pinned actions, a dry run proves them, and a fork-owned nightly runs `integrationTest` on ClickHouse `latest` × client V2 in local Docker while upstream's `integrationTests.yaml` stays byte-identical and disabled. Publish `v1.7.0_gg5` with CI green on the published SHA.

**Criteria tags:** *[live]* needs Docker and ClickHouse `latest` × client V2. *[Docker-free]* runs under `testUnit` or as a build or static check with Docker stopped. *[record]* is a committed evidence file or document whose content is checked. *[operator]* is a user action, confirmed in the user's own words. *[CI]* is read from GitHub Actions.

- [x] **Phase 30: Release Workflows & Pin Guard** - A guard fails on any unpinned action in a fork-owned workflow. `release.yaml`, `pre-release.yaml` and `increment-version.yml` pass it, both release paths build with the Gradle wrapper through `setup-gradle` and publish with `gh release create`, and a dry run proves both without creating a release or a tag (completed 2026-10-26)
- [ ] **Phase 31: Fork-Owned Nightly Integration Tests** - A measured local `integrationTest` run comes first. A new SHA-pinned nightly then runs `integrationTest` on `latest` × V2 in the runner's Docker with no cloud job, and its first manual run is green or every failure is named in a skip list. Upstream's `integrationTests.yaml` stays byte-identical and disabled, pinned by a guard
- [ ] **Phase 32: Release & Publication** - `v1.7.0_gg5` is published with FORK-DELTA, CHANGELOG, `VERSION` and a short reviewed rollout note, two green suite runs and one green CI run on the published SHA before the tags

## Phase Details

### Phase 30: Release Workflows & Pin Guard

**Goal**: The fork's three release-path workflows run current, SHA-pinned actions only. `pre-release.yaml` and `release.yaml` build the Confluent archive with the project's own Gradle wrapper through `setup-gradle`, so the `verifyConfluentArchive` finalizer runs on the release path too, and both create the GitHub release with `gh release create`, attaching that archive. A dry run proves both release paths build and attach the archive without creating a release or a tag. A Docker-free guard fails whenever a fork-owned workflow uses an unpinned action, so the Phase 31 nightly is written under it. No connector code changes.

**Depends on**: Nothing in this milestone. Phase 29 shipped `v1.7.0_gg4` (`21c51598`). `release.yaml`, `pre-release.yaml` and `increment-version.yml` are byte-identical to upstream at both `v1.7.0` and `a6f1df83` (measured 2026-10-24). `pre-release.yaml` still runs `gradle/gradle-build-action@v2` with `gradle-version: '7.4.2'` (DEPS-F4). `release.yaml` already uses `gradle/actions/setup-gradle@v6`, by tag. Both release files use `actions/create-release@v1` and `actions/upload-release-asset@v1`, and all three use `@v3`-era actions.

**Requirements**: CIG-01, REL-01, REL-02, REL-03, REL-04, REL-05

**Success Criteria** (what must be TRUE):

  1. *[Docker-free]* A guard test reads every file under `.github/workflows/` except the upstream-identical `integrationTests.yaml`. It fails, naming the file and the line, on any `uses:` that is not a full 40-hex commit SHA with a version comment: a tag, a branch, a short SHA, or a missing comment. It discovers the workflow files from the directory, so a new workflow is covered without editing the guard. Each unpinned form was seen red on a fixture, and the guard was seen red on the unchanged release workflows before they were pinned (CIG-01).
  2. *[Docker-free]* `release.yaml`, `pre-release.yaml` and `increment-version.yml` pin every action by full SHA with a `# vX.Y.Z` comment, and they pass the guard (REL-02). Neither release workflow contains `gradle-build-action`, a `gradle-version:` input or `7.4.2`. Both set up Gradle with `gradle/actions/setup-gradle` and build with `./gradlew createConfluentArchive` (REL-01, REL-03).
  3. *[Docker-free]* No workflow uses `actions/create-release` or `actions/upload-release-asset`. Both release workflows create the release with `gh release create` and attach `build/confluent/clickhouse-kafka-connect-<VERSION>.zip`. `pre-release.yaml` marks the release a prerelease, and `release.yaml` does not (REL-04).
  4. *[CI, operator]* The user dispatches `pre-release.yaml` and `release.yaml` in dry-run mode on the pushed scratch branch. Each run is green: it builds the archive with the wrapper's Gradle, passes `verifyConfluentArchive`, and exercises the attach step on the archive it built without publishing. Afterwards origin has no new published release and no new tag, read from the REST API and `git ls-remote --tags` before and after the runs (REL-05).

**Plans**: 3/3 plans complete

Plans:
**Wave 1**
- [x] 30-01-PLAN.md — Wave 1: the CIG-01 guard seen red on the unchanged files, then the three workflows pinned (setup-gradle, `gh release create`, `dry_run`), the Dependabot and `UPSTREAM_WORKFLOWS` edit and 29 anchor rows in one green commit; cumulative anchors gate

**Wave 2** *(blocked on Wave 1 completion)*
- [x] 30-02-PLAN.md — Wave 2: the D-08 `vnull` defect measured and recorded; FORK-DELTA's workflow paragraph and Dependabot row corrected

**Wave 3** *(blocked on Wave 2 completion)*
- [x] 30-03-PLAN.md — Wave 3 (operator): readiness and before-snapshots, one blocking-human decision to push `sync/v1.7.0`, the user's two dry-run dispatches, REST proof of no release and no tag

**Research flag**: Settle these before planning:
- **The dry-run mechanism.** Either a dispatch input that stops before the publish step, or `gh release create --draft` with the asset, deleted after the run. A draft release should create no tag until it is published; confirm that before relying on it.
- **Dispatch on a scratch branch.** Both release files already exist on `custom_build` with `workflow_dispatch`, so they can be dispatched on the scratch branch, and the run uses that branch's file. Confirm that the dispatch reads a new input from the selected branch's file, not from the default branch's.
- **Action versions.** Reuse `tests.yaml`'s pins for checkout (v7.0.1), setup-java (v6.0.1, with `temurin` in place of the deprecated `adopt`) and setup-gradle (v6.4.0, by its peeled commit). Pick a current `setup-node` major and a supported Node version: the file asks for Node 16.
- **Who creates the tag.** `release.yaml`'s last step tags `VERSION` and pushes the tag itself, and `gh release create` also creates the tag when it is absent. Decide which one creates it, under the standing rule that tags reach origin only after the published SHA's own CI run is green. Decide `permissions:` and `persist-credentials` for each job to match.

**Note**:
- Before the first workflow edit, re-read `git ls-remote origin custom_build`. If a Dependabot merge has landed there, absorb it first with a DEPS-01-shaped merge.
- Order: the guard first, seen red, then the pins and the `setup-gradle` and `gh release` changes. The guard goes green in the same commit as the pins.
- Dispatch is an operator action. There is no local `gh` and no token (D-11), so the user starts each run from GitHub's `Run workflow` control and reports the result directly. A relayed answer is not accepted.
- Pushing the scratch branch publishes the local `.planning` tail one way, so it needs the user's direct approval (Phase 17 D-11 precedent). Push it at a SHA that no other pushed branch holds. The push also runs `tests.yaml` once.
- `dependabot.yml`'s `exclude-paths` lists all four upstream workflows, and `TestsWorkflowPinGuardUnitTest` pins that list through `UPSTREAM_WORKFLOWS`. Once the three release-path files are pinned, their pins go stale unless Dependabot covers them. The fix is to drop them from the list and keep `integrationTests.yaml`. That changes an existing pinned assertion, so it needs the user's sign-off.
- `increment-version.yml` runs `semver.inc` on `VERSION`. By semver's grammar, a `vX.Y.Z_ggN` value is not a version, so the step would write `vnull`. This was read from the grammar, not run. REL-02 only pins the file; decide at discuss time whether to record the defect or fix it.
- Both release files are named `Release`, so their runs look the same in the Actions list.
- Any new anchor row for a workflow edit (for example `gradle-version` with count 0 in `pre-release.yaml`) lands in the same commit as the edit.
- `.github/actions/use-clickhouse-java` is an upstream composite action that no fork workflow calls. It is not a workflow, so CIG-01 does not cover it. Record whether the guard reads it.

Phase gate: cache-busted `testUnit`, the anchors invariant, and an empty `src/main` diff.

### Phase 31: Fork-Owned Nightly Integration Tests

**Goal**: The fork has a nightly integration-test signal it can trust. A fork-owned workflow runs the `integrationTest` task on ClickHouse `latest` × client V2 in the runner's own Docker, with no cloud job and no cloud secrets. Its first manual run is green, or every failing test is named in a skip list with its cause. Upstream's `integrationTests.yaml` stays byte-identical to upstream and disabled, and a guard pins its bytes. No connector code changes.

**Depends on**: Phase 30 (the CIG-01 guard, which the new workflow must pass unchanged, and the action pins it reuses)

**Requirements**: NIT-01, NIT-02, NIT-03

**Success Criteria** (what must be TRUE):

  1. *[live, record]* Before the workflow is written, `integrationTest` has been run locally on `latest` × V2 with Docker (colima, profile `vz`). The record states `SELECT version()` and lists every test class that ran or skipped. It gives each failure a cause: environment, test harness, cloud-only, or connector defect. It also says why upstream's standalone `latest` × V2 job fails (run 38881153720).
  2. *[Docker-free]* The new workflow runs `integrationTest` with ClickHouse `latest` and client V2 on `ubuntu-24.04`. It has a timeout, a top-level `permissions: contents: read`, `persist-credentials: false`, and it uploads the test reports on failure. It passes the CIG-01 guard with no guard edit. No step references a cloud secret or variable, and no job targets ClickHouse Cloud. Its triggers are `workflow_dispatch` and a schedule that cannot fire before the NIT-03 run is recorded (NIT-01).
  3. *[Docker-free, CI]* A Docker-free guard fails when `integrationTests.yaml` differs by a single byte from upstream's copy, which is identical at `v1.7.0` and `a6f1df83`, and it was seen red on a one-byte edit. The Actions API reports that workflow as disabled, with no scheduled run since 2026-10-16 (NIT-02).
  4. *[CI, operator]* A manual dispatch of the nightly finishes green, or every failing test is listed by name in a skip list with its cause from criterion 1. The list names tests, not patterns, and it deletes or weakens no existing test. The schedule starts only after that run is recorded, and no scheduled run precedes it (NIT-03).

**Plans**: 0/6 plans complete (31-01 tasks 1-3 done, E-06 disposition `fix-harness`; gap plan 31-06 fixes the harness and re-measures before 31-02)

Plans:
**Wave 1**
- [ ] 31-01-PLAN.md — Wave 1 (tasks 1-3 done, E-06: `fix-harness`): tracer slice (latest pulled, `SELECT version()`, the cloud class skips), the cold local `integrationTest` run on `latest` × V2 and the criterion-1 record with the derived timeouts, then one blocking-human disposition of every failure (D-04, D-05, D-06, D-09)

**Wave 2** *(blocked on Wave 1 completion)*
- [ ] 31-06-PLAN.md — Wave 2 (gap closure): the harness's Connect worker on Java 17 (Confluent's CP 7.9.6 Connect image plus the pinned datagen plugin), proven broker-free and by one connector test, the whole `integrationTest` re-measured on `latest` × V2, then the user's disposition E-06b (D-05, D-06, D-08, D-09)

**Wave 3** *(blocked on Wave 2 completion)*
- [ ] 31-02-PLAN.md — Wave 3: the name-only skip list in the build from the user's disposition, its Docker-free guard and the green live re-run on `latest` × V2; or "not needed" recorded (D-07, D-08)

**Wave 4** *(blocked on Wave 3 completion)*
- [ ] 31-03-PLAN.md — Wave 4: the NIT-02 byte guard seen red on a one-byte edit of `integrationTests.yaml`, and its disabled state read from the Actions API

**Wave 5** *(blocked on Wave 4 completion)*
- [ ] 31-04-PLAN.md — Wave 5: the gated, SHA-pinned nightly with its failed-test summary, a 58-case Docker-free shape guard, the script's tests and five anchor rows (NIT-01, D-01, D-05, D-10)

**Wave 6** *(blocked on Wave 5 completion)*
- [ ] 31-05-PLAN.md — Wave 6: the phase gate on the final tree and both workflows' GitHub state, then the user's NIT-03 decision applied to ROADMAP and REQUIREMENTS (D-02)

**Research flag**: Settle these before planning:
- **Dispatching a new workflow.** GitHub documents that `workflow_dispatch` triggers only for a workflow file on the default branch. The new file reaches `custom_build` only at publication. Confirm that behaviour first, then settle how the NIT-03 dispatch runs before any schedule can fire. The candidates are a scheduled job gated on a repository variable that the user sets after the dispatch, or an early CI-only `--no-ff` publication of the workflow (the Phase 16 precedent). Phase 32 edits no `.github/` file, so anything that turns the schedule on after Phase 32 must be an operator setting, not a file edit. If NIT-03 can only finish after publication, the user decides whether it moves to Phase 32.
- **What `integrationTest` needs on a runner.** List the images it pulls (Confluent platform, ClickHouse), whether it needs the built connector archive, and its run time against `tests.yaml`'s 45-minute budget. Upstream's standalone job filters to `ClickHouseSinkConnectorIntegrationTest`, and it runs `ExactlyOnceTest` only in the cloud and cluster jobs. Decide whether the nightly runs the whole task or upstream's standalone filter, and record why.
- **Where the skip list lives.** Either the workflow's Gradle test filters or a fork-owned exclude in the build. Also decide how each entry is named and given its reason.

**Note**:
- Measurement precedes the workflow (criterion 1). A failure whose fix needs a `src/main` change stops for a user decision, because the milestone excludes connector source changes. A test-harness fix under `src/integrationTest` or `src/testFixtures` is allowed with its reason recorded. An existing assertion is never weakened.
- `ClickHouseCloudTest` skips itself without cloud properties. The record confirms it skips rather than fails.
- The nightly reuses Phase 30's action pins. It is not in `dependabot.yml`'s `exclude-paths`, so Dependabot keeps its pins current.
- `custom_build` is the default branch, so once the nightly is merged there its schedule is live unless something gates it. The gate chosen in research must already hold at the Phase 32 merge.
- The user dispatches the run from GitHub's `Run workflow` control and reports the result directly.

Phase gate: cache-busted `testUnit`, one local `integrationTest` run on `latest` × V2, the anchors invariant, and an empty `src/main` diff.

### Phase 32: Release & Publication

**Goal**: `v1.7.0_gg5` is published on `custom_build`. Operators get a short, independently reviewed rollout note that says what changed in the workflows and that no connector code changed. FORK-DELTA records the newly fork-owned workflows and the nightly. The suite is green twice on `latest` × V2, and the published SHA has exactly one green `tests.yaml` run before the tags are pushed.

**Depends on**: Phases 30 and 31 (it documents and publishes their outcome)

**Requirements**: CIG-02, CIG-03

**Success Criteria** (what must be TRUE):

  1. *[Docker-free]* After `clean`, `VERSION` reads `v1.7.0_gg5` and the generated version agrees. The `VERSION` anchor row changes in the same commit. `CHANGELOG.md` has a complete `1.7.0_gg5` section. `docs/FORK-DELTA.md` no longer says the release workflows are left as upstream ships them (DEPS-F4). It has rows for the pinned release-path workflows and `gh release`, the nightly, the CIG-01 and NIT-02 guards, and any `dependabot.yml` change, and its raw numbers are refreshed once (CIG-02).
  2. *[record]* `docs/ROLLOUT-v1.7.0_gg5.md` is short. Each claim in it is measured, and it states:
     - that `git diff v1.7.0_gg4 -- src/main` is empty;
     - that the release archive is built by the wrapper's Gradle and verified on the release path;
     - that the MBean `version=` segment and the client name read `v1.7.0_gg5`;
     - how the nightly is turned on and off;
     - the rollback.
     Before the publication gate, an orchestrator-spawned, fresh-context `gsd-code-reviewer` reviewed the note with `CHANGELOG.md` and `docs/FORK-DELTA.md`. Rounds repeated until one found nothing, at most three, and every finding's disposition is recorded (CIG-02).
  3. *[live, CI]* On the release tip, two full `--rerun` suite runs are green on `latest` × V2 with the same 18 named skips. `testUnit` is green with Docker stopped, and the anchors gate passes. A re-fetch shows `origin/custom_build` unchanged, or any change there was absorbed before gating. After the user's own direct approval:
     - `custom_build` is a non-force `--no-ff` merge whose tree equals the rehearsed tip;
     - the published SHA has exactly one `tests.yaml` run, and it is green;
     - the tags `v1.7.0_gg5` and `pre-v1.7.0_gg5` reach origin only after that run;
     - the scratch branch is deleted from origin (CIG-03).
  4. *[CI]* After publication, the Actions API still reports `integrationTests.yaml` as disabled, and the nightly is in the state Phase 31 recorded. No workflow run created a release or a tag that the plan did not intend.

**Plans**: TBD

**Research flag**: Standard; this phase follows Phase 29. Decide at plan time where the `v1.7.0_gg5` tag and any GitHub release come from: the hand-pushed tags of the Phase 29 recipe, or a real `release.yaml` dispatch after the green run. The requirements ask only for the Phase 30 dry run.

**Note**: This phase edits no `src/`, test, `build.gradle.kts`, `gradle/` or `.github/` file (the Phase 29 D-05 standing rule). A workflow defect found here goes back to Phase 30 or 31 as a fix, or is deferred with a reason, an owner and a re-open trigger in the milestone close record. It follows the Phase 29 recipe (rehearse, measure, approve, publish):
1. Push the scratch branch once, at a SHA no other pushed branch holds, and wait for green.
2. Merge with `--no-ff` from a throwaway worktree, so the merged tree equals the rehearsed tip.
3. Push `custom_build` only, and wait for its single run.
4. Push the tags by name, only after that run is green.
5. Delete the scratch branch.

The approval gates run orchestrator-inline, because executors refuse relayed approvals and `custom_build` is GSD's protected base branch. Record `SELECT version()` at each gate.

## Structure Notes

**Why three phases under `coarse` granularity.** The 11 requirements fall into three delivery boundaries: the release-path workflows (30), the nightly (31) and publication (32). The release must come last, which is a hard constraint. Phases 30 and 31 share the pin guard, but they are proven differently. Phase 30 is proven by a dispatch dry run. Phase 31 is proven by a measured integration run with failure classification, and that measurement may surface test-harness work. One gate for both would hold the release-path files hostage to the nightly. Neither can fold into Phase 32, because the release phase edits no `.github/` file.

**Placement choices worth recording:**

- **CIG-01 is Phase 30's first plan, not release hygiene.** The guard is seen red on the unpinned release files before they are pinned, and the nightly is then written under it.
- **REL-05 sits in Phase 30, not Phase 32.** Fixing a dry-run defect needs a `.github/` edit, which the release phase cannot make.
- **NIT-02's byte guard sits in Phase 31, with the nightly that replaces upstream's job.** The disabled state is re-read in Phase 32.
- **NIT-03 sits in Phase 31,** but GitHub may not dispatch the new file before it is on `custom_build`. The Phase 31 research settles this, and the user decides whether NIT-03 moves.
- **CIG-02 is entirely in Phase 32.** FORK-DELTA, CHANGELOG, `VERSION` and the note are written once, on the final tree. Anchor rows for the workflow edits still land with the edits in Phases 30 and 31.

**Execution model.** Phases run sequentially on one working tree. No phase touches `src/main`.

**Files shared across phases:**

| File | Phases | Rule |
|------|--------|------|
| `TestsWorkflowPinGuardUnitTest` or a new guard class | 30 (CIG-01, any `UPSTREAM_WORKFLOWS` change), 31 (NIT-02 byte guard) | Each new rule is seen red first; an existing assertion changes only with the user's sign-off |
| `.github/dependabot.yml` | 30 only, if the user signs off | — |
| `release.yaml`, `pre-release.yaml`, `increment-version.yml` | 30 only | — |
| The new nightly workflow | 31 only | — |
| `integrationTests.yaml` | none | Byte-identical to upstream throughout |
| `.planning/scripts/fork-anchors.txt` | 30 and 31 (workflow anchors), 32 (`VERSION` row) | Anchor rows change in the same commit as the code |
| `docs/FORK-DELTA.md`, `CHANGELOG.md`, `VERSION`, `docs/ROLLOUT-v1.7.0_gg5.md` | 32 only | FORK-DELTA raw numbers are refreshed once |

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
**Delivered:** Phase 22 (WRT-13..WRT-15), shipped in `v1.7.0_gg3` on 2026-10-20: a value-conversion failure names the column, the ClickHouse type and the exception class without the record value; retry classification is pinned by WRT-14

### Phase 999.4: Discovery drops a Tuple element named size or size0 (BACKLOG — scheduled into Phase 26)

**Goal:** `ClickHouseHelperClient.shouldSkipDescribeRow` drops every DESCRIBE row that ends in `.size`, `.size0` or `.null`, so a Tuple element named `size` or `size0` never becomes a Column, and the writer, which iterates `getTupleFields()`, writes a short tuple. Measured on `latest` 26.9.19.9 with client V2 (23-EVIDENCE.md R-01): the insert fails with code 33 `CANNOT_READ_ALL_DATA` and no row is stored, and a silently wrong stored value is reachable when the bytes of the shorter write happen to parse as a longer tuple. Severity: high for an affected table, since the inserts that were run into such tables failed (four shapes, code 32 or 33, `24-COMMAND-EVIDENCE.md` E-15). The server rejects a Tuple element named `null` (code 36), so only `size` and `size0` are affected. Since COLS-14 the unpaired-element WARN names the dropped element at every DESCRIBE. Fix sketch: keep a row that is a declared element of its parent Tuple while still dropping the structural rows `c.name.size`, `d.null`, `d.other.size`, `e.size0` and `e.v.size`. Parent-is-a-Tuple alone is not enough for `Array(Tuple)`, where `e.size0` is the array's offsets row. The fix touches upstream-region `ClickHouseHelperClient`, so it gets its own phase, with a FORK-DELTA row, an anchors row, a live round-trip test and the DESC-05 regression check. It flips the D-04 pin `COLS-14 unpaired-element WARN (D-04, known defect 999.4)`: the element then pairs and no WARN fires. Found 2026-10-20 in the Phase 23 discussion (D-03)
**Scheduled:** 2026-10-21 into milestone `v1.7.0_gg4-hardening` as Phase 26 (DESC-06..DESC-10). The measured defect is wider than this entry's title. `Table.updateParent`'s `SIZE_FIELD_MATCHER` is a second drop site, for `size<digits>` under an Array parent, and the user chose on 2026-10-21 to fix both sites. The live DESCRIBE spike (DESC-06) and the behaviour-preserving extraction of the describe loop (DESC-07) land before the fix. DESC-10 marks this entry delivered in the same commit as the fix.
**Requirements:** mapped under Phase 26
**Plans:** planned under Phase 26
**Delivered:** Phase 26 (DESC-06..DESC-10) on sync/v1.7.0, shipped in v1.7.0_gg4 on 2026-10-23: discovery keeps a declared Tuple element named size, size0 or size<digits> at both drop sites (DescribeTableBuilder and Table.updateParent); rows stored by earlier releases are not repaired

### Phase 999.5: Backslashes in column and element names come out of fromJsonRow doubled (BACKLOG — scheduled into Phase 28)

**Goal:** `ClickHouseFieldDescriptor.fromJsonRow` doubles every backslash before Jackson parses, so a server name with a backslash comes out doubled (`c.a\\nb`), and a Kafka field must use the doubled form for the writer to match it (23-EVIDENCE.md R-02). The D-12 live premise uses the doubled field name. The behaviour is pre-existing, and Phase 23 changes no code or test for it. Fix sketch: decide the intended name form, decode name escapes in upstream-region `fromJsonRow`, add a live round-trip pin for both forms and a rollout note, since operators' field names may depend on the doubled form. Found 2026-10-20 in Phase 23 research (D-20)
**Scheduled:** 2026-10-21 into milestone `v1.7.0_gg4-hardening` as Phase 28 (NAME-01..NAME-07). The canonical form was decided by the user on 2026-10-21: the raw identifier as written in `CREATE TABLE`, with no dual-form alias, and operators rename. The escaped-quote `normalise` pin (NAME-01) lands on the current code first. NAME-07 marks this entry delivered in the same commit as the fix.
**Requirements:** mapped under Phase 28
**Plans:** planned under Phase 28
**Delivered:** Phase 28 (NAME-01..NAME-07) on sync/v1.7.0, shipped in v1.7.0_gg4 on 2026-10-23: names and Enum labels are matched under the raw identifier as written in CREATE TABLE on both clients; a record using the earlier escaped form stores the DEFAULT at the root and fails for a Tuple element; rows stored by earlier releases are not repaired

## Progress

**Execution Order:** 30 → 31 → 32, sequential on one working tree.

| Phase | Milestone | Plans Complete | Status | Completed |
|-------|-----------|----------------|--------|-----------|
| 30. Release Workflows & Pin Guard | v1.7.0_gg5-ci | 3/3 | Complete    | 2026-10-26 |
| 31. Fork-Owned Nightly Integration Tests | v1.7.0_gg5-ci | 0/5 | In Progress | - |
| 32. Release & Publication | v1.7.0_gg5-ci | 0/TBD | Not started | - |

---
*Roadmap reorganized at the `v1.7.0_gg4-hardening` close: 2026-10-24. Roadmap for milestone `v1.7.0_gg5-ci` created: 2026-10-24.*
