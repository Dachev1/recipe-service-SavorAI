package dev.idachev.recipeservice.service;

import dev.idachev.recipeservice.exception.AIServiceException;
import dev.idachev.recipeservice.exception.ResourceNotFoundException;
import dev.idachev.recipeservice.exception.UnauthorizedAccessException;
import dev.idachev.recipeservice.infrastructure.ai.AIService;
import dev.idachev.recipeservice.model.Recipe;
import dev.idachev.recipeservice.model.Macros;
import dev.idachev.recipeservice.model.DifficultyLevel;
import dev.idachev.recipeservice.model.RecipeVote;
import dev.idachev.recipeservice.repository.FavoriteRecipeRepository;
import dev.idachev.recipeservice.repository.RecipeRepository;
import dev.idachev.recipeservice.user.service.UserService;
import dev.idachev.recipeservice.web.dto.RecipeRequest;
import dev.idachev.recipeservice.web.dto.RecipeResponse;
import dev.idachev.recipeservice.web.dto.SimplifiedRecipeResponse;
import dev.idachev.recipeservice.web.dto.MacrosDto;
import dev.idachev.recipeservice.web.mapper.RecipeMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RecipeServiceUTest {

    @Mock
    private RecipeRepository recipeRepository;
    @Mock
    private FavoriteRecipeRepository favoriteRecipeRepository; // TODO: Mock interactions if RecipeResponseEnhancer needs it
    @Mock
    private RecipeImageService recipeImageService;
    @Mock
    private AIService aiService; // TODO: Mock interactions for generateMeal
    @Mock
    private RecipeMapper recipeMapper;
    @Mock
    private CommentService commentService; // TODO: Mock interactions if RecipeResponseEnhancer needs it
    @Mock
    private VoteService voteService; // TODO: Mock interactions if RecipeResponseEnhancer needs it
    @Mock
    private UserService userService; // TODO: Mock interactions if RecipeResponseEnhancer needs it
    @Mock
    private RecipeResponseEnhancer recipeResponseEnhancer;

    @InjectMocks
    private RecipeService recipeService;

    @Captor
    private ArgumentCaptor<Recipe> recipeCaptor;

    private UUID testUserId;
    private UUID testRecipeId;
    private Recipe testRecipe;
    private RecipeRequest testRecipeRequest;
    private RecipeResponse testRecipeResponse;
    private Macros testMacros;
    private MacrosDto testMacrosDto;

    @BeforeEach
    void setUp() {
        testUserId = UUID.randomUUID();
        testRecipeId = UUID.randomUUID();

        testMacrosDto = new MacrosDto(
                BigDecimal.valueOf(450.5),
                BigDecimal.valueOf(30.2),
                BigDecimal.valueOf(55.0),
                BigDecimal.valueOf(15.8)
        );

        testMacros = Macros.builder()
                .id(UUID.randomUUID())
                .calories(testMacrosDto.calories())
                .proteinGrams(testMacrosDto.proteinGrams())
                .carbsGrams(testMacrosDto.carbsGrams())
                .fatGrams(testMacrosDto.fatGrams())
                .build();

        testRecipeRequest = new RecipeRequest(
                "Test Title",
                "Test Suggestions",
                "Test Instructions",
                "http://example.com/request-image.jpg", // Image URL from request
                List.of("Ingredient 1", "Ingredient 2"),
                45, // totalTimeMinutes
                DifficultyLevel.MEDIUM,
                false, // isAiGenerated
                testMacrosDto
        );

        // Represents the state of the Recipe *entity* after being saved or fetched
        testRecipe = Recipe.builder()
                .id(testRecipeId)
                .userId(testUserId)
                .title("Test Title")
                .ingredients(String.join(";", testRecipeRequest.ingredients())) // Assuming mapper joins list
                .instructions("Test Instructions")
                .imageUrl("http://example.com/request-image.jpg") // Expecting this URL for no-image case
                .servingSuggestions("Test Suggestions")
                .totalTimeMinutes(45)
                .difficulty(DifficultyLevel.MEDIUM)
                .macros(testMacros)
                .tags(List.of("tag1", "tag2")) // Added tags
                .isAiGenerated(false)
                .createdAt(LocalDateTime.now().minusDays(1))
                .updatedAt(LocalDateTime.now())
                .upvotes(10) // Assuming default or existing votes
                .downvotes(2)
                .version(1L) // Added version
                .build();

        // Represents the final *expected* response DTO AFTER the service method (including enhancement) runs
        testRecipeResponse = new RecipeResponse(
                testRecipeId,
                testUserId, // createdById
                "Test Title",
                "Test Suggestions",
                "Test Instructions",
                "http://example.com/request-image.jpg", // Final image URL
                testRecipeRequest.ingredients(), // Expect list here
                45, // totalTimeMinutes
                "Test Author Name", // authorName - Set during enhancement
                "testuser", // username - Set during enhancement
                testUserId.toString(), // authorId - Set during enhancement
                DifficultyLevel.MEDIUM,
                false, // isAiGenerated
                true, // isFavorite - Set during enhancement
                5L, // favoriteCount - Not directly set in single enhancement, usually from list enhancer or separate endpoint
                3L, // commentCount - Set during enhancement
                10, // upvotes
                2, // downvotes
                "up", // userVote - Set during enhancement
                testRecipe.getCreatedAt(),
                testRecipe.getUpdatedAt(),
                testMacrosDto,
                Collections.emptyMap()
        );
    }

    // --- Test Structure ---
    // Nested classes for each method being tested

    @Nested
    @DisplayName("createRecipe Tests")
    class CreateRecipeTests {

        @Test
        @DisplayName("Should create recipe successfully without image")
        void createRecipe_Success_NoImage() {
            // Given
            // TODO: Mock mapper, repo.save, enhancer

            // When
            // TODO: Call service.createRecipe(request, userId)

            // Then
            // TODO: Verify interactions, assert response
        }

        @Test
        @DisplayName("Should create recipe successfully with image")
        void createRecipe_Success_WithImage() {
            // Given
            // TODO: Mock image file, imageService, mapper, repo.save, enhancer

            // When
            // TODO: Call service.createRecipe(request, image, userId)

            // Then
            // TODO: Verify interactions, assert response (check image URL)
        }

         @Test
        @DisplayName("Should create recipe using request image URL when image processing fails")
        void createRecipe_ImageProcessingFails_UsesRequestUrl() {
            // Given
            // TODO: Mock image file, imageService (returns null/empty), mapper, repo.save, enhancer

            // When
            // TODO: Call service.createRecipe(request, image, userId)

            // Then
            // TODO: Verify interactions, assert response (check image URL matches request)
        }
    }

    @Nested
    @DisplayName("getRecipeById Tests")
    class GetRecipeByIdTests {

        @Test
        @DisplayName("Should return recipe when found")
        void getRecipeById_Found_ReturnsRecipe() {
            // Given
            // TODO: Mock repo.findById, mapper, enhancer

            // When
            // TODO: Call service.getRecipeById(id, userId)

            // Then
            // TODO: Verify interactions, assert response
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException when not found")
        void getRecipeById_NotFound_ThrowsResourceNotFoundException() {
            // Given
             // TODO: Mock repo.findById to return empty Optional

            // When / Then
            // TODO: Assert ResourceNotFoundException is thrown
        }
    }

    @Nested
    @DisplayName("getRecipesByUserId Tests")
    class GetRecipesByUserIdTests {

        @Test
        @DisplayName("Should return list of recipes for user")
        void getRecipesByUserId_Success_ReturnsRecipes() {
            // Given
            // TODO: Mock repo.findByUserId, mapper, enhancer

            // When
            // TODO: Call service.getRecipesByUserId(userId)

            // Then
            // TODO: Verify interactions, assert response list
        }

        @Test
        @DisplayName("Should return empty list when user has no recipes")
        void getRecipesByUserId_NoRecipes_ReturnsEmptyList() {
           // Given
           // TODO: Mock repo.findByUserId returns empty list, enhancer

           // When
           // TODO: Call service.getRecipesByUserId(userId)

           // Then
           // TODO: Verify interactions, assert response list is empty
        }
    }

     @Nested
    @DisplayName("getRecipeFeed Tests")
    class GetRecipeFeedTests {

        @Test
        @DisplayName("Should return paginated recipe feed sorted by creation date")
        void getRecipeFeed_Success_ReturnsPaginatedSortedFeed() {
            // Given
            Pageable pageable = PageRequest.of(0, 10, Sort.Direction.DESC, "createdAt");
            // TODO: Mock repo.findAll(pageable), mapper, enhancer

            // When
            // TODO: Call service.getRecipeFeed(userId, pageable)

            // Then
            // TODO: Verify interactions, assert response Page content and properties
        }
    }


    @Nested
    @DisplayName("updateRecipe Tests")
    class UpdateRecipeTests {

        private RecipeRequest updateRequest;
        private Recipe existingRecipe;

        @BeforeEach
        void updateSetUp() {
            // A different request for the update
            updateRequest = new RecipeRequest(
                    "Updated Title", "Updated Suggestions", "Updated Instructions",
                    "http://example.com/updated-request-image.jpg",
                    List.of("Updated Ingredient"), 60,
                    DifficultyLevel.HARD, true, // isAiGenerated = true
                    new MacrosDto(BigDecimal.valueOf(600), BigDecimal.valueOf(40), BigDecimal.valueOf(70), BigDecimal.valueOf(20))
            );

            // Represent the recipe *before* the update
            existingRecipe = testRecipe; // Use the base recipe from main setUp
        }

        @Test
        @DisplayName("Should update recipe successfully without new image")
        void updateRecipe_Success_NoImage() {
            // Given
            // TODO: Mock checkRecipePermission (repo.findById), mapper, repo.save, enhancer

            // When
            // TODO: Call service.updateRecipe(id, request, userId)

            // Then
            // TODO: Verify interactions (repo.save with updated fields), assert response
        }

        @Test
        @DisplayName("Should update recipe successfully with new image")
        void updateRecipe_Success_WithImage() {
             // Given
            // TODO: Mock image file, checkRecipePermission, imageService, mapper, repo.save, enhancer

            // When
            // TODO: Call service.updateRecipe(id, request, image, userId)

            // Then
            // TODO: Verify interactions (repo.save with updated fields, new image URL), assert response
        }

         @Test
        @DisplayName("Should update recipe keeping request image URL when image processing fails")
        void updateRecipe_ImageProcessingFails_UsesRequestUrl() {
            // Given
            // TODO: Mock image file, checkRecipePermission, imageService (returns null/empty), mapper, repo.save, enhancer

            // When
            // TODO: Call service.updateRecipe(id, request, image, userId)

            // Then
            // TODO: Verify interactions (repo.save with updated fields, request image URL), assert response
        }


        @Test
        @DisplayName("Should throw ResourceNotFoundException when recipe to update not found")
        void updateRecipe_NotFound_ThrowsResourceNotFoundException() {
            // Given
            UUID nonExistentId = UUID.randomUUID();
            MultipartFile mockImage = mock(MultipartFile.class);
            // Mock repo.findById to return empty during permission check
            when(recipeRepository.findById(nonExistentId)).thenReturn(Optional.empty());

            // When / Then
            // Check both versions of the update method (with and without image)
            assertThatThrownBy(() -> recipeService.updateRecipe(nonExistentId, updateRequest, testUserId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Recipe not found with id: " + nonExistentId);
            
            assertThatThrownBy(() -> recipeService.updateRecipe(nonExistentId, updateRequest, mockImage, testUserId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Recipe not found with id: " + nonExistentId);

            // Verify mocks
            verify(recipeRepository, times(2)).findById(nonExistentId);
            verifyNoInteractions(recipeMapper, recipeImageService, favoriteRecipeRepository, voteService, commentService, userService, recipeResponseEnhancer);
            verify(recipeRepository, never()).save(any()); // Ensure save is never called
        }

        @Test
        @DisplayName("Should throw UnauthorizedAccessException when user is not owner")
        void updateRecipe_NotOwner_ThrowsUnauthorizedAccessException() {
            // Given
            UUID differentUserId = UUID.randomUUID();
            MultipartFile mockImage = mock(MultipartFile.class);
            // Ensure the existing recipe used for the check is owned by the original testUserId
            Recipe recipeOwnedBySomeoneElse = existingRecipe;
            when(recipeRepository.findById(testRecipeId)).thenReturn(Optional.of(recipeOwnedBySomeoneElse));

            // When / Then
            // Use differentUserId as the user attempting the update
            assertThatThrownBy(() -> recipeService.updateRecipe(testRecipeId, updateRequest, differentUserId))
                .isInstanceOf(UnauthorizedAccessException.class)
                .hasMessageContaining("User does not have permission to update recipe");
            
            // Also test the version with image upload
            assertThatThrownBy(() -> recipeService.updateRecipe(testRecipeId, updateRequest, mockImage, differentUserId))
                .isInstanceOf(UnauthorizedAccessException.class)
                .hasMessageContaining("User does not have permission to update recipe");

             // Verify mocks
             verify(recipeRepository, times(2)).findById(testRecipeId);
             verifyNoInteractions(recipeMapper, recipeImageService, favoriteRecipeRepository, voteService, commentService, userService, recipeResponseEnhancer);
             verify(recipeRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("deleteRecipe Tests")
    class DeleteRecipeTests {
        
        private Recipe existingRecipe;

        @BeforeEach
        void deleteSetUp() {
            // Recipe exists and belongs to testUserId
            existingRecipe = testRecipe; 
        }

        @Test
        @DisplayName("Should delete recipe successfully")
        void deleteRecipe_Success() {
            // Given
            // 1. Permission Check: repo finds the recipe owned by the user
            when(recipeRepository.findById(testRecipeId)).thenReturn(Optional.of(existingRecipe));

            // 2. Mocks for deletion (void methods)
            doNothing().when(recipeRepository).deleteById(testRecipeId);
            // Assuming favorite cleanup is called:
            doNothing().when(favoriteRecipeRepository).deleteByRecipeId(testRecipeId);
            // RecipeImageService has no delete method, so no mock needed here

            // When
            recipeService.deleteRecipe(testRecipeId, testUserId);

            // Then
            // Verify mocks
            verify(recipeRepository).findById(testRecipeId); // Permission check
            verify(recipeRepository).deleteById(testRecipeId);
            verify(favoriteRecipeRepository).deleteByRecipeId(testRecipeId); // Verify cleanup
            verifyNoInteractions(recipeImageService); // No interaction expected
            verifyNoInteractions(recipeMapper, voteService, commentService, userService, recipeResponseEnhancer); 
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException when recipe to delete not found")
        void deleteRecipe_NotFound_ThrowsResourceNotFoundException() {
             // Given
             UUID nonExistentId = UUID.randomUUID();
             // Mock repo.findById to return empty during permission check
             when(recipeRepository.findById(nonExistentId)).thenReturn(Optional.empty());

             // When / Then
             assertThatThrownBy(() -> recipeService.deleteRecipe(nonExistentId, testUserId))
                 .isInstanceOf(ResourceNotFoundException.class)
                 .hasMessageContaining("Recipe not found with id: " + nonExistentId);

             // Verify mocks
             verify(recipeRepository).findById(nonExistentId);
             verify(recipeRepository, never()).deleteById(any());
             verify(favoriteRecipeRepository, never()).deleteByRecipeId(any());
             verifyNoInteractions(recipeImageService);
             verifyNoInteractions(recipeMapper, voteService, commentService, userService, recipeResponseEnhancer);
        }

        @Test
        @DisplayName("Should throw UnauthorizedAccessException when user is not owner")
        void deleteRecipe_NotOwner_ThrowsUnauthorizedAccessException() {
            // Given
            UUID differentUserId = UUID.randomUUID();
            // Recipe exists but is owned by testUserId, not differentUserId
            Recipe recipeOwnedBySomeoneElse = existingRecipe;
            when(recipeRepository.findById(testRecipeId)).thenReturn(Optional.of(recipeOwnedBySomeoneElse));

            // When / Then
            // Attempt deletion using differentUserId
            assertThatThrownBy(() -> recipeService.deleteRecipe(testRecipeId, differentUserId))
                .isInstanceOf(UnauthorizedAccessException.class)
                .hasMessageContaining("User does not have permission to delete recipe");

             // Verify mocks
             verify(recipeRepository).findById(testRecipeId);
             verify(recipeRepository, never()).deleteById(any());
             verify(favoriteRecipeRepository, never()).deleteByRecipeId(any());
             verifyNoInteractions(recipeImageService);
             verifyNoInteractions(recipeMapper, voteService, commentService, userService, recipeResponseEnhancer);
        }
    }

    @Nested
    @DisplayName("generateMeal Tests")
    class GenerateMealTests {

        @Test
        @DisplayName("Should generate meal successfully using AI service")
        void generateMeal_Success() {
            // Given
            List<String> ingredients = List.of("chicken", "broccoli");
            // SimplifiedRecipeResponse(title, description, instructions, ingredients, imageUrl, totalTimeMinutes, macros, difficulty, servingSuggestions)
            SimplifiedRecipeResponse expectedResponse = new SimplifiedRecipeResponse(
                    "AI Chicken Broccoli", // title
                    "A delicious AI generated meal", // description
                    "1. Cook chicken. 2. Add broccoli.", // instructions
                    List.of("chicken", "broccoli", "ai spice"), // ingredients
                    "http://example.com/ai-image.jpg", // imageUrl
                    35, // totalTimeMinutes
                    new MacrosDto(BigDecimal.valueOf(500), BigDecimal.valueOf(40), BigDecimal.valueOf(30), BigDecimal.valueOf(25)), // macros
                    DifficultyLevel.EASY, // difficulty
                    "Serve hot" // servingSuggestions
            );

            // Mock AI service to return the expected response
            when(aiService.generateRecipeFromIngredients(ingredients)).thenReturn(expectedResponse);

            // When
            SimplifiedRecipeResponse actualResponse = recipeService.generateMeal(ingredients);

            // Then
            assertThat(actualResponse).isEqualTo(expectedResponse);

            // Verify mocks
            verify(aiService).generateRecipeFromIngredients(ingredients);
            verifyNoInteractions(recipeRepository, favoriteRecipeRepository, recipeImageService, recipeMapper, commentService, voteService, userService, recipeResponseEnhancer);
        }

         @Test
        @DisplayName("Should handle AI service exception")
        void generateMeal_AiServiceError_ThrowsException() {
            // Given
            List<String> ingredients = List.of("chicken", "broccoli");
            String errorMessage = "AI service unavailable";
            // Mock AI service to throw an exception
            when(aiService.generateRecipeFromIngredients(ingredients)).thenThrow(new AIServiceException(errorMessage));

            // When / Then
            assertThatThrownBy(() -> recipeService.generateMeal(ingredients))
                .isInstanceOf(AIServiceException.class)
                .hasMessageContaining(errorMessage);
            
            // Verify mocks
            verify(aiService).generateRecipeFromIngredients(ingredients);
            verifyNoInteractions(recipeRepository, favoriteRecipeRepository, recipeImageService, recipeMapper, commentService, voteService, userService, recipeResponseEnhancer);
        }
    }

    // TODO: Add tests for enhanceWithUserInteractions if direct testing is needed
    // TODO: Add tests covering edge cases for image handling (e.g., empty image URL in request, different scenarios of processing failure)
    // TODO: Consider testing transactionality if specific rollback scenarios are critical (more complex, might need integration tests)

} 