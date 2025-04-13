package dev.idachev.recipeservice.web;

import dev.idachev.recipeservice.model.RecipeVote;
import dev.idachev.recipeservice.service.RecipeService;
import dev.idachev.recipeservice.service.VoteService;
import dev.idachev.recipeservice.user.service.UserService;
import dev.idachev.recipeservice.web.dto.RecipeRequest;
import dev.idachev.recipeservice.web.dto.RecipeResponse;
import dev.idachev.recipeservice.web.dto.SimplifiedRecipeResponse;
import dev.idachev.recipeservice.web.dto.VoteRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

import dev.idachev.recipeservice.exception.ResourceNotFoundException;
import dev.idachev.recipeservice.exception.ValidationException;
import dev.idachev.recipeservice.infrastructure.ai.AIService;
import dev.idachev.recipeservice.mapper.RecipeMapper;
import dev.idachev.recipeservice.model.Recipe;

/**
 * Controller for recipe management operations.
 * Follows RESTful principles for HTTP methods and status codes.
 * All exceptions are handled by the GlobalExceptionHandler.
 */
@RestController
@RequestMapping({"/api/v1/recipes", "/v1/recipes"})
@Slf4j
@Validated
@PreAuthorize("isAuthenticated()")
@Tag(name = "Recipes", description = "API for creating, updating, retrieving, and deleting recipes")
public class RecipeController {

    private final RecipeService recipeService;
    private final UserService userService;
    private final VoteService voteService;

    public RecipeController(RecipeService recipeService, UserService userService, VoteService voteService) {
        this.recipeService = recipeService;
        this.userService = userService;
        this.voteService = voteService;
    }

