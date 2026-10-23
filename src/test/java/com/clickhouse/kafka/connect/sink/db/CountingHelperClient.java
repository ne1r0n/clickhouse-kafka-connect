package com.clickhouse.kafka.connect.sink.db;

import com.clickhouse.kafka.connect.sink.db.helper.ClickHouseHelperClient;

/**
 * A real {@link ClickHouseHelperClient} that answers {@code isUseClientV2} with its constructor
 * flag and counts {@code pinReplica} and {@code unpinReplica}, the other two calls
 * {@code ClickHouseWriter.doInsertRawBinary} makes on it. Its constructor opens no connection and
 * starts no thread of its own, so the dummy builder target is never contacted. It deliberately uses
 * no Mockito: inline-mocking {@code ClickHouseHelperClient} crashed the Semeru/OpenJ9 test JVM
 * natively inside {@code retransformClasses} in the full suite (Phase 18 deferred item, HARN-08).
 * Callers close it with try-with-resources; the inherited {@code close()} closes the V2 client. It
 * holds no static mutable state, so counts never leak between tests.
 */
public final class CountingHelperClient extends ClickHouseHelperClient {

    private static final String DUMMY_HOSTNAME = "127.0.0.1";
    private static final int DUMMY_PORT = 8123;

    private final boolean useClientV2;
    private int pinCalls = 0;
    private int unpinCalls = 0;

    public CountingHelperClient(boolean useClientV2) {
        super(new ClickHouseHelperClient.ClickHouseClientBuilder(DUMMY_HOSTNAME, DUMMY_PORT, null, null, -1));
        this.useClientV2 = useClientV2;
    }

    @Override
    public boolean isUseClientV2() {
        return useClientV2;
    }

    @Override
    public void pinReplica() {
        pinCalls++;
    }

    @Override
    public void unpinReplica() {
        unpinCalls++;
    }

    public int pinCalls() {
        return pinCalls;
    }

    public int unpinCalls() {
        return unpinCalls;
    }
}
