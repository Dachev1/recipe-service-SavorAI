package dev.idachev.recipeservice.service;

import dev.idachev.recipeservice.exception.AIServiceException;
import dev.idachev.recipeservice.exception.ResourceNotFoundException;
import dev.idachev.recipeservice.exception.UnauthorizedAccessException;
import dev.idachev.recipeservice.infrastructure.ai.AIService;
import dev.idachev.recipeservice.mapper.RecipeMapper;
import dev.idachev.recipeservice.model.FavoriteRecipe;
import dev.idachev.recipeservice.model.Recipe;
import dev.idachev.recipeservice.repository.FavoriteRecipeRepository;
import dev.idachev.recipeservice.repository.RecipeRepository;
import dev.idachev.recipeservice.web.dto.RecipeRequest;
import dev.idachev.recipeservice.web.dto.RecipeResponse;
import dev.idachev.recipeservice.web.dto.SimplifiedRecipeResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Service for recipe management operations.
 * Follows Single Responsibility Principle by focusing only on recipe business logic.
 */
@Service
@Slf4j
public class RecipeService {

    private final RecipeRepository recipeRepository;
    private final FavoriteRecipeRepository favoriteRecipeRepository;
    private final RecipeImageService recipeImageService;
    private final RecipeSearchService recipeSearchService;
    private final AIService aiService;
    private final RecipeMapper recipeMapper;
    private final CommentService commentService;

    @Autowired
    public RecipeService(RecipeRepository recipeRepository,
                         FavoriteRecipeRepository favoriteRecipeRepository,
                         RecipeImageService recipeImageService,
                         RecipeSearchService recipeSearchService,
                         AIService aiService,
                         RecipeMapper recipeMapper,
                         CommentService commentService) {
        this.recipeRepository = recipeRepository;
        this.favoriteRecipeRepository = favoriteRecipeRepository;
        this.recipeImageService = recipeImageService;
        this.recipeSearchService = recipeSearchService;
        this.aiService = aiService;
        this.recipeMapper = recipeMapper;
        this.commentService = commentService;
    }

    /**
     * Create a new recipe with an optional image upload.
     */
    @Transactional
    public RecipeResponse createRecipe(RecipeRequest request, MultipartFile image, UUID userId) {
        processImageIfPresent(request, image);

        Recipe recipe = recipeMapper.toEntity(request);
        recipe.setUserId(userId);
        recipe.setCreatedAt(LocalDateTime.now());
        recipe.setUpdatedAt(LocalDateTime.now());

        Recipe savedRecipe = recipeRepository.save(recipe);
        log.info("Created recipe with ID: {}", savedRecipe.getId());

        return enhanceWithFavoriteInfo(recipeMapper.toResponse(savedRecipe), userId);
    }

    /**
     * Process and attach image URL to the recipe request if an image is provided
     */
    private void processImageIfPresent(RecipeRequest request, MultipartFile image) {
        if (image != null && !image.isEmpty()) {
            String imageUrl = recipeImageService.processRecipeImage(
                    request.getTitle(), request.getServingSuggestions(), image);

            if (imageUrl != null && !imageUrl.isEmpty()) {
                request.setImageUrl(imageUrl);
            } else {
                log.warn("Image processing returned null or empty URL");
            }
        }
    }

    /**
     * Create a recipe without image upload.
     */
    @Transactional
    public RecipeResponse createRecipe(RecipeRequest request, UUID userId) {
        return createRecipe(request, null, userId);
    }

    /**
     * Get a recipe by ID.
     */
    @Transactional(readOnly = true)
    public RecipeResponse getRecipeById(UUID id, UUID userId) {
        Recipe recipe = findRecipeByIdOrThrow(id);
        return enhanceWithFavoriteInfo(recipeMapper.toResponse(recipe), userId);
    }

    /**
     * Find a recipe by ID or throw an exception if not found
     */
    private Recipe findRecipeByIdOrThrow(UUID id) {
        return recipeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Recipe not found with id: " + id));
    }

