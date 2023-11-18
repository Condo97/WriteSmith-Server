package com.writesmith.core;

import com.oaigptconnector.model.generation.OpenAIGPTModels;
import com.writesmith.core.service.ChatContextLimiter;
import com.writesmith.database.model.objects.Chat;

import java.util.List;

public class WSChatGenerationPreparer {

    public static class PreparedChats {

        private List<Chat> limitedChats;
        private OpenAIGPTModels approvedModel;

        public PreparedChats(List<Chat> limitedChats, OpenAIGPTModels approvedModel) {
            this.limitedChats = limitedChats;
            this.approvedModel = approvedModel;
        }

        public List<Chat> getLimitedChats() {
            return limitedChats;
        }

        public OpenAIGPTModels getApprovedModel() {
            return approvedModel;
        }

    }

    public static PreparedChats prepare(List<Chat> chats, OpenAIGPTModels requestedModel, boolean isPremium) {
        return prepare(chats, requestedModel, isPremium, true);
    }

    private static PreparedChats prepare(List<Chat> chats, OpenAIGPTModels requestedModel, boolean isPremium, boolean upgradeIfNecessary) {
        // Get offeredModel and its character limit
        OpenAIGPTModels offeredModel = WSGenerationTierLimits.getOfferedModelForTier(requestedModel, isPremium);
        int offeredModelCharacterLimit = WSGenerationTierLimits.getContextCharacterLimit(offeredModel, isPremium);

        // Get limited chats for offered model, skipping images only if offeredModel is not vision and upgradeIfNecessary is false
        List<Chat> limitedChats = ChatContextLimiter.getLimitedChats(chats, offeredModelCharacterLimit, !offeredModel.isVision() && !upgradeIfNecessary);

        // If upgrade if necessary...
        if (upgradeIfNecessary) {
            // If there are images and the model is not vision...
            if (limitedChats.stream().anyMatch(c -> c.getImageData() != null && !c.getImageData().isEmpty()) && !offeredModel.isVision()) {
                // Can the model be upgraded to vision? Yes, if vision model for tier is not null
                OpenAIGPTModels visionUpgradedModel = WSGenerationTierLimits.getVisionModelForTier(offeredModel, isPremium);

                if (visionUpgradedModel != null && visionUpgradedModel.isVision() /* The isVision check is redundant */) {
                    // Model should be upgraded, re-prepare with visionUpgradedModel and upgradeIfNecessary set to false :)
                    return prepare(chats, visionUpgradedModel, isPremium, false);
                } else {
                    // Model should not be upgraded, re-prepare with offeredModel and upgradeIfNecessary set to false, which should skip images automatically since the offeredModel would not be vision and upgradeIfNecessary is set to false
                    return prepare(chats, offeredModel, isPremium, false);
                }
            }
        }

        // Return PreparedChats with limitedChats and offeredModel
        // See, I want to get if there were images removed.. which requires knowing if there are images and if it's a vision model. But maybe the images should just be removed automatically... It's just skipping adding to the context character limit, the images are still contained. So maybe set the images were removed when it is actually generating the chat

        return new PreparedChats(
                limitedChats,
                offeredModel
        );
    }

//    private static PreparedChats prepare(List<Chat> chats, OpenAIGPTModels requestedModel, int requestedModelCharacterLimit, OpenAIGPTModels visionUpgradedModel, int visionUpgradedModelCharacterLimit, boolean neverUpgrade) {
//        List<Chat> limitedChats = new ArrayList<>();
//        boolean modelUpgraded = false;
//        boolean imageRemoved = false;
//
//        int totalChars = 0;
//        boolean cannotUpgrade = neverUpgrade;
//
//        for (int i = chats.size() - 1; i >= 0; i--) {
//            if (totalChars < (modelUpgraded ? visionUpgradedModelCharacterLimit : requestedModelCharacterLimit)) {
//
//                // If image is encountered...
//                if (chats.get(i).getImageData() != null && !chats.get(i).getImageData().isEmpty()) {
//                    if (visionUpgradedModel == null || totalChars > visionUpgradedModelCharacterLimit) {
//                        // If visionUpgradedModel is null or totalChars is greater than requestedModelCharacterLimit set cannotUpgrade to true
//                        cannotUpgrade = true;
//                    } else {
//                        // If it can upgrade and is not upgraded, upgrade
//                        modelUpgraded = true;
//                    }
//                }
//
//                // If modelUpgraded, include images
//
//
//            }
//        }
//    }

}
