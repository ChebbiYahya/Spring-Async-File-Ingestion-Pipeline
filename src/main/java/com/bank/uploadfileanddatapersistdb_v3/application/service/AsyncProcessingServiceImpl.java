package com.bank.uploadfileanddatapersistdb_v3.application.service;

import com.bank.uploadfileanddatapersistdb_v3.application.interfaces.AsyncProcessingService;
import com.bank.uploadfileanddatapersistdb_v3.application.interfaces.FileIngestionService;
import com.bank.uploadfileanddatapersistdb_v3.application.interfaces.FolderService;
import com.bank.uploadfileanddatapersistdb_v3.application.interfaces.JobProgressService;
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

    private final DataFoldersProperties props;
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
            List<Path> files;
            try (var s = Files.list(props.inPath())) {
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
            // If counting fails, fallback to 0 => percent may stay 0, but processing still works.
            return 0;
        }
    }
}
