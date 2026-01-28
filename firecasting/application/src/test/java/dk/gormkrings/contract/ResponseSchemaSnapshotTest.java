package dk.gormkrings.contract;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.introspect.BeanPropertyDefinition;
import dk.gormkrings.diff.RunDiffResponse;
import dk.gormkrings.dto.StandardResultsV3Response;
import dk.gormkrings.export.ReproducibilityBundleDto;
import dk.gormkrings.statistics.MetricSummary;
import dk.gormkrings.statistics.YearlySummary;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import static org.assertj.core.api.Assertions.fail;

/**
 * Snapshot test for response DTO JSON shapes.
 * Detects missing/renamed fields and type changes even when OpenAPI is not public.
 */
class ResponseSchemaSnapshotTest {

    private static final Path SNAPSHOT = Path.of("src", "test", "resources", "contracts", "response-schemas.json");

    @Test
    void responseSchemas_matchSnapshotOrUpdate() throws Exception {
        ObjectMapper mapper = new ObjectMapper();

        Map<String, Object> schemas = new LinkedHashMap<>();
        Set<Class<?>> roots = Set.of(
            YearlySummary.class,
            MetricSummary.class,
            StandardResultsV3Response.class,
            ReproducibilityBundleDto.class,
            RunDiffResponse.class
        );

        SchemaCollector collector = new SchemaCollector(mapper);
        for (Class<?> root : roots) {
            collector.collect(root);
        }
        schemas.putAll(collector.schemas);

        // Stable JSON ordering
        String json = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(schemas);
        JsonNode runtime = mapper.readTree(json);

        boolean update = Boolean.parseBoolean(System.getProperty("schema.snapshot.update",
            System.getProperty("schemaSnapshotUpdate", "false")));
        if (update) {
            Files.createDirectories(SNAPSHOT.getParent());
            Files.writeString(SNAPSHOT, mapper.writerWithDefaultPrettyPrinter().writeValueAsString(runtime), StandardCharsets.UTF_8);
            return;
        }

        if (!Files.exists(SNAPSHOT)) {
            fail("Missing response schema snapshot at %s. Regenerate with -Dschema.snapshot.update=true".formatted(SNAPSHOT));
        }

        JsonNode expected = mapper.readTree(Files.readString(SNAPSHOT, StandardCharsets.UTF_8));
        if (!Objects.equals(expected, runtime)) {
            fail("Response schema drift detected. Regenerate with: ./mvnw.cmd -pl application -Dtest=%s -Dschema.snapshot.update=true test".formatted(ResponseSchemaSnapshotTest.class.getName()));
        }
    }

    private static final class SchemaCollector {
        private final ObjectMapper mapper;
        private final Set<String> visited = new LinkedHashSet<>();
        private final Map<String, Object> schemas = new LinkedHashMap<>();

        private SchemaCollector(ObjectMapper mapper) {
            this.mapper = mapper;
        }

        void collect(Class<?> type) {
            collect(mapper.constructType(type));
        }

        void collect(JavaType type) {
            if (type == null) return;
            Class<?> raw = type.getRawClass();
            if (raw == null) return;

            // Only snapshot our own DTOs (and their nested types)
            String name = raw.getName();
            if (!name.startsWith("dk.gormkrings.")) return;
            if (!visited.add(name)) return;

            if (com.fasterxml.jackson.databind.JsonNode.class.isAssignableFrom(raw)) {
                schemas.put(name, Map.of("$type", "any"));
                return;
            }

            var bean = mapper.getSerializationConfig().introspect(type);
            Map<String, String> props = new LinkedHashMap<>();
            for (BeanPropertyDefinition p : bean.findProperties()) {
                String propName = p.getName();
                JavaType propType = p.getPrimaryType();
                props.put(propName, renderType(propType));
                collectNested(propType);
            }
            schemas.put(name, props);
        }

        private void collectNested(JavaType t) {
            if (t == null) return;
            if (t.isArrayType()) {
                collectNested(t.getContentType());
                return;
            }
            if (t.isCollectionLikeType()) {
                collectNested(t.getContentType());
                return;
            }
            Class<?> raw = t.getRawClass();
            if (raw == null) return;
            if (raw.getName().startsWith("dk.gormkrings.")) {
                collect(t);
            }
        }

        private static String renderType(JavaType t) {
            if (t == null) return "unknown";
            Class<?> raw = t.getRawClass();
            if (raw == null) return "unknown";

            if (t.isArrayType()) {
                return "array<" + renderType(t.getContentType()) + ">";
            }
            if (t.isCollectionLikeType()) {
                return "array<" + renderType(t.getContentType()) + ">";
            }
            if (com.fasterxml.jackson.databind.JsonNode.class.isAssignableFrom(raw)) return "any";

            if (raw == String.class) return "string";
            if (raw == boolean.class || raw == Boolean.class) return "boolean";
            if (raw == int.class || raw == Integer.class || raw == long.class || raw == Long.class) return "integer";
            if (raw == double.class || raw == Double.class || raw == float.class || raw == Float.class) return "number";

            if (raw.getName().startsWith("dk.gormkrings.")) return "object:" + raw.getName();
            return raw.getSimpleName();
        }
    }
}
