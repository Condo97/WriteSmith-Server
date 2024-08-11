package com.writesmith.core.service.endpoints;

import com.oaigptconnector.model.JSONSchemaDeserializerException;
import com.oaigptconnector.model.OAISerializerException;
import com.oaigptconnector.model.exception.OpenAIGPTException;
import com.writesmith.core.service.generators.DrawerGenerator;
import com.writesmith.core.service.request.GenerateDrawersRequest;
import com.writesmith.core.service.response.GenerateDrawersResponse;
import com.writesmith.database.dao.pooled.User_AuthTokenDAOPooled;
import com.writesmith.database.model.objects.User_AuthToken;
import com.writesmith.exceptions.DBObjectNotFoundFromQueryException;
import sqlcomponentizer.dbserializer.DBSerializerException;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class GenerateDrawersEndpoint {

    public static GenerateDrawersResponse generateDrawers(GenerateDrawersRequest request) throws DBSerializerException, SQLException, DBObjectNotFoundFromQueryException, InterruptedException, InvocationTargetException, IllegalAccessException, NoSuchMethodException, InstantiationException, OAISerializerException, JSONSchemaDeserializerException, OpenAIGPTException, IOException {
        // Get u_aT from request
        User_AuthToken u_aT = User_AuthTokenDAOPooled.get(request.getAuthToken());

        // Generate drawers
        DrawerGenerator.Drawers drawers = DrawerGenerator.generateDrawers(request.getInput(), request.getImageData());

        // Transpose to GenerateDrawersResponse and return
        GenerateDrawersResponse response = new GenerateDrawersResponse(
                drawers.getTitle(),
                drawers.getDrawers().stream()
                        .map(d -> new GenerateDrawersResponse.Drawer(
                                d.getIndex(),
                                d.getTitle(),
                                d.getContent()
                        ))
                        .toList()
        );

        // Print log
        printLog();

        return response;
    }

    private static void printLog() {
        StringBuilder sb = new StringBuilder();

        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
        Date date = new Date();

        sb.append("Drawers Created ");
        sb.append(sdf.format(date));

        System.out.println(sb);
    }

}
