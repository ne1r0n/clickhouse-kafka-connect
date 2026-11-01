package com.clickhouse.kafka.connect.sink.db.helper;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.clickhouse.data.ClickHouseDataStreamFactory;
import com.clickhouse.data.ClickHousePipedOutputStream;
import com.clickhouse.kafka.connect.util.Utils;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Set;
import org.apache.kafka.connect.errors.RetriableException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Docker-free pin of the two IOException message prefixes that {@code Utils.handleException} keys
 * on. The client's piped streams produce them, so the pin builds a real stream with a 50 ms
 * timeout instead of typing the text.
 */
class ClientTimeoutPrefixPinUnitTest {

    private static final int TIMEOUT_MS = 50;
    private static final int BUFFER_BYTES = 8;
    private static final int QUEUE_SLOTS = 1;
    private static final int WRITE_ATTEMPTS = 16;
    private static final String READ_PREFIX = "Read timed out after";
    private static final String WRITE_PREFIX = "Write timed out after";

    private static void assertRetriable(IOException e) {
        assertThrows(
                RetriableException.class,
                () -> Utils.handleException(e, false, false, Set.of(), new ArrayList<>()));
    }

    @Test
    @DisplayName("CLT-01: a starved piped input stream throws an IOException that starts with the read timeout prefix and is retriable")
    void readTimeoutPrefix() throws Exception {
        try (ClickHousePipedOutputStream stream = ClickHouseDataStreamFactory.getInstance()
                .createPipedOutputStream(BUFFER_BYTES, QUEUE_SLOTS, TIMEOUT_MS)) {
            IOException e = assertThrows(IOException.class, () -> stream.getInputStream().read());
            assertTrue(e.getMessage().startsWith(READ_PREFIX), e.getMessage());
            assertRetriable(e);
        }
    }

    @Test
    @DisplayName("CLT-01: a full piped output stream throws an IOException that starts with the write timeout prefix and is retriable")
    void writeTimeoutPrefix() throws Exception {
        ClickHousePipedOutputStream stream = ClickHouseDataStreamFactory.getInstance()
                .createPipedOutputStream(BUFFER_BYTES, QUEUE_SLOTS, TIMEOUT_MS);
        try {
            byte[] chunk = new byte[BUFFER_BYTES * 4];
            IOException e = assertThrows(IOException.class, () -> {
                for (int i = 0; i < WRITE_ATTEMPTS; i++) {
                    stream.write(chunk);
                }
            });
            assertTrue(e.getMessage().startsWith(WRITE_PREFIX), e.getMessage());
            assertRetriable(e);
        } finally {
            // close() waits on the same full queue and times out with its own message
            assertThrows(IOException.class, stream::close);
        }
    }
}
