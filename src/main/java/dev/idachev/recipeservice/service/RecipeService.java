package dev.idachev.recipeservice.service;

import dev.idachev.recipeservice.exception.AIServiceException;
import dev.idachev.recipeservice.exception.ResourceNotFoundException;
import dev.idachev.recipeservice.exception.UnauthorizedAccessException;
import dev.idachev.recipeservice.infrastructure.ai.AIService;
import dev.idachev.recipeservice.model.FavoriteRecipe;
import dev.idachev.recipeservice.model.Recipe;
import dev.idachev.recipeservice.model.RecipeVote;
import dev.idachev.recipeservice.repository.FavoriteRecipeRepository;
import dev.idachev.recipeservice.repository.RecipeRepository;
import dev.idachev.recipeservice.user.service.UserService;
import dev.idachev.recipeservice.web.dto.RecipeRequest;
import dev.idachev.recipeservice.web.dto.RecipeResponse;
import dev.idachev.recipeservice.web.dto.SimplifiedRecipeResponse;
import dev.idachev.recipeservice.web.mapper.RecipeMapper;
import dev.idachev.recipeservice.service.RecipeResponseEnhancer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.PageImpl;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.Optional;

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
    private final AIService aiService;
    private final RecipeMapper recipeMapper;
    private final CommentService commentService;
    private final VoteService voteService;
    private final UserService userService;
    private final RecipeResponseEnhancer recipeResponseEnhancer;

    @Autowired
    public RecipeService(RecipeRepository recipeRepository,
                         FavoriteRecipeRepository favoriteRecipeRepository,
                         RecipeImageService recipeImageService,
                         AIService aiService,
                         RecipeMapper recipeMapper,
                         CommentService commentService,
                         VoteService voteService,
                         UserService userService,
                         RecipeResponseEnhancer recipeResponseEnhancer) {
        this.recipeRepository = recipeRepository;
        this.favoriteRecipeRepository = favoriteRecipeRepository;
        this.recipeImageService = recipeImageService;
        this.aiService = aiService;
        this.recipeMapper = recipeMapper;
        this.commentService = commentService;
        this.voteService = voteService;
        this.userService = userService;
        this.recipeResponseEnhancer = recipeResponseEnhancer;
    }

    /**
     * Create a new recipe with an optional image upload.
     * Uses Recipe builder via RecipeMapper and sets additional fields.
     */
    @Transactional
    public RecipeResponse createRecipe(RecipeRequest request, MultipartFile image, UUID userId) {
        String finalImageUrl = request.imageUrl(); // Start with request URL
        if (image != null && !image.isEmpty()) {
            String processedImageUrl = recipeImageService.processRecipeImage(
                    request.title(),
                    request.servingSuggestions(),
                    image);
            if (processedImageUrl != null && !processedImageUrl.isEmpty()) {
                finalImageUrl = processedImageUrl;
                log.info("Processed image for new recipe, obtained URL: {}", finalImageUrl);
            } else {
                log.warn("Image processing returned null or empty URL for new recipe");
                // Keep URL from request if processing failed
            }
        } 

        // Use mapper to get initial entity from request (uses builder internally)
        Recipe recipeFromRequest = recipeMapper.toEntity(request);

        // Use toBuilder() to create a mutable builder, set non-request fields, and build final entity
        Recipe recipeToSave = recipeFromRequest.toBuilder()
                .userId(userId)          // Set the user ID
                .imageUrl(finalImageUrl) // Set the final image URL
                // createdAt, updatedAt, upvotes, downvotes rely on @Builder.Default in Recipe entity
                .build();

        Recipe savedRecipe = recipeRepository.save(recipeToSave);
        log.info("Created recipe with ID: {}", savedRecipe.getId());

        return enhanceWithUserInteractions(recipeMapper.toResponse(savedRecipe), userId);
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

    /* Method removed - Logic delegated to RecipeSearchService.
     * Controllers should call RecipeSearchService.getAllRecipes directly.
    @Transactional(readOnly = true)
    public Page<RecipeResponse> getAllRecipes(Pageable pageable, UUID userId, boolean showPersonal) {
       // ... implementation removed ...
    }
    */

    /**
     * Get recipes by user ID.
     */
    @Transactional(readOnly = true)
    public List<RecipeResponse> getRecipesByUserId(UUID userId) {
        List<Recipe> recipes = recipeRepository.findByUserId(userId);
        List<RecipeResponse> baseResponses = recipes.stream()
                                                .map(recipeMapper::toResponse)
                                                .toList();
        // Use the injected enhancer
        return recipeResponseEnhancer.enhanceRecipeListWithUserInteractions(baseResponses, userId);
    }

    /**
     * Get recipe feed sorted by newest first with pagination.
     */
    @Transactional(readOnly = true)
    public Page<RecipeResponse> getRecipeFeed(UUID userId, Pageable pageable) {
        Page<Recipe> recipePage = recipeRepository.findAll(
            PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.Direction.DESC, "createdAt")
        );
        List<RecipeResponse> baseResponses = recipePage.getContent().stream()
                                                .map(recipeMapper::toResponse)
                                                .toList();
        // Use the injected enhancer
        List<RecipeResponse> enhancedResponses = recipeResponseEnhancer.enhanceRecipeListWithUserInteractions(baseResponses, userId);
        return new PageImpl<>(enhancedResponses, pageable, recipePage.getTotalElements());
    }

    /**
     * Update an existing recipe.
     */
    @Transactional
    public RecipeResponse updateRecipe(UUID id, RecipeRequest request, UUID userId) {
        // Delegate to the main update method with null image
        return updateRecipe(id, request, null, userId);
    }

    /**
     * Update an existing recipe with optional image upload.
     * Uses Recipe builder via RecipeMapper and toBuilder() for applying updates.
     */
    @Transactional
    public RecipeResponse updateRecipe(UUID id, RecipeRequest request, MultipartFile image, UUID userId) {
        // 1. Check permission and fetch the existing recipe
        Recipe existingRecipe = checkRecipePermission(id, userId);
        
        log.info("Updating recipe {} with image upload. Image present: {}", id, image != null && !image.isEmpty());

        // 2. Process image if provided
        String finalImageUrl = request.imageUrl(); // Start with URL from request DTO
        if (image != null && !image.isEmpty()) {
            String oldImageUrl = existingRecipe.getImageUrl(); // Get old URL from existing entity
            
            String processedImageUrl = recipeImageService.processRecipeImage(
                request.title(), 
                request.servingSuggestions(), 
                image);
            
            if (processedImageUrl != null && !processedImageUrl.isEmpty()) {
                finalImageUrl = processedImageUrl;
                 if (oldImageUrl != null && !oldImageUrl.equals(finalImageUrl)) {
                    log.info("Image URL changed from '{}' to '{}'", oldImageUrl, finalImageUrl);
                } else if (oldImageUrl == null) {
                    log.info("Image URL set to '{}'", finalImageUrl);
                }
            } else {
                 log.warn("Image processing returned null or empty URL for recipe {}", id);
                 // If processing failed, decide whether to keep the URL from request DTO 
                 // or revert to existingRecipe.getImageUrl(). Current logic keeps request DTO's URL.
            }
        } else {
            log.info("No new image provided for recipe {}, using URL from request: {}", id, finalImageUrl);
        }

        // 3. Use mapper to get base entity mapping from request
        Recipe recipeMappedFromRequest = recipeMapper.toEntity(request);
            
        // 4. Use toBuilder() on the mapped entity to apply non-request / existing fields
        Recipe updatedRecipe = recipeMappedFromRequest.toBuilder()
                 // Copy fields that should NOT change from the existing entity
                .id(existingRecipe.getId()) // MUST set ID for update
                .userId(existingRecipe.getUserId()) // Keep original owner
                .createdAt(existingRecipe.getCreatedAt()) // Keep original creation time
                .upvotes(existingRecipe.getUpvotes()) // Keep existing vote counts
                .downvotes(existingRecipe.getDownvotes())
                
                // Fields from request are already set by recipeMappedFromRequest base
                // Override specific fields managed here:
                .imageUrl(finalImageUrl) // Set the potentially updated image URL
                .updatedAt(LocalDateTime.now()) // Set new update timestamp
                // If isAiGenerated is optional in request, handle null (keep existing)
                .isAiGenerated(Optional.ofNullable(request.isAiGenerated()).orElse(existingRecipe.getIsAiGenerated()))
                .build();

        // 5. Save the updated entity
        Recipe savedRecipe = recipeRepository.save(updatedRecipe);
        log.info("Updated recipe with ID: {} saved successfully", savedRecipe.getId());

        // Enhance using the single-item method (still needed here)
        return enhanceWithUserInteractions(recipeMapper.toResponse(savedRecipe), userId);
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
     * Enhances a single recipe response with user-specific interaction data.
     * Note: This method performs individual lookups and can lead to N+1 issues if used on lists.
     * For lists, use RecipeResponseEnhancer.enhanceRecipeListWithUserInteractions.
     * It remains here for single-item enhancement contexts (getById, create, update).
     * Consider replacing with bulk enhancer if performance on single items becomes critical.
     */
    public RecipeResponse enhanceWithUserInteractions(RecipeResponse response, UUID userId) {
       if (response == null) {
            return null;
        }
        // This remains an N+1 approach, use enhanceRecipeListWithUserInteractions for lists.
        // Only log warn if it's called outside of a single-item context (hard to detect here)
        // log.warn("Single enhancement N+1 call for recipe ID: {}", response.id());

        UUID recipeId = response.id();
        UUID createdById = response.createdById();
        String authorName = "Unknown User";
        String authorUsername = "Unknown User";
        String authorIdStr = (createdById != null) ? createdById.toString() : null;
        boolean isFavorite = false;
        long favoriteCount = 0L;
        long commentCount = 0L;
        String userVoteStr = null;

        try { // Wrap individual fetches, log errors but continue enhancement
            favoriteCount = favoriteRecipeRepository.countByRecipeId(recipeId);
            if (userId != null) {
                 isFavorite = favoriteRecipeRepository.existsByUserIdAndRecipeId(userId, recipeId);
                 RecipeVote.VoteType userVoteType = voteService.getUserVote(recipeId, userId);
                 userVoteStr = (userVoteType != null) ? userVoteType.toString() : null;
            }
            commentCount = commentService.getCommentCount(recipeId);
            if (createdById != null) {
                 authorUsername = userService.getUsernameById(createdById);
                 authorName = (authorUsername != null) ? authorUsername : "Unknown User";
            } else {
                 authorUsername = "Unknown User"; // Ensure reset if createdById is null
                 authorName = "Unknown User";
            }
        } catch (Exception e) {
             log.error("Error enhancing recipe {} for user {}: {}", recipeId, userId, e.getMessage());
        }

        return new RecipeResponse(
            response.id(), response.createdById(), response.title(), response.servingSuggestions(),
            response.instructions(), response.imageUrl(), response.ingredients(), response.totalTimeMinutes(),
            authorName, authorUsername, authorIdStr, // Enhanced 
            response.difficulty(), response.isAiGenerated(),
            isFavorite, favoriteCount, commentCount, // Enhanced
            response.upvotes(), response.downvotes(), 
            userVoteStr, // Enhanced
            response.createdAt(), response.updatedAt(), response.macros(), response.additionalFields()
        );
    }
} 