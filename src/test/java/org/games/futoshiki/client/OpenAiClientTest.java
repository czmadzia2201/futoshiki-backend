package org.games.futoshiki.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.games.futoshiki.client.dto.*;
import org.games.futoshiki.model.Difficulty;
import org.games.futoshiki.model.FutoshikiBoard;
import org.games.futoshiki.util.TestUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.io.IOException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OpenAiClientTest {

    private MockWebServer server;

    private OpenAiClient openAiClient;
    private JsonSchemaService jsonSchemaService;
    private PromptBuilder promptBuilder;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() throws IOException {
        server = new MockWebServer();
        server.start();

        jsonSchemaService = new JsonSchemaService();
        promptBuilder = new PromptBuilder();
        objectMapper = new ObjectMapper();

        openAiClient = new OpenAiClient(
                RestClient.create(),
                jsonSchemaService,
                promptBuilder,
                objectMapper,
                "test-api-key",
                server.url("/v1/responses").toString()
        );
    }

    @Test
    void shouldSendRequestAndReturnBoard() throws Exception {
        FutoshikiBoard expectedBoard = TestUtils.loadBoardFromJson(objectMapper, "test-board-h4_001.json");
        String boardJson = objectMapper.writeValueAsString(expectedBoard);

        OpenAiResponse expectedBody = createOpenAiResponse(boardJson, "output_text");
        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .addHeader("Content-Type", "application/json")
                .setBody(objectMapper.writeValueAsString(expectedBody)));

        FutoshikiBoard responseBoard = openAiClient.sendGenerateRequest(4, Difficulty.EASY);
        assertThat(responseBoard).usingRecursiveComparison().isEqualTo(expectedBoard);

        RecordedRequest recordedRequest = server.takeRequest();

        assertThat(recordedRequest.getMethod()).isEqualTo("POST");
        assertThat(recordedRequest.getPath()).isEqualTo("/v1/responses");

        assertThat(recordedRequest.getHeader(HttpHeaders.AUTHORIZATION))
                .isEqualTo("Bearer test-api-key");

        assertThat(recordedRequest.getHeader(HttpHeaders.CONTENT_TYPE))
                .startsWith(MediaType.APPLICATION_JSON_VALUE);

        String body = recordedRequest.getBody().readUtf8();
        OpenAiRequest sentRequest = objectMapper.readValue(body, OpenAiRequest.class);

        assertThat(sentRequest.model()).isEqualTo("gpt-5.5");
        assertThat(sentRequest.input())
                .contains("Generate a FutoshikiBoard")
                .contains("- size: 4")
                .contains("- difficulty: EASY");

        Format format = sentRequest.text().format();
        assertThat(format.type()).isEqualTo("json_schema");
        assertThat(format.name()).isEqualTo("futoshiki_board");
        assertThat(format.strict()).isTrue();
        assertThat(format.schema()).isNotNull();
        assertThat(format.schema().path("additionalProperties").asBoolean()).isFalse();
    }

    @Test
    void shouldThrowWhenServerReturnsError() {
        server.enqueue(new MockResponse()
                .setResponseCode(500)
                .addHeader("Content-Type", "application/json")
                .setBody("""
                        {
                          "error": {
                            "message": "Internal server error"
                          }
                        }
                        """));

        assertThatThrownBy(() -> openAiClient.sendGenerateRequest(4, Difficulty.EASY))
                .isInstanceOfSatisfying(RestClientResponseException.class,
                        exception -> assertThat(exception.getStatusCode().value())
                                .isEqualTo(500));
    }

    @Test
    void shouldThrowWhenOpenAiResponseIsMalformed() {
        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody("not-json")
        );

        assertThatThrownBy(() -> openAiClient.sendGenerateRequest(4, Difficulty.EASY))
                .isInstanceOf(RestClientException.class);
    }

    @Test
    void shouldThrowWhenOutputTextDoesNotContainBoardJson() throws Exception {
        OpenAiResponse invalidBody = createOpenAiResponse("not-board-string", "output_text");
        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .addHeader("Content-Type", "application/json")
                .setBody(objectMapper.writeValueAsString(invalidBody)));

        assertThatThrownBy(() -> openAiClient.sendGenerateRequest(4, Difficulty.EASY))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Cannot parse OpenAI response");
    }

    @Test
    void shouldThrowWhenOutputTextIsMissing() throws Exception {
        FutoshikiBoard expectedBoard = TestUtils.loadBoardFromJson(objectMapper, "test-board-h4_001.json");
        String boardJson = objectMapper.writeValueAsString(expectedBoard);

        OpenAiResponse invalidBody = createOpenAiResponse(boardJson, "incorrect_type");
        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .addHeader("Content-Type", "application/json")
                .setBody(objectMapper.writeValueAsString(invalidBody)));

        assertThatThrownBy(() -> openAiClient.sendGenerateRequest(4, Difficulty.EASY))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("OpenAI returned no output text");
    }

    private OpenAiResponse createOpenAiResponse(String boardJson, String contentType) {
        return OpenAiResponse.builder()
                .output(List.of(
                    Output.builder()
                        .type("reasoning")
                        .content(List.of())
                        .build(),
                    Output.builder()
                        .type("message")
                        .content(List.of(Content.builder()
                            .type(contentType)
                            .text(boardJson)
                            .build()))
                    .build()))
                .build();
    }

    @AfterEach
    void tearDown() throws IOException {
        server.shutdown();
    }

}