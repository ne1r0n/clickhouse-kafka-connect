# Phase 10: Test Harness Reconciliation - Context

**Gathered:** 2026-09-20
**Status:** Ready for planning

<domain>
## Phase Boundary

Re-compose the fork's `ClickHouseBase` harness contract — `@Tag("clickhouse")`, one shared container per test JVM fork, the `@AfterEach` MBean leak sweep, `uniqueConnectorName(TestInfo)` — over upstream's rewritten test infrastructure and Testcontainers `2.0.5`, so the fork's tests have somewhere to run and **both** gates mean what they claim: `testUnit` is genuinely Docker-free, and a full live run genuinely exercises the connector against the server the milestone declares.

Delivers: the shared-container strategy restored with an explicit opt-out seam for the two Toxiproxy classes; the MBean leak sweep restored verbatim and proven against upstream's per-topic MBeans; the `ClickHouseTestHarnessTest` canary adapted to the post-merge API and un-excluded from `build.gradle.kts`; the `latest` → `25.8.18.1` image-default reversion undone and pinned by a Docker-free canary; `HARN-04`/`05`/`06`/`07` discharged as grep-and-record evidence acts; 17 `MERGE-CHECKLIST.md` rows flipped.

Gate: `env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew testUnit` with Docker genuinely stopped, **plus two consecutive clean full `./gradlew test` runs on ClickHouse `latest`** — see D-09.

Out of this phase: Phase 11's publication, CI wall-clock measurement, `tests.yaml` changes and the SUITE-03 fork-delta record; R-01's fail-fast fix and every other Future Requirement; P4-WR-01's missing `try/finally` (D-08 keeps it an accepted follow-up, deliberately); CR-01's fail-open invariant script (still unowned, due before Phase 11); `/gsd-secure-phase 07` and `09`.

### Measured starting state

Re-derived at discussion time against the merged tree at `d23c61b3` — **re-confirm by command before planning; do not re-derive by reading this table.**

| Fact | Evidence |
|---|---|
| `@Tag("clickhouse")` and `uniqueConnectorName(TestInfo)` are **already restored** | `ClickHouseBase.java:25`, `:139`; the whole merged file is upstream + 14 lines (`git diff v1.6.0 -- …/ClickHouseBase.java`). Phase 7's early sliver, commit `e1a90309` |
| The `@AfterEach` MBean leak sweep is **absent** | no `@AfterEach` anywhere in `ClickHouseBase.java`; the pre-merge `cleanupLeakedMBeans()` is not in the diff |
| The container is **per test class**, not per JVM fork | upstream's `@TestInstance(PER_CLASS)` (`:26`) + instance field `protected ClickHouseContainer db` (`:30`) + `@BeforeAll public void setup()` (`:36`) calling `setupContainer` (`:53`) + `@AfterAll tearDown()` calling `ch.stop()` (`:85`). **27 classes** extend `ClickHouseBase` |
| `sharedContainerStarts()`, `newTestDatabaseName()`, `createProps()`, `createClient()` are all gone | the pre-merge base was 291 lines; the merged one is 143. The only remaining caller, `ClickHouseTestHarnessTest`, is excluded from the test **compile** source set at `build.gradle.kts:448` |
| **HARN-04 is already true** | `git ls-files \| grep ClickHouseTestHelpers` → exactly one path, `src/testFixtures/java/com/clickhouse/kafka/connect/sink/helper/ClickHouseTestHelpers.java`. The feared orphaned `src/test` copy does not exist |
| **HARN-05 is already true** | `SchemaTestData`/`SchemalessTestData` exist only under `src/testFixtures`; `FromVersionConditionExtension` is at `src/testFixtures/java/com/clickhouse/kafka/connect/test/junit/extension/`; `ClickHouseAPI.java` is gone with **zero** references in `src/` |
| **HARN-06 is already true** | `env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew compileIntegrationTestJava compileTestFixturesJava` → **exit 0** (one pre-existing `unchecked` warning in `ClickHouseCloudAPI.java:64`) |
| **HARN-07 is de facto proven** | `gradle/libs.versions.toml:23` → `testcontainers = "2.0.5"`; Phase 9's full run was 645 tests / 0 failures / 0 errors / 18 skipped in **3m01.52s**. The requirement's "before any of the 371 fork tests are adapted" sequencing is already moot |
| ⚠ **`CLICKHOUSE_VERSION_DEFAULT` reverted `latest` → `25.8.18.1`** | pre-merge: `git show pre-v1.6.0-sync:…/ClickHouseTestHelpers.java:42` = `"latest"`. Merged: `src/testFixtures/…/ClickHouseTestHelpers.java:30` = `"25.8.18.1"`. `CLICKHOUSE_VERSION` is **unset** in this shell and `clickhouse/clickhouse-server:25.8.18.1` is in the local Docker cache — so **Phases 8 and 9 took their green evidence on 25.8.18.1, not `latest`** |
| ⚠ `PullPolicy.alwaysPull()` is gone | pre-merge `ClickHouseBase` used it; the merged `setupContainer` (`:56-70`) sets no pull policy |
| CI and local disagree | `.github/workflows/tests.yaml:33` sets `CLICKHOUSE_VERSION: latest` explicitly, so **the regression is invisible from CI** and bites only local runs — which is where every GSD gate since the merge took its evidence |
| `MBeanServerUtils` **rethrows** `InstanceAlreadyExistsException` | `MBeanServerUtils.java:22-23` → `throw new RuntimeException(e)`. HARN-02's "zero `InstanceAlreadyExistsException` in a full-suite run" is therefore self-enforcing: a collision is a test failure, not a log line |
| Upstream's per-topic MBean name is a **superset** of the task name | `SinkTaskStatistics.java:176` formats `com.clickhouse:type=ClickHouseKafkaConnector,connector=%s,name=SinkTask%d,version=%s`; `:188-189` composes the topic name as that `+ ",topic=" + topic`. The pre-merge sweep pattern `com.clickhouse:type=ClickHouseKafkaConnector,*` already matches both |
| Two classes attach Toxiproxy to the container's `Network` | `ClickHouseSinkTaskSchemalessProxyTest.java:58-60` and `ClickHouseSinkTaskWithSchemaProxyTest.java:81-83` both call `.withNetwork(db.getNetwork())` and both claim `.withNetworkAliases(TOXIPROXY_NETWORK_ALIAS)` — the same literal alias `"toxiproxy"` |
| One class mutates server-global state | `ClickHouseHelperClientTest.java:105-118` runs `CREATE USER` + `GRANT … ON *.*`. Names are timestamped via `createTestUsername`, so they do not collide — but they now accumulate on a server that outlives the class |
| `maxParallelForks = availableProcessors() / 2` | `build.gradle.kts:190-196`. On a 2-core GitHub runner that resolves to **1**, so post-merge CI would start 27 ClickHouse containers **sequentially** |
| 17 `MERGE-CHECKLIST.md` rows are pending, **all** owned by Phase 10 | Phase 10 is the last phase with pending rows; Phase 11's publication gate asserts zero |

