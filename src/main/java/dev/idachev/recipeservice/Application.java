package dev.idachev.recipeservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.PropertySource;

/**
 * Main application class for the Recipe Service.
 * This service handles recipe management, recipe generation with AI,
 * and recipe-related operations.
 */
@SpringBootApplication
@EnableFeignClients
@PropertySource({"classpath:application.yml", "classpath:.env.properties"})
public class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}