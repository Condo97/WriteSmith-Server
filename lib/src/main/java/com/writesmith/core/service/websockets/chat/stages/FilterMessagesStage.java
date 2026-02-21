package com.writesmith.core.service.websockets.chat.stages;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.oaigptconnector.model.request.chat.completion.OAIChatCompletionRequest;
import com.oaigptconnector.model.request.chat.completion.OAIChatCompletionRequestMessage;
import com.oaigptconnector.model.request.chat.completion.content.OAIChatCompletionRequestMessageContent;
import com.oaigptconnector.model.request.chat.completion.content.OAIChatCompletionRequestMessageContentImageURL;
import com.oaigptconnector.model.request.chat.completion.content.OAIChatCompletionRequestMessageContentText;
import com.writesmith.core.PremiumStatusCache;
import com.writesmith.core.service.websockets.chat.model.ChatPipelineRequest;
import com.writesmith.core.service.websockets.chat.model.FilteredRequest;
import com.writesmith.core.service.websockets.chat.util.ImageDimensionExtractor;
import com.writesmith.util.OpenRouterRequestLogger;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

public class FilterMessagesStage {

    private static final int MAX_INPUT_MESSAGES = 25;
    private static final int MAX_CONVERSATION_INPUT_LENGTH = 50000;
    private static final int IMAGE_TOKEN_DIVISOR = 750;
    private static final double IMAGE_TOKEN_WEIGHT_FREE = 1.0;
    private static final double IMAGE_TOKEN_WEIGHT_PREMIUM = 0.5;
    private static final double IMAGE_FALLBACK_FACTOR_FREE = 0.05;
    private static final double IMAGE_FALLBACK_FACTOR_PREMIUM = 0.025;

    private static final ObjectMapper NON_NULL_MAPPER;

    static {
        NON_NULL_MAPPER = new ObjectMapper();
        NON_NULL_MAPPER.setSerializationInclusion(JsonInclude.Include.NON_NULL);
    }

    private static final ExecutorService SHARED_EXECUTOR = Executors.newCachedThreadPool(r -> {
        Thread t = new Thread(r, "FilterStage-" + System.currentTimeMillis());
        t.setDaemon(true);
        return t;
    });

    public FilteredRequest execute(ChatPipelineRequest pipelineRequest) throws IOException {
        OAIChatCompletionRequest chatCompletionRequest = pipelineRequest.getCompletionRequest();
        OpenRouterRequestLogger logger = pipelineRequest.getLogger();
        int userId = pipelineRequest.getUserAuthToken().getUserID();

        java.util.List<OAIChatCompletionRequestMessage> finalMessages = new ArrayList<>();
        int originalMessageCount = chatCompletionRequest.getMessages().size();
        int totalImagesFound = 0;
        AtomicInteger totalImagesSent = new AtomicInteger(0);
        int totalImagesFiltered = 0;
        int totalConversationLength = 0;

        boolean isPremium = false;
        boolean premiumStatusChecked = false;

        for (int i = chatCompletionRequest.getMessages().size() - 1; i >= 0; i--) {
            OAIChatCompletionRequestMessage originalMessage = chatCompletionRequest.getMessages().get(i);

            int messageLength = 0;
            int effectiveImageSize = 0;
            int imagesInMessage = 0;

            for (OAIChatCompletionRequestMessageContent contentPart : originalMessage.getContent()) {
                if (contentPart instanceof OAIChatCompletionRequestMessageContentImageURL) {
                    totalImagesFound++;
                    imagesInMessage++;

                    if (!premiumStatusChecked) {
                        isPremium = PremiumStatusCache.getIsPremium(userId, SHARED_EXECUTOR);
                        logger.logPremiumStatus(isPremium, "User " + userId + " has images, premium=" + isPremium);
                        premiumStatusChecked = true;
                    }

                    String originalUrl = ((OAIChatCompletionRequestMessageContentImageURL) contentPart).getImage_url().getUrl();
                    ImageDimensionExtractor.Result dims = ImageDimensionExtractor.extract(originalUrl);

                    int thisImageEffectiveSize;
                    if (dims.isDimensionsKnown()) {
                        int imageTokens = (dims.getWidth() * dims.getHeight()) / IMAGE_TOKEN_DIVISOR;
                        double tokenWeight = isPremium ? IMAGE_TOKEN_WEIGHT_PREMIUM : IMAGE_TOKEN_WEIGHT_FREE;
                        thisImageEffectiveSize = (int) (imageTokens * tokenWeight);
                    } else {
                        double fallbackFactor = isPremium ? IMAGE_FALLBACK_FACTOR_PREMIUM : IMAGE_FALLBACK_FACTOR_FREE;
                        thisImageEffectiveSize = (int) (dims.getUrl().length() * fallbackFactor);
                    }
                    effectiveImageSize += thisImageEffectiveSize;
                } else if (contentPart instanceof OAIChatCompletionRequestMessageContentText) {
                    String text = ((OAIChatCompletionRequestMessageContentText) contentPart).getText();
                    messageLength += text.length();
                }
            }

            messageLength += effectiveImageSize;

            if (totalConversationLength + messageLength > MAX_CONVERSATION_INPUT_LENGTH) {
                if (imagesInMessage > 0) totalImagesFiltered += imagesInMessage;
                break;
            }
            if (finalMessages.size() >= MAX_INPUT_MESSAGES) {
                if (imagesInMessage > 0) totalImagesFiltered += imagesInMessage;
                break;
            }

            totalConversationLength += messageLength;
            finalMessages.add(originalMessage);
            if (imagesInMessage > 0) totalImagesSent.addAndGet(imagesInMessage);
        }

        Collections.reverse(finalMessages);
        chatCompletionRequest.setMessages(finalMessages);

        String requestedModel = chatCompletionRequest.getModel();
        if (requestedModel == null || requestedModel.trim().isEmpty()) {
            chatCompletionRequest.setModel("openai/gpt-5-mini");
        }

        JsonNode requestNode = NON_NULL_MAPPER.valueToTree(chatCompletionRequest);

        if (!pipelineRequest.hasServerFunctionOverride() && requestNode instanceof ObjectNode) {
            pipelineRequest.getPassthroughFields().injectInto((ObjectNode) requestNode);
        }

        String requestJson = NON_NULL_MAPPER.writeValueAsString(requestNode);

        logger.logMessageFiltering(originalMessageCount, finalMessages.size(),
                totalConversationLength, totalImagesFound, totalImagesSent.get());
        logger.log("OUTGOING REQUEST JSON:");
        logger.log(requestJson.substring(0, Math.min(2000, requestJson.length()))
                + (requestJson.length() > 2000 ? "... (truncated)" : ""));
        logger.log("Model: " + chatCompletionRequest.getModel());
        logger.log("Initiating stream request to OpenRouter...");

        return new FilteredRequest(requestJson, chatCompletionRequest.getModel(),
                pipelineRequest.getUserAuthToken(), logger, totalImagesFound, totalImagesSent.get());
    }
}
