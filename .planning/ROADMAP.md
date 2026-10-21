# Roadmap: ClickHouse Kafka Connect — `custom_build` Fork Test Coverage

## Milestones

- ✅ **v1.2.9_gg4-tests Fork Test Coverage** — Phases 1-5 (shipped 2026-09-18)
- ✅ **v1.6.0_gg1-sync Upstream v1.6.0 Sync** — Phases 6-11 (shipped 2026-10-07)
- ✅ **v1.7.0_gg1-sync Upstream v1.7.0 Sync & Follow-up Hardening** — Phases 12-15 (shipped 2026-10-10)
- ✅ **v1.7.0_gg2-hardening Follow-up Hardening & CI Hygiene** — Phases 16-20 (shipped 2026-10-14)
- ✅ **v1.7.0_gg3-hardening Record-Data Exposure & Review Debt** — Phases 21-24 (shipped 2026-10-20)
- 🚧 **v1.7.0_gg4-hardening Dependency Absorption, Discovery Defects & Review Debt** — Phases 25-29 (in progress)

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

### 🚧 v1.7.0_gg4-hardening Dependency Absorption, Discovery Defects & Review Debt (In Progress)

**Milestone Goal:** Absorb the nine Dependabot merges on `origin/custom_build` and upstream's untagged tail, fix discovery defects 999.4 and 999.5, clear every review advisory carried from `v1.7.0_gg3-hardening`, and publish `v1.7.0_gg4` with the suite green on ClickHouse `latest` × client V2 and CI green on the published SHA.

**Criteria tags:** *[live]* needs Docker and ClickHouse `latest` × client V2. *[Docker-free]* runs under `testUnit` or as a build or static check with Docker stopped. *[record]* is a committed evidence file or document whose content is checked. *[operator]* is a user action, confirmed in the user's own words. *[CI]* is read from GitHub Actions.

- [ ] **Phase 25: Dependency Absorption & Upstream Mini-Sync** - Fix the pin-guard reader first, then merge `custom_build`'s nine Dependabot commits and upstream's tail `a6f1df83`. Prove the Shadow 9 archive intact and `connect-api` 4.4.0 compile-time only, add a CI guard against nested jars, and close the pin-guard and slf4j advisories. The user creates the `dependencies` label
- [ ] **Phase 26: Size-Named Tuple Elements in Discovery** - Backlog 999.4: a declared Tuple element named `size`, `size0` or `size<digits>` survives discovery at both drop sites and round-trips live. The V1/V2 DESCRIBE loop becomes one tested builder
- [ ] **Phase 27: Writer Failure Path & Test Debt** - A Jackson failure on the JSON-as-string route no longer carries a Map key, and an unknown Enum label names its column. The remaining 22-REVIEW and 24-REVIEW advisories and the carried test-debt items close without weakening any test
- [ ] **Phase 28: Raw Identifier Names in Discovery** - Backlog 999.5: names are matched under the raw identifier from `CREATE TABLE`, with pairing re-keyed and `normalise` rewritten. Both name forms are pinned live, and the fork coverage list is completed once
- [ ] **Phase 29: Release & Publication** - `v1.7.0_gg4` is published with an independently reviewed rollout note, two green suite runs, an origin-freeze check and one green CI run on the published SHA before the tags

## Phase Details

### Phase 25: Dependency Absorption & Upstream Mini-Sync

**Goal**: Every later phase builds, tests and gates on the post-absorption tree. That tree holds `origin/custom_build`'s nine Dependabot merges and upstream's untagged tail. The shipped Confluent archive is proven intact under Shadow 9.6.1, and `connect-api` 4.4.0 is proven compile-time only against the production worker's Kafka 3.9 API. CI can no longer publish an archive of nested jars unnoticed. No connector behaviour changes.

**Depends on**: Nothing in this milestone. Phase 24 shipped `v1.7.0_gg3` (`3376a18f`, republished as `6a2a87a5`). Since then the user has merged nine Dependabot PRs onto `origin/custom_build` (tip `69cd680b`). `upstream/main` is `a6f1df83`, 12 commits past `v1.7.0`, with no new tag.

**Requirements**: DEBT-13, DEPS-02, DEPS-03, BLD-15, BLD-16, CI-20, DEBT-14, SYNC-11, DEBT-15, SYNC-12, OPS-04

