package com.writesmith.core.apple.iapvalidation;

import com.writesmith.model.database.objects.Receipt;
import com.writesmith.common.exceptions.PreparedStatementMissingArgumentException;
import com.writesmith.model.http.client.apple.itunes.exception.AppleItunesResponseException;
import com.writesmith.model.http.client.apple.itunes.request.verifyreceipt.VerifyReceiptRequest;
import com.writesmith.model.http.client.apple.itunes.response.verifyreceipt.VerifyReceiptResponse;
import com.writesmith.keys.Keys;
import sqlcomponentizer.dbserializer.DBSerializerException;

import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalDateTime;

public class ReceiptValidator {

    // Sets the receipt if expired or not!!! And also updates it in the database :)
    public static void validateReceipt(Receipt receipt) throws IOException, InterruptedException, SQLException, PreparedStatementMissingArgumentException, AppleItunesResponseException, DBSerializerException {
        // Create request object
        VerifyReceiptRequest request = new VerifyReceiptRequest(
                receipt.getReceiptData(),
                Keys.sharedAppSecret,
                "false"
        );

        // Update and check receipt, updating receipt object
        AppleHttpVerifyReceipt helper = new AppleHttpVerifyReceipt();
        VerifyReceiptResponse response = helper.getVerifyReceiptResponse(request);

        // Update check date
        receipt.setCheckDate(LocalDateTime.now());

        // Check if getPendingRenewalInfo exists, meaning potentially premium
        // Check if getPendingRenewalInfo size is > 0, meaning potentially premium
        // Check if expirationIntent is null, meaning is premium
        if (response.getPending_renewal_info() != null && response.getPending_renewal_info().size() > 0 && response.getPending_renewal_info().get(0).getExpiration_intent() == null) {
            // Should be premium
            // Check if premium
            // If not premium, update
            if (receipt.isExpired()) receipt.setExpired(false);
        } else {
            // Should not be premium
            // Check if premium
            // If premium, update
            if (!receipt.isExpired()) receipt.setExpired(true);
        }
    }
}
