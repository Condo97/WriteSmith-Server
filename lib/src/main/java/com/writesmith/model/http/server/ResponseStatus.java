package com.writesmith.model.http.server;

public enum ResponseStatus {
    SUCCESS(1),

    JSON_ERROR(4),
    CAP_REACHED_ERROR(51),
    OAIGPT_ERROR(60),
    INVALID_APPLE_TRANSACTION_ERROR(61),
    EXCEPTION_MAP_ERROR(70),
    ILLEGAL_ARGUMENT(80),
    UNHANDLED_ERROR(99);

    public final int Success;

    ResponseStatus(int Success) { this.Success = Success; }
}
