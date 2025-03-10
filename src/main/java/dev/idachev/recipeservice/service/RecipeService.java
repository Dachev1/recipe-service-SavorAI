package dev.idachev.recipeservice.service;

import dev.idachev.recipeservice.infrastructure.ai.AIService;
import dev.idachev.recipeservice.web.dto.GeneratedMealResponse;
import dev.idachev.recipeservice.web.dto.RecipeRequest;
import dev.idachev.recipeservice.web.dto.RecipeResponse;
import dev.idachev.recipeservice.exception.ResourceNotFoundException;
import dev.idachev.recipeservice.exception.UnauthorizedAccessException;
import dev.idachev.recipeservice.mapper.RecipeMapper;
import dev.idachev.recipeservice.model.Recipe;
import dev.idachev.recipeservice.repository.FavoriteRecipeRepository;
import dev.idachev.recipeservice.repository.RecipeRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service for recipe operations.
 */
@Service
@Slf4j
public class RecipeService {

    private final RecipeRepository recipeRepository;
    private final AIService aiService;
    private final FavoriteRecipeRepository favoriteRecipeRepository;

    @Autowired
    public RecipeService(RecipeRepository recipeRepository, 
                        AIService aiService,
                        FavoriteRecipeRepository favoriteRecipeRepository) {
        this.recipeRepository = recipeRepository;
        this.aiService = aiService;
        this.favoriteRecipeRepository = favoriteRecipeRepository;
    }

    /**
     * Enhance a recipe response with favorite information.
     *
     * @param response the recipe response to enhance
     * @param userId   the ID of the current user, or null if not authenticated
     * @return the enhanced recipe response
     */
    private RecipeResponse enhanceWithFavoriteInfo(RecipeResponse response, UUID userId) {
        if (response != null) {
            // Set favorite count
            response.setFavoriteCount(favoriteRecipeRepository.countByRecipeId(response.getId()));
            
            // Set isFavorite flag if user is authenticated
            if (userId != null) {
                response.setIsFavorite(favoriteRecipeRepository.existsByUserIdAndRecipeId(userId, response.getId()));
            } else {
                response.setIsFavorite(false);
            }
        }
        return response;
    }

    /**
     * Create a new recipe.
     *
     * @param request the recipe request
     * @param userId  the ID of the user creating the recipe
     * @return the created recipe response
     */
    @Transactional
    public RecipeResponse createRecipe(RecipeRequest request, UUID userId) {
        Recipe recipe = RecipeMapper.toEntity(request);
        recipe.setUserId(userId);
        
        Recipe savedRecipe = recipeRepository.save(recipe);
        log.info("Created recipe with ID: {}", savedRecipe.getId());
        
        RecipeResponse response = RecipeMapper.toResponse(savedRecipe);
        return enhanceWithFavoriteInfo(response, userId);
    }

    /**
     * Get a recipe by ID.
     *
     * @param id     the ID of the recipe
     * @param userId the ID of the current user, or null if not authenticated
     * @return the recipe response
     */
    @Transactional(readOnly = true)
    public RecipeResponse getRecipeById(UUID id, UUID userId) {
        Recipe recipe = recipeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Recipe not found with id: " + id));
        
        RecipeResponse response = RecipeMapper.toResponse(recipe);
        return enhanceWithFavoriteInfo(response, userId);
    }
    
    /**
     * Get a recipe by ID without user context.
     *
     * @param id the ID of the recipe
     * @return the recipe response
     */
    @Transactional(readOnly = true)
    public RecipeResponse getRecipeById(UUID id) {
        return getRecipeById(id, null);
    }

    /**
     * Get all recipes with pagination and favorite information for a specific user.
     *
     * @param pageable the pagination information
     * @param userId   the ID of the current user, or null if not authenticated
     * @return a page of recipe responses
     */
    @Transactional(readOnly = true)
    public Page<RecipeResponse> getAllRecipes(Pageable pageable, UUID userId) {
        Page<Recipe> recipePage = recipeRepository.findAll(pageable);
        
        List<RecipeResponse> responses = recipePage.getContent().stream()
                .map(RecipeMapper::toResponse)
                .map(response -> enhanceWithFavoriteInfo(response, userId))
                .collect(Collectors.toList());
        
        return new PageImpl<>(responses, pageable, recipePage.getTotalElements());
    }
    
    /**
     * Get all recipes with pagination without user context.
     *
     * @param pageable the pagination information
     * @return a page of recipe responses
     */
    @Transactional(readOnly = true)
    public Page<RecipeResponse> getAllRecipes(Pageable pageable) {
        return getAllRecipes(pageable, null);
    }

