package com.writesmith.core.service.endpoints;

import com.writesmith.core.service.request.APNSRegistrationRequest;
import com.writesmith.core.service.response.StatusResponse;
import com.writesmith.core.service.response.factory.StatusResponseFactory;
import com.writesmith.database.dao.APNSRegistrationDAO;
import com.writesmith.database.dao.pooled.APNSRegistrationDAOPooled;
import com.writesmith.database.dao.pooled.User_AuthTokenDAOPooled;
import com.writesmith.database.model.objects.APNSRegistration;
import com.writesmith.database.model.objects.User_AuthToken;
import com.writesmith.exceptions.DBObjectNotFoundFromQueryException;
import sqlcomponentizer.dbserializer.DBSerializerException;
import sqlcomponentizer.dbserializer.DBSerializerPrimaryKeyMissingException;

import java.lang.reflect.InvocationTargetException;
import java.sql.SQLException;
import java.time.LocalDateTime;

public class APNSRegistrationEndpoint {

    public static StatusResponse registerAPNS(APNSRegistrationRequest request) throws DBSerializerException, SQLException, DBObjectNotFoundFromQueryException, InterruptedException, InvocationTargetException, IllegalAccessException, NoSuchMethodException, InstantiationException, DBSerializerPrimaryKeyMissingException {
        // Get u_aT from request
        User_AuthToken u_aT = User_AuthTokenDAOPooled.get(request.getAuthToken());

        // Update APNS TODO: Move to a different class lol
        updateOrInsertAPNSRegistration(u_aT.getUserID(), request.getDeviceID());

        // Return Success StatusResponse
        return StatusResponseFactory.createSuccessStatusResponse();
    }

    private static void updateOrInsertAPNSRegistration(Integer userID, String deviceID) throws DBSerializerException, SQLException, InterruptedException, InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException, DBSerializerPrimaryKeyMissingException {
        // Get latest APNS Registration for userID.. if it exists update it, otherwise create one
        APNSRegistration currentRegistration = APNSRegistrationDAOPooled.getLatestUpdateDateByUserID(userID);

        if (currentRegistration != null) {
            // Update deviceID and updateDate locally and update in DB
            currentRegistration.setDeviceID(deviceID);
            currentRegistration.setUpdateDate(LocalDateTime.now());

            APNSRegistrationDAOPooled.updateDeviceID(currentRegistration);
            APNSRegistrationDAOPooled.updateUpdateDate(currentRegistration);
        } else {
            // Create locally and in DB TODO: This should be moved to a factory method I think lol
            APNSRegistration registration = new APNSRegistration(
                    null,
                    userID,
                    deviceID,
                    LocalDateTime.now(),
                    LocalDateTime.now()
            );

            APNSRegistrationDAOPooled.insert(registration);
        }
    }

}
