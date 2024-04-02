package com.writesmith.core.service.generators;

import com.oaigptconnector.model.*;
import com.oaigptconnector.model.exception.OpenAIGPTException;
import com.oaigptconnector.model.generation.OpenAIGPTModels;
import com.oaigptconnector.model.request.chat.completion.OAIChatCompletionRequestMessage;
import com.oaigptconnector.model.response.chat.completion.http.OAIGPTChatCompletionResponse;
import com.writesmith.Constants;
import com.writesmith.core.gpt_function_calls.CheckIfChatRequestsImageRevisionFC;
import com.writesmith.core.gpt_function_calls.ClassifyChatFC;
import com.writesmith.keys.Keys;

import java.io.IOException;

public class CheckIfChatRequestsImageRevisionGenerator {

    private static final int MAX_TOKENS = 800;
    private static final int DEFAULT_TEMPERATURE = Constants.DEFAULT_TEMPERATURE;
    private static final String API_KEY = Keys.openAiAPI;

    public static class ChatRequestsImageRevision {

        private Boolean requestsImageRevision;

        public ChatRequestsImageRevision() {

        }

        public ChatRequestsImageRevision(Boolean requestsImageRevision) {
            this.requestsImageRevision = requestsImageRevision;
        }

        public Boolean getRequestsImageRevision() {
            return requestsImageRevision;
        }

    }

    public static ChatRequestsImageRevision requestsImageRevision(String chat) throws OAISerializerException, OpenAIGPTException, OAIDeserializerException, IOException, InterruptedException {
        return generateChatRequestsImageRevision(chat);
    }

    private static ChatRequestsImageRevision generateChatRequestsImageRevision(String chat) throws OAISerializerException, OpenAIGPTException, IOException, InterruptedException, OAIDeserializerException {
        // Create message for GPT
        OAIChatCompletionRequestMessage message = new OAIChatCompletionRequestMessageBuilder(CompletionRole.USER)
                .addText(chat)
                .build();

        // Get response from FCClient
        OAIGPTChatCompletionResponse response = FCClient.serializedChatCompletion(
                CheckIfChatRequestsImageRevisionFC.class,
                OpenAIGPTModels.GPT_4.getName(),
                MAX_TOKENS,
                DEFAULT_TEMPERATURE,
                API_KEY,
                message
        );

        // Get responseString from response
        String responseString = response.getChoices()[0].getMessage().getFunction_call().getArguments();

        // Create classifyChatFC
        CheckIfChatRequestsImageRevisionFC checkIfChatRequestsImageRevisionFC = OAIFunctionCallDeserializer.deserialize(responseString, CheckIfChatRequestsImageRevisionFC.class);

        // Transpose classifyChatFC result to ClassifiedChat and return
        ChatRequestsImageRevision chatRequestsImageRevision = new ChatRequestsImageRevision(
                checkIfChatRequestsImageRevisionFC.getRequestsRevision()
        );

        return chatRequestsImageRevision;
    }

}
