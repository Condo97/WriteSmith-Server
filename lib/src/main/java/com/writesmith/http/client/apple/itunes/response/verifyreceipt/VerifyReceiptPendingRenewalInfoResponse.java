package com.writesmith.http.client.apple.itunes.response.verifyreceipt;

public class VerifyReceiptPendingRenewalInfoResponse {
    String expiration_intent;
    String auto_renew_product_id;
    String is_in_billing_retry_period;
    String product_id;
    String original_transaction_id;
    String auto_renew_status;

    public VerifyReceiptPendingRenewalInfoResponse() {

    }

    public VerifyReceiptPendingRenewalInfoResponse(String expiration_intent, String auto_renew_product_id, String is_in_billing_retry_period, String product_id, String original_transaction_id, String auto_renew_status) {
        this.expiration_intent = expiration_intent;
        this.auto_renew_product_id = auto_renew_product_id;
        this.is_in_billing_retry_period = is_in_billing_retry_period;
        this.product_id = product_id;
        this.original_transaction_id = original_transaction_id;
        this.auto_renew_status = auto_renew_status;
    }

    public String getExpiration_intent() {
        return expiration_intent;
    }

    public void setExpiration_intent(String expiration_intent) {
        this.expiration_intent = expiration_intent;
    }

    public String getAuto_renew_product_id() {
        return auto_renew_product_id;
    }

    public void setAuto_renew_product_id(String auto_renew_product_id) {
        this.auto_renew_product_id = auto_renew_product_id;
    }

    public String getIs_in_billing_retry_period() {
        return is_in_billing_retry_period;
    }

    public void setIs_in_billing_retry_period(String is_in_billing_retry_period) {
        this.is_in_billing_retry_period = is_in_billing_retry_period;
    }

    public String getProduct_id() {
        return product_id;
    }

    public void setProduct_id(String product_id) {
        this.product_id = product_id;
    }

    public String getOriginal_transaction_id() {
        return original_transaction_id;
    }

    public void setOriginal_transaction_id(String original_transaction_id) {
        this.original_transaction_id = original_transaction_id;
    }

    public String getAuto_renew_status() {
        return auto_renew_status;
    }

    public void setAuto_renew_status(String auto_renew_status) {
        this.auto_renew_status = auto_renew_status;
    }
}
