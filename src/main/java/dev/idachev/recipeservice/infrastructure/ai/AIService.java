package dev.idachev.recipeservice.infrastructure.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.idachev.recipeservice.exception.AIServiceException;
import dev.idachev.recipeservice.infrastructure.storage.CloudinaryService;
import dev.idachev.recipeservice.mapper.AIServiceMapper;
import dev.idachev.recipeservice.model.DifficultyLevel;
import dev.idachev.recipeservice.web.dto.AIErrorResponse;
import dev.idachev.recipeservice.web.dto.MacrosDto;
import dev.idachev.recipeservice.web.dto.RecipeRequest;
import dev.idachev.recipeservice.web.dto.SimplifiedRecipeResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.ChatClient;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.image.ImageClient;
import org.springframework.ai.image.ImagePrompt;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * AI recipe generation service using Spring AI's OpenAI integration
 */
@Service
@Slf4j
public class AIService {

    private static final int UUID_LENGTH = 8;

    private final ChatClient chatClient;
    private final ImageClient imageClient;
    private final ObjectMapper objectMapper;
    private final CloudinaryService cloudinaryService;

    @Value("${ai.service.max-ingredients:20}")
    private int maxIngredients;

    @Value("${ai.service.image-generation.enabled:true}")
    private boolean imageGenerationEnabled;

    @Autowired
    public AIService(ChatClient chatClient, ImageClient imageClient,
                     ObjectMapper objectMapper, CloudinaryService cloudinaryService) {
        this.chatClient = chatClient;
        this.imageClient = imageClient;
        this.objectMapper = objectMapper;
        this.cloudinaryService = cloudinaryService;
    }

    /**
     * Generate a unique recipe from ingredients
     *
     * @param ingredients List of ingredients to use
     * @return Recipe with details and image URL
     */
    @Cacheable(value = "recipes", key = "#ingredients.toString().hashCode()", unless = "#result == null")
    public SimplifiedRecipeResponse generateRecipeFromIngredients(List<String> ingredients) {
        // Validate and sanitize ingredients
        List<String> validIngredients = validateIngredients(ingredients);

        log.info("Generating recipe from {} ingredients", validIngredients.size());

        try {
            // Generate the recipe first
            RecipeRequest recipeRequest = generateRecipeRequestFromAI(validIngredients);

            // Set default image URL to null
            String imageUrl = null;

            // Generate image only if enabled
            if (imageGenerationEnabled && StringUtils.hasText(recipeRequest.getTitle())) {
                // Try to generate image, but handle failure gracefully
                try {
                    imageUrl = generateRecipeImage(recipeRequest.getTitle(), recipeRequest.getServingSuggestions());
                } catch (Exception e) {
                    log.warn("Failed to generate image for recipe {}: {}", recipeRequest.getTitle(), e.getMessage());
                    // Continue without image
                }
            }

            SimplifiedRecipeResponse result = AIServiceMapper.toSimplifiedResponse(recipeRequest, imageUrl);
            log.info("Generated recipe: {}", result.getTitle());
            return result;
        } catch (Exception e) {
            log.error("Error generating recipe: {}", e.getMessage());
            throw new AIServiceException("Failed to generate recipe", e);
        }
    }

    /**
     * Validate and sanitize ingredient list
     */
    private List<String> validateIngredients(List<String> ingredients) {
        if (CollectionUtils.isEmpty(ingredients)) {
            log.warn("Empty ingredients list provided");
            return List.of();
        }

        // Filter out empty ingredients and trim others
        List<String> validIngredients = ingredients.stream()
                .filter(StringUtils::hasText)
                .map(String::trim)
                .distinct()
                .collect(Collectors.toList());

        if (validIngredients.size() > maxIngredients) {
            log.warn("Too many ingredients ({}). Limiting to first {}", validIngredients.size(), maxIngredients);
            return validIngredients.subList(0, maxIngredients);
        }

        return validIngredients;
    }

