---
last_mapped_commit: 22f055c712de378404f73cffe198b641002ed248
last_mapped_at: 2026-09-15
---
<!-- refreshed: 2026-09-15 -->

# Architecture

**Analysis Date:** 2026-09-15

## System Overview

ClickHouse Kafka Connect Sink is a distributed Kafka Connect connector that streams Kafka messages to ClickHouse tables. The architecture implements a layered design with clear separation between intake (Kafka), transformation, state management, and output (ClickHouse database).

```text
┌─────────────────────────────────────────────────────────────────────┐
│                    Kafka Connect Framework                          │
│              (SinkConnector, SinkTask, SinkRecord)                  │
├───────────────────┬──────────────────────┬──────────────────────────┤
│ Connector Entry   │   Task Handler       │     Transforms           │
│ `ClickHouse      │ `ProxySinkTask`      │ `HeadersToValue`         │
│  SinkConnector`  │ `ClickHouseSinkTask` │ `ConvertToMapOrJson`     │
│                  │                      │ `ExtractTopic`           │
│                  │                      │ `KeyToValue`             │
└───────────┬──────┴──────────┬───────────┴──────────────┬────────────┘
            │                 │                          │
            ▼                 ▼                          ▼
┌─────────────────────────────────────────────────────────────────────┐
│              Data Intake & Record Conversion Layer                  │
│  `Record.convert()` → `RecordConvertor` (Schema/Schemaless/String) │
│              `Data`, `StructToJsonMap`, `SchemaType`                │
└───────────────────────────┬──────────────────────────────────────────┘
                            │
            ┌───────────────┴────────────────┐
            ▼                                ▼
┌──────────────────────────┐    ┌──────────────────────────┐
│   State Management       │    │  Processing Logic        │
│  `StateProvider`         │    │  `Processing.doLogic()`  │
│  `StateRecord`           │    │  (Dedup, ranges,         │
│  `InMemoryState`         │    │   offset tracking)       │
│  `KeeperStateProvider`   │    │  `RangeContainer`        │
└──────────────────────────┘    └──────────────────────────┘
            │                                │
            │                                │
            └────────────────┬───────────────┘
                             ▼
         ┌─────────────────────────────────────┐
         │   Database Write Layer              │
         │  `DBWriter` (interface)             │
         │  `ClickHouseWriter` (impl)          │
         │  `ClickHouseHelperClient`           │
         │  Table Mapping & Type Conversion    │
         └──────────────┬──────────────────────┘
                        │
                        ▼
         ┌──────────────────────────────┐
         │  ClickHouse Server           │
         │  (Direct/Proxy Connection)   │
         └──────────────────────────────┘
```

## Component Responsibilities

| Component | Responsibility | File |
|-----------|----------------|------|
| ClickHouseSinkConnector | Kafka Connect entrypoint; validates config; spawns tasks | `src/main/java/com/clickhouse/kafka/connect/ClickHouseSinkConnector.java` |
| ClickHouseSinkTask | Task wrapper; receives records; manages error reporting | `src/main/java/com/clickhouse/kafka/connect/sink/ClickHouseSinkTask.java` |
| ProxySinkTask | Orchestrates record processing; batching; state coordination | `src/main/java/com/clickhouse/kafka/connect/sink/ProxySinkTask.java` |
| Processing | Deduplication logic; state-based replay detection; range tracking | `src/main/java/com/clickhouse/kafka/connect/sink/processing/Processing.java` |
| Record | Immutable data structure representing a Kafka message ready for insert | `src/main/java/com/clickhouse/kafka/connect/sink/data/Record.java` |
| DBWriter | Interface defining database write contract | `src/main/java/com/clickhouse/kafka/connect/sink/db/DBWriter.java` |
| ClickHouseWriter | Manages ClickHouse connections; builds INSERT statements; type conversion | `src/main/java/com/clickhouse/kafka/connect/sink/db/ClickHouseWriter.java` |
| StateProvider | Persists/retrieves offset state per topic-partition | `src/main/java/com/clickhouse/kafka/connect/sink/state/StateProvider.java` |
| InMemoryState | Volatile state implementation for at-least-once semantics | `src/main/java/com/clickhouse/kafka/connect/sink/state/provider/InMemoryState.java` |
| KeeperStateProvider | Persistent state via ZK/Keeper for exactly-once semantics | `src/main/java/com/clickhouse/kafka/connect/sink/state/provider/KeeperStateProvider.java` |
| ClickHouseSinkConfig | Configuration validation and property binding | `src/main/java/com/clickhouse/kafka/connect/sink/ClickHouseSinkConfig.java` |