**Success Criteria** (what must be TRUE):

  1. *[Docker-free]* `TestsWorkflowPinGuardUnitTest` still reads `build.gradle.kts` correctly when `/*` appears inside a string literal or inside a `//` comment. The commit that fixes this precedes the milestone's first authored `build.gradle.kts` edit (DEBT-13). The guard also:
     - accepts a `runs-on:` line with a trailing comment;
     - rejects commented-out or guarded fork-count text;
     - accepts valid alternative `dependabot.yml` layouts;
     - derives its fixture list from the production constant (DEBT-14).
     Each change was seen red on a fixture before it went green.
  2. *[live]* The working branch contains `origin/custom_build`'s nine Dependabot merges through one merge commit. The tip was re-read with `git ls-remote origin custom_build` immediately before the merge, and the same commit carries the `fork-anchors.txt` edits (`kafka = "4.4.0"` and the slf4j literal). On the merged tree:
     - the full suite (`latest` × V2) is green with the same 18 named skips;
     - spotless is baseline-equal;
     - a `--warning-mode all` run is recorded, with no build-script change.
     The unedited `verify-merge-invariants.sh` passes on the tip, with `PRE_TAG` at the pre-merge rollback tag. Every published Dependabot commit that violates a pre-merge anchor has a SHA-pinned row in `fork-anchor-exemptions.txt`, generated from the gate's own output.
  3. *[Docker-free]* The archive is measured, and CI guards it:
     - `createConfluentArchive`, built on the tip under Shadow 9.6.1, contains zero nested `*.jar` entries and no Connect API classes.
     - An entry-by-entry SHA-256 compare against the `v1.7.0_gg3` archive (7,376 entries, 24-COMMAND-EVIDENCE E-04) differs only by the allow-listed set: version bumps, guava and its five small dependencies.
     - `MANIFEST.MF` differs only by versions, and the `addMultiReleaseAttribute` decision is recorded.
     - Bytecode evidence against both jars shows that the compiled connector references no Connect API member that is absent from Kafka 3.9 (`cp-kafka-connect-base:7.9.7`), and none added after 4.1.1.
     - `tests.yaml` builds the archive and fails on any nested `*.jar` entry, and the pin guard pins that step (CI-20).
     - FORK-DELTA, the `ClickHouseSinkTask` comment and the three DLQ-emulation Javadocs say the worker supplies the Connect API at runtime.
  4. *[live]* Upstream's tail is merged as one mechanical merge commit behind the unedited `verify-merge-invariants.sh`. `UPSTREAM_REF` is the full SHA `a6f1df839c079180bc7c343b5823f205e204dafc`, the stale `refs/upstream/main` and `refs/upstream/v1.6.0` were deleted first, and a rollback tag sits at the pre-merge tip.
     - The conflicts are exactly `.gitignore` and `gradle/libs.versions.toml`. The shared keys keep the fork's values, `guava = "33.7.2-jre"` is added and `httpclientTest` is gone.
     - `.planning/MERGE-CHECKLIST-v1.7.0-tail.md` records the resolution table and the conflicted paths.
     - `dependencyInsight` shows guava 33.7.2 on the runtime classpath.
     - `slf4j-reload4j` resolves from the catalog, and its anchor row has moved with it (DEBT-15).
     - FORK-DELTA records the guava addition and the httpclient5 pin, with their advisories labelled by source.
     - Two full `--rerun` suite runs on the phase tip are green. That tip is the baseline for Phases 26–28.
  5. *[operator]* The user has created the `dependencies` label and said so directly; a relayed answer is not accepted. The record then states one of two things. Either a Dependabot PR opened since then arrived without the "labels could not be found" comment, or the check is pending the next monthly run (Phase 29 re-checks it).

**Plans:** 2/8 plans executed

Plans:
**Wave 1**
- [x] 25-01-PLAN.md — OPS-04 at the start: REST facts, the user creates the `dependencies` label (blocking-human), answer recorded verbatim; opens the evidence file

**Wave 2** *(blocked on Wave 1 completion)*
- [x] 25-02-PLAN.md — DEBT-13: string- and comment-aware `ktsCode` scanner, fixtures seen red first, before any authored `build.gradle.kts` edit

**Wave 3** *(blocked on Wave 2 completion)*
- [ ] 25-03-PLAN.md — Pre-flight and merge 1 (`origin/custom_build` 69cd680b) with the kafka anchor edit and gate-generated exemption rows; cumulative gate, self-test, merged-tree Docker-free checks (DEPS-02, DEPS-03)

**Wave 4** *(blocked on Wave 3 completion)*
- [ ] 25-04-PLAN.md — BLD-15: Shadow 9.6.1 repair of `confluentJar` with two anchors; committed archive compare against `v1.7.0_gg3`

**Wave 5** *(blocked on Wave 4 completion)*
- [ ] 25-05-PLAN.md — BLD-16: worker jars from `cp-kafka-connect-base:7.9.7`, javap bless script, committed reference list and `ConnectApiReferencesUnitTest`; D-14 wording commit

