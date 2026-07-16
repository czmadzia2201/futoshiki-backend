package org.games.futoshiki.dto;

import org.games.futoshiki.model.ActiveGame;

import java.util.UUID;

public record ActiveGameDto(UUID gameId, FutoshikiBoardDto board) {

    public static ActiveGameDto toDto(ActiveGame activeGame) {
        return new ActiveGameDto(
                activeGame.gameId(),
                FutoshikiBoardDto.toDto(activeGame.board())
        );
    }
}
