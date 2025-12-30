package com.bank.uploadfileanddatapersistdb_v3.domain.exception;

/**
 * Thrown when a value does not match the expected type.
 */
public class TypeMismatchException extends ValidationException {

    public TypeMismatchException(String message, Throwable cause) {
        super(message, cause);
    }
}
