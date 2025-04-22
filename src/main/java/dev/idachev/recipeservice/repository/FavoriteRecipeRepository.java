package dev.idachev.recipeservice.repository;

import dev.idachev.recipeservice.model.FavoriteRecipe;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Repository
public interface FavoriteRecipeRepository extends JpaRepository<FavoriteRecipe, UUID> {

    Page<FavoriteRecipe> findByUserId(UUID userId, Pageable pageable);

    List<FavoriteRecipe> findByUserId(UUID userId);

    List<FavoriteRecipe> findByRecipeId(UUID recipeId);

    boolean existsByUserIdAndRecipeId(UUID userId, UUID recipeId);

    long countByRecipeId(UUID recipeId);

    Optional<FavoriteRecipe> findByUserIdAndRecipeId(UUID userId, UUID recipeId);

    void deleteByUserIdAndRecipeId(UUID userId, UUID recipeId);

    // TODO: Verify intended usage. If Recipe entity uses cascade delete for favorites,
    // this method might be redundant. It IS used by RecipeService.deleteRecipe currently.
    // Ensure cascade strategy aligns with this manual deletion.
    void deleteByRecipeId(UUID recipeId);

    /**
     * Counts favorites for a set of recipe IDs efficiently.
     * @param recipeIds Set of recipe IDs.
     * @return Map of Recipe ID to favorite count.
     */
    @Query("""
        SELECT fr.recipeId as recipeId, COUNT(fr) as favoriteCount
        FROM FavoriteRecipe fr
        WHERE fr.recipeId IN :recipeIds
        GROUP BY fr.recipeId
        """)
    List<RecipeFavoriteCount> countFavoritesByRecipeIds(@Param("recipeIds") Set<UUID> recipeIds);

    // Helper projection interface for the count query
    interface RecipeFavoriteCount {
        UUID getRecipeId();
        long getFavoriteCount();
    }

    // Convenience default method to convert the projection list to a map
    default Map<UUID, Long> getFavoriteCountsMap(Set<UUID> recipeIds) {
        if (recipeIds == null || recipeIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return countFavoritesByRecipeIds(recipeIds).stream()
                .collect(Collectors.toMap(RecipeFavoriteCount::getRecipeId, RecipeFavoriteCount::getFavoriteCount));
    }

    /**
     * Finds which recipes from a given set are favorited by a specific user.
     * @param userId The user ID.
     * @param recipeIds Set of recipe IDs to check.
     * @return Set of Recipe IDs that are favorited by the user within the given set.
     */
    @Query("""
        SELECT fr.recipeId
        FROM FavoriteRecipe fr
        WHERE fr.userId = :userId AND fr.recipeId IN :recipeIds
        """)
    Set<UUID> findUserFavoriteRecipeIds(@Param("userId") UUID userId, @Param("recipeIds") Set<UUID> recipeIds);

    // Convenience default method to convert the set of favorited IDs to a map
    default Map<UUID, Boolean> getUserFavoritesMap(UUID userId, Set<UUID> recipeIds) {
        if (userId == null || recipeIds == null || recipeIds.isEmpty()) {
            return Collections.emptyMap();
        }
        Set<UUID> favoritedIds = findUserFavoriteRecipeIds(userId, recipeIds);
        return recipeIds.stream()
                .collect(Collectors.toMap(Function.identity(), favoritedIds::contains));
    }
} 