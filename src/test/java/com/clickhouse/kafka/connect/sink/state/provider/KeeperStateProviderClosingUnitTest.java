package com.clickhouse.kafka.connect.sink.state.provider;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.clickhouse.kafka.connect.ClickHouseSinkConnector;
import com.clickhouse.kafka.connect.sink.ClickHouseSinkConfig;
import com.clickhouse.kafka.connect.sink.db.CountingHelperClient;
import com.clickhouse.kafka.connect.sink.db.helper.ClickHouseHelperClient;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Counts the helper client {@link KeeperStateProvider} builds through its factory constructor, on
 * client V1 and V2, with the KeeperMap table creation scripted so no server is needed. Each helper
 * is a {@link CountingHelperClient}, so no Mockito is involved and the class stays in the
 * Docker-free {@code testUnit} loop.
 */
class KeeperStateProviderClosingUnitTest {

    private static final String PARAMETERIZED_NAME = "{displayName}[{index}] client_version={0}";
    private static final String LOOPBACK = "127.0.0.1";
    private static final String UNUSED_PORT = "8123";
    private static final int UNUSED_PORT_NUMBER = 8123;
    private static final String KEEPER_TABLE_FAILURE = "scripted keeper table creation failure";
    private static final String OPEN_COUNT = "open count of helper clients";
    private static final String CLOSE_COUNT = "close count of the state provider's helper client";
    private static final String TRIPWIRE = "the injected-client constructor still throws before an instance exists;"
            + " if it is ever repaired, add a case asserting that close() leaves an injected client open";
    private static final String CLIENT_V2 = "V2";

    private static ClickHouseSinkConfig config(String clientVersion) {
        Map<String, String> props = new HashMap<>();
        props.put(ClickHouseSinkConnector.HOSTNAME, LOOPBACK);
        props.put(ClickHouseSinkConnector.PORT, UNUSED_PORT);
        props.put(ClickHouseSinkConnector.CLIENT_VERSION, clientVersion);
        return new ClickHouseSinkConfig(props);
    }

    private static KeeperStateProvider provider(String clientVersion, List<CountingHelperClient> created) {
        return new KeeperStateProvider(config(clientVersion), builder -> {
            CountingHelperClient helper = new CountingHelperClient(builder)
                    .pingReturns(true)
                    .queryV2Throws(new IllegalStateException(KEEPER_TABLE_FAILURE))
                    .queryV1ReturnsEmptyResponse();
            created.add(helper);
            return helper;
        });
    }

    private static void closeLeftoverHelpers(List<CountingHelperClient> created) {
        for (CountingHelperClient helper : created) {
            if (helper.closeCalls() == 0) {
                helper.close();
            }
        }
    }

    @ParameterizedTest(name = PARAMETERIZED_NAME)
    @ValueSource(strings = {"V1", CLIENT_V2})
    void closeClosesTheHelperClientTheProviderCreated(String clientVersion) {
        List<CountingHelperClient> created = new ArrayList<>();
        try {
            KeeperStateProvider provider = provider(clientVersion, created);
            assertEquals(1, created.size(), OPEN_COUNT);
            assertEquals(CLIENT_V2.equals(clientVersion), created.get(0).isUseClientV2());
            provider.close();
            assertEquals(1, created.get(0).closeCalls(), CLOSE_COUNT);
        } finally {
            closeLeftoverHelpers(created);
        }
    }

    @ParameterizedTest(name = PARAMETERIZED_NAME)
    @ValueSource(strings = {"V1", CLIENT_V2})
    void secondCloseDoesNotCloseAgain(String clientVersion) {
        List<CountingHelperClient> created = new ArrayList<>();
        try {
            KeeperStateProvider provider = provider(clientVersion, created);
            provider.close();
            provider.close();
            assertEquals(1, created.size(), OPEN_COUNT);
            assertEquals(1, created.get(0).closeCalls(), CLOSE_COUNT);
        } finally {
            closeLeftoverHelpers(created);
        }
    }

    @Test
    void injectedClientConstructorStillFailsBeforeAnyInstanceExistsTripwire() {
        CountingHelperClient helper = new CountingHelperClient(
                        new ClickHouseHelperClient.ClickHouseClientBuilder(LOOPBACK, UNUSED_PORT_NUMBER, null, null, -1))
                .pingReturns(true);
        try {
            assertThrows(NullPointerException.class, () -> new KeeperStateProvider(helper), TRIPWIRE);
            assertEquals(0, helper.closeCalls(), TRIPWIRE);
        } finally {
            helper.close();
        }
    }
}
