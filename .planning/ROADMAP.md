# Roadmap: ClickHouse Kafka Connect — `custom_build` Fork Test Coverage

## Overview

The work starts by making the existing `./gradlew test` suite green against ClickHouse `latest` with client V2. The test harness also needs to be fast and leak-safe, and JaCoCo should already produce a coverage report. Three test tracks then pin every fork addition. They depend only on that foundation: the four fork SMTs, table metadata and JSON/Dynamic types, and the explicit JSON insert path with connector plumbing. Any bug a track exposes is fixed inside that track with a small, rebase-friendly change, so there is no separate bug-fix phase. The last phase updates the fork's GitHub Actions workflow to a single `latest` × V2 job that publishes the coverage report, and confirms the whole suite is green locally and in CI.

Local note: every ClickHouse-backed test (anything extending `ClickHouseBase`) needs Docker via colima running.

## Phases

**Phase Numbering:**

- Integer phases (1, 2, 3): Planned milestone work
- Decimal phases (2.1, 2.2): Urgent insertions (marked with INSERTED)

Decimal phases appear between their surrounding integers in numeric order.

- [x] **Phase 1: Test Infrastructure & Green Baseline** - Green `latest` × V2 baseline, container singleton, `testUnit` task, leak-safe/UTF-8 test hygiene, JaCoCo wired to `test` (completed 2026-09-16)
- [x] **Phase 2: Fork SMT Coverage** - Edge cases, config contracts, and sink-level ClickHouse landing tests for all four fork SMTs (completed 2026-09-16)
- [x] **Phase 3: Table Metadata & JSON/Dynamic Types** - `targetTableFilter`, `describeTable` sub-column skipping, JSON/Dynamic mapping and round-trips, RowBinary fail-fast, non-ASCII (completed 2026-09-17)
- [ ] **Phase 4: JSON Insert Path & Connector Plumbing** - Explicit `insertFormat=json`, `jsonInsertColumns`, MBean naming, `async_insert` not forced
- [ ] **Phase 5: CI & Final Verification** - Modernized single-job CI on `latest` × V2 publishing coverage, full suite green locally and in CI

## Phase Details

### Phase 1: Test Infrastructure & Green Baseline

**Goal**: The existing suite passes against ClickHouse `latest` with client V2 on a harness that is fast (one container per JVM fork), can run unit tests without Docker, cannot leak MBeans between tests, decodes responses as UTF-8, and already produces a JaCoCo report so every later test shows up in coverage
**Depends on**: Nothing (first phase)
**Requirements**: INFRA-01, INFRA-02, INFRA-03, INFRA-04, INFRA-05, INFRA-06, COV-01, COV-02
**Success Criteria** (what must be TRUE):

  1. With colima running and no `CLICKHOUSE_VERSION` override, `./gradlew test` runs against ClickHouse `latest` with client V2 and passes in full. The first baseline result, including any pre-existing failures and how they were fixed, is recorded in the phase summary.
  2. A smoke test fails if the running ClickHouse server version does not match the requested image. A full ClickHouse-backed run starts one ClickHouse container per test JVM fork, not one per test class.
  3. With Docker stopped, `./gradlew testUnit` passes and runs every test not tagged as ClickHouse-backed, including the existing SMT tests.
  4. Sink-task tests stop their task in `finally`/`@AfterEach` and use unique connector names, so a deliberately failing test does not cause an MBean-already-registered failure in later tests. Test helpers decode ClickHouse responses as explicit UTF-8.
  5. After `./gradlew test`, JaCoCo HTML and XML reports exist, Lombok-generated methods of `Column` are excluded, and fork-changed classes can be reviewed on their own. `integrationTest` and `check` do not run JaCoCo.

**Plans**: 3/3 plans executed

Plans:
**Wave 1**

- [x] 01-01-PLAN.md — Baseline run on `latest` × V2 (untouched harness), `latest` as default image, server-version smoke test, fix pre-existing failures (wave 1)

**Wave 2** *(blocked on Wave 1 completion)*

- [x] 01-02-PLAN.md — Test-source harness: `ClickHouseBase` container singleton with `alwaysPull`, per-class database, proxy container isolation, `@Tag("clickhouse")`, MBean sweep + unique connector names, UTF-8 helpers (wave 2)

**Wave 3** *(blocked on Wave 2 completion)*

