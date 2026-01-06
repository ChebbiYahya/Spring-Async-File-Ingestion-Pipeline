package com.bank.uploadfileanddatapersistdb_v3.api.mapper;
// Mapper entite <-> DTO log.

import com.bank.uploadfileanddatapersistdb_v3.api.dto.LogChargementDetailDto;
import com.bank.uploadfileanddatapersistdb_v3.api.dto.LogChargementSummaryDto;
import com.bank.uploadfileanddatapersistdb_v3.api.dto.LogChargementWithDetailsDto;
import com.bank.uploadfileanddatapersistdb_v3.domain.model.entity.LogChargement;
import com.bank.uploadfileanddatapersistdb_v3.domain.model.entity.LogChargementDetail;
import org.springframework.stereotype.Component;

import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Maps LogChargement/LogChargementDetail to API DTOs (summary and detailed).
 */
@Component
public class LogChargementMapper {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public LogChargementSummaryDto toSummaryDto(LogChargement log) {
        return LogChargementSummaryDto.builder()
                .id(log.getId())
                .fileName(log.getFileName())
                .status(log.getStatus())
                .createdAt(log.getCreatedAt())
                .totalLines(log.getTotalLines())
                .successLines(log.getSuccessLines())
                .failedLines(log.getFailedLines())
                .build();
    }

    public LogChargementDetailDto toDetailDto(LogChargementDetail detail) {
        return LogChargementDetailDto.builder()
                .lineNumber(detail.getLineNumber())
                .status(detail.getStatus())
                .detailProblem(detail.getDetailProblem())
                .build();
    }

    public LogChargementWithDetailsDto toWithDetailsDto(LogChargement log) {
        List<LogChargementDetailDto> detailDtos = log.getDetails().stream()
                .sorted(Comparator.comparing(LogChargementDetail::getLineNumber))
                .map(this::toDetailDto)
                .collect(Collectors.toList());

        return LogChargementWithDetailsDto.builder()
                .id(log.getId())
                .fileName(log.getFileName())
                .status(log.getStatus() != null ? log.getStatus().name() : null)
                .createdAt(log.getCreatedAt() != null ? log.getCreatedAt().format(DATE_TIME_FORMATTER) : null)
                .totalLines(log.getTotalLines())
                .successLines(log.getSuccessLines())
                .failedLines(log.getFailedLines())
                .details(detailDtos)
                .build();
    }
}
