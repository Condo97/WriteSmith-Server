package com.writesmith.http.server;

public enum ResponseStatus {
    SUCCESS("Success"),
    ERROR("Error");

    public final String string;

    ResponseStatus(String string) { this.string = string; }
}
