package dk.gormkrings.returns;

import dk.gormkrings.distribution.BrownianMotionDistribution;
import dk.gormkrings.distribution.NormalDistribution;
import dk.gormkrings.distribution.RegimeBasedDistribution;
import dk.gormkrings.distribution.TDistributionImpl;
import dk.gormkrings.distribution.factory.DistributionFactory;
import dk.gormkrings.math.distribution.IDistribution;
import dk.gormkrings.math.random.SeedDerivation;
import dk.gormkrings.math.randomNumberGenerator.IRandomNumberGenerator;
import dk.gormkrings.regime.ExpectedDurationWeightedRegimeProvider;
import dk.gormkrings.regime.IRegimeProvider;
import dk.gormkrings.randomNumberGenerator.DefaultRandomNumberGenerator;
import dk.gormkrings.randomVariable.DefaultRandomVariable;
import dk.gormkrings.simulation.ReturnStep;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

@Slf4j
@Component("defaultReturnFactory")
public class DefaultReturnFactory implements IReturnFactory {
    private final ApplicationContext context;
    private final DistributionFactory distributionFactory;
    private final ReturnStep returnStep;

    @Autowired
    public DefaultReturnFactory(
            ApplicationContext context,
            DistributionFactory distributionFactory,
            ReturnStep returnStep
    ) {
        this.context = context;
        this.distributionFactory = distributionFactory;
        this.returnStep = (returnStep == null) ? ReturnStep.DAILY : returnStep;
    }

    private double resolveDt() {
        return returnStep.toDt();
    }

    /**
     * DefaultRandomNumberGenerator treats negative seeds as stochastic/unseeded.
     * Seed derivation can yield negative longs, so force deterministic derived seeds to be non-negative.
     */
    private static long toNonNegativeDeterministicSeed(long seed) {
        return seed & Long.MAX_VALUE;
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
            case "dataDrivenReturn" -> {
                IReturner r = createReturn(returnType);
                if (r instanceof DataDrivenReturn dd && config.getSeed() != null) {
                    Long masterSeed = config.getSeed();
                    long seedToUse = (masterSeed != null && masterSeed >= 0)
                            ? toNonNegativeDeterministicSeed(SeedDerivation.derive64(masterSeed, "return:dataDriven"))
                            : masterSeed;
                    dd.reseed(seedToUse);
                }
                yield r;
            }
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
        IDistribution distribution = createConfiguredDistribution(config);
        Long masterSeed = config.getSeed();
        Long derived = null;
        if (masterSeed != null && masterSeed >= 0) {
            derived = toNonNegativeDeterministicSeed(SeedDerivation.derive64(masterSeed, "return:sample"));
        }

        IRandomNumberGenerator rng = (masterSeed == null)
                ? new DefaultRandomNumberGenerator()
                : new DefaultRandomNumberGenerator((derived != null) ? derived : masterSeed);

        DefaultRandomVariable rv = new DefaultRandomVariable(distribution, rng);
        return new DistributionReturn(rv);
    }

    private IDistribution createConfiguredDistribution(ReturnerConfig returnerConfig) {
        ReturnerConfig.DistributionConfig config = (returnerConfig == null) ? null : returnerConfig.getDistribution();
        Long seed = (returnerConfig == null) ? null : returnerConfig.getSeed();
        return createConfiguredDistribution(config, seed);
    }

    private IDistribution createConfiguredDistribution(ReturnerConfig.DistributionConfig config, Long seed) {
        if (config == null || config.getType() == null || config.getType().isBlank()) {
            return distributionFactory.createDistribution("normal");
        }

        if ("regimeBased".equals(config.getType())) {
            return createConfiguredRegimeBasedDistribution(config.getRegimeBased(), seed);
        }

        String beanKey = switch (config.getType()) {
            case "studentT" -> "tDistribution";
            default -> config.getType();
        };

        IDistribution dist = distributionFactory.createDistribution(beanKey);

        // Apply configured dt so that annualized parameters scale to the chosen return step.
        double dt = resolveDt();
        if (dist instanceof NormalDistribution normal) {
            normal.setDt(dt);
        }
        if (dist instanceof BrownianMotionDistribution brownian) {
            brownian.setDt(dt);
        }
        if (dist instanceof TDistributionImpl tDist) {
            tDist.setDt(dt);
        }

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

    private IDistribution createConfiguredRegimeBasedDistribution(ReturnerConfig.RegimeBasedParams regimeBased, Long seed) {
        // Defaults: 3 regimes, monthly tick.
        final int expectedRegimes = 3;
        final double dt = resolveDt();

        if (regimeBased == null || regimeBased.getRegimes() == null || regimeBased.getRegimes().size() < expectedRegimes) {
            throw new IllegalArgumentException("regimeBased.regimes must contain at least 3 regimes (0..2)");
        }

        IDistribution[] regimeDistributions = new IDistribution[expectedRegimes];
        double[] expectedDurationMonths = new double[expectedRegimes];
        double[][] weights = new double[expectedRegimes][expectedRegimes];

        for (int i = 0; i < expectedRegimes; i++) {
            ReturnerConfig.RegimeParams r = regimeBased.getRegimes().get(i);
            if (r == null) {
                throw new IllegalArgumentException("regimeBased.regimes[" + i + "] must not be null");
            }

            expectedDurationMonths[i] = (r.getExpectedDurationMonths() == null) ? 12.0 : r.getExpectedDurationMonths();

            // Switch weights (only used when leaving a regime)
            ReturnerConfig.SwitchWeights w = r.getSwitchWeights();
            weights[i][0] = (w == null || w.getToRegime0() == null) ? 0.0 : w.getToRegime0();
            weights[i][1] = (w == null || w.getToRegime1() == null) ? 0.0 : w.getToRegime1();
            weights[i][2] = (w == null || w.getToRegime2() == null) ? 0.0 : w.getToRegime2();
            weights[i][i] = 0.0;

            String type = (r.getDistributionType() == null) ? "normal" : r.getDistributionType();
            String beanKey = switch (type) {
                case "studentT" -> "tDistribution";
                case "normal" -> "normal";
                default -> throw new IllegalArgumentException("Unsupported regime distributionType (v1): " + type);
            };

            IDistribution dist = distributionFactory.createDistribution(beanKey);

            if (dist instanceof NormalDistribution normal) {
                normal.setDt(dt);
                if (r.getNormal() != null) {
                    if (r.getNormal().getMean() != null) normal.setMean(r.getNormal().getMean());
                    if (r.getNormal().getStandardDeviation() != null) {
                        normal.setStandardDeviation(r.getNormal().getStandardDeviation());
                    }
                }
            }

            if (dist instanceof TDistributionImpl tDist) {
                tDist.setDt(dt);
                if (r.getStudentT() != null) {
                    if (r.getStudentT().getMu() != null) tDist.setMu(r.getStudentT().getMu());
                    if (r.getStudentT().getSigma() != null) tDist.setSigma(r.getStudentT().getSigma());
                    if (r.getStudentT().getNu() != null) {
                        tDist.setNu(r.getStudentT().getNu());
                        tDist.init();
                    }
                }
            }

            regimeDistributions[i] = dist;
        }

        Long providerSeed = seed;
        if (seed != null && seed >= 0) {
            providerSeed = toNonNegativeDeterministicSeed(SeedDerivation.derive64(seed, "return:regime"));
        }
        IRegimeProvider provider = new ExpectedDurationWeightedRegimeProvider(0, expectedDurationMonths, weights, providerSeed);
        return context.getBean(RegimeBasedDistribution.class, regimeDistributions, provider);
    }
}
