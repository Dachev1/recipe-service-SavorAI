package dev.idachev.recipeservice.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import java.util.UUID;

/**
 * DTO for batch operations on favorite recipes.
 */
@Schema(description = "Request containing a list of recipe IDs for batch operations")
public record BatchFavoriteRequest(
    @Schema(description = "List of recipe IDs to perform batch operation on", 
            example = "[\"a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11\", \"b5eebc99-9c0b-4ef8-bb6d-6bb9bd380a22\"]")
    List<UUID> recipeIds
) {} 