package com.writesmith.core.service.generators;

import com.oaigptconnector.model.*;
import com.oaigptconnector.model.exception.OpenAIGPTException;
import com.oaigptconnector.model.generation.OpenAIGPTModels;
import com.oaigptconnector.model.request.chat.completion.CompletionRole;
import com.oaigptconnector.model.request.chat.completion.OAIChatCompletionRequestMessage;
import com.oaigptconnector.model.request.chat.completion.OAIChatCompletionRequestResponseFormat;
import com.oaigptconnector.model.request.chat.completion.ResponseFormatType;
import com.oaigptconnector.model.response.chat.completion.http.OAIGPTChatCompletionResponse;
import com.writesmith.Constants;
import com.writesmith.openai.structuredoutput.GenerateSuggestionsSO;
import com.writesmith.keys.Keys;

import java.io.IOException;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.List;
import java.util.stream.Collectors;

public class SuggestionsGenerator {

    private static final int MAX_TOKENS = 800;
    private static final int DEFAULT_TEMPERATURE = Constants.DEFAULT_TEMPERATURE;
    private static final String API_KEY = Keys.openAiAPI;

    private static final String SUGGESTION_COUNT_PREFIX = "Make";
    private static final String SUGGESTION_COUNT_SUFFIX = "suggestions.";
    private static final String DIFFERENT_THAN_PROMPT_PREFIX = "Make the suggestions different than:";


    /***
     * Generates an array of Suggestions for the user's input text
     */

    public static class Suggestion {

        private String suggestion;

        public Suggestion() {

        }

        public Suggestion(String suggestion) {
            this.suggestion = suggestion;
        }

        public String getSuggestion() {
            return suggestion;
        }


        @Override
        public String toString() {
            return "Suggestion{" +
                    "suggestion='" + suggestion + '\'' +
                    '}';
        }

    }

    public static List<Suggestion> generateSuggestions(Integer count, List<String> conversation) throws OAISerializerException, OpenAIGPTException, JSONSchemaDeserializerException, IOException, InterruptedException {
        return generateSuggestions(count, conversation, (List<String>)null);
    }

    public static List<Suggestion> generateSuggestions(Integer count, String conversation, String differentThan) throws OAISerializerException, OpenAIGPTException, JSONSchemaDeserializerException, IOException, InterruptedException {
        return generateSuggestions(count, List.of(conversation), List.of(differentThan));
    }

    public static List<Suggestion> generateSuggestions(Integer count, List<String> conversation, String differentThan) throws OAISerializerException, OpenAIGPTException, JSONSchemaDeserializerException, IOException, InterruptedException {
        return generateSuggestions(count, conversation, List.of(differentThan));
    }

    public static List<Suggestion> generateSuggestions(Integer count, String conversation, List<String> differentThan) throws OAISerializerException, OpenAIGPTException, JSONSchemaDeserializerException, IOException, InterruptedException {
        return generateSuggestions(count, List.of(conversation), differentThan);
    }

    public static List<Suggestion> generateSuggestions(Integer count, List<String> conversation, List<String> differentThan) throws OAISerializerException, OpenAIGPTException, JSONSchemaDeserializerException, IOException, InterruptedException {
        // Create prompt String
        String prompt = "";

        // Set count to 1 if null equal to or less than zero
        if (count == null || count <= 0) {
            count = 1;
        }

        // Append suggestion count prefix and suffix with count between to prompt
        prompt += SUGGESTION_COUNT_PREFIX;
        prompt += " ";
        prompt += count;
        prompt += " ";
        prompt += SUGGESTION_COUNT_SUFFIX;
        prompt += "\n";

        // Append conversation as a String separated by commas
        prompt += conversation.stream()
                .collect(Collectors.joining(", "));

        // If differentThan is not null nor empty, append as a String separated by commas
        if (differentThan != null && differentThan.size() > 0) {
            // Append differentThan as a String separated by commas
            prompt += "\n";
            prompt += DIFFERENT_THAN_PROMPT_PREFIX;
            prompt += " ";
            prompt += differentThan.stream()
                    .collect(Collectors.joining(", "));
        }

        return generateSuggestions(prompt);
    }

    public static List<Suggestion> generateSuggestions(String prompt) throws OAISerializerException, OpenAIGPTException, JSONSchemaDeserializerException, IOException, InterruptedException {
        // Create message for GPT
        OAIChatCompletionRequestMessage message = new OAIChatCompletionRequestMessageBuilder(CompletionRole.USER)
                .addText(prompt)
                .build();

        // Create HttpClient
        final HttpClient httpClient = HttpClient.newBuilder().version(HttpClient.Version.HTTP_2).connectTimeout(Duration.ofMinutes(com.oaigptconnector.Constants.AI_TIMEOUT_MINUTES)).build();

        // Get response from FCClient
        OAIGPTChatCompletionResponse response = FCClient.serializedChatCompletion(
                GenerateSuggestionsSO.class,
                OpenAIGPTModels.GPT_4_MINI.getName(),
                MAX_TOKENS,
                DEFAULT_TEMPERATURE,
                "minimal",
                new OAIChatCompletionRequestResponseFormat(ResponseFormatType.TEXT),
                API_KEY,
                httpClient,
                Constants.OPENAI_URI,
                message
        );

        // Get responseString from response
        String responseString = response.getChoices()[0].getMessage().getTool_calls().get(0).getFunction().getArguments();

        // Create generateSuggestionsFC
        GenerateSuggestionsSO generateSuggestionsSO = JSONSchemaDeserializer.deserialize(responseString, GenerateSuggestionsSO.class);

        // Transpose getSuggestionsFC result to Suggestion list
        List<Suggestion> suggestions = generateSuggestionsSO.getSuggestions().stream()
                .map(Suggestion::new)
                .toList();

        // Return Suggestion list
        return suggestions;
    }

}
