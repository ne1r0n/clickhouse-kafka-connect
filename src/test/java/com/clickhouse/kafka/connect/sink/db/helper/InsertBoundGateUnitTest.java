package com.clickhouse.kafka.connect.sink.db.helper;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Docker-free cases for {@code ClickHouseHelperClient.insertBoundMs}, the one gate both connector
 * wall-clock insert bounds read. The bound is the insert timeout unless the operator's
 * {@code jdbcConnectionProperties} names {@code socket_timeout}, with no network and no clock.
 */
class InsertBoundGateUnitTest {
    private static final String PARAMETERIZED_NAME =
            "{displayName}[{index}] jdbcConnectionProperties={0}";
    private static final String LOOPBACK = "127.0.0.1";
    private static final int UNUSED_PORT = 8123;
    private static final long INSERT_TIMEOUT_MS = 300L;
    private static final long NO_BOUND = 0L;
    private static final String BOUND_KEPT = "the insert timeout is the bound when the operator named no socket_timeout";
    private static final String OPERATOR_OWNS =
            "no connector bound when the operator named socket_timeout, an explicit 0 included";
    private static final String NON_POSITIVE = "a non-positive insert timeout is no bound";
    private static final String LARGE_KEPT = "a very large insert timeout is passed through unclamped";

    private static ClickHouseHelperClient helper(String jdbcConnectionProperties) {
        return new ClickHouseHelperClient.ClickHouseClientBuilder(LOOPBACK, UNUSED_PORT, null, null, -1)
                .setJdbcConnectionProperties(jdbcConnectionProperties)
                .build();
    }

    @ParameterizedTest(name = PARAMETERIZED_NAME)
    @NullAndEmptySource
    @ValueSource(
            strings = {
                " ",
                "ssl=true&sslmode=strict",
                "SOCKET_TIMEOUT=5",
                "socket_timeout_x=5",
                "x_socket_timeout=5",
                "socket%5Ftimeout=0"
            })
    void theInsertTimeoutIsTheBoundWhenTheOperatorNamedNoSocketTimeout(String jdbcConnectionProperties) {
        try (ClickHouseHelperClient chc = helper(jdbcConnectionProperties)) {
            assertEquals(INSERT_TIMEOUT_MS, chc.insertBoundMs(INSERT_TIMEOUT_MS), BOUND_KEPT);
        }
    }

    @ParameterizedTest(name = PARAMETERIZED_NAME)
    @ValueSource(
            strings = {
                "socket_timeout=0",
                "socket_timeout=200",
                "socket_timeout=600000",
                "?socket_timeout=200",
                "socket_timeout=200&ssl=true",
                "ssl=true&socket_timeout=200&sslmode=strict",
                "ssl=true&sslmode=strict&socket_timeout=200",
                "socket_timeout=0&socket_timeout=5"
            })
    void noConnectorBoundWhenTheOperatorNamedSocketTimeout(String jdbcConnectionProperties) {
        try (ClickHouseHelperClient chc = helper(jdbcConnectionProperties)) {
            assertEquals(NO_BOUND, chc.insertBoundMs(INSERT_TIMEOUT_MS), OPERATOR_OWNS);
        }
    }

    @ParameterizedTest
    @ValueSource(longs = {0L, -1L, Long.MIN_VALUE})
    void aNonPositiveInsertTimeoutIsNoBound(long timeoutMs) {
        try (ClickHouseHelperClient chc = helper("")) {
            assertEquals(NO_BOUND, chc.insertBoundMs(timeoutMs), NON_POSITIVE);
        }
    }

    @Test
    void aVeryLargeInsertTimeoutIsPassedThrough() {
        try (ClickHouseHelperClient chc = helper("")) {
            assertEquals(Long.MAX_VALUE, chc.insertBoundMs(Long.MAX_VALUE), LARGE_KEPT);
        }
    }
}
