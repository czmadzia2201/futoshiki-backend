package org.games.futoshiki.provider.generator;

import org.games.futoshiki.model.Constraint;
import org.games.futoshiki.model.ConstraintOperator;
import org.games.futoshiki.model.Difficulty;
import org.games.futoshiki.model.FutoshikiBoard;
import org.games.futoshiki.model.Position;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

@Component
public class FutoshikiBoardGenerator {

    private static final int CONSTRAINT_LAYOUT_ATTEMPTS = 24;
    private static final double EASY_GIVEN_RATIO = 0.25;
    private static final double MEDIUM_GIVEN_RATIO = 0.21;
    private static final double HARD_GIVEN_RATIO = 0.18;

    private final Random random = new Random();

    /** External API intentionally unchanged. */
    public FutoshikiBoard generate(int size, Difficulty difficulty) {
        int[][] solution = generateSolution(size);
        List<Constraint> constraints = generateConstraints(solution, difficulty);
        int[][] grid = createPuzzleGrid(solution, constraints, difficulty);

        constraints = removeConstraintsBetweenGivenCells(grid, constraints);
        constraints = removeRedundantConstraintsForHiddenCells(grid, constraints);

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
        List<Integer> columns = new ArrayList<>(size);

        for (int i = 0; i < size; i++) {
            columns.add(i);
        }

        Collections.shuffle(columns, random);
        int[][] source = copy(grid);

        for (int row = 0; row < size; row++) {
            for (int newCol = 0; newCol < size; newCol++) {
                grid[row][newCol] = source[row][columns.get(newCol)];
            }
        }
    }

    private void shuffleValues(int[][] grid) {
        int size = grid.length;
        List<Integer> values = new ArrayList<>(size);

        for (int i = 1; i <= size; i++) {
            values.add(i);
        }

        Collections.shuffle(values, random);

        for (int row = 0; row < size; row++) {
            for (int col = 0; col < size; col++) {
                grid[row][col] = values.get(grid[row][col] - 1);
            }
        }
    }

    /**
     * Still samples constraints from the solved board, but no longer accepts
     * the first random subset. Several cheap layouts are scored and the best
     * one for the requested difficulty is retained.
     */
    private List<Constraint> generateConstraints(int[][] solution, Difficulty difficulty) {
        List<Constraint> allCandidates = allConstraintCandidates(solution);
        int targetCount = targetConstraintCount(solution.length, difficulty, allCandidates.size());

        List<Constraint> best = null;
        double bestScore = Double.NEGATIVE_INFINITY;

        for (int attempt = 0; attempt < CONSTRAINT_LAYOUT_ATTEMPTS; attempt++) {
            Collections.shuffle(allCandidates, random);
            List<Constraint> sample = new ArrayList<>(allCandidates.subList(0, targetCount));
            double score = scoreConstraintLayout(sample, solution.length, difficulty);

            if (score > bestScore) {
                bestScore = score;
                best = sample;
            }
        }

        return best == null ? List.of() : best;
    }

    private List<Constraint> allConstraintCandidates(int[][] solution) {
        int size = solution.length;
        List<Constraint> candidates = new ArrayList<>(2 * size * (size - 1));

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

        return candidates;
    }

    private int targetConstraintCount(int size, Difficulty difficulty, int maximum) {
        int target = switch (difficulty) {
            case EASY -> (int) Math.round(size * 5.0);
            case MEDIUM -> (int) Math.round(size * 4.5);
            case HARD -> (int) Math.round(size * 4.0);
        };

        return Math.min(target, maximum);
    }

    private double scoreConstraintLayout(
            List<Constraint> constraints,
            int size,
            Difficulty difficulty) {

        ConstraintLayoutMetrics metrics = calculateConstraintMetrics(constraints, size);

        return switch (difficulty) {
            // Long connected chains and concentrated structure make deductions
            // easier and give the player a clearer starting point.
            case EASY ->
                    4.0 * metrics.longestDirectedChain()
                            + 1.8 * metrics.connectedPairs()
                            + 1.2 * metrics.highDegreeCells()
                            - 0.8 * metrics.spatialSpread();

            // A mixed layout: some useful chains, but not one dominating area.
            case MEDIUM ->
                    2.0 * metrics.connectedPairs()
                            + 1.0 * metrics.spatialSpread()
                            - 0.8 * Math.abs(metrics.longestDirectedChain() - 3)
                            - 0.5 * metrics.highDegreeCells();

            // Prefer short, dispersed and weakly connected inequalities.
            case HARD ->
                    2.4 * metrics.spatialSpread()
                            - 3.0 * metrics.longestDirectedChain()
                            - 1.6 * metrics.connectedPairs()
                            - 1.2 * metrics.highDegreeCells();
        };
    }

