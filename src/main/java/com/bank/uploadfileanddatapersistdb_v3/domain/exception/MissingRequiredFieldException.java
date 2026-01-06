package com.bank.uploadfileanddatapersistdb_v3.domain.exception;
// Couche domain: concepts metier, exceptions, enums et entites.

/**
 * Thrown when a required field is missing / empty.
 */
public class MissingRequiredFieldException extends ValidationException {

    public MissingRequiredFieldException(String message) {
        super(message);
    }
}
