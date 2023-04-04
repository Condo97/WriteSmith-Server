package com.writesmith.http.client.apple.itunes.response.verifyreceipt;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.writesmith.http.client.apple.itunes.response.AppleItunesBaseResponse;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class VerifyReceiptResponse implements AppleItunesBaseResponse {
    private String environment;
    private String latest_receipt;
    private int status;
    private VerifyReceiptReceiptResponse receipt;
    private List<VerifyReceiptLatestReceiptInfoResponse> latest_receipt_info;
    private List<VerifyReceiptPendingRenewalInfoResponse> pending_renewal_info;

    public VerifyReceiptResponse() {

    }

    public VerifyReceiptResponse(String environment, String latest_receipt, int status, VerifyReceiptReceiptResponse receipt, List<VerifyReceiptLatestReceiptInfoResponse> latest_receipt_info, List<VerifyReceiptPendingRenewalInfoResponse> pending_renewal_info) {
        this.environment = environment;
        this.latest_receipt = latest_receipt;
        this.status = status;
        this.receipt = receipt;
        this.latest_receipt_info = latest_receipt_info;
        this.pending_renewal_info = pending_renewal_info;
    }

    public String getEnvironment() {
        return environment;
    }

    public void setEnvironment(String environment) {
        this.environment = environment;
    }

    public String getLatest_receipt() {
        return latest_receipt;
    }

    public void setLatest_receipt(String latest_receipt) {
        this.latest_receipt = latest_receipt;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public VerifyReceiptReceiptResponse getReceipt() {
        return receipt;
    }

    public void setReceipt(VerifyReceiptReceiptResponse receipt) {
        this.receipt = receipt;
    }

    public List<VerifyReceiptLatestReceiptInfoResponse> getLatest_receipt_info() {
        return latest_receipt_info;
    }

    public void setLatest_receipt_info(List<VerifyReceiptLatestReceiptInfoResponse> latest_receipt_info) {
        this.latest_receipt_info = latest_receipt_info;
    }

    public List<VerifyReceiptPendingRenewalInfoResponse> getPending_renewal_info() {
        return pending_renewal_info;
    }

    public void setPending_renewal_info(List<VerifyReceiptPendingRenewalInfoResponse> pending_renewal_info) {
        this.pending_renewal_info = pending_renewal_info;
    }
}
