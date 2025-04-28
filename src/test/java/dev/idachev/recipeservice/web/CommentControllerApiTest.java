package dev.idachev.recipeservice.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import dev.idachev.recipeservice.exception.ResourceNotFoundException;
import dev.idachev.recipeservice.exception.UnauthorizedAccessException;
import dev.idachev.recipeservice.service.CommentService;
import dev.idachev.recipeservice.web.dto.CommentRequest;
import dev.idachev.recipeservice.web.dto.CommentResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.MethodParameter;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@ActiveProfiles("test")
public class CommentControllerApiTest {

    private MockMvc mockMvc;

    @Mock
    private CommentService commentService;

    @InjectMocks
    private CommentController commentController;

    private ObjectMapper objectMapper;

    private UUID testUserId;
    private UUID testRecipeId;
    private UUID testCommentId;
    private CommentRequest testCommentRequest;
    private CommentResponse testCommentResponse;

    /**
     * Custom ArgumentResolver to handle @AuthenticationPrincipal annotation in controller methods
     */
    static class AuthenticationPrincipalArgumentResolver implements HandlerMethodArgumentResolver {
        private final UUID userId;

        public AuthenticationPrincipalArgumentResolver(UUID userId) {
            this.userId = userId;
        }

        @Override
        public boolean supportsParameter(MethodParameter parameter) {
            return parameter.getParameterAnnotation(AuthenticationPrincipal.class) != null;
        }

