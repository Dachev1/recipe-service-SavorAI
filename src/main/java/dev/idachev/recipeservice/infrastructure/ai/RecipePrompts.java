package dev.idachev.recipeservice.infrastructure.ai;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Centralized storage for AI prompts used in recipe generation.
 * This class provides static methods to get prompts for different recipe generation scenarios.
 */
public class RecipePrompts {

    /**
     * Get the system prompt for generating a recipe from ingredients.
     *
     * @return The system prompt text
     */
    public static String getRecipeFromIngredientsPrompt() {
        return """
            You are a professional chef and nutritionist. Create a detailed recipe using the provided ingredients.
            The response should be in JSON format with the following structure:
            {
                "title": "Recipe Title",
                "description": "Brief description of the dish",
                "ingredients": ["ingredient1", "ingredient2", ...],
                "instructions": "Step-by-step cooking instructions",
                "totalTimeMinutes": 45,
                "macros": {
                    "calories": 350,
                    "proteinGrams": 20.5,
                    "carbsGrams": 40.2,
                    "fatGrams": 15.3
                },
                "difficulty": "EASY",
                "servings": 4,
                "tags": ["tag1", "tag2", ...]
            }
            
            Be creative but realistic. Include all nutritional information and make sure the recipe is delicious and balanced.
            Only use the ingredients provided, plus common pantry items like salt, pepper, and basic spices.
            """;
    }
    
    /**
     * Get the user prompt for generating a recipe from ingredients.
     *
     * @param ingredients List of ingredients to use
     * @return The user prompt text
     */
    public static String getRecipeFromIngredientsUserPrompt(List<String> ingredients) {
        return "Create a recipe using these ingredients: " + String.join(", ", ingredients);
    }
    
    /**
     * Get the system prompt for generating a recipe from preferences and restrictions.
     *
     * @return The system prompt template text
     */
    public static String getRecipeFromPreferencesPrompt() {
        return """
            You are a professional chef and nutritionist. Create a detailed recipe that matches the dietary preferences 
            and avoids any dietary restrictions provided.
            
            Preferences: {preferences}
            Restrictions: {restrictions}
            
            The response should be in JSON format with the following structure:
            {
                "title": "Recipe Title",
                "description": "Brief description of the dish",
                "ingredients": ["ingredient1", "ingredient2", ...],
                "instructions": "Step-by-step cooking instructions",
                "totalTimeMinutes": 45,
                "macros": {
                    "calories": 350,
                    "proteinGrams": 20.5,
                    "carbsGrams": 40.2,
                    "fatGrams": 15.3
                },
                "difficulty": "EASY",
                "servings": 4,
                "tags": ["tag1", "tag2", ...]
            }
            
            Be creative but realistic. Include all nutritional information and make sure the recipe is delicious and balanced.
            """;
    }
    
    /**
     * Get the variables map for preferences and restrictions prompt.
     *
     * @param preferences List of dietary preferences
     * @param restrictions List of dietary restrictions
     * @return Map of variables for the prompt template
     */
    public static Map<String, Object> getPreferencesVariables(List<String> preferences, List<String> restrictions) {
        Map<String, Object> variables = new HashMap<>();
        variables.put("preferences", String.join(", ", preferences));
        variables.put("restrictions", String.join(", ", restrictions));
        return variables;
    }
    
    /**
     * Get the user prompt for generating a recipe from preferences and restrictions.
     *
     * @return The user prompt text
     */
    public static String getRecipeFromPreferencesUserPrompt() {
        return "Create a recipe that matches my preferences and restrictions";
    }
    
    /**
     * Get the system prompt for generating a meal plan.
     *
     * @return The system prompt template text
     */
    public static String getMealPlanPrompt() {
        return """
            You are a professional chef and nutritionist. Create a meal plan for {days} days with {mealsPerDay} meals per day 
            that matches the dietary preferences and avoids any dietary restrictions provided.
            
            Preferences: {preferences}
            Restrictions: {restrictions}
            
            The response should be a JSON array of recipes, each with the following structure:
            [
                {
                    "title": "Recipe Title",
                    "description": "Brief description of the dish",
                    "ingredients": ["ingredient1", "ingredient2", ...],
                    "instructions": "Step-by-step cooking instructions",
                    "totalTimeMinutes": 45,
                    "macros": {
                        "calories": 350,
                        "proteinGrams": 20.5,
                        "carbsGrams": 40.2,
                        "fatGrams": 15.3
                    },
                    "difficulty": "EASY",
                    "servings": 4,
                    "tags": ["tag1", "tag2", ...]
                },
                ...
            ]
            
            Be creative but realistic. Include all nutritional information and make sure the recipes are delicious and balanced.
            Ensure the meal plan is varied and nutritionally complete.
            """;
    }
    
    /**
     * Get the variables map for meal plan prompt.
     *
     * @param preferences List of dietary preferences
     * @param restrictions List of dietary restrictions
     * @param mealsPerDay Number of meals per day
     * @param days Number of days
     * @return Map of variables for the prompt template
     */
    public static Map<String, Object> getMealPlanVariables(List<String> preferences, List<String> restrictions, 
                                                          int mealsPerDay, int days) {
        Map<String, Object> variables = new HashMap<>();
        variables.put("preferences", String.join(", ", preferences));
        variables.put("restrictions", String.join(", ", restrictions));
        variables.put("mealsPerDay", mealsPerDay);
        variables.put("days", days);
        return variables;
    }
    
    /**
     * Get the user prompt for generating a meal plan.
     *
     * @return The user prompt text
     */
    public static String getMealPlanUserPrompt() {
        return "Create a meal plan that matches my preferences and restrictions";
    }
    
    /**
     * Get the prompt for generating a recipe image.
     *
     * @param recipeTitle The title of the recipe
     * @param recipeDescription The description of the recipe
     * @return The image generation prompt
     */
    public static String getRecipeImagePrompt(String recipeTitle, String recipeDescription) {
        return "A professional food photography image of " + recipeTitle + ". " + recipeDescription;
    }
} 