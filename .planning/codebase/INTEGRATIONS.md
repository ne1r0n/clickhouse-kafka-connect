---
last_mapped_commit: 22f055c712de378404f73cffe198b641002ed248
last_mapped_at: 2026-09-15
---
# External Integrations

**Analysis Date:** 2026-09-15

## APIs & External Services

**Message Streaming:**

- Apache Kafka (Confluent Kafka 4.1.1+) - Primary data source
  - Consumer API: `org.apache.kafka:connect-api:4.1.1`
  - Protocol: Kafka Connect protocol with exactly-once delivery guarantees
  - Configuration: `src/main/java/com/clickhouse/kafka/connect/sink/ClickHouseSinkConfig.java` lines 24-55 define Kafka topic/partition handling

**Analytics Database:**

- ClickHouse (v22.5+) - Primary data sink
  - SDK/Client: `com.clickhouse:clickhouse-client:0.9.6` (native client), `com.clickhouse:clickhouse-http-client:0.9.6` (HTTP variant), `com.clickhouse:client-v2:0.9.6` (v2 API)
  - Auth: HTTP Basic (username/password in config)
  - Connection: HTTP over TLS (port 8443 by default)
  - Integration point: `src/main/java/com/clickhouse/kafka/connect/sink/db/ClickHouseWriter.java` (query execution), `src/main/java/com/clickhouse/kafka/connect/sink/db/ClickHouseHelperClient.java` (metadata fetching)

## Data Storage

**Databases:**

- ClickHouse
  - Connection: HTTP API (`com.clickhouse:clickhouse-http-client:0.9.6`)
  - Client: Native ClickHouse JDBC driver (`com.clickhouse:clickhouse-jdbc:0.9.6:all`) for metadata queries
  - Default database: `default` (configured via `ClickHouseSinkConfig.DATABASE`)
  - Default port: 8443 (configured via `ClickHouseSinkConfig.PORT`)
  - Features:
    - Exactly-once semantics enabled via `exactlyOnce` config parameter
    - Custom insert formats: CSV, TSV, JSON (configured via `insertFormat`)
    - JSON insert columns specification via `jsonInsertColumns` parameter

**State Storage:**

- ZooKeeper or ClickHouse Keeper (distributed)
  - Purpose: Maintain Kafka offset state and task coordination for exactly-once delivery
  - Configuration: 
    - ZK path: `zkPath` (default `/kafka-connect`)
    - ZK database: `zkDatabase` (default `connect_state`)
    - Keeper cluster: `keeperOnCluster` config parameter
  - Location: `src/main/java/com/clickhouse/kafka/connect/sink/state/provider/KeeperStateProvider.java` (state management)

**File Storage:**

- Not applicable - all data flows through Kafka → ClickHouse pipeline

**Caching:**

- In-memory state caching in connector tasks
  - Offset tracking via `RangeContainer`, `OffsetContainer` (`src/main/java/com/clickhouse/kafka/connect/sink/kafka/`)
  - Table metadata caching with TTL refresh interval (`tableRefreshInterval` config)
  - Location: `src/main/java/com/clickhouse/kafka/connect/sink/db/TableMappingRefresher.java`

## Authentication & Identity

**Auth Provider:**

- Custom HTTP Basic Authentication
  - Implementation: Direct username/password to ClickHouse HTTP API
  - Configuration parameters:
    - `hostname` - ClickHouse server hostname (required, no default)
    - `port` - ClickHouse server port (default 8443)
    - `username` - ClickHouse user (default `default`)
    - `password` - ClickHouse user password (default empty)
    - `ssl` - Enable TLS for HTTPS (default true)
  - Location: `src/main/java/com/clickhouse/kafka/connect/sink/ClickHouseSinkConfig.java` lines 177-199

## Monitoring & Observability

**Error Tracking:**

- Dead Letter Queue (DLQ) support
  - Implementation: Error reporter sends failed records to DLQ topic
  - Location: `src/main/java/com/clickhouse/kafka/connect/sink/dlq/ErrorReporter.java`
  - Duplicate detection: `src/main/java/com/clickhouse/kafka/connect/sink/dlq/DuplicateException.java`

**Logs:**

- SLF4J with Reload4j backend (2.0.17)
  - Test logging config: `src/test/resources/log4j.properties`
  - Logger integration: ClickHouseSinkConfig uses `LoggerFactory.getLogger()` for debug output
  - Metrics: JMX-based statistics tracking
    - Location: `src/main/java/com/clickhouse/kafka/connect/util/jmx/SinkTaskStatistics.java`
    - Execution timer: `src/main/java/com/clickhouse/kafka/connect/util/jmx/ExecutionTimer.java`

