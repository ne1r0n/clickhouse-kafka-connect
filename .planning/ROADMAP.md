# Roadmap: ClickHouse Kafka Connect — `custom_build` Fork Test Coverage

## Milestones

- ✅ **v1.2.9_gg4-tests Fork Test Coverage** — Phases 1-5 (shipped 2026-09-18)
- ✅ **v1.6.0_gg1-sync Upstream v1.6.0 Sync** — Phases 6-11 (shipped 2026-10-07)
- ✅ **v1.7.0_gg1-sync Upstream v1.7.0 Sync & Follow-up Hardening** — Phases 12-15 (shipped 2026-10-10)
- ✅ **v1.7.0_gg2-hardening Follow-up Hardening & CI Hygiene** — Phases 16-20 (shipped 2026-10-14)
- ✅ **v1.7.0_gg3-hardening Record-Data Exposure & Review Debt** — Phases 21-24 (shipped 2026-10-20)
- ✅ **v1.7.0_gg4-hardening Dependency Absorption, Discovery Defects & Review Debt** — Phases 25-29 (shipped 2026-10-23)
- ✅ **v1.7.0_gg5-ci CI & Release Workflow Modernization** — Phases 30-32 (shipped 2026-10-27)
- ✅ **v1.7.0_gg6-maintenance Maintenance: Dependency, Upstream Tail, Client 0.11.0 & Release** — Phases 33-38 (shipped 2026-11-01)
- ✅ **v1.7.0_gg7-followup Follow-up Debt, Dependency Sweep & Document Cleanup** — Phases 39-44 (shipped 2026-11-03)
- 🚧 **v1.7.0_gg8 Gap Closure, Upstream Tail & Release** — Phases 45-49 (in progress)

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

<details>
<summary>✅ v1.7.0_gg6-maintenance Maintenance: Dependency, Upstream Tail, Client 0.11.0 & Release (Phases 33-38) — SHIPPED 2026-11-01</summary>

- [x] Phase 33: Dependency Sweep & Protobuf Decision (3/3 plans) — completed 2026-10-28
- [x] Phase 34: Upstream Tail Merge (3/3 plans) — completed 2026-10-29
- [x] Phase 35: Buffered Offset Commit Under Topic-Renaming SMTs (3/3 plans) — completed 2026-10-29
- [x] Phase 36: Client 0.11.0 Audit & Bump (4/4 plans) — completed 2026-10-31
- [x] Phase 37: CI Hardening & Advisory Debt (5/5 plans) — completed 2026-10-31
- [x] Phase 38: Release & Publication (5/5 plans) — completed 2026-11-01

Full phase details: [`milestones/v1.7.0_gg6-maintenance-ROADMAP.md`](milestones/v1.7.0_gg6-maintenance-ROADMAP.md)
Requirements as shipped: [`milestones/v1.7.0_gg6-maintenance-REQUIREMENTS.md`](milestones/v1.7.0_gg6-maintenance-REQUIREMENTS.md)
Milestone audit: [`milestones/v1.7.0_gg6-maintenance-MILESTONE-AUDIT.md`](milestones/v1.7.0_gg6-maintenance-MILESTONE-AUDIT.md)
Phase artifacts: `milestones/v1.7.0_gg6-maintenance-phases/`

</details>

<details>
<summary>✅ v1.7.0_gg7-followup Follow-up Debt, Dependency Sweep & Document Cleanup (Phases 39-44) — SHIPPED 2026-11-03</summary>

- [x] Phase 39: Dependabot Absorption & Behaviour Pins (4/4 plans) — completed 2026-11-01
- [x] Phase 40: Helper Client Closing (3/3 plans) — completed 2026-11-02
- [x] Phase 41: Bounded Insert Timeout (3/3 plans) — completed 2026-11-02
- [x] Phase 42: Isolated High-Risk Edits (4/4 plans) — completed 2026-11-02
- [x] Phase 43: Document Fixes & Phase 37 Dispositions (3/3 plans) — completed 2026-11-02
- [x] Phase 44: Release & Publication (5/5 plans) — completed 2026-11-04

Full phase details: [`milestones/v1.7.0_gg7-followup-ROADMAP.md`](milestones/v1.7.0_gg7-followup-ROADMAP.md)
Requirements as shipped: [`milestones/v1.7.0_gg7-followup-REQUIREMENTS.md`](milestones/v1.7.0_gg7-followup-REQUIREMENTS.md)
Milestone audit: [`milestones/v1.7.0_gg7-followup-MILESTONE-AUDIT.md`](milestones/v1.7.0_gg7-followup-MILESTONE-AUDIT.md)
Phase artifacts: `milestones/v1.7.0_gg7-followup-phases/`

