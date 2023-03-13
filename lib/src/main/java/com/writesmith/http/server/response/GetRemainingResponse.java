package com.writesmith.http.server.response;

public class GetRemainingResponse {
    private int remaining;

    public GetRemainingResponse() {

    }

    public GetRemainingResponse(int remaining) {
        this.remaining = remaining;
    }

    public int getRemaining() {
        return remaining;
    }

    public void setRemaining(int remaining) {
        this.remaining = remaining;
    }
}
