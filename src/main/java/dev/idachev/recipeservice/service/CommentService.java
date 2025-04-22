package dev.idachev.recipeservice.service;

import dev.idachev.recipeservice.exception.ResourceNotFoundException;
import dev.idachev.recipeservice.exception.UnauthorizedAccessException;
import dev.idachev.recipeservice.model.Comment;
import dev.idachev.recipeservice.model.Recipe;
import dev.idachev.recipeservice.repository.CommentRepository;
import dev.idachev.recipeservice.repository.RecipeRepository;
import dev.idachev.recipeservice.user.service.UserService;
import dev.idachev.recipeservice.web.dto.CommentRequest;
import dev.idachev.recipeservice.web.dto.CommentResponse;
import dev.idachev.recipeservice.user.dto.UserResponse;
import dev.idachev.recipeservice.web.mapper.CommentMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service for comment operations.
 */
@Service
@Slf4j
public class CommentService {

    private final CommentRepository commentRepository;
    private final RecipeRepository recipeRepository;
    private final CommentMapper commentMapper;
    private final UserService userService;

    @Autowired
    public CommentService(CommentRepository commentRepository,
                          RecipeRepository recipeRepository,
                          CommentMapper commentMapper,
                          UserService userService) {
        this.commentRepository = commentRepository;
        this.recipeRepository = recipeRepository;
        this.commentMapper = commentMapper;
        this.userService = userService;
    }

    /**
     * Create a new comment with user info from token.
     * This method is used when the user token is available.
     */
    @Transactional
    public CommentResponse createComment(UUID recipeId, CommentRequest request, String token) {
        // Get UserResponse directly using the token
        UserResponse user = userService.getUserResponseFromToken(token, "Create Comment"); 
        return createCommentInternal(recipeId, request, user.getId(), user.getUsername());
    }

    /**
     * Create a new comment with specified user ID and token.
     * This validates both the user ID and token for maximum security.
     */
    @Transactional
    public CommentResponse createComment(UUID recipeId, CommentRequest request, UUID userId, String token) {
        // Get user information using the token
        UserResponse userFromToken = userService.getUserResponseFromToken(token, "Create Comment (ID Validation)");

        // Verify the provided userId matches the ID from the token
        if (!userId.equals(userFromToken.getId())) {
            log.warn("User ID mismatch: Provided={}, Token Owner={}", userId, userFromToken.getId());
            throw new UnauthorizedAccessException("User ID mismatch - cannot create comment as another user");
        }

        // Use ID and username from the validated token user
        return createCommentInternal(recipeId, request, userFromToken.getId(), userFromToken.getUsername());
    }

    /**
     * Create a new comment with specified user ID.
     * This method is for system use or when user authentication is handled elsewhere.
     * Use with caution as it bypasses token-based authentication.
     */
    @Transactional
    public CommentResponse createSystemComment(UUID recipeId, CommentRequest request, UUID userId, String username) {
        if (userId == null || username == null || username.isEmpty()) {
            throw new IllegalArgumentException("User ID and username are required");
        }

        return createCommentInternal(recipeId, request, userId, username);
    }

    /**
     * Create a new comment using only the authenticated user's ID.
     * Fetches the username internally.
     */
    @Transactional
    public CommentResponse createComment(UUID recipeId, CommentRequest request, UUID userId) {
        // Fetch username using the provided userId
        String username = userService.getUsernameById(userId);
        // Delegate to the internal method
        return createCommentInternal(recipeId, request, userId, username);
    }

