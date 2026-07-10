package org.games.futoshiki.dto;

import org.games.futoshiki.model.Constraint;
import org.games.futoshiki.model.Difficulty;
import org.games.futoshiki.model.FutoshikiBoard;

import java.util.List;

public record FutoshikiBoardDto(
        String puzzleId,
        int size,
        Difficulty difficulty,
        int[][] grid,
        List<Constraint> constraints) {

    public static FutoshikiBoardDto toDto(FutoshikiBoard board) {
        return new FutoshikiBoardDto(
                board.puzzleId(),
                board.size(),
                board.difficulty(),
                board.grid(),
                board.constraints()
        );
    }
}
