package com.writesmith.core.gpt_function_calls;

import com.oaigptconnector.model.FCParameter;
import com.oaigptconnector.model.FunctionCall;

@FunctionCall(name = "Classify_Chat", functionDescription = "Takes an input chat and classifies it.")
public class ClassifyChatFC {

    @FCParameter(name = "Provided_Image_Generation_Prompt_And_Requested_Generation", description = "True if the user explicitly and certainly requested image generation, creation, making, or similar and provided a prompt to use in DALL-E, otherwise false.")
    private Boolean wantsImageGeneration;

    public ClassifyChatFC() {

    }

    public Boolean getWantsImageGeneration() {
        return wantsImageGeneration;
    }

}
