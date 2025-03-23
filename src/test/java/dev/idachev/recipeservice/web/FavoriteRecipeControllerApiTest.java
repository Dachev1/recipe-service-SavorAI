package dev.idachev.recipeservice.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.idachev.recipeservice.config.JwtAuthenticationFilter;
import dev.idachev.recipeservice.exception.ResourceNotFoundException;
import dev.idachev.recipeservice.service.FavoriteRecipeService;
import dev.idachev.recipeservice.user.service.UserService;
import dev.idachev.recipeservice.util.JwtUtil;
import dev.idachev.recipeservice.web.dto.FavoriteRecipeDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.util.AntPathMatcher;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(FavoriteRecipeController.class)
@AutoConfigureMockMvc(addFilters = false)
public class FavoriteRecipeControllerApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private FavoriteRecipeService favoriteRecipeService;

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
    @DisplayName("Given valid recipe ID and token, when adding to favorites, then return favorite recipe details")
    public void givenValidIdAndToken_whenAddToFavorites_thenReturnFavoriteRecipe() throws Exception {

        // Given
        FavoriteRecipeDto favoriteRecipeDto = createTestFavoriteRecipeDto();

        when(userService.getUserIdFromToken(VALID_TOKEN)).thenReturn(TEST_USER_ID);
        when(favoriteRecipeService.addToFavorites(TEST_USER_ID, TEST_RECIPE_ID)).thenReturn(favoriteRecipeDto);

        // When & Then
        mockMvc.perform(post("/api/v1/favorites/{recipeId}", TEST_RECIPE_ID).header(AUTHORIZATION_HEADER, VALID_TOKEN)).andExpect(status().isOk()).andExpect(jsonPath("$.recipeId").value(TEST_RECIPE_ID.toString())).andExpect(jsonPath("$.userId").value(TEST_USER_ID.toString()));

        verify(userService).getUserIdFromToken(VALID_TOKEN);
        verify(favoriteRecipeService).addToFavorites(TEST_USER_ID, TEST_RECIPE_ID);
    }

    @Test
    @DisplayName("Given non-existent recipe ID, when adding to favorites, then return 404 not found")
    public void givenNonExistentRecipeId_whenAddToFavorites_thenReturn404NotFound() throws Exception {

        // Given
        String errorMessage = "Recipe not found with id: " + TEST_RECIPE_ID;

        when(userService.getUserIdFromToken(VALID_TOKEN)).thenReturn(TEST_USER_ID);
        when(favoriteRecipeService.addToFavorites(TEST_USER_ID, TEST_RECIPE_ID)).thenThrow(new ResourceNotFoundException(errorMessage));

        // When & Then
        mockMvc.perform(post("/api/v1/favorites/{recipeId}", TEST_RECIPE_ID).header(AUTHORIZATION_HEADER, VALID_TOKEN)).andExpect(status().isNotFound()).andExpect(jsonPath("$.status").value(404)).andExpect(jsonPath("$.message").value(errorMessage));

        verify(userService).getUserIdFromToken(VALID_TOKEN);
        verify(favoriteRecipeService).addToFavorites(TEST_USER_ID, TEST_RECIPE_ID);
    }

    @Test
    @DisplayName("Given valid recipe ID and token, when removing from favorites, then return no content status")
    public void givenValidIdAndToken_whenRemoveFromFavorites_thenReturnNoContent() throws Exception {

        // Given
        when(userService.getUserIdFromToken(VALID_TOKEN)).thenReturn(TEST_USER_ID);

        // When & Then
        mockMvc.perform(delete("/api/v1/favorites/{recipeId}", TEST_RECIPE_ID).header(AUTHORIZATION_HEADER, VALID_TOKEN)).andExpect(status().isNoContent());

        verify(userService).getUserIdFromToken(VALID_TOKEN);
        verify(favoriteRecipeService).removeFromFavorites(TEST_USER_ID, TEST_RECIPE_ID);
    }

    @Test
    @DisplayName("Given recipe not in favorites, when removing from favorites, then return 404 not found")
    public void givenRecipeNotInFavorites_whenRemoveFromFavorites_thenReturn404NotFound() throws Exception {

        // Given
        String errorMessage = "Recipe not found in favorites: " + TEST_RECIPE_ID;

        when(userService.getUserIdFromToken(VALID_TOKEN)).thenReturn(TEST_USER_ID);
        doThrow(new ResourceNotFoundException(errorMessage)).when(favoriteRecipeService).removeFromFavorites(TEST_USER_ID, TEST_RECIPE_ID);

        // When & Then
        mockMvc.perform(delete("/api/v1/favorites/{recipeId}", TEST_RECIPE_ID).header(AUTHORIZATION_HEADER, VALID_TOKEN)).andExpect(status().isNotFound()).andExpect(jsonPath("$.status").value(404)).andExpect(jsonPath("$.message").value(errorMessage));

        verify(userService).getUserIdFromToken(VALID_TOKEN);
        verify(favoriteRecipeService).removeFromFavorites(TEST_USER_ID, TEST_RECIPE_ID);
    }

    @Test
    @DisplayName("Given valid token, when getting user favorites with pagination, then return paginated favorites")
    public void givenValidToken_whenGetUserFavorites_thenReturnPaginatedFavorites() throws Exception {

        // Given
        List<FavoriteRecipeDto> favorites = Arrays.asList(createTestFavoriteRecipeDto(), createTestFavoriteRecipeDto());
        Page<FavoriteRecipeDto> pageResponse = new PageImpl<>(favorites);

        when(userService.getUserIdFromToken(VALID_TOKEN)).thenReturn(TEST_USER_ID);
        when(favoriteRecipeService.getUserFavorites(eq(TEST_USER_ID), any(Pageable.class))).thenReturn(pageResponse);

        // When & Then
        mockMvc.perform(get("/api/v1/favorites").header(AUTHORIZATION_HEADER, VALID_TOKEN)).andExpect(status().isOk()).andExpect(jsonPath("$.content", hasSize(2))).andExpect(jsonPath("$.totalElements").value(2)).andExpect(jsonPath("$.content[0].userId").value(TEST_USER_ID.toString())).andExpect(jsonPath("$.content[0].recipeId").value(TEST_RECIPE_ID.toString()));

        verify(userService).getUserIdFromToken(VALID_TOKEN);
        verify(favoriteRecipeService).getUserFavorites(eq(TEST_USER_ID), any(Pageable.class));
    }

    @Test
    @DisplayName("Given valid token but no favorites, when getting user favorites, then return empty page")
    public void givenValidTokenButNoFavorites_whenGetUserFavorites_thenReturnEmptyPage() throws Exception {

        // Given
        Page<FavoriteRecipeDto> emptyPage = new PageImpl<>(Collections.emptyList());

        when(userService.getUserIdFromToken(VALID_TOKEN)).thenReturn(TEST_USER_ID);
        when(favoriteRecipeService.getUserFavorites(eq(TEST_USER_ID), any(Pageable.class))).thenReturn(emptyPage);

        // When & Then
        mockMvc.perform(get("/api/v1/favorites").header(AUTHORIZATION_HEADER, VALID_TOKEN)).andExpect(status().isOk()).andExpect(jsonPath("$.content", hasSize(0))).andExpect(jsonPath("$.totalElements").value(0));

        verify(userService).getUserIdFromToken(VALID_TOKEN);
        verify(favoriteRecipeService).getUserFavorites(eq(TEST_USER_ID), any(Pageable.class));
    }

    @Test
    @DisplayName("Given valid token, when getting all user favorites, then return list of favorites")
    public void givenValidToken_whenGetAllUserFavorites_thenReturnFavoritesList() throws Exception {

        // Given
        List<FavoriteRecipeDto> favorites = Arrays.asList(createTestFavoriteRecipeDto(), createTestFavoriteRecipeDto());

        when(userService.getUserIdFromToken(VALID_TOKEN)).thenReturn(TEST_USER_ID);
        when(favoriteRecipeService.getAllUserFavorites(TEST_USER_ID)).thenReturn(favorites);

        // When & Then
        mockMvc.perform(get("/api/v1/favorites/all").header(AUTHORIZATION_HEADER, VALID_TOKEN)).andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(2))).andExpect(jsonPath("$[0].userId").value(TEST_USER_ID.toString())).andExpect(jsonPath("$[1].userId").value(TEST_USER_ID.toString()));

        verify(userService).getUserIdFromToken(VALID_TOKEN);
        verify(favoriteRecipeService).getAllUserFavorites(TEST_USER_ID);
    }

    @Test
    @DisplayName("Given valid token but no favorites, when getting all user favorites, then return empty list")
    public void givenValidTokenButNoFavorites_whenGetAllUserFavorites_thenReturnEmptyList() throws Exception {

        // Given
        List<FavoriteRecipeDto> emptyList = Collections.emptyList();

        when(userService.getUserIdFromToken(VALID_TOKEN)).thenReturn(TEST_USER_ID);
        when(favoriteRecipeService.getAllUserFavorites(TEST_USER_ID)).thenReturn(emptyList);

        // When & Then
        mockMvc.perform(get("/api/v1/favorites/all").header(AUTHORIZATION_HEADER, VALID_TOKEN)).andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(0)));

        verify(userService).getUserIdFromToken(VALID_TOKEN);
        verify(favoriteRecipeService).getAllUserFavorites(TEST_USER_ID);
    }

    @Test
    @DisplayName("Given valid recipe ID and token, when checking if recipe is in favorites, then return boolean result")
    public void givenValidIdAndToken_whenCheckIsRecipeInFavorites_thenReturnBooleanResult() throws Exception {

        // Given
        boolean isInFavorites = true;

        when(userService.getUserIdFromToken(VALID_TOKEN)).thenReturn(TEST_USER_ID);
        when(favoriteRecipeService.isRecipeInFavorites(TEST_USER_ID, TEST_RECIPE_ID)).thenReturn(isInFavorites);

        // When & Then
        mockMvc.perform(get("/api/v1/favorites/check/{recipeId}", TEST_RECIPE_ID).header(AUTHORIZATION_HEADER, VALID_TOKEN)).andExpect(status().isOk()).andExpect(content().string("true"));

        verify(userService).getUserIdFromToken(VALID_TOKEN);
        verify(favoriteRecipeService).isRecipeInFavorites(TEST_USER_ID, TEST_RECIPE_ID);
    }

    @Test
    @DisplayName("Given valid recipe ID not in favorites, when checking if in favorites, then return false")
    public void givenValidIdNotInFavorites_whenCheckIsRecipeInFavorites_thenReturnFalse() throws Exception {

        // Given
        boolean isInFavorites = false;

        when(userService.getUserIdFromToken(VALID_TOKEN)).thenReturn(TEST_USER_ID);
        when(favoriteRecipeService.isRecipeInFavorites(TEST_USER_ID, TEST_RECIPE_ID)).thenReturn(isInFavorites);

        // When & Then
        mockMvc.perform(get("/api/v1/favorites/check/{recipeId}", TEST_RECIPE_ID).header(AUTHORIZATION_HEADER, VALID_TOKEN)).andExpect(status().isOk()).andExpect(content().string("false"));

        verify(userService).getUserIdFromToken(VALID_TOKEN);
        verify(favoriteRecipeService).isRecipeInFavorites(TEST_USER_ID, TEST_RECIPE_ID);
    }

    @Test
    @DisplayName("Given valid recipe ID, when getting favorite count, then return count value")
    public void givenValidId_whenGetFavoriteCount_thenReturnCount() throws Exception {

        // Given
        Long favoriteCount = 42L;

        when(favoriteRecipeService.getFavoriteCount(TEST_RECIPE_ID)).thenReturn(favoriteCount);

        // When & Then
        mockMvc.perform(get("/api/v1/favorites/count/{recipeId}", TEST_RECIPE_ID)).andExpect(status().isOk()).andExpect(content().string(favoriteCount.toString()));

        verify(favoriteRecipeService).getFavoriteCount(TEST_RECIPE_ID);
    }

    @Test
    @DisplayName("Given valid recipe ID with zero favorites, when getting favorite count, then return zero")
    public void givenValidIdWithZeroFavorites_whenGetFavoriteCount_thenReturnZero() throws Exception {

        // Given
        Long favoriteCount = 0L;

        when(favoriteRecipeService.getFavoriteCount(TEST_RECIPE_ID)).thenReturn(favoriteCount);

        // When & Then
        mockMvc.perform(get("/api/v1/favorites/count/{recipeId}", TEST_RECIPE_ID)).andExpect(status().isOk()).andExpect(content().string("0"));

        verify(favoriteRecipeService).getFavoriteCount(TEST_RECIPE_ID);
    }

    // Helper methods
    private FavoriteRecipeDto createTestFavoriteRecipeDto() {
        FavoriteRecipeDto dto = new FavoriteRecipeDto();
        dto.setUserId(TEST_USER_ID);
        dto.setRecipeId(TEST_RECIPE_ID);
        return dto;
    }
} 