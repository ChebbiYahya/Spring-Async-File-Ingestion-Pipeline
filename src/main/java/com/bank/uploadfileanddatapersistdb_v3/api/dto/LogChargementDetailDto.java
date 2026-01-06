package com.bank.uploadfileanddatapersistdb_v3.api.dto;
// DTO detail de ligne de log.

import com.bank.uploadfileanddatapersistdb_v3.domain.model.enums.LineStatus;
import lombok.*;

/**
 * DTO for a single line detail in an import log.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LogChargementDetailDto {
    private Integer lineNumber;
    private LineStatus status;
    private String detailProblem;
}
