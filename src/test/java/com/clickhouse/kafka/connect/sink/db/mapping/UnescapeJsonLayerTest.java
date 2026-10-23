package com.clickhouse.kafka.connect.sink.db.mapping;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Docker-free pins for {@link DeclaredNullability#unescapeJsonLayer}, the decoder of the JSON layer
 * of a declared type text. Deliberately extends nothing, so the class stays in {@code testUnit}.
 */
class UnescapeJsonLayerTest {

    @ParameterizedTest(name = "[{index}] {0} decodes to {1}")
    @CsvSource(delimiter = '|', value = {
        "\\u0041|A",
        "\\u00e9|\u00e9",
        "\\u00E9|\u00e9",
        "\\\\|\\",
        "\\/|/",
        "\\\"|\"",
        "x\\ty|x\ty",
        "\\ud83d\\ude00|\ud83d\ude00"
    })
    void decodesTheJsonEscapes(String text, String expected) {
        assertEquals(expected, DeclaredNullability.unescapeJsonLayer(text));
    }

    @Test
    void aUnicodeEscapeAndTheCharacterItNamesDecodeToTheSameText() {
        assertEquals(
                DeclaredNullability.unescapeJsonLayer("\u00e9"), DeclaredNullability.unescapeJsonLayer("\\u00e9"));
    }

    @ParameterizedTest(name = "[{index}] {0} is copied as it is")
    @ValueSource(strings = {
        "\\u00zz", // non-hex digits
        "\\u004", // fewer than four digits at the end of the text
        "\\u", // no digits at all
        "\\u00\u0663\u0663", // Arabic-Indic digits are digits for Java but not hex digits for JSON
        "\\q", // not a JSON escape
        "abc\\" // a lone trailing backslash
    })
    void copiesAnUnknownOrTruncatedEscapeAsItIs(String text) {
        assertEquals(text, DeclaredNullability.unescapeJsonLayer(text));
    }

    @Test
    void aTextWithoutABackslashIsReturnedAsItIs() {
        String text = "Enum8('a' = 1)";
        assertEquals(text, DeclaredNullability.unescapeJsonLayer(text));
    }
}
