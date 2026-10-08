package com.clickhouse.kafka.connect.sink.db;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.clickhouse.kafka.connect.sink.ClickHouseSinkConfig;
import com.clickhouse.kafka.connect.sink.data.Record;
import com.clickhouse.kafka.connect.sink.data.SchemaType;
import com.clickhouse.kafka.connect.sink.db.mapping.Column;
import com.clickhouse.kafka.connect.sink.db.mapping.Table;
import com.clickhouse.kafka.connect.sink.db.mapping.Type;
import com.clickhouse.kafka.connect.sink.kafka.OffsetContainer;
import com.clickhouse.kafka.connect.util.QueryIdentifier;
import com.clickhouse.kafka.connect.util.jmx.SinkTaskStatistics;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.apache.kafka.connect.data.Schema;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Docker-free unit pins for JSON-01 (the {@code insertFormat=json} letter-case matrix and its
 * negative half, including the upstream {@code getOrDefault(INSERT_FORMAT, "node")} typo) and
 * JSON-03 (the {@code cleanupExtraFields} metadata-free bypass, driven through a reflective
 * sink-config accessor per D-08, since upstream's writer keeps {@code csc} private with no
 * setter). New fork-only class (D-16): it does not extend or modify any upstream test class, and
 * it deliberately extends nothing - not {@code ClickHouseBase}, whose inherited {@code
 * @Tag("clickhouse")} would drop it out of {@code testUnit} (Phase 1 D-12).
 *
 * <p>Re-enabled per the JSONP-01 KEEP decision (Phase 9, D-01/D-11): the metadata-free {@code
 * insertFormat=json} bypass this class pins, dropped by the mechanical v1.6.0 merge, was
 * re-applied to {@code src/main} in Phase 9 as {@code ClickHouseWriter.shouldSkipTableMetadata()}
 * and its four guard sites. This class is that bypass's Docker-free canary inside {@code
 * testUnit} - it must keep failing loudly if a future merge silently deletes the guards again.
 */
public class MetadataFreeJsonInsertUnitTest {

    private static final String WRITER_SINK_CONFIG_FIELD = "csc";

    private static Field writerSinkConfigField() throws NoSuchFieldException {
        Field field = ClickHouseWriter.class.getDeclaredField(WRITER_SINK_CONFIG_FIELD);
        field.setAccessible(true);
        return field;
    }

    private static void setSinkConfig(ClickHouseWriter writer, ClickHouseSinkConfig csc) throws Exception {
        writerSinkConfigField().set(writer, csc);
    }

    private static Map<String, String> minimalProps() {
        Map<String, String> props = new HashMap<>();
        props.put(ClickHouseSinkConfig.HOSTNAME, "localhost");
        props.put(ClickHouseSinkConfig.PORT, "8123");
        props.put(ClickHouseSinkConfig.DATABASE, "default");
        props.put(ClickHouseSinkConfig.USERNAME, "default");
        props.put(ClickHouseSinkConfig.PASSWORD, "");
        return props;
    }

    @ParameterizedTest(name = "JSON-01: insertFormat=\"{0}\" sets isJsonInsertFormatExplicit()=true")
    @ValueSource(strings = {"json", "JSON", "Json", "  json  "})
    @DisplayName("JSON-01: insertFormat=json in any letter case (with surrounding whitespace trimmed) enables the explicit-JSON metadata-free gate")
    void explicitJsonInsertFormatIsCaseInsensitive(String insertFormatValue) {
        Map<String, String> props = minimalProps();
        props.put(ClickHouseSinkConfig.INSERT_FORMAT, insertFormatValue);
        assertTrue(new ClickHouseSinkConfig(props).isJsonInsertFormatExplicit());
    }

    @ParameterizedTest(name = "JSON-01: insertFormat=\"{0}\" leaves isJsonInsertFormatExplicit()=false")
    @ValueSource(strings = {"none", "csv", "tsv"})
    @DisplayName("JSON-01: insertFormat values other than json leave the explicit-JSON metadata-free gate false")
    void nonJsonInsertFormatsAreNotExplicit(String insertFormatValue) {
        Map<String, String> props = minimalProps();
        props.put(ClickHouseSinkConfig.INSERT_FORMAT, insertFormatValue);
        assertFalse(new ClickHouseSinkConfig(props).isJsonInsertFormatExplicit());
    }

