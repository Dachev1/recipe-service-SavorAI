package dev.idachev.recipeservice.client.ai.mapper;

import dev.idachev.recipeservice.client.ai.dto.AISimplifiedRecipeDto;
import dev.idachev.recipeservice.client.ai.dto.IngredientDto;
import dev.idachev.recipeservice.client.ai.dto.MacroNutrientDto;
import dev.idachev.recipeservice.model.DifficultyLevel;
import dev.idachev.recipeservice.web.dto.IngredientResponse;
import dev.idachev.recipeservice.web.dto.MacroNutrientResponse;
import dev.idachev.recipeservice.web.dto.MacrosDto;
import dev.idachev.recipeservice.web.dto.SimplifiedRecipeResponse;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2025-04-28T09:10:50+0300",
    comments = "version: 1.5.5.Final, compiler: Eclipse JDT (IDE) 3.42.0.z20250331-1358, environment: Java 21.0.6 (Eclipse Adoptium)"
)
@Component
public class AIServiceMapperImpl implements AIServiceMapper {

    @Override
    public AISimplifiedRecipeDto toAISimplifiedRecipeDto(List<IngredientDto> ingredients, List<MacroNutrientDto> macroNutrients, String difficultyLevel, String recipeName, String recipeDescription, String recipeInstructions, String recipeImageUrl, String recipeId) {
        if ( ingredients == null && macroNutrients == null && difficultyLevel == null && recipeName == null && recipeDescription == null && recipeInstructions == null && recipeImageUrl == null && recipeId == null ) {
            return null;
        }

        List<IngredientDto> ingredients1 = null;
        List<IngredientDto> list = ingredients;
        if ( list != null ) {
            ingredients1 = new ArrayList<IngredientDto>( list );
        }
        List<MacroNutrientDto> macroNutrients1 = null;
        List<MacroNutrientDto> list1 = macroNutrients;
        if ( list1 != null ) {
            macroNutrients1 = new ArrayList<MacroNutrientDto>( list1 );
        }
        String difficultyLevel1 = null;
        difficultyLevel1 = difficultyLevel;
        String recipeName1 = null;
        recipeName1 = recipeName;
        String recipeDescription1 = null;
        recipeDescription1 = recipeDescription;
        String recipeInstructions1 = null;
        recipeInstructions1 = recipeInstructions;
        String recipeImageUrl1 = null;
        recipeImageUrl1 = recipeImageUrl;
        String recipeId1 = null;
        recipeId1 = recipeId;

        AISimplifiedRecipeDto aISimplifiedRecipeDto = new AISimplifiedRecipeDto( ingredients1, macroNutrients1, difficultyLevel1, recipeName1, recipeDescription1, recipeInstructions1, recipeImageUrl1, recipeId1 );

        return aISimplifiedRecipeDto;
    }

    @Override
    public IngredientDto toIngredientDto(String ingredientName, String quantity, String unit) {
        if ( ingredientName == null && quantity == null && unit == null ) {
            return null;
        }

        String ingredientName1 = null;
        ingredientName1 = ingredientName;
        String quantity1 = null;
        quantity1 = quantity;
        String unit1 = null;
        unit1 = unit;

        IngredientDto ingredientDto = new IngredientDto( ingredientName1, quantity1, unit1 );

        return ingredientDto;
    }

    @Override
    public MacroNutrientDto toMacroNutrientDto(String macroNutrientName, String quantity, String unit) {
        if ( macroNutrientName == null && quantity == null && unit == null ) {
            return null;
        }

        String macroNutrientName1 = null;
        macroNutrientName1 = macroNutrientName;
        String quantity1 = null;
        quantity1 = quantity;
        String unit1 = null;
        unit1 = unit;

        MacroNutrientDto macroNutrientDto = new MacroNutrientDto( macroNutrientName1, quantity1, unit1 );

        return macroNutrientDto;
    }

    @Override
    public IngredientResponse toIngredientResponse(String ingredientName, String quantity, String unit) {
        if ( ingredientName == null && quantity == null && unit == null ) {
            return null;
        }

        String ingredientName1 = null;
        ingredientName1 = ingredientName;
        String quantity1 = null;
        quantity1 = quantity;
        String unit1 = null;
        unit1 = unit;

        IngredientResponse ingredientResponse = new IngredientResponse( ingredientName1, quantity1, unit1 );

        return ingredientResponse;
    }

    @Override
    public MacroNutrientResponse toMacroNutrientResponse(String macroNutrientName, String quantity, String unit) {
        if ( macroNutrientName == null && quantity == null && unit == null ) {
            return null;
        }

        String macroNutrientName1 = null;
        macroNutrientName1 = macroNutrientName;
        String quantity1 = null;
        quantity1 = quantity;
        String unit1 = null;
        unit1 = unit;

        MacroNutrientResponse macroNutrientResponse = new MacroNutrientResponse( macroNutrientName1, quantity1, unit1 );

        return macroNutrientResponse;
    }

    @Override
    public SimplifiedRecipeResponse toSimplifiedRecipeResponse(String recipeName, String recipeDescription, String recipeInstructions, String recipeImageUrl, String recipeId) {
        if ( recipeName == null && recipeDescription == null && recipeInstructions == null && recipeImageUrl == null && recipeId == null ) {
            return null;
        }

        String title = null;
        title = recipeName;
        String description = null;
        description = recipeDescription;
        String instructions = null;
        instructions = recipeInstructions;
        String imageUrl = null;
        imageUrl = recipeImageUrl;
        String recipeId1 = null;
        recipeId1 = recipeId;

        List<String> ingredients = null;
        Integer totalTimeMinutes = null;
        MacrosDto macros = null;
        DifficultyLevel difficulty = null;
        String servingSuggestions = null;

        SimplifiedRecipeResponse simplifiedRecipeResponse = new SimplifiedRecipeResponse( title, description, instructions, ingredients, imageUrl, totalTimeMinutes, macros, difficulty, servingSuggestions, recipeId1 );

        return simplifiedRecipeResponse;
    }
}
