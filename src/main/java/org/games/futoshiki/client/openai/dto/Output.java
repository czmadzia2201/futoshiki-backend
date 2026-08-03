package org.games.futoshiki.client.openai.dto;

import lombok.Builder;

import java.util.List;

@Builder
public record Output(
        String type,
        List<Content> content
) {}
