package dev.idachev.recipeservice.web.mapper;

import dev.idachev.recipeservice.model.DifficultyLevel;
import dev.idachev.recipeservice.web.dto.MacrosDto;
import dev.idachev.recipeservice.web.dto.RecipeRequest;
import dev.idachev.recipeservice.web.dto.SimplifiedRecipeResponse;
import lombok.experimental.UtilityClass;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Mapper for AI-related data transformations.
 * Provides methods for converting between AI-generated recipe data and response objects.
 */
@UtilityClass
public class AIServiceMapper {

    /**
     * Maps a RecipeRequest to a SimplifiedRecipeResponse.
     *
     * @param recipe   the RecipeRequest from AI generation
     * @param imageUrl the generated image URL
     * @return the SimplifiedRecipeResponse
     */
    public static SimplifiedRecipeResponse toSimplifiedResponse(RecipeRequest recipe, String imageUrl) {
        Objects.requireNonNull(recipe, "Recipe cannot be null");

        return new SimplifiedRecipeResponse(
            recipe.title(),
            null,
            recipe.instructions(),
            recipe.ingredients() != null ? List.copyOf(recipe.ingredients()) : Collections.emptyList(),
            imageUrl,
            recipe.totalTimeMinutes(),
            extractMacros(recipe),
            recipe.difficulty(),
            recipe.servingSuggestions()
        );
    }

    /**
     * Extracts macro nutrients with null safety.
     *
     * @param recipe the RecipeRequest containing macros data
     * @return a MacrosDto with nutritional information, or null if input macros are null
     */
    private static MacrosDto extractMacros(RecipeRequest recipe) {
        if (recipe == null || recipe.macros() == null) {
            return null;
        }
        MacrosDto sourceMacros = recipe.macros();
        return new MacrosDto(
            sourceMacros.calories(),
            sourceMacros.proteinGrams(),
            sourceMacros.carbsGrams(),
            sourceMacros.fatGrams()
        );
    }
} 