package dk.gormkrings.inflation;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.io.*;

@Slf4j
@Getter
public class DataAverageInflation implements IInflation {
    private double averagePercentage;

    public DataAverageInflation() {
        this("/dk/gormkrings/inflation/inflation.csv");
    }

    private DataAverageInflation(double averagePercentage) {
        this.averagePercentage = averagePercentage;
    }

    public DataAverageInflation(String filename) {
        setAverageInflation(filename);
        log.debug("Initializing DataAverageInflation: {}", averagePercentage);
    }

    @Override
    public double calculatePercentage() {
        return averagePercentage;
    }

    @Override
    public IInflation copy() {
        return new DataAverageInflation(this.averagePercentage);
    }

    private void setAverageInflation(String csvFilePath) {
        double sum = 0.0;
        int count = 0;
        String line;
        String delimiter = ",";

        InputStream is = getClass().getResourceAsStream(csvFilePath);
        if (is == null) {
            log.error("Resource not found: {}", csvFilePath);
            return;
        }

        try (BufferedReader br = new BufferedReader(new InputStreamReader(is))) {
            br.readLine();// header skipped
            br.readLine();// header skipped
            while ((line = br.readLine()) != null) {
                String[] parts = line.split(delimiter);
                if (parts.length >= 2) {
                    String inflationStr = parts[1].trim();
                    try {
                        double inflation = Double.parseDouble(inflationStr);
                        sum += inflation;
                        count++;
                    } catch (NumberFormatException e) {
                        log.warn("Skipping invalid inflation value: {}", inflationStr);
                    }
                }
            }
        } catch (IOException e) {
            log.error("Error reading CSV file", e);
        }

        averagePercentage = count > 0 ? sum / count : 0;
    }
}
