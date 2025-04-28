package dev.idachev.recipeservice.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.idachev.recipeservice.model.DifficultyLevel;
import dev.idachev.recipeservice.model.Macros;
import dev.idachev.recipeservice.model.Recipe;
import dev.idachev.recipeservice.repository.RecipeRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class SearchRecipeITest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private RecipeRepository recipeRepository;

    private final List<UUID> createdRecipeIds = new ArrayList<>();
    private static final UUID TEST_USER_ID = UUID.randomUUID();
    private static final UUID ANOTHER_USER_ID = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        // Create test recipes
        Recipe recipe1 = Recipe.builder()
                .title("Pasta Carbonara")
                .ingredients("[\"Pasta\", \"Eggs\", \"Parmesan\", \"Bacon\"]")
                .instructions("1. Cook pasta\n2. Mix with eggs and cheese")
                .servingSuggestions("Serve with garlic bread")
                .imageUrl("http://example.com/image.jpg")
                .totalTimeMinutes(30)
                .difficulty(DifficultyLevel.MEDIUM)
                .userId(TEST_USER_ID)
                .isAiGenerated(false)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .macros(Macros.builder()
                        .calories(BigDecimal.valueOf(450))
                        .proteinGrams(BigDecimal.valueOf(30))
                        .carbsGrams(BigDecimal.valueOf(50))
                        .fatGrams(BigDecimal.valueOf(15))
                        .build())
                .build();

        Recipe recipe2 = Recipe.builder()
                .title("Chicken Curry")
                .ingredients("[\"Chicken\", \"Curry Powder\", \"Coconut Milk\"]")
                .instructions("1. Cook chicken\n2. Add curry sauce")
                .servingSuggestions("Serve with rice and naan bread")
                .imageUrl("http://example.com/image.jpg")
                .totalTimeMinutes(30)
                .difficulty(DifficultyLevel.MEDIUM)
                .userId(TEST_USER_ID)
                .isAiGenerated(false)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .macros(Macros.builder()
                        .calories(BigDecimal.valueOf(450))
                        .proteinGrams(BigDecimal.valueOf(30))
                        .carbsGrams(BigDecimal.valueOf(50))
                        .fatGrams(BigDecimal.valueOf(15))
                        .build())
                .build();

        Recipe recipe3 = Recipe.builder()
                .title("Beef Stir Fry")
                .ingredients("[\"Beef\", \"Bell Peppers\", \"Soy Sauce\"]")
                .instructions("1. Stir fry beef\n2. Add vegetables")
                .servingSuggestions("Serve with steamed rice")
                .imageUrl("http://example.com/image.jpg")
                .totalTimeMinutes(30)
                .difficulty(DifficultyLevel.EASY)
                .userId(ANOTHER_USER_ID)
                .isAiGenerated(false)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .macros(Macros.builder()
                        .calories(BigDecimal.valueOf(450))
                        .proteinGrams(BigDecimal.valueOf(30))
                        .carbsGrams(BigDecimal.valueOf(50))
                        .fatGrams(BigDecimal.valueOf(15))
                        .build())
                .build();

        // Save recipes and track IDs for cleanup
        createdRecipeIds.add(recipeRepository.save(recipe1).getId());
        createdRecipeIds.add(recipeRepository.save(recipe2).getId());
        createdRecipeIds.add(recipeRepository.save(recipe3).getId());
    }

    @AfterEach
    void tearDown() {
        createdRecipeIds.forEach(id -> recipeRepository.deleteById(id));
        createdRecipeIds.clear();
    }

    @Test
    @WithMockUser
    public void testSearchRecipes_ByTitle() throws Exception {
        mockMvc.perform(get("/api/v1/recipes/search")
                        .param("keyword", "Pasta"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements", is(1)))
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].title", is("Pasta Carbonara")));
    }

    @Test
    @WithMockUser
    public void testSearchRecipes_NoResults() throws Exception {
        mockMvc.perform(get("/api/v1/recipes/search")
                        .param("keyword", "Salad"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements", is(0)))
                .andExpect(jsonPath("$.content", hasSize(0)));
    }

    @Test
    @WithMockUser
    public void testSearchRecipes_Pagination() throws Exception {
        mockMvc.perform(get("/api/v1/recipes/search")
                        .param("keyword", "")
                        .param("page", "0")
                        .param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements", is(3)))
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.number", is(0)))
                .andExpect(jsonPath("$.size", is(2)));
    }

    @Test
    @WithMockUser
    public void testSearchRecipes_ByIngredient() throws Exception {
        mockMvc.perform(get("/api/v1/recipes/search")
                        .param("keyword", "Chicken"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements", is(1)))
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].title", is("Chicken Curry")));
    }
} 