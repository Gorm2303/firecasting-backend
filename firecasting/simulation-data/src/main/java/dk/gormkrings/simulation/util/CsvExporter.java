package dk.gormkrings.simulation.util;

import dk.gormkrings.result.IResult;
import dk.gormkrings.result.ISnapshot;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

public class CsvExporter {

    public static File exportResultsToCsv(List<IResult> results, String fileName) {
        File csvFile = new File(fileName);
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(csvFile))) {
            String header = "day, month, year, date, capital, deposited, passive, returned, return, withdrawn, withdraw, taxed, tax, inflation, nettotal, net";
            writer.write(header);
            writer.newLine();

            for (IResult result : results) {
                for (ISnapshot snapshot : result.getSnapshots()) {
                    writer.write(snapshot.toCsvRow());
                    writer.newLine();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return csvFile;
    }
}

