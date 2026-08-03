package org.games.futoshiki.client.fcapi;

import okhttp3.HttpUrl;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.games.futoshiki.model.Difficulty;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FcApiClientTest {

    private MockWebServer server;

    private FcApiClient fcApiClient;

    private static final String FCAPI_RAW_BOARD = "2_._._._______._.(._.______v3_._._.v______._._.(32_3_1_4_______4_1(3_2______v3_2_4_1v______1_4_2(3";

    @BeforeEach
    void setUp() throws IOException {
        server = new MockWebServer();
        server.start();

        fcApiClient = new FcApiClient(
                RestClient.create(),
                server.url("/get.cgi").toString()
        );
    }

    @ParameterizedTest
    @CsvSource({ "EASY,1", "MEDIUM,2", "HARD,3" })
    void shouldSendRequestAndReturnRawString(Difficulty difficulty, int expectedValue) throws Exception {
        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .addHeader("Content-Type", "text/xml")
                .setBody(expectedBody(FCAPI_RAW_BOARD)));

        String rawString = fcApiClient.sendBoardRequest(4, difficulty, 1);

        assertThat(rawString).isEqualTo(FCAPI_RAW_BOARD);

        RecordedRequest request = server.takeRequest();
        assertThat(request.getMethod()).isEqualTo("GET");

        HttpUrl url = request.getRequestUrl();

        assertThat(url.encodedPath()).isEqualTo("/get.cgi");
        assertThat(url.queryParameter("size")).isEqualTo("4");
        assertThat(url.queryParameter("difficulty")).isEqualTo(String.valueOf(expectedValue));
        assertThat(url.queryParameter("id")).isEqualTo("1");
    }

    @Test
    void shouldThrowWhenServerReturnsError() {
        server.enqueue(new MockResponse()
                .setResponseCode(500));

        assertThatThrownBy(() -> fcApiClient.sendBoardRequest(4, Difficulty.EASY, 1))
                .isInstanceOfSatisfying(RestClientResponseException.class,
                        exception -> assertThat(exception.getStatusCode().value())
                                .isEqualTo(500));
    }

    @Test
    void shouldThrowWhenResponseIsMalformed() {
        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .addHeader("Content-Type", "text/xml")
                .setBody("malformed-body"));

        assertThatThrownBy(() -> fcApiClient.sendBoardRequest(5, Difficulty.HARD, 1))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Cannot parse Futoshiki.com response");
    }

    @AfterEach
    void tearDown() throws IOException {
        server.shutdown();
    }

    private String expectedBody(String rawString) {
        return """
               <?xml version="1.0"?>
               <game>%s</game>
               """.formatted(rawString);
    }

}