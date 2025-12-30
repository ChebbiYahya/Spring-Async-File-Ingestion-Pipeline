package com.bank.uploadfileanddatapersistdb_v3.application.service;

import com.bank.uploadfileanddatapersistdb_v3.application.interfaces.LogChargementService;
import com.bank.uploadfileanddatapersistdb_v3.domain.exception.LogChargementNotFoundException;
import com.bank.uploadfileanddatapersistdb_v3.domain.model.entity.LogChargement;
import com.bank.uploadfileanddatapersistdb_v3.domain.model.entity.LogChargementDetail;
import com.bank.uploadfileanddatapersistdb_v3.domain.model.enums.LineStatus;
import com.bank.uploadfileanddatapersistdb_v3.domain.model.enums.LogStatus;
import com.bank.uploadfileanddatapersistdb_v3.infrastructure.persistence.repository.LogChargementRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Import log management: start/addLine/finalize and search.
 * Counters are automatically updated in addLine().
 */
@Service
@RequiredArgsConstructor
public class LogChargementServiceImpl implements LogChargementService {

    private final LogChargementRepository logChargementRepository;

    @Override
    @Transactional
    public LogChargement startLog(String fileName) {
        LogChargement log = LogChargement.builder()
                .fileName(fileName)
                .status(LogStatus.IN_PROGRESS)
                .createdAt(LocalDateTime.now())
                .details(new ArrayList<>())
                .build();
        return logChargementRepository.save(log);
    }

    @Override
    @Transactional
    public void addLine(LogChargement log, int lineNumber, LineStatus status, String detailProblem) {
        // Update counters automatically
        log.incrementTotal();
        if (status == LineStatus.SUCCESS) log.incrementSuccess();
        else log.incrementFailed();

        LogChargementDetail detail = LogChargementDetail.builder()
                .lineNumber(lineNumber)
                .status(status)
                .detailProblem(detailProblem)
                .logChargement(log)
                .build();

        log.addDetail(detail);
    }

    @Override
    @Transactional
    public void finalizeLog(LogChargement log, int totalLines, int successLines, int failedLines) {
        int ok = (log.getSuccessLines() == null) ? 0 : log.getSuccessLines();
        int ko = (log.getFailedLines() == null) ? 0 : log.getFailedLines();

        if (ok > 0 && ko == 0) log.setStatus(LogStatus.SUCCESS);
        else if (ok == 0 && ko > 0) log.setStatus(LogStatus.FAILED);
        else if (ok > 0) log.setStatus(LogStatus.PARTIALLY_TRAITED);
        else log.setStatus(LogStatus.FAILED);

        logChargementRepository.save(log);
    }

    @Override
    @Transactional(readOnly = true)
    public List<LogChargement> getAllLogs() {
        return logChargementRepository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public LogChargement getLogById(Long id) {
        return logChargementRepository.findById(id)
                .orElseThrow(() -> new LogChargementNotFoundException("LogChargement not found with id: " + id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<LogChargement> searchLogs(String fileName, LogStatus status) {
        return logChargementRepository.findAll().stream()
                .filter(log -> {
                    if (fileName == null || fileName.isBlank()) return true;
                    return log.getFileName() != null && log.getFileName().toLowerCase().contains(fileName.toLowerCase());
                })
                .filter(log -> status == null || status.equals(log.getStatus()))
                .toList();
    }
}
