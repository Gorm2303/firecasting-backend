package dk.gormkrings.simulation.util;

import dk.gormkrings.result.IResult;
import dk.gormkrings.result.ISnapshot;
import dk.gormkrings.simulation.result.Result;
import dk.gormkrings.simulation.result.Snapshot;
import lombok.extern.slf4j.Slf4j;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

@Slf4j
public class CsvExporter {

    public static void exportResultsToCsv(List<IResult> results, String fileName) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(fileName))) {
            String header = "day, month, year, date, capital, deposited, passive, returned, return, withdrawn, withdraw, taxed, tax, inflation, nettotal, net";
            writer.write(header);
            writer.newLine();

            String format = "%-7s %-7s %-7s %-12" +
                    "s %-12s %-12s %-12s %-12s %-12s %-12s %-12s %-12s %-12s %-12s %-12s %-12s";

            log.info(String.format(format, "day", "month", "year", "date", "capital", "deposited", "passive", "returned", "return", "withdrawn", "withdraw", "taxed", "tax", "inflation", "nettotal", "net"));

            results.getFirst().getSnapshots().forEach(snapshot -> {
                String[] tokens = snapshot.toCsvRow().split(",");
                log.info(String.format(format, (Object[]) tokens));
            });

            for (IResult result : results) {
                for (ISnapshot snapshot : result.getSnapshots()) {
                    writer.write(snapshot.toCsvRow());
                    writer.newLine();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

