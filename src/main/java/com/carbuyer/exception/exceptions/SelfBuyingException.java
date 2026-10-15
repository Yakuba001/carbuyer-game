package com.carbuyer.exception.exceptions;

public class SelfBuyingException extends RuntimeException {
    public SelfBuyingException(String message) {
        super(message);
    }
}
