package dev.idachev.recipeservice.service;

import dev.idachev.recipeservice.exception.ResourceNotFoundException;
import dev.idachev.recipeservice.mapper.FavoriteRecipeMapper;
import dev.idachev.recipeservice.model.FavoriteRecipe;
import dev.idachev.recipeservice.model.Recipe;
import dev.idachev.recipeservice.repository.FavoriteRecipeRepository;
import dev.idachev.recipeservice.repository.RecipeRepository;
import dev.idachev.recipeservice.web.dto.FavoriteRecipeDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
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

        FavoriteRecipe favoriteRecipe = FavoriteRecipeMapper.createFavoriteRecipe(userId, recipe);
        favoriteRecipe = favoriteRecipeRepository.save(favoriteRecipe);
        
        log.info("Added recipe {} to favorites for user {}", recipeId, userId);
        return FavoriteRecipeMapper.toDto(favoriteRecipe);
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
        return favoriteRecipeRepository.findByUserId(userId, pageable)
                .map(FavoriteRecipeMapper::toDto);
    }

    /**
     * Get all favorite recipes for a user.
     *
     * @param userId the ID of the user
     * @return a list of favorite recipe DTOs
     */
    @Transactional(readOnly = true)
    public List<FavoriteRecipeDto> getAllUserFavorites(UUID userId) {
        return favoriteRecipeRepository.findByUserId(userId).stream()
                .map(FavoriteRecipeMapper::toDto)
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
        return FavoriteRecipeMapper.toDto(favoriteRecipe);
    }
} 