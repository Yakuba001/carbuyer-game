package com.carbuyer.exception.exceptions;

public class NotCarOwnerException extends RuntimeException {
    public NotCarOwnerException(String message) {
        super(message);
    }
}
