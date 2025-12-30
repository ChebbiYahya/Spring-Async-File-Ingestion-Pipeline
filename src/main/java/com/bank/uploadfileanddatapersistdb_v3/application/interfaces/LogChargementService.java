package com.bank.uploadfileanddatapersistdb_v3.application.interfaces;

import com.bank.uploadfileanddatapersistdb_v3.domain.model.entity.LogChargement;
import com.bank.uploadfileanddatapersistdb_v3.domain.model.enums.LineStatus;
import com.bank.uploadfileanddatapersistdb_v3.domain.model.enums.LogStatus;

import java.util.List;

/**
 * Use-case contract for import logs: create, update per line, finalize, and search.
 */
public interface LogChargementService {

    LogChargement startLog(String fileName);

    void addLine(LogChargement log, int lineNumber, LineStatus status, String detailProblem);

    void finalizeLog(LogChargement log, int totalLines, int successLines, int failedLines);

    List<LogChargement> getAllLogs();

    LogChargement getLogById(Long id);

    List<LogChargement> searchLogs(String fileName, LogStatus status);
}
