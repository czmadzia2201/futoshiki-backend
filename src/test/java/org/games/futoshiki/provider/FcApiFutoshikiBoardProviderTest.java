package org.games.futoshiki.provider;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.games.futoshiki.client.fcapi.FcApiClient;
import org.games.futoshiki.exception.BoardLoadingException;
import org.games.futoshiki.model.Difficulty;
import org.games.futoshiki.model.FutoshikiBoard;
import org.games.futoshiki.provider.parser.FcApiParser;
import org.games.futoshiki.provider.validator.FutoshikiBoardValidator;
import org.games.futoshiki.util.TestUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FcApiFutoshikiBoardProviderTest {

    @Mock
    private FcApiClient fcApiClient;

    private FcApiFutoshikiBoardProvider provider;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final String FCAPI_4_EASY_1 = "2_._._._______._.(._.______v3_._._.v______._._.(32_3_1_4_______4_1(3_2______v3_2_4_1v______1_4_2(3";

    @BeforeEach
    void setUp() {
        provider = new FcApiFutoshikiBoardProvider(
                fcApiClient,
                new FcApiParser(),
                new FutoshikiBoardValidator()
        );
    }

    @Test
    void shouldProvideBoardCorrectly() throws Exception {
        FutoshikiBoard board = TestUtils.loadBoardFromJson(objectMapper, "fcapi-board-e4_1.json");
        when(fcApiClient.sendBoardRequest(eq(4), eq(Difficulty.EASY), anyInt())).thenReturn(FCAPI_4_EASY_1);
        FutoshikiBoard result = provider.getBoard(4, Difficulty.EASY);
        assertThat(result).usingRecursiveComparison().isEqualTo(board);
    }

    @Test
    void shouldThrowWhenRawStringParsingFails() {
        when(fcApiClient.sendBoardRequest(eq(4), eq(Difficulty.EASY), anyInt())).thenThrow(new IllegalStateException("Parsing failed"));
        assertThatThrownBy(() -> provider.getBoard(4, Difficulty.EASY))
                .isInstanceOf(BoardLoadingException.class)
                .hasMessage("Cannot get board from Futoshiki.com");
    }
    @Test
    void shouldThrowWhenBoardParsingFails() {
        String fcApiResponse = FCAPI_4_EASY_1 + "1_4_2";
        when(fcApiClient.sendBoardRequest(eq(4), eq(Difficulty.EASY), anyInt())).thenReturn(fcApiResponse);
        assertThatThrownBy(() -> provider.getBoard(4, Difficulty.EASY))
                .isInstanceOf(BoardLoadingException.class)
                .hasMessage("Cannot get board from Futoshiki.com");
    }

    @Test
    void shouldThrowWhenBoardValidationFails() {
        String fcApiResponse = FCAPI_4_EASY_1.replaceFirst("2_", "1_");
        when(fcApiClient.sendBoardRequest(eq(4), eq(Difficulty.EASY), anyInt())).thenReturn(fcApiResponse);
        assertThatThrownBy(() -> provider.getBoard(4, Difficulty.EASY))
                .isInstanceOf(BoardLoadingException.class)
                .hasMessage("Cannot get board from Futoshiki.com");
    }
}