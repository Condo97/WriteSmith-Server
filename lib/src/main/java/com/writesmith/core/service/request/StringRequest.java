package com.writesmith.core.service.request;

public class StringRequest {

    private String string;

    public StringRequest() {

    }

    public StringRequest(String string) {
        this.string = string;
    }

    public String getString() {
        return string;
    }

    public void setString(String string) {
        this.string = string;
    }

}
