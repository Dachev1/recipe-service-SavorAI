package dev.idachev.recipeservice.web.dto;

import dev.idachev.recipeservice.model.DifficultyLevel;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/**
 * Immutable simplified recipe response using Java Record.
 * Used for AI-generated recipe suggestions and search previews.
 */
@Schema(description = "Simplified recipe data for AI-generated suggestions and previews")
public record SimplifiedRecipeResponse(
    @Schema(description = "Recipe title", example = "Spaghetti Carbonara")
    String title,
    
    // TODO: Clarify source and persistence of 'description'. Is it AI-generated only? Should it be added to Recipe entity?
    @Schema(description = "Recipe description", example = "Classic Italian pasta dish with eggs, cheese, pancetta, and pepper")
    String description,
    
    @Schema(description = "Step-by-step cooking instructions", example = "1. Boil pasta until al dente\n2. In a separate pan, cook pancetta...")
    String instructions,
    
    @Schema(description = "List of ingredients required for the recipe", example = "[\"200g spaghetti\", \"100g pancetta\", \"2 large eggs\"]")
    List<String> ingredients,
    
    @Schema(description = "URL to recipe image", example = "https://example.com/images/carbonara.jpg")
    String imageUrl,
    
    @Schema(description = "Total preparation and cooking time in minutes", example = "30")
    Integer totalTimeMinutes,
    
    @Schema(description = "Nutritional information for the recipe")
    MacrosDto macros,
    
    @Schema(description = "Recipe difficulty level", example = "MEDIUM")
    DifficultyLevel difficulty, 
    
    @Schema(description = "Serving suggestions including garnishes, sides, and pairings", example = "Serve hot with a sprinkle of fresh parsley and a glass of white wine")
    String servingSuggestions,
    
    @Schema(description = "Recipe ID", example = "12345")
    String recipeId
) {}