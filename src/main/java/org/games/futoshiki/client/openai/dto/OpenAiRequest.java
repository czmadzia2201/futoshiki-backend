package org.games.futoshiki.client.openai.dto;

public record OpenAiRequest(
        String model,
        String input,
        Text text
) {}