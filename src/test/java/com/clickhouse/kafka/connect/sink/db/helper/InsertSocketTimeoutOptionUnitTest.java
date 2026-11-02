package com.clickhouse.kafka.connect.sink.db.helper;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import com.clickhouse.client.api.ClientConfigProperties;
import com.clickhouse.client.api.insert.InsertSettings;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Docker-free cases for {@code ClickHouseHelperClient.setInsertSocketTimeoutV2}. The per-insert
 * option carries the insert timeout unless the operator's {@code jdbcConnectionProperties} names
 * {@code socket_timeout}, with no network and no clock.
 */
class InsertSocketTimeoutOptionUnitTest {
    private static final String PARAMETERIZED_NAME =
            "{displayName}[{index}] jdbcConnectionProperties={0}";
    private static final String LOOPBACK = "127.0.0.1";
    private static final int UNUSED_PORT = 8123;
    private static final long INSERT_TIMEOUT_MS = 300L;
    private static final String SOCKET_TIMEOUT_KEY =
            ClientConfigProperties.SOCKET_OPERATION_TIMEOUT.getKey();
    private static final String PER_INSERT_VALUE = "per-insert socket_timeout";
    private static final String PER_INSERT_ABSENT =
            "no per-insert socket_timeout when the operator named one";
    private static final String OPERATOR_VALUE = "the operator's client-level socket_timeout";
    private static final String CLAMPED = "a timeout above the int range is clamped";

    private static ClickHouseHelperClient helper(String jdbcConnectionProperties) {
        return new ClickHouseHelperClient.ClickHouseClientBuilder(LOOPBACK, UNUSED_PORT, null, null, -1)
                .setJdbcConnectionProperties(jdbcConnectionProperties)
                .build();
    }

    @ParameterizedTest(name = PARAMETERIZED_NAME)
    @ValueSource(strings = {"", "ssl=true&sslmode=strict", "SOCKET_TIMEOUT=5", "=5"})
    void insertCarriesTheInsertTimeoutWhenTheOperatorNamedNoSocketTimeout(
            String jdbcConnectionProperties) {
        try (ClickHouseHelperClient chc = helper(jdbcConnectionProperties)) {
            InsertSettings settings = new InsertSettings();
            chc.setInsertSocketTimeoutV2(settings, INSERT_TIMEOUT_MS);
            assertEquals(
                    Long.valueOf(INSERT_TIMEOUT_MS),
                    settings.getAllSettings().get(SOCKET_TIMEOUT_KEY),
                    PER_INSERT_VALUE);
        }
    }

    @ParameterizedTest(name = PARAMETERIZED_NAME)
    @CsvSource(
            delimiter = '|',
            value = {
                "socket_timeout=1500|1500",
                "?socket_timeout=1500|1500",
                "ssl=true&socket_timeout=1500&sslmode=strict|1500",
                "socket_timeout=0|0"
            })
    void insertCarriesNoSocketTimeoutWhenTheOperatorNamedOne(
            String jdbcConnectionProperties, String clientValue) {
        try (ClickHouseHelperClient chc = helper(jdbcConnectionProperties)) {
            InsertSettings settings = new InsertSettings();
            chc.setInsertSocketTimeoutV2(settings, INSERT_TIMEOUT_MS);
            assertFalse(settings.getAllSettings().containsKey(SOCKET_TIMEOUT_KEY), PER_INSERT_ABSENT);
            assertEquals(
                    clientValue,
                    chc.getClient().getConfiguration().get(SOCKET_TIMEOUT_KEY),
                    OPERATOR_VALUE);
        }
    }

    @Test
    void insertTimeoutAboveTheIntRangeIsClampedToIntegerMaxValue() {
        try (ClickHouseHelperClient chc = helper("")) {
            InsertSettings settings = new InsertSettings();
            chc.setInsertSocketTimeoutV2(settings, Long.MAX_VALUE);
            assertEquals(
                    Long.valueOf(Integer.MAX_VALUE),
                    settings.getAllSettings().get(SOCKET_TIMEOUT_KEY),
                    CLAMPED);
        }
    }

    @Test
    void nullInsertSettingsAreLeftAlone() {
        try (ClickHouseHelperClient chc = helper("")) {
            assertDoesNotThrow(() -> chc.setInsertSocketTimeoutV2(null, INSERT_TIMEOUT_MS));
        }
    }

    @Test
    void insertCarriesTheInsertTimeoutWhenJdbcConnectionPropertiesIsNullOrBlank() {
        for (String jdbcConnectionProperties : new String[] {null, "   "}) {
            try (ClickHouseHelperClient chc = helper(jdbcConnectionProperties)) {
                InsertSettings settings = new InsertSettings();
                chc.setInsertSocketTimeoutV2(settings, INSERT_TIMEOUT_MS);
                assertEquals(
                        Long.valueOf(INSERT_TIMEOUT_MS),
                        settings.getAllSettings().get(SOCKET_TIMEOUT_KEY),
                        PER_INSERT_VALUE);
            }
        }
    }
}
