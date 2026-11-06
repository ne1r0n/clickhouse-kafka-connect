package com.clickhouse.kafka.connect.ci;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Every wildcard row of every shipped confluent-archive allow-list names at least two literal path
 * segments before its first wildcard, so that a row such as {@code com/*} or {@code META-INF/*}
 * cannot silence a whole namespace of archive differences.
 *
 * <p>This is a test and not a rule of {@code compare_confluent_archives.py}: the script refuses only
 * a glob that starts with a wildcard, and a stricter loader would change what the published release
 * check accepts. The test reads every list that matches the prefix, so the list of the next release
 * joins the rule without a test edit.
 */
class AllowListWildcardRuleUnitTest {

    private static final Path ALLOW_LISTS = Path.of(".planning", "scripts");
    private static final String ALLOW_LIST_PREFIX = "confluent-archive-allowlist-";
    private static final String COMMENT = "#";
    private static final String TAB = "\t";
    private static final String WILDCARDS = "*?[";
    private static final char SEPARATOR = '/';
    private static final int MIN_LITERAL_SEGMENTS = 2;
    private static final int MIN_SHIPPED_LISTS = 5;
    private static final String NAME = "{displayName}[{index}] glob={0}";
    private static final String LISTS_FOUND = "the shipped allow-lists were not found";
    private static final String WILDCARD_ROWS_FOUND = "no shipped allow-list holds a wildcard row, so the rule checked nothing";
    private static final String OFFENDING_ROWS =
            "wildcard rows with fewer than two literal path segments before the first wildcard";
    private static final String REFUSED = "a wildcard after fewer than two literal path segments must be refused";
    private static final String ACCEPTED = "a row with two literal path segments, or no wildcard, must be accepted";

    private static int firstWildcard(String glob) {
        int first = -1;
        for (char wildcard : WILDCARDS.toCharArray()) {
            int at = glob.indexOf(wildcard);
            if (at >= 0 && (first < 0 || at < first)) {
                first = at;
            }
        }
        return first;
    }

    static boolean followsTwoLiteralSegments(String glob) {
        int first = firstWildcard(glob);
        if (first < 0) {
            return true;
        }
        return glob.substring(0, first).chars().filter(c -> c == SEPARATOR).count() >= MIN_LITERAL_SEGMENTS;
    }

    private static List<Path> shippedLists() throws IOException {
        try (Stream<Path> files = Files.list(ALLOW_LISTS)) {
            return files.filter(f -> f.getFileName().toString().startsWith(ALLOW_LIST_PREFIX))
                    .sorted()
                    .toList();
        }
    }

    @Test
    void everyShippedWildcardRowFollowsTwoLiteralSegments() throws IOException {
        List<Path> lists = shippedLists();
        assertTrue(lists.size() >= MIN_SHIPPED_LISTS, () -> LISTS_FOUND + ": " + lists);
        int wildcardRows = 0;
        List<String> offenders = new ArrayList<>();
        for (Path list : lists) {
            List<String> lines = Files.readAllLines(list, StandardCharsets.UTF_8);
            for (int i = 0; i < lines.size(); i++) {
                String line = lines.get(i);
                if (line.isBlank() || line.startsWith(COMMENT)) {
                    continue;
                }
                String glob = line.split(TAB, 2)[0].strip();
                if (firstWildcard(glob) >= 0) {
                    wildcardRows++;
                }
                if (!followsTwoLiteralSegments(glob)) {
                    offenders.add(list.getFileName() + ":" + (i + 1) + ": " + glob);
                }
            }
        }
        assertTrue(wildcardRows > 0, WILDCARD_ROWS_FOUND);
        assertTrue(offenders.isEmpty(), () -> OFFENDING_ROWS + ": " + offenders);
    }

    @ParameterizedTest(name = NAME)
    @ValueSource(strings = {"com/*", "outer/*", "META-INF/*", "c*", "com/clickhouse*"})
    void aWildcardAfterFewerThanTwoLiteralSegmentsIsRefused(String glob) {
        assertFalse(followsTwoLiteralSegments(glob), REFUSED);
    }

    @ParameterizedTest(name = NAME)
    @ValueSource(strings = {"com/google/gson/*", "META-INF/versions/*/x/*", "a/A.class"})
    void aRowWithTwoLiteralSegmentsOrNoWildcardIsAccepted(String glob) {
        assertTrue(followsTwoLiteralSegments(glob), ACCEPTED);
    }
}
