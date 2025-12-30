package com.bank.uploadfileanddatapersistdb_v3.application.interfaces;

/**
 * Interface defining asynchronous processing operations.
 * Used by controllers to start and run async ingestion jobs.
 */
public interface AsyncProcessingService {
    /**
     * Starts a job by counting total records in DATA_IN
     * and initializing progress tracking.
     *
     * @param mappingCsv CSV mapping path (optional)
     * @param mappingXml XML mapping path (optional)
     * @return generated jobId
     */
    String startJob(String mappingCsv, String mappingXml);

    /**
     * Runs the ingestion job asynchronously.
     * This method is triggered after startJob().
     *
     * @param jobId      job identifier
     * @param mappingCsv CSV mapping path
     * @param mappingXml XML mapping path
     */
    void runJob(String jobId, String mappingCsv, String mappingXml);
}
