package dev.idachev.recipeservice.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Data Transfer Object for comment responses.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Response object for a comment")
public class CommentResponse {
    
    @Schema(description = "Comment ID", example = "123e4567-e89b-12d3-a456-426614174000")
    private UUID id;
    
    @Schema(description = "Content of the comment", example = "This recipe looks amazing!")
    private String content;
    
    @Schema(description = "ID of the user who created the comment", example = "123e4567-e89b-12d3-a456-426614174001")
    private UUID userId;
    
    @Schema(description = "Username of the user who created the comment", example = "user123")
    private String username;
    
    @Schema(description = "ID of the recipe this comment belongs to", example = "123e4567-e89b-12d3-a456-426614174002")
    private UUID recipeId;
    
    @Schema(description = "Date and time when the comment was created", example = "2023-03-15T10:15:30")
    private LocalDateTime createdAt;
    
    @Schema(description = "Date and time when the comment was last updated", example = "2023-03-15T10:15:30")
    private LocalDateTime updatedAt;
    
    @Schema(description = "Whether the current user is the author of this comment")
    private boolean isOwner;
} 