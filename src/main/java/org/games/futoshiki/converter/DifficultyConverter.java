package org.games.futoshiki.converter;

import org.games.futoshiki.model.Difficulty;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

import java.util.Locale;

@Component
public class DifficultyConverter implements Converter<String, Difficulty> {
    @Override
    public Difficulty convert(String source) {
        return Difficulty.valueOf(source.toUpperCase(Locale.ROOT));
    }
}