package org.games.futoshiki.provider.generator;

import org.games.futoshiki.model.Constraint;
import org.games.futoshiki.model.ConstraintOperator;
import org.games.futoshiki.model.Position;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class FutoshikiDifficultyEvaluatorTest {

    @Test
    void shouldKeepBasicWhenNoDeductionIsNeeded() {
        int[][] grid = {
                {1, 2, 3, 4},
                {2, 3, 4, 1},
                {3, 4, 1, 2},
                {4, 1, 2, 3}
        };

        FutoshikiDifficultyEvaluator.Result result =
                new FutoshikiDifficultyEvaluator(4, grid, List.of())
                        .solve(HumanDifficulty.HIDDEN_TRIPLE);

        assertThat(result.solved()).isTrue();
        assertThat(result.hardest()).isEqualTo(HumanDifficulty.BASIC);
    }

    @Test
    void shouldDetectNakedSingle() {
        int[][] grid = {
                {1, 2, 3, 0},
                {2, 3, 4, 1},
                {3, 4, 1, 2},
                {4, 1, 2, 3}
        };

        FutoshikiDifficultyEvaluator.Result result =
                new FutoshikiDifficultyEvaluator(4, grid, List.of())
                        .solve(HumanDifficulty.HIDDEN_TRIPLE);

        assertThat(result.solved()).isTrue();
        assertThat(result.hardest()).isEqualTo(HumanDifficulty.NAKED_SINGLE);
    }

    @Test
    void shouldKeepDirectConstraintDeductionBasic() {
        int[][] grid = new int[8][8];
        grid[0][1] = 2;

        Constraint constraint = new Constraint(
                new Position(1, 2),
                ConstraintOperator.GREATER_THAN,
                new Position(1, 1)
        );

        FutoshikiDifficultyEvaluator.Result result =
                new FutoshikiDifficultyEvaluator(8, grid, List.of(constraint))
                        .solve(HumanDifficulty.NAKED_SINGLE);

        assertThat(result.solved()).isFalse();
        assertThat(result.eliminations()).isGreaterThan(0);
        assertThat(result.hardest()).isEqualTo(HumanDifficulty.BASIC);
    }

}
