package dev.idachev.recipeservice.web;

import dev.idachev.recipeservice.model.Recipe;
import dev.idachev.recipeservice.service.RecipeSearchService;
import dev.idachev.recipeservice.service.RecipeService;
import dev.idachev.recipeservice.service.VoteService;
import dev.idachev.recipeservice.user.service.UserService;
import dev.idachev.recipeservice.web.dto.RecipeRequest;
import dev.idachev.recipeservice.web.dto.RecipeResponse;
import dev.idachev.recipeservice.web.dto.SimplifiedRecipeResponse;
import dev.idachev.recipeservice.web.dto.VoteRequest;
import dev.idachev.recipeservice.web.mapper.RecipeMapper;
import dev.idachev.recipeservice.exception.UnauthorizedAccessException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
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
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

/**
 * Controller for recipe management operations.
 * Follows RESTful principles for HTTP methods and status codes.
 * All exceptions are handled by the GlobalExceptionHandler.
 */
@RestController
@RequestMapping("/api/v1/recipes")
@Slf4j
@Validated
@PreAuthorize("isAuthenticated()")
@Tag(name = "Recipes", description = "API for creating, updating, retrieving, and deleting recipes")
public class RecipeController {

    private final RecipeService recipeService;
    private final VoteService voteService;
    private final RecipeMapper recipeMapper;
    private final RecipeSearchService recipeSearchService;

