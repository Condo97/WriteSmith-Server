package com.writesmith.core.service.endpoints;

import com.writesmith.exceptions.AutoIncrementingDBObjectExistsException;
import com.writesmith.exceptions.DBObjectNotFoundFromQueryException;
import com.writesmith.exceptions.PreparedStatementMissingArgumentException;
import com.writesmith.apple.iapvalidation.ReceiptUpdater;
import com.writesmith.database.dao.pooled.User_AuthTokenDAOPooled;
import com.writesmith.core.service.response.factory.BodyResponseFactory;
import com.writesmith.database.model.objects.Receipt;
import com.writesmith.database.model.objects.User_AuthToken;
import com.writesmith.apple.iapvalidation.networking.itunes.exception.AppleItunesResponseException;
import com.writesmith.core.service.request.RegisterTransactionRequest;
import com.writesmith.core.service.response.BodyResponse;
import com.writesmith.core.service.response.IsPremiumResponse;
import sqlcomponentizer.dbserializer.DBSerializerException;
import sqlcomponentizer.dbserializer.DBSerializerPrimaryKeyMissingException;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.sql.SQLException;

public class ValidateAndUpdateReceiptEndpoint {

    public static BodyResponse validateAndUpdateReceipt(RegisterTransactionRequest registerTransactionRequest) throws DBSerializerException, SQLException, DBObjectNotFoundFromQueryException, InterruptedException, InvocationTargetException, IllegalAccessException, NoSuchMethodException, InstantiationException, DBSerializerPrimaryKeyMissingException, PreparedStatementMissingArgumentException, AppleItunesResponseException, IOException, AutoIncrementingDBObjectExistsException {
        // Get u_aT
        User_AuthToken u_aT = User_AuthTokenDAOPooled.get(registerTransactionRequest.getAuthToken());

        // Create receipt and update if needed
        Receipt receipt = new Receipt(u_aT.getUserID(), registerTransactionRequest.getReceiptString());
        ReceiptUpdater.updateIfNeeded(receipt);

        // Get vpResponse from receipt and return in success body response
        IsPremiumResponse vpResponse = new IsPremiumResponse(!receipt.isExpired());

        return BodyResponseFactory.createSuccessBodyResponse(vpResponse);
    }

}
