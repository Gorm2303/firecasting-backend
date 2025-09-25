package dk.gormkrings.statistics.mapper;

import dk.gormkrings.statistics.YearlySummary;
import dk.gormkrings.statistics.persistence.YearlySummaryEntity; // your JPA entity
import dk.gormkrings.statistics.persistence.SimulationRunEntity;

public interface YearlySummaryMapper {

    public static YearlySummary toDto(YearlySummaryEntity e) {
        YearlySummary d = new YearlySummary();
        d.setPhaseName(e.getPhaseName());
        d.setYear(e.getYear());
        d.setAverageCapital(e.getAverageCapital());
        d.setMedianCapital(e.getMedianCapital());
        d.setMinCapital(e.getMinCapital());
        d.setMaxCapital(e.getMaxCapital());
        d.setStdDevCapital(e.getStdDevCapital());
        d.setCumulativeGrowthRate(e.getCumulativeGrowthRate());
        d.setQuantile5(e.getQuantile5());
        d.setQuantile25(e.getQuantile25());
        d.setQuantile75(e.getQuantile75());
        d.setQuantile95(e.getQuantile95());
        d.setVar(e.getVar());
        d.setCvar(e.getCvar());
        d.setNegativeCapitalPercentage(e.getNegativeCapitalPercentage());
        // Note: your DTO doesn't have the 101-point grid; expose it via another DTO/endpoint if needed.
        return d;
    }

    /** Optional helper if you sometimes build entities from DTO + grid */
    public static YearlySummaryEntity toEntity(
            YearlySummary d, SimulationRunEntity run, Double[] percentilesGrid) {
        YearlySummaryEntity e = new YearlySummaryEntity();
        e.setRun(run);
        e.setPhaseName(d.getPhaseName());
        e.setYear(d.getYear());
        e.setAverageCapital(d.getAverageCapital());
        e.setMedianCapital(d.getMedianCapital());
        e.setMinCapital(d.getMinCapital());
        e.setMaxCapital(d.getMaxCapital());
        e.setStdDevCapital(d.getStdDevCapital());
        e.setCumulativeGrowthRate(d.getCumulativeGrowthRate());
        e.setQuantile5(d.getQuantile5());
        e.setQuantile25(d.getQuantile25());
        e.setQuantile75(d.getQuantile75());
        e.setQuantile95(d.getQuantile95());
        e.setVar(d.getVar());
        e.setCvar(d.getCvar());
        e.setNegativeCapitalPercentage(d.getNegativeCapitalPercentage());
        e.setPercentiles(percentilesGrid);
        return e;
    }
}
