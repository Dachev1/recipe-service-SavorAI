package dev.idachev.recipeservice.web.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * Immutable Error response DTO using Java Record for consistent API error handling.
 * Provides standardized error information that follows REST API conventions.
 * Includes error status, message, timestamp, and detailed validation errors.
 */
@JsonInclude(JsonInclude.Include.NON_NULL) // Keep NON_NULL to omit 'details' when not present
@Schema(description = "Standardized error response for API errors")
public record ErrorResponse(
    // HTTP status code
    @Schema(description = "HTTP status code", example = "400")
    int status,
    
    // Error message
    @Schema(description = "Human-readable error message", example = "Validation failed: Recipe title is required")
    String message,
    
    // When the error occurred
    @Schema(description = "Timestamp when the error occurred", example = "2023-03-15T10:15:30")
    LocalDateTime timestamp,
    
    // Field-specific validation errors (key = field name, value = error message)
    @Schema(description = "Field-specific validation errors (key = field name, value = error message)", 
            example = "{\"title\":\"Recipe title is required\",\"ingredients\":\"At least one ingredient is required\"}")
    // Note: Ensure an unmodifiable map is passed during record creation for true immutability
    Map<String, String> details
) {
    // Convenience constructor without details for simpler errors
    public ErrorResponse(int status, String message, LocalDateTime timestamp) {
        this(status, message, timestamp, null);
    }
} 