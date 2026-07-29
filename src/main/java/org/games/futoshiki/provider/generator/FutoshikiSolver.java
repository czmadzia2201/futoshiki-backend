package org.games.futoshiki.provider.generator;

import org.games.futoshiki.model.Constraint;
import org.games.futoshiki.model.Position;

import java.util.ArrayList;
import java.util.List;

/**
 * Backtracking Futoshiki solver used mainly by the board generator to verify
 * uniqueness. Its public API is intentionally unchanged.
 */
public class FutoshikiSolver {

    private final int size;
    private final int[][] grid;
    private final List<Constraint> constraints;

    private final boolean[][] rowUsed;
    private final boolean[][] columnUsed;
    private final List<Constraint>[][] constraintsByCell;
    private final int[][] minimumValue;
    private final int[][] maximumValue;

    private boolean validInitialState = true;
    private int solutionCount;

    @SuppressWarnings("unchecked")
    public FutoshikiSolver(int size, int[][] grid, List<Constraint> constraints) {
        this.size = size;
        this.grid = copy(grid);
        this.constraints = List.copyOf(constraints);
        this.rowUsed = new boolean[size][size + 1];
        this.columnUsed = new boolean[size][size + 1];
        this.constraintsByCell = (List<Constraint>[][]) new List<?>[size][size];
        this.minimumValue = new int[size][size];
        this.maximumValue = new int[size][size];

        initializeConstraintIndex();
        initializeStaticBounds();
        initializeUsedValues();
        validateGivenConstraints();
    }

    public int countSolutionsUpTo(int limit) {
        if (limit <= 0 || !validInitialState) {
            return 0;
        }

        solutionCount = 0;
        solve(limit);
        return solutionCount;
    }

    private void solve(int limit) {
        if (solutionCount >= limit) {
            return;
        }

        CellChoice choice = findBestEmptyCell();

        if (choice == null) {
            solutionCount++;
            return;
        }

        if (choice.candidateCount() == 0) {
            return;
        }

        int row = choice.row();
        int col = choice.col();

        for (int value = minimumValue[row][col]; value <= maximumValue[row][col]; value++) {
            if (!canPlace(row, col, value)) {
                continue;
            }

            place(row, col, value);
            solve(limit);
            remove(row, col, value);

            if (solutionCount >= limit) {
                return;
            }
        }
    }

    /**
     * Minimum Remaining Values heuristic. No temporary Position/list objects
     * are created while scanning the board.
     */
    private CellChoice findBestEmptyCell() {
        CellChoice best = null;

        for (int row = 0; row < size; row++) {
            for (int col = 0; col < size; col++) {
                if (grid[row][col] != 0) {
                    continue;
                }

                int candidateCount = countCandidates(row, col);

                if (candidateCount == 0) {
                    return new CellChoice(row, col, 0);
                }

                if (best == null || candidateCount < best.candidateCount()) {
                    best = new CellChoice(row, col, candidateCount);

                    if (candidateCount == 1) {
                        return best;
                    }
                }
            }
        }

        return best;
    }

    private int countCandidates(int row, int col) {
        int count = 0;

        for (int value = minimumValue[row][col]; value <= maximumValue[row][col]; value++) {
            if (canPlace(row, col, value)) {
                count++;
            }
        }

        return count;
    }

    private boolean canPlace(int row, int col, int value) {
        if (value < minimumValue[row][col] || value > maximumValue[row][col]) {
            return false;
        }

        if (rowUsed[row][value] || columnUsed[col][value]) {
            return false;
        }

        for (Constraint constraint : constraintsByCell[row][col]) {
            if (!satisfiesConstraintWithForwardCheck(constraint, row, col, value)) {
                return false;
            }
        }

        return true;
    }

    private boolean satisfiesConstraintWithForwardCheck(
            Constraint constraint,
            int changedRow,
            int changedCol,
            int changedValue) {

        int fromRow = constraint.from().row() - 1;
        int fromCol = constraint.from().col() - 1;
        int toRow = constraint.to().row() - 1;
        int toCol = constraint.to().col() - 1;

        boolean changedIsFrom = fromRow == changedRow && fromCol == changedCol;
        int otherRow = changedIsFrom ? toRow : fromRow;
        int otherCol = changedIsFrom ? toCol : fromCol;
        int otherValue = grid[otherRow][otherCol];

        if (otherValue != 0) {
            return changedIsFrom
                    ? compare(changedValue, otherValue, constraint)
                    : compare(otherValue, changedValue, constraint);
        }

        // Forward checking: the still-empty neighbour must retain at least one
        // row/column-valid value satisfying this inequality.
        for (int candidate = minimumValue[otherRow][otherCol];
             candidate <= maximumValue[otherRow][otherCol];
             candidate++) {

            if (rowUsed[otherRow][candidate] || columnUsed[otherCol][candidate]) {
                continue;
            }

            boolean relationSatisfied = changedIsFrom
                    ? compare(changedValue, candidate, constraint)
                    : compare(candidate, changedValue, constraint);

            if (relationSatisfied) {
                return true;
            }
        }

        return false;
    }

