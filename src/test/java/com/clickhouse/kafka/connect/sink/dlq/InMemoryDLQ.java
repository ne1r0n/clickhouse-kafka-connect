package com.clickhouse.kafka.connect.sink.dlq;

import com.clickhouse.kafka.connect.sink.util.ThrowableTexts;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.apache.kafka.connect.sink.SinkRecord;

public class InMemoryDLQ implements ErrorReporter {

    /**
     * The three {@code __connect.errors.exception.*} headers Kafka Connect adds to a DLQ record for
     * a reported exception.
     */
    public record ConnectDlqHeaders(String exceptionClassName, String exceptionMessage, String exceptionStacktrace) {}

    private class DLQRecord {
        private SinkRecord record = null;
        private Throwable t = null;

        public DLQRecord(SinkRecord record, Throwable t) {
            this.record = record;
            this.t = t;
        }
    }
    private List<DLQRecord> dlq = null;

    public InMemoryDLQ() {
        this.dlq = new ArrayList<DLQRecord>();

    }

    @Override
    public void report(SinkRecord record, Exception e) {
        dlq.add(new DLQRecord(record, e));
    }

    public int size() {
        return dlq.size();
    }

    /** The reported throwables in report order, as an unmodifiable copy. */
    public List<Throwable> exceptions() {
        List<Throwable> reported = new ArrayList<>();
        for (DLQRecord entry : dlq) {
            reported.add(entry.t);
        }
        return Collections.unmodifiableList(reported);
    }

    /**
     * Renders the exception headers the way Kafka Connect's {@code
     * DeadLetterQueueReporter.populateContextHeaders} does as read in 4.1.1's source (22-RESEARCH
     * F3): the class name from {@code getClass().getName()}, the message from {@code getMessage()}
     * and the stack trace from {@code printStackTrace} as UTF-8.
     * The worker supplies the Connect API at runtime, and this reporter comes from the worker's
     * connect-runtime (Kafka 3.9 on {@code cp-kafka-connect-base:7.9.7}), which is not on the test
     * classpath, so the algorithm is reproduced here. javap of that worker's method shows the same
     * three calls (Phase 25 E-12).
     */
    public static ConnectDlqHeaders connectHeaders(Throwable error) {
        return new ConnectDlqHeaders(
                error.getClass().getName(), error.getMessage(), ThrowableTexts.printedStackTrace(error));
    }
}
