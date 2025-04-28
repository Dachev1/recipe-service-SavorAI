package dev.idachev.recipeservice.service;

import dev.idachev.recipeservice.model.DifficultyLevel;
import dev.idachev.recipeservice.model.Macros;
import dev.idachev.recipeservice.model.Recipe;
import dev.idachev.recipeservice.repository.RecipeRepository;
import dev.idachev.recipeservice.web.dto.MacrosDto;
import dev.idachev.recipeservice.web.dto.RecipeResponse;
import dev.idachev.recipeservice.web.mapper.RecipeMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class RecipeSearchServiceUTest {

    @Mock
    private RecipeRepository recipeRepository;
    @Mock
    private RecipeMapper recipeMapper;
    @Mock
    private RecipeResponseEnhancer recipeResponseEnhancer;

    @InjectMocks
    private RecipeSearchService recipeSearchService;

    private UUID testUserId;
    private UUID testRecipeId;
    private Recipe testRecipe;
    private RecipeResponse testRecipeResponse; // Enhanced response
    private Pageable defaultPageable;
    private Macros testMacros;
    private MacrosDto testMacrosDto;

    @BeforeEach
    void setUp() {
        testUserId = UUID.randomUUID();
        testRecipeId = UUID.randomUUID();
        defaultPageable = PageRequest.of(0, 10, Sort.by("createdAt").descending());

        testMacrosDto = new MacrosDto(BigDecimal.valueOf(500), BigDecimal.valueOf(30), BigDecimal.valueOf(50), BigDecimal.valueOf(20));
        testMacros = Macros.builder().id(UUID.randomUUID()).calories(testMacrosDto.calories()).proteinGrams(testMacrosDto.proteinGrams()).carbsGrams(testMacrosDto.carbsGrams()).fatGrams(testMacrosDto.fatGrams()).build();

        testRecipe = Recipe.builder()
                .id(testRecipeId)
                .userId(UUID.randomUUID()) // Belongs to someone else
                .title("Searchable Test Recipe")
                .ingredients("Ingredient A;Ingredient B")
                .instructions("Some instructions")
                .imageUrl("http://example.com/search.jpg")
                .servingSuggestions("Serve hot with garnish")
                .totalTimeMinutes(40)
                .difficulty(DifficultyLevel.MEDIUM)
                .macros(testMacros)
                .tags(List.of("test", "search"))
                .isAiGenerated(false)
                .createdAt(LocalDateTime.now().minusDays(2))
                .updatedAt(LocalDateTime.now().minusHours(1))
                .upvotes(5).downvotes(1).version(1L)
                .build();

        // Represents the final enhanced response expected from the service methods
        testRecipeResponse = new RecipeResponse(
                testRecipeId, testRecipe.getUserId(), testRecipe.getTitle(),
                testRecipe.getServingSuggestions(), testRecipe.getInstructions(),
                testRecipe.getImageUrl(), List.of("Ingredient A", "Ingredient B"), // Assume mapper splits
                testRecipe.getTotalTimeMinutes(), "Author Name", "author", testRecipe.getUserId().toString(),
                testRecipe.getDifficulty(), testRecipe.getIsAiGenerated(),
                true, 10L, 5L, // isFavorite, favoriteCount, commentCount - from enhancer
                testRecipe.getUpvotes(), testRecipe.getDownvotes(), "up", // userVote - from enhancer
                testRecipe.getCreatedAt(), testRecipe.getUpdatedAt(),
                testMacrosDto, Collections.emptyMap()
        );
    }

    // Helper method to create a basic RecipeResponse before enhancement
    private RecipeResponse createBaseResponse(Recipe recipe) {
         return new RecipeResponse(
                recipe.getId(), recipe.getUserId(), recipe.getTitle(),
                recipe.getServingSuggestions(), recipe.getInstructions(),
                recipe.getImageUrl(), List.of("Ingredient A", "Ingredient B"), // Example split
                recipe.getTotalTimeMinutes(), null, null, null,
                recipe.getDifficulty(), recipe.getIsAiGenerated(),
                null, null, null, recipe.getUpvotes(), recipe.getDownvotes(), null,
                recipe.getCreatedAt(), recipe.getUpdatedAt(), testMacrosDto, null
        );
    }

    // --- Test Methods ---

    @Nested
    @DisplayName("searchRecipes Tests")
    class SearchRecipesTests {

        @Test
        @DisplayName("Should search by keyword and return enhanced results")
        void searchRecipes_WithKeyword_Success() {
            // Given
            String keyword = " Test "; // Keyword with spaces
            String trimmedKeyword = "Test"; // Expected trimmed keyword
            
            // 1. Mock repo search call
            List<Recipe> foundRecipes = List.of(testRecipe);
            Page<Recipe> recipePage = new PageImpl<>(foundRecipes, defaultPageable, 1);
            when(recipeRepository.findByTitleContainingIgnoreCaseOrServingSuggestionsContainingIgnoreCase(
                    trimmedKeyword, trimmedKeyword, defaultPageable)
            ).thenReturn(recipePage);

            // 2. Mock mapper
            RecipeResponse baseResponse = createBaseResponse(testRecipe);
            List<RecipeResponse> baseResponses = List.of(baseResponse);
            when(recipeMapper.toResponse(testRecipe)).thenReturn(baseResponse);

            // 3. Mock enhancer
            List<RecipeResponse> enhancedResponses = List.of(testRecipeResponse);
            when(recipeResponseEnhancer.enhanceRecipeListWithUserInteractions(baseResponses, testUserId))
                .thenReturn(enhancedResponses);

            Page<RecipeResponse> expectedPage = new PageImpl<>(enhancedResponses, defaultPageable, recipePage.getTotalElements());

            // When
            Page<RecipeResponse> actualPage = recipeSearchService.searchRecipes(keyword, defaultPageable, testUserId);

            // Then
            assertThat(actualPage).isEqualTo(expectedPage);
            assertThat(actualPage.getContent()).isEqualTo(enhancedResponses);

            // Verify mocks
            verify(recipeRepository).findByTitleContainingIgnoreCaseOrServingSuggestionsContainingIgnoreCase(
                trimmedKeyword, trimmedKeyword, defaultPageable);
            verify(recipeMapper).toResponse(testRecipe);
            verify(recipeResponseEnhancer).enhanceRecipeListWithUserInteractions(baseResponses, testUserId);
            verify(recipeRepository, never()).findAll(any(Pageable.class)); // Ensure findAll wasn't called
        }

        @Test
        @DisplayName("Should return all recipes if keyword is blank/null")
        void searchRecipes_BlankKeyword_ReturnsAll() {
            // Given
            String blankKeyword = "   ";
            String nullKeyword = null;
            
            // 1. Mock repo findAll call (used for both blank and null keywords)
            List<Recipe> foundRecipes = List.of(testRecipe);
            Page<Recipe> recipePage = new PageImpl<>(foundRecipes, defaultPageable, 1);
            when(recipeRepository.findAll(defaultPageable)).thenReturn(recipePage);

            // 2. Mock mapper
            RecipeResponse baseResponse = createBaseResponse(testRecipe);
            List<RecipeResponse> baseResponses = List.of(baseResponse);
            // Use lenient() because mapper might be called multiple times if test logic changes,
            // though currently it's called once per service call.
            lenient().when(recipeMapper.toResponse(testRecipe)).thenReturn(baseResponse);

            // 3. Mock enhancer
            List<RecipeResponse> enhancedResponses = List.of(testRecipeResponse);
            lenient().when(recipeResponseEnhancer.enhanceRecipeListWithUserInteractions(baseResponses, testUserId))
                .thenReturn(enhancedResponses);

            Page<RecipeResponse> expectedPage = new PageImpl<>(enhancedResponses, defaultPageable, recipePage.getTotalElements());

            // When
            Page<RecipeResponse> actualPageBlank = recipeSearchService.searchRecipes(blankKeyword, defaultPageable, testUserId);
            Page<RecipeResponse> actualPageNull = recipeSearchService.searchRecipes(nullKeyword, defaultPageable, testUserId);

            // Then
            assertThat(actualPageBlank).isEqualTo(expectedPage);
            assertThat(actualPageBlank.getContent()).isEqualTo(enhancedResponses);
            assertThat(actualPageNull).isEqualTo(expectedPage);
            assertThat(actualPageNull.getContent()).isEqualTo(enhancedResponses);

            // Verify mocks
            // findAll should be called twice (once for blank, once for null)
            verify(recipeRepository, times(2)).findAll(defaultPageable);
            // Specific search method should never be called
            verify(recipeRepository, never()).findByTitleContainingIgnoreCaseOrServingSuggestionsContainingIgnoreCase(anyString(), anyString(), any(Pageable.class));
            verify(recipeMapper, times(2)).toResponse(testRecipe); // Called once per service call
            verify(recipeResponseEnhancer, times(2)).enhanceRecipeListWithUserInteractions(baseResponses, testUserId); // Called once per service call
        }
    }

    @Nested
    @DisplayName("getAllRecipes Tests")
    class GetAllRecipesTests {

        @Test
        @DisplayName("Should get all recipes and return enhanced results")
        void getAllRecipes_Success() {
            // Given
            // 1. Mock repo findAll call
            List<Recipe> foundRecipes = List.of(testRecipe);
            Page<Recipe> recipePage = new PageImpl<>(foundRecipes, defaultPageable, 1);
            when(recipeRepository.findAll(defaultPageable)).thenReturn(recipePage);

            // 2. Mock mapper
            RecipeResponse baseResponse = createBaseResponse(testRecipe);
            List<RecipeResponse> baseResponses = List.of(baseResponse);
            when(recipeMapper.toResponse(testRecipe)).thenReturn(baseResponse);

            // 3. Mock enhancer
            List<RecipeResponse> enhancedResponses = List.of(testRecipeResponse);
            when(recipeResponseEnhancer.enhanceRecipeListWithUserInteractions(baseResponses, testUserId))
                .thenReturn(enhancedResponses);

            Page<RecipeResponse> expectedPage = new PageImpl<>(enhancedResponses, defaultPageable, recipePage.getTotalElements());

            // When
            Page<RecipeResponse> actualPage = recipeSearchService.getAllRecipes(defaultPageable, testUserId);

            // Then
            assertThat(actualPage).isEqualTo(expectedPage);
            assertThat(actualPage.getContent()).isEqualTo(enhancedResponses);

            // Verify mocks
            verify(recipeRepository).findAll(defaultPageable);
            verify(recipeMapper).toResponse(testRecipe);
            verify(recipeResponseEnhancer).enhanceRecipeListWithUserInteractions(baseResponses, testUserId);
            verify(recipeRepository, never()).findByTitleContainingIgnoreCaseOrServingSuggestionsContainingIgnoreCase(anyString(), anyString(), any(Pageable.class));
        }
    }

    @Nested
    @DisplayName("filterRecipesByTags Tests")
    class FilterRecipesByTagsTests {

        @Test
        @DisplayName("Should filter by tags and return enhanced results")
        void filterRecipesByTags_WithTags_Success() {
            // Given
            List<String> tags = List.of(" Test ", "SEARCH", " test ", "  "); // Mix of cases, spaces, duplicates, blanks
            List<String> cleanedTags = List.of("test", "search"); // Expected after cleaning
            long expectedTagCount = 2L;

            // 1. Mock repo filter call with cleaned tags and count
            List<Recipe> foundRecipes = List.of(testRecipe);
            Page<Recipe> recipePage = new PageImpl<>(foundRecipes, defaultPageable, 1);
            when(recipeRepository.findByTagsContainingAll(cleanedTags, expectedTagCount, defaultPageable))
                .thenReturn(recipePage);

            // 2. Mock mapper
            RecipeResponse baseResponse = createBaseResponse(testRecipe);
            List<RecipeResponse> baseResponses = List.of(baseResponse);
            when(recipeMapper.toResponse(testRecipe)).thenReturn(baseResponse);

            // 3. Mock enhancer
            List<RecipeResponse> enhancedResponses = List.of(testRecipeResponse);
            when(recipeResponseEnhancer.enhanceRecipeListWithUserInteractions(baseResponses, testUserId))
                .thenReturn(enhancedResponses);

            Page<RecipeResponse> expectedPage = new PageImpl<>(enhancedResponses, defaultPageable, recipePage.getTotalElements());

            // When
            Page<RecipeResponse> actualPage = recipeSearchService.filterRecipesByTags(tags, defaultPageable, testUserId);

            // Then
            assertThat(actualPage).isEqualTo(expectedPage);
            assertThat(actualPage.getContent()).isEqualTo(enhancedResponses);

            // Verify mocks
            verify(recipeRepository).findByTagsContainingAll(cleanedTags, expectedTagCount, defaultPageable);
            verify(recipeMapper).toResponse(testRecipe);
            verify(recipeResponseEnhancer).enhanceRecipeListWithUserInteractions(baseResponses, testUserId);
            verify(recipeRepository, never()).findAll(any(Pageable.class)); // Ensure findAll wasn't called
        }

        @Test
        @DisplayName("Should return all recipes if tags list is null, empty, or contains only blanks")
        void filterRecipesByTags_NoValidTags_ReturnsAll() {
            // Given
            List<String> nullTags = null;
            List<String> emptyTags = List.of();
            List<String> blankTags = List.of("  ", "\t", ""); // Include empty string too

            // 1. Mock repo findAll call (used as fallback for all invalid tag lists)
            List<Recipe> foundRecipes = List.of(testRecipe);
            Page<Recipe> recipePage = new PageImpl<>(foundRecipes, defaultPageable, 1);
            when(recipeRepository.findAll(defaultPageable)).thenReturn(recipePage);

            // 2. Mock mapper (use lenient)
            RecipeResponse baseResponse = createBaseResponse(testRecipe);
            List<RecipeResponse> baseResponses = List.of(baseResponse);
            lenient().when(recipeMapper.toResponse(testRecipe)).thenReturn(baseResponse);

            // 3. Mock enhancer (use lenient)
            List<RecipeResponse> enhancedResponses = List.of(testRecipeResponse);
            lenient().when(recipeResponseEnhancer.enhanceRecipeListWithUserInteractions(baseResponses, testUserId))
                .thenReturn(enhancedResponses);

            Page<RecipeResponse> expectedPage = new PageImpl<>(enhancedResponses, defaultPageable, recipePage.getTotalElements());

            // When
            Page<RecipeResponse> actualPageNull = recipeSearchService.filterRecipesByTags(nullTags, defaultPageable, testUserId);
            Page<RecipeResponse> actualPageEmpty = recipeSearchService.filterRecipesByTags(emptyTags, defaultPageable, testUserId);
            Page<RecipeResponse> actualPageBlank = recipeSearchService.filterRecipesByTags(blankTags, defaultPageable, testUserId);

            // Then
            assertThat(actualPageNull).isEqualTo(expectedPage);
            assertThat(actualPageEmpty).isEqualTo(expectedPage);
            assertThat(actualPageBlank).isEqualTo(expectedPage);

            // Verify mocks
            // findAll should be called 3 times (once for each invalid list)
            verify(recipeRepository, times(3)).findAll(defaultPageable);
            // Specific tag filter method should never be called
            verify(recipeRepository, never()).findByTagsContainingAll(anyList(), anyLong(), any(Pageable.class));
            verify(recipeMapper, times(3)).toResponse(testRecipe);
            verify(recipeResponseEnhancer, times(3)).enhanceRecipeListWithUserInteractions(baseResponses, testUserId);
        }
    }

    @Nested
    @DisplayName("getAllRecipesExcludingUser Tests")
    class GetAllRecipesExcludingUserTests {

        @Test
        @DisplayName("Should get recipes excluding user and return enhanced results")
        void getAllRecipesExcludingUser_Success() {
             // Given
             UUID userToExclude = UUID.randomUUID();
             
             // 1. Mock repo findByUserIdNot call
             List<Recipe> foundRecipes = List.of(testRecipe); // testRecipe is owned by someone else
             Page<Recipe> recipePage = new PageImpl<>(foundRecipes, defaultPageable, 1);
             when(recipeRepository.findByUserIdNot(userToExclude, defaultPageable)).thenReturn(recipePage);

             // 2. Mock mapper
             RecipeResponse baseResponse = createBaseResponse(testRecipe);
             List<RecipeResponse> baseResponses = List.of(baseResponse);
             when(recipeMapper.toResponse(testRecipe)).thenReturn(baseResponse);

             // 3. Mock enhancer - use any() for userId to avoid strict stubbing issues
             List<RecipeResponse> enhancedResponses = List.of(testRecipeResponse);
             when(recipeResponseEnhancer.enhanceRecipeListWithUserInteractions(eq(baseResponses), any(UUID.class)))
                 .thenReturn(enhancedResponses);

             Page<RecipeResponse> expectedPage = new PageImpl<>(enhancedResponses, defaultPageable, recipePage.getTotalElements());

             // When
             Page<RecipeResponse> actualPage = recipeSearchService.getAllRecipesExcludingUser(defaultPageable, userToExclude);

             // Then
             assertThat(actualPage).isEqualTo(expectedPage);
             assertThat(actualPage.getContent()).isEqualTo(enhancedResponses);

             // Verify mocks
             verify(recipeRepository).findByUserIdNot(userToExclude, defaultPageable);
             verify(recipeMapper).toResponse(testRecipe);
             verify(recipeResponseEnhancer).enhanceRecipeListWithUserInteractions(eq(baseResponses), any(UUID.class));
             verify(recipeRepository, never()).findAll(any(Pageable.class)); 
        }
    }
} 