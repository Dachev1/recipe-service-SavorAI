package dev.idachev.recipeservice.web.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Data Transfer Object for meal generation requests.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GenerateMealRequest {
    
    @NotEmpty(message = "At least one ingredient is required")
    private List<String> ingredients;
    
    private List<String> dietaryRestrictions;
    
    private String mealType;
    
    private String cuisine;
} 