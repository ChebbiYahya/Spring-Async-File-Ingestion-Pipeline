package com.bank.uploadfileanddatapersistdb_v3.application.service;

import com.bank.uploadfileanddatapersistdb_v3.api.dto.JobProgressDto;
import com.bank.uploadfileanddatapersistdb_v3.api.mapper.JobProgressMapper;
import com.bank.uploadfileanddatapersistdb_v3.application.interfaces.JobProgressService;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * JobProgressServiceImpl
 *
 * Service en mémoire (in-memory) pour suivre la progression des jobs asynchrones.
 *
 * Responsabilités :
 * - gérer l’état interne des jobs (RUNNING / FINISHED / FAILED)
 * - calculer percent, elapsed time et ETA
 * - déléguer la construction du DTO au JobProgressMapper
 */
@Service
public class JobProgressServiceImpl implements JobProgressService {

    /**
     * État interne d’un job.
     * Caché à l’extérieur (API expose uniquement JobProgressDto).
     */
    private static class State {
        String status;
        int totalRecords;
        int processedRecords;
        Instant startedAt;
    }

    /**
     * Store thread-safe (jobId -> State).
     * Nécessaire car accès concurrent (thread async + HTTP).
     */
    private final Map<String, State> store = new ConcurrentHashMap<>();

    /**
     * Mapper DTO dédié.
     */
    private final JobProgressMapper mapper;

    public JobProgressServiceImpl(JobProgressMapper mapper) {
        this.mapper = mapper;
    }

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

        // 1) Temps écoulé
        long elapsedSec = 0;
        if (s.startedAt != null) {
            elapsedSec = Math.max(0, Duration.between(s.startedAt, Instant.now()).getSeconds());
        }

        // 2) Pourcentage
        int percent;
        if (s.totalRecords <= 0) {
            percent = "FINISHED".equals(s.status) ? 100 : 0;
        } else {
            long p = (s.processedRecords * 100L) / s.totalRecords;
            percent = (int) Math.min(100, Math.max(0, p));
        }

        // 3) ETA (time left)
        Long timeLeft = null;
        if ("RUNNING".equals(s.status)
                && s.totalRecords > 0
                && s.processedRecords > 0
                && elapsedSec > 0) {

            double rate = (double) s.processedRecords / (double) elapsedSec;
            if (rate > 0) {
                long remaining = s.totalRecords - s.processedRecords;
                timeLeft = remaining <= 0
                        ? 0L
                        : (long) Math.ceil(remaining / rate);
            }
        }

        // 4) Mapping vers DTO (via mapper)
        return mapper.toDto(
                jobId,
                s.status,
                s.totalRecords,
                s.processedRecords,
                percent,
                timeLeft,
                elapsedSec
        );
    }
}
