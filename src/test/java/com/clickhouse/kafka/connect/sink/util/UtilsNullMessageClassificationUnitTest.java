package com.clickhouse.kafka.connect.sink.util;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.clickhouse.kafka.connect.util.Utils;
import java.io.IOException;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import org.apache.kafka.connect.errors.RetriableException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * D-05. Upstream calls {@code indexOf} on a null IOException message in {@code
 * Utils.handleException}. This class is new and fork-only, because {@code UtilsTest} is
 * upstream-identical and should not churn on a sync. Before WRT-11 the null-message
 * SocketTimeoutException case never reached here; it was already retriable in {@code Utils}, so
 * that case is a pin. It extends nothing, so that it stays in {@code testUnit}.
 */
public class UtilsNullMessageClassificationUnitTest {

    private static final boolean TOLERANCE_NONE = false;
    private static final boolean TOLERANCE_ALL = true;
    private static final boolean NO_SOCKET_RETRY = false;

    private static void handle(Exception e, boolean errorsTolerance) {
        Utils.handleException(e, errorsTolerance, NO_SOCKET_RETRY, new ArrayList<>());
    }

    private static boolean chainContains(Throwable top, Class<? extends Throwable> type) {
        for (Throwable t = top; t != null; t = t.getCause()) {
            if (type.isInstance(t)) {
                return true;
            }
        }
        return false;
    }

    @Test
    @DisplayName("D-05: a message-less IOException with errorsTolerance=false fails the task with a non-retriable RuntimeException that wraps the original")
    void nullMessageIoExceptionFailsNonRetriablyUnderToleranceNone() {
        IOException failure = new IOException();

        RuntimeException thrown = assertThrows(RuntimeException.class, () -> handle(failure, TOLERANCE_NONE));

        assertFalse(thrown instanceof RetriableException, "a message-less IOException is not a timeout");
        assertFalse(thrown instanceof NullPointerException, "the handler itself must not fail");
        assertSame(failure, thrown.getCause());
    }

    @Test
    @DisplayName("D-05: a message-less IOException with errorsTolerance=true returns normally")
    void nullMessageIoExceptionIsToleratedUnderToleranceAll() {
        assertDoesNotThrow(() -> handle(new IOException(), TOLERANCE_ALL));
    }

    @Test
    @DisplayName("D-05: a message-less IOException as the root cause of a wrapper keeps the original in the chain and raises no NullPointerException")
    void nullMessageIoExceptionAsRootCauseKeepsTheOriginal() {
        IOException root = new IOException();
        RuntimeException wrapper = new RuntimeException("query", root);

        RuntimeException thrown = assertThrows(RuntimeException.class, () -> handle(wrapper, TOLERANCE_NONE));

        assertTrue(chainContains(thrown, IOException.class));
        assertFalse(chainContains(thrown, NullPointerException.class));
        assertSame(wrapper, thrown.getCause());
    }

    @Test
    @DisplayName("D-05: a message-less SocketTimeoutException stays retriable")
    void nullMessageSocketTimeoutStaysRetriable() {
        assertThrows(RetriableException.class, () -> handle(new SocketTimeoutException(), TOLERANCE_NONE));
    }

    @Test
    @DisplayName("D-05: an IOException whose message starts with Read timed out after stays retriable")
    void readTimeoutPrefixStaysRetriable() {
        assertThrows(
                RetriableException.class,
                () -> handle(new IOException("Read timed out after 30000 ms"), TOLERANCE_NONE));
    }

    @Test
    @DisplayName("D-05: an IOException with an unrelated message and errorsTolerance=false stays a non-retriable RuntimeException")
    void otherIoExceptionMessageStaysNonRetriable() {
        IOException failure = new IOException("Broken pipe");

        RuntimeException thrown = assertThrows(RuntimeException.class, () -> handle(failure, TOLERANCE_NONE));

        assertFalse(thrown instanceof RetriableException);
        assertInstanceOf(IOException.class, thrown.getCause());
        assertSame(failure, thrown.getCause());
    }
}
