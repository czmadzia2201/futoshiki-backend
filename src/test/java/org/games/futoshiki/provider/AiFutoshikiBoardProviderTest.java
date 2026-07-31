package org.games.futoshiki.provider;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.games.futoshiki.client.OpenAiClient;
import org.games.futoshiki.exception.BoardValidationException;
import org.games.futoshiki.model.Difficulty;
import org.games.futoshiki.model.FutoshikiBoard;
import org.games.futoshiki.provider.validator.FutoshikiBoardValidator;
import org.games.futoshiki.util.TestUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AiFutoshikiBoardProviderTest {

    @Mock
    private OpenAiClient openAiClient;

    private AiFutoshikiBoardProvider provider;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        provider = new AiFutoshikiBoardProvider(
                openAiClient,
                new FutoshikiBoardValidator()
        );
    }

    @Test
    void shouldProvideBoardCorrectly() throws Exception {
        FutoshikiBoard board = TestUtils.loadBoardFromJson(objectMapper, "test-board-h4_001.json");
        when(openAiClient.sendGenerateRequest(4, Difficulty.HARD)).thenReturn(board);
        FutoshikiBoard result = provider.getBoard(4, Difficulty.HARD);
        assertThat(result).usingRecursiveComparison().isEqualTo(board);
    }

    @Test
    void shouldThrowWhenBoardValidationFails() throws Exception {
        FutoshikiBoard board = TestUtils.loadBoardFromJson(objectMapper, "invalid-board_grid-inconsistent-with-solution.json");
        when(openAiClient.sendGenerateRequest(4, Difficulty.HARD)).thenReturn(board);
        assertThatThrownBy(() -> provider.getBoard(4, Difficulty.HARD))
                .isInstanceOf(BoardValidationException.class)
                .hasMessageContaining("AI generated an invalid board");
    }

}