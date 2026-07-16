package org.games.futoshiki.repository;

import org.games.futoshiki.exception.GameNotFoundException;
import org.games.futoshiki.model.ActiveGame;
import org.games.futoshiki.model.Difficulty;
import org.games.futoshiki.model.FutoshikiBoard;
import org.games.futoshiki.provider.generator.FutoshikiBoardGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.*;

class ActiveGameStoreTest {

    private final FutoshikiBoardGenerator generator = new FutoshikiBoardGenerator();

    private ActiveGameStore activeGameStore;

    private final UUID gameId1 = UUID.fromString("9fff1a10-b5cc-443b-a5a1-e083ac2bfc86");
    private final UUID gameId2 = UUID.fromString("90a7d758-f9fb-4385-9da4-178ad6d3f490");
    private final UUID gameId3 = UUID.fromString("210ed747-1519-495a-9c49-09ed1c9437b4");
    private final UUID gameId4 = UUID.fromString("c89592fb-5f4e-40cd-a663-92657430e4a3");

    @BeforeEach
    void setUp() {
        activeGameStore = new ActiveGameStore();
        activeGameStore.save(createActiveGame(gameId1, Duration.ofDays(2)));
        activeGameStore.save(createActiveGame(gameId2, Duration.ofDays(3)));
        activeGameStore.save(createActiveGame(gameId3, Duration.ofHours(12)));
    }

    @Test
    void shouldCreateNewGameAndRemoveOldGames() {
        FutoshikiBoard board = generator.generate(4, Difficulty.EASY);
        ActiveGame activeGame = activeGameStore.createNewGame(board);
        assertThrows(GameNotFoundException.class, () -> activeGameStore.get(gameId1));
        assertThrows(GameNotFoundException.class, () -> activeGameStore.get(gameId2));
        assertNotNull(activeGameStore.get(gameId3));
        assertNotNull(activeGame);
        assertNotNull(activeGame.gameId());
        ActiveGame stored = activeGameStore.get(activeGame.gameId());
        assertNotNull(stored);
        assertEquals(board, stored.board());
    }

    @Test
    void shouldThrowExceptionForNonExistingGame() {
        assertThatThrownBy(() -> activeGameStore.get(gameId4))
                .isInstanceOf(GameNotFoundException.class)
                .hasMessage("Game not found: " + gameId4);
    }

    private ActiveGame createActiveGame(UUID gameId, Duration age) {
        FutoshikiBoard board = generator.generate(4, Difficulty.EASY);
        return new ActiveGame(gameId, board, Instant.now().minus(age));
    }

}