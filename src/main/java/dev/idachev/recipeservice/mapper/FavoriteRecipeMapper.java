package dev.idachev.recipeservice.mapper;

import dev.idachev.recipeservice.model.FavoriteRecipe;
import dev.idachev.recipeservice.model.Recipe;
import dev.idachev.recipeservice.web.dto.FavoriteRecipeDto;
import dev.idachev.recipeservice.web.dto.RecipeResponse;

import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Utility class for converting between FavoriteRecipe entities and DTOs.
 */
public final class FavoriteRecipeMapper {

    private FavoriteRecipeMapper() {}

    /**
     * Convert a FavoriteRecipe entity to a FavoriteRecipeDto with optional recipe data.
     *
     * @param favoriteRecipe the FavoriteRecipe entity
     * @param recipeResponse the recipe response (optional)
     * @return the FavoriteRecipeDto
     */
    public static FavoriteRecipeDto toDto(FavoriteRecipe favoriteRecipe, RecipeResponse recipeResponse) {
        if (favoriteRecipe == null) {
            return null;
        }

        return FavoriteRecipeDto.builder()
                .recipeId(favoriteRecipe.getRecipeId())
                .userId(favoriteRecipe.getUserId())
                .addedAt(favoriteRecipe.getAddedAt())
                .recipe(recipeResponse)
                .build();
    }
    
    /**
     * Convert a FavoriteRecipe entity to a FavoriteRecipeDto without recipe details.
     *
     * @param favoriteRecipe the FavoriteRecipe entity
     * @return the FavoriteRecipeDto with only the recipe ID
     */
    public static FavoriteRecipeDto toDto(FavoriteRecipe favoriteRecipe) {
        return toDto(favoriteRecipe, null);
    }
    
    /**
     * Convert a list of FavoriteRecipe entities to a list of DTOs.
     *
     * @param favorites the list of favorite recipes
     * @return the list of DTOs
     */
    public static List<FavoriteRecipeDto> toDtoList(List<FavoriteRecipe> favorites) {
        if (favorites == null) {
            return List.of();
        }
        
        return favorites.stream()
                .filter(Objects::nonNull)
                .map(FavoriteRecipeMapper::toDto)
                .collect(Collectors.toList());
    }

    /**
     * Create a new FavoriteRecipe entity.
     *
     * @param userId the ID of the user
     * @param recipeId the ID of the recipe
     * @return the FavoriteRecipe entity
     */
    public static FavoriteRecipe create(UUID userId, UUID recipeId) {
        if (userId == null || recipeId == null) {
            return null;
        }
        
        return FavoriteRecipe.builder()
                .userId(userId)
                .recipeId(recipeId)
                .build();
    }
    
    /**
     * Create a new FavoriteRecipe entity from a Recipe entity.
     *
     * @param userId the ID of the user
     * @param recipe the Recipe entity
     * @return the FavoriteRecipe entity
     */
    public static FavoriteRecipe create(UUID userId, Recipe recipe) {
        return recipe == null ? null : create(userId, recipe.getId());
    }
} 