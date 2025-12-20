package dk.gormkrings.returns;

import dk.gormkrings.distribution.BrownianMotionDistribution;
import dk.gormkrings.distribution.NormalDistribution;
import dk.gormkrings.distribution.RegimeBasedDistribution;
import dk.gormkrings.distribution.TDistributionImpl;
import dk.gormkrings.distribution.factory.DistributionFactory;
import dk.gormkrings.math.distribution.IDistribution;
import dk.gormkrings.regime.IRegimeProvider;
import dk.gormkrings.simulation.ReturnStep;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.context.ApplicationContext;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

public class DefaultReturnFactoryReturnStepTest {

    private static ReturnerConfig buildDistributionConfig(String type) {
        ReturnerConfig cfg = new ReturnerConfig();
        cfg.setSeed(123L);

        ReturnerConfig.DistributionConfig dist = new ReturnerConfig.DistributionConfig();
        dist.setType(type);

        if ("normal".equals(type)) {
            ReturnerConfig.NormalParams p = new ReturnerConfig.NormalParams();
            p.setMean(0.07);
            p.setStandardDeviation(0.20);
            dist.setNormal(p);
        }

        if ("brownianMotion".equals(type)) {
            ReturnerConfig.BrownianParams p = new ReturnerConfig.BrownianParams();
            p.setDrift(0.07);
            p.setVolatility(0.20);
            dist.setBrownianMotion(p);
        }

        if ("studentT".equals(type)) {
            ReturnerConfig.StudentTParams p = new ReturnerConfig.StudentTParams();
            p.setMu(0.07);
            p.setSigma(0.20);
            p.setNu(5.0);
            dist.setStudentT(p);
        }

        cfg.setDistribution(dist);
        return cfg;
    }

    @ParameterizedTest
    @EnumSource(ReturnStep.class)
    void normalDistribution_usesConfiguredDt(ReturnStep step) {
        ApplicationContext ctx = mock(ApplicationContext.class);
        DistributionFactory distributionFactory = mock(DistributionFactory.class);

        NormalDistribution normal = spy(new NormalDistribution());
        when(distributionFactory.createDistribution("normal")).thenReturn(normal);

        DefaultReturnFactory factory = new DefaultReturnFactory(ctx, distributionFactory, step);
        factory.createReturn("distributionReturn", buildDistributionConfig("normal"));

        assertEquals(step.toDt(), normal.getDt(), 1e-12);
    }

    @ParameterizedTest
    @EnumSource(ReturnStep.class)
    void brownianMotionDistribution_usesConfiguredDt(ReturnStep step) {
        ApplicationContext ctx = mock(ApplicationContext.class);
        DistributionFactory distributionFactory = mock(DistributionFactory.class);

        BrownianMotionDistribution brownian = spy(new BrownianMotionDistribution());
        when(distributionFactory.createDistribution("brownianMotion")).thenReturn(brownian);

        DefaultReturnFactory factory = new DefaultReturnFactory(ctx, distributionFactory, step);
        factory.createReturn("distributionReturn", buildDistributionConfig("brownianMotion"));

        assertEquals(step.toDt(), brownian.getDt(), 1e-12);
    }

    @ParameterizedTest
    @EnumSource(ReturnStep.class)
    void studentTDistribution_usesConfiguredDt(ReturnStep step) {
        ApplicationContext ctx = mock(ApplicationContext.class);
        DistributionFactory distributionFactory = mock(DistributionFactory.class);

        TDistributionImpl tDist = spy(new TDistributionImpl());
        when(distributionFactory.createDistribution("tDistribution")).thenReturn(tDist);

        DefaultReturnFactory factory = new DefaultReturnFactory(ctx, distributionFactory, step);
        factory.createReturn("distributionReturn", buildDistributionConfig("studentT"));

        assertEquals(step.toDt(), tDist.getDt(), 1e-12);
    }

    @ParameterizedTest
    @EnumSource(ReturnStep.class)
    void regimeBasedDistributions_usesConfiguredDtForAllRegimes(ReturnStep step) {
        ApplicationContext ctx = mock(ApplicationContext.class);
        DistributionFactory distributionFactory = mock(DistributionFactory.class);

        // Return 3 distinct NormalDistribution instances for the 3 regimes.
        NormalDistribution r0 = spy(new NormalDistribution());
        NormalDistribution r1 = spy(new NormalDistribution());
        NormalDistribution r2 = spy(new NormalDistribution());
        when(distributionFactory.createDistribution("normal")).thenReturn(r0, r1, r2);

        when(ctx.getBean(eq(RegimeBasedDistribution.class), any(), any())).thenAnswer(inv -> {
            IDistribution[] regimes = (IDistribution[]) inv.getArgument(1);
            IRegimeProvider provider = (IRegimeProvider) inv.getArgument(2);
            return new RegimeBasedDistribution(regimes, provider);
        });

        DefaultReturnFactory factory = new DefaultReturnFactory(ctx, distributionFactory, step);

        ReturnerConfig cfg = new ReturnerConfig();
        cfg.setSeed(1L);

        ReturnerConfig.DistributionConfig dist = new ReturnerConfig.DistributionConfig();
        dist.setType("regimeBased");

        ReturnerConfig.RegimeBasedParams rb = new ReturnerConfig.RegimeBasedParams();
        rb.setTickMonths(1);

        rb.setRegimes(List.of(
                regimeNormal(0.07, 0.20, 12.0, 0.0, 0.5, 0.5),
                regimeNormal(0.07, 0.20, 12.0, 0.5, 0.0, 0.5),
                regimeNormal(0.07, 0.20, 12.0, 0.5, 0.5, 0.0)
        ));

        dist.setRegimeBased(rb);
        cfg.setDistribution(dist);

        factory.createReturn("distributionReturn", cfg);

        assertEquals(step.toDt(), r0.getDt(), 1e-12);
        assertEquals(step.toDt(), r1.getDt(), 1e-12);
        assertEquals(step.toDt(), r2.getDt(), 1e-12);
    }

    private static ReturnerConfig.RegimeParams regimeNormal(
            double mean,
            double stdev,
            double expectedDurationMonths,
            double to0,
            double to1,
            double to2
    ) {
        ReturnerConfig.RegimeParams r = new ReturnerConfig.RegimeParams();
        r.setDistributionType("normal");
        r.setExpectedDurationMonths(expectedDurationMonths);

        ReturnerConfig.NormalParams np = new ReturnerConfig.NormalParams();
        np.setMean(mean);
        np.setStandardDeviation(stdev);
        r.setNormal(np);

        ReturnerConfig.SwitchWeights w = new ReturnerConfig.SwitchWeights();
        w.setToRegime0(to0);
        w.setToRegime1(to1);
        w.setToRegime2(to2);
        r.setSwitchWeights(w);
        return r;
    }
}