**Wave 6** *(blocked on Wave 5 completion)*
- [ ] 25-06-PLAN.md — CI-20: `verifyConfluentArchive` finalizer and the `tests.yaml` step, pinned red-first; DEBT-14 pin-guard advisories

**Wave 7** *(blocked on Wave 6 completion)*
- [ ] 25-07-PLAN.md — Step-4 suite gate (DEPS-02), then merge 2 (upstream tail a6f1df83) with its record commit, tail gate and self-test (SYNC-11)

**Wave 8** *(blocked on Wave 7 completion)*
- [ ] 25-08-PLAN.md — SYNC-12 archive/dependencyInsight/advisory rows, DEBT-15 catalog move, two `--rerun` runs, `baseline-v1.7.0_gg4`, OPS-04 closing observation

**Research flag**: Measurement precedes the plans for the Shadow repair:
- the archive entry diff (repair shape `configurations.add(clickhouseDependencies)` first, the `zipTree` provider as the fallback);
- whether guava is in the gg3 archive today, and at what version;
- the guava advisory identity, which has secondary sources only, so re-query it before anything cites it.

The user decisions this phase needed are already taken (2026-10-21):
- PR #10 (protobuf-java 4.36.2) and PR #4 (client `0.10.0`) stay open, unmerged and not ignored.
- `origin/custom_build` is frozen from the DEPS-02 merge until publication (OPS-05).

**Note**: Order inside the phase:
1. DEBT-13 first, with its own `testUnit` gate. The Shadow repair is the first authored `build.gradle.kts` edit, and the repair adds a `/*` string that the old reader mis-parses.
2. Pre-flight:
   - delete `refs/upstream/main` and `refs/upstream/v1.6.0`;
   - re-read `git ls-remote origin custom_build`;
   - record `SELECT version()` of `latest`;
   - tag the pre-merge tip locally.
3. Merge 1, `origin/custom_build`. `git merge-tree` shows it conflict-free. The anchor edits and the exemption rows go in the merge commit itself, and no `.planning/` commit is interleaved.
4. The Shadow repair, the archive compare, the `connect-api` bytecode evidence, the CI-20 step and its pin. Then DEBT-14 in the same pin-guard file, then one full suite run.
5. Merge 2, `a6f1df83`. `PRE_TAG` is a distinct local rollback tag (for example `pre-v1.7.0-tail`; `pre-v1.7.0_gg4` is reserved for the release), and the merge carries `CONFLICTED_PATHS_FILE` and the merge checklist. No dependency bump rides inside it.
6. DEBT-15 after merge 2, so it cannot widen the catalog conflict SYNC-11 measured. Then the two `--rerun` runs.

This phase pushes nothing. Shadow 9's `assemble` also builds a `*-all.jar` that includes connect-api; decide `addShadowJarToAssembleLifecycle` and record it. If origin moves before a gate despite the freeze, re-absorb with a fresh exemption set. OPS-04 is a user action in the GitHub UI (D-11): no agent creates the label. Present it at the start of the phase, so the observation window is as long as possible.

### Phase 26: Size-Named Tuple Elements in Discovery

**Goal**: A table whose Tuple declares an element named `size`, `size0` or `size<digits>` accepts inserts, and the typed read-back equals the records. Discovery keeps the declared element at both drop sites and still drops every structural DESCRIBE row. The pins drive the production DESCRIBE loop, which clients V1 and V2 now share. This closes backlog 999.4 and FORK-DELTA Known costs item 10.

**Depends on**: Phase 25 (the post-absorption baseline)

**Requirements**: DESC-06, DESC-07, DESC-08, DESC-09, DESC-10

**Success Criteria** (what must be TRUE):

  1. *[live, record]* Before any `src/main` change, a spike record taken on `latest` holds the DESCRIBE rows for:
     - the E-15 matrix (13 affected tables, 19 look-alikes, `s4_o01`);
     - the shapes `Array(Array(Tuple(size1, size0)))`, `Array(Tuple(size0 Tuple(a ...)))`, `Array(Tuple(size0 String, size String))`, `Array(Map(String, Tuple(size, size0)))` and `Tuple(keys, values)`.
     It confirms or denies the structural-then-declared double row and states the classifier rule.
  2. *[Docker-free, live]* The V1 and V2 describe paths share one fork-owned builder. The extraction is its own commit, with no behaviour change, and it is green on `testUnit` and the full suite before the fix lands. The 999.4 pin in `ClickHouseHelperClientForkUnitTest` drives that builder instead of a copy of the loop (23-REVIEW WR-02). One manual `CLIENT_VERSION=V1` run of the helper live tests is recorded.
  3. *[Docker-free]* A unit table of captured real DESCRIBE rows pins the rule at both drop sites, `shouldSkipDescribeRow` and `Table.updateParent`'s `SIZE_FIELD_MATCHER`:
     - a declared element named `size`, `size0` or `size<digits>` becomes a `Column`;
     - Array offsets, the Nullable null map, Map `keys`/`values`, nested `.size1` and the DESC-05 ancestor walk under JSON/Dynamic are still dropped;
     - a structural twin never enters the skip set, so the declared element's own children survive.
  4. *[live]* For every E-15 affected shape, records insert and their typed read-back equals the originals. The DESC-05 regression rows stay green. The D-04 pin is flipped: no unpaired-element WARN fires for these tables.
  5. *[record]* These land in the same commit as the fix:
     - the FORK-DELTA rows for `ClickHouseHelperClient` and `Table.java`;
     - the `fork-anchors.txt` rows;
     - a CHANGELOG line;
     - Known costs item 10, retired;
     - backlog 999.4, marked delivered.
     The anchors invariant passes at every commit of the phase.

