# Roadmap: ClickHouse Kafka Connect — `custom_build` Fork Test Coverage

## Milestones

- ✅ **v1.2.9_gg4-tests Fork Test Coverage** — Phases 1-5 (shipped 2026-09-18)
- ✅ **v1.6.0_gg1-sync Upstream v1.6.0 Sync** — Phases 6-11 (shipped 2026-10-07)
- ✅ **v1.7.0_gg1-sync Upstream v1.7.0 Sync & Follow-up Hardening** — Phases 12-15 (shipped 2026-10-10)
- 🚧 **v1.7.0_gg2-hardening Follow-up Hardening & CI Hygiene** — Phases 16-20 (in progress)

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

### 🚧 v1.7.0_gg2-hardening Follow-up Hardening & CI Hygiene (In Progress)

**Milestone Goal:** Close all 11 follow-ups left open by `v1.7.0_gg1-sync`, getting the corrected rollout doc onto `origin` first, and publish `v1.7.0_gg2` with the suite green on ClickHouse `latest` × client V2 and CI green on the published SHA.

- [x] **Phase 16: Early Docs Publication** - Fix the 15-REVIEW docs findings and publish the corrected rollout doc to `custom_build` as a docs-only `--no-ff` merge, before any code work (completed 2026-10-11)
- [x] **Phase 17: CI, Build & Test Hygiene** - SHA-pinned, least-privilege Node 24 workflow on `ubuntu-24.04` with a Gradle-reported fork count; Lombok warning, duplicate test-classpath lz4, LZ4 response leg and the NanoTimestamp edge row closed (completed 2026-10-12)
- [x] **Phase 18: Writer Correctness** - Empty-CDC skip on every route, a column-naming `DataException`, a null-message guard on the error path, and DateTime parse logs without record data (completed 2026-10-12)
- [ ] **Phase 19: Nullable Enum & Tuple Mapping** - Backlog 999.1: `Nullable(Enum8|Enum16)` round-trips; `Nullable(Tuple)` decided by a spike and a go/no-go that cannot hold the release
- [ ] **Phase 20: Release & Publication** - `v1.7.0_gg2` published with a reviewed rollout note, two green suite runs and one green CI run on the published SHA before the tag

## Phase Details

### Phase 16: Early Docs Publication

**Goal**: An operator reading `origin/custom_build` gets a rollout doc that can no longer erase other connectors' exactly-once state, with the 15-REVIEW docs findings fixed. This happens before any code work starts and ships no code.

**Depends on**: Nothing in this milestone (Phase 15 shipped `v1.7.0_gg1` as `f328fd21`)

**Requirements**: PUB-05, PUB-06

**Success Criteria** (what must be TRUE):

  1. On `sync/v1.7.0`, `docs/ROLLOUT-v1.7.0_gg1.md`, `docs/FORK-DELTA.md` and `CHANGELOG.md` read correctly to an operator, and every command quoted in the changed text was run with output matching the text:
     - rollout item 2's recovery steps say which step applies only with `exactlyOnce=true`
     - item 8 says that going forward again after `git revert -m 1` means reverting the revert
     - every FORK-DELTA grep command prints a total, not per-file counts
     - operator text uses plain language instead of bare planning IDs, and names the MBean `version=`/client-name change and the lz4 advisory reason
  2. A code review of the changed docs is recorded, with every finding dispositioned, before the merge approval is asked for.
  3. No text in the merged tree tells an operator to `TRUNCATE` the shared KeeperMap state table.
  4. After the user's own explicit approval, given directly and not relayed through an agent, `origin/custom_build` is a non-force two-parent `--no-ff` merge of `sync/v1.7.0`:
     - its diff against `f328fd21` touches only `.planning/`, `docs/` and `CHANGELOG.md`
     - its tree equals the merged `sync/v1.7.0` tip
     - it carries neither `.planning/.gsd-ws-arg` nor uncommitted `state.json` edits
     - only `custom_build` was pushed, and no tag
  5. That merge SHA has exactly one `tests.yaml` run, located by `head_sha` plus workflow path. The run is green and has no cancelled sibling.

