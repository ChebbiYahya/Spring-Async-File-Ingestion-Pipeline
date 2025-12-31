package com.bank.uploadfileanddatapersistdb_v3.application.service;

import com.bank.uploadfileanddatapersistdb_v3.api.dto.JobProgressDto;
import com.bank.uploadfileanddatapersistdb_v3.application.interfaces.JobProgressService;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class JobProgressServiceImpl implements JobProgressService {

    private static class State {
        String status;              // RUNNING / FINISHED / FAILED
        int totalRecords;
        int processedRecords;
        Instant startedAt;
    }

    private final Map<String, State> store = new ConcurrentHashMap<>();

    @Override
    public String start(int totalRecords) {
        String id = UUID.randomUUID().toString();
        State s = new State();
        s.status = "RUNNING";
        s.totalRecords = Math.max(0, totalRecords);
        s.processedRecords = 0;
        s.startedAt = Instant.now();
        store.put(id, s);
        return id;
    }

    @Override
    public void incrementProcessed(String jobId) {
        State s = store.get(jobId);
        if (s == null) return;
        s.processedRecords++;
    }

    @Override
    public void finish(String jobId) {
        State s = store.get(jobId);
        if (s == null) return;
        s.status = "FINISHED";
    }

    @Override
    public void fail(String jobId) {
        State s = store.get(jobId);
        if (s == null) return;
        s.status = "FAILED";
    }

    @Override
    public JobProgressDto get(String jobId) {
        State s = store.get(jobId);
        if (s == null) return null;

        long elapsedSec = 0;
        if (s.startedAt != null) {
            elapsedSec = Math.max(0, Duration.between(s.startedAt, Instant.now()).getSeconds());
        }

        int percent;
        if (s.totalRecords <= 0) {
            percent = "FINISHED".equals(s.status) ? 100 : 0;
        } else {
            long p = (s.processedRecords * 100L) / s.totalRecords;
            if (p < 0) p = 0;
            if (p > 100) p = 100;
            percent = (int) p;
        }

        Long timeLeft = null;
        if ("RUNNING".equals(s.status) && s.totalRecords > 0 && s.processedRecords > 0 && elapsedSec > 0) {
            double rate = (double) s.processedRecords / (double) elapsedSec; // records/sec
            if (rate > 0.0) {
                long remaining = s.totalRecords - s.processedRecords;
                if (remaining <= 0) timeLeft = 0L;
                else timeLeft = (long) Math.ceil(remaining / rate);
            }
        }

        return JobProgressDto.builder()
                .jobId(jobId)
                .status(s.status)
                .totalRecords(s.totalRecords)
                .processedRecords(s.processedRecords)
                .percent(percent)
                .timeLeft(timeLeft)
                .totalTimeSeconds(elapsedSec) // temps réel
                .build();
    }
}
