package com.bank.uploadfileanddatapersistdb_v3.domain.exception;
// Couche domain: concepts metier, exceptions, enums et entites.

/**
 * Thrown when streaming parsing fails (CSV/XML).
 */
public class StreamProcessingException extends FileProcessingException {

    public StreamProcessingException(String message, Throwable cause) {
        super(message, cause);
    }
}
