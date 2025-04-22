package dev.idachev.recipeservice.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Type-safe configuration properties for JWT settings.
 * Bound to properties starting with 'jwt'.
 */
@ConfigurationProperties(prefix = "jwt")
@Validated
public record JwtProperties(

        // For HS384, requires minimum 384 bits (48 bytes/chars in UTF-8)
        @NotBlank(message = "JWT secret must be configured")
        @Size(min = 48, message = "JWT secret must be at least 48 characters for HS384")
        String secret
) {
}