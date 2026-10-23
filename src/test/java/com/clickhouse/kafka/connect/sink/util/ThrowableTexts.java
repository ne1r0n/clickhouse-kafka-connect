package com.clickhouse.kafka.connect.sink.util;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;

/**
 * Test helper that turns a thrown chain into the texts an operator could read, so that a sentinel
 * check covers every place a record value could leave through an exception: each message, each
 * localized message, each {@code toString()}, the suppressed entries and the rendered stack trace.
 * All walks are identity-guarded, so a cause cycle ends instead of looping.
 */
public final class ThrowableTexts {

    private ThrowableTexts() {}

    /** The throwable and its causes, top first, each node once. */
    public static List<Throwable> causeChain(Throwable top) {
        List<Throwable> chain = new ArrayList<>();
        Set<Throwable> seen = Collections.newSetFromMap(new IdentityHashMap<>());
        for (Throwable t = top; t != null && seen.add(t); t = t.getCause()) {
            chain.add(t);
        }
        return chain;
    }

    /**
     * Every text reachable from {@code top} through cause and suppressed edges (message, localized
     * message and {@code toString()} of each node, nulls skipped), followed by {@link
     * #printedStackTrace(Throwable)} of {@code top}.
     */
    public static List<String> allTexts(Throwable top) {
        List<String> texts = new ArrayList<>();
        Set<Throwable> seen = Collections.newSetFromMap(new IdentityHashMap<>());
        collect(top, seen, texts);
        texts.add(printedStackTrace(top));
        return texts;
    }

    private static void collect(Throwable t, Set<Throwable> seen, List<String> texts) {
        if (t == null || !seen.add(t)) {
            return;
        }
        addIfPresent(texts, t.getMessage());
        addIfPresent(texts, t.getLocalizedMessage());
        addIfPresent(texts, t.toString());
        for (Throwable suppressed : t.getSuppressed()) {
            collect(suppressed, seen, texts);
        }
        collect(t.getCause(), seen, texts);
    }

    private static void addIfPresent(List<String> texts, String text) {
        if (text != null) {
            texts.add(text);
        }
    }

    /**
     * Renders {@code printStackTrace} as the Connect task status and Kafka Connect's DLQ {@code
     * exception.stacktrace} header do, per {@code DeadLetterQueueReporter.populateContextHeaders} as
     * read in 4.1.1's source (22-RESEARCH F3).
     * The worker supplies the Connect API at runtime, and that reporter comes from the worker's
     * connect-runtime (Kafka 3.9 on {@code cp-kafka-connect-base:7.9.7}); javap of its private
     * {@code stacktrace} method shows a {@code Throwable.printStackTrace(PrintStream)} call (Phase 25
     * E-12). The charset is explicit UTF-8 on both the
     * print and the decode side, because the test JVM runs windows-1252 and would otherwise turn a
     * non-BMP character in a value into question marks, hiding it from a sentinel check.
     */
    public static String printedStackTrace(Throwable t) {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (PrintStream out = new PrintStream(bytes, true, StandardCharsets.UTF_8)) {
            t.printStackTrace(out);
        }
        return bytes.toString(StandardCharsets.UTF_8);
    }
}
