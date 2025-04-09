package dev.idachev.recipeservice.infrastructure.ai;

/**
 * Centralized storage for AI prompts used in recipe generation.
 * This utility class contains static methods only and should not be instantiated.
 */
public final class RecipePrompts {

    /**
     * Private constructor to prevent instantiation
     */
    private RecipePrompts() {
        // Utility class, no instantiation
    }

    /**
     * System prompt for generating a recipe from ingredients
     */
    public static String getRecipeFromIngredientsPrompt() {
        return """
                You are an expert chef and nutritionist specializing in creating delicious, practical recipes.
                
                Your task is to create ONE detailed recipe using the ingredients provided by the user. Be creative but realistic.
                Include common pantry staples (salt, pepper, oil, basic spices) even if not explicitly listed.
                
                IMPORTANT: Only reject obvious non-food items (cars, electronics, furniture). Accept all common food ingredients.
                If non-food items are included, return a JSON error: {"error":"Cannot create recipe with non-food items","nonFoodItems":["item1","item2"]}
                
                FORMAT YOUR RESPONSE AS VALID JSON with this exact structure:
                {
                    "title": "Recipe Title",
                    "servingSuggestions": "Serving details, garnishes, and pairings",
                    "ingredients": ["Ingredient 1 with quantity", "Ingredient 2 with quantity", ...],
                    "instructions": "Detailed, step-by-step cooking instructions with numbered steps",
                    "totalTimeMinutes": number (prep + cooking time),
                    "macros": {
                        "calories": number,
                        "proteinGrams": number,
                        "carbsGrams": number,
                        "fatGrams": number
                    },
                    "difficulty": "EASY", "MEDIUM", or "HARD"
                }
                
                IMPORTANT GUIDELINES:
                1. Make the dish realistic and achievable for a home cook
                2. Be precise with measurements and quantities
                3. Estimate nutrition information realistically
                4. Create visually appealing dishes that would photograph well
                5. Instructions should be clear and easy to follow
                6. Always create different recipes, never repeat previous ones
                7. Provide specific serving suggestions with garnishes and pairings
                8. Never reject actual food items
                
                EXTREMELY IMPORTANT: Return ONLY raw JSON. No markdown blocks or explanations.
                """;
    }

    /**
     * Prompt for generating a recipe image
     */
    public static String getRecipeImagePrompt(String recipeTitle, String servingSuggestions) {
        StringBuilder promptBuilder = new StringBuilder(256)
            .append("Professional food photography of ")
            .append(recipeTitle)
            .append(". Overhead shot on a rustic wooden table with beautiful natural lighting. ")
            .append("The dish looks absolutely delicious with vibrant colors and perfect presentation. ");
        
        if (servingSuggestions != null && !servingSuggestions.isEmpty()) {
            promptBuilder.append("Serving suggestions: ").append(servingSuggestions);
        }
        
        return promptBuilder.toString();
    }

    /**
     * Prompt template for unique recipe generation from ingredients
     */
    public static String getUniqueRecipePrompt(String ingredients, String uniqueId) {
        return String.format(
                "I need a creative and unique recipe using these ingredients: %s. " +
                "Only reject obviously non-food items like cars, electronics, etc. " +
                "Accept all normal food ingredients. " +
                "Be creative with cuisine style and cooking method. " +
                "Make it unique with ID: %s",
                ingredients, uniqueId
        );
    }
} 