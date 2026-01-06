package com.bank.uploadfileanddatapersistdb_v3.api.dto;
// DTO de resultat pour un batch.

import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Result of processing all files in DATA_IN (sync processing).
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BatchProcessingResultDto {

    private LocalDateTime startedAt;
    private LocalDateTime finishedAt;

    private int totalFiles;
    private int succeededFiles;
    private int failedFiles;

    @Builder.Default
    private List<FileProcessingItemDto> items = new ArrayList<>();

    /**
     * Per-file processing result.
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class FileProcessingItemDto {
        private String fileName;
        private String status; // SUCCESS / FAILED
        private Integer importedRecords;
        private String error; // short message
    }
}
