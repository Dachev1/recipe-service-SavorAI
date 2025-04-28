package dev.idachev.recipeservice.service;

import dev.idachev.recipeservice.exception.ResourceNotFoundException;
import dev.idachev.recipeservice.exception.UnauthorizedAccessException;
import dev.idachev.recipeservice.model.Comment;
import dev.idachev.recipeservice.model.Recipe;
import dev.idachev.recipeservice.repository.CommentRepository;
import dev.idachev.recipeservice.repository.RecipeRepository;
import dev.idachev.recipeservice.repository.dto.RecipeCommentCountDto;
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

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import jakarta.validation.ValidationException;

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
     * Updates a comment.
     * 
     * @param commentId The ID of the comment to update
     * @param content The new content for the comment
     * @param userId The ID of the user updating the comment
     * @return The updated comment
     */
    @Transactional
    public CommentResponse updateComment(UUID commentId, String content, UUID userId) {
        if (content == null || content.isBlank()) {
            throw new ValidationException("Comment content cannot be empty");
        }

        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new ResourceNotFoundException("Comment not found with ID: " + commentId));

        // Check if the user owns the comment
        if (!comment.getUserId().equals(userId)) {
            throw new UnauthorizedAccessException("You can only update your own comments");
        }

        // Fetch the recipe to determine if the current user is the recipe owner
        // Although users cannot comment on their own recipes (checked at creation),
        // this info might be relevant for the response DTO's `isRecipeOwner` flag.
        Recipe recipe = recipeRepository.findById(comment.getRecipeId())
            .orElseThrow(() -> new ResourceNotFoundException(
                "Recipe not found with id: " + comment.getRecipeId() + " associated with the comment."
            )); // Should not happen if data is consistent

        // Update using builder pattern to maintain immutability principles
        Comment updatedCommentData = comment.toBuilder()
                .content(content)
                .updatedAt(LocalDateTime.now()) // Use LocalDateTime consistent with entity
                .build();

        Comment savedComment = commentRepository.save(updatedCommentData);
        log.info("Updated comment with ID: {}", savedComment.getId());

        // Map to response, providing ownership flags
        boolean isOwner = true; // User updating is the owner
        boolean isRecipeOwner = recipe.getUserId().equals(userId); // Check if updater owns the recipe
        // Use the existing toResponse method which takes ownership flags
        return commentMapper.toResponse(savedComment, isOwner, isRecipeOwner); 
    }

    /**
     * Deletes a comment.
     * 
     * @param commentId The ID of the comment to delete
     * @param userId The ID of the user deleting the comment
     */
    @Transactional
    public void deleteComment(UUID commentId, UUID userId) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new ResourceNotFoundException("Comment not found with ID: " + commentId));
        
        // Check if the user owns the comment
        if (!comment.getUserId().equals(userId)) {
            throw new UnauthorizedAccessException("You can only delete your own comments");
        }
        
        commentRepository.delete(comment);
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
        List<RecipeCommentCountDto> counts = commentRepository.countByRecipeIdIn(recipeIds);
        return counts.stream()
                .collect(Collectors.toMap(RecipeCommentCountDto::getRecipeId, 
                                          RecipeCommentCountDto::getCommentCount));
    }

    /**
     * Retrieves comments created by a specific user.
     * Optimized to fetch recipes in bulk for all comments.
     */
    @Transactional(readOnly = true)
    public Page<CommentResponse> getCommentsByUserId(UUID userId, Pageable pageable) {
        log.debug("Fetching comments for userId: {}, pageable: {}", userId, pageable);
        Page<Comment> commentsPage = commentRepository.findByUserId(userId, pageable);
        
        if (commentsPage.isEmpty()) {
            return new PageImpl<>(Collections.emptyList(), pageable, 0);
        }
        
        // Extract all recipe IDs from comments to fetch them in bulk
        Set<UUID> recipeIds = commentsPage.getContent().stream()
                .map(Comment::getRecipeId)
                .collect(Collectors.toSet());
                
        // Fetch all required recipes in one query
        Map<UUID, Recipe> recipeMap = recipeRepository.findAllById(recipeIds).stream()
                .collect(Collectors.toMap(Recipe::getId, recipe -> recipe));
        
        List<CommentResponse> commentResponses = commentsPage.getContent().stream()
                .map(comment -> {
                    // Since we are fetching by userId, the caller IS the owner of these comments.
                    boolean isOwner = true; 
                    boolean isRecipeOwner = false; // Default, check below
                    
                    // Use the preloaded recipe from our map instead of separate queries
                    Recipe recipe = recipeMap.get(comment.getRecipeId());
                    if (recipe != null) {
                        isRecipeOwner = recipe.getUserId().equals(userId);
                    } else {
                        log.warn("Recipe with ID {} not found for comment {}", comment.getRecipeId(), comment.getId());
                    }

                    return commentMapper.toResponse(comment, isOwner, isRecipeOwner);
                })
                .toList();

        return new PageImpl<>(commentResponses, pageable, commentsPage.getTotalElements());
    }

    /**
     * Allows an administrator to delete any comment.
     */
    @Transactional
    public void deleteCommentAsAdmin(UUID commentId) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new ResourceNotFoundException("Comment not found with ID: " + commentId));
        
        commentRepository.delete(comment);
        log.info("Comment with ID: {} deleted by admin", commentId);
    }

    // Original TODOs (kept as comments above new placeholders)
    // TODO: Add method to get comments by userId if needed
    // TODO: Add method for admin to delete/update any comment if needed
    // TODO: Consider adding rate limiting for comment creation
} 