package com.clickhouse.kafka.connect.sink;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.clickhouse.kafka.connect.ClickHouseSinkConnector;
import com.clickhouse.kafka.connect.sink.util.LogCapture;
import com.clickhouse.kafka.connect.util.Utils;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.ServerSocket;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Docker-free pins of the start-up WARN for {@code additionalRetriableErrorCodes} entries outside
 * the connector's built-in retriable set and code 216. Each case starts a real {@link
 * ClickHouseSinkTask} against a closed loopback port, so {@code start()} reaches the connection
 * step after the WARN and fails there; the WARN therefore never aborts start-up.
 */
class UnvettedRetriableCodesWarnUnitTest {

    private static final String LOOPBACK = "127.0.0.1";
    private static final String CLIENT_V2 = "V2";
    private static final String WARN_LEVEL = "WARN";
    private static final String CONNECTION_FAILURE = "Connection to ClickHouse is not active.";
    private static final String EXPECTED_WARN_FORMAT =
            "additionalRetriableErrorCodes lists %s, which the connector does not treat as transient. "
                    + "A batch that fails with such a code is redelivered with no retry budget and outside "
                    + "errors.tolerance and the dead letter queue, so a permanent error blocks the partition "
                    + "until the setting is changed.";
    private static final String START_REACHES_CONNECTION =
            "start() must reach the connection step after the unvetted-code check";
    private static final String WARN_COUNT = "unvetted-code WARN count at start-up";
    private static final String WARN_TEXT = "unvetted-code WARN text";
    private static final String NAME = "{displayName}[{index}] additionalRetriableErrorCodes={0}";

    private static int closedPort() {
        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static Map<String, String> props(String codesOrNull) {
        Map<String, String> props = new HashMap<>();
        props.put(ClickHouseSinkConfig.HOSTNAME, LOOPBACK);
        props.put(ClickHouseSinkConfig.PORT, String.valueOf(closedPort()));
        props.put(ClickHouseSinkConfig.SSL_ENABLED, "false");
        props.put(ClickHouseSinkConnector.CLIENT_VERSION, CLIENT_V2);
        if (codesOrNull != null) {
            props.put(ClickHouseSinkConfig.ADDITIONAL_RETRIABLE_ERROR_CODES, codesOrNull);
        }
        return props;
    }

    private static List<LogCapture.CapturedEvent> startAndCaptureWarns(Map<String, String> props) {
        ClickHouseSinkTask task = new ClickHouseSinkTask();
        try (LogCapture log = LogCapture.attach(ClickHouseSinkTask.class)) {
            RuntimeException thrown =
                    assertThrows(RuntimeException.class, () -> task.start(props), START_REACHES_CONNECTION);
            assertEquals(CONNECTION_FAILURE, thrown.getMessage(), START_REACHES_CONNECTION);
            return log.events(WARN_LEVEL);
        } finally {
            task.stop();
        }
    }

    static Stream<String> vettedSettings() {
        return Stream.concat(
                Utils.DEFAULT_RETRIABLE_ERROR_CODES.stream().sorted().map(String::valueOf),
                Stream.of("216", "216,159,3"));
    }

    static Stream<Arguments> unvettedSettings() {
        return Stream.of(
                Arguments.of("60", "[60]"),
                Arguments.of("62, 60,159", "[60, 62]"),
                Arguments.of("2147483647", "[2147483647]"));
    }

    static Stream<String> blankOrEmptyEntrySettings() {
        return Stream.of(" ", ",", "216,,159");
    }

    static Stream<Arguments> emptyOrRepeatedEntrySettings() {
        return Stream.of(
                Arguments.of("159,,60", "[60]"),
                Arguments.of("60,60", "[60]"),
                Arguments.of("60,", "[60]"),
                Arguments.of("159,60,", "[60]"));
    }

    @Test
    void unsetSettingLogsNoWarnAtStartup() {
        assertEquals(0, startAndCaptureWarns(props(null)).size(), WARN_COUNT);
    }

    @ParameterizedTest(name = NAME)
    @MethodSource("vettedSettings")
    void vettedCodesLogNoWarnAtStartup(String setting) {
        assertEquals(0, startAndCaptureWarns(props(setting)).size(), WARN_COUNT);
    }

    @ParameterizedTest(name = NAME)
    @MethodSource("unvettedSettings")
    void unvettedCodesLogOneWarnNamingThemAtStartup(String setting, String expectedList) {
        List<LogCapture.CapturedEvent> warns = startAndCaptureWarns(props(setting));
        assertEquals(1, warns.size(), WARN_COUNT);
        assertEquals(String.format(EXPECTED_WARN_FORMAT, expectedList), warns.get(0).message(), WARN_TEXT);
    }

    @ParameterizedTest(name = NAME)
    @MethodSource("blankOrEmptyEntrySettings")
    void blankOrEmptyEntrySettingsLogNoWarnAtStartup(String setting) {
        assertEquals(0, startAndCaptureWarns(props(setting)).size(), WARN_COUNT);
    }

    @ParameterizedTest(name = NAME)
    @MethodSource("emptyOrRepeatedEntrySettings")
    void emptyOrRepeatedEntriesNameEachUnvettedCodeOnceAtStartup(String setting, String expectedList) {
        List<LogCapture.CapturedEvent> warns = startAndCaptureWarns(props(setting));
        assertEquals(1, warns.size(), WARN_COUNT);
        assertEquals(String.format(EXPECTED_WARN_FORMAT, expectedList), warns.get(0).message(), WARN_TEXT);
    }
}
