package com.writesmith.openai;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.oaigptconnector.Constants;
import com.oaigptconnector.model.exception.OpenAIGPTException;
import com.oaigptconnector.model.response.error.OpenAIGPTErrorResponse;
import httpson.Httpson;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.entity.mime.HttpMultipartMode;
import org.apache.hc.client5.http.entity.mime.MultipartEntityBuilder;
import org.apache.hc.client5.http.entity.mime.StringBody;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.ProtocolVersion;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.function.Consumer;

import static spark.route.HttpMethod.post;

public class SpeechTranscriber {

    private static final HttpClient client = HttpClient.newBuilder().version(HttpClient.Version.HTTP_2).connectTimeout(Duration.ofMinutes(Constants.AI_TIMEOUT_MINUTES)).build();

    public static final String modelName = "whisper-1";

    public static class CompletedTranscription {

        private String text;

        public CompletedTranscription() {

        }

        public CompletedTranscription(String text) {
            this.text = text;
        }

        public String getText() {
            return text;
        }

    }


    public static CompletedTranscription transcribe(String audioFileName, byte[] audioFile, String prompt, String apiKey) throws OpenAIGPTException, IOException, InterruptedException {
        // Create speech transcription request
        SpeechTranscriptionRequest request = new SpeechTranscriptionRequest(
                audioFileName,
                audioFile,
                modelName,
                prompt
        );

        // Get speech transcription response
        SpeechTranscriptionResponse response = getTranscription(request, apiKey);

        // Return new CompletedTranscription from response
        return new CompletedTranscription(
                response.getText()
        );
    }


    private static SpeechTranscriptionResponse getTranscription(SpeechTranscriptionRequest requestObject, String apiKey) throws IOException, InterruptedException, OpenAIGPTException {
//        Consumer<HttpRequest.Builder> c = (requestBuilder) -> {
//            requestBuilder.setHeader("Authorization", "Bearer " + apiKey);
//        };

        // Create httpClient i guess lol
        CloseableHttpClient httpClient = HttpClients.createDefault();

        // Create multipart entity builder with model and file
        MultipartEntityBuilder builder = MultipartEntityBuilder.create();
        builder.addTextBody("model", "whisper-1", ContentType.TEXT_PLAIN);
        builder.addBinaryBody(
                "file",
                requestObject.getFile(),
                ContentType.DEFAULT_BINARY,
                requestObject.getFilename()
        );

        // Build multipart entity
        HttpEntity multipart = builder.build();

        // Create post request, set authorization header, and multipartEntity
        HttpPost httpPost = new HttpPost(Constants.OPENAI_SPEECH_TRANSCRIPTION_URI);

        httpPost.setHeader("Authorization", "Bearer " + apiKey);

        httpPost.setEntity(multipart);

        // Get closable http response by executing with httpclient
        CloseableHttpResponse response = httpClient.execute(httpPost);

        // Set response entity to response entity lol
        HttpEntity responseEntity = response.getEntity();

        // Get response JSON from responseEntity content
        JsonNode responseJson = new ObjectMapper().readValue(new String(responseEntity.getContent().readAllBytes()), JsonNode.class);

        try {
            SpeechTranscriptionResponse speechTranscriptionResponse = new ObjectMapper().treeToValue(responseJson, SpeechTranscriptionResponse.class);

            System.out.println(response);

            if (speechTranscriptionResponse == null) {
                // TODO: Handle Errors
                System.out.println("Got null response from server when requesting speech transcription in SpeechTranscriber!");
                throw new IOException("Got null response from server when requesting speech transcription.");
            }

            return speechTranscriptionResponse;
        } catch (JsonProcessingException e) {
            System.out.println("Issue Mapping SpeechTranscriptionResponse " + response);

            throw new OpenAIGPTException(e, new ObjectMapper().treeToValue(responseJson, OpenAIGPTErrorResponse.class)); // TODO: Okay to throw OpenAIGPTException here even though its using speech generator from open ai not gpt?
        }
    }

}
