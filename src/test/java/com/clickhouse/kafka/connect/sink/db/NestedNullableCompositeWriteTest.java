package com.clickhouse.kafka.connect.sink.db;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import com.clickhouse.kafka.connect.sink.data.Data;
import com.clickhouse.kafka.connect.sink.db.mapping.Column;
import com.clickhouse.kafka.connect.sink.db.mapping.Type;
import com.clickhouse.kafka.connect.util.jmx.SinkTaskStatistics;
import java.io.ByteArrayOutputStream;
import org.apache.kafka.connect.data.Schema;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Docker-free unit pins for CR-02: {@code doWriteColValue}'s MAP/TUPLE/VARIANT cases must not NPE
 * when reached with a null {@link Data#getObject()} - the situation a nullable child of an
 * ARRAY/MAP recurses into for a genuine {@code null} entry (e.g. {@code
 * Array(Nullable(Tuple(...)))}, {@code Map(String, Nullable(Map(String, String)))}). The
 * primitive/date/decimal/array cases already null-check and write the RowBinary null marker
 * instead of dereferencing; these three pins prove MAP/TUPLE/VARIANT now do the same instead of
 * throwing a NullPointerException mid-stream. New fork-only class: it does not extend or modify
 * any upstream test class, and it extends nothing (not {@code ClickHouseBase}) so it needs no
 * Docker and is included in {@code testUnit}.
 */
class NestedNullableCompositeWriteTest {

    private static final byte NULL_MARKER = 1;
    private static final byte VARIANT_NULL_DISCRIMINATOR = (byte) 255;

    @Test
    @DisplayName("CR-02: a null MAP value writes the RowBinary null marker instead of NPE-ing on value.getObject()")
    void nullMapValueWritesNullMarker() throws Exception {
        ClickHouseWriter writer = new ClickHouseWriter(new SinkTaskStatistics(null, 0));
        Column column = Column.builder().name("nested_map").type(Type.MAP).isNullable(true).build();
        Data nullData = new Data(Schema.OPTIONAL_STRING_SCHEMA, null);
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        assertDoesNotThrow(() -> writer.doWriteColValue(column, out, nullData, true));

        assertArrayEquals(new byte[] {NULL_MARKER}, out.toByteArray());
    }

    @Test
    @DisplayName("CR-02: a null TUPLE value writes the RowBinary null marker instead of NPE-ing on value.getObject()")
    void nullTupleValueWritesNullMarker() throws Exception {
        ClickHouseWriter writer = new ClickHouseWriter(new SinkTaskStatistics(null, 0));
        Column column = Column.builder().name("nested_tuple").type(Type.TUPLE).isNullable(true).build();
        Data nullData = new Data(Schema.OPTIONAL_STRING_SCHEMA, null);
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        assertDoesNotThrow(() -> writer.doWriteColValue(column, out, nullData, true));

        assertArrayEquals(new byte[] {NULL_MARKER}, out.toByteArray());
    }

    @Test
    @DisplayName("CR-02: a null VARIANT value writes the native null discriminator instead of NPE-ing on value.getObject()")
    void nullVariantValueWritesNullDiscriminator() throws Exception {
        ClickHouseWriter writer = new ClickHouseWriter(new SinkTaskStatistics(null, 0));
        Column column = Column.builder().name("nested_variant").type(Type.VARIANT).isNullable(true).build();
        Data nullData = new Data(Schema.OPTIONAL_STRING_SCHEMA, null);
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        assertDoesNotThrow(() -> writer.doWriteColValue(column, out, nullData, true));

        assertArrayEquals(new byte[] {VARIANT_NULL_DISCRIMINATOR}, out.toByteArray());
    }
}
