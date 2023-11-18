package com.writesmith.core.service.response;

public class GetRemainingResponse {
    private long remaining;

    public GetRemainingResponse() {

    }

    public GetRemainingResponse(long remaining) {
        this.remaining = remaining;
    }

    public long getRemaining() {
        return remaining;
    }

    public void setRemaining(long remaining) {
        this.remaining = remaining;
    }
}
