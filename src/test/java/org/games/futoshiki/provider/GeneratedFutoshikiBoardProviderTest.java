package org.games.futoshiki.provider;

import org.games.futoshiki.model.Difficulty;
import org.games.futoshiki.model.FutoshikiBoard;
import org.games.futoshiki.provider.generator.FutoshikiBoardGenerator;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Arrays;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class GeneratedFutoshikiBoardProviderTest {

    private final FutoshikiBoardGenerator generator = new FutoshikiBoardGenerator();

    private final GeneratedFutoshikiBoardProvider provider = new GeneratedFutoshikiBoardProvider(generator);

    @ParameterizedTest
    @MethodSource("boardDimensions")
    void shouldGenerateBoardOfCorrectSize(int size, Difficulty difficulty) {
        FutoshikiBoard board = provider.getBoard(size, difficulty);
        assertThat(board).isNotNull();
        assertThat(board.puzzleId()).isNull();
        assertThat(board.size()).isEqualTo(size);
        assertThat(board.difficulty()).isEqualTo(difficulty);
        assertThat(board.grid().length).isEqualTo(size);
        assertThat(Arrays.stream(board.grid())).allMatch(row -> row.length == size);
        assertThat(board.solution().length).isEqualTo(size);
        assertThat(Arrays.stream(board.solution())).allMatch(row -> row.length == size);
    }

    private static Stream<Arguments> boardDimensions() {
        return Stream.of(
                Arguments.of(6,  Difficulty.EASY),
                Arguments.of(8, Difficulty.MEDIUM),
                Arguments.of(4, Difficulty.HARD)
        );
    }

}