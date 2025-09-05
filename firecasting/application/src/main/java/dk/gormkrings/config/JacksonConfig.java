// JacksonConfig.java
package dk.gormkrings.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JacksonConfig {

    @Bean(name = "canonicalObjectMapper")
    public ObjectMapper canonicalObjectMapper() {
        return JsonMapper.builder()
                // Sort POJO properties alphabetically
                .configure(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY, true)
                // Sort Map keys
                .configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true)
                .build();
    }
}
