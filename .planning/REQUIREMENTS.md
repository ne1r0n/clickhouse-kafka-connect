# Requirements: ClickHouse Kafka Connect — `custom_build` Fork Test Coverage

**Defined:** 2026-09-15
**Core Value:** Every behavior the fork adds on top of upstream is pinned by an automated test that passes against ClickHouse `latest` with client V2, so the fork can be changed or rebased without silently breaking production pipelines.

**Conventions used below:**

- "Pinned" = the test asserts the current behavior as-is; no behavior change is intended.
- "Fixed if broken" = write the test first to establish ground truth; if it exposes a bug, fix it in the same phase (small, targeted, rebase-friendly change).
- "Sink-level" = ClickHouse-backed test through `ClickHouseSinkTask` (extends `ClickHouseBase`).
- Per-case detail and source line references: `.planning/research/FEATURES.md` and `.planning/research/PITFALLS.md`.

## v1 Requirements

### Test Infrastructure

- [x] **INFRA-01**: Baseline `./gradlew test` against ClickHouse `latest` with client V2 is run and its result recorded before new tests are added; pre-existing failures are fixed
- [x] **INFRA-02**: ClickHouse-backed tests run against `latest` by default (not the `25.3` fallback in `ClickHouseTestHelpers`), and a smoke test asserts the running server version matches the requested image
- [x] **INFRA-03**: `ClickHouseBase` reuses a single ClickHouse container per test JVM fork instead of starting one per test class
- [x] **INFRA-04**: ClickHouse-backed test classes carry a JUnit tag, and a `testUnit` Gradle task runs every other test without Docker
- [x] **INFRA-05**: New and touched tests that start a sink task always stop it (try/finally or `@AfterEach`) and use a unique connector name, so a failing test cannot leak an MBean into later tests
- [x] **INFRA-06**: Test helpers that read ClickHouse responses decode with explicit UTF-8 rather than the `windows-1252` test JVM default

### Transforms — TableOperationFilter

- [x] **TOF-01**: Truncate (`t`) events for a listed table are dropped while other operations for that table pass
- [x] **TOF-02**: With multiple tables in `tables`, only listed tables are filtered and unlisted tables pass through
- [x] **TOF-03**: Invalid, empty, or whitespace-only `skipped.operations` tokens (including `r`) are ignored without failing configuration, and `op=r` records always pass (pinned)
- [x] **TOF-04**: Schemaless (Map-valued) Debezium envelopes pass through unfiltered (pinned)
- [x] **TOF-05**: `config()` returns the transform's definition and `close()` is safe to call repeatedly
- [x] **TOF-06**: Sink-level: a Debezium record whose operation is filtered never reaches ClickHouse while unfiltered records do

### Transforms — HeadersToValue

- [x] **HDRS-01**: Tombstone (null value) records pass through unchanged
- [x] **HDRS-02**: A record with no headers gets an empty map in the configured field (schema and schemaless)
- [x] **HDRS-03**: A header with a null value is stored as a null map entry
- [x] **HDRS-04**: An absent `field` key throws `ConfigException` from `AbstractConfig` construction; a present but blank or whitespace-only `field` value throws `DataException` at `configure()`
- [x] **HDRS-05**: Field-name collision is pinned: the schema path throws `DataException` for a non-map field, the schemaless path overwrites the existing value
- [x] **HDRS-06**: The output record preserves topic, partition, key, timestamp, and the original headers
- [x] **HDRS-07**: Duplicate header keys resolve last-wins and non-string header values are stored via `toString()` (pinned)
- [x] **HDRS-08**: `close()` resets the cached schema so a reconfigured transform rebuilds it, and `config()` returns the transform's definition
- [x] **HDRS-09**: Sink-level: headers land in a ClickHouse `Map(String, Nullable(String))` column with correct values

### Transforms — HeaderToValue

- [x] **HDR-01**: Mismatched `fields`/`headers` counts are rejected with `DataException` at `configure()` (both directions)
- [x] **HDR-02**: A multi-valued header copies its last value into the target field
- [x] **HDR-03**: A missing header or a header with a null value leaves the target field untouched (schema and schemaless)
- [x] **HDR-04**: A configured field absent from the original schema is added as an optional string field
- [x] **HDR-05**: The output record preserves topic, partition, key, timestamp, and headers
- [x] **HDR-06**: Non-string header values are stored via `toString()` (pinned), `config()` returns the definition, and `close()` is safe to call repeatedly
- [x] **HDR-07**: Sink-level: a header-derived field lands in its ClickHouse column with the correct value

