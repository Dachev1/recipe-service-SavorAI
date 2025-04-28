package dev.idachev.recipeservice.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.idachev.recipeservice.model.DifficultyLevel;
import dev.idachev.recipeservice.model.Macros;
import dev.idachev.recipeservice.model.Recipe;
import dev.idachev.recipeservice.repository.RecipeRepository;
import dev.idachev.recipeservice.user.client.UserClient;
import dev.idachev.recipeservice.web.dto.RecipeResponse;
import dev.idachev.recipeservice.web.dto.VoteRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class VoteRecipeITest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private RecipeRepository recipeRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @MockitoBean
    private UserClient userClient;

    private UUID testRecipeId;
    private final UUID testUserId = UUID.randomUUID();
    private final UUID recipeOwnerId = UUID.randomUUID();
    private Authentication testUserAuthentication;

    @BeforeEach
    void setUp() {
        // Mock UserClient behavior
        when(userClient.getUsernameById(any(UUID.class))).thenReturn(ResponseEntity.ok("Test User"));

        // Setup Authentication
        var authorities = Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"));
        this.testUserAuthentication = new UsernamePasswordAuthenticationToken(testUserId, null, authorities);

        // Create a simple test recipe with macros
        Macros macros = Macros.builder()
                .calories(BigDecimal.valueOf(450))
                .proteinGrams(BigDecimal.valueOf(30))
                .carbsGrams(BigDecimal.valueOf(50))
                .fatGrams(BigDecimal.valueOf(15))
                .build();

        Recipe recipe = Recipe.builder()
                .title("Test Recipe for Voting")
                .ingredients("[\"Ingredient 1\", \"Ingredient 2\"]")
                .instructions("1. Step one\\n2. Step two")
                .servingSuggestions("Serve with wine")
                .imageUrl("http://example.com/image.jpg")
                .totalTimeMinutes(30)
                .difficulty(DifficultyLevel.EASY)
                .userId(recipeOwnerId)
                .isAiGenerated(false)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .macros(macros)
                .build();

        testRecipeId = recipeRepository.save(recipe).getId();
    }

    @AfterEach
    void tearDown() {
        if (testRecipeId != null) {
            jdbcTemplate.update("DELETE FROM recipe_votes WHERE recipe_id = ?", testRecipeId);
            recipeRepository.deleteById(testRecipeId);
        }
    }

    @Test
    public void testUpvoteRecipe_Success() throws Exception {
        // Submit upvote and verify response
        MvcResult result = mockMvc.perform(post("/api/v1/recipes/" + testRecipeId + "/vote")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new VoteRequest("UPVOTE")))
                        .with(SecurityMockMvcRequestPostProcessors.authentication(this.testUserAuthentication))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andReturn();

        RecipeResponse response = objectMapper.readValue(
                result.getResponse().getContentAsString(),
                RecipeResponse.class);

        assertNotNull(response);
        assertEquals(1, response.upvotes());
        assertEquals(0, response.downvotes());
        assertEquals("UPVOTE", response.userVote());
    }

    @Test
    public void testDownvoteRecipe_Success() throws Exception {
        // Submit downvote and verify response
        MvcResult result = mockMvc.perform(post("/api/v1/recipes/" + testRecipeId + "/vote")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new VoteRequest("DOWNVOTE")))
                        .with(SecurityMockMvcRequestPostProcessors.authentication(this.testUserAuthentication))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andReturn();

        RecipeResponse response = objectMapper.readValue(
                result.getResponse().getContentAsString(),
                RecipeResponse.class);

        assertNotNull(response);
        assertEquals(0, response.upvotes());
        assertEquals(1, response.downvotes());
        assertEquals("DOWNVOTE", response.userVote());
    }

    @Test
    public void testUpdateVote_Success() throws Exception {
        // First upvote
        mockMvc.perform(post("/api/v1/recipes/" + testRecipeId + "/vote")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new VoteRequest("UPVOTE")))
                        .with(SecurityMockMvcRequestPostProcessors.authentication(this.testUserAuthentication))
                        .with(csrf()))
                .andExpect(status().isOk());

        // Then change to downvote
        MvcResult result = mockMvc.perform(post("/api/v1/recipes/" + testRecipeId + "/vote")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new VoteRequest("DOWNVOTE")))
                        .with(SecurityMockMvcRequestPostProcessors.authentication(this.testUserAuthentication))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andReturn();

        RecipeResponse response = objectMapper.readValue(
                result.getResponse().getContentAsString(),
                RecipeResponse.class);

        assertNotNull(response);
        assertEquals(0, response.upvotes());
        assertEquals(1, response.downvotes());
        assertEquals("DOWNVOTE", response.userVote());
    }

    @Test
    public void testInvalidVoteType() throws Exception {
        mockMvc.perform(post("/api/v1/recipes/" + testRecipeId + "/vote")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new VoteRequest("invalid")))
                        .with(SecurityMockMvcRequestPostProcessors.authentication(this.testUserAuthentication))
                        .with(csrf()))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void testVoteNonExistentRecipe() throws Exception {
        mockMvc.perform(post("/api/v1/recipes/" + UUID.randomUUID() + "/vote")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new VoteRequest("UPVOTE")))
                        .with(SecurityMockMvcRequestPostProcessors.authentication(this.testUserAuthentication))
                        .with(csrf()))
                .andExpect(status().isNotFound());
    }
} 