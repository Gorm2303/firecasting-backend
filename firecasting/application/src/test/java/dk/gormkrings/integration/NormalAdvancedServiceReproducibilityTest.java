package dk.gormkrings.integration;

import dk.gormkrings.dto.AdvancedSimulationRequest;
import dk.gormkrings.dto.PhaseRequest;
import dk.gormkrings.dto.SimulationRequest;
import dk.gormkrings.queue.SimulationQueueService;
import dk.gormkrings.returns.ReturnerConfig;
import dk.gormkrings.simulation.AdvancedSimulationRequestMapper;
import dk.gormkrings.simulation.SimulationResultsCache;
import dk.gormkrings.simulation.SimulationRunSpec;
import dk.gormkrings.simulation.SimulationStartService;
import dk.gormkrings.statistics.StatisticsService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.TestPropertySource;
import java.util.stream.Collectors;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Service-level integration test with lower run count: verifies that normal-mode and advanced-mode
 * requests produce identical results when using the same seed and equivalent inputs.
 * Tests with settings.runs=1 and settings.batch-size=1 for quick validation.
 *
 * Uses realistic 10-year savings plan with monthly deposits and capital gains tax.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@org.springframework.test.context.ActiveProfiles("local")
@org.springframework.boot.autoconfigure.EnableAutoConfiguration(exclude = {
    org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration.class,
    org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration.class,
    org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration.class
})
@TestPropertySource(properties = {
    "settings.runs=1",
    "settings.batch-size=1",
    // Disable DB/Flyway to avoid requiring Postgres in tests
    "spring.flyway.enabled=false",
    "spring.jpa.hibernate.ddl-auto=none",
    "spring.datasource.url=jdbc:h2:mem:testdb;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
    "spring.datasource.driverClassName=org.h2.Driver",
    "spring.datasource.username=sa",
    "spring.datasource.password=",
    "spring.jpa.show-sql=false"
})
class NormalAdvancedServiceReproducibilityTest {

    @Autowired
    private SimulationStartService simulationStartService;

    @Autowired
    private SimulationResultsCache resultsCache;

    @MockBean
    private SimulationQueueService queue;

    @MockBean
    private StatisticsService statisticsService;

    @MockBean
    private dk.gormkrings.sse.SimulationSseService sseService;

    private static final long SEED = 98765L;
    private static final String START_DATE = "2023-06-15";
    private static final double INITIAL_DEPOSIT = 25_000.0;
    private static final double MONTHLY_DEPOSIT = 500.0;
    private static final int DEPOSIT_YEARS = 10;
    private static final float TAX_PERCENTAGE = 0.20f;  // 20% capital gains tax

    @Test
    void normalAndAdvancedWithSameSeed_ProduceIdenticalResults() {
        when(statisticsService.findExistingRunIdForSignature(any())).thenReturn(Optional.empty());
        stubQueueToRunInline();

        var normalReq = createNormalRequest(SEED);
        var normalAdv = toAdvancedEquivalentOfNormal(SEED, normalReq);
        var normalSpec = AdvancedSimulationRequestMapper.toRunSpec(normalAdv);
        var normalId = simulationStartService
            .startSimulation("/start", normalSpec, normalAdv, normalAdv, null, SEED)
            .getBody()
            .get("id");
        var normalResults = resultsCache.get(normalId);
        assertNotNull(normalResults, "Normal-mode results not found in cache");

        var advancedReq = createAdvancedRequest(SEED);
        advancedReq.setPaths(1);
        advancedReq.setBatchSize(1);
        var advancedSpec = AdvancedSimulationRequestMapper.toRunSpec(advancedReq);
        var advancedId = simulationStartService
            .startSimulation("/start-advanced", advancedSpec, advancedReq, advancedReq, null, SEED)
            .getBody()
            .get("id");
        var advancedResults = resultsCache.get(advancedId);
        assertNotNull(advancedResults, "Advanced-mode results not found in cache");

        var normalCaps = finalCapitals(normalResults);
        var advancedCaps = finalCapitals(advancedResults);
        assertEquals(normalCaps, advancedCaps, "Final capitals should match for identical seeds");

        var normalRealCaps = finalRealCapitals(normalResults);
        var advancedRealCaps = finalRealCapitals(advancedResults);
        assertEquals(normalRealCaps, advancedRealCaps, "Inflation-adjusted capitals should match for identical seeds");
    }