### Transforms — ConvertToMapOrJson

- [x] **CONV-01**: `preserve.null.values=false` omits null fields in map output and in JSON output with both Jackson and Gson
- [x] **CONV-02**: Structs nested inside Array and Map fields are converted like top-level Structs (fixed if broken)
- [x] **CONV-03**: Values that are not Structs (primitives, strings with JSON output disabled) pass through unchanged
- [x] **CONV-04**: The output record preserves key, key schema, timestamp, and headers
- [x] **CONV-05**: An invalid `output.json.library` value is rejected with `ConfigException`
- [x] **CONV-06**: Direct and sliced `ByteBuffer` values encode to the same base64 as array-backed buffers
- [x] **CONV-07**: `config()` returns the transform's definition and `close()` is safe to call repeatedly
- [x] **CONV-08**: Sink-level: JSON-string output lands in a String/JSON ClickHouse column with correct content

### Table Metadata — targetTableFilter

- [x] **FILT-01**: An empty `targetTableFilter` adds no `match()` clause and all tables in the database are mapped
- [x] **FILT-02**: A filter matching no tables leaves an empty mapping so task start fails regardless of `suppressTableExistenceException`; a filter matching some tables but not the target is where `suppressTableExistenceException` governs silent-drop versus throw
- [x] **FILT-03**: Single quotes in the filter are escaped so the generated query is valid (built clause asserted in a unit test, plus a live query)
- [x] **FILT-04**: RE2 patterns with backslash classes (e.g. `\d`) and anchors select the intended tables against the live `match()` query (fixed if broken)

### Table Metadata — describeTable

- [x] **DESC-01**: `describeTable` on ClickHouse `latest` skips `.null` subcolumns of `Nullable` columns
- [x] **DESC-02**: `describeTable` skips `.size0` subcolumns of `Array` columns
- [x] **DESC-03**: `describeTable` skips JSON internal subcolumns; raw `DESCRIBE ... describe_include_subcolumns` output on `latest` is compared with the skip list and unhandled suffixes are fixed
- [x] **DESC-04**: A column that cannot be mapped drops the whole table from mapping (pinned by test) and is logged at ERROR instead of WARN

### Column Types — JSON and Dynamic

- [x] **TYPE-01**: `JSON` and parameterized `JSON(...)` type strings map to `Type.JSON`
- [x] **TYPE-02**: The `Dynamic` type string maps to `Type.DYNAMIC`
- [x] **TYPE-03**: Records inserted into a `JSON` column via the JSON insert path read back with correct nested values
- [x] **TYPE-04**: Records inserted into a `Dynamic` column via the JSON insert path read back with correct values and types
- [x] **TYPE-05**: A table mixing several `JSON`/`Dynamic` columns with ordinary columns round-trips correct values
- [x] **TYPE-06**: Schema records targeting `JSON`/`Dynamic` columns via the RowBinary path fail fast with a clear error stating `insertFormat=json` is required (fixed if broken)
- [x] **TYPE-07**: Non-ASCII values (Cyrillic, CJK) round-trip through String, `JSON`, and `Dynamic` columns and through SMT header values under the `windows-1252` test JVM

### Explicit JSON Insert Path

- [ ] **JSON-01**: `insertFormat=json` in any letter case enables metadata-free mode, while leaving `insertFormat` unset does not
- [ ] **JSON-02**: In metadata-free mode, the target table is resolved from topic-to-table mapping (topic name ≠ table name) without any fetched table metadata
- [ ] **JSON-03**: In metadata-free mode, fields absent from the table are sent unmodified and known fields land with correct values
- [ ] **JSON-04**: Tombstone records in metadata-free mode are skipped without error
- [ ] **JSON-05**: Existing explicit-JSON tests assert inserted column values, not only row counts

### jsonInsertColumns

