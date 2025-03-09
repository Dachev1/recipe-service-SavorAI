package dev.idachev.recipeservice.web.dto;

import dev.idachev.recipeservice.model.DifficultyLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Data Transfer Object for recipe responses.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecipeResponse {
    
    private UUID id;
    private String title;
    private String description;
    private String instructions;
    private String imageUrl;
    private List<String> ingredients;
    private Set<String> tags;
    private UUID createdById;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    // Total cooking time (minutes)
    private Integer totalTimeMinutes;
    
    // Nutritional information (macros)
    private MacrosDto macros;
    
    // Difficulty level
    private DifficultyLevel difficulty;
    
    // Servings
    private Integer servings;
    
    // AI generated flag
    private Boolean isAiGenerated;
    
    // Favorite information
    private Boolean isFavorite;
    private Long favoriteCount;
} 