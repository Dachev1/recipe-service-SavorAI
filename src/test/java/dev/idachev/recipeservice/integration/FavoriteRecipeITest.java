package dev.idachev.recipeservice.integration;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.idachev.recipeservice.model.DifficultyLevel;
import dev.idachev.recipeservice.model.FavoriteRecipe;
import dev.idachev.recipeservice.model.Recipe;
import dev.idachev.recipeservice.repository.FavoriteRecipeRepository;
import dev.idachev.recipeservice.repository.RecipeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
public class FavoriteRecipeITest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private RecipeRepository recipeRepository;

    @Autowired
    private FavoriteRecipeRepository favoriteRecipeRepository;

    private UUID recipeId;
    private final UUID testUserId = UUID.randomUUID();

    private final Authentication testAuthentication = new UsernamePasswordAuthenticationToken(testUserId, null, Collections.emptyList());

    @BeforeEach
    void setUp() {
        // Create a test recipe - aligning fields with CommentRecipeITest
        Recipe recipe = Recipe.builder()
                .title("Test Recipe")
                .instructions("1. Step one\n2. Step two") // Use instructions
                .ingredients("[\"Ingredient 1\", \"Ingredient 2\"]")
                .totalTimeMinutes(30)
                .difficulty(DifficultyLevel.EASY)
                .userId(UUID.randomUUID()) // Different from test user
                .tags(List.of("Test", "Favorite")) // Add tags
                .build();

        Recipe savedRecipe = recipeRepository.save(recipe);
        recipeId = savedRecipe.getId();
    }

    /*
    @AfterEach
    void tearDown() {
        // @Transactional handles rollback, explicit deletion likely redundant.
        // favoriteRecipeRepository.deleteByUserIdAndRecipeId(testUserId, recipeId);
        if (recipeId != null) {
            recipeRepository.deleteById(recipeId);
        }
    }
    */

    @Test
    public void testAddRecipeToFavorites_Success() throws Exception {
        // When
        MvcResult result = mockMvc.perform(post("/api/v1/favorites/" + recipeId)
                        .with(authentication(testAuthentication))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andReturn();

        // Then
        assertTrue(favoriteRecipeRepository.existsByUserIdAndRecipeId(testUserId, recipeId));
    }

    @Test
    public void testRemoveRecipeFromFavorites_Success() throws Exception {
        // Given: Create the favorite directly in the test
        FavoriteRecipe favorite = FavoriteRecipe.builder()
                .userId(testUserId)
                .recipeId(recipeId)
                .build();
        favoriteRecipeRepository.save(favorite);
        // Ensure it exists before trying to delete
        assertTrue(favoriteRecipeRepository.existsByUserIdAndRecipeId(testUserId, recipeId), "Favorite should exist before deletion");

        // When
        mockMvc.perform(delete("/api/v1/favorites/" + recipeId)
                        .with(authentication(testAuthentication))
                        .with(csrf()))
                .andExpect(status().isNoContent());

        // Then
        assertFalse(favoriteRecipeRepository.existsByUserIdAndRecipeId(testUserId, recipeId));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testGetFavoriteRecipes() throws Exception {
        // Given: Create the favorite directly in the test
        FavoriteRecipe favorite = FavoriteRecipe.builder()
                .userId(testUserId)
                .recipeId(recipeId)
                .build();
        favoriteRecipeRepository.save(favorite);

        // When
        MvcResult result = mockMvc.perform(get("/api/v1/favorites")
                        .with(authentication(testAuthentication)))
                .andExpect(status().isOk())
                .andReturn();

        // Then: Deserialize into Map and assert against Map structure
        String json = result.getResponse().getContentAsString();
        Map<String, Object> pageMap = objectMapper.readValue(
                json,
                new TypeReference<Map<String, Object>>() {
                }
        );

        assertNotNull(pageMap, "Response map should not be null");

        // TODO: Deserialize response into Page<FavoriteRecipeDto> or similar instead of Map for type safety.

        // Check pagination metadata (adjust keys if needed based on actual response)
        assertTrue(pageMap.containsKey("content"), "Response should contain 'content'");
        assertTrue(pageMap.containsKey("totalElements"), "Response should contain 'totalElements'");

        // Assert against the Map structure
        assertEquals(1, ((Number) pageMap.get("totalElements")).intValue(), "Should retrieve one favorite");
        List<Map<String, Object>> contentList = (List<Map<String, Object>>) pageMap.get("content");
        assertNotNull(contentList, "Content list should not be null");
        assertFalse(contentList.isEmpty(), "Favorite list should not be empty");
        assertEquals(1, contentList.size(), "Content list should contain one favorite");

        // Get the favorite object first, then the nested recipe object
        Map<String, Object> favoriteMap = contentList.get(0);
        assertNotNull(favoriteMap.get("recipe"), "Favorite object should contain a nested 'recipe' object");
        Map<String, Object> recipeMap = (Map<String, Object>) favoriteMap.get("recipe");

        // Assert on the nested recipe details
        assertEquals(recipeId.toString(), recipeMap.get("id"));
        assertEquals("Test Recipe", recipeMap.get("title")); // Example assertion on recipe details
    }

    @Test
    public void testAddRecipeToFavorites_NonExistentRecipe() throws Exception {
        // Given
        UUID nonExistentId = UUID.randomUUID();

        // When-Then
        mockMvc.perform(post("/api/v1/favorites/" + nonExistentId)
                        .with(authentication(testAuthentication))
                        .with(csrf()))
                .andExpect(status().isNotFound());
    }

    @Test
    public void testAddAlreadyFavoritedRecipe() throws Exception {
        // Given: Create the favorite directly in the test
        FavoriteRecipe favorite = FavoriteRecipe.builder()
                .userId(testUserId)
                .recipeId(recipeId)
                .build();
        favoriteRecipeRepository.save(favorite);
        // Ensure it exists before trying to add again
        assertTrue(favoriteRecipeRepository.existsByUserIdAndRecipeId(testUserId, recipeId), "Favorite should exist before adding again");

        // When-Then
        mockMvc.perform(post("/api/v1/favorites/" + recipeId)
                        .with(authentication(testAuthentication))
                        .with(csrf()))
                .andExpect(status().isOk()); // Should still return OK even if already favorited

        // Assert that only one favorite exists for this user and recipe combo
        assertEquals(1, favoriteRecipeRepository.findByUserIdAndRecipeId(testUserId, recipeId).stream().count());
    }
} 