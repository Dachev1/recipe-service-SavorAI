package dev.idachev.recipeservice.web.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.List;

/**
 * DTO for handling error responses from the AI model when it detects non-food items
 * TODO: Investigate usage. Is this only used internally in AIService? Consider making immutable (record) for consistency.
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class AIErrorResponse {
    private String error;
    private List<String> nonFoodItems;
} 