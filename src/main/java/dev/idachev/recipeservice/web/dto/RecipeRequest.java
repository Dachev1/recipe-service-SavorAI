package dev.idachev.recipeservice.web.dto;

import dev.idachev.recipeservice.model.DifficultyLevel;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Data Transfer Object for recipe creation and update requests.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecipeRequest {

    @NotBlank(message = "Recipe title is required")
    @Size(max = 255, message = "Title must be less than 255 characters")
    private String title;

    @Size(max = 1000, message = "Description must be less than 1000 characters")
    private String description;

    @NotBlank(message = "Instructions are required")
    private String instructions;

    private String imageUrl;

    @NotEmpty(message = "At least one ingredient is required")
    private List<String> ingredients;

    // Total cooking time (minutes)
    @PositiveOrZero(message = "Total time cannot be negative")
    private Integer totalTimeMinutes;

    // Nutritional information (macros)
    @Valid
    private MacrosDto macros;

    // Difficulty level
    private DifficultyLevel difficulty;
} 