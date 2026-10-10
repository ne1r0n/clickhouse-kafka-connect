package com.clickhouse.kafka.connect.sink.state.provider;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.clickhouse.kafka.connect.ClickHouseSinkConnector;
import com.clickhouse.kafka.connect.sink.ClickHouseBase;
import com.clickhouse.kafka.connect.sink.ClickHouseSinkConfig;
import com.clickhouse.kafka.connect.sink.db.helper.ClickHouseHelperClient;
import com.clickhouse.kafka.connect.sink.helper.ClickHouseTestHelpers;
import com.clickhouse.kafka.connect.sink.state.State;
import com.clickhouse.kafka.connect.sink.state.StateRecord;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * EOS-01: exactly-once state survives a provider restart. Every read in this class goes through a
 * NEW {@link KeeperStateProvider} whose in-memory state map is empty, so the (minOffset,
 * maxOffset, state) triple can only come back from the KeeperMap table in ClickHouse. Proven to
 * fail against the pre-v1.7.0 read that counted rows from the response summary instead of the
 * streamed records (D-15, recorded in 12-04-SUMMARY.md).
 */
class KeeperStateProviderRestartLiveTest extends ClickHouseBase {
    private static final String ID_PREFIX = "eos01_";
    private static final String TOPIC_PREFIX = "t_";
    private static final int WRITTEN_PARTITION = 0;
    private static final int NEVER_WRITTEN_PARTITION = 1;

    private Map<String, String> propsFor(String id, boolean setCompressionMethod) {
        Map<String, String> props = getBaseProps();
        props.put(ClickHouseSinkConnector.CLIENT_VERSION, "V2");
        props.put(ClickHouseSinkConfig.ZK_PATH, "/" + id);
        // zkDatabase names the KeeperMap TABLE; tables sharing a path share data, so it is unique per test.
        props.put(ClickHouseSinkConfig.ZK_DATABASE, id);
        props.put(ClickHouseSinkConfig.SET_COMPRESSION_METHOD, String.valueOf(setCompressionMethod));
        return props;
    }

    private KeeperStateProvider newProvider(Map<String, String> props) {
        return new KeeperStateProvider(new ClickHouseSinkConfig(props));
    }

    private void dropStateTable(Map<String, String> props, String id) {
        try (ClickHouseHelperClient chc = ClickHouseTestHelpers.createClient(props)) {
            ClickHouseTestHelpers.dropTable(chc, id);
        }
    }

    private static void assertStateRead(StateRecord read, State state, long minOffset, long maxOffset) {
        assertEquals(state, read.getState());
        assertEquals(minOffset, read.getMinOffset());
        assertEquals(maxOffset, read.getMaxOffset());
    }

    static Stream<Arguments> flagAndOffsets() {
        long beyondInt = Integer.MAX_VALUE;
        return Stream.of(
                Arguments.of(true, 10L, 19L),
                Arguments.of(false, 10L, 19L),
                Arguments.of(true, beyondInt + 10L, beyondInt + 19L),
                Arguments.of(false, beyondInt + 10L, beyondInt + 19L));
    }

    @ParameterizedTest(name = "setCompressionMethod={0} min={1} max={2}")
    @MethodSource("flagAndOffsets")
    @DisplayName("EOS-01: state written by one provider is read back exactly by a fresh provider")
    void stateWrittenByOneProviderIsReadBackByAFreshProvider(
            boolean setCompressionMethod, long minOffset, long maxOffset) {
        String id = ID_PREFIX + System.nanoTime();
        String topic = TOPIC_PREFIX + id;
        Map<String, String> props = propsFor(id, setCompressionMethod);
        try {
            KeeperStateProvider writer = newProvider(props);
            // StateRecord takes maxOffset BEFORE minOffset; distinct values make a swap visible.
            writer.setStateRecord(
                    new StateRecord(topic, WRITTEN_PARTITION, maxOffset, minOffset, State.BEFORE_PROCESSING, topic));

            assertStateRead(
                    newProvider(props).getStateRecord(topic, WRITTEN_PARTITION),
                    State.BEFORE_PROCESSING,
                    minOffset,
                    maxOffset);

            writer.setStateRecord(
                    new StateRecord(topic, WRITTEN_PARTITION, maxOffset, minOffset, State.AFTER_PROCESSING, topic));

            assertStateRead(
                    newProvider(props).getStateRecord(topic, WRITTEN_PARTITION),
                    State.AFTER_PROCESSING,
                    minOffset,
                    maxOffset);
        } finally {
            dropStateTable(props, id);
        }
    }

    @ParameterizedTest(name = "setCompressionMethod={0}")
    @ValueSource(booleans = {true, false})
    @DisplayName("EOS-01: a topic-partition key that was never written reads back as NONE from a fresh provider")
    void freshKeyReadsBackNone(boolean setCompressionMethod) {
        String id = ID_PREFIX + System.nanoTime();
        Map<String, String> props = propsFor(id, setCompressionMethod);
        try {
            assertStateRead(
                    newProvider(props).getStateRecord(TOPIC_PREFIX + id, NEVER_WRITTEN_PARTITION), State.NONE, 0, 0);
        } finally {
            dropStateTable(props, id);
        }
    }
}
