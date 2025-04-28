package dev.idachev.recipeservice.repository;

import dev.idachev.recipeservice.model.FavoriteRecipe;
import dev.idachev.recipeservice.repository.dto.RecipeFavoriteCountDto;
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

    // Used by RecipeService.deleteRecipe. Ensure cascade strategy aligns if Recipe entity changes.
    void deleteByRecipeId(UUID recipeId);

    /**
     * Counts favorites for a set of recipe IDs efficiently.
     * @param recipeIds Set of recipe IDs.
     * @return Map of Recipe ID to favorite count.
     */
    @Query("""
        SELECT NEW dev.idachev.recipeservice.repository.dto.RecipeFavoriteCountDto(fr.recipeId, COUNT(fr))
        FROM FavoriteRecipe fr
        WHERE fr.recipeId IN :recipeIds
        GROUP BY fr.recipeId
        """)
    List<RecipeFavoriteCountDto> countFavoritesByRecipeIds(@Param("recipeIds") Set<UUID> recipeIds);

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

    // Convenience default method to convert the projection list to a map
    default Map<UUID, Long> getFavoriteCountsMap(Set<UUID> recipeIds) {
        if (recipeIds == null || recipeIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return countFavoritesByRecipeIds(recipeIds).stream()
                .collect(Collectors.toMap(RecipeFavoriteCountDto::getRecipeId, RecipeFavoriteCountDto::getFavoriteCount));
    }
} 