</details>

### 🚧 v1.7.0_gg8 Gap Closure, Upstream Tail & Release (In Progress)

**Milestone Goal:** Close every known gap left by `v1.7.0_gg7-followup`, take upstream's new tail (`main` `9bed6f79` → `83808df9`) while keeping the fork's value-free error policy, and publish `v1.7.0_gg8` with the suite green on ClickHouse `latest` × client V2 and CI green on the published SHA. (`PROJECT.md` calls this milestone `v1.7.0_gg8-closeout`; `STATE.md` and the tooling key it as `v1.7.0_gg8`.)

**Criteria tags:** *[live]* needs Docker and ClickHouse `latest` × client V2. *[Docker-free]* runs under `testUnit` or as a build or static check with Docker unreachable. *[record]* is a committed evidence file or document whose content is checked. *[operator]* is a user action or decision, confirmed in the user's own words. *[CI]* is read from GitHub Actions.

- [ ] **Phase 45: Upstream Tail Merge & Dependency Check** - Upstream `83808df9` lands as one mechanical merge behind `pre-v1.7.0_gg8`. #857 is re-expressed value-free, so the gg3 message and classification pins pass unchanged. Slash column names (#839) round-trip, `fromJsonRow` is reconciled with the gg4 raw-identifier handling, and upstream's three bumps are taken. A manual dependency and CVE check of the merged catalog follows, and only the bumps the user approves land, one commit each
- [ ] **Phase 46: Insert Timeout Limits** - Each case that `clickhouseClientInsertTimeoutMs` leaves unbounded today is bounded behind a pin seen red first, or recorded with a measured number: an insert streaming progress headers, a write-side stall, client V1 `future.get()` inserts and non-insert requests. The help text is then rewritten to say what the setting bounds, pinned Docker-free
- [ ] **Phase 47: Task Stop & Retry Classification** - `ClickHouseSinkTask.stop()` reaches every cleanup step when a delivery strategy throws, pinned red-first, and the CHANGELOG bullets match the code. A retriable `DataException` gets a retry budget or a pinned, documented limit under both `errors.tolerance` values, and FUT-05 closes with a recorded decision
- [ ] **Phase 48: Phase 37 Dispositions & Document Fixes** - The four `src/main` advisory rows, the null assertion, the 28 IN-05(b) items, the broad allow-list glob rows and the review Infos each end fixed (tests first, one commit each) or closed with a written reason, and none is left as "owner: user". IN-03 leaves the published gg6 note, and FORK-DELTA's evidence paths survive archiving
- [ ] **Phase 49: Release & Publication** - `v1.7.0_gg8` is published with CHANGELOG, FORK-DELTA, the gg8 allow-list compare and an independently reviewed rollout note. Two equal green suites, the `--no-ff` merge only on the user's explicit approval, one green `tests.yaml` on the published SHA, the publish run creates the tag, and the two `scratch/clt-07-*` branches leave `origin` after a separate confirmation

## Phase Details

### Phase 45: Upstream Tail Merge & Dependency Check

