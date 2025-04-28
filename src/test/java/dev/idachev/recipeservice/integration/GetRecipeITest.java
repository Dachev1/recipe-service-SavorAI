package dev.idachev.recipeservice.integration;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.idachev.recipeservice.model.DifficultyLevel;
import dev.idachev.recipeservice.model.Recipe;
import dev.idachev.recipeservice.repository.RecipeRepository;
import dev.idachev.recipeservice.user.client.UserClient;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.ResponseEntity;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class GetRecipeITest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private RecipeRepository recipeRepository;

    private UUID testRecipeId;
    private final UUID testUserId = UUID.randomUUID();
    private final Authentication testAuthentication = new UsernamePasswordAuthenticationToken(testUserId, null, Collections.emptyList());

    @MockitoBean
    private UserClient userClient;

    @BeforeEach
    void setUp() {
        recipeRepository.deleteAll();
        Recipe recipe = Recipe.builder()
                .title("Test Recipe for Retrieval")
                .ingredients("[\"Ingredient A\", \"Ingredient B\"]")
                .instructions("Test Instructions")
                .difficulty(DifficultyLevel.MEDIUM)
                .totalTimeMinutes(45)
                .tags(List.of("GetTest"))
                .userId(testUserId)
                .build();
        testRecipeId = recipeRepository.save(recipe).getId();
    }

    @AfterEach
    void tearDown() {
        if (testRecipeId != null) {
            recipeRepository.deleteById(testRecipeId);
        }
    }

    @Test
    void testGetRecipeById_Success() throws Exception {
        when(userClient.getUsernameById(testUserId)).thenReturn(ResponseEntity.ok("Mock Test User"));

        MvcResult result = mockMvc.perform(get("/api/v1/recipes/" + testRecipeId)
                        .with(authentication(testAuthentication)))
                .andExpect(status().isOk())
                .andReturn();

        String responseBody = result.getResponse().getContentAsString();
        Map<String, Object> responseMap = objectMapper.readValue(responseBody, new TypeReference<Map<String, Object>>() {
        });

        assertNotNull(responseMap);
        assertEquals(testRecipeId.toString(), responseMap.get("id"));
        assertEquals("Test Recipe for Retrieval", responseMap.get("title"));
    }

    @Test
    void testGetRecipeById_NotFound() throws Exception {
        UUID nonExistentId = UUID.randomUUID();
        mockMvc.perform(get("/api/v1/recipes/" + nonExistentId)
                        .with(authentication(testAuthentication)))
                .andExpect(status().isNotFound());
    }

    @Test
    void testGetRecipeById_Unauthenticated() throws Exception {
        mockMvc.perform(get("/api/v1/recipes/" + testRecipeId))
                .andExpect(status().isUnauthorized());
    }
} 