**Plans**: 3/3 plans complete

Plans:
**Wave 1**
- [x] 16-01-PLAN.md — Fix IN-07..IN-10 in the rollout doc, FORK-DELTA and CHANGELOG (D-01..D-06), with every quoted command recorded in 16-COMMAND-EVIDENCE.md; tracer dry-runs the publication gate

**Wave 2** *(blocked on Wave 1 completion)*
- [x] 16-02-PLAN.md — Pre-gate review of the Phase 16 docs diff with every finding dispositioned, IN-07..IN-10 closed in 15-REVIEW-DISPOSITION (D-09, D-10), and the measured approval bundle on X0

**Wave 3** *(blocked on Wave 2 completion)*
- [x] 16-03-PLAN.md — Blocking-human gate, then the docs-only `--no-ff` merge in a throwaway worktree, push of `custom_build` alone (no tag), and its single green `tests.yaml` run (D-11, D-12)

**Note**: `verify-merge-invariants.sh` is the upstream-sync gate. It requires the second parent to be an upstream release tag, so it does not apply here and must not be "fixed". The gate for this merge is the path allow-list diff plus a tree-equality check. No full suite is needed, because the tree outside `docs/`, `CHANGELOG.md` and `.planning/` is identical to the gated `f328fd21` tree; assert that by command.

The approval gate runs orchestrator-inline, because executors refuse relayed approvals. Merge from a throwaway worktree so the main tree stays on `sync/v1.7.0`.

Push before **2026-10-19**. From that date `ubuntu-latest` is Ubuntu 26, and this run must use the unchanged workflow on the runner it was last proven on.

Whether IN-10 corrects the published `1.7.0_gg1` CHANGELOG section in place or adds a correction note is a plan-time decision. PITFALLS 13 prefers correcting forward.

### Phase 17: CI, Build & Test Hygiene

**Goal**: CI runs on a SHA-pinned, least-privilege, Node 24 workflow on `ubuntu-24.04` that reports Gradle's own fork count, and the build/test debt from Phase 14 is closed: the Lombok warning, the duplicate lz4 on the test classpath, the unasserted LZ4 response leg and the NanoTimestamp edge row. A green run on a pushed SHA proves it, ahead of the `ubuntu-latest` switch on 2026-10-19.

**Depends on**: Phase 16 (its docs-only CI run must use the unchanged workflow, so a red result there cannot be blamed on the CI changes)

**Requirements**: CI-11, CI-12, CI-13, CI-14, CI-15, CI-16, CI-17, BLD-14, DEBT-06, DEBT-07, DEBT-08

**Success Criteria** (what must be TRUE):

  1. `tests.yaml` runs on `ubuntu-24.04`, declares `permissions: contents: read`, and checks out with `persist-credentials: false`. Every action is pinned to a full commit SHA with a `# vX.Y.Z` comment: Node 24 majors (v7.x) of `actions/checkout` and `actions/upload-artifact`, and the peeled commit for annotated tags such as `gradle/actions`. `.github/dependabot.yml` includes the `github-actions` ecosystem. `test-head.yaml` no longer calls `tests.yaml` as a reusable workflow; it is either fixed or disabled.
  2. The "Test fork count" step reports the `maxParallelForks` value read from a Gradle task. No copy of the formula is left in the workflow's shell.
  3. After the user's explicit approval, one SHA is pushed on one branch, with no tag. It has exactly one green `tests.yaml` run, and that run shows:
     - the `ubuntu-24.04` image
     - only `Contents: read`/`Metadata: read` token permissions
     - the job summary and the fork-count notice
     - both artifact uploads
  4. The build prints no Lombok "Ambiguous: Jackson2 and Jackson3 exist" warning, and a `javap` diff shows `ClickHouseFieldDescriptor`'s generated code unchanged. `testRuntimeClasspath` resolves only `at.yawk.lz4:lz4-java`, pinned by a Docker-free test. `runtimeClasspath` and `integrationTestRuntimeClasspath` resolve exactly as before.
  5. `Lz4RoundTripLiveTest` asserts LZ4 on the response leg with a standing assertion, and that assertion has been seen red with server-response compression disabled. `MetadataFreeJsonWriterTest` pins Debezium `NanoTimestamp` in mantissa-with-fraction scientific notation.

