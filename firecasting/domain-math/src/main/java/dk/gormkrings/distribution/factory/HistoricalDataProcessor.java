package dk.gormkrings.distribution.factory;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

@Component
@Slf4j
public class HistoricalDataProcessor {

    public List<Double> computeLogReturns(String csvFilePath) {
        List<Double> closingPrices = new ArrayList<>();
        List<Double> logReturns = new ArrayList<>();
        String line;
        String delimiter = ",";

        InputStream is = getClass().getResourceAsStream(csvFilePath);
        if (is == null) {
            log.error("Resource not found: {}", csvFilePath);
            return null;
        }

        try (BufferedReader br = new BufferedReader(new InputStreamReader(is))) {
            // Skip header if exists
            String header = br.readLine();
            while ((line = br.readLine()) != null) {
                String[] tokens = line.split(delimiter);
                // Assuming the closing price is the second column
                double price = Double.parseDouble(tokens[1]);
                closingPrices.add(price);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Compute log returns: log(price_t/price_{t-1})
        for (int i = 1; i < closingPrices.size(); i++) {
            double today = closingPrices.get(i);
            double yesterday = closingPrices.get(i - 1);
            double logReturn = Math.log(today / yesterday);
            logReturns.add(logReturn);
        }
        return logReturns;
    }
}
