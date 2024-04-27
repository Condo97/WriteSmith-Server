package com.writesmith.core;

import com.oaigptconnector.model.generation.OpenAIGPTModels;
import com.writesmith.Constants;
import com.writesmith.openai.OpenAIGPTModelTierSpecification;

public class WSGenerationTierLimits {

    public static int getTokenLimit(OpenAIGPTModels model, boolean isPremium) {
        if (isPremium) {
            if (model == OpenAIGPTModels.GPT_4_VISION) {
                return Constants.Response_Token_Limit_GPT_4_Vision_Paid;
            } else if (model == OpenAIGPTModels.GPT_4 || model == OpenAIGPTModels.GPT_4_LONGINPUT) {
                return Constants.Response_Token_Limit_GPT_4_Paid;
            } else if (model == OpenAIGPTModels.GPT_3_5_TURBO) {
                return Constants.Response_Token_Limit_GPT_3_Turbo_Paid;
            } else {
                System.out.println("Model not specified when getting paid tokenLimit, using GPT_3_Turbo_Paid");
                return Constants.Response_Token_Limit_GPT_3_Turbo_Paid;
            }
        } else {
            if (model == OpenAIGPTModels.GPT_4_VISION) {
                return Constants.Response_Token_Limit_GPT_4_Vision_Free;
            } else if (model == OpenAIGPTModels.GPT_4 || model == OpenAIGPTModels.GPT_4_LONGINPUT) {
                return Constants.Response_Token_Limit_GPT_4_Free;
            } else if (model == OpenAIGPTModels.GPT_3_5_TURBO) {
                return Constants.Response_Token_Limit_GPT_3_Turbo_Free;
            } else {
                System.out.println("Model not specified when getting free tokenLimit, using GPT_3_Turbo_Free");
                return Constants.Response_Token_Limit_GPT_3_Turbo_Free;
            }
        }
    }

    public static int getContextCharacterLimit(OpenAIGPTModels model, boolean isPremium) {
        if (isPremium) {
            if (model == OpenAIGPTModels.GPT_4_VISION) {
                return Constants.Character_Limit_GPT_4_Vision_Paid;
            } else if (model == OpenAIGPTModels.GPT_4 || model == OpenAIGPTModels.GPT_4_LONGINPUT) {
                return Constants.Character_Limit_GPT_4_Paid;
            } else if (model == OpenAIGPTModels.GPT_3_5_TURBO) {
                return Constants.Character_Limit_GPT_3_Turbo_Paid;
            } else {
                System.out.println("Model not specified when getting paid characterLimit, using GPT_3_Turbo_Paid");
                return Constants.Character_Limit_GPT_3_Turbo_Paid;
            }
        } else {
            if (model == OpenAIGPTModels.GPT_4_VISION) {
                return Constants.Character_Limit_GPT_4_Vision_Free;
            } else if (model == OpenAIGPTModels.GPT_4 || model == OpenAIGPTModels.GPT_4_LONGINPUT) {
                return Constants.Character_Limit_GPT_4_Free;
            } else if (model == OpenAIGPTModels.GPT_3_5_TURBO) {
                return Constants.Character_Limit_GPT_3_Turbo_Free;
            } else {
                System.out.println("Model not specified when getting free characterLimit, using GPT_3_Turbo_Free");
                return Constants.Character_Limit_GPT_3_Turbo_Free;
            }
        }

//        return isPremium ? Constants.Character_Limit_Paid : Constants.Character_Limit_Free;
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

    public static OpenAIGPTModels getVisionModelForTier(boolean isPremium) {
        return getVisionModelForTier(null, isPremium);
    }

    public static OpenAIGPTModels getVisionModelForTier(OpenAIGPTModels model, boolean isPremium) {
        // If isPremium and paidVisionModels contains model return model or if it doesn't contain model return default paid vision model, otherwise continue
        if (isPremium) {
            // If model is a paid vision model, it's fine and return the model
            if (model != null && OpenAIGPTModelTierSpecification.paidVisionModels.contains(model)) {
                return model;
            }

            // If model is not a paid vision model, return the paid vision model
            if (OpenAIGPTModelTierSpecification.defaultPaidVisionModel != null) {
                return OpenAIGPTModelTierSpecification.defaultPaidVisionModel;
            }
        }

        // If freeVisionModels contains model return model, otherwise return defaultFreeVisionModel
        if (model != null && OpenAIGPTModelTierSpecification.freeVisionModels.contains(model)) {
            return model;
        }

        return OpenAIGPTModelTierSpecification.defaultFreeVisionModel;
    }

}
