package com.writesmith.core.service.request;

public class PrintToConsoleRequest extends AuthRequest {

    private String message;

    public PrintToConsoleRequest() {

    }

    public PrintToConsoleRequest(String authToken, String message) {
        super(authToken);
        this.message = message;
    }

    public String getMessage() {
        return message;
    }

}