    /**
     * Get recipes by user ID.
     *
     * @param userId the ID of the user who created the recipes
     * @return a list of recipe responses
     */
    @Transactional(readOnly = true)
    public List<RecipeResponse> getRecipesByUserId(UUID userId) {
        List<Recipe> recipes = recipeRepository.findByUserId(userId);
        
        return recipes.stream()
                .map(RecipeMapper::toResponse)
                .map(response -> enhanceWithFavoriteInfo(response, userId))
                .collect(Collectors.toList());
    }

    /**
     * Update a recipe.
     *
     * @param id      the ID of the recipe to update
     * @param request the updated recipe data
     * @param userId  the ID of the user updating the recipe
     * @return the updated recipe response
     */
    @Transactional
    public RecipeResponse updateRecipe(UUID id, RecipeRequest request, UUID userId) {
        Recipe recipe = recipeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Recipe not found with id: " + id));
        
        // Check if the user is the owner of the recipe
        if (!recipe.getUserId().equals(userId)) {
            throw new UnauthorizedAccessException("You are not authorized to update this recipe");
        }
        
        RecipeMapper.updateEntityFromRequest(recipe, request);
        Recipe updatedRecipe = recipeRepository.save(recipe);
        log.info("Updated recipe with ID: {}", updatedRecipe.getId());
        
        RecipeResponse response = RecipeMapper.toResponse(updatedRecipe);
        return enhanceWithFavoriteInfo(response, userId);
    }

    /**
     * Delete a recipe.
     *
     * @param id     the ID of the recipe to delete
     * @param userId the ID of the user deleting the recipe
     */
    @Transactional
    public void deleteRecipe(UUID id, UUID userId) {
        Recipe recipe = recipeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Recipe not found with id: " + id));
        
        // Check if the user is the owner of the recipe
        if (!recipe.getUserId().equals(userId)) {
            throw new UnauthorizedAccessException("You are not authorized to delete this recipe");
        }
        
        recipeRepository.delete(recipe);
        log.info("Deleted recipe with ID: {}", id);
    }

    /**
     * Search recipes by keyword.
     *
     * @param keyword  the keyword to search for
     * @param pageable the pagination information
     * @param userId   the ID of the current user, or null if not authenticated
     * @return a page of recipe responses matching the search criteria
     */
    @Transactional(readOnly = true)
    public Page<RecipeResponse> searchRecipes(String keyword, Pageable pageable, UUID userId) {
        Page<Recipe> recipePage = recipeRepository.findByTitleContainingIgnoreCaseOrDescriptionContainingIgnoreCase(
                keyword, keyword, pageable);
        
        List<RecipeResponse> responses = recipePage.getContent().stream()
                .map(RecipeMapper::toResponse)
                .map(response -> enhanceWithFavoriteInfo(response, userId))
                .collect(Collectors.toList());
        
        return new PageImpl<>(responses, pageable, recipePage.getTotalElements());
    }
    
    /**
     * Search recipes by keyword without user context.
     *
     * @param keyword  the keyword to search for
     * @param pageable the pagination information
     * @return a page of recipe responses matching the search criteria
     */
    @Transactional(readOnly = true)
    public Page<RecipeResponse> searchRecipes(String keyword, Pageable pageable) {
        return searchRecipes(keyword, pageable, null);
    }

    /**
     * Filter recipes method (renamed from filterRecipesByTags).
     * Now just returns all recipes since tags functionality is removed.
     *
     * @param filters  unused parameter, kept for API compatibility
     * @param pageable the pagination information
     * @param userId   the ID of the current user, or null if not authenticated
     * @return a page of recipe responses
     */
    @Transactional(readOnly = true)
    public Page<RecipeResponse> filterRecipesByTags(List<String> filters, Pageable pageable, UUID userId) {
        return getAllRecipes(pageable, userId);
    }
    
    /**
     * Filter recipes without user context (renamed from filterRecipesByTags).
     * Now just returns all recipes since tags functionality is removed.
     *
     * @param filters  unused parameter, kept for API compatibility
     * @param pageable the pagination information
     * @return a page of recipe responses
     */
    @Transactional(readOnly = true)
    public Page<RecipeResponse> filterRecipesByTags(List<String> filters, Pageable pageable) {
        return filterRecipesByTags(filters, pageable, null);
    }

    /**
     * Upload a recipe image.
     *
     * @param file the image file to upload
     * @return the URL of the uploaded image
     */
    public String uploadImage(MultipartFile file) {
        // Implementation for image upload
        // This would typically involve storing the file and returning its URL
        log.info("Uploading image: {}", file.getOriginalFilename());
        return "https://example.com/images/" + file.getOriginalFilename(); // Placeholder
    }

    /**
     * Generate a meal based on provided ingredients.
     *
     * @param ingredients List of ingredients to use
     * @return Generated meal response
     */
    public GeneratedMealResponse generateMeal(List<String> ingredients) {
        return aiService.generateRecipeFromIngredients(ingredients);
    }
} 