## Pattern Overview

**Overall:** Multi-threaded Kafka Sink with pluggable state management and record-to-SQL conversion.

**Key Characteristics:**

- **Task-based parallelism**: One `ProxySinkTask` per Kafka worker thread; batches records by topic-partition
- **State-driven deduplication**: Tracks offset ranges per topic-partition to detect replays and overlaps
- **Type-aware conversion**: Introspects Kafka schema (or schemaless JSON) and maps to ClickHouse columns
- **Pluggable storage backends**: InMemoryState for at-least-once; KeeperStateProvider for exactly-once
- **Record transforms**: SMT integrations (HeadersToValue, ConvertToMapOrJson, KeyToValue) before write
- **Error tolerance**: Configurable error handling with Dead Letter Queue (DLQ) routing

## Layers

**Kafka Intake Layer:**

- Purpose: Receive records from Kafka; manage task lifecycle; apply SMTs
- Location: `src/main/java/com/clickhouse/kafka/connect/`
- Contains: `ClickHouseSinkConnector`, `ClickHouseSinkTask`
- Depends on: Kafka Connect APIs, `ClickHouseSinkConfig`
- Used by: Kafka Connect runtime

**Record Conversion Layer:**

- Purpose: Transform SinkRecord into internal Record format; extract schema/data
- Location: `src/main/java/com/clickhouse/kafka/connect/sink/data/` and `src/main/java/com/clickhouse/kafka/connect/sink/data/convert/`
- Contains: `Record`, `RecordConvertor` (variants: Schema, Schemaless, String, Empty), `Data`, `StructToJsonMap`, `SchemaType`
- Depends on: Kafka Connect data structures (Struct, Schema, Field)
- Used by: `ProxySinkTask`, `Processing`

**Processing & State Layer:**

- Purpose: Manage deduplication, replay detection, state persistence
- Location: `src/main/java/com/clickhouse/kafka/connect/sink/processing/` and `src/main/java/com/clickhouse/kafka/connect/sink/state/`
- Contains: `Processing` (orchestrates insert logic), `StateProvider` (interface), `StateRecord`, `RangeContainer`, `RangeState`
- Depends on: Record, `ClickHouseSinkConfig`
- Used by: `ProxySinkTask`
- Key decision: `Processing.doLogic()` examines state (NONE, BEFORE_PROCESSING, AFTER_PROCESSING) and range overlaps (SAME, NEW, CONTAINS, OVERLAPPING, ZERO) to decide if insert proceeds or is deduped

**Database Write Layer:**

- Purpose: Build and execute INSERT statements; manage connections; handle type conversions
- Location: `src/main/java/com/clickhouse/kafka/connect/sink/db/`
- Contains: `DBWriter` (interface), `ClickHouseWriter`, `ClickHouseHelperClient`, `Column`, `Table`, `Type`, mapping helpers
- Depends on: ClickHouse client (JDBC/HTTP), Record, `ClickHouseSinkConfig`
- Used by: `Processing.doInsert()`

**Configuration Layer:**

- Purpose: Define, validate, and expose connector settings
- Location: `src/main/java/com/clickhouse/kafka/connect/sink/`
- Contains: `ClickHouseSinkConfig`, validators (UTF8String, ZKPathValidator, InsertFormatValidator)
- Depends on: Kafka Connect ConfigDef
- Used by: All components

**Transformation Layer (SMTs):**

- Purpose: Mutate record structure before processing (optional)
- Location: `src/main/java/com/clickhouse/kafka/connect/transforms/`
- Contains: `HeadersToValue`, `ConvertToMapOrJson`, `ExtractTopic`, `KeyToValue`, `HeaderToValue`, `TableOperationFilter`
- Depends on: Kafka Connect Transformation API
- Used by: Kafka Connect framework (applied before `ClickHouseSinkTask.put()`)

## Data Flow

### Primary Request Path (Put Record → Insert to ClickHouse)

1. **Record Reception** — `ClickHouseSinkTask.put(Collection<SinkRecord>)` (`src/main/java/com/clickhouse/kafka/connect/sink/ClickHouseSinkTask.java:46`)
   - Called by Kafka Connect runtime with batch of records
   - Delegates to `ProxySinkTask.put()`