    private void stubQueueToRunInline() {
        when(queue.submitWithId(anyString(), any())).thenAnswer(inv -> {
            Runnable r = inv.getArgument(1);
            r.run();
            return true;
        });
    }

    private SimulationRequest createNormalRequest(long seed) {
        SimulationRequest request = new SimulationRequest();
        request.setStartDate(new dk.gormkrings.simulation.data.Date(START_DATE));
        request.setOverallTaxRule(dk.gormkrings.dto.OverallTaxRule.CAPITAL);
        request.setTaxPercentage(TAX_PERCENTAGE);
        request.setReturnPercentage(0.0f);
        request.setSeed(seed);
        request.setPhases(createPhases());
        return request;
    }

    private AdvancedSimulationRequest toAdvancedEquivalentOfNormal(long seed, SimulationRequest request) {
        AdvancedSimulationRequest advanced = new AdvancedSimulationRequest();
        advanced.setPaths(1);
        advanced.setBatchSize(1);
        advanced.setStartDate(request.getStartDate());
        advanced.setPhases(request.getPhases());
        advanced.setOverallTaxRule(request.getOverallTaxRule());
        advanced.setTaxPercentage(request.getTaxPercentage());

        advanced.setReturnType("dataDrivenReturn");
        advanced.setInflationFactor(1.02D);
        advanced.setYearlyFeePercentage(0.0D);

        ReturnerConfig rc = new ReturnerConfig();
        rc.setSeed(seed);
        advanced.setReturnerConfig(rc);
        advanced.setSeed(seed);
        return advanced;
    }

    private AdvancedSimulationRequest createAdvancedRequest(long seed) {
        AdvancedSimulationRequest request = new AdvancedSimulationRequest();
        request.setStartDate(new dk.gormkrings.simulation.data.Date(START_DATE));
        request.setOverallTaxRule(dk.gormkrings.dto.OverallTaxRule.CAPITAL);
        request.setTaxPercentage(TAX_PERCENTAGE);
        request.setSeed(seed);
        request.setReturnType("dataDrivenReturn");
        request.setInflationFactor(1.02D);
        request.setYearlyFeePercentage(0.0D);
        request.setPhases(createPhases());
        ReturnerConfig rc = new ReturnerConfig();
        rc.setSeed(seed);
        request.setReturnerConfig(rc);
        return request;
    }

    private List<PhaseRequest> createPhases() {
        List<PhaseRequest> phases = new ArrayList<>();
        
        // Deposit phase: 10 years of monthly contributions
        PhaseRequest deposit = new PhaseRequest();
        deposit.setPhaseType(dk.gormkrings.dto.PhaseType.DEPOSIT);
        deposit.setDurationInMonths(12 * DEPOSIT_YEARS);
        deposit.setInitialDeposit(INITIAL_DEPOSIT);
        deposit.setMonthlyDeposit(MONTHLY_DEPOSIT);
        deposit.setLowerVariationPercentage(0.0);
        deposit.setUpperVariationPercentage(0.0);
        phases.add(deposit);
        
        return phases;
    }

    private List<Double> finalCapitals(List<dk.gormkrings.result.IRunResult> results) {
        return results.stream()
                .map(r -> r.getSnapshots().getLast().getState().getCapital())
                .sorted()
                .collect(Collectors.toList());
    }

    private List<Double> finalRealCapitals(List<dk.gormkrings.result.IRunResult> results) {
        return results.stream()
                .map(r -> r.getSnapshots().getLast().getState())
                .map(s -> s.getCapital() / s.getInflation())
                .sorted()
                .collect(Collectors.toList());
    }
}
