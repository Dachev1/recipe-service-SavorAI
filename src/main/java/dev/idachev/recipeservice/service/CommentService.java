package dev.idachev.recipeservice.service;

import dev.idachev.recipeservice.exception.ResourceNotFoundException;
import dev.idachev.recipeservice.exception.UnauthorizedAccessException;
import dev.idachev.recipeservice.mapper.CommentMapper;
import dev.idachev.recipeservice.model.Comment;
import dev.idachev.recipeservice.model.Recipe;
import dev.idachev.recipeservice.repository.CommentRepository;
import dev.idachev.recipeservice.repository.RecipeRepository;
import dev.idachev.recipeservice.user.dto.UserDTO;
import dev.idachev.recipeservice.user.service.UserService;
import dev.idachev.recipeservice.web.dto.CommentRequest;
import dev.idachev.recipeservice.web.dto.CommentResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

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
        UserDTO user = userService.getCurrentUser(token);
        UUID userId = userService.getUserIdFromUsername(user.getUsername());
        return createCommentInternal(recipeId, request, userId, user.getUsername());
    }

    /**
     * Create a new comment with specified user ID and token.
     * This validates both the user ID and token for maximum security.
     */
    @Transactional
    public CommentResponse createComment(UUID recipeId, CommentRequest request, UUID userId, String token) {
        // Get user information using the token
        UserDTO user = userService.getCurrentUser(token);

        // Since UserDTO doesn't have an ID, generate one from username
        UUID generatedUserId = userService.getUserIdFromUsername(user.getUsername());

        // Verify the provided userId matches the generated ID
        if (!userId.equals(generatedUserId)) {
            throw new UnauthorizedAccessException("User ID mismatch - cannot create comment as another user");
        }

        return createCommentInternal(recipeId, request, userId, user.getUsername());
    }

    /**
     * Create a new comment with specified user ID.
     * This method is used when the user ID is already known.
     */
    @Transactional
    public CommentResponse createComment(UUID recipeId, CommentRequest request, UUID userId) {
        UserDTO user = userService.getUserById(userId);
        return createCommentInternal(recipeId, request, userId, user.getUsername());
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
     * Internal method to create a comment with the given user information.
     * This separates the user retrieval logic from comment creation logic.
     */
    private CommentResponse createCommentInternal(UUID recipeId, CommentRequest request, UUID userId, String username) {
        // Check if recipe exists
        Recipe recipe = recipeRepository.findById(recipeId)
                .orElseThrow(() -> new ResourceNotFoundException("Recipe not found with id: " + recipeId));

        Comment comment = commentMapper.toEntity(request, userId, username, recipeId);
        Comment savedComment = commentRepository.save(comment);
        log.info("Created comment with ID: {} for recipe ID: {}", savedComment.getId(), recipeId);

        return commentMapper.toResponse(savedComment, userId);
    }

    /**
     * Get comments for a recipe.
     */
    @Transactional(readOnly = true)
    public Page<CommentResponse> getCommentsForRecipe(UUID recipeId, Pageable pageable, UUID userId) {
        // Check if recipe exists
        if (!recipeRepository.existsById(recipeId)) {
            throw new ResourceNotFoundException("Recipe not found with id: " + recipeId);
        }

        Page<Comment> comments = commentRepository.findByRecipeId(recipeId, pageable);
        List<CommentResponse> commentResponses = comments.getContent().stream()
                .map(comment -> commentMapper.toResponse(comment, userId))
                .toList();

        return new PageImpl<>(commentResponses, pageable, comments.getTotalElements());
    }

    /**
     * Update a comment.
     */
    @Transactional
    public CommentResponse updateComment(UUID commentId, CommentRequest request, UUID userId) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new ResourceNotFoundException("Comment not found with id: " + commentId));

        // Check if the user is the owner of the comment
        if (!comment.getUserId().equals(userId)) {
            throw new UnauthorizedAccessException("You do not have permission to update this comment");
        }

        commentMapper.updateEntityFromRequest(comment, request);
        Comment updatedComment = commentRepository.save(comment);
        log.info("Updated comment with ID: {}", updatedComment.getId());

        return commentMapper.toResponse(updatedComment, userId);
    }

    /**
     * Delete a comment.
     */
    @Transactional
    public void deleteComment(UUID commentId, UUID userId) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new ResourceNotFoundException("Comment not found with id: " + commentId));

        // Check if the user is the owner of the comment
        if (!comment.getUserId().equals(userId)) {
            throw new UnauthorizedAccessException("You do not have permission to delete this comment");
        }

        commentRepository.delete(comment);
        log.info("Comment with ID {} deleted successfully", commentId);
    }

    /**
     * Get comment count for a recipe.
     */
    @Transactional(readOnly = true)
    public long getCommentCount(UUID recipeId) {
        return commentRepository.countByRecipeId(recipeId);
    }
} 