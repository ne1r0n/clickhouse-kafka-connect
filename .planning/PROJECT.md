# ClickHouse Kafka Connect — `custom_build` Fork Test Coverage

## What This Is

The `custom_build` branch of `ne1r0n/clickhouse-kafka-connect` (version `v1.2.9_gg4`) is a fork of the official ClickHouse Kafka Connect sink, carrying custom SMTs, table filtering, JSON/Dynamic column support, a metadata-free JSON insert path, and JMX naming changes used in CDC (Debezium) pipelines into ClickHouse. This project audits the existing tests for everything the fork added on top of upstream, writes the missing unit and integration tests, and fixes any bugs those tests uncover — ending with a fully green suite, a coverage report, and CI running it.

## Core Value

Every behavior the fork adds on top of upstream is pinned by an automated test that passes against ClickHouse `latest` with client V2, so the fork can be changed or rebased without silently breaking production pipelines.

## Requirements

### Validated

<!-- Existing capabilities, inferred from the codebase map and `git diff main...custom_build`. -->

**Upstream connector (inherited):**

- ✓ Sink Kafka records into ClickHouse tables from schema (Struct), schemaless (Map), and String JSON records — existing
- ✓ At-least-once (`InMemoryState`) and exactly-once (`KeeperStateProvider`) offset-range deduplication — existing
- ✓ Insert formats (RowBinary, JSON, CSV/TSV) with type mapping including Map/Array/Tuple/Variant/Decimal/DateTime64 — existing
- ✓ Topic-to-table mapping, error tolerance with DLQ routing — existing
- ✓ Upstream SMTs `KeyToValue`, `ExtractTopic` — existing

**Fork additions (the subject of this project):**

- ✓ `TableOperationFilter` SMT: drops chosen Debezium operations (`c`/`u`/`d`/`t`) for listed `database.table`s — existing (fork)
- ✓ `HeadersToValue` SMT: copies all Kafka headers into a single map field of the value (schema and schemaless) — existing (fork)
- ✓ `HeaderToValue` SMT (+ `HeaderToValueConfig`): copies selected headers into value fields, including multi-valued headers — existing (fork)
- ✓ `ConvertToMapOrJson` SMT: converts Struct values to Map or JSON string (Jackson/Gson, bytes → base64 options) — existing (fork)
- ✓ `targetTableFilter`: regex restricting table metadata discovery, applied server-side via `match(table, '<regex>')` with quote escaping in `ClickHouseHelperClient` — existing (fork)
- ✓ `JSON(...)` parameterized columns and `Dynamic` columns recognized in `Column`/`Type` mapping — existing (fork)
- ✓ `describeTable` streams rows and skips system sub-columns (`.null`, `.size0`, `.object_shared_data.`) — existing (fork)
- ✓ Explicit `insertFormat=json` skips table discovery, validation, and field cleanup; inserts `JSONEachRow` directly using topic-to-table mapping — existing (fork, WIP)
- ✓ `jsonInsertColumns`: comma-separated explicit column list passed to JSON inserts — existing (fork, WIP)
- ✓ JMX MBean name includes `connector=<name>` (from connector `name` prop), with version sanitized (`:` → `_`, `null` → `unknown`) — existing (fork)
- ✓ Connector no longer forces `async_insert=0`; async insert is controlled by server profile / `clickhouseSettings` — existing (fork, intentional)

**Delivered by this project:**

- ✓ Green baseline: `./gradlew test` passes in full against ClickHouse `latest` × client V2 (194 tests, 0 failed, 7 skipped) on a harness with one ClickHouse container per test JVM fork, per-class databases, an MBean leak sweep, UTF-8 test helpers, a server-version smoke test, and a Docker-free `testUnit` task — Phase 1
- ✓ JaCoCo coverage report generated for the test run (HTML + XML), with a focused report on the fork-changed classes and Lombok-generated code excluded — Phase 1
- ✓ `TableOperationFilter` gaps covered: truncate (`t`) operation, multiple tables, invalid/empty `skipped.operations`, whitespace handling — Phase 2
- ✓ `HeadersToValue` covered: null value, no headers, null header values, field name collision with existing value field, missing/blank `field` config, schema and schemaless paths — Phase 2
- ✓ `HeaderToValue` covered: multi-valued headers, missing headers, config parsing edge cases — Phase 2
- ✓ `ConvertToMapOrJson` existing tests reviewed; remaining gaps (null/optional fields, nested arrays/maps) covered — Phase 2
- ✓ At least one sink-level test per fork SMT showing transformed records land correctly in ClickHouse — Phase 2
- ✓ Three stale-state SMT bugs fixed and pinned: `TableOperationFilter.configure()` unioning across repeated calls (D-03), `HeaderToValue`/`HeadersToValue` serving a stale cached `valueSchema` after a source-schema change (D-02), `ConvertToMapOrJson.convertFieldValue()` not recursing into List/Map values (D-01) — Phase 2
- ✓ `ClickHouseWriter` MAP-branch RowBinary null-marker bug fixed: `Map(K, Nullable(V))` columns were silently unwritable on the client-V2 path — Phase 2 (approved out-of-scope fix)

