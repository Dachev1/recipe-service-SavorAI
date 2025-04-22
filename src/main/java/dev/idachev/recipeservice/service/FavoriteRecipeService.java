package dev.idachev.recipeservice.service;

import dev.idachev.recipeservice.exception.ResourceNotFoundException;
import dev.idachev.recipeservice.model.FavoriteRecipe;
import dev.idachev.recipeservice.model.Recipe;
import dev.idachev.recipeservice.repository.FavoriteRecipeRepository;
import dev.idachev.recipeservice.repository.RecipeRepository;
import dev.idachev.recipeservice.web.dto.FavoriteRecipeDto;
import dev.idachev.recipeservice.web.mapper.FavoriteRecipeMapper;
import dev.idachev.recipeservice.web.mapper.RecipeMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Service for managing favorite recipes.
 */
@Service
@Slf4j
public class FavoriteRecipeService {

    private final FavoriteRecipeRepository favoriteRecipeRepository;
    private final RecipeRepository recipeRepository;
    private final RecipeMapper recipeMapper;

    @Autowired
    public FavoriteRecipeService(FavoriteRecipeRepository favoriteRecipeRepository,
                                 RecipeRepository recipeRepository,
                                 RecipeMapper recipeMapper) {
        this.favoriteRecipeRepository = favoriteRecipeRepository;
        this.recipeRepository = recipeRepository;
        this.recipeMapper = recipeMapper;
    }

    /**
     * Add a recipe to user's favorites.
     */
    @Transactional
    public FavoriteRecipeDto addToFavorites(UUID userId, UUID recipeId) {
        // Check if already in favorites
        if (favoriteRecipeRepository.existsByUserIdAndRecipeId(userId, recipeId)) {
            log.info("Recipe {} already in favorites for user {}", recipeId, userId);
            return getFavoriteRecipeDto(userId, recipeId);
        }

        Recipe recipe = findRecipeByIdOrThrow(recipeId);

        FavoriteRecipe favoriteRecipe = FavoriteRecipe.builder()
                                            .userId(userId)
                                            .recipeId(recipeId)
                                            .build();

        FavoriteRecipe savedFavorite = favoriteRecipeRepository.save(favoriteRecipe);
        log.info("Added recipe {} to favorites for user {}", recipeId, userId);

        return FavoriteRecipeMapper.toDtoWithRecipe(savedFavorite, recipe, recipeMapper);
    }

    private Recipe findRecipeByIdOrThrow(UUID recipeId) {
        return recipeRepository.findById(recipeId)
                .orElseThrow(() -> new ResourceNotFoundException("Recipe not found with id: " + recipeId));
    }

    private FavoriteRecipeDto getFavoriteRecipeDto(UUID userId, UUID recipeId) {
        FavoriteRecipe favorite = findFavoriteByUserAndRecipeOrThrow(userId, recipeId);
        Recipe recipe = findRecipeByIdOrThrow(recipeId);
        return FavoriteRecipeMapper.toDtoWithRecipe(favorite, recipe, recipeMapper);
    }

    private FavoriteRecipe findFavoriteByUserAndRecipeOrThrow(UUID userId, UUID recipeId) {
        return favoriteRecipeRepository.findByUserIdAndRecipeId(userId, recipeId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Favorite not found for user " + userId + " and recipe " + recipeId));
    }

    /**
     * Remove a recipe from user's favorites.
     */
    @Transactional
    public void removeFromFavorites(UUID userId, UUID recipeId) {
        FavoriteRecipe favorite = findFavoriteByUserAndRecipeOrThrow(userId, recipeId);
        favoriteRecipeRepository.delete(favorite);
        log.info("Removed recipe {} from favorites for user {}", recipeId, userId);
    }

    /**
     * Get user's favorite recipes with pagination.
     */
    @Transactional(readOnly = true)
    public Page<FavoriteRecipeDto> getUserFavorites(UUID userId, Pageable pageable) {
        Page<FavoriteRecipe> favorites = favoriteRecipeRepository.findByUserId(userId, pageable);

        List<UUID> recipeIds = extractRecipeIds(favorites.getContent());

        Map<UUID, Recipe> recipesMap = recipeIds.isEmpty()
                ? Collections.emptyMap()
                : getRecipesMapFromIds(recipeIds);

        return favorites.map(favorite -> mapFavoriteToDto(favorite, recipesMap));
    }

    private List<UUID> extractRecipeIds(List<FavoriteRecipe> favorites) {
        return favorites.stream()
                .map(FavoriteRecipe::getRecipeId)
                .toList();
    }

    private Map<UUID, Recipe> getRecipesMapFromIds(List<UUID> recipeIds) {
        return recipeRepository.findAllById(recipeIds).stream()
                .collect(Collectors.toMap(Recipe::getId, Function.identity()));
    }

    private FavoriteRecipeDto mapFavoriteToDto(FavoriteRecipe favorite, Map<UUID, Recipe> recipesMap) {
        Recipe recipe = recipesMap.getOrDefault(favorite.getRecipeId(),
                Recipe.builder().id(favorite.getRecipeId()).title("Unknown Recipe").build());

        return FavoriteRecipeMapper.toDtoWithRecipe(favorite, recipe, recipeMapper);
    }

    /**
     * Get all user's favorite recipes without pagination.
     */
    @Transactional(readOnly = true)
    public List<FavoriteRecipeDto> getAllUserFavorites(UUID userId) {
        List<FavoriteRecipe> favorites = favoriteRecipeRepository.findByUserId(userId);

        if (favorites.isEmpty()) {
            return Collections.emptyList();
        }

        List<UUID> recipeIds = extractRecipeIds(favorites);
        Map<UUID, Recipe> recipesMap = getRecipesMapFromIds(recipeIds);

        return favorites.stream()
                .map(favorite -> mapFavoriteToDto(favorite, recipesMap))
                .toList();
    }

    /**
     * Check if a recipe is in user's favorites.
     */
    @Transactional(readOnly = true)
    public boolean isRecipeInFavorites(UUID userId, UUID recipeId) {
        return favoriteRecipeRepository.existsByUserIdAndRecipeId(userId, recipeId);
    }

    /**
     * Get favorite count for a recipe.
     */
    @Transactional(readOnly = true)
    public long getFavoriteCount(UUID recipeId) {
        return favoriteRecipeRepository.countByRecipeId(recipeId);
    }
}