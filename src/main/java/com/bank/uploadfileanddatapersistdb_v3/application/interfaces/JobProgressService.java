package com.bank.uploadfileanddatapersistdb_v3.application.interfaces;

import com.bank.uploadfileanddatapersistdb_v3.api.dto.JobProgressDto;

public interface JobProgressService {
    /**
     * Start a new job and initialize its progress state.
     *
     * @param totalRecords total number of records to process
     * @return generated jobId
     */
    String start(int totalRecords);

    /**
     * Increment processed records count by one.
     */
    void incrementProcessed(String jobId);

    /**
     * Mark job as finished successfully.
     */
    void finish(String jobId);

    /**
     * Mark job as failed.
     */
    void fail(String jobId);

    /**
     * Get current progress snapshot for a job.
     *
     * @param jobId job identifier
     * @return JobProgressDto or null if job not found
     */
    JobProgressDto get(String jobId);
}
