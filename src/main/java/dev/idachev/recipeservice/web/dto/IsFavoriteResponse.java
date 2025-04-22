package dev.idachev.recipeservice.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * DTO representing whether a recipe is favorited by the user.
 */
@Schema(description = "Response indicating if a recipe is favorited by the current user")
public record IsFavoriteResponse(
    @Schema(description = "True if the recipe is favorited, false otherwise", example = "true")
    boolean isFavorite
) {} 