package com.clickhouse.kafka.connect.sink.db.helper;

import com.clickhouse.kafka.connect.ClickHouseSinkConnector;
import com.clickhouse.kafka.connect.sink.db.helper.DescribeFixtureCatalog.Fixture;
import com.clickhouse.kafka.connect.sink.db.mapping.DeclaredNullability;
import com.clickhouse.kafka.connect.sink.helper.ClickHouseTestHelpers;
import com.clickhouse.kafka.connect.sink.util.LogCapture;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.testcontainers.clickhouse.ClickHouseContainer;
import org.testcontainers.images.PullPolicy;

/**
 * The capture harness of Phase 26 (D-06), run by {@code captureDescribeFixtures} in {@code
 * build.gradle.kts}: starts the ClickHouse image the suite uses, creates every {@link
 * DescribeFixtureCatalog#FIXTURES} table from its committed DDL and writes, per fixture, the raw
 * DESCRIBE rows ({@code <key>.jsonl}) and the Column snapshot of the production {@code
 * describeTable} ({@code <key>.columns.txt}), plus {@code SERVER_VERSION.txt}. It prints metadata
 * only, never a row value. Exit codes: 0 success, 1 failure, 2 usage or a refused environment. It
 * lives in the test source set so it never ships in the connector jar.
 */
public final class DescribeFixtureCapture {
    static final String USAGE = "usage: DescribeFixtureCapture <output directory for the DESCRIBE fixtures>";
    static final String SERVER_PREFIX = "DESCRIBE_FIXTURE_SERVER";
    static final String FIXTURE_PREFIX = "DESCRIBE_FIXTURE";
    static final String UNPAIRED_WARNING_TEXT = "have no matching DESCRIBE sub-column";

    private static final String SCRATCH_DATABASE = "describe_fixture_capture";
    private static final String CONTAINER_PASSWORD = "test_password";
    private static final int CLICKHOUSE_HTTP_PORT = 8123;
    private static final String NO_DOUBLE_ROWS = "-";
    private static final String LINE_SEPARATOR = "\n";
    private static final String WARN_LEVEL = "WARN";
    private static final int SUCCESS_EXIT = 0;
    private static final int FAILURE_EXIT = 1;
    private static final int USAGE_EXIT = 2;

    private DescribeFixtureCapture() {}

    public static void main(String[] args) {
        System.exit(run(args));
    }

    private static int run(String[] args) {
        if (args.length != 1) {
            System.out.println(USAGE);
            return USAGE_EXIT;
        }
        if (ClickHouseTestHelpers.isCloud() || ClickHouseTestHelpers.isCluster()) {
            System.out.println("refused: the fixtures are captured against the standalone Docker image only");
            return USAGE_EXIT;
        }
        try {
            capture(Paths.get(args[0]));
            return SUCCESS_EXIT;
        } catch (Exception e) {
            System.out.println("DescribeFixtureCapture failed: " + e.getClass().getName());
            return FAILURE_EXIT;
        }
    }

    private static void capture(Path outputDirectory) throws Exception {
        String image = ClickHouseTestHelpers.CLICKHOUSE_DOCKER_IMAGE;
        ClickHouseContainer container = new ClickHouseContainer(image)
                .withPassword(CONTAINER_PASSWORD)
                .withExposedPorts(CLICKHOUSE_HTTP_PORT)
                .withEnv("CLICKHOUSE_DEFAULT_ACCESS_MANAGEMENT", "1");
        if (ClickHouseTestHelpers.CLICKHOUSE_VERSION_LATEST.equals(ClickHouseTestHelpers.getClickhouseVersion())) {
            container = container.withImagePullPolicy(PullPolicy.alwaysPull());
        }
        try (ClickHouseContainer db = container) {
            db.start();
            Map<String, String> bootstrapProps = props(db, ClickHouseTestHelpers.DATABASE_DEFAULT);
            try (ClickHouseHelperClient bootstrap = ClickHouseTestHelpers.createClient(bootstrapProps)) {
                ClickHouseTestHelpers.dropDatabase(bootstrap, SCRATCH_DATABASE);
                ClickHouseTestHelpers.createDatabase(SCRATCH_DATABASE, bootstrap);
                try (ClickHouseHelperClient chc =
                        ClickHouseTestHelpers.createClient(props(db, SCRATCH_DATABASE))) {
                    captureAll(db, image, chc, outputDirectory);
                } finally {
                    ClickHouseTestHelpers.dropDatabase(bootstrap, SCRATCH_DATABASE);
                }
            }
        }
    }

