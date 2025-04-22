package dev.idachev.recipeservice.service;

// Necessary imports (similar to the previous attempt)
import dev.idachev.recipeservice.model.FavoriteRecipe;
import dev.idachev.recipeservice.model.RecipeVote;
import dev.idachev.recipeservice.repository.FavoriteRecipeRepository;
import dev.idachev.recipeservice.service.CommentService;
import dev.idachev.recipeservice.service.VoteService;
import dev.idachev.recipeservice.user.service.UserService;
import dev.idachev.recipeservice.web.dto.RecipeResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Component responsible for enhancing RecipeResponse DTOs with user-specific interaction data.
 * Used to break circular dependency between RecipeService and RecipeSearchService.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class RecipeResponseEnhancer {

    private final FavoriteRecipeRepository favoriteRecipeRepository;
    private final VoteService voteService;
    private final CommentService commentService;
    private final UserService userService;

    /**
     * Enhance a list of recipe responses with user interactions using bulk fetching.
     * Fetches favorites, votes, comments, and author names efficiently.
     *
     * @param responses The list of base RecipeResponse DTOs to enhance.
     * @param userId    The ID of the current user for whom to fetch interaction data (can be null).
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
        Map<UUID, Boolean> userFavorites = (userId != null) ?
            favoriteRecipeRepository.getUserFavoritesMap(userId, recipeIds) : Collections.emptyMap();
        Map<UUID, RecipeVote.VoteType> userVotes = (userId != null) ?
            voteService.getUserVotesForRecipes(userId, recipeIds) : Collections.emptyMap();
        Map<UUID, Long> commentCounts = commentService.getCommentCountsForRecipes(recipeIds);
        // Placeholder bulk fetch used here - replace when UserService implements the real bulk method
        Map<UUID, String> authorUsernames = userService.getUsernamesByIds(authorIds);

        // Enhance each response using the bulk-fetched data
        return responses.stream()
                .map(response -> {
                    UUID recipeId = response.id();
                    UUID createdById = response.createdById();

                    long favCount = favoriteCounts.getOrDefault(recipeId, 0L);
                    boolean isFav = userFavorites.getOrDefault(recipeId, false);
                    RecipeVote.VoteType voteType = userVotes.get(recipeId);
                    String userVoteStr = (voteType != null) ? voteType.name() : null; // Use .name() for enum string
                    long commCount = commentCounts.getOrDefault(recipeId, 0L);
                    String authorUsername = (createdById != null) ? authorUsernames.getOrDefault(createdById, "Unknown User") : "Unknown User"; // Handle null from map
                    String authorName = authorUsername; // Use username as author name for now
                    String authorIdStr = (createdById != null) ? createdById.toString() : null;

                    // Construct NEW RecipeResponse with enhanced data
                    return new RecipeResponse(
                        response.id(), response.createdById(), response.title(), response.servingSuggestions(),
                        response.instructions(), response.imageUrl(), response.ingredients(), response.totalTimeMinutes(),
                        authorName, authorUsername, authorIdStr, // Enhanced
                        response.difficulty(), response.isAiGenerated(),
                        isFav, favCount, commCount, // Enhanced
                        response.upvotes(), response.downvotes(),
                        userVoteStr, // Enhanced
                        response.createdAt(), response.updatedAt(), response.macros(), response.additionalFields()
                    );
                })
                .toList();
    }
} 