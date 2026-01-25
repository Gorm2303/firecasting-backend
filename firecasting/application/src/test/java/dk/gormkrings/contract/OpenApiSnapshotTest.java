package dk.gormkrings.contract;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import dk.gormkrings.FirecastingController;
import dk.gormkrings.config.ExecConfig;
import dk.gormkrings.config.JacksonConfig;
import dk.gormkrings.diff.RunDiffService;
import dk.gormkrings.export.ReproducibilityBundleService;
import dk.gormkrings.queue.SimulationQueueService;
import dk.gormkrings.simulation.SimulationResultsCache;
import dk.gormkrings.simulation.SimulationRunner;
import dk.gormkrings.simulation.SimulationStartService;
import dk.gormkrings.simulation.SimulationSummariesCache;
import dk.gormkrings.sse.SimulationSseService;
import dk.gormkrings.statistics.StatisticsService;
import dk.gormkrings.ui.forms.FormsController;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.security.servlet.ManagementWebSecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.oauth2.client.servlet.OAuth2ClientAutoConfiguration;
import org.springframework.boot.autoconfigure.security.oauth2.resource.servlet.OAuth2ResourceServerAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(
        classes = OpenApiSnapshotTest.ContractOpenApiTestApp.class,
        properties = {
        "spring.main.allow-bean-definition-overriding=true",
                "springdoc.api-docs.enabled=true",
                "springdoc.swagger-ui.enabled=false",
                "springdoc.enable-default-api-docs=false",
                "springdoc.writer-with-order-by-keys=true",
                // Reduce accidental noise in the OpenAPI output.
                "spring.jackson.serialization.indent_output=true"
        }
)
@AutoConfigureMockMvc
class OpenApiSnapshotTest {

    private static final String GROUP = OpenApiContractConfig.PUBLIC_GROUP;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    @Qualifier("canonicalObjectMapper")
    private ObjectMapper canonicalObjectMapper;

    @Test
    void openApiSnapshot_matchesRuntimeOrUpdates() throws Exception {
        Path snapshotPath = findFileUpwards(Path.of("src", "test", "resources", "openapi", "openapi.yaml"));

        JsonNode runtime = fetchRuntimeOpenApiJson();

        boolean update = Boolean.parseBoolean(System.getProperty("openapi.snapshot.update", "false"));
        if (update) {
            writeYamlSnapshot(snapshotPath, runtime);
            return;
        }

        if (!Files.exists(snapshotPath)) {
            fail("Missing OpenAPI snapshot at %s. Regenerate with -Dopenapi.snapshot.update=true".formatted(snapshotPath));
        }

        JsonNode expected = readYamlSnapshotAsJson(snapshotPath);

        if (!Objects.equals(expected, runtime)) {
            fail("OpenAPI snapshot drift detected. Regenerate with: ./mvnw.cmd -pl application -Dtest=%s -Dopenapi.snapshot.update=true test\n\nExpected (canonical JSON) vs Runtime (canonical JSON) differ."
                    .formatted(OpenApiSnapshotTest.class.getName()));
        }

        assertThat(runtime).isEqualTo(expected);
    }

    private JsonNode fetchRuntimeOpenApiJson() throws Exception {
        String json = mockMvc.perform(get("/v3/api-docs/" + GROUP).accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString(StandardCharsets.UTF_8);
        return canonicalObjectMapper.readTree(json);
    }

    private static JsonNode readYamlSnapshotAsJson(Path snapshotPath) throws Exception {
        ObjectMapper yaml = new ObjectMapper(new YAMLFactory());
        try (var in = Files.newInputStream(snapshotPath)) {
            return yaml.readTree(in);
        }
    }

    private void writeYamlSnapshot(Path snapshotPath, JsonNode runtime) throws Exception {
        Files.createDirectories(snapshotPath.getParent());

        // Ensure stable key ordering in what we write.
        String canonicalJson = canonicalObjectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(runtime);
        JsonNode reparsed = canonicalObjectMapper.readTree(canonicalJson);

        ObjectMapper yaml = new ObjectMapper(new YAMLFactory());
        String out = yaml.writeValueAsString(reparsed);
        Files.writeString(snapshotPath, out, StandardCharsets.UTF_8);
    }

    private static Path findFileUpwards(Path relative) {
        Path here = Path.of("").toAbsolutePath();
        for (int i = 0; i < 6; i++) {
            Path candidate = here.resolve(relative).normalize();
            if (Files.exists(candidate)) return candidate;
            here = here.getParent();
            if (here == null) break;
        }
        // Fall back: return the original relative path from current dir.
        return relative;
    }

    @SpringBootConfiguration
        @EnableAutoConfiguration(exclude = {
            DataSourceAutoConfiguration.class,
            HibernateJpaAutoConfiguration.class,
            FlywayAutoConfiguration.class,
            SecurityAutoConfiguration.class,
            SecurityFilterAutoConfiguration.class,
            UserDetailsServiceAutoConfiguration.class,
            OAuth2ClientAutoConfiguration.class,
                OAuth2ResourceServerAutoConfiguration.class,
                ManagementWebSecurityAutoConfiguration.class
        })
    @Import({
            // Controllers included in the public contract
            FirecastingController.class,
            FormsController.class,
            // Contract + shared infra
            OpenApiContractConfig.class,
            JacksonConfig.class,
            ExecConfig.class,
            ContractOpenApiTestApp.Mocks.class
    })
    static class ContractOpenApiTestApp {

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
