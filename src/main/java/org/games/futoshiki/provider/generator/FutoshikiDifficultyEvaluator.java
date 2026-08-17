package org.games.futoshiki.provider.generator;

import org.games.futoshiki.model.Constraint;
import org.games.futoshiki.model.ConstraintOperator;
import org.games.futoshiki.model.Position;

import java.util.List;

/**
 * Candidate based solver deliberately limited to deductions that are useful
 * on paper. It never guesses and is therefore suitable for grading puzzles.
 */
final class FutoshikiDifficultyEvaluator {

    record Result(boolean solved, boolean contradiction, HumanDifficulty hardest,
                  int deductionRounds, int eliminations) {
        int score() {
            return hardest.rank() * 100 + deductionRounds * 3 + eliminations;
        }
    }

    private final int size;
    private final int fullMask;
    private final int[][] candidates;
    private final List<Constraint> constraints;
    private boolean contradiction;
    private HumanDifficulty hardest = HumanDifficulty.BASIC;
    private int eliminations;

    FutoshikiDifficultyEvaluator(int size, int[][] grid, List<Constraint> constraints) {
        this.size = size;
        this.fullMask = (1 << size) - 1;
        this.candidates = new int[size][size];
        this.constraints = constraints;

        for (int row = 0; row < size; row++) {
            for (int col = 0; col < size; col++) {
                int value = grid[row][col];
                candidates[row][col] = value == 0 ? fullMask : bit(value);
            }
        }
    }

    Result solve(HumanDifficulty maximumTechnique) {
        int rounds = 0;
        boolean changed;

        do {
            rounds++;
            int before = eliminations;
            eliminateSolvedValuesFromUnits();
            propagateInequalities();
            validateUnits();

            if (!contradiction && maximumTechnique.rank() >= HumanDifficulty.HIDDEN_SINGLE.rank()) {
                applyHiddenSingles();
            }
            if (!contradiction && maximumTechnique.rank() >= HumanDifficulty.NAKED_PAIR.rank()) {
                applyNakedSubsets(2, HumanDifficulty.NAKED_PAIR);
            }
            if (!contradiction && maximumTechnique.rank() >= HumanDifficulty.HIDDEN_PAIR.rank()) {
                applyHiddenSubsets(2, HumanDifficulty.HIDDEN_PAIR);
            }
            if (!contradiction && maximumTechnique.rank() >= HumanDifficulty.NAKED_TRIPLE.rank()) {
                applyNakedSubsets(3, HumanDifficulty.NAKED_TRIPLE);
            }
            if (!contradiction && maximumTechnique.rank() >= HumanDifficulty.HIDDEN_TRIPLE.rank()) {
                applyHiddenSubsets(3, HumanDifficulty.HIDDEN_TRIPLE);
            }

            changed = eliminations != before;
        } while (changed && !contradiction && rounds <= size * size * 3);

        return new Result(isSolved(), contradiction, hardest, rounds, eliminations);
    }

    private void eliminateSolvedValuesFromUnits() {
        for (int row = 0; row < size; row++) {
            int solved = singletonMaskInRow(row);
            for (int col = 0; col < size; col++) {
                if (!isSingle(candidates[row][col])) {
                    restrictWithNakedSingleDetection(row, col, candidates[row][col] & ~solved);
                }
            }
        }

        for (int col = 0; col < size; col++) {
            int solved = singletonMaskInColumn(col);
            for (int row = 0; row < size; row++) {
                if (!isSingle(candidates[row][col])) {
                    restrictWithNakedSingleDetection(row, col, candidates[row][col] & ~solved);
                }
            }
        }
    }

    /** Arc consistency also captures direct bounds and inequality chains. */
    private void propagateInequalities() {
        boolean changed;
        do {
            int before = eliminations;
            for (Constraint constraint : constraints) {
                Position smaller = constraint.operator() == ConstraintOperator.LESS_THAN
                        ? constraint.from() : constraint.to();
                Position larger = constraint.operator() == ConstraintOperator.LESS_THAN
                        ? constraint.to() : constraint.from();

                int smallRow = smaller.row() - 1;
                int smallCol = smaller.col() - 1;
                int largeRow = larger.row() - 1;
                int largeCol = larger.col() - 1;
                int smallMask = candidates[smallRow][smallCol];
                int largeMask = candidates[largeRow][largeCol];
                int supportedSmall = 0;
                int supportedLarge = 0;

                for (int small = 1; small <= size; small++) {
                    if ((smallMask & bit(small)) == 0) continue;
                    for (int large = small + 1; large <= size; large++) {
                        if ((largeMask & bit(large)) != 0) {
                            supportedSmall |= bit(small);
                            supportedLarge |= bit(large);
                        }
                    }
                }
                restrict(smallRow, smallCol, smallMask & supportedSmall);
                restrict(largeRow, largeCol, largeMask & supportedLarge);
            }
            changed = eliminations != before;
        } while (changed && !contradiction);
    }

    private void applyHiddenSingles() {
        for (int unit = 0; unit < size; unit++) {
            for (int value = 1; value <= size; value++) {
                int valueBit = bit(value);
                int rowPlace = -1;
                int rowCount = 0;
                int columnPlace = -1;
                int columnCount = 0;

                for (int offset = 0; offset < size; offset++) {
                    if ((candidates[unit][offset] & valueBit) != 0) {
                        rowPlace = offset;
                        rowCount++;
                    }
                    if ((candidates[offset][unit] & valueBit) != 0) {
                        columnPlace = offset;
                        columnCount++;
                    }
                }

                if (rowCount == 1 && candidates[unit][rowPlace] != valueBit) {
                    mark(HumanDifficulty.HIDDEN_SINGLE);
                    restrict(unit, rowPlace, valueBit);
                }
                if (columnCount == 1 && candidates[columnPlace][unit] != valueBit) {
                    mark(HumanDifficulty.HIDDEN_SINGLE);
                    restrict(columnPlace, unit, valueBit);
                }
            }
        }
    }

