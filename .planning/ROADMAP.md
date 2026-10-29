# Roadmap: ClickHouse Kafka Connect — `custom_build` Fork Test Coverage

## Milestones

- ✅ **v1.2.9_gg4-tests Fork Test Coverage** — Phases 1-5 (shipped 2026-09-18)
- ✅ **v1.6.0_gg1-sync Upstream v1.6.0 Sync** — Phases 6-11 (shipped 2026-10-07)
- ✅ **v1.7.0_gg1-sync Upstream v1.7.0 Sync & Follow-up Hardening** — Phases 12-15 (shipped 2026-10-10)
- ✅ **v1.7.0_gg2-hardening Follow-up Hardening & CI Hygiene** — Phases 16-20 (shipped 2026-10-14)
- ✅ **v1.7.0_gg3-hardening Record-Data Exposure & Review Debt** — Phases 21-24 (shipped 2026-10-20)
- ✅ **v1.7.0_gg4-hardening Dependency Absorption, Discovery Defects & Review Debt** — Phases 25-29 (shipped 2026-10-23)
- ✅ **v1.7.0_gg5-ci CI & Release Workflow Modernization** — Phases 30-32 (shipped 2026-10-27)
- 🚧 **v1.7.0_gg6-maintenance Dependency Absorption, Upstream Tail & Advisory Debt** — Phases 33-38 (in progress)

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

<details>
<summary>✅ v1.7.0_gg5-ci CI & Release Workflow Modernization (Phases 30-32) — SHIPPED 2026-10-27</summary>

- [x] Phase 30: Release Workflows & Pin Guard (3/3 plans) — completed 2026-10-26
- [x] Phase 31: Fork-Owned Nightly Integration Tests (6/6 plans) — completed 2026-10-26
- [x] Phase 32: Release & Publication (7/7 plans) — completed 2026-10-27

Full phase details: [`milestones/v1.7.0_gg5-ci-ROADMAP.md`](milestones/v1.7.0_gg5-ci-ROADMAP.md)
Requirements as shipped: [`milestones/v1.7.0_gg5-ci-REQUIREMENTS.md`](milestones/v1.7.0_gg5-ci-REQUIREMENTS.md)
Milestone audit: [`milestones/v1.7.0_gg5-ci-MILESTONE-AUDIT.md`](milestones/v1.7.0_gg5-ci-MILESTONE-AUDIT.md)
Phase artifacts: `milestones/v1.7.0_gg5-ci-phases/`

</details>

### 🚧 v1.7.0_gg6-maintenance Dependency Absorption, Upstream Tail & Advisory Debt (In Progress)

**Milestone Goal:** Absorb the open Dependabot PRs, including the ClickHouse client jump `0.9.6` → `0.11.0`, and upstream's untagged tail. Fix the buffered offset commit under topic-renaming SMTs (upstream #777), which was found in production use. Clear the carried review advisories and CI loose ends. Publish `v1.7.0_gg6` with the suite green on ClickHouse `latest` × client V2 and CI green on the published SHA.

**Criteria tags:** *[live]* needs Docker and ClickHouse `latest` × client V2. *[Docker-free]* runs under `testUnit` or as a build or static check with Docker stopped. *[record]* is a committed evidence file or document whose content is checked. *[operator]* is a user action, confirmed in the user's own words. *[CI]* is read from GitHub Actions.

