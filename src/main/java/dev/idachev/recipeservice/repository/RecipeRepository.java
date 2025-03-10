package dev.idachev.recipeservice.repository;

import dev.idachev.recipeservice.model.Recipe;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Repository interface for Recipe entities.
 */
@Repository
public interface RecipeRepository extends JpaRepository<Recipe, UUID> {

    /**
     * Find recipes created by a specific user.
     *
     * @param userId the ID of the user who created the recipes
     * @return a list of recipes created by the specified user
     */
    List<Recipe> findByUserId(UUID userId);

    /**
     * Find recipes by title containing the given keyword, case insensitive.
     *
     * @param keyword the keyword to search for in recipe titles
     * @return a list of recipes with titles containing the keyword
     */
    List<Recipe> findByTitleContainingIgnoreCase(String keyword);

    /**
     * Search for recipes by title or description containing the given keyword, with pagination.
     *
     * @param title       the keyword to search for in titles
     * @param description the keyword to search for in descriptions
     * @param pageable    pagination information
     * @return a page of recipes matching the search criteria
     */
    Page<Recipe> findByTitleContainingIgnoreCaseOrDescriptionContainingIgnoreCase(
            String title, String description, Pageable pageable);
} 