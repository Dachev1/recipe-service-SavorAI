package dev.idachev.recipeservice.service;

import dev.idachev.recipeservice.exception.AIServiceException;
import dev.idachev.recipeservice.exception.ImageProcessingException;
import dev.idachev.recipeservice.infrastructure.ai.AIService;
import dev.idachev.recipeservice.infrastructure.storage.ImageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

/**
 * Service for recipe image operations.
 * Handles uploading and generating images for recipes.
 */
@Service
@Slf4j
public class RecipeImageService {

    private final AIService aiService;
    private final ImageService imageService;

    @Autowired
    public RecipeImageService(AIService aiService, ImageService imageService) {
        this.aiService = aiService;
        this.imageService = imageService;
    }

    /**
     * Process image for a recipe (upload or generate)
     * <p>
     * If a MultipartFile is provided, it will be uploaded.
     * If no image is provided but a title is available, an image will be generated.
     *
     * @param title               Recipe title, used for generating an image if none is uploaded
     * @param servingSuggestions  Recipe serving suggestions, used for generating an image
     * @param image               Optional image file to upload
     * @return URL of the processed image
     * @throws ImageProcessingException if processing fails (upload or generation)
     * @throws AIServiceException if AI generation specifically fails (propagated)
     */
    public String processRecipeImage(String title, String servingSuggestions, MultipartFile image)
            throws ImageProcessingException, AIServiceException {
        try {
            if (image != null && !image.isEmpty()) {
                log.debug("Uploading image for recipe title: {}", title);
                String imageUrl = uploadRecipeImage(image);
                log.info("Image uploaded for recipe '{}': {}", title, imageUrl);
                return imageUrl;
            }

            if (StringUtils.hasText(title)) {
                log.debug("No image provided, attempting generation for: {}", title);
                return generateRecipeImage(title, servingSuggestions);
            }

            log.debug("No image provided and no title available for image generation");
            return null;
        } catch (ImageProcessingException | AIServiceException e) {
            log.error("Failed to process recipe image for title '{}': {}", title, e.getMessage());
            throw e;
        } catch (Exception e) {
            String errorMessage = String.format("Unexpected error processing image for recipe title '%s': %s", title, e.getMessage());
            log.error(errorMessage, e);
            throw new ImageProcessingException(errorMessage, e);
        }
    }

    /**
     * Upload a recipe image
     *
     * @param image Image file to upload
     * @return URL of the uploaded image
     * @throws ImageProcessingException if upload fails
     */
    private String uploadRecipeImage(MultipartFile image) throws ImageProcessingException {
        try {
            return imageService.uploadImage(image);
        } catch (Exception e) {
            String errorMessage = "Failed to upload image: " + e.getMessage();
            log.error(errorMessage, e);
            throw new ImageProcessingException(errorMessage, e);
        }
    }

    /**
     * Generate image for a recipe using AI
     *
     * @param title               Recipe title, used as the primary prompt for generation
     * @param servingSuggestions  Recipe serving suggestions, used to enhance the prompt
     * @return URL of the generated image
     * @throws ImageProcessingException if generation fails unexpectedly
     * @throws AIServiceException if the AI service reports an error (propagated)
     */
    public String generateRecipeImage(String title, String servingSuggestions)
            throws ImageProcessingException, AIServiceException {
        if (!StringUtils.hasText(title)) {
            throw new IllegalArgumentException("Cannot generate image: Recipe title is empty");
        }

        try {
            log.info("Requesting AI image generation for recipe: {}", title);
            String imageUrl = aiService.generateRecipeImage(title, servingSuggestions);

            if (!StringUtils.hasText(imageUrl)) {
                String errorMessage = String.format("AI service returned empty image URL for recipe: %s", title);
                log.warn(errorMessage);
                throw new ImageProcessingException(errorMessage);
            }

            log.info("Successfully generated image for recipe: {}", title);
            return imageUrl;
        } catch (AIServiceException e) {
            log.error("AI service failed to generate image for title '{}': {}", title, e.getMessage());
            throw e;
        } catch (Exception e) {
            String errorMessage = String.format("Unexpected error generating image for recipe title '%s': %s", title, e.getMessage());
            log.error(errorMessage, e);
            throw new ImageProcessingException(errorMessage, e);
        }
    }
} 