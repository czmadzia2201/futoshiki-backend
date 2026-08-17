package org.games.futoshiki.provider.generator;

import org.games.futoshiki.model.Constraint;
import org.games.futoshiki.model.ConstraintOperator;
import org.games.futoshiki.model.Difficulty;
import org.games.futoshiki.model.FutoshikiBoard;
import org.games.futoshiki.model.Position;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

/**
 * Generates uniquely solvable puzzles and grades them by a no-guessing,
 * candidate based solve rather than by clue counts alone.
 *
 * <p>Clue and constraint counts are outcomes of stochastic pruning and
 * logical solvability checks, not fixed targets.</p>
 */

@Component
public class FutoshikiBoardGenerator {

    private static final Logger log = LoggerFactory.getLogger(FutoshikiBoardGenerator.class);
    private static final int EASY_ATTEMPTS = 6;
    private static final int MEDIUM_ATTEMPTS = 18;
    private static final int HARD_ATTEMPTS = 24;
    private static final double EASY_BASIC_ACCEPTANCE = 1.0;
    private static final double MEDIUM_BASIC_ACCEPTANCE = 0.6;
    private static final double HARD_BASIC_ACCEPTANCE = 0.2;
    private final Random random;

    public FutoshikiBoardGenerator() {
        this(new Random());
    }

    FutoshikiBoardGenerator(Random random) {
        this.random = random;
    }

    /** External API matches the original generator. */
    public FutoshikiBoard generate(int size, Difficulty difficulty) {
        Candidate best = null;
        int attempts = switch (difficulty) {
            case EASY -> EASY_ATTEMPTS;
            case MEDIUM -> MEDIUM_ATTEMPTS;
            case HARD -> HARD_ATTEMPTS;
        };
        for (int attempt = 0; attempt < attempts; attempt++) {
            Candidate candidate = generateCandidate(size, difficulty);
            if (best == null || candidate.quality() > best.quality()) best = candidate;
            if (candidate.matchesTarget()) break;
        }

        if (!best.matchesTarget()) {
            log.debug("Could not generate target difficulty {} after {} attempts; returning best candidate",
                    difficulty, attempts);
        }

        return new FutoshikiBoard(null, size, difficulty, best.grid(),
                List.copyOf(best.constraints()), best.solution());
    }

    private Candidate generateCandidate(int size, Difficulty difficulty) {
        int[][] solution = generateSolution(size);
        List<Constraint> constraints = allConstraints(solution);
        int[][] grid = copy(solution);
        List<Position> positions = allPositions(size);
        Collections.shuffle(positions, random);

        DifficultyRange difficultyRange = difficultyRange(difficulty);
        HumanDifficulty ceiling = difficultyRange.max();
        double clueRetentionChance = clueRetentionChance(difficulty);

        for (Position position : positions) {
            if (random.nextDouble() < clueRetentionChance) continue;
            int row = position.row() - 1;
            int col = position.col() - 1;
            int previous = grid[row][col];
            grid[row][col] = 0;

            FutoshikiDifficultyEvaluator.Result logical = analyze(size, grid, constraints, ceiling);
            if (!logical.solved() || countSolutions(size, grid, constraints) != 1) {
                grid[row][col] = previous;
            }
        }

        constraints = removeObviousRedundancies(grid, constraints);
        constraints = pruneConstraints(size, grid, constraints, ceiling, difficulty);

        FutoshikiDifficultyEvaluator.Result result = analyze(size, grid, constraints, ceiling);
        int givens = countGivens(grid);
        boolean inRange = result.hardest().rank() >= difficultyRange.min().rank();
        boolean basicAccepted = result.hardest() == HumanDifficulty.BASIC
                && random.nextDouble() < difficultyRange.basicAcceptanceChance();
        boolean matches = result.solved() && (inRange || basicAccepted);
        int quality = (matches ? 10_000 : 0)
                + result.score() - givens * 2;

        return new Candidate(solution, grid, constraints, quality, matches);
    }

    private FutoshikiDifficultyEvaluator.Result analyze(int size, int[][] grid,
                                                        List<Constraint> constraints,
                                                        HumanDifficulty ceiling) {
        return new FutoshikiDifficultyEvaluator(size, grid, constraints).solve(ceiling);
    }

    private int countSolutions(int size, int[][] grid, List<Constraint> constraints) {
        return new FutoshikiSolver(size, grid, constraints).countSolutionsUpTo(2);
    }

