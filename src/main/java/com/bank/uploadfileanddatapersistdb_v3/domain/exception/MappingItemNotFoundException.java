package com.bank.uploadfileanddatapersistdb_v3.domain.exception;
// Couche domain: concepts metier, exceptions, enums et entites.

public class MappingItemNotFoundException extends RuntimeException {
    public MappingItemNotFoundException(String message) {
        super(message);
    }
}
