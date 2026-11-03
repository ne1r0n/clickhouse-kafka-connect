package com.clickhouse.kafka.connect.sink;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;

import com.clickhouse.client.api.ServerException;
import com.clickhouse.kafka.connect.sink.helper.SchemalessTestData;
import com.clickhouse.kafka.connect.sink.util.ServerExceptions;
import com.clickhouse.kafka.connect.util.Utils;
import java.time.Duration;
import java.util.List;
import java.util.Set;
import org.apache.kafka.connect.errors.RetriableException;
import org.apache.kafka.connect.sink.SinkRecord;
import org.junit.jupiter.api.Test;

/**
 * Docker-free pin that {@code Utils.handleException} terminates on a cyclic cause chain that holds a
 * non-retriable ClickHouse server error, and ends in the tolerance handling as it did before the cause-chain
 * walk existed.
 *
 * <p>Extends nothing and carries no tag so it stays in {@code testUnit}.
 */
class HandleExceptionCauseCycleUnitTest {

    private static final String TOPIC = "cause_cycle";
    private static final String QUERY_TEXT = "cause-cycle-query";
    private static final String FAILURE_MESSAGE = "cause-cycle failure";
    private static final String NUMBER_OF_RECORDS_PREFIX = "Number of records: ";
    private static final int RECORD_COUNT = 3;
    private static final int SERVER_HTTP_STATUS = 500;
    private static final int NON_RETRIABLE_SERVER_CODE = 60;
    private static final Duration TERMINATION_BOUND = Duration.ofSeconds(10);

    /** A wrapper whose cause is a server error that names the wrapper as its own cause. */
    private static RuntimeException wrapperInCycleWithServerError() {
        ServerException server = ServerExceptions.create(NON_RETRIABLE_SERVER_CODE, FAILURE_MESSAGE, SERVER_HTTP_STATUS);
        RuntimeException wrapper = new RuntimeException(QUERY_TEXT, server);
        server.initCause(wrapper);
        return wrapper;
    }

    @Test
    void causeCycleThroughNonRetriableServerErrorIsWrappedUnderToleranceNone() {
        List<SinkRecord> records = SchemalessTestData.createPrimitiveTypes(TOPIC, 0, RECORD_COUNT);
        RuntimeException input = wrapperInCycleWithServerError();

        RuntimeException thrown = assertTimeoutPreemptively(
                TERMINATION_BOUND,
                () -> assertThrows(
                        RuntimeException.class, () -> Utils.handleException(input, false, false, Set.of(), records)));

        assertFalse(thrown instanceof RetriableException, "a cycle with no RetriableException stays non-retriable");
        assertEquals(RuntimeException.class, thrown.getClass());
        assertEquals(NUMBER_OF_RECORDS_PREFIX + RECORD_COUNT, thrown.getMessage());
        assertSame(input, thrown.getCause());
    }

    @Test
    void causeCycleThroughNonRetriableServerErrorReturnsUnderToleranceAll() {
        List<SinkRecord> records = SchemalessTestData.createPrimitiveTypes(TOPIC, 0, RECORD_COUNT);
        RuntimeException input = wrapperInCycleWithServerError();

        assertTimeoutPreemptively(
                TERMINATION_BOUND,
                () -> assertDoesNotThrow(() -> Utils.handleException(input, true, false, Set.of(), records)));
    }
}
