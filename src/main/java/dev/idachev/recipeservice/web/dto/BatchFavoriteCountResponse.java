package dev.idachev.recipeservice.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Map;
import java.util.UUID;

/**
 * DTO representing the favorite count for multiple recipes.
 */
@Schema(description = "Response containing the favorite counts for multiple recipes")
public record BatchFavoriteCountResponse(
    @Schema(description = "Map of recipe IDs to their favorite counts", 
            example = "{\"a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11\": 42, \"b5eebc99-9c0b-4ef8-bb6d-6bb9bd380a22\": 7}")
    Map<UUID, Long> favoriteCounts
) {} 