### What this means for the phase's shape

The ROADMAP wrote Phase 10 as "re-compose the harness so the 371 fork tests have somewhere to run". They already have somewhere to run — the suite is green at 645 tests. Four of the seven requirements (`HARN-04`, `05`, `06`, `07`) are satisfied by the merge plus Phase 7's work and reduce to grep-and-record. The real remaining work is three behaviours (`HARN-01`'s Docker-free proof, `HARN-02`'s sweep, `HARN-03`'s shared container), the canary un-exclude, and one regression this discussion found that no requirement names.

</domain>

<decisions>
## Implementation Decisions

### Shared-container strategy (HARN-03)

- **D-01: Restore one shared container per test JVM fork**, re-derived over upstream's `@TestInstance(PER_CLASS)` shape rather than taking the fallback HARN-03 permits. A static container plus a lazy start guard; `@BeforeAll setup()` stays an instance method and reuses the static container; `@AfterAll tearDown()` **drops the class database** instead of stopping the container.
  - The `tearDown()` database drop is **not optional**: `ClickHouseTestHarnessTest.classGetsFreshIsolatedDatabase` (`:99-106`) asserts that **zero other** `kafka_connect_test_%` databases exist on the server. Under per-class containers upstream got that for free by stopping the container; under sharing it must be explicit.
  - Rejected — *keep upstream's per-class container and record the fallback*: `maxParallelForks` resolves to `1` on a 2-core CI runner, so per-class means 27 sequential container starts on exactly the machine Phase 11 measures against `timeout-minutes: 45`. The locally-measured 3m01s is not representative.
  - Rejected — *measure both first, then decide*: the CI shape is the one that matters and cannot be measured cheaply from here; the fork contract is already documented and already proven (Phase 1: 0 flakes at 6 forks).
  - **Reversibility:** reversible — the container field and the `tearDown` body; no production code and no published contract is involved.

