package dk.gormkrings.config;

import org.springframework.core.env.Environment;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {
  @Bean
  SecurityFilterChain security(HttpSecurity http, Environment env) throws Exception {
    boolean exposeOpenApi = Boolean.parseBoolean(env.getProperty("settings.openapi.expose", "false"));

    return http
        .csrf(csrf -> csrf.disable())                 // dev API; adjust if you use cookies/forms
        .cors(Customizer.withDefaults())              // << enable CORS using your WebMvcConfigurer
        .authorizeHttpRequests(auth -> auth
            .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()  // allow preflight
            // Belt-and-suspenders: even if springdoc is accidentally enabled, don't expose docs unless explicitly allowed.
            .requestMatchers(
                "/v3/api-docs/**",
                "/swagger-ui/**",
                "/swagger-ui.html"
            ).access((authentication, context) -> new org.springframework.security.authorization.AuthorizationDecision(exposeOpenApi))
            .anyRequest().permitAll())
        .build();
  }
}
