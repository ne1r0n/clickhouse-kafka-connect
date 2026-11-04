package com.clickhouse.kafka.connect.sink.db.helper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.io.IOException;
import java.util.function.Function;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Docker-free pin of the one place where the fork's {@code fromJsonRow} differs from upstream's
 * (upstream #839): the fork decodes only {@code name} and keeps every other string as the server's
 * literal JSON text, while upstream's {@code readValue} decodes every string field (FORK-DELTA,
 * raw-identifier row). Deliberately extends nothing, so it runs in {@code testUnit}.
 */
class FieldDescriptorLiteralFieldsUnitTest {

    private static final String TYPE_FIELD = "type";
    private static final String PLAIN_TYPE = "String";

    private enum Field {
        TYPE(TYPE_FIELD, ClickHouseFieldDescriptor::getType),
        COMMENT("comment", ClickHouseFieldDescriptor::getComment),
        DEFAULT_EXPRESSION("default_expression", ClickHouseFieldDescriptor::getDefaultExpression),
        CODEC_EXPRESSION("codec_expression", ClickHouseFieldDescriptor::getCodecExpression),
        TTL_EXPRESSION("ttl_expression", ClickHouseFieldDescriptor::getTtlExpression);

        private final String key;
        private final Function<ClickHouseFieldDescriptor, String> getter;

        Field(String key, Function<ClickHouseFieldDescriptor, String> getter) {
            this.key = key;
            this.getter = getter;
        }

        @Override
        public String toString() {
            return key;
        }
    }

    /** A JSON string body as the server writes it, and the text a JSON decoder would make of it. */
    private record Sample(String jsonText, String decoded) {
        @Override
        public String toString() {
            return jsonText;
        }
    }

    private static Stream<Sample> samples() {
        return Stream.of(
                new Sample("a\\/b", "a/b"),
                new Sample("a\\\\b", "a\\b"),
                new Sample("a\\\"b", "a\"b"),
                new Sample("a\\nb", "a\nb"),
                new Sample("a\\tb", "a\tb"),
                new Sample("café\\/温度", "café/温度"));
    }

    private static Stream<Arguments> fieldsBySample() {
        return Stream.of(Field.values())
                .flatMap(field -> samples().map(sample -> Arguments.of(field, sample)));
    }

    private static String row(Field field, Sample sample) {
        String type = field == Field.TYPE ? sample.jsonText() : PLAIN_TYPE;
        String extra = field == Field.TYPE ? "" : ",\"" + field.key + "\":\"" + sample.jsonText() + "\"";
        return "{\"name\":\"" + sample.jsonText() + "\",\"" + TYPE_FIELD + "\":\"" + type + "\"" + extra + "}";
    }

    @ParameterizedTest(name = "{0} keeps {1} literal")
    @MethodSource("fieldsBySample")
    void everyNonNameFieldKeepsTheServersJsonText(Field field, Sample sample) throws IOException {
        ClickHouseFieldDescriptor descriptor = ClickHouseFieldDescriptor.fromJsonRow(row(field, sample));

        assertEquals(sample.decoded(), descriptor.getName(), "the name is decoded to the identifier");
        assertEquals(
                sample.jsonText(),
                field.getter.apply(descriptor),
                field + " keeps the JSON escapes as text; upstream's readValue would decode it");
    }

    @Test
    void jsonNullCommentAndDefaultExpressionStayNull() throws IOException {
        ClickHouseFieldDescriptor descriptor = ClickHouseFieldDescriptor.fromJsonRow(
                "{\"name\":\"a\",\"type\":\"String\",\"comment\":null,\"default_expression\":null}");

        assertNull(descriptor.getComment());
        assertNull(descriptor.getDefaultExpression());
    }
}
