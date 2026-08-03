package org.games.futoshiki.client.openai;

import org.games.futoshiki.model.Difficulty;
import org.springframework.stereotype.Component;

@Component
public class PromptBuilder {

    public String futoshikiPrompt(int size, Difficulty difficulty) {
        return """
                Generate a FutoshikiBoard representing a valid Futoshiki puzzle.
                
                Requirements:
                - The puzzle must have exactly one valid solution.
                - Avoid redundant clues and unnecessary constraints.
                - The puzzle should be aesthetically pleasing and resemble a puzzle created by a human.
                
                Field semantics:
                - puzzleId: null
                - size: %d
                - difficulty: %s
                - solution: The solution represents the completely solved puzzle. It must have size %d x %d and contain only numbers from 1 to %d. The solution must form a valid Latin square.
                - grid: The grid represents the puzzle before it is solved. Empty cells must contain 0. Every non-zero value must match the corresponding value in the solution.
                - constraints: Each constraint represents an inequality between two adjacent cells. Every constraint must be satisfied by the solution and must be consistent with it. The from and to Positions use 1-based indexing.
                
                Difficulty description:
                EASY: The puzzle should be solvable using straightforward deductions.
                MEDIUM: The puzzle should require combining multiple logical deductions.
                HARD: The puzzle should require advanced logical reasoning and should not be solvable by obvious single-step deductions.
            """
            .formatted(size, difficulty, size, size, size);
    }

}