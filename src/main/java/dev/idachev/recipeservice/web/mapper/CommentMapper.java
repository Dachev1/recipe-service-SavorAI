package dev.idachev.recipeservice.web.mapper;

import dev.idachev.recipeservice.model.Comment;
import dev.idachev.recipeservice.web.dto.CommentRequest;
import dev.idachev.recipeservice.web.dto.CommentResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Mapper for comment transformations.
 */
@Component
@Slf4j
public class CommentMapper {

    // No-arg constructor or rely on default if no other dependencies
    public CommentMapper() {
        // Initialization if needed
    }

    /**
     * Converts a Comment entity to a CommentResponse DTO using the record constructor.
     *
     * @param comment The Comment entity to convert
     * @param isOwner Whether the current user owns this comment
     * @param isRecipeOwner Whether the current user owns the recipe this comment belongs to
     * @return The corresponding CommentResponse DTO
     */
    public CommentResponse toResponse(Comment comment, boolean isOwner, boolean isRecipeOwner) {
        if (comment == null) {
            throw new IllegalArgumentException("Cannot convert null comment to CommentResponse");
        }

        // Use CommentResponse record constructor with provided flags
        return new CommentResponse(
                comment.getId(),
                comment.getContent(),
                comment.getUserId(),
                comment.getUsername(),
                comment.getRecipeId(),
                comment.getCreatedAt(),
                comment.getUpdatedAt(),
                isOwner, // Use provided flag
                isRecipeOwner // Use provided flag
        );
    }

    /**
     * Creates a new Comment entity from a CommentRequest DTO using the entity builder.
     *
     * @param request The CommentRequest DTO to convert
     * @param userId The ID of the user creating the comment
     * @param username The username of the user creating the comment
     * @param recipeId The ID of the recipe being commented on
     * @return The corresponding new Comment entity
     */
    public Comment toEntity(CommentRequest request, UUID userId, String username, UUID recipeId) {
        if (request == null) {
            throw new IllegalArgumentException("Cannot convert null request to Comment");
        }
        
        // Use Comment entity builder and record accessor
        return Comment.builder()
                .content(request.content()) // Use record accessor
                .userId(userId)
                .username(username)
                .recipeId(recipeId)
                .build();
    }
} 