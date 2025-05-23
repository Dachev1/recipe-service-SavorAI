package dev.idachev.recipeservice.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Immutable Data Transfer Object for favorite recipe information using Java Record.
 * Contains both favorite relationship data and the complete recipe details.
 */
@Schema(description = "Favorite recipe data including relationship and complete recipe details")
public record FavoriteRecipeDto(
    // Relationship identifiers
    @Schema(description = "ID of the recipe that has been favorited", example = "a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11")
    UUID recipeId,
    
    @Schema(description = "ID of the user who favorited the recipe", example = "b5eebc99-9c0b-4ef8-bb6d-6bb9bd380a22")
    UUID userId,
    
    // Timestamp information
    @Schema(description = "Date and time when the recipe was added to favorites", example = "2023-03-15T10:15:30")
    LocalDateTime addedAt,
    
    // Associated recipe details
    @Schema(description = "Complete recipe details")
    RecipeResponse recipe
) {} 