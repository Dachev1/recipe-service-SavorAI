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

    @Autowired
    public RecipeService(RecipeRepository recipeRepository,
                         FavoriteRecipeRepository favoriteRecipeRepository,
                         RecipeImageService recipeImageService,
                         AIService aiService,
                         RecipeMapper recipeMapper,
                         CommentService commentService,
                         VoteService voteService,
                         UserService userService) {
        this.recipeRepository = recipeRepository;
        this.favoriteRecipeRepository = favoriteRecipeRepository;
        this.recipeImageService = recipeImageService;
        this.aiService = aiService;
        this.recipeMapper = recipeMapper;
        this.commentService = commentService;
        this.voteService = voteService;
        this.userService = userService;
    }

    /**
     * Create a new recipe with an optional image upload.
     * Uses Recipe builder via RecipeMapper and sets additional fields.
     */
    @Transactional
    public RecipeResponse createRecipe(RecipeRequest request, MultipartFile image, UUID userId) {
        if (userId == null) {
            log.error("Attempted to create recipe with null userId - this should never happen!");
            throw new IllegalArgumentException("User ID cannot be null when creating a recipe");
        }
        
        log.info("Creating recipe '{}' for user ID: {}", request.title(), userId);
        
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
                
        log.debug("Recipe before saving - Title: {}, UserID: {}", recipeToSave.getTitle(), recipeToSave.getUserId());

        Recipe savedRecipe = recipeRepository.save(recipeToSave);
        log.info("Created recipe with ID: {}, UserID: {}", savedRecipe.getId(), savedRecipe.getUserId());

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
        // Enhancement logic removed
        return baseResponses;
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
        
        // Enhance responses with user information before returning
        List<RecipeResponse> enhancedResponses = baseResponses.stream()
                                               .map(response -> enhanceWithUserInteractions(response, userId))
                                               .toList();
        
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
                .version(existingRecipe.getVersion()) // Preserve version for optimistic locking
                
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
     * Throws UnauthorizedAccessException if user doesn't own the recipe.
     * 
     * @param recipeId ID of the recipe to check
     * @param userId ID of the user requesting access
     * @return Recipe if user has permission
     * @throws ResourceNotFoundException if recipe not found
     * @throws UnauthorizedAccessException if user is not the owner
     */
    public Recipe checkRecipePermission(UUID recipeId, UUID userId) {
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

        UUID recipeId = response.id();
        UUID createdById = response.createdById();
        
        // Initialize defaults
        String authorName = "Unknown User";
        String authorUsername = "Unknown User";
        String authorIdStr = (createdById != null) ? createdById.toString() : null;
        boolean isFavorite = false;
        long favoriteCount = 0L;
        long commentCount = 0L;
        String userVoteStr = null;

        try {
            // 1. Get counts and votes from database
            favoriteCount = favoriteRecipeRepository.countByRecipeId(recipeId);
            commentCount = commentService.getCommentCount(recipeId);
            
            // 2. Get user-specific data if userId provided
            if (userId != null) {
                isFavorite = favoriteRecipeRepository.existsByUserIdAndRecipeId(userId, recipeId);
                RecipeVote.VoteType userVoteType = voteService.getUserVote(recipeId, userId);
                userVoteStr = (userVoteType != null) ? userVoteType.toString() : null;
            }
            
            // 3. Get author data if available
            if (createdById != null) {
                authorUsername = userService.getUsernameById(createdById);
                authorName = authorUsername; // Use username as name
            }
        } catch (Exception e) {
            log.error("Error enhancing recipe {} for user {}: {}", recipeId, userId, e.getMessage());
            // Continue with default values rather than failing the entire request
        }

        // Build the enhanced response with all data
        return new RecipeResponse(
            response.id(), response.createdById(), response.title(), response.servingSuggestions(),
            response.instructions(), response.imageUrl(), response.ingredients(), response.totalTimeMinutes(),
            authorName, authorUsername, authorIdStr, 
            response.difficulty(), response.isAiGenerated(),
            isFavorite, favoriteCount, commentCount,
            response.upvotes(), response.downvotes(), 
            userVoteStr,
            response.createdAt(), response.updatedAt(), response.macros(), response.additionalFields()
        );
    }
} 