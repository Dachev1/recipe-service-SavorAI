package dev.idachev.recipeservice.infrastructure.storage;

import dev.idachev.recipeservice.exception.ImageProcessingException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

/**
 * Infrastructure service for image storage operations.
 */
@Service
@Slf4j
public class ImageService {

    private final CloudinaryService cloudinaryService;

    @Autowired
    public ImageService(CloudinaryService cloudinaryService) {
        this.cloudinaryService = cloudinaryService;
    }

    /**
     * Upload an image file and return its URL.
     */
    public String uploadImage(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            log.warn("Attempted to upload null or empty file");
            return null;
        }

        try {
            return cloudinaryService.uploadFile(file);
        } catch (Exception e) {
            log.error("Error uploading image: {}", e.getMessage());
            throw new ImageProcessingException("Failed to upload image", e);
        }
    }
} 