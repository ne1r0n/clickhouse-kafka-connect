---
last_mapped_commit: 22f055c712de378404f73cffe198b641002ed248
last_mapped_at: 2026-09-15
---
# Codebase Concerns

**Analysis Date:** 2026-09-15

## Tech Debt

**Unfinished WIP Features (Custom Branch):**

- Issue: Multiple commits marked `[WIP]` indicate incomplete implementations on the `custom_build` branch
- Files: 
  - `src/main/java/com/clickhouse/kafka/connect/sink/ClickHouseSinkConfig.java` (jsonInsertColumns param)
  - `src/main/java/com/clickhouse/kafka/connect/sink/db/ClickHouseWriter.java` (skip table metadata, Dynamic type)
  - `src/main/java/com/clickhouse/kafka/connect/transforms/HeadersToValue.java` (new transform)
  - `build.gradle.kts` (version management)
- Impact: Features may be unstable or have incomplete error handling; could break in production
- Fix approach: Complete and test all WIP features before merging to production branch; add integration tests for each feature

**Oversized ClickHouseWriter Class:**

- Issue: `ClickHouseWriter.java` is 1244 lines — mixing mapping logic, type handling, serialization, error recovery
- Files: `src/main/java/com/clickhouse/kafka/connect/sink/db/ClickHouseWriter.java`
- Impact: Hard to understand, test, and modify; difficult to isolate bugs
- Fix approach: Extract type-specific logic into separate classes (DateTypeHandler, DecimalTypeHandler, etc.); pull out mapping refresh logic into TableMappingManager

**Broad Exception Handling:**

- Issue: Many `catch(Exception e)` blocks without specific exception typing or recovery strategies
- Files:
  - `src/main/java/com/clickhouse/kafka/connect/sink/ClickHouseSinkTask.java` (lines 54-62)
  - `src/main/java/com/clickhouse/kafka/connect/sink/processing/Processing.java` (lines 55, 70)
  - `src/main/java/com/clickhouse/kafka/connect/ClickHouseSinkConnector.java`
  - `src/main/java/com/clickhouse/kafka/connect/util/jmx/MBeanServerUtils.java`
- Impact: Difficult to debug errors; unclear what exceptions are expected; no way to handle different failures differently
- Fix approach: Use specific exception types; log exception class and context; handle ClickHouseException, IOException, DataException separately

**Configuration Not Grouped Logically:**

- Issue: 30+ configuration fields declared sequentially with minimal organization
- Files: `src/main/java/com/clickhouse/kafka/connect/sink/ClickHouseSinkConfig.java` (lines 316)
- Impact: Hard to understand which configs affect which behavior; unclear defaults and constraints
- Fix approach: Group configs by concern (Connection, Data Format, Error Handling, State Management, Batching, Schema); add documentation comments

**Inefficient Mapping Lookups:**

- Issue: Comment at `ClickHouseWriter.java:652` indicates mapping lookups are not optimized for frequent access
- Files: `src/main/java/com/clickhouse/kafka/connect/sink/db/ClickHouseWriter.java`
- Impact: Potential performance degradation when writing many records with different tables
- Fix approach: Profile mapping access patterns; consider caching or indexing; benchmark against baseline

---

## Known Bugs

**Disabled Test for DB Topic Split:**

- Symptoms: The `testDBTopicSplit` test is commented out and marked `// @Test TODO: Fix this test`
- Files: `src/test/java/com/clickhouse/kafka/connect/sink/ClickHouseSinkTaskTest.java` (line 103)
- Trigger: Enable the DB topic split feature and run this test
- Workaround: Manually verify DB topic split feature by testing in integration environment
- Impact: Core feature (database.topic.split) may have undetected bugs; no regression prevention

**Incomplete Dynamic Type Support (RESOLVED — Phase 3 Plan 03):**

