package com.writesmith.model.http.client.apple.itunes.response.verifyreceipt;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class VerifyReceiptReceiptInAppResponse {
    private String quantity;
    private String product_id, transaction_id, original_transaction_id;
    private String purchase_date, purchase_date_ms, purchase_date_pst;
    private String original_purchase_date, original_purchase_date_ms, original_purchase_date_pst;
    private String expires_date, expires_date_ms, expires_date_pst;
//    @JsonSerialize(using = ToStringSerializer.class, as = String.class)
    private String web_order_line_item_id;
    private String is_trial_period;
    private String is_in_intro_offer_period;
    private String in_app_ownership_type;

    public VerifyReceiptReceiptInAppResponse() {

    }

    public VerifyReceiptReceiptInAppResponse(String quantity, String product_id, String transaction_id, String original_transaction_id, String purchase_date, String purchase_date_ms, String purchase_date_pst, String original_purchase_date, String original_purchase_date_ms, String original_purchase_date_pst, String expires_date, String expires_date_ms, String expires_date_pst, String web_order_line_item_id, String is_trial_period, String is_in_intro_offer_period, String in_app_ownership_type) {
        this.quantity = quantity;
        this.product_id = product_id;
        this.transaction_id = transaction_id;
        this.original_transaction_id = original_transaction_id;
        this.purchase_date = purchase_date;
        this.purchase_date_ms = purchase_date_ms;
        this.purchase_date_pst = purchase_date_pst;
        this.original_purchase_date = original_purchase_date;
        this.original_purchase_date_ms = original_purchase_date_ms;
        this.original_purchase_date_pst = original_purchase_date_pst;
        this.expires_date = expires_date;
        this.expires_date_ms = expires_date_ms;
        this.expires_date_pst = expires_date_pst;
        this.web_order_line_item_id = web_order_line_item_id;
        this.is_trial_period = is_trial_period;
        this.is_in_intro_offer_period = is_in_intro_offer_period;
        this.in_app_ownership_type = in_app_ownership_type;
    }

    public String getQuantity() {
        return quantity;
    }

    public void setQuantity(String quantity) {
        this.quantity = quantity;
    }

    public String getProduct_id() {
        return product_id;
    }

    public void setProduct_id(String product_id) {
        this.product_id = product_id;
    }

    public String getTransaction_id() {
        return transaction_id;
    }

    public void setTransaction_id(String transaction_id) {
        this.transaction_id = transaction_id;
    }

    public String getOriginal_transaction_id() {
        return original_transaction_id;
    }

    public void setOriginal_transaction_id(String original_transaction_id) {
        this.original_transaction_id = original_transaction_id;
    }

    public String getPurchase_date() {
        return purchase_date;
    }

    public void setPurchase_date(String purchase_date) {
        this.purchase_date = purchase_date;
    }

    public String getPurchase_date_ms() {
        return purchase_date_ms;
    }

    public void setPurchase_date_ms(String purchase_date_ms) {
        this.purchase_date_ms = purchase_date_ms;
    }

    public String getPurchase_date_pst() {
        return purchase_date_pst;
    }

    public void setPurchase_date_pst(String purchase_date_pst) {
        this.purchase_date_pst = purchase_date_pst;
    }

    public String getOriginal_purchase_date() {
        return original_purchase_date;
    }

    public void setOriginal_purchase_date(String original_purchase_date) {
        this.original_purchase_date = original_purchase_date;
    }

    public String getOriginal_purchase_date_ms() {
        return original_purchase_date_ms;
    }

    public void setOriginal_purchase_date_ms(String original_purchase_date_ms) {
        this.original_purchase_date_ms = original_purchase_date_ms;
    }

    public String getOriginal_purchase_date_pst() {
        return original_purchase_date_pst;
    }

    public void setOriginal_purchase_date_pst(String original_purchase_date_pst) {
        this.original_purchase_date_pst = original_purchase_date_pst;
    }

    public String getExpires_date() {
        return expires_date;
    }

    public void setExpires_date(String expires_date) {
        this.expires_date = expires_date;
    }

    public String getExpires_date_ms() {
        return expires_date_ms;
    }

    public void setExpires_date_ms(String expires_date_ms) {
        this.expires_date_ms = expires_date_ms;
    }

    public String getExpires_date_pst() {
        return expires_date_pst;
    }

    public void setExpires_date_pst(String expires_date_pst) {
        this.expires_date_pst = expires_date_pst;
    }

    public String getWeb_order_line_item_id() {
        return web_order_line_item_id;
    }

    public void setWeb_order_line_item_id(String web_order_line_item_id) {
        this.web_order_line_item_id = web_order_line_item_id;
    }

    public String getIs_trial_period() {
        return is_trial_period;
    }

    public void setIs_trial_period(String is_trial_period) {
        this.is_trial_period = is_trial_period;
    }

    public String getIs_in_intro_offer_period() {
        return is_in_intro_offer_period;
    }

    public void setIs_in_intro_offer_period(String is_in_intro_offer_period) {
        this.is_in_intro_offer_period = is_in_intro_offer_period;
    }

    public String getIn_app_ownership_type() {
        return in_app_ownership_type;
    }

    public void setIn_app_ownership_type(String in_app_ownership_type) {
        this.in_app_ownership_type = in_app_ownership_type;
    }
}