    /**
     * Generate recipe using AI
     */
    private RecipeRequest generateRecipeRequestFromAI(List<String> ingredients) {
        log.info("Attempting to generate recipe from OpenAI with ingredients: {}", ingredients);

        Message systemMessage = new SystemMessage(RecipePrompts.getRecipeFromIngredientsPrompt());
        String uniquePrompt = createUniquePrompt(ingredients);
        Message userMessage = new UserMessage(uniquePrompt);

        String content = chatClient.call(new Prompt(List.of(systemMessage, userMessage)))
                .getResult().getOutput().getContent();

        if (!StringUtils.hasText(content)) {
            throw new AIServiceException("AI returned empty response", null);
        }

        // Clean up the content to handle markdown code blocks
        content = cleanupJsonResponse(content);
        
        try {
            // First check if the response contains an error field (non-food items)
            if (content.contains("\"error\"")) {
                try {
                    AIErrorResponse errorResponse = objectMapper.readValue(content, AIErrorResponse.class);
                    String errorMsg = errorResponse.getError();
                    if (errorResponse.getNonFoodItems() != null && !errorResponse.getNonFoodItems().isEmpty()) {
                        errorMsg += ": " + String.join(", ", errorResponse.getNonFoodItems());
                    }
                    throw new AIServiceException(errorMsg, null);
                } catch (Exception e) {
                    if (e instanceof AIServiceException) throw e;
                    // If parsing fails, continue with normal processing
                }
            }
            
            // Try to parse as a recipe
            RecipeRequest recipeRequest = objectMapper.readValue(content, RecipeRequest.class);
            
            // Normalize and validate required fields are present with reasonable values
            normalizeRecipeFields(recipeRequest);
            
            return recipeRequest;
        } catch (AIServiceException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error parsing AI response: {}", e.getMessage());
            throw new AIServiceException("Failed to parse AI response", e);
        }
    }

    /**
     * Cleans up JSON response from AI by removing any markdown formatting
     * 
     * @param content Raw content from AI
     * @return Cleaned JSON string
     */
    private String cleanupJsonResponse(String content) {
        if (content == null) {
            return null;
        }
        
        // Remove markdown code fences if present
        String cleaned = content.trim();
        
        // Remove leading ```json or ``` and trailing ```
        if (cleaned.startsWith("```json")) {
            cleaned = cleaned.substring("```json".length());
        } else if (cleaned.startsWith("```")) {
            cleaned = cleaned.substring("```".length());
        }
        
        if (cleaned.endsWith("```")) {
            cleaned = cleaned.substring(0, cleaned.length() - "```".length());
        }
        
        // Remove any leading/trailing whitespace that might appear after removing code fences
        cleaned = cleaned.trim();
        
        // Log if cleanup was necessary
        if (!cleaned.equals(content)) {
            log.debug("Cleaned up markdown formatting from AI response");
        }
        
        return cleaned;
    }

    /**
     * Create unique prompt for recipe generation
     */
    private String createUniquePrompt(List<String> ingredients) {
        String uniqueId = UUID.randomUUID().toString().substring(0, UUID_LENGTH);
        String joinedIngredients = CollectionUtils.isEmpty(ingredients) ? "" : String.join(", ", ingredients);
        return String.format(
                "I need a creative and unique recipe using these ingredients: %s. " +
                "Only reject obviously non-food items like cars, electronics, etc. " +
                "Accept all normal food ingredients. " +
                "Be creative with cuisine style and cooking method. " +
                "IMPORTANT: You must include difficulty level (EASY, MEDIUM, or HARD) and totalTimeMinutes (total cooking time). " +
                "Make it unique with ID: %s",
                joinedIngredients, uniqueId
        );
    }

    /**
     * Generate recipe image asynchronously
     */
    @Async
    public CompletableFuture<String> generateRecipeImageAsync(String recipeTitle, String recipeServingSuggestions) {
        return CompletableFuture.supplyAsync(() -> generateRecipeImage(recipeTitle, recipeServingSuggestions));
    }