**Metrics/JMX:**

- MBean registration for task statistics
  - Statistics interface: `src/main/java/com/clickhouse/kafka/connect/util/jmx/SinkTaskStatisticsMBean.java`
  - MBean server utilities: `src/main/java/com/clickhouse/kafka/connect/util/jmx/MBeanServerUtils.java`

## CI/CD & Deployment

**Hosting:**

- Confluent Kafka Connect (distributed or standalone mode)
- Manifest describes integration with Confluent Hub ecosystem

**CI Pipeline:**

- None detected - no GitHub Actions or CI config files in repository
- Build locally via Gradle

**Packaging & Distribution:**

- Confluent Archive format (ZIP with manifest, JAR, docs)
  - Generation task: `createConfluentArchive` task in build.gradle.kts (line 269)
  - Manifest: `config/archive/manifest.json`
  - Fat JAR assembly: `confluentJar` ShadowJar task (line 227) creates classifier `confluent`
  - Distribution metadata:
    - Owner: ClickHouse Inc.
    - License: Apache License 2.0
    - Supported encodings: any
    - Delivery guarantees: exactly-once

## Environment Configuration

**Required env vars:**

- Connection credentials (non-environment-variable-based):
  - `hostname` - ClickHouse hostname (required parameter in config)
  - `username` - Database user (optional, default `default`)
  - `password` - Database password (optional, default empty)
  - ClickHouse connection credentials are passed via Kafka Connect connector configuration, not env vars

**Secrets location:**

- Connector configuration JSON/Properties format (passed to Kafka Connect)
- Passwords marked as `ConfigDef.Type.PASSWORD` for secure handling in UI
- No `.env` file usage detected
- JDBC connection properties: `jdbcConnectionProperties` config parameter for advanced auth/SSL settings

## Webhooks & Callbacks

**Incoming:**

- Kafka topic consumption (push-based consumer)
  - Protocol: Kafka Connect Consumer API
  - Error handling: Errors tolerance via `errors.tolerance` parameter (values: `none` or `all`)

**Outgoing:**

- INSERT queries to ClickHouse HTTP API
  - Protocol: HTTP POST to `http[s]://hostname:port`
  - Query execution: Synchronous batching with timeout (`timeoutSeconds` config, default 30s)
  - Retry mechanism: Configurable retry count (`retryCount` config, default 3, range 3-10)

**Dead Letter Queue Callbacks:**

- DLQ topic publishing for failed records
  - Location: `src/main/java/com/clickhouse/kafka/connect/sink/dlq/ErrorReporter.java`
  - Triggered on record conversion/insertion failures when `errors.tolerance=all`

## Proxy Configuration

**Outbound Proxy Support:**

- HTTP and SOCKS proxy for ClickHouse connections
  - Configuration parameters:
    - `proxyType` - Proxy type: `http`, `socks`, `direct`, or `ignore` (default)
    - `proxyHost` - Proxy hostname (default empty)
    - `proxyPort` - Proxy port (default -1)
  - Location: `src/main/java/com/clickhouse/kafka/connect/sink/ClickHouseSinkConfig.java` lines 254-267
  - Implementation: Delegated to ClickHouse client `ClickHouseProxyType` enum

## Custom Data Transformations

**Kafka Connect Single Message Transforms (SMTs):**
Transform implementations in `src/main/java/com/clickhouse/kafka/connect/transforms/`:

- `ExtractTopic` - Extract Kafka topic name into message value field
  - Config: `src/main/java/com/clickhouse/kafka/connect/transforms/ExtractTopicConfig.java`
- `KeyToValue` - Extract Kafka message key into value (new custom field `_key` or configured)
  - Implementation: `src/main/java/com/clickhouse/kafka/connect/transforms/KeyToValue.java`
- `HeaderToValue` - Extract a single Kafka header into message value
  - Config: `src/main/java/com/clickhouse/kafka/connect/transforms/HeaderToValueConfig.java`
- `HeadersToValue` - Extract multiple Kafka headers into message value (custom addition)
  - Implementation: `src/main/java/com/clickhouse/kafka/connect/transforms/HeadersToValue.java`
- `ConvertToMapOrJson` - Convert structured records to Map or JSON format
  - Implementation: `src/main/java/com/clickhouse/kafka/connect/transforms/ConvertToMapOrJson.java`
- `TableOperationFilter` - Filter records by ClickHouse operation type (INSERT, DELETE, UPDATE)
  - Implementation: `src/main/java/com/clickhouse/kafka/connect/transforms/TableOperationFilter.java`

All transforms registered in service loader: `src/main/resources/META-INF/services/org.apache.kafka.connect.transforms.Transformation`

---

*Integration audit: 2026-09-15*
