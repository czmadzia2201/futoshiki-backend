package org.games.futoshiki.provider;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.games.futoshiki.client.fcapi.FcApiClient;
import org.games.futoshiki.exception.BoardLoadingException;
import org.games.futoshiki.exception.BoardParsingException;
import org.games.futoshiki.exception.BoardValidationException;
import org.games.futoshiki.model.Difficulty;
import org.games.futoshiki.model.FutoshikiBoard;
import org.games.futoshiki.provider.parser.FcApiParser;
import org.games.futoshiki.provider.validator.FutoshikiBoardValidator;
import org.springframework.stereotype.Component;

import java.util.Random;

@Slf4j
@Component
@RequiredArgsConstructor
public class FcApiFutoshikiBoardProvider implements FutoshikiBoardProvider {

    private final Random random = new Random();

    private final FcApiClient fcApiClient;
    private final FcApiParser fcApiParser;
    private final FutoshikiBoardValidator validator;

    private static final int MAX_PUZZLE_ID = 9999;

    @Override
    public FutoshikiBoard getBoard(int size, Difficulty difficulty) {
        try {
            int puzzleId = random.nextInt(MAX_PUZZLE_ID + 1);
            String rawPuzzle = fcApiClient.sendBoardRequest(size, difficulty, puzzleId);
            FutoshikiBoard board = fcApiParser.parseBoard(rawPuzzle, size, difficulty);
            validator.validate(board, size);
            return board;
        } catch (IllegalStateException | BoardParsingException | BoardValidationException e) {
            log.error("Cannot get board from Futoshiki.com", e);
            throw new BoardLoadingException("Cannot get board from Futoshiki.com");
        }
    }

}
