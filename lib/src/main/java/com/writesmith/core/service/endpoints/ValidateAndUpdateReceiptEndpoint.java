package com.writesmith.core.service.endpoints;

import com.writesmith.common.exceptions.AutoIncrementingDBObjectExistsException;
import com.writesmith.common.exceptions.DBObjectNotFoundFromQueryException;
import com.writesmith.common.exceptions.PreparedStatementMissingArgumentException;
import com.writesmith.core.apple.iapvalidation.ReceiptUpdater;
import com.writesmith.core.service.BodyResponseFactory;
import com.writesmith.core.database.ws.managers.User_AuthTokenDBManager;
import com.writesmith.model.database.objects.Receipt;
import com.writesmith.model.database.objects.User_AuthToken;
import com.writesmith.model.http.client.apple.itunes.exception.AppleItunesResponseException;
import com.writesmith.model.http.server.request.RegisterTransactionRequest;
import com.writesmith.model.http.server.response.BodyResponse;
import com.writesmith.model.http.server.response.IsPremiumResponse;
import sqlcomponentizer.dbserializer.DBSerializerException;
import sqlcomponentizer.dbserializer.DBSerializerPrimaryKeyMissingException;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.sql.SQLException;

public class ValidateAndUpdateReceiptEndpoint {

    public static BodyResponse validateAndUpdateReceipt(RegisterTransactionRequest registerTransactionRequest) throws DBSerializerException, SQLException, DBObjectNotFoundFromQueryException, InterruptedException, InvocationTargetException, IllegalAccessException, NoSuchMethodException, InstantiationException, DBSerializerPrimaryKeyMissingException, PreparedStatementMissingArgumentException, AppleItunesResponseException, IOException, AutoIncrementingDBObjectExistsException {
        // Get u_aT
        User_AuthToken u_aT = User_AuthTokenDBManager.getFromDB(registerTransactionRequest.getAuthToken());

        // Create receipt and update if needed
        Receipt receipt = new Receipt(u_aT.getUserID(), registerTransactionRequest.getReceiptString());
        ReceiptUpdater.updateIfNeeded(receipt);

        // Get vpResponse from receipt and return in success body response
        IsPremiumResponse vpResponse = new IsPremiumResponse(!receipt.isExpired());

        return BodyResponseFactory.createSuccessBodyResponse(vpResponse);
    }

}
