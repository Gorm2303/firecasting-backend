package dk.gormkrings.contract;

import dk.gormkrings.FirecastingController;
import dk.gormkrings.config.ExecConfig;
import dk.gormkrings.config.JacksonConfig;
import dk.gormkrings.config.SecurityConfig;
import dk.gormkrings.diff.RunDiffService;
import dk.gormkrings.export.ReproducibilityBundleService;
import dk.gormkrings.queue.SimulationQueueService;
import dk.gormkrings.simulation.SimulationMetricSummariesCache;
import dk.gormkrings.simulation.SimulationTimingsCache;
import dk.gormkrings.simulation.SimulationResultsCache;
import dk.gormkrings.simulation.SimulationRunner;
import dk.gormkrings.simulation.SimulationStartService;
import dk.gormkrings.simulation.SimulationSummariesCache;
import dk.gormkrings.sse.SimulationSseService;
import dk.gormkrings.statistics.StatisticsService;
import dk.gormkrings.ui.forms.FormsController;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(
    classes = OpenApiExposureGuardTest.OpenApiExposureTestApp.class,
    properties = {
        "spring.main.allow-bean-definition-overriding=true",
        "springdoc.api-docs.enabled=true",
        "springdoc.swagger-ui.enabled=false",
        "springdoc.enable-default-api-docs=false",
        // default: settings.openapi.expose=false
    }
)
@AutoConfigureMockMvc
class OpenApiExposureGuardTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void openApi_isForbiddenUnlessExplicitlyExposed() throws Exception {
        mockMvc.perform(get("/v3/api-docs/" + OpenApiContractConfig.PUBLIC_GROUP).accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isForbidden());
    }

    @SpringBootTest(
        classes = OpenApiExposureTestApp.class,
        properties = {
            "spring.main.allow-bean-definition-overriding=true",
            "springdoc.api-docs.enabled=true",
            "springdoc.swagger-ui.enabled=false",
            "springdoc.enable-default-api-docs=false",
            "settings.openapi.expose=true"
        }
    )
    @AutoConfigureMockMvc
    static class WhenExposed {
        @Autowired
        private MockMvc mockMvc;

        @Test
        void openApi_isAccessibleWhenExplicitlyExposed() throws Exception {
            mockMvc.perform(get("/v3/api-docs/" + OpenApiContractConfig.PUBLIC_GROUP).accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
        }
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration(exclude = {
        DataSourceAutoConfiguration.class,
        HibernateJpaAutoConfiguration.class,
        FlywayAutoConfiguration.class
    })
    @Import({
        FirecastingController.class,
        FormsController.class,
        OpenApiContractConfig.class,
        JacksonConfig.class,
        ExecConfig.class,
        SecurityConfig.class,
        OpenApiExposureTestApp.Mocks.class
    })
    static class OpenApiExposureTestApp {
        @TestConfiguration
        static class Mocks {
            @Bean
            EntityManagerFactory entityManagerFactory() {
                EntityManagerFactory emf = Mockito.mock(EntityManagerFactory.class);
                Mockito.when(emf.createEntityManager()).thenReturn(Mockito.mock(EntityManager.class));
                return emf;
            }

            @Bean
            SimulationQueueService simulationQueueService() {
                return Mockito.mock(SimulationQueueService.class);
            }

            @Bean
            SimulationSseService simulationSseService() {
                return Mockito.mock(SimulationSseService.class);
            }

            @Bean
            StatisticsService statisticsService() {
                return Mockito.mock(StatisticsService.class);
            }

            @Bean
            SimulationStartService simulationStartService() {
                return Mockito.mock(SimulationStartService.class);
            }

            @Bean
            SimulationRunner simulationRunner() {
                return Mockito.mock(SimulationRunner.class);
            }

            @Bean
            SimulationResultsCache simulationResultsCache() {
                return Mockito.mock(SimulationResultsCache.class);
            }

            @Bean
            SimulationSummariesCache simulationSummariesCache() {
                return Mockito.mock(SimulationSummariesCache.class);
            }

            @Bean
            SimulationMetricSummariesCache simulationMetricSummariesCache() {
                return Mockito.mock(SimulationMetricSummariesCache.class);
            }

            @Bean
            SimulationTimingsCache simulationTimingsCache() {
                return Mockito.mock(SimulationTimingsCache.class);
            }

            @Bean
            ReproducibilityBundleService reproducibilityBundleService() {
                return Mockito.mock(ReproducibilityBundleService.class);
            }

            @Bean
            RunDiffService runDiffService() {
                return Mockito.mock(RunDiffService.class);
            }
        }
    }
}
