package dev.idachev.recipeservice.config;

import jakarta.validation.constraints.NotEmpty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.util.List;

/**
 * Type-safe configuration properties for CORS settings.
 * Bound to properties starting with 'cors'.
 */
@ConfigurationProperties(prefix = "cors")
@Validated
public record CorsProperties(

        @NotEmpty(message = "CORS allowed origins cannot be empty")
        List<String> allowedOrigins, // Bind directly to list/array from YAML

        @NotEmpty(message = "CORS allowed methods cannot be empty")
        List<String> allowedMethods,

        @NotEmpty(message = "CORS allowed headers cannot be empty")
        List<String> allowedHeaders,

        boolean allowCredentials // Default is false if not specified
) {
}