package dev.idachev.recipeservice.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.security.web.method.annotation.CurrentSecurityContextArgumentResolver;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;

import java.util.Collections;
import java.util.concurrent.ConcurrentHashMap;
import java.util.UUID;

@Configuration
@EnableConfigurationProperties({
        CloudinaryProperties.class,
        JwtProperties.class,
        CorsProperties.class
})
public class AppBeanConfig {

    @Bean
    public AntPathMatcher antPathMatcher() {
        return new AntPathMatcher();
    }

    @Bean("rateLimitOrPurposeSpecificMap")
    public ConcurrentHashMap<String, Long> purposeSpecificMap() {
        return new ConcurrentHashMap<>();
    }

    /**
     * Token blacklist for tracking revoked tokens
     * Key: token, Value: expiration time in milliseconds
     */
    @Bean
    public ConcurrentHashMap<String, Long> tokenBlacklist() {
        return new ConcurrentHashMap<>();
    }
}
