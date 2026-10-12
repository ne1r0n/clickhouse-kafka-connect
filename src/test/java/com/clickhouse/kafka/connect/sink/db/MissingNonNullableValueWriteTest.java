package com.clickhouse.kafka.connect.sink.db;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.clickhouse.kafka.connect.sink.data.Data;
import com.clickhouse.kafka.connect.sink.db.mapping.Column;
import com.clickhouse.kafka.connect.sink.db.mapping.Type;
import com.clickhouse.kafka.connect.util.jmx.SinkTaskStatistics;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import org.apache.kafka.connect.data.Schema;
import org.apache.kafka.connect.data.SchemaBuilder;
import org.apache.kafka.connect.data.Struct;
import org.apache.kafka.connect.errors.DataException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Docker-free pins for WRT-10 (13-REVIEW WR-03). A missing value for a non-nullable column without
 * a default throws a {@link DataException} that names the column and its rendered type; the message
 * is built from {@code Column} metadata only, so it carries no record value. The class is new
 * because {@code NestedNullableCompositeWriteTest} belongs to a later phase. It extends nothing, so
 * that it stays in {@code testUnit}.
 */
public class MissingNonNullableValueWriteTest {

    private static final String SENTINEL = "SENTINELq7Zx41kV";
    private static final String MISSING_VALUE_HINT =
            "): no value provided for a non-nullable column without a default; add the field, make the column Nullable, or give it a DEFAULT";

    private static final byte NO_DEFAULT_MARKER = 0x00;
    private static final byte NULL_MARKER = 0x01;
    private static final byte VARIANT_NULL_DISCRIMINATOR = (byte) 0xFF;

    private static ClickHouseWriter newWriter() {
        return new ClickHouseWriter(new SinkTaskStatistics(null, 0));
    }

    private static Column int32Column(String name) {
        return Column.builder().name(name).type(Type.INT32).build();
    }

    private static Column arrayOfTupleColumn() {
        Column tuple = Column.builder()
                .name("at")
                .type(Type.TUPLE)
                .tupleFields(List.of(
                        int32Column("at.a"),
                        Column.builder().name("at.b").type(Type.STRING).build()))
                .build();
        return Column.builder().name("at").type(Type.ARRAY).arrayType(tuple).build();
    }

    private static Data arrayOfElements(Schema elementSchema, List<?> elements) {
        return new Data(SchemaBuilder.array(elementSchema).build(), elements);
    }

    private static Struct elementWithOnlyB() {
        Schema elementSchema = SchemaBuilder.struct().field("b", Schema.STRING_SCHEMA).build();
        return new Struct(elementSchema).put("b", SENTINEL);
    }

    private static byte[] writeMissingValue(Column col, boolean defaultsSupport) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        newWriter().doWriteCol(null, false, col, out, defaultsSupport);
        return out.toByteArray();
    }

    @ParameterizedTest(name = "WRT-10: defaultsSupport={0}")
    @ValueSource(booleans = {false, true})
    @DisplayName("WRT-10: a missing value for a flat non-nullable column without a default throws a DataException naming the column and its rendered type")
    void missingFlatColumnThrowsDataExceptionNamingColumnAndType(boolean defaultsSupport) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        DataException ex = assertThrows(
                DataException.class,
                () -> newWriter().doWriteCol(null, false, int32Column("required_id"), out, defaultsSupport));

        assertEquals("column 'required_id' (INT32" + MISSING_VALUE_HINT, ex.getMessage());
        assertEquals(0, out.size(), "nothing may be written for the missing column");
    }

    @ParameterizedTest(name = "WRT-10: defaultsSupport={0}")
    @ValueSource(booleans = {false, true})
    @DisplayName("WRT-10: a missing field inside Array(Tuple(...)) names the field's dotted path, never its container, and no record value")
    void missingTupleFieldNamesDottedPathWithoutRecordValue(boolean defaultsSupport) {
        Data value = arrayOfElements(elementWithOnlyB().schema(), List.of(elementWithOnlyB()));
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        DataException ex = assertThrows(
                DataException.class,
                () -> newWriter().doWriteColValue(arrayOfTupleColumn(), out, value, defaultsSupport));

        assertEquals("column 'at.a' (INT32" + MISSING_VALUE_HINT, ex.getMessage());
        for (Throwable t = ex; t != null; t = t.getCause()) {
            assertFalse(String.valueOf(t.getMessage()).contains(SENTINEL), "the message must carry no record value");
        }
    }

    @Test
    @DisplayName("WRT-10: an empty Array(Tuple(...)) value writes a zero-length array and does not throw")
    void emptyArrayOfTupleWritesZeroLength() {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Data empty = arrayOfElements(elementWithOnlyB().schema(), List.of());

        assertDoesNotThrow(() -> newWriter().doWriteColValue(arrayOfTupleColumn(), out, empty, false));

        assertArrayEquals(new byte[] {0x00}, out.toByteArray());
    }

    @ParameterizedTest(name = "WRT-10: defaultsSupport={0}")
    @ValueSource(booleans = {false, true})
    @DisplayName("WRT-10: a missing value keeps its bytes for a defaulted, a Nullable and a Variant column")
    void missingValueBranchesOtherThanTheThrowAreUnchanged(boolean defaultsSupport) throws IOException {
        Column defaulted = Column.builder().name("d").type(Type.INT32).hasDefault(true).build();
        Column nullable = Column.builder().name("n").type(Type.INT32).isNullable(true).build();
        Column variant = Column.builder().name("v").type(Type.VARIANT).build();

        assertArrayEquals(new byte[] {NULL_MARKER}, writeMissingValue(defaulted, defaultsSupport));
        assertArrayEquals(
                defaultsSupport ? new byte[] {NO_DEFAULT_MARKER, NULL_MARKER} : new byte[] {NULL_MARKER},
                writeMissingValue(nullable, defaultsSupport));
        assertArrayEquals(
                defaultsSupport
                        ? new byte[] {NO_DEFAULT_MARKER, VARIANT_NULL_DISCRIMINATOR}
                        : new byte[] {VARIANT_NULL_DISCRIMINATOR},
                writeMissingValue(variant, defaultsSupport));
    }
}
