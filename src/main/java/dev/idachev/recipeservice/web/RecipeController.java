package dev.idachev.recipeservice.web;

import dev.idachev.recipeservice.web.dto.GeneratedMealResponse;
import dev.idachev.recipeservice.web.dto.RecipeRequest;
import dev.idachev.recipeservice.web.dto.RecipeResponse;
import dev.idachev.recipeservice.service.RecipeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

/**
 * Controller for recipe management operations.
 */
@RestController
@RequestMapping("/api/v1/recipes")
@RequiredArgsConstructor
@Slf4j
public class RecipeController {

    private final RecipeService recipeService;

    /**
     * Get the current user ID from the security context.
     *
     * @return the current user ID, or null if not authenticated
     */
    private UUID getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated() && 
                !"anonymousUser".equals(authentication.getPrincipal())) {
            try {
                return UUID.fromString(authentication.getPrincipal().toString());
            } catch (IllegalArgumentException e) {
                log.warn("Failed to parse user ID from authentication: {}", e.getMessage());
                return null;
            }
        }
        return null;
    }

    /**
     * Create a new recipe.
     *
     * @param request the recipe data
     * @return the created recipe
     */
    @PostMapping
    public ResponseEntity<RecipeResponse> createRecipe(@Valid @RequestBody RecipeRequest request) {
        UUID userId = getCurrentUserId();
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(recipeService.createRecipe(request, userId));
    }

    /**
     * Get a recipe by ID.
     *
     * @param id the recipe ID
     * @return the recipe data
     */
    @GetMapping("/{id}")
    public ResponseEntity<RecipeResponse> getRecipeById(@PathVariable UUID id) {
        UUID userId = getCurrentUserId();
        return ResponseEntity.ok(recipeService.getRecipeById(id, userId));
    }

    /**
     * Get all recipes with pagination.
     *
     * @param pageable pagination parameters
     * @return a page of recipes
     */
    @GetMapping
    public ResponseEntity<Page<RecipeResponse>> getAllRecipes(Pageable pageable) {
        UUID userId = getCurrentUserId();
        return ResponseEntity.ok(recipeService.getAllRecipes(pageable, userId));
    }

    /**
     * Get recipes created by the current user.
     *
     * @return a list of recipes created by the current user
     */
    @GetMapping("/user")
    public ResponseEntity<List<RecipeResponse>> getRecipesByUserId() {
        UUID userId = getCurrentUserId();
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return ResponseEntity.ok(recipeService.getRecipesByUserId(userId));
    }

    /**
     * Update a recipe.
     *
     * @param id the recipe ID
     * @param request the updated recipe data
     * @return the updated recipe
     */
    @PutMapping("/{id}")
    public ResponseEntity<RecipeResponse> updateRecipe(
            @PathVariable UUID id,
            @Valid @RequestBody RecipeRequest request) {
        UUID userId = getCurrentUserId();
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return ResponseEntity.ok(recipeService.updateRecipe(id, request, userId));
    }

    /**
     * Delete a recipe.
     *
     * @param id the recipe ID
     * @return no content
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteRecipe(@PathVariable UUID id) {
        UUID userId = getCurrentUserId();
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        recipeService.deleteRecipe(id, userId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Search for recipes by keyword.
     *
     * @param keyword the search keyword
     * @param pageable pagination parameters
     * @return a page of recipes matching the search
     */
    @GetMapping("/search")
    public ResponseEntity<Page<RecipeResponse>> searchRecipes(
            @RequestParam String keyword,
            Pageable pageable) {
        UUID userId = getCurrentUserId();
        return ResponseEntity.ok(recipeService.searchRecipes(keyword, pageable, userId));
    }

    /**
     * Filter recipes by tags.
     *
     * @param tags the list of tags to filter by
     * @param pageable pagination parameters
     * @return a page of recipes with the specified tags
     */
    @GetMapping("/filter")
    public ResponseEntity<Page<RecipeResponse>> filterRecipesByTags(
            @RequestParam List<String> tags,
            Pageable pageable) {
        UUID userId = getCurrentUserId();
        return ResponseEntity.ok(recipeService.filterRecipesByTags(tags, pageable, userId));
    }

    /**
     * Upload a recipe image.
     *
     * @param file the image file
     * @return the URL of the uploaded image
     */
    @PostMapping(value = "/upload-image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<String> uploadImage(@RequestParam("file") MultipartFile file) {
        UUID userId = getCurrentUserId();
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return ResponseEntity.ok(recipeService.uploadImage(file));
    }

    /**
     * Generate a meal based on ingredients.
     *
     * @param ingredients the list of ingredients
     * @return the generated meal recipe
     */
    @PostMapping("/generate-meal")
    public ResponseEntity<GeneratedMealResponse> generateMeal(@RequestBody List<String> ingredients) {
        UUID userId = getCurrentUserId();
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return ResponseEntity.ok(recipeService.generateMeal(ingredients));
    }
} 