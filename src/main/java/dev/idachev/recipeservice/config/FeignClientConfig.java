package dev.idachev.recipeservice.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;

import dev.idachev.recipeservice.util.JwtUtil;
import feign.RequestInterceptor;
import lombok.extern.slf4j.Slf4j;

/**
 * Configuration for Feign clients to handle service-to-service authentication.
 * Automatically adds JWT authentication tokens to Feign client requests
 * when no explicit Authorization header is provided.
 */
@Configuration
@Slf4j
public class FeignClientConfig {

    private final JwtUtil jwtUtil;

    public FeignClientConfig(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    /**
     * Creates a RequestInterceptor that adds service authentication tokens to Feign requests.
     * This enables automatic authentication for service-to-service communication.
     */
    @Bean
    public RequestInterceptor serviceAuthRequestInterceptor() {
        return requestTemplate -> {
            // Skip if Authorization header is already set (e.g., for user-authenticated requests)
            if (requestTemplate.headers().containsKey(HttpHeaders.AUTHORIZATION)) {
                return;
            }
            
            // Add service JWT token for service-to-service authentication
            String serviceToken = "Bearer " + jwtUtil.generateServiceToken();
            requestTemplate.header(HttpHeaders.AUTHORIZATION, serviceToken);
            
            log.debug("Added service authentication token to {}", requestTemplate.path());
        };
    }
} 