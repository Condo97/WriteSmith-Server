package com.writesmith.openai.functioncall;

import com.oaigptconnector.model.FCParameter;
import com.oaigptconnector.model.FunctionCall;

@FunctionCall(name = "Classify_Chat", functionDescription = "Takes an input chat and classifies it.")
public class ClassifyChatFC {

    @FCParameter(name = "Provided_Image_Generation_Prompt_And_Requested_Generation", description = "True if the user explicitly and certainly requested image generation, creation, making, or similar and provided a prompt to use in DALL-E, otherwise false.")
    private Boolean wantsImageGeneration;

    @FCParameter(name = "Requested_Web_Search", description = "True if the user explicitly requested a web search, otherwise false.")//description = "True if the user explicitly requested a web search, or if the user is requesting data that is not available or is too new for GPT and a google search can easily be deduced from the input, otherwise false.")
    private Boolean wantsWebSearch;

    public ClassifyChatFC() {

    }

    public Boolean getWantsImageGeneration() {
        return wantsImageGeneration;
    }

    public Boolean getWantsWebSearch() {
        return wantsWebSearch;
    }

}
