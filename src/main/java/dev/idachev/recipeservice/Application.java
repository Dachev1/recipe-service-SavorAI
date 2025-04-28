package dev.idachev.recipeservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.annotation.PropertySources;

/**
 * Main application class for the Recipe Service.
 * This service handles recipe management, recipe generation with AI,
 * and recipe-related operations.
 */
@SpringBootApplication
@EnableFeignClients
@PropertySources({
    @PropertySource("classpath:application.yml"),
    @PropertySource(value = "classpath:.env.properties", ignoreResourceNotFound = true)
})
public class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}