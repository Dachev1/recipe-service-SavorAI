package dev.idachev.recipeservice.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.AntPathMatcher;

import java.util.concurrent.ConcurrentHashMap;

@Configuration
public class AppBeanConfig {

    @Bean
    public AntPathMatcher antPathMatcher() {
        return new AntPathMatcher();
    }

    @Bean
    public ConcurrentHashMap<String, Long> stringLongConcurrentHashMap() {
        return new ConcurrentHashMap<>();
    }
}
