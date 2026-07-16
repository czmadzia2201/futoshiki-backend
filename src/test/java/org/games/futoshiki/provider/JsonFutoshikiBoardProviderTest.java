package org.games.futoshiki.provider;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.games.futoshiki.exception.BoardLoadingException;
import org.games.futoshiki.model.Difficulty;
import org.games.futoshiki.model.FutoshikiBoard;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Arrays;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JsonFutoshikiBoardProviderTest {

    private final JsonFutoshikiBoardProvider provider = new JsonFutoshikiBoardProvider(new ObjectMapper());

    @ParameterizedTest
    @MethodSource("boardDimensions")
    void shouldGenerateBoardOfCorrectSize(int size, Difficulty difficulty, String prefix) {
        FutoshikiBoard board = provider.getBoard(size, difficulty);
        assertThat(board).isNotNull();
        assertThat(board.puzzleId()).startsWith(prefix);
        assertThat(board.size()).isEqualTo(size);
        assertThat(board.difficulty()).isEqualTo(difficulty);
        assertThat(board.grid().length).isEqualTo(size);
        assertThat(Arrays.stream(board.grid())).allMatch(row -> row.length == size);
        assertThat(board.solution().length).isEqualTo(size);
        assertThat(Arrays.stream(board.solution())).allMatch(row -> row.length == size);
    }

    private static Stream<Arguments> boardDimensions() {
        return Stream.of(
                Arguments.of(6,  Difficulty.EASY, "e6_"),
                Arguments.of(8, Difficulty.MEDIUM, "m8_"),
                Arguments.of(4, Difficulty.HARD, "h4_")
        );
    }

    @Test
    void shouldThrowWhenPuzzleFileCannotBeLoaded() {
        assertThatThrownBy(() -> provider.getBoard(3, Difficulty.EASY))
                .isInstanceOf(BoardLoadingException.class)
                .hasMessage("Unable to load puzzle after 5 attempts.");
    }

    @Test
    void shouldSaveLastFiveBoardIds() {
        assertThat(provider.getPuzzleIds("e4")).isNull();

        FutoshikiBoard board1 = provider.getBoard(4, Difficulty.EASY);
        assertThat(provider.getPuzzleIds("e4"))
                .hasSize(1)
                .containsExactly(
                        extractPuzzleNo(board1)
                );

        FutoshikiBoard board2 = provider.getBoard(4, Difficulty.EASY);
        assertThat(provider.getPuzzleIds("e4"))
                .hasSize(2)
                .containsExactly(
                        extractPuzzleNo(board1),
                        extractPuzzleNo(board2)
                );

        FutoshikiBoard board3 = provider.getBoard(4, Difficulty.EASY);
        FutoshikiBoard board4 = provider.getBoard(4, Difficulty.EASY);
        FutoshikiBoard board5 = provider.getBoard(4, Difficulty.EASY);

        assertThat(provider.getPuzzleIds("e4"))
                .hasSize(5)
                .containsExactly(
                        extractPuzzleNo(board1),
                        extractPuzzleNo(board2),
                        extractPuzzleNo(board3),
                        extractPuzzleNo(board4),
                        extractPuzzleNo(board5)
                );

        FutoshikiBoard board6 = provider.getBoard(4, Difficulty.EASY);

        assertThat(provider.getPuzzleIds("e4"))
                .hasSize(5)
                .containsExactly(
                        extractPuzzleNo(board2),
                        extractPuzzleNo(board3),
                        extractPuzzleNo(board4),
                        extractPuzzleNo(board5),
                        extractPuzzleNo(board6)
                );
    }

    Integer extractPuzzleNo(FutoshikiBoard board) {
        return Integer.parseInt(board.puzzleId().split("_")[1]);
    }

}