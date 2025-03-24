package dk.gormkrings.inflation;

import dk.gormkrings.data.LiveData;
import dk.gormkrings.event.Type;
import dk.gormkrings.event.date.YearEvent;
import lombok.Getter;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEvent;

import java.io.*;

@Slf4j
@Getter
public class SimpleInflation implements Inflation {
    private double averagePercentage;

    public SimpleInflation() {
        setAverageInflation("/dk/gormkrings/inflation/inflation.csv");
        log.debug("Initializing SimpleInflation = {}", this.averagePercentage);
    }

    private SimpleInflation(double averagePercentage) {
        this.averagePercentage = averagePercentage;
    }

    public SimpleInflation(String filename) {
        setAverageInflation(filename);
        log.debug("Initializing SimpleInflation = {}", this.averagePercentage);
    }

    @Override
    public double calculatePercentage() {
        return averagePercentage;
    }

    @Override
    public void onApplicationEvent(@NonNull ApplicationEvent event) {
        YearEvent yearEvent = (YearEvent) event;
        if (yearEvent.getType() != Type.END) return;

        log.debug("Year " + (yearEvent.getData().getSessionDuration() / 365) + ": SimpleInflation calculating inflation.");

        LiveData data = (LiveData) yearEvent.getData();
        data.addToInflation(calculatePercentage());
    }

    @Override
    public boolean supportsEventType(@NonNull Class<? extends ApplicationEvent> eventType) {
        return YearEvent.class.isAssignableFrom(eventType);
    }

    @Override
    public boolean supportsSourceType(Class<?> sourceType) {
        return true;
    }


    @Override
    public Inflation copy() {
        return new SimpleInflation(this.averagePercentage);
    }

    private void setAverageInflation(String csvFilePath) {
        double sum = 0.0;
        int count = 0;
        String line;
        String delimiter = ",";

        InputStream is = getClass().getResourceAsStream(csvFilePath);
        if (is == null) {
            log.error("Resource not found: " + csvFilePath);
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
