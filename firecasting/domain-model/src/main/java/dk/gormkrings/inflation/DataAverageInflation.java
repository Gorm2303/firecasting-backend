package dk.gormkrings.inflation;

import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Value;

import java.io.*;

@Slf4j
@Getter
@Component
public class DataAverageInflation implements IInflation {
    private double averagePercentage;

    @Value("${inflation.csv-file-path}")
    private String csvFilePath;

    public DataAverageInflation() {
    }

    public DataAverageInflation(double averagePercentage) {
        this.averagePercentage = averagePercentage;
    }

    public DataAverageInflation(String filename) {
        setAverageInflation(filename);
        log.debug("Initializing DataAverageInflation: {}", averagePercentage);
    }

    @PostConstruct
    public void init() {
        setAverageInflation(csvFilePath);
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

        averagePercentage = count > 0 ? (float) (sum / count) : 0;
    }
}
