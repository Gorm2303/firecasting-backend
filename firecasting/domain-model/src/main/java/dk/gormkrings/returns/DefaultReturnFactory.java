package dk.gormkrings.returns;

import dk.gormkrings.distribution.BrownianMotionDistribution;
import dk.gormkrings.distribution.NormalDistribution;
import dk.gormkrings.distribution.TDistributionImpl;
import dk.gormkrings.distribution.factory.DistributionFactory;
import dk.gormkrings.math.distribution.IDistribution;
import dk.gormkrings.math.randomNumberGenerator.IRandomNumberGenerator;
import dk.gormkrings.randomNumberGenerator.DefaultRandomNumberGenerator;
import dk.gormkrings.randomVariable.DefaultRandomVariable;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

@Slf4j
@Component("defaultReturnFactory")
public class DefaultReturnFactory implements IReturnFactory {
    private final ApplicationContext context;
    private final DistributionFactory distributionFactory;

    @Autowired
    public DefaultReturnFactory(ApplicationContext context, DistributionFactory distributionFactory) {
        this.context = context;
        this.distributionFactory = distributionFactory;
    }

    @Override
    public IReturner createReturn(String returner) {
        IReturner returnerClass = switch (returner) {
            case "distributionReturn" -> context.getBean(DistributionReturn.class);
            case "dataDrivenReturn" -> context.getBean(DataDrivenReturn.class);
            default -> context.getBean(SimpleDailyReturn.class);
        };

        log.info("Creating new {} returner", returner.toUpperCase());
        return returnerClass;
    }

    @Override
    public IReturner createReturn(String returnType, ReturnerConfig config) {
        if (config == null) {
            return createReturn(returnType);
        }

        return switch (returnType) {
            case "distributionReturn" -> createConfiguredDistributionReturn(config);
            case "dataDrivenReturn" -> createReturn(returnType);
            default -> createConfiguredSimpleReturn(config);
        };
    }

    private IReturner createConfiguredSimpleReturn(ReturnerConfig config) {
        SimpleDailyReturn r = context.getBean(SimpleDailyReturn.class);
        Double pct = config.getSimpleAveragePercentage();
        if (pct != null) {
            r.setAveragePercentage((float) (pct / 100.0));
        }
        return r;
    }

    private IReturner createConfiguredDistributionReturn(ReturnerConfig config) {
        IDistribution distribution = createConfiguredDistribution(config.getDistribution());
        IRandomNumberGenerator rng = (config.getSeed() == null)
                ? new DefaultRandomNumberGenerator()
                : new DefaultRandomNumberGenerator(config.getSeed());

        DefaultRandomVariable rv = new DefaultRandomVariable(distribution, rng);
        return new DistributionReturn(rv);
    }

    private IDistribution createConfiguredDistribution(ReturnerConfig.DistributionConfig config) {
        if (config == null || config.getType() == null || config.getType().isBlank()) {
            return distributionFactory.createDistribution("normal");
        }

        String beanKey = switch (config.getType()) {
            case "studentT" -> "tDistribution";
            default -> config.getType();
        };

        IDistribution dist = distributionFactory.createDistribution(beanKey);

        if (dist instanceof NormalDistribution normal && config.getNormal() != null) {
            if (config.getNormal().getMean() != null) normal.setMean(config.getNormal().getMean());
            if (config.getNormal().getStandardDeviation() != null) {
                normal.setStandardDeviation(config.getNormal().getStandardDeviation());
            }
        }

        if (dist instanceof BrownianMotionDistribution brownian && config.getBrownianMotion() != null) {
            if (config.getBrownianMotion().getDrift() != null) brownian.setDrift(config.getBrownianMotion().getDrift());
            if (config.getBrownianMotion().getVolatility() != null) {
                brownian.setVolatility(config.getBrownianMotion().getVolatility());
            }
        }

        if (dist instanceof TDistributionImpl tDist && config.getStudentT() != null) {
            if (config.getStudentT().getMu() != null) tDist.setMu(config.getStudentT().getMu());
            if (config.getStudentT().getSigma() != null) tDist.setSigma(config.getStudentT().getSigma());
            if (config.getStudentT().getNu() != null) {
                tDist.setNu(config.getStudentT().getNu());
                tDist.init();
            }
        }

        return dist;
    }
}
