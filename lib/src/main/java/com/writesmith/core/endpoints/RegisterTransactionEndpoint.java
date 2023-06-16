package com.writesmith.core.endpoints;

import com.writesmith.common.exceptions.DBObjectNotFoundFromQueryException;
import com.writesmith.core.apple.iapvalidation.AppleTransactionUpdater;
import com.writesmith.core.apple.iapvalidation.TransactionPersistentAppleUpdater;
import com.writesmith.database.managers.User_AuthTokenDBManager;
import com.writesmith.model.database.AppStoreSubscriptionStatusToIsPremiumAdapter;
import com.writesmith.model.database.objects.Transaction;
import com.writesmith.model.database.objects.User_AuthToken;
import com.writesmith.model.http.client.apple.itunes.exception.AppStoreStatusResponseException;
import com.writesmith.model.http.server.ResponseStatus;
import com.writesmith.model.http.server.request.RegisterTransactionRequest;
import com.writesmith.model.http.server.response.BodyResponse;
import com.writesmith.model.http.server.response.FullValidatePremiumResponse;
import sqlcomponentizer.dbserializer.DBSerializerException;
import sqlcomponentizer.dbserializer.DBSerializerPrimaryKeyMissingException;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URISyntaxException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.security.spec.InvalidKeySpecException;
import java.sql.SQLException;

public class RegisterTransactionEndpoint extends Endpoint {

    public static BodyResponse registerTransaction(RegisterTransactionRequest registerTransactionRequest) throws DBSerializerException, SQLException, DBObjectNotFoundFromQueryException, InterruptedException, InvocationTargetException, IllegalAccessException, NoSuchMethodException, InstantiationException, UnrecoverableKeyException, CertificateException, IOException, URISyntaxException, KeyStoreException, NoSuchAlgorithmException, InvalidKeySpecException, DBSerializerPrimaryKeyMissingException, AppStoreStatusResponseException {
        // Get the user_authToken object to get the user id
        User_AuthToken u_aT = User_AuthTokenDBManager.getFromDB(registerTransactionRequest.getAuthToken());

        // Create transaction with now record date
        Transaction transaction = Transaction.withNowRecordDate(u_aT.getUserID(), registerTransactionRequest.getTransactionId());

        // Update transaction with Apple status
        TransactionPersistentAppleUpdater.updateAndSaveAppleTransactionStatus(transaction);

        // Get isPremium
        boolean isPremium = AppStoreSubscriptionStatusToIsPremiumAdapter.getIsPremium(transaction.getStatus());

        // Create full validate premium response
        FullValidatePremiumResponse fvpr = new FullValidatePremiumResponse(isPremium);

        // Create and return successful body response with full validate premium response
        return createSuccessBodyResponse(fvpr);

//        try {
//
//            // Update the transaction if needed
//            Transaction transaction = TransactionPersistentAppleUpdater.getAppleValidatedTransaction(registerTransactionRequest.getTransactionId(), u_aT.getUserID());
//
//            // Get isPremium from transaction status using adapter
//            Boolean isPremium = AppStoreSubscriptionStatusToIsPremiumAdapter.getIsPremium(transaction.getStatus());
//
//            // Create fullValidatePremiumResponse
//            FullValidatePremiumResponse fullValidatePremiumResponse = new FullValidatePremiumResponse(isPremium);
//
//            System.out.println("Full Validate Premium Response: " + fullValidatePremiumResponse);
//
//            // Create and return bodyResponse with fullValidateResponse
//            return createSuccessBodyResponse(fullValidatePremiumResponse);
//        } catch (AppStoreStatusResponseException e) {
//            return createBodyResponse(ResponseStatus.INVALID_APPLE_TRANSACTION_ERROR, null);
//        }
    }

}
