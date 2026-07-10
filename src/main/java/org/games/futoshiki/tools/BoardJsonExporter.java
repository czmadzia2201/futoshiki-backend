package org.games.futoshiki.tools;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.games.futoshiki.model.Difficulty;
import org.games.futoshiki.model.FutoshikiBoard;
import org.games.futoshiki.provider.generator.FutoshikiBoardGenerator;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
class BoardJsonExporter {

    private static final FutoshikiBoardGenerator generator = new FutoshikiBoardGenerator();

    private static final ObjectWriter objectWriter = new ObjectMapper().writer().withDefaultPrettyPrinter();

    public static void main(String[] args) throws Exception {
        for(int size = 4; size <= 9; size++) {
            for (int fileNo = 6; fileNo <= 15; fileNo++) {
                generateFile(size, Difficulty.EASY, fileNo);
                generateFile(size, Difficulty.MEDIUM, fileNo);
                generateFile(size, Difficulty.HARD, fileNo);
            }
        }
    }

    private static void generateFile(int size, Difficulty difficulty, int fileNo) throws Exception {
        FutoshikiBoard board = generator.generate(size, difficulty);
        String puzzleId = String.format("%s%d_%03d", difficulty.getSymbol(), size, fileNo);
        FutoshikiBoard boardToExport = board.toBuilder().puzzleId(puzzleId).build();
        String json = objectWriter.writeValueAsString(boardToExport);
        Path outputFilePath = Paths.get("src/main/resources/boards/" + puzzleId + ".json");
        Files.write(outputFilePath, json.getBytes(StandardCharsets.UTF_8));
    }

}
