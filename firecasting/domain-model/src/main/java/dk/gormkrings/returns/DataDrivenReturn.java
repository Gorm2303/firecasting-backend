package dk.gormkrings.returns;

import dk.gormkrings.distribution.factory.HistoricalDataProcessor;
import dk.gormkrings.math.distribution.IDistributionFitter;
import dk.gormkrings.math.randomNumberGenerator.IRandomNumberGenerator;
import dk.gormkrings.math.randomVariable.IRandomVariable;
import dk.gormkrings.randomVariable.DefaultRandomVariable;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@Scope("prototype")
@Getter
@ConfigurationProperties(prefix = "returner.data-driven")
public class DataDrivenReturn implements IReturner {
    private IRandomVariable randomVariable;

    private HistoricalDataProcessor historicalDataProcessor;
    private IDistributionFitter distributionFitter;
    private IRandomNumberGenerator randomNumberGenerator;
    @Setter
    private double dt;
    @Setter
    private String csvFilePath;

    @Autowired
    public DataDrivenReturn(
            IDistributionFitter fitter,
            HistoricalDataProcessor dataProcessor,
            IRandomNumberGenerator rng) {
        this.historicalDataProcessor = dataProcessor;
        this.distributionFitter = fitter;
        this.randomNumberGenerator = rng;
        this.randomVariable = new DefaultRandomVariable();
    }

    private DataDrivenReturn() {
    }

    @PostConstruct
    public void init() {
        fitDistribution(csvFilePath, historicalDataProcessor, distributionFitter, dt);
        randomVariable.setRandomNumberGenerator(randomNumberGenerator);
        log.info("Data Driven Return - Distribution {}", randomVariable.getDistribution().toString());
    }

    /**
     * Reads the historical CSV, computes log returns, and uses the fitter (with the factory)
     * to create a fitted distribution.
     */
    private void fitDistribution(String csvFilePath, HistoricalDataProcessor dataProcessor, IDistributionFitter fitter, double dt) {
        List<Double> logReturns = dataProcessor.computeLogReturns(csvFilePath);
        randomVariable.setDistribution(fitter.fitNormalDistribution(logReturns, dt));
    }

    /**
     * Samples from the fitted distribution and calculates the return using:
     * return amount * exp(sample) - amount
     */
    @Override
    public double calculateReturn(double amount) {
        double sample = randomVariable.sample();
        return amount * Math.exp(sample) - amount;
    }

    @Override
    public IReturner copy() {
        DataDrivenReturn copy = new DataDrivenReturn();
        copy.randomVariable = this.randomVariable.copy();
        return copy;
    }
}
