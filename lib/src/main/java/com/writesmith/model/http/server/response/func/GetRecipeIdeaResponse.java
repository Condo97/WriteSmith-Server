package com.writesmith.model.http.server.response.func;

public class GetRecipeIdeaResponse {

    private String name, summary, cuisineType;
    private String[] ingredients, equipment;

    public GetRecipeIdeaResponse() {

    }

    public GetRecipeIdeaResponse(String name, String summary, String cuisineType, String[] ingredients, String[] equipment) {
        this.name = name;
        this.summary = summary;
        this.cuisineType = cuisineType;
        this.ingredients = ingredients;
        this.equipment = equipment;
    }

    public String getName() {
        return name;
    }

    public String getSummary() {
        return summary;
    }

    public String getCuisineType() {
        return cuisineType;
    }

    public String[] getIngredients() {
        return ingredients;
    }

    public String[] getEquipment() {
        return equipment;
    }

}