    /**
     * Internal method to create a comment with the given user information.
     * This separates the user retrieval logic from comment creation logic.
     */
    private CommentResponse createCommentInternal(UUID recipeId, CommentRequest request, UUID userId, String username) {
        // Check if recipe exists
        Recipe recipe = recipeRepository.findById(recipeId)
                .orElseThrow(() -> new ResourceNotFoundException("Recipe not found with id: " + recipeId));

        // Check if user is trying to comment on their own recipe
        if (recipe.getUserId().equals(userId)) {
            throw new UnauthorizedAccessException("You cannot comment on your own recipe");
        }

        Comment comment = commentMapper.toEntity(request, userId, username, recipeId);
        Comment savedComment = commentRepository.save(comment);
        log.info("Created comment with ID: {} for recipe ID: {}", savedComment.getId(), recipeId);

        // Determine ownership flags here
        boolean isOwner = true; // The creator is always the owner initially
        // Recipe owner check was done above to prevent self-commenting, reuse logic
        boolean isRecipeOwner = recipe.getUserId().equals(userId); 
        // Pass flags to mapper
        return commentMapper.toResponse(savedComment, isOwner, isRecipeOwner);
    }

    /**
     * Get comments for a recipe.
     */
    @Transactional(readOnly = true)
    public Page<CommentResponse> getCommentsForRecipe(UUID recipeId, Pageable pageable, UUID userId) {
        // Check if recipe exists first, and get the owner ID
        Recipe recipe = recipeRepository.findById(recipeId)
                .orElseThrow(() -> new ResourceNotFoundException("Recipe not found with id: " + recipeId));
        boolean currentUserIsRecipeOwner = userId != null && recipe.getUserId().equals(userId);

        Page<Comment> comments = commentRepository.findByRecipeId(recipeId, pageable);
        List<CommentResponse> commentResponses = comments.getContent().stream()
                .map(comment -> {
                    // Determine ownership flags here
                    boolean isOwner = userId != null && comment.getUserId().equals(userId);
                    // isRecipeOwner is the same for all comments on this recipe
                    return commentMapper.toResponse(comment, isOwner, currentUserIsRecipeOwner);
                })
                .toList();

        return new PageImpl<>(commentResponses, pageable, comments.getTotalElements());
    }

    /**
     * Update a comment using the builder pattern.
     */
    @Transactional
    public CommentResponse updateComment(UUID commentId, CommentRequest request, UUID userId) {
        // 1. Fetch the existing comment
        Comment existingComment = commentRepository.findById(commentId)
                .orElseThrow(() -> new ResourceNotFoundException("Comment not found with id: " + commentId));

        // 2. Check if the user is the owner of the comment
        if (!existingComment.getUserId().equals(userId)) {
            log.warn("User {} attempted to update comment {} owned by {}", userId, commentId, existingComment.getUserId());
            throw new UnauthorizedAccessException("You do not have permission to update this comment");
        }

        // 3. Use toBuilder() and record accessor
        Comment updatedComment = existingComment.toBuilder()
                .content(request.content()) // Use record accessor
                .build(); // Let @PreUpdate handle updatedAt
        
        // 4. Save the updated comment
        Comment savedComment = commentRepository.save(updatedComment);
        log.info("Updated comment with ID: {}", savedComment.getId());

        // 5. Determine ownership flags and map to response DTO
        boolean isOwner = true; // Updater is the owner
        // Need recipe owner status for the response DTO
        Recipe recipe = recipeRepository.findById(savedComment.getRecipeId())
             .orElseThrow(() -> new ResourceNotFoundException("Recipe not found for comment: " + savedComment.getRecipeId())); // Should not happen if comment exists
        boolean isRecipeOwner = recipe.getUserId().equals(userId);

        return commentMapper.toResponse(savedComment, isOwner, isRecipeOwner); 
    }

