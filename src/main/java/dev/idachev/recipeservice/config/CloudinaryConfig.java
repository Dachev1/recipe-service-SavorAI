package dev.idachev.recipeservice.config;

import com.cloudinary.Cloudinary;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

/**
 * Configuration for Cloudinary image storage service.
 * Uses type-safe CloudinaryProperties.
 */
@Configuration
public class CloudinaryConfig {

    private final CloudinaryProperties cloudinaryProperties;

    @Autowired
    public CloudinaryConfig(CloudinaryProperties cloudinaryProperties) {
        this.cloudinaryProperties = cloudinaryProperties;
    }

    @Bean
    public Cloudinary cloudinary() {
        return new Cloudinary(Map.of(
                "cloud_name", cloudinaryProperties.cloudName(),
                "api_key", cloudinaryProperties.apiKey(),
                "api_secret", cloudinaryProperties.apiSecret(),
                "secure", true
        ));
    }
} 