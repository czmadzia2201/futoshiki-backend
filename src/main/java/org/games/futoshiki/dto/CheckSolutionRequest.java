package org.games.futoshiki.dto;

import jakarta.validation.constraints.NotNull;

public record CheckSolutionRequest(@NotNull int[][] solution) {}
