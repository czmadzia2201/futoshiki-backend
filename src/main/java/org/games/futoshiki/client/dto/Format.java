package org.games.futoshiki.client.dto;

import com.fasterxml.jackson.databind.JsonNode;

public record Format(String type, String name, boolean strict, JsonNode schema) {

    public Format(JsonNode schema) {
        this("json_schema", "futoshiki_board", true, schema);
    }
}
