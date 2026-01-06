package com.bank.uploadfileanddatapersistdb_v3.domain.exception;
// Couche domain: concepts metier, exceptions, enums et entites.

/**
 * Thrown when an import log is not found.
 */
public class LogChargementNotFoundException extends RuntimeException {

    public LogChargementNotFoundException(String message) {
        super(message);
    }
}
