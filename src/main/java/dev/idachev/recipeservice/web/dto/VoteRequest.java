package dev.idachev.recipeservice.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

/**
 * Immutable Data Transfer Object for recipe vote requests using Java Record.
 */
@Schema(description = "Request object for voting on a recipe")
public record VoteRequest(
    
    @NotBlank(message = "Vote type cannot be empty")
    @Schema(description = "Type of vote", example = "UPVOTE", allowableValues = {"UPVOTE", "DOWNVOTE"})
    String voteType
) {} 