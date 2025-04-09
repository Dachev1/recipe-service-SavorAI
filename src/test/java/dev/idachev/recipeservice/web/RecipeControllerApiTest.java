package dev.idachev.recipeservice.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.idachev.recipeservice.config.JwtAuthenticationFilter;
import dev.idachev.recipeservice.service.RecipeService;
import dev.idachev.recipeservice.user.service.UserService;
import dev.idachev.recipeservice.util.JwtUtil;
import dev.idachev.recipeservice.web.dto.RecipeRequest;
import dev.idachev.recipeservice.web.dto.RecipeResponse;
import dev.idachev.recipeservice.web.dto.SimplifiedRecipeResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.util.AntPathMatcher;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(RecipeController.class)
@AutoConfigureMockMvc(addFilters = false)
public class RecipeControllerApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private RecipeService recipeService;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private JwtUtil jwtUtil;

    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockitoBean
    private AntPathMatcher pathMatcher;

    @MockitoBean
    private ConcurrentHashMap<String, Long> tokenBlacklist;

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String VALID_TOKEN = "Bearer valid-token";
    private static final UUID TEST_USER_ID = UUID.randomUUID();
    private static final UUID TEST_RECIPE_ID = UUID.randomUUID();

    @Test
    @DisplayName("Given valid recipe request and token, when creating recipe with image, then return created status with recipe")
    public void givenValidRequestAndToken_whenCreateRecipeWithImage_thenReturnCreatedWithRecipe() throws Exception {

        // Given
        RecipeRequest request = createTestRecipeRequest();
        RecipeResponse expectedResponse = createTestRecipeResponse();
        MockMultipartFile recipeFile = new MockMultipartFile(
                "recipe", "", "application/json", objectMapper.writeValueAsBytes(request));
        MockMultipartFile imageFile = new MockMultipartFile(
                "image", "test.jpg", "image/jpeg", "test image content".getBytes());

        when(userService.getUserIdFromToken(VALID_TOKEN)).thenReturn(TEST_USER_ID);
        when(recipeService.createRecipe(any(RecipeRequest.class), any(MockMultipartFile.class), eq(TEST_USER_ID)))
                .thenReturn(expectedResponse);

        // When & Then
        mockMvc.perform(multipart("/api/v1/recipes")
                        .file(recipeFile)
                        .file(imageFile)
                        .header(AUTHORIZATION_HEADER, VALID_TOKEN))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(expectedResponse.getId().toString()))
                .andExpect(jsonPath("$.title").value(expectedResponse.getTitle()))
                .andExpect(jsonPath("$.servingSuggestions").value(expectedResponse.getServingSuggestions()));

        verify(userService).getUserIdFromToken(VALID_TOKEN);
        verify(recipeService).createRecipe(any(RecipeRequest.class), any(MockMultipartFile.class), eq(TEST_USER_ID));
    }

    @Test
    @DisplayName("Given valid recipe request and token, when creating recipe with JSON, then return created status with recipe")
    public void givenValidRequestAndToken_whenCreateRecipeWithJson_thenReturnCreatedWithRecipe() throws Exception {

        // Given
        RecipeRequest request = createTestRecipeRequest();
        RecipeResponse expectedResponse = createTestRecipeResponse();

        when(userService.getUserIdFromToken(VALID_TOKEN)).thenReturn(TEST_USER_ID);
        when(recipeService.createRecipe(any(RecipeRequest.class), eq(TEST_USER_ID)))
                .thenReturn(expectedResponse);

        // When & Then
        mockMvc.perform(post("/api/v1/recipes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .header(AUTHORIZATION_HEADER, VALID_TOKEN))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(expectedResponse.getId().toString()))
                .andExpect(jsonPath("$.title").value(expectedResponse.getTitle()))
                .andExpect(jsonPath("$.servingSuggestions").value(expectedResponse.getServingSuggestions()));

        verify(userService).getUserIdFromToken(VALID_TOKEN);
        verify(recipeService).createRecipe(any(RecipeRequest.class), eq(TEST_USER_ID));
    }

    @Test
    @DisplayName("Given valid ID and token, when getting recipe by ID, then return recipe")
    public void givenValidIdAndToken_whenGetRecipeById_thenReturnRecipe() throws Exception {

        // Given
        RecipeResponse expectedResponse = createTestRecipeResponse();

        when(userService.getUserIdFromToken(VALID_TOKEN)).thenReturn(TEST_USER_ID);
        when(recipeService.getRecipeById(TEST_RECIPE_ID, TEST_USER_ID)).thenReturn(expectedResponse);

        // When & Then
        mockMvc.perform(get("/api/v1/recipes/{id}", TEST_RECIPE_ID)
                        .header(AUTHORIZATION_HEADER, VALID_TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(expectedResponse.getId().toString()))
                .andExpect(jsonPath("$.title").value(expectedResponse.getTitle()))
                .andExpect(jsonPath("$.servingSuggestions").value(expectedResponse.getServingSuggestions()));

        verify(userService).getUserIdFromToken(VALID_TOKEN);
        verify(recipeService).getRecipeById(TEST_RECIPE_ID, TEST_USER_ID);
    }

    @Test
    @DisplayName("Given valid token, when getting all recipes, then return paginated recipes")
    public void givenValidToken_whenGetAllRecipes_thenReturnPaginatedRecipes() throws Exception {

        // Given
        List<RecipeResponse> recipes = Arrays.asList(
                createTestRecipeResponse(),
                createTestRecipeResponse()
        );
        Page<RecipeResponse> pageResponse = new PageImpl<>(recipes);

        when(userService.getUserIdFromToken(VALID_TOKEN)).thenReturn(TEST_USER_ID);
        when(recipeService.getAllRecipes(any(Pageable.class), eq(TEST_USER_ID), eq(true))).thenReturn(pageResponse);

        // When & Then
        mockMvc.perform(get("/api/v1/recipes")
                        .header(AUTHORIZATION_HEADER, VALID_TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.totalElements").value(2));

        verify(userService).getUserIdFromToken(VALID_TOKEN);
        verify(recipeService).getAllRecipes(any(Pageable.class), eq(TEST_USER_ID), eq(true));
    }

    @Test
    @DisplayName("Given valid token, when getting user's recipes, then return user recipes")
    public void givenValidToken_whenGetMyRecipes_thenReturnUserRecipes() throws Exception {

        // Given
        List<RecipeResponse> userRecipes = Arrays.asList(
                createTestRecipeResponse(),
                createTestRecipeResponse()
        );

        when(userService.getUserIdFromToken(VALID_TOKEN)).thenReturn(TEST_USER_ID);
        when(recipeService.getRecipesByUserId(TEST_USER_ID)).thenReturn(userRecipes);

        // When & Then
        mockMvc.perform(get("/api/v1/recipes/my-recipes")
                        .header(AUTHORIZATION_HEADER, VALID_TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].id").value(userRecipes.get(0).getId().toString()))
                .andExpect(jsonPath("$[1].id").value(userRecipes.get(1).getId().toString()));

        verify(userService).getUserIdFromToken(VALID_TOKEN);
        verify(recipeService).getRecipesByUserId(TEST_USER_ID);
    }

    @Test
    @DisplayName("Given valid recipe data, ID and token, when updating recipe, then return updated recipe")
    public void givenValidDataIdAndToken_whenUpdateRecipe_thenReturnUpdatedRecipe() throws Exception {

        // Given
        RecipeRequest request = createTestRecipeRequest();
        request.setTitle("Updated Recipe Title");

        RecipeResponse updatedResponse = createTestRecipeResponse();
        updatedResponse.setTitle("Updated Recipe Title");

        when(userService.getUserIdFromToken(VALID_TOKEN)).thenReturn(TEST_USER_ID);
        when(recipeService.updateRecipe(eq(TEST_RECIPE_ID), any(RecipeRequest.class), eq(TEST_USER_ID)))
                .thenReturn(updatedResponse);

        // When & Then
        mockMvc.perform(put("/api/v1/recipes/{id}", TEST_RECIPE_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .header(AUTHORIZATION_HEADER, VALID_TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(updatedResponse.getId().toString()))
                .andExpect(jsonPath("$.title").value("Updated Recipe Title"))
                .andExpect(jsonPath("$.servingSuggestions").value(updatedResponse.getServingSuggestions()));

        verify(userService).getUserIdFromToken(VALID_TOKEN);
        verify(recipeService).updateRecipe(eq(TEST_RECIPE_ID), any(RecipeRequest.class), eq(TEST_USER_ID));
    }

    @Test
    @DisplayName("Given valid ID and token, when deleting recipe, then return no content status")
    public void givenValidIdAndToken_whenDeleteRecipe_thenReturnNoContent() throws Exception {

       // Given
        when(userService.getUserIdFromToken(VALID_TOKEN)).thenReturn(TEST_USER_ID);
        doNothing().when(recipeService).deleteRecipe(TEST_RECIPE_ID, TEST_USER_ID);

        // When & Then
        mockMvc.perform(delete("/api/v1/recipes/{id}", TEST_RECIPE_ID)
                        .header(AUTHORIZATION_HEADER, VALID_TOKEN))
                .andExpect(status().isNoContent());

        verify(userService).getUserIdFromToken(VALID_TOKEN);
        verify(recipeService).deleteRecipe(TEST_RECIPE_ID, TEST_USER_ID);
    }

    @Test
    @DisplayName("Given valid keyword, token and pagination, when searching recipes, then return search results")
    public void givenValidKeywordTokenAndPagination_whenSearchRecipes_thenReturnSearchResults() throws Exception {

        // Given
        String keyword = "pasta";
        List<RecipeResponse> searchResults = Arrays.asList(
                createTestRecipeResponse(),
                createTestRecipeResponse()
        );
        Page<RecipeResponse> pageResponse = new PageImpl<>(searchResults);

        when(userService.getUserIdFromToken(VALID_TOKEN)).thenReturn(TEST_USER_ID);
        when(recipeService.searchRecipes(eq(keyword), any(Pageable.class), eq(TEST_USER_ID)))
                .thenReturn(pageResponse);

        // When & Then
        mockMvc.perform(get("/api/v1/recipes/search")
                        .param("keyword", keyword)
                        .header(AUTHORIZATION_HEADER, VALID_TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.totalElements").value(2));

        verify(userService).getUserIdFromToken(VALID_TOKEN);
        verify(recipeService).searchRecipes(eq(keyword), any(Pageable.class), eq(TEST_USER_ID));
    }

    @Test
    @DisplayName("Given valid ingredients and token, when generating meal, then return generated recipe")
    public void givenValidIngredientsAndToken_whenGenerateMeal_thenReturnGeneratedRecipe() throws Exception {

        // Given
        List<String> ingredients = Arrays.asList("tomato", "cheese", "pasta");
        SimplifiedRecipeResponse generatedRecipe = createTestSimplifiedRecipeResponse();

        doNothing().when(userService).validateToken(VALID_TOKEN);
        when(recipeService.generateMeal(ingredients)).thenReturn(generatedRecipe);

        // When & Then
        mockMvc.perform(post("/api/v1/recipes/generate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(ingredients))
                        .header(AUTHORIZATION_HEADER, VALID_TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value(generatedRecipe.getTitle()))
                .andExpect(jsonPath("$.ingredients", hasSize(3)))
                .andExpect(jsonPath("$.instructions").value(generatedRecipe.getInstructions()));

        verify(userService).validateToken(VALID_TOKEN);
        verify(recipeService).generateMeal(ingredients);
    }

    // Helper methods
    private RecipeRequest createTestRecipeRequest() {
        RecipeRequest request = new RecipeRequest();
        request.setTitle("Test Recipe");
        request.setServingSuggestions("A delicious test recipe");
        request.setIngredients(Arrays.asList("ingredient1", "ingredient2"));
        request.setInstructions("Test instructions");
        return request;
    }

    private RecipeResponse createTestRecipeResponse() {
        RecipeResponse response = new RecipeResponse();
        response.setId(TEST_RECIPE_ID);
        response.setTitle("Test Recipe");
        response.setServingSuggestions("A delicious test recipe");
        response.setIngredients(Arrays.asList("ingredient1", "ingredient2"));
        response.setInstructions("Test instructions");
        response.setImageUrl("http://test-image-url.com");
        response.setFavoriteCount(10L);
        response.setIsFavorite(false);
        return response;
    }

    private SimplifiedRecipeResponse createTestSimplifiedRecipeResponse() {
        SimplifiedRecipeResponse response = new SimplifiedRecipeResponse();
        response.setTitle("Generated Recipe");
        response.setIngredients(Arrays.asList("tomato", "cheese", "pasta"));
        response.setInstructions("Test generated instructions");
        return response;
    }
}