- Symptoms: Dynamic type added at `Type.DYNAMIC` in Column.java but no serialization logic in ClickHouseWriter
- Files: `src/main/java/com/clickhouse/kafka/connect/sink/db/mapping/Column.java` (line 154-155)
- Trigger: Try to write a Dynamic column via the RowBinary path (JSON insert already worked)
- Resolution: RowBinary serialization for `JSON`/`Dynamic` remains locked Out of Scope, but the previously silent zero-byte write (stream desync) is now a fast, actionable `DataException` thrown from `ClickHouseWriter.doWriteColValue`'s default branch and `validateDataSchema`, naming the column, its type, and the `insertFormat=json` / `bypassRowBinary=true` escape hatches. Pinned by `RowBinaryUnsupportedTypeGateTest` (Docker-free) and `RowBinaryFailFastLiveTest` (live, proves `countRows == 0` and the escape hatch works). See TYPE-06 in REQUIREMENTS.md and `03-03-SUMMARY.md`.
- Impact: No more silent stream corruption or cryptic server-side `ATTEMPT_TO_READ_AFTER_EOF` errors; operators get a clear message and two working workarounds.

---

## Security Considerations

**Unparameterized SQL in KeeperStateProvider (low severity, hardening):**

- Risk: State queries interpolate the `topic-partition` key and the configured state table name via `String.format()`. Practical exploitability is low: Kafka restricts topic names to `[a-zA-Z0-9._-]` (no quotes or semicolons), and the table name comes from operator-controlled connector config. It matters only if the key source changes (e.g. topic-to-table mapping or a transform starts feeding arbitrary strings into the key).
- Files: `src/main/java/com/clickhouse/kafka/connect/sink/state/provider/KeeperStateProvider.java` (lines 90, 121)
- Current mitigation: Implicit, via Kafka topic-name character restrictions; no explicit escaping or parameterization
- Affected queries:
  ```java
  String selectStr = String.format("SELECT * FROM `%s` WHERE `key`= '%s'", csc.getZkDatabase(), key);
  String insertStr = String.format("INSERT INTO `%s` SETTINGS wait_for_async_insert=1 VALUES ('%s', %d, %d, '%s');", csc.getZkDatabase(), key, minOffset, maxOffset, state);
  ```
- Recommendations:
  1. Use prepared statements or parameterized queries (if ClickHouse client supports)
  2. Escape single quotes in topic names: `key.replace("'", "''")` before insertion
  3. Validate topic names against a whitelist of safe characters (`[a-zA-Z0-9._-]`)
  4. Unit test with malicious topic names: `topic'; DROP TABLE--`, `topic\x00`, `topic'OR'1'='1`

**Password Logging:**

- Risk: Password could be logged in debug/trace logs even with masking
- Files: 
  - `src/main/java/com/clickhouse/kafka/connect/sink/ClickHouseSinkConfig.java`
  - `src/main/java/com/clickhouse/kafka/connect/sink/state/provider/KeeperStateProvider.java` (line 43 uses Mask class)
- Current mitigation: `Mask.passwordMask()` is applied in KeeperStateProvider but not consistently elsewhere
- Recommendations:
  1. Audit all log statements for password/credentials
  2. Create a utility to mask all sensitive fields in config logs
  3. Test with log level DEBUG to ensure no sensitive data leaks

---

## Performance Bottlenecks

**Single-Threaded Record Processing:**

- Problem: ProxySinkTask processes records sequentially in a loop (no parallelization per partition)
- Files: `src/main/java/com/clickhouse/kafka/connect/sink/ProxySinkTask.java` (lines 105-109)
- Cause: Simple for-loop over topic/partition groupings; TODO comment suggests awareness but no implementation
- Improvement path:
  1. Use ExecutorService to process independent topic/partitions in parallel
  2. Measure impact on latency and throughput (may not help if I/O bound)
  3. Consider configurable parallelism level

**Decimal Type Incompatibility with Map Keys:**

- Problem: ClickHouse doesn't allow Decimal as Map key type, but no validation or early warning
- Files: `src/main/java/com/clickhouse/kafka/connect/sink/db/mapping/Column.java` (lines 147-149)
- Cause: Type system accepts Decimal but ClickHouse server rejects during INSERT
- Improvement path:
  1. Validate Map/Array element types during schema discovery (`ClickHouseWriter.doWriteColumnValues()`)
  2. Throw descriptive error with suggestion to use String or fixed-point representation
  3. Add test: `ClickHouseSinkTaskMappingTest` with Decimal map key schema

**Missing Validation for Complex Types:**

- Problem: TODO at `ClickHouseWriter.java:242` — no extra validation for Map/Array type compatibility
- Files: `src/main/java/com/clickhouse/kafka/connect/sink/db/ClickHouseWriter.java`
- Cause: Type dispatch doesn't check nested type constraints
- Improvement path: Add validation phase before INSERT to catch nested type issues early

