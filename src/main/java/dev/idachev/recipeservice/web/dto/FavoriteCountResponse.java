package dev.idachev.recipeservice.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * DTO representing the favorite count for a recipe.
 */
@Schema(description = "Response containing the total favorite count for a recipe")
public record FavoriteCountResponse(
    @Schema(description = "Total number of users who have favorited the recipe", example = "42")
    long count
) {} 