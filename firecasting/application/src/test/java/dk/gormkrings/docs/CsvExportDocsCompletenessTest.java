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

class CsvExportDocsCompletenessTest {

    private static final Pattern DOC_ENTRY = Pattern.compile("^\\s*-\\s*`([^`]+)`\\s*:", Pattern.MULTILINE);

    @Test
    void everyCsvHeaderHasADocEntry() throws Exception {
        List<String> headers = CsvExporter.headers();

        Path docsPath = findFileUpwards(Path.of("docs", "data-dictionary", "csv-export.md"));
        if (!Files.exists(docsPath)) {
            fail("Missing docs file at %s".formatted(docsPath));
        }

        String docs = Files.readString(docsPath, StandardCharsets.UTF_8);
        Set<String> documented = parseDocumentedColumns(docs);

        List<String> missing = headers.stream().filter(h -> !documented.contains(h)).toList();
        if (!missing.isEmpty()) {
            fail("CSV docs are missing entries for: %s\nUpdate %s".formatted(missing, docsPath));
        }
    }

    private static Set<String> parseDocumentedColumns(String docs) {
        Set<String> out = new HashSet<>();
        Matcher m = DOC_ENTRY.matcher(docs);
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
