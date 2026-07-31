package org.games.futoshiki.client.dto;

import lombok.Builder;

import java.util.List;

@Builder
public record OpenAiResponse(
        List<Output> output
) {

    private static final String OUTPUT_TYPE = "message";
    private static final String CONTENT_TYPE = "output_text";

    public String extractJsonContent() {
        return output.stream()
                .filter(output -> OUTPUT_TYPE.equals(output.type()))
                .flatMap(output -> output.content().stream())
                .filter(content -> CONTENT_TYPE.equals(content.type()))
                .map(Content::text)
                .findFirst()
                .orElseThrow(() ->
                        new IllegalStateException("OpenAI returned no output text"));
    }

}
