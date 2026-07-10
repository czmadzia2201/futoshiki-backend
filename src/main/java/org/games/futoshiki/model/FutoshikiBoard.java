package org.games.futoshiki.model;

import lombok.Builder;

import java.util.List;

@Builder(toBuilder = true)
public record FutoshikiBoard(
        String puzzleId,
        int size,
        Difficulty difficulty,
        int[][] grid,
        List<Constraint> constraints,
        int[][] solution) {
}
