package dev.idachev.recipeservice.mapper;

import dev.idachev.recipeservice.model.Macros;
import dev.idachev.recipeservice.model.Recipe;
import dev.idachev.recipeservice.web.dto.MacrosDto;

/**
 * Utility class for converting between Macros entities and DTOs.
 */
public final class MacrosMapper {

    private MacrosMapper() {
        // Private constructor to prevent instantiation
    }

    /**
     * Convert a Macros entity to a MacrosDto.
     *
     * @param macros the Macros entity
     * @return the MacrosDto
     */
    public static MacrosDto toDto(Macros macros) {
        if (macros == null) {
            return null;
        }

        return MacrosDto.builder()
                .calories(macros.getCalories())
                .proteinGrams(macros.getProteinGrams())
                .carbsGrams(macros.getCarbsGrams())
                .fatGrams(macros.getFatGrams())
                .build();
    }

    /**
     * Convert a MacrosDto to a Macros entity.
     *
     * @param dto the MacrosDto
     * @return the Macros entity
     */
    public static Macros toEntity(MacrosDto dto) {
        if (dto == null) {
            return null;
        }

        return Macros.builder()
                .calories(dto.getCalories())
                .proteinGrams(dto.getProteinGrams())
                .carbsGrams(dto.getCarbsGrams())
                .fatGrams(dto.getFatGrams())
                .build();
    }

    /**
     * Update a Macros entity with data from a MacrosDto.
     *
     * @param macros the Macros entity to update
     * @param dto    the MacrosDto with new data
     */
    public static void updateEntityFromDto(Macros macros, MacrosDto dto) {
        if (macros == null || dto == null) {
            return;
        }

        macros.setCalories(dto.getCalories());
        macros.setProteinGrams(dto.getProteinGrams());
        macros.setCarbsGrams(dto.getCarbsGrams());
        macros.setFatGrams(dto.getFatGrams());
    }
} 