    private ConstraintLayoutMetrics calculateConstraintMetrics(
            List<Constraint> constraints,
            int size) {

        int cellCount = size * size;
        List<Integer>[] outgoing = createAdjacency(cellCount);
        int[] degree = new int[cellCount];
        Set<Integer> occupiedCells = new HashSet<>();

        for (Constraint constraint : constraints) {
            int from = indexOf(constraint.from(), size);
            int to = indexOf(constraint.to(), size);
            int smaller = constraint.operator() == ConstraintOperator.LESS_THAN ? from : to;
            int larger = constraint.operator() == ConstraintOperator.LESS_THAN ? to : from;

            outgoing[smaller].add(larger);
            degree[from]++;
            degree[to]++;
            occupiedCells.add(from);
            occupiedCells.add(to);
        }

        int connectedPairs = 0;
        int highDegreeCells = 0;

        for (int value : degree) {
            connectedPairs += Math.max(0, value - 1);
            if (value >= 3) {
                highDegreeCells++;
            }
        }

        int longestChain = longestDirectedChain(outgoing);
        double spread = spatialSpread(occupiedCells, size);

        return new ConstraintLayoutMetrics(longestChain, connectedPairs, highDegreeCells, spread);
    }

    @SuppressWarnings("unchecked")
    private List<Integer>[] createAdjacency(int size) {
        List<Integer>[] result = (List<Integer>[]) new List<?>[size];
        for (int i = 0; i < size; i++) {
            result[i] = new ArrayList<>();
        }
        return result;
    }

    private int longestDirectedChain(List<Integer>[] outgoing) {
        int[] memo = new int[outgoing.length];
        int longest = 1;

        for (int node = 0; node < outgoing.length; node++) {
            longest = Math.max(longest, longestPathFrom(node, outgoing, memo));
        }

        return longest;
    }

    private int longestPathFrom(int node, List<Integer>[] outgoing, int[] memo) {
        if (memo[node] != 0) {
            return memo[node];
        }

        int longest = 1;
        for (int neighbour : outgoing[node]) {
            longest = Math.max(longest, 1 + longestPathFrom(neighbour, outgoing, memo));
        }

        memo[node] = longest;
        return longest;
    }

    private double spatialSpread(Set<Integer> occupiedCells, int size) {
        if (occupiedCells.isEmpty()) {
            return 0.0;
        }

        boolean[] rows = new boolean[size];
        boolean[] columns = new boolean[size];

        for (int index : occupiedCells) {
            rows[index / size] = true;
            columns[index % size] = true;
        }

        int usedRows = countTrue(rows);
        int usedColumns = countTrue(columns);
        return (usedRows + usedColumns) / (double) (2 * size);
    }

    private int countTrue(boolean[] values) {
        int count = 0;
        for (boolean value : values) {
            if (value) {
                count++;
            }
        }
        return count;
    }

    private int indexOf(Position position, int size) {
        return (position.row() - 1) * size + position.col() - 1;
    }

    private Constraint createConstraint(
            int[][] solution,
            int fromRow,
            int fromCol,
            int toRow,
            int toCol) {

        ConstraintOperator operator = valueAt(solution, fromRow, fromCol)
                < valueAt(solution, toRow, toCol)
                ? ConstraintOperator.LESS_THAN
                : ConstraintOperator.GREATER_THAN;

        return new Constraint(
                new Position(fromRow, fromCol),
                operator,
                new Position(toRow, toCol)
        );
    }

