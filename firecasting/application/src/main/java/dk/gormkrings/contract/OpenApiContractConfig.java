package dk.gormkrings.contract;

import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.AnnotatedElementUtils;

import java.lang.reflect.Method;

@Configuration
public class OpenApiContractConfig {

    public static final String PUBLIC_GROUP = "public";

    @Bean
    public GroupedOpenApi publicApi() {
        return GroupedOpenApi.builder()
                .group(PUBLIC_GROUP)
                // Defense-in-depth: only include /api/** paths
                .pathsToMatch("/api/**")
                // Primary gate: only include endpoints explicitly marked PublicApi
                .addOpenApiMethodFilter(OpenApiContractConfig::isPublicApi)
                .build();
    }

    private static boolean isPublicApi(Method method) {
        if (method == null) return false;
        if (AnnotatedElementUtils.hasAnnotation(method, PublicApi.class)) return true;
        Class<?> declaring = method.getDeclaringClass();
        return declaring != null && AnnotatedElementUtils.hasAnnotation(declaring, PublicApi.class);
    }
}