    public RecipeController(RecipeService recipeService, UserService userService, VoteService voteService, RecipeMapper recipeMapper, RecipeSearchService recipeSearchService) {
        this.recipeService = recipeService;
        this.voteService = voteService;
        this.recipeMapper = recipeMapper;
        this.recipeSearchService = recipeSearchService;
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
            @Parameter(hidden = true) @AuthenticationPrincipal UUID userId) {
        log.debug("Entering createRecipe (multipart): userId={}", userId);
        
        if (userId == null) {
            log.error("Authentication principal (userId) is null in createRecipe endpoint!");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        
        log.info("Creating recipe with title '{}' for user: {}", request.title(), userId);
        RecipeResponse createdRecipe = recipeService.createRecipe(request, image, userId);
        log.debug("Exiting createRecipe (multipart): createdRecipeId={}, userId={}", createdRecipe.id(), userId);
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
            @Parameter(hidden = true) @AuthenticationPrincipal UUID userId) {
        log.debug("Entering createRecipeJson: userId={}", userId);
        RecipeResponse createdRecipe = recipeService.createRecipe(request, null, userId);
        log.debug("Exiting createRecipeJson: createdRecipeId={}, userId={}", createdRecipe.id(), userId);
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
            @Parameter(hidden = true) @AuthenticationPrincipal UUID userId) {
        log.debug("Entering getRecipeById: id={}, userId={}", id, userId);
        RecipeResponse recipe = recipeService.getRecipeById(id, userId);
        log.debug("Exiting getRecipeById: id={}, userId={}", id, userId);
        return ResponseEntity.ok(recipe);
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
            @Parameter(hidden = true) @AuthenticationPrincipal UUID userId) {
        log.debug("Entering getAllRecipes: pageable={}, showPersonal={}, userId={}", pageable, showPersonal, userId);
        
        Page<RecipeResponse> recipes = showPersonal
                ? recipeSearchService.getAllRecipes(pageable, userId)
                : recipeSearchService.getAllRecipesExcludingUser(pageable, userId);
        
        // Ensure we never return null to avoid NPE
        if (recipes == null) {
            log.warn("Recipe search service returned null result");
            recipes = Page.empty(pageable);
        }
        
        log.debug("Exiting getAllRecipes: userId={}, pageNumber={}, pageSize={}, results={}",
                userId, pageable.getPageNumber(), pageable.getPageSize(), recipes.getNumberOfElements());
        
        return ResponseEntity.ok(recipes);
    }

    @Operation(summary = "Get user's recipes")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "User recipes retrieved",
                    content = @Content(schema = @Schema(implementation = List.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @GetMapping("/my-recipes")
    public ResponseEntity<List<RecipeResponse>> getMyRecipes(
            @Parameter(hidden = true) @AuthenticationPrincipal UUID userId) {
        log.debug("Entering getMyRecipes: userId={}", userId);
        List<RecipeResponse> recipes = recipeService.getRecipesByUserId(userId);
        log.debug("Exiting getMyRecipes: userId={}, count={}", userId, recipes.size());
        return ResponseEntity.ok(recipes);
    }

    @Operation(summary = "Get recipe feed sorted by newest first")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Recipe feed retrieved",
                    content = @Content(schema = @Schema(implementation = Page.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @GetMapping("/feed")
    public ResponseEntity<Page<RecipeResponse>> getRecipeFeed(
            @Parameter(hidden = true) @AuthenticationPrincipal UUID userId,
            Pageable pageable) {
        log.debug("Entering getRecipeFeed: pageable={}, userId={}", pageable, userId);
        Page<RecipeResponse> feed = recipeService.getRecipeFeed(userId, pageable);
        log.debug("Exiting getRecipeFeed: userId={}, pageNumber={}, pageSize={}, results={}",
                userId, pageable.getPageNumber(), pageable.getPageSize(), feed.getNumberOfElements());
        return ResponseEntity.ok(feed);
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
            @Parameter(hidden = true) @AuthenticationPrincipal UUID userId) {
        log.debug("Entering updateRecipe: id={}, userId={}", id, userId);
        RecipeResponse updatedRecipe = recipeService.updateRecipe(id, request, userId);
        log.debug("Exiting updateRecipe: id={}, userId={}", id, userId);
        return ResponseEntity.ok(updatedRecipe);
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
            @Parameter(hidden = true) @AuthenticationPrincipal UUID userId) {
        log.debug("Entering updateRecipeWithImage: id={}, userId={}", id, userId);
        RecipeResponse updatedRecipe = recipeService.updateRecipe(id, request, image, userId);
        log.debug("Exiting updateRecipeWithImage: id={}, userId={}", id, userId);
        return ResponseEntity.ok(updatedRecipe);
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
            @Parameter(hidden = true) @AuthenticationPrincipal UUID userId) {
        log.debug("Entering deleteRecipe: id={}, userId={}", id, userId);
        recipeService.deleteRecipe(id, userId);
        log.debug("Exiting deleteRecipe: id={}, userId={}", id, userId);
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
            @Parameter(hidden = true) @AuthenticationPrincipal UUID userId) {
        log.debug("Entering searchRecipes: keyword={}, pageable={}, userId={}", keyword, pageable, userId);
        Page<RecipeResponse> results = recipeSearchService.searchRecipes(keyword, pageable, userId);
        log.debug("Exiting searchRecipes: keyword={}, userId={}, pageNumber={}, pageSize={}, results={}",
                keyword, userId, pageable.getPageNumber(), pageable.getPageSize(), results.getNumberOfElements());
        return ResponseEntity.ok(results);
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
            @Parameter(hidden = true) @AuthenticationPrincipal UUID userId) {

        log.debug("Entering voteRecipe: recipeId={}, voteType={}, userId={}", id, request.voteType(), userId);

        // Call vote service - it returns the updated Recipe entity
        Recipe updatedRecipeEntity = voteService.vote(id, request, userId);

        // 1. Map the updated entity to the base response DTO
        RecipeResponse baseResponseDto = recipeMapper.toResponse(updatedRecipeEntity);
        // 2. Enhance the response DTO with user-specific interactions
        RecipeResponse enhancedResponseDto = recipeService.enhanceWithUserInteractions(baseResponseDto, userId);

        log.debug("Exiting voteRecipe: recipeId={}, userId={}", id, userId);
        // Return the final enhanced DTO
        return ResponseEntity.ok(enhancedResponseDto);
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
        log.debug("Entering generateMeal: ingredientsCount={}", ingredients.size());
        SimplifiedRecipeResponse generatedRecipe = recipeService.generateMeal(ingredients);
        log.debug("Exiting generateMeal");
        return ResponseEntity.ok(generatedRecipe);
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
            @Parameter(hidden = true) @AuthenticationPrincipal UUID userId) {
        log.debug("Entering saveGeneratedRecipe: userId={}", userId);
        RecipeResponse savedRecipe = recipeService.createRecipe(request, userId);
        log.debug("Exiting saveGeneratedRecipe: savedRecipeId={}, userId={}", savedRecipe.id(), userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(savedRecipe);
    }
} 