    @Test
    @DisplayName("JSON-01: an unset insertFormat leaves isJsonInsertFormatExplicit()=false even though getInsertFormat() resolves to JSON via the upstream default-branch typo")
    void unsetInsertFormatIsNotExplicitDespiteResolvingToJsonEnum() {
        Map<String, String> props = minimalProps();
        ClickHouseSinkConfig csc = new ClickHouseSinkConfig(props);
        assertFalse(csc.isJsonInsertFormatExplicit());
        // The enum falling through to JSON on an unset insertFormat is an upstream default-branch
        // quirk (ClickHouseSinkConfig.java:225, props.getOrDefault(INSERT_FORMAT, "node") - "node"
        // matches none of the switch's cases, so it falls to `default:`), confirmed present on
        // upstream `main`, not a fork addition. It is pinned here by implication and deliberately
        // not fixed (D-01).
        assertEquals(ClickHouseSinkConfig.InsertFormats.JSON, csc.getInsertFormat());
    }

    @Test
    @DisplayName("JSON-03: cleanupExtraFields returns the payload untouched under insertFormat=json, and strips unknown keys under default config, without mutating the input map")
    void cleanupExtraFieldsBypassDiffersByInsertFormat() throws Exception {
        ClickHouseWriter writer = new ClickHouseWriter(new SinkTaskStatistics(null, 0));
        Column knownCol = Column.builder().name("known_col").type(Type.STRING).build();
        Table table = new Table("test_db", "test_table", false, List.of(knownCol), 1);

        Map<String, Object> payload = new HashMap<>();
        payload.put("known_col", "value");
        payload.put("unknown_col", "distinctive_unknown_value");

        Map<String, String> jsonProps = minimalProps();
        jsonProps.put(ClickHouseSinkConfig.INSERT_FORMAT, "json");
        setSinkConfig(writer, new ClickHouseSinkConfig(jsonProps));
        Map<String, Object> cleanedUnderJson = writer.cleanupExtraFields(payload, table);
        assertEquals(payload, cleanedUnderJson);
        assertTrue(payload.containsKey("known_col"));
        assertTrue(payload.containsKey("unknown_col"));

        Map<String, String> defaultProps = minimalProps();
        setSinkConfig(writer, new ClickHouseSinkConfig(defaultProps));
        Map<String, Object> cleanedUnderDefault = writer.cleanupExtraFields(payload, table);
        assertEquals(Map.of("known_col", "value"), cleanedUnderDefault);
        assertTrue(payload.containsKey("known_col"));
        assertTrue(payload.containsKey("unknown_col"));
    }

    @Test
    @DisplayName("JSON-02 (Docker-free, D-11): getTable under explicit JSON returns a synthetic Table resolved through topic2TableMap, without ever reaching the never-built helper client")
    void getTableUnderExplicitJsonReturnsSyntheticRemappedTableWithoutHelperClient() throws Exception {
        ClickHouseWriter writer = new ClickHouseWriter(new SinkTaskStatistics(null, 0));
        Map<String, String> jsonProps = minimalProps();
        jsonProps.put(ClickHouseSinkConfig.INSERT_FORMAT, "json");
        jsonProps.put(ClickHouseSinkConfig.TABLE_MAPPING, "remapped_topic=remapped_table");
        setSinkConfig(writer, new ClickHouseSinkConfig(jsonProps));

        Table remapped = writer.getTable("default", "remapped_topic");
        assertTrue(remapped.getRootColumnsList().isEmpty());
        assertFalse(remapped.hasDefaults());
        assertEquals("remapped_table", remapped.getCleanName());

        Table unmapped = writer.getTable("default", "unmapped_topic");
        assertEquals("unmapped_topic", unmapped.getCleanName());

        // Negative half: under a default (non-explicit) config, shouldSkipTableMetadata() is
        // false, so getTable falls through to updateMapping()'s never-built chc and dies with an
        // NPE instead of passing vacuously - proof the guard, not the config shape, is why the
        // positive assertions above hold.
        ClickHouseWriter defaultWriter = new ClickHouseWriter(new SinkTaskStatistics(null, 0));
        setSinkConfig(defaultWriter, new ClickHouseSinkConfig(minimalProps()));
        assertThrows(NullPointerException.class, () -> defaultWriter.getTable("default", "remapped_topic"));
    }

