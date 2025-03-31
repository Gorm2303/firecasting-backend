package dk.gormkrings.simulation.util;

import lombok.extern.slf4j.Slf4j;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

@Slf4j
public class FileMerger {

    public static void mergeAndCleanup(List<String> fileNames, String finalFileName) throws IOException {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(finalFileName))) {
            for (int i = 0; i < fileNames.size(); i++) {
                String fileName = fileNames.get(i);
                List<String> lines = Files.readAllLines(Paths.get(fileName));
                log.debug("Printing lines from each temporary file: {}", fileName);
                debugPrint(lines);
                if (i == 0) {
                    // Write all lines from the first file, including the header.
                    for (String line : lines) {
                        writer.write(line);
                        writer.newLine();
                    }
                } else {
                    // Skip header (first line) for subsequent files.
                    for (int j = 1; j < lines.size(); j++) {
                        writer.write(lines.get(j));
                        writer.newLine();
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            throw new IOException("File merging failed: " + e);
        }

        for (String fileName : fileNames) {
            try {
                Files.deleteIfExists(Paths.get(fileName));
                log.debug("Deleted temporary file: " + fileName);
            } catch (IOException e) {
                System.err.println("Failed to delete temporary file: " + fileName);
                e.printStackTrace();
            }
        }
    }

    private static void debugPrint(List<String> lines) {
        // Expected format for 16 tokens.
        String format = "%-7s %-7s %-7s %-12s %-12s %-12s %-12s %-12s %-12s %-12s %-12s %-12s %-12s %-12s %-12s %-12s";
        // Print up to 5 lines for debugging.
        for (String line : lines.subList(0, Math.min(lines.size(), 5))) {
            String[] tokens = line.split(",");
            if (tokens.length == 16) {
                log.debug(String.format(format, (Object[]) tokens));
            } else {
                log.debug(line);
            }
        }
        log.debug("");
    }
}