**Plans**: 4/4 plans complete

Plans:
**Wave 1**
- [x] 17-01-PLAN.md — Tracer: `reportTestForks` read by the fork-count step (CI-14); SHA pins, `ubuntu-24.04`, read-only token and the github-actions Dependabot entry behind a red-first Docker-free guard (CI-11..CI-13, CI-15); `test-head.yaml` deleted with FORK-DELTA rows and Recorded Fact 41 (CI-16, D-01, D-02)

**Wave 2** *(blocked on Wave 1 completion)*
- [x] 17-02-PLAN.md — Test-scoped lz4 exclude with a single-`LZ4Factory` pin (DEBT-07); response-leg LZ4 assertion seen red (DEBT-06); Lombok Jackson 2 line with a `javap` no-change proof (BLD-14); NanoTimestamp scientific-form rows (DEBT-08)

**Wave 3** *(blocked on Wave 2 completion)*
- [x] 17-03-PLAN.md — D-10 local gate and measured bundle, blocking-human push decision (D-11, surfacing F1: default branch is `main`), then one branch-only push with its single green `tests.yaml` run read through the API and the scratch branch deleted (CI-17)

**Wave 4** *(blocked on Wave 3 completion)*
- [x] 17-04-PLAN.md — User-pasted "Set up job" and job summary as E-11 (CI-17, D-13); STATE and ROADMAP record the outcome and carry the Dependabot and default-branch checks to Phase 20 (D-05)

**Note**: This phase changes no `src/main` code, so it cannot regress connector behaviour. Files shared within the phase:
- `tests.yaml` (CI-11..CI-14). The `timeout-minutes: 45` anchor in `fork-anchors.txt` must survive.
- `build.gradle.kts`: CI-14's fork-count task and DEBT-07's test-scoped exclusion or substitution. Never repeat the literal `withCapability("org.lz4:lz4-java")`, which is anchored at count 1. Never use `configurations.all { exclude }`.
- `docs/FORK-DELTA.md`: the CI row, T-14-16 and 999.2 in Known costs.

Pinning is scoped to `tests.yaml`. Upgrading the other upstream workflows is a Future Requirement. Re-resolve every action SHA at execution time, and check `object.type` so an annotated tag is dereferenced.

CI-17's push follows the D-13 precedent:
- the branch only, with an explicit refspec
- never at the SHA `custom_build` already sits on
- the scratch branch is deleted from `origin` once the run is green

`test-head.yaml` is deleted (D-01) and CI-16 is proven statically. The fork's default branch is `main`, not `custom_build` (research F1), so no schedule on `custom_build` ever ran it and publishing the deletion changes no observable schedule.

**Research flag**: DEBT-06 needs a short spike. It must confirm that `ProfileEvents['NetworkSendBytes']` against `result_bytes` can be read through client-v2 for the read's own query id. The fallback is the raw-HTTP LZ4 method-byte check from ARCHITECTURE.md. If neither works, narrow the test's claim; never leave "both legs" unbacked.

### Phase 18: Writer Correctness

**Goal**: Four `ClickHouseWriter` defects are fixed without disturbing exactly-once: empty Debezium records no longer write phantom rows on any route, a missing non-nullable value names its column, a message-less exception no longer turns into a `NullPointerException`, and DateTime parse failures stop logging record data.

**Depends on**: Phase 16 (code work starts only after the docs are published). Phase 18 shares no file with Phase 17; it runs after it only because phases run sequentially and Phase 17 is time-boxed.

