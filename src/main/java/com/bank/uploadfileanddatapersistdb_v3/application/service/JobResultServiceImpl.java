package com.bank.uploadfileanddatapersistdb_v3.application.service;
// Stockage en memoire du resultat final d'un job.

import com.bank.uploadfileanddatapersistdb_v3.api.dto.FinalResultDto;
import com.bank.uploadfileanddatapersistdb_v3.application.interfaces.JobResultService;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class JobResultServiceImpl implements JobResultService {

    private static class State {
        private final List<String> treated;
        private final List<FinalResultDto.FileFailedDto> failed;

        private State() {
            this.treated = Collections.synchronizedList(new ArrayList<>());
            this.failed = Collections.synchronizedList(new ArrayList<>());
        }
    }

    private final Map<String, State> store = new ConcurrentHashMap<>();

    @Override
    public void start(String jobId) {
        if (jobId == null || jobId.isBlank()) {
            return;
        }
        store.put(jobId, new State());
    }

    @Override
    public void addTreated(String jobId, String fileName) {
        if (jobId == null || fileName == null) {
            return;
        }
        State s = store.get(jobId);
        if (s == null) {
            return;
        }
        s.treated.add(fileName);
    }

    @Override
    public void addFailed(String jobId, String fileName, String detailProblem) {
        if (jobId == null || fileName == null) {
            return;
        }
        State s = store.get(jobId);
        if (s == null) {
            return;
        }
        s.failed.add(FinalResultDto.FileFailedDto.builder()
                .filename(fileName)
                .detailProblem(detailProblem)
                .build());
    }

    @Override
    public FinalResultDto get(String jobId) {
        State s = store.get(jobId);
        if (s == null) {
            return null;
        }
        return FinalResultDto.builder()
                .filesTreated(new ArrayList<>(s.treated))
                .filesFailed(new ArrayList<>(s.failed))
                .build();
    }
}