2. **Record Grouping** — `ProxySinkTask.put()` (`src/main/java/com/clickhouse/kafka/connect/sink/ProxySinkTask.java:85`)
   - Converts SinkRecord → Record via `Record.convert()` (selects appropriate RecordConvertor)
   - Groups records by topic-partition (or topic only if `ignorePartitionsWhenBatching` enabled)
   - Invokes `Processing.doLogic()` per group

3. **Deduplication & State Check** — `Processing.doLogic()` (`src/main/java/com/clickhouse/kafka/connect/sink/processing/Processing.java:106`)
   - Retrieves current state from `StateProvider` using topic and partition
   - Compares incoming offset range against stored range
   - Routes to different handling based on state enum (NONE, BEFORE_PROCESSING, AFTER_PROCESSING) and overlap type (SAME, NEW, CONTAINS, OVERLAPPING, ZERO)
   - Updates state to BEFORE_PROCESSING

4. **Insert Execution** — `Processing.doInsert()` or split variants (`src/main/java/com/clickhouse/kafka/connect/sink/processing/Processing.java:43`)
   - Builds QueryIdentifier (topic, partition, min/max offset, UUID)
   - Calls `DBWriter.doInsert(records, queryId)`
   - On success: updates state to AFTER_PROCESSING

5. **Type Conversion & SQL Building** — `ClickHouseWriter.doInsert()` (`src/main/java/com/clickhouse/kafka/connect/sink/db/ClickHouseWriter.java`)
   - Determines insert format (CSV/TSV/JSON/RowBinary)
   - Converts Record fields to ClickHouse wire format
   - Streams data via ClickHousePipedOutputStream
   - Handles special types: Decimal, Date, Enum, JSON, Dynamic, Map, Array, Tuple, Variant

6. **Network Transmission** — ClickHouseWriter sends to server
   - Direct HTTP/JDBC connection or proxied via configured proxy
   - Server processes INSERT, dedupes by ReplacingMergeTree logic (if configured)

### State Management Path (Exactly-Once Mode)

1. **Initialization** — `ProxySinkTask.<init>()` (`src/main/java/com/clickhouse/kafka/connect/sink/ProxySinkTask.java:42`)
   - Reads `clickHouseSinkConfig.isExactlyOnce()` flag
   - If true: instantiates `KeeperStateProvider`; else: `InMemoryState`

2. **State Persistence** — `KeeperStateProvider` or `InMemoryState`
   - KeeperStateProvider: stores StateRecord (topic, partition, minOffset, maxOffset, State enum) in ZK/Keeper
   - InMemoryState: volatile HashMap per JVM instance

3. **State Lookup** — `StateProvider.getStateRecord(topic, partition)` (`src/main/java/com/clickhouse/kafka/connect/sink/state/StateProvider.java`)
   - Returns StateRecord with last known offset range and state enum

4. **State Update** — `StateProvider.setStateRecord(stateRecord)` 
   - Persists updated StateRecord (post-insert, post-dedup decision)

### Record Conversion Path (Schema Selection)

1. **Convertor Selection** — `Record.getConvertor()` (`src/main/java/com/clickhouse/kafka/connect/sink/data/Record.java:54`)
   - If data is null → EmptyRecordConvertor
   - Else if schema exists and data is Struct → SchemaRecordConvertor
   - Else if data is Map → SchemalessRecordConvertor
   - Else if data is String → StringRecordConvertor
   - Else → throws DataException

2. **Conversion to Record** — `RecordConvertor.convert()` (subclass implementations)
   - SchemaRecordConvertor: iterates Schema.fields() → builds Field list and jsonMap
   - SchemalessRecordConvertor: introspects Map keys → builds Field list and jsonMap
   - StringRecordConvertor: parses JSON string → builds Field list and jsonMap
   - EmptyRecordConvertor: empty Field list and jsonMap

3. **OffsetContainer Creation** — `Record.newRecord()` 
   - Wraps topic, partition, offset into OffsetContainer for state tracking

**State Management:**

- **Volatile (InMemoryState)**: ConcurrentHashMap per task; lost on restart → at-least-once semantics (may replay)
- **Persistent (KeeperStateProvider)**: ZK/Keeper coordination; survives restarts → exactly-once semantics (dedup via offset range)
- **State Transitions**: NONE → BEFORE_PROCESSING → AFTER_PROCESSING (or error states)

