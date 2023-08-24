//package com.writesmith.core.generation.openai.function.createrecipeidea;
//
//import com.oaigptconnector.model.request.chat.completion.function.OAIGPTChatCompletionRequestFunction;
//import com.oaigptconnector.model.request.chat.completion.function.objects.OAIGPTChatCompletionRequestFunctionObjectArray;
//import com.oaigptconnector.model.request.chat.completion.function.objects.OAIGPTChatCompletionRequestFunctionObjectObject;
//import com.oaigptconnector.model.request.chat.completion.function.objects.OAIGPTChatCompletionRequestFunctionObjectString;
//import com.writesmith.model.http.client.openaigpt.request.OAIGPTChatCompletionRequestFunctionObjectPropertiesCreateRecipeIdea;
//
//import java.util.List;
//
//public class OAIGPTChatCompletionRequestFunctionCreateRecipeIdeaBuilder {
//
//    private static final String defaultFunctionDescription = "Creates a recipe idea from ingredients, adding extras if necessary";
//    private static final String defaultIngredientsDescription = "All of the ingredients of the recipe, including any extras";
//    private static final String defaultEquipmentDescription = "The equipment needed to make the recipe";
//    private static final String defaultNameDescription = "An interesting and fitting name for the recipe";
//    private static final String defaultSummaryDescription = "A 60-80 word interesting summary for the recipe";
//    private static final String defaultCuisineTypeDescription = "A 1-5 word cuisine type for the recipe";
//
//    public static OAIGPTChatCompletionRequestFunction build() {
//        return build(defaultFunctionDescription, defaultIngredientsDescription, defaultEquipmentDescription, defaultNameDescription, defaultSummaryDescription, defaultCuisineTypeDescription);
//    }
//
//    public static OAIGPTChatCompletionRequestFunction build(String functionDescription, String ingredientsDescription, String equipmentDescription, String nameDescription, String summaryDescription, String cuisineTypeDescription) {
//        // Create the OAIGPTChatCompletionRequestFunctionObjectPropertiesCreateRecipeIdea
//        OAIGPTChatCompletionRequestFunctionObjectArray ingredients, equipment;
//        OAIGPTChatCompletionRequestFunctionObjectString name, summary, cuisineType;
//
//        ingredients = new OAIGPTChatCompletionRequestFunctionObjectArray(
//                ingredientsDescription,
//                new OAIGPTChatCompletionRequestFunctionObjectString()
//        );
//
//        equipment = new OAIGPTChatCompletionRequestFunctionObjectArray(
//                equipmentDescription,
//                new OAIGPTChatCompletionRequestFunctionObjectString()
//        );
//
//        name = new OAIGPTChatCompletionRequestFunctionObjectString(nameDescription);
//        summary = new OAIGPTChatCompletionRequestFunctionObjectString(summaryDescription);
//        cuisineType = new OAIGPTChatCompletionRequestFunctionObjectString(cuisineTypeDescription);
//
//        OAIGPTChatCompletionRequestFunctionObjectPropertiesCreateRecipeIdea r = new OAIGPTChatCompletionRequestFunctionObjectPropertiesCreateRecipeIdea(
//                ingredients,
//                equipment,
//                name,
//                summary,
//                cuisineType
//        );
//
//        // Create the OAIGPTChatCompletionRequestFunctionObjectObject
//        OAIGPTChatCompletionRequestFunctionObjectObject rContainer = new OAIGPTChatCompletionRequestFunctionObjectObject(r, null, List.of(
//                "ingredients",
//                "equipment",
//                "name",
//                "summary",
//                "cuisineType"
//        ));
//
//        // Create OAIGPTChatCompletionRequestFunction
//        OAIGPTChatCompletionRequestFunction rFunction = new OAIGPTChatCompletionRequestFunction(
//                "create_recipe_idea",
//                functionDescription,
//                rContainer
//        );
//
//        return rFunction;
//    }
//
//}
