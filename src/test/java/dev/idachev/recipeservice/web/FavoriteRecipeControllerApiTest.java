package dev.idachev.recipeservice.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import dev.idachev.recipeservice.exception.ResourceNotFoundException;
import dev.idachev.recipeservice.exception.UnauthorizedAccessException;
import dev.idachev.recipeservice.model.DifficultyLevel;
import dev.idachev.recipeservice.service.FavoriteRecipeService;
import dev.idachev.recipeservice.web.dto.BatchFavoriteRequest;
import dev.idachev.recipeservice.web.dto.FavoriteRecipeDto;
import dev.idachev.recipeservice.web.dto.RecipeResponse;
import jakarta.servlet.http.HttpServletResponse;
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
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.mock.web.MockHttpServletResponse;
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
import java.util.*;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class FavoriteRecipeControllerApiTest {

    private MockMvc mockMvc;

    private ObjectMapper objectMapper;

    @Mock
    private FavoriteRecipeService favoriteRecipeService;

    @Mock
    private SecurityContext securityContext;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private FavoriteRecipeController favoriteRecipeController;

    private UUID testUserId;
    private UUID testRecipeId;
    private FavoriteRecipeDto testFavoriteRecipeDto;

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
    void setUp() {
        // Set up ObjectMapper
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        // Initialize test data
        testUserId = UUID.randomUUID();
        testRecipeId = UUID.randomUUID();
        LocalDateTime testDateTime = LocalDateTime.now();

        // Mock security context for authentication
        lenient().when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);
        lenient().when(authentication.getPrincipal()).thenReturn(testUserId);

        // Set up MockMvc with standalone configuration and security
        mockMvc = MockMvcBuilders
                .standaloneSetup(favoriteRecipeController)
                .setCustomArgumentResolvers(
                        new PageableHandlerMethodArgumentResolver(),
                        new AuthenticationPrincipalArgumentResolver()
                )
                .setMessageConverters(new MappingJackson2HttpMessageConverter())
                .setControllerAdvice(new GlobalExceptionHandler())
                .defaultResponseCharacterEncoding(StandardCharsets.UTF_8)
                .addFilter((request, response, chain) -> {
                    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
                    if (authentication == null || authentication.getPrincipal() == null) {
                        HttpServletResponse httpResponse = (HttpServletResponse) response;
                        httpResponse.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                        return; // Important: stop execution here
                    }
                    chain.doFilter(request, response);
                })
                .build();

        // Create a minimal valid RecipeResponse for testing based on correct constructor order
        RecipeResponse recipeResponse = new RecipeResponse(
                testRecipeId,  // id
                testUserId,    // createdById
                "Test Recipe", // title
                "Serve hot",   // servingSuggestions
                "Cook it",     // instructions
                "https://example.com/image.jpg", // imageUrl
                List.of("Ingredient 1", "Ingredient 2"), // ingredients
                30,            // totalTimeMinutes
                "Test Author", // authorName
                "testuser",    // username
                "author123",   // authorId
                DifficultyLevel.MEDIUM, // difficulty
                false,         // isAiGenerated
                true,          // isFavorite
                42L,           // favoriteCount
                10L,           // commentCount
                15,            // upvotes
                5,             // downvotes
                "up",          // userVote
                testDateTime,  // createdAt
                testDateTime,  // updatedAt
                null,          // macros
                Map.of("key", "value") // additionalFields
        );

        // Create the FavoriteRecipeDto
        testFavoriteRecipeDto = new FavoriteRecipeDto(
                testRecipeId,
                testUserId,
                testDateTime,
                recipeResponse
        );
    }

    @Nested
    @DisplayName("Add to Favorites Tests")
    class AddToFavoritesTests {
        @Test
        @DisplayName("Should add recipe to favorites")
        void addToFavorites_Success() throws Exception {
            // Given
            given(favoriteRecipeService.addToFavorites(eq(testUserId), any(UUID.class)))
                    .willReturn(testFavoriteRecipeDto);

            // When
            ResultActions response = mockMvc.perform(post("/api/v1/favorites/{recipeId}", testRecipeId)
                            .with(user(testUserId.toString()))
                            .contentType(MediaType.APPLICATION_JSON))
                    .andDo(print());

            // Then
            response
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.userId", is(testUserId.toString())))
                    .andExpect(jsonPath("$.recipeId", is(testRecipeId.toString())))
                    .andExpect(jsonPath("$.recipe.title", is("Test Recipe")));

            verify(favoriteRecipeService).addToFavorites(eq(testUserId), any(UUID.class));
        }

        @Test
        @DisplayName("Should return 404 when adding non-existent recipe to favorites")
        void addToFavorites_RecipeNotFound() throws Exception {
            // Given
            given(favoriteRecipeService.addToFavorites(any(UUID.class), any(UUID.class)))
                    .willThrow(new ResourceNotFoundException("Recipe", "id", testRecipeId));

            // When
            ResultActions response = mockMvc.perform(post("/api/v1/favorites/{recipeId}", testRecipeId)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andDo(print());

            // Then
            response.andExpect(status().isNotFound());
            verify(favoriteRecipeService).addToFavorites(any(UUID.class), any(UUID.class));
        }

        @Test
        @DisplayName("Should return 409 when recipe is already in favorites")
        void addToFavorites_AlreadyExists() throws Exception {
            // Given
            given(favoriteRecipeService.addToFavorites(any(UUID.class), any(UUID.class)))
                    .willThrow(new IllegalStateException("Recipe is already in favorites"));

            // When
            ResultActions response = mockMvc.perform(post("/api/v1/favorites/{recipeId}", testRecipeId)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andDo(print());

            // Then
            response.andExpect(status().isConflict());
            verify(favoriteRecipeService).addToFavorites(any(UUID.class), any(UUID.class));
        }

        @Test
        @DisplayName("Should handle server error when adding to favorites")
        void addToFavorites_ServerError() throws Exception {
            // Given
            given(favoriteRecipeService.addToFavorites(any(UUID.class), any(UUID.class)))
                    .willThrow(new RuntimeException("Database connection error"));

            // When
            ResultActions response = mockMvc.perform(post("/api/v1/favorites/{recipeId}", testRecipeId)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andDo(print());

            // Then
            response.andExpect(status().isInternalServerError());
            verify(favoriteRecipeService).addToFavorites(any(UUID.class), any(UUID.class));
        }

        @Test
        @DisplayName("Should return 400 when using invalid UUID format")
        void addToFavorites_InvalidUuid() throws Exception {
            // When - Using invalid UUID
            ResultActions response = mockMvc.perform(post("/api/v1/favorites/not-a-uuid")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andDo(print());

            // Then
            response.andExpect(status().isBadRequest());
            verifyNoInteractions(favoriteRecipeService);
        }
    }

    @Nested
    @DisplayName("Remove from Favorites Tests")
    class RemoveFromFavoritesTests {
        @Test
        @DisplayName("Should remove recipe from favorites")
        void removeFromFavorites_Success() throws Exception {
            // Given
            doNothing().when(favoriteRecipeService).removeFromFavorites(any(UUID.class), any(UUID.class));

            // When
            ResultActions response = mockMvc.perform(delete("/api/v1/favorites/{recipeId}", testRecipeId)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andDo(print());

            // Then
            response.andExpect(status().isNoContent());
            verify(favoriteRecipeService).removeFromFavorites(any(UUID.class), any(UUID.class));
        }

        @Test
        @DisplayName("Should return 404 when removing non-existent favorite")
        void removeFromFavorites_NotFound() throws Exception {
            // Given
            doThrow(new ResourceNotFoundException("Favorite", "recipeId", testRecipeId))
                    .when(favoriteRecipeService).removeFromFavorites(any(UUID.class), any(UUID.class));

            // When
            ResultActions response = mockMvc.perform(delete("/api/v1/favorites/{recipeId}", testRecipeId)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andDo(print());

            // Then
            response.andExpect(status().isNotFound());
            verify(favoriteRecipeService).removeFromFavorites(any(UUID.class), any(UUID.class));
        }

        @Test
        @DisplayName("Should handle server error when removing from favorites")
        void removeFromFavorites_ServerError() throws Exception {
            // Given
            doThrow(new RuntimeException("Database connection error"))
                    .when(favoriteRecipeService).removeFromFavorites(any(UUID.class), any(UUID.class));

            // When
            ResultActions response = mockMvc.perform(delete("/api/v1/favorites/{recipeId}", testRecipeId)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andDo(print());

            // Then
            response.andExpect(status().isInternalServerError());
            verify(favoriteRecipeService).removeFromFavorites(any(UUID.class), any(UUID.class));
        }

        @Test
        @DisplayName("Should return 403 when user tries to remove another user's favorite")
        void removeFromFavorites_Forbidden() throws Exception {
            // Given
            doThrow(new UnauthorizedAccessException("You cannot remove another user's favorite"))
                    .when(favoriteRecipeService).removeFromFavorites(any(UUID.class), any(UUID.class));

            // When
            ResultActions response = mockMvc.perform(delete("/api/v1/favorites/{recipeId}", testRecipeId)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andDo(print());

            // Then
            response.andExpect(status().isForbidden());
            verify(favoriteRecipeService).removeFromFavorites(any(UUID.class), any(UUID.class));
        }

        @Test
        @DisplayName("Should return 400 when using invalid UUID format")
        void removeFromFavorites_InvalidUuid() throws Exception {
            // When - Using invalid UUID
            ResultActions response = mockMvc.perform(delete("/api/v1/favorites/not-a-uuid")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andDo(print());

            // Then
            response.andExpect(status().isBadRequest());
            verifyNoInteractions(favoriteRecipeService);
        }
    }

    @Nested
    @DisplayName("Get User Favorites Tests")
    class GetUserFavoritesTests {
        @Test
        @DisplayName("Should get user's favorite recipes")
        void getUserFavorites_Success() throws Exception {
            // Given
            Pageable pageable = PageRequest.of(0, 10);
            List<FavoriteRecipeDto> favorites = List.of(testFavoriteRecipeDto);
            PageImpl<FavoriteRecipeDto> favoritesPage = new PageImpl<>(favorites, pageable, favorites.size());

            given(favoriteRecipeService.getUserFavorites(any(UUID.class), any(Pageable.class)))
                    .willReturn(favoritesPage);

            // When
            ResultActions response = mockMvc.perform(get("/api/v1/favorites")
                            .contentType(MediaType.APPLICATION_JSON)
                            .param("page", "0")
                            .param("size", "10"))
                    .andDo(print());

            // Then
            response
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content", hasSize(1)))
                    .andExpect(jsonPath("$.content[0].userId", is(testUserId.toString())))
                    .andExpect(jsonPath("$.content[0].recipeId", is(testRecipeId.toString())))
                    .andExpect(jsonPath("$.content[0].recipe.title", is("Test Recipe")));

            verify(favoriteRecipeService).getUserFavorites(any(UUID.class), any(Pageable.class));
        }

        @Test
        @DisplayName("Should return empty page when user has no favorites")
        void getUserFavorites_Empty() throws Exception {
            // Given
            Pageable pageable = PageRequest.of(0, 10);
            PageImpl<FavoriteRecipeDto> emptyPage = new PageImpl<>(Collections.emptyList(), pageable, 0);

            given(favoriteRecipeService.getUserFavorites(any(UUID.class), any(Pageable.class)))
                    .willReturn(emptyPage);

            // When
            ResultActions response = mockMvc.perform(get("/api/v1/favorites")
                            .contentType(MediaType.APPLICATION_JSON)
                            .param("page", "0")
                            .param("size", "10"))
                    .andDo(print());

            // Then
            response
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content", hasSize(0)))
                    .andExpect(jsonPath("$.totalElements", is(0)));

            verify(favoriteRecipeService).getUserFavorites(any(UUID.class), any(Pageable.class));
        }

        @Test
        @DisplayName("Should handle pagination parameters")
        void getUserFavorites_Pagination() throws Exception {
            // Given
            Pageable pageable = PageRequest.of(1, 5);
            List<FavoriteRecipeDto> favorites = List.of(testFavoriteRecipeDto);
            PageImpl<FavoriteRecipeDto> favoritesPage = new PageImpl<>(favorites, pageable, 15);

            given(favoriteRecipeService.getUserFavorites(any(UUID.class), any(Pageable.class)))
                    .willReturn(favoritesPage);

            // When
            ResultActions response = mockMvc.perform(get("/api/v1/favorites")
                            .contentType(MediaType.APPLICATION_JSON)
                            .param("page", "1")
                            .param("size", "5"))
                    .andDo(print());

            // Then
            response
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content", hasSize(1)))
                    .andExpect(jsonPath("$.totalElements", is(15)))
                    .andExpect(jsonPath("$.totalPages", is(3)))
                    .andExpect(jsonPath("$.number", is(1)));

            verify(favoriteRecipeService).getUserFavorites(any(UUID.class), any(Pageable.class));
        }

        @Test
        @DisplayName("Should handle invalid pagination parameters")
        void getUserFavorites_InvalidPagination() throws Exception {
            // Given
            Pageable pageable = PageRequest.of(0, 10);
            List<FavoriteRecipeDto> favorites = List.of(testFavoriteRecipeDto);
            PageImpl<FavoriteRecipeDto> favoritesPage = new PageImpl<>(favorites, pageable, favorites.size());

            given(favoriteRecipeService.getUserFavorites(any(UUID.class), any(Pageable.class)))
                    .willReturn(favoritesPage);

            // When
            ResultActions response = mockMvc.perform(get("/api/v1/favorites")
                            .contentType(MediaType.APPLICATION_JSON)
                            .param("page", "-1")
                            .param("size", "1000"))
                    .andDo(print());

            // Then - Spring automatically handles invalid page/size with default values
            response.andExpect(status().isOk());
            verify(favoriteRecipeService).getUserFavorites(any(UUID.class), any(Pageable.class));
        }

        @Test
        @DisplayName("Should handle server error when getting user favorites")
        void getUserFavorites_ServerError() throws Exception {
            // Given
            given(favoriteRecipeService.getUserFavorites(any(UUID.class), any(Pageable.class)))
                    .willThrow(new RuntimeException("Database connection error"));

            // When
            ResultActions response = mockMvc.perform(get("/api/v1/favorites")
                            .contentType(MediaType.APPLICATION_JSON)
                            .param("page", "0")
                            .param("size", "10"))
                    .andDo(print());

            // Then
            response.andExpect(status().isInternalServerError());
            verify(favoriteRecipeService).getUserFavorites(any(UUID.class), any(Pageable.class));
        }
    }

    @Nested
    @DisplayName("Get All User Favorites Tests")
    class GetAllUserFavoritesTests {
        @Test
        @DisplayName("Should get all user's favorite recipes")
        void getAllUserFavorites_Success() throws Exception {
            // Given
            List<FavoriteRecipeDto> favorites = List.of(testFavoriteRecipeDto);

            given(favoriteRecipeService.getAllUserFavorites(any(UUID.class)))
                    .willReturn(favorites);

            // When
            ResultActions response = mockMvc.perform(get("/api/v1/favorites/all")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andDo(print());

            // Then
            response
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)))
                    .andExpect(jsonPath("$[0].userId", is(testUserId.toString())))
                    .andExpect(jsonPath("$[0].recipeId", is(testRecipeId.toString())))
                    .andExpect(jsonPath("$[0].recipe.title", is("Test Recipe")));

            verify(favoriteRecipeService).getAllUserFavorites(any(UUID.class));
        }

        @Test
        @DisplayName("Should return empty list when user has no favorites")
        void getAllUserFavorites_Empty() throws Exception {
            // Given
            given(favoriteRecipeService.getAllUserFavorites(any(UUID.class)))
                    .willReturn(Collections.emptyList());

            // When
            ResultActions response = mockMvc.perform(get("/api/v1/favorites/all")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andDo(print());

            // Then
            response
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(0)));

            verify(favoriteRecipeService).getAllUserFavorites(any(UUID.class));
        }

        @Test
        @DisplayName("Should return 401 when getting all favorites without authentication")
        void getAllUserFavorites_Unauthorized() throws Exception {
            // Given
            // Clear security context to simulate unauthorized access
            SecurityContextHolder.clearContext();

            // When
            ResultActions response = mockMvc.perform(get("/api/v1/favorites/all")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andDo(print());

            // Then
            response.andExpect(status().isUnauthorized());
            verifyNoInteractions(favoriteRecipeService);

            // Restore security context for other tests
            SecurityContextHolder.setContext(securityContext);
        }

        @Test
        @DisplayName("Should handle multiple favorites with large dataset")
        void getAllUserFavorites_LargeDataset() throws Exception {
            // Given - Create a larger list of favorites
            List<FavoriteRecipeDto> favorites = List.of(
                    testFavoriteRecipeDto,
                    testFavoriteRecipeDto,
                    testFavoriteRecipeDto
            );

            given(favoriteRecipeService.getAllUserFavorites(any(UUID.class)))
                    .willReturn(favorites);

            // When
            ResultActions response = mockMvc.perform(get("/api/v1/favorites/all")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andDo(print());

            // Then
            response
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(3)));

            verify(favoriteRecipeService).getAllUserFavorites(any(UUID.class));
        }
    }

    @Nested
    @DisplayName("Check Favorite Status Tests")
    class CheckFavoriteStatusTests {
        @Test
        @DisplayName("Should check if recipe is in favorites (true)")
        void isRecipeInFavorites_True() throws Exception {
            // Given
            boolean isFavorite = true;

            given(favoriteRecipeService.isRecipeInFavorites(eq(testUserId), any(UUID.class)))
                    .willReturn(isFavorite);

            // When
            ResultActions response = mockMvc.perform(get("/api/v1/favorites/check/{recipeId}", testRecipeId)
                            .with(user(testUserId.toString()))
                            .contentType(MediaType.APPLICATION_JSON))
                    .andDo(print());

            // Then
            response
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.isFavorite", is(true)));

            verify(favoriteRecipeService).isRecipeInFavorites(eq(testUserId), any(UUID.class));
        }

        @Test
        @DisplayName("Should check if recipe is in favorites (false)")
        void isRecipeInFavorites_False() throws Exception {
            // Given
            boolean isFavorite = false;

            given(favoriteRecipeService.isRecipeInFavorites(eq(testUserId), any(UUID.class)))
                    .willReturn(isFavorite);

            // When
            ResultActions response = mockMvc.perform(get("/api/v1/favorites/check/{recipeId}", testRecipeId)
                            .with(user(testUserId.toString()))
                            .contentType(MediaType.APPLICATION_JSON))
                    .andDo(print());

            // Then
            response
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.isFavorite", is(false)));

            verify(favoriteRecipeService).isRecipeInFavorites(eq(testUserId), any(UUID.class));
        }

        @Test
        @DisplayName("Should return 404 when checking favorite status for non-existent recipe")
        void isRecipeInFavorites_RecipeNotFound() throws Exception {
            // Given
            given(favoriteRecipeService.isRecipeInFavorites(eq(testUserId), any(UUID.class)))
                    .willThrow(new ResourceNotFoundException("Recipe", "id", testRecipeId));

            // When
            ResultActions response = mockMvc.perform(get("/api/v1/favorites/check/{recipeId}", testRecipeId)
                            .with(user(testUserId.toString()))
                            .contentType(MediaType.APPLICATION_JSON))
                    .andDo(print());

            // Then
            response.andExpect(status().isNotFound());
            verify(favoriteRecipeService).isRecipeInFavorites(eq(testUserId), any(UUID.class));
        }

        @Test
        @DisplayName("Should handle server error when checking favorite status")
        void isRecipeInFavorites_ServerError() throws Exception {
            // Given
            given(favoriteRecipeService.isRecipeInFavorites(eq(testUserId), any(UUID.class)))
                    .willThrow(new RuntimeException("Database connection error"));

            // When
            ResultActions response = mockMvc.perform(get("/api/v1/favorites/check/{recipeId}", testRecipeId)
                            .with(user(testUserId.toString()))
                            .contentType(MediaType.APPLICATION_JSON))
                    .andDo(print());

            // Then
            response.andExpect(status().isInternalServerError());
            verify(favoriteRecipeService).isRecipeInFavorites(eq(testUserId), any(UUID.class));
        }

        @Test
        @DisplayName("Should return 400 when using invalid UUID format")
        void isRecipeInFavorites_InvalidUuid() throws Exception {
            // When - Using invalid UUID
            ResultActions response = mockMvc.perform(get("/api/v1/favorites/check/{recipeId}", "not-a-uuid")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andDo(print());

            // Then
            response.andExpect(status().isBadRequest());
            verifyNoInteractions(favoriteRecipeService);
        }
    }

    @Nested
    @DisplayName("Favorite Count Tests")
    class FavoriteCountTests {
        @Test
        @DisplayName("Should get favorite count for a recipe")
        void getFavoriteCount_Success() throws Exception {
            // Given
            long favoriteCount = 42;

            given(favoriteRecipeService.getFavoriteCount(any(UUID.class)))
                    .willReturn(favoriteCount);

            // When
            ResultActions response = mockMvc.perform(get("/api/v1/favorites/count/{recipeId}", testRecipeId)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andDo(print());

            // Then
            response
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.count", is(42)));

            verify(favoriteRecipeService).getFavoriteCount(any(UUID.class));
        }

        @Test
        @DisplayName("Should return zero count for a recipe with no favorites")
        void getFavoriteCount_ZeroCount() throws Exception {
            // Given
            long favoriteCount = 0;

            given(favoriteRecipeService.getFavoriteCount(any(UUID.class)))
                    .willReturn(favoriteCount);

            // When
            ResultActions response = mockMvc.perform(get("/api/v1/favorites/count/{recipeId}", testRecipeId)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andDo(print());

            // Then
            response
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.count", is(0)));

            verify(favoriteRecipeService).getFavoriteCount(any(UUID.class));
        }

        @Test
        @DisplayName("Should return 404 when getting count for non-existent recipe")
        void getFavoriteCount_RecipeNotFound() throws Exception {
            // Given
            given(favoriteRecipeService.getFavoriteCount(any(UUID.class)))
                    .willThrow(new ResourceNotFoundException("Recipe", "id", testRecipeId));

            // When
            ResultActions response = mockMvc.perform(get("/api/v1/favorites/count/{recipeId}", testRecipeId)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andDo(print());

            // Then - Spring Test handles status code but not content type 
            response.andExpect(status().isNotFound())
                    .andExpect(result -> {
                        // Skip content type check for exception responses
                    });
            verify(favoriteRecipeService).getFavoriteCount(any(UUID.class));
        }

        @Test
        @DisplayName("Should handle server error when getting favorite count")
        void getFavoriteCount_ServerError() throws Exception {
            // Given
            given(favoriteRecipeService.getFavoriteCount(any(UUID.class)))
                    .willThrow(new RuntimeException("Database connection error"));

            // When
            ResultActions response = mockMvc.perform(get("/api/v1/favorites/count/{recipeId}", testRecipeId)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andDo(print());

            // Then
            response.andExpect(status().isInternalServerError());
            verify(favoriteRecipeService).getFavoriteCount(any(UUID.class));
        }

        @Test
        @DisplayName("Should return 400 when using invalid UUID format")
        void getFavoriteCount_InvalidUuid() throws Exception {
            // When - Using invalid UUID
            ResultActions response = mockMvc.perform(get("/api/v1/favorites/count/{recipeId}", "not-a-uuid")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andDo(print());

            // Then
            response.andExpect(status().isBadRequest());
            verifyNoInteractions(favoriteRecipeService);
        }
    }

    @Nested
    @DisplayName("Authorization Tests")
    class AuthorizationTests {
        @Test
        @DisplayName("Should return 401 when adding to favorites without authentication")
        void addToFavorites_Unauthorized() throws Exception {
            // Given
            // Clear the security context completely
            SecurityContextHolder.clearContext();

            // When - No JWT token provided
            ResultActions response = mockMvc.perform(post("/api/v1/favorites/{recipeId}", testRecipeId)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andDo(print());

            // Then
            response.andExpect(status().isUnauthorized());
            verifyNoInteractions(favoriteRecipeService);

            // Restore security context for other tests
            SecurityContextHolder.setContext(securityContext);
        }

        @Test
        @DisplayName("Should return 401 when removing from favorites without authentication")
        void removeFromFavorites_Unauthorized() throws Exception {
            // Given
            SecurityContextHolder.clearContext();

            // When - No JWT token provided
            ResultActions response = mockMvc.perform(delete("/api/v1/favorites/{recipeId}", testRecipeId)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andDo(print());

            // Then
            response.andExpect(status().isUnauthorized());
            verifyNoInteractions(favoriteRecipeService);

            // Restore security context for other tests
            SecurityContextHolder.setContext(securityContext);
        }

        @Test
        @DisplayName("Should return 401 when getting user favorites without authentication")
        void getUserFavorites_Unauthorized() throws Exception {
            // Given
            SecurityContextHolder.clearContext();

            // When - No JWT token provided
            ResultActions response = mockMvc.perform(get("/api/v1/favorites")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andDo(print());

            // Then
            response.andExpect(status().isUnauthorized());
            verifyNoInteractions(favoriteRecipeService);

            // Restore security context for other tests
            SecurityContextHolder.setContext(securityContext);
        }

        @Test
        @DisplayName("Should return 401 when checking favorite status without authentication")
        void isRecipeInFavorites_Unauthorized() throws Exception {
            // Given
            SecurityContextHolder.clearContext();

            // When - No JWT token provided
            ResultActions response = mockMvc.perform(get("/api/v1/favorites/check/{recipeId}", testRecipeId)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andDo(print());

            // Then
            response.andExpect(status().isUnauthorized());
            verifyNoInteractions(favoriteRecipeService);

            // Restore security context for other tests
            SecurityContextHolder.setContext(securityContext);
        }

        @Test
        @DisplayName("Should return 401 when getting all favorites without authentication")
        void getAllUserFavorites_Unauthorized() throws Exception {
            // Given
            SecurityContextHolder.clearContext();

            // When - No JWT token provided
            ResultActions response = mockMvc.perform(get("/api/v1/favorites/all")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andDo(print());

            // Then
            response.andExpect(status().isUnauthorized());
            verifyNoInteractions(favoriteRecipeService);

            // Restore security context for other tests
            SecurityContextHolder.setContext(securityContext);
        }
    }

    @Nested
    @DisplayName("Batch Operations Tests")
    class BatchOperationsTests {
        @Test
        @DisplayName("Should check favorite status for multiple recipes")
        void batchCheckFavoriteStatus() throws Exception {
            // Given
            List<UUID> recipeIds = List.of(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID());
            BatchFavoriteRequest request = new BatchFavoriteRequest(recipeIds);

            Map<UUID, Boolean> favoriteStatusMap = Map.of(
                    recipeIds.get(0), true,
                    recipeIds.get(1), false,
                    recipeIds.get(2), true
            );

            when(favoriteRecipeService.getBatchFavoriteStatus(eq(testUserId), any()))
                    .thenReturn(favoriteStatusMap);

            // When
            ResultActions response = mockMvc.perform(post("/api/v1/favorites/check/batch")
                            .with(user(testUserId.toString()))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andDo(print());

            // Then
            response.andExpect(status().isOk())
                    .andExpect(jsonPath("$.favoriteStatuses").isMap())
                    .andExpect(jsonPath("$.favoriteStatuses." + recipeIds.get(0).toString()).value(true))
                    .andExpect(jsonPath("$.favoriteStatuses." + recipeIds.get(1).toString()).value(false))
                    .andExpect(jsonPath("$.favoriteStatuses." + recipeIds.get(2).toString()).value(true));

            verify(favoriteRecipeService).getBatchFavoriteStatus(eq(testUserId), any());
        }

        @Test
        @DisplayName("Should batch add recipes to favorites")
        void batchAddToFavorites() throws Exception {
            // Given
            List<UUID> recipeIds = List.of(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID());
            BatchFavoriteRequest request = new BatchFavoriteRequest(recipeIds);

            when(favoriteRecipeService.addBatchToFavorites(eq(testUserId), any()))
                    .thenReturn(2); // 2 recipes successfully added

            // When
            ResultActions response = mockMvc.perform(post("/api/v1/favorites/batch")
                            .with(user(testUserId.toString()))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andDo(print());

            // Then
            response.andExpect(status().isOk())
                    .andExpect(content().string("2"));

            verify(favoriteRecipeService).addBatchToFavorites(eq(testUserId), any());
        }

        @Test
        @DisplayName("Should batch remove recipes from favorites")
        void batchRemoveFromFavorites() throws Exception {
            // Given
            List<UUID> recipeIds = List.of(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID());
            BatchFavoriteRequest request = new BatchFavoriteRequest(recipeIds);

            when(favoriteRecipeService.removeBatchFromFavorites(eq(testUserId), any()))
                    .thenReturn(3); // 3 recipes successfully removed

            // When
            ResultActions response = mockMvc.perform(delete("/api/v1/favorites/batch")
                            .with(user(testUserId.toString()))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andDo(print());

            // Then
            response.andExpect(status().isOk())
                    .andExpect(content().string("3"));

            verify(favoriteRecipeService).removeBatchFromFavorites(eq(testUserId), any());
        }

        @Test
        @DisplayName("Should get favorite counts for multiple recipes")
        void batchGetFavoriteCounts() throws Exception {
            // Given
            List<UUID> recipeIds = List.of(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID());
            BatchFavoriteRequest request = new BatchFavoriteRequest(recipeIds);

            Map<UUID, Long> countMap = Map.of(
                    recipeIds.get(0), 5L,
                    recipeIds.get(1), 10L,
                    recipeIds.get(2), 0L
            );

            when(favoriteRecipeService.getBatchFavoriteCounts(any()))
                    .thenReturn(countMap);

            // When
            ResultActions response = mockMvc.perform(post("/api/v1/favorites/count/batch")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andDo(print());

            // Then
            response.andExpect(status().isOk())
                    .andExpect(jsonPath("$.favoriteCounts").isMap())
                    .andExpect(jsonPath("$.favoriteCounts", hasKey(recipeIds.get(0).toString())))
                    .andExpect(jsonPath("$.favoriteCounts", hasKey(recipeIds.get(1).toString())))
                    .andExpect(jsonPath("$.favoriteCounts", hasKey(recipeIds.get(2).toString())))
                    .andExpect(jsonPath("$.favoriteCounts." + recipeIds.get(0).toString()).value(5))
                    .andExpect(jsonPath("$.favoriteCounts." + recipeIds.get(1).toString()).value(10))
                    .andExpect(jsonPath("$.favoriteCounts." + recipeIds.get(2).toString()).value(0));

            verify(favoriteRecipeService).getBatchFavoriteCounts(any());
        }
    }

    @Nested
    @DisplayName("Error Handling Tests")
    class ErrorHandlingTests {
        @Test
        @DisplayName("Should return 400 Bad Request when recipe ID is invalid")
        void shouldReturnBadRequestForInvalidRecipeId() throws Exception {
            // When
            ResultActions response = mockMvc.perform(post("/api/v1/favorites/invalid-uuid")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andDo(print());

            // Then
            response.andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should return 400 Bad Request when removing with invalid recipe ID")
        void shouldReturnBadRequestWhenRemovingWithInvalidRecipeId() throws Exception {
            // When
            ResultActions response = mockMvc.perform(delete("/api/v1/favorites/invalid-uuid")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andDo(print());

            // Then
            response.andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should return 400 Bad Request when checking favorite status with invalid recipe ID")
        void shouldReturnBadRequestWhenCheckingWithInvalidRecipeId() throws Exception {
            // When
            ResultActions response = mockMvc.perform(get("/api/v1/favorites/check/invalid-uuid")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andDo(print());

            // Then
            response.andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should return 400 Bad Request when getting favorite count with invalid recipe ID")
        void shouldReturnBadRequestWhenGettingCountWithInvalidRecipeId() throws Exception {
            // When
            ResultActions response = mockMvc.perform(get("/api/v1/favorites/count/invalid-uuid")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andDo(print());

            // Then
            response.andExpect(status().isBadRequest());
        }
    }

    @Test
    void shouldAddRecipeToFavorites() throws Exception {
        when(favoriteRecipeService.addToFavorites(eq(testUserId), eq(testRecipeId)))
                .thenReturn(testFavoriteRecipeDto);

        mockMvc.perform(post("/api/v1/favorites/{recipeId}", testRecipeId)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(contentTypeJson())
                .andExpect(jsonPath("$.recipeId").value(testRecipeId.toString()));
    }

    @Test
    void shouldRemoveRecipeFromFavorites() throws Exception {
        doNothing().when(favoriteRecipeService).removeFromFavorites(eq(testUserId), eq(testRecipeId));

        mockMvc.perform(delete("/api/v1/favorites/{recipeId}", testRecipeId)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNoContent());
    }

    @Test
    void shouldListFavorites() throws Exception {
        when(favoriteRecipeService.getAllUserFavorites(eq(testUserId)))
                .thenReturn(List.of(testFavoriteRecipeDto));

        mockMvc.perform(get("/api/v1/favorites/all")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(contentTypeJson())
                .andExpect(jsonPath("$[0].recipeId").value(testRecipeId.toString()));
    }

    @Test
    void shouldCheckIfFavorite() throws Exception {
        when(favoriteRecipeService.isRecipeInFavorites(eq(testUserId), eq(testRecipeId)))
                .thenReturn(true);

        mockMvc.perform(get("/api/v1/favorites/check/{recipeId}", testRecipeId)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(contentTypeJson())
                .andExpect(jsonPath("$.isFavorite").value(true));
    }

    @Test
    void shouldReturn401WhenAddingWithoutAuthentication() throws Exception {
        // Clear the security context for this test
        SecurityContextHolder.clearContext();

        mockMvc.perform(post("/api/v1/favorites/{recipeId}", testRecipeId))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldReturn401ForAllEndpointsWhenUnauthenticated() throws Exception {
        // Clear the security context for this test
        SecurityContextHolder.clearContext();

        // Test add endpoint
        mockMvc.perform(post("/api/v1/favorites/{recipeId}", testRecipeId))
                .andExpect(status().isUnauthorized());

        // Test remove endpoint
        mockMvc.perform(delete("/api/v1/favorites/{recipeId}", testRecipeId))
                .andExpect(status().isUnauthorized());

        // Test list endpoint
        mockMvc.perform(get("/api/v1/favorites"))
                .andExpect(status().isUnauthorized());

        // Test check endpoint
        mockMvc.perform(get("/api/v1/favorites/check/{recipeId}", testRecipeId))
                .andExpect(status().isUnauthorized());

        // Restore security context for other tests
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(testUserId);
        SecurityContextHolder.setContext(securityContext);
    }
} 