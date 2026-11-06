package com.clickhouse.kafka.connect.sink.db;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.clickhouse.client.ClickHouseException;
import com.clickhouse.client.api.ServerException;
import com.clickhouse.kafka.connect.sink.ClickHouseSinkConfig;
import com.clickhouse.kafka.connect.sink.data.Data;
import com.clickhouse.kafka.connect.sink.data.Record;
import com.clickhouse.kafka.connect.sink.data.SchemaType;
import com.clickhouse.kafka.connect.sink.db.mapping.Column;
import com.clickhouse.kafka.connect.sink.db.mapping.Table;
import com.clickhouse.kafka.connect.sink.db.mapping.Type;
import com.clickhouse.kafka.connect.sink.dlq.InMemoryDLQ;
import com.clickhouse.kafka.connect.sink.util.LogCapture;
import com.clickhouse.kafka.connect.sink.util.ThrowableTexts;
import com.clickhouse.kafka.connect.util.DataJson;
import com.clickhouse.kafka.connect.util.QueryIdentifier;
import com.clickhouse.kafka.connect.util.Utils;
import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonSerializable;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.jsontype.TypeSerializer;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.function.Supplier;
import org.apache.kafka.connect.data.Schema;
import org.apache.kafka.connect.data.SchemaBuilder;
import org.apache.kafka.connect.data.Struct;
import org.apache.kafka.connect.errors.ConnectException;
import org.apache.kafka.connect.errors.DataException;
import org.apache.kafka.connect.errors.RetriableException;
import org.apache.kafka.connect.sink.SinkRecord;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

/**
 * Pins the JSON insert route: a serialization failure that leaves {@code writeRecordsJson} carries
 * no record value on any surface (messages, stack trace, DLQ headers, logs) while every fact retry
 * classification reads stays. A Jackson message can quote record data (a reference chain names a
 * Map key, and a wrapped exception's message is copied), so a chain holding a Jackson link is
 * rebuilt value-free; a chain without one is rethrown as it is.
 *
 * <p>Each shape is driven through the real {@code writeRecordsJson} of a {@link
 * CapturingClickHouseWriter} whose value carries a scripted serializer failure. The oracle is the
 * exception the serializer itself throws on the same value. The class extends nothing, carries no
 * tag and uses no mocking library, so it runs without Docker.
 */
public class JsonRouteValueFreeUnitTest {

    public static final String SENTINEL = "RECORD-VALUE-SENTINEL-45";
    public static final String TOPIC = "json-topic";
    public static final String COLUMN = "payload";
    public static final String SCRIPTED_KEY = "k";
    public static final String BOOM = "boom";
    private static final String QUERY_ID_TEXT = "json-route-query";
    private static final String WITHHELD_NOTE = " (message withheld: may contain record data)";
    private static final String JSON_ROW_FAILURE_TEXT = "Failed to serialize a record as JSON for table `%s`: %s";
    private static final String READ_TIMEOUT_PREFIX = "Read timed out after";
    private static final String WRITE_TIMEOUT_PREFIX = "Write timed out after";
    private static final int BATCH_SIZE = 3;

    /** A value whose Jackson serialization throws the scripted failure, whatever the type handling. */
    public static final class Scripted implements JsonSerializable {
        private final Exception failure;

        public Scripted(Exception failure) {
            this.failure = failure;
        }

        @Override
        public void serialize(JsonGenerator gen, SerializerProvider provider) throws IOException {
            fail();
        }

        @Override
        public void serializeWithType(JsonGenerator gen, SerializerProvider provider, TypeSerializer typeSer)
                throws IOException {
            fail();
        }

        private void fail() throws IOException {
            if (failure instanceof IOException io) {
                throw io;
            }
            throw (RuntimeException) failure;
        }

        @Override
        public String toString() {
            return "scripted";
        }
    }

    public enum Route {
        SCHEMALESS,
        SCHEMA_BYPASS,
        METADATA_FREE_STRUCT,
        METADATA_FREE_CDC,
        CDC_BYPASS_NULL_DATA
    }

