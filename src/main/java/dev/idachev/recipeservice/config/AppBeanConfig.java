package dev.idachev.recipeservice.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.AntPathMatcher;

import java.util.concurrent.ConcurrentHashMap;

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
}
