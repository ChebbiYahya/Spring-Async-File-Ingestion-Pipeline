package com.bank.uploadfileanddatapersistdb_v3.infrastructure.ingestion.duplicate;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Builds a duplicate key by concatenating configured fields from a validated record.
 */
public class DuplicateKeyBuilder {

    public String buildKey(List<String> fields, Map<String, ?> record) {
        return fields.stream()
                .map(f -> java.util.Objects.toString(record.get(f), ""))
                .collect(Collectors.joining("|"));
    }
}