**Plans**: TBD

**Research flag**: The live DESCRIBE spike (DESC-06) precedes the classifier's plan, and it must confirm the `Array(Tuple(size0 ...))` double-row collision. The scope is decided: the user chose on 2026-10-21 to fix both drop sites, including `size<digits>` under an Array parent in `Table.java`.

**Note**: Order inside the phase:
1. The spike.
2. The behaviour-preserving extraction, green first.
3. The classifier at both sites, with the cascade guard and every DESC-10 document edit, in one commit.
4. The live round-trip.
5. The phase gate.

The classifier's inputs:
- Classify on the parent's raw type, its array depth and the row's own `type`.
- Never classify on occurrence order or on a closed list of names.
- Never use "the parent is a Tuple" alone, which is wrong for `Array(Tuple)`.

`Table.java` is adopted wholesale from upstream. Narrow its change to "the child is not a declared element", and record the fork delta. The DESCRIBE Column list is the RowBinary wire format, so this fix changes the bytes of a retried batch for an affected table. Record the rollout facts for Phase 29 as they land:
- an upgrade needs no clean stop, because the failing inserts stored nothing;
- a rollback to gg3 needs a clean stop first;
- short writes that happened to parse are a data tail that nothing repairs.

The new pins use backslash-free names, so they survive Phase 28. The one public element-key helper this phase adds to `DeclaredNullability` is the single definition of a Tuple element's name, and Phase 28 reuses it.

Phase gate: cache-busted `testUnit`, a full suite on `latest` × V2, and the anchors invariant.

### Phase 27: Writer Failure Path & Test Debt

**Goal**: On the opt-in JSON-as-string route, a Jackson failure no longer carries a Map key into the task status, the DLQ stack-trace header or the `Error inserting records` throwable. A timeout beneath that failure stays retriable. An unknown Enum label names its column instead of surfacing as a `NullPointerException`. The remaining 22-REVIEW and 24-REVIEW advisories and the carried test-debt items are closed, and no existing assertion is deleted or weakened. This removes the one stated exception in FORK-DELTA Known costs item 6.

**Depends on**: Phase 25 (the post-absorption baseline). It is independent of Phase 26 and runs after it because phases run sequentially.

**Requirements**: WRT-16, WRT-17, WRT-18, WRT-19, DEBT-16

**Success Criteria** (what must be TRUE):

  1. *[Docker-free]* `WriterFailureClassificationUnitTest` gains rows for a `JsonProcessingException` alone, one over a `SocketTimeoutException`, one over a timeout-text `IOException`, and a plain `IOException`. The new rows are committed green on the unchanged writer. The existing 184 cases are byte-identical before and after the fix.
  2. *[live, Docker-free]* With a sentinel Map key on the JSON-as-string route, the key reaches none of the task status, the DLQ stack-trace header and the `Error inserting records` throwable. `JacksonException` is excluded link by link along the cause chain, so a socket timeout beneath it still passes through as retriable. `JsonAsStringKeyResidualLiveTest` is flipped deliberately, with the flip seen red first. The exception sentence in FORK-DELTA Known costs item 6 is gone (22-REVIEW WR-01).
  3. *[Docker-free]* An unknown Enum label fails with a message that names the column and a lookup-miss cause, not with a `NullPointerException`, and the `No content in enum` text names the column. Both stay value-free, and the `Failed to write column ` prefix is byte-identical (22-REVIEW IN-01, IN-02).
  4. *[Docker-free, live]* The advisories close on the record:
     - `ClickHouseWriter` derives failure metadata in one place;
     - `src/main` comments, `JsonAsStringKeyResidualLiveTest`'s Javadoc and its display names carry no planning IDs;
     - `schemaWithFixedStringMismatchTest` fails when `put` does not throw, seen red;
     - `CountingHelperClient.useClientV2Calls` is asserted or removed;
     - `NullableTupleMatrixLiveTest`'s premise loops fail with a message instead of passing vacuously or throwing a bare `NullPointerException`.
     A diff check shows that no existing assertion was deleted or weakened, and the anchors invariant passes at every commit.