## Key Abstractions

**Record:**

- Purpose: Internal representation of a Kafka message after conversion; immutable
- Examples: `src/main/java/com/clickhouse/kafka/connect/sink/data/Record.java`
- Pattern: Factory method `Record.convert()` with polymorphic convertors

**RecordConvertor:**

- Purpose: Strategy pattern for converting SinkRecord → Record based on input type
- Examples: `SchemaRecordConvertor`, `SchemalessRecordConvertor`, `StringRecordConvertor`, `EmptyRecordConvertor`
- Pattern: Interface + implementations in `src/main/java/com/clickhouse/kafka/connect/sink/data/convert/`

**StateRecord:**

- Purpose: Immutable snapshot of offset range and processing state for a topic-partition
- Examples: `src/main/java/com/clickhouse/kafka/connect/sink/state/StateRecord.java`
- Pattern: Value object holding (topic, partition, minOffset, maxOffset, State enum)

**RangeContainer & RangeState:**

- Purpose: Track min/max offset in current batch; compute overlap relationship with stored state
- Examples: `src/main/java/com/clickhouse/kafka/connect/sink/kafka/RangeContainer.java`, `RangeState` enum
- Pattern: Enum (SAME, NEW, CONTAINS, OVERLAPPING, ZERO, ERROR) for decision routing

**Column & Type:**

- Purpose: Metadata about ClickHouse table columns; enumerates supported types
- Examples: `src/main/java/com/clickhouse/kafka/connect/sink/db/mapping/Column.java`, `Type` enum
- Pattern: Reflection-based introspection of ClickHouse schema; recursive for complex types (Map, Array, Tuple, Variant)

**QueryIdentifier:**

- Purpose: Unique identifier for an INSERT statement (topic, partition, offset range, UUID)
- Examples: `src/main/java/com/clickhouse/kafka/connect/util/QueryIdentifier.java`
- Pattern: Immutable tuple; passed through error handling chain for debugging

## Entry Points

**ClickHouseSinkConnector.start()**:

- Location: `src/main/java/com/clickhouse/kafka/connect/ClickHouseSinkConnector.java:53`
- Triggers: Kafka Connect framework on connector startup
- Responsibilities: Logs version; stores configuration map

**ClickHouseSinkConnector.taskClass()**:

- Location: `src/main/java/com/clickhouse/kafka/connect/ClickHouseSinkConnector.java:60`
- Triggers: Kafka Connect framework during task creation
- Responsibilities: Returns `ClickHouseSinkTask.class` for worker threads

**ClickHouseSinkTask.start()**:

- Location: `src/main/java/com/clickhouse/kafka/connect/sink/ClickHouseSinkTask.java:30`
- Triggers: Kafka Connect worker on task spawn
- Responsibilities: Creates `ClickHouseSinkConfig`; instantiates error reporter; creates `ProxySinkTask`

**ClickHouseSinkTask.put()**:

- Location: `src/main/java/com/clickhouse/kafka/connect/sink/ClickHouseSinkTask.java:46`
- Triggers: Kafka Connect worker when records are ready for processing
- Responsibilities: Delegates to `ProxySinkTask.put()`; catches exceptions; routes to DLQ on error tolerance

**ProxySinkTask.put()**:

- Location: `src/main/java/com/clickhouse/kafka/connect/sink/ProxySinkTask.java:85`
- Triggers: `ClickHouseSinkTask.put()`
- Responsibilities: Converts SinkRecord → Record; batches by topic-partition; invokes `Processing.doLogic()`

**Processing.doLogic()**:

- Location: `src/main/java/com/clickhouse/kafka/connect/sink/processing/Processing.java:106`
- Triggers: `ProxySinkTask.put()` per topic-partition batch
- Responsibilities: State lookup; dedup routing; insert invocation

## Architectural Constraints

