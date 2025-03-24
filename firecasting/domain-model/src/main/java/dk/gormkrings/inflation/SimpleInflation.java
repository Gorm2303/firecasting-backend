package dk.gormkrings.inflation;

import dk.gormkrings.event.Type;
import dk.gormkrings.event.date.YearEvent;
import lombok.Getter;
import lombok.NonNull;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.event.SmartApplicationListener;

import java.io.*;

@Getter
public class SimpleInflation implements Inflation, SmartApplicationListener {
    private double averagePercentage;

    public SimpleInflation() {
        setAverageInflation("/dk/gormkrings/inflation/inflation.csv");
        System.out.println("Initializing SimpleInflation = " + this.averagePercentage);
    }

    private SimpleInflation(double averagePercentage) {
        this.averagePercentage = averagePercentage;
    }

    public SimpleInflation(String filename) {
        System.out.println("Initializing SimpleInflation");
        setAverageInflation(filename);
    }

    @Override
    public double calculateInflation(double amount) {
        return averagePercentage*amount/100;
    }

    @Override
    public void onApplicationEvent(@NonNull ApplicationEvent event) {
        YearEvent yearEvent = (YearEvent) event;
        if (yearEvent.getType() != Type.END) return;

        long day = yearEvent.getData().getSessionDuration();
        System.out.println("Year " + (day / 365) + ": SimpleInflation calculating inflation.");
        // Implement your tax calculation logic here
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
            System.err.println("Resource not found: " + csvFilePath);
            return;
        }

        try (BufferedReader br = new BufferedReader(new InputStreamReader(is))) {
            while ((line = br.readLine()) != null) {
                String[] parts = line.split(delimiter);
                if (parts.length >= 2) {
                    String inflationStr = parts[1].trim();
                    try {
                        double inflation = Double.parseDouble(inflationStr);
                        sum += inflation;
                        count++;
                    } catch (NumberFormatException e) {
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        averagePercentage = count > 0 ? sum / count : 0;
    }
}
