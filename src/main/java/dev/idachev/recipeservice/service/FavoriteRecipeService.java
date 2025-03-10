package dev.idachev.recipeservice.service;

import dev.idachev.recipeservice.exception.ResourceNotFoundException;
import dev.idachev.recipeservice.mapper.FavoriteRecipeMapper;
import dev.idachev.recipeservice.mapper.RecipeMapper;
import dev.idachev.recipeservice.model.FavoriteRecipe;
import dev.idachev.recipeservice.model.Recipe;
import dev.idachev.recipeservice.repository.FavoriteRecipeRepository;
import dev.idachev.recipeservice.repository.RecipeRepository;
import dev.idachev.recipeservice.web.dto.FavoriteRecipeDto;
import dev.idachev.recipeservice.web.dto.RecipeResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service for managing user's favorite recipes.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class FavoriteRecipeService {

    private final FavoriteRecipeRepository favoriteRecipeRepository;
    private final RecipeRepository recipeRepository;

    /**
     * Add a recipe to user's favorites.
     *
     * @param userId   the ID of the user
     * @param recipeId the ID of the recipe to add to favorites
     * @return the created favorite recipe DTO
     */
    @Transactional
    public FavoriteRecipeDto addToFavorites(UUID userId, UUID recipeId) {
        if (favoriteRecipeRepository.existsByUserIdAndRecipeId(userId, recipeId)) {
            log.info("Recipe {} is already in favorites for user {}", recipeId, userId);
            return getFavoriteRecipeDto(userId, recipeId);
        }

        Recipe recipe = recipeRepository.findById(recipeId)
                .orElseThrow(() -> new ResourceNotFoundException("Recipe not found: " + recipeId));

        FavoriteRecipe favoriteRecipe = FavoriteRecipeMapper.create(userId, recipe);
        favoriteRecipe = favoriteRecipeRepository.save(favoriteRecipe);
        
        log.info("Added recipe {} to favorites for user {}", recipeId, userId);
        
        // Include recipe details in the response
        RecipeResponse recipeResponse = RecipeMapper.toResponse(recipe);
        return FavoriteRecipeMapper.toDto(favoriteRecipe, recipeResponse);
    }

    /**
     * Remove a recipe from user's favorites.
     *
     * @param userId   the ID of the user
     * @param recipeId the ID of the recipe to remove from favorites
     */
    @Transactional
    public void removeFromFavorites(UUID userId, UUID recipeId) {
        FavoriteRecipe favoriteRecipe = favoriteRecipeRepository.findByUserIdAndRecipeId(userId, recipeId)
                .orElseThrow(() -> new ResourceNotFoundException("Recipe not found in favorites"));
        
        favoriteRecipeRepository.delete(favoriteRecipe);
        log.info("Removed recipe {} from favorites for user {}", recipeId, userId);
    }

    /**
     * Get all favorite recipes for a user with pagination.
     *
     * @param userId   the ID of the user
     * @param pageable pagination information
     * @return a page of favorite recipe DTOs
     */
    @Transactional(readOnly = true)
    public Page<FavoriteRecipeDto> getUserFavorites(UUID userId, Pageable pageable) {
        Page<FavoriteRecipe> favoritesPage = favoriteRecipeRepository.findByUserId(userId, pageable);
        
        // Fetch recipes in batch
        List<UUID> recipeIds = favoritesPage.getContent().stream()
                .map(FavoriteRecipe::getRecipeId)
                .collect(Collectors.toList());
        
        Map<UUID, Recipe> recipesMap = recipeRepository.findAllById(recipeIds).stream()
                .collect(Collectors.toMap(Recipe::getId, recipe -> recipe));
        
        return favoritesPage.map(favorite -> {
            Recipe recipe = recipesMap.get(favorite.getRecipeId());
            RecipeResponse recipeResponse = recipe != null ? RecipeMapper.toResponse(recipe) : null;
            return FavoriteRecipeMapper.toDto(favorite, recipeResponse);
        });
    }

    /**
     * Get all favorite recipes for a user.
     *
     * @param userId the ID of the user
     * @return a list of favorite recipe DTOs
     */
    @Transactional(readOnly = true)
    public List<FavoriteRecipeDto> getAllUserFavorites(UUID userId) {
        List<FavoriteRecipe> favorites = favoriteRecipeRepository.findByUserId(userId);
        
        // Fetch recipes in batch for better performance
        List<UUID> recipeIds = favorites.stream()
                .map(FavoriteRecipe::getRecipeId)
                .collect(Collectors.toList());
        
        Map<UUID, Recipe> recipesMap = recipeRepository.findAllById(recipeIds).stream()
                .collect(Collectors.toMap(Recipe::getId, recipe -> recipe));
        
        return favorites.stream()
                .map(favorite -> {
                    Recipe recipe = recipesMap.get(favorite.getRecipeId());
                    RecipeResponse recipeResponse = recipe != null ? RecipeMapper.toResponse(recipe) : null;
                    return FavoriteRecipeMapper.toDto(favorite, recipeResponse);
                })
                .collect(Collectors.toList());
    }

    /**
     * Check if a recipe is in user's favorites.
     *
     * @param userId   the ID of the user
     * @param recipeId the ID of the recipe
     * @return true if the recipe is in user's favorites, false otherwise
     */
    @Transactional(readOnly = true)
    public boolean isRecipeInFavorites(UUID userId, UUID recipeId) {
        return favoriteRecipeRepository.existsByUserIdAndRecipeId(userId, recipeId);
    }

    /**
     * Get the number of users who have favorited a recipe.
     *
     * @param recipeId the ID of the recipe
     * @return the number of users who have favorited the recipe
     */
    @Transactional(readOnly = true)
    public long getFavoriteCount(UUID recipeId) {
        return favoriteRecipeRepository.countByRecipeId(recipeId);
    }
    
    private FavoriteRecipeDto getFavoriteRecipeDto(UUID userId, UUID recipeId) {
        FavoriteRecipe favoriteRecipe = favoriteRecipeRepository.findByUserIdAndRecipeId(userId, recipeId)
                .orElseThrow(() -> new ResourceNotFoundException("Favorite recipe not found"));
        
        Recipe recipe = recipeRepository.findById(recipeId).orElse(null);
        RecipeResponse recipeResponse = recipe != null ? RecipeMapper.toResponse(recipe) : null;
        
        return FavoriteRecipeMapper.toDto(favoriteRecipe, recipeResponse);
    }
} 