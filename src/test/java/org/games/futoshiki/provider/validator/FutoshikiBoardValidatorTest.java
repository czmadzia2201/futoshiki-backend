package org.games.futoshiki.provider.validator;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.games.futoshiki.exception.BoardValidationException;
import org.games.futoshiki.model.FutoshikiBoard;
import org.games.futoshiki.util.TestUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class FutoshikiBoardValidatorTest {

    private final FutoshikiBoardValidator validator = new FutoshikiBoardValidator();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void shouldAcceptValidBoard() throws Exception {
        FutoshikiBoard board = TestUtils.loadBoardFromJson(objectMapper, "test-board-h4_001.json");
        assertDoesNotThrow(() -> validator.validate(board, 4));
    }

    @Test
    void shouldThrowWhenBoardIsNull() {
        FutoshikiBoard board = null;
        assertThatThrownBy(() -> validator.validate(board, 4))
                .isInstanceOf(BoardValidationException.class)
                .hasMessage("Board is null");
    }

    @Test
    void shouldThrowWhenSizeDoesNotMatchExpected() throws Exception {
        FutoshikiBoard board = TestUtils.loadBoardFromJson(objectMapper, "test-board-h4_001.json");
        assertThatThrownBy(() -> validator.validate(board, 5))
                .isInstanceOf(BoardValidationException.class)
                .hasMessage("Board size does not match expected size");
    }

    @ParameterizedTest
    @MethodSource("invalidCases")
    void shouldThrowForInvalidBoard(String inputJson, String exceptionMsg) throws Exception {
        FutoshikiBoard board = TestUtils.loadBoardFromJson(objectMapper, inputJson);
        assertThatThrownBy(() -> validator.validate(board, 4))
                .isInstanceOf(BoardValidationException.class)
                .hasMessageContaining(exceptionMsg);
    }

    private static Stream<Arguments> invalidCases() {
        return Stream.of(
                Arguments.of(
                        "invalid-board_null-solution.json",
                        "Solution is null"),
                Arguments.of(
                        "invalid-board_solution-with-too-few-rows.json",
                        "Solution row count does not match board size"),
                Arguments.of(
                        "invalid-board_solution-with-too-short-cols.json",
                        "Solution col count does not match board size"),
                Arguments.of(
                        "invalid-board_solution-invalid-row1.json",
                        "Invalid values at row"),
                Arguments.of(
                        "invalid-board_solution-invalid-row2.json",
                        "Invalid values at row"),
                Arguments.of(
                        "invalid-board_solution-invalid-col.json",
                        "Invalid values at col"),
                Arguments.of(
                        "invalid-board_null-grid.json",
                        "Grid is null"),
                Arguments.of(
                        "invalid-board_grid-with-too-few-rows.json",
                        "Grid row count does not match board size"),
                Arguments.of(
                        "invalid-board_grid-with-too-short-cols.json",
                        "Grid col count does not match board size"),
                Arguments.of(
                        "invalid-board_grid-inconsistent-with-solution.json",
                        "Grid and solution inconsistent"),
                Arguments.of(
                        "invalid-board_null-constraints.json",
                        "Constraint list is null"),
                Arguments.of(
                        "invalid-board_null-constraint.json",
                        "One or more constraints is null"),
                Arguments.of(
                        "invalid-board_constraint-with-null-values.json",
                        "Constraint contains null values"),
                Arguments.of(
                        "invalid-board_constraint-positions-out-of-bounds.json",
                        "Constraint positions are out of bounds"),
                Arguments.of(
                        "invalid-board_constraint-positions-incorrect.json",
                        "Constraint positions are not correct"),
                Arguments.of(
                        "invalid-board_constraint-operator-incorrect.json",
                        "Constraint violated between positions"),
                Arguments.of(
                        "invalid-board_solution-not-unique.json",
                        "Board does not have exactly one solution")
        );
    }

}