package service;

import dev.idachev.recipeservice.exception.ImageProcessingException;
import dev.idachev.recipeservice.infrastructure.ai.AIService;
import dev.idachev.recipeservice.infrastructure.storage.ImageService;
import dev.idachev.recipeservice.service.RecipeImageService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class RecipeImageServiceUTest {

    @Mock
    private AIService aiService;
    
    @Mock
    private ImageService imageService;
    
    @InjectMocks
    private RecipeImageService recipeImageService;
    
    @Test
    void givenImageFile_whenProcessRecipeImage_thenUploadImage() {
        // Given
        String title = "Pasta Carbonara";
        String description = "A delicious pasta dish";
        
        MultipartFile image = new MockMultipartFile(
            "image", 
            "image.jpg", 
            "image/jpeg", 
            "image content".getBytes()
        );
        
        String expectedUrl = "http://example.com/images/pasta.jpg";
        
        when(imageService.uploadImage(image)).thenReturn(expectedUrl);
        
        // When
        String result = recipeImageService.processRecipeImage(title, description, image);
        
        // Then
        assertNotNull(result);
        assertEquals(expectedUrl, result);
        
        verify(imageService).uploadImage(image);
        verify(aiService, never()).generateRecipeImage(anyString(), anyString());
    }
    
    @Test
    void givenNoImageButTitle_whenProcessRecipeImage_thenGenerateImage() {

        // Given
        String title = "Pasta Carbonara";
        String description = "A delicious pasta dish";
        MultipartFile image = null;
        
        String expectedUrl = "http://example.com/ai-images/pasta.jpg";
        
        when(aiService.generateRecipeImage(title, description)).thenReturn(expectedUrl);
        
        // When
        String result = recipeImageService.processRecipeImage(title, description, image);
        
        // Then
        assertNotNull(result);
        assertEquals(expectedUrl, result);
        
        verify(aiService).generateRecipeImage(title, description);
        verify(imageService, never()).uploadImage(any(MultipartFile.class));
    }
    
    @Test
    void givenEmptyImageFile_whenProcessRecipeImage_thenGenerateImage() {

        // Given
        String title = "Pasta Carbonara";
        String description = "A delicious pasta dish";
        
        // Create a properly mocked MultipartFile
        MultipartFile emptyImage = mock(MultipartFile.class);
        when(emptyImage.isEmpty()).thenReturn(true);
        
        String expectedUrl = "http://example.com/ai-images/pasta.jpg";
        
        when(aiService.generateRecipeImage(title, description)).thenReturn(expectedUrl);
        
        // When
        String result = recipeImageService.processRecipeImage(title, description, emptyImage);
        
        // Then
        assertNotNull(result);
        assertEquals(expectedUrl, result);
        
        verify(aiService).generateRecipeImage(title, description);
        verify(imageService, never()).uploadImage(any(MultipartFile.class));
    }
    
    @Test
    void givenNoImageAndNoTitle_whenProcessRecipeImage_thenReturnNull() {

        // Given
        String title = "";
        String description = "A delicious dish";
        MultipartFile image = null;
        
        // When
        String result = recipeImageService.processRecipeImage(title, description, image);
        
        // Then
        assertNull(result);
        
        verify(aiService, never()).generateRecipeImage(anyString(), anyString());
        verify(imageService, never()).uploadImage(any(MultipartFile.class));
    }
    
    @Test
    void givenUploadError_whenProcessRecipeImage_thenReturnNull() {

        // Given
        String title = "Pasta Carbonara";
        String description = "A delicious pasta dish";
        
        MultipartFile image = new MockMultipartFile(
            "image", 
            "image.jpg", 
            "image/jpeg", 
            "image content".getBytes()
        );
        
        when(imageService.uploadImage(image)).thenThrow(new ImageProcessingException("Upload failed", null));
        
        // When
        String result = recipeImageService.processRecipeImage(title, description, image);
        
        // Then
        assertNull(result);
        
        verify(imageService).uploadImage(image);
        verify(aiService, never()).generateRecipeImage(anyString(), anyString());
    }
    
    @Test
    void givenTitle_whenGenerateRecipeImage_thenReturnImageUrl() {

        // Given
        String title = "Pasta Carbonara";
        String description = "A delicious pasta dish";
        String expectedUrl = "http://example.com/ai-images/pasta.jpg";
        
        when(aiService.generateRecipeImage(title, description)).thenReturn(expectedUrl);
        
        // When
        String result = recipeImageService.generateRecipeImage(title, description);
        
        // Then
        assertNotNull(result);
        assertEquals(expectedUrl, result);
        
        verify(aiService).generateRecipeImage(title, description);
    }
    
    @Test
    void givenEmptyTitle_whenGenerateRecipeImage_thenReturnNull() {

        // Given
        String title = "";
        String description = "A delicious dish";
        
        // When
        String result = recipeImageService.generateRecipeImage(title, description);
        
        // Then
        assertNull(result);
        
        verify(aiService, never()).generateRecipeImage(anyString(), anyString());
    }
    
    @Test
    void givenGenerationError_whenGenerateRecipeImage_thenReturnNull() {

        // Given
        String title = "Pasta Carbonara";
        String description = "A delicious pasta dish";
        
        when(aiService.generateRecipeImage(title, description)).thenThrow(new RuntimeException("Generation failed"));
        
        // When
        String result = recipeImageService.generateRecipeImage(title, description);
        
        // Then
        assertNull(result);
        
        verify(aiService).generateRecipeImage(title, description);
    }
    
    @Test
    void givenEmptyGeneratedUrl_whenGenerateRecipeImage_thenReturnNull() {

        // Given
        String title = "Pasta Carbonara";
        String description = "A delicious pasta dish";
        
        when(aiService.generateRecipeImage(title, description)).thenReturn("");
        
        // When
        String result = recipeImageService.generateRecipeImage(title, description);
        
        // Then
        assertNull(result);
        
        verify(aiService).generateRecipeImage(title, description);
    }
}
