package dev.idachev.recipeservice.mapper;

import dev.idachev.recipeservice.model.FavoriteRecipe;
import dev.idachev.recipeservice.model.Recipe;
import dev.idachev.recipeservice.web.dto.FavoriteRecipeDto;

import java.util.UUID;

/**
 * Utility class for converting between FavoriteRecipe entities and DTOs.
 */
public final class FavoriteRecipeMapper {

    private FavoriteRecipeMapper() {}

    /**
     * Convert a FavoriteRecipe entity to a FavoriteRecipeDto.
     *
     * @param favoriteRecipe the FavoriteRecipe entity
     * @return the FavoriteRecipeDto
     */
    public static FavoriteRecipeDto toDto(FavoriteRecipe favoriteRecipe) {
        if (favoriteRecipe == null) {
            return null;
        }

        return FavoriteRecipeDto.builder()
                .recipeId(favoriteRecipe.getRecipeId())
                .userId(favoriteRecipe.getUserId())
                .addedAt(favoriteRecipe.getAddedAt())
                .recipe(RecipeMapper.toResponse(favoriteRecipe.getRecipe()))
                .build();
    }

    /**
     * Create a new FavoriteRecipe entity.
     *
     * @param userId the ID of the user
     * @param recipe the Recipe entity
     * @return the FavoriteRecipe entity
     */
    public static FavoriteRecipe createFavoriteRecipe(UUID userId, Recipe recipe) {
        if (userId == null || recipe == null) {
            return null;
        }
        
        return FavoriteRecipe.builder()
                .userId(userId)
                .recipeId(recipe.getId())
                .recipe(recipe)
                .build();
    }
} 