**Requirements**: JSONP-09, WRT-10, WRT-11, WRT-12

**Success Criteria** (what must be TRUE):

  1. An empty Debezium record (`op=t`, or null `before` and `after`) inserts no row on any of three routes: explicit `insertFormat=json`, upstream `bypassRowBinary=true` JSON, and RowBinary. Each route has one live test. Offsets and exactly-once state still advance, and a batch made only of empty records succeeds. The skip is decided inside the writer, so a retried batch is byte-identical.
  2. A record that omits a non-nullable column with no default, including a field inside `Array(Tuple(...))`, fails with a `DataException`. The exception names the column and its ClickHouse type and carries no record value, so Connect status and the DLQ show which column failed.
  3. An exception with a null message that reaches `ClickHouseWriter`'s error path surfaces as itself, not as a `NullPointerException`. Retriable vs non-retriable classification is unchanged. A Docker-free test pins both.
  4. An unparseable `DateTime` or `DateTime64` string logs the column name, the ClickHouse and Kafka types, the exception class and the value length. A log-capture test proves the raw value appears neither directly nor through the parse exception's message. `docs/FORK-DELTA.md` has a row for this patch to upstream-identical lines.

**Plans**: 4/4 plans complete

Plans:
**Wave 1**
- [x] 18-01-PLAN.md — Tracer: null-safe failure-message lookup in `doInsertRawBinary`, pinned Docker-free and through `put()` (WRT-11, D-06/D-07); `Utils.handleException` null-message IOException guard (D-05); the column-naming `DataException` for a missing non-nullable value, flat and `Array(Tuple)`, ERROR line dropped (WRT-10, D-10/D-11). Three commits, each with its anchors, FORK-DELTA and ledger rows

**Wave 2** *(blocked on Wave 1 completion)*
- [x] 18-02-PLAN.md — Tracer: `LogCapture` helper and masked DateTime/DateTime64 parse logs plus the column-naming conversion exception (WRT-12, D-08/D-12); throwable-free generic write-failure log with the rethrow residual recorded in FORK-DELTA and backlog 999.3 (D-09)

**Wave 3** *(blocked on Wave 2 completion)*
- [x] 18-03-PLAN.md — Tracer: one shared empty-CDC skip in both serialization loops, proven live on RowBinary (JSONP-09); bypass, explicit and plain-table routes live on V1/V2; `ExplicitJsonInsertPathLiveTest` moved onto shared CDC fixtures

**Wave 4** *(blocked on Wave 3 completion)*
- [x] 18-04-PLAN.md — Docker-free byte-identity, edge, log-line and exactly-once state pins for the empty-CDC skip, mutation-checked (JSONP-09, D-02/D-03/D-04); phase gate (cache-busted testUnit and full suite on `latest` x V2 run by the gate command itself, `verify-merge-invariants.sh 54e63648 HEAD`, phase scope)

**Note**: All four items edit `ClickHouseWriter.java`, a high-risk file (AGENTS.md), in disjoint regions. Land them as separate commits.

WRT-10 and WRT-11 are coupled and belong in one plan. The message-less `RuntimeException` is exactly the null-message case that reaches the catch block unwrapped on client V1.

For JSONP-09:
- Use one shared helper; never add a `shouldSkipTableMetadata()` call, because `fork-anchors.txt` pins that string at 8 occurrences.
- Never filter records in `ProxySinkTask` or `Processing`, because that would change offset ranges and the dedup token.

Each skip and message change is an operator-visible behaviour change. Record it as it lands, for Phase 20's rollout note.

Phase gate: a cache-busted full suite on `latest` × V2, plus the anchors invariant (`verify-merge-invariants.sh 54e63648 HEAD`).

**Research flag**: confirm with a short spike that an insert whose rows are all skipped (empty body) is accepted by both clients.

### Phase 19: Nullable Enum & Tuple Mapping

