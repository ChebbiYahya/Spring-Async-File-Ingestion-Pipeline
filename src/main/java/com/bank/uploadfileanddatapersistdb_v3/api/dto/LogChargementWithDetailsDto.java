package com.bank.uploadfileanddatapersistdb_v3.api.dto;

import lombok.*;

import java.util.List;

/**
 * Detailed DTO for a log including line-by-line details.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LogChargementWithDetailsDto {
    private Long id;
    private String fileName;
    private String status;
    private String createdAt;
    private Integer totalLines;
    private Integer successLines;
    private Integer failedLines;
    private List<LogChargementDetailDto> details;
}
