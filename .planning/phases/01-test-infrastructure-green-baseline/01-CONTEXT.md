# Phase 1: Test Infrastructure & Green Baseline - Context

**Gathered:** 2026-09-15
**Status:** Ready for planning

<domain>
## Phase Boundary

The existing `./gradlew test` suite passes against ClickHouse `latest` with client V2, on a harness that:
- starts one shared ClickHouse container per test JVM fork
- runs every non-ClickHouse test through `testUnit` without Docker
- cannot leak sink-task MBeans between tests
- decodes ClickHouse responses in test helpers as UTF-8
- already produces JaCoCo HTML/XML reports, with a separate fork-focused report, after `test`

Requirements: INFRA-01..06, COV-01, COV-02.

Out of this phase:
- new fork-feature tests (Phases 2–4)
- CI workflow changes (Phase 5)
- main-code UTF-8 fixes in `describeTable` (Phase 3)

</domain>

<decisions>
## Implementation Decisions

### Baseline failure policy (INFRA-01)
- **D-01:** Run the untouched suite first with `CLICKHOUSE_VERSION=latest`, `CLIENT_VERSION=V2`, and record the raw result before changing the harness.
  - If an existing test fails only because its expectation is outdated for `latest` (for example type rendering, error text, or defaults) and the connector is correct, **update the assertion to match `latest`**.
  - This is a test-only change. Record every such change, with before/after and the reason, in the phase summary.
- **D-02:** If a baseline failure comes from a genuine bug in upstream (non-fork) main code, **make a small targeted main-code fix**.
  - "Small" means a few lines, rebase-friendly, and mirroring the upstream fix if one exists.
  - Anything larger **stops for a user decision**. Do not disable the test and move on.
- **D-03:** **Leave every existing `@Disabled` test as-is.** That covers:
  - the 2 upstream Variant tests in `ClickHouseSinkTaskWithSchemaTest` ("requires a flag on the instance")
  - the proxy Variant test
  - the `JsonColumnTest` case, which belongs to Phase 3
  - the perf tests (`InsertFormatPerformanceTest`, `ConvertToMapOrJsonPerformanceTestJson`)

  Do not try to re-enable them in this phase.
- **D-04:** Flaky tests (timing, `Thread.sleep`, colima resource pressure):
  - rerun to confirm the flake, then **fix the root cause**, e.g. polling instead of sleeping, or lowering the fork count if resources are the cause
  - **No test-retry plugin.** JaCoCo stays the only new build dependency.
  - Do not `@Disabled` a flaky test to get green.

### `latest` image, version smoke test, proxy tests, isolation (INFRA-02, INFRA-03)
- **D-05:** Change the default image to `latest` (`CLICKHOUSE_VERSION_DEFAULT` in `ClickHouseTestHelpers`). Keep the `CLICKHOUSE_VERSION` env override.
  - The shared ClickHouse container uses **`PullPolicy.alwaysPull()`**, so local `latest` matches CI.
  - `clickhouse/clickhouse-server:latest` is not cached locally today; only `25.3` and `23.8` are.
- **D-06:** Server-version smoke test:
  - **Numeric tag:** `SELECT version()` must start with the tag.
  - **`latest`:** `version()` must **equal the version label/metadata of the pulled image**.
  - The test logs the actual server version so the baseline summary can record it.
  - The smoke test covers the shared container only.
  - *Research:* check whether the ClickHouse image carries a usable version label (e.g. `org.opencontainers.image.version`). If it doesn't, propose the closest equivalent check.
- **D-07:** The two Toxiproxy classes (`ClickHouseSinkTaskWithSchemaProxyTest`, `ClickHouseSinkTaskSchemalessProxyTest`) **stay on ClickHouse 23.8**, as a documented exception to "`latest` only".
  - Reason: ClickHouse issue #58828 (absolute-form URIs through Toxiproxy) is still open.
  - Give them **their own container field and lifecycle**. They must never assign, overwrite, or stop the shared `latest` singleton.
  - Today both assign the static `db` in their own `@BeforeAll`, and the base `@AfterAll` stops it. That is the conflict to remove.
  - They are still ClickHouse-backed and tagged (D-12).
- **D-08:** **Fresh database per test class** on the shared container.
  - `ClickHouseBase` creates a unique database in `@BeforeAll` and drops it in `@AfterAll`, replacing today's single static `kafka_connect_test_<ts>`.
  - Reason: fixed table names are reused across classes that will share a container (`special-char-table-test`, `mapping_table_test`, `table_mapping_test`, `array_string_table_test`), and later table-mapping assertions (e.g. FILT-01) must only see their own class's tables.
  - No upstream table renames.

