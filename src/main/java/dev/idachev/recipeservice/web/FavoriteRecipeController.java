package dev.idachev.recipeservice.web;

import dev.idachev.recipeservice.service.FavoriteRecipeService;
import dev.idachev.recipeservice.web.dto.BatchFavoriteCountResponse;
import dev.idachev.recipeservice.web.dto.BatchFavoriteRequest;
import dev.idachev.recipeservice.web.dto.BatchFavoriteStatusResponse;
import dev.idachev.recipeservice.web.dto.FavoriteCountResponse;
import dev.idachev.recipeservice.web.dto.FavoriteRecipeDto;
import dev.idachev.recipeservice.web.dto.IsFavoriteResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Controller for managing favorite recipes.
 * Follows RESTful principles for HTTP methods and status codes.
 * All exceptions are handled by the GlobalExceptionHandler.
 */
@RestController
@RequestMapping("/api/v1/favorites")
@Slf4j
@Tag(name = "Favorites", description = "API for managing favorite recipes")
public class FavoriteRecipeController {

    private final FavoriteRecipeService favoriteRecipeService;

    @Autowired
    public FavoriteRecipeController(FavoriteRecipeService favoriteRecipeService) {
        this.favoriteRecipeService = favoriteRecipeService;
    }

    @Operation(summary = "Add recipe to favorites", description = "Adds a recipe to the current user's favorites")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Recipe added to favorites",
                    content = @Content(schema = @Schema(implementation = FavoriteRecipeDto.class))),
            @ApiResponse(responseCode = "404", description = "Recipe not found"),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @PostMapping("/{recipeId}")
    public ResponseEntity<FavoriteRecipeDto> addToFavorites(
            @Parameter(description = "ID of the recipe to add to favorites")
            @PathVariable UUID recipeId,
            @Parameter(hidden = true) @AuthenticationPrincipal UUID userId) {
        log.debug("Entering addToFavorites: recipeId={}, userId={}", recipeId, userId);
        FavoriteRecipeDto favoriteDto = favoriteRecipeService.addToFavorites(userId, recipeId);
        log.debug("Exiting addToFavorites: recipeId={}, userId={}", recipeId, userId);
        return ResponseEntity.ok(favoriteDto);
    }

    @Operation(summary = "Remove recipe from favorites", description = "Removes a recipe from the current user's favorites")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Recipe removed from favorites"),
            @ApiResponse(responseCode = "404", description = "Recipe not found in favorites"),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @DeleteMapping("/{recipeId}")
    public ResponseEntity<Void> removeFromFavorites(
            @Parameter(description = "ID of the recipe to remove from favorites")
            @PathVariable UUID recipeId,
            @Parameter(hidden = true) @AuthenticationPrincipal UUID userId) {
        log.debug("Entering removeFromFavorites: recipeId={}, userId={}", recipeId, userId);
        favoriteRecipeService.removeFromFavorites(userId, recipeId);
        log.debug("Exiting removeFromFavorites: recipeId={}, userId={}", recipeId, userId);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Get user's favorite recipes", description = "Returns the current user's favorite recipes with pagination")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "List of favorite recipes returned",
                    content = @Content(schema = @Schema(implementation = Page.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @GetMapping
    public ResponseEntity<Page<FavoriteRecipeDto>> getUserFavorites(
            @Parameter(description = "Pagination parameters")
            Pageable pageable,
            @Parameter(hidden = true) @AuthenticationPrincipal UUID userId) {
        log.debug("Entering getUserFavorites: pageable={}, userId={}", pageable, userId);
        Page<FavoriteRecipeDto> favoritesPage = favoriteRecipeService.getUserFavorites(userId, pageable);
        log.debug("Exiting getUserFavorites: userId={}, pageNumber={}, pageSize={}", userId, pageable.getPageNumber(), pageable.getPageSize());
        return ResponseEntity.ok(favoritesPage);
    }

    @Operation(summary = "Get all user's favorites", description = "Returns all favorite recipes for the current user")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "List of all favorite recipes returned",
                    content = @Content(schema = @Schema(implementation = List.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @GetMapping("/all")
    public ResponseEntity<List<FavoriteRecipeDto>> getAllUserFavorites(
            @Parameter(hidden = true) @AuthenticationPrincipal UUID userId) {
        log.debug("Entering getAllUserFavorites: userId={}", userId);
        List<FavoriteRecipeDto> favorites = favoriteRecipeService.getAllUserFavorites(userId);
        log.debug("Exiting getAllUserFavorites: userId={}, count={}", userId, favorites.size());
        return ResponseEntity.ok(favorites);
    }

    @Operation(summary = "Check if recipe is in favorites", description = "Checks if a recipe is in the current user's favorites")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Returns status indicating if recipe is in favorites",
                    content = @Content(schema = @Schema(implementation = IsFavoriteResponse.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @GetMapping("/check/{recipeId}")
    public ResponseEntity<IsFavoriteResponse> isRecipeInFavorites(
            @Parameter(description = "ID of the recipe to check")
            @PathVariable UUID recipeId,
            @Parameter(hidden = true) @AuthenticationPrincipal UUID userId) {
        log.debug("Entering isRecipeInFavorites: recipeId={}, userId={}", recipeId, userId);
        boolean isFavorite = favoriteRecipeService.isRecipeInFavorites(userId, recipeId);
        log.debug("Exiting isRecipeInFavorites: recipeId={}, userId={}, isFavorite={}", recipeId, userId, isFavorite);
        return ResponseEntity.ok(new IsFavoriteResponse(isFavorite));
    }

    @Operation(summary = "Get favorite count", description = "Returns the number of users who have favorited a recipe")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Favorite count returned",
                    content = @Content(schema = @Schema(implementation = FavoriteCountResponse.class)))
    })
    @GetMapping("/count/{recipeId}")
    public ResponseEntity<FavoriteCountResponse> getFavoriteCount(
            @Parameter(description = "ID of the recipe")
            @PathVariable UUID recipeId) {
        log.debug("Entering getFavoriteCount: recipeId={}", recipeId);
        long count = favoriteRecipeService.getFavoriteCount(recipeId);
        log.debug("Exiting getFavoriteCount: recipeId={}, count={}", recipeId, count);
        return ResponseEntity.ok(new FavoriteCountResponse(count));
    }
    
    // --- Batch Operations ---
    
    @Operation(summary = "Check multiple recipes favorite status", 
              description = "Checks if multiple recipes are in the current user's favorites")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Favorite statuses returned",
                    content = @Content(schema = @Schema(implementation = BatchFavoriteStatusResponse.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @PostMapping("/check/batch")
    public ResponseEntity<BatchFavoriteStatusResponse> batchCheckFavoriteStatus(
            @RequestBody BatchFavoriteRequest request,
            @Parameter(hidden = true) @AuthenticationPrincipal UUID userId) {
        log.debug("Entering batchCheckFavoriteStatus: userId={}, recipeCount={}", 
                 userId, request.recipeIds().size());
                 
        Set<UUID> recipeIds = new HashSet<>(request.recipeIds());
        Map<UUID, Boolean> statuses = favoriteRecipeService.getBatchFavoriteStatus(userId, recipeIds);
        
        log.debug("Exiting batchCheckFavoriteStatus: userId={}, statusCount={}", 
                 userId, statuses.size());
                 
        return ResponseEntity.ok(new BatchFavoriteStatusResponse(statuses));
    }
    
    @Operation(summary = "Get favorite counts for multiple recipes", 
              description = "Returns the number of users who have favorited each recipe")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Favorite counts returned",
                    content = @Content(schema = @Schema(implementation = BatchFavoriteCountResponse.class)))
    })
    @PostMapping("/count/batch")
    public ResponseEntity<BatchFavoriteCountResponse> batchGetFavoriteCounts(
            @RequestBody BatchFavoriteRequest request) {
        log.debug("Entering batchGetFavoriteCounts: recipeCount={}", request.recipeIds().size());
        
        Set<UUID> recipeIds = new HashSet<>(request.recipeIds());
        Map<UUID, Long> counts = favoriteRecipeService.getBatchFavoriteCounts(recipeIds);
        
        log.debug("Exiting batchGetFavoriteCounts: countSize={}", counts.size());
        
        return ResponseEntity.ok(new BatchFavoriteCountResponse(counts));
    }
    
    @Operation(summary = "Add multiple recipes to favorites", 
              description = "Adds multiple recipes to the current user's favorites")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Number of recipes added to favorites",
                    content = @Content(schema = @Schema(implementation = Integer.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @PostMapping("/batch")
    public ResponseEntity<Integer> batchAddToFavorites(
            @RequestBody BatchFavoriteRequest request,
            @Parameter(hidden = true) @AuthenticationPrincipal UUID userId) {
        log.debug("Entering batchAddToFavorites: userId={}, recipeCount={}", 
                 userId, request.recipeIds().size());
        
        int addedCount = favoriteRecipeService.addBatchToFavorites(userId, request.recipeIds());
        
        log.debug("Exiting batchAddToFavorites: userId={}, addedCount={}", userId, addedCount);
        
        return ResponseEntity.ok(addedCount);
    }
    
    @Operation(summary = "Remove multiple recipes from favorites", 
              description = "Removes multiple recipes from the current user's favorites")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Number of recipes removed from favorites",
                    content = @Content(schema = @Schema(implementation = Integer.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @DeleteMapping("/batch")
    public ResponseEntity<Integer> batchRemoveFromFavorites(
            @RequestBody BatchFavoriteRequest request,
            @Parameter(hidden = true) @AuthenticationPrincipal UUID userId) {
        log.debug("Entering batchRemoveFromFavorites: userId={}, recipeCount={}", 
                 userId, request.recipeIds().size());
        
        int removedCount = favoriteRecipeService.removeBatchFromFavorites(userId, request.recipeIds());
        
        log.debug("Exiting batchRemoveFromFavorites: userId={}, removedCount={}", 
                 userId, removedCount);
        
        return ResponseEntity.ok(removedCount);
    }
} 