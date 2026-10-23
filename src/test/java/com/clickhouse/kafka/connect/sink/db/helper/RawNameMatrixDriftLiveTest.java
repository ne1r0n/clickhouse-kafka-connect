package com.clickhouse.kafka.connect.sink.db.helper;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.clickhouse.kafka.connect.sink.ClickHouseBase;
import com.clickhouse.kafka.connect.sink.db.helper.RawNameMatrix.Entry;
import com.clickhouse.kafka.connect.sink.helper.ClickHouseTestHelpers;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * The live drift premise of the Phase 28 name matrix: the DESCRIBE rows committed under {@code
 * src/test/resources/raw-name-matrix} are what the floating {@code latest} server reports today.
 * Tagged like {@link DescribeFixtureDriftLiveTest} and therefore run by {@code describeFixtureDrift}
 * only, never by {@code test}. A failure names the entry, the file and the live and committed server
 * versions; the fix is a re-run of {@code RawNameMatrixLiveTest} and a reviewed copy of its capture.
 */
@Tag(DescribeFixtureDriftLiveTest.DRIFT_TAG)
class RawNameMatrixDriftLiveTest extends ClickHouseBase {

    private static final String TABLE_PREFIX = "rnmd_";
    private static final String NAMES_TABLE_SUFFIX = "_n";
    private static final String ENUM_TABLE_SUFFIX = "_e";
    private static final String COMMITTED_VERSION_KEY = "version=";
    private static final String DRIFT_MESSAGE_FORMAT = "entry %s: the live rows differ from the committed file %s"
            + " (live server %s, committed %s); re-run RawNameMatrixLiveTest and review the capture diff";

    private ClickHouseHelperClient chc;

    static Stream<Entry> entries() {
        return RawNameMatrix.ENTRIES.stream();
    }

    @BeforeAll
    void createClient() {
        chc = ClickHouseTestHelpers.createClient(getBaseProps());
    }

    @AfterAll
    void closeClient() {
        chc.close();
    }

    @ParameterizedTest(name = "NAME-02 drift: live DESCRIBE rows of entry {0} equal the committed rows")
    @MethodSource("entries")
    void liveRowsEqualTheCommittedRows(Entry entry) throws Exception {
        String namesTable = createTopicName(TABLE_PREFIX + entry.key() + NAMES_TABLE_SUFFIX);
        String enumTable = createTopicName(TABLE_PREFIX + entry.key() + ENUM_TABLE_SUFFIX);
        try {
            RawNameMatrix.createTable(chc, RawNameMatrix.namesTableDdl(entry), namesTable);
            RawNameMatrix.createTable(chc, RawNameMatrix.enumTableDdl(entry), enumTable);
            assertRowsEqual(entry, entry.key() + RawNameMatrix.NAMES_SUFFIX, namesTable);
            assertRowsEqual(entry, entry.key() + RawNameMatrix.ENUM_SUFFIX, enumTable);
        } finally {
            ClickHouseTestHelpers.dropTable(chc, namesTable);
            ClickHouseTestHelpers.dropTable(chc, enumTable);
        }
    }

    private void assertRowsEqual(Entry entry, String fileName, String tableName) throws Exception {
        List<String> committed = DescribeFixtureCatalog.resourceLines(RawNameMatrix.RESOURCE_DIRECTORY, fileName);
        assertEquals(
                committed,
                DescribeFixtureCatalog.describeRawLines(chc, tableName),
                () -> String.format(DRIFT_MESSAGE_FORMAT, entry.key(), fileName, chc.version(), committedVersion()));
    }

    private String committedVersion() {
        return DescribeFixtureCatalog.resourceLines(RawNameMatrix.RESOURCE_DIRECTORY, RawNameMatrix.SERVER_VERSION_FILE)
                .stream()
                .filter(line -> line.startsWith(COMMITTED_VERSION_KEY))
                .map(line -> line.substring(COMMITTED_VERSION_KEY.length()))
                .findFirst()
                .orElse("unknown");
    }
}
