package transforms;

import static org.junit.jupiter.api.Assertions.*;

import com.clickhouse.kafka.connect.transforms.DropValueSchema;
import java.util.HashMap;
import org.apache.kafka.connect.data.Schema;
import org.apache.kafka.connect.data.SchemaBuilder;
import org.apache.kafka.connect.sink.SinkRecord;
import org.junit.jupiter.api.Test;

public class DropValueSchemaTest {
    @Test
    public void testDropValueSchemaWithSchema() {
        Schema schema = SchemaBuilder.struct().field("foo", Schema.STRING_SCHEMA).build();
        SinkRecord record = new SinkRecord("topic", 0, null, null, schema, "bar", 0L);
        DropValueSchema<SinkRecord> xform = new DropValueSchema<>();
        xform.configure(new HashMap<>());
        SinkRecord result = xform.apply(record);
        assertNull(result.valueSchema());
        assertEquals("bar", result.value());
    }

    @Test
    public void testDropValueSchemaWithoutSchema() {
        SinkRecord record = new SinkRecord("topic", 0, null, null, null, "baz", 0L);
        DropValueSchema<SinkRecord> xform = new DropValueSchema<>();
        xform.configure(new HashMap<>());
        SinkRecord result = xform.apply(record);
        assertNull(result.valueSchema());
        assertEquals("baz", result.value());
    }
}
