package dev.idachev.recipeservice.mapper;

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

    /**
     * Converts a Comment entity to a CommentResponse DTO.
     *
     * @param comment The Comment entity to convert
     * @param currentUserId The ID of the current user to determine ownership
     * @return The corresponding CommentResponse DTO
     */
    public CommentResponse toResponse(Comment comment, UUID currentUserId) {
        if (comment == null) {
            throw new IllegalArgumentException("Cannot convert null comment to CommentResponse");
        }

        return CommentResponse.builder()
                .id(comment.getId())
                .content(comment.getContent())
                .userId(comment.getUserId())
                .username(comment.getUsername())
                .recipeId(comment.getRecipeId())
                .createdAt(comment.getCreatedAt())
                .updatedAt(comment.getUpdatedAt())
                .isOwner(currentUserId != null && currentUserId.equals(comment.getUserId()))
                .build();
    }

    /**
     * Creates a new Comment entity from a CommentRequest DTO.
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
        
        return Comment.builder()
                .content(request.getContent())
                .userId(userId)
                .username(username)
                .recipeId(recipeId)
                .build();
    }

    /**
     * Updates an existing Comment entity from a CommentRequest DTO.
     *
     * @param comment The existing Comment entity to update
     * @param request The CommentRequest DTO containing the new data
     */
    public void updateEntityFromRequest(Comment comment, CommentRequest request) {
        if (comment == null || request == null) {
            throw new IllegalArgumentException("Comment and request cannot be null");
        }
        
        comment.setContent(request.getContent());
    }
} 