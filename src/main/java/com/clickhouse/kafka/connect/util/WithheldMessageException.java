package com.clickhouse.kafka.connect.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * A copy of a failure's cause chain that keeps the stack frames and the class names and none of
 * the text. Messages of conversion failures (an invalid UUID string, an unparseable number) embed
 * the record value, and an exception's message reaches the Connect task status, the DLQ headers
 * and the logs, so the writer attaches this copy as the cause of its value-free exception instead
 * of the original (WRT-13, D-05, D-06).
 *
 * <p>Invariants:
 * <ul>
 *   <li>No original text is kept. {@link #getMessage()} and {@link #getLocalizedMessage()} return
 *       the fixed note, and nothing of the original message, localized message or {@code toString()}
 *       is read.
 *   <li>{@link #toString()} is {@code <original class name> <note>}. {@code printStackTrace} prints
 *       it as {@code Caused by: java.lang.IllegalArgumentException (message withheld: may contain
 *       record data)}, so an operator sees the real class and knows the missing text is deliberate.
 *   <li>Suppressed exceptions are dropped: suppression is disabled, so {@code addSuppressed} is a
 *       no-op.
 *   <li>The original's stack frames are copied to each node, so the trace still points at the
 *       failing line.
 *   <li>The copy of a cause cycle ends at the first repeated node, and a chain longer than {@link
 *       #MAX_CAUSE_DEPTH} is cut at that depth, so the copy always terminates.
 * </ul>
 */
public final class WithheldMessageException extends RuntimeException {

    private static final String WITHHELD_NOTE = "(message withheld: may contain record data)";

    /** The most nodes of one chain that are copied. */
    static final int MAX_CAUSE_DEPTH = 32;

    private final String originalClassName;

    private WithheldMessageException(String originalClassName, Throwable sanitizedCause) {
        super(WITHHELD_NOTE, sanitizedCause, false, true);
        this.originalClassName = originalClassName;
    }

    /**
     * Copies {@code original} and its causes, top first, each node once and at most {@link
     * #MAX_CAUSE_DEPTH} nodes.
     */
    public static WithheldMessageException of(Throwable original) {
        Objects.requireNonNull(original, "original");
        List<Throwable> chain = new ArrayList<>();
        Set<Throwable> seen = Collections.newSetFromMap(new IdentityHashMap<>());
        for (Throwable node = original;
                node != null && chain.size() < MAX_CAUSE_DEPTH && seen.add(node);
                node = node.getCause()) {
            chain.add(node);
        }
        WithheldMessageException copy = null;
        for (int i = chain.size() - 1; i >= 0; i--) {
            Throwable node = chain.get(i);
            WithheldMessageException next = new WithheldMessageException(node.getClass().getName(), copy);
            next.setStackTrace(node.getStackTrace());
            copy = next;
        }
        return copy;
    }

    /** The class name of the exception this node stands for. */
    public String getOriginalClassName() {
        return originalClassName;
    }

    @Override
    public String toString() {
        return originalClassName + " " + WITHHELD_NOTE;
    }
}
