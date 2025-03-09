package dev.idachev.recipeservice.web;

import dev.idachev.recipeservice.infrastructure.ai.AIService;
import dev.idachev.recipeservice.web.dto.GeneratedMealResponse;
import dev.idachev.recipeservice.web.dto.RecipeRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/ai")
@RequiredArgsConstructor
@Slf4j
public class AIRecipeController {

    private final AIService aiService;

    @PostMapping("/recipe/ingredients")
    public ResponseEntity<GeneratedMealResponse> generateFromIngredients(
            @RequestBody List<String> ingredients,
            @AuthenticationPrincipal Jwt jwt) {
        
        return ResponseEntity.ok(aiService.generateRecipeFromIngredients(ingredients));
    }

    @PostMapping("/recipe/preferences")
    public ResponseEntity<RecipeRequest> generateFromPreferences(
            @RequestParam List<String> preferences,
            @RequestParam List<String> restrictions,
            @AuthenticationPrincipal Jwt jwt) {
        
        return ResponseEntity.ok(aiService.generateRecipeFromPreferences(preferences, restrictions));
    }

    @PostMapping("/meal-plan")
    public ResponseEntity<List<GeneratedMealResponse>> generateMealPlan(
            @RequestParam int days,
            @RequestParam int mealsPerDay,
            @RequestParam List<String> preferences,
            @RequestParam List<String> restrictions,
            @AuthenticationPrincipal Jwt jwt) {
        
        return ResponseEntity.ok(aiService.generateMealPlan(preferences, restrictions, mealsPerDay, days));
    }

    @PostMapping("/image")
    public ResponseEntity<String> generateImage(
            @RequestParam String title,
            @RequestParam String description,
            @AuthenticationPrincipal Jwt jwt) {
        
        return ResponseEntity.ok(aiService.generateRecipeImage(title, description));
    }
} 