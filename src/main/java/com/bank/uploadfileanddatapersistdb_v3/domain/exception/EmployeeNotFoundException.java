package com.bank.uploadfileanddatapersistdb_v3.domain.exception;

/**
 * Thrown when a requested Employee does not exist.
 */
public class EmployeeNotFoundException extends RuntimeException {

    public EmployeeNotFoundException(String message) {
        super(message);
    }
}
