package dev.idachev.recipeservice.mapper;

import dev.idachev.recipeservice.model.FavoriteRecipe;
import dev.idachev.recipeservice.model.Recipe;
import dev.idachev.recipeservice.web.dto.FavoriteRecipeDto;
import dev.idachev.recipeservice.web.dto.RecipeResponse;
import lombok.experimental.UtilityClass;

import java.util.Objects;
import java.util.UUID;

/**
 * Mapper for favorite recipe transformations.
 */
@UtilityClass
public class FavoriteRecipeMapper {


    /**
     * Creates a new FavoriteRecipe entity.
     */
    public static FavoriteRecipe create(UUID userId, UUID recipeId) {
        Objects.requireNonNull(userId, "User ID cannot be null");
        Objects.requireNonNull(recipeId, "Recipe ID cannot be null");

        return FavoriteRecipe.builder()
                .userId(userId)
                .recipeId(recipeId)
                .build();
    }

    /**
     * Creates a new FavoriteRecipe entity from a Recipe entity.
     */
    public static FavoriteRecipe create(UUID userId, Recipe recipe) {
        Objects.requireNonNull(userId, "User ID cannot be null");
        Objects.requireNonNull(recipe, "Recipe cannot be null");

        return FavoriteRecipe.builder()
                .userId(userId)
                .recipeId(recipe.getId())
                .build();
    }

    /**
     * Converts a FavoriteRecipe entity and Recipe entity to a complete FavoriteRecipeDto.
     */
    public static FavoriteRecipeDto toDtoWithRecipe(FavoriteRecipe favoriteRecipe, Recipe recipe, RecipeMapper recipeMapper) {
        Objects.requireNonNull(favoriteRecipe, "FavoriteRecipe cannot be null");
        Objects.requireNonNull(recipe, "Recipe cannot be null");
        Objects.requireNonNull(recipeMapper, "RecipeMapper cannot be null");

        RecipeResponse recipeResponse = recipeMapper.toResponse(recipe);

        return FavoriteRecipeDto.builder()
                .recipeId(favoriteRecipe.getRecipeId())
                .userId(favoriteRecipe.getUserId())
                .addedAt(favoriteRecipe.getCreatedAt())
                .recipe(recipeResponse)
                .build();
    }
} 