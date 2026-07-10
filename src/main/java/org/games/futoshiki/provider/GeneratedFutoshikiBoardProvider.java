package org.games.futoshiki.provider;

import lombok.RequiredArgsConstructor;
import org.games.futoshiki.model.Difficulty;
import org.games.futoshiki.model.FutoshikiBoard;
import org.games.futoshiki.provider.generator.FutoshikiBoardGenerator;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class GeneratedFutoshikiBoardProvider implements FutoshikiBoardProvider {

    private final FutoshikiBoardGenerator generator;

    @Override
    public FutoshikiBoard getBoard(int size, Difficulty difficulty) {
        return generator.generate(size, difficulty);
    }

}
