package org.games.futoshiki.model;

import lombok.Builder;

@Builder
public record Constraint(Position from, ConstraintOperator operator, Position to) {
}
