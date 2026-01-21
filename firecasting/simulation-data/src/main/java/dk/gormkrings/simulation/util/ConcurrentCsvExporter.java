package dk.gormkrings.simulation.util;

import dk.gormkrings.result.IRunResult;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public class ConcurrentCsvExporter {

    public static File exportCsv(List<IRunResult> resultsList, String finalFileName) throws IOException {
        Path tempDir = Files.createTempDirectory("firecasting-csv-");
        File csvFile = tempDir.resolve(finalFileName + ".csv").toFile();

        int numThreads = Runtime.getRuntime().availableProcessors();
        ExecutorService executor = Executors.newFixedThreadPool(numThreads);
        List<Future<?>> futures = new ArrayList<>();
        List<String> fileNames = new ArrayList<>();


        int totalResults = resultsList.size();
        int partitionSize = totalResults / numThreads;
        int remaining = totalResults % numThreads;
        int startIndex = 0;
        for (int i = 0; i < numThreads; i++) {
            // Extra index in case of the list size not dividing evenly with the number of threads
            int extra = (i < remaining ? 1 : 0);
            int endIndex = startIndex + partitionSize + extra;
            if (startIndex >= totalResults) break; // No more results to partition.
            List<IRunResult> partition = resultsList.subList(startIndex, Math.min(endIndex, totalResults));
            String tmpFileName = tempDir.resolve(finalFileName + "_part_" + (i + 1) + ".csv").toString();
            fileNames.add(tmpFileName);

            futures.add(executor.submit(() -> {
                CsvExporter.exportResultsToCsv(partition, tmpFileName);
                return null;
            }));
            startIndex = endIndex;
        }

        try {
            for (Future<?> future : futures) {
                future.get();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("CSV export interrupted", e);
        } catch (ExecutionException e) {
            throw new IOException("CSV export failed", e);
        } finally {
            executor.shutdown();
            try {
                executor.awaitTermination(30, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        FileMerger.mergeAndCleanup(fileNames, csvFile.getAbsolutePath());
        return csvFile;
    }
}