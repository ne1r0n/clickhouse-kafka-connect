package com.clickhouse.kafka.connect.sink.db.helper;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Docker-free pin of what the connector sees when the real V2 client meets an HTTP error: one row
 * per client line, observed through a real socket against {@link StubClickHouseServer}. The rows
 * live in {@link ClientErrorShapes}, which the live pin class reuses. Extends nothing, so it stays
 * in {@code testUnit}.
 */
class ClientErrorShapePinUnitTest {

    @Test
    @DisplayName("CLT-01: a 502 is a ClientException on 0.9.6 and a retried ConnectException on 0.11.0")
    void badGateway() throws Exception {
        ClientErrorShapes.assertBadGateway();
    }

    @Test
    @DisplayName("CLT-01: a 503 is a code-0 ServerException on 0.9.6 and a retried ConnectException on 0.11.0")
    void serviceUnavailable() throws Exception {
        ClientErrorShapes.assertServiceUnavailable();
    }

    @Test
    @DisplayName("CLT-01: an unexpected status stays non-retriable with and without retryOnSocketException")
    void unexpectedStatus() throws Exception {
        ClientErrorShapes.assertUnexpectedStatus();
    }

    @Test
    @DisplayName("CLT-01: server error 159 is a RetriableException, retried 3 times by the client on 0.9.6 and not at all on 0.11.0")
    void timeoutExceeded() throws Exception {
        ClientErrorShapes.assertTimeoutExceeded();
    }

    @Test
    @DisplayName("CLT-01: a client-retried retriable code 252 is a RetriableException after 4 attempts on both lines")
    void tooManyParts() throws Exception {
        ClientErrorShapes.assertTooManyParts();
    }

    @Test
    @DisplayName("CLT-01: a non-retriable server code 60 is attempted once and fails on both lines")
    void unknownTable() throws Exception {
        ClientErrorShapes.assertUnknownTable();
    }

    @Test
    @DisplayName("CLT-01: a ClientException is never retriable")
    void clientExceptionIsNotRetriable() {
        ClientErrorShapes.assertClientExceptionNeverRetriable();
    }
}
