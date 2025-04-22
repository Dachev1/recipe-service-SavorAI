package dev.idachev.recipeservice.web.mapper;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.idachev.recipeservice.exception.ValidationException;
import dev.idachev.recipeservice.model.DifficultyLevel;
import dev.idachev.recipeservice.model.Recipe;
import dev.idachev.recipeservice.web.dto.MacrosDto;
import dev.idachev.recipeservice.web.dto.RecipeRequest;
import dev.idachev.recipeservice.web.dto.RecipeResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Arrays;

/**
 * Mapper utility for recipe transformations.
 * Provides methods for converting between Recipe entities and DTOs.
 */
@Component
@Slf4j
public class RecipeMapper {

    private final ObjectMapper objectMapper;
    private static final TypeReference<List<String>> INGREDIENTS_TYPE = new TypeReference<>() {
    };

    @Autowired
    public RecipeMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        log.debug("ObjectMapper initialized in RecipeMapper");
    }

    /**
     * Converts a Recipe entity to a RecipeResponse DTO using the record constructor.
     *
     * @param recipe The Recipe entity to convert
     * @return The corresponding RecipeResponse DTO
     * @throws IllegalArgumentException if recipe is null
     */
    public RecipeResponse toResponse(Recipe recipe) {
        if (recipe == null) {
            throw new IllegalArgumentException("Cannot convert null recipe to RecipeResponse");
        }

        List<String> ingredientsList = parseIngredients(recipe.getIngredients());
        MacrosDto macrosDto = (recipe.getMacros() != null) ? MacrosMapper.toDto(recipe.getMacros()) : null;
        DifficultyLevel difficulty = recipe.getDifficulty();

        return new RecipeResponse(
                recipe.getId(),
                recipe.getUserId(),
                recipe.getTitle(),
                recipe.getServingSuggestions(),
                recipe.getInstructions(),
                recipe.getImageUrl(),
                ingredientsList,
                recipe.getTotalTimeMinutes(),
                null,
                null,
                null,
                difficulty,
                recipe.getIsAiGenerated(),
                null,
                null,
                null,
                recipe.getUpvotes(),
                recipe.getDownvotes(),
                null,
                recipe.getCreatedAt(),
                recipe.getUpdatedAt(),
                macrosDto,
                null
        );
    }

    /**
     * Converts a RecipeRequest DTO to a Recipe entity using the builder.
     *
     * @param request The RecipeRequest DTO to convert
     * @return The corresponding Recipe entity
     * @throws IllegalArgumentException if request is null
     * @throws ValidationException      if JSON serialization fails
     */
    public Recipe toEntity(RecipeRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Cannot convert null request to Recipe entity");
        }

        return Recipe.builder()
                .title(request.title())
                .servingSuggestions(request.servingSuggestions())
                .instructions(request.instructions())
                .imageUrl(request.imageUrl())
                .ingredients(serializeIngredients(request.ingredients()))
                .totalTimeMinutes(request.totalTimeMinutes())
                .difficulty(request.difficulty())
                .isAiGenerated(Optional.ofNullable(request.isAiGenerated()).orElse(false))
                .macros(MacrosMapper.toEntity(request.macros()))
                .build();
    }

    /**
     * Serializes the list of ingredients into a JSON array string.
     * @param ingredients List of ingredients
     * @return JSON string representation, or null if input is null/empty.
     */
    private String serializeIngredients(List<String> ingredients) {
        if (ingredients == null || ingredients.isEmpty()) {
            return null; // Store null or empty string based on DB preference, null seems better
        }
        try {
            return objectMapper.writeValueAsString(ingredients);
        } catch (Exception e) {
            log.error("Failed to serialize ingredients list to JSON: {}", ingredients, e);
            // Decide on fallback: null, empty string, or throw exception? Throwing seems safest here.
            throw new ValidationException("Failed to serialize ingredients list", e);
        }
    }

    /**
     * Parses the JSON array string representation of ingredients into a List.
     * @param ingredientsJson JSON string from the database.
     * @return List of ingredients, or empty list if input is null/blank or parsing fails.
     */
    private List<String> parseIngredients(String ingredientsJson) {
        if (ingredientsJson == null || ingredientsJson.isBlank()) {
            return Collections.emptyList();
        }
        try {
            // Directly parse assuming it's a JSON array
            return objectMapper.readValue(ingredientsJson, INGREDIENTS_TYPE);
        } catch (Exception e) {
            log.warn("Failed to parse ingredients JSON string: '{}'. Returning empty list. Error: {}", 
                     ingredientsJson, e.getMessage());
            // Return empty list on parse failure to avoid errors downstream
            return Collections.emptyList(); 
        }
        // Removed fallback comma split logic - we now expect JSON.
    }
}