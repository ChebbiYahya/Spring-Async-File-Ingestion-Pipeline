package com.bank.uploadfileanddatapersistdb_v3.application.service;

import com.bank.uploadfileanddatapersistdb_v3.api.dto.JobProgressDto;
import com.bank.uploadfileanddatapersistdb_v3.application.interfaces.JobProgressService;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory job progress store for async processing.
 * This is intentionally simple (no DB) to keep behavior identical to your current project.
 */
@Service
public class JobProgressServiceImpl implements JobProgressService {

    private static class State {
        String status;
        int totalRecords;
        int processedRecords;
        Instant startedAt;
        long estimatedTotalTimeSeconds;
    }

    private final Map<String, State> store = new ConcurrentHashMap<>();

    public String start(int totalRecords) {
        String id = UUID.randomUUID().toString();
        State s = new State();
        s.status = "RUNNING";
        s.totalRecords = Math.max(0, totalRecords);
        s.processedRecords = 0;
        s.startedAt = Instant.now();
        int recordsPerSecond = 5; // estimation métier
        if (s.totalRecords > 0) {
            s.estimatedTotalTimeSeconds =
                    Math.max(1, Math.round((double) s.totalRecords / recordsPerSecond));
        } else {
            s.estimatedTotalTimeSeconds = 0;
        }
        store.put(id, s);
        return id;
    }

    public void incrementProcessed(String jobId) {
        State s = store.get(jobId);
        if (s == null) return;
        s.processedRecords++;
    }

    public void finish(String jobId) {
        State s = store.get(jobId);
        if (s == null) return;
        s.status = "FINISHED";
    }

    public void fail(String jobId) {
        State s = store.get(jobId);
        if (s == null) return;
        s.status = "FAILED";
    }

    public JobProgressDto get(String jobId) {
        State s = store.get(jobId);
        if (s == null) return null;

        // ---- PERCENT ----
        int percent = (s.totalRecords <= 0)
                ? ("FINISHED".equals(s.status) ? 100 : 0)
                : (int) Math.min(100, Math.round((s.processedRecords * 100.0) / s.totalRecords));
        // ---- ETA ----
        Long etaSeconds = null;
        if ("RUNNING".equals(s.status) && s.estimatedTotalTimeSeconds > 0) {
            etaSeconds = Math.max(
                    0,
                    Math.round(
                            s.estimatedTotalTimeSeconds * (1 - percent / 100.0)
                    )
            );
        }

        return JobProgressDto.builder()
                .jobId(jobId)
                .status(s.status)
                .totalRecords(s.totalRecords)
                .processedRecords(s.processedRecords)
                .percent(percent)
                .timeLeft(etaSeconds)
                .totalTimeSeconds(s.estimatedTotalTimeSeconds)
                .build();
    }
}
