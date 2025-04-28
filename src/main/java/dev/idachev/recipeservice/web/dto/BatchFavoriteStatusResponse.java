package dev.idachev.recipeservice.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Map;
import java.util.UUID;

/**
 * DTO representing the favorite status for multiple recipes.
 */
@Schema(description = "Response containing the favorite status for multiple recipes")
public record BatchFavoriteStatusResponse(
    @Schema(description = "Map of recipe IDs to their favorite status", 
            example = "{\"a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11\": true, \"b5eebc99-9c0b-4ef8-bb6d-6bb9bd380a22\": false}")
    Map<UUID, Boolean> favoriteStatuses
) {} 