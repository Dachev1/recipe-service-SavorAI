package dev.idachev.recipeservice.mapper;

import dev.idachev.recipeservice.model.Macros;
import dev.idachev.recipeservice.web.dto.MacrosDto;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MacrosMapperUTest {

    @Test
    void givenCompleteMacros_whenToDto_thenReturnMappedDto() {

        // Given
        Macros macros = createMacros(500.0, 30.0, 60.0, 20.0);

        // When
        MacrosDto result = MacrosMapper.toDto(macros);

        // Then
        assertNotNull(result);
        assertEquals(500, result.getCalories());
        assertEquals(30.0, result.getProteinGrams());
        assertEquals(60.0, result.getCarbsGrams());
        assertEquals(20.0, result.getFatGrams());
    }

    @Test
    void givenNullMacros_whenToDto_thenReturnNull() {

        // Given
        Macros macros = null;

        // When
        MacrosDto result = MacrosMapper.toDto(macros);

        // Then
        assertNull(result);
    }

    @Test
    void givenMacrosWithNullFields_whenToDto_thenHandleNullsGracefully() {

        // Given
        Macros macros = createMacros(null, null, null, null);

        // When
        MacrosDto result = MacrosMapper.toDto(macros);

        // Then
        assertNotNull(result);
        // Default values should be used
        assertNull(result.getCalories());
        assertNull(result.getProteinGrams());
        assertNull(result.getCarbsGrams());
        assertNull(result.getFatGrams());
    }

    @Test
    void givenCompleteMacrosDto_whenToEntity_thenReturnMappedEntity() {

        // Given
        MacrosDto macrosDto = MacrosDto.builder()
                .calories(500)
                .proteinGrams(30.0)
                .carbsGrams(60.0)
                .fatGrams(20.0)
                .build();

        // When
        Macros result = MacrosMapper.toEntity(macrosDto);

        // Then
        assertNotNull(result);
        assertEquals(500.0, result.getCalories());
        assertEquals(30.0, result.getProteinGrams());
        assertEquals(60.0, result.getCarbsGrams());
        assertEquals(20.0, result.getFatGrams());
    }

    @Test
    void givenNullMacrosDto_whenToEntity_thenReturnNull() {

        // Given
        MacrosDto macrosDto = null;

        // When
        Macros result = MacrosMapper.toEntity(macrosDto);

        // Then
        assertNull(result);
    }

    @Test
    void givenMacrosDtoWithNullFields_whenToEntity_thenHandleNullsGracefully() {

        // Given
        MacrosDto macrosDto = MacrosDto.builder().build();
        // All fields left null

        // When
        Macros result = MacrosMapper.toEntity(macrosDto);

        // Then
        assertNotNull(result);
        // Default values should be null
        assertNull(result.getCalories());
        assertNull(result.getProteinGrams());
        assertNull(result.getCarbsGrams());
        assertNull(result.getFatGrams());
    }

    @Test
    void givenMacrosEntityAndDto_whenUpdateEntityFromDto_thenUpdateEntityCorrectly() {

        // Given
        Macros macros = createMacros(400.0, 25.0, 50.0, 15.0);

        MacrosDto macrosDto = MacrosDto.builder()
                .calories(500)
                .proteinGrams(30.0)
                .carbsGrams(60.0)
                .fatGrams(20.0)
                .build();

        // When
        MacrosMapper.updateEntityFromDto(macros, macrosDto);

        // Then
        assertEquals(500.0, macros.getCalories());
        assertEquals(30.0, macros.getProteinGrams());
        assertEquals(60.0, macros.getCarbsGrams());
        assertEquals(20.0, macros.getFatGrams());
    }

    @Test
    void givenMacrosEntityAndNullDto_whenUpdateEntityFromDto_thenDoNotModifyEntity() {

        // Given
        Macros macros = createMacros(400.0, 25.0, 50.0, 15.0);
        MacrosDto macrosDto = null;

        // When
        MacrosMapper.updateEntityFromDto(macros, macrosDto);

        // Then
        assertEquals(400.0, macros.getCalories());
        assertEquals(25.0, macros.getProteinGrams());
        assertEquals(50.0, macros.getCarbsGrams());
        assertEquals(15.0, macros.getFatGrams());
    }

    @Test
    void givenNullMacrosEntity_whenUpdateEntityFromDto_thenThrowIllegalArgumentException() {

        // Given
        Macros macros = null;
        MacrosDto macrosDto = MacrosDto.builder().calories(500).build();

        // When & Then
        assertThrows(IllegalArgumentException.class, () -> MacrosMapper.updateEntityFromDto(macros, macrosDto));
    }

    @Test
    void givenMacrosEntityAndDtoWithNullFields_whenUpdateEntityFromDto_thenUpdateWithNulls() {

        // Given
        Macros macros = createMacros(400.0, 25.0, 50.0, 15.0);
        MacrosDto macrosDto = MacrosDto.builder().build();
        // All fields in DTO are null

        // When
        MacrosMapper.updateEntityFromDto(macros, macrosDto);

        // Then
        // Entity should be updated with null values
        assertNull(macros.getCalories());
        assertNull(macros.getProteinGrams());
        assertNull(macros.getCarbsGrams());
        assertNull(macros.getFatGrams());
    }

    // Test helper to create Macros instances with builder-like syntax
    private static Macros createMacros(Double calories, Double protein, Double carbs, Double fat) {
        Macros macros = new Macros();
        macros.setCalories(calories);
        macros.setProteinGrams(protein);
        macros.setCarbsGrams(carbs);
        macros.setFatGrams(fat);
        return macros;
    }
} 