    /**
     * Generate recipe image and store in Cloudinary
     * Designed to fail gracefully and return null rather than throw exceptions
     */
    public String generateRecipeImage(String recipeTitle, String recipeServingSuggestions) {
        if (!StringUtils.hasText(recipeTitle)) {
            log.warn("Recipe title empty, cannot generate image");
            return null;
        }

        try {
            String promptText = RecipePrompts.getRecipeImagePrompt(recipeTitle, recipeServingSuggestions);
            log.debug("Generating image for recipe: {}", recipeTitle);
            
            String imageUrl = imageClient.call(new ImagePrompt(promptText))
                    .getResult().getOutput().getUrl();

            if (!StringUtils.hasText(imageUrl)) {
                log.warn("AI returned empty image URL for recipe: {}", recipeTitle);
                return null;
            }

            log.debug("Image generated successfully, uploading to Cloudinary");
            return uploadToCloudinary(imageUrl, recipeTitle);
        } catch (Exception e) {
            log.error("Error generating recipe image: {}", e.getMessage());
            return null;
        }
    }

    private String uploadToCloudinary(String imageUrl, String recipeTitle) {
        if (!StringUtils.hasText(imageUrl)) {
            return null;
        }

        try {
            String cloudinaryUrl = cloudinaryService.uploadImageFromUrl(imageUrl);
            if (cloudinaryUrl == null) {
                log.warn("Cloudinary returned null URL, falling back to original URL");
                return imageUrl;
            }
            return cloudinaryUrl;
        } catch (Exception e) {
            log.error("Error uploading to Cloudinary for recipe {}: {}", recipeTitle, e.getMessage());
            // Fallback to original URL instead of returning null
            return imageUrl;
        }
    }

    /**
     * Ensures all required fields in the recipe have valid values, providing defaults when necessary.
     * Particularly focuses on difficulty level and totalTimeMinutes which are critical for the UI.
     * 
     * @param recipe The recipe request object to normalize
     */
    private void normalizeRecipeFields(RecipeRequest recipe) {
        // Ensure difficulty level is set
        if (recipe.getDifficulty() == null) {
            log.debug("Recipe missing difficulty level, setting default MEDIUM");
            recipe.setDifficulty(DifficultyLevel.MEDIUM);
        }
        
        // Ensure totalTimeMinutes is set with a reasonable value based on recipe complexity
        if (recipe.getTotalTimeMinutes() == null || recipe.getTotalTimeMinutes() <= 0) {
            int defaultTime;
            
            // Base default time on recipe complexity (ingredients count, difficulty)
            if (recipe.getIngredients() != null) {
                int ingredientCount = recipe.getIngredients().size();
                
                if (DifficultyLevel.EASY.equals(recipe.getDifficulty())) {
                    defaultTime = Math.max(15, ingredientCount * 2);
                } else if (DifficultyLevel.HARD.equals(recipe.getDifficulty())) {
                    defaultTime = Math.max(45, ingredientCount * 5);
                } else {
                    // MEDIUM difficulty
                    defaultTime = Math.max(30, ingredientCount * 3);
                }
            } else {
                // If no ingredients, use fixed defaults based on difficulty
                if (DifficultyLevel.EASY.equals(recipe.getDifficulty())) {
                    defaultTime = 20;
                } else if (DifficultyLevel.HARD.equals(recipe.getDifficulty())) {
                    defaultTime = 60;
                } else {
                    defaultTime = 40;
                }
            }
            
            log.debug("Recipe missing totalTimeMinutes, setting default {} based on difficulty and ingredients", defaultTime);
            recipe.setTotalTimeMinutes(defaultTime);
        }
        
        // Ensure macros object exists
        if (recipe.getMacros() == null) {
            MacrosDto macros = new MacrosDto();
            macros.setCalories(0);
            macros.setProteinGrams(0.0);
            macros.setCarbsGrams(0.0);
            macros.setFatGrams(0.0);
            recipe.setMacros(macros);
            log.debug("Recipe missing macros, initializing empty macros object");
        }
    }
} 