package com.clickhouse.kafka.connect.sink;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.clickhouse.client.ClickHouseException;
import com.clickhouse.client.ClickHouseNode;
import com.clickhouse.kafka.connect.sink.data.Record;
import com.clickhouse.kafka.connect.sink.db.InMemoryDBWriter;
import com.clickhouse.kafka.connect.sink.dlq.ErrorReporter;
import com.clickhouse.kafka.connect.sink.helper.SchemalessTestData;
import com.clickhouse.kafka.connect.sink.util.ServerExceptions;
import com.clickhouse.kafka.connect.util.QueryIdentifier;
import com.clickhouse.kafka.connect.util.Utils;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.apache.kafka.common.config.ConfigDef;
import org.apache.kafka.common.config.ConfigException;
import org.apache.kafka.common.config.ConfigValue;
import org.apache.kafka.connect.errors.DataException;
import org.apache.kafka.connect.errors.RetriableException;
import org.apache.kafka.connect.sink.SinkRecord;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Docker-free pin for TAL-04: {@code additionalRetriableErrorCodes} (upstream PR #848) is a supported
 * setting whose default changes nothing. It is pinned from the config string through the ConfigDef,
 * the {@link ClickHouseSinkConfig} constructor and the {@link ProxySinkTask} {@code DBWriter} seam
 * into {@link Utils#handleException}.
 *
 * <p>"A DataException is never reclassified as retriable" is pinned as: adding codes never makes a
 * DataException that carries no ClickHouse server error and no network root cause retriable.
 * A DataException wrapping a retriable server error, or one whose root cause is a socket timeout
 * under {@code errors.tolerance=all}, is retriable by {@code handleException}'s root-cause-first rule
 * and is deliberately not asserted here.
 *
 * <p>Extends nothing so it stays in {@code testUnit}: extending {@code ClickHouseBase} would inherit
 * its {@code @Tag("clickhouse")} and drop this class out of the Docker-free loop.
 */
class AdditionalRetriableErrorCodesPinUnitTest {

    private static final String TOPIC = "tal04_additional_retriable_error_codes";
    private static final String HOSTNAME_VALUE = "localhost";
    private static final String SERVER_ERROR_MESSAGE = "tal04 server error";
    private static final String DATA_ERROR_MESSAGE = "tal04 data error";
    private static final String ODD_IO_ERROR_MESSAGE = "tal04 io error without a timeout prefix";
    private static final String ERROR_KEY = ClickHouseSinkConfig.ADDITIONAL_RETRIABLE_ERROR_CODES;
    private static final String POSITIVE_INTEGER_MESSAGE = "positive integer";
    private static final String WRITER_ERROR_CODES = "216,159";
    private static final int SERVER_HTTP_STATUS = 500;
    private static final int MAX_SWEPT_CODE = 1100;
    private static final int RECORD_COUNT = 10;
    private static final int NON_DEFAULT_CODE = 216;
    private static final int DEFAULT_CODE_159 = 159;
    private static final boolean ERRORS_TOLERANCE_NONE = false;
    private static final boolean ERRORS_TOLERANCE_ALL = true;
    private static final boolean RETRY_ON_SOCKET_EXCEPTION = false;
    private static final ErrorReporter NO_ERROR_REPORTER = null;

    // Copies the case labels of the retriable switch Utils.handleException had at pre-v1.7.0-tail-2.
    private static final Set<Integer> PRE_MERGE_RETRIABLE_CODES =
            Set.of(3, 107, 159, 164, 202, 203, 209, 210, 241, 242, 252, 285, 319, 425, 999);

    private static final Set<Integer> NO_ADDED_CODES = Set.of();

    private static Exception v2ServerError(int code) {
        // Client 0.11.0 changes this constructor (Phase 36).
        return ServerExceptions.create(code, SERVER_ERROR_MESSAGE, SERVER_HTTP_STATUS);
    }

    private static Exception v1ServerError(int code) {
        return new ClickHouseException(code, SERVER_ERROR_MESSAGE, (ClickHouseNode) null);
    }

    private static Map<String, String> hostnameOnly() {
        Map<String, String> props = new HashMap<>();
        props.put(ClickHouseSinkConfig.HOSTNAME, HOSTNAME_VALUE);
        return props;
    }

    private static Map<String, String> withCodes(String codes) {
        Map<String, String> props = hostnameOnly();
        props.put(ERROR_KEY, codes);
        return props;
    }

    private static ConfigValue configValueOf(Map<String, String> props) {
        return ClickHouseSinkConfig.CONFIG.validate(props).stream()
                .filter(value -> ERROR_KEY.equals(value.name()))
                .findFirst()
                .orElseThrow();
    }

    private static void handle(Exception e, boolean errorsTolerance, Set<Integer> addedCodes) {
        Utils.handleException(e, errorsTolerance, RETRY_ON_SOCKET_EXCEPTION, addedCodes, new ArrayList<>());
    }

    private static List<Set<Integer>> addedCodeSets() {
        List<Set<Integer>> sets = PRE_MERGE_RETRIABLE_CODES.stream().map(Set::of).collect(Collectors.toList());
        sets.add(Set.of(NON_DEFAULT_CODE));
        sets.add(PRE_MERGE_RETRIABLE_CODES);
        sets.add(Set.of(NON_DEFAULT_CODE, DEFAULT_CODE_159));
        return sets;
    }

    private static Stream<Arguments> normalisedCodeLists() {
        return Stream.of(
                Arguments.of("216, 999", Set.of(216, 999)),
                Arguments.of(" 216 , 216 , ", Set.of(216)),
                Arguments.of("216,,999", Set.of(216, 999)),
                Arguments.of("999,216,216", Set.of(216, 999)));
    }

    private static Stream<String> malformedCodeLists() {
        return Stream.of("abc", "21.6", "-1", "0", "99999999999", "216, abc", "216,0");
    }

    /** Fails every insert with a value-free DataException, as a converter or writer data error would. */
    private static final class DataExceptionDBWriter extends InMemoryDBWriter {
        @Override
        public void doInsert(List<Record> records, QueryIdentifier queryId, ErrorReporter errorReporter) {
            throw new DataException(DATA_ERROR_MESSAGE);
        }
    }

    @Test
    @DisplayName("TAL-04: the default retriable set is exactly the 15 pre-merge codes including 159 and is unmodifiable")
    void defaultRetriableSetIsExactlyTheFifteenPreMergeCodesIncluding159() {
        assertEquals(PRE_MERGE_RETRIABLE_CODES, Utils.DEFAULT_RETRIABLE_ERROR_CODES);
        assertEquals(15, Utils.DEFAULT_RETRIABLE_ERROR_CODES.size());
        assertTrue(Utils.DEFAULT_RETRIABLE_ERROR_CODES.contains(DEFAULT_CODE_159));
        assertThrows(UnsupportedOperationException.class, () -> Utils.DEFAULT_RETRIABLE_ERROR_CODES.add(NON_DEFAULT_CODE));
    }

    @Test
    @DisplayName("TAL-04: with the key unset every server code 0..1100 on client V2 and V1 classifies like the pre-merge switch")
    void unsetKeyClassifiesEveryServerCodeLikeThePreMergeSwitchOnBothClients() {
        Set<Integer> unsetCodes = new ClickHouseSinkConfig(new HashMap<>()).getAdditionalRetriableErrorCodes();

        IntStream.rangeClosed(0, MAX_SWEPT_CODE).forEach(code -> {
            boolean retriable = PRE_MERGE_RETRIABLE_CODES.contains(code);
            for (Exception serverError : List.of(v2ServerError(code), v1ServerError(code))) {
                Exception wrapped = new RuntimeException(serverError);
                String context = "code " + code + " via " + serverError.getClass().getSimpleName();

                RuntimeException thrown = assertThrows(
                        RuntimeException.class, () -> handle(wrapped, ERRORS_TOLERANCE_NONE, unsetCodes), context);
                assertEquals(retriable, thrown instanceof RetriableException, context);
            }
            Exception tolerated = new RuntimeException(v2ServerError(code));
            if (retriable) {
                assertThrows(
                        RetriableException.class,
                        () -> handle(tolerated, ERRORS_TOLERANCE_ALL, unsetCodes),
                        "code " + code + " under errors.tolerance=all");
            } else {
                assertDoesNotThrow(
                        () -> handle(tolerated, ERRORS_TOLERANCE_ALL, unsetCodes),
                        "code " + code + " under errors.tolerance=all");
            }
        });
    }

    @Test
    @DisplayName("TAL-04: with the key unset the constructor yields an empty unmodifiable set and the ConfigDef reports no error")
    void unsetKeyYieldsAnEmptyUnmodifiableSetAndNoConfigDefError() {
        Set<Integer> codes = new ClickHouseSinkConfig(new HashMap<>()).getAdditionalRetriableErrorCodes();
        ConfigValue configValue = configValueOf(hostnameOnly());

        assertTrue(codes.isEmpty());
        assertThrows(UnsupportedOperationException.class, () -> codes.add(1));
        assertTrue(configValue.errorMessages().isEmpty(), "Unexpected errors: " + configValue.errorMessages());
        assertEquals(List.of(), configValue.value());
    }

    @Test
    @DisplayName("TAL-04: the ConfigDef registers the ErrorCodeListValidator on a LIST key with an empty default and LOW importance")
    void configDefRegistersTheErrorCodeListValidatorWithAnEmptyListDefault() {
        ConfigDef.ConfigKey key = ClickHouseSinkConfig.CONFIG.configKeys().get(ERROR_KEY);

        assertEquals(ConfigDef.Type.LIST, key.type);
        assertEquals(List.of(), key.defaultValue);
        assertEquals(ConfigDef.Importance.LOW, key.importance);
        assertInstanceOf(ClickHouseSinkConfig.ErrorCodeListValidator.class, key.validator);
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @MethodSource("normalisedCodeLists")
    @DisplayName("TAL-04: whitespace, duplicates and empty items pass the ConfigDef and the constructor yields the trimmed de-duplicated set")
    void trimmedDuplicatedAndEmptyItemsPassTheConfigDefAndTheConstructorNormalisesThem(
            String codes, Set<Integer> expected) {
        Map<String, String> props = withCodes(codes);
        ConfigValue configValue = configValueOf(props);

        assertTrue(configValue.errorMessages().isEmpty(), "Unexpected errors: " + configValue.errorMessages());
        assertDoesNotThrow(() -> ClickHouseSinkConfig.CONFIG.parse(props));
        assertEquals(expected, new ClickHouseSinkConfig(props).getAdditionalRetriableErrorCodes());
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @MethodSource("malformedCodeLists")
    @DisplayName("TAL-04: non-integers, non-positive and out-of-range values are rejected by the ConfigDef and the constructor, naming the key")
    void nonIntegersAndNonPositiveValuesAreRejectedByTheConfigDefAndTheConstructorNamingTheKey(String codes) {
        Map<String, String> props = withCodes(codes);
        List<String> errors = configValueOf(props).errorMessages();

        assertEquals(1, errors.size(), "Expected exactly one error for \"" + codes + "\": " + errors);
        assertTrue(errors.get(0).contains(ERROR_KEY), errors.get(0));
        assertTrue(errors.get(0).contains(POSITIVE_INTEGER_MESSAGE), errors.get(0));
        ConfigException parseFailure = assertThrows(ConfigException.class, () -> ClickHouseSinkConfig.CONFIG.parse(props));
        assertTrue(parseFailure.getMessage().contains(ERROR_KEY), parseFailure.getMessage());
        assertThrows(IllegalArgumentException.class, () -> new ClickHouseSinkConfig(props));
    }

    @Test
    @DisplayName("TAL-04: the validator accepts blank and empty list items and rejects null")
    void validatorAcceptsBlankAndEmptyListItemsAndRejectsNull() {
        ClickHouseSinkConfig.ErrorCodeListValidator validator = new ClickHouseSinkConfig.ErrorCodeListValidator();

        assertDoesNotThrow(() -> validator.ensureValid(ERROR_KEY, Arrays.asList("216", " ", "999")));
        assertDoesNotThrow(() -> validator.ensureValid(ERROR_KEY, Arrays.asList("216", "", "999")));
        assertThrows(ConfigException.class, () -> validator.ensureValid(ERROR_KEY, null));
    }

    @Test
    @DisplayName("TAL-04: a plain DataException is never retriable whichever codes are added")
    void plainDataExceptionIsNeverRetriableWhicheverCodesAreAdded() {
        for (Set<Integer> addedCodes : addedCodeSets()) {
            DataException dataException = new DataException(DATA_ERROR_MESSAGE);

            DataException rethrown =
                    assertThrows(DataException.class, () -> handle(dataException, ERRORS_TOLERANCE_NONE, addedCodes));
            assertSame(dataException, rethrown, "Added codes: " + addedCodes);
            assertDoesNotThrow(() -> handle(dataException, ERRORS_TOLERANCE_ALL, addedCodes), "Added codes: " + addedCodes);
        }
    }

    @Test
    @DisplayName("TAL-04: a DataException caused by a non-ClickHouse, non-network exception is never retriable")
    void dataExceptionWithANonClickHouseNonNetworkCauseIsNeverRetriable() {
        Set<Integer> addedCodes = Set.of(NON_DEFAULT_CODE, DEFAULT_CODE_159);
        List<Exception> causes = List.of(new IllegalStateException(DATA_ERROR_MESSAGE), new IOException(ODD_IO_ERROR_MESSAGE));

        for (Exception cause : causes) {
            DataException dataException = new DataException(DATA_ERROR_MESSAGE, cause);

            DataException rethrown =
                    assertThrows(DataException.class, () -> handle(dataException, ERRORS_TOLERANCE_NONE, addedCodes));
            assertSame(dataException, rethrown, "Cause: " + cause.getClass().getSimpleName());
            assertDoesNotThrow(
                    () -> handle(dataException, ERRORS_TOLERANCE_ALL, addedCodes),
                    "Cause: " + cause.getClass().getSimpleName());
        }
    }

    @Test
    @DisplayName("TAL-04: a writer DataException fails the task non-retriably through ProxySinkTask with codes configured")
    void writerDataExceptionFailsTheTaskNonRetriablyThroughProxySinkTaskWithCodesConfigured() throws Exception {
        List<SinkRecord> records = SchemalessTestData.createPrimitiveTypes(TOPIC, 0, RECORD_COUNT);

        Map<String, String> strictProps = withCodes(WRITER_ERROR_CODES);
        strictProps.put(ClickHouseSinkConfig.ERRORS_TOLERANCE, ClickHouseSinkConfig.ERROR_TOLERANCE_NONE);
        DataExceptionDBWriter strictWriter = new DataExceptionDBWriter();
        ProxySinkTask strictTask = new ProxySinkTask(new ClickHouseSinkConfig(strictProps), NO_ERROR_REPORTER, strictWriter);
        try {
            RuntimeException thrown = assertThrows(RuntimeException.class, () -> strictTask.put(records));
            assertFalse(thrown instanceof RetriableException, "A writer DataException must not become retriable");
            Throwable cause = thrown;
            boolean foundDataException = false;
            while (cause != null && !foundDataException) {
                foundDataException = cause instanceof DataException;
                cause = cause.getCause();
            }
            assertTrue(foundDataException, "The DataException must stay in the cause chain of " + thrown);
        } finally {
            strictTask.stop();
        }

        Map<String, String> tolerantProps = withCodes(WRITER_ERROR_CODES);
        tolerantProps.put(ClickHouseSinkConfig.ERRORS_TOLERANCE, ClickHouseSinkConfig.ERROR_TOLERANCE_ALL);
        DataExceptionDBWriter tolerantWriter = new DataExceptionDBWriter();
        ProxySinkTask tolerantTask =
                new ProxySinkTask(new ClickHouseSinkConfig(tolerantProps), NO_ERROR_REPORTER, tolerantWriter);

        try {
            assertDoesNotThrow(() -> tolerantTask.put(records));
            assertEquals(0, tolerantWriter.recordsInserted());
        } finally {
            tolerantTask.stop();
        }
    }
}
