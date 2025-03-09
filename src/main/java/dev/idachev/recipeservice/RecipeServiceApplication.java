package dev.idachev.recipeservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * Main application class for the Recipe Service.
 * This service handles recipe management, recipe generation with AI,
 * and recipe-related operations.
 */
@SpringBootApplication
@EnableDiscoveryClient
public class RecipeServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(RecipeServiceApplication.class, args);
    }
} 