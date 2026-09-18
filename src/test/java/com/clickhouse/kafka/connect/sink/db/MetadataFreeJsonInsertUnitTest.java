package com.clickhouse.kafka.connect.sink.db;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.clickhouse.kafka.connect.sink.ClickHouseSinkConfig;
import com.clickhouse.kafka.connect.sink.db.mapping.Column;
import com.clickhouse.kafka.connect.sink.db.mapping.Table;
import com.clickhouse.kafka.connect.sink.db.mapping.Type;
import com.clickhouse.kafka.connect.util.jmx.SinkTaskStatistics;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
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
 * <p>Disabled pending the JSONP-01 keep/drop decision (Phase 9, D-11): the metadata-free
 * {@code insertFormat=json} bypass this class pins was dropped by the mechanical v1.6.0 merge and
 * has not been re-applied to {@code src/main} in this phase. Every assertion below is left intact
 * so Phase 9 can re-enable this class unchanged if the decision is keep.
 */
@Disabled("Pending JSONP-01 keep/drop decision — Phase 9")
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
        ClickHouseWriter writer = new ClickHouseWriter(new SinkTaskStatistics(0));
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
}
