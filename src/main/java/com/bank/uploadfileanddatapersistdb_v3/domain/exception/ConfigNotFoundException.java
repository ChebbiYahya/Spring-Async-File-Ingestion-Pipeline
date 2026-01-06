package com.bank.uploadfileanddatapersistdb_v3.domain.exception;
// Couche domain: concepts metier, exceptions, enums et entites.

public class ConfigNotFoundException extends RuntimeException {
    public ConfigNotFoundException(String msg) { super(msg); }
}
