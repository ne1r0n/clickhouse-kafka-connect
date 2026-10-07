# Roadmap: ClickHouse Kafka Connect — `custom_build` Fork Test Coverage

## Milestones

- ✅ **v1.2.9_gg4-tests Fork Test Coverage** — Phases 1-5 (shipped 2026-09-18)
- ✅ **v1.6.0_gg1-sync Upstream v1.6.0 Sync** — Phases 6-11 (shipped 2026-10-07)
- 🚧 **v1.7.0_gg1-sync Upstream v1.7.0 Sync & Follow-up Hardening** — Phases 12-15 (in progress)

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

### v1.7.0_gg1-sync — Upstream v1.7.0 Sync & Follow-up Hardening

**Milestone goal:** Merge upstream `v1.7.0` into `custom_build`, close all 17 open follow-ups (including R-01's silent data loss), and publish `v1.7.0_gg1` with the suite green on ClickHouse `latest` × client V2 and CI green on the published SHA.

- [ ] **Phase 12: Upstream v1.7.0 Merge & Reconcile** - `v1.7.0` merged mechanically on a scratch branch with every fork feature proven intact, D-17 retired onto upstream's `setCompressionMethod`, #820's null-Map fix made reachable, and exactly-once state reads proven across a restart
- [ ] **Phase 13: Fork Hardening on the Merged Tree** - R-01's silent data loss fixed by routing, nulls in Map/Array/Tuple slots either round-trip or fail loudly, and the metadata, config and test-debt follow-ups closed
- [ ] **Phase 14: Dependency Bumps & CI** - lz4-java, Jackson, okhttp, org.json and mockito bumped one attributable commit at a time; CI compiles integration sources, reports its fork count, and runs once per SHA
- [ ] **Phase 15: Suite Gate, Publication & Rollout** - Two clean full-suite runs, `v1.7.0_gg1` published to `custom_build` with green CI on the published SHA, planning history on `origin`, operator rollout checklist written

## Phase Details

### Phase 12: Upstream v1.7.0 Merge & Reconcile

**Goal**: Upstream `v1.7.0` is merged onto a disposable scratch branch with every fork feature intact and the three silent hazards in the merge resolved by intent: D-17 retired in favour of upstream's `setCompressionMethod`, upstream's #820 null-Map fix actually reachable rather than shadowed by the fork's MAP guard, and the build down to one LZ4 rule. Upstream's exactly-once state-read change is proven across a provider restart. `c61d90ce` stays one tag away.

**Depends on**: Phase 11 (shipped — `v1.6.0_gg1`, `c61d90ce`); nothing in this milestone

**Requirements**: SYNC-07, SYNC-08, SYNC-09, SYNC-10, CMP-01, CMP-02, CMP-03, WRT-06, WRT-09, BLD-09, BLD-13, EOS-01

**Success Criteria** (what must be TRUE):

  1. A local `pre-v1.7.0-sync` tag names the pre-sync `custom_build` SHA. Upstream `v1.7.0` (`379b8d01`, addressed by tag and never as bare `upstream/main`) sits on a scratch branch as one mechanical merge commit. The invariant script is now parameterised (`PRE_TAG`, `UPSTREAM_REF`) and passes both its line-provenance check and its grep that every `docs/FORK-DELTA.md` anchor construct survives. `custom_build` and `origin` are untouched.
  2. A v1.7.0 merge checklist lists every path upstream changed with its predicted and actual conflict class. It also gives a reviewed verdict for each auto-merged hunk inside a fork-touched file: the `ClickHouseWriter` MAP branch, the `build.gradle.kts` LZ4 rule, the `ClickHouseClientBuilder` setter sites and the test helpers.
  3. Every fork feature in `FORK-DELTA.md` passes its existing pin test: `Type.DYNAMIC` fail-fast, JMX `connector=<name>`, `targetTableFilter`, describe sub-column skip, metadata-free JSON path, Connect API `4.1.1` and `CLICKHOUSE_VERSION_DEFAULT = latest`. Upstream's new and changed tests (`nullMapTypeTest`, `testSetCompressionMethod_*`, #800 Avro-union, #801 buffer, cluster `buildFromClause`) pass byte-unmodified on `latest` × V2 or skip for a reason named in `FORK-DELTA.md`. The build has one LZ4 rule: `dependencyInsight` shows exactly one `at.yawk.lz4:lz4-java` on `runtimeClasspath`, and the stale "1.10.2" wording is gone from `FORK-DELTA.md`, `MERGE-CHECKLIST.md` and `.claude/CLAUDE.md`. The catalog no longer has `clickhouse-jdbc` or the `# FORK OVERRIDE (upstream 0.9.5)` comment and still has `kafka = "4.1.1"`.
  4. D-17's constants and pin lines are gone, and every client the connector builds receives `setCompressionMethod`: writer V1/V2, the writer's temporary client and `KeeperStateProvider`. Docker-free tests pin `compress=false` / `compressServerResponse(false)` and `network_compression_method=lz4` on the V1 options, the V1 node and the V2 config. With the flag `false`, lz4 is absent and response compression stays off. A live insert-and-read-back on `latest` succeeds with the flag `true` and with `false`, and `FORK-DELTA.md` records from that run which control is load-bearing against the ZSTD default.
  5. A null optional `Map` written into a non-nullable `Map` column produces upstream #820's bytes (`00`, or `00 00` with defaults), pinned Docker-free, because the fork's MAP null guard now fires only for nullable columns. Every CR-02 pin re-pinned to the correct bytes carries its rationale next to the changed assertion. A `KeeperStateProvider` restart-read test passes: state is written, a new provider instance is created, and the same min/max/state is read back. That test has been seen to fail against the old summary-count code.

**Plans**: TBD

**Research flag**: `KeeperStateProvider` is a high-risk exactly-once file (AGENTS.md Key Files) with no fork edits, and `exactlyOnceStateMismatchTest` is one of the 18 named standalone skips, so nothing in the standard suite exercises the new stream-count read. EOS-01 needs a deliberate mutation check: temporarily restore `getSummary().getResultRows()` and watch the new test go red. Planning should also run the short live compression experiment from research (`compress=true` without lz4 against `latest`, then `compress=false` without lz4) before CMP-03 writes its `FORK-DELTA.md` conclusion.

### Phase 13: Fork Hardening on the Merged Tree

**Goal**: The fork's open correctness follow-ups are closed on top of the merged writer. R-01's silent data loss is gone: `Struct` records in metadata-free `insertFormat=json` mode land through the JSON writer with correct typed values. A null in a Map/Array/Tuple slot either round-trips as `NULL` or fails loudly instead of desyncing RowBinary. The metadata, config and test-debt findings from earlier review are fixed and pinned.

**Depends on**: Phase 12 (the R-01 route, WR-02 and P9-IN-01 all sit on hunks the `v1.7.0` merge rewrites, including #820's MAP/ARRAY null branches)

**Requirements**: JSONP-05, JSONP-06, JSONP-07, JSONP-08, WRT-07, WRT-08, DESC-05, TYPE-08, COLS-07, CFG-04, DEBT-01, DEBT-02, DEBT-03, DEBT-04, DEBT-05

**Success Criteria** (what must be TRUE):

  1. With `insertFormat=json`, a `Struct` record and `bypassRowBinary` at its default `false`, every record lands. Routing keys on `shouldSkipTableMetadata()`, never on the raw format string. The former R-01 pin test now asserts typed read-back of every record instead of "0 rows, no throw".
  2. On that route, a typed read-back matrix shows Kafka `Timestamp`, `Date`, `Time`, `Decimal` and `bytes` landing with correct values in `DateTime`, `DateTime64`, `Date`/`Date32`, `Decimal` and `String` columns, with conversion fixed wherever Jackson's defaults stored a wrong value. A null field lands as `NULL` in a Nullable column and as the column `DEFAULT` otherwise. The route's interaction with `jsonInsertColumns` and with `bypassFieldCleanup` is pinned.
  3. A `null` entry inside a `Map(K, Nullable(V))` column reads back as SQL `NULL`. A `null` in a nested non-nullable slot (Array element, Map value, Tuple field) throws a `DataException` naming the column, with no row written. A top-level null in a non-nullable Map/Array column keeps upstream's coercion.
  4. Each of these fixes is pinned by a test:
     - A DESCRIBE row two or more levels below a `JSON`/`Dynamic` column is skipped.
     - `Int256`/`UInt256` columns fail fast through `validateDataSchema` with the actionable message, and `Int128`/`UInt128` still write.
     - `Column.extractColumn`'s Array recursion passes `hasDefaultValue` and `isSubColumn` in the right order.
     - `ConfigDef` reports `"default"` as the `USERNAME` default.
     - `cleanupExtraFields` evaluates `shouldSkipTableMetadata()` before it dereferences `csc`.
  5. Test debt is closed:
     - `HeaderToValue`'s behaviour when `fields` names a pre-existing non-STRING column is pinned.
     - `JsonColumnTest` asserts read-back values, not row counts.
     - Both `ClickHouseSinkTaskJsonInsertFormatTest` tests use the `try/finally` task lifecycle.
     - A database-drop failure in `ClickHouseBase.tearDown()` is attributed to the class that caused it rather than logged and swallowed.
     - The `ClickHouseVersionDefaultUnitTest`, `AsyncInsertLiveTest` and `NetworkCompressionPin*` Javadocs describe the code as it now is.

**Plans**: TBD

**Research flag**: spike before planning on (a) JSON-route type fidelity: what `Timestamp`/`Date`/`Time`/`Decimal`/`bytes` actually store through Jackson today. The matrix asserts observed values and does not assume parity with RowBinary. (b) WR-02's discriminator: how `doWriteColValue` tells a nested non-nullable slot from a top-level column, and what that means for the CR-02 TUPLE/VARIANT branches. Ordering inside the phase is structural. R-01 routing, WR-02, P9-IN-01 and P3-WR-03 all edit `ClickHouseWriter.java` and serialise. The metadata/config fixes and the test debt touch other files and can be planned as parallel plans. Both the R-01 change and the nested-null `DataException` are operator-visible behaviour changes. Each is recorded for the CHANGELOG and `FORK-DELTA.md` (written in Phase 15) as it lands.

### Phase 14: Dependency Bumps & CI

**Goal**: The approved dependency bumps land on the hardened tree one attributable commit at a time, so a regression points at exactly one version change. CI checks what a local run checks: integration sources compile, and the fork count is measured. A published SHA gets exactly one run.

**Depends on**: Phase 13 (Jackson sits under the R-01 JSON route, so bumping it before the route and its type matrix exist would make a JSON-path regression ambiguous)

**Requirements**: BLD-10, BLD-11, BLD-12, CI-08, CI-09, CI-10

**Success Criteria** (what must be TRUE):

  1. `lz4-java` resolves on `runtimeClasspath` to an advisory-free release past upstream's `1.10.4` (at least 1.11.2, target 1.12.0). Client `0.9.6` is shown working with it: it compiles, Docker-free `testUnit` passes, and a live LZ4 round-trip succeeds on `latest`.
  2. Jackson `2.22.3` lands in its own commit with the JSON-route tests green, including Phase 13's R-01 matrix. okhttp `5.5.0`, org.json `20260814` and mockito `5.24.0` land as plain version edits rather than dependabot cherry-picks, one commit each, each gated by `compileIntegrationTestJava` and Docker-free `testUnit`.
  3. `tests.yaml` compiles the integration-test source set (`compileIntegrationTestJava`) on every push. CI's test fork count is read from a job log or summary of a run on this tree, or is explicitly recorded as unmeasured with the reason.
  4. `tests.yaml`'s trigger and concurrency group are fixed so that a branch push and a tag push on the same SHA produce exactly one completed run. The fix is committed before any tag reaches `origin`.

**Plans**: TBD

**Note**: org.json `20260719` introduced a configurable length limit on BigInteger/Decimal numbers, and its default is unknown. If the first full run after the bump changes an Int256/Decimal256 fixture, bisect by reverting `orgJson` alone. Getting a CI run for criterion 3 means pushing the scratch branch, which triggers `tests.yaml`. That is allowed (branch only, as in Phase 11's rehearsal). No tag is pushed in this phase.

### Phase 15: Suite Gate, Publication & Rollout

**Goal**: `v1.7.0_gg1` is published. The merged and hardened tree passes the full suite twice on ClickHouse `latest` × client V2. `custom_build` receives it by a non-force `--no-ff` merge after human approval, with CI green on the published SHA. The post-`c61d90ce` planning history is on `origin`, and operators have a written checklist for every behaviour change they will see.

**Depends on**: Phase 14 (CI-08 must be in the published tree before any tag is pushed)

**Requirements**: SUITE-04, PUB-01, PUB-02, PUB-03, PUB-04, ROLL-01

**Success Criteria** (what must be TRUE):

  1. After `clean`, `VERSION` reads `v1.7.0_gg1` and the generated `Version.java` agrees. `CHANGELOG.md` has a `1.7.0_gg1` section naming the R-01 behaviour change, WR-02 strictness, compression and lz4. `docs/FORK-DELTA.md` shows:
     - D-17 retired
     - R-01 gone from Known costs
     - the LZ4 rule and version
     - the MAP guard scoping
     - every new kept patch or cost
  2. The full suite on `latest` × V2 passes twice, the second run with `--rerun`. Both runs have 0 failures, 0 errors and a skip set equal to the named skips in `FORK-DELTA.md`. `testUnit` passes with the Docker daemon stopped.
  3. An operator rollout checklist covers:
     - the JMX MBean name before and after (`connector=` and the per-release `version=` segment)
     - compression semantics, the `compress=true`-in-`jdbcConnectionProperties` caveat and the server-profile check
     - R-01's behaviour change, with the exactly-once backfill procedure (offset reset plus KeeperMap state clear)
     - JSON-route type notes
     - the nested-null `DataException` (DLQ-able)
     - the rollback tag
  4. After a recorded human approval, `custom_build` on `origin` is a non-force `--no-ff` merge, and tags `v1.7.0_gg1` and `pre-v1.7.0-sync` are on `origin`. The published SHA has exactly one completed `tests.yaml` run, located by `head_sha` plus workflow path, with no cancelled sibling. That run is green, which proves CI-08 end to end.
  5. The `.planning/` history after `c61d90ce` is on `origin` through a merge, not a fast-forward. It carries neither `.planning/.gsd-ws-arg` nor any local-only tag.

**Plans**: TBD

**Note**: this phase mirrors Phase 11's shape: rehearse, measure, approve, publish. The human approval is a blocking-human gate. `gsd-executor` refuses approvals relayed through the orchestrator, so the orchestrator runs post-gate tasks inline. `custom_build` is GSD's protected base branch.

## Sync Mechanics (applies to every phase)

- **Scratch branch, not `custom_build`.** All merge and hardening work happens on a scratch branch, with the pre-sync tip tagged `pre-v1.7.0-sync`. `custom_build` is published and protected, so it only moves forward by a non-force `--no-ff` merge after human approval (Phase 15). That keeps `git revert -m 1` viable.
- **Upstream ref is the tag.** Always `v1.7.0` (`379b8d01`). Never bare `upstream/main`, because a stale `refs/upstream/main` makes it ambiguous.
- **Commit cadence.** No `.planning/` commit is interleaved with in-progress conflict resolution. No dependency bump rides inside the sync commit; bumps live in Phase 14, one commit per group.
- **Gradle invocation.** Always `env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew ...`. This shell sets the variable to the literal string `"null"`, which breaks Ryuk. Docker is colima.
- **Gates.** `testUnit` (Docker-free) is the cheap gate. Any two-run gate busts Gradle's cache with `--rerun`. A red full suite on unchanged code means checking the `latest` image date first, because ClickHouse 26.9's ZSTD default is the precedent.
- **Pushes.** Pushing any branch triggers `tests.yaml`. No tag reaches `origin` before CI-08 lands (Phase 14).
- **Editing this file by script.** Slice to the target `### Phase N:` section before any find-and-replace. Wave and note headings repeat across phases, and an unanchored replace truncated this roadmap once (`c1480f8f`).

## Structure Notes

This roadmap follows the research's four-phase order (sync → hardening → dependencies and CI → publication), which is locked: the sync lands first because `v1.7.0` rewrites the code WR-01/WR-02, P9-IN-01 and D-17 touch. Three placement choices are worth recording:

- **Phase 12 carries more than the merge.** CMP-01..03, WRT-06, WRT-09, BLD-09, BLD-13 and EOS-01 are merge-line decisions, not later fixes. Without them the merged tree is wrong in ways no conflict marker reveals:
  - the shadowed MAP guard desyncs RowBinary
  - the fork's duplicate LZ4 rule masks the 1.10.4 bump
  - upstream's `testSetCompressionMethod_Disabled` cannot pass beside D-17's unconditional pin
  - the `KeeperStateProvider` read change travels with `COMPRESS=false`
- **Phase 13 is not split, despite owning 15 requirements.** Its four writer items (R-01 routing, WR-02, P9-IN-01, P3-WR-03) serialise on `ClickHouseWriter.java` anyway. The remaining one-line metadata/config fixes and the test debt would form a thin internal-quality phase, which coarse granularity argues against. Keeping the test debt here also means `JsonColumnTest` read-back and the JSON-format `try/finally` are already tightened when Phase 14's Jackson bump is measured against them. The non-writer items touch different files, so they can plan as parallel plans within the phase.
- **CI-08 lands in Phase 14 but is proven in Phase 15.** The workflow fix has to be in the tree before any tag push. Its end-to-end evidence, exactly one completed run on the published SHA, can only exist once Phase 15 publishes. Phase 15's criterion 4 is that proof.

## Progress

| Phase | Plans Complete | Status | Completed |
|-------|----------------|--------|-----------|
| 12. Upstream v1.7.0 Merge & Reconcile | 0/TBD | Not started | - |
| 13. Fork Hardening on the Merged Tree | 0/TBD | Not started | - |
| 14. Dependency Bumps & CI | 0/TBD | Not started | - |
| 15. Suite Gate, Publication & Rollout | 0/TBD | Not started | - |

---
*Roadmap for milestone `v1.7.0_gg1-sync` created: 2026-10-07*
