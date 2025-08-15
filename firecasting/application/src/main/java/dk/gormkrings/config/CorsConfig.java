package dk.gormkrings.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class CorsConfig {

  // Read from env var you set in docker-compose (comma-separated list supported)
  @Value("${SPRING_WEB_CORS_ALLOWED_ORIGINS:https://fire.local.test}")
  private String allowedOrigins;

  @Bean
  public WebMvcConfigurer corsConfigurer() {
    return new WebMvcConfigurer() {
      @Override
      public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
            .allowedOrigins(allowedOrigins.split("\\s*,\\s*"))
            .allowedMethods("GET","POST","PUT","DELETE","OPTIONS","PATCH")
            .allowedHeaders("*")
            .exposedHeaders("Content-Disposition")
            .allowCredentials(false)   // set true only if you actually send cookies/Authorization
            .maxAge(3600);
      }
    };
  }
}
