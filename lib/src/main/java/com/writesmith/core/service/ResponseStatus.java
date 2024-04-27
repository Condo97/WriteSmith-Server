package com.writesmith.core.service;

import com.fasterxml.jackson.annotation.JsonValue;

public enum ResponseStatus {
    SUCCESS(1),

    JSON_ERROR(4),              // JSON Cannot be read
    INVALID_AUTHENTICATION(5),  // AuthToken not found
    UNAUTHORIZED_ACCESS(6),     // User not associated with
    INVALID_FILE_TYPE(10),      // Invalid file type attached
    CAP_REACHED_ERROR(51),      // User reached daily generate cap

    OAIGPT_ERROR(60),
    INVALID_APPLE_TRANSACTION_ERROR(61),
    EXCEPTION_MAP_ERROR(70),
    ILLEGAL_ARGUMENT(80),
    UNHANDLED_ERROR(99);

    public final int Success;

    ResponseStatus(int Success) { this.Success = Success; }

    @JsonValue
    public int getValue() {
        return Success;
    }

}
