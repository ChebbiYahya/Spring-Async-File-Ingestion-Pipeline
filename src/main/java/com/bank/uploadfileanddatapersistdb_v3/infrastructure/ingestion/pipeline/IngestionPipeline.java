package com.bank.uploadfileanddatapersistdb_v3.infrastructure.ingestion.pipeline;

import com.bank.uploadfileanddatapersistdb_v3.application.interfaces.LogChargementService;
import com.bank.uploadfileanddatapersistdb_v3.domain.model.entity.LogChargement;
import com.bank.uploadfileanddatapersistdb_v3.domain.model.enums.LineStatus;
import com.bank.uploadfileanddatapersistdb_v3.infrastructure.ingestion.duplicate.DuplicateKeyBuilder;
import com.bank.uploadfileanddatapersistdb_v3.infrastructure.ingestion.duplicate.InFileDuplicateChecker;
import com.bank.uploadfileanddatapersistdb_v3.infrastructure.ingestion.validation.ErrorCode;
import com.bank.uploadfileanddatapersistdb_v3.infrastructure.ingestion.validation.FieldValidator;
import com.bank.uploadfileanddatapersistdb_v3.infrastructure.ingestion.validation.RecordValidationException;
import com.bank.uploadfileanddatapersistdb_v3.infrastructure.mapping.model.rules.FieldRule;
import com.bank.uploadfileanddatapersistdb_v3.application.interfaces.ProgressReporter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Generic ingestion pipeline:
 * 1) validate record using YAML mapping rules
 * 2) detect duplicates in file and in DB
 * 3) persist
 * 4) write line-by-line status into LogChargement
 *
 * Designed to be reusable for CSV and XML (any RecordReader producing Map<String,String>).
 */
@Component
@RequiredArgsConstructor
public class IngestionPipeline {

    private final LogChargementService logService;

    private final FieldValidator fieldValidator = new FieldValidator();
    private final DuplicateKeyBuilder keyBuilder = new DuplicateKeyBuilder();

    // Backward-compatible method (no progress reporting)
    public int process(
            String fileName,
            List<String> duplicateCheck,
            Iterator<Map<String, String>> rawRecords,
            List<? extends FieldRule> rules,
            RecordPersister persister,
            DuplicateDbChecker dbChecker
    ) {
        return process(fileName, duplicateCheck, rawRecords, rules, persister, dbChecker, null);
    }

    /**
     * Process records and optionally report progress per record.
     * Progress is reported after each record is handled (success or fail).
     */
    public int process(
            String fileName,
            List<String> duplicateCheck,
            Iterator<Map<String, String>> rawRecords,
            List<? extends FieldRule> rules,
            RecordPersister persister,
            DuplicateDbChecker dbChecker,
            ProgressReporter progressReporter
    ) {
        LogChargement log = logService.startLog(fileName);

        InFileDuplicateChecker inFile = new InFileDuplicateChecker();
        int success = 0;
        int line = 0;

        while (rawRecords.hasNext()) {
            line++;
            try {
                Map<String, String> raw = rawRecords.next();

                // 1) validation (string-level)
                Map<String, String> validated = new HashMap<>();
                for (FieldRule r : rules) {
                    String v = fieldValidator.validate(r, raw.get(r.getName()), line);
                    validated.put(r.getName(), v);
                }

                // 2) duplicates
                if (duplicateCheck != null && !duplicateCheck.isEmpty()) {
                    String key = keyBuilder.buildKey(duplicateCheck, new HashMap<>(validated));

                    if (inFile.isDuplicate(key)) {
                        throw new RecordValidationException(
                                ErrorCode.DUPLICATE_IN_FILE,
                                String.join(",", duplicateCheck),
                                line,
                                "Duplicate key in file for fields: " + duplicateCheck
                        );
                    }

                    if (dbChecker.exists(validated, duplicateCheck)) {
                        throw new RecordValidationException(
                                ErrorCode.DUPLICATE_IN_DB,
                                String.join(",", duplicateCheck),
                                line,
                                "Duplicate key in DB for fields: " + duplicateCheck
                        );
                    }
                }

                // 3) persist
                persister.persist(validated);
                success++;

                // 4) log
                logService.addLine(log, line, LineStatus.SUCCESS, null);

            } catch (RecordValidationException e) {
                logService.addLine(log, line, LineStatus.FAILED, e.getCode() + " - " + e.getMessage());

            } catch (Exception e) {
                logService.addLine(log, line, LineStatus.FAILED, "TECHNICAL - " + e.getMessage());

            } finally {
                // report progress for each record processed (success or failure)
                if (progressReporter != null) {
                    progressReporter.onRecordProcessed();
                }
            }
        }

        // finalizeLog sets overall status; counters are maintained in addLine()
        logService.finalizeLog(log, 0, 0, 0);
        return success;
    }

    public interface RecordPersister {
        void persist(Map<String, String> record);
    }

    public interface DuplicateDbChecker {
        boolean exists(Map<String, String> record, List<String> duplicateFields);
    }
}