    /**
     * Delete a comment.
     */
    @Transactional
    public void deleteComment(UUID commentId, UUID userId) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new ResourceNotFoundException("Comment not found with id: " + commentId));

        // Get the recipe to check if the user is the recipe owner
        Recipe recipe = recipeRepository.findById(comment.getRecipeId())
                .orElseThrow(() -> new ResourceNotFoundException("Recipe not found with id: " + comment.getRecipeId()));

        // Check if the user is either the owner of the comment or the owner of the recipe
        boolean isCommentOwner = comment.getUserId().equals(userId);
        boolean isRecipeOwner = recipe.getUserId().equals(userId);

        if (!isCommentOwner && !isRecipeOwner) {
            throw new UnauthorizedAccessException("You do not have permission to delete this comment");
        }

        commentRepository.delete(comment);
        log.info("Comment with ID {} deleted successfully by user {}. Comment owner: {}, Recipe owner: {}", 
                commentId, userId, comment.getUserId(), recipe.getUserId());
    }

    /**
     * Get comment count for a recipe.
     */
    @Transactional(readOnly = true)
    public long getCommentCount(UUID recipeId) {
        return commentRepository.countByRecipeId(recipeId);
    }

    /**
     * Fetches comment counts for multiple recipes efficiently.
     * @param recipeIds Set of recipe IDs.
     * @return Map of Recipe ID to comment count.
     */
    @Transactional(readOnly = true)
    public Map<UUID, Long> getCommentCountsForRecipes(Set<UUID> recipeIds) {
        if (recipeIds == null || recipeIds.isEmpty()) {
            return Collections.emptyMap();
        }
        log.debug("Fetching comment counts for {} recipe IDs", recipeIds.size());
        List<CommentRepository.RecipeCommentCount> counts = commentRepository.countByRecipeIdIn(recipeIds);
        return counts.stream()
                .collect(Collectors.toMap(CommentRepository.RecipeCommentCount::getRecipeId, 
                                          CommentRepository.RecipeCommentCount::getCommentCount));
    }

    /**
     * Retrieves comments created by a specific user.
     * TODO: Implement this method. Requires:
     *       1. Defining `findByUserId(UUID userId, Pageable pageable)` in CommentRepository.
     *       2. Mapping the results to CommentResponse, considering ownership flags.
     */
    public Page<CommentResponse> getCommentsByUserId(UUID userId, Pageable pageable) {
        log.error("UNIMPLEMENTED: getCommentsByUserId called but feature is not implemented.");
        // Example: Page<Comment> comments = commentRepository.findByUserId(userId, pageable);
        // List<CommentResponse> responses = comments.stream().map(c -> commentMapper.toResponse(c, userId)).toList(); // Pass userId for ownership check
        // return new PageImpl<>(responses, pageable, comments.getTotalElements());
        // Returning empty page might hide the fact that it's unimplemented.
        // Throwing exception makes it clearer during development/testing.
        throw new UnsupportedOperationException("Get comments by user ID not implemented yet."); 
    }

    /**
     * Allows an administrator to delete any comment.
     * TODO: Implement this method. Requires:
     *       1. Adding role-based authorization check (e.g., @PreAuthorize("hasRole('ADMIN')")).
     *       2. Fetching the comment or throwing ResourceNotFoundException.
     *       3. Calling commentRepository.delete(comment).
     */
    @Transactional
    public void deleteCommentAsAdmin(UUID commentId) {
        log.error("UNIMPLEMENTED: deleteCommentAsAdmin called but feature is not implemented.");
        // 1. Check if SecurityContextHolder.getContext().getAuthentication() has ADMIN role.
        // 2. Comment comment = commentRepository.findById(commentId).orElseThrow(...);
        // 3. commentRepository.delete(comment);
        throw new UnsupportedOperationException("Admin comment deletion not implemented yet.");
    }

    /**
     * Placeholder for applying rate limiting logic to comment creation.
     * TODO: Implement rate limiting if needed. Consider using libraries like
     *       Resilience4j (Bucket4j) or database-level tracking.
     *       This method would likely be called within createCommentInternal.
     */
    private void applyRateLimitingCheck(UUID userId) {
        // Placeholder for rate limiting logic
        // log.debug("Rate limiting check for user {}", userId); 
        // Example using a hypothetical RateLimiterService:
        // if (!rateLimiterService.allowCommentCreation(userId)) {
        //     throw new TooManyRequestsException("Comment creation limit exceeded.");
        // }
        // log.warn("Rate limiting check not implemented for comment creation.");
    }

    // Original TODOs (kept as comments above new placeholders)
    // TODO: Add method to get comments by userId if needed
    // TODO: Add method for admin to delete/update any comment if needed
    // TODO: Consider adding rate limiting for comment creation
} 