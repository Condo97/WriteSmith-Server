package com.writesmith.core.service.endpoints;

import com.oaigptconnector.model.OAIDeserializerException;
import com.oaigptconnector.model.OAISerializerException;
import com.oaigptconnector.model.exception.OpenAIGPTException;
import com.writesmith.core.service.generators.ClassifyChatGenerator;
import com.writesmith.core.service.request.ClassifyChatRequest;
import com.writesmith.core.service.response.ClassifyChatResponse;
import com.writesmith.database.dao.pooled.User_AuthTokenDAOPooled;
import com.writesmith.database.model.objects.User_AuthToken;
import com.writesmith.exceptions.DBObjectNotFoundFromQueryException;
import sqlcomponentizer.dbserializer.DBSerializerException;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.sql.SQLException;

public class ClassifyChatEndpoint {

    public static ClassifyChatResponse classifyChat(ClassifyChatRequest request) throws DBSerializerException, SQLException, DBObjectNotFoundFromQueryException, InterruptedException, InvocationTargetException, IllegalAccessException, NoSuchMethodException, InstantiationException, OAISerializerException, OpenAIGPTException, OAIDeserializerException, IOException {
        // Get u_aT from authRequest
        User_AuthToken u_aT = User_AuthTokenDAOPooled.get(request.getAuthToken());

        // Classify chat
        ClassifyChatGenerator.ClassifiedChat classifiedChat = ClassifyChatGenerator.classifyChat(request.getChat());

        // Transpose to ClassifyChatResponse and return
        ClassifyChatResponse ccResponse = new ClassifyChatResponse(
                classifiedChat.getWantsImageGeneration(),
                classifiedChat.getWantsWebSearch()
        );

        return ccResponse;
    }

}