    @Test
    @DisplayName("JSON-02 (Docker-free, D-11): updateMapping under explicit JSON short-circuits and never strands the in-progress flag across repeated calls")
    void updateMappingUnderExplicitJsonShortCircuitsAndLeavesTheInProgressFlagClear() throws Exception {
        ClickHouseWriter writer = new ClickHouseWriter(new SinkTaskStatistics(null, 0));
        Map<String, String> jsonProps = minimalProps();
        jsonProps.put(ClickHouseSinkConfig.INSERT_FORMAT, "json");
        setSinkConfig(writer, new ClickHouseSinkConfig(jsonProps));

        assertFalse(writer.updateMapping("default"));
        assertTrue(writer.getMapping().isEmpty());

        // The second consecutive call is the whole point of this test: if the guard were placed
        // below isUpdateMappingRunning.compareAndSet(false, true), the first call would strand
        // the flag true and this second call would silently take upstream's "update already in
        // progress" branch instead of the explicit-JSON guard - also returning false, so the
        // guard's own effect (never touching chc/mapping) is what the getMapping() assertion below
        // actually proves.
        assertFalse(writer.updateMapping("default"));
        assertTrue(writer.getMapping().isEmpty());
    }

    /**
     * Test-only probe that isolates {@code doInsert}'s {@code auto.evolve} guard (CR-01) from
     * table resolution: {@link #getTable} always returns the fixed {@code stubTable} instead of
     * touching the never-built {@code chc}, and {@link #evolveTableSchema} records whether it was
     * invoked instead of running the real DDL/{@code describeTable} logic.
     */
    private static final class RecordingAutoEvolveWriter extends ClickHouseWriter {
        private final Table stubTable;
        private boolean evolveTableSchemaCalled = false;

        RecordingAutoEvolveWriter(SinkTaskStatistics statistics, Table stubTable) {
            super(statistics);
            this.stubTable = stubTable;
        }

        @Override
        protected Table getTable(String database, String topic) {
            return stubTable;
        }

        @Override
        protected Table evolveTableSchema(Table table, Map<String, Schema> allFields) {
            evolveTableSchemaCalled = true;
            return table;
        }
    }

    private static Record schemaRecordWithOneField(String topic, String database) {
        return recordWithOneField(SchemaType.SCHEMA, topic, database);
    }

    private static Record recordWithOneField(SchemaType schemaType, String topic, String database) {
        org.apache.kafka.connect.data.Field field =
                new org.apache.kafka.connect.data.Field("new_column", 0, Schema.STRING_SCHEMA);
        return new Record(schemaType, new OffsetContainer(topic, 0, 0), List.of(field), null, database, null);
    }

    @Test
    @DisplayName("CR-01: doInsert's auto.evolve branch is skipped under explicit JSON metadata-free mode, so evolveTableSchema is never reached")
    void autoEvolveIsSkippedUnderExplicitJsonMetadataFreeMode() throws Exception {
        Table stubTable = new Table("default", "auto_evolve_topic", false, List.of(), 0);
        RecordingAutoEvolveWriter writer = new RecordingAutoEvolveWriter(new SinkTaskStatistics(null, 0), stubTable);

        Map<String, String> jsonProps = minimalProps();
        jsonProps.put(ClickHouseSinkConfig.INSERT_FORMAT, "json");
        jsonProps.put(ClickHouseSinkConfig.AUTO_EVOLVE, "true");
        jsonProps.put("bypassRowBinary", "true");
        setSinkConfig(writer, new ClickHouseSinkConfig(jsonProps));

        List<Record> records = List.of(schemaRecordWithOneField("auto_evolve_topic", "default"));
        QueryIdentifier queryId = new QueryIdentifier("auto_evolve_topic", "test-query-id");

        // evolveTableSchema is skipped by the CR-01 guard, so doInsert falls straight through to
        // doInsertBatch, which still needs the never-built chc for the actual insert and dies with
        // an NPE there instead of passing vacuously - proof the guard, not an unrelated early
        // failure, is why evolveTableSchemaCalled stays false below.
        assertThrows(NullPointerException.class, () -> writer.doInsert(records, queryId));
        assertFalse(writer.evolveTableSchemaCalled);
    }