        @Override
        public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                                      NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
            return userId;
        }
    }

    @BeforeEach
    void setUp() {
        testUserId = UUID.randomUUID();
        testRecipeId = UUID.randomUUID();
        testCommentId = UUID.randomUUID();
        testCommentRequest = new CommentRequest("This is a test comment");

        LocalDateTime now = LocalDateTime.now();
        testCommentResponse = new CommentResponse(
                testCommentId,
                "This is a test comment",
                testUserId,
                "testUser",
                testRecipeId,
                now,
                now,
                true,
                false
        );

        // Setup MockMvc with custom AuthenticationPrincipal resolver
        mockMvc = MockMvcBuilders.standaloneSetup(commentController)
                .setCustomArgumentResolvers(
                        new PageableHandlerMethodArgumentResolver(),
                        new AuthenticationPrincipalArgumentResolver(testUserId)
                )
                .build();

        // Configure ObjectMapper with JavaTimeModule for date handling
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
    }

    @Nested
    @DisplayName("Get Comments Tests")
    class GetCommentsTests {
        @Test
        @DisplayName("Should get comments for a recipe")
        void getCommentsForRecipe_Success() throws Exception {
            // Given
            Pageable pageable = PageRequest.of(0, 10);
            List<CommentResponse> comments = List.of(testCommentResponse);
            PageImpl<CommentResponse> commentPage = new PageImpl<>(comments, pageable, comments.size());

            given(commentService.getCommentsForRecipe(eq(testRecipeId), any(Pageable.class), eq(testUserId)))
                    .willReturn(commentPage);

            // When
            ResultActions response = mockMvc.perform(
                    get("/api/v1/recipes/{recipeId}/comments", testRecipeId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .param("page", "0")
                            .param("size", "10")
            ).andDo(print());

            // Then
            response
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content", hasSize(1)))
                    .andExpect(jsonPath("$.content[0].id", is(testCommentId.toString())))
                    .andExpect(jsonPath("$.content[0].content", is("This is a test comment")))
                    .andExpect(jsonPath("$.content[0].userId", is(testUserId.toString())))
                    .andExpect(jsonPath("$.content[0].username", is("testUser")))
                    .andExpect(jsonPath("$.content[0].recipeId", is(testRecipeId.toString())))
                    .andExpect(jsonPath("$.content[0].isOwner", is(true)))
                    .andExpect(jsonPath("$.content[0].isRecipeOwner", is(false)));

            verify(commentService).getCommentsForRecipe(eq(testRecipeId), any(Pageable.class), eq(testUserId));
        }

        @Test
        @DisplayName("Should return empty page when recipe has no comments")
        void getCommentsForRecipe_EmptyPage() throws Exception {
            // Given
            Pageable pageable = PageRequest.of(0, 10);
            PageImpl<CommentResponse> emptyPage = new PageImpl<>(Collections.emptyList(), pageable, 0);

            given(commentService.getCommentsForRecipe(eq(testRecipeId), any(Pageable.class), eq(testUserId)))
                    .willReturn(emptyPage);

            // When
            ResultActions response = mockMvc.perform(
                    get("/api/v1/recipes/{recipeId}/comments", testRecipeId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .param("page", "0")
                            .param("size", "10")
            ).andDo(print());

            // Then
            response
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content", hasSize(0)))
                    .andExpect(jsonPath("$.totalElements", is(0)));

            verify(commentService).getCommentsForRecipe(eq(testRecipeId), any(Pageable.class), eq(testUserId));
        }

        @Test
        @DisplayName("Should return 404 when recipe not found")
        void getCommentsForRecipe_RecipeNotFound() throws Exception {
            // Given
            given(commentService.getCommentsForRecipe(eq(testRecipeId), any(Pageable.class), eq(testUserId)))
                    .willThrow(new ResourceNotFoundException("Recipe", "id", testRecipeId));

            // When
            ResultActions response = mockMvc.perform(
                    get("/api/v1/recipes/{recipeId}/comments", testRecipeId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .param("page", "0")
                            .param("size", "10")
            ).andDo(print());

            // Then
            response.andExpect(status().isNotFound());
            verify(commentService).getCommentsForRecipe(eq(testRecipeId), any(Pageable.class), eq(testUserId));
        }
    }

    @Nested
    @DisplayName("Add Comment Tests")
    class AddCommentTests {
        @Test
        @DisplayName("Should add comment to a recipe")
        void addComment_Success() throws Exception {
            // Given
            given(commentService.createComment(eq(testRecipeId), eq(testCommentRequest), eq(testUserId)))
                    .willReturn(testCommentResponse);

            // When
            ResultActions response = mockMvc.perform(
                    post("/api/v1/recipes/{recipeId}/comments", testRecipeId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(testCommentRequest))
            ).andDo(print());

            // Then
            response
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id", is(testCommentId.toString())))
                    .andExpect(jsonPath("$.content", is("This is a test comment")))
                    .andExpect(jsonPath("$.userId", is(testUserId.toString())))
                    .andExpect(jsonPath("$.username", is("testUser")))
                    .andExpect(jsonPath("$.recipeId", is(testRecipeId.toString())))
                    .andExpect(jsonPath("$.isOwner", is(true)))
                    .andExpect(jsonPath("$.isRecipeOwner", is(false)));

            verify(commentService).createComment(eq(testRecipeId), eq(testCommentRequest), eq(testUserId));
        }

        @Test
        @DisplayName("Should return 400 when comment content is invalid")
        void addComment_InvalidContent() throws Exception {
            // Given
            CommentRequest invalidRequest = new CommentRequest("");

            // When
            ResultActions response = mockMvc.perform(
                    post("/api/v1/recipes/{recipeId}/comments", testRecipeId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalidRequest))
            ).andDo(print());

            // Then
            response.andExpect(status().isBadRequest());
            verify(commentService, never()).createComment(any(UUID.class), any(CommentRequest.class), any(UUID.class));
        }
    }

    @Nested
    @DisplayName("Update Comment Tests")
    class UpdateCommentTests {
        @Test
        @DisplayName("Should update a comment")
        void updateComment_Success() throws Exception {
            // Given
            CommentRequest updateRequest = new CommentRequest("Updated comment");
            CommentResponse updatedComment = new CommentResponse(
                    testCommentId,
                    "Updated comment",
                    testUserId,
                    "testUser",
                    testRecipeId,
                    LocalDateTime.now(),
                    LocalDateTime.now(),
                    true,
                    false
            );

            given(commentService.updateComment(eq(testCommentId), eq(updateRequest.content()), eq(testUserId)))
                    .willReturn(updatedComment);

            // When
            ResultActions response = mockMvc.perform(
                    put("/api/v1/recipes/comments/{commentId}", testCommentId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(updateRequest))
            ).andDo(print());

            // Then
            response
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id", is(testCommentId.toString())))
                    .andExpect(jsonPath("$.content", is("Updated comment")))
                    .andExpect(jsonPath("$.userId", is(testUserId.toString())))
                    .andExpect(jsonPath("$.username", is("testUser")))
                    .andExpect(jsonPath("$.recipeId", is(testRecipeId.toString())))
                    .andExpect(jsonPath("$.isOwner", is(true)))
                    .andExpect(jsonPath("$.isRecipeOwner", is(false)));

            verify(commentService).updateComment(eq(testCommentId), eq(updateRequest.content()), eq(testUserId));
        }

        @Test
        @DisplayName("Should return 400 when update content is invalid")
        void updateComment_InvalidContent() throws Exception {
            // Given
            CommentRequest invalidRequest = new CommentRequest("");

            // When
            ResultActions response = mockMvc.perform(
                    put("/api/v1/recipes/comments/{commentId}", testCommentId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalidRequest))
            ).andDo(print());

            // Then
            response.andExpect(status().isBadRequest());
            verify(commentService, never()).updateComment(any(), any(), any());
        }

        @Test
        @DisplayName("Should return 404 when comment not found")
        void updateComment_CommentNotFound() throws Exception {
            // Given
            CommentRequest updateRequest = new CommentRequest("Updated comment");
            given(commentService.updateComment(eq(testCommentId), eq(updateRequest.content()), eq(testUserId)))
                    .willThrow(new ResourceNotFoundException("Comment", "id", testCommentId));

            // When
            ResultActions response = mockMvc.perform(
                    put("/api/v1/recipes/comments/{commentId}", testCommentId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(updateRequest))
            ).andDo(print());

            // Then
            response.andExpect(status().isNotFound());
            verify(commentService).updateComment(eq(testCommentId), eq(updateRequest.content()), eq(testUserId));
        }

        @Test
        @DisplayName("Should return 403 when user attempts to update another user's comment")
        void updateComment_Forbidden() throws Exception {
            // Given
            CommentRequest updateRequest = new CommentRequest("Updated comment");
            given(commentService.updateComment(eq(testCommentId), eq(updateRequest.content()), eq(testUserId)))
                    .willThrow(new UnauthorizedAccessException("You are not authorized to update this comment"));

            // When
            ResultActions response = mockMvc.perform(
                    put("/api/v1/recipes/comments/{commentId}", testCommentId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(updateRequest))
            ).andDo(print());

            // Then
            response.andExpect(status().isForbidden());
            verify(commentService).updateComment(eq(testCommentId), eq(updateRequest.content()), eq(testUserId));
        }
    }

    @Nested
    @DisplayName("Delete Comment Tests")
    class DeleteCommentTests {
        @Test
        @DisplayName("Should delete a comment")
        void deleteComment_Success() throws Exception {
            // Given
            doNothing().when(commentService).deleteComment(eq(testCommentId), eq(testUserId));

            // When
            ResultActions response = mockMvc.perform(
                    delete("/api/v1/recipes/comments/{commentId}", testCommentId)
                            .contentType(MediaType.APPLICATION_JSON)
            ).andDo(print());

            // Then
            response.andExpect(status().isNoContent());
            verify(commentService).deleteComment(eq(testCommentId), eq(testUserId));
        }

        @Test
        @DisplayName("Should return 404 when comment not found")
        void deleteComment_CommentNotFound() throws Exception {
            // Given
            doThrow(new ResourceNotFoundException("Comment", "id", testCommentId))
                    .when(commentService).deleteComment(eq(testCommentId), eq(testUserId));

            // When
            ResultActions response = mockMvc.perform(
                    delete("/api/v1/recipes/comments/{commentId}", testCommentId)
                            .contentType(MediaType.APPLICATION_JSON)
            ).andDo(print());

            // Then
            response.andExpect(status().isNotFound());
            verify(commentService).deleteComment(eq(testCommentId), eq(testUserId));
        }

        @Test
        @DisplayName("Should return 403 when user attempts to delete another user's comment")
        void deleteComment_Forbidden() throws Exception {
            // Given
            doThrow(new UnauthorizedAccessException("You are not authorized to delete this comment"))
                    .when(commentService).deleteComment(eq(testCommentId), eq(testUserId));

            // When
            ResultActions response = mockMvc.perform(
                    delete("/api/v1/recipes/comments/{commentId}", testCommentId)
                            .contentType(MediaType.APPLICATION_JSON)
            ).andDo(print());

            // Then
            response.andExpect(status().isForbidden());
            verify(commentService).deleteComment(eq(testCommentId), eq(testUserId));
        }
    }
}