    private int[][] createPuzzleGrid(
            int[][] solution,
            List<Constraint> constraints,
            Difficulty difficulty) {

        int size = solution.length;
        int[][] grid = copy(solution);
        int targetGivenCount = targetGivenCount(size, difficulty);
        int givenCount = size * size;

        List<Position> remaining = allPositions(size);
        ClueLayoutProfile clueLayout = createClueLayoutProfile(size, difficulty);
        int[] rowGivenCount = new int[size];
        int[] columnGivenCount = new int[size];

        for (int i = 0; i < size; i++) {
            rowGivenCount[i] = size;
            columnGivenCount[i] = size;
        }

        while (givenCount > targetGivenCount && !remaining.isEmpty()) {
            Position position = chooseNextRemoval(
                    remaining,
                    rowGivenCount,
                    columnGivenCount,
                    difficulty,
                    clueLayout);
            remaining.remove(position);

            int row = position.row() - 1;
            int col = position.col() - 1;
            int previousValue = grid[row][col];
            grid[row][col] = 0;

            FutoshikiSolver solver = new FutoshikiSolver(size, grid, constraints);

            if (solver.countSolutionsUpTo(2) == 1) {
                givenCount--;
                rowGivenCount[row]--;
                columnGivenCount[col]--;
            } else {
                grid[row][col] = previousValue;
            }
        }

        return grid;
    }

    private int targetGivenCount(int size, Difficulty difficulty) {
        int area = size * size;

        return switch (difficulty) {
            // Roughly half of the previous EASY clues.
            case EASY -> Math.max(size, (int) Math.round(area * EASY_GIVEN_RATIO));

            // Considerably fewer than before, but still slightly above HARD.
            case MEDIUM -> Math.max(size - 1, (int) Math.round(area * MEDIUM_GIVEN_RATIO));

            // A smaller reduction than for EASY and MEDIUM.
            case HARD -> Math.max(size - 2, (int) Math.round(area * HARD_GIVEN_RATIO));
        };
    }

    /**
     * EASY keeps clues in one or two loose, irregular neighbourhoods. The
     * clusters are deliberately fuzzy: row/column balancing and random noise
     * prevent full rows, full columns and obvious rectangles.
     *
     * MEDIUM uses a much weaker version of the same preference. HARD does not
     * use clustering and remains driven mainly by an even row/column spread.
     */
    private ClueLayoutProfile createClueLayoutProfile(int size, Difficulty difficulty) {
        if (difficulty == Difficulty.HARD) {
            return new ClueLayoutProfile(List.of());
        }

        int clusterCount = difficulty == Difficulty.EASY && random.nextDouble() < 0.55 ? 2 : 1;
        List<ClusterCenter> centers = new ArrayList<>(clusterCount);

        for (int i = 0; i < clusterCount; i++) {
            double row = random.nextDouble() * (size - 1);
            double col = random.nextDouble() * (size - 1);
            double radius = difficulty == Difficulty.EASY
                    ? Math.max(1.4, size * (0.34 + random.nextDouble() * 0.12))
                    : Math.max(1.2, size * (0.27 + random.nextDouble() * 0.08));
            centers.add(new ClusterCenter(row, col, radius));
        }

        return new ClueLayoutProfile(centers);
    }

    private Position chooseNextRemoval(
            List<Position> remaining,
            int[] rowGivenCount,
            int[] columnGivenCount,
            Difficulty difficulty,
            ClueLayoutProfile clueLayout) {

        Position best = null;
        double bestScore = Double.NEGATIVE_INFINITY;

        for (Position position : remaining) {
            int row = position.row() - 1;
            int col = position.col() - 1;
            double density = rowGivenCount[row] + columnGivenCount[col];
            double clusterAffinity = clusterAffinity(position, clueLayout);
            double noise = random.nextDouble();

            double score = switch (difficulty) {
                // Remove mainly outside the chosen neighbourhoods. A moderate
                // density term prevents neat full rows/columns from surviving.
                case EASY ->
                        -2.8 * clusterAffinity
                                + 0.55 * density
                                + 1.8 * noise;

                // Keep only a faint clustering signal and more balancing.
                case MEDIUM ->
                        -1.15 * clusterAffinity
                                + 0.85 * density
                                + 1.7 * noise;

                // Remove from dense rows and columns first, leaving a fairly
                // even final distribution.
                case HARD ->
                        1.35 * density
                                + 1.2 * noise;
            };

            if (score > bestScore) {
                bestScore = score;
                best = position;
            }
        }

        return best;
    }