### Active

<!-- Current scope: tests for fork additions, bug fixes they surface, green suite, coverage, CI. -->

**Transform follow-ups (emerged in Phase 2 code review, not yet scheduled):**

- [ ] End-to-end coverage for an actual `null` entry inside a `Map(K, Nullable(V))` column — Phase 2's fix is asserted for non-null values only; the null path is code-read, never exercised through RowBinary (WR-01)
- [ ] `ClickHouseWriter` MAP/ARRAY branches fail loudly, as `doWriteCol()` does, when a `null` lands in a non-nullable element slot instead of silently desyncing the RowBinary stream (WR-02)
- [ ] Pin `HeaderToValue`'s behavior when `fields` names a pre-existing non-STRING column — currently throws `DataException` on every record, unlike `HeadersToValue`'s tested collision contract (WR-03)

**Table metadata & types:**

- [ ] `targetTableFilter` covered: regex match/no-match, empty filter = all tables, regex containing quotes is escaped, integration against real tables
- [ ] `describeTable` sub-column skipping covered for `Nullable`, `Array`, and `JSON` columns
- [ ] `JSON(...)` with parameters and `Dynamic` columns covered: mapping from `DESCRIBE` output and real inserts on ClickHouse `latest`

**Explicit JSON insert path (tested as-is):**

- [ ] Explicit `insertFormat=json` covered: startup without target table, `getTable` bypass using topic-to-table mapping, `cleanupExtraFields` bypass, `targetTableFilter` ignored
- [ ] `jsonInsertColumns` covered: parsing (empty, whitespace, empty entries), insert of a column subset into a table with extra/default columns

**Connector plumbing:**

- [ ] MBean name format covered, including connector `name` present/absent and null implementation version
- [ ] Test asserts the connector does not inject `async_insert`, and user-supplied `clickhouseSettings` for it are honored

**Quality gates:**

- [ ] Bugs surfaced by the new tests are fixed in the same phase that finds them
- [ ] `./gradlew test` passes in full (fork and upstream tests) against ClickHouse `latest` with client V2 — baseline green since Phase 1; must still hold after all new tests land
- [ ] Fork CI (GitHub Actions) runs the test suite on push against ClickHouse `latest` × client V2 and publishes the coverage report

### Out of Scope

- New connector features — the goal is coverage of existing fork behavior, not new capability
- Completing WIP features beyond bug fixes (e.g., validating `jsonInsertColumns` against the table schema, new config semantics) — user chose to test WIP behavior as-is
- Refactoring `ClickHouseWriter`, `Processing`, or `ClickHouseSinkConfig` — not required for coverage; high churn on a fork that must stay rebasable
- Syncing `custom_build` with upstream `ClickHouse/clickhouse-kafka-connect` — separate effort
- Test matrix for ClickHouse 23.7 / 24.3 and client V1 — production connectors use V2; only `latest` matters (older versions also lack JSON/Dynamic)
- ClickHouse Cloud test matrix — fork has no cloud credentials and does not use Cloud
- Upstream's disabled `testDBTopicSplit` — upstream feature, not a fork addition
- Upstream hardening items from `.planning/codebase/CONCERNS.md` (SQL interpolation in `KeeperStateProvider`, reload4j replacement, error categorization) — not fork additions

## Context

