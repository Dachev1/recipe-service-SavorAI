package dev.idachev.recipeservice.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Immutable Data Transfer Object for comment responses using Java Record.
 */
@Schema(description = "Response object for a comment")
public record CommentResponse(
    
    @Schema(description = "Comment ID", example = "123e4567-e89b-12d3-a456-426614174000")
    UUID id,
    
    @Schema(description = "Content of the comment", example = "This recipe looks amazing!")
    String content,
    
    @Schema(description = "ID of the user who created the comment", example = "123e4567-e89b-12d3-a456-426614174001")
    UUID userId,
    
    @Schema(description = "Username of the user who created the comment", example = "user123")
    String username,
    
    @Schema(description = "ID of the recipe this comment belongs to", example = "123e4567-e89b-12d3-a456-426614174002")
    UUID recipeId,
    
    @Schema(description = "Date and time when the comment was created", example = "2023-03-15T10:15:30")
    LocalDateTime createdAt,
    
    @Schema(description = "Date and time when the comment was last updated", example = "2023-03-15T10:15:30")
    LocalDateTime updatedAt,
    
    @Schema(description = "Whether the current user is the author of this comment")
    boolean isOwner,
    
    @Schema(description = "Whether the current user is the owner of the recipe this comment belongs to")
    boolean isRecipeOwner
) {} 