**Plans**: TBD

**Research flag**: Standard patterns: the WRT-14 pin-first order and the Phase 22 L-47 precedent.

**Note**: `ClickHouseWriter.java` is a high-risk file (AGENTS.md). No edit reaches `Processing`, `KeeperStateProvider` or `ClickHouseSinkConfig`. Order inside the phase:
1. The WRT-16 rows, green on the unchanged writer.
2. WRT-17, with the Known costs edit in the same commit, then the deliberate live-test flip.
3. WRT-18, then WRT-19.
4. DEBT-16.

Rules that hold throughout:
- Keep `Failed to write column ` and `have no matching DESCRIBE sub-column` byte-identical. Any intended text change is a ledger row and a rollout line, recorded for Phase 29.
- A new writer message must not embed a record value.
- Flipping any existing assertion needs the user's sign-off and seen-red evidence (Phase 22 L-47, Phase 23 D-19).

Two items here are inputs to Phase 28:
- DEBT-16's premise-loop fix in `NullableTupleMatrixLiveTest` lands before Phase 28 uses that file's COLS-13 twins as its oracle. After a name decode, a premise lookup by the old name must fail loudly, not skip silently.
- WRT-18's lookup-miss pins are in place before Phase 28 measures enum labels (NAME-06) in the same `Column` code.

Phase gate: cache-busted `testUnit`, a full suite on `latest` × V2, and the anchors invariant.

### Phase 28: Raw Identifier Names in Discovery

**Goal**: A column or Tuple element whose name contains a backslash, a literal backslash-n, a line feed, a tab, a carriage return, a slash, a double quote, a backtick or a non-ASCII character is matched under its raw identifier, as written in `CREATE TABLE`. A Kafka record using that name lands, and the typed read-back equals it. Declared nullability still pairs. The doubled form's behaviour is pinned as the rollout note will state it, and there is no dual-form alias. This closes backlog 999.5 and FORK-DELTA Known costs item 11, and the fork coverage report lists every fork class.

**Depends on**: Phase 26 (the extracted builder and the element-key helper this phase rewrites against) and Phase 27 (the `NullableTupleMatrixLiveTest` premise fix and the enum lookup-miss pins this phase relies on)

**Requirements**: NAME-01, NAME-02, NAME-03, NAME-04, NAME-05, NAME-06, NAME-07, DEBT-17

**Success Criteria** (what must be TRUE):

  1. *[Docker-free]* Before any decode change, the escaped-quote branch of `DeclaredNullability.normalise` is pinned on the current code. The pin was seen red under a mutation that always closes the quote (23-REVIEW WR-01).
  2. *[live, record]* A name-matrix spike record taken on `latest` lists the current form and the decoded form of every spiked character, for root columns, Tuple elements and enum labels.
  3. *[Docker-free, live]* The decode is names-only and pairing follows it:
     - `ClickHouseFieldDescriptor.fromJsonRow` yields the raw identifier for column and element names, and the type text is unchanged.
     - The V1 describe path reads raw lines in UTF-8, and `TableTest.extractCommentV1` stays green.
     - `DeclaredNullability` pairs on the raw-name key, and the decode and the pairing key land in the same commit.
     - The COLS-13 live twins in `NullableTupleMatrixLiveTest` stay green.
     - 23-REVIEW IN-01 and IN-02 are closed.
  4. *[live]* For every spiked character, a Kafka record that uses the raw name lands, with typed read-back equal to the record. A record that still uses the doubled form is pinned as it behaves today: a root column is ignored and its `DEFAULT` is stored, while a Tuple element fails loudly. Enum labels that contain an escaped character either round-trip under the raw form with a live pin, or their remaining behaviour is written into FORK-DELTA Known costs and recorded for the rollout note.
  5. *[record, Docker-free]* These land in the same commit as the fix:
     - the FORK-DELTA row;
     - a CHANGELOG line;
     - Known costs item 11, retired;
     - backlog 999.5, marked delivered.
     `jacocoForkReport` lists `DeclaredNullability`, `WithheldMessageException` and every fork class Phases 25–28 added. The list is edited once, its FORK-DELTA row is in the same commit, and the generated fork report shows those classes. The anchors invariant passes at every commit.

**Plans**: TBD

**Research flag**: The live name-matrix spike (NAME-02) precedes the plans for the decode and the `normalise` rewrite, because the coupling to pairing and to enum labels is inferred, not measured. The canonical form is decided: on 2026-10-21 the user chose the raw identifier, with no dual-form alias, and operators rename. NAME-06's outcome, fix or document, follows the measurement. If a fix would reach beyond `Column`'s enum-label parsing, it stops for a user decision.

