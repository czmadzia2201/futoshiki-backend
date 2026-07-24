package org.games.futoshiki.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.games.futoshiki.dto.ActiveGameDto;
import org.games.futoshiki.dto.SolutionDto;
import org.games.futoshiki.dto.SolutionValidationDto;
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
            @PathVariable @Min(4) @Max(9) int size,
            @PathVariable Difficulty difficulty,
            @RequestParam(defaultValue = "generator") ProviderStrategy strategy
    ) {
        return futoshikiService.newGame(size, difficulty, strategy);
    }

    @PostMapping("/{id}/check-solution")
    public SolutionValidationDto checkSolution(
            @PathVariable UUID id,
            @Valid @RequestBody SolutionDto request) {
        boolean isCorrect = futoshikiService.checkSolution(id, request.solution());
        return new SolutionValidationDto(isCorrect);
    }

    @GetMapping("/{id}/show-solution")
    public SolutionDto showSolution(@PathVariable UUID id) {
        return futoshikiService.showSolution(id);
    }
}
