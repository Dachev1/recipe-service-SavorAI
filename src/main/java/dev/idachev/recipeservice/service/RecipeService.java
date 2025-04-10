package dev.idachev.recipeservice.service;

import dev.idachev.recipeservice.exception.AIServiceException;
import dev.idachev.recipeservice.exception.ResourceNotFoundException;
import dev.idachev.recipeservice.exception.UnauthorizedAccessException;
import dev.idachev.recipeservice.infrastructure.ai.AIService;
import dev.idachev.recipeservice.mapper.RecipeMapper;
import dev.idachev.recipeservice.model.FavoriteRecipe;
import dev.idachev.recipeservice.model.Recipe;
import dev.idachev.recipeservice.model.RecipeVote;
import dev.idachev.recipeservice.repository.FavoriteRecipeRepository;
import dev.idachev.recipeservice.repository.RecipeRepository;
import dev.idachev.recipeservice.user.service.UserService;
import dev.idachev.recipeservice.web.dto.RecipeRequest;
import dev.idachev.recipeservice.web.dto.RecipeResponse;
import dev.idachev.recipeservice.web.dto.SimplifiedRecipeResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.Comparator;
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
    private final VoteService voteService;
    private final UserService userService;

    @Autowired
    public RecipeService(RecipeRepository recipeRepository,
                         FavoriteRecipeRepository favoriteRecipeRepository,
                         RecipeImageService recipeImageService,
                         RecipeSearchService recipeSearchService,
                         AIService aiService,
                         RecipeMapper recipeMapper,
                         CommentService commentService,
                         VoteService voteService,
                         UserService userService) {
        this.recipeRepository = recipeRepository;
        this.favoriteRecipeRepository = favoriteRecipeRepository;
        this.recipeImageService = recipeImageService;
        this.recipeSearchService = recipeSearchService;
        this.aiService = aiService;
        this.recipeMapper = recipeMapper;
        this.commentService = commentService;
        this.voteService = voteService;
        this.userService = userService;
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

        return enhanceWithUserInteractions(recipeMapper.toResponse(savedRecipe), userId);
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
        return enhanceWithUserInteractions(recipeMapper.toResponse(recipe), userId);
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
                .map(response -> enhanceWithUserInteractions(response, userId))
                .toList();
    }

    /**
     * Get recipe feed sorted by newest first with pagination.
     */
    @Transactional(readOnly = true)
    public Page<RecipeResponse> getRecipeFeed(UUID userId, Pageable pageable) {
        // Get paginated recipes sorted by creation date (newest first)
        Page<Recipe> recipePage = recipeRepository.findAll(
            PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                Sort.Direction.DESC,
                "createdAt"
            )
        );

        // Map to response DTOs with user interactions
        return recipePage.map(recipe -> enhanceWithUserInteractions(recipeMapper.toResponse(recipe), userId));
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

        return enhanceWithUserInteractions(recipeMapper.toResponse(updatedRecipe), userId);
    }

    /**
     * Update an existing recipe with optional image upload.
     */
    @Transactional
    public RecipeResponse updateRecipe(UUID id, RecipeRequest request, MultipartFile image, UUID userId) {
        Recipe recipe = checkRecipePermission(id, userId);
        
        log.info("Updating recipe {} with image upload. Image present: {}", id, image != null && !image.isEmpty());

        // Process image if provided and set the new URL
        if (image != null && !image.isEmpty()) {
            log.info("Processing new image for recipe {}", id);
            String oldImageUrl = recipe.getImageUrl();
            
            // Process the new image
            processImageIfPresent(request, image);
            
            // Log the image URL change 
            if (oldImageUrl != null && !oldImageUrl.equals(request.getImageUrl())) {
                log.info("Image URL changed from '{}' to '{}'", oldImageUrl, request.getImageUrl());
            }
        } else {
            log.info("No new image provided for recipe {}", id);
        }

        // Update entity fields from the request
        recipeMapper.updateEntityFromRequest(recipe, request);
        recipe.setUpdatedAt(LocalDateTime.now());

        Recipe updatedRecipe = recipeRepository.save(recipe);
        log.info("Updated recipe with ID: {} and image", updatedRecipe.getId());

        return enhanceWithUserInteractions(recipeMapper.toResponse(updatedRecipe), userId);
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
     * Enhance a recipe response with user interaction information.
     * Public to allow other services to access the complete enhancement logic.
     */
    public RecipeResponse enhanceWithUserInteractions(RecipeResponse response, UUID userId) {
        if (response == null) {
            log.debug("enhanceWithUserInteractions: response is null");
            return null;
        }

        try {
            // Check if this recipe is in the user's favorites
            boolean isFavorite = favoriteRecipeRepository.existsByUserIdAndRecipeId(userId, response.getId());
            response.setIsFavorite(isFavorite);
    
            // Get the favorite count for this recipe
            long favoriteCount = favoriteRecipeRepository.countByRecipeId(response.getId());
            response.setFavoriteCount(favoriteCount);
            
            // Get the comment count for this recipe
            long commentCount = commentService.getCommentCount(response.getId());
            response.setCommentCount(commentCount);
            
            // Get vote information
            response.setUpvotes(response.getUpvotes() != null ? response.getUpvotes() : 0);
            response.setDownvotes(response.getDownvotes() != null ? response.getDownvotes() : 0);
            
            // Get user's vote on this recipe
            RecipeVote.VoteType userVote = voteService.getUserVote(response.getId(), userId);
            if (userVote != null) {
                response.setUserVote(userVote.toString());
            }
    
            // Add author information
            log.info("Enhancing recipe {} with author information. CreatedById: {}", 
                    response.getId(), response.getCreatedById());
            
            if (response.getCreatedById() != null) {
                try {
                    // Get author information from user service
                    String authorName = userService.getUsernameById(response.getCreatedById());
                    log.info("Setting author name '{}' for recipe {}", authorName, response.getId());
                    
                    // If we get back Unknown User, use a better default
                    if (authorName == null || "Unknown User".equals(authorName)) {
                        log.warn("User service returned 'Unknown User' for ID {}, using better fallback", response.getCreatedById());
                        authorName = "Chef"; 
                    }
                    
                    // EXPLICITLY set both authorName and username fields with the same value
                    response.setAuthorName(authorName);
                    response.setUsername(authorName);
                    
                    // Verify fields are set correctly in the log
                    log.info("AUTHOR DEBUG - Recipe {} author fields after setting: authorName='{}', username='{}'", 
                        response.getId(), response.getAuthorName(), response.getUsername());
                } catch (Exception e) {
                    log.error("Failed to get author name for recipe {}: {}", response.getId(), e.getMessage(), e);
                    // Set a default value if author name lookup fails
                    response.setAuthorName("Chef");
                    response.setUsername("Chef");
                    
                    log.info("AUTHOR DEBUG - Recipe {} using fallback author: authorName='{}', username='{}'", 
                        response.getId(), response.getAuthorName(), response.getUsername());
                }
            } else {
                log.warn("Recipe {} has no author ID", response.getId());
                response.setAuthorName("Unknown Chef");
                response.setUsername("Unknown Chef");
                
                log.info("AUTHOR DEBUG - Recipe {} has no author ID, using: authorName='{}', username='{}'", 
                    response.getId(), response.getAuthorName(), response.getUsername());
            }
        } catch (Exception e) {
            log.error("Error enhancing recipe with user interactions: {}", e.getMessage(), e);
        }

        return response;
    }
    
    /**
     * Convert Recipe entity to RecipeResponse DTO for use with the VoteService.
     */
    public RecipeResponse toResponse(Recipe recipe, UUID userId) {
        if (recipe == null) {
            return null;
        }
        return enhanceWithUserInteractions(recipeMapper.toResponse(recipe), userId);
    }
} 