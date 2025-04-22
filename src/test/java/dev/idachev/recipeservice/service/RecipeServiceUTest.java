package dev.idachev.recipeservice.service;

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
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;
import org.springframework.web.multipart.MultipartFile;

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
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;

import java.math.BigDecimal;

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
            // 1. Mapper converts request DTO to an initial entity (no ID, etc.)
            Recipe mappedRecipe = Recipe.builder()
                    .title(testRecipeRequest.title())
                    .ingredients(String.join(";", testRecipeRequest.ingredients())) // Example mapping
                    .instructions(testRecipeRequest.instructions())
                    .servingSuggestions(testRecipeRequest.servingSuggestions())
                    .imageUrl(testRecipeRequest.imageUrl()) // Comes from request
                    .totalTimeMinutes(testRecipeRequest.totalTimeMinutes())
                    .difficulty(testRecipeRequest.difficulty())
                    .macros(testMacros) // Assume mapper handles nested DTO -> Entity
                    .isAiGenerated(testRecipeRequest.isAiGenerated())
                    // Defaults set by Lombok/JPA usually handled AFTER save or by builder defaults
                    .upvotes(0)
                    .downvotes(0)
                    .build();

            // 2. Repository saves the entity, adding ID, timestamps, etc.
            Recipe savedRecipe = testRecipe; // Use the one from setUp with ID, votes etc.

            // 3. Mapper converts saved entity back to a basic response DTO
            // This response LACKS the user-specific fields (isFavorite, userVote, commentCount, author...)
            RecipeResponse initialResponseBeforeEnhancement = new RecipeResponse(
                savedRecipe.getId(),
                savedRecipe.getUserId(),
                savedRecipe.getTitle(),
                savedRecipe.getServingSuggestions(),
                savedRecipe.getInstructions(),
                savedRecipe.getImageUrl(),
                testRecipeRequest.ingredients(),
                savedRecipe.getTotalTimeMinutes(),
                null, null, null, // authorName, username, authorId - added later
                savedRecipe.getDifficulty(),
                savedRecipe.getIsAiGenerated(),
                null, // isFavorite - added later
                null, // favoriteCount - not added by single enhancement
                null, // commentCount - added later
                savedRecipe.getUpvotes(),
                savedRecipe.getDownvotes(),
                null, // userVote - added later
                savedRecipe.getCreatedAt(),
                savedRecipe.getUpdatedAt(),
                testMacrosDto,
                null
            );

            // 4. Mocks for the *internal* enhancement logic within RecipeService.enhanceWithUserInteractions
            boolean expectedIsFavorite = true;
            RecipeVote.VoteType expectedVoteType = RecipeVote.VoteType.UPVOTE;
            long expectedCommentCount = 3L;
            // Mock user service for author name/details (Assuming it's called by enhance method)
            // TODO: If UserService isn't actually called by enhance, remove these mocks
            String expectedAuthorName = "Test Author Name";
            String expectedUsername = "testuser";

            when(recipeMapper.toEntity(testRecipeRequest)).thenReturn(mappedRecipe);
            when(recipeRepository.save(recipeCaptor.capture())).thenReturn(savedRecipe);
            when(recipeMapper.toResponse(savedRecipe)).thenReturn(initialResponseBeforeEnhancement);

            // Mock calls made by RecipeService.enhanceWithUserInteractions
            when(favoriteRecipeRepository.existsByUserIdAndRecipeId(testRecipeId, testUserId)).thenReturn(expectedIsFavorite);
            when(voteService.getUserVote(testRecipeId, testUserId)).thenReturn(expectedVoteType);
            when(commentService.getCommentCount(testRecipeId)).thenReturn(expectedCommentCount);
            // Mock user service calls (if any) for author details
            // when(userService.getUserDetailsById(testUserId)).thenReturn(someUserDetailsDto); // Adjust based on actual UserService method

            // When
            RecipeResponse actualResponse = recipeService.createRecipe(testRecipeRequest, testUserId);

            // Then
            // Assert the final response matches the fully populated one defined in setUp
            assertThat(actualResponse).isEqualTo(testRecipeResponse);

            // Verify mocks
            verify(recipeMapper).toEntity(testRecipeRequest);
            verify(recipeRepository).save(any(Recipe.class));
            verify(recipeMapper).toResponse(savedRecipe);
            // Verify enhancement mocks
            verify(favoriteRecipeRepository).existsByUserIdAndRecipeId(testRecipeId, testUserId);
            verify(voteService).getUserVote(testRecipeId, testUserId);
            verify(commentService).getCommentCount(testRecipeId);
            // verify(userService).getUserDetailsById(testUserId); // Verify user service call if mocked
            verifyNoInteractions(recipeImageService); // No image processing expected
            verifyNoInteractions(recipeResponseEnhancer); // Enhancer component not used here

            // Verify the entity passed to save()
            Recipe recipeToSave = recipeCaptor.getValue();
            assertThat(recipeToSave.getUserId()).isEqualTo(testUserId);
            assertThat(recipeToSave.getImageUrl()).isEqualTo(testRecipeRequest.imageUrl());
            assertThat(recipeToSave.getId()).isNull();
            assertThat(recipeToSave.getCreatedAt()).isNull();
        }

        @Test
        @DisplayName("Should create recipe successfully with image")
        void createRecipe_Success_WithImage() {
            // Given
            MultipartFile mockImage = mock(MultipartFile.class);
            String processedImageUrl = "http://example.com/processed-image.jpg";

            // --- Mocks (Similar setup as NoImage test, but with imageService and different URL) ---

            // 1. Mapper converts request DTO to initial entity
            Recipe mappedRecipe = Recipe.builder()
                    .title(testRecipeRequest.title())
                    .ingredients(String.join(";", testRecipeRequest.ingredients()))
                    .instructions(testRecipeRequest.instructions())
                    .servingSuggestions(testRecipeRequest.servingSuggestions())
                    .imageUrl(testRecipeRequest.imageUrl()) // Still has original URL from request
                    .totalTimeMinutes(testRecipeRequest.totalTimeMinutes())
                    .difficulty(testRecipeRequest.difficulty())
                    .macros(testMacros)
                    .isAiGenerated(testRecipeRequest.isAiGenerated())
                    .upvotes(0)
                    .downvotes(0)
                    .build();

            // 2. Image Service processes the image
            when(mockImage.isEmpty()).thenReturn(false);
            when(recipeImageService.processRecipeImage(
                    testRecipeRequest.title(),
                    testRecipeRequest.servingSuggestions(),
                    mockImage
            )).thenReturn(processedImageUrl);

            // 3. Repository saves the entity (now with processedImageUrl)
            Recipe savedRecipe = testRecipe.toBuilder()
                                        .imageUrl(processedImageUrl) // Expect saved entity to have processed URL
                                        .build();

            // 4. Mapper converts saved entity back to a basic response DTO
            RecipeResponse initialResponseBeforeEnhancement = new RecipeResponse(
                savedRecipe.getId(), savedRecipe.getUserId(), savedRecipe.getTitle(),
                savedRecipe.getServingSuggestions(), savedRecipe.getInstructions(),
                savedRecipe.getImageUrl(), // Use processed URL
                testRecipeRequest.ingredients(), savedRecipe.getTotalTimeMinutes(),
                null, null, null, savedRecipe.getDifficulty(), savedRecipe.getIsAiGenerated(),
                null, null, null, savedRecipe.getUpvotes(), savedRecipe.getDownvotes(), null,
                savedRecipe.getCreatedAt(), savedRecipe.getUpdatedAt(), testMacrosDto, null
            );

            // 5. Mocks for enhancement logic
            boolean expectedIsFavorite = false; // Example: Not favorited
            RecipeVote.VoteType expectedVoteType = null; // Example: No vote
            long expectedCommentCount = 0L; // Example: No comments

            // 6. Final expected response after enhancement
            // Records don't have toBuilder(), create a new instance manually
            RecipeResponse finalEnhancedResponse = new RecipeResponse(
                    savedRecipe.getId(),
                    savedRecipe.getUserId(), // createdById
                    savedRecipe.getTitle(),
                    savedRecipe.getServingSuggestions(),
                    savedRecipe.getInstructions(),
                    processedImageUrl, // Use processed URL
                    testRecipeRequest.ingredients(),
                    savedRecipe.getTotalTimeMinutes(),
                    testRecipeResponse.authorName(), // Keep other enhanced fields from setUp for consistency if not mocked differently
                    testRecipeResponse.username(),
                    testRecipeResponse.authorId(),
                    savedRecipe.getDifficulty(),
                    savedRecipe.getIsAiGenerated(),
                    expectedIsFavorite, // Use the mocked value
                    testRecipeResponse.favoriteCount(), // Keep original unless mocked differently
                    expectedCommentCount, // Use the mocked value
                    savedRecipe.getUpvotes(),
                    savedRecipe.getDownvotes(),
                    null, // Use null as expectedVoteType is null in this test's mocks
                    savedRecipe.getCreatedAt(),
                    savedRecipe.getUpdatedAt(),
                    testMacrosDto,
                    testRecipeResponse.additionalFields()
            );

            // --- Setup Mock Interactions ---
            when(recipeMapper.toEntity(testRecipeRequest)).thenReturn(mappedRecipe);
            when(recipeRepository.save(recipeCaptor.capture())).thenReturn(savedRecipe);
            when(recipeMapper.toResponse(savedRecipe)).thenReturn(initialResponseBeforeEnhancement);

            // Mock enhancement calls
            when(favoriteRecipeRepository.existsByUserIdAndRecipeId(savedRecipe.getId(), testUserId)).thenReturn(expectedIsFavorite);
            when(voteService.getUserVote(savedRecipe.getId(), testUserId)).thenReturn(expectedVoteType);
            when(commentService.getCommentCount(savedRecipe.getId())).thenReturn(expectedCommentCount);
            // Mock userService if needed for author details

            // When
            RecipeResponse actualResponse = recipeService.createRecipe(testRecipeRequest, mockImage, testUserId);

            // Then
            assertThat(actualResponse).isEqualTo(finalEnhancedResponse);

            // Verify mocks
            verify(recipeMapper).toEntity(testRecipeRequest);
            verify(recipeImageService).processRecipeImage(testRecipeRequest.title(), testRecipeRequest.servingSuggestions(), mockImage);
            verify(recipeRepository).save(any(Recipe.class));
            verify(recipeMapper).toResponse(savedRecipe);
            // Verify enhancement mocks
            verify(favoriteRecipeRepository).existsByUserIdAndRecipeId(savedRecipe.getId(), testUserId);
            verify(voteService).getUserVote(savedRecipe.getId(), testUserId);
            verify(commentService).getCommentCount(savedRecipe.getId());
            // Verify userService if mocked
            verifyNoInteractions(recipeResponseEnhancer);

            // Verify the entity passed to save() has correct userId and the *processed* image URL
            Recipe recipeToSave = recipeCaptor.getValue();
            assertThat(recipeToSave.getUserId()).isEqualTo(testUserId);
            assertThat(recipeToSave.getImageUrl()).isEqualTo(processedImageUrl); // Crucial check
            assertThat(recipeToSave.getId()).isNull();
        }

         @Test
        @DisplayName("Should create recipe using request image URL when image processing fails")
        void createRecipe_ImageProcessingFails_UsesRequestUrl() {
            // Given
            MultipartFile mockImage = mock(MultipartFile.class);
            String requestImageUrl = testRecipeRequest.imageUrl(); // URL from the DTO

            // --- Mocks ---
            // 1. Mapper converts DTO to entity
            Recipe mappedRecipe = Recipe.builder()
                    .title(testRecipeRequest.title())
                    .ingredients(String.join(";", testRecipeRequest.ingredients()))
                    .instructions(testRecipeRequest.instructions())
                    .servingSuggestions(testRecipeRequest.servingSuggestions())
                    .imageUrl(requestImageUrl)
                    .totalTimeMinutes(testRecipeRequest.totalTimeMinutes())
                    .difficulty(testRecipeRequest.difficulty())
                    .macros(testMacros)
                    .isAiGenerated(testRecipeRequest.isAiGenerated())
                    .upvotes(0).downvotes(0)
                    .build();

            // 2. Image Service FAILS to process (returns null)
            when(mockImage.isEmpty()).thenReturn(false);
            when(recipeImageService.processRecipeImage(
                    testRecipeRequest.title(),
                    testRecipeRequest.servingSuggestions(),
                    mockImage
            )).thenReturn(null); // Simulate processing failure

            // 3. Repository saves the entity (should use requestImageUrl)
            // Use the testRecipe from setUp as the base for the saved state,
            // ensuring its imageUrl matches the request URL
            Recipe savedRecipe = testRecipe.toBuilder()
                                        .imageUrl(requestImageUrl)
                                        .build();

            // 4. Mapper converts saved entity to basic response
            RecipeResponse initialResponseBeforeEnhancement = new RecipeResponse(
                savedRecipe.getId(), savedRecipe.getUserId(), savedRecipe.getTitle(),
                savedRecipe.getServingSuggestions(), savedRecipe.getInstructions(),
                savedRecipe.getImageUrl(), // Should be requestImageUrl
                testRecipeRequest.ingredients(), savedRecipe.getTotalTimeMinutes(),
                null, null, null, savedRecipe.getDifficulty(), savedRecipe.getIsAiGenerated(),
                null, null, null, savedRecipe.getUpvotes(), savedRecipe.getDownvotes(), null,
                savedRecipe.getCreatedAt(), savedRecipe.getUpdatedAt(), testMacrosDto, null
            );

            // 5. Mocks for enhancement
            boolean expectedIsFavorite = true;
            RecipeVote.VoteType expectedVoteType = RecipeVote.VoteType.DOWNVOTE;
            long expectedCommentCount = 1L;

            // 6. Final expected response
            RecipeResponse finalEnhancedResponse = new RecipeResponse(
                    savedRecipe.getId(), savedRecipe.getUserId(), savedRecipe.getTitle(),
                    savedRecipe.getServingSuggestions(), savedRecipe.getInstructions(),
                    requestImageUrl, // Expect request URL in final response
                    testRecipeRequest.ingredients(), savedRecipe.getTotalTimeMinutes(),
                    testRecipeResponse.authorName(), testRecipeResponse.username(), testRecipeResponse.authorId(),
                    savedRecipe.getDifficulty(), savedRecipe.getIsAiGenerated(),
                    expectedIsFavorite, testRecipeResponse.favoriteCount(), expectedCommentCount,
                    savedRecipe.getUpvotes(), savedRecipe.getDownvotes(),
                    expectedVoteType.name().toLowerCase(), // Map enum to string
                    savedRecipe.getCreatedAt(), savedRecipe.getUpdatedAt(), testMacrosDto, testRecipeResponse.additionalFields()
            );

            // --- Setup Mock Interactions ---
            when(recipeMapper.toEntity(testRecipeRequest)).thenReturn(mappedRecipe);
            when(recipeRepository.save(recipeCaptor.capture())).thenReturn(savedRecipe);
            when(recipeMapper.toResponse(savedRecipe)).thenReturn(initialResponseBeforeEnhancement);

            // Mock enhancement calls
            when(favoriteRecipeRepository.existsByUserIdAndRecipeId(savedRecipe.getId(), testUserId)).thenReturn(expectedIsFavorite);
            when(voteService.getUserVote(savedRecipe.getId(), testUserId)).thenReturn(expectedVoteType);
            when(commentService.getCommentCount(savedRecipe.getId())).thenReturn(expectedCommentCount);

            // When
            RecipeResponse actualResponse = recipeService.createRecipe(testRecipeRequest, mockImage, testUserId);

            // Then
            assertThat(actualResponse).isEqualTo(finalEnhancedResponse);

            // Verify mocks
            verify(recipeMapper).toEntity(testRecipeRequest);
            verify(recipeImageService).processRecipeImage(testRecipeRequest.title(), testRecipeRequest.servingSuggestions(), mockImage);
            verify(recipeRepository).save(any(Recipe.class));
            verify(recipeMapper).toResponse(savedRecipe);
            // Verify enhancement mocks
            verify(favoriteRecipeRepository).existsByUserIdAndRecipeId(savedRecipe.getId(), testUserId);
            verify(voteService).getUserVote(savedRecipe.getId(), testUserId);
            verify(commentService).getCommentCount(savedRecipe.getId());
            verifyNoInteractions(recipeResponseEnhancer);

            // Verify the entity passed to save() used the request image URL
            Recipe recipeToSave = recipeCaptor.getValue();
            assertThat(recipeToSave.getUserId()).isEqualTo(testUserId);
            assertThat(recipeToSave.getImageUrl()).isEqualTo(requestImageUrl); // Check fallback URL
            assertThat(recipeToSave.getId()).isNull();
        }
    }

    @Nested
    @DisplayName("getRecipeById Tests")
    class GetRecipeByIdTests {

        @Test
        @DisplayName("Should return recipe when found")
        void getRecipeById_Found_ReturnsRecipe() {
            // Given
            // 1. Repository finds the recipe
            Recipe foundRecipe = testRecipe; // Use the one from setUp
            when(recipeRepository.findById(testRecipeId)).thenReturn(Optional.of(foundRecipe));

            // 2. Mapper converts entity to basic response
            RecipeResponse initialResponseBeforeEnhancement = new RecipeResponse(
                foundRecipe.getId(), foundRecipe.getUserId(), foundRecipe.getTitle(),
                foundRecipe.getServingSuggestions(), foundRecipe.getInstructions(),
                foundRecipe.getImageUrl(), testRecipeRequest.ingredients(), // Assume mapper handles string -> list
                foundRecipe.getTotalTimeMinutes(), null, null, null,
                foundRecipe.getDifficulty(), foundRecipe.getIsAiGenerated(),
                null, null, null, foundRecipe.getUpvotes(), foundRecipe.getDownvotes(), null,
                foundRecipe.getCreatedAt(), foundRecipe.getUpdatedAt(), testMacrosDto, null
            );
            when(recipeMapper.toResponse(foundRecipe)).thenReturn(initialResponseBeforeEnhancement);

            // 3. Mocks for enhancement
            boolean expectedIsFavorite = true;
            RecipeVote.VoteType expectedVoteType = RecipeVote.VoteType.UPVOTE;
            long expectedCommentCount = 3L;

            when(favoriteRecipeRepository.existsByUserIdAndRecipeId(testRecipeId, testUserId)).thenReturn(expectedIsFavorite);
            when(voteService.getUserVote(testRecipeId, testUserId)).thenReturn(expectedVoteType);
            when(commentService.getCommentCount(testRecipeId)).thenReturn(expectedCommentCount);
            // Mock userService if needed

            // 4. Final expected response (matches the one from setUp)
            RecipeResponse expectedFinalResponse = testRecipeResponse;

            // When
            RecipeResponse actualResponse = recipeService.getRecipeById(testRecipeId, testUserId);

            // Then
            assertThat(actualResponse).isEqualTo(expectedFinalResponse);

            // Verify mocks
            verify(recipeRepository).findById(testRecipeId);
            verify(recipeMapper).toResponse(foundRecipe);
            // Verify enhancement mocks
            verify(favoriteRecipeRepository).existsByUserIdAndRecipeId(testRecipeId, testUserId);
            verify(voteService).getUserVote(testRecipeId, testUserId);
            verify(commentService).getCommentCount(testRecipeId);
            // Verify userService if mocked
            verifyNoInteractions(recipeResponseEnhancer);
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException when not found")
        void getRecipeById_NotFound_ThrowsResourceNotFoundException() {
            // Given
            UUID nonExistentId = UUID.randomUUID();
            when(recipeRepository.findById(nonExistentId)).thenReturn(Optional.empty());

            // When / Then
            assertThatThrownBy(() -> recipeService.getRecipeById(nonExistentId, testUserId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Recipe not found with id: " + nonExistentId);

            // Verify mocks
            verify(recipeRepository).findById(nonExistentId);
            verifyNoInteractions(recipeMapper, favoriteRecipeRepository, voteService, commentService, userService, recipeResponseEnhancer);
        }
    }

    @Nested
    @DisplayName("getRecipesByUserId Tests")
    class GetRecipesByUserIdTests {

        @Test
        @DisplayName("Should return list of recipes for user")
        void getRecipesByUserId_Success_ReturnsRecipes() {
            // Given
            // 1. Repository finds recipes by user ID
            List<Recipe> foundRecipes = List.of(testRecipe);
            when(recipeRepository.findByUserId(testUserId)).thenReturn(foundRecipes);

            // 2. Mapper converts entities to basic response DTOs
            RecipeResponse initialResponse = new RecipeResponse(
                 testRecipe.getId(), testRecipe.getUserId(), testRecipe.getTitle(),
                 testRecipe.getServingSuggestions(), testRecipe.getInstructions(),
                 testRecipe.getImageUrl(), testRecipeRequest.ingredients(),
                 testRecipe.getTotalTimeMinutes(), null, null, null,
                 testRecipe.getDifficulty(), testRecipe.getIsAiGenerated(),
                 null, null, null, testRecipe.getUpvotes(), testRecipe.getDownvotes(), null,
                 testRecipe.getCreatedAt(), testRecipe.getUpdatedAt(), testMacrosDto, null
             );
            List<RecipeResponse> initialResponses = List.of(initialResponse);
            when(recipeMapper.toResponse(testRecipe)).thenReturn(initialResponse);

            // 3. Enhancer adds user-specific details to the list
            List<RecipeResponse> expectedEnhancedResponses = List.of(testRecipeResponse); // Use the full response from setUp
            when(recipeResponseEnhancer.enhanceRecipeListWithUserInteractions(initialResponses, testUserId))
                .thenReturn(expectedEnhancedResponses);

            // When
            List<RecipeResponse> actualResponses = recipeService.getRecipesByUserId(testUserId);

            // Then
            assertThat(actualResponses).isEqualTo(expectedEnhancedResponses);

            // Verify mocks
            verify(recipeRepository).findByUserId(testUserId);
            verify(recipeMapper).toResponse(testRecipe); // Called for each recipe
            verify(recipeResponseEnhancer).enhanceRecipeListWithUserInteractions(initialResponses, testUserId);
            verifyNoInteractions(favoriteRecipeRepository, voteService, commentService, userService); // Enhancement done by Enhancer component
        }

        @Test
        @DisplayName("Should return empty list when user has no recipes")
        void getRecipesByUserId_NoRecipes_ReturnsEmptyList() {
           // Given
           // 1. Repository returns empty list
           when(recipeRepository.findByUserId(testUserId)).thenReturn(Collections.emptyList());

           // 2. Enhancer gets called with empty list and returns empty list
           List<RecipeResponse> emptyInitialList = Collections.emptyList();
           List<RecipeResponse> emptyEnhancedList = Collections.emptyList();
           when(recipeResponseEnhancer.enhanceRecipeListWithUserInteractions(emptyInitialList, testUserId))
               .thenReturn(emptyEnhancedList);

           // When
           List<RecipeResponse> actualResponses = recipeService.getRecipesByUserId(testUserId);

           // Then
           assertThat(actualResponses).isEmpty();

           // Verify mocks
           verify(recipeRepository).findByUserId(testUserId);
           // Mapper not called if list is empty
           verify(recipeMapper, never()).toResponse(any(Recipe.class));
           // Enhancer still called, but with an empty list
           verify(recipeResponseEnhancer).enhanceRecipeListWithUserInteractions(emptyInitialList, testUserId);
           verifyNoInteractions(favoriteRecipeRepository, voteService, commentService, userService);
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
            
            // 1. Repository returns a Page of recipes
            List<Recipe> recipesInPage = List.of(testRecipe);
            Page<Recipe> recipePage = new PageImpl<>(recipesInPage, pageable, 1); // Total 1 element for simplicity
            when(recipeRepository.findAll(pageable)).thenReturn(recipePage);

            // 2. Mapper converts entities to basic responses
            RecipeResponse initialResponse = new RecipeResponse(
                 testRecipe.getId(), testRecipe.getUserId(), testRecipe.getTitle(),
                 testRecipe.getServingSuggestions(), testRecipe.getInstructions(),
                 testRecipe.getImageUrl(), testRecipeRequest.ingredients(),
                 testRecipe.getTotalTimeMinutes(), null, null, null,
                 testRecipe.getDifficulty(), testRecipe.getIsAiGenerated(),
                 null, null, null, testRecipe.getUpvotes(), testRecipe.getDownvotes(), null,
                 testRecipe.getCreatedAt(), testRecipe.getUpdatedAt(), testMacrosDto, null
             );
            List<RecipeResponse> initialResponses = List.of(initialResponse);
            when(recipeMapper.toResponse(testRecipe)).thenReturn(initialResponse);

            // 3. Enhancer enhances the list of responses
            List<RecipeResponse> enhancedResponses = List.of(testRecipeResponse);
            when(recipeResponseEnhancer.enhanceRecipeListWithUserInteractions(initialResponses, testUserId))
                .thenReturn(enhancedResponses);

            // 4. Expected final Page<RecipeResponse>
            Page<RecipeResponse> expectedPageResponse = new PageImpl<>(enhancedResponses, pageable, recipePage.getTotalElements());

            // When
            Page<RecipeResponse> actualPageResponse = recipeService.getRecipeFeed(testUserId, pageable);

            // Then
            assertThat(actualPageResponse).isEqualTo(expectedPageResponse);
            assertThat(actualPageResponse.getContent()).isEqualTo(enhancedResponses);

            // Verify mocks
            verify(recipeRepository).findAll(pageable);
            verify(recipeMapper).toResponse(testRecipe);
            verify(recipeResponseEnhancer).enhanceRecipeListWithUserInteractions(initialResponses, testUserId);
            verifyNoInteractions(favoriteRecipeRepository, voteService, commentService, userService); // Enhancement handled by component
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
            // 1. Permission Check: repo finds the existing recipe owned by the user
            when(recipeRepository.findById(testRecipeId)).thenReturn(Optional.of(existingRecipe));

            // 2. Mapper converts update DTO to a base entity for update
            Recipe mappedFromRequest = Recipe.builder()
                    .title(updateRequest.title())
                    .ingredients(String.join(";", updateRequest.ingredients()))
                    .instructions(updateRequest.instructions())
                    .servingSuggestions(updateRequest.servingSuggestions())
                    .imageUrl(updateRequest.imageUrl()) // Comes from update request DTO
                    .totalTimeMinutes(updateRequest.totalTimeMinutes())
                    .difficulty(updateRequest.difficulty())
                    .macros(Macros.builder() // Assuming mapper handles this
                        .calories(updateRequest.macros().calories())
                        .proteinGrams(updateRequest.macros().proteinGrams())
                        .carbsGrams(updateRequest.macros().carbsGrams())
                        .fatGrams(updateRequest.macros().fatGrams())
                        .build())
                    .isAiGenerated(updateRequest.isAiGenerated())
                    // Defaults / non-updatable fields are not set here
                    .build();
            when(recipeMapper.toEntity(updateRequest)).thenReturn(mappedFromRequest);

            // 3. Repository saves the updated entity
            // Capture the entity passed to save to verify fields
            Recipe expectedSavedRecipe = existingRecipe.toBuilder() // Start from existing
                    .title(updateRequest.title())
                    .ingredients(String.join(";", updateRequest.ingredients()))
                    .instructions(updateRequest.instructions())
                    .servingSuggestions(updateRequest.servingSuggestions())
                    .imageUrl(updateRequest.imageUrl()) // Use URL from DTO as no new image provided
                    .totalTimeMinutes(updateRequest.totalTimeMinutes())
                    .difficulty(updateRequest.difficulty())
                    .macros(mappedFromRequest.getMacros()) // Use the mapped macros
                    .isAiGenerated(updateRequest.isAiGenerated())
                    .updatedAt(any(LocalDateTime.class)) // Expect updatedAt to change
                    // Fields like id, userId, createdAt, votes should remain the same
                    .build();
            when(recipeRepository.save(recipeCaptor.capture())).thenReturn(expectedSavedRecipe);

            // 4. Mapper converts the *saved* (updated) entity to a basic response
            RecipeResponse initialResponseBeforeEnhancement = new RecipeResponse(
                expectedSavedRecipe.getId(), expectedSavedRecipe.getUserId(), expectedSavedRecipe.getTitle(),
                expectedSavedRecipe.getServingSuggestions(), expectedSavedRecipe.getInstructions(),
                expectedSavedRecipe.getImageUrl(), updateRequest.ingredients(), // Mapper needs to split string
                expectedSavedRecipe.getTotalTimeMinutes(), null, null, null,
                expectedSavedRecipe.getDifficulty(), expectedSavedRecipe.getIsAiGenerated(),
                null, null, null, expectedSavedRecipe.getUpvotes(), expectedSavedRecipe.getDownvotes(), null,
                expectedSavedRecipe.getCreatedAt(), expectedSavedRecipe.getUpdatedAt(), updateRequest.macros(), null
            );
            when(recipeMapper.toResponse(expectedSavedRecipe)).thenReturn(initialResponseBeforeEnhancement);

            // 5. Mocks for enhancement
            boolean expectedIsFavorite = false;
            RecipeVote.VoteType expectedVoteType = null;
            long expectedCommentCount = 5L;
            when(favoriteRecipeRepository.existsByUserIdAndRecipeId(testRecipeId, testUserId)).thenReturn(expectedIsFavorite);
            when(voteService.getUserVote(testRecipeId, testUserId)).thenReturn(expectedVoteType);
            when(commentService.getCommentCount(testRecipeId)).thenReturn(expectedCommentCount);

            // 6. Final expected response
            RecipeResponse finalEnhancedResponse = new RecipeResponse(
                expectedSavedRecipe.getId(), expectedSavedRecipe.getUserId(), expectedSavedRecipe.getTitle(),
                expectedSavedRecipe.getServingSuggestions(), expectedSavedRecipe.getInstructions(),
                expectedSavedRecipe.getImageUrl(), updateRequest.ingredients(),
                expectedSavedRecipe.getTotalTimeMinutes(),
                testRecipeResponse.authorName(), testRecipeResponse.username(), testRecipeResponse.authorId(), // Keep from original setup
                expectedSavedRecipe.getDifficulty(), expectedSavedRecipe.getIsAiGenerated(),
                expectedIsFavorite, testRecipeResponse.favoriteCount(), expectedCommentCount,
                expectedSavedRecipe.getUpvotes(), expectedSavedRecipe.getDownvotes(), null,
                expectedSavedRecipe.getCreatedAt(), expectedSavedRecipe.getUpdatedAt(), updateRequest.macros(), testRecipeResponse.additionalFields()
            );

            // When
            // Call the update method without the image parameter
            RecipeResponse actualResponse = recipeService.updateRecipe(testRecipeId, updateRequest, testUserId);

            // Then
            assertThat(actualResponse).isEqualTo(finalEnhancedResponse);

            // Verify mocks
            verify(recipeRepository).findById(testRecipeId); // Permission check
            verify(recipeMapper).toEntity(updateRequest);
            verify(recipeRepository).save(any(Recipe.class));
            verify(recipeMapper).toResponse(expectedSavedRecipe);
            // Verify enhancement mocks
            verify(favoriteRecipeRepository).existsByUserIdAndRecipeId(testRecipeId, testUserId);
            verify(voteService).getUserVote(testRecipeId, testUserId);
            verify(commentService).getCommentCount(testRecipeId);
            verifyNoInteractions(recipeImageService); // No image interaction
            verifyNoInteractions(recipeResponseEnhancer);

            // Verify captured entity for save()
            Recipe savedEntity = recipeCaptor.getValue();
            assertThat(savedEntity.getId()).isEqualTo(testRecipeId);
            assertThat(savedEntity.getUserId()).isEqualTo(testUserId);
            assertThat(savedEntity.getTitle()).isEqualTo(updateRequest.title());
            assertThat(savedEntity.getImageUrl()).isEqualTo(updateRequest.imageUrl()); // Should match DTO URL
            assertThat(savedEntity.getCreatedAt()).isEqualTo(existingRecipe.getCreatedAt()); // Should not change
            assertThat(savedEntity.getUpvotes()).isEqualTo(existingRecipe.getUpvotes()); // Should not change
            assertThat(savedEntity.getDownvotes()).isEqualTo(existingRecipe.getDownvotes()); // Should not change
            // We can't easily assert the exact updated time, but could check it's different if needed
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
            // TODO: Mock repo.findById to return empty Optional

            // When / Then
            // TODO: Assert ResourceNotFoundException is thrown during checkRecipePermission
        }

        @Test
        @DisplayName("Should throw UnauthorizedAccessException when user is not owner")
        void updateRecipe_NotOwner_ThrowsUnauthorizedAccessException() {
            // Given
            UUID differentUserId = UUID.randomUUID();
            testRecipe = testRecipe.toBuilder().userId(differentUserId).build(); // Set owner to someone else
            // TODO: Mock repo.findById to return recipe owned by different user

            // When / Then
             // TODO: Assert UnauthorizedAccessException is thrown during checkRecipePermission
        }
    }

    @Nested
    @DisplayName("deleteRecipe Tests")
    class DeleteRecipeTests {

        @Test
        @DisplayName("Should delete recipe successfully")
        void deleteRecipe_Success() {
            // Given
            // TODO: Mock checkRecipePermission (repo.findById), repo.deleteById, imageService.deleteImage (optional)

            // When
            // TODO: Call service.deleteRecipe(id, userId)

            // Then
            // TODO: Verify interactions (repo.deleteById called)
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException when recipe to delete not found")
        void deleteRecipe_NotFound_ThrowsResourceNotFoundException() {
             // Given
            // TODO: Mock repo.findById to return empty Optional

            // When / Then
            // TODO: Assert ResourceNotFoundException is thrown during checkRecipePermission
        }

        @Test
        @DisplayName("Should throw UnauthorizedAccessException when user is not owner")
        void deleteRecipe_NotOwner_ThrowsUnauthorizedAccessException() {
            // Given
            UUID differentUserId = UUID.randomUUID();
            testRecipe = testRecipe.toBuilder().userId(differentUserId).build(); // Set owner to someone else
            // TODO: Mock repo.findById to return recipe owned by different user

            // When / Then
             // TODO: Assert UnauthorizedAccessException is thrown during checkRecipePermission
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
            // TODO: Mock aiService.generateRecipe, mapper? (Maybe AI service returns DTO directly)

            // When
            // TODO: Call service.generateMeal(ingredients)

            // Then
            // TODO: Verify aiService interaction, assert response
        }

         @Test
        @DisplayName("Should handle AI service exception")
        void generateMeal_AiServiceError_ThrowsException() {
            // Given
            List<String> ingredients = List.of("chicken", "broccoli");
            // TODO: Mock aiService.generateRecipe to throw AIServiceException

            // When / Then
            // TODO: Assert AIServiceException (or a wrapper) is thrown
        }
    }

    // TODO: Add tests for enhanceWithUserInteractions if direct testing is needed
    // TODO: Add tests covering edge cases for image handling (e.g., empty image URL in request, different scenarios of processing failure)
    // TODO: Consider testing transactionality if specific rollback scenarios are critical (more complex, might need integration tests)

} 