**Note**: Order inside the phase:
1. The NAME-01 pin.
2. The NAME-02 spike.
3. NAME-03 and NAME-04 in one commit. Never change `fromJsonRow` without the pairing key.
4. The NAME-05 live pins.
5. NAME-06.
6. NAME-07's document edits, in the fix commit.
7. DEBT-17, last.

Constraints:
- `normalise` is rewritten, not extended. After the decode, the type text holds one escape layer fewer, and a naive decode turns the COLS-13 twins red for line feed, tab and carriage return.
- Decode names only. Decoding the type text is out of scope, because it changes every multi-line Tuple type.
- The fix changes the bytes of a retried batch. Record for Phase 29 the clean stop in both directions, with the rename as its precondition.
- No metadata-only WARN for a doubled-form name (NAME-F1, deferred).
- DEBT-17 is the milestone's last `build.gradle.kts` edit. Record one manual `CLIENT_VERSION=V1` run for the V1 raw-line read.

Phase gate: cache-busted `testUnit`, a full suite on `latest` × V2, and the anchors invariant.

### Phase 29: Release & Publication

**Goal**: `v1.7.0_gg4` is published on `custom_build`. Operators have an independently reviewed rollout note for every behaviour change, with each claim backed by a measurement. The suite is green twice on `latest` × V2, and the published SHA has exactly one green CI run before the tags are pushed.

**Depends on**: Phases 25, 26, 27 and 28 (it documents and publishes their outcome)

**Requirements**: PUB-11, ROLL-04, ROLL-05, OPS-06, PUB-12, OPS-05, PUB-13

**Success Criteria** (what must be TRUE):

  1. *[Docker-free]* After `clean`, `VERSION` reads `v1.7.0_gg4` and the generated version agrees. The anchor row for `VERSION` changes in the same commit. `CHANGELOG.md` has a complete `1.7.0_gg4` section. FORK-DELTA's rows and raw numbers are refreshed once, in this phase.
  2. *[record, live]* `docs/ROLLOUT-v1.7.0_gg4.md` covers:
     - 999.4 and 999.5, each with its clean-stop rule;
     - the removed Map-key exception;
     - `connect-api` 4.4.0;
     - Shadow 9 and Gradle 9.8.1;
     - the guava and httpclient5 advisories, labelled by source;
     - the MBean `version=` segment.
     It names `cp-kafka-connect-base:7.9.7` (Kafka 3.9) as the production worker and states the oldest worker the connector is verified against. It also says that KIP-1161's LIST-config strictness applies only from Kafka 4.2 workers. Every quantifier in it matches a recorded measurement, and every command in it was run live. `docs/ROLLOUT-v1.7.0_gg1.md` and `docs/ROLLOUT-v1.7.0_gg2.md` carry the corrected `enableDbTopicSplit=true` key rule and state-table query from the gg3 note.
  3. *[record]* Before the publication gate, an orchestrator-spawned, fresh-context `gsd-code-reviewer` has reviewed every operator document. Rounds repeated until one found nothing, at most three. Every finding's disposition is recorded, and no finding ID is reused.
  4. *[live, CI]* On the release tip:
     - two full `--rerun` suite runs are green on `latest` × V2, and the anchors gate passes;
     - a re-fetch shows `origin/custom_build` unchanged since the DEPS-02 merge, or it was re-absorbed with a fresh exemption set before gating.
     After the user's own direct approval:
     - `custom_build` is a non-force `--no-ff` merge whose tree equals the rehearsed tip;
     - the published SHA has exactly one `tests.yaml` run, and it is green;
     - the tags `v1.7.0_gg4` and `pre-v1.7.0_gg4` reach `origin` only after that run;
     - the scratch branch is deleted from `origin`.

**Plans**: TBD

**Research flag**: Standard; this phase follows Phase 24. Before the note states them, re-check two inputs: the Kafka 4.4.0 upgrade notes (not yet published at research time) and the guava advisory identity.

**Note**: This phase makes no `src/main` edit and no edit of an existing test. It follows the Phase 24 recipe (rehearse, measure, approve, publish):
1. Push `sync/v1.7.0` once as a scratch branch, never at a SHA another pushed branch already holds, and wait for green.
2. Merge with `--no-ff` from a throwaway worktree, so the merged tree equals the rehearsed tip.
3. Push `custom_build` only, and wait for its single run.
4. Push the tags by name, only after that run is green.
5. Delete the scratch branch.

