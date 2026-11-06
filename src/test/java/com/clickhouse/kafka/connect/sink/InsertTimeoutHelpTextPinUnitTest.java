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
 * it, and the claims that the setting bounds the wait for a single insert response, or that it bounds a V1
 * body write, or that the 30 s limit is a one-attempt figure ending at "the timeout setting", are gone.
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
                            "H0",
                            "Time limit in milliseconds for the phases of one ClickHouse insert listed below",
                            "review IN-01: the lead sentence is not a general bound on one insert"),
                    new Phrase(
                            "H1",
                            "a server that stops answering, as an inactivity limit that restarts on every byte received",
                            "InsertStallBoundPinUnitTest (the V2 read-inactivity bound), green in E-01 and E-14"),
                    new Phrase(
                            "H2",
                            "the sending of the batch, serialization included, which must finish within this time",
                            "E-05: write stall and slow upload end at the bound"),
                    new Phrase(
                            "H14",
                            "(again for each attempt when the client retries the insert)",
                            "E-16 clientInternalRetryOfAnInsertGetsAFreshWriteDeadline: the deadline is re-armed on a client retry"),
                    new Phrase(
                            "H13",
                            "even if the server already sends progress headers while the batch is still being sent",
                            "E-05 progressStreamedWhileTheBodyIsStillSentIsCutAtTheBound; E-10: the live 64 MiB body left the client inside the bound, so the shape rests on the stub pin"),
                    new Phrase(
                            "H3",
                            "the wait for the insert response, whether the server stays quiet or keeps sending progress headers, and nothing else",
                            "E-08 (quiet and progress shapes), E-10 v1_progress_ms_fixed; review WR-03: the V1 bound is the response wait only"),
                    new Phrase(
                            "H15",
                            "A server that stops reading the batch is cut off by that wait for batches up to 256 MiB (measured: the batch is handed to the client before the wait starts)",
                            "E-07, E-09: at 16, 64 and 256 MiB the task thread was in the response wait, never in the pipe write"),
                    new Phrase(
                            "H16",
                            "a write that blocks on the client's full 4 MiB pipe is not bounded by this setting",
                            "E-07, E-09: the pipe write stall was not reached, so no bound is claimed; review WR-03"),
                    new Phrase(
                            "H17",
                            "A server that stays quiet is also ended by the client library's own 30 s inactivity limit, so a value above 30000 ms does not let a quiet insert run longer; name socket_timeout in jdbcConnectionProperties to change that limit, which also removes this setting's bound",
                            "E-07 and E-08: the unchanged tree waited for the library's 30 s limit; review WR-02"),
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
                            "on client V1 each attempt ends at the client library's default 30 s inactivity limit, the connector makes up to retryCount (default 3) attempts at some of them, and the ping uses timeoutSeconds",
                            "E-11 v1_library_bound_ms (one attempt, helper retry 1), v1_ping_elapsed_ms; the retry loop of pingV1 and queryV1 read from the code, not measured with 3; review WR-04"),
                    new Phrase(
                            "H9",
                            "When jdbcConnectionProperties names socket_timeout, the connector adds no limit of its own to an insert on either client, except that on client V2 with async=true the wait for the response still ends at this setting",
                            "E-05 and E-08 operator cases; the Phase 41 operator pins; awaitInsertResponse applies the key to the async=true future (review IN-04)"),
                    new Phrase(
                            "H10",
                            "An insert that legitimately runs longer than a limit above fails and is retried",
                            "E-05 slow upload, E-08; review WR-02: on client V1 the smaller of this setting and the library's 30 s limit applies"),
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

    private static final List<String> RETIRED_PHRASES =
            List.of(
                    "wait for a single ClickHouse insert response",
                    "Maximum time in milliseconds the connector gives one ClickHouse insert",
                    "including a stalled body write",
                    "and ping at the timeout setting",
                    "the connector adds no limit of its own on either client");
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

    private static Stream<Arguments> retiredPhrases() {
        return RETIRED_PHRASES.stream().map(Arguments::of);
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

    @ParameterizedTest(name = "{0}")
    @MethodSource("retiredPhrases")
    @DisplayName("the help text no longer carries a claim the review found wider than its measurement")
    void helpTextNoLongerCarriesARetiredClaim(String phrase) {
        assertFalse(key().documentation.contains(phrase), "help text still carries the retired claim: " + phrase);
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
