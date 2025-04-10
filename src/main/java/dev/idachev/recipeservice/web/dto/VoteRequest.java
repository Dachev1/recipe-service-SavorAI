package dev.idachev.recipeservice.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Data Transfer Object for recipe vote requests.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request object for voting on a recipe")
public class VoteRequest {
    
    @NotBlank(message = "Vote type cannot be empty")
    @Pattern(regexp = "^(up|down)$", message = "Vote type must be either 'up' or 'down'")
    @Schema(description = "Type of vote", example = "up", allowableValues = {"up", "down"})
    private String voteType;
} 