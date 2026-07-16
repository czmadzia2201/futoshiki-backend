package org.games.futoshiki.provider.generator;

import org.games.futoshiki.model.Constraint;
import org.games.futoshiki.model.ConstraintOperator;
import org.games.futoshiki.model.Position;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class FutoshikiSolverTest {

    @Test
    void shouldFindOneSolutionForCompletedValidBoard() {
        int[][] grid = {
                {1, 2, 3, 4},
                {2, 3, 4, 1},
                {3, 4, 1, 2},
                {4, 1, 2, 3}
        };

        FutoshikiSolver solver =
                new FutoshikiSolver(4, grid, List.of());

        assertThat(solver.countSolutionsUpTo(2))
                .isEqualTo(1);
    }

    @Test
    void shouldFindNoSolutionForInvalidBoard() {
        int[][] grid = {
                {1, 1, 3, 4},
                {2, 3, 4, 1},
                {3, 4, 1, 2},
                {4, 2, 2, 3}
        };

        FutoshikiSolver solver =
                new FutoshikiSolver(4, grid, List.of());

        assertThat(solver.countSolutionsUpTo(2))
                .isZero();
    }

    @Test
    void shouldStopCountingAtSpecifiedLimit() {
        int[][] emptyGrid = new int[4][4];

        FutoshikiSolver solver =
                new FutoshikiSolver(4, emptyGrid, List.of());

        assertThat(solver.countSolutionsUpTo(2))
                .isEqualTo(2);
    }

    @Test
    void shouldRespectConstraints() {
        int[][] grid = {
                {1, 2, 3, 4},
                {2, 3, 4, 1},
                {3, 4, 1, 2},
                {4, 1, 2, 3}
        };

        Constraint contradictoryConstraint = new Constraint(
                new Position(1, 1),
                ConstraintOperator.GREATER_THAN,
                new Position(1, 2)
        );

        FutoshikiSolver solver = new FutoshikiSolver(
                4,
                grid,
                List.of(contradictoryConstraint)
        );

        assertThat(solver.countSolutionsUpTo(2))
                .isZero();
    }
}