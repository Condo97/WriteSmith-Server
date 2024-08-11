package com.writesmith.core.service.endpoints;

import com.oaigptconnector.model.JSONSchemaDeserializerException;
import com.oaigptconnector.model.OAISerializerException;
import com.oaigptconnector.model.exception.OpenAIGPTException;
import com.writesmith.core.service.generators.TitleGenerator;
import com.writesmith.core.service.request.GenerateTitleRequest;
import com.writesmith.core.service.response.GenerateTitleResponse;
import com.writesmith.database.dao.pooled.User_AuthTokenDAOPooled;
import com.writesmith.database.model.objects.User_AuthToken;
import com.writesmith.exceptions.DBObjectNotFoundFromQueryException;
import sqlcomponentizer.dbserializer.DBSerializerException;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.sql.SQLException;

public class GenerateTitleEndpoint {

    public static GenerateTitleResponse generateTitle(GenerateTitleRequest request) throws DBSerializerException, SQLException, DBObjectNotFoundFromQueryException, InterruptedException, InvocationTargetException, IllegalAccessException, NoSuchMethodException, InstantiationException, OAISerializerException, JSONSchemaDeserializerException, OpenAIGPTException, IOException {
        // Get u_aT from authRequest
        User_AuthToken u_aT = User_AuthTokenDAOPooled.get(request.getAuthToken());

        // Generate title
        TitleGenerator.Title title = TitleGenerator.generateTitle(request.getInput(), request.getImageData());

        // Create GenerateTitleResponse and return
        GenerateTitleResponse gtResponse = new GenerateTitleResponse(title.getTitle());

        return gtResponse;
    }

}
