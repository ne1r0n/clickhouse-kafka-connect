package com.clickhouse.kafka.connect.ci;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

/**
 * The one implementation of the CI-20 archive rules (D-07, widened to kafka-clients per D-12). The
 * Confluent archive must hold exactly one {@code lib/*.jar}, and that jar must be a flat jar of the
 * connector and the ClickHouse client: no jar nested inside it (Shadow 9 produced exactly that from
 * an unchanged build line) and no {@code org/apache/kafka/} class, because the worker supplies the
 * Kafka API at runtime. It is run by {@code verifyConfluentArchive} in {@code build.gradle.kts} as
 * the finalizer of {@code createConfluentArchive} and exercised on synthetic archives by {@code
 * ConfluentArchiveCheckUnitTest}. It lives in the test source set so it never ships in the connector
 * jar or the archive, and uses the JDK only.
 */
public final class ConfluentArchiveCheck {
    static final List<String> REQUIRED_CLASSES =
            List.of(
                    "com/clickhouse/kafka/connect/ClickHouseSinkConnector.class",
                    "com/clickhouse/client/api/Client.class");
    static final int MAX_LISTED = 20;
    static final String NESTED_JAR_PREFIX = "nested jar: ";
    static final String KAFKA_CLASS_PREFIX = "Kafka API class: ";
    static final String MISSING_CLASS_PREFIX = "missing class: ";
    static final String SHAPE_PREFIX = "expected exactly one jar under lib/ in the archive, found ";
    static final String EMPTY_PREFIX = "no inner jar entries read from ";
    static final String FAILURE_HEADER = "Confluent archive check failed";
    static final String SUCCESS_SUFFIX = " inner entries, no nested jar, no Kafka API class";
    static final String SUCCESS_PREFIX = "verifyConfluentArchive: ";
    static final String USAGE = "usage: ConfluentArchiveCheck <path to the Confluent archive zip>";

    private static final String LIB_DIRECTORY = "/lib/";
    private static final String JAR_SUFFIX = ".jar";
    private static final String KAFKA_PACKAGE = "org/apache/kafka/";
    private static final int USAGE_EXIT = 2;
    private static final int VIOLATION_EXIT = 1;

    /** The number of inner entries read (directories included) and every violation found. */
    record Result(int innerEntries, List<String> violations) {}

    private ConfluentArchiveCheck() {}

    static Result check(Path archive) throws IOException {
        List<String> innerViolations = new ArrayList<>();
        Set<String> seenClasses = new HashSet<>();
        int libJars = 0;
        int innerEntries = 0;
        try (ZipFile outer = new ZipFile(archive.toFile())) {
            Enumeration<? extends ZipEntry> entries = outer.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                if (!entry.getName().contains(LIB_DIRECTORY) || !entry.getName().endsWith(JAR_SUFFIX)) {
                    continue;
                }
                libJars++;
                try (ZipInputStream inner = new ZipInputStream(outer.getInputStream(entry))) {
                    ZipEntry innerEntry;
                    while ((innerEntry = inner.getNextEntry()) != null) {
                        innerEntries++;
                        String name = innerEntry.getName();
                        seenClasses.add(name);
                        if (name.endsWith(JAR_SUFFIX)) {
                            innerViolations.add(NESTED_JAR_PREFIX + name);
                        }
                        if (name.startsWith(KAFKA_PACKAGE)) {
                            innerViolations.add(KAFKA_CLASS_PREFIX + name);
                        }
                    }
                }
            }
        }
        List<String> violations = new ArrayList<>();
        if (libJars != 1) {
            violations.add(SHAPE_PREFIX + libJars);
        }
        violations.addAll(innerViolations);
        if (innerEntries == 0) {
            violations.add(EMPTY_PREFIX + archive);
        }
        for (String required : REQUIRED_CLASSES) {
            if (!seenClasses.contains(required)) {
                violations.add(MISSING_CLASS_PREFIX + required);
            }
        }
        return new Result(innerEntries, violations);
    }

    static int run(String[] args, PrintStream out, PrintStream err) throws IOException {
        if (args.length != 1) {
            err.println(USAGE);
            return USAGE_EXIT;
        }
        Result result = check(Path.of(args[0]));
        if (result.violations().isEmpty()) {
            out.println(SUCCESS_PREFIX + result.innerEntries() + SUCCESS_SUFFIX);
            return 0;
        }
        err.println(
                FAILURE_HEADER
                        + " ("
                        + result.violations().size()
                        + " violations of "
                        + result.innerEntries()
                        + " entries):");
        result.violations().stream().limit(MAX_LISTED).forEach(err::println);
        return VIOLATION_EXIT;
    }

    public static void main(String[] args) throws IOException {
        System.exit(run(args, System.out, System.err));
    }
}