- [x] 01-03-PLAN.md — Build script: JaCoCo `0.8.15` on `test` only (full + fork-changed reports, `lombok.config`), `testUnit` kept out of `check`, full covered run on the new harness (wave 3: needs 01-02's tag and harness)

### Phase 2: Fork SMT Coverage

**Goal**: Every behavior of `TableOperationFilter`, `HeadersToValue`, `HeaderToValue`, and `ConvertToMapOrJson` is pinned: edge cases, config validation, record metadata preservation, and records reaching ClickHouse through the sink after transformation
**Depends on**: Phase 1
**Requirements**: TOF-01, TOF-02, TOF-03, TOF-04, TOF-05, TOF-06, HDRS-01, HDRS-02, HDRS-03, HDRS-04, HDRS-05, HDRS-06, HDRS-07, HDRS-08, HDRS-09, HDR-01, HDR-02, HDR-03, HDR-04, HDR-05, HDR-06, HDR-07, CONV-01, CONV-02, CONV-03, CONV-04, CONV-05, CONV-06, CONV-07, CONV-08
**Success Criteria** (what must be TRUE):

  1. `./gradlew testUnit` passes with `TableOperationFilter` tests for:
     - truncate drops
     - multi-table filtering
     - invalid, empty, whitespace, and `r` tokens ignored (warn-and-ignore pinned)
     - `op=r` and schemaless envelopes passing through
     - `config()` and repeated `close()`
  2. `HeadersToValue` and `HeaderToValue` tests pass on both schema and schemaless paths for:
     - tombstones
     - missing, null, duplicate, multi-valued, and non-string headers
     - `DataException` on bad config
     - the pinned field-collision behavior
     - an added optional string field
     - preserved topic, partition, key, timestamp, and headers
  3. `ConvertToMapOrJson` tests pass for:
     - null-field omission with Jackson and Gson
     - Structs nested in Array and Map fields
     - non-Struct passthrough
     - `ConfigException` for an invalid JSON library
     - direct and sliced `ByteBuffer` base64 matching array-backed output
     - preserved key, key schema, timestamp, and headers
  4. On ClickHouse `latest`, one sink-level test per SMT passes:
     - filtered Debezium operations never reach the table while unfiltered rows do
     - headers land in `Map(String, Nullable(String))`
     - a header-derived field lands in its column
     - JSON-string output lands in a String/JSON column with correct content
  5. Any bug these tests expose (for example nested Struct conversion) is fixed in this phase with a small targeted change, and `./gradlew test` stays green.

**Plans**: 5/5 plans executed

Plans:
**Wave 1**

- [x] 02-01-PLAN.md — Shared `TransformTestHelpers` builders + `TableOperationFilter` unit coverage (TOF-01..05) and the D-03 `configure()` clear (wave 1)
- [x] 02-05-PLAN.md — Sink-level SMT landing tests in a new ClickHouse-backed `SmtSinkLandingTest` (TOF-06, HDRS-09, HDR-07, CONV-08) (wave 1: builds its own fixtures, depends on nothing)

**Wave 2** *(blocked on 02-01's shared test helper)*

- [x] 02-02-PLAN.md — `HeaderToValue` unit coverage (HDR-01..06) + the D-02 stale-schema guard (wave 2)
- [x] 02-03-PLAN.md — `HeadersToValue` unit coverage (HDRS-01..08) + the D-02 stale-schema guard + the D-14 HDRS-04 wording correction (wave 2)
- [x] 02-04-PLAN.md — `ConvertToMapOrJson` unit coverage (CONV-01..07) + the D-01 nested-collection recursion fix (wave 2)

*Split note: the roadmap's original 3-plan sketch put all five SMT concerns in three wave-1 plans. Planning split them five ways because (a) D-12's shared `TransformTestHelpers` is created once in 02-01 and consumed by 02-02/02-03/02-04, which is a real ordering dependency, and (b) one plan per SMT keeps each executor inside its context budget. Wave 1 still runs two plans in parallel and wave 2 runs three.*

### Phase 3: Table Metadata & JSON/Dynamic Types

**Goal**: Table discovery and column typing added by the fork work correctly on ClickHouse `latest`. `targetTableFilter` selects the intended tables, `describeTable` never drops tables because of sub-columns, `JSON`/`Dynamic` columns round-trip real data, and the RowBinary path fails clearly instead of silently for those types.
**Depends on**: Phase 1
**Requirements**: FILT-01, FILT-02, FILT-03, FILT-04, DESC-01, DESC-02, DESC-03, DESC-04, TYPE-01, TYPE-02, TYPE-03, TYPE-04, TYPE-05, TYPE-06, TYPE-07
**Research flags**:

- Before writing DESC-03, capture raw `DESCRIBE TABLE ... SETTINGS describe_include_subcolumns=1` output on `latest` for `Nullable`, `Array`, `JSON`, and `Dynamic` columns and compare it with the skip list.
- Before choosing the TYPE-06 fix, run a ground-truth test of what currently happens when `JSON`/`Dynamic` columns go through RowBinary (crash, silent drop, or wrong data).

**Success Criteria** (what must be TRUE):

  1. `targetTableFilter` tests pass:
     - an empty filter adds no `match()` clause and maps all tables
     - a filter matching nothing yields an empty mapping, and the connector follows `suppressTableExistenceException`
     - a filter with single quotes builds a valid clause (unit test) and runs live
     - RE2 patterns with `\d` and anchors select the intended tables on live `latest`
  2. On `latest`, `describeTable` returns correct tables with `Nullable`, `Array`, and `JSON` columns without leaking `.null`, `.size0`, or JSON-internal sub-columns. Any suffix the audit finds unhandled is fixed. An unmappable column still drops the table (pinned) and is logged at ERROR.
  3. `JSON`, parameterized `JSON(...)`, and `Dynamic` type strings map to `Type.JSON`/`Type.DYNAMIC`. Records inserted through the JSON insert path read back correctly: nested values and types in `JSON` columns, `Dynamic` columns, and mixed tables with ordinary columns.
  4. A schema record targeting a `JSON` or `Dynamic` column through RowBinary fails fast with an error saying `insertFormat=json` is required, with no silent data loss.
  5. Cyrillic and CJK values round-trip through String, `JSON`, and `Dynamic` columns and through SMT header values under the `windows-1252` test JVM.

**Plans**: 3/3 plans executed

Plans:
**Wave 1** *(all three run in parallel: 03-01 owns `ClickHouseHelperClient`, 03-03 owns `ClickHouseWriter`, 03-02 touches no main code)*

- [x] 03-01-PLAN.md — `buildTableFilterClause` + `shouldSkipDescribeRow` extraction, warn→error, the conditional UTF-8 reader fix, and unit + live coverage for FILT-01..04, DESC-01..04 and the TYPE-07 identifier half (wave 1)
- [x] 03-02-PLAN.md — `JSON`/`Dynamic` type-string pins and typed round-trips on both D-13 insert routes, mixed table, and the TYPE-07 values half (TYPE-01..05, TYPE-07) (wave 1)
- [x] 03-03-PLAN.md — RowBinary fail-fast in `ClickHouseWriter`: the `doWriteColValue` throw, the `validateDataSchema` gate, and unit + live proof of the actionable message (TYPE-06) (wave 1)

*Scope note: TYPE-07 is split across two plans. Its identifier half (non-ASCII column names and `Enum` members) lands in 03-01 because those bytes only ever pass through the two `describeTable` readers that plan already owns; its values half (Cyrillic and CJK through String/`JSON`/`Dynamic` columns and SMT header values) lands in 03-02. That split is what keeps `ClickHouseHelperClient.java` exclusive to one plan so all three stay in wave 1.*

### Phase 4: JSON Insert Path & Connector Plumbing

**Goal**: The work-in-progress metadata-free JSON insert path and `jsonInsertColumns` are pinned as they are today, and connector-level changes are covered: MBean naming, per-task registration, and `async_insert` left to the user
**Depends on**: Phase 1
**Requirements**: JSON-01, JSON-02, JSON-03, JSON-04, JSON-05, COLS-01, COLS-02, COLS-03, COLS-04, COLS-05, COLS-06, JMX-01, JMX-02, JMX-03, JMX-04, ASYNC-01, ASYNC-02, ASYNC-03
**Research flags**:

- ~~Before writing COLS-02, check how client-v2 `0.9.6` handles an empty column list: omitted column clause, malformed INSERT, or something else.~~ **Withdrawn** by CONTEXT.md D-11 — Phase 3's `JsonDynamicRoundTripTest` round-trips already drove `doInsertJsonV2` with the default empty list and are green. COLS-02 is a plain confirmatory live test.
- R-01 (what `insertFormat=json` + a Struct record + `bypassRowBinary` unset actually does) was **resolved empirically** during research: silent data loss, `Written Rows: [0]`, no exception. Per D-01/D-07 the fail-fast fix is not pre-approved; plan 04-01 surfaces it at a blocking-human decision checkpoint.

**Success Criteria** (what must be TRUE):

  1. `insertFormat=json` in any letter case enables metadata-free mode, and leaving it unset does not. With topic name different from table name, the connector starts and inserts without fetched table metadata: unknown fields are sent unmodified, known fields land with correct values, and tombstones are skipped. Existing explicit-JSON tests now assert column values.
  2. `jsonInsertColumns` tests pass:
     - parsing: blank means an empty list; whitespace and empty entries are dropped; order is preserved
     - the default empty list inserts correct values (fixed if broken)
     - an explicit subset fills listed columns and gives unlisted columns their `DEFAULT`
     - extra JSON keys do not break the insert
     - the list also applies on the non-explicit JSON path
  3. A `jsonInsertColumns` entry naming a column that does not exist produces a clear error, with no partial insert and no NPE.
  4. MBean tests pass:
     - `connector=<name>` when the name prop is set
     - `connector=null` without it
     - `version=unknown` for a null version, and `:` rendered as `_`
     - multiple tasks and connectors register distinct MBeans, and each `stop()` unregisters only its own
  5. With default config the connector sends no `async_insert` setting. A user-supplied `async_insert` in `clickhouseSettings` is passed through unchanged. A sink insert with `async_insert=1` completes and its data is present in ClickHouse `latest`.

**Plans**: 1/3 plans executed

Plans:
**Wave 1** *(all three run in parallel: 04-03 is the only plan touching main code and owns `ProxySinkTask.java`; the other two touch no main code, and no test file is written by two plans — CONTEXT.md D-16)*

- [x] 04-01-PLAN.md — Explicit `insertFormat=json` path: a new live class (JSON-01 differential, JSON-02, JSON-03 live half, JSON-04 tombstones), a Docker-free `cleanupExtraFields`/letter-case class, typed value assertions added to the existing explicit-JSON tests (JSON-05), and a blocking-human checkpoint on R-01's silent data loss (wave 1, not autonomous)
- [ ] 04-02-PLAN.md — `jsonInsertColumns`: a Docker-free parsing class (COLS-01) and a new live class covering the default empty list, an explicit subset against non-trivial `DEFAULT`s, extra payload keys, the nonexistent-column error plus its retriable/non-retriable classification, and the non-explicit path's `cleanupExtraFields` interaction (COLS-02..06) (wave 1)
- [ ] 04-03-PLAN.md — Connector plumbing: the pre-approved `ProxySinkTask.buildMBeanName` extraction plus a Docker-free name class (JMX-01..03) and a live four-task registration class (JMX-04), and `async_insert` config pins plus a live `wait_for_async_insert=1` insert (ASYNC-01..03) (wave 1)

*Scope note: the plan split matches the roadmap's original 3-plan sketch. `ASYNC-01..03` stays inside 04-03 rather than becoming a fourth plan because its two new test classes are small and a fourth plan would cost an extra executor cold start — worktree isolation is currently degraded to sequential execution while `custom_build` has unpushed commits (STATE.md).*

### Phase 5: CI & Final Verification

**Goal**: The fork's CI runs the full test suite on every push against ClickHouse `latest` × client V2 using current actions and publishes coverage, and the whole suite is confirmed green both locally and in CI
**Depends on**: Phase 2, Phase 3, Phase 4
**Requirements**: CI-01, CI-02, CI-03, CI-04
**Success Criteria** (what must be TRUE):

  1. `.github/workflows/tests.yaml` uses `actions/checkout@v4`, `actions/setup-java@v6` (temurin 17), and `gradle/actions/setup-gradle@v6`, with no deprecated actions left.
  2. A push to `custom_build` triggers exactly one test job, using `CLICKHOUSE_VERSION=latest` and client V2, with no V1, 23.7, 24.3, or Cloud legs.
  3. That CI run is green, uploads the JaCoCo report as an artifact, and writes a coverage summary to the job summary. No coverage threshold can fail the build.
  4. With colima running, `./gradlew test` passes in full locally (fork and upstream tests) against `latest` × V2, and the final report shows coverage for the fork-changed classes.

**Plans**: 2 plans

Plans:

- [ ] 05-01: Update the workflow: current actions, single `latest` × V2 job, JaCoCo artifact upload + job-summary coverage (wave 1)
- [ ] 05-02: Final full-suite run locally, coverage report review, confirm the green CI run on push (`gh` not installed; check via GitHub web/API) (wave 2)

## Progress

**Execution Order:**
Phases execute in numeric order: 1 → 2 → 3 → 4 → 5. Phases 2, 3, and 4 depend only on Phase 1 and do not depend on each other.

| Phase | Plans Complete | Status | Completed |
|-------|----------------|--------|-----------|
| 1. Test Infrastructure & Green Baseline | 3/3 | Complete    | 2026-09-16 |
| 2. Fork SMT Coverage | 5/5 | Complete    | 2026-09-16 |
| 3. Table Metadata & JSON/Dynamic Types | 3/3 | Complete    | 2026-09-17 |
| 4. JSON Insert Path & Connector Plumbing | 1/3 | In Progress|  |
| 5. CI & Final Verification | 0/2 | Not started | - |
