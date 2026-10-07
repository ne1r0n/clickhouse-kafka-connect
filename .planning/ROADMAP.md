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

**Wave 2** *(blocked on Wave 1 completion)*

- [x] 06-02-PLAN.md — Commit 2: catalog overrides, `VERSION` = `v1.6.0_gg1`, `.gitignore` union, the fork's `tests.yaml`, the fork build additions re-applied onto upstream's file, and the `help` + `dependencies` gate (wave 2)

**Wave 3** *(blocked on Wave 2 completion)*

- [x] 06-03-PLAN.md — `.planning/MERGE-CHECKLIST.md` with the generated per-path Section 1, Section 2, and `## Recorded Facts`, plus the full-range invariant re-run (wave 3)

**Note on the conflict count:** re-measured at planning time, `git merge-tree --write-tree custom_build v1.6.0` produces 40 conflict messages across **40** unique paths — one message per path. The "39 unique paths" figure above and in `REQUIREMENTS.md` § SYNC-03 is a summary miscount; no path was lost and no path carries two messages. Section 1 of the checklist is generated from the command rather than transcribed, so the correct count is self-evident.

### Phase 7: Type Mapping, Writer, Helper Client & Config

**Goal**: The five fork behaviours with no upstream anchor — `Type.DYNAMIC`, the RowBinary fail-fast, `targetTableFilter`, the describe sub-column skip, and UTF-8 describe readers — survive upstream's rewrite of `Column`/`Table`/`ClickHouseWriter`/`ClickHouseHelperClient`, and `src/main` compiles again.

**Depends on**: Phase 6

**Requirements**: MAP-01, MAP-02, MAP-03, WRT-01, WRT-02, WRT-03, WRT-04, WRT-05, HLP-01, HLP-02, HLP-03, HLP-04, CFG-01, CFG-02, CFG-03, FREE-02, FREE-03

