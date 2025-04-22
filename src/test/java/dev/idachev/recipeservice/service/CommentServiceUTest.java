package dev.idachev.recipeservice.service;

import dev.idachev.recipeservice.exception.ResourceNotFoundException;
import dev.idachev.recipeservice.exception.UnauthorizedAccessException;
import dev.idachev.recipeservice.model.Comment;
import dev.idachev.recipeservice.model.Recipe;
import dev.idachev.recipeservice.repository.CommentRepository;
import dev.idachev.recipeservice.repository.RecipeRepository;
import dev.idachev.recipeservice.user.dto.UserResponse;
import dev.idachev.recipeservice.user.service.UserService;
import dev.idachev.recipeservice.web.dto.CommentRequest;
import dev.idachev.recipeservice.web.dto.CommentResponse;
import dev.idachev.recipeservice.web.mapper.CommentMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CommentServiceUTest {

    @Mock
    private CommentRepository commentRepository;
    @Mock
    private RecipeRepository recipeRepository;
    @Mock
    private CommentMapper commentMapper;
    @Mock
    private UserService userService;

    @InjectMocks
    private CommentService commentService;

    @Captor
    private ArgumentCaptor<Comment> commentCaptor;

    private UUID testUserId;
    private UUID testRecipeId;
    private UUID testCommentId;
    private UUID testRecipeOwnerId;
    private String testUsername;
    private String testToken;
    private Recipe testRecipe; // Recipe being commented on
    private Comment testComment;
    private CommentRequest testCommentRequest;
    private CommentResponse testCommentResponse;
    private UserResponse testUserResponse;

    @BeforeEach
    void setUp() {
        testUserId = UUID.randomUUID();
        testRecipeOwnerId = UUID.randomUUID(); // Different from testUserId
        testRecipeId = UUID.randomUUID();
        testCommentId = UUID.randomUUID();
        testUsername = "testuser";
        testToken = "dummy-token";

        testRecipe = Recipe.builder()
                .id(testRecipeId)
                .userId(testRecipeOwnerId) // Recipe owned by someone else
                // Add other necessary recipe fields if needed for checks
                .title("Test Recipe Title")
                .build();

        testCommentRequest = new CommentRequest("This is a test comment");

        testComment = Comment.builder()
                .id(testCommentId)
                .recipeId(testRecipeId)
                .userId(testUserId)
                .username(testUsername)
                .content(testCommentRequest.content())
                .createdAt(LocalDateTime.now().minusMinutes(5))
                .updatedAt(LocalDateTime.now())
                .build();

        // Fix CommentResponse instantiation order and fields
        testCommentResponse = new CommentResponse(
                testCommentId,
                testCommentRequest.content(),
                testUserId,
                testUsername,
                testRecipeId,
                testComment.getCreatedAt(),
                testComment.getUpdatedAt(),
                true, // isOwner - Assume the response is for the comment owner
                false // isRecipeOwner - Commenter is not recipe owner
        );

        // Fix UserResponse instantiation - Use builder or ensure constructor matches
        // Using builder for clarity
        testUserResponse = UserResponse.builder()
                .id(testUserId)
                .username(testUsername)
                .email("user@example.com")
                .verified(true)
                .verificationPending(false)
                .banned(false)
                .role("ROLE_USER")
                .createdOn(LocalDateTime.now().minusDays(10))
                .lastLogin(LocalDateTime.now().minusHours(1))
                .build();
    }

    // --- Test Structure ---
    // Group tests by method

    @Nested
    @DisplayName("createComment Tests (Multiple Overloads)")
    class CreateCommentTests {

        @Test
        @DisplayName("Should create comment using token")
        void createComment_WithToken_Success() {
            // Given
            // 1. User service returns user info based on token
            when(userService.getUserResponseFromToken(testToken, "Create Comment")).thenReturn(testUserResponse);
            
            // 2. Recipe repo finds the recipe (not owned by the commenter)
            when(recipeRepository.findById(testRecipeId)).thenReturn(Optional.of(testRecipe));
            
            // 3. Mapper maps request to entity
            Comment commentToSave = Comment.builder()
                    // id, createdAt, updatedAt are set by repo/JPA
                    .recipeId(testRecipeId)
                    .userId(testUserResponse.getId()) // From token user
                    .username(testUserResponse.getUsername()) // From token user
                    .content(testCommentRequest.content())
                    .build();
            when(commentMapper.toEntity(testCommentRequest, testUserResponse.getId(), testUserResponse.getUsername(), testRecipeId))
                .thenReturn(commentToSave);
            
            // 4. Repo saves the comment
            Comment savedComment = testComment; // Use the fully formed comment from setUp
            when(commentRepository.save(commentCaptor.capture())).thenReturn(savedComment);
            
            // 5. Mapper maps saved entity to response, passing ownership flags
            // Expect isOwner=true (creator is owner), isRecipeOwner=false (commenter != recipe owner)
            when(commentMapper.toResponse(savedComment, true, false)).thenReturn(testCommentResponse);

            // When
            CommentResponse actualResponse = commentService.createComment(testRecipeId, testCommentRequest, testToken);

            // Then
            assertThat(actualResponse).isEqualTo(testCommentResponse);
            
            // Verify interactions
            verify(userService).getUserResponseFromToken(testToken, "Create Comment");
            verify(recipeRepository).findById(testRecipeId);
            verify(commentMapper).toEntity(testCommentRequest, testUserResponse.getId(), testUserResponse.getUsername(), testRecipeId);
            verify(commentRepository).save(any(Comment.class));
            verify(commentMapper).toResponse(savedComment, true, false);
            
            // Verify captured comment details before save
            Comment captured = commentCaptor.getValue();
            assertThat(captured.getId()).isNull();
            assertThat(captured.getRecipeId()).isEqualTo(testRecipeId);
            assertThat(captured.getUserId()).isEqualTo(testUserResponse.getId());
            assertThat(captured.getUsername()).isEqualTo(testUserResponse.getUsername());
            assertThat(captured.getContent()).isEqualTo(testCommentRequest.content());
        }

        @Test
        @DisplayName("Should create comment using userId and token (validation)")
        void createComment_WithUserIdAndToken_Success() {
            // Given
            // 1. User service validates token and returns matching user
            when(userService.getUserResponseFromToken(testToken, "Create Comment (ID Validation)")).thenReturn(testUserResponse);
            
            // 2. Recipe repo finds the recipe
            when(recipeRepository.findById(testRecipeId)).thenReturn(Optional.of(testRecipe));
            
            // 3. Mapper maps request to entity (using info from validated user)
            Comment commentToSave = Comment.builder()
                    .recipeId(testRecipeId)
                    .userId(testUserResponse.getId())
                    .username(testUserResponse.getUsername())
                    .content(testCommentRequest.content())
                    .build();
            when(commentMapper.toEntity(testCommentRequest, testUserResponse.getId(), testUserResponse.getUsername(), testRecipeId))
                .thenReturn(commentToSave);
            
            // 4. Repo saves the comment
            Comment savedComment = testComment;
            when(commentRepository.save(commentCaptor.capture())).thenReturn(savedComment);
            
            // 5. Mapper maps saved entity to response
            when(commentMapper.toResponse(savedComment, true, false)).thenReturn(testCommentResponse);

            // When
            // Call with the correct userId matching the token's user
            CommentResponse actualResponse = commentService.createComment(testRecipeId, testCommentRequest, testUserId, testToken);

            // Then
            assertThat(actualResponse).isEqualTo(testCommentResponse);
            
            // Verify interactions
            verify(userService).getUserResponseFromToken(testToken, "Create Comment (ID Validation)");
            verify(recipeRepository).findById(testRecipeId);
            verify(commentMapper).toEntity(testCommentRequest, testUserId, testUsername, testRecipeId);
            verify(commentRepository).save(any(Comment.class));
            verify(commentMapper).toResponse(savedComment, true, false);
            
            // Verify captured comment
            Comment captured = commentCaptor.getValue();
            assertThat(captured.getUserId()).isEqualTo(testUserId);
            assertThat(captured.getUsername()).isEqualTo(testUsername);
        }

        @Test
        @DisplayName("Should throw UnauthorizedAccessException if userId and token mismatch")
        void createComment_WithUserIdAndToken_MismatchThrowsException() {
            // Given
            UUID wrongUserId = UUID.randomUUID(); // A different userId than the one in the token
            // Mock user service to return the testUserResponse (which has testUserId)
            when(userService.getUserResponseFromToken(testToken, "Create Comment (ID Validation)")).thenReturn(testUserResponse);

            // When / Then
            // Call createComment with wrongUserId but the valid token
            assertThatThrownBy(() -> commentService.createComment(testRecipeId, testCommentRequest, wrongUserId, testToken))
                .isInstanceOf(UnauthorizedAccessException.class)
                .hasMessageContaining("User ID mismatch");

            // Verify mocks
            verify(userService).getUserResponseFromToken(testToken, "Create Comment (ID Validation)");
            // Should fail before interacting with other repositories or mappers
            verifyNoInteractions(recipeRepository, commentRepository, commentMapper);
        }

        @Test
        @DisplayName("Should create comment using userId only")
        void createComment_WithUserIdOnly_Success() {
            // Given
            // 1. User service returns username for the given userId
            when(userService.getUsernameById(testUserId)).thenReturn(testUsername);
            
            // 2. Recipe repo finds recipe
            when(recipeRepository.findById(testRecipeId)).thenReturn(Optional.of(testRecipe));
            
            // 3. Mapper maps request to entity
            Comment commentToSave = Comment.builder()
                    .recipeId(testRecipeId)
                    .userId(testUserId)
                    .username(testUsername)
                    .content(testCommentRequest.content())
                    .build();
            when(commentMapper.toEntity(testCommentRequest, testUserId, testUsername, testRecipeId))
                .thenReturn(commentToSave);
            
            // 4. Repo saves comment
            Comment savedComment = testComment;
            when(commentRepository.save(commentCaptor.capture())).thenReturn(savedComment);
            
            // 5. Mapper maps saved entity to response
            when(commentMapper.toResponse(savedComment, true, false)).thenReturn(testCommentResponse);

            // When
            CommentResponse actualResponse = commentService.createComment(testRecipeId, testCommentRequest, testUserId);

            // Then
            assertThat(actualResponse).isEqualTo(testCommentResponse);
            
            // Verify interactions
            verify(userService).getUsernameById(testUserId);
            verify(recipeRepository).findById(testRecipeId);
            verify(commentMapper).toEntity(testCommentRequest, testUserId, testUsername, testRecipeId);
            verify(commentRepository).save(any(Comment.class));
            verify(commentMapper).toResponse(savedComment, true, false);
            verifyNoMoreInteractions(userService); // No other userService calls expected
            
            // Verify captured comment
            Comment captured = commentCaptor.getValue();
            assertThat(captured.getUserId()).isEqualTo(testUserId);
            assertThat(captured.getUsername()).isEqualTo(testUsername);
        }
        
        @Test
        @DisplayName("Should create comment using system method (userId and username)")
        void createSystemComment_Success() {
             // Given
             String systemUsername = "systemUser";
             // 1. Recipe repo finds recipe
             when(recipeRepository.findById(testRecipeId)).thenReturn(Optional.of(testRecipe));
            
             // 2. Mapper maps request to entity
             Comment commentToSave = Comment.builder()
                     .recipeId(testRecipeId)
                     .userId(testUserId) // Use provided userId
                     .username(systemUsername) // Use provided username
                     .content(testCommentRequest.content())
                     .build();
             when(commentMapper.toEntity(testCommentRequest, testUserId, systemUsername, testRecipeId))
                 .thenReturn(commentToSave);
            
             // 3. Repo saves comment
             Comment savedComment = testComment.toBuilder()
                                          .username(systemUsername)
                                          .build(); // Adjust the expected saved comment
             when(commentRepository.save(commentCaptor.capture())).thenReturn(savedComment);
            
             // 4. Mapper maps saved entity to response
             CommentResponse expectedResponse = new CommentResponse(
                savedComment.getId(), savedComment.getContent(), savedComment.getUserId(),
                savedComment.getUsername(), savedComment.getRecipeId(), savedComment.getCreatedAt(),
                savedComment.getUpdatedAt(), true, false
             );
             when(commentMapper.toResponse(savedComment, true, false)).thenReturn(expectedResponse);

             // When
             CommentResponse actualResponse = commentService.createSystemComment(testRecipeId, testCommentRequest, testUserId, systemUsername);

             // Then
             assertThat(actualResponse).isEqualTo(expectedResponse);
            
             // Verify interactions
             verify(recipeRepository).findById(testRecipeId);
             verify(commentMapper).toEntity(testCommentRequest, testUserId, systemUsername, testRecipeId);
             verify(commentRepository).save(any(Comment.class));
             verify(commentMapper).toResponse(savedComment, true, false);
             verifyNoInteractions(userService); // No user service interaction
            
             // Verify captured comment
             Comment captured = commentCaptor.getValue();
             assertThat(captured.getUserId()).isEqualTo(testUserId);
             assertThat(captured.getUsername()).isEqualTo(systemUsername);
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException if recipe not found")
        void createComment_RecipeNotFound_ThrowsException() {
            // Given
            UUID nonExistentRecipeId = UUID.randomUUID();
            // Mock recipe repo to find nothing
            when(recipeRepository.findById(nonExistentRecipeId)).thenReturn(Optional.empty());
            
            // Mock user service calls needed for different overloads
            when(userService.getUserResponseFromToken(testToken, "Create Comment")).thenReturn(testUserResponse);
            when(userService.getUserResponseFromToken(testToken, "Create Comment (ID Validation)")).thenReturn(testUserResponse);
            when(userService.getUsernameById(testUserId)).thenReturn(testUsername);

            // When / Then
            // Assert for each create overload that uses recipeRepository.findById
            assertThatThrownBy(() -> commentService.createComment(nonExistentRecipeId, testCommentRequest, testToken))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Recipe not found");
                
            assertThatThrownBy(() -> commentService.createComment(nonExistentRecipeId, testCommentRequest, testUserId, testToken))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Recipe not found");
                
            assertThatThrownBy(() -> commentService.createComment(nonExistentRecipeId, testCommentRequest, testUserId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Recipe not found");
                
            assertThatThrownBy(() -> commentService.createSystemComment(nonExistentRecipeId, testCommentRequest, testUserId, testUsername))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Recipe not found");

            // Verify that recipeRepository.findById was called, but save wasn't
            verify(recipeRepository, times(4)).findById(nonExistentRecipeId);
            verify(commentRepository, never()).save(any(Comment.class));
            verifyNoInteractions(commentMapper); 
        }

        @Test
        @DisplayName("Should throw UnauthorizedAccessException if user comments on own recipe")
        void createComment_OwnRecipe_ThrowsException() {
            // Given
            // Recipe is owned by testUserId (the commenter)
            Recipe ownRecipe = Recipe.builder().id(testRecipeId).userId(testUserId).build(); 
            when(recipeRepository.findById(testRecipeId)).thenReturn(Optional.of(ownRecipe));
            
            // Mock user service calls needed for different overloads
            when(userService.getUserResponseFromToken(testToken, "Create Comment")).thenReturn(testUserResponse); // Returns testUserId
            when(userService.getUserResponseFromToken(testToken, "Create Comment (ID Validation)")).thenReturn(testUserResponse); // Returns testUserId
            when(userService.getUsernameById(testUserId)).thenReturn(testUsername);

            // When / Then
            assertThatThrownBy(() -> commentService.createComment(testRecipeId, testCommentRequest, testToken))
                .isInstanceOf(UnauthorizedAccessException.class)
                .hasMessageContaining("You cannot comment on your own recipe");
                
            assertThatThrownBy(() -> commentService.createComment(testRecipeId, testCommentRequest, testUserId, testToken))
                .isInstanceOf(UnauthorizedAccessException.class)
                .hasMessageContaining("You cannot comment on your own recipe");
                
            assertThatThrownBy(() -> commentService.createComment(testRecipeId, testCommentRequest, testUserId))
                .isInstanceOf(UnauthorizedAccessException.class)
                .hasMessageContaining("You cannot comment on your own recipe");
                
            assertThatThrownBy(() -> commentService.createSystemComment(testRecipeId, testCommentRequest, testUserId, testUsername))
                .isInstanceOf(UnauthorizedAccessException.class)
                .hasMessageContaining("You cannot comment on your own recipe");

            // Verify that recipeRepository.findById was called, but save wasn't
            verify(recipeRepository, times(4)).findById(testRecipeId);
            verify(commentRepository, never()).save(any(Comment.class));
            verifyNoInteractions(commentMapper); 
        }
        
         @Test
        @DisplayName("createSystemComment should throw IllegalArgumentException for null user info")
        void createSystemComment_NullUser_ThrowsException() {
             // Given: Null user ID or username
             UUID nullUserId = null;
             String nullUsername = null;
             String validUsername = "validUser";
             UUID validUserId = UUID.randomUUID();

            // When / Then
            // Test null userId
            assertThatThrownBy(() -> commentService.createSystemComment(testRecipeId, testCommentRequest, nullUserId, validUsername))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("User ID and username are required");
                
            // Test null username
             assertThatThrownBy(() -> commentService.createSystemComment(testRecipeId, testCommentRequest, validUserId, nullUsername))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("User ID and username are required");
                
             // Test empty username
             assertThatThrownBy(() -> commentService.createSystemComment(testRecipeId, testCommentRequest, validUserId, ""))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("User ID and username are required");
                
             // Verify no interactions with repositories or mappers
             verifyNoInteractions(recipeRepository, commentRepository, commentMapper, userService);
        }
    }

    @Nested
    @DisplayName("getCommentsForRecipe Tests")
    class GetCommentsForRecipeTests {

        @Test
        @DisplayName("Should return page of comments for recipe")
        void getCommentsForRecipe_Success() {
            // Given
            Pageable pageable = PageRequest.of(0, 10);
            UUID viewerUserId = UUID.randomUUID(); // ID of the user viewing the comments

            // 1. Mock recipe repo finds the recipe (owned by testRecipeOwnerId)
            when(recipeRepository.findById(testRecipeId)).thenReturn(Optional.of(testRecipe));

            // 2. Mock comment repo returns a page of comments
            // Comment owned by testUserId (different from viewerUserId and recipeOwnerId)
            Comment comment1 = testComment; 
            // Comment owned by the viewer
            Comment comment2 = Comment.builder()
                    .id(UUID.randomUUID()).recipeId(testRecipeId).userId(viewerUserId).username("viewerUser")
                    .content("Viewer's comment").createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now())
                    .build();
            List<Comment> commentsList = List.of(comment1, comment2);
            Page<Comment> commentsPage = new PageImpl<>(commentsList, pageable, commentsList.size());
            when(commentRepository.findByRecipeId(testRecipeId, pageable)).thenReturn(commentsPage);

            // 3. Mock mapper responses for each comment, checking flags
            // For comment1 (owned by testUserId, viewed by viewerUserId, recipe owned by testRecipeOwnerId)
            CommentResponse response1 = new CommentResponse(comment1.getId(), comment1.getContent(), comment1.getUserId(), comment1.getUsername(), comment1.getRecipeId(), comment1.getCreatedAt(), comment1.getUpdatedAt(), false, false);
            when(commentMapper.toResponse(comment1, false, false)).thenReturn(response1); // isOwner=false, isRecipeOwner=false
            
            // For comment2 (owned by viewerUserId, viewed by viewerUserId, recipe owned by testRecipeOwnerId)
            CommentResponse response2 = new CommentResponse(comment2.getId(), comment2.getContent(), comment2.getUserId(), comment2.getUsername(), comment2.getRecipeId(), comment2.getCreatedAt(), comment2.getUpdatedAt(), true, false);
            when(commentMapper.toResponse(comment2, true, false)).thenReturn(response2); // isOwner=true, isRecipeOwner=false
            
            List<CommentResponse> expectedResponses = List.of(response1, response2);
            Page<CommentResponse> expectedPage = new PageImpl<>(expectedResponses, pageable, commentsPage.getTotalElements());

            // When
            Page<CommentResponse> actualPage = commentService.getCommentsForRecipe(testRecipeId, pageable, viewerUserId);

            // Then
            assertThat(actualPage).isEqualTo(expectedPage);
            assertThat(actualPage.getContent()).containsExactlyInAnyOrderElementsOf(expectedResponses);

            // Verify mocks
            verify(recipeRepository).findById(testRecipeId);
            verify(commentRepository).findByRecipeId(testRecipeId, pageable);
            // Verify mapper called correctly for each comment with appropriate flags
            verify(commentMapper).toResponse(comment1, false, false); 
            verify(commentMapper).toResponse(comment2, true, false); 
            verifyNoMoreInteractions(commentMapper);
            verifyNoInteractions(userService); // UserService not directly used here
        }

        @Test
        @DisplayName("Should return empty page if no comments exist")
        void getCommentsForRecipe_NoComments_ReturnsEmptyPage() {
            // Given
            Pageable pageable = PageRequest.of(0, 10);
            UUID viewerUserId = UUID.randomUUID();
            // 1. Recipe repo finds the recipe
            when(recipeRepository.findById(testRecipeId)).thenReturn(Optional.of(testRecipe));
            
            // 2. Comment repo returns an empty page
            Page<Comment> emptyCommentsPage = Page.empty(pageable);
            when(commentRepository.findByRecipeId(testRecipeId, pageable)).thenReturn(emptyCommentsPage);
            
            Page<CommentResponse> expectedPage = Page.empty(pageable);

            // When
            Page<CommentResponse> actualPage = commentService.getCommentsForRecipe(testRecipeId, pageable, viewerUserId);

            // Then
            assertThat(actualPage).isEqualTo(expectedPage);
            assertThat(actualPage.getContent()).isEmpty();

            // Verify mocks
            verify(recipeRepository).findById(testRecipeId);
            verify(commentRepository).findByRecipeId(testRecipeId, pageable);
            // Mapper should not be called if there are no comments
            verifyNoInteractions(commentMapper);
            verifyNoInteractions(userService);
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException if recipe not found")
        void getCommentsForRecipe_RecipeNotFound_ThrowsException() {
            // Given
            Pageable pageable = PageRequest.of(0, 10);
            UUID nonExistentRecipeId = UUID.randomUUID();
            UUID viewerUserId = UUID.randomUUID();
            // Mock recipe repo to return empty optional
            when(recipeRepository.findById(nonExistentRecipeId)).thenReturn(Optional.empty());

            // When / Then
            assertThatThrownBy(() -> commentService.getCommentsForRecipe(nonExistentRecipeId, pageable, viewerUserId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Recipe not found");

            // Verify mocks
            verify(recipeRepository).findById(nonExistentRecipeId);
            // Should fail before calling comment repo or mapper
            verifyNoInteractions(commentRepository, commentMapper, userService);
        }
    }

    @Nested
    @DisplayName("updateComment Tests")
    class UpdateCommentTests {
        
        private CommentRequest updateRequest;
        private Comment existingComment; // Comment before update
        
        @BeforeEach
        void updateSetup() {
             updateRequest = new CommentRequest("Updated comment content");
             // Use the comment from main setUp, owned by testUserId
             existingComment = testComment; 
        }

        @Test
        @DisplayName("Should update comment successfully")
        void updateComment_Success() {
            // Given
            // 1. Comment repo finds the comment to update
            when(commentRepository.findById(testCommentId)).thenReturn(Optional.of(existingComment));
            
            // 2. Recipe repo finds the associated recipe (needed for isRecipeOwner flag)
            // Assuming recipe is owned by testRecipeOwnerId
            when(recipeRepository.findById(testRecipeId)).thenReturn(Optional.of(testRecipe));
            
            // 3. Comment repo saves the updated comment
            Comment updatedComment = existingComment.toBuilder()
                                        .content(updateRequest.content())
                                        .updatedAt(LocalDateTime.now()) // Expect update time change
                                        .build();
            when(commentRepository.save(commentCaptor.capture())).thenReturn(updatedComment);
            
            // 4. Mapper maps the *updated* comment to response
            // The user updating (testUserId) is the owner, but not the recipe owner
            CommentResponse expectedResponse = new CommentResponse(
                updatedComment.getId(), updatedComment.getContent(), updatedComment.getUserId(),
                updatedComment.getUsername(), updatedComment.getRecipeId(), updatedComment.getCreatedAt(),
                updatedComment.getUpdatedAt(), true, false // isOwner=true, isRecipeOwner=false
            );
            when(commentMapper.toResponse(updatedComment, true, false)).thenReturn(expectedResponse);

            // When
            // Call update with the owner's userId (testUserId)
            CommentResponse actualResponse = commentService.updateComment(testCommentId, updateRequest, testUserId);

            // Then
            assertThat(actualResponse).isEqualTo(expectedResponse);
            
            // Verify interactions
            verify(commentRepository).findById(testCommentId);
            verify(recipeRepository).findById(testRecipeId);
            verify(commentRepository).save(any(Comment.class));
            verify(commentMapper).toResponse(updatedComment, true, false);
            verifyNoInteractions(userService); // Not used in update
            
            // Verify captured comment
            Comment captured = commentCaptor.getValue();
            assertThat(captured.getId()).isEqualTo(testCommentId); // Should have ID for update
            assertThat(captured.getContent()).isEqualTo(updateRequest.content());
            assertThat(captured.getUserId()).isEqualTo(testUserId);
            // Could verify updatedAt is different if needed
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException if comment not found")
        void updateComment_CommentNotFound_ThrowsException() {
            // Given
            UUID nonExistentCommentId = UUID.randomUUID();
            // Mock comment repo to return empty optional
            when(commentRepository.findById(nonExistentCommentId)).thenReturn(Optional.empty());

            // When / Then
            assertThatThrownBy(() -> commentService.updateComment(nonExistentCommentId, updateRequest, testUserId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Comment not found");

            // Verify mocks
            verify(commentRepository).findById(nonExistentCommentId);
            // Should fail before calling recipe repo, save, or mapper
            verifyNoInteractions(recipeRepository, commentMapper, userService);
            verify(commentRepository, never()).save(any(Comment.class));
        }

        @Test
        @DisplayName("Should throw UnauthorizedAccessException if user is not owner")
        void updateComment_NotOwner_ThrowsException() {
            // Given
            UUID differentUserId = UUID.randomUUID(); // User attempting update is not the owner
            // Mock comment repo to return the comment owned by testUserId
            when(commentRepository.findById(testCommentId)).thenReturn(Optional.of(existingComment));

            // When / Then
            // Call update with differentUserId
            assertThatThrownBy(() -> commentService.updateComment(testCommentId, updateRequest, differentUserId))
                .isInstanceOf(UnauthorizedAccessException.class)
                .hasMessageContaining("You do not have permission to update this comment");

            // Verify mocks
            verify(commentRepository).findById(testCommentId);
            // Should fail before calling recipe repo, save, or mapper
            verifyNoInteractions(recipeRepository, commentMapper, userService);
            verify(commentRepository, never()).save(any(Comment.class));
        }
    }

    @Nested
    @DisplayName("deleteComment Tests")
    class DeleteCommentTests {
        
        private Comment existingComment; // Comment owned by testUserId
        private Recipe commentedRecipe; // Recipe owned by testRecipeOwnerId
        
        @BeforeEach
        void deleteSetUp() {
            existingComment = testComment; 
            commentedRecipe = testRecipe;
        }

        @Test
        @DisplayName("Should delete comment successfully by owner")
        void deleteComment_ByOwner_Success() {
            // Given
            // 1. Comment repo finds the comment
            when(commentRepository.findById(testCommentId)).thenReturn(Optional.of(existingComment));
            
            // 2. Recipe repo finds the recipe (needed for permission check, even if not recipe owner deleting)
            when(recipeRepository.findById(testRecipeId)).thenReturn(Optional.of(commentedRecipe));
            
            // 3. Mock the delete operation
            doNothing().when(commentRepository).delete(existingComment);

            // When
            // Call delete with the comment owner's userId (testUserId)
            commentService.deleteComment(testCommentId, testUserId);

            // Then
            // Verify mocks
            verify(commentRepository).findById(testCommentId);
            verify(recipeRepository).findById(testRecipeId);
            verify(commentRepository).delete(existingComment);
            verifyNoInteractions(commentMapper, userService); // No other interactions
        }

        @Test
        @DisplayName("Should delete comment successfully by recipe owner")
        void deleteComment_ByRecipeOwner_Success() {
           // Given
           // 1. Comment repo finds the comment (owned by testUserId)
           when(commentRepository.findById(testCommentId)).thenReturn(Optional.of(existingComment));
            
           // 2. Recipe repo finds the recipe (owned by testRecipeOwnerId)
           when(recipeRepository.findById(testRecipeId)).thenReturn(Optional.of(commentedRecipe));
            
           // 3. Mock the delete operation
           doNothing().when(commentRepository).delete(existingComment);

           // When
           // Call delete with the recipe owner's userId (testRecipeOwnerId)
           commentService.deleteComment(testCommentId, testRecipeOwnerId); 

           // Then
           // Verify mocks
           verify(commentRepository).findById(testCommentId);
           verify(recipeRepository).findById(testRecipeId);
           verify(commentRepository).delete(existingComment);
           verifyNoInteractions(commentMapper, userService); 
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException if comment not found")
        void deleteComment_CommentNotFound_ThrowsException() {
            // Given
            UUID nonExistentCommentId = UUID.randomUUID();
            // Mock comment repo to return empty
            when(commentRepository.findById(nonExistentCommentId)).thenReturn(Optional.empty());

            // When / Then
            assertThatThrownBy(() -> commentService.deleteComment(nonExistentCommentId, testUserId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Comment not found");
                
            // Also test with recipe owner ID
             assertThatThrownBy(() -> commentService.deleteComment(nonExistentCommentId, testRecipeOwnerId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Comment not found");

            // Verify mocks
            verify(commentRepository, times(2)).findById(nonExistentCommentId);
            verify(commentRepository, never()).delete(any(Comment.class));
            // Recipe repo shouldn't be called if comment isn't found first
            verifyNoInteractions(recipeRepository, commentMapper, userService);
        }
        
        @Test
        @DisplayName("Should throw ResourceNotFoundException if recipe for comment not found")
        void deleteComment_RecipeNotFound_ThrowsException() {
            // Given
            // Comment exists
            when(commentRepository.findById(testCommentId)).thenReturn(Optional.of(existingComment));
            // But recipe repo returns empty for the comment's recipeId
            when(recipeRepository.findById(testRecipeId)).thenReturn(Optional.empty());

            // When / Then
            // Test with comment owner ID
            assertThatThrownBy(() -> commentService.deleteComment(testCommentId, testUserId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Recipe not found with id: " + testRecipeId);
                
             // Test with recipe owner ID (though recipe owner doesn't exist here)
             assertThatThrownBy(() -> commentService.deleteComment(testCommentId, testRecipeOwnerId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Recipe not found with id: " + testRecipeId);

            // Verify mocks
            verify(commentRepository).findById(testCommentId);
            verify(recipeRepository, times(2)).findById(testRecipeId);
            verify(commentRepository, never()).delete(any(Comment.class));
            verifyNoInteractions(commentMapper, userService);
        }

        @Test
        @DisplayName("Should throw UnauthorizedAccessException if user is neither owner nor recipe owner")
        void deleteComment_NotOwnerNorRecipeOwner_ThrowsException() {
             // Given
             UUID randomUserId = UUID.randomUUID(); // A user who doesn't own the comment or recipe
             // Comment exists (owned by testUserId)
             when(commentRepository.findById(testCommentId)).thenReturn(Optional.of(existingComment));
             // Recipe exists (owned by testRecipeOwnerId)
             when(recipeRepository.findById(testRecipeId)).thenReturn(Optional.of(commentedRecipe));

             // When / Then
             // Attempt deletion with randomUserId
             assertThatThrownBy(() -> commentService.deleteComment(testCommentId, randomUserId))
                .isInstanceOf(UnauthorizedAccessException.class)
                .hasMessageContaining("You do not have permission to delete this comment");

            // Verify mocks
            verify(commentRepository).findById(testCommentId);
            verify(recipeRepository).findById(testRecipeId);
            verify(commentRepository, never()).delete(any(Comment.class));
            verifyNoInteractions(commentMapper, userService);
        }
    }

    @Nested
    @DisplayName("getCommentCount Tests")
    class GetCommentCountTests {

        @Test
        @DisplayName("Should return correct comment count")
        void getCommentCount_Success() {
            // Given
            long expectedCount = 5L;
            // Mock repository count method
            when(commentRepository.countByRecipeId(testRecipeId)).thenReturn(expectedCount);

            // When
            long actualCount = commentService.getCommentCount(testRecipeId);

            // Then
            assertThat(actualCount).isEqualTo(expectedCount);
            
            // Verify mocks
            verify(commentRepository).countByRecipeId(testRecipeId);
            verifyNoInteractions(recipeRepository, commentMapper, userService); 
        }
    }
    
    @Nested
    @DisplayName("getCommentCountsForRecipes Tests")
    class GetCommentCountsForRecipesTests {

        @Test
        @DisplayName("Should return map of comment counts for multiple recipes")
        void getCommentCountsForRecipes_Success() {
            // Given
            UUID recipeId1 = UUID.randomUUID();
            UUID recipeId2 = UUID.randomUUID();
            UUID recipeId3 = UUID.randomUUID(); // Recipe with 0 comments
            Set<UUID> recipeIds = Set.of(recipeId1, recipeId2, recipeId3);
            
            // Mock repository response
            CommentRepository.RecipeCommentCount count1 = mock(CommentRepository.RecipeCommentCount.class);
            when(count1.getRecipeId()).thenReturn(recipeId1);
            when(count1.getCommentCount()).thenReturn(3L);
            
            CommentRepository.RecipeCommentCount count2 = mock(CommentRepository.RecipeCommentCount.class);
            when(count2.getRecipeId()).thenReturn(recipeId2);
            when(count2.getCommentCount()).thenReturn(10L);
            
            List<CommentRepository.RecipeCommentCount> repoResponse = List.of(count1, count2);
            when(commentRepository.countByRecipeIdIn(recipeIds)).thenReturn(repoResponse);
            
            // Expected map (should include recipeId3 with count 0)
            Map<UUID, Long> expectedMap = Map.of(recipeId1, 3L, recipeId2, 10L);
            // Note: The service method itself doesn't add entries for IDs with 0 counts 
            // if they are not returned by the repository query. Let's test the actual behavior.

            // When
            Map<UUID, Long> actualMap = commentService.getCommentCountsForRecipes(recipeIds);

            // Then
            assertThat(actualMap).isEqualTo(expectedMap);
            assertThat(actualMap).doesNotContainKey(recipeId3); // Verify recipeId3 is not included

            // Verify mocks
            verify(commentRepository).countByRecipeIdIn(recipeIds);
            verifyNoInteractions(recipeRepository, commentMapper, userService);
        }

        @Test
        @DisplayName("Should return empty map for empty or null recipe ID set")
        void getCommentCountsForRecipes_EmptyOrNullInput_ReturnsEmptyMap() {
             // Given
             Set<UUID> emptySet = Collections.emptySet();
             Set<UUID> nullSet = null;

            // When
            Map<UUID, Long> actualMapEmpty = commentService.getCommentCountsForRecipes(emptySet);
            Map<UUID, Long> actualMapNull = commentService.getCommentCountsForRecipes(nullSet);

            // Then
            assertThat(actualMapEmpty).isNotNull().isEmpty();
            assertThat(actualMapNull).isNotNull().isEmpty();
            
            // Verify no repository interaction
            verify(commentRepository, never()).countByRecipeIdIn(anySet());
            verifyNoInteractions(recipeRepository, commentMapper, userService);
        }
    }

    // TODO: Add tests for getCommentsByUserId once implemented
    // TODO: Add tests for deleteCommentAsAdmin
    // TODO: Consider testing rate limiting if applyRateLimitingCheck becomes complex
} 