### Lifecycle retrofit scope (INFRA-04, INFRA-05, INFRA-06)
- **D-09:** **Base-class safety net plus a rule for new tests.** Existing upstream test files are not retrofitted.
  - An `@AfterEach` in `ClickHouseBase` unregisters any leftover `com.clickhouse:type=ClickHouseKafkaConnector,*` MBeans and logs each one.
  - New and touched tests must wrap `start`/`put`/`stop` in try/finally (or `@AfterEach`) and use a unique connector name.
  - A deliberate-failure test proves that a test failing between `start()` and `stop()` leaves no stale MBean for later tests.
  - Context: MBean ids come from a static `AtomicInteger` (`ProxySinkTask.java:31`), so leaks linger rather than crash. Stale registrations would still distort Phase 4 JMX count assertions. `ClickHouseSinkTaskTest` already has 2 `start()` calls and 1 `stop()`.
- **D-10:** **Opt-in unique connector-name helper** in `ClickHouseBase`, e.g. class name + method name + counter, that new tests call.
  - `createProps()` stays unchanged, so existing tests keep their current MBean names.
  - Phase 4's JMX-02 test (`connector=null` when the `name` prop is absent) needs no workaround.
- **D-11:** **UTF-8 now in test helpers only.**
  - Change: `ClickHouseTestHelpers.java:158` (`getAllRowsAsJson`) and `:252` (`validateRows`) get explicit `StandardCharsets.UTF_8`.
  - Not changed in Phase 1: the main-code readers `ClickHouseHelperClient.java:370` (fork commit `648c3cc`) and `:417` (upstream `60b0fe6`). They get fixed in Phase 3 when the TYPE-07/DESC non-ASCII tests prove the bug.
- **D-12:** **A single `@Tag("clickhouse")` on `ClickHouseBase`.**
  - JUnit's `@Tag` is `@Inherited`, so all 14 current subclasses (proxy classes included) and future ones are covered without editing upstream test files.
  - `testUnit` excludes this tag.
  - Nothing outside `ClickHouseBase` subclasses starts a Testcontainer today.

### Fork-changed coverage view (COV-01, COV-02)
- **D-13:** The fork-focused view uses a **hand-picked, named include list** in `build.gradle.kts`, mapped to the 11 fork additions. Globs include inner classes, e.g. `Column*`. Update the list when the fork adds classes.
  - Transforms (`com/clickhouse/kafka/connect/transforms/`): `TableOperationFilter`, `HeadersToValue`, `HeaderToValue`, `HeaderToValueConfig`, `ConvertToMapOrJson`
  - `com/clickhouse/kafka/connect/sink/db/helper/ClickHouseHelperClient`
  - `com/clickhouse/kafka/connect/sink/db/mapping/`: `Column`, `Type`, `Table`
  - `com/clickhouse/kafka/connect/sink/db/ClickHouseWriter`
  - `com/clickhouse/kafka/connect/sink/`: `ClickHouseSinkConfig`, `ProxySinkTask`, `ClickHouseSinkTask`
  - `com/clickhouse/kafka/connect/ClickHouseSinkConnector`
  - `com/clickhouse/kafka/connect/util/jmx/MBeanServerUtils`

  Rejected: deriving the list from `git diff main...custom_build` (import-only noise; no `main` ref on shallow CI checkouts), and package globs (pull in upstream-only `KeyToValue`, `ExtractTopic`, `ClickHouseFieldDescriptor`).
- **D-14:** **A second `JacocoReport` task** reads the same `test` exec data, filtered to D-13, and writes its own HTML+XML (e.g. `build/reports/jacoco/fork/`).
  - It runs automatically after `test`, next to the full `jacocoTestReport`.
  - Phase 5 CI can publish or summarize its XML directly.
- **D-15:** **Add a root `lombok.config`** with `config.stopBubbling = true` and `lombok.addLombokGeneratedAnnotation = true`.
  - JaCoCo then filters generated getters, setters, and builders via `@lombok.Generated`.
  - Upstream has no `lombok.config`, so there is no rebase conflict.
  - The shipped jar only gains a class-retention annotation; no runtime change.
  - Verify by looking at `Column` in the generated report.
- **D-16:** JaCoCo toolVersion `0.8.15`. The agent runs on the **`test` task only**.
  - Explicitly disable the agent on `testUnit` and `integrationTest`.
  - **Exclude `testUnit` from `check`.** It is a subset of `test`; the existing `tasks.withType<Test>` loop in `build.gradle.kts` would otherwise add it.
  - Wire the reports to `test` (finalizedBy), never to `check`.
  - Leave the pre-existing `check` → `integrationTest` dependency from that loop untouched (upstream, out of scope).

