package dk.gormkrings.reproducibility;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dk.gormkrings.api.ApiValidationException;
import dk.gormkrings.export.ReproducibilityBundleDto;
import dk.gormkrings.reproducibility.persistence.ReproducibilityReplayEntity;
import dk.gormkrings.reproducibility.persistence.ReproducibilityReplayRepository;
import dk.gormkrings.simulation.SimulationRunSpec;
import dk.gormkrings.simulation.SimulationStartService;
import dk.gormkrings.statistics.StatisticsService;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ReproducibilityReplayServiceValidationTest {

    private static ReproducibilityReplayService newService(
            ObjectMapper objectMapper,
            SimulationStartService startService,
            ReproducibilityReplayRepository replayRepo,
            Validator validator
    ) {
        StatisticsService stats = mock(StatisticsService.class);
        return new ReproducibilityReplayService(
                objectMapper,
                stats,
                startService,
                replayRepo,
                Optional.empty(),
                validator
        );
    }

    private static ReproducibilityBundleDto bundleWithInputs(JsonNode raw, String kind) {
        var bundle = new ReproducibilityBundleDto();
        var inputs = new ReproducibilityBundleDto.Inputs();
        inputs.setKind(kind);
        inputs.setRaw(raw);
        bundle.setInputs(inputs);
        return bundle;
    }

    @Test
    void missingInputsReportsExactFailure() {
        ObjectMapper om = new ObjectMapper();
        Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

        SimulationStartService start = mock(SimulationStartService.class);
        ReproducibilityReplayRepository repo = mock(ReproducibilityReplayRepository.class);

        when(repo.save(any())).thenAnswer(inv -> {
            ReproducibilityReplayEntity e = inv.getArgument(0);
            e.setId("replay-1");
            return e;
        });

        var svc = newService(om, start, repo, validator);

        var ex = assertThrows(ApiValidationException.class, () -> svc.importBundle(new ReproducibilityBundleDto()));
        assertEquals("Validation failed", ex.getMessage());
        assertTrue(ex.getDetails().contains("inputs: is required"));
    }

    @Test
    void missingRawReportsExactFailure() {
        ObjectMapper om = new ObjectMapper();
        Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

        SimulationStartService start = mock(SimulationStartService.class);
        ReproducibilityReplayRepository repo = mock(ReproducibilityReplayRepository.class);

        when(repo.save(any())).thenAnswer(inv -> {
            ReproducibilityReplayEntity e = inv.getArgument(0);
            e.setId("replay-1");
            return e;
        });

        var svc = newService(om, start, repo, validator);

        var bundle = new ReproducibilityBundleDto();
        bundle.setInputs(new ReproducibilityBundleDto.Inputs());

        var ex = assertThrows(ApiValidationException.class, () -> svc.importBundle(bundle));
        assertEquals("Validation failed", ex.getMessage());
        assertTrue(ex.getDetails().contains("inputs.raw: is required"));
    }

    @Test
    void invalidNormalInputReportsFieldPath() {
        ObjectMapper om = new ObjectMapper();
        Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

        SimulationStartService start = mock(SimulationStartService.class);
        ReproducibilityReplayRepository repo = mock(ReproducibilityReplayRepository.class);

        when(repo.save(any())).thenAnswer(inv -> {
            ReproducibilityReplayEntity e = inv.getArgument(0);
            e.setId("replay-1");
            return e;
        });

        var svc = newService(om, start, repo, validator);

        // Missing overallTaxRule (required)
        JsonNode raw = om.valueToTree(Map.of(
                "startDate", Map.of("date", "2020-01-01"),
                "phases", List.of(Map.of("phaseType", "DEPOSIT", "durationInMonths", 12)),
                "taxPercentage", 37.0
        ));

        var ex = assertThrows(ApiValidationException.class, () -> svc.importBundle(bundleWithInputs(raw, "normal")));
        assertEquals("Validation failed", ex.getMessage());
        assertTrue(ex.getDetails().stream().anyMatch(d -> d.startsWith("inputs.raw.overallTaxRule:")));
    }

    @Test
    void advancedMissingReturnTypeDefaultsSafely() {
        ObjectMapper om = new ObjectMapper();
        Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

        SimulationStartService start = mock(SimulationStartService.class);
        when(start.startSimulation(anyString(), any(), any(), any(SimulationStartService.SimulationPostProcessor.class)))
                .thenReturn(ResponseEntity.accepted().body(Map.of("id", "sim-1")));

        ReproducibilityReplayRepository repo = mock(ReproducibilityReplayRepository.class);
        when(repo.save(any())).thenAnswer(inv -> {
            ReproducibilityReplayEntity e = inv.getArgument(0);
            e.setId("replay-1");
            return e;
        });

        var svc = newService(om, start, repo, validator);

        // returnType omitted; mapper should default to dataDrivenReturn
        JsonNode raw = om.valueToTree(Map.of(
                "startDate", Map.of("date", "2020-01-01"),
                "phases", List.of(Map.of("phaseType", "DEPOSIT", "durationInMonths", 12)),
                "overallTaxRule", "CAPITAL",
                "taxPercentage", 37.0,
                "inflationFactor", 0.0
        ));

        svc.importBundle(bundleWithInputs(raw, "advanced"));

        ArgumentCaptor<SimulationRunSpec> specCap = ArgumentCaptor.forClass(SimulationRunSpec.class);
        verify(start, times(1)).startSimulation(eq("/import"), specCap.capture(), any(), any(SimulationStartService.SimulationPostProcessor.class));
        assertEquals("dataDrivenReturn", specCap.getValue().getReturnType());
        assertEquals(1.02D, specCap.getValue().getInflationFactor(), 1e-12);
    }
}
