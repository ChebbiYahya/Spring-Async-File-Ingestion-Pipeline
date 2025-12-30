package com.bank.uploadfileanddatapersistdb_v3.domain.exception;

/**
 * Root exception for file ingestion / processing errors.
 */
public class FileProcessingException extends RuntimeException {

    public FileProcessingException(String message) {
        super(message);
    }

    public FileProcessingException(String message, Throwable cause) {
        super(message, cause);
    }
}
