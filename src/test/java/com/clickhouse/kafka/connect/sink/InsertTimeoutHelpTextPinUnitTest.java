package com.clickhouse.kafka.connect.sink;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.stream.Stream;
import org.apache.kafka.common.config.ConfigDef;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Docker-free pin for the help text of {@code clickhouseClientInsertTimeoutMs}: the text states
 * what the setting bounds and what it does not, each phrase tied to the measurement that supports
 * it, and the old claim that the setting bounds the wait for a single insert response is gone.
 *
 * <p>The text is read from the registered {@link ConfigDef.ConfigKey}, so a sync that restores the
 * old wording, or a rewrite that drops a measured limit, fails {@code testUnit}. The other
 * attributes of the key are pinned unchanged so that a help-text edit cannot carry a behaviour
 * change.
 *
 * <p>Extends nothing so it stays in {@code testUnit}: extending {@code ClickHouseBase} would
 * inherit its {@code @Tag("clickhouse")} and drop this class out of the Docker-free loop.
 */
class InsertTimeoutHelpTextPinUnitTest {

    /** A phrase the text must contain, with the evidence entry that measured what it states. */
    private record Phrase(String id, String text, String evidence) {}

    private static final List<Phrase> REQUIRED_PHRASES =
            List.of(
                    new Phrase(
                            "H1",
                            "a server that stops answering, as an inactivity limit that restarts on every byte received",
                            "InsertStallBoundPinUnitTest (the V2 read-inactivity bound), green in E-01 and E-14"),
                    new Phrase(
                            "H2",
                            "the sending of the batch, serialization included, which must finish within this time",
                            "E-05: write stall and slow upload end at the bound"),
                    new Phrase(
                            "H13",
                            "even if the server already sends progress headers while the batch is still being sent",
                            "E-05 progressStreamedWhileTheBodyIsStillSentIsCutAtTheBound; E-10: the live 64 MiB body left the client inside the bound, so the shape rests on the stub pin"),
                    new Phrase(
                            "H3",
                            "the whole wait for the insert response, including a stalled body write (measured with batches up to 256 MiB), a quiet server and a server that keeps sending progress headers",
                            "E-07, E-08, E-09 (a stall inside the client's pipe write was not reached), E-10 v1_progress_ms_fixed"),
                    new Phrase(
                            "H4",
                            "What it does not bound",
                            "E-05 progress cases, E-10, E-11"),
                    new Phrase(
                            "H5",
                            "On client V2, an insert whose batch has been sent and whose server keeps sending progress headers runs to its natural end",
                            "E-05 progressStreamingV2InsertRunsPastTheBoundOnceTheBodyIsSent; E-10 v2_progress_escape_ms_fixed"),
                    new Phrase(
                            "H6",
                            "Requests that are not inserts (DESCRIBE, table refresh, version query, ping, ALTER, Keeper state reads and writes) have no connector limit",
                            "E-11"),
                    new Phrase(
                            "H7",
                            "except the Keeper state read, which always uses client V1",
                            "E-11 v2_keeper_read_elapsed_ms"),
                    new Phrase(
                            "H8",
                            "on client V1 they end at the client library's default 30 s inactivity limit, and ping at the timeout setting",
                            "E-11 v1_library_bound_ms, v1_ping_elapsed_ms"),
                    new Phrase(
                            "H9",
                            "When jdbcConnectionProperties names socket_timeout, the connector adds no limit of its own on either client",
                            "E-05 and E-08 operator cases; the Phase 41 operator pins"),
                    new Phrase(
                            "H10",
                            "An insert that legitimately runs longer than this limit fails and is retried",
                            "E-05 slow upload, E-08"),
                    new Phrase(
                            "H11",
                            "a timed-out insert may still be committed by the server, in part or in full",
                            "E-10 rows after the timed-out inserts"));

    private static final List<String> KEPT_PHRASES =
            List.of(
                    "RetriableException",
                    "insert_deduplication_token",
                    "consumer.override.max.poll.interval.ms",
                    "Default: 240000 ms.");

    private static final String RETIRED_PHRASE = "wait for a single ClickHouse insert response";
    private static final String DISPLAY_NAME = "ClickHouse client insert timeout in ms.";
    private static final long DEFAULT_MS = 240000L;

    private static ConfigDef.ConfigKey key() {
        ConfigDef.ConfigKey key =
                ClickHouseSinkConfig.CONFIG
                        .configKeys()
                        .get(ClickHouseSinkConfig.CLICKHOUSE_CLIENT_INSERT_TIMEOUT_MS);
        assertNotNull(key, "the insert timeout key must be registered");
        return key;
    }

    private static Stream<Arguments> requiredPhrases() {
        return REQUIRED_PHRASES.stream().map(p -> Arguments.of(p.id(), p.text(), p.evidence()));
    }

    private static Stream<Arguments> keptPhrases() {
        return KEPT_PHRASES.stream().map(Arguments::of);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("requiredPhrases")
    @DisplayName("the help text states the measured behaviour")
    void helpTextStatesTheMeasuredBehaviour(String id, String phrase, String evidence) {
        assertTrue(
                key().documentation.contains(phrase),
                "help text lacks " + id + " (measured by " + evidence + "): " + phrase);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("keptPhrases")
    @DisplayName("the help text keeps the facts it already stated")
    void helpTextKeepsTheExistingFacts(String phrase) {
        assertTrue(key().documentation.contains(phrase), "help text lacks: " + phrase);
    }

    @Test
    @DisplayName("the help text no longer claims the setting bounds the wait for one insert response")
    void helpTextNoLongerClaimsAResponseWaitBound() {
        assertFalse(
                key().documentation.contains(RETIRED_PHRASE),
                "help text still carries the retired claim: " + RETIRED_PHRASE);
    }

    @Test
    @DisplayName("only the documentation of the key changed: type, default, validator, importance, display name and width")
    void onlyTheDocumentationOfTheKeyChanged() {
        ConfigDef.ConfigKey key = key();

        assertEquals(ConfigDef.Type.LONG, key.type);
        assertEquals(DEFAULT_MS, key.defaultValue);
        assertEquals(ConfigDef.Range.atLeast(100).toString(), String.valueOf(key.validator));
        assertEquals(ConfigDef.Importance.LOW, key.importance);
        assertEquals(DISPLAY_NAME, key.displayName);
        assertEquals(ConfigDef.Width.SHORT, key.width);
    }
}
