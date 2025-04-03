package dev.idachev.recipeservice.web;

import dev.idachev.recipeservice.service.CommentService;
import dev.idachev.recipeservice.user.service.UserService;
import dev.idachev.recipeservice.web.dto.CommentRequest;
import dev.idachev.recipeservice.web.dto.CommentResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Controller for comment operations.
 * Follows RESTful principles for HTTP methods and status codes.
 * All exceptions are handled by the GlobalExceptionHandler.
 */
@RestController
@RequestMapping("/api/v1/recipes/{recipeId}/comments")
@Slf4j
@PreAuthorize("isAuthenticated()")
@Tag(name = "Comments", description = "API for managing recipe comments")
public class CommentController {

    private final CommentService commentService;
    private final UserService userService;

    @Autowired
    public CommentController(CommentService commentService, UserService userService) {
        this.commentService = commentService;
        this.userService = userService;
    }

    @Operation(summary = "Add a comment to a recipe", description = "Creates a new comment for a specific recipe")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Comment created successfully",
                    content = @Content(schema = @Schema(implementation = CommentResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid input"),
            @ApiResponse(responseCode = "404", description = "Recipe not found"),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @PostMapping
    public ResponseEntity<CommentResponse> addComment(
            @Parameter(description = "ID of the recipe to comment on")
            @PathVariable UUID recipeId,
            @Parameter(description = "Comment data")
            @Valid @RequestBody CommentRequest request,
            @RequestHeader("Authorization") String token) {

        // Use the simpler method that just takes token
        CommentResponse comment = commentService.createComment(recipeId, request, token);
        return ResponseEntity.status(HttpStatus.CREATED).body(comment);
    }

    @Operation(summary = "Get comments for a recipe", description = "Returns comments for a specific recipe with pagination")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Comments retrieved successfully",
                    content = @Content(schema = @Schema(implementation = Page.class))),
            @ApiResponse(responseCode = "404", description = "Recipe not found"),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @GetMapping
    public ResponseEntity<Page<CommentResponse>> getCommentsForRecipe(
            @Parameter(description = "ID of the recipe to get comments for")
            @PathVariable UUID recipeId,
            @Parameter(description = "Pagination parameters")
            Pageable pageable,
            @RequestHeader("Authorization") String token) {

        UUID userId = userService.getUserIdFromToken(token);
        return ResponseEntity.ok(commentService.getCommentsForRecipe(recipeId, pageable, userId));
    }

    @Operation(summary = "Update a comment", description = "Updates an existing comment")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Comment updated successfully",
                    content = @Content(schema = @Schema(implementation = CommentResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid input"),
            @ApiResponse(responseCode = "404", description = "Comment not found"),
            @ApiResponse(responseCode = "403", description = "Forbidden - User does not own the comment"),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @PutMapping("/{commentId}")
    public ResponseEntity<CommentResponse> updateComment(
            @Parameter(description = "ID of the recipe the comment belongs to")
            @PathVariable UUID recipeId,
            @Parameter(description = "ID of the comment to update")
            @PathVariable UUID commentId,
            @Parameter(description = "Updated comment data")
            @Valid @RequestBody CommentRequest request,
            @RequestHeader("Authorization") String token) {

        UUID userId = userService.getUserIdFromToken(token);
        return ResponseEntity.ok(commentService.updateComment(commentId, request, userId));
    }

    @Operation(summary = "Delete a comment", description = "Deletes an existing comment")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Comment deleted successfully"),
            @ApiResponse(responseCode = "404", description = "Comment not found"),
            @ApiResponse(responseCode = "403", description = "Forbidden - User does not own the comment"),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @DeleteMapping("/{commentId}")
    public ResponseEntity<Void> deleteComment(
            @Parameter(description = "ID of the recipe the comment belongs to")
            @PathVariable UUID recipeId,
            @Parameter(description = "ID of the comment to delete")
            @PathVariable UUID commentId,
            @RequestHeader("Authorization") String token) {

        UUID userId = userService.getUserIdFromToken(token);
        commentService.deleteComment(commentId, userId);
        return ResponseEntity.noContent().build();
    }
} 