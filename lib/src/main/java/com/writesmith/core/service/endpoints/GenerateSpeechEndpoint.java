package com.writesmith.core.service.endpoints;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.oaigptconnector.model.generation.OpenAIGPTModels;
import com.writesmith.Constants;
import com.writesmith.core.service.request.GenerateSpeechRequest;
import com.writesmith.exceptions.responsestatus.MalformedJSONException;
import com.writesmith.keys.Keys;
import com.writesmith.openai.SpeechGenerationModels;
import com.writesmith.openai.SpeechGenerationRequest;
import com.writesmith.openai.SpeechGenerator;
import spark.Request;
import spark.Response;

import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

public class GenerateSpeechEndpoint {

    public static HttpServletResponse generateSpeech(Request req, Response res) throws IOException, MalformedJSONException, InterruptedException {
        // Get GenerateSpeechRequest
        GenerateSpeechRequest gsRequest;

        try {
            gsRequest = new ObjectMapper().readValue(req.body(), GenerateSpeechRequest.class);
        } catch (JsonMappingException | JsonParseException e) {
            System.out.println("Exception when reading GenerateSpeechRequest in GenerateSpeechEndpoint.. The request: " + req.body());
            e.printStackTrace();
            throw new MalformedJSONException("Malformed JSON - " + e.getMessage()); //TODO: This can just be replaced with JsonMappingException and/or JsonParseException lmao
        }

        // Create SpeechGenerationRequest
        SpeechGenerationRequest speechGenerationRequest = new SpeechGenerationRequest(
                SpeechGenerationModels.TTS.getId(),
                gsRequest.getInput(),
                gsRequest.getVoice(),
                gsRequest.getResponseFormat(),
                gsRequest.getSpeed()
        );

        // Generate speech
        byte[] generatedSpeech = SpeechGenerator.postSpeechGeneration(
                speechGenerationRequest,
                Keys.openAiAPI
        );

        // Write the byte array directly to the response output stream
        InputStream inputStream = new ByteArrayInputStream(generatedSpeech);
        byte[] buffer = new byte[1024];
        int bytesRead;
        while ((bytesRead = inputStream.read(buffer)) != -1) {
            res.raw().getOutputStream().write(buffer, bytesRead, bytesRead);
        }
        inputStream.close();

        return res.raw();
    }

}
