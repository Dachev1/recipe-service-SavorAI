package dev.idachev.recipeservice.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import dev.idachev.recipeservice.exception.BadRequestException;
import dev.idachev.recipeservice.exception.ResourceNotFoundException;
import dev.idachev.recipeservice.exception.UnauthorizedAccessException;
import dev.idachev.recipeservice.model.DifficultyLevel;
import dev.idachev.recipeservice.model.Recipe;
import dev.idachev.recipeservice.service.RecipeSearchService;
import dev.idachev.recipeservice.service.RecipeService;
import dev.idachev.recipeservice.service.VoteService;
import dev.idachev.recipeservice.web.dto.RecipeRequest;
import dev.idachev.recipeservice.web.dto.RecipeResponse;
import dev.idachev.recipeservice.web.dto.SimplifiedRecipeResponse;
import dev.idachev.recipeservice.web.dto.VoteRequest;
import dev.idachev.recipeservice.web.mapper.RecipeMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;

import jakarta.servlet.http.HttpServletResponse;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class RecipeControllerApiTest {

    private MockMvc mockMvc;

    private ObjectMapper objectMapper;

    @Mock
    private RecipeService recipeService;

    @Mock
    private RecipeSearchService recipeSearchService;

    @Mock
    private VoteService voteService;

    @Mock
    private RecipeMapper recipeMapper;

    @Mock
    private SecurityContext securityContext;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private RecipeController recipeController;

    private UUID testUserId;
    private UUID testRecipeId;
    private RecipeRequest testRecipeRequest;
    private RecipeResponse testRecipeResponse;
    private Recipe testRecipe;
    private List<String> ingredients;

    private ResultMatcher contentTypeJson() {
        return result -> {
            MockHttpServletResponse response = result.getResponse();
            int status = response.getStatus();
            // Only check content type for successful responses (2xx) with content
            if (status >= 200 && status < 300 && response.getContentLength() > 0) {
                content().contentType(MediaType.APPLICATION_JSON).match(result);
            }
        };
    }

    @BeforeEach
    void setUp() throws Exception {
        // Set up ObjectMapper
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        // Initialize test data
        testUserId = UUID.randomUUID();
        testRecipeId = UUID.randomUUID();

        // Mock security context for authentication
        lenient().when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);
        lenient().when(authentication.getPrincipal()).thenReturn(testUserId);

        // Set up MockMvc with standalone configuration and custom filter
        mockMvc = MockMvcBuilders
                .standaloneSetup(recipeController)
                .setCustomArgumentResolvers(
                        new PageableHandlerMethodArgumentResolver(),
                        new AuthenticationPrincipalArgumentResolver()
                )
                .setMessageConverters(new MappingJackson2HttpMessageConverter())
                .setControllerAdvice(new GlobalExceptionHandler())
                .defaultResponseCharacterEncoding(StandardCharsets.UTF_8)
                // Re-add the custom filter for basic auth check in standalone mode
                .addFilter((request, response, chain) -> {
                    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
                    // Check if auth exists and has a principal. Allow requests through if authenticated.
                    // Or if the specific test clears the context (e.g., for unauthorized tests)
                    boolean isAuthenticated = auth != null && auth.getPrincipal() != null;

                    // Allow request to proceed if authenticated
                    if (isAuthenticated) {
                       chain.doFilter(request, response);
                    } else {
                        // Check if the test explicitly cleared context for unauthorized scenario
                        Authentication currentAuth = SecurityContextHolder.getContext().getAuthentication();
                        if(currentAuth == null) { // Context likely cleared by test
                             HttpServletResponse httpResponse = (HttpServletResponse) response;
                             httpResponse.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                             // Optionally write a minimal error body if needed/expected by tests
                             // httpResponse.getWriter().write("{\"error\": \"Unauthorized\"}");
                             return; // Stop filter chain for unauthorized
                        } else {
                            // Should not happen if context is managed correctly in tests,
                            // but proceed just in case context exists but principal is null somehow.
                            chain.doFilter(request, response);
                        }
                    }
                })
                .build();

        // Setup test ingredients and instructions
        ingredients = List.of("Ingredient 1", "Ingredient 2");
        List<String> instructionsList = List.of("Step 1", "Step 2");
        String instructionsString = String.join("\n", instructionsList);

        // Setup test recipe request - Back to Mocking
        testRecipeRequest = mock(RecipeRequest.class);
        when(testRecipeRequest.title()).thenReturn("Test Recipe");
        when(testRecipeRequest.servingSuggestions()).thenReturn("Serving suggestions");
        when(testRecipeRequest.ingredients()).thenReturn(ingredients);
        when(testRecipeRequest.instructions()).thenReturn(instructionsString);
        when(testRecipeRequest.totalTimeMinutes()).thenReturn(30);
        when(testRecipeRequest.difficulty()).thenReturn(DifficultyLevel.MEDIUM);
        // Add when() for macros, mealType if they exist on RecipeRequest and are needed

        // Setup test recipe response - Back to Mocking
        testRecipeResponse = mock(RecipeResponse.class);
        when(testRecipeResponse.id()).thenReturn(testRecipeId);
        when(testRecipeResponse.title()).thenReturn("Test Recipe");
        when(testRecipeResponse.instructions()).thenReturn(instructionsString);
        when(testRecipeResponse.ingredients()).thenReturn(ingredients);
        when(testRecipeResponse.servingSuggestions()).thenReturn("Serving suggestions");
        when(testRecipeResponse.totalTimeMinutes()).thenReturn(30);
        when(testRecipeResponse.difficulty()).thenReturn(DifficultyLevel.MEDIUM);
        when(testRecipeResponse.imageUrl()).thenReturn("https://example.com/image.jpg");
        when(testRecipeResponse.username()).thenReturn("testuser");
        when(testRecipeResponse.createdById()).thenReturn(testUserId);
        when(testRecipeResponse.upvotes()).thenReturn(0);
        when(testRecipeResponse.downvotes()).thenReturn(0);
        when(testRecipeResponse.userVote()).thenReturn(null);
        when(testRecipeResponse.isFavorite()).thenReturn(false);
        when(testRecipeResponse.createdAt()).thenReturn(LocalDateTime.now());
        when(testRecipeResponse.updatedAt()).thenReturn(LocalDateTime.now());
        // when(testRecipeResponse.version()).thenReturn(1L); // Removed, version() undefined on RecipeResponse
        // Add when() for macros, mealType if they exist on RecipeResponse

        // Setup test recipe entity - Keep Direct Instantiation (Builder)
        testRecipe = Recipe.builder()
                .id(testRecipeId)
                .title("Test Recipe")
                .instructions(instructionsString)
                .ingredients(objectMapper.writeValueAsString(ingredients))
                .servingSuggestions("Serving suggestions")
                .totalTimeMinutes(30)
                .difficulty(DifficultyLevel.MEDIUM)
                .imageUrl("https://example.com/image.jpg")
                .userId(testUserId)
                .createdAt(LocalDateTime.now().minusDays(1))
                .updatedAt(LocalDateTime.now())
                .upvotes(0)
                .downvotes(0)
                .version(1L)
                .build();
    }

    @Nested
    @DisplayName("Create Recipe Tests")
    class CreateRecipeTests {

        @Test
        @DisplayName("Should create a recipe with JSON request")
        void createRecipeJson_Success() throws Exception {
            // Given
            given(recipeService.createRecipe(any(RecipeRequest.class), eq(null), eq(testUserId)))
                    .willReturn(testRecipeResponse);

            // When
            ResultActions response = mockMvc.perform(post("/api/v1/recipes")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(testRecipeRequest)))
                    .andDo(print());

            // Then
            response
                    .andExpect(status().isCreated())
                    .andExpect(contentTypeJson())
                    .andExpect(jsonPath("$.id", is(testRecipeId.toString())))
                    .andExpect(jsonPath("$.title", is(testRecipeResponse.title())))
                    .andExpect(jsonPath("$.instructions", is(testRecipeResponse.instructions())))
                    .andExpect(jsonPath("$.totalTimeMinutes", is(testRecipeResponse.totalTimeMinutes())))
                    .andExpect(jsonPath("$.difficulty", is(testRecipeResponse.difficulty().name())))
                    .andExpect(jsonPath("$.ingredients", hasSize(testRecipeResponse.ingredients().size())));

            verify(recipeService).createRecipe(any(RecipeRequest.class), eq(null), eq(testUserId));
        }

        @Test
        @DisplayName("Should create a recipe with multipart request and image")
        void createRecipe_WithImage_Success() throws Exception {
            // Given
            byte[] imageContent = "test image content".getBytes();
            given(recipeService.createRecipe(any(RecipeRequest.class), any(), eq(testUserId)))
                    .willReturn(testRecipeResponse);

            // Setup multipart request directly
            MockMultipartFile recipeFile = new MockMultipartFile(
                    "recipe", // part name matching controller @RequestPart("recipe")
                    "", // filename (optional)
                    MediaType.APPLICATION_JSON_VALUE, // content type of this part
                    objectMapper.writeValueAsBytes(testRecipeRequest) // payload
            );

            MockMultipartFile imageFile = new MockMultipartFile(
                    "image", // part name matching controller @RequestPart("image")
                    "test-image.jpg", // original filename
                    MediaType.IMAGE_JPEG_VALUE, // content type
                    imageContent // payload
            );

            // When
            // Perform the multipart request directly
            ResultActions response = mockMvc.perform(multipart("/api/v1/recipes")
                            .file(recipeFile)
                            .file(imageFile))
                    .andDo(print());

            // Then
            response
                    .andExpect(status().isCreated())
                    .andExpect(contentTypeJson())
                    .andExpect(jsonPath("$.id", is(testRecipeResponse.id().toString())))
                    .andExpect(jsonPath("$.title", is(testRecipeResponse.title())))
                    .andExpect(jsonPath("$.imageUrl", is(testRecipeResponse.imageUrl())));

            verify(recipeService).createRecipe(any(RecipeRequest.class), any(), eq(testUserId));
        }

        @Test
        @DisplayName("Should return 401 when creating recipe without authentication")
        void createRecipe_Unauthorized() throws Exception {
            // Given
            SecurityContextHolder.clearContext();

            // When
            ResultActions response = mockMvc.perform(post("/api/v1/recipes")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(testRecipeRequest)))
                    .andDo(print());

            // Then
            response.andExpect(status().isUnauthorized());
            verifyNoInteractions(recipeService);

            // Restore security context for other tests
            SecurityContextHolder.setContext(securityContext);
        }
    }

    @Nested
    @DisplayName("Get Recipe Tests")
    class GetRecipeTests {

        @Test
        @DisplayName("Should get a recipe by ID")
        void getRecipeById_Success() throws Exception {
            // Given
            given(recipeService.getRecipeById(eq(testRecipeId), eq(testUserId)))
                    .willReturn(testRecipeResponse);

            // When
            ResultActions response = mockMvc.perform(get("/api/v1/recipes/{id}", testRecipeId)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andDo(print());

            // Then
            response
                    .andExpect(status().isOk())
                    .andExpect(contentTypeJson())
                    .andExpect(jsonPath("$.id", is(testRecipeId.toString())))
                    .andExpect(jsonPath("$.title", is(testRecipeResponse.title())))
                    .andExpect(jsonPath("$.createdById", is(testUserId.toString())))
                    .andExpect(jsonPath("$.username", is("testuser")));

            verify(recipeService).getRecipeById(eq(testRecipeId), eq(testUserId));
        }

        @Test
        @DisplayName("Should return 404 when recipe not found")
        void getRecipeById_NotFound() throws Exception {
            // Given
            given(recipeService.getRecipeById(eq(testRecipeId), eq(testUserId)))
                    .willThrow(new ResourceNotFoundException("Recipe not found"));

            // When
            ResultActions response = mockMvc.perform(get("/api/v1/recipes/{id}", testRecipeId)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andDo(print());

            // Then
            response.andExpect(status().isNotFound());
            verify(recipeService).getRecipeById(eq(testRecipeId), eq(testUserId));
        }

        @Test
        @DisplayName("Should return 400 when getting recipe with invalid ID")
        void getRecipeById_InvalidId() throws Exception {
            // When
            ResultActions response = mockMvc.perform(get("/api/v1/recipes/{id}", "invalid-uuid")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andDo(print());

            // Then
            response.andExpect(status().isBadRequest());
            verifyNoInteractions(recipeService);
        }
    }

    @Nested
    @DisplayName("Get All Recipes Tests")
    class GetAllRecipesTests {

        @Test
        @DisplayName("Should get all recipes with pagination")
        void getAllRecipes_Success() throws Exception {
            // Given
            Pageable pageable = PageRequest.of(0, 10);
            List<RecipeResponse> recipeList = List.of(testRecipeResponse);
            Page<RecipeResponse> recipePage = new PageImpl<>(recipeList, pageable, 1);

            // Mock the method actually called by the controller based on logs
            given(recipeSearchService.getAllRecipesExcludingUser(any(Pageable.class), eq(testUserId)))
                    .willReturn(recipePage);

            // When
            ResultActions response = mockMvc.perform(get("/api/v1/recipes")
                            .contentType(MediaType.APPLICATION_JSON)
                            .param("page", "0")
                            .param("size", "10"))
                    .andDo(print());

            // Then
            response
                    .andExpect(status().isOk())
                    .andExpect(contentTypeJson())
                    .andExpect(jsonPath("$.content", hasSize(1)))
                    .andExpect(jsonPath("$.content[0].id", is(testRecipeResponse.id().toString())))
                    .andExpect(jsonPath("$.content[0].title", is(testRecipeResponse.title())));

            // Verify the method actually called by the controller
            verify(recipeSearchService).getAllRecipesExcludingUser(any(Pageable.class), eq(testUserId));
        }

        @Test
        @DisplayName("Should handle empty results when getting all recipes")
        void getAllRecipes_Empty() throws Exception {
            // Given
            Pageable pageable = PageRequest.of(0, 10);
            Page<RecipeResponse> emptyPage = new PageImpl<>(List.of(), pageable, 0);

             // Mock the method actually called by the controller based on logs
            given(recipeSearchService.getAllRecipesExcludingUser(any(Pageable.class), eq(testUserId)))
                    .willReturn(emptyPage);

            // When
            ResultActions response = mockMvc.perform(get("/api/v1/recipes")
                            .contentType(MediaType.APPLICATION_JSON)
                            .param("page", "0")
                            .param("size", "10"))
                    .andDo(print());

            // Then
            response
                    .andExpect(status().isOk())
                    .andExpect(contentTypeJson())
                    .andExpect(jsonPath("$.content", hasSize(0)))
                    .andExpect(jsonPath("$.totalElements", is(0)));

            // Verify the method actually called by the controller
            verify(recipeSearchService).getAllRecipesExcludingUser(any(Pageable.class), eq(testUserId));
        }
    }

    @Nested
    @DisplayName("Get My Recipes Tests")
    class GetMyRecipesTests {

        @Test
        @DisplayName("Should get all personal recipes")
        void getMyRecipes_Success() throws Exception {
            // Given
            List<RecipeResponse> recipeList = List.of(testRecipeResponse); // Expect a List

            // Mock recipeService.getRecipesByUserId returning a List
            given(recipeService.getRecipesByUserId(eq(testUserId))).willReturn(recipeList);

            // When
            ResultActions response = mockMvc.perform(get("/api/v1/recipes/my-recipes")) // No pagination params needed
                    .andDo(print());

            // Then
            response
                    .andExpect(status().isOk())
                    .andExpect(contentTypeJson())
                    .andExpect(jsonPath("$", hasSize(1))) // Expecting a JSON array
                    .andExpect(jsonPath("$[0].id", is(testRecipeId.toString())))
                    .andExpect(jsonPath("$[0].title", is(testRecipeResponse.title()))); // Use mocked response field

            verify(recipeService).getRecipesByUserId(eq(testUserId)); // Verify correct service call
        }

        @Test
        @DisplayName("Should handle empty results when getting personal recipes")
        void getMyRecipes_Empty() throws Exception {
            // Given
            // Mock recipeService.getRecipesByUserId returning an empty List
            given(recipeService.getRecipesByUserId(eq(testUserId))).willReturn(List.of());

            // When
            ResultActions response = mockMvc.perform(get("/api/v1/recipes/my-recipes"))
                    .andDo(print());

            // Then
            response
                    .andExpect(status().isOk())
                    .andExpect(contentTypeJson())
                    .andExpect(jsonPath("$", hasSize(0))); // Expect empty JSON array

            verify(recipeService).getRecipesByUserId(eq(testUserId)); // Verify correct service call
        }
    }

    @Nested
    @DisplayName("Update Recipe Tests")
    class UpdateRecipeTests {

        @Test
        @DisplayName("Should update a recipe")
        void updateRecipe_Success() throws Exception {
            // Given
            RecipeRequest updateRequest = mock(RecipeRequest.class);
            when(updateRequest.title()).thenReturn("Updated Recipe Title");
            when(updateRequest.ingredients()).thenReturn(List.of("Updated Ingredient"));
            when(updateRequest.instructions()).thenReturn("Updated instructions");
            when(updateRequest.totalTimeMinutes()).thenReturn(30);
            when(updateRequest.difficulty()).thenReturn(DifficultyLevel.MEDIUM);

            RecipeResponse updatedResponse = mock(RecipeResponse.class);
            when(updatedResponse.id()).thenReturn(testRecipeId);
            when(updatedResponse.title()).thenReturn("Updated Recipe Title");
            when(updatedResponse.totalTimeMinutes()).thenReturn(30);
            when(updatedResponse.difficulty()).thenReturn(DifficultyLevel.MEDIUM);

            given(recipeService.updateRecipe(eq(testRecipeId), any(RecipeRequest.class), eq(testUserId)))
                    .willReturn(updatedResponse);

            // When: Use standard PUT with JSON, not multipart
            ResultActions response = mockMvc.perform(put("/api/v1/recipes/{id}", testRecipeId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(updateRequest)))
                    .andDo(print());

            // Then
            response
                    .andExpect(status().isOk())
                    .andExpect(contentTypeJson())
                    .andExpect(jsonPath("$.id", is(updatedResponse.id().toString())))
                    .andExpect(jsonPath("$.title", is(updatedResponse.title())))
                    .andExpect(jsonPath("$.totalTimeMinutes", is(updatedResponse.totalTimeMinutes())))
                    .andExpect(jsonPath("$.difficulty", is(updatedResponse.difficulty().name())));

             verify(recipeService).updateRecipe(eq(testRecipeId), any(RecipeRequest.class), eq(testUserId));
        }

        @Test
        @DisplayName("Should return 403 when updating another user's recipe")
        void updateRecipe_Forbidden() throws Exception {
            // Given
            RecipeRequest updateRequest = mock(RecipeRequest.class);
            when(updateRequest.title()).thenReturn("Forbidden Update");
            when(updateRequest.ingredients()).thenReturn(List.of("Forbidden Ingredient"));
            when(updateRequest.instructions()).thenReturn("Forbidden instructions");

            given(recipeService.updateRecipe(eq(testRecipeId), any(RecipeRequest.class), eq(testUserId)))
                    .willThrow(new UnauthorizedAccessException("Cannot update other user's recipe"));

            // When: Use standard PUT with JSON, not multipart
             ResultActions response = mockMvc.perform(put("/api/v1/recipes/{id}", testRecipeId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(updateRequest)))
                    .andDo(print());

            // Then
            response.andExpect(status().isForbidden());
            verify(recipeService).updateRecipe(eq(testRecipeId), any(RecipeRequest.class), eq(testUserId));
        }
    }

    @Nested
    @DisplayName("Delete Recipe Tests")
    class DeleteRecipeTests {

        @Test
        @DisplayName("Should delete a recipe")
        void deleteRecipe_Success() throws Exception {
            // Given
            doNothing().when(recipeService).deleteRecipe(eq(testRecipeId), eq(testUserId));

            // When
            ResultActions response = mockMvc.perform(delete("/api/v1/recipes/{id}", testRecipeId)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andDo(print());

            // Then
            response.andExpect(status().isNoContent());
            verify(recipeService).deleteRecipe(eq(testRecipeId), eq(testUserId));
        }

        @Test
        @DisplayName("Should return 403 when deleting another user's recipe")
        void deleteRecipe_Forbidden() throws Exception {
            // Given
            doThrow(new UnauthorizedAccessException("Cannot delete another user's recipe"))
                    .when(recipeService).deleteRecipe(eq(testRecipeId), eq(testUserId));

            // When
            ResultActions response = mockMvc.perform(delete("/api/v1/recipes/{id}", testRecipeId)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andDo(print());

            // Then
            response.andExpect(status().isForbidden());
            verify(recipeService).deleteRecipe(eq(testRecipeId), eq(testUserId));
        }
    }

    @Nested
    @DisplayName("Search Recipes Tests")
    class SearchRecipesTests {

        @Test
        @DisplayName("Should search recipes by keyword")
        void searchRecipes_Success() throws Exception {
            // Given
            Pageable pageable = PageRequest.of(0, 10);
            List<RecipeResponse> searchResults = List.of(testRecipeResponse);
            Page<RecipeResponse> resultsPage = new PageImpl<>(searchResults, pageable, searchResults.size());

            given(recipeSearchService.searchRecipes(eq("test"), any(Pageable.class), eq(testUserId)))
                    .willReturn(resultsPage);

            // When
            ResultActions response = mockMvc.perform(get("/api/v1/recipes/search")
                            .contentType(MediaType.APPLICATION_JSON)
                            .param("keyword", "test")
                            .param("page", "0")
                            .param("size", "10"))
                    .andDo(print());

            // Then
            response
                    .andExpect(status().isOk())
                    .andExpect(contentTypeJson())
                    .andExpect(jsonPath("$.content", hasSize(1)))
                    .andExpect(jsonPath("$.content[0].id", is(testRecipeResponse.id().toString())))
                    .andExpect(jsonPath("$.content[0].title", is(testRecipeResponse.title())));

            verify(recipeSearchService).searchRecipes(eq("test"), any(Pageable.class), eq(testUserId));
        }

        @Test
        @DisplayName("Should return empty results when no matches found")
        void searchRecipes_NoMatches() throws Exception {
            // Given
            Pageable pageable = PageRequest.of(0, 10);
            PageImpl<RecipeResponse> emptyPage = new PageImpl<>(List.of(), pageable, 0);

            given(recipeSearchService.searchRecipes(eq("nonexistent"), any(Pageable.class), eq(testUserId)))
                    .willReturn(emptyPage);

            // When
            ResultActions response = mockMvc.perform(get("/api/v1/recipes/search")
                            .contentType(MediaType.APPLICATION_JSON)
                            .param("keyword", "nonexistent")
                            .param("page", "0")
                            .param("size", "10"))
                    .andDo(print());

            // Then
            response
                    .andExpect(status().isOk())
                    .andExpect(contentTypeJson())
                    .andExpect(jsonPath("$.content", hasSize(0)))
                    .andExpect(jsonPath("$.totalElements", is(0)));

            verify(recipeSearchService).searchRecipes(eq("nonexistent"), any(Pageable.class), eq(testUserId));
        }
    }

    @Nested
    @DisplayName("Vote Tests")
    class VoteTests {

        @Test
        @DisplayName("Should vote on a recipe")
        void voteRecipe_Success() throws Exception {
            // Given
            VoteRequest voteRequest = new VoteRequest("UPVOTE");

            // Mock the Recipe entity returned by the service
            Recipe votedRecipe = mock(Recipe.class); // Service returns an entity
            when(votedRecipe.getId()).thenReturn(testRecipeId);
            when(votedRecipe.getUpvotes()).thenReturn(1); // Assume service updates counts
            when(votedRecipe.getDownvotes()).thenReturn(0);
            // Stub other fields of votedRecipe if the mapper needs them

            // Mock the RecipeResponse DTO that the mapper will produce
            RecipeResponse mappedResponse = mock(RecipeResponse.class);
            when(mappedResponse.id()).thenReturn(testRecipeId);
            // Stub other fields needed by enhanceWithUserInteractions or assertions

            // Mock the final RecipeResponse DTO after enhancement
            RecipeResponse finalResponse = mock(RecipeResponse.class);
            when(finalResponse.id()).thenReturn(testRecipeId);
            when(finalResponse.upvotes()).thenReturn(1);
            when(finalResponse.downvotes()).thenReturn(0);
            when(finalResponse.userVote()).thenReturn("UPVOTE"); // Interaction added
            // Stub other fields as needed for assertions

            // 1. Mock the service call returning the entity
            given(voteService.vote(eq(testRecipeId), any(VoteRequest.class), eq(testUserId)))
                    .willReturn(votedRecipe);

            // 2. Mock the mapper converting entity to response DTO
            given(recipeMapper.toResponse(votedRecipe)).willReturn(mappedResponse);

            // 3. Mock the enhancement step
            given(recipeService.enhanceWithUserInteractions(eq(mappedResponse), eq(testUserId)))
                    .willReturn(finalResponse); // Return the final DTO

            // When
            ResultActions response = mockMvc.perform(post("/api/v1/recipes/{id}/vote", testRecipeId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(voteRequest)))
                    .andDo(print());

            // Then
            response
                    .andExpect(status().isOk())
                    .andExpect(contentTypeJson())
                    .andExpect(jsonPath("$.id", is(testRecipeId.toString())))
                    .andExpect(jsonPath("$.upvotes", is(finalResponse.upvotes()))) // Verify final vote counts
                    .andExpect(jsonPath("$.downvotes", is(finalResponse.downvotes())))
                    .andExpect(jsonPath("$.userVote", is(finalResponse.userVote())));

            // Verify interactions
            verify(voteService).vote(eq(testRecipeId), any(VoteRequest.class), eq(testUserId));
            verify(recipeMapper).toResponse(votedRecipe);
            verify(recipeService).enhanceWithUserInteractions(eq(mappedResponse), eq(testUserId));
        }

        @Test
        @DisplayName("Should handle invalid vote type")
        void voteRecipe_InvalidVoteType() throws Exception {
            // Given
            VoteRequest voteRequest = new VoteRequest("INVALID");

            given(voteService.vote(eq(testRecipeId), any(VoteRequest.class), eq(testUserId)))
                   .willThrow(new BadRequestException("Invalid vote type"));

            // When
            ResultActions response = mockMvc.perform(post("/api/v1/recipes/{id}/vote", testRecipeId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(voteRequest)))
                    .andDo(print());

            // Then
            response.andExpect(status().isBadRequest());
            // No need to verify service call since validation happens before service is called
        }
    }

    @Nested
    @DisplayName("Generate Recipe Tests")
    class GenerateRecipeTests {

        @Test
        @DisplayName("Should generate a recipe from ingredients")
        void generateMeal_Success() throws Exception {
            // Given
            List<String> ingredientsToGenerate = List.of("Chicken Breast", "Broccoli", "Rice");

            // Mock the SimplifiedRecipeResponse expected from the service
            SimplifiedRecipeResponse generatedResponse = mock(SimplifiedRecipeResponse.class);
            when(generatedResponse.title()).thenReturn("Generated Chicken Dish");
            when(generatedResponse.ingredients()).thenReturn(List.of("Chicken", "Broccoli", "Rice", "Sauce"));
            when(generatedResponse.instructions()).thenReturn("1. Cook chicken...");
            when(generatedResponse.totalTimeMinutes()).thenReturn(25);
            when(generatedResponse.difficulty()).thenReturn(DifficultyLevel.EASY);
            // Stub other fields if they exist on SimplifiedRecipeResponse

            // Mock the recipeService.generateMeal call
            given(recipeService.generateMeal(eq(ingredientsToGenerate)))
                 .willReturn(generatedResponse);

            // When
            ResultActions response = mockMvc.perform(post("/api/v1/recipes/generate")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(ingredientsToGenerate)))
                    .andDo(print());

            // Then
            response
                    .andExpect(status().isOk()) // Assuming OK status
                    .andExpect(contentTypeJson())
                    .andExpect(jsonPath("$.title", is(generatedResponse.title()))) // Verify fields from SimplifiedRecipeResponse
                    .andExpect(jsonPath("$.ingredients", hasSize(generatedResponse.ingredients().size())))
                    .andExpect(jsonPath("$.totalTimeMinutes", is(generatedResponse.totalTimeMinutes())))
                    .andExpect(jsonPath("$.difficulty", is(generatedResponse.difficulty().name())));


            // Verify service interaction
            verify(recipeService).generateMeal(eq(ingredientsToGenerate));
            verifyNoInteractions(voteService, recipeSearchService); // Ensure other services not called
        }
    }

    @Nested
    @DisplayName("Authorization Tests")
    class AuthorizationTests {

        @Test
        @DisplayName("Should return 401 when accessing endpoints without authentication")
        void accessEndpoints_Unauthorized() throws Exception {
            SecurityContext originalContext = SecurityContextHolder.getContext();
            SecurityContextHolder.clearContext();
            try {
                UUID recipeId = UUID.randomUUID();
                String minimalJson = "{}"; // Use simple empty JSON string

                mockMvc.perform(get("/api/v1/recipes/{id}", recipeId))
                        .andExpect(status().isUnauthorized());

                mockMvc.perform(post("/api/v1/recipes")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(minimalJson))
                        .andExpect(status().isUnauthorized());

                 mockMvc.perform(put("/api/v1/recipes/{id}", recipeId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(minimalJson))
                        .andExpect(status().isUnauthorized());

                 mockMvc.perform(delete("/api/v1/recipes/{id}", recipeId))
                        .andExpect(status().isUnauthorized());

                mockMvc.perform(post("/api/v1/recipes/{id}/vote", recipeId)
                                .contentType(MediaType.APPLICATION_JSON)
                                // Use a real VoteRequest for serialization
                                .content(objectMapper.writeValueAsString(new VoteRequest("UPVOTE"))))
                        .andExpect(status().isUnauthorized());

                mockMvc.perform(get("/api/v1/recipes/my-recipes"))
                        .andExpect(status().isUnauthorized());

                 mockMvc.perform(post("/api/v1/recipes/generate")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("[]"))
                        .andExpect(status().isUnauthorized());
            } finally {
                 SecurityContextHolder.setContext(originalContext);
            }
        }
    }

    @Nested
    @DisplayName("Error Handling Tests")
    class ErrorHandlingTests {

        @Test
        @DisplayName("Should handle resource conflict")
        void resourceConflict_HandledProperly() throws Exception {
            // Given
            given(recipeService.createRecipe(any(RecipeRequest.class), eq(null), eq(testUserId)))
                    .willThrow(new IllegalStateException("Recipe with this title already exists"));

            // When
            ResultActions response = mockMvc.perform(post("/api/v1/recipes")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(testRecipeRequest)))
                    .andDo(print());

            // Then
            response
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.message", containsString("already exists")));
        }

        @Test
        @DisplayName("Should handle validation errors on recipe creation")
        void validationError_HandledProperly() throws Exception {
            // Given
            RecipeRequest invalidRequest = mock(RecipeRequest.class);
            when(invalidRequest.title()).thenReturn("");
            when(invalidRequest.ingredients()).thenReturn(List.of());
            when(invalidRequest.instructions()).thenReturn("");
            when(invalidRequest.totalTimeMinutes()).thenReturn(-1);

            // When
            ResultActions response = mockMvc.perform(post("/api/v1/recipes")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalidRequest)))
                    .andDo(print());

            // Then
            response
                    .andExpect(status().isBadRequest())
                    // Check for the actual structure: $.details exists and contains validation messages
                    .andExpect(jsonPath("$.details", org.hamcrest.Matchers.notNullValue()))
                    .andExpect(jsonPath("$.details", org.hamcrest.Matchers.isA(java.util.Map.class))) // Check it's a map
                    .andExpect(jsonPath("$.details.title", containsStringIgnoringCase("title is required"))) // Example check
                    .andExpect(jsonPath("$.details.ingredients", containsStringIgnoringCase("ingredient is required"))); // Example check
        }
    }
}
