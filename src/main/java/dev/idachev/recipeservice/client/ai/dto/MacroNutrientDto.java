package dev.idachev.recipeservice.client.ai.dto;

public record MacroNutrientDto(
    String macroNutrientName,
    String quantity,
    String unit
) {
} 