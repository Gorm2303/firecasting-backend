package dk.gormkrings.statistics;

import lombok.Data;

@Data
public class YearlySummary {
    private String phaseName;
    private int year;
    private double averageCapital;
    private double medianCapital;
    private double minCapital;
    private double maxCapital;
    private double stdDevCapital;
    private double cumulativeGrowthRate;
    private double quantile5;
    private double quantile25;
    private double quantile75;
    private double quantile95;
    private double var;
    private double cvar;
    private double negativeCapitalPercentage;
}
