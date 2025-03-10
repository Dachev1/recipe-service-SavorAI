package dev.idachev.recipeservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Main application class for the Recipe Service.
 * This service handles recipe management, recipe generation with AI,
 * and recipe-related operations.
 */
@SpringBootApplication
public class RecipeServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(RecipeServiceApplication.class, args);
    }
} 