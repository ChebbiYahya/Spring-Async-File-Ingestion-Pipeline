package com.bank.uploadfileanddatapersistdb_v3.infrastructure.ingestion.validation;

import com.bank.uploadfileanddatapersistdb_v3.infrastructure.mapping.model.rules.FieldRule;

/**
 * Applies validation rules for a single field:
 * - required / nullable checks
 * - type check
 * - regex pattern check (if configured)
 */
public class FieldValidator {

    private final TypeChecker typeChecker = new TypeChecker();

    public String validate(FieldRule rule, String raw, int line) {
        boolean blank = (raw == null || raw.trim().isEmpty());

        // 1) required / nullable
        if (blank) {
            if (rule.isRequired()) {
                throw new RecordValidationException(
                        ErrorCode.REQUIRED_FIELD_MISSING,
                        rule.getName(),
                        line,
                        "Required field '" + rule.getName() + "' is missing/empty"
                );
            }
            if (!rule.isNullable()) {
                throw new RecordValidationException(
                        ErrorCode.NULL_NOT_ALLOWED,
                        rule.getName(),
                        line,
                        "Field '" + rule.getName() + "' cannot be null/empty"
                );
            }
            return null;
        }

        String value = raw.trim();

        // 2) type check
        if (!typeChecker.matches(rule.getType(), value)) {
            throw new RecordValidationException(
                    ErrorCode.TYPE_MISMATCH,
                    rule.getName(),
                    line,
                    "Type mismatch for '" + rule.getName() + "': expected " + rule.getType()
            );
        }

        // 3) pattern check
        if (rule.getPattern() != null && !rule.getPattern().isBlank()) {
            if (!java.util.regex.Pattern.matches(rule.getPattern(), value)) {
                throw new RecordValidationException(
                        ErrorCode.PATTERN_MISMATCH,
                        rule.getName(),
                        line,
                        "Field '" + rule.getName() + "' does not match pattern"
                );
            }
        }

        return value;
    }
}
