package com.writesmith.core.service.generators;

import com.oaigptconnector.model.*;
import com.oaigptconnector.model.exception.OpenAIGPTException;
import com.oaigptconnector.model.generation.OpenAIGPTModels;
import com.oaigptconnector.model.request.chat.completion.OAIChatCompletionRequestMessage;
import com.oaigptconnector.model.response.chat.completion.http.OAIGPTChatCompletionResponse;
import com.writesmith.Constants;
import com.writesmith.core.gpt_function_calls.ClassifyChatFC;
import com.writesmith.keys.Keys;

import java.io.IOException;
import java.net.http.HttpClient;
import java.time.Duration;

public class ClassifyChatGenerator {

    private static final int MAX_TOKENS = 800;
    private static final int DEFAULT_TEMPERATURE = Constants.DEFAULT_TEMPERATURE;
    private static final String API_KEY = Keys.openAiAPI;
    
    public static class ClassifiedChat {
        
        private Boolean wantsImageGeneration;

        public ClassifiedChat() {
            
        }

        public ClassifiedChat(Boolean wantsImageGeneration) {
            this.wantsImageGeneration = wantsImageGeneration;
        }

        public Boolean getWantsImageGeneration() {
            return wantsImageGeneration;
        }
        
    }

    public static ClassifiedChat classifyChat(String chat) throws OAISerializerException, OpenAIGPTException, OAIDeserializerException, IOException, InterruptedException {
        return generateClassifiedChat(chat);
    }
    
    private static ClassifiedChat generateClassifiedChat(String chat) throws OAISerializerException, OpenAIGPTException, IOException, InterruptedException, OAIDeserializerException {
        // Create message for GPT
        OAIChatCompletionRequestMessage message = new OAIChatCompletionRequestMessageBuilder(CompletionRole.USER)
                .addText(chat)
                .build();

        // Create HttpClient
        final HttpClient httpClient = HttpClient.newBuilder().version(HttpClient.Version.HTTP_2).connectTimeout(Duration.ofMinutes(com.oaigptconnector.Constants.AI_TIMEOUT_MINUTES)).build();

        // Get response from FCClient
        OAIGPTChatCompletionResponse response = FCClient.serializedChatCompletion(
                ClassifyChatFC.class,
                OpenAIGPTModels.GPT_3_5_TURBO.getName(),
                MAX_TOKENS,
                DEFAULT_TEMPERATURE,
                API_KEY,
                httpClient,
                message
        );

        // Get responseString from response
        String responseString = response.getChoices()[0].getMessage().getFunction_call().getArguments();

        // Create classifyChatFC
        ClassifyChatFC classifyChatFC = OAIFunctionCallDeserializer.deserialize(responseString, ClassifyChatFC.class);

        // Transpose classifyChatFC result to ClassifiedChat and return
        ClassifiedChat classifiedChat = new ClassifiedChat(
                classifyChatFC.getWantsImageGeneration()
        );

        return classifiedChat;
    }
    
}