    /** The pinned shapes; a control has no Jackson link and no value. */
    enum Shape {
        JSON_SCRIPTED_RUNTIME(Route.SCHEMALESS, SCRIPTED_KEY, () -> new RuntimeException(SENTINEL)),
        JSON_KEY_IN_REFERENCE_CHAIN(Route.SCHEMALESS, SENTINEL, () -> new RuntimeException(BOOM)),
        JSON_SCRIPTED_JACKSON_ALONE(Route.SCHEMALESS, SCRIPTED_KEY, () -> new JsonMappingException(SENTINEL)),
        JSON_SCRIPTED_JACKSON_OVER_SOCKET_TIMEOUT(
                Route.SCHEMALESS,
                SCRIPTED_KEY,
                () -> new JsonMappingException(SENTINEL, new SocketTimeoutException("timed out"))),
        JSON_NO_JACKSON_NULL_DATA_CONTROL(Route.CDC_BYPASS_NULL_DATA, SCRIPTED_KEY, () -> new RuntimeException(BOOM)),
        JSON_SCHEMA_BYPASS_KEY(Route.SCHEMA_BYPASS, SENTINEL, () -> new RuntimeException(BOOM)),
        JSON_METADATA_FREE_STRUCT_VALUE(Route.METADATA_FREE_STRUCT, SCRIPTED_KEY, () -> new RuntimeException(SENTINEL)),
        JSON_METADATA_FREE_CDC_VALUE(Route.METADATA_FREE_CDC, SCRIPTED_KEY, () -> new RuntimeException(SENTINEL));

        final Route route;
        final String key;
        private final Supplier<Exception> failure;

        Shape(Route route, String key, Supplier<Exception> failure) {
            this.route = route;
            this.key = key;
            this.failure = failure;
        }

        boolean isControl() {
            return route == Route.CDC_BYPASS_NULL_DATA;
        }
    }

    enum Outcome {
        RETRIABLE,
        DATA_EXCEPTION,
        RUNTIME_WRAP,
        RETURNS
    }

    /** One batch: its records, the config of its route, the table and an oracle for the serializer's own failure. */
    private record Fixture(List<Record> records, ClickHouseSinkConfig csc, Table table, Callable<Object> original) {}

    public static ClickHouseSinkConfig config(Route route, String errorsTolerance) {
        Map<String, String> props = new HashMap<>();
        props.put(ClickHouseSinkConfig.HOSTNAME, "localhost");
        props.put(ClickHouseSinkConfig.PORT, "8123");
        props.put(ClickHouseSinkConfig.DATABASE, "default");
        props.put(ClickHouseSinkConfig.USERNAME, "default");
        props.put(ClickHouseSinkConfig.PASSWORD, "");
        if (errorsTolerance != null) {
            props.put(ClickHouseSinkConfig.ERRORS_TOLERANCE, errorsTolerance);
        }
        if (route == Route.SCHEMA_BYPASS || route == Route.CDC_BYPASS_NULL_DATA) {
            props.put("bypassRowBinary", "true");
        } else if (route == Route.METADATA_FREE_STRUCT || route == Route.METADATA_FREE_CDC) {
            props.put(ClickHouseSinkConfig.INSERT_FORMAT, "json");
        }
        return new ClickHouseSinkConfig(props);
    }

    public static Table table() {
        return new Table("default", TOPIC, false, List.of(Column.builder().name(COLUMN).type(Type.JSON).build()), 1);
    }

    private static final Schema MAP_SCHEMA = SchemaBuilder.map(Schema.STRING_SCHEMA, Schema.STRING_SCHEMA).build();
    private static final Schema STRUCT_SCHEMA = SchemaBuilder.struct().field(COLUMN, MAP_SCHEMA).build();

