package com.clickhouse.kafka.connect.sink.db.helper;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

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
    private static final String CLIENT_DEFAULT =
            "an upper-case key leaves the client-level socket_timeout at its default";
    private static final String NOT_BUILT =
            "a socket_timeout pair the client cannot read fails at construction, not later";
    private static final String NON_POSITIVE = "a non-positive timeout adds no per-insert value";
    private static final String CLAMPED = "a timeout above the int range is clamped";

    private static ClickHouseHelperClient helper(String jdbcConnectionProperties) {
        return new ClickHouseHelperClient.ClickHouseClientBuilder(LOOPBACK, UNUSED_PORT, null, null, -1)
                .setJdbcConnectionProperties(jdbcConnectionProperties)
                .build();
    }

    @ParameterizedTest(name = PARAMETERIZED_NAME)
    @ValueSource(
            strings = {
                "",
                "ssl=true&sslmode=strict",
                "SOCKET_TIMEOUT=5",
                "=5",
                "socket_timeout_x=5",
                "x_socket_timeout=5",
                "socket%5Ftimeout=0"
            })
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
                "socket_timeout=0|0",
                "&socket_timeout=0|0",
                "socket_timeout=0&socket_timeout=5|5"
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

    @ParameterizedTest(name = PARAMETERIZED_NAME)
    @ValueSource(strings = {"socket_timeout", "socket_timeout="})
    void aSocketTimeoutPairWithoutANumberFailsHelperConstruction(String jdbcConnectionProperties) {
        assertThrows(NumberFormatException.class, () -> helper(jdbcConnectionProperties), NOT_BUILT);
    }

    @Test
    void aKeyWithSurroundingWhitespaceFailsHelperConstruction() {
        assertThrows(IllegalArgumentException.class, () -> helper(" socket_timeout=5"), NOT_BUILT);
    }

    @Test
    void anUpperCaseSocketTimeoutKeyIsNotAClientSocketTimeout() {
        String clientDefault;
        try (ClickHouseHelperClient chc = helper("")) {
            clientDefault = chc.getClient().getConfiguration().get(SOCKET_TIMEOUT_KEY);
        }
        try (ClickHouseHelperClient chc = helper("SOCKET_TIMEOUT=5")) {
            assertEquals(
                    clientDefault,
                    chc.getClient().getConfiguration().get(SOCKET_TIMEOUT_KEY),
                    CLIENT_DEFAULT);
        }
    }

    @ParameterizedTest
    @ValueSource(longs = {0L, -1L, Long.MIN_VALUE})
    void aNonPositiveInsertTimeoutAddsNoPerInsertSocketTimeout(long timeoutMs) {
        try (ClickHouseHelperClient chc = helper("")) {
            InsertSettings settings = new InsertSettings();
            chc.setInsertSocketTimeoutV2(settings, timeoutMs);
            assertFalse(settings.getAllSettings().containsKey(SOCKET_TIMEOUT_KEY), NON_POSITIVE);
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
