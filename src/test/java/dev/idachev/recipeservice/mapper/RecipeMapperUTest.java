package dev.idachev.recipeservice.mapper;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.idachev.recipeservice.model.DifficultyLevel;
import dev.idachev.recipeservice.model.Macros;
import dev.idachev.recipeservice.model.Recipe;
import dev.idachev.recipeservice.web.dto.MacrosDto;
import dev.idachev.recipeservice.web.dto.RecipeRequest;
import dev.idachev.recipeservice.web.dto.RecipeResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class RecipeMapperUTest {

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private RecipeMapper recipeMapper;

    private final String INGREDIENTS_JSON = "[\"Ingredient 1\", \"Ingredient 2\"]";
    private final List<String> INGREDIENTS_LIST = Arrays.asList("Ingredient 1", "Ingredient 2");

    @BeforeEach
    void setUp() throws JsonProcessingException {
        // Set up common mocking behavior
        when(objectMapper.writeValueAsString(any())).thenReturn(INGREDIENTS_JSON);
        when(objectMapper.readValue(anyString(), any(TypeReference.class))).thenReturn(INGREDIENTS_LIST);

        recipeMapper = new RecipeMapper(objectMapper);
    }

    @Test
    void givenCompleteRecipe_whenToResponse_thenReturnMappedResponse() throws JsonProcessingException {

        // Given
        UUID recipeId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();

        Macros macros = new Macros();
        macros.setCalories(500.0);
        macros.setProteinGrams(30.0);
        macros.setCarbsGrams(60.0);
        macros.setFatGrams(20.0);

        Recipe recipe = Recipe.builder()
                .id(recipeId)
                .title("Test Recipe")
                .instructions("Test Instructions")
                .imageUrl("http://example.com/image.jpg")
                .ingredients(INGREDIENTS_JSON)
                .userId(userId)
                .createdAt(now)
                .updatedAt(now)
                .totalTimeMinutes(30)
                .macros(macros)
                .difficulty(DifficultyLevel.EASY.name())
                .isAiGenerated(true)
                .build();

        when(objectMapper.readValue(anyString(), any(TypeReference.class))).thenReturn(INGREDIENTS_LIST);

        // When
        RecipeResponse response = recipeMapper.toResponse(recipe);

        // Then
        assertNotNull(response);
        assertEquals(recipeId, response.getId());
        assertEquals("Test Recipe", response.getTitle());
        assertEquals("Test Instructions", response.getInstructions());
        assertEquals("http://example.com/image.jpg", response.getImageUrl());
        assertEquals(INGREDIENTS_LIST, response.getIngredients());
        assertEquals(userId, response.getCreatedById());
        assertTrue(response.getCreatedAt() != null);
        assertTrue(response.getUpdatedAt() != null);
        assertEquals(30, response.getTotalTimeMinutes());
        assertNotNull(response.getMacros());
        assertEquals(500, response.getMacros().getCalories());
        assertEquals(30.0, response.getMacros().getProteinGrams());
        assertEquals(60.0, response.getMacros().getCarbsGrams());
        assertEquals(20.0, response.getMacros().getFatGrams());
        assertEquals(DifficultyLevel.EASY, response.getDifficulty());
        assertTrue(response.getIsAiGenerated());
        assertFalse(response.getIsFavorite());
        assertEquals(0L, response.getFavoriteCount());
    }

    @Test
    void givenNullRecipe_whenToResponse_thenThrowIllegalArgumentException() {

        // Given
        Recipe recipe = null;

        // When & Then
        assertThrows(IllegalArgumentException.class, () -> recipeMapper.toResponse(recipe));
    }

    @Test
    void givenRecipeWithNullFields_whenToResponse_thenHandleNullsGracefully() throws JsonProcessingException {

        // Given
        UUID recipeId = UUID.randomUUID();

        Recipe recipe = Recipe.builder()
                .id(recipeId)
                .title("Test Recipe")
                .createdAt(null)
                .updatedAt(null)
                .build();

        when(objectMapper.readValue(anyString(), any(TypeReference.class))).thenReturn(Collections.emptyList());

        // When
        RecipeResponse response = recipeMapper.toResponse(recipe);

        // Then
        assertNotNull(response);
        assertEquals("Test Recipe", response.getTitle());
        assertNull(response.getDescription());
        assertNull(response.getInstructions());
        assertNull(response.getImageUrl());
        assertEquals(Collections.emptyList(), response.getIngredients());
        assertNull(response.getCreatedById());
        assertNull(response.getCreatedAt());
        assertNull(response.getUpdatedAt());
        assertNull(response.getTotalTimeMinutes());
        assertNull(response.getMacros());
        assertNull(response.getDifficulty());
        assertFalse(response.getIsAiGenerated());
        assertFalse(response.getIsFavorite());
        assertEquals(0L, response.getFavoriteCount());
    }

    @Test
    void givenCompleteRecipeRequest_whenToEntity_thenReturnMappedEntity() throws JsonProcessingException {

        // Given
        MacrosDto macrosDto = MacrosDto.builder()
                .calories(500)
                .proteinGrams(30.0)
                .carbsGrams(60.0)
                .fatGrams(20.0)
                .build();

        RecipeRequest request = RecipeRequest.builder()
                .title("Test Recipe")
                .description("Test Description")
                .instructions("Test Instructions")
                .imageUrl("http://example.com/image.jpg")
                .ingredients(INGREDIENTS_LIST)
                .totalTimeMinutes(30)
                .difficulty(DifficultyLevel.EASY)
                .macros(macrosDto)
                .isAiGenerated(true)
                .build();

        when(objectMapper.writeValueAsString(any())).thenReturn(INGREDIENTS_JSON);

        // When
        Recipe result = recipeMapper.toEntity(request);

        // Then
        assertNotNull(result);
        assertEquals("Test Recipe", result.getTitle());
        assertEquals("Test Description", result.getDescription());
        assertEquals("Test Instructions", result.getInstructions());
        assertEquals("http://example.com/image.jpg", result.getImageUrl());
        assertEquals(INGREDIENTS_JSON, result.getIngredients());
        assertEquals(30, result.getTotalTimeMinutes());
        assertEquals("EASY", result.getDifficulty());
        assertTrue(result.getIsAiGenerated());
        assertNotNull(result.getMacros());
        assertEquals(500.0, result.getMacros().getCalories());
        assertEquals(30.0, result.getMacros().getProteinGrams());
        assertEquals(60.0, result.getMacros().getCarbsGrams());
        assertEquals(20.0, result.getMacros().getFatGrams());
    }

    @Test
    void givenNullRecipeRequest_whenToEntity_thenThrowIllegalArgumentException() {

        // Given
        RecipeRequest request = null;

        // When & Then
        assertThrows(IllegalArgumentException.class, () -> recipeMapper.toEntity(request));
    }

    @Test
    void givenRecipeRequestWithNullFields_whenToEntity_thenHandleNullsGracefully() throws JsonProcessingException {

        // Given
        RecipeRequest request = RecipeRequest.builder()
                .title("Test Recipe")
                .build();

        when(objectMapper.writeValueAsString(any())).thenReturn("[]");

        // When
        Recipe result = recipeMapper.toEntity(request);

        // Then
        assertNotNull(result);
        assertEquals("Test Recipe", result.getTitle());
        assertNull(result.getDescription());
        assertNull(result.getInstructions());
        assertNull(result.getImageUrl());
        assertEquals("[]", result.getIngredients());
        assertNull(result.getTotalTimeMinutes());
        assertNull(result.getDifficulty());
        assertFalse(result.getIsAiGenerated());
        assertNull(result.getMacros());
    }

    @Test
    void givenRecipeAndRequest_whenUpdateEntityFromRequest_thenUpdateEntityCorrectly() throws JsonProcessingException {

        // Given
        Recipe recipe = Recipe.builder()
                .title("Old Title")
                .description("Old Description")
                .instructions("Old Instructions")
                .imageUrl("http://example.com/old.jpg")
                .ingredients("[]")
                .totalTimeMinutes(20)
                .difficulty(DifficultyLevel.HARD.name())
                .isAiGenerated(false)
                .build();

        MacrosDto macrosDto = MacrosDto.builder()
                .calories(500)
                .proteinGrams(30.0)
                .carbsGrams(60.0)
                .fatGrams(20.0)
                .build();

        RecipeRequest request = RecipeRequest.builder()
                .title("New Title")
                .description("New Description")
                .instructions("New Instructions")
                .imageUrl("http://example.com/new.jpg")
                .ingredients(INGREDIENTS_LIST)
                .totalTimeMinutes(30)
                .difficulty(DifficultyLevel.EASY)
                .macros(macrosDto)
                .isAiGenerated(true)
                .build();

        when(objectMapper.writeValueAsString(any())).thenReturn(INGREDIENTS_JSON);

        // When
        recipeMapper.updateEntityFromRequest(recipe, request);

        // Then
        assertEquals("New Title", recipe.getTitle());
        assertEquals("New Description", recipe.getDescription());
        assertEquals("New Instructions", recipe.getInstructions());
        assertEquals("http://example.com/new.jpg", recipe.getImageUrl());
        assertEquals(INGREDIENTS_JSON, recipe.getIngredients());
        assertEquals(30, recipe.getTotalTimeMinutes());
        assertEquals("EASY", recipe.getDifficulty());
        assertTrue(recipe.getIsAiGenerated());
        assertNotNull(recipe.getMacros());
        assertEquals(500.0, recipe.getMacros().getCalories());
        assertEquals(30.0, recipe.getMacros().getProteinGrams());
        assertEquals(60.0, recipe.getMacros().getCarbsGrams());
        assertEquals(20.0, recipe.getMacros().getFatGrams());
    }

    @Test
    void givenNullRecipe_whenUpdateEntityFromRequest_thenThrowIllegalArgumentException() {
        // Given
        Recipe recipe = null;
        RecipeRequest request = RecipeRequest.builder().title("Test").build();

        // When & Then
        assertThrows(IllegalArgumentException.class, () -> recipeMapper.updateEntityFromRequest(recipe, request));
    }

    @Test
    void givenNullRequest_whenUpdateEntityFromRequest_thenThrowIllegalArgumentException() {
        // Given
        Recipe recipe = Recipe.builder().build();
        RecipeRequest request = null;

        // When & Then
        assertThrows(IllegalArgumentException.class, () -> recipeMapper.updateEntityFromRequest(recipe, request));
    }

    @Test
    void givenRecipeAndPartialRequest_whenUpdateEntityFromRequest_thenOnlyUpdateProvidedFields() throws JsonProcessingException {

        // Given
        Recipe recipe = Recipe.builder()
                .title("Old Title")
                .description("Old Description")
                .instructions("Old Instructions")
                .imageUrl("http://example.com/old.jpg")
                .ingredients("[]")
                .totalTimeMinutes(20)
                .difficulty(DifficultyLevel.HARD.name())
                .isAiGenerated(false)
                .build();

        RecipeRequest request = RecipeRequest.builder()
                .title("New Title")
                .description("New Description")
                .instructions("New Instructions")
                .build();

        // When
        recipeMapper.updateEntityFromRequest(recipe, request);

        // Then
        assertEquals("New Title", recipe.getTitle());
        assertEquals("New Description", recipe.getDescription());
        assertEquals("New Instructions", recipe.getInstructions());
        assertEquals("http://example.com/old.jpg", recipe.getImageUrl());
        assertEquals("[]", recipe.getIngredients());
        assertEquals(20, recipe.getTotalTimeMinutes());
        assertEquals("HARD", recipe.getDifficulty());
        assertFalse(recipe.getIsAiGenerated());
    }
} 