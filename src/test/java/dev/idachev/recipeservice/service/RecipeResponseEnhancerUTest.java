package dev.idachev.recipeservice.service;

import dev.idachev.recipeservice.model.DifficultyLevel;
import dev.idachev.recipeservice.model.RecipeVote;
import dev.idachev.recipeservice.repository.FavoriteRecipeRepository;
import dev.idachev.recipeservice.user.service.UserService;
import dev.idachev.recipeservice.web.dto.MacrosDto;
import dev.idachev.recipeservice.web.dto.RecipeResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RecipeResponseEnhancerUTest {

    @Mock
    private FavoriteRecipeRepository favoriteRecipeRepository;
    @Mock
    private VoteService voteService;
    @Mock
    private CommentService commentService;
    @Mock
    private UserService userService;

    @InjectMocks
    private RecipeResponseEnhancer recipeResponseEnhancer;

    private UUID recipeId1;
    private UUID recipeId2;
    private UUID authorId1;
    private UUID authorId2;
    private UUID currentUserId;
    private RecipeResponse baseResponse1;
    private RecipeResponse baseResponse2;
    private List<RecipeResponse> baseResponses;
    private MacrosDto testMacrosDto;

    @BeforeEach
    void setUp() {
        recipeId1 = UUID.randomUUID();
        recipeId2 = UUID.randomUUID();
        authorId1 = UUID.randomUUID();
        authorId2 = UUID.randomUUID(); // Author of recipe 2
        currentUserId = UUID.randomUUID(); // User viewing the recipes

        testMacrosDto = new MacrosDto(BigDecimal.valueOf(400), BigDecimal.valueOf(25), BigDecimal.valueOf(45), BigDecimal.valueOf(15));

        // Create base responses (as they would come from RecipeMapper, without enhancement)
        baseResponse1 = new RecipeResponse(
            recipeId1, authorId1, "Recipe 1 Title", "Serving 1", "Instructions 1",
            "img1.jpg", List.of("Ing1"), 30, null, null, null,
            DifficultyLevel.EASY, false, null, null, null, 5, 1, null,
            LocalDateTime.now().minusDays(1), LocalDateTime.now(), testMacrosDto, null
        );

        baseResponse2 = new RecipeResponse(
            recipeId2, authorId2, "Recipe 2 Title", "Serving 2", "Instructions 2",
            "img2.jpg", List.of("Ing2"), 60, null, null, null,
            DifficultyLevel.HARD, true, null, null, null, 10, 0, null,
            LocalDateTime.now().minusDays(2), LocalDateTime.now().minusHours(5), testMacrosDto, null
        );
        
        baseResponses = List.of(baseResponse1, baseResponse2);
    }

    // --- Test Methods ---

    @Nested
    @DisplayName("enhanceRecipeListWithUserInteractions Tests")
    class EnhanceRecipeListTests {

        @Test
        @DisplayName("Should enhance list with user interactions when userId is provided")
        void enhance_WithUserId_Success() {
            // Given
            Set<UUID> recipeIds = Set.of(recipeId1, recipeId2);
            Set<UUID> authorIds = Set.of(authorId1, authorId2);

            // Mock dependency responses
            Map<UUID, Long> favCounts = Map.of(recipeId1, 5L, recipeId2, 10L);
            Map<UUID, Boolean> userFavs = Map.of(recipeId1, true, recipeId2, false);
            Map<UUID, RecipeVote.VoteType> userVotes = Map.of(recipeId1, RecipeVote.VoteType.UPVOTE); // No vote for recipeId2
            Map<UUID, Long> commentCounts = Map.of(recipeId1, 3L, recipeId2, 0L);
            Map<UUID, String> authorUsernames = Map.of(authorId1, "author1", authorId2, "author2");

            when(favoriteRecipeRepository.getFavoriteCountsMap(recipeIds)).thenReturn(favCounts);
            when(favoriteRecipeRepository.getUserFavoritesMap(currentUserId, recipeIds)).thenReturn(userFavs);
            when(voteService.getUserVotesForRecipes(currentUserId, recipeIds)).thenReturn(userVotes);
            when(commentService.getCommentCountsForRecipes(recipeIds)).thenReturn(commentCounts);
            when(userService.getUsernamesByIds(authorIds)).thenReturn(authorUsernames);

            // Define expected enhanced responses
            RecipeResponse expectedResponse1 = new RecipeResponse(
                baseResponse1.id(), baseResponse1.createdById(), baseResponse1.title(), baseResponse1.servingSuggestions(),
                baseResponse1.instructions(), baseResponse1.imageUrl(), baseResponse1.ingredients(), baseResponse1.totalTimeMinutes(),
                "author1", "author1", authorId1.toString(), // Author info
                baseResponse1.difficulty(), baseResponse1.isAiGenerated(),
                true, 5L, 3L, // isFavorite, favoriteCount, commentCount
                baseResponse1.upvotes(), baseResponse1.downvotes(),
                "UPVOTE", // userVote
                baseResponse1.createdAt(), baseResponse1.updatedAt(), baseResponse1.macros(), baseResponse1.additionalFields()
            );
            RecipeResponse expectedResponse2 = new RecipeResponse(
                baseResponse2.id(), baseResponse2.createdById(), baseResponse2.title(), baseResponse2.servingSuggestions(),
                baseResponse2.instructions(), baseResponse2.imageUrl(), baseResponse2.ingredients(), baseResponse2.totalTimeMinutes(),
                "author2", "author2", authorId2.toString(), // Author info
                baseResponse2.difficulty(), baseResponse2.isAiGenerated(),
                false, 10L, 0L, // isFavorite, favoriteCount, commentCount
                baseResponse2.upvotes(), baseResponse2.downvotes(),
                null, // userVote (null because not in userVotes map)
                baseResponse2.createdAt(), baseResponse2.updatedAt(), baseResponse2.macros(), baseResponse2.additionalFields()
            );
            List<RecipeResponse> expectedEnhancedResponses = List.of(expectedResponse1, expectedResponse2);

            // When
            List<RecipeResponse> actualEnhancedResponses = recipeResponseEnhancer.enhanceRecipeListWithUserInteractions(baseResponses, currentUserId);

            // Then
            assertThat(actualEnhancedResponses)
                .isNotNull()
                .containsExactlyInAnyOrderElementsOf(expectedEnhancedResponses); // Order might change due to stream processing

            // Verify all mocks were called correctly
            verify(favoriteRecipeRepository).getFavoriteCountsMap(recipeIds);
            verify(favoriteRecipeRepository).getUserFavoritesMap(currentUserId, recipeIds);
            verify(voteService).getUserVotesForRecipes(currentUserId, recipeIds);
            verify(commentService).getCommentCountsForRecipes(recipeIds);
            verify(userService).getUsernamesByIds(authorIds);
        }

        @Test
        @DisplayName("Should enhance list without user-specific data when userId is null")
        void enhance_NullUserId_SkipsUserSpecificData() {
            // Given
            Set<UUID> recipeIds = Set.of(recipeId1, recipeId2);
            Set<UUID> authorIds = Set.of(authorId1, authorId2);
            UUID nullUserId = null;

            // Mock non-user-specific dependency responses
            Map<UUID, Long> favCounts = Map.of(recipeId1, 5L, recipeId2, 10L);
            Map<UUID, Long> commentCounts = Map.of(recipeId1, 3L, recipeId2, 0L);
            Map<UUID, String> authorUsernames = Map.of(authorId1, "author1", authorId2, "author2");

            when(favoriteRecipeRepository.getFavoriteCountsMap(recipeIds)).thenReturn(favCounts);
            // No mock for getUserFavoritesMap
            // No mock for getUserVotesForRecipes
            when(commentService.getCommentCountsForRecipes(recipeIds)).thenReturn(commentCounts);
            when(userService.getUsernamesByIds(authorIds)).thenReturn(authorUsernames);

            // Define expected enhanced responses (isFavorite=false, userVote=null)
            RecipeResponse expectedResponse1 = new RecipeResponse(
                baseResponse1.id(), baseResponse1.createdById(), baseResponse1.title(), baseResponse1.servingSuggestions(),
                baseResponse1.instructions(), baseResponse1.imageUrl(), baseResponse1.ingredients(), baseResponse1.totalTimeMinutes(),
                "author1", "author1", authorId1.toString(),
                baseResponse1.difficulty(), baseResponse1.isAiGenerated(),
                false, 5L, 3L, // isFavorite = false
                baseResponse1.upvotes(), baseResponse1.downvotes(),
                null, // userVote = null
                baseResponse1.createdAt(), baseResponse1.updatedAt(), baseResponse1.macros(), baseResponse1.additionalFields()
            );
            RecipeResponse expectedResponse2 = new RecipeResponse(
                baseResponse2.id(), baseResponse2.createdById(), baseResponse2.title(), baseResponse2.servingSuggestions(),
                baseResponse2.instructions(), baseResponse2.imageUrl(), baseResponse2.ingredients(), baseResponse2.totalTimeMinutes(),
                "author2", "author2", authorId2.toString(),
                baseResponse2.difficulty(), baseResponse2.isAiGenerated(),
                false, 10L, 0L, // isFavorite = false
                baseResponse2.upvotes(), baseResponse2.downvotes(),
                null, // userVote = null
                baseResponse2.createdAt(), baseResponse2.updatedAt(), baseResponse2.macros(), baseResponse2.additionalFields()
            );
            List<RecipeResponse> expectedEnhancedResponses = List.of(expectedResponse1, expectedResponse2);

            // When
            List<RecipeResponse> actualEnhancedResponses = recipeResponseEnhancer.enhanceRecipeListWithUserInteractions(baseResponses, nullUserId);

            // Then
            assertThat(actualEnhancedResponses)
                .isNotNull()
                .containsExactlyInAnyOrderElementsOf(expectedEnhancedResponses);

            // Verify mocks
            verify(favoriteRecipeRepository).getFavoriteCountsMap(recipeIds);
            verify(commentService).getCommentCountsForRecipes(recipeIds);
            verify(userService).getUsernamesByIds(authorIds);
            // Verify user-specific methods were NOT called
            verify(favoriteRecipeRepository, never()).getUserFavoritesMap(any(), any());
            verify(voteService, never()).getUserVotesForRecipes(any(), any());
        }

        @Test
        @DisplayName("Should return empty list when input list is empty")
        void enhance_EmptyInputList_ReturnsEmptyList() {
            // Given
            List<RecipeResponse> emptyList = Collections.emptyList();

            // When
            List<RecipeResponse> result = recipeResponseEnhancer.enhanceRecipeListWithUserInteractions(emptyList, currentUserId);

            // Then
            assertThat(result).isNotNull().isEmpty();
            // Verify no interactions with any mocks
            verifyNoInteractions(favoriteRecipeRepository, voteService, commentService, userService);
        }
        
        @Test
        @DisplayName("Should return empty list when input list is null")
        void enhance_NullInputList_ReturnsEmptyList() {
             // Given
             List<RecipeResponse> nullList = null;

            // When
            List<RecipeResponse> result = recipeResponseEnhancer.enhanceRecipeListWithUserInteractions(nullList, currentUserId);

            // Then
            assertThat(result).isNotNull().isEmpty();
             // Verify no interactions with any mocks
             verifyNoInteractions(favoriteRecipeRepository, voteService, commentService, userService);
        }

        @Test
        @DisplayName("Should handle missing data from dependencies gracefully (use defaults)")
        void enhance_MissingDataFromMocks_UsesDefaults() {
            // Given
            Set<UUID> recipeIds = Set.of(recipeId1, recipeId2);
            Set<UUID> authorIds = Set.of(authorId1, authorId2);

            // Mock dependencies to return incomplete/empty data
            when(favoriteRecipeRepository.getFavoriteCountsMap(recipeIds)).thenReturn(Map.of(recipeId1, 2L)); // Missing recipeId2
            when(favoriteRecipeRepository.getUserFavoritesMap(currentUserId, recipeIds)).thenReturn(Collections.emptyMap()); // User favorited none
            when(voteService.getUserVotesForRecipes(currentUserId, recipeIds)).thenReturn(Map.of(recipeId2, RecipeVote.VoteType.DOWNVOTE)); // Only vote for recipe 2
            when(commentService.getCommentCountsForRecipes(recipeIds)).thenReturn(Collections.emptyMap()); // No comments
            when(userService.getUsernamesByIds(authorIds)).thenReturn(Map.of(authorId1, "author1")); // Missing authorId2

            // Define expected enhanced responses using defaults
            RecipeResponse expectedResponse1 = new RecipeResponse(
                baseResponse1.id(), baseResponse1.createdById(), baseResponse1.title(), baseResponse1.servingSuggestions(),
                baseResponse1.instructions(), baseResponse1.imageUrl(), baseResponse1.ingredients(), baseResponse1.totalTimeMinutes(),
                "author1", "author1", authorId1.toString(), // Found author
                baseResponse1.difficulty(), baseResponse1.isAiGenerated(),
                false, 2L, 0L, // isFavorite=false (default), favCount=2, commentCount=0 (default)
                baseResponse1.upvotes(), baseResponse1.downvotes(),
                null, // userVote=null (default)
                baseResponse1.createdAt(), baseResponse1.updatedAt(), baseResponse1.macros(), baseResponse1.additionalFields()
            );
            RecipeResponse expectedResponse2 = new RecipeResponse(
                baseResponse2.id(), baseResponse2.createdById(), baseResponse2.title(), baseResponse2.servingSuggestions(),
                baseResponse2.instructions(), baseResponse2.imageUrl(), baseResponse2.ingredients(), baseResponse2.totalTimeMinutes(),
                "Unknown User", "Unknown User", authorId2.toString(), // Default author names
                baseResponse2.difficulty(), baseResponse2.isAiGenerated(),
                false, 0L, 0L, // isFavorite=false (default), favCount=0 (default), commentCount=0 (default)
                baseResponse2.upvotes(), baseResponse2.downvotes(),
                "DOWNVOTE", // userVote=DOWNVOTE
                baseResponse2.createdAt(), baseResponse2.updatedAt(), baseResponse2.macros(), baseResponse2.additionalFields()
            );
            List<RecipeResponse> expectedEnhancedResponses = List.of(expectedResponse1, expectedResponse2);

            // When
            List<RecipeResponse> actualEnhancedResponses = recipeResponseEnhancer.enhanceRecipeListWithUserInteractions(baseResponses, currentUserId);

            // Then
            assertThat(actualEnhancedResponses)
                .isNotNull()
                .containsExactlyInAnyOrderElementsOf(expectedEnhancedResponses);

            // Verify all mocks were called
            verify(favoriteRecipeRepository).getFavoriteCountsMap(recipeIds);
            verify(favoriteRecipeRepository).getUserFavoritesMap(currentUserId, recipeIds);
            verify(voteService).getUserVotesForRecipes(currentUserId, recipeIds);
            verify(commentService).getCommentCountsForRecipes(recipeIds);
            verify(userService).getUsernamesByIds(authorIds);
        }
    }
} 