package org.games.futoshiki.client.openai.dto;

import lombok.Builder;

@Builder
public record Content(
        String type,
        String text
) {}
