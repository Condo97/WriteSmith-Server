package com.writesmith.core.service.generators;

import com.oaigptconnector.model.*;
import com.oaigptconnector.model.exception.OpenAIGPTException;
import com.oaigptconnector.model.generation.OpenAIGPTModels;
import com.oaigptconnector.model.request.chat.completion.CompletionRole;
import com.oaigptconnector.model.request.chat.completion.OAIChatCompletionRequestMessage;
import com.oaigptconnector.model.request.chat.completion.OAIChatCompletionRequestResponseFormat;
import com.oaigptconnector.model.request.chat.completion.ResponseFormatType;
import com.oaigptconnector.model.request.chat.completion.content.InputImageDetail;
import com.oaigptconnector.model.response.chat.completion.http.OAIGPTChatCompletionResponse;
import com.writesmith.Constants;
import com.writesmith.openai.structuredoutput.DrawersSO;
import com.writesmith.keys.Keys;

import java.io.IOException;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.List;

public class DrawerGenerator {

    private static final int MAX_TOKENS = 800;
    private static final int DEFAULT_TEMPERATURE = Constants.DEFAULT_TEMPERATURE;
    private static final String API_KEY = Keys.openAiAPI;

    public static class Drawers {

        public static class Drawer {

            private Integer index;
            private String title;
            private String content;

            public Drawer() {

            }

            public Drawer(Integer index, String title, String content) {
                this.index = index;
                this.title = title;
                this.content = content;
            }

            public Integer getIndex() {
                return index;
            }

            public void setIndex(Integer index) {
                this.index = index;
            }

            public String getTitle() {
                return title;
            }

            public void setTitle(String title) {
                this.title = title;
            }

            public String getContent() {
                return content;
            }

            public void setContent(String content) {
                this.content = content;
            }

        }

        private String title;
        private List<Drawer> drawers;

        public Drawers() {

        }

        public Drawers(String title, List<Drawer> drawers) {
            this.title = title;
            this.drawers = drawers;
        }

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public List<Drawer> getDrawers() {
            return drawers;
        }

        public void setDrawers(List<Drawer> drawers) {
            this.drawers = drawers;
        }

    }

    public static Drawers generateDrawers(String input, String imageData) throws OAISerializerException, JSONSchemaDeserializerException, OpenAIGPTException, IOException, InterruptedException {
        // Create message for GPT
        OAIChatCompletionRequestMessage message = new OAIChatCompletionRequestMessageBuilder(CompletionRole.USER)
                .addText(input)
                .addImage(imageData, InputImageDetail.LOW)
                .build();

        // Create HttpClient
        final HttpClient httpClient = HttpClient.newBuilder().version(HttpClient.Version.HTTP_2).connectTimeout(Duration.ofMinutes(com.oaigptconnector.Constants.AI_TIMEOUT_MINUTES)).build();

        // Get response from FCClient
        OAIGPTChatCompletionResponse response = FCClient.serializedChatCompletion(
                DrawersSO.class,
                OpenAIGPTModels.GPT_4_MINI.getName(),
                MAX_TOKENS,
                DEFAULT_TEMPERATURE,
                new OAIChatCompletionRequestResponseFormat(ResponseFormatType.TEXT),
                API_KEY,
                httpClient,
                Constants.OPENAI_URI,
                message
        );

        // Get responseString from response
        String responseString = response.getChoices()[0].getMessage().getTool_calls().get(0).getFunction().getArguments();

        // Create DrawersFC
        DrawersSO drawersSO = JSONSchemaDeserializer.deserialize(responseString, DrawersSO.class);

        // Transpose drawersFC result to Drawers and return
        Drawers drawers = new Drawers(
                drawersSO.getTitle(),
                drawersSO.getDrawers().stream()
                        .map(d -> new Drawers.Drawer(
                                d.getIndex(),
                                d.getTitle(),
                                d.getContent()
                        ))
                        .toList()
        );

        return drawers;
    }

}
