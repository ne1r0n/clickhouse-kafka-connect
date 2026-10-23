package com.clickhouse.kafka.connect.sink.db.helper;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.clickhouse.client.api.query.Records;
import com.clickhouse.kafka.connect.sink.ClickHouseBase;
import com.clickhouse.kafka.connect.sink.db.helper.DescribeFixtureCatalog.Fixture;
import com.clickhouse.kafka.connect.sink.helper.ClickHouseTestHelpers;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * The live drift premise of Phase 26 (D-07): the DESCRIBE rows and the production Column snapshot
 * committed under {@code src/test/resources/describe-fixtures} are what the server the suite
 * pulls (a floating {@code latest}) reports today. A failure names the drifted fixture and the
 * live and committed server versions; the fix is a re-capture with {@code captureDescribeFixtures}
 * followed by a reviewed diff.
 *
 * <p>Tagged {@value #DRIFT_TAG} and therefore excluded from {@code ./gradlew test} and {@code
 * check}: an upstream change to DESCRIBE formatting would otherwise turn 94 cases red without any
 * connector change. Run it on demand, or from a separate non-gating job, with {@code
 * env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew describeFixtureDrift}. The Docker-free unit tests
 * over the committed fixtures ({@code DescribeFixturesUnitTest}, {@code
 * DescribeFixtureSnapshotUnitTest}, {@code DescribeTableBuilderUnitTest}) stay in the main suite
 * and carry the regression protection.
 */
@Tag(DescribeFixtureDriftLiveTest.DRIFT_TAG)
class DescribeFixtureDriftLiveTest extends ClickHouseBase {

    // Must equal describeFixtureDriftTag in build.gradle.kts; DescribeFixturesUnitTest asserts it.
    static final String DRIFT_TAG = "describe-drift";

    private static final String DRIFT_PREFIX = "DESCRIBE_FIXTURE_DRIFT";
    private static final String RECAPTURE_COMMAND =
            "env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain captureDescribeFixtures";
    private static final String DRIFT_MESSAGE_FORMAT = "fixture %s: the live %s differs from the committed file"
            + " (live server %s, committed %s); re-capture with `%s` and review the diff";
    private static final String COMMITTED_VERSION_KEY = "version=";

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
                () -> driftMessage(key, "DESCRIBE rows"));
    }

    @ParameterizedTest(name = "DESC-07 production describe: {0} Column snapshot equals the committed snapshot")
    @MethodSource("fixtureKeys")
    void productionDescribeSnapshotEqualsTheCommittedSnapshot(String key) {
        assertEquals(
                DescribeFixtureCatalog.resourceLines(key + DescribeFixtureCatalog.COLUMNS_SUFFIX),
                DescribeFixtureCatalog.renderColumns(chc.describeTable(chc.getDatabase(), key)),
                () -> driftMessage(key, "Column snapshot"));
    }

    private String driftMessage(String key, String what) {
        String committed = DescribeFixtureCatalog.resourceLines(DescribeFixtureCatalog.SERVER_VERSION_FILE).stream()
                .filter(line -> line.startsWith(COMMITTED_VERSION_KEY))
                .map(line -> line.substring(COMMITTED_VERSION_KEY.length()))
                .findFirst()
                .orElse("unknown");
        return String.format(DRIFT_MESSAGE_FORMAT, key, what, chc.version(), committed, RECAPTURE_COMMAND);
    }
}