### Claude's Discretion
- Shared-container cleanup: rely on Ryuk or add a JVM shutdown hook. Pick whatever works reliably with colima.
- Exact names: the unique connector-name helper, the fork report task (`jacocoForkReport` or similar), the per-class database naming scheme, and the report output directory.
- Where the version smoke test lives: a new tagged class, or a test in an existing base-derived class.
- How the proxy classes get their own container field: separate static field plus an overridden `createProps`/teardown, or a small base hook. Keep the diff minimal.
- How `testUnit` gets its test classpath and excludes the tag, and how it is kept out of `check` in the Kotlin DSL.
- Whether to lower `maxParallelForks` locally if colima resource pressure causes flakes (D-04). Record the evidence in the summary.

</decisions>

<canonical_refs>
## Canonical References

**Downstream agents MUST read these before planning or implementing.**

### Scope and requirements
- `.planning/ROADMAP.md` § "Phase 1: Test Infrastructure & Green Baseline": goal, the 5 success criteria, and the plan split 01-01 / 01-02 / 01-03 with waves
- `.planning/REQUIREMENTS.md` § Test Infrastructure (INFRA-01..06), § Coverage (COV-01, COV-02), § Out of Scope
- `.planning/PROJECT.md` § Constraints, § Key Decisions, § Context (test layout, CI today, known risks)

### Research (patterns and pitfalls this phase implements)
- `.planning/research/ARCHITECTURE.md`:
  - Pattern 1: container-per-fork singleton
  - Pattern 2: JUnit tags + `testUnit`
  - Pattern 4: JaCoCo on `test` only, not `check`
  - Anti-Pattern 3: wiring coverage to `check`
- `.planning/research/PITFALLS.md`:
  - Pitfall 1: `CLICKHOUSE_VERSION` defaults to 25.3
  - Pitfall 5: MBean leak / no try/finally
  - Pitfall 7: `windows-1252` test encoding
  - Pitfall 9: Lombok coverage dilution
  - Integration Gotchas: Testcontainers + colima
- `.planning/research/STACK.md`: JaCoCo `0.8.15` on Gradle 8.14 (default `0.8.13`), `jacocoForkReport` config sketch. Note its `transforms/**` glob is wrong for main classes; the correct package is `com/clickhouse/kafka/connect/transforms/`.
- `.planning/codebase/TESTING.md`: current test framework, setup/teardown patterns, `build.gradle.kts` test config

### External
- `https://github.com/ClickHouse/ClickHouse/issues/58828`: "does not support URI with absolute-form" (Toxiproxy). Still open as of 2026-09-15, so the proxy tests stay on 23.8 (D-07).

</canonical_refs>

<code_context>
## Existing Code Insights

### Reusable Assets
- `src/test/java/com/clickhouse/kafka/connect/sink/ClickHouseBase.java`: the only place to change the container lifecycle, per-class database, `@Tag`, MBean `@AfterEach` cleanup, and the connector-name helper.
  - Static `db` and `database` fields
  - `@BeforeAll setup()` starts a container for every class
  - `@AfterAll tearDown()` stops it
  - `createProps()`, `createClient(props)` (creates and switches to `database`), `createTopicName()`
- `src/test/java/com/clickhouse/kafka/connect/sink/helper/ClickHouseTestHelpers.java`: `CLICKHOUSE_VERSION_DEFAULT = "25.3"` (line 41), `CLICKHOUSE_PROXY_VERSION_DEFAULT = "23.8"`, `CLICKHOUSE_DOCKER_IMAGE`, and the default-charset readers at lines 158 and 252.
- `src/test/java/com/clickhouse/kafka/connect/sink/junit/extension/FromVersionConditionExtension.java` plus `SinceClickHouseVersion`: already treats an unset `CLICKHOUSE_VERSION` as `latest`, which becomes consistent with the image once D-05 flips the default.
- `src/main/java/com/clickhouse/kafka/connect/util/jmx/MBeanServerUtils.java`: `unregisterMBean(name)` is safe for the base cleanup. `registerMBean` throws on `InstanceAlreadyExistsException`.
- `src/main/java/com/clickhouse/kafka/connect/sink/ProxySinkTask.java:71-83`: MBean name format `com.clickhouse:type=ClickHouseKafkaConnector,connector=%s,name=SinkTask%d,version=%s`, unregistered in `stop()`.

