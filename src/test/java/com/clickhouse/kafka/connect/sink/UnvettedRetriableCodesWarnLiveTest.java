package com.clickhouse.kafka.connect.sink;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.clickhouse.kafka.connect.sink.db.helper.ClickHouseHelperClient;
import com.clickhouse.kafka.connect.sink.helper.ClickHouseTestHelpers;
import com.clickhouse.kafka.connect.sink.util.LogCapture;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.apache.kafka.connect.sink.SinkRecord;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;

/**
 * A task started with an unvetted additional retriable code still starts and inserts on ClickHouse,
 * and logs the one WARN.
 */
public class UnvettedRetriableCodesWarnLiveTest extends ClickHouseBase {

    private static final String WARN_LEVEL = "WARN";
    private static final String UNVETTED_CODE = "60";
    private static final String EXPECTED_WARN =
            "additionalRetriableErrorCodes lists [60], which the connector does not treat as transient. "
                    + "A batch that fails with such a code is redelivered with no retry budget and outside "
                    + "errors.tolerance and the dead letter queue, so a permanent error blocks the partition "
                    + "until the setting is changed.";
    private static final String WARN_COUNT = "unvetted-code WARN count at start-up";
    private static final String WARN_TEXT = "unvetted-code WARN text";
    private static final String ROW_COUNT = "rows read back after the insert";
    private static final String ID_COLUMN = "id";
    private static final String STR_COLUMN = "str";
    private static final int RECORD_COUNT = 5;

    @Test
    void startupWithAnUnvettedCodeWarnsAndStillInserts(TestInfo testInfo) throws Exception {
        String tableName = createTopicName("unvetted_codes_warn");
        Map<String, String> props = getBaseProps();
        ClickHouseHelperClient chc = ClickHouseTestHelpers.createClient(props);
        ClickHouseTestHelpers.executeQueryIgnoreResult(chc, String.format(
                "CREATE TABLE %s (id UInt32, str String) ENGINE = MergeTree ORDER BY id", tableName));
        props.put(ClickHouseSinkConfig.ADDITIONAL_RETRIABLE_ERROR_CODES, UNVETTED_CODE);
        props.put("name", uniqueConnectorName(testInfo));

        try {
            List<SinkRecord> records = new ArrayList<>();
            for (int i = 1; i <= RECORD_COUNT; i++) {
                Map<String, Object> value = new LinkedHashMap<>();
                value.put(ID_COLUMN, i);
                value.put(STR_COLUMN, "unvetted" + i);
                records.add(new SinkRecord(tableName, 0, null, null, null, value, i));
            }

            List<LogCapture.CapturedEvent> warns;
            ClickHouseSinkTask task = new ClickHouseSinkTask();
            try (LogCapture log = LogCapture.attach(ClickHouseSinkTask.class)) {
                try {
                    task.start(props);
                    task.put(records);
                } finally {
                    task.stop();
                }
                warns = log.events(WARN_LEVEL);
            }

            List<JSONObject> rows = ClickHouseTestHelpers.getAllRowsAsJson(chc, tableName);
            rows.sort(Comparator.comparingInt(row -> row.getInt(ID_COLUMN)));
            assertEquals(RECORD_COUNT, rows.size(), ROW_COUNT);
            for (int i = 0; i < RECORD_COUNT; i++) {
                int id = i + 1;
                assertEquals(id, rows.get(i).getInt(ID_COLUMN));
                assertEquals("unvetted" + id, rows.get(i).getString(STR_COLUMN));
            }

            assertEquals(1, warns.size(), WARN_COUNT);
            assertEquals(EXPECTED_WARN, warns.get(0).message(), WARN_TEXT);
        } finally {
            ClickHouseTestHelpers.dropTable(chc, tableName);
        }
    }
}
