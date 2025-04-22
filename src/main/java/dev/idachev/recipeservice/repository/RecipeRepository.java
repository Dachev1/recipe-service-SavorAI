package dev.idachev.recipeservice.repository;

import dev.idachev.recipeservice.model.Recipe;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Set;
import java.util.UUID;

@Repository
public interface RecipeRepository extends JpaRepository<Recipe, UUID> {

    List<Recipe> findByUserId(UUID userId);

    Page<Recipe> findByUserIdNot(UUID userId, Pageable pageable);

    Page<Recipe> findByTitleContainingIgnoreCaseOrServingSuggestionsContainingIgnoreCase(
            String title, String servingSuggestions, Pageable pageable);

    @Query("SELECT r FROM Recipe r JOIN r.tags t WHERE t IN :tags GROUP BY r HAVING COUNT(DISTINCT t) = :tagCount")
    Page<Recipe> findByTagsContainingAll(@Param("tags") List<String> tags, @Param("tagCount") long tagCount, Pageable pageable);
} 