    /** The map gains its entries after {@code Struct.put}, which validates values against the schema. */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private static Fixture failingBatch(Shape shape) {
        Scripted scripted = new Scripted(shape.failure.get());
        List<Record> records = new ArrayList<>();
        Callable<Object> original = null;
        for (int offset = 0; offset < BATCH_SIZE; offset++) {
            Map<String, Object> inner = new HashMap<>();
            Record record;
            Callable<Object> oracle;
            switch (shape.route) {
                case SCHEMALESS -> {
                    inner.put(shape.key, scripted);
                    Map<String, Object> value = new HashMap<>();
                    value.put(COLUMN, inner);
                    SinkRecord sinkRecord = new SinkRecord(TOPIC, 0, null, null, null, value, offset);
                    record = Record.convert(sinkRecord, false, ".", "default", false);
                    oracle = () -> DataJson.OBJECT_MAPPER.writeValueAsBytes(Map.of(COLUMN, inner));
                }
                case SCHEMA_BYPASS, METADATA_FREE_STRUCT -> {
                    Struct struct = new Struct(STRUCT_SCHEMA).put(COLUMN, inner);
                    inner.put(shape.key, scripted);
                    SinkRecord sinkRecord = new SinkRecord(TOPIC, 0, null, null, STRUCT_SCHEMA, struct, offset);
                    record = Record.newRecord(
                            SchemaType.SCHEMA, TOPIC, 0, offset, STRUCT_SCHEMA.fields(), new HashMap<>(), "default", sinkRecord);
                    oracle = shape.route == Route.SCHEMA_BYPASS
                            ? () -> DataJson.OBJECT_MAPPER.writeValueAsBytes(Map.of(COLUMN, inner))
                            : () -> MetadataFreeJsonWriter.structRow(struct);
                }
                case METADATA_FREE_CDC -> {
                    inner.put(shape.key, scripted);
                    Map<String, Data> jsonMap = new HashMap<>();
                    jsonMap.put(COLUMN, new Data(MAP_SCHEMA, inner));
                    SinkRecord sinkRecord = new SinkRecord(TOPIC, 0, null, null, Schema.STRING_SCHEMA, "v", offset);
                    record = Record.newRecord(SchemaType.DEBEZIUM_CDC, TOPIC, 0, offset, List.of(), jsonMap, "default", sinkRecord);
                    oracle = () -> MetadataFreeJsonWriter.cdcRow(jsonMap);
                }
                default -> {
                    Map<String, Data> jsonMap = new HashMap<>();
                    jsonMap.put(COLUMN, null);
                    SinkRecord sinkRecord = new SinkRecord(TOPIC, 0, null, null, Schema.STRING_SCHEMA, "v", offset);
                    record = Record.newRecord(SchemaType.DEBEZIUM_CDC, TOPIC, 0, offset, List.of(), jsonMap, "default", sinkRecord);
                    oracle = () -> {
                        Map<String, Object> data = new HashMap<>();
                        jsonMap.forEach((k, v) -> data.put(k, v.getObject()));
                        return data;
                    };
                }
            }
            records.add(record);
            if (offset == 0) {
                original = oracle;
            }
        }
        return new Fixture(records, config(shape.route, null), table(), original);
    }

    /** What one {@code doInsert} threw, what the serializer itself throws, and the log texts written meanwhile. */
    private record Captured(Throwable thrown, Throwable original, List<String> logTexts, Table table) {}

    private static Throwable catching(Callable<Object> action) {
        try {
            action.call();
            return null;
        } catch (Throwable t) {
            return t;
        }
    }

    private static Captured drive(Shape shape) {
        Fixture fixture = failingBatch(shape);
        CapturingClickHouseWriter writer = new CapturingClickHouseWriter(fixture.table(), fixture.csc());
        try (LogCapture log = LogCapture.attach(ClickHouseWriter.class)) {
            Throwable thrown = catching(() -> {
                writer.doInsert(fixture.records(), new QueryIdentifier(TOPIC, QUERY_ID_TEXT), null);
                return null;
            });
            assertNotNull(thrown, "the scripted failure must leave doInsert");
            Throwable original = catching(fixture.original());
            assertNotNull(original, "the oracle must fail the same way");
            return new Captured(thrown, original, log.allTexts(), fixture.table());
        }
    }

    private static String withheld(Throwable link) {
        return link.getClass().getName() + WITHHELD_NOTE;
    }

    private static boolean isTrustedIoLink(Throwable link) {
        return link instanceof IOException && !(link instanceof JacksonException);
    }

