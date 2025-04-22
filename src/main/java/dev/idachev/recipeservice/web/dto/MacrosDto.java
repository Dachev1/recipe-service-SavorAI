package dev.idachev.recipeservice.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;

/**
 * Immutable Data Transfer Object for nutritional information (macronutrients) using Java Record.
 * Used in recipe requests and responses to represent nutritional content.
 * All values represent amounts per serving.
 */
@Schema(description = "Nutritional information (macronutrients) per serving")
public record MacrosDto(
    /**
     * Total calories per serving (kcal)
     */
    @Schema(description = "Total calories per serving in kcal", example = "450.00")
    @NotNull(message = "Calories must not be null")
    @PositiveOrZero(message = "Calories cannot be negative")
    @Digits(integer=6, fraction=2, message = "Calories format error")
    BigDecimal calories,
    
    /**
     * Protein content in grams per serving
     */
    @Schema(description = "Protein content in grams per serving", example = "12.50")
    @NotNull(message = "Protein must not be null")
    @PositiveOrZero(message = "Protein cannot be negative")
    @Digits(integer=6, fraction=2, message = "Protein format error")
    BigDecimal proteinGrams,
    
    /**
     * Carbohydrate content in grams per serving
     */
    @Schema(description = "Carbohydrate content in grams per serving", example = "58.30")
    @NotNull(message = "Carbs must not be null")
    @PositiveOrZero(message = "Carbs cannot be negative")
    @Digits(integer=6, fraction=2, message = "Carbs format error")
    BigDecimal carbsGrams,
    
    /**
     * Fat content in grams per serving
     */
    @Schema(description = "Fat content in grams per serving", example = "18.20")
    @NotNull(message = "Fat must not be null")
    @PositiveOrZero(message = "Fat cannot be negative")
    @Digits(integer=6, fraction=2, message = "Fat format error")
    BigDecimal fatGrams
) {} 