package org.games.futoshiki.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.games.futoshiki.dto.ActiveGameDto;
import org.games.futoshiki.dto.SolutionDto;
import org.games.futoshiki.dto.FutoshikiBoardDto;
import org.games.futoshiki.exception.InvalidSolutionException;
import org.games.futoshiki.model.ActiveGame;
import org.games.futoshiki.model.Difficulty;
import org.games.futoshiki.model.FutoshikiBoard;
import org.games.futoshiki.provider.ProviderStrategy;
import org.games.futoshiki.repository.ActiveGameStore;
import org.games.futoshiki.util.TestUtils;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class FutoshikiServiceTest {

    @Autowired
    private ActiveGameStore activeGameStore;

    @Autowired
    private FutoshikiService futoshikiService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void shouldCreateNewGame_generatorStrategy() {
        Instant before = Instant.now();

        ActiveGameDto activeGameDto = futoshikiService.newGame(5, Difficulty.MEDIUM, ProviderStrategy.GENERATOR);
        assertThat(activeGameDto).isNotNull();

        UUID gameId = activeGameDto.gameId();
        assertThat(gameId).isNotNull();

        FutoshikiBoardDto boardDto = activeGameDto.board();
        assertBoardDto(boardDto, 5, Difficulty.MEDIUM);
        assertThat(boardDto.puzzleId()).isNull();

        ActiveGame activeGame = activeGameStore.get(gameId);
        assertThat(activeGame.createdAt()).isBetween(before, Instant.now());

        FutoshikiBoard board = activeGame.board();
        assertBoard(board, 5, Difficulty.MEDIUM);
        assertThat(board.puzzleId()).isNull();
    }

    @Test
    void shouldCreateNewGame_fileStrategy() {
        Instant before = Instant.now();

        ActiveGameDto activeGameDto = futoshikiService.newGame(6, Difficulty.EASY, ProviderStrategy.FILE);
        assertThat(activeGameDto).isNotNull();

        UUID gameId = activeGameDto.gameId();
        assertThat(gameId).isNotNull();

        FutoshikiBoardDto boardDto = activeGameDto.board();
        assertBoardDto(boardDto, 6, Difficulty.EASY);
        assertThat(boardDto.puzzleId()).isNotNull();

        ActiveGame activeGame = activeGameStore.get(gameId);
        assertThat(activeGame.createdAt()).isBetween(before, Instant.now());

        FutoshikiBoard board = activeGame.board();
        assertBoard(board, 6, Difficulty.EASY);
        assertThat(board.puzzleId()).isNotNull();
    }

    private void assertBoardDto(FutoshikiBoardDto boardDto, int size, Difficulty difficulty) {
        assertThat(boardDto).isNotNull();
        assertThat(boardDto.size()).isEqualTo(size);
        assertThat(boardDto.difficulty()).isEqualTo(difficulty);
        assertThat(boardDto.grid()).isNotNull();
        assertThat(boardDto.grid().length).isEqualTo(size);
    }

    private void assertBoard(FutoshikiBoard board, int size, Difficulty difficulty) {
        assertThat(board).isNotNull();
        assertThat(board.size()).isEqualTo(size);
        assertThat(board.difficulty()).isEqualTo(difficulty);
        assertThat(board.grid()).isNotNull();
        assertThat(board.grid().length).isEqualTo(size);
        assertThat(board.solution()).isNotNull();
        assertThat(board.solution().length).isEqualTo(size);
    }

    @Test
    void shouldNotCreateNewGame_invalidSize() {
        assertThatThrownBy(() -> futoshikiService.newGame(3, Difficulty.MEDIUM, ProviderStrategy.GENERATOR))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Board size must be between 4 and 9");
    }

    @Test
    void shouldCheckSolution() throws Exception {
        FutoshikiBoard board = TestUtils.loadBoardFromJson(objectMapper, "test-board-h4_001.json");
        ActiveGame activeGame = activeGameStore.createNewGame(board);

        SolutionDto correctSolution =
                TestUtils.loadSolutionFromJson(objectMapper,"solution-correct-h4_001.json");
        boolean isCorrect1 = futoshikiService.checkSolution(activeGame.gameId(), correctSolution.solution());
        assertTrue(isCorrect1);

        SolutionDto incorrectSolution =
                TestUtils.loadSolutionFromJson(objectMapper,"solution-incorrect-h4_001.json");
        boolean isCorrect2 = futoshikiService.checkSolution(activeGame.gameId(), incorrectSolution.solution());
        assertFalse(isCorrect2);
    }

    @Test
    void shouldNotCheckSolution_numbersNotInRange() throws Exception {
        FutoshikiBoard board = TestUtils.loadBoardFromJson(objectMapper, "test-board-h4_001.json");
        ActiveGame activeGame = activeGameStore.createNewGame(board);

        SolutionDto correctSolution
                = TestUtils.loadSolutionFromJson(objectMapper, "solution-outofrange-h4_001.json");
        assertThatThrownBy(() -> futoshikiService.checkSolution(activeGame.gameId(), correctSolution.solution()))
                .isInstanceOf(InvalidSolutionException.class)
                .hasMessage("All values in the solution must be between 1 and 4");
    }

    @Test
    void shouldNotCheckSolution_nullElement() throws Exception {
        FutoshikiBoard board = TestUtils.loadBoardFromJson(objectMapper, "test-board-h4_001.json");
        ActiveGame activeGame = activeGameStore.createNewGame(board);

        SolutionDto correctSolution =
                TestUtils.loadSolutionFromJson(objectMapper, "solution-nullelement-h4_001.json");
        assertThatThrownBy(() -> futoshikiService.checkSolution(activeGame.gameId(), correctSolution.solution()))
                .isInstanceOf(InvalidSolutionException.class)
                .hasMessage("Solution cannot contain null values");
    }

    @Test
    void shouldNotCheckSolution_arrayWrongSize() throws Exception {
        FutoshikiBoard board = TestUtils.loadBoardFromJson(objectMapper, "test-board-h4_001.json");
        ActiveGame activeGame = activeGameStore.createNewGame(board);

        SolutionDto correctSolution =
                TestUtils.loadSolutionFromJson(objectMapper, "solution-wrongsize-h4_001.json");
        assertThatThrownBy(() -> futoshikiService.checkSolution(activeGame.gameId(), correctSolution.solution()))
                .isInstanceOf(InvalidSolutionException.class)
                .hasMessage("Solution must have size: 4 x 4");
    }

    @Test
    void shouldShowSolution() throws Exception {
        FutoshikiBoard board = TestUtils.loadBoardFromJson(objectMapper, "test-board-h4_001.json");
        ActiveGame activeGame = activeGameStore.createNewGame(board);

        SolutionDto expectedSolution =
                TestUtils.loadSolutionFromJson(objectMapper,"solution-correct-h4_001.json");

        SolutionDto actualSolution =  futoshikiService.showSolution(activeGame.gameId());
        assertThat(actualSolution.solution()).isDeepEqualTo(expectedSolution.solution());
    }

}