package com.bank.uploadfileanddatapersistdb_v3.api.dto;

import lombok.*;

/**
 * In-memory progress view for async processing jobs.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JobProgressDto {
    private String jobId;
    private String status; // RUNNING / FINISHED / FAILED

    private int totalRecords;
    private int processedRecords;

    private int percent; // 0..100

    private Long timeLeft;        // Estimated remaining time (null if unknown)
    private Long totalTimeSeconds;  // Elapsed time since job start
}
