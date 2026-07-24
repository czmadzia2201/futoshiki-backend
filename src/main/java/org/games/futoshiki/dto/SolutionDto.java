package org.games.futoshiki.dto;

import jakarta.validation.constraints.NotNull;

public record SolutionDto(@NotNull int[][] solution) {}
