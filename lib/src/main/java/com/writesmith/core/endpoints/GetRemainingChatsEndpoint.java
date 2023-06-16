package com.writesmith.core.endpoints;

import com.writesmith.common.exceptions.DBObjectNotFoundFromQueryException;
import com.writesmith.core.generation.calculators.ChatRemainingCalculator;
import com.writesmith.database.managers.ReceiptDBManager;
import com.writesmith.database.managers.User_AuthTokenDBManager;
import com.writesmith.model.database.objects.Receipt;
import com.writesmith.model.database.objects.User_AuthToken;
import com.writesmith.model.http.server.request.AuthRequest;
import com.writesmith.model.http.server.response.BodyResponse;
import com.writesmith.model.http.server.response.GetRemainingResponse;
import sqlcomponentizer.dbserializer.DBSerializerException;

import java.lang.reflect.InvocationTargetException;
import java.sql.SQLException;

public class GetRemainingChatsEndpoint extends Endpoint {

    public static BodyResponse getRemaining(AuthRequest authRequest) throws DBSerializerException, SQLException, DBObjectNotFoundFromQueryException, InterruptedException, IllegalAccessException, InvocationTargetException, NoSuchMethodException, InstantiationException {
        // Get u_aT from authRequest
        User_AuthToken u_aT = User_AuthTokenDBManager.getFromDB(authRequest.getAuthToken());

        // Try to get the most recent receipt
        Receipt receipt;
        try {
            receipt = ReceiptDBManager.getMostRecentReceiptFromDB(u_aT.getUserID());
        } catch (DBObjectNotFoundFromQueryException e) {
            receipt = null;
        }

        // Check most recent receipt if it is premium if it is not null and not expired
        boolean isPremium = receipt != null && !receipt.isExpired();

        // Get remaining from userID and if null set to -1
        Long remaining = ChatRemainingCalculator.calculateRemaining(u_aT.getUserID(), isPremium);
        if (remaining == null)
            remaining = -1l;

        // Build getRemainingResponse
        GetRemainingResponse getRemainingResponse = new GetRemainingResponse(remaining);

        // Build and return bodyResponse with getRemainingResponse and success
        return createSuccessBodyResponse(getRemainingResponse);
        
    }

}