**Goal**: `Nullable(Enum8)`/`Nullable(Enum16)` columns store exactly what the record says. `Nullable(Tuple)` either round-trips or stays explicitly warned, with a recorded Known cost. A spike decides which, so a no-go cannot hold the release.

**Depends on**: Phase 18 (it shares `ClickHouseWriter.java`, and the flipped pins rely on Phase 18's column-naming `DataException`)

**Requirements**: COLS-08, COLS-09, COLS-10, COLS-11, DEBT-09

**Success Criteria** (what must be TRUE):

  1. A live twin-table spike on ClickHouse `latest` runs before any Tuple production code lands. It covers `Nullable(Tuple(...))` with nullable and non-nullable elements, and `Array(Nullable(Tuple(...)))`. It records a go/no-go decision together with the type-string parsing approach.
  2. `Nullable(Enum8)` and `Nullable(Enum16)` values, NULL and non-NULL, round-trip through RowBinary, checked by typed read-back. This holds at top level and nested in `Array(Nullable(Enum))` and `Map(K, Nullable(Enum))`. The D-16/L-13 pins that asserted the old behaviour are flipped with a written rationale, and none is deleted.
  3. On go, `Nullable(Tuple(...))` round-trips with element nullability taken from the declared type string, not from DESCRIBE's all-`Nullable` sub-columns, and `Table.java` is untouched. `Array(Nullable(Tuple(...)))` still round-trips. On no-go, the discovery WARN names Tuple only, `docs/FORK-DELTA.md` records a Known cost, and the 999.1 backlog entry is updated.
  4. The discovery WARN, the `fork-anchors.txt` row for `hasNullableEnumOrTuple` and `docs/FORK-DELTA.md` each change in the same commit as the code they describe. The merge-invariant script passes at every commit in the phase.
  5. Each of 15-REVIEW IN-01..IN-05 is fixed, or closed with a written reason in the disposition file, consistently with the Tuple outcome.

**Plans**: 7/10 plans executed (19-04 and 19-05 are alternatives; the 19-03 gate retires one; 19-07 to 19-10 close the verification gaps)

Plans:
**Wave 1**
- [x] 19-01-PLAN.md — Tracer: committed live twin-table Nullable(Tuple) matrix (C1-C3 deciding, X1-X3 informational), both sides per cell, control and parsing-approach evidence; writes the go/no-go bundle 19-SPIKE.md (COLS-09, D-04..D-08)

**Wave 2** *(blocked on Wave 1 completion)*
- [x] 19-02-PLAN.md — Tracer: Enum branch carries Nullable (COLS-08), discovery WARN narrowed to Tuple in the same commit with flipped pins, anchors, FORK-DELTA and the new behaviour ledger (D-09, D-11, COLS-11); full Enum8/Enum16 matrix on both twins, Docker-free bytes, mutation-checked

**Wave 3** *(blocked on Wave 2 completion)*
- [x] 19-03-PLAN.md — Gate: bundle refreshed on that day's latest, blocking-human go/no-go answered directly by the user, decision recorded and the alternative plan retired (D-01..D-03)

**Wave 4** *(blocked on Wave 3; runs only on go)*
- [x] 19-04-PLAN.md — Tracer: Tuple element nullability from the declared type (DeclaredNullability after Table, Table.java untouched), WARN machinery deleted with its anchors and FORK-DELTA rows, D-16 and WARN pins flipped (COLS-10, D-10, D-11); edge pins, mutation; tests on client V2 only, V1 coverage structural (both describe loops return through one shared helper)

**Wave 5** *(blocked on Wave 4 being resolved; runs only on no-go)*
- [x] 19-05-PLAN.md — Tracer: route-qualified, record-keyed, text-pinned Tuple WARN (IN-01, IN-03, IN-04), Tuple Known cost in FORK-DELTA, 999.1 entry updated (COLS-10, D-12)

**Wave 6** *(blocked on Wave 5 completion)*
- [x] 19-06-PLAN.md — DEBT-09: IN-01..IN-05 dispositioned per the outcome (D-12, D-13); phase gate (cache-busted testUnit plus two full runs on latest x V2, anchors invariant, scope)

**Wave 7** *(gap closure; blocked on 19-06 completion)*
- [x] 19-07-PLAN.md — Gap closure, tracer: CR-01 (SC3 / COLS-10) SimpleAggregateFunction unwrap in DeclaredNullability.collect with seen-red Docker-free CR-01 and root-wrapper pins and a live twin round trip; anchors row, FORK-DELTA clause and ledger L-41 in one commit; matrix held at 30

**Wave 8** *(gap closure; blocked on 19-07 completion)*
- [ ] 19-08-PLAN.md — Gap closure: WR-01 (total pass) and IN-01 (table context in the fallback WARN), WR-03 (control asserts the read-past-end code) and IN-03 (production normalisation, one shared flattener)

**Wave 9** *(gap closure; blocked on 19-08 completion)*
- [ ] 19-09-PLAN.md — Gap closure: parser premise, then LowCardinality (Tuple element, Array element, Map value) and SimpleAggregateFunction boundary pins; seen red by a never-committed mutation; adjacent-suite regression with the matrix held at 30; anchors gate

**Wave 10** *(gap closure; blocked on 19-09 completion)*
- [ ] 19-10-PLAN.md — Gap closure: phase gate first (cache-busted testUnit plus two full runs, anchors invariant, scope, ledger L-31..L-41), then all eight 19-REVIEW findings dispositioned (open 0) and WR-02 (L-38), the VERSION and CHANGELOG obligation and IN-04 carried into the Phase 20 entry, committed last

**Note**: Order inside the phase:
1. Spike (COLS-09).
2. Enum fix (COLS-08). It ships whatever the spike decides.
3. Go/no-go.
4. Tuple fix or Known cost (COLS-10).
5. IN-01..IN-05 disposition (DEBT-09) last, because every one of those findings is about the WARN block that this phase narrows or deletes.

Never edit upstream-identical tests (`ColumnTest`, `TableTest`). Add fork-only classes beside them, following the `ColumnArrayRecursionTest` precedent.

The Enum fix changes stored values. It also lets a record that omits a `Nullable(Enum)` field pass validation and write NULL. Record both for Phase 20's rollout note.

**Research flag** (HIGH):
- Wire shape: `Nullable(Tuple)` is a flag followed by raw elements, with element markers only where the element is declared `Nullable`.
- Setting `isNullable` on Tuple from DESCRIBE silently corrupts single-field tuples that work today.
- Run every matrix cell against a table with a `DEFAULT` column (`RowBinaryWithDefaults`) and one without.
- Get a seen-red mutation.
- Parse the type string once per `Column`, never per record.
- Do not set `enable_nullable_tuple_type` in test DDL.
- Record `SELECT version()` at each gate.

### Phase 20: Release & Publication

**Goal**: `v1.7.0_gg2` is published on `custom_build`. Operators have a reviewed rollout note for every behaviour change, the suite is green twice on `latest` × V2, and the published SHA has one green CI run before the tag is pushed.

**Depends on**: Phases 17, 18 and 19 (it documents and publishes their outcome, including the Tuple go/no-go)

**Requirements**: ROLL-02, PUB-07, SUITE-05, PUB-08

**Success Criteria** (what must be TRUE):

  1. `docs/ROLLOUT-v1.7.0_gg2.md` gives operators each of the following with a self-check, and its review is recorded before the publication gate:
     - the 999.1 value change, stating that rows already stored wrongly are not repaired
     - the dedup-token retry consequence, as verified on `latest`
     - the empty-CDC skip change
     - the MBean `version=` change
  2. After `clean`:
     - `VERSION` reads `v1.7.0_gg2` and the generated version agrees
     - `CHANGELOG.md` has a plain-language `1.7.0_gg2` section
     - `docs/FORK-DELTA.md` has a row for every fork-side patch added in Phases 17-19
  3. The full suite passes twice on ClickHouse `latest` × client V2, the second run with `--rerun`. Both runs have 0 failures, 0 errors and every skip named. `testUnit` passes with the Docker daemon stopped.
  4. After the user's own explicit approval, `custom_build` on `origin` is a non-force `--no-ff` merge, and its SHA has exactly one green `tests.yaml` run. The `v1.7.0_gg2` tag reaches `origin` only after that run is green. The rollback tag points at a commit that already carries the branch-only trigger.

**Plans**: TBD

**Note**: This phase follows the Phase 11/15 recipe: rehearse, measure, approve, publish. The steps:
1. Push `sync/v1.7.0` once as a scratch branch (explicit refspec, never at a SHA another pushed branch already holds) and wait for green.
2. Merge with `--no-ff` from a throwaway worktree, so that the merged tree equals the rehearsed tip.
3. Push `custom_build` only.
4. Wait for its single run.
5. Push the tag by name.
6. Delete the scratch branch.

The approval gate runs orchestrator-inline. The upstream-sync merge-parent checks of `verify-merge-invariants.sh` do not apply to this merge either. Use Phase 16's milestone-specific checks: two parents, the first parent equal to `origin/custom_build`, and the merged tree equal to the rehearsed tip. Run the anchors invariant on the branch tip.

The rollback point is the Phase 16 merge commit, which already carries the branch-only trigger. The `VERSION` anchor row moves in the bump commit. Docs must be inside the merged tree, written and reviewed before the gate, so nothing is stranded locally the way `505e4981` was. Refresh FORK-DELTA's raw numbers here only.

Manual checks for the user, by hand, after publication (D-05, F1). No agent changes repository settings.

- Whether Dependabot version updates are enabled in the fork's settings.
- Which branch is the GitHub default: `main` at Phase 17; the user's intent is undecided (E-10). Dependabot (CI-15) and scheduled workflows read only the default branch.
- There is no `test-head.yaml` nightly run to check, since CI-16 was proven statically in Phase 17.

## Structure Notes

**Why five phases under `coarse` granularity.** Phases 16 and 20 are fixed by user decision: docs-only publication first, release last. That leaves 22 requirements, which split along the one boundary that matters, `ClickHouseWriter.java` and the Tuple go/no-go. Folding Phase 19 into Phase 18 would put a spike with an open outcome in front of four independent fixes. Phase 19 also relies on Phase 18's column-naming `DataException`. Splitting further would create thin internal-quality phases, so Phase 17 stays whole.

**Placement choices worth recording:**

- **CI, build and test debt share one phase (17).** None of it touches `src/main`. CI-14 and DEBT-07 both edit `build.gradle.kts`, and DEBT-06/DEBT-07 are both lz4 test-infra items. The 2026-10-19 runner switch time-boxes the phase. DEBT-08 is a single test row in a file no other phase edits, so it rides here rather than forming its own phase. The research files disagreed on placement: PITFALLS put CI last before release, while ARCHITECTURE and SUMMARY put it early. The user's ordering and the runner deadline settle it as early.
- **CI-17 is proven in Phase 17, not by the release run.** The release merge's run should not be the first run of the new workflow. CI-16 is proven statically in Phase 17; the fork's default branch is `main`, so no schedule observes it (F1).
- **JSONP-09 covers all three routes**, including RowBinary, per the user's scope decision. The skip stays inside the writer.
- **DEBT-09 sits in Phase 19, not with the other debt.** IN-01..IN-05 are all about the WARN block that 999.1 narrows or deletes, and their disposition depends on the COLS-10 outcome.
- **IN-07..IN-10 (PUB-05) ride the early publication** by user decision. They touch the same files, and the docs get their review before the gate.

**Execution model.** Phases run sequentially on one working tree (dispatch isolation `none`). The cost to minimise is re-touching the same lines and repeating gates, not merge conflicts.

**Files shared across phases:**

| File | Phases | Rule |
|------|--------|------|
| `docs/FORK-DELTA.md` | 16 (IN-09 grep totals), 17 (CI row, 999.2, T-14-16), 18 (T-13-19, empty-CDC rows), 19 (WARN/`Column` rows, Known cost), 20 (raw numbers, completeness) | Each phase edits only its own rows; raw numbers are refreshed in Phase 20 only |
| `CHANGELOG.md` | 16 (IN-10 wording), 20 (`1.7.0_gg2` section) | Never rewrite released content without a recorded reason |
| `src/main/.../sink/db/ClickHouseWriter.java` | 18 (four regions), 19 (TUPLE case, on go) | 18 before 19 |
| `.planning/scripts/fork-anchors.txt` | 18 (keep `shouldSkipTableMetadata()` at 8), 19 (`hasNullableEnumOrTuple` row), 20 (`VERSION` row) | Anchor rows change in the same commit as the code |
| `build.gradle.kts`, `.github/workflows/*`, `lombok.config` | 17 only | — |
| `Column.java`, `ClickHouseHelperClient.java`, `NestedNull*Test`, `ClickHouseHelperClientFork*Test` | 19 only | — |
| `docs/ROLLOUT-v1.7.0_gg1.md` | 16 only | The gg2 note is a new file in Phase 20 |

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

### Phase 999.2: Test classpath carries org.lz4:lz4-java 1.8.0 beside at.yawk.lz4 (BACKLOG — scheduled into Phase 17)

**Goal:** `testRuntimeClasspath` selects `org.lz4:lz4-java:1.8.0` (via the Confluent 7.9.1 test stack) beside `at.yawk.lz4:lz4-java`, although upstream's capability rule redirects it on `runtimeClasspath` and `integrationTestRuntimeClasspath`; it is test-only and not shipped; it is affected by the 2026-10-07 lz4 advisories, with no fix on that coordinate; fixing it needs a `build.gradle.kts` change outside Phase 14's scope; found 2026-10-09, Phase 14 research
**Scheduled:** 2026-10-10 into milestone `v1.7.0_gg2-hardening` as Phase 17 (DEBT-07)
**Requirements:** mapped under Phase 17
**Plans:** planned under Phase 17

### Phase 999.3: Rethrown write-failure messages can carry record values (BACKLOG)

**Goal:** `ClickHouseWriter.doWriteColValue`'s generic catch rethrows the original exception (kept by Phase 18 D-09 so classification cannot change). For some failures its message embeds the record value (`UUID.fromString`: `Invalid UUID string: <value>`; number parsing: `For input string: ...`), which reaches the Connect task status, the DLQ headers and `doInsertRawBinary`'s two `Error inserting records` ERROR logs. Fixing it means changing the rethrown type or message without changing retriable classification. Found 2026-10-12 in the Phase 18 discussion (D-09)
**Requirements:** TBD
**Plans:** 0 plans

Plans:
- [ ] TBD (promote with /gsd-review-backlog when ready)

## Progress

**Execution Order:** 16 → 17 → 18 → 19 → 20, sequential on one working tree.

| Phase | Milestone | Plans Complete | Status | Completed |
|-------|-----------|----------------|--------|-----------|
| 16. Early Docs Publication | v1.7.0_gg2-hardening | 3/3 | Complete    | 2026-10-11 |
| 17. CI, Build & Test Hygiene | v1.7.0_gg2-hardening | 4/4 | Complete    | 2026-10-12 |
| 18. Writer Correctness | v1.7.0_gg2-hardening | 4/4 | Complete    | 2026-10-12 |
| 19. Nullable Enum & Tuple Mapping | v1.7.0_gg2-hardening | 7/10 | In Progress | - |
| 20. Release & Publication | v1.7.0_gg2-hardening | 0/TBD | Not started | - |

---
*Roadmap for milestone `v1.7.0_gg2-hardening` created: 2026-10-10*
