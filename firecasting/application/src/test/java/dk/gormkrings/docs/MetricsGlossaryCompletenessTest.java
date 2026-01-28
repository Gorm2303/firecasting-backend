package dk.gormkrings.docs;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.fail;

class MetricsGlossaryCompletenessTest {

    private static final Pattern METRIC_TOKEN = Pattern.compile("`([^`]+)`");
    private static final Pattern METRIC_NAME = Pattern.compile("^[a-z][a-zA-Z0-9]*(?:-[a-zA-Z0-9]+)*$");

    @Test
    void everyTaxonomyMetricHasAGlossaryDefinitionWithTags() throws Exception {
        Path taxonomyPath = findFileUpwards(Path.of("docs", "data-dictionary", "metrics-taxonomy.md"));
        if (!Files.exists(taxonomyPath)) {
            fail("Missing taxonomy file at %s".formatted(taxonomyPath));
        }

        Path glossaryPath = findFileUpwards(Path.of("docs", "data-dictionary", "glossary.md"));
        if (!Files.exists(glossaryPath)) {
            fail("Missing glossary file at %s".formatted(glossaryPath));
        }

        String taxonomy = Files.readString(taxonomyPath, StandardCharsets.UTF_8);
        Set<String> metrics = parseBacktickedTokens(taxonomy);

        String glossary = Files.readString(glossaryPath, StandardCharsets.UTF_8);

        Set<String> missing = new HashSet<>();
        Set<String> missingTags = new HashSet<>();
        for (String metric : metrics) {
            Pattern entry = Pattern.compile("(?m)^- `" + Pattern.quote(metric) + "`:.*$");
            Matcher m = entry.matcher(glossary);
            if (!m.find()) {
                missing.add(metric);
                continue;
            }

            String line = m.group(0);
            if (!line.contains("Provenance:") || !line.contains("Storage:")) {
                missingTags.add(metric);
            }
        }

        if (!missing.isEmpty()) {
            fail("Glossary is missing entries for metrics: %s\nUpdate %s".formatted(missing.stream().sorted().toList(), glossaryPath));
        }
        if (!missingTags.isEmpty()) {
            fail("Glossary entries missing Provenance/Storage tags for: %s\nUpdate %s".formatted(missingTags.stream().sorted().toList(), glossaryPath));
        }
    }

    private static Set<String> parseBacktickedTokens(String markdown) {
        Set<String> out = new HashSet<>();
        Matcher m = METRIC_TOKEN.matcher(markdown);
        while (m.find()) {
            String token = m.group(1).trim();
            if (METRIC_NAME.matcher(token).matches()) {
                out.add(token);
            }
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
