package com.clickhouse.kafka.connect.sink.db.helper;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.clickhouse.client.api.query.Records;
import com.clickhouse.kafka.connect.sink.ClickHouseBase;
import com.clickhouse.kafka.connect.sink.db.helper.DescribeFixtureCatalog.Fixture;
import com.clickhouse.kafka.connect.sink.helper.ClickHouseTestHelpers;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * The live drift premise of Phase 26 (D-07): the DESCRIBE rows and the production Column snapshot
 * committed under {@code src/test/resources/describe-fixtures} are what the server the suite
 * pulls (a floating {@code latest}) reports today. A failure names the drifted fixture; the fix is
 * a re-capture with {@code captureDescribeFixtures} followed by a reviewed diff. Full suite only
 * (it extends {@link ClickHouseBase}).
 */
class DescribeFixtureDriftLiveTest extends ClickHouseBase {

    private static final String DRIFT_PREFIX = "DESCRIBE_FIXTURE_DRIFT";
    private static final String RECAPTURE_COMMAND =
            "env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain captureDescribeFixtures";
    private static final String DRIFT_MESSAGE_FORMAT =
            "fixture %s: the live %s differs from the committed file; re-capture with `%s` and review the diff";

    private ClickHouseHelperClient chc;

    static Stream<String> fixtureKeys() {
        return DescribeFixtureCatalog.FIXTURES.stream().map(Fixture::key);
    }

    @BeforeAll
    void createFixtureTables() {
        chc = ClickHouseTestHelpers.createClient(getBaseProps());
        for (Fixture fixture : DescribeFixtureCatalog.FIXTURES) {
            try (Records ignored =
                    chc.queryV2(DescribeFixtureCatalog.createTableSql(fixture, "`" + fixture.key() + "`"))) {
                // table created
            } catch (Exception e) {
                throw new IllegalStateException("could not create fixture table " + fixture.key(), e);
            }
        }
        System.out.println(DRIFT_PREFIX + " client=" + (chc.isUseClientV2() ? "V2" : "V1") + " server="
                + chc.version());
    }

    @AfterAll
    void closeClient() {
        chc.close();
    }

    @ParameterizedTest(name = "DESC-06 drift premise: {0} DESCRIBE rows equal the committed fixture")
    @MethodSource("fixtureKeys")
    void liveDescribeRowsEqualTheCommittedFixture(String key) throws Exception {
        assertEquals(
                DescribeFixtureCatalog.resourceLines(key + DescribeFixtureCatalog.JSONL_SUFFIX),
                DescribeFixtureCatalog.describeRawLines(chc, key),
                String.format(DRIFT_MESSAGE_FORMAT, key, "DESCRIBE rows", RECAPTURE_COMMAND));
    }

    @ParameterizedTest(name = "DESC-07 production describe: {0} Column snapshot equals the committed snapshot")
    @MethodSource("fixtureKeys")
    void productionDescribeSnapshotEqualsTheCommittedSnapshot(String key) {
        assertEquals(
                DescribeFixtureCatalog.resourceLines(key + DescribeFixtureCatalog.COLUMNS_SUFFIX),
                DescribeFixtureCatalog.renderColumns(chc.describeTable(chc.getDatabase(), key)),
                String.format(DRIFT_MESSAGE_FORMAT, key, "Column snapshot", RECAPTURE_COMMAND));
    }
}