    private static Throwable trustedSuffixStart(List<Throwable> chain) {
        Throwable start = null;
        for (int i = chain.size() - 1; i >= 0 && isTrustedIoLink(chain.get(i)); i--) {
            start = chain.get(i);
        }
        return start;
    }

    private static boolean holdsJackson(Throwable top) {
        for (Throwable link : ThrowableTexts.causeChain(top)) {
            if (link instanceof JacksonException) {
                return true;
            }
        }
        return false;
    }

    private static boolean anyMessageContains(Throwable top, String text) {
        for (Throwable link : ThrowableTexts.causeChain(top)) {
            if (link.getMessage() != null && link.getMessage().contains(text)) {
                return true;
            }
        }
        return false;
    }

    @ParameterizedTest(name = "rawFailureCarriesTheValue {0}")
    @EnumSource(Shape.class)
    void rawFailureCarriesTheValue(Shape shape) {
        Throwable original = drive(shape).original();

        if (shape.isControl()) {
            assertFalse(anyMessageContains(original, SENTINEL), "the control carries no value");
            assertFalse(holdsJackson(original), "the control has no Jackson link");
        } else {
            assertTrue(anyMessageContains(original, SENTINEL), "self-check: the serializer's own failure quotes the value");
        }
    }

    @ParameterizedTest(name = "isValueFreeAndKeepsTheTrustedSuffix {0}")
    @EnumSource(Shape.class)
    void isValueFreeAndKeepsTheTrustedSuffix(Shape shape) {
        Captured captured = drive(shape);
        Throwable thrown = captured.thrown();

        if (shape.isControl()) {
            assertSame(NullPointerException.class, thrown.getClass(), "a chain without a Jackson link is rethrown as it is");
            assertNull(thrown.getCause());
            assertFalse(holdsJackson(thrown));
            assertTrue(String.valueOf(thrown.getMessage()).contains("getObject"));
            return;
        }
        List<Throwable> original = ThrowableTexts.causeChain(captured.original());
        List<Throwable> rebuilt = ThrowableTexts.causeChain(thrown);

        for (Throwable link : rebuilt) {
            assertFalse(link instanceof JacksonException, "no Jackson link may be left in the thrown chain");
        }
        assertEquals(
                String.format(JSON_ROW_FAILURE_TEXT, captured.table().getCleanName(), captured.original().getClass().getName()),
                thrown.getMessage());
        Throwable suffixStart = trustedSuffixStart(original);
        if (suffixStart != null) {
            int at = -1;
            for (int i = 0; i < rebuilt.size(); i++) {
                if (rebuilt.get(i) == suffixStart) {
                    at = i;
                }
            }
            assertTrue(at > 0, "the first link of the trusted suffix must be kept as the same object, below the top");
            Set<Throwable> originals = Collections.newSetFromMap(new IdentityHashMap<>());
            originals.addAll(original);
            for (int i = 0; i < at; i++) {
                assertFalse(originals.contains(rebuilt.get(i)), "a link above the kept suffix must be a stand-in");
            }
        } else if (original.size() == 1) {
            assertEquals(1, rebuilt.size(), "a lone Jackson exception becomes the value-free top alone");
            assertSame(IOException.class, thrown.getClass());
            assertNull(thrown.getCause());
        } else {
            Throwable originalRoot = original.get(original.size() - 1);
            Throwable root = rebuilt.get(rebuilt.size() - 1);
            assertNull(root.getCause());
            if (originalRoot instanceof JacksonException) {
                assertSame(IOException.class, root.getClass());
                String message = originalRoot.getMessage();
                String prefix = "";
                if (message.startsWith(READ_TIMEOUT_PREFIX)) {
                    prefix = READ_TIMEOUT_PREFIX + " ";
                } else if (message.startsWith(WRITE_TIMEOUT_PREFIX)) {
                    prefix = WRITE_TIMEOUT_PREFIX + " ";
                }
                assertEquals(prefix + withheld(originalRoot), root.getMessage());
            } else {
                assertSame(originalRoot.getClass(), root.getClass(), "a non-Jackson root keeps its kind");
                assertEquals(withheld(originalRoot), root.getMessage());
            }
        }
    }

