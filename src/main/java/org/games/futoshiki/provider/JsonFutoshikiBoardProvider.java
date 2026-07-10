package org.games.futoshiki.provider;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.games.futoshiki.model.Difficulty;
import org.games.futoshiki.model.FutoshikiBoard;
import org.springframework.stereotype.Component;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
@RequiredArgsConstructor
public class JsonFutoshikiBoardProvider implements FutoshikiBoardProvider {

    private final ObjectMapper objectMapper;

    private final Map<String, Queue<Integer>> puzzleIdsMap = new ConcurrentHashMap<>();

    private final Random random = new Random();

    private static final int FILE_COUNT = 15;
    private static final int BLOCKED_COUNT = 5;
    private static final int MAX_ATTEMPTS = 5;

    @Override
    public FutoshikiBoard getBoard(int size, Difficulty difficulty) {
        String mapKey = String.format("%c%d", difficulty.getSymbol(), size);

        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            try {
                int puzzleNumber = providePuzzleNumber(mapKey);
                String fileName = String.format("%s_%03d.json", mapKey, puzzleNumber);
                FutoshikiBoard board = generatePuzzleFromFile(fileName);
                updatePuzzleIdValues(mapKey, puzzleNumber);
                return board;
            } catch (IOException e) {
                log.warn("Failed to load puzzle file on attempt {}.", attempt, e);
            }
        }
        throw new IllegalStateException("Unable to load puzzle after " + MAX_ATTEMPTS + " attempts.");
    }

    private int providePuzzleNumber(String mapKey) {
        Queue<Integer> excluded = puzzleIdsMap.computeIfAbsent(mapKey, k -> new LinkedList<>());
        int puzzleNumber;
        do {
            puzzleNumber = random.nextInt(FILE_COUNT) + 1;
        } while (excluded.contains(puzzleNumber));
        return puzzleNumber;
    }

    private FutoshikiBoard generatePuzzleFromFile(String fileName) throws IOException {
        URL resource = this.getClass().getClassLoader().getResource("boards/" + fileName);
        if (resource == null) {
            throw new FileNotFoundException("Puzzle file not found: " + fileName);
        }
        return objectMapper.readValue(resource, FutoshikiBoard.class);
    }

    private void updatePuzzleIdValues(String mapKey, int drawnNo) {
        Queue<Integer> puzzleIds = puzzleIdsMap.get(mapKey);
        if (puzzleIds.size() >= BLOCKED_COUNT) {
            puzzleIds.remove();
        }
        puzzleIds.add(drawnNo);
    }

}
