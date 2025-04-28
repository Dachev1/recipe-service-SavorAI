package dev.idachev.recipeservice.client.ai.dto;

import java.util.List;

public record AISimplifiedRecipeDto(
    List<IngredientDto> ingredients,
    List<MacroNutrientDto> macroNutrients,
    String difficultyLevel,
    String recipeName,
    String recipeDescription,
    String recipeInstructions,
    String recipeImageUrl,
    String recipeId
) {
} 