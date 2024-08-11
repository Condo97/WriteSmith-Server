package com.writesmith.core.service.endpoints;

import com.oaigptconnector.model.JSONSchemaDeserializerException;
import com.oaigptconnector.model.OAISerializerException;
import com.oaigptconnector.model.exception.OpenAIGPTException;
import com.writesmith.core.service.generators.CheckIfChatRequestsImageRevisionGenerator;
import com.writesmith.core.service.request.CheckIfChatRequestsImageRevisionRequest;
import com.writesmith.core.service.response.CheckIfChatRequestsImageRevisionResponse;
import com.writesmith.database.dao.pooled.User_AuthTokenDAOPooled;
import com.writesmith.database.model.objects.User_AuthToken;
import com.writesmith.exceptions.DBObjectNotFoundFromQueryException;
import sqlcomponentizer.dbserializer.DBSerializerException;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.sql.SQLException;

public class CheckIfChatRequestsImageRevisionEndpoint {

    public static CheckIfChatRequestsImageRevisionResponse checkIfChatRequestsImageRevision(CheckIfChatRequestsImageRevisionRequest request) throws DBSerializerException, SQLException, DBObjectNotFoundFromQueryException, InterruptedException, InvocationTargetException, IllegalAccessException, NoSuchMethodException, InstantiationException, OAISerializerException, JSONSchemaDeserializerException, OpenAIGPTException, IOException {
        // Get u_aT from authRequest
        User_AuthToken u_aT = User_AuthTokenDAOPooled.get(request.getAuthToken());

        // Check if chat requests image revision
        CheckIfChatRequestsImageRevisionGenerator.ChatRequestsImageRevision chatRequestsImageRevision = CheckIfChatRequestsImageRevisionGenerator.requestsImageRevision(request.getChat());

        // Transpose to CheckIfChatRequestsImageRevisionResponse and return
        CheckIfChatRequestsImageRevisionResponse cicrirResponse = new CheckIfChatRequestsImageRevisionResponse(
                chatRequestsImageRevision.getRequestsImageRevision()
        );

        return cicrirResponse;
    }

}
