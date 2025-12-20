package dk.gormkrings.ui.forms;

import org.springframework.core.io.ClassPathResource;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StreamUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/api/forms")
public class FormsController {

    @GetMapping(value = "/{formId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> getForm(@PathVariable String formId) {
        // For now we serve JSON configs from classpath resources.
        // This keeps the frontend config backend-controlled without introducing new dependencies.
        String path = "forms/" + formId + ".json";

        try {
            var res = new ClassPathResource(path);
            if (!res.exists()) {
                return ResponseEntity.notFound().build();
            }
            String json = StreamUtils.copyToString(res.getInputStream(), StandardCharsets.UTF_8);
            return ResponseEntity.ok()
                    .cacheControl(CacheControl.maxAge(5, TimeUnit.MINUTES).cachePublic())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(json);
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body("{\"error\":\"Failed to load form config\"}");
        }
    }
}
