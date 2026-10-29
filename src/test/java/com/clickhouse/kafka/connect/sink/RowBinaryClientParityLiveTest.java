package com.clickhouse.kafka.connect.sink;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

import com.clickhouse.client.api.Client;
import com.clickhouse.kafka.connect.sink.data.Record;
import com.clickhouse.kafka.connect.sink.db.CapturingClickHouseWriter;
import com.clickhouse.kafka.connect.sink.db.helper.ClickHouseHelperClient;
import com.clickhouse.kafka.connect.sink.db.mapping.Table;
import com.clickhouse.kafka.connect.sink.helper.ClickHouseTestHelpers;
import com.clickhouse.kafka.connect.util.QueryIdentifier;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import org.apache.kafka.connect.data.Schema;
import org.apache.kafka.connect.data.SchemaBuilder;
import org.apache.kafka.connect.data.Struct;
import org.apache.kafka.connect.sink.SinkRecord;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * CLT-06: the exact RowBinary body the connector hands to the client, for {@link RowBinaryParityCases}
 * in both the plain and the RowBinaryWithDefaults variant, equals the golden SHA-256 list blessed on
 * client 0.9.6. A batch replayed after the client bump therefore carries the bytes the pre-upgrade
 * attempt carried, which is what ClickHouse's block deduplication needs. The bytes are produced by
 * the connector's serialisers and do not depend on the client, so a red run means a writer change or
 * a {@code clickhouse-data} change. To re-bless after an intended writer change, run this class once
 * with {@code ROWBINARY_GOLDEN_BLESS=true} in the environment: it rewrites the golden file and then
 * fails on purpose, so a CI run cannot go green in bless mode.
 */
class RowBinaryClientParityLiveTest extends ClickHouseBase {

    private static final String GOLDEN_RESOURCE = "/rowbinary-golden-sha256.txt";
    private static final Path GOLDEN_FILE = Path.of("src", "test", "resources", "rowbinary-golden-sha256.txt");
    private static final String BLESS_ENVIRONMENT_VARIABLE = "ROWBINARY_GOLDEN_BLESS";
    private static final String FIELD_SEPARATOR = "|";
    private static final String COMMENT = "#";
    private static final String PLAIN = "plain";
    private static final String DEFAULTS = "defaults";
    private static final String SHA_256 = "SHA-256";
    private static final String DEFAULTS_COLUMN = ", d UInt32 DEFAULT 42";

    private static String sha256(byte[] body) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance(SHA_256).digest(body));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    private String captureLine(
            ClickHouseHelperClient chc, ClickHouseSinkConfig csc, RowBinaryParityCases.Case parityCase, boolean withDefaults)
            throws Exception {
        String variant = withDefaults ? DEFAULTS : PLAIN;
        String tableName = createTopicName("clt06_" + parityCase.name() + "_" + variant);
        ClickHouseTestHelpers.dropTable(chc, tableName);
        try {
            ClickHouseTestHelpers.executeQueryIgnoreResult(
                    chc,
                    "CREATE TABLE `" + tableName + "` (id Int32, c " + parityCase.ddlType()
                            + (withDefaults ? DEFAULTS_COLUMN : "") + ") ENGINE = MergeTree ORDER BY tuple()");
            Table table = chc.describeTable(chc.getDatabase(), tableName);
            Schema value = SchemaBuilder.struct().field("id", Schema.INT32_SCHEMA).field("c", parityCase.schema()).build();
            List<Record> records = new ArrayList<>();
            for (int i = 0; i < parityCase.values().size(); i++) {
                Struct struct = new Struct(value);
                struct.put("id", i);
                struct.put("c", parityCase.values().get(i));
                SinkRecord sinkRecord = new SinkRecord(tableName, 0, null, null, value, struct, i);
                records.add(Record.convert(sinkRecord, false, ".", csc.getDatabase(), false));
            }
            CapturingClickHouseWriter writer = new CapturingClickHouseWriter(table, csc);
            writer.doInsert(records, new QueryIdentifier(table.getCleanName(), 0, 0, records.size() - 1, "clt06"));
            CapturingClickHouseWriter.Capture capture = writer.captures().get(0);
            return String.join(
                    FIELD_SEPARATOR,
                    variant,
                    parityCase.name(),
                    capture.route(),
                    String.valueOf(capture.supportDefaults()),
                    String.valueOf(capture.body().length),
                    sha256(capture.body()));
        } finally {
            ClickHouseTestHelpers.dropTable(chc, tableName);
        }
    }

    private List<String> captureAll() throws Exception {
        Map<String, String> props = getBaseProps();
        ClickHouseSinkConfig csc = new ClickHouseSinkConfig(props);
        List<String> lines = new ArrayList<>();
        try (ClickHouseHelperClient chc = ClickHouseTestHelpers.createClient(props)) {
            for (boolean withDefaults : new boolean[] {false, true}) {
                for (RowBinaryParityCases.Case parityCase : RowBinaryParityCases.all()) {
                    lines.add(captureLine(chc, csc, parityCase, withDefaults));
                }
            }
        }
        return lines;
    }

    private static List<String> readGolden() throws IOException {
        try (InputStream in = RowBinaryClientParityLiveTest.class.getResourceAsStream(GOLDEN_RESOURCE)) {
            if (in == null) {
                fail(GOLDEN_RESOURCE + " is missing; bless it with " + BLESS_ENVIRONMENT_VARIABLE + "=true");
            }
            return new String(in.readAllBytes(), StandardCharsets.UTF_8)
                    .lines()
                    .filter(line -> !line.isBlank() && !line.startsWith(COMMENT))
                    .toList();
        }
    }

    private static void bless(List<String> lines) throws IOException {
        List<String> file = new ArrayList<>();
        file.add(COMMENT + " RowBinary bodies of RowBinaryParityCases: variant|case|route|supportDefaults|length|sha256");
        file.add(COMMENT + " blessed on " + Client.class.getPackage().getImplementationVersion());
        file.addAll(lines);
        Files.write(GOLDEN_FILE, file, StandardCharsets.UTF_8);
        fail("golden re-blessed on " + Client.class.getPackage().getImplementationVersion() + "; unset "
                + BLESS_ENVIRONMENT_VARIABLE + " and re-run");
    }

    @Test
    @DisplayName("CLT-06: the RowBinary body of every type case equals the golden hash blessed on client 0.9.6")
    void bodiesMatchTheGolden() throws Exception {
        List<String> actual = captureAll();
        if (Boolean.parseBoolean(System.getenv(BLESS_ENVIRONMENT_VARIABLE))) {
            bless(actual);
        }
        List<String> golden = readGolden();
        List<String> differing = new ArrayList<>();
        for (int i = 0; i < Math.max(golden.size(), actual.size()); i++) {
            String expected = i < golden.size() ? golden.get(i) : "<none>";
            String got = i < actual.size() ? actual.get(i) : "<none>";
            if (!expected.equals(got)) {
                differing.add("golden " + expected + " actual " + got);
            }
        }
        assertEquals(List.of(), differing, "RowBinary body differs from the golden on "
                + Client.class.getPackage().getImplementationVersion());
    }
}
