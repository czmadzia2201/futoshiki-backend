package org.games.futoshiki.model;

import lombok.Getter;

@Getter
public enum Difficulty {
    EASY('e'),
    MEDIUM('m'),
    HARD('h');

    private char symbol;

    Difficulty(char symbol) {
        this.symbol = symbol;
    }

}
