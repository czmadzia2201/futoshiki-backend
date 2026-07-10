package org.games.futoshiki.provider;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class FutoshikiBoardProviderFactory {

    private final JsonFutoshikiBoardProvider jsonBoardProvider;
    private final GeneratedFutoshikiBoardProvider generatedBoardProvider;

    public FutoshikiBoardProvider getProvider(ProviderStrategy strategy) {
        return switch (strategy) {
            case FILE -> jsonBoardProvider;
            case GENERATOR -> generatedBoardProvider;
            case AI -> throw new UnsupportedOperationException(
                    "AI provider is not implemented"
            );
        };
    }

}