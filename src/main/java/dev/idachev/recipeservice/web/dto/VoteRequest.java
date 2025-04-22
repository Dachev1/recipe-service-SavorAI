package dev.idachev.recipeservice.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * Immutable Data Transfer Object for recipe vote requests using Java Record.
 */
@Schema(description = "Request object for voting on a recipe")
public record VoteRequest(
    
    @NotBlank(message = "Vote type cannot be empty")
    @Pattern(regexp = "^(up|down)$", message = "Vote type must be either 'up' or 'down'")
    @Schema(description = "Type of vote", example = "up", allowableValues = {"up", "down"})
    String voteType
) {} 