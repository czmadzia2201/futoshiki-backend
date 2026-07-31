package org.games.futoshiki.provider.validator;

import org.games.futoshiki.exception.BoardValidationException;
import org.games.futoshiki.model.Constraint;
import org.games.futoshiki.model.FutoshikiBoard;
import org.games.futoshiki.model.Position;
import org.games.futoshiki.provider.generator.FutoshikiSolver;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Component
public class FutoshikiBoardValidator {

    public void validate(FutoshikiBoard board, int size) {
        if (board == null) {
            throw new BoardValidationException("Board is null");
        }
        validateBoardSize(board, size);
        validateSolutionValues(board);
        validateGridAgainstSolution(board);
        validateConstraints(board);
        validateUniqueness(board);
    }

    private void validateBoardSize(FutoshikiBoard board, int size) {
        if (board.size() != size) {
            throw new BoardValidationException("Board size does not match expected size");
        }
    }

    private void validateSolutionValues(FutoshikiBoard board) {
        if (board.solution() == null) {
            throw new BoardValidationException("Solution is null");
        }
        if (board.solution().length != board.size()) {
            throw new BoardValidationException("Solution row count does not match board size");
        }
        Set<Integer> expected = IntStream.rangeClosed(1, board.size()).boxed().collect(Collectors.toSet());
        for (int row = 0; row < board.size(); row++) {
            if (board.solution()[row] == null || board.solution()[row].length != board.size()) {
                throw new BoardValidationException("Solution col count does not match board size at row: " + row);
            }
            if (!containsExactlyOneToN(expected, board.solution()[row])) {
                throw new BoardValidationException("Invalid values at row: " + row);
            }
        }
        for (int col = 0; col < board.size(); col++) {
            int[] colValues = new int[board.size()];
            for (int row = 0; row < board.size(); row++) {
                colValues[row] = board.solution()[row][col];
            }
            if (!containsExactlyOneToN(expected, colValues)) {
                throw new BoardValidationException("Invalid values at col: " + col);
            }
        }
    }

    private void validateGridAgainstSolution(FutoshikiBoard board) {
        if (board.grid() == null) {
            throw new BoardValidationException("Grid is null");
        }
        if (board.grid().length != board.size()) {
            throw new BoardValidationException("Grid row count does not match board size");
        }
        for (int row = 0; row < board.size(); row++) {
            if (board.grid()[row] == null || board.grid()[row].length != board.size()) {
                throw new BoardValidationException("Grid col count does not match board size at row: " + row);
            }
            for (int col = 0; col < board.size(); col++) {
                if (board.grid()[row][col] != 0 && board.grid()[row][col] != board.solution()[row][col]) {
                    throw new BoardValidationException("Grid and solution inconsistent at row: " + row + ", col: " + col);
                }
            }
        }
    }

    private void validateConstraints(FutoshikiBoard board) {
        if (board.constraints() == null) {
            throw new BoardValidationException("Constraint list is null");
        }
        for (Constraint constraint : board.constraints()) {
            if (constraint == null) {
                throw new BoardValidationException("One or more constraints is null");
            }
            if (constraint.operator() == null || constraint.from() == null || constraint.to() == null) {
                throw new BoardValidationException("Constraint contains null values");
            }
            if (!constraintPositionInBounds(constraint.from(), board.size()) ||
                    !constraintPositionInBounds(constraint.to(), board.size())) {
                throw new BoardValidationException(
                        "Constraint positions are out of bounds: " + constraint.from() + ", " + constraint.to()
                );
            }
            if (!positionsCorrectlyLocated(constraint.from(), constraint.to())) {
                throw new BoardValidationException(
                        "Constraint positions are not correct:  " + constraint.from() + ", " + constraint.to()
                );
            }

            int fromValue = board.solution()[constraint.from().row()-1][constraint.from().col()-1];
            int toValue = board.solution()[constraint.to().row()-1][constraint.to().col()-1];
            boolean satisfied = switch (constraint.operator()) {
                case GREATER_THAN -> fromValue > toValue;
                case LESS_THAN -> fromValue < toValue;
            };
            if (!satisfied) {
                throw new BoardValidationException(
                        "Constraint violated between positions: " + constraint.from() + " and " + constraint.to()
                );
            }
        }
    }

    private void validateUniqueness(FutoshikiBoard board) {
        FutoshikiSolver solver = new FutoshikiSolver(
                board.size(),
                board.grid(),
                board.constraints()
        );

        if (solver.countSolutionsUpTo(2) != 1) {
            throw new BoardValidationException("Board does not have exactly one solution");
        }
    }

    private boolean constraintPositionInBounds(Position position, int size) {
        return  position.row() >= 1 && position.row() <= size &&
                position.col() >= 1 && position.col() <= size;
    }

    private boolean positionsCorrectlyLocated(Position from, Position to) {
        // Manhattan distance: check if positions are horizontally or vertically adjacent
        return Math.abs(from.row() - to.row()) + Math.abs(from.col() - to.col()) == 1;
    }

    private boolean containsExactlyOneToN(Set<Integer> expected, int[] values) {
        Set<Integer> actual = Arrays.stream(values).boxed().collect(Collectors.toSet());
        return actual.equals(expected);
    }
}
