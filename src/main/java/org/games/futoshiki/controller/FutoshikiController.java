package org.games.futoshiki.controller;

import lombok.RequiredArgsConstructor;
import org.games.futoshiki.dto.ActiveGameDto;
import org.games.futoshiki.dto.CheckSolutionRequest;
import org.games.futoshiki.dto.CheckSolutionResponse;
import org.games.futoshiki.model.Difficulty;
import org.games.futoshiki.provider.ProviderStrategy;
import org.games.futoshiki.service.FutoshikiService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Validated
@RestController
@RequestMapping("/futoshiki")
@RequiredArgsConstructor
public class FutoshikiController {

    private final FutoshikiService futoshikiService;

    @PostMapping("/new-game/{size}/{difficulty}")
    public ActiveGameDto newGame(
            @PathVariable int size,
            @PathVariable Difficulty difficulty,
            @RequestParam(defaultValue = "generator") ProviderStrategy strategy
    ) {
        return futoshikiService.newGame(size, difficulty, strategy);
    }

    @PostMapping("/{id}/check-solution")
    public CheckSolutionResponse checkSolution(
            @PathVariable UUID id,
            @RequestBody CheckSolutionRequest request) {
        boolean isCorrect = futoshikiService.checkSolution(id, request.solution());
        return new CheckSolutionResponse(isCorrect);
    }
}
