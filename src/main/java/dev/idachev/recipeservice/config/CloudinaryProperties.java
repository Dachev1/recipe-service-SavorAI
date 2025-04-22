package dev.idachev.recipeservice.config;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Type-safe configuration properties for Cloudinary integration.
 * Bound to properties starting with 'cloudinary'.
 */
@ConfigurationProperties(prefix = "cloudinary")
@Validated // Enable validation of properties
public record CloudinaryProperties(
        @NotBlank(message = "Cloudinary cloud name must be configured")
        String cloudName,

        @NotBlank(message = "Cloudinary API key must be configured")
        String apiKey,

        @NotBlank(message = "Cloudinary API secret must be configured")
        String apiSecret
) {
}