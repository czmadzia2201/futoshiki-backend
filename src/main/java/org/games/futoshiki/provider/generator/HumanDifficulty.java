package org.games.futoshiki.provider.generator;

/**
 * The strongest deduction a human-style solve is allowed to use.
 */
enum HumanDifficulty {
    BASIC(0),
    NAKED_SINGLE(1),
    HIDDEN_SINGLE(2),
    NAKED_PAIR(3),
    HIDDEN_PAIR(4),
    NAKED_TRIPLE(5),
    HIDDEN_TRIPLE(6);

    private final int rank;

    HumanDifficulty(int rank) {
        this.rank = rank;
    }

    int rank() {
        return rank;
    }
}
