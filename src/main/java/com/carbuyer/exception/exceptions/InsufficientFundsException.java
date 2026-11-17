package com.carbuyer.exception.exceptions;

import lombok.Getter;

import java.util.UUID;

@Getter
public class InsufficientFundsException extends RuntimeException {

    private final UUID listingId;

    public InsufficientFundsException(String message, UUID listingId) {
        super(message);
        this.listingId = listingId;
    }
}