- **Threading:** Single-threaded per task (no internal parallelism). ProxySinkTask processes one batch at a time. `RangeContainer` and state checks are non-blocking.
- **Global state:** `ClickHouseWriter.mapping` (table metadata cache) is instance-level; shared across all records in a task. `ProxySinkTask.statistics` is task-level. State provider is thread-safe (InMemoryState uses ConcurrentHashMap; KeeperStateProvider uses ZK locks).
- **Circular imports:** None detected. Clear dependency hierarchy: Connector → Task → Processing + StateProvider + DBWriter → Record + Config.
- **Connection pooling:** ClickHouseHelperClient manages connection via ClickHouseClient; one client per ClickHouseWriter instance (task-scoped). No connection reuse across tasks.
- **State consistency:** KeeperStateProvider enforces atomicity via ZK transactions; InMemoryState is process-scoped only. Multi-task deployments require KeeperStateProvider for consistency.
- **Record immutability:** Record is immutable after construction; avoids mutation bugs during batching.

## Anti-Patterns

### Overly Complex State Transitions

**What happens:** `Processing.doLogic()` implements a large switch statement with nested cases for BEFORE_PROCESSING and AFTER_PROCESSING states, each handling multiple overlap conditions (SAME, NEW, CONTAINS, OVERLAPPING, ZERO, ERROR). The logic is difficult to reason about and extends over 200+ lines.

**Why it's wrong:** The state machine is coupled to offset range comparison logic, making it hard to add new states or overlap types without refactoring the entire method.

**Do this instead:** Extract state transition logic into a separate `StateTransitionResolver` class with a map-based decision table or a fluent builder that makes valid transitions explicit. Example:

```java
class StateTransitionResolver {
  InsertDecision resolve(StateRecord current, RangeContainer incoming) {
    return stateToHandlers.get(current.getState())
      .apply(current, incoming);
  }
}
```

### Type Conversion Scattered Across ClickHouseWriter

**What happens:** `ClickHouseWriter.doInsert()` directly builds binary/CSV/JSON formats inline, mixing concerns of format selection, type conversion, null handling, and wire protocol.

**Why it's wrong:** Adds new format or fixing a type conversion bug requires modifying a large, monolithic method.

**Do this instead:** Extract each format into a separate `InsertFormatter` (CSV, TSV, JSON, RowBinary) implementing a common interface. Move type conversion to a reusable `TypeConverter` class indexed by `Column.type`.

### Column Metadata Refresh via Timer

**What happens:** `ProxySinkTask` spawns a JTimer with `TableMappingRefresher` to periodically reload table metadata from ClickHouse. If the refresh fails silently or the timer is not cancelled on task stop, stale metadata may be used.

**Why it's wrong:** Timer threads are hard to debug; no error propagation if refresh fails; stop() may leave timer threads running if exceptions occur.

**Do this instead:** Use a scheduled executor service or reactive scheduler (Reactor, CompletableFuture) with proper lifecycle management and exception handlers. Ensure cancellation in a try-finally block.

## Error Handling

**Strategy:** Three-tier error handling:

1. **Record-level:** `ClickHouseSinkTask.put()` wraps `ProxySinkTask.put()` in try-catch; if `errorsTolerance` is true, records are sent to DLQ via `Utils.sendTODlq()`.
2. **Insert-level:** `Processing.doInsert()` catches exceptions and wraps them in RuntimeException with QueryIdentifier context.
3. **Connection-level:** `ClickHouseWriter.start()` validates connection (ping), version check, and table discovery; throws RuntimeException if connection fails.

**Patterns:**

- Exception wrapping with context: `throw new RuntimeException(queryId.toString(), e)` preserves offset range for debugging.
- DLQ routing: Errant records are reported via `ErrorReporter.report()` (Kafka Connect's DLQ mechanism).
- Config-based tolerance: `errors.tolerance=all` enables DLQ routing; `errors.tolerance=none` (default) fails the task.

## Cross-Cutting Concerns

**Logging:** SLF4J with log4j backend. Levels: TRACE (offset details), DEBUG (insert decisions, state transitions), INFO (task startup, connection success), WARN (state resets, overlap warnings), ERROR (connection failures, schema errors).

**Validation:** 

- Config validation via `ClickHouseSinkConfig` constructor and custom validators (UTF8String, ZKPathValidator, InsertFormatValidator).
- Schema validation (optional) via `bypassSchemaValidation` flag; if disabled, columns must exist in target table.
- Record validation: `RecordConvertor` throws `DataException` if conversion fails (e.g., no convertor for type).

**Authentication:** ClickHouse username/password via config; stored in `ClickHouseHelperClient` and passed to client on each request. No token refresh; assumes static credentials.

---

*Architecture analysis: 2026-09-15*
