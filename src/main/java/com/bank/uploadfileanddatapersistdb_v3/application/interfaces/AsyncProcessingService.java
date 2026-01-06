package com.bank.uploadfileanddatapersistdb_v3.application.interfaces;
// Interface pour demarrer et executer un traitement asynchrone.

/**
 * Interface defining asynchronous processing operations.
 * Used by controllers to start and run async ingestion jobs.
 */
public interface AsyncProcessingService {
    /**
     * Starts a job by counting total records in DATA_IN
     * and initializing progress tracking.

     */
    String startJob(String configId);

    /**
     * Runs the ingestion job asynchronously.
     * This method is triggered after startJob().
     */
    void runJob(String jobId, String configId);
}
