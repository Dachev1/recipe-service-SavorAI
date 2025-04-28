package dev.idachev.recipeservice.web.dto;

public record MacroNutrientResponse(
    String macroNutrientName,
    String quantity,
    String unit
) {
} 