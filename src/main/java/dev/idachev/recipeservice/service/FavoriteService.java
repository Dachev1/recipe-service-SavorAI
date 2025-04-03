package dev.idachev.recipeservice.service;

import dev.idachev.recipeservice.model.FavoriteRecipe;
import dev.idachev.recipeservice.model.Recipe;
import dev.idachev.recipeservice.repository.FavoriteRecipeRepository;
import dev.idachev.recipeservice.repository.RecipeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FavoriteService {
    private final FavoriteRecipeRepository favoriteRepository;
    private final RecipeRepository recipeRepository;
    
    public List<Recipe> getFavoriteRecipes(UUID userId) {
        return favoriteRepository.findByUserId(userId).stream()
            .map(FavoriteRecipe::getRecipeId)
            .map(recipeRepository::findById)
            .filter(Optional::isPresent)
            .map(Optional::get)
            .collect(Collectors.toList());
    }
    
    @Transactional
    public boolean toggleFavorite(UUID userId, UUID recipeId) {
        boolean exists = favoriteRepository.existsByUserIdAndRecipeId(userId, recipeId);
        
        if (exists) {
            favoriteRepository.deleteByUserIdAndRecipeId(userId, recipeId);
            return false;
        }
        
        FavoriteRecipe favorite = FavoriteRecipe.builder()
            .userId(userId)
            .recipeId(recipeId)
            .createdAt(LocalDateTime.now())
            .build();
        favoriteRepository.save(favorite);
        return true;
    }
    
    public boolean isFavorite(UUID userId, UUID recipeId) {
        return favoriteRepository.existsByUserIdAndRecipeId(userId, recipeId);
    }
} 