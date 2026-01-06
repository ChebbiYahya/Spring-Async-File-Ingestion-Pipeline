package com.bank.uploadfileanddatapersistdb_v3.domain.exception;
// Couche domain: concepts metier, exceptions, enums et entites.

/**
 * Thrown when the input file is empty or does not match expected format.
 */
public class InvalidFileFormatException extends RuntimeException {

    public InvalidFileFormatException(String message) {
        super(message);
    }
}