    /**
     * Get all recipes with pagination and favorite information.
     */
    @Transactional(readOnly = true)
    public Page<RecipeResponse> getAllRecipes(Pageable pageable, UUID userId, boolean showPersonal) {
        if (showPersonal) {
            // If showPersonal is true, return all recipes
            return recipeSearchService.getAllRecipes(pageable, userId);
        } else {
            // If showPersonal is false, exclude recipes created by the current user
            return recipeSearchService.getAllRecipesExcludingUser(pageable, userId);
        }
    }

    /**
     * Get recipes by user ID.
     */
    @Transactional(readOnly = true)
    public List<RecipeResponse> getRecipesByUserId(UUID userId) {
        return recipeRepository.findByUserId(userId).stream()
                .map(recipeMapper::toResponse)
                .map(response -> enhanceWithFavoriteInfo(response, userId))
                .toList();
    }

    /**
     * Update an existing recipe.
     */
    @Transactional
    public RecipeResponse updateRecipe(UUID id, RecipeRequest request, UUID userId) {
        Recipe recipe = checkRecipePermission(id, userId);

        recipeMapper.updateEntityFromRequest(recipe, request);
        recipe.setUpdatedAt(LocalDateTime.now());

        Recipe updatedRecipe = recipeRepository.save(recipe);
        log.info("Updated recipe with ID: {}", updatedRecipe.getId());

        return enhanceWithFavoriteInfo(recipeMapper.toResponse(updatedRecipe), userId);
    }

    /**
     * Delete a recipe.
     */
    @Transactional
    public void deleteRecipe(UUID id, UUID userId) {
        Recipe recipe = checkRecipePermission(id, userId);
        
        // First delete all favorites related to this recipe
        List<UUID> userIds = favoriteRecipeRepository.findByRecipeId(id).stream()
                .map(FavoriteRecipe::getUserId)
                .toList();
        
        // Log how many favorite entries will be deleted
        if (!userIds.isEmpty()) {
            log.info("Deleting {} favorite entries for recipe {}", userIds.size(), id);
            favoriteRecipeRepository.deleteByRecipeId(id);
        }
        
        // Then delete the recipe
        recipeRepository.delete(recipe);
        log.info("Recipe with ID {} deleted successfully", id);
    }

    /**
     * Search recipes by keyword.
     */
    @Transactional(readOnly = true)
    public Page<RecipeResponse> searchRecipes(String keyword, Pageable pageable, UUID userId) {
        return recipeSearchService.searchRecipes(keyword, pageable, userId);
    }

    /**
     * Generate a meal from ingredients
     */
    public SimplifiedRecipeResponse generateMeal(List<String> ingredients) {
        log.info("Generating meal from {} ingredients", ingredients != null ? ingredients.size() : 0);
        try {
            return aiService.generateRecipeFromIngredients(ingredients);
        } catch (AIServiceException e) {
            log.error("AI Service error: {}", e.getMessage());
            throw e; // Propagate the original error
        }
    }

    /**
     * Check if a user has permission to modify a recipe.
     */
    private Recipe checkRecipePermission(UUID recipeId, UUID userId) {
        Recipe recipe = findRecipeByIdOrThrow(recipeId);

        if (!recipe.getUserId().equals(userId)) {
            log.warn("Unauthorized access attempt: User {} attempted to access recipe {}", userId, recipeId);
            throw new UnauthorizedAccessException("You do not have permission to modify this recipe");
        }

        return recipe;
    }

    /**
     * Enhance a recipe response with favorite information.
     */
    private RecipeResponse enhanceWithFavoriteInfo(RecipeResponse response, UUID userId) {
        if (response == null) {
            return null;
        }

        // Check if this recipe is in the user's favorites
        boolean isFavorite = favoriteRecipeRepository.existsByUserIdAndRecipeId(userId, response.getId());
        response.setIsFavorite(isFavorite);

        // Get the favorite count for this recipe
        long favoriteCount = favoriteRecipeRepository.countByRecipeId(response.getId());
        response.setFavoriteCount(favoriteCount);
        
        // Get the comment count for this recipe
        long commentCount = commentService.getCommentCount(response.getId());
        response.setCommentCount(commentCount);

        return response;
    }
} 