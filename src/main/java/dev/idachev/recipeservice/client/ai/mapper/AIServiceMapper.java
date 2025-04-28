package dev.idachev.recipeservice.client.ai.mapper;

import dev.idachev.recipeservice.client.ai.dto.AISimplifiedRecipeDto;
import dev.idachev.recipeservice.client.ai.dto.IngredientDto;
import dev.idachev.recipeservice.client.ai.dto.MacroNutrientDto;
import dev.idachev.recipeservice.web.dto.IngredientResponse;
import dev.idachev.recipeservice.web.dto.MacroNutrientResponse;
import dev.idachev.recipeservice.web.dto.SimplifiedRecipeResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface AIServiceMapper {

    @Mapping(target = "ingredients", source = "ingredients")
    @Mapping(target = "macroNutrients", source = "macroNutrients")
    @Mapping(target = "difficultyLevel", source = "difficultyLevel")
    @Mapping(target = "recipeName", source = "recipeName")
    @Mapping(target = "recipeDescription", source = "recipeDescription")
    @Mapping(target = "recipeInstructions", source = "recipeInstructions")
    @Mapping(target = "recipeImageUrl", source = "recipeImageUrl")
    @Mapping(target = "recipeId", source = "recipeId")
    AISimplifiedRecipeDto toAISimplifiedRecipeDto(List<IngredientDto> ingredients, List<MacroNutrientDto> macroNutrients, String difficultyLevel, String recipeName, String recipeDescription, String recipeInstructions, String recipeImageUrl, String recipeId);

    @Mapping(target = "ingredientName", source = "ingredientName")
    @Mapping(target = "quantity", source = "quantity")
    @Mapping(target = "unit", source = "unit")
    IngredientDto toIngredientDto(String ingredientName, String quantity, String unit);

    @Mapping(target = "macroNutrientName", source = "macroNutrientName")
    @Mapping(target = "quantity", source = "quantity")
    @Mapping(target = "unit", source = "unit")
    MacroNutrientDto toMacroNutrientDto(String macroNutrientName, String quantity, String unit);

    @Mapping(target = "ingredientName", source = "ingredientName")
    @Mapping(target = "quantity", source = "quantity")
    @Mapping(target = "unit", source = "unit")
    IngredientResponse toIngredientResponse(String ingredientName, String quantity, String unit);

    @Mapping(target = "macroNutrientName", source = "macroNutrientName")
    @Mapping(target = "quantity", source = "quantity")
    @Mapping(target = "unit", source = "unit")
    MacroNutrientResponse toMacroNutrientResponse(String macroNutrientName, String quantity, String unit);

    @Mapping(target = "title", source = "recipeName")
    @Mapping(target = "description", source = "recipeDescription")
    @Mapping(target = "instructions", source = "recipeInstructions")
    @Mapping(target = "imageUrl", source = "recipeImageUrl")
    @Mapping(target = "recipeId", source = "recipeId")
    @Mapping(target = "ingredients", ignore = true)
    @Mapping(target = "totalTimeMinutes", ignore = true)
    @Mapping(target = "macros", ignore = true)
    @Mapping(target = "difficulty", ignore = true)
    @Mapping(target = "servingSuggestions", ignore = true)
    SimplifiedRecipeResponse toSimplifiedRecipeResponse(String recipeName, String recipeDescription, String recipeInstructions, String recipeImageUrl, String recipeId);
} 