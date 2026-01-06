package com.bank.uploadfileanddatapersistdb_v3.domain.exception;
// Couche domain: concepts metier, exceptions, enums et entites.

/**
 * Thrown when the file does not match mapping schema (CSV headers, XML root/record element...).
 */
public class SchemaValidationException extends FileProcessingException {

    public SchemaValidationException(String message) {
        super(message);
    }

    public SchemaValidationException(String message, Throwable cause) {
        super(message, cause);
    }
}
