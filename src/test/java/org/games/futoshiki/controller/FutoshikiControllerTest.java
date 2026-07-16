package org.games.futoshiki.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.games.futoshiki.dto.ActiveGameDto;
import org.games.futoshiki.dto.CheckSolutionRequest;
import org.games.futoshiki.dto.FutoshikiBoardDto;
import org.games.futoshiki.exception.BoardLoadingException;
import org.games.futoshiki.exception.GameNotFoundException;
import org.games.futoshiki.exception.InvalidSolutionException;
import org.games.futoshiki.model.Difficulty;
import org.games.futoshiki.model.FutoshikiBoard;
import org.games.futoshiki.provider.ProviderStrategy;
import org.games.futoshiki.provider.generator.FutoshikiBoardGenerator;
import org.games.futoshiki.service.FutoshikiService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(FutoshikiController.class)
class FutoshikiControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private FutoshikiService futoshikiService;

    @Autowired
    private ObjectMapper objectMapper;

    private final FutoshikiBoardGenerator generator = new FutoshikiBoardGenerator();

    @Test
    void newGame_shouldReturnActiveGame_defaultProviderStrategy() throws Exception {
        FutoshikiBoard board = generator.generate(4, Difficulty.EASY);
        UUID gameId = UUID.randomUUID();
        ActiveGameDto gameDto = new ActiveGameDto(gameId, FutoshikiBoardDto.toDto(board));

        when(futoshikiService.newGame(4, Difficulty.EASY, ProviderStrategy.GENERATOR)).thenReturn(gameDto);

        mockMvc.perform(post("/futoshiki/new-game/4/easy"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.gameId").value(gameId.toString()));
    }

    @Test
    void newGame_shouldReturnActiveGame_fileProviderStrategy() throws Exception {
        FutoshikiBoard board = generator.generate(4, Difficulty.EASY);
        UUID gameId = UUID.randomUUID();
        ActiveGameDto gameDto = new ActiveGameDto(gameId, FutoshikiBoardDto.toDto(board));

        when(futoshikiService.newGame(4, Difficulty.EASY, ProviderStrategy.FILE)).thenReturn(gameDto);

        mockMvc.perform(post("/futoshiki/new-game/4/easy?strategy=file"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.gameId").value(gameId.toString()));
    }

    @Test
    void newGame_shouldReturnInternalServerError_boardCannotBeLoaded() throws Exception {
        when(futoshikiService.newGame(4, Difficulty.EASY, ProviderStrategy.FILE))
                .thenThrow(new BoardLoadingException("Unable to load puzzle after 5 attempts."));

        mockMvc.perform(post("/futoshiki/new-game/4/easy?strategy=file"))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string("Unable to load puzzle after 5 attempts."));
    }

    @Test
    void newGame_shouldReturnBadRequest_sizeNotInRange() throws Exception {
        mockMvc.perform(post("/futoshiki/new-game/3/easy?strategy=file"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(futoshikiService);
    }

    @Test
    void newGame_shouldReturnBadRequest_difficultyNotValid() throws Exception {
        mockMvc.perform(post("/futoshiki/new-game/4/extreme?strategy=file"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(futoshikiService);
    }

    @Test
    void newGame_shouldReturnBadRequest_providerNotValid() throws Exception {
        mockMvc.perform(post("/futoshiki/new-game/4/medium?strategy=random"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(futoshikiService);
    }

    @Test
    void checkSolution_shouldReturnSuccessResponse() throws Exception {
        int[][] solution = new int[][]{{1, 2, 3}, {4, 5, 6}, {7, 8, 9}};
        CheckSolutionRequest request = new CheckSolutionRequest(solution);
        UUID gameId = UUID.randomUUID();
        when(futoshikiService.checkSolution(
                eq(gameId), argThat(actual -> Arrays.deepEquals(actual, solution)))).thenReturn(true);

        mockMvc.perform(post("/futoshiki/{id}/check-solution", gameId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isCorrect").value(true));
    }

    @Test
    void checkSolution_shouldReturnBadRequest_incorrectRequestSize() throws Exception {
        int[][] solution = new int[][]{{1, 2, 3}, {4, 5, 6}, {7, 8, 9}};
        CheckSolutionRequest request = new CheckSolutionRequest(solution);
        UUID gameId = UUID.randomUUID();
        when(futoshikiService.checkSolution(eq(gameId), argThat(actual -> Arrays.deepEquals(actual, solution))))
                .thenThrow(new InvalidSolutionException("Invalid solution size"));

        mockMvc.perform(post("/futoshiki/{id}/check-solution", gameId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void checkSolution_shouldReturnNotFound_gameNotFound() throws Exception {
        int[][] solution = new int[][]{{1, 2, 3}, {4, 5, 6}, {7, 8, 9}};
        CheckSolutionRequest request = new CheckSolutionRequest(solution);
        UUID gameId = UUID.randomUUID();
        when(futoshikiService.checkSolution(eq(gameId), argThat(actual -> Arrays.deepEquals(actual, solution))))
                .thenThrow(new GameNotFoundException(gameId));

        mockMvc.perform(post("/futoshiki/{id}/check-solution", gameId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    void checkSolution_shouldReturnBadRequest_invalidRequestBody() throws Exception {
        mockMvc.perform(post("/futoshiki/{id}/check-solution", UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {"solution": [["abc"]]}
                            """))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(futoshikiService);
    }

    @Test
    void checkSolution_shouldReturnBadRequest_nullBody() throws Exception {
        mockMvc.perform(post("/futoshiki/{id}/check-solution", UUID.randomUUID()))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(futoshikiService);
    }

    @Test
    void checkSolution_shouldReturnBadRequest_nullSolution() throws Exception {
        CheckSolutionRequest request = new CheckSolutionRequest(null);

        mockMvc.perform(post("/futoshiki/{id}/check-solution", UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(futoshikiService);
    }

}