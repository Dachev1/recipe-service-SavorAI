package dev.idachev.recipeservice.infrastructure.storage;

import com.cloudinary.Cloudinary;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;

/**
 * Service for handling image uploads to Cloudinary
 */
@Service
@Slf4j
public class CloudinaryService {

    private static final String RECIPE_IMAGES_FOLDER = "recipe-images";
    private static final String GENERATED_RECIPE_IMAGES_FOLDER = "generated-recipe-images";
    private static final String RESOURCE_TYPE = "auto";
    // Fallback images that will always work
    private static final String FALLBACK_IMAGE_URL = "https://images.unsplash.com/photo-1546069901-ba9599a7e63c?w=600&auto=format&fit=crop";

    private final Cloudinary cloudinary;
    private final boolean isCloudinaryConfigured;

    @Autowired
    public CloudinaryService(Cloudinary cloudinary) {
        this.cloudinary = cloudinary;
        // Check if Cloudinary is properly configured
        Map<String, Object> config = cloudinary.config.asMap();
        String cloudName = (String) config.get("cloud_name");
        isCloudinaryConfigured = cloudName != null && !cloudName.startsWith("${") && !cloudName.isEmpty();
        
        if (!isCloudinaryConfigured) {
            log.warn("Cloudinary is not properly configured. Cloud name is missing or invalid. Falling back to default image URL.");
        } else {
            log.info("Cloudinary configured with cloud name: {}", cloudName);
        }
    }

    /**
     * Uploads an image from a URL to Cloudinary
     */
    public String uploadImageFromUrl(String imageUrl) {
        if (imageUrl == null || imageUrl.trim().isEmpty() || !isCloudinaryConfigured) {
            return FALLBACK_IMAGE_URL;
        }

        log.debug("Uploading image from URL: {}", imageUrl);
        String uniqueFilename = generateUniqueFilename();

        Map<String, Object> options = Map.of(
                "folder", GENERATED_RECIPE_IMAGES_FOLDER,
                "resource_type", RESOURCE_TYPE,
                "public_id", uniqueFilename
        );

        try {
            return processUpload(imageUrl, options);
        } catch (Exception e) {
            log.error("Failed to upload image from URL: {}", imageUrl, e);
            // Return a fallback URL that will always work
            return FALLBACK_IMAGE_URL;
        }
    }

    /**
     * Uploads a file to Cloudinary
     */
    public String uploadFile(MultipartFile file) {
        if (file == null || file.isEmpty() || !isCloudinaryConfigured) {
            return FALLBACK_IMAGE_URL;
        }

        log.debug("Uploading file: {}", file.getOriginalFilename());

        Map<String, Object> options = Map.of(
                "folder", RECIPE_IMAGES_FOLDER,
                "resource_type", RESOURCE_TYPE,
                "public_id", generateUniqueFilename()
        );

        try {
            return processUpload(file.getBytes(), options);
        } catch (Exception e) {
            log.error("Failed to upload file: {}", file.getOriginalFilename(), e);
            // Return a fallback URL that will always work
            return FALLBACK_IMAGE_URL;
        }
    }

    /**
     * Processes the upload to Cloudinary
     */
    private String processUpload(Object input, Map<String, Object> options) throws IOException {
        if (!isCloudinaryConfigured) {
            log.warn("Skipping Cloudinary upload because configuration is incomplete");
            return FALLBACK_IMAGE_URL;
        }
        
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> uploadResult = cloudinary.uploader().upload(input, options);

            String secureUrl = (String) uploadResult.get("secure_url");
            if (secureUrl == null) {
                log.warn("Cloudinary upload succeeded but secure URL is missing");
                return FALLBACK_IMAGE_URL;
            }

            log.debug("Image uploaded successfully: {}", secureUrl);
            return secureUrl;
        } catch (Exception e) {
            log.error("Error during Cloudinary upload: {}", e.getMessage());
            throw new IOException("Failed to upload to Cloudinary", e);
        }
    }

    private String generateUniqueFilename() {
        return String.valueOf(UUID.randomUUID());
    }
} 