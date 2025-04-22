package dev.idachev.recipeservice.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Immutable Data Transfer Object for comment requests using Java Record.
 */
@Schema(description = "Request object for creating or updating a comment")
public record CommentRequest(
    
    @NotBlank(message = "Comment content cannot be empty")
    @Size(min = 1, max = 1000, message = "Comment content must be between 1 and 1000 characters")
    @Schema(description = "Content of the comment", example = "This recipe looks amazing!")
    String content
) {} 