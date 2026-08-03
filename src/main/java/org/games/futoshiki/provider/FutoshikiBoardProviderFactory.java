package org.games.futoshiki.provider;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class FutoshikiBoardProviderFactory {

    private final JsonFutoshikiBoardProvider jsonBoardProvider;
    private final GeneratedFutoshikiBoardProvider generatedBoardProvider;
    private final AiFutoshikiBoardProvider aiFutoshikiBoardProvider;
    private final FcApiFutoshikiBoardProvider fcApiFutoshikiBoardProvider;

    public FutoshikiBoardProvider getProvider(ProviderStrategy strategy) {
        return switch (strategy) {
            case FILE -> jsonBoardProvider;
            case GENERATOR -> generatedBoardProvider;
            case AI -> aiFutoshikiBoardProvider;
            case FUTOSHIKI_COM_API -> fcApiFutoshikiBoardProvider;
        };
    }

}