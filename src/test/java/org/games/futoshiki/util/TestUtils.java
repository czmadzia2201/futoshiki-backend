package org.games.futoshiki.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.games.futoshiki.dto.SolutionDto;
import org.games.futoshiki.model.FutoshikiBoard;

import java.net.URL;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class TestUtils {

    public static SolutionDto loadSolutionFromJson(ObjectMapper objectMapper, String fileName) throws Exception {
        URL resource = loadFromJson(fileName);
        return objectMapper.readValue(resource, SolutionDto.class);
    }

    public static FutoshikiBoard loadBoardFromJson(ObjectMapper objectMapper, String fileName) throws Exception {
        URL resource = loadFromJson(fileName);
        return objectMapper.readValue(resource, FutoshikiBoard.class);
    }

    private static URL loadFromJson(String fileName) {
        return TestUtils.class.getClassLoader().getResource(fileName);
    }
}