    private void applyNakedSubsets(int subsetSize, HumanDifficulty technique) {
        for (int unit = 0; unit < size; unit++) {
            applyNakedSubsetsToUnit(unit, true, subsetSize, technique);
            applyNakedSubsetsToUnit(unit, false, subsetSize, technique);
        }
    }

    private void applyNakedSubsetsToUnit(int unit, boolean rowUnit, int subsetSize,
                                         HumanDifficulty technique) {
        int combinations = 1 << size;
        for (int selected = 0; selected < combinations; selected++) {
            if (Integer.bitCount(selected) != subsetSize) continue;
            int values = 0;
            for (int offset = 0; offset < size; offset++) {
                if ((selected & (1 << offset)) != 0) values |= maskAt(unit, offset, rowUnit);
            }
            if (Integer.bitCount(values) != subsetSize) continue;

            for (int other = 0; other < size; other++) {
                if ((selected & (1 << other)) != 0) continue;
                int row = rowUnit ? unit : other;
                int col = rowUnit ? other : unit;
                int reduced = candidates[row][col] & ~values;
                if (reduced != candidates[row][col]) {
                    mark(technique);
                    restrict(row, col, reduced);
                }
            }
        }
    }

    private void applyHiddenSubsets(int subsetSize, HumanDifficulty technique) {
        for (int unit = 0; unit < size; unit++) {
            applyHiddenSubsetsToUnit(unit, true, subsetSize, technique);
            applyHiddenSubsetsToUnit(unit, false, subsetSize, technique);
        }
    }

    private void applyHiddenSubsetsToUnit(int unit, boolean rowUnit, int subsetSize,
                                          HumanDifficulty technique) {
        for (int values = 1; values <= fullMask; values++) {
            if (Integer.bitCount(values) != subsetSize) continue;
            int locations = 0;
            boolean everyValuePresent = true;
            for (int offset = 0; offset < size; offset++) {
                if ((maskAt(unit, offset, rowUnit) & values) != 0) locations |= 1 << offset;
            }
            for (int value = 1; value <= size; value++) {
                int valueBit = bit(value);
                if ((values & valueBit) == 0) continue;
                boolean present = false;
                for (int offset = 0; offset < size; offset++) {
                    if ((maskAt(unit, offset, rowUnit) & valueBit) != 0) present = true;
                }
                if (!present) everyValuePresent = false;
            }
            if (!everyValuePresent) continue;
            if (Integer.bitCount(locations) != subsetSize) continue;

            for (int offset = 0; offset < size; offset++) {
                if ((locations & (1 << offset)) == 0) continue;
                int row = rowUnit ? unit : offset;
                int col = rowUnit ? offset : unit;
                int reduced = candidates[row][col] & values;
                if (reduced != candidates[row][col]) {
                    mark(technique);
                    restrict(row, col, reduced);
                }
            }
        }
    }

    private void mark(HumanDifficulty technique) {
        if (technique.rank() > hardest.rank()) hardest = technique;
    }

    private void validateUnits() {
        for (int unit = 0; unit < size; unit++) {
            int rowUnion = 0;
            int columnUnion = 0;
            int rowSingles = 0;
            int columnSingles = 0;

            for (int offset = 0; offset < size; offset++) {
                int rowMask = candidates[unit][offset];
                int columnMask = candidates[offset][unit];
                rowUnion |= rowMask;
                columnUnion |= columnMask;
                if (isSingle(rowMask)) {
                    if ((rowSingles & rowMask) != 0) contradiction = true;
                    rowSingles |= rowMask;
                }
                if (isSingle(columnMask)) {
                    if ((columnSingles & columnMask) != 0) contradiction = true;
                    columnSingles |= columnMask;
                }
            }
            if (rowUnion != fullMask || columnUnion != fullMask) contradiction = true;
        }
    }

    private int singletonMaskInRow(int row) {
        int result = 0;
        for (int col = 0; col < size; col++) {
            if (isSingle(candidates[row][col])) result |= candidates[row][col];
        }
        return result;
    }

    private int singletonMaskInColumn(int col) {
        int result = 0;
        for (int row = 0; row < size; row++) {
            if (isSingle(candidates[row][col])) result |= candidates[row][col];
        }
        return result;
    }

    private void restrict(int row, int col, int newMask) {
        int oldMask = candidates[row][col];
        if (newMask == oldMask) return;
        eliminations += Integer.bitCount(oldMask) - Integer.bitCount(newMask);
        candidates[row][col] = newMask;
        if (newMask == 0) contradiction = true;
    }

    private void restrictWithNakedSingleDetection(int row, int col, int newMask) {
        int oldMask = candidates[row][col];
        if (!isSingle(oldMask) && isSingle(newMask)) {
            mark(HumanDifficulty.NAKED_SINGLE);
        }
        restrict(row, col, newMask);
    }

    private boolean isSolved() {
        if (contradiction) return false;
        for (int[] row : candidates) {
            for (int mask : row) {
                if (!isSingle(mask)) return false;
            }
        }
        validateUnits();
        return !contradiction;
    }

    private int maskAt(int unit, int offset, boolean rowUnit) {
        return rowUnit ? candidates[unit][offset] : candidates[offset][unit];
    }

    private int bit(int value) {
        return 1 << (value - 1);
    }

    private boolean isSingle(int mask) {
        return mask != 0 && (mask & (mask - 1)) == 0;
    }
}