- **Repository:** fork `git@github.com:ne1r0n/clickhouse-kafka-connect.git`, branch `custom_build`, 34 commits ahead of local `main` (no upstream remote configured). Codebase map lives in `.planning/codebase/` (mapped at commit `22f055c`).
- **Stack:** Java 17 toolchain, Gradle Kotlin DSL, Kafka Connect API 4.1.1, ClickHouse Java client 0.9.6 (V1 and V2 code paths), Jackson + Gson, Lombok. Tests: JUnit Jupiter, Mockito, Hamcrest, Testcontainers 1.21.4 (ClickHouse, Kafka, Toxiproxy).
- **Test layout:**
  - `src/test/java` — unit tests plus ClickHouse-backed tests extending `ClickHouseBase` (starts a Testcontainers ClickHouse per class); run by `./gradlew test`.
  - `src/test/java/transforms/` — SMT tests (outside the `com.clickhouse...` package).
  - `src/integrationTest/java` — Confluent-platform / Cloud tests, `integrationTest` task, only `workflow_dispatch` in CI.
  - ClickHouse image chosen by `CLICKHOUSE_VERSION` env (default `25.3`, `ClickHouseTestHelpers`); client by `CLIENT_VERSION` env (default V2). Tests run with `file.encoding=windows-1252`.
  - Version gating helper exists: `src/test/java/.../extension/FromVersionConditionExtension.java`.
- **Fork test inventory (2026-09-15):**

  | Fork addition | Existing tests |
  |---|---|
  | `TableOperationFilter` | 26 unit (`TableOperationFilterTest`) + 1 sink-level — Phase 2 (was 9) |
  | `ConvertToMapOrJson` | 37 unit + perf test + 1 sink-level — Phase 2 (was 13) |
  | `HeadersToValue` | 25 unit + 1 sink-level — Phase 2 (was 2) |
  | `HeaderToValue` | 24 unit + 1 sink-level — Phase 2 (was 2) |
  | `targetTableFilter` | 1 ClickHouse-backed (`ClickHouseSinkTaskMappingTest.tableFilterTest`) + 1 in JSON insert test |
  | Explicit JSON insert path | 2 ClickHouse-backed (`ClickHouseSinkTaskJsonInsertFormatTest`) |
  | `jsonInsertColumns` | none |
  | `JSON(...)` / `Dynamic` types | none (`JsonColumnTest` has a `@Disabled` case) |
  | `describeTable` sub-column skip | none |
  | MBean `connector=` name | none |
  | `async_insert` not forced | none |

- **CI today:** `.github/workflows/tests.yaml` runs `gradle test` on every push with matrix client V1/V2 × ClickHouse `23.7`, `24.3`, `latest`, `cloud` (cloud skipped without secrets), using deprecated `gradle/gradle-build-action@v2` and `actions/setup-java@v3`. GitHub Actions **is enabled** on the fork (verified 2026-09-15 via the public GitHub API): 5 runs on `custom_build`, the last 4 green — most recent `22f055c` on 2026-01-29; `b7ffde4` failed. No JaCoCo configured. `gh` CLI is not installed locally.
- **Local environment:** Docker via colima is installed but was not running at initialization — required for all `ClickHouseBase` tests. JDK 21 on PATH; Gradle toolchain targets 17.
- **Known risk:** `.planning/codebase/CONCERNS.md` notes `Type.DYNAMIC` has no RowBinary serialization in `ClickHouseWriter`. With explicit JSON insert, ClickHouse parses `JSONEachRow` server-side, but Dynamic via the RowBinary path may fail — if tests confirm, this is a bug to fix (or turn into an explicit, clear error) under the "fix immediately" rule.
- **Baseline:** CI was green at `22f055c` (2026-01-29) on the full matrix, but the `latest` image has moved since and local tests default to `25.3` — whether `./gradlew test` is green today on `latest`/V2 has not been checked; the first phase should establish it.
- **Research:** `.planning/research/` (STACK, FEATURES, ARCHITECTURE, PITFALLS, SUMMARY) holds per-feature test-case inventories and 10 pitfalls grounded in fork source.

## Constraints

- **Test target**: ClickHouse `latest` + client V2 only — matches production connectors; older versions lack JSON/Dynamic.
- **Tooling**: Reuse existing JUnit/Mockito/Testcontainers setup, `ClickHouseBase`, and test helpers; only new build dependency expected is the JaCoCo Gradle plugin — keeps the fork close to upstream conventions.
- **Behavior preservation**: Tests pin current fork behavior (including WIP JSON path and not forcing `async_insert`); code changes are limited to genuine bug fixes — the fork runs in production.
- **Environment**: Docker (colima) must be running for ClickHouse-backed tests locally and available on CI runners.
- **Rebasability**: Keep changes additive (new test files/cases, small targeted fixes) to minimize conflicts with future upstream syncs.

