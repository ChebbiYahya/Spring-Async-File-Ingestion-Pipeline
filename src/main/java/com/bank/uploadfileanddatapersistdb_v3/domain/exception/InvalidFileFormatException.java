package com.bank.uploadfileanddatapersistdb_v3.domain.exception;

/**
 * Thrown when the input file is empty or does not match expected format.
 */
public class InvalidFileFormatException extends RuntimeException {

    public InvalidFileFormatException(String message) {
        super(message);
    }
}