    private double clusterAffinity(Position position, ClueLayoutProfile profile) {
        double strongest = 0.0;
        double row = position.row() - 1;
        double col = position.col() - 1;

        for (ClusterCenter center : profile.centers()) {
            double rowDistance = row - center.row();
            double colDistance = col - center.col();
            double distance = Math.sqrt(rowDistance * rowDistance + colDistance * colDistance);
            double affinity = Math.max(0.0, 1.0 - distance / center.radius());
            strongest = Math.max(strongest, affinity);
        }

        return strongest;
    }

    private List<Constraint> removeConstraintsBetweenGivenCells(
            int[][] grid,
            List<Constraint> constraints) {

        return constraints.stream()
                .filter(constraint -> isHidden(grid, constraint.from())
                        || isHidden(grid, constraint.to()))
                .toList();
    }

    private List<Constraint> removeRedundantConstraintsForHiddenCells(
            int[][] grid,
            List<Constraint> constraints) {

        Map<HiddenConstraintKey, ConstraintCandidate> strongestConstraints = new HashMap<>();

        for (Constraint constraint : constraints) {
            boolean fromHidden = isHidden(grid, constraint.from());
            boolean toHidden = isHidden(grid, constraint.to());

            if (fromHidden == toHidden) {
                continue;
            }

            Position hiddenPosition = fromHidden ? constraint.from() : constraint.to();
            Position givenPosition = fromHidden ? constraint.to() : constraint.from();
            ConstraintOperator effectiveOperator = fromHidden
                    ? constraint.operator()
                    : reverse(constraint.operator());
            int givenValue = valueAt(grid, givenPosition.row(), givenPosition.col());

            HiddenConstraintKey key = new HiddenConstraintKey(hiddenPosition, effectiveOperator);
            ConstraintCandidate candidate = new ConstraintCandidate(
                    constraint,
                    effectiveOperator,
                    givenValue);

            strongestConstraints.merge(key, candidate, this::strongerConstraint);
        }

        Set<Constraint> constraintsToKeep = Collections.newSetFromMap(new IdentityHashMap<>());
        strongestConstraints.values().stream()
                .map(ConstraintCandidate::constraint)
                .forEach(constraintsToKeep::add);

        return constraints.stream()
                .filter(constraint -> {
                    boolean fromHidden = isHidden(grid, constraint.from());
                    boolean toHidden = isHidden(grid, constraint.to());
                    return fromHidden == toHidden || constraintsToKeep.contains(constraint);
                })
                .toList();
    }

    private ConstraintCandidate strongerConstraint(
            ConstraintCandidate first,
            ConstraintCandidate second) {

        return switch (first.effectiveOperator()) {
            case LESS_THAN -> first.givenValue() <= second.givenValue() ? first : second;
            case GREATER_THAN -> first.givenValue() >= second.givenValue() ? first : second;
        };
    }

    private ConstraintOperator reverse(ConstraintOperator operator) {
        return switch (operator) {
            case LESS_THAN -> ConstraintOperator.GREATER_THAN;
            case GREATER_THAN -> ConstraintOperator.LESS_THAN;
        };
    }

    private boolean isHidden(int[][] grid, Position position) {
        return valueAt(grid, position.row(), position.col()) == 0;
    }

    private List<Position> allPositions(int size) {
        List<Position> positions = new ArrayList<>(size * size);

        for (int row = 1; row <= size; row++) {
            for (int col = 1; col <= size; col++) {
                positions.add(new Position(row, col));
            }
        }

        return positions;
    }

    private int valueAt(int[][] grid, int row, int col) {
        return grid[row - 1][col - 1];
    }

    private int[][] copy(int[][] source) {
        int[][] result = new int[source.length][];

        for (int i = 0; i < source.length; i++) {
            result[i] = source[i].clone();
        }

        return result;
    }

    private record HiddenConstraintKey(
            Position hiddenPosition,
            ConstraintOperator operator) {}

    private record ConstraintCandidate(
            Constraint constraint,
            ConstraintOperator effectiveOperator,
            int givenValue) {}

    private record ConstraintLayoutMetrics(
            int longestDirectedChain,
            int connectedPairs,
            int highDegreeCells,
            double spatialSpread) {}

    private record ClueLayoutProfile(List<ClusterCenter> centers) {}

    private record ClusterCenter(double row, double col, double radius) {}
}