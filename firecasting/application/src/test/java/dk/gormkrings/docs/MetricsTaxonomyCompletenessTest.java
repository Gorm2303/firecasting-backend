package dk.gormkrings.docs;

import dk.gormkrings.simulation.util.CsvExporter;
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

class MetricsTaxonomyCompletenessTest {

    private static final Pattern METRIC_TOKEN = Pattern.compile("`([^`]+)`");

    @Test
    void everyCsvHeaderIsPlacedInTheMetricsTaxonomy() throws Exception {
        List<String> headers = CsvExporter.headers();

        Path taxonomyPath = findFileUpwards(Path.of("docs", "data-dictionary", "metrics-taxonomy.md"));
        if (!Files.exists(taxonomyPath)) {
            fail("Missing taxonomy file at %s".formatted(taxonomyPath));
        }

        String taxonomy = Files.readString(taxonomyPath, StandardCharsets.UTF_8);
        Set<String> mentioned = parseBacktickedTokens(taxonomy);

        List<String> missing = headers.stream().filter(h -> !mentioned.contains(h)).toList();
        if (!missing.isEmpty()) {
            fail("Metrics taxonomy is missing entries for: %s\nUpdate %s".formatted(missing, taxonomyPath));
        }
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
