package dev.idachev.recipeservice.web.dto;

import dev.idachev.recipeservice.model.DifficultyLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Data Transfer Object for AI-generated meal/recipe responses.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GeneratedMealResponse {
    
    private String title;
    private String description;
    private String instructions;
    private List<String> ingredients;
    private List<String> tags;
    private String imageUrl;
    
    // Total cooking time (minutes)
    private Integer totalTimeMinutes;
    
    // Nutritional information (macros)
    private MacrosDto macros;
    
    // Additional metadata
    private DifficultyLevel difficulty;
    private Integer servings;
    private RecipeRequest recipe; // Full recipe data
} 