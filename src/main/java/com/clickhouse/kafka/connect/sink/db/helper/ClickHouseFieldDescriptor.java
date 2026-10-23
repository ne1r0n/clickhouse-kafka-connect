package com.clickhouse.kafka.connect.sink.db.helper;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.IOException;
import lombok.Builder;
import lombok.Data;
import lombok.extern.jackson.Jacksonized;

/**
 * Java object representation of one DESCRIBE TABLE result row.
 * <p>
 * We use Jackson to instantiate it from JSON.
 */
@Data
@Builder
@Jacksonized
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class ClickHouseFieldDescriptor {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final String NAME_FIELD = "name";
    private static final String ROW_READ_FAILURE_MESSAGE = "DESCRIBE row could not be read";
    private String name;
    private String type;
    private String defaultType;
    private String defaultExpression;
    private String comment;
    private String codecExpression;
    private String ttlExpression;
    private boolean isSubcolumn;

    public boolean isAlias() {
        return "ALIAS".equals(defaultType);
    }

    public boolean isMaterialized() {
        return "MATERIALIZED".equals(defaultType);
    }

    public boolean isEphemeral() {
        return "EPHEMERAL".equals(defaultType);
    }

    public boolean hasDefault() {
        return "DEFAULT".equals(defaultType);
    }

    /**
     * Reads one JSON row of {@code DESCRIBE TABLE}. The name is decoded to the identifier as written in
     * {@code CREATE TABLE}. Every other string keeps the server's JSON escapes as text, because the
     * type text's identifiers and labels are parsed later with that layer still in place.
     *
     * @throws JsonProcessingException when the line is not a well-formed row; the message never
     *     embeds the row
     */
    public static ClickHouseFieldDescriptor fromJsonRow(String json) throws JsonProcessingException {
        try (JsonParser parser = OBJECT_MAPPER.createParser(json)) {
            if (parser.nextToken() != JsonToken.START_OBJECT) {
                return OBJECT_MAPPER.readValue(json, ClickHouseFieldDescriptor.class);
            }
            ObjectNode row = OBJECT_MAPPER.createObjectNode();
            while (parser.nextToken() == JsonToken.FIELD_NAME) {
                String field = parser.currentName();
                if (parser.nextToken() != JsonToken.VALUE_STRING) {
                    row.set(field, parser.readValueAsTree());
                } else if (NAME_FIELD.equals(field)) {
                    row.put(field, parser.getText());
                } else {
                    int start = (int) parser.currentTokenLocation().getCharOffset();
                    parser.getText();
                    int end = (int) parser.currentLocation().getCharOffset();
                    row.put(field, json.substring(start + 1, end - 1));
                }
            }
            return OBJECT_MAPPER.treeToValue(row, ClickHouseFieldDescriptor.class);
        } catch (JsonProcessingException e) {
            throw e;
        } catch (IOException e) {
            throw JsonMappingException.from((JsonParser) null, ROW_READ_FAILURE_MESSAGE, e);
        }
    }
}
