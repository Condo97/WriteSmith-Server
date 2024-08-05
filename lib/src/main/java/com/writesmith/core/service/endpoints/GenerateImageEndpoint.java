package com.writesmith.core.service.endpoints;

import com.oaigptconnector.model.exception.OpenAIGPTException;
import com.writesmith.core.service.request.GenerateImageRequest;
import com.writesmith.core.service.response.GenerateImageResponse;
import com.writesmith.database.dao.pooled.User_AuthTokenDAOPooled;
import com.writesmith.database.model.objects.User_AuthToken;
import com.writesmith.exceptions.DBObjectNotFoundFromQueryException;
import com.writesmith.openai.DALLE3ImageGenerator;
import sqlcomponentizer.dbserializer.DBSerializerException;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class GenerateImageEndpoint {

    public static GenerateImageResponse generateImage(GenerateImageRequest request) throws DBSerializerException, SQLException, DBObjectNotFoundFromQueryException, InterruptedException, InvocationTargetException, IllegalAccessException, NoSuchMethodException, InstantiationException, OpenAIGPTException, IOException {
        // Get u_aT from request
        User_AuthToken u_aT = User_AuthTokenDAOPooled.get(request.getAuthToken());

        // Generate image
        DALLE3ImageGenerator.CompletedGeneration completedGeneration = DALLE3ImageGenerator.generate(request.getPrompt());

        // Transpose to GenerateImageResponse and return
        GenerateImageResponse giResponse = new GenerateImageResponse(
                completedGeneration.getB64_json(),
                completedGeneration.getUrl(),
                completedGeneration.getRevised_prompt()
        );

        // Print log
        printLog();

        return giResponse;
    }

    private static void printLog() {
        StringBuilder sb = new StringBuilder();

        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
        Date date = new Date();

        sb.append("Image Created ");
        sb.append(sdf.format(date));

        System.out.println(sb);
    }

}
