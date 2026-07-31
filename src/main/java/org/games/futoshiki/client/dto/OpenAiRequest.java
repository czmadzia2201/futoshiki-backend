package org.games.futoshiki.client.dto;

public record OpenAiRequest(
        String model,
        String input,
        Text text
) {}