    /** The classification-read link kinds of a chain, top first. */
    private static List<String> classifiedKinds(Throwable top) {
        List<String> kinds = new ArrayList<>();
        for (Throwable link : ThrowableTexts.causeChain(top)) {
            if (link instanceof RetriableException) {
                kinds.add("Retriable");
            } else if (link instanceof DataException) {
                kinds.add("Data");
            } else if (link instanceof ConnectException) {
                kinds.add("Connect");
            } else if (link instanceof ClickHouseException v1) {
                kinds.add("V1:" + v1.getErrorCode());
            } else if (link instanceof ServerException v2) {
                kinds.add("V2:" + v2.getCode());
            }
        }
        return kinds;
    }

    /** The classified root's type family and its timeout-prefix fact, as handleException reads them. */
    private static String rootFacts(Exception top) {
        Exception root = Utils.getRootCause(top, true);
        String family;
        if (root instanceof ClickHouseException v1) {
            family = "V1:" + v1.getErrorCode();
        } else if (root instanceof ServerException v2) {
            family = "V2:" + v2.getCode();
        } else if (root instanceof SocketTimeoutException) {
            family = "SocketTimeout";
        } else if (root instanceof UnknownHostException) {
            family = "UnknownHost";
        } else if (root instanceof SocketException) {
            family = "Socket";
        } else if (root instanceof IOException) {
            family = "IO";
        } else {
            family = "Other";
        }
        String message = root == null ? null : root.getMessage();
        boolean timeoutPrefix = message != null
                && (message.indexOf(READ_TIMEOUT_PREFIX) == 0 || message.indexOf(WRITE_TIMEOUT_PREFIX) == 0);
        return family + " timeoutPrefix=" + timeoutPrefix;
    }

    @ParameterizedTest(name = "keepsTheClassifiedLinks {0}")
    @EnumSource(Shape.class)
    void keepsTheClassifiedLinks(Shape shape) {
        Captured captured = drive(shape);
        Throwable thrown = captured.thrown();
        Throwable original = captured.original();

        assertEquals(original instanceof IOException, thrown instanceof IOException);
        assertEquals(original instanceof RuntimeException, thrown instanceof RuntimeException);
        assertEquals(classifiedKinds(original), classifiedKinds(thrown));
        assertEquals(rootFacts((Exception) original), rootFacts((Exception) thrown));
    }

    @ParameterizedTest(name = "carriesNoRecordValue {0}")
    @EnumSource(Shape.class)
    void carriesNoRecordValue(Shape shape) {
        Captured captured = drive(shape);
        Throwable thrown = captured.thrown();

        assertFalse(anyMessageContains(thrown, SENTINEL), "no message of the thrown chain may carry the value");
        for (String text : ThrowableTexts.allTexts(thrown)) {
            assertFalse(text.contains(SENTINEL), "a thrown text must carry no record value");
        }
        InMemoryDLQ.ConnectDlqHeaders headers = InMemoryDLQ.connectHeaders(new RuntimeException(QUERY_ID_TEXT, thrown));
        assertFalse(String.valueOf(headers.exceptionMessage()).contains(SENTINEL));
        assertFalse(headers.exceptionStacktrace().contains(SENTINEL));
        for (String text : captured.logTexts()) {
            assertFalse(String.valueOf(text).contains(SENTINEL), "a log text must carry no record value");
        }
    }

    /** What Utils.handleException decides for a failure wrapped the way Processing wraps it. */
    private static Outcome outcome(Throwable writerFailure, boolean clientV2, boolean errorsTolerance, boolean retryOnSocket) {
        Throwable seenByProcessing = writerFailure;
        if (clientV2 && writerFailure instanceof RuntimeException) {
            seenByProcessing = new com.clickhouse.client.api.ClickHouseException(
                    ScriptedWriteFailureWriter.V2_INSERT_FAILED_MESSAGE, writerFailure);
        }
        try {
            Utils.handleException(
                    new RuntimeException(QUERY_ID_TEXT, seenByProcessing),
                    errorsTolerance,
                    retryOnSocket,
                    Set.of(),
                    new ArrayList<>());
            return Outcome.RETURNS;
        } catch (RetriableException e) {
            return Outcome.RETRIABLE;
        } catch (DataException e) {
            return Outcome.DATA_EXCEPTION;
        } catch (Exception e) {
            return Outcome.RUNTIME_WRAP;
        }
    }

