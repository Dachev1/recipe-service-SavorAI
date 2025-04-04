package dev.idachev.recipeservice.web;

import dev.idachev.recipeservice.service.RecipeService;
import dev.idachev.recipeservice.user.service.UserService;
import dev.idachev.recipeservice.web.dto.RecipeRequest;
import dev.idachev.recipeservice.web.dto.RecipeResponse;
import dev.idachev.recipeservice.web.dto.SimplifiedRecipeResponse;
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
import org.springframework.beans.factory.annotation.Autowired;
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
    private final UserService userService;

    @Autowired
    public RecipeController(RecipeService recipeService, UserService userService) {
        this.recipeService = recipeService;
        this.userService = userService;
    }

    @Operation(summary = "Create recipe with image", description = "Creates a new recipe with optional image upload")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Recipe created successfully", 
                    content = @Content(schema = @Schema(implementation = RecipeResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid input"),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<RecipeResponse> createRecipe(
            @Parameter(description = "Recipe data") 
            @RequestPart("recipe") @Valid RecipeRequest request,
            @Parameter(description = "Optional image file") 
            @RequestPart(value = "image", required = false) MultipartFile image,
            @RequestHeader("Authorization") String token) {
        
        UUID userId = userService.getUserIdFromToken(token);
        RecipeResponse createdRecipe = recipeService.createRecipe(request, image, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdRecipe);
    }

    @Operation(summary = "Create recipe (JSON only)", description = "Creates a new recipe with JSON data only")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Recipe created successfully", 
                    content = @Content(schema = @Schema(implementation = RecipeResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid input"),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<RecipeResponse> createRecipeJson(
            @Parameter(description = "Recipe data") 
            @Valid @RequestBody RecipeRequest request,
            @RequestHeader("Authorization") String token) {
        
        UUID userId = userService.getUserIdFromToken(token);
        RecipeResponse createdRecipe = recipeService.createRecipe(request, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdRecipe);
    }

    @Operation(summary = "Get recipe by ID", description = "Returns a recipe by its ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Recipe found", 
                    content = @Content(schema = @Schema(implementation = RecipeResponse.class))),
            @ApiResponse(responseCode = "404", description = "Recipe not found"),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @GetMapping("/{id}")
    public ResponseEntity<RecipeResponse> getRecipeById(
            @Parameter(description = "Recipe ID") 
            @PathVariable UUID id,
            @RequestHeader("Authorization") String token) {
        
        UUID userId = userService.getUserIdFromToken(token);
        return ResponseEntity.ok(recipeService.getRecipeById(id, userId));
    }

    @Operation(summary = "Get all recipes", description = "Returns all recipes with pagination")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Recipes returned successfully", 
                    content = @Content(schema = @Schema(implementation = Page.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @GetMapping
    public ResponseEntity<Page<RecipeResponse>> getAllRecipes(
            @Parameter(description = "Pagination parameters") 
            Pageable pageable,
            @Parameter(description = "Whether to show user's personal recipes in catalog")
            @RequestParam(required = false, defaultValue = "false") boolean showPersonal,
            @RequestHeader("Authorization") String token) {
        
        UUID userId = userService.getUserIdFromToken(token);
        return ResponseEntity.ok(recipeService.getAllRecipes(pageable, userId, showPersonal));
    }

    @Operation(summary = "Get current user's recipes", description = "Returns recipes created by the current user")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "User recipes retrieved successfully", 
                    content = @Content(schema = @Schema(implementation = RecipeResponse.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @GetMapping("/my-recipes")
    public ResponseEntity<List<RecipeResponse>> getMyRecipes(
            @RequestHeader("Authorization") String token) {
        
        UUID userId = userService.getUserIdFromToken(token);
        return ResponseEntity.ok(recipeService.getRecipesByUserId(userId));
    }

    @Operation(summary = "Update recipe", description = "Updates an existing recipe")
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
            @Parameter(description = "Recipe ID") 
            @PathVariable UUID id,
            @Parameter(description = "Updated recipe data") 
            @Valid @RequestBody RecipeRequest request,
            @RequestHeader("Authorization") String token) {
        
        UUID userId = userService.getUserIdFromToken(token);
        return ResponseEntity.ok(recipeService.updateRecipe(id, request, userId));
    }

    @Operation(summary = "Delete recipe", description = "Deletes a recipe")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Recipe deleted successfully"),
            @ApiResponse(responseCode = "404", description = "Recipe not found"),
            @ApiResponse(responseCode = "403", description = "Forbidden - not the recipe owner"),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteRecipe(
            @Parameter(description = "Recipe ID") 
            @PathVariable UUID id,
            @RequestHeader("Authorization") String token) {
        
        UUID userId = userService.getUserIdFromToken(token);
        recipeService.deleteRecipe(id, userId);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Search recipes", description = "Searches recipes by keyword")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Search results returned", 
                    content = @Content(schema = @Schema(implementation = Page.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @GetMapping("/search")
    public ResponseEntity<Page<RecipeResponse>> searchRecipes(
            @Parameter(description = "Search keyword") 
            @RequestParam String keyword,
            @Parameter(description = "Pagination parameters") 
            Pageable pageable,
            @RequestHeader("Authorization") String token) {
        
        UUID userId = userService.getUserIdFromToken(token);
        return ResponseEntity.ok(recipeService.searchRecipes(keyword, pageable, userId));
    }

    @Operation(summary = "Generate recipe from ingredients", description = "Uses AI to generate a recipe from a list of ingredients")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Recipe generated successfully", 
                    content = @Content(schema = @Schema(implementation = SimplifiedRecipeResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid input - empty ingredients list"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - session expired or invalid"),
            @ApiResponse(responseCode = "503", description = "AI service unavailable")
    })
    @PostMapping("/generate")
    public ResponseEntity<SimplifiedRecipeResponse> generateMeal(
            @Parameter(description = "List of ingredients") 
            @RequestBody @NotEmpty(message = "Ingredients list cannot be empty") List<String> ingredients) {
        
        return ResponseEntity.ok(recipeService.generateMeal(ingredients));
    }
} 