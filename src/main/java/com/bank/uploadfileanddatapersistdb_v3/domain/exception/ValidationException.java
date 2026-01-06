package com.bank.uploadfileanddatapersistdb_v3.domain.exception;
// Couche domain: concepts metier, exceptions, enums et entites.

/**
 * Base class for functional validation exceptions.
 * Extends FileProcessingException to keep a single exception family for ingestion.
 */
public class ValidationException extends FileProcessingException {

    public ValidationException(String message) {
        super(message);
    }

    public ValidationException(String message, Throwable cause) {
        super(message, cause);
    }
}
