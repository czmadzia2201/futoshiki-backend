package org.games.futoshiki.provider;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.games.futoshiki.client.OpenAiClient;
import org.games.futoshiki.exception.BoardValidationException;
import org.games.futoshiki.model.Difficulty;
import org.games.futoshiki.model.FutoshikiBoard;
import org.games.futoshiki.provider.validator.FutoshikiBoardValidator;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class AiFutoshikiBoardProvider implements FutoshikiBoardProvider {

    private final OpenAiClient openAiClient;
    private final FutoshikiBoardValidator validator;

    @Override
    public FutoshikiBoard getBoard(int size, Difficulty difficulty) {
        FutoshikiBoard board = openAiClient.sendGenerateRequest(size, difficulty);
        try {
            validator.validate(board, size);
        } catch (BoardValidationException e) {
            log.error("AI generated an invalid board", e);
            throw new BoardValidationException("AI generated an invalid board");
        }
        return board;
    }

}
