package com.bank.uploadfileanddatapersistdb_v3.domain.exception;

/**
 * Thrown when a required field is missing / empty.
 */
public class MissingRequiredFieldException extends ValidationException {

    public MissingRequiredFieldException(String message) {
        super(message);
    }
}