- [x] **Phase 33: Dependency Sweep & Protobuf Decision** - A rollback tag comes first. Then lz4-java 1.12.1, okhttp 5.5.1, Gradle 9.8.2 and `setup-java` 6.1.0 are absorbed from their PR heads on the scratch branch, one commit and one gate each. PR #10 (protobuf-java 4.x) is declined with a Dependabot `ignore` and a FORK-DELTA record (completed 2026-10-28)
- [x] **Phase 34: Upstream Tail Merge** - Upstream's untagged tail is merged as one mechanical commit while the client is still 0.9.6. `ProxySinkTask` keeps both constructors without an ambiguous `null`, every `handleException` caller uses the 5-argument form, and `additionalRetriableErrorCodes` is pinned with a no-op default (completed 2026-10-29)
- [x] **Phase 35: Buffered Offset Commit Under Topic-Renaming SMTs** - With `bufferCount > 0` and a `RegexRouter`-style SMT, offsets are committed under the original topic and partition, so a restart stops rereading from `auto.offset.reset` (upstream #777). The fix is pinned Docker-free and live, and `Processing.doLogic` is checked for the same defect. It is releasable without the client bump (completed 2026-10-29)
- [ ] **Phase 36: Client 0.11.0 Audit & Bump** - Pins on 0.9.6 come first. Then one revertible catalog commit moves to 0.11.0, with the two `createClientV2` fixes, a behaviour pin class, an exactly-once byte comparison, the reworded D-17 rationale and the archive change recorded. If the audit fails, gg6 ships on 0.9.6
- [ ] **Phase 37: CI Hardening & Advisory Debt** - `increment-version.yml` stops writing `vnull`, through a script with a test. The release workflows compare the archive against a `VERSION`-keyed allow-list. Every Phase 25–31 review advisory is fixed or dispositioned, and the real `v1.7.0_gg5` release is on the record
- [ ] **Phase 38: Release & Publication** - `v1.7.0_gg6` is published with CHANGELOG, FORK-DELTA, an independently reviewed rollout note and the gg6 allow-list compare. The suite is green twice on one pinned `latest` build, and the published SHA has one green CI run before the tags

## Phase Details

### Phase 33: Dependency Sweep & Protobuf Decision

**Goal**: Every later phase builds on a tree that already holds the four safe Dependabot bumps, each in its own commit, so a later red is attributable to one change. The lz4-java security release (GHSA-48mj-8f7c-7qfm) reaches the shipped jar first. PR #10 is settled by a decision rather than a merge: protobuf-java stays on 3.x, Dependabot stops proposing 4.x for the `gradle` block, and FORK-DELTA records why. Nothing is merged on `origin`, and a rollback tag marks the start. No connector behaviour changes.

**Depends on**: Nothing in this milestone. Phase 32 shipped `v1.7.0_gg5` (`3a3a15a9`). The open Dependabot PRs on `origin` are #10, #16, #17, #18, #19 and #20 (checked 2026-10-28). Local `sync/v1.7.0` differs from `origin/custom_build` only by the gg4 and gg5 release merge commits.

**Requirements**: SWP-01, SWP-02, SWP-03, SWP-04

**Success Criteria** (what must be TRUE):

  1. *[record]* Before the first change, `git ls-remote origin custom_build` was re-read and a local `pre-v1.7.0_gg6` rollback tag exists. Each absorbed bump is a fetched Dependabot PR head, merged on the scratch branch with its head SHA recorded. At the phase gate, a re-read shows that no PR was merged on `origin` during the phase, or that any such merge was absorbed before gating (SWP-04).
  2. *[live]* lz4-java 1.12.1 (PR #19) lands first, in its own commit. `dependencyInsight` shows `at.yawk.lz4:lz4-java:1.12.1` on `runtimeClasspath`. The archive from `createConfluentArchive`, checked by its `verifyConfluentArchive` finalizer, carries 1.12.1. The full suite on `latest` × V2 is green with the 18 named skips (SWP-01).
  3. *[Docker-free, live]* okhttp 5.5.1 (PR #18), Gradle wrapper 9.8.2 (PR #20) and `actions/setup-java` 6.1.0 (PR #16) each land in their own commit, with their own gate (SWP-02):
     - okhttp: `compileIntegrationTestJava` passes.
     - Wrapper: the commit changes only `gradle/wrapper/gradle-wrapper.properties`, `./gradlew --version` reads 9.8.2, and `gradlew.bat` keeps its CRLF line endings.
     - setup-java: every workflow that uses it pins a full 40-hex SHA with a `# v6.1.0` comment, and the pin-guard tests pass under `testUnit`.
     Any anchor row that a bump moves changes in the same commit, and the full suite is green on the sweep tip.
  4. *[Docker-free, record]* PR #10 is not merged. `dependabot.yml` ignores protobuf-java major versions 4 and above, in the `gradle` block only, and the pin guard still passes. `docs/FORK-DELTA.md` records the decision with its measured cause and its re-open trigger. The cause is that `ProtobufIngestTest` fails with `VerifyError` against the Confluent 7.9.1 test stack (PR #10 run 39501799049). The trigger is FUT-03, a move to a Confluent 8.x test stack. PR #10 is either closed by the user, confirmed in their own words, or recorded as closing once the `ignore` reaches `custom_build` at publication (SWP-03).

**Plans**: 3/3 plans complete

Plans:
**Wave 1**
- [x] 33-01-PLAN.md — Wave 1: origin re-read and the local `pre-v1.7.0_gg6` tag first, a baseline full suite on the untouched tip, then the tracer: PR #19 (lz4-java 1.12.1) fetched by ref and merged as its own commit, proven by `dependencyInsight`, in the archive and by the full suite; cumulative anchors gate (SWP-01, SWP-04)

**Wave 2** *(blocked on Wave 1 completion)*
- [x] 33-02-PLAN.md — Wave 2: PR #18 (okhttp 5.5.1), PR #20 (Gradle 9.8.2, `gradlew.bat` CRLF intact) and PR #16 (setup-java 6.1.0), each fetched, merged as its own commit and gated, the full suite on the sweep tip, then the FORK-DELTA rows the sweep made stale (SWP-02)

**Wave 3** *(blocked on Wave 2 completion)*
- [x] 33-03-PLAN.md — Wave 3: the protobuf guard seen red, the `gradle`-entry `ignore` making it green, the FORK-DELTA protobuf row and open item with the `VerifyError` cause and the FUT-03 trigger, PR #10 recorded as closing at publication; the phase gate (SWP-03, SWP-04)

**Research flag**: Standard. Each bump is one line with green PR CI, and the protobuf decision is already made. At plan time, confirm whether Dependabot closes PR #10 by itself once the `ignore` is on the default branch, or whether the user has to close it.

**Note**:
- At the start of the phase, ask the user not to merge Dependabot PRs on `origin` until publication. A merge there diverges the scratch branch again, as it did before gg4 and gg5.
- Decide at plan time what `pre-v1.7.0_gg6` marks. Following `pre-v1.7.0_gg5`, it should be the `origin/custom_build` tip that publication rolls back to. Phase 38 pushes it only after the published SHA's run is green.
- PR #17 (client 0.11.0) is not absorbed here. It lands in Phase 36, after its pins.
- Dependabot rebases its PR heads. Fetch each head by `refs/pull/<N>/head` right before merging it, and record the SHA.
- This phase pushes nothing.

Phase gate: cache-busted `testUnit`, a full suite on `latest` × V2 with `SELECT version()` recorded, and the anchors invariant.

### Phase 34: Upstream Tail Merge

**Goal**: The working tree holds upstream's untagged tail: `9bed6f79`, or whatever `upstream/main` reads immediately before the merge. It is merged in the fork's sync shape while the client is still 0.9.6, so every compile break is attributable to the tail. `additionalRetriableErrorCodes` is a supported, pinned setting, and leaving it unset changes nothing. In `ProxySinkTask`, the fork's JMX `connectorName` plumbing and upstream's `DBWriter` test seam coexist without an ambiguous constructor, and new anchor rows protect both. The BUF fix in Phase 35 edits `ChunkFlusher` again, and it starts from this tree.

**Depends on**: Phase 33 (the sweep tip)

**Requirements**: TAL-01, TAL-02, TAL-03, TAL-04, TAL-05

**Success Criteria** (what must be TRUE):

  1. *[record]* Immediately before the merge, `git ls-remote upstream main` and `git merge-tree` were re-run, and their output is recorded. The merge is one mechanical commit with no authored lines. The unedited `verify-merge-invariants.sh` passes on it, with `UPSTREAM_REF` set to the full upstream SHA and `PRE_TAG=pre-v1.7.0-tail-2`. `.planning/MERGE-CHECKLIST-v1.7.0-tail-2.md` lists every conflicted path and its resolution. The measured set is `CHANGELOG.md`, `Utils.java`, `ClickHouseSinkTaskMappingTest.java`, `ClickHouseWriterTest.java` and `ProxySinkTask.java` (TAL-01).
  2. *[Docker-free]* After the merge, separate commits leave `ProxySinkTask` with two constructors: the fork's `connectorName` constructor and upstream's `DBWriter` test-seam constructor. Both delegate to one 4-argument package-private constructor, and no call site passes an ambiguous `null` (TAL-02). Every fork caller and test of `Utils.handleException` uses the 5-argument form. `compileJava`, `compileTestJava` and `compileIntegrationTestJava` pass after upstream's `CreateTableStatement` refactor (TAL-03).
  3. *[Docker-free]* Tests pin `additionalRetriableErrorCodes` (TAL-04):
     - Parsing trims values, removes duplicates, and rejects non-integers and values <= 0. This holds on both the `ConfigDef` validator and the constructor path.
     - The default retriable set holds exactly 15 codes, including 159.
     - With the key unset, classification is the same as before the merge.
     - A `DataException` is never reclassified as retriable.
  4. *[Docker-free, live]* `fork-anchors.txt` has new rows for the `ProxySinkTask` statistics and `connectorName` plumbing (ORCH-02) and for the `DEFAULT_RETRIABLE_ERROR_CODES` lookup. The anchors gate passes on the phase tip (TAL-05). Two full `--rerun` suite runs on that tip are green on `latest` × V2 with the 18 named skips. That tip is the baseline for Phases 35–37.

**Plans**: 3/3 plans complete

Plans:
**Wave 1**
- [x] 34-01-PLAN.md — Wave 1: upstream and origin re-read, the conflict set re-measured and a baseline suite on the untouched tip, then the tracer: the `pre-v1.7.0-tail-2` tag, one mechanical merge M3 (STOP on a release tag past v1.7.0), the conflicted-paths record and `MERGE-CHECKLIST-v1.7.0-tail-2.md` in the next commit, the unedited tail gate passing; the post-merge break inventory (TAL-01, TAL-03)

**Wave 2** *(blocked on Wave 1 completion)*
- [x] 34-02-PLAN.md — Wave 2: separate repair commits: `ProxySinkTask` with the `connectorName` constructor and the `DBWriter` seam on one four-argument core plus 8 anchor rows, the stale `handleException` caller on the five-argument form plus the `DEFAULT_RETRIABLE_ERROR_CODES` rows, all four compile tasks, the BLD-16 re-bless from the worker jars; the `ProxySinkTaskConstructorUnitTest` pins with a mutation run (TAL-02, TAL-03, TAL-05)

**Wave 3** *(blocked on Wave 2 completion)*
- [x] 34-03-PLAN.md — Wave 3: the `AdditionalRetriableErrorCodesPinUnitTest` pins (parse matrix on both paths, 15-code literal, unset-key sweep, DataException) with a mutation run, the Phase 36/38 handoff record, and the phase gate: origin and upstream re-read, cache-busted `testUnit`, two `--rerun` full suites with the 18 named skips, anchors mirror, tail and cumulative gates (TAL-04, TAL-05, TAL-01)

**Research flag**: Standard. The fork has a rehearsed recipe: the gg1, gg4 and earlier tail merges, the gate script and the checklist. The conflict set was measured with `git merge-tree` against both `sync/v1.7.0` and `origin/custom_build`. If `upstream/main` has moved past `9bed6f79`, re-measure the conflict set before planning. If upstream has tagged a release past `v1.7.0`, stop, because that makes this a tagged sync (FUT-01) and the user decides the scope.

**Note**:
- `ClickHouseSinkConfig` is a high-risk file (AGENTS.md). The mechanical merge takes the fork side of `ProxySinkTask`, and the dual-constructor shape is a separate re-expression commit. Never hand-edit anchors inside the merge commit.
- Two breaks merge without a conflict marker. `UtilsNullMessageClassificationUnitTest` auto-merges with a stale 4-argument call, and about 20 test files must follow the `CreateTableStatement` refactor. Only the compile gates catch these, so those gates matter more than conflict markers.
- Record this for Phase 38: Connect redelivers a code listed in `additionalRetriableErrorCodes`, which bypasses `errors.tolerance` and the DLQ and has no retry budget (FUT-05).
- The tail adds five more 3-argument `ServerException` sites to `UtilsTest`. They compile on 0.9.6, and Phase 36 fixes all eight for 0.11.0 at once.
- This phase pushes nothing.

Phase gate: cache-busted `testUnit`, two `--rerun` full suites on `latest` × V2, and the anchors invariant.

### Phase 35: Buffered Offset Commit Under Topic-Renaming SMTs

**Goal**: With `bufferCount > 0` and a topic-renaming SMT such as `RegexRouter`, the connector commits consumer offsets for the partitions it was assigned. A restart or rebalance then resumes from the last flushed offset instead of rereading from `auto.offset.reset`. Revoked partitions drop their buffered records, and the exactly-once buffer groups records by their original partition. The non-buffered path and the non-renamed case behave exactly as before. `Processing.doLogic` is checked for the same defect, and the outcome is recorded. This fixes upstream #777 on the fork, since upstream PR #786 was closed unmerged.

The fix lands on client 0.9.6, before the client bump, so it can be released independently of that bump. The CLT-09 fallback reverts only the one catalog commit, which leaves this fix intact.

**Depends on**: Phase 34 (the tail edits `ChunkFlusher`, so the fix lands on the merged shape)

**Requirements**: BUF-01, BUF-02, BUF-03, BUF-04, BUF-05, BUF-06, BUF-07

**Success Criteria** (what must be TRUE):

  1. *[Docker-free]* Unit tests build a `SinkRecord` through `newRecord` with a different topic, and they fail on the unchanged code. After the fix, they pin four behaviours (BUF-01, BUF-02, BUF-04):
     - `preCommit` returns offsets keyed by `originalTopic()` and `originalKafkaPartition()`, at `originalKafkaOffset() + 1`.
     - `AtLeastOnceBufferStrategy.close()` drops the buffered records of a revoked partition, matched by original topic and partition.
     - `ExactlyOnceBufferStrategy.put` keys `perPartitionBuffer` by original topic and partition.
     - A record whose topic was not renamed behaves exactly as before.
     `ConnectApiReferencesUnitTest` passes with the three new `SinkRecord` accessors listed. Each accessor is shown to be present in the production worker's 7.9.7 Connect jars.
  2. *[record]* `Processing.doLogic` was checked. Its exactly-once state record is keyed by the transformed topic, `getRecordOffsetContainer().getTopic()`. Its `originalTopic` variable also reads the transformed name, from `getSinkRecord().topic()`. The record states the result with evidence: either a fix with pins that were seen red first, or a measured non-issue. Any fix must keep the `BEFORE`/`AFTER` sequence intact in three cases: a crash before the insert, a crash after it, and a crash during the flag write. It must also say what happens to KeeperMap state rows stored under the old key. The operator runs `exactlyOnce=false`, so this result does not gate the at-least-once fix (BUF-03).
  3. *[live]* On `latest` × V2, a live pin with `bufferCount > 0` and a `RegexRouter` shows committed offsets equal to the end offsets. The non-buffered `DirectDeliveryStrategy` path and the `bufferCount`/`bufferFlushTime` contract are unchanged, and the existing buffer tests pass unedited (BUF-05).
  4. *[record]* The real-environment rerun of the fixed jar is performed and recorded outside this repository and is not gated by this phase (BUF-06).
  5. *[record]* The fix commit carries a `docs/FORK-DELTA.md` row and a `fork-anchors.txt` anchor. FORK-DELTA states that the fix is fork-only until upstream resolves #777 (PR #786 was closed unmerged). A rollout item for Phase 38 is recorded with its evidence label. It says that offsets are now committed, so the group no longer rereads from `auto.offset.reset` after a restart or rebalance. It also says where the first start after the upgrade resumes (BUF-07).

**Plans**: 3/3 plans complete

Plans:
**Wave 1**
- [x] 35-01-PLAN.md — Wave 1: pre-flight and baselines, the red-first Docker-free `BufferedOffsetCommitUnitTest` built through `newRecord` (8 of 13 executions red on the unchanged tree), then one fix commit: the three offset sites keyed by `originalTopic()`, `originalKafkaPartition()` and `originalKafkaOffset() + 1`, the BLD-16 re-bless from the 7.9.7 worker jars, 7 anchor rows and the FORK-DELTA row, with a mutation proof (BUF-01, BUF-02, BUF-04, BUF-07)

**Wave 2** *(blocked on Wave 1 completion)*
- [x] 35-02-PLAN.md — Wave 2: the ClickHouse-backed live pin written red in a worktree on the unchanged tree, with the harness decision recorded, then the BUF-03 probes on `Processing.doLogic` in a worktree, a Docker-free state-key tripwire and `35-BUF03-RECORD.md` with the open items for the milestone close (BUF-02, BUF-03, BUF-05)

**Wave 3** *(blocked on Wave 2 completion)*
- [x] 35-03-PLAN.md — Wave 3: the labelled rollout item for Phase 38 with the first-start derivation, and the phase gate: cache-busted `testUnit`, one full `--rerun` suite on `latest` with client V2, the anchors mirror, the tail and cumulative merge gates and the scope facts (BUF-07, BUF-01 to BUF-05)

**Research flag**: Settle these before planning:
- **Where the live pin lives.** One option is the `test` suite, which has ClickHouse only and drives `preCommit` directly. The other is the `integrationTest` Connect-worker harness, which has a real consumer group and real committed offsets. BUF-05 asks for committed offsets, so record why the chosen harness observes them.
- **`Processing.doLogic`.** A renaming SMT can map two source topics to one target. If their partition numbers are equal, can they share one exactly-once state record? If so, what does the state machine do with the interleaved ranges? Measure this with `InMemoryState` and `ProcessingTest`-style pins before any edit.
- **First start after the upgrade.** Before the fix, the group committed nothing for these partitions. Derive where the first start on the fixed jar resumes from Kafka consumer-group semantics and, if cheap, measure it in the repo's own test harness, so the rollout item can state it.

**Note**:
- The fix is written against the post-tail `ChunkFlusher`, after its tests are seen red.
- `ChunkFlusher` and both buffer strategies are on the hot path (AGENTS.md). Add no per-record allocation beyond the `TopicPartition` the code already builds.
- A change to `Processing.java` or `KeeperStateProvider` is high-risk. Changing the state key would orphan the stored state of existing exactly-once deployments, so such a change stops for a user decision before it lands.

Phase gate: cache-busted `testUnit`, a full suite on `latest` × V2, and the anchors invariant.

### Phase 36: Client 0.11.0 Audit & Bump

**Goal**: The connector runs on ClickHouse Java client 0.11.0, and every behaviour change is recorded. Each classification, endpoint, config-key and compression behaviour that operators rely on is pinned first on 0.9.6 and then on 0.11.0. The two required `createClientV2` fixes land. An exactly-once retry across the upgrade either sends identical bytes or needs a documented clean stop. The archive's change is recorded. The bump is one catalog commit, so if the audit fails, gg6 ships on 0.9.6 by reverting it (CLT-09).

**Depends on**: Phase 35. The pins target the final 5-argument `handleException`, the tail's 15-code default set and the post-BUF tree. If CLT-09 fires, the revert leaves Phases 33–35 intact.

**Requirements**: CLT-01, CLT-02, CLT-03, CLT-04, CLT-05, CLT-06, CLT-07, CLT-08, CLT-09

**Success Criteria** (what must be TRUE):

  1. *[live, Docker-free]* Before the bump, pins on 0.9.6 are committed green (CLT-01):
     - Error 159 is a `RetriableException`.
     - A 502 or 503, and a `ClientException` with an unexpected status, are classified as they are today.
     - The timeout-message prefixes are produced.
     The endpoint pin and the `jdbcConnectionProperties` pin are also green on 0.9.6. The first checks that the endpoint has no path. The second checks that `?ssl=true&sslmode=strict` still connects. On 0.11.0 without the two fixes, both pins were seen red.
  2. *[Docker-free, live]* `clickHouseDriver` moves to 0.11.0 as a single catalog change marked `# FORK OVERRIDE (upstream 0.9.6)`, with an anchor row and a Docker-free catalog pin test (CLT-02). `ClickHouseHelperClient.createClientV2` passes only `protocol://host:port` to `addEndpoint`. It sets `ignore_unknown_config_key=true` before the `jdbcConnectionProperties` loop. Both pins are green on 0.11.0 (CLT-03).
  3. *[live]* `Client011BehaviourPinLiveTest` pins four behaviours on 0.11.0 (CLT-04):
     - Error 159 is a `RetriableException`.
     - The 502 and 503 shapes are classified, both with and without `retryOnSocketException`.
     - A `ClientException` is non-retriable.
     - V2 decodes a ZSTD response with `setCompressionMethod=false` and `compress=true`.
     The date and datetime live pins also pass on a non-UTC JVM. The Connect-worker `integrationTest` passes on 0.11.0, and so does one manual nightly dispatch, which the user starts and reports directly (CLT-07).
  4. *[live, record]* Exactly-once safety is recorded (CLT-06). A RowBinary byte comparison of 0.9.6 against 0.11.0 covers the type matrices, including the nested `Tuple` and `Map` nullable markers. A `BEFORE_PROCESSING` batch replayed across the upgrade either has identical bytes, or the rollout item for Phase 38 states the clean-stop requirement.
  5. *[record, live]* The D-17 `network_compression_method=lz4` default stays. Its rationale is reworded to what was measured on 0.11.0, in three places: the Javadoc, `docs/FORK-DELTA.md` (Recorded Fact 26 and the CMP rows), and the warning in `docs/ROLLOUT-v1.7.0_gg1.md` (CLT-05). The Confluent archive listings are captured before and after the bump, and the `zstd-jni` addition and the lost guava CVE override are recorded (CLT-08). A go or no-go decision is recorded with its evidence (CLT-09):
     - On go, two full `--rerun` suite runs are green on 0.11.0.
     - On no-go, the catalog commit is reverted, the record lists which fixes and pins were kept, and the suite is green on 0.9.6.

**Plans**: TBD

**Research flag**: Settle these before planning:
- **503 and `ConnectException`.** This classification is derived from source and was never exercised against a real proxy. Decide whether a fault-injection pin is feasible, or label the rollout wording "read from the code".
- **zstd-jni.** Check native loading inside a Connect plugin classloader and under a noexec `/tmp`, and list the platforms the bundled natives cover.
- **V1 client path.** Beyond the existing live tests, only compilation gates it.
- **Socket buffers.** Their throughput effect is unmeasured, and changing them is out of scope. Record what the rollout note can claim.
- **What the CLT-09 revert keeps.** For each `createClientV2` fix, confirm whether it is safe on 0.9.6. In particular, check whether 0.9.6 accepts `ignore_unknown_config_key` or forwards it to the server.

**Note**:
- Order inside the phase: the 0.9.6 pins first, then the bump with the two fixes. Next come the pin class, the byte comparison, the doc rewording and the archive listings. The two runs come last.
- Arrange the commits so that the CLT-09 revert really is one commit. The `ServerException` constructor in 0.11.0 takes four arguments. So the eight `UtilsTest` sites either ride in the catalog commit or are written to compile on both clients.
- Do not trust PR #17's green CI. It never reached the `Test` task, and `testUnit` hides both runtime breaks.
- Out of scope, per REQUIREMENTS: adding `ServerTimeoutExceeded` to the client retry list, setting `socket_rcvbuf`/`socket_sndbuf`, and removing D-17.
- The nightly dispatch needs the scratch branch on `origin`, at a SHA that no other pushed branch holds. That push publishes the local `.planning` tail one way and runs `tests.yaml` once. It needs the user's direct approval, as in Phase 30.
- Record `SELECT version()` in every evidence block, because `latest` drifts.

Phase gate: cache-busted `testUnit`, two `--rerun` full suites on `latest` × V2 on whichever client ships, and the anchors invariant.

### Phase 37: CI Hardening & Advisory Debt

**Goal**: The fork's release tooling stops carrying known defects. `increment-version.yml` can bump a `_ggN` version without writing `vnull`, because its logic is a script with a test. The release workflows compare the built Confluent archive entry by entry against the previous release, under an allow-list keyed by `VERSION`. An archive change such as the zstd-jni native jar can then no longer ship unseen. Every review advisory carried from Phases 25–31 is fixed, or dispositioned with an owner and a re-open trigger. The real `v1.7.0_gg5` release is on the record. No connector behaviour changes.

**Depends on**: Phase 36. The compare consumer is written against the real post-bump archive, and it shares the pin-guard test files with the version workflow.

**Requirements**: CIH-01, CIH-02, CIH-03, CIH-04, CIH-05, ADV-01, ADV-02, ADV-03

**Success Criteria** (what must be TRUE):

  1. *[Docker-free]* The version logic is a script with a table-driven test that runs with Docker stopped (CIH-01, CIH-02). The test covers four rules:
     - Input and output are validated against `^v\d+\.\d+\.\d+(_gg[1-9]\d*)?$`.
     - `gg` bumps the counter (`v1.7.0_gg5` → `v1.7.0_gg6`), and turns a suffix-less `vX.Y.Z` into `vX.Y.Z_gg1`.
     - `patch`, `minor` and `major` reset the suffix to `_gg1`.
     - Invalid input fails, and nothing is written.
     No case writes `vnull`. `increment-version.yml` calls the script and installs no npm `semver`. It keeps its SHA pins and permissions, and it passes the pin guard. Its "known and left unfixed" header note is gone, and FORK-DELTA's row for the file no longer calls the defect open.
  2. *[Docker-free]* The version bump also edits the `VERSION` row of `fork-anchors.txt`, or the script fails when that row does not match. The failure was seen red on a fixture (CIH-03).
  3. *[Docker-free]* The compare script lives under `.github/scripts/`. Its synthetic-archive self-test covers the UNALLOWED and STALE-ROW failures and the exit codes 0, 1 and 2 (CIH-05). `release.yaml` and `pre-release.yaml` run it against an allow-list keyed by `VERSION`, and they publish the report as an artifact. The baseline is rebuilt from the previous tag in a separate worktree. The `REL-xx` anchor counts and `TestsWorkflowPinGuardUnitTest` are updated, and both pass (CIH-04).
  4. *[record]* The Phase 30 review rows (WR-01..WR-03, IN-01..IN-04) and the Phase 31 rows (WR-01..WR-03, IN-01..IN-03) are each closed. A row is either fixed, with a commit pointer, or dispositioned with an owner and a re-open trigger (ADV-01). The same holds for the 16 unfixed findings in `29-MILESTONE-CLOSE-RECORD.md` (ADV-02). A diff check shows that no existing assertion was deleted or weakened, and an assertion changes only with the user's sign-off.
  5. *[record, CI]* The real `v1.7.0_gg5` GitHub release of 2026-10-28 is recorded as evidence, read from the REST API. The record gives the release, the workflow run that created it, its tag commit, and the attached archive's name, size and SHA-256. This closes the "no run exercised the publish path" caveat (ADV-03).

**Plans**: TBD

**Research flag**: Settle these before planning:
- **The compare before `VERSION` moves.** Phase 38 owns the `VERSION` bump and the gg6 allow-list, which is generated last (RLS-03). Decide what this phase's step runs against. One option is a dry-run dispatch against a provisional allow-list. The other is to prove here only that the step runs and reports, and to leave the passing compare to RLS-03 and RLS-05.
- **The tag rebuild.** The real gg5 release asset (ADV-03) is the first chance to check that a rebuild from the tag reproduces the published archive under Shadow 9. Record the result before the compare relies on a rebuilt baseline.
- **Allow-list naming.** The allow-list is keyed by `VERSION` (CIH-04). Settle how it relates to the existing `confluent-archive-allowlist-25/29/32.txt` under `.planning/scripts/`.

**Note**:
- Phase 38 cannot edit `.github/`, by the Phase 29 D-05 standing rule. Every workflow fix belongs here, including a fix for a defect found in a Phase 38 dry run. Phase 30 put its dry run in the workflow phase for the same reason. Decide at plan time whether a dry-run dispatch here proves the compare step before Phase 38 relies on it. That dispatch needs the user's approval for the push, and the user starts the run and reports it.
- Phase 30 WR-01 (the shared `release-publish` concurrency group) and IN-01..IN-04 (about 180 duplicated lines) touch the same release files as CIH-04. Sequence them with it.
- Some gg4 findings sit in `src/main`. For example, 26-REVIEW IN-13 notes that the fork-owned `allColumnsMap.merge` has no anchor row. A `src/main` change outside this phase's scope stops for a user decision.

Phase gate: cache-busted `testUnit` with the pin-guard and script self-tests, a full suite on `latest` × V2, the anchors invariant, and an empty `src/main` diff unless the user approved a fix.

### Phase 38: Release & Publication

**Goal**: `v1.7.0_gg6` is published on `custom_build`. Operators get an independently reviewed rollout note that labels every operator-visible effect with its evidence. That covers the buffered-offset fix, the client 0.11.0 shifts (or the fact that the client stayed 0.9.6), `additionalRetriableErrorCodes` and the clean-stop requirement. The gg6 allow-list compare passes on the final tree. The suite is green twice on one pinned `latest` build, and the published SHA has one green `tests.yaml` run before the tags are pushed.

**Depends on**: Phases 33–37 (it documents and publishes their outcome)

**Requirements**: RLS-01, RLS-02, RLS-03, RLS-04, RLS-05, RLS-06

**Success Criteria** (what must be TRUE):

  1. *[Docker-free]* After `clean`, `VERSION` reads `v1.7.0_gg6` and the generated version agrees. The `VERSION` anchor row changes in the same commit. `CHANGELOG.md` has a complete `1.7.0_gg6` section that absorbs upstream's `Unreleased` entries. `docs/FORK-DELTA.md` is final, with its raw numbers refreshed once (RLS-01).
  2. *[record]* `docs/ROLLOUT-v1.7.0_gg6.md` states each operator-visible effect, labelled "measured" or "read from the code" (RLS-02):
     - `ssl` and `sslmode` were never honoured on V2.
     - The 159 retry cadence and the 502/503 shape.
     - The `User-Agent` tail.
     - The socket-buffer opt-back keys and the zstd native platforms.
     - `additionalRetriableErrorCodes`, including its bypass of `errors.tolerance`.
     - The clean-stop requirement.
     - The buffered offset-commit change that Phase 35 recorded (BUF-07).
     If CLT-09 fired, the note says the client stayed 0.9.6 and keeps only the items that hold on 0.9.6.
  3. *[record]* Before the push gate, an orchestrator-spawned, fresh-context `gsd-code-reviewer` checked the note together with `CHANGELOG.md` and `docs/FORK-DELTA.md`. Rounds repeated until one found nothing, at most three unless the user authorised another. Every finding's disposition is recorded (RLS-04). The gg6 allow-list was generated last, from the final tree, and the compare passes against it (RLS-03).
  4. *[live]* On the release tip, two full `--rerun` suite runs pass on one pinned `latest` build with client V2, with `SELECT version()` recorded and the named skips. `testUnit` passes with Docker stopped, and the anchors gate passes (RLS-06).
  5. *[CI, operator]* The release dry run passes. `git log sync/v1.7.0..origin/custom_build` was checked for divergence, and any change there was absorbed before gating. After the user's own direct approval (RLS-05):
     - `custom_build` is a non-force `--no-ff` merge whose tree equals the rehearsed tip.
     - The published SHA has exactly one `tests.yaml` run, and it is green before the tags `v1.7.0_gg6` and `pre-v1.7.0_gg6` are pushed.
     - The scratch branch is deleted from `origin`.

**Plans**: TBD

**Research flag**: Standard; this phase follows Phases 29 and 32. Decide at plan time where the `v1.7.0_gg6` tag and the GitHub release come from. One option is the hand-pushed tags of the Phase 29 recipe. The other is a real `release.yaml` dispatch after the green run, which is the path the user took for gg5 (ADV-03).

**Note**: This phase edits no `src/`, test, `build.gradle.kts`, `gradle/` or `.github/` file (the Phase 29 D-05 standing rule). A defect found here goes back to its phase as a gap. Otherwise it is deferred with a reason, an owner and a re-open trigger in the milestone close record. The phase follows the Phase 29 and Phase 32 recipe (rehearse, measure, approve, publish):
1. Push the scratch branch once, at a SHA that no other pushed branch holds, and wait for green.
2. Merge with `--no-ff` from a throwaway worktree, so the merged tree equals the rehearsed tip.
3. Push `custom_build` only, and wait for its single run.
4. Push the tags by name, only after that run is green.
5. Delete the scratch branch.

The approval gates run orchestrator-inline, because executors refuse relayed approvals and `custom_build` is GSD's protected base branch. The divergence check's left side is expected to hold the gg4 and gg5 release merges, so its count is not 0. After publication, re-read the state of the absorbed Dependabot PRs and of PR #10 (SWP-03).

## Structure Notes

**Why six phases under `coarse` granularity.** The 39 requirements fall into six delivery boundaries, and each is proven differently:
- the sweep, by one gate per bump;
- the tail, by the merge-invariant script and the compile gates;
- the BUF fix, by red-first unit pins, and a live commit check;
- the client, by pins on two client versions and a byte comparison;
- the CI and advisory work, by Docker-free guards and ledgers;
- the release, by the publication recipe.

The order is the user's (2026-10-28). Phases 33 and 34 could share a phase under `coarse`, as Phase 25 did. They stay separate so that the tail's merge gate runs on a tree whose only other change is four one-line bumps.

**Placement choices worth recording:**

- **SWP-04's rollback tag comes before the first change.** It is the milestone's first act.
- **The tail comes before BUF.** The tail edits `ChunkFlusher` to pass the extra retriable set. The fix therefore lands on the merged shape and does not have to be re-resolved.
- **BUF comes before the client.** The fix is production-relevant and must be releasable on 0.9.6. Landing it first means CLT-09's one-commit revert cannot touch it. Its live pin then runs on 0.11.0 in the gates of Phases 36 and 38.
- **The client comes last among the code changes.** It is the only optional change that alters behaviour, so descoping it costs one revert.
- **CIH comes after the client.** The archive compare is written against the real post-bump archive, which adds zstd-jni. The gg6 allow-list itself is generated last, in Phase 38 (RLS-03), because STALE-ROW makes an allow-list valid for exactly one tree.
- **ADV sits with CIH.** Most carried advisories are in the same release workflows and pin-guard files, and the release phase cannot edit `.github/`. ADV-03's record of the real gg5 release also checks the compare's tag-rebuilt baseline.
- **Phase 35 owns BUF-07.** Its FORK-DELTA row and anchor land with the fix, and its rollout item is recorded there with evidence. Phase 38 places that item in the note, and RLS-02's criterion checks it.
- **CLT-05 edits a published rollout note.** Phase 36 rewords the D-17 warning in the gg1 note. This follows the gg4 precedent, which corrected the published gg1 and gg2 notes.

**Execution model.** Phases run sequentially on one working tree (`sync/v1.7.0`). Phases 34, 35 and 36 change `src/main`. Phase 37 changes it only with the user's approval, and Phases 33 and 38 do not change it.

**Files shared across phases:**

| File | Phases | Rule |
|------|--------|------|
| `gradle/libs.versions.toml` | 33 (lz4, okhttp), 34 (merge only), 36 (`clickHouseDriver`) | One bump per commit; the client bump is one revertible commit |
| `.github/workflows/*` | 33 (setup-java SHA), 37 (version script, archive compare, advisories) | Each edit passes the pin guard; Phase 38 edits none |
| `.github/dependabot.yml` | 33 (protobuf `ignore`), 37 (Phase 30 WR-03 comment, if fixed) | — |
| `TestsWorkflowPinGuardUnitTest` and the other pin-guard tests | 33, 37 | A new rule is seen red first; an existing assertion changes only with the user's sign-off |
| `ProxySinkTask`, `ChunkFlusher` | 34 (tail, dual constructor), 35 (BUF) | BUF lands after the tail |
| `Utils.handleException`, `ClickHouseSinkConfig` | 34 | High-risk; Phase 36 leaves D-17 unchanged |
| `ClickHouseHelperClient` | 36 | The bump's only production-code change |
| `Processing.java`, `KeeperStateProvider` | 35, only if BUF-03 finds a fix | High-risk; a state-key change stops for a user decision |
| `src/test/resources/connect-api-references.txt` | 35 | Updated only with evidence from the production worker's jars |
| `.planning/scripts/fork-anchors.txt` | 33–38 | Anchor rows change in the same commit as the code |
| `docs/FORK-DELTA.md` | 33 (protobuf), 35 (BUF row), 36 (D-17 rewording, client override), 37 (`increment-version.yml` row), 38 (final) | Raw numbers are refreshed once, in Phase 38 |
| `docs/ROLLOUT-v1.7.0_gg1.md` | 36 (CLT-05) | — |
| `CHANGELOG.md`, `VERSION`, `docs/ROLLOUT-v1.7.0_gg6.md` | 38 | Phase 34's merge resolves the `CHANGELOG.md` conflict mechanically; the `1.7.0_gg6` section is written in Phase 38 |

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

**Execution Order:** 33 → 34 → 35 → 36 → 37 → 38, sequential on one working tree.

| Phase | Milestone | Plans Complete | Status | Completed |
|-------|-----------|----------------|--------|-----------|
| 33. Dependency Sweep & Protobuf Decision | v1.7.0_gg6-maintenance | 3/3 | Complete    | 2026-10-28 |
| 34. Upstream Tail Merge | v1.7.0_gg6-maintenance | 3/3 | Complete    | 2026-10-29 |
| 35. Buffered Offset Commit Under Topic-Renaming SMTs | v1.7.0_gg6-maintenance | 3/3 | Complete    | 2026-10-29 |
| 36. Client 0.11.0 Audit & Bump | v1.7.0_gg6-maintenance | 0/TBD | Not started | - |
| 37. CI Hardening & Advisory Debt | v1.7.0_gg6-maintenance | 0/TBD | Not started | - |
| 38. Release & Publication | v1.7.0_gg6-maintenance | 0/TBD | Not started | - |

---
*Roadmap reorganized at the `v1.7.0_gg5-ci` close: 2026-10-28. Roadmap for milestone `v1.7.0_gg6-maintenance` created: 2026-10-28.*
