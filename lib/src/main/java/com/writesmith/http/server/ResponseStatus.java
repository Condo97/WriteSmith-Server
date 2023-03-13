package com.writesmith.http.server;

public enum ResponseStatus {
    SUCCESS(1),
    ERROR(-1),
    CAP_REACHED_ERROR(51);

    public final int Success;

    ResponseStatus(int Success) { this.Success = Success; }
}
