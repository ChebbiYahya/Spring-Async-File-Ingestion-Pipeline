package com.bank.uploadfileanddatapersistdb_v3.application.interfaces;
// Callback simple pour signaler un record traite.

/**
 * Reports progress at record level (line/record).
 * Implementation is typically JobProgressService (in-memory).
 */
@FunctionalInterface
public interface ProgressReporter {
    void onRecordProcessed();
}
