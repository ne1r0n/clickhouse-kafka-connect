package transforms;

import static org.junit.jupiter.api.Assertions.*;

import com.clickhouse.kafka.connect.transforms.StructToMap;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import org.apache.kafka.connect.data.Schema;
import org.apache.kafka.connect.data.SchemaBuilder;
import org.apache.kafka.connect.data.Struct;
import org.apache.kafka.connect.sink.SinkRecord;
import org.junit.jupiter.api.Test;

public class StructToMapTest {
    @Test
    public void testStructToMapSimple() {
        Schema schema = SchemaBuilder.struct()
                .field("foo", Schema.STRING_SCHEMA)
                .field("bar", Schema.INT32_SCHEMA)
                .build();
        Struct struct = new Struct(schema)
                .put("foo", "abc")
                .put("bar", 123);
        SinkRecord record = new SinkRecord("topic", 0, null, null, schema, struct, 0L);
        StructToMap<SinkRecord> xform = new StructToMap<>();
        xform.configure(new HashMap<>());
        SinkRecord result = xform.apply(record);
        assertTrue(result.value() instanceof Map);
        Map<?,?> map = (Map<?,?>) result.value();
        assertEquals("abc", map.get("foo"));
        assertEquals(123, map.get("bar"));
    }

    @Test
    public void testStructToMapNested() {
        Schema nestedSchema = SchemaBuilder.struct()
                .field("x", Schema.STRING_SCHEMA)
                .build();
        Schema schema = SchemaBuilder.struct()
                .field("foo", Schema.STRING_SCHEMA)
                .field("nested", nestedSchema)
                .build();
        Struct nested = new Struct(nestedSchema).put("x", "val");
        Struct struct = new Struct(schema)
                .put("foo", "abc")
                .put("nested", nested);
        SinkRecord record = new SinkRecord("topic", 0, null, null, schema, struct, 0L);
        StructToMap<SinkRecord> xform = new StructToMap<>();
        xform.configure(new HashMap<>());
        SinkRecord result = xform.apply(record);
        assertTrue(result.value() instanceof Map);
        Map<?,?> map = (Map<?,?>) result.value();
        assertEquals("abc", map.get("foo"));
        assertTrue(map.get("nested") instanceof Map);
        Map<?,?> nestedMap = (Map<?,?>) map.get("nested");
        assertEquals("val", nestedMap.get("x"));
    }

    @Test
    public void testNonStructValue() {
        SinkRecord record = new SinkRecord("topic", 0, null, null, null, "string", 0L);
        StructToMap<SinkRecord> xform = new StructToMap<>();
        xform.configure(new HashMap<>());
        SinkRecord result = xform.apply(record);
        assertEquals("string", result.value());
    }

    @Test
    public void testStructToMapBytesToBase64() {
        Schema schema = SchemaBuilder.struct()
                .field("foo", Schema.BYTES_SCHEMA)
                .field("bar", Schema.STRING_SCHEMA)
                .build();
        byte[] bytes = new byte[] {1, 2, 3, 4};
        Struct struct = new Struct(schema)
                .put("foo", bytes)
                .put("bar", "abc");
        SinkRecord record = new SinkRecord("topic", 0, null, null, schema, struct, 0L);
        StructToMap<SinkRecord> xform = new StructToMap<>();
        Map<String, Object> config = new HashMap<>();
        config.put("bytes.to.base64", true);
        xform.configure(config);
        SinkRecord result = xform.apply(record);
        assertTrue(result.value() instanceof Map);
        Map<?,?> map = (Map<?,?>) result.value();
        assertEquals(Base64.getEncoder().encodeToString(bytes), map.get("foo"));
        assertEquals("abc", map.get("bar"));
    }

    @Test
    public void testStructToMapByteBufferToBase64() {
        Schema schema = SchemaBuilder.struct()
                .field("foo", Schema.BYTES_SCHEMA)
                .build();
        byte[] bytes = new byte[] {10, 20, 30};
        java.nio.ByteBuffer buf = java.nio.ByteBuffer.wrap(bytes);
        Struct struct = new Struct(schema)
                .put("foo", buf);
        SinkRecord record = new SinkRecord("topic", 0, null, null, schema, struct, 0L);
        StructToMap<SinkRecord> xform = new StructToMap<>();
        Map<String, Object> config = new HashMap<>();
        config.put("bytes.to.base64", true);
        xform.configure(config);
        SinkRecord result = xform.apply(record);
        assertTrue(result.value() instanceof Map);
        Map<?,?> map = (Map<?,?>) result.value();
        assertEquals(Base64.getEncoder().encodeToString(bytes), map.get("foo"));
    }

}
