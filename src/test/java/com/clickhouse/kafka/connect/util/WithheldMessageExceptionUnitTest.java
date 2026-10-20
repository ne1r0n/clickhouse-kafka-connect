package com.clickhouse.kafka.connect.util;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.clickhouse.kafka.connect.sink.util.ThrowableTexts;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import org.apache.kafka.connect.errors.DataException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * The contract of {@link WithheldMessageException} (WRT-13, D-05, D-06): it keeps frames and class
 * names, never text, drops suppressed exceptions and always terminates. It lives in the production
 * package so that it can read the package-private depth limit. It extends nothing and needs no
 * Docker.
 */
public class WithheldMessageExceptionUnitTest {

    private static final String SENTINEL = "SENTINELq7Zx41kV";
    private static final String NOTE = "(message withheld: may contain record data)";
    private static final String IAE_NAME = IllegalArgumentException.class.getName();
    private static final String EXTRA_NODES_MESSAGE = "node ";
    private static final int EXTRA_DEPTH = 10;

    private static void assertNoSentinel(Throwable thrown) {
        for (String text : ThrowableTexts.allTexts(thrown)) {
            assertFalse(text.contains(SENTINEL), "a text must carry no record value");
        }
    }

    private static Throwable chainOf(int nodes) {
        Throwable cause = null;
        for (int i = nodes; i >= 1; i--) {
            cause = cause == null
                    ? new RuntimeException(EXTRA_NODES_MESSAGE + i + SENTINEL)
                    : new RuntimeException(EXTRA_NODES_MESSAGE + i + SENTINEL, cause);
        }
        return cause;
    }

    private static Throwable thrownByUuid() {
        return assertThrows(IllegalArgumentException.class, () -> UUID.fromString(SENTINEL));
    }

    @Test
    @DisplayName("the message, the localized message and toString carry the fixed note and the original class name, never the original message")
    void noteAndToStringCarryNoOriginalText() {
        IllegalArgumentException original = new IllegalArgumentException(SENTINEL);

        WithheldMessageException copy = WithheldMessageException.of(original);

        assertEquals(NOTE, copy.getMessage());
        assertEquals(NOTE, copy.getLocalizedMessage());
        assertEquals(IAE_NAME + " " + NOTE, copy.toString());
        assertEquals(IAE_NAME, copy.getOriginalClassName());
        assertNoSentinel(copy);
    }

    @Test
    @DisplayName("toString joins the class name and the note with exactly one space")
    void toStringJoinsWithExactlyOneSpace() {
        String text = WithheldMessageException.of(new IllegalStateException()).toString();

        assertEquals(IllegalStateException.class.getName() + " " + NOTE, text);
        assertFalse(text.contains("  "));
    }

    @Test
    @DisplayName("the stack frames of the original are copied, in order")
    void framesAreCopied() {
        Throwable original = thrownByUuid();

        WithheldMessageException copy = WithheldMessageException.of(original);

        assertTrue(original.getStackTrace().length > 0);
        assertArrayEquals(original.getStackTrace(), copy.getStackTrace());
    }

    @Test
    @DisplayName("suppressed exceptions are dropped and stay dropped when another one is added")
    void suppressedExceptionsAreDropped() {
        IllegalArgumentException original = new IllegalArgumentException(SENTINEL);
        original.addSuppressed(new RuntimeException(SENTINEL));

        WithheldMessageException copy = WithheldMessageException.of(original);
        copy.addSuppressed(new RuntimeException(SENTINEL));

        assertEquals(0, copy.getSuppressed().length);
        assertNoSentinel(copy);
    }

    @Test
    @DisplayName("a three-level chain keeps the class order and the frames of each level, and every level carries the note")
    void chainKeepsClassOrderAndFrames() {
        Throwable bottom = new NumberFormatException(SENTINEL);
        Throwable middle = new IllegalStateException(SENTINEL, bottom);
        Throwable top = new IllegalArgumentException(SENTINEL, middle);
        List<Throwable> originals = ThrowableTexts.causeChain(top);

        WithheldMessageException copy = WithheldMessageException.of(top);

        List<Throwable> copies = ThrowableTexts.causeChain(copy);
        assertEquals(
                originals.stream().map(t -> t.getClass().getName()).collect(Collectors.toList()),
                copies.stream().map(t -> ((WithheldMessageException) t).getOriginalClassName()).collect(Collectors.toList()));
        for (int i = 0; i < originals.size(); i++) {
            assertEquals(NOTE, copies.get(i).getMessage());
            assertArrayEquals(originals.get(i).getStackTrace(), copies.get(i).getStackTrace());
        }
        assertNoSentinel(copy);
    }

    @Test
    @DisplayName("a cause cycle ends with a two-node copy")
    void causeCycleTerminatesWithTwoNodes() {
        Exception first = new IllegalStateException(SENTINEL);
        Exception second = new IllegalArgumentException(SENTINEL, first);
        first.initCause(second);

        WithheldMessageException copy = WithheldMessageException.of(second);

        List<Throwable> copies = ThrowableTexts.causeChain(copy);
        assertEquals(2, copies.size());
        assertEquals(IAE_NAME, ((WithheldMessageException) copies.get(0)).getOriginalClassName());
        assertEquals(
                IllegalStateException.class.getName(), ((WithheldMessageException) copies.get(1)).getOriginalClassName());
        assertNull(copies.get(1).getCause());
    }

    @Test
    @DisplayName("a chain of exactly MAX_CAUSE_DEPTH nodes is copied whole")
    void chainAtTheDepthLimitIsCopiedWhole() {
        WithheldMessageException copy = WithheldMessageException.of(chainOf(WithheldMessageException.MAX_CAUSE_DEPTH));

        assertEquals(WithheldMessageException.MAX_CAUSE_DEPTH, ThrowableTexts.causeChain(copy).size());
    }

    @Test
    @DisplayName("a chain longer than MAX_CAUSE_DEPTH is cut to MAX_CAUSE_DEPTH nodes")
    void chainBeyondTheDepthLimitIsCut() {
        WithheldMessageException copy =
                WithheldMessageException.of(chainOf(WithheldMessageException.MAX_CAUSE_DEPTH + EXTRA_DEPTH));

        assertEquals(WithheldMessageException.MAX_CAUSE_DEPTH, ThrowableTexts.causeChain(copy).size());
        assertNoSentinel(copy);
    }

    @Test
    @DisplayName("an original without a message still sanitizes to the class name and the note")
    void messageLessOriginalSanitizesWithoutThrowing() {
        WithheldMessageException copy = WithheldMessageException.of(new IllegalArgumentException());

        assertEquals(IAE_NAME + " " + NOTE, copy.toString());
        assertEquals(NOTE, copy.getMessage());
    }

    @Test
    @DisplayName("the rendered stack trace shows the withheld-message cause line and the original frames, and no value")
    void printedStackTraceShowsCauseLineAndFramesWithoutValue() {
        Throwable original = thrownByUuid();
        assertTrue(original.getMessage().contains(SENTINEL), "self-check: the JDK message embeds the value");

        String printed = ThrowableTexts.printedStackTrace(new DataException("x", WithheldMessageException.of(original)));

        assertTrue(printed.contains("Caused by: " + IAE_NAME + " " + NOTE), printed);
        assertTrue(printed.contains("java.util.UUID.fromString"), printed);
        assertFalse(printed.contains(SENTINEL), printed);
    }

    @Test
    @DisplayName("a null original is rejected")
    void nullOriginalIsRejected() {
        assertThrows(NullPointerException.class, () -> WithheldMessageException.of(null));
    }
}
