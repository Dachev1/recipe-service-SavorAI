package dev.idachev.recipeservice.service;

import dev.idachev.recipeservice.model.Recipe;
import dev.idachev.recipeservice.repository.RecipeRepository;
import dev.idachev.recipeservice.web.dto.RecipeResponse;
import dev.idachev.recipeservice.web.mapper.RecipeMapper;
import dev.idachev.recipeservice.service.RecipeResponseEnhancer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service for recipe search operations.
 * Responsible for all search-related functionality.
 */
@Service
@Slf4j
public class RecipeSearchService {

    private final RecipeRepository recipeRepository;
    private final RecipeMapper recipeMapper;
    private final RecipeResponseEnhancer recipeResponseEnhancer;

    @Autowired
    public RecipeSearchService(RecipeRepository recipeRepository,
                               RecipeMapper recipeMapper,
                               RecipeResponseEnhancer recipeResponseEnhancer) {
        this.recipeRepository = recipeRepository;
        this.recipeMapper = recipeMapper;
        this.recipeResponseEnhancer = recipeResponseEnhancer;
    }

    /**
     * Search recipes by keyword.
     *
     * @param keyword  Search term
     * @param pageable Pagination information
     * @param userId   Optional user ID for favorite information
     * @return Page of matching recipes
     */
    @Transactional(readOnly = true)
    public Page<RecipeResponse> searchRecipes(String keyword, Pageable pageable, UUID userId) {
        Page<Recipe> recipePage;
        if (!StringUtils.hasText(keyword)) {
            log.debug("Empty search keyword, returning all recipes");
            recipePage = recipeRepository.findAll(pageable);
        } else {
            log.debug("Searching recipes with keyword: {}", keyword);
            String trimmedKeyword = keyword.trim();
            recipePage = recipeRepository.findByTitleContainingIgnoreCaseOrServingSuggestionsContainingIgnoreCase(
                    trimmedKeyword, trimmedKeyword, pageable);
        }
        log.debug("Found {} recipes matching keyword/criteria", recipePage.getTotalElements());

        // Map to base response
        List<RecipeResponse> baseResponses = recipePage.getContent().stream()
                                                .map(recipeMapper::toResponse)
                                                .toList();
        // Use the injected enhancer
        List<RecipeResponse> enhancedResponses = recipeResponseEnhancer.enhanceRecipeListWithUserInteractions(baseResponses, userId);
        
        return new PageImpl<>(enhancedResponses, pageable, recipePage.getTotalElements());
    }

    /**
     * Get all recipes with pagination.
     *
     * @param pageable Pagination information
     * @param userId   Optional user ID for favorite information
     * @return Page of recipes
     */
    @Transactional(readOnly = true)
    public Page<RecipeResponse> getAllRecipes(Pageable pageable, UUID userId) {
        log.debug("Fetching all recipes with pagination: {}", pageable);
        Page<Recipe> recipePage = recipeRepository.findAll(pageable);
        log.debug("Found {} total recipes", recipePage.getTotalElements());

        List<RecipeResponse> baseResponses = recipePage.getContent().stream()
                                                .map(recipeMapper::toResponse)
                                                .toList();
        // Use the injected enhancer
        List<RecipeResponse> enhancedResponses = recipeResponseEnhancer.enhanceRecipeListWithUserInteractions(baseResponses, userId);
        
        return new PageImpl<>(enhancedResponses, pageable, recipePage.getTotalElements());
    }

    /**
     * Filter recipes by tags.
     * Finds recipes containing ALL specified tags.
     *
     * @param filters  List of tag filters
     * @param pageable Pagination information
     * @param userId   Optional user ID for favorite information
     * @return Page of filtered recipes
     */
    @Transactional(readOnly = true)
    public Page<RecipeResponse> filterRecipesByTags(List<String> filters, Pageable pageable, UUID userId) {
        if (filters == null || filters.isEmpty()) {
            log.debug("No filter tags provided, returning all recipes");
            // If no filters, delegate to getAllRecipes for consistent enhancement
            return getAllRecipes(pageable, userId);
        } else {
            // Clean up tags (trim, lowercase, distinct)
            List<String> cleanedFilters = filters.stream()
                                                 .filter(StringUtils::hasText)
                                                 .map(String::trim)
                                                 .map(String::toLowerCase) // Assuming tags are stored/queried lowercase
                                                 .distinct()
                                                 .toList();
            
            if (cleanedFilters.isEmpty()) {
                log.debug("Filter tags were blank, returning all recipes");
                return getAllRecipes(pageable, userId);
            }

            log.debug("Filtering recipes containing all tags: {}", cleanedFilters);
            // Use the repository method that finds recipes containing ALL tags
            Page<Recipe> recipePage = recipeRepository.findByTagsContainingAll(
                                             cleanedFilters, 
                                             (long) cleanedFilters.size(), // Pass the count of distinct tags
                                             pageable
                                         );
            log.debug("Found {} recipes matching all tags: {}", recipePage.getTotalElements(), cleanedFilters);

            List<RecipeResponse> baseResponses = recipePage.getContent().stream()
                                                    .map(recipeMapper::toResponse)
                                                    .toList();
            // Use the enhancer for consistency
            List<RecipeResponse> enhancedResponses = recipeResponseEnhancer.enhanceRecipeListWithUserInteractions(baseResponses, userId);
            
            return new PageImpl<>(enhancedResponses, pageable, recipePage.getTotalElements());
        }
    }

    /**
     * Get all recipes excluding those created by a specific user.
     */
    @Transactional(readOnly = true)
    public Page<RecipeResponse> getAllRecipesExcludingUser(Pageable pageable, UUID userId) {
        log.debug("Fetching recipes excluding user {} with pagination: {}", userId, pageable);
        Page<Recipe> recipePage = recipeRepository.findByUserIdNot(userId, pageable);
        log.debug("Found {} recipes not created by user {}", recipePage.getTotalElements(), userId);
        
        List<RecipeResponse> baseResponses = recipePage.getContent().stream()
                                                .map(recipeMapper::toResponse)
                                                .toList();
        // Use the injected enhancer
        List<RecipeResponse> enhancedResponses = recipeResponseEnhancer.enhanceRecipeListWithUserInteractions(baseResponses, userId);

        return new PageImpl<>(enhancedResponses, pageable, recipePage.getTotalElements());
    }
} 