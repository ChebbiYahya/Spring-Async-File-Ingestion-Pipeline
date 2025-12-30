package com.bank.uploadfileanddatapersistdb_v3.infrastructure.ingestion.validation;

/**
 * Checks whether a raw string matches the expected type (LONG/DECIMAL/LOCAL_DATE/STRING).
 */
public class TypeChecker {

    public boolean matches(String type, String raw) {
        return switch (type) {
            case "LONG" -> isLong(raw);
            case "DECIMAL" -> isDecimal(raw);
            case "LOCAL_DATE" -> isIsoLocalDate(raw); // yyyy-MM-dd
            case "STRING" -> true;
            default -> true;
        };
    }

    private boolean isLong(String s) {
        try {
            Long.parseLong(s);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private boolean isDecimal(String s) {
        try {
            new java.math.BigDecimal(s);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private boolean isIsoLocalDate(String s) {
        try {
            java.time.LocalDate.parse(s);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