    private static Map<String, String> props(ClickHouseContainer db, String database) {
        Map<String, String> props = new HashMap<>();
        props.put(ClickHouseSinkConnector.CLIENT_VERSION, ClickHouseTestHelpers.extractClientVersion());
        props.put(ClickHouseSinkConnector.HOSTNAME, db.getHost());
        props.put(ClickHouseSinkConnector.PORT, db.getMappedPort(CLICKHOUSE_HTTP_PORT).toString());
        props.put(ClickHouseSinkConnector.DATABASE, database);
        props.put(ClickHouseSinkConnector.USERNAME, db.getUsername());
        props.put(ClickHouseSinkConnector.PASSWORD, db.getPassword());
        props.put(ClickHouseSinkConnector.SSL_ENABLED, "false");
        return props;
    }

    private static void captureAll(
            ClickHouseContainer db, String image, ClickHouseHelperClient chc, Path outputDirectory)
            throws Exception {
        Files.createDirectories(outputDirectory);
        deleteStaleFixtureFiles(outputDirectory);
        String version = chc.version();
        String imageId = db.getContainerInfo().getImageId();
        System.out.println(SERVER_PREFIX + " version=" + version + " image=" + image + " image_id=" + imageId
                + " java=" + System.getProperty("java.version"));
        for (Fixture fixture : DescribeFixtureCatalog.FIXTURES) {
            String key = fixture.key();
            try (var created = chc.queryV2(DescribeFixtureCatalog.createTableSql(
                    fixture, "`" + SCRATCH_DATABASE + "`.`" + key + "`"))) {
                // table created
            }
            List<String> rawLines = DescribeFixtureCatalog.describeRawLines(chc, key);
            List<String> snapshot;
            int warns;
            boolean unpaired;
            try (LogCapture helperLog = LogCapture.attach(ClickHouseHelperClient.class);
                    LogCapture nullabilityLog = LogCapture.attach(DeclaredNullability.class)) {
                snapshot = DescribeFixtureCatalog.renderColumns(chc.describeTable(SCRATCH_DATABASE, key));
                List<LogCapture.CapturedEvent> warnEvents = new ArrayList<>(helperLog.events(WARN_LEVEL));
                warnEvents.addAll(nullabilityLog.events(WARN_LEVEL));
                warns = warnEvents.size();
                unpaired = warnEvents.stream().anyMatch(e -> e.message().contains(UNPAIRED_WARNING_TEXT));
            }
            write(outputDirectory.resolve(key + DescribeFixtureCatalog.JSONL_SUFFIX), rawLines);
            write(outputDirectory.resolve(key + DescribeFixtureCatalog.COLUMNS_SUFFIX), snapshot);
            System.out.println(FIXTURE_PREFIX + " key=" + key + " source=" + fixture.source() + " rows="
                    + rawLines.size() + " double_rows=" + doubleRows(rawLines) + " warns=" + warns
                    + " unpaired_warn=" + (unpaired ? "yes" : "no"));
        }
        write(
                outputDirectory.resolve(DescribeFixtureCatalog.SERVER_VERSION_FILE),
                List.of("version=" + version, "image=" + image, "image_id=" + imageId));
    }

    private static String doubleRows(List<String> rawLines) throws IOException {
        Set<String> seen = new LinkedHashSet<>();
        Set<String> doubled = new LinkedHashSet<>();
        for (String line : rawLines) {
            String name = ClickHouseFieldDescriptor.fromJsonRow(line).getName();
            if (!seen.add(name)) {
                doubled.add(name);
            }
        }
        return doubled.isEmpty() ? NO_DOUBLE_ROWS : doubled.stream().collect(Collectors.joining(","));
    }

    private static void deleteStaleFixtureFiles(Path outputDirectory) throws IOException {
        Set<String> keys = DescribeFixtureCatalog.FIXTURES.stream().map(Fixture::key).collect(Collectors.toSet());
        try (DirectoryStream<Path> files = Files.newDirectoryStream(outputDirectory)) {
            for (Path file : files) {
                String name = file.getFileName().toString();
                String key = null;
                if (name.endsWith(DescribeFixtureCatalog.JSONL_SUFFIX)) {
                    key = name.substring(0, name.length() - DescribeFixtureCatalog.JSONL_SUFFIX.length());
                } else if (name.endsWith(DescribeFixtureCatalog.COLUMNS_SUFFIX)) {
                    key = name.substring(0, name.length() - DescribeFixtureCatalog.COLUMNS_SUFFIX.length());
                }
                if (key != null && !keys.contains(key)) {
                    Files.delete(file);
                }
            }
        }
    }

    private static void write(Path file, List<String> lines) throws IOException {
        Files.writeString(file, String.join(LINE_SEPARATOR, lines) + LINE_SEPARATOR, StandardCharsets.UTF_8);
    }
}
