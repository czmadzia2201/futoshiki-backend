package org.games.futoshiki.provider.parser;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.games.futoshiki.exception.BoardParsingException;
import org.games.futoshiki.model.Difficulty;
import org.games.futoshiki.model.FutoshikiBoard;
import org.games.futoshiki.util.TestUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FcApiParserTest {

    private final FcApiParser parser = new FcApiParser();
    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final String FCAPI_4_EASY_1 = "2_._._._______._.(._.______v3_._._.v______._._.(32_3_1_4_______4_1(3_2______v3_2_4_1v______1_4_2(3";
    private static final String FCAPI_5_MEDIUM_1 = "._._._.).________v._._._._.__^_^____.)._._._.____v___v._._._.(.__^______._._._._.1_5_2_4)3________v4_1_3_5_2__^_^____3)2_4_1_5____v___v5_3_1_2(4__^______2_4_5_3_1";
    private static final String FCAPI_6_HARD_1 = "2_4_._._._.____v_v___v.(._._._._2___________._.(._._._.____^______._2_._4_._.__v________._._4_._._.___________._._.(._.(.2_4_3_6_1_5____v_v___v4(6_1_5_3_2___________6_3(5_1_2_4____^______3_2_6_4_5_1__v________5_1_4_2_6_3___________1_5_2(3_4(6";

    @ParameterizedTest
    @MethodSource("validCases")
    void shouldCorrectlyParseValidRawBoards(String jsonBoard, String rawBoard, int size, Difficulty difficulty) throws Exception {
        FutoshikiBoard board = TestUtils.loadBoardFromJson(objectMapper, jsonBoard);
        FutoshikiBoard result = parser.parseBoard(rawBoard, size, difficulty);
        assertThat(result).usingRecursiveComparison().isEqualTo(board);
    }

    private static Stream<Arguments> validCases() {
        return Stream.of(
                Arguments.of("fcapi-board-e4_1.json", FCAPI_4_EASY_1, 4, Difficulty.EASY),
                Arguments.of("fcapi-board-m5_1.json", FCAPI_5_MEDIUM_1, 5, Difficulty.MEDIUM),
                Arguments.of("fcapi-board-h6_1.json", FCAPI_6_HARD_1, 6, Difficulty.HARD)
        );
    }

    @Test
    void shouldThrowWhenRawBoardIsNull() {
        String rawBoard = null;
        assertThatThrownBy(() -> parser.parseBoard(rawBoard, 4, Difficulty.EASY))
                .isInstanceOf(BoardParsingException.class)
                .hasMessage("Raw board is null");
    }

    @Test
    void shouldThrowWhenRawBoardHasInvalidLength() {
        String rawBoard = FCAPI_4_EASY_1.substring(0, FCAPI_4_EASY_1.length() - 5);
        assertThatThrownBy(() -> parser.parseBoard(rawBoard, 4, Difficulty.EASY))
                .isInstanceOf(BoardParsingException.class)
                .hasMessage("Invalid board length");
    }

    @Test
    void shouldThrowWhenRawBoardHasInvalidCharacters_gridPart() {
        String rawBoard = FCAPI_4_EASY_1.replaceFirst("2_", "X_");
        assertThatThrownBy(() -> parser.parseBoard(rawBoard, 4, Difficulty.EASY))
                .isInstanceOf(BoardParsingException.class)
                .hasMessage("Non-numeric cell value in grid");
    }

    @Test
    void shouldThrowWhenRawBoardHasInvalidCharacters_solutionPart() {
        String rawBoard = FCAPI_4_EASY_1.replaceFirst("2_3_1_4", "2_3_a_4");
        assertThatThrownBy(() -> parser.parseBoard(rawBoard, 4, Difficulty.EASY))
                .isInstanceOf(BoardParsingException.class)
                .hasMessage("Non-numeric cell value in solution");
    }
}