    @ParameterizedTest(name = "keepsTheRetryOutcome {0}")
    @EnumSource(Shape.class)
    void keepsTheRetryOutcome(Shape shape) {
        Captured captured = drive(shape);

        for (boolean clientV2 : new boolean[] {false, true}) {
            for (boolean errorsTolerance : new boolean[] {false, true}) {
                for (boolean retryOnSocket : new boolean[] {false, true}) {
                    assertEquals(
                            outcome(captured.original(), clientV2, errorsTolerance, retryOnSocket),
                            outcome(captured.thrown(), clientV2, errorsTolerance, retryOnSocket),
                            "clientV2=" + clientV2 + " errorsTolerance=" + errorsTolerance
                                    + " retryOnSocketException=" + retryOnSocket);
                }
            }
        }
    }

    private static String validValue(int offset) {
        return "value-" + offset;
    }

    private static byte[] concatenated(List<byte[]> rows) {
        ByteArrayOutputStream joined = new ByteArrayOutputStream();
        for (byte[] row : rows) {
            joined.writeBytes(row);
        }
        return joined.toByteArray();
    }

    @Test
    void successPathBytesUnchangedSchemaless() throws Exception {
        List<Record> records = new ArrayList<>();
        List<byte[]> expectedRows = new ArrayList<>();
        for (int offset = 0; offset < BATCH_SIZE; offset++) {
            Map<String, Object> inner = new HashMap<>();
            inner.put(SCRIPTED_KEY, validValue(offset));
            Map<String, Object> value = new HashMap<>();
            value.put(COLUMN, inner);
            records.add(Record.convert(new SinkRecord(TOPIC, 0, null, null, null, value, offset), false, ".", "default", false));
            expectedRows.add(DataJson.OBJECT_MAPPER.writeValueAsBytes(Map.of(COLUMN, inner)));
        }
        CapturingClickHouseWriter writer = new CapturingClickHouseWriter(table(), config(Route.SCHEMALESS, null));

        writer.doInsert(records, new QueryIdentifier(TOPIC, QUERY_ID_TEXT), null);

        assertEquals(1, writer.captures().size());
        assertEquals(CapturingClickHouseWriter.ROUTE_JSON, writer.captures().get(0).route());
        assertArrayEquals(concatenated(expectedRows), writer.captures().get(0).body());
    }

    @Test
    void successPathBytesUnchangedMetadataFree() throws Exception {
        List<Record> records = new ArrayList<>();
        List<byte[]> expectedRows = new ArrayList<>();
        for (int offset = 0; offset < BATCH_SIZE; offset++) {
            Map<String, String> inner = new HashMap<>();
            inner.put(SCRIPTED_KEY, validValue(offset));
            Struct struct = new Struct(STRUCT_SCHEMA).put(COLUMN, inner);
            SinkRecord sinkRecord = new SinkRecord(TOPIC, 0, null, null, STRUCT_SCHEMA, struct, offset);
            records.add(Record.newRecord(
                    SchemaType.SCHEMA, TOPIC, 0, offset, STRUCT_SCHEMA.fields(), new HashMap<>(), "default", sinkRecord));
            expectedRows.add(MetadataFreeJsonWriter.structRow(struct));
        }
        CapturingClickHouseWriter writer = new CapturingClickHouseWriter(table(), config(Route.METADATA_FREE_STRUCT, null));

        writer.doInsert(records, new QueryIdentifier(TOPIC, QUERY_ID_TEXT), null);

        assertEquals(1, writer.captures().size());
        assertEquals(CapturingClickHouseWriter.ROUTE_JSON, writer.captures().get(0).route());
        assertArrayEquals(concatenated(expectedRows), writer.captures().get(0).body());
    }
}
