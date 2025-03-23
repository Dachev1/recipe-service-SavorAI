package dev.idachev.recipeservice.mapper;

import dev.idachev.recipeservice.model.DifficultyLevel;
import dev.idachev.recipeservice.web.dto.MacrosDto;
import dev.idachev.recipeservice.web.dto.RecipeRequest;
import dev.idachev.recipeservice.web.dto.SimplifiedRecipeResponse;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class AIServiceMapperUTest {

    @Test
    void givenCompleteRecipeRequest_whenToSimplifiedResponse_thenReturnMappedResponse() {

        // Given
        List<String> ingredients = Arrays.asList("Ingredient 1", "Ingredient 2");

        MacrosDto macros = MacrosDto.builder()
                .calories(500)
                .proteinGrams(30.0)
                .carbsGrams(60.0)
                .fatGrams(20.0)
                .build();

        RecipeRequest request = RecipeRequest.builder()
                .title("Test Recipe")
                .description("Test Description")
                .instructions("Instruction 1\nInstruction 2")
                .ingredients(ingredients)
                .totalTimeMinutes(30)
                .difficulty(DifficultyLevel.EASY)
                .macros(macros)
                .isAiGenerated(true)
                .build();

        String imageUrl = "http://example.com/image.jpg";

        // When
        SimplifiedRecipeResponse response = AIServiceMapper.toSimplifiedResponse(request, imageUrl);

        // Then
        assertNotNull(response);
        assertEquals("Test Recipe", response.getTitle());
        assertEquals("Test Description", response.getDescription());
        assertEquals("Instruction 1\nInstruction 2", response.getInstructions());
        assertEquals(ingredients, response.getIngredients());
        assertEquals(imageUrl, response.getImageUrl());
        assertEquals(30, response.getTotalTimeMinutes());
        assertEquals(DifficultyLevel.EASY.toString(), response.getDifficulty());
        assertNotNull(response.getMacros());
        assertEquals(500, response.getMacros().getCalories());
        assertEquals(30.0, response.getMacros().getProteinGrams());
        assertEquals(60.0, response.getMacros().getCarbsGrams());
        assertEquals(20.0, response.getMacros().getFatGrams());
    }

    @Test
    void givenRecipeRequestWithNullFields_whenToSimplifiedResponse_thenHandleNullsGracefully() {

        // Given
        RecipeRequest request = RecipeRequest.builder()
                .title("Test Recipe")
                .build();

        // When
        SimplifiedRecipeResponse response = AIServiceMapper.toSimplifiedResponse(request, null);

        // Then
        assertNotNull(response);
        assertEquals("Test Recipe", response.getTitle());
        assertNull(response.getDescription());
        assertNull(response.getInstructions());
        assertEquals(Collections.emptyList(), response.getIngredients());
        assertNull(response.getImageUrl());
        assertNull(response.getTotalTimeMinutes());
        assertEquals("MEDIUM", response.getDifficulty());
        assertNotNull(response.getMacros());
        assertNull(response.getMacros().getCalories());
        assertNull(response.getMacros().getProteinGrams());
        assertNull(response.getMacros().getCarbsGrams());
        assertNull(response.getMacros().getFatGrams());
    }

    @Test
    void givenNullRecipeRequest_whenToSimplifiedResponse_thenThrowNullPointerException() {

        // Given
        RecipeRequest request = null;
        String imageUrl = "http://example.com/image.jpg";

        // When & Then
        assertThrows(NullPointerException.class, () -> AIServiceMapper.toSimplifiedResponse(request, imageUrl));
    }
} 