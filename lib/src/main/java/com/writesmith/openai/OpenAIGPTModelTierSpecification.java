package com.writesmith.openai;

import com.oaigptconnector.model.generation.OpenAIGPTModels;

import java.util.List;

public class OpenAIGPTModelTierSpecification {

    public static final OpenAIGPTModels defaultModel = OpenAIGPTModels.GPT_3_5_TURBO;
    public static final OpenAIGPTModels defaultFunctionModel = OpenAIGPTModels.GPT_3_5_TURBO_0613;
    public static final OpenAIGPTModels defaultFreeVisionModel = null;
    public static final OpenAIGPTModels defaultPaidVisionModel = OpenAIGPTModels.GPT_4_VISION;
    public static final List<OpenAIGPTModels> freeModels = List.of(
            OpenAIGPTModels.GPT_3_5_TURBO,
            OpenAIGPTModels.GPT_3_5_TURBO_0613
    );
    public static final List<OpenAIGPTModels> paidModels = List.of(
            OpenAIGPTModels.GPT_4,
            OpenAIGPTModels.GPT_4_LONGINPUT,
            OpenAIGPTModels.GPT_4_VISION
    );

    public static final List<OpenAIGPTModels> freeVisionModels = List.of();

    public static final List<OpenAIGPTModels> paidVisionModels = List.of(
            OpenAIGPTModels.GPT_4_VISION
    );

}
