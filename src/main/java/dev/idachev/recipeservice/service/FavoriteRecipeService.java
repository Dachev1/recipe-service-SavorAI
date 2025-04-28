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
import java.util.Set;
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
        log.debug("Getting favorite count for recipe with ID: {}", recipeId);
        return favoriteRecipeRepository.countByRecipeId(recipeId);
    }

    /**
     * Adds multiple recipes to user's favorites.
     *
     * @param userId    the user ID
     * @param recipeIds list of recipe IDs to add to favorites
     * @return number of recipes successfully added to favorites
     */
    @Transactional
    public int addBatchToFavorites(UUID userId, List<UUID> recipeIds) {
        log.debug("Adding batch of recipes to favorites for user with ID: {}", userId);
        
        int addedCount = 0;
        
        for (UUID recipeId : recipeIds) {
            try {
                // Check if recipe exists
                if (!recipeRepository.existsById(recipeId)) {
                    log.warn("Recipe with ID {} not found when adding to favorites", recipeId);
                    continue;
                }
                
                // Check if already in favorites
                if (favoriteRecipeRepository.existsByUserIdAndRecipeId(userId, recipeId)) {
                    log.debug("Recipe with ID {} is already in favorites for user {}", recipeId, userId);
                    continue;
                }
                
                // Add to favorites using builder
                FavoriteRecipe favoriteRecipe = FavoriteRecipe.builder()
                    .userId(userId)
                    .recipeId(recipeId)
                    .build();
                
                favoriteRecipeRepository.save(favoriteRecipe);
                addedCount++;
                
                log.debug("Recipe with ID {} added to favorites for user {}", recipeId, userId);
            } catch (Exception e) {
                log.error("Error adding recipe {} to favorites for user {}: {}", recipeId, userId, e.getMessage());
            }
        }
        
        log.info("Added {} recipes to favorites for user {}", addedCount, userId);
        return addedCount;
    }

    /**
     * Removes multiple recipes from user's favorites.
     *
     * @param userId    the user ID
     * @param recipeIds list of recipe IDs to remove from favorites
     * @return number of recipes successfully removed from favorites
     */
    @Transactional
    public int removeBatchFromFavorites(UUID userId, List<UUID> recipeIds) {
        log.debug("Removing batch of recipes from favorites for user with ID: {}", userId);
        
        int removedCount = 0;
        
        for (UUID recipeId : recipeIds) {
            try {
                favoriteRecipeRepository.deleteByUserIdAndRecipeId(userId, recipeId);
                removedCount++;
                log.debug("Recipe with ID {} removed from favorites for user {}", recipeId, userId);
            } catch (Exception e) {
                log.error("Error removing recipe {} from favorites for user {}: {}", recipeId, userId, e.getMessage());
            }
        }
        
        log.info("Removed {} recipes from favorites for user {}", removedCount, userId);
        return removedCount;
    }
    
    /**
     * Check favorite status for multiple recipes.
     *
     * @param userId    the user ID
     * @param recipeIds set of recipe IDs to check
     * @return map of recipe IDs to their favorite status
     */
    @Transactional(readOnly = true)
    public Map<UUID, Boolean> getBatchFavoriteStatus(UUID userId, Set<UUID> recipeIds) {
        log.debug("Checking batch favorite status for user with ID: {} and {} recipes", userId, recipeIds.size());
        return favoriteRecipeRepository.getUserFavoritesMap(userId, recipeIds);
    }
    
    /**
     * Get favorite counts for multiple recipes.
     *
     * @param recipeIds set of recipe IDs to get counts for
     * @return map of recipe IDs to their favorite counts
     */
    @Transactional(readOnly = true)
    public Map<UUID, Long> getBatchFavoriteCounts(Set<UUID> recipeIds) {
        log.debug("Getting batch favorite counts for {} recipes", recipeIds.size());
        return favoriteRecipeRepository.getFavoriteCountsMap(recipeIds);
    }
}