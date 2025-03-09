package dev.idachev.recipeservice.web;

import dev.idachev.recipeservice.service.FavoriteRecipeService;
import dev.idachev.recipeservice.web.dto.FavoriteRecipeDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Controller for managing user's favorite recipes.
 */
@RestController
@RequestMapping("/api/v1/favorites")
@RequiredArgsConstructor
@Slf4j
public class FavoriteRecipeController {

    private final FavoriteRecipeService favoriteRecipeService;

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
     * Add a recipe to user's favorites.
     *
     * @param recipeId the ID of the recipe to add to favorites
     * @return the added favorite recipe
     */
    @PostMapping("/{recipeId}")
    public ResponseEntity<FavoriteRecipeDto> addToFavorites(@PathVariable UUID recipeId) {
        UUID userId = getCurrentUserId();
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return ResponseEntity.ok(favoriteRecipeService.addToFavorites(userId, recipeId));
    }

    /**
     * Remove a recipe from user's favorites.
     *
     * @param recipeId the ID of the recipe to remove from favorites
     * @return no content
     */
    @DeleteMapping("/{recipeId}")
    public ResponseEntity<Void> removeFromFavorites(@PathVariable UUID recipeId) {
        UUID userId = getCurrentUserId();
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        favoriteRecipeService.removeFromFavorites(userId, recipeId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Get user's favorite recipes with pagination.
     *
     * @param pageable pagination parameters
     * @return a page of favorite recipes
     */
    @GetMapping
    public ResponseEntity<Page<FavoriteRecipeDto>> getUserFavorites(Pageable pageable) {
        UUID userId = getCurrentUserId();
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return ResponseEntity.ok(favoriteRecipeService.getUserFavorites(userId, pageable));
    }

    /**
     * Get all user's favorite recipes.
     *
     * @return a list of all favorite recipes for the current user
     */
    @GetMapping("/all")
    public ResponseEntity<List<FavoriteRecipeDto>> getAllUserFavorites() {
        UUID userId = getCurrentUserId();
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return ResponseEntity.ok(favoriteRecipeService.getAllUserFavorites(userId));
    }

    /**
     * Check if a recipe is in user's favorites.
     *
     * @param recipeId the ID of the recipe to check
     * @return true if the recipe is in user's favorites, false otherwise
     */
    @GetMapping("/check/{recipeId}")
    public ResponseEntity<Boolean> isRecipeInFavorites(@PathVariable UUID recipeId) {
        UUID userId = getCurrentUserId();
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return ResponseEntity.ok(favoriteRecipeService.isRecipeInFavorites(userId, recipeId));
    }

    /**
     * Get the number of users who have favorited a recipe.
     *
     * @param recipeId the ID of the recipe
     * @return the number of users who have favorited the recipe
     */
    @GetMapping("/count/{recipeId}")
    public ResponseEntity<Long> getFavoriteCount(@PathVariable UUID recipeId) {
        return ResponseEntity.ok(favoriteRecipeService.getFavoriteCount(recipeId));
    }
} 