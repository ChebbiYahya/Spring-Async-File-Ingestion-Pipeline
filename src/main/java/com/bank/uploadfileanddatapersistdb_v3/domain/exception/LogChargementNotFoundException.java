package com.bank.uploadfileanddatapersistdb_v3.domain.exception;

/**
 * Thrown when an import log is not found.
 */
public class LogChargementNotFoundException extends RuntimeException {

    public LogChargementNotFoundException(String message) {
        super(message);
    }
}
