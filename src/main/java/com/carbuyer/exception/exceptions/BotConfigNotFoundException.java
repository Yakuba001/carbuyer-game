package com.carbuyer.exception.exceptions;

public class BotConfigNotFoundException extends RuntimeException {
    public BotConfigNotFoundException(String message) {
        super(message);
    }
}