    private DifficultyRange difficultyRange(Difficulty difficulty) {
        return switch (difficulty) {
            case EASY -> new DifficultyRange(
                    HumanDifficulty.NAKED_SINGLE,
                    HumanDifficulty.HIDDEN_SINGLE,
                    EASY_BASIC_ACCEPTANCE);
            case MEDIUM -> new DifficultyRange(
                    HumanDifficulty.NAKED_SINGLE,
                    HumanDifficulty.HIDDEN_PAIR,
                    MEDIUM_BASIC_ACCEPTANCE);
            case HARD -> new DifficultyRange(
                    HumanDifficulty.NAKED_SINGLE,
                    HumanDifficulty.HIDDEN_TRIPLE,
                    HARD_BASIC_ACCEPTANCE);
        };
    }

    private double clueRetentionChance(Difficulty difficulty) {
        double base = switch (difficulty) {
            case EASY -> 0.17;
            case MEDIUM -> 0.16;
            case HARD -> 0.15;
        };
        return base + random.nextDouble() * 0.04;
    }

    private List<Constraint> pruneConstraints(int size, int[][] grid,
                                              List<Constraint> source,
                                              HumanDifficulty ceiling,
                                              Difficulty difficulty) {
        List<Constraint> baseline = pruneConstraintsInOrder(
                size, grid, source, ceiling, shuffledCopy(source), 0);
        int targetCount = baseline.size();

        List<Constraint> result = new ArrayList<>(source);
        List<Constraint> removalOrder = new ArrayList<>(source);
        Map<Constraint, Double> usefulness = constraintUsefulness(
                size, grid, source, difficulty);
        removalOrder.sort(Comparator.comparingDouble(usefulness::get));

        result = pruneConstraintsInOrder(
                size, grid, result, ceiling, removalOrder, targetCount);
        return result.size() == targetCount ? result : baseline;
    }

    private List<Constraint> shuffledCopy(List<Constraint> source) {
        List<Constraint> result = new ArrayList<>(source);
        Collections.shuffle(result, random);
        return result;
    }

    private List<Constraint> pruneConstraintsInOrder(
            int size,
            int[][] grid,
            List<Constraint> source,
            HumanDifficulty ceiling,
            List<Constraint> removalOrder,
            int targetCount) {
        List<Constraint> result = new ArrayList<>(source);

        for (Constraint constraint : removalOrder) {
            if (targetCount > 0 && result.size() <= targetCount) break;
            int index = result.indexOf(constraint);
            if (index < 0) continue;
            result.remove(index);
            FutoshikiDifficultyEvaluator.Result logical = analyze(size, grid, result, ceiling);
            if (!logical.solved() || countSolutions(size, grid, result) != 1) {
                result.add(index, constraint);
            }
        }
        return result;
    }

    /**
     * Low-value and isolated inequalities are tested for removal first. This
     * changes which constraints survive, but neither introduces a target count
     * nor alters the clue-removal process.
     */
    private Map<Constraint, Double> constraintUsefulness(int size, int[][] grid,
                                                         List<Constraint> constraints,
                                                         Difficulty difficulty) {
        ChainMetrics chains = calculateChainMetrics(size, constraints);
        double preferenceStrength = switch (difficulty) {
            case EASY -> 1.30;
            case MEDIUM -> 0.90;
            case HARD -> 0.48;
        };
        Map<Constraint, Double> result = new HashMap<>();

        for (Constraint constraint : constraints) {
            int information = directCandidateEliminations(size, grid, constraint);
            int chainLength = chains.lengthByConstraint().getOrDefault(constraint, 1);
            int connectedness = chains.degreeByPosition().getOrDefault(constraint.from(), 0)
                    + chains.degreeByPosition().getOrDefault(constraint.to(), 0) - 2;
            double structuralValue = information * 3.0
                    + Math.max(0, chainLength - 1) * 2.5
                    + Math.max(0, connectedness) * 0.8;

            // Noise preserves variety. Its relative influence is deliberately
            // larger on HARD, where fewer obvious signposts are desirable.
            double noise = random.nextDouble() * (2.2 - preferenceStrength);
            result.put(constraint, structuralValue * preferenceStrength + noise);
        }
        return result;
    }

