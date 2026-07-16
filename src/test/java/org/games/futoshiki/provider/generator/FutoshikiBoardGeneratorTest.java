package org.games.futoshiki.provider.generator;

import org.games.futoshiki.model.Constraint;
import org.games.futoshiki.model.Difficulty;
import org.games.futoshiki.model.FutoshikiBoard;
import org.games.futoshiki.model.Position;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

class FutoshikiBoardGeneratorTest {

    private final FutoshikiBoardGenerator generator =
            new FutoshikiBoardGenerator();

    @Test
    void shouldGenerateBoardWithRequestedProperties() {
        int size = 4;

        FutoshikiBoard board =
                generator.generate(size, Difficulty.EASY);

        assertThat(board).isNotNull();
        assertThat(board.puzzleId()).isNull();
        assertThat(board.size()).isEqualTo(size);
        assertThat(board.difficulty()).isEqualTo(Difficulty.EASY);

        assertSquareArray(board.grid(), size);
        assertSquareArray(board.solution(), size);

        assertThat(board.constraints()).isNotNull();
    }

    @Test
    void shouldGenerateValidLatinSquareSolution() {
        int size = 5;

        FutoshikiBoard board =
                generator.generate(size, Difficulty.MEDIUM);

        int[][] solution = board.solution();

        for (int[] row : solution) {
            assertContainsAllValues(row, size);
        }

        for (int columnIndex = 0; columnIndex < size; columnIndex++) {
            int finalColumnIndex = columnIndex;

            int[] column = IntStream.range(0, size)
                    .map(rowIndex -> solution[rowIndex][finalColumnIndex])
                    .toArray();

            assertContainsAllValues(column, size);
        }
    }

    @Test
    void shouldGenerateGridConsistentWithSolution() {
        int size = 6;

        FutoshikiBoard board =
                generator.generate(size, Difficulty.HARD);

        for (int row = 0; row < size; row++) {
            for (int column = 0; column < size; column++) {
                int gridValue = board.grid()[row][column];
                int solutionValue = board.solution()[row][column];

                assertThat(gridValue)
                        .isIn(0, solutionValue);
            }
        }
    }

    @Test
    void shouldGenerateConstraintsConsistentWithSolution() {
        FutoshikiBoard board =
                generator.generate(4, Difficulty.MEDIUM);

        int size = board.size();

        for (Constraint constraint : board.constraints()) {
            assertPositionWithinBoard(constraint.from(), size);
            assertPositionWithinBoard(constraint.to(), size);
            assertAdjacent(constraint.from(), constraint.to());

            int fromValue = valueAt(board.solution(), constraint.from());
            int toValue = valueAt(board.solution(), constraint.to());

            switch (constraint.operator()) {
                case LESS_THAN ->
                        assertThat(fromValue).isLessThan(toValue);
                case GREATER_THAN ->
                        assertThat(fromValue).isGreaterThan(toValue);
            }
        }
    }

    @Test
    void shouldGenerateBoardWithExactlyOneSolution() {
        FutoshikiBoard board =
                generator.generate(4, Difficulty.MEDIUM);

        FutoshikiSolver solver = new FutoshikiSolver(
                board.size(),
                board.grid(),
                board.constraints()
        );

        assertThat(solver.countSolutionsUpTo(2))
                .isEqualTo(1);
    }

    private void assertSquareArray(int[][] array, int size) {
        assertThat(array).isNotNull();
        assertThat(array.length).isEqualTo(size);

        assertThat(Arrays.stream(array))
                .allMatch(row -> row != null && row.length == size);
    }

    private void assertContainsAllValues(int[] values, int size) {
        assertThat(values)
                .containsExactlyInAnyOrder(
                        IntStream.rangeClosed(1, size).toArray()
                );
    }

    private void assertPositionWithinBoard(Position position, int size) {
        assertThat(position.row()).isBetween(1, size);
        assertThat(position.col()).isBetween(1, size);
    }

    private void assertAdjacent(Position first, Position second) {
        int rowDistance = Math.abs(first.row() - second.row());
        int columnDistance = Math.abs(first.col() - second.col());

        assertThat(rowDistance + columnDistance)
                .isEqualTo(1);
    }

    private int valueAt(int[][] grid, Position position) {
        return grid[position.row() - 1][position.col() - 1];
    }
}