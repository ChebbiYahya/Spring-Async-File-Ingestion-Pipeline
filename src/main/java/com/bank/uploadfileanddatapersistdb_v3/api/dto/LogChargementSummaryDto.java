package com.bank.uploadfileanddatapersistdb_v3.api.dto;
// DTO resume de log.

import com.bank.uploadfileanddatapersistdb_v3.domain.model.enums.LogStatus;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Lightweight DTO for listing import logs without details.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LogChargementSummaryDto {
    private Long id;
    private String fileName;
    private LogStatus status;
    private LocalDateTime createdAt;
    private Integer totalLines;
    private Integer successLines;
    private Integer failedLines;
}
