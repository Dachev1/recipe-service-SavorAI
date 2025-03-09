package dev.idachev.recipeservice.mapper;

import dev.idachev.recipeservice.web.dto.MacrosDto;
import dev.idachev.recipeservice.web.dto.RecipeRequest;
import dev.idachev.recipeservice.web.dto.RecipeResponse;
import dev.idachev.recipeservice.model.DifficultyLevel;
import dev.idachev.recipeservice.model.Macros;
import dev.idachev.recipeservice.model.Recipe;

import java.util.ArrayList;
import java.util.HashSet;

/**
 * Utility class for converting between Recipe entities and DTOs.
 */
public final class RecipeMapper {

    private RecipeMapper() {
        // Private constructor to prevent instantiation
    }

    /**
     * Convert a Recipe entity to a RecipeResponse DTO.
     *
     * @param recipe the Recipe entity
     * @return the RecipeResponse DTO
     */
    public static RecipeResponse toResponse(Recipe recipe) {
        if (recipe == null) {
            return null;
        }

        return RecipeResponse.builder()
                .id(recipe.getId())
                .title(recipe.getTitle())
                .description(recipe.getDescription())
                .instructions(recipe.getInstructions())
                .imageUrl(recipe.getImageUrl())
                .ingredients(recipe.getIngredients())
                .tags(recipe.getTags())
                .createdById(recipe.getUserId())
                .createdAt(recipe.getCreatedAt())
                .updatedAt(recipe.getUpdatedAt())
                .totalTimeMinutes(recipe.getTotalTimeMinutes())
                .macros(MacrosMapper.toDto(recipe.getMacros()))
                .difficulty(recipe.getDifficulty())
                .servings(recipe.getServings())
                .isAiGenerated(recipe.getIsAiGenerated())
                .isFavorite(false) // Default value, will be set by service
                .favoriteCount(0L) // Default value, will be set by service
                .build();
    }

    /**
     * Convert a RecipeRequest DTO to a Recipe entity.
     *
     * @param request the RecipeRequest DTO
     * @return the Recipe entity
     */
    public static Recipe toEntity(RecipeRequest request) {
        if (request == null) {
            return null;
        }
        
        Recipe recipe = Recipe.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .instructions(request.getInstructions())
                .imageUrl(request.getImageUrl())
                .ingredients(new ArrayList<>(request.getIngredients()))
                .tags(new HashSet<>(request.getTags()))
                .totalTimeMinutes(request.getTotalTimeMinutes())
                .difficulty(request.getDifficulty())
                .servings(request.getServings())
                .isAiGenerated(false)
                .build();
        
        // Create and associate macros if provided
        if (request.getMacros() != null) {
            Macros macros = MacrosMapper.toEntity(request.getMacros());
            macros.setRecipe(recipe);
            recipe.setMacros(macros);
        }
        
        return recipe;
    }

    /**
     * Update a Recipe entity with data from a RecipeRequest DTO.
     *
     * @param recipe  the Recipe entity to update
     * @param request the RecipeRequest DTO with new data
     */
    public static void updateEntityFromRequest(Recipe recipe, RecipeRequest request) {
        if (recipe == null || request == null) {
            return;
        }
        
        recipe.setTitle(request.getTitle());
        recipe.setDescription(request.getDescription());
        recipe.setInstructions(request.getInstructions());
        recipe.setImageUrl(request.getImageUrl());
        recipe.setIngredients(new ArrayList<>(request.getIngredients()));
        recipe.setTags(new HashSet<>(request.getTags()));
        recipe.setTotalTimeMinutes(request.getTotalTimeMinutes());
        recipe.setDifficulty(request.getDifficulty());
        recipe.setServings(request.getServings());
        
        // Update macros if provided
        if (request.getMacros() != null) {
            if (recipe.getMacros() == null) {
                // Create new macros if not exists
                Macros macros = MacrosMapper.toEntity(request.getMacros());
                macros.setRecipe(recipe);
                recipe.setMacros(macros);
            } else {
                // Update existing macros
                MacrosMapper.updateEntityFromDto(recipe.getMacros(), request.getMacros());
            }
        }
    }
} 