    @Test
    @DisplayName("CR-01: doInsert's auto.evolve branch still runs under a non-explicit insertFormat, proving the new guard does not over-fire")
    void autoEvolveStillRunsUnderNonExplicitInsertFormat() throws Exception {
        Table stubTable = new Table("default", "auto_evolve_topic", false, List.of(), 0);
        RecordingAutoEvolveWriter writer = new RecordingAutoEvolveWriter(new SinkTaskStatistics(null, 0), stubTable);

        Map<String, String> defaultProps = minimalProps();
        defaultProps.put(ClickHouseSinkConfig.AUTO_EVOLVE, "true");
        defaultProps.put("bypassRowBinary", "true");
        setSinkConfig(writer, new ClickHouseSinkConfig(defaultProps));

        List<Record> records = List.of(schemaRecordWithOneField("auto_evolve_topic", "default"));
        QueryIdentifier queryId = new QueryIdentifier("auto_evolve_topic", "test-query-id");

        // Under a non-explicit insertFormat, shouldSkipTableMetadata() is false, so the guard must
        // not suppress the existing evolve path - doInsertBatch still NPEs on the never-built chc
        // afterward, but evolveTableSchemaCalled being true here is what proves the guard didn't
        // over-fire.
        assertThrows(NullPointerException.class, () -> writer.doInsert(records, queryId));
        assertTrue(writer.evolveTableSchemaCalled);
    }

    /**
     * Test-only probe for {@code doInsertBatch}'s routing: both insert hooks are replaced by
     * counters, so a routing test asserts which hook ran without a ClickHouse server.
     */
    private static final class RecordingRouteWriter extends ClickHouseWriter {
        private final Table stubTable;
        private int jsonCalls = 0;
        private int rawBinaryCalls = 0;

        RecordingRouteWriter(SinkTaskStatistics statistics, Table stubTable) {
            super(statistics);
            this.stubTable = stubTable;
        }

        @Override
        protected Table getTable(String database, String topic) {
            return stubTable;
        }

        @Override
        protected void doInsertJson(List<Record> records, Table table, QueryIdentifier queryId) {
            jsonCalls++;
        }

        @Override
        protected void doInsertRawBinary(
                List<Record> records, Table table, QueryIdentifier queryId, boolean supportDefaults, boolean retry) {
            rawBinaryCalls++;
        }
    }

    private static Stream<Arguments> routingCases() {
        return Stream.of(
                Arguments.of("SCHEMA + insertFormat=json -> JSON", SchemaType.SCHEMA, "json", null, true),
                Arguments.of("DEBEZIUM_CDC + insertFormat=json -> JSON", SchemaType.DEBEZIUM_CDC, "json", null, true),
                Arguments.of("SCHEMA + insertFormat unset -> RowBinary", SchemaType.SCHEMA, null, null, false),
                Arguments.of("DEBEZIUM_CDC + insertFormat unset -> RowBinary", SchemaType.DEBEZIUM_CDC, null, null, false),
                Arguments.of("SCHEMA + insertFormat=none -> RowBinary", SchemaType.SCHEMA, "none", null, false),
                Arguments.of("SCHEMA + bypassRowBinary=true, insertFormat unset -> JSON", SchemaType.SCHEMA, null, "true", true),
                Arguments.of("SCHEMA + insertFormat=json + bypassRowBinary=true -> JSON", SchemaType.SCHEMA, "json", "true", true));
    }

    @ParameterizedTest(name = "JSONP-05: {0}")
    @MethodSource("routingCases")
    @DisplayName("JSONP-05: doInsertBatch routes SCHEMA and DEBEZIUM_CDC records to the JSON writer when bypassRowBinary or an explicit insertFormat=json asks for it, and to RowBinary otherwise")
    void schemaAndCdcRecordsRouteToJsonOnlyWhenMetadataFreeOrBypassed(
            String caseName, SchemaType schemaType, String insertFormat, String bypassRowBinary, boolean expectJson)
            throws Exception {
        Table stubTable = new Table("default", "route_topic", false, List.of(), 0);
        RecordingRouteWriter writer = new RecordingRouteWriter(new SinkTaskStatistics(null, 0), stubTable);

        Map<String, String> props = minimalProps();
        if (insertFormat != null) {
            props.put(ClickHouseSinkConfig.INSERT_FORMAT, insertFormat);
        }
        if (bypassRowBinary != null) {
            props.put("bypassRowBinary", bypassRowBinary);
        }
        setSinkConfig(writer, new ClickHouseSinkConfig(props));

        List<Record> records = List.of(recordWithOneField(schemaType, "route_topic", "default"));
        writer.doInsert(records, new QueryIdentifier("route_topic", "test-query-id"));

        assertEquals(expectJson ? 1 : 0, writer.jsonCalls, caseName + ": doInsertJson calls");
        assertEquals(expectJson ? 0 : 1, writer.rawBinaryCalls, caseName + ": doInsertRawBinary calls");
    }
}
