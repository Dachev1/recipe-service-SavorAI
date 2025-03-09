package dev.idachev.recipeservice.web.dto;

import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO for favorite recipe information.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FavoriteRecipeDto {
    private UUID recipeId;
    private UUID userId;
    private LocalDateTime addedAt;
    private RecipeResponse recipe;
} 