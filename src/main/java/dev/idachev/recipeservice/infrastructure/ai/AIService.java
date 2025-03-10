package dev.idachev.recipeservice.infrastructure.ai;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.idachev.recipeservice.exception.AIServiceException;
import dev.idachev.recipeservice.infrastructure.storage.CloudinaryService;
import dev.idachev.recipeservice.web.dto.GeneratedMealResponse;
import dev.idachev.recipeservice.web.dto.RecipeRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.ChatClient;
import org.springframework.ai.chat.ChatResponse;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.SystemPromptTemplate;
import org.springframework.ai.image.ImageClient;
import org.springframework.ai.image.ImagePrompt;
import org.springframework.ai.image.ImageResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Service for AI-powered recipe generation.
 * This service handles all AI interactions for recipe generation and image creation.
 */
@Service
@Slf4j
@Validated
public class AIService {

    private final ChatClient chatClient;
    private final ImageClient imageClient;
    private final ObjectMapper objectMapper;
    private final CloudinaryService cloudinaryService;

    @Value("${spring.ai.image.options.model:dall-e-3}")
    private String imageModel;

    @Value("${spring.ai.image.options.quality:standard}")
    private String imageQuality;

    @Value("${spring.ai.image.options.width:1024}")
    private int imageWidth;

    @Value("${spring.ai.image.options.height:1024}")
    private int imageHeight;

    @Autowired
    public AIService(ChatClient chatClient, ImageClient imageClient,
                     ObjectMapper objectMapper, CloudinaryService cloudinaryService) {
        this.chatClient = chatClient;
        this.imageClient = imageClient;
        this.objectMapper = objectMapper;
        this.cloudinaryService = cloudinaryService;
    }

    /**
     * Generate a recipe from a list of ingredients.
     *
     * @param ingredients List of ingredients to use in the recipe
     * @return Generated recipe response
     * @throws AIServiceException if there's an error with the AI service
     */
    @Cacheable(value = "recipeCache", key = "#ingredients.toString()")
    public GeneratedMealResponse generateRecipeFromIngredients(
            @NotEmpty(message = "Ingredients list cannot be empty")
            @Size(min = 1, max = 20, message = "Number of ingredients must be between 1 and 20")
            List<String> ingredients) {

        log.info("Generating recipe from ingredients: {}", ingredients);

        try {
            // Get prompts from the RecipePrompts class
            String systemPrompt = RecipePrompts.getRecipeFromIngredientsPrompt();
            String userPrompt = RecipePrompts.getRecipeFromIngredientsUserPrompt(ingredients);

            Message systemMessage = new SystemPromptTemplate(systemPrompt).createMessage();
            Message userMessage = new UserMessage(userPrompt);

            Prompt prompt = new Prompt(List.of(systemMessage, userMessage));
            ChatResponse response = chatClient.call(prompt);

            String content = response.getResult().getOutput().getContent();
            RecipeRequest recipeRequest = objectMapper.readValue(content, RecipeRequest.class);

            // Generate an image for the recipe
            String imageUrl = generateRecipeImage(recipeRequest.getTitle(), recipeRequest.getDescription());

            return GeneratedMealResponse.builder()
                    .title(recipeRequest.getTitle())
                    .description(recipeRequest.getDescription())
                    .instructions(recipeRequest.getInstructions())
                    .ingredients(recipeRequest.getIngredients())
                    .totalTimeMinutes(recipeRequest.getTotalTimeMinutes())
                    .macros(recipeRequest.getMacros())
                    .difficulty(recipeRequest.getDifficulty())
                    .recipe(recipeRequest)
                    .imageUrl(imageUrl)
                    .build();

        } catch (JsonProcessingException e) {
            log.error("Error parsing AI response: {}", e.getMessage());
            throw new AIServiceException("Failed to parse AI-generated recipe", e);
        } catch (Exception e) {
            log.error("Error generating recipe: {}", e.getMessage());
            throw new AIServiceException("Failed to generate recipe", e);
        }
    }

    /**
     * Generate a recipe from user preferences and restrictions.
     *
     * @param preferences  List of dietary preferences
     * @param restrictions List of dietary restrictions
     * @return Generated recipe
     * @throws AIServiceException if there's an error with the AI service
     */
    @Cacheable(value = "recipeCache", key = "'pref:' + #preferences.toString() + '-restr:' + #restrictions.toString()")
    public RecipeRequest generateRecipeFromPreferences(
            @NotEmpty(message = "Preferences list cannot be empty") List<String> preferences,
            @NotEmpty(message = "Restrictions list cannot be empty") List<String> restrictions) {

        log.info("Generating recipe from preferences: {} and restrictions: {}", preferences, restrictions);

        try {
            // Get prompts and variables from the RecipePrompts class
            String systemPrompt = RecipePrompts.getRecipeFromPreferencesPrompt();
            String userPrompt = RecipePrompts.getRecipeFromPreferencesUserPrompt();
            Map<String, Object> variables = RecipePrompts.getPreferencesVariables(preferences, restrictions);

            Message systemMessage = new SystemPromptTemplate(systemPrompt).createMessage(variables);
            Message userMessage = new UserMessage(userPrompt);

            Prompt prompt = new Prompt(List.of(systemMessage, userMessage));
            ChatResponse response = chatClient.call(prompt);

            String content = response.getResult().getOutput().getContent();
            return objectMapper.readValue(content, RecipeRequest.class);

        } catch (JsonProcessingException e) {
            log.error("Error parsing AI response: {}", e.getMessage());
            throw new AIServiceException("Failed to parse AI-generated recipe", e);
        } catch (Exception e) {
            log.error("Error generating recipe: {}", e.getMessage());
            throw new AIServiceException("Failed to generate recipe", e);
        }
    }

