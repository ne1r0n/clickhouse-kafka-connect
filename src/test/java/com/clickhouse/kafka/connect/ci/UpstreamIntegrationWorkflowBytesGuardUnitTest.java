package com.clickhouse.kafka.connect.ci;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.Optional;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Docker-free pin for NIT-02: upstream's {@code integrationTests.yaml} stays byte-identical to the
 * copy at {@code v1.7.0} and {@code a6f1df83}, because the fork-owned nightly replaces it and it
 * stays disabled on GitHub. The file is compared as raw bytes, not lines, because a line-based read
 * would hide a changed line ending or a missing final newline.
 *
 * <p>Deliberately extends nothing - extending {@code ClickHouseBase} would inherit its {@code
 * clickhouse} tag and drop this class out of {@code testUnit}.
 */
class UpstreamIntegrationWorkflowBytesGuardUnitTest {
    private static final Path UPSTREAM_WORKFLOW =
            Path.of(".github", "workflows", "integrationTests.yaml");
    private static final String EXPECTED_SHA256 =
            "07dff3d0973ae821a259544632d161a270126ff8054b45adbf64849edd2d5109";
    // The git blob is 3e9b9be050fa05805227b2eee0d1fe8c72df56c0 at v1.7.0, a6f1df83 and upstream.
    private static final int EXPECTED_LENGTH = 4812;
    private static final String UPDATE_PROCEDURE =
            "if an upstream sync changed this file on purpose, confirm the new bytes equal the upstream"
                    + " tag's copy (git rev-parse TAG:.github/workflows/integrationTests.yaml, with the"
                    + " synced upstream tag for TAG) and replace EXPECTED_SHA256 and EXPECTED_LENGTH in"
                    + " the same commit";

    private static final String FIRST_BYTE_FLIPPED = "the first byte flipped";
    private static final byte FLIP_MASK = 0x01;

    static String sha256Hex(byte[] bytes) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("every JDK provides SHA-256", e);
        }
    }

    static Optional<String> byteIdentityProblem(byte[] actual) {
        String found = sha256Hex(actual);
        if (actual.length == EXPECTED_LENGTH && found.equals(EXPECTED_SHA256)) {
            return Optional.empty();
        }
        return Optional.of(
                "integrationTests.yaml must stay byte-identical to upstream: expected "
                        + EXPECTED_LENGTH
                        + " bytes / "
                        + EXPECTED_SHA256
                        + ", found "
                        + actual.length
                        + " bytes / "
                        + found
                        + "; "
                        + UPDATE_PROCEDURE);
    }

    private static byte[] flipped(byte[] bytes, int index) {
        byte[] copy = bytes.clone();
        copy[index] ^= FLIP_MASK;
        return copy;
    }

    private static byte[] withCrlf(byte[] bytes) {
        return new String(bytes, StandardCharsets.ISO_8859_1)
                .replace("\n", "\r\n")
                .getBytes(StandardCharsets.ISO_8859_1);
    }

    private static byte[] firstByteFlipped() throws IOException {
        return flipped(Files.readAllBytes(UPSTREAM_WORKFLOW), 0);
    }

    static Stream<Arguments> editedCopies() throws IOException {
        byte[] real = Files.readAllBytes(UPSTREAM_WORKFLOW);
        byte[] appended = Arrays.copyOf(real, real.length + 1);
        appended[real.length] = ' ';
        return Stream.of(
                Arguments.of(FIRST_BYTE_FLIPPED, flipped(real, 0)),
                Arguments.of("a middle byte flipped", flipped(real, real.length / 2)),
                Arguments.of("the last byte flipped", flipped(real, real.length - 1)),
                Arguments.of("one byte appended", appended),
                Arguments.of("the final newline removed", Arrays.copyOf(real, real.length - 1)),
                Arguments.of("LF line endings replaced by CRLF", withCrlf(real)),
                Arguments.of("an empty file", new byte[0]));
    }

    @Test
    @DisplayName("integrationTests.yaml is byte-identical to upstream at v1.7.0 and a6f1df83 (NIT-02)")
    void integrationTestsWorkflowIsByteIdenticalToUpstream() throws IOException {
        byteIdentityProblem(Files.readAllBytes(UPSTREAM_WORKFLOW))
                .ifPresent(problem -> fail(problem));
    }

    @ParameterizedTest(name = "fixture: the byte rule rejects {0} (NIT-02)")
    @MethodSource("editedCopies")
    void byteRuleRejectsEveryEditedCopy(String label, byte[] edited) {
        assertTrue(byteIdentityProblem(edited).isPresent(), () -> "must reject " + label);
    }

    @Test
    @DisplayName(
            "fixture: a byte-rule problem names the expected and the found length and SHA-256 and the"
                    + " update procedure (NIT-02)")
    void problemNamesExpectedAndFoundValuesAndUpdateProcedure() throws IOException {
        byte[] edited = firstByteFlipped();
        String message = byteIdentityProblem(edited).orElse("");
        assertTrue(message.contains(String.valueOf(EXPECTED_LENGTH)), message);
        assertTrue(message.contains(EXPECTED_SHA256), message);
        assertTrue(message.contains(sha256Hex(edited)), message);
        assertTrue(message.contains("replace EXPECTED_SHA256"), message);
    }
}
