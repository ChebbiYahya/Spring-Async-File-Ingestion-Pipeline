package com.bank.uploadfileanddatapersistdb_v3.infrastructure.ingestion.duplicate;

import java.util.HashSet;
import java.util.Set;

/**
 * Detects duplicates within the same file using an in-memory Set.
 */
public class InFileDuplicateChecker {

    private final Set<String> seen = new HashSet<>();

    public boolean isDuplicate(String key) {
        return !seen.add(key);
    }
}