    private int directCandidateEliminations(int size, int[][] grid,
                                            Constraint constraint) {
        Position smaller = constraint.operator() == ConstraintOperator.LESS_THAN
                ? constraint.from() : constraint.to();
        Position larger = constraint.operator() == ConstraintOperator.LESS_THAN
                ? constraint.to() : constraint.from();
        int smallerMask = baseCandidates(size, grid, smaller);
        int largerMask = baseCandidates(size, grid, larger);
        int supportedSmaller = 0;
        int supportedLarger = 0;

        for (int small = 1; small <= size; small++) {
            if ((smallerMask & bit(small)) == 0) continue;
            for (int large = small + 1; large <= size; large++) {
                if ((largerMask & bit(large)) == 0) continue;
                supportedSmaller |= bit(small);
                supportedLarger |= bit(large);
            }
        }
        return Integer.bitCount(smallerMask) + Integer.bitCount(largerMask)
                - Integer.bitCount(supportedSmaller) - Integer.bitCount(supportedLarger);
    }

    private int baseCandidates(int size, int[][] grid, Position position) {
        int row = position.row() - 1;
        int col = position.col() - 1;
        if (grid[row][col] != 0) return bit(grid[row][col]);

        int mask = (1 << size) - 1;
        for (int offset = 0; offset < size; offset++) {
            if (grid[row][offset] != 0) mask &= ~bit(grid[row][offset]);
            if (grid[offset][col] != 0) mask &= ~bit(grid[offset][col]);
        }
        return mask;
    }

    private ChainMetrics calculateChainMetrics(int size, List<Constraint> constraints) {
        Map<Position, List<Position>> outgoing = new HashMap<>();
        Map<Position, List<Position>> incoming = new HashMap<>();
        Map<Position, Integer> degree = new HashMap<>();

        for (Constraint constraint : constraints) {
            Position smaller = constraint.operator() == ConstraintOperator.LESS_THAN
                    ? constraint.from() : constraint.to();
            Position larger = constraint.operator() == ConstraintOperator.LESS_THAN
                    ? constraint.to() : constraint.from();
            outgoing.computeIfAbsent(smaller, ignored -> new ArrayList<>()).add(larger);
            incoming.computeIfAbsent(larger, ignored -> new ArrayList<>()).add(smaller);
            degree.merge(smaller, 1, Integer::sum);
            degree.merge(larger, 1, Integer::sum);
        }

        Map<Position, Integer> forwardMemo = new HashMap<>();
        Map<Position, Integer> backwardMemo = new HashMap<>();
        Map<Constraint, Integer> lengthByConstraint = new HashMap<>();
        for (Constraint constraint : constraints) {
            Position smaller = constraint.operator() == ConstraintOperator.LESS_THAN
                    ? constraint.from() : constraint.to();
            Position larger = constraint.operator() == ConstraintOperator.LESS_THAN
                    ? constraint.to() : constraint.from();
            int before = longestPath(smaller, incoming, backwardMemo);
            int after = longestPath(larger, outgoing, forwardMemo);
            lengthByConstraint.put(constraint, Math.min(size, before + after));
        }
        return new ChainMetrics(lengthByConstraint, degree);
    }

    private int longestPath(Position position, Map<Position, List<Position>> adjacency,
                            Map<Position, Integer> memo) {
        Integer cached = memo.get(position);
        if (cached != null) return cached;
        int longest = 1;
        for (Position neighbour : adjacency.getOrDefault(position, List.of())) {
            longest = Math.max(longest, 1 + longestPath(neighbour, adjacency, memo));
        }
        memo.put(position, longest);
        return longest;
    }

    private int bit(int value) {
        return 1 << (value - 1);
    }

    private List<Constraint> removeObviousRedundancies(int[][] grid,
                                                       List<Constraint> source) {
        List<Constraint> result = source.stream()
                .filter(constraint -> isHidden(grid, constraint.from())
                        || isHidden(grid, constraint.to()))
                .collect(java.util.stream.Collectors.toCollection(ArrayList::new));
        Map<HiddenBound, Constraint> strongest = new HashMap<>();
        Set<Constraint> hiddenToGiven = new HashSet<>();

        for (Constraint constraint : result) {
            boolean fromHidden = isHidden(grid, constraint.from());
            boolean toHidden = isHidden(grid, constraint.to());
            if (fromHidden == toHidden) continue;

            Position hidden = fromHidden ? constraint.from() : constraint.to();
            Position given = fromHidden ? constraint.to() : constraint.from();
            ConstraintOperator relativeToHidden = fromHidden
                    ? constraint.operator() : reverse(constraint.operator());
            HiddenBound key = new HiddenBound(hidden, relativeToHidden);
            Constraint current = strongest.get(key);
            if (current == null || isStronger(grid, constraint, current, relativeToHidden)) {
                strongest.put(key, constraint);
            }
            hiddenToGiven.add(constraint);
        }

        result.removeIf(constraint -> hiddenToGiven.contains(constraint)
                && !strongest.containsValue(constraint));
        return result;
    }

