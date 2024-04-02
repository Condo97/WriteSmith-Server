package com.writesmith.core.service.endpoints;

import appletransactionclient.exception.AppStoreErrorResponseException;
import com.writesmith.exceptions.DBObjectNotFoundFromQueryException;
import com.writesmith.apple.iapvalidation.TransactionPersistentAppleUpdater;
import com.writesmith.database.dao.pooled.User_AuthTokenDAOPooled;
import com.writesmith.core.service.response.factory.BodyResponseFactory;
import com.writesmith.core.AppStoreSubscriptionStatusToIsPremiumAdapter;
import com.writesmith.database.model.objects.Transaction;
import com.writesmith.database.model.objects.User_AuthToken;
import com.writesmith.core.service.request.RegisterTransactionRequest;
import com.writesmith.core.service.response.BodyResponse;
import com.writesmith.core.service.response.IsPremiumResponse;
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
import java.text.SimpleDateFormat;
import java.util.Date;

public class RegisterTransactionEndpoint {

    public static BodyResponse registerTransaction(RegisterTransactionRequest registerTransactionRequest) throws DBSerializerException, SQLException, DBObjectNotFoundFromQueryException, InterruptedException, InvocationTargetException, IllegalAccessException, NoSuchMethodException, InstantiationException, UnrecoverableKeyException, CertificateException, IOException, URISyntaxException, KeyStoreException, NoSuchAlgorithmException, InvalidKeySpecException, DBSerializerPrimaryKeyMissingException, AppStoreErrorResponseException {
        // Get the user_authToken object to get the user id
        User_AuthToken u_aT = User_AuthTokenDAOPooled.get(registerTransactionRequest.getAuthToken());

        // Create transaction with now record date
        Transaction transaction = Transaction.withNowRecordDate(u_aT.getUserID(), registerTransactionRequest.getTransactionId());

        // Update transaction with Apple status
        TransactionPersistentAppleUpdater.updateAndSaveAppleTransactionStatus(transaction);

        // Get isPremium
        boolean isPremium = AppStoreSubscriptionStatusToIsPremiumAdapter.getIsPremium(transaction.getStatus());

                // TODO: Just logging to see things, remove and make a better logging system!
                if (isPremium)
                    System.out.println("User " + u_aT.getUserID() + " just registered a transaction at " + new SimpleDateFormat("HH:mm:ss").format(new Date()));

        // Create full validate premium response
        IsPremiumResponse fvpr = new IsPremiumResponse(isPremium);

        // Create and return successful body response with full validate premium response
        return BodyResponseFactory.createSuccessBodyResponse(fvpr);
    }

}
