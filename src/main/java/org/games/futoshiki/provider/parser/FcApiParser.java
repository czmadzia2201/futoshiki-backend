package org.games.futoshiki.provider.parser;

import org.games.futoshiki.exception.BoardParsingException;
import org.games.futoshiki.model.*;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class FcApiParser {

    public FutoshikiBoard parseBoard(String rawBoard, int size, Difficulty difficulty) {
        if (rawBoard == null) {
            throw new BoardParsingException("Raw board is null");
        }
        int rowLength = size * 2 - 1;
        int expectedRawBoardLength = rowLength * rowLength * 2;
        if (rawBoard.length() != expectedRawBoardLength) {
            throw new BoardParsingException("Invalid board length");
        }
        String grid = rawBoard.substring(0, rawBoard.length() / 2);
        String solution = rawBoard.substring(rawBoard.length() / 2);
        return FutoshikiBoard.builder()
                .puzzleId(null)
                .size(size)
                .difficulty(difficulty)
                .grid(parseGrid(grid, rowLength, size))
                .solution(parseSolution(solution, rowLength, size))
                .constraints(parseConstraints(grid, rowLength))
                .build();
    }

    private int[][] parseGrid(String gridString, int rowLength, int size) {
        String[] gridAsRows = splitIntoRows(gridString, rowLength);
        int[][] grid = new int[size][size];
        int gridRowIndex = 0;
        for (int row = 0; row < rowLength; row += 2) {
            String gridPart = gridAsRows[row];
            int[] gridRow = new int[size];
            int gridColIndex = 0;
            for (int col = 0; col < rowLength; col += 2) {
                char cellValueAsChar = gridPart.charAt(col);
                try {
                    int cellValue = (cellValueAsChar == '.') ? 0 : Integer.parseInt(String.valueOf(cellValueAsChar));
                    gridRow[gridColIndex] = cellValue;
                } catch (NumberFormatException e) {
                    throw new BoardParsingException("Non-numeric cell value in grid");
                }
                gridColIndex++;
            }
            grid[gridRowIndex] = gridRow;
            gridRowIndex++;
        }
        return grid;
    }

    private int[][] parseSolution(String solutionString, int rowLength, int size) {
        String[] solutionAsRows = splitIntoRows(solutionString, rowLength);
        int[][] solution = new int[size][size];
        int solutionRowIndex = 0;
        for (int row = 0; row < rowLength; row += 2) {
            String solutionPart = solutionAsRows[row];
            int[] solutionRow = new int[size];
            int solutionColIndex = 0;
            for (int col = 0; col < rowLength; col += 2) {
                char cellValueAsChar = solutionPart.charAt(col);
                try {
                    int cellValue = Integer.parseInt(String.valueOf(cellValueAsChar));
                    solutionRow[solutionColIndex] = cellValue;
                } catch (NumberFormatException e) {
                    throw new BoardParsingException("Non-numeric cell value in solution");
                }
                solutionColIndex++;
            }
            solution[solutionRowIndex] = solutionRow;
            solutionRowIndex++;
        }
        return solution;
    }

    private List<Constraint> parseConstraints(String gridString, int rowLength) {
        String[] gridAsRows = splitIntoRows(gridString, rowLength);
        List<Constraint> constraints = new ArrayList<>();
        for (int row = 0; row < rowLength; row++) {
            String gridPart = gridAsRows[row];
            for (int col = 0; col < rowLength; col++) {
                if (isConstraintOperator(gridPart.charAt(col))) {
                    constraints.add(calculateConstraint(gridPart.charAt(col), row, col));
                }
            }
        }
        return constraints;
    }

    private String[] splitIntoRows(String value, int n) {
        String[] rows = new String[n];

        for (int i = 0; i < n; i++) {
            int start = i * n;
            int end = start + n;
            rows[i] = value.substring(start, end);
        }

        return rows;
    }

    private boolean isConstraintOperator(char character) {
        return switch (character) {
            case '(', ')', '^', 'v' -> true;
            default -> false;
        };
    }

    private Constraint calculateConstraint(char character, int row, int col) {
        boolean horizontal = character == '(' || character == ')';

        Position from = horizontal
                ? new Position(row / 2 + 1, (col - 1) / 2 + 1)
                : new Position((row - 1) / 2 + 1, col / 2 + 1);

        Position to = horizontal
                ? new Position(row / 2 + 1, (col + 1) / 2 + 1)
                : new Position((row + 1) / 2 + 1, col / 2 + 1);

        ConstraintOperator operator = switch (character) {
            case '(', '^' -> ConstraintOperator.LESS_THAN;
            case ')', 'v' -> ConstraintOperator.GREATER_THAN;
            default -> throw new BoardParsingException("Unsupported constraint operator: " + character);
        };

        return new Constraint(from, operator, to);
    }

}
