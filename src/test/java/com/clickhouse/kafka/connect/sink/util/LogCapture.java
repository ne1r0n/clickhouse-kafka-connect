package com.clickhouse.kafka.connect.sink.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import org.apache.log4j.AppenderSkeleton;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.spi.LoggingEvent;
import org.apache.log4j.spi.ThrowableInformation;

/**
 * Captures the events of one reload4j logger so that a test can assert what the connector wrote
 * to its logs (Phase 18, D-12). reload4j is already on the test classpath through {@code
 * slf4j-reload4j}, so this adds no dependency.
 *
 * <p>Attach it in a try-with-resources block. {@link #close()} detaches the appender and restores
 * the level that the logger had before, so a capture never leaks into a later test. The logger is
 * set to DEBUG for the duration, so a test does not depend on {@code log4j.properties}; TRACE stays
 * out on purpose, because TRACE may carry per-record detail by design.
 *
 * <p>Each event is snapshotted when it is appended: the level name, the rendered message and the
 * throwable texts (every line of the string representation, plus the message of the throwable and
 * of every cause). A sentinel check over {@link #allTexts()} therefore covers all the places a
 * record value could leave through a log call.
 */
public final class LogCapture implements AutoCloseable {

    /** An immutable snapshot of one logging event. */
    public static final class CapturedEvent {
        private final String level;
        private final String message;
        private final List<String> throwableTexts;

        private CapturedEvent(String level, String message, List<String> throwableTexts) {
            this.level = level;
            this.message = message;
            this.throwableTexts = Collections.unmodifiableList(throwableTexts);
        }

        public String level() {
            return level;
        }

        public String message() {
            return message;
        }

        public List<String> throwableTexts() {
            return throwableTexts;
        }

        public boolean hasThrowable() {
            return !throwableTexts.isEmpty();
        }
    }

    private final Logger logger;
    private final Level previousLevel;
    private final AppenderSkeleton appender;
    private final List<CapturedEvent> events = new CopyOnWriteArrayList<>();

    private LogCapture(Logger logger) {
        this.logger = logger;
        this.previousLevel = logger.getLevel();
        this.appender = new AppenderSkeleton() {
            @Override
            protected void append(LoggingEvent event) {
                events.add(snapshot(event));
            }

            @Override
            public void close() {
                // nothing to release; LogCapture.close() detaches this appender
            }

            @Override
            public boolean requiresLayout() {
                return false;
            }
        };
        logger.setLevel(Level.DEBUG);
        logger.addAppender(appender);
    }

    public static LogCapture attach(Class<?> loggerClass) {
        return new LogCapture(Logger.getLogger(loggerClass));
    }

    private static CapturedEvent snapshot(LoggingEvent event) {
        List<String> throwableTexts = new ArrayList<>();
        ThrowableInformation information = event.getThrowableInformation();
        if (information != null) {
            String[] representation = information.getThrowableStrRep();
            if (representation != null) {
                Collections.addAll(throwableTexts, representation);
            }
            for (Throwable t = information.getThrowable(); t != null; t = t.getCause()) {
                if (t.getMessage() != null) {
                    throwableTexts.add(t.getMessage());
                }
            }
        }
        return new CapturedEvent(event.getLevel().toString(), event.getRenderedMessage(), throwableTexts);
    }

    public List<CapturedEvent> events() {
        return new ArrayList<>(events);
    }

    public List<CapturedEvent> events(String level) {
        List<CapturedEvent> matching = new ArrayList<>();
        for (CapturedEvent event : events) {
            if (event.level().equals(level)) {
                matching.add(event);
            }
        }
        return matching;
    }

    /** Every captured message and every captured throwable text, for a sentinel-absence check. */
    public List<String> allTexts() {
        List<String> texts = new ArrayList<>();
        for (CapturedEvent event : events) {
            texts.add(event.message());
            texts.addAll(event.throwableTexts());
        }
        return texts;
    }

    @Override
    public void close() {
        logger.removeAppender(appender);
        logger.setLevel(previousLevel);
    }
}
