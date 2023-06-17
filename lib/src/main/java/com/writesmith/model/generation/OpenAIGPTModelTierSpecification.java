package com.writesmith.model.generation;

import java.util.List;

public class OpenAIGPTModelTierSpecification {

    public static final OpenAIGPTModels defaultModel = OpenAIGPTModels.GPT_3_5_TURBO;
    public static final List<OpenAIGPTModels> freeModels = List.of(
            OpenAIGPTModels.GPT_3_5_TURBO
    );
    public static final List<OpenAIGPTModels> paidModels = List.of(
            OpenAIGPTModels.GPT_4
    );

}