**Success Criteria** (what must be TRUE):

  1. `./gradlew compileJava` succeeds over the whole of `src/main`: `Type.DYNAMIC` exists alongside upstream's native `Type.JSON`, upstream's rewritten `Column` is adopted with the Dynamic branch ported in, and upstream's immutable `Table` is used as-is with every fork call site rewritten to the new constructor rather than the constructor being re-opened.
  2. `./gradlew testUnit` (Docker-free) is green over the mapping, writer-gate and config-parsing tests: a `Dynamic` type string resolves to `Type.DYNAMIC` and never silently to `UNKNOWN`, the RowBinary gate throws the fork's actionable `DataException` for `DYNAMIC`/`UNKNOWN` while `JSON` follows upstream's flag-gated write-as-string with `TYPE-06`'s JSON assertions rewritten to upstream's exception type and message, and `targetTableFilter` / `jsonInsertColumns` still parse to their pinned contracts.
  3. A connector started from a default config sends no `async_insert` setting, and all thirteen new upstream config options are present and reach their consumers at upstream defaults.
  4. Table discovery is still filtered server-side by `match(table, '<regex>')` re-expressed over upstream's `List<Table.TableDesc>` signatures; `shouldSkipDescribeRow` still skips `.null`, `.size0`, bare `.size` and JSON/Dynamic children over upstream's local-`List<Column>` loop with `numColumns` incremented before any skip; and every `InputStreamReader(` in the resolved `ClickHouseHelperClient` specifies `StandardCharsets.UTF_8` — grep-verified across the whole file, not just the two call sites the fork originally patched. (Live `FILT-01..04` / `DESC-01..04` / `TYPE-07` confirmation lands in Phase 11's full suite; the Docker-free half of each passes here.)
  5. Two free wins are recorded either way rather than assumed: upstream's `Map(K, Nullable(V))` null-marker fix is confirmed present with the fork's duplicate patch dropped and WR-01's status stated, and whether upstream issue #580 subsumes the dormant P3-WR-02 follow-up is checked and written down.

**Plans**: 4/4 plans executed

**Wave 1**

- [x] 07-01-PLAN.md — Unblock `compileJava`, `Type.DYNAMIC` + `Column` dispatch, and the RowBinary fail-fast for `DYNAMIC`/`UNKNOWN` (wave 1)

**Wave 2** *(blocked on Wave 1 completion)*

- [x] 07-02-PLAN.md — The `targetTableFilter` chain (config → writer → helper client → `showTables*`), the DESCRIBE sub-column skip over a loop-local column map, and UTF-8 describe readers (wave 2)

**Wave 3** *(blocked on Wave 2 completion)*

- [x] 07-03-PLAN.md — `jsonInsertColumns` + the explicit-JSON flag, `async_insert` un-forced, and the Phase 10 carve-out that makes `compileTestJava` green (wave 3)

**Wave 4** *(blocked on Wave 3 completion)*

- [x] 07-04-PLAN.md — The thirteen-defaults pin, the Docker-free `testUnit` acceptance gate, twelve checklist row flips and the FREE-02/FREE-03 recorded facts (wave 4)

**Note on the wave shape:** all four waves are sequential, and that is structural rather than conservative. `compileTestJava` is all-or-nothing — Gradle compiles the whole test source set before running any test — so no fork test can execute until every missing API (`Type.DYNAMIC`, `shouldSkipDescribeRow`, `buildTableFilterClause`, both config keys) is present. The three production plans therefore gate on `compileJava` plus source assertions, and every behavioural criterion in the phase resolves in plan 07-04's single `testUnit` run. The three production files also overlap across behaviours (`ClickHouseWriter.java` in plans 01 and 02, `ClickHouseSinkConfig.java` in plans 02 and 03), which independently forbids same-wave parallelism.

### Phase 8: Orchestration & JMX Re-plumbing

**Goal**: Restore the JMX `connector=<name>` segment in its new home — upstream deleted the fork's carrier method and moved naming into `util.jmx.SinkTaskStatistics` with no `connector=` segment, so this is a deliberate design re-plumb, and the milestone's "prefer upstream" policy explicitly does **not** apply here because upstream's version fails JMX-01/02.

**Depends on**: Phase 7 (`ProxySinkTask` constructs both the writer and the helper client, so its JMX work cannot finish until they compile)

**Requirements**: ORCH-01, ORCH-02, ORCH-03, ORCH-04, FREE-01

**Success Criteria** (what must be TRUE):

  1. `MBeanNameUnitTest`'s literal `connector=` substring assertion is green against the new home: the sink-task ObjectName renders `connector=<name>` sourced from `ClickHouseSinkTask.start()`, with colon sanitization retained — and `JMX-01..04` pass re-targeted at `SinkTaskStatistics` rather than the deleted `ProxySinkTask.buildMBeanName`.
  2. Upstream's new per-topic MBean carries the same `connector=<name>` segment, so per-topic metrics are attributable to a connector in a multi-connector worker.
  3. `./gradlew testUnit` is green with the new layering in place — `DeliveryStrategy`/`ChunkFlusher` above `ProxySinkTask`, upstream's 5-arg `Processing` construction below, and no stale 4-arg `Processing` call site anywhere in the tree.
  4. `Utils.java` and `Processing.java` — which merge with no conflict marker despite both sides touching them — have been read and explicitly reviewed, `UtilsTest`/`ProcessingTest` take upstream's versions, and the fork's "error classification is dead code on client V2" follow-up is closed in writing citing upstream issue #818 as the mechanism.

**Plans**: 3/3 plans executed

**Wave 1**

- [x] 08-01-PLAN.md — The JMX re-plumb end-to-end (`SinkTaskStatistics` carrier + `ProxySinkTask` + `ClickHouseSinkTask` + the four call sites + the canary's build un-exclude), the per-topic composition cases, and the UTF-8 fixture fix (wave 1)

**Wave 2** *(blocked on Wave 1 completion)*

- [x] 08-02-PLAN.md — The acceptance gate: `testUnit` green, then a full live suite with D-12's re-baselined pass condition and a named failure inventory, plus the `TableTest.extractNullables()` triage (wave 2)

**Wave 3** *(blocked on Wave 2 completion)*

- [x] 08-03-PLAN.md — ORCH-01/ORCH-04/FREE-01 discharged by evidence, then eight checklist row flips, two annotate-don't-flip notes, and the three Recorded Facts (wave 3)

**Note on the wave shape:** all three waves are sequential and that is structural, not conservative. The JMX re-plumb is one connected edit whose every link has exactly one call site, so it does not decompose into parallel work; D-12's live gate can only measure a tree that already has the re-plumb and the charset fix in it; and the checklist rows are flipped only once the live run has proved the behaviours, so that a `done` row never outruns its evidence. The UTF-8 fixture fix rides in wave 1 rather than a parallel plan because D-12 made a full live suite the gate, and a gate running against four known reds is a gate that can hide a fifth.

### Phase 9: Metadata-free JSON Path — Keep or Drop

**Goal**: Settle the one genuinely open design question in this milestone — whether the fork's metadata-free `insertFormat=json` bypass (and with it R-01's silent data loss) is kept or retired in favour of upstream's `writeRecordsJson` — at a blocking-human checkpoint on evidence read from the merged code, then make the code and the tests say the same thing.

**Depends on**: Phase 7 (the bypass is written against `Table` and the writer's insert methods); Phase 8 for a compiling orchestration layer

**Requirements**: JSONP-01, JSONP-02, JSONP-03, JSONP-04

**Success Criteria** (what must be TRUE):

  1. A recorded keep-or-drop decision with its rationale exists, made at a blocking-human checkpoint on evidence from the merged code — and made *before* the dependent implementation work, not discovered afterwards in a test result nobody looked at.
  2. The JSON-path tests reflect that decision: if kept, the metadata-free bypass works and R-01's labelled loss test still passes; if dropped, R-01's test failing is recorded as the wanted signal that the loss is gone.
  3. `jsonInsertColumns` works again, redesigned as a field filter inside upstream's `writeRecordsJson` — the `client.insert(table, columns, data, ...)` overload it attached to no longer exists at that call site — with the Docker-free `JsonInsertColumnsUnitTest` green here and `COLS-01..06`'s live half re-run in Phase 11.
  4. Reading the resolved diff (not merely a green test) shows no fork-only JSON-path code left standing against a `Table` shape upstream no longer produces.

**Plans**: 3/3 plans executed

Plans:
**Wave 1**

- [x] 09-01-PLAN.md — Re-apply the four bypass guards behind one predicate, rebuild the synthetic `Table` on upstream's 5-arg constructor, re-attach `jsonInsertColumns` server-side, and un-park the Docker-free canary (wave 1)

**Wave 2** *(blocked on Wave 1 completion)*

- [x] 09-02-PLAN.md — Adopt the `TableTest` carry-in, un-park the three live JSON-path classes, and hold the full suite to a zero-failure binary gate (wave 2)

**Wave 3** *(blocked on Wave 2 completion)*

- [x] 09-03-PLAN.md — Discharge JSONP-04 by reading the resolved diff, append the JSONP-03 rationale correction, and record the KEEP disposition with its operator evidence and accepted cost (wave 3)

### Phase 10: Test Harness Reconciliation

**Goal**: The previous milestone's harness contract — `@Tag("clickhouse")`, the MBean leak sweep, `uniqueConnectorName`, one shared container per JVM fork — survives upstream's rewritten test infrastructure and the Testcontainers `2.0.5` major bump, so the 371 fork tests have somewhere to run.

**Depends on**: Phase 7, Phase 8, Phase 9 (test helpers call production APIs whose shapes those phases fix)

**Requirements**: HARN-01, HARN-02, HARN-03, HARN-04, HARN-05, HARN-06, HARN-07

**Success Criteria** (what must be TRUE):

  1. `./gradlew testUnit` completes with Docker stopped — the loudest, fastest proof that `@Tag("clickhouse")` is back on `ClickHouseBase` and that the Docker-free gate still means what it says.
  2. Testcontainers `2.0.5` is verified to start a ClickHouse container through `ClickHouseBase` **before** any of the 371 fork tests are adapted to the new harness.
  3. Exactly one `ClickHouseTestHelpers.java` exists in the tree — under `src/testFixtures`, carrying the fork's helpers, every fork import updated — and `SchemaTestData`/`SchemalessTestData` exist only there, verified by grep rather than by "it compiles".
  4. `./gradlew compileTestJava compileTestFixturesJava` and `./gradlew compileIntegrationTestJava` all succeed, so the out-of-scope integration source set does not rot into a permanently broken state.
  5. `uniqueConnectorName(TestInfo)` is available again and the `@AfterEach` MBean leak sweep runs, extended to cover upstream's per-topic MBeans; the per-JVM-fork shared-container strategy is re-derived over upstream's `@TestInstance(PER_CLASS)` shape, or its cost is measured and the fallback recorded.

**Plans**: 3/3 plans executed

**Wave 1**

- [x] 10-01-PLAN.md — The shared container per JVM fork with its opt-out seam, the `tearDown` database drop and class-keyed log copy, the `@AfterEach` MBean sweep, the restored `latest` image default with floating-tag `alwaysPull`, and the un-parked `ClickHouseTestHarnessTest` canary plus its two new pins (wave 1)

**Wave 2** *(blocked on Wave 1 completion)*

- [x] 10-02-PLAN.md — The acceptance gate: `testUnit` with the Docker daemon provably stopped, then two consecutive clean full `./gradlew test` runs on ClickHouse `latest`, with D-06 attribution and a decision checkpoint for anything the new server surfaces (wave 2)

**Wave 3** *(blocked on Wave 2 completion)*

- [x] 10-03-PLAN.md — `HARN-04`/`05`/`06`/`07` discharged as re-measured evidence, the seventeen `MERGE-CHECKLIST.md` row flips, Recorded Facts 15 and 16, and the `HARN-01..07` closure in `REQUIREMENTS.md` (wave 3)

**Note on the wave shape and on this phase's real scope:** the three waves are sequential and that is structural. Plans 10-01 and 10-02 both resolve against `ClickHouseBase.java`'s final state, the gate can only measure a tree that already has the re-composition in it, and a checklist row flipped before its evidence exists would hollow out Phase 11's zero-pending assertion. Worth recording separately: **four of this phase's seven requirements were already satisfied before it began.** `HARN-04`'s feared duplicate helper does not exist, `HARN-05`'s test-data helpers are already single-copy, `HARN-06`'s integration source set already compiles, and `HARN-07`'s Testcontainers `2.0.5` already ran 645 tests green in Phase 9 — all four close on evidence re-measured at execution time, not on new code. What no requirement named, and what discussion found, is that `CLICKHOUSE_VERSION_DEFAULT` came out of the merge as a pinned LTS version, so every local gate since the merge — including the green closes of Phases 8 and 9 — silently ran against a different server from the one every planning document claims. CI could never have caught it, because `tests.yaml` pins the version in its job environment. That reversion is the seventh silently-vanishing fork behaviour and is fixed and pinned here.

### Phase 11: Full Suite, CI & Publication

**Goal**: The merged connector is accepted by the suite the previous milestone was built to provide — green locally on ClickHouse `latest` × client V2, green in CI on the pushed SHA, published to the public fork by a non-force merge, with the shrunken fork delta written down.

**Depends on**: Phase 10

**Requirements**: SUITE-01, SUITE-02, SUITE-03, CI-05, CI-06, CI-07

**Success Criteria** (what must be TRUE):

  1. `env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew test` is green against ClickHouse `latest` × client V2 over the fork's tests plus upstream's 56 test files, with every skip accounted for and explained and zero `InstanceAlreadyExistsException` in a full-suite run.
  2. The four fork SMTs (`TableOperationFilter`, `HeadersToValue`, `HeaderToValue`, `ConvertToMapOrJson`) and their sink-landing tests pass untouched — upstream touches nothing in that package, so any failure there is investigated as a genuine defect rather than absorbed as merge fallout.
  3. `tests.yaml` runs the merged suite on push and publishes coverage, its wall-clock measured against the job's `timeout-minutes: 45`, and a run is machine-verified green on the pushed SHA — located by `head_sha` plus workflow `path`, asserting exactly one match.
  4. `custom_build` is brought forward from `sync/v1.6.0` by a non-force merge and pushed to the public fork after human approval.
  5. A written record lists every fork change dropped in favour of upstream and why, and shows the fork's delta over upstream is smaller after this milestone than before it.

**Plans:** 1/7 plans executed across 5 waves — `rehearse → close the debt → measure → approve → publish`. Wave 1 is genuinely parallel; waves 2 through 5 are sequential and that is structural, because the local gate can only measure the tree as it will be published, the CI rehearsal can only run on the tree the local gate measured, and the approval can only weigh evidence that already exists.

Plans:
**Wave 1**

- [x] 11-01-PLAN.md — Pre-publication debt: fix the fail-open invariant script, flip the six unowned checklist rows on measured diffs, run the two missing security audits (D-05/D-06/D-07)
- [ ] 11-02-PLAN.md — Drop the redundant CI job-environment pins so CI exercises the code defaults, and correct the Javadoc asserting them in the same commit (D-09)
- [ ] 11-03-PLAN.md — `docs/FORK-DELTA.md`: behaviour ledger verified against the merged tree, raw numbers at both scopes, and a Known costs section leading on R-01 (D-13..D-16)

**Wave 2** *(blocked on Wave 1 completion)*

- [ ] 11-04-PLAN.md — Local acceptance gate: one cache-busted full run on the published tree with the skip inventory re-derived from it, plus Docker-free `testUnit` (D-12)

**Wave 3** *(blocked on Wave 2 completion)*

- [ ] 11-05-PLAN.md — Tracer: scratch-branch CI rehearsal end-to-end, wall-clock measured against a stated threshold, then D-03's evidence bundle (D-02/D-10/D-11)

**Wave 4** *(blocked on Wave 3 completion)*

- [ ] 11-06-PLAN.md — The one blocking human gate, then the forced non-fast-forward merge, the non-force push of branch and both tags, and CI-06's evidence on the published SHA (D-01/D-03/D-04)

**Wave 5** *(blocked on Wave 4 completion)*

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
| 11. Full Suite, CI & Publication | 1/7 | In Progress | - |

---
*Roadmap for milestone `v1.6.0_gg1-sync` created: 2026-09-18*
