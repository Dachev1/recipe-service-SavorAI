package dev.idachev.recipeservice.service;

import dev.idachev.recipeservice.exception.BadRequestException;
import dev.idachev.recipeservice.exception.ResourceNotFoundException;
import dev.idachev.recipeservice.exception.UnauthorizedAccessException;
import dev.idachev.recipeservice.model.Recipe;
import dev.idachev.recipeservice.model.RecipeVote;
import dev.idachev.recipeservice.repository.RecipeRepository;
import dev.idachev.recipeservice.repository.RecipeVoteRepository;
import dev.idachev.recipeservice.web.dto.VoteRequest;
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

import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VoteServiceUTest {

    @Mock
    private RecipeVoteRepository voteRepository;
    @Mock
    private RecipeRepository recipeRepository;

    @InjectMocks
    private VoteService voteService;

    @Captor
    private ArgumentCaptor<RecipeVote> voteCaptor;
    @Captor
    private ArgumentCaptor<Recipe> recipeCaptor;

    private UUID testUserId;
    private UUID testRecipeId;
    private UUID testRecipeOwnerId;
    private Recipe testRecipe;
    private VoteRequest upvoteRequest;
    private VoteRequest downvoteRequest;

    @BeforeEach
    void setUp() {
        testUserId = UUID.randomUUID();
        testRecipeId = UUID.randomUUID();
        testRecipeOwnerId = UUID.randomUUID(); // Different from testUserId

        // Initial recipe state (e.g., 10 upvotes, 2 downvotes)
        testRecipe = Recipe.builder()
                .id(testRecipeId)
                .userId(testRecipeOwnerId)
                .title("Vote Test Recipe")
                .upvotes(10)
                .downvotes(2)
                .version(1L)
                .createdAt(LocalDateTime.now().minusDays(1))
                .updatedAt(LocalDateTime.now().minusHours(1))
                .build();

        upvoteRequest = new VoteRequest("UPVOTE");
        downvoteRequest = new VoteRequest("DOWNVOTE");
    }

    // --- Test Methods ---

    @Nested
    @DisplayName("vote Tests")
    class VoteTests {

        @Test
        @DisplayName("Should create new upvote successfully")
        void vote_NewUpvote_Success() {
            when(recipeRepository.findById(eq(testRecipeId))).thenReturn(Optional.of(testRecipe));
            when(voteRepository.findByUserIdAndRecipeId(eq(testUserId), eq(testRecipeId))).thenReturn(Optional.empty());
            when(voteRepository.save(voteCaptor.capture())).thenAnswer(invocation -> {
                RecipeVote voteToSave = invocation.getArgument(0);
                // Simulate ID generation on save
                return voteToSave.toBuilder().id(UUID.randomUUID()).build();
            });
            Recipe expectedRecipeAfterSave = testRecipe.toBuilder()
                                                .upvotes(testRecipe.getUpvotes() + 1)
                                                .updatedAt(LocalDateTime.now())
                                                .build();
            // Need to return the recipe state *after* it would have been saved
            when(recipeRepository.save(recipeCaptor.capture())).thenReturn(expectedRecipeAfterSave);

            Recipe returnedRecipe = voteService.vote(testRecipeId, upvoteRequest, testUserId);

            verify(voteRepository).findByUserIdAndRecipeId(eq(testUserId), eq(testRecipeId));
            verify(voteRepository).save(any(RecipeVote.class));
            verify(voteRepository, never()).delete(any(RecipeVote.class)); // No vote deleted
            
            RecipeVote savedVote = voteCaptor.getValue();
            assertThat(savedVote.getId()).isNull(); // ID is null before save
            assertThat(savedVote.getUserId()).isEqualTo(testUserId);
            assertThat(savedVote.getRecipeId()).isEqualTo(testRecipeId);
            assertThat(savedVote.getVoteType()).isEqualTo(RecipeVote.VoteType.UPVOTE);

            verify(recipeRepository).findById(eq(testRecipeId));
            verify(recipeRepository).save(any(Recipe.class));
            
            Recipe recipeToSave = recipeCaptor.getValue();
            assertThat(recipeToSave.getId()).isEqualTo(testRecipeId);
            assertThat(recipeToSave.getUpvotes()).isEqualTo(testRecipe.getUpvotes() + 1);
            assertThat(recipeToSave.getDownvotes()).isEqualTo(testRecipe.getDownvotes()); // Downvotes unchanged

            assertThat(returnedRecipe).isNotNull();
            // Assert against the state *after* save was mocked
            assertThat(returnedRecipe.getUpvotes()).isEqualTo(expectedRecipeAfterSave.getUpvotes());
            assertThat(returnedRecipe.getDownvotes()).isEqualTo(expectedRecipeAfterSave.getDownvotes());
        }

        @Test
        @DisplayName("Should create new downvote successfully")
        void vote_NewDownvote_Success() {
            when(recipeRepository.findById(eq(testRecipeId))).thenReturn(Optional.of(testRecipe));
            when(voteRepository.findByUserIdAndRecipeId(eq(testUserId), eq(testRecipeId))).thenReturn(Optional.empty());
            when(voteRepository.save(voteCaptor.capture())).thenAnswer(invocation -> {
                RecipeVote voteToSave = invocation.getArgument(0);
                return voteToSave.toBuilder().id(UUID.randomUUID()).build();
            });
            Recipe expectedRecipeAfterSave = testRecipe.toBuilder()
                                                .downvotes(testRecipe.getDownvotes() + 1)
                                                .updatedAt(LocalDateTime.now())
                                                .build();
            when(recipeRepository.save(recipeCaptor.capture())).thenReturn(expectedRecipeAfterSave);

            Recipe returnedRecipe = voteService.vote(testRecipeId, downvoteRequest, testUserId);

            verify(voteRepository).findByUserIdAndRecipeId(eq(testUserId), eq(testRecipeId));
            verify(voteRepository).save(any(RecipeVote.class));
            verify(voteRepository, never()).delete(any(RecipeVote.class));
            
            RecipeVote savedVote = voteCaptor.getValue();
            assertThat(savedVote.getVoteType()).isEqualTo(RecipeVote.VoteType.DOWNVOTE);
            assertThat(savedVote.getUserId()).isEqualTo(testUserId);
            assertThat(savedVote.getRecipeId()).isEqualTo(testRecipeId);

            verify(recipeRepository).findById(eq(testRecipeId));
            verify(recipeRepository).save(any(Recipe.class));
            
            Recipe recipeToSave = recipeCaptor.getValue();
            assertThat(recipeToSave.getDownvotes()).isEqualTo(testRecipe.getDownvotes() + 1);
            assertThat(recipeToSave.getUpvotes()).isEqualTo(testRecipe.getUpvotes()); // Upvotes unchanged

            assertThat(returnedRecipe).isNotNull();
            assertThat(returnedRecipe.getDownvotes()).isEqualTo(expectedRecipeAfterSave.getDownvotes());
            assertThat(returnedRecipe.getUpvotes()).isEqualTo(expectedRecipeAfterSave.getUpvotes());
        }

        @Test
        @DisplayName("Should change existing upvote to downvote")
        void vote_ChangeUpToDown_Success() {
            RecipeVote existingUpvote = RecipeVote.builder()
                    .id(UUID.randomUUID()).userId(testUserId).recipeId(testRecipeId)
                    .voteType(RecipeVote.VoteType.UPVOTE)
                    .build();
            
            when(recipeRepository.findById(eq(testRecipeId))).thenReturn(Optional.of(testRecipe));
            when(voteRepository.findByUserIdAndRecipeId(eq(testUserId), eq(testRecipeId))).thenReturn(Optional.of(existingUpvote));
            doNothing().when(voteRepository).delete(existingUpvote); // Mock deletion of old vote
            when(voteRepository.save(voteCaptor.capture())).thenAnswer(invocation -> { // Mock saving new vote
                RecipeVote voteToSave = invocation.getArgument(0);
                return voteToSave.toBuilder().id(UUID.randomUUID()).build();
            });
            
            Recipe expectedRecipeAfterSave = testRecipe.toBuilder()
                                                .upvotes(testRecipe.getUpvotes() - 1) // Decrement upvote
                                                .downvotes(testRecipe.getDownvotes() + 1) // Increment downvote
                                                .updatedAt(LocalDateTime.now())
                                                .build();
            when(recipeRepository.save(recipeCaptor.capture())).thenReturn(expectedRecipeAfterSave);

            Recipe returnedRecipe = voteService.vote(testRecipeId, downvoteRequest, testUserId);

            verify(voteRepository).findByUserIdAndRecipeId(eq(testUserId), eq(testRecipeId));
            verify(voteRepository).delete(existingUpvote); // Verify old vote deleted
            verify(voteRepository).save(any(RecipeVote.class)); // Verify new vote saved
            
            RecipeVote savedVote = voteCaptor.getValue();
            assertThat(savedVote.getVoteType()).isEqualTo(RecipeVote.VoteType.DOWNVOTE);
            assertThat(savedVote.getUserId()).isEqualTo(testUserId);
            assertThat(savedVote.getRecipeId()).isEqualTo(testRecipeId);

            verify(recipeRepository).findById(eq(testRecipeId));
            verify(recipeRepository).save(any(Recipe.class));
            
            Recipe recipeToSave = recipeCaptor.getValue();
            assertThat(recipeToSave.getUpvotes()).isEqualTo(testRecipe.getUpvotes() - 1);
            assertThat(recipeToSave.getDownvotes()).isEqualTo(testRecipe.getDownvotes() + 1);

            assertThat(returnedRecipe).isNotNull();
            assertThat(returnedRecipe.getUpvotes()).isEqualTo(expectedRecipeAfterSave.getUpvotes());
            assertThat(returnedRecipe.getDownvotes()).isEqualTo(expectedRecipeAfterSave.getDownvotes());
        }

        @Test
        @DisplayName("Should change existing downvote to upvote")
        void vote_ChangeDownToUp_Success() {
           RecipeVote existingDownvote = RecipeVote.builder()
                    .id(UUID.randomUUID()).userId(testUserId).recipeId(testRecipeId)
                    .voteType(RecipeVote.VoteType.DOWNVOTE)
                    .build();
            
            when(recipeRepository.findById(eq(testRecipeId))).thenReturn(Optional.of(testRecipe));
            when(voteRepository.findByUserIdAndRecipeId(eq(testUserId), eq(testRecipeId))).thenReturn(Optional.of(existingDownvote));
            doNothing().when(voteRepository).delete(existingDownvote);
            when(voteRepository.save(voteCaptor.capture())).thenAnswer(invocation -> {
                RecipeVote voteToSave = invocation.getArgument(0);
                return voteToSave.toBuilder().id(UUID.randomUUID()).build();
            });
            
            Recipe expectedRecipeAfterSave = testRecipe.toBuilder()
                                                .upvotes(testRecipe.getUpvotes() + 1) // Increment upvote
                                                .downvotes(testRecipe.getDownvotes() - 1) // Decrement downvote
                                                .updatedAt(LocalDateTime.now())
                                                .build();
            when(recipeRepository.save(recipeCaptor.capture())).thenReturn(expectedRecipeAfterSave);

            Recipe returnedRecipe = voteService.vote(testRecipeId, upvoteRequest, testUserId);

            verify(voteRepository).findByUserIdAndRecipeId(eq(testUserId), eq(testRecipeId));
            verify(voteRepository).delete(existingDownvote);
            verify(voteRepository).save(any(RecipeVote.class));
            
            RecipeVote savedVote = voteCaptor.getValue();
            assertThat(savedVote.getVoteType()).isEqualTo(RecipeVote.VoteType.UPVOTE);
            assertThat(savedVote.getUserId()).isEqualTo(testUserId);
            assertThat(savedVote.getRecipeId()).isEqualTo(testRecipeId);

            verify(recipeRepository).findById(eq(testRecipeId));
            verify(recipeRepository).save(any(Recipe.class));
            
            Recipe recipeToSave = recipeCaptor.getValue();
            assertThat(recipeToSave.getUpvotes()).isEqualTo(testRecipe.getUpvotes() + 1);
            assertThat(recipeToSave.getDownvotes()).isEqualTo(testRecipe.getDownvotes() - 1);

            assertThat(returnedRecipe).isNotNull();
            assertThat(returnedRecipe.getUpvotes()).isEqualTo(expectedRecipeAfterSave.getUpvotes());
            assertThat(returnedRecipe.getDownvotes()).isEqualTo(expectedRecipeAfterSave.getDownvotes());
        }

        @Test
        @DisplayName("Should remove existing upvote when upvoting again")
        void vote_RemoveUpvote_Success() {
            RecipeVote existingUpvote = RecipeVote.builder()
                    .id(UUID.randomUUID()).userId(testUserId).recipeId(testRecipeId)
                    .voteType(RecipeVote.VoteType.UPVOTE)
                    .build();
            
            when(recipeRepository.findById(eq(testRecipeId))).thenReturn(Optional.of(testRecipe));
            when(voteRepository.findByUserIdAndRecipeId(eq(testUserId), eq(testRecipeId))).thenReturn(Optional.of(existingUpvote));
            doNothing().when(voteRepository).delete(existingUpvote); // Mock deletion
            
            Recipe expectedRecipeAfterSave = testRecipe.toBuilder()
                                                .upvotes(testRecipe.getUpvotes() - 1) // Decrement upvote
                                                .updatedAt(LocalDateTime.now())
                                                .build();
            when(recipeRepository.save(recipeCaptor.capture())).thenReturn(expectedRecipeAfterSave);

            Recipe returnedRecipe = voteService.vote(testRecipeId, upvoteRequest, testUserId);

            verify(voteRepository).findByUserIdAndRecipeId(eq(testUserId), eq(testRecipeId));
            verify(voteRepository).delete(existingUpvote);
            verify(voteRepository, never()).save(any(RecipeVote.class)); // No new vote saved
            
            verify(recipeRepository).findById(eq(testRecipeId));
            verify(recipeRepository).save(any(Recipe.class));
            
            Recipe recipeToSave = recipeCaptor.getValue();
            assertThat(recipeToSave.getUpvotes()).isEqualTo(testRecipe.getUpvotes() - 1);
            assertThat(recipeToSave.getDownvotes()).isEqualTo(testRecipe.getDownvotes()); // Downvotes unchanged

            assertThat(returnedRecipe).isNotNull();
            assertThat(returnedRecipe.getUpvotes()).isEqualTo(expectedRecipeAfterSave.getUpvotes());
            assertThat(returnedRecipe.getDownvotes()).isEqualTo(expectedRecipeAfterSave.getDownvotes());
        }

        @Test
        @DisplayName("Should remove existing downvote when downvoting again")
        void vote_RemoveDownvote_Success() {
            RecipeVote existingDownvote = RecipeVote.builder()
                    .id(UUID.randomUUID()).userId(testUserId).recipeId(testRecipeId)
                    .voteType(RecipeVote.VoteType.DOWNVOTE)
                    .build();
            
            when(recipeRepository.findById(eq(testRecipeId))).thenReturn(Optional.of(testRecipe));
            when(voteRepository.findByUserIdAndRecipeId(eq(testUserId), eq(testRecipeId))).thenReturn(Optional.of(existingDownvote));
            doNothing().when(voteRepository).delete(existingDownvote);
            
            Recipe expectedRecipeAfterSave = testRecipe.toBuilder()
                                                .downvotes(testRecipe.getDownvotes() - 1) // Decrement downvote
                                                .updatedAt(LocalDateTime.now())
                                                .build();
            when(recipeRepository.save(recipeCaptor.capture())).thenReturn(expectedRecipeAfterSave);

            Recipe returnedRecipe = voteService.vote(testRecipeId, downvoteRequest, testUserId);

            verify(voteRepository).findByUserIdAndRecipeId(eq(testUserId), eq(testRecipeId));
            verify(voteRepository).delete(existingDownvote);
            verify(voteRepository, never()).save(any(RecipeVote.class));
            
            verify(recipeRepository).findById(eq(testRecipeId));
            verify(recipeRepository).save(any(Recipe.class));
            
            Recipe recipeToSave = recipeCaptor.getValue();
            assertThat(recipeToSave.getDownvotes()).isEqualTo(testRecipe.getDownvotes() - 1);
            assertThat(recipeToSave.getUpvotes()).isEqualTo(testRecipe.getUpvotes()); // Upvotes unchanged

            assertThat(returnedRecipe).isNotNull();
            assertThat(returnedRecipe.getDownvotes()).isEqualTo(expectedRecipeAfterSave.getDownvotes());
            assertThat(returnedRecipe.getUpvotes()).isEqualTo(expectedRecipeAfterSave.getUpvotes());
        }

        @Test
        @DisplayName("Should throw BadRequestException for invalid vote type string")
        void vote_InvalidTypeString_ThrowsBadRequestException() {
            VoteRequest invalidRequest = new VoteRequest("INVALID_TYPE");

            assertThatThrownBy(() -> voteService.vote(testRecipeId, invalidRequest, testUserId))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("Invalid vote type");

            verifyNoInteractions(recipeRepository, voteRepository);
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException if recipe does not exist")
        void vote_RecipeNotFound_ThrowsResourceNotFoundException() {
            when(recipeRepository.findById(eq(testRecipeId))).thenReturn(Optional.empty());

            assertThatThrownBy(() -> voteService.vote(testRecipeId, upvoteRequest, testUserId))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Recipe not found with id: " + testRecipeId);

            verify(recipeRepository).findById(eq(testRecipeId));
            verifyNoInteractions(voteRepository);
        }

        @Test
        @DisplayName("Should throw UnauthorizedAccessException if user votes on own recipe")
        void vote_OwnRecipe_ThrowsUnauthorizedAccessException() {
            Recipe ownRecipe = testRecipe.toBuilder().userId(testUserId).build(); // Set owner to testUser
            when(recipeRepository.findById(eq(testRecipeId))).thenReturn(Optional.of(ownRecipe));

            assertThatThrownBy(() -> voteService.vote(testRecipeId, upvoteRequest, testUserId))
                    .isInstanceOf(UnauthorizedAccessException.class)
                    .hasMessageContaining("You cannot vote on your own recipe");

            verify(recipeRepository).findById(eq(testRecipeId));
            verifyNoInteractions(voteRepository);
        }
    }

    @Nested
    @DisplayName("getUserVote Tests")
    class GetUserVoteTests {

        @Test
        @DisplayName("Should return user's vote type if vote exists")
        void getUserVote_Exists_ReturnsVoteType() {
            RecipeVote existingVote = RecipeVote.builder()
                .id(UUID.randomUUID()).userId(testUserId).recipeId(testRecipeId)
                .voteType(RecipeVote.VoteType.DOWNVOTE)
                .build();
            when(voteRepository.findByUserIdAndRecipeId(eq(testUserId), eq(testRecipeId)))
                .thenReturn(Optional.of(existingVote));

            RecipeVote.VoteType actualVoteType = voteService.getUserVote(testRecipeId, testUserId);

            assertThat(actualVoteType).isEqualTo(RecipeVote.VoteType.DOWNVOTE);
            verify(voteRepository).findByUserIdAndRecipeId(eq(testUserId), eq(testRecipeId));
            verifyNoMoreInteractions(voteRepository);
            verifyNoInteractions(recipeRepository);
        }

        @Test
        @DisplayName("Should return null if user vote does not exist")
        void getUserVote_NotExists_ReturnsNull() {
            when(voteRepository.findByUserIdAndRecipeId(eq(testUserId), eq(testRecipeId)))
                .thenReturn(Optional.empty());

            RecipeVote.VoteType actualVoteType = voteService.getUserVote(testRecipeId, testUserId);

            assertThat(actualVoteType).isNull();
            verify(voteRepository).findByUserIdAndRecipeId(eq(testUserId), eq(testRecipeId));
            verifyNoMoreInteractions(voteRepository);
            verifyNoInteractions(recipeRepository);
        }
    }

    @Nested
    @DisplayName("getUserVotesForRecipes Tests")
    class GetUserVotesForRecipesTests {

        @Test
        @DisplayName("Should return map of user votes for multiple recipes")
        void getUserVotesForRecipes_Success() {
            UUID recipeId1 = UUID.randomUUID();
            UUID recipeId2 = UUID.randomUUID();
            UUID recipeId3 = UUID.randomUUID(); // User hasn't voted on this one
            Set<UUID> recipeIds = Set.of(recipeId1, recipeId2, recipeId3);

            RecipeVote vote1 = RecipeVote.builder().userId(testUserId).recipeId(recipeId1).voteType(RecipeVote.VoteType.UPVOTE).build();
            RecipeVote vote2 = RecipeVote.builder().userId(testUserId).recipeId(recipeId2).voteType(RecipeVote.VoteType.DOWNVOTE).build();
            List<RecipeVote> existingVotes = List.of(vote1, vote2);

            when(voteRepository.findByUserIdAndRecipeIdIn(eq(testUserId), eq(recipeIds))).thenReturn(existingVotes);

            Map<UUID, RecipeVote.VoteType> userVotes = voteService.getUserVotesForRecipes(testUserId, recipeIds);

            assertThat(userVotes).isNotNull();
            assertThat(userVotes).hasSize(2);
            assertThat(userVotes).containsEntry(recipeId1, RecipeVote.VoteType.UPVOTE);
            assertThat(userVotes).containsEntry(recipeId2, RecipeVote.VoteType.DOWNVOTE);
            assertThat(userVotes).doesNotContainKey(recipeId3);

            verify(voteRepository).findByUserIdAndRecipeIdIn(eq(testUserId), eq(recipeIds));
            verifyNoMoreInteractions(voteRepository);
            verifyNoInteractions(recipeRepository);
        }

        @Test
        @DisplayName("Should return empty map for empty or null recipe ID set")
        void getUserVotesForRecipes_EmptyOrNullInput_ReturnsEmptyMap() {
            Map<UUID, RecipeVote.VoteType> userVotesEmpty = voteService.getUserVotesForRecipes(testUserId, Collections.emptySet());

            assertThat(userVotesEmpty).isNotNull();
            assertThat(userVotesEmpty).isEmpty();

            Map<UUID, RecipeVote.VoteType> userVotesNull = voteService.getUserVotesForRecipes(testUserId, null);

            assertThat(userVotesNull).isNotNull();
            assertThat(userVotesNull).isEmpty();

            verify(voteRepository, never()).findByUserIdAndRecipeIdIn(any(), any());
            verifyNoInteractions(recipeRepository);
        }
    }
}
