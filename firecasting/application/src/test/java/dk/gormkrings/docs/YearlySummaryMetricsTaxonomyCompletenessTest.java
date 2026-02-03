package dk.gormkrings.docs;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.introspect.BeanPropertyDefinition;
import dk.gormkrings.export.ReproducibilityBundleDto;
import dk.gormkrings.statistics.YearlySummary;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.fail;

class YearlySummaryMetricsTaxonomyCompletenessTest {

    private static final Pattern METRIC_TOKEN = Pattern.compile("`([^`]+)`");

    private static final Set<String> NON_METRIC_FIELDS = Set.of(
        "phaseName",
        "year"
    );

    @Test
    void yearlySummaryJsonFieldsArePlacedInTheMetricsTaxonomy() throws Exception {
        Path taxonomyPath = findFileUpwards(Path.of("docs", "data-dictionary", "metrics-taxonomy.md"));
        if (!Files.exists(taxonomyPath)) {
            fail("Missing taxonomy file at %s".formatted(taxonomyPath));
        }

        Path resultsPayloadPath = findFileUpwards(Path.of("docs", "data-dictionary", "results-payload-v3.md"));
        if (!Files.exists(resultsPayloadPath)) {
            fail("Missing results payload doc at %s".formatted(resultsPayloadPath));
        }

        String taxonomy = Files.readString(taxonomyPath, StandardCharsets.UTF_8);
        Set<String> mentioned = parseBacktickedTokens(taxonomy);

        String resultsPayload = Files.readString(resultsPayloadPath, StandardCharsets.UTF_8);
        Set<String> described = parseBacktickedTokens(resultsPayload);

        Set<String> required = new HashSet<>();
        required.addAll(jsonFieldNames(YearlySummary.class));
        required.addAll(jsonFieldNames(ReproducibilityBundleDto.YearlySummary.class));
        required.removeAll(NON_METRIC_FIELDS);

        List<String> missingTaxonomy = required.stream().sorted().filter(f -> !mentioned.contains(f)).toList();
        if (!missingTaxonomy.isEmpty()) {
            fail("Metrics taxonomy is missing yearly-summary fields: %s\nUpdate %s".formatted(missingTaxonomy, taxonomyPath));
        }

        List<String> missingResultsDoc = required.stream().sorted().filter(f -> !described.contains(f)).toList();
        if (!missingResultsDoc.isEmpty()) {
            fail("Results payload doc is missing yearly-summary fields: %s\nUpdate %s".formatted(missingResultsDoc, resultsPayloadPath));
        }
    }

    private static Set<String> jsonFieldNames(Class<?> type) {
        ObjectMapper mapper = new ObjectMapper();
        var javaType = mapper.constructType(type);
        var bean = mapper.getSerializationConfig().introspect(javaType);
        return bean.findProperties().stream().map(BeanPropertyDefinition::getName).collect(java.util.stream.Collectors.toSet());
    }

    private static Set<String> parseBacktickedTokens(String markdown) {
        Set<String> out = new HashSet<>();
        Matcher m = METRIC_TOKEN.matcher(markdown);
        while (m.find()) {
            out.add(m.group(1).trim());
        }
        return out;
    }

    private static Path findFileUpwards(Path relative) {
        Path here = Path.of("").toAbsolutePath();
        for (int i = 0; i < 6; i++) {
            Path candidate = here.resolve(relative).normalize();
            if (Files.exists(candidate)) return candidate;
            here = here.getParent();
            if (here == null) break;
        }
        return relative;
    }
}
