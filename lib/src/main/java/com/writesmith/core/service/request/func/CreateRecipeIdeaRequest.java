package com.writesmith.core.service.request.func;

import java.util.List;

public class CreateRecipeIdeaRequest {

    private String authToken;
    private List<String> ingredients, modifiers;
    private Integer expandedIngredients;

    public CreateRecipeIdeaRequest() {

    }

    public CreateRecipeIdeaRequest(String authToken, List<String> ingredients, List<String> modifiers, Integer expandedIngredients) {
        this.authToken = authToken;
        this.ingredients = ingredients;
        this.modifiers = modifiers;
        this.expandedIngredients = expandedIngredients;
    }

    public String getAuthToken() {
        return authToken;
    }

    public List<String> getIngredients() {
        return ingredients;
    }

    public List<String> getModifiers() {
        return modifiers;
    }

    public Integer getExpandedIngredients() {
        return expandedIngredients;
    }

}
