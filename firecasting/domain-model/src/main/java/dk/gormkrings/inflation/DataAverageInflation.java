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
    private double averageInflation;

    private final String csvFilePath = "/dk/gormkrings/inflation/inflation.csv";

    public DataAverageInflation() {
    }

    public DataAverageInflation(double averageInflation) {
        this.averageInflation = averageInflation;
    }

    public DataAverageInflation(String filename) {
        setAverageInflation(filename);
        log.debug("Initializing DataAverageInflation: {}", averageInflation);
    }

    @PostConstruct
    public void init() {
        setAverageInflation(csvFilePath);
    }

    @Override
    public double calculateInflation() {
        return averageInflation;
    }

    @Override
    public IInflation copy() {
        return new DataAverageInflation(this.averageInflation);
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

        averageInflation = count > 0 ? (float) (1 + sum / count / 100) : 1;
    }
}
