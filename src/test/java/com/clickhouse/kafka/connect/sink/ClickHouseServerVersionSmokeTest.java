package com.clickhouse.kafka.connect.sink;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.clickhouse.kafka.connect.sink.db.helper.ClickHouseHelperClient;
import com.clickhouse.kafka.connect.sink.helper.ClickHouseTestHelpers;
import java.util.Map;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Server-version smoke test for the shared {@link ClickHouseBase} container (INFRA-02, D-06).
 *
 * <p>D-06 deviation note: no ClickHouse image carries a usable version label to compare
 * against for the {@code latest} tag. ClickHouse's {@code Dockerfile.ubuntu} has no {@code
 * LABEL} instructions, and {@code org.opencontainers.image.version}, where present at all,
 * reflects the Ubuntu base image version, not the ClickHouse semantic version (see ClickHouse
 * issue #54224, "No version in docker inspect output for clickhouse-server latest"). The
 * closest observable check is therefore threefold: the container image reference equals {@link
 * ClickHouseTestHelpers#CLICKHOUSE_DOCKER_IMAGE}, the reported {@code SELECT VERSION()} string
 * is well-formed ({@code MAJOR.MINOR.PATCH.BUILD}), and the logged version is paired with the
 * image RepoDigest recorded in the phase SUMMARY for auditability.
 */
public class ClickHouseServerVersionSmokeTest extends ClickHouseBase {
    private static final Logger LOGGER = LoggerFactory.getLogger(ClickHouseServerVersionSmokeTest.class);

    static boolean versionMatchesRequestedTag(String serverVersion, String requestedTag) {
        if (serverVersion == null || serverVersion.isBlank()) {
            return false;
        }
        if (requestedTag != null && requestedTag.equalsIgnoreCase("latest")) {
            return serverVersion.matches("^\\d+\\.\\d+\\.\\d+\\.\\d+$");
        }
        return serverVersion.equals(requestedTag) || serverVersion.startsWith(requestedTag + ".");
    }

    @Test
    public void versionMatchPredicatePinsEdgeCases() {
        assertTrue(versionMatchesRequestedTag("25.3.2.39", "25.3"));
        assertTrue(!versionMatchesRequestedTag("25.30.1.1", "25.3"));
        assertTrue(!versionMatchesRequestedTag("26.8.5.13", "25.3"));
        assertTrue(versionMatchesRequestedTag("25.3.2.39", "25.3.2.39"));

        assertTrue(versionMatchesRequestedTag("26.8.5.13", "latest"));
        assertTrue(versionMatchesRequestedTag("26.8.5.13", "LATEST"));
        assertTrue(!versionMatchesRequestedTag("26.8", "latest"));
        assertTrue(!versionMatchesRequestedTag("", "latest"));
        assertTrue(!versionMatchesRequestedTag(null, "25.3"));
    }

    @Test
    public void serverVersionMatchesRequestedImage() {
        Assumptions.assumeFalse(isCloud, "No container to inspect against ClickHouse Cloud");

        Map<String, String> props = getBaseProps();
        ClickHouseHelperClient chc = ClickHouseTestHelpers.createClient(props);
        String serverVersion = chc.version();
        String requestedTag = ClickHouseTestHelpers.getClickhouseVersion();

        LOGGER.info(
                "ClickHouse server version: {} (requested tag: {}, image: {})",
                serverVersion,
                requestedTag,
                db.getDockerImageName());

        assertEquals(
                ClickHouseTestHelpers.CLICKHOUSE_DOCKER_IMAGE,
                db.getDockerImageName(),
                "Container was not built from the requested CLICKHOUSE_DOCKER_IMAGE");
        assertTrue(
                versionMatchesRequestedTag(serverVersion, requestedTag),
                () -> String.format(
                        "Expected server version '%s' to match requested tag '%s'", serverVersion, requestedTag));
    }
}