- [ ] **COLS-01**: Unset or blank `jsonInsertColumns` parses to an empty list; whitespace and empty entries are dropped; order is preserved
- [ ] **COLS-02**: With the default empty list, inserts produce correct column values (client-v2 empty-list behavior verified; fixed if broken)
- [ ] **COLS-03**: With an explicit subset, only listed columns are populated from the payload and unlisted columns receive their `DEFAULT`
- [ ] **COLS-04**: JSON keys outside the configured list do not break the insert (pinned)
- [ ] **COLS-05**: A column in the list that does not exist in the table produces a clear error with no partial insert and no NPE
- [ ] **COLS-06**: `jsonInsertColumns` also applies in the non-explicit JSON insert path (pinned)

### Connector Plumbing — JMX

- [ ] **JMX-01**: A task's MBean name includes `connector=<name>` when the connector `name` prop is set
- [ ] **JMX-02**: Without a connector `name` prop the MBean name contains `connector=null` (pinned)
- [ ] **JMX-03**: A null implementation version renders as `version=unknown`, and `:` in a version renders as `_`
- [ ] **JMX-04**: Multiple tasks and connectors register distinct MBeans, and each task's `stop()` unregisters exactly its own

### Connector Plumbing — async_insert

- [ ] **ASYNC-01**: The default configuration does not add `async_insert` to the ClickHouse settings the connector sends
- [ ] **ASYNC-02**: A user-supplied `async_insert` in `clickhouseSettings` is passed through unchanged
- [ ] **ASYNC-03**: A sink insert with `async_insert=1` completes and the data is present in ClickHouse

### Coverage

- [x] **COV-01**: JaCoCo `0.8.15` is attached to the `test` task only (not `integrationTest`/`check`) and produces HTML and XML reports
- [x] **COV-02**: The coverage report excludes Lombok-generated code (verified by inspecting `Column`) and includes a focused view of fork-changed classes

### CI

- [ ] **CI-01**: The CI workflow uses current actions: `actions/checkout@v4`, `actions/setup-java@v6` (temurin 17), `gradle/actions/setup-gradle@v6`
- [ ] **CI-02**: CI runs a single job on push against ClickHouse `latest` × client V2 (V1, 23.7, 24.3, and Cloud legs removed)
- [ ] **CI-03**: CI uploads the JaCoCo report as an artifact and writes a coverage summary to the job summary, without a coverage threshold
- [ ] **CI-04**: `./gradlew test` passes in full locally and in CI against ClickHouse `latest` × client V2

## v2 Requirements

Deferred to a later milestone. Tracked but not in the current roadmap.

### Cleanup & Parity

- **CLEAN-01**: Determine whether `HeaderToValueConfig` is dead code and remove or test it
- **PAR-01**: `describeTableV1` and `describeTableV2` produce identical `Table` results (relevant only if client V1 returns)

### Coverage Reporting

- **COVR-01**: Coverage comment on pull requests (requires a `pull_request` CI trigger)

## Out of Scope

| Feature | Reason |
|---------|--------|
| RowBinary serialization for `JSON`/`Dynamic` columns | Decided: explicit clear error instead; serialization is new capability in `ClickHouseWriter` |
| Auto-routing tables with `JSON`/`Dynamic` columns to JSON insert | Behavior change beyond a bug fix |
| `ConfigException` for invalid `skipped.operations` / support for `r` | Decided: pin warn-and-ignore to avoid breaking production configs |
| Coverage threshold gate in CI | Decided: report only |
| New connector features; completing WIP features beyond bug fixes | Goal is coverage of existing fork behavior; WIP tested as-is |
| Refactoring `ClickHouseWriter`, `Processing`, `ClickHouseSinkConfig` | Not needed for coverage; hurts rebasability |
| Syncing `custom_build` with upstream | Separate effort |
| ClickHouse 23.7/24.3, client V1, ClickHouse Cloud test legs | Production uses V2; only `latest` matters |
| Testcontainers 2.x migration | Renames all artifacts for zero functional gain |
| `integrationTest` source set (Confluent/Cloud) and nightly `test-head.yaml` | Not part of `./gradlew test`; left unchanged |
| Upstream's disabled `testDBTopicSplit` | Upstream feature, not a fork addition |
| Upstream hardening from `.planning/codebase/CONCERNS.md` (KeeperStateProvider SQL, reload4j, error categories) | Not fork additions |