## Key Decisions

| Decision | Rationale | Outcome |
|----------|-----------|---------|
| Unit + integration tests (Testcontainers ClickHouse) | Unit tests alone can't verify real inserts for JSON/Dynamic, `jsonInsertColumns`, server-side `targetTableFilter` | — Pending |
| Fix bugs found by tests in the same phase | User wants working features, not just documented failures | ✓ Phase 2 fixed 4 bugs inline (D-01, D-02 ×2, D-03) plus one approved out-of-scope writer fix |
| Test WIP JSON-path features as-is | Pin current behavior; no feature expansion in this project | — Pending |
| `async_insert=0` intentionally not forced | Async insert is controlled via server profile / `clickhouseSettings` | — Pending |
| Test matrix: ClickHouse `latest` × client V2 only | Production uses V2; only latest ClickHouse matters | ◐ Local default test image is `latest` (Phase 1); CI matrix pending (Phase 5) |
| Done = per-feature tests + fully green `./gradlew test` + JaCoCo report + CI | User's definition of done | — Pending |
| All research differentiators included in v1 (sink-level SMT tests, SMT contracts, RE2 live tests, non-ASCII, container singleton, `testUnit` task, etc.) | User selected every optional test group during scoping | — Pending |
| `JSON`/`Dynamic` via RowBinary, if broken → explicit clear error (not serialization, not auto-routing) | Smallest rebase-friendly fix; serialization would be new capability | — Pending |
| `TableOperationFilter` invalid `skipped.operations` (incl. `r`) → pin warn-and-ignore | Avoid breaking existing production connector configs | ✓ Phase 2 (TOF-03, pinned behaviourally with no log assertions) |
| A main-code fix reaching outside `transforms/` stops for a user decision rather than being taken autonomously | Fork runs in production; the phase's declared fix scope is `transforms/`-only | ✓ Phase 2 — plan 02-05 halted on a `ClickHouseWriter` MAP-branch bug; user approved the 3-line fix mirroring the existing ARRAY branch |
| `ClickHouseWriter` MAP branch writes the RowBinary null marker for `Nullable` map values; an actual `null` map value stays unimplemented | Smallest fix that unblocks `Map(K, Nullable(V))` columns; a `writeNull`/`defaultsSupport` path is a larger change deferred out of Phase 2 | ✓ Phase 2 (commit `3929ce6`), null-entry path tracked as an Active follow-up |
| Coverage: report only, no CI threshold | Visibility wanted, not a gate | ✓ JaCoCo is report-only on `test`, no coverage-verification rule (Phase 1) |
| Server-version smoke test asserts the container image reference plus a well-formed reported version, not an image version label | ClickHouse `latest` images carry no usable version label (ClickHouse issue #54224), so label equality is unsatisfiable | ✓ Phase 1 |
| One shared ClickHouse container per test JVM fork (`alwaysPull`), a fresh database per test class, cleanup left to Ryuk (no JVM shutdown hook), default `maxParallelForks` kept | Fast runs with class isolation and no change to test semantics; 0 flakes observed at 6 forks | ✓ Phase 1 |
| Fork coverage view is an explicit list of the 15 fork-changed classes (`X.class` + `X$*.class`) in a separate `jacocoForkReport` | Prefix globs over-match (e.g. `HeaderToValue*` would pull in `HeaderToValueConfig`) | ✓ Phase 1 |
| `testUnit` selects tests via `excludeTags("clickhouse")` and stays out of `check` | Docker-free unit loop without a new source set or changing `check` | ✓ Phase 1 |

## Evolution

This document evolves at phase transitions and milestone boundaries.

**After each phase transition** (via `/gsd-transition`):
1. Requirements invalidated? → Move to Out of Scope with reason
2. Requirements validated? → Move to Validated with phase reference
3. New requirements emerged? → Add to Active
4. Decisions to log? → Add to Key Decisions
5. "What This Is" still accurate? → Update if drifted

**After each milestone** (via `/gsd-complete-milestone`):
1. Full review of all sections
2. Core Value check — still the right priority?
3. Audit Out of Scope — reasons still valid?
4. Update Context with current state

---
*Last updated: 2026-09-16 after Phase 2*
