package dev.idachev.recipeservice.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.idachev.recipeservice.config.JwtAuthenticationFilter;
import dev.idachev.recipeservice.exception.BadRequestException;
import dev.idachev.recipeservice.exception.ResourceNotFoundException;
import dev.idachev.recipeservice.exception.UnauthorizedException;
import dev.idachev.recipeservice.service.RecipeService;
import dev.idachev.recipeservice.user.service.UserService;
import dev.idachev.recipeservice.util.JwtUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.util.AntPathMatcher;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {RecipeController.class, GlobalExceptionHandler.class})
@AutoConfigureMockMvc(addFilters = false)
public class GlobalExceptionHandlerApiTest {

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
    @DisplayName("Given non-existent recipe ID, when getting recipe, then return 404 not found error")
    public void givenNonExistentId_whenGetRecipe_thenReturn404NotFound() throws Exception {

        // Given
        String errorMessage = "Recipe not found with id: " + TEST_RECIPE_ID;
        when(userService.getUserIdFromToken(VALID_TOKEN)).thenReturn(TEST_USER_ID);
        when(recipeService.getRecipeById(TEST_RECIPE_ID, TEST_USER_ID))
                .thenThrow(new ResourceNotFoundException(errorMessage));

        // When & Then
        mockMvc.perform(get("/api/v1/recipes/{id}", TEST_RECIPE_ID)
                        .header(AUTHORIZATION_HEADER, VALID_TOKEN))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status", is(HttpStatus.NOT_FOUND.value())))
                .andExpect(jsonPath("$.message", is(errorMessage)))
                .andExpect(jsonPath("$.timestamp", notNullValue()));
    }

    @Test
    @DisplayName("Given invalid request data, when creating recipe, then return 400 bad request with validation errors")
    public void givenInvalidRequestData_whenCreateRecipe_thenReturn400BadRequestWithValidationErrors() throws Exception {

        // Given
        String requestJson = "{}"; // Empty object will cause validation failures

        when(userService.getUserIdFromToken(VALID_TOKEN)).thenReturn(TEST_USER_ID);

        // When & Then
        mockMvc.perform(post("/api/v1/recipes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson)
                        .header(AUTHORIZATION_HEADER, VALID_TOKEN))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(HttpStatus.BAD_REQUEST.value())))
                .andExpect(jsonPath("$.message", is("Validation failed")))
                .andExpect(jsonPath("$.timestamp", notNullValue()))
                .andExpect(jsonPath("$.details", notNullValue()));
    }

    @Test
    @DisplayName("Given bad request exception, when processing request, then return 400 bad request")
    public void givenBadRequestException_whenProcessingRequest_thenReturn400BadRequest() throws Exception {

        // Given
        String errorMessage = "Invalid request data";
        when(userService.getUserIdFromToken(VALID_TOKEN)).thenReturn(TEST_USER_ID);
        // Use a non-empty array with at least one ingredient to avoid triggering validation
        when(recipeService.generateMeal(any()))
                .thenThrow(new BadRequestException(errorMessage));

        // When & Then
        mockMvc.perform(post("/api/v1/recipes/generate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("[\"ingredient1\"]") // Add a valid ingredient to the list
                        .header(AUTHORIZATION_HEADER, VALID_TOKEN))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(HttpStatus.BAD_REQUEST.value())))
                .andExpect(jsonPath("$.message", is(errorMessage)))
                .andExpect(jsonPath("$.timestamp", notNullValue()));
    }

    @Test
    @DisplayName("Given unauthorized exception, when accessing protected resource, then return 401 unauthorized")
    public void givenUnauthorizedException_whenAccessingProtectedResource_thenReturn401Unauthorized() throws Exception {

        // Given
        when(userService.getUserIdFromToken(VALID_TOKEN))
                .thenThrow(new UnauthorizedException("Invalid token"));

        // When & Then
        mockMvc.perform(get("/api/v1/recipes")
                        .header(AUTHORIZATION_HEADER, VALID_TOKEN))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status", is(HttpStatus.UNAUTHORIZED.value())))
                .andExpect(jsonPath("$.message", is("Your session has expired or is invalid. Please log in again.")))
                .andExpect(jsonPath("$.timestamp", notNullValue()));
    }

    @Test
    @DisplayName("Given malformed JSON, when creating recipe, then return 400 bad request for HttpMessageNotReadableException")
    public void givenMalformedJson_whenCreateRecipe_thenReturn400BadRequest() throws Exception {

        // Given
        String malformedJson = "{malformed json}"; // Malformed JSON that will cause HttpMessageNotReadableException

        // When & Then
        mockMvc.perform(post("/api/v1/recipes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(malformedJson)
                        .header(AUTHORIZATION_HEADER, VALID_TOKEN))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(HttpStatus.BAD_REQUEST.value())))
                .andExpect(jsonPath("$.message", is("Invalid request format: The request body could not be read")))
                .andExpect(jsonPath("$.timestamp", notNullValue()));
    }

    @Test
    @DisplayName("Given invalid UUID format, when getting recipe by ID, then return 400 bad request for MethodArgumentTypeMismatchException")
    public void givenInvalidUuid_whenGetRecipeById_thenReturn400BadRequest() throws Exception {

        // Given
        String invalidUuid = "not-a-valid-uuid";

        // When & Then
        mockMvc.perform(get("/api/v1/recipes/{id}", invalidUuid)
                        .header(AUTHORIZATION_HEADER, VALID_TOKEN))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(HttpStatus.BAD_REQUEST.value())))
                .andExpect(jsonPath("$.message", is("Invalid parameter type: id")))
                .andExpect(jsonPath("$.timestamp", notNullValue()));
    }

    @Test
    @DisplayName("Given runtime exception, when processing request, then return 500 internal server error")
    public void givenRuntimeException_whenProcessingRequest_thenReturn500InternalServerError() throws Exception {

        // Given
        when(userService.getUserIdFromToken(VALID_TOKEN)).thenReturn(TEST_USER_ID);
        when(recipeService.getAllRecipes(any(), eq(TEST_USER_ID)))
                .thenThrow(new RuntimeException("Unexpected server error"));

        // When & Then
        mockMvc.perform(get("/api/v1/recipes")
                        .header(AUTHORIZATION_HEADER, VALID_TOKEN))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status", is(HttpStatus.INTERNAL_SERVER_ERROR.value())))
                .andExpect(jsonPath("$.message", is("An unexpected error occurred")))
                .andExpect(jsonPath("$.timestamp", notNullValue()));
    }

    @Test
    @DisplayName("Given missing required parameter, when searching recipes, then return 400 bad request")
    public void givenMissingRequiredParameter_whenSearchRecipes_thenReturn400BadRequest() throws Exception {

        // When & Then - Missing required 'keyword' parameter
        mockMvc.perform(get("/api/v1/recipes/search")
                        .header(AUTHORIZATION_HEADER, VALID_TOKEN))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(HttpStatus.BAD_REQUEST.value())))
                .andExpect(jsonPath("$.message", is("Missing required parameter: keyword")))
                .andExpect(jsonPath("$.timestamp", notNullValue()));
    }
}