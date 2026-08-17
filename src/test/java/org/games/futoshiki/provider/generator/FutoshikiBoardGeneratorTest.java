package org.games.futoshiki.provider.generator;

import org.games.futoshiki.model.Constraint;
import org.games.futoshiki.model.Difficulty;
import org.games.futoshiki.model.FutoshikiBoard;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class FutoshikiBoardGeneratorTest {

    @ParameterizedTest
    @EnumSource(Difficulty.class)
    void generatesValidUniqueHumanSolvableBoard(Difficulty difficulty) {
        FutoshikiBoard board = new FutoshikiBoardGenerator().generate(5, difficulty);

        assertThat(board.size()).isEqualTo(5);
        assertThat(board.difficulty()).isEqualTo(difficulty);
        assertLatinSquare(board.solution());
        assertCluesMatchSolution(board);
        assertConstraintsMatchSolution(board);
        assertNoDominatedGivenBounds(board);

        assertThat(new FutoshikiSolver(board.size(), board.grid(), board.constraints())
                .countSolutionsUpTo(2)).isEqualTo(1);

        HumanDifficulty ceiling = switch (difficulty) {
            case EASY -> HumanDifficulty.HIDDEN_SINGLE;
            case MEDIUM -> HumanDifficulty.HIDDEN_PAIR;
            case HARD -> HumanDifficulty.HIDDEN_TRIPLE;
        };
        FutoshikiDifficultyEvaluator.Result logical = new FutoshikiDifficultyEvaluator(
                board.size(), board.grid(), board.constraints()).solve(ceiling);
        assertThat(logical.solved()).isTrue();
    }

    private void assertNoDominatedGivenBounds(FutoshikiBoard board) {
        List<Constraint> constraints = board.constraints();
        for (int firstIndex = 0; firstIndex < constraints.size(); firstIndex++) {
            Bound first = boundForGivenAndHidden(board, constraints.get(firstIndex));
            if (first == null) continue;
            for (int secondIndex = firstIndex + 1; secondIndex < constraints.size(); secondIndex++) {
                Bound second = boundForGivenAndHidden(board, constraints.get(secondIndex));
                if (second == null || !first.hidden().equals(second.hidden())
                        || first.hiddenOperator() != second.hiddenOperator()) continue;
                throw new AssertionError("Two bounds in the same direction remain for " + first.hidden());
            }
        }
    }

    private Bound boundForGivenAndHidden(FutoshikiBoard board, Constraint constraint) {
        int from = board.grid()[constraint.from().row() - 1][constraint.from().col() - 1];
        int to = board.grid()[constraint.to().row() - 1][constraint.to().col() - 1];
        if ((from == 0) == (to == 0)) return null;
        if (from == 0) return new Bound(constraint.from(), constraint.operator());
        return new Bound(constraint.to(), switch (constraint.operator()) {
            case LESS_THAN -> org.games.futoshiki.model.ConstraintOperator.GREATER_THAN;
            case GREATER_THAN -> org.games.futoshiki.model.ConstraintOperator.LESS_THAN;
        });
    }

    private record Bound(org.games.futoshiki.model.Position hidden,
                         org.games.futoshiki.model.ConstraintOperator hiddenOperator) {}

    private void assertLatinSquare(int[][] solution) {
        int size = solution.length;
        for (int index = 0; index < size; index++) {
            Set<Integer> row = new HashSet<>();
            Set<Integer> column = new HashSet<>();
            for (int offset = 0; offset < size; offset++) {
                row.add(solution[index][offset]);
                column.add(solution[offset][index]);
            }
            assertThat(row).containsExactlyInAnyOrderElementsOf(values(size));
            assertThat(column).containsExactlyInAnyOrderElementsOf(values(size));
        }
    }

    private Set<Integer> values(int size) {
        Set<Integer> result = new HashSet<>();
        for (int value = 1; value <= size; value++) result.add(value);
        return result;
    }

    private void assertCluesMatchSolution(FutoshikiBoard board) {
        for (int row = 0; row < board.size(); row++) {
            for (int col = 0; col < board.size(); col++) {
                assertThat(board.grid()[row][col]).isIn(0, board.solution()[row][col]);
            }
        }
    }

    private void assertConstraintsMatchSolution(FutoshikiBoard board) {
        for (Constraint constraint : board.constraints()) {
            int from = board.solution()[constraint.from().row() - 1][constraint.from().col() - 1];
            int to = board.solution()[constraint.to().row() - 1][constraint.to().col() - 1];
            switch (constraint.operator()) {
                case LESS_THAN -> assertThat(from).isLessThan(to);
                case GREATER_THAN -> assertThat(from).isGreaterThan(to);
            }
        }
    }
}
