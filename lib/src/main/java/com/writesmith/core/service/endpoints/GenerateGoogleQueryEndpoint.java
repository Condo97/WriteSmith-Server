package com.writesmith.core.service.endpoints;

import com.oaigptconnector.model.OAIDeserializerException;
import com.oaigptconnector.model.OAISerializerException;
import com.oaigptconnector.model.exception.OpenAIGPTException;
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

public class GenerateGoogleQueryEndpoint {

    public static GenerateGoogleQueryResponse generateGoogleQuery(GenerateGoogleQueryRequest request) throws DBSerializerException, SQLException, DBObjectNotFoundFromQueryException, InterruptedException, InvocationTargetException, IllegalAccessException, NoSuchMethodException, InstantiationException, OAISerializerException, OpenAIGPTException, OAIDeserializerException, IOException {
        // Get u_aT from authToken
        User_AuthToken u_aT = User_AuthTokenDAOPooled.get(request.getAuthToken());

        // Get GoogleQueryGenerator GoogleQuery
        GoogleQueryGenerator.GoogleQuery query = GoogleQueryGenerator.generateGoogleQuery(request.getInput());

        // Transpose and return in GenerateGoogleQueryResponse
        return new GenerateGoogleQueryResponse(
                query.getQuery()
        );
    }

}
