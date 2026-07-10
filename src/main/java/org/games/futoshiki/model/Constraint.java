package org.games.futoshiki.model;

public record Constraint(Position from, ConstraintOperator operator, Position to) {
}