## Traceability

Which phases cover which requirements. Updated during roadmap creation.

| Requirement | Phase | Status |
|-------------|-------|--------|
| INFRA-01 | Phase 1 | Complete |
| INFRA-02 | Phase 1 | Complete |
| INFRA-03 | Phase 1 | Complete |
| INFRA-04 | Phase 1 | Complete |
| INFRA-05 | Phase 1 | Complete |
| INFRA-06 | Phase 1 | Complete |
| COV-01 | Phase 1 | Complete |
| COV-02 | Phase 1 | Complete |
| TOF-01 | Phase 2 | Complete |
| TOF-02 | Phase 2 | Complete |
| TOF-03 | Phase 2 | Complete |
| TOF-04 | Phase 2 | Complete |
| TOF-05 | Phase 2 | Complete |
| TOF-06 | Phase 2 | Complete |
| HDRS-01 | Phase 2 | Complete |
| HDRS-02 | Phase 2 | Complete |
| HDRS-03 | Phase 2 | Complete |
| HDRS-04 | Phase 2 | Complete |
| HDRS-05 | Phase 2 | Complete |
| HDRS-06 | Phase 2 | Complete |
| HDRS-07 | Phase 2 | Complete |
| HDRS-08 | Phase 2 | Complete |
| HDRS-09 | Phase 2 | Complete |
| HDR-01 | Phase 2 | Complete |
| HDR-02 | Phase 2 | Complete |
| HDR-03 | Phase 2 | Complete |
| HDR-04 | Phase 2 | Complete |
| HDR-05 | Phase 2 | Complete |
| HDR-06 | Phase 2 | Complete |
| HDR-07 | Phase 2 | Complete |
| CONV-01 | Phase 2 | Complete |
| CONV-02 | Phase 2 | Complete |
| CONV-03 | Phase 2 | Complete |
| CONV-04 | Phase 2 | Complete |
| CONV-05 | Phase 2 | Complete |
| CONV-06 | Phase 2 | Complete |
| CONV-07 | Phase 2 | Complete |
| CONV-08 | Phase 2 | Complete |
| FILT-01 | Phase 3 | Complete |
| FILT-02 | Phase 3 | Complete |
| FILT-03 | Phase 3 | Complete |
| FILT-04 | Phase 3 | Complete |
| DESC-01 | Phase 3 | Complete |
| DESC-02 | Phase 3 | Complete |
| DESC-03 | Phase 3 | Complete |
| DESC-04 | Phase 3 | Complete |
| TYPE-01 | Phase 3 | Complete |
| TYPE-02 | Phase 3 | Complete |
| TYPE-03 | Phase 3 | Complete |
| TYPE-04 | Phase 3 | Complete |
| TYPE-05 | Phase 3 | Complete |
| TYPE-06 | Phase 3 | Complete |
| TYPE-07 | Phase 3 | Complete |
| JSON-01 | Phase 4 | Pending |
| JSON-02 | Phase 4 | Pending |
| JSON-03 | Phase 4 | Pending |
| JSON-04 | Phase 4 | Pending |
| JSON-05 | Phase 4 | Pending |
| COLS-01 | Phase 4 | Pending |
| COLS-02 | Phase 4 | Pending |
| COLS-03 | Phase 4 | Pending |
| COLS-04 | Phase 4 | Pending |
| COLS-05 | Phase 4 | Pending |
| COLS-06 | Phase 4 | Pending |
| JMX-01 | Phase 4 | Pending |
| JMX-02 | Phase 4 | Pending |
| JMX-03 | Phase 4 | Pending |
| JMX-04 | Phase 4 | Pending |
| ASYNC-01 | Phase 4 | Pending |
| ASYNC-02 | Phase 4 | Pending |
| ASYNC-03 | Phase 4 | Pending |
| CI-01 | Phase 5 | Pending |
| CI-02 | Phase 5 | Pending |
| CI-03 | Phase 5 | Pending |
| CI-04 | Phase 5 | Pending |

**Coverage:**

- v1 requirements: 75 total
- Mapped to phases: 75
- Unmapped: 0 ✓

---
*Requirements defined: 2026-09-15*
*Last updated: 2026-09-15 after roadmap creation*
