package com.clickhouse.kafka.connect.sink.db;

import com.clickhouse.client.ClickHouseResponse;
import com.clickhouse.client.api.query.Records;
import com.clickhouse.kafka.connect.sink.db.helper.ClickHouseHelperClient;
import com.clickhouse.kafka.connect.sink.db.mapping.Table;
import java.util.List;
import java.util.Map;

/**
 * A real {@link ClickHouseHelperClient} that counts {@code pinReplica}, {@code unpinReplica} and
 * {@code close}, the calls {@code ClickHouseWriter.doInsertRawBinary} and the connector's lifecycle
 * make on it. It answers {@code isUseClientV2} with its constructor flag, or, when built from a real
 * {@link ClickHouseHelperClient.ClickHouseClientBuilder}, from that builder. It scripts the server
 * answers a writer or a state provider asks for at start (ping, version, table mapping, the two
 * query calls), so neither needs a server; anything left unscripted falls back to the real class.
 * Its constructors open no connection and start no thread of their own. It deliberately uses no
 * Mockito: inline-mocking {@code ClickHouseHelperClient} crashed the Semeru/OpenJ9 test JVM natively
 * inside {@code retransformClasses} in the full suite (Phase 18 deferred item, HARN-08). Callers
 * close it with try-with-resources; the inherited {@code close()} closes the V2 client. It holds no
 * static mutable state, so counts never leak between tests.
 */
public final class CountingHelperClient extends ClickHouseHelperClient {

    private static final String DUMMY_HOSTNAME = "127.0.0.1";
    private static final int DUMMY_PORT = 8123;

    private final Boolean forcedUseClientV2;
    private int pinCalls = 0;
    private int unpinCalls = 0;
    private int closeCalls = 0;
    private Boolean scriptedPing = null;
    private String scriptedVersion = null;
    private RuntimeException scriptedVersionFailure = null;
    private List<Table> scriptedTables = null;
    private RuntimeException scriptedTablesFailure = null;
    private RuntimeException scriptedQueryV2Failure = null;
    private boolean emptyQueryV1Response = false;
    private Runnable closeHook = null;

    public CountingHelperClient(boolean useClientV2) {
        super(new ClickHouseHelperClient.ClickHouseClientBuilder(DUMMY_HOSTNAME, DUMMY_PORT, null, null, -1));
        this.forcedUseClientV2 = useClientV2;
    }

    public CountingHelperClient(ClickHouseHelperClient.ClickHouseClientBuilder builder) {
        super(builder);
        this.forcedUseClientV2 = null;
    }

    public CountingHelperClient pingReturns(boolean answer) {
        this.scriptedPing = answer;
        return this;
    }

    public CountingHelperClient versionReturns(String version) {
        this.scriptedVersion = version;
        return this;
    }

    public CountingHelperClient versionThrows(RuntimeException failure) {
        this.scriptedVersionFailure = failure;
        return this;
    }

    public CountingHelperClient tablesReturn(List<Table> tables) {
        this.scriptedTables = tables;
        return this;
    }

    public CountingHelperClient tablesThrow(RuntimeException failure) {
        this.scriptedTablesFailure = failure;
        return this;
    }

    public CountingHelperClient queryV2Throws(RuntimeException failure) {
        this.scriptedQueryV2Failure = failure;
        return this;
    }

    public CountingHelperClient queryV1ReturnsEmptyResponse() {
        this.emptyQueryV1Response = true;
        return this;
    }

    public CountingHelperClient onClose(Runnable hook) {
        this.closeHook = hook;
        return this;
    }

    @Override
    public boolean isUseClientV2() {
        return forcedUseClientV2 != null ? forcedUseClientV2 : super.isUseClientV2();
    }

    @Override
    public boolean ping() {
        return scriptedPing != null ? scriptedPing : super.ping();
    }

    @Override
    public String version() {
        if (scriptedVersionFailure != null) {
            throw scriptedVersionFailure;
        }
        return scriptedVersion != null ? scriptedVersion : super.version();
    }

    @Override
    public List<Table> extractTablesMapping(String database, Map<String, Table> cache) {
        if (scriptedTablesFailure != null) {
            throw scriptedTablesFailure;
        }
        return scriptedTables != null ? scriptedTables : super.extractTablesMapping(database, cache);
    }

    @Override
    public Records queryV2(String query) {
        if (scriptedQueryV2Failure != null) {
            throw scriptedQueryV2Failure;
        }
        return super.queryV2(query);
    }

    @Override
    public ClickHouseResponse queryV1(String query) {
        return emptyQueryV1Response ? ClickHouseResponse.EMPTY : super.queryV1(query);
    }

    @Override
    public void pinReplica() {
        pinCalls++;
    }

    @Override
    public void unpinReplica() {
        unpinCalls++;
    }

    @Override
    public void close() {
        closeCalls++;
        if (closeHook != null) {
            closeHook.run();
        }
        super.close();
    }

    public int pinCalls() {
        return pinCalls;
    }

    public int unpinCalls() {
        return unpinCalls;
    }

    public int closeCalls() {
        return closeCalls;
    }
}
