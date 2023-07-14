package com.writesmith.model.generation;

import com.oaigptconnector.model.generation.OpenAIGPTModels;

import java.util.List;

public class OpenAIGPTModelTierSpecification {

    public static final OpenAIGPTModels defaultModel = OpenAIGPTModels.GPT_3_5_TURBO;
    public static final OpenAIGPTModels defaultFunctionModel = OpenAIGPTModels.GPT_3_5_TURBO_0613;
    public static final List<OpenAIGPTModels> freeModels = List.of(
            OpenAIGPTModels.GPT_3_5_TURBO,
            OpenAIGPTModels.GPT_3_5_TURBO_0613
    );
    public static final List<OpenAIGPTModels> paidModels = List.of(
            OpenAIGPTModels.GPT_4,
            OpenAIGPTModels.GPT_4_0613
    );

}
