package com.writesmith.exceptions;

public class BasicException extends Exception {
    String description;

    public BasicException(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
