package org.games.futoshiki.model;

import java.time.Instant;
import java.util.UUID;

public record ActiveGame(UUID gameId, FutoshikiBoard board, Instant createdAt) {

    public static ActiveGame create(FutoshikiBoard board) {
        return new ActiveGame(UUID.randomUUID(), board, Instant.now());
    }
}
