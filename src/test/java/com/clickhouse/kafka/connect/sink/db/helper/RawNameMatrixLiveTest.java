package com.clickhouse.kafka.connect.sink.db.helper;

import com.clickhouse.kafka.connect.ClickHouseSinkConnector;
import com.clickhouse.kafka.connect.sink.ClickHouseBase;
import com.clickhouse.kafka.connect.sink.db.helper.RawNameMatrix.Entry;
import com.clickhouse.kafka.connect.sink.db.helper.RawNameMatrix.Results;
import com.clickhouse.kafka.connect.sink.db.mapping.DeclaredNullability;
import com.clickhouse.kafka.connect.sink.db.mapping.Table;
import com.clickhouse.kafka.connect.sink.helper.ClickHouseTestHelpers;
import com.clickhouse.kafka.connect.sink.util.LogCapture;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The live NAME-02 spike of Phase 28: for every character of {@link RawNameMatrix#ENTRIES} the
 * server's DESCRIBE JSON carries the raw identifier, in a names table and in an enum table. The
 * test captures the server's rows under {@code build/raw-name-matrix} (the source of the committed
 * fixtures) and renders, per shape, the server's JSON text, the form the production {@code
 * describeTable} yields under client V2 and under client V1 (each built explicitly, whatever
 * {@code CLIENT_VERSION} says) and the decoded form. It asserts only the premises that hold before
 * and after the name decode; the forms are recorded, never asserted, and never logged.
 */
class RawNameMatrixLiveTest extends ClickHouseBase {

    private static final Logger LOGGER = LoggerFactory.getLogger(RawNameMatrixLiveTest.class);
    private static final String TABLE_PREFIX = "rnm_";
    private static final String NAMES_TABLE_SUFFIX = "_n";
    private static final String ENUM_TABLE_SUFFIX = "_e";
    private static final String CONTROL_TABLE_SUFFIX = "_c";
    private static final String CELLS_FILE_FORMAT = "cells-%s.txt";
    private static final String VERSION_LINE_FORMAT = "version=%s";
    private static final String WARN_LEVEL = "WARN";

    private ClickHouseHelperClient chcV2;
    private ClickHouseHelperClient chcV1;

    static Stream<Entry> entries() {
        return RawNameMatrix.ENTRIES.stream();
    }

    @BeforeAll
    void createClients() {
        chcV2 = clientOf("V2");
        chcV1 = clientOf("V1");
    }

    @AfterAll
    void closeClients() {
        chcV2.close();
        chcV1.close();
    }

    private ClickHouseHelperClient clientOf(String clientVersion) {
        Map<String, String> props = getBaseProps();
        props.put(ClickHouseSinkConnector.CLIENT_VERSION, clientVersion);
        return ClickHouseTestHelpers.createClient(props);
    }

    @ParameterizedTest(name = "NAME-02 spike: the server JSON carries the raw identifier for every row of entry {0}")
    @MethodSource("entries")
    void serverJsonCarriesTheRawIdentifier(Entry entry) throws Exception {
        String namesTable = createTopicName(TABLE_PREFIX + entry.key() + NAMES_TABLE_SUFFIX);
        String enumTable = createTopicName(TABLE_PREFIX + entry.key() + ENUM_TABLE_SUFFIX);
        String controlTable = createTopicName(TABLE_PREFIX + RawNameMatrix.CONTROL_KEY + CONTROL_TABLE_SUFFIX);
        Entry control = RawNameMatrix.ENTRIES.get(0);
        try {
            RawNameMatrix.createTable(chcV2, RawNameMatrix.namesTableDdl(entry), namesTable);
            RawNameMatrix.createTable(chcV2, RawNameMatrix.enumTableDdl(entry), enumTable);
            RawNameMatrix.createTable(chcV2, RawNameMatrix.namesTableDdl(control), controlTable);

            List<String> namesLines = DescribeFixtureCatalog.describeRawLines(chcV2, namesTable);
            List<String> enumLines = DescribeFixtureCatalog.describeRawLines(chcV2, enumTable);
            List<String> controlLines = DescribeFixtureCatalog.describeRawLines(chcV2, controlTable);
            RawNameMatrix.writeCapture(entry.key() + RawNameMatrix.NAMES_SUFFIX, namesLines);
            RawNameMatrix.writeCapture(entry.key() + RawNameMatrix.ENUM_SUFFIX, enumLines);

            Results results = new Results(
                    RawNameMatrix.describe(chcV2, namesTable),
                    RawNameMatrix.describe(chcV1, namesTable),
                    RawNameMatrix.describe(chcV2, enumTable),
                    RawNameMatrix.describe(chcV1, enumTable));
            RawNameMatrix.writeCapture(
                    String.format(CELLS_FILE_FORMAT, entry.key()),
                    RawNameMatrix.renderCells(entry, namesLines, enumLines, results));

            RawNameMatrix.assertServerPremises(entry, namesLines, enumLines, controlLines);
        } finally {
            ClickHouseTestHelpers.dropTable(chcV2, namesTable);
            ClickHouseTestHelpers.dropTable(chcV2, enumTable);
            ClickHouseTestHelpers.dropTable(chcV2, controlTable);
        }
    }

    @ParameterizedTest(name = "NAME-03 client V1: describeTable returns the raw identifiers of entry {0}")
    @MethodSource("entries")
    void clientV1DescribesTheRawIdentifiers(Entry entry) {
        String namesTable = createTopicName(TABLE_PREFIX + entry.key() + NAMES_TABLE_SUFFIX);
        try {
            RawNameMatrix.createTable(chcV2, RawNameMatrix.namesTableDdl(entry), namesTable);
            Table table;
            List<LogCapture.CapturedEvent> warnings = new ArrayList<>();
            try (LogCapture helperLog = LogCapture.attach(ClickHouseHelperClient.class);
                    LogCapture nullabilityLog = LogCapture.attach(DeclaredNullability.class)) {
                table = chcV1.describeTable(chcV1.getDatabase(), namesTable);
                warnings.addAll(helperLog.events(WARN_LEVEL));
                warnings.addAll(nullabilityLog.events(WARN_LEVEL));
            }
            RawNameMatrix.assertDiscoveredUnderRawNames(entry, table, warnings);
        } finally {
            ClickHouseTestHelpers.dropTable(chcV2, namesTable);
        }
    }

    @Test
    @DisplayName("NAME-02 spike: server version is recorded")
    void serverVersionIsRecorded() throws Exception {
        String version = chcV2.version();
        LOGGER.info("RAW_NAME_MATRIX server version={}", version);
        RawNameMatrix.writeCapture(
                RawNameMatrix.SERVER_VERSION_FILE, List.of(String.format(VERSION_LINE_FORMAT, version)));
    }
}
