package dk.gormkrings.statistics.mapper;

import dk.gormkrings.statistics.MetricSummary;
import dk.gormkrings.statistics.persistence.MetricSummaryEntity;
import dk.gormkrings.statistics.persistence.SimulationRunEntity;

public final class MetricSummaryMapper {

    private MetricSummaryMapper() {
    }

    public static MetricSummary toDto(MetricSummaryEntity e) {
        if (e == null) return null;
        MetricSummary d = new MetricSummary();
        d.setScope(e.getScope());
        d.setPhaseName(e.getPhaseName());
        d.setYear(e.getYear());
        d.setMetric(e.getMetric());
        d.setP5(e.getP5());
        d.setP10(e.getP10());
        d.setP25(e.getP25());
        d.setP50(e.getP50());
        d.setP75(e.getP75());
        d.setP90(e.getP90());
        d.setP95(e.getP95());
        return d;
    }

    public static MetricSummaryEntity toEntity(MetricSummary d, SimulationRunEntity runRef) {
        if (d == null) return null;
        MetricSummaryEntity e = new MetricSummaryEntity();
        e.setRun(runRef);
        e.setScope(d.getScope());
        e.setPhaseName(d.getPhaseName());
        e.setYear(d.getYear());
        e.setMetric(d.getMetric());
        e.setP5(d.getP5());
        e.setP10(d.getP10());
        e.setP25(d.getP25());
        e.setP50(d.getP50());
        e.setP75(d.getP75());
        e.setP90(d.getP90());
        e.setP95(d.getP95());
        return e;
    }
}
