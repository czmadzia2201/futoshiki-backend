package org.games.futoshiki.converter;

import org.games.futoshiki.provider.ProviderStrategy;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

@Component
public class ProviderStrategyConverter implements Converter<String, ProviderStrategy> {
    @Override
    public ProviderStrategy convert(String source) {
        return ProviderStrategy.valueOf(source.toUpperCase());
    }
}
