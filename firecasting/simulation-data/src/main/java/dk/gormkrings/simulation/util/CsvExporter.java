package dk.gormkrings.simulation.util;

import dk.gormkrings.result.IRunResult;
import dk.gormkrings.result.ISnapshot;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

public class CsvExporter {

    public static File exportResultsToCsv(List<IRunResult> results, String fileName) {
        File csvFile = new File(fileName);
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(csvFile))) {
            String header = "phase, day, month, year, date, capital, deposited, passive, returned, return, withdrawn, withdraw, taxed, tax, inflation, nettotal, net" +
                    ", y-return, y-withdraw, y-tax, y-net";
            writer.write(header);
            writer.newLine();

            ISnapshot previousSnapshot = null;
            for (IRunResult result : results) {
                for (ISnapshot snapshot : result.getSnapshots()) {
                    writer.write(snapshot.toCsvRow());
                    if (previousSnapshot != null) {
                        writer.write(addYearlyValues(snapshot, previousSnapshot));
                    }
                    writer.newLine();
                    previousSnapshot = snapshot;
                }
                previousSnapshot = null;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return csvFile;
    }

    private static String addYearlyValues(ISnapshot snapshot, ISnapshot previousSnapshot) {
        double yearReturn = snapshot.getState().getReturned() - previousSnapshot.getState().getReturned();
        double yearWithdraw = snapshot.getState().getWithdrawn() - previousSnapshot.getState().getWithdrawn();
        double yearTax = snapshot.getState().getTax() - previousSnapshot.getState().getTax();
        double yearNet = snapshot.getState().getNet() - previousSnapshot.getState().getNet();

        return  "," +
                Formatter.numberToString(yearReturn) + "," +
                Formatter.numberToString(yearWithdraw) + "," +
                Formatter.numberToString(yearTax) + "," +
                Formatter.numberToString(yearNet);
    }
}