    private boolean compare(int fromValue, int toValue, Constraint constraint) {
        return switch (constraint.operator()) {
            case LESS_THAN -> fromValue < toValue;
            case GREATER_THAN -> fromValue > toValue;
        };
    }

    private void place(int row, int col, int value) {
        grid[row][col] = value;
        rowUsed[row][value] = true;
        columnUsed[col][value] = true;
    }

    private void remove(int row, int col, int value) {
        grid[row][col] = 0;
        rowUsed[row][value] = false;
        columnUsed[col][value] = false;
    }

    private void initializeConstraintIndex() {
        for (int row = 0; row < size; row++) {
            for (int col = 0; col < size; col++) {
                constraintsByCell[row][col] = new ArrayList<>(4);
            }
        }

        for (Constraint constraint : constraints) {
            int fromRow = constraint.from().row() - 1;
            int fromCol = constraint.from().col() - 1;
            int toRow = constraint.to().row() - 1;
            int toCol = constraint.to().col() - 1;

            constraintsByCell[fromRow][fromCol].add(constraint);
            constraintsByCell[toRow][toCol].add(constraint);
        }
    }

    /**
     * Propagates static lower/upper bounds through inequality chains.
     * Example for size 5: a < b < c implies a <= 3, b in [2,4], c >= 3.
     */
    private void initializeStaticBounds() {
        for (int row = 0; row < size; row++) {
            for (int col = 0; col < size; col++) {
                minimumValue[row][col] = 1;
                maximumValue[row][col] = size;
            }
        }

        boolean changed;
        int maxIterations = size * size;

        do {
            changed = false;

            for (Constraint constraint : constraints) {
                Cell smaller = smallerCell(constraint);
                Cell larger = largerCell(constraint);

                int newLargerMinimum = Math.max(
                        minimumValue[larger.row()][larger.col()],
                        minimumValue[smaller.row()][smaller.col()] + 1);

                int newSmallerMaximum = Math.min(
                        maximumValue[smaller.row()][smaller.col()],
                        maximumValue[larger.row()][larger.col()] - 1);

                if (newLargerMinimum != minimumValue[larger.row()][larger.col()]) {
                    minimumValue[larger.row()][larger.col()] = newLargerMinimum;
                    changed = true;
                }

                if (newSmallerMaximum != maximumValue[smaller.row()][smaller.col()]) {
                    maximumValue[smaller.row()][smaller.col()] = newSmallerMaximum;
                    changed = true;
                }
            }
        } while (changed && --maxIterations > 0);

        for (int row = 0; row < size; row++) {
            for (int col = 0; col < size; col++) {
                if (minimumValue[row][col] > maximumValue[row][col]) {
                    validInitialState = false;
                }
            }
        }
    }

    private Cell smallerCell(Constraint constraint) {
        Position position = switch (constraint.operator()) {
            case LESS_THAN -> constraint.from();
            case GREATER_THAN -> constraint.to();
        };
        return new Cell(position.row() - 1, position.col() - 1);
    }

    private Cell largerCell(Constraint constraint) {
        Position position = switch (constraint.operator()) {
            case LESS_THAN -> constraint.to();
            case GREATER_THAN -> constraint.from();
        };
        return new Cell(position.row() - 1, position.col() - 1);
    }

    private void initializeUsedValues() {
        for (int row = 0; row < size; row++) {
            for (int col = 0; col < size; col++) {
                int value = grid[row][col];

                if (value == 0) {
                    continue;
                }

                if (value < 1 || value > size
                        || rowUsed[row][value]
                        || columnUsed[col][value]
                        || value < minimumValue[row][col]
                        || value > maximumValue[row][col]) {
                    validInitialState = false;
                    continue;
                }

                rowUsed[row][value] = true;
                columnUsed[col][value] = true;
            }
        }
    }

    private void validateGivenConstraints() {
        if (!validInitialState) {
            return;
        }

        for (Constraint constraint : constraints) {
            int fromValue = valueAt(constraint.from());
            int toValue = valueAt(constraint.to());

            if (fromValue != 0 && toValue != 0 && !compare(fromValue, toValue, constraint)) {
                validInitialState = false;
                return;
            }
        }
    }

    private int valueAt(Position position) {
        return grid[position.row() - 1][position.col() - 1];
    }

    private int[][] copy(int[][] source) {
        int[][] result = new int[source.length][];

        for (int i = 0; i < source.length; i++) {
            result[i] = source[i].clone();
        }

        return result;
    }

    private record CellChoice(int row, int col, int candidateCount) {}

    private record Cell(int row, int col) {}
}