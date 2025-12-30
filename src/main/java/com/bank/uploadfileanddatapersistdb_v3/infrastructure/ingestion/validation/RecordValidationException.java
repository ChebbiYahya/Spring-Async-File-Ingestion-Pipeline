package com.bank.uploadfileanddatapersistdb_v3.infrastructure.ingestion.validation;

import lombok.Getter;

/**
 * Functional validation exception describing a specific record issue.
 * Carries error code + field name + line number.
 */
@Getter
public class RecordValidationException extends RuntimeException {

    private final ErrorCode code;
    private final String field;
    private final int line;

    public RecordValidationException(ErrorCode code, String field, int line, String message) {
        super(message);
        this.code = code;
        this.field = field;
        this.line = line;
    }
}
