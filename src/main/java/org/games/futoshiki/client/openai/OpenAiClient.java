package org.games.futoshiki.client.openai;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.games.futoshiki.client.openai.dto.*;
import org.games.futoshiki.model.Difficulty;
import org.games.futoshiki.model.FutoshikiBoard;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class OpenAiClient {

    private final RestClient restClient;
    private final JsonSchemaService jsonSchemaService;
    private final PromptBuilder promptBuilder;
    private final ObjectMapper objectMapper;
    private final String apiKey;
    private final String openAiUri;

    public OpenAiClient(
            RestClient restClient,
            JsonSchemaService jsonSchemaService,
            PromptBuilder promptBuilder,
            ObjectMapper objectMapper,
            @Value("${openai.api-key}") String apiKey,
            @Value("${openai.uri}") String openAiUri
    ) {
        this.restClient = restClient;
        this.jsonSchemaService = jsonSchemaService;
        this.promptBuilder = promptBuilder;
        this.objectMapper = objectMapper;
        this.apiKey = apiKey;
        this.openAiUri = openAiUri;
    }

    public FutoshikiBoard sendBoardRequest(int size, Difficulty difficulty) {

        JsonNode futoshikiBoard = jsonSchemaService.generate(FutoshikiBoard.class);
        Text text = new Text(new Format(futoshikiBoard));

        OpenAiRequest request = new OpenAiRequest(
                "gpt-5.5",
                promptBuilder.futoshikiPrompt(size, difficulty),
                text
        );

        OpenAiResponse response = restClient.post()
                .uri(openAiUri)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .body(OpenAiResponse.class);

        try {
            return objectMapper.readValue(response.extractJsonContent(), FutoshikiBoard.class);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Cannot parse OpenAI response", e);
        }
    }
}