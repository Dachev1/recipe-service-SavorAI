package service;

import dev.idachev.recipeservice.exception.ResourceNotFoundException;
import dev.idachev.recipeservice.mapper.RecipeMapper;
import dev.idachev.recipeservice.model.FavoriteRecipe;
import dev.idachev.recipeservice.model.Recipe;
import dev.idachev.recipeservice.repository.FavoriteRecipeRepository;
import dev.idachev.recipeservice.repository.RecipeRepository;
import dev.idachev.recipeservice.service.FavoriteRecipeService;
import dev.idachev.recipeservice.service.RecipeImageService;
import dev.idachev.recipeservice.web.dto.FavoriteRecipeDto;
import dev.idachev.recipeservice.web.dto.RecipeResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class FavoriteRecipeServiceUTest {

    @Mock
    private FavoriteRecipeRepository favoriteRecipeRepository;

    @Mock
    private RecipeRepository recipeRepository;

    @Mock
    private RecipeImageService recipeImageService;

    @Mock
    private RecipeMapper recipeMapper;

    @InjectMocks
    private FavoriteRecipeService favoriteRecipeService;

    @Test
    void givenNewFavorite_whenAddToFavorites_thenCreateAndReturnFavorite() {

        // Given
        UUID userId = UUID.randomUUID();
        UUID recipeId = UUID.randomUUID();

        Recipe recipe = Recipe.builder()
                .id(recipeId)
                .title("Favorite Recipe")
                .description("A recipe to be favorited")
                .imageUrl("http://example.com/image.jpg")
                .isAiGenerated(false)
                .build();

        RecipeResponse recipeResponse = RecipeResponse.builder()
                .id(recipeId)
                .title("Favorite Recipe")
                .description("A recipe to be favorited")
                .imageUrl("http://example.com/image.jpg")
                .build();

        FavoriteRecipe savedFavorite = FavoriteRecipe.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .recipeId(recipeId)
                .addedAt(LocalDateTime.now())
                .build();

        when(favoriteRecipeRepository.existsByUserIdAndRecipeId(userId, recipeId)).thenReturn(false);
        when(recipeRepository.findById(recipeId)).thenReturn(Optional.of(recipe));
        when(favoriteRecipeRepository.save(any(FavoriteRecipe.class))).thenReturn(savedFavorite);
        when(recipeMapper.toResponse(recipe)).thenReturn(recipeResponse);

        // When
        FavoriteRecipeDto result = favoriteRecipeService.addToFavorites(userId, recipeId);

        // Then
        assertNotNull(result);
        assertEquals(userId, result.getUserId());
        assertEquals(recipeId, result.getRecipeId());
        assertNotNull(result.getRecipe());
        assertEquals(recipeId, result.getRecipe().getId());

        verify(favoriteRecipeRepository).existsByUserIdAndRecipeId(userId, recipeId);
        verify(recipeRepository).findById(recipeId);
        verify(favoriteRecipeRepository).save(any(FavoriteRecipe.class));
        verify(recipeMapper).toResponse(recipe);
    }

    @Test
    void givenExistingFavorite_whenAddToFavorites_thenReturnExistingFavorite() {

        // Given
        UUID userId = UUID.randomUUID();
        UUID recipeId = UUID.randomUUID();

        Recipe recipe = Recipe.builder()
                .id(recipeId)
                .title("Favorite Recipe")
                .build();

        RecipeResponse recipeResponse = RecipeResponse.builder()
                .id(recipeId)
                .title("Favorite Recipe")
                .build();

        FavoriteRecipe existingFavorite = FavoriteRecipe.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .recipeId(recipeId)
                .addedAt(LocalDateTime.now())
                .build();

        when(favoriteRecipeRepository.existsByUserIdAndRecipeId(userId, recipeId)).thenReturn(true);
        when(favoriteRecipeRepository.findByUserIdAndRecipeId(userId, recipeId))
                .thenReturn(Optional.of(existingFavorite));
        when(recipeRepository.findById(recipeId)).thenReturn(Optional.of(recipe));
        when(recipeMapper.toResponse(recipe)).thenReturn(recipeResponse);

        // When
        FavoriteRecipeDto result = favoriteRecipeService.addToFavorites(userId, recipeId);

        // Then
        assertNotNull(result);
        assertEquals(userId, result.getUserId());
        assertEquals(recipeId, result.getRecipeId());
        assertNotNull(result.getRecipe());

        verify(favoriteRecipeRepository).existsByUserIdAndRecipeId(userId, recipeId);
        verify(favoriteRecipeRepository).findByUserIdAndRecipeId(userId, recipeId);
        verify(favoriteRecipeRepository, never()).save(any(FavoriteRecipe.class));
    }

    @Test
    void givenAiGeneratedRecipeWithoutImage_whenAddToFavorites_thenGenerateImage() {

        // Given
        UUID userId = UUID.randomUUID();
        UUID recipeId = UUID.randomUUID();

        Recipe recipe = Recipe.builder()
                .id(recipeId)
                .title("AI Recipe")
                .description("AI generated recipe")
                .isAiGenerated(true)
                .imageUrl(null) // No image yet
                .build();

        String generatedImageUrl = "http://example.com/ai-image.jpg";

        RecipeResponse recipeResponse = RecipeResponse.builder()
                .id(recipeId)
                .title("AI Recipe")
                .imageUrl(generatedImageUrl)
                .build();

        FavoriteRecipe savedFavorite = FavoriteRecipe.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .recipeId(recipeId)
                .build();

        when(favoriteRecipeRepository.existsByUserIdAndRecipeId(userId, recipeId)).thenReturn(false);
        when(recipeRepository.findById(recipeId)).thenReturn(Optional.of(recipe));
        when(recipeImageService.generateRecipeImage(recipe.getTitle(), recipe.getDescription()))
                .thenReturn(generatedImageUrl);
        when(favoriteRecipeRepository.save(any(FavoriteRecipe.class))).thenReturn(savedFavorite);
        when(recipeMapper.toResponse(recipe)).thenReturn(recipeResponse);

        // When
        FavoriteRecipeDto result = favoriteRecipeService.addToFavorites(userId, recipeId);

        // Then
        assertNotNull(result);
        assertEquals(userId, result.getUserId());
        assertEquals(recipeId, result.getRecipeId());
        assertEquals(generatedImageUrl, result.getRecipe().getImageUrl());

        verify(recipeImageService).generateRecipeImage(recipe.getTitle(), recipe.getDescription());
        verify(recipeRepository).save(recipe);
    }

    @Test
    void givenInvalidRecipeId_whenAddToFavorites_thenThrowResourceNotFoundException() {

        // Given
        UUID userId = UUID.randomUUID();
        UUID recipeId = UUID.randomUUID();

        when(favoriteRecipeRepository.existsByUserIdAndRecipeId(userId, recipeId)).thenReturn(false);
        when(recipeRepository.findById(recipeId)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(ResourceNotFoundException.class,
                () -> favoriteRecipeService.addToFavorites(userId, recipeId));

        verify(favoriteRecipeRepository, never()).save(any(FavoriteRecipe.class));
    }

    @Test
    void givenValidIds_whenRemoveFromFavorites_thenDeleteFavorite() {

        // Given
        UUID userId = UUID.randomUUID();
        UUID recipeId = UUID.randomUUID();

        FavoriteRecipe favoriteRecipe = FavoriteRecipe.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .recipeId(recipeId)
                .build();

        when(favoriteRecipeRepository.findByUserIdAndRecipeId(userId, recipeId))
                .thenReturn(Optional.of(favoriteRecipe));

        // When
        favoriteRecipeService.removeFromFavorites(userId, recipeId);

        // Then
        verify(favoriteRecipeRepository).findByUserIdAndRecipeId(userId, recipeId);
        verify(favoriteRecipeRepository).delete(favoriteRecipe);
    }

    @Test
    void givenInvalidFavoriteIds_whenRemoveFromFavorites_thenThrowResourceNotFoundException() {

        // Given
        UUID userId = UUID.randomUUID();
        UUID recipeId = UUID.randomUUID();

        when(favoriteRecipeRepository.findByUserIdAndRecipeId(userId, recipeId))
                .thenReturn(Optional.empty());

        // When & Then
        assertThrows(ResourceNotFoundException.class,
                () -> favoriteRecipeService.removeFromFavorites(userId, recipeId));

        verify(favoriteRecipeRepository).findByUserIdAndRecipeId(userId, recipeId);
        verify(favoriteRecipeRepository, never()).delete(any(FavoriteRecipe.class));
    }

    @Test
    void givenValidUserIdWithFavorites_whenGetUserFavorites_thenReturnPagedFavorites() {

        // Given
        UUID userId = UUID.randomUUID();
        Pageable pageable = Pageable.unpaged();

        UUID recipeId1 = UUID.randomUUID();
        UUID recipeId2 = UUID.randomUUID();

        FavoriteRecipe favorite1 = FavoriteRecipe.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .recipeId(recipeId1)
                .build();

        FavoriteRecipe favorite2 = FavoriteRecipe.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .recipeId(recipeId2)
                .build();

        List<FavoriteRecipe> favorites = Arrays.asList(favorite1, favorite2);
        Page<FavoriteRecipe> favoritesPage = new PageImpl<>(favorites, pageable, favorites.size());

        Recipe recipe1 = Recipe.builder()
                .id(recipeId1)
                .title("Recipe 1")
                .build();

        Recipe recipe2 = Recipe.builder()
                .id(recipeId2)
                .title("Recipe 2")
                .build();

        RecipeResponse response1 = RecipeResponse.builder()
                .id(recipeId1)
                .title("Recipe 1")
                .build();

        RecipeResponse response2 = RecipeResponse.builder()
                .id(recipeId2)
                .title("Recipe 2")
                .build();

        when(favoriteRecipeRepository.findByUserId(userId, pageable)).thenReturn(favoritesPage);
        when(recipeRepository.findAllById(Arrays.asList(recipeId1, recipeId2)))
                .thenReturn(Arrays.asList(recipe1, recipe2));
        when(recipeMapper.toResponse(recipe1)).thenReturn(response1);
        when(recipeMapper.toResponse(recipe2)).thenReturn(response2);

        // When
        Page<FavoriteRecipeDto> result = favoriteRecipeService.getUserFavorites(userId, pageable);

        // Then
        assertNotNull(result);
        assertEquals(2, result.getTotalElements());

        List<FavoriteRecipeDto> content = result.getContent();
        assertEquals(2, content.size());
        assertEquals(recipeId1, content.get(0).getRecipeId());
        assertEquals(recipeId2, content.get(1).getRecipeId());

        verify(favoriteRecipeRepository).findByUserId(userId, pageable);
        verify(recipeRepository).findAllById(Arrays.asList(recipeId1, recipeId2));
        verify(recipeMapper, times(2)).toResponse(any(Recipe.class));
    }

    @Test
    void givenValidUserIdWithoutFavorites_whenGetUserFavorites_thenReturnEmptyPage() {

        // Given
        UUID userId = UUID.randomUUID();
        Pageable pageable = Pageable.unpaged();

        Page<FavoriteRecipe> emptyPage = Page.empty(pageable);

        when(favoriteRecipeRepository.findByUserId(userId, pageable)).thenReturn(emptyPage);

        // When
        Page<FavoriteRecipeDto> result = favoriteRecipeService.getUserFavorites(userId, pageable);

        // Then
        assertNotNull(result);
        assertTrue(result.isEmpty());

        verify(favoriteRecipeRepository).findByUserId(userId, pageable);
        verify(recipeRepository, never()).findAllById(any());
    }

    @Test
    void givenValidUserIdWithFavorites_whenGetAllUserFavorites_thenReturnAllFavorites() {

        // Given
        UUID userId = UUID.randomUUID();

        UUID recipeId1 = UUID.randomUUID();
        UUID recipeId2 = UUID.randomUUID();

        FavoriteRecipe favorite1 = FavoriteRecipe.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .recipeId(recipeId1)
                .build();

        FavoriteRecipe favorite2 = FavoriteRecipe.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .recipeId(recipeId2)
                .build();

        List<FavoriteRecipe> favorites = Arrays.asList(favorite1, favorite2);

        Recipe recipe1 = Recipe.builder()
                .id(recipeId1)
                .title("Recipe 1")
                .build();

        Recipe recipe2 = Recipe.builder()
                .id(recipeId2)
                .title("Recipe 2")
                .build();

        RecipeResponse response1 = RecipeResponse.builder()
                .id(recipeId1)
                .title("Recipe 1")
                .build();

        RecipeResponse response2 = RecipeResponse.builder()
                .id(recipeId2)
                .title("Recipe 2")
                .build();

        when(favoriteRecipeRepository.findByUserId(userId)).thenReturn(favorites);
        when(recipeRepository.findAllById(Arrays.asList(recipeId1, recipeId2)))
                .thenReturn(Arrays.asList(recipe1, recipe2));
        when(recipeMapper.toResponse(recipe1)).thenReturn(response1);
        when(recipeMapper.toResponse(recipe2)).thenReturn(response2);

        // When
        List<FavoriteRecipeDto> result = favoriteRecipeService.getAllUserFavorites(userId);

        // Then
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals(recipeId1, result.get(0).getRecipeId());
        assertEquals(recipeId2, result.get(1).getRecipeId());

        verify(favoriteRecipeRepository).findByUserId(userId);
        verify(recipeRepository).findAllById(Arrays.asList(recipeId1, recipeId2));
        verify(recipeMapper, times(2)).toResponse(any(Recipe.class));
    }

    @Test
    void givenValidUserIdWithoutFavorites_whenGetAllUserFavorites_thenReturnEmptyList() {

        // Given
        UUID userId = UUID.randomUUID();

        when(favoriteRecipeRepository.findByUserId(userId)).thenReturn(Collections.emptyList());

        // When
        List<FavoriteRecipeDto> result = favoriteRecipeService.getAllUserFavorites(userId);

        // Then
        assertNotNull(result);
        assertTrue(result.isEmpty());

        verify(favoriteRecipeRepository).findByUserId(userId);
        verify(recipeRepository, never()).findAllById(any());
    }

    @Test
    void givenValidIds_whenIsRecipeInFavorites_thenReturnTrue() {

        // Given
        UUID userId = UUID.randomUUID();
        UUID recipeId = UUID.randomUUID();

        when(favoriteRecipeRepository.existsByUserIdAndRecipeId(userId, recipeId)).thenReturn(true);

        // When
        boolean result = favoriteRecipeService.isRecipeInFavorites(userId, recipeId);

        // Then
        assertTrue(result);
        verify(favoriteRecipeRepository).existsByUserIdAndRecipeId(userId, recipeId);
    }

    @Test
    void givenValidRecipeId_whenGetFavoriteCount_thenReturnCount() {

        // Given
        UUID recipeId = UUID.randomUUID();
        long expectedCount = 5;

        when(favoriteRecipeRepository.countByRecipeId(recipeId)).thenReturn(expectedCount);

        // When
        long result = favoriteRecipeService.getFavoriteCount(recipeId);

        // Then
        assertEquals(expectedCount, result);
        verify(favoriteRecipeRepository).countByRecipeId(recipeId);
    }
}