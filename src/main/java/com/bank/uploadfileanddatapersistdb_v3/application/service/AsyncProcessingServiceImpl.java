package com.bank.uploadfileanddatapersistdb_v3.application.service;

import com.bank.uploadfileanddatapersistdb_v3.application.interfaces.*;
import com.bank.uploadfileanddatapersistdb_v3.config.DataFoldersProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * Asynchronous processing service:
 * - counts total records in DATA_IN
 * - runs file ingestion in async loop
 * - updates JobProgressService after each record
 */
@Service
@RequiredArgsConstructor
public class AsyncProcessingServiceImpl implements AsyncProcessingService {

    private final DataFoldersProvider folders;
    private final FolderService folderService;
    private final FileIngestionService ingestionService;
    private final JobProgressService jobProgressService;
    private final FileRecordCounter fileRecordCounter;

    /**
     * Starts a job: counts total records in current DATA_IN, stores job state.
     */
    public String startJob(String configId) {
        folderService.ensureFoldersExist();
        String id = (configId == null || configId.isBlank()) ? "EMPLOYEES" : configId;
        int totalRecords = countTotalRecordsInDataIn(id);
        return jobProgressService.start(totalRecords);
    }

    /**
     * Runs the job asynchronously. The controller triggers this method after startJob().
     */
    @Async
    public void runJob(String jobId, String configId) {
        String id = (configId == null || configId.isBlank()) ? "EMPLOYEES" : configId;
        try {
            while (true) {
                Path treatmentFile = folderService.moveOneFromInToTreatmentWithTimestamp();
                if (treatmentFile == null) break;

                String name = treatmentFile.getFileName().toString().toLowerCase(Locale.ROOT);

                try {
                    if (name.endsWith(".csv")) {
                        ingestionService.ingestCsvPathWithProgress(treatmentFile, id, () -> jobProgressService.incrementProcessed(jobId));
                    } else if (name.endsWith(".xml")) {
                        ingestionService.ingestXmlPathWithProgress(treatmentFile, id, () -> jobProgressService.incrementProcessed(jobId));
                    }
                    else {
                        // Unsupported -> failed (do not increment processedRecords; totalRecords was 0 for it)
                        folderService.moveTreatmentToFailed(treatmentFile);
                        continue;
                    }

                    folderService.moveTreatmentToBackup(treatmentFile);

                } catch (Exception ex) {
                    org.slf4j.LoggerFactory.getLogger(AsyncProcessingServiceImpl.class)
                            .error("Processing failed for file {}: {}", treatmentFile.getFileName(), ex.getMessage(), ex);

                    folderService.moveTreatmentToFailed(treatmentFile);
                    // Continue with next files, job is not interrupted
                }
            }

            jobProgressService.finish(jobId);

        } catch (Exception ex) {
            jobProgressService.fail(jobId);
        }
    }

    private int countTotalRecordsInDataIn(String configId) {
        try {
            // Dossier IN venant de la DB
            Path inDir = folders.inPath(configId);

            List<Path> files;
            try (var s = Files.list(inDir)) {
                files = s.filter(Files::isRegularFile)
                        .sorted(Comparator.comparing(Path::toString))
                        .collect(Collectors.toList());
            }

            int total = 0;
            for (Path p : files) {
                total += Math.max(0, fileRecordCounter.countRecords(p, configId));
            }
            return total;

        } catch (Exception e) {
            org.slf4j.LoggerFactory.getLogger(AsyncProcessingServiceImpl.class)
                    .error("Failed to count total records in DATA_IN: {}", e.getMessage(), e);
            return 0;
        }
    }

}
