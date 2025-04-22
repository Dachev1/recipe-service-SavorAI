package dev.idachev.recipeservice.service;

import dev.idachev.recipeservice.exception.ResourceNotFoundException;
import dev.idachev.recipeservice.model.FavoriteRecipe;
import dev.idachev.recipeservice.model.Recipe;
import dev.idachev.recipeservice.repository.FavoriteRecipeRepository;
import dev.idachev.recipeservice.repository.RecipeRepository;
import dev.idachev.recipeservice.web.dto.FavoriteRecipeDto;
import dev.idachev.recipeservice.web.dto.RecipeResponse;
import dev.idachev.recipeservice.web.mapper.FavoriteRecipeMapper;
import dev.idachev.recipeservice.web.mapper.RecipeMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FavoriteRecipeServiceUTest {

    @Mock
    private FavoriteRecipeRepository favoriteRecipeRepository;
    @Mock
    private RecipeRepository recipeRepository;
    @Mock
    private RecipeMapper recipeMapper;

    @InjectMocks
    private FavoriteRecipeService favoriteRecipeService;

    @Captor
    private ArgumentCaptor<FavoriteRecipe> favoriteRecipeCaptor;

    private UUID testUserId;
    private UUID testRecipeId;
    private Recipe testRecipe;
    private FavoriteRecipe testFavorite;
    private FavoriteRecipeDto testFavoriteDto;
    private RecipeResponse testRecipeResponse;

    @BeforeEach
    void setUp() {
        testUserId = UUID.randomUUID();
        testRecipeId = UUID.randomUUID();

        testRecipe = Recipe.builder()
                .id(testRecipeId)
                .userId(UUID.randomUUID())
                .title("Favorite Test Recipe")
                .createdAt(LocalDateTime.now().minusDays(1))
                .updatedAt(LocalDateTime.now())
                .build();

        testRecipeResponse = new RecipeResponse(
            testRecipeId, testRecipe.getUserId(), testRecipe.getTitle(), null, null, null,
            null, 0, null, null, null, null, false, null, null, null, 0, 0, null,
            testRecipe.getCreatedAt(), testRecipe.getUpdatedAt(), null, null
        );

        testFavorite = FavoriteRecipe.builder()
                .id(UUID.randomUUID())
                .userId(testUserId)
                .recipeId(testRecipeId)
                .createdAt(LocalDateTime.now())
                .build();

        testFavoriteDto = new FavoriteRecipeDto(
                testRecipeId,
                testUserId,
                testFavorite.getCreatedAt(),
                testRecipeResponse
        );

        lenient().when(recipeMapper.toResponse(any(Recipe.class))).thenReturn(testRecipeResponse);
    }

    @Nested
    @DisplayName("addToFavorites Tests")
    class AddToFavoritesTests {

        @Test
        @DisplayName("Should add new favorite successfully")
        void addToFavorites_NewFavorite_Success() {
            when(favoriteRecipeRepository.existsByUserIdAndRecipeId(testUserId, testRecipeId)).thenReturn(false);
            when(recipeRepository.findById(testRecipeId)).thenReturn(Optional.of(testRecipe));
            FavoriteRecipe savedFavorite = FavoriteRecipe.builder()
                .id(testFavorite.getId())
                .userId(testUserId)
                .recipeId(testRecipeId)
                .createdAt(testFavorite.getCreatedAt())
                .build();
            when(favoriteRecipeRepository.save(favoriteRecipeCaptor.capture())).thenReturn(savedFavorite);

            FavoriteRecipeDto actualDto = favoriteRecipeService.addToFavorites(testUserId, testRecipeId);

            assertThat(actualDto).isNotNull();
            assertThat(actualDto.recipeId()).isEqualTo(testFavoriteDto.recipeId());
            assertThat(actualDto.userId()).isEqualTo(testFavoriteDto.userId());
            assertThat(actualDto.recipe()).isEqualTo(testFavoriteDto.recipe());
            assertThat(actualDto.addedAt()).isEqualTo(testFavoriteDto.addedAt());

            verify(favoriteRecipeRepository).existsByUserIdAndRecipeId(testUserId, testRecipeId);
            verify(recipeRepository).findById(testRecipeId);
            verify(favoriteRecipeRepository).save(any(FavoriteRecipe.class));
            verify(recipeMapper).toResponse(testRecipe);

            FavoriteRecipe captured = favoriteRecipeCaptor.getValue();
            assertThat(captured.getId()).isNull();
            assertThat(captured.getUserId()).isEqualTo(testUserId);
            assertThat(captured.getRecipeId()).isEqualTo(testRecipeId);
        }

        @Test
        @DisplayName("Should return existing favorite DTO if already favorited")
        void addToFavorites_AlreadyExists_ReturnsExisting() {
            when(favoriteRecipeRepository.existsByUserIdAndRecipeId(testUserId, testRecipeId)).thenReturn(true);
            when(favoriteRecipeRepository.findByUserIdAndRecipeId(testUserId, testRecipeId)).thenReturn(Optional.of(testFavorite));
            when(recipeRepository.findById(testRecipeId)).thenReturn(Optional.of(testRecipe));

            FavoriteRecipeDto actualDto = favoriteRecipeService.addToFavorites(testUserId, testRecipeId);

            assertThat(actualDto).isNotNull();
            assertThat(actualDto.recipeId()).isEqualTo(testFavoriteDto.recipeId());
            assertThat(actualDto.userId()).isEqualTo(testFavoriteDto.userId());
            assertThat(actualDto.recipe()).isEqualTo(testFavoriteDto.recipe());
            assertThat(actualDto.addedAt()).isEqualTo(testFavoriteDto.addedAt());

            verify(favoriteRecipeRepository).existsByUserIdAndRecipeId(testUserId, testRecipeId);
            verify(favoriteRecipeRepository).findByUserIdAndRecipeId(testUserId, testRecipeId);
            verify(recipeRepository).findById(testRecipeId);
            verify(recipeMapper).toResponse(testRecipe);
            verify(favoriteRecipeRepository, never()).save(any(FavoriteRecipe.class));
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException if recipe not found")
        void addToFavorites_RecipeNotFound_ThrowsException() {
            when(favoriteRecipeRepository.existsByUserIdAndRecipeId(testUserId, testRecipeId)).thenReturn(false);
            when(recipeRepository.findById(testRecipeId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> favoriteRecipeService.addToFavorites(testUserId, testRecipeId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Recipe not found with id: " + testRecipeId);

            verify(favoriteRecipeRepository).existsByUserIdAndRecipeId(testUserId, testRecipeId);
            verify(recipeRepository).findById(testRecipeId);
            verify(favoriteRecipeRepository, never()).save(any(FavoriteRecipe.class));
            verify(favoriteRecipeRepository, never()).findByUserIdAndRecipeId(any(), any());
            verify(recipeMapper, never()).toResponse(any());
        }
    }

    @Nested
    @DisplayName("removeFromFavorites Tests")
    class RemoveFromFavoritesTests {
        @Test
        @DisplayName("Should remove favorite successfully")
        void removeFromFavorites_Success() {
            when(favoriteRecipeRepository.findByUserIdAndRecipeId(testUserId, testRecipeId))
                .thenReturn(Optional.of(testFavorite));
            doNothing().when(favoriteRecipeRepository).delete(testFavorite);

            favoriteRecipeService.removeFromFavorites(testUserId, testRecipeId);

            verify(favoriteRecipeRepository).findByUserIdAndRecipeId(testUserId, testRecipeId);
            verify(favoriteRecipeRepository).delete(testFavorite);
            verifyNoInteractions(recipeRepository, recipeMapper);
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException if favorite to remove not found")
        void removeFromFavorites_NotFound_ThrowsException() {
            when(favoriteRecipeRepository.findByUserIdAndRecipeId(testUserId, testRecipeId))
                .thenReturn(Optional.empty());

            assertThatThrownBy(() -> favoriteRecipeService.removeFromFavorites(testUserId, testRecipeId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Favorite not found for user " + testUserId + " and recipe " + testRecipeId);

            verify(favoriteRecipeRepository).findByUserIdAndRecipeId(testUserId, testRecipeId);
            verify(favoriteRecipeRepository, never()).delete(any(FavoriteRecipe.class));
            verifyNoInteractions(recipeRepository, recipeMapper);
        }
    }

    @Nested
    @DisplayName("getUserFavorites Tests")
    class GetUserFavoritesTests {
        @Test
        @DisplayName("Should return paginated user favorites")
        void getUserFavorites_Success() {
            Pageable pageable = PageRequest.of(0, 10);
            FavoriteRecipe fav1 = testFavorite;
            FavoriteRecipe fav2 = FavoriteRecipe.builder().id(UUID.randomUUID()).userId(testUserId).recipeId(UUID.randomUUID()).createdAt(LocalDateTime.now()).build();
            List<FavoriteRecipe> favList = List.of(fav1, fav2);
            Page<FavoriteRecipe> favPage = new PageImpl<>(favList, pageable, favList.size());

            Recipe recipe1 = testRecipe;
            Recipe recipe2 = Recipe.builder().id(fav2.getRecipeId()).title("Recipe 2").build();
            List<Recipe> recipeList = List.of(recipe1, recipe2);
            List<UUID> recipeIds = List.of(fav1.getRecipeId(), fav2.getRecipeId());

            RecipeResponse recipeResponse1 = testRecipeResponse;
            RecipeResponse recipeResponse2 = new RecipeResponse(
                 recipe2.getId(), recipe2.getUserId(), recipe2.getTitle(), null, null, null, null, 0, null, null, null, null, false, null, null, null, 0, 0, null, null, null, null, null
            );

            FavoriteRecipeDto expectedDto1 = testFavoriteDto;
            FavoriteRecipeDto expectedDto2 = new FavoriteRecipeDto(
                recipe2.getId(), testUserId, fav2.getCreatedAt(), recipeResponse2
            );
            List<FavoriteRecipeDto> expectedDtoList = List.of(expectedDto1, expectedDto2);
            Page<FavoriteRecipeDto> expectedPage = new PageImpl<>(expectedDtoList, pageable, favPage.getTotalElements());

            when(favoriteRecipeRepository.findByUserId(testUserId, pageable)).thenReturn(favPage);
            when(recipeRepository.findAllById(recipeIds)).thenReturn(recipeList);
            when(recipeMapper.toResponse(recipe1)).thenReturn(recipeResponse1);
            when(recipeMapper.toResponse(recipe2)).thenReturn(recipeResponse2);

            Page<FavoriteRecipeDto> actualPage = favoriteRecipeService.getUserFavorites(testUserId, pageable);

            assertThat(actualPage).isNotNull();
            assertThat(actualPage.getTotalElements()).isEqualTo(expectedPage.getTotalElements());
            assertThat(actualPage.getContent()).containsExactlyInAnyOrderElementsOf(expectedDtoList);

            verify(favoriteRecipeRepository).findByUserId(testUserId, pageable);
            verify(recipeRepository).findAllById(recipeIds);
            verify(recipeMapper).toResponse(recipe1);
            verify(recipeMapper).toResponse(recipe2);
        }

        @Test
        @DisplayName("Should return empty page if user has no favorites")
        void getUserFavorites_NoFavorites_ReturnsEmptyPage() {
            Pageable pageable = PageRequest.of(0, 10);
            Page<FavoriteRecipe> emptyFavPage = Page.empty(pageable);
            when(favoriteRecipeRepository.findByUserId(testUserId, pageable)).thenReturn(emptyFavPage);

            Page<FavoriteRecipeDto> actualPage = favoriteRecipeService.getUserFavorites(testUserId, pageable);

            assertThat(actualPage).isNotNull();
            assertThat(actualPage.getContent()).isEmpty();
            assertThat(actualPage.getTotalElements()).isZero();

            verify(favoriteRecipeRepository).findByUserId(testUserId, pageable);
            verify(recipeRepository, never()).findAllById(anyList());
            verify(recipeMapper, never()).toResponse(any());
        }
    }

    @Nested
    @DisplayName("getAllUserFavorites Tests")
    class GetAllUserFavoritesTests {
        @Test
        @DisplayName("Should return list of all user favorites")
        void getAllUserFavorites_Success() {
            FavoriteRecipe fav1 = testFavorite;
            FavoriteRecipe fav2 = FavoriteRecipe.builder().id(UUID.randomUUID()).userId(testUserId).recipeId(UUID.randomUUID()).createdAt(LocalDateTime.now()).build();
            List<FavoriteRecipe> favList = List.of(fav1, fav2);

            Recipe recipe1 = testRecipe;
            Recipe recipe2 = Recipe.builder().id(fav2.getRecipeId()).title("Recipe 2").build();
            List<Recipe> recipeList = List.of(recipe1, recipe2);
            List<UUID> recipeIds = List.of(fav1.getRecipeId(), fav2.getRecipeId());

            RecipeResponse recipeResponse1 = testRecipeResponse;
            RecipeResponse recipeResponse2 = new RecipeResponse(
                 recipe2.getId(), recipe2.getUserId(), recipe2.getTitle(), null, null, null, null, 0, null, null, null, null, false, null, null, null, 0, 0, null, null, null, null, null
            );

            FavoriteRecipeDto expectedDto1 = testFavoriteDto;
            FavoriteRecipeDto expectedDto2 = new FavoriteRecipeDto(
                recipe2.getId(), testUserId, fav2.getCreatedAt(), recipeResponse2
            );
            List<FavoriteRecipeDto> expectedDtoList = List.of(expectedDto1, expectedDto2);

            when(favoriteRecipeRepository.findByUserId(testUserId)).thenReturn(favList);
            when(recipeRepository.findAllById(recipeIds)).thenReturn(recipeList);
            when(recipeMapper.toResponse(recipe1)).thenReturn(recipeResponse1);
            when(recipeMapper.toResponse(recipe2)).thenReturn(recipeResponse2);

            List<FavoriteRecipeDto> actualList = favoriteRecipeService.getAllUserFavorites(testUserId);

            assertThat(actualList).isNotNull();
            assertThat(actualList).containsExactlyInAnyOrderElementsOf(expectedDtoList);

            verify(favoriteRecipeRepository).findByUserId(testUserId);
            verify(recipeRepository).findAllById(recipeIds);
            verify(recipeMapper).toResponse(recipe1);
            verify(recipeMapper).toResponse(recipe2);
        }

        @Test
        @DisplayName("Should return empty list if user has no favorites")
        void getAllUserFavorites_NoFavorites_ReturnsEmptyList() {
            when(favoriteRecipeRepository.findByUserId(testUserId)).thenReturn(Collections.emptyList());

            List<FavoriteRecipeDto> actualList = favoriteRecipeService.getAllUserFavorites(testUserId);

            assertThat(actualList).isNotNull().isEmpty();

            verify(favoriteRecipeRepository).findByUserId(testUserId);
            verify(recipeRepository, never()).findAllById(anyList());
            verify(recipeMapper, never()).toResponse(any());
        }
    }

    @Nested
    @DisplayName("isRecipeInFavorites Tests")
    class IsRecipeInFavoritesTests {
        @Test
        @DisplayName("Should return true if recipe is in favorites")
        void isRecipeInFavorites_True() {
            when(favoriteRecipeRepository.existsByUserIdAndRecipeId(testUserId, testRecipeId)).thenReturn(true);

            boolean result = favoriteRecipeService.isRecipeInFavorites(testUserId, testRecipeId);

            assertThat(result).isTrue();
            verify(favoriteRecipeRepository).existsByUserIdAndRecipeId(testUserId, testRecipeId);
            verifyNoInteractions(recipeRepository, recipeMapper);
        }

        @Test
        @DisplayName("Should return false if recipe is not in favorites")
        void isRecipeInFavorites_False() {
            when(favoriteRecipeRepository.existsByUserIdAndRecipeId(testUserId, testRecipeId)).thenReturn(false);

            boolean result = favoriteRecipeService.isRecipeInFavorites(testUserId, testRecipeId);

            assertThat(result).isFalse();
            verify(favoriteRecipeRepository).existsByUserIdAndRecipeId(testUserId, testRecipeId);
            verifyNoInteractions(recipeRepository, recipeMapper);
        }
    }

    @Nested
    @DisplayName("getFavoriteCount Tests")
    class GetFavoriteCountTests {
        @Test
        @DisplayName("Should return correct favorite count for a recipe")
        void getFavoriteCount_Success() {
            long expectedCount = 15L;
            when(favoriteRecipeRepository.countByRecipeId(testRecipeId)).thenReturn(expectedCount);

            long actualCount = favoriteRecipeService.getFavoriteCount(testRecipeId);

            assertThat(actualCount).isEqualTo(expectedCount);
            verify(favoriteRecipeRepository).countByRecipeId(testRecipeId);
            verifyNoInteractions(recipeRepository, recipeMapper);
        }
    }
} 