package com.bank.uploadfileanddatapersistdb_v3.domain.exception;

/**
 * Thrown when streaming parsing fails (CSV/XML).
 */
public class StreamProcessingException extends FileProcessingException {

    public StreamProcessingException(String message, Throwable cause) {
        super(message, cause);
    }
}
