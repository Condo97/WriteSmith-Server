package com.writesmith.model.generation;

public enum OpenAIGPTModels {

    GPT_3_5_TURBO("gpt-3.5-turbo"),
    GPT_4("gpt-4");

    public String name;

    OpenAIGPTModels(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return name;
    }

}
