package com.bank.uploadfileanddatapersistdb_v3.application.interfaces;
// Interface pour recuperer le resultat final d'un job.

import com.bank.uploadfileanddatapersistdb_v3.api.dto.FinalResultDto;

public interface JobResultService {
    void start(String jobId);

    void addTreated(String jobId, String fileName);

    void addFailed(String jobId, String fileName, String detailProblem);

    FinalResultDto get(String jobId);
}