    private boolean isStronger(int[][] grid, Constraint candidate, Constraint current,
                               ConstraintOperator relativeToHidden) {
        int candidateGiven = givenValue(grid, candidate);
        int currentGiven = givenValue(grid, current);
        return relativeToHidden == ConstraintOperator.LESS_THAN
                ? candidateGiven < currentGiven : candidateGiven > currentGiven;
    }

    private int givenValue(int[][] grid, Constraint constraint) {
        Position position = isHidden(grid, constraint.from()) ? constraint.to() : constraint.from();
        return grid[position.row() - 1][position.col() - 1];
    }

    private ConstraintOperator reverse(ConstraintOperator operator) {
        return operator == ConstraintOperator.LESS_THAN
                ? ConstraintOperator.GREATER_THAN : ConstraintOperator.LESS_THAN;
    }

    private boolean isHidden(int[][] grid, Position position) {
        return grid[position.row() - 1][position.col() - 1] == 0;
    }

    private int countGivens(int[][] grid) {
        int count = 0;
        for (int[] row : grid) for (int value : row) if (value != 0) count++;
        return count;
    }

    private int[][] generateSolution(int size) {
        int[][] grid = new int[size][size];
        List<Integer> rowOrder = shuffledRange(size);
        List<Integer> columnOrder = shuffledRange(size);
        List<Integer> symbols = shuffledRange(size);

        for (int row = 0; row < size; row++) {
            for (int col = 0; col < size; col++) {
                int baseValue = (rowOrder.get(row) + columnOrder.get(col)) % size;
                grid[row][col] = symbols.get(baseValue) + 1;
            }
        }
        return grid;
    }

    private List<Integer> shuffledRange(int size) {
        List<Integer> result = new ArrayList<>(size);
        for (int value = 0; value < size; value++) result.add(value);
        Collections.shuffle(result, random);
        return result;
    }

    private List<Constraint> allConstraints(int[][] solution) {
        int size = solution.length;
        List<Constraint> result = new ArrayList<>(2 * size * (size - 1));
        for (int row = 0; row < size; row++) {
            for (int col = 0; col < size; col++) {
                if (col + 1 < size) result.add(constraint(solution, row, col, row, col + 1));
                if (row + 1 < size) result.add(constraint(solution, row, col, row + 1, col));
            }
        }
        return result;
    }

    private Constraint constraint(int[][] solution, int fromRow, int fromCol,
                                  int toRow, int toCol) {
        ConstraintOperator operator = solution[fromRow][fromCol] < solution[toRow][toCol]
                ? ConstraintOperator.LESS_THAN : ConstraintOperator.GREATER_THAN;
        return new Constraint(new Position(fromRow + 1, fromCol + 1), operator,
                new Position(toRow + 1, toCol + 1));
    }

    private List<Position> allPositions(int size) {
        List<Position> result = new ArrayList<>(size * size);
        for (int row = 1; row <= size; row++) {
            for (int col = 1; col <= size; col++) result.add(new Position(row, col));
        }
        return result;
    }

    private int[][] copy(int[][] source) {
        int[][] result = new int[source.length][];
        for (int row = 0; row < source.length; row++) result[row] = source[row].clone();
        return result;
    }

    private record Candidate(int[][] solution, int[][] grid, List<Constraint> constraints,
                             int quality, boolean matchesTarget) {}

    private record DifficultyRange(HumanDifficulty min, HumanDifficulty max,
                                   double basicAcceptanceChance) {}

    private record HiddenBound(Position position, ConstraintOperator operator) {}

    private record ChainMetrics(Map<Constraint, Integer> lengthByConstraint,
                                Map<Position, Integer> degreeByPosition) {}
}
