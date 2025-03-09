package dev.idachev.recipeservice.web.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.PositiveOrZero;

/**
 * DTO for representing nutritional information (macros) in API requests and responses.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MacrosDto {
    
    @PositiveOrZero(message = "Calories cannot be negative")
    private Integer calories;
    
    @PositiveOrZero(message = "Protein cannot be negative")
    private Double proteinGrams;
    
    @PositiveOrZero(message = "Carbs cannot be negative")
    private Double carbsGrams;
    
    @PositiveOrZero(message = "Fat cannot be negative")
    private Double fatGrams;
} 