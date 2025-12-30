package com.bank.uploadfileanddatapersistdb_v3.infrastructure.ingestion.validation;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Converts a validated string value into a Java type.
 * This utility is currently optional; persistence layer does conversions directly.
 */
public class TypeConverter {

    public Object convert(String type, String raw) {
        return switch (type) {
            case "LONG" -> Long.valueOf(raw);
            case "DECIMAL" -> new BigDecimal(raw);
            case "LOCAL_DATE" -> LocalDate.parse(raw);
            case "STRING" -> raw;
            default -> raw;
        };
    }
}