Decide two things once, at plan time:
- The anchors-gate form: the cumulative `PRE_TAG=pre-v1.7.0-sync` with the exemption rows, or `pre-v1.7.0-tail`.
- How the gg1 and gg2 note corrections reach `origin`: with the release merge, or a docs-only `--no-ff` merge before it. Either way, the reviewer prompt names those two notes.

Write and review the rollout note before the gate, inside the merged tree. Label each claim "measured", "read from connector code" or "inferred, to be pinned live", and budget two to three review rounds for its operator command text. Reviewer-proposed fix text is itself reviewed.

The approval gates run orchestrator-inline, because executors refuse relayed approvals, and `custom_build` is GSD's protected base branch. The pre-gate bundle re-checks OPS-04 (the `dependencies` label comment) and OPS-05 (the origin freeze). Record `SELECT version()` at each gate.

## Structure Notes

**Why five phases under `coarse` granularity.** Absorption must land first and the release must come last, both hard constraints. The research proposed six phases. This roadmap folds the sixth, "test and build hygiene", into its neighbours, because `24-MILESTONE-CLOSE-RECORD.md` gives each of its items a re-open trigger that fires in an earlier phase:
- DEBT-14 (21-REVIEW WR-01, WR-03, IN-01, IN-02). Its trigger is the next edit of the pin-guard test, and DEBT-13 and CI-20 make that edit in Phase 25.
- DEBT-15 (21-REVIEW IN-04). Its trigger is the next bump of the slf4j literal; DEPS-02 brings that bump and moves the literal's anchor row in Phase 25.
- DEBT-16 (22-REVIEW IN-04, the IN-05 test half, 23-REVIEW IN-03). Two of its three items are writer tests, and Phase 27's `ClickHouseWriter` edit fires IN-05's trigger. The `NullableTupleMatrixLiveTest` premise fix must precede Phase 28, which uses that test as its oracle.
- DEBT-17. It must be in the last code phase, which is now Phase 28.

The four code phases split along the production files that change: the build and the catalog (25), the describe loop and `Table` (26), `ClickHouseWriter` and the `Column` enum path (27), and `ClickHouseFieldDescriptor` with `DeclaredNullability` (28). Folding 26 and 28 together would put two discovery changes, each of which alters retried-batch bytes and needs its own live spike, behind one gate. Folding 27 into either would put a high-risk writer change behind a discovery gate. The two merges share Phase 25 because both are absorption, and SYNC-12's two runs are the single baseline that Phases 26–28 start from.

**Placement choices worth recording:**

- **DEBT-13 is Phase 25's first plan, not hygiene.** The Shadow repair is the first authored `build.gradle.kts` edit, and it adds text the old reader mis-parses.
- **The writer phase sits between the two discovery phases, the research order kept.** It is independent of both, and it lands the premise-loop fix and the enum pins that Phase 28 builds on. The 999.5 canonical form was decided on 2026-10-21, so Phase 28 no longer needs the decision time the research reserved for it.
- **OPS-04 sits in Phase 25.** The earliest creation gives the longest window for a Dependabot PR to show the comment gone, and Phase 29 re-checks it. This is the Phase 21 OPS-01 precedent.
- **OPS-05 and OPS-06 sit in Phase 29.** The freeze spans from the DEPS-02 merge to publication and is verified at the release gate. The worker facts are rollout-note content.

**Execution model.** Phases run sequentially on one working tree. The cost to minimise is re-touching the same lines and repeating gates, not merge conflicts.

**Carried advisories, one owner each:**

| Carried item | Requirement | Phase |
|--------------|-------------|-------|
| 21-REVIEW WR-02 | DEBT-13 | 25 |
| 21-REVIEW WR-01, WR-03, IN-01, IN-02 | DEBT-14 | 25 |
| 21-REVIEW IN-04 | DEBT-15 | 25 |
| 23-REVIEW WR-02 | DESC-07 | 26 |
| 22-REVIEW WR-01 | WRT-17 | 27 |
| 22-REVIEW IN-01, IN-02 | WRT-18 | 27 |
| 22-REVIEW IN-03, IN-05 (`src/main` half); 24-REVIEW IN-03 | WRT-19 | 27 |
| 22-REVIEW IN-04, IN-05 (test half); 23-REVIEW IN-03 | DEBT-16 | 27 |
| 23-REVIEW WR-01 | NAME-01 | 28 |
| 23-REVIEW IN-01, IN-02 | NAME-04 | 28 |
| `jacocoForkReport` class list | DEBT-17 | 28 |

**Files shared across phases:**