    /**
     * Generate a meal plan based on preferences and restrictions.
     *
     * @param preferences  List of dietary preferences
     * @param restrictions List of dietary restrictions
     * @param mealsPerDay  Number of meals per day
     * @param days         Number of days
     * @return List of generated meals
     * @throws AIServiceException if there's an error with the AI service
     */
    @Cacheable(value = "mealPlanCache",
            key = "'pref:' + #preferences.toString() + '-restr:' + #restrictions.toString() + '-meals:' + #mealsPerDay + '-days:' + #days")
    public List<GeneratedMealResponse> generateMealPlan(
            @NotEmpty(message = "Preferences list cannot be empty") List<String> preferences,
            @NotEmpty(message = "Restrictions list cannot be empty") List<String> restrictions,
            @Valid @Size(min = 1, max = 6, message = "Meals per day must be between 1 and 6") int mealsPerDay,
            @Valid @Size(min = 1, max = 30, message = "Days must be between 1 and 30") int days) {

        log.info("Generating meal plan with preferences: {}, restrictions: {}, meals per day: {}, days: {}",
                preferences, restrictions, mealsPerDay, days);

        try {
            // Get prompts and variables from the RecipePrompts class
            String systemPrompt = RecipePrompts.getMealPlanPrompt();
            String userPrompt = RecipePrompts.getMealPlanUserPrompt();
            Map<String, Object> variables = RecipePrompts.getMealPlanVariables(preferences, restrictions, mealsPerDay, days);

            Message systemMessage = new SystemPromptTemplate(systemPrompt).createMessage(variables);
            Message userMessage = new UserMessage(userPrompt);

            Prompt prompt = new Prompt(List.of(systemMessage, userMessage));
            ChatResponse response = chatClient.call(prompt);

            String content = response.getResult().getOutput().getContent();
            RecipeRequest[] recipes = objectMapper.readValue(content, RecipeRequest[].class);
            List<GeneratedMealResponse> mealResponses = new ArrayList<>();

            for (RecipeRequest recipe : recipes) {
                String imageUrl = generateRecipeImage(recipe.getTitle(), recipe.getDescription());

                GeneratedMealResponse mealResponse = GeneratedMealResponse.builder()
                        .title(recipe.getTitle())
                        .description(recipe.getDescription())
                        .instructions(recipe.getInstructions())
                        .ingredients(recipe.getIngredients())
                        .totalTimeMinutes(recipe.getTotalTimeMinutes())
                        .macros(recipe.getMacros())
                        .difficulty(recipe.getDifficulty())
                        .recipe(recipe)
                        .imageUrl(imageUrl)
                        .build();
                mealResponses.add(mealResponse);
            }

            return mealResponses;

        } catch (JsonProcessingException e) {
            log.error("Error parsing AI response: {}", e.getMessage());
            throw new AIServiceException("Failed to parse AI-generated meal plan", e);
        } catch (Exception e) {
            log.error("Error generating meal plan: {}", e.getMessage());
            throw new AIServiceException("Failed to generate meal plan", e);
        }
    }

    /**
     * Generate an image for a recipe using AI and upload it to Cloudinary.
     *
     * @param recipeTitle       the title of the recipe
     * @param recipeDescription the description of the recipe
     * @return the URL of the generated image
     */
    public String generateRecipeImage(
            @NotEmpty(message = "Recipe title cannot be empty") String recipeTitle,
            @NotEmpty(message = "Recipe description cannot be empty") String recipeDescription) {

        log.info("Generating image for recipe: {}", recipeTitle);

        try {
            // Get the image prompt from the RecipePrompts class
            String prompt = RecipePrompts.getRecipeImagePrompt(recipeTitle, recipeDescription);

            ImagePrompt imagePrompt = new ImagePrompt(prompt);
            ImageResponse imageResponse = imageClient.call(imagePrompt);

            String aiGeneratedImageUrl = imageResponse.getResult().getOutput().getUrl();

            // Upload the AI-generated image to Cloudinary for permanent storage
            return cloudinaryService.uploadImageFromUrl(aiGeneratedImageUrl);
        } catch (Exception e) {
            log.error("Error generating image for recipe: {}", recipeTitle, e);
            throw new AIServiceException("Failed to generate recipe image", e);
        }
    }
} 