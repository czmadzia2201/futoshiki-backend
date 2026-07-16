package org.games.futoshiki.provider.generator;

import org.games.futoshiki.model.*;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class FutoshikiBoardGenerator {

    private final Random random = new Random();

    public FutoshikiBoard generate(int size, Difficulty difficulty) {
        int[][] solution = generateSolution(size);
        List<Constraint> constraints = generateConstraints(solution, difficulty);
        int[][] grid = createPuzzleGrid(solution, constraints, difficulty);

        return new FutoshikiBoard(
                null,
                size,
                difficulty,
                grid,
                constraints,
                solution
        );
    }

    private int[][] generateSolution(int size) {
        int[][] base = new int[size][size];

        for (int row = 0; row < size; row++) {
            for (int col = 0; col < size; col++) {
                base[row][col] = ((row + col) % size) + 1;
            }
        }

        shuffleRows(base);
        shuffleColumns(base);
        shuffleValues(base);

        return base;
    }

    private void shuffleRows(int[][] grid) {
        List<int[]> rows = new ArrayList<>(List.of(grid));
        Collections.shuffle(rows, random);

        for (int i = 0; i < grid.length; i++) {
            grid[i] = rows.get(i);
        }
    }

    private void shuffleColumns(int[][] grid) {
        int size = grid.length;
        List<Integer> columns = new ArrayList<>();

        for (int i = 0; i < size; i++) {
            columns.add(i);
        }

        Collections.shuffle(columns, random);

        int[][] copy = copy(grid);

        for (int row = 0; row < size; row++) {
            for (int newCol = 0; newCol < size; newCol++) {
                grid[row][newCol] = copy[row][columns.get(newCol)];
            }
        }
    }

    private void shuffleValues(int[][] grid) {
        int size = grid.length;
        List<Integer> values = new ArrayList<>();

        for (int i = 1; i <= size; i++) {
            values.add(i);
        }

        Collections.shuffle(values, random);

        for (int row = 0; row < size; row++) {
            for (int col = 0; col < size; col++) {
                int oldValue = grid[row][col];
                grid[row][col] = values.get(oldValue - 1);
            }
        }
    }

    private List<Constraint> generateConstraints(int[][] solution, Difficulty difficulty) {
        int size = solution.length;
        List<Constraint> candidates = new ArrayList<>();

        for (int row = 1; row <= size; row++) {
            for (int col = 1; col <= size; col++) {
                if (col < size) {
                    candidates.add(createConstraint(solution, row, col, row, col + 1));
                }

                if (row < size) {
                    candidates.add(createConstraint(solution, row, col, row + 1, col));
                }
            }
        }

        Collections.shuffle(candidates, random);

        int targetCount = switch (difficulty) {
            case EASY -> size * 4;
            case MEDIUM -> size * 3;
            case HARD -> size * 2;
        };

        return new ArrayList<>(candidates.subList(0, Math.min(targetCount, candidates.size())));
    }

    private Constraint createConstraint(int[][] solution, int fromRow, int fromCol, int toRow, int toCol) {
        int fromValue = valueAt(solution, fromRow, fromCol);
        int toValue = valueAt(solution, toRow, toCol);

        ConstraintOperator operator = fromValue < toValue
                ? ConstraintOperator.LESS_THAN
                : ConstraintOperator.GREATER_THAN;

        return new Constraint(
                new Position(fromRow, fromCol),
                operator,
                new Position(toRow, toCol)
        );
    }

    private int[][] createPuzzleGrid(int[][] solution, List<Constraint> constraints, Difficulty difficulty) {
        int size = solution.length;
        int[][] grid = copy(solution);

        int targetGivenCount = switch (difficulty) {
            case EASY -> Math.max(size + 2, size * size / 2);
            case MEDIUM -> Math.max(size, size * size / 3);
            case HARD -> Math.max(size - 1, size * size / 4);
        };

        List<Position> positions = allPositions(size);
        Collections.shuffle(positions, random);

        for (Position position : positions) {
            if (countGivenCells(grid) <= targetGivenCount) {
                break;
            }

            int rowIndex = position.row() - 1;
            int colIndex = position.col() - 1;
            int previousValue = grid[rowIndex][colIndex];

            grid[rowIndex][colIndex] = 0;

            FutoshikiSolver solver = new FutoshikiSolver(size, grid, constraints);
            boolean hasUniqueSolution = solver.countSolutionsUpTo(2) == 1;

            if (!hasUniqueSolution) {
                grid[rowIndex][colIndex] = previousValue;
            }
        }

        return grid;
    }

    private List<Position> allPositions(int size) {
        List<Position> positions = new ArrayList<>();

        for (int row = 1; row <= size; row++) {
            for (int col = 1; col <= size; col++) {
                positions.add(new Position(row, col));
            }
        }

        return positions;
    }

    private int countGivenCells(int[][] grid) {
        int count = 0;

        for (int[] row : grid) {
            for (int value : row) {
                if (value != 0) {
                    count++;
                }
            }
        }

        return count;
    }

    private int valueAt(int[][] grid, int row, int col) {
        return grid[row - 1][col - 1];
    }

    private int[][] copy(int[][] source) {
        int[][] copy = new int[source.length][];

        for (int i = 0; i < source.length; i++) {
            copy[i] = source[i].clone();
        }

        return copy;
    }
}