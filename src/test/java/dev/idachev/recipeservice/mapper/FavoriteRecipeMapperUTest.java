package dev.idachev.recipeservice.mapper;

import dev.idachev.recipeservice.model.FavoriteRecipe;
import dev.idachev.recipeservice.model.Recipe;
import dev.idachev.recipeservice.web.dto.FavoriteRecipeDto;
import dev.idachev.recipeservice.web.dto.RecipeResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FavoriteRecipeMapperUTest {

    @Mock
    private RecipeMapper recipeMapper;

    @Test
    void givenUserIdAndRecipeId_whenCreate_thenReturnFavoriteRecipeEntity() {

        // Given
        UUID userId = UUID.randomUUID();
        UUID recipeId = UUID.randomUUID();

        // When
        FavoriteRecipe result = FavoriteRecipeMapper.create(userId, recipeId);

        // Then
        assertNotNull(result);
        assertEquals(userId, result.getUserId());
        assertEquals(recipeId, result.getRecipeId());
        assertNull(result.getCreatedAt());
    }

    @Test
    void givenNullUserId_whenCreate_thenThrowNullPointerException() {

        // Given
        UUID userId = null;
        UUID recipeId = UUID.randomUUID();

        // When & Then
        assertThrows(NullPointerException.class, () -> FavoriteRecipeMapper.create(userId, recipeId));
    }

    @Test
    void givenNullRecipeId_whenCreate_thenThrowNullPointerException() {

        // Given
        UUID userId = UUID.randomUUID();
        UUID recipeId = null;

        // When & Then
        assertThrows(NullPointerException.class, () -> FavoriteRecipeMapper.create(userId, recipeId));
    }

    @Test
    void givenUserIdAndRecipe_whenCreate_thenReturnFavoriteRecipeEntity() {

        // Given
        UUID userId = UUID.randomUUID();
        UUID recipeId = UUID.randomUUID();

        Recipe recipe = Recipe.builder()
                .id(recipeId)
                .build();

        // When
        FavoriteRecipe result = FavoriteRecipeMapper.create(userId, recipe);

        // Then
        assertNotNull(result);
        assertEquals(userId, result.getUserId());
        assertEquals(recipeId, result.getRecipeId());
        assertNull(result.getCreatedAt());
    }

    @Test
    void givenNullRecipe_whenCreate_thenThrowNullPointerException() {

        // Given
        UUID userId = UUID.randomUUID();
        Recipe recipe = null;

        // When & Then
        assertThrows(NullPointerException.class, () -> FavoriteRecipeMapper.create(userId, recipe));
    }

    @Test
    void givenFavoriteRecipeAndRecipe_whenToDtoWithRecipe_thenReturnMappedDto() {

        // Given
        UUID userId = UUID.randomUUID();
        UUID recipeId = UUID.randomUUID();
        LocalDateTime addedAt = LocalDateTime.now();

        FavoriteRecipe favoriteRecipe = FavoriteRecipe.builder()
                .userId(userId)
                .recipeId(recipeId)
                .createdAt(addedAt)
                .build();

        Recipe recipe = Recipe.builder()
                .id(recipeId)
                .title("Test Recipe")
                .build();

        RecipeResponse recipeResponse = RecipeResponse.builder()
                .id(recipeId)
                .title("Test Recipe")
                .build();

        when(recipeMapper.toResponse(any(Recipe.class))).thenReturn(recipeResponse);

        // When
        FavoriteRecipeDto result = FavoriteRecipeMapper.toDtoWithRecipe(favoriteRecipe, recipe, recipeMapper);

        // Then
        assertNotNull(result);
        assertEquals(userId, result.getUserId());
        assertEquals(recipeId, result.getRecipeId());
        assertEquals(addedAt, result.getAddedAt());
        assertNotNull(result.getRecipe());
        assertEquals(recipeId, result.getRecipe().getId());
        assertEquals("Test Recipe", result.getRecipe().getTitle());
    }

    @Test
    void givenNullFavoriteRecipe_whenToDtoWithRecipe_thenThrowNullPointerException() {

        // Given
        FavoriteRecipe favoriteRecipe = null;
        Recipe recipe = Recipe.builder().build();

        // When & Then
        assertThrows(NullPointerException.class,
                () -> FavoriteRecipeMapper.toDtoWithRecipe(favoriteRecipe, recipe, recipeMapper));
    }

    @Test
    void givenNullRecipe_whenToDtoWithRecipe_thenThrowNullPointerException() {

        // Given
        FavoriteRecipe favoriteRecipe = FavoriteRecipe.builder().build();
        Recipe recipe = null;

        // When & Then
        assertThrows(NullPointerException.class,
                () -> FavoriteRecipeMapper.toDtoWithRecipe(favoriteRecipe, recipe, recipeMapper));
    }

    @Test
    void givenNullRecipeMapper_whenToDtoWithRecipe_thenThrowNullPointerException() {

        // Given
        FavoriteRecipe favoriteRecipe = FavoriteRecipe.builder().build();
        Recipe recipe = Recipe.builder().build();
        RecipeMapper mapper = null;

        // When & Then
        assertThrows(NullPointerException.class,
                () -> FavoriteRecipeMapper.toDtoWithRecipe(favoriteRecipe, recipe, mapper));
    }
} 