### Established Patterns
- Unit tests and ClickHouse-backed tests are split by inheritance, not by directory. Pure unit tests:
  - `src/test/java/transforms/*`
  - `sink/{util,processing,provider,kafa}/*Test`
  - `sink/db/mapping/ColumnTest`
- All 14 Docker-using classes extend `ClickHouseBase`:
  - `ClickHouseSinkJdbcPropertiesTest`, `ClickHouseSinkTaskJsonInsertFormatTest`, `ClickHouseSinkTaskMappingTest`
  - `ClickHouseSinkTaskSchemalessProxyTest`, `ClickHouseSinkTaskSchemalessTest`, `ClickHouseSinkTaskStringTest`, `ClickHouseSinkTaskTest`
  - `ClickHouseSinkTaskWithSchemaProxyTest`, `ClickHouseSinkTaskWithSchemaTest`
  - `InsertFormatPerformanceTest`, `JsonColumnTest`
  - `db/ClickHouseWriterTest`, `db/helper/ClickHouseHelperClientTest`, `db/mapping/TableTest`
- Table naming in tests: 65 fixed names vs 66 `createTopicName(...)`; most tests drop before create.
- The only server-level state change is `CREATE USER IF NOT EXISTS unflatten` + `GRANT` in `ClickHouseHelperClientTest`, which can run repeatedly.
- Build (`build.gradle.kts`):
  - `tasks.withType<Test>` sets `maxParallelForks = cores/2`, `file.encoding=windows-1252`, the JUnit Platform, the Java 17 launcher, and `check.dependsOn(this)` for every Test task
  - `integrationTest` is registered with `tasks.create`
  - Lombok `1.18.42` is `compileOnly`/`annotationProcessor`; there is no `lombok.config`
- Lombok in D-13 classes: `Column` (`@Builder`, `@Getter`, `@Setter`, `@Accessors`), `Table` (`@Getter`, `@Setter`, `@Accessors`), `ClickHouseHelperClient` (`@Getter`), `ClickHouseSinkConfig` (`@Getter`).

### Integration Points
- `build.gradle.kts`: `plugins { jacoco }`, `jacoco { toolVersion }`, `jacocoTestReport` reports, the fork `JacocoReport` task, the `testUnit` Test task, and JaCoCo extension disablement on `testUnit`/`integrationTest`.
- Repo root: a new `lombok.config`.
- `ClickHouseSinkTaskWithSchemaProxyTest` / `ClickHouseSinkTaskSchemalessProxyTest` `@BeforeAll`: the only upstream test files that must change (D-07 container isolation), plus any tests edited under D-01.

</code_context>

<specifics>
## Specific Ideas

Environment facts found during discussion (planner/executor should rely on these, not STATE.md's older note):
- **Docker is available locally.**
  - colima profile `vz` is **running** (aarch64, 8 CPU / 16 GiB) and `DOCKER_HOST=unix:///Users/alexandr.chashnikov/.colima/vz/docker.sock`. The `default` profile is the one that is stopped, so the STATE.md "colima not running" blocker is out of date.
  - Ryuk images (`0.11.0`, `0.12.0`) are cached, so Testcontainers has worked on this machine before.
  - There is no `~/.testcontainers.properties`.
- **The colima `vz` VM also runs a local Airflow dev stack** (several containers, some unhealthy/restarting) and Portainer. Expect resource contention with `maxParallelForks = cores/2` when judging flakiness (D-04).
- **Tooling drift from `.claude/CLAUDE.md`:**
  - the Gradle wrapper is **8.14** (not 7.4.2)
  - the JUnit BOM is **6.0.2** (the `junit-jupiter-api:5.11.3` line is also present)

  Use the real versions.
- **The baseline summary must include:**
  - the raw first-run result (pass/fail/skip counts, failing tests)
  - the actual `latest` server version from the smoke test
  - every test updated under D-01 and every fix under D-02

</specifics>

<deferred>
## Deferred Ideas

- **Explicit UTF-8 in main-code `describeTable` readers** (`ClickHouseHelperClient.java:370`, `:417`): Phase 3, once the TYPE-07/DESC non-ASCII tests prove the bug.
- **Re-enabling upstream Variant `@Disabled` tests on `latest`:** not a fork addition. Left as-is; could be a future upstream-sync item.
- **Pre-existing `check` → `integrationTest` dependency** (via the `tasks.withType<Test>` loop): upstream quirk, left untouched this milestone.
- **Moving the Toxiproxy tests to `latest`:** blocked by ClickHouse #58828. Revisit when that issue is fixed.

</deferred>

---

*Phase: 01-test-infrastructure-green-baseline*
*Context gathered: 2026-09-15*
