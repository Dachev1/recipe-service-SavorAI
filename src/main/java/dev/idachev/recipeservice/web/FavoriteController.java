package dev.idachev.recipeservice.web;

import dev.idachev.recipeservice.model.Recipe;
import dev.idachev.recipeservice.service.FavoriteService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/v1/favorites")
@RequiredArgsConstructor
public class FavoriteController {
    private final FavoriteService favoriteService;
    
    @GetMapping
    public List<Recipe> getFavorites(@AuthenticationPrincipal Jwt jwt) {
        UUID userId = UUID.fromString(jwt.getSubject());
        return favoriteService.getFavoriteRecipes(userId);
    }
    
    @PostMapping("/{recipeId}")
    public Map<String, Boolean> toggleFavorite(
        @AuthenticationPrincipal Jwt jwt,
        @PathVariable String recipeId
    ) {
        UUID userId = UUID.fromString(jwt.getSubject());
        UUID recipeUuid = UUID.fromString(recipeId);
        boolean isFavorite = favoriteService.toggleFavorite(userId, recipeUuid);
        return Map.of("isFavorite", isFavorite);
    }
    
    @GetMapping("/check/{recipeId}")
    public Map<String, Boolean> checkFavorite(
        @AuthenticationPrincipal Jwt jwt,
        @PathVariable String recipeId
    ) {
        UUID userId = UUID.fromString(jwt.getSubject());
        UUID recipeUuid = UUID.fromString(recipeId);
        return Map.of("isFavorite", favoriteService.isFavorite(userId, recipeUuid));
    }
} 