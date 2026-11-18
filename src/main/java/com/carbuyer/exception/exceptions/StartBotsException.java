package com.carbuyer.exception.exceptions;

public class StartBotsException extends RuntimeException {
    public StartBotsException(String message, Throwable cause) {
        super(message, cause);
    }

    public StartBotsException(String message) {
        super(message);
    }
}
