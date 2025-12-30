package com.bank.uploadfileanddatapersistdb_v3.domain.model.enums;

/**
 * Overall status of a file import job.
 */
public enum LogStatus {
    SUCCESS,            // All records inserted successfully
    FAILED,             // No record inserted
    PARTIALLY_TRAITED,  // At least one success and one failure (kept as-is to preserve DB values)
    IN_PROGRESS         // While processing
}
