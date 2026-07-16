package org.games.futoshiki.provider.generator;

import org.games.futoshiki.model.Constraint;
import org.games.futoshiki.model.Position;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class FutoshikiSolver {

    private final int size;
    private final int[][] grid;
    private final List<Constraint> constraints;

    private int solutionCount;

    public FutoshikiSolver(int size, int[][] grid, List<Constraint> constraints) {
        this.size = size;
        this.grid = copy(grid);
        this.constraints = constraints;
    }

    public int countSolutionsUpTo(int limit) {
        solutionCount = 0;
        solve(limit);
        return solutionCount;
    }

    private void solve(int limit) {
        if (solutionCount >= limit) {
            return;
        }

        Position empty = findBestEmptyPosition();

        if (empty == null) {
            if (isValidCompletedGrid()) {
                solutionCount++;
            }
            return;
        }

        for (int candidate = 1; candidate <= size; candidate++) {
            if (canPlace(empty, candidate)) {
                setValue(empty, candidate);
                solve(limit);
                setValue(empty, 0);
            }
        }
    }

    private boolean isValidCompletedGrid() {
        return hasValidRows()
                && hasValidColumns()
                && satisfiesAllConstraints();
    }

    private boolean hasValidRows() {
        for (int row = 1; row <= size; row++) {
            boolean[] used = new boolean[size + 1];

            for (int col = 1; col <= size; col++) {
                int value = valueAt(row, col);

                if (value < 1 || value > size || used[value]) {
                    return false;
                }

                used[value] = true;
            }
        }

        return true;
    }

    private boolean hasValidColumns() {
        for (int col = 1; col <= size; col++) {
            boolean[] used = new boolean[size + 1];

            for (int row = 1; row <= size; row++) {
                int value = valueAt(row, col);

                if (value < 1 || value > size || used[value]) {
                    return false;
                }

                used[value] = true;
            }
        }

        return true;
    }

    private boolean satisfiesAllConstraints() {
        for (Constraint constraint : constraints) {
            int fromValue = valueAt(constraint.from());
            int toValue = valueAt(constraint.to());

            boolean satisfied = switch (constraint.operator()) {
                case LESS_THAN -> fromValue < toValue;
                case GREATER_THAN -> fromValue > toValue;
            };

            if (!satisfied) {
                return false;
            }
        }

        return true;
    }

    private Position findBestEmptyPosition() {
        List<PositionWithCandidateCount> emptyPositions = new ArrayList<>();

        for (int row = 1; row <= size; row++) {
            for (int col = 1; col <= size; col++) {
                Position position = new Position(row, col);

                if (valueAt(position) == 0) {
                    int candidateCount = countCandidates(position);
                    emptyPositions.add(new PositionWithCandidateCount(position, candidateCount));
                }
            }
        }

        return emptyPositions.stream()
                .min(Comparator.comparingInt(PositionWithCandidateCount::candidateCount))
                .map(PositionWithCandidateCount::position)
                .orElse(null);
    }

    private int countCandidates(Position position) {
        int count = 0;

        for (int value = 1; value <= size; value++) {
            if (canPlace(position, value)) {
                count++;
            }
        }

        return count;
    }

    private boolean canPlace(Position position, int value) {
        return isUniqueInRow(position, value)
                && isUniqueInColumn(position, value)
                && satisfiesConstraints(position, value);
    }

    private boolean isUniqueInRow(Position position, int value) {
        int row = position.row();

        for (int col = 1; col <= size; col++) {
            if (col != position.col() && valueAt(row, col) == value) {
                return false;
            }
        }

        return true;
    }

    private boolean isUniqueInColumn(Position position, int value) {
        int col = position.col();

        for (int row = 1; row <= size; row++) {
            if (row != position.row() && valueAt(row, col) == value) {
                return false;
            }
        }

        return true;
    }

    private boolean satisfiesConstraints(Position position, int value) {
        for (Constraint constraint : constraints) {
            if (!satisfiesConstraint(constraint, position, value)) {
                return false;
            }
        }

        return true;
    }

    private boolean satisfiesConstraint(Constraint constraint, Position changedPosition, int changedValue) {
        Integer fromValue = resolveValue(constraint.from(), changedPosition, changedValue);
        Integer toValue = resolveValue(constraint.to(), changedPosition, changedValue);

        if (fromValue == null || toValue == null) {
            return true;
        }

        return switch (constraint.operator()) {
            case LESS_THAN -> fromValue < toValue;
            case GREATER_THAN -> fromValue > toValue;
        };
    }

    private Integer resolveValue(Position position, Position changedPosition, int changedValue) {
        if (position.equals(changedPosition)) {
            return changedValue;
        }

        int value = valueAt(position);

        return value == 0 ? null : value;
    }

    private int valueAt(Position position) {
        return valueAt(position.row(), position.col());
    }

    private int valueAt(int row, int col) {
        return grid[row - 1][col - 1];
    }

    private void setValue(Position position, int value) {
        grid[position.row() - 1][position.col() - 1] = value;
    }

    private int[][] copy(int[][] source) {
        int[][] copy = new int[source.length][];

        for (int i = 0; i < source.length; i++) {
            copy[i] = source[i].clone();
        }

        return copy;
    }

    private record PositionWithCandidateCount(Position position, int candidateCount) {}
}
