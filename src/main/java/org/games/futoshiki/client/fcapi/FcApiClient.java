package org.games.futoshiki.client.fcapi;

import org.games.futoshiki.model.Difficulty;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

@Component
public class FcApiClient {

    private final RestClient restClient;
    private final String fcApiUri;

    public FcApiClient(
            RestClient restClient,
            @Value("${fcapi.uri}") String fcApiUri
    ) {
        this.restClient = restClient;
        this.fcApiUri = fcApiUri;
    }

    public String sendBoardRequest(int size, Difficulty difficulty, int id) {
        String xml = restClient.get()
                .uri(UriComponentsBuilder
                        .fromUriString(fcApiUri)
                        .queryParam("size", size)
                        .queryParam("difficulty", mapDifficulty(difficulty))
                        .queryParam("id", id)
                        .build()
                        .toUri())
                .accept(MediaType.TEXT_XML)
                .retrieve()
                .body(String.class);

        return extractGame(xml);
    }

    private String extractGame(String xml) {
        if (xml == null) {
            throw new IllegalStateException("Cannot parse Futoshiki.com response");
        }

        int start = xml.indexOf("<game>");
        int end = xml.indexOf("</game>");

        if (start < 0 || end < 0 || end < start) {
            throw new IllegalStateException("Cannot parse Futoshiki.com response");
        }
        return xml.substring(start + "<game>".length(), end);
    }

    private int mapDifficulty(Difficulty difficulty) {
        return switch (difficulty) {
            case EASY -> 1;
            case MEDIUM -> 2;
            case HARD -> 3;
        };
    }

}
