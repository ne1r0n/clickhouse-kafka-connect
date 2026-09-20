package com.clickhouse.kafka.connect.sink;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.clickhouse.kafka.connect.sink.helper.ClickHouseTestHelpers;
import org.junit.jupiter.api.Test;

/**
 * [D-04] Docker-free pin for the seventh fork behaviour to vanish from the v1.6.0 merge with no
 * conflict marker and no compile error: {@code CLICKHOUSE_VERSION_DEFAULT} came out of the merge
 * as a pinned LTS version, so every local gate since the merge (including two prior phases' green
 * closes) took its green evidence on the wrong server. CI pins {@code CLICKHOUSE_VERSION: latest}
 * in its job environment, so CI could never have surfaced this - only a Docker-free assertion
 * inside {@code testUnit} can. This class deliberately extends nothing - not {@link
 * ClickHouseBase}, whose inherited {@code @Tag("clickhouse")} would drop it out of {@code
 * testUnit}. It asserts the default value itself, not the composed image name -
 * {@code ClickHouseTestHarnessTest}'s existing image-name assertion is self-consistent whatever
 * the default happens to be and would not have caught this regression.
 */
public class ClickHouseVersionDefaultUnitTest {

    @Test
    void clickhouseVersionDefaultIsTheFloatingLatestTag() {
        assertEquals(
                "latest",
                ClickHouseTestHelpers.CLICKHOUSE_VERSION_DEFAULT,
                "A pinned server version here silently moves every local run off the ClickHouse "
                        + "server this milestone declares it tests");
        assertEquals(
                "latest",
                ClickHouseTestHelpers.CLICKHOUSE_VERSION_LATEST,
                "The named constant itself must stay the floating tag, so the indirection above "
                        + "cannot be defeated by redefining it");
    }
}
