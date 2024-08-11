package com.writesmith.core.service.endpoints;

import com.oaigptconnector.model.JSONSchemaDeserializerException;
import com.oaigptconnector.model.OAISerializerException;
import com.oaigptconnector.model.exception.OpenAIGPTException;
import com.oaigptconnector.model.request.chat.completion.CompletionRole;
import com.writesmith.core.service.generators.GoogleQueryGenerator;
import com.writesmith.core.service.request.GenerateGoogleQueryRequest;
import com.writesmith.core.service.response.GenerateGoogleQueryResponse;
import com.writesmith.database.dao.pooled.User_AuthTokenDAOPooled;
import com.writesmith.database.model.objects.User_AuthToken;
import com.writesmith.exceptions.DBObjectNotFoundFromQueryException;
import sqlcomponentizer.dbserializer.DBSerializerException;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.sql.SQLException;
import java.util.List;

public class GenerateGoogleQueryEndpoint {

    public static GenerateGoogleQueryResponse generateGoogleQuery(GenerateGoogleQueryRequest request) throws DBSerializerException, SQLException, DBObjectNotFoundFromQueryException, InterruptedException, InvocationTargetException, IllegalAccessException, NoSuchMethodException, InstantiationException, OAISerializerException, JSONSchemaDeserializerException, OpenAIGPTException, IOException {
        // Get u_aT from authToken
        User_AuthToken u_aT = User_AuthTokenDAOPooled.get(request.getAuthToken());

        // Create inputChats from inputs if not null or empty or input otherwise
        List<GoogleQueryGenerator.InputChat> inputChats;
        if (request.getInputs() != null && !request.getInputs().isEmpty()) {
            inputChats = request.getInputs().stream().map(i -> new GoogleQueryGenerator.InputChat(i.getRole(), i.getInput())).toList();
        } else {
            inputChats = List.of(new GoogleQueryGenerator.InputChat(CompletionRole.USER, request.getInput()));
        }

        // Get GoogleQueryGenerator GoogleQuery
        GoogleQueryGenerator.GoogleQuery query = GoogleQueryGenerator.generateGoogleQuery(inputChats);

        // Transpose and return in GenerateGoogleQueryResponse
        return new GenerateGoogleQueryResponse(
                query.getQuery()
        );
    }

}
