package org.games.futoshiki.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.games.futoshiki.dto.ActiveGameDto;
import org.games.futoshiki.dto.SolutionDto;
import org.games.futoshiki.dto.SolutionValidationDto;
import org.games.futoshiki.dto.FutoshikiBoardDto;
import org.games.futoshiki.model.ActiveGame;
import org.games.futoshiki.model.Difficulty;
import org.games.futoshiki.model.FutoshikiBoard;
import org.games.futoshiki.repository.ActiveGameStore;
import org.games.futoshiki.util.TestUtils;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class FutoshikiIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private ActiveGameStore activeGameStore;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void shouldCreateNewGame_defaultProviderStrategy() {
        // WHEN
        ResponseEntity<ActiveGameDto> response = restTemplate.postForEntity("/futoshiki/new-game/4/easy", null, ActiveGameDto.class);

        // THEN
        assertCorrectFlow(response, 4, Difficulty.EASY);
        assertThat(response.getBody().board().puzzleId()).isNull();

    }

    @Test
    void shouldCreateNewGame_fileProviderStrategy() {
        // WHEN
        ResponseEntity<ActiveGameDto> response = restTemplate.postForEntity("/futoshiki/new-game/4/easy?strategy=file", null, ActiveGameDto.class);

        // THEN
        assertCorrectFlow(response, 4, Difficulty.EASY);
        assertThat(response.getBody().board().puzzleId()).isNotNull();
    }

    private void assertCorrectFlow(ResponseEntity<ActiveGameDto> response, int size, Difficulty difficulty) {
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        ActiveGameDto activeGameDto = response.getBody();
        assertThat(activeGameDto).isNotNull();
        UUID gameId = activeGameDto.gameId();
        assertThat(gameId).isNotNull();

        FutoshikiBoardDto boardDto = activeGameDto.board();
        assertThat(boardDto).isNotNull();
        assertThat(boardDto.size()).isEqualTo(size);
        assertThat(boardDto.difficulty()).isEqualTo(difficulty);
        assertThat(boardDto.grid()).isNotNull();
        assertThat(boardDto.grid().length).isEqualTo(size);

        ActiveGame activeGame = activeGameStore.get(gameId);
        assertThat(activeGame).isNotNull();
    }

    @Test
    void shouldCheckSolution_successResponse() throws Exception {
        // GIVEN
        FutoshikiBoard board = TestUtils.loadBoardFromJson(objectMapper, "test-board-h4_001.json");
        ActiveGame activeGame = activeGameStore.createNewGame(board);
        SolutionDto request =
                TestUtils.loadSolutionFromJson(objectMapper,"solution-correct-h4_001.json");

        // WHEN
        ResponseEntity<SolutionValidationDto> response = restTemplate.postForEntity(
                "/futoshiki/" + activeGame.gameId() + "/check-solution", request, SolutionValidationDto.class);

        // THEN
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        SolutionValidationDto solutionValidationDto = response.getBody();
        assertThat(solutionValidationDto).isNotNull();
        assertThat(solutionValidationDto.isCorrect()).isTrue();
    }

    @Test
    void shouldNotCheckSolution_gameNotFound() throws Exception {
        // GIVEN
        SolutionDto request =
                TestUtils.loadSolutionFromJson(objectMapper,"solution-correct-h4_001.json");
        UUID gameId = UUID.randomUUID();

        // WHEN
        ResponseEntity<String> response = restTemplate.postForEntity(
                "/futoshiki/" + gameId + "/check-solution", request, String.class);

        // THEN
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        String message = response.getBody();
        assertThat(message).isEqualTo("Game not found: " + gameId);
    }

    @Test
    void shouldNotCheckSolution_invalidSolution() throws Exception {
        // GIVEN
        FutoshikiBoard board = TestUtils.loadBoardFromJson(objectMapper, "test-board-h4_001.json");
        ActiveGame activeGame = activeGameStore.createNewGame(board);
        SolutionDto request =
                TestUtils.loadSolutionFromJson(objectMapper,"solution-wrongsize-h4_001.json");

        // WHEN
        ResponseEntity<String> response = restTemplate.postForEntity(
                "/futoshiki/" + activeGame.gameId() + "/check-solution", request, String.class);

        // THEN
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        String message = response.getBody();
        assertThat(message).isEqualTo("Solution must have size: 4 x 4");
    }

    @Test
    void shouldShowSolution_successResponse() throws Exception {
        // GIVEN
        FutoshikiBoard board = TestUtils.loadBoardFromJson(objectMapper, "test-board-h4_001.json");
        ActiveGame activeGame = activeGameStore.createNewGame(board);
        SolutionDto expectedSolution =
                TestUtils.loadSolutionFromJson(objectMapper,"solution-correct-h4_001.json");

        // WHEN
        ResponseEntity<SolutionDto> response = restTemplate.getForEntity(
                "/futoshiki/" + activeGame.gameId() + "/show-solution", SolutionDto.class);

        // THEN
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        SolutionDto actualSolution = response.getBody();
        assertThat(actualSolution).isNotNull();
        assertThat(actualSolution.solution()).isDeepEqualTo(expectedSolution.solution());
    }

}
