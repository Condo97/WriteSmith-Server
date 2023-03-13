package com.writesmith.helpers.receipt;

import com.writesmith.database.DatabaseHelper;
import com.writesmith.database.objects.Receipt;
import com.writesmith.exceptions.PreparedStatementMissingArgumentException;
import com.writesmith.http.client.apple.itunes.AppleItunesHttpHelper;
import com.writesmith.http.client.apple.itunes.VerifyReceiptRequestBuilder;
import com.writesmith.http.client.apple.itunes.exception.AppleItunesResponseException;
import com.writesmith.http.client.apple.itunes.request.verifyreceipt.VerifyReceiptRequest;
import com.writesmith.http.client.apple.itunes.response.verifyreceipt.VerifyReceiptResponse;

import java.io.IOException;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Date;

public class ReceiptValidator {

    // Sets the receipt if expired or not!!! And also updates it in the database :)
    public static void validateReceipt(Receipt receipt, DatabaseHelper db) throws IOException, InterruptedException, SQLException, PreparedStatementMissingArgumentException, AppleItunesResponseException {
        // Build request object
        VerifyReceiptRequest request = new VerifyReceiptRequestBuilder().setReceiptData(receipt.getReceiptData()).build(); // Can also do VerifyReceiptRequestBuilder(receipt.getReceiptData()).build(); but this is more fun :)

        // Update and check receipt, updating receipt object
        AppleItunesHttpHelper helper = new AppleItunesHttpHelper();
        VerifyReceiptResponse response = helper.getVerifyReceiptResponse(request);

        // Update check date
        receipt.setCheckDate(new Timestamp(new Date().getTime()));

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

        // Update receipt
        db.updateReceipt(receipt);
    }
}
