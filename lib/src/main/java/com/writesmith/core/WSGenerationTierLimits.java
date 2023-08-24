package com.writesmith.core;

import com.oaigptconnector.model.generation.OpenAIGPTModels;
import com.writesmith.Constants;
import com.writesmith.model.generation.OpenAIGPTModelTierSpecification;

public class WSGenerationTierLimits {

    public static int getTokenLimit(boolean isPremium) {
        return isPremium ? Constants.Response_Token_Limit_Paid : Constants.Response_Token_Limit_Free;
    }

    public static int getContextCharacterLimit(boolean isPremium) {
        return isPremium ? Constants.Character_Limit_Paid : Constants.Character_Limit_Free;
    }

    public static OpenAIGPTModels getOfferedModelForTier(OpenAIGPTModels model, boolean isPremium) {
        // If isPremium, currently will always return the current model, so just do that for now
        if (isPremium)
            return model;

        // If is not premium and model is not a premium model, return that model
        if (OpenAIGPTModelTierSpecification.freeModels.contains(model))
            return model;

        // Otherwise, return default model
        return OpenAIGPTModelTierSpecification.defaultModel;
    }

}
