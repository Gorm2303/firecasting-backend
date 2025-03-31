package dk.gormkrings.simulation.util;

import dk.gormkrings.result.IResult;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class ConcurrentCsvExporter {

    public static File exportCsv(List<IResult> resultsList, String finalFileName) {
        File csvFile = new File(finalFileName + ".csv");

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
            List<IResult> partition = resultsList.subList(startIndex, Math.min(endIndex, totalResults));
            String tmpFileName = finalFileName + "_part_" + (i + 1) + ".csv";
            fileNames.add(tmpFileName);

            futures.add(executor.submit(() -> CsvExporter.exportResultsToCsv(partition, tmpFileName)));
            startIndex = endIndex;
        }

        for (Future<?> future : futures) {
            try {
                future.get();
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
            }
        }

        try {
            FileMerger.mergeAndCleanup(fileNames, csvFile.getAbsolutePath());
        } catch (IOException e) {

        }
        return csvFile;
    }
}