- **D-02: Shared by default, with an explicit opt-out seam.** Add an overridable hook on `ClickHouseBase` (shape at the planner's discretion, e.g. `protected boolean usesSharedContainer()` defaulting to `true`). `ClickHouseSinkTaskSchemalessProxyTest` and `ClickHouseSinkTaskWithSchemaProxyTest` override it to `false` and get their own container + `Network`; 25 of 27 classes use the shared one.
  - **Why a seam and not luck:** both proxy classes bind Toxiproxy to `db.getNetwork()` and both claim the alias `"toxiproxy"`. Under one shared container they land on the same network with a colliding alias **whenever Gradle assigns them to the same fork** — which varies run to run. A green run would prove nothing about the next one.
  - **Constraint for the planner:** an opt-out class's container **must not** increment the shared-start counter, or `assertEquals(1, sharedContainerStarts())` becomes order-dependent.
  - Rejected — *attach the shared container to each proxy class's own `Network` at class setup*: live network surgery on a running container, to buy back one container.
  - **Reversibility:** reversible.

- **D-03: Keep upstream's server-log extraction, keyed by test-class name.** Copy `clickhouse-server.log` / `.err.log` into `build/reports/tests/server_<SimpleClassName>.log` while the shared container is still alive, rather than upstream's `server_<mappedPort>.log` written just before `ch.stop()`.
  - No JVM shutdown hook — Phase 1 explicitly ruled one out ("cleanup left to Ryuk, no JVM shutdown hook"), and that decision stands.
  - Accepted cost: 27 cumulative snapshots of the same growing log; the last one written is the complete record.
  - Rejected — *drop the copy* (the verbatim pre-merge shape): server-side logs are how a live-suite failure gets diagnosed, and this milestone's remaining gates are live-suite gates.
  - Rejected — *keep it only for opt-out classes*: leaves the 25 shared classes without the diagnostic, which is backwards.
  - **Reversibility:** reversible.

### ClickHouse image default — the seventh silently-vanishing fork behaviour

- **D-04: Restore `CLICKHOUSE_VERSION_DEFAULT = "latest"` and add a Docker-free canary that pins it.** The canary asserts the default value itself (a `testUnit`-visible test), not the composed image name — `ClickHouseTestHarnessTest`'s existing `assertEquals(CLICKHOUSE_DOCKER_IMAGE, db.getDockerImageName())` is self-consistent whatever the default is and would not have caught this.
  - **Why this is a canary and not a one-liner:** this is the seventh entry for `STATE.md`'s "vanishes with no conflict marker and no compile error" list, and the only one found *after* the phase that would normally have caught it. Same treatment Phase 9's D-11 gave the metadata-free bypass.
  - **Why it hid:** CI pins `CLICKHOUSE_VERSION: latest` in the job env (`tests.yaml:33`), so CI has been testing the right server all along. Only local runs — every GSD gate since the merge — silently moved to `25.8.18.1`.
  - Rejected — *keep upstream's LTS pin and lean on CI's env*: the milestone's own constraint is "ClickHouse `latest` + client V2 only", and Phase 11's criterion 1 is "green against ClickHouse `latest`". Leaning on CI makes that claim rest on a single machine nobody watches during development.
  - **Reversibility:** reversible.

- **D-05: `alwaysPull()` for floating tags only.** Apply `PullPolicy.alwaysPull()` when the resolved version is a floating tag (`latest`); use the default cached pull when `CLICKHOUSE_VERSION` names a pinned version.
  - Rejected — *restore unconditionally* (the exact pre-merge shape): pre-merge the default was always `latest`, so the distinction never arose. Now that D-09's comparison runs deliberately pin a version, re-pulling a pinned tag is pure waste and breaks offline runs for no benefit.
  - Rejected — *leave it off*: a floating tag that is never re-pulled is a softer version of the bug D-04 just fixed.
  - **Reversibility:** reversible.

- **D-06 (ownership rule): Phase 10 owns what `latest` surfaces, with a two-server comparison run for attribution.** The gate runs on `latest` at zero failures. If a class is red on `latest` where `25.8.18.1` is green, re-run **that class** pinned to `25.8.18.1` to separate "the harness broke it" from "the server changed", then decide fix-here vs hand-to-Phase-11 at a checkpoint, on that evidence.
  - **Why it matters:** `latest` is now 26.x and Phase 9's green was on 25.8. That version band is where `DESCRIBE` output for `JSON`/`Dynamic` sub-columns is most likely to have moved — exactly what `DESC-01..04` and `shouldSkipDescribeRow` pin.
  - This sits under the milestone's locked rule ("bug fixes live in the phase whose tests expose them") but adds the attribution step, so a 26.x server change does not silently balloon a harness phase into a type-mapping phase.
  - Rejected — *re-baseline the gate and hand server-version failures forward unconditionally*: Phase 9's own CONTEXT named a re-baselined gate as the thing that lets a second failure hide.
  - **Reversibility:** reversible.

### The `ClickHouseTestHarnessTest` un-exclude

- **D-07: Adapt the canary to the post-merge API; do not re-open `ClickHouseBase`.** Redirect `createProps()` → `getBaseProps()` and `createClient(props)` → `ClickHouseTestHelpers.createClient(props)`, exactly as Phase 7 did for 12 sibling files under D-06/D-09's locked restrictions. Then delete `exclude("**/ClickHouseTestHarnessTest.java")` from `build.gradle.kts:448`. `ClickHouseBase` gains only `sharedContainerStarts()`, which D-01 requires anyway.
  - **The tension, recorded:** this canary is what *proves* the harness contract, so rewriting it means the proof and the thing proved change in the same commit — the opposite of Phase 9 D-10's argument for keeping the parked classes byte-identical. Accepted knowingly: the proof now lives in the assertions, not in a zero diff. Commit them separately (D-11) so the API redirection and the contract changes are reviewable apart.
  - Rejected — *restore `createProps()`/`createClient()` on `ClickHouseBase`*: re-adds an API 12 files were just migrated off, creating two ways to do one thing on the most conflict-prone test file in the tree.
  - Rejected — *thin deprecated delegates*: a shim with exactly one caller that the next sync has to re-argue.
  - **Reversibility:** reversible.

- **D-08: Extend the canary to pin the opt-out hook and the per-topic MBean sweep.**
  - **Opt-out hook:** a class overriding the hook to `false` gets its own container **and the shared-start counter does not move**. Without this pin, `assertEquals(1, sharedContainerStarts())` is only true by luck of fork assignment, and the hook is exactly the kind of fork-only seam a future sync deletes with no compile error.
  - **Per-topic sweep:** register a sink task with a topic, leak it deliberately, assert the `…,topic=<t>` MBean is gone after `@AfterEach`. HARN-02 names per-topic coverage explicitly, and this class already carries the `DeliberateMidTestFailure` / `SwallowDeliberateMidTestFailure` machinery (`:36-51`) to prove it.
  - **Not pinned:** the class-name-keyed log copy (D-03). Asserting on `build/reports` output is brittle, and the copy is a diagnostic convenience, not a contract the connector depends on.
  - **Reversibility:** reversible.

### MBean leak sweep (HARN-02)

- **D-09a: Restore the sweep verbatim — WARN, unregister, continue.** `@AfterEach` querying `com.clickhouse:type=ClickHouseKafkaConnector,*`, logging each leak at WARN, and unregistering via `MBeanServerUtils.unregisterMBean`. The sweep's job is to stop a leak crossing into the **next** test, not to police test hygiene; D-08's `@Order(3)` → `@Order(4)` pair supplies the strict proof that it works.
  - **The per-topic extension may cost nothing:** the `,*` pattern already matches `…,connector=X,name=SinkTask0,version=V,topic=T`. Confirm by command, then discharge HARN-02's "extended to cover per-topic" with D-08's proving test rather than a pattern change. If the confirmation fails, widening the pattern is the fix.
  - Rejected — *unregister then fail the test*: would immediately red the two `ClickHouseSinkTaskJsonInsertFormatTest` methods that Phase 9's D-10 deliberately re-enabled unchanged, turning P4-WR-01 (an accepted, tracked follow-up) into a Phase 10 blocker in a file this phase does not own.
  - Rejected — *silent unregister at DEBUG*: a genuine leak regression becomes invisible in normal logs, which is how these behaviours vanish in the first place.
  - **Reversibility:** reversible.

- **HARN-02's second half needs no new machinery.** `MBeanServerUtils.java:22-23` rethrows `InstanceAlreadyExistsException` as a `RuntimeException`, so a name collision is already a test failure. "A full-suite run showing zero `InstanceAlreadyExistsException`" is discharged by **a green full suite**, stated as such in the SUMMARY.

### Acceptance gate

- **D-09: `testUnit` with Docker genuinely stopped, plus TWO consecutive clean full `./gradlew test` runs on `latest`.** Pass condition for each live run: zero failures, zero errors, every skip explained.
  - This raises ROADMAP criterion 1 (which asks only for the Docker-free `testUnit`), continuing the Phase 8 / Phase 9 precedent — and goes one step further for a reason specific to this phase: **D-01 makes 27 classes order-dependent within a fork for the first time**, and fork assignment varies run to run. One green run proves less here than it did in Phase 9. Two consecutive clean runs cost roughly 6 minutes and are the only thing that distinguishes "works" from "worked once". Phase 1 recorded 0 flakes at 6 forks for the pre-merge shape; this re-establishes that claim post-merge.
  - "Docker genuinely stopped" means stopped, not merely untouched — Phase 7's plan 07-04 set the precedent by running its `testUnit` gate with Docker actually down.
  - **Reversibility:** n/a — a gate, not an artifact.

### Claude's Discretion

The user did not constrain the following. All reversible; a planner may override with a stated reason.

- **D-10: Discharging `HARN-04`/`05`/`06`/`07`.** All four are already true (see the measured-state table). Discharge them as recorded evidence acts — the `git ls-files` output for the single `ClickHouseTestHelpers.java` and the `SchemaTestData`/`SchemalessTestData`/`FromVersionConditionExtension`/`ClickHouseAPI` paths (HARN-04/05, both requirements say "verified by grep, not by compilation"), the `compileIntegrationTestJava` exit code (HARN-06), and the Testcontainers `2.0.5` catalog pin plus a live container start through the re-composed `ClickHouseBase` (HARN-07). Re-run each command at execution time rather than citing this document.
- **D-11: Commit sequence**, per the one-behaviour-per-commit rule inherited from Phase 7 D-19 via Phase 8 D-14 and Phase 9 D-13: (1) the shared container + opt-out seam + `tearDown` database drop + class-name-keyed log copy; (2) the `@AfterEach` MBean sweep; (3) the `latest` default + floating-tag `alwaysPull` + its Docker-free canary; (4) the canary's API redirection **and** the `build.gradle.kts:448` un-exclude; (5) the canary's two new pins; (6) the 17 checklist flips and Recorded Facts. Not one commit per file, not one per plan. Run `./gradlew spotlessApply` over **only the files each commit touches** (Phase 8 D-15 — a tree-wide reformat has been pending since the merge and would destroy the single-behaviour diff property).
- **The opt-out hook's exact shape** — a `protected boolean` override, a JUnit `@Tag`, or an annotation. `usesSharedContainer()` is named here for readability only; the constraint that matters is D-02's counter rule.
- **Wave shape.** The sweep (D-09a) and the image default (D-04/D-05) are independent of the container work (D-01/D-02/D-03) and of each other. The canary un-exclude (D-07/D-08) depends on all three. The gate is necessarily last.
- **Cloud and cluster branches.** `isCloud` and `isCluster` already bypass `setupContainer` entirely (`ClickHouseBase.java:39-44`); this milestone tests neither. Leave both paths untouched and let the existing `Assumptions.assumeFalse(isCloud, …)` guards in the canary stand.
- **No new metric.** `AGENTS.md` asks that metrics land in `SinkTaskStatistics`/`TopicStatistics`; this phase changes no production insert path.

</decisions>

<canonical_refs>
## Canonical References

**Downstream agents MUST read these before planning or implementing.**

### Milestone contract
- `.planning/ROADMAP.md` § "Phase 10: Test Harness Reconciliation" — the five success criteria this phase is judged against. **Criterion 1 is `testUnit` only; D-09 deliberately raises it.** Criterion 2's "before any of the 371 fork tests are adapted" sequencing is already moot — the suite runs green today
- `.planning/ROADMAP.md` § "Merge Mechanics (applies to every phase)" — scratch-branch rule, the no-`.planning`-churn-during-conflict-resolution cadence, the mandatory `env -u TESTCONTAINERS_HOST_OVERRIDE` prefix, `testUnit` as the cheap gate
- `.planning/ROADMAP.md` § "Phase 11" criterion 3 — CI wall-clock measured against `timeout-minutes: 45`; the shared-container decision (D-01) is what that measurement will land on
- `.planning/REQUIREMENTS.md` § "Test Harness (HARN)" lines 96-102 — `HARN-01..07`, this phase's seven requirements. **`HARN-04`/`05`/`06`/`07` are already satisfied — see D-10 before planning work for them**
- `.planning/REQUIREMENTS.md` § "Locked decisions this milestone is built on" — conflict policy (D-04 is a carve-out on the same footing as the JMX and metadata-free-JSON ones), and the "bug fixes live in the phase whose tests expose them" rule D-06 operates under
- `.planning/REQUIREMENTS.md` § "Future Requirements" — P4-WR-01 (`ClickHouseSinkTaskJsonInsertFormatTest`'s missing `try/finally`) is listed here; D-09a deliberately keeps it there
- `.planning/REQUIREMENTS.md` § "Out of Scope" line 158 — `src/integrationTest` is compile-only by design (HARN-06)
- `.planning/MERGE-CHECKLIST.md` — **17 pending rows, all owned by Phase 10**; Phase 11's publication gate asserts zero pending. Rows at lines 16-22 (integrationTest paths, HARN-05/06), 33 (`ClickHouseBase`, HARN-01/02/03, with Phase 7's D-06/D-07 note), 37 (`ClickHouseSinkTaskTest`, with Phase 8's D-02 note), 44 (`ClickHouseTestHelpers`, HARN-04, with Phase 8's D-13 note), 49-50 (`SchemaTestData`/`SchemalessTestData`, HARN-05)
- `.planning/PROJECT.md` § Key Decisions — "One shared ClickHouse container per test JVM fork (`alwaysPull`), a fresh database per test class, cleanup left to Ryuk (no JVM shutdown hook), default `maxParallelForks` kept" is the Phase 1 decision D-01/D-03/D-05 re-derive; "Test matrix: ClickHouse `latest` × client V2 only" is the constraint D-04 restores

### Prior phase context
- `.planning/phases/07-type-mapping-writer-helper-client-config/07-CONTEXT.md` § D-06/D-09 — the locked restrictions on `ClickHouseBase`/`ClickHouseTestHelpers` that make D-07 the consistent choice, and the `getBaseProps()` / `ClickHouseTestHelpers.createClient` redirection pattern applied to 12 files
- `.planning/phases/07-type-mapping-writer-helper-client-config/07-CONTEXT.md` § D-19 — one-behaviour-per-commit, inherited by D-11
- `.planning/phases/08-orchestration-jmx-re-plumbing/08-CONTEXT.md` § D-12 — the raised-gate precedent and the Phase 7 gap-closure lesson behind it
- `.planning/phases/08-orchestration-jmx-re-plumbing/08-CONTEXT.md` § D-13 — annotate-don't-flip for cross-phase bookkeeping
- `.planning/phases/08-orchestration-jmx-re-plumbing/08-CONTEXT.md` § D-15 — scoped `spotlessApply` only
- `.planning/phases/09-metadata-free-json-path-keep-or-drop/09-CONTEXT.md` § D-10/D-11 — the byte-identical-diff-as-proof argument D-07 knowingly departs from, and the Docker-free-canary pattern D-04 reuses
- `.planning/phases/09-metadata-free-json-path-keep-or-drop/09-CONTEXT.md` § Integration Points — "Phase 10 inherits changed names/behaviour: these classes now *run*, so Phase 10's MBean leak sweep and shared-container work meets 13 more live tests than Phase 8's baseline did"

### Re-expression source (the pre-merge fork harness)
- `git show pre-v1.6.0-sync:src/test/java/com/clickhouse/kafka/connect/sink/ClickHouseBase.java` — 291 lines. The static `db` + `if (db == null)` lazy start, `SHARED_CONTAINER_STARTS` / `sharedContainerStarts()`, `DATABASE_COUNTER` / `newTestDatabaseName()`, `PullPolicy.alwaysPull()`, the `@AfterEach cleanupLeakedMBeans()` sweep in full, and the `tearDown` that drops the database and leaves the container to Ryuk
- `git show pre-v1.6.0-sync:src/test/java/com/clickhouse/kafka/connect/sink/helper/ClickHouseTestHelpers.java` — line 42 is `CLICKHOUSE_VERSION_DEFAULT = "latest"`, the value D-04 restores

### Files this phase touches directly
- `src/test/java/com/clickhouse/kafka/connect/sink/ClickHouseBase.java` — the static shared container + lazy guard (D-01); `sharedContainerStarts()`; the opt-out hook (D-02); `tearDown()`'s database drop and class-name-keyed log copy (D-01/D-03); the `@AfterEach` MBean sweep (D-09a). Currently `:25` `@Tag`, `:26` `@TestInstance`, `:36-51` `setup()`, `:53-71` `setupContainer()`, `:73-88` `tearDown()`, `:139` `uniqueConnectorName`
- `src/testFixtures/java/com/clickhouse/kafka/connect/sink/helper/ClickHouseTestHelpers.java:30` — `CLICKHOUSE_VERSION_DEFAULT` → `"latest"` (D-04); `:55-61` `getClickhouseVersion()` is the reader
- `src/test/java/com/clickhouse/kafka/connect/sink/ClickHouseTestHarnessTest.java` — API redirection (D-07) and two new pins (D-08)
- `build.gradle.kts:448` — delete `exclude("**/ClickHouseTestHarnessTest.java")` (D-07); the surrounding TEMPORARY comment block at `:439-450` goes with it
- `src/test/java/com/clickhouse/kafka/connect/sink/ClickHouseSinkTaskSchemalessProxyTest.java:58-60` and `…/ClickHouseSinkTaskWithSchemaProxyTest.java:81-83` — opt-out override only (D-02); the Toxiproxy wiring itself stays upstream's
- A new Docker-free test pinning `CLICKHOUSE_VERSION_DEFAULT` (D-04) — location at the planner's discretion; it must be reachable from `testUnit`, i.e. **not** under a `@Tag("clickhouse")` class
- `.planning/MERGE-CHECKLIST.md` — 17 row flips plus any Recorded Facts (D-04's reversion is a new one)
- `.planning/REQUIREMENTS.md` § HARN — `HARN-01..07` closed; if `HARN-04`/`05`/`06`/`07` close on evidence rather than work, say so in the flip, using the annotate-don't-flip pattern where a row belongs to another phase
- `src/main/java/com/clickhouse/kafka/connect/util/jmx/MBeanServerUtils.java` and `…/SinkTaskStatistics.java` — **read only**; `:22-23` and `:176`/`:188-189` are the facts D-08 and D-09a rest on. No production change in this phase

### Environment and process
- `.planning/STATE.md` § "Blockers/Concerns" — the `TESTCONTAINERS_HOST_OVERRIDE="null"` hazard and colima profile `vz`; the `[Phase 10]` "half done" row this phase closes; the `[Merge]` row naming `ClickHouseBase`'s contract as the last of the six silently-vanishing fork behaviours (**D-04 makes it seven**); the `[Merge]` `ClickHouseTestHelpers` duplicate row (already resolved — see D-10); the `[Phase 06]` CR-01 row; the GSD worktree-isolation quirk needing `--force-isolation none` immediately before each dispatch
- `.planning/config.json` — `build_command`/`test_command` carry the `env -u` prefix but **no `CLICKHOUSE_VERSION`**, which is precisely how D-04's reversion went unnoticed; `granularity: coarse`, `mode: yolo`, `test_gate_timeout: 2400`
- `build.gradle.kts:190-236` — the `withType<Test>` block: `maxParallelForks`, `file.encoding=windows-1252`, the JDK-17 `javaLauncher`, the summary listener; `:239-250` the `testUnit` task with `excludeTags("clickhouse")`
- `gradle/libs.versions.toml:23` — `testcontainers = "2.0.5"` (HARN-07)
- `.github/workflows/tests.yaml:33-35` — `CLICKHOUSE_VERSION: latest`, `CLIENT_VERSION: V2`, `./gradlew test`. **Do not change this file in Phase 10** — it is Phase 11's
- `AGENTS.md` § "Testing Discipline" (only add, never delete or weaken existing tests; read `ClickHouseTestHelpers.java` before writing tests), § "Coding Conventions", § "Correctness & Safety First"
- `.planning/codebase/TESTING.md` and `.planning/codebase/CONVENTIONS.md` — harness layout and SLF4J level conventions (the sweep logs at WARN per D-09a)
- `.planning/research/PITFALLS.md` — 17 pitfalls with symptom → cause → prevention → detection → owning phase

</canonical_refs>

<code_context>
## Existing Code Insights

### Reusable Assets

- **The pre-merge harness is recoverable verbatim** from `pre-v1.6.0-sync` — the static container guard, the counters, `alwaysPull()`, and `cleanupLeakedMBeans()` are all a handful of lines each. D-01/D-09a re-express rather than restore (upstream's `setupContainer` gained a `Network`, a network alias, a 2 GB memory cap, and the keeper_map XML mount that the pre-merge version never had), but the original is the reference for exact semantics.
- **`ClickHouseTestHarnessTest` already carries everything D-08's new pins need** — the `DeliberateMidTestFailure` exception, the `SwallowDeliberateMidTestFailure` handler that lets `@AfterEach` still run, `mbeanCountForConnector(String)`, and `@Order`-based sequencing that proves a leak in one test is gone by the next.
- **`@Tag("clickhouse")` and `uniqueConnectorName` are already in place** (Phase 7's sliver), so HARN-01 is a proof act — run `testUnit` with Docker stopped — not an edit.
- **`ClickHouseTestHelpers.createClient(props)` and `getBaseProps()` are the established post-merge way to build a client and props**; 12 fork test files already use them (Phase 7 D-06/D-09). D-07 makes the canary the thirteenth.

### Established Patterns

- **Docker-free canary for a silently-vanishing behaviour** (Phase 9 D-11's `MetadataFreeJsonInsertUnitTest`): when a fork behaviour has no conflict marker and no compile error to protect it, the fix is a `testUnit`-visible assertion. D-04 applies this to the image default — the seventh entry on that list.
- **Annotate-don't-flip** for cross-phase bookkeeping (Phase 7 D-03/D-07, Phase 8 D-02/D-13, Phase 9 D-07/D-14): when this phase touches a checklist row or requirement another phase owns, append a note with the commit SHA rather than rewriting it. Rows 33, 37 and 44 already carry such notes from Phases 7 and 8.
- **One behaviour per commit, scoped `spotlessApply`** (Phase 7 D-19 → Phase 8 D-14/D-15 → Phase 9 D-13). A tree-wide reformat has been pending since the merge and would destroy the single-behaviour diff property.
- **Assume-away rather than branch** for cloud/cluster: the canary uses `Assumptions.assumeFalse(isCloud, …)`; `ClickHouseBase.setup()` short-circuits both. Keep both paths untouched.

### Integration Points

- **⚠ `ClickHouseBase` is the widest fan-in in the milestone — 27 classes extend it**, and every one of them is a live ClickHouse test. A defect in D-01's lazy guard, D-02's opt-out seam or D-09a's sweep is blast-radius-wide. This is the structural reason D-09 demands two consecutive clean full runs rather than one.
- **⚠ The shared container makes classes order-dependent within a fork for the first time.** Per-class databases still give data isolation, but server-global state does not reset: `ClickHouseHelperClientTest.java:105-118` creates users and grants `ON *.*`, and the keeper_map config plus `CLICKHOUSE_DEFAULT_ACCESS_MANAGEMENT=1` are now shared across 25 classes instead of one. Phase 1 observed 0 flakes at 6 forks pre-merge, but upstream's 56 new test files have never run under a shared container.
- **⚠ `tearDown()`'s database drop is load-bearing, not tidiness.** `ClickHouseTestHarnessTest.classGetsFreshIsolatedDatabase` asserts zero *other* `kafka_connect_test_%` databases exist. Under sharing, a class that fails to drop its database reds an unrelated class later in the same fork.
- **MBeans are JVM-scoped, not container-scoped**, so the sweep's semantics are unchanged by D-01 — but with the container shared, a leaked MBean from an earlier class now coexists with a live server that the next class is also using, which is exactly the crossing D-09a prevents.
- **Phase 11 inherits the wall-clock number.** Its criterion 3 measures CI against `timeout-minutes: 45`; D-01 is the decision that number lands on. Worth a SUMMARY note with the measured local figures for both shapes if the phase happens to produce them.
- **`MBeanServerUtils.registerMBean` rethrowing** (`:22-23`) means the sweep failing silently is not a risk: the *next* registration of the same name throws. HARN-02's zero-`InstanceAlreadyExistsException` clause is enforced by the suite itself.

</code_context>

<specifics>
## Specific Ideas

- **The organising fact of this phase is that the ROADMAP over-estimated the work and under-estimated the risk.** Four of seven requirements (`HARN-04`/`05`/`06`/`07`) are already satisfied — the feared duplicate `ClickHouseTestHelpers.java` does not exist, `SchemaTestData`/`SchemalessTestData` are already single-copy, `compileIntegrationTestJava` already exits 0, and Testcontainers `2.0.5` already runs 645 tests green. What is *not* in any requirement is the thing this discussion found: the test suite has been silently running against the wrong ClickHouse version since the merge. The plan should say so plainly rather than presenting `HARN-04`/`05`/`06` as work.
- **The image-default reversion is the clearest example yet of why "green" is not the same as "correct".** Phases 8 and 9 both closed on a green full suite. Both were green against `25.8.18.1` while every planning document said `latest`. Nothing failed, nothing warned, and CI — which pins the right version — could never have caught it. This is the same failure shape as Phase 7's `targetTableFilter` (green `testUnit` on top of an entirely broken behaviour), one level up: a green *suite* on top of an entirely different server.
- **The proxy-class alias collision is a design risk, not a test flake.** Two containers claiming `"toxiproxy"` on one network is nondeterministic by construction — it depends on Gradle's fork assignment, which varies run to run. D-02 removes it by construction rather than by observation, which is why the opt-out seam is worth its fork delta.
- **D-07 knowingly inverts Phase 9's D-10 argument, and that is the right call here.** Phase 9 kept the parked test classes byte-identical *because* the unchanged diff was the proof the contract had not moved. Here the contract deliberately *does* move — the harness is the subject of the phase — so a byte-identical canary would be proving the wrong thing. The proof moves into the assertions, and D-11's commit split keeps the API redirection reviewable apart from the contract change.

</specifics>

<deferred>
## Deferred Ideas

- **Should `tests.yaml` stop setting `CLICKHOUSE_VERSION` explicitly** once D-04 makes the default correct? Keeping it is belt-and-braces; dropping it would make CI exercise the same default developers do, turning CI into a real check on the canary. `tests.yaml` is Phase 11's file — raise it there.
- **P4-WR-01 — `ClickHouseSinkTaskJsonInsertFormatTest`'s missing `try/finally`.** D-09a deliberately keeps the sweep permissive so this stays an accepted follow-up rather than a Phase 10 blocker. Still a Future Requirement; the sweep mitigates it, it does not fix it.
- **CR-01 — `.planning/scripts/verify-merge-invariants.sh:44` fails open** on a `git grep` error (exit ≥ 2 treated as "no match", stderr suppressed). Carried from Phase 6's security audit, still unowned, **must be fixed before Phase 11**, which gates publication on that script running unattended. Not Phase 10 work.
- **`/gsd-secure-phase 07` and `/gsd-secure-phase 09`** — both phases lack a `SECURITY.md` while `workflow.security_enforcement` is `true`. Due before Phase 11's publication gate, or recorded as a deliberate omission.
- **`maxParallelForks = availableProcessors() / 2` is untuned for the post-merge suite.** It resolves to 1 on a 2-core CI runner and to a large number on a developer laptop, and D-01 changes what a fork costs. Worth revisiting with a measured number in Phase 11, alongside the `timeout-minutes: 45` check — not worth guessing at here.
- **R-01's fail-fast fix** — still a Future Requirement, still unscheduled, unaffected by this phase.
- **Reopening `Column.extractColumn`'s swapped `hasDefaultValue`/`isSubColumn` args, the `USERNAME` ConfigDef default, and `JsonColumnTest`'s row-count-only assertions** — the three open Warning findings in `07-REVIEW.md`. Not harness work.

### Reviewed Todos (not folded)

`gsd_run query todo.match-phase 10` returned zero matches — no pending todos intersect this phase.

</deferred>

---

*Phase: 10-Test Harness Reconciliation*
*Context gathered: 2026-09-20*
