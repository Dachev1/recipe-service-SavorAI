package dev.idachev.recipeservice.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.idachev.recipeservice.model.DifficultyLevel;
import dev.idachev.recipeservice.repository.RecipeRepository;
import dev.idachev.recipeservice.web.dto.MacrosDto;
import dev.idachev.recipeservice.web.dto.RecipeRequest;
import dev.idachev.recipeservice.web.dto.RecipeResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class CreateRecipeITest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private RecipeRepository recipeRepository;
    
    private UUID createdRecipeId;
    
    @AfterEach
    void cleanup() {
        if (createdRecipeId != null) {
            recipeRepository.deleteById(createdRecipeId);
        }
    }

    @Test
    @WithMockUser
    public void testCreateRecipe_Success() throws Exception {
        RecipeRequest request = new RecipeRequest(
                "Test Recipe",
                "Serve with wine",
                "1. Step one\n2. Step two",
                "http://example.com/image.jpg",
                List.of("Ingredient 1", "Ingredient 2"),
                30,
                DifficultyLevel.MEDIUM,
                false,
                new MacrosDto(
                    BigDecimal.valueOf(450),
                    BigDecimal.valueOf(30),
                    BigDecimal.valueOf(50),
                    BigDecimal.valueOf(15)
                )
        );

        MvcResult result = mockMvc.perform(post("/api/v1/recipes")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
                .with(csrf()))
            .andExpect(status().isCreated())
            .andReturn();

        RecipeResponse response = objectMapper.readValue(
                result.getResponse().getContentAsString(),
                RecipeResponse.class);

        assertNotNull(response);
        assertNotNull(response.id());
        createdRecipeId = response.id();
        
        assertEquals("Test Recipe", response.title());
        assertEquals("Serve with wine", response.servingSuggestions());
        assertEquals(DifficultyLevel.MEDIUM, response.difficulty());
        assertEquals(2, response.ingredients().size());
    }

    @Test
    @WithMockUser
    public void testCreateRecipe_InvalidRequest() throws Exception {
        RecipeRequest invalidRequest = new RecipeRequest(
                "",
                "Serve with wine",
                "",
                "http://example.com/image.jpg",
                List.of(),
                30,
                DifficultyLevel.MEDIUM,
                false,
                null
        );

        mockMvc.perform(post("/api/v1/recipes")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequest))
                .with(csrf()))
            .andExpect(status().isBadRequest());
    }
} 