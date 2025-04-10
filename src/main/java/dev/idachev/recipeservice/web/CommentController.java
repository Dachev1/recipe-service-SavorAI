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
 * Controller for comment management operations.
 */
@RestController
@RequestMapping({"/api/v1/recipes", "/v1/recipes"})
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

    @Operation(summary = "Get comments for a recipe")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Comments retrieved successfully", 
                    content = @Content(schema = @Schema(implementation = Page.class))),
            @ApiResponse(responseCode = "404", description = "Recipe not found"),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @GetMapping("/{recipeId}/comments")
    public ResponseEntity<Page<CommentResponse>> getCommentsForRecipe(
            @PathVariable UUID recipeId,
            Pageable pageable,
            @RequestHeader("Authorization") String token) {
        
        UUID userId = userService.getUserIdFromToken(token);
        return ResponseEntity.ok(commentService.getCommentsForRecipe(recipeId, pageable, userId));
    }
    
    @Operation(summary = "Add a comment to a recipe")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Comment added successfully", 
                    content = @Content(schema = @Schema(implementation = CommentResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid input"),
            @ApiResponse(responseCode = "404", description = "Recipe not found"),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @PostMapping("/{recipeId}/comments")
    public ResponseEntity<CommentResponse> addComment(
            @PathVariable UUID recipeId,
            @Valid @RequestBody CommentRequest request,
            @RequestHeader("Authorization") String token) {
        
        CommentResponse createdComment = commentService.createComment(recipeId, request, token);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdComment);
    }
    
    @Operation(summary = "Update a comment")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Comment updated successfully", 
                    content = @Content(schema = @Schema(implementation = CommentResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid input"),
            @ApiResponse(responseCode = "404", description = "Comment not found"),
            @ApiResponse(responseCode = "403", description = "Forbidden - not the comment owner"),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @PutMapping("/comments/{commentId}")
    public ResponseEntity<CommentResponse> updateComment(
            @PathVariable UUID commentId,
            @Valid @RequestBody CommentRequest request,
            @RequestHeader("Authorization") String token) {
        
        UUID userId = userService.getUserIdFromToken(token);
        return ResponseEntity.ok(commentService.updateComment(commentId, request, userId));
    }
    
    @Operation(summary = "Delete a comment")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Comment deleted successfully"),
            @ApiResponse(responseCode = "404", description = "Comment not found"),
            @ApiResponse(responseCode = "403", description = "Forbidden - not the comment owner or recipe owner"),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @DeleteMapping("/comments/{commentId}")
    public ResponseEntity<Void> deleteComment(
            @PathVariable UUID commentId,
            @RequestHeader("Authorization") String token) {
        
        UUID userId = userService.getUserIdFromToken(token);
        commentService.deleteComment(commentId, userId);
        return ResponseEntity.noContent().build();
    }
} 