---

## Fragile Areas

**Configuration Parsing (ClickHouseSinkConfig):**

- Files: `src/main/java/com/clickhouse/kafka/connect/sink/ClickHouseSinkConfig.java`
- Why fragile: 
  - 651 lines mixing parsing, validation, and defaults
  - No explicit validation order (could fail early or late depending on config)
  - Topic-to-table mapping parsed with basic split() — no error handling for malformed input
  - Boolean/integer configs lack bounds checking
- Safe modification: 
  - Add ConfigValidator class with explicit validation rules
  - Add unit tests for edge cases: empty strings, null values, out-of-range numbers, special characters
  - Use ConfigException instead of RuntimeException for configuration errors

**Table Mapping Refresh (TableMappingRefresher):**

- Files: `src/main/java/com/clickhouse/kafka/connect/sink/db/TableMappingRefresher.java`
- Why fragile:
  - Runs in a separate Timer thread but doesn't handle ClickHouseException or connection failures gracefully
  - No deadlock prevention if refresh is slow
  - Atomic refresh flag (`isUpdateMappingRunning`) could cause stale data if exception occurs mid-update
- Safe modification:
  - Wrap refresh in try-catch; log exceptions; don't propagate
  - Set timeout on metadata query
  - Use ConcurrentHashMap for thread-safe updates without blocking writes

**Type Dispatch in ClickHouseWriter:**

- Files: `src/main/java/com/clickhouse/kafka/connect/sink/db/ClickHouseWriter.java` (doWriteColumnValues method)
- Why fragile:
  - Large switch statement with inline logic
  - Missing cases (e.g., IPv4, IPv6, Enum, Interval types)
  - No fallback for unknown types (defaults to logging + skip)
- Safe modification:
  - Extract each type handler to a separate class implementing TypeHandler interface
  - Add explicit exception for unknown types
  - Add test for each ClickHouse scalar type

---

## Scaling Limits

**In-Memory Table Mapping:**

- Current capacity: HashMap<String, Table> holds all table metadata in memory
- Limit: With 10,000+ tables, memory footprint grows; lookup performance OK for < 100K entries
- Scaling path:
  1. Monitor memory usage in production with large schemas
  2. Implement table metadata caching with TTL instead of always in-memory
  3. Lazy-load table metadata on first access

**Single ClickHouse Connection Instance:**

- Current capacity: One `ClickHouseHelperClient` per task; connection pooling handled by underlying client
- Limit: If server has low max connections, connector tasks may exhaust server limit
- Scaling path:
  1. Document ClickHouse `max_connections` setting required for N connector tasks
  2. Implement connection pool validation at startup (ping + reserved connection test)
  3. Consider per-batch connection reuse vs. per-insert connection

---

## Dependencies at Risk

**SLF4J Reload4J (Deprecated):**

- Risk: SLF4J binding used is deprecated; reload4j is end-of-life
- Dependency: `org.slf4j:slf4j-reload4j:2.0.17` at `build.gradle.kts:103`
- Impact: No longer receives security updates; may have unpatched vulnerabilities
- Migration plan:
  1. Replace with `org.slf4j:slf4j-logback:2.0.17` (modern, maintained)
  2. Test log output and performance
  3. Add logback configuration file if needed
- Priority: High — resolve before next production release

**Jackson Databind (2.21.0):**

- Risk: Monitor for security patches; version 2.21 is recent but should be kept current
- Dependency: `com.fasterxml.jackson.core:jackson-databind:2.21.0` at `build.gradle.kts:98`
- Impact: If a deserialization vulnerability exists, Kafka record values could be exploited
- Monitoring: Check `dependencyUpdates` gradle task regularly
- Priority: Medium — plan quarterly updates

**ClickHouse Client (0.9.6):**

- Risk: Version 0.9.6 is stable but verify it matches ClickHouse server version
- Dependency: `com.clickhouse:clickhouse-client:0.9.6` at `build.gradle.kts:64`
- Impact: Version mismatch with server could cause wire protocol incompatibility
- Monitoring: Document minimum/maximum supported ClickHouse server versions
- Priority: Medium — check compatibility matrix quarterly

---

## Missing Critical Features

**Graceful Shutdown on ClickHouse Connection Loss:**