**Goal**: The code-gap phases start from a tree that already holds upstream's tail, so a later red points at the fork's own change and not at the merge. The tail is merged in the fork's sync shape: scratch branch, rollback tag, one mechanical commit, a per-path checklist, the invariant script and two full runs. Upstream #857's record context is kept only where it carries no record value. An operator still sees the column, the types and the exception class, never the value, and the retry classification does not move. Slash column names (#839) round-trip, and `fromJsonRow` agrees with the fork's raw-identifier names. The dependencies that ship have been checked for open CVEs on the merged catalog, and every bump is the user's decision.

**Depends on**: Nothing in this milestone. Phase 44 published `v1.7.0_gg7` (`94834763`). Measured 2026-11-04:
- local `upstream/HEAD` is `83808df9`, there is no local tag past `v1.7.0`, and upstream's `VERSION` at `83808df9` reads `v1.7.1`;
- a `git merge-tree` dry run of `83808df9` into `sync/v1.7.0` (`2efb2213`) conflicts in six paths: `.github/workflows/tests.yaml`, `CHANGELOG.md`, `VERSION`, `gradle/libs.versions.toml`, `ClickHouseFieldDescriptor.java` and `ClickHouseHelperClient.java`;
- the same dry run auto-merges `ClickHouseWriter.java` (#857) and `build.gradle.kts` with no conflict marker.

**Requirements**: UPS-01, UPS-02, UPS-03, UPS-04, UPS-05, DEP-01, DEP-02

**Success Criteria** (what must be TRUE):

  1. *[record]* Immediately before the merge, `git ls-remote upstream main`, upstream's tag list and `git merge-tree` were re-run, and their output is recorded. `pre-v1.7.0_gg8` tags the pre-merge tip, and the work happens on a scratch branch. The merge is one mechanical commit with no authored lines. A generated per-path checklist (`.planning/MERGE-CHECKLIST-v1.7.0-tail-3.md`) lists every conflicted path and every auto-merged hunk in a fork-touched file, each with its resolution (UPS-01).
  2. *[Docker-free, record]* Before #857's code is re-expressed, a recorded measurement shows what its record context and message carry for each writer failure shape. After the repair commit, no error message, DLQ header or log line of a serialization failure carries a record value. The gg3 column/type/class-only pins and the 184-case `WriterFailureClassificationUnitTest` pass unchanged, and every difference from upstream's text is named in FORK-DELTA (UPS-02).
  3. *[live]* A column whose name contains a slash (#839) round-trips on ClickHouse `latest` × client V2 through DESCRIBE and insert, with typed read-back, under the fork's raw-identifier matching. `fromJsonRow` is reconciled with the Phase 28 handling: the gg4 NAME-01..NAME-07 pins pass unchanged, and each behaviour that differs from upstream's `fromJsonRow` is pinned by a test and named in FORK-DELTA (UPS-03, UPS-04).
  4. *[Docker-free, live]* slf4j-simple 2.0.20, spotless 8.10.4 and commons-lang3 3.21.0 resolve on their classpaths (by `dependencyInsight`). `verify-merge-invariants.sh` reads `PASS` on the phase tip with `PRE_TAG=pre-v1.7.0_gg8` and the full upstream SHA, and the anchor count changes in the same commit as the rows it counts. Two full `--rerun` suites on that tip are green on `latest` × V2 (UPS-05).
  5. *[record, operator]* A manual dependency and CVE check of the merged catalog is recorded. It lists every runtime and test dependency with its latest version and any open advisory, and it names each dependency with an open CVE. Each proposed bump carries the user's decision in their own words. Each approved bump is its own commit, with `testUnit` green with Docker unreachable and the full suite green on `latest` × V2. If none is approved, the record says so and nothing moves (DEP-01, DEP-02).

**Plans**: 5 plans

Plans:
- [ ] 45-01-PLAN.md — pre-flight re-read and baseline suites on the untouched tip, rollback tag `pre-v1.7.0_gg8`, one mechanical merge of `83808df9` (six conflicts and the dead `describeTableV1` reader on the fork side), the tail-3 checklist and the unedited tail gate, then the bare-merge break inventory (UPS-01)
- [ ] 45-02-PLAN.md — the U-2 upstream-test edits (RecordContextTest compile fix, FieldDescriptorEscapeTest literal JSON) with testUnit green and a full suite red on WRT-10 alone, then the #857 measurement of 15 failure shapes in a worktree probe (UPS-02, UPS-04)
- [ ] 45-03-PLAN.md — U-1 Option A: the writer keeps its own exception and logs coordinates value-free, Docker-free pins through `writeRecordsRowBinary` and `ProxySinkTask.put` seen red first, four anchor rows, mutations, live WRT-10 and the gg3 pins unchanged (UPS-02)
- [ ] 45-04-PLAN.md — the `fromJsonRow` literal-fields pin biting against upstream's decode-all, the live slash-column and NAME run on `latest` × V2, the UPS-05 `dependencyInsight` facts with three anchor rows, and the FORK-DELTA rows naming every difference (UPS-03, UPS-04, UPS-05, UPS-02)
- [ ] 45-05-PLAN.md — the DEP-01 record (shipped set first, then test classpaths, plugins, actions, images), the DEP-02 disposition under U-4, and the phase gate: testUnit with Docker unreachable, two `--rerun` suites, the tail gate on explicit SHAs, nothing pushed (DEP-01, DEP-02, UPS-05, UPS-01)

**Research flag**: Standard. The fork has a rehearsed recipe: Phase 34 and the earlier tails, the invariant script and the checklist. Settle these at plan time:
- whether upstream has tagged a release past `v1.7.0`. Upstream's `VERSION` already reads `v1.7.1`. A tag at or past the merge target makes this a tagged sync (FUT-01), so stop, and the user decides the scope;
- whether `upstream/main` has moved past `83808df9`. If it has, re-measure the conflict set before planning;
- what #857's `serializationFailure` puts in the text. It formats `cause.getMessage()` into the `DataException` message and logs it at ERROR with the cause. It also wraps every exception that is not a `RetriableException`, including the IO chains, the `ConnectException` and the exact `RuntimeException` that the gg3 policy passes through. Taken as it is, it changes the value-free text and the retry classification at once. Whether the fork keeps the topic, partition and offset coordinates (metadata, not record values) is the user's call at plan time;
- how upstream's new tests (`ClickHouseWriterRecordContextTest`, `SlashColumnRoundTripTest`, `FieldDescriptorEscapeTest`) are kept: green as they are, or as a named fork difference decided by the user;
- `build.gradle.kts` gains `connect-json` (version ref `kafka`) on the test classpath. Check it against the Connect API pin and the Confluent test stack;
- the scope of the DEP-01 check (runtime, test fixtures, Gradle plugins, workflow actions) and where the advisories are read from, since the repository's Dependabot alerts are disabled.

**Note**:
- #857 auto-merges into `ClickHouseWriter.java` with no conflict marker (the 2026-11-04 dry run). Only the gg3 pins and the classification matrix catch it, so those gates matter more than conflict markers.
- The merge takes the fork side of `VERSION`, `CHANGELOG.md` and `tests.yaml`; upstream renames its cloud secrets in a step of its own workflow. The pin guard and the workflow guard classes pass on the phase tip.
- No fork-authored bump rides inside the sync commit. DEP-02 bumps land after the merge and its repair commits.
- AGENTS.md: no dependency moves without the user's approval. New tests use no Mockito inline mocks (OpenJ9).
- This phase pushes nothing.

Phase gate: cache-busted `testUnit` with Docker unreachable, two `--rerun` full suites on `latest` × V2 with `SELECT version()` recorded, and the invariant script once on an explicit SHA.

### Phase 46: Insert Timeout Limits

**Goal**: An operator can trust what `clickhouseClientInsertTimeoutMs` says. The gg7 fix bounded a stalled client V2 `async=false` insert and left four cases open: an insert that keeps streaming progress headers, a write-side stall (the server stops reading the request body), the client V1 `future.get()` inserts and the non-insert requests. Each of them is now either bounded, failing within the bound as a retriable error, or written down as a limit with a measured number that shows why it cannot be bounded. The help text then names exactly what the setting bounds.

**Depends on**: Phase 45. The tail rewrites the describe path in `ClickHouseHelperClient` and adds #857's wrapper to `ClickHouseWriter`, and the timeout work starts from that tree.

**Requirements**: TMO-01, TMO-02, TMO-03

**Success Criteria** (what must be TRUE):

  1. *[live, record]* An insert that streams progress headers is either bounded or recorded. Bounded means it ends after `clickhouseClientInsertTimeoutMs` with a retriable error, behind a pin seen red on the unchanged tree. Recorded means its escape is re-measured against a real ClickHouse `latest` server and written into FORK-DELTA as a limit with the measured number (TMO-02).
  2. *[Docker-free, record]* A write-side stall, the client V1 `future.get()` insert sites and the non-insert requests are each either bounded, with a pin that was run red on the unchanged tree and passes after the fix, or recorded in FORK-DELTA with a measurement that shows why they cannot be bounded (TMO-03).
  3. *[Docker-free]* Through `ProxySinkTask.put`, each case bounded in this phase surfaces as retriable under `errors.tolerance=none` and `errors.tolerance=all`: the task is not failed and no DLQ record is written. An operator's `socket_timeout` in `jdbcConnectionProperties` still wins. No config key is added, because a new key is a feature and out of scope, and `async_insert` is not touched (TMO-02, TMO-03).
  4. *[Docker-free]* The `clickhouseClientInsertTimeoutMs` help text is written after criteria 1–3 settle. It says what the setting bounds and what it does not, and a Docker-free test pins the text against the measured list. The `ClickHouseSinkConfig` diff is that text and nothing else (TMO-01).

**Plans**: TBD

**Research flag**: Settle these before planning:
- Which mechanism reaches each case on client 0.11.0. The gg7 decision used a per-insert `socket_timeout` and rejected a client-level timeout that bounds every request. A mechanism that bounds requests beyond the case being fixed stops for the user.
- How to make the server stream progress headers. Phase 41 E-06 recorded the escape as inferred with the outcome unmeasured, because a connector body insert streamed no progress header on 26.10.3.7. Find a server setting or query shape that streams them before choosing between bounding and recording.
- The three V1 `future.get()` sites in `ClickHouseWriter`, and what a bound does to a V1 insert that legitimately runs longer (the same rollout consequence as F-2).
- Which non-insert requests exist (DESCRIBE, ping, mutation requests, the Keeper state reads and writes), and which of them run on the put path.

**Note**: Never use `async=true` or `async_insert` as a fix: each timed-out batch would leak a thread and a connection. No Mockito; extend `StubClickHouseServer` with the responders the pins need. A bound added here fails an insert that legitimately runs longer than the bound, and the rollout item states it.

Phase gate: cache-busted `testUnit`, a full suite on `latest` × V2, and the anchors invariant.

### Phase 47: Task Stop & Retry Classification

**Goal**: A stopping task reaches every cleanup step even when one delivery strategy throws, and the published CHANGELOG stops claiming more than the code does. An operator knows what happens to a retriable `DataException`: either Connect stops redelivering it after a bounded budget, or the endless redelivery is pinned and documented as a limit. FUT-05 ends with a recorded decision instead of staying open.

**Depends on**: Phase 46. RTY-01's changed-row list is written against the final classification pins, including the timeout cases that Phase 46 bounds, and #857's wrapper from Phase 45 is part of the classification it measures.

**Requirements**: LIF-01, LIF-02, RTY-01, RTY-02

**Success Criteria** (what must be TRUE):

  1. *[Docker-free]* `ClickHouseSinkTask.stop()` stops every delivery strategy and still reaches the later cleanup when one strategy's `stop()` throws (`try`/`finally`). The pin uses no Mockito and runs with Docker unreachable. It was run red on the unchanged tree, and the result was recorded before the fix (LIF-01).
  2. *[record]* The CHANGELOG `stop()` Bug Fixes bullet and its Known Issues bullet describe the behaviour the code now has, and neither claims more than the LIF-01 pin proves (LIF-02).
  3. *[Docker-free, record]* Through `ProxySinkTask.put` under `errors.tolerance=none` and `errors.tolerance=all`, each retriable `DataException` shape takes one of two outcomes; at least the shapes that PIN-02 and RSK-02 pinned are covered. Either it stops after a bounded retry budget, with the expected changed rows of the classification matrix committed before the change and the actual delta equal to that list, or it keeps today's unbounded redelivery, pinned by a test and documented as a limit in FORK-DELTA (RTY-01).
  4. *[record, operator]* FUT-05 is closed. The start-up WARN for unvetted `additionalRetriableErrorCodes` stays, a validator lands only if the user approves it in their own words, and the decision is recorded with its reason. FORK-DELTA no longer lists FUT-05 as open (RTY-02).

**Plans**: TBD

**Research flag**: Settle these before planning:
- What a retry budget can be built on without a new config key: what Connect does with a `RetriableException` thrown from `put`, whether the connector's existing retry settings reach it, and whether a budget needs state that survives a rebalance. If every option needs a new key or new persisted state, record the limit instead.
- If the user approves a FUT-05 validator: where it lives (`ClickHouseSinkConfig` is a high-risk file), and what it does to a connector whose configuration lists a non-transient code today. A start-up failure would be an upgrade-breaking item for the rollout note.
- Where the CHANGELOG correction lives: the published `1.7.0_gg7` section, the new `1.7.0_gg8` section, or both.

**Note**: `ClickHouseSinkTask`, `ProxySinkTask` and `Utils` are on the hot path, and `Utils.handleException` decides classification. Any diff beyond LIF-01's `try`/`finally`, RTY-01's budget or pin and RTY-02's approved validator stops for a user decision. Each code change is its own commit and revert unit, behind its own gate. Preserve the `BEFORE`/`AFTER` flag sequence (AGENTS.md).

Phase gate: cache-busted `testUnit`, a full suite on `latest` × V2, and the anchors invariant.

### Phase 48: Phase 37 Dispositions & Document Fixes

**Goal**: The release phase starts with no carried review row left to the user by default. Each `src/main` advisory row from Phases 26–28, the null assertion, the 28 IN-05(b) items, the broad allow-list glob rows and the review Infos ends in one of two ways: fixed, tests first and one commit each, or closed with a written reason. The published documents stop pointing at planning identifiers and at paths that moved when gg7 was archived.

**Depends on**: Phase 47. Dispositions and documents are written against the final code, and 26 IN-02 and 27 IN-01 sit in `ClickHouseHelperClient` and `ClickHouseWriter`, which the tail changed in Phase 45.

**Requirements**: P37-01, P37-02, P37-03, P37-04, DOC-01, DOC-02

**Success Criteria** (what must be TRUE):

  1. *[Docker-free, record]* The four `src/main` advisory rows and the null assertion are each fixed in their own commit with a test seen red first, or closed with a written reason in a new disposition ledger. The rows are 26 IN-02 (`shouldSkipDescribeRow`), 27 WR-01 (`Column.convertEnumValues`), 27 IN-01 (`passesThroughUnchanged`) and 28 IN-01 (`ESCAPED_NEWLINE`). A fix that changes an existing assertion or an operator-visible text lands only after the user's approval in their own words (P37-01).
  2. *[record]* In that ledger, each 28 IN-05(b) item and each review Info has a fix with a commit pointer, or a named disposition with its reason. No row's disposition is "owner: user" alone (P37-02, P37-04).
  3. *[Docker-free]* Each broad allow-list glob row (UF-37-3) is narrowed, or justified in the ledger, and the allow-list dry run reads `0 unallowed, 0 stale rows` (P37-03).
  4. *[record]* The published `docs/ROLLOUT-v1.7.0_gg6.md` names no planning identifier: IN-03 is replaced by a public reference (DOC-01). Every evidence path that FORK-DELTA cites resolves on the phase tip, under `.planning/milestones/` where it moved. The test path carries its package, and item 20 names its ledger by a path that survives the next archive (DOC-02).

**Plans**: TBD

**Research flag**: Settle these before planning:
- What "the null assertion" of P37-01 and "the 28 IN-05(b) items" of P37-02 are, read from the Phase 28 review and `milestones/v1.7.0_gg7-followup-phases/43-document-fixes-phase-37-dispositions/43-DISPOSITION-LEDGER.md`. That ledger's 28-IN-05(b) row is the `DescribeUnreadableEnumMockServerTest` null assertion.
- Which review Infos P37-04 covers: the 37-REVIEW and 37-REVIEW-UF Infos carried in the Phase 43 ledger, and any Info that the 39–44 reviews left open.
- Which allow-lists UF-37-3 may change. The gg6 and gg7 lists belong to published tags and stay identical to their tag copies. A rule in `.github/scripts/compare_confluent_archives.py` changes the release path, and this is the last phase that may edit it.
- The cost of each row recorded at the Phase 43 deferral: removing 26 IN-02 retargets 17 test sites and an anchor row, and 27 WR-01's reviewed fix changes two existing assertions and an operator-visible text.

**Note**: This is the last phase that may edit `.github/`, tests, `build.gradle.kts` or `src/main`. The gg6 note edited here and the FORK-DELTA changes go into Phase 49's independent review. No existing assertion is weakened.

Phase gate: cache-busted `testUnit`, the anchors invariant, and a full suite on `latest` × V2 if `src/main` changed.

### Phase 49: Release & Publication

**Goal**: `v1.7.0_gg8` is published on `custom_build`. Operators get an independently reviewed rollout note that states what changes for them: the upstream tail, the `stop()` fix, the timeout limits, the retry outcome and the FUT-05 decision, each claim labelled by its evidence. The gg8 allow-list compare passes on the final tree, and the suite is green twice. The published SHA has one green `tests.yaml` run before the publish run creates the tag. The two client-0.11.0 scratch branches leave `origin`. Every push, the merge, the publish run and the branch deletion happen only on the user's direct word.

**Depends on**: Phases 45–48 (it documents and publishes their outcome)

**Requirements**: RLS-01, RLS-02, RLS-03, RLS-04, RLS-05, RLS-06

**Success Criteria** (what must be TRUE):

  1. *[Docker-free]* `VERSION` reads `v1.7.0_gg8`, set by `bump_version.py`, and the generated version agrees after `clean`. `CHANGELOG.md` has a `1.7.0_gg8` section, and `docs/FORK-DELTA.md` is final, with its raw numbers refreshed once. Every `fork-anchors.txt` row changes in the same commit as the count it changes (RLS-01).
  2. *[Docker-free]* `confluent-archive-allowlist-v1.7.0_gg8.txt` is generated last, from the final tree. Every wildcard row follows at least two literal path segments, and a dry run of the compare reads `0 unallowed, 0 stale rows` (RLS-02).
  3. *[record]* `docs/ROLLOUT-v1.7.0_gg8.md` labels every effect `measured`, `read from the code` or `derived`. Before the push gate, a fresh `gsd-code-reviewer` spawned by the orchestrator reviews it together with `CHANGELOG.md`, `docs/FORK-DELTA.md` and the gg6 note edited in Phase 48. Rounds repeat until one finds nothing (at most three unless the user authorises more), and every finding's disposition is recorded (RLS-03).
  4. *[live]* On the release tip, two full `--rerun` suites on one pinned `latest` build with client V2 are equal and green, with `SELECT version()` recorded. `testUnit` passes with Docker unreachable, and the anchors gate passes (RLS-04).
  5. *[operator, CI]* The user gives an explicit approval, to the orchestrator and in their own words. After it, `custom_build` is a non-force `--no-ff` merge whose tree equals the rehearsed tip. The published SHA has exactly one `tests.yaml` run, and it is green before the publish run creates the tag `v1.7.0_gg8` and the GitHub release (RLS-05). After a separate confirmation in the user's own words, `scratch/clt-07-client-0.11.0` and `scratch/clt-07-prebump-0.9.6` are deleted from `origin`, and `git ls-remote origin` lists neither (RLS-06).

**Plans**: TBD

**Note**: This phase edits no `src/`, test, `build.gradle.kts`, `gradle/` or `.github/` file (Phase 29 D-05). A defect found here goes back to its phase as a gap. Otherwise it is deferred with a reason, an owner and a re-open trigger in the milestone close record. The recipe is the gg7 one:
1. Check `origin/custom_build...<scratch branch>` for divergence before gating. A left count above 0 means `origin` moved (the user merges Dependabot PRs there directly).
2. Push the scratch branch once, at a SHA that no other pushed branch holds, and wait for green.
3. Merge with `--no-ff` from a throwaway worktree, so the merged tree equals the rehearsed tip.
4. Push `custom_build` only, and wait for its single run.
5. The publish run (`release.yaml`, `dry_run` not ticked) is launched on the user's word. Push `pre-v1.7.0_gg8` by name after it.
6. Delete the scratch branch. Delete `scratch/clt-07-client-0.11.0` and `scratch/clt-07-prebump-0.9.6` only after the separate RLS-06 confirmation, by name, in one push.

Before the gate, recheck upstream: tags past `v1.7.0`, and the open PRs #861, #855, #854 and #788. The CHANGELOG heading carries the merge date in UTC. The approval gates run orchestrator-inline, because executors refuse relayed approvals and `custom_build` is the protected base branch.

## User-Owned Gates

Each gate below is a blocking-human checkpoint. `mode: yolo`, `auto_advance: true` and `skip_discuss: true` move work between steps and phases, but they pass none of these gates. The user answers each one in their own words, directly to the orchestrator. A message relayed by an agent is not approval, and an executor that reaches one of these gates stops and returns it.

| Gate | Phase | Why it is the user's |
|------|-------|----------------------|
| A push of any branch | any (planned only in 49) | Every push runs `tests.yaml` once and publishes the local `.planning` tail one way |
| An upstream tag past `v1.7.0` at merge time | 45 | It makes this a tagged sync (FUT-01), and the user decides the scope |
| How #857's record context is kept, and any change to an assertion of an upstream test that arrives with the merge | 45 | UPS-02: the fork's value-free policy wins over upstream |
| Each dependency bump | 45 | DEP-01, AGENTS.md |
| A timeout mechanism that bounds requests beyond the case being fixed (for example a client-level `socket_timeout`) | 46 | The gg7 decision chose a per-insert bound |
| A change to `KeeperStateProvider`, the exactly-once state key or KeeperMap rows | 46, 47 | Exactly-once consistency; a state-key change would orphan stored rows |
| A `src/main` change outside a phase's declared scope | 45–48 | The fork runs in production |
| The FUT-05 validator | 47 | RTY-02 |
| A Phase 37 fix that changes an existing assertion or an operator-visible text | 48 | P37-01 |
| Changing or removing an existing assertion | any | Tests are only added, never weakened |
| A fourth review round on the release documents | 49 | RLS-03 |
| The `--no-ff` merge into `custom_build` and its push | 49 | RLS-05 |
| The publish run (`release.yaml`, `dry_run` not ticked) | 49 | RLS-05 |
| Deleting `scratch/clt-07-client-0.11.0` and `scratch/clt-07-prebump-0.9.6` from `origin` | 49 | RLS-06: a separate confirmation before that push |
| `NIGHTLY_ENABLED`, Dependabot settings | — | They stay with the user, and agents do not touch them |

## Structure Notes

**Why five phases under `coarse` granularity.** The 26 requirements fall into five delivery boundaries, and each is proven differently:
- the upstream tail and the dependency check, by the sync recipe (one mechanical merge, the invariant script, two full runs) and a recorded check with the user's decisions;
- the timeout limits, by stall pins seen red, or by numbers measured against a real server;
- the task boundary (`stop()` and retry classification), by Docker-free pins through `ClickHouseSinkTask` and `ProxySinkTask`, with a changed-row list written in advance;
- the dispositions and documents, by a ledger and the user's approvals;
- the release, by the publication recipe.

**Folded rather than standalone:**
- **DEP sits in the sync phase (45).** It is a check plus the bumps the user approves, too thin for a phase of its own. Run after the merge, it reads the catalog that ships: upstream's tail already brings slf4j-simple, spotless and commons-lang3, and adds `connect-json` to the test classpath. gg4's Phase 25 combined the same two kinds of work.
- **LIF shares a phase with RTY (47).** LIF-01 is one `try`/`finally` and LIF-02 one CHANGELOG pair. Both live at the task boundary in `ClickHouseSinkTask`, next to RTY-02's start-up WARN.
- **DOC shares a phase with the Phase 37 rows (48),** as in gg7's Phase 43. The new disposition ledger is what DOC-02's rule (a ledger path that survives archiving) applies to, and the gg6 note goes into the same Phase 49 review.

**Order:**
- The sync comes first, on a scratch branch behind `pre-v1.7.0_gg8`. The tail edits `ClickHouseWriter`, `ClickHouseHelperClient` and `ClickHouseFieldDescriptor`, which Phases 46 and 48 edit too, so every later red points at the fork's own change.
- The timeout limits (46) come before the retry classification (47), so RTY-01's changed-row list is measured against the final set of retriable outcomes. This is the gg7 order of F-2 before RSK-02.
- Within each code phase, the pins land red-first before the code they pin.
- The dispositions and documents (48) come after the code they describe. The release (49) is last and edits only documents.

**Execution model.** Phases run one after another on one working tree, the scratch branch created in Phase 45. Phases 45, 46 and 47 change `src/main`. Phase 48 changes it only for the rows fixed under P37-01, and Phase 49 does not change it.

**Files shared across phases:**

| File | Phases | Rule |
|------|--------|------|
| `ClickHouseWriter` | 45 (#857), 46 (V1 `future.get()` and insert sites), 48 (27 IN-01) | The tail lands first; the gg3 classification matrix passes unchanged after each phase |
| `ClickHouseHelperClient`, `ClickHouseFieldDescriptor` | 45 (#839, `fromJsonRow`), 46 (non-insert requests), 48 (26 IN-02) | NAME-01..NAME-07 and the describe pins stay green |
| `ClickHouseSinkConfig` | 46 (help text), 47 (FUT-05 validator, only if approved) | High-risk file: the diff is the text, or the approved validator, and nothing else |
| `ClickHouseSinkTask`, `ProxySinkTask`, `Utils` | 47 | One commit and one gate per change |
| `StubClickHouseServer` | 46 | The responders land with the pins that need them |
| `gradle/libs.versions.toml`, `build.gradle.kts` | 45 | Upstream's bumps come with the merge; each approved bump is its own commit |
| `.github/workflows/tests.yaml`, `.github/scripts/compare_confluent_archives.py` | 45 (the merge takes the fork side of `tests.yaml`), 48 (UF-37-3, Infos) | Each edit passes the pin guard; Phase 49 edits none |
| `.planning/scripts/fork-anchors.txt` | 45–49 | Anchor rows change in the same commit as the code |
| `docs/FORK-DELTA.md` | 45 (tail rows), 46 (limits), 47 (RTY-01, FUT-05), 48 (evidence paths, item 20), 49 (final) | Raw numbers are refreshed once, in Phase 49 |
| `CHANGELOG.md` | 45 (the merge takes the fork side), 47 (`stop()` bullets), 49 (`1.7.0_gg8` section) | Each bullet states only what a pin proves |
| `docs/ROLLOUT-v1.7.0_gg6.md` | 48 (IN-03) | Reviewed independently in Phase 49 |
| `VERSION`, `docs/ROLLOUT-v1.7.0_gg8.md`, the gg8 allow-list | 49 | The allow-list is generated last, from the final tree |

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

**Execution Order:** Phases execute in numeric order: 45 → 46 → 47 → 48 → 49.

| Phase | Milestone | Plans Complete | Status | Completed |
|-------|-----------|----------------|--------|-----------|
| 45. Upstream Tail Merge & Dependency Check | v1.7.0_gg8 | 0/TBD | Not started | - |
| 46. Insert Timeout Limits | v1.7.0_gg8 | 0/TBD | Not started | - |
| 47. Task Stop & Retry Classification | v1.7.0_gg8 | 0/TBD | Not started | - |
| 48. Phase 37 Dispositions & Document Fixes | v1.7.0_gg8 | 0/TBD | Not started | - |
| 49. Release & Publication | v1.7.0_gg8 | 0/TBD | Not started | - |

---
*Roadmap reorganized at the `v1.7.0_gg7-followup` close: 2026-11-04. Milestone `v1.7.0_gg8` phases 45-49 added: 2026-11-04.*
