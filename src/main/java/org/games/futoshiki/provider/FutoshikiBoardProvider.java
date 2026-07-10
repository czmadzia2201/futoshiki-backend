package org.games.futoshiki.provider;

import org.games.futoshiki.model.Difficulty;
import org.games.futoshiki.model.FutoshikiBoard;

public interface FutoshikiBoardProvider {
    FutoshikiBoard getBoard(int size, Difficulty difficulty);
}