- Problem: KeeperStateProvider fails to exit gracefully if initial ping fails (line 58: `// TODO: exit`)
- Blocks: Exactly-once semantics mode cannot start if ClickHouse is unavailable; no retry or fallback
- Impact: If ClickHouse is temporarily down, connector task fails to start; manual restart required after recovery
- Recommendation: Implement exponential backoff retry on startup; allow configurable timeout before giving up

**Parametric Query Execution:**

- Problem: Using `query()` instead of `exec()` for DDL (line 78: `// TODO: exec instead of query`)
- Blocks: Schema creation may not be idempotent; unclear if results are consumed correctly
- Impact: Potential resource leak if query response is not properly closed; possible execution failures
- Recommendation: Use `exec()` method for DDL statements; verify with ClickHouse client API documentation

**Comprehensive Error Categorization:**

- Problem: Line 1226 TODO in ClickHouseWriter — unclear which exception type to throw
- Blocks: Proper error handling in DLQ vs. retry logic cannot be implemented
- Recommendation: Create ErrorCategory enum (TRANSIENT, PERMANENT, SCHEMA_MISMATCH, etc.) and throw accordingly

---

## Test Coverage Gaps

**Date/DateTime Type Handling:**

- What's not tested: Timezone handling, precision loss in DateTime64 conversion, edge cases (epoch, far future)
- Files: `src/main/java/com/clickhouse/kafka/connect/sink/db/ClickHouseWriter.java` (doWriteDates method, line 284)
- Risk: Silent data corruption (datetime values written incorrectly); users unaware until querying
- Priority: High — add parametrized test for all datetime/date types with various precisions and timezones

**Dynamic Type Serialization:**

- What's not tested: Binary serialization of Dynamic type; no test cases
- Files: `src/main/java/com/clickhouse/kafka/connect/sink/db/ClickHouseWriter.java` (type dispatch)
- Risk: Runtime exception when Dynamic column encountered; feature appears incomplete
- Priority: High — add unit test for Dynamic type with sample values (int, string, array)

**DB Topic Split Feature:**

- What's not tested: Test disabled (testDBTopicSplit commented out)
- Files: `src/test/java/com/clickhouse/kafka/connect/sink/ClickHouseSinkTaskTest.java` (line 103)
- Risk: Feature may be broken; no regression prevention
- Priority: High — re-enable test and verify feature works end-to-end

**HeadersToValue Transform:**

- What's not tested: New transform added but no integration tests with ClickHouse writes
- Files: `src/main/java/com/clickhouse/kafka/connect/transforms/HeadersToValue.java`
- Risk: Transform may work in isolation but fail when data reaches ClickHouse (type mismatches, null handling)
- Priority: Medium — add integration test showing headers → map → JSON insert

**JsonInsertColumns Parameter:**

- What's not tested: New parameter added in WIP commit but no tests validating behavior
- Files: `src/main/java/com/clickhouse/kafka/connect/sink/ClickHouseSinkConfig.java` (JSON_INSERT_COLUMNS)
- Risk: Feature incomplete; unclear how to use; may have edge cases (empty list, invalid columns)
- Priority: High — add unit test for parameter parsing and integration test for selective column insertion

**Proxy Task Concurrency:**

- What's not tested: Multi-task behavior; how tasks share state; race conditions in table mapping refresh
- Files: `src/main/java/com/clickhouse/kafka/connect/sink/ProxySinkTask.java`
- Risk: Intermittent failures under load; data loss or duplicates in exactly-once mode
- Priority: High — add concurrent stress test with multiple tasks writing to same table

---

## Recommendations Summary

**Immediate (Before Production):**

1. Harden KeeperStateProvider SQL (parameterize or escape the state key); low severity since Kafka topic names cannot contain quotes
2. Re-enable and fix testDBTopicSplit test
3. Add tests for Dynamic type serialization
4. Replace deprecated SLF4J reload4j with logback

**Short-term (Next Release):**

1. Extract ClickHouseWriter type handling into separate classes
2. Complete and test WIP features (jsonInsertColumns, skip table metadata, HeadersToValue)
3. Implement graceful shutdown and retry logic for connection failures
4. Add parametrized tests for all datetime/date types

**Medium-term (Refactoring Phase):**

1. Reorganize ClickHouseSinkConfig with explicit validation groups
2. Profile and optimize mapping lookups
3. Implement concurrent record processing per partition
4. Add comprehensive error categorization and handling

---

*Concerns audit: 2026-09-15*
