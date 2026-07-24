package org.games.futoshiki.service;

import lombok.RequiredArgsConstructor;
import org.games.futoshiki.dto.ActiveGameDto;
import org.games.futoshiki.dto.SolutionDto;
import org.games.futoshiki.exception.InvalidSolutionException;
import org.games.futoshiki.model.ActiveGame;
import org.games.futoshiki.model.Difficulty;
import org.games.futoshiki.model.FutoshikiBoard;
import org.games.futoshiki.provider.FutoshikiBoardProvider;
import org.games.futoshiki.provider.FutoshikiBoardProviderFactory;
import org.games.futoshiki.provider.ProviderStrategy;
import org.games.futoshiki.repository.ActiveGameStore;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FutoshikiService {

    private final ActiveGameStore activeGameStore;

    private final FutoshikiBoardProviderFactory providerFactory;

    public ActiveGameDto newGame(int size, Difficulty difficulty, ProviderStrategy strategy) {
        validateSize(size);
        FutoshikiBoardProvider provider = providerFactory.getProvider(strategy);
        FutoshikiBoard board = provider.getBoard(size, difficulty);
        ActiveGame activeGame = activeGameStore.createNewGame(board);
        return ActiveGameDto.toDto(activeGame);
    }

    public boolean checkSolution(UUID gameId, int[][] solution) {
        ActiveGame activeGame = activeGameStore.get(gameId);
        validateSolution(activeGame.board().size(), solution);
        int[][] expectedSolution = activeGame.board().solution();
        return Arrays.deepEquals(solution, expectedSolution);
    }

    public SolutionDto showSolution(UUID gameId) {
        ActiveGame activeGame = activeGameStore.get(gameId);
        return new SolutionDto(activeGame.board().solution());
    }

    private void validateSize(int size) {
        if (size < 4 || size > 9) {
            throw new IllegalArgumentException(
                    "Board size must be between 4 and 9"
            );
        }
    }

    private void validateSolution(int size, int[][] solution) {
        if (Arrays.stream(solution).anyMatch(Objects::isNull)) {
            throw new InvalidSolutionException("Solution cannot contain null values");
        }

        if (solution.length != size || Arrays.stream(solution).anyMatch(i -> i.length != size)) {
            throw new InvalidSolutionException("Solution must have size: %d x %d".formatted(size, size));
        }

        if (Arrays.stream(solution).flatMapToInt(Arrays::stream).anyMatch(i -> i < 1 || i > size)) {
            throw new InvalidSolutionException("All values in the solution must be between 1 and %d".formatted(size));
        }
    }

}
