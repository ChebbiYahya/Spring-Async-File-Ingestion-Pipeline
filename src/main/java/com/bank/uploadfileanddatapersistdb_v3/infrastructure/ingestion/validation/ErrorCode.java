package com.bank.uploadfileanddatapersistdb_v3.infrastructure.ingestion.validation;

/**
 * Normalized error codes for record validation.
 */
public enum ErrorCode {
    MISSING_COLUMN,
    UNEXPECTED_COLUMN,
    REQUIRED_FIELD_MISSING,
    NULL_NOT_ALLOWED,
    PATTERN_MISMATCH,
    TYPE_MISMATCH,
    DUPLICATE_IN_FILE,
    DUPLICATE_IN_DB,
    XML_ROOT_MISMATCH
}
