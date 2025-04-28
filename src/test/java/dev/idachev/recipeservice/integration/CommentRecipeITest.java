package dev.idachev.recipeservice.integration;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.idachev.recipeservice.model.DifficultyLevel;
import dev.idachev.recipeservice.model.Recipe;
import dev.idachev.recipeservice.repository.CommentRepository;
import dev.idachev.recipeservice.repository.RecipeRepository;
import dev.idachev.recipeservice.user.service.UserService;
import dev.idachev.recipeservice.web.dto.CommentRequest;
import dev.idachev.recipeservice.web.dto.CommentResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class CommentRecipeITest {

    private static final UUID TEST_USER_ID = UUID.randomUUID();
    private static final String TEST_USER_NAME = "test-user";

    // Define Authentication object once for reuse
    private final Authentication testAuthentication = new UsernamePasswordAuthenticationToken(TEST_USER_ID, null,
            Collections.emptyList());

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private RecipeRepository recipeRepository;

    @Autowired
    private CommentRepository commentRepository;

    @MockitoBean
    private UserService userService;

    private Recipe testRecipe;
    private final UUID recipeCreatorId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        // Remove commented out lines regarding ObjectMapper configuration

        // userService mock setup
        when(userService.getUsernameById(any(UUID.class)))
                .thenAnswer(invocation -> {
                    UUID userId = invocation.getArgument(0);
                    if (TEST_USER_ID.equals(userId)) {
                        return TEST_USER_NAME;
                    } else if (recipeCreatorId.equals(userId)) {
                        return "recipe-creator";
                    }
                    return "user-" + userId.toString().substring(0, 4);
                });

        // Create and save the test recipe
        testRecipe = Recipe.builder()
                .title("Test Recipe for Comments")
                .instructions("Step 1, Step 2")
                .ingredients("Ingredient 1, Ingredient 2")
                .userId(recipeCreatorId)
                .difficulty(DifficultyLevel.EASY)
                .totalTimeMinutes(30)
                .tags(List.of("Test", "CommentTest"))
                .build();
        recipeRepository.save(testRecipe);

        // Remove commented out SecurityContextHolder line
        // SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        commentRepository.deleteAll();
        recipeRepository.deleteAll();
    }

    private UUID createTestComment(UUID recipeId, String content) throws Exception {
        CommentRequest request = new CommentRequest(content);
        MvcResult result = mockMvc.perform(post("/api/v1/recipes/" + recipeId + "/comments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .with(csrf())
                        .with(authentication(testAuthentication)))
                .andExpect(status().isCreated())
                .andReturn();
        CommentResponse response = objectMapper.readValue(result.getResponse().getContentAsString(),
                CommentResponse.class);
        assertNotNull(response.id(), "Comment ID should not be null after creation");
        return response.id();
    }

    @Test
    void testAddComment_Success() throws Exception {
        CommentRequest request = new CommentRequest("This is delicious!");

        MvcResult result = mockMvc.perform(post("/api/v1/recipes/" + testRecipe.getId() + "/comments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .with(csrf())
                        .with(authentication(testAuthentication)))
                .andExpect(status().isCreated())
                .andReturn();

        CommentResponse response = objectMapper.readValue(
                result.getResponse().getContentAsString(),
                CommentResponse.class);

        assertNotNull(response.id());
        assertEquals("This is delicious!", response.content());
        assertEquals("test-user", response.username(), "Username should match the mock user");
        assertNotNull(response.userId(), "User ID should be populated by the service");
        assertEquals(testRecipe.getId(), response.recipeId());
        assertTrue(response.isOwner(), "The user posting the comment should be the owner");
        assertFalse(response.isRecipeOwner(), "The user is not the owner of the recipe");

        assertTrue(commentRepository.findById(response.id()).isPresent(),
                "Comment should be saved in the repository");
    }

    @Test
    @SuppressWarnings("unchecked")
    void testGetComments_Success() throws Exception {
        UUID createdCommentId = createTestComment(testRecipe.getId(), "Great recipe!");

        MvcResult result = mockMvc.perform(get("/api/v1/recipes/" + testRecipe.getId() + "/comments")
                        .with(csrf())
                        .with(authentication(testAuthentication)))
                .andExpect(status().isOk())
                .andReturn();

        String json = result.getResponse().getContentAsString();

        Map<String, Object> pageMap = objectMapper.readValue(
                json,
                new TypeReference<Map<String, Object>>() {
                });

        assertEquals(1, ((Number) pageMap.get("totalElements")).intValue(), "Should retrieve one comment");
        List<Map<String, Object>> contentList = (List<Map<String, Object>>) pageMap.get("content");
        assertFalse(contentList.isEmpty(), "Comment list should not be empty");
        assertEquals(1, contentList.size(), "Content list should contain one comment");

        Map<String, Object> commentMap = contentList.get(0);
        assertEquals(createdCommentId.toString(), commentMap.get("id"));
        assertEquals("Great recipe!", commentMap.get("content"));
        assertEquals(TEST_USER_NAME, commentMap.get("username"), "Username should match the comment creator");
        assertNotNull(commentMap.get("userId"));
        assertEquals(testRecipe.getId().toString(), commentMap.get("recipeId"));
    }

    @Test
    void testUpdateComment_Success() throws Exception {
        UUID commentToUpdateId = createTestComment(testRecipe.getId(), "Initial comment");

        CommentRequest updateRequest = new CommentRequest("Updated comment content");

        MvcResult result = mockMvc.perform(put("/api/v1/recipes/comments/" + commentToUpdateId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest))
                        .with(csrf())
                        .with(authentication(testAuthentication)))
                .andExpect(status().isOk())
                .andReturn();

        CommentResponse response = objectMapper.readValue(
                result.getResponse().getContentAsString(),
                CommentResponse.class);

        assertEquals(commentToUpdateId, response.id());
        assertEquals("Updated comment content", response.content());
        assertEquals("test-user", response.username());
        assertNotNull(response.userId());
        assertEquals(testRecipe.getId(), response.recipeId());
        assertTrue(response.isOwner(), "User should be marked as owner in the response");

        dev.idachev.recipeservice.model.Comment updatedComment = commentRepository.findById(commentToUpdateId)
                .orElseThrow();
        assertEquals("Updated comment content", updatedComment.getContent(),
                "Comment content should be updated in the repository");
    }

    @Test
    void testDeleteComment_Success() throws Exception {
        UUID commentToDeleteId = createTestComment(testRecipe.getId(), "Comment to be deleted");

        mockMvc.perform(delete("/api/v1/recipes/comments/" + commentToDeleteId)
                        .with(csrf())
                        .with(authentication(testAuthentication)))
                .andExpect(status().isNoContent());

        assertFalse(commentRepository.findById(commentToDeleteId).isPresent(),
                "Comment should be deleted from the repository");
    }

    @Test
    void testAddComment_RecipeNotFound() throws Exception {
        CommentRequest request = new CommentRequest("Comment for non-existent recipe");
        UUID nonExistentRecipeId = UUID.randomUUID();

        mockMvc.perform(post("/api/v1/recipes/" + nonExistentRecipeId + "/comments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .with(csrf())
                        .with(authentication(testAuthentication)))
                .andExpect(status().isNotFound());
    }

    @Test
    void testAddComment_InvalidRequest() throws Exception {
        CommentRequest invalidRequest = new CommentRequest("");

        mockMvc.perform(post("/api/v1/recipes/" + testRecipe.getId() + "/comments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest))
                        .with(csrf())
                        .with(authentication(testAuthentication)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testAddComment_NotOwnRecipe() throws Exception {
        CommentRequest request = new CommentRequest("Commenting on someone else's recipe");

        MvcResult result = mockMvc.perform(post("/api/v1/recipes/" + testRecipe.getId() + "/comments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .with(csrf())
                        .with(authentication(testAuthentication)))
                .andExpect(status().isCreated())
                .andReturn();

        CommentResponse response = objectMapper.readValue(
                result.getResponse().getContentAsString(),
                CommentResponse.class);

        assertNotNull(response.id());
        assertEquals("Commenting on someone else's recipe", response.content());
        assertEquals("test-user", response.username());
        assertNotNull(response.userId());
        assertEquals(testRecipe.getId(), response.recipeId());
        assertTrue(response.isOwner(), "User is the owner of the comment");
        assertFalse(response.isRecipeOwner(), "User is not the owner of the recipe");
    }
}