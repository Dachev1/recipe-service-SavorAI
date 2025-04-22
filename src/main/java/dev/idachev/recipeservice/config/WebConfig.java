package dev.idachev.recipeservice.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.lang.NonNull;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web MVC configuration including CORS settings.
 * Uses type-safe CorsProperties.
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final CorsProperties corsProperties;

    @Autowired // Inject properties bean
    public WebConfig(CorsProperties corsProperties) {
        this.corsProperties = corsProperties;
    }

    @Override
    public void addCorsMappings(@NonNull CorsRegistry registry) {
        // Use properties directly from the bean
        registry.addMapping("/**")
                .allowedOrigins(corsProperties.allowedOrigins().toArray(new String[0])) // Convert List to String[]
                .allowedMethods(corsProperties.allowedMethods().toArray(new String[0]))
                .allowedHeaders(corsProperties.allowedHeaders().toArray(new String[0]))
                .allowCredentials(corsProperties.allowCredentials())
                .maxAge(3600); // maxAge could also be configurable
    }
} 