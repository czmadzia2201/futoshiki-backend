package org.games.futoshiki.client.dto;

import lombok.Builder;

@Builder
public record Content(
        String type,
        String text
) {}
