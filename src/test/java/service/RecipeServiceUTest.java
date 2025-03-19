package service;

import dev.idachev.recipeservice.exception.ResourceNotFoundException;
import dev.idachev.recipeservice.exception.UnauthorizedAccessException;
import dev.idachev.recipeservice.infrastructure.ai.AIService;
import dev.idachev.recipeservice.mapper.RecipeMapper;
import dev.idachev.recipeservice.model.Recipe;
import dev.idachev.recipeservice.repository.FavoriteRecipeRepository;
import dev.idachev.recipeservice.repository.RecipeRepository;
import dev.idachev.recipeservice.service.RecipeImageService;
import dev.idachev.recipeservice.service.RecipeSearchService;
import dev.idachev.recipeservice.service.RecipeService;
import dev.idachev.recipeservice.web.dto.RecipeRequest;
import dev.idachev.recipeservice.web.dto.RecipeResponse;
import dev.idachev.recipeservice.web.dto.SimplifiedRecipeResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class RecipeServiceUTest {

    @Mock
    private RecipeRepository recipeRepository;

    @Mock
    private FavoriteRecipeRepository favoriteRecipeRepository;

    @Mock
    private RecipeImageService recipeImageService;

    @Mock
    private RecipeSearchService recipeSearchService;

    @Mock
    private AIService aiService;

    @Mock
    private RecipeMapper recipeMapper;

    @Mock
    private MultipartFile mockImage;

    @InjectMocks
    private RecipeService recipeService;

    @Test
    void givenValidRequest_whenCreateRecipe_thenReturnRecipeResponse() {

        // Given
        UUID userId = UUID.randomUUID();

        RecipeRequest request = RecipeRequest.builder()
                .title("Test Recipe")
                .description("Test Description")
                .build();

        Recipe recipe = Recipe.builder()
                .title("Test Recipe")
                .description("Test Description")
                .build();

        Recipe savedRecipe = Recipe.builder()
                .id(UUID.randomUUID())
                .title("Test Recipe")
                .description("Test Description")
                .userId(userId)
                .build();

        RecipeResponse response = RecipeResponse.builder()
                .id(savedRecipe.getId())
                .title(savedRecipe.getTitle())
                .description(savedRecipe.getDescription())
                .build();

        when(recipeMapper.toEntity(request)).thenReturn(recipe);
        when(recipeRepository.save(any(Recipe.class))).thenReturn(savedRecipe);
        when(recipeMapper.toResponse(savedRecipe)).thenReturn(response);
        when(favoriteRecipeRepository.countByRecipeId(response.getId())).thenReturn(0L);
        when(favoriteRecipeRepository.existsByUserIdAndRecipeId(userId, response.getId())).thenReturn(false);

        // When
        RecipeResponse result = recipeService.createRecipe(request, userId);

        // Then
        assertNotNull(result);
        assertEquals(savedRecipe.getId(), result.getId());
        assertEquals(savedRecipe.getTitle(), result.getTitle());
        assertEquals(savedRecipe.getDescription(), result.getDescription());
        assertEquals(0L, result.getFavoriteCount());
        assertFalse(result.getIsFavorite());

        verify(recipeRepository).save(any(Recipe.class));
        verify(recipeMapper).toEntity(request);
        verify(recipeMapper).toResponse(savedRecipe);

    }

    @Test
    void givenImageFile_whenCreateRecipe_thenProcessImage() {

        // Given
        UUID userId = UUID.randomUUID();

        RecipeRequest request = RecipeRequest.builder()
                .title("Test Recipe")
                .description("Test Description")
                .build();

        String imageUrl = "http://example.com/image.jpg";

        Recipe recipe = Recipe.builder()
                .title("Test Recipe")
                .description("Test Description")
                .build();

        Recipe savedRecipe = Recipe.builder()
                .id(UUID.randomUUID())
                .title("Test Recipe")
                .description("Test Description")
                .userId(userId)
                .imageUrl(imageUrl)
                .build();

        RecipeResponse response = RecipeResponse.builder()
                .id(savedRecipe.getId())
                .title(savedRecipe.getTitle())
                .description(savedRecipe.getDescription())
                .imageUrl(savedRecipe.getImageUrl())
                .build();

        when(mockImage.isEmpty()).thenReturn(false);
        when(recipeImageService.processRecipeImage(request.getTitle(), request.getDescription(), mockImage)).thenReturn(imageUrl);
        when(recipeMapper.toEntity(request)).thenReturn(recipe);
        when(recipeRepository.save(any(Recipe.class))).thenReturn(savedRecipe);
        when(recipeMapper.toResponse(savedRecipe)).thenReturn(response);

        // When
        RecipeResponse result = recipeService.createRecipe(request, mockImage, userId);

        // Then
        assertNotNull(result);
        assertEquals(imageUrl, response.getImageUrl());
        verify(recipeImageService).processRecipeImage(request.getTitle(), request.getDescription(), mockImage);
    }

    @Test
    void givenValidId_whenGetRecipeById_thenReturnRecipeResponse() {

        // Given
        UUID recipeId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        Recipe recipe = Recipe.builder()
                .id(recipeId)
                .title("Test Recipe")
                .description("Test Description")
                .userId(userId)
                .build();

        RecipeResponse response = RecipeResponse.builder()
                .id(recipeId)
                .title("Test Recipe")
                .description("Test Description")
                .build();

        when(recipeRepository.findById(recipeId)).thenReturn(Optional.of(recipe));
        when(recipeMapper.toResponse(recipe)).thenReturn(response);
        when(favoriteRecipeRepository.countByRecipeId(recipeId)).thenReturn(5L);
        when(favoriteRecipeRepository.existsByUserIdAndRecipeId(userId, recipeId)).thenReturn(true);

        // When
        RecipeResponse result = recipeService.getRecipeById(recipeId, userId);

        // Then
        assertNotNull(result);
        assertEquals(recipeId, result.getId());
        assertEquals(5L, result.getFavoriteCount());
        assertTrue(result.getIsFavorite());

        verify(recipeRepository).findById(recipeId);
        verify(favoriteRecipeRepository).countByRecipeId(recipeId);
        verify(favoriteRecipeRepository).existsByUserIdAndRecipeId(userId, recipeId);
    }

    @Test
    void givenInvalidId_whenGetRecipeById_thenThrowRecipeNotFoundException() {

        // Given
        UUID recipeId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        when(recipeRepository.findById(recipeId)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(ResourceNotFoundException.class, () -> recipeService.getRecipeById(recipeId, userId));
        verify(recipeRepository).findById(recipeId);
    }

    @Test
    void givenValidPagination_whenGetAllRecipes_thenReturnPagedRecipes() {

        // Given
        UUID userId = UUID.randomUUID();
        Pageable pageable = Pageable.unpaged();
        Page<RecipeResponse> expectedPage = Page.empty();

        when(recipeSearchService.getAllRecipes(pageable, userId)).thenReturn(expectedPage);

        //When
        Page<RecipeResponse> result = recipeService.getAllRecipes(pageable, userId);

        // Then
        assertNotNull(result);
        assertEquals(expectedPage, result);
        verify(recipeSearchService).getAllRecipes(pageable, userId);
    }

    @Test
    void givenValidOwner_whenUpdateRecipe_thenUpdateRecipe() {

        // Given
        UUID recipeId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        RecipeRequest request = RecipeRequest.builder()
                .title("Updated Recipe")
                .description("Updated Description")
                .build();

        Recipe existingRecipe = Recipe.builder()
                .id(recipeId)
                .userId(userId)
                .title("Original Recipe")
                .description("Original Description")
                .build();

        Recipe updatedRecipe = Recipe.builder()
                .id(recipeId)
                .userId(userId)
                .title("Updated Recipe")
                .description("Updated Description")
                .build();

        RecipeResponse response = RecipeResponse.builder()
                .id(recipeId)
                .title("Updated Recipe")
                .description("Updated Description")
                .build();

        when(recipeRepository.findById(recipeId)).thenReturn(Optional.of(existingRecipe));
        when(recipeRepository.save(any(Recipe.class))).thenReturn(updatedRecipe);
        when(recipeMapper.toResponse(updatedRecipe)).thenReturn(response);

        // When
        RecipeResponse result = recipeService.updateRecipe(recipeId, request, userId);

        // Then
        assertNotNull(result);
        assertEquals("Updated Recipe", result.getTitle());
        assertEquals("Updated Description", result.getDescription());

        verify(recipeRepository).findById(recipeId);
        verify(recipeMapper).updateEntityFromRequest(existingRecipe, request);
        verify(recipeRepository).save(existingRecipe);
    }

    @Test
    void givenInvalidOwner_whenUpdateRecipe_thenThrowUnauthorizedException() {

        // Given
        UUID recipeId = UUID.randomUUID();
        UUID recipeOwnerId = UUID.randomUUID();
        UUID differentUserId = UUID.randomUUID();

        RecipeRequest request = new RecipeRequest();

        Recipe existingRecipe = Recipe.builder()
                .id(recipeId)
                .userId(recipeOwnerId) // Different from the user trying to update
                .build();

        when(recipeRepository.findById(recipeId)).thenReturn(Optional.of(existingRecipe));

        // When & Then
        assertThrows(UnauthorizedAccessException.class, () -> recipeService.updateRecipe(recipeId, request, differentUserId));

        verify(recipeRepository).findById(recipeId);
        verify(recipeRepository, never()).save(any(Recipe.class));
    }

    @Test
    void givenValidOwner_whenDeleteRecipe_thenCallDeleteRepository() {

        // Given
        UUID recipeId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        Recipe existingRecipe = Recipe.builder()
                .id(recipeId)
                .userId(userId)
                .build();

        when(recipeRepository.findById(recipeId)).thenReturn(Optional.of(existingRecipe));

        // When
        recipeService.deleteRecipe(recipeId, userId);

        // Then
        verify(recipeRepository).findById(recipeId);
        verify(recipeRepository).delete(existingRecipe);
    }

    @Test
    void givenInvalidOwner_whenDeleteRecipe_thenThrowUnauthorizedException() {

        // Given
        UUID recipeId = UUID.randomUUID();
        UUID recipeOwnerId = UUID.randomUUID();
        UUID differentUserId = UUID.randomUUID();

        RecipeRequest request = new RecipeRequest();

        Recipe existingRecipe = Recipe.builder()
                .id(recipeId)
                .userId(recipeOwnerId) // Different from the user trying to update
                .build();

        when(recipeRepository.findById(recipeId)).thenReturn(Optional.of(existingRecipe));

        // When & Then
        assertThrows(UnauthorizedAccessException.class, () -> recipeService.deleteRecipe(recipeId, differentUserId));

        verify(recipeRepository).findById(recipeId);
        verify(recipeRepository, never()).delete(any(Recipe.class));
    }


    @Test
    void givenKeyword_whenSearchRecipes_thenReturnMatchedRecipes() {

        // Given
        String keyword = "pasta";
        UUID userId = UUID.randomUUID();
        Pageable pageable = Pageable.unpaged();
        Page<RecipeResponse> expectedPage = Page.empty();

        when(recipeSearchService.searchRecipes(keyword, pageable, userId)).thenReturn(expectedPage);

        // When
        Page<RecipeResponse> result = recipeService.searchRecipes(keyword, pageable, userId);

        // Then
        assertNotNull(result);
        assertEquals(expectedPage, result);
        verify(recipeSearchService).searchRecipes(keyword, pageable, userId);
    }

    @Test
    void givenIngredients_whenGenerateMeal_thenReturnGeneratedRecipe() {

        // Given
        List<String> ingredients = List.of("potato", "cheese", "bacon");
        SimplifiedRecipeResponse expectedResponse = SimplifiedRecipeResponse.builder()
                .title("Potato and Cheese Bake")
                .build();

        when(aiService.generateRecipeFromIngredients(ingredients)).thenReturn(expectedResponse);

        // When
        SimplifiedRecipeResponse result = recipeService.generateMeal(ingredients);

        // Then
        assertNotNull(result);
        assertEquals(expectedResponse.getTitle(), result.getTitle());
        verify(aiService).generateRecipeFromIngredients(ingredients);
    }
}