package dev.idachev.recipeservice.client.ai.dto;

public record IngredientDto(
    String ingredientName,
    String quantity,
    String unit
) {
} 