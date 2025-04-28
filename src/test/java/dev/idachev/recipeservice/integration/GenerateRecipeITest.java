package dev.idachev.recipeservice.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.idachev.recipeservice.infrastructure.ai.AIService;
import dev.idachev.recipeservice.model.DifficultyLevel;
import dev.idachev.recipeservice.web.dto.MacrosDto;
import dev.idachev.recipeservice.web.dto.SimplifiedRecipeResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class GenerateRecipeITest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AIService aiService;

    private final UUID testUserId = UUID.randomUUID();

    @Test
    public void testGenerateRecipe_Success() throws Exception {
        // Setup test data
        List<String> ingredients = List.of("chicken", "rice", "garlic");
        MacrosDto macros = new MacrosDto(
                BigDecimal.valueOf(450), BigDecimal.valueOf(30),
                BigDecimal.valueOf(50), BigDecimal.valueOf(15));

        SimplifiedRecipeResponse mockResponse = new SimplifiedRecipeResponse(
                "Garlic Chicken with Rice", "A delicious chicken dish",
                "1. Season chicken\n2. Cook rice",
                List.of("1 lb chicken", "2 cups rice"),
                "http://example.com/image.jpg", 30, macros,
                DifficultyLevel.MEDIUM, "Serve hot with vegetables", null);

        when(aiService.generateRecipeFromIngredients(anyList())).thenReturn(mockResponse);

        // Execute test
        MvcResult result = mockMvc.perform(post("/api/v1/recipes/generate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(ingredients))
                        .with(SecurityMockMvcRequestPostProcessors.user(testUserId.toString()).roles("USER"))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andReturn();

        // Verify results
        SimplifiedRecipeResponse response = objectMapper.readValue(
                result.getResponse().getContentAsString(),
                SimplifiedRecipeResponse.class);

        assertNotNull(response);
        assertEquals("Garlic Chicken with Rice", response.title());
        assertEquals(DifficultyLevel.MEDIUM, response.difficulty());
    }

    @Test
    public void testGenerateRecipe_EmptyIngredients() throws Exception {
        mockMvc.perform(post("/api/v1/recipes/generate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(List.of()))
                        .with(SecurityMockMvcRequestPostProcessors.user(testUserId.toString()).roles("USER"))
                        .with(csrf()))
                .andExpect(status().isBadRequest());
    }
} 