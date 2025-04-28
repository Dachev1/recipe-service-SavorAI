package dev.idachev.recipeservice.web.dto;

public record IngredientResponse(
    String ingredientName,
    String quantity,
    String unit
) {
} 