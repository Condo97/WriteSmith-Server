package com.writesmith;

import com.oaigptconnector.model.exception.OpenAIGPTException;
import com.writesmith.connectionpool.SQLConnectionPoolInstance;
import com.writesmith.core.service.endpoints.GenerateImageEndpoint;
import com.writesmith.core.service.endpoints.RegisterUserEndpoint;
import com.writesmith.core.service.request.GenerateImageRequest;
import com.writesmith.core.service.response.AuthResponse;
import com.writesmith.core.service.response.BodyResponse;
import com.writesmith.core.service.response.GenerateImageResponse;
import com.writesmith.exceptions.AutoIncrementingDBObjectExistsException;
import com.writesmith.exceptions.DBObjectNotFoundFromQueryException;
import com.writesmith.keys.Keys;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import sqlcomponentizer.dbserializer.DBSerializerException;
import sqlcomponentizer.dbserializer.DBSerializerPrimaryKeyMissingException;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.sql.SQLException;

public class ImageGenerationTests {

    private static String authToken = "";

    @BeforeAll
    static void setup() throws DBSerializerPrimaryKeyMissingException, DBSerializerException, SQLException, AutoIncrementingDBObjectExistsException, InterruptedException, InvocationTargetException, IllegalAccessException {
        SQLConnectionPoolInstance.create(Constants.MYSQL_URL, Keys.MYSQL_USER, Keys.MYSQL_PASS, 10);

        // Generate authToken
        BodyResponse br = RegisterUserEndpoint.registerUser();

        authToken = ((AuthResponse)br.getBody()).getAuthToken();
    }

    @Test
    @DisplayName("Test Image Generation")
    void testImageGeneration() throws DBSerializerException, SQLException, OpenAIGPTException, DBObjectNotFoundFromQueryException, IOException, InterruptedException, InvocationTargetException, IllegalAccessException, NoSuchMethodException, InstantiationException {
        final String prompt = "An image of a banana.";

        // Create GenerateImageRequest
        GenerateImageRequest giRequest = new GenerateImageRequest(
                authToken,
                prompt
        );

        // Get GenerateImageResponse from GenerateImageEndpoint
        GenerateImageResponse giResponse = GenerateImageEndpoint.generateImage(giRequest);

        // Print and test
        assert(giResponse != null);
//        assert(giResponse.getImageData() != null);

        System.out.println(giResponse.getImageURL());
        System.out.println(giResponse.getImageData());
        System.out.println(giResponse.getRevisedPrompt());
    }

}
