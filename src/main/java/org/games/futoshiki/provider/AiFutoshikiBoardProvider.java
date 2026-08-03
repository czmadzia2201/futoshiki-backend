package org.games.futoshiki.provider;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.games.futoshiki.client.openai.OpenAiClient;
import org.games.futoshiki.exception.BoardLoadingException;
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
        try {
            FutoshikiBoard board = openAiClient.sendBoardRequest(size, difficulty);
            validator.validate(board, size);
            return board;
        } catch (IllegalStateException | BoardValidationException e) {
            log.error("Cannot get board from OpenAI", e);
            throw new BoardLoadingException("Cannot get board from OpenAI");
        }
    }

}
