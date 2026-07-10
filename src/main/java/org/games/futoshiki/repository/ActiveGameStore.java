package org.games.futoshiki.repository;

import org.games.futoshiki.exception.GameNotFoundException;
import org.games.futoshiki.model.ActiveGame;
import org.games.futoshiki.model.FutoshikiBoard;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class ActiveGameStore {

    private final Map<UUID, ActiveGame> games = new ConcurrentHashMap<>();

    public ActiveGame createNewGame(FutoshikiBoard board) {
        ActiveGame activeGame = ActiveGame.create(board);
        games.put(activeGame.gameId(), activeGame);
        cleanup();
        return activeGame;
    }

    public ActiveGame get(UUID gameId) {
        ActiveGame game = games.get(gameId);
        if (game == null) {
            throw new GameNotFoundException(gameId);
        }
        return game;
    }

    private void cleanup() {
        Instant cutoff = Instant.now().minus(Duration.ofDays(1));
        games.entrySet().removeIf(entry ->
                entry.getValue().createdAt().isBefore(cutoff)
        );
    }
}
