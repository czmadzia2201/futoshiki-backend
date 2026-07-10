package org.games.futoshiki.service;

import lombok.RequiredArgsConstructor;
import org.games.futoshiki.dto.ActiveGameDto;
import org.games.futoshiki.model.ActiveGame;
import org.games.futoshiki.model.Difficulty;
import org.games.futoshiki.model.FutoshikiBoard;
import org.games.futoshiki.provider.FutoshikiBoardProvider;
import org.games.futoshiki.provider.FutoshikiBoardProviderFactory;
import org.games.futoshiki.provider.ProviderStrategy;
import org.games.futoshiki.repository.ActiveGameStore;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FutoshikiService {

    private final ActiveGameStore activeGameStore;

    private final FutoshikiBoardProviderFactory providerFactory;

    public ActiveGameDto newGame(int size, Difficulty difficulty, ProviderStrategy strategy) {
        FutoshikiBoardProvider provider = providerFactory.getProvider(strategy);
        FutoshikiBoard board = provider.getBoard(size, difficulty);
        ActiveGame activeGame = activeGameStore.createNewGame(board);
        return ActiveGameDto.toDto(activeGame);
    }

    public boolean checkSolution(UUID gameId, int[][] solution) {
        ActiveGame activeGame = activeGameStore.get(gameId);
        int[][] expectedSolution = activeGame.board().solution();
        return Arrays.deepEquals(solution, expectedSolution);
    }

}