| File | Phases | Rule |
|------|--------|------|
| `docs/FORK-DELTA.md` | 25 (Connect API sentence, kafka, guava, httpclient5 and slf4j rows), 26 (`ClickHouseHelperClient` and `Table.java` rows, Known costs item 10), 27 (Known costs item 6), 28 (999.5 row, Known costs item 11, jacoco row), 29 (raw numbers, completeness) | Each phase edits only its own rows; raw numbers are refreshed in Phase 29 only |
| `.planning/scripts/fork-anchors.txt` | 25 (kafka row and the slf4j literal row, moved again by DEBT-15), 26–28 (any new anchor), 29 (`VERSION` row) | Anchor rows change in the same commit as the code |
| `.planning/scripts/fork-anchor-exemptions.txt` | 25 only (29 only if origin moves despite the freeze) | Rows generated from the gate's own output; `verify-merge-invariants.sh` is never edited (D-02) |
| `build.gradle.kts` | 25 (Shadow repair, DEBT-15), 28 (jacoco list, DEBT-17) | DEBT-13 lands before the first authored edit; the jacoco list is edited once |
| `gradle/libs.versions.toml`, `.github/workflows/tests.yaml`, `TestsWorkflowPinGuardUnitTest` | 25 only | — |
| `ClickHouseHelperClient.java`, the new describe builder, `Table.java` | 26 (extraction, both drop sites), 28 (V1 raw-line UTF-8 read only) | Phase 28 builds on Phase 26's builder and does not re-extract it |
| `DeclaredNullability.java` | 26 (one public element-key helper), 28 (`normalise` rewrite, raw-name pairing, IN-01, IN-02) | The NAME-01 pin lands on the code as Phase 26 leaves it |
| `ClickHouseWriter.java`, `JsonAsStringKeyResidualLiveTest` | 27 only | — |
| `Column.java` | 27 (`convertEnumValues`, WRT-18), 28 (enum-label keys, if NAME-06 fixes them) | WRT-18's pins are the oracle for any NAME-06 change |
| `NullableTupleMatrixLiveTest` | 27 (premise loops, DEBT-16) | Phase 28 relies on its COLS-13 twins unchanged |
| `CHANGELOG.md` | 26 and 28 (one line each, in the fix commit), 29 (completes the `1.7.0_gg4` section) | Released sections are never rewritten without a recorded reason |
| `.planning/ROADMAP.md` backlog entries | 26 (999.4), 28 (999.5) | Marked delivered in the same commit as the fix |
| `VERSION`, `docs/ROLLOUT-v1.7.0_gg4.md`, `docs/ROLLOUT-v1.7.0_gg1.md`, `docs/ROLLOUT-v1.7.0_gg2.md` | 29 only | — |

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

### Phase 999.5: Backslashes in column and element names come out of fromJsonRow doubled (BACKLOG — scheduled into Phase 28)

**Goal:** `ClickHouseFieldDescriptor.fromJsonRow` doubles every backslash before Jackson parses, so a server name with a backslash comes out doubled (`c.a\\nb`), and a Kafka field must use the doubled form for the writer to match it (23-EVIDENCE.md R-02). The D-12 live premise uses the doubled field name. The behaviour is pre-existing, and Phase 23 changes no code or test for it. Fix sketch: decide the intended name form, decode name escapes in upstream-region `fromJsonRow`, add a live round-trip pin for both forms and a rollout note, since operators' field names may depend on the doubled form. Found 2026-10-20 in Phase 23 research (D-20)
**Scheduled:** 2026-10-21 into milestone `v1.7.0_gg4-hardening` as Phase 28 (NAME-01..NAME-07). The canonical form was decided by the user on 2026-10-21: the raw identifier as written in `CREATE TABLE`, with no dual-form alias, and operators rename. The escaped-quote `normalise` pin (NAME-01) lands on the current code first. NAME-07 marks this entry delivered in the same commit as the fix.
**Requirements:** mapped under Phase 28
**Plans:** planned under Phase 28

## Progress

**Execution Order:** 25 → 26 → 27 → 28 → 29, sequential on one working tree.

| Phase | Milestone | Plans Complete | Status | Completed |
|-------|-----------|----------------|--------|-----------|
| 25. Dependency Absorption & Upstream Mini-Sync | v1.7.0_gg4-hardening | 2/8 | In Progress | - |
| 26. Size-Named Tuple Elements in Discovery | v1.7.0_gg4-hardening | 0/TBD | Not started | - |
| 27. Writer Failure Path & Test Debt | v1.7.0_gg4-hardening | 0/TBD | Not started | - |
| 28. Raw Identifier Names in Discovery | v1.7.0_gg4-hardening | 0/TBD | Not started | - |
| 29. Release & Publication | v1.7.0_gg4-hardening | 0/TBD | Not started | - |

---
*Roadmap reorganized at the `v1.7.0_gg3-hardening` close: 2026-10-21. Roadmap for milestone `v1.7.0_gg4-hardening` created: 2026-10-21.*
