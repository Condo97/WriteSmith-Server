package com.writesmith.core.service.endpoints;

import com.writesmith.core.service.request.PrintToConsoleRequest;
import com.writesmith.core.service.response.StatusResponse;
import com.writesmith.core.service.response.factory.StatusResponseFactory;
import com.writesmith.database.dao.pooled.User_AuthTokenDAOPooled;
import com.writesmith.database.model.objects.User_AuthToken;
import com.writesmith.exceptions.DBObjectNotFoundFromQueryException;
import sqlcomponentizer.dbserializer.DBSerializerException;

import java.lang.reflect.InvocationTargetException;
import java.sql.SQLException;

public class PrintToConsoleEndpoint {

    public static StatusResponse printToConsole(PrintToConsoleRequest request) {
        // Get u_aT from authToken
        try {
            User_AuthToken u_aT = User_AuthTokenDAOPooled.get(request.getAuthToken());

            System.out.print("User " + u_aT.getUserID() + " ");
        } catch (DBSerializerException | SQLException | DBObjectNotFoundFromQueryException | InterruptedException |
                 InvocationTargetException | IllegalAccessException | NoSuchMethodException | InstantiationException e) {

        }

        System.out.println("Printed: " + request.getMessage());

        return StatusResponseFactory.createSuccessStatusResponse();
    }

}
