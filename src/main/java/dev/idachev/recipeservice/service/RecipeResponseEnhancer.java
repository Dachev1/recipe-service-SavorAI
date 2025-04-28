package dev.idachev.recipeservice.service;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

// Removed: import dev.idachev.recipeservice.model.FavoriteRecipe;
import dev.idachev.recipeservice.model.RecipeVote;
import dev.idachev.recipeservice.repository.FavoriteRecipeRepository;
import dev.idachev.recipeservice.user.service.UserService;
import dev.idachev.recipeservice.web.dto.RecipeResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Enhances RecipeResponse DTOs with user-specific interaction data (favorites,
 * votes)
 * and counts (comments, favorites) using efficient bulk fetching.
 */
@Component
@RequiredArgsConstructor // This handles constructor injection for all final fields
@Slf4j
public class RecipeResponseEnhancer {

    private final FavoriteRecipeRepository favoriteRecipeRepository;
    private final VoteService voteService; // Add back
    private final CommentService commentService; // Add back
    private final UserService userService; // Add back

    /**
     * Enhance a list of recipe responses with user interactions using bulk
     * fetching.
     * Fetches favorites, votes, comments, and author names efficiently.
     *
     * @param responses The list of base RecipeResponse DTOs to enhance.
     * @param userId    The ID of the current user for whom to fetch interaction
     *                  data (can be null).
     * @return The list of enhanced RecipeResponse DTOs.
     */
    @Transactional(readOnly = true) // Read-only transaction as we only fetch data
    public List<RecipeResponse> enhanceRecipeListWithUserInteractions(List<RecipeResponse> responses, UUID userId) {
        if (responses == null || responses.isEmpty()) {
            return Collections.emptyList();
        }

        // Collect Recipe IDs and User IDs for bulk fetching
        Set<UUID> recipeIds = responses.stream().map(RecipeResponse::id).collect(Collectors.toSet());
        Set<UUID> authorIds = responses.stream()
                .map(RecipeResponse::createdById)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        // Bulk fetch data
        Map<UUID, Long> favoriteCounts = favoriteRecipeRepository.getFavoriteCountsMap(recipeIds);
        Map<UUID, Boolean> userFavorites = (userId != null)
                ? favoriteRecipeRepository.getUserFavoritesMap(userId, recipeIds)
                : Collections.emptyMap();
        Map<UUID, RecipeVote.VoteType> userVotes = (userId != null)
                ? voteService.getUserVotesForRecipes(userId, recipeIds)
                : Collections.emptyMap(); // Use voteService
        Map<UUID, Long> commentCounts = commentService.getCommentCountsForRecipes(recipeIds); // Use commentService
        Map<UUID, String> authorUsernames = userService.getUsernamesByIds(authorIds); // Use userService

        // Enhance each response using the bulk-fetched data
        return responses.stream()
                .map(response -> {
                    UUID recipeId = response.id();
                    UUID createdById = response.createdById();

                    long favCount = favoriteCounts.getOrDefault(recipeId, 0L);
                    boolean isFav = userFavorites.getOrDefault(recipeId, false);
                    RecipeVote.VoteType voteType = userVotes.get(recipeId);
                    String userVoteStr = (voteType != null) ? voteType.name() : null;
                    long commCount = commentCounts.getOrDefault(recipeId, 0L);
                    String authorUsername = (createdById != null)
                            ? authorUsernames.getOrDefault(createdById, "Unknown User")
                            : "Unknown User";
                    String authorName = authorUsername;
                    String authorIdStr = (createdById != null) ? createdById.toString() : null;

                    return new RecipeResponse(
                            response.id(), response.createdById(), response.title(), response.servingSuggestions(),
                            response.instructions(), response.imageUrl(), response.ingredients(),
                            response.totalTimeMinutes(),
                            authorName, authorUsername, authorIdStr,
                            response.difficulty(), response.isAiGenerated(),
                            isFav, favCount, commCount,
                            response.upvotes(), response.downvotes(),
                            userVoteStr,
                            response.createdAt(), response.updatedAt(), response.macros(), response.additionalFields());
                })
                .toList();
    }
}