    @Operation(summary = "Create recipe with image")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Recipe created successfully", 
                    content = @Content(schema = @Schema(implementation = RecipeResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid input"),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<RecipeResponse> createRecipe(
            @RequestPart("recipe") @Valid RecipeRequest request,
            @RequestPart(value = "image", required = false) MultipartFile image,
            @RequestHeader("Authorization") String token) {
        
        UUID userId = userService.getUserIdFromToken(token);
        RecipeResponse createdRecipe = recipeService.createRecipe(request, image, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdRecipe);
    }

    @Operation(summary = "Create recipe (JSON only)")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Recipe created successfully", 
                    content = @Content(schema = @Schema(implementation = RecipeResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid input"),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<RecipeResponse> createRecipeJson(
            @Valid @RequestBody RecipeRequest request,
            @RequestHeader("Authorization") String token) {
        
        UUID userId = userService.getUserIdFromToken(token);
        RecipeResponse createdRecipe = recipeService.createRecipe(request, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdRecipe);
    }

    @Operation(summary = "Get recipe by ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Recipe found", 
                    content = @Content(schema = @Schema(implementation = RecipeResponse.class))),
            @ApiResponse(responseCode = "404", description = "Recipe not found"),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @GetMapping("/{id}")
    public ResponseEntity<RecipeResponse> getRecipeById(
            @PathVariable UUID id,
            @RequestHeader("Authorization") String token) {
        
        UUID userId = userService.getUserIdFromToken(token);
        return ResponseEntity.ok(recipeService.getRecipeById(id, userId));
    }

    @Operation(summary = "Get all recipes")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Recipes returned successfully", 
                    content = @Content(schema = @Schema(implementation = Page.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @GetMapping
    public ResponseEntity<Page<RecipeResponse>> getAllRecipes(
            Pageable pageable,
            @RequestParam(required = false, defaultValue = "false") boolean showPersonal,
            @RequestHeader("Authorization") String token) {
        
        UUID userId = userService.getUserIdFromToken(token);
        return ResponseEntity.ok(recipeService.getAllRecipes(pageable, userId, showPersonal));
    }

    @Operation(summary = "Get user's recipes")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "User recipes retrieved", 
                    content = @Content(schema = @Schema(implementation = RecipeResponse.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @GetMapping("/my-recipes")
    public ResponseEntity<List<RecipeResponse>> getMyRecipes(@RequestHeader("Authorization") String token) {
        UUID userId = userService.getUserIdFromToken(token);
        return ResponseEntity.ok(recipeService.getRecipesByUserId(userId));
    }

    @Operation(summary = "Get recipe feed sorted by newest first")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Recipe feed retrieved", 
                    content = @Content(schema = @Schema(implementation = RecipeResponse.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @GetMapping("/feed")
    public ResponseEntity<Page<RecipeResponse>> getRecipeFeed(
            @RequestHeader("Authorization") String token,
            Pageable pageable) {
        UUID userId = userService.getUserIdFromToken(token);
        return ResponseEntity.ok(recipeService.getRecipeFeed(userId, pageable));
    }

    @Operation(summary = "Debug endpoint to get recipe with author info")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Recipe with debug info retrieved"),
            @ApiResponse(responseCode = "404", description = "Recipe not found"),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @GetMapping("/debug/{id}")
    public ResponseEntity<RecipeResponse> getRecipeWithDebugInfo(
            @PathVariable UUID id,
            @RequestHeader("Authorization") String token) {
        UUID userId = userService.getUserIdFromToken(token);
        RecipeResponse response = recipeService.getRecipeById(id, userId);
        
        // Try to fix missing username if needed
        if (response.getCreatedById() != null && 
            ("Unknown User".equals(response.getAuthorName()) || response.getAuthorName() == null)) {
            
            try {
                // Direct API call attempt
                String username = userService.getUsernameById(response.getCreatedById());
                if (username != null && !"Unknown User".equals(username)) {
                    response.setUsername(username);
                    response.setAuthorName(username);
                    log.debug("DEBUG - Fixed username via API: {}", username);
                }
            } catch (Exception e) {
                log.warn("DEBUG - API username lookup failed for recipe {}", id);
            }
        }
        
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Update recipe")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Recipe updated successfully", 
                    content = @Content(schema = @Schema(implementation = RecipeResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid input"),
            @ApiResponse(responseCode = "404", description = "Recipe not found"),
            @ApiResponse(responseCode = "403", description = "Forbidden - not the recipe owner"),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @PutMapping("/{id}")
    public ResponseEntity<RecipeResponse> updateRecipe(
            @PathVariable UUID id,
            @Valid @RequestBody RecipeRequest request,
            @RequestHeader("Authorization") String token) {
        
        UUID userId = userService.getUserIdFromToken(token);
        return ResponseEntity.ok(recipeService.updateRecipe(id, request, userId));
    }

    @Operation(summary = "Update recipe with image")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Recipe updated successfully", 
                    content = @Content(schema = @Schema(implementation = RecipeResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid input"),
            @ApiResponse(responseCode = "404", description = "Recipe not found"),
            @ApiResponse(responseCode = "403", description = "Forbidden - not the recipe owner"),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @PutMapping(path = "/{id}/multipart", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<RecipeResponse> updateRecipeWithImage(
            @PathVariable UUID id,
            @RequestPart("recipe") @Valid RecipeRequest request,
            @RequestPart(value = "image", required = false) MultipartFile image,
            @RequestHeader("Authorization") String token) {
        
        UUID userId = userService.getUserIdFromToken(token);
        return ResponseEntity.ok(recipeService.updateRecipe(id, request, image, userId));
    }

    @Operation(summary = "Delete recipe")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Recipe deleted successfully"),
            @ApiResponse(responseCode = "404", description = "Recipe not found"),
            @ApiResponse(responseCode = "403", description = "Forbidden - not the recipe owner"),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteRecipe(
            @PathVariable UUID id,
            @RequestHeader("Authorization") String token) {
        
        UUID userId = userService.getUserIdFromToken(token);
        recipeService.deleteRecipe(id, userId);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Search recipes")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Search results returned", 
                    content = @Content(schema = @Schema(implementation = Page.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @GetMapping("/search")
    public ResponseEntity<Page<RecipeResponse>> searchRecipes(
            @RequestParam String keyword,
            Pageable pageable,
            @RequestHeader("Authorization") String token) {
        
        UUID userId = userService.getUserIdFromToken(token);
        return ResponseEntity.ok(recipeService.searchRecipes(keyword, pageable, userId));
    }

    @Operation(summary = "Vote on a recipe")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Vote submitted successfully", 
                    content = @Content(schema = @Schema(implementation = RecipeResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid input"),
            @ApiResponse(responseCode = "404", description = "Recipe not found"),
            @ApiResponse(responseCode = "403", description = "Forbidden - cannot vote on own recipe"),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @PostMapping("/{id}/vote")
    public ResponseEntity<RecipeResponse> voteRecipe(
            @PathVariable UUID id,
            @Valid @RequestBody VoteRequest request,
            @RequestHeader("Authorization") String token) {
        
        log.debug("Vote request received: recipeId={}, voteType={}", id, request.getVoteType());
        
        UUID userId = userService.getUserIdFromToken(token);
        log.debug("Extracted userId: {}", userId);
        
        RecipeVote.VoteType voteType = "up".equalsIgnoreCase(request.getVoteType()) 
                ? RecipeVote.VoteType.UPVOTE 
                : RecipeVote.VoteType.DOWNVOTE;
        
        log.debug("Mapped vote type: {}", voteType);
                
        try {
            Recipe updatedRecipe = voteService.vote(id, voteType, userId);
            log.debug("Vote processed successfully, new upvotes: {}, downvotes: {}", 
                    updatedRecipe.getUpvotes(), updatedRecipe.getDownvotes());
            
            RecipeResponse response = recipeService.toResponse(updatedRecipe, userId);
            log.debug("Response prepared, upvotes: {}, downvotes: {}, userVote: {}", 
                    response.getUpvotes(), response.getDownvotes(), response.getUserVote());
                    
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error processing vote: {}", e.getMessage(), e);
            throw e;
        }
    }

    @Operation(summary = "Generate recipe from ingredients")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Recipe generated", 
                    content = @Content(schema = @Schema(implementation = SimplifiedRecipeResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid input - empty ingredients list"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "503", description = "AI service unavailable")
    })
    @PostMapping("/generate")
    public ResponseEntity<SimplifiedRecipeResponse> generateMeal(
            @RequestBody @NotEmpty(message = "Ingredients list cannot be empty") List<String> ingredients) {
        return ResponseEntity.ok(recipeService.generateMeal(ingredients));
    }

    @Operation(summary = "Save generated recipe")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Recipe saved", 
                    content = @Content(schema = @Schema(implementation = RecipeResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid input"),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @PostMapping("/save")
    public ResponseEntity<RecipeResponse> saveGeneratedRecipe(
            @Valid @RequestBody RecipeRequest request,
            @RequestHeader("Authorization") String token) {
        
        UUID userId = userService.getUserIdFromToken(token);
        RecipeResponse savedRecipe = recipeService.createRecipe(request, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(savedRecipe);
    }
} 