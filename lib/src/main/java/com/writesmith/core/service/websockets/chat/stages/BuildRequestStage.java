package com.writesmith.core.service.websockets.chat.stages;

import com.oaigptconnector.model.FCJSONSchemaSerializer;
import com.oaigptconnector.model.JSONSchemaSerializer;
import com.oaigptconnector.model.OAISerializerException;
import com.oaigptconnector.model.request.chat.completion.*;
import com.oaigptconnector.model.request.chat.completion.content.OAIChatCompletionRequestMessageContent;
import com.oaigptconnector.model.request.chat.completion.content.OAIChatCompletionRequestMessageContentImageURL;
import com.oaigptconnector.model.request.chat.completion.content.OAIChatCompletionRequestMessageContentText;
import com.oaigptconnector.model.OAIChatCompletionRequestMessageBuilder;
import com.writesmith.core.service.websockets.chat.model.AuthResult;
import com.writesmith.core.service.websockets.chat.model.ChatPipelineRequest;
import com.writesmith.util.OpenRouterRequestLogger;

import java.util.List;

public class BuildRequestStage {

    private static final String PERSISTENT_SYSTEM_TEXT =
            "You have a personality that mirrors and fits the user and you learn over time. " +
            "You have the capability to see images when a user sends one. Ensure that the user knows " +
            "that they can send a picture to get visual help and that GPT can see. You have the capability " +
            "to read websites and PDFs and their text will be supplied if included. If relevant inform the " +
            "user they can ask to search the web in chat, and as relevant let them know it uses Google. " +
            "Attempt to improve socialness and get the user to share and promote the app in subtle ways.";

    public ChatPipelineRequest execute(AuthResult authResult) throws OAISerializerException {
        OpenRouterRequestLogger logger = authResult.getLogger();
        OAIChatCompletionRequest chatCompletionRequest = authResult.getRequest().getChatCompletionRequest();

        boolean hasImages = chatCompletionRequest.getMessages().stream()
                .flatMap(m -> m.getContent().stream())
                .anyMatch(c -> c instanceof OAIChatCompletionRequestMessageContentImageURL);
        logger.logParsedRequest(
                chatCompletionRequest.getModel(),
                chatCompletionRequest.getMessages().size(),
                hasImages,
                authResult.getRequest().getFunction() != null
        );

        OAIChatCompletionRequestStreamOptions streamOptions = chatCompletionRequest.getStream_options();
        if (streamOptions == null)
            streamOptions = new OAIChatCompletionRequestStreamOptions(true);
        else
            streamOptions.setInclude_usage(true);
        chatCompletionRequest.setStream_options(new OAIChatCompletionRequestStreamOptions(true));

        boolean serverFunctionOverride = false;

        boolean hasClientTools = chatCompletionRequest.getTools() != null && !chatCompletionRequest.getTools().isEmpty();
        boolean hasClientResponseFormat = chatCompletionRequest.getResponse_format() != null;
        if (hasClientTools || hasClientResponseFormat) {
            logger.log("[PASSTHROUGH] Client provided: " +
                    (hasClientTools ? "tools=" + chatCompletionRequest.getTools().size() + " " : "") +
                    (hasClientResponseFormat ? "response_format=yes" : ""));
        }

        if (authResult.getRequest().getFunction() != null && authResult.getRequest().getFunction().getJSONSchemaClass() != null) {
            serverFunctionOverride = true;
            logger.log("[FUNCTION] Using server-side function: " + authResult.getRequest().getFunction().getName());
            Object serializedFCObject = FCJSONSchemaSerializer.objectify(authResult.getRequest().getFunction().getJSONSchemaClass());
            String fcName = JSONSchemaSerializer.getFunctionName(authResult.getRequest().getFunction().getJSONSchemaClass());
            OAIChatCompletionRequestToolChoiceFunction.Function requestToolChoiceFunction =
                    new OAIChatCompletionRequestToolChoiceFunction.Function(fcName);
            OAIChatCompletionRequestToolChoiceFunction requestToolChoice =
                    new OAIChatCompletionRequestToolChoiceFunction(requestToolChoiceFunction);
            chatCompletionRequest.setTools(List.of(new OAIChatCompletionRequestTool(
                    OAIChatCompletionRequestToolType.FUNCTION,
                    serializedFCObject
            )));
            chatCompletionRequest.setTool_choice(requestToolChoice);
        }

        appendPersistentSystemText(chatCompletionRequest);

        return new ChatPipelineRequest(chatCompletionRequest, authResult.getUserAuthToken(),
                logger, authResult.getPassthroughFields(), serverFunctionOverride);
    }

    private void appendPersistentSystemText(OAIChatCompletionRequest request) {
        boolean found = false;
        for (OAIChatCompletionRequestMessage msg : request.getMessages()) {
            if (msg.getRole() == CompletionRole.SYSTEM) {
                for (OAIChatCompletionRequestMessageContent content : msg.getContent()) {
                    if (content instanceof OAIChatCompletionRequestMessageContentText) {
                        OAIChatCompletionRequestMessageContentText textContent =
                                (OAIChatCompletionRequestMessageContentText) content;
                        textContent.setText(PERSISTENT_SYSTEM_TEXT + "\n" + textContent.getText());
                        found = true;
                    }
                }
            }
        }
        if (!found) {
            request.getMessages().add(
                    new OAIChatCompletionRequestMessageBuilder(CompletionRole.SYSTEM)
                            .addText(PERSISTENT_SYSTEM_TEXT)
                            .build()
            );
        }
    }
}
