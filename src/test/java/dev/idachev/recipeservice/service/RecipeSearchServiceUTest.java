package dev.idachev.recipeservice.service;

import dev.idachev.recipeservice.mapper.RecipeMapper;
import dev.idachev.recipeservice.model.Recipe;
import dev.idachev.recipeservice.repository.FavoriteRecipeRepository;
import dev.idachev.recipeservice.repository.RecipeRepository;
import dev.idachev.recipeservice.service.RecipeSearchService;
import dev.idachev.recipeservice.web.dto.RecipeResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class RecipeSearchServiceUTest {

    @Mock
    private RecipeRepository recipeRepository;

    @Mock
    private FavoriteRecipeRepository favoriteRecipeRepository;

    @Mock
    private RecipeMapper recipeMapper;

    @InjectMocks
    private RecipeSearchService recipeSearchService;

    @Test
    void givenValidKeyword_whenSearch_thenSearchResult() {

        // Given
        String keyword = "pasta";
        UUID userId = UUID.randomUUID();
        Pageable pageable = Pageable.unpaged();

        Recipe recipe1 = Recipe.builder()
                .id(UUID.randomUUID())
                .title("Pasta Carbonara")
                .build();

        Recipe recipe2 = Recipe.builder()
                .id(UUID.randomUUID())
                .title("Pasta with Tomato Sauce")
                .build();

        List<Recipe> recipes = List.of(recipe1, recipe2);
        Page<Recipe> recipePage = new PageImpl<>(recipes, pageable, recipes.size());

        RecipeResponse response1 = RecipeResponse.builder()
                .id(recipe1.getId())
                .title(recipe1.getTitle())
                .build();

        RecipeResponse response2 = RecipeResponse.builder()
                .id(recipe2.getId())
                .title(recipe2.getTitle())
                .build();

        when(recipeRepository.findByTitleContainingIgnoreCaseOrServingSuggestionsContainingIgnoreCase(keyword, keyword, pageable))
                .thenReturn(recipePage);
        when(recipeMapper.toResponse(recipe1)).thenReturn(response1);
        when(recipeMapper.toResponse(recipe2)).thenReturn(response2);
        when(favoriteRecipeRepository.countByRecipeId(any(UUID.class))).thenReturn(0L);
        when(favoriteRecipeRepository.existsByUserIdAndRecipeId(eq(userId), any(UUID.class))).thenReturn(false);

        // When
        Page<RecipeResponse> result = recipeSearchService.searchRecipes(keyword, pageable, userId);

        // Then
        assertNotNull(result);
        assertEquals(2, result.getTotalElements());

        verify(recipeRepository).findByTitleContainingIgnoreCaseOrServingSuggestionsContainingIgnoreCase(keyword, keyword, pageable);
        verify(recipeMapper, times(2)).toResponse(any(Recipe.class));
        verify(favoriteRecipeRepository, times(2)).countByRecipeId(any(UUID.class));
        verify(favoriteRecipeRepository, times(2)).existsByUserIdAndRecipeId(eq(userId), any(UUID.class));
    }

    @Test
    void givenEmptyKeyword_whenSearchRecipes_thenReturnAllRecipes() {

        // Given
        String keyword = "";
        UUID userId = UUID.randomUUID();
        Pageable pageable = Pageable.unpaged();

        Recipe recipe = Recipe.builder()
                .id(UUID.randomUUID())
                .title("Any Recipe")
                .build();

        List<Recipe> recipes = Collections.singletonList(recipe);
        Page<Recipe> recipePage = new PageImpl<>(recipes, pageable, recipes.size());

        RecipeResponse response = RecipeResponse.builder()
                .id(recipe.getId())
                .title(recipe.getTitle())
                .build();

        when(recipeRepository.findAll(pageable)).thenReturn(recipePage);
        when(recipeMapper.toResponse(recipe)).thenReturn(response);
        when(favoriteRecipeRepository.countByRecipeId(any(UUID.class))).thenReturn(0L);
        when(favoriteRecipeRepository.existsByUserIdAndRecipeId(eq(userId), any(UUID.class))).thenReturn(false);

        // When
        Page<RecipeResponse> result = recipeSearchService.searchRecipes(keyword, pageable, userId);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());

        verify(recipeRepository).findAll(pageable);
        verify(recipeRepository, never()).findByTitleContainingIgnoreCaseOrServingSuggestionsContainingIgnoreCase(anyString(), anyString(), any(Pageable.class));
    }

    @Test
    void givenNullKeyword_whenSearchRecipes_thenReturnAllRecipes() {

        // Given
        String keyword = null;
        UUID userId = UUID.randomUUID();
        Pageable pageable = Pageable.unpaged();

        Recipe recipe = Recipe.builder()
                .id(UUID.randomUUID())
                .title("Any Recipe")
                .build();

        List<Recipe> recipes = Collections.singletonList(recipe);
        Page<Recipe> recipePage = new PageImpl<>(recipes, pageable, recipes.size());

        RecipeResponse response = RecipeResponse.builder()
                .id(recipe.getId())
                .title(recipe.getTitle())
                .build();

        when(recipeRepository.findAll(pageable)).thenReturn(recipePage);
        when(recipeMapper.toResponse(recipe)).thenReturn(response);
        when(favoriteRecipeRepository.countByRecipeId(any(UUID.class))).thenReturn(0L);
        when(favoriteRecipeRepository.existsByUserIdAndRecipeId(eq(userId), any(UUID.class))).thenReturn(false);

        // When
        Page<RecipeResponse> result = recipeSearchService.searchRecipes(keyword, pageable, userId);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        verify(recipeRepository).findAll(pageable);
    }

    @Test
    void givenPagination_whenGetAllRecipes_thenReturnPagedRecipes() {

        // Given
        UUID userId = UUID.randomUUID();
        Pageable pageable = Pageable.unpaged();

        Recipe recipe1 = Recipe.builder()
                .id(UUID.randomUUID())
                .title("Recipe One")
                .build();

        Recipe recipe2 = Recipe.builder()
                .id(UUID.randomUUID())
                .title("Recipe Two")
                .build();

        List<Recipe> recipes = List.of(recipe1, recipe2);
        Page<Recipe> recipePage = new PageImpl<>(recipes, pageable, recipes.size());

        RecipeResponse response1 = RecipeResponse.builder()
                .id(recipe1.getId())
                .title(recipe1.getTitle())
                .build();

        RecipeResponse response2 = RecipeResponse.builder()
                .id(recipe2.getId())
                .title(recipe2.getTitle())
                .build();

        when(recipeRepository.findAll(pageable)).thenReturn(recipePage);
        when(recipeMapper.toResponse(recipe1)).thenReturn(response1);
        when(recipeMapper.toResponse(recipe2)).thenReturn(response2);
        when(favoriteRecipeRepository.countByRecipeId(any(UUID.class))).thenReturn(0L);

        // When
        Page<RecipeResponse> result = recipeSearchService.getAllRecipes(pageable, userId);

        // Then
        assertNotNull(result);
        assertEquals(2, result.getTotalElements());
        verify(recipeRepository).findAll(pageable);
        verify(recipeMapper, times(2)).toResponse(any(Recipe.class));
        verify(favoriteRecipeRepository, times(2)).countByRecipeId(any(UUID.class));
    }

    @Test
    void givenTags_whenFilterRecipesByTags_thenReturnFilteredRecipes() {

        // Given
        List<String> tags = List.of("tag1", "tag2");
        UUID userId = UUID.randomUUID();
        Pageable pageable = Pageable.unpaged();

        Recipe recipe = Recipe.builder()
                .id(UUID.randomUUID())
                .title("Tagged Recipe")
                .build();

        List<Recipe> recipes = Collections.singletonList(recipe);
        Page<Recipe> recipePage = new PageImpl<>(recipes, pageable, recipes.size());

        RecipeResponse response = RecipeResponse.builder()
                .id(recipe.getId())
                .title(recipe.getTitle())
                .build();

        when(recipeRepository.findAll(pageable)).thenReturn(recipePage);
        when(recipeMapper.toResponse(recipe)).thenReturn(response);
        when(favoriteRecipeRepository.countByRecipeId(any(UUID.class))).thenReturn(0L);

        // When
        Page<RecipeResponse> result = recipeSearchService.filterRecipesByTags(tags, pageable, userId);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        verify(recipeRepository).findAll(pageable);
    }

    @Test
    void givenNoTags_whenFilterRecipesByTags_thenReturnAllRecipes() {

        // Given
        List<String> tags = Collections.emptyList();
        UUID userId = UUID.randomUUID();
        Pageable pageable = Pageable.unpaged();

        Recipe recipe = Recipe.builder()
                .id(UUID.randomUUID())
                .title("Any Recipe")
                .build();

        List<Recipe> recipes = Collections.singletonList(recipe);
        Page<Recipe> recipePage = new PageImpl<>(recipes, pageable, recipes.size());

        RecipeResponse response = RecipeResponse.builder()
                .id(recipe.getId())
                .title(recipe.getTitle())
                .build();

        when(recipeRepository.findAll(pageable)).thenReturn(recipePage);
        when(recipeMapper.toResponse(recipe)).thenReturn(response);
        when(favoriteRecipeRepository.countByRecipeId(any(UUID.class))).thenReturn(0L);

        // When
        Page<RecipeResponse> result = recipeSearchService.filterRecipesByTags(tags, pageable, userId);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        verify(recipeRepository).findAll(pageable);
    }
}
