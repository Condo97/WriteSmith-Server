package com.writesmith.exceptions;

public class AutoIncrementingDBObjectExistsException extends Exception {

    public AutoIncrementingDBObjectExistsException(String message) {
        super(message);
    }
}
